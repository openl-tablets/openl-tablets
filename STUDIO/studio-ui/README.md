# Studio UI

## Project structure

- `src/App.tsx`: React entry that wires routing, providers and global state checks.
- `src/components/`: Reusable presentation components (forms, menus, icons, modals).
- `src/constants/`: Shared constants for roles, repositories, system flags, etc.
- `src/containers/`: Feature-level screens that orchestrate data fetching and state.
- `src/contexts/`: React contexts (`PermissionContext`, `SystemContext`) consumed across the app.
- `src/hooks/`: Shared hooks (`useIsFormChanged`, `useWebSocket`, etc.).
- `src/layouts/`: Layout wrappers such as `DefaultLayout` and `AdministrationLayout`.
- `src/locales/`: i18next resource bundles (English lives in `*.en.ts` files).
- `src/providers/`: Cross-cutting providers (`SecurityProvider`).
- `src/routes/`: Router configuration built with `createBrowserRouter`.
- `src/services/`: Runtime config, REST wrapper, websocket client.
- `src/store/`: Zustand stores (`appStore`, `userStore`, `notificationStore`).
- `src/utils/`: Cross-cutting helpers (error handling, global scripts).
- `src/pages/`: Standalone routes such as `LoginPage`, `403`, `404`, `500`.
- `src/types/`: TypeScript definitions aligned with backend DTOs.
- `src/index.tsx`: React entry point, registers i18n bundles and mounts the app.
- `src/index.scss`: Global styles and Ant Design overrides.
- `src/setPublicPath.ts`: Configures webpack public path at runtime.

Supporting files:
- `public/`: Static HTML shell and icons.
- `webpack.config.js`, `eslint.config.js`, `tsconfig.json`: build and linting configuration.
- `pom.xml`: Maven module that invokes the frontend toolchain via `frontend-maven-plugin`.

For a deeper architectural overview, see `AGENTS.md`.

---

## Prerequisites

- Node.js 24.x (used for local builds only; the backend ships bundles produced by npm).
- npm 11.x (required for compatibility with `lockfileVersion@3` in `package-lock.json`).
- Java 21 and Maven (matching the root `maven-compiler-plugin` release). The Maven build installs Node/npm automatically and runs the npm scripts.

---

## Quick start

```bash
# 1. Sync dependencies through Maven
mvn clean install -DskipTests

# 2. Launch the backend stack if you want a full environment (optional)
docker compose up --build

# 3. Start the React dev server (http://localhost:3100 by default)
npm run start
```

- `npm run start` proxies API calls to `http://localhost:8080` as configured in `package.json`. Adjust with `npm run start -- --proxy http://your-host`.
- For a production bundle run `npm run build` (license check included). The Maven build re-runs this automatically during packaging.

---

## Docker integration

When developing against the backend in Docker, point the containerised WebStudio to the local dev server by editing the root `compose.yaml`:

```yaml
services:
  webstudio:
    environment:
      _REACT_UI_ROOT_: http://localhost:3100
```

Rebuild or restart the stack after changing the environment variable:

```bash
docker compose up --build
```

---

## Useful npm scripts

- `npm run start` – webpack dev server with hot reload.
- `npm run build` – production build with license compliance check.
- `npm run serve` – static server for the `dist/` output (`serve -p 3002`).
- `npm run lint` / `npm run lint:fix` – ESLint + Stylelint.

---

## Troubleshooting

- If `npm install` keeps reinstalling dependencies unexpectedly, ensure you ran the Maven sync (`mvn clean install ...`) first; the Maven plugin will overwrite `node_modules/`.
- Websocket connection errors usually mean the backend isn’t exposing `${CONTEXT}/web/ws`; check reverse proxy rules.
- Missing translations? Import the relevant locale bundle in `src/locales/index.ts` and restart the dev server so webpack picks up the new module.
