package com.example.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

object DownloadHelper {
    private const val HTTP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private const val REFERER_HEADER = "https://moviebox.ph/"
    private const val ORIGIN_HEADER = "https://moviebox.ph"

    fun downloadVideo(
        context: Context,
        url: String,
        title: String,
        resolution: String,
        season: Int? = null,
        episode: Int? = null
    ) {
        if (url.isBlank()) {
            Toast.makeText(context, "No download stream link available.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                ?: throw IllegalStateException("DownloadManager is not available on this device.")

            val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
            val ext = if (url.contains(".m3u8", ignoreCase = true)) "m3u8" else "mp4"
            val fileName = if (season != null && episode != null && season > 0 && episode > 0) {
                "${safeTitle}_S${season}E${episode}_${resolution}.${ext}"
            } else {
                "${safeTitle}_${resolution}.${ext}"
            }

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(fileName)
                .setDescription("Downloading $title ($resolution) via CinePulse")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "CinePulse/$fileName")
                .addRequestHeader("User-Agent", HTTP_USER_AGENT)
                .addRequestHeader("Referer", REFERER_HEADER)
                .addRequestHeader("Origin", ORIGIN_HEADER)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            downloadManager.enqueue(request)
            Toast.makeText(context, "Downloading $fileName to Downloads/CinePulse", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Fallback: If subfolder creation is restricted on specific vendor ROM, retry directly in Downloads root
            try {
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    ?: throw e
                val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_").trim('_')
                val fileName = "${safeTitle}_${resolution}.mp4"
                val fallbackRequest = DownloadManager.Request(Uri.parse(url))
                    .setTitle(fileName)
                    .setDescription("Downloading $title ($resolution) via CinePulse")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    .addRequestHeader("User-Agent", HTTP_USER_AGENT)
                    .addRequestHeader("Referer", REFERER_HEADER)
                    .addRequestHeader("Origin", ORIGIN_HEADER)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                downloadManager.enqueue(fallbackRequest)
                Toast.makeText(context, "Downloading $fileName to Downloads", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Toast.makeText(context, "Download failed: ${e2.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
