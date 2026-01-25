// Top-level tabs. Shared definition so the web tab bar and a future native (Capacitor/iOS) tab bar
// show the same sections. Pure data and functions only: no React, no DOM.
export const TABS = Object.freeze([
  // The Home tab also owns the lost and found flows, so those screens keep the tab highlighted.
  { id: 'home', label: 'Home', path: '/', sectionPrefixes: ['/lost', '/found'] },
  { id: 'map', label: 'Map', path: '/map', sectionPrefixes: [] },
  { id: 'messages', label: 'Messages', path: '/chat', sectionPrefixes: [] },
  { id: 'profile', label: 'Profile', path: '/profile', sectionPrefixes: [] },
]);

/** Returns the tab a pathname belongs to, or null when the screen has no tab bar (item detail, auth, etc.). */
export function tabForPath(pathname) {
  const path = (pathname || '/').replace(/\/+$/, '') || '/';
  return (
    TABS.find(
      (tab) =>
        tab.path === path ||
        tab.sectionPrefixes.some((prefix) => path === prefix || path.startsWith(`${prefix}/`)),
    ) ?? null
  );
}
