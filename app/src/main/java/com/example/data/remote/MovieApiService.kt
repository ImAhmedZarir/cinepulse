package com.example.data.remote

import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface MovieApiService {

    @GET("health")
    suspend fun checkHealth(): HealthResponse

    @GET("home")
    suspend fun getHome(): HomeResponse

    @GET("movies")
    suspend fun getMovies(
        @Query("page") page: Int = 1,
        @Query("sort") sort: String? = null,
        @Query("genre") genre: String? = null,
        @Query("country") country: String? = null,
        @Query("year") year: String? = null
    ): CatalogResponse

    @GET("tv-series")
    suspend fun getTvSeries(
        @Query("page") page: Int = 1,
        @Query("sort") sort: String? = null,
        @Query("genre") genre: String? = null,
        @Query("country") country: String? = null,
        @Query("year") year: String? = null
    ): CatalogResponse

    @GET("animation")
    suspend fun getAnimation(
        @Query("page") page: Int = 1,
        @Query("sort") sort: String? = null,
        @Query("genre") genre: String? = null
    ): CatalogResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1
    ): SearchResponse

    @GET("search/suggest")
    suspend fun getSuggestions(
        @Query("q") query: String
    ): SuggestResponse

    @GET("detail/{slug}")
    suspend fun getDetail(
        @Path("slug") slug: String
    ): DetailResponse

    @GET("stream/{subject_id}")
    suspend fun getStream(
        @Path("subject_id") subjectId: String,
        @Query("detail_path") detailPath: String,
        @Query("se") season: Int = 1,
        @Query("ep") episode: Int = 1
    ): StreamResponse

    @GET("stream/{subject_id}/captions")
    suspend fun getCaptions(
        @Path("subject_id") subjectId: String,
        @Query("detail_path") detailPath: String,
        @Query("se") season: Int = 1,
        @Query("ep") episode: Int = 1
    ): CaptionsResponse
}

object NetworkClient {
    private const val BASE_URL = "https://movieapi.zarhosting.dpdns.org/api/public/v1/"
    private var cacheDir: java.io.File? = null

    fun init(cacheDirectory: java.io.File) {
        cacheDir = cacheDirectory
    }

    val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .writeTimeout(18, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "CinePulse-Android/1.0")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                // Cache public GET requests for 15 minutes (900 seconds) on device
                if (chain.request().method.equals("GET", ignoreCase = true) && response.isSuccessful) {
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=900")
                        .removeHeader("Pragma")
                        .build()
                } else {
                    response
                }
            }

        cacheDir?.let { dir ->
            try {
                val httpCacheDir = java.io.File(dir, "http_cache")
                builder.cache(okhttp3.Cache(httpCacheDir, 30L * 1024 * 1024))
            } catch (_: Exception) {}
        }

        builder.build()
    }

    val api: MovieApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MovieApiService::class.java)
    }
}
