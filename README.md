# cs3803-lost-and-found

Lost and Found is a full-stack Georgia Tech lost-and-found application. It combines a React frontend, a Spring Boot backend, and a PostGIS-backed PostgreSQL database for item posts, claims, chat, and location-based search.

## Stack

- Frontend: React 19 + Vite + React Router
- Backend: Java 21 + Spring Boot 4 + Spring Security + JPA
- Database: PostgreSQL + PostGIS
- Infrastructure: Docker Compose

## Repository Layout

```text
.
├── backend/           Spring Boot application container build
│   └── app/           backend source, Maven wrapper, and env template
├── frontend/          React + Vite frontend
├── postgres/          database initialization and pgAdmin server config
├── docker-compose.yml
└── README.md
```

## Features

- User registration, login, logout, and forgot-password flow
- Lost/found item creation, editing, deletion, and detail pages
- Nearby item search using latitude/longitude with PostGIS
- Claims workflow for item owners and claimants
- Per-item chat threads and message history
- User profile and current-session endpoints
- Google Maps integration in the frontend when an API key is configured

## Prerequisites

- Docker and Docker Compose
- Node.js 22+ if you want to run the frontend locally
- Java 21 and Maven-compatible tooling if you want to run the backend locally

## Environment Setup

See [install-guide.md](install-guide.md) for extended installation and setup documentation.

### Backend

Create `backend/app/.env` from `backend/app/.env.template`.

```bash
cp backend/app/.env.template backend/app/.env
```

Minimum variables to review before running:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_HMAC_SECRET_KEY`
- `JWT_ES256_PRIVATE_KEY_PEM`
- `JWT_ES256_PUBLIC_KEY_PEM`
- `JWT_EXPIRATION_TIME`
- `DEV_HOSTNAME`
- `PROD_HOSTNAME`
- `GOOGLE_OAUTH2_CLIENT_ID`
- `GOOGLE_OAUTH2_CLIENT_SECRET`
- `GOOGLE_CLIENT_IDS`
- `NATIVE_ORIGINS`
- `BREVO_API_KEY`
- `EMAIL_FROM`

Notes:

- The template uses `postgres` as the database hostname, which is correct for Docker Compose.
- Replace any checked-in placeholder or real credentials with your own local values.
- HMAC is used to simplify local development; ES256 keys are intended for production-style setups.

### Frontend

Create `frontend/.env` if you want Google Maps enabled.

```bash
echo "VITE_GOOGLE_MAPS_API_KEY=your_key_here" > frontend/.env
```

Without that key, the app still runs, but map-based picking/search UI will be limited.

## Running With Docker

From the repository root:

```bash
docker compose up --build
```

Services:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- PostgreSQL/PostGIS: `localhost:5432`
- pgAdmin: `http://localhost:5050`

Docker service names:

- Database: `postgres`
- Backend container: `laf-backend`
- Frontend container: `frontend`

## Running Locally

### Frontend only

Use this mode if the backend is already running in Docker or on your host at `http://localhost:8080`.

```bash
cd frontend
npm install
npm run dev
```

The frontend code currently calls the backend at `http://localhost:8080` for session and auth requests.

### Backend only

If you run the backend outside Docker, make sure PostgreSQL/PostGIS is available and update `backend/app/.env` if `postgres` is not the correct hostname.

```bash
cd backend/app
./mvnw spring-boot:run
```

## Deployment

Production uses three hosted services. The backend runs on Render, the database runs on Neon, and account emails go through Brevo. The web frontend is not hosted yet. The iOS app is built with Capacitor and calls the Render backend.

| Part | Service | Notes |
| --- | --- | --- |
| Backend | Render (Docker web service) | Builds from this GitHub repo. Free tier sleeps when idle, so the first request can be slow. |
| Database | Neon (PostgreSQL) | PostGIS is enabled by the init script. |
| Email | Brevo (HTTPS API) | Render blocks SMTP ports, so email is sent over HTTPS. |

### Backend on Render

1. Create a new Web Service from the GitHub repo.
2. Set the runtime to Docker, the root directory to `backend`, and the Dockerfile path to `./Dockerfile`.
3. Add the environment variables from `backend/app/.env.template`. On Render, set `SPRING_PROFILES_ACTIVE=prod`, which uses the ES256 keys. Set `SERVER_PORT=10000`, which the service listens on.
4. Deploy. Changing an environment variable triggers a new deploy. Check that `GET /items` returns `200`.

Do not copy `.env` files to Render. Enter each value in the Render dashboard.

### Database on Neon

1. Create a Neon project and a database named `lost_and_found`.
2. In the Neon SQL editor, run `postgres/01-init.sql`, then `postgres/02-timeline-search.sql`. Run them in this order on an empty database.
3. Set `SPRING_DATASOURCE_URL` to `jdbc:postgresql://<host>/lost_and_found?sslmode=require`. Use the host from Neon. Set `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` to the Neon role's values.

If you reset the Neon password, update `SPRING_DATASOURCE_PASSWORD` on Render and redeploy. Otherwise the backend fails with `password authentication failed`.

### Email on Brevo

1. In Brevo, go to SMTP & API, then API keys, and create a key. It starts with `xkeysib-`. An `xsmtpsib-` key is an SMTP key and gets rejected by the HTTPS API.
2. In Brevo, go to Senders and verify the sender address.
3. Set `BREVO_API_KEY` to the API key and `EMAIL_FROM` to `GT Lost and Found <your-verified-address>`.

Verification and password-reset links point at `FRONTEND_URL`. Set it to the frontend URL you use, or to `http://localhost:5173` when testing against a local frontend.

### Frontend

- The iOS build reads `VITE_API_URL`, which should be the Render backend URL. Run `npm run build` and `npx cap sync ios` after changing it, since Vite bakes the value into the bundle.
- When the web frontend is hosted, set `FRONTEND_URL` and `PROD_HOSTNAME` on Render to its URL.

## API Summary

### Auth

- `GET /test`
- `POST /auth/register`
- `POST /auth/forgot-password`
- `POST /auth/login`
- `POST /auth/logout`

### Users

- `GET /users/me`
- `GET /users/{userId}`

### Items

- `POST /items`
- `PATCH /items/{itemId}`
- `DELETE /items/{itemId}`
- `GET /items/{itemId}`
- `GET /items`
- `GET /items/nearby`
- `GET /items/{itemId}/claims`

### Claims

- `POST /claims`
- `DELETE /claims/{claimId}`
- `PATCH /claims/{claimId}/accept`
- `PATCH /claims/{claimId}/reject`
- `GET /claims/me`

### Chat

- `GET /chat/threads`
- `GET /chat/threads?itemId={itemId}`
- `POST /chat/threads`
- `GET /chat/threads/{threadId}/messages`
- `POST /chat/threads/{threadId}/messages`

## Query Parameters

### `GET /items`

All filters are optional:

- `status`
- `category`
- `userId`

Examples:

- `/items`
- `/items?status=OPEN`
- `/items?category=Bags`
- `/items?userId=12`
- `/items?status=RESOLVED&category=Electronics`

### `GET /items/nearby`

Required query params:

- `lon`
- `lat`
- `radiusMeters`

Example:

- `/items/nearby?lon=-84.3963&lat=33.7756&radiusMeters=500`

## Development Notes

- The root `package.json` is not the main app entry point; frontend dependencies live in `frontend/package.json`.
- `frontend/README.md` is outdated and should not be treated as the source of truth.
- The backend stores auth in an `access_token` HTTP-only cookie.
