package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.PlanDto
import com.clouderp.wifireseller.data.SiteSummary
import com.clouderp.wifireseller.data.VoucherDto
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.data.money
import com.clouderp.wifireseller.data.planMeta
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ChoiceChip
import com.clouderp.wifireseller.ui.components.EmptyState
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.GhostButton
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.MetaText
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SecondaryButton
import com.clouderp.wifireseller.ui.components.SectionLabel
import com.clouderp.wifireseller.ui.components.SoftCard
import com.clouderp.wifireseller.ui.components.VoucherSlip
import com.clouderp.wifireseller.ui.theme.MoneyStyle
import com.clouderp.wifireseller.ui.util.printVoucherSlip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SellScreen(
    repository: WifiRepository,
    canOpenSites: Boolean,
    canOpenPlans: Boolean,
    onOpenSites: () -> Unit,
    onOpenPlans: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var selling by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sites by remember { mutableStateOf<List<SiteSummary>>(emptyList()) }
    var plans by remember { mutableStateOf<List<PlanDto>>(emptyList()) }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var planId by remember { mutableStateOf<Long?>(null) }
    var search by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var tendered by remember { mutableStateOf("") }
    var cashIssueMode by remember { mutableStateOf("preview") }
    var cashShowQr by remember { mutableStateOf(true) }
    var issued by remember { mutableStateOf<VoucherDto?>(null) }
    var cartOpen by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun reload() {
        loading = true
        issued = null
        error = null
        scope.launch {
            runCatching { repository.sellOptions() }
                .onSuccess {
                    sites = it.sites
                    plans = it.plans
                    siteId = it.sites.firstOrNull()?.id
                    planId = null
                    cashIssueMode = it.cashIssueMode ?: "preview"
                    cashShowQr = it.cashShowQr
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }
    }

    LaunchedEffect(Unit) { reload() }

    val visiblePlans = plans.filter { plan ->
        val matchesSite = plan.siteId == null || plan.siteId == siteId
        val matchesSearch = search.isBlank() || plan.name.contains(search, ignoreCase = true)
        matchesSite && matchesSearch
    }
    val selected = visiblePlans.firstOrNull { it.id == planId }
    val price = selected?.price ?: 0.0
    val given = tendered.toDoubleOrNull() ?: 0.0
    val change = given - price

    Scaffold(topBar = { TopAppBar(title = { Text("Till") }) }) { padding ->
        when {
            loading -> LoadingBox()
            issued != null -> Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                VoucherSlip(voucher = issued!!, showQr = cashShowQr)
                PrimaryButton("Sell another") {
                    issued = null
                    planId = null
                    phone = ""
                    tendered = ""
                }
            }
            sites.isEmpty() || plans.isEmpty() -> Column(Modifier.padding(padding)) {
                EmptyState("Add an active site and plan before selling cash vouchers.") {
                    if (canOpenSites) SecondaryButton("Sites", onClick = onOpenSites)
                    if (canOpenPlans) SecondaryButton("Plans", onClick = onOpenPlans)
                }
            }
            else -> BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
                val wide = maxWidth >= 720.dp
                val catalog: @Composable (Modifier) -> Unit = { modifier ->
                    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ErrorText(error)
                        SectionLabel("Catalog")
                        MetaText("Choose the site, then tap a plan to add it to the cart.")
                        Text("Site", style = MaterialTheme.typography.titleSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sites.forEach { site ->
                                ChoiceChip(site.name, siteId == site.id) {
                                    siteId = site.id
                                    if (selected?.siteId != null && selected.siteId != site.id) planId = null
                                }
                            }
                        }
                        AppTextField(search, { search = it }, "Search plans")
                        if (visiblePlans.isEmpty()) {
                            MetaText("No plans match this site or search.")
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(160.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f, fill = true),
                            ) {
                                items(visiblePlans, key = { it.id ?: it.name }) { plan ->
                                    SoftCard(
                                        highlighted = plan.id == planId,
                                        onClick = { planId = plan.id },
                                    ) {
                                        Text(plan.name, style = MaterialTheme.typography.titleMedium)
                                        MetaText(plan.planMeta())
                                        Text(
                                            money(plan.currencyCode, plan.price),
                                            style = MoneyStyle,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val cart: @Composable () -> Unit = {
                    TillCart(
                        selected = selected,
                        phone = phone,
                        onPhone = { phone = it },
                        tendered = tendered,
                        onTendered = { tendered = it },
                        change = change,
                        selling = selling,
                        onClear = {
                            planId = null
                            phone = ""
                            tendered = ""
                        },
                        onCheckout = {
                            val site = siteId
                            val plan = selected?.id
                            if (site == null || plan == null) return@TillCart
                            scope.launch {
                                selling = true
                                error = null
                                runCatching { repository.sell(site, plan, phone) }
                                    .onSuccess { voucher ->
                                        issued = voucher
                                        cartOpen = false
                                        if (cashIssueMode == "auto") printVoucherSlip(context, voucher)
                                    }
                                    .onFailure { error = it.message }
                                selling = false
                            }
                        },
                    )
                }
                if (wide) {
                    Row(Modifier.fillMaxSize()) {
                        catalog(Modifier.weight(1.35f))
                        Surface(tonalElevation = 1.dp, modifier = Modifier.weight(0.75f).fillMaxSize()) {
                            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) { cart() }
                        }
                    }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        catalog(Modifier.weight(1f))
                        Surface(tonalElevation = 2.dp) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total")
                                    Text(
                                        selected?.let { money(it.currencyCode, it.price) } ?: "0.00",
                                        style = MoneyStyle,
                                    )
                                }
                                PrimaryButton(
                                    if (selected == null) "Cart is empty" else "Complete sale",
                                    enabled = selected != null && !selling,
                                ) { cartOpen = true }
                            }
                        }
                    }
                    if (cartOpen) {
                        ModalBottomSheet(
                            onDismissRequest = { cartOpen = false },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        ) {
                            Column(Modifier.padding(16.dp).heightIn(max = 640.dp).verticalScroll(rememberScrollState())) {
                                cart()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TillCart(
    selected: PlanDto?,
    phone: String,
    onPhone: (String) -> Unit,
    tendered: String,
    onTendered: (String) -> Unit,
    change: Double,
    selling: Boolean,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SectionLabel("Cart")
            GhostButton("Clear", onClick = onClear)
        }
        AppTextField(
            phone,
            onPhone,
            "Guest phone",
            placeholder = "Optional",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        if (selected == null) {
            MetaText("Cart is empty. Tap a plan in the catalog.")
        } else {
            SoftCard(highlighted = true) {
                Text(selected.name, style = MaterialTheme.typography.titleMedium)
                MetaText(selected.planMeta())
                Text(money(selected.currencyCode, selected.price), style = MoneyStyle, modifier = Modifier.padding(top = 8.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total", style = MaterialTheme.typography.titleLarge)
            Text(selected?.let { money(it.currencyCode, it.price) } ?: "0.00", style = MoneyStyle)
        }
        SoftCard {
            Text("Cash", style = MaterialTheme.typography.titleMedium)
            MetaText("Hand the guest the voucher code after they pay.")
        }
        AppTextField(
            tendered,
            onTendered,
            "Cash tendered",
            placeholder = "0.00",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            supportingText = "Till helper only — not stored.",
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MetaText("Change due")
            Text(
                "%.2f".format(change),
                color = when {
                    selected == null || tendered.isBlank() -> MaterialTheme.colorScheme.onSurface
                    change >= 0 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                style = MoneyStyle,
            )
        }
        PrimaryButton(
            if (selling) "Issuing..." else "Complete sale",
            enabled = selected != null && !selling,
            onClick = onCheckout,
        )
    }
}
