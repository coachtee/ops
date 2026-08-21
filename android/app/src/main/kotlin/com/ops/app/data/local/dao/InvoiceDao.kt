package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.InvoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoices WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE customerId = :customerId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByCustomerId(customerId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE jobId = :jobId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByJobId(jobId: String): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE id = :id")
    fun observeById(id: String): Flow<InvoiceEntity?>

    @Query("SELECT * FROM invoices WHERE id = :id")
    suspend fun getById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoices WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<InvoiceEntity>

    @Query("SELECT * FROM invoices WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<InvoiceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<InvoiceEntity>)
}
