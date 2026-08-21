package com.ops.coredomain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

/**
 * Mirrors backend/tests/test_money.py case-for-case so the Android client's
 * offline totals can never silently drift from what the server considers
 * canonical.
 */
class MoneyTest {

    @Test
    fun `line total multiplies and rounds to cents, half-up`() {
        // Python: compute_line_total("2", "950.005") == Decimal("1900.01")
        val result = Money.computeLineTotal(BigDecimal("2"), BigDecimal("950.005"))
        assertEquals(BigDecimal("1900.01"), result)
    }

    @Test
    fun `document totals with vat`() {
        val totals = Money.computeDocumentTotals(
            lineTotals = listOf(BigDecimal("100.00"), BigDecimal("50.00")),
            discountAmount = BigDecimal("0"),
            isVatApplicable = true,
        )
        assertEquals(BigDecimal("150.00"), totals.subtotal)
        assertEquals(BigDecimal("22.50"), totals.vatAmount) // 15% of 150
        assertEquals(BigDecimal("172.50"), totals.total)
    }

    @Test
    fun `document totals without vat`() {
        val totals = Money.computeDocumentTotals(
            lineTotals = listOf(BigDecimal("100.00")),
            discountAmount = BigDecimal("0"),
            isVatApplicable = false,
        )
        assertEquals(BigDecimal("0.00"), totals.vatAmount)
        assertEquals(BigDecimal("100.00"), totals.total)
    }

    @Test
    fun `discount applied before vat`() {
        val totals = Money.computeDocumentTotals(
            lineTotals = listOf(BigDecimal("1000.00")),
            discountAmount = BigDecimal("100.00"),
            isVatApplicable = true,
        )
        assertEquals(BigDecimal("1000.00"), totals.subtotal)
        // taxable = 1000 - 100 = 900; vat = 135.00
        assertEquals(BigDecimal("135.00"), totals.vatAmount)
        assertEquals(BigDecimal("1035.00"), totals.total)
    }

    @Test
    fun `discount larger than subtotal never goes negative`() {
        val totals = Money.computeDocumentTotals(
            lineTotals = listOf(BigDecimal("50.00")),
            discountAmount = BigDecimal("500.00"),
            isVatApplicable = true,
        )
        assertEquals(BigDecimal("0.00"), totals.vatAmount)
        assertEquals(BigDecimal("0.00"), totals.total)
    }

    @Test
    fun `empty line items totals zero`() {
        val totals = Money.computeDocumentTotals(
            lineTotals = emptyList(),
            discountAmount = BigDecimal("0"),
            isVatApplicable = true,
        )
        assertEquals(BigDecimal("0.00"), totals.subtotal)
        assertEquals(BigDecimal("0.00"), totals.vatAmount)
        assertEquals(BigDecimal("0.00"), totals.total)
    }

    @Test
    fun `vat rate is the flat 15 percent SA rate`() {
        assertEquals(BigDecimal("0.15"), Money.VAT_RATE)
    }

    @Test
    fun `quantize rounds half up not half even`() {
        // 0.005 half-up must round to 0.01 (a naive HALF_EVEN would give 0.00)
        assertEquals(BigDecimal("0.01"), Money.quantize(BigDecimal("0.005")))
        assertEquals(BigDecimal("2.35"), Money.quantize(BigDecimal("2.345")))
    }

    @Test
    fun `line total handles fractional quantities`() {
        // 2.5 hours at R450.00 per hour
        val result = Money.computeLineTotal(BigDecimal("2.5"), BigDecimal("450.00"))
        assertEquals(BigDecimal("1125.00"), result)
    }
}

/**
 * Mirrors backend/tests/test_money.py's VatInclusiveExtractionTests
 * case-for-case. Expenses run VAT the opposite direction from
 * quotes/invoices — see Money.extractVatFromInclusive's doc comment.
 */
class VatInclusiveExtractionTest {

    @Test
    fun `clean multiple of 115 extracts exactly`() {
        // R115 inclusive = R100 exclusive + R15 VAT, the textbook case.
        assertEquals(BigDecimal("15.00"), Money.extractVatFromInclusive(BigDecimal("115.00"), true))
    }

    @Test
    fun `another clean case`() {
        assertEquals(BigDecimal("30.00"), Money.extractVatFromInclusive(BigDecimal("230.00"), true))
    }

    @Test
    fun `rounds half up on an unclean division`() {
        // 100 * 15/115 = 13.0434... -> 13.04
        assertEquals(BigDecimal("13.04"), Money.extractVatFromInclusive(BigDecimal("100.00"), true))
        // 50 * 15/115 = 6.5217... -> 6.52
        assertEquals(BigDecimal("6.52"), Money.extractVatFromInclusive(BigDecimal("50.00"), true))
    }

    @Test
    fun `not vat applicable is always zero`() {
        assertEquals(BigDecimal("0.00"), Money.extractVatFromInclusive(BigDecimal("1000.00"), false))
    }

    @Test
    fun `zero amount extracts zero`() {
        assertEquals(BigDecimal("0.00"), Money.extractVatFromInclusive(BigDecimal("0.00"), true))
    }
}
