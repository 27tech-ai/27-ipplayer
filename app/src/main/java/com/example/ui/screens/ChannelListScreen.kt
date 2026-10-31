package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.Playlist
import com.example.ui.components.AddPlaylistDialog
import com.example.ui.components.ChannelCard
import com.example.ui.viewmodel.IptvUiState
import com.example.ui.viewmodel.IptvViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelListScreen(
    viewModel: IptvViewModel,
    uiState: IptvUiState
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isPlaylistMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        uiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        containerColor = Color(0xFF0C0E14),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF12151D),
                    titleContentColor = Color.White
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "27-ipplayer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = Color.White
                            )
                            uiState.selectedPlaylist?.let { pl ->
                                Text(
                                    text = "${pl.name} (${pl.channelCount} channels)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Playlist Switcher Menu
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isPlaylistMenuOpen = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .testTag("btn_switch_playlist")
                        ) {
                            Text(
                                text = "Playlists",
                                color = Color(0xFF00E5FF),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = "Playlists",
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = isPlaylistMenuOpen,
                            onDismissRequest = { isPlaylistMenuOpen = false },
                            modifier = Modifier.background(Color(0xFF1E222D))
                        ) {
                            uiState.playlists.forEach { playlist ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = playlist.name,
                                                color = if (playlist.id == uiState.selectedPlaylist?.id) Color(0xFF00E5FF) else Color.White,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${playlist.channelCount} channels • ${playlist.type}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                viewModel.deletePlaylist(playlist.id)
                                                isPlaylistMenuOpen = false
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectPlaylist(playlist)
                                        isPlaylistMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    // Add Playlist Button
                    IconButton(
                        onClick = { viewModel.openAddPlaylistDialog() },
                        modifier = Modifier.testTag("btn_add_playlist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Playlist",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddPlaylistDialog() },
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_playlist")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Playlist")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.search(it) },
                placeholder = { Text("Search channel or category...", color = Color.Gray) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.search("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF141720),
                    unfocusedContainerColor = Color(0xFF141720)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_channels_input")
            )

            // Category Chips Row (D-Pad remote friendly)
            if (uiState.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.categories) { category ->
                        CategoryChip(
                            title = category,
                            isSelected = category.equals(uiState.selectedCategory, ignoreCase = true),
                            onClick = { viewModel.selectCategory(category) }
                        )
                    }
                }
            }

            // Channel Content / Grid
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF00E5FF)
                        )
                    }
                    uiState.filteredChannels.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tv,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.channels.isEmpty()) "No channels in this playlist" else "No channels match '${uiState.searchQuery}'",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.openAddPlaylistDialog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00E5FF),
                                    contentColor = Color.Black
                                ),
                                modifier = Modifier.testTag("btn_empty_add")
                            ) {
                                Text("Add M3U / Xtream Playlist")
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize().testTag("channels_grid")
                        ) {
                            items(
                                items = uiState.filteredChannels,
                                key = { it.id }
                            ) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    onClick = { viewModel.playChannel(channel) },
                                    onToggleFavorite = { viewModel.toggleFavorite(channel) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Playlist Dialog
    AddPlaylistDialog(
        isOpen = uiState.isAddDialogOpen,
        isImporting = uiState.isImporting,
        onDismiss = { viewModel.closeAddPlaylistDialog() },
        onImportM3UUrl = { name, url -> viewModel.importM3UUrl(name, url) },
        onImportM3UText = { name, text -> viewModel.importM3UText(name, text) },
        onImportXtream = { name, srv, usr, pwd -> viewModel.importXtream(name, srv, usr, pwd) }
    )
}

@Composable
private fun CategoryChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale = if (isFocused) 1.08f else 1.0f
    val backgroundColor = when {
        isSelected -> Color(0xFF00E5FF)
        isFocused -> Color(0xFF1E2638)
        else -> Color(0xFF181C26)
    }
    val textColor = when {
        isSelected -> Color.Black
        isFocused -> Color(0xFF00E5FF)
        else -> Color(0xFFCBD5E1)
    }
    val borderColor = if (isFocused) Color(0xFF00E5FF) else Color.Transparent

    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("chip_$title")
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Normal
        )
    }
}
