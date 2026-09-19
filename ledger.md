# Dead-code sweep ledger — openl-tablets

## Resume point

- Main is `abb3d5ae81`, unmoved since the last full sweep. Four consecutive exhaustive runs found zero deletable
  items, so there is no commit, no branch and no open PR.
- Sweep only the delta from `abb3d5ae81`. Zero new commits on main means ledger upkeep only; a full re-sweep costs
  ~95 min (25 min build + 30 min PMD + detectors) and earns nothing until main moves substantially.
- Before every push: list open `dead-code/*` PRs and re-fetch main; parallel runs of this routine share the branch.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | done; 7 blocks, all prose or parked calls with a rationale |
| 2 | Never-read assignments, dead stores | done; 14 PMD hits, all documented FPs |
| 3 | Unused locals, private fields/methods/params | done; 14 PMD hits, all documented FPs |
| 4 | Unused Maven dependency declarations | done; 569 analyze hits, all FPs |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | done; 6 hits, all plugin-read flags |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done; constant guards are named arguments |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 220 candidates, 0 unreferenced |
| 8 | CSS rules and inline styles | done; 1 file, 4 selectors, all used |
| 9 | Legacy JS functions and pages | done; 0 `.xhtml` remain, only keep-listed vendor JS |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done; 1,583 keys, 113 flagged, all template-resolved |
| 11 | TypeScript exports, types, components, imports | done; 1,151 exports, 0 dead |
| 12 | Test fixtures: workbooks, utility classes, stub members | done |
| 13 | Package-private/protected members and unreferenced internal classes | done; 0 non-public dead members |

## Open PR

- none

## Merged PRs

- #2120 (-487), #2129 (-1), #2134 (-87, SessionTimeoutFilter), #2135 (-54, the session passivation path).
- A removal proven by unreachable behaviour rather than by non-reference is accepted on that evidence alone.
  `activate()` stays — `RulesUserSession.getUserWorkspace()` calls it.
- The maintainer merges a small, well-evidenced sweep PR within the hour, before CI finishes; do not wait on green.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type at `abb3d5ae81`; nothing open.

## Deferred findings

- ~599 public members and ~35 public types across the DEV, STUDIO and WSFrontend jars are unreferenced in
  bytecode. All are published API and stay under rail 8.2, so no run acts on them; the ASM scan below re-derives
  the names in one pass whenever a maintainer wants the list. Write-only Lombok setters are part of this class.
- ExpressionFactoryImpl `_getFromCache` and `_putInCache` are both `= false`, compiling out findExpression and the
  cache writes: a cache toggle pair, not dead code.
- MergeResult record: `status` component ignored by the compact constructor; removal changes a public record signature.
- `STUDIO/org.openl.rules.diff/doc/Diff Algorithm.xlsx`: unreferenced design material, not code.
- `org.eclipse.jetty:jetty-home` managed entry: in no dependency tree and declared by no pom; the DEMO scripts
  fetch Jetty by `jetty.version` themselves. Confirm nothing resolves it before dropping it.
- `org.openl.rules.jackson` in ruleservice.ws.common and `spring-security-config` in org.openl.security are unused
  where declared but are the transitive providers their consumers compile against undeclared; fixing that is an
  addition (hygiene PR), never a deletion here.

## False-positive shapes

- A detector that picks text files by an extension allowlist silently drops whole formats: `.webmanifest` was
  missing and reported 18 live resources dead. Select text by "no NUL byte in the first 8 KB" instead — that took
  the corpus from 7,089 to 13,856 files and the 18 findings to zero.
- javac inlines `static final` primitive and String constants, so a bytecode scan never sees a read and reports
  every such field dead: 1,097 of 1,515 raw hits. Judge constants by source text, never by bytecode.
- Lombok-generated accessors exist in bytecode but not in source: require the member name to appear in its own
  `.java` file before treating a bytecode hit as deletable.
- i18next resolves `t(key, {count})` to `key_one`/`key_other`, which no literal names: treat a plural suffix pair
  whose base is used as alive. Keys built by interpolation are likewise alive — `status_${s}`, `role.${r}`,
  `tests.${kind}`, `debug.status.*` and some 20 more of that shape. Do not enumerate them by hand: read the union
  type feeding the template, which names exactly the live keys. Search the `no_`/prefix form too, or a bare tail
  search misses `tests.no_${kind}`.
- An exported TS type used only inside its own file looks unimported; that makes the `export` redundant, not the
  type dead. Same for a `const`/function used only in its own file. Removing `export` is a rename, not a deletion.
- A leading positional callback parameter reported by `tsc --noUnusedParameters` or PMD `UnusedFormalParameter` is
  never removable: dropping it shifts the parameter that IS used (`Array.from(…, (unused, column) => …)`,
  a mock's `(url, options)`).
- A pom `<properties>` entry with no `${...}` dereference may still be read by a plugin by name:
  `lombok.delombok.skip`, `archetype.test.skip`, `invoker.skip`. Check the plugin before calling it dead.
- A private or package member whose name appears in any string literal is reflective (`@MethodSource`, JAXB, OpenL
  datatype binding): filter bytecode hits on Java string literals, non-Java text and workbook strings.
- Getter/setter hits must also be checked by property name: Jackson DTOs (RepositorySettings, `*Append.setTableType`,
  SupportedFeaturesModel) and OpenL datatype beans are bound by `basePath`, not `setBasePath`.
- JAXB private `beforeMarshal`/`afterUnmarshal` run reflectively; Spring MVC handlers have no Java caller; record
  component accessors and generic bridge overrides (`InputStats.getAvgX` erasing to Number) are alive.
- A top-level type whose simple name occurs only in its own file is still alive when a framework names it: JUnit
  by file pattern, Spring by classpath scan, `@Mojo` by the plugin descriptor (the 4 openl-maven-plugin mojos).
- PMD UnusedAssignment blind spots: constructor early return (CellStyle), a value read back through a callback
  (DynamicPropertySource.settings), a field read by a getter (AProjectCreator), `key = null` before `System.gc()`,
  publish-before-block stores (DebugChannel, DebugHookImpl), a re-entrancy store (ServiceManagerImpl.deploy) and
  `result = null` in an empty catch (GitRepository).
- PMD UnusedLocalVariable: try-with-resources locals held only for their lifecycle — Mockito `mockConstruction`
  scopes (ExcelFilesProjectCreatorTest), a Spring context (ExtensionsConfigurationTest), `WebSocketAuthTest.stomp`
  — plus a counting for-each (`RulesUtils.getValues`) and a cast hosted by an assignment before `fail()`.
- A `@SuppressWarnings` javac stays silent about is still alive when the element holds a raw cast, a raw
  `instanceof` or a raw type argument. Keys javac does not know (`unused`, `resource`, `squid:*`,
  `NullableProblems`, Error Prone names) are IDE or Sonar keys, judged by that tool.
- Reflection fixtures asserted by name: epbds6830 BeanA.getAB, AOpenClassTest.getC, JavaOpenClassTest.gg, MyProp
  fields named in a binary .xls, YamlMapperFactoryTest transient fields, InterfaceTransformerTest.TestInterface,
  JsonUtilsTest key/value/field.
- Bundle conventions: ValidationMessages `openl.error.<status>.<code>.message` (code composed in Java), sql-errors
  keyed by vendor error code, `openl-default.properties` keys composed as `repo-<id>.` and `$ref` indirection.
- Resource stems alive by convention: Flyway `db/flyway/**`, `META-INF/openl/extension-*.xml`,
  `openl-db-repository-<code>.properties`, `rapi-doc/rapidoc-min.js`, `site.webmanifest` icons,
  `META-INF/maven/archetype-metadata.xml`, `compose.override.example.yaml`.
- dependency:analyze FPs, confirmed a third time at `abb3d5ae81`: a module declaring no `<dependencies>` still
  gets findings from its parent; inherited test harness (junit-jupiter, junit-pioneer, mockito-junit-jupiter,
  log4j-to-slf4j are 296 of the 569); provided annotations and processors (jspecify, lombok); runtime providers
  named from configuration (the five in security.standalone via `security-hibernate-beans.xml`; maven-scm;
  cxf-rt-features-logging; the Azure SDK's jackson dataformats and reactor-core); aggregators (swagger-parser, Web
  Services, the Maven Plugin); wars and jdbc drivers an ITEST needs only to boot a server.
- A managed entry no pom declares is a transitive version pin: judge it by `dependency:tree -Dverbose -Pitest` over
  all modules, not by declaration. An `exclusion` is judged by resolving its parent alone in a scratch pom.
- A `#{...}` occurrence is not JSF EL: in this repo every one is a Spring property placeholder, SpEL, or a
  TypeScript template literal. Do not read it as a surviving page binding.

## Method rules

- Build the whole repo once per run: `LANG=C.UTF-8 mvn clean install -Dquick -DnoPerf -T2
  -Daether.syncContext.named.time=600`. 25 min with a warm `~/.m2`, 23 min cold. Unset `gpg.format` and
  `commit.gpgsign` globally first or JGit tests die.
- Build one identifier-frequency index over the whole tree once (regex `[A-Za-z_$][\w$]*` per file, into a
  Counter) and answer every "is this name used" question from it: 2 seconds, versus hours of per-name scanning.
  A name whose total count equals its declaration count is unreferenced.
- Never name a scratch script after a stdlib module — `.toDelete/types.py` breaks every `import` in the run.
- Never edit the working tree while any Maven run is active, including `test-compile`-bound detector runs; never
  rebase during a build either, because a rebase checks out intermediate trees.
- PMD needs reactor artifacts and the plugin is not in a cold `~/.m2`, so run it online with the fully qualified
  goal: `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only -Pitest
  -fae -T2 -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`. A `pmd:` prefix fails to resolve offline. 30 min, 75 reports.
- Add maven-pmd-plugin 3.28.0 under root `<build><plugins>` with `includeTests` and the ruleset at
  `${maven.multiModuleProjectDirectory}/.toDelete/pmd-dead-code.xml`; parse every `target/pmd.xml`; ignore
  `target/generated-sources`; restore the pom with `git checkout -- pom.xml`.
- Bytecode scan: ~120-line ASM program (asm 9.10.1 from `~/.m2`) over every `target/classes` and
  `target/test-classes`; record invocations, field access, method handles, invokedynamic args and `ldc` strings;
  drop annotated members, overrides (unknown third-party supertype counts as override), names in literals. Then
  apply the constant-inlining and Lombok filters above — 5,162 classes, 53,375 members, 1,515 raw hits, 0 survivors.
- Chain the install and the PMD run in one detached `setsid nohup` script that touches a DONE file; poll that
  file, never the log's last line. `pkill -f <script>.py` kills the calling shell too, since the pattern matches
  the shell's own command line — anchor it (`pkill -f 'name[.]py'`).
- A class named only by a container registration (`web.xml` filter/servlet/listener, `@WebFilter`) is NOT proven
  alive by that reference: judge it by whether its behaviour is still reachable. SessionTimeoutFilter was mapped to
  `/*` yet could never act. Ask what removed the consumers (here: zero `.xhtml` left after the React migration).
- A `<listener>` registration serves only the interfaces the container sorts it into, so a registered-but-unbound
  listener never fires. Verified by `javap -c` on Jetty 12.1.13 and Tomcat 10.1.55: neither keeps an activation
  list, and passivation iterates session ATTRIBUTES. Prove such claims from the container jar, never the spec.
- A servlet guard pairing `isRequestedSessionIdValid()` with `getSession(false) == null` is unsatisfiable by the
  servlet contract: a valid requested id always yields that session. Read filter guards for contradictions.
- A `private static final boolean` is only a dead-branch lead when it gates an `if`; the ones here are named
  arguments passed to a setter (EnabledAclConfiguration.ALC_CLASS_ID_SUPPORTED) or public API constants.
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
- A docs tree reached only through a directory link (`README.MD` → `examples/` → `index.md` → subfolder) is
  alive: count a link to the PARENT directory, not just to the file, before calling a Docs page unreferenced.
- Removing members is a fixpoint: re-check fields, private helpers, constructor parameters and imports the removal
  orphaned. SonarCloud's "new issues" list exactly those, so read it after every push (no auth):
  `sonarcloud.io/api/issues/search?componentKeys=org.openl.rules:openl-tablets&pullRequest=N&sinceLeakPeriod=true`.
- Stage every commit by explicit path (`git add -- <files>`); a `git rm` staged earlier rides into the next commit
  otherwise. Never `git diff --cached --stat A B` (invalid); use `git show --stat`.
- Frontend gate: `npx tsc --noEmit --noUnusedLocals --noUnusedParameters`, `npx vitest run <area>`. Both need
  `node_modules`, which the reactor build populates; run them after it, never beside it.

## Keep-list

- OpenL datatype beans and rules interfaces in tests are bound from Excel by property or method name
  (IChildBean.getMyBean, Tutorial4Interface.getTheft_rating, Location setters, RulesUtilsTest.testFlatten).
- Jackson-bound webstudio models keep every accessor: RepositorySettings, AWSS3RepositorySettings,
  GitRepositorySettings, `*Append`, SettingValueWrapper, SupportedFeaturesModel.
- Convention files: Flyway migrations, `META-INF/openl/extension-*.xml`, `openl-db-repository-*.properties`, static
  rapi-doc, site.webmanifest icons, `META-INF/services/**`, ITEST `application-*.properties` (Spring profiles),
  archetype `archetype-metadata.xml`, `compose.override.example.yaml`.
- Config defaults in `openl-default.properties` are documented in Docs guides and composed at runtime; all alive.
- Demo project workbooks under `STUDIO/org.openl.rules.demo/src/**` and
  `webstudio/test/rules/decisionTableIndexes/` are folder-loaded.
- `.gitignore` entries for IDE and OS artefacts stay even when no such file is tracked.
- Root exclusions on httpclient, azure-storage-blob, hibernate-validator, swagger-parser and webstudio's
  poi-ooxml-lite all still remove a transitive artifact (verified by scratch-pom resolution).
- A parked call that carries its own rationale comment is a documented decision, not dead code:
  `ResultExport.validateMergedRegions` (EPBDS-7848), `trackAllColumnsForAutoSizing`, the `intern()` TODO in
  RuleRowHelper, the bulk OpenAPI rewrite block in ITEST `HttpClient`.
- `Docs/examples/**` (index.md, k8s, production) is linked from `Docs/README.MD` as a directory.
- webstudio `SessionListener` is alive: it implements HttpSessionListener and HttpSessionIdListener, both of which
  the container dispatches, and drives the session cache, Spring security session events and `WebStudio.destroy()`.

## CI flakes

- IT (studio-acl): `OracleRdbmsTest.upgrade` fails "Failed requests: expected 0 but was N" with `ORA-12516` while
  the other vendors pass. Oracle Free container limit, not the diff; one rerun clears it.
- IT (services-data) flake, HIGH RATE: `apache/kafka-native:latest` exits code 1 in its own `setup`, GraalVM
  segfault at `Pwd.getpwuid`; Testcontainers then times out on "RECOVERY to RUNNING". The job starts the container
  once per suite, so any of Kafka Smoke / WS Tracing / WS Store Log Data can be the victim, a different one each
  time. One start in three dies. Budget two reruns per SHA; repeated failures here are the flake, not a regression.
- Before calling such a failure a tag regression, check whether ANOTHER PR ran the same job in the same window;
  that disproved exactly that theory once. The base `Build` workflow is a multi-JDK matrix red since August.
- Tests (without ITEST): `ModuleWorkspace.test.tsx` two cases on `module-workspace-error` fail on the CI runner
  while the same tree passes all studio-ui tests locally.
- A job log is fetched with `get_job_logs` (tail 8000 lines lands in a file); find the failing requests with
  `test-resources/... - FAIL` and the cause with `ORA-|SQLException|expected: <`.
- The `Sonar analysis` job is skipped when any job of the run fails, so a red flake also hides Sonar's verdict on
  that head; `sonarcloud.io/api/project_pull_requests/list?project=org.openl.rules:openl-tablets` names the SHA.
- `rerun_failed_jobs` returns 403 "This workflow is already running" while ANY job of the run is in flight; wait
  for the run to finish. It reuses the workspace, so exec files truncated by the crashed attempt make the later
  `Sonar analysis` die in `report-aggregate` with "Unknown block type f9" — corrupt jacoco data, not a quality
  gate. Re-running Sonar alone re-reads the same files; rebase onto main instead.

## Container facts

- No `gh` CLI: GitHub MCP tools only (pull_request_read, update_pull_request, add_issue_comment, actions_list
  list_workflow_jobs, actions_run_trigger rerun_failed_jobs with the workflow run id from a check's html_url).
- Container presets `gpg.format=ssh` and `commit.gpgsign=true` globally but sets no `GIT_AUTHOR_*`/`GIT_COMMITTER_*`
  environment variables, so a plain `git config --global user.*` is enough; verify once after the first commit.
- Error Prone's Unused checks are not enabled in the build, so PMD and the bytecode scan are the Java detectors;
  javac options go into the root pom's compilerArgs, never on the `mvn` command line.
- 4 cores, 15 GB RAM: `-T2` for the reactor. `npx tsc` runs fine beside Maven, vitest does not.
- `.toDelete/` is gitignored: keep the PMD ruleset, scratch poms and detector scripts there. `~/.m2` and
  `STUDIO/studio-ui/node_modules` may already be warm — check before budgeting a cold build.
- A detached `setsid nohup script.sh` survives the tool timeout; wait on the process, not on the log's last line.
- Edit the ledger through `git worktree add` on `origin/dead-code/ledger`, never by switching the sweep branch.

## Exhausted veins

- ALL 13 change types re-swept repo-wide at `abb3d5ae81` over 86 reactor modules, studio-ui, Docs and DEMO, with
  zero findings: commented-out code, PMD 5 rules (28 hits), ASM member scan (5,162 classes / 53,375 members),
  whole-type scan (4,148 types), identifier index (13,856 text files), resources and images by name and stem,
  message bundles and locales, config defaults, TS exports plus `tsc --noUnusedLocals --noUnusedParameters`,
  dependency:analyze-only (569 hits), pom properties, managed entries, exclusions, managed plugins,
  `@SuppressWarnings`, constant boolean guards, `.editorconfig`, `.gitignore`, Docs page graph and DEMO css.
  Four consecutive exhaustive runs found nothing; sweep only the delta from here.
- Container registrations audited for behavioural deadness at `1dc89c91e0`: webstudio web.xml (4 filters,
  3 listeners) and all 8 `@WebFilter`/`@WebServlet` classes in webstudio and ruleservice.ws. One finding
  (PR #2135); everything else is reachable. Re-run only when a registration is added or a consumer removed.
- JSF-era orphans left by the React migration: none exist. Zero `.xhtml`/`.jsp`, no `faces-config.xml`, no
  `@ManagedBean`/`@ViewScoped`/`@FacesConverter` and friends, and no faces/richfaces/jstl/primefaces dependency in
  any pom. The post-#2135 web.xml carries only reachable registrations. Do not re-probe this vein.

## Human follow-ups

- `site.webmanifest` in `WSFrontend/org.openl.rules.ruleservice.ws/resources/static/` and
  `STUDIO/studio-ui/public/icons/` names the 512x512 icon `android-chrome-512x512.pngs` (trailing `s`), so it
  resolves in neither module; the DEMO copy is correct. A typo to fix, not dead code — raised on PR #2129.
- `CorsFilter` is registered twice under different names — `@WebFilter("/*")` (name = the FQCN) and web.xml's
  `CorsFilter` — so it runs twice per request, doubling each `Access-Control-*` header; browsers reject a
  duplicated `Access-Control-Allow-Origin`. Latent while `cors.allowed.origins` is unset. A fix, not a deletion.
- `Docs/examples/production/` and `Docs/production-deployment/` are two 320K near-identical copies of the same
  example tree, differing only in a README. Both are reachable, so neither is dead; merging them is editorial.
- Swapping `de.qaware.xff.filter.ForwardedHeaderFilter` (`org.openl:x-forwarded-filter:2.0`) for Spring's
  `ForwardedHeaderFilter` is BLOCKED: the root pom documents the opposite decision, and both call sites set
  `xForwardedPrefixStrategy=PREPEND`, which Spring's filter cannot express — it always REPLACES the context path.
- Workspace passivation never ran and is now removed from main. To revive it, `RulesUserSession` must implement
  `HttpSessionActivationListener` itself — it IS a session attribute, whereas `SessionListener` was only
  registered. An addition, not this routine's work.
- ORA-12516 in IT (studio-acl) deserves a real fix in the Oracle container setup (process/session limit), and the
  `ModuleWorkspace.test.tsx` timing failure a source-level fix; both bite green PRs.
- Dependency hygiene (additions): ~293 used-undeclared findings, notably spring-security-core in org.openl.security
  and org.openl.rules.jackson in ruleservice.ws.
- PMD `Parsing failed in ParseLock#doParse()` on `BranchedProjectIndexService$IndexState`: a PMD 7 type resolution
  bug, harmless to the report.
- Flyway migration `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` script; confirm Flyway applies it.
- ITEST pulls `apache/kafka-native:latest` in three suites (RunKafkaSmokeITest:43, RunStoreLogDataITest:68,
  RunTracingITest:51, documented at `ITEST/AGENTS.md:54`); its startup flake is frequent enough to cost reruns.
  Pinning the tag or moving to the JVM image `apache/kafka:4.3.1` would help. Infrastructure, outside the sweep.
- `RulesUtilsTest.testParseFormattedDouble` carries `@SuppressWarnings("deprecated")`, a key javac ignores, while
  both methods it calls are deprecated: the fix is the key `deprecation`, a rename this routine may not make.

## Run log

- 2026-09-18 b: container-registration vein worked; PR #2135 removed the session passivation path and merged.
  Cost 4 kafka flakes / 3 reruns / 1 rebase.
- 2026-09-19 a: user-requested full repo-wide re-sweep of all 13 change types at `abb3d5ae81`; whole-repo build
  green, zero deletable findings, no commit and no PR. Added 4 FP shapes and 1 human follow-up.
- 2026-09-19 b: main unmoved, no open sweep PR, so no delta to sweep. Probed one new vein (JSF-era orphans from the
  React migration) to zero findings and closed it; compacted the ledger off its 300-line ceiling.
