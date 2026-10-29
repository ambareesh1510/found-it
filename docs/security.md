# Security & Privacy

## Authentication

- Credentials are verified by `AuthenticationService` against BCrypt hashes; Google
  accounts go through `OAuth2AuthenticationService` and have no local password.
- **Web.** A successful login issues a JWT in an **`HttpOnly`, `SameSite`, `Secure`-in-prod
  `access_token` cookie** (`CookieUtils`). The web app never reads the token.
- **Native iOS.** The same JWT is also returned in the login response body. The app stores it in
  the iOS Keychain (`@aparajita/capacitor-secure-storage`) and sends `Authorization: Bearer`.
  It is never written to `localStorage`. `RequestTokens` accepts the header first and the cookie second.
- **Native sign-in (Google and Sign in with Apple).** `POST /auth/google` and `POST /auth/apple`
  accept the ID token from the provider's native SDK. `SocialIdTokenVerifier` checks the signature
  against the provider's published keys (JWKS), then the issuer, audience (`GOOGLE_CLIENT_IDS` or
  `APPLE_BUNDLE_IDS`), expiry, and `email_verified` (Google). Any failure returns `401`.
  A social sign-in links to an existing local account with the same email only if that account is
  already enabled. Otherwise it is refused, so an unverified registration cannot be taken over.
- **WebSocket tickets.** `POST /chat/ws-ticket` issues a JWT with `typ=ws`, valid for 60 seconds.
  The chat socket accepts only `?ticket=`. Session tokens and cookies are rejected there, so the
  long-lived token never appears in a URL. `JwtAuthFilter` ignores ticket tokens for REST calls.
- Signing: HS256 (`HS256JwtService`) in development, ES256 (`ES256JwtService`) in
  production. `security.jwt.is-prod` selects the implementation. Production must not run
  on the shared HMAC secret.
- Logout writes the token's `jti` to `jwt_token_blacklist`; `JwtAuthFilter` rejects
  blacklisted tokens. Expiry is `JWT_EXPIRATION_TIME` (one day).
- **Email verification.** Registration creates the user with `enabled = false` and emails a
  24-hour verification link (`/verify-email?token=...`). `POST /auth/verify` enables the
  account. Login with the correct password but an unverified account returns `403`.
  Forgot-password emails a 1-hour reset link (`/reset-password/:token`); `POST
  /auth/reset-password` sets the new password and enables the account.
  Social sign-in does not need verification: Google and Apple have already verified the email.
- **Tokens are single-use and stored hashed.** `TokenUtil` generates 32 random bytes
  (base64url). Only the SHA-256 hash is stored, in `verification_code` or `reset_token`,
  with an expiry. Using a token clears it, and an expired token is rejected.
- **Enumeration resistance.** Forgot-password and resend-verification return the same
  message whether or not the email exists. Login checks the password before revealing
  that the account is unverified, so a wrong password gets the generic `401`.
- **Password policy.** At least 12 characters, with at least one uppercase letter, one
  lowercase letter, and one special character (anything that is not a letter, digit, or
  whitespace). `PasswordPolicy` enforces this on the server for registration and reset.
  The frontend checklist (`passwordPolicy.js`) mirrors it but is advisory only.

## Authorization

The session is stateless (`SessionCreationPolicy.STATELESS`). `AuthConfig` permits
`/auth/**`, `/oauth2/**`, `/error`, and **all `GET /items/**`** anonymously; everything
else requires a valid session, either the cookie or a bearer token. Ownership checks live in the service layer, not the
controller — `ItemServiceJPAImpl` and `ClaimServiceJPAImpl` compare the authenticated
`userId` against the row and throw `UserMismatchException` (→ `403`) on mismatch.

Rules to preserve:
- Only the poster may edit or delete a post, and never once it is `RESOLVED`.
- Only the post owner may accept or reject a claim; only the claimant may delete theirs.
- Only thread participants may read or write that thread's messages, over REST and over
  the WebSocket alike.

> **Gap — anonymous item reads undermine the redesign.** `GET /items/**` being public
> means found-item details are readable without ever filing a declared search, which is
> exactly the loophole the new flow closes. The loser search endpoint must require
> authentication, and public `GET` on found items should be narrowed to non-identifying
> fields or removed.

## The anti-false-claim model

The redesign's security property is procedural, not cryptographic:

1. A loser must declare **category, location, and time window** before any found item is
   shown. The declaration is persisted in `lost_searches` with a server timestamp.
2. Results are filtered server-side; the client never receives non-matching posts.
3. Found-item listings withhold precise detail until contact is established — the
   `detail_location` field and the second photo are the finder's verification secret and
   must not appear in list responses.
4. A finder reviewing a claim can compare it against the claimant's recorded search.

Any change that lets a client enumerate found items, or that returns
`detail_location`/`imageUrl2` in a list payload, breaks this model. Treat such a change
as a security regression.

## Data handling & privacy

- **Location history is opt-in and device-local.** The browser test implementation records
  coarse points at most every five minutes only while FindIt is open, after the user enables
  Location Access and grants OS permission. It retains seven days in per-user browser storage.
  Revoking the toggle or OS permission stops collection.
- Timeline search sends only points from the previous 24 hours. The backend uses them
  transiently for PostGIS matching and never persists them; only the declaration metadata is
  stored. There is no timeline read endpoint or cross-user view.
- **Private Profile** (Settings) hides name and contact info from public listings; honor
  it in every DTO that serializes `posterUsername` or contact fields.
- Item photos may contain faces, plates, or documents. Never make the upload bucket
  publicly listable.
- **Account deletion (`DELETE /users/me`) anonymizes; it does not hard-delete.** `AccountService`
  clears the username (`deleted-<id>`), email (`deleted-<id>@deleted.invalid`), password hash,
  provider id and type, and picture, and sets `enabled = false`. It deletes the user's declared
  searches (`lost_searches`) and blacklists the current token. The row stays so posts, claims, and
  chat threads keep their history and foreign keys. Posts show as from a deleted user.
  - Sign in with Apple is revoked at Apple when the account is deleted. The server keeps the Apple
    refresh token from sign-in and revokes it with `APPLE_TEAM_ID`, `APPLE_KEY_ID`, and
    `APPLE_PRIVATE_KEY_PEM` set. Without those, revocation is skipped and logged. Google sign-in
    needs no revocation: no provider token is stored.

## Secrets & configuration

> **Gap — committed secrets must be rotated.** `backend/app/.env.template` previously
> contained real-looking values, and `frontend/.env` was tracked in git. The working tree is
> fixed: the template now holds placeholders only, `frontend/.env` is untracked and git-ignored,
> and `frontend/.env.example` documents the variables. Git history still contains the old values.
> Rotate the datasource password, JWT HMAC secret, Google OAuth client secret, Gmail app password,
> and Maps key. Then delete this note.

All secrets come from `backend/app/.env` and `frontend/.env`, which are never committed:
`SPRING_DATASOURCE_*`, `JWT_HMAC_SECRET_KEY`, `JWT_ES256_PRIVATE_KEY_PEM` /
`JWT_ES256_PUBLIC_KEY_PEM`, `GOOGLE_OAUTH2_CLIENT_ID` / `_SECRET`, `SUPPORT_EMAIL`,
`APP_EMAIL_PASSWORD`. Native-app settings (not secret, but required):
`NATIVE_ORIGINS` (default `capacitor://localhost`), `GOOGLE_CLIENT_IDS` (web and iOS client ids,
comma-separated), `APPLE_BUNDLE_IDS` (the iOS bundle id). Sign in with Apple revocation also needs `APPLE_TEAM_ID`,
`APPLE_KEY_ID`, and `APPLE_PRIVATE_KEY_PEM` (the `.p8` key contents; a secret). The frontend reads `VITE_API_URL`,
`VITE_GOOGLE_MAPS_API_KEY`, `VITE_GOOGLE_WEB_CLIENT_ID`, `VITE_GOOGLE_IOS_CLIENT_ID`, and
`VITE_APPLE_CLIENT_ID` from `frontend/.env`; see `frontend/.env.example`. Replace every
placeholder before any shared deployment.

The Google Maps key is a browser key, so restrict it by HTTP referrer. Add `capacitor://localhost`
for the iOS app, or the map will not load there.

## Known weaknesses to address before release

- CSRF protection is disabled while auth lives in a cookie. Acceptable only because
  `SameSite` and a CORS allowlist (`DEV_HOSTNAME`, `PROD_HOSTNAME`, `NATIVE_ORIGINS`) are in
  force; revisit before production. The iOS app sends a bearer token, which the browser never
  attaches automatically, so it is not exposed to CSRF.
- Apple revocation on deletion is best-effort. If Apple is unreachable, the failure is logged,
  the deletion still goes ahead, and the stored refresh token is discarded, so it cannot be retried.
  Only accounts that signed in with an authorization code (current app builds) have a token to revoke.
  Refresh tokens are stored as plain text in `users.apple_refresh_token`. Encrypting them at rest is a follow-up.
  App Store review expects this revocation, so confirm the configured keys before submission.
- Item photos are stored as base64 in text columns. Large photos inflate the database. The native
  camera resizes to 1600 px wide, and moving photos to object storage is still open.
- `logging.level.org.springframework.security: DEBUG` leaks auth detail into logs.
- `spring.jpa.hibernate.ddl-auto: update` lets the app mutate production schema. See
  [task-001](tasks/task-001-schema-migrations.md).
- No rate limiting on login, password reset, resend-verification, or the search endpoint.
  Verification and reset tokens make brute-forcing them impractical, but the endpoints can
  still be flooded with emails.
- Resolved: wrong-password logins used to return HTTP 500. `BadCredentialsException` is now
  mapped to `401` (`Incorrect email or password.`).
