// Typed wrappers for every endpoint the lost/found flows use.
// Components call these instead of building URLs, so a native client can swap the transport later.
import { apiRequest } from './http.js';
import { POST_TYPES } from './reports.js';

/** Open LOST reports, i.e. the "missing item" list shown to people who found something. */
export function fetchMissingReports(fetchImpl) {
  return apiRequest('GET', `/items?postType=${POST_TYPES.LOST}&status=OPEN`, undefined, fetchImpl);
}

/** Creates a LOST or FOUND report. `payload` comes from `buildCreateItemRequest`. */
export function createReport(payload, fetchImpl) {
  return apiRequest('POST', '/items', payload, fetchImpl);
}

/** Runs the lost search. Returns `{ searchId, results: FoundItemSummaryDTO[] }`. */
export function runLostSearch(payload, fetchImpl) {
  return apiRequest('POST', '/searches/lost', payload, fetchImpl);
}

/**
 * Opens (or reuses) a chat thread between the current user and the item's poster.
 * The server looks up the poster, so the client never needs a poster id. Returns a thread summary.
 */
export function contactItemPoster(itemId, fetchImpl) {
  return apiRequest('POST', `/chat/items/${itemId}/thread`, undefined, fetchImpl);
}

/** Exchanges a Google ID token (from the native sign-in plugin) for the app's session. Returns `{ token, username, ... }`. */
export function signInWithGoogle(idToken, fetchImpl) {
  return apiRequest('POST', '/auth/google', { idToken }, fetchImpl);
}

/**
 * Exchanges an Apple identity token (from the native sign-in plugin) for the app's session.
 * `authorizationCode` is Apple's one-time code from the same sign-in; the server keeps a refresh token from it
 * so the Apple link can be revoked when the account is deleted.
 */
export function signInWithApple(idToken, authorizationCode, fetchImpl) {
  return apiRequest('POST', '/auth/apple', { idToken, authorizationCode }, fetchImpl);
}

/** Deletes the signed-in account. The server anonymizes it, keeps the user's posts, and ends the session. */
export function deleteAccount(fetchImpl) {
  return apiRequest('DELETE', '/users/me', undefined, fetchImpl);
}

/** Short-lived ticket for opening the chat WebSocket (`/ws/chat?ticket=...`). Valid for about a minute. */
export function createChatSocketTicket(fetchImpl) {
  return apiRequest('POST', '/chat/ws-ticket', undefined, fetchImpl);
}

/**
 * Proposes resolving an item. With a `resolvedToUserId` the item moves to PENDING_RESOLUTION and that
 * person must confirm; without one the owner closes it outright. Returns the updated item.
 */
export function markItemResolved(itemId, resolvedToUserId, fetchImpl) {
  const body = { itemId, status: 'RESOLVED' };
  if (resolvedToUserId != null) body.resolvedToUserId = resolvedToUserId;
  return apiRequest('PATCH', `/items/${itemId}`, body, fetchImpl);
}

/** Confirms, as the chosen counterparty, a resolution the poster proposed. Returns the updated item. */
export function confirmItemResolution(itemId, fetchImpl) {
  return apiRequest('POST', `/items/${itemId}/confirm-resolution`, undefined, fetchImpl);
}
