package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.clouderp.wifireseller.data.SiteDto
import com.clouderp.wifireseller.data.SiteUpsertRequest
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
fun SitesScreen(
    repository: WifiRepository,
    onCreate: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var sites by remember { mutableStateOf<List<SiteDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { repository.sites() }
            .onSuccess { sites = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Sites") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = "Add site")
            }
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            else -> Column(Modifier.padding(padding)) {
                ErrorText(error)
                SimpleList(sites, "No sites yet") { site ->
                    SoftCard {
                        Column(Modifier.clickable { site.id?.let(onOpen) }) {
                            Text(site.name, fontWeight = FontWeight.SemiBold)
                            Text(site.slug ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (site.isActive) "Active" else "Inactive")
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
    var site by remember { mutableStateOf<SiteDto?>(null) }
    var plainToken by remember { mutableStateOf<String?>(null) }
    var syncUrl by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            loading = true
            runCatching { repository.site(siteId) }
                .onSuccess {
                    site = it.site
                    syncUrl = it.syncUrl
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
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
                actions = { TextButton(onClick = onEdit) { Text("Edit") } },
            )
        },
    ) { padding ->
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
                SoftCard {
                    Text("Slug: ${site?.slug}")
                    Text("SSID: ${site?.ssid ?: "-"}")
                    Text("Hotspot login: ${site?.hotspotLoginUrl ?: "-"}")
                    Text("Sync URL: ${syncUrl ?: "-"}")
                }
                if (!plainToken.isNullOrBlank()) {
                    SoftCard {
                        Text("Router token (copy now)", fontWeight = FontWeight.Bold)
                        Text(plainToken!!, fontFamily = FontFamily.Monospace)
                    }
                }
                PrimaryButton("Rotate router token") {
                    scope.launch {
                        runCatching { repository.rotateToken(siteId) }
                            .onSuccess {
                                site = it.site
                                plainToken = it.plainToken
                            }
                            .onFailure { error = it.message }
                    }
                }
            }
        }
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
    var hotspotLoginUrl by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(siteId != null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(siteId) {
        if (siteId != null) {
            runCatching { repository.site(siteId) }
                .onSuccess {
                    name = it.site.name
                    slug = it.site.slug.orEmpty()
                    ssid = it.site.ssid.orEmpty()
                    hotspotLoginUrl = it.site.hotspotLoginUrl.orEmpty()
                    active = it.site.isActive
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (siteId == null) "New site" else "Edit site") }) },
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
                AppTextField(slug, { slug = it }, "Slug (optional)")
                AppTextField(ssid, { ssid = it }, "SSID")
                AppTextField(hotspotLoginUrl, { hotspotLoginUrl = it }, "Hotspot login URL")
                Switch(checked = active, onCheckedChange = { active = it })
                Text(if (active) "Active" else "Inactive")
                PrimaryButton(if (saving) "Saving..." else "Save", enabled = !saving && name.isNotBlank()) {
                    scope.launch {
                        saving = true
                        val body = SiteUpsertRequest(
                            name = name.trim(),
                            slug = slug.trim().ifBlank { null },
                            ssid = ssid.trim().ifBlank { null },
                            hotspotLoginUrl = hotspotLoginUrl.trim().ifBlank { null },
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
