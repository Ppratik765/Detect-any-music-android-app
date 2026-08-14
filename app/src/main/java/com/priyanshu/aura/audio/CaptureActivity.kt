package com.priyanshu.aura.audio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 1001
    }

    private lateinit var mediaProjectionManager: MediaProjectionManager
    private val audioRecorder by lazy { AudioRecorder(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OrbStateHolder.updateState(OrbState.Selection)
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            AuraTheme {
                val orbState by OrbStateHolder.orbState.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    when (val state = orbState) {
                        is OrbState.Selection -> {
                            SelectionDialog(
                                onAroundYou = {
                                    startExternalAudio()
                                },
                                onOnDevice = {
                                    startActivityForResult(
                                        mediaProjectionManager.createScreenCaptureIntent(),
                                        REQUEST_MEDIA_PROJECTION
                                    )
                                },
                                onClose = { finish() }
                            )
                        }
                        is OrbState.Listening, is OrbState.Processing -> {
                            GlowingOrb(isProcessing = state is OrbState.Processing)
                        }
                        is OrbState.Success -> {
                            SuccessCard(state.result) {
                                finish()
                            }
                        }
                        is OrbState.Error -> {
                            ErrorCard(state.message) {
                                finish()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startExternalAudio() {
        OrbStateHolder.updateState(OrbState.Listening)
        lifecycleScope.launch {
            val audioBytes = audioRecorder.startRecording { _ -> }
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                OrbStateHolder.updateState(OrbState.Processing)
                val result = IdentificationRepository.identifyAudio(audioBytes)
                if (result.title != "Never Gonna Give You Up" && result.title != "Unknown Title") {
                    OrbStateHolder.updateState(OrbState.Success(result))
                } else {
                    OrbStateHolder.updateState(OrbState.Error("No match found"))
                }
            } else {
                OrbStateHolder.updateState(OrbState.Error("Failed to record audio"))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                OrbStateHolder.updateState(OrbState.Listening)
                val intent = Intent(this, InternalAudioService::class.java).apply {
                    putExtra("RESULT_CODE", resultCode)
                    putExtra("DATA", data)
                }
                startForegroundService(intent)
            } else {
                finish() // User denied projection
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioRecorder.stopRecording()
    }
}

@Composable
fun SelectionDialog(onAroundYou: () -> Unit, onOnDevice: () -> Unit, onClose: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Identify Music",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onAroundYou,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Option 1: Around You")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOnDevice,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.PhoneAndroid, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Option 2: On This Device")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onClose) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun GlowingOrb(isProcessing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val color by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        targetValue = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color, Color.Transparent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isProcessing) "Processing..." else "Listening...",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
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
