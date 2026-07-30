# Dead-code sweep ledger

State memory for the daily sweep of openl-tablets. Read in full at the start of every run.

## Resume point

- Open PR #1933 on `dead-code/studio-resources`, 2 commits, head `4d1cf6b657`. Body rewritten to match the diff
  (12 files, 1 insertion, 74 deletions). No review threads; the only red job is the `main`-wide `LockTest` failure.
- First action next run: retry `mvn validate -N`. If the opensaml BOM resolves, the five Java/Maven queue rows open
  up; if it 403s again, stay on resource rows.
- Every cheap resource vein is now exhausted (see *Exhausted veins*). The remaining resource ideas are all
  judgment-heavy: `Docs/` markdown pages nothing links to (Jekyll publishes them regardless, so orphan is not dead),
  and whole-file removal inside vendored libraries.

## Change-type queue

| # | Change type | Status |
|---|---|---|
| 1 | Unreferenced images | done 07-30, no findings (incl. all 610 under `Docs/`) |
| 2 | Unreferenced `.xhtml` pages | done 07-30, no findings |
| 3 | Dead CSS classes and ids | done 07-30, only `.te_hidden`, deferred |
| 4 | Unreferenced `.js` files | done 07-30, no findings |
| 5 | Unused `.properties` keys | done 07-30, no findings |
| 6 | Unused i18n keys in studio-ui bundles | done 07-30, 57 keys in PR #1933 |
| 7 | Unreferenced exported TS symbols and modules | done 07-30, only deferred findings |
| 8 | Dead functions inside legacy `.js` | done 07-30, `PopupMenu.showChild` in PR #1933 |
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
- No review threads. Both change types are distinct, so nothing to squash.

## Merged PRs

(none)

## Module coverage

- `STUDIO/studio-ui` — locale bundles and the whole module import graph swept; only deferrals left.
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

## Method rules

- Prove non-reference with a plain repo-wide literal search excluding `target/`, `node_modules/` and `.git/` — every
  file type, never a regex scoped to one attribute or one module.
- For a bundle key, search the full dotted path **and** the bare leaf name; either hit means keep.
- Validate any new bulk detector by feeding it two fabricated names; if they come back "referenced", the search is
  wrong, not the repository.
- Resolve studio-ui imports with tsconfig `paths` `"*": ["./src/*"]` (a bare specifier is `src/`-relative), and follow
  side-effect `import 'x'` and `vi.mock('x')` too, or barrel-only and test-only modules look dead.
- The tableeditor JS bundles `js/tableeditor.all.js` and `js/tableeditor.min.js` are checked in and are what the
  runtime loads, not the individual sources. `bash compile.js.sh` reproduces both byte-for-byte from the unmodified
  sources using the checked-in `yuicompressor-2.4.7.jar`, so any source removal must regenerate them in the same
  commit. `compile.css.sh` does **not** reproduce the committed CSS bundles — never regenerate those.
- Run the frontend gate from `STUDIO/studio-ui` with no Maven build competing: `npx tsc --noEmit`, `npx eslint src`
  (whole tree, not just edited files), `npx vitest run`. Baseline is 162 files / 1410 tests green, eslint and tsc clean.
- `npm ci` works here — registry.npmjs.org bypasses the proxy. `node_modules` is gitignored.
- When a deletion empties a parent object literal, delete the parent in the same commit.
- A PR body loses angle-bracketed placeholders even inside backticks — write such a segment as prose, then re-read the
  stored body to confirm.

## Keep-list

- `RestRuntimeException.getErrorCode()` builds `openl.error.` + HTTP status + the code passed to the exception, so a
  `ValidationMessages.properties` key is only ever written as its suffix in Java. Search by suffix.
- A JSF page reaching a bundle as a prefix string concatenated with a lowercased enum name keeps that whole
  `messages.properties` family alive.
- Property names composed at runtime stay: `repo-default.` plus the repository type plus the suffix, and
  `repository.` plus the id plus `.settings.`; the reference-key suffix is exercised from ITEST init params.
- Files that exist by library convention stay (Bean Validation message overrides, favicons, web-manifest icons, tag
  library descriptors under `META-INF/`, a vendored library's own source map).
- Rules-tree and diff icons are referenced by literal path built as `"images/" + name`; they stay.
- Vendored third-party sources are removed whole or not at all — never trim their API. `js/datepicker.js` and
  `js/prototype/prototype-1.7.3.js` in tableeditor are vendored despite not living under a `vendor/` folder; read the
  file header for a third-party licence before treating any `.js` as ours.

## CI flakes

- `LockTest.testSimultaneousMultiThreadsWithWaiting` in `STUDIO/org.openl.rules.repository` fails on **`main`**, not
  just on sweep branches — job `Tests (without ITEST)`, tell `expected: <800> but was: <79x>`. It asserts all 8x100
  `tryLock` attempts beat a 30 s timeout. Ten of the last eleven `Quick Build` runs on `main` are red on it. Do not
  rerun and do not treat it as your own breakage: check the latest `main` run first, then say so in the thread once.

## Container facts

- No `gh` CLI. Use the GitHub MCP tools for every PR operation.
- **No Maven goal runs at all**, including `mvn validate -N`: the network policy denies CONNECT to
  `build.shibboleth.net` (403), `org.opensaml:opensaml-bom:5.2.3` is not on Maven Central (404), and that import sits
  in the root dependency management block, so even reading the root pom fails. Confirm with the agent-proxy status
  endpoint. While this holds, the Spotless format gate cannot run either — say so rather than claiming the change was
  format-checked.
- Java 21 and Maven 3.9.11 are installed; only artifact resolution is blocked.
- Listing workflow runs through the GitHub MCP tool overflows the tool result. It saves the JSON to a file; parse that
  with python instead of retrying with a smaller page size.

## Exhausted veins

- Base-name search over every image, `.xhtml` and `.js` file outside `Docs/`, and over all 610 images under `Docs/`.
- Class and id selectors across all 11 hand-written CSS files in STUDIO — no provable orphan but `.te_hidden`.
- Key-reference scan over `openapi.properties` (605 keys), `ValidationMessages.properties` (204),
  `messages.properties` (46), `sql-errors.properties`, and webstudio `openl-default.properties` (105).
- All 1292 leaf keys of the 15 studio-ui locale bundles.
- `export`ed const/function/class/interface/type/enum across studio-ui `src` — zero-reference symbols all deferred.
- Import graph over all 345 studio-ui modules — every one has a production importer, so no component is reachable
  only from its own test.
- Function, object-literal-method and prototype definitions across all 23 hand-written legacy `.js` files (211 names in
  tableeditor plus the webstudio ones) — `PopupMenu.showChild` was the single finding.
- Whole-file reference check over Velocity templates, tag library descriptors, source maps, fonts and schema files
  outside ITEST — all referenced.

## Human follow-ups

- Decide whether the four unused `MergeModal/types.ts` interfaces should stay as the frontend mirror of the merge REST
  contract; if they go, `Docs/api/projects-merge-api.md` moves with them.
- The committed tableeditor CSS bundles do not match what `compile.css.sh` produces from the committed CSS sources, so
  they are stale or hand-edited. Someone has to decide which side is authoritative before any tableeditor CSS removal
  can ship; `.te_hidden` is blocked on it. The JS bundles have no such problem — they reproduce exactly.
- The tableeditor bundling step is documented nowhere — no `AGENTS.md` or `Docs/` page mentions the two compile
  scripts or the checked-in yuicompressor jar, and it is not wired into Maven, so editing a source under `js/` or
  `css/` silently fails to reach the runtime. Worth a note in `STUDIO/AGENTS.md`; this routine only deletes, so it did
  not add one.
- `main` is red: `LockTest.testSimultaneousMultiThreadsWithWaiting` keeps the `Quick Build` unit-test job failing, so no
  pull request can reach a fully green CI. Its sibling is already `@Disabled` as unstable; this one needs the same
  decision or a real fix to the file-system lock. A maintainer call, out of scope for this routine.

## Run log

- 07-30 — first run. Maven blocked; swept 7 resource change types; shipped 57 dead i18n keys as PR #1933.
- 07-30 — second run. Maven still 403. Proved the tableeditor JS bundles byte-reproducible and the CSS ones not;
  shipped `PopupMenu.showChild`. Four more veins (Docs images, legacy JS functions, studio-ui import graph, misc
  resource types) closed with one finding between them.
