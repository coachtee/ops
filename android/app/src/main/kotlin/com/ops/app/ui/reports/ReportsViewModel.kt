package com.ops.app.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.repository.ExpenseRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.PaymentRepository
import com.ops.coredomain.InvoiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

private const val PROFIT_MONTHS = 6

data class MonthlyProfitRow(
    val month: YearMonth,
    val revenue: BigDecimal,
    val expenses: BigDecimal,
) {
    val profit: BigDecimal get() = revenue.subtract(expenses)
}

data class ExpenseCategoryRow(
    val category: String,
    val total: BigDecimal,
)

data class ReportsUiState(
    /** Last [PROFIT_MONTHS] calendar months, oldest first — a month with no
     * activity is a zero row, not omitted, same as the backend's
     * `/api/reports/profit-summary/` (see API_CONTRACT.md's "Reports"
     * section). "Revenue" is cash-basis (payments received), matching
     * Home's "money in" definition exactly. */
    val monthlyProfit: List<MonthlyProfitRow> = emptyList(),
    /** This calendar month's expenses by category, biggest first —
     * categories with nothing spent are omitted. */
    val expenseCategoriesThisMonth: List<ExpenseCategoryRow> = emptyList(),
    val vatCollectedThisMonth: BigDecimal = BigDecimal.ZERO,
    val vatPaidThisMonth: BigDecimal = BigDecimal.ZERO,
) {
    val netVatPosition: BigDecimal get() = vatCollectedThisMonth.subtract(vatPaidThisMonth)
}

/**
 * REPORTS tab: the question-shaped answers DISCOVERY.md section 4 promises
 * ("what did I make this month", "what are my biggest expenses", VAT
 * collected vs paid) — computed entirely from data already synced locally
 * (Payment, Expense, Invoice), the same offline-first way Home's stat
 * cards already work. No network call of its own; mirrors
 * `backend/reports/views.py`'s aggregation logic (same cash-basis revenue
 * definition, same draft/cancelled exclusion for VAT collected) so the
 * numbers agree if this is ever checked against the server-side
 * equivalent, but nothing here needs to be byte-exact with a server
 * computation the way VAT/net_pay do (plain BigDecimal subtraction has no
 * rounding-mode ambiguity), so — same precedent as HomeViewModel's own
 * stat cards — this lives in the `app` module, not core-domain.
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    paymentRepository: PaymentRepository,
    expenseRepository: ExpenseRepository,
    invoiceRepository: InvoiceRepository,
) : ViewModel() {

    val uiState: StateFlow<ReportsUiState> = combine(
        paymentRepository.observeAll(),
        expenseRepository.observeAll(),
        invoiceRepository.observeAll(),
    ) { payments, expenses, invoices ->
        val thisMonth = YearMonth.now()
        val months = (PROFIT_MONTHS - 1 downTo 0).map { thisMonth.minusMonths(it.toLong()) }

        val monthlyProfit = months.map { month ->
            val revenue = payments
                .filter { runCatching { YearMonth.from(LocalDate.parse(it.paidDate)) == month }.getOrDefault(false) }
                .fold(BigDecimal.ZERO) { acc, p -> acc.add(p.amount.toSafeBigDecimalOrZero()) }
            val monthExpenses = expenses
                .filter { runCatching { YearMonth.from(LocalDate.parse(it.date)) == month }.getOrDefault(false) }
                .fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount.toSafeBigDecimalOrZero()) }
            MonthlyProfitRow(month = month, revenue = revenue, expenses = monthExpenses)
        }

        val thisMonthExpenses = expenses.filter {
            runCatching { YearMonth.from(LocalDate.parse(it.date)) == thisMonth }.getOrDefault(false)
        }
        val expenseCategories = thisMonthExpenses
            .groupBy { it.category }
            .map { (category, group) ->
                ExpenseCategoryRow(
                    category = category,
                    total = group.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.amount.toSafeBigDecimalOrZero()) },
                )
            }
            .filter { it.total.signum() > 0 }
            .sortedByDescending { it.total }

        val nonFiledStatuses = setOf(InvoiceStatus.DRAFT.wire, InvoiceStatus.CANCELLED.wire)
        val vatCollected = invoices
            .filter { it.status !in nonFiledStatuses }
            .filter { runCatching { YearMonth.from(LocalDate.parse(it.issueDate)) == thisMonth }.getOrDefault(false) }
            .fold(BigDecimal.ZERO) { acc, inv -> acc.add(inv.vatAmount.toSafeBigDecimalOrZero()) }
        val vatPaid = thisMonthExpenses.fold(BigDecimal.ZERO) { acc, e -> acc.add(e.vatAmount.toSafeBigDecimalOrZero()) }

        ReportsUiState(
            monthlyProfit = monthlyProfit,
            expenseCategoriesThisMonth = expenseCategories,
            vatCollectedThisMonth = vatCollected,
            vatPaidThisMonth = vatPaid,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())
}

private fun String.toSafeBigDecimalOrZero(): BigDecimal = runCatching { BigDecimal(this) }.getOrDefault(BigDecimal.ZERO)
