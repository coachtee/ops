package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.SupplierDao
import com.ops.app.data.local.entities.SupplierEntity
import com.ops.app.data.remote.dto.SupplierFieldsDto
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
class SupplierRepository @Inject constructor(
    private val supplierDao: SupplierDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<SupplierEntity>> = supplierDao.observeAll()
    fun observeById(id: String): Flow<SupplierEntity?> = supplierDao.observeById(id)
    suspend fun getById(id: String): SupplierEntity? = supplierDao.getById(id)

    /** Handles both create ([id] null) and edit ([id] set) — same pattern as
     * ExpenseRepository.save. */
    suspend fun save(id: String?, name: String, contactPerson: String, phone: String, email: String, notes: String): String {
        val existing = id?.let { supplierDao.getById(it) }
        val resolvedId = existing?.id ?: id ?: UUID.randomUUID().toString()
        supplierDao.upsert(
            SupplierEntity(
                id = resolvedId,
                name = name,
                contactPerson = contactPerson,
                phone = phone,
                email = email,
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
        val existing = supplierDao.getById(id) ?: return
        val now = nowIso()
        supplierDao.upsert(existing.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        enqueueSync()
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = supplierDao.getById(id) ?: return
        supplierDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = supplierDao.getById(id) ?: return
        supplierDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = supplierDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(SupplierFieldsDto.serializer(), serverJson)
        supplierDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
