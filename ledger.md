# Dead-code sweep ledger — openl-tablets

## Resume point

- Main is still `1dc89c91e0`; the last delta sweep found nothing. Sweep the next delta from `1dc89c91e0`;
  0 commits means ledger upkeep plus PR #2135 maintenance only.
- The container-registration vein is worked out (see Exhausted veins); no vein is left open.
- A full re-sweep is not worth a run until main moves substantially; every vein under Exhausted veins is
  exhausted at `000e6d889f`.
- Before every push: list open `dead-code/*` PRs and re-fetch main; parallel runs of this routine share the branch.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | done; remaining hits are prose or parked calls with a rationale |
| 2 | Never-read assignments, dead stores | done; 14 PMD hits, all documented FPs |
| 3 | Unused locals, private fields/methods/params | done; 14 PMD hits, all documented FPs |
| 4 | Unused Maven dependency declarations | done; 530 analyze hits, all FPs |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | done |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 634 candidates, 0 unreferenced |
| 8 | CSS rules and inline styles | done |
| 9 | Legacy JS functions and pages | done; no legacy JS or pages exist |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done (PR #2129 merged); 1 key of 1,590 |
| 11 | TypeScript exports, types, components, imports | done; 1,094 exports, all referenced |
| 12 | Test fixtures: workbooks, utility classes, stub members | done |
| 13 | Package-private/protected members and unreferenced internal classes | done; 0 non-public dead types |

## Open PR

- #2135 on `dead-code/session-activation-callbacks`, head `7f40512465`, 1 commit, -54/+1, 6 files.
- Commit: remove the session passivation path the servlet container never invokes — SessionListener's
  HttpSessionActivationListener half, the two RulesUserSession methods, UserWorkspace.passivate with both
  implementations, and the two Docs mentions.
- Maintainer accepted the bytecode evidence and asked to drop UserWorkspace.activate() and passivate(). passivate()
  is done; activate() was kept and explained — RulesUserSession.getUserWorkspace() calls it. Awaiting his answer.
- CodeRabbit on the previous head: no actionable comments. Its Docstring Coverage warning was declined by comment;
  do not re-litigate it.

## Merged PRs

- #2120 merged 2026-09-17: 7 commits, -487/+2, across commented-out code, locale keys, a test workbook, dead
  `@SuppressWarnings`, spring-security-core in security.standalone, AspectJ managed versions and webstudio members.
- #2129 merged 2026-09-17: 1 commit, -1 line, the dead `users:edit_modal.cancel` locale key.
- #2134 merged 2026-09-18: 1 commit, -87 lines, SessionTimeoutFilter and its web.xml registration. A removal
  proven by unreachable behaviour rather than by non-reference is accepted on that evidence alone.
- The maintainer merges a small, well-evidenced sweep PR within the hour, before CI finishes; do not wait on green.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type at `000e6d889f`; nothing left open.

## Deferred findings

- Public in published DEV jars, a human decides: types IBoundModuleNode, DependencyWrapperLogicToType, RuleInfo,
  RowParserElement, TableObjectDelegator, TableParser, EmptyCell, UndoableRemoveMergedColumnsAction, CellFont,
  RegionGridSelector, the 8 tbasic.runtime.operations classes, ResultNotFoundException, DefaultRuntimeContext,
  ValidationServiceClassException; members IConditionEvaluator.DECORATOR_CONDITION_PRIORITY, write-only
  Module.wildcardName and TablePropertyDefinition.securityFilter (Lombok setters), DecisionTableBuilder.methodName.
- Public in STUDIO/WSFrontend jars: HistoryLog, RepositoryException, ProjectGrouping, RandomUUID,
  ZonedDateTimeToDateConvertor, RuleServicePublisherMapper, AppPropertiesServlet, CXFServlet;
  ProjectVersion.getVersionComment and VersionInfo.getEmailCreatedBy with their fields; SimpleGroup.description.
- 564 further public members are unreferenced in bytecode; all are published API and stay.
- ExpressionFactoryImpl.findExpression is compiled out by the constant `_getFromCache = false`: a cache toggle.
- MergeResult record: `status` component ignored by the compact constructor; removal changes a public record signature.
- `STUDIO/org.openl.rules.diff/doc/Diff Algorithm.xlsx`: unreferenced design material, not code.
- `org.eclipse.jetty:jetty-home` managed entry: in no dependency tree and declared by no pom; the DEMO scripts fetch
  Jetty by `jetty.version` themselves. Confirm nothing resolves it before dropping it.
- `org.openl.rules.jackson` in ruleservice.ws.common and `spring-security-config` in org.openl.security are unused
  where declared but are the transitive providers their consumers compile against undeclared; fixing that is an
  addition (hygiene PR), never a deletion here.

## False-positive shapes

- A detector that picks text files by an extension allowlist silently drops whole formats: `.webmanifest` was
  missing and reported 18 live resources dead. Select text by "no NUL byte in the first 8 KB" instead — that took
  the corpus from 7,089 to 13,790 files and the 18 findings to zero.
- javac inlines `static final` primitive and String constants, so a bytecode scan never sees a read and reports
  every such field dead. Judge constants by source text, never by bytecode.
- Lombok-generated accessors exist in bytecode but not in source: require the member name to appear in its own
  `.java` file before treating a bytecode hit as deletable.
- i18next resolves `t(key, {count})` to `key_one`/`key_other`, which no literal names: treat a plural suffix pair
  whose base is used as alive. Template keys likewise — `status_${s}`, `browser.access.role_${r}`,
  `browser.module.edit_${key}`, `editor_kind_${x}`, `range_${p}`, `view_${n}`, `browser.compile.${state}`,
  `browser.${id}_confirm`, `browser.files.change.${t}`, `tests.${kind}`, `notifications.${kind}_deleted`,
  `debug.status.*`, `fill_preview.state.*`, `create_table_modal.types.*`, `update_project_modal.*_hint`,
  `details_inherited_${level}`. Check the union type feeding the template: it names exactly the live keys.
- An exported TS type used only inside its own file looks unimported; that makes the `export` redundant, not the
  type dead. Count occurrences including the defining file.
- A private or package member whose name appears in any string literal is reflective (`@MethodSource`, JAXB, OpenL
  datatype binding): filter bytecode hits on Java string literals, non-Java text and workbook strings.
- Getter/setter hits must also be checked by property name: Jackson DTOs (RepositorySettings, `*Append.setTableType`,
  SupportedFeaturesModel) and OpenL datatype beans are bound by `basePath`, not `setBasePath`.
- JAXB private `beforeMarshal`/`afterUnmarshal` run reflectively; Spring MVC handlers have no Java caller; record
  component accessors and generic bridge overrides (`InputStats.getAvgX` erasing to Number) are alive.
- PMD UnusedAssignment blind spots: constructor early return (CellStyle), a value read back through a callback
  (DynamicPropertySource.settings), a field read by a getter (AProjectCreator), `key = null` before `System.gc()`,
  publish-before-block stores (DebugChannel, DebugHookImpl), a re-entrancy store (ServiceManagerImpl.deploy),
  `result = null` in an otherwise empty catch (GitRepository).
- PMD UnusedLocalVariable: try-with-resources locals held only for their lifecycle — Mockito `mockConstruction`
  scopes (ExcelFilesProjectCreatorTest), a Spring `AnnotationConfigApplicationContext`
  (ExtensionsConfigurationTest), `WebSocketAuthTest.stomp` — plus a counting for-each (`RulesUtils.getValues`) and
  a cast hosted by an assignment before `fail()`.
- A `@SuppressWarnings` javac stays silent about is still alive when the element holds a raw cast, a raw
  `instanceof` or a raw type argument. Keys javac does not know (`unused`, `resource`, `squid:*`,
  `NullableProblems`, Error Prone names) are IDE or Sonar keys, judged by that tool.
- Reflection fixtures asserted by name: epbds6830 BeanA.getAB, AOpenClassTest.getC, JavaOpenClassTest.gg, MyProp
  fields named in a binary .xls, YamlMapperFactoryTest transient fields, InterfaceTransformerTest.TestInterface.
- Bundle conventions: ValidationMessages `openl.error.<status>.<code>.message` (code composed in Java), sql-errors
  keyed by vendor error code, `openl-default.properties` keys composed as `repo-<id>.` and `$ref` indirection.
- Resource stems alive by convention: Flyway `db/flyway/**`, `META-INF/openl/extension-*.xml`,
  `openl-db-repository-<code>.properties`, `rapi-doc/rapidoc-min.js`, `site.webmanifest` icons.
- dependency:analyze FPs, all confirmed again at `000e6d889f`: a module that declares no `<dependencies>` of its own
  still gets findings from its parent (storelogdata.db.annotation and kafka-clients); inherited test harness;
  provided/CLASS-retention annotations and processors; runtime providers named from configuration (all five in
  security.standalone are instantiated by name in `security-hibernate-beans.xml`); aggregators (swagger-parser);
  wars and jdbc drivers an ITEST needs only to boot a server; transitive providers of undeclared uses.
- A managed entry no pom declares is a transitive version pin: judge it by `dependency:tree -Dverbose -Pitest` over
  all modules, not by declaration. An `exclusion` is judged by resolving its parent alone in a scratch pom.

## Method rules

- Build the whole repo once per run: `LANG=C.UTF-8 mvn clean install -Dquick -DnoPerf -T2
  -Daether.syncContext.named.time=600` online, 23 min from a cold `~/.m2`. Unset `gpg.format` and `commit.gpgsign`
  globally first or JGit tests die.
- Build one identifier-frequency index over the whole tree once (regex `[A-Za-z_$][\w$]*` per file, into a
  Counter) and answer every "is this name used" question from it: 2 seconds, versus hours of per-name scanning.
  A name whose total count equals its declaration count is unreferenced.
- Never edit the working tree while any Maven run is active, including `test-compile`-bound detector runs; never
  rebase during a build either, because a rebase checks out intermediate trees.
- PMD needs reactor artifacts and the plugin is not in a cold `~/.m2`, so run it online with the fully qualified
  goal: `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only -Pitest
  -fae -T2 -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`. A `pmd:` prefix fails to resolve offline.
- Add maven-pmd-plugin 3.28.0 under root `<build><plugins>` with `includeTests` and the ruleset at
  `${maven.multiModuleProjectDirectory}/.toDelete/pmd-dead-code.xml`; parse every `target/pmd.xml`; ignore
  `target/generated-sources`; restore the pom with `git checkout -- pom.xml`.
- Bytecode scan: ~180-line ASM program (asm 9.10.1 from `~/.m2`) over every `target/classes` and
  `target/test-classes`; record invocations, field access, method handles, invokedynamic args and `ldc` strings;
  drop annotated members, overrides (unknown third-party supertype counts as override), names in literals. Then
  apply the constant-inlining and Lombok filters above — without them it reports 1,568 members, with them 0.
- `pkill -f <script>.py` from a Bash tool call kills the calling shell too, because the pattern matches the shell's
  own command line. Anchor it (`pkill -f 'name[.]py'`).
- A class named only by a container registration (`web.xml` filter/servlet/listener, `@WebFilter`) is NOT proven
  alive by that reference: judge it by whether its behaviour is still reachable. SessionTimeoutFilter was mapped to
  `/*` yet could never act. Ask what removed the consumers (here: zero `.xhtml` left after the React migration).
- A `<listener>` registration serves only the interfaces the container sorts it into. Verified in bytecode on
  Jetty 12.1.13 and Tomcat 10.1.55: `SessionHandler.addEventListener` keeps only attribute, session and id
  listener lists (no activation list), while `onSessionPassivation`/`StandardSession.passivate` iterate session
  ATTRIBUTES and call `HttpSessionActivationListener` only on those. So a registered-but-unbound activation
  listener never fires. Prove such claims by unzipping the container jar and reading `javap -c`, not from the spec.
- A servlet guard pairing `isRequestedSessionIdValid()` with `getSession(false) == null` is unsatisfiable by the
  servlet contract: a valid requested id always yields that session. Read filter guards for contradictions.
- CodeRabbit's `Docstring Coverage` pre-merge check fails every deletion-only PR: it scores the functions inside
  the touched hunks, which on a sweep PR are the removed ones, so the metric is unreachable without adding JavaDoc
  to untouched methods. Decline it by comment citing `git diff -U0`; never widen a sweep PR to satisfy it.
- The container resets `git config --global user.*` to Claude mid-session, so an amend or a later commit silently
  gets the wrong committer even though the first commit was right. Re-assert the identity and re-check
  `git log -1 --pretty='%an <%ae> | %cn <%ce>'` after EVERY commit, not just the first.
- Public API deferred under rail 8.2 is worth naming explicitly in the PR body: the maintainer approved removing
  UserWorkspace.passivate() straight off that 'Deliberately kept' line. Deferring is not dropping.
- Search documentation for a removed dependency case-insensitively (`grep -i`).
- Prove non-reference with `grep -rIwF <name>` over all tracked files plus `grep -raF` for binaries and `unzip -p`
  for workbooks; a `.xls` is searched as latin-1 and UTF-16 bytes. Use `git ls-files`, never a raw `grep -r`:
  untracked build output (`STUDIO/studio-ui/dist/`) otherwise answers every query.
- Removing members is a fixpoint: re-check fields, private helpers, constructor parameters and imports the removal
  orphaned. SonarCloud's "new issues" on the PR (`sonarcloud.io/api/issues/search?componentKeys=
  org.openl.rules:openl-tablets&pullRequest=N&sinceLeakPeriod=true`, no auth) list exactly those: read it after
  every push.
- Stage every commit by explicit path (`git add -- <files>`); a `git rm` staged earlier rides into the next commit
  otherwise. Never `git diff --cached --stat A B` (invalid); use `git show --stat`.
- Frontend gate: `npx tsc --noEmit --noUnusedLocals`, `npx vitest run <area>`. Both need `node_modules`, which the
  reactor build populates; run them after it, never beside it.

## Keep-list

- OpenL datatype beans and rules interfaces in tests are bound from Excel by property or method name
  (IChildBean.getMyBean, Tutorial4Interface.getTheft_rating, Location setters, RulesUtilsTest.testFlatten).
- Jackson-bound webstudio models keep every accessor: RepositorySettings, AWSS3RepositorySettings,
  GitRepositorySettings, `*Append`, SettingValueWrapper, SupportedFeaturesModel.
- Convention files: Flyway migrations, `META-INF/openl/extension-*.xml`, `openl-db-repository-*.properties`, static
  rapi-doc, site.webmanifest icons, `META-INF/services/**`, ITEST `application-*.properties` (Spring profiles).
- Config defaults in `openl-default.properties` are documented in Docs guides and composed at runtime; all alive.
- Demo project workbooks under `STUDIO/org.openl.rules.demo/src/**` and
  `webstudio/test/rules/decisionTableIndexes/` are folder-loaded.
- `.gitignore` entries for IDE and OS artefacts stay even when no such file is tracked.
- Root exclusions on httpclient, azure-storage-blob, hibernate-validator, swagger-parser and webstudio's
  poi-ooxml-lite all still remove a transitive artifact (verified by scratch-pom resolution).
- A parked call that carries its own rationale comment is a documented decision, not dead code:
  `ResultExport.validateMergedRegions` (EPBDS-7848), `trackAllColumnsForAutoSizing`, the `intern()` TODO in
  RuleRowHelper, the bulk OpenAPI rewrite block in ITEST `HttpClient`.

## CI flakes

- Both flakes below passed first try on `49ad0b4388`; they are intermittent, not constant.
- IT (studio-acl): `OracleRdbmsTest.upgrade` fails "Failed requests: expected 0 but was N" with `ORA-12516` while
  the other vendors pass. Oracle Free container limit, not the diff; one rerun clears it.
- IT (services-data): `RunTracingITest.setUp` / `RunStoreLogDataITest.setUp` fail on `apache/kafka-native:latest`
  with a segfault at image start or "Text file busy"; the same image starts fine for an earlier suite in the same
  job. `latest` equals `4.3.1`, so pinning changes nothing. One rerun is the retry, then a maintainer by comment.
- Tests (without ITEST): `ModuleWorkspace.test.tsx` two cases on `module-workspace-error` fail on the CI runner
  while the same tree passes all studio-ui tests locally.
- A job log is fetched with `get_job_logs` (tail 8000 lines lands in a file); find the failing requests with
  `test-resources/... - FAIL` and the cause with `ORA-|SQLException|expected: <`.
- The `Sonar analysis` job is skipped when any job of the run fails, so a red flake also hides Sonar's verdict on
  that head; `sonarcloud.io/api/project_pull_requests/list?project=org.openl.rules:openl-tablets` names the SHA.

## Container facts

- No `gh` CLI: GitHub MCP tools only (pull_request_read, update_pull_request, add_issue_comment, actions_list
  list_workflow_jobs, actions_run_trigger rerun_failed_jobs with the workflow run id from a check's html_url).
- Container presets `gpg.format=ssh` and `commit.gpgsign=true` globally but sets no `GIT_AUTHOR_*`/`GIT_COMMITTER_*`
  environment variables, so a plain `git config --global user.*` is enough; verify once after the first commit.
- Error Prone's Unused checks are not enabled in the build, so PMD and the bytecode scan are the Java detectors;
  javac options go into the root pom's compilerArgs, never on the `mvn` command line.
- 4 cores, 15 GB RAM: `-T2` for the reactor. `npx tsc` runs fine beside Maven, vitest does not.
- `.toDelete/` is gitignored: keep the PMD ruleset and scratch poms there. `~/.m2` starts empty in every container.
- A detached `setsid nohup script.sh` survives the tool timeout; wait on the process, not on the log's last line.
- Edit the ledger through `git worktree add` on `origin/dead-code/ledger`, never by switching the sweep branch.

## Exhausted veins

- Container registrations audited for behavioural deadness at `1dc89c91e0`: webstudio web.xml (4 filters,
  3 listeners) and all 8 `@WebFilter`/`@WebServlet` classes in webstudio and ruleservice.ws. One finding
  (PR #2135); everything else is reachable. Re-run only when a registration is added or a consumer removed.
- At `000e6d889f`: commented-out code (all sources), PMD 5 rules over 86 modules and ITEST, ASM member scan over
  5,150 classes in 106 output dirs, whole-type scan over 4,021 Java files, identifier count-1 scan, resources and
  images by name and stem over text and binaries, all message bundles and locales (1,590 studio-ui keys plus the
  Java bundles), config defaults, TS export scan (1,094 names), dependency:analyze-only over the whole reactor,
  managed entries and exclusions by tree, managed plugins, `@SuppressWarnings` by `-Xlint`, `.editorconfig`,
  `.gitignore`, Docs page graph, DEMO css, and every studio-ui `package.json` dependency.

## Human follow-ups

- `site.webmanifest` in `WSFrontend/org.openl.rules.ruleservice.ws/resources/static/` and
  `STUDIO/studio-ui/public/icons/` names the 512x512 icon `android-chrome-512x512.pngs` (trailing `s`), so that
  icon resolves in neither module. The DEMO copy is correct. A typo to fix, not dead code — raised on PR #2129,
  still unfixed on main; re-raise it if a maintainer has not acted.
- `CorsFilter` is registered twice: `@WebFilter("/*")` (default name = the FQCN) and web.xml's `CorsFilter`. The
  names differ, so both registrations apply and the filter runs twice per request, adding each `Access-Control-*`
  header twice; browsers reject a duplicated `Access-Control-Allow-Origin`. Latent while `cors.allowed.origins` is
  unset. De-duplicating changes behaviour, so it is a fix, not a deletion. Raised on PR #2135.
- Request to swap `de.qaware.xff.filter.ForwardedHeaderFilter` (artifact `org.openl:x-forwarded-filter:2.0`) for
  Spring's `org.springframework.web.filter.ForwardedHeaderFilter` is BLOCKED and was not done: the root pom
  documents the opposite decision ("Neither Spring filter, nor CXF filter works correctly within the same reverse
  proxy"), and both call sites set `xForwardedPrefixStrategy=PREPEND` while Spring's filter exposes only
  `setRemoveOnly`/`setRelativeRedirects` and always REPLACES the context path with `X-Forwarded-Prefix`. Swapping
  changes proxy behaviour. Needs a maintainer decision; also touches WSFrontend RuleServicesFilter, not just web.xml.
- Workspace passivation never ran and is now removed. If it is wanted back, `RulesUserSession` must implement
  `HttpSessionActivationListener` itself — it IS a session attribute (`WebStudioUtils.registerRulesUserSession`),
  whereas `SessionListener`, which forwarded to it, was only registered. An addition, not this routine's work.
- ORA-12516 in IT (studio-acl) deserves a real fix in the Oracle container setup (process/session limit), and the
  `ModuleWorkspace.test.tsx` timing failure a source-level fix; both bite green PRs.
- Dependency hygiene (additions): ~293 used-undeclared findings, notably spring-security-core in org.openl.security
  and org.openl.rules.jackson in ruleservice.ws.
- PMD `Parsing failed in ParseLock#doParse()` on `BranchedProjectIndexService$IndexState`: a PMD 7 type resolution
  bug, harmless to the report.
- Flyway migration `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` script; confirm Flyway applies it.
- ITEST pulls `apache/kafka-native:latest`; the robust options are the JVM image `apache/kafka:4.3.1` or a
  `KafkaContainer` startup retry in the three suites. Test infrastructure outside the sweep.
- `RulesUtilsTest.testParseFormattedDouble` carries `@SuppressWarnings("deprecated")`, a key javac ignores, while
  both methods it calls are deprecated: the fix is the key `deprecation`, a rename this routine may not make.

## Run log

- 2026-09-17 b: full re-sweep from zero at main `000e6d889f` on user instruction. Two detector bugs found and fixed
  (extension allowlist, constant inlining); one dead locale key removed in PR #2129; a broken webmanifest icon
  raised for a maintainer.
- 2026-09-18: delta sweep of 7 commits found nothing; on user instruction removed SessionTimeoutFilter in PR #2134,
  merged the same hour. A second user request (swap the X-Forwarded filter for Spring's) was blocked on a
  documented contrary decision and recorded under Human follow-ups.
- 2026-09-18 b: main unmoved, so the run worked the container-registration vein: SessionListener's activation
  callbacks removed in PR #2135, the CorsFilter double registration raised for a maintainer.
