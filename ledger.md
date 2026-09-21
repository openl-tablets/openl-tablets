# Dead-code sweep ledger — openl-tablets

## Resume point

- Everything up to and including `origin/main` at the EPBDS-14123 tag migration is swept; sweep only what main
  gains after it. Do not pin main's SHA — dependabot bumps move it constantly and are never a dead-code delta.
- All 13 change types are exhausted repo-wide. A run is now: maintain the PR, sweep the delta (expect zero),
  spend the rest on a NEW vein. Code veins are mined out; the paying vein is documentation that names something
  the code no longer has.
- A cold `~/.m2` costs ~35 min for the reactor build; PMD then takes 6 min on the warm tree, not 30.
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
| 14 | Documentation of settings and classes the code no longer has | in-progress; 1 removal, PR #2145 |

## Open PR

- #2145 `dead-code/delta-sweep`. Its owner rebased it once onto new main, so reset the local branch to the remote
  before adding to it and never force-push what you did not write.
- Two commits: "Drop the decision-table match type no matched definition can carry" (type 6,
  `MatchType.PARAMS_RENAMED_CASTED`) and "Stop documenting the data source setting removed in 5.23.8" (type 14,
  `ruleservice.datasource.filesystem.supportVersion`), head `9d76bf21c7`.
- It was GREEN on the one-commit head with all 21 checks and a clean Sonar gate; reviewers are all it waits on.

## Merged PRs

- #2120 (-487), #2129 (-1), #2134 (-87, SessionTimeoutFilter), #2135 (-54, the session passivation path).
- A removal proven by unreachable behaviour rather than by non-reference is accepted on that evidence alone.
  `activate()` stays — `RulesUserSession.getUserWorkspace()` calls it.
- The maintainer does not merge a sweep PR red: on #2145 they rebased it onto new main and left it to CI.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type; nothing open.

## Deferred findings

- ~599 public members and ~35 public types across the DEV, STUDIO and WSFrontend jars are unreferenced in
  bytecode, write-only Lombok setters among them. All published API, kept under rail 8.2; the ASM scan below
  re-derives the list in one pass whenever a maintainer wants it. Named examples: `XlsProjectionType` builds 4 of
  its 12 constants and carries its own `// TODO do we need the rest?`; `EventOfInterestConstants.MINMAX`; the
  `MergeResult` record's `status` component, ignored by its compact constructor.
- ExpressionFactoryImpl `_getFromCache`/`_putInCache` are both `= false`, compiling out findExpression and the
  cache writes: a toggle pair, not dead code. `MergeResult.status` is covered by the public-API line above.
- `org.eclipse.jetty:jetty-home` is in no dependency tree and declared by no pom; the DEMO scripts fetch Jetty by
  `jetty.version`. Confirm nothing resolves it before dropping it.
- `org.openl.rules.jackson` in ruleservice.ws.common and `spring-security-config` in org.openl.security are unused
  where declared but provide what their consumers compile against undeclared (~293 such findings repo-wide).
- `.gitattributes` keeps `**/openl-repository/workspace/**/*.xml text eol=lf`, and no such path exists. Deleting
  it is safe but its intent may belong on the ITEST `openl-repository` XML that does exist — a repoint, not a
  deletion, so it needs a human.

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
  `tests.${kind}` and some 20 more. Read the union type feeding the template, which names exactly the live keys,
  and search the `no_`/prefix form too or a bare tail search misses `tests.no_${kind}`.
- An exported TS type or const used only inside its own file looks unimported; that makes the `export` redundant,
  not the type dead. Removing `export` is a rename, not a deletion.
- A leading positional callback parameter reported by `tsc --noUnusedParameters` or PMD `UnusedFormalParameter` is
  never removable: dropping it shifts the parameter that IS used (`Array.from(…, (unused, column) => …)`).
- A pom `<properties>` entry with no `${...}` dereference may still be read by a plugin by name:
  `lombok.delombok.skip`, `archetype.test.skip`, `invoker.skip`. Check the plugin before calling it dead.
- A private or package member whose name appears in any string literal is reflective (`@MethodSource`, JAXB, OpenL
  datatype binding): filter bytecode hits on Java string literals, non-Java text and workbook strings.
- Getter/setter hits must also be checked by property name: Jackson DTOs (RepositorySettings, `*Append`,
  SupportedFeaturesModel) and OpenL datatype beans are bound by `basePath`, not `setBasePath`.
- JAXB private `beforeMarshal`/`afterUnmarshal` run reflectively; Spring MVC handlers have no Java caller; record
  component accessors and generic bridge overrides (`InputStats.getAvgX` erasing to Number) are alive.
- A top-level type whose simple name occurs only in its own file is still alive when a framework names it: JUnit
  by file pattern, Spring by classpath scan, `@Mojo` by the plugin descriptor.
- PMD UnusedAssignment blind spots: constructor early return (CellStyle), a value read back through a callback
  (DynamicPropertySource.settings), a field read by a getter (AProjectCreator), `key = null` before `System.gc()`,
  publish-before-block stores (DebugChannel, DebugHookImpl), a re-entrancy store (ServiceManagerImpl.deploy) and
  `result = null` in an empty catch (GitRepository).
- PMD UnusedLocalVariable: try-with-resources locals held only for their lifecycle — Mockito `mockConstruction`
  scopes, a Spring context, `WebSocketAuthTest.stomp` — plus a counting for-each and a cast before `fail()`.
- A `@SuppressWarnings` javac stays silent about is still alive when the element holds a raw cast, a raw
  `instanceof` or a raw type argument. Keys javac does not know (`unused`, `resource`, `squid:*`,
  `NullableProblems`, Error Prone names) are IDE or Sonar keys, judged by that tool.
- Reflection fixtures asserted by name: epbds6830 BeanA.getAB, AOpenClassTest.getC, JavaOpenClassTest.gg, MyProp
  fields named in a binary .xls, YamlMapperFactoryTest transient fields, InterfaceTransformerTest.TestInterface.
- Bundle conventions: ValidationMessages `openl.error.<status>.<code>.message` (code composed in Java), sql-errors
  keyed by vendor error code, `openl-default.properties` keys composed as `repo-<id>.` and `$ref` indirection.
  The documented `repository.archive.*` and `repository.design.*` names are that same composition, not dead keys.
- Resource stems alive by convention: Flyway `db/flyway/**`, `META-INF/openl/extension-*.xml`,
  `openl-db-repository-<code>.properties`, `rapi-doc/rapidoc-min.js`, `site.webmanifest` icons,
  `META-INF/maven/archetype-metadata.xml`, `compose.override.example.yaml`.
- dependency:analyze FPs: a module declaring no `<dependencies>` still gets findings from its parent; inherited
  test harness (junit-jupiter, junit-pioneer, mockito-junit-jupiter, log4j-to-slf4j are 296 of 569); provided
  annotations and processors (jspecify, lombok); runtime providers named from configuration (the five in
  security.standalone via `security-hibernate-beans.xml`; maven-scm; cxf-rt-features-logging; the Azure SDK's
  jackson dataformats and reactor-core); aggregators; wars and jdbc drivers an ITEST needs only to boot a server.
- A managed entry no pom declares is a transitive version pin: judge it by `dependency:tree -Dverbose -Pitest`
  over all modules, not by declaration. An `exclusion` is judged by resolving its parent alone in a scratch pom.
- A class named by string composition has no textual reference at all: `OperationFactory` builds the 13 TBasic
  runtime operations as package + `conversionStep.getOperationType()` + `Operation`. Before calling a type dead,
  search the name MINUS a common suffix, not only the whole name.
- An enum whose `values()` is iterated keeps every constant alive, however unreferenced it looks. To prove an enum
  constant dead, show it is never stored and never returned, then show the `values()` loop is a no-op for it.
- In a Spring application, a production type referenced only from test sources is the norm: 62 such types, all
  found by classpath scan. This vein yields nothing.
- An npm dependency is invoked from `package.json` `scripts` or read implicitly by tsc (`@types/*`): exclude the
  lockfile from the search, never package.json itself.
- An identifier index keyed on `[A-Za-z_$][\w$]*` misses a file stem starting with a digit: confirm with `git grep -lF`.
- A dotted-name detector over Markdown catches heading anchor slugs and wrapped table cells, not settings: require
  dot separators only, drop hyphenated slugs, and rejoin a name split across a line wrap.
- Jekyll lists pages by `nav: "auto"` and by `migration-notes.md`, so a link-graph orphan check calls 265 live
  pages orphans. Site-absolute links (`/Docs/…`) resolve on the published site, not on disk — only a wrong-case
  path (`/docs/…`) or a missing file is really broken.
- The clone is SHALLOW (~49 commits) and its root commit adds every file, so `git log -S` answers that root for
  every string. Never claim when something was removed from history here; prove absence at HEAD instead.

## Method rules

- Build the whole repo once per run: `LANG=C.UTF-8 mvn clean install -Dquick -DnoPerf -T2
  -Daether.syncContext.named.time=600`. Unset `gpg.format` and `commit.gpgsign` globally first or JGit tests die.
- Build one identifier-frequency index over the whole tree once (regex `[A-Za-z_$][\w$]*` per file, into a
  Counter) and answer every "is this name used" question from it: 2 seconds, versus hours of per-name scanning.
  A name whose total count equals its declaration count is unreferenced.
- Never name a scratch script after a stdlib module (`.toDelete/types.py` breaks every `import`), and never edit
  the working tree or rebase while a Maven run is active (a rebase checks out intermediate trees).
- PMD needs reactor artifacts and a warm `~/.m2`, so run it online and fully qualified (a `pmd:` prefix fails
  offline): `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only -Pitest
  -fae -T2 -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`. Add the plugin under root `<build><plugins>` with the ruleset
  at `${maven.multiModuleProjectDirectory}/.toDelete/`, parse every `target/pmd.xml`, then restore the pom.
- Bytecode scan: ~120-line ASM program (asm 9.10.1) over every `target/classes` and `target/test-classes`
  recording invocations, field access, method handles, invokedynamic args and `ldc` strings; drop annotated
  members, overrides and names in literals, then apply the constant-inlining and Lombok filters above.
- Chain the install and PMD in one detached `setsid nohup` script touching a DONE file; poll the file, never the
  log's last line. Anchor any `pkill -f 'name[.]py'` or it matches the calling shell's own command line.
- A class named only by a container registration (`web.xml`, `@WebFilter`) is NOT proven alive by it: judge it by
  whether its behaviour is reachable. A `<listener>` serves only the interfaces the container sorts it into, so a
  registered-but-unbound one never fires — verify from the container jar with `javap -c`, never from the spec.
- Documentation is the one vein still paying, and it has two decisive cross-checks. For a class: every
  `org.openl.*` token in a guide must resolve to a type that exists at HEAD. For a setting: every property a
  release-note migration table marks **Removed** must appear in no current guide. Both are mechanical and both
  found real defects; a doc that merely names the WRONG name needs a rename and is not this routine's work.
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
- CodeRabbit's `Docstring Coverage` pre-merge check fails every deletion-only PR: it scores the functions inside
  the touched hunks, which on a sweep PR are the removed ones. Decline it by comment citing `git diff -U0`.
- The container resets `git config --global user.*` to Claude mid-session. Pass `GIT_AUTHOR_*`/`GIT_COMMITTER_*`
  INLINE on every commit and verify BEFORE pushing: rail 8.4 bars force-pushing `dead-code/ledger`.

## Keep-list

- OpenL datatype beans and rules interfaces in tests are bound from Excel by property or method name
  (IChildBean.getMyBean, Tutorial4Interface.getTheft_rating, Location setters, RulesUtilsTest.testFlatten).
- Jackson-bound webstudio models keep every accessor: RepositorySettings, AWSS3RepositorySettings,
  GitRepositorySettings, `*Append`, SettingValueWrapper, SupportedFeaturesModel.
- Convention files: Flyway migrations, `META-INF/openl/extension-*.xml`, `openl-db-repository-*.properties`, static
  rapi-doc, site.webmanifest icons, `META-INF/services/**`, ITEST `application-*.properties` (Spring profiles),
  archetype `archetype-metadata.xml`, `compose.override.example.yaml`.
- Config defaults in `openl-default.properties` are documented in Docs guides and composed at runtime; all alive.
- Demo workbooks under `org.openl.rules.demo/src/**` and `webstudio/test/rules/decisionTableIndexes/` load by folder.
- `.gitignore` and `.gitattributes` entries for a file TYPE stay even when no such file is tracked — they are
  prophylactic. Only a PATH-specific rule naming a directory that does not exist is a candidate.
- Root exclusions on httpclient, azure-storage-blob, hibernate-validator, swagger-parser and webstudio's
  poi-ooxml-lite all still remove a transitive artifact (verified by scratch-pom resolution).
- A parked call that carries its own rationale comment is a documented decision, not dead code:
  `ResultExport.validateMergedRegions` (EPBDS-7848), `trackAllColumnsForAutoSizing`, the `intern()` TODO in
  RuleRowHelper, the bulk OpenAPI rewrite block in ITEST `HttpClient`.
- webstudio `SessionListener` is alive: it implements HttpSessionListener and HttpSessionIdListener, both of which
  the container dispatches, and drives the session cache, Spring security session events and `WebStudio.destroy()`.
- The Hibernate `Tag`, `TagType` and `TagTemplate` entities and their DAOs stay: the tag catalogue, templates and
  the Studio tag admin still read them. Only the `OpenLProject` entity went with the JDBC tag migration.

## CI flakes

- IT (studio-acl): `OracleRdbmsTest.upgrade` fails "Failed requests: expected 0 but was N" with `ORA-12516` while
  the other vendors pass. Oracle Free container limit, not the diff; one rerun clears it.
- IT (services-data), HIGH RATE: `apache/kafka-native:latest` exits code 1 in its own `setup`, GraalVM segfault at
  `Pwd.getpwuid`; Testcontainers then times out on "RECOVERY to RUNNING". The container starts once per suite, so
  the victim ROTATES — a rotating victim proves the flake, a regression kills the same suite every time. Budget
  two reruns per SHA.
- Before calling such a failure a tag regression, check whether ANOTHER PR ran the same job in the same window.
  The base `Build` workflow is a multi-JDK matrix red since August.
- Tests (without ITEST), studio-ui: `ModuleWorkspace.test.tsx` times out in `waitFor` only on a loaded runner. The
  tell is a DOM dump still showing `browser.compile.compiling` and a vitest wall time near 860 s against the 20 s
  per-test CI ceiling, plus a failing set that SHRINKS between attempts. Never push a vitest change to chase it;
  a new SHA is the cheapest cure.
- Fetch a job log with `get_job_logs` (tail 8000); find failures with `... - FAIL`, the cause with `ORA-|expected: <`.
- `Sonar analysis` is skipped when any job of the run fails, so a red flake hides its verdict and the issues API
  answers 0 because the head was never analysed. Confirm the SHA at
  `sonarcloud.io/api/project_pull_requests/list?project=org.openl.rules:openl-tablets`.
- `rerun_failed_jobs` returns 403 while ANY job is in flight, and it reuses the workspace, so exec files truncated
  by a crashed attempt make `Sonar analysis` die with "Unknown block type f9". Rebase onto main instead.

## Container facts

- No `gh` CLI: GitHub MCP tools only (pull_request_read, update_pull_request, add_issue_comment, actions_list
  list_workflow_jobs, actions_run_trigger rerun_failed_jobs with the workflow run id from a check's html_url).
- Container presets `gpg.format=ssh` and `commit.gpgsign=true` globally but sets no `GIT_AUTHOR_*`/`GIT_COMMITTER_*`
  environment variables, so a plain `git config --global user.*` is enough; verify once after the first commit.
- Error Prone's Unused checks are not enabled in the build, so PMD and the bytecode scan are the Java detectors;
  javac options go into the root pom's compilerArgs, never on the `mvn` command line.
- 4 cores, 15 GB RAM: `-T2` for the reactor. `npx tsc` runs fine beside Maven, vitest does not.
- `.toDelete/` is gitignored: keep the PMD ruleset, scratch poms and detector scripts there. `~/.m2` and
  `STUDIO/studio-ui/node_modules` start COLD in a fresh container — check before budgeting.
- Edit the ledger through `git worktree add` on `origin/dead-code/ledger`, never by switching the sweep branch.

## Exhausted veins

- ALL 13 change types re-swept repo-wide over 86 reactor modules, studio-ui, Docs and DEMO with zero findings:
  commented-out code, PMD 5 rules, ASM member scan (5,162 classes / 53,375 members), whole-type scan, identifier
  index (13,856 text files), resources and images by name and stem, bundles and locales, config defaults, TS
  exports plus `tsc --noUnusedLocals --noUnusedParameters`, dependency:analyze-only, pom properties, managed
  entries, exclusions, managed plugins, `@SuppressWarnings`, constant guards, `.editorconfig`, `.gitignore`, the
  Docs page graph and DEMO css. Six consecutive exhaustive runs found two items.
- Veins probed and closed at zero: Maven profiles (11), npm dependencies (45), orphaned `package-info.java`,
  empty tracked files, production types referenced only from tests (62), container registrations (web.xml and all
  8 `@WebFilter`/`@WebServlet`), JSF-era orphans (no `.xhtml` or faces dependency exists anywhere), pom file-path
  references (6 hits, all destination paths or archetype velocity tokens), Spring XML bean definitions (15 files,
  every bean type-injected or component-scanned), duplicate dependency/plugin/module/property declarations across
  209 poms, dependencies a parent already declares, and servlet init-params. Only the enum-constant vein and the
  documentation veins have ever paid.
- The EPBDS-14123 JDBC tag migration left no orphan: `OpenLProject` has zero references anywhere and its Tag
  entities are all still read. A migration commit is worth checking for orphans, but this one was clean.

## Human follow-ups

- Four current guides name a class that does not exist at HEAD; each needs the right name, which is a rename this
  routine may not make: `...databinding.annotation.MixInClassFor` in rule-services/configuration.md (twice in one
  sentence; the real annotation is `MixInClass`, used correctly in the example below it), `...kafka.ser.
  MessageDeserializer` in the same file (real: `RequestMessageDeserializer`), `...storelogdata.annotation.
  SkipFaultStoreLogData` in advanced-configuration.md (real: `SkipFault`), and `org.openl.rules.table.
  TableNotFoundException` in onboarding/troubleshooting.md, which never existed.
- `ruleservice.store.logs.enabled` is documented as the global switch for logging to external storage, and NO code
  reads it; only `ruleservice.store.logs.db.enabled` gates the one surviving backend. Either the guide is stale or
  the global gate was lost when the cassandra and elasticsearch backends went. A maintainer decides which.
- `Docs/DEPLOYMENT.md` (22 of 81 properties), `Docs/API_GUIDE.md` (6 of 10) and `Docs/TROUBLESHOOTING.md` (3 of
  28) document settings the code never reads, among them `openl.parallel.compilation.*` and `security.api-key.*`.
  These pages read as generated; the maintained guides are clean by comparison. Editorial, not a sweep.
- `Docs/developer-guides/externalized-config.md` illustrates `-D` syntax with
  `ruleservice.datasource.filesystem.supportDeployments`, removed in 5.24.0. Pick a live property for the example.
- 51 relative links in `Docs/` resolve to nothing, among them `/DEV/CLAUDE.md` and friends from `Docs/README.MD`
  and a set of lowercase `/docs/...` paths that 404 on a case-sensitive server.
- `OpenAPIConverterTest` (640 lines) and `RulesDeployerServiceTest` (354 lines) carry a bare class-level
  `@Disabled` with no reason while the code they cover is live (Studio OpenAPI project creation, the deploy Mojo
  and the S3 itests). Restore or delete is a maintainer's call; deleting them here would drop real coverage.
- `CorsFilter` is registered twice (`@WebFilter("/*")` and web.xml), so it runs twice and doubles each
  `Access-Control-*` header. Latent while `cors.allowed.origins` is unset.
- `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` of the 17 flyway/common scripts, and nothing
  sets `sqlMigrationPrefix`, so Flyway's case-sensitive default `V` skips it: version 14 is absent from every
  deployed schema and the ExternalGroups index is never created. A rename, so not this routine's work.
- ITEST pulls `apache/kafka-native:latest` in three suites; pinning the tag or moving to `apache/kafka:4.3.1`
  would stop its startup flake costing reruns. ORA-12516 in IT (studio-acl) deserves a real fix in the Oracle
  container setup. `ModuleWorkspace.test.tsx` would be steadier with the CI `testTimeout` raised from 20_000.
- `RulesUtilsTest.testParseFormattedDouble` suppresses `"deprecated"`, a key javac ignores, while both methods it
  calls are deprecated; the fix is the key `deprecation`, a rename.
- Swapping `org.openl:x-forwarded-filter` for Spring's `ForwardedHeaderFilter` is BLOCKED: the root pom documents
  the opposite decision and both call sites need `xForwardedPrefixStrategy=PREPEND`, which Spring always REPLACES.
- `Docs/examples/production/` and `Docs/production-deployment/` are near-identical 320K copies; both reachable.
  Reviving workspace passivation needs `RulesUserSession` to implement `HttpSessionActivationListener` itself.
- `KafkaMessageHeader.Type.PRODUCER_RECORD` is documented as usable but `StoreLogDataMapper` acts only on
  CONSUMER_RECORD. Mapper or guide is wrong; the constant is user-written API and stays either way.

## Run log

- 2026-09-19 b: no delta to sweep; closed the JSF-orphan vein at zero and compacted the ledger to its ceiling.
- 2026-09-20: swept the delta over 13 change types plus 6 new veins. One finding, PR #2145.
- 2026-09-21: delta was 2 dependabot bumps and the EPBDS-14123 tag migration, which left no orphan. Six new code
  veins closed at zero; the documentation veins paid one removal and seven follow-ups.
