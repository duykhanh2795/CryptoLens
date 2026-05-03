package com.cryptolens.app;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.cryptolens.app.core.data.local.CryptoDatabase;
import com.cryptolens.app.di.DatabaseModule_ProvideDatabaseFactory;
import com.cryptolens.app.di.DatabaseModule_ProvideExchangeConnectionDaoFactory;
import com.cryptolens.app.di.DatabaseModule_ProvidePortfolioAssetDaoFactory;
import com.cryptolens.app.di.DatabaseModule_ProvideTransactionDaoFactory;
import com.cryptolens.app.di.NetworkModule_ProvideBinanceApiClientFactory;
import com.cryptolens.app.di.NetworkModule_ProvideBinanceApiServiceFactory;
import com.cryptolens.app.di.NetworkModule_ProvideBinanceRetrofitFactory;
import com.cryptolens.app.di.NetworkModule_ProvideBinanceWsManagerFactory;
import com.cryptolens.app.di.NetworkModule_ProvideCoinGeckoApiServiceFactory;
import com.cryptolens.app.di.NetworkModule_ProvideCoinGeckoRetrofitFactory;
import com.cryptolens.app.di.NetworkModule_ProvideJsonFactory;
import com.cryptolens.app.di.NetworkModule_ProvideOkHttpClientFactory;
import com.cryptolens.app.di.SupabaseModule_ProvideSupabaseClientFactory;
import com.cryptolens.app.features.auth.data.repository.AuthRepositoryImpl;
import com.cryptolens.app.features.auth.domain.usecase.GetCurrentUserUseCase;
import com.cryptolens.app.features.auth.domain.usecase.LoginUseCase;
import com.cryptolens.app.features.auth.domain.usecase.LogoutUseCase;
import com.cryptolens.app.features.auth.domain.usecase.RegisterUseCase;
import com.cryptolens.app.features.auth.domain.usecase.ResetPasswordUseCase;
import com.cryptolens.app.features.auth.presentation.login.LoginViewModel;
import com.cryptolens.app.features.auth.presentation.login.LoginViewModel_HiltModules;
import com.cryptolens.app.features.auth.presentation.login.LoginViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.auth.presentation.login.LoginViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.auth.presentation.register.RegisterViewModel;
import com.cryptolens.app.features.auth.presentation.register.RegisterViewModel_HiltModules;
import com.cryptolens.app.features.auth.presentation.register.RegisterViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.auth.presentation.register.RegisterViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.auth.presentation.splash.SplashViewModel;
import com.cryptolens.app.features.auth.presentation.splash.SplashViewModel_HiltModules;
import com.cryptolens.app.features.auth.presentation.splash.SplashViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.auth.presentation.splash.SplashViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.exchange.data.local.CryptoManager;
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionDao;
import com.cryptolens.app.features.exchange.data.remote.BinanceApiClient;
import com.cryptolens.app.features.exchange.data.repository.ExchangeRepositoryImpl;
import com.cryptolens.app.features.exchange.domain.usecase.DeleteExchangeConnectionUseCase;
import com.cryptolens.app.features.exchange.domain.usecase.ObserveExchangeConnectionsUseCase;
import com.cryptolens.app.features.exchange.domain.usecase.SaveExchangeConnectionUseCase;
import com.cryptolens.app.features.exchange.domain.usecase.SetConnectionActiveUseCase;
import com.cryptolens.app.features.exchange.domain.usecase.SyncExchangeTradesUseCase;
import com.cryptolens.app.features.exchange.domain.usecase.ValidateApiKeyUseCase;
import com.cryptolens.app.features.exchange.presentation.connect.ConnectExchangeViewModel;
import com.cryptolens.app.features.exchange.presentation.connect.ConnectExchangeViewModel_HiltModules;
import com.cryptolens.app.features.exchange.presentation.connect.ConnectExchangeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.exchange.presentation.connect.ConnectExchangeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.exchange.presentation.manage.ManageExchangeViewModel;
import com.cryptolens.app.features.exchange.presentation.manage.ManageExchangeViewModel_HiltModules;
import com.cryptolens.app.features.exchange.presentation.manage.ManageExchangeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.exchange.presentation.manage.ManageExchangeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.home.presentation.HomeViewModel;
import com.cryptolens.app.features.home.presentation.HomeViewModel_HiltModules;
import com.cryptolens.app.features.home.presentation.HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.home.presentation.HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.market.data.remote.BinanceApiService;
import com.cryptolens.app.features.market.data.remote.BinanceWebSocketManager;
import com.cryptolens.app.features.market.data.remote.CoinGeckoApiService;
import com.cryptolens.app.features.market.data.repository.MarketRepositoryImpl;
import com.cryptolens.app.features.market.domain.usecase.GetCoinDetailUseCase;
import com.cryptolens.app.features.market.domain.usecase.GetKlinesUseCase;
import com.cryptolens.app.features.market.domain.usecase.GetTopCoinsUseCase;
import com.cryptolens.app.features.market.domain.usecase.ObserveMultipleTickersUseCase;
import com.cryptolens.app.features.market.domain.usecase.ObservePriceTickerUseCase;
import com.cryptolens.app.features.market.presentation.detail.CoinDetailViewModel;
import com.cryptolens.app.features.market.presentation.detail.CoinDetailViewModel_HiltModules;
import com.cryptolens.app.features.market.presentation.detail.CoinDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.market.presentation.detail.CoinDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.market.presentation.marketlist.MarketListViewModel;
import com.cryptolens.app.features.market.presentation.marketlist.MarketListViewModel_HiltModules;
import com.cryptolens.app.features.market.presentation.marketlist.MarketListViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.market.presentation.marketlist.MarketListViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao;
import com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao;
import com.cryptolens.app.features.portfolio.data.repository.PortfolioRepositoryImpl;
import com.cryptolens.app.features.portfolio.domain.usecase.AddTransactionUseCase;
import com.cryptolens.app.features.portfolio.domain.usecase.DeleteAssetUseCase;
import com.cryptolens.app.features.portfolio.domain.usecase.DeleteTransactionUseCase;
import com.cryptolens.app.features.portfolio.domain.usecase.ObservePortfolioSummaryUseCase;
import com.cryptolens.app.features.portfolio.domain.usecase.ObserveTransactionsUseCase;
import com.cryptolens.app.features.portfolio.presentation.PortfolioViewModel;
import com.cryptolens.app.features.portfolio.presentation.PortfolioViewModel_HiltModules;
import com.cryptolens.app.features.portfolio.presentation.PortfolioViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.portfolio.presentation.PortfolioViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.cryptolens.app.features.profile.presentation.ProfileViewModel;
import com.cryptolens.app.features.profile.presentation.ProfileViewModel_HiltModules;
import com.cryptolens.app.features.profile.presentation.ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.cryptolens.app.features.profile.presentation.ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import io.github.jan.supabase.SupabaseClient;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

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
public final class DaggerCryptoLensApp_HiltComponents_SingletonC {
  private DaggerCryptoLensApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public CryptoLensApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements CryptoLensApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public CryptoLensApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements CryptoLensApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public CryptoLensApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements CryptoLensApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public CryptoLensApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements CryptoLensApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CryptoLensApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements CryptoLensApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public CryptoLensApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements CryptoLensApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public CryptoLensApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements CryptoLensApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public CryptoLensApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends CryptoLensApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends CryptoLensApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends CryptoLensApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends CryptoLensApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(10).put(CoinDetailViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, CoinDetailViewModel_HiltModules.KeyModule.provide()).put(ConnectExchangeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ConnectExchangeViewModel_HiltModules.KeyModule.provide()).put(HomeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HomeViewModel_HiltModules.KeyModule.provide()).put(LoginViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, LoginViewModel_HiltModules.KeyModule.provide()).put(ManageExchangeViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ManageExchangeViewModel_HiltModules.KeyModule.provide()).put(MarketListViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, MarketListViewModel_HiltModules.KeyModule.provide()).put(PortfolioViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, PortfolioViewModel_HiltModules.KeyModule.provide()).put(ProfileViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, ProfileViewModel_HiltModules.KeyModule.provide()).put(RegisterViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, RegisterViewModel_HiltModules.KeyModule.provide()).put(SplashViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SplashViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends CryptoLensApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<CoinDetailViewModel> coinDetailViewModelProvider;

    private Provider<ConnectExchangeViewModel> connectExchangeViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<ManageExchangeViewModel> manageExchangeViewModelProvider;

    private Provider<MarketListViewModel> marketListViewModelProvider;

    private Provider<PortfolioViewModel> portfolioViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<RegisterViewModel> registerViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private GetCoinDetailUseCase getCoinDetailUseCase() {
      return new GetCoinDetailUseCase(singletonCImpl.marketRepositoryImplProvider.get());
    }

    private GetKlinesUseCase getKlinesUseCase() {
      return new GetKlinesUseCase(singletonCImpl.marketRepositoryImplProvider.get());
    }

    private ObservePriceTickerUseCase observePriceTickerUseCase() {
      return new ObservePriceTickerUseCase(singletonCImpl.marketRepositoryImplProvider.get());
    }

    private ValidateApiKeyUseCase validateApiKeyUseCase() {
      return new ValidateApiKeyUseCase(singletonCImpl.exchangeRepositoryImplProvider.get());
    }

    private SaveExchangeConnectionUseCase saveExchangeConnectionUseCase() {
      return new SaveExchangeConnectionUseCase(singletonCImpl.exchangeRepositoryImplProvider.get());
    }

    private GetTopCoinsUseCase getTopCoinsUseCase() {
      return new GetTopCoinsUseCase(singletonCImpl.marketRepositoryImplProvider.get());
    }

    private GetCurrentUserUseCase getCurrentUserUseCase() {
      return new GetCurrentUserUseCase(singletonCImpl.authRepositoryImplProvider.get());
    }

    private LoginUseCase loginUseCase() {
      return new LoginUseCase(singletonCImpl.authRepositoryImplProvider.get());
    }

    private ResetPasswordUseCase resetPasswordUseCase() {
      return new ResetPasswordUseCase(singletonCImpl.authRepositoryImplProvider.get());
    }

    private ObserveExchangeConnectionsUseCase observeExchangeConnectionsUseCase() {
      return new ObserveExchangeConnectionsUseCase(singletonCImpl.exchangeRepositoryImplProvider.get());
    }

    private DeleteExchangeConnectionUseCase deleteExchangeConnectionUseCase() {
      return new DeleteExchangeConnectionUseCase(singletonCImpl.exchangeRepositoryImplProvider.get());
    }

    private SyncExchangeTradesUseCase syncExchangeTradesUseCase() {
      return new SyncExchangeTradesUseCase(singletonCImpl.exchangeRepositoryImplProvider.get());
    }

    private SetConnectionActiveUseCase setConnectionActiveUseCase() {
      return new SetConnectionActiveUseCase(singletonCImpl.exchangeRepositoryImplProvider.get());
    }

    private ObserveMultipleTickersUseCase observeMultipleTickersUseCase() {
      return new ObserveMultipleTickersUseCase(singletonCImpl.marketRepositoryImplProvider.get());
    }

    private ObservePortfolioSummaryUseCase observePortfolioSummaryUseCase() {
      return new ObservePortfolioSummaryUseCase(singletonCImpl.portfolioRepositoryImplProvider.get());
    }

    private ObserveTransactionsUseCase observeTransactionsUseCase() {
      return new ObserveTransactionsUseCase(singletonCImpl.portfolioRepositoryImplProvider.get());
    }

    private AddTransactionUseCase addTransactionUseCase() {
      return new AddTransactionUseCase(singletonCImpl.portfolioRepositoryImplProvider.get());
    }

    private DeleteTransactionUseCase deleteTransactionUseCase() {
      return new DeleteTransactionUseCase(singletonCImpl.portfolioRepositoryImplProvider.get());
    }

    private DeleteAssetUseCase deleteAssetUseCase() {
      return new DeleteAssetUseCase(singletonCImpl.portfolioRepositoryImplProvider.get());
    }

    private LogoutUseCase logoutUseCase() {
      return new LogoutUseCase(singletonCImpl.authRepositoryImplProvider.get());
    }

    private RegisterUseCase registerUseCase() {
      return new RegisterUseCase(singletonCImpl.authRepositoryImplProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.coinDetailViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.connectExchangeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.manageExchangeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.marketListViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.portfolioViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.registerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(10).put(CoinDetailViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) coinDetailViewModelProvider)).put(ConnectExchangeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) connectExchangeViewModelProvider)).put(HomeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) homeViewModelProvider)).put(LoginViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) loginViewModelProvider)).put(ManageExchangeViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) manageExchangeViewModelProvider)).put(MarketListViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) marketListViewModelProvider)).put(PortfolioViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) portfolioViewModelProvider)).put(ProfileViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) profileViewModelProvider)).put(RegisterViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) registerViewModelProvider)).put(SplashViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) splashViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.cryptolens.app.features.market.presentation.detail.CoinDetailViewModel 
          return (T) new CoinDetailViewModel(viewModelCImpl.getCoinDetailUseCase(), viewModelCImpl.getKlinesUseCase(), viewModelCImpl.observePriceTickerUseCase(), viewModelCImpl.savedStateHandle);

          case 1: // com.cryptolens.app.features.exchange.presentation.connect.ConnectExchangeViewModel 
          return (T) new ConnectExchangeViewModel(viewModelCImpl.validateApiKeyUseCase(), viewModelCImpl.saveExchangeConnectionUseCase());

          case 2: // com.cryptolens.app.features.home.presentation.HomeViewModel 
          return (T) new HomeViewModel(viewModelCImpl.getTopCoinsUseCase(), viewModelCImpl.getCurrentUserUseCase());

          case 3: // com.cryptolens.app.features.auth.presentation.login.LoginViewModel 
          return (T) new LoginViewModel(viewModelCImpl.loginUseCase(), viewModelCImpl.resetPasswordUseCase());

          case 4: // com.cryptolens.app.features.exchange.presentation.manage.ManageExchangeViewModel 
          return (T) new ManageExchangeViewModel(viewModelCImpl.observeExchangeConnectionsUseCase(), viewModelCImpl.deleteExchangeConnectionUseCase(), viewModelCImpl.syncExchangeTradesUseCase(), viewModelCImpl.setConnectionActiveUseCase());

          case 5: // com.cryptolens.app.features.market.presentation.marketlist.MarketListViewModel 
          return (T) new MarketListViewModel(viewModelCImpl.getTopCoinsUseCase(), viewModelCImpl.observeMultipleTickersUseCase());

          case 6: // com.cryptolens.app.features.portfolio.presentation.PortfolioViewModel 
          return (T) new PortfolioViewModel(viewModelCImpl.observePortfolioSummaryUseCase(), viewModelCImpl.observeTransactionsUseCase(), viewModelCImpl.addTransactionUseCase(), viewModelCImpl.deleteTransactionUseCase(), viewModelCImpl.deleteAssetUseCase(), viewModelCImpl.getCurrentUserUseCase());

          case 7: // com.cryptolens.app.features.profile.presentation.ProfileViewModel 
          return (T) new ProfileViewModel(viewModelCImpl.getCurrentUserUseCase(), viewModelCImpl.logoutUseCase());

          case 8: // com.cryptolens.app.features.auth.presentation.register.RegisterViewModel 
          return (T) new RegisterViewModel(viewModelCImpl.registerUseCase());

          case 9: // com.cryptolens.app.features.auth.presentation.splash.SplashViewModel 
          return (T) new SplashViewModel(viewModelCImpl.getCurrentUserUseCase());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends CryptoLensApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends CryptoLensApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends CryptoLensApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Json> provideJsonProvider;

    private Provider<Retrofit> provideCoinGeckoRetrofitProvider;

    private Provider<CoinGeckoApiService> provideCoinGeckoApiServiceProvider;

    private Provider<Retrofit> provideBinanceRetrofitProvider;

    private Provider<BinanceApiService> provideBinanceApiServiceProvider;

    private Provider<BinanceWebSocketManager> provideBinanceWsManagerProvider;

    private Provider<MarketRepositoryImpl> marketRepositoryImplProvider;

    private Provider<CryptoDatabase> provideDatabaseProvider;

    private Provider<ExchangeConnectionDao> provideExchangeConnectionDaoProvider;

    private Provider<CryptoManager> cryptoManagerProvider;

    private Provider<BinanceApiClient> provideBinanceApiClientProvider;

    private Provider<PortfolioAssetDao> providePortfolioAssetDaoProvider;

    private Provider<TransactionDao> provideTransactionDaoProvider;

    private Provider<ExchangeRepositoryImpl> exchangeRepositoryImplProvider;

    private Provider<SupabaseClient> provideSupabaseClientProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<PortfolioRepositoryImpl> portfolioRepositoryImplProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 3));
      this.provideJsonProvider = DoubleCheck.provider(new SwitchingProvider<Json>(singletonCImpl, 4));
      this.provideCoinGeckoRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 2));
      this.provideCoinGeckoApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<CoinGeckoApiService>(singletonCImpl, 1));
      this.provideBinanceRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 6));
      this.provideBinanceApiServiceProvider = DoubleCheck.provider(new SwitchingProvider<BinanceApiService>(singletonCImpl, 5));
      this.provideBinanceWsManagerProvider = DoubleCheck.provider(new SwitchingProvider<BinanceWebSocketManager>(singletonCImpl, 7));
      this.marketRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<MarketRepositoryImpl>(singletonCImpl, 0));
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<CryptoDatabase>(singletonCImpl, 10));
      this.provideExchangeConnectionDaoProvider = DoubleCheck.provider(new SwitchingProvider<ExchangeConnectionDao>(singletonCImpl, 9));
      this.cryptoManagerProvider = DoubleCheck.provider(new SwitchingProvider<CryptoManager>(singletonCImpl, 11));
      this.provideBinanceApiClientProvider = DoubleCheck.provider(new SwitchingProvider<BinanceApiClient>(singletonCImpl, 12));
      this.providePortfolioAssetDaoProvider = DoubleCheck.provider(new SwitchingProvider<PortfolioAssetDao>(singletonCImpl, 13));
      this.provideTransactionDaoProvider = DoubleCheck.provider(new SwitchingProvider<TransactionDao>(singletonCImpl, 14));
      this.exchangeRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ExchangeRepositoryImpl>(singletonCImpl, 8));
      this.provideSupabaseClientProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseClient>(singletonCImpl, 16));
      this.authRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepositoryImpl>(singletonCImpl, 15));
      this.portfolioRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<PortfolioRepositoryImpl>(singletonCImpl, 17));
    }

    @Override
    public void injectCryptoLensApp(CryptoLensApp cryptoLensApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.cryptolens.app.features.market.data.repository.MarketRepositoryImpl 
          return (T) new MarketRepositoryImpl(singletonCImpl.provideCoinGeckoApiServiceProvider.get(), singletonCImpl.provideBinanceApiServiceProvider.get(), singletonCImpl.provideBinanceWsManagerProvider.get());

          case 1: // com.cryptolens.app.features.market.data.remote.CoinGeckoApiService 
          return (T) NetworkModule_ProvideCoinGeckoApiServiceFactory.provideCoinGeckoApiService(singletonCImpl.provideCoinGeckoRetrofitProvider.get());

          case 2: // @javax.inject.Named("coingecko") retrofit2.Retrofit 
          return (T) NetworkModule_ProvideCoinGeckoRetrofitFactory.provideCoinGeckoRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 3: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 4: // kotlinx.serialization.json.Json 
          return (T) NetworkModule_ProvideJsonFactory.provideJson();

          case 5: // com.cryptolens.app.features.market.data.remote.BinanceApiService 
          return (T) NetworkModule_ProvideBinanceApiServiceFactory.provideBinanceApiService(singletonCImpl.provideBinanceRetrofitProvider.get());

          case 6: // @javax.inject.Named("binance") retrofit2.Retrofit 
          return (T) NetworkModule_ProvideBinanceRetrofitFactory.provideBinanceRetrofit(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 7: // com.cryptolens.app.features.market.data.remote.BinanceWebSocketManager 
          return (T) NetworkModule_ProvideBinanceWsManagerFactory.provideBinanceWsManager(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 8: // com.cryptolens.app.features.exchange.data.repository.ExchangeRepositoryImpl 
          return (T) new ExchangeRepositoryImpl(singletonCImpl.provideExchangeConnectionDaoProvider.get(), singletonCImpl.cryptoManagerProvider.get(), singletonCImpl.provideBinanceApiClientProvider.get(), singletonCImpl.providePortfolioAssetDaoProvider.get(), singletonCImpl.provideTransactionDaoProvider.get());

          case 9: // com.cryptolens.app.features.exchange.data.local.ExchangeConnectionDao 
          return (T) DatabaseModule_ProvideExchangeConnectionDaoFactory.provideExchangeConnectionDao(singletonCImpl.provideDatabaseProvider.get());

          case 10: // com.cryptolens.app.core.data.local.CryptoDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // com.cryptolens.app.features.exchange.data.local.CryptoManager 
          return (T) new CryptoManager();

          case 12: // com.cryptolens.app.features.exchange.data.remote.BinanceApiClient 
          return (T) NetworkModule_ProvideBinanceApiClientFactory.provideBinanceApiClient(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.provideJsonProvider.get());

          case 13: // com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao 
          return (T) DatabaseModule_ProvidePortfolioAssetDaoFactory.providePortfolioAssetDao(singletonCImpl.provideDatabaseProvider.get());

          case 14: // com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao 
          return (T) DatabaseModule_ProvideTransactionDaoFactory.provideTransactionDao(singletonCImpl.provideDatabaseProvider.get());

          case 15: // com.cryptolens.app.features.auth.data.repository.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.provideSupabaseClientProvider.get());

          case 16: // io.github.jan.supabase.SupabaseClient 
          return (T) SupabaseModule_ProvideSupabaseClientFactory.provideSupabaseClient();

          case 17: // com.cryptolens.app.features.portfolio.data.repository.PortfolioRepositoryImpl 
          return (T) new PortfolioRepositoryImpl(singletonCImpl.providePortfolioAssetDaoProvider.get(), singletonCImpl.provideTransactionDaoProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
