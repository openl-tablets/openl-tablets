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
- **JSF concurrency** — the modern REST path is fully concurrency-safe per project. The legacy JSF UI still
  shares a single session-global "current selection" (`currentProject/currentModule/tableUri`); the registry
  removes the destructive compiled-state clobber (the reported bug), but truly simultaneous requests from two
  JSF browser tabs still share those current-selection pointers. Closing this gap would require per-tab context
  supplied by the front end.
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

## Key classes

- `org.openl.rules.ui.WebStudio` — session hub; holds the current-selection labels and delegates model
  operations to `OpenedProjectsSession`.
- `org.openl.rules.ui.OpenedProjectsSession` — opened-project state machine; owns the model store and the
  shared compilation context.
- `org.openl.rules.ui.ProjectModelRegistry`, `org.openl.rules.ui.ProjectModelKey` — per-project model store.
- `org.openl.rules.ui.WorkspaceCompilationContext` — shared dependency manager + syntax-node index.
- `org.openl.rules.ui.ProjectModel` — one project's compiled state and opened module.
- `org.openl.studio.projects.service.WorkspaceProjectService` — REST open/edit, targeted invalidation.
- `org.openl.studio.projects.service.project.compile.CompilationJobRegistry` — per-project compilation jobs.
