package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clouderp.wifireseller.data.PlanDto
import com.clouderp.wifireseller.data.SiteSummary
import com.clouderp.wifireseller.data.VoucherDto
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SimpleList
import com.clouderp.wifireseller.ui.components.SoftCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellScreen(repository: WifiRepository) {
    var loading by remember { mutableStateOf(true) }
    var selling by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sites by remember { mutableStateOf<List<SiteSummary>>(emptyList()) }
    var plans by remember { mutableStateOf<List<PlanDto>>(emptyList()) }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var planId by remember { mutableStateOf<Long?>(null) }
    var phone by remember { mutableStateOf("") }
    var issued by remember { mutableStateOf<VoucherDto?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repository.sellOptions() }
            .onSuccess {
                sites = it.sites
                plans = it.plans
                siteId = it.sites.firstOrNull()?.id
                planId = it.plans.firstOrNull()?.id
                loading = false
            }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Sell cash voucher") }) }) { padding ->
        if (loading) LoadingBox() else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ErrorText(error)
                DropdownField(
                    label = "Site",
                    options = sites.map { it.id to it.name },
                    selected = siteId,
                    onSelected = { siteId = it },
                )
                DropdownField(
                    label = "Plan",
                    options = plans.mapNotNull { p -> p.id?.let { it to "${p.name} (${p.currencyCode} ${"%.2f".format(p.price)})" } },
                    selected = planId,
                    onSelected = { planId = it },
                )
                AppTextField(phone, { phone = it }, "Guest phone (optional)")
                PrimaryButton(
                    text = if (selling) "Issuing..." else "Issue voucher",
                    enabled = !selling && siteId != null && planId != null,
                ) {
                    scope.launch {
                        selling = true
                        error = null
                        runCatching { repository.sell(siteId!!, planId!!, phone) }
                            .onSuccess { issued = it }
                            .onFailure { error = it.message }
                        selling = false
                    }
                }
                issued?.let { voucher ->
                    SoftCard {
                        Text("Voucher ready", fontWeight = FontWeight.SemiBold)
                        Text(
                            voucher.code,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                        Text("Username and password are this code on the hotspot login.")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<Pair<Long, String>>,
    selected: Long?,
    onSelected: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selected }?.second ?: "Select"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (id, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSelected(id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VouchersScreen(
    repository: WifiRepository,
    onOpen: (Long) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var q by remember { mutableStateOf("") }
    var vouchers by remember { mutableStateOf<List<VoucherDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            loading = true
            runCatching { repository.vouchers(q.ifBlank { null }) }
                .onSuccess { vouchers = it; loading = false }
                .onFailure { error = it.message; loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Vouchers") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            AppTextField(q, { q = it }, "Search code or phone")
            PrimaryButton("Search", onClick = { load() })
            ErrorText(error)
            if (loading) LoadingBox() else {
                SimpleList(vouchers, "No vouchers found") { voucher ->
                    SoftCard {
                        Column(Modifier.clickable { onOpen(voucher.id) }) {
                            Text(voucher.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Text("${voucher.status} · ${voucher.channel}")
                            Text(voucher.site?.name ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherDetailScreen(
    repository: WifiRepository,
    voucherId: Long,
    canRevoke: Boolean,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var voucher by remember { mutableStateOf<VoucherDto?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(voucherId) {
        runCatching { repository.voucher(voucherId) }
            .onSuccess { voucher = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voucher") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        if (loading) LoadingBox() else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ErrorText(error)
                SoftCard {
                    Text(voucher?.code.orEmpty(), fontFamily = FontFamily.Monospace, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Status: ${voucher?.status}")
                    Text("Channel: ${voucher?.channel}")
                    Text("Amount: ${voucher?.currencyCode} ${"%.2f".format(voucher?.amount ?: 0.0)}")
                    Text("Site: ${voucher?.site?.name}")
                    Text("Plan: ${voucher?.plan?.name}")
                    Text("Expires: ${voucher?.expiresAt ?: "-"}")
                }
                if (canRevoke && voucher?.status == "active") {
                    PrimaryButton("Revoke voucher") {
                        scope.launch {
                            runCatching { repository.revoke(voucherId) }
                                .onSuccess { voucher = it }
                                .onFailure { error = it.message }
                        }
                    }
                }
            }
        }
    }
}
