import { test } from 'node:test';
import assert from 'node:assert/strict';
import { TABS, tabForPath } from '../navigation.js';

test('the four tabs are Home, Map, Messages, Profile in that order', () => {
  assert.deepEqual(
    TABS.map((tab) => tab.label),
    ['Home', 'Map', 'Messages', 'Profile'],
  );
});

test('each tab root maps to its own tab', () => {
  assert.equal(tabForPath('/').id, 'home');
  assert.equal(tabForPath('/map').id, 'map');
  assert.equal(tabForPath('/chat').id, 'messages');
  assert.equal(tabForPath('/profile').id, 'profile');
});

test('lost and found flow screens belong to the Home tab', () => {
  assert.equal(tabForPath('/lost').id, 'home');
  assert.equal(tabForPath('/lost/where').id, 'home');
  assert.equal(tabForPath('/lost/results').id, 'home');
  assert.equal(tabForPath('/found').id, 'home');
  assert.equal(tabForPath('/found/report').id, 'home');
});

test('trailing slashes do not change the tab', () => {
  assert.equal(tabForPath('/map/').id, 'map');
  assert.equal(tabForPath('/profile/').id, 'profile');
});

test('screens outside the tabs have no tab bar', () => {
  assert.equal(tabForPath('/items/12'), null);
  assert.equal(tabForPath('/create'), null);
  assert.equal(tabForPath('/login'), null);
  assert.equal(tabForPath('/reset-password/abc'), null);
  // Prefix matching must be whole path segments, not string prefixes.
  assert.equal(tabForPath('/lostandfound'), null);
});
