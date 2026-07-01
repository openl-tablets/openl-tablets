---
title: OpenL Tablets 6.3.0 Release Notes
date: 2026-06-30
description: Adds a REST API for granular, in-place editing of table sources that powers efficient large-table
    manipulation in the OpenL MCP server, extends Personal Access Tokens to Active Directory and multi-user modes.
    Fixes a Metaspace leak, a trace StackOverflowError, and SAML single logout.
---

OpenL Tablets **6.3.0** introduces a REST API for granular, in-place editing of table sources, which lets the OpenL MCP
server change large tables one cell or one row at a time instead of resending the whole table. It also extends Personal
Access Token authentication to Active Directory and multi-user deployments.

The release resolves a critical `OpenLClassLoader`/Metaspace leak that crashed long-running servers, a trace
`StackOverflowError` on self-referential spreadsheets, and SAML Single Logout, which previously left the identity
provider session active. The SAML logout fix changes the observable logout behavior — review it before upgrading.

## New Features

### Granular Table Source Editing API

OpenL Studio adds a REST endpoint that applies a single, addressable edit to the raw source of a table in the currently
opened project, selected by the `operation` field of the request body:

```text
POST /rest/projects/{projectId}/tables/{tableId}/actions
```

* `update` — overwrite a single `cell`, an entire `row` or `column`, or a rectangular `range` (a block of cells) in
  place.
* `insert` and `append` — add a row or column (or a block of rows or columns) at a position, or at the end of the
  table.
* `delete` — remove a row or column (or a block) at a position.
* `merge` and `unmerge` — merge a rectangular range of cells, or unmerge the merged cell that covers a position.

Coordinates are 0-based and address the matrix returned by `GET /rest/projects/{projectId}/tables/{tableId}?raw=true`.
The table type is never interpreted, so the same operations work on any table — decision, lookup, spreadsheet,
datatype, or test.

```json
{
  "operation": "update",
  "target": {
    "type": "cell",
    "row": 5,
    "column": 2,
    "value": "Buenos Dias"
  }
}
```

When an edit leaves the table in place, the endpoint returns `204 No Content`. When the table has no room to grow and is
relocated on the sheet, its ID changes and the endpoint returns `200 OK` with the new table ID in the body and the new
table URL in the `Location` header. Malformed edits are rejected with `400 Bad Request` — for example a row wider than
the table, a blank line that would split the table and drop the data beyond it, a merge that would discard data, or a
first cell that does not start with a recognized OpenL table type.

A companion endpoint deletes a table, clearing its entire area from the sheet regardless of type:

```text
DELETE /rest/projects/{projectId}/tables/{tableId}
```

Together these endpoints back the OpenL MCP server's large-table tools: a single cell or row can be changed in one call,
which keeps edits within MCP message and token budgets and avoids clobbering concurrent changes.

## Improvements

### OpenL Studio

* Extended Personal Access Token (PAT) authentication to every authenticated user mode — `oauth2`, `saml`, `ad`
  (Active Directory / LDAP) and `multi` (multi-user), that is, all modes except `single`. It was previously limited to
  `oauth2` and `saml`. The `PatAuthenticationFilter` is now wired into the form-based (`ad` / `multi`) `/rest/**`
  security chain ahead of HTTP Basic authentication, and the Personal Access Tokens screen appears in all authenticated
  modes. Active Directory and internal multi-user deployments can now use token-based, service-to-service REST access as
  an alternative to interactive login. PAT remains disabled in `single` mode.

## Bug Fixes

* Fixed an `OpenLClassLoader` and Metaspace leak that crashed long-running servers after a few hours of rule execution.
  The proxy factory generated a uniquely named class on every `newInstance()` call, so identical generated classes piled
  up in the long-lived class loader; proxy classes now use a deterministic name derived from their interfaces and are
  generated once and reused.
* Fixed a `StackOverflowError` ("Failed to load node details") when tracing a self-referential `SpreadsheetResult`
  table. JSON-schema generation for the recursive bean now fails safe and yields no schema instead of aborting the
  trace, run-result, and test-summary responses.
* Fixed table updates leaving stale merged cells when editing a lookup table through the tables API. Merges removed from
  the new source lingered in the grid, producing an incorrect merge layout and extra empty return values; merges inside
  an updated area are now cleared before the new merges are applied.
* Fixed double-escaped cell content on the Trace screen, where a value such as `"<>"` was displayed as `<>`. Cell
  content is already HTML-safe, and re-escaping it turned `&lt;` into `&amp;lt;`, which the UI rendered literally; it is
  now emitted verbatim.
* Fixed SAML Single Logout. Logout cleared only the local session and the identity provider immediately re-authenticated
  the user, because the logout filter used a no-op handler. Logout now sends a SAML `LogoutRequest` to the identity
  provider's Single Logout Service and ends the IdP session.

## Breaking Changes

This section summarizes changes that may require action before or after upgrading.

* **SAML logout behavior** — In `saml` mode, logout now redirects through the identity provider's Single Logout
  Service and ends the IdP session, instead of clearing only the local OpenL Studio session. Deployments whose
  identity provider does not expose a Single Logout Service endpoint should verify their logout flow after upgrading.

## Library Updates

### Runtime Dependencies

| Library              | Version                          |
|:---------------------|:---------------------------------|
| Spring Boot          | 3.5.16 (from 3.5.15)             |
| Spring Integration   | 6.5.10 (from 6.5.9)              |
| Hibernate ORM        | 6.6.54.Final (from 6.6.53.Final) |
| gRPC                 | 1.82.1 (from 1.82.0)             |
| Apache Kafka Client  | 4.3.1 (from 4.3.0)               |
| Swagger Parser       | 2.1.45 (from 2.1.44)             |
| Apache HttpClient5   | 5.6.2 (*newly pinned*)           |
| Apache HttpCore5     | 5.4.3 (*newly pinned*)           |
| Spring LDAP          | 3.3.8 (*newly pinned*)           |

Apache HttpClient5, Apache HttpCore5, and Spring LDAP arrive as transitive dependencies and are now pinned explicitly to
current patched releases.

### Test Dependencies

| Library                | Version                |
|:-----------------------|:-----------------------|
| JUnit                  | 6.1.1 (from 6.1.0)     |
| PostgreSQL JDBC Driver | 42.7.12 (from 42.7.11) |
