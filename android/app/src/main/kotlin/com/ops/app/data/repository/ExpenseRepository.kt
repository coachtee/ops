package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.ExpenseDao
import com.ops.app.data.local.entities.ExpenseEntity
import com.ops.app.data.remote.dto.ExpenseFieldsDto
import com.ops.app.data.sync.SyncWorker
import com.ops.app.data.sync.toEntity
import com.ops.coredomain.IsoTimestamp
import com.ops.coredomain.Money
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<ExpenseEntity>> = expenseDao.observeAll()
    fun observeById(id: String): Flow<ExpenseEntity?> = expenseDao.observeById(id)
    fun observeByJobId(jobId: String): Flow<List<ExpenseEntity>> = expenseDao.observeByJobId(jobId)
    suspend fun getById(id: String): ExpenseEntity? = expenseDao.getById(id)

    /**
     * Handles both create ([id] null) and edit ([id] set). `vatAmount` is
     * always derived from [amount] here via [Money.extractVatFromInclusive]
     * — the owner never types a VAT figure directly, same as the backend
     * (see ExpenseSerializer.vat_amount being read-only). Receipt fields
     * are untouched by this call — see [attachReceipt].
     */
    suspend fun save(
        id: String?,
        jobId: String?,
        category: String,
        description: String,
        amount: BigDecimal,
        isVatApplicable: Boolean,
        date: String,
    ): String {
        val existing = id?.let { expenseDao.getById(it) }
        val resolvedId = existing?.id ?: id ?: UUID.randomUUID().toString()
        val vatAmount = Money.extractVatFromInclusive(amount, isVatApplicable)
        expenseDao.upsert(
            ExpenseEntity(
                id = resolvedId,
                jobId = jobId,
                category = category,
                description = description,
                amount = amount.toPlainString(),
                isVatApplicable = isVatApplicable,
                vatAmount = vatAmount.toPlainString(),
                date = date,
                receiptUrl = existing?.receiptUrl,
                localReceiptPath = existing?.localReceiptPath,
                receiptSyncState = existing?.receiptSyncState ?: ReceiptSyncState.NONE,
                receiptSyncError = existing?.receiptSyncError,
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

    /** A photo was just captured/picked and copied to permanent local
     * storage at [localFilePath] — queue it for the next sync cycle's
     * receipt-upload phase (see SyncManager.syncReceipts). Does NOT touch
     * the expense's own `syncState`/`updatedAt` — attaching a receipt isn't
     * a JSON-field edit, it's the separate attachment-sync phase. */
    suspend fun attachReceipt(id: String, localFilePath: String) {
        val existing = expenseDao.getById(id) ?: return
        expenseDao.upsert(
            existing.copy(
                localReceiptPath = localFilePath,
                receiptSyncState = ReceiptSyncState.PENDING,
                receiptSyncError = null,
            ),
        )
        enqueueSync()
    }

    /** Retry a receipt upload that previously failed. */
    suspend fun retryReceipt(id: String) {
        val existing = expenseDao.getById(id) ?: return
        if (existing.localReceiptPath == null) return
        expenseDao.upsert(existing.copy(receiptSyncState = ReceiptSyncState.PENDING, receiptSyncError = null))
        enqueueSync()
    }

    suspend fun delete(id: String) {
        val existing = expenseDao.getById(id) ?: return
        val now = nowIso()
        expenseDao.upsert(existing.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        enqueueSync()
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = expenseDao.getById(id) ?: return
        expenseDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = expenseDao.getById(id) ?: return
        expenseDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = expenseDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(ExpenseFieldsDto.serializer(), serverJson)
        expenseDao.upsert(
            dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED, existing = existing),
        )
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
