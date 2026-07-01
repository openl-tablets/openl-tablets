# REST API Access with Tokens

- **Status** — Accepted. The core shipped as [Client Sessions](client-sessions.md); §4 is the remaining plan
- **Date** — 2026-10-23
- **Tickets** — [EPBDS-14371](https://jira.eisgroup.com/browse/EPBDS-14371) (client scope),
  [EPBDS-16314](https://jira.eisgroup.com/browse/EPBDS-16314) (concurrent workspace manager),
  [EPBDS-16979](https://jira.eisgroup.com/browse/EPBDS-16979) (writes of one client one after another),
  [EPBDS-16757](https://jira.eisgroup.com/browse/EPBDS-16757) (transfer links)
- **Scope** — how a client that authenticates every request with a token — a personal access token, a token of the
  identity provider, Basic authentication — works with the REST API of OpenL Studio without a cookie, and what it
  still lacks. Who a request acts as is [Authentication Precedence](auth-precedence-header-over-cookie.md); the API
  contract and the operations around it are
  [REST/Token Access — Contract and Operations](rest-token-contract-and-ops.md).

---

## 1. Context

A token client — the MCP server built into Studio, a CI job, a service — sends its credential in every request and
keeps no cookie. Until EPBDS-14371 every such request opened an HTTP session, compiled the project again in it and
left a copy of the project in memory for 30 minutes.

What a token client needs between its requests:

- **The working copy** — the projects the user opened and their unsaved changes. It is per user already:
  `MultiUserWorkspaceManager` keeps one `UserWorkspace` per user, counts its holders
  (`acquireUserWorkspace`/`releaseUserWorkspace`) and releases it with the last one.
- **The derived state** — the compiled project (`WebStudio` and its `ProjectModel`), the running compilation, the
  test, run and benchmark results, the debug session, merges, merge conflicts and comparisons.

## 2. Decision

**The derived state is kept per credential, under the client scope. The working copy stays per user. Selection
travels with each request.**

- **D1 — Per credential, not per principal.** `ClientSessions` keeps the beans of `@ClientSessionScope` for each
  credential: a PAT by its public id, an identity-provider token by the sign-in (`sid`), anything else by the user. A
  browser keeps them in its HTTP session. In `single` mode every request shares the one user's state.
  - **Why not per principal** — two PATs of one user are two clients: an agent and a CI job of the same user must not
    cancel each other's test run or switch each other's current project. Sharing one compiled model and one job
    registry across all channels of a user is the deferred alternative of
    [Principal-Scoped Shared State](principal-scoped-shared-state.md).
  - **Why not per token value** — an identity-provider sign-in refreshes its access token every few minutes; keyed
    by the value, every refresh would compile the project again. The `sid` stays the same.
- **D2 — The token authenticates; it does not hold state.** An expired or revoked PAT, or a token the identity
  provider no longer accepts, answers `401` before any controller runs, so it never reaches the state kept for it.
  That state ends after the idle timeout of an HTTP session, like the session of a browser that stopped calling.
  A JWT is checked by its own `exp`, without the identity provider: one revoked at the provider authenticates until
  it expires.
- **D3 — Selection is part of the request.** Every REST endpoint names the project in its path (`{projectId}`,
  [Stable Project Ids](project-ids.md)) and the module, the table and the branch in its parameters. The endpoints
  that read a selection kept on the server — `InitController`, `WorkspaceCompileController`,
  `OpenLMessageController`, `NotificationController`, `TestDownloadController` — are gone with the JSF pages.
- **D4 — One client has one current project.** `WebStudio` compiles one module at a time, and a request opens the
  module it names in its client's `WebStudio` (`WorkspaceProjectService.openProject`). Two requests of one credential
  on different projects switch it back and forth and compile on every switch, as two tabs of one browser do.
  Writes of one client run one after another inside its `WorkbookWrites` lock
  ([Project Editing Lock](project-editing-lock.md)). Clients that work in parallel on different projects use a
  credential each. Rejected for now: several compiled models per client (§3, G3).
- **D5 — A transfer link acts for the client that issued it.** A link of
  [MCP File Transfer Links](mcp-file-transfer.md) carries no credential; it stores the key of its client
  (`ClientSessions.currentKey`) and its request enters that client (`ClientSessions.enter`): it finds the client's
  pending merge conflict and compiled project, and keeps the client from being idle while it runs. A key whose client
  ended enters nothing, and the link is refused; the links of a client end with it. A browser session has no key, so
  `POST /rest/transfer-links` refuses a browser with `400`.

## 3. Open gaps

- **G1 — `LocalWorkspaceManagerImpl` is not thread-safe.** Its `localWorkspaces` is a plain `HashMap`:
  `getWorkspace` runs `computeIfAbsent` on it and `workspaceReleased` removes from it, both unsynchronized. Two
  first requests of one user, a browser and a token at once, can corrupt the map. `MultiUserWorkspaceManager` was
  fixed in EPBDS-16314; this one was not.
- **G2 — One job of each kind per client.** The test, run and benchmark registries (`AbstractExecutionResultRegistry`)
  hold one task and cancel the previous one when a new one starts; `DebugSessionRegistry` holds one debug session.
  Two parallel test runs of one credential — on two tables or two projects — leave only the second.
- **G3 — Everything that reads the current project.** Under D4 these follow whichever request switched the client
  last, not the request itself:
  - `ProjectObjectMapperService.createObjectMapper` serializes run, test, benchmark and debug results with the
    Jackson configuration of the current project;
  - `ProjectHistoryController.restore` passes the client's `WebStudio`;
  - `ProjectsMergeLauncher` resets the client's `WebStudio` and opens the merged project in it;
  - a request without `module` or `fromModule` takes the module the client already has open.
- **G4 — Two clients of one user write one working copy.** Each client compiles its own model and holds its own
  workbooks, and `WorkbookWrites` serializes only the writes of one client. The project lock is held in the user's
  name, so it does not tell a browser and a token of the same user apart. A client that writes a workbook another
  client of the user changed after its module read it writes the older content back over that change: no write
  compares the file with the one its module read.
- **G5 — The authorities of a client are those of its first request.** `WebStudio` captures the `Authentication`
  of the request that created it and `runAsSessionUser` replays it when it publishes the status of a compilation
  and when the design repository changes. A client kept by `sid` lives across many refreshed tokens; a role removed
  from the user in the meantime still applies there until the client ends.
- **G6 — No bound on the number of clients.** Every credential that calls compiles its own copy of a project and
  keeps it for the idle timeout. A user with a browser, an agent and a CI job holds three compiled copies; a script
  that creates a PAT per run holds one per run. Nothing caps the clients of a user or of the server, and nothing
  reports how many there are.

## 4. Plan

| Step | Work | WP |
|---|---|---|
| 1 | G1 — `LocalWorkspaceManagerImpl` over a `ConcurrentHashMap`, get and release together, with a concurrency test | 0.5 |
| 2 | G6 — report the clients kept (count, per user) in the log and the system information; a setting that caps the clients of one user and ends the least recently used one beyond it | 1 |
| 3 | G2 — key the result registries by the target of the job (project, module, table) and keep a few per client, bounded, so a new job cancels only the job of the same target | 1.5 |
| 4 | G4 — before a write, compare the workbook on disk with the one the module read, and reload the module when another client changed it; the stamp `ProjectModel.isModified` reads is the starting point | 1.5 |
| 5 | G5 — publish the status and react to repository changes as the user of the latest request of the client, not of the first | 1 |
| 6 | G3 — take the Jackson configuration, the history restore and the merge from the project of the request; needs the `ProjectModel` ports of [Decoupling](decoupling-projectmodel-from-webstudio.md) | 2 |
| 7 | Tests — two clients of one user writing one workbook; two jobs of one credential on two targets; ≥80% diff coverage | 1.5 |
| | **Total** | **9** |

Steps 1 and 2 stand alone and go first. Step 6 is worth it only together with several compiled models per client,
which D4 rejects for now: it becomes the next step once a client is shown to need parallel work on two projects under
one credential.

## 5. Acceptance criteria

1. **Token only, full lifecycle** — a client that sends only its `Authorization` header, never a cookie, lists,
   opens, edits, saves, compiles, runs and tests a project, and no HTTP session is created. Holds today; the
   `task_EPBDS-14371` ITEST of the SSO suite covers creating, opening, testing and closing with a bearer token.
2. **One working copy, two clients** — a browser and a token of one user writing the same workbook one after another
   both keep their change (step 4).
3. **Two jobs, one credential** — two test runs of one credential on two tables both complete and both can be read
   (step 3).
4. **Revocation** — a request with a revoked or expired PAT answers `401` and reads nothing kept for it; the state
   kept for it ends within the idle timeout. Holds today.
5. **Bounded memory** — with a cap of `n` clients per user, the `n+1`-th credential of the user ends the least
   recently used client and its compiled project (step 2).
