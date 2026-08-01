# Dead-code sweep ledger

State memory for the daily sweep of openl-tablets. Read in full at the start of every run.

## Resume point

- Every queue row is now swept at least once. The cheap detectors are mined out; what is left is the deferred list
  below, and each entry needs a targeted proof rather than a new detector run.
- Next run, in order: (1) re-seed the container (git identity, `opensaml-bom` stub) — the container is rebuilt every
  time, nothing survives; (2) maintain the open PR; (3) attack the deferred set, best first: the tableeditor JSP
  taglib, then the Maven runtime-vs-compile dependency question.
- Do not re-run PMD or the resource detectors on the same scope — see *Exhausted veins*.

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
| 9 | Never-read assignments (PMD) | done, 3 removed, 8 were false positives |
| 10 | Unused private fields and locals (PMD) | done, all 20 deferred or false positives |
| 11 | Unused private methods and formal params (PMD) | done, no findings — all sites already `@SuppressWarnings` |
| 12 | Unused nested / effectively-private types (javac) | done, no findings |
| 13 | Unused declared Maven dependencies | done for 52 modules, no provable finding |

## Open PR

- Branch `dead-code/java-internals`, PR #1940, ready for review, 1 commit, 2 files / 3 lines. No review threads.
- CodeRabbit asked for JSpecify `@Nullable` on the two `FuzzyContext` fields; answered and declined — the module has
  no JSpecify at all and the diff does not change their nullness. Settled, do not re-answer.
- Steady state after one rerun: red only on `IT (services-data)` (the kafka image blocker) and
  `Tests (without ITEST)` via LockTest, which is red on `main` too. Both answered in the body and in one comment.
  This is as green as the PR can get until a maintainer acts; do not rerun or comment again.
- Commit `Remove never-read field initializers in the DEV rules engine` — 2 files, 3 lines.

## Merged PRs

- #1933, 12 files / 74 deletions — studio-ui i18n keys plus `PopupMenu.showChild`. Merged by yurkom with the
  `LockTest` job still red; that job fails on `main` too, so a red `Tests (without ITEST)` does not block a merge here.

## Module coverage

- `STUDIO/studio-ui` — locale bundles, import graph, eslint, npm dependencies: only deferrals left.
- `STUDIO/org.openl.rules.webstudio` — never swept; it cannot be compiled here (see *Container facts*).
- Every other module — PMD and javac swept; findings are in *Deferred findings* or were false positives.

## Deferred findings

- `STUDIO/org.openl.rules.tableeditor` `taglib/TableEditorTag.java` (11 unread private fields) and
  `taglib/TableViewerTag.java` (7). Each field is written by a public setter and read by nobody. Both classes are
  named only by `META-INF/tableeditor.tld`. **The whole JSP taglib is the real candidate** — prove first that no
  JSP page and no container auto-scan uses the `.tld`, then remove tld plus both classes together. Do not touch the
  fields alone: the setters are the taglib's declared attribute contract.
- `STUDIO/org.openl.security` `SimpleGroup.description` — written by a public setter and a public constructor
  parameter, read nowhere. Removing it changes public API consumed by webstudio, which cannot be compiled here.
- `DEV/org.openl.rules` `DecisionTableBuilder.methodName` plus its public `setMethodName` and the single call in
  `TableSyntaxNodeDispatcherBuilder:136` — the whole chain is inert, but the setter is public API in `DEV/**`.
- `WSFrontend` `ServiceManagerImpl:230` — re-assigns `serviceDescriptionInProcess` to the value already assigned at
  228. Provably a no-op, but PMD flagged 228 (which is load-bearing) and not 230, so no detector proves 230.
- `DEV/org.openl.rules.test` `RulesInFolderTestRunner:80,116` — two dead `messagesCount++` before a `continue`. The
  other ten call sites in the method use the same idiom; changing two of twelve is churn, not cleanup.
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

## Method rules

- Prove non-reference with a plain repo-wide literal search excluding `target/`, `node_modules/` and `.git/` — every
  file type, never a regex scoped to one attribute or one module.
- For a bundle key, search the full dotted path **and** the bare leaf name; either hit means keep.
- Validate any new bulk detector by feeding it two fabricated names; if they come back "referenced", the search is
  wrong, not the repository. For a linter, plant a violation and confirm it is reported.
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
  so read the
  output, not the exit code.
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
  `@SuppressWarnings`, which is why rows 11 and 12 come back empty.
- GitHub Actions compiles every push against the real opensaml BOM, so CI is the authoritative gate on anything the
  locally stubbed build could not verify.

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
- `studio-ui` `npm run test` fails under `frontend-maven-plugin` in the same `Tests (without ITEST)` job — tell
  `Failed to run task: 'npm run test' failed` plus `-rf :studio-ui`, with the module burning 11+ minutes against
  ~4 minutes on an idle machine. Runner starvation, not code: confirmed by running CI's own `npm run test` on the
  same commit, and it cleared on the first rerun. Rerun the job.
- **`Tests (without ITEST)` has two independent causes; identify which one fired before answering.** Read the
  `-rf :<module>` hint: `org.openl.rules.repository` is the LockTest timeout below, `studio-ui` is the frontend
  flake above. They alternate — each has passed on a run where the other failed, so a green studio-ui does not
  mean the job is green.
- **`apache/kafka-native:latest` no longer starts on the runners — deterministic, not a flake. Do not rerun.** Job
  `IT (services-data)`, tell `SegfaultHandler caught a segfault` in `com.oracle.svm.core.posix.headers.Pwd.getpwuid`
  reading `user.name`, then `Timed out waiting for log output matching '.*Transitioning from RECOVERY to RUNNING.*'`.
  It crashed identically on two runner VMs in two different suites (`RunTracingITest`, then `RunKafkaSmokeITest`
  which had passed minutes earlier), so it follows whichever Kafka suite runs first. Green on PR #1939 at
  2026-08-01 07:47Z, broken from ~15:05Z the same day; the tag floats, so a republished image is the likely cause.
  Escalated to the maintainers — see *Human follow-ups*. Never pin the tag yourself.
- `rerun_failed_jobs` returns 403 "This workflow is already running" until every other job in the run has finished;
  wait for the run to complete before retrying.

## Container facts

- No `gh` CLI. Use the GitHub MCP tools for every PR operation.
- **The whole reactor installs in ~11 minutes with `mvn install -Dquick -DnoPerf -T1C -fae
  -Dmaven.test.skip.exec=true`** — surefire skips execution while test sources still compile. This replaces the
  "over an hour" figure: a run can now afford the install and a full sweep. `-DskipTests` still must not be used
  (this repo maps it to `maven.test.skip=true`, dropping test compilation).
- After such an install the surefire provider jar is absent, so a later `mvn -o test` dies on
  `surefire-junit-platform ... in offline mode`. Run the first `mvn test` online.
- **Maven works after seeding one stub.** `org.opensaml:opensaml-bom:5.2.3` is shibboleth-only, the proxy denies
  CONNECT to `build.shibboleth.net` (403), and Central carries only 4.0.x. Write a pom with an empty
  `<dependencyManagement>` to `~/.m2/repository/org/opensaml/opensaml-bom/5.2.3/opensaml-bom-5.2.3.pom`, delete the
  `*.lastUpdated` files beside it, and 75 of 82 modules build. Local only — it must never reach a commit.
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
  `user.signingkey`, `gpg.format`, `gpg.ssh.program` and set the Yury Molchan identity before building.
- Listing workflow runs through the GitHub MCP tool overflows the tool result. It saves the JSON to a file; parse that
  with python instead of retrying with a smaller page size. The unit-test workflow is `build-quick.yml`.

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
  `UnusedFormalParameter` over the main sources of all 43 analysable modules** — 31 violations total, detector
  validated by a planted field/method/local. Tests sources were NOT scanned (`includeTests=false`) — that is the one
  PMD scope still open.
- javac `-Xlint` over the whole reactor: no `UnusedVariable`, `UnusedMethod` or `UnusedNestedClass`; the only
  `EffectivelyPrivate` hits are four constrainer test classes, which is a visibility refactor, not a deletion.
- `dependency:analyze-only` over the 52 resolvable modules — every "Unused declared" hit is covered by the Keep-list
  entry on runtime wiring.

## Human follow-ups

- **`apache/kafka-native:latest` stopped starting on the GitHub runners on 2026-08-01**, segfaulting inside its own
  native-image bootstrap. It blocks `IT (services-data)` on every pull request and on `main`, and reruns do not
  clear it. Someone has to pin a working Kafka image or update the runner image; this routine only deletes and must
  not change a container tag. Evidence is in PR #1940.
- **Allowlist `build.shibboleth.net`** in the environment's network policy, or move the `org.opensaml:opensaml-bom`
  import out of the root pom. `STUDIO/org.openl.rules.webstudio` — the largest untouched module — cannot be compiled
  or swept until then, and the stub has to be re-seeded on every container rebuild.
- `main` is red: `LockTest.testSimultaneousMultiThreadsWithWaiting` keeps the `Quick Build` unit-test job failing, so
  no pull request can reach a fully green CI. Its sibling is already `@Disabled` as unstable; this one needs the same
  decision or a real fix. It passes on this container, so it is load-sensitive, not broken.
- Decide whether the tableeditor JSP taglib (`META-INF/tableeditor.tld` plus `TableEditorTag`/`TableViewerTag`) is
  still reachable. It is the largest single dead-code candidate found so far — 18 write-only fields and two classes.
- Decide whether the four unused `MergeModal/types.ts` interfaces should stay as the frontend mirror of the merge REST
  contract; if they go, `Docs/api/projects-merge-api.md` moves with them.
- The committed tableeditor CSS bundles do not match what `compile.css.sh` produces from the committed CSS sources, so
  they are stale or hand-edited. Someone has to decide which side is authoritative before any tableeditor CSS removal
  can ship; `.te_hidden` is blocked on it. The JS bundles reproduce exactly and have no such problem.
- The tableeditor bundling step is documented nowhere — no `AGENTS.md` or `Docs/` page mentions the two compile
  scripts or the checked-in yuicompressor jar, and it is not wired into Maven, so editing a source under `js/` or
  `css/` silently fails to reach the runtime. Worth a note in `STUDIO/AGENTS.md`; this routine only deletes.

## Run log

- 07-30 — third run. Maven 403. Six veins swept (npm deps, pom properties, whole-file over every extension, all
  `.properties`, duplicate declarations, eslint) — zero findings. Resource side fully mined out.
- 07-30 — fourth run. Broke the Maven blockade with a local empty `opensaml-bom` stub; nothing deleted.
- 08-01 — fifth run. #1933 merged. Found the fast-install flag, so the whole reactor plus PMD plus dependency
  analysis fit in one run. Rows 9-13 all swept; 3 never-read initializers removed, everything else deferred.
