package com.cryptolens.app.features.market.data.repository;

import com.cryptolens.app.features.market.data.remote.BinanceApiService;
import com.cryptolens.app.features.market.data.remote.BinanceFuturesApiService;
import com.cryptolens.app.features.market.data.remote.BinanceFuturesDataApiService;
import com.cryptolens.app.features.market.data.remote.BinanceWebSocketManager;
import com.cryptolens.app.features.market.data.remote.CoinGeckoApiService;
import com.cryptolens.app.features.market.data.remote.MarketCache;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class MarketRepositoryImpl_Factory implements Factory<MarketRepositoryImpl> {
  private final Provider<CoinGeckoApiService> coinGeckoApiProvider;

  private final Provider<BinanceApiService> binanceApiProvider;

  private final Provider<BinanceFuturesApiService> binanceFuturesApiProvider;

  private final Provider<BinanceFuturesDataApiService> binanceFuturesDataApiProvider;

  private final Provider<BinanceWebSocketManager> binanceWsProvider;

  private final Provider<MarketCache> cacheProvider;

  public MarketRepositoryImpl_Factory(Provider<CoinGeckoApiService> coinGeckoApiProvider,
      Provider<BinanceApiService> binanceApiProvider,
      Provider<BinanceFuturesApiService> binanceFuturesApiProvider,
      Provider<BinanceFuturesDataApiService> binanceFuturesDataApiProvider,
      Provider<BinanceWebSocketManager> binanceWsProvider, Provider<MarketCache> cacheProvider) {
    this.coinGeckoApiProvider = coinGeckoApiProvider;
    this.binanceApiProvider = binanceApiProvider;
    this.binanceFuturesApiProvider = binanceFuturesApiProvider;
    this.binanceFuturesDataApiProvider = binanceFuturesDataApiProvider;
    this.binanceWsProvider = binanceWsProvider;
    this.cacheProvider = cacheProvider;
  }

  @Override
  public MarketRepositoryImpl get() {
    return newInstance(coinGeckoApiProvider.get(), binanceApiProvider.get(), binanceFuturesApiProvider.get(), binanceFuturesDataApiProvider.get(), binanceWsProvider.get(), cacheProvider.get());
  }

  public static MarketRepositoryImpl_Factory create(
      javax.inject.Provider<CoinGeckoApiService> coinGeckoApiProvider,
      javax.inject.Provider<BinanceApiService> binanceApiProvider,
      javax.inject.Provider<BinanceFuturesApiService> binanceFuturesApiProvider,
      javax.inject.Provider<BinanceFuturesDataApiService> binanceFuturesDataApiProvider,
      javax.inject.Provider<BinanceWebSocketManager> binanceWsProvider,
      javax.inject.Provider<MarketCache> cacheProvider) {
    return new MarketRepositoryImpl_Factory(Providers.asDaggerProvider(coinGeckoApiProvider), Providers.asDaggerProvider(binanceApiProvider), Providers.asDaggerProvider(binanceFuturesApiProvider), Providers.asDaggerProvider(binanceFuturesDataApiProvider), Providers.asDaggerProvider(binanceWsProvider), Providers.asDaggerProvider(cacheProvider));
  }

  public static MarketRepositoryImpl_Factory create(
      Provider<CoinGeckoApiService> coinGeckoApiProvider,
      Provider<BinanceApiService> binanceApiProvider,
      Provider<BinanceFuturesApiService> binanceFuturesApiProvider,
      Provider<BinanceFuturesDataApiService> binanceFuturesDataApiProvider,
      Provider<BinanceWebSocketManager> binanceWsProvider, Provider<MarketCache> cacheProvider) {
    return new MarketRepositoryImpl_Factory(coinGeckoApiProvider, binanceApiProvider, binanceFuturesApiProvider, binanceFuturesDataApiProvider, binanceWsProvider, cacheProvider);
  }

  public static MarketRepositoryImpl newInstance(CoinGeckoApiService coinGeckoApi,
      BinanceApiService binanceApi, BinanceFuturesApiService binanceFuturesApi,
      BinanceFuturesDataApiService binanceFuturesDataApi, BinanceWebSocketManager binanceWs,
      MarketCache cache) {
    return new MarketRepositoryImpl(coinGeckoApi, binanceApi, binanceFuturesApi, binanceFuturesDataApi, binanceWs, cache);
  }
}
