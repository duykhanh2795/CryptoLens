package com.cryptolens.app.di;

import com.cryptolens.app.features.exchange.data.remote.BinanceApiClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class NetworkModule_ProvideBinanceApiClientFactory implements Factory<BinanceApiClient> {
  private final Provider<OkHttpClient> clientProvider;

  private final Provider<Json> jsonProvider;

  public NetworkModule_ProvideBinanceApiClientFactory(Provider<OkHttpClient> clientProvider,
      Provider<Json> jsonProvider) {
    this.clientProvider = clientProvider;
    this.jsonProvider = jsonProvider;
  }

  @Override
  public BinanceApiClient get() {
    return provideBinanceApiClient(clientProvider.get(), jsonProvider.get());
  }

  public static NetworkModule_ProvideBinanceApiClientFactory create(
      javax.inject.Provider<OkHttpClient> clientProvider,
      javax.inject.Provider<Json> jsonProvider) {
    return new NetworkModule_ProvideBinanceApiClientFactory(Providers.asDaggerProvider(clientProvider), Providers.asDaggerProvider(jsonProvider));
  }

  public static NetworkModule_ProvideBinanceApiClientFactory create(
      Provider<OkHttpClient> clientProvider, Provider<Json> jsonProvider) {
    return new NetworkModule_ProvideBinanceApiClientFactory(clientProvider, jsonProvider);
  }

  public static BinanceApiClient provideBinanceApiClient(OkHttpClient client, Json json) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideBinanceApiClient(client, json));
  }
}
