package com.clouderp.wifireseller.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.clouderp.wifireseller.data.CompanyDto
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.ui.components.AppTextField
import com.clouderp.wifireseller.ui.components.ErrorText
import com.clouderp.wifireseller.ui.components.LoadingBox
import com.clouderp.wifireseller.ui.components.PrimaryButton
import com.clouderp.wifireseller.ui.components.SoftCard
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: WifiRepository,
    onLoggedIn: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "WiFi Hotspot Reseller",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Sign in with your Cloud ERP account",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        ErrorText(error)
        AppTextField(email, { email = it }, "Email")
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp),
        )
        PrimaryButton(
            text = if (loading) "Signing in..." else "Sign in",
            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    runCatching { repository.login(email, password) }
                        .onSuccess { onLoggedIn() }
                        .onFailure { error = it.message }
                    loading = false
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyPickerScreen(
    repository: WifiRepository,
    onPicked: () -> Unit,
    onLogout: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var companies by remember { mutableStateOf<List<CompanyDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { repository.companies() }
            .onSuccess { companies = it; loading = false }
            .onFailure { error = it.message; loading = false }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose workspace") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Logout") }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox()
            else -> Column(Modifier.padding(padding).padding(16.dp)) {
                ErrorText(error)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(companies) { company ->
                        SoftCard {
                            Text(company.name, fontWeight = FontWeight.SemiBold)
                            Text(company.subdomain ?: company.uuid, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PrimaryButton("Continue") {
                                scope.launch {
                                    repository.selectCompany(company)
                                    onPicked()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
