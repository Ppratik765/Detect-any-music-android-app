package com.priyanshu.aura.viewmodel

import com.priyanshu.aura.network.SongResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class OrbState {
    object Selection : OrbState()
    object Listening : OrbState()
    object Processing : OrbState()
    data class Success(val result: SongResult) : OrbState()
    data class Error(val message: String) : OrbState()
}

object OrbStateHolder {
    private val _orbState = MutableStateFlow<OrbState>(OrbState.Selection)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    fun updateState(state: OrbState) {
        _orbState.value = state
    }
}
