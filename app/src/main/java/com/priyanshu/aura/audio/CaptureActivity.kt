package com.priyanshu.aura.audio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColor
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.priyanshu.aura.network.IdentificationRepository
import com.priyanshu.aura.ui.theme.AuraTheme
import com.priyanshu.aura.viewmodel.OrbState
import com.priyanshu.aura.viewmodel.OrbStateHolder
import kotlinx.coroutines.launch

class CaptureActivity : ComponentActivity() {

    private val audioRecorder by lazy { AudioRecorder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OrbStateHolder.updateState(OrbState.Listening)

        setContent {
            AuraTheme {
                val orbState by OrbStateHolder.orbState.collectAsState()
                val hapticFeedback = LocalHapticFeedback.current
                
                LaunchedEffect(Unit) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    startExternalAudio()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = orbState) {
                        is OrbState.Selection -> {
                            GlowingOrb(orbState = state)
                        }
                        is OrbState.Listening, is OrbState.Processing -> {
                            GlowingOrb(orbState = state)
                        }
                        is OrbState.Success -> {
                            // Should not be reached visually since we redirect, but handle just in case
                        }
                        is OrbState.Error -> {
                            LaunchedEffect(state.message) {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            ErrorCard(state.message) {
                                finish()
                            }
                        }
                    }

                    if (orbState !is OrbState.Success && orbState !is OrbState.Error) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                            IconButton(onClick = { 
                                val intent = Intent(this@CaptureActivity, com.priyanshu.aura.MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.List, contentDescription = "History", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startExternalAudio() {
        OrbStateHolder.updateState(OrbState.Selection) // Idle state initially
        lifecycleScope.launch {
            var audioDetected = false
            var timeoutJob: kotlinx.coroutines.Job? = null
            
            val audioBytes = audioRecorder.startRecording { fftData ->
                if (!audioDetected) {
                    // Check if there is significant audio (VAD)
                    val sum = fftData.sum()
                    if (sum > 25f) { // Threshold for speech/music
                        audioDetected = true
                        OrbStateHolder.updateState(OrbState.Listening)
                        
                        // Automatically stop recording after 10 seconds of actual audio
                        timeoutJob = launch {
                            kotlinx.coroutines.delay(10_000)
                            audioRecorder.stopRecording()
                        }
                    }
                    audioDetected // Return true to start saving only when audio is detected
                } else {
                    true // Continue saving
                }
            }
            
            timeoutJob?.cancel()
            
            if (audioBytes != null && audioBytes.isNotEmpty() && audioDetected) {
                OrbStateHolder.updateState(OrbState.Processing)
                val result = IdentificationRepository.identifyAudio(audioBytes)
                if (result.title != "Never Gonna Give You Up" && result.title != "Unknown Title") {
                    // Redirect back to Aura App with the result
                    val intent = Intent(this@CaptureActivity, com.priyanshu.aura.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("EXTRA_TITLE", result.title)
                        putExtra("EXTRA_ARTIST", result.artist)
                        putExtra("EXTRA_ALBUM_COVER_URL", result.albumCoverUrl)
                        putExtra("EXTRA_SPOTIFY_ID", result.spotifyId)
                        putExtra("EXTRA_YOUTUBE_ID", result.youtubeId)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    OrbStateHolder.updateState(OrbState.Error("No match found"))
                }
            } else {
                OrbStateHolder.updateState(OrbState.Error("Failed to record audio"))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stopRecording()
    }
}

@Composable
fun GlowingOrb(orbState: OrbState) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbTransition")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )

    val color by infiniteTransition.animateColor(
        initialValue = Color(0xFF6C5CE7).copy(alpha = 0.4f),
        targetValue = Color(0xFF74B9FF).copy(alpha = 0.8f),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbColor"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Massive soft glow
        Box(
            modifier = Modifier
                .size(350.dp)
                .scale(scale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color, Color.Transparent)
                    )
                )
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Waveform
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 5 bars animating
                listOf(0, 1, 2, 3, 4).forEach { index ->
                    val height by infiniteTransition.animateFloat(
                        initialValue = if (index % 2 == 0) 24f else 48f,
                        targetValue = if (index % 2 == 0) 64f else 32f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(300 + index * 100, easing = FastOutLinearInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "barHeight"
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(height.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val text = when (orbState) {
                is OrbState.Processing -> "Processing..."
                is OrbState.Listening -> "Listening..."
                else -> "Play, sing or hum a song..."
            }
            Text(
                text = text,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun SuccessCard(result: com.priyanshu.aura.network.SongResult, onClose: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable { onClose() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = result.albumCoverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = result.artist,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tap to close", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ErrorCard(message: String, onClose: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .clickable { onClose() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Tap to close", color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}
