# User Flows

Screen appearance is specified in [ui-spec.md](ui-spec.md); this file specifies order,
branching, and the rules that must hold at each step.

## Why the loser flow is ordered this way

Declare first, browse second. The loser states category, then place and time, and only
then sees found items. Because the declaration is persisted before any listing is
returned, a later claim can be checked against what the claimant knew beforehand — they
cannot read "black leather wallet, Central Park South, 2:30pm" off a listing and then
file a matching lost report. Any shortcut that reaches results without a recorded
declaration defeats the feature.

## 0. Entry

Unauthenticated users are redirected to `/login`.

- **Register:** submit the form → "Check your email" screen (resend available) → open the
  link (`/verify-email`) → "Your email is verified" → log in.
- **Login before verifying:** the wrong password shows "Incorrect email or password." A
  correct password shows "Account not verified" with a resend action.
- **Forgot password:** enter email → generic "if an account exists, a link is on its way"
  → open the link (`/reset-password/:token`) → set a new password (rules checklist, confirm)
  → "Password updated" → log in.

Google OAuth is an alternative entry and does not need verification. On success the user
lands on **Home** (Items tab).

## 1. I Lost an Item

```
Home ──tap "I Lost an Item"──► Step 1: category
                                  │ select one of the 9 categories
                                  ▼
                               Step 2: where and when
                                  ├─ manual: Location* + Date* + Time*
                                  └─ or: "Search along my Timeline"
                                  │ tap "Search Found Items"
                                  ▼        (declaration persisted here)
                               Results: FOUND posts matching category + radius + window
                                  ├─ tap a card ──► found-item detail ──► Contact ──► Messages
                                  └─ tap "+ Report Lost" ──► Report Lost form ──► my LOST post
```

Rules:
- `Search Found Items` is disabled until the manual group is complete **or** Timeline is
  selected. The two inputs are mutually exclusive.
- The search is one server call that writes the declaration and returns results together,
  so results can never be obtained without the record.
- Timeline requires Location Access to be enabled in Settings and OS permission granted.
  If either is missing, prompt once; if declined, the option is disabled with an
  explanation and the manual path remains available. Never silently fall back.
- Matching is `post_type = FOUND`, `status = OPEN`, same category, within the search
  radius (PostGIS `ST_DWithin`), and `event_time` inside the declared window. Timeline
  searches match against the user's pings for that window.
- In the browser test implementation, Timeline points are recorded locally every five
  minutes while FindIt is open and retained for seven days. A Timeline search submits points
  from the previous 24 hours and matches within 0.5 miles; the server does not retain them.
- Results show non-identifying fields only. Zero results shows the empty state with
  `+ Report Lost` still available.
- `Report Lost` prefills category, location, date, and time from the declaration. Photo
  is optional.

## 2. I Found an Item

```
Home ──tap "I Found an Item"──► Missing-item reports (all OPEN LOST posts)
                                  ├─ tap "+ Report Found" ──► Report Found form ──► my FOUND post
                                  ├─ tap a card ──► lost-item detail
                                  └─ tap "Contact" ──► chat thread for that item ──► Messages
```

Rules:
- The finder browses immediately with no declaration gate. A finder gains nothing from
  lying, and friction here costs real returns.
- Only `post_type = LOST`, `status = OPEN` posts appear, ordered most recent first, each
  with its reward chip when a reward is set.
- A photo is **required** on the Report Found form — it is the evidence an owner is
  verified against.
- Submitting a found report notifies users whose recorded lost searches match it by
  category, proximity, and time window. This is the promise the green banner makes.
- `Contact` opens or reuses the single thread for (item, this pair of users).

## 3. Messages

Messages tab → conversation list → thread. A thread shows the related listing above the
messages with a link to the full listing. Participants arrange the handoff, then the post
owner marks the item resolved. Only participants may read a thread.

## 4. Claims and resolution

A claim moves `PENDING → ACCEPTED | REJECTED`, decided by the post owner. One claim per
user per item. When a claim is accepted the post becomes `RESOLVED`, and a `RESOLVED`
post can no longer be edited, deleted, or claimed. Owners review their own posts and
claims from their profile.

## 5. Settings

Toggles take effect immediately and persist per user. Turning Location Access off stops
ping collection and disables Timeline search. Private Profile removes the user's name and
contact details from public listings.

## Flows explicitly removed from earlier documents

- The bottom-bar "loser side / founder side" switcher. Replaced by the two home cards.
- Loser and finder sides opening directly onto a browsable list with filter and search
  controls at the top. The loser now reaches results only through the declared search;
  the "filter/search the found item list" user stories in the Iteration Plan are
  superseded by it.
- Messages reached only from within a side. Messages is now a top-level tab.
