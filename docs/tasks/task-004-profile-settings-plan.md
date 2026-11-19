# Task 004 — Profile section & Settings (approved plan)

**Status:** Approved by the client on 2026-11-18 (decisions below). Implemented in this change.

Brings the profile section ([frontend/src/components/Profile.jsx](../../frontend/src/components/Profile.jsx))
in line with `docs/design/ui-spec.md` §Settings (screen `09-settings.png`) and the
"My Profile" screen.

## Client decisions

1. **Layout:** keep the current sidebar dashboard; add the missing content (no mobile-first
   refactor this task). _Known deviation from AGENTS.md #8 — recorded as a `TODO(spec)`._
2. **Private Profile:** store **and enforce**. When a poster's `private_profile` is on, their
   username is hidden from non-owners in public item responses and in `GET /users/{id}`.
3. **My Profile edits:** username and photo are editable; email is read-only (changing it
   would need a re-verification flow — out of scope).
4. **Notifications:** store preferences only. The five notification toggles are persisted but
   no push/email is actually sent (there is no notification system yet).

## Schema

New table `user_settings` (one row per user, created lazily on first read). Seven booleans with
the defaults from `ui-spec.md`:

| column | default |
| --- | --- |
| `location_access` | true |
| `private_profile` | false |
| `push_notifications` | true |
| `lost_item_alerts` | true |
| `found_item_matches` | true |
| `notification_sound` | true |
| `email_updates` | false |

`user_id` is PK + FK to `users(id)` `ON DELETE CASCADE`. Added to `postgres/03-user-settings.sql`
for fresh volumes; on existing databases `ddl-auto: update` creates it from the entity (the repo
still has no migration tool — see task-001).

## Backend

- `shared/domain/UserSettings.java` — entity, `@OneToOne @MapsId` to `User`.
- `User` gains a lazy `settings` relation and a null-safe `isPrivateProfile()` helper.
- `shared/infra/UserSettingsRepository.java`.
- `shared/service/SettingsService.java` — `getOrCreateForUser`, `update`.
- `shared/service/ProfileService.java` — `updateProfile(userId, username, picture)`; rejects a
  blank or already-taken username (`UsernameTakenException` → 409).
- `shared/dto/UserSettingsDTO.java`, `shared/dto/UpdateProfileDTO.java`.
- `UserController` gains `PUT /users/me`, `GET /users/me/settings`, `PUT /users/me/settings`, and
  passes the viewer into `GET /users/{id}` so a private profile hides the username.
- `items/service/ItemMapper.java` — null the `posterUsername` for non-owner viewers when the
  poster is private. Mapping already runs inside `@Transactional`, so the lazy read is safe.

## Frontend

- `core/services.js` — `fetchMySettings`, `updateMySettings`, `updateMyProfile`.
- `Profile.jsx` — "My Profile" tab shows account details and an inline edit form (username +
  photo URL); "Settings" tab renders the two grouped toggle sections, loads them on open, and
  PUTs each change. Timeline toggle and Delete Account stay.
- `Profile.css` — toggle switch and profile-edit styles. No `window.alert/confirm`.

## Verification

- `cd backend/app && ./mvnw test` — adds `SettingsServiceTest`, `ProfileServiceTest`,
  `ProfileSettingsControllerWebTest` (incl. 401), and `ItemMapperPrivacyTest`.
- `cd frontend && npm run lint && npm run build`.
- Manual: toggle each setting (persists across reload); set Private Profile on and confirm a
  second user no longer sees the poster's name on that item; edit username/photo.

## Docs updated in this change

`data-model.md` (new table), `api.md` (new endpoints), `security.md` (Private Profile
enforcement), and this plan file.
