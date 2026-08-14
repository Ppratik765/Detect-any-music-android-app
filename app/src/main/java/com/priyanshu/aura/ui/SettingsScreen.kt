package com.priyanshu.aura.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.priyanshu.aura.audio.AmbientListeningService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("AuraPrefs", Context.MODE_PRIVATE)
    
    var autoAuraEnabled by remember { 
        mutableStateOf(prefs.getBoolean("auto_aura", false)) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Aura (Ambient Mode)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Silently identifies and logs songs playing around you in the background.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(
                    checked = autoAuraEnabled,
                    onCheckedChange = { enabled ->
                        autoAuraEnabled = enabled
                        prefs.edit().putBoolean("auto_aura", enabled).apply()
                        
                        val intent = Intent(context, AmbientListeningService::class.java)
                        if (enabled) {
                            context.startForegroundService(intent)
                        } else {
                            context.stopService(intent)
                        }
                    }
                )
            }
        }
    }
}
