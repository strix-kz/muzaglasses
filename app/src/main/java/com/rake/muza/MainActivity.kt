package com.rake.muza

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rake.muza.core.LanguageManager
import com.rake.muza.core.MuzaFeatures
import com.rake.muza.data.firebase.configureAppCheck
import com.rake.muza.navigation.MuzaAppRoot
import com.rake.muza.ui.theme.MuzaGlassesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // CRITICAL: App Check MUST be configured before any Firebase operation or ViewModel init.
        configureAppCheck(this, intent)

        LanguageManager.init(this)
        MuzaFeatures.init(this)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.parseColor("#11131D")),
        )

        setContent {
            MuzaGlassesTheme {
                MuzaAppRoot()
            }
        }
    }
}
