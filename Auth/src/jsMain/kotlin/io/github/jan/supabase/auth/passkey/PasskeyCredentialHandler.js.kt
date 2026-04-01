package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.js.Promise

@SupabaseInternal
internal actual fun createPlatformPasskeyCredentialHandler(): PasskeyCredentialHandler =
    JsPasskeyCredentialHandler()

private class JsPasskeyCredentialHandler : PasskeyCredentialHandler {

    override suspend fun create(options: JsonObject): JsonObject {
        val resultJson = jsWebAuthnCreate(options.toString()).await()
        return Json.parseToJsonElement(resultJson) as JsonObject
    }

    override suspend fun get(options: JsonObject): JsonObject {
        val resultJson = jsWebAuthnGet(options.toString()).await()
        return Json.parseToJsonElement(resultJson) as JsonObject
    }
}

// JS-only interop using js() which is available in Kotlin/JS but not WasmJS.

private fun jsWebAuthnCreate(optionsJson: String): Promise<String> = js("""(function() {
    var options = JSON.parse(optionsJson);
    var pk = options.publicKey || options;
    function b2a(s){s=s.replace(/-/g,'+').replace(/_/g,'/');while(s.length%4)s+='=';var b=atob(s),a=new Uint8Array(b.length);for(var i=0;i<b.length;i++)a[i]=b.charCodeAt(i);return a.buffer}
    function a2b(buf){var a=new Uint8Array(buf),s='';for(var i=0;i<a.length;i++)s+=String.fromCharCode(a[i]);return btoa(s).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'')}
    if(pk.challenge)pk.challenge=b2a(pk.challenge);
    if(pk.user&&pk.user.id)pk.user.id=b2a(pk.user.id);
    if(pk.excludeCredentials)pk.excludeCredentials=pk.excludeCredentials.map(function(c){return Object.assign({},c,{id:b2a(c.id)})});
    return navigator.credentials.create({publicKey:pk}).then(function(c){var r=c.response;return JSON.stringify({id:c.id,rawId:a2b(c.rawId),response:{attestationObject:a2b(r.attestationObject),clientDataJSON:a2b(r.clientDataJSON)},type:c.type,authenticatorAttachment:c.authenticatorAttachment||null,clientExtensionResults:c.getClientExtensionResults()})});
})()""")

private fun jsWebAuthnGet(optionsJson: String): Promise<String> = js("""(function() {
    var options = JSON.parse(optionsJson);
    var pk = options.publicKey || options;
    function b2a(s){s=s.replace(/-/g,'+').replace(/_/g,'/');while(s.length%4)s+='=';var b=atob(s),a=new Uint8Array(b.length);for(var i=0;i<b.length;i++)a[i]=b.charCodeAt(i);return a.buffer}
    function a2b(buf){var a=new Uint8Array(buf),s='';for(var i=0;i<a.length;i++)s+=String.fromCharCode(a[i]);return btoa(s).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'')}
    if(pk.challenge)pk.challenge=b2a(pk.challenge);
    if(pk.allowCredentials)pk.allowCredentials=pk.allowCredentials.map(function(c){return Object.assign({},c,{id:b2a(c.id)})});
    return navigator.credentials.get({publicKey:pk}).then(function(c){var r=c.response;return JSON.stringify({id:c.id,rawId:a2b(c.rawId),response:{authenticatorData:a2b(r.authenticatorData),clientDataJSON:a2b(r.clientDataJSON),signature:a2b(r.signature),userHandle:r.userHandle?a2b(r.userHandle):null},type:c.type,authenticatorAttachment:c.authenticatorAttachment||null,clientExtensionResults:c.getClientExtensionResults()})});
})()""")
