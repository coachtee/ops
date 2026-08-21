package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.InvoiceDao
import com.ops.app.data.local.dao.InvoiceLineItemDao
import com.ops.app.data.local.dao.PaymentDao
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity
import com.ops.app.data.remote.dto.InvoiceFieldsDto
import com.ops.app.data.remote.dto.InvoiceLineItemFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.InvoiceStatus
import com.ops.coredomain.IsoTimestamp
import com.ops.coredomain.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One line item as edited on screen. */
data class InvoiceLineItemInput(
    val id: String?,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
)

@Singleton
class InvoiceRepository @Inject constructor(
    private val invoiceDao: InvoiceDao,
    private val invoiceLineItemDao: InvoiceLineItemDao,
    private val paymentDao: PaymentDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<InvoiceEntity>> = invoiceDao.observeAll()
    fun observeByCustomerId(customerId: String): Flow<List<InvoiceEntity>> = invoiceDao.observeByCustomerId(customerId)
    fun observeByJobId(jobId: String): Flow<List<InvoiceEntity>> = invoiceDao.observeByJobId(jobId)
    fun observeById(id: String): Flow<InvoiceEntity?> = invoiceDao.observeById(id)
    suspend fun getById(id: String): InvoiceEntity? = invoiceDao.getById(id)
    fun observeLineItems(invoiceId: String): Flow<List<InvoiceLineItemEntity>> = invoiceLineItemDao.observeByInvoiceId(invoiceId)
    suspend fun getLineItems(invoiceId: String): List<InvoiceLineItemEntity> = invoiceLineItemDao.getByInvoiceId(invoiceId)

    /** Prefills line items when creating an invoice from a job/quote — the
     * screen calls this to seed its editable line item list, it does not
     * write anything by itself. */
    fun toLineItemInputs(items: List<InvoiceLineItemEntity>): List<InvoiceLineItemInput> =
        items.map { InvoiceLineItemInput(id = null, description = it.description, quantity = BigDecimal(it.quantity), unitPrice = BigDecimal(it.unitPrice)) }

    fun toLineItemInputsFromQuote(items: List<QuoteLineItemEntity>): List<InvoiceLineItemInput> =
        items.map { InvoiceLineItemInput(id = null, description = it.description, quantity = BigDecimal(it.quantity), unitPrice = BigDecimal(it.unitPrice)) }

    suspend fun saveInvoice(
        invoiceId: String?,
        customerId: String,
        jobId: String?,
        quoteId: String?,
        issueDate: String,
        dueDate: String?,
        notes: String,
        terms: String,
        isVatApplicable: Boolean,
        discountAmount: BigDecimal,
        status: String,
        lineItems: List<InvoiceLineItemInput>,
    ): String {
        val id = invoiceId ?: UUID.randomUUID().toString()
        val now = nowIso()
        val existingInvoice = invoiceId?.let { invoiceDao.getById(it) }

        val lineTotals = lineItems.map { Money.computeLineTotal(it.quantity, it.unitPrice) }
        val totals = Money.computeDocumentTotals(lineTotals, discountAmount, isVatApplicable)

        val existingItems = if (invoiceId != null) invoiceLineItemDao.getByInvoiceId(invoiceId) else emptyList()
        val keepIds = lineItems.mapNotNull { it.id }.toSet()
        existingItems.filter { it.id !in keepIds }.forEach { removed ->
            invoiceLineItemDao.upsert(removed.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        }
        lineItems.forEachIndexed { index, input ->
            val itemId = input.id ?: UUID.randomUUID().toString()
            val lineTotal = Money.computeLineTotal(input.quantity, input.unitPrice)
            invoiceLineItemDao.upsert(
                InvoiceLineItemEntity(
                    id = itemId,
                    invoiceId = id,
                    description = input.description,
                    quantity = input.quantity.toPlainString(),
                    unitPrice = input.unitPrice.toPlainString(),
                    lineTotal = lineTotal.toPlainString(),
                    sortOrder = index,
                    updatedAt = now,
                    syncState = SyncState.PENDING,
                ),
            )
        }

        invoiceDao.upsert(
            InvoiceEntity(
                id = id,
                customerId = customerId,
                jobId = jobId,
                quoteId = quoteId,
                number = existingInvoice?.number,
                status = status,
                issueDate = issueDate,
                dueDate = dueDate,
                notes = notes,
                terms = terms,
                isVatApplicable = isVatApplicable,
                discountAmount = Money.quantize(discountAmount).toPlainString(),
                subtotal = totals.subtotal.toPlainString(),
                vatAmount = totals.vatAmount.toPlainString(),
                total = totals.total.toPlainString(),
                amountPaid = existingInvoice?.amountPaid ?: "0.00",
                sentAt = existingInvoice?.sentAt,
                updatedAt = now,
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        // Total may have changed (e.g. discount edited after a payment was
        // already recorded) — re-derive paid/partially-paid vs the new total.
        recomputePaymentState(id)
        return id
    }

    suspend fun markSent(id: String) {
        val existing = invoiceDao.getById(id) ?: return
        invoiceDao.upsert(existing.copy(status = InvoiceStatus.SENT.wire, sentAt = nowIso(), updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    /**
     * Mirrors backend/finance/services.py `recompute_invoice_payment_state`
     * exactly: `amount_paid` is always derived from this invoice's actual
     * (non-deleted) payments, never entered by hand, and PAID/PARTIALLY_PAID
     * are fully derived from amount_paid vs total in both directions —
     * including a correction pulling the invoice back out of "Paid" — so
     * "who owes me money" is never a stale/false positive. Cancelled
     * invoices are never touched. Called after every payment is recorded and
     * after any edit that could change the total.
     */
    suspend fun recomputePaymentState(invoiceId: String) {
        val invoice = invoiceDao.getById(invoiceId) ?: return
        val totalPaid = paymentDao.getByInvoiceId(invoiceId)
            .fold(BigDecimal.ZERO) { acc, payment -> acc.add(BigDecimal(payment.amount)) }
        val amountPaid = Money.quantize(totalPaid)
        val total = BigDecimal(invoice.total)

        val newStatus = if (invoice.status == InvoiceStatus.CANCELLED.wire) {
            invoice.status
        } else if (amountPaid > BigDecimal.ZERO && total > BigDecimal.ZERO && amountPaid >= total) {
            InvoiceStatus.PAID.wire
        } else if (amountPaid > BigDecimal.ZERO) {
            InvoiceStatus.PARTIALLY_PAID.wire
        } else if (invoice.status == InvoiceStatus.PAID.wire || invoice.status == InvoiceStatus.PARTIALLY_PAID.wire) {
            InvoiceStatus.SENT.wire
        } else {
            invoice.status
        }

        invoiceDao.upsert(
            invoice.copy(
                amountPaid = amountPaid.toPlainString(),
                status = newStatus,
                updatedAt = nowIso(),
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
    }

    // ---- sync status screen actions (invoice) ------------------------------

    suspend fun retry(id: String) {
        val existing = invoiceDao.getById(id) ?: return
        invoiceDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = invoiceDao.getById(id) ?: return
        invoiceDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = invoiceDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(InvoiceFieldsDto.serializer(), serverJson)
        invoiceDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    // ---- sync status screen actions (invoice line item) --------------------

    suspend fun retryLineItem(id: String) {
        val existing = invoiceLineItemDao.getById(id) ?: return
        invoiceLineItemDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMineLineItem(id: String) {
        val existing = invoiceLineItemDao.getById(id) ?: return
        invoiceLineItemDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirsLineItem(id: String) {
        val existing = invoiceLineItemDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(InvoiceLineItemFieldsDto.serializer(), serverJson)
        invoiceLineItemDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
