package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.LeadDao
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.app.data.remote.dto.LeadFieldsDto
import com.ops.coredomain.IsoTimestamp
import com.ops.coredomain.LeadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every write here follows the same shape (see DISCOVERY.md section 6 /
 * the task brief): set a fresh UUID + `updatedAt = now` + `syncState =
 * PENDING`, upsert into Room (which every screen already observes via
 * [Flow], so the UI updates instantly), then enqueue a background sync —
 * never await it.
 */
@Singleton
class LeadRepository @Inject constructor(
    private val leadDao: LeadDao,
    private val customerRepository: CustomerRepository,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<LeadEntity>> = leadDao.observeAll()
    fun observeById(id: String): Flow<LeadEntity?> = leadDao.observeById(id)
    suspend fun getById(id: String): LeadEntity? = leadDao.getById(id)

    suspend fun create(
        name: String,
        phone: String,
        email: String,
        source: String,
        enquiry: String,
        followUpDate: String?,
    ): String {
        val id = UUID.randomUUID().toString()
        leadDao.upsert(
            LeadEntity(
                id = id,
                name = name,
                phone = phone,
                email = email,
                source = source,
                enquiry = enquiry,
                notes = "",
                status = LeadStatus.NEW.wire,
                followUpDate = followUpDate,
                convertedCustomerId = null,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        return id
    }

    /** Saves an edited lead — pass the full entity with the fields the user changed. */
    suspend fun save(lead: LeadEntity) {
        leadDao.upsert(lead.copy(updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    suspend fun updateNotes(id: String, notes: String) {
        val existing = leadDao.getById(id) ?: return
        save(existing.copy(notes = notes))
    }

    suspend fun updateFollowUpDate(id: String, followUpDate: String?) {
        val existing = leadDao.getById(id) ?: return
        save(existing.copy(followUpDate = followUpDate))
    }

    suspend fun updateStatus(id: String, status: String) {
        val existing = leadDao.getById(id) ?: return
        save(existing.copy(status = status))
    }

    /**
     * Creates a Customer from this lead's contact details and links the two
     * records both ways (`Customer.source_lead_id` / `Lead.converted_customer_id`).
     * Per API_CONTRACT.md's sync section, that mutual cross-reference — both
     * sides pointing at records that may be new in the SAME push batch — is
     * the one case the server's dependency ordering doesn't fully resolve in
     * a single cycle; it's expected to settle over two sync cycles (the
     * customer syncs first since the lead it references already existed
     * from an earlier cycle; the lead's reference to the brand-new customer
     * then succeeds on the very next retry, since FAILED rows are retried
     * automatically). Nothing extra is needed here for that — it is exactly
     * what this repository's normal PENDING/FAILED outbox handling already does.
     */
    suspend fun convertToCustomer(leadId: String): String? {
        val lead = leadDao.getById(leadId) ?: return null
        if (lead.convertedCustomerId != null) return lead.convertedCustomerId

        val customerId = customerRepository.createFromLead(lead)
        leadDao.upsert(
            lead.copy(
                status = LeadStatus.CONVERTED.wire,
                convertedCustomerId = customerId,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        return customerId
    }

    // ---- sync status screen actions ----------------------------------------

    suspend fun retry(id: String) {
        val existing = leadDao.getById(id) ?: return
        leadDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    /** "Keep mine": bump `updatedAt` to now and re-push — this local edit will
     * now be strictly newer than the server's conflicting row. */
    suspend fun keepMine(id: String) {
        val existing = leadDao.getById(id) ?: return
        leadDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    /** "Use theirs": overwrite the local row from the stored server_record, clearing the conflict. */
    suspend fun useTheirs(id: String) {
        val existing = leadDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(LeadFieldsDto.serializer(), serverJson)
        leadDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
