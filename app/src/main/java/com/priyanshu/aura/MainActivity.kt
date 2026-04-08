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
        setContent {
            AuraTheme {
                AuraAppScreen(viewModel = auraViewModel)
            }
        }
    }
}