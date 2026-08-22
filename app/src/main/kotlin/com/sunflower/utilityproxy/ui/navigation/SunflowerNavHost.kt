package com.sunflower.utilityproxy.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List as ListIcon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sunflower.utilityproxy.R
import com.sunflower.utilityproxy.ui.servers.ServersScreen
import com.sunflower.utilityproxy.ui.settings.AboutScreen
import com.sunflower.utilityproxy.ui.settings.LogsScreen
import com.sunflower.utilityproxy.ui.settings.ResetScreen
import com.sunflower.utilityproxy.ui.settings.SettingsScreen
import com.sunflower.utilityproxy.ui.subscriptions.AddSubscriptionScreen
import com.sunflower.utilityproxy.ui.subscriptions.SubscriptionsScreen

object Routes {
    const val SERVERS = "servers"
    const val SUBSCRIPTIONS = "subscriptions"
    const val ADD_SUBSCRIPTION = "add_subscription"
    const val SETTINGS = "settings"
    const val LOGS = "logs"
    const val RESET = "reset"
    const val ABOUT = "about"
}

private data class BottomTab(val route: String, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

// @StringRes id, не готовая строка — top-level val инициализируется вне
// composable-контекста, stringResource() резолвится в NavigationBarItem ниже.
private val bottomTabs = listOf(
    BottomTab(Routes.SERVERS, R.string.nav_servers, Icons.Filled.Home),
    BottomTab(Routes.SUBSCRIPTIONS, R.string.nav_subscriptions, Icons.Filled.ListIcon),
    BottomTab(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

/**
 * Нижний таб-бар вместо TopAppBar-кнопок кросс-навигации из прошлых
 * версий — привычный вид для айфон-подобного интерфейса (3 основных
 * раздела всегда на виду внизу). AddSubscription/Logs/Reset/About —
 * НЕ табы, а экраны, на которые переходят изнутри разделов (push,
 * не таб) — как модальные/detail-экраны в iOS.
 *
 * Паттерн "popUpTo(startDestination){saveState=true} + launchSingleTop +
 * restoreState" при переключении таба — стандартная, документированная
 * официально схема Compose Navigation для нижних таб-баров (сохраняет
 * состояние каждого таба при переключении между ними), не самодельная.
 */
@Composable
fun SunflowerNavHost(pendingDeepLink: String? = null, onDeepLinkHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val deepLinkViewModel: DeepLinkViewModel = hiltViewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(pendingDeepLink) {
        val link = pendingDeepLink ?: return@LaunchedEffect
        val scheme = link.substringBefore("://", missingDelimiterValue = "")
        if (scheme == "http" || scheme == "https") {
            navController.navigate("${Routes.ADD_SUBSCRIPTION}?url=${Uri.encode(link)}")
        } else {
            deepLinkViewModel.importServerLink(link)
            navController.navigate(Routes.SERVERS) { launchSingleTop = true }
        }
        onDeepLinkHandled()
    }

    Scaffold(
        bottomBar = {
            if (currentRoute == null || bottomTabs.any { it.route == currentRoute }) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SERVERS,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable(Routes.SERVERS) {
                ServersScreen(onOpenSubscriptions = {
                    navController.navigate(Routes.SUBSCRIPTIONS) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
            composable(Routes.SUBSCRIPTIONS) {
                SubscriptionsScreen(
                    onAddSubscription = { navController.navigate(Routes.ADD_SUBSCRIPTION) },
                )
            }
            composable(
                "${Routes.ADD_SUBSCRIPTION}?url={url}",
                arguments = listOf(navArgument("url") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { backStack ->
                AddSubscriptionScreen(
                    initialUrl = backStack.arguments?.getString("url"),
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenLogs = { navController.navigate(Routes.LOGS) },
                    onOpenReset = { navController.navigate(Routes.RESET) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                )
            }
            composable(Routes.LOGS) { LogsScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.RESET) { ResetScreen(onBack = { navController.popBackStack() }) }
            composable(Routes.ABOUT) { AboutScreen(onBack = { navController.popBackStack() }) }
        }
    }
}
