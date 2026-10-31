package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.model.Channel
import com.example.data.model.Playlist
import com.example.data.model.PlaylistType
import com.example.data.network.NetworkClient
import com.example.data.network.XtreamCodesService
import com.example.data.parser.M3UParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException
import java.util.UUID

class IptvRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val channelDao = db.channelDao()
    private val playlistDao = db.playlistDao()
    private val xtreamService = XtreamCodesService()

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getChannelsForPlaylist(playlistId: String): Flow<List<Channel>> =
        channelDao.getChannelsForPlaylist(playlistId)

    fun getAllChannels(): Flow<List<Channel>> = channelDao.getAllChannels()

    fun getFavoriteChannels(): Flow<List<Channel>> = channelDao.getFavoriteChannels()

    fun getCategoriesForPlaylist(playlistId: String): Flow<List<String>> =
        channelDao.getCategoriesForPlaylist(playlistId)

    fun getChannelsByCategory(playlistId: String, category: String): Flow<List<Channel>> =
        channelDao.getChannelsByCategory(playlistId, category)

    fun searchChannels(query: String): Flow<List<Channel>> =
        channelDao.searchChannels(query)

    suspend fun toggleFavorite(channel: Channel) = withContext(Dispatchers.IO) {
        channelDao.setFavorite(channel.id, !channel.isFavorite)
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        channelDao.deleteChannelsForPlaylist(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    /**
     * Import an M3U/M3U8 playlist from a remote URL.
     */
    suspend fun importM3UFromUrl(name: String, url: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = UUID.randomUUID().toString()
            val request = Request.Builder().url(url).build()

            val channels = NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code} downloading playlist: ${response.message}")
                }
                val bodyStream = response.body?.byteStream()
                    ?: throw IOException("Empty playlist response body")
                M3UParser.parse(bodyStream, playlistId)
            }

            if (channels.isEmpty()) {
                throw IOException("No valid stream channels found in M3U playlist")
            }

            val playlist = Playlist(
                id = playlistId,
                name = name.ifBlank { "M3U Playlist" },
                type = PlaylistType.M3U_URL,
                url = url,
                channelCount = channels.size
            )

            playlistDao.insertPlaylist(playlist)
            channelDao.insertChannels(channels)

            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import an M3U/M3U8 playlist from raw text.
     */
    suspend fun importM3UFromText(name: String, text: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = UUID.randomUUID().toString()
            val channels = M3UParser.parse(text, playlistId)

            if (channels.isEmpty()) {
                throw IOException("No valid stream channels parsed from text")
            }

            val playlist = Playlist(
                id = playlistId,
                name = name.ifBlank { "Imported Playlist" },
                type = PlaylistType.M3U_TEXT,
                channelCount = channels.size
            )

            playlistDao.insertPlaylist(playlist)
            channelDao.insertChannels(channels)

            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import streams from Xtream Codes server.
     */
    suspend fun importXtreamCodes(
        name: String,
        serverUrl: String,
        username: String,
        password: String
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = UUID.randomUUID().toString()
            val channels = xtreamService.fetchChannels(
                playlistId = playlistId,
                serverUrl = serverUrl,
                user = username,
                pass = password
            )

            if (channels.isEmpty()) {
                throw IOException("No channels found on Xtream Codes server")
            }

            val playlist = Playlist(
                id = playlistId,
                name = name.ifBlank { "Xtream ($serverUrl)" },
                type = PlaylistType.XTREAM,
                url = serverUrl,
                username = username,
                password = password,
                channelCount = channels.size
            )

            playlistDao.insertPlaylist(playlist)
            channelDao.insertChannels(channels)

            Result.success(playlist)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Initialize sample Free-to-Air legal channels if database is completely empty.
     */
    suspend fun ensureDefaultSamplePlaylist() = withContext(Dispatchers.IO) {
        val playlists = playlistDao.getPlaylistById("sample_demo_playlist")
        if (playlists == null) {
            val sampleM3U = """
                #EXTM3U
                #EXTINF:-1 tvg-id="bloomberg" tvg-name="Bloomberg TV" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/Bloomberg_Television_logo.svg/512px-Bloomberg_Television_logo.svg.png" group-title="News",Bloomberg News Live
                https://liveproduseast.akamaized.net/us/Channel-News-sp-fast/playlist.m3u8
                #EXTINF:-1 tvg-id="pbs" tvg-name="PBS Kids" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/thumb/9/91/PBS_Kids_logo_2022.svg/512px-PBS_Kids_logo_2022.svg.png" group-title="Kids",PBS Kids Live
                https://pbskids.org/video/livestream.m3u8
                #EXTINF:-1 tvg-id="dw" tvg-name="DW English" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/thumb/7/75/Deutsche_Welle_symbol_2012.svg/512px-Deutsche_Welle_symbol_2012.svg.png" group-title="News",DW English HD
                https://dwamdstream102.akamaized.net/hls/live/2015525/dl_live_102/master.m3u8
                #EXTINF:-1 tvg-id="redbull" tvg-name="Red Bull TV" tvg-logo="https://upload.wikimedia.org/wikipedia/en/thumb/e/eb/Red_Bull_TV_logo.svg/512px-Red_Bull_TV_logo.svg.png" group-title="Sports",Red Bull TV Action
                https://rbmn-live.akamaized.net/hls/live/590964/BoRB-AT/master.m3u8
                #EXTINF:-1 tvg-id="france24" tvg-name="France 24 EN" tvg-logo="https://upload.wikimedia.org/wikipedia/en/thumb/6/65/France_24_logo.svg/512px-France_24_logo.svg.png" group-title="News",France 24 English
                https://stream.france24.com/hls/live/2037568/F24_EN_LO_HLS/master.m3u8
                #EXTINF:-1 tvg-id="euronews" tvg-name="Euronews" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/thumb/0/07/Euronews_2016_logo.svg/512px-Euronews_2016_logo.svg.png" group-title="News",Euronews English
                https://euronews-euronews-world-1-au.samsung.wurl.tv/playlist.m3u8
                #EXTINF:-1 tvg-id="nasa" tvg-name="NASA TV" tvg-logo="https://upload.wikimedia.org/wikipedia/commons/thumb/e/e5/NASA_logo.svg/512px-NASA_logo.svg.png" group-title="Science",NASA TV HD
                https://ntv1.akamaized.net/hls/live/2014075/NASA-NTV1-HLS/master.m3u8
                #EXTINF:-1 tvg-id="bbb_hls" tvg-name="Test HLS 1080p" tvg-logo="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg" group-title="Test Streams",Big Buck Bunny (HLS 1080p)
                https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
                #EXTINF:-1 tvg-id="tears_dash" tvg-name="Test DASH" tvg-logo="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg" group-title="Test Streams",Tears of Steel (DASH Stream)
                https://dash.akamaized.net/dash2go/contents/testres/60fps/prog_index.mpd
                #EXTINF:-1 tvg-id="sintel" tvg-name="Sintel Stream" tvg-logo="https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg" group-title="Movies",Sintel Open Movie HD
                https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4
            """.trimIndent()

            val playlistId = "sample_demo_playlist"
            val channels = M3UParser.parse(sampleM3U, playlistId)
            val demoPlaylist = Playlist(
                id = playlistId,
                name = "Public IPTV Demo",
                type = PlaylistType.M3U_TEXT,
                channelCount = channels.size
            )
            playlistDao.insertPlaylist(demoPlaylist)
            channelDao.insertChannels(channels)
        }
    }
}
