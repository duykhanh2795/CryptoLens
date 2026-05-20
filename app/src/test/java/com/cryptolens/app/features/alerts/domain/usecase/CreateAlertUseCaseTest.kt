package com.cryptolens.app.features.alerts.domain.usecase

import com.cryptolens.app.core.domain.util.Resource
import com.cryptolens.app.features.alerts.domain.model.AlertCondition
import com.cryptolens.app.features.alerts.domain.model.AlertFrequency
import com.cryptolens.app.features.alerts.domain.model.AlertStatus
import com.cryptolens.app.features.alerts.domain.model.AlertType
import com.cryptolens.app.features.alerts.domain.model.AlertValueType
import com.cryptolens.app.features.alerts.domain.model.PriceAlert
import com.cryptolens.app.features.alerts.domain.repository.AlertRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateAlertUseCaseTest {
    private val repository = FakeAlertRepository()
    private val useCase = CreateAlertUseCase(repository)

    @Test
    fun `percent alert requires baseline value`() = runBlocking {
        val result = useCase(
            coinId = "ethereum",
            symbol = "ETH",
            name = "Ethereum",
            imageUrl = "",
            type = AlertType.VOLUME,
            valueType = AlertValueType.PERCENT,
            frequency = AlertFrequency.ONE_TIME,
            condition = AlertCondition.ABOVE,
            targetValueText = "10",
            baselineValue = 0.0,
            note = "",
        )

        assertTrue(result is Resource.Error)
        assertEquals(
            "Current volume is unavailable for percent alerts.",
            (result as Resource.Error).message,
        )
    }

    @Test
    fun `creates typed persistent number alert`() = runBlocking {
        val result = useCase(
            coinId = "bitcoin",
            symbol = "btc",
            name = "Bitcoin",
            imageUrl = "",
            type = AlertType.MARKET_CAP,
            valueType = AlertValueType.NUMBER,
            frequency = AlertFrequency.PERSISTENT,
            condition = AlertCondition.BELOW,
            targetValueText = "1000000000",
            baselineValue = 1500000000.0,
            note = "risk",
        )

        assertTrue(result is Resource.Success)
        val alert = (result as Resource.Success).data
        assertEquals(AlertType.MARKET_CAP, alert.type)
        assertEquals(AlertValueType.NUMBER, alert.valueType)
        assertEquals(AlertFrequency.PERSISTENT, alert.frequency)
        assertEquals("BTC", alert.symbol)
        assertEquals(1_000_000_000.0, alert.targetPrice, 0.0)
    }

    @Test
    fun `decrease percent alert rejects values above one hundred percent`() = runBlocking {
        val result = useCase(
            coinId = "ethereum",
            symbol = "ETH",
            name = "Ethereum",
            imageUrl = "",
            type = AlertType.PRICE_LIMIT,
            valueType = AlertValueType.PERCENT,
            frequency = AlertFrequency.ONE_TIME,
            condition = AlertCondition.BELOW,
            targetValueText = "120",
            baselineValue = 3000.0,
            note = "",
        )

        assertTrue(result is Resource.Error)
        assertEquals(
            "Decrease percent alerts cannot exceed 100%.",
            (result as Resource.Error).message,
        )
    }

    private class FakeAlertRepository : AlertRepository {
        override fun observeAlerts(): Flow<List<PriceAlert>> = emptyFlow()
        override fun observeActiveAlerts(): Flow<List<PriceAlert>> = emptyFlow()
        override suspend fun getActiveAlerts(): List<PriceAlert> = emptyList()

        override suspend fun createAlert(
            coinId: String,
            symbol: String,
            name: String,
            imageUrl: String,
            type: AlertType,
            valueType: AlertValueType,
            frequency: AlertFrequency,
            condition: AlertCondition,
            targetValue: Double,
            baselineValue: Double,
            note: String,
        ): Resource<PriceAlert> = Resource.Success(
            PriceAlert(
                id = "alert-1",
                coinId = coinId,
                symbol = symbol,
                name = name,
                imageUrl = imageUrl,
                type = type,
                valueType = valueType,
                frequency = frequency,
                condition = condition,
                targetPrice = targetValue,
                baselineValue = baselineValue,
                status = AlertStatus.ACTIVE,
                note = note,
                createdAt = 1L,
            ),
        )

        override suspend fun deleteAlert(id: String): Resource<Unit> = Resource.Success(Unit)
        override suspend fun setActive(id: String, active: Boolean): Resource<Unit> = Resource.Success(Unit)
        override suspend fun markTriggered(alert: PriceAlert, currentValue: Double): Resource<Unit> =
            Resource.Success(Unit)
    }
}
