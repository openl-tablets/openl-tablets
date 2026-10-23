# OpenL MCP Server — Agent Guide

This is the runtime reference for AI agents using the server — its tools and
behaviour — and the conventions for changing it, in
[Working in this module](#working-in-this-module) at the end. OpenL Studio carries
the server in its war and serves it at `/mcp`; the design is
[Built-in MCP Server](../../Docs/architecture/mcp-server.md).

## Overview

The OpenL MCP Server connects AI coding agents (Claude Code, Claude Desktop,
Cursor, VS Code / GitHub Copilot) to the OpenL Studio Business Rules Management
System (BRMS). Through its tools you can:

- **Get oriented** with an onboarding entry point and the OpenL reference docs OpenL Studio serves
- **Discover** repositories, projects, and rules
- **Read** project structure, table definitions, and rule logic
- **Modify** rules, tables, and project files
- **Test** rules and inspect results
- **Debug** rule execution interactively (breakpoints, stepping, live inspection)
- **Deploy** projects, and manage Git-based history

## How it talks to OpenL Studio

The server calls the OpenL Studio REST API only (JSON, optional Personal Access Token);
it opens no WebSocket. Studio's background work is read within one tool call where
Studio answers `202 notReady` until it is done — test results, merges, table runs —
while `openl_project_status` reports a compilation that is still running as `compiling`.

## Tools (76 Total)

All tools are prefixed with `openl_` and share the server's version.

### Guidance Tools (4)
The onboarding and reference-documentation layer. Call `openl_get_started` once per
session before anything else; call `openl_get_project_agent_context` before working on
or creating any project. The documentation tools serve the OpenL Tablets docs **OpenL Studio
serves for its own version** at `/docs`: the specifications of `docs/ref` and the Reference Guide of
`docs/reference-guide` — progressive disclosure: the index is metadata-only, bodies are fetched by id on demand.
- `openl_get_started` - Read-only onboarding bootstrap: the mandatory workflow protocol (load agent context per project, consult guides on demand, edit → validate → save) plus a workspace orientation (which specification/guide categories exist and how to discover more — not an index dump)
- `openl_get_project_agent_context` - Resolve the **AGENTS.md hierarchy** for a project as a **single aggregated markdown document**: walks UP from the project (or an optional `folder`) to the repository root, collects every applicable `AGENTS.md`, and returns them concatenated in one response — ordered from the root folder (lowest priority) down to the project folder (highest priority), later sections winning on conflict. Ends with the ids of the guides the guidance references
- `openl_list_guides` - The canonical index of the docs: **metadata only** (id, type, title, path, size), filterable by `type` ('specification'/'guide') and case-insensitive `search` over id+title, paginated; `path` is the address of the document relative to the address of OpenL Studio
- `openl_get_guides` - Full markdown bodies for 1-5 ids from the index (e.g. `spec/rules.xml`, `guide/introduction/basic-concepts`), verbatim: their links and images are relative to the path of the document; unknown ids fail with an actionable error — it never falls back to the index

### Repository Tools (4)
- `openl_list_repositories` - List all design repositories
- `openl_list_branches` - List Git branches in a repository
- `openl_list_repository_features` - Get repository features, including whether change comments are supported
- `openl_repository_project_revisions` - Get project revision history by stable project ID in the project's current branch

### Project Tools (16)
- `openl_list_projects` - List projects with filters and pagination; follow `has_more` / `next_offset` until `has_more` is false when a complete inventory is required
- `openl_get_project` - Get project details
- `openl_project_status` - Compile lazily when needed and return project compile state and diagnostics (errors/warnings with location); a supplied `branch` is switched to first, an idle project is compiled within `timeoutMs` (30 seconds by default), and a compilation already running or outlasting it returns `compiling` with a note to call again
- `openl_create_project` - Create or copy a project atomically through Studio: omit `template` for a BLANK project, or pass an existing project's exact `projectId` from `openl_list_projects` to copy its full structure and rename it. Both modes accept an optional target `branch` and return the commit revision plus Studio's opaque `projectId` of the new project
- `openl_open_project` - Open project for editing (supports branch/revision switching)
- `openl_save_project` - Save project changes; a comment is required when the repository supports comments
- `openl_close_project` - Close project with save/discard options; saving requires a comment only when supported
- `openl_create_project_branch` - Create new branch
- `openl_list_project_local_changes` - View one module's workspace history using explicit `projectId` and `moduleName`; requires an opened project and does not support repository `local`
- `openl_restore_project_local_change` - Restore one module to a listed local version using explicit `projectId` and `moduleName`; requires an opened project and does not support repository `local`
- `openl_start_project_tests` - Start project test execution
- `openl_get_test_results_summary` - Get brief test execution summary; waits internally through Studio's `202 {"status":"notReady"}` responses until the asynchronous run finishes
- `openl_get_test_results` - Get full test execution results with pagination; waits internally for completed results instead of requiring agent-side polling. A page is cut by whole test tables to the 25K-character limit (`pagination.next_offset` continues it); only `unpaged: true` returns every test table without a limit
- `openl_get_test_results_by_table` - Get the test results of one test table, which Studio filters and pages by the `tableId` of the summary; waits while the run is in progress
- `openl_list_project_modules` - List declared project modules, including path patterns and their matched modules
- `openl_list_module_sheets` - List worksheets in a project module

### Project Branch/Merge/Deletion Tools (10, BETA)

Studio keeps merge conflict state for the client (its credential). After a merge returns
`status: "conflicts"`, inspect it through the same MCP server instance and present
the evidence to the user: the conflicted files, their versions, and, for a workbook,
the conflicting tables and cells. The user decides how each conflict is resolved,
in Studio or by telling the agent; choosing `OURS` or `THEIRS` can discard valid
work, so never choose a side or type a value the user did not decide or approve.
`openl_resolve_merge_conflicts` then resolves every conflicted file as decided and
completes the merge, merging a workbook table by table and cell by cell as the
**Merge table by table** window of Studio does. Use the cancel tool only to clear
pending MCP session state after the user takes over or the merge is abandoned.
`receive` merges the other branch into the project's current branch; `send` merges
the current branch into the other branch.

- `openl_list_project_branches` - List branches with base/protected flags: `scope: "project"` (default) returns branches that hold the project for switching/deletion; `scope: "repository"` returns every repository branch for merge-target discovery, including branches that do not hold the project yet
- `openl_check_project_merge` - Check merge direction, branch relationship, permissions, and blockers without changing Git; `mergeable`/`canMerge` permits an attempt but does not predict conflicts
- `openl_merge_project_branches` - Recheck and attempt the merge, where conflicts are first discovered; Studio runs the merge in the background and the tool waits until it ends; creates session-bound conflict state when needed
- `openl_get_merge_conflicts` - Get grouped conflicted paths, BASE/OURS/THEIRS revisions, and the default message
- `openl_read_merge_conflict_file` - Read a bounded UTF-8 or base64 binary chunk for one BASE/OURS/THEIRS file version
- `openl_get_merge_conflict_workbook` - Read a conflicted Excel file as Studio merges it table by table: without `table`, the sheets either branch changed and the conflicting tables, a table with `byCells: true` listing its conflicting cells (row and column in the merged table, from 0) with what your and their version hold; with `table`, that table's cells in your, the base and their version and in the merged table, without formatting, the merged table naming in `changedCells` the cells Studio took from the one branch that changed them
- `openl_resolve_merge_conflicts` - Resolve every conflicted file of the pending conflict as the user decided and complete the merge: a strategy (`OURS`, `THEIRS`, `BASE`) per file, or workbook `decisions` — a table or a sheet taken whole (`{id, side}`), or a table merged cell by cell (`{id, cells}`: the version of the table and of single cells, and typed values read as Excel reads typing); the tool merges each workbook through Studio and sends it as the `CUSTOM` file, and refuses a call that leaves a conflicted file out
- `openl_cancel_merge_conflicts` - Clear the pending conflict session without modifying files or branches
- `openl_delete_project` - Delete a project after exact current-name confirmation
- `openl_delete_project_branch` - Delete a non-base branch after exact branch confirmation and a safe-delete preflight against the repository base; unmerged commits, unsaved changes, or unverifiable divergence require explicit data-loss confirmation, while protected bypass also requires force confirmation

### Rules/Tables Tools (10)
- `openl_list_tables` - List project tables with pagination; follow `has_more` / `next_offset` until `has_more` is false when a complete inventory is required
- `openl_get_table` - Get the authoritative `RawSource` 2D cell matrix, including context-parsed one-dimensional multi-value arrays; `startRow`/`maxRows` read a large table in row slices of exactly those rows (a merged cell the edge of a slice cuts keeps its value and names its whole area in `mergedArea`, so every row of a long merge is readable) and `styles=true` includes read-only Excel cell styles. Every answer states its rows in `window` next to `data`: `startRow`, `endRow`, `rows`, `totalRows`, `complete` (whether it holds every row) and `nextStartRow` while rows remain. A sliced response carries `totalRows` and is for reading or narrow raw actions only
- `openl_update_table` - Replace the complete `RawSource` matrix while preserving or adding rows, including scalar and representable one-dimensional array values returned by `openl_get_table`; before writing, it reads one live row and rejects a shorter source even if `totalRows` was removed, preventing a sliced view from deleting unseen rows. Remove rows with `openl_delete_table_rows`. Call `openl_get_table` without `styles=true`: Studio write APIs cannot change formatting, and `style` is rejected rather than silently ignored
- `openl_append_table` - Append full-width `RawSource` rows with scalar or representable one-dimensional array cell values
- `openl_create_project_table` - Create a table from a complete `RawSource` matrix in an existing module, or pass `modulePath` (an `.xlsx` project-relative path) to create a new module; cell values may be scalars or representable one-dimensional arrays, while cell formatting is unsupported by Studio write APIs
- `openl_delete_table` - Delete an entire table (to remove a row/column WITHIN a table, use the raw action tools below)
- `openl_run_table` - Execute a regular (non-Test) table with JSON input and wait for its result, treating Studio's `202 {"status":"notReady"}` result response as still running; provide parameters by name, directly or under `params`, or in order, under `params` or as a top-level array; for a method whose only parameter is an array, a top-level array is that parameter's value. `timeoutMs` bounds the complete start-and-result workflow (default 2 minutes), and cancellation, timeout, or any other failed workflow clears the pending Studio run. Studio keeps only one run per client, so concurrent calls through the same MCP session are rejected rather than allowed to replace each other's result
- `openl_get_table_dependencies` - Get the executable and datatype dependency graph: omit `tableId` for a whole-project/module view with optional `layer` (`executable`/`datatype`/`all`), or provide it for a table's dependency/dependent neighborhood. JSON returns the adjacency list, including bounded first/last value previews for vocabularies; Markdown visualizes executable calls as a Mermaid flowchart and the data model with declared fields, `Name<Type>` vocabulary headers, bounded values (`+ N more` when truncated), and reference cardinalities as a Mermaid ER diagram, plus a separate inheritance diagram when needed
- `openl_list_table_property_definitions` - List properties allowed in a table context, including types and enum values
- `openl_copy_table` - Copy a table server-side inside the project while preserving formatting, merged cells, comments, and structure

### Raw Table-Source Action Tools (12)
In-place edits to a table's raw source (any table type). One tool per operation×orientation handles **one OR more** rows/columns — pass a single row/column or several; the studio takes a single `rows`/`columns` block target (one row/column is just a one-element block), so there is no separate "row" vs "rows" tool. Positions are 0-based (row 0 is the header, column 0 the leading labels). `cells` is required and non-empty (one cell per column/row; use `{ value: null }` for a blank cell, and `{ covered: true }` for a position an existing merged cell covers, such as row 0 under a title that spans the table — Studio refuses to write it). Writable values use the same round-trip contract as full-table writes: string/number/boolean/null scalars or one-dimensional arrays of those scalars. Arrays must be non-empty; `[null]`, nested arrays, objects, and string elements surrounded by characters Studio trims (whitespace or ISO controls) are rejected. Each tool returns the table's CURRENT `tableId` (plus `previousTableId` when the edit renamed the table) and reads the table back to trigger a recompile; a table that does not compile after the edit is answered with `compiles: false` and the `errors` of its compilation.

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
- `openl_read_project_file` - Read a file (text verbatim; arbitrary binary as lossless base64 `content` in a JSON TextContent envelope with MIME/byte metadata; optional `offset`/`length` byte range, whose text chunk arrives in the same envelope with `encoding: "utf-8"`, `nextOffset` while the file goes on and `lineEnding: "CRLF"` when its lines end with CR LF), read file metadata (`view: "meta"`), or list a folder (`recursive`, `viewMode` FLAT/NESTED, `extensions`, `namePattern`, `foldersOnly`); optional `version` reads a historical revision
- `openl_write_project_file` - Create/replace a file from UTF-8 `content` or a base64 `blob` advertised with JSON Schema `contentEncoding: "base64"`; the legacy base64 `content` + `encoding` form remains accepted, including whitespace-wrapped base64; `createFolders` (default true), `conflictPolicy` FAIL/OVERWRITE/SKIP; OVERWRITE of a text file whose lines end with CRLF by `content` whose lines end with LF writes CRLF, so a read and write back leaves the file unchanged
- `openl_edit_project_file` - Change part of a UTF-8 text file without sending it whole: each of `edits` replaces an `oldText` found exactly once with `newText` (an empty `oldText` appends); every other byte stays as it is, LF line ends match and are written as CRLF in a CRLF file, and `message` commits. Binary and non-UTF-8 files are refused
- `openl_delete_project_file` - Delete a file/folder (auto-cleans dangling config references)
- `openl_search_project_files` - Search by glob `pattern`, `extensions`, `type`, or case-insensitive `content` substring; `scope` SUBTREE (default) or ANCESTORS. Studio searches `content` only inside text files—not XLSX/XLS/ZIP/images; locate binary files by path/extension and read them separately
- `openl_copy_project_file` - Copy a file within the project (no overwrite — destination collision returns 409)
- `openl_move_project_file` - Move or rename a file within the project

### Trace Tools (9, BETA)
An **interactive debugger** for rules: the rule runs on a server-side worker that
suspends at breakpoints and step points, and the tools inspect that live, suspended
execution. Studio keeps the debug session for the client — its Personal Access Token,
or the one user in single-user mode — so every call of the flow that comes with it
finds the session. One active session per user (a new start terminates the
previous); idle sessions are reaped after ~10 minutes.

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
- `openl_get_server_info` - Report the OpenL Studio the server works with, as Markdown: `Server: OpenL Studio <version>` and `URL: <address>` — the address its users open, restored from the `X-Forwarded-*` headers or the first `Forwarded` element of the request, its `Host` otherwise, with the context path of Studio. `full: true` adds the build number, the start and the current time, OS, architecture, Java, CPUs and their load, threads, and the memory of the machine and of the JVM, from `/rest/public/info/build.json` and `/rest/public/info/sys.json`. The `path` of a document from `openl_list_guides` is relative to that address

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

## Authentication

Authentication is optional — an OpenL Studio in single-user mode accepts
unauthenticated requests. Otherwise OpenL Studio checks a request to `/mcp` as it checks
a REST call: the client sends a Personal Access Token (PAT) of its user as
`Authorization: Token <PAT>`, or in the oauth2 mode an access token of the identity
provider as `Bearer`, and the server calls Studio with the same header; the server holds
no token of its own. Without one, requests are
anonymous (single-user Studio).

## MCP transports

- The server speaks the Streamable HTTP transport at `/mcp`: the modern `2026-07-28` protocol, and the sessionful protocol of legacy 2025 clients.
- `McpProxyServlet` of OpenL Studio passes each request on to the server once the security of Studio has checked it, and tells the server where to call Studio in the `OpenL-Base-Url` header and the address of Studio the client opened in the `OpenL-Public-Url` header.
- Browser origins are a matter of OpenL Studio: its `cors.allowed.*` settings answer `/mcp` as `/rest`, and `cors.exposed.headers` exposes `Mcp-Session-Id` to an allowed origin for legacy clients. The server checks no `Origin`.
- Every legacy MCP session owns a distinct `OpenLClient`. Never reuse a client across MCP sessions.
- Most legacy clients never close their session, so opening a session closes the others no request has used for
  `LEGACY_SESSION_IDLE_MS`, the 30-minute HTTP session timeout of Studio. A session with a request in progress, its
  stream of server messages included, is in use.
- Modern HTTP is stateless and constructs a fresh Studio client per request. Studio keeps the state of multi-call workflows (interactive trace, test results, and merge-conflict inspection) for the Personal Access Token a request carries, and for the one user in single-user mode, so they work over modern HTTP.

## Response formatting

- Formats: `json` (default), `markdown`, `markdown_concise`, `markdown_detailed` (the `response_format` argument). JSON is the authoritative, round-trippable representation for agent workflows; request a Markdown format only for human-readable output.
- Binary file reads use a lossless JSON TextContent envelope with base64 `content`, MIME type, total/returned byte counts, and optional range metadata. Do not return arbitrary XLSX/ZIP/octet-stream bytes as embedded resources: clients commonly route every embedded blob to an image decoder and reject valid non-image files. Use `offset`/`length` to page large files.
- List operations return pagination metadata.
- Large responses are truncated at a 25K-character limit — except `openl_get_guides` bodies, which are returned verbatim (sizes are published in the index so callers can budget). A response over the limit is first written without indentation; when it still does not fit, it is cut by whole items (a list by items with the pagination moved to match, `openl_get_table` by rows with `totalRows` and the `startRow` to continue from), and the note names only a way to continue that the tool has.

## OpenL-specific behaviour

- **Dual versioning** — Git commits (temporal) and dimension properties (business context).
- **Table types** — Rules, SimpleRules, SmartRules, Lookups, Spreadsheet, Datatype, Method, Test, and others.
- **Project ids** — opaque 12-character ids OpenL Studio answers; a project keeps its id while it stays in its folder. Never build or decode one.

### RawSource-only table content

- The MCP table-content contract is intentionally **RawSource-only**. `openl_get_table`
  always returns the raw cell matrix; create, update, and append accept only
  `tableType: "RawSource"` payloads.
- Never add typed table request/response variants such as `EditableTableView`,
  `AppendTableView`, `SimpleRules`, `Spreadsheet`, `Datatype`, or `Test` DTOs to
  MCP schemas, TypeScript content types, handlers, or examples. Studio's
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
  trims (whitespace or ISO controls) are not representable. MCP checks the shape
  of a scalar or a flat array; Studio refuses the rest and does all context-dependent
  OpenL parsing and workbook serialization.
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
[MCP Server Internals](../../Docs/architecture/mcp-server-internals.md) maps the sources, the path of a request to
OpenL Studio and where the server keeps its state; the
[OpenL Studio MCP Server Guide](../../Docs/user-guides/mcp-server/index.md) is what the users read.

### Build and packaging

- `mvn install -pl STUDIO/studio-mcp` installs Node.js and npm of the root `pom.xml`, runs `npm install`, `npm run build`
  and `npm test`, and packs the server into the non-public `mcp/` folder of the jar: `index.mjs`, an ES module
  Node.js runs without a `package.json`. The OpenL Studio war unpacks it into `WEB-INF/mcp`, and `McpServerProcess`
  of `studio-backend` runs `WEB-INF/mcp/index.mjs`.
- `npm run build` type-checks `src`, the tests included, with `tsc`, which emits nothing, and Rolldown
  (`rolldown.config.js`) bundles `src/index.ts` with the code it takes of the production dependencies into one
  minified `dist/index.mjs`. The name, version and description of `package.json` go into the bundle.
- The `licenses` plugin of the bundler (`src/build-licenses.ts`) writes `dist/mcp-licenses.json`, which the jar
  carries as `licenses/mcp-licenses.json`: every package the bundle takes code of, with its license file and its
  NOTICE, for the About dialog of OpenL Studio. A package whose license is outside the `--onlyAllow` list of the
  `build` script of `studio-ui`, which the plugin reads, fails the build; a license added to that list needs its
  public text in `PUBLIC_LICENSES` of `studio-ui`.
- `-Dnpm.build.skip` and `-Dnpm.test.skip` leave out the build and the tests, as for `studio-ui`.
- The server reports the version of the OpenL Studio that runs it, which `McpServerProcess` passes in `OPENL_VERSION`,
  in `serverInfo` and in its startup log line; the version of `package.json` is a placeholder that only a server
  outside Studio reports.
- Inside OpenL Studio the HTTP transport listens on the loopback interface (`HOST=127.0.0.1`) at a free port
  (`PORT=0`), and calls OpenL Studio at the address `McpProxyServlet` passes with every request in `OpenL-Base-Url`.
  Studio reads the port from the end of the line the server logs once it listens, `listening on <host>, port <port>`:
  keep it.
- The package is private: it ships only inside OpenL Studio and has no release line of its own.

### Running and debugging

- `npm test` runs every test and reports the coverage; `npm test -- src/<file>.test.ts` runs one file, and
  `-t "<name>"` one test. `npm run lint` covers `src`, the tests included.
- Against a running OpenL Studio, outside Studio:
  `npm run build && HOST=127.0.0.1 PORT=3001 node dist/index.mjs`, then connect a client to
  `http://127.0.0.1:3001/mcp` with the header `OpenL-Base-Url: http://localhost:8080`. Inside OpenL Studio, every
  other variable of the environment of Studio reaches the server.
- A browser-based inspector needs its origin in `cors.allowed.origins` of OpenL Studio.

### Code quality

- Keep the code clean at all times: no dead code (unused files, exports, functions, variables, or unreachable branches) and no unused dependencies. Remove them as soon as they become orphaned.
- Add a third-party library only when it brings significant benefit — that is, it substantially reduces the code we would otherwise write and maintain. Prefer reimplementing small or simple functionality over taking on a dependency.
- `npm run lint` must report no errors.
- The code is ECMAScript 2025, which every Node.js 24 runs (`engines` of `package.json`): use its built-ins —
  `replaceAll`, `at`, `Object.groupBy`, iterator helpers, `Set` methods, `Promise.withResolvers` — rather than
  hand-written equivalents.
- Every tool schema is a `z.strictObject`: an unknown argument, also a nested one, is refused, not ignored.
- A mistake of the caller throws `ProtocolError` with `InvalidParams` or `InvalidRequest`; any other `Error` reaches
  the agent as an internal error.
- Long work waits inside one call — `waitForResult` of `handlers/wait.ts` for Studio's `202 notReady` — honours the
  `AbortSignal` of the call and sends progress notifications. The server opens no WebSocket: only
  `openl_project_status` may answer `compiling` and ask the agent to call again.
- Page a list Studio pages with `paginateCollection`; `formatPage` pages and formats an array the server holds itself.

### Testing

- Tests must exercise real logic — the behavior a unit computes (transformations, branches, parsing, error paths, edge cases) — not static facts. Asserting the shape or literal value of a declared constant, that a literal equals itself, or a type the compiler already guarantees adds no coverage; don't write such tests.
- Do not duplicate tests: cover each behavior once. Before adding a test, check whether an existing one already exercises that path — if so, strengthen it instead of adding a near-copy.
- Tests sit beside their sources, as in `studio-ui`: the test of `src/<module>.ts` is `src/<module>.test.ts`, and the
  tests of the tools of `src/handlers/<module>.ts`, which drive them through the client's mocked HTTP layer, are
  `src/handlers/<module>.test.ts`. The helpers the tests share are in `src/testing/`. When code is moved or renamed,
  move or rename its test file in the same change.
- A tool a handler passes through to the client unchanged is tested once, through `executeTool`; a unit test of the
  client covers what the client itself computes.
- The seams: `createHttpApp({ clientFactory, baseUrl })` with the SDK `Client` for the transport
  (`src/http-server.test.ts`), and `MockHttp` of `src/testing/mock-http.ts` on the client for a tool.
  `useToolHarness()` of `src/testing/harness.ts` registers the tools and gives every test a new client and mock;
  `textOf` and `dataOf` read the answer of a tool. `src/testing/user-guides-fixture.ts` answers the user guides of
  `/docs`, and `src/testing/openl-api-mocks.ts` holds the shared answers of Studio.
- Read the requests a test sent from `mockHttp.history` (`bodyOf` parses a JSON body) instead of capturing them in a
  reply callback; check that no request was sent by the length of the history.
- `tsc` of `npm run build` checks the tests as it checks the rest of `src/`: a type error in a test fails the build.
- `ITEST/itest.studio/mcp` runs the server inside OpenL Studio, behind its security and its `/mcp` servlet.

### Documentation

- Keep this `AGENTS.md`, the tool descriptions and the documents under `Docs/` that describe the server in step with
  every change of tools, configuration or behaviour. The server offers no MCP prompts: guidance for agents lives in
  the tool descriptions, `openl_get_started` and the guides OpenL Studio serves.
