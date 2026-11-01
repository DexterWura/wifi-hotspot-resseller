package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.SettingsUpdateRequest
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.PrimaryButton
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(repository: WifiRepository) {
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    var paynowEnabled by remember { mutableStateOf(false) }
    var paynowId by remember { mutableStateOf("") }
    var paynowKey by remember { mutableStateOf("") }
    var pesepayEnabled by remember { mutableStateOf(false) }
    var ecocashEnabled by remember { mutableStateOf(false) }
    var browserNotices by remember { mutableStateOf(true) }
    var smsWarnings by remember { mutableStateOf(false) }
    var warnPercent by remember { mutableStateOf("80") }
    var warnMinutes by remember { mutableStateOf("15") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repository.settings() }
            .onSuccess {
                paynowEnabled = it.paynowEnabled
                paynowId = it.paynowIntegrationId.orEmpty()
                pesepayEnabled = it.pesepayEnabled
                ecocashEnabled = it.ecocashEnabled
                browserNotices = it.guestBrowserNoticesEnabled
                smsWarnings = it.guestSmsWarningsEnabled
                warnPercent = it.guestWarningPercent.toString()
                warnMinutes = it.guestExpiryWarnMinutes.toString()
                loading = false
            }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        if (loading) LoadingBox() else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ErrorText(error)
                if (!status.isNullOrBlank()) Text(status!!, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                SettingSwitch("Enable Paynow", paynowEnabled) { paynowEnabled = it }
                AppTextField(paynowId, { paynowId = it }, "Paynow integration ID")
                AppTextField(paynowKey, { paynowKey = it }, "Paynow key (blank keeps current)")
                SettingSwitch("Enable Pesepay", pesepayEnabled) { pesepayEnabled = it }
                SettingSwitch("Enable EcoCash", ecocashEnabled) { ecocashEnabled = it }
                SettingSwitch("Browser guest notices", browserNotices) { browserNotices = it }
                SettingSwitch("SMS guest warnings", smsWarnings) { smsWarnings = it }
                AppTextField(warnPercent, { warnPercent = it }, "Warn at data %")
                AppTextField(warnMinutes, { warnMinutes = it }, "Warn before expiry (minutes)")
                PrimaryButton(if (saving) "Saving..." else "Save settings", enabled = !saving) {
                    scope.launch {
                        saving = true
                        error = null
                        status = null
                        runCatching {
                            repository.updateSettings(
                                SettingsUpdateRequest(
                                    paynowEnabled = paynowEnabled,
                                    paynowIntegrationId = paynowId.ifBlank { null },
                                    paynowIntegrationKey = paynowKey.ifBlank { null },
                                    pesepayEnabled = pesepayEnabled,
                                    ecocashEnabled = ecocashEnabled,
                                    guestBrowserNoticesEnabled = browserNotices,
                                    guestSmsWarningsEnabled = smsWarnings,
                                    guestWarningPercent = warnPercent.toIntOrNull(),
                                    guestExpiryWarnMinutes = warnMinutes.toIntOrNull(),
                                ),
                            )
                        }.onSuccess { status = "Settings saved." }
                            .onFailure { error = it.message }
                        saving = false
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
