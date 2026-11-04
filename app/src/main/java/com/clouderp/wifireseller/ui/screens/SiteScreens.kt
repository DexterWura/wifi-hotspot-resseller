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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.SiteDto
import com.clouderp.wifireseller.data.SiteShowResponse
import com.clouderp.wifireseller.data.SiteUpsertRequest
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.data.formatIso
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.CopyBlock
import com.clouderp.wifireseller.ui.components.DangerButton
import com.clouderp.wifireseller.ui.components.DetailRow
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
import com.clouderp.wifireseller.ui.util.copyText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitesScreen(
    repository: WifiRepository,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var lastPage by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    fun load(nextPage: Int = 1) {
        scope.launch {
            loading = true
            runCatching { repository.sites(nextPage) }
                .onSuccess {
                    sites = if (nextPage == 1) it.data else sites + it.data
                    page = it.currentPage
                    lastPage = it.lastPage
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sites") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = "Add site")
            }
        },
    ) { padding ->
        when {
            loading && sites.isEmpty() -> LoadingBox()
            else -> Column(Modifier.padding(padding)) {
                ErrorText(error)
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(sites, key = { it.id ?: it.name }) { site ->
                        SoftCard(onClick = { site.id?.let(onOpen) }) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(site.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusChip(if (site.isActive) "Active" else "Inactive")
                            }
                            MetaText(site.slug ?: "")
                            MetaText(if (site.automaticSync || site.apiPasswordSet) "Automatic login" else "Token / script")
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
fun SiteDetailScreen(
    repository: WifiRepository,
    siteId: Long,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf<SiteShowResponse?>(null) }
    var plainToken by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val site = detail?.site

    fun reload() {
        scope.launch {
            loading = true
            runCatching { repository.site(siteId) }
                .onSuccess {
                    detail = it
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }
    }

    LaunchedEffect(siteId) { reload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(site?.name ?: "Site") },
                navigationIcon = { GhostButton("Back", onClick = onBack) },
                actions = { GhostButton("Edit", onClick = onEdit) },
            )
        },
    ) { padding ->
        when {
            loading && detail == null -> LoadingBox()
            else -> Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ErrorText(error)
                if (!status.isNullOrBlank()) Text(status!!, color = MaterialTheme.colorScheme.primary)
                SoftCard {
                    DetailRow("Slug", site?.slug ?: "—")
                    DetailRow("SSID", site?.ssid ?: "—")
                    DetailRow("Hotspot server", site?.hotspotServer ?: "—")
                    DetailRow("Status", if (site?.isActive == true) "Active" else "Inactive")
                    DetailRow("Last seen", site?.lastSeenAt?.let { formatIso(it) } ?: "Never")
                    DetailRow("Vouchers", (site?.vouchersCount ?: 0).toString())
                    DetailRow("Online now", (site?.sessionsCount ?: 0).toString())
                    DetailRow("Router login", if (site?.automaticSync == true) "Automatic" else "Not set")
                    DetailRow("API host", site?.apiHost ?: "—")
                    DetailRow("Hotspot login", site?.hotspotLoginUrl ?: "—", mono = true)
                }
                if (!plainToken.isNullOrBlank()) {
                    CopyBlock("Router token (copy now)", plainToken!!) {
                        copyText(context, "Router token", plainToken!!)
                    }
                }
                if (site?.automaticSync == true || site?.apiPasswordSet == true) {
                    SecondaryButton("Test login") {
                        scope.launch {
                            runCatching { repository.testConnection(siteId) }
                                .onSuccess { status = it.message ?: "Logged in to the router." }
                                .onFailure { error = it.message }
                        }
                    }
                    PrimaryButton("Sync now") {
                        scope.launch {
                            runCatching { repository.syncSite(siteId) }
                                .onSuccess {
                                    status = it.message
                                    reload()
                                }
                                .onFailure { error = it.message }
                        }
                    }
                }
                SecondaryButton("Rotate router token") {
                    scope.launch {
                        runCatching { repository.rotateToken(siteId) }
                            .onSuccess {
                                detail = detail?.copy(site = it.site)
                                plainToken = it.plainToken
                                status = "Copy the new token now — it is shown only once."
                            }
                            .onFailure { error = it.message }
                    }
                }
                detail?.syncUrl?.let { CopyBlock("Sync URL", it) { copyText(context, "Sync URL", it) } }
                detail?.noticeUrl?.let { CopyBlock("Notice page", it) { copyText(context, "Notice URL", it) } }
                SoftCard {
                    SectionLabel("Walled garden")
                    val hosts = detail?.walledGardenHosts.orEmpty()
                    if (hosts.isEmpty()) MetaText("No hosts yet — set payment gateways under Settings.")
                    else hosts.forEach { MetaText(it) }
                }
                detail?.loginHtmlSnippet?.let {
                    CopyBlock("Hotspot login.html snippet", it) { copyText(context, "Login snippet", it) }
                }
                detail?.routerScript?.let {
                    CopyBlock("RouterOS fallback script", it) { copyText(context, "Router script", it) }
                }
                DangerButton("Delete site") { confirmDelete = true }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this site?") },
            text = { Text("Sites with vouchers cannot be deleted. Deactivate instead.") },
            confirmButton = {
                GhostButton("Delete") {
                    scope.launch {
                        runCatching { repository.deleteSite(siteId) }
                            .onSuccess { onBack() }
                            .onFailure { error = it.message; confirmDelete = false }
                    }
                }
            },
            dismissButton = { GhostButton("Cancel") { confirmDelete = false } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteFormScreen(
    repository: WifiRepository,
    siteId: Long?,
    onDone: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var ssid by remember { mutableStateOf("") }
    var hotspotServer by remember { mutableStateOf("") }
    var hotspotLoginUrl by remember { mutableStateOf("") }
    var apiHost by remember { mutableStateOf("") }
    var apiPort by remember { mutableStateOf("") }
    var apiTls by remember { mutableStateOf(true) }
    var apiUsername by remember { mutableStateOf("") }
    var apiPassword by remember { mutableStateOf("") }
    var passwordSet by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(siteId != null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(siteId) {
        if (siteId != null) {
            runCatching { repository.site(siteId) }
                .onSuccess {
                    val site = it.site
                    name = site.name
                    slug = site.slug.orEmpty()
                    ssid = site.ssid.orEmpty()
                    hotspotServer = site.hotspotServer.orEmpty()
                    hotspotLoginUrl = site.hotspotLoginUrl.orEmpty()
                    apiHost = site.apiHost.orEmpty()
                    apiPort = site.apiPort?.toString().orEmpty()
                    apiTls = site.apiUseTls
                    apiUsername = site.apiUsername.orEmpty()
                    passwordSet = site.apiPasswordSet
                    active = site.isActive
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (siteId == null) "New site" else "Edit site") },
                navigationIcon = { GhostButton("Back", onClick = onDone) },
            )
        },
    ) { padding ->
        if (loading) {
            LoadingBox()
        } else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ErrorText(error)
                AppTextField(name, { name = it }, "Name")
                AppTextField(slug, { slug = it }, "Slug", supportingText = "Used in the guest link.")
                AppTextField(ssid, { ssid = it }, "SSID", placeholder = "Cafe-Guest")
                AppTextField(hotspotServer, { hotspotServer = it }, "Hotspot server name", placeholder = "hotspot1")
                AppTextField(hotspotLoginUrl, { hotspotLoginUrl = it }, "Hotspot login URL", placeholder = "http://192.168.88.1/login")
                SettingSwitch("Active", active) { active = it }
                SectionLabel("Router login")
                MetaText("Enter a MikroTik user so this app can log in over REST.")
                AppTextField(apiHost, { apiHost = it }, "API host")
                AppTextField(apiPort, { apiPort = it }, "API port", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), placeholder = "443")
                SettingSwitch("Use TLS", apiTls) { apiTls = it }
                AppTextField(apiUsername, { apiUsername = it }, "API username")
                AppTextField(
                    apiPassword,
                    { apiPassword = it },
                    "API password",
                    visualTransformation = PasswordVisualTransformation(),
                    placeholder = if (passwordSet) "Leave blank to keep" else null,
                )
                PrimaryButton(if (saving) "Saving..." else "Save", enabled = !saving && name.isNotBlank()) {
                    scope.launch {
                        saving = true
                        error = null
                        val body = SiteUpsertRequest(
                            name = name.trim(),
                            slug = slug.trim().ifBlank { null },
                            ssid = ssid.trim().ifBlank { null },
                            hotspotServer = hotspotServer.trim().ifBlank { null },
                            hotspotLoginUrl = hotspotLoginUrl.trim().ifBlank { null },
                            apiHost = apiHost.trim().ifBlank { null },
                            apiPort = apiPort.toIntOrNull(),
                            apiUseTls = apiTls,
                            apiUsername = apiUsername.trim().ifBlank { null },
                            apiPassword = apiPassword.ifBlank { null },
                            isActive = active,
                        )
                        val result = if (siteId == null) {
                            runCatching { repository.createSite(body) }
                        } else {
                            runCatching { repository.updateSite(siteId, body) }
                        }
                        result.onSuccess { onDone() }.onFailure { error = it.message }
                        saving = false
                    }
                }
            }
        }
    }
}
