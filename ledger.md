# Dead-code sweep ledger — openl-tablets

## Resume point

Maven is usable again — see *Container facts* for the one-line local POM edit that unblocks it. Start the
reactor build as the FIRST action of the run, in the background, and do everything else while it runs.
No PR is open. Cut a fresh branch off `origin/main`.
Next work: the 39 PMD findings below are already triaged in *Deferred findings*; re-run the scan to get exact
line numbers, then ship the safe ones. Take rows 7, 8 and 10 first (18 findings, low risk), row 9 last.
Row 12 (Maven dependencies) needs the real POM, which cannot coexist with the local edit — see *Method rules*.
Only one sweep PR may be open: check `git ls-remote --heads origin 'dead-code/*'` before cutting a branch.

## Change-type queue

| # | Change type | Status |
|---|---|---|
| 1 | Unreferenced images | done (run 1) |
| 2 | Unreferenced whole `.xhtml` pages | done, no finding (run 1) |
| 3 | Unreferenced whole `.js` / `.css` files | done, no finding (run 1) |
| 4 | Unused `.properties` keys (STUDIO bundles) | done, no finding (runs 1-2) |
| 5 | Dead CSS rules | done for own stylesheets (runs 1, 3); vendor files stay |
| 6 | Dead JS functions (own scripts) | done, no finding (runs 2-3) |
| 7 | Never-read assignments (PMD `UnusedAssignment`) | ready — 12 found (run 4) |
| 8 | Unused local variables (PMD) | ready — 2 found (run 4) |
| 9 | Unused private fields (PMD) | ready — 23 found (run 4), mostly framework-bound |
| 10 | Unused private methods (PMD) | ready — 2 found (run 4) |
| 11 | Unused formal parameters (PMD, private only) | no finding (run 4); webstudio unscanned |
| 12 | Unused declared Maven dependencies | blocked — needs the real root POM (see *Method rules*) |
| 13 | Whole-type deadness in `.impl.` / internal packages | open; public API never removed |
| 14 | Dead TypeScript in `STUDIO/studio-ui` | done, no finding (runs 2-3) |
| 15 | Unused `studio-ui` locale keys | done (run 3, 17 keys) |

## Open PR

None. Cut the next branch from a freshly fetched `origin/main`.

## Merged PRs

- #2054 — 2 commits, 12 lines, change types 1 and 5. Merged by yurkom with no review comments, which sets the
  precedent that a mechanical single-type resource sweep is accepted as-is.
- #2056 — 2 commits, 30 deletions, change types 5 and 15. Merged within 35 minutes, no review comments;
  CodeRabbit and the Sonar quality gate both clean. A locale-key batch spanning three bundles is accepted
  as one commit.

## Module coverage

- Search-provable veins: nothing open. `DEV`, `WSFrontend`, `Util`, `ITEST`, `Docs`, `STUDIO/*` are swept for
  every change type that needs no Maven.
- `STUDIO/org.openl.rules.webstudio` has never been scanned by PMD — it is the one module that cannot be built
  here. Its Java surface is the largest unswept area in the repository.

## Deferred findings

- 17 `rf-*` classes and `ant-select-input` in webstudio `common.css` — generated outside the repository;
  unprovable, keep permanently.
- `MergeRequest`, `ResolveConflictsRequest`, `ResolveConflictsResponse` in
  `STUDIO/studio-ui/src/containers/MergeModal/types.ts` — unused in code, but `Docs/api/projects-merge-api.md`
  documents all three by name as the REST contract. Maintainer decision.
- ~70 exported types in `STUDIO/studio-ui/src` are used only inside their own file. Dropping `export` is a
  refactor, not a deletion, so it stays out of scope permanently.
- `tooltip_skin-{blue,green,red}`, `tooltip_top_center`, `tooltip_top_left` in tableeditor `css/tooltip.css` —
  the widget's theming API; the single caller passes neither, so they are unreachable. Maintainer decision.
- `iframe.iehack` in tableeditor `css/datepicker.css` — no producer, but the file is a vendored stylesheet.
- PMD `UnusedPrivateField`, 19 of 23 are framework-bound and must NOT be deleted without reading the binding:
  the 11 fields in tableeditor `taglib/TableEditorTag.java` and 7 in `taglib/TableViewerTag.java` back JSP tag
  attributes declared in a `.tld`, and the three in `org.openl.rules.jackson` `JsonUtilsTest` plus `gg` in
  `JavaOpenClassTest` are read reflectively. Check the `.tld` and the reflection before touching any of them.
- PMD `UnusedPrivateField` worth acting on: `methodName` in DEV `dimentional/DecisionTableBuilder.java` and
  `description` in `STUDIO/org.openl.security` `SimpleGroup.java` (check serialization on the latter first).
- PMD `UnusedPrivateMethod`: `getAB()` in DEV test bean `generated/epbds6830/BeanA.java` and `getC()` in
  `AOpenClassTest.java` — both are OpenL test fixtures reached by reflection. Verify before deleting.

## False-positive shapes

- A basename regex that admits `(` swallows the paren from markdown `![alt](name.png)`, so the token never
  equals the basename and every image referenced only from markdown looks dead. Strip leading punctuation.
- A plain substring search inflates hits in the other direction: `add.png` matches inside `toolbar_add.png`,
  `add_repository` inside `click_add_repository_to_create_first`. Require a boundary or compare token sets.
- A CSS "class" may be a property value: `.gradient` came from
  `filter: progid:DXImageTransform.Microsoft.gradient(...)`, not a selector.
- A message key can be built by EL `concat` from an enum constant, so the literal key appears nowhere.
- A `.js` / `.css` source whose only hits are `compile.*.sh` / `compile.*.cmd` is a build input, not dead.
- Excluding the defining file from a reference search hides in-file callers: every "dead" function found in
  `common.js` and in the tableeditor scripts is called from its own file. Count hits in the file too.
- An unused *export* is not dead code when the symbol is used inside its own file. Compare total occurrences
  against 1, not external occurrences against 0.
- A key suffixed by convention is never found by its literal: `ValidationMessages.properties` keys, and every
  i18next plural pair, whose `_one` / `_other` forms are reached by the base key plus a `count` option.
- A key composed in a template literal (`browser.compile.${state}`) hides every member of its group. Build a
  regex from each template in the corpus — a bare prefix match is too coarse and hides real findings.
- An identifier regex anchored on a letter never matches a key segment that starts with a digit
  (`expiration_options.7_days`), so such keys look dead in a token scan.
- A composed lookup can still be guarded: `browser.${id}_confirm` fires only when `id === 'unlock'`, so
  `browser.delete_confirm` was dead despite matching the shape. Read the branch, not just the template.
- A private field backing a JSP tag attribute or read by reflection is reported by PMD as unused. Every
  `UnusedPrivateField` hit in a `taglib` package or a test bean is a false positive until the `.tld` or the
  reflective reader says otherwise.

## Method rules

- Prove non-reference with a plain repo-wide full-text search over every text file type, not a regex scoped to
  one attribute, one file type or one module.
- For a large candidate set, extract candidate-shaped tokens from the whole corpus in ONE pass and set-difference;
  per-candidate regex over ~13k files does not finish inside the time budget.
- Confirm every survivor of a bulk scan with an individual `grep -rIF` before deleting it — the bulk scan finds
  candidates, the individual search is the proof.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Search a message key by its full literal AND by its prefix up to the last dot, to catch composed lookups.
- Flatten `studio-ui` locale bundles by replacing `i18next.addResourceBundle(` with a capture function and
  running the file in `node:vm` — the bundles are plain object literals, so no TypeScript tooling is needed.
- Before deleting one name from a grouped CSS selector, verify each remaining name separately; keep the rule.
- A `#`-hash link in legacy WebStudio is a server page route, not a React route. The crossroads routes in
  `index.xhtml` build `page + ".xhtml"` from the fragment, so a page is named without its extension. Search a
  page by its base name with and without `.xhtml`.
- Dead-CSS cleanup has maintainer precedent on `main`: "Drop the common.css titleColumn rules orphaned by the
  retired commit info dialog". Same change type — no need to hedge on it.
- Parse `target/pmd.xml` with the namespace `http://pmd.sourceforge.net/report/2.0.0`. The ruleset namespace
  (`ruleset/2.0.0`) and the schema name (`report_2_0_0`) both differ from it, and either wrong guess silently
  yields zero violations from reports that are full of them.
- Row 12 needs an unedited root POM, which the local build edit removes, so it cannot share a run with the PMD
  rows unless the edit is re-applied for verification and reverted for the commit. Give it a run of its own.

## Keep-list

- `org.openl.rules.tableeditor` `js/*.js` and `css/*.css` are build inputs: `HTMLRenderer` loads
  `js/tableeditor.min.js` and `css/tableeditor.min.css`, concatenated by `compile.js.sh` / `compile.css.sh`.
  Editing a source means regenerating both bundles in the same commit.
- Keys under `ws.project.openapi.mode.` are reached by
  `#{msg['ws.project.openapi.mode.'.concat(project.openapi.mode.name().toLowerCase())]}` in `project.xhtml`.
- `rf-*` (RichFaces, JS inside a jar) and antd-generated class names can never be proven dead.
- `openapi.properties` keys are annotation values resolved by `OpenApiPropertyResolverImpl`; all 625 are live.
- Public API in `DEV/**` and every published artifact is off limits even when unused in-repo.
- `ValidationMessages.properties` keys are looked up by a short form: the code drops the `openl.error.`
  prefix and, for exceptions, the three-digit status segment. All are live. See
  `.claude/skills/localized-exceptions-and-validation-skill/SKILL.md`.
- `onFailure` in the tableeditor scripts is a Prototype Ajax callback, invoked as `'on' + state`.
- ITEST `001-Get-Static-CSS` fixtures GET `/css/common.css` with a `***` wildcard body: they assert status and
  `Content-Type` only, so editing that file's content cannot break them.

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails with `ORA-12516: ... does not have a protocol handler
  for TCP ready or registered for service freepdb1`, the Oracle TestContainer listener not yet accepting
  connections. Infrastructure, never the diff. Budget: one rerun per SHA.
- `rerun_failed_jobs` returns 403 "This workflow is already running" while any job of the run is still in
  progress. Wait for the whole run to finish, then re-run.

## Container facts

- Maven works after deleting ONLY the `org.opensaml:opensaml-bom` import block from the root `pom.xml`
  (`<dependencyManagement>`, near the jackson-bom import). Never commit it; `git checkout -- pom.xml` after.
  With that edit the whole reactor builds except `org.openl.rules.webstudio`, whose opensaml jars resolve only
  from `build.shibboleth.net` (proxy answers 403 to CONNECT); Maven Central has no opensaml artifact at all.
  Exclude it: `mvn install -Dquick -DnoPerf -T1C -B -pl '!:org.openl.rules.webstudio'`.
- A full reactor build from an empty `~/.m2` costs 35-40 minutes. Start it in the background as the run's first
  action and do the search-provable work while it runs; nothing else in the run is that slow.
- The container's global git config signs commits over ssh (`gpg.format=ssh`, `commit.gpgsign=true`), which
  fails `GitRepositoryTest` and `SameSecondHistoryOrderTest` in STUDIO Repository Git with jgit
  `UnsupportedSigningFormatException`. Fix once per session: `git config --global commit.gpgsign false`.
- `mvn pmd:pmd` must run with `-fae`: several ITEST modules cannot resolve `org.openl.itest:server-core`
  outside the itest profile and abort the whole scan at the first one.
- `-rf` breaks resolution for modules built earlier in the same reactor but never installed
  (`org.openl.rules.test`). Resume with a plain full build, not with `-rf`.
- Error Prone contributes nothing: the whole reactor emits 8 unused-* warnings, all reflection false positives
  or `EffectivelyPrivate` visibility narrowing, which is a refactor and out of scope. PMD is the only Java signal.
- Frontend verification works and is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`,
  `npx eslint <files>`, and `npx vitest run` (183 files, ~3 minutes).
- `compile.js.sh` reproduces `tableeditor.all.js` and `.min.js` byte for byte; `compile.css.sh` drops the
  trailing newline of `tableeditor.min.css` and joins two sources without one, so restore the newline and
  expect a one-line comment shift in `tableeditor.all.css`. The `yuicompressor` jar is committed.
- The global git identity is rewritten back to `Claude <noreply@anthropic.com>` mid-session. Re-set it and
  pass `GIT_AUTHOR_*` / `GIT_COMMITTER_*` inline on every commit; `--amend` alone keeps the wrong author,
  so it needs `--reset-author`.
- `git push origin --delete <branch>` fails through the proxy with HTTP 403; normal pushes work.
- `~/.m2/repository` starts empty each session; a full build re-downloads everything.
- `gh` CLI and `xxd` are absent. Use the GitHub MCP tools.
- `.toDelete/` is gitignored (`.gitignore:35`) and safe for scan scratch files.

## Exhausted veins

- Unreferenced images — whole repository, all 710 files, all extensions.
- Unreferenced whole `.xhtml` files (46) and whole `.js` / `.css` files (55 non-`studio-ui`).
- Unused keys in `i18n/openapi.properties` (625), webstudio `messages.properties` (46) and
  `ValidationMessages.properties`.
- Class-level deadness in webstudio `css/common.css`, `layout/main.css`, `layout/simple.css`, and in all seven
  own tableeditor stylesheets (77 class tokens).
- Function and prototype-method deadness in `webapp/javascript/common.js`, `bomjs.js` and every own
  tableeditor script (119 method names).
- Unused-export scan over all 776 exports in `STUDIO/studio-ui/src`, and whole-file deadness over its 562
  source files — only tests and `.d.ts` files are unreferenced, which is expected.
- All 1316 `studio-ui` locale keys, by leaf name and by full dotted path.
- Message bundles and templates outside STUDIO: `DEV`, `WSFrontend` and `Util` hold no message bundle at all
  (only Spring `openl-default.properties`, log4j and maven-invoker fixtures) and no `.ftl` or non-test `.xsd`.
- PMD dead-code scan over the whole reactor except `org.openl.rules.webstudio` — 39 findings, all listed above.

## Human follow-ups

- Allowlist `build.shibboleth.net`, or mirror the opensaml artifacts, so `org.openl.rules.webstudio` can build
  here. It is now the ONLY module this routine cannot compile, and the largest one never scanned.
- Delete the abandoned remote branch `dead-code/studio-resources` (PR #2055 closed unmerged; its only change is
  already on `main` via #2054). `git push --delete` gets HTTP 403 through the proxy and the GitHub MCP server
  has no delete-branch tool, so this needs a human or the repo's auto-delete setting.

## Run log

- Run 2: overlapped run 1 and duplicated its image finding; PR #2055 closed, no code shipped. Swept
  `ValidationMessages.properties`, `layout/*.css`, `common.js`/`bomjs.js` and the studio-ui exports — all clean.
- Run 3: PR #2056 (2 commits, 30 deletions) merged the same hour — 17 dead locale keys and the `.te_hidden`
  rule. Closed rows 5, 6 and 14 and added row 15. Maven still blocked by the shibboleth 403.
- Run 4: unblocked Maven, built the whole reactor bar webstudio, and ran the first PMD scan — 39 findings,
  triaged, none shipped: the build consumed the run. Closed row 11, moved rows 7-10 to ready.
