package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.ThemePreference
import com.clouderp.wifireseller.ui.components.ChoiceChip
import com.clouderp.wifireseller.ui.components.DangerButton
import com.clouderp.wifireseller.ui.components.MetaText
import com.clouderp.wifireseller.ui.components.SectionLabel
import com.clouderp.wifireseller.ui.components.SoftCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    companyName: String,
    userName: String?,
    userEmail: String?,
    canOpenLive: Boolean,
    canOpenSales: Boolean,
    canOpenSites: Boolean,
    canOpenPlans: Boolean,
    canOpenSettings: Boolean,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    onOpenLive: () -> Unit,
    onOpenSales: () -> Unit,
    onOpenSites: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("More") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SoftCard {
                Text(userName ?: "Signed in", style = MaterialTheme.typography.titleMedium)
                if (!userEmail.isNullOrBlank()) MetaText(userEmail)
                MetaText(companyName)
            }
            SectionLabel("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("System", themePreference == ThemePreference.System) {
                    onThemeChange(ThemePreference.System)
                }
                ChoiceChip("Light", themePreference == ThemePreference.Light) {
                    onThemeChange(ThemePreference.Light)
                }
                ChoiceChip("Dark", themePreference == ThemePreference.Dark) {
                    onThemeChange(ThemePreference.Dark)
                }
            }
            SectionLabel("Workspace")
            if (canOpenLive) MoreLink("Live monitor", Icons.Default.Speed, onOpenLive)
            if (canOpenSales) MoreLink("Sales report", Icons.AutoMirrored.Filled.List, onOpenSales)
            if (canOpenSites) MoreLink("Sites", Icons.Default.Place, onOpenSites)
            if (canOpenPlans) MoreLink("Plans", Icons.Default.Wifi, onOpenPlans)
            if (canOpenSettings) MoreLink("Settings", Icons.Default.Settings, onOpenSettings)
            DangerButton("Sign out", onClick = onLogout)
        }
    }
}

@Composable
private fun MoreLink(label: String, icon: ImageVector, onClick: () -> Unit) {
    SoftCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}
