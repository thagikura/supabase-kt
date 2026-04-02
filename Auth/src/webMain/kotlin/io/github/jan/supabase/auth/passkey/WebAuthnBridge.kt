@file:OptIn(ExperimentalWasmJsInterop::class)
package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

/**
 * Minimal platform bridge for WebAuthn operations.
 *
 * These two functions encapsulate the entire browser WebAuthn ceremony:
 * - Parse the server's JSON options (converting base64url fields to ArrayBuffer)
 * - Call navigator.credentials.create/get
 * - Serialize the credential response back to JSON (converting ArrayBuffer to base64url)
 *
 * The JavaScript logic is identical for JS and WasmJS but uses different interop
 * mechanisms (js() vs @JsFun). All other credential handler logic is shared in webMain.
 *
 * @param optionsJson The raw JSON string from the server's challenge response
 * @return A Promise that resolves to the serialized credential response JSON string
 */

@SupabaseInternal
internal expect fun webAuthnCreateCredential(optionsJson: String): Promise<JsString>

@SupabaseInternal
internal expect fun webAuthnGetCredential(optionsJson: String): Promise<JsString>
