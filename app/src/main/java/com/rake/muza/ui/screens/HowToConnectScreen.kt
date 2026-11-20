package com.rake.muza.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.ConnectionUiState
import com.rake.muza.core.LanguageManager
import com.rake.muza.ui.components.HolographicGlassesCanvas
import com.rake.muza.ui.components.MuzaCard
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors

@Composable
fun HowToConnectScreen(
    connectionState: ConnectionUiState,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectNow: (String) -> Unit,
) {
    val strings by LanguageManager.strings.collectAsState()
    var activeStep by remember { mutableIntStateOf(2) } // 1..3
    var pinCode by remember { mutableStateOf("884210") }

    DisposableEffect(Unit) {
        onStartScan()
        onDispose { onStopScan() }
    }

    val progress = when (activeStep) {
        1 -> 0.33f
        2 -> 0.66f
        else -> 1.0f
    }
    val detectedLabel = connectionState.discoveredDevices.firstOrNull()?.name
        ?: "MUZA-GL-${pinCode.takeLast(4).ifBlank { "8842" }}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag("how_to_connect_screen"),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MuzaLogo(titleSize = 32.sp, subtitleSize = 19.sp)
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MuzaColors.BlueGlow,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = strings.howToConnectTitle,
            style = MaterialTheme.typography.headlineLarge,
            color = MuzaColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.howToConnectSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MuzaColors.TextSecondary,
        )

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MuzaColors.BluePrimary,
            trackColor = MuzaColors.SurfaceElevated,
        )

        Spacer(modifier = Modifier.height(18.dp))

        // 3 Interactive Steps
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StepRowCard(
                stepNumber = 1,
                title = strings.step1Title,
                description = strings.step1Desc,
                isCompleted = activeStep > 1,
                isCurrent = activeStep == 1,
                onClick = { activeStep = 1 },
            )
            StepRowCard(
                stepNumber = 2,
                title = strings.step2Title,
                description = strings.step2Desc,
                isCompleted = activeStep > 2,
                isCurrent = activeStep == 2,
                onClick = { activeStep = 2 },
            )
            StepRowCard(
                stepNumber = 3,
                title = strings.step3Title,
                description = strings.step3Desc,
                isCompleted = false,
                isCurrent = activeStep == 3,
                onClick = { activeStep = 3 },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Glowing Holographic Blue Glasses Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(136.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MuzaColors.Surface)
                .border(1.dp, MuzaColors.Border, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            HolographicGlassesCanvas(
                modifier = Modifier
                    .fillMaxWidth(0.76f)
                    .height(104.dp),
                showCornerBrackets = true,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bluetooth Device Detection Pill
        MuzaCard(
            modifier = Modifier.fillMaxWidth(),
            isHighlighted = true,
            cornerRadius = 18.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MuzaColors.BluePrimary.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bluetooth,
                        contentDescription = "Bluetooth",
                        tint = MuzaColors.BlueGlow,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (connectionState.isScanning) strings.waitingConnection else strings.glassesFoundPrefix,
                        style = MaterialTheme.typography.labelMedium,
                        color = MuzaColors.TextSecondary,
                    )
                    Text(
                        text = detectedLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MuzaColors.BlueSoft,
                    )
                }
                IconButton(
                    onClick = onStartScan,
                    modifier = Modifier.testTag("rescan_bluetooth_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = strings.scanBluetooth,
                        tint = MuzaColors.BlueGlow,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = pinCode,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(6)
                pinCode = digits
                if (digits.length == 6) activeStep = 3
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pairing_pin_input"),
            label = { Text(strings.enterPinHint, color = MuzaColors.TextSecondary) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MuzaColors.Surface,
                unfocusedContainerColor = MuzaColors.Surface,
                focusedBorderColor = MuzaColors.BluePrimary,
                unfocusedBorderColor = MuzaColors.Border,
                focusedTextColor = MuzaColors.TextPrimary,
                unfocusedTextColor = MuzaColors.TextPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(18.dp))

        MuzaGradientButton(
            text = "${strings.continueAction} →",
            onClick = { onConnectNow(pinCode.ifBlank { "884210" }) },
            testTag = "how_to_connect_continue_button",
        )
        Spacer(modifier = Modifier.height(14.dp))
    }
}

@Composable
private fun StepRowCard(
    stepNumber: Int,
    title: String,
    description: String,
    isCompleted: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    MuzaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("connect_step_$stepNumber"),
        isHighlighted = isCurrent,
        cornerRadius = 16.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isCompleted -> MuzaColors.Emerald
                            isCurrent -> MuzaColors.BluePrimary
                            else -> MuzaColors.SurfaceElevated
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Step completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = "$stepNumber",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MuzaColors.TextPrimary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuzaColors.TextSecondary,
                )
            }
        }
    }
}
