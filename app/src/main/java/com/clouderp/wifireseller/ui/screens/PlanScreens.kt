package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.CurrencyDto
import com.clouderp.wifireseller.data.PlanDto
import com.clouderp.wifireseller.data.PlanUpsertRequest
import com.clouderp.wifireseller.data.SiteSummary
import com.clouderp.wifireseller.data.VoucherDto
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.data.money
import com.clouderp.wifireseller.data.planMeta
import com.clouderp.wifireseller.data.timeModeLabel
import com.clouderp.wifireseller.data.timeModeNote
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ChoiceChip
import com.clouderp.wifireseller.ui.components.DangerButton
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.GhostButton
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.MetaText
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SecondaryButton
import com.clouderp.wifireseller.ui.components.SectionLabel
import com.clouderp.wifireseller.ui.components.SettingSwitch
import com.clouderp.wifireseller.ui.components.SoftCard
import com.clouderp.wifireseller.ui.components.StatusChip
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    repository: WifiRepository,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<PlanDto>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var lastPage by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    fun load(nextPage: Int = 1) {
        scope.launch {
            loading = true
            runCatching { repository.plansIndex(nextPage) }
                .onSuccess {
                    val pageData = it.plans
                    val rows = pageData?.data.orEmpty()
                    plans = if (nextPage == 1) rows else plans + rows
                    page = pageData?.currentPage ?: 1
                    lastPage = pageData?.lastPage ?: 1
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Plans") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = "Add plan")
            }
        },
    ) { padding ->
        when {
            loading && plans.isEmpty() -> LoadingBox()
            else -> Column(Modifier.padding(padding)) {
                ErrorText(error)
                if (plans.isEmpty()) {
                    MetaText("No plans yet")
                } else {
                    LazyColumn(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(plans, key = { it.id ?: it.name }) { plan ->
                            SoftCard(onClick = { plan.id?.let(onEdit) }) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(plan.name, style = MaterialTheme.typography.titleMedium)
                                    StatusChip(if (plan.isSellable) "Guest" else "Owner")
                                }
                                MetaText(plan.planMeta())
                                Text(
                                    if (plan.isSellable) money(plan.currencyCode, plan.price) else "Free",
                                    modifier = Modifier.padding(top = 6.dp),
                                )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFormScreen(
    repository: WifiRepository,
    planId: Long?,
    onDone: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onIssued: (VoucherDto) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf("1.00") }
    var currency by remember { mutableStateOf("USD") }
    var minutes by remember { mutableStateOf("60") }
    var timeMode by remember { mutableStateOf("wall_clock") }
    var dataBytes by remember { mutableStateOf("") }
    var rateLimit by remember { mutableStateOf("") }
    var sharedUsers by remember { mutableStateOf("1") }
    var profile by remember { mutableStateOf("") }
    var siteId by remember { mutableStateOf<Long?>(null) }
    var sortOrder by remember { mutableStateOf("0") }
    var active by remember { mutableStateOf(true) }
    var currencies by remember { mutableStateOf<List<CurrencyDto>>(emptyList()) }
    var sites by remember { mutableStateOf<List<SiteSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var issuing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var ownerPhone by remember { mutableStateOf("") }
    var ownerSiteId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(planId) {
        runCatching { repository.plansIndex() }
            .onSuccess { index ->
                currencies = index.currencies
                sites = index.sites
                if (currency.isBlank() && index.currencies.isNotEmpty()) {
                    currency = index.currencies.first().code
                }
            }
        if (planId != null) {
            runCatching { repository.plan(planId) }
                .onSuccess { plan ->
                    name = plan.name
                    owner = !plan.isSellable
                    price = plan.price.toString()
                    currency = plan.currencyCode
                    minutes = plan.durationMinutes.toString()
                    timeMode = plan.timeMode ?: "wall_clock"
                    dataBytes = plan.dataLimitBytes?.toString().orEmpty()
                    rateLimit = plan.rateLimit.orEmpty()
                    sharedUsers = plan.sharedUsers.toString()
                    profile = plan.mikrotikProfile.orEmpty()
                    siteId = plan.siteId
                    sortOrder = plan.sortOrder.toString()
                    active = plan.isActive
                    ownerSiteId = plan.siteId ?: sites.firstOrNull { it.isActive }?.id
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        } else {
            ownerSiteId = sites.firstOrNull { it.isActive }?.id
            loading = false
        }
    }

    fun body(): PlanUpsertRequest = PlanUpsertRequest(
        name = name.trim(),
        price = if (owner) 0.0 else price.toDoubleOrNull() ?: 0.0,
        currencyCode = currency.trim().uppercase(),
        durationMinutes = if (owner) (minutes.toIntOrNull() ?: 1) else (minutes.toIntOrNull() ?: 60),
        timeMode = if (owner) "connected" else timeMode,
        isOwnerPlan = owner,
        dataLimitBytes = dataBytes.toLongOrNull()?.takeIf { it > 0 },
        rateLimit = rateLimit.trim().ifBlank { null },
        sharedUsers = sharedUsers.toIntOrNull()?.coerceIn(1, 100) ?: 1,
        mikrotikProfile = profile.trim().ifBlank { null },
        siteId = siteId,
        isActive = active,
        sortOrder = sortOrder.toIntOrNull() ?: 0,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (planId == null) "New plan" else "Edit plan") },
                navigationIcon = { GhostButton("Back", onClick = onDone) },
            )
        },
    ) { padding ->
        if (loading) LoadingBox() else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ErrorText(error)
                if (!status.isNullOrBlank()) Text(status!!, color = MaterialTheme.colorScheme.primary)
                AppTextField(name, { name = it }, "Name")
                SettingSwitch(
                    "Owner access (not for sale)",
                    owner,
                    "Hidden from the till. Codes never expire.",
                ) { owner = it }
                if (!owner) {
                    AppTextField(price, { price = it }, "Price", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    currencies.ifEmpty { listOf(CurrencyDto(currency)) }.forEach { item ->
                        ChoiceChip(item.code, currency == item.code) { currency = item.code }
                    }
                }
                if (!owner) {
                    AppTextField(minutes, { minutes = it }, "Duration (minutes)", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    SectionLabel("Time mode")
                    SoftCard(highlighted = timeMode == "wall_clock", onClick = { timeMode = "wall_clock" }) {
                        Text(timeModeLabel("wall_clock"), style = MaterialTheme.typography.titleMedium)
                        MetaText(timeModeNote("wall_clock"))
                    }
                    SoftCard(highlighted = timeMode == "connected", onClick = { timeMode = "connected" }) {
                        Text(timeModeLabel("connected"), style = MaterialTheme.typography.titleMedium)
                        MetaText(timeModeNote("connected"))
                    }
                }
                AppTextField(
                    dataBytes,
                    { dataBytes = it },
                    "Data limit (bytes)",
                    supportingText = "Leave empty for unlimited. Do not use 0.",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                AppTextField(rateLimit, { rateLimit = it }, "Rate limit", placeholder = "2M/2M")
                AppTextField(sharedUsers, { sharedUsers = it }, "Shared users", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                AppTextField(profile, { profile = it }, "MikroTik profile", placeholder = "default")
                SectionLabel("Site")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("All sites", siteId == null) { siteId = null }
                    sites.forEach { site ->
                        ChoiceChip(site.name, siteId == site.id) { siteId = site.id }
                    }
                }
                AppTextField(sortOrder, { sortOrder = it }, "Sort order", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                SettingSwitch("Active", active) { active = it }
                PrimaryButton(if (saving) "Saving..." else "Save", enabled = !saving && name.isNotBlank()) {
                    scope.launch {
                        saving = true
                        error = null
                        val result = if (planId == null) {
                            runCatching { repository.createPlan(body()) }
                        } else {
                            runCatching { repository.updatePlan(planId, body()) }
                        }
                        result.onSuccess { saved ->
                            val id = saved.id
                            status = "Plan saved."
                            when {
                                planId == null && !saved.isSellable && id != null -> onOpenPlan(id)
                                planId == null -> onDone()
                            }
                        }.onFailure { error = it.message }
                        saving = false
                    }
                }

                if (planId != null && owner && active) {
                    SoftCard {
                        SectionLabel("Issue owner code")
                        MetaText("Creates a hotspot username and password that does not expire.")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                            val ownerSites = if (siteId != null) sites.filter { it.id == siteId && it.isActive } else sites.filter { it.isActive }
                            ownerSites.forEach { site ->
                                ChoiceChip(site.name, ownerSiteId == site.id) { ownerSiteId = site.id }
                            }
                        }
                        AppTextField(ownerPhone, { ownerPhone = it }, "Phone (optional)")
                        PrimaryButton(
                            if (issuing) "Issuing..." else "Issue owner code",
                            enabled = !issuing && ownerSiteId != null,
                        ) {
                            scope.launch {
                                issuing = true
                                error = null
                                runCatching { repository.issueOwnerVoucher(planId, ownerSiteId!!, ownerPhone) }
                                    .onSuccess(onIssued)
                                    .onFailure { error = it.message }
                                issuing = false
                            }
                        }
                    }
                }

                if (planId != null) {
                    DangerButton("Delete plan") { confirmDelete = true }
                }
            }
        }
    }

    if (confirmDelete && planId != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this plan?") },
            text = { Text("This cannot be undone if the plan has no vouchers.") },
            confirmButton = {
                GhostButton("Delete") {
                    scope.launch {
                        runCatching { repository.deletePlan(planId) }
                            .onSuccess { confirmDelete = false; onDone() }
                            .onFailure { error = it.message; confirmDelete = false }
                    }
                }
            },
            dismissButton = { GhostButton("Cancel") { confirmDelete = false } },
        )
    }
}
