package com.priyanshu.aura.audio

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.priyanshu.aura.MainActivity
import com.priyanshu.aura.R
import com.priyanshu.aura.data.AuraDatabase
import com.priyanshu.aura.data.HistoryEntity
import com.priyanshu.aura.network.IdentificationRepository
import com.priyanshu.aura.network.SongResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class InternalAudioService : Service() {

    private val CHANNEL_ID = "AuraInternalCaptureChannel"
    private val RESULT_CHANNEL_ID = "AuraResultChannel"
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data = intent?.getParcelableExtra<Intent>("DATA")

        createNotificationChannels()
        startForeground(1, createListeningNotification())

        if (resultCode == Activity.RESULT_OK && data != null) {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            startInternalCapture()
        } else {
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startInternalCapture() {
        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(44100)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        audioRecord = AudioRecord.Builder()
            .setAudioFormat(format)
            .setAudioPlaybackCaptureConfig(config)
            .build()

        serviceScope.launch {
            try {
                audioRecord?.startRecording()
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                
                // Record for roughly 8 seconds
                val endTime = System.currentTimeMillis() + 8000
                while (System.currentTimeMillis() < endTime && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) out.write(buffer, 0, read)
                }
                audioRecord?.stop()

                val audioBytes = out.toByteArray()
                if (audioBytes.isNotEmpty()) {
                    val result = IdentificationRepository.identifyAudio(audioBytes)
                    if (result.title != "Never Gonna Give You Up" && result.title != "Unknown Title") {
                        saveToHistory(result)
                        showResultNotification(result)
                    } else {
                        showErrorNotification()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopSelf()
            }
        }
    }

    private suspend fun saveToHistory(result: SongResult) {
        val dao = AuraDatabase.getDatabase(applicationContext).historyDao()
        dao.insertHistory(
            HistoryEntity(
                title = result.title,
                artist = result.artist,
                albumCoverUrl = result.albumCoverUrl,
                spotifyId = result.spotifyId,
                youtubeId = result.youtubeId
            )
        )
    }

    private fun createListeningNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aura is Listening...")
            .setContentText("Identifying internal audio")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Temp icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showResultNotification(result: SongResult) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // Spotify Intent for Action Button
        val spotifyIntent = if (result.spotifyId != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse("spotify:track:${result.spotifyId}"))
        } else {
            val query = Uri.encode("${result.title} ${result.artist}")
            Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$query"))
        }
        val spotifyPending = PendingIntent.getActivity(this, 1, spotifyIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setContentTitle(result.title)
            .setContentText("By ${result.artist}")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Open in Spotify", spotifyPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification)
    }
    
    private fun showErrorNotification() {
        val notification = NotificationCompat.Builder(this, RESULT_CHANNEL_ID)
            .setContentTitle("No match found")
            .setContentText("Aura couldn't identify the song.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Aura Listening Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val resultChannel = NotificationChannel(
                RESULT_CHANNEL_ID,
                "Aura Identifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
            manager.createNotificationChannel(resultChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecord?.release()
        mediaProjection?.stop()
    }
}
