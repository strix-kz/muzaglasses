package com.rake.muza.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.LanguageManager
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors
import com.rake.muza.viewmodel.AiChatUiState
import com.rake.muza.viewmodel.ChatMessageUi

@Composable
fun AiChatScreen(
    uiState: AiChatUiState,
    initialPrompt: String?,
    onConsumeInitialPrompt: () -> Unit,
    onAttachBitmap: (Bitmap?) -> Unit,
    onGrabLiveCameraFrame: () -> Bitmap?,
    onVoiceDictationClick: () -> Unit,
    onSendPrompt: (String) -> Unit,
    onClearError: () -> Unit,
) {
    val context = LocalContext.current
    val strings by LanguageManager.strings.collectAsState()
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Zero-permission Android Photo Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { bmp ->
                onAttachBitmap(bmp)
            }
        }
    }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank()) {
            onSendPrompt(initialPrompt)
            onConsumeInitialPrompt()
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.streamingReply.length) {
        val total = uiState.messages.size + if (uiState.isGenerating) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .testTag("ai_chat_screen"),
    ) {
        // Header: Top-Left MuzaLogo + AI-көмекші ✨
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MuzaLogo(titleSize = 32.sp, subtitleSize = 19.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.aiAssistantTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MuzaColors.TextPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = strings.aiAssistantTitle,
                    tint = MuzaColors.BlueGlow,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Centered pill badge
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MuzaColors.SurfaceElevated,
                border = BorderStroke(1.dp, MuzaColors.Border),
            ) {
                Text(
                    text = strings.aiChatBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = MuzaColors.BlueSoft,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Greeting bubble at top
            item(key = "greeting") {
                AiBubble(
                    text = strings.aiGreeting,
                    timestamp = "Muza AI",
                    isStreaming = false,
                )
            }

            items(uiState.messages, key = { it.id }) { msg ->
                if (msg.isUser) {
                    UserBubble(msg)
                } else {
                    AiBubble(
                        text = msg.text,
                        timestamp = msg.timestampLabel,
                        isStreaming = false,
                    )
                }
            }

            if (uiState.isGenerating) {
                item(key = "streaming") {
                    AiBubble(
                        text = uiState.streamingReply.ifBlank { strings.aiThinking },
                        timestamp = "...",
                        isStreaming = true,
                    )
                }
            }
        }

        // Real error banner (never fake AI replies)
        if (!uiState.errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = MuzaColors.Danger.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, MuzaColors.Danger.copy(alpha = 0.6f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MuzaColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onClearError, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss error",
                            tint = MuzaColors.TextPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

        // Attached photo preview chip
        if (uiState.attachedBitmap != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box {
                    Image(
                        bitmap = uiState.attachedBitmap.asImageBitmap(),
                        contentDescription = "Attached photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MuzaColors.BlueGlow, RoundedCornerShape(12.dp)),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MuzaColors.Background.copy(alpha = 0.85f))
                            .clickable { onAttachBitmap(null) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Remove attachment",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Multimodal Vision Ready",
                    style = MaterialTheme.typography.labelMedium,
                    color = MuzaColors.BlueSoft,
                )
            }
        }

        // Quick multimodal chips + Input composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChatQuickChip(
                icon = Icons.Rounded.Image,
                label = strings.aiAttachPhoto,
                testTag = "ai_pick_photo_button",
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            )
            ChatQuickChip(
                icon = Icons.Rounded.PhotoCamera,
                label = strings.whatDoYouSee,
                testTag = "ai_camera_vision_chip",
                onClick = {
                    val frame = onGrabLiveCameraFrame()
                    if (frame != null) onAttachBitmap(frame)
                    onSendPrompt(strings.whatDoYouSee)
                },
            )
            ChatQuickChip(
                icon = Icons.Rounded.Mic,
                label = strings.aiVoicePrompt,
                testTag = "ai_voice_chip",
                onClick = onVoiceDictationClick,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input"),
                placeholder = { Text(strings.aiInputPlaceholder, color = MuzaColors.TextMuted) },
                maxLines = 3,
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MuzaColors.Surface,
                    unfocusedContainerColor = MuzaColors.Surface,
                    focusedBorderColor = MuzaColors.BluePrimary,
                    unfocusedBorderColor = MuzaColors.Border,
                    focusedTextColor = MuzaColors.TextPrimary,
                    unfocusedTextColor = MuzaColors.TextPrimary,
                ),
            )

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MuzaColors.ButtonGradient)
                    .clickable(enabled = !uiState.isGenerating && draft.isNotBlank()) {
                        val toSend = draft.trim()
                        draft = ""
                        onSendPrompt(toSend)
                    }
                    .testTag("ai_send_button"),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.2.dp,
                        color = Color.White,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send message",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun UserBubble(message: ChatMessageUi) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 290.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp),
            color = Color(0xFF283C67),
            border = BorderStroke(1.dp, MuzaColors.BluePrimary.copy(alpha = 0.45f)),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MuzaColors.TextPrimary,
                )
                if (message.timestampLabel.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.timestampLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MuzaColors.BlueSoft,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
private fun AiBubble(
    text: String,
    timestamp: String,
    isStreaming: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 316.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
            color = MuzaColors.SurfaceElevated,
            border = BorderStroke(1.dp, MuzaColors.Border),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Muza AI",
                        tint = MuzaColors.BlueGlow,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Muza AI",
                        style = MaterialTheme.typography.labelLarge,
                        color = MuzaColors.BlueSoft,
                    )
                    if (isStreaming) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.8.dp,
                            color = MuzaColors.BlueGlow,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MuzaColors.TextPrimary,
                )
                if (timestamp.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MuzaColors.TextMuted,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatQuickChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = MuzaColors.Surface,
        border = BorderStroke(1.dp, MuzaColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MuzaColors.BlueGlow,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MuzaColors.TextPrimary,
            )
        }
    }
}
