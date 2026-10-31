package com.example.ui.viewmodel

import com.example.data.model.Channel
import com.example.data.model.Playlist

data class IptvUiState(
    val playlists: List<Playlist> = emptyList(),
    val selectedPlaylist: Playlist? = null,
    val channels: List<Channel> = emptyList(),
    val filteredChannels: List<Channel> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val currentPlayingChannel: Channel? = null,
    val isPlaybackActive: Boolean = false,
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val userMessage: String? = null,
    val isAddDialogOpen: Boolean = false
)
