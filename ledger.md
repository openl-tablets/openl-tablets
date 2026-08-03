# Dead-code sweep ledger

State memory for the daily sweep of openl-tablets. Read in full at the start of every run.

## Resume point

- Every queue row is done and every vein below is closed. New scope arrives only as new commits on `main`; the
  incremental sweep is current through `e605fe8b`. Diff `git log --name-status e605fe8b..origin/main`, sweep only
  the source files it touches, skip webstudio Java, ITEST fixtures, `Docs/` and `.github/`. Never invent a detector.
- #1940 carries the whole sweep, is green on all 12 Actions checks, has no unanswered comment, and waits on a human
  review this routine cannot supply. Do not nudge it, do not re-argue the SonarCloud gate, do not comment to
  restate a green status — the description carries it.
- The idle pass is five calls: merge-base plus counts against the description's 4 commits / 8 files / 8 insertions /
  24 deletions, `get_comments`, `get_check_runs`, and one `curl` for `opensaml-bom` — still 000 shibboleth and
  404 Central, so webstudio Java stays the one unswept surface. Seed the stub only if a build is needed.

## Change-type queue

| # | Change type | Status |
|---|---|---|
| 1 | Unreferenced images | done, no findings (incl. all 610 under `Docs/`) |
| 2 | Unreferenced `.xhtml` pages | done, no findings |
| 3 | Dead CSS classes and ids | done, only `.te_hidden`, deferred |
| 4 | Unreferenced `.js` files | done, no findings |
| 5 | Unused `.properties` keys | done, no findings |
| 6 | Unused i18n keys in studio-ui bundles | done, merged |
| 7 | Unreferenced exported TS symbols and modules | done, only deferred findings |
| 8 | Dead functions inside legacy `.js` | done, merged |
| 9 | Never-read assignments (PMD) | done, main and test sources |
| 10 | Unused private fields and locals (PMD) | done; 20 fields deferred |
| 11 | Unused private methods and formal params (PMD) | done, no removable finding |
| 12 | Unused nested / effectively-private types (javac) | done, no findings |
| 13 | Unused declared Maven dependencies | done for 52 modules, no provable finding |
| 14 | Whole-type deadness over Java simple names | done, all 853 non-public top-level types alive |
| 15 | Package-private/protected members of non-public types | done, all 386 examined are alive |
| 16 | Package-private members of public types | done, all 835 declarations are alive |
| 17 | Unreferenced public types in test trees | done, 1 removed |
| 18 | Dead Maven profiles and `pluginManagement` entries | done, all 14 profiles and 44 managed plugins alive |

## Open PR

- Branch `dead-code/java-internals`, PR #1940, ready for review, 4 commits, 8 files / 8 insertions /
  24 deletions, no review threads, blocked on the missing human review. Re-derive the counts mechanically.
- `676a8e9071 Remove never-read variable and field initializers` — 5 files.
- `ac445bd038 Remove unused local variables that assertions never read` — 1 file.
- `90168ba313 Remove an unused private field from the grid-table test stub` — 1 file.
- `8957deb3c5 Delete a test fixture class that no test and no spreadsheet references` — 1 file.
- The commits are one detector rule each, so they are different change types: never squash them together.
- CodeRabbit asked for JSpecify `@Nullable` on the two `FuzzyContext` fields; answered and declined — the module
  has no JSpecify at all and the diff does not change their nullness. Settled, do not re-answer.

## Merged PRs

- #1933, 12 files / 74 deletions — studio-ui i18n keys plus `PopupMenu.showChild`. Merged with the SonarCloud gate
  *and* `Tests (without ITEST)` red, which is the precedent #1940 rests on.

## Module coverage

- `STUDIO/studio-ui` — locale bundles, import graph, eslint, npm dependencies: only deferrals left.
- `STUDIO/org.openl.rules.webstudio` Java — never swept, uncompilable here (see *Container facts*); its resources
  are already covered by the repo-wide resource veins.
- Every other module — PMD (main and test sources), javac, member and whole-type deadness all swept.

## Deferred findings

- `STUDIO/org.openl.rules.tableeditor` `taglib/TableEditorTag.java` (11 unread private fields) and
  `TableViewerTag.java` (7): written by public setters, read by nobody, named only by `META-INF/tableeditor.tld`.
  Never touch the fields alone — the setters are the taglib's declared attribute contract.
- `STUDIO/org.openl.security` `SimpleGroup.description` — written by a public setter and a public constructor
  parameter, read nowhere. Removing it changes public API consumed by webstudio, which cannot be compiled here.
- `DEV/org.openl.rules` `DecisionTableBuilder.methodName` plus its public `setMethodName` and the single call in
  `TableSyntaxNodeDispatcherBuilder:136` — the whole chain is inert, but the setter is public API in `DEV/**`.
- `WSFrontend` `ServiceManagerImpl:230` re-assigns `serviceDescriptionInProcess` to the value already at 228;
  proving it dead is an absence-of-path argument through the whole compile subsystem for a one-line payoff.
- `DEV/org.openl.rules.test` `RulesInFolderTestRunner:80,116` — the flagged `messagesCount++` passes its value on,
  so only the increment is wasted: an expression rewrite, not a deletion.
- `STUDIO/studio-ui/src/containers/MergeModal/types.ts`: `MergeRequest`, `ResolveConflictsRequest`,
  `ResolveConflictsResponse`, `FileConflictResolution` — unused in TS but mirror a live REST contract in
  `Docs/api/projects-merge-api.md` backed by a Java record and an OpenAPI schema.
- ~45 studio-ui exported types used only inside their own file — alive; dropping `export` is a refactor, not a delete.
- `js/datepicker.js` `dateValidForSelection`, `getSelectedDate`, `setDisabledDays`, `joinNodeLists` — no call site,
  but the file is vendored third party (DatePicker v5.4, frequency-decoder.com, CC BY-SA 3.0) and these are its API.
- `.te_hidden` in `STUDIO/org.openl.rules.tableeditor/css/common.css` — the only real CSS orphan. Blocked because the
  CSS bundles are not reproducible (see *Method rules*), so the removal cannot be propagated to what ships.
- `STUDIO/org.openl.rules.workspace/resources/deployer.properties` — a `production-repository.$ref` sample no file
  names, in a library jar with no matching application. `{appName}.properties` would load it for an app named
  `deployer`, so only a human knows whether such an app still exists downstream.

## False-positive shapes

- **PMD field-initializer "never used" ignores an early `return` in the constructor.** `CellStyle` returns when its
  argument is null, so `= HorizontalAlignment.GENERAL` is observable. Check every constructor path.
- **A `= null` initializer is live whenever any path reaches a read without the assignment.** PMD also treats an
  assignment in a `try` as overwritten by one in the `catch` — `GitRepository.result` has both, both live. Test the
  candidate against a sibling declared in the same block with no initializer: if that compiles, it is redundant.
- **A never-read local can be the test itself.** Three shapes, all reported as unused: a variable that only hosts a
  cast whose failure is asserted (a cast cannot stand alone as a statement); a try-with-resources name Java requires,
  where the close is the point; and a null-out near `Runtime.gc()` or a weak reference, dropping the strong reference
  so collection can happen. Read the assertions first.
- **A test can consume a member by its mere existence.** `JsonUtilsTest.BindingClasses` is the binding target passed
  to `getCachedObjectMapper` and `KeyClass.field` carries the cache key's identity; `AOpenClassTest.C.getC()` is what
  `assertNull(findMethod(methods, "getC"))` fails against, so deleting it leaves the assertion vacuous.
- **Search an accessor by the property name — the field's own name may never appear as a read.** Lombok
  `@Getter`/`@Setter` generate it, so `TablePart.partName` is reached only as `getPartName`; and a private field can
  exist solely to make a bean property writable — `JavaOpenClassTest.BeanA.gg` is read by no code, but `setGg` is
  what makes the asserted `gg` property work.
- **A test bean is usually named from inside an `.xlsx`/`.xls`, which no text search can see** — `Bean1`, `Bean2`,
  `EPBDS7956`, `IChildBean`, `MyProp`. Unzip every Excel resource and search as UTF-8 and UTF-16LE before calling one
  dead. A deliberately malformed bean is worse: `epbds6830.BeanA` feeds
  `EPBDS-6830_external_datatypes_validation.xlsx.msg.txt` with one defect per expected error, and its private,
  wrongly-cased `getAB()` is one of them — the message list is unchanged by removing it, so the loss is silent.
- **Surefire's default includes match a `Test` prefix as well as a suffix**, so `TestIf`, `TestAutoType0` and
  friends are executed although nothing names them. Never call a `Test*` class dead.
- **A nested `@Configuration @ComponentScan` keeps every class in its own package alive** — the 17 `appNNN`
  controller fixtures in `spring.openapi`. A JMH `@Benchmark` class is likewise run by the harness and named by nothing.
- **A `@Component`/`@Service` class implementing an interface is injected by interface type**, so its own name
  appears in no other file — `FileNodeMapperImpl`. Check for a stereotype annotation before believing it is orphaned.
- **A field can be written for the next line to read back, or blanked to change what a resolver returns.**
  `ServiceManagerImpl` sets `serviceDescriptionInProcess` before `createService`, which reaches it through
  `getRulesDeployInProcess()`; `DynamicPropertySource` sets `settings = Map.of()` so `resolver.getRawProperty` falls
  through to defaults. Look for a getter, and for self-reference, before calling a field assignment dead.
- **Neither pom configuration shape is ever deletable here.** A `pluginManagement` entry needs no `<plugin>` consumer:
  it pins a plugin bound by a lifecycle default (`deploy`, `site`), by a packaging type (`maven-archetype`), or
  invoked as a bare goal (`release:prepare`, `scm:`, `license:`). A profile with no `-P` reference is alive when a
  property activates it (`quick`, `skipTests`, `!noPerf`, `noDocker`, `sonar`, `!sonar`, `!skipTests`, `env.CI`) or
  when it is documented tooling — `owasp`, named by `SECURITY.md`, is the only one with neither.
- A `for (Object item : c) { size++; }` counting loop reports `item` as unused; the variable is required syntax.
- i18next appends `_one`/`_other` itself when `count` is passed; check the plural-stripped base before deleting.
- A locale key reached only through a template literal: enumerate `t(` + backtick call sites, treat each composed
  prefix as keeping its whole family alive. `t(someKeyVariable)` means the literals sit at the call sites instead.
- Never test a CSS class by "does this token appear anywhere" — `ui-layout-*`, `tooltip_*` and `te_toolbar_*` are all
  built by string concatenation in JS or Java. `rf-*` cannot be proven dead (RichFaces ships its JS inside a jar) and
  `ant-*` come from antd at runtime. A regex of `#[a-zA-Z0-9]+` over CSS also reports every hex colour as an id.
- `\b` does not match before `$`, so a `\b`-anchored search finds no call site for a `$`-leading name. Use
  `(?<![\w$])name(?![\w$])`.
- A name defined as a key in an options object literal passed to a framework is a callback, not something anyone calls
  by name — `onFailure` in `TableEditor.js` belongs to `new Ajax.Request(...)`. Check the enclosing call first.
- A module can be imported by a specifier that already carries its extension (`from './App.styles.ts'`), which
  defeats a resolver that only appends `.ts`/`.tsx`. Try the specifier verbatim before appending.
- A "dead" JS function is usually called from inside its own file — exclude the defining file from the search and
  every private helper looks unreferenced.
- **A dependency jar can read a resource by a name hardcoded in its own bytecode.** CXF's `AbstractHTTPServlet`
  loads `/cxfServletStaticResourcesMap.txt`, so that file is named by no file here and is still load-bearing. When
  a resource name looks invented-but-conventional, fetch the owning jar and grep its constants.
- A pom property with no `${...}` reference anywhere is almost always a plugin convention parameter — `maven.*`,
  `sonar.*`, `invoker.*`, `archetype.*`, `spotless.*`, `lombok.delombok.skip`, `project.build.sourceEncoding`.
- Duplicate-`<dependency>` detection must first strip `<dependencyManagement>`, `<plugin><dependencies>` and XML
  comments; otherwise correct Maven practice and commented-out samples both report as duplicates.
- A CodeRabbit walkthrough describes intent, not the diff — it reported an insertion in a file with zero insertions.
  Verify any bot claim against `git diff --numstat` before answering it.
- **A multi-line annotation defeats a scan-upwards for annotations**, so a `@ParameterizedTest` whose `@CsvSource`
  ends on `})` reads as an unannotated method. Walk back over a closing bracket, or the liveliest tests look dead.
- A Java field regex with no scope tracking matches every local declaration — 3313 "fields" against 112 real ones.
  Track brace depth and only accept a declaration whose enclosing brace was opened by a type declaration.

## Method rules

- Prove non-reference with a plain repo-wide literal search excluding `target/`, `node_modules/` and `.git/` — every
  file type, never a regex scoped to one attribute or one module.
- **One detector rule is one change type**, so one PMD rule is one commit. The queue rows are coarser than that
  and group several rules per row — never squash two commits just because they share a queue row.
- For a bundle key, search the full dotted path **and** the bare leaf name; either hit means keep.
- Validate any new bulk detector by feeding it two fabricated names; if they come back "referenced", the search is
  wrong, not the repository. For a linter, plant a violation and confirm it is reported.
- For any deadness check on a type **or a member**, "appears in one file" is not enough — count occurrences
  **inside** that file too. This one rule killed every member candidate ever raised: a secondary top-level class
  used by its file's primary class, and a helper called only by its own file's other methods, both look orphaned.
- **Run two independent Java detectors and resolve the union.** A brace-tracking parser silently drops members behind
  an annotation or a multi-line signature; an indent-based one drops nested members and reports interface members,
  which are implicitly public. Their disagreement is where the bugs are.
- **A production package can be named `test`** — `webstudio/src/.../web/test/`. Detect a test tree by the module's
  test source root, never by a path segment.
- **This clone is shallow (50 commits).** Its earliest commit "adds" all 15032 files, so `git log --diff-filter=A`
  attributes every older file to that graft and history proves nothing about a file's origin. Never argue from it.
- Resolve studio-ui imports with tsconfig `paths` `"*": ["./src/*"]` (a bare specifier is `src/`-relative), and follow
  side-effect `import 'x'` and `vi.mock('x')` too, or barrel-only and test-only modules look dead.
- The tableeditor JS bundles `js/tableeditor.all.js` and `js/tableeditor.min.js` are checked in and are what the
  runtime loads, not the individual sources. `bash compile.js.sh` reproduces both byte-for-byte using the checked-in
  `yuicompressor-2.4.7.jar`, so a source removal must regenerate them in the same commit. `compile.css.sh` does
  **not** reproduce the committed CSS bundles — never regenerate those.
- Run the frontend gate from `STUDIO/studio-ui` with no Maven build competing: `npx tsc --noEmit`, `npx eslint src`
  (whole tree), and `npm run test` — **which is `vitest run --coverage`, not plain `vitest run`; match CI's own
  command or the coverage pass goes unverified**. Baseline 164 files / 1460 tests. `no-unused-vars` is `warn`, so
  read the output, not the exit code. `npm ci` works here; `node_modules` is gitignored.
- When a deletion empties a parent object literal, delete the parent in the same commit.
- A `Docs/` markdown page that nothing links to is not a candidate — Jekyll publishes it and documentation is this
  repository's approved source of truth. The whole `Docs/` prose tree is out of scope.
- **PMD report XML uses namespace `http://pmd.sourceforge.net/report/2.0.0`** (slash-dot, not underscores). A parser
  built for the ruleset namespace silently returns zero violations from a non-empty report. Most `target/pmd.xml`
  files here hold only `<suppressedviolation>` elements — grep `<violation ` to find the reports that matter.
- The `pmd` plugin prefix does not resolve with `-o` on a cold cache. Invoke the goal by full coordinates,
  `mvn org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd -fae`, and run it online the first time.
- Two of the 207 poms are archetype-resource templates whose first line is Velocity, not XML, so any XML parser
  fails on them. Skip them by name rather than aborting the scan; they declare no profiles and no plugins of ours.
- GitHub Actions compiles every push against the real opensaml BOM, so CI is the authoritative gate on anything the
  locally stubbed build could not verify.
- **Never edit a source file while a Maven build is running.** A half-applied edit — import gone, field still
  there — reads as a genuine compile error and costs the whole reactor pass. Finish the edits, then build.
- `pgrep -f <pattern>` matches the polling shell's own command line, so a wait loop on it never exits. Poll the
  log's mtime, or grep the process list for the JVM path instead.

## Keep-list

- `RestRuntimeException.getErrorCode()` builds `openl.error.` + HTTP status + the code passed to the exception, so a
  `ValidationMessages.properties` key is only ever written as its suffix in Java. Search by suffix.
- A JSF page reaching a bundle as a prefix string concatenated with a lowercased enum name keeps that whole
  `messages.properties` family alive.
- Property names composed at runtime stay: `repo-default.` plus the repository type plus the suffix, `repository.`
  plus the id plus `.settings.`, and `openl-db-repository-` plus the database product name; the reference-key suffix
  is exercised from ITEST init params.
- `ApplicationPropertySource` loads `classpath:{appName}.properties`, so a properties file at a jar root can be alive
  through the deployed application's name alone — `DEMO/webservice.properties` is exactly that shape.
- Spring pulls every `META-INF/openl/extension-*.xml` in through `@ImportResource("classpath*:...")` in
  `ExtensionsConfiguration`, so those bean files are never referenced by name.
- Files that exist by library convention stay: Flyway migrations under `db/flyway/**`, `simplelogger.properties`,
  JUL `logging.properties`, `META-INF/io/opentelemetry/instrumentation/*.properties`, `archetype-metadata.xml`,
  maven-site `site.xml`, Facelets `*.taglib.xml` and `.tld`, Bean Validation message overrides, favicons and
  web-manifest icons, `DEV/org.openl.rules.gen/enums/*.csv` codegen inputs, and a vendored library's own source map.
- `WSFrontend/org.openl.rules.ruleservice` and `.ruleservice.deployer` publish a **test-jar**, so their test classes
  are consumer API. Test types anywhere else are not published and may be deleted once proven unreferenced.
- The tableeditor `compile.js.sh`/`compile.css.sh` and their `.cmd` twins are manual developer tooling wired into
  nothing. Unreferenced by design — this routine itself runs `compile.js.sh`, so they stay.
- Rules-tree and diff icons are referenced by literal path built as `"images/" + name`; they stay.
- Vendored third-party sources are removed whole or not at all — never trim their API. `js/datepicker.js` and
  `js/prototype/prototype-1.7.3.js` in tableeditor are vendored despite not living under a `vendor/` folder; read the
  file header for a third-party licence before treating any `.js` as ours.
- A private method named `readObject`, `writeObject`, `readResolve`, `writeReplace` or `readObjectNoData` is a Java
  serialization hook the JVM calls reflectively — PMD reports it as unused. Never delete one.
- A private field carrying an injection or binding annotation (`@Autowired`, `@Inject`, `@Value`, `@Mock`,
  `@InjectMocks`, `@PersistenceContext`, a Jackson or JAXB annotation) is written by a framework, not by code.
  The same holds for a Maven plugin `@Parameter` field in `Util/openl-maven-plugin`.
- **`dependency:analyze-only` cannot see runtime wiring**, and every "Unused declared" hit in this repository so far
  is one of: `lombok`/`jmh-generator-annprocess` (annotation processors), `jspecify` (CLASS-retention annotations
  the root `AGENTS.md` mandates), `junit-jupiter` (aggregate whose `-api` is what code imports), a `log4j-*-impl` or
  `jaxb-runtime` binding, a Spring/Hibernate/CXF/Kafka module reached by configuration, or a dependency of the
  `ws.all` aggregate that exists to be packaged into the WAR. Prove runtime disuse before removing any of them.

## CI flakes

- `studio-ui` `npm run test` is the standing failure of `Tests (without ITEST)` — tell `Failed to run task:
  'npm run test' failed` plus `-rf :studio-ui`. Always the same file and the same 2 of its 28 tests:
  `src/containers/projects/OverviewPanel.test.tsx`, "edits the descriptor text in place…" (`Test timed out in
  15000ms`) and "edits the sources and the declared dependencies…" (`vitest-fail-on-console` rejecting a React
  `act(...)` warning from `OverviewPanel.tsx:799` and `:1231`). Load, not code — never your own breakage. Reproduce
  it by running that one file with eight busy loops beside it on these four cores: idle 28/28 in 15 s, loaded
  exactly those 2 failing in 35 s. Use this before blaming any frontend red on a diff.
- `LockTest.testSimultaneousMultiThreadsWithWaiting` was fixed on `main` by one shared 90 s deadline. Do not
  re-escalate it.
- **`IT (studio)` has two failure shapes; separate them by duration.** Normal is 6-8 min. (a) Fails in 3-7 min at
  `-rf :itest.studio.repos` with three assertion diffs under `task_EPBDS-15439/100_MergeWithoutConflicts/500-verify`
  — the same-second commit-ordering race, also on `main`, 1 run in 7. (b) Runs far past 8 min with every step
  logging `HttpTimeoutException` at 10001ms — WebStudio under Jetty stopped answering; no test is at fault. Shape
  (b) never self-terminates before the 6 h job limit: cancel the whole run, then `rerun_failed_jobs`, which cleared
  it on the same commit in 7m30s — the proof it is environmental and not the diff.
- A floating container tag can break a job for hours and then fix itself — `apache/kafka-native:latest` segfaulted
  in its own bootstrap, then recovered with no code change. Check for a later green run before escalating one.
- `rerun_failed_jobs` returns 403 until every other job in the run has finished; wait for the run to complete.

## Container facts

- No `gh` CLI. Use the GitHub MCP tools for every PR operation. `git fetch --prune origin` can exceed a 2-minute
  tool timeout on a cold clone; give it 300 s.
- **The whole reactor installs in ~35 min from a cold `~/.m2` with `mvn install -Dquick -DnoPerf -T1C -fae
  -Dmaven.test.skip.exec=true`** — surefire skips execution while test sources still compile. The cache never
  survives a container rebuild, so budget the download every run. `-DskipTests` must still not be used (this repo
  maps it to `maven.test.skip=true`, dropping test compilation).
- For a change confined to one module, `mvn test -pl <module> -am -Dquick -DnoPerf` is far cheaper than the reactor
  and never reaches webstudio, so the opensaml block does not apply to it.
- That install still runs `studio-ui`'s `npm run test` through frontend-maven-plugin, which `maven.test.skip.exec`
  does not gate, and it fails under the parallel load. Add `-pl '!STUDIO/studio-ui'` or expect one FAILURE that
  skips the Studio application module and the Studio ITEST suites. Afterwards the surefire provider jar is absent,
  so a later `mvn -o test` dies on `surefire-junit-platform ... in offline mode` — run the first `mvn test` online.
- **Maven works after seeding one stub.** `org.opensaml:opensaml-bom:5.2.3` is shibboleth-only, the proxy refuses
  `build.shibboleth.net`, and Central 404s on every `org.opensaml` artifact probed. Write a pom with an empty
  `<dependencyManagement>` to `~/.m2/repository/org/opensaml/opensaml-bom/5.2.3/opensaml-bom-5.2.3.pom`, delete the
  `*.lastUpdated` files beside it, and 74 of 82 modules build. Local only — it must never reach a commit. The stub is
  safe: the root pom imports the Bouncy Castle, Jackson and HttpComponents BOMs *above* opensaml-bom, so
  first-declaration-wins had already excluded opensaml's management of them.
- **`STUDIO/org.openl.rules.webstudio` cannot be built here** — `spring-security-saml2-service-provider` pulls
  `opensaml-saml-api`/`opensaml-saml-impl` 4.3.2 as non-optional compile dependencies and both are shibboleth-only.
  No stub helps, because webstudio code imports those classes. Six ITEST Studio suites skip with it. Never delete
  webstudio Java from a run here.
- ITEST modules cannot run `pmd:pmd` or `dependency:analyze-only`: the install does not publish `server-core`, so
  those 16 modules fail dependency resolution. They are out of the sweep's scope anyway.
- Maven Central, registry.npmjs.org and github.com all work. So a dependency jar can be fetched with `curl` straight
  from Central and inspected with `unzip` plus `javap -c` when a convention is in doubt.
- **`sonarcloud.io` is blocked too** — 403 to CONNECT, same shape as shibboleth, confirmed against
  `$HTTPS_PROXY/__agentproxy/status`. So a red `SonarCloud Code Analysis` check can never be diagnosed from here:
  report the failed conditions from the check-run summary and hand the judgement to a maintainer.
- **The container presets a git config that misattributes commits and breaks tests**: `user.name=Claude`,
  `commit.gpgsign=true`, `gpg.format=ssh`. Every JGit commit in `STUDIO/org.openl.rules.repository.git` then dies
  with "No signer for ssh signatures" — 15 test errors that are not a code defect. Unset `commit.gpgsign`,
  `user.signingkey`, `gpg.format`, `gpg.ssh.program` before building.
- **`/root/.gitconfig` is rewritten back to `Claude` while the run is in flight**, so setting only the global
  identity is not enough — it silently reverts. Set `git config --local user.name/user.email` in the clone and in
  every worktree, and repair a bad commit with `git commit --amend --reset-author`: a plain `--amend` keeps the old
  author even when `GIT_AUTHOR_*` is exported. Verify with `git log --pretty='%an <%ae> | %cn <%ce>'`.
- Listing workflow runs through the GitHub MCP tool returns a very large result; ask for `per_page` 3 or less. The
  unit-test workflow is `build-quick.yml`. `get_job_logs` truncates from the **end**, and the reactor summary alone
  fills the last 700 lines, so a failing test name needs `tail_lines` 4000 — which overflows context, so let it save
  to a file and grep the file instead.
- **Angle-bracketed text does not survive the PR-body MCP round trip** — a bare XML element name is swallowed, and
  `Map<String, X>` reads back as `Map` even inside a fenced code block, so a quoted signature silently becomes
  wrong. Keep generics and element names out of bodies, name the identifiers in prose, and re-read after writing.

## Exhausted veins

- Base-name search over every image, `.xhtml` and `.js` file outside `Docs/`, and over all 610 images under `Docs/`.
- Class and id selectors across all 11 hand-written CSS files in STUDIO — no provable orphan but `.te_hidden`.
- Key-reference scan over `openapi.properties` (605 keys), `ValidationMessages.properties` (204),
  `messages.properties` (46), `sql-errors.properties`, and webstudio `openl-default.properties` (105).
- All 1292 leaf keys of the 15 studio-ui locale bundles; every bundle is English, there is no other language.
- `export`ed const/function/class/interface/type/enum across studio-ui `src`, and the import graph over all 345
  modules — every one has a production importer.
- Function, object-literal-method and prototype definitions across all 23 hand-written legacy `.js` files.
- Whole-file reference check over every extension outside ITEST and test-resources: Velocity templates, taglibs,
  source maps, fonts, schemas, `.html`, `.csv`, `.sql`, `.groovy`, `.sh`, `.cmd`, `.txt`, `.json`, `.yaml`, `.xml`,
  `.svg`, and all 117 `.properties` files. Every orphan found is convention-loaded — see *Keep-list*.
- All 62 declared npm dependencies of `STUDIO/studio-ui`, and `@typescript-eslint/no-unused-vars` over all of `src`.
- All 114 distinct properties defined in the 207 poms, and duplicate `<dependency>` / duplicate `.properties` keys.
- **Every Maven profile (14 declarations, 11 distinct ids) and every `pluginManagement` entry (44 declarations over
  25 distinct coordinates) across the 207 poms**, each resolved against `-P` references, activation blocks,
  packaging types and goal invocations. Zero removable — see the *False-positive shapes* entry on pom configuration.
- **PMD 7.17 `UnusedAssignment`, `UnusedLocalVariable`, `UnusedPrivateField`, `UnusedPrivateMethod`,
  `UnusedFormalParameter` over main *and* test sources of all 42 analysable modules** (`includeTests=true`) — 45
  violations, detector validated against the known main-source baseline. No PMD scope is left open.
- **Whole-type deadness over all 3943 `.java` files**: every non-public top-level type (853), each name searched
  repository-wide with a literal word-boundary search over every file type. Zero removable.
- **Member deadness below `private`** — every unannotated package-private and protected method (274) and field (112)
  of the 915 files whose top-level type is non-public, resolved against an identifier index over all 15151 text
  files. 33 had no outside reference and all 33 are read inside their own file. Zero removable.
- **Package-private members of *public* types** — 835 declarations over 593 names, from the union of a brace-tracking
  and an indent-based detector. Every one is alive: read in its own file, reached through a Lombok accessor, or
  written by a framework annotation.
- **Public top-level types declared in test trees** — 193 types over 165 names, ITEST and the two test-jar modules
  excluded, each name resolved against the identifier index and then against every Excel and archive resource. Only
  `JavaType` was removable; the rest are surefire, Spring-scanned, JMH or named from a spreadsheet.
- javac `-Xlint` over the whole reactor: no `UnusedVariable`, `UnusedMethod` or `UnusedNestedClass`; the only
  `EffectivelyPrivate` hits are four constrainer test classes, which is a visibility refactor, not a deletion.
- `dependency:analyze-only` over the 52 resolvable modules — every "Unused declared" hit is covered by the Keep-list
  entry on runtime wiring.
- Incremental `main` scope (SHA reached is in *Resume point*): the only new source surface since the veins above
  closed is studio-ui TraceView — `SimpleTraceTree`, `TraceDetails`, `TraceTree.styles.ts`, `traceStore.ts` — and
  every new export, style key and helper is referenced. Dependabot bumps touch `pom.xml` values only, adding none.
- Every `org.openl.*` class name referenced from `.xml`, `.xhtml`, `.properties`, `.tld`, `.yaml`, `.json` and
  `.txt`, resolved against the source tree. The ~90 with no `.java` are all runtime-generated datatype and
  spreadsheet-result beans or rule-project fixtures under test resources. No stale configuration exists.

## Human follow-ups

- **Allowlist `build.shibboleth.net`** in the environment's network policy, or move the `org.opensaml:opensaml-bom`
  import out of the root pom. `STUDIO/org.openl.rules.webstudio` — the largest untouched module — cannot be compiled
  or swept until then, and the stub has to be re-seeded on every container rebuild.
- `OverviewPanel.tsx` starts two unawaited promises that resolve into state setters, at lines 799 and 1231. Nothing
  synchronises them with the test, so `OverviewPanel.test.tsx` fails whenever the machine is slow. Needs the effect
  awaited or the test made to wait on it; the 15 s per-test timeout is also tight.
- `itest.studio.repos` has a same-second commit-ordering race: when a scenario's two commits land in the same second
  the history endpoint returns them oldest-first, so `task_EPBDS-15439/100_MergeWithoutConflicts/500-verify` reads
  the pre-merge revision and an empty table list. Needs a tiebreaker or a fixture that does not depend on ordering.
- Decide whether the tableeditor JSP taglib is still reachable by a downstream consumer — the largest single
  candidate found so far, and no in-repository evidence can settle it.
- Decide whether the four unused `MergeModal/types.ts` interfaces should stay as the frontend mirror of the merge REST
  contract; if they go, `Docs/api/projects-merge-api.md` moves with them.
- The committed tableeditor CSS bundles do not match what `compile.css.sh` produces from the committed sources, so
  they are stale or hand-edited, and `.te_hidden` is blocked until someone says which side is authoritative. That
  step is wired into no Maven phase, so editing a `js/` or `css/` source silently fails to reach the runtime.

## Run log

- 08-03 — run twenty-five. Unchanged; resume point compacted. No commit to the sweep branch.
- 08-03 — run twenty-six. Swept 4 new `main` commits, no findings. Identified and reproduced the studio-ui
  flake (`OverviewPanel.test.tsx`); LockTest fixed upstream. #1940 description corrected. No sweep commit.
- 08-03 — run twenty-seven. Swept 2 new `main` commits (pom property bumps only), no findings. #1940 green and
  unchanged. Ledger compacted 385 to 374 lines. No sweep commit.
