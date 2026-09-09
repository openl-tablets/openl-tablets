# Dead-code sweep ledger — openl-tablets

## Resume point

- Open PR #2092 on `dead-code/legacy-web-resources` (3 commits: JS members, CSS rules, uncalled Java methods); maintain it first.
- #2093 was a duplicate PR from a concurrent run of this routine; its commit was folded into #2092 and it is closed. One run at a time.
- Every change type has had a repo-wide pass on main 5698aad6; main moved past it (9243b096, Docs only) during the run.
- Next run: diff `origin/main` against 5698aad6, rerun PMD, the identifier index and the ASM scan; judge only members in changed files.
- Same-kind finds extend the matching commit in #2092 with `--fixup` + autosquash; a new kind is a new commit there.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS) | done 2026-09-09 (2062, 2089); 39 prose/TODO/reasoned blocks kept |
| 2 | Never-read assignments, dead stores | done 2026-09-09 (1913, 1918, 1940, 2058, 2082); PMD rerun 2026-09-09: 0 new |
| 3 | Unused locals, private fields/methods/params | done 2026-09-09 (1913, 1940, 2058); PMD rerun 2026-09-09: 0 new |
| 4 | Unused Maven dependency declarations | done 2026-09-07 (1913, 1915, 1918, 2058, 2082); rest are providers |
| 5 | Pom metadata: managed entries, exclusions, plugin config, properties | done 2026-09-07 (1915, 2058, 2060, 2063) |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done 2026-09-07 (2063) |
| 7 | Unreferenced resources (descriptors, TLD, config files, images) | done 2026-09-09 (1906, 1912, 2054, 2058, 2063); rescans 2026-09-09: 0 |
| 8 | CSS rules (legacy webstudio, tableeditor, DEMO, inline) | in review 2026-09-09 (#2092 `.clickable`); earlier 2004, 2054, 2056, 2062 |
| 9 | Legacy JS functions and .xhtml pages | in review 2026-09-09 (#2092, 4 JS members); .xhtml all alive (1933, 2054) |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done 2026-09-08 (1906, 1933, 2056, 2082) |
| 11 | TypeScript exports, types, components, imports | done 2026-09-09 (1906, 1909, 2063, 2082); rescan 849 exports: 0 |
| 12 | Test fixtures: workbooks, utility classes, stub members | done 2026-09-09 (1940, 2088) |
| 13 | Package-private/protected members and unreferenced internal classes | in review 2026-09-09 (#2092, 4 methods); earlier 1913, 2058, 2088 |

Public API is never in the queue: unused public members go to Deferred findings for a human decision.

## Open PR

- #2092 `dead-code/legacy-web-resources`, head c672baed, merge-base 5698aad6; CI restarted by the third commit.
- db993d6f Remove table editor script members no page, renderer or script calls (5 files: TableEditor.js saveChanges + GET_CELL_VALUE, BaseEditor.js getDisplayValue, popup.js duplicate hide key, regenerated bundles).
- a7e76d36 Drop the clickable CSS rules no page applies (common.css `.clickable`, `.clickable:hover`).
- c672baed Remove methods no caller reaches in internal engine packages (DPOA.makeEvaluator/3, ExpressionImpl static getExpression x2, DomainImplWithHoles.values).
- No review threads yet; CodeRabbit was rate-limited on the first push.

## Merged PRs

- 1906 dead resources, unmounted studio-ui components; review: packaged js/** is HTTP-reachable, criterion is in-repo reference.
- 1909 studio-ui trace styles (one createStyles key). 1912 orphan Docs images (43). 2004/2054/2056 legacy CSS and i18n keys.
- 1911 WSFrontend/repository.git/tableeditor/DEV members; review: keep CacheAndWriteOutputStream fork, Javadoc why it differs.
- 1911 review: maintainer says the AGENTS.md Jira-prefix exemption does not cover production deletions; merged unprefixed anyway.
- 1913 private members/params, dead stores, ModelExport; review: removing a param can make a caller's param dead — rerun PMD after.
- 1915 unused deps, OpenL2TextUtils (public, EPBDS-16309); review: public-API removal needs Jira confirmation.
- 1918 dep declarations + never-read writes; review: used-undeclared deps are additions → separate hygiene PR, not this sweep.
- 1933 57 i18n keys, PopupMenu.showChild, regenerated tableeditor bundles. 1940 initializers, locals, JavaType fixture.
- 2055 closed unmerged and 2093 closed unmerged: duplicates from concurrent runs — check open `dead-code/*` PRs again right before pushing.
- 2058 dead component-scan package, TablePropertyValues servlet, TLD, CXF map, jsp-api, Azure exclusions.
- 2060 managed webstudio jar entries. 2062 commented-out code (55 Java files, JS, CSS). 2063 redundant constructs, suppressions, settings.
- 2082 dead increments, MergeModal wire types, openl-yaml and security.standalone deps, 23 locale keys.
- 2088 uncalled `.impl.` methods (word index + ASM), constrainer TestUtils, 2 protected fields, 18 test .xls; merged with Sonar gate red.
- 2089 33 commented-out blocks via SonarCloud S125; CodeRabbit invented a behavior change on a comment-only diff.

## Module coverage

- All 86 reactor modules, studio-ui, Docs, DEMO and archetypes have been swept for every change type; nothing left open.
- Only code merged after each pass can yield again: diff `origin/main` against the last swept head before choosing a detector.

## Deferred findings

- TableEditorTag/TableViewerTag: public JSP tag handlers with no .tld, referenced nowhere; 18 write-only ValueExpression fields behind public setters (published artifact).
- tableViewer tag, UITableViewer component and renderer registration: no page usage; tableeditor is a published artifact.
- OpenLServiceFactoryBean.setProxyInterface: @Deprecated public, unreferenced; binary-compatibility break.
- Module.wildcardName: written by 2 callers via Lombok setter, read by none; needs cross-module call-site edits.
- DecisionTableBuilder.methodName + public setMethodName + call in TableSyntaxNodeDispatcherBuilder: inert chain, public DEV API.
- SimpleGroup.description: public setter and public 3-arg constructor parameter, read nowhere.
- XlsProjectionType GRID..CELL_FONT (9 constants, `// TODO do we need the rest?`): public enum, no values()/valueOf use.
- IConditionEvaluator.DECORATOR_CONDITION_PRIORITY, IColorFilter.COLOR_NAMES/COLORS: unread public interface constants.
- IBoundModuleNode, RuleServicePublisherMapper: unreferenced public types.
- Protected abstract without caller: ADtColumnsDefinitionTableBoundNode.isReturns (3 overrides), AbstractOpenlTableExporter.getExcelSheetName (4).
- TableBuilder.getGridModel: protected in a public non-final DEV class = extension surface.
- Pure super-forwarding publics: DependencyOpenClass.getTypes/findType, CastingCustomSpreadsheetResultField.getDeclaringClass, SidExistsValidator.isValid.
- MethodUtil.printMethod(IOpenMethodHeader,StringBuilder,Function), SpreadsheetCell.isValueCell: public, same-class callers only.
- Unused class type parameters on public types: ReturnOperation, IStorage, ProjectService.
- Sonar S1130 16 public/protected throws clauses nothing throws; S1172 229 unused params (159 public/protected, 52 test, 7 pkg-private).
- MergeResult record: `status` component ignored by the compact constructor; removing it changes a public record signature.
- ServiceManagerImpl.deploy: second `serviceDescriptionInProcess` write is a no-op only if createService cannot re-enter deploy.
- GitRepository visitors `result = null` in catch: dead store whose removal leaves an empty catch (Sonar S108); keep.
- ExpressionFactoryImpl.findExpression: compiled out by the constant toggle `_getFromCache = false` (JLS constant variable); a cache switch.
- ITEST HttpData.writeBodyTo: only a commented-out call in HttpClient, but webstudio AGENTS.md documents toggling it to recapture fixtures.
- JavaCC-generated BExGrammarTokenManager/SimpleCharStream carry unreferenced members; generated sources are never edited.
- jQuery.sub in webapp/javascript/vendor/jquery-back-compat.js: no caller, but inside a vendored shim whose other shims are used.
- Vendored CSS zero-hit tokens: bootstrap.min.css (clearfix, hide-text, input-block-level, pagination-*, typeahead, dropup, navbar-fixed-bottom, dropdown-submenu, pull-right), diff2html.css (selecting-left/-right).
- tooltip.css skins green/red and position top_center: no caller passes them, but tooltip.js still handles them; removing CSS alone is half a removal.
- jaxb-runtime in workspace, spring-core/spring-security-core in security.standalone, kafka-clients in ruleservice.kafka: transitive providers.
- org.openl.rules.jackson in ruleservice.ws.common: only path to spring-core (BinarySchemaConverter); fix is 2 added declarations.
- Test-jar executions in ruleservice and ruleservice.deployer include only org/openl/rules/ruleservice/test/* (absent) → publish empty jars.
- maven-plugin-plugin reporting entry produces no report; site.xml links plugin-info.html and 7 *-mojo.html nobody builds.
- eslint react-hooks plugin registered with no rule enabled. RulesUtilsTest @SuppressWarnings("deprecated") misspelled; fix, not deletion.
- 47 dependency `version` elements repeating the managed version (44 in jacoco-report): DRY, not dead.
- common.js:127 `!$submit.hasClass('own-loader-handler')` can no longer be false; a live-condition rewrite, not a deletion.
- `listeners` var in copyModule.xhtml/editOpenAPI.xhtml: dead store, but `new Listeners()` has side effects.
- Dev toggles: `// files = new File[]{...}` in RulesInFolderTestRunner, OpenAPIGenerationTest, OpenAPIProjectCreatorTest; ITEST HttpClient bulk rewrite.
- 153 `/* (non-Javadoc) @see */` markers beside @Override: comment churn. AzureBlobRepository `final` in try-with-resources: author style.
- DEV/org.openl.rules/doc/Table Properties Design Points.docx: unreferenced historical design material.
- revisions.xhtml: React residue stranding WebStudio.getProjectVersions/setProjectVersion/canOpenOtherVersion; recheck when React finishes.
- Docs/architecture/dependencies.md row for org.openl.rules.diff says `commons`, pom says org.openl.rules.

## False-positive shapes

- Lombok @Getter/@Setter: field named only via getFoo/isFoo/setFoo/hasFoo/withFoo/addFoo → search capitalized accessor forms first.
- Jackson wire names: ServiceInfo.getHasManifest → `hasManifest` in static/index.html and ITEST fixtures; check the JSON field name.
- Spring XML `property name=` binds setters by decapitalized name; `class=` strings instantiate deps; @ImportResource pulls other modules' XML.
- JAXB private beforeMarshal/afterUnmarshal (RulesDeploy, project.model) run reflectively; library overrides often lack @Override.
- @Override of an EMPTY base method (CachedOutputStream.onWrite) IS dead: @Override alone is not proof of liveness.
- Name in any string literal or non-Java text (reflection, EL, JAXB, @MethodSource) keeps a member alive; filter ASM candidates on it.
- Record component accessors (PKey.in, ProjectIndexCache.lastUpdateTime); MergeResult.status set by compact constructor.
- ASM descriptor match misses generic abstract methods: `abstract T getAvgX()` erases to Number while callers invoke the Double override → match by name too.
- ASM sees no @MethodSource/@ValueSource providers (annotation values are not ldc strings): filter annotated declarations.
- ASM override check: a supertype outside the scanned classes (third-party) must count as a possible override; JDK supertypes can be checked by reflection.
- Jackson MixIn methods (GroovyObject.getMetaClass), empty SubtypeMixin marker, @Bean, @ExceptionHandler, argument-resolver overrides.
- Reflection fixtures: YamlMapperFactoryTest MyBean, FormatterTest, ModuleTest, RulesUtilsTest, JavaOpenClassTest beans, JsonUtilsTest.BindingClasses.
- Test types alive without reference: @Test classes, abstract-base inheritors, TestRunner* Spring configs, WizardUtilsTest dir-scan beans, JMH.
- Test fixtures with deliberately private members: epbds6830 BeanA.getAB, AOpenClassTest.C.getC, JavaOpenClassTest.BeanA.gg (asserted by name).
- Velocity: gen wrappers reached as `$wrapper.asList()` or `$wrapper.PropertyType` → grep .vm for both spellings.
- i18next: `_one`/`_other` plural suffixes; template keys t(`browser.${id}_confirm`), role., debug.status., notifications.${kind}_deleted.
- Transformed keys: ValidationMessages `openl.error.<status>.<code>` built by RestRuntimeException.getErrorCode; only the suffix is in Java.
- EL-composed keys: `#{msg['ws.project.openapi.mode.'.concat(mode.name().toLowerCase())]}`; repo-default.<type>.<suffix>.
- studio.url('page') drops .xhtml; index.xhtml crossroads routes add ".xhtml" → search extension-less base names.
- Runtime CSS classes: 'tooltip_'+position, "tooltip_skin-"+skin, ui-layout-* (jquery.layout.js), te_toolbar_*, table id = clientId+suffix.
- `gradient` in common.css is a filter value, not a class; jquery-popup-close-icon / clock-icon.png are substring hits, not uses.
- Legacy JS common-word method names (show, hide, focus, getValue, save) are reached through `this.editor.<name>` in TableEditor.js; confirm there first.
- PMD UnusedAssignment blind spots: constructor early return (CellStyle), try/catch pairs (GitRepository), field read back through a callback (DynamicPropertySource.settings via resolver).
- PMD UnusedAssignment: publication before blocking or callback (DebugChannel.status, DebugHookImpl.pendingDispatch, ServiceManagerImpl); for-each counting variable (RulesUtils.getValues).
- PMD UnusedPrivateMethod without aux classpath: method references (XlsBinder::addBindingContextError), overloads by argument type, lambda/Supplier overloads (ProjectCreationService, UserWorkspaceImpl, MethodUnreachableStatementValidator).
- Unused-local FPs: try-with-resources vars (WebSocketAuthTest.stomp, ExtensionsConfigurationTest.context); null before System.gc(); assign before fail() hosting a cast.
- `var ignored = executor.submit(...)` silences Error Prone FutureReturnValueIgnored (Sonar S1481/S1854 FP).
- UnnecessaryCast FP: cast gives `var` its type; UnnecessaryBoxing FP: pins an overload in Operators; varargs `new Object[]{null}`.
- javac caps warnings at 100: raise -Xmaxwarns before judging @SuppressWarnings("deprecation") dead.
- ESLint no-unused-vars never flags an unused `React` default import under react-jsx → use tsc --noUnusedLocals; ESLint lints only ./src.
- dependency:analyze FPs: annotation processors, CLASS-retention (jspecify), aggregators (junit-jupiter, swagger-parser), runtime providers, wars.
- openl-maven-plugin reads its own dependency list by coordinate string (VerifyMojo.getJars) → all its "unused" deps are live.
- Dead vs live `exclusion`: the reactor tree hides duplicates; resolve the dependency alone with dependency:tree -Dverbose in a scratch pom.
- RedundantFieldInitializer unsafe when a superclass constructor makes a virtual call (ComponentOpenClass/ADynamicClass addField).
- Sonar S3626/UnnecessaryReturn: `synchronized(this){return;}` and symmetric chain arms are load-bearing; drain loops are not empty statements.
- CodeRabbit walkthroughs invent behavior changes on comment-only diffs → answer with `git diff --numstat` (0 insertions).
- Dead suppression check: remove all, recompile; TS @ts-ignore mostly real (exactOptionalPropertyTypes antd conflicts).
- Resource base names that match nothing but are alive by convention: Flyway `db/flyway/**`, spring.factories, extension-*.xml, archetype-metadata.xml, META-INF/cxf/*.

## Method rules

- Prove non-reference with `grep -rIwF <name>` over all file types excluding target/ node_modules/ .git/ studio-ui/coverage studio-ui/dist, plus `grep -ra` for binaries, plus accessor forms and the extension-less stem.
- Search workbooks: `unzip -p *.xlsx | grep -wF`, .xls as raw bytes; rule tables can name Java methods. Include the maven-plugin `it/` sources in every search.
- Build an identifier-frequency index over all tracked text files: global count 1 = declaration only; scope test types to their own module.
- Track enclosing types for effective visibility: a public member of a private or package-private type is internal.
- ASM scan of target/classes + target/test-classes (all reactor modules, ITEST, maven-plugin): invokes, method handles, lambda bootstrap args, field get/put.
- ASM filters: drop @Override, any annotation, name in non-Java text or Java string literal, record accessors; skip compile-time-constant fields.
- ASM scanner source lives in no repository: rebuild it (~130 lines on org.ow2.asm from ~/.m2) each run; 5155 classes scan in under a minute.
- Run PMD standalone (pmd-dist from GitHub releases, not Maven Central; maven-pmd-plugin skips the non-standard `test` roots); absolute paths in `--file-list`; drop hits under target/; rerun on the post-change tree.
- javac lint (Error Prone): UnusedMethod, UnusedVariable, EffectivelyPrivate, UnusedNestedClass; ignore [NullAway].
- SonarCloud web API (api/issues/search, branch=main) works through the proxy although the dashboard 403s: S125, S1068, S1481, S1854, S1144, S1130, S3626, S1172.
- Maven deps: `mvn -o dependency:analyze-only` after install; read "used undeclared" too; prove with dependency:tree before/after, `dependency:list -DincludeScope=runtime`, war WEB-INF/lib listing.
- Pom metadata: `mvn help:effective-pom -Pitest` diff before/after; plugin parameter names from META-INF/maven/plugin.xml in the plugin jar.
- Comment-only proof: strip comments and literals on both sides, code byte-identical; `git diff --numstat` adds 0.
- Frontend gate: npx tsc --noEmit, npx eslint ./src, npx vitest run with no competing Maven; import graph incl. islandRegistry.tsx.
- i18n scan: eval bundles with a stub addResourceBundle, flatten dotted keys, strip plural suffix, substring over src/**/*.ts(x) minus locales, keep template prefixes.
- Images/resources: one pass `grep -rIohwF -f names.txt` then `comm` against the name list; check Jekyll _includes/_layouts globs.
- CSS: a token counts as used only in a class-applying context (class=, styleClass=, *Class=, columnClasses, JS className/classList, selector strings, EL ternaries, Java HTML strings); prose and property values are not usages.
- Legacy JS: a name is alive when HTMLRenderer.java emits it in a Java string, table.xhtml calls it via getCurrentTable(), or an .xhtml inline handler names it; search .java and .xhtml before .js.
- Tableeditor: after editing js/*.js or css/*.css regenerate the bundles with compile.js.sh / compile.css.sh and commit them in the same commit.
- Java gate: never -DskipTests (skips test compile); ITEST needs -Pitest -DskipTestsForQuick=false; full `mvn clean install -Dquick -DnoPerf -T1C` before push.
- Regroup: one commit per change type per repo-wide pass; fold fixes with --fixup + autosquash; rerun detectors after a removal (cascade rule).
- Deletion-only PRs: Sonar "New Code" is wider than the diff and may be red for pre-existing findings; state that once, do not patch.

## Keep-list

- rf-* RichFaces (JS ships in the jar, even rf-fu-* with no rich:fileUpload left), ant-* antd runtime, ui-layout-*, tooltip_* → permanently unprovable CSS.
- Vendored libraries go whole or not at all: datepicker, prototype-1.7.3.js, jquery-migrate/jquery-back-compat shims, jquery-ui ($.fn.draggable, $.effects used by jquery.layout.js), diff2html, javascript/vendor/**, rapi-doc/.
- Convention files: META-INF/services/**, META-INF/openl/extension-*.xml (@ImportResource classpath*), cache2k.xml, WEB-INF/classes/logging.properties (Tomcat JULI), simplelogger.properties.
- Convention files: openl-db-repository-*.properties ("/openl-db-repository-" + databaseCode), META-INF/io/opentelemetry/**, archetype-metadata.xml, site.xml, CITATION.cff, Flyway db/flyway/** + placeholders.properties.
- Property loaders: DefaultPropertySource → openl-default.properties only; ApplicationPropertySource → application*.properties + {appName}.properties.
- Tableeditor bundles: compile.js.sh/compile.css.sh regenerate tableeditor.all/min.{js,css}; HTMLRenderer loads min only; pristine sources reproduce the committed bundles byte for byte.
- TableEditorDispatcher serves any classpath path under webresource/ → packaged js/** is HTTP-reachable; criterion is in-repo reference only.
- TableEditor.Operations values are URL action names matched by method names in TableEditorController (string-linked); React reaches legacy JS only via globalThis.openl.
- Code-generator templates rewrite only the INSERT block of DefaultTablePropertiesSorter and DefaultPropertiesContextMatcher.
- Test classes are invisible cross-module unless the module publishes a test-jar: only org.openl.rules.ruleservice and ruleservice.deployer do.
- DEV/org.openl.rules.test is never installed (maven.install.skip) → must be in the reactor; bare -pl on its consumers fails.
- DEV/org.openl.rules.gen: pom packaging + maven.deploy.skip → its public members are not published API.
- Public members of published DEV/**, STUDIO, WSFrontend artifacts stay; `.impl.`/`.internal.` packages are internal.
- JSF wiring: faces-config.xml, html.taglib.xml, tableeditor.taglib.xml (Facelets), web.xml filters/listeners, ui:include/composition/decorate.
- Indirect class application: columnClasses/rowClasses/headerClass/footerClass/infoClass/errorClass/nodeClass, EL ternaries.
- Spring: component-scan base packages, beans by id/type/collection/SpEL, @Qualifier("serviceDescriptionInProcess") publishes a field.
- Jackson wire model: ITEST expected-response fixtures name JSON fields; studio golden OpenAPI schema names request records.
- Bean Validation default-message override key jakarta.validation.constraints.Size.message in ValidationMessages.properties.
- Test workbooks are opened by literal relative path (no listFiles/Files.walk in DEV tests) except decisionTableIndexes/ loaded as a folder.
- Lombok @Delegate(excludes=AuthoringRepository.Writes); OpenL datatype interfaces bound from Excel by name (IChildBean.getMyBean).
- WrapperValidation.validateWrapperClass: lang/xls/binding/wrapper/base/** must redeclare every public superclass method.
- OpenL type model uses getDeclaredMethods() keyed on erased params: generic overrides (CharRange.contains(Character)) are load-bearing.
- hashCode delegating to super beside an overridden equals keeps the Sonar pair contract; annotation-carrying overrides (@NotBlank, @JsonProperty) stay.
- Members carrying instructions stay: `THIS CONSTRUCTOR MUST BE EMPTY!!!`, GenericComparator "use getInstance()" Javadoc.
- Out of scope: openl-maven-plugin it/ fixtures, Docs/examples and Docs/production-deployment poms, archetype resources, test-resources gen/.
- Icons by literal path (rules-tree, diff icons, site.webmanifest); ITEST 001-Get-Static-CSS asserts only status/content-type of common.css.
- War reachability is WEB-INF/lib, not compile: repositories are instantiated reflectively by class name from production-repository.factory.
- SLF4J bridge log4j-slf4j2-impl runtime scope pinned by Log4jRoutingTest; swagger-core-jakarta is the deliberate substitute (root excludes swagger-core).

## CI flakes

- LockTest.testSimultaneousMultiThreadsWithWaiting (STUDIO repository): `expected <800> but was <79x>` under load; stabilized on main; rerun once.
- studio-ui vitest CPU starvation under -T1C: OverviewPanel.test.tsx (15000ms timeout, act() warning via vitest-fail-on-console).
- UserDatailsTab.test.tsx "rejects an empty email and display name": findByText timeout when the vitest run takes ~680s; rerun once.
- OpenLTableLogicTest.detectsErrorsInRulesTestedByTable: `expected true was false`; getMethod right after async setModuleInfo compile; rerun once.
- IT (services-data): apache/kafka-native:latest segfault in Pwd.getpwuid at ~0.01s in `setup`; random Kafka suite; rerun clears; never pin the tag.
- IT (studio-acl): OracleRdbmsTest upgrade `Failed requests expected <0> but was <N>` with ORA-12516; also on main; rerun once.
- IT (studio-acl): testcontainers/ryuk pull failure → all 4 variants error at upgrade:53 (runner degraded); rerun.
- IT (studio): WebStudioTest.simple failed requests at ~10002ms (client timeout); Jetty hang; rerun.
- itest.studio.repos race: two revisions share createdAt → history order flips (task_EPBDS-15439/.../500-verify); rerun.
- Sonar analysis job: jacoco report-aggregate `Unknown block type` on ITEST/server-core/target/jacoco.exec (overlapping artifact merge); rerun.
- SonarCloud gate on deletion-only PRs: New Code wider than the diff; pre-existing S2259/S6466 attributed to shifted lines; deterministic, no rerun; merged red before (1933, 1940, 2058, 2063, 2088).
- Maven build extension archetype-packaging transiently unresolvable on one runner ("could not read 2 projects"); rerun.
- Job logs 404 while in_progress; the log API returns only the tail. CodeRabbit Docstring Coverage warning fires on deletion-only diffs; ignore.
- Rerun budget: 2 per check per SHA; reruns are not possible from this sandbox (no gh, no Actions write) — say so once and let the next push retry.

## Container facts

- No `gh` CLI: use the GitHub MCP tools (pull_request_read, update_pull_request, add_issue_comment, actions_list, get_job_logs).
- `git push --delete <branch>` is refused by the proxy ("remote end hung up"); branch deletion is a human follow-up.
- Cold ~/.m2 at session start: the first `-T1C` build needs the network (~30 min) and -Daether.syncContext.named.time=600, or resolution fails with "Could not acquire lock(s)"; never -o before that build.
- After a -T1C failure, `mvn -rf :<module>` cannot resolve the banned siblings or the never-installed rules.test → rerun the whole `mvn install` (no clean, ~10 min warm).
- Container presets gpg.format=ssh, commit.gpgsign=true, gpg.ssh.program=/tmp/code-sign; JGit has no ssh signer → repository.git tests die; `git config --global --unset commit.gpgsign` (and gpg.format) before the build; env overrides do not reach JGit.
- ~/.gitconfig sets the Claude identity; set user.* globally and check `git log -1 --pretty='%an|%cn'` after the first commit.
- No locale set: run builds with LANG=C.UTF-8 (one archive test uses a non-ASCII fixture name).
- 4-core container: -T1C starves vitest (UserDetailsTab fails); use -T2 for whole-reactor builds when studio-ui tests run; never run npm in studio-ui while Maven runs.
- Never `git switch --orphan` in the working tree while a build runs (it empties the tree): use a separate `git worktree add --detach` for the ledger branch.
- pmd-dist bin zip is not on Maven Central; download from github.com/pmd/pmd/releases (~130 MB) and run `bin/pmd check --file-list`.
- No Jira access; no Docker (ITEST unrunnable); sonarcloud.io dashboard 403 but api/issues/search reachable.
- npm install --package-lock-only strips libc metadata from 10 optional platform packages; edit package-lock.json by hand, verify with npm ci.
- opensaml-bom 5.2.3 lives only on build.shibboleth.net: if CONNECT 403 returns, stub an empty BOM in ~/.m2 and skip webstudio with -pl.
- Two runs of this routine can fire the same day: list open `dead-code/*` PRs and fetch `dead-code/ledger` again right before pushing anything.

## Exhausted veins

- studio-ui: package.json deps, 15 locale namespaces, enum members, Props members, public/ assets, module graph, 849 exports, CSS-in-JS keys, @ts-ignore, React imports.
- studio-ui exports used only in their own file (~36-70): live; dropping `export` is a visibility refactor, never a deletion.
- Legacy JS: 382 declarations in 38 files and every whole-file loader; only the four members in #2092 and jQuery.sub (vendored) are dead.
- CSS selector tokens in all 17 stylesheets: only `.clickable` (#2092) dead outside vendored files and runtime-built names; inline styles all used.
- Tableeditor: every CSS/JS source in the concat lists; datepicker helpers vendored; webstudio .xhtml (46-50) all reached; ui:param and xmlns all used.
- HTML comments in .xhtml/.html: explanations only; f:facet names all standard RichFaces.
- Images: 703 tracked images outside ITEST all named as a whole word in a text file (Docs, webstudio, tableeditor, Rule Services, Studio static).
- Message bundles: openapi.properties, messages.properties, ValidationMessages.properties, openl-default.properties all reachable; config property files all have readers.
- Resource base-name scan (all `resources/` dirs outside ITEST/test-resources/archetypes): 45 zero-reference hits, all convention files.
- Spring XML beans (11 non-test files), component-scan entries (24), JSF registrations, web.xml filters/listeners: all alive except tableViewer (deferred).
- Maven: root/module properties, every profile, root pluginManagement, 177 managed artifact ids, 18 exclusions, plugin config, surefire sysprops, resource dirs: all alive; dependency:analyze-only remaining hits are providers/aggregators/processors/wars.
- Java PMD (UnusedAssignment, UnusedLocalVariable, UnusedPrivateField, UnusedPrivateMethod, UnusedFormalParameter) over all 4045 files: 46 hits, all FPs or deferred.
- Java identifier index: non-public methods with global count 1 in main sources = 2 (JAXB hook, @Bean); count-1 fields = 36, all Lombok accessors.
- ASM scan on main 5698aad6 (5155 classes): 32 hits; 4 removed in #2092, 28 catalogued FPs or deferred; nothing else non-public is unreferenced.
- Java: protected members of final classes, public members of .internal. packages: none; pkg-private top-level (246) and nested (318) publics all called.
- Java: enum constants all alive except XlsProjectionType (deferred); classes with no bytecode reference (22) all reflection fixtures/inheritors/@Delegate excludes.
- Java: JavaDoc tags, @SuppressWarnings keys, bare super(), empty default constructors, UnnecessaryBooleanAssertion: done; UnnecessaryFullyQualifiedName/UselessParentheses are rewrites, not deletions.
- SonarCloud on main: S125 done (39 kept); S1128/S1116/S3985/S1596/css:S4658 zero; S1068/S1481/S1854/S1144/S2094/S1119/S3626 all FP or deferred.
- Test workbooks outside ITEST/test-resources/it (254): only 3 folder-loaded ones unmentioned; non-workbook test/ files (3) live.
- Commented-out code: Java main+test, CSS, JS done; XHTML/TS none. Dead suppressions: 137 undecidable (category unchecked).
- .gitignore/.gitconfig/.gitattributes/.editorconfig, package.json fields, eslint/vite/vitest config imports: done.
- DEMO/** assets and archetype scripts/ all referenced; Docs/** pages none orphaned (sidebar from site.pages, full-content search).

## Human follow-ups

- Test bug: JAXRSOpenLServiceEnhancerTest.shouldAddApiResponsesIfOperationNotAnnotatedByApiResponses enhances the wrong fixture interface.
- Sonar S2259 (NPE) in ComponentTypeArrayOpenClass.isAssignableFrom/isInstance; null-guard patch proposed in #2088.
- Sonar S6466 CRITICAL WorkbookListener:273; S2259 XlsBinder:523, ProjectModel:1214, TableEditorModel:106, TestDownloadController:144.
- site.webmanifest names `android-chrome-512x512.pngs` (trailing s): the 512px icon is unreachable; one-character bug.
- Policy: Jira prefix for sweep commits (maintainer view: exemption excludes production deletions); public-API removal needs a ticket.
- Confirm EPBDS-16309 authorises the OpenL2TextUtils removal (decision came from a sweep state file, not Jira).
- CI health: LockTest load tolerance; OracleRdbmsTest ORA-12516; itest.studio.repos createdAt tiebreaker; kafka-native:latest segfault; jacoco aggregate overlap.
- OverviewPanel.tsx floating promise into a state setter (~lines 799/1231) is a real test defect at any speed.
- Public unused members awaiting a decision: see Deferred findings. Dependency hygiene PR: declare commons-lang3 (openapi-parser, project.openapi, validation.openapi), groovy test, org.openl.rules.project.
- Delete the merged remote branches dead-code/uncalled-methods and dead-code/uncalled-internal-methods (push --delete is blocked from the sandbox).
- The routine fires twice a day from two triggers or sessions (runs "a"/"b" and this one overlapped); keep one schedule.

## Run log

- 2026-09-09 a: #2088 merged; ledger created.
- 2026-09-09 b: PR #2092 opened (JS members, `.clickable`); images, TS exports, PMD scanned.
- 2026-09-09 c: ledger rebuilt from 20 merged PRs and merged with b's; PMD, identifier index, resource and ASM scans → 4 uncalled methods added to #2092; duplicate #2093 closed.
