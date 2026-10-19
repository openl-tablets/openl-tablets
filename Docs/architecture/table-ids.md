# Stable Table Ids

How OpenL Studio names a table so that a client can keep addressing it: across edits that move it, edits made in
Excel, merges, recompiles, restarts and other Studio instances.

## Goals

- **A table keeps its id while it stays the same table.** Moving it, growing it, inserting rows above it in Excel,
  merging a branch that moved it, recompiling the module, restarting Studio or opening the project in another
  Studio instance leaves the id as it was.
- **An id never silently names another table.** An id that no longer names a table is refused; it never reads or
  writes whatever table happens to stand where the table stood.
- **The id is known without a successful compile.** A table whose header or body does not compile still has an
  id, and fixing the error does not change it.
- **Nothing is written to make an id.** The id is computed from the workbook as it is; no file changes for it.

## Decisions at a glance

- **The id** — built from what the table is: its module, its kind, and its name or `id` property. Neither the
  project, the parameters nor the properties are part of it.
- **Format** — 16 characters of URL-safe base64, 96 bits of a SHA-256 digest.
- **Write answers** — a write that moves a table keeps its id; only a rename, or a change of the `id` property,
  answers a new one.
- **Old ids** — the position ids of earlier versions are an implementation detail, not a public contract. They are
  not accepted and not migrated: such an id answers `404`.
- **Workbooks** — nothing is written into them: no anchor, no hidden name.
- **Renamed and deleted tables** — the server remembers the ids it saw change and serves an old id under the current
  one, and an id of a deleted table answers `410 Gone`; see [Stale-Id Ledger](#stale-id-ledger).

## The Identity Id

`TableIds` (DEV/org.openl.rules) makes the ids; `TableSyntaxNode.getId()` answers them.

### Format

The id is the first 96 bits (12 bytes) of the SHA-256 digest of the table's identity, written as 16 characters of
URL-safe base64 (RFC 4648 alphabet `A-Z`, `a-z`, `0-9`, `-`, `_`; 12 bytes need no padding):

```text
v1 | rules/Rating.xlsx | method | rate   ->   9d8vgdp7SqnnDpLZ
```

The `|` stands for U+001F, the unit separator, which joins the fields; no OpenL name or file path practically
contains it.

- **Distinct from the old ids**, which were 32 hex characters: an old id is never taken for a new one.
- **Safe in an address** — a URL path segment, a query parameter and JSON take the id as it is: the alphabet has
  neither `/` nor `+`, and no padding `=`.
- **Case matters** — two ids may differ in case alone, so an id is compared exactly as it is written.
- **Collisions are handled, not assumed away.** With 96 bits, the chance that two tables of a project of ten
  thousand tables share an id is about 6 in 10^22. Two identities of a module with one digest are told apart as
  tables sharing an identity are: the later one takes another number, which hashes to another id.
- **SHA-256**, not MD5, keeps Sonar's weak-hash rule quiet.
- **Versioned.** The identity starts with `v1`, so a later change of the rules gives new ids instead of silently
  colliding with old ones.

### What the identity is made of

`v1`, the module, the kind and the key:

- **Module** — the path of the table's workbook from the project root, with `/` separators. The project folder is
  told by the module source, `IProjectSourceCodeModule`, which `ModulePathSourceCodeModule` is. Neither the project
  name nor its id is part of it: the project is named by the `{projectId}` of the address, and renaming or moving a
  project changes no id. A workbook included by an Environment table is named by its path from the project root
  too, so its tables have one id for every user and machine. A table the compiler built, which stands in no file, is
  named after the module it was built for.
- **Kind** — what the kind of table declares into. All executable kinds share `method`, so turning a SmartRules table
  into a SimpleRules table keeps its id.
- **Key** — the `id` property when the table declares it; otherwise the name.

| Kind | Keywords | Kind label | Key |
|---|---|---|---|
| Executable | `Rules`, `DT`, `SimpleRules`, `SmartRules`, `SimpleLookup`, `SmartLookup`, `Spreadsheet`, `Calc`, `Method`, `Code`, `TBasic`, `Algorithm`, `ColumnMatch` | `method` | The `id` property when declared; otherwise the name: `rate` |
| Datatype, vocabulary | `Datatype` | `type` | The name: `Bank` for `Datatype Bank extends Base` and for `Datatype Gender <String>` |
| Data | `Data` | `data` | The table name: `bankData` for `Data Bank bankData` |
| Test | `Test`, `Testmethod` | `test` | The test name; an unnamed test: the tested method, `calc` |
| Run | `Run`, `Runmethod` | `run` | As for a test |
| Constants | `Constants` | `constants` | — |
| SmartRules column tables | `Conditions`, `Actions`, `Returns` | `conditions`, `actions`, `returns` | — |
| Properties | `Properties` | `properties` | The scope and the category it applies to |
| Environment | `Environment` | `environment` | — |
| Table part | `TablePart` | `tablepart` | The table name and the part number |
| Generated | — (compiler) | `generated` | The generated name: `validateGapOverlap_rate` |
| Other | Any other first word | `other` | The first cell text, trimmed and with whitespace collapsed |

- **The key is read from the cells, never compiled.** A method is named by the word before its `(`; a data table, a
  datatype and a test by their words after the keyword; the `id` property is read from the properties section. A
  table whose parameter list or types do not compile keeps its id. Only a header whose name cannot be found at all
  takes the key `!` followed by the header text after the keyword; fixing it changes the id.
- **Left out on purpose** — the parameters, whatever their names, types or number; the return type; the `Collect`
  clause; the keyword within a kind; and the properties, declared in the table or inherited, dimension properties
  and `version` included. Changing them keeps the id. The versions of a table that differ by their properties
  usually live in modules of their own, whose path or name carries the properties, so the module already tells them
  apart.
- **The `id` property** — not to be confused with the table id of this page. It is unique in the module
  (`UniquePropertyValueValidator`), limited to a Java identifier and declared in the table only, so it is the most
  stable key a user can give a rule table: renaming the table keeps the id.
- **An unknown keyword** — `Rulse Double rate()` is an Other table, as the compiler reads it.

### Tables that share an identity

Several tables of one module can share an identity:

- versions of one table that differ only by their properties, kept in one workbook — rare, as versions usually
  live in modules of their own;
- overloads that differ only in their parameters;
- duplicated tables the compiler reports;
- unnamed tests of one method, and the kinds with no key: `Constants`, `Environment`, the SmartRules column tables.

They are numbered in the order the module holds them — by workbook, by sheet, then from top to bottom, with the
tables gathered from parts after the tables of the sheets and the tables the compiler built last. The first keeps
the bare identity; the next ones add `~2`, `~3` as the last field.

- **Studio's own moves and deletes shift nothing.** A Studio write that moves such a table — appending cases to an
  unnamed test, or retiring the version a copy keeping its name replaces, which takes the rows of its `version` and
  `active` properties — pins the table at its new place to the number it had. A Studio delete of such a table pins
  the others at their places (`TableIds.deleting`), so the tables after it do not each take the id of the one
  before. The numbering honors the pins: the pinned tables keep their numbers, and the others are numbered around
  them. `TableIds` keeps the 1,000 most recent pins in memory, so they last until Studio restarts; the numbering
  after a restart follows the order again.
- **Reordering outside Studio can swap ids.** Tables of one identity moved past each other in Excel, or by a merge,
  exchange their numbers, and an id held from before names the other table. The `id` property, or distinct names,
  give such tables ids of their own.

### What changes an id

| Change | Id |
|---|---|
| Studio moves the table because it has no room to grow | Kept |
| Rows or columns inserted or deleted elsewhere, in Studio or in Excel | Kept |
| The table cut and pasted in Excel within its workbook, also to another sheet | Kept |
| A merge moves the table | Kept |
| A sheet renamed | Kept |
| A parameter added, removed, renamed or retyped; the return type changed | Kept |
| A property changed, dimension properties and `version` included | Kept |
| The keyword changed within its kind: Rules to SmartRules | Kept |
| A header that does not compile, as long as its name can be read | Kept |
| The project renamed or moved | Kept |
| The module recompiled, Studio restarted, the project opened in another Studio instance | Kept |
| The table renamed, without the `id` property | New |
| The `id` property added, changed or removed | New |
| The module file renamed or moved | New |
| The table moved to another module: a copy and a delete | New |
| The table copied | New table, new id |
| Tables that share an identity reordered outside Studio | Their numbers may swap |

A new id does not end the old one at once: the [Stale-Id Ledger](#stale-id-ledger) serves the old id of a renamed
table, and of the tables of a module file moved in Studio, while the server runs.

### Uniqueness

- **Within a project.** An identity includes the module path, and the numbering makes it unique within the module,
  so ids are unique in the project.
- **Across projects.** The `{projectId}` of the address tells projects apart. A project and the projects it depends
  on can hold modules with one path and tables with one identity: a lookup in a project finds the table of the
  project first, and a table of a dependency whose id is taken is read through its own project.

## Computing Ids

- **Numbered per module, on first use.** The numbering and the collision check need every table of the module, so
  the first `getId()` of a table numbers all the tables of its module at once. A table reaches its module through
  its parents in the syntax tree; a table gathered from parts belongs to its workbook as the tables of its sheets
  do.
- **A written table** — a write answers the id the table is known by once its module is compiled again:
  `TableIds.idAfterWrite` for a table that was there before, `TableIds.idOfWritten` for a new or copied table. The
  written table is read as the compiler reads it and numbered among the tables of its module at its new place. A
  table that is still what it was keeps its id, which is found without reading the old header again. The id is read
  before the workbook is saved: once saved, the sheet of a compiled table is read as it was compiled.
- **Where else an id comes from**
  - test, run and benchmark results — `TestSuite.getTableId()`, from the test table or the rule table run;
  - trace frames — the table of the executed method, read by `DefaultSourceClassifier`;
  - the dependency graph — the tables of the compiled methods, and of the datatypes by the location they know their
    table at;
  - cell usages — the table at the location a word leads to, looked up once per read in the compiled project
    (`TableModules.tableIdOf`).
- **Lookups** — `StaleTableIds.tableOf` asks `ProjectModel.getTableById`, which compares the ids of the tables of the
  compiled project, and the ledger for an id no table has.
- **Cost** — one SHA-256 of a short string per table and compile, a few milliseconds for ten thousand tables.

## Where Ids Are Taken and Given

Paths are under `/rest`.

| Area | Takes an id | Gives an id | Resolved by |
|---|---|---|---|
| Table list and search, `GET /projects/{projectId}/tables` | — | `id` of each table, with `file`, `sheet`, `pos` | `TableSyntaxNode.getId()` |
| Table read, `GET .../tables/{tableId}` and its `tests`, `targets`, `properties`, `details`, `input`, `input/cases` | Path | `id`; `metaInfo` usages `tableId`; `messages[].location.id`; `inheritedTableId`; target and test `id` | `StaleTableIds.tableOf`, scoped by `?module=` when given |
| Table writes, `PUT .../tables/{tableId}`, `POST .../lines`, `.../actions`, `.../actions/batch`, `PATCH .../properties` | Path | `204`, or `200 {"id"}` and `Location` when the write renamed the table or the request named it by an old id | `StaleTableIds.tableOf` for a writable table |
| Create and copy, `POST .../tables`, `POST .../tables/{tableId}/copy` | Path of the source | `201` with the `id` of the new table | The new table is read back by id, or by name in a new module |
| Delete, `DELETE .../tables/{tableId}`; editors, `GET` and `DELETE .../tables/{tableId}/editors` | Path; ignored by the `DELETE` of editors | — | `StaleTableIds.tableOf` |
| Dependency graph, `GET .../tables/graph`, `GET .../tables/{tableId}/graph` | Path | Node `id`, `dependencies`, `dependents`; a dispatcher node is `dispatcher:<id>`, which no lookup resolves | `StaleTableIds.tableOf` |
| Format, `POST /projects/{projectId}/format` | — | `formatted`, `skipped` | — |
| Run, tests, benchmark, `POST .../run`, `.../tests/run`, `.../benchmarks` with `?tableId=` | Query | `tableId` of the results | `RunnableTables.require`, through `StaleTableIds.tableOf` |
| Tests summary, `GET .../tests/summary` and `.../tests/summary/{tableId}/cases/...` | Query and path | `testCases[].tableId` | The retained run, by `TestSuite.getTableId()`, then `StaleTableIds.currentIdOf` |
| Trace, `POST .../trace?tableId=` and the stack, step and watch answers | Query | `uri` and `tableId` of every frame | `RunnableTables.require`; the tree children and the breakpoints take table URIs or names, not ids |
| Project status, `GET /projects/{projectId}?include=status` | — | `messages[].location.id` | — |
| WebSocket | Destinations `/user/topic/projects/{projectId}/tables/{tableId}/{tests,run,trace,benchmarks}/...` | `tableId` in test results; `location.id` in the status stream | Keyed by the id of the request that started the run |
| Studio UI | `?table=<id>` on `/projects/:projectId/modules/:moduleName`; `tableId` of the trace window; links from results, problems, usages, search and the graph | — | The editor follows the `id` of a `200` write answer |
| Browser storage | localStorage `openl.execution.window` keeps the `tableId` of the last run window | — | — |
| Server memory | The run, test and benchmark result registries and the debug session, per client | — | Cleared on a workspace reset |

- **Not persisted.** No database table, user setting, project file or metainfo record holds a table id.
- **Not a table id.** The `tableId` of `GET /compare/{comparisonId}/tables/{tableId}` is the position of a table in
  a comparison, `<sheet index>-<table index>`.
- **An unknown id** answers `404` with `table.message`, or `table.in.module.message` when `?module=` is given. An id of
  a table OpenL Studio deleted answers `410` with `table.deleted.message`.

## Why Not the Position

Earlier versions hashed the place of a table — project folder, file, sheet and top-left cell. The id then changed
whenever the table moved, although it stayed the same table:

- OpenL Studio moved a table that had no room to grow to the end of its sheet (EPBDS-16086, EPBDS-16740);
- a row or column inserted or deleted in Excel moved every table below or to the right of it;
- a merge brought such a move in from the other branch (EPBDS-16884);
- a sheet, a module file or the project was renamed;
- a generated table got a new id at every compile, and a table of an included workbook one id per user;
- worse, a table that moved away left its id to whatever table later landed on its cell, so a client holding the id
  read or wrote another table with no error.

Clients compensated: the MCP server kept an alias map of its own, learned from its own edits only (EPBDS-16084).

## Stale-Id Ledger

`StaleTableIds` remembers the ids it saw change, so that a client holding an id from before a rename is served
instead of answered `404`, and a client holding the id of a deleted table is told so.

- **Where** — one ledger per project folder of a user's workspace, shared by every session and client of the user,
  in memory: the most recent 1,000 changes of the project, lost on restart. After a restart an old id answers `404`.
- **What feeds it**
  - a Studio write that renames a table or changes its `id` property: the old id leads to the one the write answers;
  - a module file or folder moved through `POST /rest/projects/{projectId}/file-move`, for the project open in the
    session: the tables of the moved workbooks keep their kinds, keys and numbers under the new place of their
    workbook (`TableIds.idsAfterMove`), worked out before the move and learned once it is done;
  - a compilation: `StaleTableIds.Compilations`, held by `ProjectModel`, takes the ids of a module when its
    compilation is dropped — a reset of the module, or a reload that lets go of every module — and compares them
    when the module is compiled again. A table whose id no table has any more and a table under an id the module did
    not have, starting at one cell (workbook file, sheet, top-left cell), are one table renamed. The ids are taken
    only when one of them was asked for, as nobody holds the others, and are kept for the 1,000 most recent modules;
  - a Studio delete: the id of the table, and every id that led to it, answer `410 Gone` with "The table was
    deleted."
- **Rules**
  - a live id always wins: the ledger is asked only for an id no table has;
  - chains are compressed: every old id leads straight to the current one;
  - a deleted table stays deleted: a table a compilation finds later at its cell does not take its old id over,
    while a Studio write that renames a table which took the deleted id again is learned as any rename;
  - an id a table takes again leaves the ledger, so it never leads elsewhere once that table goes.
- **Served, not redirected** — the request is answered for the current table, with the current id in the body and
  the address of the request, the old id replaced in its path or in its `tableId` query parameter, in a
  `Content-Location` header; an old project id in the same address is replaced too (see
  [Stable Project Ids](project-ids.md#stale-id-ledger)). A redirect would turn every write into a `307` or `308` that
  some clients replay and others drop, and would collide with the `Location` header a write already answers.
- **One resolver** — `StaleTableIds.tableOf` resolves the `{tableId}` of every table path, the graph and, through
  `RunnableTables.require`, the `tableId` query parameter of run, trace, tests and benchmarks. The tests summary asks
  `StaleTableIds.currentIdOf` when the retained run holds no test table of the id. A run started by an old id keeps
  that id for its WebSocket destinations and its result registry.
- **Not covered** — a table renamed outside Studio and moved to another cell at once; a table moved to another
  module; a module file moved outside Studio or in a project the session does not have open; a table deleted
  outside Studio, which answers `404`.

## Rejected Alternatives

- **Anchors in the workbook** — a hidden defined name on each table's header cell would carry the id through
  renames and to other Studio instances. Every Studio write that moves a table, and the workbook merge, would have
  to keep the names in step with their tables; that complexity outweighs the renames it would cover.
- **Accepting old position ids** — they are internal, not a public API, so they are neither accepted nor migrated.
- **The project in the id** — the project is already in the address, and its name or id in the identity would
  change every id on a rename or move.
- **Parameter types in the id** — a signature edit would change the id. Overloads that differ only in their
  parameters are told apart by their order, or by the `id` property.
- **Properties in the id** — editing a dimension property or `version` would change the id. Versions of a table
  usually live in modules of their own, named by their properties; the rare versions kept in one workbook are told
  apart by their order, or by the `id` property.
