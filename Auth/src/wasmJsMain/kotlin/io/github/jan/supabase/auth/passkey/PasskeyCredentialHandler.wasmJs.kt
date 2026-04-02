@file:OptIn(ExperimentalWasmJsInterop::class)
package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

// External declarations for functions defined in webauthn-bridge.js (loaded from resources).
// WasmJS uses JsString parameters instead of String for external JS function calls.

@JsFun("(optionsJson) => supabaseWebAuthnCreate(optionsJson)")
private external fun wasmWebAuthnCreate(optionsJson: JsString): Promise<JsString>

@JsFun("(optionsJson) => supabaseWebAuthnGet(optionsJson)")
private external fun wasmWebAuthnGet(optionsJson: JsString): Promise<JsString>

@SupabaseInternal
internal actual fun webAuthnCreateCredential(optionsJson: String): Promise<JsString> =
    wasmWebAuthnCreate(optionsJson.toJsString())

@SupabaseInternal
internal actual fun webAuthnGetCredential(optionsJson: String): Promise<JsString> =
    wasmWebAuthnGet(optionsJson.toJsString())
