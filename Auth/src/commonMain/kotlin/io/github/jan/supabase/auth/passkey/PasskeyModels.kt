package io.github.jan.supabase.auth.passkey

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Represents a registered passkey for a user.
 */
@Serializable
data class Passkey(
    val id: String,
    @SerialName("friendly_name") val friendlyName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_used_at") val lastUsedAt: String? = null,
)

@Serializable
internal data class PasskeyChallengeResponse(
    @SerialName("challenge_id") val challengeId: String,
    val options: JsonObject,
    @SerialName("expires_at") val expiresAt: Long,
)

@Serializable
internal data class PasskeyVerifyRequest(
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("credential_response") val credentialResponse: JsonObject,
)

@Serializable
internal data class PasskeyUpdateRequest(
    @SerialName("friendly_name") val friendlyName: String,
)
