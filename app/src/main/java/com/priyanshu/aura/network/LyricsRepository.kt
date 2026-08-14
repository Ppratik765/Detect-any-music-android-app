package com.priyanshu.aura.network

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object LyricsRepository {

    private const val TAG = "LyricsRepository"
    private const val BASE_URL = "https://lrclib.net/api/get"

    suspend fun getLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val urlBuilder = Uri.parse(BASE_URL).buildUpon()
            urlBuilder.appendQueryParameter("track_name", title)
            urlBuilder.appendQueryParameter("artist_name", artist)

            val url = URL(urlBuilder.build().toString())
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            if (connection.responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                
                val root = JSONObject(responseBody)
                // Prefer plainLyrics if available as it is already formatted without timestamps
                var plainLyrics = root.optString("plainLyrics", "")
                if (plainLyrics.isNotBlank()) {
                    return@withContext plainLyrics.trim()
                }

                // If only syncedLyrics is available, strip out the timestamps [mm:ss.xx]
                val syncedLyrics = root.optString("syncedLyrics", "")
                if (syncedLyrics.isNotBlank()) {
                    return@withContext cleanLyrics(syncedLyrics)
                }
            }
            connection.disconnect()
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch lyrics", e)
            return@withContext null
        }
    }

    private fun cleanLyrics(raw: String): String {
        return raw.lines()
            .map { line ->
                // Strip timestamps like [00:12.34], [01:05.123], [02:10]
                line.replace(Regex("^\\[\\d+:\\d+(?:\\.\\d+)?\\]\\s*"), "")
                    .replace(Regex("\\[\\d+:\\d+(?:\\.\\d+)?\\]"), "")
            }
            // Strip metadata headers like [ar:Artist], [ti:Title], [length:03:30]
            .filterNot { it.matches(Regex("^\\[[a-zA-Z]+:.*?\\]$")) }
            .joinToString("\n")
            .trim()
    }
}
