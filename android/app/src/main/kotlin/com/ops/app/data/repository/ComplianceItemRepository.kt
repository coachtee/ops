package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.ComplianceItemDao
import com.ops.app.data.local.entities.ComplianceItemEntity
import com.ops.app.data.remote.dto.ComplianceItemFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.IsoTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComplianceItemRepository @Inject constructor(
    private val complianceItemDao: ComplianceItemDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<ComplianceItemEntity>> = complianceItemDao.observeAll()
    fun observeById(id: String): Flow<ComplianceItemEntity?> = complianceItemDao.observeById(id)
    suspend fun getById(id: String): ComplianceItemEntity? = complianceItemDao.getById(id)

    /** Handles both create ([id] null) and edit ([id] set) — same pattern as
     * SupplierRepository.save. */
    suspend fun save(
        id: String?,
        category: String,
        title: String,
        dueDate: String,
        completedDate: String?,
        isRecurring: Boolean,
        notes: String,
    ): String {
        val existing = id?.let { complianceItemDao.getById(it) }
        val resolvedId = existing?.id ?: id ?: UUID.randomUUID().toString()
        complianceItemDao.upsert(
            ComplianceItemEntity(
                id = resolvedId,
                category = category,
                title = title,
                dueDate = dueDate,
                completedDate = completedDate,
                isRecurring = isRecurring,
                notes = notes,
                updatedAt = nowIso(),
                deletedAt = null,
                syncState = SyncState.PENDING,
                syncError = null,
                conflictServerJson = existing?.conflictServerJson,
            ),
        )
        enqueueSync()
        return resolvedId
    }

    suspend fun delete(id: String) {
        val existing = complianceItemDao.getById(id) ?: return
        val now = nowIso()
        complianceItemDao.upsert(existing.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        enqueueSync()
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = complianceItemDao.getById(id) ?: return
        complianceItemDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = complianceItemDao.getById(id) ?: return
        complianceItemDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = complianceItemDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(ComplianceItemFieldsDto.serializer(), serverJson)
        complianceItemDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
