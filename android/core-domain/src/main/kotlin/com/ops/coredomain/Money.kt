package com.ops.coredomain

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Money math shared by quotes and invoices. Mirrors backend/common/money.py
 * EXACTLY — same rounding mode, same order of operations — because the
 * Android app computes these totals locally for instant offline UI, and the
 * numbers must match what the server recomputes as canonical on sync. See
 * docs/API_CONTRACT.md ("subtotal/vat_amount/total ... are always recomputed
 * server-side ... a client may compute them locally for instant offline UI").
 *
 * Everything is [BigDecimal], never Double/Float — money is never a binary
 * float in this codebase, on either side of the wire.
 */
object Money {

    /** Current flat South African VAT rate. Server-configurable per business
     * only via the is_vat_applicable flag on a document, not a variable rate. */
    val VAT_RATE: BigDecimal = BigDecimal("0.15")

    private val CENTS = BigDecimal("0.01")
    private val ZERO = BigDecimal("0.00")

    /** Quantize to 2 decimal places, half-up — same as Python's
     * `Decimal(value).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)`. */
    fun quantize(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

    /** A single quote/invoice line item's total: quantity * unit price, quantized to cents. */
    fun computeLineTotal(quantity: BigDecimal, unitPrice: BigDecimal): BigDecimal =
        quantize(quantity.multiply(unitPrice))

    /**
     * Whole-document totals from its line items. Discount is applied to the
     * subtotal BEFORE VAT; the taxable amount is never allowed to go
     * negative (a discount larger than the subtotal simply zeroes out the
     * taxable amount, it does not produce a negative VAT/total). VAT is a
     * flat 0.00 when the document is not VAT-applicable at all.
     */
    fun computeDocumentTotals(
        lineTotals: List<BigDecimal>,
        discountAmount: BigDecimal,
        isVatApplicable: Boolean,
    ): DocumentTotals {
        val subtotal = quantize(lineTotals.fold(BigDecimal.ZERO) { acc, t -> acc.add(t) })
        val discount = quantize(discountAmount)
        var taxable = subtotal.subtract(discount)
        if (taxable.signum() < 0) {
            taxable = ZERO
        }
        val vatAmount = if (isVatApplicable) quantize(taxable.multiply(VAT_RATE)) else ZERO
        val total = quantize(taxable.add(vatAmount))
        return DocumentTotals(subtotal = subtotal, vatAmount = vatAmount, total = total)
    }
}

/** subtotal / vatAmount / total for a quote or invoice, all quantized to 2dp. */
data class DocumentTotals(
    val subtotal: BigDecimal,
    val vatAmount: BigDecimal,
    val total: BigDecimal,
)
