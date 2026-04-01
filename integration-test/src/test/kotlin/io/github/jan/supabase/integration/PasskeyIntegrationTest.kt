package io.github.jan.supabase.integration

import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Integration tests for the Passkey (WebAuthn) API.
 *
 * Requirements:
 * - GoTrue with passkeys enabled via env vars on the auth container:
 *   GOTRUE_PASSKEY_ENABLED=true
 *   GOTRUE_WEBAUTHN_RP_ID=localhost
 *   GOTRUE_WEBAUTHN_RP_ORIGINS=http://localhost:54321
 *   GOTRUE_WEBAUTHN_RP_DISPLAY_NAME=supabase-kt-tests
 *
 * These tests verify the server-side API contract for passkey management.
 * Full WebAuthn ceremony tests (register + sign in) require a browser
 * authenticator and are not possible in a headless CI environment.
 */
class PasskeyIntegrationTest : IntegrationTestBase() {

    @Test
    fun testListPasskeysForNewUser() = runTest {
        val client = createAuthenticatedClient()
        val passkeys = client.auth.passkeys.list()
        assertTrue(passkeys.isEmpty(), "New user should have no passkeys")
    }

    @Test
    fun testDeleteNonExistentPasskey() = runTest {
        val client = createAuthenticatedClient()
        assertFailsWith<RestException> {
            client.auth.passkeys.delete("00000000-0000-0000-0000-000000000000")
        }
    }

    @Test
    fun testUpdateNonExistentPasskey() = runTest {
        val client = createAuthenticatedClient()
        assertFailsWith<RestException> {
            client.auth.passkeys.update("00000000-0000-0000-0000-000000000000", "New Name")
        }
    }
}
