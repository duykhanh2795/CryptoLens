package com.cryptolens.app.features.market.data.remote

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.market.domain.model.Coin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory shared cache for market data.
 * All ViewModels read from here instead of calling CoinGecko independently.
 * This prevents 429 rate-limit errors when multiple screens are active.
 */
@Singleton
class MarketCache @Inject constructor() {

    private val _coins = MutableStateFlow<Resource<List<Coin>>>(Resource.Loading)
    val coins: StateFlow<Resource<List<Coin>>> = _coins.asStateFlow()

    private var lastFetchMs: Long = 0L
    private val cacheTtlMs: Long  = 60_000L  // 1 minute TTL

    fun isFresh(): Boolean =
        _coins.value is Resource.Success &&
        System.currentTimeMillis() - lastFetchMs < cacheTtlMs

    fun update(resource: Resource<List<Coin>>) {
        _coins.value = resource
        if (resource is Resource.Success) {
            lastFetchMs = System.currentTimeMillis()
        }
    }

    fun invalidate() {
        lastFetchMs = 0L
    }
}
