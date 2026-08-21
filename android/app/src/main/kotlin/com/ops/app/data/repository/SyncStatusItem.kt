package com.ops.app.data.repository

import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.ExpenseEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity

/** One row on the sync status screen — wraps whichever concrete entity it
 * is, plus the display bits every row needs regardless of type. See
 * [SyncStatusRepository]. */
sealed class SyncStatusItem(
    val id: String,
    val syncState: String,
    val syncError: String?,
    /** e.g. "Lead", "Invoice", "Payment" — the row's type badge. */
    val kindLabel: String,
    /** e.g. a lead's name, an invoice's number — the row's main text. */
    val title: String,
) {
    class Lead(val entity: LeadEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Lead", entity.name)

    class Customer(val entity: CustomerEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Customer", entity.name)

    class Quote(val entity: QuoteEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Quote", entity.number ?: "Draft quote")

    class QuoteLineItem(val entity: QuoteLineItemEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Quote item", entity.description)

    class Job(val entity: JobEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Job", entity.number ?: entity.title)

    class Invoice(val entity: InvoiceEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Invoice", entity.number ?: "Draft invoice")

    class InvoiceLineItem(val entity: InvoiceLineItemEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Invoice item", entity.description)

    class Payment(val entity: PaymentEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Payment", "R${entity.amount}")

    class Expense(val entity: ExpenseEntity) :
        SyncStatusItem(entity.id, entity.syncState, entity.syncError, "Expense", entity.description.ifBlank { "R${entity.amount}" })
}
