# Dead-code sweep ledger

State memory for the daily sweep of openl-tablets. Read in full at the start of every run.

## Resume point

- Open PR #1933 on `dead-code/studio-resources`, 2 commits, head `4d1cf6b657`, body verified against the diff
  (12 files, 1 insertion, 74 deletions). No review threads. Only red job is the `main`-wide `LockTest` failure.
- **The routine is out of work it can verify.** Every resource vein is closed (see *Exhausted veins*) and every
  Java/Maven vein needs an artifact resolution this container denies. A run cannot ship a removal until either
  `build.shibboleth.net` is allowlisted or the opensaml BOM import leaves the root pom.
- First action next run: retry `mvn validate -N`. If it resolves, sweep queue rows 9-13 in that order. If it 403s
  again, there is nothing to sweep — maintain PR #1933, compact this file, and stop. Do not re-mine resources.

## Change-type queue

| # | Change type | Status |
|---|---|---|
| 1 | Unreferenced images | done, no findings (incl. all 610 under `Docs/`) |
| 2 | Unreferenced `.xhtml` pages | done, no findings |
| 3 | Dead CSS classes and ids | done, only `.te_hidden`, deferred |
| 4 | Unreferenced `.js` files | done, no findings |
| 5 | Unused `.properties` keys | done, no findings |
| 6 | Unused i18n keys in studio-ui bundles | done, 57 keys in PR #1933 |
| 7 | Unreferenced exported TS symbols and modules | done, only deferred findings |
| 8 | Dead functions inside legacy `.js` | done, `PopupMenu.showChild` in PR #1933 |
| 9 | Never-read assignments (PMD) | blocked: no Maven |
| 10 | Unused private fields and locals (PMD) | blocked: no Maven |
| 11 | Unused private methods and formal params (PMD) | blocked: no Maven |
| 12 | Unused nested / effectively-private types (javac) | blocked: no Maven |
| 13 | Unused declared Maven dependencies | blocked: no Maven |

## Open PR

- Branch `dead-code/studio-resources`, PR #1933, head `4d1cf6b657`, ready for review.
- Commit 1 `ba44801c5e` — `Remove i18n keys no longer referenced by any studio-ui component` (9 files, 63 deletions).
- Commit 2 `4d1cf6b657` — `Remove the uncalled PopupMenu.showChild helper from the legacy table editor`
  (3 files: the source plus both regenerated JS bundles).
- Two distinct change types, so nothing to squash. The `LockTest` job is already answered in two comments — do not
  comment on it a third time, and do not spend a re-run while `main` is red.

## Merged PRs

(none)

## Module coverage

- `STUDIO/studio-ui` — locale bundles, whole import graph, eslint unused rules, npm dependencies: only deferrals left.
- `STUDIO/org.openl.rules.webstudio`, `STUDIO/org.openl.rules.tableeditor` — resources and hand-written JS swept.
- Every other module — Java only, so nothing swept while Maven is blocked.

## Deferred findings

- `STUDIO/studio-ui/src/containers/MergeModal/types.ts`: `MergeRequest`, `ResolveConflictsRequest`,
  `ResolveConflictsResponse`, `FileConflictResolution` — unused in TS but mirror a live REST contract documented in
  `Docs/api/projects-merge-api.md` with a Java record and an OpenAPI schema. Needs a human decision.
- ~45 studio-ui exported types used only inside their own file — alive; dropping `export` is a refactor, not a deletion.
- `js/datepicker.js` `dateValidForSelection`, `getSelectedDate`, `setDisabledDays`, `joinNodeLists` — no call site, but
  the file is vendored third party (DatePicker v5.4, frequency-decoder.com, CC BY-SA 3.0) and these are its public API.
- `.te_hidden` in `STUDIO/org.openl.rules.tableeditor/css/common.css` — the only real CSS orphan. Blocked because the
  CSS bundles are not reproducible (see *Method rules*), so the removal cannot be propagated to what ships.
- `STUDIO/org.openl.rules.workspace/resources/deployer.properties` — a `production-repository.$ref` sample no file
  names, in a library jar with no matching application. Its `$ref` syntax is still live, and `{appName}.properties`
  would load it for an app named `deployer`, so only a human knows whether such an app still exists downstream.

## False-positive shapes

- i18next appends `_one`/`_other` itself when `count` is passed; check the plural-stripped base before deleting.
- A locale key reached only through a template literal: enumerate `t(` + backtick call sites, treat each composed
  prefix as keeping its whole family alive. `t(someKeyVariable)` means the literals sit at the call sites instead.
- Never test a CSS class by "does this token appear anywhere" — `ui-layout-*`, `tooltip_*` and `te_toolbar_*` are all
  built by string concatenation in JS or Java.
- `rf-*` classes cannot be proven dead (RichFaces ships its JS inside a jar); `ant-*` come from antd at runtime.
- A regex of `#[a-zA-Z0-9]+` over CSS reports every hex colour as an id selector. Filter hex before reading results.
- `.properties` keys are routinely assembled from a prefix plus a runtime segment — see Keep-list for each convention.
- An interface with zero code references can still be a documented API contract mirrored from the backend.
- `\b` does not match before `$`, so a `\b`-anchored search finds no call site for a `$`-leading name. `$cell` in
  `TableEditor.js` looked dead while `this.$cell(cellPos)` sat two lines below. Use `(?<![\w$])name(?![\w$])`.
- A name defined as a key in an options object literal passed to a framework is a callback, not something anyone calls
  by name — `onFailure` in `TableEditor.js` belongs to `new Ajax.Request(...)`. Check the enclosing call first.
- A module can be imported by a specifier that already carries its extension (`from './App.styles.ts'`), which
  defeats a resolver that only appends `.ts`/`.tsx`. Try the specifier verbatim before appending.
- A "dead" JS function is usually called from inside its own file — exclude the defining file from the search and
  every private helper looks unreferenced.
- **A dependency jar can read a resource by a name hardcoded in its own bytecode.** CXF's `AbstractHTTPServlet`
  loads `/cxfServletStaticResourcesMap.txt`, so that file is named by no file in this repository and is still
  load-bearing. When a resource name looks invented-but-conventional, fetch the owning jar and grep its constants.
- A pom property with no `${...}` reference anywhere is almost always a plugin convention parameter — `maven.*`,
  `sonar.*`, `invoker.*`, `archetype.*`, `spotless.*`, `lombok.delombok.skip`, `project.build.sourceEncoding`.
- Duplicate-`<dependency>` detection must first strip `<dependencyManagement>`, `<plugin><dependencies>` and XML
  comments; otherwise correct Maven practice and commented-out samples both report as duplicates.

## Method rules

- Prove non-reference with a plain repo-wide literal search excluding `target/`, `node_modules/` and `.git/` — every
  file type, never a regex scoped to one attribute or one module.
- For a bundle key, search the full dotted path **and** the bare leaf name; either hit means keep.
- Validate any new bulk detector by feeding it two fabricated names; if they come back "referenced", the search is
  wrong, not the repository. For a linter, plant a violation and confirm it is reported.
- **This clone is shallow (50 commits).** Its earliest commit "adds" all 15032 files, so `git log --diff-filter=A`
  attributes every older file to that graft and history proves nothing about a file's origin. Never argue from it.
- Resolve studio-ui imports with tsconfig `paths` `"*": ["./src/*"]` (a bare specifier is `src/`-relative), and follow
  side-effect `import 'x'` and `vi.mock('x')` too, or barrel-only and test-only modules look dead.
- The tableeditor JS bundles `js/tableeditor.all.js` and `js/tableeditor.min.js` are checked in and are what the
  runtime loads, not the individual sources. `bash compile.js.sh` reproduces both byte-for-byte from the unmodified
  sources using the checked-in `yuicompressor-2.4.7.jar`, so any source removal must regenerate them in the same
  commit. `compile.css.sh` does **not** reproduce the committed CSS bundles — never regenerate those.
- Run the frontend gate from `STUDIO/studio-ui` with no Maven build competing: `npx tsc --noEmit`, `npx eslint src`
  (whole tree, not just edited files), `npx vitest run`. Baseline is 162 files / 1410 tests green, eslint and tsc clean.
  `@typescript-eslint/no-unused-vars` is `warn`, so read the output, not just the exit code.
- `npm ci` works here — registry.npmjs.org bypasses the proxy. `node_modules` is gitignored.
- When a deletion empties a parent object literal, delete the parent in the same commit.
- A PR body loses angle-bracketed placeholders even inside backticks — write such a segment as prose, then re-read the
  stored body to confirm.
- Documentation is this repository's approved source of truth, so a `Docs/` markdown page that nothing links to is
  not a deletion candidate — Jekyll publishes it regardless. Treat the whole `Docs/` prose tree as out of scope.

## Keep-list

- `RestRuntimeException.getErrorCode()` builds `openl.error.` + HTTP status + the code passed to the exception, so a
  `ValidationMessages.properties` key is only ever written as its suffix in Java. Search by suffix.
- A JSF page reaching a bundle as a prefix string concatenated with a lowercased enum name keeps that whole
  `messages.properties` family alive.
- Property names composed at runtime stay: `repo-default.` plus the repository type plus the suffix, `repository.`
  plus the id plus `.settings.`, and `openl-db-repository-` plus the database product name; the reference-key suffix
  is exercised from ITEST init params.
- `ApplicationPropertySource` loads `classpath:{appName}.properties`, so a properties file at a jar root can be alive
  through the deployed application's name alone — `DEMO/webservice.properties` is exactly that shape.
- Spring pulls every `META-INF/openl/extension-*.xml` in through `@ImportResource("classpath*:...")` in
  `ExtensionsConfiguration`, so those bean files are never referenced by name.
- Files that exist by library convention stay: Flyway migrations under `db/flyway/**`, `simplelogger.properties`,
  JUL `logging.properties`, `META-INF/io/opentelemetry/instrumentation/*.properties`, `archetype-metadata.xml`,
  maven-site `site.xml`, Facelets `*.taglib.xml` and `.tld`, Bean Validation message overrides, favicons and
  web-manifest icons, `DEV/org.openl.rules.gen/enums/*.csv` codegen inputs, and a vendored library's own source map.
- The tableeditor `compile.js.sh`/`compile.css.sh` and their `.cmd` twins are manual developer tooling wired into
  nothing. Unreferenced by design — this routine itself runs `compile.js.sh`, so they stay.
- Rules-tree and diff icons are referenced by literal path built as `"images/" + name`; they stay.
- Vendored third-party sources are removed whole or not at all — never trim their API. `js/datepicker.js` and
  `js/prototype/prototype-1.7.3.js` in tableeditor are vendored despite not living under a `vendor/` folder; read the
  file header for a third-party licence before treating any `.js` as ours.

## CI flakes

- `LockTest.testSimultaneousMultiThreadsWithWaiting` in `STUDIO/org.openl.rules.repository` fails on **`main`**, not
  just on sweep branches — job `Tests (without ITEST)`, tell `expected: <800> but was: <79x>`. It asserts all 8x100
  `tryLock` attempts beat a 30 s timeout. Do not rerun and do not treat it as your own breakage: check the latest
  `main` run of `build-quick.yml` first, then say so in the thread once.

## Container facts

- No `gh` CLI. Use the GitHub MCP tools for every PR operation.
- **No Maven goal runs at all**, including `mvn validate -N`: the network policy denies CONNECT to
  `build.shibboleth.net` (403) and `org.opensaml:opensaml-bom:5.2.3` is not on Maven Central (404). That import sits
  in the root dependency management block, so even reading the root pom fails. While this holds the Spotless format
  gate cannot run either — say so rather than claiming the change was format-checked.
- Maven Central, registry.npmjs.org and github.com all work; only shibboleth is blocked. So a dependency jar can be
  fetched with `curl` straight from Central and inspected with `unzip` plus `javap -c` when a convention is in doubt.
- Java 21 and Maven 3.9.11 are installed; only artifact resolution is blocked.
- Listing workflow runs through the GitHub MCP tool overflows the tool result. It saves the JSON to a file; parse that
  with python instead of retrying with a smaller page size. The unit-test workflow is `build-quick.yml`.

## Exhausted veins

- Base-name search over every image, `.xhtml` and `.js` file outside `Docs/`, and over all 610 images under `Docs/`.
- Class and id selectors across all 11 hand-written CSS files in STUDIO — no provable orphan but `.te_hidden`.
- Key-reference scan over `openapi.properties` (605 keys), `ValidationMessages.properties` (204),
  `messages.properties` (46), `sql-errors.properties`, and webstudio `openl-default.properties` (105).
- All 1292 leaf keys of the 15 studio-ui locale bundles; every bundle is English, there is no other language.
- `export`ed const/function/class/interface/type/enum across studio-ui `src` — zero-reference symbols all deferred.
- Import graph over all 345 studio-ui modules — every one has a production importer, so no component is reachable
  only from its own test.
- Function, object-literal-method and prototype definitions across all 23 hand-written legacy `.js` files (211 names in
  tableeditor plus the webstudio ones) — `PopupMenu.showChild` was the single finding.
- Whole-file reference check over every extension outside ITEST and test-resources: Velocity templates, taglibs,
  source maps, fonts, schemas, `.html`, `.csv`, `.sql`, `.groovy`, `.sh`, `.cmd`, `.txt`, `.json`, `.yaml`, `.xml`,
  `.svg`, and all 117 `.properties` files. Every orphan found is convention-loaded — see *Keep-list*.
- All 62 declared npm dependencies of `STUDIO/studio-ui` — every one is imported, or used by a `scripts` entry
  (`license-checker-rseidelsohn` in `build`, `@vitest/coverage-v8` through `--coverage`), or an ambient `@types/*`.
- All 114 distinct properties defined in the 207 poms — every `*.version` is interpolated somewhere, and the only
  uninterpolated ones are plugin convention parameters.
- Duplicate `<dependency>` declarations inside a single pom, and duplicate keys inside a single `.properties` file —
  no real instance; `specs.properties` `duplicateKey` is a deliberate parser fixture.
- `@typescript-eslint/no-unused-vars` over all of studio-ui `src` — zero warnings, detector validated by a planted
  unused variable.

## Human follow-ups

- **Unblock the sweep.** With `build.shibboleth.net` denied no Maven goal runs, and the resource veins are now all
  closed, so the routine has nothing left it may verify. Allowlist that host in the environment's network policy, or
  move the `org.opensaml:opensaml-bom` import out of the root pom, and rows 9-13 of the queue open up at once.
- `main` is red: `LockTest.testSimultaneousMultiThreadsWithWaiting` keeps the `Quick Build` unit-test job failing, so
  no pull request can reach a fully green CI. Its sibling is already `@Disabled` as unstable; this one needs the same
  decision or a real fix to the file-system lock.
- Decide whether the four unused `MergeModal/types.ts` interfaces should stay as the frontend mirror of the merge REST
  contract; if they go, `Docs/api/projects-merge-api.md` moves with them.
- The committed tableeditor CSS bundles do not match what `compile.css.sh` produces from the committed CSS sources, so
  they are stale or hand-edited. Someone has to decide which side is authoritative before any tableeditor CSS removal
  can ship; `.te_hidden` is blocked on it. The JS bundles reproduce exactly and have no such problem.
- The tableeditor bundling step is documented nowhere — no `AGENTS.md` or `Docs/` page mentions the two compile
  scripts or the checked-in yuicompressor jar, and it is not wired into Maven, so editing a source under `js/` or
  `css/` silently fails to reach the runtime. Worth a note in `STUDIO/AGENTS.md`; this routine only deletes.

## Run log

- 07-30 — first run. Maven blocked; swept 7 resource change types; shipped 57 dead i18n keys as PR #1933.
- 07-30 — second run. Maven still 403. Proved the tableeditor JS bundles byte-reproducible and the CSS ones not;
  shipped `PopupMenu.showChild`. Four more veins closed with one finding between them.
- 07-30 — third run. Maven still 403. Six new veins swept (npm dependencies, pom properties, whole-file over every
  remaining extension, all `.properties` files, duplicate declarations, eslint unused vars) — zero findings, one
  deferral. Resource side is now fully mined out; nothing pushed but this file.
