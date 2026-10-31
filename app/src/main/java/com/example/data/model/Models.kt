package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class Channel(
    @PrimaryKey
    val id: String,
    val playlistId: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String = "General",
    val tvgId: String? = null,
    val tvgName: String? = null,
    val isFavorite: Boolean = false,
    val orderIndex: Int = 0
)

enum class PlaylistType {
    M3U_URL,
    M3U_TEXT,
    XTREAM
}

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey
    val id: String,
    val name: String,
    val type: PlaylistType,
    val url: String? = null,
    val username: String? = null,
    val password: String? = null,
    val channelCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
