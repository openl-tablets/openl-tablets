# Dead-code sweep ledger — openl-tablets

## Resume point

- PR #2166 is open on `dead-code/lombok-noop-annotations` (1 commit, -8). Drive it to green, then continue.
- All 14 change types are exhausted repo-wide. A run is: maintain the open PR, sweep the delta (expect zero), spend
  the rest on a NEW vein. The paying veins are documentation, build config and framework-generated members: a
  no-op annotation or a generated member no caller names is the one CODE shape still paying.
- Start the reactor build detached in the FIRST minute and mine read-only veins beside it; before every push, list
  open `dead-code/*` PRs and re-fetch main, since parallel runs of this routine share the branch.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | done; 7 blocks, all prose or parked calls with a rationale |
| 2 | Never-read assignments, dead stores | done; 14 PMD hits, all documented FPs |
| 3 | Unused locals, private fields/methods/params | done; 14 PMD hits, all documented FPs |
| 4 | Unused Maven dependency declarations | done; 537 analyze hits + npm deps, all FPs |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | done; 6 hits, all plugin-read flags |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | in-progress; 4 no-op Lombok annos in #2166 |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 220 candidates, 0 unreferenced |
| 8 | CSS rules and inline styles | done; 1 file, 4 selectors, all used |
| 9 | Legacy JS functions and pages | done; 0 `.xhtml` remain, only keep-listed vendor JS |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done; 1,583 keys, 113 flagged, all template-resolved |
| 11 | TypeScript exports, types, components, imports | done; 1,151 exports, 0 dead |
| 12 | Test fixtures: workbooks, utility classes, stub members | done |
| 13 | Package-private/protected members and unreferenced internal classes | done; 1,025 raw hits, 0 survivors |
| 14 | Documentation of settings and classes the code no longer has | done; 1 removal, merged in #2145 |

## Open PR

- #2166, branch `dead-code/lombok-noop-annotations`, head da93de2f2d, cut from main 4513cebb5e.
- Commit 1 (change type 6): drop the Lombok annotations that generate a member nothing calls — @Slf4j on
  OpenApiGenerator, @Builder on FileChange and PendingChanges, @RequiredArgsConstructor on
  ProjectComparisonService. 4 files, -8. No review thread yet.

## Merged PRs

- #2120 (-487), #2129 (-1), #2134 (-87, SessionTimeoutFilter), #2135 (-54, passivation), #2145 (-2), #2152 (-4).
- A removal proven by unreachable behaviour, not by non-reference, is accepted on that evidence alone.
- The maintainer does not merge a sweep PR red: they rebase it onto new main, wait for green, then rebase-merge.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type; nothing open.

## Deferred findings

- ~599 public members and ~35 public types across the DEV, STUDIO and WSFrontend jars are unreferenced in bytecode,
  write-only Lombok setters among them; all published API, kept under rail 8.2, and the ASM scan below re-derives
  the list on demand. Examples: `XlsProjectionType` (4 of 12 constants built, under its own TODO),
  `EventOfInterestConstants.MINMAX`, `MergeResult.status`.
- `org.eclipse.jetty:jetty-home` is in no dependency tree and declared by no pom (DEMO fetches Jetty by
  `jetty.version`); confirm nothing resolves it before dropping it.
- `.gitattributes` keeps `**/openl-repository/workspace/**/*.xml text eol=lf` with no such path; a repoint onto the
  ITEST `openl-repository` XML that exists needs a human.

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
- A generated member can be reached where the generator is not: `@SuperBuilder` on an abstract parent serves its
  subclasses' `builder()`, a Spring `@Bean` is collected by its framework interface (ViewResolver), and a
  package-visible `LOG` is read as `Owner.LOG.x`, so a same-file search sees neither.
- An indent-anchored regex reads a nested class's members as the outer class's: it then calls a class-level
  `@Getter` or `@RequiredArgsConstructor` fieldless. Anchor on the declaration, and read `final @Nullable T x`.
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
  -Daether.syncContext.named.time=600` — ~25 min from a cold `~/.m2`. Unset `gpg.format`/`commit.gpgsign` first.
- Index the whole tree once (regex `[A-Za-z_$][\w$]*` per file into a Counter, ~8 s) and answer every "is this name
  used" question from it; a name whose total count equals its count in its own file is unreferenced.
- PMD needs reactor artifacts and a warm `~/.m2`, so run it online and fully qualified (a `pmd:` prefix fails):
  `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only -Pitest -fae -T2
  -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`, plugin under root `<build><plugins>`, ruleset at
  `${maven.multiModuleProjectDirectory}/.toDelete/`; parse every `target/pmd.xml`, then restore the pom.
- Bytecode scan: ~120-line ASM program (asm 9.10.1) over every `target/classes` and `target/test-classes` recording
  invocations, field access, method handles, invokedynamic args and `ldc` strings; drop annotated members,
  overrides and names in literals, then apply the constant-inlining and Lombok filters above.
- Chain install and PMD in one detached `setsid nohup` script touching a DONE file; poll the file, never the log's
  tail. Anchor any `pkill -f 'name[.]py'`. Never edit the working tree or rebase while Maven runs.
- A container registration (`web.xml`, `@WebFilter`) does NOT prove a class alive: judge whether its behaviour is
  reachable. A `<listener>` serves only the interfaces the container sorts it into, so a registered-but-unbound one
  never fires — verify from the container jar with `javap -c`, never from the spec.
- Both Docs cross-checks are SPENT for deletions: every `org.openl.*` token in a guide resolves at HEAD or sits in
  Human follow-ups, and no guide names a property a release note marks **Removed**. Re-run them only over new Docs
  commits; what is left needs a rename this routine may not make.
- Cross-check Docs against what the build produces, not only types: every `<artifactId>` and `org.openl.rules:<id>`
  token in a guide against the reactor artifactIds finds a stale coordinate in one pass.
- Public API deferred under rail 8.2 is worth naming explicitly in the PR body: the maintainer approved removing
  UserWorkspace.passivate() straight off that 'Deliberately kept' line. Deferring is not dropping.
- Prove non-reference with `grep -rIwF <name>` over tracked files, `grep -raF` for binaries and `unzip -p` for
  workbooks (a `.xls` as latin-1 and UTF-16; documentation case-insensitively). Drive it from `git ls-files`, or
  untracked `STUDIO/studio-ui/dist/` answers every query.
- A docs tree reached only through a directory link (`README.MD` → `examples/` → `index.md`) is alive: count a link
  to the PARENT directory before calling a Docs page unreferenced.
- Removing members is a fixpoint: re-check the fields, helpers, parameters and imports it orphaned. SonarCloud's
  "new issues" lists exactly those, no auth, after every push, and must read ZERO before the PR is done:
  `sonarcloud.io/api/issues/search?componentKeys=org.openl.rules:openl-tablets&pullRequest=N&sinceLeakPeriod=true`.
- Stage every commit by explicit path (`git add -- <files>`) or a `git rm` staged earlier rides into it.
- Frontend gate: `npx tsc --noEmit --noUnusedLocals --noUnusedParameters`, `npx vitest run <area>`. Both need
  `node_modules`, which the reactor build populates; run them after it, never beside it.
- CodeRabbit's `Docstring Coverage` scores only functions inside touched hunks: it PASSES a diff touching none,
  fails when removed hunks hold functions. Decline that case by comment citing `git diff -U0`.
- Verify `git log -1 --pretty='%an <%ae> | %cn <%ce>'` BEFORE pushing; rail 8.4 bars force-pushing
  `dead-code/ledger`, so a wrong identity there cannot be repaired.

## Keep-list

- OpenL datatype beans and rules interfaces in tests are bound from Excel by property or method name
  (IChildBean.getMyBean, Tutorial4Interface.getTheft_rating, Location setters, RulesUtilsTest.testFlatten).
- Jackson-bound webstudio models keep every accessor: RepositorySettings, AWSS3RepositorySettings,
  GitRepositorySettings, `*Append`, SettingValueWrapper, SupportedFeaturesModel.
- Convention files: Flyway migrations, `META-INF/openl/extension-*.xml`, `openl-db-repository-*.properties`, static
  rapi-doc, site.webmanifest icons, `META-INF/services/**`, ITEST `application-*.properties` (Spring profiles),
  archetype `archetype-metadata.xml`, `compose.override.example.yaml`.
- Config defaults in `openl-default.properties` are documented in Docs guides and composed at runtime; all alive.
- `DEMO/webstudio.properties` and `DEMO/webservice.properties` are named nowhere: ApplicationPropertySource reads
  `file:{appName}.properties` from the working directory, and DEMO deploys those two context names.
- Demo workbooks under `org.openl.rules.demo/src/**` and `webstudio/test/rules/decisionTableIndexes/` load by folder.
- `.gitignore` and `.gitattributes` entries for a file TYPE stay even when no such file is tracked — they are
  prophylactic. Only a PATH-specific rule naming a directory that does not exist is a candidate.
- Root exclusions on httpclient, azure-storage-blob, hibernate-validator, swagger-parser and poi-ooxml-lite each
  still remove a transitive artifact (scratch-pom resolution).
- A parked call that carries its own rationale comment is a documented decision, not dead code:
  `ResultExport.validateMergedRegions` (EPBDS-7848), `trackAllColumnsForAutoSizing`, the `intern()` TODO in
  RuleRowHelper, the bulk OpenAPI rewrite block in ITEST `HttpClient`.
- webstudio `SessionListener` is alive: it implements HttpSessionListener and HttpSessionIdListener, which the
  container dispatches, and drives the session cache, security events and `WebStudio.destroy()`.
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
- `Sonar analysis` is skipped when any job fails and lands ~10 min after the last, so the issues API answers 0 for
  "never analysed": confirm the analysed SHA at `project_pull_requests/list` before trusting a 0.
- `rerun_failed_jobs` returns 403 while any job is in flight and re-reads the same jacoco artifacts, so it cannot
  cure `Sonar analysis` dying in `report-aggregate` with "Unknown block type N" — which needs no crashed attempt and
  has hit an otherwise-green run. Cure that with `rerun_workflow_run`, confirmed to turn such a run's gate clean.
- Fetch a job log with `get_job_logs` (tail 8000); find failures with `... - FAIL`, the cause with `ORA-|expected: <`.

## Container facts

- No `gh` CLI: GitHub MCP tools only (pull_request_read, update_pull_request, add_issue_comment, actions_list
  list_workflow_jobs, actions_run_trigger rerun_failed_jobs with the workflow run id from a check's html_url).
- Container presets `gpg.format=ssh` and `commit.gpgsign=true` globally (unset both) but no `GIT_AUTHOR_*`
  variables; a plain `git config --global user.*` holds, and passing the identity inline as well costs nothing.
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
  `@Deprecated` members unreferenced outside their file (8, published API or Spring handlers), studio-ui npm
  scripts and config files, poms no parent declares as a module (27, invoker `it/`, archetype resources, Docs
  examples), test-bearing classes surefire would not select, exception types never instantiated, `@Bean` methods
  nothing names, listPageTheme palette tokens, Docker/compose environment variables, dependabot entries, and the
  Jekyll navigation and plugin list. The `.aj`/`.apt`/`.scss` Spotless includes for types the repo has no file of
  are prophylactic and KEPT — settled. Only the enum-constant, documentation, build-config and no-op-annotation
  veins have ever paid.

## Human follow-ups

- Guides naming a class absent at HEAD; each needs a rename this routine may not make: `MixInClassFor` (real
  `MixInClass`) and `kafka.ser.MessageDeserializer` (real `RequestMessageDeserializer`) in
  rule-services/configuration.md, `SkipFaultStoreLogData` (real `SkipFault`) in advanced-configuration.md, and
  `org.openl.rules.table.TableNotFoundException` in onboarding/troubleshooting.md, which never existed.
- `Docs/developer-guide/index.md` builds its Debugging section on `mvn jetty:run` (no jetty-maven-plugin) and names
  `src/main/resources/log4j2.xml` against the real `resources/log4j2.properties`; onboarding/development-setup.md
  and onboarding/codebase-tour.md repeat `jetty:run`, and one documents `mvn rewrite:run` with no plugin.
- `Docs/user-guides/rule-services/configuration.md` line 811 fetches `org.openl.rules.ruleservice.ws.full:war`,
  which no module builds; the real one is `...ws.all` (final name `webservice-all`). A rename, so not this routine.
- `ruleservice.store.logs.enabled` is documented as the global switch for external-storage logging and NO code reads
  it; only `...db.enabled` gates the surviving backend. Stale guide, or a gate lost with the cassandra and
  elasticsearch backends — a maintainer decides.
- `Docs/DEPLOYMENT.md` (22 of 81 properties), `API_GUIDE.md` (6 of 10) and `TROUBLESHOOTING.md` (3 of 28) document
  settings no code reads; API_GUIDE and `api/public-api-reference.md` map `/admin/*` and `/api/projects/*/git/*`
  to no controller. Generated-looking pages; editorial, not a sweep.
- `Docs/developer-guides/externalized-config.md` illustrates `-D` with
  `ruleservice.datasource.filesystem.supportDeployments`, removed in 5.24.0; pick a live property.
- 51 relative links in `Docs/` resolve to nothing: `/DEV/CLAUDE.md` and friends from `README.MD`, plus lowercase
  `/docs/...` paths that 404 on a case-sensitive server.
- `OpenAPIConverterTest` (640 lines) and `RulesDeployerServiceTest` (354 lines) carry a bare class-level `@Disabled`
  over live code. Restore or delete is a maintainer's call.
- `CorsFilter` is registered twice (`@WebFilter("/*")` and web.xml) and doubles each `Access-Control-*` header;
  latent while `cors.allowed.origins` is unset.
- `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` of the 17 flyway/common scripts and nothing sets
  `sqlMigrationPrefix`, so Flyway's default `V` skips it: the ExternalGroups index is never created. A rename.
- Flake fixes a human could make: pin ITEST's `apache/kafka-native:latest` (3 suites) or move to
  `apache/kafka:4.3.1`; fix ORA-12516 in the IT (studio-acl) Oracle container; raise the CI vitest `testTimeout`
  from 20_000 for `ModuleWorkspace.test.tsx`.
- `RulesUtilsTest.testParseFormattedDouble` suppresses `"deprecated"`, which javac ignores, while both methods it
  calls are deprecated; the key should be `deprecation`. A rename.
- Swapping `org.openl:x-forwarded-filter` for Spring's `ForwardedHeaderFilter` is BLOCKED: the root pom documents
  the opposite decision, and both call sites need `xForwardedPrefixStrategy=PREPEND`; Spring always REPLACES.
- `Docs/examples/production/` and `Docs/production-deployment/` are near-identical 320K copies, both reachable.
- KafkaMessageHeader.Type.PRODUCER_RECORD is documented as usable but StoreLogDataMapper acts only on
  CONSUMER_RECORD; the mapper or the guide is wrong. The constant is user-written API and stays.

## Run log

- 2026-09-22: swept the 209-file EPBDS-16692/16660/16661/16662 delta at zero; 4 new veins closed, build config
  paid 1 removal (PR #2152) and the Docs-artifact cross-check paid 1 follow-up.
- 2026-09-23: rebased #2152 onto main past Groovy 6 and JGit 7.8; swept the 183-file EPBDS-16664..16667 delta at
  zero (PMD 40, all generated or known FPs). Duplicate-config and doc-path veins paid 2 removals and 3 follow-ups;
  #2152 MERGED (-4) the same day and its branch deleted.
- 2026-09-24: swept the 140-file EPBDS-16668..16702 delta at zero; 9 new veins closed. The Lombok no-op vein paid
  4 removals, opened as PR #2166 (-8). Near-miss avoided: the two DEMO `.properties` are keep-listed.
