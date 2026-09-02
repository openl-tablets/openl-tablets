# Dead-code sweep ledger — openl-tablets

## Resume point

PR #2063 (types 67-72, 77-79, 88-90, 94) is open, head `defc0ddd`, and now FULLY green — all 14 checks pass, the
Sonar quality gate included, `mergeable_state` clean. It waits only on the owner's merge; nothing on it is
actionable. No human review comment on any head, and CodeRabbit reports none either. All 99 change types are
closed and every vein is exhausted: Java members, whole types, resources, descriptors, build configuration,
every dependency scope, JavaDoc, the project manifest and plugin configuration. Expect a run to be PR
maintenance plus a zero-finding pass, and prefer ledger compaction over inventing a vein. Numbering continues
at 100.
CONCURRENCY: sessions two hours apart share this ledger and the same PR — add what is missing instead of
replacing another run's text, treat a CI event for a superseded `head_sha` as stale, never arm a check-in chain.

## Change-type queue

All 99 closed — 30 shipped a deletion, 69 found nothing; *Exhausted veins* records what each covered.

## Open PR

- #2063, branch `dead-code/dead-suppressions`, head `defc0ddd`, 18 files and 92 deleted lines, 0 added, every
  check green. Ten commits, one per change type: the dead `@ts-ignore` and Java `deprecation` suppression
  (67, 72), the `syntax: glob` line with the unread `.gitconfig` (68, 70), the surefire property nothing reads
  (71), the build configuration naming absent files (77), the unused ESLint config import (78) and its
  `@eslint/js` dev dependency (79), the unloadable `deployer.properties` (88), the JavaDoc tags documenting
  nothing (89), the CRA `homepage` field (90) and the plugin `requirements` block no mojo accepts (94). Derive
  the counts with `git log --oneline` and `git diff --shortstat` before editing the body.

## Merged PRs

- #2054/#2056, #2058, #2060 and #2062 merged, no review comment on any. The owner merges with or without a green
  gate, accepts one commit per change type; merged branches auto-delete.

## Module coverage

- Nothing open: every module, main and test sources, webstudio included; only ITEST fixtures are out of scope.

## Deferred findings

- Public members in a published artifact, dead but held back by safety rail 2: `TableViewerTag` and
  `TableEditorTag` (their `faces-config.xml` component types are the live path), five `XlsProjectionType` `CELL_*`
  constants, `DecisionTableBuilder.methodName`, `SimpleGroup.description` and `MergeResult.status` (each takes a
  public setter with it), and ~190 accessors named only at their declaration.
- The `test-jar` executions of `org.openl.rules.ruleservice` and `.deployer` include only a package neither
  module has, so each publishes an empty test jar no pom consumes; un-publishing an artifact is a human's call.
- `MergeRequest`, `ResolveConflictsRequest`, `ResolveConflictsResponse` (`studio-ui` `MergeModal/types.ts`) —
  unused in code, but `Docs/api/projects-merge-api.md` documents all three.
- ~70 exported types in `studio-ui` are used only inside their own file (dropping `export` is a refactor), and
  `npm run clean` is the one script nothing names — but an npm script is a human entry point.
- The tableeditor `css/tooltip.css` `tooltip_skin-*` and `tooltip_top_*` classes are the widget's theming API.
- `kafka-clients` is declared only by `org.openl.rules.ruleservice.kafka`, which never touches it, yet two
  modules reach it transitively from there; removing it needs a declaration added elsewhere.
- `studio-ui` `eslint.config.js` registers the `react-hooks` plugin but enables no rule from it, so the
  registration is inert; enabling the rules and dropping the plugin are both changes of intent, not deletions.
- The 153 `/* (non-Javadoc) @see ... */` markers beside overriding methods — redundant next to `@Override`, but
  comment churn rather than dead code, so a 600-line diff needs a maintainer's word first.
- Commented-out code left standing: two alternatives sharing a line with live code (`ColumnDescriptor
  .loadMultiRowArray`, `XlsSheetGridModel.setCellStyle`), the rejected `provided` scope in `DEV/org.openl.commons`
  its own comment explains, and ~30 runs in TEST sources, where a disabled body can be a known-issue marker.
- The class-level `@return` of `TableVersionComparator` is real prose describing `compare`, which JavaDoc drops
  because a type returns nothing; moving it onto the method is a refactor, not a deletion.
- 47 `<dependency><version>` elements repeat the version the root already manages — 44 in `jacoco-report`, one
  each in `itest.storelogdata`, `itest.tracing` and the root's own lombok. Maven reads them and they pin, so
  they are a DRY fix for a human, not a deletion this routine may make.
- The one `<reporting>` block (`Util/openl-maven-plugin`) declares maven-plugin-plugin, which ships no report
  mojo, so it generates nothing. Broken rather than dead — migrating it is the *Human follow-ups* entry, and the
  declaration stays either way. Only its `requirements` configuration was dead, and that shipped.
- Every `id` attribute in the 46 `.xhtml` pages: 70 of the 323 are named by nothing else in the repository, and
  none is deletable. A JSF id is a naming-container prefix whose removal renames every descendant's client id
  (`h:form`, `f:subview`), or the anchor out-of-repo UI automation clicks, or what keeps a generated client id
  stable instead of `j_idt123`. An unreferenced id here is markup, not dead code.

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
- A `provided`-scope "unused declared" dependency finding IS real when it sits in the module's own
  `<dependencies>` block. The scope heuristic only dismisses the root POM's inherited block.
- An "unused declared" finding on an AGGREGATOR artifact is always false: `junit-jupiter` carries no class of its
  own, so a reference to `Assertions` resolves to `junit-jupiter-api` beneath it and the declaration is the path.
- A managed dependency or plugin that NO module declares is normally still live: a deliberate transitive version
  pin whose neighbouring comment or release note names the CVE, or a plugin the default lifecycle, the packaging,
  a workflow goal or a `site/` configuration reaches. Only an entry no build can produce or reach is dead.
- A pom `<include>`/`<exclude>` naming a path absent from git is usually build-generated (`jetty-home/`, `logs/`,
  `release.properties`) — but a WILDCARD pattern naming a source file (`**/SomeTest.java`) is not generated by
  anything, and three such surefire excludes were dead. Decide by what could create the path, not by its absence.
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
- A token comparison fails in both directions unless it is exact: a regex admitting `(` swallows the paren from
  markdown `![alt](name.png)`, a substring search matches `add.png` inside `toolbar_add.png` and a typo'd
  `512x512.pngs` inside a manifest, so a BROKEN reference makes a file look alive; and a greedy indent in a Java
  signature regex backtracks past a visibility keyword. Require a boundary; reject a line by token.
- Read the installed package's own source before calling a configuration key dead: an option missing from your
  memory of the API may be a recent addition, as `resolve.tsconfigPaths` is in Vite 8.
- A dependency whose only consumer is itself dead counts as used, so a dependency sweep that runs before the code
  sweep misses it: `@eslint/js` survived the npm pass because the dead import in `eslint.config.js` named it.
- A scan keyed on a file list misses whatever owns no file of its own: an extensionless dotfile is invisible to an
  extension filter and held the only caller of `nav_auto.html` (`Docs/_includes/nav_list`), and a NESTED class has
  no file at all, which made the live `@throws FileUtils.ContentTooLargeException` look like an unknown type.
- An identifier is routinely composed at runtime, so its literal appears nowhere: an i18next plural suffix, a
  template literal, prefix composition, a `$ref` tail, a digit-leading segment, JSF `compared_#{bean.order}`, JS
  `"status-" + status`, the tableeditor `t_te_table` id. Build a regex per template, and read the branch too —
  `browser.${id}_confirm` fires only when `id === 'unlock'`.
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
- A plugin parameter can exist on ONE goal only, so a plugin-level `<configuration>` element is live as soon as
  any bound goal accepts it: maven-jar-plugin defines `skip` on `test-jar` and not on `jar`, which makes the root
  pom's `<skip>${maven.deploy.skip}</skip>` read by the two `test-jar` executions.

## Method rules

- Never pipe a proof grep through `head` and never read a source from the middle — both hide the refuting caller.
- Confirm every survivor of a bulk scan with an individual `grep -rIF` before deleting it — the bulk scan finds
  candidates, the individual search is the proof. For a large candidate set, tokenize the whole corpus in ONE
  pass and set-difference: per-candidate regex never finishes, one pass over 13.4k text files takes a minute.
- Anchor a per-line regex with `[ \t]*`, never `\s*`: a greedy `\s*` consumes the newline, so the match runs past
  the line end and the scan silently skips the next line's tag. Print the capture to check.
- A module that publishes nothing has no public API to protect: `maven.deploy.skip` plus `pom` packaging (as in
  `DEV/org.openl.rules.gen`) means no artifact is ever released, so its public members are internal code. The
  full set is `DEMO/`, `DEV/org.openl.rules.gen`, `DEV/org.openl.rules.test` and `ITEST/**`; all are swept.
- Safety rail 2 admits `.impl.` / `.internal.` code even in a published artifact, which is the one way a public
  member becomes deletable. That subset is swept and empty.
- Deleting a Java class is provable without compiling its module: a type can be named only by its simple or
  fully qualified name, so a repo-wide search for both plus the reflective string registrations is complete —
  that is how the webstudio servlet shipped before that module could build here. The same holds for a local or
  private member, whose scope is one file; CI's `Build artifacts` job is then the compile gate.
- Search an accessor by its property name as well as its method name: Velocity `$w.propertyType` and JSF EL
  `#{bean.propertyType}` call `getPropertyType()` without ever spelling it.
- A resource named by no file in the repository can still be loaded by a DEPENDENCY, by filename convention:
  CXF's `AbstractHTTPServlet` reads `/WEB-INF/cxfServletStaticResourcesMap.txt`, then `/<same name>`. Decompress
  every class of both built wars' `WEB-INF/lib` (295 and 189 jars, a minute each) and search the literal base
  name, then prove the value it feeds is never read — a loader is not a reader.
- Resolve every dotted reference in configuration against the repository: collect each `.java` file's package,
  then check class and package names separately. Artifact ids share the shape, so exclude POMs.
- No tool reports on its own configuration file: ESLint's `files` covers `./src/**` only and the tsconfig
  `include` omits `vite.config.ts`, so read those by hand; byte-identical linter output is then the proof.
- When this container's npm rewrites unrelated lock metadata, remove the lock's own regions by hand instead and
  prove coherence with `npm ci`, which fails when the lock and `package.json` disagree.
- For any PMD finding on a FIELD, grep the field name repo-wide before editing: the "never read" window is one
  method, but a field can be read by a callee, another thread, or an injected bean.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Deleting a local variable takes its now-unused import with it, in the same commit — check the type's every
  remaining occurrence, bare and parameterized, or Spotless removes it later as unexplained churn.
- Dead CSS and an unreachable `/action/*` servlet have maintainer precedent; only a removal a user could
  observe needs a release-notes entry, which an internal endpoint with no button never is.
- A comment-only removal is proved by the diff, not a build: strip every comment from both versions of each
  touched file and compare, treating a maximal comment run as the unit and dropping the blank line it orphaned.
- Maven silently ignores a `<configuration>` element no mojo declares, so the plugin's own descriptor is the
  oracle: read `META-INF/maven/plugin.xml` out of the plugin jar in `~/.m2` and collect every `<parameter><name>`
  and `<alias>` per goal. That listing also proves which goals a plugin still HAS.

## Keep-list

- `org.openl.rules.tableeditor` `js/*.js` and `css/*.css` are build inputs: `HTMLRenderer` loads
  `js/tableeditor.min.js` and `css/tableeditor.min.css`, concatenated by `compile.js.sh` / `compile.css.sh`.
  Editing a source means regenerating both bundles in the same commit.
- `tableeditor.taglib.xml` and `faces-config.xml` are the live path for the two OpenL Facelets tags; only the
  `.tld` beside them was dead. Never treat the taglib or the faces config as the same finding.
- Keys under `ws.project.openapi.mode.` are reached by
  `#{msg['ws.project.openapi.mode.'.concat(project.openapi.mode.name().toLowerCase())]}` in `project.xhtml`.
- `rf-*` (RichFaces, JS inside a jar) and antd-generated class names can never be proven dead: the 17 `rf-*`
  classes and `ant-select-input` in `common.css` and in three pages' `<style>` blocks stay. RichFaces is alive.
- `openapi.properties` keys are annotation values resolved by `OpenApiPropertyResolverImpl`; all 625 are live.
- Public API in `DEV/**` and every published artifact is off limits even when unused in-repo.
- `ValidationMessages.properties` keys are looked up by a short form: the code drops the `openl.error.` prefix
  and, for exceptions, the three-digit status segment. All live; see the localized-exceptions skill.
- tableeditor `onFailure` is a Prototype callback (`'on' + state`); ITEST `001-Get-Static-CSS` ignores the body.
- Vendored scripts and stylesheets stay untouched — editing one forks the upstream copy: tableeditor
  `js/datepicker.*`, `css/datepicker.css`, `js/prototype/*`, webstudio `diff2html.*`, `javascript/vendor/**` and
  the Rule Services `static/rapi-doc/` bundle with its `.js.map`.
- `serviceDescriptionInProcess` in `ServiceManagerImpl` is published to other beans through
  `@Qualifier("serviceDescriptionInProcess")` getters; its assignments are a deployment protocol, not bookkeeping.
- `META-INF/openl/extension-*.xml` is pulled in by a wildcard `@ImportResource` in `ExtensionsConfiguration`;
  `openl-db-repository-<databaseCode>[-v<major>[.<minor>]].properties` and `-ext` are loaded by a name `Settings`
  composes at runtime; the flyway `db/flyway/**/V*.sql` migrations are loaded by directory convention.
- Webstudio `logging.properties` is the per-webapp JUL configuration a servlet container reads from
  `WEB-INF/classes`; no file names it, and it is the one property file whose loader is the container itself.
- Both `beans.xml` and webstudio `META-INF/context.xml` are read by the container, not by any file here: CDI is
  live (`org.openl.rules.webstudio` declares `weld-servlet-core`) and Tomcat reads the context descriptor.
- Convention files nothing names: `banner.txt` (filtered by the pom), `.claude/**`, `.github/workflows/*`,
  `.github/dependabot.yml` (both ecosystems live), `archetype-metadata.xml`, `CITATION.cff`, `Gemfile`,
  `compose.override.example.yaml`, `.idea/**`, the three `favicon.ico` files, and the empty `flyway.location`
  markers and `file.jar` / `file.zip` zero-byte fixtures.
- Runtime-only artifacts that `dependency:analyze` always calls unused: `jaxb-runtime`, `awssdk:sts`,
  `log4j-slf4j2-impl`, `hibernate-hikaricp`, the CXF `cxf-rt-*` feature and provider jars, and the Jackson
  artifacts the Azure repository pins.
- The `sources` and `gpg-sign` profiles are activated by `release.yml`; `owasp` and `no-sonar` are documented in
  `Docs/architecture/technology-stack.md`. All nine root profiles are live. Both root `<repositories>` entries
  serve OpenSAML, which Maven Central does not host, and `lombok.config` declares only the two copied
  annotations `AGENTS.md` documents.
- `redirectPage` is read by `SessionTimeoutFilter.getInitParameter`; `xForwardedPrefixStrategy` by the
  third-party `de.qaware.xff.filter.ForwardedHeaderFilter`. The other six `param-name`s are framework constants.
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
- `Util/openl-maven-plugin/site/` is the one `site/` tree the root `siteDirectory` points at; its four `.apt`
  pages are all linked from `site.xml`, and `DEMO/start.ps1` is launched by `start.cmd`.

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails two ways, both infrastructure and never the diff: `ORA-12516
  no protocol handler for TCP ready`, the listener not yet accepting connections, or `Failed requests: expected
  <0> but was <5>` raised after Oracle and Jetty are up and the suite has run for two minutes. One rerun.
- `IT (services-data)` — any suite that starts `apache/kafka-native:latest` (`RunKafkaSmokeITest`,
  `RunStoreLogDataITest`, `RunTracingITest`) exiting 1 inside its entrypoint before any test body is upstream,
  and intermittent: one suite can pass while the next fails in the same job. Never pin the tag. One rerun.
- `Sonar analysis` — `jacoco:report-aggregate` fails with "Unknown block type c7", a malformed `.exec` from the
  overlapping `coverage-*` artifacts the job merges. Transient, and it suppresses the gate entirely because
  nothing is uploaded. One rerun per SHA.
- `rerun_failed_jobs` is refused while a job still runs — 403, or a bare 500 that is not failure. Read `run_attempt`.

## Container facts

- `~/.m2` can be EMPTY at session start; it is not warm across runs. The whole reactor still builds cold —
  `LANG=C.UTF-8 LC_ALL=C.UTF-8 mvn clean install -Dquick -DnoPerf -T1C -B`, BUILD SUCCESS over all 85 modules,
  none skipped — but it takes 32 minutes instead of 18. Start it in the harness's background mode as the FIRST
  action of a run that will need Java verification, and do the text-search passes while it downloads. A plugin
  only a release profile runs is absent afterwards; fetch its jar with `dependency-plugin:3.11.0:get`.
- The build must run ONLINE: `-o` fails before the reactor starts, because `main` keeps bumping dependencies past
  what any cache holds. `build.shibboleth.net` answers 200, so the root `pom.xml` needs no surgery.
- That locale is required: the container's own is POSIX, and `ZipArchiveValidatorTest.testArchives` then dies on
  `InvalidPath ... unmappable characters` for a Cyrillic file name and takes the nine studio ITEST modules down.
- One ITEST suite CAN be built here, and needs the `install` lifecycle: `mvn install -Pitest -Dquick -DnoPerf
  -T1C -B -pl ITEST/<suite> -am`, a 28-module reactor. `test-compile` is too early: `unpack-dependencies` of the
  webapp fails with MDEP-98 first.
- PMD run standalone (`pmd-cli` + `pmd-java` fetched into `.toDelete/`, no Maven, no auxclasspath) scans all 4031
  files in a minute, test sources included. Revive it only for a NEW rule; maven-pmd-plugin misses test sources.
- `help:effective-pom -Pitest` writes all 172 effective poms into one 20 MB file in seconds — the cheapest proof.
- `dependency:analyze-only` needs `-fae`; Error Prone contributes nothing — PMD is the only Java signal.
- `javadoc -Xdoclint:all` is only an oracle WITH a classpath: it stops at the first unresolved import, so point
  `-classpath` at the built `target/classes` of the module and its dependencies before reading its tag errors.
- Frontend verification is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`, `npx eslint <files>` and
  `npx vitest run` (183 files, ~3 min); never judge it while Maven runs `-T1C`, and never run npm while the
  reactor is building — its own `npm install` writes the same `node_modules`. `npx eslint ./src` itself exits 1
  on `main` (15 `object-curly-spacing` errors, 2 warnings, six untouched files, no CI lint job) — never fix those.
- `compile.js.sh` reproduces the tableeditor JS bundles byte for byte; `compile.css.sh` drops the trailing
  newline of `tableeditor.min.css`, so restore it. The `yuicompressor` jar is committed. A comment-only edit to a
  bundled source changes `tableeditor.all.js` but never `*.min.*` — the minifier already strips comments.
- `rg` is the tokenizer: `xargs -a <list> rg -oH --no-line-number -w -F -f names.txt` scans the corpus in a
  minute, where `grep -f` never finishes and rg's `--files-from` yields nothing. Never `pkill -f` a grep pattern.
- Run a long build in the harness's background mode and read the log it names: `nohup mvn ... &` hides the exit
  code, a second launch races the same `target/` dirs, and `-rf` breaks modules built earlier but not installed.
- The container's global git config signs commits over ssh, failing `GitRepositoryTest` and
  `SameSecondHistoryOrderTest` with jgit `UnsupportedSigningFormatException`: `git config --global commit.gpgsign 0`.
- The global git identity can be rewritten back to `Claude <noreply@anthropic.com>` mid-session. Re-set it and
  pass `GIT_AUTHOR_*` / `GIT_COMMITTER_*` inline on every commit; `--amend` alone keeps the wrong author,
  so it needs `--reset-author`.
- `git push origin --delete <branch>` fails through the proxy with HTTP 403; normal pushes work. `gh` CLI and
  `xxd` are absent — use the GitHub MCP tools, which swallow angle-bracketed text in a body: name an XML element
  in prose with backticks, never as a tag, or the evidence it carried is silently gutted.
- `sonarcloud.io` is blocked by the sandbox proxy (403), and a failed SonarCloud check run carries only the
  rating — no annotations, no comments — so a quality-gate failure cannot be diagnosed from here.
- The clone is shallow (50 commits), so `git log --diff-filter=A` names the boundary commit, not a file's author.
- `.toDelete/` is gitignored and safe for scratch files; Spotless runs from `validate` on, so check `git status`.
- Write the ledger through `git worktree add --detach <dir> origin/dead-code/ledger`, never an orphan-branch dance.

## Exhausted veins

Java, all of it closed. PMD's five dead-code rules over all 4031 files (50 violations, one real); every
`@SuppressWarnings` key (151, only the 14 `deprecation` ones decidable, one dead); every JavaDoc `@param`,
`@return`, `@throws` and `{@inheritDoc}` (nine dead tags shipped, one `@return` deferred); commented-out code in
all 4033 files as `//` runs and `/* */` blocks (58 production files shipped); every method type parameter (only
generic-overload fixtures unused); all 79 `serialVersionUID`; `package-info.java` in a source-less package.
Members: all 474 package-private methods, 645 package-private fields, 572 enum constants by occurrence, and the
186 non-overriding public or protected members of the 246 package-private top-level and 318 non-public nested
classes — every one has a caller. Whole types: all 977 non-public top-level, all 310 in `.impl.`/`.internal.`,
all 193 test types with no `@Test`, and the 246 non-public production types against a production/test split.
Public members of the non-published modules (133 files, 52 candidates) — all framework-driven.

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
configurations; `compose.yaml` and the `Dockerfile` in full.

Web and frontend, all of it closed. CSS exhaustively: every class and id selector in all eleven own stylesheets
(86 tokens), the 22 inline `<style>` blocks (166) and `DEMO/webapps/ROOT/main.css`, plus the 10 duplicate
selector pairs and every repeated property in a rule block. That is every stylesheet the repository owns —
`studio-ui` ships none at all, its styling coming from antd and inline styles. JSF: both Facelets taglibs, every
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
all 44 npm declarations, every top-level `package.json` field (the CRA `homepage` shipped), and the imports of
the three files no linter covers (one dead). Also all 442 `xmlns:` prefixes, all 18 `data-*` attributes, every
`.editorconfig` section, `.gitattributes` pattern and `.gitignore` line (one dead), every Jekyll layout, include
and `navigation.yml` url, the whole of `DEV/org.openl.rules.gen` (four dead members shipped), all 5 own
`.sh`/`.cmd` scripts, identical-content duplicates across production files, and every tracked build leftover.

## Human follow-ups

- Allowlist `sonarcloud.io`, or paste the rule key and file/line when the gate fails — undiagnosable from here.
- Delete the abandoned remote branch `dead-code/studio-resources` — auto-delete does not reach it, since PR
  #2055 closed unmerged. `git push --delete` gets HTTP 403 here and the MCP server has no delete-branch tool.
- Decide on the *Deferred findings* entries that are public in a published artifact, the two empty test jars,
  the commented-out test code, and the 153 `(non-Javadoc) @see` markers — each is dead, each needs a human's word.
- Restore the plugin documentation `Util/openl-maven-plugin/site/site.xml` links to: the report goal lives in
  maven-plugin-report-plugin now, not in maven-plugin-plugin, so that `<reporting>` entry generates nothing and
  `plugin-info.html` plus the seven `*-mojo.html` pages are dead links.
- Correct `@SuppressWarnings("deprecated")` on `RulesUtilsTest.testParseFormattedDouble`: the key is `deprecation`.
- Collapse the duplicated deployment examples: `Docs/examples/production/` and `Docs/production-deployment/`
  hold the same 32 files twice, so every future edit has to be made twice.
- Fix the `studio-ui` web manifest: its 512x512 icon entry names `android-chrome-512x512.pngs`, one character off
  the file that exists, so that icon never loads. A typo fix, not a deletion.
- Stale documentation, text fixes rather than deletions: `Docs/developer-guides/rules-projects.md` names
  `TablePropertyValidatorsWrapper.init()`, which does not exist; `Docs/onboarding/common-tasks.md` and the
  `studio-ui` README name npm scripts the project no longer has; the archetype descriptor name is a WSO2 leftover.

## Run log

- Run 27: type 90 shipped the CRA `homepage` field, proved by a byte-identical `dist`; types 91-93 (`f:facet`
  names, pom plugin declarations, method type parameters) closed empty, the last two on false positives only.
- Run 28: type 94 shipped the plugin `requirements` block no mojo accepts, proved by each plugin's own
  descriptor; types 95-96 (`.html` whole files, the file types a type census missed) closed empty. Ledger
  compacted from its 400-line ceiling.
- Run 29: no deletion and none to make. #2063 reached fully green, Sonar included, so PR maintenance was a
  no-op; types 97-99 (module-scope JS variables, own Facelets tag attributes, `.xhtml` ids) closed empty.
