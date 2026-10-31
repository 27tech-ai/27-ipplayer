package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AddPlaylistDialog(
    isOpen: Boolean,
    isImporting: Boolean,
    onDismiss: () -> Unit,
    onImportM3UUrl: (name: String, url: String) -> Unit,
    onImportM3UText: (name: String, text: String) -> Unit,
    onImportXtream: (name: String, server: String, user: String, pass: String) -> Unit
) {
    if (!isOpen) return

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("M3U URL", "Xtream Codes", "Paste M3U")

    // M3U URL Fields
    var m3uName by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    // Xtream Fields
    var xtreamName by remember { mutableStateOf("") }
    var xtreamServer by remember { mutableStateOf("") }
    var xtreamUser by remember { mutableStateOf("") }
    var xtreamPassword by remember { mutableStateOf("") }

    // Raw Text Fields
    var rawName by remember { mutableStateOf("") }
    var rawText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add IPTV Playlist",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_add_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color(0xFF0F1218),
                    contentColor = Color(0xFF00E5FF),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = Color(0xFF00E5FF)
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    fontSize = 13.sp,
                                    color = if (selectedTabIndex == index) Color(0xFF00E5FF) else Color(0xFF9CA3AF)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF00E5FF),
                    unfocusedLabelColor = Color.Gray
                )

                when (selectedTabIndex) {
                    0 -> {
                        // M3U URL
                        OutlinedTextField(
                            value = m3uName,
                            onValueChange = { m3uName = it },
                            label = { Text("Playlist Name") },
                            placeholder = { Text("e.g. World Channels", color = Color.Gray) },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_m3u_name")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = m3uUrl,
                            onValueChange = { m3uUrl = it },
                            label = { Text("M3U / M3U8 URL") },
                            placeholder = { Text("http://example.com/playlist.m3u", color = Color.Gray) },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_m3u_url")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Public Stream Preset button
                        OutlinedButton(
                            onClick = {
                                m3uName = "IPTV-org Free Channels"
                                m3uUrl = "https://iptv-org.github.io/iptv/countries/us.m3u"
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00E5FF)),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF00E5FF).copy(alpha = 0.5f))
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("preset_iptv_org_btn")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF00E5FF))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Sample Free-To-Air Preset", fontSize = 12.sp)
                        }
                    }

                    1 -> {
                        // Xtream Codes
                        OutlinedTextField(
                            value = xtreamName,
                            onValueChange = { xtreamName = it },
                            label = { Text("Account Name (Optional)") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_xtream_name")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = xtreamServer,
                            onValueChange = { xtreamServer = it },
                            label = { Text("Server URL & Port") },
                            placeholder = { Text("http://provider.net:8080", color = Color.Gray) },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_xtream_server")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = xtreamUser,
                            onValueChange = { xtreamUser = it },
                            label = { Text("Username") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_xtream_user")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = xtreamPassword,
                            onValueChange = { xtreamPassword = it },
                            label = { Text("Password") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_xtream_pass")
                        )
                    }

                    2 -> {
                        // Paste M3U Raw Text
                        OutlinedTextField(
                            value = rawName,
                            onValueChange = { rawName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            colors = textFieldColors,
                            modifier = Modifier.fillMaxWidth().testTag("input_raw_name")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            label = { Text("M3U Content (#EXTM3U ...)") },
                            colors = textFieldColors,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 200.dp)
                                .testTag("input_raw_text")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            when (selectedTabIndex) {
                                0 -> onImportM3UUrl(m3uName, m3uUrl)
                                1 -> onImportXtream(xtreamName, xtreamServer, xtreamUser, xtreamPassword)
                                2 -> onImportM3UText(rawName, rawText)
                            }
                        },
                        enabled = !isImporting && when (selectedTabIndex) {
                            0 -> m3uUrl.isNotBlank()
                            1 -> xtreamServer.isNotBlank() && xtreamUser.isNotBlank() && xtreamPassword.isNotBlank()
                            2 -> rawText.isNotBlank()
                            else -> false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("submit_import_btn")
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(18.dp).width(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importing...")
                        } else {
                            Text("Save & Parse", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
