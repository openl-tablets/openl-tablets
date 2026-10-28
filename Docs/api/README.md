# REST API Documentation

OpenL Studio publishes a REST API under `{context-path}/rest`, and OpenL Rule Services publishes a management API under
`{context-path}/admin`. These pages describe the parts of the API that need more than the generated reference.

## Reference

- **OpenAPI document** — `GET /rest/openapi.json`. It needs no sign-in and lists every endpoint of OpenL Studio.
- **API documentation page** — `/api-docs` in OpenL Studio, linked from the **Help** page as *Internal REST API
  Documentation*. It shows the OpenAPI document.
- **WebSocket** — `/ws`. The server pushes the changes of projects, compilation, comparison, and trace sessions; see
  [WebSocket Change Notifications](../architecture/websocket-change-notifications.md).

## Pages

### OpenL Studio

- [Data Tables API](data-tables-api.md) — read, replace, and append the rows of a Data table.
- [Test Tables API](test-tables-api.md) — read, replace, and append the test cases of a Test table.
- [Lookup Tables API](lookup-api.md) — read, replace, and append the rows of a SmartLookup or a SimpleLookup table.
- [Raw Tables API](raw-tables-api.md) — read and edit any table as a matrix of cells: values, merges, styles, rows,
  and columns.
- [Projects Merge API](projects-merge-api.md) — check and perform the merge of Git branches, and resolve the
  conflicts. [Architecture](projects-merge-architecture.md).
- [Project Local History API](project-local-history-api.md) — read, restore, compare, and clear the unsaved versions of
  a module.
- [Personal Access Token API](personal-access-token-api.md) — create and revoke the tokens for programs.
  [Architecture](personal-access-token-architecture.md).
- [Projects Trace API](projects-trace-api.md) — the interactive debugger of a table.
  [Architecture](projects-trace-architecture.md).

### OpenL Rule Services

- [Rule Services Admin API](rule-services-admin-api.md) — the deployed services, the health probes, and the deployment
  of projects.

## Conventions

### Authentication

- **`single`** — the user mode `single` has no authentication.
- **Other user modes** — a request carries the credentials that the mode accepts: the browser session, HTTP Basic in
  `ad` and `multi`, an OAuth2 bearer token in `oauth2`, or a
  [personal access token](personal-access-token-api.md) in every mode except `single`.
- **Public paths** — `/rest/openapi.json`, `/rest/settings`, and `/rest/public/**` need no credentials.

### Projects

`{projectId}` in a path is the project id `GET /rest/projects` hands out: 13 upper-case characters of Crockford's Base32, computed
from the repository and the folder that hold the project, so it stays the same when the project is opened, closed,
renamed in `rules.xml` or switched to another branch. An id that matches no project answers `404 Not Found`, and so do
a project name and an id of OpenL Studio 6.x. While the server runs, the id a project had before its folder moved
outside OpenL Studio still names it: the request is served for the project, and the `Content-Location` header gives
the address under its current id. The id of a deleted project answers `410` with
`openl.error.410.project.deleted.message`. See [Stable Project Ids](../architecture/project-ids.md).

The status of a project tells `changedOnDisk` when another program changed the files of the project since the session
compiled it. A client asks it in short before working with the tables,
`GET /rest/projects/{projectId}?include=status&fields=compileStatus(compileState,changedOnDisk)`, and compiles the
module again with `POST /rest/projects/{projectId}/modules/{module}/compile?reset=true` when the files changed.

### Pages of Results

A list endpoint takes `size` (the page size, `50` by default) and either `page` (the zero-based page number) or `offset`
(the zero-based index of the first item), or `unpaged=true` for all the results. It answers
`{"content": [...], "pageNumber": 0, "pageSize": 50, "numberOfElements": 50, "total": 120}`. `total` is absent when the
total is not known.

### Response Fields

A response carries the fields of its type, minus the empty values. The query parameter `fields` keeps only some of them:
`fields=name,owner(email)` keeps `name` and the `email` of `owner`. An unknown field is dropped. A malformed selection, or
one longer than 4096 characters, deeper than 16 levels, or with more than 256 names, answers `400`, and a request that
creates or changes data then changes nothing.

A field left out may cost nothing. `GET /rest/projects/{projectId}` looks for the projects using this one (`usedBy`),
reading the descriptor of every other project, only when the selection keeps them. It resolves the messages of the
status to their tables (`compileStatus.compilation.messages.items`), which waits for a compilation under way, only when
the selection keeps them or the names of the compiled modules.

### Errors

An error answers with a body of this form, where `<key>` is the message code, and `message` is its localized text:

```json
{
  "code": "openl.error.404.table.message",
  "message": "The table is not found."
}
```

- **`400 Bad Request`** — the request is malformed or refused by a rule of the endpoint. A request body that fails
  validation answers `{"message": "Bad Request", "fields": [{"code": "...", "message": "...", "field": "...",
  "rejectedValue": ...}]}`. A body field or a request parameter that breaks a constraint is coded by the constraint,
  such as `openl.error.NotBlank` or `openl.error.Size`, whatever message the endpoint gives it.
- **`401 Unauthorized`** — the request has no valid credentials.
- **`403 Forbidden`** — the user may not do it, or the endpoint is closed to personal access tokens. A refusal by a
  security rule answers `{"message": "Access Denied"}`.
- **`404 Not Found`** — the project, the table, or another resource does not exist.
- **`409 Conflict`** — the state of the project does not allow it: the project is not opened, is locked by another user,
  or has unresolved merge conflicts.
- **`410 Gone`** — the project or the table an id named was deleted.
- **`412 Precondition Failed`** — the version named in `If-Match` is not the current one: the table or the file changed
  since it was read, and nothing is written.

## Table Endpoints

The tables of a project have one set of endpoints for every table type. The table types differ in the JSON model that the
endpoints read and write, and in `tableType` that selects it.

- **`GET /rest/projects/{projectId}/tables`** — a page of the tables of the project, with the `id` of each. The filters
  are `kind`, `name`, `module`, `scope`, `header`, `text`, and `properties.<name>`, and `includeOther=true` adds the tables
  that OpenL does not recognize.
- **`POST /rest/projects/{projectId}/tables`** — creates a table in a module and answers `201 Created` with its summary.
- **`GET /rest/projects/{projectId}/tables/{tableId}`** — reads the table in the model of its type, or, with `raw=true`,
  as a matrix of cells.
- **`PUT /rest/projects/{projectId}/tables/{tableId}`** — replaces the content of the table.
- **`POST /rest/projects/{projectId}/tables/{tableId}/lines`** — appends lines to the table.
- **`POST /rest/projects/{projectId}/tables/{tableId}/actions`** and **`.../actions/batch`** — edit the raw source; see
  [Raw Tables API](raw-tables-api.md#editing-the-source).
- **`PATCH /rest/projects/{projectId}/tables/{tableId}/properties`** — updates the properties of the table.
- **`DELETE /rest/projects/{projectId}/tables/{tableId}`** — deletes the table.

Table types: `Datatype`, `Vocabulary`, `Spreadsheet`, `SimpleSpreadsheet`, `SimpleRules`, `SmartRules`, `SmartLookup`,
`SimpleLookup`, `Data`, `Test`, and `RawSource`. The write endpoints take the model of the type in `tableType`.

**Rules for every table endpoint:**

- **Opened project** — the project is opened first with `PATCH /rest/projects/{projectId}` and
  `{"status": "OPENED"}`. Otherwise the answer is `409` with `openl.error.409.project.not.opened.message`.
- **Table id** — `{tableId}` is an opaque `id` from the table list, made of the module, the kind and the name of the
  table, or its `id` property. A table keeps it when it moves, on its sheet or to another sheet, and gets a new one
  when it is renamed or its `id` property changes. An id the table had before a rename still names it while the
  server runs: the request is served for the table, and the `Content-Location` header gives the address under the
  current id. An id of a table OpenL Studio deleted answers `410` with `openl.error.410.table.deleted.message`; see
  [Stable Table Ids](../architecture/table-ids.md).
- **Module** — the optional query parameter `module` reads or writes the table through one module. The answer is ready
  once that module is compiled, without waiting for the rest of the project. Without it the endpoint waits for the whole
  project.
- **Responses of a write** — `204 No Content` when the table keeps its id, also when it had no room to grow and
  moved. `200 OK` with `{"id": "<new id>"}` and a `Location` header when the write renamed the table or changed its
  `id` property, or when the request named the table by an id it had before.
- **Lock** — a write locks the project for the user, and a write by another user answers `409` with
  `openl.error.409.project.locked.by.message`; see [The Project Editing Lock](../architecture/project-editing-lock.md).
- **Partial tables** — a table that is written as several partial tables is read, and a write to it answers `400` with
  `openl.error.400.table.partial.message`. Excel edits such a table.
- **Version** — a read tells the version of the table in the
  [`ETag`](https://www.rfc-editor.org/rfc/rfc9110#section-8.8.3) header; the body carries none. A write that names it in
  the [`If-Match`](https://www.rfc-editor.org/rfc/rfc9110#section-13.1.1) header is refused with
  [`412`](https://www.rfc-editor.org/rfc/rfc9110#section-15.5.13) and `openl.error.412.table.changed.since.read.message`
  when the table changed since the read, on disk or through another client, and nothing is written. A write without the
  header, or with `*`, is made over any version; a weak tag matches none. A write of a workbook whose file another
  program changed since OpenL Studio read it answers `409` with `openl.error.409.workbook.changed.outside.message`; see
  [Changes Made Outside OpenL Studio](../architecture/changes-made-outside.md).

**Read-only fields of every table model:**

- `messages` — the compilation messages of the table. A message has an `id`, a `summary`, a `severity` (`INFO`, `WARN`, or
  `ERROR`), a `location` (the `type` `module` or `table`, and where it is), and `stacktrace: true` when a stack trace
  is available, which `GET /rest/projects/{projectId}/messages/{messageId}/stacktrace` reads.
- `runState` — with `runState=true`: `can-run`, `can-run-module` (runs against the module that holds the table only), or
  `cannot-run`.
- `partial` — `true` for a table that is written as several partial tables.

The version of the table read is not a field of the model: the `ETag` header of the read answers it, and a write names
it in `If-Match`. It changes whenever the text of a cell of the table changes.

**Cell values** in the models are a string, a number, a boolean, or `null`. A date is an ISO 8601 string.
