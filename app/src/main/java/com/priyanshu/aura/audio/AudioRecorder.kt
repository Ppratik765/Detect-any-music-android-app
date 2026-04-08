package com.priyanshu.aura.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.coroutines.coroutineContext

class AudioRecorder(private val context: Context) {

    // Standard properties for ACRCloud (8000 Hz, Mono, 16-bit PCM)
    // We use standard 44100 since most phones mic supports it without downsampling exception,
    // ACRCloud can handle 44100 Hz PCM data perfectly fine.
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    @Volatile
    private var isRecording = false

    /**
     * Starts recording audio. Emits FFT magnitudes periodically and returns the raw PCM byte array upon completion.
     */
    suspend fun startRecording(
        onFftData: (FloatArray) -> Unit
    ): ByteArray? = withContext(Dispatchers.IO) {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            minBufferSize * 4
        )

        val rawAudioOut = ByteArrayOutputStream()

        try {
            audioRecord.startRecording()
            isRecording = true

            // We want FFT chunks of size 2048 (must be power of 2)
            val fftChunkSize = 2048
            val shortBuffer = ShortArray(fftChunkSize)

            while (isRecording && coroutineContext.isActive) {
                val readResult = audioRecord.read(shortBuffer, 0, shortBuffer.size)
                if (readResult > 0) {
                    // Convert short array to byte array for ACRCloud
                    val byteBuffer = ByteArray(readResult * 2)
                    for (i in 0 until readResult) {
                        val s = shortBuffer[i]
                        byteBuffer[i * 2] = (s.toInt() and 0x00FF).toByte()
                        byteBuffer[i * 2 + 1] = (s.toInt() shr 8).toByte()
                    }
                    rawAudioOut.write(byteBuffer)

                    // Compute FFT and give to UI
                    val magnitudes = FFT.computeMagnitudes(shortBuffer)
                    onFftData(magnitudes)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isRecording = false
            audioRecord.stop()
            audioRecord.release()
        }

        val pcmData = rawAudioOut.toByteArray()
        val wavHeader = getWavHeader(pcmData.size.toLong())
        val finalAudio = ByteArrayOutputStream()
        finalAudio.write(wavHeader)
        finalAudio.write(pcmData)

        return@withContext finalAudio.toByteArray()
    }
    
    private fun getWavHeader(pcmDataLen: Long): ByteArray {
        val totalDataLen = pcmDataLen + 36
        val sampleRateLong = sampleRate.toLong()
        val channels = 1
        val byteRate = sampleRateLong * channels * 2
        val header = ByteArray(44)
        
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRateLong and 0xff).toByte()
        header[25] = ((sampleRateLong shr 8) and 0xff).toByte()
        header[26] = ((sampleRateLong shr 16) and 0xff).toByte()
        header[27] = ((sampleRateLong shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (pcmDataLen and 0xff).toByte()
        header[41] = ((pcmDataLen shr 8) and 0xff).toByte()
        header[42] = ((pcmDataLen shr 16) and 0xff).toByte()
        header[43] = ((pcmDataLen shr 24) and 0xff).toByte()
        
        return header
    }
    
    fun stopRecording() {
        isRecording = false
    }
}
