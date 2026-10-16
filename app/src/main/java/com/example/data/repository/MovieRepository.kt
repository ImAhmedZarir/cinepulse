package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.*
import com.example.data.remote.MovieApiService
import com.example.data.remote.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MovieRepository(
    private val api: MovieApiService = NetworkClient.api,
    private val database: AppDatabase,
    private val context: android.content.Context? = null
) {
    private val dao = database.watchDao()
    private val homeJsonAdapter by lazy { NetworkClient.moshi.adapter(HomeResponse::class.java) }
    private val cacheFile by lazy {
        context?.cacheDir?.resolve("cached_home_feed.json")
    }

    companion object {
        @Volatile private var inMemoryHome: HomeResponse? = null
        private val catalogCache = java.util.concurrent.ConcurrentHashMap<String, CatalogResponse>()
    }

    fun getCachedHome(): HomeResponse? {
        inMemoryHome?.let { return it }
        try {
            val file = cacheFile
            if (file != null && file.exists()) {
                val json = file.readText()
                val parsed = homeJsonAdapter.fromJson(json)
                if (parsed != null) {
                    inMemoryHome = parsed
                    return parsed
                }
            }
        } catch (_: Exception) {}
        return null
    }

    suspend fun getHome(): Result<HomeResponse> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHome()
            if (!response.sections.isNullOrEmpty()) {
                inMemoryHome = response
                try {
                    cacheFile?.writeText(homeJsonAdapter.toJson(response))
                } catch (_: Exception) {}
            }
            Result.success(response)
        } catch (e: Exception) {
            val cached = getCachedHome()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    fun getCachedCatalog(key: String): CatalogResponse? {
        return catalogCache[key]
    }

    suspend fun getMovies(page: Int, sort: String?, genre: String?): Result<CatalogResponse> =
        withContext(Dispatchers.IO) {
            val cacheKey = "movies_${page}_${sort}_${genre}"
            try {
                val response = api.getMovies(page = page, sort = sort, genre = genre)
                if (response.items.isNotEmpty()) {
                    catalogCache[cacheKey] = response
                }
                Result.success(response)
            } catch (e: Exception) {
                catalogCache[cacheKey]?.let { return@withContext Result.success(it) }
                Result.failure(e)
            }
        }

    suspend fun getTvSeries(page: Int, sort: String?, genre: String?): Result<CatalogResponse> =
        withContext(Dispatchers.IO) {
            val cacheKey = "tv_${page}_${sort}_${genre}"
            try {
                val response = api.getTvSeries(page = page, sort = sort, genre = genre)
                if (response.items.isNotEmpty()) {
                    catalogCache[cacheKey] = response
                }
                Result.success(response)
            } catch (e: Exception) {
                catalogCache[cacheKey]?.let { return@withContext Result.success(it) }
                Result.failure(e)
            }
        }

    suspend fun getAnimation(page: Int, sort: String?, genre: String?): Result<CatalogResponse> =
        withContext(Dispatchers.IO) {
            val cacheKey = "anim_${page}_${sort}_${genre}"
            try {
                val response = api.getAnimation(page = page, sort = sort, genre = genre)
                if (response.items.isNotEmpty()) {
                    catalogCache[cacheKey] = response
                }
                Result.success(response)
            } catch (e: Exception) {
                catalogCache[cacheKey]?.let { return@withContext Result.success(it) }
                Result.failure(e)
            }
        }

    suspend fun search(query: String, page: Int = 1): Result<SearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.search(query = query, page = page)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getSuggestions(query: String): Result<SuggestResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getSuggestions(query)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getDetail(slug: String): Result<DetailResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.getDetail(slug)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getStream(
        subjectId: String,
        slug: String,
        season: Int,
        episode: Int,
        isSeries: Boolean = false
    ): Result<StreamResponse> = withContext(Dispatchers.IO) {
        try {
            // For movies, Zar MovieBox API explicitly expects se=0, ep=0!
            val initialSe = if (isSeries) season.coerceAtLeast(1) else 0
            val initialEp = if (isSeries) episode.coerceAtLeast(1) else 0

            var response = try {
                api.getStream(subjectId = subjectId, detailPath = slug, season = initialSe, episode = initialEp)
            } catch (e: Exception) {
                StreamResponse(subjectId = subjectId, hasResource = false, sources = emptyList())
            }

            if (!response.hasResource || response.sources.isEmpty()) {
                val fallbacks = if (isSeries) {
                    listOf(Pair(season, episode), Pair(0, 0), Pair(1, 1), Pair(0, 1), Pair(1, 0))
                } else {
                    listOf(Pair(0, 0), Pair(1, 1), Pair(0, 1), Pair(1, 0))
                }.filter { it.first != initialSe || it.second != initialEp }

                for ((s, e) in fallbacks) {
                    try {
                        val alt = api.getStream(subjectId = subjectId, detailPath = slug, season = s, episode = e)
                        if (alt.hasResource && alt.sources.isNotEmpty()) {
                            response = alt
                            break
                        }
                    } catch (_: Exception) {}
                }
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCaptions(
        subjectId: String,
        slug: String,
        season: Int,
        episode: Int,
        isSeries: Boolean = false
    ): Result<CaptionsResponse> = withContext(Dispatchers.IO) {
        try {
            val initialSe = if (isSeries) season.coerceAtLeast(1) else 0
            val initialEp = if (isSeries) episode.coerceAtLeast(1) else 0

            var response = try {
                api.getCaptions(subjectId = subjectId, detailPath = slug, season = initialSe, episode = initialEp)
            } catch (e: Exception) {
                CaptionsResponse(subjectId = subjectId, captions = emptyList())
            }

            if (response.captions.isEmpty()) {
                val fallbacks = if (isSeries) {
                    listOf(Pair(season, episode), Pair(0, 0), Pair(1, 1), Pair(0, 1))
                } else {
                    listOf(Pair(0, 0), Pair(1, 1), Pair(0, 1))
                }.filter { it.first != initialSe || it.second != initialEp }

                for ((s, e) in fallbacks) {
                    try {
                        val alt = api.getCaptions(subjectId = subjectId, detailPath = slug, season = s, episode = e)
                        if (alt.captions.isNotEmpty()) {
                            response = alt
                            break
                        }
                    } catch (_: Exception) {}
                }
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Local Watch History
    val watchHistory: Flow<List<WatchHistoryEntity>> = dao.getAllHistory()

    suspend fun getHistoryItem(subjectId: String): WatchHistoryEntity? = withContext(Dispatchers.IO) {
        try {
            dao.getHistory(subjectId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveWatchProgress(
        subjectId: String,
        slug: String,
        title: String,
        posterUrl: String?,
        season: Int,
        episode: Int,
        positionMs: Long,
        durationMs: Long,
        type: String?
    ) = withContext(Dispatchers.IO) {
        try {
            if (durationMs > 0 && positionMs > 0) {
                dao.upsertHistory(
                    WatchHistoryEntity(
                        subjectId = subjectId,
                        slug = slug,
                        title = title,
                        posterUrl = posterUrl,
                        season = season,
                        episode = episode,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        lastWatchedTimestamp = System.currentTimeMillis(),
                        type = type
                    )
                )
            }
        } catch (_: Exception) {}
    }

    suspend fun deleteHistory(subjectId: String) = withContext(Dispatchers.IO) {
        try {
            dao.deleteHistory(subjectId)
        } catch (_: Exception) {}
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            dao.clearHistory()
        } catch (_: Exception) {}
    }

    // Local Watchlist
    val watchlist: Flow<List<WatchlistEntity>> = dao.getAllWatchlist()

    fun isWatchlisted(subjectId: String): Flow<Boolean> = dao.isWatchlisted(subjectId)

    suspend fun toggleWatchlist(
        subjectId: String,
        slug: String,
        title: String,
        posterUrl: String?,
        rating: String?,
        year: String?,
        genre: String?,
        type: String?,
        currentlyWatchlisted: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            if (currentlyWatchlisted) {
                dao.removeFromWatchlist(subjectId)
            } else {
                dao.addToWatchlist(
                    WatchlistEntity(
                        subjectId = subjectId,
                        slug = slug,
                        title = title,
                        posterUrl = posterUrl,
                        rating = rating,
                        year = year,
                        genre = genre,
                        type = type,
                        addedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        } catch (_: Exception) {}
    }
}
