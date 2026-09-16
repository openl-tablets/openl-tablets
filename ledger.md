# Dead-code sweep ledger — openl-tablets

## Resume point

- Every change type is re-derived on `origin/main` b48c862793 and the result is PR #2120 on `dead-code/full-resweep`,
  green and waiting on a human review. Maintain that PR first (section 4 of the prompt).
- Next sweep: `git rev-list --count b48c862793..origin/main`; 0 means nothing new to scan and the run is PR maintenance
  plus the ledger. Otherwise scan the new commits first, then rerun the detectors under Method rules.
- Every vein in Exhausted veins is covered at b48c862793 for the tree PR #2120 is rebased on; a new commit on main is
  the only source of new findings.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | in PR #2120; 27 lines |
| 2 | Never-read assignments, dead stores | done; 30 PMD hits, all generated grammar or FPs |
| 3 | Unused locals, private fields/methods/params | done; the 2 ProjectModel counters landed on main by a parallel run |
| 4 | Unused Maven dependency declarations | in PR #2120; 1 of 250 analyze hits |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | in PR #2120; aspectj only |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | in PR #2120; `[*.scss]` and 9 `@SuppressWarnings` |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 82 stems, 608 images, all alive |
| 8 | CSS rules and inline styles | done; DEMO main.css only, all selectors used |
| 9 | Legacy JS functions and pages | done; no legacy JS or pages exist |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | in PR #2120; 3 keys (17 + 1 landed on main in parallel) |
| 11 | TypeScript exports, types, components, imports | done; tsc --noUnusedLocals clean, 0 unimported exports |
| 12 | Test fixtures: workbooks, utility classes, stub members | in PR #2120; 1 workbook, 2 stubs |
| 13 | Package-private/protected members and unreferenced internal classes | in PR #2120; webstudio only |

## Open PR

- PR #2120, branch `dead-code/full-resweep`, head 058071e382, 7 commits, 41 files, -487/+2, on main b48c862793; every
  check was green on head 4e770f27f5 after one rerun of IT (studio-acl). On 058071e382 all checks green except
  IT (services-data): the Kafka container segfault below, commented at 22:21 UTC, failed jobs re-run once at 22:50 UTC.
  PR body rewritten for this head (commit 4 title, fixpoint members, totals, -Xlint verification).
- Commits: ec2e2d02bf commented-out code; c5120bb143 message keys; 64679e2f93 test workbook and stubs; 017f32d08d
  `.editorconfig` scss section plus 9 `@SuppressWarnings`; 34cc58f7a9 spring-security-core in security.standalone;
  f8a5ffa557 AspectJ managed versions; 058071e382 webstudio members and mapper type. No review thread is open.

## Merged PRs

- None since the reset.

## Module coverage

- All 85 reactor modules, studio-ui, Docs and DEMO scanned for every change type at b48c862793; only code merged after
  that can yield again.

## Deferred findings

- Public in published DEV jars, a human decides: types IBoundModuleNode, DependencyWrapperLogicToType, RuleInfo,
  RowParserElement, TableObjectDelegator, TableParser, EmptyCell, UndoableRemoveMergedColumnsAction, CellFont,
  RegionGridSelector, the 8 tbasic.runtime.operations classes, ResultNotFoundException, DefaultRuntimeContext,
  ValidationServiceClassException; members IConditionEvaluator.DECORATOR_CONDITION_PRIORITY, write-only
  Module.wildcardName and TablePropertyDefinition.securityFilter (Lombok setters), DecisionTableBuilder.methodName.
- Public in STUDIO/WSFrontend jars: HistoryLog, RepositoryException, ProjectGrouping, RandomUUID,
  ZonedDateTimeToDateConvertor, RuleServicePublisherMapper; ProjectVersion.getVersionComment and
  VersionInfo.getEmailCreatedBy with their fields; SimpleGroup.description (public setter and ctor parameter).
- ExpressionFactoryImpl.findExpression is compiled out by the constant `_getFromCache = false`: a cache toggle, not dead.
- MergeResult record: `status` component ignored by the compact constructor; removal changes a public record signature.
- `STUDIO/org.openl.rules.diff/doc/Diff Algorithm.xlsx`: unreferenced design material, not code.
- `org.eclipse.jetty:jetty-home` managed entry: in no dependency tree and declared by no pom; the DEMO scripts fetch
  Jetty by `jetty.version` themselves. Confirm nothing resolves it before dropping it.
- `org.openl.rules.jackson` in ruleservice.ws.common and `spring-security-config` in org.openl.security are unused
  where declared but are the transitive providers their consumers compile against undeclared; fixing that is an
  addition (hygiene PR), never a deletion here.
- Write-only or reflective PMD hits kept: GitRepository `result = null` in catch (empty catch otherwise), DebugChannel and
  DebugHookImpl publish-before-block stores, ServiceManagerImpl.deploy re-entrancy store.

## False-positive shapes

- A private or package member whose name appears in any string literal is reflective (`@MethodSource`, JAXB, OpenL
  datatype binding): filter bytecode-scan hits on Java string literals, non-Java text and workbook strings.
- Getter/setter hits must also be checked by property name: Jackson DTOs (RepositorySettings, `*Append.setTableType`,
  SupportedFeaturesModel) and OpenL datatype beans are bound by `basePath`, not `setBasePath`.
- A count-1 identifier index misses same-file callers only when built from the wrong working directory: verify every
  count-1 hit with a plain grep before acting (the git test constants looked unused and were used 2-9 times).
- JAXB private `beforeMarshal`/`afterUnmarshal` run reflectively; Spring MVC handlers have no Java caller; record
  component accessors and generic bridge overrides (`InputStats.getAvgX` erasing to Number) are alive.
- PMD UnusedAssignment blind spots: constructor early return (CellStyle), a value read back through a callback
  (DynamicPropertySource.settings), a field read by a getter (AProjectCreator), `key = null` before `System.gc()`.
- PMD UnusedLocalVariable: try-with-resources locals (`WebSocketAuthTest.stomp`), a counting for-each
  (`RulesUtils.getValues`), a cast hosted by an assignment before `fail()`.
- A `@SuppressWarnings` javac stays silent about is still alive when the element holds a raw cast, a raw `instanceof` or a
  raw type argument (`(List) x`, `Class<Enum>`): javac exempts those from `rawtypes`, IDEs do not. Only an element with no
  raw, unchecked or deprecated construct at all is dead. Keys javac does not know (`unused`, `resource`, `squid:*`,
  `NullableProblems`, Error Prone names) are IDE or Sonar keys and are judged by that tool, not by javac.
- Reflection fixtures asserted by name: epbds6830 BeanA.getAB, AOpenClassTest.getC, JavaOpenClassTest.gg, MyProp fields
  named in a binary .xls, YamlMapperFactoryTest transient fields, InterfaceTransformerTest.TestInterface.
- i18n keys reached by template: `browser.module.edit_${key}`, `editor_kind_${x}`, `range_${p}`, `view_${n}`,
  `browser.access.role_${r}`, `browser.compile.${state}`, `browser.${id}_confirm`, `browser.files.change.${t}`,
  `tests.${kind}`/`tests.no_${kind}`, `notifications.${kind}_deleted`, `debug.status.*`, `fill_preview.state.*`,
  `create_table_modal.types.*`/`.blocked.*`, `update_project_modal.*_hint`, `details_inherited_${level}`.
- Bundle conventions: ValidationMessages `openl.error.<status>.<code>.message` (code in Java), sql-errors keyed by
  vendor error code, `openl-default.properties` keys composed as `repo-<id>.` and `$ref` indirection.
- Resource stems alive by convention: Flyway `db/flyway/**`, `META-INF/openl/extension-*.xml`,
  `openl-db-repository-<code>.properties`, `rapi-doc/rapidoc-min.js` (named with its extension in swagger-ui.html).
- dependency:analyze FPs: inherited test harness, provided/CLASS-retention annotations and processors, runtime
  providers named from configuration, aggregators (swagger-parser), wars, and transitive providers of undeclared uses.
- A managed entry no pom declares is a transitive version pin: judge it by `dependency:tree -Dverbose -Pitest` over
  all modules, not by declaration. An `exclusion` is judged by resolving its parent alone in a scratch pom.

## Method rules

- Cold `~/.m2`: the first build is `LANG=C.UTF-8 mvn clean install -Dquick -DnoPerf -T2 -Daether.syncContext.named.time=600`
  online (27 min). Unset `gpg.format` and `commit.gpgsign` globally first or JGit tests die.
- Never edit the working tree while any Maven run is active, including `test-compile`-bound detector runs; never rebase
  during a build either, because a rebase checks out intermediate trees.
- PMD and dependency:analyze need reactor artifacts: run `mvn -o test-compile pmd:pmd dependency:analyze-only
  dependency:tree -Dverbose -Pitest -fae -pl '!STUDIO/studio-ui'`; a bare `pmd:pmd` fails at the first consumer of the
  never-installed rules.test. DEMO needs the network for its plugins; run it without `-o`.
- Add maven-pmd-plugin 3.28.0 under root `<build><plugins>` with `includeTests` and the ruleset at
  `${maven.multiModuleProjectDirectory}/.toDelete/pmd-dead-code.xml`; parse every `target/pmd.xml`; ignore
  `target/generated-sources`; restore the pom with `git checkout -- pom.xml`.
- Bytecode scan: ~180-line ASM program (asm 9.10 from `~/.m2`) over every `target/classes` and `target/test-classes`;
  record invocations, field access, method handles, invokedynamic args and `ldc` strings; drop annotated members,
  overrides (unknown third-party supertype counts as override), names in literals/non-Java text/workbooks; then grep each
  survivor. Public members are candidates only in the webstudio war and in test classes.
- Search documentation for a removed dependency case-insensitively (`grep -i`): `aspectj` missed the `AspectJ` row in
  technology-stack.md that CodeRabbit then flagged. Release-notes version tables are history and stay.
- Prove non-reference with `grep -rIwF <name>` over all tracked files plus `grep -raF` for binaries and `unzip -p` for
  workbooks; a `.xls` is searched as latin-1 and UTF-16 bytes.
- Removing members is a fixpoint: re-check fields, private helpers, constructor parameters and imports the removal
  orphaned; Spotless does not remove imports in a module `validate` here, so check them with a script. SonarCloud's
  "new issues" on the PR (`sonarcloud.io/api/issues/search?componentKeys=org.openl.rules:openl-tablets&pullRequest=N
  &sinceLeakPeriod=true`, no auth) list exactly the members a removal left unused: read it after every push.
- Stage every commit by explicit path (`git add -- <files>`, `git add -A -- <deleted>`); a `git rm` staged earlier rides
  into the next commit otherwise. Never `git diff --cached --stat A B` (invalid); use `git show --stat`.
- Frontend gate: `npx tsc --noEmit --noUnusedLocals` (clean at 737e6794be), `npx vitest run src/locales`; export scan by
  regex over `src/**/*.ts(x)` with `(?<![\w$])name(?![\w$])` boundaries.
- Whole-type scan: simple name absent from every other file → filter Spring/JUnit/JAXB annotations; a public type in a
  jar goes to Deferred, a pkg-private one is a candidate.
- Dead-suppression scan: blank every javac-key `@SuppressWarnings` in place (same byte length), add
  `-Xlint:deprecation,unchecked,rawtypes,serial,removal` and `-Xmaxwarns 100000` to the root compilerArgs, run
  `mvn test-compile -Pitest -fae -T2 -pl '!STUDIO/studio-ui'`, then match `[category]` warnings to the element's line range.
  The first warning of each compile is printed without the `[WARNING]` prefix: make the prefix optional when parsing.
- Before every push: list open `dead-code/*` PRs and re-fetch main; a parallel run of this routine once pushed to main.

## Keep-list

- OpenL datatype beans and rules interfaces in tests are bound from Excel by property or method name (IChildBean.getMyBean,
  Tutorial4Interface.getTheft_rating, Location setters, RulesUtilsTest.testFlatten, ComputeInterface stand-ins).
- Jackson-bound webstudio models keep every accessor: RepositorySettings, AWSS3RepositorySettings, GitRepositorySettings,
  `*Append`, SettingValueWrapper, SupportedFeaturesModel.
- Convention files: Flyway migrations, `META-INF/openl/extension-*.xml`, `openl-db-repository-*.properties`, static
  rapi-doc, site.webmanifest icons, `META-INF/services/**`.
- Config defaults in `openl-default.properties` are documented in Docs guides and composed at runtime; all 199 alive.
- Demo project workbooks under `STUDIO/org.openl.rules.demo/src/**` and `webstudio/test/rules/decisionTableIndexes/` are
  folder-loaded.
- `.gitignore` entries for IDE and OS artefacts stay even when no such file is tracked.
- Root exclusions on httpclient, azure-storage-blob, hibernate-validator, swagger-parser and webstudio's poi-ooxml-lite
  all still remove a transitive artifact (verified by scratch-pom resolution).

## CI flakes

- IT (studio-acl): `OracleRdbmsTest.upgrade` fails "Failed requests: expected 0 but was N" with `ORA-12516: Listener ... does
  not have a protocol handler for TCP ready` at `task_EPBDS-16253/030-read-access-revoked/1x0` while MySQL, PostgreSQL and
  SQL Server pass; hit two consecutive PR heads the same hour main passed it twice. Oracle Free container limit, not the
  diff; one `actions_run_trigger rerun_failed_jobs` on the run id cleared it; the container fix is a human follow-up.
- IT (services-data): `RunTracingITest.setUp` "Container startup failed for image apache/kafka-native:latest" after a
  `SegfaultHandler caught a segfault` inside the native image at `===> Launching ...`, 60 s wait for "Transitioning from
  RECOVERY to RUNNING" times out; the same image started in 0.5 s for RunKafkaSmokeITest a minute earlier in the same job.
  Floating `latest` tag; one `rerun_failed_jobs` is the retry.
- Tests (without ITEST): `ModuleWorkspace.test.tsx` two cases on `module-workspace-error` fail on the CI runner while the
  same tree passes all 2209 studio-ui tests locally; seen on main b48c862793.
- A job log is fetched with `get_job_logs` (tail 8000 lines lands in a file); find the failing requests with
  `test-resources/... - FAIL` and the cause with `ORA-|SQLException|expected: <`.

## Container facts

- No `gh` CLI: GitHub MCP tools only (pull_request_read, update_pull_request, add_issue_comment, actions_list
  list_workflow_jobs, actions_run_trigger rerun_failed_jobs with the workflow run id from a check's html_url).
- Container presets `gpg.format=ssh` and `commit.gpgsign=true` globally; `~/.gitconfig` user.* may be rewritten, so set
  `git config --local user.*` and pass the identity inline on every commit and rebase.
- Error Prone's Unused checks are not enabled in the build, so PMD and the bytecode scan are the Java detectors; javac
  options go into the root pom's compilerArgs, never on the `mvn` command line.
- 4 cores: `-T2` for the reactor; `npx tsc` runs fine beside Maven, vitest does not.
- `.toDelete/` is gitignored: keep the PMD ruleset and scratch poms there. `~/.m2` starts empty in every container.
- ITEST artifacts are never installed: run `pmd:pmd` there as `mvn -o test-compile pmd:pmd -f ITEST/pom.xml -Pitest`.
- A detached `setsid nohup script.sh` survives the tool timeout; wait on the process, not on the log's last line.

## Exhausted veins

- At b48c862793: commented-out code (all sources), PMD 5 rules over 85 modules and the 32 ITEST modules, ASM member
  scan over 5156 classes, whole-type scan, identifier count-1 scan, resources/images/workbooks by name, all message
  bundles and locales, config defaults, tsc/eslint-free export scan, dependency:analyze-only, managed entries and
  exclusions by tree, managed plugins by declaration, all 191 javac-key `@SuppressWarnings` by `-Xlint`, `.editorconfig`,
  `.gitignore`, Docs page graph, DEMO css.

## Human follow-ups

- ORA-12516 in IT (studio-acl) deserves a real fix in the Oracle container setup (process/session limit), and the
  `ModuleWorkspace.test.tsx` timing failure a source-level fix; both bite green PRs.
- Dependency hygiene (additions): 293 used-undeclared findings, notably spring-security-core in org.openl.security and
  org.openl.rules.jackson in ruleservice.ws.
- PMD `Parsing failed in ParseLock#doParse()` on `BranchedProjectIndexService$IndexState` in workspace: a PMD 7 type
  resolution bug, harmless to the report.
- Flyway migration `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` script; confirm Flyway applies it.
- ITEST pulls `apache/kafka-native:latest` (RunTracingITest, RunKafkaSmokeITest); the image segfaulted at start once on
  2026-09-16. Pinning the tag is test infrastructure outside the sweep.
- `RulesUtilsTest.testParseFormattedDouble` carries `@SuppressWarnings("deprecated")`, a key javac ignores, while both
  methods it calls are deprecated: the fix is the key `deprecation`, a rename this routine may not make.

## Run log

- 2026-09-16 g: ledger reset to zero; full re-sweep from `origin/main` 737e6794be; PR #2120, now 7 commits, -453 lines after
  a rebase onto main.
- 2026-09-16 h: main unchanged; IT (studio-acl) rerun green; 9 dead `@SuppressWarnings` and the Sonar-flagged WebStudio
  fixpoint folded into PR #2120 (-487 lines); ITEST PMD, managed plugins and javac-key suppressions exhausted. Run g's
  session then verified the new head (per-file `javac -Xlint`, webstudio tests), rewrote the PR body and re-ran the
  Kafka-container failure of IT (services-data) once.
