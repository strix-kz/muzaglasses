package com.rake.muza.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rake.muza.core.MuzaFeatures
import com.rake.muza.ui.theme.MuzaColors
import com.rake.muza.viewmodel.CameraFilterMode
import java.util.concurrent.Executors

private const val TAG = "MuzaCameraPreview"

@Composable
fun CameraPreviewBox(
    hasCameraPermission: Boolean,
    isFlashOn: Boolean,
    brightness: Float,
    filterMode: CameraFilterMode,
    onFrameAvailable: (Bitmap) -> Unit,
    onRequestCameraPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraEnabled = MuzaFeatures.cameraLivePreview

    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var lastFrameDispatchMs by remember { mutableStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(boundCamera, isFlashOn) {
        runCatching {
            boundCamera?.cameraControl?.enableTorch(isFlashOn)
        }
    }

    Box(modifier = modifier.background(MuzaColors.Background)) {
        val granted = hasCameraPermission ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        if (granted && cameraEnabled && cameraError == null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                        try {
                                            val now = System.currentTimeMillis()
                                            if (now - lastFrameDispatchMs >= 750L) {
                                                lastFrameDispatchMs = now
                                                val rawBmp = imageProxy.toBitmap()
                                                val rot = imageProxy.imageInfo.rotationDegrees
                                                val oriented = if (rot != 0) {
                                                    val matrix = Matrix().apply { postRotate(rot.toFloat()) }
                                                    Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
                                                } else {
                                                    rawBmp
                                                }
                                                onFrameAvailable(oriented)
                                            }
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Frame capture skipped: ${e.message}")
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            } else {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            }

                            cameraProvider.unbindAll()
                            boundCamera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageAnalysis,
                            )
                            boundCamera?.cameraControl?.enableTorch(isFlashOn)
                        } catch (e: Exception) {
                            Log.w(TAG, "CameraX unavailable on this device: ${e.message}")
                            cameraError = e.localizedMessage ?: "Camera unavailable"
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )
        } else {
            // Graceful fallback studio backdrop when camera permission isn't granted yet or disabled
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0F1E),
                                Color(0xFF101930),
                                MuzaColors.Background,
                            )
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = "Camera preview status",
                        tint = MuzaColors.BlueGlow,
                        modifier = Modifier.size(44.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (!granted) "Камераға рұқсат беріңіз" else (cameraError ?: "Тікелей камера өшірулі"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MuzaColors.TextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    if (!granted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        MuzaGradientButton(
                            text = "Рұқсат беру",
                            onClick = onRequestCameraPermission,
                            testTag = "grant_camera_permission_button",
                            height = 48.dp,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )
                    }
                }
            }
        }

        // Brightness & color filter subtle overlays
        val tintOverlay = when (filterMode) {
            CameraFilterMode.NATURAL -> Color.Transparent
            CameraFilterMode.WARM -> Color(0x22F59E0B)
            CameraFilterMode.CONTRAST -> Color(0x280047BB)
        }
        val dimAlpha = ((1f - brightness) * 0.55f).coerceIn(0f, 0.55f)
        if (tintOverlay != Color.Transparent || dimAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tintOverlay)
                    .background(Color.Black.copy(alpha = dimAlpha))
            )
        }
    }
}
