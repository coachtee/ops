package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.PaymentDao
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.remote.dto.PaymentFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.IsoTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val invoiceRepository: InvoiceRepository,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<PaymentEntity>> = paymentDao.observeAll()
    fun observeByCustomerId(customerId: String): Flow<List<PaymentEntity>> = paymentDao.observeByCustomerId(customerId)
    fun observeByInvoiceId(invoiceId: String): Flow<List<PaymentEntity>> = paymentDao.observeByInvoiceId(invoiceId)

    /** `invoiceId == null` records a payment on account, against the
     * customer directly, per API_CONTRACT.md's `payment` model. */
    suspend fun record(
        customerId: String,
        invoiceId: String?,
        amount: BigDecimal,
        method: String,
        reference: String,
        paidDate: String,
        notes: String,
    ): String {
        val id = UUID.randomUUID().toString()
        paymentDao.upsert(
            PaymentEntity(
                id = id,
                customerId = customerId,
                invoiceId = invoiceId,
                amount = amount.toPlainString(),
                method = method,
                reference = reference,
                paidDate = paidDate,
                notes = notes,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        // Instant offline-correct "who owes me money" — see InvoiceRepository.recomputePaymentState.
        if (invoiceId != null) {
            invoiceRepository.recomputePaymentState(invoiceId)
        }
        return id
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = paymentDao.getById(id) ?: return
        paymentDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = paymentDao.getById(id) ?: return
        paymentDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = paymentDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(PaymentFieldsDto.serializer(), serverJson)
        paymentDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
        existing.invoiceId?.let { invoiceRepository.recomputePaymentState(it) }
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
