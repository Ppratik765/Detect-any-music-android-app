package com.priyanshu.aura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.priyanshu.aura.ui.AuraAppScreen
import com.priyanshu.aura.ui.theme.AuraTheme
import com.priyanshu.aura.viewmodel.AuraViewModel

class MainActivity : ComponentActivity() {

    private val auraViewModel: AuraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)
        
        setContent {
            AuraTheme {
                AuraAppScreen(viewModel = auraViewModel)
            }
        }
    }
    
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: android.content.Intent?) {
        val title = intent?.getStringExtra("EXTRA_TITLE")
        if (title != null) {
            val artist = intent.getStringExtra("EXTRA_ARTIST") ?: ""
            val albumCoverUrl = intent.getStringExtra("EXTRA_ALBUM_COVER_URL")
            val spotifyId = intent.getStringExtra("EXTRA_SPOTIFY_ID")
            val youtubeId = intent.getStringExtra("EXTRA_YOUTUBE_ID")
            
            val result = com.priyanshu.aura.network.SongResult(
                title = title,
                artist = artist,
                albumCoverUrl = albumCoverUrl,
                spotifyId = spotifyId,
                youtubeId = youtubeId
            )
            auraViewModel.setResultFromIntent(result)
        }
    }
}