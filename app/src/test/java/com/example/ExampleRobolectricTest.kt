package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.parser.M3UParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("27-ipplayer", appName)
    }

    @Test
    fun `test M3UParser parses attributes correctly`() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cnn" tvg-name="CNN Live" tvg-logo="https://example.com/logo.png" group-title="News",CNN News Channel
            http://stream.example.com/live/cnn.m3u8
        """.trimIndent()

        val channels = M3UParser.parse(m3u, "test_playlist")
        assertEquals(1, channels.size)

        val channel = channels[0]
        assertEquals("CNN News Channel", channel.name)
        assertEquals("http://stream.example.com/live/cnn.m3u8", channel.streamUrl)
        assertEquals("https://example.com/logo.png", channel.logoUrl)
        assertEquals("News", channel.groupTitle)
        assertEquals("cnn", channel.tvgId)
    }
}
