package com.cryptolens.app.features.market.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

@ScopeMetadata
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
public final class BinanceWebSocketManager_Factory implements Factory<BinanceWebSocketManager> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Json> jsonProvider;

  public BinanceWebSocketManager_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public BinanceWebSocketManager get() {
    return newInstance(okHttpClientProvider.get(), jsonProvider.get());
  }

  public static BinanceWebSocketManager_Factory create(
      javax.inject.Provider<OkHttpClient> okHttpClientProvider,
      javax.inject.Provider<Json> jsonProvider) {
    return new BinanceWebSocketManager_Factory(Providers.asDaggerProvider(okHttpClientProvider), Providers.asDaggerProvider(jsonProvider));
  }

  public static BinanceWebSocketManager_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    return new BinanceWebSocketManager_Factory(okHttpClientProvider, jsonProvider);
  }

  public static BinanceWebSocketManager newInstance(OkHttpClient okHttpClient, Json json) {
    return new BinanceWebSocketManager(okHttpClient, json);
  }
}
