# Dead-code sweep ledger — openl-tablets

## Resume point

- Swept head is `origin/main` ff879e6652. EPBDS-16599 (JSF editor → React) landed 23 commits, 1063 files,
  -57001 lines, and was the vein the previous eight idle runs were waiting for.
- The merge closed the JSF veins for good, not merely swept them: zero `.xhtml`, zero legacy `.js`/`.css`/images
  and zero JSF/RichFaces coordinates remain anywhere in the repository. Never re-derive those categories.
- Every change type is swept at this head and every detector rerun; the repository is clean again. First command of
  a run is `git rev-list --count ff879e6652..origin/main`: 0 means stop after maintaining PR #2118.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS) | done 2026-09-09; 39 prose/TODO/reasoned blocks kept |
| 2 | Never-read assignments, dead stores | done 2026-09-16 at ff879e6 (needRedirect) |
| 3 | Unused locals, private fields/methods/params | done 2026-09-16 at ff879e6 (deploymentManager) |
| 4 | Unused Maven dependency declarations | done 2026-09-07; the rest are providers |
| 5 | Pom metadata: managed entries, exclusions, plugin config, properties | done 2026-09-07 |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | done 2026-09-07 |
| 7 | Unreferenced resources (descriptors, TLD, config files, images) | closed by the React merge |
| 8 | CSS rules (legacy webstudio, tableeditor, DEMO, inline) | closed by the React merge |
| 9 | Legacy JS functions and .xhtml pages | closed by the React merge |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | done 2026-09-11 g; 66 candidates all template-composed |
| 11 | TypeScript exports, types, components, imports | done 2026-09-16 at ff879e6; tsc clean |
| 12 | Test fixtures: workbooks, utility classes, stub members | done 2026-09-09 |
| 13 | Package-private/protected members and unreferenced internal classes | done 2026-09-16 at ff879e6 |

## Open PR

- `dead-code/react-editor-residue`, PR #2118, head 703ec5ab8c, both commits stranded by EPBDS-16599.
- Remove the webstudio classes … unreferenced: IProjectTypes, ObjectRegistry, ListItem (76 lines).
- Remove the WebStudio and ProjectModel members … unreferenced: 21 methods, 2 fields, the DeploymentManager
  injection and its one call site and test (281 lines).

## Merged PRs

- 24 sweep PRs merged (1906-2109); what each removed is covered by Exhausted veins. A repo-wide single-type commit is
  accepted as-is (2101 and 2104 were rebase-merged, one commit per change type onto main).
- A small, fully green, evidence-backed sweep PR merges unreviewed on its own (2109 in ~22h): never nudge, never pad one.
- 2104 merged with `IT (studio)` still red: a failure proven to be `main`'s, with a standing-down comment naming it,
  does not block a merge. Ordering a referrer's commit before the resources only it reached survived review untouched.
- GitHub deletes a merged PR's branch by itself; CodeRabbit reviews at most 2 pull requests an hour, silently skipping the rest.

## Module coverage

- All 86 reactor modules, studio-ui, Docs, DEMO and archetypes are swept for every change type; only code merged after
  a pass can yield again, and a big feature merge is the reliable source (16599 stranded 357 lines, 16560 393, 16576 162).

## Deferred findings

- A feature merge deletes its own referrers but not what they reached: after EPBDS-16599 the residue was three whole
  types plus 21 members on the surviving session beans. Scan a big merge's deleted imports first — highest yield.
- CellFont (DEV): public, named by no other file at all. IGridSelector and RegionGridSelector name only each other,
  so the pair is dead together; all are public in a published jar.
- Unreferenced once EPBDS-16576 retired the JSF diff UI, but public in published jars: EmptyCell (DEV).
- OpenLServiceFactoryBean.setProxyInterface: @Deprecated public, unreferenced; binary-compatibility break.
- Module.wildcardName: written by 2 callers via Lombok setter, read by none; needs cross-module call-site edits. DecisionTableBuilder.methodName + setMethodName + its TableSyntaxNodeDispatcherBuilder call: inert chain, public DEV API.
- SimpleGroup.description: public setter and 3-arg ctor parameter, read nowhere. XlsProjectionType GRID..CELL_FONT (9 constants, `// TODO do we need the rest?`): public enum, no values()/valueOf use.
- IConditionEvaluator.DECORATOR_CONDITION_PRIORITY, IColorFilter.COLOR_NAMES/COLORS: unread public interface constants; IBoundModuleNode, RuleServicePublisherMapper, HistoryLog: unreferenced public types.
- ChoicePointLabel: public in DEV constrainer, its only ctor package-private and uninvoked, so the Constrainer/Failure/GoalOr/GoalStack members typed on it always hold null; removal edits public signatures.
- Extension surface, no caller: protected abstract ADtColumnsDefinitionTableBoundNode.isReturns (3 overrides) and AbstractOpenlTableExporter.getExcelSheetName (4); protected TableBuilder.getGridModel in a public non-final DEV class.
- Same-class or super-forwarding callers only: DependencyOpenClass.getTypes/findType, CastingCustomSpreadsheetResultField.getDeclaringClass, SidExistsValidator.isValid, MethodUtil.printMethod(IOpenMethodHeader,StringBuilder,Function), SpreadsheetCell.isValueCell.
- Unused class type parameters on public types: ReturnOperation, IStorage, ProjectService.
- MappedRepository.create(Repository,String): the 2-arg overload now forwards `false`; only its own tests call it.
- Sonar S1130 (129) and S1172 (228) are closed, not pending: dropping an unthrown `throws` or an unused parameter is a
  signature edit, never a deletion. 115 S1130 hits are package-private and 111 sit in tests, where `throws Exception` on
  a JUnit method is deliberate style; the public/protected ones need a ticket. Do not re-derive either rule.
- MergeResult record: `status` component ignored by the compact constructor; removing it changes a public record signature.
- ServiceManagerImpl.deploy: second `serviceDescriptionInProcess` write is a no-op only if createService cannot re-enter deploy.
- GitRepository visitors `result = null` in catch: dead store whose removal leaves an empty catch (Sonar S108); keep.
- ExpressionFactoryImpl.findExpression: compiled out by the constant toggle `_getFromCache = false` (JLS constant variable); a cache switch.
- ITEST HttpData.writeBodyTo: only a commented-out call in HttpClient, but webstudio AGENTS.md documents toggling it to recapture fixtures.
- JavaCC-generated BExGrammarTokenManager/SimpleCharStream carry unreferenced members; generated sources are never edited.
- Vendored zero-hit tokens, kept because a vendored file goes whole or not at all: jQuery.sub in jquery-back-compat.js;
  bootstrap.min.css and diff2html.css tokens. All of these live in studio-ui now; the webstudio copies went with the merge.
- Transitive providers, not dead: jaxb-runtime in workspace, spring-core/spring-security-core in security.standalone, kafka-clients in ruleservice.kafka.
- Test-jar executions in ruleservice and ruleservice.deployer include only org/openl/rules/ruleservice/test/* (absent) → publish empty jars; maven-plugin-plugin's reporting entry produces no report, and site.xml links plugin-info.html and 7 *-mojo.html nobody builds.
- Not deletions but fixes: eslint react-hooks plugin registered with no rule enabled; RulesUtilsTest @SuppressWarnings("deprecated") misspelled.
- Not dead, just repetitive: 47 dependency `version` elements repeating the managed version (44 in jacoco-report) are DRY; 153 `/* (non-Javadoc) @see */` markers beside @Override are comment churn; AzureBlobRepository `final` in try-with-resources is author style.
- Dev toggles: `// files = new File[]{...}` in RulesInFolderTestRunner, OpenAPIGenerationTest, OpenAPIProjectCreatorTest; ITEST HttpClient bulk rewrite.
- DEV/org.openl.rules/doc/Table Properties Design Points.docx: unreferenced historical design material.
- Docs/architecture/dependencies.md row for org.openl.rules.diff says `commons`, pom says org.openl.rules.

## False-positive shapes

- Searching an accessor by its property name can also hide a real finding: `setProjectVersion` was masked because the
  bare word `projectVersion` occurs elsewhere. Search the method name and the property form separately, not as one
  alternation, and judge a zero on the method name on its own.
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
- A zero-reference type scan over a Spring module is almost all beans: at ff879e6 51 webstudio types had no external
  reference and 48 carried @RestController/@Configuration/@Component/@Service. Filter by annotation before reading any.
- Uninvoked ctor FPs: Jackson-bound records (readValue/TypeReference/getForObject/awaitMatching), JAXB XmlAdapter, Jackson MixIn, @Component/@Conditional, ctor params carrying @Autowired/@Value, OpenL String-constructor datatypes, JUnit classes.
- A private or protected no-arg ctor is the anti-instantiation idiom, never a finding; an implicit default ctor has no source line to delete; a record's canonical ctor cannot be removed.
- Reflection fixtures: YamlMapperFactoryTest MyBean, FormatterTest, ModuleTest, RulesUtilsTest, JavaOpenClassTest beans, JsonUtilsTest.BindingClasses.
- Test types alive without reference: @Test classes, abstract-base inheritors, TestRunner* Spring configs, WizardUtilsTest dir-scan beans, JMH.
- Test fixtures with deliberately private members: epbds6830 BeanA.getAB, AOpenClassTest.C.getC, JavaOpenClassTest.BeanA.gg (asserted by name).
- Velocity: gen wrappers reached as `$wrapper.asList()` or `$wrapper.PropertyType` → grep .vm for both spellings.
- i18next: `_one`/`_other` plural suffixes; template keys t(`browser.${id}_confirm`), role., debug.status., notifications.${kind}_deleted, create_table_modal.types./.blocked., update_project_modal., fill_preview.state., tests.${kind}/tests.no_${kind}.
- Transformed keys: ValidationMessages `openl.error.<status>.<code>` built by RestRuntimeException.getErrorCode; only the suffix is in Java.
- A React `data-testid` is not a class: a CSS token surviving only as a test id (param-tree) is dead CSS.
- A React rewrite copies a legacy helper rather than calling it (getDb, isCollection): a same-named method in another file is not a caller — check the qualifier.
- PMD UnusedAssignment blind spots: constructor early return (CellStyle), try/catch pairs (GitRepository), for-each counting variable (RulesUtils.getValues), and a value read back elsewhere — through a callback (DynamicPropertySource.settings via resolver) or published before blocking (DebugChannel.status, DebugHookImpl.pendingDispatch, ServiceManagerImpl).
- PMD UnusedPrivateMethod without aux classpath: method references (XlsBinder::addBindingContextError), overloads by argument type, lambda/Supplier overloads (ProjectCreationService, UserWorkspaceImpl, MethodUnreachableStatementValidator).
- Unused-local FPs: try-with-resources vars (WebSocketAuthTest.stomp, ExtensionsConfigurationTest.context); null before System.gc(); assign before fail() hosting a cast.
- An unread parameter that precedes a read one (mock callback `(url, options)`) cannot be dropped without changing arity; renaming to `_x` is a refactor, so it is never a finding.
- `var ignored = executor.submit(...)` silences Error Prone FutureReturnValueIgnored (Sonar S1481/S1854 FP).
- UnnecessaryCast FP: cast gives `var` its type; UnnecessaryBoxing FP: pins an overload in Operators; varargs `new Object[]{null}`.
- javac caps warnings at 100: raise -Xmaxwarns before judging @SuppressWarnings("deprecation") dead.
- ESLint no-unused-vars never flags an unused `React` default import under react-jsx → use tsc --noUnusedLocals; ESLint lints only ./src.
- dependency:analyze FPs: annotation processors, CLASS-retention (jspecify), aggregators (junit-jupiter, swagger-parser), runtime providers, wars.
- openl-maven-plugin reads its own dependency list by coordinate string (VerifyMojo.getJars) → all its "unused" deps are live.
- Dead vs live `exclusion`: the reactor tree hides duplicates; resolve the dependency alone with dependency:tree -Dverbose in a scratch pom.
- RedundantFieldInitializer unsafe when a superclass constructor makes a virtual call (ComponentOpenClass/ADynamicClass addField).
- Sonar S3626/UnnecessaryReturn: `synchronized(this){return;}` and symmetric chain arms are load-bearing; drain loops are not empty statements.
- CodeRabbit's walkthrough restates a deletion-only diff as "Removed Capabilities" (and a comment-only one as a
  behavior change) even while its own review reports no actionable comments and minimal merge risk. Correct it once,
  with `git diff --numstat` (0 insertions) and the surviving path for each named feature, so no reviewer reads it as lost work.
- Dead suppression check: remove all, recompile; TS @ts-ignore mostly real (exactOptionalPropertyTypes antd conflicts).
- Resource base names that match nothing but are alive by convention: Flyway `db/flyway/**`, spring.factories, extension-*.xml, archetype-metadata.xml, META-INF/cxf/*.

## Method rules

- Prove non-reference with `grep -rIwF <name>` over all file types excluding target/ node_modules/ .git/ studio-ui/coverage studio-ui/dist, plus `grep -ra` for binaries, plus accessor forms and the extension-less stem.
- Normalise paths before excluding a candidate's own file from its hit list: `find` yields `STUDIO/...` and `grep -r .`
  yields `./STUDIO/...`, so an un-normalised `grep -vxF` makes every file count itself and the scan reports zero findings.
- Never truncate a proof grep with `head`: a listing cut short once hid org.openl.rules.diff's live consumer and nearly cost a wrong deletion. Count first, print second.
- Removing members is a fixpoint, not a pass: each removal can strand its callee, its field and its constructor
  parameter. Re-run the scan until it reports nothing, then check the fields and imports the last round orphaned.
- Separate a leaf from a cluster by counting the name inside its own file: one hit is the declaration alone and is
  safe; more means internal callers, and those are dead only if every one of them is dead too (overload pairs usually are).
- The clone is shallow, so `git log -S` lists boundary commits that touch nothing: confirm a removal by reading the diff of the newest hit (`git show <sha> | grep '^-.*<token>'`), never by the list alone.
- Search workbooks: `unzip -p *.xlsx | grep -wF`, .xls as raw bytes; rule tables can name Java methods. Include the maven-plugin `it/` sources in every search.
- Build an identifier-frequency index over all tracked text files: global count 1 = declaration only; scope test types to their own module.
- A keep-list mechanism can itself die: the images/diff icons were "referenced by literal path" only from UiConst, so they fell with it. Re-check a keep-listed referrer before trusting the keep.
- Scan a module's types for zero external references, then filter by annotation; the unannotated remainder is the finding.
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
- Java gate: never -DskipTests (skips test compile); ITEST needs -Pitest -DskipTestsForQuick=false; full `mvn clean install -Dquick -DnoPerf -T2` before push.
- Verify a deletion with `clean install`, never a warm `install`: stale target/classes from before the removal keep a
  dependent module compiling against a class the branch no longer has.
- Regroup: one commit per change type per repo-wide pass; fold fixes with --fixup + autosquash; rerun detectors after a removal, because removing a param can leave a caller's param dead.
- Order the commits so a referrer dies before the resources only it kept alive; an explicit `git rebase -i` todo through GIT_SEQUENCE_EDITOR both reorders and rewords in one pass.
- Compare the PR run's job list with the base commit's own run: a job red in both is pre-existing, not yours.
- A "used undeclared" dependency is an addition, not a deletion: it belongs to the separate hygiene PR, never to this sweep.

## Keep-list

- ant-* antd runtime CSS tokens in studio-ui are unprovable; vendored libraries go whole or not at all (diff2html, rapi-doc, jquery shims that survive under studio-ui).
- Convention files: META-INF/services/**, META-INF/openl/extension-*.xml (@ImportResource classpath*), cache2k.xml, WEB-INF/classes/logging.properties (Tomcat JULI), simplelogger.properties.
- Convention files: openl-db-repository-*.properties ("/openl-db-repository-" + databaseCode), META-INF/io/opentelemetry/**, archetype-metadata.xml, site.xml, CITATION.cff, Flyway db/flyway/** + placeholders.properties.
- Property loaders: DefaultPropertySource → openl-default.properties only; ApplicationPropertySource → application*.properties + {appName}.properties.
- web.xml still registers CorsFilter, ForwardedFilter, SecurityFilter, SessionTimeoutFilter, ChangeOriginFilter,
  SpringInitializer, SessionListener and RequestContextListener by class name: they have no Java caller and stay.
- The webstudio session beans WebStudio, ProjectModel and RecentlyVisitedTables survive the React migration and are
  built in ServiceApiConfig; they are reached from org/openl/studio REST services, so judge their members one by one.
- SystemValuesManager and its ISystemValue/CurrentUserValue/CurrentDateValue package are reached from
  SystemPropertiesService, not from the retired JSF pages.
- Code-generator templates rewrite only the INSERT block of DefaultTablePropertiesSorter and DefaultPropertiesContextMatcher.
- Test classes are invisible cross-module unless the module publishes a test-jar: only org.openl.rules.ruleservice and ruleservice.deployer do.
- DEV/org.openl.rules.test is never installed (maven.install.skip) → must be in the reactor; bare -pl on its consumers fails.
- DEV/org.openl.rules.gen: pom packaging + maven.deploy.skip → its public members are not published API.
- Public members of published DEV/**, STUDIO, WSFrontend jar artifacts stay; `.impl.`/`.internal.` packages are internal.
- The org.openl.rules.diff module stays whole: org.openl.studio.compare reaches all 23 of its types through XlsDiff2 and DiffTreeBuilder2.
- The CacheAndWriteOutputStream fork stays; its Javadoc records why it differs from the upstream class.
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
- Icons by literal path (rules-tree, site.webmanifest); ITEST 001-Get-Static-CSS asserts only status/content-type of common.css.
- War reachability is WEB-INF/lib, not compile: repositories are instantiated reflectively by class name from production-repository.factory.
- SLF4J bridge log4j-slf4j2-impl runtime scope pinned by Log4jRoutingTest; swagger-core-jakarta is the deliberate substitute (root excludes swagger-core).
- The `lz4.version` property and its `at.yawk.lz4:lz4-java` managed entry are a CVE pin overriding Kafka's transitive version; the pom comment states when it may go.

## CI flakes

- LockTest.testSimultaneousMultiThreadsWithWaiting (STUDIO repository): `expected <800> but was <79x>` under load; stabilized on main; rerun once.
- studio-ui vitest CPU starvation under -T1C: OverviewPanel.test.tsx (15000ms timeout, act() warning via
  vitest-fail-on-console) and UserDatailsTab.test.tsx findByText timeout when the run takes ~680s; rerun once.
- OpenLTableLogicTest.detectsErrorsInRulesTestedByTable: `expected true was false`; getMethod right after async setModuleInfo compile; rerun once.
- IT (services-data): apache/kafka-native:latest segfaults in its `setup` entrypoint (Pwd.getpwuid on `user.name`),
  so the error reads `Timed out waiting for ... RECOVERY to RUNNING`; random module per attempt, budget 3 reruns.
- IT (studio-acl): OracleRdbmsTest upgrade `Failed requests expected <0> but was <N>` with ORA-12516 (also on main), or
  a testcontainers/ryuk pull failure erroring all 4 variants at upgrade:53 (runner degraded); rerun once either way.
- IT (studio): WebStudioTest.simple failed requests at ~10002ms (client timeout); Jetty hang; rerun.
- itest.studio.repos race: two revisions share createdAt → history order flips (task_EPBDS-15439/.../500-verify); rerun.
- Sonar analysis job: jacoco report-aggregate `Unknown block type` on ITEST/server-core/target/jacoco.exec (overlapping artifact merge); rerun.
- SonarCloud gate on deletion-only PRs: New Code wider than the diff; pre-existing S2259/S6466 attributed to shifted lines; deterministic, no rerun; merged red before. A diff confined to test files passes it clean (#2109, 0 new issues), so the gate only bites when production lines shift.
- Maven build extension archetype-packaging transiently unresolvable on one runner ("could not read 2 projects"); rerun.

## Container facts

- Never edit the working tree while the reactor build runs: Maven compiles whatever is on disk when it reaches a
  module, so an edit made mid-run surfaces as a compile error in a module you have not finished, and reads as main's.
- End a build command with the mvn call or capture `${PIPESTATUS}`: a trailing `tail`/`echo` returns 0 and a BUILD
  FAILURE is reported as success.
- No `gh` CLI: use GitHub MCP (pull_request_read, update_pull_request, add_issue_comment, actions_list, get_job_logs);
  job logs 404 while in_progress and return only the tail. No Actions-write tool: a CI rerun cannot be triggered here.
- Cold ~/.m2 at session start: the first `-T2` build needs the network (15-27 min wall clock with tests) and -Daether.syncContext.named.time=600, or resolution fails with "Could not acquire lock(s)"; never -o before that build.
- After a build failure, `mvn -rf :<module>` cannot resolve the banned siblings or the never-installed rules.test → rerun the whole `mvn install` (no clean, ~10 min warm).
- Container presets gpg.format=ssh, commit.gpgsign=true; JGit has no ssh signer → repository.git tests die and every module after it is skipped. Unset both **globally** (`git config --global --unset`) before the build; a local unset in the clone is not enough and env overrides do not reach JGit.
- The container rewrites ~/.gitconfig back to the Claude identity mid-session, silently reverting `git config --global user.*`. Set `git config --local user.*` in the clone instead (worktrees share it) and re-check `git log -1 --pretty='%an|%cn'` before every push, not only after the first commit.
- No locale set: run builds with LANG=C.UTF-8 (one archive test uses a non-ASCII fixture name).
- 4-core container: -T1C starves vitest (UserDetailsTab fails); use -T2 for whole-reactor builds when studio-ui tests run; never run npm in studio-ui while Maven runs.
- Never `git switch --orphan` in the working tree while a build runs (it empties the tree): use a separate `git worktree add --detach` for the ledger branch.
- pmd-dist bin zip is not on Maven Central; download from github.com/pmd/pmd/releases (~130 MB) and run `bin/pmd check --file-list`.
- ITEST does run here despite no Docker: the Jetty-based itest.studio.* suites execute in a plain `mvn install`, so a whole-reactor build is the real CI gate. No Jira; sonarcloud.io dashboard 403 but api/issues/search reachable.
- npm install --package-lock-only strips libc metadata from 10 optional platform packages; edit package-lock.json by hand, verify with npm ci.
- opensaml-bom 5.2.3 lives only on build.shibboleth.net: if CONNECT 403 returns, stub an empty BOM in ~/.m2 and skip webstudio with -pl.
- Trigger fires on cron `17 */4 * * *` (six runs a day though the prompt says daily), so two can overlap: list open `dead-code/*` PRs and re-fetch `dead-code/ledger` right before pushing anything.

## Exhausted veins

- studio-ui: package.json deps, 15 locale namespaces, enum members, Props members, public/ assets, module graph, 849 exports, CSS-in-JS keys, @ts-ignore.
- Unused React default imports re-open with every new .tsx: `tsc --noEmit --noUnusedLocals` over src is the whole scan (5 of 64 dead at 9a32b54, removed in #2109); it also covers every other unused import and local in one pass.
- studio-ui exports used only in their own file (~36-70): live; dropping `export` is a visibility refactor, never a deletion.
- studio-ui at ff879e6: `tsc --noEmit --noUnusedLocals` exits clean. Adding --noUnusedParameters reports only the two
  known FPs (a positional `Array.from` mapper arg, a mock `(url, options)` callback) — do not add that flag again.
- tableeditor's 12 types with no outside user all have intra-module referrers: the cell-editor cluster is internal
  machinery whose entry points webstudio still calls. Settled, not a vein.
- At ff879e6: the 154 org.openl types the merge's deleted files imported, all 858 webstudio main types, and every
  public/protected member of the legacy org/openl/rules/ui package. Everything left there is a Spring bean or live.
- Message bundles: openapi (625), messages (46), ValidationMessages (228) keys all reached by literal, `openl.error.<status>.` suffix, EL enum name or Bean Validation default, except the one already removed; sql-errors keyed by vendor error code at runtime.
- Config defaults: all 199 keys of the 10 openl-default.properties files are named outside their own file (Java @Value/getProperty, Docs guides, ITEST application.properties); none dead.
- Resource base-name scan (all `resources/` dirs outside ITEST/test-resources/archetypes): 45 zero-reference hits, all convention files.
- Spring XML beans (11 non-test files), component-scan entries (24), web.xml filters/listeners: all alive.
- Maven: root/module properties, every profile, root pluginManagement, 177 managed artifact ids, 18 exclusions, plugin config, surefire sysprops, resource dirs: all alive; dependency:analyze-only remaining hits are providers/aggregators/processors/wars.
- Java PMD (UnusedAssignment, UnusedLocalVariable, UnusedPrivateField, UnusedPrivateMethod, UnusedFormalParameter) over all 4045 files: 46 hits, all FPs or deferred.
- Java identifier index: non-public methods with global count 1 in main sources = 2 (JAXB hook, @Bean); count-1 fields = 36, all Lombok accessors.
- ASM scan on main 5698aad6 (5155 classes): 32 hits; 4 removed, 28 catalogued FPs or deferred; nothing else non-public is unreferenced.
- ASM ctor scan on main ef74952e: 9 of 830 non-public parameterized ctors and 901 no-arg ones uninvoked; only the removed pair was dead, and the 11 production no-arg hits are all reflective or implicit.
- Java: protected members of final classes, public members of .internal. packages: none; pkg-private top-level (246) and nested (318) publics all called.
- Java: enum constants all alive except XlsProjectionType (deferred); classes with no bytecode reference (22) all reflection fixtures/inheritors/@Delegate excludes.
- Java: JavaDoc tags, @SuppressWarnings keys, bare super(), empty default ctors, UnnecessaryBooleanAssertion: done; UnnecessaryFullyQualifiedName/UselessParentheses are rewrites, not deletions. STUDIO type scan (532 non-webstudio + 876 webstudio types): every zero-reference hit is a JUnit class, a Spring bean or a published jar's public type (deferred).
- SonarCloud on main: S125 done (39 kept). Its 254 still-open hits are not a vein — 173 are constrainer Javadoc prose
  (the class docs embed pseudo-code) and the rest are stale line ranges; only 3 vestigial `// throws Failure` trailing
  comments exist repo-wide, kept as signature history. Do not reopen S125 on the raw count.
- SonarCloud on main: S1128/S1116/S3985/S1596/css:S4658 zero; S1068/S1481/S1854/S1144/S2094/S1119/S3626 all FP or deferred.
- Test workbooks outside ITEST/test-resources/it (254): only 3 folder-loaded ones unmentioned; non-workbook test/ files (3) live.
- Commented-out code: Java main+test, CSS, JS done; XHTML/TS none. Dead suppressions: 137 undecidable (category unchecked).
- .gitignore/.gitconfig/.gitattributes/.editorconfig, package.json fields, eslint/vite/vitest config imports: done.
- DEMO/** assets and archetype scripts/ all referenced; Docs/** pages none orphaned (sidebar from site.pages, full-content search); Jekyll _includes/_layouts/_data all referenced.
- EPBDS-16560, EPBDS-16576 and EPBDS-16599 residue: all swept for every change type.

## Human follow-ups

- `npx eslint ./src` fails on main with 15 errors (object-curly-spacing in migration.ts and 4 test files) and 2
  perfectionist warnings; CI never runs it, and fixing formatting is outside a deletion-only sweep.
- Docs/analysis/studio-wsfrontend-util-overview.md is stale after the React merge: it still describes WebContext,
  OpenLFilter, RuleServiceDeployer, RuleServiceManager and ServletUtils, none of which exist. Rewriting prose is not a deletion.
- Test bug: JAXRSOpenLServiceEnhancerTest.shouldAddApiResponsesIfOperationNotAnnotatedByApiResponses enhances the wrong fixture interface.
- Sonar: S2259 (NPE) in ComponentTypeArrayOpenClass.isAssignableFrom/isInstance (null-guard patch proposed in #2088), XlsBinder:523, ProjectModel:1214, TableEditorModel:106, TestDownloadController:144; S6466 CRITICAL WorkbookListener:273.
- site.webmanifest names `android-chrome-512x512.pngs` (trailing s): the 512px icon is unreachable; one-character bug.
- Policy: Jira prefix for sweep commits (maintainer view: exemption excludes production deletions); public-API removal needs a ticket.
- Confirm EPBDS-16309 authorises the OpenL2TextUtils removal (decision came from a sweep state file, not Jira).
- CI health: every entry under CI flakes deserves a real fix; the kafka-native `setup` segfault is the costliest.
- OverviewPanel.tsx floating promise into a state setter (~lines 799/1231) is a real test defect at any speed.
- Dependency hygiene PR (additions, never this sweep): declare commons-lang3 (openapi-parser, project.openapi,
  validation.openapi), groovy test, org.openl.rules.project, spring-core in ruleservice.ws.common (via rules.jackson).
- Three stale `dead-code/*` branches await a human delete: `push --delete` is 403 here, no MCP tool — never probe.

## Run log

- 2026-09-16 c: main unmoved (0 commits since the swept head), #2105 still draft; no detector rerun; at 289.
- 2026-09-16 d: main still unmoved, no `dead-code/*` PR open, #2105 still draft; resume point and run log only; at 288.
- 2026-09-16 e: EPBDS-16599 merged; swept its residue into PR #2118 (2 commits, 357 lines); all detectors rerun clean; at 297.
