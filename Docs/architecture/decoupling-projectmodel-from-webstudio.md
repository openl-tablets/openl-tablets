# Decoupling `ProjectModel` from `WebStudio`

- **Status** — Proposed; deferred until a step that needs it is scheduled (§1.2)
- **Date** — 2026-10-23
- **Scope** — the reference of the compiled project (`ProjectModel`) to the client that owns it (`WebStudio`), in
  `STUDIO/studio-backend`, and the narrow interfaces that would replace it.

---

## 1. Context

`WebStudio` is the state of one client ([Client Sessions](client-sessions.md)): its current project and module,
the user, the workbook write lock. It creates one `ProjectModel` in its constructor
(`new ProjectModel(this, testSuiteExecutor)`) and the model keeps that reference as `private final WebStudio studio`,
which `@Getter` also hands out. The model reads the current selection, the user, global services and settings
through it, and `WebStudioWorkspaceDependencyManagerFactory` takes the same `WebStudio` to build the dependency
manager.

### 1.1 What changed since the spike

The spike was written when a request without an HTTP session made the model fail. That reason is gone:

- **A stateless request has a model** — every credential gets a `WebStudio` and its `ProjectModel` from the client
  scope (EPBDS-14371).
- **No thread-bound reads** — `WebStudioUtils` is deleted; the model reaches the workspace through
  `studio.getUserWorkspace()`, which works from any thread. No `RequestContextHolder`, `HttpSession` or JSF
  `FacesContext` is left in the model.
- **Less to cut** — the JSF removal took the project tree, the tree and table view modes, the table editor model,
  the link builder, the frozen-project state, the modification poll and the self-call
  `studio.getModel().isTestable(...)` with it.

### 1.2 Why it is still worth doing

The reference ties one model to one client and to that client's single current selection. Three items of the
remaining plan need a model that does not read its selection from `WebStudio`:

- **Results serialized by the project of the request** — G3 of
  [REST API Access with Tokens](rest-api-token-access.md): the Jackson configuration, the history restore and the
  merge read the current project of the client.
- **The acting user instead of the first one** — G5 there: status publishing replays the `Authentication`
  `WebStudio` captured when it was created (`runAsSessionUser`).
- **Several models per client, or one per user** — the options D4 of that page and
  [Principal-Scoped Shared State](principal-scoped-shared-state.md) defer.

None of them is scheduled. This page keeps the inventory current, so that the work starts from facts once one is.

## 2. Coupling inventory

Every use of `studio` in `ProjectModel` and in the dependency-manager factory, by what it reads:

| # | Kind | Use | Where |
|---|---|---|---|
| 1 | Structure | the `studio` field, its `@Getter`, the constructor parameter | `ProjectModel` |
| 2 | Structure | `new WebStudioWorkspaceDependencyManagerFactory(studio)` | `ProjectModel` constructor |
| 3 | Outward reach | `model.getStudio()` to build project addresses | `TableModules.ofWorkspaceAddressed` |
| 4 | Selection | `studio.getCurrentProject()` | `getProject`, used by `isEditable`, `isCurrentBranchProtected`, `doPublishStatusChanged`, `getProjectDescriptor`, `historyFolderOf` |
| 5 | Selection | `studio.getCurrentModule()` | `isEditable` (twice), `reset(FORCED)`, `getSearchScopeData` |
| 6 | Selection | `studio.getCurrentProjectDescriptor()` | `getCurrentProjectTableSyntaxNodes` |
| 7 | Workspace | `studio.getUserWorkspace()`, `studio.getCurrentRepositoryId()` | `getLocalRepository`, `historyFolderOf` |
| 8 | User | `studio.runAsSessionUser(...)`, `studio.getCurrentUsername()` | `doPublishStatusChanged` |
| 9 | Global service | `studio.getDesignRepositoryAclService()` | `isEditable` |
| 10 | Global service | `studio.getBypassService()` | `isCurrentBranchProtected` |
| 11 | Global service | `studio.getProjectResolver()` | `findReloadedModule`, `getProjectDescriptor` |
| 12 | Global service | `studio.getEventPublisher()` | `publishStatusChanged` |
| 13 | Setting | `studio.getExternalProperties()` | `getRulesInstantiationStrategy`; the factory |
| 14 | Setting | `studio.isAutoCompile()` — a global administration setting | `isManualCompileNeeded`; the factory |
| 15 | Catalog | `studio.getAllProjects()` | the factory, on the compile path of `buildDependencyManager` and `resolveWorkspace` |

That is 18 lines of `studio.*` in 14 methods of the model and 3 reads in the factory.

The reverse direction is already clean and is the template for the rest: the dependency manager reports back
through listeners the model registers (`registerOnCompilationCompleteListener`,
`registerOnResetCompleteListener`) and knows nothing of the model.

## 3. Seam design

Replace the reference with narrow interfaces. `WebStudio` implements them first, so behavior stays the same until a
caller supplies another implementation.

| Replaces | With | Responsibility |
|---|---|---|
| 9–12 | the beans themselves, injected | `RepositoryAclService`, `ProtectedBranchBypassService`, `ProjectResolver`, `ApplicationEventPublisher` are singletons; `WebStudio` only passes them on |
| 13, 14 | `CompilationConfig` | the external properties and the auto-compile setting; one global bean serves the model and the factory |
| 4, 7 | `WorkspaceContext` | the open `RulesProject` and the local repository of a selection, from the user's `UserWorkspace` |
| 15 | `ProjectCatalog` | the projects of the user's workspace, for dependencies between projects |
| 5, 6 | `ModuleSelection` | a record of the repository, the project descriptor, the module and the acting user, passed into the four methods that read the selection |
| 8, 12 | `ProjectStatusNotifier` | publishes a status change as the acting user; keeps the `statusNotifier` executor hand-off, the coalescing of `statusUpdateHandedOff` and the inline `progressOnly` event |
| 3 | a project-addressing port, or the caller takes the address from `WebStudio` itself | the getter goes away with the field |

```java
@NullMarked
public record ModuleSelection(String repositoryId, @Nullable ProjectDescriptor projectDescriptor,
        @Nullable Module module, Principal user) {
}
```

- **`WorkspaceContext` and `ProjectCatalog` share one source** — the user's `UserWorkspace`, reached with
  `acquireUserWorkspace` and given back with `releaseUserWorkspace`: a bare `getUserWorkspace` could be released
  under them by the last client of the user.
- **`ModuleSelection` carries the acting user** — the substitute for the `Authentication` that `WebStudio` captured,
  so the ACL check of `isEditable` and the status stamp use the user of the request.

## 4. Plan

Each step keeps the build green and the behavior unchanged; only step 4 changes ownership.

1. **Inject the global services** — rows 9–12, five call lines. 0.5 WP.
2. **Introduce the interfaces with `WebStudio` as their implementation** — `CompilationConfig`, `WorkspaceContext`,
   `ProjectCatalog`, `ProjectStatusNotifier`, the `ModuleSelection` record. 1 WP.
3. **Move the call sites onto them** — rows 4–8 and 13–15, the factory taking `ProjectCatalog` and
   `CompilationConfig` instead of `WebStudio`, the four selection reads taking a `ModuleSelection`, and row 3.
   After it, nothing but the implementations reads `studio`. 2 WP.
4. **Change the owner** — remove the field and the constructor parameter; `WebStudio`, or the holder of several
   models, creates the model from the interfaces. Only with the step of
   [REST API Access with Tokens](rest-api-token-access.md) or
   [Principal-Scoped Shared State](principal-scoped-shared-state.md) that needs it. 1 WP, plus that step.

Steps 1–3 come to about 3.5 WP with their tests.

## 5. Risks

| Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|
| Lock order — `WebStudio.init` and `getAllProjects` are `synchronized` on `WebStudio`, `compileProject` and `setModuleInfo` on the model, and the factory calls back into `WebStudio` under the model's monitor | High | Low | `ProjectCatalog` reads the workspace without the `WebStudio` monitor; keep status publication on the `statusNotifier` executor, never inline under a held lock except the existing `progressOnly` event |
| `setModuleInfo` and `WebStudio.init` decide "another module opened" from one shared pointer (`isSameModule`) | Medium | Medium | Move both onto `ModuleSelection` in the same step |
| Status publication changes shape — coalescing, `progressOnly`, the boundaries that publish | Medium | Medium | Tests on the published events before and after step 3 |
| The notifier outlives its client — `ProjectModel.destroy` shuts the executor down when `RulesUserSession` ends `WebStudio` | Medium | Low | Keep `destroy` owned by the model; the notifier only binds the user |
| An implementation that is not equivalent to `WebStudio` | Medium | Medium | The `WebStudio` implementation first; existing tests stay green after every step |

## 6. Acceptance criteria

1. After step 3, no production code of `ProjectModel` or of the dependency-manager factory reads `studio`.
2. A model built from test implementations of the interfaces, with no `WebStudio`, compiles a module and publishes
   its status as the user of its `ModuleSelection`.
3. The published status events are the same before and after, in number and order, for a compilation of a
   multi-module project.
4. The concurrency tests of the model — compilation, cancel, reset, status publication — pass unchanged.
