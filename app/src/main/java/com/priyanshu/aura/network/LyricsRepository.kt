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
                // Prefer synced lyrics, fallback to plain
                var lyrics = root.optString("syncedLyrics", "")
                if (lyrics.isBlank()) {
                    lyrics = root.optString("plainLyrics", "")
                }
                
                if (lyrics.isNotBlank()) return@withContext lyrics
            }
            connection.disconnect()
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch lyrics", e)
            return@withContext null
        }
    }
}
