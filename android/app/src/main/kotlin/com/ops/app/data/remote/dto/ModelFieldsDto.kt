package com.ops.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One `@Serializable` data class per syncable model, matching the `fields`
 * payload documented in API_CONTRACT.md's "Model field payloads" section
 * field-for-field (same names as the CRUD body). These are encoded into /
 * decoded out of [SyncChangeDto.fields] by the mapper functions in
 * com.ops.app.data.sync.SyncMappers.kt — never used as a Retrofit `@Body`
 * directly, since the per-resource CRUD endpoints are not this app's main
 * read/write path (the sync endpoints are — see API_CONTRACT.md).
 *
 * Server-computed fields (`line_total`, `subtotal`, `vat_amount`, `total`,
 * `amount_paid`, `number`) are still included so a pulled `server_record`
 * can be decoded, but this client always sends its own locally-computed
 * value on push — the server recomputes and echoes back the authoritative
 * figure in its `accepted` response regardless of what was sent.
 *
 * Each class also carries a trailing pair of server-echo fields
 * (`serverUpdatedAt`/`serverDeletedAt`, mapped from the wire's
 * `updated_at`/`deleted_at`): `sync/services.py` serializes `server_record`
 * (push) and pull's `fields` from the SAME full ModelSerializer, so both
 * actually contain the whole row — id/updated_at/deleted_at included —
 * flattened in alongside the model-specific keys below, not just the
 * narrower "Model field payloads" shape. These two stay `null` (and, thanks
 * to `Json { explicitNulls = false }`, are omitted entirely) on anything
 * *this app* encodes for a push — only decoding a server response ever
 * populates them.
 */

@Serializable
data class LeadFieldsDto(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val source: String = "other",
    val enquiry: String = "",
    val notes: String = "",
    val status: String = "new",
    @SerialName("follow_up_date") val followUpDate: String? = null,
    @SerialName("converted_customer_id") val convertedCustomerId: String? = null,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

@Serializable
data class CustomerFieldsDto(
    val name: String = "",
    @SerialName("customer_type") val customerType: String = "individual",
    val phone: String = "",
    val email: String = "",
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String = "",
    val suburb: String = "",
    val city: String = "",
    val province: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    val notes: String = "",
    @SerialName("source_lead_id") val sourceLeadId: String? = null,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

@Serializable
data class QuoteFieldsDto(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("lead_id") val leadId: String? = null,
    val number: String? = null,
    val status: String = "draft",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("valid_until") val validUntil: String? = null,
    val notes: String = "",
    val terms: String = "",
    @SerialName("is_vat_applicable") val isVatApplicable: Boolean = true,
    @SerialName("discount_amount") val discountAmount: String = "0.00",
    val subtotal: String = "0.00",
    @SerialName("vat_amount") val vatAmount: String = "0.00",
    val total: String = "0.00",
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("accepted_at") val acceptedAt: String? = null,
    @SerialName("declined_at") val declinedAt: String? = null,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

@Serializable
data class QuoteLineItemFieldsDto(
    @SerialName("quote_id") val quoteId: String = "",
    val description: String = "",
    val quantity: String = "1.00",
    @SerialName("unit_price") val unitPrice: String = "0.00",
    @SerialName("line_total") val lineTotal: String = "0.00",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

@Serializable
data class JobFieldsDto(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("quote_id") val quoteId: String? = null,
    val number: String? = null,
    val title: String = "",
    val description: String = "",
    val status: String = "not_started",
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completed_date") val completedDate: String? = null,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

@Serializable
data class InvoiceFieldsDto(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("job_id") val jobId: String? = null,
    @SerialName("quote_id") val quoteId: String? = null,
    val number: String? = null,
    val status: String = "draft",
    @SerialName("issue_date") val issueDate: String = "",
    @SerialName("due_date") val dueDate: String? = null,
    val notes: String = "",
    val terms: String = "",
    @SerialName("is_vat_applicable") val isVatApplicable: Boolean = true,
    @SerialName("discount_amount") val discountAmount: String = "0.00",
    val subtotal: String = "0.00",
    @SerialName("vat_amount") val vatAmount: String = "0.00",
    val total: String = "0.00",
    @SerialName("amount_paid") val amountPaid: String = "0.00",
    @SerialName("sent_at") val sentAt: String? = null,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

@Serializable
data class InvoiceLineItemFieldsDto(
    @SerialName("invoice_id") val invoiceId: String = "",
    val description: String = "",
    val quantity: String = "1.00",
    @SerialName("unit_price") val unitPrice: String = "0.00",
    @SerialName("line_total") val lineTotal: String = "0.00",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

@Serializable
data class PaymentFieldsDto(
    @SerialName("customer_id") val customerId: String = "",
    @SerialName("invoice_id") val invoiceId: String? = null,
    val amount: String = "0.00",
    val method: String = "eft",
    val reference: String = "",
    @SerialName("paid_date") val paidDate: String = "",
    val notes: String = "",
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)

/**
 * `receipt_image` is read-only on the wire (never sent on push — see
 * API_CONTRACT.md's `expense` row and its "Expense receipt attachments"
 * addendum) — it's only ever populated when decoding a `server_record` or a
 * pull change, from a photo uploaded via the separate multipart endpoint.
 */
@Serializable
data class ExpenseFieldsDto(
    @SerialName("job_id") val jobId: String? = null,
    val category: String = "other",
    val description: String = "",
    val amount: String = "0.00",
    @SerialName("is_vat_applicable") val isVatApplicable: Boolean = false,
    @SerialName("vat_amount") val vatAmount: String = "0.00",
    val date: String = "",
    @SerialName("receipt_image") val receiptImage: String? = null,
    @SerialName("updated_at") val serverUpdatedAt: String? = null,
    @SerialName("deleted_at") val serverDeletedAt: String? = null,
)
