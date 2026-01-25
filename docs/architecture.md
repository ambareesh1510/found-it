# Architecture

## Components

| Component | Technology | Responsibility |
| --- | --- | --- |
| `frontend/` | React 19, Vite 7, React Router 7, Capacitor 8, plain CSS | Mobile-first SPA, shipped to the web and to iOS (`frontend/ios/`, via Capacitor). Owns routing, forms, map rendering. No business rules. |
| `backend/app/` | Java 21, Spring Boot 4, Spring Security, Spring Data JPA | REST API + chat WebSocket. Owns all authorization, validation, and search logic. |
| `postgres/` | PostgreSQL 18 + PostGIS 3.6 | Durable store. `01-init.sql` seeds the schema on first container start. |
| `docker-compose.yml` | Docker Compose | Local orchestration: `postgres`, `pg-admin`, `laf-backend`, `frontend`. |

Backend packages follow a `domain` / `dto` / `service` / `infra` split per feature
(`auth`, `items`, `claims`, `chat`, `shared`). Controllers live in `infra`. Keep new code
in this shape: a controller must not contain business logic, and a service must not
import `jakarta.servlet`.

## Interactions

```
Browser (React SPA :5173) or iOS app (Capacitor WKWebView, origin capacitor://localhost)
  │  REST: cookie (web) or Authorization: Bearer (iOS)  ──►  Spring Boot :8080  ──►  PostgreSQL/PostGIS :5432
  │  WebSocket /ws/chat?ticket=…   ──►  ChatWebSocketHandler
  └─ Google Maps JS API (browser-side, VITE_GOOGLE_MAPS_API_KEY)

Spring Boot also calls out to:
  • Google OAuth2        — federated login (web redirect flow)
  • Google / Apple JWKS  — verifies the ID tokens sent by the native sign-in buttons
  • Gmail SMTP           — verification + password reset mail
  • HuggingFace Inference — google/vit-base-patch16-224 image auto-tagging (async, optional)
```

Auth state is one JWT, delivered two ways. The web app receives it in an `HttpOnly`
`access_token` cookie. The iOS app receives it in the sign-in response body and keeps it in the
iOS Keychain (`platform/session.js`), sending it as `Authorization: Bearer`. `RequestTokens`
reads either source. WebSockets never take the session token: they use a 60-second ticket
(see [api.md](api.md#chat--chat)).

## Design decisions a developer must not re-litigate

- **Server-side search only.** The loser's category/location/time search is evaluated in
  the backend. Never ship the full found-item list to the client and filter it there —
  that reintroduces the fabrication hole the redesign exists to close.
- **PostGIS for proximity.** Radius queries use `geography(Point,4326)` and
  `ST_DWithin`, not Haversine in Java. See `ItemSpecifications` and `PointUtil`.
- **One `item_posts` table for both sides.** `post_type` (`LOST` | `FOUND`) discriminates.
  Do not create parallel tables.
- **Vision tagging is best-effort.** `VisionTaggingService` runs `@Async` after the post
  is persisted. Post creation must succeed when `VISION_ENABLED=false` or the API errors.
- **`detail_location` is owner-only.** `ItemMapper` returns it only when the viewer is the
  poster. Do not add a new read path that maps items without passing the viewer id.
- **The frontend is also the iOS app.** Capacitor packages the same build for Xcode, so
  avoid browser-only APIs without a Capacitor-compatible fallback, and keep the viewport
  mobile-first.
- **Tokens are never in `localStorage`.** The web token stays in an `HttpOnly` cookie; the
  native token lives in the Keychain. Do not add a third storage location.

## Known architectural gaps

- **Gap — no migration tool.** Schema lives in `postgres/01-init.sql` and
  `spring.jpa.hibernate.ddl-auto: update`. This cannot express the redesign's schema
  changes safely. Resolving this is [task-001](tasks/task-001-schema-migrations.md).
- **Gap — no CI.** There is no `.github/workflows`. Verification is manual; see
  [development.md](development.md).
## Frontend layering (iOS port seam)

The frontend is split so that a Capacitor/iOS build can reuse most of it unchanged:

| Folder | Contents | Rule |
| --- | --- | --- |
| `src/core/` | Config, `apiRequest` / `authFetch` (adds the bearer token), endpoint wrappers (`services.js`), domain rules (`lostSearch.js`, `reports.js`, `categories.js`, `eventTime.js`) | Plain JavaScript. No React, no DOM, no `window`, no `localStorage`. Covered by `npm test`. |
| `src/platform/` | `session.js` (Keychain token), `socialSignIn.js` (native Google/Apple), `geolocation.js` (`@capacitor/geolocation`), `photos.js` (`@capacitor/camera` + FileReader), `places.js` | The only device-specific code. Each file exports one stable function; web and native share the signature and branch on `Capacitor.isNativePlatform()` inside. |
| `src/theme/`, `src/ui/` | Design tokens (`tokens.css`), flow styles (`flows.css`), presentational components | No API calls. Props in, markup out. |
| `src/features/<flow>/` | Screens: `home`, `lost`, `found`, `reports` | Compose `core` + `ui` + `platform`. Must not call `fetch` directly. |

Top-level navigation: `core/navigation.js` defines the four tabs (Home, Map, Messages, Profile) and maps a path to its tab (`tabForPath`). `ui/AppTabBar.jsx` renders the bar, and `App.jsx` shows it only for signed-in users on a tab section (item detail, create/edit, and auth screens have no bar). A native iOS tab bar can read the same `TABS` list.

Rules for new code:

- Business rules (validation, payload shape, search window) go in `core/`, not in a page.
- Pages call functions from `core/services.js`, never `fetch`. Screens that need the raw
  `Response` use `authFetch` from `core/http.js`, which adds the session token.
- Device APIs (location, maps, camera, storage) go behind `src/platform/` with a
  documented signature so the native build can swap the implementation.
- Keep API payloads and error handling in `core/http.js` so there is one place to
  change for native networking.
