package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.ComplianceItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ComplianceItemDao {

    @Query("SELECT * FROM compliance_items WHERE deletedAt IS NULL ORDER BY dueDate")
    fun observeAll(): Flow<List<ComplianceItemEntity>>

    @Query("SELECT * FROM compliance_items WHERE id = :id")
    fun observeById(id: String): Flow<ComplianceItemEntity?>

    @Query("SELECT * FROM compliance_items WHERE id = :id")
    suspend fun getById(id: String): ComplianceItemEntity?

    /** The outbox: rows waiting to be pushed, or that failed last time and need retrying. */
    @Query("SELECT * FROM compliance_items WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<ComplianceItemEntity>

    /** Every row not yet clean-synced (PENDING/SYNCING/FAILED/CONFLICT) — feeds
     * the sync status screen and the top-bar sync chip. */
    @Query("SELECT * FROM compliance_items WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<ComplianceItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ComplianceItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ComplianceItemEntity>)
}
