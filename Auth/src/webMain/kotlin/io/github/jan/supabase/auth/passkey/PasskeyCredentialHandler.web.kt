package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.util.PlatformUtils.IS_BROWSER
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Creates a [PasskeyCredentialHandler] for browser environments.
 * Returns null in non-browser (Node.js) environments.
 */
@SupabaseInternal
fun createBrowserPasskeyCredentialHandler(): PasskeyCredentialHandler? {
    return if (IS_BROWSER) BrowserPasskeyCredentialHandler() else null
}

@OptIn(ExperimentalWasmJsInterop::class)
internal class BrowserPasskeyCredentialHandler : PasskeyCredentialHandler {

    override suspend fun create(options: JsonObject): JsonObject {
        val resultJson = webAuthnCreateCredential(options.toString()).await()
        return Json.parseToJsonElement(resultJson.toString()) as JsonObject
    }

    override suspend fun get(options: JsonObject): JsonObject {
        val resultJson = webAuthnGetCredential(options.toString()).await()
        return Json.parseToJsonElement(resultJson.toString()) as JsonObject
    }
}
