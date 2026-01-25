import { test, afterEach } from 'node:test';
import assert from 'node:assert/strict';
import { apiRequest, authFetch, setSessionToken } from '../http.js';
import { deleteAccount, signInWithApple, signInWithGoogle, createChatSocketTicket } from '../services.js';
import { WS_BASE_URL, API_BASE_URL } from '../config.js';

afterEach(() => setSessionToken(null));

function recorder(status = 200, body = {}) {
  const calls = [];
  const fakeFetch = async (url, init) => {
    calls.push({ url, init });
    return new Response(status === 204 ? null : JSON.stringify(body), {
      status,
      headers: { 'Content-Type': 'application/json' },
    });
  };
  return { calls, fakeFetch };
}

test('apiRequest sends the bearer token once a session is set', async () => {
  const { calls, fakeFetch } = recorder(200, []);
  setSessionToken('jwt-abc');
  await apiRequest('GET', '/items', undefined, fakeFetch);
  assert.equal(calls[0].init.headers.Authorization, 'Bearer jwt-abc');
});

test('apiRequest keeps Content-Type and adds Authorization for JSON bodies', async () => {
  const { calls, fakeFetch } = recorder(200, {});
  setSessionToken('jwt-abc');
  await apiRequest('POST', '/chat/threads', { itemId: 1 }, fakeFetch);
  assert.equal(calls[0].init.headers['Content-Type'], 'application/json');
  assert.equal(calls[0].init.headers.Authorization, 'Bearer jwt-abc');
});

test('clearing the session stops sending the bearer token', async () => {
  const { calls, fakeFetch } = recorder(200, []);
  setSessionToken('jwt-abc');
  setSessionToken(null);
  await apiRequest('GET', '/items', undefined, fakeFetch);
  assert.equal(calls[0].init.headers, undefined);
});

test('an Authorization header passed explicitly is not overwritten', async () => {
  const originalFetch = globalThis.fetch;
  const seen = [];
  globalThis.fetch = async (url, init) => {
    seen.push(init);
    return new Response('{}', { status: 200 });
  };
  try {
    setSessionToken('jwt-abc');
    await authFetch('/x', { headers: { authorization: 'Bearer other' } });
    assert.equal(seen[0].headers.authorization, 'Bearer other');
    assert.equal(seen[0].headers.Authorization, undefined);
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('authFetch keeps caller headers and attaches the cookie and token', async () => {
  const originalFetch = globalThis.fetch;
  const seen = [];
  globalThis.fetch = async (url, init) => {
    seen.push({ url, init });
    return new Response('{}', { status: 200 });
  };
  try {
    setSessionToken('jwt-abc');
    await authFetch('/items/1', { method: 'DELETE', headers: { 'X-Test': '1' } });
    assert.equal(seen[0].init.method, 'DELETE');
    assert.equal(seen[0].init.credentials, 'include');
    assert.equal(seen[0].init.headers['X-Test'], '1');
    assert.equal(seen[0].init.headers.Authorization, 'Bearer jwt-abc');
  } finally {
    globalThis.fetch = originalFetch;
  }
});

test('signInWithGoogle and signInWithApple post the provider ID token', async () => {
  const google = recorder(200, { token: 't' });
  await signInWithGoogle('google-id-token', google.fakeFetch);
  assert.match(google.calls[0].url, /\/auth\/google$/);
  assert.deepEqual(JSON.parse(google.calls[0].init.body), { idToken: 'google-id-token' });

  const apple = recorder(200, { token: 't' });
  await signInWithApple('apple-id-token', 'apple-auth-code', apple.fakeFetch);
  assert.match(apple.calls[0].url, /\/auth\/apple$/);
  assert.deepEqual(JSON.parse(apple.calls[0].init.body), {
    idToken: 'apple-id-token',
    authorizationCode: 'apple-auth-code',
  });
});

test('deleteAccount sends DELETE /users/me and resolves on 204', async () => {
  const { calls, fakeFetch } = recorder(204);
  assert.equal(await deleteAccount(fakeFetch), null);
  assert.equal(calls[0].init.method, 'DELETE');
  assert.match(calls[0].url, /\/users\/me$/);
});

test('createChatSocketTicket returns the ticket from the server', async () => {
  const { fakeFetch } = recorder(200, { ticket: 'short-lived' });
  const response = await createChatSocketTicket(fakeFetch);
  assert.equal(response.ticket, 'short-lived');
});

test('the WebSocket base URL is the API host over ws', () => {
  assert.equal(WS_BASE_URL, API_BASE_URL.replace(/^http/, 'ws'));
  assert.match(WS_BASE_URL, /^wss?:\/\//);
});
