package com.example.data.network

import com.example.data.model.Channel
import com.example.data.model.XtreamAuthResponse
import com.example.data.model.XtreamCategory
import com.example.data.model.XtreamLiveStream
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

class XtreamCodesService {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authAdapter = moshi.adapter(XtreamAuthResponse::class.java)
    private val categoriesListAdapter = moshi.adapter<List<XtreamCategory>>(
        Types.newParameterizedType(List::class.java, XtreamCategory::class.java)
    )
    private val streamsListAdapter = moshi.adapter<List<XtreamLiveStream>>(
        Types.newParameterizedType(List::class.java, XtreamLiveStream::class.java)
    )

    /**
     * Authenticate and verify Xtream Codes credentials.
     */
    suspend fun authenticate(
        serverUrl: String,
        user: String,
        pass: String
    ): Boolean = withContext(Dispatchers.IO) {
        val base = sanitizeUrl(serverUrl)
        val url = "$base/player_api.php?username=$user&password=$pass"
        val request = Request.Builder().url(url).build()

        NetworkClient.okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
            val body = response.body?.string() ?: return@withContext false
            val auth = authAdapter.fromJson(body)
            auth?.userInfo?.auth == 1 || auth?.userInfo?.status.equals("Active", ignoreCase = true)
        }
    }

    /**
     * Fetch all live streams and categories from Xtream Codes server and map into Channels.
     */
    suspend fun fetchChannels(
        playlistId: String,
        serverUrl: String,
        user: String,
        pass: String
    ): List<Channel> = withContext(Dispatchers.IO) {
        val base = sanitizeUrl(serverUrl)

        // 1. Fetch categories
        val categoriesMap = mutableMapOf<String, String>()
        try {
            val catUrl = "$base/player_api.php?username=$user&password=$pass&action=get_live_categories"
            val catRequest = Request.Builder().url(catUrl).build()
            NetworkClient.okHttpClient.newCall(catRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val categories = categoriesListAdapter.fromJson(body)
                        categories?.forEach { cat ->
                            categoriesMap[cat.categoryId] = cat.categoryName
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Non-fatal, fallback to default categories
        }

        // 2. Fetch live streams
        val streamsUrl = "$base/player_api.php?username=$user&password=$pass&action=get_live_streams"
        val request = Request.Builder().url(streamsUrl).build()
        val channels = mutableListOf<Channel>()

        NetworkClient.okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Xtream Codes returned HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response from Xtream Codes")
            val streams = streamsListAdapter.fromJson(body) ?: emptyList()

            streams.forEachIndexed { index, stream ->
                val streamIdStr = stream.streamId.toString()
                val ext = stream.containerExtension?.ifBlank { "ts" } ?: "ts"
                val streamUrl = "$base/live/$user/$pass/$streamIdStr.$ext"
                val categoryName = stream.categoryId?.let { categoriesMap[it] } ?: "Live TV"
                val name = stream.name?.ifBlank { null } ?: "Stream $streamIdStr"

                channels.add(
                    Channel(
                        id = "${playlistId}_xtream_$streamIdStr",
                        playlistId = playlistId,
                        name = name,
                        streamUrl = streamUrl,
                        logoUrl = stream.streamIcon?.ifBlank { null },
                        groupTitle = categoryName,
                        tvgId = stream.epgChannelId,
                        tvgName = stream.name,
                        orderIndex = index
                    )
                )
            }
        }

        channels
    }

    private fun sanitizeUrl(url: String): String {
        var trimmed = url.trim()
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            trimmed = "http://$trimmed"
        }
        return trimmed.trimEnd('/')
    }
}
