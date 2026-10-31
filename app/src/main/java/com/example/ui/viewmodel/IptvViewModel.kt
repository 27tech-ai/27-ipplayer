package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Channel
import com.example.data.model.Playlist
import com.example.data.repository.IptvRepository
import com.example.player.IptvPlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IptvViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = IptvRepository(application)
    val playerManager = IptvPlayerManager(application)

    private val _uiState = MutableStateFlow(IptvUiState(isLoading = true))
    val uiState: StateFlow<IptvUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultSamplePlaylist()
            observePlaylists()
        }
    }

    private fun observePlaylists() {
        viewModelScope.launch {
            repository.getAllPlaylists().collectLatest { playlists ->
                _uiState.update { current ->
                    val currentSelected = current.selectedPlaylist
                    val selected = if (currentSelected != null && playlists.any { it.id == currentSelected.id }) {
                        playlists.first { it.id == currentSelected.id }
                    } else {
                        playlists.firstOrNull()
                    }
                    current.copy(
                        playlists = playlists,
                        selectedPlaylist = selected,
                        isLoading = false
                    )
                }
                _uiState.value.selectedPlaylist?.let { observeChannelsForPlaylist(it.id) }
            }
        }
    }

    fun selectPlaylist(playlist: Playlist) {
        _uiState.update { it.copy(selectedPlaylist = playlist, selectedCategory = "All", searchQuery = "") }
        observeChannelsForPlaylist(playlist.id)
    }

    private fun observeChannelsForPlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.getChannelsForPlaylist(playlistId).collectLatest { channels ->
                val distinctCategories = listOf("All", "★ Favorites") +
                        channels.map { it.groupTitle }
                            .filter { it.isNotBlank() && it != "All" }
                            .distinct()
                            .sorted()

                _uiState.update { current ->
                    val filtered = applyFilter(channels, current.selectedCategory, current.searchQuery)
                    current.copy(
                        channels = channels,
                        filteredChannels = filtered,
                        categories = distinctCategories
                    )
                }
            }
        }
    }

    fun selectCategory(category: String) {
        _uiState.update { current ->
            val filtered = applyFilter(current.channels, category, current.searchQuery)
            current.copy(selectedCategory = category, filteredChannels = filtered)
        }
    }

    fun search(query: String) {
        _uiState.update { current ->
            val filtered = applyFilter(current.channels, current.selectedCategory, query)
            current.copy(searchQuery = query, filteredChannels = filtered)
        }
    }

    private fun applyFilter(channels: List<Channel>, category: String, query: String): List<Channel> {
        return channels.filter { channel ->
            val matchesCategory = when (category) {
                "All" -> true
                "★ Favorites" -> channel.isFavorite
                else -> channel.groupTitle.equals(category, ignoreCase = true)
            }
            val matchesQuery = query.isBlank() ||
                    channel.name.contains(query, ignoreCase = true) ||
                    channel.groupTitle.contains(query, ignoreCase = true)

            matchesCategory && matchesQuery
        }
    }

    fun playChannel(channel: Channel) {
        _uiState.update {
            it.copy(
                currentPlayingChannel = channel,
                isPlaybackActive = true
            )
        }
        playerManager.playStream(channel.streamUrl)
    }

    fun stopPlayback() {
        playerManager.stop()
        _uiState.update {
            it.copy(
                isPlaybackActive = false,
                currentPlayingChannel = null
            )
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch {
            repository.toggleFavorite(channel)
        }
    }

    fun importM3UUrl(name: String, url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, userMessage = null) }
            val result = repository.importM3UFromUrl(name, url)
            result.fold(
                onSuccess = { playlist ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            isAddDialogOpen = false,
                            userMessage = "Imported ${playlist.channelCount} channels from ${playlist.name}"
                        )
                    }
                    selectPlaylist(playlist)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userMessage = "Failed to import M3U: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun importM3UText(name: String, text: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, userMessage = null) }
            val result = repository.importM3UFromText(name, text)
            result.fold(
                onSuccess = { playlist ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            isAddDialogOpen = false,
                            userMessage = "Imported ${playlist.channelCount} channels"
                        )
                    }
                    selectPlaylist(playlist)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userMessage = "Failed to import M3U text: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun importXtream(name: String, serverUrl: String, username: String, pass: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, userMessage = null) }
            val result = repository.importXtreamCodes(name, serverUrl, username, pass)
            result.fold(
                onSuccess = { playlist ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            isAddDialogOpen = false,
                            userMessage = "Imported ${playlist.channelCount} channels from Xtream server"
                        )
                    }
                    selectPlaylist(playlist)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            userMessage = "Xtream connection error: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            _uiState.update { it.copy(userMessage = "Playlist deleted") }
        }
    }

    fun openAddPlaylistDialog() {
        _uiState.update { it.copy(isAddDialogOpen = true) }
    }

    fun closeAddPlaylistDialog() {
        _uiState.update { it.copy(isAddDialogOpen = false) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
