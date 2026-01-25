import { test } from 'node:test';
import assert from 'node:assert/strict';
import { apiRequest, ApiError } from '../http.js';
import { NETWORK_ERROR_MESSAGE } from '../config.js';

function jsonResponse(status, body) {
  return new Response(body === undefined ? '' : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
}

test('sends JSON with the session cookie and returns parsed JSON', async () => {
  let seen;
  const fakeFetch = async (url, init) => {
    seen = { url, init };
    return jsonResponse(200, { searchId: 7, results: [] });
  };

  const data = await apiRequest('POST', '/searches/lost', { category: 'Keys' }, fakeFetch);

  assert.deepEqual(data, { searchId: 7, results: [] });
  assert.match(seen.url, /\/searches\/lost$/);
  assert.equal(seen.init.method, 'POST');
  assert.equal(seen.init.credentials, 'include');
  assert.equal(seen.init.headers['Content-Type'], 'application/json');
  assert.deepEqual(JSON.parse(seen.init.body), { category: 'Keys' });
});

test('GET requests send no body or content type', async () => {
  let seen;
  const fakeFetch = async (url, init) => {
    seen = init;
    return jsonResponse(200, []);
  };
  await apiRequest('GET', '/items?postType=LOST', undefined, fakeFetch);
  assert.equal(seen.body, undefined);
  assert.equal(seen.headers, undefined);
});

test('an empty success body resolves to null', async () => {
  const fakeFetch = async () => new Response(null, { status: 204 });
  assert.equal(await apiRequest('POST', '/auth/logout', undefined, fakeFetch), null);
});

test('error responses surface the server message', async () => {
  const fakeFetch = async () => jsonResponse(400, { message: 'postType must be one of: LOST, FOUND.' });
  await assert.rejects(apiRequest('GET', '/items?postType=X', undefined, fakeFetch), (error) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 400);
    assert.equal(error.message, 'postType must be one of: LOST, FOUND.');
    return true;
  });
});

test('error responses without a message fall back by status', async () => {
  const fakeFetch = async () => new Response('', { status: 401 });
  await assert.rejects(apiRequest('GET', '/items', undefined, fakeFetch), (error) => {
    assert.equal(error.status, 401);
    assert.equal(error.message, 'Please log in to continue.');
    return true;
  });
});

test('a network failure becomes a status 0 ApiError with the shared message', async () => {
  const fakeFetch = async () => {
    throw new TypeError('Failed to fetch');
  };
  await assert.rejects(apiRequest('GET', '/items', undefined, fakeFetch), (error) => {
    assert.equal(error.status, 0);
    assert.equal(error.message, NETWORK_ERROR_MESSAGE);
    return true;
  });
});
