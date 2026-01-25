import { LOST_CATEGORIES } from './categories.js';
import { isValidLocation } from './location.js';
import { toIsoFromLocal } from './eventTime.js';

export const POST_TYPES = Object.freeze({ LOST: 'LOST', FOUND: 'FOUND' });

// The backend accepts a few seconds of clock skew on eventTime, so mirror that here.
const CLOCK_SKEW_MS = 5000;

// A photo is either an http(s) link or an inline image (the file picker produces a data URL).
// Must stay in sync with CreateItemDTO.imageUrl on the backend.
const PHOTO_SOURCE = /^(https?:\/\/\S+|data:image\/[a-z0-9.+-]+;base64,\S+)$/i;

// Reward cap matches the backend (10,000,000 cents). Confirm with the client.
export const MAX_REWARD_DOLLARS = 100000;

export function isPhotoSource(value) {
  return typeof value === 'string' && PHOTO_SOURCE.test(value.trim());
}

/** Parses a dollar amount typed by the user. Blank means no reward. Returns { cents, error }. */
export function parseRewardDollars(text) {
  const trimmed = (text ?? '').trim();
  if (trimmed === '') return { cents: null, error: null };

  if (!/^\d+(\.\d{1,2})?$/.test(trimmed)) {
    return { cents: null, error: 'Enter an amount in dollars, like 25 or 25.50.' };
  }
  const dollars = Number(trimmed);
  if (dollars > MAX_REWARD_DOLLARS) {
    return { cents: null, error: `The reward cannot be more than $${MAX_REWARD_DOLLARS.toLocaleString('en-US')}.` };
  }
  return { cents: Math.round(dollars * 100), error: null };
}

/** Formats a reward for display: 5000 -> "$50", 1250 -> "$12.50". Null when there is no reward. */
export function formatRewardCents(cents) {
  if (!cents || cents <= 0) return null;
  const dollars = cents / 100;
  return Number.isInteger(dollars) ? `$${dollars}` : `$${dollars.toFixed(2)}`;
}

/**
 * Validates a "report lost" or "report found" form.
 * The photo is optional for both types. The reward is optional and applies to LOST only.
 */
export function validateReport({
  postType,
  title,
  category,
  imageUrl,
  rewardDollars,
  location,
  date,
  time,
  now = new Date(),
}) {
  const errors = {};

  if (!title || !title.trim()) {
    errors.title = 'Enter a name for the item.';
  } else if (title.trim().length > 255) {
    errors.title = 'Keep the name under 255 characters.';
  }
  if (!LOST_CATEGORIES.includes(category)) {
    errors.category = 'Choose a category.';
  }
  if (imageUrl && imageUrl.trim() && !isPhotoSource(imageUrl)) {
    errors.imageUrl = 'Use a photo link that starts with https://, or upload an image.';
  }
  if (postType === POST_TYPES.LOST && rewardDollars !== undefined) {
    const { error } = parseRewardDollars(rewardDollars);
    if (error) errors.reward = error;
  }
  if (!isValidLocation(location)) {
    errors.location = 'Choose a place from the suggestions or use your current location.';
  }
  if (!date) {
    errors.date = 'Enter a date.';
  }
  if (!time) {
    errors.time = 'Enter a time.';
  }
  if (date && time) {
    const iso = toIsoFromLocal(date, time);
    if (!iso) {
      errors.time = 'Enter a valid time.';
    } else if (new Date(iso).getTime() > now.getTime() + CLOCK_SKEW_MS) {
      errors.time = 'The time cannot be in the future.';
    }
  }

  return { valid: Object.keys(errors).length === 0, errors };
}

/** Builds the `POST /items` body. Only call this after `validateReport` passes. */
export function buildCreateItemRequest({
  postType,
  title,
  description,
  category,
  imageUrl,
  rewardDollars,
  location,
  date,
  time,
}) {
  const photo = imageUrl?.trim() ? imageUrl.trim() : null;
  const reward = postType === POST_TYPES.LOST ? parseRewardDollars(rewardDollars).cents : null;

  return {
    postType,
    title: title.trim(),
    description: description?.trim() ? description.trim() : null,
    category,
    imageUrl: photo,
    rewardCents: reward,
    latitude: location.lat,
    longitude: location.lng,
    locationText: location.text?.trim() ? location.text.trim() : null,
    detailLocation: null,
    eventTime: toIsoFromLocal(date, time),
  };
}
