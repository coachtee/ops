package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.EmployeeDao
import com.ops.app.data.local.entities.EmployeeEntity
import com.ops.app.data.remote.dto.EmployeeFieldsDto
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
class EmployeeRepository @Inject constructor(
    private val employeeDao: EmployeeDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<EmployeeEntity>> = employeeDao.observeAll()
    fun observeById(id: String): Flow<EmployeeEntity?> = employeeDao.observeById(id)
    suspend fun getById(id: String): EmployeeEntity? = employeeDao.getById(id)

    /** Handles both create ([id] null) and edit ([id] set) — same pattern as
     * SupplierRepository.save. */
    suspend fun save(
        id: String?,
        name: String,
        role: String,
        phone: String,
        email: String,
        payRateType: String,
        payRate: String,
        startDate: String?,
        notes: String,
    ): String {
        val existing = id?.let { employeeDao.getById(it) }
        val resolvedId = existing?.id ?: id ?: UUID.randomUUID().toString()
        employeeDao.upsert(
            EmployeeEntity(
                id = resolvedId,
                name = name,
                role = role,
                phone = phone,
                email = email,
                payRateType = payRateType,
                payRate = payRate,
                startDate = startDate,
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
        val existing = employeeDao.getById(id) ?: return
        val now = nowIso()
        employeeDao.upsert(existing.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        enqueueSync()
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = employeeDao.getById(id) ?: return
        employeeDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = employeeDao.getById(id) ?: return
        employeeDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = employeeDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(EmployeeFieldsDto.serializer(), serverJson)
        employeeDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
