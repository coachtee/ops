package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.entities.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE deletedAt IS NULL ORDER BY date DESC, updatedAt DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    fun observeById(id: String): Flow<ExpenseEntity?>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE jobId = :jobId AND deletedAt IS NULL ORDER BY date DESC")
    fun observeByJobId(jobId: String): Flow<List<ExpenseEntity>>

    /** The outbox: rows waiting to be pushed, or that failed last time and need retrying. */
    @Query("SELECT * FROM expenses WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<ExpenseEntity>

    /** Every row not yet clean-synced (PENDING/SYNCING/FAILED/CONFLICT) — feeds
     * the sync status screen and the top-bar sync chip. */
    @Query("SELECT * FROM expenses WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<ExpenseEntity>>

    /** Rows with a local photo waiting to be uploaded, whose own record has
     * already been confirmed by the server — see SyncManager.syncReceipts
     * and API_CONTRACT.md's "Expense receipt attachments" (the upload 404s
     * for an expense the server doesn't have yet, so this only considers
     * SYNCED parents). */
    @Query(
        "SELECT * FROM expenses WHERE localReceiptPath IS NOT NULL " +
            "AND receiptSyncState = '${ReceiptSyncState.PENDING}' AND syncState = 'SYNCED'",
    )
    suspend fun getReceiptOutbox(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ExpenseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ExpenseEntity>)
}
