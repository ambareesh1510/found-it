// PLATFORM SEAM. Keeps the session token for the native app in the iOS Keychain.
// On the web the server sets an HttpOnly cookie instead, so nothing is stored here.
import { Capacitor } from '@capacitor/core';
import { SecureStorage } from '@aparajita/capacitor-secure-storage';
import { setSessionToken } from '../core/http.js';

const TOKEN_KEY = 'session-token';

export function isNativeApp() {
  return Capacitor.isNativePlatform();
}

/** Call once at startup, before the first API request. Loads a saved native session, if any. */
export async function restoreSession() {
  if (!isNativeApp()) return;
  try {
    setSessionToken(await SecureStorage.getItem(TOKEN_KEY));
  } catch {
    setSessionToken(null);
  }
}

/** Call after a successful login or sign-in with the token the server returned. */
export async function startSession(token) {
  setSessionToken(token);
  if (isNativeApp()) {
    await SecureStorage.setItem(TOKEN_KEY, token);
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
  }
}
