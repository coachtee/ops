package com.ops.app.ui.money

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ops.app.data.local.entities.ExpenseEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.repository.CustomerRepository
import com.ops.app.data.repository.ExpenseRepository
import com.ops.app.data.repository.InvoiceRepository
import com.ops.app.data.repository.PaymentRepository
import com.ops.coredomain.InvoiceStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MoneyUiState(
    val outstandingInvoices: List<InvoiceEntity> = emptyList(),
    val paymentsReceived: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val customerNames: Map<String, String> = emptyMap(),
)

/** MONEY tab: "Outstanding invoices · Payments received · Expenses" per
 * DISCOVERY.md's IA. */
@HiltViewModel
class MoneyViewModel @Inject constructor(
    invoiceRepository: InvoiceRepository,
    paymentRepository: PaymentRepository,
    expenseRepository: ExpenseRepository,
    customerRepository: CustomerRepository,
) : ViewModel() {

    val uiState: StateFlow<MoneyUiState> = combine(
        invoiceRepository.observeAll(),
        paymentRepository.observeAll(),
        expenseRepository.observeAll(),
        customerRepository.observeAll(),
    ) { invoices, payments, expenses, customers ->
        val outstanding = invoices
            .filter { it.status != InvoiceStatus.PAID.wire && it.status != InvoiceStatus.CANCELLED.wire && it.status != InvoiceStatus.DRAFT.wire }
            .sortedBy { it.issueDate } // oldest-first, per DISCOVERY.md's "Who owes me money" journey
        val recentPayments = payments.sortedByDescending { it.paidDate }
        val recentExpenses = expenses.sortedByDescending { it.date }
        MoneyUiState(
            outstandingInvoices = outstanding,
            paymentsReceived = recentPayments,
            expenses = recentExpenses,
            customerNames = customers.associate { it.id to it.name },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyUiState())
}
