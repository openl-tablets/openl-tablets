# MCP Server Internals

The MCP server built into OpenL Studio is the TypeScript program of `STUDIO/studio-mcp`. This page maps its sources,
the path of a request from `/mcp` to the OpenL Studio REST API, and where the server keeps state. How OpenL Studio
runs and secures the server is in [Built-in MCP Server for OpenL Studio](mcp-server-jetty-architecture.md); how users
connect agents is in the [OpenL Studio MCP Server Guide](../user-guides/mcp-server/index.md); the conventions for
changing the server are in [`STUDIO/studio-mcp/AGENTS.md`](../../STUDIO/studio-mcp/AGENTS.md).

## Sources

All paths are under `STUDIO/studio-mcp/src`.

- **`index.ts`** — the entry point; `--http` starts the HTTP transport.
- **`http-server.ts`** — Express with `/mcp` and `/health`: the `Origin` check, the body limit and the routing of a
  request to its transport.
- **`mcp-core.ts`** — one MCP `Server` with the `tools/*` and `prompts/*` handlers wired to the registries.
- **`handlers/`** — the tools, one module per category; `handlers/common.ts` holds the registry, argument validation,
  error mapping and the wait for Studio's background work.
- **`client.ts`**, **`auth.ts`** — `OpenLClient`, the axios client of the Studio REST API, and the `Authorization`
  header it sends.
- **`stomp-client.ts`**, **`stomp-waits.ts`** — the STOMP WebSocket that waits for a compilation.
- **`schemas.ts`**, **`types.ts`**, **`formatters.ts`** — the Zod schemas of the tool arguments, the Studio REST
  types, and the rendering of a result as JSON or Markdown.
- **`guides-registry.ts`**, **`prompts-registry.ts`** — the bundled reference guides and the prompts of `prompts/`.
- **`table-id-tracking.ts`** — the aliases from an old table id to the current one after an edit moved the table.
- **Build time** — `generate-build-info.ts` writes `build-info.json`, `build-guides.ts` the `guides/` bundle from
  `Docs/`, and `build-licenses.ts` the list of the libraries the bundle ships.

## The Path of a Request

1. Jetty's `/mcp` context proxies the request to `127.0.0.1:<mcp.port>`, path and query unchanged.
2. A request with an `Origin` outside `MCP_ALLOWED_ORIGINS` is refused with `403`; `OPTIONS` answers `204`.
3. `express.json` parses the body, up to `MCP_MAX_BODY_SIZE` (5 MB by default).
4. A 2026-07-28 request goes to the stateless handler, which builds a fresh `Server` and a fresh `OpenLClient` with
   the `Authorization` header of the request. A 2025 client gets a sessionful transport per `Mcp-Session-Id`, with a
   client of its own.
5. `tools/call` checks the `OPENL_MCP_TOOLS` allow-list; a tool left out answers as an unknown one. The arguments are
   validated against the strict schema of the tool, nested keys included, and the handler runs.
6. `OpenLClient` calls `<OPENL_BASE_URL>/rest`, with a fixed timeout of 30 seconds per call. A `Bearer` credential is
   sent to Studio as `Token`, unless `OPENL_MCP_PRESERVE_AUTH_SCHEME` is set. The client captures the `JSESSIONID`
   cookie Studio sets and sends it back on its next calls.

Tool names are bare inside the registry (`get_table`); the `openl_` prefix is added and stripped on the wire only.

## Where the State Lives

- **Per request** — a 2026-07-28 request has its own client: its cookie, its cache of repositories, nothing more.
- **Per legacy session** — one transport, one `Server` and one client for the life of the session.
- **Per process** — the tool registry, the prompts and guides read at start, and the table-id aliases, which all
  callers share.
- **In OpenL Studio** — the compiled project, test and run results, the debug session and merge conflicts live in
  the client scope of Studio ([Client Sessions](client-sessions.md)), kept per credential: per personal access
  token, per sign-in of the identity provider, or per user. A request without an `Authorization` header keeps them in
  its HTTP session.

## Waiting for Studio

- **Background results** — `waitForResult` of `handlers/common.ts` reads a result Studio answers with
  `202 {"status":"notReady"}` until it is ready: test results and merges. It reads again after a pause that grows
  from 250 ms to 2 s, sends `notifications/progress` meanwhile, and stops on the `AbortSignal` of the call.
- **Table runs** — `openl_run_table` starts the run and polls its result within the same call.
- **Compilation** — `openl_project_status` with `wait: true` compiles an idle project through a table read, then
  subscribes to `/user/topic/projects/{id}[/branches/{branch}]/status` over STOMP at `<base>/ws` and returns at the
  first final state, or the last state seen after 120 s. The topic sends transitions only, so the status is read
  again right after subscribing.
- **Trace** — `openl_resume_trace` polls the trace status every 250 ms, 30 s by default.

## Responses and Errors

- **Results** — JSON in a text content block. A result over 25,000 characters is cut: a list by items, with the page
  cursor moved to match, anything else to a preview. Test results, guides, binary file reads and merge-conflict
  chunks are never cut.
- **Errors** — a failed tool answers with an `isError` result; only an unknown tool is a protocol error. A Studio
  status maps to a code: `400` and `404` to `InvalidParams`, `401` and `403` to `InvalidRequest`, `405` to
  `MethodNotFound`, the rest to `InternalError`. Validation errors of Studio are folded into the message, up to 20
  lines; the REST address stays in the log.

## Guides and Prompts

- **Guides** — `npm run build` copies `Docs/ref` (ids `spec/…`) and `Docs/user-guides/reference-guide` (ids
  `guide/…`) into `guides/`, without `.md` and number prefixes; the title is the first heading. Renaming a page
  changes its id.
- **Prompts** — a file of `prompts/` is a prompt of the same name; its front matter holds the title, the description
  and the arguments, and the body takes `{arg}` and `{if arg}…{end if}`.

## Coupling with OpenL Studio

- `McpServerProcess` starts `dist/index.js --http` and waits for `/health` to answer `service: "openl-mcp"`: the
  `name` of `package.json` and the entry point must stay as they are.
- Studio sets `PORT`, `HOST` and `OPENL_BASE_URL`; every other variable comes from the environment of Studio.
- The output of the server lands in the `org.openl.studio.mcp.node` logger: standard output as INFO, standard error
  as WARN.

## Known Limitations

- **The compile wait needs a session cookie.** A request with a token opens no HTTP session, so while a compilation is
  still running `openl_project_status` returns the current state instead of waiting for the end.
- **Concurrent table runs** — `openl_run_table` refuses a second run only within one client. Over 2026-07-28 every
  request has a client of its own, so two runs with one token share the single run slot Studio keeps for it.
- **Legacy sessions** have no idle timeout and no limit, and keep the credential sent with `initialize`.
- **`tools/list` and `prompts/list`** carry no `ttlMs` or `cacheScope`.
- **`Host`** is not validated.
- **A body over the limit** fails with HTTP `500`, not a JSON-RPC error; with the base64 overhead, a file write is
  limited to about 3.7 MB.
- **The REST timeout** of 30 seconds cannot be changed.
- **Unbounded answers** — `openl_get_test_results` with `unpaged: true`, and a binary file read without `length`.
- **`openl_open_project` with a branch** ignores a failed branch switch and opens the project again, and ignores
  `revision` for a project that is already open.
- **`openl_deploy_project`** finds the deployment repository by its exact name, case included.
- **`openl_watch_trace_cells`** drops the status of the run, so a failed run returns part of the series with no error.
