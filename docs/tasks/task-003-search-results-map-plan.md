# Task 003 — Lost-search results map

**Status:** Approved by the client on 2026-11-18.

## Decisions

- Remove the global Map tab and global item map. An old `/map` URL redirects to Home.
- Show a map only after a lost-item declaration has been persisted and matching results
  have been returned.
- The gated lost-search response may include rounded coordinates (three decimal places,
  approximately city-block precision) for matching found posts. Exact coordinates,
  `detailLocation`, `imageUrl2`, and poster identity remain withheld.
- Manual results show the declared point and radius. Timeline results draw the submitted
  device-local points as a chronological path. Timeline points remain transient and are not
  persisted by the backend.

## Implementation plan

1. Add rounded latitude/longitude to `FoundItemSummaryDTO` and map them only in the
   declaration-gated `POST /searches/lost` response.
2. Carry Timeline points from the where/when screen to the results route state.
3. Add a mobile-first results map with matching-item markers, manual search point/radius,
   Timeline polyline, viewport fitting, and inline unavailable/loading states.
4. Connect marker selection to result cards without changing the contact flow.
5. Remove Map from top-level navigation and redirect `/map` to Home.
6. Update API, security, UI, architecture, and user-flow documentation.
7. Run backend tests, frontend tests/lint/build, Docker startup, and manual 390 px checks.
