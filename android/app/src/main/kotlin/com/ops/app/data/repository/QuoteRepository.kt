package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.QuoteDao
import com.ops.app.data.local.dao.QuoteLineItemDao
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity
import com.ops.app.data.remote.dto.QuoteFieldsDto
import com.ops.app.data.remote.dto.QuoteLineItemFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.IsoTimestamp
import com.ops.coredomain.Money
import com.ops.coredomain.QuoteStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One line item as edited on screen, before it has a stable id (new) or with one (existing). */
data class QuoteLineItemInput(
    val id: String?,
    val description: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
)

@Singleton
class QuoteRepository @Inject constructor(
    private val quoteDao: QuoteDao,
    private val quoteLineItemDao: QuoteLineItemDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<QuoteEntity>> = quoteDao.observeAll()
    fun observeByCustomerId(customerId: String): Flow<List<QuoteEntity>> = quoteDao.observeByCustomerId(customerId)
    fun observeById(id: String): Flow<QuoteEntity?> = quoteDao.observeById(id)
    suspend fun getById(id: String): QuoteEntity? = quoteDao.getById(id)
    fun observeLineItems(quoteId: String): Flow<List<QuoteLineItemEntity>> = quoteLineItemDao.observeByQuoteId(quoteId)
    suspend fun getLineItems(quoteId: String): List<QuoteLineItemEntity> = quoteLineItemDao.getByQuoteId(quoteId)

    /**
     * Creates or fully re-saves a quote and its line items in one go — the
     * quote edit screen always submits the whole current line-item list.
     * Totals are computed here via core-domain's [Money] (same VAT math as
     * the backend, see `common/money.py`) for instant offline-correct
     * numbers; the server recomputes and echoes back the authoritative
     * figure on sync regardless (see API_CONTRACT.md).
     */
    suspend fun saveQuote(
        quoteId: String?,
        customerId: String,
        leadId: String?,
        issueDate: String,
        validUntil: String?,
        notes: String,
        terms: String,
        isVatApplicable: Boolean,
        discountAmount: BigDecimal,
        status: String,
        lineItems: List<QuoteLineItemInput>,
    ): String {
        val id = quoteId ?: UUID.randomUUID().toString()
        val now = nowIso()
        val existingQuote = quoteId?.let { quoteDao.getById(it) }

        val lineTotals = lineItems.map { Money.computeLineTotal(it.quantity, it.unitPrice) }
        val totals = Money.computeDocumentTotals(lineTotals, discountAmount, isVatApplicable)

        val existingItems = if (quoteId != null) quoteLineItemDao.getByQuoteId(quoteId) else emptyList()
        val keepIds = lineItems.mapNotNull { it.id }.toSet()
        existingItems.filter { it.id !in keepIds }.forEach { removed ->
            quoteLineItemDao.upsert(removed.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        }
        lineItems.forEachIndexed { index, input ->
            val itemId = input.id ?: UUID.randomUUID().toString()
            val lineTotal = Money.computeLineTotal(input.quantity, input.unitPrice)
            quoteLineItemDao.upsert(
                QuoteLineItemEntity(
                    id = itemId,
                    quoteId = id,
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

        quoteDao.upsert(
            QuoteEntity(
                id = id,
                customerId = customerId,
                leadId = leadId,
                number = existingQuote?.number,
                status = status,
                issueDate = issueDate,
                validUntil = validUntil,
                notes = notes,
                terms = terms,
                isVatApplicable = isVatApplicable,
                discountAmount = Money.quantize(discountAmount).toPlainString(),
                subtotal = totals.subtotal.toPlainString(),
                vatAmount = totals.vatAmount.toPlainString(),
                total = totals.total.toPlainString(),
                sentAt = existingQuote?.sentAt,
                acceptedAt = existingQuote?.acceptedAt,
                declinedAt = existingQuote?.declinedAt,
                updatedAt = now,
                syncState = SyncState.PENDING,
            ),
        )
        enqueueSync()
        return id
    }

    suspend fun markSent(id: String) {
        val existing = quoteDao.getById(id) ?: return
        quoteDao.upsert(existing.copy(status = QuoteStatus.SENT.wire, sentAt = nowIso(), updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    /** Only flips the quote's own status/timestamp — creating the follow-on
     * Job is orchestrated by the caller (see ui/quotes), which also has
     * JobRepository, to keep this repository focused on Quote itself. */
    suspend fun markAccepted(id: String) {
        val existing = quoteDao.getById(id) ?: return
        quoteDao.upsert(existing.copy(status = QuoteStatus.ACCEPTED.wire, acceptedAt = nowIso(), updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    suspend fun markDeclined(id: String) {
        val existing = quoteDao.getById(id) ?: return
        quoteDao.upsert(existing.copy(status = QuoteStatus.DECLINED.wire, declinedAt = nowIso(), updatedAt = nowIso(), syncState = SyncState.PENDING))
        enqueueSync()
    }

    // ---- sync status screen actions (quote) --------------------------------

    suspend fun retry(id: String) {
        val existing = quoteDao.getById(id) ?: return
        quoteDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = quoteDao.getById(id) ?: return
        quoteDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = quoteDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(QuoteFieldsDto.serializer(), serverJson)
        quoteDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    // ---- sync status screen actions (quote line item) ----------------------

    suspend fun retryLineItem(id: String) {
        val existing = quoteLineItemDao.getById(id) ?: return
        quoteLineItemDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMineLineItem(id: String) {
        val existing = quoteLineItemDao.getById(id) ?: return
        quoteLineItemDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirsLineItem(id: String) {
        val existing = quoteLineItemDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(QuoteLineItemFieldsDto.serializer(), serverJson)
        quoteLineItemDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
