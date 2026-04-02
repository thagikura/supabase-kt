/**
 * WebAuthn bridge functions for supabase-kt.
 *
 * These functions handle the full WebAuthn credential ceremony:
 * 1. Parse the server's JSON options (converting base64url strings to ArrayBuffer)
 * 2. Call navigator.credentials.create() or .get()
 * 3. Serialize the credential response back to JSON (converting ArrayBuffer to base64url)
 *
 * Called from Kotlin via external declarations.
 */

// --- Base64url helpers ---

function base64urlToArrayBuffer(base64url) {
    var base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
    while (base64.length % 4) base64 += '=';
    var binary = atob(base64);
    var bytes = new Uint8Array(binary.length);
    for (var i = 0; i < binary.length; i++) {
        bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
}

function arrayBufferToBase64url(buffer) {
    var bytes = new Uint8Array(buffer);
    var binary = '';
    for (var i = 0; i < bytes.length; i++) {
        binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

// --- Credential creation (registration) ---

function supabaseWebAuthnCreate(optionsJson) {
    var options = JSON.parse(optionsJson);
    var publicKey = options.publicKey || options;

    // Convert base64url strings to ArrayBuffer for the browser API
    if (publicKey.challenge) {
        publicKey.challenge = base64urlToArrayBuffer(publicKey.challenge);
    }
    if (publicKey.user && publicKey.user.id) {
        publicKey.user.id = base64urlToArrayBuffer(publicKey.user.id);
    }
    if (publicKey.excludeCredentials) {
        publicKey.excludeCredentials = publicKey.excludeCredentials.map(function (cred) {
            return Object.assign({}, cred, { id: base64urlToArrayBuffer(cred.id) });
        });
    }

    return navigator.credentials.create({ publicKey: publicKey }).then(function (credential) {
        var response = credential.response;
        return JSON.stringify({
            id: credential.id,
            rawId: arrayBufferToBase64url(credential.rawId),
            response: {
                attestationObject: arrayBufferToBase64url(response.attestationObject),
                clientDataJSON: arrayBufferToBase64url(response.clientDataJSON)
            },
            type: credential.type,
            authenticatorAttachment: credential.authenticatorAttachment || null,
            clientExtensionResults: credential.getClientExtensionResults()
        });
    });
}

// --- Credential assertion (authentication) ---

function supabaseWebAuthnGet(optionsJson) {
    var options = JSON.parse(optionsJson);
    var publicKey = options.publicKey || options;

    if (publicKey.challenge) {
        publicKey.challenge = base64urlToArrayBuffer(publicKey.challenge);
    }
    if (publicKey.allowCredentials) {
        publicKey.allowCredentials = publicKey.allowCredentials.map(function (cred) {
            return Object.assign({}, cred, { id: base64urlToArrayBuffer(cred.id) });
        });
    }

    return navigator.credentials.get({ publicKey: publicKey }).then(function (credential) {
        var response = credential.response;
        return JSON.stringify({
            id: credential.id,
            rawId: arrayBufferToBase64url(credential.rawId),
            response: {
                authenticatorData: arrayBufferToBase64url(response.authenticatorData),
                clientDataJSON: arrayBufferToBase64url(response.clientDataJSON),
                signature: arrayBufferToBase64url(response.signature),
                userHandle: response.userHandle
                    ? arrayBufferToBase64url(response.userHandle)
                    : null
            },
            type: credential.type,
            authenticatorAttachment: credential.authenticatorAttachment || null,
            clientExtensionResults: credential.getClientExtensionResults()
        });
    });
}
