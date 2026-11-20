package com.rake.muza.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.LanguageManager
import com.rake.muza.core.RayBanModel
import com.rake.muza.ui.components.MuzaCard
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors

@Composable
fun GlassesSelectionScreen(
    selectedModel: RayBanModel,
    onSelectModel: (RayBanModel) -> Unit,
    onProceed: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    val strings by LanguageManager.strings.collectAsState()
    val currentLang by LanguageManager.language.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("glasses_selection_screen"),
    ) {
        // Header with top-left persistent MuzaLogo + title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("glasses_back_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MuzaColors.TextPrimary,
                    )
                }
            }
            MuzaLogo(titleSize = 30.sp, subtitleSize = 18.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.addGlassesTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MuzaColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = strings.addGlassesSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuzaColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(RayBanModel.entries, key = { it.id }) { model ->
                val isSelected = model == selectedModel
                MuzaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("glasses_model_${model.id.lowercase()}"),
                    isHighlighted = isSelected,
                    onClick = { onSelectModel(model) },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GlassesModelThumbnail(
                            model = model,
                            modifier = Modifier.size(72.dp),
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MuzaColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = model.subtitle(currentLang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MuzaColors.TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (model.isPopular || model.hasDisplay) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (model.isPopular) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MuzaColors.BluePrimary)
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = strings.popularBadge,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color.White,
                                            )
                                        }
                                    }
                                    if (model.hasDisplay) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MuzaColors.BlueDeep.copy(alpha = 0.45f))
                                                .border(1.dp, MuzaColors.BlueGlow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 3.dp),
                                        ) {
                                            Text(
                                                text = "AR HUD",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MuzaColors.BlueSoft,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MuzaColors.BluePrimary else Color.Transparent)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) MuzaColors.BluePrimary else MuzaColors.TextMuted,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = if (isSelected) "Selected" else "Select",
                                tint = if (isSelected) Color.White else MuzaColors.TextMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        // Bottom selection summary + CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MuzaColors.Background)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "${strings.selectedPrefix} ${selectedModel.title} • ${selectedModel.priceKzt}",
                style = MaterialTheme.typography.bodyMedium,
                color = MuzaColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))
            MuzaGradientButton(
                text = strings.connectAction,
                onClick = onProceed,
                testTag = "glasses_connect_cta",
            )
        }
    }
}

@Composable
private fun GlassesModelThumbnail(
    model: RayBanModel,
    modifier: Modifier = Modifier,
) {
    val isDisplay = model.hasDisplay
    val isSport = model == RayBanModel.OAKLEY_META_HSTN || model == RayBanModel.OAKLEY_META_VANGUARD

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isDisplay) {
                    Brush.linearGradient(listOf(Color(0xFF1A2844), Color(0xFF0E1628)))
                } else {
                    Brush.linearGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0)))
                }
            )
            .border(1.dp, MuzaColors.Border, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width
            val h = size.height
            val frameColor = if (isDisplay) Color(0xFF93C5FD) else Color(0xFF111827)
            val lensColor = when {
                isDisplay -> Color(0xFF3B82F6).copy(alpha = 0.35f)
                isSport -> Color(0xFF1E3A8A).copy(alpha = 0.55f)
                else -> Color(0xFF1E293B).copy(alpha = 0.85f)
            }
            val lensW = w * 0.40f
            val lensH = h * (if (isSport) 0.44f else 0.38f)
            val topY = h * 0.30f

            // Left lens
            drawRoundRect(
                color = lensColor,
                topLeft = Offset(w * 0.05f, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(w * 0.05f, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 4.2f),
            )

            // Right lens
            drawRoundRect(
                color = lensColor,
                topLeft = Offset(w * 0.55f, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(w * 0.55f, topY),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 4.2f),
            )

            // Bridge
            drawLine(
                color = frameColor,
                start = Offset(w * 0.45f, topY + lensH * 0.30f),
                end = Offset(w * 0.55f, topY + lensH * 0.30f),
                strokeWidth = 4.5f,
            )

            // Camera LED dot or AR HUD preview inside right lens
            if (isDisplay) {
                drawRoundRect(
                    color = Color(0xFF60A5FA),
                    topLeft = Offset(w * 0.61f, topY + lensH * 0.25f),
                    size = Size(lensW * 0.65f, lensH * 0.45f),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            } else {
                drawCircle(
                    color = Color(0xFF3B82F6),
                    radius = 3.2f,
                    center = Offset(w * 0.12f, topY + 6f),
                )
            }
        }
    }
}
