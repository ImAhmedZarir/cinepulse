package com.example.data.model

import com.squareup.moshi.Json

data class HealthResponse(
    val status: String? = null,
    val time: String? = null
)

data class HomeResponse(
    val status: String? = null,
    val sections: List<HomeSection>? = null
)

data class HomeSection(
    val section: String,
    val count: Int? = null,
    val items: List<MovieItem> = emptyList()
)

data class MovieItem(
    val name: String? = "",
    @Json(name = "poster_url") val posterUrl: String? = null,
    val slug: String? = "",
    @Json(name = "subject_id") val rawSubjectId: Any? = null,
    val badge: String? = null,
    @Json(name = "rating") val rawRating: Any? = null,
    @Json(name = "year") val rawYear: Any? = null,
    val genre: String? = null,
    @Json(name = "type") val rawType: Any? = null
) {
    constructor(
        name: String?,
        posterUrl: String?,
        slug: String?,
        subjectId: String?,
        badge: String? = null,
        rating: String? = null,
        year: String? = null,
        genre: String? = null,
        type: String? = null
    ) : this(
        name = name,
        posterUrl = posterUrl,
        slug = slug,
        rawSubjectId = subjectId,
        badge = badge,
        rawRating = rating,
        rawYear = year,
        genre = genre,
        rawType = type
    )

    val subjectId: String
        get() = rawSubjectId?.toString() ?: ""

    val safeName: String
        get() = name ?: "Untitled"

    val safeSlug: String
        get() = slug ?: ""

    val rating: String?
        get() = rawRating?.toString()?.takeIf { it.isNotBlank() }

    val year: String?
        get() = when (rawYear) {
            is Number -> rawYear.toInt().toString()
            else -> rawYear?.toString()?.takeIf { it.isNotBlank() }
        }

    val type: String?
        get() = when (rawType) {
            is Number -> {
                val intVal = rawType.toInt()
                when (intVal) {
                    1 -> "movie"
                    2 -> "tv"
                    else -> intVal.toString()
                }
            }
            else -> rawType?.toString()
        }

    val isTvSeries: Boolean
        get() = type.equals("tv", ignoreCase = true) || type.equals("tv-series", ignoreCase = true)
}

data class CatalogResponse(
    val page: Int? = 1,
    @Json(name = "per_page") val perPage: Int? = 24,
    @Json(name = "has_more") val hasMore: Boolean? = false,
    val total: Long? = 0,
    val items: List<MovieItem> = emptyList()
)

data class SearchResponse(
    val query: String? = null,
    val page: Int? = 1,
    val total: Long? = 0,
    val items: List<MovieItem> = emptyList()
)

data class SuggestResponse(
    val suggestions: List<SuggestionItem> = emptyList()
)

data class SuggestionItem(
    val title: String? = "",
    val slug: String? = null,
    @Json(name = "subject_id") val rawSubjectId: Any? = null
) {
    val subjectId: String?
        get() = rawSubjectId?.toString()
}

data class DetailResponse(
    val code: Int? = 0,
    val message: String? = null,
    val data: DetailData? = null
)

data class DetailData(
    val subject: SubjectDetail? = null,
    val stars: List<StarItem>? = null,
    val resource: ResourceDetail? = null
)

data class SubjectDetail(
    @Json(name = "subjectId") val rawSubjectId: Any? = null,
    @Json(name = "subjectType") val rawSubjectType: Any? = null,
    val title: String? = null,
    val description: String? = null,
    val releaseDate: String? = null,
    @Json(name = "duration") val rawDuration: Any? = null,
    val genre: String? = null,
    val countryName: String? = null,
    @Json(name = "imdbRatingValue") val rawRating: Any? = null,
    val subtitles: String? = null,
    val hasResource: Boolean? = true,
    val cover: CoverDetail? = null,
    val trailer: TrailerDetail? = null
) {
    val subjectId: String?
        get() = rawSubjectId?.toString()

    val subjectType: Int?
        get() = when (rawSubjectType) {
            is Number -> rawSubjectType.toInt()
            is String -> rawSubjectType.toIntOrNull()
            else -> null
        }

    val duration: Long?
        get() = when (rawDuration) {
            is Number -> rawDuration.toLong()
            is String -> rawDuration.toLongOrNull()
            else -> null
        }

    val imdbRatingValue: String?
        get() = rawRating?.toString()?.takeIf { it.isNotBlank() }

    val isSeries: Boolean
        get() = subjectType == 2
}

data class StarItem(
    val name: String? = null,
    @Json(name = "character") val character: String? = null,
    @Json(name = "avatarUrl") val avatarUrl: String? = null,
    @Json(name = "staffType") val staffType: Int? = null
)

data class CoverDetail(
    val url: String? = null,
    val avgHueLight: String? = null,
    val avgHueDark: String? = null
)

data class TrailerDetail(
    val videoAddress: TrailerVideo? = null,
    val cover: CoverDetail? = null
)

data class TrailerVideo(
    val url: String? = null,
    val duration: Long? = null
)

data class ResourceDetail(
    val seasons: List<SeasonItem>? = null
)

data class SeasonItem(
    val se: Int = 1,
    val maxEp: Int = 1,
    val allEp: String? = null,
    val resolutions: List<SeasonResolution>? = null
)

data class SeasonResolution(
    val resolution: Int? = null,
    val epNum: Int? = null
)

data class StreamResponse(
    @Json(name = "subject_id") val rawSubjectId: Any? = null,
    val se: Int? = 1,
    val ep: Int? = 1,
    @Json(name = "has_resource") val hasResource: Boolean = false,
    val sources: List<StreamSource> = emptyList(),
    val hls: List<HlsStream> = emptyList(),
    val dash: List<DashStream> = emptyList(),
    @Json(name = "free_episodes") val freeEpisodes: Int? = null,
    val limited: Boolean? = false,
    val note: String? = null
) {
    constructor(
        subjectId: String?,
        se: Int? = 1,
        ep: Int? = 1,
        hasResource: Boolean = false,
        sources: List<StreamSource> = emptyList(),
        hls: List<HlsStream> = emptyList(),
        dash: List<DashStream> = emptyList(),
        freeEpisodes: Int? = null,
        limited: Boolean? = false,
        note: String? = null
    ) : this(
        rawSubjectId = subjectId,
        se = se,
        ep = ep,
        hasResource = hasResource,
        sources = sources,
        hls = hls,
        dash = dash,
        freeEpisodes = freeEpisodes,
        limited = limited,
        note = note
    )

    val subjectId: String?
        get() = rawSubjectId?.toString()
}

data class StreamSource(
    val resolution: String = "Auto",
    val format: String? = "MP4",
    val url: String = "",
    @Json(name = "size") val rawSize: Any? = null,
    @Json(name = "duration") val rawDuration: Any? = null,
    val codec: String? = null
) {
    val size: String?
        get() = rawSize?.toString()

    val duration: Long?
        get() = when (rawDuration) {
            is Number -> rawDuration.toLong()
            is String -> rawDuration.toLongOrNull()
            else -> null
        }

    val formattedSize: String?
        get() {
            val bytes = size?.toLongOrNull() ?: return null
            val mb = bytes / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format(java.util.Locale.US, "%.1f GB", mb / 1024.0)
            } else {
                String.format(java.util.Locale.US, "%.0f MB", mb)
            }
        }
}

data class HlsStream(
    val resolution: String? = null,
    val url: String? = null
)

data class DashStream(
    val resolution: String? = null,
    val url: String? = null
)

data class CaptionsResponse(
    @Json(name = "subject_id") val rawSubjectId: Any? = null,
    val se: Int? = 1,
    val ep: Int? = 1,
    val count: Int? = 0,
    val captions: List<CaptionItem> = emptyList()
) {
    constructor(
        subjectId: String?,
        se: Int? = 1,
        ep: Int? = 1,
        count: Int? = 0,
        captions: List<CaptionItem> = emptyList()
    ) : this(
        rawSubjectId = subjectId,
        se = se,
        ep = ep,
        count = count,
        captions = captions
    )

    val subjectId: String?
        get() = rawSubjectId?.toString()
}

data class CaptionItem(
    val id: String? = null,
    val lan: String? = null,
    val lanName: String? = null,
    val url: String = "",
    val size: String? = null,
    val delay: Long? = 0
)
