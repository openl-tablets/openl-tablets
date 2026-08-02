# Dead-code sweep ledger

State memory for the daily sweep of openl-tablets. Read in full at the start of every run.

## Resume point

- Compare `origin/main` with #1940's merge base first. While they are equal there is no new scope: every queue
  row is done and every remaining candidate needs a human, so go straight to maintenance — never invent a
  detector to have something to delete.
- No Java vein is left open. Member deadness below `private` and whole-type deadness are now both closed for
  public and non-public types alike (see *Exhausted veins*). Only webstudio is unswept, and it cannot be built here.
- #1940 carries the sweep. Its `LockTest` check is the `main`-wide red one; do not rerun it, and do not notify
  the owner about it again — that was done once already.
- One `curl` for the `opensaml-bom` pom is the whole scope probe: a 200 from `build.shibboleth.net` unlocks
  webstudio, still CONNECT 403. Seed the stub, git identity and gpg unset only when a build is actually needed.

## Change-type queue

| # | Change type | Status |
|---|---|---|
| 1 | Unreferenced images | done, no findings (incl. all 610 under `Docs/`) |
| 2 | Unreferenced `.xhtml` pages | done, no findings |
| 3 | Dead CSS classes and ids | done, only `.te_hidden`, deferred |
| 4 | Unreferenced `.js` files | done, no findings |
| 5 | Unused `.properties` keys | done, no findings |
| 6 | Unused i18n keys in studio-ui bundles | done, merged in #1933 |
| 7 | Unreferenced exported TS symbols and modules | done, only deferred findings |
| 8 | Dead functions inside legacy `.js` | done, merged in #1933 |
| 9 | Never-read assignments (PMD) | done main+test, 6 removed in #1940 |
| 10 | Unused private fields and locals (PMD) | done, 3 removed in #1940; the other 20 fields deferred |
| 11 | Unused private methods and formal params (PMD) | done main+test, no removable finding |
| 12 | Unused nested / effectively-private types (javac) | done, no findings |
| 13 | Unused declared Maven dependencies | done for 52 modules, no provable finding |
| 14 | Whole-type deadness over Java simple names | done, all 853 non-public top-level types alive |
| 15 | Package-private/protected members of non-public types | done, all 386 examined are alive |
| 16 | Package-private members of public types | done, all 835 declarations are alive |
| 17 | Unreferenced public types in test trees | done, 1 removed in #1940 |

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

- #1933, 12 files / 74 deletions — studio-ui i18n keys plus `PopupMenu.showChild`. Merged with `LockTest` still
  red, so a red `Tests (without ITEST)` does not block a merge here.

## Module coverage

- `STUDIO/studio-ui` — locale bundles, import graph, eslint, npm dependencies: only deferrals left.
- `STUDIO/org.openl.rules.webstudio` — never swept; it cannot be compiled here (see *Container facts*).
- Every other module — PMD (main and test sources), javac, member and whole-type deadness all swept.

## Deferred findings

- `STUDIO/org.openl.rules.tableeditor` `taglib/TableEditorTag.java` (11 unread private fields) and
  `taglib/TableViewerTag.java` (7). Each field is written by a public setter and read by nobody. Both classes are
  named only by `META-INF/tableeditor.tld`. **The whole JSP taglib is the real candidate, and it cannot be settled
  from inside this repository** — the module is a published artifact, so a consumer's own JSP may use the taglib.
  Do not touch the fields alone: the setters are the taglib's declared attribute contract.
- `STUDIO/org.openl.security` `SimpleGroup.description` — written by a public setter and a public constructor
  parameter, read nowhere. Removing it changes public API consumed by webstudio, which cannot be compiled here.
- `DEV/org.openl.rules` `DecisionTableBuilder.methodName` plus its public `setMethodName` and the single call in
  `TableSyntaxNodeDispatcherBuilder:136` — the whole chain is inert, but the setter is public API in `DEV/**`.
- `WSFrontend` `ServiceManagerImpl:230` re-assigns `serviceDescriptionInProcess` to the value already assigned at
  228. Proving 230 dead is an absence-of-path argument through the whole compile subsystem for a one-line payoff.
- `DEV/org.openl.rules.test` `RulesInFolderTestRunner:80,116` — the flagged `messagesCount++` passes its value to
  `error(...)`; only the increment is wasted, so this is an expression rewrite, not a deletion. Out of scope.
- `STUDIO/studio-ui/src/containers/MergeModal/types.ts`: `MergeRequest`, `ResolveConflictsRequest`,
  `ResolveConflictsResponse`, `FileConflictResolution` — unused in TS but mirror a live REST contract documented in
  `Docs/api/projects-merge-api.md` with a Java record and an OpenAPI schema. Needs a human decision.
- ~45 studio-ui exported types used only inside their own file — alive; dropping `export` is a refactor, not a deletion.
- `js/datepicker.js` `dateValidForSelection`, `getSelectedDate`, `setDisabledDays`, `joinNodeLists` — no call site, but
  the file is vendored third party (DatePicker v5.4, frequency-decoder.com, CC BY-SA 3.0) and these are its public API.
- `.te_hidden` in `STUDIO/org.openl.rules.tableeditor/css/common.css` — the only real CSS orphan. Blocked because the
  CSS bundles are not reproducible (see *Method rules*), so the removal cannot be propagated to what ships.
- `STUDIO/org.openl.rules.workspace/resources/deployer.properties` — a `production-repository.$ref` sample no file
  names, in a library jar with no matching application. Its `$ref` syntax is still live, and `{appName}.properties`
  would load it for an app named `deployer`, so only a human knows whether such an app still exists downstream.

## False-positive shapes

- **PMD field-initializer "never used" ignores an early `return` in the constructor.** `CellStyle` returns when its
  argument is null, so `= HorizontalAlignment.GENERAL` is observable. Check every constructor path before deleting.
- **PMD treats an assignment in a `try` as overwritten by one in the `catch`.** `GitRepository` assigns `result` in
  the try and nulls it in the catch; both are live. Any try/catch pair on the same variable reads as a violation.
- **A `= null` initializer before a `try` is required whenever any path can reach a read without the assignment** —
  a `catch`/`finally` that mentions the variable, or a conditional assignment inside the try. Test it against a
  sibling declared in the same block with no initializer: if that compiles, the flagged one is genuinely redundant.
- **An assignment can exist only to host a cast that is the thing under test.** `CastFactoryTest` assigns a
  never-read variable so that `(int[][][][])` throws the `ClassCastException` it asserts; a cast expression cannot
  stand alone as a statement, so the assignment cannot be dropped.
- **A try-with-resources resource reads as an unused local.** Java requires the name, and the block's whole point
  may be the close — `ExtensionsConfigurationTest` asserts on the bean's destruction afterwards.
- **An assignment to `null` before a `gc()` call is the test.** `JsonUtilsTest` drops the strong reference so the
  cache key can be collected. Never delete a null-out that sits near `Runtime.gc()` or a weak/soft reference.
- **Fields of a fixture class handed to a serializer are its contract.** `JsonUtilsTest.BindingClasses` is passed
  to `getCachedObjectMapper` as the binding target, and `KeyClass.field` carries the cache key's identity plus the
  label saying which test owns which mapper. Both read as unused; both are what the test relies on.
- **A private member can exist so a test asserts it is *not* found.** `AOpenClassTest.C.getC()` is what
  `assertNull(findMethod(methods, "getC"))` fails against; deleting it leaves the assertion passing but vacuous.
- **Lombok `@Getter`/`@Setter` generate the accessor, so the field's own name never appears as a read** —
  `TablePart.partName` is reached only as `getPartName`. Search the generated accessor name, not the field.
- **A private field can back a setter that bean introspection discovers.** `JavaOpenClassTest.BeanA.gg` is read
  by no code, but `setGg` is what makes the asserted `gg` property writable. Search for an accessor, not a read.
- **A test bean is usually named from inside an `.xlsx`/`.xls`, which no text search can see.** `Bean1`, `Bean2`,
  `EPBDS7956`, `IChildBean` and `MyProp` are all bound by simple name from a spreadsheet. Before calling any test
  bean dead, unzip every Excel resource and search the name as UTF-8 and as UTF-16LE.
- **A deliberately malformed bean encodes one defect per expected error.** `epbds6830.BeanA` feeds
  `EPBDS-6830_external_datatypes_validation.xlsx.msg.txt`; its private, wrongly-cased `getAB()` is one of them.
  The message list is unchanged by removing it — that is why the loss would be silent. Check the `.msg.txt` first.
- **Surefire's default includes match a `Test` prefix as well as a suffix**, so `TestIf`, `TestAutoType0` and
  friends are executed although nothing names them. Never call a `Test*` class dead.
- **A nested `@Configuration @ComponentScan` keeps every class in its own package alive** — the 17 `appNNN`
  controller fixtures in `spring.openapi` are reached only that way. A JMH `@Benchmark` class is likewise run by
  the harness and named by nothing.
- **A `@Component`/`@Service` class implementing an interface is injected by interface type**, so its own name
  appears in no other file — `FileNodeMapperImpl`. Check for a stereotype annotation before believing a class is orphaned.
- **A field can be written so that a call made on the next line reads it back.** `ServiceManagerImpl` sets
  `serviceDescriptionInProcess` before `createService`, which reaches it through `getRulesDeployInProcess()`. Look
  for a getter on the same field before calling any field assignment dead.
- **A field can be blanked to change what a resolver returns, then restored.** `DynamicPropertySource` sets
  `settings = Map.of()` so `resolver.getRawProperty` falls through to defaults. Self-referential property sources
  defeat dataflow entirely.
- A `for (Object item : c) { size++; }` counting loop reports `item` as an unused local; the variable is required
  syntax and cannot be removed.
- i18next appends `_one`/`_other` itself when `count` is passed; check the plural-stripped base before deleting.
- A locale key reached only through a template literal: enumerate `t(` + backtick call sites, treat each composed
  prefix as keeping its whole family alive. `t(someKeyVariable)` means the literals sit at the call sites instead.
- Never test a CSS class by "does this token appear anywhere" — `ui-layout-*`, `tooltip_*` and `te_toolbar_*` are all
  built by string concatenation in JS or Java.
- `rf-*` classes cannot be proven dead (RichFaces ships its JS inside a jar); `ant-*` come from antd at runtime.
- A regex of `#[a-zA-Z0-9]+` over CSS reports every hex colour as an id selector. Filter hex before reading results.
- `.properties` keys are routinely assembled from a prefix plus a runtime segment — see Keep-list for each convention.
- An interface with zero code references can still be a documented API contract mirrored from the backend.
- `\b` does not match before `$`, so a `\b`-anchored search finds no call site for a `$`-leading name. Use
  `(?<![\w$])name(?![\w$])`.
- A name defined as a key in an options object literal passed to a framework is a callback, not something anyone calls
  by name — `onFailure` in `TableEditor.js` belongs to `new Ajax.Request(...)`. Check the enclosing call first.
- A module can be imported by a specifier that already carries its extension (`from './App.styles.ts'`), which
  defeats a resolver that only appends `.ts`/`.tsx`. Try the specifier verbatim before appending.
- A "dead" JS function is usually called from inside its own file — exclude the defining file from the search and
  every private helper looks unreferenced.
- **A dependency jar can read a resource by a name hardcoded in its own bytecode.** CXF's `AbstractHTTPServlet`
  loads `/cxfServletStaticResourcesMap.txt`, so that file is named by no file in this repository and is still
  load-bearing. When a resource name looks invented-but-conventional, fetch the owning jar and grep its constants.
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
- **Run two independent Java detectors and resolve the union.** A brace-tracking parser silently drops members
  behind an annotation or a multi-line signature; an indent-based one drops nested members and reports interface
  members, which are implicitly public. Neither alone is trustworthy; their disagreement is where the bugs are.
- **A production package can be named `test`** — `webstudio/src/.../web/test/`. Detect a test tree by the module's
  test source root, never by a path segment.
- **This clone is shallow (50 commits).** Its earliest commit "adds" all 15032 files, so `git log --diff-filter=A`
  attributes every older file to that graft and history proves nothing about a file's origin. Never argue from it.
- Resolve studio-ui imports with tsconfig `paths` `"*": ["./src/*"]` (a bare specifier is `src/`-relative), and follow
  side-effect `import 'x'` and `vi.mock('x')` too, or barrel-only and test-only modules look dead.
- The tableeditor JS bundles `js/tableeditor.all.js` and `js/tableeditor.min.js` are checked in and are what the
  runtime loads, not the individual sources. `bash compile.js.sh` reproduces both byte-for-byte from the unmodified
  sources using the checked-in `yuicompressor-2.4.7.jar`, so any source removal must regenerate them in the same
  commit. `compile.css.sh` does **not** reproduce the committed CSS bundles — never regenerate those.
- Run the frontend gate from `STUDIO/studio-ui` with no Maven build competing: `npx tsc --noEmit`, `npx eslint src`
  (whole tree), and `npm run test` — **which is `vitest run --coverage`, not plain `vitest run`; match CI's own
  command or the coverage pass goes unverified**. Baseline 164 files / 1455 tests green. `no-unused-vars` is `warn`,
  so read the output, not the exit code.
- `npm ci` works here — registry.npmjs.org bypasses the proxy. `node_modules` is gitignored.
- When a deletion empties a parent object literal, delete the parent in the same commit.
- A PR body loses angle-bracketed placeholders even inside backticks — write such a segment as prose, then re-read the
  stored body to confirm.
- Documentation is this repository's approved source of truth, so a `Docs/` markdown page that nothing links to is
  not a deletion candidate — Jekyll publishes it regardless. Treat the whole `Docs/` prose tree as out of scope.
- **PMD report XML uses namespace `http://pmd.sourceforge.net/report/2.0.0`** (slash-dot, not underscores). A parser
  built for the ruleset namespace silently returns zero violations from a non-empty report.
- Most `target/pmd.xml` files here contain only `<suppressedviolation>` elements — grep `<violation ` to find the
  reports that matter. The repository already annotates its JAXB `beforeMarshal`/`afterUnmarshal` hooks with
  `@SuppressWarnings`, which is why row 11 comes back empty on main sources.
- The `pmd` plugin prefix does not resolve with `-o` on a cold cache. Invoke the goal by full coordinates,
  `mvn org.apache.maven.plugins:maven-pmd-plugin:3.28.0:pmd -fae`, and run it online the first time.
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

- `LockTest.testSimultaneousMultiThreadsWithWaiting` in `STUDIO/org.openl.rules.repository` fails on **`main`**, not
  just on sweep branches — job `Tests (without ITEST)`, tell `expected: <800> but was: <79x>`. It asserts all 8x100
  `tryLock` attempts beat a 30 s timeout. Do not rerun and do not treat it as your own breakage: check the latest
  `main` run of `build-quick.yml` first, then say so in the thread once. It passes on this container.
- `studio-ui` `npm run test` is the same job's other failure mode — tell `Failed to run task: 'npm run test' failed`
  plus `-rf :studio-ui`. It is CPU starvation, not code: it also fails inside this container's own `-T1C` reactor
  build, where `OverviewPanel.test.tsx` lost 3 of 28 tests to 15 s timeouts in a file that took 79 s against ~4 min
  for the whole suite on an idle machine. Rerun the job; never judge the frontend from a run that shared the CPU.
- **`Tests (without ITEST)` has two independent causes; identify which one fired before answering.** Read the
  `-rf :<module>` hint: `org.openl.rules.repository` is the LockTest timeout, `studio-ui` is the frontend flake.
  They alternate — each has passed on a run where the other failed, so a green studio-ui does not mean a green job.
- **`IT (studio)` has two failure shapes; separate them by duration.** Normal is 6-8 min. (a) Fails in 3-7 min at
  `-rf :itest.studio.repos` with three assertion diffs under `task_EPBDS-15439/100_MergeWithoutConflicts/500-verify`
  — the same-second commit-ordering race, also on `main`, 1 run in 7. (b) Runs far past 8 min with every step
  logging `HttpTimeoutException` at 10001ms — WebStudio under Jetty stopped answering; no test is at fault.
- Shape (b) never self-terminates before the 6 h job limit. Cancel the whole run, then `rerun_failed_jobs` — this
  cleared it on the same commit in 7m30s, which is the proof it is environmental and not the diff.
- A floating container tag can break a job for hours and then fix itself: `apache/kafka-native:latest` segfaulted
  in its own native-image bootstrap and `IT (services-data)` went green again a day later with no code change.
  Before escalating an image failure, check whether a later run of the same job already recovered.
- `rerun_failed_jobs` returns 403 "This workflow is already running" until every other job in the run has finished;
  wait for the run to complete before retrying.

## Container facts

- No `gh` CLI. Use the GitHub MCP tools for every PR operation.
- **The whole reactor installs in ~35 min from a cold `~/.m2` with `mvn install -Dquick -DnoPerf -T1C -fae
  -Dmaven.test.skip.exec=true`** — surefire skips execution while test sources still compile. The cache never
  survives a container rebuild, so budget the download every run. `-DskipTests` still must not be used (this repo
  maps it to `maven.test.skip=true`, dropping test compilation).
- For a change confined to one module, `mvn test -pl <module> -am -Dquick -DnoPerf` is far cheaper than the reactor
  and never reaches webstudio, so the opensaml block does not apply to it.
- That install still runs `studio-ui`'s `npm run test` through frontend-maven-plugin, which `maven.test.skip.exec`
  does not gate, and it fails under the parallel load. Add `-pl '!STUDIO/studio-ui'` or expect one FAILURE that
  skips the Studio application module and the Studio ITEST suites.
- After such an install the surefire provider jar is absent, so a later `mvn -o test` dies on
  `surefire-junit-platform ... in offline mode`. Run the first `mvn test` online.
- **Maven works after seeding one stub.** `org.opensaml:opensaml-bom:5.2.3` is shibboleth-only, the proxy denies
  CONNECT to `build.shibboleth.net` (403), and Central carries only 4.0.x. Write a pom with an empty
  `<dependencyManagement>` to `~/.m2/repository/org/opensaml/opensaml-bom/5.2.3/opensaml-bom-5.2.3.pom`, delete the
  `*.lastUpdated` files beside it, and 74 of 82 modules build. Local only — it must never reach a commit.
- The stub is safe: the root pom already imports the Bouncy Castle, Jackson and HttpComponents BOMs *above*
  opensaml-bom, so first-declaration-wins had already excluded opensaml's management of them.
- **`STUDIO/org.openl.rules.webstudio` cannot be built here** — confirmed: `spring-security-saml2-service-provider`
  pulls `opensaml-saml-api`/`opensaml-saml-impl` 4.3.2 as non-optional compile dependencies and both are
  shibboleth-only. Six ITEST Studio suites skip with it. Never delete webstudio Java from a run here.
- ITEST modules cannot run `pmd:pmd` or `dependency:analyze-only`: the install does not publish `server-core`, so
  those 16 modules fail dependency resolution. They are out of the sweep's scope anyway.
- Maven Central, registry.npmjs.org and github.com all work; only shibboleth is blocked. So a dependency jar can be
  fetched with `curl` straight from Central and inspected with `unzip` plus `javap -c` when a convention is in doubt.
- **The container presets a git config that misattributes commits and breaks tests**: `user.name=Claude`,
  `commit.gpgsign=true`, `gpg.format=ssh`. Every JGit commit in `STUDIO/org.openl.rules.repository.git` then dies
  with "No signer for ssh signatures" — 15 test errors that are not a code defect. Unset `commit.gpgsign`,
  `user.signingkey`, `gpg.format`, `gpg.ssh.program` before building.
- **`/root/.gitconfig` is rewritten back to `Claude` while the run is in flight**, so setting only the global
  identity is not enough — it silently reverts. Set `git config --local user.name/user.email` in the clone too,
  and repair a bad commit with `git commit --amend --reset-author`: a plain `--amend` keeps the old author even
  when `GIT_AUTHOR_*` is exported. Verify with `git log --pretty='%an <%ae> | %cn <%ce>'`, which shows both sides.
- Listing workflow runs through the GitHub MCP tool returns a very large result; ask for `per_page` 3 or less.
  The unit-test workflow is `build-quick.yml`.

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
- **PMD 7.17 `UnusedAssignment`, `UnusedLocalVariable`, `UnusedPrivateField`, `UnusedPrivateMethod`,
  `UnusedFormalParameter` over main *and* test sources of all 42 analysable modules** (`includeTests=true`) — 45
  violations, detector validated by reproducing the known main-source baseline. No PMD scope is left open.
- **Whole-type deadness over all 3943 `.java` files**: every non-public top-level type (853 of them), each name
  searched repository-wide with a literal word-boundary search over every file type. Zero removable.
- **Member deadness below `private` — the gap between PMD (private only) and whole-type deadness.** Every
  unannotated package-private and protected method (274) and field (112) of the 915 files whose top-level type is
  non-public, each name resolved against an identifier index over all 15151 text files. 33 had no outside
  reference and all 33 are read inside their own file. Zero removable; do not repeat this scan.
- **Package-private members of *public* types** — 835 declarations over 593 names, from the union of a
  brace-tracking and an indent-based detector, resolved against the identifier index. Every one is alive: read in
  its own file, reached through a Lombok accessor, or written by a framework annotation. Zero removable.
- **Public top-level types declared in test trees** — 193 types over 165 names, ITEST and the two test-jar modules
  excluded, each name resolved against the identifier index and then against every Excel and archive resource.
  Only `JavaType` was removable; the rest are surefire, Spring-scanned, JMH or named from a spreadsheet.
- javac `-Xlint` over the whole reactor: no `UnusedVariable`, `UnusedMethod` or `UnusedNestedClass`; the only
  `EffectivelyPrivate` hits are four constrainer test classes, which is a visibility refactor, not a deletion.
- `dependency:analyze-only` over the 52 resolvable modules — every "Unused declared" hit is covered by the Keep-list
  entry on runtime wiring.
- Every `org.openl.*` class name referenced from `.xml`, `.xhtml`, `.properties`, `.tld`, `.yaml`, `.json` and
  `.txt`, resolved against the source tree. The ~90 with no `.java` are all runtime-generated datatype and
  spreadsheet-result beans or rule-project fixtures under test resources. No stale configuration exists; do not
  repeat this scan.

## Human follow-ups

- **Allowlist `build.shibboleth.net`** in the environment's network policy, or move the `org.opensaml:opensaml-bom`
  import out of the root pom. `STUDIO/org.openl.rules.webstudio` — the largest untouched module — cannot be compiled
  or swept until then, and the stub has to be re-seeded on every container rebuild.
- `main` is red: `LockTest.testSimultaneousMultiThreadsWithWaiting` fails on all 7 of the last 7 `main` runs, so
  no pull request can reach a fully green CI. Its sibling is already `@Disabled` as unstable; this one needs the same
  decision or a real fix. It passes on this container, so it is load-sensitive, not broken.
- `itest.studio.repos` has a same-second commit-ordering race: when a scenario's two commits land in the same second
  the history endpoint returns them oldest-first, so `task_EPBDS-15439/100_MergeWithoutConflicts/500-verify` reads
  the pre-merge revision and an empty table list. Needs a tiebreaker in the ordering or a fixture that does not
  depend on it. Intermittent on `main`, so it will keep costing pull requests a rerun.
- Decide whether the tableeditor JSP taglib is still reachable by a downstream consumer — the largest single
  candidate found so far, and no in-repository evidence can settle it.
- Decide whether the four unused `MergeModal/types.ts` interfaces should stay as the frontend mirror of the merge REST
  contract; if they go, `Docs/api/projects-merge-api.md` moves with them.
- The committed tableeditor CSS bundles do not match what `compile.css.sh` produces from the committed sources, so
  they are stale or hand-edited, and `.te_hidden` is blocked until someone says which side is authoritative. That
  bundling step is documented nowhere and is wired into no Maven phase, so editing a `js/` or `css/` source
  silently fails to reach the runtime — worth a note in `STUDIO/AGENTS.md`; this routine only deletes.

## Run log

- 08-02 — thirteenth run. Fifth idle run; only product was compaction, 355 to 350 lines.
- 08-02 — fourteenth run. Opened and closed the last member vein below `private`: zero removable, no commit.
- 08-02 — fifteenth run. Closed the two remaining Java surfaces — package-private members of public types (zero
  removable) and public types in test trees (one: the `JavaType` fixture, committed to #1940).
