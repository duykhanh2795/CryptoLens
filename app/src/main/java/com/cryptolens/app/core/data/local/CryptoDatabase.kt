package com.cryptolens.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cryptolens.app.features.alerts.data.local.dao.PriceAlertDao
import com.cryptolens.app.features.alerts.data.local.entity.PriceAlertEntity
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionDao
import com.cryptolens.app.features.exchange.data.local.ExchangeConnectionEntity
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioAssetDao
import com.cryptolens.app.features.portfolio.data.local.dao.PortfolioSnapshotDao
import com.cryptolens.app.features.portfolio.data.local.dao.TransactionDao
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioAssetEntity
import com.cryptolens.app.features.portfolio.data.local.entity.PortfolioSnapshotEntity
import com.cryptolens.app.features.portfolio.data.local.entity.TransactionEntity
import com.cryptolens.app.features.wallet.data.local.dao.WalletWatchDao
import com.cryptolens.app.features.wallet.data.local.entity.WalletWatchEntity
import com.cryptolens.app.features.watchlist.data.local.dao.WatchlistDao
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistComparePresetEntity
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistEntity
import com.cryptolens.app.features.watchlist.data.local.entity.WatchlistGroupEntity

@Database(
    entities = [
        PortfolioAssetEntity::class,
        TransactionEntity::class,
        ExchangeConnectionEntity::class,
        WatchlistEntity::class,
        PriceAlertEntity::class,
        PortfolioSnapshotEntity::class,
        WalletWatchEntity::class,
        WatchlistGroupEntity::class,
        WatchlistComparePresetEntity::class,
    ],
    version      = 10,
    exportSchema = true,
)
abstract class CryptoDatabase : RoomDatabase() {
    abstract fun portfolioAssetDao(): PortfolioAssetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun exchangeConnectionDao(): ExchangeConnectionDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun priceAlertDao(): PriceAlertDao
    abstract fun portfolioSnapshotDao(): PortfolioSnapshotDao
    abstract fun walletWatchDao(): WalletWatchDao

    companion object {
        const val DATABASE_NAME = "crypto_lens_db"
    }
}

object CryptoDatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
            addColumnIfMissing(db, "transactions", "sourceConnectionId", "TEXT")
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
            createPriceAlertsTableIfMissing(db)
        }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
            createPortfolioSnapshotsTableIfMissing(db)
        }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
            createWalletWatchlistTableIfMissing(db)
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
            addAdvancedAlertColumnsIfMissing(db)
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
            createWatchlistGroupsTableIfMissing(db)
            addWatchlistGroupColumnsIfMissing(db)
        }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            createCurrentTablesIfMissing(db)
            createWatchlistComparePresetsTableIfMissing(db)
        }
    }

    val ALL = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
    )

    private fun createCurrentTablesIfMissing(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `portfolio_assets` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `coinId` TEXT NOT NULL,
                `symbol` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `averageBuyPrice` REAL NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `coinId` TEXT NOT NULL,
                `symbol` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `quantity` REAL NOT NULL,
                `price` REAL NOT NULL,
                `fee` REAL NOT NULL,
                `note` TEXT NOT NULL,
                `sourceConnectionId` TEXT,
                `timestamp` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exchange_connections` (
                `id` TEXT NOT NULL,
                `exchangeType` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `apiKey` TEXT NOT NULL,
                `apiSecretEncrypted` TEXT NOT NULL,
                `isActive` INTEGER NOT NULL,
                `lastSyncAt` INTEGER,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watchlist` (
                `coinId` TEXT NOT NULL,
                `symbol` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `addedAt` INTEGER NOT NULL,
                `groupIds` TEXT NOT NULL DEFAULT 'spot',
                `isPinned` INTEGER NOT NULL DEFAULT 0,
                `sortIndex` INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(`coinId`)
            )
            """.trimIndent()
        )
        addWatchlistGroupColumnsIfMissing(db)
        createWatchlistGroupsTableIfMissing(db)
        createWatchlistComparePresetsTableIfMissing(db)
        createPriceAlertsTableIfMissing(db)
        createPortfolioSnapshotsTableIfMissing(db)
        createWalletWatchlistTableIfMissing(db)
    }

    private fun createWatchlistGroupsTableIfMissing(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watchlist_groups` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `sortIndex` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }

    private fun createWatchlistComparePresetsTableIfMissing(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watchlist_compare_presets` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `coinIds` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
    }

    private fun addWatchlistGroupColumnsIfMissing(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "watchlist", "groupIds", "TEXT NOT NULL DEFAULT 'spot'")
        addColumnIfMissing(db, "watchlist", "isPinned", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(db, "watchlist", "sortIndex", "INTEGER NOT NULL DEFAULT 0")
    }

    private fun createPriceAlertsTableIfMissing(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `price_alerts` (
                `id` TEXT NOT NULL,
                `coinId` TEXT NOT NULL,
                `symbol` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `imageUrl` TEXT NOT NULL,
                `alertType` TEXT NOT NULL DEFAULT 'PRICE_LIMIT',
                `valueType` TEXT NOT NULL DEFAULT 'NUMBER',
                `frequency` TEXT NOT NULL DEFAULT 'ONE_TIME',
                `condition` TEXT NOT NULL,
                `targetPrice` REAL NOT NULL,
                `baselineValue` REAL NOT NULL DEFAULT 0.0,
                `status` TEXT NOT NULL,
                `note` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `triggeredAt` INTEGER,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        addAdvancedAlertColumnsIfMissing(db)
    }

    private fun addAdvancedAlertColumnsIfMissing(db: SupportSQLiteDatabase) {
        addColumnIfMissing(db, "price_alerts", "alertType", "TEXT NOT NULL DEFAULT 'PRICE_LIMIT'")
        addColumnIfMissing(db, "price_alerts", "valueType", "TEXT NOT NULL DEFAULT 'NUMBER'")
        addColumnIfMissing(db, "price_alerts", "frequency", "TEXT NOT NULL DEFAULT 'ONE_TIME'")
        addColumnIfMissing(db, "price_alerts", "baselineValue", "REAL NOT NULL DEFAULT 0.0")
    }

    private fun createPortfolioSnapshotsTableIfMissing(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `portfolio_snapshots` (
                `userId` TEXT NOT NULL,
                `dayStart` INTEGER NOT NULL,
                `totalValue` REAL NOT NULL,
                `totalInvested` REAL NOT NULL,
                `totalProfitLoss` REAL NOT NULL,
                `totalProfitLossPercent` REAL NOT NULL,
                `assetCount` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`userId`, `dayStart`)
            )
            """.trimIndent()
        )
    }

    private fun createWalletWatchlistTableIfMissing(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wallet_watchlist` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `label` TEXT NOT NULL,
                `address` TEXT NOT NULL,
                `chain` TEXT NOT NULL,
                `nativeBalance` REAL NOT NULL,
                `txCount` INTEGER NOT NULL,
                `lastSyncedAt` INTEGER,
                `syncError` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS `index_wallet_watchlist_userId_chain_address`
            ON `wallet_watchlist` (`userId`, `chain`, `address`)
            """.trimIndent()
        )
    }

    private fun addColumnIfMissing(
        db: SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
        columnDefinition: String,
    ) {
        db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return
            }
        }
        db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
    }
}
