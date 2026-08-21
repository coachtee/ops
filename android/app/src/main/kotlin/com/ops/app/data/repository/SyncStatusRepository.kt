package com.ops.app.data.repository

import com.ops.app.data.local.SyncState
import com.ops.app.data.local.dao.CustomerDao
import com.ops.app.data.local.dao.EmployeeDao
import com.ops.app.data.local.dao.ExpenseDao
import com.ops.app.data.local.dao.InvoiceDao
import com.ops.app.data.local.dao.InvoiceLineItemDao
import com.ops.app.data.local.dao.JobDao
import com.ops.app.data.local.dao.LeadDao
import com.ops.app.data.local.dao.PaymentDao
import com.ops.app.data.local.dao.PayslipDao
import com.ops.app.data.local.dao.QuoteDao
import com.ops.app.data.local.dao.QuoteLineItemDao
import com.ops.app.data.local.dao.SupplierDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs the sync status screen: every not-yet-SYNCED row across all 12
 * syncable models, as one flat list, with a retry action and the explicit
 * "Keep mine" / "Use theirs" conflict resolution the brief requires (see
 * DISCOVERY.md section 6 and API_CONTRACT.md's conflict handling) — each
 * dispatched to the owning aggregate repository, which is where the actual
 * Room write + re-sync trigger happens. Note this only covers an expense's
 * own JSON record — a stuck receipt-photo upload is a separate state
 * ([com.ops.app.data.local.ReceiptSyncState]) surfaced inline on the
 * expense screen itself, not here (see ExpenseRepository.retryReceipt).
 */
@Singleton
class SyncStatusRepository @Inject constructor(
    private val leadDao: LeadDao,
    private val customerDao: CustomerDao,
    private val quoteDao: QuoteDao,
    private val quoteLineItemDao: QuoteLineItemDao,
    private val jobDao: JobDao,
    private val invoiceDao: InvoiceDao,
    private val invoiceLineItemDao: InvoiceLineItemDao,
    private val paymentDao: PaymentDao,
    private val supplierDao: SupplierDao,
    private val expenseDao: ExpenseDao,
    private val employeeDao: EmployeeDao,
    private val payslipDao: PayslipDao,
    private val leadRepository: LeadRepository,
    private val customerRepository: CustomerRepository,
    private val quoteRepository: QuoteRepository,
    private val jobRepository: JobRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    private val supplierRepository: SupplierRepository,
    private val expenseRepository: ExpenseRepository,
    private val employeeRepository: EmployeeRepository,
    private val payslipRepository: PayslipRepository,
) {
    fun observeItems(): Flow<List<SyncStatusItem>> {
        val flows: List<Flow<List<SyncStatusItem>>> = listOf(
            leadDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Lead(it) } },
            customerDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Customer(it) } },
            quoteDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Quote(it) } },
            quoteLineItemDao.observeUnsynced().map { list -> list.map { SyncStatusItem.QuoteLineItem(it) } },
            jobDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Job(it) } },
            invoiceDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Invoice(it) } },
            invoiceLineItemDao.observeUnsynced().map { list -> list.map { SyncStatusItem.InvoiceLineItem(it) } },
            paymentDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Payment(it) } },
            supplierDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Supplier(it) } },
            expenseDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Expense(it) } },
            employeeDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Employee(it) } },
            payslipDao.observeUnsynced().map { list -> list.map { SyncStatusItem.Payslip(it) } },
        )
        return combine(flows) { arrays -> arrays.toList().flatten().sortedBy { priority(it.syncState) } }
    }

    suspend fun retry(item: SyncStatusItem) {
        when (item) {
            is SyncStatusItem.Lead -> leadRepository.retry(item.id)
            is SyncStatusItem.Customer -> customerRepository.retry(item.id)
            is SyncStatusItem.Quote -> quoteRepository.retry(item.id)
            is SyncStatusItem.QuoteLineItem -> quoteRepository.retryLineItem(item.id)
            is SyncStatusItem.Job -> jobRepository.retry(item.id)
            is SyncStatusItem.Invoice -> invoiceRepository.retry(item.id)
            is SyncStatusItem.InvoiceLineItem -> invoiceRepository.retryLineItem(item.id)
            is SyncStatusItem.Payment -> paymentRepository.retry(item.id)
            is SyncStatusItem.Supplier -> supplierRepository.retry(item.id)
            is SyncStatusItem.Expense -> expenseRepository.retry(item.id)
            is SyncStatusItem.Employee -> employeeRepository.retry(item.id)
            is SyncStatusItem.Payslip -> payslipRepository.retry(item.id)
        }
    }

    /** "Keep mine": bump this row's `updatedAt` to now and re-push it. */
    suspend fun keepMine(item: SyncStatusItem) {
        when (item) {
            is SyncStatusItem.Lead -> leadRepository.keepMine(item.id)
            is SyncStatusItem.Customer -> customerRepository.keepMine(item.id)
            is SyncStatusItem.Quote -> quoteRepository.keepMine(item.id)
            is SyncStatusItem.QuoteLineItem -> quoteRepository.keepMineLineItem(item.id)
            is SyncStatusItem.Job -> jobRepository.keepMine(item.id)
            is SyncStatusItem.Invoice -> invoiceRepository.keepMine(item.id)
            is SyncStatusItem.InvoiceLineItem -> invoiceRepository.keepMineLineItem(item.id)
            is SyncStatusItem.Payment -> paymentRepository.keepMine(item.id)
            is SyncStatusItem.Supplier -> supplierRepository.keepMine(item.id)
            is SyncStatusItem.Expense -> expenseRepository.keepMine(item.id)
            is SyncStatusItem.Employee -> employeeRepository.keepMine(item.id)
            is SyncStatusItem.Payslip -> payslipRepository.keepMine(item.id)
        }
    }

    /** "Use theirs": overwrite the local row from the stored `conflictServerJson`. */
    suspend fun useTheirs(item: SyncStatusItem) {
        when (item) {
            is SyncStatusItem.Lead -> leadRepository.useTheirs(item.id)
            is SyncStatusItem.Customer -> customerRepository.useTheirs(item.id)
            is SyncStatusItem.Quote -> quoteRepository.useTheirs(item.id)
            is SyncStatusItem.QuoteLineItem -> quoteRepository.useTheirsLineItem(item.id)
            is SyncStatusItem.Job -> jobRepository.useTheirs(item.id)
            is SyncStatusItem.Invoice -> invoiceRepository.useTheirs(item.id)
            is SyncStatusItem.InvoiceLineItem -> invoiceRepository.useTheirsLineItem(item.id)
            is SyncStatusItem.Payment -> paymentRepository.useTheirs(item.id)
            is SyncStatusItem.Supplier -> supplierRepository.useTheirs(item.id)
            is SyncStatusItem.Expense -> expenseRepository.useTheirs(item.id)
            is SyncStatusItem.Employee -> employeeRepository.useTheirs(item.id)
            is SyncStatusItem.Payslip -> payslipRepository.useTheirs(item.id)
        }
    }

    private fun priority(state: String) = when (state) {
        SyncState.CONFLICT -> 0
        SyncState.FAILED -> 1
        SyncState.SYNCING -> 2
        SyncState.PENDING -> 3
        else -> 4
    }
}
