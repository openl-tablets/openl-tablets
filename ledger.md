# Dead-code sweep ledger — openl-tablets

## Resume point

PR #2063 waits only on the owner's merge: no human has ever reviewed it, CodeRabbit reports nothing, every CI job
is green, and only the SonarCloud gate is red on the pre-existing Critical already answered on the PR.
Every detector is now spent: PMD, SonarCloud's own analysis of `main`, Maven, resources and the frontend. There is
no queued vein left, so a run whose PR is green and answered has only compaction and a new Sonar rule to check.
CONCURRENCY: sessions two hours apart share this ledger and the same PR — add what is missing instead of
replacing another run's text, treat a CI event for a superseded `head_sha` as stale, never arm a check-in chain.

## Change-type queue

All 122 closed — 45 shipped a deletion, 77 found nothing; *Exhausted veins* records what each covered.
The queue is empty. Open a new row only for a rule NEW to the Sonar profile; re-running a spent detector is waste.

## Open PR

- #2063, branch `dead-code/dead-suppressions`, head `f840d7f4`, 290 files, 626 deleted and 344 added lines, 25
  commits, one per change type with its own PR-body section. Derive the counts against the MERGE BASE; that body
  alone records what each commit kept and why.

## Merged PRs

- #2054/#2056, #2058, #2060, #2062 merged with no review comment; the owner merges green or not, branch auto-deletes.

## Module coverage

- Nothing open: every module, main and test sources, webstudio included; only ITEST fixtures are out of scope.

## Deferred findings

- Public members in a published artifact, dead but held back by safety rail 2: `TableViewerTag`, `TableEditorTag`
  (their `faces-config.xml` component types are the live path), five `XlsProjectionType` `CELL_*` constants,
  `DecisionTableBuilder.methodName`, `SimpleGroup.description`, `MergeResult.status` (each with its public
  setter), ~190 accessors named only at their declaration, and the four `super`-only overrides listed above.
- `studio-ui`: `MergeModal/types.ts`'s three merge types are unused in code but documented in
  `Docs/api/projects-merge-api.md`; ~70 exports are used only in their own file (un-exporting is a refactor);
  `npm run clean` is named by nothing, but an npm script is a human entry point.
- `kafka-clients` is declared only by `org.openl.rules.ruleservice.kafka`, which never touches it, yet two
  modules reach it transitively from there; removing it needs a declaration added elsewhere.
- `studio-ui` `eslint.config.js` registers `react-hooks` but enables no rule of it; both fixes change intent.
- The `listeners` dead store in `copyModule.xhtml` and `editOpenAPI.xhtml`: dropping the variable turns the
  assignment into a bare `new Listeners(...)`, the shape `javascript:S1848` already flags in `tableDetails
  .xhtml`. A deletion that trades a dead store for a new Sonar Bug is a rewrite, so both stay.
- `TableVersionComparator`'s class-level `@return` is real prose about `compare`; moving it is a refactor.
- `MappedRepository.refreshMappingWithLock`'s dead `throws IOException` stays: its only caller is the public
  `initialize()`, whose own `throws IOException` has no other source, so the deletion moves the smell onto a
  member rail 2 holds. The other 14 live `java:S1130` sites are public or protected in a published module.
- 47 `<dependency><version>` elements the root already manages, and three default-valued elements, are read by
  Maven and take precedence, so removing one changes behavior later: a human's DRY fix, not a deletion.
- Three class-level type parameters are unused inside their own declaration yet public in a published artifact:
  `ReturnOperation<ResultValueType>`, `IStorage<T>` and `ProjectService<T extends AProject>`. Dropping one
  breaks every caller that writes the type with an argument.

## False-positive shapes

- An enum constant reached through `values()` is never named anywhere, so a name scan reports it dead. It is
  usually load-bearing: `Separator.DASH` is the primary range separator, `Brackets.CURLY` a bracket pair.
- A token scan that counts the FILES containing a name hides every in-file caller, which for a private or
  package-private member is the only possible caller. Count occurrences and compare against the declaration count.
- A text-file token scan cannot see an `.xls*` rule workbook, and that is exactly where OpenL names a Java bean's
  property or a rule helper method. Accessor candidates in `DEV/org.openl.rules.test` and the ITEST rule projects
  are therefore unprovable, not dead.
- A framework calls an accessor without naming it, in four ways worth checking before believing a hit: a JPA
  `@Entity` accessor (Hibernate), a `@Bean` factory method (Spring), a setter for injection (`setEntityManager`),
  and an interface method whose implementation carries no `@Override` (`StompSessionHandler.handleFrame`).
- Deleting `var self = this` in browser JS fails SILENTLY when something still reads `self`: it is a global
  (`window.self === window`), so no ReferenceError — read every nested closure, not just the function body.
- An override whose body is only a `super` call is load-bearing in four ways, and all 22 here are: its annotations
  are the point; a `hashCode` delegating to `super` satisfies the contract Sonar enforces; an override of a GENERIC
  super method keeps the concrete parameter type erasure drops; `wrapper/base/**` needs `WrapperValidation`.
- A redundant no-op construct is load-bearing when the construct itself is the mechanism: `synchronized (this) {
  return; }` waits out every other synchronized block, an empty `while (in.read() != -1);` drains a stream, and a
  trailing `return;` closing a chain of `if (…) { setX(); return; }` or of arms that throw is symmetry. PMD and
  Sonar `java:S3626` flag all of them; only a `continue` ending an `if`/`else if` arm is really deletable, and it
  leaves a comment-only block, which Sonar accepts and which the `java:S135` one-jump-per-loop rule prefers.
- Sonar sees no method reference in its unused-private-method rule: `java:S1144` calls `XlsBinder
  .addBindingContextError` dead while `holder::addBindingContextError` is its caller one screen above.
- An API-safety check comparing a member's access against its class must match the class DECLARATION: a regex for
  `class X` hits the JavaDoc prose "The class X implements…" first and invents an API break that is not there.
- Before standing down on a red check, compare the MERGE BASE's own run, not only the latest `main`: a job red at
  the base with every other job green is inherited, and rebasing onto a recovered tip is the fix, not a re-run.
- A managed dependency or plugin that NO module declares is normally still live: a deliberate transitive version
  pin whose neighbouring comment or release note names the CVE, or a plugin the default lifecycle, the packaging,
  a workflow goal or a `site/` configuration reaches. Only an entry no build can produce or reach is dead.
- A path in a descriptor is relative or servlet-mapped, so it looks absent: `html/inputVersion.xhtml` resolves
  under `WEB-INF/taglib/`, `/faces/pages/x.xhtml` under `pages/`, and `/cxf/cxf.xml` inside a dependency jar.
- An "overwritten" assignment is load-bearing whenever anything between the two writes can observe it: an early
  return leaves the initializer as what the getter returns, `Condition.await` hands the field to another thread,
  a callee reads a field restored in `finally`, and a getter publishes it to other beans. Read the control flow.
- A "never used" local is load-bearing when the declaration itself is the point: a try-with-resources resource
  whose construction and close are what the test asserts, or a loop variable used only to count iterations.
- Test sources dominate any unreferenced-member scan and are almost never dead: the runner discovers the required
  package-private classes, a `@Test`-less type is a fixture or a named bean, a private member an assertion's
  subject. Never tidy any of it.
- A package-private `@Component` is injected by its interface, so its own simple name appears in no other file.
- A token comparison fails both ways unless exact: a regex admitting `(` swallows the paren of markdown
  `![alt](name.png)`, a substring matches `add.png` inside `toolbar_add.png` and a typo'd `512x512.pngs`, so a
  BROKEN reference makes a file look alive. Require a boundary; reject a line by token.
- A linter or compiler is blind by construction on parts of its own project, so its silence proves nothing: none
  reports on its own config file, `@typescript-eslint` synthesises a `React` reference per JSX element so an unread
  import is invisible, and an option absent from memory may be new (`resolve.tsconfigPaths`). Read the package.
- An unread LEADING callback parameter is positional, not dead: dropping `url` from `(url, options) => …`
  shifts `options` onto the first argument. ESLint's `args: 'after-used'` default encodes the same rule.
- A dependency whose only consumer is itself dead counts as used, so a dependency sweep that runs before the code
  sweep misses it: `@eslint/js` survived the npm pass because the dead import in `eslint.config.js` named it.
- A scan keyed on a file list misses whatever owns no file: an extensionless dotfile escaped an extension filter
  yet held the only caller of `nav_auto.html`, and a NESTED class has no file, so `FileUtils
  .ContentTooLargeException` read as an unknown type.
- An identifier is routinely composed at runtime, so its literal appears nowhere: an i18next plural suffix, a
  template literal, prefix composition, a `$ref` tail, JSF `compared_#{bean.order}`, JS `"status-" + status`, the
  tableeditor `t_te_table` id. Build a regex per template, and read the branch (`browser.${id}_confirm`).
- A Jackson MixIn declares abstract methods only to carry annotations, so being named nowhere is the point:
  `OpenApiXmlIgnoreMixIn.getXml` is matched by name against `Schema`. Never delete a member of a MixIn class.
- A commented-out line is often documentation, not a leftover: pseudo-code, the Java equivalent of emitted
  bytecode, an alternative formula, a retained reason or author, an `else`-branch marker, a sample of the string
  built below, a `/* package */` type marker. A scanner must also consume `"""` text blocks as literals.
- javac stops reporting after 100 warnings per compilation and Maven never raises that limit, so a build log read
  as proof is silently truncated: pass `-Xmaxwarns 100000`, and confirm the category is live in the same run.
- A "test sources" path filter must anchor the directory exactly: `(^|/)test/` also matches the PRODUCTION
  packages `src/.../web/test/` and `src/.../testmethod/`, turning live code into a phantom finding.
- A Java type parameter can be used only as the RETURN type, which sits before the parameter list: a scan that
  starts its usage window at the opening paren calls every `<R> R invoke(...)` dead. Start it at the modifiers.
- A manifest field is read by a tool DEEP in the dependency tree, not by the one the project names: Babel, which
  `@vitejs/plugin-react` drives, reads the `browserslist` field because it never sets `browserslistConfigFile`.
- A field initializer assigning the JVM default is redundant EXCEPT where the field can be written before it runs:
  a superclass constructor making a virtual call, or a static block placed above a static field. Only
  `ComponentOpenClass`/`ADynamicClass` call out of a constructor here (`addField`, `addMethod`, `fieldMap`).
- A redundant-construct detector reads the construct, not the type context that makes it load-bearing: a cast IS
  the declared type under `var` (`var x = (double) 10` boxes to Double, without the cast to Integer), explicit
  boxing pins one of the 16 `Operators` overloads per operator, `((Double) o).doubleValue()` differs from
  `(Double) o` on null, and `invoke(m, new Object[]{null})` passes one null ARGUMENT where `invoke(m, null)`
  passes a null ARRAY. Check the declaration and the overload set before believing any of them.
- A redundant construct whose fix needs a REPLACEMENT is a rewrite, not a deletion, and out of scope: `"" + x`
  needs `String.valueOf(x)`, inlining a local into its `return` is a refactor, an empty catch needs a comment.
- A plugin parameter can exist on ONE goal only, so a plugin-level `<configuration>` element is live as soon as
  any bound goal accepts it: maven-jar-plugin defines `skip` on `test-jar` and not on `jar`, which makes the root
  pom's `<skip>${maven.deploy.skip}</skip>` read by the two `test-jar` executions.

## Method rules

- Never pipe a proof grep through `head`, nor read a source from the middle — both hide the refuting caller.
- Re-derive every site of a bulk PMD finding with an own parse of the same construct, and let that parse descend
  into NESTED annotations — the single disagreement is where the real site hides (Lombok `onMethod_`).
- Confirm every survivor of a bulk scan with an individual `grep -rIF` before deleting it — the bulk scan finds
  candidates, the individual search is the proof. For a large candidate set, tokenize the whole corpus in ONE
  pass and set-difference: per-candidate regex never finishes, one pass over 13.4k text files takes a minute.
- Anchor a per-line regex with `[ \t]*`, never `\s*`: a greedy `\s*` eats the newline, skipping the next tag.
- A module that publishes nothing has no public API to protect: `maven.deploy.skip` plus `pom` packaging (as in
  `DEV/org.openl.rules.gen`) means no artifact is ever released, so its public members are internal code. The
  full set is `DEMO/`, `DEV/org.openl.rules.gen`, `DEV/org.openl.rules.test` and `ITEST/**`; all are swept.
- OpenL models a Java type from `getDeclaredMethods()` alone (`JavaOpenClass.initMethodMap`), keyed on erased
  parameter types, so deleting a declaration a subclass merely inherits still changes the rule-visible signature
  set. A member removal in a rule-reachable type needs that checked, not just the compile.
- Deleting a Java class is provable without compiling its module: a repo-wide search for its simple and its
  fully qualified name plus the reflective string registrations is complete. The same holds for a local or
  private member, whose scope is one file; CI's `Build artifacts` job is then the compile gate.
- Sonar's reliability rating on new code follows the WORST severity — Critical is D, Major only C — so one
  pre-existing Critical re-attributed because its file entered the diff flips the gate on a deletion-only PR.
  Query the same rule and line on `main` before believing it is new; `WorkbookListener.java:273` was.
- Search an accessor by its property name as well as its method name: Velocity `$w.propertyType` and JSF EL
  `#{bean.propertyType}` call `getPropertyType()` without ever spelling it.
- When this container's npm rewrites unrelated lock metadata, remove the lock's own regions by hand instead and
  prove coherence with `npm ci`, which fails when the lock and `package.json` disagree.
- Before deleting a dead `throws`, read the callers: an unreachable `catch` at a call site becomes a compile
  error, and a caller whose own clause had no other source inherits the finding — deletable only if that caller
  is not itself held by a safety rail.
- For any PMD finding on a FIELD, grep the field name repo-wide before editing: the "never read" window is one
  method, but a field can be read by a callee, another thread, or an injected bean.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Deleting a local variable takes its now-unused import with it, in the same commit — check the type's every
  remaining occurrence, bare and parameterized, or Spotless removes it later as unexplained churn.
- Dead CSS and an unreachable `/action/*` servlet have maintainer precedent; only a removal a user could
  observe needs a release-notes entry, which an internal endpoint with no button never is.
- Maven silently ignores a `<configuration>` element no mojo declares, so the oracle is `META-INF/maven/plugin
  .xml` inside the plugin jar in `~/.m2`: every `<parameter><name>` and `<alias>` per goal, and the goals it has.

## Keep-list

- `org.openl.rules.tableeditor` `js/*.js` and `css/*.css` are build inputs: `HTMLRenderer` loads
  `js/tableeditor.min.js` and `css/tableeditor.min.css`, concatenated by `compile.js.sh` / `compile.css.sh`.
  Editing a source means regenerating both bundles in the same commit.
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
- `help:effective-pom -Pitest` writes all 172 effective poms into one 20 MB file in seconds — the cheapest proof.
- `dependency:analyze-only` needs `-fae`; Error Prone contributes nothing — PMD is the only Java signal.
- Frontend verification is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`, `npx eslint <files>`,
  `npx vitest run` (183 files, 1699 tests, ~3.5 min) and `npm run build` — the WHOLE gate for a TypeScript-only
  diff, since that is what frontend-maven-plugin runs. Never judge it while Maven holds the same `node_modules`.
  `npx eslint ./src` exits 1 on `main` (15 `object-curly-spacing` errors, 2 warnings) — never fix those.
- `compile.js.sh` reproduces the tableeditor JS bundles byte for byte; `compile.css.sh` drops the final newline of
  `tableeditor.min.css`, so restore it.
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
- `gh` and `xxd` are absent and `git push --delete` gets 403, but `$GITHUB_TOKEN` is set: `curl` the REST API to
  read a PR body to a file and `PATCH` it back. An MCP body argument also swallows angle-bracketed text.
- `sonarcloud.io`'s WEB UI is blocked (403) but its API answers 200, so a gate failure IS diagnosable here:
  `api/issues/search?componentKeys=org.openl.rules:openl-tablets&pullRequest=<n>&types=BUG` names every issue,
  and the same query without `pullRequest` gives `main`'s open BUGs, which is how one is proved pre-existing
  — check `api/project_analyses/search` too, since that list is only as fresh as main's last analysis.
  `&facets=rules&ps=1` on that same endpoint ranks every rule by hit count without returning an issue, and
  `api/qualitygates/project_status` gives the gate's own conditions; an unknown `rules=` key is silently IGNORED
  and returns the unfiltered total, so treat a suspiciously round 7504 as "no such rule", never as a finding.
- Write the ledger through `git worktree add --detach <dir> origin/dead-code/ledger`, never an orphan-branch dance.

## Exhausted veins

Java, all of it closed. PMD's five dead-code rules over all 4031 files (50 violations, one real); every
`@SuppressWarnings` key (151, only the 14 `deprecation` ones decidable, one dead); every JavaDoc `@param`,
`@return`, `@throws` and `{@inheritDoc}` (nine dead tags shipped, one `@return` deferred); commented-out code in
all 4033 files as `//` runs and `/* */` blocks (58 production files shipped); every method type parameter (only
generic-overload fixtures unused); all 79 `serialVersionUID`; every class-level type parameter of the 105
generic declarations (three unused, all public API); `package-info.java` in a source-less package. Statements:
every bare `super();` (47, all shipped) and every override whose body is only a `super` call (22, none deletable);
the one lone `;` is an enum's required separator, and every `};` is an initializer or an anonymous class.
Members: all 474 package-private methods, 645 package-private fields, 572 enum constants by occurrence, and the
186 non-overriding public or protected members of the 246 package-private top-level and 318 non-public nested
classes — every one has a caller. Whole types: all 977 non-public top-level, all 310 in `.impl.`/`.internal.`,
all 193 test types with no `@Test`, and the 246 non-public production types against a production/test split.
Public members of the non-published modules (133 files, 52 candidates) — all framework-driven. EVERY PMD java
rule whose fix is a deletion has run over all ~4033 files, so that family is SPENT — enumerate the categories
again only to check a NEW rule, never for a new vein. Eleven of its rules report nothing; the rest report only
style (`UselessParentheses`, `UnnecessaryFullyQualifiedName`), load-bearing constructs (`EmptyControlStatement`,
`UselessOverridingMethod`, `DanglingJavadoc` — 104 of 120 an Exigen copyright banner) or rewrites
(`AddEmptyString`, `UnnecessaryLocalBeforeReturn`, `EmptyCatchBlock`, `UselessPureMethodCall`).

SonarCloud's own analysis of `main`, faceted over all 7504 issues and then queried rule by rule, is a second Java
signal PMD does not subsume. Its whole deletion family is now checked: `S1128`/`S1116`/`S3985`/
`S1596`/`S2168`/`css:S4658` report nothing at all, `S1144`, `S1068`, `S1481`, `S1854`, `S2094` and `S1119` report
only false positives or the members safety rail 2 already holds, `S3626` yielded the two `continue`s shipped, and
`S1197`, `S1226`, `S1155`, `S3457`, `S4144`, `S1871`, `S2696` are refactors. `S1130`'s 133 dead `throws` clauses
gave 5 (97 sit on test methods, 16 on published public or protected members, 2 cascade — see *Deferred*), and
`S1172`'s 229 unused parameters give NOTHING: none is private, 52 are test code, 159 are public or protected in a
published module, and the 7 package-private ones need an edit at every call site, which is a refactor. Its `javascript:*` dead-store and
unused-local hits are all in vendored or generated files but for `popup.js`, shipped, and the two deferred
`.xhtml` pages. Only a rule NEW to the Sonar profile is worth another pass.

Maven, all of it closed. `dependency:analyze-only` over all 51 analyzable modules in every scope; all 18
`<exclusion>` entries resolved in isolation (three shipped); all 147 non-import `dependencyManagement` and 25
`pluginManagement` entries (five shipped) and all 16 import BOMs; all 129 `<build><plugins>` declarations; all
112 `<include>`/`<exclude>` patterns, literal and wildcard (four shipped); all 114 `<properties>`; all 9 profile
ids; every empty element and path-valued `sonar.*` property; every system property and `argLine` flag handed to
surefire in all 10 blocks (one shipped); both root `<repositories>` and no `<pluginRepository>`; every one of the
206 pom directories reachable from the reactor; both archetype modules and all four assembly descriptors; all 36
`openl-maven-plugin` mojo `@Parameter` fields. And every top-level child of every `<configuration>` block — 121
elements over 51 blocks and 23 plugins, checked against each plugin's own `plugin.xml`, execution-level ones also
against the goals their execution declares — one dead `requirements` block shipped, everything else accepted.
Also every dependency declaration against its whole parent chain's own `<dependencies>` (two overlaps, both real
scope overrides) and every element whose value is simply the Maven default (three, all deferred).

Resources and descriptors, all of it closed. Unreferenced whole files: all 710 images of every extension, 46
`.xhtml`, 55 non-`studio-ui` `.js`/`.css`, the 8 tracked `.html` files, every non-image non-web resource type
(`.xml`, `.properties`, `.txt`, `.json`, `.yaml`, `.sql`, `.env`, `.csv`, `.vm`, `.tld`, `.groovy`, `.md`, a
catch-all and the extensionless dotfiles), and the leftovers a type census turned up — `.ps1`, `.apt`, `.map`,
`.webmanifest`, `.svg`, `.jj`, all live or vendored. Every non-test `.properties` file again against its actual
loader, not a name search (three findings, the last `deployer.properties`). Keys: `openapi.properties` (625),
webstudio `messages.properties` (46), `ValidationMessages.properties`, all 1316 `studio-ui` locale keys, all 194
`openl-default.properties` keys and the 8 DEMO overrides, and each of the eight flyway placeholders. Duplicate
declarations inside one file: all 118 `.properties`, every `.json`, and `<dependency>`, `<exclusion>`,
`<plugin>`, `<module>` and `<properties>` children in all 208 poms. Descriptors: class references in all 67
convention-loaded ones and paths in the seven webapp descriptors; every entry of the webstudio `web.xml`, the
only one (the `/action/prop_values` servlet shipped); all 8 servlet `param-name`s; the three log4j2
configurations; `compose.yaml` and the `Dockerfile` in full. There is no `.dockerignore` and no `.mvn/`.

Web and frontend, all of it closed. CSS exhaustively: every class and id selector in all eleven own stylesheets
(86 tokens), the 22 inline `<style>` blocks (166) and `DEMO/webapps/ROOT/main.css`, plus the 10 duplicate
selector pairs and every repeated property in a rule block; the own stylesheets hold no at-rule and no custom
property at all — no `@keyframes`, `@font-face` or `--var` anywhere. That is every stylesheet the repository
owns — `studio-ui` ships none, its styling coming from antd and inline styles. JSF: both Facelets taglibs, every
`ui:define` against every `ui:insert`, all 62 `ui:param`, all 45 `<c:set var>`, all 43 `f:facet` names, all 8
`f:param`, the one `<ui:remove>`, no navigation rule anywhere, and every `id` attribute of the 46 pages (323,
70 unreferenced and none deletable — see *Deferred findings*). Also both directions of the attributes on OpenL's
own two tags: the 21 passed by the 6 `rules:tableEditor` sites against `renderkit.TableEditor`, which reads the
attribute map since `UITableEditor` is a bare `UIOutput`, and all 46 `Constants` members that name them — every
one read, only `collapseProps` supported without a caller. JS: function and prototype-method deadness in
`common.js`, `bomjs.js` and every own tableeditor script (119 names), the two own jQuery plugins, all 26 inputs
of the four bundle scripts, and every module-scope variable in those scripts — 13, all in `TableEditor.js`, all
read by its toolbar code. `studio-ui`: all 776 exports, whole-file deadness over its 562 sources, every
`tsconfig.json` option, all 8 `@ts-ignore` (one dead), every `eslint-disable` (none exist), the nine npm scripts,
all 44 npm declarations, every top-level `package.json` field (the CRA `homepage` shipped), the imports of
the three files no linter covers (one dead), and `tsc --noUnusedLocals --noUnusedParameters` over the whole
project — 52 findings, the 51 dead `React` imports shipped. Also all 442 `xmlns:` prefixes, all 18 `data-*`
attributes, every `.editorconfig` section, `.gitattributes` pattern and `.gitignore` line (one dead), every
Jekyll layout, include and `navigation.yml` url, the whole of `DEV/org.openl.rules.gen` (four dead members
shipped), all 5 own `.sh`/`.cmd` scripts, identical-content duplicates across production files, and every
tracked build leftover.

## Human follow-ups

- Delete the abandoned remote branch `dead-code/studio-resources` (PR #2055 closed unmerged, so auto-delete
  missed it); `git push --delete` gets 403 here.
- Decide on the *Deferred findings* entries that are public in a published artifact, the two empty test jars,
  the commented-out test code, and the 153 `(non-Javadoc) @see` markers — each is dead, each needs a human's word.
- Restore what `Util/openl-maven-plugin/site/site.xml` links to: the report goal lives in
  maven-plugin-report-plugin now, so that `<reporting>` entry generates none of the 8 pages the menu names.
- Correct `@SuppressWarnings("deprecated")` on `RulesUtilsTest.testParseFormattedDouble`: the key is `deprecation`.
- Collapse the duplicated deployment examples: `Docs/examples/production/` and `Docs/production-deployment/`
  hold the same 32 files twice, so every future edit has to be made twice.
- Fix the `studio-ui` web manifest: its 512x512 icon entry names `android-chrome-512x512.pngs`, one character off
  the file that exists, so that icon never loads. A typo fix, not a deletion.
- Stale documentation, text fixes rather than deletions: `rules-projects.md` names a nonexistent
  `TablePropertyValidatorsWrapper.init()`; `common-tasks.md` and the `studio-ui` README name npm scripts the
  project no longer has; the archetype descriptor name is a WSO2 leftover.

## Run log

- Runs 35-37: types 110-118 plus the SonarCloud family shipped ~180 lines; the PMD deletion family is spent.
- Run 38: `java:S1130` shipped 5 dead `throws` clauses; `java:S1172` assessed and closed; every detector is spent.
