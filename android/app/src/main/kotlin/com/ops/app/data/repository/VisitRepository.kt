package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.VisitDao
import com.ops.app.data.local.entities.VisitEntity
import com.ops.app.data.remote.dto.VisitFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.IsoTimestamp
import com.ops.coredomain.VisitStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VisitRepository @Inject constructor(
    private val visitDao: VisitDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<VisitEntity>> = visitDao.observeAll()
    fun observeByJobId(jobId: String): Flow<List<VisitEntity>> = visitDao.observeByJobId(jobId)
    fun observeById(id: String): Flow<VisitEntity?> = visitDao.observeById(id)
    suspend fun getById(id: String): VisitEntity? = visitDao.getById(id)

    suspend fun schedule(jobId: String, employeeId: String?, scheduledDate: String, startTime: String?): String {
        val id = UUID.randomUUID().toString()
        visitDao.upsert(
            VisitEntity(
                id = id,
                jobId = jobId,
                employeeId = employeeId,
                scheduledDate = scheduledDate,
                startTime = startTime,
                endTime = null,
                status = VisitStatus.SCHEDULED.wire,
                notes = "",
                startedAt = null,
                completedAt = null,
                photoUrl = null,
                localPhotoPath = null,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        return id
    }

    suspend fun save(visit: VisitEntity) {
        visitDao.upsert(visit.copy(updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    /** [VisitStatus.EN_ROUTE] and [VisitStatus.IN_PROGRESS] are plain status
     * moves; starting work specifically also stamps [VisitEntity.startedAt]
     * the first time, same "set once, never move" rule [complete] uses for
     * `completedAt`. */
    suspend fun start(id: String) {
        val existing = visitDao.getById(id) ?: return
        save(
            existing.copy(
                status = VisitStatus.IN_PROGRESS.wire,
                startedAt = existing.startedAt ?: nowIso(),
            ),
        )
    }

    suspend fun updateStatus(id: String, status: String) {
        val existing = visitDao.getById(id) ?: return
        save(existing.copy(status = status))
    }

    suspend fun updateNotes(id: String, notes: String) {
        val existing = visitDao.getById(id) ?: return
        save(existing.copy(notes = notes))
    }

    suspend fun complete(id: String) {
        val existing = visitDao.getById(id) ?: return
        save(
            existing.copy(
                status = VisitStatus.COMPLETED.wire,
                completedAt = existing.completedAt ?: nowIso(),
            ),
        )
    }

    /** Attaches a just-captured/picked photo — mirrors
     * ExpenseRepository.attachReceipt. The actual upload happens in
     * SyncManager.syncVisitPhotos once this visit's own JSON record is
     * SYNCED (see the entity's doc comment). */
    suspend fun attachPhoto(id: String, localPath: String) {
        val existing = visitDao.getById(id) ?: return
        visitDao.upsert(existing.copy(localPhotoPath = localPath, photoSyncState = ReceiptSyncState.PENDING, photoSyncError = null))
        enqueueSync()
    }

    suspend fun retryPhoto(id: String) {
        val existing = visitDao.getById(id) ?: return
        if (existing.localPhotoPath == null) return
        visitDao.upsert(existing.copy(photoSyncState = ReceiptSyncState.PENDING, photoSyncError = null))
        enqueueSync()
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = visitDao.getById(id) ?: return
        visitDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = visitDao.getById(id) ?: return
        visitDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = visitDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(VisitFieldsDto.serializer(), serverJson)
        visitDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED, existing = existing))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
