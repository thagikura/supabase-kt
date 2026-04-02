import { test, expect, type CDPSession } from '@playwright/test';

/**
 * E2E tests that exercise the full passkey ceremony through the compiled
 * supabase-kt Kotlin/JS SDK (not raw fetch calls).
 *
 * The compiled bundle (passkey-e2e.js) loads the real supabase-kt Auth module
 * and exposes test functions on window.__passkeyTest__.
 *
 * Prerequisites:
 *   1. Build the bundle: ./gradlew :integration-test:e2e-kt:jsBrowserProductionWebpack
 *   2. Copy to e2e dir:  cp integration-test/e2e-kt/build/kotlin-webpack/js/productionExecutable/passkey-e2e.js integration-test/e2e/
 *   3. Supabase running with passkeys enabled
 */

test.describe('Passkey via supabase-kt SDK', () => {
  let cdpSession: CDPSession;
  let authenticatorId: string;

  test.beforeEach(async ({ page }) => {
    cdpSession = await page.context().newCDPSession(page);
    await cdpSession.send('WebAuthn.enable');
    const result = await cdpSession.send('WebAuthn.addVirtualAuthenticator', {
      options: {
        protocol: 'ctap2',
        transport: 'internal',
        hasResidentKey: true,
        hasUserVerification: true,
        isUserVerified: true,
      },
    });
    authenticatorId = result.authenticatorId;
  });

  test.afterEach(async () => {
    if (cdpSession && authenticatorId) {
      await cdpSession.send('WebAuthn.removeVirtualAuthenticator', { authenticatorId });
    }
  });

  test('full passkey ceremony via supabase-kt SDK', async ({ page }) => {
    await page.goto('/passkey-kt-test.html');

    // Wait for the Kotlin module to load
    await expect(page.locator('#status')).toHaveText('Module loaded', { timeout: 10000 });

    const email = `kt-e2e-${Date.now()}@example.com`;

    // Step 1: Sign up via supabase-kt SDK
    const signUpResult = await page.evaluate(async (email) => {
      return await (window as any).__passkeyTest__.signUp(email, 'test-password-123!');
    }, email);
    expect(signUpResult).toBe('ok');

    // Step 2: Register passkey via supabase-kt SDK (uses BrowserPasskeyCredentialHandler)
    const registerResult = await page.evaluate(async () => {
      return await (window as any).__passkeyTest__.registerPasskey();
    });
    expect(registerResult).toBeTruthy();
    const passkey = JSON.parse(registerResult);
    expect(passkey.id).toBeTruthy();

    // Step 3: List passkeys via supabase-kt SDK
    const listResult = await page.evaluate(async () => {
      return await (window as any).__passkeyTest__.listPasskeys();
    });
    const passkeys = JSON.parse(listResult);
    expect(passkeys.length).toBeGreaterThan(0);

    // Step 4: Sign out, then sign in with passkey via supabase-kt SDK
    await page.evaluate(async () => {
      return await (window as any).__passkeyTest__.signOut();
    });

    const signInResult = await page.evaluate(async () => {
      return await (window as any).__passkeyTest__.signInWithPasskey();
    });
    // signInWithPasskey returns the access token
    expect(signInResult).toBeTruthy();
    expect(typeof signInResult).toBe('string');
  });
});
