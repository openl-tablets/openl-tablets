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

`{projectId}` in a path is the project id that `GET /rest/projects` hands out, or the project name. A name that matches
several projects answers `409 Conflict` with `openl.error.409.project.identifier.ambiguous.message` and the list of the
candidates. An id that matches no project answers `404 Not Found`.

### Pages of Results

A list endpoint takes `size` (the page size, `50` by default) and either `page` (the zero-based page number) or `offset`
(the zero-based index of the first item), or `unpaged=true` for all the results. It answers
`{"content": [...], "pageNumber": 0, "pageSize": 50, "numberOfElements": 50, "total": 120}`. `total` is absent when the
total is not known.

### Response Fields

A response carries the fields of its type, minus the empty values. The query parameter `fields` keeps only some of them:
`fields=name,owner(email)` keeps `name` and the `email` of `owner`. An unknown field is dropped. A malformed selection, or
one longer than 4096 characters, deeper than 16 levels, or with more than 256 names, answers `400`.

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
  "rejectedValue": ...}]}`.
- **`401 Unauthorized`** — the request has no valid credentials.
- **`403 Forbidden`** — the user may not do it, or the endpoint is closed to personal access tokens. A refusal by a
  security rule answers `{"message": "Access Denied"}`.
- **`404 Not Found`** — the project, the table, or another resource does not exist.
- **`409 Conflict`** — the state of the project does not allow it: the project is not opened, is locked by another user,
  has unresolved merge conflicts, or the name is ambiguous.

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
- **Table id** — `{tableId}` is an opaque `id` from the table list. A write that has to move the table changes it.
- **Module** — the optional query parameter `module` reads or writes the table through one module. The answer is ready
  once that module is compiled, without waiting for the rest of the project. Without it the endpoint waits for the whole
  project.
- **Responses of a write** — `204 No Content` when the table keeps its id. `200 OK` with `{"id": "<new id>"}` and a
  `Location` header when the table had no room to grow and moved.
- **Lock** — a write locks the project for the user, and a write by another user answers `409` with
  `openl.error.409.project.locked.by.message`; see [The Project Editing Lock](../architecture/project-editing-lock.md).
- **Partial tables** — a table that is written as several partial tables is read, and a write to it answers `400` with
  `openl.error.400.table.partial.message`. Excel edits such a table.

**Read-only fields of every table model:**

- `messages` — the compilation messages of the table. A message has an `id`, a `summary`, a `severity` (`INFO`, `WARN`, or
  `ERROR`), a `location` (the `type` `module` or `table`, and where it is), and `stacktrace` when a stack trace is
  available.
- `runState` — with `runState=true`: `can-run`, `can-run-module` (runs against the module that holds the table only), or
  `cannot-run`.
- `partial` — `true` for a table that is written as several partial tables.

**Cell values** in the models are a string, a number, a boolean, or `null`. A date is an ISO 8601 string.
