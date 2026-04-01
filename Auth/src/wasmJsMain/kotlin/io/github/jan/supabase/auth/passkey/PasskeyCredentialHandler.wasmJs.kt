package io.github.jan.supabase.auth.passkey

import io.github.jan.supabase.annotations.SupabaseInternal
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString
import kotlin.js.Promise

@SupabaseInternal
internal actual fun createPlatformPasskeyCredentialHandler(): PasskeyCredentialHandler =
    WasmJsPasskeyCredentialHandler()

private class WasmJsPasskeyCredentialHandler : PasskeyCredentialHandler {

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun create(options: JsonObject): JsonObject {
        val resultJs = wasmWebAuthnCreate(options.toString().toJsString()).await<JsString>()
        return Json.parseToJsonElement(resultJs.toString()) as JsonObject
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun get(options: JsonObject): JsonObject {
        val resultJs = wasmWebAuthnGet(options.toString().toJsString()).await<JsString>()
        return Json.parseToJsonElement(resultJs.toString()) as JsonObject
    }
}

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(optionsJson) => {
    var options = JSON.parse(optionsJson);
    var pk = options.publicKey || options;
    function b2a(s){s=s.replace(/-/g,'+').replace(/_/g,'/');while(s.length%4)s+='=';var b=atob(s),a=new Uint8Array(b.length);for(var i=0;i<b.length;i++)a[i]=b.charCodeAt(i);return a.buffer}
    function a2b(buf){var a=new Uint8Array(buf),s='';for(var i=0;i<a.length;i++)s+=String.fromCharCode(a[i]);return btoa(s).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'')}
    if(pk.challenge)pk.challenge=b2a(pk.challenge);
    if(pk.user&&pk.user.id)pk.user.id=b2a(pk.user.id);
    if(pk.excludeCredentials)pk.excludeCredentials=pk.excludeCredentials.map(function(c){return Object.assign({},c,{id:b2a(c.id)})});
    return navigator.credentials.create({publicKey:pk}).then(function(c){var r=c.response;return JSON.stringify({id:c.id,rawId:a2b(c.rawId),response:{attestationObject:a2b(r.attestationObject),clientDataJSON:a2b(r.clientDataJSON)},type:c.type,authenticatorAttachment:c.authenticatorAttachment||null,clientExtensionResults:c.getClientExtensionResults()})});
}""")
private external fun wasmWebAuthnCreate(optionsJson: JsString): Promise<JsString>

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("""(optionsJson) => {
    var options = JSON.parse(optionsJson);
    var pk = options.publicKey || options;
    function b2a(s){s=s.replace(/-/g,'+').replace(/_/g,'/');while(s.length%4)s+='=';var b=atob(s),a=new Uint8Array(b.length);for(var i=0;i<b.length;i++)a[i]=b.charCodeAt(i);return a.buffer}
    function a2b(buf){var a=new Uint8Array(buf),s='';for(var i=0;i<a.length;i++)s+=String.fromCharCode(a[i]);return btoa(s).replace(/\+/g,'-').replace(/\//g,'_').replace(/=+$/,'')}
    if(pk.challenge)pk.challenge=b2a(pk.challenge);
    if(pk.allowCredentials)pk.allowCredentials=pk.allowCredentials.map(function(c){return Object.assign({},c,{id:b2a(c.id)})});
    return navigator.credentials.get({publicKey:pk}).then(function(c){var r=c.response;return JSON.stringify({id:c.id,rawId:a2b(c.rawId),response:{authenticatorData:a2b(r.authenticatorData),clientDataJSON:a2b(r.clientDataJSON),signature:a2b(r.signature),userHandle:r.userHandle?a2b(r.userHandle):null},type:c.type,authenticatorAttachment:c.authenticatorAttachment||null,clientExtensionResults:c.getClientExtensionResults()})});
}""")
private external fun wasmWebAuthnGet(optionsJson: JsString): Promise<JsString>
