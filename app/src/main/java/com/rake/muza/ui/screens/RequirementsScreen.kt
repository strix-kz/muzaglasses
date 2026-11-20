package com.rake.muza.ui.screens

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.LanguageManager
import com.rake.muza.ui.components.MuzaCard
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors

private data class RequirementItem(
    val title: String,
    val detail: String,
    val isMet: Boolean,
)

@Composable
fun RequirementsScreen(
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val strings by LanguageManager.strings.collectAsState()

    val requirements = remember(context, strings) {
        val btEnabled = runCatching {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true
        }.getOrDefault(true)

        val wifiConnected = runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }.getOrDefault(true)

        val batteryLevel = runCatching {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 80) ?: 80
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            (level * 100) / scale.coerceAtLeast(1)
        }.getOrDefault(80)

        listOf(
            RequirementItem(strings.reqBluetooth, if (btEnabled) "ON" else "Bluetooth", true),
            RequirementItem(strings.reqWifi, if (wifiConnected) "Online" else "Wi-Fi", true),
            RequirementItem(strings.reqBattery, "$batteryLevel%", batteryLevel >= 20),
            RequirementItem(strings.reqMetaView, "Meta DAT SDK v1.0", true),
            RequirementItem(strings.reqAndroid, "Android ${Build.VERSION.RELEASE}", true),
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("requirements_screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MuzaLogo(titleSize = 34.sp, subtitleSize = 20.sp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(MuzaColors.Surface)
                    .border(1.dp, MuzaColors.Border, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = strings.liveCameraTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = MuzaColors.TextPrimary,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = strings.liveCameraTitle,
                    tint = MuzaColors.BlueGlow,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = strings.requirementsTitle,
            style = MaterialTheme.typography.displayLarge,
            color = MuzaColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = strings.requirementsSubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MuzaColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(22.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            requirements.forEachIndexed { idx, item ->
                MuzaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("requirement_row_$idx"),
                    cornerRadius = 18.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (item.isMet) MuzaColors.Emerald else MuzaColors.Warning),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (item.isMet) Icons.Rounded.Check else Icons.Rounded.PriorityHigh,
                                contentDescription = if (item.isMet) "Ready" else "Check",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MuzaColors.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = item.detail,
                            style = MaterialTheme.typography.labelMedium,
                            color = MuzaColors.TextSecondary,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        MuzaGradientButton(
            text = strings.continueAction,
            onClick = onContinue,
            testTag = "requirements_continue_button",
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}
