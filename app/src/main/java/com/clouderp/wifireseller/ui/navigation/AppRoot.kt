package com.clouderp.wifireseller.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clouderp.wifireseller.data.SessionStore
import com.clouderp.wifireseller.data.ThemePreference
import com.clouderp.wifireseller.data.ThemeStore
import com.clouderp.wifireseller.data.WifiRepository
import com.clouderp.wifireseller.ui.screens.DashboardScreen
import com.clouderp.wifireseller.ui.screens.LiveMonitorScreen
import com.clouderp.wifireseller.ui.screens.LoginScreen
import com.clouderp.wifireseller.ui.screens.MoreScreen
import com.clouderp.wifireseller.ui.screens.PlanFormScreen
import com.clouderp.wifireseller.ui.screens.PlansScreen
import com.clouderp.wifireseller.ui.screens.SalesScreen
import com.clouderp.wifireseller.ui.screens.SellScreen
import com.clouderp.wifireseller.ui.screens.SettingsScreen
import com.clouderp.wifireseller.ui.screens.SiteDetailScreen
import com.clouderp.wifireseller.ui.screens.SiteFormScreen
import com.clouderp.wifireseller.ui.screens.SitesScreen
import com.clouderp.wifireseller.ui.screens.VoucherDetailScreen
import com.clouderp.wifireseller.ui.screens.VouchersScreen
import kotlinx.coroutines.launch

object Routes {
    const val Login = "login"
    const val Home = "home"
    const val Dashboard = "dashboard"
    const val Live = "live"
    const val Sales = "sales"
    const val Sites = "sites"
    const val SiteCreate = "sites/create"
    const val SiteEdit = "sites/{id}/edit"
    const val SiteDetail = "sites/{id}"
    const val Plans = "plans"
    const val PlanCreate = "plans/create"
    const val PlanEdit = "plans/{id}/edit"
    const val Sell = "sell"
    const val Vouchers = "vouchers"
    const val VoucherDetail = "vouchers/{id}"
    const val Settings = "settings"
    const val More = "more"
}

data class TabItem(
    val route: String,
    val label: String,
    val permission: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
fun AppRoot(
    repository: WifiRepository,
    sessionStore: SessionStore,
    themeStore: ThemeStore,
    themePreference: ThemePreference,
    onAuthChanged: (String?, String?) -> Unit,
) {
    val session by sessionStore.session.collectAsState(
        initial = com.clouderp.wifireseller.data.SessionSnapshot(null, null, null, null, null),
    )
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var permissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bootError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(session.token, session.companyUuid) {
        onAuthChanged(session.token, session.companyUuid)
        if (!session.token.isNullOrBlank() && session.companyUuid.isNullOrBlank()) {
            runCatching { repository.bindWorkspace() }
                .onFailure {
                    bootError = it.message
                    repository.logout()
                }
            return@LaunchedEffect
        }
        if (!session.token.isNullOrBlank() && !session.companyUuid.isNullOrBlank()) {
            runCatching { repository.bootstrap() }
                .onSuccess { permissions = it.permissions.toSet(); bootError = null }
                .onFailure { bootError = it.message }
        } else {
            permissions = emptySet()
        }
    }

    LaunchedEffect(session.isLoggedIn, session.hasCompany) {
        val target = when {
            !session.isLoggedIn -> Routes.Login
            !session.hasCompany -> Routes.Login
            else -> Routes.Home
        }
        val current = navController.currentDestination?.route
        if (current != target) {
            navController.navigate(target) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Login) {
        composable(Routes.Login) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Login) { inclusive = true }
                    }
                },
                repository = repository,
            )
        }
        composable(Routes.Home) {
            HomeShell(
                permissions = permissions,
                companyName = session.companyName ?: "Workspace",
                userName = session.userName,
                userEmail = session.userEmail,
                bootError = bootError,
                repository = repository,
                themePreference = themePreference,
                onThemeChange = { scope.launch { themeStore.setPreference(it) } },
                onLogout = {
                    scope.launch {
                        repository.logout()
                        navController.navigate(Routes.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeShell(
    permissions: Set<String>,
    companyName: String,
    userName: String?,
    userEmail: String?,
    bootError: String?,
    repository: WifiRepository,
    themePreference: ThemePreference,
    onThemeChange: (ThemePreference) -> Unit,
    onLogout: () -> Unit,
) {
    val tabs = listOf(
        TabItem(Routes.Dashboard, "Home", "wifi.dashboard.view", Icons.Default.Dashboard),
        TabItem(Routes.Sell, "Till", "wifi.vouchers.sell", Icons.Default.ShoppingCart),
        TabItem(Routes.Vouchers, "Codes", "wifi.vouchers.view", Icons.Default.QrCode),
        TabItem(Routes.More, "More", null, Icons.Default.MoreHoriz),
    ).filter { it.permission == null || permissions.contains(it.permission) }

    val navController = rememberNavController()
    val start = tabs.firstOrNull()?.route ?: Routes.More
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route ?: start

    fun selected(tab: String): Boolean = when (tab) {
        Routes.Dashboard -> current == Routes.Dashboard
        Routes.Sell -> current == Routes.Sell
        Routes.Vouchers -> current.startsWith("vouchers")
        Routes.More -> current == Routes.More ||
            current.startsWith("sites") ||
            current.startsWith("plans") ||
            current == Routes.Live ||
            current == Routes.Sales ||
            current == Routes.Settings
        else -> current == tab
    }

    Scaffold(
        bottomBar = {
            if (tabs.isNotEmpty()) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = selected(tab.route),
                            onClick = {
                                val atRoot = current == tab.route
                                if (!atRoot) {
                                    val popped = navController.popBackStack(tab.route, inclusive = false)
                                    if (!popped) {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = false
                                            }
                                            launchSingleTop = true
                                            restoreState = false
                                        }
                                    }
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Dashboard) {
                DashboardScreen(
                    repository = repository,
                    companyName = companyName,
                    bootError = bootError,
                    canOpenLive = permissions.contains("wifi.live.view"),
                    canOpenSales = permissions.contains("wifi.dashboard.view"),
                    onOpenLive = { navController.navigate(Routes.Live) },
                    onOpenSales = { navController.navigate(Routes.Sales) },
                )
            }
            composable(Routes.Live) {
                LiveMonitorScreen(repository = repository)
            }
            composable(Routes.Sales) {
                SalesScreen(repository = repository)
            }
            composable(Routes.Sites) {
                SitesScreen(
                    repository = repository,
                    onCreate = { navController.navigate(Routes.SiteCreate) },
                    onOpen = { id -> navController.navigate("sites/$id") },
                )
            }
            composable(Routes.SiteCreate) {
                SiteFormScreen(
                    repository = repository,
                    siteId = null,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                Routes.SiteEdit,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                SiteFormScreen(
                    repository = repository,
                    siteId = entry.arguments?.getLong("id"),
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                Routes.SiteDetail,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                SiteDetailScreen(
                    repository = repository,
                    siteId = id,
                    onEdit = { navController.navigate("sites/$id/edit") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Plans) {
                PlansScreen(
                    repository = repository,
                    onCreate = { navController.navigate(Routes.PlanCreate) },
                    onEdit = { id -> navController.navigate("plans/$id/edit") },
                )
            }
            composable(Routes.PlanCreate) {
                PlanFormScreen(
                    repository = repository,
                    planId = null,
                    onDone = { navController.popBackStack() },
                    onOpenPlan = { id ->
                        navController.navigate("plans/$id/edit") {
                            popUpTo(Routes.PlanCreate) { inclusive = true }
                        }
                    },
                    onIssued = { voucher -> navController.navigate("vouchers/${voucher.id}") },
                )
            }
            composable(
                Routes.PlanEdit,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                PlanFormScreen(
                    repository = repository,
                    planId = entry.arguments?.getLong("id"),
                    onDone = { navController.popBackStack() },
                    onOpenPlan = { id -> navController.navigate("plans/$id/edit") },
                    onIssued = { voucher -> navController.navigate("vouchers/${voucher.id}") },
                )
            }
            composable(Routes.Sell) {
                SellScreen(
                    repository = repository,
                    canOpenSites = permissions.contains("wifi.sites.manage"),
                    canOpenPlans = permissions.contains("wifi.plans.manage"),
                    onOpenSites = { navController.navigate(Routes.Sites) },
                    onOpenPlans = { navController.navigate(Routes.Plans) },
                )
            }
            composable(Routes.Vouchers) {
                VouchersScreen(
                    repository = repository,
                    onOpen = { id -> navController.navigate("vouchers/$id") },
                )
            }
            composable(
                Routes.VoucherDetail,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                VoucherDetailScreen(
                    repository = repository,
                    voucherId = entry.arguments?.getLong("id") ?: return@composable,
                    canRevoke = permissions.contains("wifi.vouchers.revoke"),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(repository = repository)
            }
            composable(Routes.More) {
                MoreScreen(
                    companyName = companyName,
                    userName = userName,
                    userEmail = userEmail,
                    canOpenLive = permissions.contains("wifi.live.view"),
                    canOpenSales = permissions.contains("wifi.dashboard.view"),
                    canOpenSites = permissions.contains("wifi.sites.manage"),
                    canOpenPlans = permissions.contains("wifi.plans.manage"),
                    canOpenSettings = permissions.contains("wifi.settings.manage"),
                    themePreference = themePreference,
                    onThemeChange = onThemeChange,
                    onOpenLive = { navController.navigate(Routes.Live) },
                    onOpenSales = { navController.navigate(Routes.Sales) },
                    onOpenSites = { navController.navigate(Routes.Sites) },
                    onOpenPlans = { navController.navigate(Routes.Plans) },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onLogout = onLogout,
                )
            }
        }
    }
}
