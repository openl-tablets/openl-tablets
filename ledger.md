# Dead-code sweep ledger — openl-tablets

## Resume point

PR #2058 is open on `dead-code/java-unused-members`, 9 commits; maintain it before anything else. Its only red
check is the SonarCloud quality gate, which cannot be diagnosed from here (see *Open PR*). Push no further commit
to that branch while the gate is red — bank new findings for the next PR.
Every change type in the queue is closed. New work needs a NEW detector, not another pass of an old one.
Two shapes still pay: contract-satisfiability (an entry loaded by convention whose declared target cannot exist)
and non-published internals (a module that publishes nothing has no public API to protect — see *Method rules*);
purely name-based veins over published code return nothing.
Candidate next detectors, none tried: Spring bean ids nothing refs; `<exclusion>` entries for artifacts their
dependency never brings; `openl-maven-plugin` mojo parameters no goal reads; `archetype-resources` files.
Only one sweep PR may be open: check `git ls-remote --heads origin 'dead-code/*'` before cutting a branch.

## Change-type queue

All 39 change types are closed; *Exhausted veins* records what each one covered. Thirteen produced a shipped
deletion — 1, 5, 7, 11, 12, 15, 16, 22, 23, 27, 28, 34, 39, named in *Open PR* and *Merged PRs*. The other
twenty-six (2-4, 6, 8-10, 13-14, 17-21, 24-26, 29-33, 35-38) closed with no deletable finding.
Numbering continues at 40 for any new detector.

## Open PR

- #2058 on `dead-code/java-unused-members`, head `8b8003615c`, 9 commits, 12 files, 4 insertions / 397 deletions.
  - `9a074c89c3` Remove the never-read base folder normalization in MappedRepository — change type 7.
  - `f09316534a` Drop the unused base folder parameter of the project index scan — change type 11.
  - `481a3ed7cd` Delete descriptor resources that no loader can ever read — change type 16, two files.
  - `7d41eeea8c` Drop the JSP API dependency that no TableEditor source uses — change type 12.
  - `af5b8accc7` Drop dependency management entries that no module declares — change type 23, two entries.
  - `65e0bc1d41` Drop plugin management entries for plugins nothing invokes — change type 27, two entries.
  - `41aecf7662` Drop the component scan of a package left behind by the studio move — change type 28.
  - `b25fced566` Remove the table property values servlet no client requests — change type 34.
  - `8b8003615c` Remove code generator helpers no template or generator calls — change type 39, four members.
- No review threads. CodeRabbit auto-paused its reviews for "an influx of new commits", and its "Docstring
  Coverage" pre-merge warning asks for additions, which a delete-only sweep never makes; ignore both.
- The SonarCloud quality gate fails on "C Reliability Rating on New Code"; every other check is green. It has
  failed on four freshly analysed heads, so it is stable, not a force-push boundary artefact. Reported in two
  comments; do not comment again unless something new appears. The check run's `output.text` is empty and the
  issue list is only on the unreachable dashboard, so a human must supply the rule key and file/line.

## Merged PRs

- #2054 and #2056 (42 deletions, types 1, 5 and 15) merged fast with no review comment: a mechanical single-type
  sweep, a locale-key batch across three bundles included, is accepted as one commit.

## Module coverage

- Nothing open: every module that builds here is swept for all 39 change types.
- `STUDIO/org.openl.rules.webstudio` has never been scanned — the one module that cannot be built here, so a
  Java deletion in it could never be compile-verified. Largest unswept area in the repository.

## Deferred findings

- `TableViewerTag` and `TableEditorTag` (tableeditor `taglib/`) — after run 8 deleted the TLD that named them,
  nothing references either class; `faces-config.xml` registers `component.UITableEditor` / `UITableViewer` for
  both tag names instead, and their 18 private fields are write-only. Deletable but `public` in a published jar.
- Five `XlsProjectionType` `CELL_*` constants (`STUDIO/org.openl.rules.diff`) — named nowhere; the enum's own
  comment asks whether they are needed. Public enum constants in a published artifact.
- `DecisionTableBuilder.methodName` (DEV `validation/properties/dimentional`) and `SimpleGroup.description`
  (`STUDIO/org.openl.security`) — private, written, never read; removing either takes a public setter with it.
- `MergeRequest`, `ResolveConflictsRequest`, `ResolveConflictsResponse` in
  `STUDIO/studio-ui/src/containers/MergeModal/types.ts` — unused in code, but `Docs/api/projects-merge-api.md`
  documents all three by name as the REST contract.
- ~70 exported types in `STUDIO/studio-ui/src` are used only inside their own file; dropping `export` is a
  refactor, so it is permanently out of scope.
- `tooltip_skin-{blue,green,red}` and `tooltip_top_{center,left}` in tableeditor `css/tooltip.css` — the
  widget's theming API, unreachable because its single caller passes none of them.
- ~190 public accessors and static helpers in `DEV/**`, `STUDIO/**` and `WSFrontend/**` have their name in exactly
  one place in the repository, their own declaration. All are published API, so none is deletable here.
- `kafka-clients` is declared only by `org.openl.rules.ruleservice.kafka`, whose two classes never touch it, but
  two other modules reach it transitively from there. Removing it needs a declaration added elsewhere.
- The module `WSFrontend/org.openl.rules.ruleservice.ws.annotation` holds only a pom: it is a published
  pom-packaged aggregator of annotation dependencies for rule service consumers, so nothing in-repo depends on it.

## False-positive shapes

- An enum constant reached through `values()` is never named anywhere, so a name scan reports it dead. It is
  usually load-bearing: `Separator.DASH` is the primary range separator, `Brackets.CURLY` a bracket pair.
- A token scan that counts the FILES containing a name hides every in-file caller, which for a private or
  package-private member is the only possible caller. Count occurrences and compare against the declaration count.
- A `provided`-scope "unused declared" dependency finding IS real when it sits in the module's own
  `<dependencies>` block. The scope heuristic only dismisses the root POM's inherited block.
- A root-pom managed entry that NO module declares is normally a deliberate transitive version pin — the comment
  beside it, or a release note, names the CVE or the provider it corrects. Only an entry whose artifact no build
  can produce, or that no consumer can reach, is dead. Seventeen of nineteen such entries are live pins.
- Keying managed entries by group and artifact alone invents duplicates: `org.openl.rules.ruleservice.ws` has
  four entries separated only by type and classifier. Include both in the key.
- A managed PLUGIN nothing declares is usually still reached: by the default lifecycle (deploy, clean, jar), by a
  packaging that binds it (`maven-archetype`), by a workflow goal (`release:prepare`), or by its own configuration
  feeding a `site/` directory. Only a plugin no lifecycle binds and no command names is dead.
- A pom `<include>` or `<exclude>` naming a path absent from git is usually a build-generated directory
  (`jetty-home/`, `logs/`, `release.properties`). Nothing in this shape has ever been dead.
- A path in a descriptor is relative or servlet-mapped, so it looks absent: `html/inputVersion.xhtml` resolves
  under `WEB-INF/taglib/`, `/faces/pages/x.xhtml` under `pages/`, and `/cxf/cxf.xml` inside a dependency jar.
- A `<component-type>` or `<renderer-type>` in a faces config is a dotted identifier, not a class name, so a
  class-existence scan reports it missing. The `<component-class>` beside it is the real class.
- PMD `UnusedAssignment` on a field initializer is wrong when the constructor can return early: on that path the
  initializer IS the value the getter returns (`CellStyle` returns early on a null argument). It also misreads
  control flow twice over — a `try`-block assignment paired with one in the `catch` is called overwritten though
  the success path reads it, and a line "overwritten" by an EARLIER line number followed a loop back-edge.
- A duplicated field assignment straddling a call is not free to remove at the line PMD names: an intervening
  call may read the field through a getter exposed to other beans. Decide which of the two writes is load-bearing.
- `UnusedLocalVariable` on an enhanced-`for` variable used only to count iterations is not deletable — the
  variable cannot be removed without rewriting the loop, which a delete-only sweep may not do.
- A private field whose only writer is a public setter is not deletable: the setter goes with it, and that is a
  public API change. The mirror case IS deletable: a field whose only reader is a dead getter goes with it.
- A private field read only by reflection is reported by PMD as unused, so an `UnusedPrivateField` hit in a test
  bean stays a false positive until the reflective reader says otherwise.
- Test sources dominate any non-public or unreferenced-member scan and are almost never dead: `AGENTS.md` requires
  JUnit 5 classes package-private and the runner discovers them (674 hits fell to 1 after filtering); a type with
  no `@Test`-family annotation is a component-scanned Spring fixture, a runner driven by an abstract base that
  owns the `@Test`, or an OpenL bean loaded by name; a private member can be the SUBJECT of an assertion
  (`assertNull(findMethod(methods, "getC"))`); and a bean under `test/org/openl/generated/` deliberately mixes
  accessor visibility (private `getAB()` beside public `setAB()`) to exercise accessor discovery — never tidy it.
- A package-private `@Component` is injected by its interface, so its own simple name appears in no other file.
- A token comparison fails in both directions unless it is exact: a regex admitting `(` swallows the paren from
  markdown `![alt](name.png)`, a substring search matches `add.png` inside `toolbar_add.png`. Require a boundary.
  In a Java signature regex, a greedy `[ \t]+` indent backtracks past a visibility keyword, so a
  `(?!public|...)` lookahead silently admits public members — reject the line by token instead.
- A file with NO extension is invisible to an extension-filtered grep, and the caller that keeps a finding alive
  can sit in exactly such a file: `Docs/_includes/nav_list` is the only place that includes `nav_auto.html`.
- A key is routinely unreachable by its own literal, in four ways: a convention suffix
  (`ValidationMessages.properties` keys, every i18next `_one` / `_other` plural pair reached by base key plus
  `count`); a template literal (`browser.compile.${state}`) — build a regex from each template in the corpus,
  a bare prefix match is too coarse; prefix composition or `$ref` indirection
  (`"ruleservice." + "jackson.typingPropertyName"`) — search the tail of the key too; and a segment starting with
  a digit (`expiration_options.7_days`), which an identifier regex anchored on a letter never matches.
- A composed lookup can still be guarded: `browser.${id}_confirm` fires only when `id === 'unlock'`, so
  `browser.delete_confirm` was dead despite matching the shape. Read the branch, not just the template.
- A markdown link is relative, so a grep for the path from the repository root misses it: `Docs/examples/index.md`
  reaches `examples/production/` as `production/README.md`. Search a doc folder by its own name, not its full path.

## Method rules

- Prove non-reference with a plain repo-wide full-text search over every text file type, not a regex scoped to
  one attribute, one file type or one module. Never pipe a proof grep through `head`, and never read a source
  from the middle: both hide the very caller that would have refuted the finding.
- Confirm every survivor of a bulk scan with an individual `grep -rIF` before deleting it — the bulk scan finds
  candidates, the individual search is the proof. For a large candidate set, extract candidate-shaped tokens from
  the whole corpus in ONE pass and set-difference: per-candidate regex over the ~16.3k text files never finishes,
  one tokenizing pass costs under a minute.
- A module that publishes nothing has no public API to protect: `maven.deploy.skip` plus `pom` packaging (as in
  `DEV/org.openl.rules.gen`) means no artifact is ever released, so its public members are internal code. Check
  `maven.deploy.skip`, the packaging and every in-repo dependency on the module before applying that.
- Deleting a Java class is provable without compiling its module: Java can name a type only by its simple name or
  its fully qualified name, so a repo-wide search for both, plus the reflective registrations that name it as a
  string, is a complete reference check. That is how the webstudio servlet shipped though the module cannot build.
- Search an accessor by its property name as well as its method name: Velocity `$w.propertyType` and JSF EL
  `#{bean.propertyType}` call `getPropertyType()` without ever spelling it.
- Search a message key by its full literal AND by its prefix up to the last dot, to catch composed lookups.
- A `#`-hash link in legacy WebStudio is a server page route, not a React route. The crossroads routes in
  `index.xhtml` build `page + ".xhtml"` from the fragment, so search a page by its base name with and without
  the extension.
- A descriptor loaded by convention is dead when its declared handler cannot satisfy the loader's contract, even
  though the class it names exists. A `.tld` tag class must implement the JSP tag interface; the tableeditor
  ones extend `UIComponentBase`, so no container could ever instantiate them. Check the contract, not the name.
  Before deleting a descriptor, confirm it declares nothing a scanner registers on sight — a TLD listener,
  function or validator entry is active even when no page uses the tags.
- A resource named by no file in the repository can still be loaded by a DEPENDENCY, by filename convention:
  CXF's `AbstractHTTPServlet` reads `/WEB-INF/cxfServletStaticResourcesMap.txt`, then `/<same name>`. Grep the
  dependency jars for the base name, then prove the value it feeds is never read — a loader is not a reader.
- Resolve every dotted reference in configuration against the repository: collect the package of each `.java`
  file, then check class names (last segment capitalized) and package names separately. Artifact ids share the
  package shape, so exclude POM files, and treat a name from a third-party package as unverifiable, not dead.
- Collect Maven dependency consumers by PARSING every pom, not grepping: `<artifactItem>` blocks of the
  dependency plugin consume a managed version exactly as `<dependency>` does, and a grep for the artifact name
  cannot tell the type and classifier apart.
- Verify a `dependencyManagement` or `pluginManagement` removal with `mvn help:effective-pom -Doutput=<file>`
  over the reactor before and after the edit. The diff must add no line and remove only the entry, once per
  effective POM. That runs in seconds and proves more than the 19-minute rebuild does.
- `mvn dependency:analyze-only` after a reactor build costs about a minute and needs no recompilation. Triage
  its output by scope AND by where the declaration sits: only the root POM's inherited `provided` / `test` block
  is a false positive. A `compile`-scope "unused declared" finding is only deletable when no dependent module
  reaches the artifact THROUGH it — grep the artifact's packages across the whole repository, and check
  `Used undeclared` for the same module, since swapping one declaration for another is an addition.
- For any PMD finding on a FIELD, grep the field name repo-wide before editing: the "never read" window is one
  method, but a field can be read by a callee, another thread, or an injected bean.
- Parse `target/pmd.xml` with the namespace `http://pmd.sourceforge.net/report/2.0.0` — the ruleset namespace
  (`ruleset/2.0.0`) and the schema name (`report_2_0_0`) both differ, and either wrong guess silently yields zero
  violations. The root POM has no `<build><plugins>` opening pair to anchor on (`<build>` starts with
  `<defaultGoal>`); insert the PMD plugin after the unique `</pluginManagement>` + `<plugins>` two-line anchor.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Removing an unused parameter of a PRIVATE method is a legitimate deletion, but it is change type 11, so it
  ships in its own commit even when the assignment that made it unused ships in the same PR.
- No war module here sets `attachClasses`, so none publishes a `classes`-classifier jar; that classifier in the
  docs belongs to rule projects built by `openl:package`.
- Two removals have maintainer precedent on `main` and need no hedging: dead CSS, and an unreachable `/action/*`
  servlet, which the 6.2.0 release notes record for `/action/launch` when its button went. A removal needs a
  release-notes entry only when a user could observe it — an internal AJAX endpoint with no button, menu or
  documented contract ships without one; a user-visible feature does not.

## Keep-list

- `org.openl.rules.tableeditor` `js/*.js` and `css/*.css` are build inputs: `HTMLRenderer` loads
  `js/tableeditor.min.js` and `css/tableeditor.min.css`, concatenated by `compile.js.sh` / `compile.css.sh`.
  Editing a source means regenerating both bundles in the same commit.
- `tableeditor.taglib.xml` and `faces-config.xml` are the live path for the two OpenL Facelets tags; only the
  `.tld` beside them was dead. Never treat the taglib or the faces config as the same finding.
- Keys under `ws.project.openapi.mode.` are reached by
  `#{msg['ws.project.openapi.mode.'.concat(project.openapi.mode.name().toLowerCase())]}` in `project.xhtml`.
- `rf-*` (RichFaces, JS inside a jar) and antd-generated class names can never be proven dead, so the 17 `rf-*`
  classes and `ant-select-input` in webstudio `common.css` stay permanently. RichFaces itself is alive:
  `org.openl.richfaces` is declared by two modules and used across webstudio.
- `openapi.properties` keys are annotation values resolved by `OpenApiPropertyResolverImpl`; all 625 are live.
- Public API in `DEV/**` and every published artifact is off limits even when unused in-repo.
- `ValidationMessages.properties` keys are looked up by a short form: the code drops the `openl.error.` prefix
  and, for exceptions, the three-digit status segment. All live; see the localized-exceptions skill.
- `onFailure` in the tableeditor scripts is a Prototype Ajax callback, invoked as `'on' + state`.
- ITEST `001-Get-Static-CSS` fixtures GET `/css/common.css` with a `***` wildcard body: they assert status and
  `Content-Type` only, so editing that file's content cannot break them.
- `serviceDescriptionInProcess` in `ServiceManagerImpl` is published to other beans through
  `@Qualifier("serviceDescriptionInProcess")` getters; its assignments are a deployment protocol, not bookkeeping.
- `META-INF/openl/extension-*.xml` files are pulled in by a wildcard `@ImportResource` in `ExtensionsConfiguration`,
  so no file names one.
- `openl-db-repository-<databaseCode>[-v<major>[.<minor>]].properties` and `-ext` are loaded by a name `Settings`
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
- `Docs/` renders through the remote theme `mmistakes/minimal-mistakes`, whose gem owns the layouts and calls the
  includes. A file under `Docs/_layouts` or `Docs/_includes` overrides a theme file of the same name, so nothing
  in this repository has to name it: `release-notes.html`, `nav_list` and `nav_auto.html` are all live.
- `DEV/org.openl.rules.gen` templates and helpers are all reachable: `GenRulesCode.run()` calls all eleven
  `generate*` methods, and every `VelocityTool` method and template variable is used by a template.

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails with `ORA-12516: ... does not have a protocol handler
  for TCP ready or registered for service freepdb1`, the Oracle TestContainer listener not yet accepting
  connections. Infrastructure, never the diff. Budget: one rerun per SHA.
- `IT (services-data)` — `RunStoreLogDataITest.setUp` cannot start `apache/kafka-native:latest`: the image
  segfaults in its own entrypoint at `Pwd.getpwuid` resolving `user.name`, exits 1, and the wait strategy times
  out on `Transitioning from RECOVERY to RUNNING`. A crash before any test body, never the diff — `ITEST - Kafka
  Smoke` passes in the same job. Budget: one rerun per SHA. The tag floats; never pin it away.
- `rerun_failed_jobs` returns 403 "This workflow is already running" while any job of the run is still in
  progress. Wait for the whole run to finish, then re-run.

## Container facts

- The build must run ONLINE. `-o` fails before the reactor starts: `main` has bumped junit, mockito, spring,
  jackson, log4j and asm past what the image's `~/.m2` holds. Maven Central is reachable; only
  `build.shibboleth.net` is 403, so the `org.opensaml:opensaml-bom` import block has to come out of the root
  `pom.xml` first (`mvn validate -N` needs it out too). Never commit that; `git checkout -- pom.xml` after.
  OpenSAML 5.2.3 and `net.shibboleth:parent` are NOT on Maven Central (404), so no repository swap fixes this.
- Ten modules need the webstudio WAR and must all be excluded, or the reactor dies on the first of them:
  `mvn install -Dquick -DnoPerf -T1C -B -pl '!:org.openl.rules.webstudio,!:itest.studio.demo,!:itest.studio.disabled-settings,!:itest.studio.acl,!:itest.studio.dtr,!:itest.studio.repos,!:itest.studio.multi,!:itest.studio.simple,!:itest.studio.users,!:itest.studio.sso'`
- That build takes about 19 minutes and installs 55 modules. The remaining ITEST modules, `ruleservice.ws.all`
  and the Maven plugin are skipped, which is expected and blocks no change type.
- `DEV/org.openl.rules.gen` has `pom` packaging, so Maven never compiles its Java and no CI job covers it. Compile
  it by hand: `mvn install -DskipTests -DnoPerf -T1C -pl :org.openl.rules -am` (about 6 minutes), then
  `mvn -pl DEV/org.openl.rules.gen dependency:build-classpath -Dmdep.outputFile=cp.txt`, then
  `javac -cp "$(cat cp.txt)" -d <tmp> $(find DEV/org.openl.rules.gen/src -name '*.java')`.
- `help:effective-pom` accepts the same `-pl` exclusions, finishes in seconds and writes all 55 effective poms
  into one 17 MB file. It is the cheapest whole-reactor verification available here.
- `dependency:analyze-only` needs the same `-pl` exclusions plus `-fae`; ITEST modules cannot resolve
  `org.openl.itest:server-core` outside the itest profile. 51 modules analyze successfully.
- `mvn pmd:pmd` needs no installed artifacts and produces a usable report even when the reactor later fails; with
  `-fae` it wrote 43 of the module reports after a downstream resolution error. It is far cheaper than the build,
  so a run short on time can scan without a green build — but a Java deletion still needs a compile.
- Error Prone contributes nothing: 8 unused-* warnings over the whole reactor, all reflection false positives or
  `EffectivelyPrivate` narrowing, a refactor and out of scope. PMD is the only Java signal.
- Frontend verification works and is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`,
  `npx eslint <files>`, and `npx vitest run` (183 files, ~3 minutes).
- `compile.js.sh` reproduces the tableeditor JS bundles byte for byte; `compile.css.sh` drops the trailing
  newline of `tableeditor.min.css`, so restore it. The `yuicompressor` jar is committed.
- `pgrep -f "mvn install"` never matches a running build — match `[c]lassworlds`. Killing the launcher shell
  leaves the Maven JVM alive, so a second launch races the first on the same `target/` dirs. `-rf` breaks
  resolution for modules built earlier in the same reactor but never installed (`org.openl.rules.test`); resume
  with a plain full build. A foreground `sleep` is blocked by the harness — wait for a background build with a
  backgrounded `until grep -qE 'BUILD SUCCESS|BUILD FAILURE' <log>; do sleep 10; done`.
- The container's global git config signs commits over ssh (`gpg.format=ssh`, `commit.gpgsign=true`), which
  fails `GitRepositoryTest` and `SameSecondHistoryOrderTest` in STUDIO Repository Git with jgit
  `UnsupportedSigningFormatException`. Fix once per session: `git config --global commit.gpgsign false`.
- The global git identity can be rewritten back to `Claude <noreply@anthropic.com>` mid-session. Re-set it and
  pass `GIT_AUTHOR_*` / `GIT_COMMITTER_*` inline on every commit; `--amend` alone keeps the wrong author,
  so it needs `--reset-author`.
- `git push origin --delete <branch>` fails through the proxy with HTTP 403; normal pushes work.
- `gh` CLI and `xxd` are absent. Use the GitHub MCP tools.
- `sonarcloud.io` is blocked by the sandbox proxy (`CONNECT tunnel failed, response 403`), and a failed
  SonarCloud check run carries only the rating — empty `output.text`, no annotations, no review comments. A
  quality-gate failure therefore cannot be diagnosed from here; say so and ask for the rule key and file/line.
- `.toDelete/` is gitignored (`.gitignore:35`) and safe for scan scratch files.
- Spotless runs from the `validate` phase on; after any build check `git status` and revert churn you did not
  intend. Runs 4-12 saw none beyond the deliberate POM edit.
- Write the ledger through `git worktree add --detach <dir> origin/dead-code/ledger`; it never touches the
  sweep branch's working tree and needs no orphan-branch dance.

## Exhausted veins

- Unreferenced images — whole repository, all 710 files, all extensions.
- Unreferenced whole `.xhtml` files (46) and whole `.js` / `.css` files (55 non-`studio-ui`).
  `STUDIO/studio-ui` has no stylesheet of any kind, so there is no React CSS vein to open.
- Unused keys in `i18n/openapi.properties` (625), webstudio `messages.properties` (46) and
  `ValidationMessages.properties`; all 1316 `studio-ui` locale keys, by leaf name and by full dotted path.
  `DEV`, `WSFrontend` and `Util` hold no message bundle at all and no `.ftl` or non-test `.xsd`.
- Class-level deadness in webstudio `css/common.css`, `layout/main.css`, `layout/simple.css`, and in all seven
  own tableeditor stylesheets (77 class tokens), plus every id selector in the same eleven files (9 tokens).
- Function and prototype-method deadness in `webapp/javascript/common.js`, `bomjs.js` and every own
  tableeditor script (119 method names).
- Unused-export scan over all 776 exports in `STUDIO/studio-ui/src`, and whole-file deadness over its 562
  source files — only tests and `.d.ts` files are unreferenced, which is expected.
- Whole-type deadness, repo-wide: all 977 non-public top-level types and all 310 types in `.impl.` / `.internal.`
  packages, plus all 193 test-source types carrying no `@Test`-family annotation. Zero real findings — every
  name-unique candidate is a framework-discovered fixture. Do not repeat these scans.
- PMD dead-code scan over the reactor except `org.openl.rules.webstudio` — 29 findings, all triaged.
- `dependency:analyze-only` over the 51 analyzable modules — every compile-scope finding is a runtime provider or
  is consumed transitively by a dependent.
- All 147 non-import root-pom `dependencyManagement` entries, keyed by group, artifact, type and classifier
  against every `<dependency>` and `<artifactItem>` in every pom. Two dead, seventeen live transitive pins.
- All 25 root-pom `pluginManagement` entries against every plugin declared outside plugin management in every
  pom, profiles and reporting sections included. Two dead, four reached without a declaration.
- All 9 Maven profile ids, all 8 servlet `param-name` entries, all 114 pom `<properties>`, all 44 `studio-ui`
  npm dependencies, all 194 `openl-default.properties` keys, and every literal include/exclude path in every
  pom. No finding in any of them.
- Every `org.openl` class and package reference in every configuration file, and all 24 component-scan base
  packages. One finding; the rest resolve or are third-party.
- Every input named by the four tableeditor bundle build scripts. All 26 exist; the only unbundled source is the
  vendored prototype library, which loads on its own.
- Whole-file deadness over every non-image, non-web resource type outside test fixtures — `.xml`, `.properties`,
  `.txt`, `.json`, `.yaml`, `.sql`, `.env`, `.csv`, `.vm`, `.tld`, `.groovy`, `.md`, plus a catch-all over every
  remaining extension. One finding; every other hit is a convention file now on the keep-list.
- All 474 package-private methods and 645 package-private fields in production sources: every name is referenced.
- All 572 enum constants in production sources, counted by occurrence — 10 candidates, every one reached through
  `values()` or a public constant of a published enum.
- Identical-content duplicates across production files — only the two `Docs` example trees, which stay.
- Class references in all 67 convention-loaded descriptors (`web.xml`, `faces-config.xml`, `*.taglib.xml`,
  `*.tld`, `META-INF/services/*`, `spring.factories`) and path references in the seven webapp descriptors. Every
  named class exists; the single finding was the TLD whose classes cannot satisfy the JSP contract.
- Every entry of the webstudio web descriptor, the repository's only `web.xml`: all six filters and both servlets
  against their mappings, all three listeners, the error page target, and every mapped URL against every client.
  One finding, the `/action/prop_values` servlet; `/action/*` now has no entry left.
- Both Facelets taglibs (3 custom tags, each used by a page), every `ui:define` name against every `ui:insert`
  (only `content` and `title` exist, both matched), and every Jekyll layout and include under `Docs/`. No finding.
- The three log4j2 configurations: every appender used, and no named logger declared at all. `compose.yaml` uses
  all five of its volumes and all five services, and the `Dockerfile` uses every stage, `ARG` and `ENV`.
- The whole of `DEV/org.openl.rules.gen`: all 12 templates against the variables the generator supplies, both
  directions, plus every public member of the eleven helper classes. Four dead members, now shipped.

## Human follow-ups

- Mirror the OpenSAML artifacts, or allowlist `build.shibboleth.net`, so `org.openl.rules.webstudio` can build
  here. It is the ONLY module this routine cannot compile and the largest one never scanned, and Maven Central
  is not an alternative — the artifacts are not published there.
- Give the sweep the SonarCloud rule key and file/line for PR #2058's new-code reliability issue, or allowlist
  `sonarcloud.io`. The quality gate is the only thing keeping that pull request from green.
- Delete the abandoned remote branch `dead-code/studio-resources` (PR #2055 closed unmerged; its only change is
  already on `main` via #2054). `git push --delete` gets HTTP 403 through the proxy and the GitHub MCP server
  has no delete-branch tool, so this needs a human or the repo's auto-delete setting.
- Decide on `TableViewerTag` / `TableEditorTag`, `DecisionTableBuilder.methodName`, `SimpleGroup.description`
  and the five `XlsProjectionType` cell constants: all are dead but public in published artifacts.
- Collapse the duplicated deployment examples: `Docs/examples/production/` and `Docs/production-deployment/` hold
  the same 32 files under two navigable paths, so every future edit has to be made twice.
- `Docs/developer-guides/rules-projects.md` tells a developer to define a validator constraint in
  `TablePropertyValidatorsWrapper.init()`, a method that does not exist; the constructor does that work.

## Run log

- Runs 10-12: thirteen new detectors (rows 27-39) produced four shipped commits on #2058 — the dead plugin pins,
  the stale component scan, the `/action/prop_values` servlet, and four members of the non-published code
  generator, compile-verified by hand since Maven never compiles that module. #2058 now holds 9 commits.
  SonarCloud has been blocked throughout, so its gate is still the only red check.
