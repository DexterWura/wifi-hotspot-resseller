package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.VoucherDto
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.data.channelLabel
import com.clouderp.wifireseller.data.statusLabel
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ChoiceChip
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.GhostButton
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.MetaText
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SecondaryButton
import com.clouderp.wifireseller.ui.components.SoftCard
import com.clouderp.wifireseller.ui.components.StatusChip
import com.clouderp.wifireseller.ui.components.VoucherSlip
import kotlinx.coroutines.launch

private val StatusFilters = listOf(
    null to "All",
    "pending_payment" to "Pending",
    "active" to "Active",
    "expired" to "Expired",
    "revoked" to "Revoked",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VouchersScreen(
    repository: WifiRepository,
    onOpen: (Long) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var q by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var vouchers by remember { mutableStateOf<List<VoucherDto>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var lastPage by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    fun load(nextPage: Int = 1) {
        scope.launch {
            loading = true
            error = null
            runCatching { repository.vouchers(q.ifBlank { null }, status, nextPage) }
                .onSuccess { result ->
                    vouchers = if (nextPage == 1) result.data else vouchers + result.data
                    page = result.currentPage
                    lastPage = result.lastPage
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }
    }

    LaunchedEffect(status) { load(1) }

    Scaffold(topBar = { TopAppBar(title = { Text("Vouchers") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
            AppTextField(q, { q = it }, "Search code or phone")
            PrimaryButton("Search", onClick = { load(1) })
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                StatusFilters.forEach { (value, label) ->
                    ChoiceChip(label, status == value) { status = value }
                }
            }
            ErrorText(error)
            if (loading && vouchers.isEmpty()) {
                LoadingBox()
            } else if (vouchers.isEmpty()) {
                MetaText("No vouchers found")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(vouchers, key = { it.id }) { voucher ->
                        SoftCard(onClick = { onOpen(voucher.id) }) {
                            Text(voucher.code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusChip(statusLabel(voucher.status))
                                MetaText(channelLabel(voucher.channel))
                            }
                            MetaText(voucher.site?.name ?: "")
                        }
                    }
                    if (page < lastPage) {
                        item {
                            SecondaryButton(if (loading) "Loading..." else "Load more", enabled = !loading) {
                                load(page + 1)
                            }
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
    var showQr by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(voucherId) {
        runCatching { repository.voucher(voucherId) }
            .onSuccess { voucher = it; loading = false }
            .onFailure { error = it.message; loading = false }
        runCatching { repository.settings() }
            .onSuccess { showQr = it.cashShowQr }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voucher") },
                navigationIcon = { GhostButton("Back", onClick = onBack) },
            )
        },
    ) { padding ->
        if (loading) LoadingBox() else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ErrorText(error)
                voucher?.let { current ->
                    VoucherSlip(
                        voucher = current,
                        showQr = showQr,
                        onRevoke = if (canRevoke) {
                            {
                                scope.launch {
                                    runCatching { repository.revoke(voucherId) }
                                        .onSuccess { voucher = it }
                                        .onFailure { error = it.message }
                                }
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
