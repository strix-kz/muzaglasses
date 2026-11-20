package com.rake.muza.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rake.muza.core.DisplayHudPayload
import com.rake.muza.core.DisplayHudType
import com.rake.muza.ui.theme.MuzaColors
import com.rake.muza.ui.theme.MuzaLogoFontFamily

/**
 * Persistent top-left Muza Glasses brand mark used consistently on EVERY screen:
 * "muza" large handwritten rounded blue gradient on top, "glasses" smaller underneath.
 * Intentionally contains NO glasses icon inside the logo.
 */
@Composable
fun MuzaLogo(
    modifier: Modifier = Modifier,
    titleSize: TextUnit = 34.sp,
    subtitleSize: TextUnit = 20.sp,
) {
    Column(
        modifier = modifier.testTag("muza_logo"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "muza",
            style = TextStyle(
                fontFamily = MuzaLogoFontFamily,
                fontSize = titleSize,
                fontStyle = FontStyle.Italic,
                lineHeight = titleSize * 0.92f,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MuzaColors.BlueDeep,
                        MuzaColors.BluePrimary,
                        MuzaColors.BlueGlow,
                    )
                ),
            ),
            maxLines = 1,
        )
        Text(
            text = "glasses",
            modifier = Modifier.padding(top = 0.dp),
            style = TextStyle(
                fontFamily = MuzaLogoFontFamily,
                fontSize = subtitleSize,
                fontStyle = FontStyle.Italic,
                lineHeight = subtitleSize * 0.95f,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MuzaColors.BlueDeep,
                        MuzaColors.BluePrimary,
                        MuzaColors.BlueSoft,
                    )
                ),
            ),
            maxLines = 1,
        )
    }
}

/**
 * Smooth animated handwriting splash logo: progressively writes "muza" then "glasses"
 * from left to right with a subtle electric-blue luminous ink stroke.
 */
@Composable
fun MuzaAnimatedSplashLogo(
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit = {},
) {
    val muzaReveal = remember { Animatable(0f) }
    val glassesReveal = remember { Animatable(0f) }
    val glowAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        muzaReveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
        )
        glassesReveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 820, easing = EaseInOutCubic),
        )
        glowAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420),
        )
        onAnimationFinished()
    }

    Column(
        modifier = modifier.testTag("muza_animated_splash_logo"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.drawWithContent {
                val revealWidth = size.width * muzaReveal.value
                clipRect(left = 0f, top = 0f, right = revealWidth, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
                if (muzaReveal.value in 0.03f..0.97f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(MuzaColors.BlueGlow, Color.Transparent),
                            center = Offset(revealWidth, size.height * 0.55f),
                            radius = 28.dp.toPx(),
                        ),
                        radius = 28.dp.toPx(),
                        center = Offset(revealWidth, size.height * 0.55f),
                    )
                }
            }
        ) {
            Text(
                text = "muza",
                style = TextStyle(
                    fontFamily = MuzaLogoFontFamily,
                    fontSize = 74.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 68.sp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MuzaColors.BlueDeep,
                            MuzaColors.BluePrimary,
                            MuzaColors.BlueGlow,
                        )
                    ),
                ),
                maxLines = 1,
            )
        }

        Box(
            modifier = Modifier.drawWithContent {
                val revealWidth = size.width * glassesReveal.value
                clipRect(left = 0f, top = 0f, right = revealWidth, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
                if (glassesReveal.value in 0.03f..0.97f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(MuzaColors.BlueSoft, Color.Transparent),
                            center = Offset(revealWidth, size.height * 0.5f),
                            radius = 20.dp.toPx(),
                        ),
                        radius = 20.dp.toPx(),
                        center = Offset(revealWidth, size.height * 0.5f),
                    )
                }
            }
        ) {
            Text(
                text = "glasses",
                style = TextStyle(
                    fontFamily = MuzaLogoFontFamily,
                    fontSize = 44.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 44.sp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MuzaColors.BlueDeep,
                            MuzaColors.BluePrimary,
                            MuzaColors.BlueSoft,
                        )
                    ),
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
fun MuzaGradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    testTag: String = "muza_primary_button",
    height: Dp = 56.dp,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .testTag(testTag)
            .clip(shape)
            .background(
                if (enabled) MuzaColors.ButtonGradient
                else Brush.horizontalGradient(listOf(MuzaColors.SurfaceElevated, MuzaColors.SurfaceElevated))
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            contentColor = MuzaColors.TextPrimary,
            disabledContentColor = MuzaColors.TextMuted,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        shape = shape,
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun MuzaCard(
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    cornerRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderColor = if (isHighlighted) MuzaColors.BorderActive else MuzaColors.Border
    val bgBrush = if (isHighlighted) MuzaColors.CardGlowGradient else Brush.verticalGradient(
        listOf(MuzaColors.Surface, MuzaColors.Surface)
    )
    Column(
        modifier = modifier
            .clip(shape)
            .background(bgBrush)
            .border(width = if (isHighlighted) 1.5.dp else 1.dp, color = borderColor, shape = shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        content = content,
    )
}

/**
 * Glowing holographic Wayfarer smart-glasses wireframe with AR corner framing brackets.
 * Uses DrawScope size properties exclusively so it adapts cleanly to any screen size.
 */
@Composable
fun HolographicGlassesCanvas(
    modifier: Modifier = Modifier,
    showCornerBrackets: Boolean = true,
) {
    val infinite = rememberInfiniteTransition(label = "holo_glasses")
    val pulse by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padX = w * 0.06f
        val topY = h * 0.16f
        val botY = h * 0.84f
        val midX = w * 0.5f
        val bridgeW = w * 0.10f
        val lensW = (w - padX * 2f - bridgeW) / 2f

        val outerLeft = Path().apply {
            moveTo(padX, topY + h * 0.06f)
            cubicTo(
                padX + lensW * 0.25f, topY - h * 0.04f,
                padX + lensW * 0.82f, topY - h * 0.02f,
                midX - bridgeW / 2f, topY + h * 0.12f,
            )
            cubicTo(
                midX - bridgeW / 2f - lensW * 0.06f, botY - h * 0.04f,
                padX + lensW * 0.28f, botY + h * 0.03f,
                padX + lensW * 0.08f, botY - h * 0.18f,
            )
            cubicTo(
                padX - w * 0.01f, topY + h * 0.42f,
                padX - w * 0.01f, topY + h * 0.22f,
                padX, topY + h * 0.06f,
            )
            close()
        }

        val outerRight = Path().apply {
            val rx = midX + bridgeW / 2f
            moveTo(rx, topY + h * 0.12f)
            cubicTo(
                rx + lensW * 0.18f, topY - h * 0.02f,
                rx + lensW * 0.75f, topY - h * 0.04f,
                w - padX, topY + h * 0.06f,
            )
            cubicTo(
                w - padX + w * 0.01f, topY + h * 0.22f,
                w - padX + w * 0.01f, topY + h * 0.42f,
                w - padX - lensW * 0.08f, botY - h * 0.18f,
            )
            cubicTo(
                rx + lensW * 0.72f, botY + h * 0.03f,
                rx + lensW * 0.06f, botY - h * 0.04f,
                rx, topY + h * 0.12f,
            )
            close()
        }

        val bridgePath = Path().apply {
            moveTo(midX - bridgeW * 0.62f, topY + h * 0.14f)
            quadraticTo(midX, topY + h * 0.06f, midX + bridgeW * 0.62f, topY + h * 0.14f)
            moveTo(midX - bridgeW * 0.45f, topY + h * 0.28f)
            quadraticTo(midX, topY + h * 0.20f, midX + bridgeW * 0.45f, topY + h * 0.28f)
        }

        val glowColor = MuzaColors.BluePrimary.copy(alpha = 0.28f * pulse)
        val mainStrokeColor = MuzaColors.BlueGlow.copy(alpha = 0.92f * pulse)
        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)

        // Outer neon halo
        drawPath(outerLeft, color = glowColor, style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(outerRight, color = glowColor, style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(bridgePath, color = glowColor, style = Stroke(width = 10f, cap = StrokeCap.Round))

        // Crisp dashed blueprint wireframe
        drawPath(outerLeft, color = mainStrokeColor, style = Stroke(width = 3.2f, pathEffect = dashEffect, cap = StrokeCap.Round))
        drawPath(outerRight, color = mainStrokeColor, style = Stroke(width = 3.2f, pathEffect = dashEffect, cap = StrokeCap.Round))
        drawPath(bridgePath, color = mainStrokeColor, style = Stroke(width = 3.2f, cap = StrokeCap.Round))

        // Inner lens rim highlight
        drawPath(
            outerLeft,
            color = MuzaColors.BlueSoft.copy(alpha = 0.35f * pulse),
            style = Stroke(width = 1.4f),
        )
        drawPath(
            outerRight,
            color = MuzaColors.BlueSoft.copy(alpha = 0.35f * pulse),
            style = Stroke(width = 1.4f),
        )

        if (showCornerBrackets) {
            val bracketLen = w * 0.075f
            val bracketColor = MuzaColors.BluePrimary.copy(alpha = 0.85f)
            val strokeW = 3.5f

            // Top-left & bottom-left
            drawLine(bracketColor, Offset(padX * 0.5f, topY * 0.65f), Offset(padX * 0.5f + bracketLen, topY * 0.65f), strokeW)
            drawLine(bracketColor, Offset(padX * 0.5f, topY * 0.65f), Offset(padX * 0.5f, topY * 0.65f + bracketLen), strokeW)
            drawLine(bracketColor, Offset(padX * 0.5f, botY * 1.04f), Offset(padX * 0.5f + bracketLen, botY * 1.04f), strokeW)
            drawLine(bracketColor, Offset(padX * 0.5f, botY * 1.04f), Offset(padX * 0.5f, botY * 1.04f - bracketLen), strokeW)

            // Top-right & bottom-right
            drawLine(bracketColor, Offset(w - padX * 0.5f, topY * 0.65f), Offset(w - padX * 0.5f - bracketLen, topY * 0.65f), strokeW)
            drawLine(bracketColor, Offset(w - padX * 0.5f, topY * 0.65f), Offset(w - padX * 0.5f, topY * 0.65f + bracketLen), strokeW)
            drawLine(bracketColor, Offset(w - padX * 0.5f, botY * 1.04f), Offset(w - padX * 0.5f - bracketLen, botY * 1.04f), strokeW)
            drawLine(bracketColor, Offset(w - padX * 0.5f, botY * 1.04f), Offset(w - padX * 0.5f, botY * 1.04f - bracketLen), strokeW)
        }
    }
}

/**
 * Corner-positioned Meta Display Glasses HUD overlay.
 * Displays compact, glanceable text (Navigation, Translation, AI Answer, Messages, Recording)
 * without cluttering or blocking the live view.
 */
@Composable
fun DisplayHudOverlay(
    hud: DisplayHudPayload,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = hud.isVisible,
        enter = fadeIn() + slideInVertically { -it / 3 },
        exit = fadeOut() + slideOutVertically { -it / 3 },
        modifier = modifier,
    ) {
        val accentColor = when (hud.type) {
            DisplayHudType.RECORDING -> MuzaColors.Danger
            DisplayHudType.PHOTO_CAPTURED -> MuzaColors.Emerald
            DisplayHudType.NAVIGATION -> MuzaColors.BlueGlow
            DisplayHudType.TRANSLATION -> MuzaColors.EmeraldSoft
            DisplayHudType.AI_ANSWER -> MuzaColors.BluePrimary
            DisplayHudType.MESSAGE -> MuzaColors.Emerald
            else -> MuzaColors.BlueGlow
        }

        Surface(
            modifier = Modifier
                .width(264.dp)
                .testTag("display_hud_overlay"),
            shape = RoundedCornerShape(16.dp),
            color = MuzaColors.HudBackground,
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hud.header,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = accentColor,
                                fontWeight = FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("dismiss_hud_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Dismiss HUD",
                            tint = MuzaColors.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (hud.primaryLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hud.primaryLine,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MuzaColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hud.secondaryLine.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = hud.secondaryLine,
                        style = MaterialTheme.typography.bodyMedium.copy(color = MuzaColors.BlueSoft),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (hud.footerMeta.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hud.footerMeta,
                        style = MaterialTheme.typography.labelSmall.copy(color = MuzaColors.TextSecondary),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
