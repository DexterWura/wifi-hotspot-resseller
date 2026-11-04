package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.DashboardData
import com.clouderp.wifireseller.data.LiveMonitorData
import com.clouderp.wifireseller.data.MoneyTotal
import com.clouderp.wifireseller.data.SalesData
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.data.channelLabel
import com.clouderp.wifireseller.data.formatIso
import com.clouderp.wifireseller.data.money
import com.clouderp.wifireseller.data.moneyLines
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ChoiceChip
import com.clouderp.wifireseller.ui.components.DetailRow
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.GhostButton
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.MetaText
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SecondaryButton
import com.clouderp.wifireseller.ui.components.SectionLabel
import com.clouderp.wifireseller.ui.components.SectionTitle
import com.clouderp.wifireseller.ui.components.SoftCard
import com.clouderp.wifireseller.ui.components.StatCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val ChannelKeys = listOf("cash", "paynow", "pesepay", "ecocash")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    repository: WifiRepository,
    companyName: String,
    bootError: String?,
    canOpenLive: Boolean,
    canOpenSales: Boolean,
    onOpenLive: () -> Unit,
    onOpenSales: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<DashboardData?>(null) }

    fun load() {
        loading = true
        error = null
    }

    LaunchedEffect(loading) {
        if (!loading) return@LaunchedEffect
        runCatching { repository.dashboard() }
            .onSuccess { data = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(companyName) }) },
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
                if (error != null) SecondaryButton("Retry", onClick = { load() })
                MetaText("Sell guest Wi-Fi, issue owner codes, and sync hotspot users to MikroTik.")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Earned today", moneyLines(data?.earningsToday.orEmpty()), Modifier.weight(1f))
                    StatCard("This month", moneyLines(data?.earningsMonth.orEmpty()), Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard("Sold today", (data?.vouchersSoldToday ?: 0).toString(), Modifier.weight(1f))
                    StatCard("Active vouchers", (data?.activeVouchers ?: 0).toString(), Modifier.weight(1f))
                }
                MetaText("Pending router sync: ${data?.pendingSync ?: 0} · Sold this month: ${data?.vouchersSoldMonth ?: 0}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canOpenSales) {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                            SecondaryButton("Sales report", onClick = onOpenSales)
                        }
                    }
                    if (canOpenLive) {
                        androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                            SecondaryButton("Live monitor", onClick = onOpenLive)
                        }
                    }
                }
                SoftCard {
                    SectionLabel("Earnings today by channel")
                    ChannelKeys.forEach { key ->
                        val rows = data?.salesByChannelToday?.get(key).orEmpty()
                        DetailRow(channelLabel(key), moneyLines(rows))
                    }
                }
                SoftCard {
                    SectionLabel("All-time earnings")
                    val allTime = data?.earningsAllTime.orEmpty()
                    if (allTime.isEmpty()) {
                        MetaText("No sales yet. Cash and guest payments will show here.")
                    } else {
                        allTime.forEach { row ->
                            DetailRow(row.currencyCode, "${money(row.currencyCode, row.total)} · ${row.count}")
                        }
                    }
                }
                SoftCard {
                    SectionLabel("Sites")
                    val sites = data?.sites.orEmpty()
                    if (sites.isEmpty()) {
                        MetaText("Add a MikroTik hotspot site to start selling.")
                    } else {
                        sites.forEach { site ->
                            val online = data?.onlineBySite?.get(site.id.toString()) ?: 0
                            DetailRow(site.name, "$online online · ${formatIso(site.lastSeenAt).let { if (it == "—") "Never" else it }}")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMonitorScreen(repository: WifiRepository) {
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<LiveMonitorData?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load(refresh: Boolean) {
        runCatching { repository.live(refresh) }
            .onSuccess {
                data = it
                error = null
                loading = false
                refreshing = false
            }
            .onFailure {
                error = it.message
                loading = false
                refreshing = false
            }
    }

    LaunchedEffect(Unit) {
        load(false)
        while (isActive) {
            delay(5_000)
            load(false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live monitor") },
                actions = {
                    GhostButton(
                        if (refreshing) "Refreshing..." else "Refresh routers",
                        enabled = !refreshing,
                    ) {
                        refreshing = true
                        scope.launch { load(true) }
                    }
                },
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
                ErrorText(error)
                MetaText("Updated ${formatIso(data?.refreshedAt)}")
                SoftCard {
                    SectionTitle("Right now")
                    Text("Connected: ${data?.connectedCount ?: 0}", fontWeight = FontWeight.SemiBold)
                    Text("Combined speed: ${data?.totalRateLabel ?: "0 kbps"}")
                    Text("Data transferred: ${data?.totalBytesLabel ?: "0 B"}")
                    Text("Active vouchers: ${data?.activeVouchers ?: 0}")
                }
                SoftCard {
                    SectionTitle("By site")
                    val sites = data?.sites.orEmpty()
                    if (sites.isEmpty()) {
                        MetaText("No guests connected.")
                    } else {
                        sites.forEach { site ->
                            DetailRow(site.name, "${site.connected} online · ${site.totalRateBps / 1000} kbps")
                        }
                    }
                }
                SectionTitle("Connected sessions")
                val sessions = data?.sessions.orEmpty()
                if (sessions.isEmpty()) {
                    SoftCard {
                        MetaText("No hotspot sessions reported yet. Routers push on sync, or tap Refresh routers if API credentials are set.")
                    }
                } else {
                    sessions.forEach { session ->
                        SoftCard {
                            Text(session.username, fontWeight = FontWeight.Bold)
                            Text(session.site?.name ?: "Site")
                            MetaText("IP ${session.ipAddress ?: "—"} · MAC ${session.macAddress ?: "—"}")
                            MetaText("Uptime ${session.uptime ?: "—"} · Idle ${session.idleTime ?: "—"}")
                            Text("Speed ${session.rateLabel} · Data ${session.bytesTotalLabel}")
                            val planName = session.plan?.name
                            val planLimit = session.plan?.rateLimit ?: session.plan?.dataLimitLabel
                            if (!planName.isNullOrBlank() || !planLimit.isNullOrBlank()) {
                                MetaText(listOfNotNull(planName, planLimit).joinToString(" · "))
                            }
                            if (session.isStale) {
                                Text("Stale report", color = MaterialTheme.colorScheme.error)
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
fun SalesScreen(repository: WifiRepository) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<SalesData?>(null) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var channel by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()

    fun load(nextPage: Int = 1) {
        scope.launch {
            loading = true
            error = null
            runCatching {
                repository.sales(
                    from = from.ifBlank { null },
                    to = to.ifBlank { null },
                    siteId = siteId,
                    channel = channel,
                    page = nextPage,
                )
            }.onSuccess {
                if (nextPage > 1 && data != null) {
                    val previous = data!!.rows?.data.orEmpty()
                    data = it.copy(rows = it.rows?.copy(data = previous + it.rows.data))
                } else {
                    data = it
                    if (from.isBlank()) from = it.from.orEmpty()
                    if (to.isBlank()) to = it.to.orEmpty()
                }
                page = nextPage
                loading = false
            }.onFailure { error = it.message; loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(topBar = { TopAppBar(title = { Text("Sales") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ErrorText(error)
            AppTextField(from, { from = it }, "From (YYYY-MM-DD)")
            AppTextField(to, { to = it }, "To (YYYY-MM-DD)")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("All sites", siteId == null) { siteId = null }
                data?.sites.orEmpty().forEach { site ->
                    ChoiceChip(site.name, siteId == site.id) { siteId = site.id }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("All channels", channel == null) { channel = null }
                (data?.channels?.keys ?: ChannelKeys).forEach { key ->
                    ChoiceChip(data?.channels?.get(key) ?: channelLabel(key), channel == key) { channel = key }
                }
            }
            PrimaryButton("Apply filters", enabled = !loading) { load(1) }
            if (loading && data == null) {
                LoadingBox()
            } else {
                Text("From ${data?.from ?: "—"} to ${data?.to ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                SoftCard {
                    SectionTitle("Totals")
                    MoneyLines(data?.totals.orEmpty())
                    Text("Vouchers: ${data?.voucherCount ?: 0}")
                }
                SoftCard {
                    SectionLabel("By channel")
                    ChannelKeys.forEach { key ->
                        DetailRow(channelLabel(key), moneyLines(data?.byChannel?.get(key).orEmpty()))
                    }
                }
                SoftCard {
                    SectionLabel("By site")
                    val bySite = data?.bySite.orEmpty()
                    if (bySite.isEmpty()) MetaText("No site totals for this range.")
                    else bySite.forEach { row ->
                        DetailRow(row.siteName, "${money(row.currencyCode, row.total)} · ${row.count}")
                    }
                }
                data?.rows?.data.orEmpty().forEach { row ->
                    SoftCard {
                        Text(row.code, fontWeight = FontWeight.Bold, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        Text("${channelLabel(row.channel)} · ${money(row.currencyCode, row.amount)}")
                        MetaText(listOfNotNull(row.site?.name, row.plan?.name).joinToString(" · "))
                    }
                }
                val lastPage = data?.rows?.lastPage ?: 1
                if (page < lastPage) {
                    SecondaryButton(if (loading) "Loading..." else "Load more", enabled = !loading) { load(page + 1) }
                }
            }
        }
    }
}

@Composable
private fun MoneyLines(rows: List<MoneyTotal>) {
    if (rows.isEmpty()) {
        MetaText("No sales yet")
    } else {
        rows.forEach {
            Text(
                "${money(it.currencyCode, it.total)} (${it.count})",
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
