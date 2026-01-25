import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  buildCreateItemRequest,
  formatRewardCents,
  isPhotoSource,
  parseRewardDollars,
  validateReport,
  POST_TYPES,
} from '../reports.js';
import { MAX_PHOTO_BYTES, validatePhotoFile } from '../photos.js';

const NOW = new Date(2026, 9, 13, 12, 0); // 2026-10-13 12:00 local
const PLACE = { text: 'Bobby Dodd Stadium', lat: 33.7726, lng: -84.3929 };

const BASE = {
  postType: POST_TYPES.FOUND,
  title: 'Black wallet',
  category: 'Documents',
  description: '  Leather, has a GT sticker  ',
  imageUrl: 'https://example.com/wallet.jpg',
  rewardDollars: '',
  location: PLACE,
  date: '2026-10-12',
  time: '18:15',
  now: NOW,
};

test('a complete found report with a photo link is valid', () => {
  assert.deepEqual(validateReport(BASE), { valid: true, errors: {} });
});

test('the photo is optional for both lost and found reports', () => {
  for (const postType of [POST_TYPES.LOST, POST_TYPES.FOUND]) {
    assert.equal(validateReport({ ...BASE, postType, imageUrl: '' }).valid, true);
    assert.equal(validateReport({ ...BASE, postType, imageUrl: undefined }).valid, true);
  }
});

test('a photo that is neither an http link nor an image data URL is rejected', () => {
  assert.ok(validateReport({ ...BASE, imageUrl: 'javascript:alert(1)' }).errors.imageUrl);
  assert.ok(validateReport({ ...BASE, imageUrl: 'ftp://example.com/a.jpg' }).errors.imageUrl);
  assert.equal(validateReport({ ...BASE, imageUrl: 'data:image/png;base64,iVBORw0KGgo=' }).valid, true);
});

test('a blank title is rejected', () => {
  assert.ok(validateReport({ ...BASE, title: '   ' }).errors.title);
});

test('a time more than the clock-skew allowance in the future is rejected', () => {
  const later = validateReport({ ...BASE, date: '2026-10-13', time: '12:01' });
  assert.match(later.errors.time, /future/);

  const within = validateReport({ ...BASE, date: '2026-10-13', time: '12:00' });
  assert.equal(within.errors.time, undefined);
});

test('a reward is optional and checked only for lost reports', () => {
  assert.equal(validateReport({ ...BASE, postType: POST_TYPES.LOST, rewardDollars: '50' }).valid, true);
  assert.equal(validateReport({ ...BASE, postType: POST_TYPES.LOST, rewardDollars: 'abc' }).valid, false);
  // Found reports never show the reward field, so an invalid value there is ignored.
  assert.equal(validateReport({ ...BASE, postType: POST_TYPES.FOUND, rewardDollars: 'abc' }).valid, true);
});

test('parseRewardDollars converts dollars to whole cents', () => {
  assert.deepEqual(parseRewardDollars(''), { cents: null, error: null });
  assert.deepEqual(parseRewardDollars('  '), { cents: null, error: null });
  assert.deepEqual(parseRewardDollars('50'), { cents: 5000, error: null });
  assert.deepEqual(parseRewardDollars('12.5'), { cents: 1250, error: null });
  assert.deepEqual(parseRewardDollars('0.1'), { cents: 10, error: null });
});

test('parseRewardDollars rejects negatives, extra decimals, and amounts over the cap', () => {
  assert.ok(parseRewardDollars('-5').error);
  assert.ok(parseRewardDollars('1.234').error);
  assert.ok(parseRewardDollars('$50').error);
  assert.ok(parseRewardDollars('100001').error);
  assert.equal(parseRewardDollars('100000').cents, 10000000);
});

test('formatRewardCents shows whole dollars without decimals and hides zero', () => {
  assert.equal(formatRewardCents(5000), '$50');
  assert.equal(formatRewardCents(1250), '$12.50');
  assert.equal(formatRewardCents(0), null);
  assert.equal(formatRewardCents(null), null);
});

test('buildCreateItemRequest trims text and sends the post type', () => {
  const body = buildCreateItemRequest(BASE);

  assert.equal(body.postType, 'FOUND');
  assert.equal(body.title, 'Black wallet');
  assert.equal(body.description, 'Leather, has a GT sticker');
  assert.equal(body.imageUrl, 'https://example.com/wallet.jpg');
  assert.equal(body.locationText, 'Bobby Dodd Stadium');
  assert.equal(body.latitude, PLACE.lat);
  assert.equal(body.longitude, PLACE.lng);
  assert.equal(body.detailLocation, null);
  assert.equal(body.rewardCents, null);
  assert.match(body.eventTime, /Z$/);
  assert.equal(new Date(body.eventTime).getTime(), new Date(2026, 9, 12, 18, 15).getTime());
});

test('buildCreateItemRequest sends the reward in cents for lost reports only', () => {
  const lost = buildCreateItemRequest({ ...BASE, postType: POST_TYPES.LOST, rewardDollars: '25.50' });
  assert.equal(lost.rewardCents, 2550);

  const found = buildCreateItemRequest({ ...BASE, postType: POST_TYPES.FOUND, rewardDollars: '25.50' });
  assert.equal(found.rewardCents, null);
});

test('blank optional values are sent as null, including an empty photo', () => {
  const body = buildCreateItemRequest({
    ...BASE,
    postType: POST_TYPES.LOST,
    description: '   ',
    imageUrl: '   ',
    location: { text: '  ', lat: PLACE.lat, lng: PLACE.lng },
  });
  assert.equal(body.description, null);
  assert.equal(body.imageUrl, null);
  assert.equal(body.locationText, null);
});

test('isPhotoSource accepts http links and image data URLs only', () => {
  assert.equal(isPhotoSource('https://example.com/a.jpg'), true);
  assert.equal(isPhotoSource('data:image/jpeg;base64,/9j/4AAQ'), true);
  assert.equal(isPhotoSource('data:text/html;base64,PGgxPg=='), false);
  assert.equal(isPhotoSource('javascript:alert(1)'), false);
  assert.equal(isPhotoSource(''), false);
});

test('validatePhotoFile requires an image under the size limit', () => {
  assert.match(validatePhotoFile(undefined), /Choose a photo/);
  assert.match(validatePhotoFile({ type: 'application/pdf', size: 10 }), /image file/);
  assert.match(validatePhotoFile({ type: 'image/png', size: MAX_PHOTO_BYTES + 1 }), /under 4 MB/);
  assert.equal(validatePhotoFile({ type: 'image/png', size: 1000 }), null);
});
