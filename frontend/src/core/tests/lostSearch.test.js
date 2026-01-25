import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildLostSearchRequest,
  buildTimelineSearchRequest,
  validateLostDeclaration,
  SEARCH_RADIUS_METERS,
} from '../lostSearch.js';

const NOW = new Date(2026, 9, 13, 12, 0); // 2026-10-13 12:00 local
const PLACE = { text: 'Clough Undergraduate Learning Commons', lat: 33.7756, lng: -84.3963 };

test('empty declaration reports every required field', () => {
  const { valid, errors } = validateLostDeclaration({
    category: '',
    location: { text: '', lat: null, lng: null },
    date: '',
    time: '',
    now: NOW,
  });
  assert.equal(valid, false);
  assert.ok(errors.category);
  assert.ok(errors.location);
  assert.ok(errors.date);
  assert.ok(errors.time);
});

test('buildTimelineSearchRequest sends points without a manual declaration', () => {
  const timelinePoints = [{ latitude: 33.775, longitude: -84.396, recordedAt: '2026-10-21T12:00:00.000Z' }];
  assert.deepEqual(buildTimelineSearchRequest({ category: 'Keys', timelinePoints }), {
    category: 'Keys',
    latitude: null,
    longitude: null,
    radiusMeters: null,
    windowStart: null,
    windowEnd: null,
    useTimeline: true,
    timelinePoints,
  });
});

test('complete past declaration is valid', () => {
  const result = validateLostDeclaration({
    category: 'Keys',
    location: PLACE,
    date: '2026-10-12',
    time: '14:30',
    now: NOW,
  });
  assert.deepEqual(result, { valid: true, errors: {} });
});

test('a time after now is rejected', () => {
  const { valid, errors } = validateLostDeclaration({
    category: 'Keys',
    location: PLACE,
    date: '2026-10-14',
    time: '09:00',
    now: NOW,
  });
  assert.equal(valid, false);
  assert.match(errors.time, /future/);
});

test('a category outside the canonical list is rejected', () => {
  const { errors } = validateLostDeclaration({
    category: 'Phones',
    location: PLACE,
    date: '2026-10-12',
    time: '10:00',
    now: NOW,
  });
  assert.ok(errors.category);
});

test('coordinates outside the valid range are rejected', () => {
  const { errors } = validateLostDeclaration({
    category: 'Keys',
    location: { text: 'Somewhere', lat: 123, lng: 0 },
    date: '2026-10-12',
    time: '10:00',
    now: NOW,
  });
  assert.ok(errors.location);
});

test('search request uses a 12 hour window around the declared time', () => {
  const body = buildLostSearchRequest({
    category: 'Keys',
    location: PLACE,
    date: '2026-10-12',
    time: '14:30',
  });

  assert.equal(body.category, 'Keys');
  assert.equal(body.latitude, PLACE.lat);
  assert.equal(body.longitude, PLACE.lng);
  assert.equal(body.radiusMeters, SEARCH_RADIUS_METERS);
  assert.equal(body.useTimeline, false);

  const start = new Date(body.windowStart).getTime();
  const end = new Date(body.windowEnd).getTime();
  const center = new Date(2026, 9, 12, 14, 30).getTime();
  assert.equal(center - start, 12 * 3600 * 1000);
  assert.equal(end - center, 12 * 3600 * 1000);
});
