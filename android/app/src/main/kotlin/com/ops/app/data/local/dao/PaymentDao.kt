package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.PaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE deletedAt IS NULL ORDER BY paidDate DESC")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE customerId = :customerId AND deletedAt IS NULL ORDER BY paidDate DESC")
    fun observeByCustomerId(customerId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId AND deletedAt IS NULL ORDER BY paidDate DESC")
    fun observeByInvoiceId(invoiceId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE invoiceId = :invoiceId AND deletedAt IS NULL ORDER BY paidDate DESC")
    suspend fun getByInvoiceId(invoiceId: String): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getById(id: String): PaymentEntity?

    @Query("SELECT * FROM payments WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<PaymentEntity>

    @Query("SELECT * FROM payments WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PaymentEntity>)
}
