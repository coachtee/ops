package com.ops.app.data.sync

import com.ops.app.data.datastore.AuthPreferences
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.SyncableRecord
import com.ops.app.data.local.dao.CustomerDao
import com.ops.app.data.local.dao.ExpenseDao
import com.ops.app.data.local.dao.InvoiceDao
import com.ops.app.data.local.dao.InvoiceLineItemDao
import com.ops.app.data.local.dao.JobDao
import com.ops.app.data.local.dao.LeadDao
import com.ops.app.data.local.dao.PaymentDao
import com.ops.app.data.local.dao.QuoteDao
import com.ops.app.data.local.dao.QuoteLineItemDao
import com.ops.app.data.remote.OpsApiService
import com.ops.app.data.remote.dto.CustomerFieldsDto
import com.ops.app.data.remote.dto.ExpenseFieldsDto
import com.ops.app.data.remote.dto.InvoiceFieldsDto
import com.ops.app.data.remote.dto.InvoiceLineItemFieldsDto
import com.ops.app.data.remote.dto.JobFieldsDto
import com.ops.app.data.remote.dto.LeadFieldsDto
import com.ops.app.data.remote.dto.PaymentFieldsDto
import com.ops.app.data.remote.dto.QuoteFieldsDto
import com.ops.app.data.remote.dto.QuoteLineItemFieldsDto
import com.ops.app.data.remote.dto.SyncChangeDto
import com.ops.app.data.remote.dto.SyncPushRequestDto
import com.ops.app.data.remote.dto.SyncResultDto
import com.ops.coredomain.IsoTimestamp
import com.ops.coredomain.SyncDecision
import com.ops.coredomain.decideSyncOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SyncOutcome {
    data object NotSignedIn : SyncOutcome
    data object Success : SyncOutcome
    data class Failed(val message: String) : SyncOutcome
}

/**
 * The offline-first sync engine's client half — see DISCOVERY.md section 6
 * and API_CONTRACT.md's "Sync" section for the protocol this implements.
 *
 * [syncNow] does, in order (never on the UI thread's critical path — always
 * called from a coroutine/WorkManager worker the UI merely observes via
 * [observeChipState] / each screen's Room [Flow]s):
 *
 *  1. Gather every PENDING/FAILED row across all 9 syncable DAOs into ONE
 *     `POST /api/sync/push/` batch (any order — the server applies a fixed
 *     dependency order within the batch, see API_CONTRACT.md).
 *  2. Mark them SYNCING first. Per result: `accepted` -> overwrite local
 *     fields from `server_record` (this is where server-assigned numbers and
 *     recomputed totals come back) and mark SYNCED; `conflict` -> mark
 *     CONFLICT, store `conflictServerJson`, and do NOT touch the user's
 *     local edit; `error` -> mark FAILED with `syncError`, local data stays
 *     intact.
 *  3. `GET /api/sync/pull/?since=<cursor>` (cursor omitted on first-ever
 *     sync). Per pulled change: if no local row exists, or the local row is
 *     SYNCED, upsert it. If the local row is PENDING/SYNCING/FAILED/CONFLICT,
 *     the pull is skipped for that id entirely — never overwritten — which
 *     is what naturally "queues it behind the next push/conflict check": the
 *     next time that still-pending row is pushed, its now-stale `updated_at`
 *     will lose to the server's newer one and come back as an explicit
 *     `conflict` instead, so nothing is silently lost either way.
 *  4. The pull cursor is only persisted (to `server_time`, captured by the
 *     server BEFORE its query ran) once the whole pull succeeds.
 *  5. [syncReceipts]: a second, separate phase — expense receipt photos
 *     aren't part of the JSON batch above (see API_CONTRACT.md's "Expense
 *     receipt attachments"). Any expense with a local photo still pending
 *     upload, whose own JSON record is now SYNCED, gets that photo uploaded
 *     via a dedicated multipart endpoint. Runs after step 3 so a record
 *     that just became SYNCED in this very cycle is picked up immediately
 *     rather than waiting a whole extra cycle.
 *
 * A single [Mutex] serialises calls so the ~15 min WorkManager heartbeat, an
 * expedited post-write sync, and manual pull-to-refresh can never race each
 * other's push/pull halves.
 */
@Singleton
class SyncManager @Inject constructor(
    private val leadDao: LeadDao,
    private val customerDao: CustomerDao,
    private val quoteDao: QuoteDao,
    private val quoteLineItemDao: QuoteLineItemDao,
    private val jobDao: JobDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceLineItemDao: InvoiceLineItemDao,
    private val paymentDao: PaymentDao,
    private val expenseDao: ExpenseDao,
    private val apiService: OpsApiService,
    private val authPreferences: AuthPreferences,
    private val json: Json,
) {
    private val mutex = Mutex()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Every syncable row not yet cleanly SYNCED, across all 9 models — feeds
     * the top-bar chip. The sync status screen instead combines the typed
     * per-model DAO flows directly (via SyncStatusRepository) so it can show
     * per-record labels, not just counts. */
    private val unsyncedFlows: List<Flow<List<SyncableRecord>>> = listOf(
        leadDao.observeUnsynced(),
        customerDao.observeUnsynced(),
        quoteDao.observeUnsynced(),
        quoteLineItemDao.observeUnsynced(),
        jobDao.observeUnsynced(),
        invoiceDao.observeUnsynced(),
        invoiceLineItemDao.observeUnsynced(),
        paymentDao.observeUnsynced(),
        expenseDao.observeUnsynced(),
    )

    fun observeChipState(): Flow<SyncChipState> =
        combine(combine(unsyncedFlows) { arrays -> arrays.toList().flatten() }, isSyncing) { records, syncing ->
            val failedOrConflict = records.count { it.syncState == SyncState.FAILED || it.syncState == SyncState.CONFLICT }
            val pending = records.count { it.syncState == SyncState.PENDING || it.syncState == SyncState.SYNCING }
            when {
                syncing -> SyncChipState.Syncing
                failedOrConflict > 0 -> SyncChipState.Failed(failedOrConflict)
                pending > 0 -> SyncChipState.Pending(pending)
                else -> SyncChipState.Synced
            }
        }

    suspend fun syncNow(): SyncOutcome {
        if (authPreferences.currentAccessToken().isNullOrBlank()) return SyncOutcome.NotSignedIn
        return mutex.withLock {
            _isSyncing.value = true
            try {
                pushOutbox()
                pullChanges()
                syncReceipts()
                SyncOutcome.Success
            } catch (e: Exception) {
                SyncOutcome.Failed(describeError(e))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // ---- push --------------------------------------------------------------

    private suspend fun pushOutbox() {
        val leadOutbox = leadDao.getOutbox()
        val customerOutbox = customerDao.getOutbox()
        val quoteOutbox = quoteDao.getOutbox()
        val quoteLineItemOutbox = quoteLineItemDao.getOutbox()
        val jobOutbox = jobDao.getOutbox()
        val invoiceOutbox = invoiceDao.getOutbox()
        val invoiceLineItemOutbox = invoiceLineItemDao.getOutbox()
        val paymentOutbox = paymentDao.getOutbox()
        val expenseOutbox = expenseDao.getOutbox()

        val total = leadOutbox.size + customerOutbox.size + quoteOutbox.size + quoteLineItemOutbox.size +
            jobOutbox.size + invoiceOutbox.size + invoiceLineItemOutbox.size + paymentOutbox.size + expenseOutbox.size
        if (total == 0) return

        leadOutbox.forEach { leadDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        customerOutbox.forEach { customerDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        quoteOutbox.forEach { quoteDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        quoteLineItemOutbox.forEach { quoteLineItemDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        jobOutbox.forEach { jobDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        invoiceOutbox.forEach { invoiceDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        invoiceLineItemOutbox.forEach { invoiceLineItemDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        paymentOutbox.forEach { paymentDao.upsert(it.copy(syncState = SyncState.SYNCING)) }
        expenseOutbox.forEach { expenseDao.upsert(it.copy(syncState = SyncState.SYNCING)) }

        val changes: List<SyncChangeDto> = buildList {
            leadOutbox.forEach { add(it.toSyncChange(json)) }
            customerOutbox.forEach { add(it.toSyncChange(json)) }
            quoteOutbox.forEach { add(it.toSyncChange(json)) }
            quoteLineItemOutbox.forEach { add(it.toSyncChange(json)) }
            jobOutbox.forEach { add(it.toSyncChange(json)) }
            invoiceOutbox.forEach { add(it.toSyncChange(json)) }
            invoiceLineItemOutbox.forEach { add(it.toSyncChange(json)) }
            paymentOutbox.forEach { add(it.toSyncChange(json)) }
            expenseOutbox.forEach { add(it.toSyncChange(json)) }
        }

        val response = try {
            apiService.syncPush(SyncPushRequestDto(changes))
        } catch (e: Exception) {
            // Network/server failure for the whole batch: nothing was
            // dropped, every row that was about to sync goes to FAILED with
            // a visible retry action, per DISCOVERY.md section 6.
            val message = describeError(e)
            leadOutbox.forEach { leadDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            customerOutbox.forEach { customerDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            quoteOutbox.forEach { quoteDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            quoteLineItemOutbox.forEach { quoteLineItemDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            jobOutbox.forEach { jobDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            invoiceOutbox.forEach { invoiceDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            invoiceLineItemOutbox.forEach { invoiceLineItemDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            paymentOutbox.forEach { paymentDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            expenseOutbox.forEach { expenseDao.upsert(it.copy(syncState = SyncState.FAILED, syncError = message)) }
            throw e
        }

        response.results.forEach { result -> applyPushResult(result) }
    }

    private suspend fun applyPushResult(result: SyncResultDto) {
        when (result.model) {
            SyncModelKeys.LEAD -> leadDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<LeadFieldsDto>(it) }
                        ?.let { dto -> leadDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> leadDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> leadDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.CUSTOMER -> customerDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<CustomerFieldsDto>(it) }
                        ?.let { dto -> customerDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> customerDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> customerDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.QUOTE -> quoteDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<QuoteFieldsDto>(it) }
                        ?.let { dto -> quoteDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> quoteDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> quoteDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.QUOTE_LINE_ITEM -> quoteLineItemDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<QuoteLineItemFieldsDto>(it) }
                        ?.let { dto -> quoteLineItemDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> quoteLineItemDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> quoteLineItemDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.JOB -> jobDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<JobFieldsDto>(it) }
                        ?.let { dto -> jobDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> jobDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> jobDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.INVOICE -> invoiceDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<InvoiceFieldsDto>(it) }
                        ?.let { dto -> invoiceDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> invoiceDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> invoiceDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.INVOICE_LINE_ITEM -> invoiceLineItemDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<InvoiceLineItemFieldsDto>(it) }
                        ?.let { dto -> invoiceLineItemDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> invoiceLineItemDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> invoiceLineItemDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.PAYMENT -> paymentDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<PaymentFieldsDto>(it) }
                        ?.let { dto -> paymentDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED)) }
                    "conflict" -> paymentDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> paymentDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }

            SyncModelKeys.EXPENSE -> expenseDao.getById(result.id)?.let { existing ->
                when (result.status) {
                    "accepted" -> result.serverRecord?.let { json.decodeFromJsonElement<ExpenseFieldsDto>(it) }
                        ?.let { dto -> expenseDao.upsert(dto.toEntity(result.id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED, existing = existing)) }
                    "conflict" -> expenseDao.upsert(existing.copy(syncState = SyncState.CONFLICT, syncError = null, conflictServerJson = result.serverRecord?.toString()))
                    else -> expenseDao.upsert(existing.copy(syncState = SyncState.FAILED, syncError = result.errors.toSyncErrorMessage()))
                }
            }
        }
    }

    // ---- pull --------------------------------------------------------------

    private suspend fun pullChanges() {
        val since = authPreferences.currentSyncCursor()
        val response = apiService.syncPull(since)
        response.changes.forEach { change -> applyPullChange(change) }
        // Only persisted once the WHOLE pull has been applied — see class doc.
        authPreferences.saveSyncCursor(response.serverTime)
    }

    private suspend fun applyPullChange(change: SyncChangeDto) {
        val incomingUpdatedAt = runCatching { IsoTimestamp.parse(change.updatedAt) }.getOrNull() ?: return

        when (change.model) {
            SyncModelKeys.LEAD -> applyPull(leadDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<LeadFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { leadDao.upsert(it) }

            SyncModelKeys.CUSTOMER -> applyPull(customerDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<CustomerFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { customerDao.upsert(it) }

            SyncModelKeys.QUOTE -> applyPull(quoteDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<QuoteFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { quoteDao.upsert(it) }

            SyncModelKeys.QUOTE_LINE_ITEM -> applyPull(quoteLineItemDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<QuoteLineItemFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { quoteLineItemDao.upsert(it) }

            SyncModelKeys.JOB -> applyPull(jobDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<JobFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { jobDao.upsert(it) }

            SyncModelKeys.INVOICE -> applyPull(invoiceDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<InvoiceFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { invoiceDao.upsert(it) }

            SyncModelKeys.INVOICE_LINE_ITEM -> applyPull(invoiceLineItemDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<InvoiceLineItemFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { invoiceLineItemDao.upsert(it) }

            SyncModelKeys.PAYMENT -> applyPull(paymentDao.getById(change.id), incomingUpdatedAt) {
                json.decodeFromJsonElement<PaymentFieldsDto>(change.fields)
                    .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED)
            }?.let { paymentDao.upsert(it) }

            SyncModelKeys.EXPENSE -> {
                val existingExpense = expenseDao.getById(change.id)
                applyPull(existingExpense, incomingUpdatedAt) {
                    json.decodeFromJsonElement<ExpenseFieldsDto>(change.fields)
                        .toEntity(change.id, change.updatedAt, change.deletedAt, SyncState.SYNCED, existing = existingExpense)
                }?.let { expenseDao.upsert(it) }
            }
        }
    }

    /**
     * Shared guard for every pull branch above, in two layers:
     *
     *  1. A still-unsynced local row (PENDING/SYNCING/FAILED/CONFLICT) is
     *     never overwritten by a pull, unconditionally, regardless of
     *     timestamps — see API_CONTRACT.md's pull section ("a client never
     *     lets a pulled row overwrite a still-PENDING local row") and the
     *     class doc above. It queues behind the next push/conflict check.
     *  2. Otherwise (no local row yet, or the local row is clean/SYNCED),
     *     this is exactly core-domain's [decideSyncOutcome] — the same
     *     last-write-wins comparison the server itself makes. Consulting it
     *     even for a SYNCED row guards against an out-of-order or duplicate
     *     pull response ever regressing already-clean local data to a
     *     stale value.
     */
    private inline fun <T : SyncableRecord> applyPull(
        existing: T?,
        incomingUpdatedAt: Instant,
        build: () -> T,
    ): T? {
        if (existing != null && existing.syncState != SyncState.SYNCED) return null

        val existingUpdatedAt = existing?.let { runCatching { IsoTimestamp.parse(it.updatedAt) }.getOrNull() }
        return if (decideSyncOutcome(existingUpdatedAt, incomingUpdatedAt) == SyncDecision.ACCEPT) build() else null
    }

    // ---- receipt attachments -------------------------------------------------

    /**
     * Second, separate sync phase for expense receipt photos — see the
     * class doc and API_CONTRACT.md's "Expense receipt attachments". Each
     * upload is independent: one failing never blocks the others, and a
     * failure here never fails the overall [syncNow] outcome — a receipt
     * photo is an addition on top of an already-synced expense record, not
     * something worth retry-looping the whole sync engine over. Failures
     * are visible per-record instead (`receiptSyncState = FAILED`, with a
     * retry surfaced the same way a failed JSON push is).
     */
    private suspend fun syncReceipts() {
        for (expense in expenseDao.getReceiptOutbox()) {
            val localPath = expense.localReceiptPath
            val file = localPath?.let { File(it) }
            if (file == null || !file.exists()) {
                expenseDao.upsert(
                    expense.copy(
                        receiptSyncState = ReceiptSyncState.FAILED,
                        receiptSyncError = "That photo is missing on this phone. Try attaching it again.",
                    ),
                )
                continue
            }

            expenseDao.upsert(expense.copy(receiptSyncState = ReceiptSyncState.UPLOADING))
            try {
                val part = MultipartBody.Part.createFormData(
                    "receipt",
                    file.name,
                    file.asRequestBody(guessImageMediaType(file.name)),
                )
                val dto = apiService.uploadExpenseReceipt(expense.id, part)
                val current = expenseDao.getById(expense.id) ?: expense
                expenseDao.upsert(
                    current.copy(
                        receiptUrl = dto.receiptImage,
                        receiptSyncState = ReceiptSyncState.UPLOADED,
                        receiptSyncError = null,
                    ),
                )
            } catch (e: Exception) {
                val current = expenseDao.getById(expense.id) ?: expense
                expenseDao.upsert(current.copy(receiptSyncState = ReceiptSyncState.FAILED, receiptSyncError = describeError(e)))
            }
        }
    }

    private fun guessImageMediaType(fileName: String) = when (fileName.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }.toMediaType()

    private fun describeError(e: Exception): String = when (e) {
        is HttpException -> "Server error (HTTP ${e.code()}). Your changes are safe on this phone."
        is IOException -> "No connection. Your changes are safe on this phone — they'll sync automatically."
        else -> e.message ?: "Sync failed. Your changes are safe on this phone."
    }
}
