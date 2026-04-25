# studio-ui — React/TypeScript Frontend

Replaces legacy JSF screens for administration, user/group management, repository settings, notifications.

## Tech Stack

For current dependency versions, read `package.json`.

- **React** v19 with functional components and hooks
- **TypeScript** v6 in strict mode
- **Ant Design** v6 for UI components
- **Zustand** for state management (`appStore`, `userStore`, `notificationStore`, `traceStore`)
- **React Router** with `createBrowserRouter`, scoped to backend context path
- **i18next + react-i18next** for internationalization
- **@stomp/stompjs** for WebSocket notifications (reconnecting singleton)
- **Vite** v8 + `@vitejs/plugin-react` (`vite.config.ts`)
- **ESLint** v10 (flat config: `eslint.config.js`) + **Stylelint** + **Husky** pre-commit hooks
- **Vitest** + React Testing Library configured (`vite.config.ts`, `jsdom` environment)

## Project Structure

```
src/
├── App.tsx              # Root: error boundary, auth gate, router mount
├── index.tsx            # Entry: inits i18n, mounts App
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

1. `index.tsx` initializes i18n, mounts `App` into `#appRoot`
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

Tests are co-located with their source files (e.g. `src/containers/DeployModal.test.tsx` alongside `DeployModal.tsx`). Run all with `npm test` (one-shot via `vitest run --coverage`), or watch mode with `npm run test:watch`.

- Mock `services` module for API calls, `react-i18next` for translations
- `vitest.setup.ts` polyfills `MessageChannel`, `ResizeObserver`, `matchMedia` for jsdom. The `MessageChannel` polyfill delivers messages via `setTimeout(0)` — required for React's scheduler to commit async-scheduled state updates
- **Fixing "current testing environment is not configured to support act(...)"**: this warning fires when a component's async callback chain schedules a `setState` after an outer `act(async () => { await userEvent.click(...) })` wrapper has already closed — the late update lands outside any active act scope. The fix is to **remove** the outer `act` wrapper. `userEvent` already wraps clicks in act internally, and the following `waitFor(...)` retries each run inside their own act scope, catching detached promise-tail updates. Example:

    ```tsx
    // ❌ wrong — outer act closes before the async onFinish chain reaches setState
    await act(async () => {
        await userEvent.click(screen.getByText('Save'))
    })
    await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(...))

    // ✅ right — userEvent's internal act covers the sync part,
    // waitFor's retries cover the detached async tail
    await userEvent.click(screen.getByText('Save'))
    await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(...))
    ```

    For the initial `render()` it's still fine to wrap in `await act(async () => { render(...) })` so mount-time effects flush before the first assertion — the issue is only with wrapping `userEvent` interactions.
- **Ant Design Modal in jsdom**: Modal uses CSS animations (`ant-zoom-appear`) that prevent body content from rendering synchronously. Wrap the initial `render()` in `await act(async () => { ... })` to flush async effects (e.g., API loads in `useEffect`), or use `waitFor` to wait for content to appear
- **Per-test store overrides**: Use `vi.spyOn(storeModule, 'useUserStore').mockReturnValue(...)` with `mockRestore()` in a `finally` block. Do not mutate module exports directly — if the test throws before restoration, leaked state breaks subsequent tests
- **Avoid hardcoded Ant Design default labels** (e.g., "OK", "Cancel") in assertions — they depend on AntD locale config. Select buttons by excluding known buttons (save, close) or by setting explicit `okText`/`cancelText` props
- **Mock child components** to capture props via `vi.fn()` when testing a parent orchestrator (e.g., MergeModal). Use a `getLatestProps` helper that reads the last mock call — earlier calls may have stale closures after React re-renders
- **Ant Design `Table` causes infinite `act()` loops in jsdom**: Components that render `Table`, `Descriptions`, or other heavy AntD components with async `useEffect` data loading will hang during `act()`. Mock `antd` entirely with simple HTML equivalents (`<table>`, `<dl>`, `<button>`, etc.) and flush async effects via `await act(async () => { render(...); await new Promise(r => setTimeout(r, 50)) })`. See `ConflictResolutionStep.test.tsx` for the full mock pattern
- **Stable `react-i18next` mock**: Define the `t` function once inside the `vi.mock` factory, not inline in the return. A new `t` reference each render causes infinite `useCallback`/`useEffect` loops when `t` is in a dependency array
- **Do not spy on or mock `console.*` in tests.** `vitest-fail-on-console` is wired into `vitest.setup.ts` and fails any test that logs `console.error`. If a test trips it, fix the source — remove redundant logging or demote to `console.warn` (silenced globally: vitest-fail-on-console's `silenceMessage` drops every `methodName === 'warn'` call, so warns never fail and never print). Per-test `vi.spyOn(console, 'error').mockImplementation(...)` is forbidden; it hides real regressions and interacts poorly with `vitest-fail-on-console`'s per-test re-wrap
- **Do not edit `failOnConsole` options in `vitest.setup.ts`.** Don't add new `silenceMessage` patterns, `skipTest` entries, or widen the existing ones to make a failing test pass. The existing silences are reserved for jsdom/framework limitations that have no source-level fix (e.g., `Not implemented: navigation`, rc-form's orphan `useForm` warning). Any other noise must be fixed at the source

## Quality Rules

- Use `apiCall` wrapper, never raw `fetch`
- Guard screens with `PermissionContext` and `SystemContext` flags
- Add translations from day one — no hardcoded strings
- Prefer Zustand selectors over full store subscriptions
- Unsubscribe WebSocket listeners or use `cleanupWebSocket` to prevent duplicates
- **Do not call `console.error` from components, hooks, or services.** Propagate errors via callbacks, thrown exceptions, or explicit state so callers (error boundaries, `notification.error`, future global logger) decide how to surface them. `console.warn` is allowed for transient recoverable signals (reconnect attempts, queued work, disconnected sends) — `vitest-fail-on-console` silences every warn via `silenceMessage`, so warns are harmless in CI but still visible in the browser dev console during normal development
- Use current Ant Design API — avoid deprecated props:
  - `Spin`: use `description` instead of `tip`
  - `Modal`: use `destroyOnHidden` instead of `destroyOnClose`, `mask={{ closable }}` instead of `maskClosable`
  - `Space`: use `orientation` instead of `direction`
  - `Typography.Text`: use `ellipsis={{ tooltip: text }}` for conditional truncation tooltips (only shows on overflow)
- **Never reach into a library's internals.** Import only from a package's public surface — its main entry (e.g. `antd`, `react`) or its documented subpath exports declared in the package's `exports`/`typesVersions` map (e.g. `antd/es/select` for `DefaultOptionType`). Forbidden in particular:
  - **Transitive dependencies** that aren't listed in `package.json` (e.g. `@rc-component/form/lib/interface`, `rc-util/...`) — they are implementation details of a direct dep and may disappear on a minor upgrade.
  - **Undocumented deep paths** of any package — `<pkg>/src/...`, `<pkg>/internal/...`, `<pkg>/lib/<file>` not surfaced via the package's exports map, files inside `node_modules` reached by hand-written paths, etc.

  If a needed type/value is not re-exported, prefer the public alias (e.g. antd re-exports `Rule` as `FormRule`); if no public export exists, derive a local type — do not reach into internals.
