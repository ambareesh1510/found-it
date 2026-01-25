import { getCurrentCoordinates } from './geolocation.js';

const FIVE_MINUTES_MS = 5 * 60 * 1000;
const SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000;
const ONE_DAY_MS = 24 * 60 * 60 * 1000;

const historyKey = (userId) => `findit:timeline:${userId}`;
const enabledKey = (userId) => `findit:timeline-enabled:${userId}`;

function storage() {
  return globalThis.localStorage;
}

function readHistory(userId, now = Date.now()) {
  try {
    const parsed = JSON.parse(storage()?.getItem(historyKey(userId)) || '[]');
    return Array.isArray(parsed)
      ? parsed.filter((point) => Number.isFinite(point.lat) && Number.isFinite(point.lng)
          && Number.isFinite(point.recordedAt) && point.recordedAt >= now - SEVEN_DAYS_MS)
      : [];
  } catch {
    return [];
  }
}

function writeHistory(userId, points) {
  storage()?.setItem(historyKey(userId), JSON.stringify(points));
}

export function isTimelineEnabled(userId) {
  return storage()?.getItem(enabledKey(userId)) === 'true';
}

export function setTimelineEnabled(userId, enabled) {
  storage()?.setItem(enabledKey(userId), String(Boolean(enabled)));
}

export function getRecentTimelinePoints(userId, now = Date.now()) {
  const retained = readHistory(userId, now);
  writeHistory(userId, retained);
  return retained
    .filter((point) => point.recordedAt >= now - ONE_DAY_MS && point.recordedAt <= now)
    .map((point) => ({
      latitude: point.lat,
      longitude: point.lng,
      recordedAt: new Date(point.recordedAt).toISOString(),
    }));
}

export async function captureTimelinePoint(userId, now = Date.now(), verifyPermission = false) {
  if (!isTimelineEnabled(userId)) return false;
  const points = readHistory(userId, now);
  const latest = points.at(-1);
  if (latest && now - latest.recordedAt < FIVE_MINUTES_MS && !verifyPermission) return false;

  const coordinates = await getCurrentCoordinates();
  if (latest && now - latest.recordedAt < FIVE_MINUTES_MS) return false;
  // Roughly city-block precision; detailed browser coordinates are not retained.
  points.push({ lat: Number(coordinates.lat.toFixed(3)), lng: Number(coordinates.lng.toFixed(3)), recordedAt: now });
  writeHistory(userId, points);
  return true;
}

export function startTimelineCapture(userId, onError = () => {}) {
  let stopped = false;
  const capture = () => {
    if (!stopped && isTimelineEnabled(userId)) captureTimelinePoint(userId).catch(onError);
  };
  capture();
  const interval = globalThis.setInterval?.(capture, FIVE_MINUTES_MS);
  return () => {
    stopped = true;
    if (interval !== undefined) globalThis.clearInterval?.(interval);
  };
}
