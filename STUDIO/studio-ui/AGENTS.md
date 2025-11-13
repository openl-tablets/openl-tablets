# studio-ui - React/TypeScript Frontend Conventions

**Module**: `studio-ui` (OpenL Studio UI)  
**Location**: `STUDIO/studio-ui/`  
**Version**: 6.0.0-SNAPSHOT *(inherited from the parent Maven POM `org.openl.rules.studio-pom`)*  
**Last Updated**: 2025-11-13

---

## Table of Contents

- [Module Purpose](#module-purpose)
- [Quick Start](#quick-start)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Application Boot Sequence](#application-boot-sequence)
- [Routing & Navigation](#routing--navigation)
- [State Management](#state-management)
- [Services & Data Access](#services--data-access)
- [Internationalization](#internationalization)
- [Forms & Validation](#forms--validation)
- [Styling](#styling)
- [Configuration & Environment](#configuration--environment)
- [Integration with OpenL Studio Backend](#integration-with-openl-studio-backend)
- [Local Development & Build](#local-development--build)
- [Quality Guidelines](#quality-guidelines)

---

## Module Purpose

The `studio-ui` module delivers the modern React experience for OpenL Studio. It replaces legacy JSF screens for system configuration, user and group management, repository administration, notifications, and auxiliary utilities while still coexisting with JSF pages for editor/repository flows.

**Key capabilities:**

- Authenticate users and expose administration features based on their permissions.
- Manage system, security, repository, tags, notification, and mail settings inside a unified React SPA.
- Provide personal profile/settings pages and context-aware navigation.
- Bridge to existing JSF routes under `faces/*` so legacy features remain accessible.
- Surface platform events through a STOMP websocket channel and in-app notifications.

---

## Quick Start

```bash
# Step 0: Maven sync (installs Node/npm and runs npm install via frontend-maven-plugin)
mvn clean install -DskipTests 

# Step 1: run against backend from Docker compose (root compose.yaml)
docker compose up --build

# Step 2: launch webpack dev-server on http://localhost:3100
npm run start
```

- Maven must run before local hacks because `frontend-maven-plugin` installs frontend dependencies during `clean install`. Running Step 0 ensures `node_modules/` matches the backend build.
- Development server expects the OpenL backend to be available at the proxy target declared in `package.json` (`http://localhost:8080` by default).
- When running inside Docker, expose the React UI by setting `_REACT_UI_ROOT_` under the `webstudio` service in the repository root `compose.yaml`, e.g.:

  ```yaml
  services:
    webstudio:
      environment:
        _REACT_UI_ROOT_: http://localhost:3100
  ```

---

## Technology Stack

### Core framework
- React 18.3.1 with functional components and hooks.
- TypeScript 5.8.3 in strict mode.
- Node.js 24.x for build tooling only.

### UI & styling
- Ant Design 5.26 for base components.
- Global SCSS (`src/index.scss`) plus feature-specific SCSS files such as `MainMenu.scss` and `UserMenu.scss`.
- No CSS modules today; styles are either global overrides or scoped via BEM-like class names.

### State, data & realtime
- Zustand 5 stores (`appStore`, `userStore`, `notificationStore`) for client state.
- Native `fetch` wrapped by `services/apiCall` for REST calls.
- `@stomp/stompjs` websocket client with a reconnecting singleton in `services/websocket`.

### Internationalization
- i18next 25.3.1 + react-i18next 15.6.
- English resource bundles live in `src/locales/*.en.ts` and are registered via `i18next.addResourceBundle`.

### Tooling
- Webpack 5 with Babel 7 and `tsconfig-paths-webpack-plugin`.
- ESLint 9 (flat config) + Stylelint 16, wired through Husky pre-commit/pre-push hooks.
- `npm run start` serves through `webpack-dev-server`; `npm run build` performs a license check and emits production bundles.

### Testing
- `npm test` is currently a placeholder (`exit 0`). `src/setupTests.ts` is reserved for future Jest/RTL integration.

---

## Project Structure

```text
studio-ui/
├── public/               # Static assets and HTML shell
├── src/
│   ├── App.tsx
│   ├── index.tsx
│   ├── index.scss
│   ├── setPublicPath.ts
│   ├── components/
│   │   ├── accessManagement/...
│   │   ├── form/...
│   │   ├── modal/...
│   │   └── shared widgets (Logo, RouteErrorFallback, etc.)
│   ├── constants/        # Domain constants (roles, repositories, system flags)
│   ├── containers/       # Feature screens (System, Security, Users, Groups, Tags, etc.)
│   ├── contexts/         # React contexts for permissions/system flags
│   ├── hooks/            # Shared hooks (forms, global events, websocket, scripts)
│   ├── layouts/          # High-level page layouts
│   ├── locales/          # i18n bundles
│   ├── pages/            # Standalone routes (403/404/500, Login)
│   ├── providers/        # Cross-cutting providers (SecurityProvider)
│   ├── routes/           # Router configuration and helpers
│   ├── services/         # API wrapper, websocket client, config
│   ├── store/            # Zustand stores
│   ├── types/            # Domain typings aligned with backend DTOs
│   └── utils/            # Error handling and helpers
├── eslint.config.js
├── package.json
├── tsconfig.json
└── webpack.config.js
```

### Directory overview

- `components/` contains reusable building blocks. Domain-specific widgets for access management live under `components/accessManagement`, while `components/form` exposes Ant Design-based inputs with common validation behaviour.
- `containers/` implement full-featured screens. They orchestrate data fetching, compose components, and handle user actions for administration domains.
- `contexts/` and `providers/SecurityProvider.tsx` expose permission and system settings determined at runtime.
- `services/` centralises runtime configuration (`config.ts`), REST access (`apiCall.ts`), and websocket messaging (`websocket.ts`).
- `store/` holds the Zustand stores that gate rendering, login state, notifications, and allow components to subscribe to app data.
- `locales/` groups translation bundles (`*.en.ts`) that register with i18next as soon as the bundle is imported by `src/locales/index.ts`.
- `setPublicPath.ts` adjusts `__webpack_public_path__` at runtime so that assets resolve correctly when the app is hosted behind a context path.

---

## Application Boot Sequence

1. `src/index.tsx` sets the webpack public path, initializes i18n bundles, and mounts `App` into `#appRoot`.
2. `App` registers global error handlers, triggers an initial user/profile fetch, and blocks rendering until authentication completes. Once authenticated it mounts the router inside Ant Design's `App` provider and initializes websocket notifications.

```11:70:STUDIO/studio-ui/src/App.tsx
function App() {
    const { showLogin } = useAppStore()
    const { fetchUserInfo, isLoggedIn } = useUserStore()
    const { initializeWebSocket, cleanupWebSocket } = useNotificationStore()
// ... existing code ...
    return(
        <ErrorBoundary
            onError={(error: Error, errorInfo: any) => {
                errorHandler.logError(error, {
                    componentStack: errorInfo?.componentStack || undefined,
                    message: `App Level Error: ${error.message}`,
                })
            }}
        >
            <Suspense fallback={<div>Loading...</div>}>
                <AntApp>
                    <SecurityProvider>
                        <RouterProvider router={router} />
                    </SecurityProvider>
                </AntApp>
            </Suspense>
        </ErrorBoundary>
    )
}
```

---

## Routing & Navigation

The router is built with `createBrowserRouter` and scoped to the backend context path (`CONFIG.CONTEXT`). Routes are grouped under `DefaultLayout`, with an `AdministrationLayout` subtree dedicated to admin features. Legacy JSF content is still mounted under `faces/*`, and dedicated error pages exist for 403/404/500 flows.

```26:115:STUDIO/studio-ui/src/routes/index.tsx
const router = createBrowserRouter([
    {
        path: '/',
        element: <DefaultLayout />,
        errorElement: <RouteErrorFallback />,
        children: [
            {
                path: 'faces/*', // To integrate with JSF Repository and Editor tabs
            },
            {
                path: 'administration',
                element: <AdministrationLayout />,
                errorElement: <RouteErrorFallback />,
                children: [
                    {
                        index: true,
                        element: <System />,
                    },
// ... existing code ...
                ],
            },
            {
                path: '*',
                element: <Tesseract size={400} />
            }
        ],
    },
    {
        path: '/login',
        element: <LoginPage />,
        errorElement: <RouteErrorFallback />,
    },
    {
        path: '/logout',
        element: <RedirectRoute to="logout" />,
    },
], {
    basename: CONFIG.CONTEXT,
})
```

### Layouts

- `DefaultLayout` injects the global header, wraps route outlets, and displays server error screens when the stores request it.
- `AdministrationLayout` combines the left-side navigation (`MainMenu`) with the active administration pane (`Outlet`).

---

## State Management

Zustand stores live in `src/store` and are re-exported from `src/store/index.ts` for easy imports.

- `useAppStore` toggles modal pages (`showLogin`, `showForbidden`, `showNotFound`, `showServerError`).
- `useUserStore` retrieves authenticated user data and exposes `isLoggedIn` to gate the app.

```1:40:STUDIO/studio-ui/src/store/userStore.ts
export const useUserStore = create<UserStore>((set) => ({
    userProfile: undefined,
    userDetails: undefined,
    loading: false,
    error: null,
    isLoggedIn: false,
    fetchUserInfo: async () => {
        set({ loading: true, error: null })
        try {
            const userProfile = await apiCall('/users/profile')
            const userDetails = await apiCall(`/users/${userProfile.username}`)
            set({ userProfile, userDetails, isLoggedIn: true, loading: false })
        } catch (error) {
            set({ error, loading: false })
        }
    },
// ... existing code ...
}))
```

- `useNotificationStore` manages a websocket connection, sends outbound notifications, and watches for errors.

The `SecurityProvider` derives system flags and wraps the app with `SystemContext` and `PermissionContext`, allowing components to check feature enablement before rendering.

---

## Services & Data Access

All backend interaction flows through `services/apiCall.ts`. It prepends the backend context (`CONFIG.CONTEXT`), handles JSON/text responses, surfaces validation errors, and updates the global `useAppStore` flags for common HTTP status codes.

```1:73:STUDIO/studio-ui/src/services/apiCall.ts
import { notification } from 'antd'
import CONFIG from './config'
import { useAppStore } from 'store'
// ... existing code ...
const apiCall = async (url: string, params?: RequestInit, throwError = false) => {
// ... existing code ...
        .catch(error => {
            if (throwError) {
                throw error
            } else if (error instanceof EmptyError) {
            } else if (error instanceof Error) {
                notification.error({ message: error.toString() })
            }
        })
// ... existing code ...
}
```

`services/websocket.ts` exposes a singleton STOMP client that reconnects automatically and records active subscriptions. `useNotificationStore` is responsible for connecting, subscribing, and cleaning up to prevent duplicate listeners.

`services/config.ts` inspects `document.baseURI` to compute the current backend context path, keeping deployments under reverse proxies functional.

---

## Internationalization

- `src/i18n.ts` initializes i18next with English as both the starting language and fallback; `debug` toggles with `NODE_ENV === 'development'`.
- `src/locales/index.ts` imports all `*.en.ts` bundles so the side-effects register namespace-specific resources.
- To add a new locale:
  1. Create `src/locales/common.de.ts` (or the relevant namespace) that calls `i18next.addResourceBundle('de', 'common', {...})`.
  2. Import the file inside `src/locales/index.ts`.
  3. Switch language at runtime via `i18n.changeLanguage('de')` or persist the preference in local storage before boot.
- Namespaces align with filenames, so reference keys as `t('common:menu.users')` or `t('system:tabs.repositories')`.

---

## Forms & Validation

Components under `components/form` wrap Ant Design inputs and consolidate validation rules in `hooks.ts`. Use `useRules` and `usePasswordRules` when you want to automatically inject the mandatory "required" message with i18n support. `hooks/useIsFormChanged.ts` compares initial form values against current data to enable dirty-state prompts on save buttons.

---

## Styling

- Global styles and Ant Design overrides are placed in `src/index.scss`. Keep overrides minimal and prefer component-level SCSS files (e.g., `MainMenu.scss`, `UserMenu.scss`, `Repositories.scss`) for feature-specific styling.
- When introducing new styles, follow the existing convention: import the SCSS file alongside the component and scope selectors under an id or class to avoid leaking styles.

---

## Configuration & Environment

- `services/config.ts` inspects `document.baseURI` at runtime and exposes `CONFIG.CONTEXT`. Bundle assets are loaded relative to this context via `setPublicPath.ts`.
- `webpack-dev-server` respects the `proxy` value in `package.json`. Override it with `npm run start -- --proxy http://your-host`.
- Environment toggles returned by the backend are exposed through `SystemContext` (e.g., `isExternalAuthSystem`, `isUserManagementEnabled`, `isGroupsManagementEnabled`). Use these flags to hide unsupported UI features.
- Docker development: set `_REACT_UI_ROOT_` in `compose.yaml` to the dev server URL so the backend redirects to the React app.
- Circuits for tests: `npm test` currently exits immediately. Enable actual Jest tests by replacing the placeholder script and wiring up `setupTests.ts`.

---

## Integration with OpenL Studio Backend

- Maven module `STUDIO/studio-ui` inherits version `6.0.0-SNAPSHOT` from `org.openl.rules.studio-pom`. The `mvn clean install` lifecycle automatically runs `npm install`, `npm run build`, and copies the resulting `dist/` assets where the backend can serve them.
- The bundled UI is published under `/web/` relative to `CONFIG.CONTEXT`, which is derived from the servlet context (e.g., `/webstudio` in packaged deployments).
- Legacy JSF views remain accessible through `faces/*`. React routes coexist by sharing the same base path and delegating to server rendering where appropriate.
- Websocket endpoints live under `${CONFIG.CONTEXT}/web/ws`. Ensure reverse proxies (nginx, Apache) forward this path to allow STOMP subscriptions.
- Authentication relies on server-managed sessions. When the backend responds with 401/403/404/500, `apiCall` toggles the corresponding flags in `useAppStore`, prompting immediate UI feedback or redirects handled by `App.tsx`.

---

## Local Development & Build

- Install dependencies once: `npm install`
- Start the development server: `npm run start`
- Build for production (with OSS license check): `npm run build`
- Serve a built bundle locally: `npm run serve`

Docker-based development expects you to expose the React UI via `_REACT_UI_ROOT_` inside `compose.yaml` (see the module `README.md` for the exact snippet).

---

## Quality Guidelines

- Keep components focused and favour composition inside `containers/`.
- Use the shared `apiCall` wrapper instead of calling `fetch` directly so that authentication and error handling stay consistent.
- Guard new screens/components with `PermissionContext` and `SystemContext` flags when the backend may disable features (e.g., user management in external auth mode).
- Add translations to the relevant namespace from day one; avoid hard-coded strings.
- Prefer Zustand selectors that subscribe to specific slices to prevent unnecessary re-renders.
- When working with websocket updates, ensure you unsubscribe or rely on `useNotificationStore.cleanupWebSocket` to avoid duplicate subscriptions.