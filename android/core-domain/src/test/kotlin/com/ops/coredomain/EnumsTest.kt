package com.ops.coredomain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Locks the exact wire strings against the Django `choices` values in
 * backend/{crm,sales,work,finance}/models.py. If one of these ever fails,
 * either this enum or the backend model drifted — check both sides.
 */
class EnumsTest {

    @Test
    fun `lead source wire values match crm models Lead SOURCE_CHOICES`() {
        assertEquals(
            listOf("whatsapp", "call", "facebook", "website", "email", "referral", "walkin", "tender", "other"),
            LeadSource.entries.map { it.wire },
        )
    }

    @Test
    fun `lead status wire values match crm models Lead STATUS_CHOICES`() {
        assertEquals(
            listOf("new", "contacted", "quoted", "converted", "lost"),
            LeadStatus.entries.map { it.wire },
        )
    }

    @Test
    fun `customer type wire values match crm models Customer TYPE_CHOICES`() {
        assertEquals(listOf("individual", "company"), CustomerType.entries.map { it.wire })
    }

    @Test
    fun `quote status wire values match sales models Quote STATUS_CHOICES`() {
        assertEquals(
            listOf("draft", "sent", "accepted", "declined", "expired"),
            QuoteStatus.entries.map { it.wire },
        )
    }

    @Test
    fun `job status wire values match work models Job STATUS_CHOICES`() {
        assertEquals(
            listOf("not_started", "in_progress", "completed", "cancelled"),
            JobStatus.entries.map { it.wire },
        )
    }

    @Test
    fun `invoice status wire values match finance models Invoice STATUS_CHOICES`() {
        assertEquals(
            listOf("draft", "sent", "partially_paid", "paid", "overdue", "cancelled"),
            InvoiceStatus.entries.map { it.wire },
        )
    }

    @Test
    fun `payment method wire values match finance models Payment METHOD_CHOICES`() {
        assertEquals(listOf("cash", "eft", "card", "snapscan", "other"), PaymentMethod.entries.map { it.wire })
    }

    @Test
    fun `expense category wire values match finance models Expense CATEGORY_CHOICES`() {
        assertEquals(
            listOf(
                "materials_stock", "fuel_travel", "tools_equipment", "rent", "utilities",
                "insurance", "bank_charges", "professional_fees", "marketing",
                "telephone_internet", "vehicle", "repairs_maintenance", "wages_subcontractors", "other",
            ),
            ExpenseCategory.entries.map { it.wire },
        )
    }

    @Test
    fun `pay rate type wire values match people models Employee PAY_RATE_TYPE_CHOICES`() {
        assertEquals(listOf("hourly", "daily", "monthly"), PayRateType.entries.map { it.wire })
    }

    @Test
    fun `compliance category wire values match compliance models ComplianceItem CATEGORY_CHOICES`() {
        assertEquals(
            listOf("vat_return", "paye_uif_sdl", "provisional_tax", "cipc_annual_return", "other"),
            ComplianceCategory.entries.map { it.wire },
        )
    }

    @Test
    fun `fromWire round trips every enum value`() {
        LeadSource.entries.forEach { assertEquals(it, LeadSource.fromWire(it.wire)) }
        LeadStatus.entries.forEach { assertEquals(it, LeadStatus.fromWire(it.wire)) }
        CustomerType.entries.forEach { assertEquals(it, CustomerType.fromWire(it.wire)) }
        QuoteStatus.entries.forEach { assertEquals(it, QuoteStatus.fromWire(it.wire)) }
        JobStatus.entries.forEach { assertEquals(it, JobStatus.fromWire(it.wire)) }
        InvoiceStatus.entries.forEach { assertEquals(it, InvoiceStatus.fromWire(it.wire)) }
        PaymentMethod.entries.forEach { assertEquals(it, PaymentMethod.fromWire(it.wire)) }
        ExpenseCategory.entries.forEach { assertEquals(it, ExpenseCategory.fromWire(it.wire)) }
        PayRateType.entries.forEach { assertEquals(it, PayRateType.fromWire(it.wire)) }
        ComplianceCategory.entries.forEach { assertEquals(it, ComplianceCategory.fromWire(it.wire)) }
    }

    @Test
    fun `fromWire rejects an unknown value instead of guessing`() {
        assertThrows(IllegalArgumentException::class.java) { LeadStatus.fromWire("bogus") }
    }
}
