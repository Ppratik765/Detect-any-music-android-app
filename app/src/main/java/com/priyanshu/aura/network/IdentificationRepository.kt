package com.priyanshu.aura.network

import android.util.Base64
import android.util.Log
import com.priyanshu.aura.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object IdentificationRepository {

    private const val TAG = "IdentificationRepo"
    private const val REQ_URL = "/v1/identify"
    private const val HTTP_METHOD = "POST"
    private const val SIGNATURE_VERSION = "1"
    private const val DATA_TYPE = "audio"
    private const val TIMEOUT_MS = 20_000

    suspend fun identifyAudio(audioData: ByteArray): SongResult = withContext(Dispatchers.IO) {
        try {
            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val host = BuildConfig.ACR_HOST
            val accessKey = BuildConfig.ACR_ACCESS_KEY
            val secretKey = BuildConfig.ACR_SECRET_KEY

            val signature = generateSignature(
                httpMethod = HTTP_METHOD,
                reqUrl = REQ_URL,
                accessKey = accessKey,
                dataType = DATA_TYPE,
                signatureVersion = SIGNATURE_VERSION,
                timestamp = timestamp,
                secretKey = secretKey
            )

            val url = URL("https://$host$REQ_URL")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = HTTP_METHOD
                doOutput = true
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }

            val boundary = "----AuraV2Boundary${System.currentTimeMillis()}"
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

            val outputStream: OutputStream = connection.outputStream

            fun writeField(name: String, value: String) {
                outputStream.write("--$boundary\r\n".toByteArray())
                outputStream.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
                outputStream.write("$value\r\n".toByteArray())
            }

            writeField("access_key", accessKey)
            writeField("sample_bytes", audioData.size.toString())
            writeField("timestamp", timestamp)
            writeField("signature", signature)
            writeField("data_type", DATA_TYPE)
            writeField("signature_version", SIGNATURE_VERSION)

            outputStream.write("--$boundary\r\n".toByteArray())
            outputStream.write("Content-Disposition: form-data; name=\"sample\"; filename=\"sample.wav\"\r\n".toByteArray())
            outputStream.write("Content-Type: audio/wav\r\n\r\n".toByteArray())
            outputStream.write(audioData)
            outputStream.write("\r\n".toByteArray())
            outputStream.write("--$boundary--\r\n".toByteArray())
            outputStream.flush()
            outputStream.close()

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                Log.d(TAG, "ACRCloud raw response: $responseBody")
                return@withContext parseAcrResponse(responseBody)
            } else {
                connection.disconnect()
                return@withContext defaultFallback()
            }
        } catch (e: Exception) {
            Log.e(TAG, "identifyAudio failed", e)
            return@withContext defaultFallback()
        }
    }

    private fun generateSignature(
        httpMethod: String, reqUrl: String, accessKey: String, dataType: String,
        signatureVersion: String, timestamp: String, secretKey: String
    ): String {
        val stringToSign = "$httpMethod\n$reqUrl\n$accessKey\n$dataType\n$signatureVersion\n$timestamp"
        val signingKey = SecretKeySpec(secretKey.toByteArray(Charsets.UTF_8), "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(signingKey)
        val rawHmac = mac.doFinal(stringToSign.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(rawHmac, Base64.NO_WRAP)
    }

    /**
     * Scans the JSON Array and returns the first result that uses standard English (ASCII) characters.
     * If no English match is found, it safely falls back to the very first result.
     */
    private fun findBestEnglishMatch(jsonArray: JSONArray?): JSONObject? {
        if (jsonArray == null || jsonArray.length() == 0) return null

        // Regex to match standard English characters, numbers, and basic punctuation
        val englishRegex = "^[\\x00-\\x7F]+$".toRegex()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val title = item.optString("title", "")
            if (englishRegex.matches(title)) {
                return item // Found an English match!
            }
        }

        // Fallback to the first item if no English title is found
        return jsonArray.getJSONObject(0)
    }

    private fun parseAcrResponse(jsonString: String): SongResult {
        return try {
            val root = JSONObject(jsonString)
            val status = root.getJSONObject("status")
            if (status.getInt("code") != 0) return defaultFallback()

            val metadata = root.getJSONObject("metadata")

            // Use the new helper function to prioritise English matches
            val musicMatch = findBestEnglishMatch(metadata.optJSONArray("music"))
            val hummingMatch = findBestEnglishMatch(metadata.optJSONArray("humming"))

            val topResult = musicMatch ?: hummingMatch ?: return defaultFallback()

            val title = topResult.optString("title", "Unknown Title")
            val artistsArray = topResult.optJSONArray("artists")
            val artistName = if (artistsArray != null && artistsArray.length() > 0) {
                artistsArray.getJSONObject(0).optString("name", "Unknown Artist")
            } else "Unknown Artist"

            val externalMetadata = topResult.optJSONObject("external_metadata")
            var spotifyId: String? = null
            var youtubeId: String? = null

            if (externalMetadata != null) {
                spotifyId = externalMetadata.optJSONObject("spotify")?.optJSONObject("track")?.optString("id", null)
                youtubeId = externalMetadata.optJSONObject("youtube")?.optString("vid", null)
            }

            SongResult(title, artistName, spotifyId, youtubeId)
        } catch (e: Exception) {
            Log.e(TAG, "parseAcrResponse failed", e)
            defaultFallback()
        }
    }

    private fun defaultFallback() = SongResult(
        title = "Never Gonna Give You Up",
        artist = "Rick Astley",
        spotifyId = "4cOdK2wGLETKBW3PvgPWqT",
        youtubeId = "dQw4w9WgXcQ"
    )
}