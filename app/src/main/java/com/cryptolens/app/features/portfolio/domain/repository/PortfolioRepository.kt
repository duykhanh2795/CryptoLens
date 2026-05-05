package com.cryptolens.app.features.portfolio.domain.repository

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.portfolio.domain.model.PortfolioAsset
import com.cryptolens.app.features.portfolio.domain.model.PortfolioSummary
import com.cryptolens.app.features.portfolio.domain.model.Transaction
import com.cryptolens.app.features.portfolio.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun observeAssets(userId: String): Flow<List<PortfolioAsset>>
    fun observeTransactions(userId: String): Flow<List<Transaction>>
    fun observeTransactionsByCoin(userId: String, coinId: String): Flow<List<Transaction>>
    suspend fun addTransaction(
        userId: String,
        coinId: String,
        symbol: String,
        name: String,
        imageUrl: String,
        type: TransactionType,
        quantity: Double,
        price: Double,
        fee: Double,
        note: String,
    ): Resource<Unit>
    suspend fun deleteTransaction(transactionId: String, userId: String): Resource<Unit>
    suspend fun deleteAsset(assetId: String, userId: String): Resource<Unit>
    suspend fun clearAll(userId: String): Resource<Unit>
}
