package com.rake.muza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.GlassesDeviceInfo
import com.rake.muza.core.LanguageManager
import com.rake.muza.ui.components.MuzaCard
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors

@Composable
fun ConnectedScreen(
    deviceInfo: GlassesDeviceInfo,
    onStart: () -> Unit,
) {
    val strings by LanguageManager.strings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("connected_screen"),
    ) {
        MuzaLogo(titleSize = 34.sp, subtitleSize = 20.sp)

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(MuzaColors.Emerald.copy(alpha = 0.28f), Color.Transparent)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(MuzaColors.Emerald)
                        .border(2.dp, MuzaColors.EmeraldSoft, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = strings.connectedTitle,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.connectedTitle,
                style = MaterialTheme.typography.displayLarge,
                color = MuzaColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${deviceInfo.deviceName} ${strings.connectedSuffix}",
                style = MaterialTheme.typography.titleMedium,
                color = MuzaColors.BlueSoft,
                textAlign = TextAlign.Center,
            )
            Text(
                text = deviceInfo.deviceCode,
                style = MaterialTheme.typography.labelMedium,
                color = MuzaColors.TextMuted,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DeviceStatusMetricCard(
                icon = Icons.Rounded.BatteryChargingFull,
                iconTint = MuzaColors.Emerald,
                title = "${strings.batteryLabel} ${deviceInfo.batteryPercent}%",
                subtitle = strings.fullChargeEta,
            )
            DeviceStatusMetricCard(
                icon = Icons.Rounded.BluetoothConnected,
                iconTint = MuzaColors.BlueGlow,
                title = strings.bluetoothConnected,
                subtitle = strings.stableLink,
            )
            DeviceStatusMetricCard(
                icon = Icons.Rounded.SignalCellularAlt,
                iconTint = MuzaColors.EmeraldSoft,
                title = strings.signalStrong,
                subtitle = strings.latencyLabel,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = strings.glassesReadyNote,
            style = MaterialTheme.typography.bodyMedium,
            color = MuzaColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        MuzaGradientButton(
            text = strings.startAction,
            onClick = onStart,
            testTag = "connected_start_button",
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DeviceStatusMetricCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
) {
    MuzaCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MuzaColors.TextPrimary,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuzaColors.TextSecondary,
                )
            }
        }
    }
}
