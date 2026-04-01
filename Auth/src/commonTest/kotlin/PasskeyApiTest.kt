import io.github.jan.supabase.SupabaseClientBuilder
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.passkey.Passkey
import io.github.jan.supabase.auth.passkey.PasskeyApi
import io.github.jan.supabase.auth.passkey.PasskeyApiImpl
import io.github.jan.supabase.auth.passkey.PasskeyCredentialHandler
import io.github.jan.supabase.auth.passkey.PasskeyUpdateRequest
import io.github.jan.supabase.testing.assertMethodIs
import io.github.jan.supabase.testing.assertPathIs
import io.github.jan.supabase.testing.createMockedSupabaseClient
import io.github.jan.supabase.testing.pathAfterVersion
import io.github.jan.supabase.testing.respondJson
import io.github.jan.supabase.testing.toJsonElement
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PasskeyApiTest {

    private val configuration: SupabaseClientBuilder.() -> Unit = {
        install(Auth) {
            minimalConfig()
        }
    }

    private val samplePasskey = Passkey(
        id = "pk-123",
        friendlyName = "MacBook Pro",
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z",
        lastUsedAt = "2024-01-15T10:30:00Z"
    )

    // --- Data Model Tests ---

    @Test
    fun testPasskeySerialization() {
        val json = """
            {
                "id": "pk-123",
                "friendly_name": "MacBook Pro",
                "created_at": "2024-01-01T00:00:00Z",
                "updated_at": "2024-01-01T00:00:00Z",
                "last_used_at": "2024-01-15T10:30:00Z"
            }
        """.trimIndent()
        val passkey = Json.decodeFromString<Passkey>(json)
        assertEquals("pk-123", passkey.id)
        assertEquals("MacBook Pro", passkey.friendlyName)
        assertEquals("2024-01-01T00:00:00Z", passkey.createdAt)
        assertEquals("2024-01-15T10:30:00Z", passkey.lastUsedAt)
    }

    @Test
    fun testPasskeyDeserializationWithNulls() {
        val json = """{"id": "pk-456"}"""
        val passkey = Json.decodeFromString<Passkey>(json)
        assertEquals("pk-456", passkey.id)
        assertNull(passkey.friendlyName)
        assertNull(passkey.createdAt)
        assertNull(passkey.lastUsedAt)
    }

    @Test
    fun testPasskeyUpdateRequestSerialization() {
        val request = PasskeyUpdateRequest(friendlyName = "My Phone")
        val json = Json.encodeToString(request)
        val parsed = Json.parseToJsonElement(json).jsonObject
        assertEquals("My Phone", parsed["friendly_name"]?.jsonPrimitive?.content)
    }

    // --- API Path Tests ---

    @Test
    fun testListPasskeys() = runTest {
        val client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Get, it.method)
            respondJson(Json.encodeToString(listOf(samplePasskey)))
        }
        val passkeys = client.auth.passkeys.list()
        assertEquals(1, passkeys.size)
        assertEquals("pk-123", passkeys.first().id)
        assertEquals("MacBook Pro", passkeys.first().friendlyName)
    }

    @Test
    fun testUpdatePasskey() = runTest {
        val passkeyId = "pk-123"
        val newName = "Updated Name"
        val client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/$passkeyId", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Patch, it.method)
            val body = it.body.toJsonElement().jsonObject
            assertEquals(newName, body["friendly_name"]?.jsonPrimitive?.content)
            respondJson(Json.encodeToString(samplePasskey.copy(friendlyName = newName)))
        }
        val passkey = client.auth.passkeys.update(passkeyId, newName)
        assertEquals(newName, passkey.friendlyName)
    }

    @Test
    fun testDeletePasskey() = runTest {
        val passkeyId = "pk-123"
        val client = createMockedSupabaseClient(
            configuration = configuration
        ) {
            assertPathIs("/passkeys/$passkeyId", it.url.pathAfterVersion())
            assertMethodIs(HttpMethod.Delete, it.method)
            respond("")
        }
        client.auth.passkeys.delete(passkeyId)
    }
}
