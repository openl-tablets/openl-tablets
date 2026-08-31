# Dead-code sweep ledger — openl-tablets

## Resume point

No PR is open; cut a fresh branch off `origin/main`. Maven works after the local POM edit in *Container facts* —
start the reactor build as the FIRST action, in the background, with the FULL exclusion list (four modules, not
one) given there. The previous two runs were consumed by that build; budget for it.
Rows 7 and 9 are the only shippable Java work left, and *Deferred findings* already carries every finding with
its exact file and line, so the next run can edit immediately and use the build purely as the verification gate.
Ship row 7 first: exactly two of its nine findings are safe, both named in *Deferred findings*.
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
| 7 | Never-read assignments (PMD `UnusedAssignment`) | ready — 9 found, 2 safe (run 5) |
| 8 | Unused local variables (PMD) | done, no deletable finding (run 5) |
| 9 | Unused private fields (PMD) | ready — 19 found (run 5), 18 framework-bound |
| 10 | Unused private methods (PMD) | done, no finding (run 5) |
| 11 | Unused formal parameters (PMD, private only) | no finding (runs 4-5); webstudio unscanned |
| 12 | Unused declared Maven dependencies | blocked — needs the real root POM (see *Method rules*) |
| 13 | Whole-type deadness in `.impl.` / internal packages | done, no finding (run 5) |
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
  here. Its Java surface is the largest unswept area in the repository. PMD is source-only and would scan it,
  but a Java deletion there could never be compile-verified in this container, so it stays out of scope.

## Deferred findings

Row 7 (`UnusedAssignment`, 9 findings) — SAFE TO DELETE, exactly two:
- `STUDIO/.../workspace/dtr/impl/MappedRepository.java:650` — `baseFolder` is normalized and never read again.
  Delete the assignment statement only; leave the parameter, since dropping it is a signature change.
- `DEV/org.openl.rules/src/org/openl/rules/table/ui/CellStyle.java:16,19` — field initializers for
  `horizontalAlignment` / `verticalAlignment`, overwritten in the only constructor. Confirm no second
  constructor before deleting.

Row 7 — DO NOT DELETE, and the reason is behavioral, not stylistic:
- `WSFrontend/.../management/ServiceManagerImpl.java:228` — PMD names line 228, which is the load-bearing
  assignment: `createService()` on the next line lets other beans read the marker through the
  `@Qualifier("serviceDescriptionInProcess")` getters. The redundant twin is line 230, AFTER the call. Following
  PMD here breaks service deployment.
- `STUDIO/.../repository/git/GitRepository.java:3199,3234` — `result` is assigned in a `try` and re-assigned only
  in `catch (FileNotFoundException)`; on the success path `getResult()` returns it. See *False-positive shapes*.
- `DEV/.../spring/env/DynamicPropertySource.java:156` — field `settings`, same try/catch shape; verify the
  success path before touching.
- `DEV/org.openl.rules.test/src/.../RulesInFolderTestRunner.java:80,116` — `messagesCount` is reported as
  "overwritten on line 72", i.e. BEFORE the flagged lines, so PMD's dataflow is reading a loop back-edge.

Row 9 (`UnusedPrivateField`, 19 findings) — 18 are framework-bound and must NOT be deleted without reading the
binding: 11 fields in tableeditor `taglib/TableEditorTag.java` and 7 in `taglib/TableViewerTag.java` back JSP tag
attributes declared in a `.tld`. Only two are worth acting on: `methodName` in DEV
`validation/properties/dimentional/DecisionTableBuilder.java:32` and `description` in
`STUDIO/org.openl.security` `SimpleGroup.java:14` (check serialization on the latter first).

Not code, kept for a maintainer:
- 17 `rf-*` classes and `ant-select-input` in webstudio `common.css` — generated outside the repository;
  unprovable, keep permanently.
- `MergeRequest`, `ResolveConflictsRequest`, `ResolveConflictsResponse` in
  `STUDIO/studio-ui/src/containers/MergeModal/types.ts` — unused in code, but `Docs/api/projects-merge-api.md`
  documents all three by name as the REST contract.
- ~70 exported types in `STUDIO/studio-ui/src` are used only inside their own file. Dropping `export` is a
  refactor, not a deletion, so it stays out of scope permanently.
- `tooltip_skin-{blue,green,red}`, `tooltip_top_center`, `tooltip_top_left` in tableeditor `css/tooltip.css` —
  the widget's theming API; the single caller passes neither, so they are unreachable.
- `iframe.iehack` in tableeditor `css/datepicker.css` — no producer, but the file is a vendored stylesheet.

## False-positive shapes

- PMD `UnusedAssignment` misreads try/catch: for `x = f();` in a `try` with `x = null;` in the `catch`, it calls
  the try-block assignment overwritten, ignoring the success path. Every hit whose "overwritten on" line sits in
  a `catch` is a false positive.
- PMD `UnusedAssignment` also reports a line as overwritten by an EARLIER line number, which means it followed a
  loop back-edge. Read the loop before believing it.
- A duplicated field assignment straddling a call is not free to remove at the line PMD names: an intervening
  call may read the field through a getter exposed to other beans. Decide which of the two writes is load-bearing.
- `UnusedLocalVariable` on an enhanced-`for` variable used only to count iterations is not deletable — the
  variable cannot be removed without rewriting the loop, which a delete-only sweep may not do.
- A non-public-type scan is dominated by JUnit 5 test classes: `AGENTS.md` requires them package-private and the
  runner discovers them, so none is ever referenced by name. Filter test sources before triaging (674 hits fell
  to 1).
- A package-private `@Component` is injected by its interface, so its own simple name appears in no other file.
- A private member can be the SUBJECT of a test assertion — `assertNull(findMethod(methods, "getC"))` proves the
  private method is not exposed, so PMD reporting it uncalled is the point of the fixture.
- A fixture bean deliberately mixes accessor visibility (private `getAB()` beside public `setAB()`) to exercise
  accessor discovery. Never "tidy" a bean under `test/org/openl/generated/`.
- A private field backing a JSP tag attribute or read by reflection is reported by PMD as unused. Every
  `UnusedPrivateField` hit in a `taglib` package or a test bean is a false positive until the `.tld` or the
  reflective reader says otherwise.
- A basename regex that admits `(` swallows the paren from markdown `![alt](name.png)`, so the token never
  equals the basename and every image referenced only from markdown looks dead. Strip leading punctuation.
- A plain substring search inflates hits in the other direction: `add.png` matches inside `toolbar_add.png`.
  Require a boundary or compare token sets.
- A CSS "class" may be a property value: `.gradient` came from
  `filter: progid:DXImageTransform.Microsoft.gradient(...)`, not a selector.
- A message key can be built by EL `concat` from an enum constant, so the literal key appears nowhere.
- A `.js` / `.css` source whose only hits are `compile.*.sh` / `compile.*.cmd` is a build input, not dead.
- Excluding the defining file from a reference search hides in-file callers. Count hits in the file too, and
  compare total occurrences against 1 rather than external occurrences against 0.
- A key suffixed by convention is never found by its literal: `ValidationMessages.properties` keys, and every
  i18next plural pair, whose `_one` / `_other` forms are reached by the base key plus a `count` option.
- A key composed in a template literal (`browser.compile.${state}`) hides every member of its group. Build a
  regex from each template in the corpus — a bare prefix match is too coarse and hides real findings.
- An identifier regex anchored on a letter never matches a key segment that starts with a digit
  (`expiration_options.7_days`), so such keys look dead in a token scan.
- A composed lookup can still be guarded: `browser.${id}_confirm` fires only when `id === 'unlock'`, so
  `browser.delete_confirm` was dead despite matching the shape. Read the branch, not just the template.

## Method rules

- Prove non-reference with a plain repo-wide full-text search over every text file type, not a regex scoped to
  one attribute, one file type or one module.
- For a large candidate set, extract candidate-shaped tokens from the whole corpus in ONE pass and set-difference;
  per-candidate regex over ~13k files does not finish inside the time budget. The corpus is ~13.5k text files and
  ~4k `.java`; one tokenizing pass over all of it costs well under a minute.
- Confirm every survivor of a bulk scan with an individual `grep -rIF` before deleting it — the bulk scan finds
  candidates, the individual search is the proof.
- For any PMD finding on a FIELD, grep the field name repo-wide before editing: the "never read" window is one
  method, but a field can be read by a callee, another thread, or an injected bean.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Search a message key by its full literal AND by its prefix up to the last dot, to catch composed lookups.
- Flatten `studio-ui` locale bundles by replacing `i18next.addResourceBundle(` with a capture function and
  running the file in `node:vm` — the bundles are plain object literals, so no TypeScript tooling is needed.
- Before deleting one name from a grouped CSS selector, verify each remaining name separately; keep the rule.
- A `#`-hash link in legacy WebStudio is a server page route, not a React route. The crossroads routes in
  `index.xhtml` build `page + ".xhtml"` from the fragment, so search a page by its base name with and without
  the extension.
- Dead-CSS cleanup has maintainer precedent on `main`: "Drop the common.css titleColumn rules orphaned by the
  retired commit info dialog". Same change type — no need to hedge on it.
- Parse `target/pmd.xml` with the namespace `http://pmd.sourceforge.net/report/2.0.0`. The ruleset namespace
  (`ruleset/2.0.0`) and the schema name (`report_2_0_0`) both differ from it, and either wrong guess silently
  yields zero violations from reports that are full of them.
- The root POM has no `<build><plugins>` opening pair to anchor on — `<build>` starts with `<defaultGoal>`. Insert
  the PMD plugin after the unique two-line anchor `</pluginManagement>` followed by `<plugins>`.
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
- `serviceDescriptionInProcess` in `ServiceManagerImpl` is published to other beans through
  `@Qualifier("serviceDescriptionInProcess")` getters; its assignments are a deployment protocol, not bookkeeping.
- Fields in `tableeditor/taglib/*Tag.java` back JSP tag attributes declared in a `.tld`.

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails with `ORA-12516: ... does not have a protocol handler
  for TCP ready or registered for service freepdb1`, the Oracle TestContainer listener not yet accepting
  connections. Infrastructure, never the diff. Budget: one rerun per SHA.
- `rerun_failed_jobs` returns 403 "This workflow is already running" while any job of the run is still in
  progress. Wait for the whole run to finish, then re-run.

## Container facts

- Maven works after deleting ONLY the `org.opensaml:opensaml-bom` import block from the root `pom.xml`
  (`<dependencyManagement>`, near the jackson-bom import). Never commit it; `git checkout -- pom.xml` after.
- Excluding `org.openl.rules.webstudio` is NOT enough: `itest.studio.demo`, `itest.studio.disabled-settings` and
  `itest.studio.acl` depend on its WAR and fail the reactor at module 56 of 84. Use the full list:
  `mvn install -Dquick -DnoPerf -T1C -B -pl '!:org.openl.rules.webstudio,!:itest.studio.demo,!:itest.studio.disabled-settings,!:itest.studio.acl'`
- `pgrep -f "mvn install"` NEVER matches the running build — `mvn` execs java through plexus classworlds, so the
  string is gone from the cmdline. Match `[c]lassworlds`. A wait loop on the wrong pattern exits instantly and
  makes a running build look finished. Wait on a terminal marker in the log instead.
- Killing the launcher shell does NOT kill the Maven JVM. A second launch then races the first on the same
  `target/` dirs and both logs interleave into one file. Check `pgrep -f '[c]lassworlds'` before every launch,
  and never `pkill -f <pattern>` where the pattern also matches your own shell — it kills the tool call.
- A full reactor build from an empty `~/.m2` costs 35-40 minutes and is the run's binding constraint. Start it in
  the background as the first action and do the search-provable work while it runs.
- `mvn pmd:pmd` needs no installed artifacts and produces a usable report even when the reactor later fails; with
  `-fae` it wrote 43 of the module reports after a downstream resolution error. It is far cheaper than the build,
  so a run short on time can scan without a green build — but a Java deletion still needs a compile.
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
- Spotless runs from the `validate` phase on; after any build check `git status` and revert churn you did not
  intend. Runs 4 and 5 saw none beyond the deliberate POM edit.

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
  and no `.ftl` or non-test `.xsd`.
- Whole-type deadness, repo-wide: all 977 non-public top-level types and all 310 types in `.impl.` / `.internal.`
  packages. Zero real findings — do not repeat this scan.
- PMD dead-code scan over the reactor except `org.openl.rules.webstudio` — 29 findings, all listed above.

## Human follow-ups

- Allowlist `build.shibboleth.net`, or mirror the opensaml artifacts, so `org.openl.rules.webstudio` can build
  here. It is now the ONLY module this routine cannot compile, and the largest one never scanned.
- Delete the abandoned remote branch `dead-code/studio-resources` (PR #2055 closed unmerged; its only change is
  already on `main` via #2054). `git push --delete` gets HTTP 403 through the proxy and the GitHub MCP server
  has no delete-branch tool, so this needs a human or the repo's auto-delete setting.

## Run log

- Run 3: PR #2056 (2 commits, 30 deletions) merged the same hour — 17 dead locale keys and the `.te_hidden`
  rule. Closed rows 5, 6 and 14 and added row 15. Maven still blocked by the shibboleth 403.
- Run 4: unblocked Maven, built the whole reactor bar webstudio, and ran the first PMD scan — 39 findings,
  triaged, none shipped: the build consumed the run. Closed row 11, moved rows 7-10 to ready.
- Run 5: no code shipped. Closed rows 8, 10 and 13 as no-finding with repo-wide proof, re-ran PMD to exact line
  numbers, and found the two build/process facts that had been silently wasting runs (incomplete `-pl` exclusion
  list, `pgrep` never matching Maven). Caught that PMD's named line in `ServiceManagerImpl` is the load-bearing
  one.
