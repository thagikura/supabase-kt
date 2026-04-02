@file:OptIn(ExperimentalWasmJsInterop::class)
package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

// External declarations for functions defined in webauthn-bridge.js (loaded from resources).

@JsName("supabaseWebAuthnCreate")
private external fun jsWebAuthnCreate(optionsJson: String): Promise<JsString>

@JsName("supabaseWebAuthnGet")
private external fun jsWebAuthnGet(optionsJson: String): Promise<JsString>

@SupabaseInternal
internal actual fun webAuthnCreateCredential(optionsJson: String): Promise<JsString> =
    jsWebAuthnCreate(optionsJson)

@SupabaseInternal
internal actual fun webAuthnGetCredential(optionsJson: String): Promise<JsString> =
    jsWebAuthnGet(optionsJson)
