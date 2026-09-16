# Dead-code sweep ledger — openl-tablets

## Resume point

- Maintain PR #2119 (`dead-code/repo-sweep`) first; it carries one commit per change type 1, 2, 3 and 10.
- Next new work: change type 4 — rerun the reactor scan command (Method rules) and triage only the module-specific
  compile/runtime-scope "Unused declared" findings; root-inherited test/provided deps are noise.
- Then change type 5 (root `dependencyManagement` entries, see Deferred findings), 12 and 13.
- Sweep base: `origin/main` 737e6794be.

## Change-type queue

| # | Change type | Status |
|---|-------------|--------|
| 1 | Commented-out code (Java, CSS, JS, TS) | in PR #2119 (2026-09-16) |
| 2 | Never-read assignments, dead stores | in PR #2119 (2026-09-16) |
| 3 | Unused locals, private fields/methods/params | in PR #2119 (2026-09-16) |
| 4 | Unused Maven dependency declarations | in-progress: scan done, triage pending |
| 5 | Pom metadata: managed entries, exclusions, plugin config, properties | pending; properties vein exhausted |
| 6 | Redundant constructs, dead suppressions, VCS/build settings | pending |
| 7 | Unreferenced resources (descriptors, config files, images) | done: nothing unreferenced |
| 8 | CSS rules and inline styles | n/a: one CSS file left (DEMO), JSF layer is gone |
| 9 | Legacy JS functions and pages | n/a: two JS files left (rapi-doc vendor, DEMO) |
| 10 | i18n and message keys (studio-ui locales, Java bundles) | in PR #2119 (2026-09-16) |
| 11 | TypeScript exports, types, components, imports | done: only dead `export` modifiers, nothing to delete |
| 12 | Test fixtures: workbooks, utility classes, stub members | pending |
| 13 | Package-private/protected members and unreferenced internal classes | pending |

## Open PR

- Branch `dead-code/repo-sweep`, PR #2119, head 32bea2b6b9, 4 commits, 73 deleted lines.
- 135a53efae Remove commented-out code that no longer matches any live member
- 133786c4d6 Drop translation and OpenAPI description keys that no screen or endpoint reads
- 315203a96f Remove private table-counting helpers that the React editor migration left uncalled
- 32bea2b6b9 Remove a duplicate assignment of the service description in progress
- Four maintainer threads (keep the debug toggles) answered and resolved; none left open.

## Merged PRs

- None since the reset.

## Module coverage

- Types 1, 2, 3, 7, 10, 11 swept repo-wide; every other module has open work only under types 4, 5, 6, 12, 13.

## Deferred findings

- `DecisionTableBuilder.methodName` (DEV) is written by public `setMethodName` and never read — public API.
- `SimpleGroup.description` (org.openl.security) is written by constructor and public setter, never read — public API.
- `DebugChannel:51`, `DebugHookImpl:149-150` field stores overwritten before a read — trace debugger concurrency code, human review.
- `TestIntExpAddArray.testRemoveValue` is an empty test whose body is a block comment — remove whole method under type 12.
- Root `dependencyManagement` entries with no direct declaration anywhere: aspectjrt, aspectjweaver, swagger-models,
  swagger-core, swagger-parser-v2-converter, gson, jboss-logging, httpcore5, httpcore5-h2, httpclient, httpclient5,
  httpclient5-cache, testcontainers, nimbus-jose-jwt, jakarta.activation-api, spring-ldap-core, jetty-home, json-smart,
  netty-tcnative-boringssl-static, lz4-java, xmlsec, guava, commons-logging — they may pin transitive versions; prove
  each absent from `mvn dependency:tree` before removing (type 5).

## False-positive shapes

- PMD UnusedLocalVariable on a for-each variable or a try-with-resources variable — the syntax requires it.
- PMD UnusedAssignment on a field initializer whose constructor returns early on `null` — the default is the null path.
- PMD UnusedAssignment on a field read by another thread or through a callback during the next call
  (`ServiceManagerImpl.serviceDescriptionInProcess`, `DynamicPropertySource.settings` read by the resolver).
- PMD unused private members in test classes under `types/impl`, `types/java`, `serialization`, `org/openl/generated`
  — reflection fixtures that test what the type system must ignore.
- `y = (T) cast.convert(x)` in a test expecting ClassCastException — the assignment carries the cast.
- A regex for `/* ... */` blocks matches `/*` inside glob strings (`**/*.xlsx`) and text blocks.
- knip lists `src/api-docs.tsx` as an unused file — it is the second Vite entry (`api-docs.html`).
- knip "unused export" on a symbol used inside its own file — only the `export` keyword is dead, not a deletion.
- `dependency:analyze-only` reports the root-pom inherited deps (jspecify, lombok, log4j-to-slf4j, junit-jupiter,
  junit-pioneer, mockito-junit-jupiter) in every module — declared once in the root pom; ignore.
- A TODO/FIXME comment holding a code sketch documents an intention; only a comment whose code names a member that
  no longer exists is dead.
- SonarCloud counts a pre-existing issue as "new" when a deletion above it shifts its line (S6204 in
  `ServiceManagerImpl`); the Quality Gate still passes and a refactor is outside this routine — leave it.

## Method rules

- Search a translation key by full dotted path, by `ns:path`, and with `_one/_other/_zero` stripped; a template
  literal `t(\`a.b.${...}\`)` keeps every key under `a.b.`.
- Search a ValidationMessages key by its short form: strip `openl.error.` and `openl.error.<status>.`.
- openapi.properties keys are literal annotation values resolved by `getMessage(key)`; grep the exact key.
- Run PMD and dependency analysis in one reactor pass after the full build:
  `mvn -o test-compile pmd:pmd dependency:analyze-only -Dquick -DnoPerf -Dunpack-webapp.skip=true -fae -T1C`;
  without a compile phase in the same session sibling SNAPSHOT modules do not resolve.
- PMD config needs `includeTests=true`; ignore every hit under `target/generated-sources`.
- Never edit a file while a Maven build runs — Error Prone crashes on a source changed mid-compilation.
- Prove a private member dead with the module's own tests; only a non-private removal needs dependents rebuilt.

## Keep-list

- Commented-out debug toggles are maintained conveniences, never dead: the OpenAPI bulk-update block in ITEST
  `HttpClient` and the `files = new File[] {...}` single-folder toggles in `RulesInFolderTestRunner`,
  `OpenAPIGenerationTest`, `OpenAPIProjectCreatorTest` (maintainer decision on PR #2119).
- Convention-loaded resources: Flyway SQL under `db/flyway`, `openl-db-repository-<dialect>.properties`,
  `archetype-metadata.xml`, `META-INF/cxf/org.apache.cxf.Logger`, `META-INF/spring.factories`,
  `META-INF/openl/extension-*-beans.xml`.
- `jakarta.validation.constraints.*.message` keys — Bean Validation message overrides.
- Root pom properties `project.build.sourceEncoding`, `maven.install.skip`, `lombok.delombok.skip` — plugin
  parameters, referenced without `${}`.
- pluginManagement entries for maven-site/deploy/release/archetype plugins and `error_prone_core`/`nullaway` —
  lifecycle plugins and annotation-processor paths.

## CI flakes

- None recorded yet.

## Container facts

- `gh` is absent — use the GitHub MCP tools for PRs, comments and checks.
- The sandbox rewrites `~/.gitconfig` user.name to `Claude` between commands — pass `GIT_AUTHOR_*` and
  `GIT_COMMITTER_*` inline on every `git commit` and `git rebase`, then verify with `git log -1 --pretty='%an|%cn'`.
- `~/.gitconfig` sets `commit.gpgsign=true` with `gpg.format=ssh`; JGit tests fail with "No signer for ssh
  signatures" — run `git config --global commit.gpgsign false` before Maven and restore `true` afterwards.
- The JVM default charset is not UTF-8; `ZipArchiveValidatorTest` fails on a non-ASCII file name — prefix every
  Maven command with `LC_ALL=C.UTF-8`.
- `~/.m2` starts empty; `mvn clean install -Dquick -DnoPerf -T1C` takes about 16 min with network. `-rf` resume
  cannot resolve earlier sibling modules — always run the whole reactor. `-o` works after the first full build.
- knip runs with `npx --yes knip@latest --no-progress --reporter compact` in `STUDIO/studio-ui`.

## Exhausted veins

- Commented-out code: `//` lines ending in `; { }`, `/* */` blocks with statements, over every `.java/.ts/.tsx/.js/.css`;
  commented XML elements in poms; commented keys in `.properties`/`.yaml` — repo-wide.
- Resource files under `*/resources/**`, `*/webapp/**`, `*/static/**` by base-name search — none unreferenced.
- studio-ui locale keys (1598), `openapi.properties` and `ValidationMessages.properties` keys — all remaining are used.
- knip unused exports and types in studio-ui — none deletable.
- PMD UnusedAssignment, UnusedLocalVariable, UnusedPrivateField, UnusedPrivateMethod, UnusedFormalParameter over
  every module including tests.
- Root pom `<properties>` without a `${}` reference — none removable.

## Human follow-ups

- Decide whether `DecisionTableBuilder.setMethodName` and `SimpleGroup.setDescription` (write-only) may go.
- `GitRepositoryTest` and `ZipArchiveValidatorTest` depend on the developer's git signing config and default charset.

## Run log

- 2026-09-16 g: ledger reset to zero on the owner's instruction; full re-sweep started from `origin/main` 737e6794be.
- 2026-09-16 h: types 1, 2, 3, 10 swept and committed, 7, 11 found empty; PR #2119 opened with 4 commits; maintainer kept four debug toggles.
