# Task 001 — Proposed implementation plan

Proposed by the coding agent after reading
[task-001-schema-migrations.md](task-001-schema-migrations.md), `docs/architecture.md`,
`docs/data-model.md`, `docs/security.md`, and investigating the repository.
**Not implemented. Awaiting team review.**

## What the investigation found

- Schema is defined twice and owned by neither: `postgres/01-init.sql` is mounted at
  `/docker-entrypoint-initdb.d` and runs only on an empty volume, while
  `application.yaml` sets `spring.jpa.hibernate.ddl-auto: update`, letting Hibernate
  alter the schema at boot.
- There is real drift already: `ItemPost` declares `image_url2`, and `01-init.sql` has no
  such column. It exists in deployed databases only because Hibernate added it. Switching
  to `validate` will surface this and possibly more.
- `pom.xml` has no Flyway, no Liquibase, and no Testcontainers. Tests run entirely on
  hand-written fakes in `src/test/java/.../test_implementations/`, so **no test currently
  touches a real database** — a broken migration would pass CI today.
- Spatial support comes from `hibernate-spatial` 7.2.6 and `jts-core` 1.20 against the
  `postgis/postgis:18-3.6` image. `ST_DWithin` usage lives in `ItemSpecifications`, and
  `PointUtil` builds `Point` values with SRID 4326.
- Spring Boot 4.0.2 / Java 21. Flyway's version is managed by the Boot parent, so no
  explicit version pin is needed.

## Plan

**Step 1 — Add Flyway.** Add `flyway-core` and `flyway-database-postgresql` to
`backend/app/pom.xml`. Configure in `application.yaml`:
`spring.flyway.enabled: true`, `locations: classpath:db/migration`,
`baseline-on-migrate: false`. Leave `ddl-auto: update` for now so the app still starts.

**Step 2 — Baseline.** Create `src/main/resources/db/migration/V1__baseline.sql` from
`postgres/01-init.sql` verbatim, plus the two corrections the current code requires:
`CREATE EXTENSION IF NOT EXISTS postgis` stays first, and `item_posts.image_url2 TEXT` is
added so V1 reflects what the entities actually expect. Verify by
`docker compose down -v && docker compose up --build` and diffing `\d+` output for every
table against a pre-change database.

**Step 3 — Redesign additions.** `V2__redesign_schema.sql`:

- `ALTER TABLE item_posts ADD COLUMN reward_cents INTEGER CHECK (reward_cents >= 0)`
- `ALTER TABLE item_posts ALTER COLUMN image_url DROP NOT NULL`, with a check constraint
  keeping the photo mandatory for found reports:
  `CHECK (post_type <> 'FOUND' OR image_url IS NOT NULL)`
- `lost_searches` — `id`, `user_id` FK cascade, `category`, `search_point
  GEOGRAPHY(POINT,4326)` nullable, `search_radius_meters INTEGER`, `window_start`,
  `window_end`, `used_timeline BOOLEAN NOT NULL DEFAULT FALSE`, `created_at`; GiST index
  on `search_point`, btree on `(user_id, created_at)`; check that either `search_point`
  is present or `used_timeline` is true.
- `location_pings` — `id`, `user_id` FK cascade, `point GEOGRAPHY(POINT,4326) NOT NULL`,
  `recorded_at TIMESTAMPTZ NOT NULL`, GiST index on `point`, btree on
  `(user_id, recorded_at)`.
- `user_settings` — `user_id` PK/FK cascade plus the seven boolean toggles from
  `docs/design/ui-spec.md`, defaulting to the values shown in the prototype.

**Step 4 — Hand schema ownership to Flyway.** Set `ddl-auto: validate`, remove the
`./postgres:/docker-entrypoint-initdb.d` volume mount from `docker-compose.yml`, and
delete `postgres/01-init.sql`. Fix every mismatch `validate` reports by adding a
migration — never by editing V1 or loosening an entity.

**Step 5 — Test the migrations.** Add Testcontainers (`postgresql` + `junit-jupiter`) and
one `@SpringBootTest` that boots against a real `postgis/postgis:18-3.6` container,
asserting migrations apply cleanly, `flyway_schema_history` contains V1 and V2, the new
tables and columns exist with the right types, and an `ST_DWithin` query over
`item_posts.location` returns a seeded row. This is the test that makes criterion 6
meaningful; existing fake-repository tests stay untouched.

**Step 6 — Document.** Add a "Database migrations" section to `docs/development.md`
covering naming (`V{n}__snake_case.sql`), the rule that applied migrations are immutable,
the local reset command, and the migration-required checklist. Remove the four "no
migration tooling" Gap notes.

Order matters: steps 1–3 are additive and safe to land together; step 4 is the breaking
change and should be its own commit so it can be reverted independently.

## Verification

`docker compose down -v && docker compose up --build` from clean; then restart against
the existing volume to prove migrations are incremental and non-destructive. Then
`./mvnw test`, `npm run lint`, `npm run build`. Manual check: create an item through the
UI and confirm `GET /items/nearby` still returns it.

## Open questions for the team

1. **Developers with existing volumes.** Simplest is "everyone runs `down -v` once."
   Alternative is `baseline-on-migrate: true` with `baseline-version: 1`. Recommending
   the clean reset — local data is disposable and baselining hides drift.
2. **Testcontainers in scope?** It adds a Docker requirement to the test run but is the
   only way to actually test migrations. Recommending yes; dropping it makes criterion 6
   unachievable.
3. **Reward units.** `reward_cents INTEGER` avoids floating-point money. Confirm the
   prototype's `Reward: $50` is always whole dollars and whether a currency is needed.
4. **Ping retention.** `docs/security.md` requires a retention window for
   `location_pings` but does not set a number. A concrete value (e.g. 30 days) is needed
   before the timeline feature ships; it does not block this task.
5. **Scope check.** This task deliberately creates tables without entities or endpoints.
   The alternative is folding the search feature in, which makes the migration change
   much harder to review. Recommending we keep them separate.
