package com.rake.muza.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.rake.muza.ui.components.MuzaAnimatedSplashLogo
import com.rake.muza.ui.theme.MuzaColors
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
) {
    LaunchedEffect(Unit) {
        delay(2350L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MuzaColors.BlueDeep.copy(alpha = 0.24f),
                        Color.Transparent,
                    ),
                    radius = 700f,
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            MuzaAnimatedSplashLogo()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Smart AR • Ray-Ban & Meta Display",
                style = MaterialTheme.typography.labelLarge,
                color = MuzaColors.BlueSoft.copy(alpha = 0.78f),
            )
        }
    }
}
