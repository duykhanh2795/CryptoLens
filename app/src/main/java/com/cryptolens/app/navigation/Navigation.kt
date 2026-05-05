package com.cryptolens.app.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cryptolens.app.features.auth.presentation.login.LoginScreen
import com.cryptolens.app.features.auth.presentation.register.RegisterScreen
import com.cryptolens.app.features.auth.presentation.splash.SplashScreen
import com.cryptolens.app.features.exchange.presentation.connect.ConnectExchangeScreen
import com.cryptolens.app.features.exchange.presentation.manage.ManageExchangeScreen
import com.cryptolens.app.features.home.presentation.HomeScreen
import com.cryptolens.app.features.market.presentation.detail.CoinDetailScreen
import com.cryptolens.app.features.market.presentation.marketlist.MarketListScreen
import com.cryptolens.app.features.portfolio.presentation.PortfolioScreen
import com.cryptolens.app.features.profile.presentation.ProfileScreen
import com.cryptolens.app.features.settings.presentation.SettingsScreen
import com.cryptolens.app.features.watchlist.presentation.WatchlistScreen
import com.cryptolens.app.ui.theme.*

sealed class Screen(val route: String) {
    data object Splash          : Screen("splash")
    data object Login           : Screen("login")
    data object Register        : Screen("register")
    data object Home            : Screen("home")
    data object Markets         : Screen("markets")
    data object Watchlist       : Screen("watchlist")
    data object Portfolio       : Screen("portfolio")
    data object Profile         : Screen("profile")
    data object ManageExchanges : Screen("manage_exchanges")
    data object ConnectExchange : Screen("connect_exchange")
    data object Settings        : Screen("settings")
    data object CoinDetail      : Screen("coin_detail/{coinId}") {
        fun createRoute(coinId: String) = "coin_detail/$coinId"
    }
}

private val mainRoutes = setOf(
    Screen.Home.route,
    Screen.Markets.route,
    Screen.Watchlist.route,
    Screen.Portfolio.route,
    Screen.Profile.route,
)

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val iconSelected: ImageVector,
    val iconDefault: ImageVector,
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,      "Home",      Icons.Filled.Home,                 Icons.Outlined.Home),
    BottomNavItem(Screen.Markets,   "Markets",   Icons.Filled.BarChart,             Icons.Outlined.BarChart),
    BottomNavItem(Screen.Watchlist, "Watchlist", Icons.Filled.Star,                 Icons.Outlined.StarOutline),
    BottomNavItem(Screen.Portfolio, "Portfolio", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    BottomNavItem(Screen.Profile,   "Profile",   Icons.Filled.Person,               Icons.Outlined.Person),
)

@Composable
fun CryptoLensNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStack    by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in mainRoutes

    Scaffold(
        containerColor = BackgroundDark,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter   = slideInVertically { it } + fadeIn(tween(200)),
                exit    = slideOutVertically { it } + fadeOut(tween(200)),
            ) {
                NavigationBar(
                    containerColor = SurfaceElevated,
                    tonalElevation = 0.dp,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = {
                                Icon(
                                    if (selected) item.iconSelected else item.iconDefault,
                                    contentDescription = item.label,
                                )
                            },
                            label  = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = BinanceYellow,
                                selectedTextColor   = BinanceYellow,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor      = SurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController       = navController,
            startDestination    = Screen.Splash.route,
            modifier            = Modifier.padding(padding),
            enterTransition     = { fadeIn(tween(220)) },
            exitTransition      = { fadeOut(tween(180)) },
            popEnterTransition  = { fadeIn(tween(220)) },
            popExitTransition   = { fadeOut(tween(180)) },
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToHome     = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                )
            }

            composable(Screen.Register.route) {
                RegisterScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateBack   = { navController.popBackStack() },
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onCoinClick    = { navController.navigate(Screen.CoinDetail.createRoute(it)) },
                    onSeeAllMarket = { navController.navigate(Screen.Markets.route) },
                )
            }

            composable(Screen.Markets.route) {
                MarketListScreen(
                    onCoinClick        = { navController.navigate(Screen.CoinDetail.createRoute(it)) },
                    onNavigateToWatchlist = { navController.navigate(Screen.Watchlist.route) },
                )
            }

            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    onCoinClick = { navController.navigate(Screen.CoinDetail.createRoute(it)) },
                )
            }

            composable(Screen.Portfolio.route) {
                PortfolioScreen(
                    onCoinClick       = { navController.navigate(Screen.CoinDetail.createRoute(it)) },
                    onConnectExchange = { navController.navigate(Screen.ManageExchanges.route) },
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToLogin     = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToExchanges = { navController.navigate(Screen.ManageExchanges.route) },
                    onNavigateToSettings  = { navController.navigate(Screen.Settings.route) },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.ManageExchanges.route) {
                ManageExchangeScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onConnectNew   = { navController.navigate(Screen.ConnectExchange.route) },
                )
            }

            composable(Screen.ConnectExchange.route) {
                ConnectExchangeScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route     = Screen.CoinDetail.route,
                arguments = listOf(navArgument("coinId") { type = NavType.StringType }),
            ) {
                CoinDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
