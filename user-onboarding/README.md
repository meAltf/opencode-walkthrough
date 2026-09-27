# User Onboarding

Full-stack user onboarding app — a Spring Boot REST API with an Angular UI. Built
with opencode by following the project skills in
[`.opencode/skills`](../.opencode/skills): `spring-api` for the backend layers,
`angular-feature` for the UI.

This folder is self-contained. Everything the app needs is here.

## Layout

```
user-onboarding/
├── backend/          # Spring Boot 4.1 REST API  (Java 21, Maven)
│   ├── pom.xml
│   ├── src/main/
│   ├── src/test/     # 17 tests
│   └── README.md     # API reference, error codes, design notes
└── frontend/         # Angular 20 SPA            (TypeScript, npm)
    ├── src/app/
    └── README.md
```

Each side has its own README with the detail — start there rather than here.

## Run

Start the backend first: the UI proxies `/api` to `http://localhost:8080`, so the
API has to be up before the frontend is useful.

```bash
# terminal 1 — API on :8080
cd backend && mvn spring-boot:run

# terminal 2 — UI on :4200
cd frontend && npm install && npm start
```

Then open <http://localhost:4200>; it redirects to `/onboarding`.

## Verify

```bash
cd backend  && mvn test       # 17 tests
cd frontend && npm run build  # production bundle → frontend/dist
```

## What it does

| Method | Path                                | Success             |
|--------|-------------------------------------|---------------------|
| POST   | `/api/v1/users/onboarding`          | `ONB_201_USER_ONBOARDED` |
| GET    | `/api/v1/users/onboarding/{id}`     | `ONB_200_USER_FOUND`     |
| GET    | `/api/v1/users/onboarding?email=`   | `ONB_200_USER_FOUND`     |

Submit the form, or look a user up by the id or email the response returns.
Failures surface as toasts rather than raw errors. See
[`backend/README.md`](backend/README.md) for the request body, the full error-code
table, and the design notes.
