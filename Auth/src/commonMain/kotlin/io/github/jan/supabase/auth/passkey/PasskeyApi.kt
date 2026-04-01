package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionSource
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.network.SupabaseHttpClient
import io.github.jan.supabase.safeBody
import kotlinx.serialization.json.buildJsonObject

/**
 * API for managing passkeys (WebAuthn) for authentication.
 *
 * Passkeys provide passwordless sign-in using biometrics (Face ID, fingerprint, security keys).
 * This API supports both registering new passkeys and signing in with existing ones.
 */
interface PasskeyApi {

    /**
     * Signs in using a passkey. This performs the full WebAuthn authentication ceremony:
     * 1. Fetches authentication options from the server
     * 2. Invokes the platform credential handler to get the assertion
     * 3. Verifies the assertion with the server
     * 4. Imports the resulting session
     *
     * @param saveSession Whether to import the session into Auth after verification
     * @return The resulting [UserSession]
     */
    suspend fun signIn(saveSession: Boolean = true): UserSession

    /**
     * Registers a new passkey for the currently authenticated user.
     * 1. Fetches registration options from the server
     * 2. Invokes the platform credential handler to create a credential
     * 3. Verifies the credential with the server
     *
     * @param friendlyName Optional human-readable name for the passkey (e.g., "MacBook Pro")
     * @return The registered [Passkey] metadata
     */
    suspend fun register(friendlyName: String? = null): Passkey

    /**
     * Lists all passkeys for the currently authenticated user.
     */
    suspend fun list(): List<Passkey>

    /**
     * Updates the friendly name of a passkey.
     * @param passkeyId The ID of the passkey to update
     * @param friendlyName The new friendly name
     * @return The updated [Passkey]
     */
    suspend fun update(passkeyId: String, friendlyName: String): Passkey

    /**
     * Deletes a passkey.
     * @param passkeyId The ID of the passkey to delete
     */
    suspend fun delete(passkeyId: String)
}

@PublishedApi
internal class PasskeyApiImpl(
    private val authenticatedApi: SupabaseHttpClient,
    private val publicApi: SupabaseHttpClient,
    private val auth: Auth,
    internal val credentialHandler: PasskeyCredentialHandler?
) : PasskeyApi {

    private fun requireHandler(): PasskeyCredentialHandler =
        credentialHandler ?: error("Passkey authentication is not supported on this platform. Set passkeyCredentialHandler in AuthConfig to provide a handler.")

    override suspend fun signIn(saveSession: Boolean): UserSession {
        val handler = requireHandler()
        val challenge: PasskeyChallengeResponse =
            publicApi.postJson("passkeys/authentication/options", buildJsonObject { }).safeBody()
        val credentialResponse = handler.get(challenge.options)
        val session: UserSession = publicApi.postJson(
            "passkeys/authentication/verify",
            PasskeyVerifyRequest(
                challengeId = challenge.challengeId,
                credentialResponse = credentialResponse
            )
        ).safeBody()
        if (saveSession) {
            auth.importSession(session, source = SessionSource.PasskeySignIn)
        }
        return session
    }

    override suspend fun register(friendlyName: String?): Passkey {
        val handler = requireHandler()
        val challenge: PasskeyChallengeResponse =
            authenticatedApi.postJson("passkeys/registration/options", buildJsonObject { }).safeBody()
        val credentialResponse = handler.create(challenge.options)
        return authenticatedApi.postJson(
            "passkeys/registration/verify",
            PasskeyVerifyRequest(
                challengeId = challenge.challengeId,
                credentialResponse = credentialResponse
            )
        ).safeBody()
    }

    override suspend fun list(): List<Passkey> {
        return authenticatedApi.get("passkeys").safeBody()
    }

    override suspend fun update(passkeyId: String, friendlyName: String): Passkey {
        return authenticatedApi.patchJson(
            "passkeys/$passkeyId",
            PasskeyUpdateRequest(friendlyName = friendlyName)
        ).safeBody()
    }

    override suspend fun delete(passkeyId: String) {
        authenticatedApi.delete("passkeys/$passkeyId")
    }
}
