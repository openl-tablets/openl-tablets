# Project Local History API

The Project Local History API reads, restores, and clears temporary workbook versions created while a module is edited
in OpenL Studio. It is separate from the committed project revisions stored in the design repository.

## Read Local History

```http
GET /projects/{projectId}/local-history?module={moduleName}
```

- **projectId** — URL-safe project identifier. The project must be opened in the caller's workspace.
- **module** — optional module name. A missing or blank value selects the first module in the project descriptor.

The project and module are resolved from the request. The endpoint does not use the project or module selected
in the HTTP session, so it can be called in a fresh session and cannot return another project's history.

The response is ordered from the newest local version to the oldest. The revision baseline is last. The
`current` field is present and `true` only for the workbook version currently being edited.

```json
[
  {
    "id": "1787821200000_current",
    "modifiedOn": "08/27/2026 12:00 PM",
    "current": true
  },
  {
    "id": "Revision Version",
    "modifiedOn": "Revision Version"
  }
]
```

An empty array means the module has no earlier local version to compare or restore.

## Restore a Local Version

```http
POST /projects/{projectId}/local-history/restore?module={moduleName}
Content-Type: application/json

{
  "version": "Revision Version"
}
```

The request body `version` field is the `id` of a non-current entry returned by the read endpoint. The project and
module are resolved from the request before the workbook is replaced. The caller must have write permission on the
selected module. If that workbook is also open in the HTTP session, OpenL Studio reloads its compiled model. A
different module open in the same session is left unchanged.

## Clear Project Local History

```http
DELETE /projects/{projectId}/local-history
```

The endpoint removes the local edit history of every module in the named project from the caller's workspace. It does
not affect another project or another user's workspace. The project must be opened, and the caller must have write
permission on it.

Administrators can still clear local history for every user and project from the System settings screen. That global
operation is explicitly exposed as `DELETE /admin/local-history`; it is separate from the project API.

## Errors

- `403 Forbidden` — restore requires write permission on the selected module; clearing requires write permission on
  the project.
- `404 Not Found` — the project identifier, module name, or local history entry does not resolve.
- `409 Conflict` — the project is not opened in the caller's workspace.

The former `GET /history/project`, `POST /history/restore`, and `DELETE /history` endpoints are removed. Callers must
use the project-scoped endpoints or the explicit administrator endpoint.
