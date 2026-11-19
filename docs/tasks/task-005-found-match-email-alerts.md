# Task 005 — Email alerts for found-item matches

**Status:** Scoped, awaiting review. Client decisions recorded 2026-11-18.

Deliver the "Found Item Matches" notification (docs/design/ui-spec.md §Settings) over email:
when someone posts a FOUND item, email the owners of open LOST reports it plausibly matches,
so they come back and run the declared search.

## Client decisions

1. **Trigger source:** the user's **open LOST reports** (`item_posts`, `post_type = LOST`,
   `status = OPEN`), which already carry category + location. Not the one-shot `lost_searches`
   rows. (Matches the spec's "your lost report".)
2. **Email content:** **minimal nudge only.** The email reveals nothing about the found item —
   no category, photo, location, or poster. It may reference the recipient's *own* lost report
   title (their data) and links them into the app to run their search. This preserves the
   anti-false-claim gate (docs/security.md): found-item detail stays behind a declared search.
3. **Gating:** send only when the recipient's `user_settings.found_item_matches` **and**
   `email_updates` are both true. (Default `email_updates = false`, so it is opt-in.)

## Decided by the agent (open to change in review)

- **Matching:** same category (case-insensitive) within **1 mile** (1609.34 m) via PostGIS
  `ST_DWithin`, excluding the finder's own posts. 1 mile = the existing lost-search default.
- **Delivery:** asynchronous (`@Async`; `@EnableAsync` is already on `AppApplication`). Item
  creation never blocks on or fails because of email — failures are caught and logged, mirroring
  `AuthenticationService.sendVerificationEmailSafely`.
- **When:** on FOUND-item **creation** only (not edits), MVP. One email per matching owner per
  found item; de-duplicated by owner within that trigger.
- **No schema change and no new endpoint** — reuses `item_posts` and `user_settings`, so this
  does not depend on migration tooling (task-001).

## Plan

**Backend**

1. `items/infra/ItemPostRepository` — add a native query:
   ```sql
   SELECT * FROM item_posts ip
   WHERE ip.post_type = 'LOST' AND ip.status = 'OPEN'
     AND lower(ip.category) = lower(:category)
     AND ip.user_id <> :finderUserId
     AND ST_DWithin(ip.location, :center, :radiusMeters)
   ```
   returning candidate LOST posts (each carries its owner `user`).
2. `shared/service/EmailService` — add `sendFoundMatchEmail(String to, String lostReportTitle)`
   that sends the minimal-nudge HTML (links to `app.frontend-url`), no found-item data.
3. New `items/service/FoundMatchNotifier` (feature package `items`):
   - `@Async void notifyForFoundItem(Long foundItemId)`.
   - Loads the found item, runs the match query, and for each candidate owner whose
     `found_item_matches && email_updates` are both on, de-duped by owner id, calls
     `EmailService.sendFoundMatchEmail`. Each send is wrapped so one failure does not stop the rest.
   - Reads settings via `UserSettingsRepository` (null row ⇒ treat as defaults ⇒ `email_updates`
     false ⇒ not sent).
4. `items/service/ItemServiceJPAImpl.createItem` — after `save`, if `postType == FOUND`, call
   `foundMatchNotifier.notifyForFoundItem(saved.getId())`. The async boundary means the mapped
   response returns immediately; the notifier re-loads the item in its own transaction.

**Tests** (fakes, per AGENTS.md — no real DB)

- `FoundMatchNotifierTest`: emails an eligible owner; skips when `found_item_matches` off; skips
  when `email_updates` off; skips the finder's own matching LOST post; de-dupes a single owner with
  two matching reports; one email failure does not suppress the others. Uses a fake
  `ItemPostRepository` match result, the in-memory `UserSettingsRepository`, and a recording
  `EmailService` double.
- `EmailServiceTest`: `sendFoundMatchEmail` body contains **no** found-item fields (guards the
  anti-leak rule) and does contain the app link.
- Keep `createItem` tests green; add one asserting the notifier is invoked for FOUND and not for
  LOST (inject a recording notifier).

**Docs**

- `docs/security.md` — document the alert and its minimal-content rule under the anti-false-claim
  model (an alert must never carry found-item detail).
- `docs/api.md` / `docs/design/ui-spec.md` — note that "Found Item Matches" is now delivered by
  email, gated on `email_updates`, and that "Email Updates"/"Lost Item Alerts"/push remain
  unbuilt. Update the Profile Settings note that said notifications are not wired up.
- This task file; save the reviewed version as `task-005-...-plan.md` if the plan changes.

## Verification

- `cd backend/app && ./mvnw test`; `cd frontend && npm run lint && npm run build`.
- Manual (real DB, placeholder Brevo logs the attempt): user A posts a LOST "keys" near a point and
  turns on Found Item Matches + Email Updates; user B posts a FOUND "keys" within a mile; confirm an
  email attempt to A is logged and its body carries no found-item detail. Confirm no attempt when A
  has Email Updates off, and none to B about their own post.

## Risks / notes

- **Anti-leak is the whole point.** The one rule that must hold: the email carries zero found-item
  data. The `EmailServiceTest` content assertion is the guard; review it closely.
- **Email spam / repeat matches:** a loser matching many found items gets many emails. MVP accepts
  this; a future "already notified (owner, found_item)" ledger or digest would bound it. Noted, not
  built.
- **Placeholder Brevo:** with the dev key, sends fail and are logged; the feature is correct but not
  observable as a delivered email until a real `BREVO_API_KEY` is set.
- **No background capture of intent:** this keys off standing LOST reports, so a user who only ran a
  search (no LOST report) is not alerted. That follows decision 1.
