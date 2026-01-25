# AGENTS.md

Instructions for AI coding agents working in this repository. This file describes **how
to work**; `docs/` describes **what the system is**. Do not duplicate specifications here.

## Where the specifications live

Read [`docs/README.md`](docs/README.md) first; it indexes everything. Load the specific
documents your task touches:

| Topic | File |
| --- | --- |
| Components, technologies, boundaries | `docs/architecture.md` |
| Tables, fields, constraints | `docs/data-model.md` |
| HTTP and WebSocket contracts | `docs/api.md` |
| Auth, authorization, privacy | `docs/security.md` |
| Screens, tokens, components | `docs/design/ui-spec.md` |
| Flow order and branching rules | `docs/design/user-flows.md` |
| Setup and verification commands | `docs/development.md` |
| Scoped tasks and approved plans | `docs/tasks/` |

**Precedence when sources conflict:** `docs/` → source code → the Figma prototype →
root `README.md` / `install-guide.md` → `docs/Project/*.docx`. The `.docx` files are
historical course deliverables; the two-sided bottom-tab navigation and the browse-first
loser flow they describe have been **replaced**. `frontend/README.md` is the stock Vite
template and is not authoritative about anything.

## Non-negotiable constraints

1. **Never let a client reach found-item results without a recorded declaration.** The
   loser's category/location/time search is written server-side before results are
   returned. Do not add an endpoint, query parameter, or client-side filter that lets a
   user enumerate found items. This is the reason the product was redesigned; see
   `docs/security.md`.
2. **Authorization lives in the service layer.** Compare the authenticated user id
   against the row and throw `UserMismatchException`. Controllers stay thin; a controller
   must not contain business logic and a service must not import `jakarta.servlet`.
3. **Respect the package shape** `domain` / `dto` / `service` / `infra`, per feature
   (`auth`, `items`, `claims`, `chat`, `shared`).
4. **Proximity queries use PostGIS** (`ST_DWithin` on `geography(Point,4326)`), not
   distance math in Java.
5. **One `item_posts` table** for both sides, discriminated by `post_type`. No parallel
   tables.
6. **Never commit secrets.** `backend/app/.env` and `frontend/.env` stay untracked. Add
   new variables to `.env.template` with placeholder values and document them in
   `docs/security.md`.
7. **No `window.alert` / `window.confirm` in new frontend code.** Errors and
   confirmations render inline, per `docs/design/ui-spec.md`.
8. **Mobile-first.** This ships to iOS via Capacitor. Design at ~390 px, keep touch
   targets ≥ 44 px, and avoid browser-only APIs without a Capacitor fallback.

## How to approach a nontrivial task

1. Read the task file in `docs/tasks/` and the spec sections it references.
2. Investigate the existing code before proposing anything — this repo already implements
   auth, item CRUD, PostGIS search, claims, and chat. Prefer extending over rewriting.
3. Produce a written plan: files to change, schema/contract changes, order of work, how
   each acceptance criterion will be verified, and risks.
4. **Stop and wait for review.** Do not start implementing a plan that has not been
   approved. Save the approved plan next to the task as `<task>-plan.md`.
5. Implement in small, reviewable commits on a feature branch. One concern per PR.

## How to test and verify

Run from the repository root unless noted:

```bash
cd backend/app && ./mvnw test     # must pass
cd frontend && npm run lint       # must pass
cd frontend && npm run build      # must pass
```

- Backend tests use the fake repositories in
  `src/test/java/.../test_implementations/`; extend those rather than adding a new
  mocking framework. Controller tests are `@WebMvcTest` slices.
- Any new endpoint needs a controller test and a service test, including the
  unauthorized (`401`) and wrong-owner (`403`) cases.
- Any schema change needs a migration plus a test that exercises the new column or table.
- If you touch a flow in `docs/design/user-flows.md`, say in your summary which manual
  steps a reviewer should walk through.
- Report honestly when something fails. Do not describe unverified work as verified.

## When requirements are unclear

Do not guess at product behavior or invent a design decision. In order:

1. Check `docs/`, then the Figma prototype and the exported screens in
   `docs/design/screens/`.
2. If still ambiguous, **stop and ask**. State the ambiguity, the options you see, and
   which you would pick and why.
3. If you must proceed, pick the option that preserves the anti-false-claim property and
   record the assumption in your summary and in a `TODO(spec):` comment at the site.

Contradictions between `docs/` and the code are bugs. Point them out in your summary
rather than silently coding to one side.

## Keeping docs current

When a change alters a documented contract, update the matching file in `docs/` in the
same PR. New entity or column → `docs/data-model.md`. New or changed endpoint →
`docs/api.md`. New screen or flow step → `docs/design/`. Resolving a **Gap** note →
delete the note.

## Pending README updates

Add these to the project README the next time it is updated (client decisions, not yet written there):

- Lost search window is ±12 hours around the time the user says they lost the item, and the
  radius is 1 mile. Both are current defaults, kept for now.
- API host: the frontend still calls `http://localhost:8080` (the backend's Docker port). The
  client asked to keep "localhost 8000"; confirm which port is intended before writing this down.
