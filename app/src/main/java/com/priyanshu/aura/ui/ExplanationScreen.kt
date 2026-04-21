package com.priyanshu.aura.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priyanshu.aura.network.SongResult

@Composable
fun ExplanationScreen(result: SongResult, fftSnapshot: FloatArray, onBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "How Aura Works",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Section 1: The Raw Signal
            ExplanationCard(title = "1. Microphone Sampling") {
                Text(
                    text = "Aura captured exactly 5 seconds of raw audio from your device's microphone. We recorded this at 44,100 Hz as a pure 16-bit PCM signal. This produces millions of individual audio amplitude values representing the sound pressure wave.",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Section 2: The FFT Transformation with Unique Graph
            ExplanationCard(title = "2. Fast Fourier Transform (FFT)") {
                Text(
                    text = "Aura applied a mathematical algorithm called the Radix-2 Fast Fourier Transform to convert those raw time-based amplitude values into the frequency domain. Below is the ACTUAL unique frequency signature derived from your recording that identified '${result.title}':",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Draw the actual final FFT snapshot
                val primaryColor = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                        .padding(8.dp)
                ) {
                    if (fftSnapshot.isNotEmpty()) {
                        // We will plot the 60 bins as a connected waveform or detailed bar chart
                        val barWidth = size.width / fftSnapshot.size
                        val maxBarHeight = size.height

                        fftSnapshot.forEachIndexed { index, magnitude ->
                            val cleanMagnitude = (magnitude * 10).coerceIn(0f, 1f) // Amplification
                            val barHeight = cleanMagnitude * maxBarHeight
                            val x = index * barWidth
                            
                            drawRoundRect(
                                color = primaryColor,
                                topLeft = Offset(x, maxBarHeight - barHeight),
                                size = Size(maxOf(barWidth - 2, 2f), barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Section 3: The Fingerprint and Cloud Match
            ExplanationCard(title = "3. Hashing & Verification") {
                Text(
                    text = "The unique peaks from the frequency graph were condensed into an audio spectral fingerprint. Aura then securely calculated an HMAC-SHA1 signature and bundled the 5-second PCM array using a multipart form-data payload.\n\nACRCloud instantly matched your payload's fingerprint to their massive celestial database, returning a 100% positive match for:\n\n${result.title} by ${result.artist}!",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun ExplanationCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface, // Golden surface
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))
        content()
    }
}
