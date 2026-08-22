package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.entities.VisitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitDao {

    @Query("SELECT * FROM visits WHERE deletedAt IS NULL ORDER BY scheduledDate ASC, startTime ASC")
    fun observeAll(): Flow<List<VisitEntity>>

    @Query("SELECT * FROM visits WHERE id = :id")
    fun observeById(id: String): Flow<VisitEntity?>

    @Query("SELECT * FROM visits WHERE id = :id")
    suspend fun getById(id: String): VisitEntity?

    @Query("SELECT * FROM visits WHERE jobId = :jobId AND deletedAt IS NULL ORDER BY scheduledDate ASC, startTime ASC")
    fun observeByJobId(jobId: String): Flow<List<VisitEntity>>

    /** The outbox: rows waiting to be pushed, or that failed last time and need retrying. */
    @Query("SELECT * FROM visits WHERE syncState IN ('PENDING', 'FAILED')")
    suspend fun getOutbox(): List<VisitEntity>

    /** Every row not yet clean-synced (PENDING/SYNCING/FAILED/CONFLICT) — feeds
     * the sync status screen and the top-bar sync chip. */
    @Query("SELECT * FROM visits WHERE syncState != 'SYNCED'")
    fun observeUnsynced(): Flow<List<VisitEntity>>

    /** Rows with a local photo waiting to be uploaded, whose own record has
     * already been confirmed by the server — see SyncManager.syncVisitPhotos
     * and API_CONTRACT.md's "Visit photo attachment" (the upload 404s for a
     * visit the server doesn't have yet, so this only considers SYNCED parents). */
    @Query(
        "SELECT * FROM visits WHERE localPhotoPath IS NOT NULL " +
            "AND photoSyncState = '${ReceiptSyncState.PENDING}' AND syncState = 'SYNCED'",
    )
    suspend fun getPhotoOutbox(): List<VisitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VisitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<VisitEntity>)
}
