# Task 001 — Migration tooling and the redesign schema baseline

**Source:** Iteration Plan → Global Setup & Infrastructure, "Database: configure
migration tooling" and "approved database can be created using the project's
migration/setup process."
**Status:** scoped, awaiting approval. Proposed plan: [task-001-plan.md](task-001-plan.md).

## Why this is first

Every feature in the redesign needs a schema change: `post_type` exposed through the API,
a `lost_searches` audit table, `location_pings`, `reward_cents`, `user_settings`. Today
the schema is a single `postgres/01-init.sql` that runs only against an empty volume,
plus `spring.jpa.hibernate.ddl-auto: update`, which lets the running application mutate
the schema on its own. That combination means a developer cannot apply a schema change
without destroying local data, teammates silently drift apart, and production schema is
whatever Hibernate decided. Nothing else should be built on it.

## Scope

**In scope**

1. Add Flyway to `backend/app/pom.xml` and wire it into Spring Boot.
2. Convert the current schema into `V1__baseline.sql` under
   `src/main/resources/db/migration`, preserving the PostGIS extension, every table,
   check constraint, unique constraint, and the GiST index on `item_posts.location`.
3. Add `V2__redesign_schema.sql` creating the additions in
   [data-model.md](../data-model.md#additions-required-by-the-redesign--not-yet-built):
   `item_posts.reward_cents`, `lost_searches`, `location_pings`, `user_settings`, and
   relaxing `item_posts.image_url` to nullable for `LOST` posts.
4. Set `spring.jpa.hibernate.ddl-auto: validate` so Hibernate can no longer alter schema,
   and reconcile any entity/SQL mismatch the switch exposes (`image_url2` is known to
   exist in the entity but not in `01-init.sql`).
5. Remove or neutralize `postgres/01-init.sql` so the schema has exactly one owner.
6. Document the migration workflow in [development.md](../development.md).

**Out of scope** — JPA entities, DTOs, controllers, and services for the new tables; any
frontend work; the search endpoint itself. Those are task-002 onward.

## Acceptance criteria

1. `docker compose down -v && docker compose up --build` produces a schema identical to
   today's, created by Flyway, with `flyway_schema_history` showing V1 and V2 applied.
2. Starting the backend against an **existing** database applies only pending migrations
   and does not drop or recreate data.
3. `ddl-auto: validate` is active and the application starts cleanly, proving entities
   and schema agree.
4. `location`-based queries still work: `GET /items/nearby?lon=..&lat=..&radiusMeters=..`
   returns correct results against a seeded row.
5. `lost_searches`, `location_pings`, `user_settings`, and `item_posts.reward_cents`
   exist with the columns, types, foreign keys, and indexes in `data-model.md`, including
   GiST indexes on both geography columns.
6. `cd backend/app && ./mvnw test` passes, with at least one test that fails if a
   migration is missing or malformed.
7. `docs/development.md` explains how to add a migration and how to reset local state;
   the "no migration tooling" Gap notes in `architecture.md`, `data-model.md`,
   `security.md`, and `development.md` are removed.
8. A teammate can pull the branch and reach a correct schema with documented commands
   only — no manual SQL.

## Risks

- Flyway baselining against a developer's existing volume can fail; the plan must state
  whether `baseline-on-migrate` is used and what a developer with old data should do.
- `ddl-auto: validate` will surface pre-existing entity/SQL drift and may block startup
  until each mismatch is fixed.
- Tests currently run on fake repositories, so a migration can be broken without any test
  failing unless real-database coverage is added.
