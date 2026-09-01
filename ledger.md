# Dead-code sweep ledger — openl-tablets

## Resume point

PR #2058 is open on `dead-code/java-unused-members`, 7 commits; maintain it before anything else. Its only red
check is the SonarCloud quality gate, which cannot be diagnosed from here (see *Open PR*).
Every change type in the queue is closed. New work needs a NEW detector, not another pass of an old one.
The productive shape is contract-satisfiability: an entry loaded or resolved by convention whose declared target
cannot exist. It has now found the dead TLD, two dead managed entries, two dead plugin pins and a component scan
of a package that no longer exists — every name-based vein returns nothing.
Candidate next detectors, none tried: Spring `@Bean` methods whose type nothing injects; `id` attributes in
webstudio pages; `<exclusion>` entries for artifacts their dependency never brings; entries in the JS/CSS bundle
build scripts naming a source that is gone.
Only one sweep PR may be open: check `git ls-remote --heads origin 'dead-code/*'` before cutting a branch.

## Change-type queue

| # | Change type | Status |
|---|---|---|
| 1 | Unreferenced images | done (run 1) |
| 2 | Unreferenced whole `.xhtml` pages | done, no finding (run 1) |
| 3 | Unreferenced whole `.js` / `.css` files | done, no finding (run 1) |
| 4 | Unused `.properties` keys (STUDIO bundles) | done, no finding (runs 1-2) |
| 5 | Dead CSS rules | done for own stylesheets (runs 1, 3); vendor files stay |
| 6 | Dead JS functions (own scripts) | done, no finding (runs 2-3) |
| 7 | Never-read assignments (PMD `UnusedAssignment`) | done (run 6, 1 shipped of 9) |
| 8 | Unused local variables (PMD) | done, no deletable finding (run 5) |
| 9 | Unused private fields (PMD) | done, no deletable finding (run 6) |
| 10 | Unused private methods (PMD) | done, no finding (run 5) |
| 11 | Unused formal parameters (PMD, private only) | done (run 6, 1 shipped); webstudio unscanned |
| 12 | Unused declared Maven dependencies | done (runs 6, 8; 1 shipped) |
| 13 | Whole-type deadness in `.impl.` / internal packages | done, no finding (run 5) |
| 14 | Dead TypeScript in `STUDIO/studio-ui` | done, no finding (runs 2-3) |
| 15 | Unused `studio-ui` locale keys | done (run 3, 17 keys) |
| 16 | Orphaned resource files (config, data, descriptors) | done (runs 7-8, 2 shipped) |
| 17 | Unused pom `<properties>` | done, no finding (run 7) |
| 18 | Unused `studio-ui` npm dependencies | done, no finding (run 7) |
| 19 | Unused `openl-default.properties` keys | done, no finding (run 7) |
| 20 | Unused package-private methods and fields | done, no finding (run 7) |
| 21 | Unused enum constants | done, no deletable finding (run 8) |
| 22 | Descriptors whose declared class breaks the contract | done (run 8, 1 shipped) |
| 23 | Unused root-pom `dependencyManagement` entries | done (run 9, 2 shipped) |
| 24 | Dead Maven profiles | done, no finding (run 9) |
| 25 | Unread servlet `param-name` entries | done, no finding (run 9) |
| 26 | Dead helper types in test sources | done, no deletable finding (run 9) |
| 27 | Unused root-pom `pluginManagement` entries | done (run 10, 1 shipped) |
| 28 | Config entries whose declared target does not exist | done (run 10, 1 shipped) |
| 29 | Dead CSS id selectors | done, no finding (run 10) |

## Open PR

- #2058 on `dead-code/java-unused-members`, head `41aecf7662`, 7 commits, 6 files, 4 insertions / 287 deletions.
  - `9a074c89c3` Remove the never-read base folder normalization in MappedRepository — change type 7.
  - `f09316534a` Drop the unused base folder parameter of the project index scan — change type 11.
  - `481a3ed7cd` Delete descriptor resources that no loader can ever read — change type 16, two files.
  - `7d41eeea8c` Drop the JSP API dependency that no TableEditor source uses — change type 12.
  - `af5b8accc7` Drop dependency management entries that no module declares — change type 23, two entries.
  - `65e0bc1d41` Drop plugin management entries for plugins nothing invokes — change type 27, two entries.
  - `41aecf7662` Drop the component scan of a package left behind by the studio move — change type 28.
- No review threads. CodeRabbit's "Docstring Coverage" pre-merge warning is advisory and asks for additions,
  which a delete-only sweep never makes; ignore it, do not answer it again.
- The SonarCloud quality gate fails on "C Reliability Rating on New Code"; every other check is green. It has
  failed on two freshly analysed heads and passed on an earlier one, so it is stable, not a boundary artefact of
  the force-push. Reported in two comments; do not comment again unless something new appears. The issue list is
  only on the unreachable dashboard, so a human must supply the rule key and file/line.

## Merged PRs

- #2054 (12 lines, types 1 and 5) and #2056 (30 deletions, types 5 and 15) merged fast with no review comment:
  a mechanical single-type sweep, a locale-key batch across three bundles included, is accepted as one commit.

## Module coverage

- Nothing open. Every module that builds here is swept for all 26 change types.
- `STUDIO/org.openl.rules.webstudio` has never been scanned: it is the one module that cannot be built here, so
  a Java deletion in it could never be compile-verified. Largest unswept area in the repository.

## Deferred findings

- `TableViewerTag` and `TableEditorTag` (tableeditor `taglib/`) — after run 8 deleted the TLD that named them,
  nothing references either class; `faces-config.xml` registers `component.UITableEditor` / `UITableViewer` for
  both tag names instead, and their 18 private fields are write-only. Deletable but `public` in a published jar.
- Five `XlsProjectionType` `CELL_*` constants (`STUDIO/org.openl.rules.diff`) — named nowhere; the enum's own
  comment asks whether they are needed. Public enum constants in a published artifact.
- `DecisionTableBuilder.methodName` (DEV `validation/properties/dimentional`) and `SimpleGroup.description`
  (`STUDIO/org.openl.security`) — private, written, never read; removing either takes a public setter with it.
- 17 `rf-*` classes and `ant-select-input` in webstudio `common.css` — generated outside the repository;
  unprovable, keep permanently.
- `MergeRequest`, `ResolveConflictsRequest`, `ResolveConflictsResponse` in
  `STUDIO/studio-ui/src/containers/MergeModal/types.ts` — unused in code, but `Docs/api/projects-merge-api.md`
  documents all three by name as the REST contract.
- ~70 exported types in `STUDIO/studio-ui/src` are used only inside their own file. Dropping `export` is a
  refactor, not a deletion, so it stays out of scope permanently.
- `tooltip_skin-{blue,green,red}`, `tooltip_top_center`, `tooltip_top_left` in tableeditor `css/tooltip.css` —
  the widget's theming API; the single caller passes neither, so they are unreachable.
- `iframe.iehack` in tableeditor `css/datepicker.css` — no producer, but the file is a vendored stylesheet.
- `Docs/examples/production/` (33 files) and `Docs/production-deployment/` (32) are byte-identical apart from two
  README files, and both are navigable. Collapsing them means repointing links, which a delete-only sweep may not do.
- ~190 public accessors and static helpers in `DEV/**`, `STUDIO/**` and `WSFrontend/**` have their name in exactly
  one place in the repository, their own declaration. All are published API, so none is deletable here.
- `kafka-clients` is declared only by `org.openl.rules.ruleservice.kafka`, whose two classes never touch it, but
  `ruleservice.ws` and `ruleservice.ws.storelogdata` reach it transitively from there. Removing it needs a
  declaration added elsewhere, which a delete-only sweep may not do.
- The module `WSFrontend/org.openl.rules.ruleservice.ws.annotation` holds only a pom: it is a published
  pom-packaged aggregator of annotation dependencies for rule service consumers, so nothing in-repo depends on it.

## False-positive shapes

- An enum constant reached through `values()` is never named anywhere, so a name scan reports it as dead. It is
  usually load-bearing: `Separator.DASH` is the primary range separator, `Brackets.CURLY` a matched bracket pair.
- A token scan that counts the FILES containing a name hides every in-file caller, which for a private or
  package-private member is the only possible caller. Count occurrences and compare against the declaration count.
- A `provided`-scope "unused declared" dependency finding IS real when it sits in the module's own
  `<dependencies>` block. The scope heuristic only dismisses the root POM's inherited block.
- A root-pom managed entry that NO module declares is normally a deliberate transitive version pin — the comment
  beside it, or a release note, names the CVE or the provider it corrects. Only an entry whose artifact no build
  can produce, or that no consumer can reach, is dead. Seventeen of nineteen such entries are live pins.
- Keying managed entries by group and artifact alone invents duplicates: `org.openl.rules.ruleservice.ws` has
  four entries separated only by type and classifier. Include both in the key.
- A path in a descriptor is relative or servlet-mapped, so it looks absent: `html/inputVersion.xhtml` resolves
  under `WEB-INF/taglib/`, `/faces/pages/x.xhtml` under `pages/`, and `/cxf/cxf.xml` inside a dependency jar.
- PMD `UnusedAssignment` on a field initializer is wrong when the constructor can return early: on that path the
  initializer IS the value the getter returns. `CellStyle` returns early on a null argument.
- PMD `UnusedAssignment` misreads try/catch: for `x = f();` in a `try` with `x = null;` in the `catch`, it calls
  the try-block assignment overwritten, ignoring the success path. Every hit whose "overwritten on" line sits in
  a `catch` is a false positive.
- PMD `UnusedAssignment` also reports a line as overwritten by an EARLIER line number, which means it followed a
  loop back-edge. Read the loop before believing it.
- A duplicated field assignment straddling a call is not free to remove at the line PMD names: an intervening
  call may read the field through a getter exposed to other beans. Decide which of the two writes is load-bearing.
- `UnusedLocalVariable` on an enhanced-`for` variable used only to count iterations is not deletable — the
  variable cannot be removed without rewriting the loop, which a delete-only sweep may not do.
- A private field whose only writer is a public setter is not deletable: the setter goes with it, and that is a
  public API change.
- A non-public-type scan is dominated by JUnit 5 test classes: `AGENTS.md` requires them package-private and the
  runner discovers them, so none is ever referenced by name. Filter test sources before triaging (674 hits fell
  to 1).
- A test-source type carrying no `@Test`-family annotation is still almost never dead: it is a component-scanned
  Spring fixture, a runner driven by an abstract base that owns the `@Test`, or an OpenL bean loaded by name.
  193 such types yielded 50 name-unique candidates and zero deletions.
- A package-private `@Component` is injected by its interface, so its own simple name appears in no other file.
- A private member can be the SUBJECT of a test assertion — `assertNull(findMethod(methods, "getC"))` proves the
  private method is not exposed, so PMD reporting it uncalled is the point of the fixture.
- A fixture bean deliberately mixes accessor visibility (private `getAB()` beside public `setAB()`) to exercise
  accessor discovery. Never "tidy" a bean under `test/org/openl/generated/`.
- A private field read only by reflection is reported by PMD as unused, so an `UnusedPrivateField` hit in a test
  bean stays a false positive until the reflective reader says otherwise. A `taglib` package is NOT such a case
  any more: the TLD that bound those fields is gone, and their 18 write-only fields are real (see *Deferred*).
- A basename regex that admits `(` swallows the paren from markdown `![alt](name.png)`, so the token never
  equals the basename and every image referenced only from markdown looks dead. Strip leading punctuation.
- A plain substring search inflates hits in the other direction: `add.png` matches inside `toolbar_add.png`.
  Require a boundary or compare token sets.
- A CSS "class" may be a property value: `.gradient` came from
  `filter: progid:DXImageTransform.Microsoft.gradient(...)`, not a selector.
- A message key can be built by EL `concat` from an enum constant, so the literal key appears nowhere.
- A `.js` / `.css` source whose only hits are `compile.*.sh` / `compile.*.cmd` is a build input, not dead.
- A key suffixed by convention is never found by its literal: `ValidationMessages.properties` keys, and every
  i18next plural pair, whose `_one` / `_other` forms are reached by the base key plus a `count` option.
- A key composed in a template literal (`browser.compile.${state}`) hides every member of its group. Build a
  regex from each template in the corpus — a bare prefix match is too coarse and hides real findings.
- An identifier regex anchored on a letter never matches a key segment that starts with a digit
  (`expiration_options.7_days`), so such keys look dead in a token scan.
- A composed lookup can still be guarded: `browser.${id}_confirm` fires only when `id === 'unlock'`, so
  `browser.delete_confirm` was dead despite matching the shape. Read the branch, not just the template.
- A configuration key is reached by prefix composition (`"ruleservice." + "jackson.typingPropertyName"`) or by
  `$ref` indirection from user settings, so its full literal appears nowhere else. Search the tail of the key too.
- A markdown link is relative, so a grep for the path from the repository root misses it: `Docs/examples/index.md`
  reaches `examples/production/` as `production/README.md`. Search a doc folder by its own name, not its full path.
- In a Java signature regex, a greedy `[ \t]+` indent backtracks past a visibility keyword, so a `(?!public|...)`
  lookahead silently admits public members. Reject the line by token instead of by lookahead.
- A managed PLUGIN nothing declares is usually still reached: by the default lifecycle (deploy, clean, jar), by a
  packaging that binds it (`maven-archetype`), by a workflow goal (`release:prepare`), or by its own configuration
  feeding a `site/` directory. Only a plugin no lifecycle binds and no command names is dead.
- A `<component-type>` or `<renderer-type>` in a faces config is a dotted identifier, not a class name, so a
  class-existence scan reports it missing. The `<component-class>` beside it is the real class.
- A pom `<include>` or `<exclude>` naming a path that does not exist in git is usually a build-generated
  directory (`jetty-home/`, `logs/`, `release.properties`). Nothing in this shape has ever been dead.

## Method rules

- Prove non-reference with a plain repo-wide full-text search over every text file type, not a regex scoped to
  one attribute, one file type or one module.
- A descriptor loaded by convention is dead when its declared handler cannot satisfy the loader's contract, even
  though the class it names exists. A `.tld` tag class must implement the JSP tag interface; the tableeditor
  ones extend `UIComponentBase`, so no container could ever instantiate them. Check the contract, not the name.
- Before deleting a descriptor, confirm it declares nothing a scanner registers on sight — a TLD listener,
  function or validator entry is active even when no page uses the tags.
- Collect Maven dependency consumers by PARSING every pom, not grepping: `<artifactItem>` blocks of the
  dependency plugin consume a managed version exactly as `<dependency>` does, and a grep for the artifact name
  cannot tell the type and classifier apart.
- Verify a `dependencyManagement` or `pluginManagement` removal with `mvn help:effective-pom -Doutput=<file>`
  over the reactor before and after the edit. The diff must add no line and remove only the entry, once per
  effective POM. That runs in seconds and proves more than the 19-minute rebuild does.
- Resolve every dotted reference in configuration against the repository: collect the package of each `.java`
  file, then check class names (last segment capitalized) and package names separately. Artifact ids share the
  package shape, so exclude POM files, and treat a name from a third-party package as unverifiable, not dead.
- No war module here sets `attachClasses`, so none publishes a `classes`-classifier jar. The extra jar that
  `ruleservice.ws` attaches comes from `assembly/assembly-jar.xml` with `appendAssemblyId` false, so it has no
  classifier. The `classes` classifier in the docs belongs to rule projects built by `openl:package`.
- For a large candidate set, extract candidate-shaped tokens from the whole corpus in ONE pass and set-difference;
  per-candidate regex over ~13k files does not finish inside the time budget. The corpus is ~16.3k text files;
  one tokenizing pass over all of it costs well under a minute.
- Confirm every survivor of a bulk scan with an individual `grep -rIF` before deleting it — the bulk scan finds
  candidates, the individual search is the proof.
- For any PMD finding on a FIELD, grep the field name repo-wide before editing: the "never read" window is one
  method, but a field can be read by a callee, another thread, or an injected bean.
- Delete lines by matching their exact text, never by line number from an earlier listing and never by a
  repeated fragment: a bare `},` deletes every closing brace in the file.
- Search a message key by its full literal AND by its prefix up to the last dot, to catch composed lookups.
- Flatten `studio-ui` locale bundles by replacing `i18next.addResourceBundle(` with a capture function and
  running the file in `node:vm` — the bundles are plain object literals, so no TypeScript tooling is needed.
- Before deleting one name from a grouped CSS selector, verify each remaining name separately; keep the rule.
- A `#`-hash link in legacy WebStudio is a server page route, not a React route. The crossroads routes in
  `index.xhtml` build `page + ".xhtml"` from the fragment, so search a page by its base name with and without
  the extension.
- Dead-CSS cleanup has maintainer precedent on `main`: "Drop the common.css titleColumn rules orphaned by the
  retired commit info dialog". Same change type — no need to hedge on it.
- Parse `target/pmd.xml` with the namespace `http://pmd.sourceforge.net/report/2.0.0`. The ruleset namespace
  (`ruleset/2.0.0`) and the schema name (`report_2_0_0`) both differ from it, and either wrong guess silently
  yields zero violations from reports that are full of them.
- The root POM has no `<build><plugins>` opening pair to anchor on — `<build>` starts with `<defaultGoal>`. Insert
  the PMD plugin after the unique two-line anchor `</pluginManagement>` followed by `<plugins>`.
- Removing an unused parameter of a PRIVATE method is a legitimate deletion, but it is change type 11, so it
  ships in its own commit even when the assignment that made it unused ships in the same PR.
- A resource named by no file in the repository can still be loaded by a DEPENDENCY, by filename convention:
  CXF's `AbstractHTTPServlet` reads `/WEB-INF/cxfServletStaticResourcesMap.txt`, then `/<same name>`. Before
  deleting a config-shaped resource, grep the dependency jars for its base name, then prove the value it feeds is
  never read — the loader existing is not the same as the value being used.
- `ruleservice.ws` serves `resources/static/**` from `RuleServicesFilter`, typed by `ServletContext::getMimeType`.
  Its `CXFServlet` sets no static-resources list, welcome file or redirect list, so CXF's own static-content path
  is unreachable there.
- `mvn dependency:analyze-only` after a reactor build costs about a minute and needs no recompilation. Triage
  its output by scope AND by where the declaration sits: only the root POM's inherited `provided` / `test` block
  is a false positive.
- A `compile`-scope "unused declared" finding is only deletable when no dependent module reaches the artifact
  THROUGH it. Grep the artifact's packages across the whole repository, not just the declaring module, and check
  `Used undeclared` for the same module — a swap of one declaration for another is an addition, not a deletion.

## Keep-list

- `org.openl.rules.tableeditor` `js/*.js` and `css/*.css` are build inputs: `HTMLRenderer` loads
  `js/tableeditor.min.js` and `css/tableeditor.min.css`, concatenated by `compile.js.sh` / `compile.css.sh`.
  Editing a source means regenerating both bundles in the same commit.
- `tableeditor.taglib.xml` and `faces-config.xml` are the live path for the two OpenL Facelets tags; only the
  `.tld` beside them was dead. Never treat the taglib or the faces config as the same finding.
- Keys under `ws.project.openapi.mode.` are reached by
  `#{msg['ws.project.openapi.mode.'.concat(project.openapi.mode.name().toLowerCase())]}` in `project.xhtml`.
- `rf-*` (RichFaces, JS inside a jar) and antd-generated class names can never be proven dead. RichFaces itself
  is alive: `org.openl.richfaces` is declared by two modules and used across webstudio.
- `openapi.properties` keys are annotation values resolved by `OpenApiPropertyResolverImpl`; all 625 are live.
- Public API in `DEV/**` and every published artifact is off limits even when unused in-repo.
- `ValidationMessages.properties` keys are looked up by a short form: the code drops the `openl.error.`
  prefix and, for exceptions, the three-digit status segment. All are live. See
  `.claude/skills/localized-exceptions-and-validation-skill/SKILL.md`.
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
  `build.shibboleth.net` is 403, so the `org.opensaml:opensaml-bom` import block still has to come out of the
  root `pom.xml` first. Never commit that; `git checkout -- pom.xml` after. `mvn validate -N` needs it out too.
- Ten modules need the webstudio WAR and must all be excluded, or the reactor dies on the first of them:
  `mvn install -Dquick -DnoPerf -T1C -B -pl '!:org.openl.rules.webstudio,!:itest.studio.demo,!:itest.studio.disabled-settings,!:itest.studio.acl,!:itest.studio.dtr,!:itest.studio.repos,!:itest.studio.multi,!:itest.studio.simple,!:itest.studio.users,!:itest.studio.sso'`
- That build takes about 19 minutes including downloads and installs 55 modules, TableEditor and STUDIO Web
  components among them. The remaining ITEST modules, `ruleservice.ws.all` and the Maven plugin are skipped,
  which is expected and does not block any change type.
- `help:effective-pom` accepts the same `-pl` exclusions, finishes in seconds and writes all 55 effective poms
  into one 17 MB file. It is the cheapest whole-reactor verification available here.
- `pgrep -f "mvn install"` never matches a running build — `mvn` execs java through plexus classworlds; match
  `[c]lassworlds` instead, and check it before every launch. Killing the launcher shell leaves the Maven JVM
  alive, so a second launch races the first on the same `target/` dirs. Never `pkill -f <pattern>` where the
  pattern also matches your own shell — it kills the tool call.
- `dependency:analyze-only` needs the same `-pl` exclusions plus `-fae`; ITEST modules cannot resolve
  `org.openl.itest:server-core` outside the itest profile. 51 modules analyze successfully.
- `mvn pmd:pmd` needs no installed artifacts and produces a usable report even when the reactor later fails; with
  `-fae` it wrote 43 of the module reports after a downstream resolution error. It is far cheaper than the build,
  so a run short on time can scan without a green build — but a Java deletion still needs a compile.
- The container's global git config signs commits over ssh (`gpg.format=ssh`, `commit.gpgsign=true`), which
  fails `GitRepositoryTest` and `SameSecondHistoryOrderTest` in STUDIO Repository Git with jgit
  `UnsupportedSigningFormatException`. Fix once per session: `git config --global commit.gpgsign false`.
- `-rf` breaks resolution for modules built earlier in the same reactor but never installed
  (`org.openl.rules.test`). Resume with a plain full build, not with `-rf`.
- Error Prone contributes nothing: the whole reactor emits 8 unused-* warnings, all reflection false positives
  or `EffectivelyPrivate` visibility narrowing, which is a refactor and out of scope. PMD is the only Java signal.
- Frontend verification works and is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`,
  `npx eslint <files>`, and `npx vitest run` (183 files, ~3 minutes).
- `compile.js.sh` reproduces `tableeditor.all.js` and `.min.js` byte for byte; `compile.css.sh` drops the
  trailing newline of `tableeditor.min.css` and joins two sources without one, so restore the newline and
  expect a one-line comment shift in `tableeditor.all.css`. The `yuicompressor` jar is committed.
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
  intend. Runs 4-9 saw none beyond the deliberate POM edit.
- Write the ledger through `git worktree add --detach <dir> origin/dead-code/ledger`; it never touches the
  sweep branch's working tree and needs no orphan-branch dance.

## Exhausted veins

- Unreferenced images — whole repository, all 710 files, all extensions.
- Unreferenced whole `.xhtml` files (46) and whole `.js` / `.css` files (55 non-`studio-ui`).
  `STUDIO/studio-ui` has no stylesheet of any kind, so there is no React CSS vein to open.
- Unused keys in `i18n/openapi.properties` (625), webstudio `messages.properties` (46) and
  `ValidationMessages.properties`.
- Class-level deadness in webstudio `css/common.css`, `layout/main.css`, `layout/simple.css`, and in all seven
  own tableeditor stylesheets (77 class tokens), plus every id selector in the same eleven files (9 tokens).
- Function and prototype-method deadness in `webapp/javascript/common.js`, `bomjs.js` and every own
  tableeditor script (119 method names).
- Unused-export scan over all 776 exports in `STUDIO/studio-ui/src`, and whole-file deadness over its 562
  source files — only tests and `.d.ts` files are unreferenced, which is expected.
- All 1316 `studio-ui` locale keys, by leaf name and by full dotted path.
- Message bundles and templates outside STUDIO: `DEV`, `WSFrontend` and `Util` hold no message bundle at all
  and no `.ftl` or non-test `.xsd`.
- Whole-type deadness, repo-wide: all 977 non-public top-level types and all 310 types in `.impl.` / `.internal.`
  packages. Zero real findings — do not repeat this scan.
- All 193 test-source types that carry no `@Test`-family annotation, by simple-name occurrence across the whole
  corpus. Zero deletions; every name-unique candidate is a framework-discovered fixture.
- PMD dead-code scan over the reactor except `org.openl.rules.webstudio` — 29 findings, all triaged.
- `dependency:analyze-only` over the 51 analyzable modules — every compile-scope finding is a runtime provider or
  is consumed transitively by a dependent.
- All 147 non-import root-pom `dependencyManagement` entries, keyed by group, artifact, type and classifier
  against every `<dependency>` and `<artifactItem>` in every pom. Two dead, seventeen live transitive pins.
- All 9 Maven profile ids, all 8 servlet `param-name` entries, and every literal include/exclude path in every
  pom. No finding in any of the three.
- All 25 root-pom `pluginManagement` entries against every plugin declared outside plugin management in every
  pom, profiles and reporting sections included. Two dead, four reached without a declaration.
- Every `org.openl` class and package reference in every configuration file, and all 24 component-scan base
  packages. One finding; the rest resolve or are third-party.
- Whole-file deadness over every non-image, non-web resource type outside test fixtures — `.xml`, `.properties`,
  `.txt`, `.json`, `.yaml`, `.sql`, `.env`, `.csv`, `.vm`, `.tld`, `.groovy`, `.md`, plus a catch-all over every
  remaining extension. One finding; every other hit is a convention file now on the keep-list.
- All 114 pom `<properties>`, all 44 `studio-ui` npm dependencies and all 194 `openl-default.properties` keys.
- All 474 package-private methods and 645 package-private fields in production sources: every name is referenced.
- Identical-content duplicates across production files — only the two `Docs` example trees, which stay.
- All 572 enum constants in production sources, counted by occurrence — 10 candidates, every one reached through
  `values()` or a public constant of a published enum.
- Class references in all 67 convention-loaded descriptors (`web.xml`, `faces-config.xml`, `*.taglib.xml`,
  `*.tld`, `META-INF/services/*`, `spring.factories`) and path references in the seven webapp descriptors. Every
  named class exists; the single finding was the TLD whose classes cannot satisfy the JSP contract.

## Human follow-ups

- Allowlist `build.shibboleth.net`, or mirror the opensaml artifacts, so `org.openl.rules.webstudio` can build
  here. It is now the ONLY module this routine cannot compile, and the largest one never scanned.
- Give the sweep the SonarCloud rule key and file/line for PR #2058's new-code reliability issue, or allowlist
  `sonarcloud.io`. The quality gate is the only thing keeping that pull request from green.
- Delete the abandoned remote branch `dead-code/studio-resources` (PR #2055 closed unmerged; its only change is
  already on `main` via #2054). `git push --delete` gets HTTP 403 through the proxy and the GitHub MCP server
  has no delete-branch tool, so this needs a human or the repo's auto-delete setting.
- Decide on `TableViewerTag` / `TableEditorTag`, `DecisionTableBuilder.methodName`, `SimpleGroup.description`
  and the five `XlsProjectionType` cell constants: all are dead but public in published artifacts.
- Collapse the duplicated deployment examples: `Docs/examples/production/` and `Docs/production-deployment/` hold
  the same 32 files under two navigable paths, so every future edit has to be made twice.

## Run log

- Run 8: two new detectors (rows 21-22). The contract-satisfiability scan found the TableEditor JSP TLD, dead
  since the Faces 4 migration; it and the JSP API dependency it justified shipped onto #2058, now 4 commits.
- Run 9: four new detectors (rows 23-26). Row 23 found two dead managed entries, verified by an effective-pom
  diff over the whole reactor, and shipped onto #2058, now 5 commits. SonarCloud still unreachable.
- Run 10: three new detectors (rows 27-29). Two findings, both shipped onto #2058, now 7 commits: the dead plugin
  pins and the stale component scan. SonarCloud is still blocked, so the gate is still the only red check.
