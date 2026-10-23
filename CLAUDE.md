# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository overview

This is a graduation project: **《基于 Spring Boot 与 Vue 的智能仓储管理系统设计与实现》** (a Spring Boot + Vue warehouse management system, with AHP-TOPSIS slot recommendation as a core module). The repository contains three layers plus supporting material:

- **Root `*.html` + `assets/`** — a complete static design prototype (HTML/CSS/JS, no build step). It is the visual/interaction reference, not the production app.
- **`backend/`** — Spring Boot 4.0.8 (Java 21) + MyBatis-Plus + MySQL/H2. REST API under `/api/v1`.
- **`frontend/`** — Vue 3 + Vite + Pinia + Vue Router + ECharts SPA. It proxies `/api` to the backend and mirrors the static design.
- **`simulator/`** — Python MQTT virtual device simulator plus an `amqtt` broker config for local end-to-end tests. No physical hardware is used or required.
- **`experiments/`** — reproducible slot-assignment experiment. Its JSON output is served by the analytics API and drives the analytics page.
- **`docs/`** — thesis topic/outline, experiment report, technical plan, API reference, design checklist, screenshots.

The static demo and the Vue app share the same design tokens. `assets/css/app.css` is the source; `frontend/src/styles/app.css` is a copy used by Vite. If you change design tokens or shared component styles, update both, otherwise the reference demo and the app drift apart.

The static demo is published to GitHub Pages at <https://yueliangshuxia.github.io/cangxu-wms/> from the orphan `gh-pages` branch (only root `*.html` + `assets/`), while `master` holds the full source. Static login validates a small hard-coded demo account list in `assets/js/app.js` and stores the session in `localStorage` under `wms_demo_user`; it is a client-side demo gate, not real authentication.

## Commands

### Backend

```bash
cd backend
./mvnw spring-boot:run                     # dev profile: H2 in-memory, auto schema.sql + data.sql
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
./mvnw test                                # all tests
./mvnw -Dtest=AhpWeightCalculatorTest test # one test class
./mvnw -Dtest=InboundRecommendationTest,ModuleIntegrationTest test
./mvnw -DskipTests package                 # build target/wms-0.0.1-SNAPSHOT.jar
./mvnw clean package -Pportable -DskipTests # copy frontend/dist into the jar; serve everything on :8080
```

The `portable` profile calls `maven-resources-plugin` at `prepare-package` to copy
`../frontend/dist` into `target/classes/static`; `SpaController` forwards the SPA
routes to `index.html`.

MySQL first-time setup:

```bash
mysql -u root -p < backend/database/mysql-init.sql
```

It creates database `cangxu_wms`, user `wms` / `wms2026`, all tables, and seed data. The app's MySQL profile connects with those defaults (`DB_USERNAME` / `DB_PASSWORD` override them). The `mysql` profile sets `spring.sql.init.mode: never`; it expects this script to have been run. The H2 dev profile is the default and resets on every restart.

Seed logins: `chenyu / wms2026` (MANAGER), `admin / admin123`, `linzhou / wms2026` (RECEIVER), `zhouke / wms2026` (INSPECTOR).

### Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, proxy /api -> http://localhost:8080
npm run build      # production build; this is the current compile check
npm run preview
```

There is no lint or unit-test script in `frontend/`. Use `npm run build` to catch template/import errors.

### Simulator

```bash
cd simulator
pip install -r requirements.txt
python simulator.py --dry-run
python simulator.py --host localhost --port 1883
python simulator.py --scenario heatwave
python simulator.py --scenario agv-fault
```

To let the backend consume MQTT, start it with `MQTT_ENABLED=true MQTT_BROKER_URL=tcp://127.0.0.1:1883` (default is `false`, so the app starts without a broker). The broker can be Mosquitto, EMQX, or the bundled `amqtt-broker.yaml` (`amqtt -c simulator/amqtt-broker.yaml`). The config must allow subscribing to `wms/sz01/#`; otherwise the broker returns Suback 0x80. Topic pattern: `wms/sz01/sensor/{code}/telemetry`, `wms/sz01/agv/{code}/state`, `wms/sz01/rfid/{code}/scan`.

### Experiment

```bash
python experiments/run_experiment.py
```

Writes `backend/src/main/resources/experiments/algorithm-comparison.json`. Re-run it after changing the simulation; the analytics endpoint reads that file from the classpath.

## Backend architecture

Package-by-feature under `com.cangxu.wms`:

- `auth` + `security` — login, JWT, roles, and user CRUD (`UserController` / `UserService`, ADMIN/MANAGER only, last-admin protection). `SecurityConfig` permits login/health/H2, the static SPA shell, and the known SPA routes; everything else is authenticated. `JwtAuthenticationFilter` builds a stateless `SecurityContext` from the Bearer token. Mutating endpoints use `@PreAuthorize` with `ADMIN` / `MANAGER` / `RECEIVER` / `INSPECTOR`.
- `masterdata` — products, suppliers, customers, inventory, zones, locations. Inventory status is computed per **SKU total** available vs safety stock, not per location row.
- `inbound` — inbound orders/items and the core algorithm endpoint. `InboundService.recommend(id)` loads `FREE` locations, builds four metrics (turnover, order correlation, distance, fit), computes AHP weights via `AhpWeightCalculator`, ranks by `TopsisRanker` (distance is a cost criterion), and returns the top candidates. `putaway()` writes the item location, marks the order `DONE`, marks the location `OCCUPIED`, upserts inventory, and writes an audit log.
- `outbound` — order list/create, wave generation, pick/review, ship, and `/outbound/route` built from `outbound_item` rows sorted zone → row → column.
- `stocktake` — task list/summary/create/start/scan and difference items.
- `returns` — return orders, status summary, inspection disposition (`resell` / `repair` / `scrap`).
- `device` — device list/summary/telemetry plus `MqttSubscriber`. The subscriber is conditional on `mqtt.enabled`; payloads use `{deviceCode, metric, value, unit, state?}`.
- `settings` — `system_setting` key-value store for warehouse info, alert flags, and notification flags.
- `audit` — `AuditService.logCurrent(...)` reads the current user and request IP from Spring context and writes `audit_log`; other services call it after mutations.
- `dashboard` — real counts, 7-day inbound/outbound trend, zone occupancy, stock accuracy, inventory days, and recent audit activity.
- `analytics` — 7/30/90-day trend, category shares, zone×aisle heatmap, KPIs, plus the experiment JSON. `ExperimentService` falls back to zeroed results if the resource is missing.
- `alert` — `/alerts` derives low-stock, offline-device, temperature/humidity, pending-return, and pending-stocktake notifications.
- `export` — UTF-8 BOM CSV endpoints for inventory, locations, inbound, outbound, stocktakes, returns, audit logs, master data, and analytics.
- `algorithm` — plain Java AHP/TOPSIS/Apriori implementations with unit tests. Keep these independent of Spring so they are easy to test and reuse. `InboundService` feeds real outbound quantities and Apriori rules into the recommendation; do not reintroduce hash-based scores.
- `common` — `Result<T>` envelope (`code`, `message`, `data`, `traceId`), `PageResult`, exceptions and `GlobalExceptionHandler`.
- `config` — security, MyBatis-Plus pagination, startup initializers.

All mappers are MyBatis-Plus `BaseMapper` interfaces annotated `@Mapper`; there are no XML mapper files. Services generally use `LambdaQueryWrapper`. The API returns entities/DTO records directly; no MapStruct.

Startup behavior matters in dev and MySQL:

- `LocationInitializer` (`@Order(10)`) creates 5 zones if absent, then generates the A–E location grid, skipping codes already seeded. Seed has 8 known locations, so a fresh DB ends with 140 locations.
- `DeviceTelemetryInitializer` (`@Order(20)`) seeds 24 hours of temperature/humidity for `TH-A01` and current values for `TH-B01` / `TH-D01` when `device_telemetry` is empty.

## Frontend architecture

- `src/main.js` installs Pinia and the router, imports the generated icon sprite (`src/icons.js`), and loads `src/styles/app.css`.
- `src/layouts/AppLayout.vue` owns the rail, topbar, theme toggle, command palette, notification/user popovers, and mobile rail. `src/config/nav.js` is the single source for nav items and page metadata.
- `src/router/index.js` maps the 12 business routes plus `/login`; `LoginView` is public, everything else requires `wms_token` in localStorage.
- `src/api/http.js` is the shared Axios instance. It unwraps the backend `{code,message,data,traceId}` envelope, attaches `Authorization: Bearer <wms_token>`, and clears the token + redirects to `/login` on HTTP 401. `downloadCsv` uses `fetch` because binary CSV must bypass the JSON interceptor. API modules split by domain (`auth.js`, `masterdata.js`, `warehouse.js`, `dashboard.js`, `users.js`, `analytics.js`, `alerts.js`, `exports.js`).
- State: `stores/auth.js` (`wms_token`, `wms_user`), `stores/theme.js` (`wms-theme`), `stores/toast.js`. Components use `Icon.vue` with `<use href="#i-name">` backed by the injected sprite.
- ECharts views (`DashboardView`, `DevicesView`, `AnalyticsView`) create chart instances in `onMounted`, watch `theme.theme` to re-render, and dispose on unmount. Reuse this pattern rather than adding a chart wrapper library.
- Data wiring: dashboard, analytics, inbound, outbound, inventory, locations, masterdata, devices, audit, stocktake, returns, settings, user management, alerts, and CSV exports call the Spring Boot API. Analytics renders the generated experiment JSON; dashboard trend/occupancy come from SQL aggregates. The static root HTML pages are separate and are not connected to the backend.

## Data, schema and generated files

- `backend/src/main/resources/schema.sql` and `data.sql` are the source of truth for the H2 dev profile.
- `backend/database/mysql-init.sql` is generated from `schema.sql` + `data.sql` (seed `INSERT`s are changed to `INSERT IGNORE`), plus database/user creation. If you edit `schema.sql` or `data.sql`, regenerate this file before testing MySQL. Tables and rows with unique keys are idempotent; rows in `inventory`, `audit_log`, and detail tables without unique keys will be appended on a re-run. `CREATE USER IF NOT EXISTS` does not reset an existing user's password.
- MySQL reserved words already caused two renames; keep them: `device.signal_strength` (not `signal`) and `device_telemetry.metric_value` (not `value`). Check new columns against MySQL keywords.
- H2 dev data disappears on restart; MySQL data persists. `spring.sql.init.mode: never` under the MySQL profile is intentional.
- Static demo data lives in `assets/js/data.js`; the Vue app does not read it. Do not assume changes there affect the real app.

## Project-specific conventions

- User-facing copy is Chinese; code identifiers and API field names are English.
- The app is normally run on Windows with Git Bash. Maven is not installed globally — always use `./mvnw`. Docker is not available in this environment; do not add Docker-only commands as the primary workflow.
- The backend JWT secret and MySQL demo credentials are local-development defaults; change them before any real deployment.
- Do not introduce physical-hardware requirements. The IoT layer is intentionally virtual (Python simulator + MQTT); the thesis title deliberately avoids promising hardware.
- Redis is not used. Do not re-add `spring-boot-starter-data-redis` without a real cache requirement; the current stack queries MySQL/H2 directly.
- Prefer read-only verification against the running app for UI changes, and add backend tests under `src/test/java` when changing services. `ModuleIntegrationTest` is `@Transactional` so its mutations roll back.

## Agent skills

### Issue tracker

Issues live as local markdown under `.scratch/<feature-slug>/`; PRs are not a triage surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Canonical five-state vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`), recorded as `Status:` lines. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
