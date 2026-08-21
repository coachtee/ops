package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.CustomerDao
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.remote.dto.CustomerFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.CustomerType
import com.ops.coredomain.IsoTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomerRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<CustomerEntity>> = customerDao.observeAll()
    fun search(query: String): Flow<List<CustomerEntity>> = customerDao.search(query)
    fun observeById(id: String): Flow<CustomerEntity?> = customerDao.observeById(id)
    suspend fun getById(id: String): CustomerEntity? = customerDao.getById(id)

    suspend fun create(
        name: String,
        customerType: String,
        phone: String,
        email: String,
        addressLine1: String,
        addressLine2: String,
        suburb: String,
        city: String,
        province: String,
        postalCode: String,
        notes: String,
    ): String {
        val id = UUID.randomUUID().toString()
        customerDao.upsert(
            CustomerEntity(
                id = id,
                name = name,
                customerType = customerType,
                phone = phone,
                email = email,
                addressLine1 = addressLine1,
                addressLine2 = addressLine2,
                suburb = suburb,
                city = city,
                province = province,
                postalCode = postalCode,
                notes = notes,
                sourceLeadId = null,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        return id
    }

    /** Used by [LeadRepository.convertToCustomer] — carries the lead's
     * contact details across, and links back via `sourceLeadId`. */
    suspend fun createFromLead(lead: LeadEntity): String {
        val id = UUID.randomUUID().toString()
        customerDao.upsert(
            CustomerEntity(
                id = id,
                name = lead.name,
                customerType = CustomerType.INDIVIDUAL.wire,
                phone = lead.phone,
                email = lead.email,
                addressLine1 = "",
                addressLine2 = "",
                suburb = "",
                city = "",
                province = "",
                postalCode = "",
                notes = if (lead.enquiry.isBlank()) "" else "From enquiry: ${lead.enquiry}",
                sourceLeadId = lead.id,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        return id
    }

    suspend fun save(customer: CustomerEntity) {
        customerDao.upsert(customer.copy(updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    suspend fun updateNotes(id: String, notes: String) {
        val existing = customerDao.getById(id) ?: return
        save(existing.copy(notes = notes))
    }

    // ---- sync status screen actions ----------------------------------------

    suspend fun retry(id: String) {
        val existing = customerDao.getById(id) ?: return
        customerDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = customerDao.getById(id) ?: return
        customerDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = customerDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(CustomerFieldsDto.serializer(), serverJson)
        customerDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
