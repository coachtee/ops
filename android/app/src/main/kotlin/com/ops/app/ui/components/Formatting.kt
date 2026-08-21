package com.ops.app.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** ZAR display formatting — "R 1,250.00". Currency is always ZAR in this
 * app (see API_CONTRACT.md, "Currency is always ZAR — not sent per record"). */
fun formatZar(amount: BigDecimal): String {
    val scaled = amount.setScale(2, RoundingMode.HALF_UP)
    val negative = scaled.signum() < 0
    val plain = scaled.abs().toPlainString()
    val (whole, cents) = plain.split(".")
    val grouped = whole.reversed().chunked(3).joinToString(",").reversed()
    return (if (negative) "-R " else "R ") + grouped + "." + cents
}

fun formatZar(amountText: String): String = formatZar(runCatching { BigDecimal(amountText) }.getOrDefault(BigDecimal.ZERO))

private val DISPLAY_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")

/** `YYYY-MM-DD` -> "21 Aug 2026"; blank/null/unparseable -> the fallback text. */
fun formatDate(isoDate: String?, fallback: String = "—"): String {
    if (isoDate.isNullOrBlank()) return fallback
    return try {
        LocalDate.parse(isoDate).format(DISPLAY_DATE)
    } catch (e: DateTimeParseException) {
        fallback
    }
}
