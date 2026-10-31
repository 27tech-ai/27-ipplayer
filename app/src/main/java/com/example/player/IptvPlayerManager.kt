package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import com.example.data.network.NetworkClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerUiState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(UnstableApi::class)
class IptvPlayerManager(context: Context) {

    private val appContext = context.applicationContext

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    // OkHttp DataSource for custom headers and reliable streaming
    private val dataSourceFactory = OkHttpDataSource.Factory(NetworkClient.okHttpClient)

    // Robust extractors for HLS, DASH, and TS (MPEG-TS) streams
    private val extractorsFactory = DefaultExtractorsFactory().apply {
        setConstantBitrateSeekingEnabled(true)
    }

    private val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

    val exoPlayer: ExoPlayer = ExoPlayer.Builder(appContext)
        .setMediaSourceFactory(mediaSourceFactory)
        .setAudioAttributes(AudioAttributes.DEFAULT, true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setHandleAudioBecomingNoisy(true)
        .build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = isPlaying,
                        isBuffering = playbackState == Player.STATE_BUFFERING
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val isBuffering = playbackState == Player.STATE_BUFFERING
                    val isPlaying = isPlaying
                    _playerState.value = _playerState.value.copy(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        hasError = false,
                        errorMessage = null
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    _playerState.value = _playerState.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        hasError = true,
                        errorMessage = error.localizedMessage ?: "Playback error"
                    )
                }
            })
        }

    /**
     * Start playback of stream URL. Detects HLS, DASH, TS, or Progressive formats.
     */
    fun playStream(streamUrl: String) {
        val uri = Uri.parse(streamUrl.trim())
        val mediaItemBuilder = MediaItem.Builder().setUri(uri)

        val path = uri.path?.lowercase() ?: ""
        when {
            path.endsWith(".m3u8") || streamUrl.contains("m3u8", ignoreCase = true) -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            path.endsWith(".mpd") || streamUrl.contains("dash", ignoreCase = true) -> {
                mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_MPD)
            }
            path.endsWith(".ts") || streamUrl.contains(".ts", ignoreCase = true) -> {
                mediaItemBuilder.setMimeType(MimeTypes.VIDEO_MP2T)
            }
        }

        val mediaItem = mediaItemBuilder.build()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun play() {
        if (exoPlayer.playbackState == Player.STATE_IDLE) {
            exoPlayer.prepare()
        }
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun stop() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playerState.value = PlayerUiState(isPlaying = false, isBuffering = false)
    }

    fun release() {
        exoPlayer.release()
    }
}
