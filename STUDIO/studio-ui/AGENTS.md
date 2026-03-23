# studio-ui — React/TypeScript Frontend

Replaces legacy JSF screens for administration, user/group management, repository settings, notifications.

## Tech Stack

For current dependency versions, read `package.json`.

- **React** with functional components and hooks
- **TypeScript** in strict mode
- **Ant Design** for UI components
- **Zustand** for state management (`appStore`, `userStore`, `notificationStore`, `traceStore`)
- **React Router** with `createBrowserRouter`, scoped to backend context path
- **i18next + react-i18next** for internationalization
- **@stomp/stompjs** for WebSocket notifications (reconnecting singleton)
- **Webpack** + Babel, `tsconfig-paths-webpack-plugin`
- **ESLint** (flat config: `eslint.config.js`) + **Stylelint** + **Husky** pre-commit hooks
- **Jest** + React Testing Library configured (`jest.config.js`, `ts-jest`, `jsdom`)

## Project Structure

```
src/
├── App.tsx              # Root: error boundary, auth gate, router mount
├── index.tsx            # Entry: sets public path, inits i18n, mounts App
├── setPublicPath.ts     # Adjusts __webpack_public_path__ for context path
├── components/          # Reusable widgets (accessManagement/, form/, modal/, shared)
├── containers/          # Feature screens (System, Security, Users, Groups, Tags, etc.)
├── contexts/            # React contexts for permissions/system flags
├── providers/           # SecurityProvider (wraps app with SystemContext + PermissionContext)
├── hooks/               # Shared hooks (forms, global events, websocket, scripts)
├── layouts/             # DefaultLayout, AdministrationLayout
├── pages/               # Standalone routes (403/404/500, Login)
├── routes/              # Router config (createBrowserRouter)
├── services/            # apiCall.ts (REST wrapper), websocket.ts, config.ts
├── store/               # Zustand stores (re-exported from index.ts)
├── locales/             # i18n bundles (*.en.ts), registered via addResourceBundle
├── types/               # Domain typings aligned with backend DTOs
├── constants/           # Domain constants (roles, repositories, system flags)
└── utils/               # Error handling and helpers
```

## Boot Sequence

1. `index.tsx` sets webpack public path, initializes i18n, mounts `App` into `#appRoot`
2. `App` fetches user profile, blocks rendering until auth completes, then mounts router inside Ant Design `App` provider and initializes WebSocket notifications

## Key Patterns

**REST calls**: Always use `services/apiCall.ts` — it prepends `CONFIG.CONTEXT`, handles JSON/text, surfaces validation errors, updates `useAppStore` flags for 401/403/404/500.

**State**: Zustand stores in `src/store/`. Use selectors that subscribe to specific slices to avoid re-renders.

**Routing**: `createBrowserRouter` with `CONFIG.CONTEXT` as basename. Legacy JSF content under `faces/*`. Admin features under `administration/` with `AdministrationLayout`.

**i18n**: Bundles in `src/locales/*.en.ts`, registered as namespaces. Reference keys as `t('common:menu.users')` or `t('system:tabs.repositories')`.

**Permissions**: `SecurityProvider` derives system flags from backend. Use `PermissionContext` and `SystemContext` to gate features (e.g., `isUserManagementEnabled`, `isExternalAuthSystem`).

**Forms**: `components/form` wraps Ant Design inputs. `hooks/useIsFormChanged.ts` enables dirty-state detection.

**Styling**: Global SCSS in `src/index.scss`. Feature-specific SCSS files co-located with components. BEM-like class naming. Prefer component-level styles over global overrides.

## Development

```bash
npm install                    # Install dependencies
npm run start                  # Dev server (proxied to backend)
npm run build                  # Production build with license check
npm run serve                  # Serve built bundle locally
```

Docker dev: set `_REACT_UI_ROOT_: http://localhost:3100` in root `compose.override.yaml` under `studio` service.

Maven: `mvn clean install` runs `npm install` + `npm run build` + `npm run test`  via `frontend-maven-plugin`. Bundled UI published under `/web/` relative to `CONFIG.CONTEXT`.

## Testing

Tests live in `test/` (not co-located). Run all with `npm test`, or a specific file with `npm test -- <path>`.

- Mock `services` module for API calls, `react-i18next` for translations
- `setupTests.ts` polyfills `MessageChannel`, `ResizeObserver`, `matchMedia` for jsdom — but the `MessageChannel` polyfill is a no-op stub, which breaks `Form.useWatch` value propagation
- **`Form.useWatch` does not work in tests** — the Ant Design form state synchronization relies on `MessageChannel` internally, and the jsdom polyfill does not deliver messages. Do not use `Form.useWatch` to drive component logic that needs testing. Instead, use local `useState` set from the field's `onChange` callback, and mock `components/form` in the test to render a native `<select>`/`<input>` that triggers the same `onChange` prop
- When component state is updated from async callbacks (e.g., API results inside `onChange`), wrap the triggering interaction in `act()` so React flushes the state updates

## Quality Rules

- Use `apiCall` wrapper, never raw `fetch`
- Guard screens with `PermissionContext` and `SystemContext` flags
- Add translations from day one — no hardcoded strings
- Prefer Zustand selectors over full store subscriptions
- Unsubscribe WebSocket listeners or use `cleanupWebSocket` to prevent duplicates
