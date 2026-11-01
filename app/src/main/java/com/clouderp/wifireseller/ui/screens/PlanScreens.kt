package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.PlanDto
import com.clouderp.wifireseller.data.PlanUpsertRequest
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
fun PlansScreen(
    repository: WifiRepository,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<PlanDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { repository.plansIndex() }
            .onSuccess {
                plans = it.plans?.data.orEmpty()
                loading = false
            }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Plans") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Default.Add, contentDescription = "Add plan")
            }
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            else -> Column(Modifier.padding(padding)) {
                ErrorText(error)
                SimpleList(plans, "No plans yet") { plan ->
                    SoftCard {
                        Column(Modifier.clickable { plan.id?.let(onEdit) }) {
                            Text(plan.name, fontWeight = FontWeight.SemiBold)
                            Text("${plan.currencyCode} ${"%.2f".format(plan.price)} · ${plan.durationLabel ?: "${plan.durationMinutes} min"}")
                            Text(plan.dataLimitLabel ?: "Unlimited", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("1.00") }
    var currency by remember { mutableStateOf("USD") }
    var minutes by remember { mutableStateOf("60") }
    var dataMb by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(planId != null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(planId) {
        if (planId != null) {
            runCatching { repository.plansIndex() }
                .onSuccess { index ->
                    val plan = index.plans?.data.orEmpty().firstOrNull { it.id == planId }
                    if (plan != null) {
                        name = plan.name
                        price = plan.price.toString()
                        currency = plan.currencyCode
                        minutes = plan.durationMinutes.toString()
                        dataMb = plan.dataLimitBytes?.let { (it / (1024 * 1024)).toString() }.orEmpty()
                        active = plan.isActive
                        if (index.currencies.isNotEmpty() && currency.isBlank()) {
                            currency = index.currencies.first().code
                        }
                    }
                    loading = false
                }
                .onFailure { error = it.message; loading = false }
        } else {
            runCatching { repository.plansIndex() }
                .onSuccess {
                    if (it.currencies.isNotEmpty()) currency = it.currencies.first().code
                    loading = false
                }
                .onFailure { loading = false }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (planId == null) "New plan" else "Edit plan") }) }) { padding ->
        if (loading) LoadingBox() else {
            Column(
                Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ErrorText(error)
                AppTextField(name, { name = it }, "Name")
                AppTextField(price, { price = it }, "Price")
                AppTextField(currency, { currency = it }, "Currency")
                AppTextField(minutes, { minutes = it }, "Duration (minutes)")
                AppTextField(dataMb, { dataMb = it }, "Data limit MB (blank = unlimited)")
                Switch(checked = active, onCheckedChange = { active = it })
                Text(if (active) "Active" else "Inactive")
                PrimaryButton(if (saving) "Saving..." else "Save", enabled = !saving && name.isNotBlank()) {
                    scope.launch {
                        saving = true
                        val body = PlanUpsertRequest(
                            name = name.trim(),
                            price = price.toDoubleOrNull() ?: 0.0,
                            currencyCode = currency.trim().uppercase(),
                            durationMinutes = minutes.toIntOrNull() ?: 60,
                            dataLimitBytes = dataMb.toLongOrNull()?.takeIf { it > 0 }?.times(1024 * 1024),
                            isActive = active,
                        )
                        val result = if (planId == null) {
                            runCatching { repository.createPlan(body) }
                        } else {
                            runCatching { repository.updatePlan(planId, body) }
                        }
                        result.onSuccess { onDone() }.onFailure { error = it.message }
                        saving = false
                    }
                }
            }
        }
    }
}
