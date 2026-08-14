package com.priyanshu.aura.network

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ArtworkRepository {

    private const val TAG = "ArtworkRepository"

    suspend fun getArtworkUrl(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        // Try iTunes Search first for high-res cover art (600x600)
        val itunesArt = fetchFromItunes(title, artist)
        if (!itunesArt.isNullOrBlank()) return@withContext itunesArt

        // Fallback to Deezer Search API
        val deezerArt = fetchFromDeezer(title, artist)
        if (!deezerArt.isNullOrBlank()) return@withContext deezerArt

        return@withContext null
    }

    private fun fetchFromItunes(title: String, artist: String): String? {
        try {
            val query = "$title $artist".trim()
            val uri = Uri.parse("https://itunes.apple.com/search")
                .buildUpon()
                .appendQueryParameter("term", query)
                .appendQueryParameter("entity", "song")
                .appendQueryParameter("limit", "1")
                .build()

            val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode in 200..299) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val root = JSONObject(body)
                val results = root.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val first = results.getJSONObject(0)
                    val rawUrl = first.optString("artworkUrl100", "")
                    if (rawUrl.isNotBlank()) {
                        // Upgrade to high-resolution 600x600
                        return rawUrl.replace("100x100bb", "600x600bb")
                    }
                }
            } else {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "iTunes artwork fetch failed", e)
        }
        return null
    }

    private fun fetchFromDeezer(title: String, artist: String): String? {
        try {
            val query = "$title $artist".trim()
            val uri = Uri.parse("https://api.deezer.com/search")
                .buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("limit", "1")
                .build()

            val connection = (URL(uri.toString()).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
            }

            if (connection.responseCode in 200..299) {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val root = JSONObject(body)
                val data = root.optJSONArray("data")
                if (data != null && data.length() > 0) {
                    val first = data.getJSONObject(0)
                    val album = first.optJSONObject("album")
                    val cover = album?.optString("cover_big", "")
                        ?: album?.optString("cover_medium", "")
                    if (!cover.isNullOrBlank()) {
                        return cover
                    }
                }
            } else {
                connection.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Deezer artwork fetch failed", e)
        }
        return null
    }
}
