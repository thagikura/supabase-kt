import { test, expect, type CDPSession } from '@playwright/test';

const SUPABASE_URL = process.env.SUPABASE_URL || 'http://127.0.0.1:54321';
const SUPABASE_ANON_KEY = process.env.SUPABASE_ANON_KEY ||
  'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZS1kZW1vIiwicm9sZSI6ImFub24iLCJleHAiOjE5ODM4MTI5OTZ9.CRXP1A7WOeoJeXxjNni43kdQwgnWNReilDMblYTn_I0';

/**
 * Helper to call the GoTrue API directly from Node.js context.
 */
async function authFetch(path: string, options: { method?: string; body?: string; accessToken?: string } = {}) {
  const headers: Record<string, string> = {
    'apikey': SUPABASE_ANON_KEY,
    'Content-Type': 'application/json',
  };
  if (options.accessToken) {
    headers['Authorization'] = `Bearer ${options.accessToken}`;
  }
  const res = await fetch(`${SUPABASE_URL}/auth/v1${path}`, {
    method: options.method || 'GET',
    headers,
    body: options.body,
  });
  const text = await res.text();
  let json: any;
  try { json = JSON.parse(text); } catch { json = text; }
  if (!res.ok) throw new Error(`${res.status}: ${JSON.stringify(json)}`);
  return json;
}

test.describe('Passkey (WebAuthn) E2E', () => {
  let cdpSession: CDPSession;
  let authenticatorId: string;
  let accessToken: string;

  test.beforeAll(async ({ browser }) => {
    // Sign up a test user via API
    const email = `passkey-e2e-${Date.now()}@example.com`;
    const data = await authFetch('/signup', {
      method: 'POST',
      body: JSON.stringify({ email, password: 'test-password-123!' }),
    });
    accessToken = data.access_token;
    expect(accessToken).toBeTruthy();
  });

  test.beforeEach(async ({ page }) => {
    // Attach a virtual authenticator via CDP before each test
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

  test('full passkey registration and authentication ceremony', async ({ page }) => {
    // Navigate to the test page
    await page.goto('/passkey-test.html');

    // Fill in config (the page defaults should work, but let's be explicit)
    await page.fill('#url', SUPABASE_URL);
    await page.fill('#anonKey', SUPABASE_ANON_KEY);

    // Step 1: Sign up
    const email = `passkey-e2e-browser-${Date.now()}@example.com`;
    await page.fill('#email', email);
    await page.fill('#password', 'test-password-123!');
    await page.click('button:has-text("Sign Up")');

    // Wait for sign-up success (log entries are prepended, so first = most recent)
    await expect(page.locator('.success').first()).toContainText('Signed up', { timeout: 10000 });

    // Step 2: Register passkey (virtual authenticator handles the ceremony)
    await page.click('#btnRegister');
    await expect(page.locator('.success').first()).toContainText('Passkey registered', { timeout: 10000 });

    // Step 3: List passkeys
    await page.click('#btnList');
    await expect(page.locator('.success').first()).toContainText('passkey(s)', { timeout: 10000 });

    // Step 4: Sign in with passkey
    await page.click('#btnSignIn');
    await expect(page.locator('.success').first()).toContainText('Signed in with passkey', { timeout: 10000 });
  });

  test('list passkeys returns empty for new user', async () => {
    const passkeys = await authFetch('/passkeys', { accessToken });
    expect(Array.isArray(passkeys)).toBe(true);
    expect(passkeys.length).toBe(0);
  });

  test('registration options returns challenge', async () => {
    const challenge = await authFetch('/passkeys/registration/options', {
      method: 'POST',
      body: '{}',
      accessToken,
    });
    expect(challenge.challenge_id).toBeTruthy();
    expect(challenge.options).toBeTruthy();
    expect(challenge.options.publicKey).toBeTruthy();
    expect(challenge.options.publicKey.challenge).toBeTruthy();
    expect(challenge.options.publicKey.rp.id).toBe('localhost');
  });

  test('authentication options returns challenge without auth', async () => {
    const challenge = await authFetch('/passkeys/authentication/options', {
      method: 'POST',
      body: '{}',
    });
    expect(challenge.challenge_id).toBeTruthy();
    expect(challenge.options).toBeTruthy();
    expect(challenge.options.publicKey).toBeTruthy();
    expect(challenge.options.publicKey.challenge).toBeTruthy();
  });
});
