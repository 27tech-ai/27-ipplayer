package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels ORDER BY orderIndex ASC, name ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY orderIndex ASC, name ASC")
    fun getChannelsForPlaylist(playlistId: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY orderIndex ASC, name ASC")
    fun getFavoriteChannels(): Flow<List<Channel>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId ORDER BY groupTitle ASC")
    fun getCategoriesForPlaylist(playlistId: String): Flow<List<String>>

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId AND groupTitle = :groupTitle ORDER BY orderIndex ASC, name ASC")
    fun getChannelsByCategory(playlistId: String, groupTitle: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchChannels(query: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): Channel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: String, isFavorite: Boolean)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsForPlaylist(playlistId: String)

    @Query("DELETE FROM channels")
    suspend fun clearAll()
}
