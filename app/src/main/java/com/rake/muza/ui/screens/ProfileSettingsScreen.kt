package com.rake.muza.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.AppLanguage
import com.rake.muza.core.ConnectionUiState
import com.rake.muza.core.DisplayManager
import com.rake.muza.core.LanguageManager
import com.rake.muza.core.MuzaFeatures
import com.rake.muza.core.VoiceGender
import com.rake.muza.core.VoiceSpeed
import com.rake.muza.core.VoiceUiState
import com.rake.muza.ui.components.MuzaCard
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors

@Composable
fun ProfileSettingsScreen(
    displayName: String,
    email: String,
    connectionState: ConnectionUiState,
    voiceState: VoiceUiState,
    displayManager: DisplayManager,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectVoiceGender: (VoiceGender) -> Unit,
    onSelectVoiceSpeed: (VoiceSpeed) -> Unit,
    onTestVoice: () -> Unit,
    onManageGlasses: () -> Unit,
    onSignOut: () -> Unit,
) {
    val strings by LanguageManager.strings.collectAsState()
    val currentLang by LanguageManager.language.collectAsState()
    val features by MuzaFeatures.state.collectAsState()

    var showModules by remember { mutableStateOf(false) }
    var showHudTester by remember { mutableStateOf(false) }

    val initials = remember(displayName) {
        displayName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifBlank { "MG" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("profile_settings_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MuzaLogo(titleSize = 32.sp, subtitleSize = 19.sp)

        Text(
            text = strings.profileSettingsTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = MuzaColors.TextPrimary,
        )

        // User Profile Card with Initials Avatar + Premium Badge
        MuzaCard(modifier = Modifier.fillMaxWidth(), isHighlighted = true) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MuzaColors.ButtonGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayName.ifBlank { "Muza User" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MuzaColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MuzaColors.BluePrimary.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, MuzaColors.BlueGlow),
                        ) {
                            Text(
                                text = strings.premiumBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MuzaColors.BlueSoft,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MuzaColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // 1. Language Selector: Қазақша / Русский / English
        MuzaCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = strings.languageSection,
                    tint = MuzaColors.BlueGlow,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.languagePrefix} ${currentLang.displayName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MuzaColors.TextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppLanguage.entries.forEach { lang ->
                    val selected = lang == currentLang
                    SegmentChoiceButton(
                        label = lang.displayName,
                        selected = selected,
                        testTag = "profile_lang_${lang.code.lowercase()}",
                        onClick = { onSelectLanguage(lang) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 2. Voice Settings: Female / Male + Slow / Medium / Fast + Test Voice
        MuzaCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = strings.voiceSection,
                    tint = MuzaColors.BlueGlow,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.voiceMenu,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MuzaColors.TextPrimary,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SegmentChoiceButton(
                    label = strings.femaleVoice,
                    selected = voiceState.gender == VoiceGender.FEMALE,
                    testTag = "voice_gender_female",
                    onClick = { onSelectVoiceGender(VoiceGender.FEMALE) },
                    modifier = Modifier.weight(1f),
                )
                SegmentChoiceButton(
                    label = strings.maleVoice,
                    selected = voiceState.gender == VoiceGender.MALE,
                    testTag = "voice_gender_male",
                    onClick = { onSelectVoiceGender(VoiceGender.MALE) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.speedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MuzaColors.TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SegmentChoiceButton(
                    label = strings.speedSlow,
                    selected = voiceState.speed == VoiceSpeed.SLOW,
                    testTag = "voice_speed_slow",
                    onClick = { onSelectVoiceSpeed(VoiceSpeed.SLOW) },
                    modifier = Modifier.weight(1f),
                )
                SegmentChoiceButton(
                    label = strings.speedMedium,
                    selected = voiceState.speed == VoiceSpeed.MEDIUM,
                    testTag = "voice_speed_medium",
                    onClick = { onSelectVoiceSpeed(VoiceSpeed.MEDIUM) },
                    modifier = Modifier.weight(1f),
                )
                SegmentChoiceButton(
                    label = strings.speedFast,
                    selected = voiceState.speed == VoiceSpeed.FAST,
                    testTag = "voice_speed_fast",
                    onClick = { onSelectVoiceSpeed(VoiceSpeed.FAST) },
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MuzaGradientButton(
                text = strings.testVoiceButton,
                onClick = onTestVoice,
                testTag = "test_voice_button",
                height = 48.dp,
            )
        }

        // 3. Connected Glasses & Battery Row
        SettingsNavCard(
            icon = Icons.Rounded.Visibility,
            title = strings.glassesMenu,
            subtitle = "${connectionState.deviceInfo.deviceName} • ${connectionState.deviceInfo.deviceCode}",
            testTag = "profile_manage_glasses",
            onClick = onManageGlasses,
        )

        SettingsNavCard(
            icon = Icons.Rounded.BatteryChargingFull,
            title = "${strings.batteryMenu} ${connectionState.deviceInfo.batteryPercent}%",
            subtitle = "${strings.signalStrong} • ${strings.latencyLabel}",
            testTag = "profile_battery_card",
            onClick = onManageGlasses,
        )

        // 4. Meta Display Glasses HUD Live Preview Controls
        SettingsNavCard(
            icon = Icons.Rounded.SmartDisplay,
            title = strings.displayHudMenu,
            subtitle = "Navigation • Live Translation • AI • WhatsApp",
            testTag = "profile_display_hud_toggle",
            onClick = { showHudTester = !showHudTester },
        )

        if (showHudTester) {
            MuzaCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Meta Display Glasses HUD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MuzaColors.BlueSoft,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SegmentChoiceButton(
                        label = "🧭 Nav",
                        selected = false,
                        testTag = "hud_test_nav",
                        onClick = { displayManager.showNavigation(batteryPercent = connectionState.deviceInfo.batteryPercent) },
                        modifier = Modifier.weight(1f),
                    )
                    SegmentChoiceButton(
                        label = "🌐 Translate",
                        selected = false,
                        testTag = "hud_test_translate",
                        onClick = { displayManager.showTranslation("OPEN", "АШЫҚ", "EN → KK") },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SegmentChoiceButton(
                        label = "💬 WhatsApp",
                        selected = false,
                        testTag = "hud_test_msg",
                        onClick = { displayManager.showMessage("Arman", "Кездесу сағат 18:00-де!") },
                        modifier = Modifier.weight(1f),
                    )
                    SegmentChoiceButton(
                        label = "✨ AI 5s",
                        selected = false,
                        testTag = "hud_test_ai",
                        onClick = { displayManager.showAiResponse(strings.howAreYouResponse) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // 5. Modular Feature Switches (MuzaFeatures)
        SettingsNavCard(
            icon = Icons.Rounded.Tune,
            title = strings.modulesTitle,
            subtitle = "Camera • Voice • Display • AI • Ray-Ban",
            testTag = "profile_modules_toggle",
            onClick = { showModules = !showModules },
        )

        if (showModules) {
            MuzaCard(modifier = Modifier.fillMaxWidth()) {
                FeatureSwitchRow("Камера: Фото / Photo", features.cameraPhoto) { v ->
                    MuzaFeatures.update { it.copy(cameraPhoto = v) }
                }
                FeatureSwitchRow("Камера: Видео / Video", features.cameraVideo) { v ->
                    MuzaFeatures.update { it.copy(cameraVideo = v) }
                }
                FeatureSwitchRow("Камера: Тікелей эфир / Live", features.cameraLivePreview) { v ->
                    MuzaFeatures.update { it.copy(cameraLivePreview = v) }
                }
                HorizontalDivider(color = MuzaColors.Border, modifier = Modifier.padding(vertical = 6.dp))
                FeatureSwitchRow("Дауыс: Wake word «Muza»", features.voiceWakeWordMuza) { v ->
                    MuzaFeatures.update { it.copy(voiceWakeWordMuza = v) }
                }
                FeatureSwitchRow("Дауыс: Дауыстап жауап / TTS", features.voiceSpokenReply) { v ->
                    MuzaFeatures.update { it.copy(voiceSpokenReply = v) }
                }
                HorizontalDivider(color = MuzaColors.Border, modifier = Modifier.padding(vertical = 6.dp))
                FeatureSwitchRow("Display: Аударма / Translation", features.displayTranslation) { v ->
                    MuzaFeatures.update { it.copy(displayTranslation = v) }
                }
                FeatureSwitchRow("Display: Навигация / Navigation", features.displayNavigation) { v ->
                    MuzaFeatures.update { it.copy(displayNavigation = v) }
                }
                FeatureSwitchRow("Display: Хабарлама / Notifications", features.displayNotifications) { v ->
                    MuzaFeatures.update { it.copy(displayNotifications = v) }
                }
                FeatureSwitchRow("Display: AI жауабы / AI HUD", features.displayAiAnswer) { v ->
                    MuzaFeatures.update { it.copy(displayAiAnswer = v) }
                }
                HorizontalDivider(color = MuzaColors.Border, modifier = Modifier.padding(vertical = 6.dp))
                FeatureSwitchRow("AI: Көріп айту / Vision", features.aiVisionDescribe) { v ->
                    MuzaFeatures.update { it.copy(aiVisionDescribe = v) }
                }
                FeatureSwitchRow("AI: Аударма / Translation", features.aiTranslation) { v ->
                    MuzaFeatures.update { it.copy(aiTranslation = v) }
                }
                FeatureSwitchRow("AI: Сөйлесу / Conversation", features.aiConversation) { v ->
                    MuzaFeatures.update { it.copy(aiConversation = v) }
                }
                FeatureSwitchRow("Барлық Ray-Ban нұсқалары", features.glassesAllRayBanVersions) { v ->
                    MuzaFeatures.update { it.copy(glassesAllRayBanVersions = v) }
                }
            }
        }

        // 6. Sign Out Card (matching red "Шығу" row in screenshot #2)
        MuzaCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("profile_sign_out_button"),
            onClick = onSignOut,
            cornerRadius = 18.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.PowerSettingsNew,
                    contentDescription = strings.signOutAction,
                    tint = MuzaColors.Danger,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.signOutAction,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MuzaColors.Danger,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MuzaColors.TextMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun SegmentChoiceButton(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) MuzaColors.BluePrimary else MuzaColors.SurfaceElevated)
            .border(
                1.dp,
                if (selected) MuzaColors.BlueGlow else MuzaColors.Border,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MuzaColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SettingsNavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    MuzaCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        onClick = onClick,
        cornerRadius = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MuzaColors.BlueGlow,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MuzaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = MuzaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MuzaColors.TextMuted,
            )
        }
    }
}

@Composable
private fun FeatureSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MuzaColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MuzaColors.BluePrimary,
                uncheckedThumbColor = MuzaColors.TextSecondary,
                uncheckedTrackColor = MuzaColors.SurfaceElevated,
            ),
        )
    }
}
