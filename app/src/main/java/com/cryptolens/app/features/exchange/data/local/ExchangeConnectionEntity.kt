package com.cryptolens.app.features.exchange.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exchange_connections")
data class ExchangeConnectionEntity(
    @PrimaryKey val id: String,
    val exchangeType: String,       // ExchangeType.name
    val label: String,
    val apiKey: String,             // AES-256 encrypted, base64 encoded
    val apiSecretEncrypted: String, // AES-256 encrypted, base64 encoded
    val isActive: Boolean,
    val lastSyncAt: Long?,
    val createdAt: Long,
)
