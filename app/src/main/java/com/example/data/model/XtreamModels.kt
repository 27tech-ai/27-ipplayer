package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class XtreamUserInfo(
    @Json(name = "username") val username: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "auth") val auth: Int? = null,
    @Json(name = "exp_date") val expDate: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamServerInfo(
    @Json(name = "url") val url: String? = null,
    @Json(name = "port") val port: String? = null,
    @Json(name = "https_port") val httpsPort: String? = null,
    @Json(name = "server_protocol") val serverProtocol: String? = null
)

@JsonClass(generateAdapter = true)
data class XtreamAuthResponse(
    @Json(name = "user_info") val userInfo: XtreamUserInfo? = null,
    @Json(name = "server_info") val serverInfo: XtreamServerInfo? = null
)

@JsonClass(generateAdapter = true)
data class XtreamCategory(
    @Json(name = "category_id") val categoryId: String,
    @Json(name = "category_name") val categoryName: String
)

@JsonClass(generateAdapter = true)
data class XtreamLiveStream(
    @Json(name = "stream_id") val streamId: Any, // Can be int or string in some servers
    @Json(name = "num") val num: Any? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "stream_icon") val streamIcon: String? = null,
    @Json(name = "epg_channel_id") val epgChannelId: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "container_extension") val containerExtension: String? = "ts"
)
