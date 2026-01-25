# Development Environment

## Prerequisites

Docker + Docker Compose. For running pieces outside containers: Node 22+, JDK 21
(Maven comes from the wrapper at `backend/app/mvnw`).

## First-time setup

```bash
cp backend/app/.env.template backend/app/.env   # then fill in real values
cp frontend/.env.example frontend/.env          # then fill in the VITE_ values
docker compose up --build
```

Required backend variables are listed in [security.md](security.md#secrets--configuration).
`SPRING_DATASOURCE_URL` must use `postgres` as the host under Compose. Without a Maps key
the app still runs but map picking and map views are degraded. Without
`HUGGINGFACE_API_TOKEN` set `VISION_ENABLED=false`; posting must still work.

| Service | URL |
| --- | --- |
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 |
| PostgreSQL / PostGIS | localhost:5432 (`user` / `pass` / `lost_and_found`) |
| pgAdmin | http://localhost:5050 (`admin@admin.com` / `admin`) |

`postgres/01-init.sql` runs **only on an empty volume**. To pick up schema changes today
you must `docker compose down -v` and rebuild, which destroys local data — the reason
[task-001](tasks/task-001-schema-migrations.md) exists.

Timeline search also adds `postgres/02-timeline-search.sql`. Existing local volumes must
be reset with the command above so `lost_searches.location` becomes nullable. Location
history itself is never stored in PostgreSQL.

## iOS build (Capacitor)

The iOS app is the same React build inside a native shell. It needs a public HTTPS API, because
the phone cannot reach your laptop's `localhost`. Simulator and device both need this.

1. **Backend.** `NATIVE_ORIGINS` defaults to `capacitor://localhost`, which is correct for the app.
   Also set `GOOGLE_CLIENT_IDS` (the web and iOS OAuth client ids, comma-separated), and `APPLE_BUNDLE_IDS`
   (the bundle id). Deploy it on HTTPS.
   For Sign in with Apple account revocation, also set `APPLE_TEAM_ID`, `APPLE_KEY_ID`, and
   `APPLE_PRIVATE_KEY_PEM`. Create the key in the Apple Developer portal (Keys, with Sign in with Apple
   enabled). Apple lets you download the `.p8` only once. Put it in `backend/app/.env` and never commit it.
2. **Frontend env.** In `frontend/.env` set `VITE_API_URL=https://<your-api-host>`, plus the
   `VITE_GOOGLE_*` and `VITE_APPLE_CLIENT_ID` values from `.env.example`.
3. **Build and sync.**
   ```bash
   cd frontend && npm run build && npx cap sync ios
   ```
   `cap sync` copies `dist/` into `ios/App/App/public` and updates the Swift Package list.
4. **Xcode.** `npx cap open ios`, then:
   - Set your Team under Signing & Capabilities, and change the bundle id to a real one in
     both Xcode and `capacitor.config.json` (`appId` is a placeholder, `com.example.foundit`).
     The same id goes in `APPLE_BUNDLE_IDS`.
   - Add the **Sign in with Apple** capability. This cannot be done in code; Xcode writes the
     entitlement.
   - In `ios/App/App/Info.plist`, replace `REVERSED_CLIENT_ID` in the
     `com.googleusercontent.apps.REPLACE-WITH-IOS-CLIENT-ID` URL scheme with the reversed iOS
     client id. Google sign-in fails until this is real.
5. **Manual check** on a simulator or device: sign in with Google, sign in with Apple, open the
   Map (location prompt), create a report with the camera, open chat (live messages), and delete
   the account from Settings. Then log out and back in.

Not verifiable on a machine without Xcode. The steps above have not been run end to end.

## Running pieces individually

```bash
cd frontend && npm install && npm run dev     # expects backend on :8080
cd backend/app && ./mvnw spring-boot:run      # expects PostGIS reachable
```

## Verifying a change

```bash
cd backend/app && ./mvnw test        # JUnit: services, controllers (@WebMvcTest), JWT, repos
cd backend/app && ./mvnw verify      # compile + test
cd frontend && npm run lint          # ESLint 9 flat config
cd frontend && npm run build         # Vite production build must succeed
```

Backend tests use fake in-memory repositories under
`src/test/java/.../test_implementations/`; prefer extending those over introducing a new
mocking approach. Controller tests are `@WebMvcTest` slices with mocked services.

Every change must leave `./mvnw test`, `npm run lint`, and `npm run build` green. A
change to a flow in [design/user-flows.md](design/user-flows.md) also needs a manual pass
through that flow in the browser at 390 px width.

## Environment gaps to close

- **No component tests.** `npm test` runs Node's built-in test runner against `src/core/`.
  Screens have no automated tests; Vitest + React Testing Library is the intended choice.
- **No CI.** Add `.github/workflows/ci.yml` running the four commands above on pull
  requests.
- **No formatter or type checking.** ESLint only; no Prettier, no TypeScript. New
  frontend code stays `.jsx` until the team decides otherwise.
- **No migration tooling.** See [task-001](tasks/task-001-schema-migrations.md).
