package com.ops.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** One entry of `POST /api/sync/push/`'s `changes` array, or of
 * `GET /api/sync/pull/`'s `changes` array in the response. `fields` is kept
 * as a generic [JsonElement] here because its shape depends on [model] — see
 * API_CONTRACT.md's "Model field payloads" section and the per-model
 * `*FieldsDto` classes in this package, which are what actually get encoded
 * into / decoded out of this field (via [com.ops.app.data.sync.SyncCodec]). */
@Serializable
data class SyncChangeDto(
    val model: String,
    val id: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    val fields: JsonElement,
)

@Serializable
data class SyncPushRequestDto(
    val changes: List<SyncChangeDto>,
)

/** `status` is one of `"accepted" | "conflict" | "error"` — see API_CONTRACT.md. */
@Serializable
data class SyncResultDto(
    val model: String,
    val id: String,
    val status: String,
    @SerialName("server_record") val serverRecord: JsonElement? = null,
    val errors: Map<String, List<String>>? = null,
)

@Serializable
data class SyncPushResponseDto(
    val results: List<SyncResultDto>,
)

@Serializable
data class SyncPullResponseDto(
    @SerialName("server_time") val serverTime: String,
    val changes: List<SyncChangeDto>,
)
