package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import io.ktor.util.PlatformUtils.IS_BROWSER

/**
 * Creates a [PasskeyCredentialHandler] for browser environments.
 * Returns null in non-browser (Node.js) environments.
 */
@SupabaseInternal
fun createBrowserPasskeyCredentialHandler(): PasskeyCredentialHandler? {
    return if (IS_BROWSER) createPlatformPasskeyCredentialHandler() else null
}

@SupabaseInternal
internal expect fun createPlatformPasskeyCredentialHandler(): PasskeyCredentialHandler
