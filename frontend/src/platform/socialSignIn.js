// PLATFORM SEAM. Native Google and Sign in with Apple via @capgo/capacitor-social-login.
// Resolves the provider's ID token; the server verifies it (see POST /auth/google and /auth/apple).
import { SocialLogin } from '@capgo/capacitor-social-login';

const GOOGLE_WEB_CLIENT_ID = import.meta.env.VITE_GOOGLE_WEB_CLIENT_ID || '';
const GOOGLE_IOS_CLIENT_ID = import.meta.env.VITE_GOOGLE_IOS_CLIENT_ID || '';
const APPLE_CLIENT_ID = import.meta.env.VITE_APPLE_CLIENT_ID || '';

let initialized = null;

function initialize() {
  if (!initialized) {
    initialized = SocialLogin.initialize({
      google: { webClientId: GOOGLE_WEB_CLIENT_ID, iOSClientId: GOOGLE_IOS_CLIENT_ID, mode: 'online' },
      // Needed for the Apple authorization code, which the server exchanges for a refresh token (see POST /auth/apple).
      apple: { clientId: APPLE_CLIENT_ID, useProperTokenExchange: true },
    }).catch((error) => {
      initialized = null;
      throw error;
    });
  }
  return initialized;
}

/**
 * Shows the native sheet for `provider` ('google' | 'apple').
 * Resolves `{ idToken, authorizationCode }`. The code is only present for Apple.
 */
export async function getNativeSignIn(provider) {
  await initialize();
  const options = provider === 'apple' ? { scopes: ['email', 'name'] } : { scopes: ['email', 'profile'] };
  const response = await SocialLogin.login({ provider, options });
  const idToken = response?.result?.idToken;
  if (!idToken) {
    throw new Error('Sign-in did not return an identity token. Please try again.');
  }
  return { idToken, authorizationCode: response.result.authorizationCode ?? null };
}
