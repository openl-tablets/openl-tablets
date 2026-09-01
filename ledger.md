# Dead-code sweep ledger — openl-tablets

## Resume point

NO PR IS OPEN. #2062 merged; cut a fresh `dead-code/<topic>` branch from `origin/main` for any new work.
CONCURRENCY: sessions two hours apart share this ledger and the same PR — add what is missing instead of
replacing another run's text, treat a CI event for a superseded `head_sha` as stale, never arm a check-in chain.
All 66 change types are closed and every remaining lead named in an earlier resume point has been run and came
back empty. Nothing deletable is known to be left; a run that finds nothing is the expected outcome now.

## Change-type queue

All 66 closed — eighteen shipped a deletion, forty-eight found nothing; *Exhausted veins* records what each
covered. Numbering continues at 67.

## Open PR

- None. Cut a branch, then record its number, head and one line per commit here.

## Merged PRs

- #2054/#2056 (42 deletions), #2058 (11 commits, 411 deletions), #2060 (1 commit), #2062 (2 commits, 417
  deletions, types 56-57); no review comment on any. The owner merges with or without a green gate, accepts one
  commit per change type; merged branches auto-delete.
- A comment-only sweep is mergeable: #2062 went in as two commits, so both the deletion-only proof and one
  commit per change type are accepted for comments as well as code.

## Module coverage

- Nothing open: every module, main and test sources, webstudio included; only ITEST fixtures are out of scope.

## Deferred findings

- Public members in a published artifact, dead but held back by safety rail 2: `TableViewerTag` and
  `TableEditorTag` (their `faces-config.xml` component types are the live path, unlike the TLD run 8 deleted),
  five `XlsProjectionType` `CELL_*` constants, `DecisionTableBuilder.methodName` and `SimpleGroup.description`
  (each takes a public setter with it), `MergeResult.status` (a record component its constructor overwrites),
  and ~190 accessors across `DEV`, `STUDIO` and `WSFrontend` named only at their own declaration.
- `MergeRequest`, `ResolveConflictsRequest`, `ResolveConflictsResponse` (`studio-ui` `MergeModal/types.ts`) —
  unused in code, but `Docs/api/projects-merge-api.md` documents all three.
- ~70 exported types in `studio-ui` are used only inside their own file (dropping `export` is a refactor), and
  `npm run clean` is the one script nothing names — but an npm script is a human entry point.
- The `tooltip_skin-*` and `tooltip_top_*` classes in tableeditor `css/tooltip.css` are the widget's theming
  API, unreachable only because its single caller passes none of them.
- `kafka-clients` is declared only by `org.openl.rules.ruleservice.kafka`, whose two classes never touch it, yet
  two other modules reach it transitively from there; removing it needs a declaration added elsewhere.
- `WSFrontend/org.openl.rules.ruleservice.ws.annotation` holds only a pom — a published aggregator of annotation
  dependencies for rule service consumers, so nothing in-repo depends on it.
- The 153 `/* (non-Javadoc) @see ... */` markers beside overriding methods — redundant next to `@Override`, but
  comment churn rather than dead code, so a 600-line diff needs a maintainer's word first.
- Two commented-out alternatives that share a line with live code (`ColumnDescriptor.loadMultiRowArray`,
  `XlsSheetGridModel.setCellStyle`) — removing either edits a live line, so the deletion-only proof is lost.
- Commented-out statements in TEST sources, ~30 runs — disabled bodies (`ValidatorTest`, `TokenizerParserTest`)
  and disabled assertions (`MethodSearchTest`, `PropertyTableTest`, `OpenAPIGroovyScriptGeneratorTest`) can be
  known-issue markers, and the commented `files = new File[] {...}` line in `RulesInFolderTestRunner`,
  `OpenAPIGenerationTest` and `OpenAPIProjectCreatorTest` is a deliberate single-folder debug switch. Type 56
  therefore shipped production sources only.

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
- A reactor `dependency:tree` cannot tell a live `<exclusion>` from a dead one: when the excluded artifact wins
  from another path, the node it suppresses is omitted as a duplicate and never printed. Resolve the dependency
  ALONE, in a scratch project with no dependency management, under `-Dverbose`.
- A `provided`-scope "unused declared" dependency finding IS real when it sits in the module's own
  `<dependencies>` block. The scope heuristic only dismisses the root POM's inherited block.
- A root-pom managed entry that NO module declares is normally a deliberate transitive version pin — the comment
  beside it, or a release note, names the CVE or the provider it corrects. Only an entry whose artifact no build
  can produce, or that no consumer can reach, is dead. Seventeen of nineteen such entries are live pins.
- A managed PLUGIN nothing declares is usually still reached: by the default lifecycle, by a packaging that binds
  it, by a workflow goal (`release:prepare`), or by its own `site/` configuration.
- A pom `<include>`/`<exclude>` path absent from git is usually build-generated (`jetty-home/`, `logs/`,
  `release.properties`); nothing in this shape has ever been dead.
- A path in a descriptor is relative or servlet-mapped, so it looks absent: `html/inputVersion.xhtml` resolves
  under `WEB-INF/taglib/`, `/faces/pages/x.xhtml` under `pages/`, and `/cxf/cxf.xml` inside a dependency jar.
- A `<component-type>` or `<renderer-type>` in a faces config is a dotted identifier, not a class name, so a
  class-existence scan reports it missing. The `<component-class>` beside it is the real class.
- An "overwritten" assignment is load-bearing whenever anything between the two writes can observe it: an early
  return leaves the initializer as what the getter returns, `Condition.await` hands the field to another thread,
  a callee reads a field restored in `finally`, and a getter publishes it to other beans. Read the control flow.
- A "never used" local is load-bearing when the declaration itself is the point: a try-with-resources resource
  whose construction and close are what the test asserts, or a loop variable used only to count iterations.
- A dead-member report from a scan run without a classpath cannot resolve a method reference, an overload picked
  by argument type or a lambda argument, so it calls all three unused. Grep every hit before believing it.
- A private field whose only writer is a public setter is not deletable: the setter goes with it, and that is a
  public API change. The mirror case IS deletable: a field whose only reader is a dead getter goes with it.
- A private field read only by reflection is reported by PMD as unused, so an `UnusedPrivateField` hit in a test
  bean is a false positive until the reflective reader says otherwise (`JsonUtilsTest.BindingClasses`).
- Test sources dominate any unreferenced-member scan and are almost never dead: JUnit 5 classes are required
  package-private and the runner discovers them (674 hits fell to 1); a type with no `@Test` is a fixture, a
  runner or an OpenL bean loaded by name; a private member can be an assertion's SUBJECT. Never tidy any of it.
- A package-private `@Component` is injected by its interface, so its own simple name appears in no other file.
- A token comparison fails in both directions unless it is exact: a regex admitting `(` swallows the paren from
  markdown `![alt](name.png)`, a substring search matches `add.png` inside `toolbar_add.png`, and a greedy indent
  in a Java signature regex backtracks past a visibility keyword. Require a boundary; reject a line by token.
- A file with NO extension is invisible to an extension-filtered grep, and the caller that keeps a finding alive
  can sit in exactly such a file: `Docs/_includes/nav_list` is the only place that includes `nav_auto.html`.
- An identifier is routinely composed at runtime, so its literal appears nowhere. Message keys: a convention
  suffix (an i18next `_one`/`_other` plural pair reached by base key plus `count`), a template literal, prefix
  composition or `$ref` indirection (search the tail too), or a segment starting with a digit, which a
  letter-anchored regex misses. CSS classes and element ids too: JSF `styleClass="compared_#{bean.order}"`, JS
  `"status-" + service.status.toLowerCase()`, and the tableeditor table id, the JSF client id plus `_te` plus
  `_table` (`id="t"` renders `t_te_table`). Build a regex from each template in the corpus, never a bare prefix
  match, and read the branch too — `browser.${id}_confirm` fires only when `id === 'unlock'`.
- A duplicate-key scan over `.properties` must join backslash continuation lines first: a multi-line value that
  embeds JSON or Markdown otherwise reports its own `"params"` and `**Note` lines as repeated keys.
- A Jackson MixIn declares abstract methods only to carry annotations, so being named nowhere is the point:
  `OpenApiXmlIgnoreMixIn.getXml` is matched by name against `Schema`. Never delete a member of a MixIn class.
- A managed entry that looks duplicated inside one pom is usually two artifacts: the pair differs by `type`, so
  a group-plus-artifact key both reports a false duplicate and hides the jar variant that is the real finding.
- A commented-out line is often documentation, not a leftover: pseudo-code naming the branches below it
  (`MatchAlgorithmCompiler`, `IfNode`), the Java equivalent of the bytecode an ASM generator emits (every
  `// bean = new Bean();` in `SpreadsheetResultBeanByteCodeGenerator`), an equivalent formula beside the one in
  use, code kept with its reason next to it (`ResultExport`, EPBDS-7848) or with an author and date (`IntExpImpl`,
  "commented by SV 02.06.03"), an inline `/* if (!blocks && !overrides) */` naming the `else` branch it precedes,
  a sample of the string built below it (`OpenLService`), a `/* package */` visibility marker, and a
  `/* Element */` parameter-type annotation in Prototype-era JS.
- A Java comment scanner must consume `"""` text blocks as literals: a text block holding `rules/*.xlsx`
  otherwise opens a comment that swallows the rest of the file and reports it as commented-out code.
- A `serialVersionUID` looks dead whenever serializability arrives through a framework base a repo-only
  inheritance map cannot see — `RecursiveAction`, `PhaseListener`, `HttpServlet` all carry it. A nested class
  named with `$` also escapes a `\w+` type-name regex, so its field is misattributed to the enclosing class.

## Method rules

- Never pipe a proof grep through `head` and never read a source from the middle — both hide the refuting caller.
- Confirm every survivor of a bulk scan with an individual `grep -rIF` before deleting it — the bulk scan finds
  candidates, the individual search is the proof. For a large candidate set, tokenize the whole corpus in ONE
  pass and set-difference: per-candidate regex never finishes, one pass over 13.4k text files takes a minute.
- A module that publishes nothing has no public API to protect: `maven.deploy.skip` plus `pom` packaging (as in
  `DEV/org.openl.rules.gen`) means no artifact is ever released, so its public members are internal code. The
  full set is `DEMO/`, `DEV/org.openl.rules.gen`, `DEV/org.openl.rules.test` and `ITEST/**`; all are swept.
- Safety rail 2 admits `.impl.` / `.internal.` code even in a published artifact, which is the one way a public
  member becomes deletable. That subset is swept and empty.
- Deleting a Java class is provable without compiling its module: a type can be named only by its simple or
  fully qualified name, so a repo-wide search for both plus the reflective string registrations is complete —
  that is how the webstudio servlet shipped though the module cannot build. The same holds for a local or
  private member, whose scope is one file; CI's `Build artifacts` job is then the compile gate.
- Search an accessor by its property name as well as its method name: Velocity `$w.propertyType` and JSF EL
  `#{bean.propertyType}` call `getPropertyType()` without ever spelling it.
- A `#`-hash link in legacy WebStudio is a server page route, not a React route: the crossroads routes in
  `index.xhtml` build `page + ".xhtml"` from the fragment, so search a page with and without the extension.
- A descriptor loaded by convention is dead when its declared handler cannot satisfy the loader's contract, even
  though the class it names exists: a `.tld` tag class must implement the JSP tag interface, and the tableeditor
  ones extend `UIComponentBase`. Check the contract, not the name — but first confirm the descriptor declares
  nothing a scanner registers on sight, since a TLD listener or function entry is active with no page at all.
- A resource named by no file in the repository can still be loaded by a DEPENDENCY, by filename convention:
  CXF's `AbstractHTTPServlet` reads `/WEB-INF/cxfServletStaticResourcesMap.txt`, then `/<same name>`. Grep the
  dependency jars for the base name, then prove the value it feeds is never read — a loader is not a reader.
- Resolve every dotted reference in configuration against the repository: collect each `.java` file's package,
  then check class and package names separately. Artifact ids share the shape, so exclude POMs.
- Collect Maven dependency consumers by PARSING every pom, not grepping: `<artifactItem>` blocks of the
  dependency plugin consume a managed version exactly as `<dependency>` does, and a grep for the artifact name
  cannot tell the type and classifier apart.
- Verify a `dependencyManagement` or `pluginManagement` removal with `mvn help:effective-pom -Doutput=<file>`
  before and after the edit: the diff must add no line and remove only the entry, once per effective POM.
- Verify an `<exclusion>` removal with a reactor-wide `dependency:tree` before and after: every tree node must be
  byte-identical, which proves no module's resolved graph moves.
- `mvn dependency:analyze-only` after a reactor build costs about a minute. A `compile`-scope "unused declared"
  finding is deletable only when no dependent module reaches the artifact THROUGH it, and `Used undeclared` for
  the same module is empty — swapping one declaration for another is an addition, not a deletion.
- For any PMD finding on a FIELD, grep the field name repo-wide before editing: the "never read" window is one
  method, but a field can be read by a callee, another thread, or an injected bean.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Deleting a local variable takes its now-unused import with it, in the same commit — check the type's every
  remaining occurrence in the file, bare as well as parameterized, or Spotless removes the import for you later
  as unexplained churn.
- Dead CSS and an unreachable `/action/*` servlet both have maintainer precedent on `main`. A removal needs a
  release-notes entry only when a user could observe it, which an internal endpoint with no button never is.
- A public member of a package-private or nested class is not published API, so it is admissible — but its
  caller is usually in the same file or package. Count occurrences repo-wide and read every hit; two is a call.
- Two rule blocks sharing a selector are BOTH live unless the earlier one's property set is a subset of the
  later one's — compare property sets and `!important`, never selectors. Every duplicate here is disjoint.
- A comment-only removal is proved by the diff, not a build: strip every comment from each touched file at both
  ends and compare — identical code proves it, and unlike a "removed lines all start with `//`" check it also
  covers a `/* */` block whose interior lines do not. `git diff --numstat` must add zero lines. Treat a maximal
  comment run as the unit — a per-line rule leaves half a statement behind — and drop the blank line it
  orphaned, comparing `{`-then-blank, blank-then-`}` and double-blank counts against HEAD.

## Keep-list

- `org.openl.rules.tableeditor` `js/*.js` and `css/*.css` are build inputs: `HTMLRenderer` loads
  `js/tableeditor.min.js` and `css/tableeditor.min.css`, concatenated by `compile.js.sh` / `compile.css.sh`.
  Editing a source means regenerating both bundles in the same commit.
- `tableeditor.taglib.xml` and `faces-config.xml` are the live path for the two OpenL Facelets tags; only the
  `.tld` beside them was dead. Never treat the taglib or the faces config as the same finding.
- Keys under `ws.project.openapi.mode.` are reached by
  `#{msg['ws.project.openapi.mode.'.concat(project.openapi.mode.name().toLowerCase())]}` in `project.xhtml`.
- `rf-*` (RichFaces, JS inside a jar) and antd-generated class names can never be proven dead, so the 17 `rf-*`
  classes and `ant-select-input` in webstudio `common.css` and the `rf-*` selectors in the `<style>` blocks of
  `searchForm.xhtml`, `compare.xhtml` and `tree.xhtml` stay. RichFaces is alive: two modules declare it.
- `openapi.properties` keys are annotation values resolved by `OpenApiPropertyResolverImpl`; all 625 are live.
- Public API in `DEV/**` and every published artifact is off limits even when unused in-repo.
- `ValidationMessages.properties` keys are looked up by a short form: the code drops the `openl.error.` prefix
  and, for exceptions, the three-digit status segment. All live; see the localized-exceptions skill.
- `onFailure` in the tableeditor scripts is a Prototype Ajax callback, invoked as `'on' + state`. ITEST
  `001-Get-Static-CSS` asserts only the status and `Content-Type` of `/css/common.css`, never its body.
- Vendored third-party scripts and stylesheets carry commented-out code of their own and stay untouched, since
  editing one forks the upstream copy: tableeditor `js/datepicker.js` and `css/datepicker.css` (DatePicker 5.4,
  CC BY-SA), `js/prototype/prototype-1.7.3.js`, and webstudio `diff2html.*` and `javascript/vendor/**`.
- `serviceDescriptionInProcess` in `ServiceManagerImpl` is published to other beans through
  `@Qualifier("serviceDescriptionInProcess")` getters; its assignments are a deployment protocol, not bookkeeping.
- `META-INF/openl/extension-*.xml` is pulled in by a wildcard `@ImportResource` in `ExtensionsConfiguration`;
  `openl-db-repository-<databaseCode>[-v<major>[.<minor>]].properties` and `-ext` are loaded by a name `Settings`
  composes at runtime; the flyway `db/flyway/**/V*.sql` migrations are loaded by directory convention.
- Convention files nothing names: `banner.txt` (filtered by the pom), `.claude/**`, `.github/workflows/*`,
  `archetype-metadata.xml`, `CITATION.cff`, `Gemfile`, `compose.override.example.yaml`, `.idea/**`.
- Runtime-only artifacts that `dependency:analyze` always calls unused: `jaxb-runtime`, `awssdk:sts`,
  `log4j-slf4j2-impl`, `hibernate-hikaricp`, the CXF `cxf-rt-*` feature and provider jars, and the Jackson
  artifacts the Azure repository pins.
- The `sources` and `gpg-sign` profiles are activated by `release.yml`; `owasp` and `no-sonar` are documented in
  `Docs/architecture/technology-stack.md`. All nine root profiles are live.
- `redirectPage` is read by `SessionTimeoutFilter.getInitParameter`; `xForwardedPrefixStrategy` by the
  third-party `de.qaware.xff.filter.ForwardedHeaderFilter`. The other six `param-name`s are framework constants.
- `Docs/` renders through the remote theme `mmistakes/minimal-mistakes`: a file under `Docs/_layouts` or
  `_includes` overrides a theme file of the same name, so nothing has to name it. All three such files are live.
- `DEV/org.openl.rules.gen` templates and helpers are all reachable: `GenRulesCode.run()` calls all eleven
  `generate*` methods, and every `VelocityTool` method and template variable is used by a template.
- `archetype-resources/pom.xml` is processed by the archetype plugin itself, not by a `<fileSet>`, so no fileset
  has to name it. The two empty `assembly/*.xml` files under `openl-maven-plugin/it/openl-multiproject` are
  fixtures `verify.groovy` asserts are EXCLUDED from the built artifact.
- `DEV/org.openl.commons/test-resources/specs.properties` declares `hello` and `duplicateKey` twice on purpose:
  it is the fixture for the properties-spec parser. Never dedupe it.

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails with `ORA-12516 ... no protocol handler for TCP ready`,
  the Oracle TestContainer listener not yet accepting connections. Infrastructure, never the diff. One rerun.
- `IT (services-data)` — `RunStoreLogDataITest.setUp` cannot start `apache/kafka-native:latest`: log4j
  `StatusLogger` init segfaults in `Pwd.getpwuid` inside the image's own entrypoint, so the container exits 1
  before any test body. The tag floats, never pin it away. One rerun.
- `Sonar analysis` — `jacoco:report-aggregate` fails with "Unknown block type c7", a malformed `.exec` from the
  overlapping `coverage-*` artifacts the job merges. Transient, and it suppresses the gate entirely because
  nothing is uploaded. One rerun per SHA.
- `rerun_failed_jobs` is refused while a job of the run is still in progress — 403 "This workflow is already
  running", or a bare 500. A 500 is NOT proof it failed: on #2060 the run reached `run_attempt` 2 and went green
  after one. Read `run_attempt` before retrying or reporting a budget unspent.

## Container facts

- The build must run ONLINE: `-o` fails before the reactor starts, because `main` keeps bumping dependencies
  past what the image's `~/.m2` holds. Maven Central is reachable; only `build.shibboleth.net` is 403, so the
  `org.opensaml:opensaml-bom` import block must come out of the root `pom.xml` first (`mvn validate -N` needs it
  out too) and go back in with `git checkout -- pom.xml`. OpenSAML is not on Central; no mirror swap fixes it.
- Ten modules need the webstudio WAR and must all be excluded, or the reactor dies on the first of them:
  `mvn install -Dquick -DnoPerf -T1C -B -pl '!:org.openl.rules.webstudio,!:itest.studio.demo,!:itest.studio.disabled-settings,!:itest.studio.acl,!:itest.studio.dtr,!:itest.studio.repos,!:itest.studio.multi,!:itest.studio.simple,!:itest.studio.users,!:itest.studio.sso'`
  That build takes about 20 minutes and installs 55 modules; the remaining ITEST modules, `ruleservice.ws.all`
  and the Maven plugin are skipped, which blocks no change type.
- One ITEST suite CAN be built here, and needs the `install` lifecycle: `mvn install -Pitest -Dquick -DnoPerf
  -T1C -B -pl ITEST/<suite> -am`, a 28-module reactor. `test-compile` is too early: `unpack-dependencies` of the
  webapp fails with MDEP-98 first.
- PMD run standalone (`pmd-cli` + `pmd-java` fetched into `.toDelete/`, no Maven, no auxclasspath) scans all
  4031 files in a minute and is the only way to reach webstudio. Revive the recipe only for a NEW rule, never
  the maven-pmd-plugin route, which misses webstudio and test sources.
- `help:effective-pom -Pitest` writes all 172 effective poms into one 20 MB file in seconds — the cheapest
  whole-reactor verification here, and the proof for any managed-entry removal.
- `dependency:analyze-only` needs the same `-pl` exclusions plus `-fae` (ITEST cannot resolve `server-core`
  outside the itest profile); 51 modules analyze. Error Prone contributes nothing — PMD is the only Java signal.
- Frontend verification works and is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`,
  `npx eslint <files>`, and `npx vitest run` (183 files, ~3 minutes).
- `compile.js.sh` reproduces the tableeditor JS bundles byte for byte; `compile.css.sh` drops the trailing
  newline of `tableeditor.min.css`, so restore it. The `yuicompressor` jar is committed. A comment-only edit to
  a bundled source changes `tableeditor.all.js` (concatenation) but never `*.min.*` — the minifier strips
  comments already, so an unchanged `.min.` file is correct, not a forgotten regeneration.
- Run a long build in the harness's background mode and read the log it names: a `nohup mvn ... &` exit code
  says nothing, killing the launcher leaves the JVM running, and a second launch races the first on the same
  `target/` dirs. `-rf` breaks modules built earlier but never installed. A foreground `sleep` is blocked.
- The container's global git config signs commits over ssh (`gpg.format=ssh`, `commit.gpgsign=true`), which
  fails `GitRepositoryTest` and `SameSecondHistoryOrderTest` in STUDIO Repository Git with jgit
  `UnsupportedSigningFormatException`. Fix once per session: `git config --global commit.gpgsign false`.
- The global git identity can be rewritten back to `Claude <noreply@anthropic.com>` mid-session. Re-set it and
  pass `GIT_AUTHOR_*` / `GIT_COMMITTER_*` inline on every commit; `--amend` alone keeps the wrong author,
  so it needs `--reset-author`.
- `git push origin --delete <branch>` fails through the proxy with HTTP 403; normal pushes work. `gh` CLI and
  `xxd` are absent — use the GitHub MCP tools. A body sent through them loses angle-bracketed text: a
  `maven.deploy.skip` XML element written as a tag pair was stored as a bare `true`, silently gutting the
  evidence it carried. Name an XML element in prose with backticks, never as a tag.
- `sonarcloud.io` is blocked by the sandbox proxy (`CONNECT tunnel failed, response 403`), and a failed
  SonarCloud check run carries only the rating — empty `output.text`, no annotations, no review comments. A
  quality-gate failure therefore cannot be diagnosed from here; say so and ask for the rule key and file/line.
- `.toDelete/` is gitignored (`.gitignore:35`) and safe for scan scratch files. Spotless runs from `validate`
  on; check `git status` after any build and revert churn you did not intend.
- Write the ledger through `git worktree add --detach <dir> origin/dead-code/ledger`; it never touches the
  sweep branch's working tree and needs no orphan-branch dance.

## Exhausted veins

- PMD dead-code rules (`UnusedPrivateField`, `UnusedPrivateMethod`, `UnusedLocalVariable`, `UnusedAssignment`,
  `UnusedFormalParameter`) over ALL 4031 Java files — main and test sources, every module, webstudio included.
  50 violations, one real. Never add `UnnecessaryImport`, which Spotless already owns.
- Public members of the non-published modules (`DEV/org.openl.rules.test`, `ITEST/**`, 133 files): 52 candidates,
  every one framework-driven — a JPA accessor, a Spring `@Bean`, an override, or an OpenL workbook property.
- Public members of `.impl.` / `.internal.` production packages: 1514 declarations, 595 distinct names over 306
  files, every name referenced elsewhere. Zero findings.
- Unreferenced whole files: all 710 images of every extension, 46 `.xhtml`, and 55 non-`studio-ui` `.js`/`.css`.
  `STUDIO/studio-ui` has no stylesheet of any kind, so there is no React CSS vein to open.
- Unused keys in `i18n/openapi.properties` (625), webstudio `messages.properties` (46),
  `ValidationMessages.properties`, and all 1316 `studio-ui` locale keys by leaf name and dotted path. `DEV`,
  `WSFrontend` and `Util` hold no bundle.
- Duplicate declarations inside one file: keys in all 118 `.properties` (continuation-aware) and every `.json`,
  `<dependency>`, `<exclusion>`, `<plugin>`, `<module>` and `<properties>` children in all 208 poms, and every
  property repeated in one CSS rule block in all 17 stylesheets. The one hit is the deliberate `specs.properties`
  fixture; a repeated CSS property always carries a different value, so it is a browser fallback.
- The nine `STUDIO/studio-ui` npm scripts (only `clean` is unnamed), and identical-content duplicates across
  production files (only the two `Docs` example trees, which stay).
- Class-level deadness in webstudio `css/common.css`, `layout/main.css`, `layout/simple.css` and all seven own
  tableeditor stylesheets (77 class tokens), every id selector in the same eleven files (9 tokens), and all 166
  class and id selectors of the 22 inline `<style>` blocks: every one is used or composed at runtime.
- Function and prototype-method deadness in `webapp/javascript/common.js`, `bomjs.js` and every own
  tableeditor script (119 method names).
- Unused-export scan over all 776 exports in `STUDIO/studio-ui/src`, and whole-file deadness over its 562
  source files — only tests and `.d.ts` files are unreferenced, which is expected.
- Whole-type deadness, repo-wide: all 977 non-public top-level types, all 310 types in `.impl.` / `.internal.`
  packages, and all 193 test-source types with no `@Test`-family annotation. Zero real findings — every
  name-unique candidate is a framework-discovered fixture.
- `dependency:analyze-only` over the 51 analyzable modules — every compile-scope finding is a runtime provider or
  is consumed transitively by a dependent.
- All 18 `<exclusion>` entries in every pom, each resolved in isolation under `-Dverbose`: three dead on
  `azure-storage-blob` and shipped, eleven suppress a real node, one (`poi-ooxml-lite`) is deliberately replaced
  by `poi-ooxml-full`, and three belong to `openl-maven-plugin` `it/` fixtures and are out of scope.
- All 147 non-import root-pom `dependencyManagement` entries, keyed by group, artifact, type and classifier
  against every `<dependency>` and `<artifactItem>` in every pom: two dead, seventeen live transitive pins. All
  25 `pluginManagement` entries against every plugin declared anywhere, profiles and reporting included: two
  dead, four reached without a declaration. Non-root blocks the same way: one dead entry in `ITEST/pom.xml` and
  its twin in the root pom, both shipped.
- All 9 Maven profile ids, all 8 servlet `param-name` entries, all 114 pom `<properties>`, all 44 `studio-ui` npm
  dependencies, all 194 `openl-default.properties` keys, every literal include/exclude path in every pom, and all
  26 inputs of the four tableeditor bundle scripts. No finding in any of them.
- Every `org.openl` class and package reference in every configuration file, and all 24 component-scan base
  packages. One finding; the rest resolve or are third-party.
- Whole-file deadness over every non-image, non-web resource type outside test fixtures — `.xml`, `.properties`,
  `.txt`, `.json`, `.yaml`, `.sql`, `.env`, `.csv`, `.vm`, `.tld`, `.groovy`, `.md` plus a catch-all. One
  finding; every other hit is a convention file now on the keep-list. No backup or placeholder file is tracked.
- In production sources: all 474 package-private methods and 645 package-private fields are referenced, and of
  all 572 enum constants counted by occurrence, the 10 candidates all reach `values()` or a published enum.
- Class references in all 67 convention-loaded descriptors (`web.xml`, `faces-config.xml`, `*.taglib.xml`,
  `*.tld`, `META-INF/services/*`, `spring.factories`) and path references in the seven webapp descriptors. Every
  named class exists; the single finding was the TLD whose classes cannot satisfy the JSP contract.
- Every entry of the webstudio web descriptor, the repository's only `web.xml`: all six filters and both servlets
  against their mappings, all three listeners, the error page target, and every mapped URL against every client.
  One finding, the `/action/prop_values` servlet; `/action/*` now has no entry left.
- Both Facelets taglibs (3 custom tags, each used by a page), every `ui:define` name against every `ui:insert`
  (only `content` and `title` exist, both matched), all 62 `ui:param` declarations (38 names, 14 pages — every
  one read), and every Jekyll layout, include and `navigation.yml` url under `Docs/`. No finding.
- All 442 `xmlns:` prefix declarations in the 279 XML, XHTML, TLD, XSD and HTML files that carry one: every
  prefix is used by a tag or an attribute in its own file.
- The three log4j2 configurations: every appender used, no named logger declared. `compose.yaml` uses all five
  volumes and services; the `Dockerfile` uses every stage, `ARG` and `ENV`.
- The whole of `DEV/org.openl.rules.gen`: all 12 templates against the variables the generator supplies, both
  directions, plus every public member of the eleven helper classes. Four dead members, now shipped.
- Both archetype modules, both directions: every `archetype-resources` file against every `<fileSet>`, and every
  fileset against the files it matches. Also all four assembly descriptors. Pom `<resource>`/`<testResource>`
  directories all exist, and every module re-declaring a root-inherited dependency changes its scope. Docs
  example poms and `openl-maven-plugin` `it/` fixtures are out of scope of every pom scan.
- All 45 JSF `<c:set var>` declarations, every one read by an EL expression; all 36 `openl-maven-plugin` mojo
  `@Parameter` fields, every one read by its goal; and all 5 own `.sh`/`.cmd` scripts. No finding in any.
- All 79 `serialVersionUID` declarations — the 4 whose type looks unserializable inherit it from a framework
  base. The one `<ui:remove>` block in the repository is prose, and no faces config declares a navigation rule.
- Duplicate selector blocks in every own stylesheet (10 pairs in `common.css` and `layout/main.css`): each pair
  declares disjoint properties, so none is shadowed. No page includes the same `.js` or `.css` resource twice.
- CSS animation names and custom properties: the repository declares neither. Selector deadness in
  `DEMO/webapps/ROOT/main.css`, the last own stylesheet uncovered — every id and class is used by `index.html`.
- Public and protected members of package-private top-level classes (246 types) and non-public nested classes
  (318 types) — 186 non-overriding members, every one with a caller. This closed Java member deadness entirely.
- Commented-out code, every language: all 4033 `.java` files as `//` runs and all 6511 `/* */` blocks, all 543
  `/* */` blocks in `.css` and `.js`, all 1540 in `studio-ui`, and all 23 HTML comments in `.xhtml`/`.html`.
  Shipped from production sources only (58 files); test sources, 15 documentation-shaped blocks and every
  vendored library stay. Markup and TypeScript hold none at all.

## Human follow-ups

- Allowlist `sonarcloud.io`, or paste the rule key and file/line when the gate fails — undiagnosable from here;
  the owner merges either way. Also mirror the OpenSAML artifacts, or allowlist `build.shibboleth.net`, so
  `org.openl.rules.webstudio` can build here — they are not on Central and PMD scans it without compiling.
- Delete the abandoned remote branch `dead-code/studio-resources` — auto-delete does not reach it, since PR
  #2055 closed unmerged. `git push --delete` gets HTTP 403 here and the MCP server has no delete-branch tool.
- Decide on the *Deferred findings* entries that are public in a published artifact, the commented-out test
  code, and the 153 `(non-Javadoc) @see` markers — each is dead, and each needs a human's word to go.
- Collapse the duplicated deployment examples: `Docs/examples/production/` and `Docs/production-deployment/`
  hold the same 32 files twice, so every future edit has to be made twice.
- Stale documentation, text fixes rather than deletions: `Docs/developer-guides/rules-projects.md` names
  `TablePropertyValidatorsWrapper.init()`, which does not exist; `Docs/onboarding/common-tasks.md` and the
  `studio-ui` README name npm scripts the project no longer has; the archetype descriptor name is a WSO2 leftover.

## Run log

- Run 18: types 53-57 shipped as #2062 (three runs contributed); re-ran the Sonar flake; the owner merged it
  about three hours after opening and its branch auto-deleted.
- Run 19: types 58-60 closed empty; verified #2062's two commits with a 75-module reactor build.
- Run 20: types 61-66 closed empty, no commit; ledger compacted.
