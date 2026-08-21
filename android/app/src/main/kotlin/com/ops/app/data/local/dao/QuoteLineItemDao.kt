package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.QuoteLineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteLineItemDao {

    @Query("SELECT * FROM quote_line_items WHERE quoteId = :quoteId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeByQuoteId(quoteId: String): Flow<List<QuoteLineItemEntity>>

    @Query("SELECT * FROM quote_line_items WHERE quoteId = :quoteId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getByQuoteId(quoteId: String): List<QuoteLineItemEntity>

    @Query("SELECT * FROM quote_line_items WHERE id = :id")
    suspend fun getById(id: String): QuoteLineItemEntity?

    @Query("SELECT * FROM quote_line_items WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<QuoteLineItemEntity>

    @Query("SELECT * FROM quote_line_items WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<QuoteLineItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: QuoteLineItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<QuoteLineItemEntity>)

    @Query("DELETE FROM quote_line_items WHERE id = :id")
    suspend fun hardDeleteLocalOnly(id: String)
}
