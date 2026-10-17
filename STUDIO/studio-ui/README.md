# Studio UI

The React/TypeScript frontend of OpenL Studio. It draws every screen in the browser; the backend
(`STUDIO/studio-backend`) serves the built pages and answers REST and WebSocket calls.

The module structure, the architecture and the coding and testing conventions live in [`AGENTS.md`](AGENTS.md).

## Prerequisites

- **Java and Maven** — the versions the root `pom.xml` builds with.
- **Node.js and npm** — needed only to run npm commands by hand. The Maven build downloads its own copy into
  `target/`, at the versions the root `pom.xml` names (`node.version`, `npm.version`); the `engines` field of
  `package.json` gives the lowest versions accepted.

## Maven Build

Maven builds the module through `frontend-maven-plugin`: it installs Node.js and npm, then runs `npm install`,
`npm run typecheck`, `npm run build` and `npm run test`. The bundle in `dist/` is packed into the jar the backend
war depends on.

```bash
mvn clean install -pl STUDIO/studio-ui   # Build the frontend alone
```

- **`-Dnpm.test.skip`** — leave out the Vitest suite; `-DskipTests` implies it.
- **`-Dnpm.typecheck.skip`** — leave out the `tsc --noEmit` pass.
- **`-Dnpm.build.skip`** — leave out the production bundle.

## npm Scripts

Run them from `STUDIO/studio-ui`, after `npm install`.

- **`npm run start`** — the Vite dev server at `http://localhost:3100`, with hot reload.
- **`npm run build`** — the license check of the production dependencies, then the production bundle in `dist/`.
- **`npm run serve`** — serves the built `dist/` at `http://localhost:3002` (`vite preview`).
- **`npm run typecheck`** — type-checks the sources (`tsc --noEmit`).
- **`npm run lint`** / **`npm run lint:fix`** — ESLint over `src/`, reporting or fixing.
- **`npm test`** — the Vitest suite once, with coverage.
- **`npm run test:watch`** — Vitest in watch mode.

## Developing Against the Backend

The dev server needs a running OpenL Studio at `http://localhost:8080` — started from the IDE or with
`docker compose up --build` from the repository root. Two ways to work with it:

- **Open the dev server** at `http://localhost:3100`. It forwards `/rest`, `/ws`, `/login`, `/logout`,
  `/licenses` and the files of `/docs` to `http://localhost:8080` (`server.proxy` in `vite.config.ts`).
- **Open the backend** at `http://localhost:8080` and let it load the scripts from the dev server. Set
  `_REACT_UI_ROOT_` to the dev server address; for Docker, copy the root `compose.override.example.yaml` to
  `compose.override.yaml` and set it under the `studio` service:

```yaml
services:
  studio:
    environment:
      _REACT_UI_ROOT_: http://localhost:3100
```

Restart the stack after changing the setting — the backend reads it once, at start.
