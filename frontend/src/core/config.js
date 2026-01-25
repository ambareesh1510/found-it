// Single source for the API host. Set VITE_API_URL to override (Compose and the iOS build will set it).
// Plain constants only: this module must not touch React, the DOM, or window.
export const API_BASE_URL = (import.meta.env?.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');

// Same host as the API, over ws:// or wss://. Chat uses this for its WebSocket.
export const WS_BASE_URL = API_BASE_URL.replace(/^http/, 'ws');

export const NETWORK_ERROR_MESSAGE =
  'Could not reach the server. Check your connection and try again.';
