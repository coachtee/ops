package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.JobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {

    @Query("SELECT * FROM jobs WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE customerId = :customerId AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByCustomerId(customerId: String): Flow<List<JobEntity>>

    @Query("SELECT * FROM jobs WHERE quoteId = :quoteId AND deletedAt IS NULL LIMIT 1")
    suspend fun getByQuoteId(quoteId: String): JobEntity?

    @Query("SELECT * FROM jobs WHERE id = :id")
    fun observeById(id: String): Flow<JobEntity?>

    @Query("SELECT * FROM jobs WHERE id = :id")
    suspend fun getById(id: String): JobEntity?

    @Query("SELECT * FROM jobs WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<JobEntity>

    @Query("SELECT * FROM jobs WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<JobEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: JobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<JobEntity>)
}
