# Dead-code sweep ledger

## Resume point

- **Converged**: every queue row and vein is done, and `main` has yielded nothing deletable for many runs. Screened
  and closed up to *Write a spreadsheet result of a test as the bean a client can read* — match that **subject** in
  `git log --oneline -25 origin/main`, since a fresh clone loses the SHA — plus every Dependabot or lock-file bump
  above it, which add no surface and need no reading. New scope is only the authored commits above that subject.
  Never invent a detector; never re-diagnose CI on an unchanged SHA. The idle pass is two calls, those commits and
  the open-PR baseline.
- Sweep only what a new commit touches, skipping webstudio Java, ITEST fixtures, `Docs/` and `.github/`; read its
  **deleted** lines first, and check every locale key, image and model an additive commit *adds* repo-wide too. A
  commit deleting a screen is the richest vein: its locale keys, service functions, helper modules, dropped `throws`
  and class/id selectors over `webstudio/webapp/css`. Search a dropped helper **per method** — the class survives
  through its other callers while the dropped method keeps only its own test.

## Change-type queue

All 19 change types are **done**; *Exhausted veins* records the scope each covered. A new row is warranted only by a
detector this ledger has never run — not by re-running one of these.

## Open PR

None. Open the next one from a fresh branch off the current `main`.

## Merged PRs

- #2004, 11 CSS lines, merged after four days green and unreviewed — a deletion PR waits on a maintainer, and
  `mergeable_state` `blocked` or `unknown` is that wait, never a conflict. Settle mergeability locally with
  `git merge-tree --write-tree origin/main <branch>`; never push a catch-up merge to re-run green CI. Head SHA,
  comment count and `updated_at` unchanged together settle the whole PR check in one call.

## Module coverage

- `STUDIO/org.openl.rules.webstudio` Java is the one unswept surface (see *Container facts*); its resources are
  covered by the repo-wide veins. Every other module is swept, leaving only *Deferred findings*.

## Deferred findings

- `STUDIO/org.openl.rules.tableeditor` `taglib/TableEditorTag.java` (11 unread private fields) and
  `TableViewerTag.java` (7): written by public setters, read by nobody, named only by `META-INF/tableeditor.tld`.
  Never touch the fields alone — the setters are the taglib's declared attribute contract. Whether a downstream
  consumer still reaches this JSP taglib is the largest open question here and no in-repository evidence settles it.
- `STUDIO/org.openl.rules.workspace` `dtr/RepositoryException` — a public class with zero repository-wide references
  since EPBDS-8537 dropped the `throws` on `DesignTimeRepositoryImpl.init()`. A whole public type in a published
  artifact, so a downstream `catch` may still need it.
- `STUDIO/org.openl.rules.repository` `BranchRepository` — the four `@Deprecated(forRemoval = true)` default methods
  (`createBranch` twice, `deleteBranch(String, String)`, `getBranches(String)`) have no caller of their signature
  left; everything moved to `listBranches`/`createRepositoryBranch`. Public API, so a human removes them.
- `DEV/org.openl.commons` `FileSignatureHelper.isOle2Sign` — its only production caller was
  `ProjectFilesServiceImpl.validateFileSignature`, which EPBDS-16379 replaced with `FileIntegrityValidator`; only its
  own test names it now. Public API, and its siblings `isArchiveSign`/`isEmptyArchive` stay live.
- `STUDIO/org.openl.rules.jackson` `JsonUtils.fromJSON` — all four public static overloads (one already
  `@Deprecated`) lost their last production callers when EPBDS-16460 rewrote `InputArgsBean` and
  `TableInputParserServiceImpl`; only `JsonUtilsTest` names them, in text and in every binary resource. The sibling
  `splitJSON` stays live. Public API in a published jar, so a human decides.
- `STUDIO/org.openl.security` `SimpleGroup.description` — written by a public setter and a public constructor
  parameter, read nowhere. Removing it changes public API consumed by webstudio, which cannot be compiled here.
- `DEV/org.openl.rules` `DecisionTableBuilder.methodName` plus its public `setMethodName` and the single call in
  `TableSyntaxNodeDispatcherBuilder:136` — the whole chain is inert, but the setter is public API in `DEV/**`.
- Two PMD hits are expression rewrites, not deletions, and stay: `WSFrontend` `ServiceManagerImpl:230` re-assigning
  `serviceDescriptionInProcess` to the value already at 228 (proof needs an absence-of-path argument through the whole
  compile subsystem), and `DEV/org.openl.rules.test` `RulesInFolderTestRunner:80,116`, where the flagged
  `messagesCount++` passes its value on so only the increment is wasted.
- `STUDIO/studio-ui/src/containers/MergeModal/types.ts`: `MergeRequest`, `ResolveConflictsRequest`,
  `ResolveConflictsResponse`, `FileConflictResolution` — unused in TS but mirror a live REST contract in
  `Docs/api/projects-merge-api.md` backed by a Java record and an OpenAPI schema; if they go, that page goes too.
- `.te_hidden` in `STUDIO/org.openl.rules.tableeditor/css/common.css` — the only real CSS orphan. Blocked because the
  CSS bundles are not reproducible (see *Method rules*), so the removal cannot be propagated to what ships.
- webstudio `webapp/javascript/common.js:127` guards a submit handler with `!$submit.hasClass('own-loader-handler')`;
  the class is carried by no element since the Export dialogs went to React, but dropping the test rewrites a live
  condition rather than deleting a declaration, and its explanatory comment would go with it.
- `STUDIO/org.openl.rules.workspace/resources/deployer.properties` — a `production-repository.$ref` sample no file
  names, in a library jar with no matching application. `{appName}.properties` would load it for an app named
  `deployer`, so only a human knows whether one still exists downstream.

## False-positive shapes

- **An initializer a detector calls overwritten is live whenever any path reaches a read without the later
  assignment.** Three ways it misses one: an early `return` in the constructor (`CellStyle`), an assignment in a `try`
  it treats as overwritten by the `catch` (`GitRepository.result`), and any conditional assignment. Check every branch
  path; a sibling declared in the same block with no initializer that still compiles proves it.
- **A never-read local can be the test itself.** Three shapes: a variable hosting only a cast whose failure is
  asserted (a cast cannot stand alone as a statement); a try-with-resources name Java requires, where the close is the
  point; and a null-out near `Runtime.gc()` or a weak reference, dropping the strong reference so collection can
  happen. Read the assertions first. A `for (Object item : c) { size++; }` counting loop likewise needs its variable.
- **A test can consume a member by its mere existence.** `JsonUtilsTest.BindingClasses` is the binding target passed
  to `getCachedObjectMapper` and `KeyClass.field` carries the cache key's identity; `AOpenClassTest.C.getC()` is what
  `assertNull(findMethod(methods, "getC"))` fails against, so deleting it leaves the assertion vacuous.
- **Search an accessor by the property name — the field's own name may never appear as a read.** Lombok
  `@Getter`/`@Setter` generate it, so `TablePart.partName` is reached only as `getPartName`; and a private field can
  exist solely to make a bean property writable — `JavaOpenClassTest.BeanA.gg` is read by no code, but `setGg` is what
  makes the asserted `gg` property work. Lombok also *replaces* deleted code, so read a diff's **added annotations**
  first: a dropped hand-written `@Autowired` constructor orphans none of its parameters once `@RequiredArgsConstructor`
  is on the class, and a dropped nested `Builder` with its `builder()` factory orphans nothing once `@Builder` is.
- **A test bean is usually named from inside an `.xlsx`/`.xls`, which no text search can see** — `Bean1`, `Bean2`,
  `EPBDS7956`, `IChildBean`, `MyProp`. Unzip every Excel resource and search as UTF-8 and UTF-16LE first. A
  deliberately malformed bean is worse: `epbds6830.BeanA` feeds
  `EPBDS-6830_external_datatypes_validation.xlsx.msg.txt` with one defect per expected error, and its private,
  wrongly-cased `getAB()` is one of them — the message list is unchanged by removing it, so the loss is silent.
- **Surefire's default includes match a `Test` prefix as well as a suffix**, so `TestIf`, `TestAutoType0` and friends
  are executed although nothing names them. Never call a `Test*` class dead. A nested `@Configuration @ComponentScan`
  likewise keeps every class in its own package alive (the 17 `appNNN` fixtures in `spring.openapi`), and a JMH
  `@Benchmark` class is run by the harness and named by nothing.
- **A `@Component`/`@Service` class implementing an interface is injected by interface type**, so its own name appears
  in no other file — `FileNodeMapperImpl`. Check for a stereotype annotation before believing it is orphaned.
- **A field can be written for the next line to read back, or blanked to change what a resolver returns.**
  `ServiceManagerImpl` sets `serviceDescriptionInProcess` before `createService`, which reaches it through
  `getRulesDeployInProcess()`; `DynamicPropertySource` sets `settings = Map.of()` so `resolver.getRawProperty` falls
  through to defaults. Look for a getter, and for self-reference, before calling a field assignment dead.
- **No pom configuration shape here is ever deletable.** A `pluginManagement` entry needs no `<plugin>` consumer: it
  pins a plugin bound by a lifecycle default (`deploy`, `site`), by a packaging type (`maven-archetype`), or invoked
  as a bare goal (`release:prepare`, `scm:`, `license:`). A profile with no `-P` reference is alive when a property
  activates it (`quick`, `skipTests`, `!noPerf`, `noDocker`, `sonar`, `!sonar`, `!skipTests`, `env.CI`) or when it is
  documented tooling. An `<exclusions>` entry is defensive and stays correct even when the excluded artifact is absent
  today, because a transitive upgrade can reintroduce it. A hand-written CVE pin looks superseded once Dependabot
  bumps its property, but the `dependencyManagement` import is what overrides the parent BOM, and its comment names
  the fix condition rather than the version, so a bump leaves it accurate. A pom property with no `${...}` reference
  is almost always a plugin convention parameter — `maven.*`, `sonar.*`, `invoker.*`, `archetype.*`, `spotless.*`.
  A `<resource>`/`<testResource>` `<directory>` naming a path that does not exist beside its own pom is inherited
  configuration resolved against each *module's* basedir: the root's `resources`, `src` and `test-resources`, and a
  fixture parent's, are exactly that shape. An archetype's `archetype-resources/pom.xml` is a Velocity template and
  does not parse as XML at all.
- **A pom outside the root aggregator's `<module>` graph is normally a fixture, not an orphan.** All 120 unreachable
  poms are maven-invoker projects under `Util/openl-maven-plugin/it/**` or documentation examples under `Docs/`.
- **A signature an incremental diff deletes is usually renamed or moved, not removed.** Six shapes seen: renamed on
  the same stem (`convertRegexToGlob` to `convertRegexToGlobs`); an inline local promoted to a constant; moved into a
  shared module the diff newly imports, or into a new same-package class the commit itself adds; a deleted **inline
  condition** moved into a new method of a collaborator the caller already constructs; the *same* lines dropped from
  several files promoted to one new named export; and two siblings swapping, where `ProjectIdModel.encode` went while
  `encodeUrlSafe` took over its name and its `@JsonValue`. So read the commit's **added files** and added imports,
  search the added lines for the stem, and always grep the **post-image** file — a deleted `import` or JSX line
  returns with one more name on it.
- **A fixture can exist so a test asserts it is *absent* from the output** — the empty `assembly-template.xml` in the
  `openl-child-dependency` invoker project is named only by a negated assert in `openl-multiproject/verify.groovy`
  proving the pom's `**/assembly/*` exclude works. Read the one reference's polarity before calling a file orphaned.
- Never test a CSS class by "does this token appear anywhere" — `ui-layout-*`, `tooltip_*` and `te_toolbar_*` are all
  built by string concatenation in JS or Java. `rf-*` cannot be proven dead (RichFaces ships its JS inside a jar) and
  `ant-*` come from antd at runtime. A regex of `#[a-zA-Z0-9]+` over CSS also reports every hex colour as an id.
- i18next appends `_one`/`_other` itself when `count` is passed; check the plural-stripped base before deleting. A
  locale key reached only through a template literal needs its `t(` + backtick call sites enumerated, each composed
  prefix keeping its whole family alive; `t(someKeyVariable)` means the literals sit at the call sites instead.
- `\b` does not match before `$`, so a `\b`-anchored search finds no call site for a `$`-leading name. Use
  `(?<![\w$])name(?![\w$])`. Mixing `grep -E` with `-P` errors out, so hidden stderr reads every name dead.
- A name defined as a key in an options object literal passed to a framework is a callback, not something anyone calls
  by name — `onFailure` in `TableEditor.js` belongs to `new Ajax.Request(...)`. Check the enclosing call first.
- A module can be imported by a specifier that already carries its extension (`from './App.styles.ts'`), which
  defeats a resolver that only appends `.ts`/`.tsx`. Try the specifier verbatim before appending.
- A "dead" JS function is usually called from inside its own file — exclude the defining file from the search and
  every private helper looks unreferenced. A studio-ui export used only inside its own file is likewise alive, and
  dropping the `export` keyword is a refactor this routine may not make: never list one.
- **A dependency jar can read a resource by a name hardcoded in its own bytecode.** CXF's `AbstractHTTPServlet` loads
  `/cxfServletStaticResourcesMap.txt`, so that file is named by no file here and is still load-bearing. When a
  resource name looks invented-but-conventional, fetch the owning jar and grep its constants.
- A CodeRabbit walkthrough describes intent, not the diff — it reported an insertion in a file with zero insertions.
  Verify any bot claim against `git diff --numstat`; its "possibly related PRs" is a similarity hint, not a claim.

## Method rules

- **Screen an incremental commit mechanically**: per changed file, take the identifiers of its deleted lines minus the
  identifiers of its post-image; an empty result proves the file orphaned nothing and needs no reading. A residual of
  ordinary English words is the same proof — a JavaDoc or `.properties` description rewrite drops prose, not names;
  check only residuals that could be a type or a member, and a `.properties` diff that changes values while every key
  survives orphans nothing.
- For any deadness check on a type **or a member**, "appears in one file" is not enough — count occurrences
  **inside** that file too. This one rule killed every member candidate ever raised: a secondary top-level class used
  by its file's primary class, a helper called only by its own file's other methods, and a TS type alias read only by
  a sibling interface's field, all look orphaned.
- A maven-release-plugin pair (*prepare release*, *prepare for next development iteration*) rewrites every pom but
  deletes only version and scm tag values, so it orphans nothing — screen it by that deleted-line set and skip it.
- For a bundle key, search the full dotted path **and** the bare leaf name; either hit means keep.
- Validate any bulk detector on two fabricated names *and* one known-live name — a fabricated hit or a live miss
  means the search is wrong. For a linter, plant a violation and confirm it is reported.
- **A production package can be named `test`** — `webstudio/src/.../web/test/`. Detect a test tree by the module's
  test source root, never by a path segment.
- **This clone is shallow (50 commits).** Its earliest commit "adds" all 15032 files, so `git log --diff-filter=A`
  attributes every older file to that graft and history proves nothing about a file's origin.
- Resolve studio-ui imports with tsconfig `paths` `"*": ["./src/*"]` (a bare specifier is `src/`-relative), and follow
  side-effect `import 'x'` and `vi.mock('x')` too, or barrel-only and test-only modules look dead.
- The tableeditor JS bundles `js/tableeditor.all.js` and `js/tableeditor.min.js` are checked in and are what the
  runtime loads, not the individual sources. `bash compile.js.sh` reproduces both byte-for-byte using the checked-in
  `yuicompressor-2.4.7.jar`, so a source removal must regenerate them in the same commit. `compile.css.sh` does
  **not** reproduce the committed CSS bundles — never regenerate those.
- Run the frontend gate from `STUDIO/studio-ui` with no Maven build competing: `npx tsc --noEmit`, `npx eslint src`,
  and `npm run test` — **which is `vitest run --coverage`, not plain `vitest run`; match CI's own command or the
  coverage pass goes unverified**. Baseline 164 files / 1460 tests. `no-unused-vars` is `warn`, so read the output,
  not the exit code. `npm ci` works here; `node_modules` is gitignored.
- When a deletion empties a parent object literal, delete the parent in the same commit.
- The whole `Docs/` prose tree is out of scope: Jekyll publishes every page and documentation is this repository's
  approved source of truth, so an unlinked or duplicated page is not a candidate.
- GitHub Actions compiles every push against the real opensaml BOM, so CI is the authoritative gate on anything the
  locally stubbed build could not verify.
- **Never edit a source file while a Maven build is running** — a half-applied edit reads as a genuine compile error
  and costs the whole reactor pass. Finish the edits, then build. `pgrep -f <pattern>` matches the polling shell
  itself, so a wait loop on it never exits — poll the log's mtime.

## Keep-list

- `RestRuntimeException.getErrorCode()` builds `openl.error.` + HTTP status + the code passed to the exception, so a
  `ValidationMessages.properties` key is only ever written as its suffix in Java. Search by suffix.
- A JSF page reaching a bundle as a prefix string concatenated with a lowercased enum name keeps that whole
  `messages.properties` family alive.
- Property names composed at runtime stay: `repo-default.` or `repo-` plus the repository type (`jdbc`, `jndi`, `git`,
  `azure`) plus the suffix, `repository.` plus the id plus `.settings.`, and `openl-db-repository-` plus the database
  product name; the reference-key suffix is exercised from ITEST init params. An `openl-default.properties` key is
  therefore routinely written in full by its own defaults file alone, and a `hibernate.*`/`hikari.*` one is
  library-read.
- `ApplicationPropertySource` loads `classpath:{appName}.properties`, so a properties file at a jar root can be alive
  through the deployed application's name alone — `DEMO/webservice.properties` is exactly that shape.
- Spring pulls every `META-INF/openl/extension-*.xml` in through `@ImportResource("classpath*:...")` in
  `ExtensionsConfiguration`, so those bean files are never referenced by name.
- Files that exist by library convention stay: Flyway migrations under `db/flyway/**`, `simplelogger.properties`,
  JUL `logging.properties`, `META-INF/io/opentelemetry/instrumentation/*.properties`, `archetype-metadata.xml`,
  maven-site `site.xml`, Facelets `*.taglib.xml` and `.tld`, Bean Validation message overrides, favicons and
  web-manifest icons, `DEV/org.openl.rules.gen/enums/*.csv` codegen inputs, and a vendored library's own source map.
- Every `META-INF/services` file is reached by `ServiceLoader` alone, so no file names it.
- `WSFrontend/org.openl.rules.ruleservice` and `.ruleservice.deployer` publish a **test-jar**, so their test classes
  are consumer API. Test types anywhere else are not published and may be deleted once proven unreferenced.
- The tableeditor `compile.js.sh`/`compile.css.sh` and their `.cmd` twins are manual developer tooling wired into
  nothing. Unreferenced by design — this routine itself runs `compile.js.sh`, so they stay.
- Rules-tree and diff icons are referenced by literal path built as `"images/" + name`; they stay.
- Vendored third-party sources are removed whole or not at all — never trim their API. `js/datepicker.js` and
  `js/prototype/prototype-1.7.3.js` in tableeditor are vendored despite not living under a `vendor/` folder; read the
  file header for a third-party licence before treating any `.js` as ours.
- A private method named `readObject`, `writeObject`, `readResolve`, `writeReplace` or `readObjectNoData` is a Java
  serialization hook the JVM calls reflectively — a detector reports it as unused. Never delete one.
- A private field carrying an injection or binding annotation (`@Autowired`, `@Inject`, `@Value`, `@Mock`,
  `@InjectMocks`, `@PersistenceContext`, a Jackson or JAXB annotation) is written by a framework, not by code.
  The same holds for a Maven plugin `@Parameter` field in `Util/openl-maven-plugin`.
- **`dependency:analyze-only` cannot see runtime wiring**, and every "Unused declared" hit in this repository so far
  is one of: `lombok`/`jmh-generator-annprocess` (annotation processors), `jspecify` (CLASS-retention annotations
  the root `AGENTS.md` mandates), `junit-jupiter` (aggregate whose `-api` is what code imports), a `log4j-*-impl` or
  `jaxb-runtime` binding, a Spring/Hibernate/CXF/Kafka module reached by configuration, or a dependency of the
  `ws.all` aggregate that exists to be packaged into the WAR. Prove runtime disuse before removing any of them.

## CI flakes

- **`apache/kafka-native:latest` is a floating tag that breaks a job for hours, then fixes itself** with no code
  change; it has reddened `main` for four commits at a stretch. The job is `IT (services-data)` (none is named for
  kafka or tracing); the failing module is `itest.tracing` (`RunTracingITest.setUp:57`, waiting for `Transitioning
  from RECOVERY to RUNNING`) while `ITEST - Kafka Smoke` passes beside it. That job failing while every sibling
  passes identifies it with no log fetch; in a log the tell is a GraalVM segfault at `Pwd.getpwuid` reading
  `user.name`. Never pin the tag away; check for a later green run before escalating.
- `studio-ui` `npm run test` is the standing failure of `Tests (without ITEST)` — tell `Failed to run task:
  'npm run test' failed` plus `-rf :studio-ui`. Always the same 2 of the 28 tests in
  `src/containers/projects/OverviewPanel.test.tsx`: one `Test timed out` against the 20 s ceiling in
  `vite.config.ts`, one `vitest-fail-on-console` rejecting a React `act(...)` warning from the floating `.then`s
  (see *Human follow-ups*). Load, not code — never your own breakage; reproduce with eight busy loops beside it.
- `LockTest.testSimultaneousMultiThreadsWithWaiting` was fixed on `main` by one shared 90 s deadline; never re-escalate.
- **`IT (studio)` normally takes 6-9 min; it fails by running far past that**, every step logging
  `HttpTimeoutException` at 10001ms — WebStudio under Jetty stopped answering, no test at fault. It never
  self-terminates before the 6 h job limit: cancel the run, then `rerun_failed_jobs` cleared it on the same commit.
  Its other shape, a 3-7 min fail at `-rf :itest.studio.repos` with `WebStudioTest.repos:11 Failed requests:
  expected 0 but was 3`, is **fixed on `main` by EPBDS-16438** — a fresh occurrence is a regression, not a flake.
- `rerun_failed_jobs` returns 403 until every other job in the run has finished; wait for the run to complete.
- The weekly cross-platform `build.yml` ("Build", Java 21/25/26 × ubuntu/windows/macos) has failed on `main` every
  run since 2026-07-01, 6 of 9 jobs, in three different modules (`studio-ui`, `openl-maven-plugin`,
  `org.openl.rules.test`). Undiagnosed and unrelated to `build-quick.yml`; never treat it as the sweep's gate.

## Container facts

- No `gh` CLI. Use the GitHub MCP tools for every PR operation. `git fetch --prune origin` can exceed a 2-minute
  tool timeout on a cold clone; give it 300 s.
- The `get_comments` method 404s on this repository, so issue comments cannot be listed. Read new activity from the
  PR's own `comments` count and `updated_at` plus `get_review_comments`; a 404 there is not a missing PR.
- **The working tree never starts at `origin/main`** — the session opens on a `claude/*` branch and local `main` sits
  at the container's checkout base, an ancestor. Fast-forward first, or a grep answers about the wrong revision.
- **The whole reactor installs in ~35 min from a cold `~/.m2` with `mvn install -Dquick -DnoPerf -T1C -fae
  -Dmaven.test.skip.exec=true`** — surefire skips execution while test sources still compile. The cache never
  survives a container rebuild, so budget the download every run. For a change confined to one module,
  `mvn test -pl <module> -am -Dquick -DnoPerf` is far cheaper and never reaches webstudio.
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
- Maven Central, registry.npmjs.org and github.com all work: `curl` a jar from Central and read it with `unzip` or
  `javap -c`.
- **`sonarcloud.io` is blocked too** — 403 to CONNECT, same shape as shibboleth, confirmed against
  `$HTTPS_PROXY/__agentproxy/status`. So a red `SonarCloud Code Analysis` check can never be diagnosed from here:
  report the failed conditions from the check-run summary and hand the judgement to a maintainer. Sweep PRs have
  been merged twice with that gate red, so it is confirmed not to block a deletion-only sweep — never chase it.
- **Reading a CI log takes one shape only.** Listing runs ignores `per_page`, always returns 30 rows and overflows
  the tool limit — save it to a file and parse that with python for the run `id` (`run_number` is not an id).
  **Never trust the filters**: one workflow file plus branch `main` returned a page 9 days and ~20 commits stale.
  List with no filter and sort the 30 rows by `created_at` — the newest push to `main` is at the top and one call
  covers every workflow. `get_job_logs` truncates from the **end**: a failing ITEST needs `tail_lines` about 130 to
  reach the reactor summary (60 lands mid-cleanup, `failed_only` at 40 over a 6-job matrix is cleanup noise), and its
  `logs_url` is Azure blob storage the proxy 403s, so always use `return_content`. Unit tests: `build-quick.yml`.
- **Angle-bracketed text does not survive the PR-body MCP round trip** — a bare XML element name is swallowed, and
  `Map<String, X>` reads back as `Map` even inside a fenced code block, so a quoted signature silently becomes
  wrong. Keep generics and element names out of bodies, name the identifiers in prose, and re-read after writing.
- **The container presets a git config that misattributes commits and breaks tests**, and `/root/.gitconfig` is
  rewritten back to `user.name=Claude` mid-run, so a global identity silently reverts. Set `git config --local
  user.name/user.email` in the clone **and in every worktree**, and repair a bad commit with `git commit --amend
  --reset-author` — a plain `--amend` keeps the old author even when `GIT_AUTHOR_*` is exported. The same preset
  turns on `commit.gpgsign` with `gpg.format=ssh`, killing every JGit commit in
  `STUDIO/org.openl.rules.repository.git` with "No signer for ssh signatures" — 15 test errors that are not a code
  defect. Unset `commit.gpgsign`, `user.signingkey`, `gpg.format` and `gpg.ssh.program` before building.

## Exhausted veins

- Base-name search over every image, `.xhtml` and `.js` file outside `Docs/`, and over all `Docs/` images.
- Class and id selectors across all 11 hand-written CSS files in STUDIO — no provable orphan but `.te_hidden`. The
  at-rule level has no surface at all: no stylesheet here, Bootstrap 2.3.2 included, declares a `@keyframes`, a
  `@font-face` or a CSS custom property, and studio-ui styles live in `.styles.ts`.
- Key-reference scan over every `.properties` bundle — `openapi.properties`, `ValidationMessages.properties`,
  `messages.properties`, `sql-errors.properties` and all 9 `openl-default.properties` — plus every leaf key of the 15
  studio-ui locale bundles, which are all English with no other language. Every survivor is reached by leaf or
  composed at runtime; see *Keep-list*.
- studio-ui: every exported const/function/class/interface/type/enum, the import graph over all 345 modules (every
  one has a production importer), all 62 declared npm dependencies, and `@typescript-eslint/no-unused-vars`.
- Function, object-literal-method and prototype definitions across all 23 hand-written legacy `.js` files.
- Whole-file reference check over every extension outside ITEST and test-resources: Velocity templates, taglibs,
  source maps, fonts, schemas, `.html`, `.csv`, `.sql`, `.groovy`, `.sh`, `.cmd`, `.txt`, `.json`, `.yaml`, `.xml`,
  `.svg`, and all 117 `.properties` files. Every orphan found is convention-loaded — see *Keep-list*. Every
  zero-byte tracked file is deliberate too: empty jar and zip classpath fixtures, and Flyway `flyway.location` markers.
- **Across all 208 poms**: every defined property, duplicate `<dependency>` and duplicate `.properties` keys, every
  profile, every `pluginManagement` entry, every `<exclusion>` and every `<resource>`/`<testResource>` directory path
  resolved against disk, each also resolved against `-P` references, activation blocks, packaging types and goal
  invocations. Zero removable; every exclusion is defensive. The root aggregator's `<module>` graph and all 8
  `META-INF/services` files closed empty too.
- **PMD 7.17 `UnusedAssignment`, `UnusedLocalVariable`, `UnusedPrivateField`, `UnusedPrivateMethod` and
  `UnusedFormalParameter` over main *and* test sources of all 42 analysable modules** (`includeTests=true`), the
  detector validated against the known main-source baseline. No PMD scope is left open. javac `-Xlint` over the whole
  reactor adds nothing: no `UnusedVariable`, `UnusedMethod` or `UnusedNestedClass`, and its only `EffectivelyPrivate`
  hits are four constrainer test classes, which is a visibility refactor rather than a deletion.
- **Java type and member deadness, closed on every surface below public**: every non-public top-level type; every
  unannotated package-private and protected member of those files; the package-private members of *public* types from
  the union of a brace-tracking and an indent-based detector; and every public top-level type in a test tree (ITEST
  and the two test-jar modules excluded), resolved against an identifier index over all text files and then against
  every Excel and archive resource. Only `JavaType` was ever removable.
- `dependency:analyze-only` over the 52 resolvable modules — every "Unused declared" hit is covered by the Keep-list
  entry on runtime wiring.
- Incremental `main` scope up to the *Resume point* commit: every author's own deletion was already complete, leaving
  no orphaned export, locale key, bundle key or helper. Dependabot and npm lock-file bumps add no surface at all.
- **Compilation reachability of every tracked `.java` file**, deriving each file's source root from its own `package`
  declaration: zero package/path mismatches, and all 142 implied roots are either the reactor-wide `src`/`test` the
  root pom declares or sit in a Docs example, an invoker fixture, an archetype template or a `test-resources` rule
  project. No uncompiled source file exists, and both assembly descriptors are named.
- Every `org.openl.*` class name referenced from `.xml`, `.xhtml`, `.properties`, `.tld`, `.yaml`, `.json` and
  `.txt`, resolved against the source tree. The ~90 with no `.java` are all runtime-generated datatype and
  spreadsheet-result beans or rule-project fixtures under test resources. No stale configuration exists.

## Human follow-ups

- **Allowlist `build.shibboleth.net`**, or move the `org.opensaml:opensaml-bom` import out of the root pom:
  `STUDIO/org.openl.rules.webstudio`, the largest untouched module, cannot be compiled or swept until then.
- **`itest.tracing` and `itest.kafka` both start the unpinned `apache/kafka-native:latest`** (see *CI flakes*), whose
  image segfaults in its own bootstrap and keeps reddening `main`. Pinning it to a released version needs a human.
- The weekly cross-platform `build.yml` matrix needs an owner; *CI flakes* records what is failing.
- `OverviewPanel.tsx` starts two unawaited promises that resolve into state setters, at lines 797 and 1227. Nothing
  synchronises them with the test, so `OverviewPanel.test.tsx` fails whenever the machine is slow. Needs the effect
  awaited or the test made to wait on it; raising the CI timeout to 20 s treated the symptom only.
- Weigh every public-API removal parked in *Deferred findings*: each needs a downstream-break judgement this routine
  may not make, and the tableeditor taglib and the `MergeModal` interfaces carry the largest consequences.
- Name the authoritative side of the committed tableeditor CSS bundles, which `compile.css.sh` does not reproduce;
  `.te_hidden` is blocked until then. That step is wired into no Maven phase, so editing a `js/` or `css/` source
  silently fails to reach the runtime.
- **`Docs/production-deployment/example/` and `Docs/examples/production/` are the same 32-file example tree twice**,
  differing only in one relative link in `README.md`, and both published and linked, so neither is dead and this
  routine cannot pick. `Docs/README.MD:232` also points at a missing `operations/production-deployment.md`.
  Needs a maintainer to name the authoritative copy.

## Run log

- 08-20 — run 228: screened nine EPBDS commits, 459 deleted lines; one deferred finding, nothing deletable.
- 08-20/21 — runs 229-243: idle; new `main` work was a spotless bump, three EPBDS-16473 start-up fixes and two
  EPBDS-16463 commits confined to webstudio Java and ITEST fixtures. Every deletion was in-file or moved into a
  file the same commit adds, so nothing was orphaned. `main` unchanged since run 242, no `dead-code/*` PR open.
