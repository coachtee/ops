package com.ops.app.data.repository

import androidx.work.WorkManager
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.PayslipDao
import com.ops.app.data.local.entities.PayslipEntity
import com.ops.app.data.remote.dto.PayslipFieldsDto
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
class PayslipRepository @Inject constructor(
    private val payslipDao: PayslipDao,
    private val workManager: WorkManager,
    private val json: Json,
) {
    fun observeAll(): Flow<List<PayslipEntity>> = payslipDao.observeAll()
    fun observeById(id: String): Flow<PayslipEntity?> = payslipDao.observeById(id)
    fun observeByEmployeeId(employeeId: String): Flow<List<PayslipEntity>> = payslipDao.observeByEmployeeId(employeeId)
    suspend fun getById(id: String): PayslipEntity? = payslipDao.getById(id)

    /**
     * Handles both create ([id] null) and edit ([id] set). `netPay` is
     * always derived from [grossPay]/[deductions] here via
     * [Money.computeNetPay] — the owner never types a net figure directly,
     * same as ExpenseRepository.save does for vatAmount. No PAYE/UIF
     * tax-table computation happens anywhere in this app.
     */
    suspend fun save(
        id: String?,
        employeeId: String,
        periodStart: String,
        periodEnd: String,
        grossPay: BigDecimal,
        deductions: BigDecimal,
        deductionsNote: String,
        paidDate: String?,
        notes: String,
    ): String {
        val existing = id?.let { payslipDao.getById(it) }
        val resolvedId = existing?.id ?: id ?: UUID.randomUUID().toString()
        val netPay = Money.computeNetPay(grossPay, deductions)
        payslipDao.upsert(
            PayslipEntity(
                id = resolvedId,
                employeeId = employeeId,
                periodStart = periodStart,
                periodEnd = periodEnd,
                grossPay = grossPay.toPlainString(),
                deductions = deductions.toPlainString(),
                deductionsNote = deductionsNote,
                netPay = netPay.toPlainString(),
                paidDate = paidDate,
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
        val existing = payslipDao.getById(id) ?: return
        val now = nowIso()
        payslipDao.upsert(existing.copy(deletedAt = now, updatedAt = now, syncState = SyncState.PENDING))
        enqueueSync()
    }

    // ---- sync status screen actions -----------------------------------------

    suspend fun retry(id: String) {
        val existing = payslipDao.getById(id) ?: return
        payslipDao.upsert(existing.copy(syncState = SyncState.PENDING, syncError = null))
        enqueueSync()
    }

    suspend fun keepMine(id: String) {
        val existing = payslipDao.getById(id) ?: return
        payslipDao.upsert(existing.copy(updatedAt = nowIso(), syncState = SyncState.PENDING, conflictServerJson = null))
        enqueueSync()
    }

    suspend fun useTheirs(id: String) {
        val existing = payslipDao.getById(id) ?: return
        val serverJson = existing.conflictServerJson ?: return
        val dto = json.decodeFromString(PayslipFieldsDto.serializer(), serverJson)
        payslipDao.upsert(dto.toEntity(id, dto.serverUpdatedAt ?: existing.updatedAt, dto.serverDeletedAt, SyncState.SYNCED))
    }

    private fun enqueueSync() = SyncWorker.enqueueOneTime(workManager)

    private fun nowIso() = IsoTimestamp.format(Instant.now())
}
