# MCP Server Internals

The MCP server built into OpenL Studio is the TypeScript program of `STUDIO/studio-mcp`. This page maps its sources,
the path of a request from `/mcp` to the OpenL Studio REST API, and where the server keeps state. How OpenL Studio
runs and secures the server is in [Built-in MCP Server](mcp-server.md); how users
connect agents is in the [OpenL Studio MCP Server Guide](../user-guides/mcp-server/index.md); the conventions for
changing the server are in [`STUDIO/studio-mcp/AGENTS.md`](../../STUDIO/studio-mcp/AGENTS.md).

## Sources

All paths are under `STUDIO/studio-mcp/src`.

- **`index.ts`** — the entry point, which starts the HTTP transport; the server has no other mode.
- **`http-server.ts`** — the `node:http` request listener with `/mcp`: the body limit and the routing of a request to
  its transport.
- **`mcp-core.ts`** — one `McpServer` with every tool of the registry and the skills registered on it; the server
  offers no prompts.
- **`skills.ts`** — the OpenL skills, which the skills extension of the MCP SDK serves.
- **`handlers/`** — the tools, one module per category. `handlers/common.ts` holds the registry, argument validation,
  the answer of a tool and error mapping; `handlers/wait.ts` the wait for Studio's background work;
  `handlers/table-edit.ts` the end of every table edit: the id Studio answers for a renamed table, and the read that
  compiles the table again.
- **`client.ts`**, **`auth.ts`** — `OpenLClient`, the client of the Studio REST API, and the `Authorization` header
  it sends.
- **`http.ts`** — the HTTP client under `OpenLClient`, on the `fetch` of Node.js: query strings, JSON and multipart
  bodies, timeouts and the errors of a call. It ignores `HTTP_PROXY`, as `fetch` does unless `NODE_USE_ENV_PROXY` or
  `--use-env-proxy` turns the proxy of the environment on.
- **`schemas.ts`**, **`types.ts`**, **`formatters.ts`** — the Zod schemas of the tool arguments, the Studio REST
  types, and the rendering of a result as JSON or Markdown; `dependency-markdown.ts` renders a table dependency graph
  as Mermaid diagrams, `json-cut.ts` cuts a JSON answer over the limit in its structure, and `text.ts` holds the text
  helpers both renderings share.
- **`guides-registry.ts`** — the specifications and the Reference Guide OpenL Studio serves at `/docs`.
- **`transfer-links.ts`** — the links a file tool answers instead of the bytes of a file: a transfer link Studio
  issues where it authenticates, the address of the REST API in single-user mode, with where the file lives in its
  repository and a `curl` command ([MCP File Transfer Links](mcp-file-transfer.md)).
- **`telemetry.ts`**, **`tool-telemetry.ts`** — OpenTelemetry. `index.ts` loads `telemetry.ts`, the OpenTelemetry SDK
  for Node.js with the instrumentation of `node:http` and of `fetch`, only when `OTEL_SDK_DISABLED` is `false`, and
  stops it on `SIGTERM`, so the buffered telemetry is sent. A failure to start it is logged, and the server runs
  without telemetry. `tool-telemetry.ts` wraps every tool call in a span
  `tools/call <tool>` and records `mcp.server.operation.duration`, through the OpenTelemetry API, which does nothing
  without the SDK.
- **Build time** — `rolldown.config.js` of the module bundles the server and its dependencies into minified ES modules:
  `dist/index.mjs`, and beside it a chunk for each module the server imports only when it needs it, such as the
  OpenTelemetry SDK; `build-requires.ts`, a plugin of the bundler, bundles the packages a dependency loads with a
  `require` of its own instead of leaving them to a `node_modules` folder the war does not carry; `build-licenses.ts`, a
  plugin of the bundler, lists the libraries the bundle ships in `dist/mcp-licenses.json`, and `build-skills.ts`,
  another, embeds the files of the skills of `STUDIO/studio-mcp/skills` as the module `virtual:skills`, which the
  tests import through the same plugin.

## The Path of a Request

1. `McpProxyServlet` of OpenL Studio gets the request once the security of Studio has checked its credentials, as for a
   REST call. It passes the request to `127.0.0.1` at the port of the server with its path, query, headers and body, and
   adds two addresses of Studio: the one to call it at, the local address and port the request came in through, in
   `OpenL-Base-Url`, and the one the client opened in `OpenL-Public-Url`. A virtual thread streams the answer back, so
   no thread of the servlet container waits for the server.
2. A JSON body is read up to 5 MB; a larger one answers `413`, and one that is not JSON answers `400`, both with a
   JSON-RPC error.
3. A 2026-07-28 request goes to the stateless handler, which builds a fresh `McpServer` and a fresh `OpenLClient` with
   the `Authorization` header of the request. A 2025 client gets a sessionful transport per `Mcp-Session-Id`, with a
   client of its own. The client also knows the address of OpenL Studio the caller opened, for
   `openl_get_server_info`, from `OpenL-Public-Url`.
4. The server registers every tool; an unknown one answers with the `InvalidParams` error `Tool <name> not found` of
   the SDK. Each tool is registered with a schema that publishes its JSON Schema and passes the arguments on. The
   published schema leaves out the dialect and the bounds of a safe integer Zod puts on every integer, which every
   agent would load in every session and which tell it nothing; `executeTool` validates the arguments against the
   strict schema of the tool, nested keys included, with the hints of the tool, and the handler runs.
5. `OpenLClient` calls `/rest` of the address in `OpenL-Base-Url`, with a fixed timeout of 30 seconds per call. The
   `Authorization` header of the request, which Studio has checked at `/mcp`, goes to Studio as it is. The client keeps
   no cookie: Studio keeps the state of a client for its credential.

Tool names are bare inside the registry (`get_table`); the `openl_` prefix is added and stripped on the wire only.

## Where the State Lives

- **Per request** — a 2026-07-28 request has its own client: its cache of repositories, nothing more.
- **Per legacy session** — one transport, one `McpServer` and one client for the life of the session. The session
  ends when the client closes it. Most clients never do, so opening a session closes the others no request has used
  for 30 minutes, the HTTP session timeout of Studio. A session with a request in progress, its stream of server
  messages included, is in use.
- **Per process** — the tool registry, which all callers share. Table ids need no state of the server: Studio keeps
  a table's id when the table moves and serves an id from before a rename ([Stable Table Ids](table-ids.md)).
- **In OpenL Studio** — the compiled project, test and run results, the debug session and merge conflicts live in
  the client scope of Studio ([Client Sessions](client-sessions.md)), kept per credential: per personal access
  token, per sign-in of the identity provider, or per user. In single-user mode they are kept for the one user, so
  stateless requests without a token find them too.

## Waiting for Studio

- **Background results** — `waitForResult` of `handlers/wait.ts` reads a result Studio answers with
  `202 {"status":"notReady"}` until it is ready: test results and merges. It reads again after a pause that grows
  from 250 ms to 2 s, sends `notifications/progress` meanwhile, and stops on the `AbortSignal` of the call.
- **Table runs** — `openl_run_table` starts the run and polls its result within the same call.
- **Compilation** — `openl_project_status` switches the project to a requested branch, then compiles an idle project
  through a table read, which Studio answers once the compilation ends. A compilation already running, or one that
  outlasts `timeoutMs` (30 seconds by default), is answered as `compiling`, with a note to call again. The server opens
  no WebSocket.
- **Trace** — `openl_resume_trace` polls the trace status every 250 ms, 30 s by default.

## Responses and Errors

- **Results** — JSON without indentation in a text content block: the agent reads it as well as indented JSON, and
  indentation alone would take more of its context than the data, 60% of a table read. A result over 25,000
  characters is cut by whole items, in JSON and Markdown alike: a list by items, with the page cursor moved to match,
  a table read by rows, with `totalRows` and the `startRow` to continue from. The note on a cut result names only a
  way to continue that the tool has. A JSON answer that has no items to cut by, or whose one item does not fit, is
  cut in its structure (`json-cut.ts`): texts longer than 200 characters are shortened, then the largest array loses
  items from its middle, keeping its first and last ones, then the largest object is replaced by the count of its keys,
  and the note names each cut by its path. A page of `openl_get_test_results` is cut by
  whole test tables, and the tests of one test table by whole tests. An unpaged read of test results and guides are
  never cut. A file
  read answers a text of up to 5,000 bytes inline, and any other file by a link the agent runs `curl` with
  ([MCP File Transfer Links](mcp-file-transfer.md)).
- **Errors** — a failed tool answers with an `isError` result; only an unknown tool is a protocol error. A Studio
  status maps to a code: `400` and `404` to `InvalidParams`, `401` and `403` to `InvalidRequest`, `405` to
  `MethodNotFound`, the rest to `InternalError`. Validation errors of Studio are folded into the message, up to 20
  lines; the REST address stays in the log.

## Guides

- **Source** — OpenL Studio serves the specifications at `/docs/ref` and the Reference Guide at
  `/docs/reference-guide`, with its other user guides. Every guide tool reads the table of contents, `/docs/toc.json`,
  which gives the title and the size of each page, and `openl_get_guides` reads the pages it is asked for. The
  `index.md` page of a folder stays out; `openl_get_started` groups the guides by the sections Studio titles.
- **Ids** — `spec/…` and `guide/…`: the path in the folder without `.md` and number prefixes. Renaming a page
  changes its id.
- **Paths** — `path` is the address of a document relative to the address of OpenL Studio. Bodies come as Studio
  serves them, so their links and images are relative to that address too.

## Skills

- **Source** — one folder per skill in `STUDIO/studio-mcp/skills`, in the layout of the
  [Agent Skills specification](https://agentskills.io/specification). Each holds only what the tools,
  `openl_get_started` and the guides Studio serves do not say.
- **Build** — the bundler loads `skills/` with the skills extension of the MCP SDK and embeds the text of every file
  it serves; hidden files and folders, such as `.DS_Store`, are left out. A skill the extension refuses — one
  against the specification, such as a `name` other than its folder or a description over 1024 characters, or over
  the limits of 512 files or 16 MiB — or a file that is not UTF-8 text fails the build.
- **Protocol** — the SDK serves the embedded files (`SkillsExtension` of `@modelcontextprotocol/server/ext/skills`)
  and declares `io.modelcontextprotocol/skills` in the capabilities, with `directoryRead`. Every file is a resource
  `skill://<skill>/<path>`, which the template `skill://{+path}` reads; `resources/list` names each `SKILL.md` by
  the name and description of its frontmatter. `skills/list` answers the skills, `skills/get` one by the URI of its
  `SKILL.md`, and `resources/directory/read` the files of a folder of a skill. An entry carries the frontmatter as
  JSON and every file with its SHA-256 digest and size.
- **Clients without the extension** — the `instructions` of the server, which a client gives its agent when it
  connects, send the agent to `openl_get_started` first and list the skills with their URIs and descriptions, as
  `openl_get_started` does too, so an agent reads the skill that matches its task as resources.

## Coupling with OpenL Studio

- `McpServerProcess` starts `WEB-INF/mcp/index.mjs` with `--max-semi-space-size=1`, which keeps the young generation
  of V8 at 1 MB per semi-space, and waits for the line the server logs once it listens: its end,
  `listening on <host>, port <port>`, names the free port the server took, and must stay as it is, as the entry point.
- Studio sets `PORT` to `0`, `HOST` and `OPENL_VERSION`, the version the server reports in `serverInfo`, and removes
  `NODE_USE_ENV_PROXY`, which would send the calls to Studio through the proxy of the environment; every other variable,
  `NODE_OPTIONS` included, comes from the environment of Studio.
- Studio sets `OTEL_SDK_DISABLED` to `false` when it sends traces or metrics itself, by the rule of `OtelEnvironment`,
  and to `true` otherwise, and `OTEL_SERVICE_NAME` to its own service name with the suffix `-mcp`. The other `OTEL_*`
  variables of its environment reach the server as they are.
- The output of the server lands in the `org.openl.studio.mcp.node` logger: standard output as INFO, standard error
  as WARN.

## Known Limitations

- **Concurrent table runs** — `openl_run_table` refuses a second run only within one client. Over 2026-07-28 every
  request has a client of its own, so two runs with one token share the single run slot Studio keeps for it.
- **Legacy sessions** have no idle timeout and no limit, and keep the credential sent with `initialize`.
- **`tools/list`** carries no `ttlMs` or `cacheScope`.
- **`Host`** is not validated.
- **A text write** travels in the MCP request, whose whole body — JSON escaping included — is limited to 5 MB, so
  a text well under that size may already need an upload link; a binary file always goes by one.
- **The REST timeout** of 30 seconds cannot be changed.
- **Unbounded answers** — `openl_get_test_results` with `unpaged: true`.
- **`openl_open_project` with a branch** ignores a failed branch switch and opens the project again, and ignores
  `revision` for a project that is already open.
- **`openl_deploy_project`** finds the deployment repository by its exact name, case included.
- **`openl_watch_trace_cells`** drops the status of the run, so a failed run returns part of the series with no error.
