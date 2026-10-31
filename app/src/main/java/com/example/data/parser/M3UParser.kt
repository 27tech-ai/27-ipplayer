package com.example.data.parser

import com.example.data.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID
import java.util.regex.Pattern

object M3UParser {

    private val TVG_ID_REGEX = Pattern.compile("""tvg-id=["']?([^"'\s,]+)["']?""", Pattern.CASE_INSENSITIVE)
    private val TVG_NAME_REGEX = Pattern.compile("""tvg-name=["']?([^"']+)["']?""", Pattern.CASE_INSENSITIVE)
    private val TVG_LOGO_REGEX = Pattern.compile("""tvg-logo=["']?([^"'\s,]+)["']?""", Pattern.CASE_INSENSITIVE)
    private val GROUP_TITLE_REGEX = Pattern.compile("""group-title=["']?([^"']+)["']?""", Pattern.CASE_INSENSITIVE)

    /**
     * Parse M3U playlist from an input stream line by line to minimize memory footprint.
     */
    fun parse(inputStream: InputStream, playlistId: String): List<Channel> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        return parseReader(reader, playlistId)
    }

    /**
     * Parse M3U playlist from a raw string.
     */
    fun parse(content: String, playlistId: String): List<Channel> {
        val reader = BufferedReader(content.reader())
        return parseReader(reader, playlistId)
    }

    private fun parseReader(reader: BufferedReader, playlistId: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        var lineIndex = 0

        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentLogoUrl: String? = null
        var currentGroupTitle: String? = null
        var currentChannelName: String? = null
        var hasPendingExtInf = false

        reader.useLines { lines ->
            for (rawLine in lines) {
                var line = rawLine.trim()
                // Strip UTF-8 Byte Order Mark if present
                if (line.startsWith("\uFEFF")) {
                    line = line.substring(1).trim()
                }

                if (line.isEmpty()) continue

                if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                    hasPendingExtInf = true
                    currentTvgId = extractAttribute(line, TVG_ID_REGEX)
                    currentTvgName = extractAttribute(line, TVG_NAME_REGEX)
                    currentLogoUrl = extractAttribute(line, TVG_LOGO_REGEX)
                    currentGroupTitle = extractAttribute(line, GROUP_TITLE_REGEX)

                    // Channel title is after the comma
                    val commaIndex = line.lastIndexOf(',')
                    currentChannelName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                        line.substring(commaIndex + 1).trim()
                    } else {
                        currentTvgName ?: "Channel ${lineIndex + 1}"
                    }
                } else if (line.startsWith("#EXTGRP:", ignoreCase = true)) {
                    // Alternative group directive used in some IPTV streams
                    val group = line.substringAfter("#EXTGRP:").trim()
                    if (group.isNotEmpty()) {
                        currentGroupTitle = group
                    }
                } else if (line.startsWith("#")) {
                    // Ignore metadata directives such as #EXTM3U, #EXTVLCOPT, #KODIPROP
                    continue
                } else if (hasPendingExtInf && (line.startsWith("http://", ignoreCase = true) ||
                            line.startsWith("https://", ignoreCase = true) ||
                            line.startsWith("rtmp://", ignoreCase = true) ||
                            line.startsWith("rtsp://", ignoreCase = true) ||
                            line.startsWith("mms://", ignoreCase = true))) {

                    val name = currentChannelName?.ifBlank { null }
                        ?: currentTvgName?.ifBlank { null }
                        ?: "Channel ${lineIndex + 1}"

                    val group = currentGroupTitle?.ifBlank { null } ?: "All Channels"

                    val channel = Channel(
                        id = "${playlistId}_${lineIndex}_${UUID.randomUUID().toString().take(8)}",
                        playlistId = playlistId,
                        name = name,
                        streamUrl = line,
                        logoUrl = currentLogoUrl?.ifBlank { null },
                        groupTitle = group,
                        tvgId = currentTvgId,
                        tvgName = currentTvgName,
                        orderIndex = lineIndex
                    )

                    channels.add(channel)
                    lineIndex++

                    // Reset pending info
                    currentTvgId = null
                    currentTvgName = null
                    currentLogoUrl = null
                    currentGroupTitle = null
                    currentChannelName = null
                    hasPendingExtInf = false
                }
            }
        }

        return channels
    }

    private fun extractAttribute(line: String, pattern: Pattern): String? {
        val matcher = pattern.matcher(line)
        return if (matcher.find()) {
            matcher.group(1)?.trim()
        } else {
            null
        }
    }
}
