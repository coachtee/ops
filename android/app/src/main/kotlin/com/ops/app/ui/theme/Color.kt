package com.ops.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * OPS Design System v3 palette — blue-first, per the Phase 3 product
 * direction brief. Every business-management screen's primary action and
 * brand accent is this one blue; every other color is a neutral until a
 * status needs to speak, and even then it means one specific thing and
 * nothing else:
 *
 * - primary (blue) = the product's own interaction color — the one filled
 *   button on a screen, links, selection state. Never used for "this
 *   succeeded" — that's tertiary/green.
 * - tertiary (green) = success/positive outcome only (paid, accepted,
 *   completed). Restrained on purpose: v2's mistake was making green do
 *   double duty as both brand AND success, which is why almost everything
 *   ended up green. It no longer carries any brand meaning at all.
 * - secondary (amber) = needs-attention / warning.
 * - error (red) = failed/critical/danger.
 *
 * A category label, a default chip, or any other non-status UI stays
 * ink-on-neutral — color is reserved for the four meanings above.
 */
val OpsBlue40 = Color(0xFF0B5FA8)
val OpsBlue80 = Color(0xFF9FCBFF)
val OpsBlue90 = Color(0xFFD3E4FF)
val OpsBlue10 = Color(0xFF001C38)
val OpsBlue20 = Color(0xFF00325A)

/** Success accent only (paid/accepted/completed) — not brand, not primary. */
val OpsGreen40 = Color(0xFF15824B)
val OpsGreen80 = Color(0xFF7FD8A6)
val OpsGreen90 = Color(0xFFC5F0D6)
val OpsGreen10 = Color(0xFF00210F)
val OpsGreen20 = Color(0xFF00391D)

val OpsAmber40 = Color(0xFFB4770E) // outstanding money / needs-attention accent
val OpsAmber90 = Color(0xFFFFDDB1)
val OpsAmber20 = Color(0xFF3A2D14)

val OpsRed40 = Color(0xFFBA1A1A) // failed/conflict sync states
val OpsRed90 = Color(0xFFFFDAD6)

val OpsNeutral10 = Color(0xFF1A1C1E) // near-black/charcoal text
val OpsNeutral90 = Color(0xFFE2E2E5)
val OpsNeutral99 = Color(0xFFFCFCFF) // near-white background

/** Card/list-row outline — a flat 1dp border instead of Material's default
 * elevation shadow, calmer on a phone screen at list-scroll speed. Cool
 * neutral grey, not warmed by the old green-tinted palette. */
val OpsBorderLight = Color(0xFFDDE1E6)
val OpsBorderDark = Color(0xFF2B2F33)

/** Sunken surface for code/receipt-preview backdrops and the status-pill
 * "neutral" state — one shade off the page background, not a new hue. */
val OpsSurfaceSunkenLight = Color(0xFFF0F2F5)
val OpsSurfaceSunkenDark = Color(0xFF1D2024)
