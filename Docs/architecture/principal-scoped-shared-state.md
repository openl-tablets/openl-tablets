# Principal-Scoped Shared State

- **Status** — Deferred. [Client Sessions](client-sessions.md) keeps state per credential instead; this page records
  the alternative and the conditions to revisit it
- **Date** — 2026-10-23
- **Scope** — one container of derived state per user — compiled projects and jobs — shared by every browser and
  every token of the user, in OpenL Studio.

---

## 1. The idea

Keep two homes for state:

- **The window** — a browser session, or a token client — holds only what it is looking at: its current project,
  module and table.
- **The user** — one container per user holds the heavy state: the compiled projects, the compilations, test runs and
  debug sessions, the opened projects. Every window of the user refers to the same container.

A job started from one window could then be watched and its result read from another, and a project compiled for the
browser would be ready for the agent of the same user.

## 2. What the code does instead

- **Per user** — the working copy: `MultiUserWorkspaceManager` keeps one `UserWorkspace` per user and releases it with
  its last holder.
- **Per client** — everything derived from it: each browser session and each credential has its own `WebStudio`,
  `ProjectModel` and job registries under `@ClientSessionScope`, ended after the idle timeout
  ([Client Sessions](client-sessions.md)).
- **Per user, already shared** — the notifications: STOMP delivers by user name, so every window of the user sees
  the progress of a compilation or a test run of any of them.

## 3. Why it is deferred

- **Most of the goal is met** — the working copy, the source of truth, is shared per user. What a shared container
  adds is a derived cache: it saves compiling a project once more for a second client, and lets one client read the
  result of another client's job.
- **No workflow needs cross-client results** — no user story asks to start a test run in the browser and read its
  result through a token, or the opposite; the progress of either already reaches both.
- **Isolation is a feature** — an agent and a CI job of one user must not cancel each other's test run or switch each
  other's current project. Per client they cannot; in one container they could, unless every job is keyed by its
  target and every window keeps its own selection.
- **It brings liabilities of its own**:
  - **Lock order** — the request path takes `WebStudio.init`, then `ProjectModel.setModuleInfo`; the compile path
    takes `setModuleInfo`, then `WebStudio.getAllProjects` through the dependency-manager factory, and a design
    repository change reaches `clearModuleInfo` from another thread. One model shared by many windows makes that
    inversion reachable; a lock on top of both monitors does not remove it.
  - **Memory** — a model kept as long as any window of the user lives cannot be evicted under memory pressure by an
    idle rule; it needs a budget and forced eviction.
  - **Reach of a token** — a token shares the warm state the browser built, so a leaked token reaches more.
  - **Identity** — the user name keys the working copy, STOMP and the lock owner as it is, so a case-insensitive
    directory can split one person into two containers
    ([Contract and Operations §5.4](rest-token-contract-and-ops.md#54-one-user-name)).

## 4. Conditions to revisit

Revisit when a validated workflow needs one client to read a job another client started, or when the memory of
several compiled copies per user is shown to matter (G6 of
[REST API Access with Tokens](rest-api-token-access.md) measures it). The work would then need, in order:

1. **The `ProjectModel` interfaces** of [Decoupling](decoupling-projectmodel-from-webstudio.md), steps 1–4: the model
   reads its selection and its acting user from the request, not from one `WebStudio`.
2. **A written lock order** covering request threads, the compilation worker and the design repository callback,
   proven by an interleaving test.
3. **Jobs keyed by target** — project, module, table — with an explicit rule for which window may cancel which job.
4. **One normalized user name** for every key.
5. **A memory budget per user**, with eviction that also ends models still referenced but idle.
6. **CSRF protection of the cookie** ([Authentication Precedence](auth-precedence-header-over-cookie.md) §4), as the
   cookie would reach the shared state of every channel of the user.

`single` mode is the degenerate case already: every request shares the one user's state.
