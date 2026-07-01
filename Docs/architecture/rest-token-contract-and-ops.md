# REST/Token Access — Contract and Operations

- **Status** — Proposed; companion of [REST API Access with Tokens](rest-api-token-access.md)
- **Date** — 2026-10-23
- **Scope** — what a token client of the `/rest` API of OpenL Studio can rely on — selection, asynchronous jobs,
  status codes — and what running such clients needs: scopes of a token, audit, realtime, clustering, limits,
  observability. Each item states the behavior today and the proposal.

---

## 1. Already in place

These items of the earlier proposal shipped, sometimes in another form:

| Topic | Behavior | Where |
|---|---|---|
| State without a session | kept per credential, ends after the idle timeout | [Client Sessions](client-sessions.md) |
| Project id | 13 upper-case Crockford Base32 characters, safe in a path and a STOMP destination; a deleted project answers `410` | [Stable Project Ids](project-ids.md) |
| A locked project | `409` naming the holder; the write right is checked first, so a reader gets `403` | [Project Editing Lock](project-editing-lock.md) |
| Writes of one client | one after another, inside `WorkbookWrites` | [Project Editing Lock](project-editing-lock.md) |
| A job not finished yet | `202` with `{"status":"notReady"}` | `ResultNotReadyView` |
| Run or debug of a table with compile errors | `409` | `ProjectsRunController`, `ProjectsTraceDebugController` |
| Audit | sign-ins with the PAT public id, PAT creation and deletion, project events, each with its authentication method | [Audit Log](audit-log.md) |
| Realtime for a header client | the STOMP handshake at `/ws` takes the same credentials as `/rest` | [WebSocket Change Notifications](websocket-change-notifications.md) |
| Work pools | compilation, tests, merge and comparison have pools of their own; each carries the security context and the audit client of the caller | `AsyncExecutors` |

---

## 2. Contract

### 2.1 Module

- **Today** — the run, test, benchmark, debug and input endpoints name the module `fromModule`; the table, graph and
  table-write endpoints name it `module`. A blank `fromModule` is trimmed to none by the test and benchmark endpoints
  and passed as it is by the run and debug ones. No module means the module the client already has open, else the
  first one. An unknown module answers `404`.
- **Proposal** — accept `module` everywhere and keep `fromModule` as a deprecated alias for one release; trim both
  the same way. Document that no module means the module already open, which depends on the earlier requests of the
  client ([REST API Access with Tokens](rest-api-token-access.md) G3).

### 2.2 Branch

- **Today** — the branch is state of the user's project, shared by every client of the user. `PATCH /projects/{id}`
  with `branch` switches it, releases the user's lock and resets the client; a branch with no history is refused
  with `409` and the switch undone. `BranchGuard.requireBranch` asserts a `branch` parameter, `409` on a mismatch,
  and only the files API uses it. Run, test, debug and table reads take no branch.
- **Proposal, phase 1** — document the branch as shared per user: a client that needs one branch switches to it and
  then acts, and tolerates another client of the user switching it in between. Add the assert of `BranchGuard` to
  run, test, debug and table reads, so that a client that names the branch gets a `409` rather than results of
  another branch.
- **Phase 2, deferred** — `branch` as a coordinate of the request. It needs a working copy per branch on disk —
  today one tree per user — and a compiled model per branch; the lock already includes the branch
  (`RulesProject.tryLock`). Built only when an integration needs two branches of one project at once.

### 2.3 Asynchronous jobs

- **Today** — `POST .../tests/run`, `POST .../run` and `POST .../benchmarks` answer `202` with no body and no
  `Location`. A poll answers `202 notReady`, the result, or `404` when there is no job. A cancelled job answers `404`;
  a job that failed answers `500` from `future.get()`. The registries keep one job per client and look it up by the
  project only; the table is stored but not compared.
- **Proposal**:
  - key a job by project, module and table, with a few kept per client
    ([REST API Access with Tokens](rest-api-token-access.md) step 3);
  - answer a job that failed with `200` and `{"status":"failed","error":…}` from the poll, so a failure of the job
    is told apart from a failure of the poll;
  - add `Retry-After` to `202 notReady`;
  - document that a job lives as long as its client — the idle timeout — and does not survive a restart: a `404`
    after a `202` means start it again.

### 2.4 Status codes

- **Today** — `ApiExceptionControllerAdvice` maps the `RestRuntimeException` family (`400`, `403`, `404`, `409`,
  `410`, …), validation to `400`, security to `403` with an `accessDenied` audit event, and anything else to `500`.
  The body is `BaseError`: `code` and `message`.
- **Proposals**:
  - **Lock holder as fields** — the `409` of a locked project names the holder in the message only. Add `lockedBy`
    and `lockedAt` to the body, from `LockInfo`.
  - **Compilation that cannot run** — `awaitCompiled` rethrows a `CompletionException` that maps to `500`; a module
    that fails to load is a client-visible condition. Answer `422` with the compile messages.
  - **`Accept`** — the result endpoints compare `Accept` literally and answer `406` to `*/*`. Treat `*/*` and a
    missing header as JSON.

### 2.5 Leaving BETA

The `/projects` controllers are tagged BETA. Before the tag goes: module naming (§2.1), the phase 1 branch contract
(§2.2), the job key and the failed-job answer (§2.3), and the `422` of §2.4. Each is a change a client can observe,
listed in the release notes when it ships.

---

## 3. Authorization and audit

### 3.1 Scopes of a personal access token

- **Today** — a PAT carries every authority of its user, external groups included; the entity has no scope.
  `@NotPatAuth` keeps a PAT from managing tokens. PATs do not exist in `single` mode.
- **Proposal** — an optional scope, `READ`, `WRITE` or `DEPLOY`, stored with the token and defaulting to all for the
  existing ones. `PatAuthServiceImpl` intersects the user's authorities with those the scope allows when it builds
  the `PatAuthenticationToken`. A scope only narrows: ACLs, controllers and services see fewer authorities and answer
  `403` as they do today. Open: whether writing a table and writing a project are separate scopes.

### 3.2 Audit of token work

- **Today** — the audit log records the authentication method of every event, and the PAT public id at sign-in.
- **Proposals**:
  - the public id of the PAT on every project event, not only on the sign-in;
  - an `OpenL-Token: <public id>` trailer on commits made with a PAT, so the history tells the token apart from the
    browser of the same user;
  - `lastUsedAt` on the token, written on validation at most once a minute;
  - a maximum lifetime, as a setting: today a token without `expiresAt` never expires.

### 3.3 Authorities that change during a job

A job runs with the security context captured when it was submitted, and the client's `WebStudio` with the one of
its first request ([REST API Access with Tokens](rest-api-token-access.md) G5). A role removed while a job runs
applies from the next request. Accepted for jobs; a long deployment job is the case that would need a check on the
worker.

---

## 4. Realtime

A header client connects to `/ws` with its `Authorization` header and receives the notifications of its user
(`convertAndSendToUser`), as an ITEST of the `multi` suite does. A browser cannot set a header on the WebSocket
handshake, so a browser that holds a token and no cookie gets no realtime; a STOMP `CONNECT` that takes the token in
its headers would serve it. Not planned: the MCP server polls `202 notReady` instead
([Built-in MCP Server](mcp-server.md)).

---

## 5. Operations

### 5.1 Clustering

Studio runs as one node: the working copy, the `.locks` files under `user.workspace.home`, the kept clients, the
pending transfer links and the jobs are local, and horizontal scaling is a non-goal of the
[Built-in MCP Server](mcp-server.md). Behind a load balancer with two nodes and no affinity, a token client meets a
different working copy and different locks on every request. Before any multi-node setup, one of:

- **Affinity by user** — route by a key derived from the user, set by an authenticating gateway, since a token
  request has no cookie to stick to.
- **Shared storage** — `user.workspace.home` on a shared file system, after validating that the file locks
  (`Lock.finishLockCreating`, which relies on atomic create and modification times) hold on it.

Either way, clients and jobs remain per node and end with a restart; project ids are the same on every node.

### 5.2 Limits

- **Today** — no rate limit. A PAT is checked with bcrypt on every request. The test pool has a queue of 10 and
  rejects beyond it with a `500`. Nothing bounds the clients of a user
  ([REST API Access with Tokens](rest-api-token-access.md) G6).
- **Proposals**:
  - at most two running jobs per client, beyond it `429` with `Retry-After`;
  - a full pool answers `503` with `Retry-After`, never `500`;
  - a cache of successful PAT checks for a few seconds, keyed by the hash of the token, which bounds the bcrypt cost
    and delays a revocation by as long.

### 5.3 Observability

- **Today** — no Micrometer or Actuator. In the `otel` logging mode with an exporter endpoint, the OpenTelemetry
  agent exports its own JVM and HTTP metrics and traces.
- **Proposal** — a few metrics of Studio itself through Micrometer, exported by the same agent: clients kept, per
  user and evicted; jobs running per kind; pool queue depth and rejections; compile time per module; requests per PAT
  public id and status.

### 5.4 One user name

The user name reaches four keys that compare it as it is: the folder of the working copy
(`WorkspaceUserImpl.generateUserId`, which keeps letters, digits, `_` and `-` and escapes the rest), the STOMP user
destination, the lock owner (`isLockedByUser`) and the key of a client in `ClientSessions`. An identity provider that
returns `Alice` once and `alice` once makes two working copies but one lock owner. Proposal: normalize the name once
— trimmed, and case-folded where the directory is case-insensitive, as Active Directory and LDAP are — where the
request is authenticated, so every key derives from the same value. Open: whether case-folding is a setting of the
deployment.

---

## 6. Open decisions

- **Branch** — phase 1 now; phase 2 only for an integration that needs two branches of one project at once.
- **Failed job** — `200` with `failed` from the poll, or `422`.
- **PAT scopes** — the granularity, and `ALL` for existing tokens.
- **Compile errors** — keep answering a project with rule errors with `200` and its messages, or refuse writes too.
- **PAT check cache** — how many seconds of delayed revocation are acceptable.
- **Case-folding of user names** — always, or a setting.
- **Clustering** — affinity or shared storage, decided before the first multi-node installation.
