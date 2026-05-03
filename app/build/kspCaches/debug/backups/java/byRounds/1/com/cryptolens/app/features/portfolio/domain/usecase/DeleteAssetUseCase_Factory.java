package com.cryptolens.app.features.portfolio.domain.usecase;

import com.cryptolens.app.features.portfolio.domain.repository.PortfolioRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class DeleteAssetUseCase_Factory implements Factory<DeleteAssetUseCase> {
  private final Provider<PortfolioRepository> repoProvider;

  public DeleteAssetUseCase_Factory(Provider<PortfolioRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @Override
  public DeleteAssetUseCase get() {
    return newInstance(repoProvider.get());
  }

  public static DeleteAssetUseCase_Factory create(
      javax.inject.Provider<PortfolioRepository> repoProvider) {
    return new DeleteAssetUseCase_Factory(Providers.asDaggerProvider(repoProvider));
  }

  public static DeleteAssetUseCase_Factory create(Provider<PortfolioRepository> repoProvider) {
    return new DeleteAssetUseCase_Factory(repoProvider);
  }

  public static DeleteAssetUseCase newInstance(PortfolioRepository repo) {
    return new DeleteAssetUseCase(repo);
  }
}
