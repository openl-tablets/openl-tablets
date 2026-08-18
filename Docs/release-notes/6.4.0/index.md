---
title: OpenL Tablets 6.4.0 Release Notes
date: 2026-08-18
description: Turns Trace into an interactive step debugger with a business view, rebuilds table creation and the
    Projects screens in React, and creates and deletes projects in any Git branch. Project changes now reach open
    pages over WebSocket. Project deletion is permanent and Git commit messages are stored verbatim.
---

OpenL Tablets **6.4.0** reworks Trace into an interactive step debugger. A rule can be paused, stepped into, over and
out of, and inspected frame by frame, with a business view that explains a decision-table outcome in plain language
and an Advanced view for the full debugger. Large rules that previously failed to trace now run to completion.

The move of OpenL Studio to React continues: the Projects and Deployments tabs reach production parity with the
legacy JSF Repository screens, which are removed, and the table creation and copy wizards become React modals that
cover every OpenL table type. Project and workspace changes are pushed to open pages over WebSocket, so edits made by
another session, an MCP client or an external Git push appear without a manual reload.

Projects can now be created and deleted in any branch, not only the default one. Two changes alter established
behavior and need review before upgrading: projects are deleted permanently instead of being archived, and Git commit
messages are stored exactly as entered rather than wrapped in a configurable envelope.

## Highlights

* Trace is an interactive step debugger with breakpoints, watches and a plain-language business view.
* Table creation and copying are React modals covering all fifteen OpenL table types.
* Projects and Deployments run entirely on React; the legacy JSF Repository screens are gone.
* Projects can be created and deleted in any Git branch.
* Project and workspace changes reach open pages over WebSocket instead of waiting for a reload.
* Deleting a project is permanent and recorded as an ordinary Git delete commit.
* Revoking access takes effect immediately and no longer turns a project into a local copy.

## New Features

### Interactive Rule Debugger

Trace no longer runs a rule to the end and builds its whole execution at once. It starts from a selected test case or
a custom input and can be paused, resumed, stopped and stepped through — into, over and out of called rules — so an
author can stop where it matters and inspect what happened.

* Breakpoints pause execution when it reaches a chosen table, step or rule.
* Each pause shows the chain of rules that led to the current point, the inputs and values reached so far, and
  highlights the current line on the table.
* Decision tables are broken into their conditions in the trace tree, and the fired rule and its result are
  highlighted so it is clear which rule matched and how each condition evaluated.
* Watches, per-step and per-rule execution times, and the applied version of a versioned rule are shown.
* Errors are surfaced in business terms, naming the failing rule and its location, with the failed path opened so the
  error shows at once.

The trace opens in a **business view** aimed at rule authors — no spreadsheet grid, no decision panel, classic
detailed titles, and the tree marked by the current call path — with the full debugger kept behind an **Advanced**
switch. The business tree is downloaded in one capped request rather than thousands of lazy pages, so a
production-sized rule can be traced to completion.

The debugger is driven by a REST API under `/rest/projects/{projectId}/trace`, with `POST /step`, `POST /resume`,
`POST /pause`, `PUT /breakpoints`, `GET /watches` and frame-scoped reads such as `GET /frames/{index}/variables` and
`GET /frames/{index}/highlights`.

### Table Creation and Copying in React

The table creation and copy wizards are replaced by React modals built around the header cell OpenL actually
compiles. The dialog is a settings strip over the sheet the table will be written to: what it shows is exactly what
is written to the workbook, and the name OpenL compiles is read back out of the generated header the same way the
compiler reads it — never from the Name field.

Fifteen table types are supported, each with a body the dialog knows how to lay out: Datatype, Vocabulary, Constants,
Spreadsheet, Smart Rules, Simple Rules, Smart Lookup, Simple Lookup, Rules, Test, Run, Data, Environment, Properties
and Free Form. Lookup types build a two-dimensional matrix, values are entered through inputs typed to the column
they fill, and a Datatype's optional **Extends** setting writes `extends <parentType>` into the header.

Copying a table reuses the same path: the browser reads the raw cell matrix of the source, applies the header and
property changes the author selected, and submits the result through the table-creation API. The copy itself runs on
the server by table id instead of re-posting the table content.

### React Projects and Deployments

The Projects and Deployments tabs replace the legacy JSF Repository screens, which are removed along with the
RichFaces upload dialogs, the jQuery loading panel and the JSF user-profile and confirmation popups.

* Projects are grouped by repository on a first visit, with a filter rail that hides states no project is in.
* Search covers author and branch in addition to name and tag, and project actions collapse into an overflow menu
  instead of wrapping.
* A project can be created from a template, an archive, an existing project or a folder, with its name pre-filled
  from the source and its initial status selectable.
* Compilation state is shown as one consistent chip across every projects view, and the list loads only message
  counts rather than full compilation messages.
* Text files can be edited directly in the editor.
* Switching between React screens happens in place instead of reloading the page.

Project descriptors and tags are cached per revision, so the used-by column and the facet counts no longer re-read
every `rules.xml` on each request.

### Projects in Any Branch

A project can be created and deleted in a non-default branch. Project discovery derives membership from Git trees, so
a project is found when at least one readable branch contains it, and it stays one logical workspace item while its
content, path and permissions differ per branch.

Each branch membership gets its own repository view, mapped path and file metadata, a deterministic home branch and a
user-specific effective branch are selected per project, and authorization is applied separately to every branch
membership. Default and protected branches are marked across the project screens and can be searched. Project-list
and project-read requests no longer trigger repository-wide branch or history scans.

### Live Project and Workspace Updates

Changes now reach open pages over the existing STOMP/WebSocket channel instead of waiting for a manual reload:

* `/topic/projects/changed` — a broadcast ping when the design repository changes, including commits from other users
  and external Git pushes, debounced against merge and copy bursts.
* `/user/topic/workspace/changed` — a per-user ping when a draft edit is made through any entry path, so an edit made
  by an MCP client or the editor flips the project to Editing at once.
* `/user/topic/workspace/projects/status` — per-project compilation status for the subscribing user.

Broadcast topics carry no project data; clients re-read through REST under the usual access control. External pushes
surface within the repository polling period.

### Data Model Visualization

The tables dependency graph draws the project's data model as an ER diagram of typed entities, so datatypes and the
relations between them can be read alongside the table dependencies they participate in.

### Startup and Readiness Health Checks

OpenL Studio serves two unauthenticated endpoints for container orchestration:

```text
GET /healthcheck/startup     -> 200 UP
GET /healthcheck/readiness   -> 200 READY
```

Any other path under `/healthcheck` stays behind the security filter. The Kubernetes example manifests use them for a
startup probe and an HTTP readiness probe, which removes the fixed `initialDelaySeconds` guesswork:

```yaml
startupProbe:
  httpGet:
    path: /healthcheck/startup
    port: 8080
  periodSeconds: 10
  failureThreshold: 30
livenessProbe:
  tcpSocket:
    port: 8080
  periodSeconds: 15
  failureThreshold: 4
readinessProbe:
  httpGet:
    path: /healthcheck/readiness
    port: 8080
  periodSeconds: 10
  failureThreshold: 3
```

## Improvements

### OpenL Studio Administration

* Improved the Users and Groups management tables, and added search to both tabs.
* Added editing of user data in single user mode through the OpenL Studio UI.
* Added a prompt for the profile data a user is missing after authentication.
* Moved the active user badge to Last Login.

### OpenL Studio User Interface

* Replaced the jQuery loading panel with the Ant Design spinner.
* Replaced the JSF confirmation popup with a React Ant Design dialog where data would otherwise be lost.
* Unified all OpenL Studio messages into one style, using Ant Design notifications.
* Replaced the RichFaces upload in the project and module update dialogs with the Ant Design upload.
* Moved the remaining editor toolbar dialogs to the React modals that already exist.
* Unified repository, branch and commit text sizing across the project screens, and replaced the monospace motif with
  the default Ant Design font.

### Repository and Workspace

* Moved project metainfo out of project folders into a per-user workspace registry, reconciled on sign-in.
* Added an edit lock to the REST files API, which previously modified a project without taking one.
* Added atomic folder uploads and a `REPLACE` conflict policy, so a folder upload is committed as one change.
* Added a permission check for mapped content paths, so access is evaluated against the path a project is mapped to.
* Improved branch deletion to check every project a deleted branch would take with it.
* Extended Sync to be offered whenever the repository has a branch to merge into, and merge to offer every repository
  branch as a target on request.
* Improved merge to run on a project that is not opened instead of refusing it.
* Added a prompt before opening the dependencies of a project, naming the ones that are off its branch.
* Improved commit ordering so commits sharing a commit-time second are ordered by ancestry.
* Added the design revision a deployed project was built from to the deployment view.
* Added validation of the properties file name settings when `rules.xml` is written, so invalid settings are reported
  on save instead of surfacing later as a compilation failure.

### REST API

* Added cell styles and row slicing to the raw table format.
* Added declaration of the modules an OpenAPI import generates.
* Improved the run result to name the executed table instead of the virtual suite.

### Maven Plugin

* Migrated the Maven archetype to a `pom.xml`-less structure, moving the simple project archetype to the top-level
  OpenL layout.

## Bug Fixes

* Fixed projects being converted to `LOCAL` when access was revoked, when the original project was deleted on Azure
  Blob storage, or when the Git URL in `webstudio.properties` was changed to an invalid one — the workspace refresh
  now keeps the repository link so every access check applies to the opened copy.
* Fixed revoked access remaining ineffective: the first project listing after a revocation now evicts the opened copy
  — files, metainfo record and lock — from the user workspace.
* Fixed a deleted project or file reappearing after a page refresh.
* Fixed a project not being creatable under the name of a deleted project.
* Fixed a locked project not preventing another user from deleting its branch.
* Fixed the errors raised when deleting a local project — a missing workspace folder, and commit info being demanded
  for a project created in a Git repository.
* Fixed a project failing to open and a duplicate `LOCAL` project being created when the commit carried no user.
* Fixed an empty Modules list on the project page, caused by the `<modules>` block being rewritten on Project Info
  and OpenAPI edits.
* Fixed implicit modules being dropped when an Excel module was added or copied.
* Fixed the profiled Trace failing on large runs, and added tooltips to the trace controls.
* Fixed lost and incorrect parameter-field inputs in trace step details, a business-mode node switch flashing the
  root table, and the Basic Trace error path on failed runs.
* Fixed an NPE from `StaticResourcesServlet` when the OpenL Studio context was requested without a trailing slash.
* Fixed the online user marker not appearing in the admin Users table in `multi` and `ad` modes.
* Fixed copy to clipboard failing on plain HTTP origins, where the Clipboard API is unavailable.
* Fixed the DEMO start script failing when its path contains spaces.
* Fixed URL-safe Base64 encoding of project and deployment identifiers, which broke redeployment when an identifier
  contained a slash.
* Fixed an NPE when creating a project from an OpenAPI file over REST.
* Fixed the table graph crashing because Prototype.js clobbered `Object.values`.
* Fixed the endless editor reload after the shell page expires.
* Fixed the sheet name mirrored from a table name exceeding Excel's length limit.

## Breaking Changes

This section summarizes changes that may require action before or after upgrading.

* **Project deletion is permanent** — The two-phase Archive, Undelete and Erase lifecycle is removed. Deleting a
  project removes it from the user's workspace and from the current state of the Design repository, and for Git
  repositories the change is stored as an ordinary delete commit, so repository history keeps the deletion event. An
  open project is closed before removal. There is no Undelete action.
* **Git commit messages are stored verbatim** — The commit message template is removed, along with the
  `comment-template` property and its `{user-message}`, `{commit-type}`, `{project-name}` and `{revision}`
  placeholders. Git repositories store the entered comment directly as the commit message. The user message pattern
  and its hint remain configurable.
* **The legacy JSF Repository screens are removed** — Bookmarks and deep links into the old Repository pages no
  longer resolve.
* **Manual project import is removed** — Importing a project from a repository is superseded by automatic project
  detection.
* **The project descriptor size limit is 1 MB** — A `rules.xml` above 1 MB is refused, where the previous limit was
  16 MB.
* **gRPC is no longer a dependency** — The obsolete Studio AI search integration is removed together with its gRPC
  dependency and `OpenL2TextUtils`, which existed only for text-based search indexing.

## Library Updates

### Runtime Dependencies

| Library             | Version                          |
|:--------------------|:---------------------------------|
| Jetty               | 12.1.12 (from 12.1.10)           |
| Netty               | 4.2.17.Final (from 4.2.15.Final) |
| Jackson             | 2.22.1 (from 2.22.0)             |
| CXF                 | 4.1.8 (from 4.1.7)               |
| Hibernate ORM       | 6.6.55.Final (from 6.6.54.Final) |
| Hibernate Validator | 8.0.5.Final (from 8.0.4.Final)   |
| Log4j               | 2.26.1 (from 2.26.0)             |
| OpenTelemetry       | 2.30.0 (from 2.29.0)             |
| OpenSAML            | 5.2.3 (from 5.2.2)               |
| Apache HttpClient5  | 5.6.4 (from 5.6.2)               |
| Swagger Core        | 2.2.53 (from 2.2.52)             |
| Swagger Parser      | 2.1.46 (from 2.1.45)             |
| Groovy              | 4.0.33 (from 4.0.32)             |
| Guava               | 33.7.0-jre (from 33.6.0-jre)     |
| Bouncy Castle       | 1.85.2 (from 1.84)               |
| Byte Buddy          | 1.18.11 (from 1.18.10)           |
| Commons Codec       | 1.22.1 (from 1.22.0)             |
| Commons Collections | 4.6.0 (from 4.5.0)               |
| JSpecify            | 1.0.1 (from 1.0.0)               |
| Micrometer          | 1.17.0 (*newly pinned*)          |
| lz4-java            | 1.11.2 (*newly pinned*)          |
| Oracle JDBC         | 23.26.3.0.0 (from 23.26.2.0.0)   |
| PostgreSQL JDBC     | 42.7.13 (from 42.7.12)           |
| MariaDB JDBC        | 2.7.15 (from 2.7.14)             |

Micrometer is now pinned explicitly to address a denial-of-service vulnerability (CVE-2026-40984) in the version
Spring LDAP pulled in transitively. lz4-java is pinned to override the version Kafka brings in, addressing
CVE-2026-59949. gRPC is no longer a dependency; it was removed with the AI search integration.

### Test Dependencies

| Library                 | Version              |
|:------------------------|:---------------------|
| JUnit                   | 6.1.3 (from 6.1.1)   |
| XMLUnit                 | 2.13.0 (from 2.12.0) |
| Testcontainers Keycloak | 4.3.1 (from 4.2.1)   |
| GreenMail               | 2.1.12 (from 2.1.9)  |

### Build Tooling

| Tool     | Version                  |
|:---------|:-------------------------|
| Node.js  | v24.18.0 (from v24.17.0) |
| npm      | 12.0.1 (from 11.17.0)    |
| NullAway | 0.13.8 (from 0.13.7)     |
