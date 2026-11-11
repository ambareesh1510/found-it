// PLATFORM SEAM. Keeps the session token for the native app in the iOS Keychain.
// On the web the token is kept in localStorage, so it works when browsers block the cross-site cookie.
import { Capacitor } from '@capacitor/core';
import { SecureStorage } from '@aparajita/capacitor-secure-storage';
import { setSessionToken } from '../core/http.js';

const TOKEN_KEY = 'session-token';

export function isNativeApp() {
  return Capacitor.isNativePlatform();
}

function readWebToken() {
  try {
    return window.localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

function writeWebToken(token) {
  try {
    if (token) window.localStorage.setItem(TOKEN_KEY, token);
    else window.localStorage.removeItem(TOKEN_KEY);
  } catch {
    // Storage can be unavailable (for example in some private modes). The in-memory token still works.
  }
}

/** Call once at startup, before the first API request. Loads a saved session, if any. */
export async function restoreSession() {
  if (!isNativeApp()) {
    setSessionToken(readWebToken());
    return;
  }
  try {
    setSessionToken(await SecureStorage.getItem(TOKEN_KEY));
  } catch {
    setSessionToken(null);
  }
}

/**
 * Reads a token from the URL fragment (#token=...) that the backend adds after Google sign-in on the web,
 * then removes it from the address bar. Returns the token, or null if there is none.
 */
export function takeTokenFromUrl() {
  const match = window.location.hash.match(/(?:^#|&)token=([^&]+)/);
  if (!match) return null;
  window.history.replaceState(null, '', window.location.pathname + window.location.search);
  return decodeURIComponent(match[1]);
}

/** Call after a successful login or sign-in with the token the server returned. */
export async function startSession(token) {
  setSessionToken(token);
  if (isNativeApp()) {
    await SecureStorage.setItem(TOKEN_KEY, token);
  } else {
    writeWebToken(token);
  }
}

/** Call on logout or account deletion. */
export async function endSession() {
  setSessionToken(null);
  if (isNativeApp()) {
    try {
      await SecureStorage.removeItem(TOKEN_KEY);
    } catch {
      // Nothing stored or the Keychain refused; the in-memory token is already cleared.
    }
  } else {
    writeWebToken(null);
  }
}
