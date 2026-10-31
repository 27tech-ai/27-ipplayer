package com.example.data.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

object NetworkClient {

    const val USER_AGENT = "27-ipplayer/1.0 (Linux; Android TV; ExoPlayer)"

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
}
