package com.clouderp.wifireseller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.clouderp.wifireseller.data.ThemePreference
import com.clouderp.wifireseller.ui.navigation.AppRoot
import com.clouderp.wifireseller.ui.theme.WifiResellerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WifiResellerApp
        setContent {
            val themePreference by app.themeStore.preference.collectAsState(initial = ThemePreference.System)
            WifiResellerTheme(preference = themePreference) {
                AppRoot(
                    repository = app.repository,
                    sessionStore = app.sessionStore,
                    themeStore = app.themeStore,
                    themePreference = themePreference,
                    onAuthChanged = { token, company -> app.syncAuth(token, company) },
                )
            }
        }
    }
}
