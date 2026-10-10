# STUDIO Module — Web IDE

Spring Boot backend serving a REST API, and a React/TypeScript frontend that draws every screen.

## Key Conventions

- **The server renders no page.** Every address answers with the one page the frontend build wrote
  (`AppPageServlet`), and the screen is drawn in the browser; everything else the server answers is REST or
  a WebSocket message. A feature therefore lands as an endpoint plus a React screen — never as a server-rendered
  page or a fragment of HTML. Only a few addresses answer differently, each through a servlet mapping of its own:
  `ApiDocsServlet` answers `/api-docs` with the page the API documentation is drawn on, `StaticResourcesServlet`
  hands a built file (`/assets`, `/icons`, `/licenses`, the favicons) to the container, `UserGuidesServlet` answers
  `/docs` — a file of the user guides goes to the container, `/docs/toc.json` is their table of contents, and any
  other address is the page a guide is drawn on — and the Spring `StudioDispatcherServlet`
  serves the REST API under `/rest` and the WebSocket handshake at `/ws`. The two are different protocols, so the
  handshake is not under `/rest`: a proxy and a timeout treat a long-lived connection differently from a request.
  Both addresses share one security chain. The dispatcher lets a handler answer at its own address only.
- **ACL checks are the last thing a condition evaluates.** A permission probe reaches the ACL database
  through a transaction of its own, while the project state it is weighed against is already at hand, so a
  condition tests the state first and asks for the permission only when the answer still depends on it.
  Compute a permission lazily — never up front "in case it is needed" — and reuse one answer across every
  condition that weighs it (`ProjectAccessService.computeCapabilities` is the worked example). The same
  applies in the large: never ask per branch, per file or per artefact what one question about the project
  answers, and never repeat a question a pass over the same set has already answered.
- **Third-party licenses**: the About dialog of the user menu lists the libraries OpenL Studio ships from three files
  under `/licenses`. Vite writes `frontend-licenses.json` (`build.license`: the libraries bundled into the pages, with
  their license texts); the war build writes `backend-licenses.json` (`license-maven-plugin` `add-third-party` through
  `studio-backend/license/backend-licenses.json.ftl`: the third-party jars of `WEB-INF/lib`, with the license text a
  jar ships and the address its POM gives); `studio-mcp` writes `mcp-licenses.json` (`npm run licenses`, run in the
  bundle once `npm ci --omit=dev` has installed it: the npm packages of `WEB-INF/mcp/node_modules`, with the license
  file and the NOTICE each ships), which its jar carries in `licenses/` and the war unpacks beside the backend list.
  All share one shape — `name`, `version`, `identifier` (an SPDX expression), `text`, `url`, then `notice`.
    - **Every license is an SPDX identifier.** npm packages declare one already, and `npm run build` of `studio-ui` and
      `npm run licenses` of `studio-mcp` accept only the licenses their `--onlyAllow` lists. A POM names its license as it likes, so the `licenseMerges` of
      `studio-backend/pom.xml` turn each name into its identifier, and `includedLicenses` fails the war build on a
      name no merge turns: a new name is merged there, never shown as it is. A license either build accepts needs its
      public text in `PUBLIC_LICENSES` of `studio-ui/src/services/licenses.ts`, which a test holds both lists to.
    - **`notice` is the NOTICE a library ships**, the attribution a license such as Apache-2.0 (§4(d)) asks a
      redistribution to carry. Bundling leaves the NOTICE of an npm package behind, so `libraryNotices` in
      `vite.config.ts` adds it to the frontend list.
    - **The war build reads `text` and `notice` from each jar.** It unpacks the `META-INF/LICENSE*`,
      `META-INF/license*`, `META-INF/NOTICE*` and `META-INF/notice*` of every jar into `target/license/files` by the
      path of the jar in a repository (`unpack-license-files`) and runs a copy of the template there
      (`license-template`), since a template reads files of its own folder only: it includes the first LICENSE and the
      first NOTICE of each library as raw text (`.get_optional_template`).
    - **The Docker image adds `server-licenses.json`** beside the two lists, in their shape: the `licenses` stage of
      the root `Dockerfile` lists the Temurin JRE, Jetty, Log4j, the OpenTelemetry agent and Node.js, each with what of
      its LICENSE and NOTICE it ships, and Alpine Linux, linked to its package index. `jq` stays in that stage. Outside
      the image the file is not found, and the About dialog shows no side for it.
- **Static resources are public.** Every file the build leaves beside the pages — `/assets`, `/icons`, `/licenses`,
  the favicons, the files of the user guides — is listed in `SecurityConfig.staticResourcesFilterChain`, which runs
  no security filters at all. A page is never listed: the application page answers a page of a guide too, so
  `RequestMatchers.userGuideFiles()` admits an address below `/docs` only when its last part ends with an extension,
  the rule by which `UserGuidesServlet` never answers it with the page.
- **New features** → React in `studio-ui/`
- **DB migrations**: Flyway scripts in `studio-backend/resources/db/flyway/`
- **Authentication**: Form-based, SAML, OAuth2, LDAP/AD, Personal Access Tokens
- **REST API / OpenAPI**: Externalized descriptions, `@Parameter` vs `@Schema`, enum wire codes, and request
  validation follow strict rules — see [`studio-backend/AGENTS.md`](studio-backend/AGENTS.md)
- **Response field projection**: Clients add `?fields=id,name,modules(id,name)` to reduce JSON to selected fields, including nested objects and arrays (hierarchical, GraphQL-like). Applied globally during serialization (`org.openl.studio.common.projection`) — no controller-side parameter, no configuration. The selection is read before an endpoint declaring a projectable type runs, so a malformed one answers `400` without side effects. Any DTO under `org.openl.rules.*` or `org.openl.studio.*` is projectable, except framework infrastructure in `org.openl.studio.common.model` (errors, pagination wrappers). Errors, binary and non-JSON responses are never touched. OpenAPI integration lives separately in `org.openl.studio.openapi` and registers itself when the projection feature is present.

## Submodules

**Core application**:
- **studio-backend** — Main Spring Boot app (packages: `org.openl.studio.*`, `org.openl.rules.webstudio`, `org.openl.rules.rest`, `org.openl.rules.ui`).
  Its Maven artifact is `org.openl.rules.studio:studio-backend`. Its war is a GitHub release asset: the module is
  installed into the local repository but never deployed
- **studio-ui/** — React/TypeScript frontend (see `studio-ui/AGENTS.md`)
- **studio-docs/** — packs `Docs/user-guides` into a jar the war serves at `/docs`; never deployed to a remote
  repository, so the war depends on it as `optional` and copies it into `WEB-INF/lib` itself. Its tests validate the
  guides — see [`Docs/AGENTS.md`](../Docs/AGENTS.md)
- **studio-mcp/** — the built-in MCP server, openl-mcp in TypeScript (see `studio-mcp/AGENTS.md`). Its jar carries
  the server with its production `node_modules` in a non-public `mcp/` folder; never deployed to a remote repository,
  so the war depends on it as `optional` and unpacks the folder into `WEB-INF/mcp`. `McpServerProcess`
  (`org.openl.studio.mcp`) runs it with Node.js on the loopback interface, and the Docker image proxies `/mcp` to it

**Repository & storage**:
- **org.openl.rules.repository** — Repository abstraction layer
- **org.openl.rules.repository.git** — Git repository implementation
- **org.openl.rules.repository.aws** — AWS S3 storage
- **org.openl.rules.repository.azure** — Azure Blob storage

**Security** (inside `studio-backend`):
- `org.openl.rules.security` — Security abstractions
- `org.openl.rules.security.standalone` — Standalone auth (form-based, DB-backed, Flyway migrations in `resources/db/flyway/`)
- `org.openl.security.acl` — Access Control Lists

**OpenAPI** (inside `studio-backend`):
- `org.openl.rules.spring.openapi` — Spring OpenAPI integration

**Supporting modules**:
- **org.openl.rules.workspace** — Workspace management
- **org.openl.rules.diff** — Rule diff/comparison
- **org.openl.rules.jackson** / **org.openl.rules.jackson.configuration** — JSON serialization
- **org.openl.rules.project.openapi** / **org.openl.rules.project.validation.openapi** — OpenAPI generation and validation
- **org.openl.rules.xls.merge** — Excel merge utilities

## Backend Package Structure

```
org.openl.studio
├── config/          # Spring Boot configuration
├── security/        # Auth: AD, OAuth2, SAML, PAT (personal access tokens)
├── settings/        # System settings (rest/controller, service, model, converter)
├── notification/    # WebSocket notifications
├── projects/        # Project management and validation
├── repositories/    # Repository management
├── deployment/      # Deployment management
├── users/           # User management
├── tags/            # Tag management
├── rest/            # REST controllers
├── socket/          # WebSocket endpoints
└── common/          # Shared utilities
```
