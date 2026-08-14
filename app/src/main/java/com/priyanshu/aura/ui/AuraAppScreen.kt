package com.priyanshu.aura.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.priyanshu.aura.network.LyricsRepository
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.priyanshu.aura.network.SongResult
import com.priyanshu.aura.viewmodel.AuraState
import com.priyanshu.aura.viewmodel.AuraViewModel

@Composable
fun AuraAppScreen(viewModel: AuraViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPassportFoldable = configuration.screenWidthDp >= 600 && configuration.screenHeightDp >= 600
    
    // Detect exceptionally small screens (like Galaxy Z Flip cover display)
    val isCompactHeight = configuration.screenHeightDp < 480
    val isCompactWidth = configuration.screenWidthDp < 360
    
    val hapticFeedback = LocalHapticFeedback.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    if (showHistory) {
        HistoryScreen(viewModel = viewModel, onBack = { showHistory = false })
        return
    }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }




    val paddingEnd by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (state is AuraState.Success && isPassportFoldable) (configuration.screenWidthDp / 2f).dp else 0.dp,
        animationSpec = tween(500),
        label = "mainContentPadding"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
            // Top Right Icons
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = { showHistory = true }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.List, contentDescription = "History", tint = MaterialTheme.colorScheme.onBackground)
                }
            }

        // Main interaction area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = paddingEnd),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // Header
                Text(
                    text = "A U R A",
                    fontSize = if (isCompactHeight) 24.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = 8.sp
                )

                Spacer(modifier = Modifier.height(if (isCompactHeight) 24.dp else 80.dp))

                // Main interaction area
                Box(
                    modifier = Modifier
                        .size(if (isCompactHeight) 200.dp else 300.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsating Ripple
                    if (state is AuraState.Listening) {
                        RippleEffect()
                    }

                    // Central Button
                    Box(
                        modifier = Modifier
                            .size(if (isCompactHeight) 80.dp else 120.dp)
                            .clip(CircleShape)
                            .background(if (state is AuraState.Listening) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else Color.White)
                            .clickable {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (state is AuraState.Idle) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        viewModel.handleActionButtonClick()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                } else {
                                    viewModel.handleActionButtonClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val isListening = state is AuraState.Listening
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Listen",
                            tint = if (isListening) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(if (isCompactHeight) 40.dp else 64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else 40.dp))

                // State specific content
                AnimatedVisibility(
                    visible = state is AuraState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Tap to listen",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                AnimatedVisibility(
                    visible = state is AuraState.Listening,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Listening...",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        val fftData = (state as? AuraState.Listening)?.fftData ?: FloatArray(0)
                        AudioVisualizer(fftData)
                    }
                }

                AnimatedVisibility(
                    visible = state is AuraState.Processing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    SkeletonLoader()
                }
            }
        }

        // Result Sheet
        val enterAnimation = if (isPassportFoldable) slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500)) + fadeIn(tween(500)) else slideInVertically(initialOffsetY = { it }, animationSpec = tween(500)) + fadeIn(tween(500))
        val exitAnimation = if (isPassportFoldable) slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(tween(300)) else slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(tween(300))

        AnimatedVisibility(
            visible = state is AuraState.Success,
            enter = enterAnimation,
            exit = exitAnimation,
            modifier = Modifier.align(if (isPassportFoldable) Alignment.CenterEnd else Alignment.BottomCenter)
        ) {
            val result = (state as? AuraState.Success)?.result
            if (result != null) {
                LaunchedEffect(result) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) // Success tactile feedback
                }
                ResultBottomSheet(
                    result = result,
                    isPassportFoldable = isPassportFoldable,
                    onClose = { viewModel.resetToIdle() },
                    onExplain = { viewModel.showExplanation() }
                )
            }
        }

        // Explanation Full Screen
        AnimatedVisibility(
            visible = state is AuraState.Explanation,
            enter = fadeIn(tween(400)),
            exit = fadeOut(tween(300)),
            modifier = Modifier.fillMaxSize()
        ) {
            val stateData = state as? AuraState.Explanation
            if (stateData != null) {
                ExplanationScreen(
                    result = stateData.result,
                    fftSnapshot = stateData.fftSnapshot,
                    onBack = { viewModel.hideExplanation() }
                )
            }
        }
    }
}

@Composable
fun RippleEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "ripple")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
    )
}

@Composable
fun AudioVisualizer(fftData: FloatArray) {
    val hapticFeedback = LocalHapticFeedback.current
    
    LaunchedEffect(fftData) {
        val sum = fftData.sum()
        if (sum > 40f) { // Threshold for a noticeable beat/audio level
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Canvas(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(0.8f)
            .height(100.dp)
    ) {
        val barWidth = size.width / (fftData.size.coerceAtLeast(1) * 2)
        val maxBarHeight = size.height

        fftData.forEachIndexed { index, magnitude ->
            val cleanMagnitude = (magnitude * 5).coerceIn(0f, 1f) // Amplification factor for visuals
            val barHeight = cleanMagnitude * maxBarHeight
            val x = index * 2 * barWidth + barWidth / 2

            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(x, maxBarHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth/2, barWidth/2)
            )
        }
    }
}

@Composable
fun SkeletonLoader() {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(180.dp, 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .size(120.dp, 16.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = alpha))
        )
    }
}

@Composable
fun ResultBottomSheet(result: SongResult, isPassportFoldable: Boolean, onClose: () -> Unit, onExplain: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 480
    val isCompactWidth = configuration.screenWidthDp < 360

    var lyricsText by remember { mutableStateOf<String?>(null) }
    var isLyricsLoading by remember { mutableStateOf(true) }

    LaunchedEffect(result) {
        lyricsText = LyricsRepository.getLyrics(result.title, result.artist)
        isLyricsLoading = false
    }

    val maxPortraitHeight = (configuration.screenHeightDp * 0.72f).dp
    val sheetModifier = if (isPassportFoldable) {
        Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.5f)
    } else {
        Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .height(maxPortraitHeight)
    }

    val sheetShape = if (isPassportFoldable) {
        RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp)
    } else {
        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    }

    Surface(
        modifier = sheetModifier,
        shape = sheetShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shadowElevation = 24.dp,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(if (isCompactWidth) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "How this works?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onExplain() }
                        .padding(8.dp)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Result Song Album Art Thumbnail
            Box(
                modifier = Modifier
                    .size(if (isCompactHeight) 90.dp else 130.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(com.priyanshu.aura.ui.theme.AuraCoverBg),
                contentAlignment = Alignment.Center
            ) {
                if (!result.albumCoverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(result.albumCoverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = result.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = result.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = result.artist,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Smart Spotify Button
                Button(
                    onClick = {
                        val intent = if (result.spotifyId != null) {
                            // Direct link if ID exists
                            Intent(Intent.ACTION_VIEW, Uri.parse("spotify:track:${result.spotifyId}"))
                        } else {
                            // Fallback: Search Spotify for Title + Artist
                            val query = Uri.encode("${result.title} ${result.artist}")
                            Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$query"))
                        }

                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // If Spotify app isn't installed, open in browser
                            val webQuery = Uri.encode("${result.title} ${result.artist}")
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/$webQuery")))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1DB954), contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (result.spotifyId != null) "Spotify" else "Search Spotify", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Smart YouTube Button
                Button(
                    onClick = {
                        val intent = if (result.youtubeId != null) {
                            // Direct link if ID exists
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${result.youtubeId}"))
                        } else {
                            // Fallback: Search YouTube for Title + Artist
                            val query = Uri.encode("${result.title} ${result.artist}")
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                        }

                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000), contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (result.youtubeId != null) "YouTube" else "Search YouTube", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lyrics Section
            Text(
                text = "Lyrics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            if (isLyricsLoading) {
                Text(
                    text = "Loading lyrics...",
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            } else if (lyricsText.isNullOrBlank()) {
                Text(
                    text = "No lyrics found for this song.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            } else {
                Text(
                    text = lyricsText ?: "",
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}