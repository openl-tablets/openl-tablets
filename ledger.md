# Dead-code sweep ledger — openl-tablets

## Resume point

- Open PR #2104 (`dead-code/compare-residue`): 4 commits, 13 files, 162 deletions. Keep it green and answered.
- Swept head is `origin/main` e01088de. The EPBDS-16576 comparison vein is done; the 8 EPBDS-16415 commits after it
  (97 files, 0 deletions) are additive and stranded nothing. Next vein: whatever merges after e01088de.
- `origin/main` is red in ITEST and no rerun fixes it (see Human follow-ups) — never this PR's failure.
- On changed code only: rerun PMD, the identifier index and the ASM scans over the changed files, never the whole tree.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS) | done 2026-09-09; 39 prose/TODO/reasoned blocks kept |
| 2 | Never-read assignments, dead stores | done 2026-09-09; PMD rerun: 0 new |
| 3 | Unused locals, private fields/methods/params | done 2026-09-11; PMD clean |
| 4 | Unused Maven dependency declarations | done 2026-09-07; the rest are providers |
| 5 | Pom metadata: managed entries, exclusions, plugin config, properties | done 2026-09-07 |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done 2026-09-07 |
| 7 | Unreferenced resources (descriptors, TLD, config files, images) | done 2026-09-11 g; 9 files in #2104 |
| 8 | CSS rules (legacy webstudio, tableeditor, DEMO, inline) | done 2026-09-11 g; 2 rules in #2104 |
| 9 | Legacy JS functions and .xhtml pages | done 2026-09-11 g; 2 pages in #2104, JS clean |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done 2026-09-11 g; 66 candidates all template-composed |
| 11 | TypeScript exports, types, components, imports | done 2026-09-11 |
| 12 | Test fixtures: workbooks, utility classes, stub members | done 2026-09-09 |
| 13 | Package-private/protected members and unreferenced internal classes | done 2026-09-11 g; UiConst in #2104 |

Public API is never in the queue: unused public members go to Deferred findings for a human decision.

## Open PR

- #2104 `dead-code/compare-residue`, head 33329ccbe7, cut from `origin/main` e01088de. EPBDS-16576 residue.
- Four commits, one per change type, ordered so a referrer dies before the resources it alone kept alive:
  UiConst.java (13); simpleLayout.xhtml + messagePopup.xhtml (9); 8 images + css/layout/simple.css (7);
  common.css .scrollable and .dropdown-form (8). No review threads yet.
- #2103 (`dead-code/openapi-layouts-residue`) was a concurrent firing that found a strict subset; closed as superseded
  with a comment. Two overlapping sweep PRs are the real cost of the 4-hourly cron — check open PRs again before pushing.

## Merged PRs

- 1906, 1909, 1911-1913, 1915, 1918, 1933, 1940, 2004, 2054, 2056, 2058, 2060, 2062-2063, 2082, 2088-2089, 2092,
  2095-2096, 2101 — all merged; what each removed is covered by Exhausted veins. 2101 was rebase-merged, so one commit
  per change type survived onto main: a repo-wide single-type commit is accepted as-is.
- GitHub deletes a merged PR's branch by itself; CodeRabbit reviews at most 2 pull requests an hour, silently skipping the rest.

## Module coverage

- All 86 reactor modules, studio-ui, Docs, DEMO and archetypes are swept for every change type; nothing left open. Only
  code merged after a pass can yield again, and a big feature merge is the reliable source: EPBDS-16560 stranded 393
  lines, EPBDS-16576 another 162. Diff `origin/main` against the last swept head before picking a detector.

## Deferred findings

- Unreferenced once EPBDS-16576 retired the JSF diff UI, but public in published jars: EmptyCell, RegionGridSelector
  (DEV); tableeditor grid filters ColorGridFilter, GreyColorFilter, TransparentColorFilter, FontGridFilter, CellStyleGridFilter.
- TableEditorTag/TableViewerTag: public JSP tag handlers with no .tld, referenced nowhere; 18 write-only ValueExpression fields behind public setters (published artifact).
- tableViewer tag, UITableViewer component and renderer registration: no page usage; tableeditor is a published artifact.
- OpenLServiceFactoryBean.setProxyInterface: @Deprecated public, unreferenced; binary-compatibility break.
- Module.wildcardName: written by 2 callers via Lombok setter, read by none; needs cross-module call-site edits.
- DecisionTableBuilder.methodName + public setMethodName + call in TableSyntaxNodeDispatcherBuilder: inert chain, public DEV API.
- SimpleGroup.description: public setter and public 3-arg constructor parameter, read nowhere.
- XlsProjectionType GRID..CELL_FONT (9 constants, `// TODO do we need the rest?`): public enum, no values()/valueOf use.
- IConditionEvaluator.DECORATOR_CONDITION_PRIORITY, IColorFilter.COLOR_NAMES/COLORS: unread public interface constants; IBoundModuleNode, RuleServicePublisherMapper, HistoryLog: unreferenced public types.
- ChoicePointLabel: public in DEV constrainer, its only ctor package-private and uninvoked, so the Constrainer/Failure/GoalOr/GoalStack members typed on it always hold null; removal edits public signatures.
- Extension surface, no caller: protected abstract ADtColumnsDefinitionTableBoundNode.isReturns (3 overrides) and AbstractOpenlTableExporter.getExcelSheetName (4); protected TableBuilder.getGridModel in a public non-final DEV class.
- Same-class or super-forwarding callers only: DependencyOpenClass.getTypes/findType, CastingCustomSpreadsheetResultField.getDeclaringClass, SidExistsValidator.isValid, MethodUtil.printMethod(IOpenMethodHeader,StringBuilder,Function), SpreadsheetCell.isValueCell.
- Unused class type parameters on public types: ReturnOperation, IStorage, ProjectService.
- MappedRepository.create(Repository,String): the 2-arg overload now forwards `false`; only its own tests call it.
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
- Transitive providers, not dead: jaxb-runtime in workspace, spring-core/spring-security-core in security.standalone, kafka-clients in ruleservice.kafka.
- org.openl.rules.jackson in ruleservice.ws.common: only path to spring-core (BinarySchemaConverter); fix is 2 added declarations.
- Test-jar executions in ruleservice and ruleservice.deployer include only org/openl/rules/ruleservice/test/* (absent) → publish empty jars; maven-plugin-plugin's reporting entry produces no report, and site.xml links plugin-info.html and 7 *-mojo.html nobody builds.
- Not deletions but fixes: eslint react-hooks plugin registered with no rule enabled; RulesUtilsTest @SuppressWarnings("deprecated") misspelled; common.js:127 `!$submit.hasClass('own-loader-handler')` can no longer be false.
- Not dead, just repetitive: 47 dependency `version` elements repeating the managed version (44 in jacoco-report) are DRY; 153 `/* (non-Javadoc) @see */` markers beside @Override are comment churn; AzureBlobRepository `final` in try-with-resources is author style.
- `listeners` var in copyModule.xhtml/editOpenAPI.xhtml: dead store, but `new Listeners()` has side effects.
- Dev toggles: `// files = new File[]{...}` in RulesInFolderTestRunner, OpenAPIGenerationTest, OpenAPIProjectCreatorTest; ITEST HttpClient bulk rewrite.
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
- Spring MVC handlers (@GetMapping and friends) have no Java caller at all; the mapping annotation is the only proof of life.
- Uninvoked ctor FPs: Jackson-bound records (readValue/TypeReference/getForObject/awaitMatching), JAXB XmlAdapter, Jackson MixIn, @Component/@Conditional, ctor params carrying @Autowired/@Value, OpenL String-constructor datatypes, JUnit classes.
- A private or protected no-arg ctor is the anti-instantiation idiom, never a finding; an implicit default ctor has no source line to delete; a record's canonical ctor cannot be removed.
- Reflection fixtures: YamlMapperFactoryTest MyBean, FormatterTest, ModuleTest, RulesUtilsTest, JavaOpenClassTest beans, JsonUtilsTest.BindingClasses.
- Test types alive without reference: @Test classes, abstract-base inheritors, TestRunner* Spring configs, WizardUtilsTest dir-scan beans, JMH.
- Test fixtures with deliberately private members: epbds6830 BeanA.getAB, AOpenClassTest.C.getC, JavaOpenClassTest.BeanA.gg (asserted by name).
- Velocity: gen wrappers reached as `$wrapper.asList()` or `$wrapper.PropertyType` → grep .vm for both spellings.
- i18next: `_one`/`_other` plural suffixes; template keys t(`browser.${id}_confirm`), role., debug.status., notifications.${kind}_deleted, create_table_modal.types./.blocked., update_project_modal., fill_preview.state., tests.${kind}/tests.no_${kind}.
- Transformed keys: ValidationMessages `openl.error.<status>.<code>` built by RestRuntimeException.getErrorCode; only the suffix is in Java.
- EL-composed keys: `#{msg['ws.project.openapi.mode.'.concat(mode.name().toLowerCase())]}`; repo-default.<type>.<suffix>.
- studio.url('page') drops .xhtml; index.xhtml crossroads routes add ".xhtml" → search extension-less base names.
- Runtime CSS classes: 'tooltip_'+position, "tooltip_skin-"+skin, ui-layout-* (jquery.layout.js), te_toolbar_*, table id = clientId+suffix.
- A React `data-testid` is not a class: a CSS token surviving only as a test id (param-tree) is dead CSS.
- A React rewrite copies a legacy helper rather than calling it (getDb, isCollection): a same-named method in another file is not a caller — check the qualifier.
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
- Never truncate a proof grep with `head`: a listing cut short once hid org.openl.rules.diff's live consumer and nearly cost a wrong deletion. Count first, print second.
- The clone is shallow, so `git log -S` lists boundary commits that touch nothing: confirm a removal by reading the diff of the newest hit (`git show <sha> | grep '^-.*<token>'`), never by the list alone.
- Search workbooks: `unzip -p *.xlsx | grep -wF`, .xls as raw bytes; rule tables can name Java methods. Include the maven-plugin `it/` sources in every search.
- Build an identifier-frequency index over all tracked text files: global count 1 = declaration only; scope test types to their own module.
- A keep-list mechanism can itself die: the images/diff icons were "referenced by literal path" only from UiConst, so they fell with it. Re-check a keep-listed referrer before trusting the keep.
- Scan a module's types for zero external references, then filter by annotation: in webstudio 59 types had none and 58 carried @Component/@RestController/@Configuration/JSF — the one unannotated class was the only finding.
- A war module's classes are not published API (webstudio), so a public class there is judged by reference; a jar (DEV/**, tableeditor) is, so its public types go to Deferred.
- A handful of changed files is scanned by listing their declared members and grepping each, not by building: ~/.m2 is cold every session.
- Track enclosing types for effective visibility: a public member of a private or package-private type is internal.
- ASM scan of target/classes + target/test-classes (all reactor modules, ITEST, maven-plugin): invokes, method handles, lambda bootstrap args, field get/put.
- ASM filters: drop @Override, any annotation, name in non-Java text or Java string literal, record accessors; skip compile-time-constant fields.
- Ctor reachability: one ASM pass recording every INVOKESPECIAL, method handle and invokedynamic bootstrap argument naming a constructor, matched on owner+descriptor; intersect owners with target/classes to drop JUnit classes.
- ASM scanner source lives in no repository: rebuild it (~130 lines on org.ow2.asm from ~/.m2) each run; 5155 classes scan in under a minute.
- Run PMD standalone (pmd-dist from GitHub releases, not Maven Central; maven-pmd-plugin skips the non-standard `test` roots); absolute paths in `--file-list`; drop hits under target/; rerun on the post-change tree.
- javac lint (Error Prone): UnusedMethod, UnusedVariable, UnusedNestedClass; ignore [NullAway]. EffectivelyPrivate is a visibility refactor, never a deletion.
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
- Java gate: never -DskipTests (skips test compile); ITEST needs -Pitest -DskipTestsForQuick=false; full `mvn clean install -Dquick -DnoPerf -T2` before push.
- Regroup: one commit per change type per repo-wide pass; fold fixes with --fixup + autosquash; rerun detectors after a removal, because removing a param can leave a caller's param dead.
- Order the commits so a referrer dies before the resources only it kept alive; an explicit `git rebase -i` todo through GIT_SEQUENCE_EDITOR both reorders and rewords in one pass.
- Deletion-only PRs: Sonar "New Code" is wider than the diff and may be red for pre-existing findings; state that once, do not patch.
- A "used undeclared" dependency is an addition, not a deletion: it belongs to the separate hygiene PR, never to this sweep.

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
- Public members of published DEV/**, STUDIO, WSFrontend jar artifacts stay; `.impl.`/`.internal.` packages are internal.
- The org.openl.rules.diff module stays whole: org.openl.studio.compare reaches all 23 of its types through XlsDiff2 and DiffTreeBuilder2.
- The CacheAndWriteOutputStream fork stays; its Javadoc records why it differs from the upstream class.
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
- `test-resources/**` in every module (1014 non-workbook files outside ITEST): rail 5 names the pattern and the trees are folder-loaded fixtures; never judge single files. Workbooks under `test/` dirs are judged.
- Icons by literal path (rules-tree, diff icons, site.webmanifest); ITEST 001-Get-Static-CSS asserts only status/content-type of common.css.
- War reachability is WEB-INF/lib, not compile: repositories are instantiated reflectively by class name from production-repository.factory.
- SLF4J bridge log4j-slf4j2-impl runtime scope pinned by Log4jRoutingTest; swagger-core-jakarta is the deliberate substitute (root excludes swagger-core).

## CI flakes

- LockTest.testSimultaneousMultiThreadsWithWaiting (STUDIO repository): `expected <800> but was <79x>` under load; stabilized on main; rerun once.
- studio-ui vitest CPU starvation under -T1C: OverviewPanel.test.tsx (15000ms timeout, act() warning via
  vitest-fail-on-console) and UserDatailsTab.test.tsx findByText timeout when the run takes ~680s; rerun once.
- OpenLTableLogicTest.detectsErrorsInRulesTestedByTable: `expected true was false`; getMethod right after async setModuleInfo compile; rerun once.
- IT (services-data): apache/kafka-native:latest segfaults in its own `setup` entrypoint at VM uptime ~0.007s —
  Pwd.getpwuid <- PosixSystemPropertiesSupport.userNameValue <- PerfManager$PerfDataThread resolving `user.name`.
  Container exits 1, so the visible error is `Timed out waiting for ... RECOVERY to RUNNING`. It picks a random
  Kafka module per attempt (itest.tracing, then itest.kafka.smoke), and took 3 attempts on one SHA to pass, so
  budget more than one rerun before calling it real. Never pin the tag; no in-repo fix.
- IT (studio-acl): OracleRdbmsTest upgrade `Failed requests expected <0> but was <N>` with ORA-12516; also on main; rerun once.
- IT (studio-acl): testcontainers/ryuk pull failure → all 4 variants error at upgrade:53 (runner degraded); rerun.
- IT (studio): WebStudioTest.simple failed requests at ~10002ms (client timeout); Jetty hang; rerun.
- itest.studio.repos race: two revisions share createdAt → history order flips (task_EPBDS-15439/.../500-verify); rerun.
- Sonar analysis job: jacoco report-aggregate `Unknown block type` on ITEST/server-core/target/jacoco.exec (overlapping artifact merge); rerun.
- SonarCloud gate on deletion-only PRs: New Code wider than the diff; pre-existing S2259/S6466 attributed to shifted lines; deterministic, no rerun; merged red before.
- Maven build extension archetype-packaging transiently unresolvable on one runner ("could not read 2 projects"); rerun.
- Job logs 404 while in_progress; the log API returns only the tail. CodeRabbit Docstring Coverage warning fires on deletion-only diffs; ignore.
- Rerun budget: 2 per check per SHA; reruns are not possible from this sandbox (no gh, no Actions write) — say so once and let the next push retry.

## Container facts

- No `gh` CLI: use the GitHub MCP tools (pull_request_read, update_pull_request, add_issue_comment, actions_list, get_job_logs).
- `git push --delete <branch>` is refused by the proxy (HTTP 403, "remote end hung up"); branch deletion is a human follow-up.
- Cold ~/.m2 at session start: the first `-T2` build needs the network (27 min wall clock with tests) and -Daether.syncContext.named.time=600, or resolution fails with "Could not acquire lock(s)"; never -o before that build.
- After a build failure, `mvn -rf :<module>` cannot resolve the banned siblings or the never-installed rules.test → rerun the whole `mvn install` (no clean, ~10 min warm).
- Container presets gpg.format=ssh, commit.gpgsign=true; JGit has no ssh signer → repository.git tests die and every module after it is skipped. Unset both **globally** (`git config --global --unset`) before the build; a local unset in the clone is not enough and env overrides do not reach JGit.
- The container rewrites ~/.gitconfig back to the Claude identity mid-session, silently reverting `git config --global user.*`. Set `git config --local user.*` in the clone instead (worktrees share it) and re-check `git log -1 --pretty='%an|%cn'` before every push, not only after the first commit.
- No locale set: run builds with LANG=C.UTF-8 (one archive test uses a non-ASCII fixture name).
- 4-core container: -T1C starves vitest (UserDetailsTab fails); use -T2 for whole-reactor builds when studio-ui tests run; never run npm in studio-ui while Maven runs.
- A `git rebase` during a running build is safe only while the reactor is far from the edited modules; otherwise the intermediate checkouts feed Maven a half-applied tree.
- Never `git switch --orphan` in the working tree while a build runs (it empties the tree): use a separate `git worktree add --detach` for the ledger branch.
- pmd-dist bin zip is not on Maven Central; download from github.com/pmd/pmd/releases (~130 MB) and run `bin/pmd check --file-list`.
- ITEST does run here despite no Docker: the Jetty-based itest.studio.* suites execute in a plain `mvn install`, so a whole-reactor build is the real CI gate. No Jira; sonarcloud.io dashboard 403 but api/issues/search reachable.
- npm install --package-lock-only strips libc metadata from 10 optional platform packages; edit package-lock.json by hand, verify with npm ci.
- opensaml-bom 5.2.3 lives only on build.shibboleth.net: if CONNECT 403 returns, stub an empty BOM in ~/.m2 and skip webstudio with -pl.
- Two runs of this routine can fire the same day: list open `dead-code/*` PRs and fetch `dead-code/ledger` again right before pushing anything.

## Exhausted veins

- studio-ui: package.json deps, 15 locale namespaces, enum members, Props members, public/ assets, module graph, 849 exports, CSS-in-JS keys, @ts-ignore, React imports.
- studio-ui exports used only in their own file (~36-70): live; dropping `export` is a visibility refactor, never a deletion.
- Legacy JS: 382 declarations in 38 files and every whole-file loader; only the four already removed and jQuery.sub (vendored) are dead.
- CSS selector tokens in all 17 stylesheets: only `.clickable`, `.scrollable` and `.dropdown-form` (all removed) dead outside vendored files and runtime-built names; inline styles all used.
- Tableeditor: every CSS/JS source in the concat lists; datepicker helpers vendored; webstudio .xhtml all reached except the two removed; ui:param and xmlns all used.
- HTML comments in .xhtml/.html: explanations only; f:facet names all standard RichFaces.
- Images: 683 tracked images outside ITEST all named as a whole word in a text file, except the 8 removed.
- Message bundles: openapi (625), messages (46), ValidationMessages (228) keys all reached by literal, `openl.error.<status>.` suffix, EL enum name or Bean Validation default, except the one already removed; sql-errors keyed by vendor error code at runtime.
- Config defaults: all 199 keys of the 10 openl-default.properties files are named outside their own file (Java @Value/getProperty, Docs guides, ITEST application.properties); none dead.
- Resource base-name scan (all `resources/` dirs outside ITEST/test-resources/archetypes): 45 zero-reference hits, all convention files.
- Spring XML beans (11 non-test files), component-scan entries (24), JSF registrations, web.xml filters/listeners: all alive except tableViewer (deferred).
- Maven: root/module properties, every profile, root pluginManagement, 177 managed artifact ids, 18 exclusions, plugin config, surefire sysprops, resource dirs: all alive; dependency:analyze-only remaining hits are providers/aggregators/processors/wars.
- Java PMD (UnusedAssignment, UnusedLocalVariable, UnusedPrivateField, UnusedPrivateMethod, UnusedFormalParameter) over all 4045 files: 46 hits, all FPs or deferred.
- Java identifier index: non-public methods with global count 1 in main sources = 2 (JAXB hook, @Bean); count-1 fields = 36, all Lombok accessors.
- ASM scan on main 5698aad6 (5155 classes): 32 hits; 4 removed, 28 catalogued FPs or deferred; nothing else non-public is unreferenced.
- ASM ctor scan on main ef74952e: 9 of 830 non-public parameterized ctors and 901 no-arg ones uninvoked; only the removed pair was dead, and the 11 production no-arg hits are all reflective or implicit.
- Java: protected members of final classes, public members of .internal. packages: none; pkg-private top-level (246) and nested (318) publics all called.
- Java: enum constants all alive except XlsProjectionType (deferred); classes with no bytecode reference (22) all reflection fixtures/inheritors/@Delegate excludes.
- Java: JavaDoc tags, @SuppressWarnings keys, bare super(), empty default ctors, UnnecessaryBooleanAssertion: done; UnnecessaryFullyQualifiedName/UselessParentheses are rewrites, not deletions. STUDIO type scan (532 non-webstudio + 876 webstudio types): every zero-reference hit is a JUnit class, a Spring bean or a published jar's public type (deferred); only UiConst was deletable.
- SonarCloud on main: S125 done (39 kept); S1128/S1116/S3985/S1596/css:S4658 zero; S1068/S1481/S1854/S1144/S2094/S1119/S3626 all FP or deferred.
- Test workbooks outside ITEST/test-resources/it (254): only 3 folder-loaded ones unmentioned; non-workbook test/ files (3) live.
- Commented-out code: Java main+test, CSS, JS done; XHTML/TS none. Dead suppressions: 137 undecidable (category unchecked).
- .gitignore/.gitconfig/.gitattributes/.editorconfig, package.json fields, eslint/vite/vitest config imports: done.
- DEMO/** assets and archetype scripts/ all referenced; Docs/** pages none orphaned (sidebar from site.pages, full-content search); Jekyll _includes/_layouts/_data all referenced.
- Static html/css/js outside webstudio (WSFrontend static/, DEMO/webapps/ROOT): covered by the CSS-rule and inline-style passes; studio-ui has no plain stylesheets.
- EPBDS-16560 residue (21 JSF beans, 7 .xhtml, response-monitor.js deleted): swept for Java types, CSS, JS, images,
  message bundles, common.js and studio-ui exports/locales; everything found is in #2101.
- EPBDS-16576 residue (5 JSF diff pages, 13 controllers, diff2html, legacyCompare.ts deleted): swept for Java types
  and members, .xhtml, CSS tokens, images, message keys, studio-ui locales and legacy JS; all of it is in #2104. The
  218 methods the deleted controllers called all keep other callers; the diff module's own types stay reachable.

## Human follow-ups

- `origin/main` e01088de is RED: itest.studio.repos WebStudioTest.repos fails 5 requests. EPBDS-16415 moved uploaded
  workbooks into a `rules/` folder and rewrote 21 fixtures under itest.studio/repos/test-resources to match, but missed
  task_EPBDS-16576-conflicts/020-compare/010-merge-side-branch.post.resp, which still expects
  `EPBDS-16576-Conflicts/Main.xlsx` where the server answers `.../rules/Main.xlsx`; the next request then 404s. Rail 5
  forbids this routine from editing ITEST fixtures, and no rerun fixes it — it needs the EPBDS-16415 author.
- Test bug: JAXRSOpenLServiceEnhancerTest.shouldAddApiResponsesIfOperationNotAnnotatedByApiResponses enhances the wrong fixture interface.
- Sonar: S2259 (NPE) in ComponentTypeArrayOpenClass.isAssignableFrom/isInstance (null-guard patch proposed in #2088), XlsBinder:523, ProjectModel:1214, TableEditorModel:106, TestDownloadController:144; S6466 CRITICAL WorkbookListener:273.
- site.webmanifest names `android-chrome-512x512.pngs` (trailing s): the 512px icon is unreachable; one-character bug.
- Policy: Jira prefix for sweep commits (maintainer view: exemption excludes production deletions); public-API removal needs a ticket.
- Confirm EPBDS-16309 authorises the OpenL2TextUtils removal (decision came from a sweep state file, not Jira).
- CI health: LockTest load tolerance; OracleRdbmsTest ORA-12516; itest.studio.repos createdAt tiebreaker; jacoco aggregate overlap; kafka-native:latest `setup` segfault (Pwd.getpwuid), which cost 3 CI attempts on one SHA and burns runner time on every PR.
- OverviewPanel.tsx floating promise into a state setter (~lines 799/1231) is a real test defect at any speed.
- Public unused members awaiting a decision: see Deferred findings. Dependency hygiene PR: declare commons-lang3 (openapi-parser, project.openapi, validation.openapi), groovy test, org.openl.rules.project.
- Delete the stale remote branches dead-code/uncalled-methods and dead-code/uncalled-internal-methods; push --delete is 403 from the sandbox, retried and still blocked.
- Trigger `Dead code sweep (openl-tablets)` runs on cron `17 */4 * * *` (six firings a day) while its prompt says daily; with every vein exhausted most firings only re-read the ledger.
- Three early ledger commits are authored `Claude <noreply@anthropic.com>` against the identity rule; the branch is never force-pushed, so they stay.

## Run log

- 2026-09-11 f: #2101 rebase-merged (393 deletions on main); ledger closed out, EPBDS-16576 left as the next vein.
- 2026-09-11 g: swept the EPBDS-16576 vein concurrently with another firing; it opened #2103 (subset).
- 2026-09-11 h: opened #2104 (4 commits, 13 files, 162 deletions, superset), closed #2103; found main red in ITEST.
