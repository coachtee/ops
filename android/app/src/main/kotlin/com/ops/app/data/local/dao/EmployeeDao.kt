package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Query("SELECT * FROM employees WHERE deletedAt IS NULL ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id")
    fun observeById(id: String): Flow<EmployeeEntity?>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: String): EmployeeEntity?

    /** The outbox: rows waiting to be pushed, or that failed last time and need retrying. */
    @Query("SELECT * FROM employees WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<EmployeeEntity>

    /** Every row not yet clean-synced (PENDING/SYNCING/FAILED/CONFLICT) — feeds
     * the sync status screen and the top-bar sync chip. */
    @Query("SELECT * FROM employees WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<EmployeeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<EmployeeEntity>)
}
