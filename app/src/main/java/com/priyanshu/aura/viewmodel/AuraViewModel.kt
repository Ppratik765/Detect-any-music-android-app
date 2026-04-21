package com.priyanshu.aura.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priyanshu.aura.audio.AudioRecorder
import com.priyanshu.aura.network.IdentificationRepository
import com.priyanshu.aura.network.SongResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * V2 state machine:
 *
 *   Idle ──▶ Listening ──▶ Processing ──▶ Success ──▶ Explanation
 *    ▲                                        │              │
 *    └────────────────────────────────────────┘──────────────┘
 *
 * - **Idle**: waiting for user tap.
 * - **Listening**: mic is active, FFT frames are emitted for the visualizer.
 * - **Processing**: audio has been sent to the combined ACRCloud engine;
 *   the server may take 3–6 seconds because it runs both fingerprinting
 *   and humming identification in parallel.
 * - **Success**: result received.
 * - **Explanation**: deep-dive "How it works" screen.
 */
sealed class AuraState {
    object Idle : AuraState()
    data class Listening(val fftData: FloatArray) : AuraState()
    object Processing : AuraState()
    data class Success(val result: SongResult, val fftSnapshot: FloatArray) : AuraState()
    data class Explanation(val result: SongResult, val fftSnapshot: FloatArray) : AuraState()
}

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AuraState>(AuraState.Idle)
    val uiState: StateFlow<AuraState> = _uiState.asStateFlow()

    private val audioRecorder = AudioRecorder(application.applicationContext)
    private var recordingJob: Job? = null

    /** Maximum recording window — 15 seconds of audio is more than enough for both engines. */
    private val RECORD_DURATION_MS = 15_000L

    // ──────────────────────────────────────────────────────────────────────
    //  Recording flow
    // ──────────────────────────────────────────────────────────────────────

    fun startListening() {
        // Guard: don't restart if already active.
        if (_uiState.value is AuraState.Listening || _uiState.value is AuraState.Processing) {
            return
        }

        _uiState.value = AuraState.Listening(FloatArray(0))

        recordingJob = viewModelScope.launch {
            // Safety timeout — auto-stop after 15 s
            val timeoutJob = launch {
                delay(RECORD_DURATION_MS)
                audioRecorder.stopRecording()
            }

            var lastFft: FloatArray = FloatArray(0)

            // Blocks until stopRecording() is called (or coroutine is cancelled)
            val rawAudio = audioRecorder.startRecording { fftMagnitudes ->
                if (_uiState.value is AuraState.Listening) {
                    val frame = fftMagnitudes.take(60).toFloatArray()
                    lastFft = frame
                    _uiState.value = AuraState.Listening(frame)
                }
            }

            timeoutJob.cancel()

            rawAudio?.let { audioBytes ->
                if (audioBytes.isNotEmpty()) {
                    // Transition: Listening ──▶ Processing
                    _uiState.value = AuraState.Processing

                    // V2: calls the combined fingerprint + humming engine.
                    // The repository handles timeouts & errors internally,
                    // always returning a SongResult (never throws).
                    val result = IdentificationRepository.identifyAudio(audioBytes)

                    // Transition: Processing ──▶ Success
                    _uiState.value = AuraState.Success(result, lastFft)
                } else {
                    _uiState.value = AuraState.Idle
                }
            } ?: run {
                _uiState.value = AuraState.Idle
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Explanation screen toggle
    // ──────────────────────────────────────────────────────────────────────

    fun showExplanation() {
        val currentState = _uiState.value
        if (currentState is AuraState.Success) {
            _uiState.value = AuraState.Explanation(currentState.result, currentState.fftSnapshot)
        }
    }

    fun hideExplanation() {
        val currentState = _uiState.value
        if (currentState is AuraState.Explanation) {
            _uiState.value = AuraState.Success(currentState.result, currentState.fftSnapshot)
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    //  Central action‑button handler
    // ──────────────────────────────────────────────────────────────────────

    fun handleActionButtonClick() {
        when (_uiState.value) {
            is AuraState.Idle -> startListening()
            is AuraState.Listening -> audioRecorder.stopRecording()   // early stop → advances to Processing
            is AuraState.Success -> resetToIdle()
            else -> { /* Processing / Explanation — no-op on button press */ }
        }
    }

    fun resetToIdle() {
        _uiState.value = AuraState.Idle
        recordingJob?.cancel()
    }
}
