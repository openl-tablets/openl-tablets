# Dead-code sweep ledger — openl-tablets

## Resume point

- Open PR #2092 on `dead-code/legacy-web-resources` (2 commits, head `a7e76d36e6`): maintain it first (section 4).
- Next: finish the `.xhtml` page and `.properties` key scans (started, no result recorded) and fold `.xhtml` hits into the
  JS commit with `--fixup`; then verify the PMD hits listed under Deferred findings (test fixtures) and drop the safe ones.
- Do not repeat the veins in Exhausted veins; the PMD scan itself is done and its non-generated hits are all recorded here.

## Change-type queue

| # | Change type | Proof | Status |
|---|-------------|-------|--------|
| 1 | Commented-out code | read + grep | done (#2088) |
| 2 | Unused non-public fields | ASM field-access scan | done (#2088) |
| 3 | Uncalled non-public methods | ASM invoke scan + word index | done (#2088) |
| 4 | Test workbooks no test loads | base-name grep | done (#2088) |
| 5 | Unreferenced images | base-name grep, whole word | exhausted, zero yield |
| 6 | Unreferenced CSS rules | selector token grep | in PR #2092 (`.clickable`); vendor and tooltip rules deferred |
| 7 | Unreferenced legacy JS functions and `.xhtml` pages | name grep incl. Java-built HTML | JS in PR #2092; `.xhtml` scan pending |
| 8 | Unreferenced `.properties` keys | key grep + bundle lookup convention | scan pending |
| 9 | Never-read assignments and unused locals (PMD) | PMD + full build | scanned 2026-09-09; hits in Deferred findings |
| 10 | Unused private fields/methods/parameters (PMD) | PMD + full build | scanned 2026-09-09; hits in Deferred findings |
| 11 | Dead internal types (non-public / `.impl.`) | ASM class refs + word index | exhausted (#2088) |
| 12 | Unused TypeScript exports (`studio-ui`) | export grep + own-file count | exhausted, zero yield |
| 13 | Unused declared Maven dependencies | `dependency:analyze-only` after full build | queued, last |

## Open PR

- #2092 `dead-code/legacy-web-resources`, head `a7e76d36e6`, base `main` @ `5698aad6b6`; CI running at end of run.
- `db993d6ffc` Remove table editor script members no page, renderer or script calls (type 7, 5 files, −56)
- `a7e76d36e6` Drop the clickable CSS rules no page applies (type 6, 1 file, −10)
- No review threads yet; CodeRabbit was rate-limited on the first push.

## Merged PRs

- #2088 — 5 commits (types 1–4), 31 files, −303 lines + 18 workbooks; the commented-out-code commit landed on `main` directly.

## Module coverage

- Types 1–5, 11, 12 cover every module. Type 6/7: tableeditor and webstudio legacy webapp scanned; DEMO and
  ruleservice static pages have no dead rules or scripts. Type 9/10: whole reactor scanned, hits below.

## Deferred findings

- `ADtColumnsDefinitionTableBoundNode.isReturns()`, `AbstractOpenlTableExporter.getExcelSheetName()` — protected abstract, no caller, outside `.impl.`.
- `IConditionEvaluator.DECORATOR_CONDITION_PRIORITY`, `IColorFilter.COLORS`, `IBoundModuleNode`, `RuleServicePublisherMapper`, `XlsProjectionType` constants `GRID`…`CELL_FONT` — public API read by nothing.
- `JAXRSOpenLServiceEnhancerTest` enhances `TestNotAnnotatedByApiResponsesInterface` instead of its own fixture — test bug, not dead code.
- `tableeditor/.../taglib/TableEditorTag`, `TableViewerTag` — public JSP tag handlers, no `.tld`, referenced nowhere; PMD reports all their private fields unused. Whole-class candidates; public, so human decision.
- Write-only fields behind a public setter (removal needs an API change): `DecisionTableBuilder.methodName`, `SimpleGroup.description`.
- `jQuery.sub` in `webapp/javascript/vendor/jquery-back-compat.js` — no caller, but inside a vendored shim file.
- Vendored CSS rules with zero-hit tokens: `bootstrap.min.css` (`clearfix`, `hide-text`, `input-block-level`, `pagination-right/-large/-small`, `typeahead`, `dropup`, `navbar-fixed-bottom`, `dropdown-submenu`, `pull-right`), `diff2html.css` (`selecting-left/-right`).
- `tableeditor/css/tooltip.css` skins `green`/`red` and position `top_center` — never passed by a caller, but `tooltip.js` still handles them.
- PMD field-assignment hits (`value assigned to field … overwritten`): `DynamicPropertySource.settings`, `GitRepository.result` ×2, `AProjectCreator.createdProjectName`, `DebugChannel.status`, `DebugHookImpl.pendingDispatch/pendingChosen`, `ServiceManagerImpl.serviceDescriptionInProcess` — dropping the first write changes state on an exception path; keep.
- PMD test-fixture hits to verify next run: `CastFactoryTest` `y` (lines 122/129), `JsonUtilsTest` `key` (181) and private fields `key`/`value`/`field` (132–137), `JavaOpenClassTest.gg`, `AOpenClassTest.C.getC()`, `epbds6830/BeanA.getAB()`.

## False-positive shapes

- Lombok accessors: a field `lessThan`/`incBound` is read as `isLessThan()`/`getIncBound()`; search the property name, not the accessor.
- Overrides of library methods carry no caller in the repo (`removeEldestEntry`, `createPropertyResolver`, `resolveDiscriminator`, `loadBus`, Spring `@ExceptionHandler`).
- Jackson mix-ins (`GroovyObject.getMetaClass`), `@Bean` methods, JAXB `beforeMarshal`/`afterUnmarshal` callbacks are invoked by name.
- Record component accessors (`PKey.in()`) and compile-time constant fields (inlined) show no bytecode reference.
- `public` fields of test beans are filled by Jackson or the OpenL binder by reflection; private members of reflection fixtures (`JavaOpenClassTest`, `AOpenClassTest`, `epbds6830` beans) exist to be discovered, not called.
- Test types with no reference: `@Test` classes run by Surefire, Spring `TestRunner*` configs and the controllers they scan, tests inherited from an abstract base, directory-scan fixtures (`WizardUtilsTest`), JMH benchmarks, JUnit extensions in `META-INF/services`, `@Delegate(excludes=...)` marker types.
- Enum constants named nowhere are iterated through `values()` or are public API.
- Spring XML beans with no id reference are injected by type, picked by SpEL, or have lifecycle side effects.
- Maven properties with no `${}` reference are plugin user properties.
- `Docs/**` pages are never orphaned: the sidebar is generated from `site.pages`.
- PMD `UnusedLocalVariable` flags the for-each variable of a counting loop and a try-with-resources variable; both are syntax, not dead code.
- PMD `UnusedAssignment` flags a field initializer that a constructor overwrites even when an early `return` keeps it (`CellStyle`), and the reassigned parameter of a record compact constructor (`MergeResult`).
- PMD reports the JavaCC output under `target/generated-sources/javacc`; drop every hit whose path contains `target/`.
- A TypeScript export used only inside its own file is a superfluous `export` keyword, not dead code; one used only by its own test is a test fixture.
- Legacy JS method names that are common words (`show`, `hide`, `getValue`, `save`) are reached through `this.editor.<name>` in `TableEditor.js`; confirm there before judging.

## Method rules

- Prove non-reference with `grep -rIwF <token>` over the whole repo excluding `target/`, `node_modules/`, `.git/`, `studio-ui/coverage`, `studio-ui/dist`; never a regex scoped to one file type.
- Search a candidate name inside `.xlsx` workbooks too (`unzip -p | grep -wF`): rule tables name Java methods.
- Include the maven-plugin `it/` project sources in every search; the reactor does not compile them.
- Test types are invisible outside their module unless it publishes a test-jar (only `ruleservice` and `ruleservice.deployer` do).
- `test/rules/**` workbooks are not on any classpath; a test opens them by literal relative path, so a base-name grep is the proof.
- Scan every image/resource name in one pass: `grep -rIohwF -f names.txt` then `comm` against the name list.
- A legacy JS name is alive when `HTMLRenderer.java` emits it inside a Java string, when `table.xhtml` calls it through `getCurrentTable()`, or when an `.xhtml` inline handler names it; search `.java` and `.xhtml` before `.js`.
- A CSS token counts as used only in a class-applying context (`class=`, `styleClass=`, `*Class=`, `columnClasses`, JS `className`/`classList`, selector strings, EL ternaries, Java HTML strings); prose, comments and property values are not usages.
- After editing any `tableeditor/js/*.js` or `css/*.css`, regenerate the bundles with `compile.js.sh` / `compile.css.sh` (yuicompressor jar in the module) and commit them in the same commit; pristine sources reproduce the committed bundles byte for byte.
- Run PMD as `mvn -o compile pmd:pmd -DskipTests -Dquick -DnoPerf -T1C -fae` with the plugin in the root pom: a bare `pmd:pmd` cannot resolve reactor modules from `~/.m2` (`org.openl.rules.test`, `server-core` are never installed).

## Keep-list

- Workbooks under a folder a test loads as a whole project (`decisionTableIndexes/`) stay even when unnamed.
- `DEMO/**` assets are named by `index.html`; `DynamicPropertySource` loads `<appName>.properties` by convention.
- Code-generator templates rewrite only the `INSERT` block of `DefaultTablePropertiesSorter` and `DefaultPropertiesContextMatcher`.
- RichFaces `rf-*` classes are rendered by JS inside a jar; never provable dead (even `rf-fu-*` with no `rich:fileUpload` tag left).
- `jquery.layout.js` builds `ui-layout-*` class names at runtime and uses `$.fn.draggable`/`$.effects` from `jquery-ui`; both stay.
- `tooltip.js` builds `tooltip_skin-<skin>` and `tooltip_<position>` class names at runtime from caller options.
- `TableEditor.Operations` values are URL action names matched by method names in `TableEditorController`; string-linked.
- The React app reaches legacy JS only through `globalThis.openl.notification/loader` (`common.js`).

## CI flakes

- `OpenLTableLogicTest.detectsErrorsInRulesTestedByTable` (STUDIO webstudio) — async compile race, `expected: <true> but was: <false>`; rerun once.
- `UserDatailsTab.test.tsx` "rejects an empty email…" — vitest wait timeout on a slow runner; rerun once.
- `Sonar analysis` › `Aggregate coverage`: `Unknown block type 3` reading `ITEST/server-core/target/jacoco.exec` — overlapping artifact download; rerun once.
- `SonarCloud Code Analysis` gate: deleting lines above an existing `S2259` finding re-attributes it to new code; deterministic, comment instead of rerun.

## Container facts

- `gh` is absent; use the GitHub MCP tools (`pull_request_read`, `update_pull_request`, `add_issue_comment`, `actions_list`, `get_job_logs`); reruns are not possible from here.
- `~/.m2` starts empty; the first `mvn clean install -Dquick -DnoPerf -T1C` needs the network (~30 min for the whole reactor once cached); pass `-Daether.syncContext.named.time=600` or parallel resolution fails with "Could not acquire lock(s)".
- Never resume with `-rf` after a `-T1C` failure: the parallel order leaves earlier reactor modules unbuilt and resolution fails. Rerun from the root.
- `mvn -o` fails on ITEST modules until their test dependencies were downloaded once; run the whole-repo build online.
- Run Maven with `LANG=C.UTF-8` and `git config --global commit.gpgsign false`: no locale breaks one archive test, and JGit reads `~/.gitconfig`, where the sandbox forces ssh signing (env overrides do not reach JGit).
- `~/.gitconfig` sets the `Claude` identity; set `user.*` globally and check `git log -1 --pretty='%an|%cn'` after the first commit.
- `git push origin --delete <branch>` fails with "remote end hung up"; the proxy refuses branch deletes.
- Never `git switch --orphan` in the working tree while a build runs: it empties the tree. Commit the ledger with `hash-object` → `mktree` → `commit-tree` → `update-ref refs/heads/dead-code/ledger`, then push.
- `node_modules` in `studio-ui` is created by the Maven build; do not run npm there while Maven runs.

## Exhausted veins

- Unreferenced images, whole repo (703 files outside ITEST): every base name occurs as a whole word in a text file.
- Non-public methods, fields and types without bytecode reference, whole reactor (5140 classes): remaining hits are all alive.
- Spring XML beans, `DEMO/**` assets, Maven properties, enum constants, `Docs/**` pages: zero yield.
- Test workbooks by base name, whole repo outside ITEST.
- TypeScript exports in `studio-ui` (849): none unreferenced except two test-only constants.
- Legacy JS names (382 declarations in 38 files) and whole-file loaders: only the four members in #2092 and `jQuery.sub` are dead.
- CSS selector tokens in all 17 stylesheets: only `.clickable` is dead outside vendored files and runtime-built names.
- PMD `UnusedAssignment`, `UnusedLocalVariable`, `UnusedPrivateField`, `UnusedPrivateMethod`, `UnusedFormalParameter`, whole reactor outside ITEST: 69 hits, all classified above.

## Human follow-ups

- Delete the merged remote branch `dead-code/uncalled-methods` (the sandbox cannot).
- Decide on the public-API items in Deferred findings, notably the two JSP tag handler classes.
- Fix the `JAXRSOpenLServiceEnhancerTest` fixture mix-up.
- `ComponentTypeArrayOpenClass.isAssignableFrom`/`isInstance` null guards (SonarCloud `S2259`, pre-existing on `main`).

## Run log

- 2026-09-09 a: #2088 merged; ledger created.
- 2026-09-09 b: PR #2092 opened (JS members, `.clickable`); images, TS exports, PMD scanned; `.xhtml`/`.properties` scan unfinished.
