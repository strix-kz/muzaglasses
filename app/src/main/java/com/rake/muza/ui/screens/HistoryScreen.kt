package com.rake.muza.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.rake.muza.core.LanguageManager
import com.rake.muza.data.local.PhotoHistoryEntity
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors
import java.io.File

@Composable
fun HistoryScreen(
    allPhotos: List<PhotoHistoryEntity>,
    savedPhotos: List<PhotoHistoryEntity>,
    onToggleFavorite: (PhotoHistoryEntity) -> Unit,
    onDeletePhoto: (PhotoHistoryEntity) -> Unit,
) {
    val context = LocalContext.current
    val strings by LanguageManager.strings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = History, 1 = Saved
    var searchQuery by remember { mutableStateOf("") }
    var inspectedPhoto by remember { mutableStateOf<PhotoHistoryEntity?>(null) }

    val baseList = if (selectedTab == 0) allPhotos else savedPhotos
    val displayedPhotos = remember(baseList, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) baseList
        else baseList.filter {
            it.city.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.dateLabel.lowercase().contains(q) ||
                it.glassesModel.lowercase().contains(q)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .testTag("history_screen"),
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Top Bar: Persistent Top-Left Logo + Search Pill (matching screenshot #5)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MuzaLogo(titleSize = 30.sp, subtitleSize = 18.sp)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("history_search_input"),
                placeholder = {
                    Text(
                        text = strings.searchPlaceholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MuzaColors.TextMuted,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = strings.searchPlaceholder,
                        tint = MuzaColors.TextSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MuzaColors.Surface,
                    unfocusedContainerColor = MuzaColors.Surface,
                    focusedBorderColor = MuzaColors.BluePrimary,
                    unfocusedBorderColor = MuzaColors.Border,
                    focusedTextColor = MuzaColors.TextPrimary,
                    unfocusedTextColor = MuzaColors.TextPrimary,
                ),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = strings.historyHeader,
            style = MaterialTheme.typography.headlineMedium,
            color = MuzaColors.TextPrimary,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Segmented Tabs: Тарих / Сақталған
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HistoryTabPill(
                label = strings.tabHistory,
                selected = selectedTab == 0,
                testTag = "history_tab_all",
                onClick = { selectedTab = 0 },
                modifier = Modifier.weight(1f),
            )
            HistoryTabPill(
                label = strings.tabSaved,
                selected = selectedTab == 1,
                testTag = "history_tab_saved",
                onClick = { selectedTab = 1 },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (displayedPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = strings.emptyHistoryTitle,
                        tint = MuzaColors.BlueGlow.copy(alpha = 0.6f),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.emptyHistoryTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MuzaColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.emptyHistoryHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MuzaColors.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(displayedPhotos, key = { it.id }) { photo ->
                    PhotoGridCard(
                        photo = photo,
                        onClick = { inspectedPhoto = photo },
                        onToggleFavorite = { onToggleFavorite(photo) },
                    )
                }
            }
        }

        Text(
            text = String.format(strings.totalPhotosFormat, displayedPhotos.size),
            style = MaterialTheme.typography.bodyMedium,
            color = MuzaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        )
    }

    // Full-screen Photo Detail Modal
    val currentDetail = inspectedPhoto?.let { selected ->
        allPhotos.firstOrNull { it.id == selected.id } ?: selected
    }
    if (currentDetail != null) {
        PhotoDetailDialog(
            photo = currentDetail,
            onDismiss = { inspectedPhoto = null },
            onToggleFavorite = { onToggleFavorite(currentDetail) },
            onShare = { sharePhoto(context, currentDetail) },
            onDelete = {
                onDeletePhoto(currentDetail)
                inspectedPhoto = null
            },
        )
    }
}

@Composable
private fun HistoryTabPill(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) MuzaColors.ButtonGradient else Brush.horizontalGradient(listOf(MuzaColors.SurfaceElevated, MuzaColors.SurfaceElevated)))
            .border(
                1.dp,
                if (selected) MuzaColors.BlueGlow else MuzaColors.Border,
                RoundedCornerShape(24.dp),
            )
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) Color.White else MuzaColors.TextSecondary,
        )
    }
}

@Composable
private fun PhotoGridCard(
    photo: PhotoHistoryEntity,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val imageModel: Any = remember(photo.localFilePath, photo.mediaUri) {
        val localFile = File(photo.localFilePath)
        if (photo.localFilePath.isNotBlank() && localFile.exists()) localFile else Uri.parse(photo.mediaUri)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(18.dp))
            .background(MuzaColors.Surface)
            .border(1.dp, MuzaColors.Border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("photo_card_${photo.id}"),
    ) {
        AsyncImage(
            model = imageModel,
            contentDescription = photo.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Heart Favorite Toggle in top-right corner
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MuzaColors.Background.copy(alpha = 0.55f))
                .testTag("favorite_photo_${photo.id}"),
        ) {
            Icon(
                imageVector = if (photo.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite photo",
                tint = if (photo.isFavorite) MuzaColors.Danger else Color.White,
                modifier = Modifier.size(20.dp),
            )
        }

        // Bottom Date + City bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(MuzaColors.Background.copy(alpha = 0.80f))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = photo.city,
                tint = MuzaColors.BlueGlow,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${photo.dateLabel} • ${photo.city}",
                style = MaterialTheme.typography.labelSmall,
                color = MuzaColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PhotoDetailDialog(
    photo: PhotoHistoryEntity,
    onDismiss: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val strings by LanguageManager.strings.collectAsState()
    val imageModel: Any = remember(photo.localFilePath, photo.mediaUri) {
        val localFile = File(photo.localFilePath)
        if (photo.localFilePath.isNotBlank() && localFile.exists()) localFile else Uri.parse(photo.mediaUri)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .testTag("photo_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MuzaColors.Surface,
            border = BorderStroke(1.dp, MuzaColors.Border),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${photo.dateLabel} • ${photo.city}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MuzaColors.TextPrimary,
                    )
                    Row {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (photo.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (photo.isFavorite) MuzaColors.Danger else MuzaColors.TextPrimary,
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_photo_detail")) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.closeAction,
                                tint = MuzaColors.TextPrimary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AsyncImage(
                    model = imageModel,
                    contentDescription = photo.description,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.85f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MuzaColors.Background),
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = photo.glassesModel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MuzaColors.BlueGlow,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.descriptionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MuzaColors.TextSecondary,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = photo.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MuzaColors.TextPrimary,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_photo_button"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MuzaColors.BluePrimary),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = strings.shareAction,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.shareAction, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("delete_photo_button"),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MuzaColors.Danger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MuzaColors.Danger),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = strings.deleteAction,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.deleteAction, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun sharePhoto(context: Context, photo: PhotoHistoryEntity) {
    runCatching {
        val localFile = File(photo.localFilePath)
        val uri = if (photo.localFilePath.isNotBlank() && localFile.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
        } else {
            Uri.parse(photo.mediaUri)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, photo.description)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Muza Glasses Photo"))
    }
}
