# Data Model

Source of truth for the live schema is `postgres/01-init.sql` plus the JPA entities under
`backend/app/src/main/java/lost_and_found/app/*/domain/`. Entities and SQL must agree;
when they drift, the SQL wins and the entity is a bug.

## Entities

### `users`
`id` PK · `username` unique · `password` (null for OAuth accounts) · `email` unique ·
`enabled` · `provider_id` unique / `provider_type` (`GOOGLE` | `APPLE`, null for local accounts) · `picture` ·
`verification_code` (SHA-256 hash) + `verification_code_expiry` (24h) ·
`reset_token` (SHA-256 hash) + `reset_token_expiry` (1h) ·
`created_at` / `updated_at`.

Local registration sets `enabled = false` until the verification link is used. Google and
Apple accounts are created with `enabled = true`. For Apple accounts, `apple_refresh_token` holds the
refresh token from sign-in, used to revoke the Sign in with Apple link on deletion (null when Apple did not issue one). Raw tokens are only sent by email and never
stored. Either `password` or `provider_id` must be present.

**Anonymized accounts** (after `DELETE /users/me`) keep the row with `enabled = false`,
`username = deleted-<id>`, `email = deleted-<id>@deleted.invalid`, and `password`, `provider_id`,
`provider_type`, `picture`, and `apple_refresh_token` set to null. Their `item_posts`, claims, and chat threads remain.

### `item_posts`
The central entity. One row per report, both sides.

`id` PK · `user_id` → `users` (cascade delete) · `title` (≤150, required) ·
`description` · `category` (≤50) · `post_type` **`LOST` | `FOUND`** ·
`status` **`OPEN` | `RESOLVED`** · `image_url` (NOT NULL) · `image_url2` ·
`location` `GEOGRAPHY(POINT,4326)` NOT NULL, GiST-indexed · `location_text` (display
string) · `detail_location` (free text, e.g. "under the bench") · `event_time` (when the
item was lost or found) · `created_at` / `updated_at`.

Constraints that matter:
- `event_time` must not be in the future (enforced in `CreateItemDTO`, 5s clock skew).
- `image_url` is nullable: a photo is optional for both lost and found reports (built).
- `reward_cents` is nullable, `>= 0`, and set only on `LOST` posts (built). Stored in cents so money is never a float.
- `detail_location` is private to the poster. Never return it to other viewers (see api.md).
- `post_type` defaults to `LOST` in `@PrePersist` — a silent default that has caused bugs.
- A `RESOLVED` post must not be editable or deletable.

### `lost_searches` (built)
One row per loser search, written before results are returned: `id` PK · `user_id` →
`users` · `category` (≤50) · `location` `GEOGRAPHY(POINT,4326)` · `search_radius_meters` ·
`window_start` · `window_end` · `used_timeline BOOLEAN` · `created_at`. This is the audit
trail that makes false claims detectable. `location` is required for manual searches and
null for Timeline searches; submitted Timeline points are used transiently and are not stored.

### `chat_threads`, `chat_messages`
`chat_threads`: `id` · `item_post_id` · `user1_id` · `user2_id` · timestamps.
Invariants enforced in SQL: `user1_id <> user2_id`, `user1_id < user2_id` (canonical
ordering), and `UNIQUE (item_post_id, user1_id, user2_id)` — one thread per item per pair.
Always normalize the pair before inserting.

`chat_messages`: `id` · `thread_id` · `sender_user_id` · `message_text` · `sent_at`.

### `item_claims`
`id` · `item_post_id` · `claimant_user_id` · `chat_thread_id` (nullable, `SET NULL`) ·
`status` **`PENDING` | `ACCEPTED` | `REJECTED`** · timestamps ·
`UNIQUE (item_post_id, claimant_user_id)` — a user gets one claim per item, ever.

### `jwt_token_blacklist`
`id` · `jti` unique · `username` → `users.username` · `blacklisted_at`. Written on logout.

## Relationships

```
users 1──* item_posts
      │                 1──* item_claims
      │                 1──* chat_threads 1──* chat_messages
      └──* jwt_token_blacklist
```

## Additions required by the redesign — not yet built

| Addition | Why |
| --- | --- |
| `user_settings` | Settings screen toggles: location access, private profile, push/lost-alert/found-match/sound/email notification preferences. |

Each of these is a schema change, so none can land before migration tooling exists
([task-001](tasks/task-001-schema-migrations.md)).
