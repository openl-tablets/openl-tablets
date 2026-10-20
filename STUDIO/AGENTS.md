# STUDIO Module — Web IDE

A Spring backend serving a REST API, and a React/TypeScript frontend that draws every screen.

The backend is a Spring web application, not a Spring Boot one: `SpringInitializer`, a listener declared in
`studio-backend/webapp/WEB-INF/web.xml`, builds an `XmlWebApplicationContext` from `/WEB-INF/spring/webstudio.xml`.
From Spring Boot it takes only the condition annotations of `spring-boot-autoconfigure`, such as
`@ConditionalOnExpression` — see
[`mcp-server.md`](../Docs/architecture/mcp-server.md).

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
  jar ships and the address its POM gives); `studio-mcp` writes `mcp-licenses.json` (a plugin of its bundler,
  `src/build-licenses.ts`: the npm packages the bundle takes code of, with the license file and the NOTICE each
  ships), which its jar carries in `licenses/` and the war unpacks beside the backend list.
  All share one shape — `name`, `version`, `identifier` (an SPDX expression), `text`, `url`, then `notice`.
    - **Every license is an SPDX identifier.** npm packages declare one already, and `npm run build` of `studio-ui` and
      of `studio-mcp` accept only the licenses the `--onlyAllow` of `studio-ui` lists. A POM names its license as it
      likes, so the `licenseMerges` of `studio-backend/pom.xml` turn each name into its identifier, and
      `includedLicenses` fails the war build on a name no merge turns: a new name is merged there, never shown as it
      is. A license either build accepts needs its public text in `PUBLIC_LICENSES` of
      `studio-ui/src/services/licenses.ts`, which a test holds both lists to.
    - **`notice` is the NOTICE a library ships**, the attribution a license such as Apache-2.0 (§4(d)) asks a
      redistribution to carry. Bundling leaves the NOTICE of an npm package behind, so `libraryNotices` in
      `vite.config.ts` adds it to the frontend list.
    - **The war build reads `text` and `notice` from each jar.** It unpacks the `META-INF/LICENSE*`,
      `META-INF/license*`, `META-INF/NOTICE*` and `META-INF/notice*` of every jar into `target/license/files` by the
      path of the jar in a repository (`unpack-license-files`) and runs a copy of the template there
      (`license-template`), since a template reads files of its own folder only: it includes the first LICENSE and the
      first NOTICE of each library as raw text (`.get_optional_template`).
    - **The Docker image adds `server-licenses.json`** beside the two lists, in their shape: the `licenses` stage of
      the root `Dockerfile` lists the Temurin JRE, Jetty, Log4j, the OpenTelemetry agent and, in the OpenL Studio image
      only, Node.js, each with what of its LICENSE and NOTICE it ships, and Alpine Linux, linked to its package index. `jq` stays in that stage. Outside
      the image the file is not found, and the About dialog shows no side for it.
- **Static resources are public.** Every file the build leaves beside the pages — `/assets`, `/icons`, `/licenses`,
  the favicons, the files of the user guides — is listed in `SecurityConfig.staticResourcesFilterChain`, which runs
  no security filters at all. A page is never listed: the application page answers a page of a guide too, so
  `RequestMatchers.userGuideFiles()` admits an address below `/docs` only when its last part ends with an extension,
  the rule by which `UserGuidesServlet` never answers it with the page.
- **The audit log records who did what.** An action worth accounting for records an `AuditEvent` — a new kind of
  action is a constant of `AuditAction`, with its category and its wire code — in the transaction of the change it
  records, so the two are saved together. A service publishes the event through the `ApplicationEventPublisher` it is
  constructed with, which `AuditService.recordEvent` listens to; a controller may call `recordEvent` itself. Only a
  change is announced, with the values before and after (`AuditChanges`). An event never holds a password or a token.
  The sign-ins come from the
  events of Spring Security, so a new way of signing in publishes them. A new writer of the ACL reads the role it
  replaces and announces both through `AclRoleAuditor`. A `403` of the REST API is recorded by
  `ApiExceptionControllerAdvice`, so a refusal needs no event of its own. An action on a project is announced
  through `ProjectAuditor` once it is done, since no transaction covers a repository. A save of the system settings
  is announced by `SettingsServiceImpl.commit` through `SettingsAuditor`, one event for every save; a new secret
  setting is declared `secret` in its model (`@SettingPropertyName`), so its value never reaches the log. See
  [`audit-log.md`](../Docs/architecture/audit-log.md).
- **New features** → React in `studio-ui/`
- **DB migrations**: Flyway scripts in `studio-backend/resources/db/flyway/`. A time is a `${timestamp}` column that
  holds UTC, mapped as an `Instant`; Oracle reads it through `OracleInstantTypeContributor`. A text of any length is
  a `${text}` column, a large object on Oracle and SQL Server: nothing sorts or groups by it, and no ordinary index
  holds it, so a search of it reads the table unless a database has an index of its own for it. A value
  one database keeps or reads back differently is written and read back by the `itest.studio/acl` suite, which CI
  runs on every supported database (see [`ITEST/AGENTS.md`](../ITEST/AGENTS.md))
- **Authentication**: Form-based, SAML, OAuth2, LDAP/AD, Personal Access Tokens
- **REST API / OpenAPI**: Externalized descriptions, `@Parameter` vs `@Schema`, enum wire codes, and request
  validation follow strict rules — see [`studio-backend/AGENTS.md`](studio-backend/AGENTS.md)
- **Response field projection**: Clients add `?fields=id,name,modules(id,name)` to reduce JSON to selected fields, including nested objects and arrays (hierarchical, GraphQL-like). Applied globally during serialization (`org.openl.studio.common.projection`) — no controller-side parameter, no configuration. The selection is read before an endpoint declaring a projectable type runs, so a malformed one answers `400` without side effects. Any DTO under `org.openl.rules.*` or `org.openl.studio.*` is projectable, except framework infrastructure in `org.openl.studio.common.model` (errors, pagination wrappers). Errors, binary and non-JSON responses are never touched. OpenAPI integration lives separately in `org.openl.studio.openapi` and registers itself when the projection feature is present.

## Submodules

**Core application**:
- **studio-backend** — the backend and its war (packages: see [Backend Packages](#backend-packages)).
  Its Maven artifact is `org.openl.rules.studio:studio-backend`. Its war is a GitHub release asset: the module is
  installed into the local repository but never deployed
- **studio-ui/** — React/TypeScript frontend (see `studio-ui/AGENTS.md`)
- **studio-docs/** — packs `Docs/user-guides`, and `Docs/ref` under `ref/`, into a jar the war serves at `/docs`;
  never deployed to a remote repository, so the war depends on it as `optional` and copies it into `WEB-INF/lib`
  itself. Its tests validate the guides — see [`Docs/AGENTS.md`](../Docs/AGENTS.md)
- **studio-mcp/** — the built-in MCP server, in TypeScript (see `studio-mcp/AGENTS.md`). Its jar carries
  the server, bundled with its dependencies into one file, in a non-public `mcp/` folder; never deployed to a remote
  repository, so the war depends on it as `optional` and unpacks the folder into `WEB-INF/mcp`. `McpServerProcess`
  (`org.openl.studio.mcp`) runs it with Node.js on the loopback interface, and `McpProxyServlet` serves it at `/mcp`

**Repository & storage**:
- **org.openl.rules.repository** — Repository abstraction layer
- **org.openl.rules.repository.git** — Git repository implementation
- **org.openl.rules.repository.aws** — AWS S3 storage
- **org.openl.rules.repository.azure** — Azure Blob storage

**Supporting modules**:
- **org.openl.rules.workspace** — Workspace management
- **org.openl.rules.diff** — Rule diff/comparison
- **org.openl.rules.jackson** / **org.openl.rules.jackson.configuration** — JSON serialization
- **org.openl.rules.project.openapi** / **org.openl.rules.project.validation.openapi** — OpenAPI generation and validation
- **org.openl.rules.xls.merge** — Excel merge utilities
- **`../WSFrontend/org.openl.rules.ruleservice.annotation`**, **`.common`**, **`.ws.common`** — modules of OpenL Rule
  Services that `STUDIO/pom.xml` builds as well

## Backend Packages

The full map of `studio-backend/src/org/openl/` is in
[`codebase-tour.md`](../Docs/onboarding/codebase-tour.md). To place new code:

- **REST API of an area** — `org.openl.studio.<area>` (`projects`, `repositories`, `deployment`, `users`, `tags`,
  `settings`, `compare`, `audit`) keeps its controllers in `rest/controller` (`compare/rest`) beside its services and
  models.
  `org.openl.studio.rest` holds only the argument resolvers of pagination; `org.openl.rules.rest` holds the mail,
  management, user and ACL controllers.
- **WebSocket** — configuration and handshake in `org.openl.studio.socket`; notifications to the clients in
  `org.openl.rules.webstudio.notification`.
- **Shared** — `org.openl.studio.common` (exception handling, error and page models, projection, validation),
  `org.openl.studio.config`, `org.openl.studio.session` (client sessions).
- **Security** — `org.openl.studio.security` (security chains, AD, OAuth2, SAML, PAT), `org.openl.rules.security`
  (users and groups; `standalone` for the users of the database), `org.openl.security.acl` (access control lists).
- **OpenAPI** — `org.openl.rules.spring.openapi` (the Spring OpenAPI integration), `org.openl.studio.openapi` (the
  projection in the documentation).
