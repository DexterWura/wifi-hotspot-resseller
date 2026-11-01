package com.clouderp.wifireseller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.clouderp.wifireseller.ui.navigation.AppRoot
import com.clouderp.wifireseller.ui.theme.WifiResellerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as WifiResellerApp
        setContent {
            WifiResellerTheme {
                AppRoot(
                    repository = app.repository,
                    sessionStore = app.sessionStore,
                    onAuthChanged = { token, company -> app.syncAuth(token, company) },
                )
            }
        }
    }
}
