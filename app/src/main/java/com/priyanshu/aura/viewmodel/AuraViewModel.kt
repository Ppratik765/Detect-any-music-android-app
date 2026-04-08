package com.priyanshu.aura.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priyanshu.aura.audio.AudioRecorder
import com.priyanshu.aura.network.AcrCloudApi
import com.priyanshu.aura.network.SongResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuraState {
    object Idle : AuraState()
    data class Recording(val fftData: FloatArray) : AuraState()
    object Processing : AuraState()
    data class Success(val result: SongResult) : AuraState()
}

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuraState>(AuraState.Idle)
    val uiState: StateFlow<AuraState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder(application.applicationContext)
    private var recordingJob: Job? = null
    
    // Limits the recording duration as per user request to 15 seconds
    private val RECORD_DURATION_MS = 15000L

    fun startListening() {
        if (_uiState.value is AuraState.Recording || _uiState.value is AuraState.Processing) {
            return
        }

        _uiState.value = AuraState.Recording(FloatArray(0))

        recordingJob = viewModelScope.launch {
            // A timeout job that will gracefully stop the recorder after 15 seconds
            val timeoutJob = launch { 
                delay(RECORD_DURATION_MS)
                audioRecorder.stopRecording()
            }
            
            // startRecording now blocks until stopRecording() is called, returning the byte array
            val rawAudio = audioRecorder.startRecording { fftMagnitudes ->
                if (_uiState.value is AuraState.Recording) {
                    _uiState.value = AuraState.Recording(fftMagnitudes.take(60).toFloatArray())
                }
            }
            
            timeoutJob.cancel() // Cancel the timeout if it stopped early

            rawAudio?.let { audioBytes ->
                if (audioBytes.isNotEmpty()) {
                    _uiState.value = AuraState.Processing
                    val result = AcrCloudApi.identifySong(audioBytes)
                    _uiState.value = AuraState.Success(result)
                } else {
                    _uiState.value = AuraState.Idle
                }
            } ?: run {
                _uiState.value = AuraState.Idle
            }
        }
    }

    fun handleActionButtonClick() {
        when (_uiState.value) {
            is AuraState.Idle -> startListening()
            is AuraState.Recording -> audioRecorder.stopRecording() // Stops recording early, advances to Processing
            is AuraState.Success -> resetToIdle()
            else -> {}
        }
    }
    
    fun resetToIdle() {
        _uiState.value = AuraState.Idle
        recordingJob?.cancel()
    }
}
