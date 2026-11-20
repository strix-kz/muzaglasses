package com.rake.muza.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.rake.muza.core.AppLanguage
import com.rake.muza.core.LanguageManager
import com.rake.muza.data.firebase.attemptSilentGoogleSignIn
import com.rake.muza.data.firebase.launchInteractiveGoogleSignIn
import com.rake.muza.ui.components.MuzaGradientButton
import com.rake.muza.ui.components.MuzaLogo
import com.rake.muza.ui.theme.MuzaColors

@Composable
fun AuthScreen(
    onSignedIn: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val strings by LanguageManager.strings.collectAsState()
    val currentLang by LanguageManager.language.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptSilentGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = onSignedIn,
            onUnauthenticated = {},
        )
    }

    fun triggerGoogleSignIn() {
        if (isSigningIn) return
        isSigningIn = true
        infoMessage = null
        launchInteractiveGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            scope = scope,
            onAuthSuccess = {
                isSigningIn = false
                onSignedIn()
            },
            onAuthError = { err ->
                isSigningIn = false
                infoMessage = err
            },
            onAuthCancelled = {
                isSigningIn = false
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("auth_screen"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            // Top bar: persistent top-left logo + 3-language quick toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                MuzaLogo(titleSize = 38.sp, subtitleSize = 22.sp)

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(MuzaColors.Surface)
                        .border(1.dp, MuzaColors.Border, RoundedCornerShape(14.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val active = lang == currentLang
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (active) MuzaColors.BluePrimary else Color.Transparent)
                                .clickable { LanguageManager.setLanguage(lang) }
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                                .testTag("auth_lang_${lang.code.lowercase()}"),
                        ) {
                            Text(
                                text = lang.code,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (active) Color.White else MuzaColors.TextSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = strings.welcomeTitle,
                style = MaterialTheme.typography.displayLarge,
                color = MuzaColors.TextPrimary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.signInSubtitle,
                style = MaterialTheme.typography.titleLarge,
                color = MuzaColors.BlueSoft,
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Google Sign-In white pill button (matching screenshot #1)
            Button(
                onClick = { triggerGoogleSignIn() },
                enabled = !isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(29.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF111827),
                    disabledContainerColor = Color.White.copy(alpha = 0.7f),
                ),
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MuzaColors.BlueDeep,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    GoogleGMark(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = strings.signInWithGoogle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF111827),
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MuzaColors.Border)
                Text(
                    text = strings.orDivider,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuzaColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MuzaColors.Border)
            }

            Spacer(modifier = Modifier.height(22.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MuzaColors.Surface,
                unfocusedContainerColor = MuzaColors.Surface,
                focusedBorderColor = MuzaColors.BluePrimary,
                unfocusedBorderColor = MuzaColors.Border,
                focusedTextColor = MuzaColors.TextPrimary,
                unfocusedTextColor = MuzaColors.TextPrimary,
                cursorColor = MuzaColors.BlueGlow,
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_email_input"),
                placeholder = { Text(strings.emailHint, color = MuzaColors.TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = strings.emailHint,
                        tint = MuzaColors.TextSecondary,
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(28.dp),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_password_input"),
                placeholder = { Text(strings.passwordHint, color = MuzaColors.TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = strings.passwordHint,
                        tint = MuzaColors.TextSecondary,
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.testTag("toggle_password_visibility"),
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = "Toggle password",
                            tint = MuzaColors.TextSecondary,
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape = RoundedCornerShape(28.dp),
                colors = fieldColors,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = strings.forgotPassword,
                style = MaterialTheme.typography.bodyMedium,
                color = MuzaColors.BlueGlow,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { infoMessage = strings.googleAuthRequiredNotice }
                    .padding(vertical = 6.dp)
                    .testTag("forgot_password_button"),
            )

            if (!infoMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MuzaColors.BlueDeep.copy(alpha = 0.25f))
                        .border(1.dp, MuzaColors.BluePrimary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = infoMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MuzaColors.BlueSoft,
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            MuzaGradientButton(
                text = strings.signInButton,
                enabled = !isSigningIn,
                onClick = {
                    // Route sign-in cleanly through Google Credential Manager for zero-trust Firebase Auth
                    infoMessage = strings.googleAuthRequiredNotice
                    triggerGoogleSignIn()
                },
                testTag = "primary_sign_in_button",
            )

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = strings.noAccountPrompt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MuzaColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.registerLink,
                    style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline),
                    color = MuzaColors.BlueGlow,
                    modifier = Modifier
                        .clickable { triggerGoogleSignIn() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("register_google_link"),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun GoogleGMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.width * 0.19f, cap = StrokeCap.Butt)
        val inset = size.width * 0.14f
        val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
        val topLeft = Offset(inset, inset)
        // Red
        drawArc(Color(0xFFEA4335), startAngle = 215f, sweepAngle = 105f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        // Yellow
        drawArc(Color(0xFFFBBC05), startAngle = 140f, sweepAngle = 75f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        // Green
        drawArc(Color(0xFF34A853), startAngle = 45f, sweepAngle = 95f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        // Blue
        drawArc(Color(0xFF4285F4), startAngle = -15f, sweepAngle = 60f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
        // Horizontal bar
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.50f, size.height * 0.50f),
            end = Offset(size.width * 0.88f, size.height * 0.50f),
            strokeWidth = size.width * 0.18f,
        )
    }
}
