# Migrating the Project page (`project.xhtml`) to React

## Goal

Replace the content of `pages/modules/project.xhtml` with a single React component (`ProjectPage`), mounted inside the
JSF editor shell (left tree, breadcrumbs, bottom panel stay JSF; a standalone React route is out of scope). The page's
blocks are all micro-editors of one file — `rules.xml` (the project descriptor) — so the whole page maps to one
descriptor endpoint plus a read-only summary.

PoC reference (the Summary block, already an island): `studio-ui/src/containers/ProjectSummaryPanel/` and
`studio-ui/src/components/JsfIslandHost.tsx`.

## Decisions

- **One editable resource.** `GET` / `PUT /projects/{projectId}/descriptor`, a thin façade over the files API.
  `/descriptor` is the **source of truth for editable fields**; `GET /projects/{projectId}` (`ProjectViewModel`) is a
  read-only summary and must not be used to edit — it already echoes `name`, `comment` and `dependencies`, which stay
  read-only there.
- **Never (de)serialize `rules.xml` in the browser.** It is an internal JAXB format (`ProjectDescriptor.SERIALIZER`,
  `JAXBSerializer`); the codec and semantic validation stay server-side.
- **Whole-page island, not block-by-block.** From Phase 2 the entire `#content` region is one React island; editing is
  enabled per section in later phases. No JSF-block coexistence.
- **Global edit toggle, no popups.** Read-only by default; one **Edit** makes every section editable at once; one
  **Save** (`PUT /descriptor`); one **Cancel** (revert to last-loaded).
- **Concurrency by content hash.** `GET` returns a hash of `rules.xml`; `PUT` echoes it; mismatch → `409` →
  "overwrite a newer revision?" confirm → retry with a force flag. (Project revision is unusable — it changes when any
  other file in the project changes.)
- **Save side effects.** After writing the file, `PUT /descriptor` triggers a workspace reset/recompile; the React
  page then refreshes the summary/compile status and signals the legacy shell to reload its tree and breadcrumbs.
- **Validation on both sides.** React for immediate UX; backend authoritative, returning localized `fields[]` errors.
  `PUT` enforces the design-repo WRITE ACL and the protected-branch check.
- **Deprecated/internal fields are hidden but preserved.** Module method-filters (deprecated),
  `compileThisModuleOnly` (internal/experimental) and the custom properties-file-name **processor** (internal) are read
  and written back unchanged, but have no editor — so existing values are never silently dropped.
- **Exposed methods are one typed list.** The UI edits a single ordered `[{pattern, type: include|exclude}]` list; the
  backend splits it into the descriptor's include/exclude sets on write and flattens them back on read.
- **Order-significant lists reorder by drag-and-drop.** Properties-file-name **patterns** are matched in order, so they
  are an ordered list reorderable via `@dnd-kit` (pointer and keyboard); a read-only numbered list conveys the order.
- **Every list is edited per line.** One input per entry with add/remove — never a free-text area.

## API

| Method & path | Purpose | Status |
| --- | --- | --- |
| `GET /projects/{projectId}` | Read-only summary: name, status, branch, repository, revision, modified-by/at, path, lock, comment | exists (`ProjectViewModel`) |
| `GET /projects/{projectId}/descriptor` | Editable model: name, comment, modules[], dependencies[], classpath[], openapi config, exposedMethods[] `{pattern,type}`, properties-file-name patterns[]. Deprecated/internal fields (module method-filters, `compileThisModuleOnly`, properties-file-name processor) round-tripped but not editable. + `editable` flag + content hash | done |
| `PUT /projects/{projectId}/descriptor` | Validate → write `rules.xml` → reset/recompile; content-hash concurrency (`409` + `force`); WRITE ACL | done |
| `POST /projects/{projectId}/openapi` | No body: generate the OpenAPI schema from the compiled rules/datatypes → `openapi.json` + reconcile. With a body (`OpenApiTablesRequest`: spec path + rules/data module names and paths): generate rules and datatype tables from the committed spec → two modules + generated classes + `rules-deploy.xml` + `<openapi>` generation config. Requires WRITE. | done (backend); import UI pending |

`openapi.json` / `openapi.yaml` content may be read and written straight through the files API
(`/projects/{projectId}/files/{*path}`) — they are standard formats, not an OpenL-internal serialization.

## Mechanism (island)

`JsfIslandHost` (mounted in `DefaultLayout`) watches `#content` with a `MutationObserver`; when the JSF fragment
injects `<div data-island="project-page" data-project-id="…">`, it `createPortal`s `ProjectPage` into it, so the island
shares the app's Ant Design, i18n, security and store context. It drops the portal when the node leaves the DOM
(navigation / reload), unsubscribing listeners.

- One React root (`#appRoot`); never a second `createRoot` inside `#content`.
- Generalize the PoC host from the fixed id `#project-summary-root` to a `data-island="<name>"` registry.

## Frontend component (`ProjectPage`)

Fetches `GET /projects/{projectId}` (summary) and `GET /projects/{projectId}/descriptor` (editable model). Renders
read-only; **Edit** flips the whole page editable; **Save** issues one `PUT`; **Cancel** reverts. Sections:

- **Summary** — read-only, from `GET /projects/{projectId}` (live compile status via the `projectStatus`
  subscription): status, compilation, modified-by/at, repository, branch, path.
- **Identity** — editable **name** (the document title; writes `rules.xml`'s `<name>`, with the folder name as the
  placeholder fallback) and **comment** (the lead paragraph).
- **Modules** — per-line editor: **name** + **rules-root path** only, add / remove. Wildcard-matched modules are
  read-only derived rows. Method-filters and `compileThisModuleOnly` are not shown (see Decisions).
- **Dependencies** — per-line editor: project **name** + an "all modules" toggle, add / remove. (A workspace-project
  picker instead of free text is a future enhancement.)
- **Sources** — classpath list (`StringListEditor`: one input per entry, add / remove).
- **Exposed Methods** — one ordered `[{pattern, type}]` list; each row is a pattern input + an Include/Exclude select.
- **File name patterns** — ordered, drag-and-drop list (`SortablePatternList`, `@dnd-kit`); order is significant, so a
  read-only numbered list is shown.
- **OpenAPI** — config edited inline (path, mode, rules/data module names); a **Generate OpenAPI schema** action
  (`POST …/openapi`) exports the compiled rules to `openapi.json`. Generating the default file writes no `<openapi>`
  element (OpenL reconciles the default file automatically, and writing a descriptor would drop an auto-discovered
  project's modules). The spec→tables **import** direction is still pending.

Dual validation; reuse `apiCall`, antd-style (`*.styles.ts`), i18n (`project` namespace), and `data-testid`.

## Plan

- **Phase 0 (done).** Summary island — reference implementation.
- **Phase 1 (done).** Descriptor read model + `GET /descriptor` (map via `ProjectDescriptor.SERIALIZER`; editability
  flag; content hash).
- **Phase 2 (done).** Generalize `JsfIslandHost` to the `data-island` registry; `ProjectPage` renders the whole page
  read-only.
- **Phase 3 (done).** `PUT /descriptor` (validation, content-hash concurrency, reset/recompile) + global edit toggle /
  Save / Cancel + `409` flow; inline editing for Identity, Sources, Exposed Methods, Dependencies. Cancel sits in the
  Edit slot so a stray click right after Edit cancels rather than saves.
- **Phase 4 (done).** Modules editor (add / edit / remove name + rules-root path); exposed methods as one typed list;
  editable project name; properties-file-name patterns as an ordered drag-and-drop list. (No copy action; method-filter
  and `compileThisModuleOnly` editing dropped per Decisions.)
- **Phase 5 (in progress).** OpenAPI config edited inline (done); `POST …/openapi` schema generation — rules →
  `openapi.json` (done); spec → tables generation (`ProjectOpenApiService.generateTables`, reusing the JSF-free
  `OpenAPIScaffoldingConverter` / `OpenAPIJavaClassGenerator` / `OpenAPIHelper`; session freeze/lock/history dropped;
  spec read via a temp file) — **backend + tests done**, the import UI and live verification are pending. Known limits:
  a project whose existing modules are auto-discovered (no `rules.xml`) would keep only the generated modules on write;
  external `$ref`s in the spec are not resolved (temp-file input).
- **Phase 6 (in progress).** `project.xhtml` is already the shell + one `data-island="project-page"` placeholder,
  and its inline descriptor-editing popups went with it. The now-dead descriptor-editing `ProjectBean` actions
  (`editName`, `editSources`, `editDependencies`, `editExposedMethods`, `removeModule`, `removeDependency`,
  `migrateMethodFilters` + their exclusive helpers, validators, fields, and the two `*MigrateMethodFilters*` /
  `*ConvertRegexToGlob*` test classes) have been removed. What remains: the OpenAPI `ProjectBean` methods (they back the
  orphaned `editOpenAPI.xhtml` and the not-yet-migrated tables-generation) and the full bean/fragment retirement — both
  blocked on the spec→tables import migration.

## Done when

- The descriptor is read and written only via `GET` / `PUT /projects/{projectId}/descriptor`; editing never goes
  through `GET /projects/{projectId}`.
- `PUT` validates server-side (localized `fields[]`), enforces WRITE ACL + protected branch, and rejects a stale
  content hash with `409`.
- One React component, no popups, global edit toggle; renders via `createPortal` with only `#appRoot` as a root and
  unmounts cleanly on navigation.
- After Phase 6 no `ProjectBean` descriptor actions or RichFaces popups remain; new/changed code keeps ≥80% diff line
  coverage.
