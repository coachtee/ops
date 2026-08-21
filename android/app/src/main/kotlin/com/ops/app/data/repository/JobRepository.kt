package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.JobDao
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.remote.dto.JobFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.IsoTimestamp
import com.ops.coredomain.JobStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobDao: JobDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<JobEntity>> = jobDao.observeAll()
    fun observeByCustomerId(customerId: String): Flow<List<JobEntity>> = jobDao.observeByCustomerId(customerId)
    fun observeById(id: String): Flow<JobEntity?> = jobDao.observeById(id)
    suspend fun getById(id: String): JobEntity? = jobDao.getById(id)
    suspend fun getByQuoteId(quoteId: String): JobEntity? = jobDao.getByQuoteId(quoteId)

    /** "(accepted) -> Job (auto-created)" per DISCOVERY.md's IA — called right
     * after [QuoteRepository.markAccepted] by the quote screen, which already
     * has the customer's name on hand for a sensible default title. */
    suspend fun createFromQuote(quote: QuoteEntity, customerName: String): String {
        val id = UUID.randomUUID().toString()
        jobDao.upsert(
            JobEntity(
                id = id,
                customerId = quote.customerId,
                quoteId = quote.id,
                number = null,
                title = "Work for $customerName",
                description = quote.notes,
                status = JobStatus.NOT_STARTED.wire,
                startDate = null,
                dueDate = quote.validUntil,
                completedDate = null,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        return id
    }

    suspend fun save(job: JobEntity) {
        jobDao.upsert(job.copy(updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    suspend fun updateStatus(id: String, status: String) {
        val existing = jobDao.getById(id) ?: return
        val completedDate = if (status == JobStatus.COMPLETED.wire) {
            existing.completedDate ?: LocalDate.now().toString()
        } else {
            existing.completedDate
        }
        save(existing.copy(status = status, completedDate = completedDate))
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = jobDao.getById(id) ?: return
        jobDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = jobDao.getById(id) ?: return
        jobDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = jobDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(JobFieldsDto.serializer(), serverJson)
        jobDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
