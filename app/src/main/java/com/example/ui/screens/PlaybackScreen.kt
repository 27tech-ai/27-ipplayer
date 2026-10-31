package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Channel
import com.example.player.IptvPlayerManager
import kotlinx.coroutines.delay

/**
 * STRICT UI/UX REQUIREMENT:
 * During channel playback, the screen MUST ONLY display:
 * Play, Pause, Stop buttons, and the Channel Icon/Logo.
 * NO text, NO extra metadata, NO background clutter.
 * If a UI element is not Play/Pause/Stop/Icon, DO NOT RENDER IT.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlaybackScreen(
    channel: Channel,
    playerManager: IptvPlayerManager,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val playerState by playerManager.playerState.collectAsState()

    // Controls visibility state: tap on mobile or D-pad interaction toggles/resets timer
    var controlsVisible by remember { mutableStateOf(true) }
    val playFocusRequester = remember { FocusRequester() }

    BackHandler {
        onStop()
    }

    // Auto-hide controls after 6 seconds of inactivity to keep screen ultra-minimalist
    LaunchedEffect(controlsVisible, playerState.isPlaying) {
        if (controlsVisible && playerState.isPlaying) {
            delay(6000)
            controlsVisible = false
        }
    }

    // Request initial focus for D-Pad / Android TV remote navigation
    LaunchedEffect(Unit) {
        delay(300)
        try {
            playFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown) {
                    controlsVisible = true
                    when (keyEvent.key) {
                        Key.MediaPlay -> {
                            playerManager.play()
                            true
                        }
                        Key.MediaPause -> {
                            playerManager.pause()
                            true
                        }
                        Key.MediaStop -> {
                            onStop()
                            true
                        }
                        Key.MediaPlayPause -> {
                            if (playerState.isPlaying) playerManager.pause() else playerManager.play()
                            true
                        }
                        Key.Back, Key.Escape -> {
                            onStop()
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                controlsVisible = !controlsVisible
            }
    ) {
        // 1. Fullscreen Video Surface using Media3 PlayerView with no default controller
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    useController = false
                    player = playerManager.exoPlayer
                    keepScreenOn = true
                }
            },
            update = { playerView ->
                playerView.player = playerManager.exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Channel Icon / Logo (Top Start) - NO text, ONLY the channel logo
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(28.dp)
                .size(76.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f))
                .border(2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!channel.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(channel.logoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Channel Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .testTag("channel_logo_image")
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = "Channel Logo",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Buffering indicator (subtle non-text spinner strictly while loading stream)
        if (playerState.isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp),
                color = Color(0xFF00E5FF),
                strokeWidth = 3.dp
            )
        }

        // 3. Play, Pause, Stop Buttons - STRICT: ONLY Play, Pause, Stop buttons
        AnimatedVisibility(
            visible = controlsVisible || !playerState.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Button
                MinimalistControlButton(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    testTag = "btn_play",
                    focusRequester = playFocusRequester,
                    isHighlightActive = playerState.isPlaying,
                    onClick = {
                        controlsVisible = true
                        playerManager.play()
                    }
                )

                // Pause Button
                MinimalistControlButton(
                    icon = Icons.Default.Pause,
                    contentDescription = "Pause",
                    testTag = "btn_pause",
                    isHighlightActive = !playerState.isPlaying,
                    onClick = {
                        controlsVisible = true
                        playerManager.pause()
                    }
                )

                // Stop Button
                MinimalistControlButton(
                    icon = Icons.Default.Stop,
                    contentDescription = "Stop",
                    testTag = "btn_stop",
                    isHighlightActive = false,
                    onClick = {
                        onStop()
                    }
                )
            }
        }
    }
}

/**
 * Minimalist Play / Pause / Stop button with D-pad focus glow and touch response.
 * STRICT: NO text labels, strictly an icon inside an accessible, focusable circle.
 */
@Composable
private fun MinimalistControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isHighlightActive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale = if (isFocused) 1.22f else 1.0f
    val borderColor = when {
        isFocused -> Color(0xFF00E5FF)
        isHighlightActive -> Color(0xFF00E5FF).copy(alpha = 0.7f)
        else -> Color.White.copy(alpha = 0.25f)
    }
    val backgroundColor = when {
        isFocused -> Color(0xFF00E5FF).copy(alpha = 0.35f)
        isHighlightActive -> Color(0xFF00E5FF).copy(alpha = 0.2f)
        else -> Color.Black.copy(alpha = 0.7f)
    }
    val iconTint = when {
        isFocused -> Color(0xFF00E5FF)
        isHighlightActive -> Color(0xFF00E5FF)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .shadow(if (isFocused) 16.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(if (isFocused) 3.dp else 1.5.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(32.dp)
        )
    }
}
