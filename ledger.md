# Dead-code sweep ledger — openl-tablets

## Resume point

PR #2063 waits only on the owner's merge: mergeable, trial-merges clean onto main's tip, 12 CI jobs green with
`dependabot` skipped, never a human comment, CodeRabbit paused. Only the deterministic SonarCloud gate is red, on
its one condition `new_reliability_rating` 4 — the pre-existing Critical `javabugs:S6466`, open on main at the SAME
file and line (`WorkbookListener.java` 273); `rules=javabugs:S6466` without `pullRequest` re-proves it, its five
BUGs are answered, and no clean removal is ever reverted for it. Redo that proof and the trial merge whenever main
advances; every detector is spent, so a run is PR maintenance, compaction, the profile-delta check, main's delta.
Main has now stood at `4a45294c` and head at `a0e0041f` for eleven runs, so expect a run to find nothing and end
in minutes: read the four, write the run-log line, stop. Do not manufacture work from an unchanged repository.
Skip the rule-facet re-check unless BOTH main's commit and an analyzed profile moved: main is re-analyzed on
unchanged code, so a fresh `api/project_analyses/search` key alone still returns an unchanged facet.
CONCURRENCY: runs share this ledger and this PR; a stale CI event names a superseded `head_sha`.

## Change-type queue

Empty; *Exhausted veins* records what every closed row covered. Open a row only for a rule the profile-delta check
returns whose fix is a DELETION — a spent detector re-run is waste, and a rewrite or an added check is out of scope.

## Open PR

- #2063, branch `dead-code/dead-suppressions`, head `a0e0041f`, merge base `c0213fe6`, 291 files, 628 deleted and
  346 added lines, 26 commits, one per change type with its own PR-body section, which alone records what each kept
  and why. Derive every count against the MERGE BASE, never against main. The body's 28 `##` headings are the 26
  commit subjects verbatim plus two findings-only sections — not a drift.

## Merged PRs

- #2054/#2056, #2058, #2060, #2062 merged with no review comment; the owner merges green or not, branch auto-deletes.

## Module coverage

- Nothing open: every module, main and test sources, webstudio included; only ITEST fixtures are out of scope.

## Deferred findings

- Public members in a published artifact, dead but held back by safety rail 2: `TableViewerTag`, `TableEditorTag`
  (their `faces-config.xml` component types are the live path), five `XlsProjectionType` `CELL_*` constants,
  `DecisionTableBuilder.methodName`, `SimpleGroup.description`, `MergeResult.status` (each with its public
  setter), and ~190 accessors named only at their declaration.
- All 104 `java:S1133` deprecated members: 98 public, protected or interface in a published artifact, one an
  openl-maven-plugin `@Parameter` a pom can set, five non-public with live callers. Deprecation proves nothing.
- Held back because the fix is a rewrite, not a deletion: the 61 `typescript:S8980` `act()` wrappers (each unwrap
  re-indents its block); the `listeners` dead store in `copyModule.xhtml` and `editOpenAPI.xhtml` (dropping the
  variable leaves the bare `new Listeners(...)` that `javascript:S1848` flags); `TableVersionComparator`'s
  class-level `@return`, real prose about `compare`; `studio-ui`'s ~70 exports used only in their own file; and
  `eslint.config.js` registering `react-hooks` while enabling no rule of it. Keep `MergeModal/types.ts` too
  (documented in `Docs/api/projects-merge-api.md`) and `npm run clean`, a human entry point.
- `kafka-clients` is declared only by `org.openl.rules.ruleservice.kafka`, which never touches it, yet two modules
  reach it transitively from there — removing it needs a declaration added elsewhere.
- `MappedRepository.refreshMappingWithLock`'s dead `throws IOException` stays: deleting it moves the finding onto
  its only caller, the public `initialize()`, which rail 2 holds. The other 14 `java:S1130` sites are published.
- 47 `<dependency><version>` elements the root already manages, plus three default-valued ones, are read by Maven
  and take precedence — a human's DRY fix, not a deletion.
- Three class-level type parameters are unused in their own declaration yet public: `ReturnOperation`,
  `IStorage` and `ProjectService`. Dropping one breaks every caller that writes the type with an argument.

## False-positive shapes

- An enum constant reached through `values()` is never named anywhere, so a name scan reports it dead. It is
  usually load-bearing: `Separator.DASH` is the primary range separator, `Brackets.CURLY` a bracket pair.
- Scan hygiene, three ways a token scan lies. Counting the FILES holding a name hides every in-file caller, the
  only possible caller of a private member — count occurrences and compare against the declaration count. A
  non-exact token fails both ways: a regex admitting `(` swallows the paren of markdown `![alt](name.png)`, a
  substring matches `add.png` inside `toolbar_add.png` and a typo'd `512x512.pngs`, so a BROKEN reference makes a
  file look alive; require a boundary and reject a line by token. And a scan keyed on a file list misses whatever
  owns no file: an extensionless dotfile escapes an extension filter, and a NESTED class has no file, so
  `FileUtils.ContentTooLargeException` reads as an unknown type.
- A text-file token scan cannot see an `.xls*` rule workbook, and that is exactly where OpenL names a Java bean's
  property or a rule helper method. Accessor candidates in `DEV/org.openl.rules.test` and the ITEST rule projects
  are therefore unprovable, not dead.
- A framework calls an accessor without naming it, four ways worth checking first: a JPA `@Entity` accessor, a
  `@Bean` factory method, an injection setter, and an interface method whose impl carries no `@Override`.
- Deleting `var self = this` in browser JS fails SILENTLY when something still reads `self`: it is a global
  (`window.self === window`), so no ReferenceError — read every nested closure, not just the function body.
- An override whose body is only a `super` call is load-bearing four ways, and all 22 here are: its annotations are
  the point; `hashCode` delegating to `super` satisfies Sonar's contract; an override of a GENERIC super method
  keeps the concrete type erasure drops; `wrapper/base/**` needs `WrapperValidation`.
- A redundant no-op construct is load-bearing when the construct itself is the mechanism: `synchronized (this) {
  return; }` waits out every other synchronized block, an empty `while (in.read() != -1);` drains a stream, and a
  trailing `return;` closing a chain of `if (…) { setX(); return; }` or of arms that throw is symmetry. PMD and
  Sonar `java:S3626` flag all of them; only a `continue` ending an `if`/`else if` arm is really deletable, and it
  leaves a comment-only block, which Sonar accepts and which the `java:S135` one-jump-per-loop rule prefers.
- Sonar sees no method reference in its unused-private-method rule: `java:S1144` calls `XlsBinder
  .addBindingContextError` dead while `holder::addBindingContextError` is its caller one screen above.
- Before standing down on a red check, compare the MERGE BASE's own run, not only the latest `main`: a job red at
  the base with every other job green is inherited, and rebasing onto a recovered tip is the fix, not a re-run.
- A managed dependency or plugin that NO module declares is normally still live: a deliberate transitive version
  pin whose neighbouring comment or release note names the CVE, or a plugin the default lifecycle, the packaging,
  a workflow goal or a `site/` configuration reaches. Only an entry no build can produce or reach is dead.
- An "overwritten" assignment is load-bearing whenever anything between the two writes can observe it: an early
  return leaves the initializer as what the getter returns, `Condition.await` hands the field to another thread,
  a callee reads a field restored in `finally`, and a getter publishes it to other beans. Read the control flow.
- A "never used" local is load-bearing when the declaration is the point: a try-with-resources whose construction
  and close are what the test asserts, or a loop variable used only to count iterations.
- Test sources dominate any unreferenced-member scan and are almost never dead: the runner discovers the required
  package-private classes, a `@Test`-less type is a fixture or a named bean, a private member an assertion's
  subject. Never tidy any of it.
- A package-private `@Component` is injected by its interface, so its simple name appears in no other file.
- A linter or compiler is blind by construction on parts of its own project, so its silence proves nothing: none
  reports on its own config file, `@typescript-eslint` synthesises a `React` reference per JSX element so an unread
  import is invisible, and an option absent from memory may be new (`resolve.tsconfigPaths`). Read the package.
- An unread LEADING callback parameter is positional, not dead: dropping `url` from `(url, options) => …`
  shifts `options` onto the first argument. ESLint's `args: 'after-used'` default encodes the same rule.
- A dependency whose only consumer is itself dead counts as used, so sweep code before dependencies, never after.
- An identifier is routinely composed at runtime, so its literal appears nowhere: an i18next plural suffix, a
  template literal, prefix composition, a `$ref` tail, JSF `compared_#{bean.order}`, JS `"status-" + status`, the
  tableeditor `t_te_table` id. Build a regex per template, and read the branch (`browser.${id}_confirm`).
- A Jackson MixIn declares abstract methods only to carry annotations, so being named nowhere is the point:
  `OpenApiXmlIgnoreMixIn.getXml` is matched by name against `Schema`. Never delete a member of a MixIn class.
- A commented-out line is often documentation, not a leftover: pseudo-code, the Java equivalent of emitted
  bytecode, an alternative formula, a retained reason or author, an `else`-branch marker, a sample of the string
  built below, a `/* package */` type marker. A scanner must also consume `"""` text blocks as literals.
- javac stops after 100 warnings per compilation and Maven never raises it, so a build log read as proof is
  silently truncated: pass `-Xmaxwarns 100000`.
- A "test sources" path filter must anchor the directory exactly: `(^|/)test/` also matches the PRODUCTION
  packages `src/.../web/test/` and `src/.../testmethod/`, turning live code into a phantom finding.
- A Java type parameter can be used only as the RETURN type, which sits before the parameter list: a scan that
  starts its usage window at the opening paren calls every `<R> R invoke(...)` dead. Start it at the modifiers.
- A manifest field is read by a tool DEEP in the dependency tree, not by the one the project names: Babel, which
  `@vitejs/plugin-react` drives, reads the `browserslist` field because it never sets `browserslistConfigFile`.
- A redundant-construct detector reads the construct, not the type context that makes it load-bearing: a cast IS
  the declared type under `var` (`var x = (double) 10` boxes to Double, without the cast to Integer), explicit
  boxing pins one of the 16 `Operators` overloads per operator, `((Double) o).doubleValue()` differs from
  `(Double) o` on null, and `invoke(m, new Object[]{null})` passes one null ARGUMENT where `invoke(m, null)`
  passes a null ARRAY. Check the declaration and the overload set before believing any of them.
- A redundant construct whose fix needs a REPLACEMENT is a rewrite, not a deletion, and out of scope: `"" + x`
  needs `String.valueOf(x)`, inlining a local into its `return` is a refactor, an empty catch needs a comment.

## Method rules

- Never pipe a proof grep through `head`, nor read a source from the middle — both hide the refuting caller.
- A bulk scan finds candidates; the individual `grep -rIF` is the proof, and every survivor needs one — for a
  FIELD especially, whose "never read" window is one method while a callee, another thread or an injected bean
  can read it. Re-derive a bulk PMD finding with an own parse of the same construct that descends into NESTED
  annotations; the single disagreement is where the real site hides (Lombok `onMethod_`). For a large candidate
  set tokenize the corpus in ONE pass and set-difference: per-candidate regex never finishes, one pass over 13.4k
  text files takes a minute.
- Anchor a per-line regex with `[ \t]*`, never `\s*`: a greedy `\s*` eats the newline, skipping the next tag.
- A module that publishes nothing has no public API to protect — `maven.deploy.skip` plus `pom` packaging releases
  no artifact, so its public members are internal: `DEMO/`, `DEV/org.openl.rules.gen`, `.test`, `ITEST/**`, swept.
- OpenL models a Java type from `getDeclaredMethods()` alone (`JavaOpenClass.initMethodMap`), keyed on erased
  parameter types, so deleting a declaration a subclass merely inherits still changes the rule-visible signature
  set. A member removal in a rule-reachable type needs that checked, not just the compile.
- Deleting a Java class is provable without compiling its module: a repo-wide search for its simple and its
  fully qualified name plus the reflective string registrations is complete. The same holds for a local or
  private member, whose scope is one file; CI's `Build artifacts` job is then the compile gate.
- Sonar's reliability rating on new code follows the WORST severity — Critical is D, Major only C — so a
  pre-existing Critical re-attributed because its file entered the diff reds the gate. A PR analysis reports
  `javabugs` issues on UNCHANGED lines of a changed file, and names different lines than main's own list over the
  same bytes, so "no issue at that line on main" proves nothing: diff the file against main and read the shape.
- Search an accessor by its property name as well as its method name: Velocity `$w.propertyType` and JSF EL
  `#{bean.propertyType}` call `getPropertyType()` without ever spelling it.
- When this container's npm rewrites unrelated lock metadata, remove the lock's own regions by hand instead and
  prove coherence with `npm ci`, which fails when the lock and `package.json` disagree.
- Never rebase a green PR merely to be current: `git merge-tree --write-tree origin/main <head>` proves the
  merge read-only, no worktree and no checkout, printing every CONFLICT and exiting non-zero when there is one.
  A rebase burns a CI cycle, and the merge is where a clean-merging lock is caught.
- Before deleting a dead `throws`, read the callers: an unreachable `catch` at a call site becomes a compile
  error, and a caller whose own clause had no other source inherits the finding — deletable only if that caller
  is not itself held by a safety rail.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Deleting a local variable takes its now-unused import with it, in the same commit — check the type's every
  remaining occurrence, bare and parameterized, or Spotless removes it later as unexplained churn.
- Only a user-observable removal needs a release-notes entry; dead CSS and an unreachable internal endpoint with
  no button never are, and both have maintainer precedent.
- Maven silently ignores a `<configuration>` element no mojo declares, so the oracle is `META-INF/maven/plugin
  .xml` inside the plugin jar in `~/.m2`: every `<parameter><name>` and `<alias>` per goal, and the goals it has.
  PER GOAL is the point — a parameter can exist on one goal only, and maven-jar-plugin declaring `skip` on
  `test-jar` alone is what makes the root pom's plugin-level `<skip>` live.

## Keep-list

- `org.openl.rules.tableeditor` `js/*.js` and `css/*.css` are build inputs: `HTMLRenderer` loads
  `js/tableeditor.min.js` and `css/tableeditor.min.css`, concatenated by `compile.js.sh` / `compile.css.sh`.
  Editing a source means regenerating both bundles in the same commit. `compile.js.sh` reproduces the JS bundles
  byte for byte; `compile.css.sh` drops the final newline of `tableeditor.min.css`, so restore it.
- Every public method of a class under `lang/xls/binding/wrapper/base/**` stays, whatever its body: the static
  initializer calls `WrapperValidation.validateWrapperClass`, which throws at class-load unless the wrapper
  declares each public non-static method of its superclass by `getDeclaredMethod`.
- `tableeditor.taglib.xml` and `faces-config.xml` are the live path for the two OpenL Facelets tags; only the
  `.tld` beside them was dead. Never treat the taglib or the faces config as the same finding.
- Keys under `ws.project.openapi.mode.` are reached by
  `#{msg['ws.project.openapi.mode.'.concat(project.openapi.mode.name().toLowerCase())]}` in `project.xhtml`.
- `rf-*` (RichFaces, JS inside a jar) and antd-generated class names can never be proven dead: the 17 `rf-*`
  classes and `ant-select-input` in `common.css` and in three pages' `<style>` blocks stay. RichFaces is alive.
- `openapi.properties` keys are annotation values resolved by `OpenApiPropertyResolverImpl`; all 625 are live.
- `ValidationMessages.properties` keys are looked up by a short form: the code drops the `openl.error.` prefix
  and, for exceptions, the three-digit status segment. All live; see the localized-exceptions skill.
- tableeditor `onFailure` is a Prototype callback (`'on' + state`); ITEST `001-Get-Static-CSS` ignores the body.
- Vendored scripts and stylesheets stay untouched — editing one forks the upstream copy: tableeditor
  `js/datepicker.*`, `css/datepicker.css`, `js/prototype/*`, webstudio `diff2html.*`, `javascript/vendor/**`, the
  Rule Services `static/rapi-doc/` bundle and its `.js.map`.
- `serviceDescriptionInProcess` in `ServiceManagerImpl` is published to other beans through
  `@Qualifier("serviceDescriptionInProcess")` getters; its assignments are a deployment protocol, not bookkeeping.
- `META-INF/openl/extension-*.xml` is pulled in by a wildcard `@ImportResource` in `ExtensionsConfiguration`;
  `openl-db-repository-<databaseCode>[-v<major>[.<minor>]].properties` and `-ext` are loaded by a name `Settings`
  composes at runtime; the flyway `db/flyway/**/V*.sql` migrations are loaded by directory convention.
- Files whose only loader is the servlet container, named by no file here: webstudio `logging.properties` (the
  per-webapp JUL configuration in `WEB-INF/classes`), `beans.xml` (CDI is live — webstudio declares
  `weld-servlet-core`) and webstudio `META-INF/context.xml` (Tomcat).
- Convention files nothing names: `banner.txt`, `.claude/**`, `.github/workflows/*`, `.github/dependabot.yml`
  (both ecosystems live), `archetype-metadata.xml`, `CITATION.cff`, `Gemfile`, `compose.override.example.yaml`,
  `.idea/**`, three `favicon.ico`, the `flyway.location` markers and the zero-byte `file.jar`/`file.zip`.
- Runtime-only artifacts that `dependency:analyze` always calls unused: `jaxb-runtime`, `awssdk:sts`,
  `log4j-slf4j2-impl`, `hibernate-hikaricp`, the CXF `cxf-rt-*` feature and provider jars, and the Jackson
  artifacts the Azure repository pins.
- All nine root profiles are live, both root `<repositories>` serve OpenSAML (absent from Central), and
  `lombok.config` declares only the two copied annotations `AGENTS.md` documents.
- `redirectPage` is read by `SessionTimeoutFilter.getInitParameter`, `xForwardedPrefixStrategy` by
  `de.qaware.xff.filter.ForwardedHeaderFilter`; the other six `param-name`s are framework constants.
- `Docs/` renders through the remote theme `mmistakes/minimal-mistakes`: a file under `Docs/_layouts` or
  `_includes` overrides a theme file of the same name, so nothing has to name it. All three such files are live,
  `release-notes.html` through a `_config.yml` default.
- `DEV/org.openl.rules.gen` templates and helpers are all reachable: `GenRulesCode.run()` calls all eleven
  `generate*` methods, and every `VelocityTool` method and template variable is used by a template.
- `archetype-resources/pom.xml` is processed by the archetype plugin itself, so no `<fileSet>` names it. The two
  empty `assembly/*.xml` files under `openl-maven-plugin/it/` are fixtures `verify.groovy` asserts are excluded.
- `DEV/org.openl.commons/test-resources/specs.properties` declares `hello` and `duplicateKey` twice on purpose:
  it is the fixture for the properties-spec parser. Never dedupe it.
- Four `studio-ui` dependencies no file imports are live all the same: `license-checker-rseidelsohn` is run by
  the `build` script, the three `@types/*` by the tsconfig `types` list, `@vitest/coverage-v8` by the reporter.
- Every remaining `package.json` field is live: `browserslist` reaches Babel, `engines` is npm's own check, and
  `name`, `version`, `private` and `type` govern resolution and publishing. Only `homepage` was CRA's.
- The tableeditor `css/tooltip.css` `tooltip_skin-*` and `tooltip_top_*` classes are the widget's theming API.

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails two infrastructure ways, never the diff: `ORA-12516` with
  the listener not yet accepting, or `Failed requests: expected <0> but was <N>`. One log read settles the second,
  no rerun: `MysqlRdbmsTest` passes the SAME suite in that job, N varies on identical code, `main` fails it too.
- `IT (services-data)` — `apache/kafka-native:latest` exiting 1 in its entrypoint before any test body (any of
  the 3 kafka suites) is upstream and intermittent: one suite can pass as the next fails. Never pin the tag.
- `Sonar analysis` — runs only when every IT job is green, so it reads as `skipped` while any flake is red, and
  clearing them is what finally gates the PR. Its own flake, one rerun per SHA: `jacoco:report-aggregate` dying
  with "Unknown block type c7" from the overlapping `coverage-*` artifacts, which suppresses the gate.
- A RE-RUN can die in 8 s resolving the `archetype-packaging` extension: a runner-local transient that burns it.
- `rerun_failed_jobs` is refused while a job still runs — 403, or a bare 500 that is not failure. Read `run_attempt`.

## Container facts

- `~/.m2` is EMPTY at session start, so the cold reactor build — `LANG=C.UTF-8 LC_ALL=C.UTF-8 mvn install -Dquick
  -DnoPerf -T2 -B`, all 86 modules — takes 32 minutes. Use `-T2`, NOT `-T1C`: four threads starve the studio-ui
  vitest run and `UserDetailsTab` fails a 6 s `findByText` that passes in 1.3 s idle. Start it in the harness's
  background mode as soon as the edits are in; never pipe it through `tail`, and fetch a release-only plugin
  with `dependency-plugin:3.11.0:get`.
- The build must run ONLINE: `-o` fails before the reactor starts, because `main` keeps bumping dependencies past
  what any cache holds. `build.shibboleth.net` answers 200, so the root `pom.xml` needs no surgery.
- That locale is required: the container's own is POSIX, and `ZipArchiveValidatorTest.testArchives` then dies on
  `InvalidPath ... unmappable characters` for a Cyrillic file name and takes the nine studio ITEST modules down.
- One ITEST suite CAN be built here, and needs the `install` lifecycle: `mvn install -Pitest -Dquick -DnoPerf
  -T1C -B -pl ITEST/<suite> -am`, a 28-module reactor. `test-compile` is too early: `unpack-dependencies` of the
  webapp fails with MDEP-98 first.
- PMD run standalone scans all 4031 files in 16 s, test sources included, and is the cheapest new detector there
  is: `mvn dependency:copy-dependencies` for `net.sourceforge.pmd:pmd-cli`+`pmd-java` 7.18.0 into `.toDelete/`,
  then `java -cp 'lib/*' net.sourceforge.pmd.cli.PmdCli check --no-cache --file-list <ABSOLUTE paths> -f csv`.
  Relative paths in the list resolve against PMD's own cwd and silently match nothing. maven-pmd-plugin
  misses test sources; PMD reports a field of a nested or anonymous class, which reads like a local variable.
- Frontend verification is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`, `npx eslint <files>`,
  `npx vitest run` (183 files, 1699 tests, ~3.5 min) and `npm run build` — the WHOLE gate for a TypeScript-only
  diff, since that is what frontend-maven-plugin runs. Never judge it while Maven holds the same `node_modules`.
  `npx eslint ./src` exits 1 on `main` (15 `object-curly-spacing` errors, 2 warnings) — never fix those.
- `rg` is the tokenizer: `xargs -a <list> rg -oH --no-line-number -w -F -f names.txt` scans the corpus in a
  minute, where `grep -f` never finishes and rg's `--files-from` yields nothing. Never `pkill -f` a grep pattern.
- A failed reactor can only be resumed by a FULL rebuild, never `-rf`: `org.openl.rules.test` skips artifact
  installation, so nothing downstream of it resolves outside one reactor session. Dropping `clean` is what makes
  the second pass cheap. `nohup mvn ... &` hides the exit code and a second launch races the same `target/`.
- The container's global git config signs commits over ssh, failing `GitRepositoryTest` and
  `SameSecondHistoryOrderTest` with jgit `UnsupportedSigningFormatException`: `git config --global commit.gpgsign 0`.
- The global git identity can be rewritten back to `Claude <noreply@anthropic.com>` mid-session. Re-set it and
  pass `GIT_AUTHOR_*` / `GIT_COMMITTER_*` inline on every commit; `--amend` alone keeps the wrong author,
  so it needs `--reset-author`.
- `gh` and `xxd` are absent, but `$GITHUB_TOKEN` is set: `curl` the REST API to read a PR body to a file and
  `PATCH` it back — MCP `pull_request_read` `get` overflows the tool-result cap on a body this long, and an MCP
  body argument swallows angle-bracketed text, so curl is the only read and write that fit. The proxy allows PATCH
  yet refuses a REF write — `DELETE /git/refs/heads/<b>` and `git push --delete` both 403 — so no run can
  delete a branch, whatever the token's own scopes. Read the 403 body: it names the proxy, not a permission.
- `sonarcloud.io`'s WEB UI is blocked (403) but its API answers 200, so a gate failure IS diagnosable here:
  `api/issues/search?componentKeys=org.openl.rules:openl-tablets&pullRequest=<n>&types=BUG` names every issue,
  the same query without `pullRequest` gives main's open BUGs, which proves one pre-existing, and
  `api/qualitygates/project_status` gives the gate's conditions — check `api/project_analyses/search` too, that
  list being only as fresh as main's last analysis. `&facets=rules&ps=1` ranks every rule by hit count without
  returning an issue, the one cross-check that no deletion rule was missed, every rule above 80 hits being a
  rewrite or style. An unknown `rules=` key is silently IGNORED and returns the unfiltered total, so fetch it too.
- PROFILE-DELTA CHECK, the one recurring detector left, two minutes: `api/qualityprofiles/search?organization=
  openl-tablets&project=org.openl.rules:openl-tablets` gives each profile's `rulesUpdatedAt`, and only the seven
  languages Sonar analyzes matter — css, java, js, ts, web, xml, yaml, per `api/measures/component?metricKeys=
  ncloc_language_distribution`; yaml is 2 credential detectors and the 133 `.groovy` files are unanalyzed, so a
  groovy bump is noise. A profile newer than the last covered batch goes to `api/qualityprofiles/changelog?
  organization=…&qualityProfile=Sonar%20way&language=<l>`, which dates every ACTIVATION where `available_since`
  fails, filtering rule CREATION; classify each by its fix and count only a deletion rule with `rules=<key>`. A
  `javabugs:*` rule is always a check, and every call needs `organization`. Compare the RULE's changelog date
  against `api/project_analyses/search`: one activated after main's last analysis reports 0 for never having run,
  while a later-touched profile can still hold only rules that ran.
- Write the ledger through `git worktree add --detach <dir> origin/dead-code/ledger`, never an orphan-branch dance.

## Exhausted veins

Java, all of it closed. PMD's five dead-code rules over every file; every `@SuppressWarnings` key; every JavaDoc
`@param`, `@return`, `@throws` and `{@inheritDoc}`; commented-out code as `//` runs and `/* */` blocks; every
method and class-level type parameter; every `serialVersionUID`; `package-info.java` in a source-less package.
Statements: every bare `super();` and every `super`-only override; the lone `;` is an enum's required separator,
every `};` an initializer or anonymous class. Members: every package-private method and field, every enum
constant by occurrence, every non-overriding public or protected member of a package-private top-level or
non-public nested class. Whole types: every non-public top-level type, every one in `.impl.`/`.internal.`, every
test type with no `@Test`, the non-public production types against a production/test split. Public members of the
non-published modules — all framework-driven. EVERY PMD java rule whose fix is a deletion has run over every
file, so that family is SPENT — enumerate the categories again only to check a NEW rule, never for a new vein;
the rest report style, rewrites, or the load-bearing constructs *False-positive shapes* describes.

SonarCloud's own analysis of `main`, faceted over every issue and then queried rule by rule, is a second Java
signal PMD does not subsume, and its whole deletion family is now checked: `S1128`/`S1116`/`S3985`/`S1596`/
`S2168`/`css:S4658` report nothing at all; `S1144`, `S1068`, `S1481`, `S1854`, `S2094`, `S1119`, `S1130` and
`S1172` report only false positives, test methods, members safety rail 2 holds, or an edit at every call site;
`S1197`, `S1226`, `S1155`, `S3457`, `S4144`, `S1871`, `S2696` are refactors; the whole `S6xxx`-`S9xxx`
generation, named rule by rule, is style or rewrite throughout. Its `javascript:*` dead-store, unused-local and
`S2814` hits are vendored, generated, already shipped, or the two deferred `.xhtml` pages; `java:S1133` and
`typescript:S8980` gave nothing (see *Deferred*). The profile-delta check covers every rule ACTIVATED since
2026-08-01 in all seven analyzed languages, through the 2026-09-02 batch. Exactly one has a deletion for a fix,
`java:S9341` redundant Spring annotations, whose 0 is PROVEN against a main analysis POSTDATING its activation.
The rule facet, re-run over that analysis, adds nothing: `typescript:S9020` is a Testing-Library rewrite.

Maven, all of it closed. `dependency:analyze-only` over every analyzable module in every scope; every
`<exclusion>` resolved in isolation; every non-import `dependencyManagement` and `pluginManagement` entry and
every import BOM; every `<build><plugins>` declaration; every `<include>`/`<exclude>` pattern, literal and
wildcard; every `<properties>` and profile id; every empty element and path-valued `sonar.*` property; every
system property and `argLine` flag handed to surefire; both root `<repositories>` and no `<pluginRepository>`;
every pom directory reachable from the reactor; both archetype modules and all four assembly descriptors; every
`openl-maven-plugin` mojo `@Parameter` field; every top-level child of every `<configuration>` block against the
plugin's own `plugin.xml`, execution-level ones also against their execution's goals; every dependency
declaration against its whole parent chain's `<dependencies>` (both overlaps real scope overrides); and every
element whose value is simply the Maven default (all deferred).

Main is swept clean through `4a45294c`, EPBDS-16529 included; only a commit past that tip is new ground.

Resources and descriptors, all of it closed. Whole-file deadness by a type census leaving no extension out, down
to the extensionless dotfiles (the `.ps1`, `.apt`, `.map`, `.webmanifest`, `.svg`, `.jj` stragglers all live or
vendored). Every non-test `.properties` file against its actual loader, not a name search. Every key of
`openapi.properties`, webstudio `messages.properties`, `ValidationMessages.properties`, the `studio-ui` locales,
`openl-default.properties` and the DEMO overrides, plus each flyway placeholder. Duplicate declarations within
one file: every `.properties`, every `.json`, every pom's repeatable children. Descriptors: class references in
every convention-loaded one, paths in the seven webapp descriptors, every entry of the webstudio `web.xml` (the
only one), every servlet `param-name`, the three log4j2 configurations, `compose.yaml` and the `Dockerfile` in
full. There is no `.dockerignore` and no `.mvn/`.

Web and frontend, all of it closed. CSS exhaustively: every class and id selector in all eleven own stylesheets,
the inline `<style>` blocks and `DEMO/webapps/ROOT/main.css`, plus every duplicate selector pair and repeated
property. Those are every stylesheet the repository owns — `studio-ui` ships none, styling from antd and inline
styles — and they hold no at-rule and no custom property at all. JSF: both Facelets taglibs, every `ui:define`
against every `ui:insert`, every `ui:param`, `<c:set var>`, `f:facet` name and `f:param`, the one `<ui:remove>`,
no navigation rule anywhere, and every `id` attribute of the 46 pages (none deletable — see *Deferred findings*).
Also both directions of the attributes on OpenL's own two tags: those passed by the `rules:tableEditor` sites
against `renderkit.TableEditor`, which reads the attribute map since `UITableEditor` is a bare `UIOutput`, and
every `Constants` member naming them — only `collapseProps` is supported without a caller. JS: function and
prototype-method deadness in `common.js`, `bomjs.js` and every own tableeditor script, the two own jQuery
plugins, every input of the four bundle scripts, and every module-scope variable in them. `studio-ui`: every
export, whole-file deadness, every `tsconfig.json` option, every `@ts-ignore`, every `eslint-disable` (none
exist), the npm scripts and declarations, every top-level `package.json` field, the imports of the three files no
linter covers, and `tsc --noUnusedLocals --noUnusedParameters` project-wide. Also every `xmlns:` prefix and
`data-*` attribute, every `.editorconfig` section, `.gitattributes` pattern and `.gitignore` line, every Jekyll
layout, include and `navigation.yml` url, the whole of `DEV/org.openl.rules.gen`, all 5 own `.sh`/`.cmd` scripts,
identical-content duplicates across production files, and every tracked build leftover.

## Human follow-ups

- Delete the abandoned branch `dead-code/studio-resources` (PR #2055 closed unmerged) — see *Container facts*.
- Decide the *Deferred findings* that are public in a published artifact, plus the two empty test jars, the
  commented-out test code and the 153 `(non-Javadoc) @see` markers — each dead, each needing a human's word.
- `Util/openl-maven-plugin/site/site.xml` links 8 pages its `<reporting>` entry no longer generates: the report
  goal moved to maven-plugin-report-plugin.
- Typos to fix, not deletions: `@SuppressWarnings("deprecated")` on `RulesUtilsTest.testParseFormattedDouble`
  (the key is `deprecation`), and the web manifest's `android-chrome-512x512.pngs`, so that icon never loads.
- `Docs/examples/production/` and `Docs/production-deployment/` hold the same 32 files twice. Stale prose:
  `rules-projects.md` names a nonexistent `TablePropertyValidatorsWrapper.init()`; `common-tasks.md` and the
  `studio-ui` README name npm scripts that are gone; the archetype descriptor name is a WSO2 leftover.

## Run log

- Run 68: no deletion; a ninth identical run — same SHAs, counts, jobs, analysis; `groovy` still the only mover.
- Run 69: no deletion; a tenth identical run — same SHAs, 26/291/346/628, 28 headings, 12 jobs green; main was
  re-analyzed on unchanged code and the facet was unchanged, which is what sharpened the skip rule above.
- Run 70: no deletion; an eleventh identical run — same SHAs, 26/291/346/628, 12 jobs green, no new comment; the
  seven analyzed profiles all sit inside the 09-02 batch and only `groovy` moved, so the skip rule held. 8 minutes.
