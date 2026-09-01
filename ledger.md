# Dead-code sweep ledger — openl-tablets

## Resume point

SESSIONS RUN CONCURRENTLY: the cron spawns a FRESH session every two hours, so several sweeps share this branch,
ledger and PR at once — two runs pushed commits and ledger updates past each other today. Never arm a
self-perpetuating check-in chain; the next firing already covers the PR and the chains accumulate forever.
Re-fetch before every write, never force-push, and treat a CI event for a superseded `head_sha` as stale.
PR #2058 is open on `dead-code/java-unused-members`, 11 commits; maintain it before anything else. Its only red
check is the SonarCloud quality gate, which cannot be diagnosed from here (see *Open PR*). Keep pushing verified
work to it anyway: only one sweep PR may be open, so holding findings back ships nothing.
All 48 change types are closed; new work needs a NEW detector. Member-level deadness is finished in every
admissible scope — published API is off limits, non-published modules are unprovable (`.xls*` callers), and
internal packages hold nothing. Maven metadata still pays: type 48 found three dead exclusions.
Candidate next detectors, none tried: `openl-maven-plugin` mojo parameters no goal reads; Spring bean ids
nothing names; `<resource>`/`<testResource>` directories no module ships; duplicate CSS rules inside one file.
Check `git ls-remote --heads origin 'dead-code/*'` before cutting a branch.

## Change-type queue

All 48 closed; *Exhausted veins* records what each covered. Fifteen shipped a deletion — 1, 5, 7, 11, 12, 15,
16, 22, 23, 27, 28, 34, 39, 42, 48 — the other thirty-three found nothing. Numbering continues at 49.

## Open PR

- #2058 on `dead-code/java-unused-members`, 11 commits, head `60c15c0e19`. Derive counts mechanically before
  editing the body: `git log --oneline origin/main..HEAD | wc -l` and `git diff --shortstat origin/main...HEAD`.
  - `9a074c89c3` never-read base folder normalization in MappedRepository — type 7.
  - `f09316534a` unused base folder parameter of the project index scan — type 11.
  - `481a3ed7cd` descriptor resources no loader can read — type 16, two files.
  - `7d41eeea8c` JSP API dependency no TableEditor source uses — type 12.
  - `af5b8accc7` dependency management entries no module declares — type 23, two entries.
  - `65e0bc1d41` plugin management entries for plugins nothing invokes — type 27, two entries.
  - `41aecf7662` component scan of a package left behind by the studio move — type 28.
  - `b25fced566` table property values servlet no client requests — type 34.
  - `8b8003615c` code generator helpers no template calls — type 39, four members.
  - `63b66a5957` local declaration the tracing span check never uses — type 42.
  - `60c15c0e19` three Azure blob exclusions of artifacts it no longer brings — type 48.
- No review threads. CodeRabbit auto-paused its reviews for "an influx of new commits", and its "Docstring
  Coverage" pre-merge warning asks for additions, which a delete-only sweep never makes; ignore both.
- The SonarCloud quality gate fails on "C Reliability Rating on New Code"; every other check is green. It failed
  on four freshly analysed heads, so it is stable, not a force-push artefact. Reported in two comments; do not
  comment again. The issue list is only on the unreachable dashboard, so a human must supply the rule and line.

## Merged PRs

- #2054 and #2056 (42 deletions, types 1, 5, 15) merged with no review comment, a locale-key batch included.

## Module coverage

- Nothing open: every module is swept for all 48 change types, main and test sources, webstudio included. Only
  ITEST fixtures stay out of scope.

## Deferred findings

- `TableViewerTag` and `TableEditorTag` (tableeditor `taglib/`) — unreferenced since run 8 deleted their TLD;
  `faces-config.xml` registers component types for both tag names. Deletable but `public` in a published jar.
- Five `XlsProjectionType` `CELL_*` constants (`STUDIO/org.openl.rules.diff`) — named nowhere; the enum's own
  comment asks whether they are needed. Public enum constants in a published artifact.
- `DecisionTableBuilder.methodName` (DEV `validation/properties/dimentional`) and `SimpleGroup.description`
  (`STUDIO/org.openl.security`) — private, written, never read; removing either takes a public setter with it.
- `MergeRequest`, `ResolveConflictsRequest` and `ResolveConflictsResponse` (`studio-ui`
  `containers/MergeModal/types.ts`) — unused in code, but `Docs/api/projects-merge-api.md` documents all three.
- ~70 exported types in `studio-ui` are used only inside their own file; dropping `export` is a refactor.
- `tooltip_skin-{blue,green,red}` and `tooltip_top_{center,left}` in tableeditor `css/tooltip.css` — the
  widget's theming API, unreachable because its single caller passes none of them.
- ~190 public accessors in `DEV/**`/`STUDIO/**`/`WSFrontend/**` are named only at their declaration; published API.
- `kafka-clients` is declared only by `org.openl.rules.ruleservice.kafka`, whose two classes never touch it, but
  two other modules reach it transitively from there. Removing it needs a declaration added elsewhere.
- The module `WSFrontend/org.openl.rules.ruleservice.ws.annotation` holds only a pom: it is a published
  pom-packaged aggregator of annotation dependencies for rule service consumers, so nothing in-repo depends on it.
- `MergeResult.status` is a record component its compact constructor always overwrites, so the `@Builder` takes a
  value no caller can influence. Removing a record component is a public API change.
- `npm run clean` is the one `studio-ui` script nothing names, but an npm script is a human entry point.

## False-positive shapes

- An enum constant reached through `values()` is never named anywhere, so a name scan reports it dead. It is
  usually load-bearing: `Separator.DASH` is the primary range separator, `Brackets.CURLY` a bracket pair.
- A token scan that counts the FILES containing a name hides every in-file caller, which for a private or
  package-private member is the only possible caller. Count occurrences and compare against the declaration count.
- A text-file token scan cannot see an `.xls*` rule workbook, and that is exactly where OpenL names a Java bean's
  property or a rule helper method. Every accessor candidate in `DEV/org.openl.rules.test` and the ITEST rule
  projects is therefore unprovable, not dead.
- A framework calls an accessor without naming it, in four ways worth checking before believing a hit: a JPA
  `@Entity` accessor (Hibernate), a `@Bean` factory method (Spring), a setter for injection (`setEntityManager`),
  and an interface method whose implementation carries no `@Override` (`StompSessionHandler.handleFrame`).
- A reactor `dependency:tree` cannot tell a live `<exclusion>` from a dead one: when the excluded artifact
  already wins from another path, the node the exclusion suppresses is omitted as a duplicate and never printed.
  Resolve the dependency ALONE, in a scratch project with no dependency management, under `-Dverbose`.
- A `provided`-scope "unused declared" dependency finding IS real when it sits in the module's own
  `<dependencies>` block. The scope heuristic only dismisses the root POM's inherited block.
- A root-pom managed entry that NO module declares is normally a deliberate transitive version pin — the comment
  beside it, or a release note, names the CVE or the provider it corrects. Only an entry whose artifact no build
  can produce, or that no consumer can reach, is dead. Seventeen of nineteen such entries are live pins.
- A managed PLUGIN nothing declares is usually still reached: by the default lifecycle, by a packaging that binds
  it (`maven-archetype`), by a workflow goal (`release:prepare`), or by its own `site/` configuration.
- A pom `<include>` or `<exclude>` naming a path absent from git is usually a build-generated directory
  (`jetty-home/`, `logs/`, `release.properties`). Nothing in this shape has ever been dead.
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
  bean stays a false positive until the reflective reader says otherwise — `JsonUtilsTest.BindingClasses` fields
  are the Jackson binding surface the test passes to `getCachedObjectMapper`.
- Test sources dominate any unreferenced-member scan and are almost never dead: JUnit 5 classes are required
  package-private and the runner discovers them (674 hits fell to 1); a type with no `@Test` is a Spring fixture,
  an abstract base's runner or an OpenL bean loaded by name; a private member can be an assertion's SUBJECT; a
  bean under `test/org/openl/generated/` mixes accessor visibility on purpose. Never tidy any of it.
- A package-private `@Component` is injected by its interface, so its own simple name appears in no other file.
- A token comparison fails in both directions unless it is exact: a regex admitting `(` swallows the paren from
  markdown `![alt](name.png)`, a substring search matches `add.png` inside `toolbar_add.png`, and a greedy
  indent in a Java signature regex backtracks past a visibility keyword so a lookahead admits public members.
  Require a boundary, and reject a line by token rather than by lookahead.
- A file with NO extension is invisible to an extension-filtered grep, and the caller that keeps a finding alive
  can sit in exactly such a file: `Docs/_includes/nav_list` is the only place that includes `nav_auto.html`.
- A key is routinely unreachable by its own literal: through a convention suffix (an i18next `_one`/`_other`
  plural pair reached by base key plus `count`), a template literal, prefix composition or `$ref` indirection
  (search the tail of the key too), or a segment starting with a digit, which a letter-anchored regex misses.
  Build a regex from each template in the corpus, never a bare prefix match — but read the branch as well, since
  a composed lookup can be guarded (`browser.${id}_confirm` fires only when `id === 'unlock'`).
- A duplicate-key scan over `.properties` must join backslash continuation lines first: a multi-line value that
  embeds JSON or Markdown otherwise reports its own `"params"` and `**Note` lines as repeated keys.

## Method rules

- Never pipe a proof grep through `head`, and never read a source from the middle: both hide the very caller that
  would have refuted the finding.
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
  that is how the webstudio servlet shipped though the module cannot build. The same holds for a local or a
  private member, whose scope is one file; CI's `Build artifacts` job is then the compile gate.
- Search an accessor by its property name as well as its method name: Velocity `$w.propertyType` and JSF EL
  `#{bean.propertyType}` call `getPropertyType()` without ever spelling it.
- Search a message key by its full literal AND by its prefix up to the last dot, to catch composed lookups.
- A `#`-hash link in legacy WebStudio is a server page route, not a React route. The crossroads routes in
  `index.xhtml` build `page + ".xhtml"` from the fragment, so search a page by its base name with and without
  the extension.
- A descriptor loaded by convention is dead when its declared handler cannot satisfy the loader's contract, even
  though the class it names exists: a `.tld` tag class must implement the JSP tag interface, and the tableeditor
  ones extend `UIComponentBase`. Check the contract, not the name — but first confirm the descriptor declares
  nothing a scanner registers on sight, since a TLD listener or function entry is active with no page at all.
- A resource named by no file in the repository can still be loaded by a DEPENDENCY, by filename convention:
  CXF's `AbstractHTTPServlet` reads `/WEB-INF/cxfServletStaticResourcesMap.txt`, then `/<same name>`. Grep the
  dependency jars for the base name, then prove the value it feeds is never read — a loader is not a reader.
- Resolve every dotted reference in configuration against the repository: collect each `.java` file's package,
  then check class names (last segment capitalized) and package names separately. Artifact ids share the package
  shape, so exclude POMs, and treat a third-party name as unverifiable, not dead.
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
- Removing an unused parameter of a PRIVATE method is a legitimate deletion, but it is change type 11, so it
  ships in its own commit even when the assignment that made it unused ships in the same PR.
- Dead CSS and an unreachable `/action/*` servlet both have maintainer precedent on `main` and need no hedging.
  A removal needs a release-notes entry only when a user could observe it — an internal AJAX endpoint with no
  button, menu or documented contract ships without one; a user-visible feature does not.

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
- ITEST `001-Get-Static-CSS` asserts only the status and `Content-Type` of `/css/common.css`, never its body.
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
- `Docs/` renders through the remote theme `mmistakes/minimal-mistakes`, whose gem owns the layouts. A file
  under `Docs/_layouts` or `Docs/_includes` overrides a theme file of the same name, so nothing here has to name
  it: `release-notes.html`, `nav_list` and `nav_auto.html` are all live.
- `DEV/org.openl.rules.gen` templates and helpers are all reachable: `GenRulesCode.run()` calls all eleven
  `generate*` methods, and every `VelocityTool` method and template variable is used by a template.
- `archetype-resources/pom.xml` is processed by the archetype plugin itself, not by a `<fileSet>`, so no fileset
  has to name it. The two empty `assembly/*.xml` files under `openl-maven-plugin/it/openl-multiproject` are
  fixtures `verify.groovy` asserts are EXCLUDED from the built artifact.
- `DEV/org.openl.commons/test-resources/specs.properties` declares `hello` and `duplicateKey` twice on purpose:
  it is the fixture for the properties-spec parser. Never dedupe it.

## CI flakes

- `IT (studio-acl)` — `OracleRdbmsTest.upgrade` fails with `ORA-12516: ... does not have a protocol handler
  for TCP ready or registered for service freepdb1`, the Oracle TestContainer listener not yet accepting
  connections. Infrastructure, never the diff. Budget: one rerun per SHA.
- `IT (services-data)` — `RunStoreLogDataITest.setUp` cannot start `apache/kafka-native:latest`: the image
  segfaults in its own entrypoint at `Pwd.getpwuid` resolving `user.name`, exits 1, and the wait strategy times
  out on `Transitioning from RECOVERY to RUNNING`. A crash before any test body, never the diff — `ITEST - Kafka
  Smoke` passes in the same job. Budget: one rerun per SHA. The tag floats; never pin it away.
- `Sonar analysis` — `jacoco:report-aggregate` fails with "Unknown block type c7", a malformed `.exec` from the
  overlapping `coverage-*` artifacts the job merges. Transient: it passed on re-run. Distinct from the gate, and
  it suppresses the gate entirely because nothing is uploaded. Budget: one rerun per SHA.
- `rerun_failed_jobs` returns 403 "This workflow is already running" while any job of the run is still in
  progress. Wait for the whole run to finish, then re-run.

## Container facts

- The build must run ONLINE: `-o` fails before the reactor starts, because `main` keeps bumping dependencies
  past what the image's `~/.m2` holds. Maven Central is reachable; only `build.shibboleth.net` is 403, so the
  `org.opensaml:opensaml-bom` import block must come out of the root `pom.xml` first (`mvn validate -N` needs it
  out too) and go back in with `git checkout -- pom.xml`. OpenSAML is not on Central, so no repository swap
  fixes this.
- Ten modules need the webstudio WAR and must all be excluded, or the reactor dies on the first of them:
  `mvn install -Dquick -DnoPerf -T1C -B -pl '!:org.openl.rules.webstudio,!:itest.studio.demo,!:itest.studio.disabled-settings,!:itest.studio.acl,!:itest.studio.dtr,!:itest.studio.repos,!:itest.studio.multi,!:itest.studio.simple,!:itest.studio.users,!:itest.studio.sso'`
- That build takes about 20 minutes and installs 55 modules. The remaining ITEST modules, `ruleservice.ws.all`
  and the Maven plugin are skipped, which is expected and blocks no change type.
- One ITEST suite CAN be built here, and needs the `install` lifecycle: `mvn install -Pitest -Dquick -DnoPerf
  -T1C -B -pl ITEST/<suite> -am` builds a 28-module reactor. `test-compile` is too early a phase — the suite's
  `unpack-dependencies` of the webapp fails with MDEP-98 before any test source is compiled.
- PMD runs standalone, with no Maven and no auxclasspath, and is the ONLY way to scan
  `org.openl.rules.webstudio`: fetch `net.sourceforge.pmd:pmd-cli` and `pmd-java` with a scratch pom plus
  `dependency:copy-dependencies`, then `java -cp '.toDelete/pmd/lib/*' net.sourceforge.pmd.cli.PmdCli check
  --file-list <list> -R <ruleset> -f xml -r <out> --no-cache -t 4`. All 4031 files scan in a minute. Prefer it
  over the maven-pmd-plugin route, which needs the root-pom edit, misses webstudio and skips test sources.
- `help:effective-pom` accepts the same `-pl` exclusions, finishes in seconds and writes all 55 effective poms
  into one 17 MB file. It is the cheapest whole-reactor verification available here.
- `dependency:analyze-only` needs the same `-pl` exclusions plus `-fae`; ITEST modules cannot resolve
  `org.openl.itest:server-core` outside the itest profile. 51 modules analyze successfully.
- Error Prone contributes nothing: 8 unused-* warnings over the whole reactor, all reflection false positives or
  `EffectivelyPrivate` narrowing, a refactor and out of scope. PMD is the only Java signal.
- Frontend verification works and is the gate for `studio-ui`: `npm ci`, `npx tsc --noEmit`,
  `npx eslint <files>`, and `npx vitest run` (183 files, ~3 minutes).
- `compile.js.sh` reproduces the tableeditor JS bundles byte for byte; `compile.css.sh` drops the trailing
  newline of `tableeditor.min.css`, so restore it. The `yuicompressor` jar is committed.
- `nohup mvn ... &` returns instantly and the harness calls the launcher "completed" while Maven still runs, so
  never read a build result from that exit code; match `[c]lassworlds`, not `mvn install`, to see it alive.
  Killing the launcher leaves the JVM running, so a second launch races the first on the same `target/` dirs.
  `-rf` breaks resolution for modules built earlier but never installed (`org.openl.rules.test`). A foreground
  `sleep` is blocked — wait with a backgrounded `until grep -qE 'BUILD (SUCCESS|FAILURE)' <log>; do sleep 30; done`.
- The container's global git config signs commits over ssh (`gpg.format=ssh`, `commit.gpgsign=true`), which
  fails `GitRepositoryTest` and `SameSecondHistoryOrderTest` in STUDIO Repository Git with jgit
  `UnsupportedSigningFormatException`. Fix once per session: `git config --global commit.gpgsign false`.
- The global git identity can be rewritten back to `Claude <noreply@anthropic.com>` mid-session. Re-set it and
  pass `GIT_AUTHOR_*` / `GIT_COMMITTER_*` inline on every commit; `--amend` alone keeps the wrong author,
  so it needs `--reset-author`.
- `git push origin --delete <branch>` fails through the proxy with HTTP 403; normal pushes work.
- `gh` CLI and `xxd` are absent. Use the GitHub MCP tools. A body sent through them loses angle-bracketed text:
  a `maven.deploy.skip` XML element written as a tag pair was stored as a bare `true`, silently gutting the
  evidence it carried. Name an XML element in prose with backticks, never as a tag.
- `sonarcloud.io` is blocked by the sandbox proxy (`CONNECT tunnel failed, response 403`), and a failed
  SonarCloud check run carries only the rating — empty `output.text`, no annotations, no review comments. A
  quality-gate failure therefore cannot be diagnosed from here; say so and ask for the rule key and file/line.
- `.toDelete/` is gitignored (`.gitignore:35`) and safe for scan scratch files.
- Spotless runs from the `validate` phase on; after any build check `git status` and revert churn you did not
  intend. Runs 4-15 saw none beyond the deliberate POM edit.
- Write the ledger through `git worktree add --detach <dir> origin/dead-code/ledger`; it never touches the
  sweep branch's working tree and needs no orphan-branch dance.

## Exhausted veins

- PMD dead-code rules (`UnusedPrivateField`, `UnusedPrivateMethod`, `UnusedLocalVariable`, `UnusedAssignment`,
  `UnusedFormalParameter`) over ALL 4031 Java files — main and test sources, every module,
  `org.openl.rules.webstudio` included. 50 violations, one real. Java holds nothing else of this shape; do not
  repeat this scan, and never add `UnnecessaryImport`, which Spotless already owns.
- Public members of the non-published modules (`DEV/org.openl.rules.test`, `ITEST/**`, 133 files): 52 candidates,
  every one framework-driven — a JPA accessor, a Spring `@Bean`, an interface override, or an OpenL bean property
  named only from a binary workbook. Unprovable by construction; do not repeat.
- Public members of `.impl.` / `.internal.` production packages: 1514 declarations, 595 distinct names over 306
  files, every name referenced elsewhere. Zero findings.
- Unreferenced images — whole repository, all 710 files, all extensions.
- Unreferenced whole `.xhtml` files (46) and whole `.js` / `.css` files (55 non-`studio-ui`).
  `STUDIO/studio-ui` has no stylesheet of any kind, so there is no React CSS vein to open.
- Unused keys in `i18n/openapi.properties` (625), webstudio `messages.properties` (46) and
  `ValidationMessages.properties`, and all 1316 `studio-ui` locale keys by leaf name and full dotted path. `DEV`,
  `WSFrontend` and `Util` hold no bundle at all and no `.ftl` or non-test `.xsd`.
- Duplicate declarations inside one file: keys in all 118 `.properties` (continuation-aware) and every `.json`,
  and `<dependency>`, `<exclusion>`, `<plugin>`, `<module>` and `<properties>` children in all 208 poms. The one
  hit is the deliberate `specs.properties` fixture.
- The nine `STUDIO/studio-ui` npm scripts (only `clean` is unnamed), and identical-content duplicates across
  production files (only the two `Docs` example trees, which stay).
- Class-level deadness in webstudio `css/common.css`, `layout/main.css`, `layout/simple.css`, and in all seven
  own tableeditor stylesheets (77 class tokens), plus every id selector in the same eleven files (9 tokens).
- Function and prototype-method deadness in `webapp/javascript/common.js`, `bomjs.js` and every own
  tableeditor script (119 method names).
- Unused-export scan over all 776 exports in `STUDIO/studio-ui/src`, and whole-file deadness over its 562
  source files — only tests and `.d.ts` files are unreferenced, which is expected.
- Whole-type deadness, repo-wide: all 977 non-public top-level types and all 310 types in `.impl.` / `.internal.`
  packages, plus all 193 test-source types carrying no `@Test`-family annotation. Zero real findings — every
  name-unique candidate is a framework-discovered fixture. Do not repeat these scans.
- `dependency:analyze-only` over the 51 analyzable modules — every compile-scope finding is a runtime provider or
  is consumed transitively by a dependent.
- All 18 `<exclusion>` entries in every pom, each resolved in isolation under `-Dverbose`: three dead on
  `azure-storage-blob` and shipped, eleven suppress a real node, one (`poi-ooxml-lite`) is deliberately replaced
  by `poi-ooxml-full`, and three belong to `openl-maven-plugin` `it/` fixtures and are out of scope.
- All 147 non-import root-pom `dependencyManagement` entries, keyed by group, artifact, type and classifier
  against every `<dependency>` and `<artifactItem>` in every pom: two dead, seventeen live transitive pins. All
  25 `pluginManagement` entries against every plugin declared anywhere, profiles and reporting included: two
  dead, four reached without a declaration.
- All 9 Maven profile ids, all 8 servlet `param-name` entries, all 114 pom `<properties>`, all 44 `studio-ui`
  npm dependencies, all 194 `openl-default.properties` keys, and every literal include/exclude path in every
  pom. No finding in any of them.
- Every `org.openl` class and package reference in every configuration file, and all 24 component-scan base
  packages. One finding; the rest resolve or are third-party.
- Every input named by the four tableeditor bundle build scripts: all 26 exist, and the only unbundled source is
  the vendored prototype library, which loads on its own.
- Whole-file deadness over every non-image, non-web resource type outside test fixtures — `.xml`, `.properties`,
  `.txt`, `.json`, `.yaml`, `.sql`, `.env`, `.csv`, `.vm`, `.tld`, `.groovy`, `.md` and a catch-all over the
  rest. One finding; every other hit is a convention file now on the keep-list.
- All 474 package-private methods and 645 package-private fields in production sources: every name is referenced.
- All 572 enum constants in production sources, counted by occurrence — 10 candidates, every one reached through
  `values()` or a public constant of a published enum.
- Class references in all 67 convention-loaded descriptors (`web.xml`, `faces-config.xml`, `*.taglib.xml`,
  `*.tld`, `META-INF/services/*`, `spring.factories`) and path references in the seven webapp descriptors. Every
  named class exists; the single finding was the TLD whose classes cannot satisfy the JSP contract.
- Every entry of the webstudio web descriptor, the repository's only `web.xml`: all six filters and both servlets
  against their mappings, all three listeners, the error page target, and every mapped URL against every client.
  One finding, the `/action/prop_values` servlet; `/action/*` now has no entry left.
- Both Facelets taglibs (3 custom tags, each used by a page), every `ui:define` name against every `ui:insert`
  (only `content` and `title` exist, both matched), and every Jekyll layout, include and `navigation.yml` url
  under `Docs/`. No finding.
- The three log4j2 configurations: every appender used, and no named logger declared at all. `compose.yaml` uses
  all five of its volumes and all five services, and the `Dockerfile` uses every stage, `ARG` and `ENV`.
- The whole of `DEV/org.openl.rules.gen`: all 12 templates against the variables the generator supplies, both
  directions, plus every public member of the eleven helper classes. Four dead members, now shipped.
- Both archetype modules, both directions: every `archetype-resources` file against every `<fileSet>`, and every
  fileset against the files it matches. Also all four assembly descriptors in the repository. No finding.

## Human follow-ups

- Give the sweep the SonarCloud rule key and file/line for PR #2058's new-code reliability issue, or allowlist
  `sonarcloud.io`. The quality gate is the only thing keeping that pull request from green.
- Mirror the OpenSAML artifacts, or allowlist `build.shibboleth.net`, so `org.openl.rules.webstudio` can build
  here. Maven Central is not an alternative — the artifacts are not published there. PMD now scans the module
  without compiling it, so this only blocks compile-verifying a Java deletion in it.
- Delete the abandoned remote branch `dead-code/studio-resources` (PR #2055 closed unmerged; its only change is
  already on `main` via #2054). `git push --delete` gets HTTP 403 through the proxy and the GitHub MCP server
  has no delete-branch tool, so this needs a human or the repo's auto-delete setting.
- Decide on `TableViewerTag` / `TableEditorTag`, `DecisionTableBuilder.methodName`, `SimpleGroup.description`,
  `MergeResult.status` and the five `XlsProjectionType` cell constants: all are dead but public in published
  artifacts.
- Collapse the duplicated deployment examples: `Docs/examples/production/` and `Docs/production-deployment/` hold
  the same 32 files under two navigable paths, so every future edit has to be made twice.
- `Docs/developer-guides/rules-projects.md` tells a developer to define a validator constraint in
  `TablePropertyValidatorsWrapper.init()`, a method that does not exist; the constructor does that work.
- `openl-project-archetype`'s descriptor name is `org.wso2.carbon.authenticator.connectors.email`, a copy-paste
  leftover; renaming is not a deletion.
- `studio-ui` developer documentation is stale: `Docs/onboarding/common-tasks.md` names `npm run test:coverage`,
  which is not a script, and `README.md` calls `start` webpack, `serve` a `serve -p 3002` run and `lint` a
  Stylelint run. Fixing text is not a deletion.

## Run log

- Run 13: no new detector; a `Sonar analysis` re-run passed, proving its JaCoCo error transient.
- Run 14: PMD standalone over all 4031 Java files — 50 violations, one shipped; three cheap detectors empty.
- Run 15: types 44-47 (public members of non-published modules and of internal packages, duplicate keys inside
  one file, npm scripts) closed empty; type 48 shipped three dead exclusions. Ledger compacted from 398 lines.
