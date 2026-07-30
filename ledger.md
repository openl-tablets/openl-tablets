# Dead-code sweep ledger

State memory for the daily sweep of openl-tablets. Read in full at the start of every run.

## Resume point

- Open PR #1933 on `dead-code/studio-resources`, 1 commit `ba44801c5e`, `mergeable_state` blocked only by the red check.
  `Build artifacts` and all 6 ITEST jobs green; `Tests (without ITEST)` hit the `LockTest` flake below, diagnosis
  posted, rerun 1 of 2 in flight. If it goes green, say nothing; if it flakes again, one rerun is left.
- First action next run: retry `mvn -o clean install -Dquick -DnoPerf -T1C`. If the opensaml BOM resolves, the eight
  Java/Maven queue rows open up; if it 403s again, stay on resource rows.
- Resource veins still untouched: dead functions inside the legacy `.js` files, `Docs/` images and pages, React
  components referenced only by their own test.

## Change-type queue

| # | Change type | Status |
|---|---|---|
| 1 | Unreferenced images | done 07-30, no findings |
| 2 | Unreferenced `.xhtml` pages | done 07-30, no findings |
| 3 | Dead CSS classes and ids | done 07-30, no findings |
| 4 | Unreferenced `.js` files | done 07-30, no findings |
| 5 | Unused `.properties` keys | done 07-30, no findings |
| 6 | Unused i18n keys in studio-ui bundles | done 07-30, 57 keys in PR #1933 |
| 7 | Unreferenced exported TS symbols | done 07-30, only deferred findings |
| 8 | Dead functions inside legacy `.js` | todo |
| 9 | Never-read assignments (PMD) | blocked: no Maven |
| 10 | Unused private fields and locals (PMD) | blocked: no Maven |
| 11 | Unused private methods and formal params (PMD) | blocked: no Maven |
| 12 | Unused nested / effectively-private types (javac) | blocked: no Maven |
| 13 | Unused declared Maven dependencies | blocked: no Maven |

## Open PR

- Branch `dead-code/studio-resources`, PR #1933, head `ba44801c5e`, ready for review.
- Commit 1 — `Remove i18n keys no longer referenced by any studio-ui component` (9 files, 63 deletions).
- No review threads yet.

## Merged PRs

(none)

## Module coverage

- `STUDIO/studio-ui` — locale bundles swept; unused-export vein produced deferrals only.
- `STUDIO/org.openl.rules.webstudio`, `STUDIO/org.openl.rules.tableeditor` — resources swept clean; Java untouched.
- Every other module — Java only, so nothing swept while Maven is blocked.

## Deferred findings

- `STUDIO/studio-ui/src/containers/MergeModal/types.ts`: `MergeRequest`, `ResolveConflictsRequest`,
  `ResolveConflictsResponse`, `FileConflictResolution` — unused in TS but mirror a live REST contract documented in
  `Docs/api/projects-merge-api.md` with a Java record and an OpenAPI schema. Needs a human decision.
- ~45 studio-ui exported types used only inside their own file — alive; dropping `export` is a refactor, not a deletion.
- `.te_hidden` in `STUDIO/org.openl.rules.tableeditor/css/common.css` — only real CSS orphan found, but it is also in
  the checked-in aggregates `tableeditor.all.css` and `tableeditor.min.css`, which no run has yet proven regenerable.
- `jakarta.validation.constraints.Size.message` in `ValidationMessages.properties` — Bean Validation default override.

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

## Method rules

- Prove non-reference with `grep -rIF <token> .` excluding `target/`, `node_modules/`, `.git/` — repo-wide, every file
  type, never a regex scoped to one attribute or one module.
- For a bundle key, search the full dotted path **and** the bare leaf name; either hit means keep.
- Run the frontend gate from `STUDIO/studio-ui` with no Maven build competing: `npx tsc --noEmit`, `npx eslint src`
  (whole tree, not just edited files), `npx vitest run`. Baseline is 162 files / 1410 tests green, eslint and tsc clean.
- `npm ci` works here — registry.npmjs.org bypasses the proxy. `node_modules` is gitignored.
- When a deletion empties a parent object literal, delete the parent in the same commit.
- A PR body loses angle-bracketed placeholders even inside backticks — the stored body dropped `<type>` from
  `repo-default.<type>.*`, leaving `repo-default..*`. Write such a segment as prose and re-read the stored body.

## Keep-list

- `RestRuntimeException.getErrorCode()` builds `openl.error.` + HTTP status + the code passed to the exception, so a
  `ValidationMessages.properties` key is only ever written as its suffix in Java. Search by suffix.
- `#{msg['<prefix>'.concat(enumValue.name().toLowerCase())]}` in JSF pages keeps a whole `messages.properties` family.
- `repo-default.<repoType>.*` and `repository.<id>.settings.*` property names are composed at runtime; the `$ref`
  suffix is a reference mechanism exercised from ITEST init params.
- Files that exist by library convention stay (Bean Validation message overrides, favicons, web-manifest icons).
- Rules-tree and diff icons are referenced by literal path built as `"images/" + name`; they stay.

## CI flakes

- `LockTest.testSimultaneousMultiThreadsWithWaiting` in `STUDIO/org.openl.rules.repository`, job `Tests (without
  ITEST)`. Tell: `expected: <800> but was: <798>` — a count just under 800. It asserts all 8x100 `tryLock` attempts
  succeed with a 30 s timeout each, so a loaded runner times a few out. Rerun, at most twice per SHA; never a code fix.

## Container facts

- No `gh` CLI. Use the GitHub MCP tools for every PR operation.
- **No Maven goal runs at all**, including `mvn validate -N`: the network policy denies CONNECT to
  `build.shibboleth.net` (403), `org.opensaml:opensaml-bom:5.2.3` is not on Maven Central, and that import sits in the
  root dependency management block, so even reading the root pom fails. Confirm with
  `curl -sS "$HTTPS_PROXY/__agentproxy/status"`. While this holds, the Spotless format gate cannot run either — say so
  rather than claiming the change was format-checked.
- Java 21 and Maven 3.9.11 are installed; only artifact resolution is blocked.

## Exhausted veins

- Base-name search over every image, `.xhtml` and `.js` file outside `Docs/` — all referenced.
- Class and id selectors across all 11 hand-written CSS files in STUDIO — no provable orphan but `.te_hidden`.
- Key-reference scan over `openapi.properties` (605 keys), `ValidationMessages.properties` (204),
  `messages.properties` (46), `sql-errors.properties`, and webstudio `openl-default.properties` (105).
- All 1292 leaf keys of the 15 studio-ui locale bundles.
- `export`ed const/function/class/interface/type/enum across studio-ui `src` — zero-reference symbols all deferred.

## Human follow-ups

- Decide whether the four unused `MergeModal/types.ts` interfaces should stay as the frontend mirror of the merge REST
  contract; if they go, `Docs/api/projects-merge-api.md` moves with them.
- Confirm whether `tableeditor.all.css` / `.min.css` and `tableeditor.all.js` / `.min.js` are build outputs or
  hand-maintained. Every tableeditor resource removal is blocked on this answer.

## Run log

- 07-30 — first run. Maven blocked; swept 7 resource change types; shipped 57 dead i18n keys as PR #1933.
