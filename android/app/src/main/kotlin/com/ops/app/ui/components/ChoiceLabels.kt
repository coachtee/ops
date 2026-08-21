package com.ops.app.ui.components

import com.ops.coredomain.CustomerType
import com.ops.coredomain.ExpenseCategory
import com.ops.coredomain.InvoiceStatus
import com.ops.coredomain.JobStatus
import com.ops.coredomain.LeadSource
import com.ops.coredomain.LeadStatus
import com.ops.coredomain.PaymentMethod
import com.ops.coredomain.QuoteStatus

/** Display labels for every core-domain choice enum, paired with the wire
 * value each [com.ops.app.ui.components.LabeledDropdown] needs. Kept
 * separate from core-domain itself, which only owns the wire values (see
 * API_CONTRACT.md) — these labels are presentation, not contract. */

val LEAD_SOURCE_CHOICES = listOf(
    LeadSource.WHATSAPP.wire to "WhatsApp",
    LeadSource.CALL.wire to "Phone call",
    LeadSource.FACEBOOK.wire to "Facebook",
    LeadSource.WEBSITE.wire to "Website",
    LeadSource.EMAIL.wire to "Email",
    LeadSource.REFERRAL.wire to "Referral",
    LeadSource.WALKIN.wire to "Walk-in",
    LeadSource.TENDER.wire to "Tender / RFQ",
    LeadSource.OTHER.wire to "Other",
)

val LEAD_STATUS_CHOICES = listOf(
    LeadStatus.NEW.wire to "New",
    LeadStatus.CONTACTED.wire to "Contacted",
    LeadStatus.QUOTED.wire to "Quoted",
    LeadStatus.CONVERTED.wire to "Converted",
    LeadStatus.LOST.wire to "Lost",
)

val CUSTOMER_TYPE_CHOICES = listOf(
    CustomerType.INDIVIDUAL.wire to "Individual",
    CustomerType.COMPANY.wire to "Company",
)

val QUOTE_STATUS_CHOICES = listOf(
    QuoteStatus.DRAFT.wire to "Draft",
    QuoteStatus.SENT.wire to "Sent",
    QuoteStatus.ACCEPTED.wire to "Accepted",
    QuoteStatus.DECLINED.wire to "Declined",
    QuoteStatus.EXPIRED.wire to "Expired",
)

val JOB_STATUS_CHOICES = listOf(
    JobStatus.NOT_STARTED.wire to "Not started",
    JobStatus.IN_PROGRESS.wire to "In progress",
    JobStatus.COMPLETED.wire to "Completed",
    JobStatus.CANCELLED.wire to "Cancelled",
)

val INVOICE_STATUS_CHOICES = listOf(
    InvoiceStatus.DRAFT.wire to "Draft",
    InvoiceStatus.SENT.wire to "Sent",
    InvoiceStatus.PARTIALLY_PAID.wire to "Partially paid",
    InvoiceStatus.PAID.wire to "Paid",
    InvoiceStatus.OVERDUE.wire to "Overdue",
    InvoiceStatus.CANCELLED.wire to "Cancelled",
)

val PAYMENT_METHOD_CHOICES = listOf(
    PaymentMethod.CASH.wire to "Cash",
    PaymentMethod.EFT.wire to "EFT",
    PaymentMethod.CARD.wire to "Card",
    PaymentMethod.SNAPSCAN.wire to "SnapScan",
    PaymentMethod.OTHER.wire to "Other",
)

val EXPENSE_CATEGORY_CHOICES = listOf(
    ExpenseCategory.MATERIALS_STOCK.wire to "Materials & stock",
    ExpenseCategory.FUEL_TRAVEL.wire to "Fuel & travel",
    ExpenseCategory.TOOLS_EQUIPMENT.wire to "Tools & equipment",
    ExpenseCategory.RENT.wire to "Rent",
    ExpenseCategory.UTILITIES.wire to "Utilities",
    ExpenseCategory.INSURANCE.wire to "Insurance",
    ExpenseCategory.BANK_CHARGES.wire to "Bank charges",
    ExpenseCategory.PROFESSIONAL_FEES.wire to "Professional fees",
    ExpenseCategory.MARKETING.wire to "Marketing & advertising",
    ExpenseCategory.TELEPHONE_INTERNET.wire to "Telephone & internet",
    ExpenseCategory.VEHICLE.wire to "Vehicle expenses",
    ExpenseCategory.REPAIRS_MAINTENANCE.wire to "Repairs & maintenance",
    ExpenseCategory.WAGES_SUBCONTRACTORS.wire to "Wages & subcontractors",
    ExpenseCategory.OTHER.wire to "Other",
)

fun labelFor(choices: List<Pair<String, String>>, wireValue: String): String =
    choices.firstOrNull { it.first == wireValue }?.second ?: wireValue

/** Mirrors accounts/models.py PROVINCE_CHOICES exactly (wire value to
 * label). Shared by business setup/profile AND customer address forms. */
val PROVINCE_CHOICES = listOf(
    "EC" to "Eastern Cape",
    "FS" to "Free State",
    "GP" to "Gauteng",
    "KZN" to "KwaZulu-Natal",
    "LP" to "Limpopo",
    "MP" to "Mpumalanga",
    "NC" to "Northern Cape",
    "NW" to "North West",
    "WC" to "Western Cape",
)
