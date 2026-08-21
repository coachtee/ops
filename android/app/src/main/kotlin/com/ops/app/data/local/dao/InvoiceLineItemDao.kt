package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceLineItemDao {

    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeByInvoiceId(invoiceId: String): Flow<List<InvoiceLineItemEntity>>

    @Query("SELECT * FROM invoice_line_items WHERE invoiceId = :invoiceId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    suspend fun getByInvoiceId(invoiceId: String): List<InvoiceLineItemEntity>

    @Query("SELECT * FROM invoice_line_items WHERE id = :id")
    suspend fun getById(id: String): InvoiceLineItemEntity?

    @Query("SELECT * FROM invoice_line_items WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<InvoiceLineItemEntity>

    @Query("SELECT * FROM invoice_line_items WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<InvoiceLineItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InvoiceLineItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<InvoiceLineItemEntity>)

    @Query("DELETE FROM invoice_line_items WHERE id = :id")
    suspend fun hardDeleteLocalOnly(id: String)
}
