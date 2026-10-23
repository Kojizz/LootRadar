# LootRadar

A PC game deal tracker for Steam, Epic Games, and GOG. Compare current promotions, save games to a wishlist, set target prices, and follow observed price history.

## Features

- Live promotions from three stores, refreshed hourly through CheapShark.
- Search, store and budget filters, free-game filters, sorting, and pagination.
- Game details with store comparisons and links to available offers.
- Persistent browser-specific wishlists and target-price indicators.
- Price history collected by the application, plus CheapShark historical lows.
- Responsive interface with the custom LootRadar logo and store brand icons.

## Tech stack

| Layer | Technology |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS, Radix UI, Recharts |
| Backend | Java 21+, Spring Boot 3.5, Spring MVC, Spring Data JPA |
| Database | PostgreSQL 17 in Docker; persistent H2 for local development |
| Migrations | Flyway |
| Verification | JUnit, Testcontainers, Vitest, Playwright |
| Deployment | Docker Compose; Java 25 runtime |

## Quick start with Docker

Install Docker with Compose, then run from the project root:

```sh
docker compose up --build -d
```

Open http://127.0.0.1:8080. The app serves both the frontend and API. PostgreSQL data persists in a Docker volume.

For port 8081 in PowerShell:

```powershell
$env:APP_PORT = '8081'
docker compose up --build -d
```

Stop with `docker compose down`. This preserves the database volume.

## Local development

Requirements: Node.js 22.12+ and JDK 21 or newer. The Maven wrapper downloads Maven automatically.

```sh
npm ci
npm run dev
```

Open http://127.0.0.1:5173. The API runs on port 8080 and the first catalog import starts shortly after boot. Ctrl+C stops the development processes. Local data is stored in `backend/data/`; it is separate from Docker's PostgreSQL data.

### IntelliJ IDEA

Open the project folder and import `backend/pom.xml` as a Maven project. Select a JDK 21 or newer. The backend entry point is `dev.lootradar.LootRadarApplication`. Run the frontend separately with `npm run dev --workspace frontend`, or use `npm run dev` from the root to start both services.

## Configuration

Copy `.env.example` to `.env` for Docker configuration. Keep credentials out of Git. For an external database, configure `DATABASE_URL` (JDBC URL), `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. Flyway applies migrations at startup.

## Project structure

```text
frontend/                 React application and frontend tests
backend/                  Spring Boot API, migrations, and Java tests
scripts/dev.mjs           Local development launcher
.github/workflows/ci.yml  Automated checks
compose.yaml              Application and PostgreSQL services
Dockerfile                Production build and Java runtime
```

## API

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/catalog` | Games, stores, and refresh status |
| GET | `/api/games/{id}` | Store comparison and price history |
| GET | `/api/wishlist` | Browser profile's saved games |
| PUT | `/api/wishlist/{id}` | Save a game and optional target price |
| DELETE | `/api/wishlist/{id}` | Remove a saved game |
| GET | `/actuator/health` | Service health |

Wishlist requests require a UUID in the `X-Profile-Id` header. Save requests accept `{"targetPrice": 9.99}` or `{"targetPrice": null}`. The frontend creates and stores its profile identifier locally.

## Verification

From the project root:

```sh
npm test
npm run build
npm run test:e2e --workspace frontend
```

For browser tests, install Playwright's Chromium with `npx playwright install chromium` on Linux; the Windows test configuration uses installed Google Chrome. The tests launch the frontend and use controlled API fixtures.

From `backend/`:

```sh
./mvnw verify
```

On Windows use `.\mvnw.cmd verify`. Docker is required for the PostgreSQL Testcontainers test. CI runs frontend checks, browser tests, and backend verification.

## First-version scope

Prices are USD for the US market. The feed imports up to 60 top promotions per store; search covers this imported catalog. Open a game to refresh its store comparison. Free offers may be temporary; confirm conditions at the store.

Wishlists belong to a browser profile, without account authentication or cross-device sync. Clearing browser storage loses access to that profile. Target prices show indicators in the app; email and push notifications are not implemented.

Charts grow from prices observed while the server runs. Existing provider historical lows are displayed separately. Run one backend instance for scheduled imports. Add authentication and production security controls before operating a shared public service.

## Credits

Price data and store redirects: [CheapShark](https://www.cheapshark.com/).

Steam, Epic Games, and GOG SVGs: [Simple Icons 16.0.0](https://github.com/simple-icons/simple-icons/tree/16.0.0/icons), distributed under CC0-1.0. Brand marks belong to their respective owners. The LootRadar logo was supplied for this project.
