package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.LeadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeadDao {

    @Query("SELECT * FROM leads WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<LeadEntity>>

    @Query("SELECT * FROM leads WHERE id = :id")
    fun observeById(id: String): Flow<LeadEntity?>

    @Query("SELECT * FROM leads WHERE id = :id")
    suspend fun getById(id: String): LeadEntity?

    /** The outbox: rows waiting to be pushed, or that failed last time and need retrying. */
    @Query("SELECT * FROM leads WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<LeadEntity>

    /** Every row not yet clean-synced (PENDING/SYNCING/FAILED/CONFLICT) — feeds
     * the sync status screen and the top-bar sync chip. */
    @Query("SELECT * FROM leads WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<LeadEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LeadEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<LeadEntity>)
}
