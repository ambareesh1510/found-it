import { LOST_CATEGORIES } from './categories.js';
import { isValidLocation } from './location.js';
import { shiftIsoHours, toIsoFromLocal } from './eventTime.js';

// Assumptions (not in the docs, so confirm with the client):
// - 1 mile, the same default radius the map view already uses.
// - The search window is +/- 12 hours around the time the user says they lost the item.
export const SEARCH_RADIUS_METERS = 1609;
export const SEARCH_WINDOW_HOURS = 12;
export const TIMELINE_RADIUS_METERS = 804.672;

/**
 * Validates the "Where & When" step of the I Lost flow.
 * Timeline search (use location history) is not built yet, so only the manual path is validated.
 */
export function validateLostDeclaration({ category, location, date, time, now = new Date() }) {
  const errors = {};

  if (!LOST_CATEGORIES.includes(category)) {
    errors.category = 'Choose a category.';
  }
  if (!isValidLocation(location)) {
    errors.location = 'Choose a place from the suggestions or use your current location.';
  }
  if (!date) {
    errors.date = 'Enter the date you lost it.';
  }
  if (!time) {
    errors.time = 'Enter the time you lost it.';
  }
  if (date && time) {
    const iso = toIsoFromLocal(date, time);
    if (!iso) {
      errors.time = 'Enter a valid time.';
    } else if (new Date(iso) > now) {
      errors.time = 'The time you lost it cannot be in the future.';
    }
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

/** Builds the `POST /searches/lost` body. Only call this after `validateLostDeclaration` passes. */
export function buildLostSearchRequest({ category, location, date, time }) {
  const center = toIsoFromLocal(date, time);
  return {
    category,
    latitude: location.lat,
    longitude: location.lng,
    radiusMeters: SEARCH_RADIUS_METERS,
    windowStart: shiftIsoHours(center, -SEARCH_WINDOW_HOURS),
    windowEnd: shiftIsoHours(center, SEARCH_WINDOW_HOURS),
    useTimeline: false,
    timelinePoints: null,
  };
}

export function buildTimelineSearchRequest({ category, timelinePoints }) {
  return {
    category,
    latitude: null,
    longitude: null,
    radiusMeters: null,
    windowStart: null,
    windowEnd: null,
    useTimeline: true,
    timelinePoints,
  };
}
