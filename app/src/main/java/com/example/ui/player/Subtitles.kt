package com.example.ui.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

object SubtitleManager {
    suspend fun loadAndParseSrt(urlStr: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
        val cues = mutableListOf<SubtitleCue>()
        try {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 12000
            connection.readTimeout = 12000
            connection.setRequestProperty("User-Agent", "CinePulse-Android/1.0")
            connection.setRequestProperty("Accept", "*/*")
            connection.instanceFollowRedirects = true

            val code = connection.responseCode
            if (code !in 200..299) {
                connection.disconnect()
                return@withContext cues
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8))
            
            var line: String?
            var state = 0 // 0: index, 1: time, 2: text
            var startMs = 0L
            var endMs = 0L
            val textBuilder = StringBuilder()

            while (reader.readLine().also { line = it } != null) {
                val trimmed = line?.trim() ?: ""
                if (trimmed.isEmpty()) {
                    if (textBuilder.isNotEmpty()) {
                        cues.add(SubtitleCue(startMs, endMs, cleanSubtitleText(textBuilder.toString())))
                        textBuilder.clear()
                    }
                    state = 0
                    continue
                }

                when (state) {
                    0 -> {
                        if (trimmed.all { it.isDigit() }) {
                            state = 1
                        } else if (trimmed.contains("-->")) {
                            parseTimeLine(trimmed)?.let { (s, e) ->
                                startMs = s
                                endMs = e
                                state = 2
                            }
                        }
                    }
                    1 -> {
                        if (trimmed.contains("-->")) {
                            parseTimeLine(trimmed)?.let { (s, e) ->
                                startMs = s
                                endMs = e
                                state = 2
                            }
                        }
                    }
                    2 -> {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(trimmed)
                    }
                }
            }
            if (textBuilder.isNotEmpty()) {
                cues.add(SubtitleCue(startMs, endMs, cleanSubtitleText(textBuilder.toString())))
            }
            reader.close()
            connection.disconnect()
        } catch (e: Exception) {
            // Handled gracefully
        }
        cues
    }

    private fun cleanSubtitleText(raw: String): String {
        // Strip HTML tags like <i>, <b>, <font color="..."> if present in SRT
        return raw.replace(Regex("<[^>]*>"), "").trim()
    }

    private fun parseTimeLine(line: String): Pair<Long, Long>? {
        val parts = line.split("-->")
        if (parts.size != 2) return null
        val start = parseTimestamp(parts[0].trim()) ?: return null
        val end = parseTimestamp(parts[1].trim().split(" ").firstOrNull() ?: "") ?: return null
        return Pair(start, end)
    }

    private fun parseTimestamp(timeStr: String): Long? {
        val clean = timeStr.replace(',', '.')
        val parts = clean.split(":")
        if (parts.size < 2) return null
        return try {
            val hours = if (parts.size == 3) parts[0].toLong() else 0L
            val minutes = if (parts.size == 3) parts[1].toLong() else parts[0].toLong()
            val secParts = if (parts.size == 3) parts[2].split(".") else parts[1].split(".")
            val seconds = secParts[0].toLong()
            val millis = if (secParts.size > 1) secParts[1].padEnd(3, '0').take(3).toLong() else 0L

            (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
        } catch (e: Exception) {
            null
        }
    }
}
