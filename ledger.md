# Dead-code sweep ledger — openl-tablets

## Resume point

- No open sweep PR; #2088 merged. Next branch is cut from a fresh `origin/main`.
- Next change types: unreferenced CSS rules, legacy JS functions and `.xhtml` pages, `.properties` keys, then PMD.
- The repo-wide image scan is exhausted (see Exhausted veins); do not repeat it.

## Change-type queue

| # | Change type | Proof | Status |
|---|-------------|-------|--------|
| 1 | Commented-out code | read + grep | done (#2088, main 2026-09-09) |
| 2 | Unused non-public fields | ASM field-access scan | done (#2088) |
| 3 | Uncalled non-public methods | ASM invoke scan + word index | done (#2088) |
| 4 | Test workbooks no test loads | base-name grep | done (#2088) |
| 5 | Unreferenced images | base-name grep, whole word | exhausted, zero yield (2026-09-09) |
| 6 | Unreferenced CSS rules | selector token grep | queued |
| 7 | Unreferenced legacy JS functions and `.xhtml` pages | name grep incl. Java-built HTML | queued |
| 8 | Unreferenced `.properties` keys | key grep + bundle lookup convention | queued |
| 9 | Never-read assignments and unused locals (PMD) | PMD + full build | queued |
| 10 | Unused private formal parameters (PMD) | PMD + full build | queued |
| 11 | Dead internal types (non-public / `.impl.`) | ASM class refs + word index | exhausted, 22 hits all alive (#2088) |
| 12 | Unused TypeScript exports (`studio-ui`) | tsc + eslint + name grep + vitest | queued |
| 13 | Unused declared Maven dependencies | `dependency:analyze-only` after full build | queued, last |

## Open PR

- none

## Merged PRs

- #2088 — 5 commits (types 1–4), 31 files, −303 lines + 18 workbooks; the commented-out-code commit landed on `main` directly.

## Module coverage

- Types 1–4, 5 and 11 cover every module; every other type is untouched in every module.

## Deferred findings

- `ADtColumnsDefinitionTableBoundNode.isReturns()` and `AbstractOpenlTableExporter.getExcelSheetName()` — protected abstract, no caller, outside `.impl.`; human decision.
- `IConditionEvaluator.DECORATOR_CONDITION_PRIORITY`, `IColorFilter.COLORS` — public constants read by nothing; public API.
- `IBoundModuleNode`, `RuleServicePublisherMapper` — public types referenced nowhere; public API.
- `XlsProjectionType` (`org.openl.rules.diff`) — nine public constants `GRID`…`CELL_FONT` named nowhere; public API.
- `JAXRSOpenLServiceEnhancerTest` enhances `TestNotAnnotatedByApiResponsesInterface` instead of its own fixture `TestWithOperationNotAnnotatedByApiResponsesInterface` — test bug, not dead code.

## False-positive shapes

- Lombok accessors: a field `lessThan`/`incBound` is read as `isLessThan()`/`getIncBound()`; search the property name, not the accessor.
- Overrides of library methods carry no caller in the repo (`removeEldestEntry`, `createPropertyResolver`, `resolveDiscriminator`, `loadBus`, Spring `@ExceptionHandler`).
- Jackson mix-ins (`GroovyObject.getMetaClass`), `@Bean` methods, JAXB `beforeMarshal`/`afterUnmarshal` callbacks are invoked by name.
- Record component accessors (`PKey.in()`) and compile-time constant fields (inlined) show no bytecode reference.
- `public` fields of test beans are filled by Jackson or the OpenL binder by reflection.
- Test types with no reference: `@Test` classes run by Surefire, Spring `TestRunner*` configs and the controllers they scan, tests inherited from an abstract base, directory-scan fixtures (`WizardUtilsTest`), JMH benchmarks, JUnit extensions in `META-INF/services`, `@Delegate(excludes=...)` marker types.
- Enum constants named nowhere are iterated through `values()` or are public API.
- Spring XML beans with no id reference are injected by type, picked by SpEL, or have lifecycle side effects.
- Maven properties with no `${}` reference are plugin user properties.
- `Docs/**` pages are never orphaned: the sidebar is generated from `site.pages`.

## Method rules

- Prove non-reference with `grep -rIwF <token>` over the whole repo excluding `target/`, `node_modules/`, `.git/`; never a regex scoped to one file type.
- Search a candidate name inside `.xlsx` workbooks too (`unzip -p | grep -wF`): rule tables name Java methods.
- Include the maven-plugin `it/` project sources in every search; the reactor does not compile them.
- Test types are invisible outside their module unless it publishes a test-jar (only `ruleservice` and `ruleservice.deployer` do).
- `test/rules/**` workbooks are not on any classpath; a test opens them by literal relative path, so a base-name grep is the proof.
- Scan every image/resource name in one pass: `grep -rIohwF -f names.txt` then `comm` against the name list.

## Keep-list

- Workbooks under a folder a test loads as a whole project (`decisionTableIndexes/`) stay even when unnamed.
- `DEMO/**` assets are named by `index.html`; `DynamicPropertySource` loads `<appName>.properties` by convention.
- Code-generator templates rewrite only the `INSERT` block of `DefaultTablePropertiesSorter` and `DefaultPropertiesContextMatcher`.
- RichFaces `rf-*` classes are rendered by JS inside a jar; never provable dead.

## CI flakes

- `OpenLTableLogicTest.detectsErrorsInRulesTestedByTable` (STUDIO webstudio) — async compile race, `expected: <true> but was: <false>`; rerun once.
- `UserDatailsTab.test.tsx` "rejects an empty email…" — vitest wait timeout on a slow runner; rerun once.
- `Sonar analysis` › `Aggregate coverage`: `Unknown block type 3` reading `ITEST/server-core/target/jacoco.exec` — overlapping artifact download; rerun once.
- `SonarCloud Code Analysis` gate: deleting lines above an existing `S2259` finding re-attributes it to new code; deterministic, comment instead of rerun.

## Container facts

- `gh` is absent; use the GitHub MCP tools (`pull_request_read`, `update_pull_request`, `add_issue_comment`, `actions_list`, `get_job_logs`).
- `~/.m2` starts empty; the first `mvn clean install -Dquick -DnoPerf -T1C` downloads everything (network works through the proxy).
- Run Maven with `LANG=C.UTF-8` and `GIT_CONFIG_COUNT=1 GIT_CONFIG_KEY_0=commit.gpgsign GIT_CONFIG_VALUE_0=false`: no locale breaks one archive test, and JGit cannot ssh-sign.
- `~/.gitconfig` forces ssh commit signing and `Claude` identity; set `user.*` globally and check `git log -1 --pretty='%an|%cn'` after the first commit.
- `git push origin --delete <branch>` fails with "remote end hung up"; the proxy refuses branch deletes.

## Exhausted veins

- Unreferenced images, whole repo (703 files outside ITEST): every base name occurs as a whole word in a text file.
- Non-public methods, fields and types without bytecode reference, whole reactor (5140 classes): remaining hits are all alive.
- Spring XML beans, `DEMO/**` assets, Maven properties, enum constants, `Docs/**` pages: zero yield.
- Test workbooks by base name, whole repo outside ITEST.

## Human follow-ups

- Delete the merged remote branch `dead-code/uncalled-methods` (the sandbox cannot).
- Decide on the public-API items in Deferred findings.
- Fix the `JAXRSOpenLServiceEnhancerTest` fixture mix-up.
- `ComponentTypeArrayOpenClass.isAssignableFrom`/`isInstance` null guards (SonarCloud `S2259`, pre-existing on `main`).

## Run log

- 2026-09-09: #2088 merged; ledger created; image scan exhausted; CSS/JS/xhtml/properties scans started.
