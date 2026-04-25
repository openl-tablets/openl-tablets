# studio-ui — React/TypeScript Frontend

Replaces legacy JSF screens for administration, user/group management, repository settings, notifications.

## Tech Stack

Always get list of current version of libraries from `package.json` — do not hardcode them here.
Use almost the latest versions when possible.

- **React** + functional components and hooks (TypeScript strict mode)
- **Ant Design** for UI components, **`@ant-design/icons`** for icons
- **Zustand** for state management (`appStore`, `userStore`, `notificationStore`, `traceStore`)
- **React Router** with `createBrowserRouter`, scoped to backend context path
- **i18next + react-i18next** for internationalization
- **@stomp/stompjs** for WebSocket notifications (reconnecting singleton)
- **Vite** + `@vitejs/plugin-react` (`vite.config.ts`)
- **ESLint** (flat config: `eslint.config.js`) + **Stylelint**
- **Vitest** + React Testing Library (`jsdom` environment)

## Project Structure

```text
src/
├── App.tsx              # Root: error boundary, auth gate, router mount
├── index.tsx            # Entry: inits i18n, mounts App
├── components/          # Reusable widgets (accessManagement/, form/, modal/, shared)
├── containers/          # Feature screens (System, Security, Users, Groups, Tags, Repositories, Trace, Merge…)
├── contexts/            # PermissionContext, SystemContext, GroupsContext
├── providers/           # SecurityProvider (wraps app with SystemContext + PermissionContext)
├── hooks/               # Shared hooks (forms, global events, websocket, scripts)
├── layouts/             # DefaultLayout, AdministrationLayout
├── pages/               # Standalone routes (403/404/500, Login)
├── routes/              # Router config (createBrowserRouter)
├── services/            # apiCall.ts (REST wrapper), websocket.ts, traceService.ts, config.ts
├── store/               # Zustand stores (re-exported from index.ts)
├── locales/             # i18n bundles (*.en.ts), registered via addResourceBundle
├── types/               # Domain typings aligned with backend DTOs
├── constants/           # Domain constants (roles, repositories, system flags)
└── utils/               # Error handling and helpers
```

## Boot Sequence

1. `index.tsx` initializes i18n and mounts `App` into `#appRoot`.
2. `App` fetches the user profile, blocks rendering until auth completes, then mounts the router inside Ant Design's
   `App` provider and initializes WebSocket notifications.

## Key Patterns

- **REST**: always use `services/apiCall.ts` — it prepends `CONFIG.CONTEXT`, handles JSON/text, surfaces validation
  errors, and updates `useAppStore` flags for 401/403/404/500.
- **State**: Zustand stores in `src/store/`. Use selectors that subscribe to specific slices to avoid re-renders.
- **Routing**: `createBrowserRouter` with `CONFIG.CONTEXT` as basename. Legacy JSF content under `faces/*`. Admin
  features under `administration/` with `AdministrationLayout`.
- **i18n**: bundles in `src/locales/*.en.ts`, registered as namespaces. Reference keys like `t('common:menu.users')` or
  `t('system:tabs.repositories')`.
- **Permissions**: `SecurityProvider` derives system flags from the backend. Use `PermissionContext` and `SystemContext`
  to gate features (e.g., `isUserManagementEnabled`, `isExternalAuthSystem`).
- **Forms**: `components/form` wraps Ant Design inputs. `hooks/useIsFormChanged.ts` drives dirty-state detection.
- **Styling**: global SCSS in `src/index.scss`; feature SCSS co-located with components. BEM-like class naming. Prefer
  component-level styles over global overrides.

## Development

```bash
npm install                    # Install dependencies
npm run start                  # Dev server (proxied to backend)
npm run build                  # Production build with license check
npm run serve                  # Serve built bundle locally
npm run lint                   # ESLint + Stylelint
npm run typecheck              # tsc --noEmit
```

Docker dev: set `_REACT_UI_ROOT_: http://localhost:3100` in root `compose.override.yaml` under the `studio` service.

Maven: `mvn clean install` runs `npm install` + `npm run typecheck` +  `npm run build` + `npm run test` via
`frontend-maven-plugin`. Bundled UI is published relative to `CONFIG.CONTEXT`.

## Testing

Tests are co-located with sources (e.g. `src/containers/DeployModal.test.tsx` next to `DeployModal.tsx`). Run all with
`npm test` (`vitest run --coverage`); watch mode with `npm run test:watch`.

- Mock the `services` module for API calls and `react-i18next` for translations.
- `vitest.setup.ts` polyfills `MessageChannel`, `ResizeObserver`, `matchMedia`, and `getComputedStyle` for jsdom. The
  `MessageChannel` polyfill delivers messages via `setTimeout(0)` — required for React's scheduler to commit
  async-scheduled state updates.
- **"Current testing environment is not configured to support act(...)"**: this fires when an async callback chain
  schedules a `setState` after an outer `act(async () => { await userEvent.click(...) })` wrapper has already closed.
  Fix by **removing** the outer `act`. `userEvent` already wraps clicks in act, and the following `waitFor(...)` retries
  each run inside their own act scope, catching detached promise-tail updates:

    ```tsx
    // ❌ outer act closes before the async onFinish chain reaches setState
    await act(async () => { await userEvent.click(screen.getByText('Save')) })
    await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(...))

    // ✅ userEvent's internal act covers the sync part; waitFor covers the async tail
    await userEvent.click(screen.getByText('Save'))
    await waitFor(() => expect(mockApiCall).toHaveBeenCalledWith(...))
    ```

  Wrapping the initial `render()` in `await act(async () => { render(...) })` is still fine — that flushes mount-time
  effects before the first assertion. Only `userEvent` interactions should not be wrapped.
- **Ant Design Modal in jsdom**: Modal uses CSS animations (`ant-zoom-appear`) that block synchronous rendering of body
  content. Wrap the initial `render()` in `await act(async () => { ... })` to flush async effects (e.g., API loads in
  `useEffect`), or use `waitFor` for content to appear.
- **Ant Design `Table` causes infinite `act()` loops in jsdom**: components that render `Table`, `Descriptions`, or
  other heavy AntD components with async `useEffect` data loading hang during `act()`. Mock `antd` entirely with simple
  HTML equivalents (`<table>`, `<dl>`, `<button>`, …) and flush async effects via
  `await act(async () => { render(...); await new Promise(r => setTimeout(r, 50)) })`. See
  `ConflictResolutionStep.test.tsx`.
- **Per-test store overrides**: use `vi.spyOn(storeModule, 'useUserStore').mockReturnValue(...)` with `mockRestore()` in
  a `finally` block. Never mutate module exports directly — if the test throws before restoration, leaked state breaks
  subsequent tests.
- **No hardcoded Ant Design default labels** (e.g. "OK", "Cancel") in assertions — they depend on AntD locale config.
  Select buttons by excluding known buttons (save, close) or by setting explicit `okText`/`cancelText`.
- **Mock child components** to capture props via `vi.fn()` when testing a parent orchestrator (e.g. `MergeModal`). Use a
  `getLatestProps` helper that reads the last mock call — earlier calls may have stale closures after re-renders.
- **Stable `react-i18next` mock**: define the `t` function once inside the `vi.mock` factory, not inline in the return.
  A new `t` reference per render causes infinite `useCallback`/`useEffect` loops when `t` is in a dependency array.
- **Do not spy on or mock `console.*`.** `vitest-fail-on-console` is wired into `vitest.setup.ts` and fails any
  `console.error`. Fix the source instead — remove redundant logging or demote to `console.warn` (silenced globally:
  `silenceMessage` drops every `methodName === 'warn'`, so warns never fail and never print). Per-test
  `vi.spyOn(console, 'error').mockImplementation(...)` is forbidden — it hides regressions and conflicts with
  `vitest-fail-on-console`'s per-test re-wrap.
- **Do not edit `failOnConsole` options in `vitest.setup.ts`.** Don't add new `silenceMessage` patterns or `skipTest`
  entries to make a failing test pass. Existing silences are reserved for jsdom/framework limitations with no
  source-level fix (e.g. `Not implemented: navigation`, rc-form's orphan `useForm` warning, jsdom CSSOM parse failures,
  AntD deprecation warnings). Other noise must be fixed at the source.

## Quality Rules

- Use the `apiCall` wrapper, never raw `fetch`.
- Guard screens with `PermissionContext` and `SystemContext` flags.
- Add translations from day one — no hardcoded user-facing strings.
- Prefer Zustand selectors over full-store subscriptions.
- Unsubscribe WebSocket listeners or use `cleanupWebSocket` to prevent duplicates.
- **Do not call `console.error` from components, hooks, or services.** Propagate errors via callbacks, thrown
  exceptions, or explicit state so callers (error boundaries, `notification.error`, future global logger) decide how to
  surface them. `console.warn` is allowed for transient recoverable signals (reconnect attempts, queued work,
  disconnected sends) — `vitest-fail-on-console` silences every warn, so warns are harmless in CI but still visible in
  the browser dev console.
- Use the current Ant Design API — avoid deprecated props:
    - `Spin`: `description` instead of `tip`.
    - `Modal`: `destroyOnHidden` instead of `destroyOnClose`; `mask={{ closable }}` instead of `maskClosable`.
    - `Space`: `orientation` instead of `direction`.
    - `Typography.Text`: `ellipsis={{ tooltip: text }}` for conditional truncation tooltips (only shows on overflow).
- **Never reach into a library's internals.** Import only from a package's public surface — its main entry (e.g. `antd`,
  `react`) or its documented subpath exports declared in the package's `exports`/`typesVersions` map (e.g.
  `antd/es/select` for `DefaultOptionType`). Forbidden:
    - **Transitive dependencies** not listed in `package.json` (e.g. `@rc-component/form/lib/interface`,
      `rc-util/...`) — implementation details of a direct dep, may disappear on a minor upgrade.
    - **Undocumented deep paths** of any package — `<pkg>/src/...`, `<pkg>/internal/...`, `<pkg>/lib/<file>` not
      surfaced via the package's exports map, files inside `node_modules` reached by hand-written paths, etc.

  If a needed type/value is not re-exported, prefer the public alias (e.g. AntD re-exports `Rule` as `FormRule`); if no
  public export exists, derive a local type — never reach into internals.
