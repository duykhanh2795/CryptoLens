package com.cryptolens.app.features.portfolio.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.portfolio.domain.model.PortfolioAsset
import com.cryptolens.app.features.portfolio.domain.model.PortfolioSnapshot
import com.cryptolens.app.features.portfolio.domain.model.PortfolioSummary
import com.cryptolens.app.features.portfolio.domain.model.Transaction
import com.cryptolens.app.features.portfolio.domain.model.TransactionType
import com.cryptolens.app.features.portfolio.domain.repository.PortfolioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddTransactionUseCaseTest {
    private val repository = FakePortfolioRepository()
    private val useCase = AddTransactionUseCase(repository)

    @Test
    fun `rejects blank user`() = runBlocking {
        val result = useCase(
            userId = "",
            coinId = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            imageUrl = "",
            type = TransactionType.BUY,
            quantityStr = "1",
            priceStr = "100",
        )

        assertTrue(result is Resource.Error)
        assertEquals("Missing user", (result as Resource.Error).message)
    }

    @Test
    fun `rejects negative fee`() = runBlocking {
        val result = useCase(
            userId = "user-1",
            coinId = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            imageUrl = "",
            type = TransactionType.BUY,
            quantityStr = "1",
            priceStr = "100",
            fee = -1.0,
        )

        assertTrue(result is Resource.Error)
        assertEquals("Fee cannot be negative", (result as Resource.Error).message)
    }

    @Test
    fun `passes parsed values to repository`() = runBlocking {
        val result = useCase(
            userId = "user-1",
            coinId = "bitcoin",
            symbol = "BTC",
            name = "Bitcoin",
            imageUrl = "",
            type = TransactionType.BUY,
            quantityStr = "2.5",
            priceStr = "100.25",
            fee = 1.5,
            note = "entry",
        )

        assertTrue(result is Resource.Success)
        assertEquals(2.5, repository.lastQuantity, 0.0)
        assertEquals(100.25, repository.lastPrice, 0.0)
        assertEquals(1.5, repository.lastFee, 0.0)
    }

    private class FakePortfolioRepository : PortfolioRepository {
        var lastQuantity = 0.0
        var lastPrice = 0.0
        var lastFee = 0.0

        override fun observeAssets(userId: String): Flow<List<PortfolioAsset>> = emptyFlow()
        override fun observeTransactions(userId: String): Flow<List<Transaction>> = emptyFlow()
        override fun observeTransactionsByCoin(userId: String, coinId: String): Flow<List<Transaction>> = emptyFlow()
        override fun observeSnapshots(userId: String, limit: Int): Flow<List<PortfolioSnapshot>> = emptyFlow()

        override suspend fun addTransaction(
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
        ): Resource<Unit> {
            lastQuantity = quantity
            lastPrice = price
            lastFee = fee
            return Resource.Success(Unit)
        }

        override suspend fun deleteTransaction(transactionId: String, userId: String): Resource<Unit> =
            Resource.Success(Unit)

        override suspend fun deleteAsset(assetId: String, userId: String): Resource<Unit> =
            Resource.Success(Unit)

        override suspend fun saveSnapshot(
            userId: String,
            summary: PortfolioSummary,
            timestamp: Long,
        ): Resource<Unit> = Resource.Success(Unit)

        override suspend fun clearAll(userId: String): Resource<Unit> = Resource.Success(Unit)
    }
}
