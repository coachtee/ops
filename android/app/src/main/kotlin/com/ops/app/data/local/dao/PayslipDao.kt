package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.PayslipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PayslipDao {

    @Query("SELECT * FROM payslips WHERE deletedAt IS NULL ORDER BY periodEnd DESC")
    fun observeAll(): Flow<List<PayslipEntity>>

    @Query("SELECT * FROM payslips WHERE id = :id")
    fun observeById(id: String): Flow<PayslipEntity?>

    @Query("SELECT * FROM payslips WHERE id = :id")
    suspend fun getById(id: String): PayslipEntity?

    @Query("SELECT * FROM payslips WHERE employeeId = :employeeId AND deletedAt IS NULL ORDER BY periodEnd DESC")
    fun observeByEmployeeId(employeeId: String): Flow<List<PayslipEntity>>

    /** The outbox: rows waiting to be pushed, or that failed last time and need retrying. */
    @Query("SELECT * FROM payslips WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<PayslipEntity>

    /** Every row not yet clean-synced (PENDING/SYNCING/FAILED/CONFLICT) — feeds
     * the sync status screen and the top-bar sync chip. */
    @Query("SELECT * FROM payslips WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<PayslipEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PayslipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<PayslipEntity>)
}
