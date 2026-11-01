package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.DashboardData
import com.clouderp.wifireseller.data.MoneyTotal
import com.clouderp.wifireseller.data.SalesData
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SectionTitle
import com.clouderp.wifireseller.ui.components.SoftCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: WifiRepository,
    companyName: String,
    bootError: String?,
    canOpenSales: Boolean,
    canOpenSettings: Boolean,
    onOpenSales: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<DashboardData?>(null) }

    LaunchedEffect(Unit) {
        runCatching { repository.dashboard() }
            .onSuccess { data = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(companyName) },
                actions = { TextButton(onClick = onLogout) { Text("Logout") } },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            else -> Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ErrorText(bootError)
                ErrorText(error)
                SoftCard {
                    SectionTitle("Today")
                    MoneyLines(data?.earningsToday.orEmpty())
                    Text("Vouchers sold: ${data?.vouchersSoldToday ?: 0}")
                }
                SoftCard {
                    SectionTitle("This month")
                    MoneyLines(data?.earningsMonth.orEmpty())
                    Text("Vouchers sold: ${data?.vouchersSoldMonth ?: 0}")
                }
                SoftCard {
                    SectionTitle("Live")
                    Text("Active vouchers: ${data?.activeVouchers ?: 0}")
                    Text("Pending sync: ${data?.pendingSync ?: 0}")
                }
                if (canOpenSales) PrimaryButton("Sales report", onClick = onOpenSales)
                if (canOpenSettings) PrimaryButton("Settings", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
private fun MoneyLines(rows: List<MoneyTotal>) {
    if (rows.isEmpty()) {
        Text("No sales yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        rows.forEach {
            Text(
                "${it.currencyCode} ${"%.2f".format(it.total)} (${it.count})",
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(repository: WifiRepository) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<SalesData?>(null) }

    LaunchedEffect(Unit) {
        runCatching { repository.sales() }
            .onSuccess { data = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Sales") }) }) { padding ->
        when {
            loading -> LoadingBox()
            else -> Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ErrorText(error)
                Text("From ${data?.from ?: "-"} to ${data?.to ?: "-"}")
                SoftCard {
                    SectionTitle("Totals")
                    MoneyLines(data?.totals.orEmpty())
                    Text("Vouchers: ${data?.voucherCount ?: 0}")
                }
                data?.rows?.data.orEmpty().forEach { row ->
                    SoftCard {
                        Text(row.code, fontWeight = FontWeight.Bold)
                        Text("${row.channel} · ${row.currencyCode} ${"%.2f".format(row.amount)}")
                        Text(row.site?.name ?: "Site", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
