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
- **Follow OpenL skills** — step-by-step workflows for planning, branching, testing, versioning, tracing and change logs
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

Studio answers from what it compiled, and compiles nothing again by itself when another program changes the files
of a project: a workbook edited, added or taken away, or `rules.xml`. A tool that works with what was compiled
declares it in `registerTool` (`compiled: true`), and `executeTool` asks the short status
(`GET /projects/{projectId}` with `STATUS_SUMMARY_QUERY` of `client.ts`, the compile state and the change on disk
alone, which Studio builds cheaply) before it: when the files changed, the project is compiled again first, the
tool waits until the compilation ends, and it goes on with the files as they are. An edit of a table that changed since the agent read it is refused by the
version of the table it names, which the agent reads again. The agent never has to ask for it. A new tool reading
tables, running rules or editing them declares `compiled`.

## Tools (62 Total)

All tools are prefixed with `openl_` and share the server's version.

### Guidance Tools (4)
The onboarding and reference-documentation layer. Call `openl_get_started` once per
session before anything else; call `openl_get_project_agent_context` before working on
or creating any project. The documentation tools serve the OpenL Tablets docs **OpenL Studio
serves for its own version** at `/docs`: the specifications of `docs/ref` and the Reference Guide of
`docs/reference-guide` — progressive disclosure: the index is metadata-only, bodies are fetched by id on demand.
- `openl_get_started` - Read-only onboarding bootstrap: the mandatory workflow protocol (load agent context per project, consult guides on demand, edit → validate → save), the OpenL skills with their `skill://` URIs and descriptions, plus a workspace orientation (which specification/guide categories exist and how to discover more — not an index dump)
- `openl_get_project_agent_context` - Resolve the **AGENTS.md hierarchy** for a project as a **single aggregated markdown document**: walks UP from the project (or an optional `folder`) to the repository root, collects every applicable `AGENTS.md`, and returns them concatenated in one response — ordered from the root folder (lowest priority) down to the project folder (highest priority), later sections winning on conflict. Ends with the ids of the guides the guidance references. The document is never cut to the size limit of an answer: no argument could read a part left out
- `openl_list_guides` - The canonical index of the docs: **metadata only** (id, title, size), filterable by `type` ('specification'/'guide') and case-insensitive `search` over id+title, paginated; the id starts with its type, and `openl_get_guides` names the path
- `openl_get_guides` - Full markdown bodies for 1-5 ids from the index (e.g. `spec/rules.xml`, `guide/introduction/basic-concepts`), verbatim: their links and images are relative to the path of the document; unknown ids fail with an actionable error — it never falls back to the index

### Repository Tools (3)
- `openl_list_repositories` - List the design repositories with the features each supports (branches, comments, search, mapped folders), in JSON and in either Markdown view
- `openl_list_branches` - List Git branches in a repository
- `openl_repository_project_revisions` - Get project revision history by stable project ID in the project's current branch

### Project Tools (14)
- `openl_list_projects` - List projects as a table of `projectId`, name, repository, branch, status and last change, with filters and pagination; the first line names the offset of the next page while more follow. Each project names its id once, as `projectId`, the name the other tools take it by, and leaves its capabilities to `openl_get_project`. `include: status` adds a `compileState` column, and the summary counts of `include: summary` follow the table; a `compileState` of `idle` means the project is not compiled yet, not that it compiles; `openl_project_status` compiles it
- `openl_get_project` - Get project details; the project names its id as `projectId`, as in `openl_list_projects`. Its `capabilities`, which `openl_list_projects` leaves out, name the operations the project takes, such as `["canOpen", "canSave"]`, a quarter shorter than `true` flags. Its `compileStatus` of `include: status`, as in `openl_list_projects`, leaves out the id, the branch, the revision and the last change the project names already; a value that differs stays
- `openl_project_status` - Compile lazily when needed and return project compile state and diagnostics (errors/warnings with location); a supplied `branch` is switched to first, an idle project is compiled within `timeoutMs` (30 seconds by default), and a compilation already running or outlasting it returns `compiling` with a note to call again; a project whose files another program changed since Studio compiled them is compiled again from those files, with a note to read its tables again — every tool that reads or edits what Studio compiled does the same by itself
- `openl_create_project` - Create or copy a project atomically through Studio: omit `template` for a BLANK project, or pass an existing project's exact `projectId` from `openl_list_projects` to copy its full structure and rename it. Both modes accept an optional target `branch` and answer the `projectId`, `branch` and `revision` of the new project
- `openl_open_project` - Open project for editing (supports branch/revision switching); a project open already is left as it is when neither is asked
- `openl_save_project` - Save project changes; a comment is required when the repository supports comments. A comment has at most 255 characters, in every tool that takes one for a commit of a design project: `openl_close_project` when it saves, and the optional comments of `openl_create_project` and `openl_delete_project`
- `openl_close_project` - Close project with save/discard options; saving requires a comment only when supported
- `openl_create_project_branch` - Create new branch
- `openl_list_project_local_changes` - View one module's workspace history, each version by the `historyId` the restore takes, using explicit `projectId` and `module`; requires an opened project and does not support repository `local`. It is no check for unsaved changes: `openl_project_status` reports the pending changes
- `openl_restore_project_local_change` - Restore one module to a listed local version using explicit `projectId` and `module`; requires an opened project and does not support repository `local`. The whole module takes the version, every later edit of it undone too, and the answer names as `undo` the version of the module right before, which restores it back, or in a `note` that the history could not be read after the restore
- `openl_start_project_tests` - Start project test execution; answers that the tests `started`, and `projectOpened` when it opened a closed project for them
- `openl_get_test_results` - Read the results of the run as Markdown tables, waiting internally through Studio's `202 {"status":"notReady"}` responses until it finishes: a line of the counts and the time of the run, a table of the test tables with their tests and failures, and a table of the tests, one row per failed assertion with its expected and actual value and the inputs of the test, an input with inner structure named by its type and key (Studio's `lazyValues`). `failuresOnly`, true by default, lists only the failed tests, at most `failures` (5) per test table, and names how many a test table leaves out; `false` lists every test, a passed one in a row of its own. `summary: true` answers the line of counts alone; `tableId` reads the tests of one test table, which the server pages by `offset` and `limit`, and `caseId` with it one test with its inputs as JSON without indentation, whole up to half the response limit and otherwise cut in their structure with the cuts named. A page is cut to the 25K-character limit by whole test tables, or by whole tests of one test table, and its first lines name the offset of the next page; only `unpaged: true` returns every test table without a limit, an answer that can exceed what the client accepts. One tool reads all three views of Studio's `/tests/summary`, and one test from `/tests/summary/{tableId}/cases/{caseId}`, so their shared options are described once
- `openl_list_project_modules` - List declared project modules, including path patterns and their matched modules
- `openl_list_module_sheets` - List worksheets in a project module

### Project Branch/Merge/Deletion Tools (11, BETA)

Studio keeps merge conflict state for the client (its credential). After a merge returns
`status: "conflicts"`, inspect it through the same MCP server instance and present
the evidence to the user: the conflicted files, their versions, for a text file the
conflicting lines, and for a workbook the conflicting tables and cells. The user
decides how each conflict is resolved, in Studio or by telling the agent; choosing
`OURS` or `THEIRS` can discard valid work, so never choose a side or type lines or
a value the user did not decide or approve. `openl_resolve_merge_conflicts` then
resolves every conflicted file as decided and completes the merge, merging a text
file line by line and a workbook table by table and cell by cell as the **Merge line
by line** and **Merge table by table** windows of Studio do. Use the cancel tool only
to clear pending MCP session state, and stop a merge still running, after the user takes over or the merge is abandoned.
`receive` merges the other branch into the project's current branch; `send` merges
the current branch into the other branch.

- `openl_list_project_branches` - List branches with base/protected flags: `scope: "project"` (default) returns branches that hold the project for switching/deletion; `scope: "repository"` returns every repository branch for merge-target discovery, including branches that do not hold the project yet. The base branch leads, then the protected ones; `name` filters by a fragment, and `limit` and `offset` page the list, which counts hundreds of branches in a shared repository
- `openl_check_project_merge` - Check merge direction, branch relationship, permissions, and blockers without changing Git; `mergeable`/`canMerge` permits an attempt but does not predict conflicts; `scope` tells what the merge would bring in: every project the source branch changed, and the files both branches changed
- `openl_merge_project_branches` - Recheck and attempt the merge, where conflicts are first discovered; Studio runs the merge in the background and the tool waits until it ends; the merge brings in every project the source branch changed, which `summary` of a success lists with their files; creates session-bound conflict state when needed. `summary.after` is the new revision of the project on the target branch
- `openl_get_merge_conflicts` - Get grouped conflicted paths, which versions hold each file and whether it is UTF-8 text (`fileAvailability`), BASE/OURS/THEIRS revisions, and the default message
- `openl_read_merge_conflict_file` - Read one BASE/OURS/THEIRS file version: a text of up to 5,000 bytes inline, any other version by a download link; a version `fileAvailability` marks `false`, such as the base of a file both branches added, does not hold the file and is refused
- `openl_get_merge_conflict_workbook` - Read a conflicted Excel file that your, the base and their version all hold as Studio merges it table by table (a workbook both branches added has no base version and is resolved whole, with a strategy): without `table`, the sheets either branch changed and the conflicting tables, a table with `byCells: true` listing its conflicting cells (row and column in the merged table, from 0) with what your and their version hold; with `table`, that table's cells in your, the base and their version and in the merged table, without formatting, the merged table naming in `changedCells` the cells Studio took from the one branch that changed them
- `openl_get_merge_conflict_text` - Read a conflicted UTF-8 text file that your and their version hold as Studio merges it line by line: the parts of the file in order, the lines the merge took by itself (a run of more than 8 lines as its first and last 3, `head` and `tail`, with `omittedLines` between) and each conflict, numbered from 0, with every line your, the base and their version have in its place; an answer over the size limit holds the leading blocks and continues with `fromConflict`
- `openl_resolve_merge_conflicts` - Resolve every conflicted file of the pending conflict as the user decided and complete the merge: a strategy (`OURS`, `THEIRS`, `BASE`) per file — a version `fileAvailability` marks `false` deletes the file —, text `conflicts` — one decision per conflict of `openl_get_merge_conflict_text`, in their order: a version (`{side}` of `ours`, `theirs`, `base`), both versions whole (`ours-theirs`, `theirs-ours`) or lines written in its place (`{lines}`), put in the line breaks of the file — or workbook `decisions` — a table or a sheet taken whole (`{id, side}`, losing the cells only the other branch changed), or a table merged cell by cell (`{id, cells}`: the version of the table and of single cells, and typed values read as Excel reads typing); the tool merges each text file and each workbook as decided and sends it as the `CUSTOM` file, and refuses a call that leaves a conflicted file out, or that merges a file into nothing, which Studio cannot store
- `openl_cancel_merge_conflicts` - Clear the pending conflict session, and stop a merge Studio still runs, without modifying files or branches
- `openl_delete_project` - Delete a project after exact current-name confirmation, on its current branch only, which it answers as `deletedOnBranch`: another branch that holds the project keeps it
- `openl_delete_project_branch` - Delete a non-base branch after exact branch confirmation and a safe-delete preflight against the repository base, run while the project is on the branch or on the base branch; unmerged commits, unsaved changes, or unverifiable divergence require explicit data-loss confirmation, while protected bypass also requires force confirmation

### Rules/Tables Tools (9)
- `openl_list_tables` - List project tables as a table of id, name, kind and module, with pagination; the first line names the offset of the next page while more follow. Filters: `kind`, `name`, `properties`, `module`, `header` (the text of the header line) and `text` (the text of any cell, the only search that reads workbook cells), with `scope` `module`/`project`/`all` and `sort` `name`/`position`. A table does not repeat the project the caller named; one of a dependency, which `scope: all` finds, names its project
- `openl_get_table` - Read a table, whatever its type, in the one shape the table tools read and write (`table-grid.ts`): `rows` of plain values, one per column — a scalar, a one-dimensional array of scalars, or null for an empty cell or one a merged cell covers, a row ending at its last value — `merges` as `[row, column, rowspan, colspan]`, `formulas` as `[row, column, "=B2*C2"]` beside the values they compute, `notes` (Excel notes) and, with `styles=true`, `styles`, each by the position of its cell. A position counts from 0 in the whole table, also in a slice, and is the one `openl_edit_table` takes; row 0, column 0 is the first cell of `pos`. Studio's matrix of cell objects spent most of a read on wrapping each value, and a merged header of a wide test table on 102 `{"covered":true}`: the same table now reads in about half the tokens. `offset`/`limit` read a large table in row slices of exactly those rows (a merged cell the edge of a slice cuts keeps its value in the first row of the slice, and `merges` names its whole area). `find` reads only the rows with a cell whose text contains it, case-insensitive (a number by its digits), and the header rows, the position of each in `rowIndexes`: the key of one row of a 919-row lookup without three pages of reads. Every other answer states its rows in `window` next to `data`: `offset`, `rows`, `totalRows`, `complete` (whether it holds every row) and `next_offset` while rows remain. The answer carries the table's `version`, which the edit tools take back
- `openl_update_table` - Replace a whole table with `rows`, `merges` and `formulas` as `openl_get_table` answered them, changed, and the `version` of the read, sent in `If-Match`, so Studio refuses the write when the table changed since. Before writing it reads one live row and refuses fewer rows than the table has, so a slice cannot delete the rows it left out; remove rows with the `delete_rows` action of `openl_edit_table`. A row may end before the last column (the cells left out are written empty); notes and styles are kept, as Studio keeps them
- `openl_create_project_table` - Create a table named `name` from `rows`, `merges` and `formulas` in an existing `module`, or pass `modulePath` (an `.xlsx` project-relative path) to create a new module; cell formatting is unsupported by Studio write APIs
- `openl_delete_table` - Delete an entire table (to remove a row/column WITHIN a table, use `openl_edit_table` below); the required `version` is sent in `If-Match`
- `openl_run_table` - Execute a regular (non-Test) table with JSON input and wait for its result, treating Studio's `202 {"status":"notReady"}` result response as still running; provide parameters by name, directly or under `params`, or in order, under `params` or as a top-level array, any of them also as a JSON string; for a method whose only parameter is an array, a top-level array is that parameter's value. `withSchema: true`, with `inputJson: {}` when no input is known yet, answers the JSON Schema of each parameter: its fields and the values each vocabulary allows, to build an input from. `timeoutMs` bounds the complete start-and-result workflow (default 2 minutes), and cancellation, timeout, or any other failed workflow clears the pending Studio run. Studio keeps only one run per client, so concurrent calls through the same MCP session are rejected rather than allowed to replace each other's result
- `openl_get_table_dependencies` - Get the executable and datatype dependency graph: omit `tableId` for a whole-project/module view with optional `layer` (`executable`/`datatype`/`all`), or provide it for a table's dependency/dependent neighborhood. JSON returns the adjacency list, including bounded first/last value previews for vocabularies; Markdown visualizes executable calls as a Mermaid flowchart and the data model with declared fields, `Name<Type>` vocabulary headers, bounded values (`+ N more` when truncated), and reference cardinalities as a Mermaid ER diagram, plus a separate inheritance diagram when needed
- `openl_list_table_property_definitions` - List properties allowed in a table context, including types and enum values
- `openl_copy_table` - Copy a table server-side inside the project while preserving formatting, merged cells, comments, and structure

### Raw Table-Source Edit Tool (1)
In-place edits to a table's raw source (any table type), one tool for every edit Studio takes at
`POST /projects/{projectId}/tables/{tableId}/actions`. The `action` names the edit, and each action takes only its
own arguments — one it needs and left out, or one it does not take, is refused before Studio is called. One tool
instead of one per edit keeps the rules the edits share in one description, which every agent loads in every session.

- `openl_edit_table` - `append_rows` / `append_columns` add the lines of `cells` at the end; `insert_rows` /
  `insert_columns` add them at `position` 1..; `delete_rows` / `delete_columns` remove `count` (default 1) from
  `position` 1.. (the header row and the label column 0 stay); `update_row` / `update_column` overwrite the line at
  `position` with the one line of `cells`; `update_cell` sets the cell at (`row`, `column`) to `value` (null clears
  it), or with `formula` instead to an Excel formula; `update_range` overwrites more than one cell from (`row`, `column`); `merge` merges `rowspan`×`colspan`
  cells from (`row`, `column`), `unmerge` splits the merged cell covering it

Positions are 0-based in the whole table, as `openl_get_table` gives them (row 0 is the header, column 0 the leading
labels). `cells` is always a list of lines (rows for row actions and a range, columns for column actions), each a
non-empty list of values as the `rows` of `openl_get_table` hold them: null for an empty cell or a position an
existing merged cell covers, which stays covered. A value starting with `=` is an OpenL formula, written as text;
`update_cell` writes an Excel `formula`, such as `=B2*C2`, instead of a value, and Studio refuses the two together.
Writable values use the same round-trip contract as full-table writes:
string/number/boolean/null scalars or one-dimensional arrays of those scalars. Arrays must be non-empty; `[null]`,
nested arrays, objects, and string elements surrounded by characters Studio trims (whitespace or ISO controls) are
rejected. The tool returns the table's CURRENT `tableId` (plus `previousTableId` when the edit renamed the table) and
its new `version`, and reads the table back to trigger a recompile; a table that does not compile after the edit is
answered with `compiles: false` and the `errors` of its compilation, each led by the A1 cell it points to. The `version` — from `openl_get_table` or the
previous edit — is required and sent in `If-Match`: Studio refuses an edit of a table that changed since, in Excel,
on disk or through another client, and writes nothing.

### Project Files Tools (7, BETA)
Operate on ANY file in a project by exact project-relative path (not just Excel rule files). Writes/uploads/deletes/copies/moves land in the **working copy** of an opened project — commit them with `openl_save_project` — while Studio commits them at once to a closed project, and a project edited in place takes them in its files at once, with nothing to commit; a file write answers `committed: true` for a closed project. Use the optional `branch` to pin the project's branch (omit for `local`/non-branch repositories). No file content travels as base64: a text file of up to 5,000 bytes is read inline, and every other file goes by a **link** the agent runs `curl` with, so its bytes never enter the conversation ([MCP File Transfer Links](../../Docs/architecture/mcp-file-transfer.md)). Where Studio authenticates, the link is a transfer link of Studio — no credential in it, 15 minutes by default, an upload one used once; in single-user mode it is the address of the REST API. A link answer carries the URL, the expiry, the size when known, where the file lives in its repository (`repositoryId`, `repositoryPath`, `branch`) and a ready `curl` command.
- `openl_read_project_file` - Read a file: a text of up to 5,000 bytes — `text/*`, JSON, XML or YAML as Studio's `Content-Type` says — inline and verbatim, its byte order mark kept; a larger text, a text with bytes its charset does not decode, a binary file, or a folder as a ZIP archive, by a download link; optional `revision` reads the file at a commit, an answer then without a `version`. A path no file takes is refused with the hint that a folder path ends with `/`. The answer tells the file's current `version` — in a text block of its own beside an inline text, in the link JSON otherwise — which a replacement or a deletion of the file names back
- `openl_write_project_file` - Create a TEXT file from UTF-8 `content`, `createFolders` (default true), writing nothing over a file already at the path; or replace a file by the `version` `openl_read_project_file` answered, sent in `If-Match`, so Studio writes nothing over a file changed since. A replacement of a text file whose lines end with CRLF by `content` whose lines end with LF writes CRLF, so a read and write back leaves the file unchanged; a file on disk goes by `openl_create_upload_link` instead
- `openl_create_upload_link` - A link that writes one file from disk into the working copy: the target (`path`, `createFolders`, `branch`, and the `version` of a file it replaces) and optionally the `size` and `sha256` Studio checks before it writes anything; it creates or replaces a file as `openl_write_project_file` does
- `openl_delete_project_file` - Delete a folder, or a file by the `version` `openl_read_project_file` answered, which it requires (auto-cleans dangling config references)
- `openl_search_project_files` - Search by glob `pattern`, `extensions`, `type`, or case-insensitive `content` substring; `scope` SUBTREE (default) or ANCESTORS; a search `from` a folder without `recursive` lists that folder, each match with its size and date. Studio matches `content` in a file read as text, so the text of a workbook cell or a compressed ZIP entry is never found, and skips a file over 1 MB; locate binary files by path/extension and read them separately. The matches are a table of the fields asked for, and an ANCESTORS search answers each file it finds with its content. A SUBTREE `content` search counts, after the table, the workbooks it covers but cannot read, and points to `openl_list_tables` `text` for the text of their cells. An ANCESTORS search takes the exact name of a text file (`.md`, `.json`, `.yaml` and alike, up to 1 MB) as `pattern`, not a glob, and ignores `content`, `extensions` and `type`; its match carries its content up to 5,000 bytes, and `contentOmitted` beyond
- `openl_copy_project_file` - Copy a file within the project (no overwrite — destination collision returns 409)
- `openl_move_project_file` - Move or rename a file within the project

### Trace Tools (9, BETA)
An **interactive debugger** for rules: the rule runs on a server-side worker that
suspends at breakpoints and step points, and the tools inspect that live, suspended
execution. Studio keeps the debug session for the client — its Personal Access Token,
or the one user in single-user mode — so every call of the flow that comes with it
finds the session. One active session per user (a new start terminates the
previous); idle sessions are reaped after ~10 minutes.

- `openl_start_trace` - Start a debug session for a table (test case via `testRanges`, or `inputJson`; omit both to replay the remembered `inputJson` — test ranges are not remembered) and run to the first stop; optional initial `breakpoints`. With `profiling: true` + `stopAtEntry: false` it returns a constant-size `profile` overview (see below)
- `openl_step_trace` - Step the current frame. `out` (run the frame to its exit so its result is inspectable) + breakpoints is the main move for declarative rules; `into`/`over` are advanced (imperative TBasic/loops). Returns a compact stack (steps for the active frame only); `withValues: true` bundles the active frame's variables so you don't need a separate inspect; every step of the frame is then listed once, in the frame, with its value and its cell
- `openl_resume_trace` - Run to the next breakpoint / exception / completion (further than `step out`, which stops at the current frame's exit), waiting inside the call; after a timeout, `attach: true` keeps waiting without resuming again, which would run past a stop reached meanwhile
- `openl_inspect_trace_frame` - Freeze one stack frame: parameters, context (without the fields the run left unset, a dozen nulls otherwise), result, sub-step values; for decision tables `decision` (which rule fired, how each condition evaluated) and `ruleNames`; optional A1-keyed cell `highlights` + raw grid. Filter steps with `onlyExecutedSteps` / `excludeStepValues` (e.g. `[1]`) to surface an outlier among neutral factors
- `openl_set_trace_breakpoints` - Read the active breakpoint keys and available targets; `breakpoints`, named as for `openl_start_trace`, replaces the whole set. Key forms: `<name>`, `<uri>`, `<uri>#R{r}C{c}`, `<uri>#rule` (any rule fires), `<uri>#<ruleName>` (specific rule). Append `@N` to any key to break only on the table's N-th execution (0-based) — e.g. `<uri>#R48C0@3`; without it a cell breakpoint hits every pass of a table that runs many times. `N` matches `frames[].instance` and a watch series' `instance`
- `openl_get_trace_value` - Expand a lazy value (`lazy: true` + `parameterId`) from openl_inspect_trace_frame, openl_step_trace with `withValues` or openl_watch_trace_cells; returns name/description/value only — `withSchema: true` adds the value's (large) JSON Schema
- `openl_expand_trace_tree` - Load one level of a **profiling** run's executed call tree on demand — it comes back lazy, so each step carries a `childrenTotal` count instead of nested `children`. Expand a step whose `childrenTotal` > 0 by its node (`uri` + `instance`) and step `ref`; returns `{ children, total }` where each child is itself shallow (expand again). Page a loop's many sub-calls with `offset`/`limit` (the reply flags `hasMore`/`nextOffset`); a node's `notRetained` counts sub-calls dropped once the tree hit its size limit. Start from the `tree` root (`includeTree: true`)
- `openl_watch_trace_cells` - Watch **scalar** cells (a single number/string factor, e.g. `['$VehiclePriceFactor']`) across a whole run and return one series per cell with its value at every execution of its table — "show me this factor across all coverages" without dumping frames; spot the outlier, then jump straight to that pass with a `<point.ref>@<point.instance>` breakpoint + replay. Do NOT watch a cell whose value is a big aggregate object (a whole spreadsheet result like `$RateCardPremium`) — it makes every point huge; drill into an aggregate with a breakpoint + inspect instead. Captures cells inside lazy result branches too. Each point's value is a ParameterValue (lazy when large — expand with `openl_get_trace_value`); value JSON Schemas are omitted unless `withSchema: true`. It starts a new session, ending the current one, clears the breakpoints before the run and the watches after it, and tells a cell name the run never computed and a run that stopped before its end. A series lists its first 100 points and is marked `truncated` when its `total` counts more (reach a later execution with a `<ref>@N` breakpoint); a `note` tells when the run reached its capture limit, after which the totals count only what it captured
- `openl_stop_trace` - Terminate the session (idempotent; breakpoints survive); answers that it `stopped`

A traced value leaves out `lazy: false` and a `name` its step or watch series already tells; the variables of a frame
leave out a runtime context with no field set and an empty `errors`, and a watch point names no 1-based `label` beside
its `instance`. A stack lists its frames from the root to the active one, the last, and only the active frame lists its steps. A
frame names its `depth` only when it differs from `index` + 1, its `completed` and `error` flags only when true, and its
`location` only when no listed current step repeats it; the stack names no session id, which no tool takes.

Lifecycle (status values are lowercase): `running ⇄ suspended → completed | error | terminated`.
Stepping and inspection are valid only while `suspended`; on a terminal status read the
final state (structured `error`, profiling `profile`/`tree`) from the stack that the last
start/step/resume call already returned.

Cheapest whole-run overview: `openl_start_trace` with `profiling: true`,
`stopAtEntry: false` and `breakpoints: []` (left out, the set of the session stays) completes in one call and returns `profile` —
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

### Deployment (3)
- `openl_list_deploy_repositories` - List deployment repositories
- `openl_list_deployments` - List active deployments, each with the `id` a redeploy takes; the revisions of their projects come only with the `project` filter
- `openl_deploy_project` - Deploy to production: a new deployment by `deploymentName` and `repository` (the id or the name of the repository, in any case), or the project again to the deployment `deploymentId` names, as a newer version or a rollback

### Diagnostics (1)
- `openl_get_server_info` - Report the OpenL Studio the server works with, as Markdown: `Server: OpenL Studio <version>` and `URL: <address>` — the address its users open, restored from the `X-Forwarded-*` headers or the first `Forwarded` element of the request, its `Host` otherwise, with the context path of Studio. `full: true` adds the build number, the start and the current time, OS, architecture, Java, CPUs and their load, threads, and the memory of the machine and of the JVM, from `/rest/public/info/build.json` and `/rest/public/info/sys.json`. The path of a document `openl_get_guides` names is relative to that address

## Skills

The server serves the OpenL skills of `skills/` through the MCP skills extension (SEP-2640,
`io.modelcontextprotocol/skills`, declared with `directoryRead`):

- `planning` — the entry point for building, changing, or fixing anything in a project
- `branching` — task branches, sync with the base, conflicts, merge, hotfixes, cleanup
- `testing` — test rows, running the tests on the saved revision, diagnosing failures
- `versioning` — table versions by date, LOB, state or currency, and their tests
- `trace-investigation` — the root cause of an unexpected rule result over the trace tools
- `change-log-and-reapply` — a log of the changes of a feature, and reapplying the missing ones

Every file of a skill is a resource `skill://<skill>/<path>`: read `skill://<skill>/SKILL.md` with `resources/read`
and resolve its relative links against the folder of that `SKILL.md`. `skills/list` answers every skill — its
frontmatter as JSON and every file with its SHA-256 digest and size — and `skills/get` one skill by the URI of its
`SKILL.md`; an unknown URI is `-32602`. `resources/directory/read` lists the files of a folder of a skill. A client
without the extension finds the skills in the server instructions, which also send the agent to `openl_get_started`
first, and in `openl_get_started` itself.

## Local projects (repository: local)

Projects with `repository: 'local'` are stored on disk without Git; **OPENED/EDITING status is not checked or required** for them — local projects are always considered editable.

**For local, these work:**
- `openl_list_projects` (call without repository filter, follow pagination to completion, then filter by `repository: "local"` in the response; the `repository: "local"` filter may fail because the "local" repository is often not returned by `openl_list_repositories`), `openl_get_project`;
- Table tools: `openl_list_tables`, `openl_get_table`, `openl_update_table`, `openl_create_project_table`, `openl_copy_table`, `openl_delete_table`, dependency/property discovery, and the raw table-source edits of `openl_edit_table`;
- Module/sheet discovery and regular table execution: `openl_list_project_modules`, `openl_list_module_sheets`, `openl_run_table`;
- Project deletion: `openl_delete_project` after exact project-name confirmation;
- Test execution and results: `openl_start_project_tests`, `openl_get_test_results` (the project is not opened before running tests for local).

**For local, do not use:**
- `openl_open_project`, `openl_save_project`, `openl_close_project` (no commits or status changes);
- Git tools: `openl_list_branches`, `openl_create_project_branch`, `openl_repository_project_revisions`;
- Project branch/merge tools: `openl_list_project_branches`, `openl_check_project_merge`, `openl_merge_project_branches`, merge-conflict inspection and resolution, conflict-session cancellation, and `openl_delete_project_branch`;
- `openl_list_project_local_changes`, `openl_restore_project_local_change` (require an opened project; local projects cannot be opened).

Deployment (`openl_deploy_project`) for projects with `repository: 'local'` is typically not used via the studio.

## Projects edited in place

In single-user mode with a design repository on the file system (`repo-file`), as a developer runs OpenL Studio on
their machine, every project of the repository is edited where the folder keeps it: it is always open, never modified,
and every edit is written straight into its files. `openl_get_project` tells such a project by its `capabilities`:
`canOpen`, `canSave` and `canClose` are left out, and so is `canManageBranches`, as the folder has no branches.

- Edit its tables and files right away, without `openl_open_project`.
- Do not call `openl_save_project` or `openl_close_project`, nor the branch and merge tools: there is nothing to save,
  the project is never closed, and the folder has no branches.
- What another program, such as Excel or an IDE, writes into the files is part of the project; the tools that work
  with what OpenL Studio compiled compile it again by themselves, and a write based on an outdated read is refused.
- The project-file tools say the change is in the files already instead of asking for a commit.
- A table edit refused because a workbook changed outside OpenL Studio needs no refresh call: read the table again
  with `openl_get_table` and repeat the edit on what it holds now.

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
- Every legacy MCP session owns a distinct `OpenLClient`. Never reuse a client across MCP sessions. Each request of a
  session names the address its client calls from now (`OpenLClient.callsFrom`), as it may go on from another one.
- Most legacy clients never close their session, so opening a session closes the others no request has used for
  `LEGACY_SESSION_IDLE_MS`, the 30-minute HTTP session timeout of Studio. A session with a request in progress, its
  stream of server messages included, is in use.
- Modern HTTP is stateless and constructs a fresh Studio client per request. Studio keeps the state of multi-call workflows (interactive trace, test results, and merge-conflict inspection) for the Personal Access Token a request carries, and for the one user in single-user mode, so they work over modern HTTP.

## Response formatting

- A write answers what is new — ids, versions, revisions — and no `success` flag, as a failure is an error. A table
  edit, creation or copy answers the `tableId` and `version` of the table (with `previousTableId` after a rename, and
  `compiles: false` with its `errors`), and a table deletion the id it deleted. A file write answers its `action`, the
  new `version` of the file — the SHA-256 of its content, as Studio takes it — and whether it was `committed`; a file
  copy, move or deletion the path it made or removed. A project open answers that it `opened`, or that it was
  `alreadyOpen`; a save whether it `saved` — false with nothing to save — and, when `closeAfterSave` asked for it, that
  it `closed`; a close that it `closed`, with `saved` or `discarded`; a local-change restore the `historyId` it
  `restored` and the one that undoes it, `undo`; a merge-conflict cancellation that it `cancelled`; a project creation
  or copy its `projectId`, `branch` and `revision`; a branch creation and a deploy the branch `created`, the deployment
  `deployed` or `redeployed`. A merge answers its `summary`, or the conflicts with a one-line `nextAction`; a branch
  deletion the branch, its divergence, its base branch and any risk it accepted.
- A cell of a conflicted workbook names no A1 address of its own, as its place in the grid and the range of its version
  give it; a conflict cell names `ourCell` and `theirCell` only where a version moved it.

- Lists and test results are Markdown tables (`markdown-list.ts`): a line counting the items and naming the offset of the next page, then one row per item. A table names each field once, in its header, where a JSON list repeats every name in every item: a page of tables, projects or test results reads in a third of the tokens or less, and a page holds three times the rows before the limit cuts it.
- An answer of data is JSON without indentation; a guide, the AGENTS.md guidance, the text of a file and the server info are answered as the text they are. Only `openl_get_table_dependencies` takes `response_format`, as its Markdown draws the graph as Mermaid diagrams; every other tool refuses the argument.
- No tool carries file bytes as base64 or as an embedded resource: a file other than a short text is answered with a link (see Project Files Tools), never as a `resource_link` either, since a client may fetch or preview a link it renders.
- List operations name the page they answer and the offset of the next one in their first line.
- Large responses are truncated at a 25K-character limit — except `openl_get_guides` bodies, which are returned verbatim (sizes are published in the index so callers can budget). A response over the limit is cut by whole items (a list by rows with the next offset moved to match, `openl_get_table` by rows with `totalRows` and the `offset` to continue from), and the note names only a way to continue that the tool has.

## OpenL-specific behaviour

- **Dual versioning** — Git commits (temporal) and dimension properties (business context).
- **Table types** — Rules, SimpleRules, SmartRules, Lookups, Spreadsheet, Datatype, Method, Test, and others.
- **Project ids** — opaque 13-character ids OpenL Studio answers; a project keeps its id while it stays in its folder. Never build or decode one.

### One table shape

- The table tools read and write one shape of a table: `openl_get_table` answers `rows`, `merges` and `formulas`,
  `openl_update_table` and `openl_create_project_table` take them, and the lines of `openl_edit_table` are rows of
  the same values at the same positions. `table-grid.ts` converts between it and Studio's raw matrix, the
  `RawSource` view, which is the only content Studio's table API round-trips.
- Never add typed table request/response variants such as `EditableTableView`,
  `AppendTableView`, `SimpleRules`, `Spreadsheet`, `Datatype`, or `Test` DTOs to
  MCP schemas, TypeScript content types, handlers, or examples. Studio's
  typed views are incomplete and lossy and cannot reliably round-trip workbook
  cells, layout, styles, merged regions, and less common table features.
- A table's semantic kind still appears in list/run/dependency metadata and is
  encoded by the OpenL grid itself. That metadata is not authorization to expose
  a typed content contract.
- Prefer `openl_edit_table` for isolated edits. When a full replacement is necessary, send back every row of the
  table with its `merges` and `formulas` as `openl_get_table` read them.
- Raw cell values use Studio's round-trip-safe domain everywhere: scalar
  string/number/boolean/null values and representable one-dimensional arrays of
  those scalars. Arrays must contain at least one item; a singleton `[null]`,
  nested arrays, objects, and string elements surrounded by characters Studio
  trims (whitespace or ISO controls) are not representable. MCP checks the shape
  of a scalar or a flat array; Studio refuses the rest and does all context-dependent
  OpenL parsing and workbook serialization.
- A read cell may carry its `formula` (its `value` is the computed result) and its Excel `comment`. The write
  schemas take both back, so a read view round-trips: Studio writes the `formula` as the Excel formula of the cell,
  ignoring its `value`, and keeps the read-only `comment`. A cell sent without its formula takes its `value`.
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
  Node.js runs without a `package.json`, and the `*.mjs` chunks it imports. The OpenL Studio war unpacks it into
  `WEB-INF/mcp`, and `McpServerProcess` of `studio-backend` runs `WEB-INF/mcp/index.mjs`.
- `npm run build` type-checks `src`, the tests included, with `tsc`, which emits nothing, and Rolldown
  (`rolldown.config.js`) bundles `src/index.ts` with the code it takes of the production dependencies into a
  minified `dist/index.mjs`. A module imported with `await import()` becomes a chunk of its own beside it,
  `<name>-<hash>.mjs`, which Node.js loads only when the server imports it. The name, version and description of
  `package.json` go into the bundle.
- The `requires` plugin of the bundler (`src/build-requires.ts`) bundles the packages a dependency loads with a
  `require` of its own (`__require("<package>")`, made by `createRequire`), which the bundler would leave for Node.js
  to find in a `node_modules` folder the war does not carry: the OpenTelemetry SDK loads `forwarded-parse` and
  `@grpc/grpc-js` so. A call that still loads a package at run time works only in `dist/` of the module, where
  `node_modules` is found, so `ITEST/itest.studio/telemetry` checks the bundle inside OpenL Studio.
- The `skills` plugin of the bundler (`src/build-skills.ts`) embeds the text of every file of the skills of `skills/`
  in the bundle as the module `virtual:skills`, which `SkillsExtension` of the MCP SDK serves;
  `vitest.config.js` applies it too, so the tests import the skills the bundle carries. A skill the skills extension
  of the SDK refuses fails the build.
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
- Every call to OpenL Studio names its caller back: the `OpenL-Client-Address` of the request to the server and the
  `clientInfo` of the MCP client in `OpenL-Mcp-Client`, with the secret of the run (`OPENL_MCP_SECRET`) in
  `OpenL-Mcp-Secret` (`callerHeaders` of `auth.ts`). A session takes the `clientInfo` of its `initialize` request; a
  tool call of the stateless protocol takes the one of its `_meta` envelope (`clientInfoOf` of `mcp-core.ts`). The
  audit log of Studio records what the client does with that address and name, which it takes only with the secret.
  Never send the secret anywhere else. Studio passes each request on with the secret too, and the server refuses a
  request to `/mcp` without it with `403` (`comesThroughStudio` of `http-server.ts`), so another local process cannot
  name the address the secret goes to. A server run without a secret takes every request.
- The package is private: it ships only inside OpenL Studio and has no release line of its own.

### Running and debugging

- `npm test` runs every test and reports the coverage; `npm test -- src/<file>.test.ts` runs one file, and
  `-t "<name>"` one test. `npm run lint` covers `src`, the tests included.
- Against a running OpenL Studio, outside Studio:
  `npm run build && HOST=127.0.0.1 PORT=3001 node dist/index.mjs`, then connect a client to
  `http://127.0.0.1:3001/mcp` with the header `OpenL-Base-Url: http://localhost:8080`. Inside OpenL Studio, every
  other variable of the environment of Studio reaches the server.
- OpenTelemetry is off unless `OTEL_SDK_DISABLED=false`, which OpenL Studio sets when it sends traces or metrics itself
  (`OtelEnvironment` of `studio-backend`). A failure to start the SDK is logged, and the server runs without it.
  `src/testing/otlp-collector.ts` is a collector the tests of the telemetry send to. Outside Studio, add it and a
  collector: `OTEL_SDK_DISABLED=false OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318 node dist/index.mjs`, or
  `OTEL_TRACES_EXPORTER=console` to print the spans. `telemetry.ts` is imported with `await import()` only, so the SDK
  stays a chunk of its own; anything else reaches OpenTelemetry through `@opentelemetry/api` alone, as
  `tool-telemetry.ts` does.
- A browser-based inspector needs its origin in `cors.allowed.origins` of OpenL Studio.

### Code quality

- Keep the code clean at all times: no dead code (unused files, exports, functions, variables, or unreachable branches) and no unused dependencies. Remove them as soon as they become orphaned.
- Add a third-party library only when it brings significant benefit — that is, it substantially reduces the code we would otherwise write and maintain. Prefer reimplementing small or simple functionality over taking on a dependency.
- `npm run lint` must report no errors.
- The code is ECMAScript 2025, which every Node.js 24 runs (`engines` of `package.json`): use its built-ins —
  `replaceAll`, `at`, `Object.groupBy`, iterator helpers, `Set` methods, `Promise.withResolvers` — rather than
  hand-written equivalents.
- A tool description and the descriptions of its arguments say what an agent acts on: what the tool does, its
  limits and refusals, and how to continue. They leave out why the design is so, the Studio endpoint a tool calls,
  and what the server does inside: every agent loads them in every session. Rules several tools share go into one
  tool rather than into each description.
- Every tool names the same thing by the same argument: `projectId`, `tableId`, `module`, `branch`, `repository`,
  `path` of a file, `name` of what the call creates; a list, and a slice of the rows of a table, is paged by `offset`
  and `limit`. An agent that learned an argument of one tool uses it on the next.
- Every tool schema is a `z.strictObject`: an unknown argument, also a nested one, is refused, not ignored. The
  refusal of an unknown argument of the tool itself lists the arguments the tool takes.
- An agent loads the published schema of every tool in every session, so `getInputSchema` of `handlers/common.ts`
  leaves out what tells it nothing: the `$schema` dialect and the safe-integer bounds of `.int()`. Zod still checks
  them when the tool runs.
- A mistake of the caller throws `ProtocolError` with `InvalidParams` or `InvalidRequest`; any other `Error` reaches
  the agent as an internal error.
- Long work waits inside one call — `waitForResult` of `handlers/wait.ts` for Studio's `202 notReady` — honours the
  `AbortSignal` of the call and sends progress notifications. The server opens no WebSocket: only
  `openl_project_status` may answer `compiling` and ask the agent to call again.
- Answer a list as a Markdown table of `markdown-list.ts`: `formatStudioPage` for a page Studio pages, `formatListOf`
  for a page of an array the server holds itself, `formatWholeList` for the whole array.

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
- `ITEST/itest.studio/mcp` runs the server inside OpenL Studio, behind its security and its `/mcp` servlet;
  `ITEST/itest.studio/telemetry` checks the spans and the metrics it sends to a collector.

### Skills

The skills of `skills/` are written for this server and follow the
[best practices](https://agentskills.io/skill-creation/best-practices) of Agent Skills:

- A skill holds only what the agent lacks: the procedure, the approvals, and the gotchas of OpenL and Studio. It
  never restates a tool description, `openl_get_started`, the guides Studio serves — it names the guide to read — or
  another skill, which it names.
- `SKILL.md` holds what every run needs; a file of `references/` holds what only some runs need, and `SKILL.md`
  says when to read it.
- A skill names a tool by its `openl_` name and leaves its arguments to the tool description; a test fails on a
  name the server has no tool for.
- Evaluate a change of a skill before committing it: run an agent on realistic tasks of the skill against a
  throwaway OpenL Studio, once with the previous version and once with the new one, and compare the tool calls and
  the answers.

### Documentation

- Keep this `AGENTS.md`, the tool descriptions and the documents under `Docs/` that describe the server in step with
  every change of tools, skills, configuration or behaviour. The server offers no MCP prompts: guidance for agents
  lives in the tool descriptions, `openl_get_started`, the skills and the guides OpenL Studio serves.
