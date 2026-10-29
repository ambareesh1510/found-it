# API

Base URL `http://localhost:8080` in development (set `VITE_API_URL` for the iOS build). Web
calls use `credentials: 'include'` and the `access_token` cookie. The iOS app sends the same
JWT as `Authorization: Bearer <token>`; every authenticated endpoint accepts either one.
Errors are JSON `ApiError` bodies produced by `GlobalExceptionHandler`.

## Conventions

- `POST`/`PATCH` bodies are JSON records validated with `jakarta.validation`; a violation
  returns `400` with the message from the annotation.
- `401` means no/invalid session (cookie or bearer token), `403` means authenticated but not the owner.
- Timestamps are ISO-8601 `Instant` (UTC). Coordinates are WGS84 decimal degrees.

## Auth — `/auth`

| Method | Path | Notes |
| --- | --- | --- |
| POST | `/auth/register` | `RegisterDTO`. Creates a **disabled** user and emails a verification link. Returns the username. `400` if the password breaks the policy (12+ chars, upper, lower, special) or a field is missing; `409` if the email or username is taken. |
| POST | `/auth/verify` | `VerifyEmailDTO` `{ token }`. Enables the account. `400` if the token is unknown, used, or expired. |
| POST | `/auth/resend-verification` | `ResendVerificationDTO` `{ email }`. Sends a new link to an unverified account. Always `200` with the same message. |
| POST | `/auth/login` | `LoginDTO` `{ usernameOrEmail, password }`. Sets the `access_token` cookie and returns `LoginResponseDTO` `{ token, username }`; the native app stores `token`. `401` wrong email or password. `403` (`Account Not Verified`) correct password but unverified. `400` malformed body. |
| POST | `/auth/google` | Native Google sign-in. `IdTokenLoginDTO` `{ idToken }`, the ID token from the Google SDK. The server checks the signature (Google JWKS), issuer, audience (`GOOGLE_CLIENT_IDS`), expiry, and `email_verified`. Returns `LoginResponseDTO` (no cookie). Links to an existing enabled account with the same email. `409` if that email belongs to an unverified local account or to another sign-in method. `400` missing token; `401` token rejected. |
| POST | `/auth/apple` | Native Sign in with Apple. `{ idToken, authorizationCode }`, the identity token and the one-time code from the same sign-in (`authorizationCode` is optional, but without it the Apple link cannot be revoked on deletion). The server exchanges the code for a refresh token. Otherwise the same rules as `/auth/google`. Audience is `APPLE_BUNDLE_IDS`. If the user hides their email, the token carries Apple's private relay address, and that is what is stored. |
| POST | `/auth/logout` | Blacklists the `jti` (cookie or bearer token), clears the cookie. |
| POST | `/auth/forgot-password` | `ForgotPasswordDTO` `{ email }`. Emails a 1-hour reset link. Always `200` with the same message. |
| POST | `/auth/reset-password` | `ResetPasswordDTO` `{ token, password }`. Sets the new password and enables the account. `400` if the token is invalid or expired, or the password breaks the policy. |
| GET | `/oauth2/authorization/google` | Starts Google OAuth2; `OAuth2SuccessHandler` issues the cookie. |

## Users — `/users`

`GET /users/me` → `CurrentUserResponseDTO` (the session check the SPA runs on boot).
`GET /users/{userId}` → `UserProfileResponseDTO`.
`DELETE /users/me` → `204`. Anonymizes the signed-in account, deletes its declared searches, revokes the
current token, and revokes any Sign in with Apple link at Apple (best-effort; a failure is logged and does not block deletion). The row and its posts, claims, and threads stay. `401` without a session.
See [security.md](security.md#data-handling--privacy).

## Items — `/items`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/items` | owner | `CreateItemDTO` → `ItemPostResponseDTO`. |
| PATCH | `/items/{itemId}` | owner | `UpdateItemDTO`; path `itemId` must equal body `itemId`. |
| DELETE | `/items/{itemId}` | owner | `204`. |
| GET | `/items/{itemId}` | public | Single post. `detailLocation` is returned only to the poster. |
| GET | `/items` | public | Optional filters `status`, `category`, `userId`, `postType` (`LOST` or `FOUND`; anything else → 400). |
| GET | `/items/nearby` | public | Required `lon`, `lat`, `radiusMeters`. PostGIS `ST_DWithin`. `detailLocation` only for the poster. |

`CreateItemDTO`: `title*`, `description`, `category*`, `imageUrl` (optional), `imageUrl2`,
`latitude*`, `longitude*`, `locationText`, `detailLocation`, `eventTime*` (not future),
`postType` (`LOST` | `FOUND`; omitted → `LOST` for older clients), `rewardCents` (optional).

- `imageUrl` is optional. When given it must be an `http(s)://` link or an inline
  `data:image/...;base64,` string (the create page uploads files this way). Blank is stored as no photo.
- `rewardCents` is an integer from 0 to 10,000,000 ($100,000). It is accepted only for `LOST`
  posts; a reward on a `FOUND` post returns 400. The service also drops it for `FOUND`.

`ItemPostResponseDTO`: `itemId`, `posterId`, `posterUsername`, `title`, `description`,
`category`, `status`, `imageUrl` (nullable), `imageUrl2`, `latitude`, `longitude`, `locationText`,
`detailLocation` (**non-null only when the viewer is the poster**), `eventTime`, `createdAt`,
`updatedAt`, `postType`, `rewardCents` (nullable).

> **Rule: only the poster sees `detailLocation`.** It is stripped in `ItemMapper` for every other
> viewer (anonymous, other users, claimants) on `GET /items`, `GET /items/{id}`, `GET /items/nearby`,
> and claim responses. Clients must not rely on hiding it themselves.

> **`postType` is built.** It is on `CreateItemDTO`, `ItemPostResponseDTO`, and the
> `GET /items` filter. Clients must send it explicitly. The silent `LOST` default exists
> only for backward compatibility.

## Claims

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/claims` | any user | `CreateClaimDTO`. One claim per user per item. |
| DELETE | `/claims/{claimId}` | claimant | |
| PATCH | `/claims/{claimId}/accept` | post owner | |
| PATCH | `/claims/{claimId}/reject` | post owner | |
| GET | `/items/{itemId}/claims` | post owner | |
| GET | `/claims/me` | claimant | |

## Chat — `/chat`

| Method | Path | Notes |
| --- | --- | --- |
| GET | `/chat/threads` | Current user's threads; optional `?itemId=` narrows to one item. |
| POST | `/chat/threads` | `CreateChatThreadDTO`. Idempotent per (item, user pair). |
| POST | `/chat/items/{itemId}/thread` | "Contact" button. Opens or reuses the thread with the item's poster. The server looks up the poster, so the client sends no poster id. Rejected when the caller is the poster. |
| GET | `/chat/threads/{threadId}/messages` | Participants only. |
| POST | `/chat/threads/{threadId}/messages` | `SendChatMessageDTO`. |

| POST | `/chat/ws-ticket` | Returns `{ ticket }`, a JWT valid for **60 seconds** that opens the socket once. Request a fresh ticket before every connection, including reconnects. `401` without a session. |

Live delivery runs over a WebSocket at `/ws/chat?ticket=<ticket>`, handled by
`ChatWebSocketHandler`. `ChatWebSocketAuthHandshakeInterceptor` accepts only a ticket, so a
cookie or session token in the URL is rejected. Inbound and
outbound frames are `ChatSocketInboundMessage` / `ChatSocketOutboundEvent`. REST remains
the source of truth; the socket is a delivery optimization, so a client that only polls
REST must still work.

## Searches — `/searches`

| Method | Path | Auth | Notes |
| --- | --- | --- | --- |
| POST | `/searches/lost` | logged in | `LostSearchRequestDTO` → `LostSearchResultDTO`. |

`LostSearchRequestDTO` has two modes. Manual search sends `category`, `latitude`,
`longitude`, `radiusMeters`, ordered `windowStart`/`windowEnd`, and `useTimeline: false`.
Timeline search sends `category`, `useTimeline: true`, and 1–300 `timelinePoints` records
(`latitude`, `longitude`, `recordedAt`); manual fields are null. The server ignores points
outside the previous 24 hours and enforces a radius of 804.672 metres (0.5 miles).

The call persists the declaration in `lost_searches` **before** it runs the search, so a
search cannot happen without a record. It then returns `LostSearchResultDTO`:
`{ searchId, results: FoundItemSummaryDTO[] }`. The matching rule is `postType = FOUND`,
`status = OPEN`, same category (case-insensitive), `eventTime` inside the window, and a
location inside the radius.

For Timeline searches, a found post matches when it is within 0.5 miles of any submitted
point from the previous 24 hours. Submitted points are used only for the request and are
never persisted. The saved declaration has a null `location` and `usedTimeline: true`.

`FoundItemSummaryDTO` is deliberately narrow: `itemId`, `title`, `description`, `category`,
`imageUrl`, `locationText`, `eventTime`. It omits `detailLocation`, coordinates, and poster
identity, so a loser cannot reach a finder before a claim is made.

## Endpoints the redesign requires — not yet built

- **`GET`/`PATCH /users/me/settings`** — the Settings screen toggles.
