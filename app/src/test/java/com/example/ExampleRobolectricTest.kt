package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.WatchHistoryEntity
import com.example.data.local.WatchlistEntity
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAppNameString() {
        val appName = context.getString(R.string.app_name)
        assertEquals("CinePulse", appName)
    }

    @Test
    fun testMovieItemTypeToleranceWithNumericType() {
        // Test parsing JSON with "type": 6 (integer from YouTube clip in search results)
        val jsonNumericType = """
            {
                "name": "Avatar Clip",
                "poster_url": "https://example.com/poster.jpg",
                "slug": "avatar-clip",
                "subject_id": "3668520158454317152",
                "type": 6,
                "rating": 7.9,
                "year": 2020
            }
        """.trimIndent()
        val adapter = moshi.adapter(MovieItem::class.java)
        val item = adapter.fromJson(jsonNumericType)
        assertNotNull(item)
        assertEquals("Avatar Clip", item?.safeName)
        assertEquals("3668520158454317152", item?.subjectId)
        assertEquals("6", item?.type)
        assertEquals("7.9", item?.rating)
        assertEquals("2020", item?.year)
    }

    @Test
    fun testSubjectDetailStringDurationTolerance() {
        // Test parsing detail where duration is a string "5880"
        val jsonDetail = """
            {
                "subjectId": "223695587521217720",
                "subjectType": 1,
                "title": "Runner",
                "duration": "5880",
                "imdbRatingValue": 6.6
            }
        """.trimIndent()
        val adapter = moshi.adapter(SubjectDetail::class.java)
        val detail = adapter.fromJson(jsonDetail)
        assertNotNull(detail)
        assertEquals("Runner", detail?.title)
        assertEquals(1, detail?.subjectType)
        assertFalse(detail?.isSeries ?: true)
        assertEquals(5880L, detail?.duration)
        assertEquals("6.6", detail?.imdbRatingValue)
    }

    @Test
    fun testWatchHistoryPersistence() = runBlocking {
        val dao = database.watchDao()
        val item = WatchHistoryEntity(
            subjectId = "123456",
            slug = "test-movie-slug",
            title = "Test Movie",
            posterUrl = "https://example.com/poster.jpg",
            season = 0,
            episode = 0,
            positionMs = 125000L,
            durationMs = 3600000L,
            type = "movie"
        )
        dao.upsertHistory(item)

        val retrieved = dao.getHistory("123456")
        assertNotNull(retrieved)
        assertEquals("Test Movie", retrieved?.title)
        assertEquals(125000L, retrieved?.positionMs)
        assertEquals(0, retrieved?.season)
        assertEquals(0, retrieved?.episode)
        assertEquals("movie", retrieved?.type)

        // Test update progress
        val updated = item.copy(positionMs = 250000L)
        dao.upsertHistory(updated)

        val retrievedUpdated = dao.getHistory("123456")
        assertEquals(250000L, retrievedUpdated?.positionMs)

        // Test deletion
        dao.deleteHistory("123456")
        val afterDelete = dao.getHistory("123456")
        assertNull(afterDelete)
    }

    @Test
    fun testWatchlistToggle() = runBlocking {
        val dao = database.watchDao()
        val item = WatchlistEntity(
            subjectId = "987654",
            slug = "breaking-bad",
            title = "Breaking Bad",
            posterUrl = "https://example.com/bb.jpg",
            rating = "9.5",
            year = "2008",
            genre = "Crime,Drama",
            type = "tv"
        )

        val initiallySaved = dao.isWatchlisted("987654").first()
        assertFalse(initiallySaved)

        dao.addToWatchlist(item)
        val afterAdd = dao.isWatchlisted("987654").first()
        assertTrue(afterAdd)

        val all = dao.getAllWatchlist().first()
        assertEquals(1, all.size)
        assertEquals("Breaking Bad", all[0].title)

        dao.removeFromWatchlist("987654")
        val afterRemove = dao.isWatchlisted("987654").first()
        assertFalse(afterRemove)
    }

    @Test
    fun testStreamModel() {
        val streamResponse = StreamResponse(
            subjectId = "112233",
            se = 0,
            ep = 0,
            hasResource = true,
            sources = listOf(
                StreamSource(resolution = "1080p", url = "https://stream.example.com/1080.mp4"),
                StreamSource(resolution = "720p", url = "https://stream.example.com/720.mp4"),
                StreamSource(resolution = "480p", url = "https://stream.example.com/480.mp4")
            )
        )
        assertTrue(streamResponse.hasResource)
        assertEquals(3, streamResponse.sources.size)
        assertEquals("1080p", streamResponse.sources[0].resolution)
    }
}
