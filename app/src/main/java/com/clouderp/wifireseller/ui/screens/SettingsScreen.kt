package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.SettingsUpdateRequest
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.data.cashIssueDescription
import com.clouderp.wifireseller.data.cashIssueLabel
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.MetaText
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SectionLabel
import com.clouderp.wifireseller.ui.components.SettingSwitch
import com.clouderp.wifireseller.ui.components.SoftCard
import com.clouderp.wifireseller.ui.components.StatusChip
import kotlinx.coroutines.launch

private val CashModes = listOf("auto", "preview", "manual")

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
    var paynowConfigured by remember { mutableStateOf(false) }
    var pesepayEnabled by remember { mutableStateOf(false) }
    var pesepayKey by remember { mutableStateOf("") }
    var pesepayEncryption by remember { mutableStateOf("") }
    var pesepayConfigured by remember { mutableStateOf(false) }
    var ecocashEnabled by remember { mutableStateOf(false) }
    var ecocashUsername by remember { mutableStateOf("") }
    var ecocashPassword by remember { mutableStateOf("") }
    var ecocashMerchantCode by remember { mutableStateOf("") }
    var ecocashPin by remember { mutableStateOf("") }
    var ecocashNumber by remember { mutableStateOf("") }
    var ecocashBaseUrl by remember { mutableStateOf("") }
    var ecocashMerchantName by remember { mutableStateOf("") }
    var ecocashTerminalId by remember { mutableStateOf("") }
    var ecocashLocation by remember { mutableStateOf("") }
    var ecocashConfigured by remember { mutableStateOf(false) }
    var usernameSet by remember { mutableStateOf(false) }
    var passwordSet by remember { mutableStateOf(false) }
    var pinSet by remember { mutableStateOf(false) }
    var paynowKeySet by remember { mutableStateOf(false) }
    var pesepayKeySet by remember { mutableStateOf(false) }
    var pesepayEncSet by remember { mutableStateOf(false) }
    var browserNotices by remember { mutableStateOf(true) }
    var smsWarnings by remember { mutableStateOf(false) }
    var warnPercent by remember { mutableStateOf("80") }
    var warnMinutes by remember { mutableStateOf("15") }
    var cashIssueMode by remember { mutableStateOf("preview") }
    var cashShowQr by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repository.settings() }
            .onSuccess {
                paynowEnabled = it.paynowEnabled
                paynowId = it.paynowIntegrationId.orEmpty()
                paynowConfigured = it.paynowConfigured
                paynowKeySet = it.paynowIntegrationKeySet
                pesepayEnabled = it.pesepayEnabled
                pesepayConfigured = it.pesepayConfigured
                pesepayKeySet = it.pesepayIntegrationKeySet
                pesepayEncSet = it.pesepayEncryptionKeySet
                ecocashEnabled = it.ecocashEnabled
                ecocashConfigured = it.ecocashConfigured
                ecocashMerchantCode = it.ecocashMerchantCode.orEmpty()
                ecocashNumber = it.ecocashMerchantNumber.orEmpty()
                ecocashBaseUrl = it.ecocashBaseUrl.orEmpty()
                ecocashMerchantName = it.ecocashMerchantName.orEmpty()
                ecocashTerminalId = it.ecocashTerminalId.orEmpty()
                ecocashLocation = it.ecocashLocation.orEmpty()
                usernameSet = it.ecocashUsernameSet
                passwordSet = it.ecocashPasswordSet
                pinSet = it.ecocashMerchantPinSet
                browserNotices = it.guestBrowserNoticesEnabled
                smsWarnings = it.guestSmsWarningsEnabled
                warnPercent = it.guestWarningPercent.toString()
                warnMinutes = it.guestExpiryWarnMinutes.toString()
                cashIssueMode = it.cashIssueMode
                cashShowQr = it.cashShowQr
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ErrorText(error)
                if (!status.isNullOrBlank()) Text(status!!, color = MaterialTheme.colorScheme.primary)

                SectionLabel("Paynow")
                if (paynowConfigured) StatusChip("Configured")
                SettingSwitch("Enable Paynow", paynowEnabled) { paynowEnabled = it }
                AppTextField(paynowId, { paynowId = it }, "Paynow integration ID")
                AppTextField(
                    paynowKey,
                    { paynowKey = it },
                    "Paynow key",
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = if (paynowKeySet) "Leave blank to keep" else null,
                )

                SectionLabel("Pesepay")
                if (pesepayConfigured) StatusChip("Configured")
                SettingSwitch("Enable Pesepay", pesepayEnabled) { pesepayEnabled = it }
                AppTextField(
                    pesepayKey,
                    { pesepayKey = it },
                    "Integration key",
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = if (pesepayKeySet) "Leave blank to keep" else null,
                )
                AppTextField(
                    pesepayEncryption,
                    { pesepayEncryption = it },
                    "Encryption key",
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = if (pesepayEncSet) "Leave blank to keep" else null,
                )

                SectionLabel("EcoCash")
                if (ecocashConfigured) StatusChip("Configured")
                SettingSwitch("Enable EcoCash", ecocashEnabled) { ecocashEnabled = it }
                AppTextField(
                    ecocashUsername,
                    { ecocashUsername = it },
                    "Username",
                    placeholder = if (usernameSet) "Leave blank to keep" else null,
                )
                AppTextField(
                    ecocashPassword,
                    { ecocashPassword = it },
                    "Password",
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = if (passwordSet) "Leave blank to keep" else null,
                )
                AppTextField(ecocashMerchantCode, { ecocashMerchantCode = it }, "Merchant code")
                AppTextField(
                    ecocashPin,
                    { ecocashPin = it },
                    "Merchant PIN",
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = if (pinSet) "Leave blank to keep" else null,
                )
                AppTextField(ecocashNumber, { ecocashNumber = it }, "Merchant number")
                AppTextField(ecocashBaseUrl, { ecocashBaseUrl = it }, "Base URL")
                AppTextField(ecocashMerchantName, { ecocashMerchantName = it }, "Merchant name")
                AppTextField(ecocashTerminalId, { ecocashTerminalId = it }, "Terminal ID")
                AppTextField(ecocashLocation, { ecocashLocation = it }, "Location")

                SectionLabel("Guest notices")
                SettingSwitch("Browser guest notices", browserNotices) { browserNotices = it }
                SettingSwitch("SMS guest warnings", smsWarnings) { smsWarnings = it }
                AppTextField(warnPercent, { warnPercent = it }, "Warn at data %", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                AppTextField(warnMinutes, { warnMinutes = it }, "Warn before expiry (minutes)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                SectionLabel("Cash voucher output")
                MetaText("After a cash sale the voucher slip always opens on screen.")
                CashModes.forEach { mode ->
                    SoftCard(highlighted = cashIssueMode == mode, onClick = { cashIssueMode = mode }) {
                        Text(cashIssueLabel(mode), style = MaterialTheme.typography.titleMedium)
                        MetaText(cashIssueDescription(mode))
                    }
                }
                SettingSwitch(
                    "Show QR of the hotspot code",
                    cashShowQr,
                    "Include a scannable QR of the code on the slip.",
                ) { cashShowQr = it }

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
                                    pesepayIntegrationKey = pesepayKey.ifBlank { null },
                                    pesepayEncryptionKey = pesepayEncryption.ifBlank { null },
                                    ecocashEnabled = ecocashEnabled,
                                    ecocashUsername = ecocashUsername.ifBlank { null },
                                    ecocashPassword = ecocashPassword.ifBlank { null },
                                    ecocashMerchantCode = ecocashMerchantCode.ifBlank { null },
                                    ecocashMerchantPin = ecocashPin.ifBlank { null },
                                    ecocashMerchantNumber = ecocashNumber.ifBlank { null },
                                    ecocashBaseUrl = ecocashBaseUrl.ifBlank { null },
                                    ecocashMerchantName = ecocashMerchantName.ifBlank { null },
                                    ecocashTerminalId = ecocashTerminalId.ifBlank { null },
                                    ecocashLocation = ecocashLocation.ifBlank { null },
                                    guestBrowserNoticesEnabled = browserNotices,
                                    guestSmsWarningsEnabled = smsWarnings,
                                    guestWarningPercent = warnPercent.toIntOrNull(),
                                    guestExpiryWarnMinutes = warnMinutes.toIntOrNull(),
                                    cashIssueMode = cashIssueMode,
                                    cashShowQr = cashShowQr,
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
