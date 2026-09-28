# Dead-code sweep ledger — openl-tablets

## Resume point

- No PR is open and the 2026-09-28 run added none. Cut a fresh `dead-code/*` branch from a freshly fetched
  `origin/main` the moment a finding is proven.
- All 14 change types are exhausted repo-wide. A run is: maintain the open PR, sweep the delta (expect near zero),
  spend the rest on a NEW vein. Only documentation, build config, i18n keys and dead TS imports have ever paid.
- Start the reactor build detached in the FIRST minute (~40 min cold) and mine read-only veins beside it; before
  every push, list open `dead-code/*` PRs and re-fetch main — parallel runs share the branch.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | done; 7 blocks, all prose or parked calls with a rationale |
| 2 | Never-read assignments, dead stores | done; 14 PMD + 3 Error Prone hits, all documented FPs |
| 3 | Unused locals, private fields/methods/params | done; 14 PMD + 4 Error Prone hits, all FPs |
| 4 | Unused Maven dependency declarations | done; 537 analyze hits + npm deps, all FPs |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | done; 6 hits, all plugin-read flags |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done; 4 no-op Lombok annotations, in #2166 |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 220 candidates, 0 unreferenced |
| 8 | CSS rules and inline styles | done; 1 file, 4 selectors, all used |
| 9 | Legacy JS functions and pages | done; 0 `.xhtml` remain, only keep-listed vendor JS |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done; 6 dead keys merged in #2184; re-run per delta |
| 11 | TypeScript exports, types, components, imports | done; 0 dead exports, 1 dead import merged in #2184 |
| 12 | Test fixtures: workbooks, utility classes, stub members | done |
| 13 | Package-private/protected members and unreferenced internal classes | done; 1,025 raw hits, 0 survivors |
| 14 | Documentation of settings and classes the code no longer has | done; 1 removal, merged in #2145 |

## Open PR

- None. Open the next one as soon as a finding is pushed, ready for review, and record it here.

## Merged PRs

- #2120 (-487), #2129 (-1), #2134 (-87), #2135 (-54), #2145 (-2), #2152 (-4), #2166 (-8), #2184 (-7). Each merged
  the day it opened, with no review comment, on the PR body's evidence alone.
- A removal proven by unreachable behaviour, not by non-reference, is accepted. The maintainer never merges a sweep
  PR red: they rebase onto new main, wait for green, then rebase-merge; the head branch auto-deletes on merge.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type; nothing open.

## Deferred findings

- ~599 public members and ~35 public types across the DEV, STUDIO and WSFrontend jars are unreferenced in
  bytecode, write-only Lombok setters among them; all published API kept under rail 8.2, re-derived by the ASM scan.
- `org.eclipse.jetty:jetty-home` is in no dependency tree and declared by no pom (DEMO fetches Jetty by
  `jetty.version`). `.gitattributes` keeps `**/openl-repository/workspace/**/*.xml` with no such path; both need
  a human.

## False-positive shapes

- A detector that picks text files by an extension allowlist silently drops whole formats: `.webmanifest` was
  missing and reported 18 live resources dead. Select text by "no NUL byte in the first 8 KB" — ~14,000 files.
- javac inlines `static final` primitive and String constants, so a bytecode scan never sees a read and calls
  every such field dead (1,097 of 1,515 raw hits). Judge constants by source text, never by bytecode.
- Lombok generates accessors that exist in bytecode but not in source, so a field whose name occurs only at its own
  declaration may still be read: search `get`/`set`/`is` + the capitalised name before calling it dead.
- i18next resolves `t(key, {count})` to `key_one`/`key_other`, which no literal names: a plural pair whose base is
  used is alive. Interpolated keys are alive too — 39 such templates exist, 7 of them under `browser.`. Harvest
  them all before judging; a leaf search misses `tests.no_${k}` and calls a live key dead.
- An exported TS type or const used only inside its own file looks unimported; that makes the `export` redundant,
  not the type dead. Removing `export` is a rename, not a deletion.
- A leading positional callback parameter from `tsc --noUnusedParameters` or PMD `UnusedFormalParameter` is never
  removable: dropping it shifts the parameter that IS used (`Array.from(…, (unused, column) => …)`).
- A pom `<properties>` entry with no `${...}` dereference may still be read by a plugin by name:
  `lombok.delombok.skip`, `archetype.test.skip`, `invoker.skip`. Check the plugin before calling it dead.
- A private or package member named in any string literal is reflective (`@MethodSource`, JAXB, OpenL datatype
  binding): filter bytecode hits on Java string literals, non-Java text and workbook strings. Search an accessor by
  its property name too — Jackson DTOs and OpenL beans bind `basePath`, not `setBasePath`. Also alive: JAXB private
  `beforeMarshal`/`afterUnmarshal`, Spring MVC handlers, record accessors, generic bridge overrides.
- A top-level type whose simple name occurs only in its own file is still alive when a framework names it: JUnit by
  file pattern, Spring by classpath scan, `@Mojo` by the plugin descriptor. 73 production types read this way.
- A generated member can be reached where the generator is not: `@SuperBuilder` on an abstract parent serves its
  subclasses' `builder()`, a Spring `@Bean` is collected by its framework interface (ViewResolver), and a
  package-visible `LOG` is read as `Owner.LOG.x`, so a same-file search sees neither.
- An indent-anchored regex reads a nested class's members as the outer class's, calling a class-level `@Getter`
  or `@RequiredArgsConstructor` fieldless; `@Builder.Default` likewise matches `@Builder`. Anchor on the
  declaration, require no dot after the annotation, read `final @Nullable T x`, and assert the text before editing.
  A `@Builder`/`@SuperBuilder` with no `Type.builder(` is still alive on an abstract parent, serving the
  subclasses' builders (FsNode), or where the type calls its own `builder()` bare (RevisionDetails).
- PMD blind spots, all recurring. UnusedAssignment: constructor early return (CellStyle), read back through a
  callback (DynamicPropertySource.settings), read by a getter (AProjectCreator), `key = null` before `System.gc()`,
  publish-before-block (DebugChannel, DebugHookImpl), re-entrancy (ServiceManagerImpl.deploy), empty catch
  (GitRepository). UnusedLocalVariable: try-with-resources locals, a counting for-each, a cast before `fail()`;
  `target/generated-sources/javacc/**` supplies 21 of the 38 hits and is off-limits.
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
  security.standalone via `security-hibernate-beans.xml`, maven-scm, cxf-rt-features-logging, Azure's jackson
  dataformats and reactor-core); aggregators; wars and jdbc drivers an ITEST only boots a server with.
- A managed entry no pom declares is a transitive version pin: judge it by `dependency:tree -Dverbose -Pitest`
  over all modules, not by declaration. An `exclusion` is judged by resolving its parent alone in a scratch pom.
- A class named by string composition has no textual reference at all: `OperationFactory` builds the 13 TBasic
  operations as package + `getOperationType()` + `Operation`. Search a name MINUS a common suffix.
- An enum whose `values()` is iterated keeps every constant alive. To prove one dead, show it is never stored and
  never returned, then that the `values()` loop is a no-op for it.
- An npm dependency is invoked from `package.json` `scripts` or read implicitly by tsc (`@types/*`); exclude the
  lockfile from the search, never package.json itself.
- An identifier index keyed on `[A-Za-z_$][\w$]*` misses a stem starting with a digit: confirm with `git grep -lF`.
- A dotted-name detector over Markdown catches heading anchor slugs and wrapped table cells, not settings: require
  dot separators only, drop hyphenated slugs, and rejoin a name split across a line wrap.
- Jekyll lists pages by `nav: "auto"` and `migration-notes.md`, so a link-graph orphan check calls 265 live pages
  orphans. Site-absolute links resolve on the published site, not on disk; only a wrong-case path breaks.
- A backticked path in documentation is no claim about a real file when it is elided (`STUDIO/.../Foo.java`) or
  names a build output under `target/`; filter both before reporting a path broken. A documented
  `mvn <prefix>:<goal>` is alive when the artifactId does not follow `<prefix>-maven-plugin`
  (`dependency-check-maven` answers `dependency-check:`): resolve the prefix, never guess the artifactId.
- A namespace-prefix scan that subtracts the `xmlns:p=` declarations from the `p:` hits calls every live prefix
  dead: `xmlns:p=` contains no `p:` token, so there is nothing to subtract. Count `p:` occurrences alone.
- A framework resource path resolves inside a LIBRARY jar, not this repo: CXF ships `META-INF/cxf/cxf.xml` and
  `cxf-servlet.xml`. Judge an unresolved `<import resource=>` or `@ImportResource` against the dependencies first.
- Error Prone's unused hits repeat the PMD blind spots: a Jackson binding class handed to an ObjectMapper is
  introspected, so its fields are the payload (JsonUtilsTest.BindingClasses), and a write-only field on a test's
  cache-key type models the production key (KeyClass.field). `EffectivelyPrivate` wants visibility, barred by 8.7.
- The clone is SHALLOW (~50 commits) whose root adds every file, so `git log -S` answers that root for every
  string. Never claim when something was removed here; prove absence at HEAD instead.

## Method rules

- Build the whole repo once per run: `LANG=C.UTF-8 mvn clean install -Dquick -DnoPerf -T2
  -Daether.syncContext.named.time=600` — ~25 min from a cold `~/.m2`. Unset `gpg.format`/`commit.gpgsign` first.
- Index the whole tree once (regex `[A-Za-z_$][\w$]*` per file into a Counter, ~8 s) and answer every "is this name
  used" question from it; a name whose total count equals its count in its own file is unreferenced.
- PMD needs reactor artifacts and a warm `~/.m2`, so run it online and fully qualified (a `pmd:` prefix fails):
  `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only -Pitest -fae -T2
  -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`, plugin under root `<build><plugins>`, ruleset under `.toDelete/` by
  `${maven.multiModuleProjectDirectory}`; parse every `target/pmd.xml`, then restore the pom.
- Bytecode scan: ~120-line ASM program (asm 9.10.1) over every `target/classes` and `target/test-classes`, taking
  invocations, field access, method handles, invokedynamic args and `ldc` strings; drop annotated members,
  overrides and names in literals, then apply the constant-inlining and Lombok filters above.
- Chain install and PMD in one detached `setsid nohup` script touching a DONE file; poll the file, never the log's
  tail. Anchor any `pkill -f 'name[.]py'`. Never edit the working tree or rebase while Maven runs.
- A container registration (`web.xml`, `@WebFilter`) does NOT prove a class alive: judge whether its behaviour is
  reachable. A `<listener>` serves only the interfaces the container sorts it into, so a registered-but-unbound one
  never fires — verify from the container jar with `javap -c`, never from the spec.
- Both Docs cross-checks are SPENT for deletions: every `org.openl.*` token in a guide resolves at HEAD or sits in
  Human follow-ups, and what is left needs a rename. Re-run only over changed pages, and check guides against what
  the build produces — `<artifactId>` and `org.openl.rules:<id>` tokens against the reactor artifactIds.
- List rail-8.2 deferrals in the PR body: the maintainer approved one removal straight off that 'Deliberately
  kept' line. Deferring is not dropping.
- Prove non-reference with `grep -rIwF <name>` over tracked files, `grep -raF` for binaries and `unzip -p` for
  workbooks (a `.xls` as latin-1 and UTF-16; documentation case-insensitively). Drive it from `git ls-files`, or
  untracked `STUDIO/studio-ui/dist/` answers every query.
- A docs tree reached only through a directory link (`README.MD` → `examples/` → `index.md`) is alive: count a
  link to the PARENT directory first. `@Tag`/surefire `<groups>` are used nowhere, so neither selects tests.
- Removing members is a fixpoint: re-check the fields, helpers, parameters and imports it orphaned. SonarCloud's
  "new issues" lists exactly those, no auth, and must read ZERO before the PR is done: `sonarcloud.io/api/issues/
  search?componentKeys=org.openl.rules:openl-tablets&pullRequest=N&sinceLeakPeriod=true`.
- Stage every commit by explicit path (`git add -- <files>`) or a `git rm` staged earlier rides into it.
- Frontend gate: `npx tsc --noEmit --noUnusedLocals --noUnusedParameters`, `npx vitest run <area>`. Both need
  `node_modules`, which the reactor build populates; run them after it, never beside it.
- i18n keys: parse each `addResourceBundle` literal into dotted paths and judge a key by its FULL path — the same
  leaf sits at several depths, so a leaf search calls every orphan alive. Clear the misses against the COMPLETE
  inventory of t(backtick) literals plus plural suffixes; what survives that is dead.
- PMD's report namespace is `report/2.0.0`, NOT the ruleset's `report_2_0_0`: the wrong one parses 0 violations out
  of a full report. Assert a non-zero total before believing a clean scan.
- CodeRabbit's `Docstring Coverage` scores only functions inside touched hunks: it PASSES a diff touching none.
  Decline the other case by comment citing `git diff -U0`.
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
  `file:{appName}.properties` from the cwd, and DEMO deploys those two context names.
- Demo workbooks under `org.openl.rules.demo/src/**` and `webstudio/test/rules/decisionTableIndexes/` load by
  folder. Root exclusions on httpclient, azure-storage-blob, hibernate-validator, swagger-parser and poi-ooxml-lite
  each still remove a transitive artifact.
- `.gitignore`/`.gitattributes` entries for a file TYPE are prophylactic and stay with no such file tracked; only
  a PATH-specific rule naming a directory that does not exist is a candidate.
- A parked call carrying its own rationale comment is a documented decision, not dead code:
  `ResultExport.validateMergedRegions`, `trackAllColumnsForAutoSizing`, RuleRowHelper's `intern()` TODO, the bulk
  OpenAPI rewrite block in ITEST `HttpClient`.
- webstudio `SessionListener` is alive: it implements HttpSessionListener and HttpSessionIdListener, which the
  container dispatches, and drives the session cache, security events and `WebStudio.destroy()`.
- The Hibernate `Tag`, `TagType` and `TagTemplate` entities and their DAOs stay; only `OpenLProject` went.

## CI flakes

- IT (studio-acl): `OracleRdbmsTest.upgrade` fails "expected 0 but was N" with `ORA-12516` while other vendors
  pass. Oracle Free container limit, not the diff; one rerun clears it.
- IT (services-data), HIGH RATE: `apache/kafka-native:latest` exits 1 in its own `setup`, GraalVM segfault at
  `Pwd.getpwuid`; Testcontainers then times out on "RECOVERY to RUNNING". The victim ROTATES between suites — a
  rotating victim proves the flake, a regression kills the same suite every run. Two reruns per SHA.
- Check if another PR ran the same job in the window: the base `Build` workflow is a multi-JDK matrix red since August.
- Tests (without ITEST), studio-ui: `ModuleWorkspace.test.tsx` times out in `waitFor` only on a loaded runner —
  tell: a DOM dump still at `browser.compile.compiling`, vitest wall time near 860 s against the 20 s per-test CI
  ceiling, a failing set SHRINKING between attempts. Never chase with a vitest change; a new SHA cures it.
- `Sonar analysis` is skipped when any job fails and lands ~10 min after the last, so the issues API answers 0 for
  "never analysed": confirm the analysed SHA at `project_pull_requests/list` before trusting a 0.
- `rerun_failed_jobs` returns 403 while a job is in flight and re-reads the same jacoco artifacts, so it cannot
  cure `Sonar analysis` dying in `report-aggregate` with "Unknown block type N" on an otherwise-green run; only
  `rerun_workflow_run` clears that, confirmed.
- Fetch a job log with `get_job_logs` (tail 8000); find failures with `... - FAIL`, the cause with `ORA-|expected: <`.

## Container facts

- No `gh` CLI: GitHub MCP tools only (create_pull_request then subscribe_pr_activity; pull_request_read,
  update_pull_request, add_issue_comment, actions_list list_workflow_jobs, actions_run_trigger rerun_failed_jobs
  with the workflow run id from a check's html_url).
- Container presets `gpg.format=ssh` and `commit.gpgsign=true` globally (unset both) but no `GIT_AUTHOR_*`
  variables; a plain `git config --global user.*` holds, and passing the identity inline as well costs nothing.
- Error Prone IS enabled and its unused checks DO fire, so `mvn clean install` is itself a free Java detector:
  grep the log for `[UnusedVariable|UnusedMethod|UnusedNestedClass|EffectivelyPrivate]` (7 hits repo-wide, all
  FPs) before paying for PMD; ignore 1,108 `[NullAway]`. javac options go in the root pom's compilerArgs only.
- 4 cores, 15 GB RAM: `-T2` for the reactor. `npx tsc` runs fine beside Maven, vitest does not.
- `.toDelete/` is gitignored: keep the PMD ruleset, scratch poms and detector scripts there. `~/.m2` and
  `STUDIO/studio-ui/node_modules` start COLD in a fresh container — check before budgeting.
- Edit the ledger through `git worktree add` on `origin/dead-code/ledger`, never by switching the sweep branch.
- The clone holds ~50 commits, under one day's delta: `git fetch --deepen=120 origin main` reaches the boundary.

## Exhausted veins

- ALL 14 change types re-swept repo-wide over 86 reactor modules, studio-ui, Docs and DEMO at zero, by: PMD 5
  rules, Error Prone, the ASM member and whole-type scans, the identifier index, `tsc --noUnusedLocals
  --noUnusedParameters`, dependency:analyze-only, and hand passes over resources, images, bundles, locales, config
  defaults, pom metadata, `@SuppressWarnings`, `.editorconfig`, `.gitignore`, the Docs page graph and DEMO css.
- Closed at zero: duplicate sibling entries in 662 non-pom XML and 238 JSON files, duplicate keys across 119
  properties and ignore files, per-package logger categories, 608 image references and path-bearing elements over
  209 poms. The duplicate-entry scan paid once, on three `**/*.sql` Spotless includes in the root pom.
- Lombok no-ops: @Slf4j, @Builder/@SuperBuilder, @RequiredArgsConstructor, @Jacksonized and @Builder.Default are
  swept clean repo-wide (4 paid in #2166); @Getter/@Setter and the behavioural ones are NOT sweepable.
- studio-ui locales are NOT exhausted: a UI rewrite orphans keys, and 6 fell in #2184. Re-run the i18n pass over
  every delta touching studio-ui.
- Veins probed and closed at zero: Maven profiles, npm dependencies, orphaned `package-info.java`, empty tracked
  files, production types referenced only from tests, container registrations (web.xml and all 8 `@WebFilter`
  /`@WebServlet`), JSF-era orphans, pom file-path references, Spring XML beans, duplicate declarations across 209
  poms, dependencies a parent declares, servlet init-params, exact-duplicate tracked files (377 groups, each a
  test project's own fixture), unused XML namespace prefixes, surefire `systemPropertyVariables`, every Spring
  `base-package` and `<import resource=>`/`@ImportResource` target, log4j2 appenders, JUnit `@Tag` and surefire
  `<groups>`, `lombok.config`, Dockerfile stages and compose services and volumes, `@Deprecated` members
  unreferenced outside their file, studio-ui npm scripts and config files, poms no parent declares as a module,
  test-bearing classes surefire would not select, exception types never instantiated, `@Bean` methods nothing
  names, listPageTheme palette tokens, Docker/compose environment variables, dependabot entries, the Jekyll
  navigation and plugin list. The `.aj`/`.apt`/`.scss` Spotless includes are prophylactic and KEPT — settled.

## Human follow-ups

- Docs renames this routine may not make: `MixInClassFor`→`MixInClass`, `kafka.ser.MessageDeserializer`→
  `RequestMessageDeserializer`, `...ruleservice.ws.full:war`→`...ws.all` (rule-services/configuration.md),
  `SkipFaultStoreLogData`→`SkipFault` (advanced-configuration.md), `org.openl.rules.table.TableNotFoundException`
  (onboarding/troubleshooting.md, never existed), `src/main/resources/log4j2.xml` against the real
  `resources/log4j2.properties`, and the commands `mvn jetty:run` (3 guides) and `mvn rewrite:run`, naming no plugin.
- Docs settings no code reads, all editorial: DEPLOYMENT.md (22 of 81), API_GUIDE.md (6 of 10), TROUBLESHOOTING.md
  (3 of 28), externalized-config.md's `...filesystem.supportDeployments` (gone in 5.24.0), and
  `ruleservice.store.logs.enabled`, documented as the global external-storage switch while only `...db.enabled`
  gates it. API_GUIDE and api/public-api-reference.md map `/admin/*` and `/api/projects/*/git/*` to no controller.
- 51 relative links in `Docs/` resolve to nothing: `/DEV/CLAUDE.md` and friends from `README.MD` (every CLAUDE.md
  is now deleted), plus lowercase `/docs/...` paths that 404 on a case-sensitive server.
- `OpenAPIConverterTest` (640 lines) and `RulesDeployerServiceTest` (354 lines) carry a bare class-level `@Disabled`
  over live code. Restore or delete is a maintainer's call.
- `compose.yaml` downloads `postgresql-42.7.7.jar` while the root pom's `postgresql.version` is 42.7.13. The
  antrun mirrored-version guard covers Dockerfile, `DEMO/start` and `DEMO/start.ps1` only; compose.yaml drifts
  unnoticed. A bump plus a guard entry.
- `CorsFilter` is registered twice (`@WebFilter("/*")` and web.xml), doubling each `Access-Control-*` header.
- `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` of the 17 flyway/common scripts and nothing
  sets `sqlMigrationPrefix`, so Flyway's default `V` skips it: the ExternalGroups index is never created.
- Flake fixes a human could make: pin ITEST's `apache/kafka-native:latest` (3 suites) or move to
  `apache/kafka:4.3.1`; fix ORA-12516 in IT (studio-acl); raise the CI vitest `testTimeout` above 20_000.
- `RulesUtilsTest.testParseFormattedDouble` suppresses `"deprecated"`, which javac ignores, while both methods it
  calls are deprecated; the key should be `deprecation`. Both this and the Flyway script above are renames.
- Swapping `org.openl:x-forwarded-filter` for Spring's `ForwardedHeaderFilter` is BLOCKED: the root pom documents
  the opposite decision, and both call sites need PREPEND; Spring always REPLACES.
- `Docs/examples/production/` and `Docs/production-deployment/` are near-identical 320K copies, both reachable.
- KafkaMessageHeader.Type.PRODUCER_RECORD is documented as usable but StoreLogDataMapper acts only on
  CONSUMER_RECORD; the mapper or the guide is wrong. The constant is API and stays.

## Run log

- 2026-09-24: swept the 140-file EPBDS-16668..16702 delta at zero; 9 new veins closed. The Lombok no-op vein paid
  4 removals; #2166 MERGED (-8) the same day and its branch deleted. The two DEMO `.properties` are keep-listed.
- 2026-09-27: swept the 819-file EPBDS-16704..16762 delta (the Studio table-editor rewrite, Java 27). Reactor build
  and PMD at zero; the i18n and dead-TS-import veins paid 7 removals. #2184 MERGED (-7) the same day, CI green on
  the first run with no flake, Sonar 0 new issues; branch auto-deleted on merge.
- 2026-09-28: delta was two Dependabot bumps only, so the run went to new veins. Seven closed at zero and the
  whole-type scan re-ran repo-wide (859 raw, 63 production, all known framework FPs). Found that Error Prone's
  unused checks are enabled after all — 7 free hits, all FPs. Nothing removed, no PR opened.
