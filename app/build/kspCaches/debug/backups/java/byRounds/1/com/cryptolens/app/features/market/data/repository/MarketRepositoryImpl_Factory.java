package com.cryptolens.app.features.market.data.repository;

import com.cryptolens.app.features.market.data.remote.BinanceApiService;
import com.cryptolens.app.features.market.data.remote.BinanceWebSocketManager;
import com.cryptolens.app.features.market.data.remote.CoinGeckoApiService;
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

  private final Provider<BinanceWebSocketManager> binanceWsProvider;

  public MarketRepositoryImpl_Factory(Provider<CoinGeckoApiService> coinGeckoApiProvider,
      Provider<BinanceApiService> binanceApiProvider,
      Provider<BinanceWebSocketManager> binanceWsProvider) {
    this.coinGeckoApiProvider = coinGeckoApiProvider;
    this.binanceApiProvider = binanceApiProvider;
    this.binanceWsProvider = binanceWsProvider;
  }

  @Override
  public MarketRepositoryImpl get() {
    return newInstance(coinGeckoApiProvider.get(), binanceApiProvider.get(), binanceWsProvider.get());
  }

  public static MarketRepositoryImpl_Factory create(
      javax.inject.Provider<CoinGeckoApiService> coinGeckoApiProvider,
      javax.inject.Provider<BinanceApiService> binanceApiProvider,
      javax.inject.Provider<BinanceWebSocketManager> binanceWsProvider) {
    return new MarketRepositoryImpl_Factory(Providers.asDaggerProvider(coinGeckoApiProvider), Providers.asDaggerProvider(binanceApiProvider), Providers.asDaggerProvider(binanceWsProvider));
  }

  public static MarketRepositoryImpl_Factory create(
      Provider<CoinGeckoApiService> coinGeckoApiProvider,
      Provider<BinanceApiService> binanceApiProvider,
      Provider<BinanceWebSocketManager> binanceWsProvider) {
    return new MarketRepositoryImpl_Factory(coinGeckoApiProvider, binanceApiProvider, binanceWsProvider);
  }

  public static MarketRepositoryImpl newInstance(CoinGeckoApiService coinGeckoApi,
      BinanceApiService binanceApi, BinanceWebSocketManager binanceWs) {
    return new MarketRepositoryImpl(coinGeckoApi, binanceApi, binanceWs);
  }
}
