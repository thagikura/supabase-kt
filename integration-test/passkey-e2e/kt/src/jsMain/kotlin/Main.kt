import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.passkey.Passkey
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val supabaseUrl = window.asDynamic().__SUPABASE_URL__ as? String ?: "http://127.0.0.1:54321"
private val supabaseAnonKey = window.asDynamic().__SUPABASE_ANON_KEY__ as? String
    ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0"

private val json = Json { encodeDefaults = true }

private val client = createSupabaseClient(supabaseUrl, supabaseAnonKey) {
    install(Auth) {
        alwaysAutoRefresh = false
    }
}

fun main() {
    val exports = js("{}")

    exports.signUp = { email: String, password: String ->
        GlobalScope.promise {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            "ok"
        }
    }

    exports.registerPasskey = {
        GlobalScope.promise {
            val passkey = client.auth.passkeys.register()
            json.encodeToString(Passkey.serializer(), passkey)
        }
    }

    exports.listPasskeys = {
        GlobalScope.promise {
            val passkeys = client.auth.passkeys.list()
            json.encodeToString(ListSerializer(Passkey.serializer()), passkeys)
        }
    }

    exports.signInWithPasskey = {
        GlobalScope.promise {
            val session = client.auth.passkeys.signIn()
            session.accessToken
        }
    }

    exports.signOut = {
        GlobalScope.promise {
            client.auth.clearSession()
            "ok"
        }
    }

    window.asDynamic().__passkeyTest__ = exports
    console.log("supabase-kt passkey e2e module loaded")
}
