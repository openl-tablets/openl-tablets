# studio-ui — React/TypeScript Frontend

Draws every screen of OpenL Studio: the editor, the projects, administration, user and group management, repository settings and notifications.

## Tech Stack

Always get list of current version of libraries from `package.json` — do not hardcode them here.
Use almost the latest versions when possible.

- **React** + functional components and hooks (TypeScript strict mode)
- **TypeScript** side by side, a workaround to remove once TypeScript 7.1 ships its API and typescript-eslint supports
  it ([typescript-eslint#10940](https://github.com/typescript-eslint/typescript-eslint/issues/10940)):
    - `@typescript/native` is TypeScript 7, whose native `tsc` runs `npm run typecheck`.
    - `typescript` is TypeScript 6 (`@typescript/typescript6`), whose API typescript-eslint parses with. It carries
      no `lib/tsserver.js`, so an editor set to the workspace version falls back to its own TypeScript.
    - The removal: depend on `typescript` 7.1 alone, and drop the `//` note of `package.json` and the comment of
      `eslint.config.js`.
- **Ant Design** for UI components, **`@ant-design/icons`** for icons
- **Zustand** for state management (`appStore`, `userStore`, `notificationStore`, `traceStore`)
- **React Router** with `createBrowserRouter`, scoped to backend context path
- **i18next + react-i18next** for internationalization
- **@stomp/stompjs** for WebSocket notifications (reconnecting singleton)
- **Vite** + `@vitejs/plugin-react` (`vite.config.ts`)
- **ESLint** (flat config: `eslint.config.js`) — formatting through `@stylistic`, the Rules of Hooks as errors and
  missing hook dependencies as warnings (`eslint-plugin-react-hooks`), JSX props sorted by `perfectionist`
- **antd-style** for CSS-in-JS (`createStyles`, `createGlobalStyle`) — no SCSS/CSS files
- **Vitest** + React Testing Library (`jsdom` environment)
- **CodeMirror** through **`@uiw/react-codemirror`** for the code editor (`CodeEditor`), with the colour schemes of
  the **`@uiw/codemirror-theme-*`** packages
- **react-markdown** with remark/rehype plugins, the CodeMirror grammars through **`@lezer/highlight`** and **Mermaid**
  draw the user guides (`containers/userGuides`)

## Project Structure

```text
src/
├── App.tsx              # Root: error boundary, auth gate, router mount
├── index.tsx            # Entry: inits i18n, mounts App
├── components/          # Reusable widgets (accessManagement/, form/, modal/, schemaForm/, values/, shared)
├── containers/          # Feature screens (System, Security, Users, Groups, Tags, Repositories, Trace, execution, Merge…)
├── contexts/            # PermissionContext, SystemContext, GroupsContext
├── providers/           # AppThemeProvider (appearance, theme, density), SecurityProvider (System/PermissionContext)
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

- **REST**: always use `services/apiCall.ts` — it prepends `CONFIG.API_ROOT` (the deployment context path plus
  `/rest`, the one prefix the REST API is served on), handles JSON/text, surfaces validation errors, and updates
  `useAppStore` flags for 401/403/404/500. A call that needs a raw URL instead — a browser download, a viewer
  source — builds it from `CONFIG.API_ROOT` too, never from a literal.
- **Sign-in**: a 401 sets `showLogin`, and `App` reloads the page, the context root included. Only the server knows
  the sign-in it is configured for — the login form or an identity provider (SAML, OAuth2) — and it brings the user
  back to the page afterwards. Opening `/login` directly would not: under SSO the server guards `/login` too, and
  the identity provider would return the user to the login form. A page that comes back from the reload still
  signed out opens the login page instead: whatever served it guards no page, as the Vite dev server does not. The
  tab remembers the reloaded page in `sessionStorage` until the login page is opened for it or the user signs in,
  so a session lost after a sign-in is reloaded through the server again.
- **WebSocket**: `services/websocket.ts` connects to `${CONTEXT}/ws`, built from `document.baseURI`. The handshake has
  an address of its own, so it is not under `CONFIG.API_ROOT`.
- **User guides**: `/docs/*` opens `containers/userGuides`, loaded as a lazy chunk with everything it draws the guides
  with; Mermaid is a chunk of its own, loaded with the first diagram, and so are the code grammars and the colour
  schemes of the code editor, shared with it and loaded with the first code block (`HighlightedCode`). The guides are
  files rather than REST, so `services/userGuides.ts` reads them with `fetch` from `${CONFIG.CONTEXT}/docs` instead of
  `apiCall`. The syntax a page may use is set by the validator of `STUDIO/studio-docs`, see
  `Docs/architecture/embedded-user-guides.md`. The search indexes the pages in a module worker
  (`guideSearch.worker.ts`), and on the page itself where no worker starts — under `_REACT_UI_ROOT_` the scripts come
  from another origin than the page, so the fallback is what a developer sees. A worker has no DOM: code it imports must
  not touch `document`, which `vite.config.ts` enforces for the one package that does (`domlessEntityDecoder`), and
  the pages take that build too (see **About**). A bare `#heading` link resolves against `<base href>`, the root of
  the application, not against the page: a link to a heading — in a page or in its outline — goes through the router
  with the address of the page.
- **About**: the **About** item of the user menu opens `containers/header/AboutModal`, a lazy chunk loaded the first
  time About is chosen. It shows the version, the build date (`openl.build.date`, in the UI language) and the license
  of OpenL Studio — LGPL v3, a link to its text on gnu.org — then reads the lists of third-party libraries once
  (`services/licenses.ts`). `build.license` in `vite.config.ts` writes the frontend list,
  `dist/licenses/frontend-licenses.json` — the libraries the pages bundle, each with its license text. It skips a
  library only a worker bundles, so a worker must not bundle one the pages do not. The war build writes the backend
  list beside it, see `STUDIO/AGENTS.md`. A side of the dialog draws its libraries only while it is expanded; both
  sides scroll in the one body of the dialog, which keeps to the window, under the title of the side. A license is
  an SPDX expression. The license text a library ships opens in a new window as plain text (`openText`); a library
  shipping none links each standard license of the expression to its public text (`PUBLIC_LICENSES`), or, naming no
  standard license, to the `url` of the list.
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
- **Formatting numbers and dates**: a number or a date on a screen is written in the **UI language**, never in the
  locale of the machine the browser runs on. Take the language from `useTranslation()` — `const locale =
  i18n.resolvedLanguage ?? i18n.language` — and pass it to `new Intl.NumberFormat(locale, …)` or
  `new Intl.DateTimeFormat(locale, …)`; see `formatSize` in `containers/projects/FileTree.tsx` and the formatters
  in `containers/execution/benchmarkMetrics.ts`. Never call `toLocaleString()`, `toLocaleDateString()` or
  `new Intl.*(undefined, …)`: those follow the reader's operating system, so an English screen shows `6,84` and
  `2 284` on a Russian or German machine, and every test that reads such a number passes on CI and fails on that
  machine. A formatter kept out of a component takes the locale as an argument rather than reading it itself.
  A test mocks `react-i18next` with the language alongside `t`:
  `useTranslation: () => ({ i18n: { language: 'en', resolvedLanguage: 'en' }, t })`.
  What the machine is asked for on purpose is what the *reader* supplies rather than reads:
  `Intl.DateTimeFormat().resolvedOptions().timeZone` in `services/repositories.ts`, because the backend wants the
  reader's own zone, and `datePickerFormatForLocale()` called without a locale in `PropertyValueInput` and
  `TypedTableValueInput`, because a date is typed into those pickers in the pattern the reader is used to. Both
  are deliberate; anything else that follows the machine is a defect.
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
  third-party widget with a theme of its own (CodeMirror in `CodeEditor`) is handed the theme and the appearance
  too (`editorTheme`). The provider also keeps the `theme-color`
  meta on the surface colour in force, and `AppStyles` sits directly under it, so the loading fallback is
  themed as well.
- **Theme**: the same switcher picks the theme — `THEMES` in `styles/themes.ts`, remembered under
  `openl.theme.name` and defaulting to `standard`, which is Ant Design's own. Every other theme is the colour scheme
  of a well-known code editor, in a light and a dark variant that the appearance picks between; a pair of schemes
  such as Dracula and Alucard counts as one theme. A variant names the editor's background, text and accent
  (`EditorColors`) and the hues it writes a value in (`SyntaxHues`). `appTheme(name, isDarkMode)` turns them into
  the application-wide Ant Design tokens: the editor's background becomes the background of every container, and
  the page, the borders and the raised surfaces are mixed from it, because Ant Design's own derivation turns a
  tinted background grey. The provider hands the token it works out to its `customToken`, where
  `paletteOf(token, name, isDarkMode)` reads the `Palette` off it, and `AppStyles` republishes it as the `--openl-*`
  custom properties. The CodeMirror schemes themselves live in `styles/codeMirrorThemes.ts` (`SCHEMES`: the theme
  each package builds, with Alucard and the Gruvbox Light styles drawn there, since no package ships them): they
  bring CodeMirror along, so they load only with the first editor or code sample, and
  `THEMES` keeps copies of the colours the screens need before that — `codeMirrorThemes.test.ts` and
  `codeHighlight.test.ts` keep the copies in step. Adding a theme means an entry in `THEMES` and in `SCHEMES`
  (both appearances) plus its name in `common.en.ts`; `listPageTheme.test.ts` holds every palette to 4.5:1 for text
  and 3:1 for links and primary buttons. A test draws in a theme through `renderInTheme` and reads a palette
  through `paletteFor` (`src/testing/theme.tsx`). The code samples of the guides take the theme's highlight style
  as inline colours (`codeHighlight.ts`), so they look as the same code does in the editor, and every block wears
  the editor's background and text (`codeBlockColors`) from the start, before the grammars load.
  The palette in force also travels as the **`openl` custom token** (`styles/customToken.ts`), so a style that
  needs a real colour reads `token.openl.…` inside `createStyles` instead of importing a palette. A module that
  only refers to the colours calls on no Ant Design while it loads — `PALETTE_KEYS` names them up front, and the
  palette is read off the token the provider works out — because many tests mock `antd` whole. A test whose screen
  asks Ant Design for a colour itself keeps the real `theme` export under such a mock (`vi.importActual`): the theme
  switch, which shows Ant Design's own accent beside the standard theme.
  A table of a workbook does too, since its colours come from `paperToken()`.
  A theme scoped to one area (`ProjectsThemeProvider`) nests another antd-style `ThemeProvider` and passes the
  appearance through. Ant Design lays a nested theme's token over the parent's, so the scoped theme inherits the
  colours of the application and adds only its own shape, drawn in the palette the application's provider carries
  (`useTheme().openl`). A bare Ant Design `ConfigProvider` is not enough: `createStyles` takes its token from the
  nearest **antd-style** provider, so a `ConfigProvider` would restyle the Ant Design components and leave the
  co-located styles on the application-wide token.
- **Density**: the same `ThemeSwitch` offers the compact density, remembered in `localStorage` under
  `openl.theme.compact`. It is an Ant Design *algorithm*, not a set of tokens, so it is added through
  `densityTheme(compact)` from `AppThemeProvider` and read with `useAppTheme()`. antd-style builds the algorithm
  chain as `[appearance, ...theme.algorithm]`, and a nested provider starts that chain again — so **every scoped
  theme merges `densityTheme(compact)` in**, or its area stays comfortable inside a compact application.
- **Settings live in the profile.** Every setting of **My Settings** is a field of the user profile (`GET` and
  `PUT /users/profile`, `UserProfile` in `types/user.d.ts`), kept on the server, so it follows the user from browser
  to browser and no user reads another's. `localStorage` keeps only what the header picks for the browser — the
  appearance, the theme and the density — and the state of a screen, such as the width of a panel or a filter. A new
  setting is a field of the profile, never a key of the browser.

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

  Do not wrap `render()` or `fireEvent` in `act` either — RTL already runs them inside `act` (Sonar
  `typescript:S8980`). When the first assertion needs what a mount-time effect loads, wait for it with
  `await screen.findBy…(...)` or `await waitFor(...)`. Wait on something the load changes — the loaded content, or a
  loading placeholder going away: a `waitFor` that passes at once (on a mock called at mount) returns before React
  draws the answer, so a negative assertion after it proves nothing. `act` stays for what the test itself does outside
  React — resolving a deferred answer it handed a mock, or calling a callback it captured.
- **Ant Design Modal in jsdom**: Modal uses CSS animations (`ant-zoom-appear`) that block synchronous rendering of body
  content. Wait for the content with `await screen.findBy…(...)` or `await waitFor(...)`, which also covers async
  effects such as API loads in `useEffect`.
- **Ant Design `Table` causes infinite `act()` loops in jsdom**: components that render `Table`, `Descriptions`, or
  other heavy AntD components with async `useEffect` data loading hang during `act()`. Mock `antd` entirely with simple
  HTML equivalents (`<table>`, `<dl>`, `<button>`, …) and wait for the loading placeholder to go —
  `await waitFor(() => expect(screen.queryByTestId('spin')).not.toBeInTheDocument())`. See
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
- **Do not spy on or mock `console.*`.** `vitest-fail-on-console` is wired into `vitest.setup.ts` and fails a test on
  any `console.error` or `console.warn`. Fix the source instead — remove redundant logging, or send a diagnostic worth
  keeping to `errorHandler.logError()` (`utils/errorHandling.ts`), which prints nothing in tests. Per-test
  `vi.spyOn(console, 'error').mockImplementation(...)` is forbidden — it hides regressions and conflicts with
  `vitest-fail-on-console`'s per-test re-wrap.
- **Keep `failOnConsole()` in `vitest.setup.ts` without options.** Don't add `silenceMessage` patterns, `skipTest`
  entries or `shouldFailOn*` switches to make a failing test pass — fix the noise at the source, as was done for
  rc-form's orphan `useForm` warning and the AntD deprecation warnings. Errors that jsdom raises itself (e.g.
  `Not implemented: navigation to another Document`, CSS it cannot parse) bypass `vitest-fail-on-console` — they print
  but never fail a test, so keep them out of the output: a test that reaches `location.reload()` stubs it with
  `vi.stubGlobal('location', { ...window.location, reload })`.

## Code Coverage

```bash
npm test   # runs vitest run --coverage; prints a text-summary
```

Report: `coverage/lcov.info`. A line is uncovered when `DA:<line>,0`.

## Quality Rules

- Use the `apiCall` wrapper, never raw `fetch` — files rather than REST are the exceptions: the user guides served at
  `/docs` and the lists of third-party libraries served at `/licenses`.
- Guard screens with `PermissionContext` and `SystemContext` flags.
- Add translations from day one — no hardcoded user-facing strings.
- **Colours follow the appearance.** Never hardcode a colour in a style — take an Ant Design token
  (`createStyles(({ token }) => ...)`), or, for an OpenL hue with no token, `LIST_PAGE_COLORS` from
  `styles/listPageTheme.ts`. Its values are `var(--openl-*)` custom properties that `AppStyles` republishes when the
  theme or the appearance changes, so a style that uses them repaints with the theme. A new colour is named in
  `PALETTE_KEYS` and read off the Ant Design token in `paletteOf`, so every theme and appearance has it. Besides
  the surfaces and the text, the palette carries the hues of the states, which the compilation states take
  (`COMPILE_COLORS`), the fills of the solid status badges, and the syntax hues of a parameter value. Ant Design
  derives whole palettes from a colour and cannot read a custom property, so a `ThemeConfig` token takes the
  palette itself — see `projectsTheme(palette)`.
  A memoised piece of JSX that uses `styles` or the token is rebuilt when the theme or the appearance changes:
  its dependencies name `themeName` from `useAppTheme()` and `isDarkMode` from `useThemeMode()`, or it keeps the
  colours of the theme it was first drawn in (`treeData` in `ProjectsTree`). They do not name `styles` or the token
  themselves — antd-style hands out new ones on every render, which would rebuild it every time.
  Ant Design's **static** `notification`/`message`/`Modal.confirm` calls render outside React and stay light on a
  dark page, so ESLint forbids them. A component or a hook takes `notification` and `modal` from
  `App.useApp()` — the instances of the application's `<AntApp>`, which sits inside the theme provider. Only a
  module that cannot call a hook — a service or a store — imports them from `services/popups`, a bridge that
  `<PopupsBridge />` points at the same instances and that falls back to the static ones before the
  application mounts. Outside `<AntApp>`, `App.useApp()` answers with empty objects, so a test of a component
  that pops something up either renders it inside `<AntApp>` or mocks `antd` with
  `App: { useApp: () => ({ notification, modal }) }` (see `staticAntdApp` in `src/testing/`).
    - **A workbook's table keeps the colours of Excel.** `RawTableGrid` writes its cells in black on white
      whatever the theme, because an author fills a cell for that paper: the default text of a cell filled in
      cyan reads only in black. Everything drawn on the cells — the paper and the ink, the rules, the links, and
      every mark a screen lays on a cell (the picked and changed cells, the trace highlights and their legend)
      — takes `paperToken()` (`styles/paper.ts`, Ant Design's own light token) rather than the theme's token.
      The table editor draws its grid inside `PaperTheme`, a `ConfigProvider` that drops the theme in force
      (`inherit: false`) and keeps the density, so the Ant Design controls written into a cell, and the notes over
      one, lie on the paper as well. The line numbers beside the table belong to the screen and follow the theme.
      The one exception is the look a theme of the application gives the tables (`TABLE_LOOKS` in
      `styles/tableColours.ts`: a table theme, and the token each key of its file takes in the dark appearance), where
      the user asked for **Override with Studio theme** (My Settings, `overrideWithStudioTheme` of the profile, view
      only like the table theme of the settings: neither changes a workbook or a project). The table theme then decides
      every colour of the cells, so no colour of an author is left to need the paper. `ModuleWorkspace` reads the
      table with the table theme of the look (`followedTableTheme`: `standard` under the Standard theme) whatever
      table theme the settings name, which stays the fallback under a theme with no look, and tells `TableEditor` the
      `look` it reads the table with. The server reports the key of the theme file each colour is set at
      (`backgroundKey`, `colorKey`, such as `spreadsheet.values.background`) in the style of each cell the table theme
      draws, which names `theme` as its `source` (`inTheme`). `TableEditor` draws those cells as the look
      recolours them by their keys (`inLook`), on the paper of the look (`TablePaper`), so `RawTableGrid` knows no look:
      it lays its cells on the paper it is given, the paper of a workbook (`workbookPaper`) by default. The colours of
      the look (`tableColoursOf`) are the ground and the ink of
      its base style, the grid of the theme, and a solid colour of the token for each key. A look is written as the
      table theme file nests its keys, and shares a part where the file repeats one by an alias (`smartRules: SIMPLE`
      as `smartRules: *simple`); a colour of a key the look does not colour keeps the colour of the table theme. Only
      the Standard theme has a look today, in the colours of the table theme in the light appearance. A theme joins
      with a `TableLook` of its own: `tableColours.test.ts` reads the file of its table theme to hold an appearance it
      colours to every key the file sets a colour at, and every text of it readable on every fill. A table being edited
      and a table no theme styles keep the paper. A table read with a table theme is read again without it when the
      user edits it (`TableEditor`), so the edit starts from the style the workbook holds.
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
- **A component drawn many times over reads no styles of its own.** antd-style's `useStyles()` copies the whole
  theme for every component that calls it, about 40 KB each: a table of test results with a hook in every cell
  ran the browser out of memory. Whatever draws the many — a list, a table, a tree — reads the styles once and
  hands them down as a prop, typed off the hook (`ReturnType<typeof useStyles>['styles']`, or the whole hook
  result when the part also needs `cx`). `useValueStyles()` with `ValueCell` shows it for the values of a test
  run, `RawTableGrid` with `RawTableCellText` for the cells of a workbook table, `TraceTree` with `Twisty` and
  `DispatchBadge` for the rows of a trace. A part that needs no class of its own takes no hook at all.
- **What is being written lives in the cell it is written in.** A table draws every cell it holds on every
  render, so a draft kept in the table's own state redraws all of them at every keystroke — on a 22 000-cell
  test table a key cost an extra frame, 67 ms against 34. `OpenCell` in `containers/modules` owns the draft
  and the way the value is being written, and hands the table back only the value it keeps.
- **A tree is built as far as it is opened.** Ant Design's `Tree` walks every node of its `treeData` each time
  it is drawn, open or closed: a whole test value of 1.77 million nodes ran the browser out of memory before its
  first line was read. A node is given its children only once the reader opens it, and lists them a step at a
  time with a line that lists more — `buildValueTreeData` takes how far the reader has gone (`ValueTreeReach`),
  and `ValueTree` in `components/values/ParameterValues.tsx` keeps it. A value of a test run is not even read
  whole: `ValueCell` given `readLines` reads it from the server a level at a time as the reader opens it
  (`LevelTree`, `buildLevelTreeData`), so neither the server nor the browser holds more of it than is shown.
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
  ESLint enforces this (`no-console`); `utils/errorHandling.ts` is the only allowed `console` call site. Pick the sink
  by audience:
    - **User-actionable errors** — surface in the UI: `notification.error`, form field errors, error boundaries.
    - **Background or diagnostic errors** (and details too technical for the UI) — `errorHandler.logError()` from
      `utils/errorHandling.ts`. It attaches context (url, user agent, timestamp), keeps the last 100 errors in memory
      for support, and prints to the browser console outside tests, so production failures stay diagnosable.

  There is no `console.warn` exception — `vitest-fail-on-console` fails a test on any warn. Transient recoverable
  signals (reconnect attempts, queued work, disconnected sends) are not errors and are not logged; expose them through
  a callback or state when a caller must react.
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
