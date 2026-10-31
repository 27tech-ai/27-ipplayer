package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ChannelListScreen
import com.example.ui.screens.PlaybackScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.IptvViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0C0E14)),
                    color = Color(0xFF0C0E14)
                ) {
                    IptvApp()
                }
            }
        }
    }
}

@Composable
fun IptvApp(viewModel: IptvViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        val playingChannel = uiState.currentPlayingChannel
        if (uiState.isPlaybackActive && playingChannel != null) {
            // STRICT REQUIREMENT 4:
            // "During channel playback, the screen MUST ONLY display:
            // Play, Pause, Stop buttons, and the Channel Icon/Logo.
            // NO text, NO extra metadata, NO background clutter.
            // If a UI element is not Play/Pause/Stop/Icon, DO NOT RENDER IT."
            PlaybackScreen(
                channel = playingChannel,
                playerManager = viewModel.playerManager,
                onStop = { viewModel.stopPlayback() }
            )
        } else {
            ChannelListScreen(
                viewModel = viewModel,
                uiState = uiState
            )
        }
    }
}
