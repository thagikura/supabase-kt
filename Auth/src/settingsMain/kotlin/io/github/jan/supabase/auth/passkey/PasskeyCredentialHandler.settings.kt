package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlinx.serialization.json.JsonObject

@SupabaseInternal
actual fun createPasskeyCredentialHandler(): PasskeyCredentialHandler = UnsupportedPasskeyCredentialHandler()

private class UnsupportedPasskeyCredentialHandler : PasskeyCredentialHandler {

    override suspend fun create(options: JsonObject): JsonObject {
        throw UnsupportedOperationException("Passkey registration is not yet supported on this platform")
    }

    override suspend fun get(options: JsonObject): JsonObject {
        throw UnsupportedOperationException("Passkey authentication is not yet supported on this platform")
    }
}
