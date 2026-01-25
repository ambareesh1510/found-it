import { test } from 'node:test';
import assert from 'node:assert/strict';
import { formatEventTime, shiftIsoHours, toIsoFromLocal, toLocalDateInputValue } from '../eventTime.js';

test('formatEventTime renders local date and time as in the finder card', () => {
  const iso = new Date(2026, 8, 15, 14, 30).toISOString();
  assert.equal(formatEventTime(iso), '2026-09-15 · 14:30');
});

test('formatEventTime returns an empty string for missing or invalid input', () => {
  assert.equal(formatEventTime(null), '');
  assert.equal(formatEventTime('not a date'), '');
});

test('toIsoFromLocal interprets date and time as local time', () => {
  const iso = toIsoFromLocal('2026-09-15', '14:30');
  assert.equal(new Date(iso).getHours(), 14);
  assert.equal(new Date(iso).getMinutes(), 30);
});

test('toIsoFromLocal returns null when either part is missing', () => {
  assert.equal(toIsoFromLocal('', '14:30'), null);
  assert.equal(toIsoFromLocal('2026-09-15', ''), null);
});

test('shiftIsoHours moves by whole hours', () => {
  const iso = '2026-09-15T12:00:00.000Z';
  assert.equal(shiftIsoHours(iso, -12), '2026-09-15T00:00:00.000Z');
  assert.equal(shiftIsoHours(iso, 12), '2026-09-16T00:00:00.000Z');
});

test('toLocalDateInputValue zero-pads month and day', () => {
  assert.equal(toLocalDateInputValue(new Date(2026, 0, 5, 9, 0)), '2026-01-05');
});
