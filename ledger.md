# Dead-code sweep ledger — openl-tablets

## Resume point

- Main is `915e0047d2`, fully swept. PR #2145 (`dead-code/delta-sweep`) is open with one commit; maintain it first.
- Main moving is no longer a reason to expect findings: its last 50 commits were themselves a removal wave
  (-3013 lines) by another agent, and re-running all 13 change types over them yielded one item. Sweep the
  delta, expect zero, spend the run on new veins.
- A cold `~/.m2` costs 42 min for the reactor build; PMD then takes 6 min on the warm tree, not 30.
- Before every push: list open `dead-code/*` PRs and re-fetch main; parallel runs of this routine share the branch.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | done; 7 blocks, all prose or parked calls with a rationale |
| 2 | Never-read assignments, dead stores | done; 14 PMD hits, all documented FPs |
| 3 | Unused locals, private fields/methods/params | done; 14 PMD hits, all documented FPs |
| 4 | Unused Maven dependency declarations | done; 537 analyze hits + npm deps, all FPs |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | done; 6 hits, all plugin-read flags |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done; 1 removal, PR #2145 |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 220 candidates, 0 unreferenced |
| 8 | CSS rules and inline styles | done; 1 file, 4 selectors, all used |
| 9 | Legacy JS functions and pages | done; 0 `.xhtml` remain, only keep-listed vendor JS |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done; 1,583 keys, 113 flagged, all template-resolved |
| 11 | TypeScript exports, types, components, imports | done; 1,151 exports, 0 dead |
| 12 | Test fixtures: workbooks, utility classes, stub members | done |
| 13 | Package-private/protected members and unreferenced internal classes | done; 1,025 raw hits, 0 survivors |

## Open PR

- #2145 `dead-code/delta-sweep`, head `bc20730841`, cut from `915e0047d2`.
- `bc20730841` Drop the decision-table match type no matched definition can carry — removes
  `MatchType.PARAMS_RENAMED_CASTED`, change type 6.
- All checks green except Tests (without ITEST), red twice on the studio-ui flake above; rerun budget spent and
  two comments posted (diagnosis, then the proposed patch). Nothing else is owed until CI or a review changes.

## Merged PRs

- #2120 (-487), #2129 (-1), #2134 (-87, SessionTimeoutFilter), #2135 (-54, the session passivation path).
- A removal proven by unreachable behaviour rather than by non-reference is accepted on that evidence alone.
  `activate()` stays — `RulesUserSession.getUserWorkspace()` calls it.
- The maintainer merges a small, well-evidenced sweep PR within the hour, before CI finishes; do not wait on green.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type at `915e0047d2`; nothing open.

## Deferred findings

- ~599 public members and ~35 public types across the DEV, STUDIO and WSFrontend jars are unreferenced in
  bytecode, write-only Lombok setters among them. All published API, all kept under rail 8.2; the ASM scan below
  re-derives the list in one pass whenever a maintainer wants it.
- ExpressionFactoryImpl `_getFromCache`/`_putInCache` are both `= false`, compiling out findExpression and the
  cache writes: a toggle pair, not dead code.
- MergeResult record: `status` component ignored by the compact constructor; removal changes a public record signature.
- Public constants nothing reaches, kept under rail 8.2: `XlsProjectionType` constructs only BOOK, SHEET, TABLE
  and CELL of its 12 and carries its own `// TODO do we need the rest?`; `EventOfInterestConstants.MINMAX`.
- `org.eclipse.jetty:jetty-home` managed entry is in no dependency tree and declared by no pom; the DEMO scripts
  fetch Jetty by `jetty.version`. Confirm nothing resolves it before dropping it.
- `org.openl.rules.jackson` in ruleservice.ws.common and `spring-security-config` in org.openl.security are
  unused where declared but provide what their consumers compile against undeclared (~293 such used-undeclared
  findings repo-wide, notably spring-security-core). Fixing that is an addition, never a deletion here.

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
- A class named by string composition has no textual reference at all: `OperationFactory` builds the 13 TBasic
  runtime operations as package + `conversionStep.getOperationType()` + `Operation`. Before calling a type dead,
  search the name MINUS a common suffix, not only the whole name.
- An enum whose `values()` is iterated keeps every constant alive, however unreferenced the constant looks
  (Separator.recognize, Brackets.isBracket). To prove an enum constant dead, show it is never stored and never
  returned, then show the `values()` loop is a no-op for it — non-reference alone never settles an enum.
- In a Spring application, a production type referenced only from test sources is the norm, not a finding: 62 such
  types, all `@Component`/`@Configuration`/`@RestController` found by classpath scan. This vein yields nothing.
- An npm dependency is invoked from `package.json` `scripts` or read implicitly by tsc (`@types/*`): exclude the
  lockfile from the search, never package.json itself.
- An identifier index keyed on `[A-Za-z_$][\w$]*` cannot see a file stem starting with a digit (the hex-named
  `Docs/assets/images/**`). Confirm every resource finding with a plain `git grep -lF`.
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
- Never edit the working tree or rebase while any Maven run is active (a rebase checks out intermediate trees).
- PMD needs reactor artifacts and is not in a cold `~/.m2`, so run it online, fully qualified (a `pmd:` prefix
  fails offline): `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only
  -Pitest -fae -T2 -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`. 6 min on a warm tree, 75 reports.
- Add maven-pmd-plugin 3.28.0 under root `<build><plugins>` with `includeTests` and the ruleset at
  `${maven.multiModuleProjectDirectory}/.toDelete/pmd-dead-code.xml`; parse every `target/pmd.xml`; ignore
  `target/generated-sources`; restore the pom with `git checkout -- pom.xml`.
- Bytecode scan: ~120-line ASM program (asm 9.10.1 from `~/.m2`) over every `target/classes` and
  `target/test-classes`; record invocations, field access, method handles, invokedynamic args and `ldc` strings;
  drop annotated members, overrides (unknown third-party supertype counts as override), names in literals, then
  the constant-inlining and Lombok filters above. ~5,100 classes, ~52,500 members, ~1,000 raw hits, 0 survivors.
- Chain the install and PMD in one detached `setsid nohup` script touching a DONE file; poll the file, never the
  log's last line. Anchor any `pkill -f 'name[.]py'` or it matches the calling shell's own command line.
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
- Prove non-reference with `grep -rIwF <name>` over all tracked files plus `grep -raF` for binaries and `unzip -p`
  for workbooks; a `.xls` is searched as latin-1 and UTF-16 bytes; documentation is searched case-insensitively.
  Use `git ls-files`, never a raw `grep -r`: untracked `STUDIO/studio-ui/dist/` otherwise answers every query.
- A docs tree reached only through a directory link (`README.MD` → `examples/` → `index.md` → subfolder) is
  alive: count a link to the PARENT directory, not just to the file, before calling a Docs page unreferenced.
- Removing members is a fixpoint: re-check fields, private helpers, constructor parameters and imports the removal
  orphaned. SonarCloud's "new issues" list exactly those, so read it after every push (no auth):
  `sonarcloud.io/api/issues/search?componentKeys=org.openl.rules:openl-tablets&pullRequest=N&sinceLeakPeriod=true`.
- Stage every commit by explicit path (`git add -- <files>`) or a `git rm` staged earlier rides into it; use
  `git show --stat`, never `git diff --cached --stat A B`.
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
- Before calling such a failure a tag regression, check whether ANOTHER PR ran the same job in the same window.
  The base `Build` workflow is a multi-JDK matrix red since August.
- Tests (without ITEST), studio-ui: `ModuleWorkspace.test.tsx` times out in `waitFor` on the CI runner while the
  file passes locally in 13 s. The tell is a failing set that SHRINKS between attempts (2 cases, then only
  `:260`), a DOM dump still showing `browser.compile.compiling` and an `ant-skeleton`, and a vitest wall time
  near 860 s with ~613 s of it in imports. `vite.config.ts` allows 20 s per test under CI and calls that "the
  margin for the machine being busy"; on a loaded runner it is not enough. Re-running does NOT clear it — two
  attempts on PR #2145 both failed. Do not spend a rerun budget here; comment with the patch below and move on.
- A job log is fetched with `get_job_logs` (tail 8000 lines lands in a file); find the failing requests with
  `test-resources/... - FAIL` and the cause with `ORA-|SQLException|expected: <`.
- `Sonar analysis` is skipped when any job of the run fails, so a red flake hides Sonar's verdict on that head
  and the issues API then answers 0 because the head was never analysed, not because it is clean. Confirm the
  SHA was analysed via `sonarcloud.io/api/project_pull_requests/list?project=org.openl.rules:openl-tablets`.
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
- Edit the ledger through `git worktree add` on `origin/dead-code/ledger`, never by switching the sweep branch.

## Exhausted veins

- ALL 13 change types re-swept repo-wide at `915e0047d2` over 86 reactor modules, studio-ui, Docs and DEMO, with
  zero findings: commented-out code, PMD 5 rules (28 hits), ASM member scan (5,162 classes / 53,375 members),
  whole-type scan (4,148 types), identifier index (13,856 text files), resources and images by name and stem,
  message bundles and locales, config defaults, TS exports plus `tsc --noUnusedLocals --noUnusedParameters`,
  dependency:analyze-only (569 hits), pom properties, managed entries, exclusions, managed plugins,
  `@SuppressWarnings`, constant boolean guards, `.editorconfig`, `.gitignore`, Docs page graph and DEMO css.
  Five consecutive exhaustive runs found one item; sweep only the delta from here.
- New veins probed and closed: Maven profiles (11), npm dependencies (45), orphaned `package-info.java`, empty
  tracked files, production types referenced only from tests (62), and enum constants named only at their
  declaration (18). Only the enum vein paid.
- Container registrations audited for behavioural deadness at `1dc89c91e0`: webstudio web.xml (4 filters,
  3 listeners) and all 8 `@WebFilter`/`@WebServlet` classes in webstudio and ruleservice.ws. One finding
  (PR #2135); everything else is reachable. Re-run only when a registration is added or a consumer removed.
- JSF-era orphans left by the React migration: none exist. Zero `.xhtml`/`.jsp`, no `faces-config.xml`, no
  `@ManagedBean`/`@ViewScoped`/`@FacesConverter` and friends, and no faces/richfaces/jstl/primefaces dependency in
  any pom. The post-#2135 web.xml carries only reachable registrations. Do not re-probe this vein.

## Human follow-ups

- `CorsFilter` is registered twice under different names (`@WebFilter("/*")` and web.xml), so it runs twice and
  doubles each `Access-Control-*` header, which browsers reject. Latent while `cors.allowed.origins` is unset.
- `Docs/examples/production/` and `Docs/production-deployment/` are near-identical 320K copies differing only in
  a README. Both reachable, so neither is dead; merging them is editorial.
- Swapping `org.openl:x-forwarded-filter` for Spring's `ForwardedHeaderFilter` is BLOCKED: the root pom documents
  the opposite decision, and both call sites need `xForwardedPrefixStrategy=PREPEND`, which Spring always
  REPLACES. Do not re-open it.
- Reviving workspace passivation (removed from main) needs `RulesUserSession` to implement
  `HttpSessionActivationListener` itself: it IS a session attribute, whereas `SessionListener` was only
  registered. An addition, not this routine's work.
- ORA-12516 in IT (studio-acl) deserves a real fix in the Oracle container setup (process/session limit).
- `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` of the 17 flyway/common scripts, and neither
  `DBMigrationBean` nor `DBTestConfiguration` sets `sqlMigrationPrefix`, so Flyway's case-sensitive default `V`
  skips it: version 14 is absent from every deployed schema (V13.4 jumps to V15) and the ExternalGroups index
  is never created. A rename, so not this routine's deletion work.
- `ModuleWorkspace.test.tsx` needs the CI `testTimeout` in `STUDIO/studio-ui/vite.config.ts` raised from 20_000
  (40_000 keeps the local 5 s fast-fail), or `isolate: false`. Proposed on PR #2145; a sweep PR may not carry it.
- `KafkaMessageHeader.Type.PRODUCER_RECORD` is documented as usable in the rule-services advanced-configuration
  guide, but `StoreLogDataMapper` acts only on CONSUMER_RECORD. Mapper or guide is wrong; the constant is
  user-written public API and stays either way.
- ITEST pulls `apache/kafka-native:latest` in three suites (see `ITEST/AGENTS.md:54`); pinning the tag or moving
  to the JVM image `apache/kafka:4.3.1` would stop its startup flake costing reruns. Outside the sweep.
- `RulesUtilsTest.testParseFormattedDouble` suppresses `"deprecated"`, a key javac ignores, while both methods it
  calls are deprecated; the fix is the key `deprecation`, a rename this routine may not make.

## Run log

- 2026-09-19 a: full re-sweep of all 13 change types at `abb3d5ae81`; zero findings, no PR. Added 4 FP shapes.
- 2026-09-19 b: no delta to sweep; closed the JSF-orphan vein at zero and compacted the ledger to its ceiling.
- 2026-09-20: swept the `abb3d5ae81..915e0047d2` delta, 13 change types plus 6 new veins. One finding, PR #2145,
  left red on the studio-ui flake. Added 5 FP shapes, 2 deferred public-API items, 2 human follow-ups.
