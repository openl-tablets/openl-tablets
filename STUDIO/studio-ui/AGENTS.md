# studio-ui — React/TypeScript Frontend

Draws every screen of OpenL Studio: the editor, the projects, administration, user and group management, repository settings and notifications.

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
- **ESLint** (flat config: `eslint.config.js`)
- **antd-style** for CSS-in-JS (`createStyles`, `createGlobalStyle`) — no SCSS/CSS files
- **Vitest** + React Testing Library (`jsdom` environment)

## Project Structure

```text
src/
├── App.tsx              # Root: error boundary, auth gate, router mount
├── index.tsx            # Entry: inits i18n, mounts App
├── components/          # Reusable widgets (accessManagement/, form/, modal/, schemaForm/, values/, shared)
├── containers/          # Feature screens (System, Security, Users, Groups, Tags, Repositories, Trace, execution, Merge…)
├── contexts/            # PermissionContext, SystemContext, GroupsContext
├── providers/           # AppThemeProvider (light/dark appearance), SecurityProvider (SystemContext + PermissionContext)
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

The build writes two pages (`build.rollupOptions.input`):

1. `index.html` → an inline script paints `color-scheme` from the remembered appearance (`openl.theme.mode`,
   mirrored by a test against `THEME_MODE_KEY`) so a dark reader sees no white flash, then `index.tsx`
   initializes i18n and mounts `App` into `#appRoot`.
2. `App` fetches the user profile, blocks rendering until auth completes, then mounts the router inside
   `AppThemeProvider` and Ant Design's `App` provider (with `PopupsBridge`, see Quality Rules), and initializes
   WebSocket notifications.
3. `api-docs.html` → `api-docs.tsx` mounts `ApiDocs` alone. The REST API documentation is read without logging in,
   so it carries no shell, no router and no auth bootstrap; the server answers `/api-docs` with this page and
   redirects the former `/rest/api-docs` to it.

## Key Patterns

- **REST**: always use `services/apiCall.ts` — it prepends `CONFIG.CONTEXT`, handles JSON/text, surfaces validation
  errors, and updates `useAppStore` flags for 401/403/404/500.
- **Execution results**: a screen that follows a run, a test run or a benchmark over the socket reads the result
  once while it goes on (`get*` in `services/execution.ts`, answered `202` until the end). It retries a `202`
  (`read*`) only after the status says the execution ended. A screen that follows a run or a test run also reads
  again after every quiet spell of `useQuietSpells` while it waits: an end the socket drops, just as the screen
  subscribes or while it reconnects, reaches nobody. An execution that has said it ended counts no spell.
- **State**: Zustand stores in `src/store/`. Use selectors that subscribe to specific slices to avoid re-renders.
- **Routing**: `createBrowserRouter` with `CONFIG.CONTEXT` as basename. Admin features under `administration/`
  with `AdministrationLayout`.
- **i18n**: bundles in `src/locales/*.en.ts`, registered as namespaces. Reference keys like `t('common:menu.users')` or
  `t('system:tabs.repositories')`. A key that no bundle defines renders as the key itself, and component tests mock `t`
  so they cannot notice — `src/locales/lookups.test.ts` resolves every literal `t('…')` and `i18nKey` in the sources
  against the bundles and fails on the first miss.
- **Permissions**: `SecurityProvider` derives system flags from the backend. Use `PermissionContext` and `SystemContext`
  to gate features (e.g., `isUserManagementEnabled`, `isExternalAuthSystem`).
- **Forms**: `components/form` wraps Ant Design inputs. `hooks/useIsFormChanged.ts` drives dirty-state detection.
- **Styling**: CSS-in-JS via `antd-style`. Component styles live in co-located `*.styles.ts` files using
  `createStyles(({ css }) => ({ ... }))`; consume with `const { styles, cx } = useStyles()` and apply via
  `className={styles.foo}`. Global styles use `createGlobalStyle` (see `src/App.styles.ts`, mounted as
  `<AppStyles />` inside `<AntApp>`). Prefer component-level scoped styles over global overrides.
- **Appearance**: `AppThemeProvider` (antd-style `ThemeProvider`) gives the application the light or dark appearance
  the user picked in the header's `ThemeSwitch`, or the one the operating system asks for. The choice lives in
  `localStorage` under `openl.theme.mode` (`utils/themeMode.ts`), defaulting to `auto`. Read the appearance with
  `useThemeMode()` from antd-style, or `isDarkMode` inside `createStyles`. The provider passes
  `defaultAppearance={appearanceOf(themeMode)}`, because antd-style starts every appearance as light and
  switches in an effect — without it a dark reader sees a white frame on every load. `App` mounts the provider
  and `AppStyles` before the auth gate, so the surface is painted while the profile is still loading. A
  third-party widget with a theme of
  its own (CodeMirror in `CodeEditor`) is handed `isDarkMode` too. The provider also keeps the `theme-color`
  meta on the surface colour in force, and `AppStyles` sits directly under it, so the loading fallback is
  themed as well.
- **Theme**: the same switcher picks which palette the colours come from — `THEMES` in `styles/listPageTheme.ts`,
  remembered under `openl.theme.name` and defaulting to `standard`. A theme supplies a whole `Palette` per
  appearance; `paletteOf(name, isDarkMode)` resolves the one in force, `appTheme(palette)` turns it into the
  application-wide Ant Design tokens, and `AppStyles` republishes it as the `--openl-*` custom properties. Adding a
  theme means adding one entry to `THEMES` (both appearances, every key) plus its name in `common.en.ts`.
  The palette in force also travels as the **`openl` custom token** (`styles/customToken.ts`), so a style that
  needs a real colour reads `token.openl.…` inside `createStyles` instead of importing a palette.
  A theme scoped to one area (`ProjectsThemeProvider`) nests another antd-style `ThemeProvider` and passes the
  appearance through. A bare Ant Design `ConfigProvider` is not enough: `createStyles` takes its token from the
  nearest **antd-style** provider, so a `ConfigProvider` would restyle the Ant Design components and leave the
  co-located styles on the application-wide token.
- **Density**: the same `ThemeSwitch` offers the compact density, remembered in `localStorage` under
  `openl.theme.compact`. It is an Ant Design *algorithm*, not a set of tokens, so it is added through
  `densityTheme(compact)` from `AppThemeProvider` and read with `useAppTheme()`. antd-style builds the algorithm
  chain as `[appearance, ...theme.algorithm]`, and a nested provider starts that chain again — so **every scoped
  theme merges `densityTheme(compact)` in**, or its area stays comfortable inside a compact application.

## Development

```bash
npm install                    # Install dependencies
npm run start                  # Dev server (proxied to backend)
npm run build                  # Production build with license check
npm run serve                  # Serve built bundle locally
npm run lint                   # ESLint
npm run typecheck              # tsc --noEmit
```

Docker dev: set `_REACT_UI_ROOT_: http://localhost:3100` in root `compose.override.yaml` under the `studio` service.

Maven: `mvn clean install` runs `npm install` + `npm run typecheck` + `npm run build` + `npm run test` via
`frontend-maven-plugin`. Bundled UI is published relative to `CONFIG.CONTEXT`. Each step after the install has a
switch of its own — `-Dnpm.test.skip`, `-Dnpm.typecheck.skip`, `-Dnpm.build.skip` — for a build that needs only
some of them.

## Testing

Tests are co-located with sources (e.g. `src/containers/DeployModal.test.tsx` next to `DeployModal.tsx`). Run all with
`npm test` (`vitest run --coverage`); watch mode with `npm run test:watch`.

- Mock the `services` module for API calls and `react-i18next` for translations.
- Vitest clears every mock's call history before each test. Put calls that a test asserts in the test body or its
  `beforeEach`; calls made at module scope, in a setup file, or in `beforeAll` are cleared before the assertion runs.
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

## Code Coverage

```bash
npm test   # runs vitest run --coverage; prints a text-summary
```

Report: `coverage/lcov.info`. A line is uncovered when `DA:<line>,0`.

## Quality Rules

- Use the `apiCall` wrapper, never raw `fetch`.
- Guard screens with `PermissionContext` and `SystemContext` flags.
- Add translations from day one — no hardcoded user-facing strings.
- **Colours follow the appearance.** Never hardcode a colour in a style — take an Ant Design token
  (`createStyles(({ token }) => ...)`), or, for an OpenL hue with no token, `LIST_PAGE_COLORS` from
  `styles/listPageTheme.ts`. Its values are `var(--openl-*)` custom properties that `AppStyles` republishes when the
  appearance changes, so a style that uses them repaints with the theme. A new colour is added to **both**
  `LIGHT_PALETTE` and `DARK_PALETTE`. Besides the surfaces and the text, the palette carries the compilation
  states (`COMPILE_COLORS`), the fills of the solid status badges, and the syntax hues of a parameter value.
  Ant Design derives whole palettes from a colour and cannot read a custom property, so a `ThemeConfig` token takes
  the palette itself — see `projectsTheme(isDarkMode)`.
  Ant Design's **static** `notification`/`message`/`Modal.confirm` calls render outside React and stay light on a
  dark page, so ESLint forbids them. A component or a hook takes `notification` and `modal` from
  `App.useApp()` — the instances of the application's `<AntApp>`, which sits inside the theme provider. Only a
  module that cannot call a hook — a service or a store — imports them from `services/popups`, a bridge that
  `<PopupsBridge />` points at the same instances and that falls back to the static ones before the
  application mounts. Outside `<AntApp>`, `App.useApp()` answers with empty objects, so a test of a component
  that pops something up either renders it inside `<AntApp>` or mocks `antd` with
  `App: { useApp: () => ({ notification, modal }) }` (see `staticAntdApp` in `src/testing/`).
    - **The logo is part of the palette.** `components/Logo.tsx` draws the cube from `primary`, `brand` and
      `primaryFg`, so it turns with the theme and the appearance; it carries no colour of its own.
    - **A canvas needs a real colour.** Cytoscape paints the table dependency graph on a `<canvas>`, which cannot
      read a custom property, so the graph takes its colours from the Ant Design token through
      `containers/tableGraphTheme.ts` (`kindColor`, `kindRules`, `graphPalette`) and lists `token` among the
      dependencies of the effect that builds the instance, so it is rebuilt when the appearance changes.
    - **A shell-less screen wears the shared card.** The login page, the `403`/`404`/`500` pages and the e-mail
      verification screen take `styles/splashCard.styles.ts` rather than repeating a card of their own.
    - **A class component reads the theme through a child.** `createStyles` and `theme.useToken()` are hooks, so a
      class such as `ErrorBoundary` keeps its styled part in a small function component beside it.
- **Form field labels** use the shared `FieldRow` component (right-aligned `Label :` with the required
  asterisk to the left), matching the create-project modal and the administration screens.
- **Label casing.** A label of **at most three words** (not counting the articles `a`/`an`/`the`) is written in
  Title Case — capitalise every word except `a`/`an`/`the` when it is not first (e.g. `Project Name`, `Service
  Class`, `Provide Runtime Context`). Longer labels stay in sentence case (e.g. `Path for Module with Data
  Types`). Never append `(optional)` to a label — mark required fields instead.
- **Animate only `transform` and `opacity`.** Those two the compositor moves on its own; everything else —
  `box-shadow`, `background`, `color`, `width`, `top` — is painted by the page, so an animation of them makes
  the page recalculate style and repaint on **every frame**, for as long as it runs. While a reader is
  scrolling, that is what takes the content away and leaves the screen blank behind the scroll: the compositor
  can no longer scroll by itself. A measured example: the compiling indicator beating with a `box-shadow` cost
  346 style recalculations and 610 paints over a four-second scroll (the GPU process pegged); the same beat as
  a ring moved by `transform`/`opacity` cost 6 paints. Animate a pseudo-element rather than the element itself,
  add `will-change: transform, opacity`, and silence it under `@media (prefers-reduced-motion: reduce)`.
  The same rule applies to motion a component brings with it: Ant Design's `Tree` slides a folder open on a
  `height` animation, which the page paints frame by frame, so a tree of any size is given `motion={false}`
  and opens at once — measured at 64 repaints over 350 ms against 9.
- **A long list is virtualised.** A tree or table that can hold hundreds of rows is given a height and drawn a
  screenful at a time (Ant Design's `Tree` takes `height`, `itemHeight` and `scrollWidth`); and a table is laid
  out at the width its values need rather than squeezed into the screen, which otherwise wraps every value into
  a tower of lines and multiplies the pixels the browser has to paint.
- **A cell of a long list reads no styles of its own.** antd-style's `useStyles()` copies the whole theme for
  every component that calls it, about 40 KB each: a table of test results with a hook in every cell ran the
  browser out of memory. The list reads the styles once and hands them to its cells — `useValueStyles()` and
  `ValueCell` show how — and a cell draws a plain value without a hook at all.
- **A tree is built as far as it is opened.** Ant Design's `Tree` walks every node of its `treeData` each time
  it is drawn, open or closed: a whole test value of 1.77 million nodes ran the browser out of memory before its
  first line was read. A node is given its children only once the reader opens it, and lists them a step at a
  time with a line that lists more — `buildValueTreeData` takes how far the reader has gone (`ValueTreeReach`),
  and `ValueTree` in `components/values/ParameterValues.tsx` keeps it.
- **Names in titles.** When a form or dialog title includes the name of a concrete thing (a project, file,
  user, repository…), wrap that name in double quotes — e.g. `Copy project "{{name}}"`, `Revoke access for
  "{{subject}}"?`.
- Prefer Zustand selectors over full-store subscriptions.
- Unsubscribe WebSocket listeners or use `cleanupWebSocket` to prevent duplicates.
- Use `data-testid` for elements that tests need to target. Generated CSS-in-JS class names are unstable, and
  selecting by structure (descendant chains, `nth-child`) couples tests to layout — both break on refactors. Playwright
  resolves it via `page.getByTestId('foo')`; React Testing Library uses `getByTestId('foo')`. Choose stable,
  semantic ids (`repositories-tabs`, not `tabs1`); never reuse CSS class names as test ids.
- **Do not call `console.*` directly from components, hooks, or services — route errors, never swallow them.**
  ESLint enforces this (`no-console`); `utils/errorHandling.ts` is the only allowed `console.error` call site. Pick
  the sink by audience:
    - **User-actionable errors** — surface in the UI: `notification.error`, form field errors, error boundaries.
    - **Background or diagnostic errors** (and details too technical for the UI) — `errorHandler.logError()` from
      `utils/errorHandling.ts`. It attaches context (url, user agent, timestamp), keeps the last 100 errors in memory
      for support, and prints to the browser console outside tests, so production failures stay diagnosable.

  `console.warn` is the one direct call allowed — for transient recoverable signals (reconnect attempts, queued work,
  disconnected sends). `vitest-fail-on-console` silences every warn, so warns are harmless in CI but still visible in
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
