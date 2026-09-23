# Dead-code sweep ledger — openl-tablets

## Resume point

- No PR is open: #2152 merged. Cut a fresh `dead-code/*` branch from a freshly fetched `origin/main` for the next
  finding; never pin main's SHA, dependabot moves it.
- All 14 change types are exhausted repo-wide. A run is: maintain any open PR, sweep the delta (expect zero), spend
  the rest on a NEW vein. Code veins are mined out; the paying vein is documentation or build config that names
  something the repository no longer has — every finding of the last four runs was one of those.
- A cold `~/.m2` costs ~50 min for the reactor build and 7 more for PMD; a cold `node_modules` makes studio-ui
  alone a 7-minute module. Budget the whole run around one reactor build.
- Before every push: list open `dead-code/*` PRs and re-fetch main; parallel runs of this routine share the branch.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | done; 7 blocks, all prose or parked calls with a rationale |
| 2 | Never-read assignments, dead stores | done; 14 PMD hits, all documented FPs |
| 3 | Unused locals, private fields/methods/params | done; 14 PMD hits, all documented FPs |
| 4 | Unused Maven dependency declarations | done; 537 analyze hits + npm deps, all FPs |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | done; 6 hits, all plugin-read flags |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done; 3 removals, all merged |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 220 candidates, 0 unreferenced |
| 8 | CSS rules and inline styles | done; 1 file, 4 selectors, all used |
| 9 | Legacy JS functions and pages | done; 0 `.xhtml` remain, only keep-listed vendor JS |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done; 1,583 keys, 113 flagged, all template-resolved |
| 11 | TypeScript exports, types, components, imports | done; 1,151 exports, 0 dead |
| 12 | Test fixtures: workbooks, utility classes, stub members | done |
| 13 | Package-private/protected members and unreferenced internal classes | done; 1,025 raw hits, 0 survivors |
| 14 | Documentation of settings and classes the code no longer has | done; 1 removal, merged in #2145 |

## Open PR

- None. Open the next one as soon as a finding is pushed, ready for review, and record it here.

## Merged PRs

- #2120 (-487), #2129 (-1), #2134 (-87, SessionTimeoutFilter), #2135 (-54, passivation), #2145 (-2), #2152 (-4).
- A two-commit PR mixing build config and documentation merged without a review comment: change types need not
  share a theme, and a body with per-commit evidence carries them.
- A removal proven by unreachable behaviour, not by non-reference, is accepted on that evidence alone.
- The maintainer does not merge a sweep PR red: they rebase it onto new main, wait for green, then rebase-merge.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type; nothing open.

## Deferred findings

- ~599 public members and ~35 public types across the DEV, STUDIO and WSFrontend jars are unreferenced in
  bytecode, write-only Lombok setters among them. All published API, kept under rail 8.2; the ASM scan below
  re-derives the list whenever a maintainer wants it. Examples: `XlsProjectionType` builds 4 of its 12 constants
  under its own `// TODO do we need the rest?`; `EventOfInterestConstants.MINMAX`; `MergeResult.status`.
- ExpressionFactoryImpl `_getFromCache`/`_putInCache` are both `= false`, compiling out findExpression and the
  cache writes: a toggle pair, not dead code.
- `org.eclipse.jetty:jetty-home` is in no dependency tree and declared by no pom; the DEMO scripts fetch Jetty by
  `jetty.version`. Confirm nothing resolves it before dropping it.
- `org.openl.rules.jackson` in ruleservice.ws.common and `spring-security-config` in org.openl.security are
  unused where declared but serve consumers that compile against them undeclared (~293 findings repo-wide).
- `.gitattributes` keeps `**/openl-repository/workspace/**/*.xml text eol=lf` with no such path; deleting is safe,
  but the intent may belong on the ITEST `openl-repository` XML that exists — a repoint, so it needs a human.

## False-positive shapes

- A detector that picks text files by an extension allowlist silently drops whole formats: `.webmanifest` was
  missing and reported 18 live resources dead. Select text by "no NUL byte in the first 8 KB" instead — that
  doubled the corpus to ~14,000 files and took the 18 findings to zero.
- javac inlines `static final` primitive and String constants, so a bytecode scan never sees a read and reports
  every such field dead: 1,097 of 1,515 raw hits. Judge constants by source text, never by bytecode.
- Lombok generates accessors that exist in bytecode but not in source, so a field whose name occurs only at its own
  declaration may still be read: search `get`/`set`/`is` + the capitalised name before calling it dead.
- i18next resolves `t(key, {count})` to `key_one`/`key_other`, which no literal names: a plural suffix pair whose
  base is used is alive. Keys built by interpolation are alive too — `status_${s}`, `role.${r}` and some 20 more.
  Read the union type feeding the template; search the `no_`/prefix form or a tail search misses `tests.no_${k}`.
- An exported TS type or const used only inside its own file looks unimported; that makes the `export` redundant,
  not the type dead. Removing `export` is a rename, not a deletion.
- A leading positional callback parameter reported by `tsc --noUnusedParameters` or PMD `UnusedFormalParameter` is
  never removable: dropping it shifts the parameter that IS used (`Array.from(…, (unused, column) => …)`).
- A pom `<properties>` entry with no `${...}` dereference may still be read by a plugin by name:
  `lombok.delombok.skip`, `archetype.test.skip`, `invoker.skip`. Check the plugin before calling it dead.
- A private or package member named in any string literal is reflective (`@MethodSource`, JAXB, OpenL datatype
  binding): filter bytecode hits on Java string literals, non-Java text and workbook strings. Search an accessor
  by its property name too — Jackson DTOs and OpenL beans bind `basePath`, not `setBasePath`. Also alive: JAXB
  private `beforeMarshal`/`afterUnmarshal`, Spring MVC handlers (no Java caller), record component accessors and
  generic bridge overrides (`InputStats.getAvgX` erasing to Number).
- A top-level type whose simple name occurs only in its own file is still alive when a framework names it: JUnit
  by file pattern, Spring by classpath scan, `@Mojo` by the plugin descriptor.
- PMD blind spots, all recurring. UnusedAssignment: constructor early return (CellStyle), read back through a
  callback (DynamicPropertySource.settings), read by a getter (AProjectCreator), `key = null` before `System.gc()`,
  publish-before-block (DebugChannel, DebugHookImpl), re-entrancy (ServiceManagerImpl.deploy), empty catch
  (GitRepository). UnusedLocalVariable: try-with-resources lifecycle locals, a counting for-each, a cast before
  `fail()`.
- A `@SuppressWarnings` javac stays silent about is still alive when the element holds a raw cast, a raw
  `instanceof` or a raw type argument. Keys javac does not know (`unused`, `resource`, `squid:*`, Error Prone
  names) are IDE or Sonar keys, judged by that tool.
- Reflection fixtures asserted by name: epbds6830 BeanA.getAB, AOpenClassTest.getC, JavaOpenClassTest.gg, MyProp
  fields in a binary .xls, YamlMapperFactoryTest transients, InterfaceTransformerTest.TestInterface.
- Bundle conventions: ValidationMessages `openl.error.<status>.<code>.message` (code composed in Java), sql-errors
  keyed by vendor error code, `openl-default.properties` keys composed as `repo-<id>.` and `$ref` indirection —
  the documented `repository.archive.*` and `repository.design.*` are that composition, not dead keys.
- Resource stems alive by convention: Flyway `db/flyway/**`, `META-INF/openl/extension-*.xml`,
  `openl-db-repository-<code>.properties`, `rapi-doc/rapidoc-min.js`, `site.webmanifest` icons,
  `META-INF/maven/archetype-metadata.xml`, `compose.override.example.yaml`.
- dependency:analyze FPs: a module declaring no `<dependencies>` still gets findings from its parent; inherited
  test harness (junit-jupiter, junit-pioneer, mockito-junit-jupiter, log4j-to-slf4j are ~294); provided
  annotations and processors (jspecify, lombok); runtime providers named from configuration (the five in
  security.standalone via `security-hibernate-beans.xml`; maven-scm; cxf-rt-features-logging; Azure's jackson
  dataformats and reactor-core); aggregators; wars and jdbc drivers an ITEST needs only to boot a server.
- A managed entry no pom declares is a transitive version pin: judge it by `dependency:tree -Dverbose -Pitest`
  over all modules, not by declaration. An `exclusion` is judged by resolving its parent alone in a scratch pom.
- A class named by string composition has no textual reference at all: `OperationFactory` builds the 13 TBasic
  operations as package + `conversionStep.getOperationType()` + `Operation`. Search a name MINUS a common suffix.
- An enum whose `values()` is iterated keeps every constant alive. To prove one dead, show it is never stored and
  never returned, then that the `values()` loop is a no-op for it.
- An npm dependency is invoked from `package.json` `scripts` or read implicitly by tsc (`@types/*`); exclude the
  lockfile from the search, never package.json itself.
- An identifier index keyed on `[A-Za-z_$][\w$]*` misses a file stem starting with a digit: confirm with `git grep -lF`.
- A dotted-name detector over Markdown catches heading anchor slugs and wrapped table cells, not settings: require
  dot separators only, drop hyphenated slugs, and rejoin a name split across a line wrap.
- Jekyll lists pages by `nav: "auto"` and `migration-notes.md`, so a link-graph orphan check calls 265 live pages
  orphans. Site-absolute links resolve on the published site, not on disk; only a wrong-case path really breaks.
- A backticked path in documentation is no claim about a real file when it is elided (`STUDIO/.../Foo.java`) or
  names a build output under `target/`; filter both before reporting a path broken. A documented
  `mvn <prefix>:<goal>` is likewise alive when the plugin's artifactId does not follow `<prefix>-maven-plugin`:
  `dependency-check-maven` answers `dependency-check:`. Resolve the prefix, never guess the artifactId.
- The clone is SHALLOW (~50 commits) whose root adds every file, so `git log -S` answers that root for every
  string. Never claim when something was removed here; prove absence at HEAD instead.

## Method rules

- Build the whole repo once per run: `LANG=C.UTF-8 mvn clean install -Dquick -DnoPerf -T2
  -Daether.syncContext.named.time=600`. Unset `gpg.format` and `commit.gpgsign` globally first or JGit tests die.
- Build one identifier-frequency index over the whole tree once (regex `[A-Za-z_$][\w$]*` per file, into a
  Counter) and answer every "is this name used" question from it: 2 seconds, versus hours of per-name scanning.
  A name whose total count equals its declaration count is unreferenced.
- Never name a scratch script after a stdlib module.
- PMD needs reactor artifacts and a warm `~/.m2`, so run it online and fully qualified (a `pmd:` prefix fails
  offline): `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only -Pitest
  -fae -T2 -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`, with the plugin under root `<build><plugins>` and the ruleset
  at `${maven.multiModuleProjectDirectory}/.toDelete/`; parse every `target/pmd.xml`, then restore the pom.
- Bytecode scan: ~120-line ASM program (asm 9.10.1) over every `target/classes` and `target/test-classes`
  recording invocations, field access, method handles, invokedynamic args and `ldc` strings; drop annotated
  members, overrides and names in literals, then apply the constant-inlining and Lombok filters above.
- Chain the install and PMD in one detached `setsid nohup` script touching a DONE file; poll the file, never the
  log's last line. Anchor any `pkill -f 'name[.]py'` or it matches the calling shell's own command line. Meanwhile
  run only read-only detectors: never edit the working tree or rebase while Maven is running.
- A class named only by a container registration (`web.xml`, `@WebFilter`) is NOT proven alive by it: judge it by
  whether its behaviour is reachable. A `<listener>` serves only the interfaces the container sorts it into, so a
  registered-but-unbound one never fires — verify from the container jar with `javap -c`, never from the spec.
- A migration or upgrade commit is worth checking for orphans; EPBDS-14123 and the Groovy 6 bump were both clean.
- Documentation still pays, but both of its cross-checks are SPENT for deletions: every `org.openl.*` token in a
  guide resolves at HEAD or sits in Human follow-ups, and no guide names a property a release note marks
  **Removed**. Re-run either only over new Docs commits; what is left needs a rename this routine may not make.
- Cross-check Docs against what the build produces, not only against types: every `<artifactId>` and
  `org.openl.rules:<id>` token in a guide against the 198 reactor artifactIds finds a stale coordinate in one pass.
- Public API deferred under rail 8.2 is worth naming explicitly in the PR body: the maintainer approved removing
  UserWorkspace.passivate() straight off that 'Deliberately kept' line. Deferring is not dropping.
- Prove non-reference with `grep -rIwF <name>` over tracked files plus `grep -raF` for binaries and `unzip -p` for
  workbooks (a `.xls` as latin-1 and UTF-16 bytes; documentation case-insensitively). Use `git ls-files`, never a
  raw `grep -r`: untracked `STUDIO/studio-ui/dist/` otherwise answers every query.
- A docs tree reached only through a directory link (`README.MD` → `examples/` → `index.md` → subfolder) is
  alive: count a link to the PARENT directory, not just to the file, before calling a Docs page unreferenced.
- Removing members is a fixpoint: re-check fields, helpers, constructor parameters and imports it orphaned.
  SonarCloud's "new issues" lists exactly those; read after every push, no auth, at
  `sonarcloud.io/api/issues/search?componentKeys=org.openl.rules:openl-tablets&pullRequest=N&sinceLeakPeriod=true`.
- Stage every commit by explicit path (`git add -- <files>`) or a `git rm` staged earlier rides into it.
- Frontend gate: `npx tsc --noEmit --noUnusedLocals --noUnusedParameters`, `npx vitest run <area>`. Both need
  `node_modules`, which the reactor build populates; run them after it, never beside it.
- CodeRabbit's `Docstring Coverage` scores only functions inside touched hunks: it PASSES a diff touching none,
  fails when removed hunks hold functions. Decline that case by comment citing `git diff -U0`.
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
- Root exclusions on httpclient, azure-storage-blob, hibernate-validator, swagger-parser and poi-ooxml-lite all
  still remove a transitive artifact (verified by scratch-pom resolution).
- A parked call that carries its own rationale comment is a documented decision, not dead code:
  `ResultExport.validateMergedRegions` (EPBDS-7848), `trackAllColumnsForAutoSizing`, the `intern()` TODO in
  RuleRowHelper, the bulk OpenAPI rewrite block in ITEST `HttpClient`.
- webstudio `SessionListener` is alive: it implements HttpSessionListener and HttpSessionIdListener, which the
  container dispatches, and drives the session cache, Spring security events and `WebStudio.destroy()`.
- The Hibernate `Tag`, `TagType` and `TagTemplate` entities and their DAOs stay: the tag catalogue, templates and
  the Studio tag admin still read them. Only the `OpenLProject` entity went with the JDBC tag migration.

## CI flakes

- IT (studio-acl): `OracleRdbmsTest.upgrade` fails "Failed requests: expected 0 but was N" with `ORA-12516` while
  the other vendors pass. Oracle Free container limit, not the diff; one rerun clears it.
- IT (services-data), HIGH RATE: `apache/kafka-native:latest` exits code 1 in its own `setup`, GraalVM segfault at
  `Pwd.getpwuid`; Testcontainers then times out on "RECOVERY to RUNNING". The victim ROTATES between suites — a
  rotating victim proves the flake, a regression kills the same suite every time. Budget two reruns per SHA.
- Before calling a failure a tag regression, check whether another PR ran the same job in the same window; the
  base `Build` workflow is a multi-JDK matrix red since August.
- Tests (without ITEST), studio-ui: `ModuleWorkspace.test.tsx` times out in `waitFor` only on a loaded runner. The
  tell is a DOM dump still showing `browser.compile.compiling` and a vitest wall time near 860 s against the 20 s
  per-test CI ceiling, plus a failing set that SHRINKS between attempts. Never push a vitest change to chase it;
  a new SHA is the cheapest cure.
- `Sonar analysis` is skipped when any job fails and lands ~10 min after the last one, so the issues API answers 0
  for "never analysed". Confirm the analysed SHA and date at `project_pull_requests/list` before trusting a 0.
- `rerun_failed_jobs` returns 403 while any job is in flight and re-reads the same jacoco artifacts, so it cannot
  cure `Sonar analysis` dying in `report-aggregate` with "Unknown block type N" — which needs no crashed attempt and
  has hit an otherwise-green run. Cure that with `rerun_workflow_run`, confirmed to turn such a run's gate clean.
- Fetch a job log with `get_job_logs` (tail 8000); find failures with `... - FAIL`, the cause with `ORA-|expected: <`.

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

- ALL 14 change types re-swept repo-wide over 86 reactor modules, studio-ui, Docs and DEMO with zero findings:
  commented-out code, PMD 5 rules, ASM member scan, whole-type scan, the identifier index, resources and images,
  bundles and locales, config defaults, TS exports plus `tsc --noUnusedLocals --noUnusedParameters`,
  dependency:analyze-only, pom properties, managed entries, exclusions, managed plugins, `@SuppressWarnings`,
  constant guards, `.editorconfig`, `.gitignore`, the Docs page graph and DEMO css.
- Closed at zero: duplicate sibling entries in 662 non-pom XML and 238 JSON files, duplicate keys across 119
  properties and ignore files, per-package logger categories (the three log4j2 configs declare none), image
  references (608 tracked, every basename present in text), and path-bearing elements over 209 poms. The
  duplicate-entry scan paid once, on three identical `**/*.sql` Spotless includes in the root pom.
- Veins probed and closed at zero: Maven profiles (11), npm dependencies (45), orphaned `package-info.java`, empty
  tracked files, production types referenced only from tests (62), container registrations (web.xml and all 8
  `@WebFilter`/`@WebServlet`), JSF-era orphans (no `.xhtml` or faces dependency anywhere), pom file-path references
  (destination paths and archetype velocity tokens), Spring XML beans (15 files, all type-injected or scanned),
  duplicate dependency/plugin/module/property declarations across 209 poms, dependencies a parent declares,
  servlet init-params, exact-duplicate tracked files (377 groups, each a test project's own fixture),
  `@Deprecated` members unreferenced outside their file (8, published API or Spring handlers), and studio-ui npm
  scripts and config files. Only the enum-constant, documentation and build-config veins have ever paid.

## Human follow-ups

- Guides naming a class absent at HEAD; each needs a rename this routine may not make: `MixInClassFor` (real
  `MixInClass`) and `kafka.ser.MessageDeserializer` (real `RequestMessageDeserializer`) in
  rule-services/configuration.md, `SkipFaultStoreLogData` (real `SkipFault`) in advanced-configuration.md, and
  `org.openl.rules.table.TableNotFoundException` in onboarding/troubleshooting.md, which never existed.
- `Docs/developer-guide/index.md` builds its whole Debugging section on `mvn jetty:run`, and no pom declares
  jetty-maven-plugin; its Logging section names `src/main/resources/log4j2.xml` and `test-resources/log4j2.xml`
  against the real `resources/log4j2.properties`. onboarding/development-setup.md and onboarding/codebase-tour.md
  repeat the `jetty:run` instruction, and the same page documents `mvn rewrite:run` with no OpenRewrite plugin.
- `Docs/user-guides/rule-services/configuration.md` line 811 tells the reader to fetch
  `org.openl.rules:org.openl.rules.ruleservice.ws.full:war`, which no module builds; the full war is
  `...ruleservice.ws.all` (final name `webservice-all`). The command cannot resolve. A rename, so not this routine.
- `ruleservice.store.logs.enabled` is documented as the global switch for logging to external storage, and NO code
  reads it; only `ruleservice.store.logs.db.enabled` gates the one surviving backend. Either the guide is stale or
  the global gate was lost when the cassandra and elasticsearch backends went. A maintainer decides which.
- `Docs/DEPLOYMENT.md` (22 of 81 properties), `Docs/API_GUIDE.md` (6 of 10) and `Docs/TROUBLESHOOTING.md` (3 of 28)
  document settings no code reads; API_GUIDE and `Docs/api/public-api-reference.md` document `/admin/*` and
  `/api/projects/*/git/*` endpoints no controller maps. Generated-looking pages; editorial, not a sweep.
- `Docs/developer-guides/externalized-config.md` illustrates `-D` syntax with
  `ruleservice.datasource.filesystem.supportDeployments`, removed in 5.24.0. Pick a live property for the example.
- 51 relative links in `Docs/` resolve to nothing: `/DEV/CLAUDE.md` and friends from `Docs/README.MD`, plus
  lowercase `/docs/...` paths that 404 on a case-sensitive server.
- `OpenAPIConverterTest` (640 lines) and `RulesDeployerServiceTest` (354 lines) carry a bare class-level `@Disabled`
  while the code they cover is live. Restore or delete is a maintainer's call; deleting them drops real coverage.
- `CorsFilter` is registered twice (`@WebFilter("/*")` and web.xml), so it runs twice and doubles each
  `Access-Control-*` header. Latent while `cors.allowed.origins` is unset.
- `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` of the 17 flyway/common scripts, and nothing
  sets `sqlMigrationPrefix`, so Flyway's case-sensitive default `V` skips it: version 14 is absent from every
  deployed schema and the ExternalGroups index is never created. A rename, so not this routine's work.
- ITEST pulls `apache/kafka-native:latest` in three suites; pinning it or moving to `apache/kafka:4.3.1` would
  stop its startup flake costing reruns. ORA-12516 in IT (studio-acl) needs a real fix in the Oracle container
  setup, and `ModuleWorkspace.test.tsx` would be steadier with the CI `testTimeout` raised from 20_000.
- `RulesUtilsTest.testParseFormattedDouble` suppresses `"deprecated"`, a key javac ignores, while both methods it
  calls are deprecated. The fix is the key `deprecation` — a rename, so not this routine's.
- Swapping `org.openl:x-forwarded-filter` for Spring's `ForwardedHeaderFilter` is BLOCKED: the root pom documents
  the opposite decision and both call sites need `xForwardedPrefixStrategy=PREPEND`, which Spring always REPLACES.
- `Docs/examples/production/` and `Docs/production-deployment/` are near-identical 320K copies, both reachable.
- KafkaMessageHeader.Type.PRODUCER_RECORD is documented as usable, but StoreLogDataMapper acts only on
  CONSUMER_RECORD. The mapper or the guide is wrong; the constant is user-written API and stays either way.

## Run log

- 2026-09-21: delta was 2 dependabot bumps and a clean tag migration; 6 new code veins closed at zero, the
  documentation veins paid 1 removal and 7 follow-ups. PR #2145 MERGED (-2) and its branch deleted.
- 2026-09-22: swept the 209-file EPBDS-16692/16660/16661/16662 delta at zero; 4 new veins closed, build config
  paid 1 removal (PR #2152) and the Docs-artifact cross-check paid 1 follow-up.
- 2026-09-23: rebased #2152 onto main past Groovy 6 and JGit 7.8; swept the 183-file EPBDS-16664..16667 delta at
  zero (PMD 40, all generated or known FPs). Duplicate-config and doc-path veins paid 2 removals and 3 follow-ups;
  #2152 MERGED (-4) the same day and its branch deleted.
