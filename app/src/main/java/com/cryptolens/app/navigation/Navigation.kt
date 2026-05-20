package com.cryptolens.app.navigation

import android.net.Uri
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
import com.cryptolens.app.features.alerts.presentation.AlertCoinPrefill
import com.cryptolens.app.features.alerts.presentation.AlertsScreen
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.ai.presentation.AiAssistantOverlay
import com.cryptolens.app.features.auth.presentation.login.LoginScreen
import com.cryptolens.app.features.auth.presentation.register.RegisterScreen
import com.cryptolens.app.features.auth.presentation.splash.SplashScreen
import com.cryptolens.app.features.converter.presentation.ConverterScreen
import com.cryptolens.app.features.exchange.presentation.connect.ConnectExchangeScreen
import com.cryptolens.app.features.exchange.presentation.manage.ManageExchangeScreen
import com.cryptolens.app.features.home.presentation.HomeScreen
import com.cryptolens.app.features.market.presentation.detail.CoinDetailScreen
import com.cryptolens.app.features.market.presentation.marketlist.MarketListScreen
import com.cryptolens.app.features.news.presentation.NewsScreen
import com.cryptolens.app.features.portfolio.presentation.PortfolioScreen
import com.cryptolens.app.features.profile.presentation.ProfileScreen
import com.cryptolens.app.features.settings.presentation.SettingsScreen
import com.cryptolens.app.features.wallet.presentation.TrendingWalletDetailScreen
import com.cryptolens.app.features.wallet.presentation.TrendingWalletsScreen
import com.cryptolens.app.features.wallet.presentation.WalletWatchlistScreen
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
    data object Converter       : Screen("converter")
    data object Alerts          : Screen("alerts")
    data object News            : Screen("news?symbol={symbol}") {
        fun createRoute(symbol: String? = null): String =
            if (symbol.isNullOrBlank()) {
                "news"
            } else {
                "news?symbol=${Uri.encode(symbol)}"
            }
    }
    data object WalletWatchlist : Screen("wallet_watchlist")
    data object TrendingWallets : Screen("trending_wallets")
    data object TrendingWalletDetail : Screen("trending_wallet/{chain}/{address}") {
        fun createRoute(chain: String, address: String): String =
            "trending_wallet/${Uri.encode(chain)}/${Uri.encode(address)}"
    }
    data object AlertsFromCoin  : Screen(
        "alerts/create?coinId={coinId}&symbol={symbol}&name={name}&imageUrl={imageUrl}&price={price}&volume={volume}&marketCap={marketCap}&alertType={alertType}"
    ) {
        fun createRoute(
            coinId: String,
            symbol: String,
            name: String,
            imageUrl: String,
            price: Double,
            volume: Double = 0.0,
            marketCap: Double = 0.0,
            alertType: AlertType = AlertType.PRICE_LIMIT,
        ): String =
            "alerts/create?coinId=${Uri.encode(coinId)}" +
                "&symbol=${Uri.encode(symbol)}" +
                "&name=${Uri.encode(name)}" +
                "&imageUrl=${Uri.encode(imageUrl)}" +
                "&price=${Uri.encode(price.toString())}" +
                "&volume=${Uri.encode(volume.toString())}" +
                "&marketCap=${Uri.encode(marketCap.toString())}" +
                "&alertType=${Uri.encode(alertType.name)}"
    }
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

private val aiHiddenRoutes = setOf(
    Screen.Splash.route,
    Screen.Login.route,
    Screen.Register.route,
)

@Composable
fun CryptoLensNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val backStack    by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in mainRoutes

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onWalletClick  = { chain, address ->
                        navController.navigate(Screen.TrendingWalletDetail.createRoute(chain.name, address))
                    },
                    onSeeAllWallets = { navController.navigate(Screen.TrendingWallets.route) },
                    onSeeAllNews = { navController.navigate(Screen.News.createRoute()) },
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
                    onNavigateToAlerts    = { navController.navigate(Screen.Alerts.route) },
                    onNavigateToWalletWatchlist = { navController.navigate(Screen.WalletWatchlist.route) },
                    onNavigateToConverter = { navController.navigate(Screen.Converter.route) },
                )
            }

            composable(Screen.Alerts.route) {
                AlertsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.News.route,
                arguments = listOf(
                    navArgument("symbol") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) {
                NewsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Screen.AlertsFromCoin.route,
                arguments = listOf(
                    navArgument("coinId") { type = NavType.StringType },
                    navArgument("symbol") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType },
                    navArgument("imageUrl") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("price") {
                        type = NavType.StringType
                        defaultValue = "0.0"
                    },
                    navArgument("volume") {
                        type = NavType.StringType
                        defaultValue = "0.0"
                    },
                    navArgument("marketCap") {
                        type = NavType.StringType
                        defaultValue = "0.0"
                    },
                    navArgument("alertType") {
                        type = NavType.StringType
                        defaultValue = AlertType.PRICE_LIMIT.name
                    },
                ),
            ) { entry ->
                val args = entry.arguments
                val alertType = runCatching {
                    AlertType.valueOf(args?.getString("alertType").orEmpty())
                }.getOrDefault(AlertType.PRICE_LIMIT)
                AlertsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    prefill = AlertCoinPrefill(
                        coinId = args?.getString("coinId").orEmpty(),
                        symbol = args?.getString("symbol").orEmpty(),
                        name = args?.getString("name").orEmpty(),
                        imageUrl = args?.getString("imageUrl").orEmpty(),
                        currentPrice = args?.getString("price")?.toDoubleOrNull() ?: 0.0,
                        currentVolume = args?.getString("volume")?.toDoubleOrNull() ?: 0.0,
                        currentMarketCap = args?.getString("marketCap")?.toDoubleOrNull() ?: 0.0,
                        alertType = alertType,
                    ),
                )
            }

            composable(Screen.WalletWatchlist.route) {
                WalletWatchlistScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.TrendingWallets.route) {
                TrendingWalletsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onWalletClick = { chain, address ->
                        navController.navigate(Screen.TrendingWalletDetail.createRoute(chain.name, address))
                    },
                )
            }

            composable(
                route = Screen.TrendingWalletDetail.route,
                arguments = listOf(
                    navArgument("chain") { type = NavType.StringType },
                    navArgument("address") { type = NavType.StringType },
                ),
            ) {
                TrendingWalletDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Screen.Converter.route) {
                ConverterScreen(
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
                    onSetAlert = { coin, alertType ->
                        navController.navigate(
                            Screen.AlertsFromCoin.createRoute(
                                coinId = coin.id,
                                symbol = coin.symbol,
                                name = coin.name,
                                imageUrl = coin.imageUrl,
                                price = coin.currentPrice,
                                volume = coin.volume24h,
                                marketCap = coin.marketCap,
                                alertType = alertType,
                            )
                        )
                    },
                    onSeeAllNews = { symbol ->
                        navController.navigate(Screen.News.createRoute(symbol))
                    },
                    onOpenPortfolio = {
                        navController.navigate(Screen.Portfolio.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            }
        }

        AiAssistantOverlay(
            visible = currentRoute != null && currentRoute !in aiHiddenRoutes,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
