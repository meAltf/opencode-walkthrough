# User Onboarding UI

Angular **20.3.32** frontend for the [Spring Boot user onboarding API](../backend/README.md).
Built following the `angular-feature` skill.

## Requirements

- Node `^20.19.0 || ^22.12.0 || >=24.0.0` (developed on Node 22.19.0)
- The Spring Boot API running on port **8080**

> **Why Angular 20 and not 22?** Angular CLI 22 requires Node `^22.22.3 || ^24.15.0 || >=26.0.0`.
> The installed Node is 22.19.0, which does not satisfy that, so 20.3.x is the newest
> version that works here. Upgrade Node to move to 22.

## Run

Start the backend first, then the dev server:

```bash
# terminal 1 — API on :8080
cd ../backend && mvn spring-boot:run

# terminal 2 — UI on :4200
npm install && npm start
```

Open <http://localhost:4200>. It redirects to `/onboarding`.

## Build

```bash
npm run build          # production bundle → dist/frontend
npm run watch          # rebuild on change
```

## How it talks to the API

Requests use **relative** paths (`/api/v1/users/onboarding`) and `proxy.conf.json`
forwards them to `http://localhost:8080` in dev, so there are no CORS issues and no
environment-specific base URL to configure. The proxy is wired in `angular.json`
under `serve.options.proxyConfig`.

If the API is not running, the client surfaces a friendly toast rather than a raw
network error (see `CLIENT_NETWORK_ERROR` in the service).

## API contract consumed

Every response uses the backend's standard `ApiResponse<T>` envelope
(`success`, `code`, `message`, `data`, `timestamp`). `OnboardingApiService` unwraps it
and converts any failure into a typed `OnboardingApiError` carrying the HTTP status
and the per-field `FieldError[]` the backend returns on validation failures.

| Call  | Endpoint                                        | Success code             |
|-------|-------------------------------------------------|--------------------------|
| POST  | `/api/v1/users/onboarding`                      | `ONB_201_USER_ONBOARDED` |
| GET   | `/api/v1/users/onboarding/{id}`                 | `ONB_200_USER_FOUND`     |
| GET   | `/api/v1/users/onboarding?email=`               | `ONB_200_USER_FOUND`     |

| Error code                      | HTTP | Toast heading           |
|---------------------------------|------|-------------------------|
| `ONB_400_VALIDATION_FAILED`     | 400  | Validation failed       |
| `ONB_404_USER_NOT_FOUND`        | 404  | User not found          |
| `ONB_409_EMAIL_EXISTS`          | 409  | Email already registered |
| `ONB_500_INTERNAL_ERROR`        | 500  | Server error            |
| `CLIENT_NETWORK_ERROR`          | 0    | Request failed          |

## Structure

```
src/app/
├── app.ts / .html / .scss          shell: topbar, nav, router outlet, toast stack
├── app.config.ts                   provideRouter + provideHttpClient(withFetch)
├── app.routes.ts                   lazy-loaded routes
├── core/
│   ├── models/api.models.ts        ApiResponse, OnboardedUser, OnboardingApiError
│   ├── services/
│   │   ├── onboarding-api.service.ts  HTTP calls + envelope/error unwrapping
│   │   └── toast.service.ts           signal-based toast queue
│   └── toast/toast-stack.*         toast viewport (renders the queue)
└── features/
    ├── onboarding/                 onboard form (reactive)
    └── lookup/                     search by email or user ID
```

Routes are lazy-loaded with `loadComponent`, so each feature is a separate chunk.

## How this maps to the `angular-feature` skill

1. **Create module/component** — Angular 20 is standalone-first, so these are
   standalone components (`Onboarding`, `Lookup`, `ToastStack`) rather than NgModules.
   `app.routes.ts` lazy-loads each one.
2. **Add service** — `OnboardingApiService` (API) and `ToastService` (notifications),
   both `providedIn: 'root'`.
3. **Integrate API** — `HttpClient` via `provideHttpClient(withFetch())`, typed models
   mirroring the backend DTOs, and a dev proxy to `:8080`.
4. **Handle error (toast)** — `GlobalExceptionHandler`-style failures are normalised
   into `OnboardingApiError` by the service; components surface them through
   `ToastService`. Server-side field errors are rendered inline under the matching
   input, not only in the toast.
5. **Add UI logic** — typed reactive forms with client-side validation, password
   visibility toggle, password-strength meter, submitting/loading state, result cards,
   lookup mode tabs, and a UUID format check.

## Client-side validation

Mirrors the backend's `OnboardingRequest` constraints so users get instant feedback,
but the backend remains the source of truth.

| Field           | Client rule                                | Backend rule                          |
|-----------------|--------------------------------------------|---------------------------------------|
| `email`         | required, email format, max 255            | `@NotBlank @Email @Size(max=255)`     |
| `fullName`      | required, 2–100 chars                      | `@NotBlank @Size(min=2, max=100)`     |
| `password`      | required, 8–72 chars                       | `@NotBlank @Size(min=8, max=72)`      |
| `dateOfBirth`   | required, must not be in the future        | `@NotNull @Past`                      |
| `termsAccepted` | must be `true`                             | `@AssertTrue`                         |

Because the client cannot fully replicate server rules, backend field errors are still
merged into the form display via a `serverErrors` signal.

## Notable Angular 20 details

- **Signals** for all component state (`signal`, `computed`, `toSignal`) — no
  `ChangeDetectorRef` juggling.
- **New control flow**: `@if` / `@for` blocks instead of `*ngIf` / `*ngFor`.
- **New naming convention**: files are `onboarding.ts`, not
  `onboarding.component.ts`; styles are colocated per component.
- `OnPush` change detection on every component.
- Shared primitives (`.panel`, `.field`, `.button`, `.pill`) live in `styles.scss`;
  component-specific styles stay in each component's `.scss`.
- Accessible labels, `aria-live` toasts, `role="tablist"` lookup tabs, and
  `prefers-reduced-motion` support.
