package com.example.ui

import android.app.Application
import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp4.FragmentedMp4Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.example.data.local.AppDatabase
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.*
import com.example.data.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val HTTP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
private const val REFERER_HEADER = "https://moviebox.ph/"

enum class AppTab {
    HOME,
    CATALOG,
    SEARCH,
    LIBRARY
}

enum class CatalogType(val label: String) {
    MOVIES("Movies"),
    TV_SERIES("TV Series"),
    ANIMATION("Animation")
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val bannerItems: List<MovieItem> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val errorMessage: String? = null
)

data class CatalogUiState(
    val type: CatalogType = CatalogType.MOVIES,
    val selectedGenre: String = "All",
    val selectedSort: String = "foryou",
    val items: List<MovieItem> = emptyList(),
    val isLoading: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = true,
    val errorMessage: String? = null
)

data class SearchUiState(
    val query: String = "",
    val suggestions: List<String> = emptyList(),
    val results: List<MovieItem> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null
)

data class DetailPlayerUiState(
    val slug: String? = null,
    val subjectId: String? = null,
    val title: String = "",
    val detail: DetailResponse? = null,
    val isSeries: Boolean = false,
    val isLoadingDetail: Boolean = false,
    val selectedSeason: Int = 0,
    val selectedEpisode: Int = 0,
    val availableSeasons: List<SeasonItem> = emptyList(),
    val isPlaying: Boolean = false,
    val streamResponse: StreamResponse? = null,
    val captions: List<CaptionItem> = emptyList(),
    val isLoadingStream: Boolean = false,
    val streamError: String? = null,
    val initialSeekPositionMs: Long = 0L,
    val isWatchlisted: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = MovieRepository(database = database, context = application)

    // Navigation state
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Home state primed with disk/memory cache for instant 0ms load
    private val _homeState = MutableStateFlow(
        repository.getCachedHome()?.let { cached ->
            val sections = cached.sections.orEmpty()
            val bannerSection = sections.firstOrNull { it.section.equals("Banner", ignoreCase = true) }
            val bannerItems = bannerSection?.items.orEmpty()
            val otherSections = sections.filter { !it.section.equals("Banner", ignoreCase = true) && it.items.isNotEmpty() }
            HomeUiState(
                isLoading = false,
                bannerItems = bannerItems,
                sections = otherSections,
                errorMessage = null
            )
        } ?: HomeUiState()
    )
    val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    // Catalog state
    private val _catalogState = MutableStateFlow(CatalogUiState())
    val catalogState: StateFlow<CatalogUiState> = _catalogState.asStateFlow()

    // Search state
    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()
    private var searchJob: Job? = null

    // Detail & Player state
    private val _detailPlayerState = MutableStateFlow(DetailPlayerUiState())
    val detailPlayerState: StateFlow<DetailPlayerUiState> = _detailPlayerState.asStateFlow()

    // Room flows
    val watchHistory: StateFlow<List<WatchHistoryEntity>> = repository.watchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val watchlist: StateFlow<List<WatchlistEntity>> = repository.watchlist
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Single persistent ExoPlayer instance surviving screen rotation
    private var playerInstance: ExoPlayer? = null

    init {
        loadHomeFeed()
        loadCatalog(reset = true)
    }

    @OptIn(UnstableApi::class)
    fun getPlayer(context: Context): ExoPlayer {
        if (playerInstance == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(HTTP_USER_AGENT)
                .setDefaultRequestProperties(
                    mapOf(
                        "Referer" to REFERER_HEADER,
                        "Origin" to "https://moviebox.ph",
                        "Accept" to "*/*",
                        "Accept-Encoding" to "identity" // CRITICAL: Prevents CDN proxy from gzip-compressing MP4 containers which causes parsing container malformed
                    )
                )
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(25000)
                .setReadTimeoutMs(35000)

            val dataSourceFactory = DefaultDataSource.Factory(context.applicationContext, httpDataSourceFactory)

            // Robust MP4 and Fragmented MP4 extractors with tolerant sample table parsing and edit list workarounds
            val extractorsFactory = DefaultExtractorsFactory().apply {
                setConstantBitrateSeekingEnabled(true)
                setMp4ExtractorFlags(Mp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS)
                setFragmentedMp4ExtractorFlags(
                    FragmentedMp4Extractor.FLAG_WORKAROUND_IGNORE_EDIT_LISTS or
                    FragmentedMp4Extractor.FLAG_WORKAROUND_EVERY_VIDEO_FRAME_IS_SYNC_FRAME
                )
            }

            val loadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy(6)

            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)
                .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)

            // Resilient buffer control preventing buffer underruns and mid-stream chunk drops
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 30_000,
                    /* maxBufferMs = */ 60_000,
                    /* bufferForPlaybackMs = */ 2_500,
                    /* bufferForPlaybackAfterRebufferMs = */ 5_000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            playerInstance = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setSeekForwardIncrementMs(10000)
                .setSeekBackIncrementMs(10000)
                .build().apply {
                    playWhenReady = true
                }
        }
        return playerInstance!!
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun loadHomeFeed() {
        viewModelScope.launch {
            // Only show full loading indicator if we don't already have cached content
            if (_homeState.value.sections.isEmpty()) {
                _homeState.update { it.copy(isLoading = true, errorMessage = null) }
            }
            repository.getHome()
                .onSuccess { response ->
                    val sections = response.sections.orEmpty()
                    val bannerSection = sections.firstOrNull { it.section.equals("Banner", ignoreCase = true) }
                    val bannerItems = bannerSection?.items.orEmpty()
                    val otherSections = sections.filter { !it.section.equals("Banner", ignoreCase = true) && it.items.isNotEmpty() }
                    _homeState.update {
                        it.copy(
                            isLoading = false,
                            bannerItems = bannerItems,
                            sections = otherSections,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _homeState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (it.sections.isEmpty()) {
                                error.localizedMessage ?: "Failed to load home feed. Please check your connection."
                            } else null
                        )
                    }
                }
        }
    }

    fun setCatalogType(type: CatalogType) {
        if (_catalogState.value.type != type) {
            _catalogState.update { it.copy(type = type, page = 1) }
            loadCatalog(reset = true)
        }
    }

    fun setCatalogGenre(genre: String) {
        if (_catalogState.value.selectedGenre != genre) {
            _catalogState.update { it.copy(selectedGenre = genre, page = 1) }
            loadCatalog(reset = true)
        }
    }

    fun setCatalogSort(sort: String) {
        if (_catalogState.value.selectedSort != sort) {
            _catalogState.update { it.copy(selectedSort = sort, page = 1) }
            loadCatalog(reset = true)
        }
    }

    fun loadCatalog(reset: Boolean = false) {
        viewModelScope.launch {
            val currentState = _catalogState.value
            val targetPage = if (reset) 1 else currentState.page + 1

            if (!reset && !currentState.hasMore) return@launch

            val genreParam = if (currentState.selectedGenre == "All") null else currentState.selectedGenre
            val sortParam = currentState.selectedSort
            val cacheKey = when (currentState.type) {
                CatalogType.MOVIES -> "movies_${targetPage}_${sortParam}_${genreParam}"
                CatalogType.TV_SERIES -> "tv_${targetPage}_${sortParam}_${genreParam}"
                CatalogType.ANIMATION -> "anim_${targetPage}_${sortParam}_${genreParam}"
            }

            // Immediately display cached catalog items if available on reset
            if (reset) {
                val cached = repository.getCachedCatalog(cacheKey)
                if (cached != null && cached.items.isNotEmpty()) {
                    _catalogState.update {
                        it.copy(
                            items = cached.items,
                            isLoading = false,
                            page = 1,
                            hasMore = cached.hasMore ?: true
                        )
                    }
                } else if (currentState.items.isEmpty()) {
                    _catalogState.update { it.copy(isLoading = true, errorMessage = null) }
                }
            } else {
                _catalogState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            val result = when (currentState.type) {
                CatalogType.MOVIES -> repository.getMovies(page = targetPage, sort = sortParam, genre = genreParam)
                CatalogType.TV_SERIES -> repository.getTvSeries(page = targetPage, sort = sortParam, genre = genreParam)
                CatalogType.ANIMATION -> repository.getAnimation(page = targetPage, sort = sortParam, genre = genreParam)
            }

            result.onSuccess { response ->
                val newItems = if (reset) response.items else currentState.items + response.items
                _catalogState.update {
                    it.copy(
                        isLoading = false,
                        items = newItems,
                        page = targetPage,
                        hasMore = response.hasMore ?: (response.items.isNotEmpty()),
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _catalogState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Failed to load catalog."
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchState.update { it.copy(query = newQuery) }
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            _searchState.update { it.copy(suggestions = emptyList(), results = emptyList(), isSearching = false) }
            return
        }

        searchJob = viewModelScope.launch {
            delay(350)
            repository.getSuggestions(newQuery).onSuccess { res ->
                val titles = res.suggestions.mapNotNull { it.title }.filter { it.isNotBlank() }.distinct()
                _searchState.update { it.copy(suggestions = titles) }
            }
            performSearch(newQuery)
        }
    }

    fun performSearch(queryToSearch: String) {
        val q = queryToSearch.trim()
        if (q.isBlank()) return
        searchJob?.cancel()
        viewModelScope.launch {
            _searchState.update { it.copy(query = q, isSearching = true, errorMessage = null) }
            repository.search(query = q, page = 1)
                .onSuccess { res ->
                    _searchState.update {
                        it.copy(
                            isSearching = false,
                            results = res.items,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _searchState.update {
                        it.copy(
                            isSearching = false,
                            errorMessage = error.localizedMessage ?: "Search failed."
                        )
                    }
                }
        }
    }

    fun openDetail(slug: String, subjectId: String, title: String = "", initialType: String? = null) {
        val guessedSeries = initialType.equals("tv", ignoreCase = true) || initialType.equals("tv-series", ignoreCase = true)
        _detailPlayerState.value = DetailPlayerUiState(
            slug = slug,
            subjectId = subjectId,
            title = title,
            isSeries = guessedSeries,
            selectedSeason = if (guessedSeries) 1 else 0,
            selectedEpisode = if (guessedSeries) 1 else 0,
            isLoadingDetail = true
        )

        // Observe watchlist
        viewModelScope.launch {
            repository.isWatchlisted(subjectId).collectLatest { isWatchlisted ->
                _detailPlayerState.update { it.copy(isWatchlisted = isWatchlisted) }
            }
        }

        // Fetch detail
        viewModelScope.launch {
            val savedHistory = repository.getHistoryItem(subjectId)
            val prevPos = savedHistory?.positionMs ?: 0L
            val prevSe = savedHistory?.season ?: 1
            val prevEp = savedHistory?.episode ?: 1

            repository.getDetail(slug)
                .onSuccess { detailRes ->
                    val subjectType = detailRes.data?.subject?.subjectType
                    val rawSeasons = detailRes.data?.resource?.seasons.orEmpty()
                    // CRITICAL: A title is a series ONLY if subjectType == 2 OR (subjectType != 1 && rawSeasons has se > 0 and maxEp > 0)
                    val isMovie = subjectType == 1 || (!guessedSeries && subjectType != 2 && rawSeasons.all { it.se == 0 })
                    val isSeries = !isMovie && (subjectType == 2 || guessedSeries || rawSeasons.any { it.se > 0 && it.maxEp > 0 })

                    val validSeasons = if (isSeries) rawSeasons.filter { it.se > 0 && it.maxEp > 0 } else emptyList()
                    val actualTitle = detailRes.data?.subject?.title ?: title

                    val actualSe = if (isSeries) {
                        if (validSeasons.any { it.se == prevSe }) prevSe else (validSeasons.firstOrNull()?.se ?: 1)
                    } else 0
                    val actualEp = if (isSeries) prevEp.coerceAtLeast(1) else 0

                    _detailPlayerState.update {
                        it.copy(
                            detail = detailRes,
                            title = actualTitle,
                            isSeries = isSeries,
                            availableSeasons = validSeasons,
                            selectedSeason = actualSe,
                            selectedEpisode = actualEp,
                            isLoadingDetail = false,
                            initialSeekPositionMs = prevPos
                        )
                    }

                    // Pre-fetch captions so available subtitle languages (e.g. Hindi, English) are known immediately
                    viewModelScope.launch {
                        repository.getCaptions(subjectId, slug, season = actualSe, episode = actualEp, isSeries = isSeries)
                            .onSuccess { capRes ->
                                val caps = capRes.captions.orEmpty()
                                _detailPlayerState.update { it.copy(captions = caps) }
                            }
                    }
                }
                .onFailure { error ->
                    _detailPlayerState.update {
                        it.copy(
                            isLoadingDetail = false,
                            streamError = error.localizedMessage
                        )
                    }
                }
        }
    }

    fun playItem(
        slug: String,
        subjectId: String,
        season: Int = 0,
        episode: Int = 0,
        resumePositionMs: Long? = null
    ) {
        viewModelScope.launch {
            val current = _detailPlayerState.value
            val isSeries = current.isSeries

            // For movies, Zar MovieBox API explicitly requires se=0, ep=0!
            val actualSe = if (isSeries) season.coerceAtLeast(1) else 0
            val actualEp = if (isSeries) episode.coerceAtLeast(1) else 0

            val initialPos = resumePositionMs ?: repository.getHistoryItem(subjectId)?.positionMs ?: 0L

            _detailPlayerState.update {
                it.copy(
                    slug = slug,
                    subjectId = subjectId,
                    selectedSeason = actualSe,
                    selectedEpisode = actualEp,
                    isPlaying = true,
                    isLoadingStream = true,
                    streamError = null,
                    initialSeekPositionMs = initialPos
                )
            }

            // Fetch stream and captions in parallel
            val streamResult = repository.getStream(subjectId, slug, actualSe, actualEp, isSeries = isSeries)
            val captionsResult = repository.getCaptions(subjectId, slug, actualSe, actualEp, isSeries = isSeries)

            streamResult.onSuccess { streamRes ->
                val caps = captionsResult.getOrNull()?.captions.orEmpty()
                if (streamRes.sources.isNotEmpty()) {
                    _detailPlayerState.update {
                        it.copy(
                            streamResponse = streamRes,
                            captions = caps,
                            isLoadingStream = false,
                            streamError = null
                        )
                    }
                } else {
                    _detailPlayerState.update {
                        it.copy(
                            streamResponse = streamRes,
                            captions = caps,
                            isLoadingStream = false,
                            streamError = streamRes.note ?: "No stream sources available for this title."
                        )
                    }
                }
            }.onFailure { err ->
                _detailPlayerState.update {
                    it.copy(
                        isLoadingStream = false,
                        streamError = err.localizedMessage ?: "Failed to fetch stream link."
                    )
                }
            }
        }
    }

    suspend fun getStreamForDownload(subjectId: String, slug: String, season: Int, episode: Int): List<StreamSource> {
        val isSeries = _detailPlayerState.value.isSeries
        val s = if (isSeries) season.coerceAtLeast(1) else 0
        val e = if (isSeries) episode.coerceAtLeast(1) else 0
        return repository.getStream(subjectId, slug, s, e, isSeries = isSeries).getOrNull()?.sources.orEmpty()
    }

    fun selectEpisode(season: Int, episode: Int) {
        val s = _detailPlayerState.value
        val slug = s.slug ?: return
        val subjectId = s.subjectId ?: return
        playItem(slug, subjectId, season, episode, resumePositionMs = 0L)
    }

    fun saveProgress(posMs: Long, durMs: Long) {
        val s = _detailPlayerState.value
        val subId = s.subjectId ?: return
        val slug = s.slug ?: return
        val title = s.title.ifBlank { s.detail?.data?.subject?.title ?: "Movie" }
        val poster = s.detail?.data?.subject?.cover?.url
        val type = if (s.isSeries) "tv" else "movie"

        viewModelScope.launch {
            repository.saveWatchProgress(
                subjectId = subId,
                slug = slug,
                title = title,
                posterUrl = poster,
                season = s.selectedSeason,
                episode = s.selectedEpisode,
                positionMs = posMs,
                durationMs = durMs,
                type = type
            )
        }
    }

    fun toggleWatchlist() {
        val s = _detailPlayerState.value
        val subId = s.subjectId ?: return
        val slug = s.slug ?: return
        val subject = s.detail?.data?.subject
        val title = s.title.ifBlank { subject?.title ?: "Movie" }
        val poster = subject?.cover?.url
        val rating = subject?.imdbRatingValue
        val year = subject?.releaseDate?.take(4)
        val genre = subject?.genre
        val type = if (s.isSeries) "tv" else "movie"

        viewModelScope.launch {
            repository.toggleWatchlist(
                subjectId = subId,
                slug = slug,
                title = title,
                posterUrl = poster,
                rating = rating,
                year = year,
                genre = genre,
                type = type,
                currentlyWatchlisted = s.isWatchlisted
            )
        }
    }

    fun deleteHistory(subjectId: String) {
        viewModelScope.launch {
            repository.deleteHistory(subjectId)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun closePlayer() {
        playerInstance?.pause()
        playerInstance?.stop()
        _detailPlayerState.update { it.copy(isPlaying = false) }
    }

    fun closeDetail() {
        playerInstance?.pause()
        playerInstance?.stop()
        _detailPlayerState.value = DetailPlayerUiState()
    }

    override fun onCleared() {
        super.onCleared()
        playerInstance?.release()
        playerInstance = null
    }
}
