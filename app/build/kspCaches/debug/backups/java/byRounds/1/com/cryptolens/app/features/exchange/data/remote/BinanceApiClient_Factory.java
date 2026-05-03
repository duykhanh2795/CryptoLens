package com.cryptolens.app.features.exchange.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

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
public final class BinanceApiClient_Factory implements Factory<BinanceApiClient> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Json> jsonProvider;

  public BinanceApiClient_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public BinanceApiClient get() {
    return newInstance(okHttpClientProvider.get(), jsonProvider.get());
  }

  public static BinanceApiClient_Factory create(
      javax.inject.Provider<OkHttpClient> okHttpClientProvider,
      javax.inject.Provider<Json> jsonProvider) {
    return new BinanceApiClient_Factory(Providers.asDaggerProvider(okHttpClientProvider), Providers.asDaggerProvider(jsonProvider));
  }

  public static BinanceApiClient_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider) {
    return new BinanceApiClient_Factory(okHttpClientProvider, jsonProvider);
  }

  public static BinanceApiClient newInstance(OkHttpClient okHttpClient, Json json) {
    return new BinanceApiClient(okHttpClient, json);
  }
}
