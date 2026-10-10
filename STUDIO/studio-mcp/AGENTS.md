# OpenL MCP Server — Agent Guide

This is the runtime reference for AI agents using the server — its tools, prompts,
and behaviour — and the conventions for changing it, in
[Working in this module](#working-in-this-module) at the end. OpenL Studio carries
the server in its war and serves it at `/mcp`; the design is
[Built-in MCP Server for OpenL Studio](../../Docs/architecture/mcp-server-jetty-architecture.md).

## Overview

The OpenL MCP Server connects AI coding agents (Claude Code, Claude Desktop,
Cursor, VS Code / GitHub Copilot) to the OpenL Studio Business Rules Management
System (BRMS). Through its tools you can:

- **Get oriented** with an onboarding entry point and bundled OpenL reference docs
- **Discover** repositories, projects, and rules
- **Read** project structure, table definitions, and rule logic
- **Modify** rules, tables, and project files
- **Test** rules and inspect results
- **Debug** rule execution interactively (breakpoints, stepping, live inspection)
- **Deploy** projects, and manage Git-based history

## How it talks to OpenL Studio

The server calls the OpenL Studio REST API (JSON, optional Personal Access Token).
For the studio's asynchronous work it also opens a STOMP WebSocket so a single tool
call can wait for the result instead of agent-side polling — project compilation
uses STOMP (`openl_project_status` with `wait: true`), while regular table execution
is polled internally by `openl_run_table`.

## Tools (74 Total)

All tools are prefixed with `openl_` and share the server's version.

### Guidance Tools (4)
The onboarding and reference-documentation layer. Call `openl_get_started` once per
session before anything else; call `openl_get_project_agent_context` before working on
or creating any project. The documentation tools serve a bundle of the official OpenL
Tablets docs **embedded at build time** from `Docs/ref` and
`Docs/user-guides/reference-guide` of this repository — progressive disclosure: the index is metadata-only, bodies are fetched
by id on demand.
- `openl_get_started` - Read-only onboarding bootstrap: the mandatory workflow protocol (load agent context per project, consult guides on demand, edit → validate → save) plus a workspace orientation (which specification/guide categories exist and how to discover more — not an index dump)
- `openl_get_project_agent_context` - Resolve the **AGENTS.md hierarchy** for a project as a **single aggregated markdown document**: walks UP from the project (or an optional `folder`) to the repository root, collects every applicable `AGENTS.md`, and returns them concatenated in one response — ordered from the root folder (lowest priority) down to the project folder (highest priority), later sections winning on conflict. Ends with the ids of bundled guides the guidance references
- `openl_list_guides` - The canonical index of the bundled docs: **metadata only** (id, type, title, source path, size), filterable by `type` ('specification'/'guide') and case-insensitive `search` over id+title, paginated
- `openl_get_guides` - Full markdown bodies for 1-5 ids from the index (e.g. `spec/rules.xml`, `guide/introduction/basic-concepts`); unknown ids fail with an actionable error — it never falls back to the index

### Repository Tools (4)
- `openl_list_repositories` - List all design repositories
- `openl_list_branches` - List Git branches in a repository
- `openl_list_repository_features` - Get repository capabilities
- `openl_repository_project_revisions` - Get project revision history by stable project ID in the project's current branch

### Project Tools (16)
- `openl_list_projects` - List projects with filters and pagination; follow `has_more` / `next_offset` until `has_more` is false when a complete inventory is required
- `openl_get_project` - Get project details
- `openl_project_status` - Compile lazily when needed and return project compile state and diagnostics (errors/warnings with location); `wait=true` is the default, while explicit `wait=false` is a snapshot that may return `idle`/`compiling`
- `openl_create_project` - Create or copy a project atomically through Studio: omit `template` for a BLANK project, or pass an existing project's exact `projectId` from `openl_list_projects` to copy its full structure and rename it. Both modes accept an optional target `branch` and return the commit revision plus Studio's opaque `projectId` when it can be resolved; if it is missing, call `openl_list_projects` to find the created project
- `openl_open_project` - Open project for editing (supports branch/revision switching)
- `openl_save_project` - Save project changes to Git with validation
- `openl_close_project` - Close project with save/discard options (prevents data loss)
- `openl_create_project_branch` - Create new branch
- `openl_list_project_local_changes` - View one module's workspace history using explicit `projectId` and `moduleName`; requires an opened project and does not support repository `local`
- `openl_restore_project_local_change` - Restore one module to a listed local version using explicit `projectId` and `moduleName`; requires an opened project and does not support repository `local`
- `openl_start_project_tests` - Start project test execution
- `openl_get_test_results_summary` - Get brief test execution summary; waits internally through Studio's `202 {"status":"notReady"}` responses until the asynchronous run finishes
- `openl_get_test_results` - Get full test execution results with pagination; waits internally for completed results instead of requiring agent-side polling
- `openl_get_test_results_by_table` - Get test results filtered by table ID; waits before filtering, so an in-progress run is never presented as an empty result
- `openl_list_project_modules` - List declared project modules, including path patterns and their matched modules
- `openl_list_module_sheets` - List worksheets in a project module

### Project Branch/Merge/Deletion Tools (8, BETA)

Merge conflict state is bound to the Studio HTTP session. After a merge returns
`status: "conflicts"`, inspect it through the same MCP server instance, present
the evidence to the user, and leave resolution to them in Studio. Conflict
resolution is intentionally not exposed: Studio does not provide a sufficiently
safe API for autonomous resolution, and choosing `OURS` or `THEIRS` can discard
valid work. Never choose or apply a conflict side automatically. Use the cancel
tool only to clear pending MCP session state after the user takes over or the
merge is abandoned. `receive` merges the other branch into the project's current
branch; `send` merges the current branch into the other branch.

- `openl_list_project_branches` - List branches with base/protected flags: `scope: "project"` (default) returns branches that hold the project for switching/deletion; `scope: "repository"` returns every repository branch for merge-target discovery, including branches that do not hold the project yet
- `openl_check_project_merge` - Check merge direction, branch relationship, permissions, and blockers without changing Git; `mergeable`/`canMerge` permits an attempt but does not predict conflicts
- `openl_merge_project_branches` - Recheck and attempt the merge, where conflicts are first discovered; creates session-bound conflict state when needed
- `openl_get_merge_conflicts` - Get grouped conflicted paths, BASE/OURS/THEIRS revisions, and the default message
- `openl_read_merge_conflict_file` - Read a bounded UTF-8 or base64 binary chunk for one BASE/OURS/THEIRS file version
- `openl_cancel_merge_conflicts` - Clear the pending conflict session without modifying files or branches
- `openl_delete_project` - Delete a project after exact current-name confirmation
- `openl_delete_project_branch` - Delete a non-base branch after exact branch confirmation and a safe-delete preflight against the repository base; unmerged commits, unsaved changes, or unverifiable divergence require explicit data-loss confirmation, while protected bypass also requires force confirmation

### Rules/Tables Tools (10)
- `openl_list_tables` - List project tables with pagination; follow `has_more` / `next_offset` until `has_more` is false when a complete inventory is required
- `openl_get_table` - Get the authoritative `RawSource` 2D cell matrix, including context-parsed one-dimensional multi-value arrays; `startRow`/`maxRows` read a large table in row slices and `styles=true` includes read-only Excel cell styles. A sliced response carries `totalRows` and is for reading or narrow raw actions only
- `openl_update_table` - Replace the complete `RawSource` matrix while preserving or adding rows, including scalar and representable one-dimensional array values returned by `openl_get_table`; before writing, it reads one live row and rejects a shorter source even if `totalRows` was removed, preventing a sliced view from deleting unseen rows. Remove rows with `openl_delete_table_rows`. Call `openl_get_table` without `styles=true`: Studio write APIs cannot change formatting, and `style` is rejected rather than silently ignored
- `openl_append_table` - Append full-width `RawSource` rows with scalar or representable one-dimensional array cell values
- `openl_create_project_table` - Create a table from a complete `RawSource` matrix in an existing module, or pass `modulePath` (an `.xlsx` project-relative path) to create a new module; cell values may be scalars or representable one-dimensional arrays, while cell formatting is unsupported by Studio write APIs
- `openl_delete_table` - Delete an entire table (to remove a row/column WITHIN a table, use the raw action tools below)
- `openl_run_table` - Execute a regular (non-Test) table with JSON input and wait for its result, treating Studio's `202 {"status":"notReady"}` result response as still running (and retaining the old coded `409 *.not.completed` response only for compatibility with older Studio versions); provide parameters by name, either directly or under an object-valued `params` field. The tool rejects `{ params: [...] }` because Studio silently executes that shape with null arguments; a top-level array remains available as the value of a single array-valued parameter, not as positional arguments. `timeoutMs` bounds the complete start-and-result workflow (default 2 minutes), and cancellation, timeout, or any other failed workflow clears the pending Studio run. Studio permits only one run per HTTP session, so concurrent calls through the same MCP connection are rejected rather than allowed to replace each other's result
- `openl_get_table_dependencies` - Get the executable and datatype dependency graph: omit `tableId` for a whole-project/module view with optional `layer` (`executable`/`datatype`/`all`), or provide it for a table's dependency/dependent neighborhood. JSON returns the adjacency list, including bounded first/last value previews for vocabularies; Markdown visualizes executable calls as a Mermaid flowchart and the data model with declared fields, `Name<Type>` vocabulary headers, bounded values (`+ N more` when truncated), and reference cardinalities as a Mermaid ER diagram, plus a separate inheritance diagram when needed
- `openl_list_table_property_definitions` - List properties allowed in a table context, including types and enum values
- `openl_copy_table` - Copy a table server-side inside the project while preserving formatting, merged cells, comments, and structure

### Raw Table-Source Action Tools (12)
In-place edits to a table's raw source (any table type). One tool per operation×orientation handles **one OR more** rows/columns — pass a single row/column or several; the studio takes a single `rows`/`columns` block target (one row/column is just a one-element block), so there is no separate "row" vs "rows" tool. Positions are 0-based (row 0 is the header, column 0 the leading labels). `cells` is required and non-empty (one cell per column/row; use `{ value: null }` for a blank cell). Writable values use the same round-trip contract as full-table writes: string/number/boolean/null scalars or one-dimensional arrays of those scalars. Arrays must be non-empty; `[null]`, nested arrays, objects, and string elements surrounded by characters Studio trims (whitespace or ISO controls) are rejected. An edit that relocates the table changes its id; each tool returns the table's CURRENT `tableId` (plus `previousTableId` when it changed) and reads the table back to trigger a recompile.

Rows / columns (one or many):
- `openl_append_table_rows` / `openl_append_table_columns` - Add one or more rows/columns to the end (`cells` is a 2D array, one inner list per row/column)
- `openl_insert_table_rows` / `openl_insert_table_columns` - Insert one or more rows/columns at `position` 1..
- `openl_delete_table_rows` / `openl_delete_table_columns` - Delete `count` (default 1) rows/columns from `position` 1.. (the header row / label column 0 cannot be deleted)

Cells / ranges:
- `openl_update_table_row` / `openl_update_table_column` - Overwrite the cells of the row/column at `position`
- `openl_update_table_cell` - Set a single cell's value at (`row`, `column`)
- `openl_update_table_range` - Overwrite a rectangular range (> 1 cell) anchored at (`row`, `column`)
- `openl_merge_table_cells` - Merge a `rowspan`×`colspan` range from (`row`, `column`)
- `openl_unmerge_table_cells` - Unmerge the cell covering (`row`, `column`)

### Project Files Tools (6, BETA)
Operate on ANY file in a project by exact project-relative path (not just Excel rule files). Writes/deletes/copies/moves land in the project **working copy** — commit them with `openl_save_project`. Use the optional `branch` to pin the project's branch (omit for `local`/non-branch repositories).
- `openl_read_project_file` - Read a file (text verbatim; arbitrary binary as lossless base64 `content` in a JSON TextContent envelope with MIME/byte metadata; optional `offset`/`length` byte range), read file metadata (`view: "meta"`), or list a folder (`recursive`, `viewMode` FLAT/NESTED, `extensions`, `namePattern`, `foldersOnly`); optional `version` reads a historical revision
- `openl_write_project_file` - Create/replace a file from UTF-8 `content` or a base64 `blob` advertised with JSON Schema `contentEncoding: "base64"`; the legacy base64 `content` + `encoding` form remains accepted, including whitespace-wrapped base64; `createFolders` (default true), `conflictPolicy` FAIL/OVERWRITE/SKIP
- `openl_delete_project_file` - Delete a file/folder (auto-cleans dangling config references)
- `openl_search_project_files` - Search by glob `pattern`, `extensions`, `type`, or case-insensitive `content` substring; `scope` SUBTREE (default) or ANCESTORS. Studio searches `content` only inside text files—not XLSX/XLS/ZIP/images; locate binary files by path/extension and read them separately
- `openl_copy_project_file` - Copy a file within the project (no overwrite — destination collision returns 409)
- `openl_move_project_file` - Move or rename a file within the project

### Trace Tools (9, BETA)
An **interactive debugger** for rules: the rule runs on a server-side worker that
suspends at breakpoints and step points, and the tools inspect that live, suspended
execution. The debug session is bound to the MCP server's HTTP session — the whole
flow must go through one server instance (or one CLI `--cookie-jar`). One active
session per user (a new start terminates the previous); idle sessions are reaped
after ~10 minutes.

- `openl_start_trace` - Start a debug session for a table (test case via `testRanges`, or `inputJson`; omit both to replay the remembered input) and run to the first stop; optional initial `breakpoints`. With `profiling: true` + `stopAtEntry: false` it returns a constant-size `profile` overview (see below)
- `openl_step_trace` - Step the current frame. `out` (run the frame to its exit so its result is inspectable) + breakpoints is the main move for declarative rules; `into`/`over` are advanced (imperative TBasic/loops). Returns a compact stack (steps for the active frame only); `withValues: true` bundles the active frame's variables so you don't need a separate inspect
- `openl_resume_trace` - Run to the next breakpoint / exception / completion (further than `step out`, which stops at the current frame's exit), waiting inside the call (re-invoke after a timeout to keep waiting)
- `openl_inspect_trace_frame` - Freeze one stack frame: parameters, context, result, sub-step values; for decision tables `decision` (which rule fired, how each condition evaluated) and `ruleNames`; optional A1-keyed cell `highlights` + raw grid. Filter steps with `onlyExecutedSteps` / `excludeStepValues` (e.g. `[1]`) to surface an outlier among neutral factors
- `openl_set_trace_breakpoints` - Read the active breakpoint keys and available targets; `set` replaces the whole set. Key forms: `<name>`, `<uri>`, `<uri>#R{r}C{c}`, `<uri>#rule` (any rule fires), `<uri>#<ruleName>` (specific rule). Append `@N` to any key to break only on the table's N-th execution (0-based) — e.g. `<uri>#R48C0@3`; without it a cell breakpoint hits every pass of a table that runs many times. `N` matches `frames[].instance` and a watch series' `instance`
- `openl_get_trace_value` - Expand a lazy value (`lazy: true` + `parameterId`) from openl_inspect_trace_frame; returns name/description/value only — `withSchema: true` adds the value's (large) JSON Schema
- `openl_expand_trace_tree` - Load one level of a **profiling** run's executed call tree on demand — it comes back lazy, so each step carries a `childrenTotal` count instead of nested `children`. Expand a step whose `childrenTotal` > 0 by its node (`uri` + `instance`) and step `ref`; returns `{ children, total }` where each child is itself shallow (expand again). Page a loop's many sub-calls with `offset`/`limit` (the reply flags `hasMore`/`nextOffset`); a node's `notRetained` counts sub-calls dropped once the tree hit its size limit. Start from the `tree` root (`includeTree: true`)
- `openl_watch_trace_cells` - Watch **scalar** cells (a single number/string factor, e.g. `['$VehiclePriceFactor']`) across a whole run and return one series per cell with its value at every execution of its table — "show me this factor across all coverages" without dumping frames; spot the outlier, then jump straight to that pass with a `<point.ref>@<point.instance>` breakpoint + replay. Do NOT watch a cell whose value is a big aggregate object (a whole spreadsheet result like `$RateCardPremium`) — it makes every point huge; drill into an aggregate with a breakpoint + inspect instead. Captures cells inside lazy result branches too. Each point's value is a ParameterValue (lazy when large — expand with `openl_get_trace_value`); value JSON Schemas are omitted unless `withSchema: true`. The server caps points per series for a cell deep in a combinatorial branch — each series reports `total` (full execution count) and `WatchView.truncated` flags dropped late executions (reach a specific one with a `<ref>@N` breakpoint)
- `openl_stop_trace` - Terminate the session (idempotent; breakpoints survive)

Lifecycle (status values are lowercase): `running ⇄ suspended → completed | error | terminated`.
Stepping and inspection are valid only while `suspended`; on a terminal status read the
final state (structured `error`, profiling `profile`/`tree`) from the stack that the last
start/step/resume call already returned.

Cheapest whole-run overview: `openl_start_trace` with `profiling: true`,
`stopAtEntry: false` and no breakpoints completes in one call and returns `profile` —
a **constant-size** overview: the top-N slowest tables (`hotspots` with
`selfMillis`/`totalMillis`/`count`) plus `nodeCount`/`distinctTables`/`totalMillis`.
It stays small regardless of project size (the call `tree` is omitted by default;
`profileTop` tunes the hotspot count). The `hotspots[].count` counts every table
call, so it is accurate even on a huge run whose tree was truncated, and
`profile.truncated` flags a tree that hit its node cap.
For a profiling overview always pass `inputJson`/`testRanges` with `profiling: true`
and `stopAtEntry: false` **explicitly** — don't rely on replay (omitting the input):
a replay only reproduces the compact profile if the remembered run was itself a
profiling run, otherwise it can return a much larger stack that overflows the limit.
Find the hot or unexpected table in `hotspots`, then restart with a breakpoint on it
(the input is remembered) and inspect live for values. To browse a branch's call
structure, set `includeTree: true` for the **one-level** `tree` root (its steps carry
a `childrenTotal` count, not nested children — a huge run is no longer returned whole)
and walk it level by level with `openl_expand_trace_tree`.

### Deployment (4)
- `openl_list_deploy_repositories` - List deployment repositories
- `openl_list_deployments` - List active deployments
- `openl_deploy_project` - Deploy to production
- `openl_redeploy_project` - Redeploy with new version

### Diagnostics (1)
- `openl_get_version` - Report this server's own identity for bug reports and environment checks: the version of the OpenL Studio it is built into, build id (version + short commit, `.dirty` for a modified working tree), full commit and its date, branch or tag, build timestamp, and the Node.js/platform/architecture it runs on. Local-only — it needs no OpenL Studio connection, so it still answers when the studio is unreachable, and it returns no configuration, credentials, or URLs. Quote `build.id`: snapshot builds of one version share the same version. A `build.source` of `unavailable` means the install shipped without build metadata, so only the version is known. The same identity is printed by `openl-mcp --version`, the stdio startup log, and the HTTP `/health` probe

## Local projects (repository: local)

Projects with `repository: 'local'` are stored on disk without Git; **OPENED/EDITING status is not checked or required** for them — local projects are always considered editable.

**For local, these work:**
- `openl_list_projects` (call without repository filter, follow pagination to completion, then filter by `repository: "local"` in the response; the `repository: "local"` filter may fail because the "local" repository is often not returned by `openl_list_repositories`), `openl_get_project`;
- Table tools: `openl_list_tables`, `openl_get_table`, `openl_update_table`, `openl_append_table`, `openl_create_project_table`, `openl_copy_table`, `openl_delete_table`, dependency/property discovery, and the raw table-source action tools (`openl_insert_table_rows`/`openl_delete_table_rows`/`openl_update_table_cell`/`openl_merge_table_cells`/…);
- Module/sheet discovery and regular table execution: `openl_list_project_modules`, `openl_list_module_sheets`, `openl_run_table`;
- Project deletion: `openl_delete_project` after exact project-name confirmation;
- Test execution and results: `openl_start_project_tests`, `openl_get_test_results_summary`, `openl_get_test_results`, `openl_get_test_results_by_table` (the project is not opened before running tests for local).

**For local, do not use:**
- `openl_open_project`, `openl_save_project`, `openl_close_project` (no commits or status changes);
- Git tools: `openl_list_branches`, `openl_create_project_branch`, `openl_repository_project_revisions`;
- Project branch/merge tools: `openl_list_project_branches`, `openl_check_project_merge`, `openl_merge_project_branches`, read-only merge-conflict inspection, conflict-session cancellation, and `openl_delete_project_branch`;
- `openl_list_project_local_changes`, `openl_restore_project_local_change` (require an opened project; local projects cannot be opened).

Deployment (`openl_deploy_project`, `openl_redeploy_project`) for projects with `repository: 'local'` is typically not used via the studio.

## Prompts (14 Total)

Expert guidance templates for complex OpenL workflows:

1. **local_projects** - Working with projects in repository 'local' (no open/save/close; table/rule/test tools only)
2. **create_rule** - Guide for creating OpenL tables (general overview)
3. **create_rule_decision_tables** - Comprehensive guide for decision tables (Rules, SimpleRules, SmartRules, SimpleLookup, SmartLookup)
4. **create_rule_spreadsheet** - Detailed guide for Spreadsheet tables with formula syntax and JSON structure
5. **create_test** - Guide for creating test tables
6. **update_test** - Guide for modifying tests
7. **run_test** - Test execution workflow
8. **append_table** - Incremental table updates
9. **datatype_vocabulary** - Data structure definitions
10. **dimension_properties** - Context-based rule selection
11. **deploy_project** - Deployment workflow
12. **project_history** - Project audit trail
13. **validate_after_edit** - Post-edit validation workflow (compile state, error surfacing, re-validation)
14. **project_agents_md** - Load and apply a project's AGENTS.md guidance (walk up to repo root; nearest-file-wins)

## Authentication

Authentication is optional — an OpenL Studio in single-user mode accepts
unauthenticated requests. Otherwise a Personal Access Token (PAT) is used. The token
always comes from the client (its `env` for stdio, or the `Authorization` header for
HTTP), never from the server. A PAT is supplied as
`OPENL_PERSONAL_ACCESS_TOKEN` / `--token`, or as `Authorization: Bearer <PAT>` (or
`Token <PAT>`) at `/mcp`; without one, requests are anonymous (single-user Studio).

## MCP transports

- The MCP SDK v2 serves both the modern `2026-07-28` protocol and legacy 2025 clients over stdio and Streamable HTTP.
- Streamable HTTP validates browser `Origin` values against `MCP_ALLOWED_ORIGINS`; requests without `Origin` are treated as non-browser clients. Every approved browser response exposes `Mcp-Session-Id` for legacy clients.
- Every anonymous legacy MCP session owns a distinct `OpenLClient` and Studio cookie jar. Never reuse a credential-less client across MCP sessions.
- Modern HTTP is stateless and constructs a fresh Studio client per request. Studio keeps the state of multi-call workflows (interactive trace, test results, and merge-conflict inspection) for the Personal Access Token a request carries, so they work over modern HTTP with a token. Without a token that state lives in Studio's HTTP session: use stdio or a legacy 2025 HTTP connection.

## Response formatting

- Formats: `json` (default), `markdown`, `markdown_concise`, `markdown_detailed` (the `response_format` argument). JSON is the authoritative, round-trippable representation for agent workflows; request a Markdown format only for human-readable output.
- Binary file reads use a lossless JSON TextContent envelope with base64 `content`, MIME type, total/returned byte counts, and optional range metadata. Do not return arbitrary XLSX/ZIP/octet-stream bytes as embedded resources: clients commonly route every embedded blob to an image decoder and reject valid non-image files. Use `offset`/`length` to page large files.
- List operations return pagination metadata.
- Large responses are truncated at a 25K-character limit — except `openl_get_guides` bodies, which are returned verbatim (sizes are published in the index so callers can budget).

## OpenL-specific behaviour

- **Dual versioning** — Git commits (temporal) and dimension properties (business context).
- **Table types** — Rules, SimpleRules, SmartRules, Lookups, Spreadsheet, Datatype, Method, Test, and others.
- **Project ID formats** — both the current and legacy path formats are handled.

### RawSource-only table content

- The MCP table-content contract is intentionally **RawSource-only**. `openl_get_table`
  always returns the raw cell matrix; create, update, and append accept only
  `tableType: "RawSource"` payloads.
- Never add typed table request/response variants such as `EditableTableView`,
  `AppendTableView`, `SimpleRules`, `Spreadsheet`, `Datatype`, or `Test` DTOs to
  MCP schemas, TypeScript content types, handlers, prompts, or examples. Studio's
  typed views are incomplete and lossy and cannot reliably round-trip workbook
  cells, layout, styles, merged regions, and less common table features.
- A table's semantic kind still appears in list/run/dependency metadata and is
  encoded by the OpenL grid itself. That metadata is not authorization to expose
  a typed content contract.
- Prefer the narrow raw table-source action tools for isolated edits. When a full
  replacement is necessary, call `openl_get_table` without `styles=true` and
  round-trip the complete `source`, preserving blank/covered cells and spans.
- Raw cell values use Studio's round-trip-safe domain everywhere: scalar
  string/number/boolean/null values and representable one-dimensional arrays of
  those scalars. Arrays must contain at least one item; a singleton `[null]`,
  nested arrays, objects, and string elements surrounded by characters Studio
  trims (whitespace or ISO controls) are not representable. MCP validates this
  shape but leaves all context-dependent OpenL
  parsing and workbook serialization to Studio.
- Cell styles are read-only in Studio's table REST API. `styles=true` is useful
  for inspection, but every MCP table write schema rejects `style`; never
  advertise formatting edits unless Studio adds a working write contract.

## External Resources

- [OpenL Studio](https://github.com/openl-tablets/openl-tablets)
- [OpenL Documentation](https://openl-tablets.org/)
- [Model Context Protocol](https://modelcontextprotocol.io/)

## Working in this module

`STUDIO/studio-mcp` holds the sources of the server. Follow the root
[`AGENTS.md`](../../AGENTS.md) for commits, Jira and Markdown; the rules below add to it.

### Build and packaging

- `mvn install -pl STUDIO/studio-mcp` installs Node.js and npm of the root `pom.xml`, runs `npm ci`, `npm run build`
  and `npm run test:coverage`, and packs the server into the non-public `mcp/` folder of the jar: `dist/`, `guides/`,
  `prompts/`, `build-info.json` and the production `node_modules/`. The OpenL Studio war unpacks it into
  `WEB-INF/mcp`, and `McpServerProcess` of `studio-backend` runs `dist/index.js --http` from there.
- Once the bundle is installed, `npm run licenses` (`src/build-licenses.ts`) writes `licenses/mcp-licenses.json` of the
  jar: every production package the bundle ships, with its license file and its NOTICE, for the About dialog of
  OpenL Studio. A package whose license is outside its `--onlyAllow` list fails the build; the list is the one of
  `studio-ui`, and a license added to it needs its public text in `PUBLIC_LICENSES` of `studio-ui`.
- `-Dnpm.build.skip` and `-Dnpm.test.skip` leave out the build and the tests, as for `studio-ui`.
- `npm run build` writes `build-info.json` first — the OpenL Studio version the Maven build passes in `OPENL_VERSION`
  and the git coordinates of this repository — and then the `guides/` bundle from `Docs/` of the same checkout. The
  server reports that version in `serverInfo`, `openl_get_version` and `/health`; the version of `package.json` is a
  placeholder that only a build outside Maven reports.
- Inside OpenL Studio the HTTP transport listens on the loopback interface (`HOST=127.0.0.1`) at the port of
  `mcp.port`, and calls OpenL Studio at `mcp.studio-url` plus the context path (`OPENL_BASE_URL`).
- The package is private: it ships only inside OpenL Studio and has no release line of its own.

### Code quality

- Keep the code clean at all times: no dead code (unused files, exports, functions, variables, or unreachable branches) and no unused dependencies. Remove them as soon as they become orphaned.
- Add a third-party library only when it brings significant benefit — that is, it substantially reduces the code we would otherwise write and maintain. Prefer reimplementing small or simple functionality over taking on a dependency.
- `npm run lint` must report no errors.

### Testing

- Tests must exercise real logic — the behavior a unit computes (transformations, branches, parsing, error paths, edge cases) — not static facts. Asserting the shape or literal value of a declared constant, that a literal equals itself, or a type the compiler already guarantees adds no coverage; don't write such tests.
- Do not duplicate tests: cover each behavior once. Before adding a test, check whether an existing one already exercises that path — if so, strengthen it instead of adding a near-copy.
- A unit test for `src/<module>.ts` lives in `tests/<module>.test.ts`; tests that drive the MCP surface through the client's mocked HTTP layer live under `tests/integration/`. When code is moved or renamed, move or rename its test file in the same change.
- `ITEST/itest.studio/mcp` runs the server inside OpenL Studio behind the Jetty proxy of the Docker image.

### Documentation

- Keep this `AGENTS.md`, the prompt files in `prompts/`, the tool descriptions and the documents under `Docs/` that
  describe the server in step with every change of tools, prompts, configuration or behaviour.
