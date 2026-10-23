# LootRadar

A PC game deal tracker for Steam, Epic Games, GOG, and Microsoft / Xbox PC. Compare current promotions, save games to a wishlist, set target prices, and follow observed price history.

## Features

- Steam, Epic Games, and GOG promotions through CheapShark; Microsoft PC promotions verified against Microsoft's Windows compatibility catalog.
- Search, store and budget filters, free-game filters, sorting, and pagination.
- Game details with store comparisons and links to available offers.
- Guest wishlists and account wishlists with Google or Steam sign-in; optional import of your browser's saved games.
- Top sales ranked by discount and a separate Steam upcoming-games view, also available within wishlists.
- USD prices with approximate RUB and KZT display using Central Bank of Russia exchange rates.
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

Open the project folder and import `backend/pom.xml` as a Maven project. Select a JDK 21 or newer. The backend entry point is `dev.lootradar.LootRadarApplication`. Run the frontend separately with `npm run dev:frontend`, or use `npm run dev` from the root. The combined launcher reuses a ready LootRadar backend on port 8080 (including one started from IntelliJ); otherwise it starts both services. Ctrl+C stops only the processes the launcher started.

If the browser reports `ERR_CONNECTION_REFUSED`, keep the frontend terminal running and open http://127.0.0.1:5173/. If port 8080 belongs to another service or the IntelliJ backend is still starting, the launcher prints an explanation before starting the frontend. If port 5173 is already occupied, use the existing frontend or stop it before starting another.

## Configuration

Copy `.env.example` to `.env` for Docker configuration. Keep credentials out of Git. For an external database, configure `DATABASE_URL` (JDBC URL), `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. Flyway applies migrations at startup.

### Google sign-in for local development

Create a **Web application** OAuth client in Google Auth Platform. Configure branding, select an External audience in Testing mode, and add your Google account as a test user. Use only the basic `openid`, `email`, and `profile` scopes. Register this exact authorized redirect URI:

```text
http://127.0.0.1:5173/login/oauth2/code/google
```

The backend optionally reads `${user.home}/.lootradar/oauth.properties` (on Windows, `%USERPROFILE%\.lootradar\oauth.properties`). This file stays outside the repository. Add your credentials there:

```properties
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
APP_BASE_URL=http://127.0.0.1:5173
```

Restart `LootRadarApplication` in IntelliJ after changing these settings, then use `http://127.0.0.1:5173/` consistently so the session and callback use the same host. Google sign-in is disabled when credentials are absent. JavaScript origins are not required for this server-side OAuth flow. Environment variables with the same names can also be used; keep the secret out of frontend variables and Git. For deployment, set `APP_BASE_URL` to the public HTTPS origin, register its `/login/oauth2/code/google` callback, and set `SESSION_COOKIE_SECURE=true`.

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
| GET | `/api/currencies` | USD, RUB, and KZT conversion rates and freshness |
| GET | `/api/auth/session` | Sign-in state, Google availability, and CSRF token |
| GET | `/api/auth/steam` | Start Steam OpenID sign-in |
| GET | `/oauth2/authorization/google` | Start Google sign-in when configured |
| POST | `/api/auth/logout` | End the authenticated session |
| POST | `/api/wishlist/import` | Copy guest wishlist into the signed-in account |
| GET | `/api/games/{id}` | Store comparison and price history |
| GET | `/api/wishlist` | Browser profile's saved games |
| PUT | `/api/wishlist/{id}` | Save a game and optional target price |
| DELETE | `/api/wishlist/{id}` | Remove a saved game |
| GET | `/actuator/health` | Service health |

Guest wishlist requests require a UUID in the `X-Profile-Id` header. Signed-in requests use the session's account identity instead. Mutating requests require the CSRF header and token returned by `/api/auth/session`, together with the session cookie. Save requests accept `{"targetPrice": 9.99}` or `{"targetPrice": null}`; target values are USD. The frontend creates and stores its guest profile identifier locally. Import preserves existing account targets and leaves the guest wishlist intact.

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

Source prices are USD for the US market. RUB and KZT are conversion estimates, not regional store prices; budget filters and target input remain USD. If exchange rates are unavailable, conversion is disabled or previously loaded rates are marked cached. CheapShark imports up to 60 promotions per supported store. Microsoft imports discounted Windows-compatible products, excluding Game Pass-only pricing. Search covers this imported catalog. Store coverage depends on upstream availability; outages are reported while cached data is retained.

Guest wishlists belong to a browser profile; clearing browser storage loses access to that profile. Google and Steam account wishlists persist across browsers using the same backend. The two sign-in providers create separate identities and are not automatically linked. Steam uses verified OpenID assertions and does not require an API key. Target prices show indicators in the app; email and push notifications are not implemented.

Charts grow from prices observed while the server runs. Existing provider historical lows are displayed separately. Upcoming titles come from Steam's coming-soon results; dates can change, and availability depends on that feed. Top sales means largest discounts, not sales-volume rankings. Run one backend instance for scheduled imports. Production requires HTTPS, secure session cookies, and deployment-specific OAuth callbacks.

## Credits

Price data and store redirects: [CheapShark](https://www.cheapshark.com/), [Microsoft PC deals](https://www.microsoft.com/en-us/store/deals/games/pc), and [Steam](https://store.steampowered.com/). Currency conversions: [Central Bank of Russia](https://www.cbr.ru/).

Steam, Epic Games, and GOG SVGs: [Simple Icons 16.0.0](https://github.com/simple-icons/simple-icons/tree/16.0.0/icons); Microsoft SVG: Simple Icons 11.0.0. Icons are distributed under CC0-1.0. Brand marks belong to their respective owners. The LootRadar logo was supplied for this project.
