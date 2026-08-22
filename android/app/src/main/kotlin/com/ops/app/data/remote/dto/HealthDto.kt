package com.ops.app.data.remote.dto

import kotlinx.serialization.Serializable

/** `GET /api/health/` — see docs/API_CONTRACT.md's "Health check" section.
 * Infrastructure reachability only, never a substitute for real UAT. */
@Serializable
data class HealthResponseDto(
    val status: String = "",
    val service: String = "",
    val database: String = "",
)
