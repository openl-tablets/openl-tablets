# Dead-code sweep ledger — openl-tablets

## Resume point

PR #2063 (types 67-72, 77-79, 88-90) is open; only the known `IT (studio-acl)` Oracle flake is red, and the type
90 push is its retry. Java members, resources, descriptors, build configuration, every scope of the dependency
analysis, JavaDoc tags and now the project manifest are closed. No cheap vein is left: the next run should expect
to spend its time on PR maintenance, and treat a zero-finding pass as the normal outcome.
CONCURRENCY: sessions two hours apart share this ledger and the same PR — add what is missing instead of
replacing another run's text, treat a CI event for a superseded `head_sha` as stale, never arm a check-in chain.

## Change-type queue

All 93 closed — twenty-nine shipped a deletion, sixty-four found nothing; *Exhausted veins* records what each
covered. Numbering continues at 94.

## Open PR

- #2063, branch `dead-code/dead-suppressions`, head `1648ee33`, 17 files and 85 deleted lines. Nine commits, one
  per change type: the dead `@ts-ignore` and Java `deprecation` suppression (67, 72), the `syntax: glob` line
  with the unread `.gitconfig` (68, 70), the surefire property nothing reads (71), the build configuration
  naming absent files (77), the unused ESLint config import (78), its `@eslint/js` dev dependency (79), the
  unloadable `deployer.properties` (88), the JavaDoc tags documenting nothing (89) and the CRA `homepage` field
  (90). No human review comment on any head; CodeRabbit and the Sonar gate both pass.

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
  `js/datepicker.*`, `css/datepicker.css`, `js/prototype/*`, webstudio `diff2html.*` and `javascript/vendor/**`.
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
  `archetype-metadata.xml`, `CITATION.cff`, `Gemfile`, `compose.override.example.yaml`, `.idea/**`, the three
  `favicon.ico` files, and the empty `flyway.location` markers and `file.jar` / `file.zip` zero-byte fixtures.
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
  `_includes` overrides a theme file of the same name, so nothing has to name it. All three such files are live.
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

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails two ways, both infrastructure and never the diff: `ORA-12516
  no protocol handler for TCP ready`, the listener not yet accepting connections, or `Failed requests: expected
  <0> but was <5>` raised after Oracle and Jetty are up and the suite has run for two minutes. One rerun.
- `IT (services-data)` — `RunKafkaSmokeITest.setUp` or `RunStoreLogDataITest.setUp` exiting 1 inside the
  `apache/kafka-native:latest` entrypoint, before any test body, is upstream. Never pin the tag. One rerun.
- `Sonar analysis` — `jacoco:report-aggregate` fails with "Unknown block type c7", a malformed `.exec` from the
  overlapping `coverage-*` artifacts the job merges. Transient, and it suppresses the gate entirely because
  nothing is uploaded. One rerun per SHA.
- `rerun_failed_jobs` is refused while a job still runs — 403, or a bare 500 that is not failure. Read `run_attempt`.

## Container facts

- The build must run ONLINE: `-o` fails before the reactor starts, because `main` keeps bumping dependencies past
  what the image's `~/.m2` holds. `build.shibboleth.net` answers 200, so the root `pom.xml` needs no surgery.
- The WHOLE reactor now builds here, webstudio included, in 18 minutes and with no `-pl` exclusions:
  `LANG=C.UTF-8 LC_ALL=C.UTF-8 mvn clean install -Dquick -DnoPerf -T1C -B`. That locale is required: the
  container's own is POSIX, and `ZipArchiveValidatorTest.testArchives` then dies on `InvalidPath ... unmappable
  characters` for a Cyrillic file name and takes the nine studio ITEST modules down with it.
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

- PMD dead-code rules (`UnusedPrivateField`, `UnusedPrivateMethod`, `UnusedLocalVariable`, `UnusedAssignment`,
  `UnusedFormalParameter`) over ALL 4031 Java files, tests and webstudio included: 50 violations, one real.
- Public members of the non-published modules (`DEV/org.openl.rules.test`, `ITEST/**`, 133 files, 52 candidates)
  and of every `.impl.`/`.internal.` production package (1514 declarations, 595 distinct names, 306 files): every
  one framework-driven or referenced elsewhere — a JPA accessor, a Spring `@Bean`, an override, a workbook
  property. Zero findings in either.
- Unreferenced whole files: all 710 images of every extension, 46 `.xhtml`, and 55 non-`studio-ui` `.js`/`.css`.
  `STUDIO/studio-ui` has no stylesheet of any kind, so there is no React CSS vein to open.
- All 151 Java `@SuppressWarnings`: the build enables only `-Xlint:deprecation` beside Error Prone, so just the
  14 `deprecation` keys are decidable; the 10 in modules that build here were stripped and recompiled, one dead.
  Every other key is one javac, Error Prone, Sonar or an IDE defines; only the typo'd `deprecated` names nothing.
- All 8 `@ts-ignore` directives in `studio-ui` (one dead) and every `eslint-disable` (none exist). Every
  `.editorconfig` section, `.gitattributes` pattern and `.gitignore` line against the file types actually
  present: one dead line, the rest defensive globs. The repository has no `.dockerignore` and no `.mvn/`.
- Every JavaDoc `@param` (544 files), `@return`, `@throws` type and `{@inheritDoc}` (100 sites) against the
  declaration it documents: nine dead tags shipped from five files, one `@return` deferred. Every other
  `{@inheritDoc}` sits on an `@Override`, and every `@throws` names a real class, nested ones included.
- Unused keys in `openapi.properties` (625), webstudio `messages.properties` (46), `ValidationMessages
  .properties` and all 1316 `studio-ui` locale keys. `DEV`, `WSFrontend` and `Util` hold no bundle, and every
  locale-suffixed bundle in the tree is an ITEST or `org.openl.rules.test` fixture.
- Duplicate declarations inside one file: keys in all 118 `.properties` (continuation-aware) and every `.json`,
  `<dependency>`, `<exclusion>`, `<plugin>`, `<module>` and `<properties>` children in all 208 poms, and every
  property repeated in one CSS rule block in all 17 stylesheets. The one hit is the deliberate `specs.properties`
  fixture; a repeated CSS property always carries a different value, so it is a browser fallback.
- The nine `studio-ui` npm scripts (only `clean` is unnamed); identical-content duplicates across production
  files (only the two `Docs` example trees, which stay). CSS, exhaustively: every class and id selector in all eleven own stylesheets (86 tokens), in the 22 inline
  `<style>` blocks (166) and in `DEMO/webapps/ROOT/main.css`; the 10 duplicate selector pairs, each declaring
  disjoint properties; no animation name or custom property is declared; no page includes one resource twice.
- Function and prototype-method deadness in `common.js`, `bomjs.js` and every own tableeditor script (119 names),
  plus the two own jQuery plugins: `.popup` and `.multiselect` are both called, and every option key each defines
  is read by its own body, so the widget-option shape yields nothing here either.
- Unused-export scan over all 776 exports in `STUDIO/studio-ui/src`, and whole-file deadness over its 562
  source files — only tests and `.d.ts` files are unreferenced, which is expected. Every `tsconfig.json` option
  is one `tsc` accepts, so none can be dead.
- Whole-type deadness, repo-wide: all 977 non-public top-level types, all 310 in `.impl.`/`.internal.` packages,
  all 193 test-source types with no `@Test`, and the 246 non-public production types against a production/test
  split — none is test-only; every name-unique candidate is a framework-discovered fixture.
- `dependency:analyze-only` over the 51 analyzable modules, every scope. Each compile-scope finding is a runtime
  provider or is consumed transitively by a dependent; each test- and provided-scope finding is the root pom's
  inherited block, an annotation processor (JMH), a driver named only in a JDBC URL, or an aggregator artifact.
- All 18 `<exclusion>` entries in every pom, each resolved in isolation under `-Dverbose`: three dead on
  `azure-storage-blob` and shipped, eleven suppress a real node, one (`poi-ooxml-lite`) is deliberately replaced
  by `poi-ooxml-full`, and three belong to `openl-maven-plugin` `it/` fixtures and are out of scope.
- All `dependencyManagement` (147 non-import) and `pluginManagement` (25) entries, keyed by group, artifact, type
  and classifier against every declaration in every pom, profiles and reporting included. Five dead and shipped.
  All 16 import-scope BOMs are declared by a module or are a transitive pin their own comment documents.
- All 9 Maven profile ids (only the root, ITEST and `openl-maven-plugin` poms declare any), all 8 servlet
  `param-name` entries, all 114 pom `<properties>`, all 44 `studio-ui` npm dependencies, all 194
  `openl-default.properties` keys and the 8 DEMO overrides, both root `<repositories>` entries (no pom declares a
  `<pluginRepository>`), and all 26 inputs of the four tableeditor bundle scripts. No finding in any of them.
- Every top-level `package.json` field (one dead, the CRA `homepage`); all 43 `f:facet` names, each a standard
  RichFaces facet of the component it sits in; all 129 `<build><plugins>` declarations, where the 89 without an
  executions block are reached by the packaging lifecycle, a CLI goal or the `<reporting>` section; and every
  method type parameter in all 4030 `.java` files, where the only unused ones are generic-overload fixtures.
- All 112 `<include>` / `<exclude>` patterns in every pom, literal and wildcard, against the working tree: three
  surefire excludes and one resource include named files nothing creates, and shipped; every other absent path is
  build output. Every one of the 206 pom directories is reachable from the root reactor, `openl-maven-plugin`
  `it/` invoker projects and the two `Docs` example trees excepted, and no `<module>` names a missing directory.
- Every `org.openl` class and package reference in every configuration file, and all 24 component-scan base
  packages. One finding; the rest resolve or are third-party.
- Whole-file deadness over every non-image, non-web resource type outside test fixtures — `.xml`, `.properties`,
  `.txt`, `.json`, `.yaml`, `.sql`, `.env`, `.csv`, `.vm`, `.tld`, `.groovy`, `.md` plus a catch-all, and the
  extensionless dotfiles that filter missed. Then every non-test `.properties` file again, against its actual
  loader rather than a name search: three findings in all, the third `deployer.properties`. Each of the eight
  flyway placeholders is used by a migration and no dialect file repeats a value it inherits.
- Every system property and `argLine` flag any pom hands surefire, all 10 blocks: one dead, the rest read by a
  `System.getProperty` call, the OpenL Spring property source, the JDK or log4j. No pom declares environment
  variables and none uses failsafe.
- In production sources: all 474 package-private methods and 645 package-private fields are referenced, and of
  all 572 enum constants counted by occurrence, the 10 candidates all reach `values()` or a published enum.
  Public and protected members of the 246 package-private top-level and 318 non-public nested classes — 186
  non-overriding members — all have a caller. Java member deadness is closed entirely.
- Class references in all 67 convention-loaded descriptors (`web.xml`, `faces-config.xml`, taglibs, `*.tld`,
  `META-INF/services/*`, `spring.factories`, both `beans.xml`) and paths in the seven webapp descriptors: every
  named class exists, the one finding was the TLD whose classes cannot satisfy the JSP contract. Every entry of
  the webstudio `web.xml`, the repository's only one — six filters, both servlets, three listeners, the error
  page and every mapped URL — against every client: one finding, the `/action/prop_values` servlet.
- Both Facelets taglibs (3 custom tags, each used by a page), every `ui:define` name against every `ui:insert`
  (only `content` and `title` exist, both matched), all 62 `ui:param` declarations (38 names, 14 pages — every
  one read), and every Jekyll layout, include and `navigation.yml` url under `Docs/`. No finding.
- All 442 `xmlns:` prefix declarations in the 279 XML, XHTML, TLD, XSD and HTML files that carry one: every
  prefix is used by a tag or an attribute in its own file.
- The three log4j2 configurations: every appender used, no named logger declared. `compose.yaml` uses all five
  volumes and services; the `Dockerfile` uses every stage, `ARG` and `ENV`.
- The whole of `DEV/org.openl.rules.gen`: all 12 templates against the variables the generator supplies, both
  directions, plus every public member of the eleven helper classes. Four dead members, now shipped.
- Both archetype modules, both directions, and all four assembly descriptors. Pom resource directories all exist,
  and every module re-declaring an inherited dependency changes its scope. Docs poms and `it/` fixtures are out.
- All 45 JSF `<c:set var>` declarations, every one read by an EL expression; all 36 `openl-maven-plugin` mojo
  `@Parameter` fields, every one read by its goal; and all 5 own `.sh`/`.cmd` scripts. No finding in any.
- All 79 `serialVersionUID` declarations — the 4 whose type looks unserializable inherit it from a framework
  base. The one `<ui:remove>` block in the repository is prose, and no faces config declares a navigation rule.
- Commented-out code in every language: all 4033 `.java` files as `//` runs and `/* */` blocks, all `/* */`
  blocks in `.css`, `.js` and `studio-ui`, every HTML comment, every XML comment holding markup, and every
  commented-out setting in a `.properties` file. Shipped from production sources only (58 files); test sources,
  the documentation-shaped blocks and every vendored library stay.
- Imports in the three own JS/TS files no linter covers (`eslint.config.js`, `vite.config.ts`,
  `vitest.setup.ts`): 12 statements, one dead; every `studio-ui` npm declaration re-checked against it. The
  ESLint flat config configures no rule twice — the one `no-console` override is a deliberate second block.
- `package-info.java` in a source-less package (6), every `f:param` name (8, each read by `getRequestParameter`
  or an EL `param.` reference), every `data-*` attribute in own markup (18, each read by a selector, `.attr()`
  or the island host), every empty element and path-valued `sonar.*` property in every pom, and every tracked
  build leftover (`.class` files only, all of them inside test fixtures). No finding in any of them.

## Human follow-ups

- Allowlist `sonarcloud.io`, or paste the rule key and file/line when the gate fails — undiagnosable from here.
- Delete the abandoned remote branch `dead-code/studio-resources` — auto-delete does not reach it, since PR
  #2055 closed unmerged. `git push --delete` gets HTTP 403 here and the MCP server has no delete-branch tool.
- Decide on the *Deferred findings* entries that are public in a published artifact, the two empty test jars,
  the commented-out test code, and the 153 `(non-Javadoc) @see` markers — each is dead, each needs a human's word.
- Correct `@SuppressWarnings("deprecated")` on `RulesUtilsTest.testParseFormattedDouble`: the key is `deprecation`.
- Collapse the duplicated deployment examples: `Docs/examples/production/` and `Docs/production-deployment/`
  hold the same 32 files twice, so every future edit has to be made twice.
- Fix the `studio-ui` web manifest: its 512x512 icon entry names `android-chrome-512x512.pngs`, one character off
  the file that exists, so that icon never loads. A typo fix, not a deletion.
- Stale documentation, text fixes rather than deletions: `Docs/developer-guides/rules-projects.md` names
  `TablePropertyValidatorsWrapper.init()`, which does not exist; `Docs/onboarding/common-tasks.md` and the
  `studio-ui` README name npm scripts the project no longer has; the archetype descriptor name is a WSO2 leftover.

## Run log

- Run 25: type 88 shipped `deployer.properties` into #2063, proved dead against both wars' whole `lib`; types 86
  (dependency analysis, test and provided scope) and 87 (the own jQuery plugins) closed empty.
- Run 26: type 89 shipped nine dead JavaDoc tags into #2063 — the diff is provably comment-only. The `beans.xml`
  pair, `context.xml`, the DEMO property keys and every unknown `@SuppressWarnings` key proved live.
- Run 27: type 90 shipped the CRA `homepage` field, proved by a byte-identical `dist`; types 91-93 (`f:facet`
  names, pom plugin declarations, method type parameters) closed empty, the last two on false positives only.
