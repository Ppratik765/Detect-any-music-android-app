package com.priyanshu.aura.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.priyanshu.aura.R
import com.priyanshu.aura.data.AuraDatabase
import com.priyanshu.aura.data.HistoryEntity
import com.priyanshu.aura.network.IdentificationRepository
import com.priyanshu.aura.network.SongResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AmbientListeningService : Service() {

    private val CHANNEL_ID = "AuraAmbientChannel"
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var audioRecorder: AudioRecorder? = null
    
    // Listen for 10 seconds, sleep for 60 seconds
    private val LISTEN_DURATION_MS = 10_000L
    private val SLEEP_DURATION_MS = 60_000L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(4, createAmbientNotification())

        audioRecorder = AudioRecorder(this)
        startAmbientLoop()

        return START_STICKY
    }

    private fun startAmbientLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    var audioBytes: ByteArray? = null
                    
                    val listenJob = launch {
                        audioBytes = audioRecorder?.startRecording { _ -> true }
                    }
                    
                    delay(LISTEN_DURATION_MS)
                    audioRecorder?.stopRecording()
                    listenJob.join()

                    audioBytes?.let { bytes ->
                        if (bytes.isNotEmpty()) {
                            val result = IdentificationRepository.identifyAudio(bytes)
                            if (result.title != "Never Gonna Give You Up" && result.title != "Unknown Title") {
                                // Silent save to history
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
                        }
                    }

                    // Sleep before next identification
                    delay(SLEEP_DURATION_MS)
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(SLEEP_DURATION_MS) // backoff
                }
            }
        }
    }

    private fun createAmbientNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto-Aura is Active")
            .setContentText("Silently logging songs playing around you")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // Temp icon
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Aura Ambient Mode",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder?.stopRecording()
    }
}
