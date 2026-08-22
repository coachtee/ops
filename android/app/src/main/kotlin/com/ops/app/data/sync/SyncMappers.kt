package com.ops.app.data.sync

import com.ops.app.data.local.ReceiptSyncState
import com.ops.app.data.local.SyncState
import com.ops.app.data.local.entities.ComplianceItemEntity
import com.ops.app.data.local.entities.CustomerEntity
import com.ops.app.data.local.entities.EmployeeEntity
import com.ops.app.data.local.entities.ExpenseEntity
import com.ops.app.data.local.entities.InvoiceEntity
import com.ops.app.data.local.entities.InvoiceLineItemEntity
import com.ops.app.data.local.entities.JobEntity
import com.ops.app.data.local.entities.LeadEntity
import com.ops.app.data.local.entities.PaymentEntity
import com.ops.app.data.local.entities.PayslipEntity
import com.ops.app.data.local.entities.QuoteEntity
import com.ops.app.data.local.entities.QuoteLineItemEntity
import com.ops.app.data.local.entities.SupplierEntity
import com.ops.app.data.local.entities.VisitEntity
import com.ops.app.data.remote.dto.ComplianceItemFieldsDto
import com.ops.app.data.remote.dto.CustomerFieldsDto
import com.ops.app.data.remote.dto.EmployeeFieldsDto
import com.ops.app.data.remote.dto.ExpenseFieldsDto
import com.ops.app.data.remote.dto.InvoiceFieldsDto
import com.ops.app.data.remote.dto.InvoiceLineItemFieldsDto
import com.ops.app.data.remote.dto.JobFieldsDto
import com.ops.app.data.remote.dto.LeadFieldsDto
import com.ops.app.data.remote.dto.PaymentFieldsDto
import com.ops.app.data.remote.dto.PayslipFieldsDto
import com.ops.app.data.remote.dto.QuoteFieldsDto
import com.ops.app.data.remote.dto.QuoteLineItemFieldsDto
import com.ops.app.data.remote.dto.SupplierFieldsDto
import com.ops.app.data.remote.dto.SyncChangeDto
import com.ops.app.data.remote.dto.VisitFieldsDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Entity <-> wire mapping for every syncable model. Two directions:
 *   - `xxxEntity.toFieldsDto()` / `.toSyncChange(json)` — outgoing, for
 *     [com.ops.app.data.sync.SyncManager]'s push.
 *   - `XxxFieldsDto.toEntity(...)` — incoming, for merging an `accepted`
 *     push result or a pull change back into Room.
 *
 * These are hand-written 1:1 mappings, not reflection-driven, so a field
 * added to one side and not the other fails to compile rather than silently
 * dropping data.
 */

// ---- Lead ----------------------------------------------------------------

fun LeadEntity.toFieldsDto() = LeadFieldsDto(
    name = name,
    phone = phone,
    email = email,
    source = source,
    enquiry = enquiry,
    notes = notes,
    status = status,
    followUpDate = followUpDate,
    convertedCustomerId = convertedCustomerId,
)

fun LeadEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.LEAD,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun LeadFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = LeadEntity(
    id = id,
    name = name,
    phone = phone,
    email = email,
    source = source,
    enquiry = enquiry,
    notes = notes,
    status = status,
    followUpDate = followUpDate,
    convertedCustomerId = convertedCustomerId,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Customer --------------------------------------------------------------

fun CustomerEntity.toFieldsDto() = CustomerFieldsDto(
    name = name,
    customerType = customerType,
    phone = phone,
    email = email,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    suburb = suburb,
    city = city,
    province = province,
    postalCode = postalCode,
    notes = notes,
    sourceLeadId = sourceLeadId,
)

fun CustomerEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.CUSTOMER,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun CustomerFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = CustomerEntity(
    id = id,
    name = name,
    customerType = customerType,
    phone = phone,
    email = email,
    addressLine1 = addressLine1,
    addressLine2 = addressLine2,
    suburb = suburb,
    city = city,
    province = province,
    postalCode = postalCode,
    notes = notes,
    sourceLeadId = sourceLeadId,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Quote -----------------------------------------------------------------

fun QuoteEntity.toFieldsDto() = QuoteFieldsDto(
    customerId = customerId,
    leadId = leadId,
    number = number,
    status = status,
    issueDate = issueDate,
    validUntil = validUntil,
    notes = notes,
    terms = terms,
    isVatApplicable = isVatApplicable,
    discountAmount = discountAmount,
    subtotal = subtotal,
    vatAmount = vatAmount,
    total = total,
    sentAt = sentAt,
    acceptedAt = acceptedAt,
    declinedAt = declinedAt,
)

fun QuoteEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.QUOTE,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun QuoteFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = QuoteEntity(
    id = id,
    customerId = customerId,
    leadId = leadId,
    number = number,
    status = status,
    issueDate = issueDate,
    validUntil = validUntil,
    notes = notes,
    terms = terms,
    isVatApplicable = isVatApplicable,
    discountAmount = discountAmount,
    subtotal = subtotal,
    vatAmount = vatAmount,
    total = total,
    sentAt = sentAt,
    acceptedAt = acceptedAt,
    declinedAt = declinedAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- QuoteLineItem -----------------------------------------------------------

fun QuoteLineItemEntity.toFieldsDto() = QuoteLineItemFieldsDto(
    quoteId = quoteId,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
    sortOrder = sortOrder,
)

fun QuoteLineItemEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.QUOTE_LINE_ITEM,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun QuoteLineItemFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = QuoteLineItemEntity(
    id = id,
    quoteId = quoteId,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Job ---------------------------------------------------------------------

fun JobEntity.toFieldsDto() = JobFieldsDto(
    customerId = customerId,
    quoteId = quoteId,
    number = number,
    title = title,
    description = description,
    status = status,
    startDate = startDate,
    dueDate = dueDate,
    completedDate = completedDate,
)

fun JobEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.JOB,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun JobFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = JobEntity(
    id = id,
    customerId = customerId,
    quoteId = quoteId,
    number = number,
    title = title,
    description = description,
    status = status,
    startDate = startDate,
    dueDate = dueDate,
    completedDate = completedDate,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Visit -----------------------------------------------------------------

fun VisitEntity.toFieldsDto() = VisitFieldsDto(
    jobId = jobId,
    employeeId = employeeId,
    scheduledDate = scheduledDate,
    startTime = startTime,
    endTime = endTime,
    status = status,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt,
    // photo is read-only on the wire — never sent, see API_CONTRACT.md.
)

fun VisitEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.VISIT,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

/** Takes [existing] for the same reason [ExpenseFieldsDto.toEntity] does —
 * the photo-upload phase's local-only state (localPhotoPath/photoSyncState/
 * photoSyncError) survives being overwritten by wire data that knows
 * nothing about it. */
fun VisitFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
    existing: VisitEntity? = null,
) = VisitEntity(
    id = id,
    jobId = jobId,
    employeeId = employeeId,
    scheduledDate = scheduledDate,
    startTime = startTime,
    endTime = endTime,
    status = status,
    notes = notes,
    startedAt = startedAt,
    completedAt = completedAt,
    photoUrl = photo ?: existing?.photoUrl,
    localPhotoPath = existing?.localPhotoPath,
    photoSyncState = if (photo != null) ReceiptSyncState.UPLOADED else existing?.photoSyncState ?: ReceiptSyncState.NONE,
    photoSyncError = if (photo != null) null else existing?.photoSyncError,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Invoice -------------------------------------------------------------------

fun InvoiceEntity.toFieldsDto() = InvoiceFieldsDto(
    customerId = customerId,
    jobId = jobId,
    quoteId = quoteId,
    number = number,
    status = status,
    issueDate = issueDate,
    dueDate = dueDate,
    notes = notes,
    terms = terms,
    isVatApplicable = isVatApplicable,
    discountAmount = discountAmount,
    subtotal = subtotal,
    vatAmount = vatAmount,
    total = total,
    amountPaid = amountPaid,
    sentAt = sentAt,
)

fun InvoiceEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.INVOICE,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun InvoiceFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = InvoiceEntity(
    id = id,
    customerId = customerId,
    jobId = jobId,
    quoteId = quoteId,
    number = number,
    status = status,
    issueDate = issueDate,
    dueDate = dueDate,
    notes = notes,
    terms = terms,
    isVatApplicable = isVatApplicable,
    discountAmount = discountAmount,
    subtotal = subtotal,
    vatAmount = vatAmount,
    total = total,
    amountPaid = amountPaid,
    sentAt = sentAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- InvoiceLineItem ----------------------------------------------------------

fun InvoiceLineItemEntity.toFieldsDto() = InvoiceLineItemFieldsDto(
    invoiceId = invoiceId,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
    sortOrder = sortOrder,
)

fun InvoiceLineItemEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.INVOICE_LINE_ITEM,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun InvoiceLineItemFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = InvoiceLineItemEntity(
    id = id,
    invoiceId = invoiceId,
    description = description,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
    sortOrder = sortOrder,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Payment -------------------------------------------------------------------

fun PaymentEntity.toFieldsDto() = PaymentFieldsDto(
    customerId = customerId,
    invoiceId = invoiceId,
    amount = amount,
    method = method,
    reference = reference,
    paidDate = paidDate,
    notes = notes,
)

fun PaymentEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.PAYMENT,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun PaymentFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = PaymentEntity(
    id = id,
    customerId = customerId,
    invoiceId = invoiceId,
    amount = amount,
    method = method,
    reference = reference,
    paidDate = paidDate,
    notes = notes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Supplier --------------------------------------------------------------

fun SupplierEntity.toFieldsDto() = SupplierFieldsDto(
    name = name,
    contactPerson = contactPerson,
    phone = phone,
    email = email,
    notes = notes,
)

fun SupplierEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.SUPPLIER,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun SupplierFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = SupplierEntity(
    id = id,
    name = name,
    contactPerson = contactPerson,
    phone = phone,
    email = email,
    notes = notes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Expense -------------------------------------------------------------------

fun ExpenseEntity.toFieldsDto() = ExpenseFieldsDto(
    jobId = jobId,
    supplierId = supplierId,
    category = category,
    description = description,
    amount = amount,
    isVatApplicable = isVatApplicable,
    vatAmount = vatAmount,
    date = date,
    // receiptImage is read-only on the wire — never sent, see API_CONTRACT.md.
)

fun ExpenseEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.EXPENSE,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

/**
 * Unlike every other `toEntity`, this one takes the [existing] local row (if
 * any) so the receipt-upload phase's local-only state
 * (localReceiptPath/receiptSyncState/receiptSyncError — see
 * ExpenseEntity's doc comment) survives being overwritten by wire data,
 * which knows nothing about it. `receiptImage` from the wire becomes the
 * new [ExpenseEntity.receiptUrl] and — since its presence means a photo is
 * now confirmed uploaded (from this device or another) — also resolves any
 * pending/failed local receipt-sync state to UPLOADED.
 */
fun ExpenseFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
    existing: ExpenseEntity? = null,
) = ExpenseEntity(
    id = id,
    jobId = jobId,
    supplierId = supplierId,
    category = category,
    description = description,
    amount = amount,
    isVatApplicable = isVatApplicable,
    vatAmount = vatAmount,
    date = date,
    receiptUrl = receiptImage ?: existing?.receiptUrl,
    localReceiptPath = existing?.localReceiptPath,
    receiptSyncState = if (receiptImage != null) ReceiptSyncState.UPLOADED else existing?.receiptSyncState ?: ReceiptSyncState.NONE,
    receiptSyncError = if (receiptImage != null) null else existing?.receiptSyncError,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Employee --------------------------------------------------------------

fun EmployeeEntity.toFieldsDto() = EmployeeFieldsDto(
    name = name,
    role = role,
    phone = phone,
    email = email,
    payRateType = payRateType,
    payRate = payRate,
    startDate = startDate,
    notes = notes,
)

fun EmployeeEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.EMPLOYEE,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun EmployeeFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = EmployeeEntity(
    id = id,
    name = name,
    role = role,
    phone = phone,
    email = email,
    payRateType = payRateType,
    payRate = payRate,
    startDate = startDate,
    notes = notes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- Payslip -----------------------------------------------------------------

fun PayslipEntity.toFieldsDto() = PayslipFieldsDto(
    employeeId = employeeId,
    periodStart = periodStart,
    periodEnd = periodEnd,
    grossPay = grossPay,
    deductions = deductions,
    deductionsNote = deductionsNote,
    paidDate = paidDate,
    notes = notes,
    // netPay is read-only on the wire — never sent, see API_CONTRACT.md.
)

fun PayslipEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.PAYSLIP,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun PayslipFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = PayslipEntity(
    id = id,
    employeeId = employeeId,
    periodStart = periodStart,
    periodEnd = periodEnd,
    grossPay = grossPay,
    deductions = deductions,
    deductionsNote = deductionsNote,
    netPay = netPay,
    paidDate = paidDate,
    notes = notes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

// ---- ComplianceItem ------------------------------------------------------

fun ComplianceItemEntity.toFieldsDto() = ComplianceItemFieldsDto(
    category = category,
    title = title,
    dueDate = dueDate,
    completedDate = completedDate,
    isRecurring = isRecurring,
    notes = notes,
)

fun ComplianceItemEntity.toSyncChange(json: Json) = SyncChangeDto(
    model = SyncModelKeys.COMPLIANCE_ITEM,
    id = id,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    fields = json.encodeToJsonElement(toFieldsDto()),
)

fun ComplianceItemFieldsDto.toEntity(
    id: String,
    updatedAt: String,
    deletedAt: String?,
    syncState: String,
    syncError: String? = null,
    conflictServerJson: String? = null,
) = ComplianceItemEntity(
    id = id,
    category = category,
    title = title,
    dueDate = dueDate,
    completedDate = completedDate,
    isRecurring = isRecurring,
    notes = notes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncState = syncState,
    syncError = syncError,
    conflictServerJson = conflictServerJson,
)

/** Human-readable summary of a sync `error` result's field errors, for [SyncState.FAILED]'s syncError column. */
fun Map<String, List<String>>?.toSyncErrorMessage(): String {
    if (this.isNullOrEmpty()) return "The server rejected this change."
    return entries.joinToString("; ") { (field, messages) -> "$field: ${messages.joinToString(", ")}" }
}
