# Dead-code sweep ledger — openl-tablets

## Resume point

- PR #2212 is open with both change types it needs; drive it to green before anything else. Next new work goes on a
  fresh branch cut from a freshly fetched `origin/main`.
- All 14 change types are exhausted repo-wide. A run is: maintain the open PR, sweep the delta (expect near zero),
  spend the rest on a NEW vein. Only documentation, build config, i18n keys and dead TS imports have ever paid. A
  Sonar-cleanup wave PRE-HARVESTS the Java vein first, so a LARGE Java delta yields less, not more.
- The RELEASE-NOTE vein is the best one found and is NOT exhausted — re-run it every time guides or release notes
  change (Method rules). A module-merge wave leaves the poms and resources clean but strands package names in Docs.
- Start the reactor build detached in the FIRST minute (~27 min even from a cold `~/.m2`) and mine read-only veins
  beside it; before every push, list open `dead-code/*` PRs and re-fetch main — parallel runs share the branch.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | done; 7 blocks, all prose or parked calls with a rationale |
| 2 | Never-read assignments, dead stores | done; 14 PMD + 3 Error Prone hits, all documented FPs |
| 3 | Unused locals, private fields/methods/params | done; 14 PMD + 4 Error Prone hits, all FPs |
| 4 | Unused Maven dependency declarations | done; 676 analyze hits + npm deps, all FPs |
| 5 | Pom metadata: managed entries, exclusions, properties, managed plugins | done; 6 hits, all plugin-read flags |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done; 4 no-op Lombok annotations, in #2166 |
| 7 | Unreferenced resources (descriptors, config files, images) | done; 220 candidates, 0 unreferenced |
| 8 | CSS rules and inline styles | done; 1 file, 4 selectors, all used |
| 9 | Legacy JS functions and pages | done; 0 `.xhtml` remain, only keep-listed vendor JS |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | 1 key in #2212; 1,656 + 769 alive; re-run per delta |
| 11 | TypeScript exports, types, components, imports | done; 0 dead exports, 1 dead import merged in #2184 |
| 12 | Test fixtures: workbooks, utility classes, stub members | done |
| 13 | Package-private/protected members and unreferenced internal classes | done; 1,025 raw hits, 0 survivors |
| 14 | Documentation of settings and classes the code no longer has | 6 removals in #2212; re-run per release note |

## Open PR

- #2212 `dead-code/docs-and-locales`, head 26a2291741, opened on main at 95db26cd49.
  - a9db2af2a5 Remove documentation of settings, classes and modules the code no longer has (type 14, 5 Docs files).
  - 26a2291741 Remove the repository locale key no screen looks up (type 10, `browser.live`).
- Its 'Deliberately kept' names the stale plugin parameter table and the parallel-compilation settings.

## Merged PRs

- #2120 (-487), #2129 (-1), #2134 (-87), #2135 (-54), #2145 (-2), #2152 (-4), #2166 (-8), #2184 (-7) — each merged
  the day it opened, no review comment, on the PR body's evidence alone; a removal proven by unreachable behaviour
  rather than non-reference is accepted. The maintainer never merges a sweep PR red: rebase onto new main, wait for
  green, rebase-merge; the head branch auto-deletes.

## Module coverage

- All 86 reactor modules, studio-ui, Docs and DEMO scanned for every change type; nothing open.

## Deferred findings

- ~599 public members and ~35 public types are bytecode-unreferenced, write-only Lombok setters among them; all
  published API, kept under rail 8.2 and re-derivable by the ASM scan.
- `org.eclipse.jetty:jetty-home` is in no dependency tree and no pom (DEMO fetches Jetty by `jetty.version`);
  `.gitattributes` keeps a path that does not exist. Both human.

## False-positive shapes

- A detector that picks text files by an extension allowlist silently drops whole formats: `.webmanifest` was
  missing and reported 18 live resources dead. Select text by "no NUL byte in the first 8 KB" — ~14,000 files.
- javac inlines `static final` primitive and String constants, so a bytecode scan never sees a read and calls
  every such field dead (1,097 of 1,515 raw hits). Judge constants by source text, never by bytecode.
- Lombok generates accessors that exist in bytecode but not in source, so a field whose name occurs only at its own
  declaration may still be read: search `get`/`set`/`is` + the capitalised name before calling it dead.
- Four things make a live i18n key look dead: i18next resolves `t(key,{count})` to `key_one`/`key_other`; a key
  built by interpolation matches only WITH its namespace (`repository:notifications.`); the variable can sit
  MID-key (`${kind}_deleted`, `types.${v}`, a ternary of two leaves), not just at the end; and a leaf search misses
  `tests.no_${k}`. Harvest every t() template and plural suffix first.
- Harvesting literals is where detectors break, and a broken harvester reports ZERO, not noise: a backtick-first
  scanner swallows the `'...'` nested inside a template (`` `${t('file_name')}: x` ``) unless it recurses into every
  `${}`; a template whose static text is under ~3 chars (`` `${k}` ``) compiles to a catch-all regex that marks every
  key alive; and splitting a `.properties` line on `=` invents phantom keys from wrapped values. Match every quote
  style in ONE alternation, join continuations first, and INJECT A CANARY key before believing any clean scan.
- A "no references" test must require ZERO hits: at `<= 1` a resource with exactly one genuine reference
  (webstudio.xml's `<import resource=>`) reads as dead. 20 of 62 hits were that off-by-one.
- An exported TS type or const used only inside its own file looks unimported; that makes the `export` redundant,
  not the type dead. Removing `export` is a rename, not a deletion. A TS interface MEMBER the frontend never reads
  still models the server payload (ProjectRevision.commentParts is a Java record component in the published OpenAPI
  contract) — the TS twin of the Jackson-binding shape. Judge an API type against its Java record, not its usage.
- A leading positional callback parameter from `tsc --noUnusedParameters` or PMD `UnusedFormalParameter` is never
  removable: dropping it shifts the parameter that IS used (`Array.from(…, (unused, column) => …)`).
- A pom `<properties>` entry with no `${...}` dereference may still be read by a plugin by name:
  `lombok.delombok.skip`, `archetype.test.skip`, `invoker.skip`. Check the plugin before calling it dead.
- A private or package member named in any string literal is reflective (`@MethodSource`, JAXB, OpenL datatype
  binding): filter bytecode hits on Java string literals, non-Java text and workbook strings. Search an accessor by
  its property name too — Jackson DTOs and OpenL beans bind `basePath`, not `setBasePath`. Also alive: JAXB private
  `beforeMarshal`/`afterUnmarshal`, Spring MVC handlers, record accessors, generic bridge overrides.
- A top-level type whose simple name occurs only in its own file is still alive when a framework names it — JUnit by
  file pattern, Spring by classpath scan, `@Mojo` by the descriptor: 73 production types.
- A generated member can be reached where the generator is not: `@SuperBuilder` on an abstract parent serves its
  subclasses' `builder()`, a Spring `@Bean` is collected by its framework interface (ViewResolver), and a
  package-visible `LOG` is read as `Owner.LOG.x`, so a same-file search sees neither.
- An indent-anchored regex reads a nested class's members as the outer class's, and `@Builder.Default` matches
  `@Builder`. Anchor on the declaration, require no dot after the annotation, and assert the text before editing.
- PMD blind spots, all recurring. UnusedAssignment: constructor early return, read back through a callback, read by
  a getter, `key = null` before `System.gc()`, publish-before-block, re-entrancy, empty catch, a `var` initialised
  before the if-block that alone uses it (moving it is a refactor, barred by 8.7), and a record compact constructor
  deriving a component the caller still reads. UnusedLocalVariable: try-with-resources locals, a counting for-each,
  a cast before `fail()`; `target/generated-sources/javacc/**` is 21 of the 38 hits and is off-limits.
- Reflection fixtures asserted by name: epbds6830 BeanA.getAB, AOpenClassTest.getC, JavaOpenClassTest.gg, MyProp
  fields in a binary .xls, YamlMapperFactoryTest transients, InterfaceTransformerTest.TestInterface. Error Prone
  reports BeanA.getAB every build; it stays.
- Bundle conventions: ValidationMessages `openl.error.<status>.<code>.message` (code composed in Java), sql-errors
  keyed by vendor error code, `openl-default.properties` keys composed as `repo-<id>.` and `$ref` indirection —
  the documented `repository.archive.*` and `repository.design.*` are that composition, not dead keys.
- dependency:analyze FPs: a module declaring no `<dependencies>` still gets findings from its parent; the inherited
  test harness (~294 of them); provided annotations and processors (jspecify, lombok); runtime providers named from
  configuration, not code (Spring XML, maven-scm, cxf-rt-features-logging, Azure's jackson dataformats and
  reactor-core); aggregators; wars and jdbc drivers an ITEST only boots a server with.
- A class named by string composition has no textual reference at all: `OperationFactory` builds the 13 TBasic
  operations as package + `getOperationType()` + `Operation`. Search a name MINUS a common suffix.
- An enum whose `values()` is iterated keeps every constant alive. To prove one dead, show it is never stored and
  never returned, then that the `values()` loop is a no-op for it.
- An identifier index keyed on `[A-Za-z_$][\w$]*` misses a stem starting with a digit: confirm with `git grep -lF`.
- A dotted-name detector over Markdown catches heading anchor slugs and wrapped table cells, not settings: require
  dot separators only, drop hyphenated slugs, and rejoin a name split across a line wrap.
- Jekyll lists pages by `nav: "auto"`, so a link-graph orphan check calls 265 live pages orphans; site-absolute
  links resolve on the published site, not on disk, and only a wrong-case path breaks.
- A backticked path in documentation is no claim about a real file when it is elided (`STUDIO/.../Foo.java`) or
  names a build output under `target/`; filter both before reporting a path broken. A documented
  `mvn <prefix>:<goal>` is alive when the artifactId does not follow `<prefix>-maven-plugin`
  (`dependency-check-maven` answers `dependency-check:`): resolve the prefix, never guess the artifactId.
- Error Prone's unused hits repeat the PMD blind spots: a Jackson binding class handed to an ObjectMapper is
  introspected, so its fields are the payload (JsonUtilsTest.BindingClasses), and a write-only field on a test's
  cache-key type models the production key (KeyClass.field). `EffectivelyPrivate` wants visibility, barred by 8.7.
- The clone is SHALLOW (~50 commits) whose root adds every file, so `git log -S` answers that root for every
  string. Never claim when something was removed here; prove absence at HEAD instead.

## Method rules

- Build the whole repo once per run: `LANG=C.UTF-8 mvn clean install -Dquick -DnoPerf -T2
  -Daether.syncContext.named.time=600`. Unset `gpg.format`/`commit.gpgsign` first.
- Index the whole tree once (regex `[A-Za-z_$][\w$]*` per file into a Counter, ~8 s) and answer every "is this name
  used" question from it; a name whose total count equals its count in its own file is unreferenced.
- PMD needs reactor artifacts and a warm `~/.m2`, so run it online and fully qualified (a `pmd:` prefix fails):
  `mvn test-compile org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd dependency:analyze-only -Pitest -fae -T2
  -Dquick -DnoPerf -pl '!STUDIO/studio-ui'`, plugin under root `<build><plugins>`, ruleset under `.toDelete/` by
  `${maven.multiModuleProjectDirectory}`; parse every `target/pmd.xml`, then restore the pom.
- Bytecode scan: ASM (9.10.1) over every `target/classes` and `target/test-classes`, taking invocations, field
  access, method handles, invokedynamic args and `ldc` strings; drop annotated members, overrides and names in
  literals, then apply the constant-inlining and Lombok filters above.
- Chain install and PMD in one detached `setsid nohup` script touching a DONE file; poll the file, never the log's
  tail. Anchor any `pkill -f 'name[.]py'`. Never edit the working tree or rebase while Maven runs.
- A container registration (`web.xml`, `@WebFilter`) does NOT prove a class alive: judge whether its behaviour is
  reachable. A `<listener>` serves only the interfaces the container sorts it into, so a registered-but-unbound one
  never fires — verify from the container jar, never from the spec.
- Both Docs cross-checks are SPENT for deletions: every `org.openl.*` token resolves at HEAD, sits in Human
  follow-ups, or is a release-note reference to a class of that era. Re-run over changed pages AND after any wave
  that MOVES code — the tokens go stale in Docs while the poms stay clean.
- Release notes DATE a removal, which absence alone cannot: harvest backticked tokens from release-note lines saying
  removed/dropped/no longer, drop those still resolving in code, search the rest across the guides. Paid 6 at first
  run. The verb often sits in a 'Removed ...' HEADING over a plain list, so read those lists or a hit's companions
  are missed. `locales/lookups.test.ts` proves lookup→key; only key→lookup finds a dead key, so the two cannot
  collide. studio-ui has no `t(variable)` call, so every key is a literal or a template.
- The maintainer approved one removal straight off the body's 'Deliberately kept' line. Deferring is not dropping.
- Prove non-reference with `grep -rIwF <name>` over tracked files, `grep -raF` for binaries and `unzip -p` for
  workbooks (a `.xls` as latin-1 and UTF-16). Drive it from `git ls-files`, or untracked `studio-ui/dist/` answers
  every query.
- Removing members is a fixpoint: re-check the fields, helpers, parameters and imports it orphaned. SonarCloud's
  "new issues" must read ZERO before the PR is done, no auth: `sonarcloud.io/api/issues/search?componentKeys=
  org.openl.rules:openl-tablets&pullRequest=N&sinceLeakPeriod=true`.
- Stage every commit by explicit path (`git add -- <files>`) or a `git rm` staged earlier rides into it.
- Frontend gate: `npx tsc --noEmit --noUnusedLocals --noUnusedParameters`, `npx vitest run <area>`. Both need
  `node_modules`, which the reactor build populates — tsc then runs fine beside Maven, vitest does not.
- i18n keys: parse each `addResourceBundle` literal into dotted paths and judge by the FULL path — the same leaf
  sits at several depths, so a leaf search calls every orphan alive. Then apply the four shapes above.
- PMD's report namespace is `report/2.0.0`, NOT the ruleset's `report_2_0_0`: the wrong one parses 0 violations out
  of a full report. Assert a non-zero total before believing a clean scan.
- Verify the identity BEFORE pushing: rail 8.4 bars force-pushing `dead-code/ledger`, so a wrong one there stands.

## Keep-list

- OpenL datatype beans and rules interfaces in tests are bound from Excel by property or method name
  (IChildBean.getMyBean, Tutorial4Interface.getTheft_rating, Location setters, RulesUtilsTest.testFlatten).
- Jackson-bound webstudio models keep every accessor (RepositorySettings and its kin, SettingValueWrapper).
- Convention files: Flyway migrations, `META-INF/openl/extension-*.xml`, `openl-db-repository-*.properties`, static
  rapi-doc, site.webmanifest icons, `META-INF/services/**`, ITEST `application-*.properties` (Spring profiles),
  archetype `archetype-metadata.xml`, `compose.override.example.yaml`.
- Config defaults in `openl-default.properties` are documented in Docs guides and composed at runtime; all alive.
- `DEMO/webstudio.properties` and `DEMO/webservice.properties` are named nowhere: ApplicationPropertySource reads
  `file:{appName}.properties` from the cwd under those two context names.
- Demo workbooks under `org.openl.rules.demo/src/**` and `webstudio/test/rules/decisionTableIndexes/` load by
  folder. Each root `<exclusions>` entry still removes a transitive artifact.
- `.gitignore`/`.gitattributes` entries for a file TYPE are prophylactic and stay with no such file tracked; only
  a PATH-specific rule naming a directory that does not exist is a candidate.
- A parked call carrying its own rationale comment is a documented decision, not dead code:
  `ResultExport.validateMergedRegions`, `trackAllColumnsForAutoSizing`, RuleRowHelper's `intern()` TODO, ITEST
  `HttpClient`'s bulk OpenAPI block, and both `squid:S2095` suppressions.
- webstudio `SessionListener` is alive: the container dispatches its two listener interfaces, and it drives the
  session cache, security events and `WebStudio.destroy()`.
- The Hibernate `Tag`, `TagType` and `TagTemplate` entities and their DAOs stay; only `OpenLProject` went.

## CI flakes

- IT (studio-acl): `OracleRdbmsTest.upgrade` fails "expected 0 but was N" with `ORA-12516` while other vendors
  pass. Oracle Free container limit, not the diff; one rerun clears it.
- IT (services-data), HIGH RATE: `apache/kafka-native:latest` exits 1 in its own `setup` (GraalVM segfault), then
  Testcontainers times out on "RECOVERY to RUNNING". A ROTATING victim proves the flake, a regression kills the
  same suite every run. Two reruns per SHA.
- Tests (without ITEST), studio-ui: `ModuleWorkspace.test.tsx` times out in `waitFor` only on a loaded runner —
  tell: vitest wall time near 860 s against the 20 s per-test CI ceiling and a failing set SHRINKING between
  attempts. Never chase with a vitest change; a new SHA cures it.
- `Sonar analysis` is skipped when any job fails and lands ~10 min after the last, so the issues API answers 0 for
  "never analysed": confirm the analysed SHA at `project_pull_requests/list` before trusting a 0.
- `rerun_failed_jobs` returns 403 mid-job and re-reads the same jacoco artifacts, so it cannot cure `Sonar
  analysis` dying in `report-aggregate` with "Unknown block type N"; only `rerun_workflow_run` clears that.
- Fetch a job log with `get_job_logs` (tail 8000); find failures with `... - FAIL`, the cause with `ORA-|expected:`.
  The base `Build` workflow is a multi-JDK matrix red since August — check another PR ran the same job.

## Container facts

- No `gh` CLI: GitHub MCP tools only (create_pull_request then subscribe_pr_activity; pull_request_read,
  update_pull_request, add_issue_comment, actions_list list_workflow_jobs, actions_run_trigger rerun_failed_jobs
  with the workflow run id from a check's html_url).
- Container presets `gpg.format=ssh` and `commit.gpgsign=true` globally (unset both) but no `GIT_AUTHOR_*`
  variables; a plain `git config --global user.*` holds, and passing the identity inline as well costs nothing.
- Error Prone IS enabled and its unused checks DO fire, so `mvn clean install` is a free Java detector: grep the
  log for `[UnusedVariable|UnusedMethod|UnusedNestedClass|EffectivelyPrivate]` (now 4, three `EffectivelyPrivate`
  barred by 8.7 plus BeanA.getAB) before paying for PMD; ignore `[NullAway]`.
- 4 cores, 15 GB RAM: `-T2` for the reactor, ~27 min with a warm `~/.m2`. `npx tsc` runs beside Maven, vitest not.
- `.toDelete/` is gitignored: keep the PMD ruleset, scratch poms and detector scripts there. `~/.m2` and
  `STUDIO/studio-ui/node_modules` start COLD in a fresh container — check before budgeting.
- Edit the ledger through `git worktree add` on `origin/dead-code/ledger`, never by switching the sweep branch.
- The clone holds ~50 commits and a delta can far exceed that (one wave ran ~200): deepen until dates repeat.

## Exhausted veins

- ALL 14 change types re-swept repo-wide over 86 reactor modules, studio-ui, Docs and DEMO at zero, by: PMD 5
  rules, Error Prone, the ASM member and whole-type scans, the identifier index, `tsc --noUnusedLocals
  --noUnusedParameters`, dependency:analyze-only, and hand passes over resources, images, bundles, locales, config
  defaults, pom metadata, `@SuppressWarnings`, `.editorconfig`, `.gitignore`, the Docs page graph and DEMO css.
- Lombok no-ops: @Slf4j, @Builder/@SuperBuilder, @RequiredArgsConstructor, @Jacksonized and @Builder.Default are
  swept clean repo-wide (4 paid in #2166); @Getter/@Setter and the behavioural ones are NOT sweepable.
- studio-ui holds NO stylesheet (CSS-in-JS + Ant tokens), so `.css` is a legacy-WebStudio vein only.
  `i18n/openapi.properties`'s 769 keys are plain literals in `@Operation`; Docs' 592 images are all referenced.
- studio-ui locales are NOT exhausted: a UI rewrite orphans keys, 6 fell in #2184 and 1 in #2212. Re-run the i18n
  pass over every delta touching studio-ui.
- Merging a module into another leaves NOTHING dead in the build: after five STUDIO modules folded into webstudio,
  the module list, the merged pom's 65 deps and the per-vendor flyway resources were clean; only Docs went stale.
- TS interface and type MEMBERS swept by identifier index: 1 candidate, an API payload FP. Closed.
- Veins probed and closed at zero: Maven profiles, npm dependencies, orphaned `package-info.java`, empty tracked
  files, production types referenced only from tests, container registrations (web.xml and all 8 `@WebFilter`
  /`@WebServlet`), JSF-era orphans, pom file-path references, Spring XML beans, duplicate declarations across 209
  poms, dependencies a parent declares, servlet init-params, exact-duplicate tracked files, unused XML namespace
  prefixes, surefire `systemPropertyVariables`, every Spring
  `base-package` and `<import resource=>`/`@ImportResource` target, log4j2 appenders, JUnit `@Tag` and surefire
  `<groups>`, `lombok.config`, Dockerfile stages and compose services and volumes, `@Deprecated` members
  unreferenced outside their file, studio-ui npm scripts and config files, poms no parent declares as a module,
  test-bearing classes surefire would not select, exception types never instantiated, `@Bean` methods nothing
  names, listPageTheme palette tokens, Docker/compose environment variables, dependabot entries, the Jekyll
  navigation and plugin list, duplicate sibling entries in non-pom XML and JSON, duplicate keys over properties and
  ignore files, per-package logger categories, and image references across poms. The duplicate-entry scan paid once,
  on three `**/*.sql` Spotless includes; the `.aj`/`.apt`/`.scss` ones are prophylactic and KEPT — settled.

## Human follow-ups

- Docs renames from code that MOVED: `org.openl.rules.webstudio.web.rest`→`org.openl.rules.rest`,
  `...web.trace.debug`→`org.openl.studio.projects.service.trace`, `org.openl.security.standalone/resources/db/
  flyway/`→webstudio's, `org.openl.openclass.IOpenClass`→`org.openl.types.IOpenClass`,
  `org.openl.rules.maven.plugin`→`openl-maven-plugin`, and `org.openl.studio.mcp.node`, which names no package.
- rules-projects.md documents a `generateInterfaces` configuration found in no other file and seven parameters
  GenerateMojo lacks (its real ones: superInterface, interfaceClass, moduleName, generateSpreadsheetResultBeans,
  externalParameters). Also `#configuring-the-instantiation-strategy` points at no heading, and DEPLOYMENT.md's
  'Rule Compilation Caching' now covers only parallel compilation.
- Docs renames this routine may not make: `MixInClassFor`→`MixInClass`, `kafka.ser.MessageDeserializer`→
  `RequestMessageDeserializer`, `...ws.full:war`→`...ws.all` (rule-services/configuration.md),
  `SkipFaultStoreLogData`→`SkipFault`, `org.openl.rules.table.TableNotFoundException` (never existed),
  `src/main/resources/log4j2.xml` against the real `resources/log4j2.properties`, `mvn jetty:run` (3 guides) and
  `mvn rewrite:run` naming no plugin, and `RulesUtilsTest.testParseFormattedDouble`'s `"deprecated"`, which javac
  ignores while both methods it calls are deprecated — the key is `deprecation`.
- Docs settings no code reads, all editorial: DEPLOYMENT.md (22 of 81), API_GUIDE.md (6 of 10), TROUBLESHOOTING.md
  (3 of 28), externalized-config.md's `...filesystem.supportDeployments` (gone in 5.24.0), and
  `ruleservice.store.logs.enabled`, documented as the global switch while only `...db.enabled` gates it. API_GUIDE
  and api/public-api-reference.md map `/admin/*` and `/api/projects/*/git/*` to no controller. 51 relative links
  resolve to nothing: `/DEV/CLAUDE.md` from `README.MD` (every CLAUDE.md is deleted) and lowercase `/docs/...`.
- Bugs only a human may fix: `v14__Create_Index_ExternalGroups.sql` is the only lowercase-`v` of 17 flyway/common
  scripts and nothing sets `sqlMigrationPrefix`, so Flyway skips it and the ExternalGroups index is never created;
  `CorsFilter` is registered twice (`@WebFilter("/*")` and web.xml), doubling each `Access-Control-*` header;
  `compose.yaml` pins `postgresql-42.7.7.jar` against the pom's 42.7.13 while the antrun guard covers only
  Dockerfile and `DEMO/start*`, so it drifts unnoticed.
- `OpenAPIConverterTest` (640 lines) and `RulesDeployerServiceTest` (354 lines) carry a bare class-level `@Disabled`
  over live code. Restore or delete is a maintainer's call.
- Flake fixes a human could make: pin ITEST's `apache/kafka-native:latest` (3 suites) or move to
  `apache/kafka:4.3.1`; fix ORA-12516 in IT (studio-acl); raise the CI vitest `testTimeout` above 20_000.
- `Docs/examples/production/` and `Docs/production-deployment/` are near-identical 320K copies, both reachable.
- KafkaMessageHeader.Type.PRODUCER_RECORD is documented as usable but StoreLogDataMapper acts only on
  CONSUMER_RECORD; the mapper or the guide is wrong. The constant is API and stays.

## Run log

- 2026-09-28: delta was two Dependabot bumps only, so the run went to new veins. Seven closed at zero, the
  whole-type scan re-ran repo-wide (859 raw, 63 production, all framework FPs), and Error Prone turned out to be
  enabled. Nothing removed, no PR opened.
- 2026-09-29: delta was the ~200-commit Sonar cleanup wave (2026-09-26..29, 0 files deleted); reactor green in 26:44.
  Twelve veins re-swept at zero, PMD and dependency:analyze among them. Nothing removed, no PR opened.
- 2026-09-30: delta was 27 commits including the EPBDS-16781 merge of five STUDIO modules into webstudio. Reactor
  green in 26:25 from a COLD `~/.m2`; Error Prone, tsc and the merge-leftover checks at zero. The new release-note
  vein paid 6 documentation removals and the i18n pass 1 key. #2212 opened (-72).
