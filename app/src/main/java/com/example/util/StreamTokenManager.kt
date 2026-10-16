package com.example.util

import android.net.Uri
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import com.example.data.model.StreamResponse
import com.example.data.model.StreamSource
import com.example.data.remote.NetworkClient
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Manages signed stream URLs and token expiration for MovieBox streams.
 *
 * MovieBox streams are signed CDN links with strict short-lived validity (~1 minute cache).
 * When playing long movies or episodes, or when resuming after pause, links expire and ExoPlayer
 * throws ERROR_CODE_IO_UNSPECIFIED (HTTP 403 / connection drop on HTTP Range requests).
 *
 * StreamTokenManager provides:
 * 1. Automatic URI rewriting via ResolvingDataSource for every HTTP chunk/range request.
 * 2. Background proactive token keep-alive before expiration (35-40s cadence).
 * 3. Instant synchronized token refreshing when expired or forced.
 * 4. Transparent recovery across player buffer underruns and network glitches.
 */
@OptIn(UnstableApi::class)
object StreamTokenManager {

    private const val TAG = "StreamTokenManager"
    private const val TOKEN_MAX_AGE_MS = 40_000L // 40 seconds validity safety window

    private var repositoryRef: MovieRepository? = null
    private val lock = ReentrantLock()

    // Current active playback metadata
    @Volatile private var activeSubjectId: String? = null
    @Volatile private var activeSlug: String? = null
    @Volatile private var activeSeason: Int = 1
    @Volatile private var activeEpisode: Int = 1
    @Volatile private var activeIsSeries: Boolean = false
    @Volatile private var preferredResolution: String = "480p"

    // Cached sources and fetch timestamp
    private val lastFetchedEpochMs = AtomicLong(0L)
    @Volatile private var cachedSources: List<StreamSource> = emptyList()

    fun initialize(repository: MovieRepository) {
        this.repositoryRef = repository
    }

    fun setActiveSession(
        subjectId: String,
        slug: String,
        season: Int,
        episode: Int,
        isSeries: Boolean,
        resolution: String = "480p"
    ) {
        lock.withLock {
            val sessionChanged = activeSubjectId != subjectId ||
                    activeSlug != slug ||
                    activeSeason != season ||
                    activeEpisode != episode ||
                    activeIsSeries != isSeries

            activeSubjectId = subjectId
            activeSlug = slug
            activeSeason = season
            activeEpisode = episode
            activeIsSeries = isSeries
            preferredResolution = resolution

            if (sessionChanged) {
                cachedSources = emptyList()
                lastFetchedEpochMs.set(0L)
            }
        }
    }

    fun setPreferredResolution(resolution: String) {
        preferredResolution = resolution
    }

    fun updateActiveSources(sources: List<StreamSource>) {
        if (sources.isNotEmpty()) {
            lock.withLock {
                cachedSources = sources
                lastFetchedEpochMs.set(System.currentTimeMillis())
            }
        }
    }

    fun isTokenExpired(): Boolean {
        val last = lastFetchedEpochMs.get()
        if (last == 0L || cachedSources.isEmpty()) return true
        return (System.currentTimeMillis() - last) >= TOKEN_MAX_AGE_MS
    }

    /**
     * Resolves an ExoPlayer DataSpec on ExoPlayer's loader thread.
     * If the URI is a managed MovieBox stream and the signature is expiring or expired,
     * this automatically swaps the URI to the newly signed URL before opening the HTTP connection.
     */
    fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val currentUri = dataSpec.uri
        val uriStr = currentUri.toString()

        if (!isManagedStream(uriStr)) {
            return dataSpec
        }

        // Avoid blocking if called on Android UI/Main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return dataSpec
        }

        val freshUrl = getValidStreamUrl(uriStr, forceRefresh = false)
        return if (freshUrl != uriStr && freshUrl.isNotBlank()) {
            try {
                val newUri = Uri.parse(freshUrl)
                dataSpec.buildUpon().setUri(newUri).build()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to build resolved DataSpec: ${e.message}")
                dataSpec
            }
        } else {
            dataSpec
        }
    }

    /**
     * Returns a valid signed URL for the current title.
     * If existing URL is fresh, returns it immediately.
     * If expired or forced, fetches fresh signed URLs from API.
     */
    fun getValidStreamUrl(currentUrl: String, forceRefresh: Boolean = false): String {
        val now = System.currentTimeMillis()
        val last = lastFetchedEpochMs.get()
        val isExpired = forceRefresh || (now - last >= TOKEN_MAX_AGE_MS) || cachedSources.isEmpty()

        if (!isExpired) {
            val matching = findMatchingSource(cachedSources, preferredResolution)
            if (matching != null && matching.url.isNotBlank()) {
                return matching.url
            }
        }

        // Fetch fresh URL synchronously (running on background/loader thread)
        return lock.withLock {
            val doubleCheckNow = System.currentTimeMillis()
            val doubleCheckLast = lastFetchedEpochMs.get()
            if (!forceRefresh && (doubleCheckNow - doubleCheckLast < 5_000L) && cachedSources.isNotEmpty()) {
                val fresh = findMatchingSource(cachedSources, preferredResolution)
                return@withLock fresh?.url ?: currentUrl
            }

            val newSources = fetchFreshSourcesSync()
            if (newSources.isNotEmpty()) {
                cachedSources = newSources
                lastFetchedEpochMs.set(System.currentTimeMillis())
                val resolved = findMatchingSource(newSources, preferredResolution)
                resolved?.url ?: currentUrl
            } else {
                currentUrl
            }
        }
    }

    /**
     * Force-refreshes stream sources from the API.
     * Safe to call from UI coroutines or background threads.
     */
    suspend fun forceRefreshSources(): List<StreamSource> = withContext(Dispatchers.IO) {
        val subId = activeSubjectId ?: return@withContext emptyList()
        val slug = activeSlug ?: return@withContext emptyList()
        val se = activeSeason
        val ep = activeEpisode
        val isSeries = activeIsSeries

        try {
            val repo = repositoryRef
            val result = if (repo != null) {
                repo.getStream(subId, slug, se, ep, isSeries = isSeries).getOrNull()
            } else {
                NetworkClient.api.getStream(subjectId = subId, detailPath = slug, season = se, episode = ep)
            }

            val sources = result?.sources.orEmpty()
            if (sources.isNotEmpty()) {
                lock.withLock {
                    cachedSources = sources
                    lastFetchedEpochMs.set(System.currentTimeMillis())
                }
            }
            sources
        } catch (e: Exception) {
            Log.e(TAG, "forceRefreshSources error: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Lightweight background keep-alive check.
     * Refreshes token if it is nearing expiration (> 35s old).
     */
    suspend fun keepAlive() {
        if (activeSubjectId != null && isTokenExpired()) {
            forceRefreshSources()
        }
    }

    private fun fetchFreshSourcesSync(): List<StreamSource> {
        val subId = activeSubjectId ?: return emptyList()
        val slug = activeSlug ?: return emptyList()
        val se = activeSeason
        val ep = activeEpisode
        val isSeries = activeIsSeries

        return try {
            runBlocking(Dispatchers.IO) {
                val repo = repositoryRef
                val result = if (repo != null) {
                    repo.getStream(subId, slug, se, ep, isSeries = isSeries).getOrNull()
                } else {
                    NetworkClient.api.getStream(subjectId = subId, detailPath = slug, season = se, episode = ep)
                }
                result?.sources.orEmpty()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchFreshSourcesSync failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun findMatchingSource(sources: List<StreamSource>, resolutionPref: String): StreamSource? {
        if (sources.isEmpty()) return null
        return sources.firstOrNull { it.resolution.contains(resolutionPref, ignoreCase = true) }
            ?: sources.firstOrNull { it.resolution.contains("480", ignoreCase = true) }
            ?: sources.firstOrNull { it.resolution.contains("720", ignoreCase = true) }
            ?: sources.firstOrNull { it.resolution.contains("1080", ignoreCase = true) }
            ?: sources.firstOrNull()
    }

    private fun isManagedStream(url: String): Boolean {
        if (url.isBlank()) return false
        // Matches current active source URLs or URLs with signature parameters
        val activeMatches = cachedSources.any { it.url.isNotBlank() && url.startsWith(it.url.substringBefore("?")) }
        if (activeMatches) return true

        return url.contains("sign=", ignoreCase = true) ||
                url.contains(".mp4", ignoreCase = true) ||
                url.contains("moviebox", ignoreCase = true) ||
                url.contains("zarhosting", ignoreCase = true)
    }
}
