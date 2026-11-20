package com.rake.muza.ui.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.rake.muza.core.DisplayHudPayload
import com.rake.muza.core.LanguageManager
import com.rake.muza.core.MuzaLocationState
import com.rake.muza.ui.components.DisplayHudOverlay
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sinh
import kotlin.math.tan

private const val TILE_PX = 256.0

@Composable
fun MapScreen(
    locationState: MuzaLocationState,
    hudState: DisplayHudPayload,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    onStartGps: () -> Unit,
    onSpeakWhereAmI: () -> Unit,
    onSendToHud: () -> Unit,
    onDismissHud: () -> Unit,
) {
    val strings by LanguageManager.strings.collectAsState()
    val context = LocalContext.current

    var zoom by remember { mutableIntStateOf(15) }
    var followUser by remember { mutableStateOf(true) }
    var darkMapStyle by remember { mutableStateOf(true) }
    var centerLat by remember { mutableDoubleStateOf(locationState.latitude) }
    var centerLon by remember { mutableDoubleStateOf(locationState.longitude) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            onStartGps()
        }
    }

    LaunchedEffect(locationState.latitude, locationState.longitude, followUser) {
        if (followUser) {
            centerLat = locationState.latitude
            centerLon = locationState.longitude
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "gps_beacon")
    val beaconPulse by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .testTag("map_screen"),
    ) {
        // 1. Real Interactive Slippy-Tile Map (CartoDB Dark Matter / OpenStreetMap)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(zoom) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        followUser = false
                        val worldSize = TILE_PX * 2.0.pow(zoom)
                        val curPx = latLonToWorldPixel(centerLat, centerLon, zoom)
                        val nextX = (curPx.first - dragAmount.x.toDouble()).coerceIn(0.0, worldSize - 1.0)
                        val nextY = (curPx.second - dragAmount.y.toDouble()).coerceIn(0.0, worldSize - 1.0)
                        val (newLat, newLon) = worldPixelToLatLon(nextX, nextY, zoom)
                        centerLat = newLat.coerceIn(-85.0, 85.0)
                        centerLon = newLon.coerceIn(-180.0, 180.0)
                    }
                }
        ) {
            val density = LocalDensity.current
            val widthPx = with(density) { maxWidth.toPx() }.toDouble()
            val heightPx = with(density) { maxHeight.toPx() }.toDouble()
            val tileSizeDp = with(density) { TILE_PX.toFloat().toDp() }

            val (centerWorldX, centerWorldY) = latLonToWorldPixel(centerLat, centerLon, zoom)
            val topLeftWorldX = centerWorldX - widthPx / 2.0
            val topLeftWorldY = centerWorldY - heightPx / 2.0

            val maxTileIndex = (1 shl zoom) - 1
            val startTileX = floor(topLeftWorldX / TILE_PX).toInt() - 1
            val endTileX = floor((topLeftWorldX + widthPx) / TILE_PX).toInt() + 1
            val startTileY = floor(topLeftWorldY / TILE_PX).toInt().coerceAtLeast(0)
            val endTileY = floor((topLeftWorldY + heightPx) / TILE_PX).toInt().coerceAtMost(maxTileIndex)

            // Render visible map tiles
            for (ty in startTileY..endTileY) {
                for (tx in startTileX..endTileX) {
                    val wrappedX = ((tx % (maxTileIndex + 1)) + (maxTileIndex + 1)) % (maxTileIndex + 1)
                    val offsetX = (tx * TILE_PX - topLeftWorldX).roundToInt()
                    val offsetY = (ty * TILE_PX - topLeftWorldY).roundToInt()
                    val tileUrl = if (darkMapStyle) {
                        "https://a.basemaps.cartocdn.com/dark_all/$zoom/$wrappedX/$ty.png"
                    } else {
                        "https://tile.openstreetmap.org/$zoom/$wrappedX/$ty.png"
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(tileUrl)
                            .addHeader("User-Agent", "MuzaGlassesAndroid/1.0 (com.rake.muza)")
                            .crossfade(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .offset { IntOffset(offsetX, offsetY) }
                            .size(tileSizeDp),
                    )
                }
            }

            // Live GPS Route Polyline + Pulsing Location Beacon Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Draw walking/driving trail
                if (locationState.trail.size >= 2) {
                    val trailPath = Path()
                    locationState.trail.forEachIndexed { idx, pt ->
                        val (wx, wy) = latLonToWorldPixel(pt.latitude, pt.longitude, zoom)
                        val sx = (wx - topLeftWorldX).toFloat()
                        val sy = (wy - topLeftWorldY).toFloat()
                        if (idx == 0) trailPath.moveTo(sx, sy) else trailPath.lineTo(sx, sy)
                    }
                    drawPath(
                        path = trailPath,
                        color = MuzaColors.BluePrimary.copy(alpha = 0.35f),
                        style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                    drawPath(
                        path = trailPath,
                        color = MuzaColors.BlueGlow,
                        style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }

                // Draw current GPS user position
                val (ux, uy) = latLonToWorldPixel(locationState.latitude, locationState.longitude, zoom)
                val userOffset = Offset((ux - topLeftWorldX).toFloat(), (uy - topLeftWorldY).toFloat())

                // Outer pulsing GPS radar halo
                drawCircle(
                    color = MuzaColors.BluePrimary.copy(alpha = 0.22f * beaconPulse),
                    radius = (34f + 22f * beaconPulse),
                    center = userOffset,
                )
                drawCircle(
                    color = MuzaColors.BlueGlow.copy(alpha = 0.45f),
                    radius = 22f,
                    center = userOffset,
                    style = Stroke(width = 2.5f),
                )
                // White ring + electric blue core
                drawCircle(
                    color = Color.White,
                    radius = 12f,
                    center = userOffset,
                )
                drawCircle(
                    color = MuzaColors.BluePrimary,
                    radius = 8.5f,
                    center = userOffset,
                )
            }
        }

        // Top dark scrim for crisp logo & GPS pill readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(145.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(MuzaColors.Background.copy(alpha = 0.90f), Color.Transparent)
                    )
                )
        )

        // 2. Top Header: Persistent Top-Left Logo + Live GPS Status Pill + HUD Overlay
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
                MuzaLogo(titleSize = 30.sp, subtitleSize = 18.sp)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MuzaColors.HudBackground)
                        .border(
                            1.dp,
                            if (locationState.hasGpsFix) MuzaColors.Emerald else MuzaColors.Warning,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable {
                            if (!hasLocationPermission) onRequestLocationPermission() else onStartGps()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("map_gps_status_pill"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GpsFixed,
                        contentDescription = strings.mapTitle,
                        tint = if (locationState.hasGpsFix) MuzaColors.Emerald else MuzaColors.Warning,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (locationState.hasGpsFix) strings.gpsActiveBadge else strings.gpsSearchingBadge,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MuzaColors.TextPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                DisplayHudOverlay(
                    hud = hudState,
                    onDismiss = onDismissHud,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }

        // 3. Right-side Floating Map Controls (Zoom +, Zoom -, Recenter GPS, Dark/Light map)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MapFloatingButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Zoom in",
                testTag = "map_zoom_in",
                onClick = { zoom = (zoom + 1).coerceAtMost(18) },
            )
            MapFloatingButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Zoom out",
                testTag = "map_zoom_out",
                onClick = { zoom = (zoom - 1).coerceAtLeast(10) },
            )
            MapFloatingButton(
                icon = Icons.Rounded.MyLocation,
                contentDescription = strings.recenterMapAction,
                testTag = "map_recenter_button",
                isActive = followUser,
                onClick = {
                    if (!hasLocationPermission) {
                        onRequestLocationPermission()
                    } else {
                        onStartGps()
                        followUser = true
                        centerLat = locationState.latitude
                        centerLon = locationState.longitude
                    }
                },
            )
            MapFloatingButton(
                icon = if (darkMapStyle) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = "Toggle map style",
                testTag = "map_style_toggle",
                onClick = { darkMapStyle = !darkMapStyle },
            )
        }

        // 4. Bottom GPS Telemetry & Voice Navigation Card
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MuzaColors.HudBackground,
            border = BorderStroke(1.dp, MuzaColors.BorderActive.copy(alpha = 0.55f)),
            shadowElevation = 10.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MuzaColors.BluePrimary.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocationOn,
                            contentDescription = "Current Location",
                            tint = MuzaColors.BlueGlow,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = locationState.streetAddress,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MuzaColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.5f, %.5f • %s: %.1f km/h • %s: ±%.0fm",
                                locationState.latitude,
                                locationState.longitude,
                                strings.speedMetricLabel,
                                locationState.speedKmh,
                                strings.accuracyMetricLabel,
                                locationState.accuracyMeters.coerceAtLeast(5f),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MuzaColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!hasLocationPermission) {
                    MuzaGradientButton(
                        text = "GPS рұқсатын қосу",
                        onClick = onRequestLocationPermission,
                        testTag = "map_request_gps_permission",
                        height = 48.dp,
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        MuzaGradientButton(
                            text = strings.whereAmIAction,
                            onClick = onSpeakWhereAmI,
                            testTag = "map_where_am_i_voice_button",
                            height = 48.dp,
                            modifier = Modifier.weight(1f),
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = strings.whereAmIAction,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(onClick = onSendToHud)
                                .testTag("map_send_hud_button"),
                            shape = RoundedCornerShape(24.dp),
                            color = MuzaColors.SurfaceElevated,
                            border = BorderStroke(1.dp, MuzaColors.BlueGlow.copy(alpha = 0.65f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Navigation,
                                    contentDescription = strings.sendToHudAction,
                                    tint = MuzaColors.BlueGlow,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.sendToHudAction,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MuzaColors.TextPrimary,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapFloatingButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    isActive: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = CircleShape,
        color = if (isActive) MuzaColors.BluePrimary else MuzaColors.HudBackground,
        border = BorderStroke(1.dp, if (isActive) MuzaColors.BlueGlow else MuzaColors.Border),
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private fun latLonToWorldPixel(lat: Double, lon: Double, zoom: Int): Pair<Double, Double> {
    val scale = TILE_PX * 2.0.pow(zoom)
    val x = (lon + 180.0) / 360.0 * scale
    val sinLat = kotlin.math.sin(lat * PI / 180.0).coerceIn(-0.9999, 0.9999)
    val y = (0.5 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (4.0 * PI)) * scale
    return x to y
}

private fun worldPixelToLatLon(x: Double, y: Double, zoom: Int): Pair<Double, Double> {
    val scale = TILE_PX * 2.0.pow(zoom)
    val lon = x / scale * 360.0 - 180.0
    val n = PI - 2.0 * PI * y / scale
    val lat = 180.0 / PI * atan(sinh(n))
    return lat to lon
}
