package com.rake.muza.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FiberManualRecord
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.ConnectionUiState
import com.rake.muza.core.DisplayHudPayload
import com.rake.muza.core.LanguageManager
import com.rake.muza.core.VoiceUiState
import com.rake.muza.ui.components.CameraPreviewBox
import com.rake.muza.ui.components.DisplayHudOverlay
import com.rake.muza.ui.components.HolographicGlassesCanvas
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors
import com.rake.muza.viewmodel.CameraFilterMode
import com.rake.muza.viewmodel.HomeCameraUiState

@Composable
fun HomeScreen(
    cameraState: HomeCameraUiState,
    connectionState: ConnectionUiState,
    hudState: DisplayHudPayload,
    voiceState: VoiceUiState,
    hasCameraPermission: Boolean,
    formattedRecordingTime: String,
    onRequestCameraPermission: () -> Unit,
    onRequestAudioPermission: () -> Unit,
    onFrameAvailable: (Bitmap) -> Unit,
    onToggleFlash: () -> Unit,
    onToggleScanOverlay: () -> Unit,
    onToggleParamsSheet: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onFilterModeChange: (CameraFilterMode) -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    onAskAi: () -> Unit,
    onWhatDoYouSee: () -> Unit,
    onTranslate: () -> Unit,
    onToggleVoice: () -> Unit,
    onOpenMap: () -> Unit,
    onDismissHud: () -> Unit,
) {
    val strings by LanguageManager.strings.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .testTag("home_screen"),
    ) {
        // 1. Live Camera Feed (CameraX + DAT ready)
        CameraPreviewBox(
            hasCameraPermission = hasCameraPermission,
            isFlashOn = cameraState.isFlashOn,
            brightness = cameraState.brightness,
            filterMode = cameraState.filterMode,
            onFrameAvailable = onFrameAvailable,
            onRequestCameraPermission = onRequestCameraPermission,
            modifier = Modifier.fillMaxSize(),
        )

        // Top & bottom cinematic dark scrims for crisp readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MuzaColors.Background.copy(alpha = 0.88f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MuzaColors.Background.copy(alpha = 0.94f))
                    )
                )
        )

        // 2. Center AR Holographic Glasses Frame Measurement Overlay
        if (cameraState.isScanOverlayActive) {
            HolographicGlassesCanvas(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 72.dp)
                    .fillMaxWidth(0.76f)
                    .height(140.dp),
                showCornerBrackets = true,
            )
        }

        // 3. Top Header: Persistent Top-Left Logo + LIVE HD 1080p + Device Badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MuzaLogo(titleSize = 32.sp, subtitleSize = 19.sp)

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(MuzaColors.HudBackground)
                            .border(
                                1.dp,
                                if (cameraState.isRecording) MuzaColors.Danger else MuzaColors.BorderActive.copy(alpha = 0.6f),
                                RoundedCornerShape(14.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (cameraState.isRecording) MuzaColors.Danger else MuzaColors.Emerald)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (cameraState.isRecording) "REC $formattedRecordingTime" else "LIVE HD 1080p",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MuzaColors.TextPrimary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${connectionState.deviceInfo.deviceName} • ${connectionState.deviceInfo.batteryPercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MuzaColors.BlueSoft,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AR Frame Measurement Pill (matching screenshot #8)
            AnimatedVisibility(visible = cameraState.isScanOverlayActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MuzaColors.HudBackground,
                    border = BorderStroke(1.dp, MuzaColors.BluePrimary.copy(alpha = 0.55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MuzaColors.Emerald),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Detected",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.frameDetectedTitle,
                                style = MaterialTheme.typography.labelLarge,
                                color = MuzaColors.TextPrimary,
                            )
                            Text(
                                text = strings.optimalSizeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MuzaColors.TextSecondary,
                            )
                        }
                    }
                }
            }

            // Corner Meta Display Glasses HUD Mirror Overlay
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                DisplayHudOverlay(
                    hud = hudState,
                    onDismiss = onDismissHud,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }

        // 4. Left Floating Camera Tool Pills (Flash, Scan, Parameters — matching screenshot #8)
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, top = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VerticalToolPill(
                icon = if (cameraState.isFlashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                label = strings.flashLabel,
                isActive = cameraState.isFlashOn,
                testTag = "camera_flash_button",
                onClick = onToggleFlash,
            )
            VerticalToolPill(
                icon = Icons.Rounded.CenterFocusStrong,
                label = strings.scanLabel,
                isActive = cameraState.isScanOverlayActive,
                testTag = "camera_scan_toggle_button",
                onClick = onToggleScanOverlay,
            )
            VerticalToolPill(
                icon = Icons.Rounded.Tune,
                label = strings.paramsLabel,
                isActive = cameraState.showParamsSheet,
                testTag = "camera_params_button",
                onClick = onToggleParamsSheet,
            )
        }

        // 5. Bottom Control Dock (Parameters Drawer, Status Banner, Location + Voice Pill, Capture/Record, AI Actions)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Live toast / AI vision result banner
            AnimatedVisibility(visible = !cameraState.statusBanner.isNullOrBlank() || cameraState.isAnalyzingVision) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MuzaColors.SurfaceElevated.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, MuzaColors.BluePrimary.copy(alpha = 0.65f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (cameraState.isAnalyzingVision) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MuzaColors.BlueGlow,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.aiThinking,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MuzaColors.BlueSoft,
                            )
                        } else {
                            Text(
                                text = cameraState.statusBanner.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MuzaColors.TextPrimary,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            // Expandable Camera Parameters (Brightness 70% + Natural/Warm/Contrast)
            AnimatedVisibility(visible = cameraState.showParamsSheet) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_params_panel"),
                    shape = RoundedCornerShape(18.dp),
                    color = MuzaColors.HudBackground,
                    border = BorderStroke(1.dp, MuzaColors.Border),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = strings.brightnessLabel,
                                style = MaterialTheme.typography.labelLarge,
                                color = MuzaColors.TextPrimary,
                            )
                            Text(
                                text = "${(cameraState.brightness * 100).toInt()}%",
                                style = MaterialTheme.typography.labelLarge,
                                color = MuzaColors.BlueGlow,
                            )
                        }
                        Slider(
                            value = cameraState.brightness,
                            onValueChange = onBrightnessChange,
                            valueRange = 0.25f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MuzaColors.BlueGlow,
                                activeTrackColor = MuzaColors.BluePrimary,
                                inactiveTrackColor = MuzaColors.SurfaceHighlight,
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilterChipPill(
                                label = strings.filterNatural,
                                selected = cameraState.filterMode == CameraFilterMode.NATURAL,
                                onClick = { onFilterModeChange(CameraFilterMode.NATURAL) },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChipPill(
                                label = strings.filterWarm,
                                selected = cameraState.filterMode == CameraFilterMode.WARM,
                                onClick = { onFilterModeChange(CameraFilterMode.WARM) },
                                modifier = Modifier.weight(1f),
                            )
                            FilterChipPill(
                                label = strings.filterContrast,
                                selected = cameraState.filterMode == CameraFilterMode.CONTRAST,
                                onClick = { onFilterModeChange(CameraFilterMode.CONTRAST) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // Location (tappable -> GPS Map) + Wake Word "Muza" Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MuzaColors.HudBackground)
                        .border(1.dp, MuzaColors.Border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenMap)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("home_gps_location_badge"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = "GPS Map",
                        tint = MuzaColors.BlueGlow,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cameraState.locationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MuzaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (voiceState.isListening) MuzaColors.BluePrimary.copy(alpha = 0.28f)
                            else MuzaColors.HudBackground
                        )
                        .border(
                            1.dp,
                            if (voiceState.isListening) MuzaColors.BlueGlow else MuzaColors.Border,
                            RoundedCornerShape(12.dp),
                        )
                        .clickable {
                            onRequestAudioPermission()
                            onToggleVoice()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("wake_word_pill"),
                ) {
                    Icon(
                        imageVector = if (voiceState.isListening) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                        contentDescription = "Voice wake word",
                        tint = if (voiceState.isListening) MuzaColors.BlueGlow else MuzaColors.TextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (voiceState.isListening) {
                            "«Muza» • ${voiceState.activePreset.displayName}"
                        } else {
                            "«Muza»"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (voiceState.isListening) MuzaColors.TextPrimary else MuzaColors.TextSecondary,
                        maxLines = 1,
                    )
                }
            }

            // Quick AI, Vision, Translation, GPS Map & Voice Smart-Glasses Action Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickSmartActionChip(
                    icon = Icons.Rounded.Visibility,
                    label = strings.whatDoYouSee,
                    testTag = "home_what_do_you_see_button",
                    onClick = onWhatDoYouSee,
                )
                QuickSmartActionChip(
                    icon = Icons.Rounded.Translate,
                    label = strings.translateAction,
                    testTag = "home_translate_button",
                    onClick = onTranslate,
                )
                QuickSmartActionChip(
                    icon = Icons.Rounded.LocationOn,
                    label = strings.mapTitle,
                    testTag = "home_open_map_chip",
                    onClick = onOpenMap,
                )
                QuickSmartActionChip(
                    icon = Icons.Rounded.AutoAwesome,
                    label = strings.askQuestion,
                    testTag = "home_ask_ai_button",
                    onClick = onAskAi,
                )
                QuickSmartActionChip(
                    icon = Icons.Rounded.Mic,
                    label = strings.voiceAction,
                    testTag = "home_voice_button",
                    onClick = {
                        onRequestAudioPermission()
                        onToggleVoice()
                    },
                )
            }

            // Primary Capture Photo + Video Record Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MuzaGradientButton(
                    text = strings.capturePhoto,
                    onClick = onCapturePhoto,
                    testTag = "home_capture_photo_button",
                    height = 54.dp,
                    modifier = Modifier.weight(1.35f),
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = strings.capturePhoto,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                )

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .clickable(onClick = onToggleRecording)
                        .testTag("home_record_video_button"),
                    shape = RoundedCornerShape(27.dp),
                    color = if (cameraState.isRecording) MuzaColors.Danger else MuzaColors.SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (cameraState.isRecording) MuzaColors.Danger else MuzaColors.Border,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (cameraState.isRecording) Icons.Rounded.Stop else Icons.Rounded.FiberManualRecord,
                            contentDescription = strings.startRecording,
                            tint = if (cameraState.isRecording) Color.White else MuzaColors.Danger,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (cameraState.isRecording) formattedRecordingTime else strings.startRecording,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalToolPill(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isActive) MuzaColors.BluePrimary.copy(alpha = 0.30f) else MuzaColors.HudBackground)
            .border(
                1.dp,
                if (isActive) MuzaColors.BlueGlow else MuzaColors.Border,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) MuzaColors.BlueGlow else MuzaColors.TextPrimary,
            modifier = Modifier.size(22.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MuzaColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FilterChipPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MuzaColors.BluePrimary else MuzaColors.SurfaceElevated)
            .border(1.dp, if (selected) MuzaColors.BlueGlow else MuzaColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) Color.White else MuzaColors.TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuickSmartActionChip(
    icon: ImageVector,
    label: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(22.dp),
        color = MuzaColors.HudBackground,
        border = BorderStroke(1.dp, MuzaColors.Border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MuzaColors.BlueGlow,
                modifier = Modifier.size(17.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MuzaColors.TextPrimary,
                maxLines = 1,
            )
        }
    }
}
