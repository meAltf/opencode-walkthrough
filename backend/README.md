# User Onboarding API

Spring Boot **4.1.1** REST API for user onboarding, layered per the `spring-api` skill:
Controller → Service → DTOs → Validation → Logging → Exception handling → Standardized response.

> Part of [opencode-walkthrough](../README.md). All commands below run from this
> `backend/` directory.

## Requirements

- Java 17+ (built and tested on Java 21)
- Maven 3.9+

## Run

```bash
mvn spring-boot:run
# or
mvn package && java -jar target/user-onboarding-api-0.0.1-SNAPSHOT.jar
```

Listens on `http://localhost:8080`.

## Endpoints

| Method | Path                        | Success code               | Description                     |
|--------|-----------------------------|----------------------------|---------------------------------|
| POST   | `/api/v1/users/onboarding`  | `ONB_201_USER_ONBOARDED`   | Onboard a new user (201)        |
| GET    | `/api/v1/users/onboarding/{id}`   | `ONB_200_USER_FOUND` | Fetch by id                     |
| GET    | `/api/v1/users/onboarding?email=` | `ONB_200_USER_FOUND` | Fetch by email             |

### Request body

```json
{
  "email": "alan@example.com",
  "fullName": "Alan Turing",
  "password": "s3cret-pass",
  "dateOfBirth": "1912-06-23",
  "termsAccepted": true
}
```

### Example

```bash
curl -X POST http://localhost:8080/api/v1/users/onboarding \
  -H 'Content-Type: application/json' \
  -d '{"email":"alan@example.com","fullName":"Alan Turing","password":"s3cret-pass","dateOfBirth":"1912-06-23","termsAccepted":true}'
```

```json
{
  "success": true,
  "code": "ONB_201_USER_ONBOARDED",
  "message": "user onboarding completed",
  "data": {
    "id": "f94840c1-d1fd-41c6-9b76-433900e3a000",
    "email": "alan@example.com",
    "fullName": "Alan Turing",
    "dateOfBirth": "1912-06-23",
    "status": "COMPLETED",
    "createdAt": "2026-09-27T11:46:39.925443Z"
  },
  "timestamp": "2026-09-27T11:46:39.925761Z"
}
```

## Standardized response

Every response — success or failure — uses the same `ApiResponse<T>` envelope:
`success`, `code`, `message`, `data`, `timestamp`.

### Error codes

| Code                          | HTTP | Meaning                              |
|-------------------------------|------|--------------------------------------|
| `ONB_400_VALIDATION_FAILED`   | 400  | Bean validation failed (per-field)   |
| `ONB_400_MALFORMED_BODY`      | 400  | Missing or unparseable JSON          |
| `ONB_404_USER_NOT_FOUND`      | 404  | No user for that id/email            |
| `ONB_409_EMAIL_EXISTS`        | 409  | Email already onboarded              |
| `ONB_500_INTERNAL_ERROR`      | 500  | Unhandled exception (details logged) |

Validation failures return a `data` array of `{field, message, rejectedValue}`.
**Passwords and other secret-ish fields are redacted** to `***REDACTED***` before
being echoed back.

## Design notes

- **Storage is in-memory** (`ConcurrentHashMap` in `UserOnboardingServiceImpl`), so the
  app runs with no external dependencies. Swap the maps for a JPA repository to persist.
- **Passwords are never stored or returned in plaintext** — `PasswordHasher` uses
  PBKDF2-HMAC-SHA256 (210k iterations, 16-byte random salt) from the JDK, so no extra
  dependency is needed. Use `matches()` to verify.
- **Duplicate emails are rejected atomically** via `Map.putIfAbsent`, avoiding a
  check-then-act race.
- **Request correlation**: `RequestIdFilter` generates/propagates an `X-Request-Id` header,
  puts it in the SLF4J MDC, and logs method/path/status/duration. The log pattern in
  `application.yml` prints it as `[<requestId>]`.

## Spring Boot 4 migration gotchas

Boot 4 / Spring Framework 7 / Jackson 3 break from Boot 3 conventions. This project uses:

- `spring-boot-starter-webmvc` — `spring-boot-starter-web` is **deprecated**.
- Jackson 3: `tools.jackson.databind.ObjectMapper`, **not** `com.fasterxml.jackson.databind`.
- `HandlerMethodValidationException.getParameterValidationResults()` replaced
  `getAllValidationResults()`.
- `@AutoConfigureMockMvc` / `@WebMvcTest` live in
  `org.springframework.boot.webmvc.test.autoconfigure`; `TestRestTemplate` in
  `org.springframework.boot.resttestclient`.
- `spring-boot-starter-test` no longer brings web test support — add
  `spring-boot-starter-webmvc-test`.

## Tests

```bash
mvn test
```

17 tests: controller integration (MockMvc, 9), service unit (5), password hasher (3).
# opencode-walkthrough
