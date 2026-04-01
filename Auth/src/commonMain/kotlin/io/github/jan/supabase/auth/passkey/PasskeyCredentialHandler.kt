package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlinx.serialization.json.JsonObject

/**
 * Platform-specific handler for WebAuthn credential ceremonies.
 * Each platform provides an implementation that bridges to the native WebAuthn API.
 */
@SupabaseInternal
interface PasskeyCredentialHandler {

    /**
     * Creates a new public key credential (registration ceremony).
     * @param options The PublicKeyCredentialCreationOptions JSON from the server
     * @return The credential response JSON to send back to the server
     */
    suspend fun create(options: JsonObject): JsonObject

    /**
     * Gets an existing public key credential (authentication ceremony).
     * @param options The PublicKeyCredentialRequestOptions JSON from the server
     * @return The assertion response JSON to send back to the server
     */
    suspend fun get(options: JsonObject): JsonObject
}
