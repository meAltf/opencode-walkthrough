# opencode-walkthrough

A hands-on walkthrough of driving [opencode](https://opencode.ai) against a real
codebase. This repo exists to record what a working setup looks like: an opencode
configuration, a small set of project skills, and a full-stack app built by
following them.

Every piece of work lives in its own folder with its own README, so changes are
scoped and reviewable folder by folder.

## Layout

```
opencode-walkthrough/
├── .opencode/            # opencode project config
│   └── skills/           # project skills the agent loads on demand
│       ├── angular-feature/SKILL.md
│       ├── debug-backend/SKILL.md
│       └── spring-api/SKILL.md
├── backend/              # Spring Boot 4.1 REST API  (Java 21, Maven)
│   ├── pom.xml
│   ├── src/main/         # application code
│   ├── src/test/         # 17 tests
│   └── README.md         # API reference, error codes, design notes
├── frontend/             # Angular 20 SPA            (TypeScript, npm)
│   ├── src/app/          # components, services, models
│   └── README.md
├── opencode.json         # permissions, MCP servers, agent models
└── README.md             # you are here
```

## Working on the app

The two apps are independent. Run the backend first, then the frontend — the
frontend proxies `/api` to `http://localhost:8080` via
`frontend/proxy.conf.json`, so both must be running for the UI to reach the API.

```bash
# terminal 1 — API on :8080
cd backend && mvn spring-boot:run

# terminal 2 — UI on :4200
cd frontend && npm install && npm start
```

Verify a change before you commit it:

```bash
cd backend  && mvn test     # 17 tests
cd frontend && npm run build
```

## opencode configuration

`opencode.json` holds three things.

**Permissions** decide what the agent may do without asking. Edits, web fetches,
web search, skills, todo tracking, and questions are all `allow`; `bash` is `ask`,
so shell commands still get your confirmation.

**MCP servers** extend the agent with outside tools:

| Server   | Kind    | Purpose                                  |
|----------|---------|------------------------------------------|
| `docs`   | remote  | Context7 — current library documentation |
| `weather`| local   | Open-Meteo forecasts and climate data    |

**Agents** pin a model per mode; the default `plan` agent is set to
`opencode/gpt-5.1-codex`.

## Skills

Skills are the workflow this repo teaches. The agent picks one up when a task
matches its description.

| Skill              | Applies to                                            |
|--------------------|-------------------------------------------------------|
| `spring-api`       | Building a Spring Boot endpoint: controller → service → DTOs → validation → logging → error handling |
| `angular-feature`  | Building an Angular feature: component → service → API wiring → error toast → UI logic |
| `debug-backend`    | Debugging a backend fault: logs → failing layer → reproduce → minimal fix → test → validate |

They are deliberately short checklists. The detail lives in the code they produce —
see `backend/README.md` for how a `spring-api` endpoint is actually layered.

## Change log

Kept per folder so a diff in one place does not have to be read alongside the
others. Newest first.

### Root

- Restructured the repo into `backend/` and `frontend/` so each is independently
  buildable and documented.
- Added the root `.gitignore` for Maven, Node, and build output.
- Added `opencode.json` with permissions, the `docs` and `weather` MCP servers, and
  a pinned `plan` agent.
- Added the `spring-api`, `angular-feature`, and `debug-backend` skills.

### `backend/`

- Spring Boot 4.1 onboarding API: in-memory storage, PBKDF2 password hashing,
  bean validation, a standardized `ApiResponse` envelope, and `X-Request-Id`
  correlation via `RequestIdFilter` + SLF4J MDC. 17 tests.

### `frontend/`

- Angular 20 onboarding form plus lookup by id and by email, with a toast stack for
  API errors and a service layer over `/api/v1/users/onboarding`.
