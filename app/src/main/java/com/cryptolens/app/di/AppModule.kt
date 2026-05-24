package com.cryptolens.app.di

import android.content.Context
import androidx.room.Room
import com.cryptolens.app.BuildConfig
import com.cryptolens.app.core.data.local.CryptoDatabase
import com.cryptolens.app.core.data.local.CryptoDatabaseMigrations
import com.cryptolens.app.features.alerts.data.local.dao.PriceAlertDao
import com.cryptolens.app.features.alerts.data.repository.AlertRepositoryImpl
import com.cryptolens.app.features.alerts.domain.repository.AlertRepository
import com.cryptolens.app.features.auth.data.repository.AuthRepositoryImpl
import com.cryptolens.app.features.auth.data.repository.LoginPreferencesRepositoryImpl
import com.cryptolens.app.features.auth.domain.repository.AuthRepository
import com.cryptolens.app.features.auth.domain.repository.LoginPreferencesRepository
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionDao
import com.cryptolens.app.features.exchange.data.remote.BinanceApiClient
import com.cryptolens.app.features.exchange.data.repository.ExchangeRepositoryImpl
import com.cryptolens.app.features.exchange.domain.repository.ExchangeRepository
import com.cryptolens.app.features.market.data.remote.BinanceApiService
import com.cryptolens.app.features.market.data.remote.BinanceFuturesDataApiService
import com.cryptolens.app.features.market.data.remote.BinanceFuturesApiService
import com.cryptolens.app.features.market.data.remote.BinanceWebSocketManager
import com.cryptolens.app.features.market.data.remote.CoinGeckoApiService
import com.cryptolens.app.features.market.data.repository.MarketRepositoryImpl
import com.cryptolens.app.features.market.domain.repository.MarketRepository
import com.cryptolens.app.features.news.data.repository.NewsRepositoryImpl
import com.cryptolens.app.features.news.domain.repository.NewsRepository
import com.cryptolens.app.features.settings.data.repository.SettingsRepositoryImpl
import com.cryptolens.app.features.settings.domain.repository.SettingsRepository
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioSnapshotDao
import com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao
import com.cryptolens.app.features.portfolio.data.repository.PortfolioRepositoryImpl
import com.cryptolens.app.features.portfolio.domain.repository.PortfolioRepository
import com.cryptolens.app.features.watchlist.data.local.dao.WatchlistDao
import com.cryptolens.app.features.watchlist.data.repository.WatchlistRepositoryImpl
import com.cryptolens.app.features.watchlist.domain.repository.WatchlistRepository
import com.cryptolens.app.features.wallet.data.local.dao.WalletWatchDao
import com.cryptolens.app.features.wallet.data.walletconnect.WalletConnectRepositoryImpl
import com.cryptolens.app.features.wallet.data.repository.WalletDiscoveryRepositoryImpl
import com.cryptolens.app.features.wallet.data.repository.WalletWatchRepositoryImpl
import com.cryptolens.app.features.wallet.domain.repository.WalletConnectionRepository
import com.cryptolens.app.features.wallet.domain.repository.WalletDiscoveryRepository
import com.cryptolens.app.features.wallet.domain.repository.WalletWatchRepository
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient         = true
        coerceInputValues = true
    }

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            redactHeader("X-API-Key")
            redactHeader("X-MBX-APIKEY")
            redactHeader("x-goog-api-key")
            level = HttpLoggingInterceptor.Level.NONE
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    @Provides @Singleton @Named("coingecko")
    fun provideCoinGeckoRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton @Named("binance")
    fun provideBinanceRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.binance.com/api/v3/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton @Named("binanceFutures")
    fun provideBinanceFuturesRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://fapi.binance.com/fapi/v1/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton @Named("binanceFuturesData")
    fun provideBinanceFuturesDataRetrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://fapi.binance.com/futures/data/")
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides @Singleton
    fun provideCoinGeckoApiService(@Named("coingecko") r: Retrofit): CoinGeckoApiService =
        r.create(CoinGeckoApiService::class.java)

    @Provides @Singleton
    fun provideBinanceApiService(@Named("binance") r: Retrofit): BinanceApiService =
        r.create(BinanceApiService::class.java)

    @Provides @Singleton
    fun provideBinanceFuturesApiService(@Named("binanceFutures") r: Retrofit): BinanceFuturesApiService =
        r.create(BinanceFuturesApiService::class.java)

    @Provides @Singleton
    fun provideBinanceFuturesDataApiService(
        @Named("binanceFuturesData") r: Retrofit,
    ): BinanceFuturesDataApiService =
        r.create(BinanceFuturesDataApiService::class.java)

    @Provides @Singleton
    fun provideBinanceWsManager(client: OkHttpClient, json: Json): BinanceWebSocketManager =
        BinanceWebSocketManager(client, json)

    @Provides @Singleton
    fun provideBinanceApiClient(client: OkHttpClient, json: Json): BinanceApiClient =
        BinanceApiClient(client, json)
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
    ) {
        install(Auth) {
            alwaysAutoRefresh   = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): CryptoDatabase =
        Room.databaseBuilder(ctx, CryptoDatabase::class.java, CryptoDatabase.DATABASE_NAME)
            .addMigrations(*CryptoDatabaseMigrations.ALL)
            .build()

    @Provides @Singleton
    fun providePortfolioAssetDao(db: CryptoDatabase): PortfolioAssetDao = db.portfolioAssetDao()

    @Provides @Singleton
    fun provideTransactionDao(db: CryptoDatabase): TransactionDao = db.transactionDao()

    @Provides @Singleton
    fun providePortfolioSnapshotDao(db: CryptoDatabase): PortfolioSnapshotDao = db.portfolioSnapshotDao()

    @Provides @Singleton
    fun provideExchangeConnectionDao(db: CryptoDatabase): ExchangeConnectionDao = db.exchangeConnectionDao()

    @Provides @Singleton
    fun provideWatchlistDao(db: CryptoDatabase): WatchlistDao = db.watchlistDao()

    @Provides @Singleton
    fun providePriceAlertDao(db: CryptoDatabase): PriceAlertDao = db.priceAlertDao()

    @Provides @Singleton
    fun provideWalletWatchDao(db: CryptoDatabase): WalletWatchDao = db.walletWatchDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindMarketRepository(impl: MarketRepositoryImpl): MarketRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindLoginPreferencesRepository(impl: LoginPreferencesRepositoryImpl): LoginPreferencesRepository

    @Binds @Singleton
    abstract fun bindPortfolioRepository(impl: PortfolioRepositoryImpl): PortfolioRepository

    @Binds @Singleton
    abstract fun bindExchangeRepository(impl: ExchangeRepositoryImpl): ExchangeRepository

    @Binds @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds @Singleton
    abstract fun bindAlertRepository(impl: AlertRepositoryImpl): AlertRepository

    @Binds @Singleton
    abstract fun bindWalletWatchRepository(impl: WalletWatchRepositoryImpl): WalletWatchRepository

    @Binds @Singleton
    abstract fun bindWalletConnectionRepository(impl: WalletConnectRepositoryImpl): WalletConnectionRepository

    @Binds @Singleton
    abstract fun bindWalletDiscoveryRepository(impl: WalletDiscoveryRepositoryImpl): WalletDiscoveryRepository

    @Binds @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository
}
