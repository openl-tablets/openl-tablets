# OpenL Tablets — Agent Instructions

OpenL Tablets is a business rules engine that compiles Excel spreadsheets into executable Java via runtime bytecode
generation.

## Strict Rules (**MUST**. No exceptions.)

- Check folder-specific `AGENTS.md` hierarchically before modifying files in a folder.
- **Every change of functionality ships in one commit together with its tests and its documentation update** — never
  as separate follow-up commits.
- Run tests after changes. New or changed Java code keeps ≥80% line coverage on the diff (see Build and Verify).
- Execute `mvn validate -N` after changes and before committing to ensure all files are formatted correctly.
- Follow `.editorconfig` formatting (LF endings, 4-space indent for Java/XML, 120 char line length).
- Use correct casing: **OpenL**, **OpenL Studio**, **OpenL Rule Services**, **OpenL Tablets**.
- Use modern Java 21 syntax (`var`, `record`, `sealed`, `switch` expressions, record patterns, text blocks) and
  features (Virtual Threads, Sequenced Collections, new String, Collections and IO/NIO methods).
- Use Lombok wherever it removes hand-written code: `@RequiredArgsConstructor` for constructor injection,
  `@Getter`/`@Setter` for accessors, `@Slf4j` for loggers, etc.
- When constructor injection needs `@Qualifier` or `@Value`, put the annotation on the **field** — the root
  `lombok.config` lists both as `copyableAnnotations`, so Lombok copies them onto the generated constructor parameter.
- Use JSpecify annotations (`@NullMarked` on packages; `@Nullable`/`@NonNull` on all reference types).
- Never use deprecated APIs — migrate to alternatives.
- Follow **KISS**, **YAGNI**, and **DRY** — choose the simplest solution that works, build only what a current
  requirement needs, and factor out duplication rather than copy it.
- Keep methods compact and single-purpose; use **at most one** `break`/`continue` per loop (Sonar `java:S135`) —
  extract per-iteration filtering into a helper that returns a value or flag instead of stacking guard `continue`s.
- Use the narrowest visibility — prefer `private`, widen only when something outside genuinely needs it. OpenL rule
  interfaces and data beans referenced by generated proxies stay `public`.
- Follow JUnit 5 best practices: test classes and their `@Test`/lifecycle methods are package-private, never `public`
  (Sonar `java:S5786`); a cross-package abstract base or fixture stays `public` with `protected` lifecycle methods.
- New file names: ASCII alphanumeric plus `-_.` only, no spaces. Existing file names with spaces and `,+%$#` symbols
  exist for test purposes — never rename them during refactoring.
- No HTML in Markdown when an equivalent exists (see Markdown Rules).

## Repository Layout

Multi-module Maven project. The version inherits from the root `pom.xml`.

- **DEV/** — Core rules engine (type system, parser, binding, bytecode gen, project model)
- **STUDIO/** — Web IDE (Spring Boot backend + React/TypeScript frontend + legacy JSF)
- **WSFrontend/** — Rule Services (REST endpoints, Kafka, logging, metrics)
- **ITEST/** — Integration tests (TestContainers, declarative HTTP req/resp suites)
- **Util/** — CLI tools and utilities
- **Docs/** — Jekyll-based documentation site (GitHub Pages); user guides under `Docs/user-guides/`,
  cross-cutting architecture notes under `Docs/architecture/`

Dependency versions are managed in the root `pom.xml` (Java/Maven) and `STUDIO/studio-ui/package.json` (frontend).
Read those files for current versions, prefer the latest ones, and do not hardcode versions in documentation or
`AGENTS.md` files.

## Build and Verify

```bash
mvn clean install -Dquick -DnoPerf -T1C   # Fast dev build
mvn clean install -DskipTests              # Skip all tests; also drops ITEST and the archetypes from the reactor
mvn test -pl <module-path>                 # Test specific module
mvn validate -N                            # Format and mirrored-version check — run before committing
mvn verify -Dsonar                         # Coverage: JaCoCo runs ONLY with -Dsonar
docker compose up --build                  # Studio :8080, Rule Services :8081 (compose.yaml, NOT docker-compose.yaml)
```

- **`-Dquick`** — skip heavy tests
- **`-DnoPerf`** — relax memory limits
- **`-DnoDocker`** — skip Docker-based tests
- **`-DskipTests`** — skip all tests and drop the integration-test modules (ITEST, the archetypes) from
  the reactor; openl-maven-plugin still builds with its tests skipped. CI re-adds the dropped modules
  with `-Pitest` where it needs them
- **Single test** — Java: `mvn test -pl <module-path> -Dtest=ClassName#method`; frontend:
  `cd STUDIO/studio-ui && npx vitest run src/<file>.test.tsx` (watch: `npm run test:watch`); one integration suite:
  `mvn verify -pl ITEST/<suite> -am` (e.g. `ITEST/itest.smoke`).
- **Mirrored versions** — `Dockerfile` and the `DEMO/start*` launch scripts cannot read Maven properties, so they
  spell out `log4j.version`, `opentelemetry.version`, `jetty.version`, `postgresql.version` and `mssql.version` a
  second time. `mvn validate -N` fails when a copy drifts from its property; bump both sides together.
- Coverage report: `jacoco-report/target/site/jacoco-aggregate/jacoco.xml`; a line is uncovered when `ci="0"`.
  Coverage is measured on the diff, not the whole project — add tests until new lines reach ≥80%.

## Commit Convention

```
EPBDS-NNNNN <subject>

<optional body>
```

- **One logical change per commit** — one small piece of functionality or one refactoring step, with its tests and
  documentation, buildable and green on its own.
- **Fix issues in the commit that introduced them.** On an unpushed branch, fold fixes (bugs, failing tests,
  documentation, review findings) into the originating commit instead of stacking follow-up commits:
    - for the latest commit, use `git commit --amend`;
    - for an earlier commit, use `git commit --fixup=<sha>` and squash with
      `GIT_SEQUENCE_EDITOR=: git rebase -i --autosquash --autostash <base>`;
    - when a fix interacts with code changed by later commits (for example, an import they removed), adjust those
      commits in the same rebase so that every commit in the history stays buildable.
- **Prefix with the Jira ticket** (`EPBDS-NNNNN`), usually equal to the branch name.
- **The subject explains _why_ or _what_, not the mechanical move** already visible in the diff. Start it with an
  imperative verb.
    - Good — `EPBDS-15494 Stream file downloads instead of buffering`
    - Avoid — `EPBDS-15494 Move FileService into the rest package`
- **For bug fixes, name the cause and its observable effect**, not the symptom:
    - `EPBDS-15981 Fix NPE when ProjectDescriptor.name is null` — not `Fix 'something went wrong' message`
    - `Fix date parsing which breaks UI rendering` — not `Fix missed input`
- **Subject line only.** Add a body only when a single line cannot explain the change with fewer words.
- **No `Co-Authored-By:` or other co-author trailers.**
- **Skip the Jira prefix** when the change is unrelated to the ticket or conversation theme — an independent bug, a
  misconfiguration, or a dependency bump.

## Sources of Truth

- **Repository documentation is the centralized primary source.** `Docs/` (notably `Docs/architecture/`) and
  `AGENTS.md` files hold the approved architecture and decisions and must always contain the most current
  knowledge.
- **Jira is supplementary, non-centralized working information.** Tickets may contradict each other and the
  repository documentation.
- **Surface every conflict.** When tickets disagree with the repository documentation or with each other, show
  the divergence to the user instead of silently preferring one side. The repository document remains the
  approved position until the user decides otherwise.

## Jira Workflow

- **Search Jira before creating a ticket.** When a bug or an improvement is implemented, look for existing issues
  first, trying different wordings — do not duplicate tickets.
- **Keep the ticket description up to date.** When the scope or behavior changes during development, update the
  description so it matches the real implementation.
- **Create the ticket when it is absent** and fill it in completely:
    - the actual sprint;
    - the component;
    - the fix version;
    - additionally the affected version for a bug;
    - Story Points and the original estimate (1 Story Point ≈ 8 hours).
- **Link the tickets** when the relation is known: related to, depends on, and caused by.
- **Show ticket IDs as links** (`https://jira.eisgroup.com/browse/EPBDS-NNNNN`) in replies and reports for easy
  navigation.
- **Ticket creation can be skipped** when the change does not affect the code functionality (build configuration,
  process documentation, developer tooling) and no relevant ticket exists in Jira.

## Markdown Rules

- GFM style only
- Single located images MUST have descriptive title text
- Prefer bullet lists over dense prose
- Tables only when both columns are short or 3+ columns; otherwise use `- **label** — description`
- No version stamp in headings
- Mermaid for structural diagrams
- Admonitions: `> [!Note]` (single blockquote level only without title and nesting)

## JavaDoc Rules

- Describe **what** the code does, not **how** it is implemented
- Keep sentences short — prefer several short sentences over one long multi-clause sentence
- Use domain language (`path`, `field`, `parent object`, `collection element`) instead of internal jargon (`chain`,
  `deepest-first traversal`, `segments produced by iteration`)
- Skip implementation details (specific collections, algorithms, iteration order, internal APIs) unless they affect
  observable behavior
- Preserve behavioral details — edge cases, special behavior, invariants, constraints, assumptions
- Structure for scanning: short intro sentence, then separate paragraphs for behavior and special cases
- Aim for 3 short paragraphs rather than 1 dense paragraph
- Refactor only for readability — do not change behavior, contracts, or assumptions
