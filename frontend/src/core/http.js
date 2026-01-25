import { API_BASE_URL, NETWORK_ERROR_MESSAGE } from './config.js';

/** A failed API call. `status` is 0 when the server could not be reached. */
export class ApiError extends Error {
  constructor(status, message) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

function fallbackMessage(status) {
  if (status === 401) return 'Please log in to continue.';
  if (status === 403) return 'You do not have permission to do that.';
  if (status === 404) return 'That item could not be found.';
  return 'Something went wrong. Please try again.';
}

// Bearer token for the native app (kept in the iOS Keychain by platform/session.js).
// The web build leaves it null and relies on the HttpOnly session cookie instead.
let sessionToken = null;

/** Sets (or clears, with null) the token sent as `Authorization: Bearer` on every request. */
export function setSessionToken(token) {
  sessionToken = token || null;
}

function withAuthorization(headers) {
  const hasAuthorization = Object.keys(headers).some((name) => name.toLowerCase() === 'authorization');
  if (sessionToken && !hasAuthorization) {
    return { ...headers, Authorization: `Bearer ${sessionToken}` };
  }
  return headers;
}

/**
 * fetch() for the rare call that needs raw Response access (e.g. the file-based screens).
 * Adds the session token and cookie the same way `apiRequest` does.
 */
export function authFetch(url, init = {}) {
  return globalThis.fetch(url, {
    credentials: 'include',
    ...init,
    headers: withAuthorization({ ...(init.headers || {}) }),
  });
}

/**
 * JSON request to the API with the session attached (bearer token on native, cookie on web).
 * Every network call in the app goes through here, so error handling and the base URL
 * live in one place. `fetchImpl` is injectable for tests.
 */
export async function apiRequest(method, path, body, fetchImpl = globalThis.fetch) {
  const baseHeaders = body === undefined ? {} : { 'Content-Type': 'application/json' };
  const headers = withAuthorization(baseHeaders);

  let response;
  try {
    response = await fetchImpl(`${API_BASE_URL}${path}`, {
      method,
      credentials: 'include',
      headers: Object.keys(headers).length === 0 ? undefined : headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, NETWORK_ERROR_MESSAGE);
  }

  if (!response.ok) {
    const data = await response.json().catch(() => null);
    throw new ApiError(response.status, data?.message || fallbackMessage(response.status));
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
