# Parallel Multi-Project Editing (OpenL Studio)

**Purpose**: Describe how one user session edits several projects in parallel without their compiled state
interfering.

---

## Problem

Historically a session held a single `ProjectModel` plus one "current selection"
(`WebStudio.currentRepositoryId/currentProject/currentModule`). Every read or edit funnelled through
`WebStudio.init(...)`, which re-pointed that one model, and each modern REST edit finished with a global
`WebStudio.reset()`. As a result:

- Working on project **B** dropped project **A**'s compiled state, so a second browser tab on **A** showed
  stale / constantly-recompiling / unpredictable results.
- Asynchronous REST edits to different projects clobbered each other.

## Design

A session now keeps **one `ProjectModel` per opened project**, all sharing **one dependency manager**.

- **`OpenedProjectsSession`** (owned by `WebStudio`) — the opened-project state machine. It owns the model
  store and the shared compilation context, and keeps the "current model is the pinned model" invariant in one
  place. `WebStudio` keeps the JSF "current selection" labels (current project/module/repository) and delegates
  the model operations here.
- **`ProjectModelRegistry`** (owned by `OpenedProjectsSession`) — a bounded, LRU map of `ProjectModel` keyed by
  `ProjectModelKey(repositoryId, projectFolderName, branch)`. `getModel()` still returns the current-selection
  model for the JSF UI and other legacy callers; `openProjectModule(...)` resolves a specific project's model
  without changing the current selection.
- **`WorkspaceCompilationContext`** (owned by `OpenedProjectsSession`) — a single
  `WebStudioWorkspaceRelatedDependencyManager` for the whole session, expanded to the union of all opened
  projects' dependency graphs. Compiled dependencies are cached by name, so opening **B** reuses **B**'s
  modules already compiled while opening **A** (which depends on **B**) — no redundant recompilation. Each
  `ProjectModel` keeps only its own compiled result, opened module, and UI state.
- **`CompilationJobRegistry`** — one in-flight compilation job per `(projectId, branch)`, so several projects
  can compile and report status independently.
- **Decoupled `ProjectModel`** — a model resolves its **own** project identity (`getProject()`, search scope,
  history path) instead of reading the session's current selection, so models never follow each other.

### Editing and invalidation

Modern REST edits replace the global `WebStudio.reset()` with targeted invalidation
(`WorkspaceProjectService.invalidateAfterEdit`): the edited project and any **opened** projects that depend on
it are eagerly recompiled in the background, while unrelated projects keep their compiled state. Cross-project
invalidation rides the shared dependency manager's own dependency graph (a reset of the edited project cascades
to its dependents).

### Concurrency

Different projects use separate `ProjectModel` monitors, so opening/editing them does not serialize on a shared
lock. The shared dependency manager serializes the actual byte-code compilation work within a session (its
`loadDependency` is synchronized) — but switching to an already-compiled or dependency project is instant. The
EPBDS-16092 invariant is preserved: project-status events are still delivered off the compilation threads via a
per-model notifier, so no compilation lock is held while a model monitor is acquired.

## Configuration

- **`webstudio.max.opened.models`** — maximum number of project models kept compiled at once per session
  (default `8`). Beyond this, the least-recently-used idle, non-current model is evicted and its resources
  released. The current selection is never evicted.

## Behavior notes & limitations

- **Recently visited tables** are now tracked per project (previously effectively per session). Visiting tables
  in **A**, switching to **B**, and back no longer clears **A**'s recent list.
- **Closed-project memory** — a project's model is destroyed when the project is closed. Its compiled
  dependencies remain cached in the shared manager until the whole session is reset/destroyed; the shared
  manager grows within a session and is fully released on workspace reset.
- **JSF per-tab selection** — the legacy JSF UI now resolves the project/module/table per request from the
  tab's own identity (see *Legacy JSF: per-tab project selection* below), so several browser tabs edit different
  projects concurrently in one session without clobbering each other. The session-global selection remains as
  the fallback for requests that carry no tab identity.
- **Same project name across repositories** — the shared dependency manager identifies projects/modules by
  name (OpenL's `ResolvedDependency` is name-based). Two opened projects with the same name in different
  repositories share one compiled entry and one syntax-node bucket. Repository-qualified dependency keys would
  be a deeper dependency-manager change; for now keep opened project names unique across repositories.
- **Cross-project compile restart on edit** — the shared manager has a single compilation version counter, so
  invalidating one project (an edit, or its eager dependent recompile) interrupts and restarts any other
  project's in-flight background compile. Work converges, but a session compiling many projects while edits
  arrive does redundant recompilation.
- **Eviction under heavy concurrency** — only the current selection is pinned; a REST-opened model is
  evictable once idle. Opening more than `webstudio.max.opened.models` distinct projects concurrently could
  evict a model another in-flight request still holds. Raise the limit for sessions that work on many projects
  at once.

## Legacy JSF: per-tab project selection

The JSF UI historically funneled every read through one session-global selection
(`WebStudio.getModel()/getCurrentProject()/getCurrentModule()/getCurrentRepositoryId()`), so a second browser
tab on another project clobbered the first. Each tab now carries its own identity on every request and the
server resolves that tab's model, leaving the session-global selection only as a fallback.

- **Transport** — the tab's identity lives in the page URL hash (client-only). The module page sends it on
  every server call: a `$.ajaxPrefilter` appends `tabRepositoryId/tabProject/tabModule` to same-origin jQuery
  requests (sub-page loads, the `/web/compile|message` calls), and matching hidden fields are kept in every form
  (re-injected after each load and a4j re-render) so a4j postbacks carry it too.
- **Resolution** — a JSF `PhaseListener` (for `/faces/*`) and a Spring `HandlerInterceptor` (for the legacy
  `/web/compile/**` and `/web/message/**` endpoints, which run on the DispatcherServlet) read those parameters
  and build a request-scoped `TabContext` via `TabContextResolver`. Resolution reuses the opened-project
  registry (`WebStudio.openProjectModule`/`getModelIfPresent`), never mutates the session selection, and fails
  safe (any resolution error yields no context, so the request falls back to the session selection).
- **Accessor flip** — the `WebStudio` selection accessors (`getModel`, `getCurrentProject`,
  `getCurrentProjectDescriptor`, `getCurrentModule`, `getCurrentRepositoryId`) return the `TabContext` values
  when one is bound and resolved, otherwise the session-global fields. This re-points the whole JSF read surface
  centrally. Methods that combine these reads use the same flipped accessors throughout so a tab's project,
  module and repository stay consistent. Requests with no tab identity (project-selection `/web/init`, the modern
  `/rest/projects/**` API, background compile threads) have no `TabContext` and keep today's behavior.
- **View-expiry recovery** — `AjaxViewExpiredExceptionHandler` answers an a4j postback whose view expired with a
  partial-response error naming `ViewExpiredException` (and then defers to the default handler, so it never
  writes twice); the page's existing handler reloads the tab from its hash. This replaces the silent hung
  spinner and error-log storm.

### Bean scope notes

The accessor flip makes every bean that reads the current selection (request-, view- and session-scoped alike)
operate on the requesting tab's model, so most `@SessionScope` beans needed no rescoping. The remaining
session-scoped state is intentionally session-wide:

- **Repository tree** (`RepositoryTreeState`, `RepositorySelectNodeStateHolder`,
  `ProductionRepositoriesTreeState`/`Controller`) — shared workspace navigation, correctly one per session.
- **UI preferences** (e.g. `TreeBean.hideUtilityTables`) — a persisted toggle; session scope keeps it stable
  across navigation rather than resetting per view.
- **`RunTestHelper`** — `@Deprecated(forRemoval = true)`; its model and table reads flip per tab. Its leftover
  manual-run input parameters are session-wide, a narrow edge for two tabs running parameterized manual methods
  at the same instant.

### Known gaps

- **Selected-table URI (`WebStudio.getTableUri()/setTableUri()`)** stays session-global. It is only a
  "last visited table" hint used when a request omits the table id; the table itself is identified per request by
  its id against the tab's model, so the per-tab isolation does not depend on it.
- **Legacy cell editor** — the in-browser table editor (`org.openl.rules.tableeditor`, prototype.js
  `Ajax.Request`) is a third client transport that the jQuery prefilter and a4j hidden fields do not cover, so
  its requests resolve the session-global selection. The edited grid itself is keyed per editor instance, but a
  concurrent cell save from a non-active tab can target the session selection. Use the modern React/REST editor
  for concurrent cell editing across tabs.

## Key classes

- `org.openl.rules.ui.WebStudio` — session hub; holds the current-selection labels and delegates model
  operations to `OpenedProjectsSession`.
- `org.openl.rules.webstudio.web.tab.TabContext`, `TabContextResolver`, `TabContextHolder`,
  `TabContextPhaseListener`, `TabContextInterceptor` — per-tab request context for the legacy JSF UI.
- `org.openl.rules.webstudio.web.jsf.AjaxViewExpiredExceptionHandler` — AJAX view-expiry recovery.
- `org.openl.rules.ui.OpenedProjectsSession` — opened-project state machine; owns the model store and the
  shared compilation context.
- `org.openl.rules.ui.ProjectModelRegistry`, `org.openl.rules.ui.ProjectModelKey` — per-project model store.
- `org.openl.rules.ui.WorkspaceCompilationContext` — shared dependency manager + syntax-node index.
- `org.openl.rules.ui.ProjectModel` — one project's compiled state and opened module.
- `org.openl.studio.projects.service.WorkspaceProjectService` — REST open/edit, targeted invalidation.
- `org.openl.studio.projects.service.project.compile.CompilationJobRegistry` — per-project compilation jobs.
