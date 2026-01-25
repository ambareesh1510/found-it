# Task 002 — Browser-local location timeline and Timeline search

**Status:** Approved by the client on 2026-10-21.

## Decisions

- The web implementation records location only while the app is open. Reliable background
  capture is deferred to the future Capacitor/native integration.
- Location history remains on the user's device. The client sends recent points only with a
  Timeline search request; the server uses them transiently and never stores them.
- Capture at most once every five minutes and retain only the most recent seven days locally.
- Timeline search uses points from the previous 24 hours and matches open found reports of the
  chosen category within 0.5 miles (804.672 metres) of any point.
- The normal lost-search audit row remains mandatory. Timeline rows set `location` to null and
  record category, the server-controlled window and radius, and `used_timeline = true`.

## Implementation plan

1. Make `lost_searches.location` nullable for Timeline audit rows and document the constraint.
2. Add a browser platform adapter for opt-in local history, five-minute foreground capture,
   seven-day pruning, and permission/error states.
3. Start and stop foreground capture with the authenticated application lifecycle.
4. Enable mutually exclusive manual and Timeline modes on the lost-item where/when screen.
5. Extend `POST /searches/lost` to accept Timeline points, enforce the 24-hour/0.5-mile rules
   server-side, save the declaration before returning results, and run proximity matching in
   PostGIS without persisting submitted points.
6. Update API, data-model, security, and flow documentation.
7. Add backend service/controller validation coverage and frontend core/platform tests. Run
   backend tests and frontend tests, lint, and production build, then manually verify at 390 px.

## Risks

- Browser timers and geolocation stop when the page is suspended or closed; the UI must state
  that this test implementation records only while FindIt is open.
- The repository does not yet have approved migration tooling. The schema source and entity will
  be kept aligned; existing developer databases may require the documented local reset until
  task 001 introduces versioned migrations.
