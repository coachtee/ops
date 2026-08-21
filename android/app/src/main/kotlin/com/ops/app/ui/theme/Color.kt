package com.ops.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * OPS Design System v2 palette — see the "OPS Product Reset" design doc
 * (product review + design system, reviewed before this file changed).
 * One brand green, matching the "O" launcher mark; every other color is a
 * neutral until a status needs to speak — sync state, an overdue amount, a
 * failure. Never used decoratively (a category label, a default chip, a
 * non-status icon all stay ink-on-neutral). Kept to system-default
 * typography and this one accent family — still not a heavy design system,
 * just a complete one instead of a five-style stub.
 */
val OpsGreen40 = Color(0xFF0B6E4F)
val OpsGreen80 = Color(0xFF7FD8B2)
val OpsGreen90 = Color(0xFFC7F0DC)
val OpsGreen10 = Color(0xFF00210F)
val OpsGreen20 = Color(0xFF00391D)

val OpsAmber40 = Color(0xFFB4770E) // outstanding money / needs-attention accent
val OpsAmber90 = Color(0xFFFFDDB1)
val OpsAmber20 = Color(0xFF3A2D14)

val OpsRed40 = Color(0xFFBA1A1A) // failed/conflict sync states
val OpsRed90 = Color(0xFFFFDAD6)

val OpsNeutral10 = Color(0xFF191C1A)
val OpsNeutral90 = Color(0xFFE1E3DF)
val OpsNeutral99 = Color(0xFFFBFDF8)

/** Card/list-row outline — a flat 1dp border instead of Material's default
 * elevation shadow, calmer on a phone screen at list-scroll speed. */
val OpsBorderLight = Color(0xFFDEE5DF)
val OpsBorderDark = Color(0xFF2B342E)

/** Sunken surface for code/receipt-preview backdrops and the status-pill
 * "neutral" state — one shade off the page background, not a new hue. */
val OpsSurfaceSunkenLight = Color(0xFFF1F4F0)
val OpsSurfaceSunkenDark = Color(0xFF1D251F)
