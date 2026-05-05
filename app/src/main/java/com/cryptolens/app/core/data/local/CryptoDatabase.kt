package com.cryptolens.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionDao
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionEntity
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao
import com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioAssetEntity
import com.cryptolens.app.features.portfolio.data.local.entity.TransactionEntity
import com.cryptolens.app.features.watchlist.data.local.dao.WatchlistDao
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistEntity

@Database(
    entities = [
        PortfolioAssetEntity::class,
        TransactionEntity::class,
        ExchangeConnectionEntity::class,
        WatchlistEntity::class,
    ],
    version      = 3,
    exportSchema = false,
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun portfolioAssetDao(): PortfolioAssetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun exchangeConnectionDao(): ExchangeConnectionDao
    abstract fun watchlistDao(): WatchlistDao

    companion object {
        const val DATABASE_NAME = "crypto_lens_db"
    }
}
