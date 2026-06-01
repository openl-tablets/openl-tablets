# OpenL Tablets — Agent Instructions

OpenL Tablets is a business rules engine that compiles Excel spreadsheets into executable Java via runtime bytecode generation.

## Strict Rules (**MUST**. No exceptions.)

- Follow `.editorconfig` formatting (LF endings, 4-space indent for Java/XML, 120 char line length)
- Use correct casing: **OpenL**, **OpenL Studio**, **OpenL Rule Services**, **OpenL Tablets**
- Use Java 21 modern syntax (`var`, `record`, `sealed`, `switch` expressions, record pattern matching, text blocks and etc.)
  and features (Virtual Threads, Sequenced Collections, new String, Collections & IO/NIO methods and etc.)
- Use Lombok wherever it removes hand-written code: `@RequiredArgsConstructor` for constructor injection, `@Getter`/`@Setter` for accessors, `@Slf4j` for loggers, etc.
- When constructor injection needs `@Qualifier` or `@Value`, put the annotation on the **field** — the root `lombok.config` lists both as `copyableAnnotations`, so Lombok copies them onto the generated constructor parameter automatically
- Use JSpecify annotations (`@NullMarked` on packages; `@Nullable`/`@NonNull` on all reference types)
- Check folder-specific `AGENTS.md` hierarchically before modifying files in a folder
- Never use deprecated APIs — migrate to alternatives
- Run tests after changes
- No HTML in Markdown when equivalents exist (see Markdown Rules below)
- All new file names must be only in ASCII alphanumeric without spaces and any special characters, except for `-_.`
- Existed file names contains spaces and `,+%$#` symbols for tests purposes and must not be renamed during refactoring.
- Execute `mvn validate -N` after changes and before committing to ensure all files are formatted correctly.

## Repository Layout

Multi-module Maven project. Version: inherits from root `pom.xml`.

- **DEV/** — Core rules engine (type system, parser, binding, bytecode gen, project model)
- **STUDIO/** — Web IDE (Spring Boot backend + React/TypeScript frontend + legacy JSF)
- **WSFrontend/** — Rule Services (REST endpoints, Kafka, logging, metrics)
- **ITEST/** — Integration tests (TestContainers, declarative HTTP req/resp suites)
- **Util/** — CLI tools and utilities
- **Docs/** — Jekyll-based documentation site (GitHub Pages)

## Dependency Versions

Managed in root `pom.xml` (Java/Maven) and `STUDIO/studio-ui/package.json` (frontend). Read those files for current versions — do not hardcode versions in documentation or AGENTS.md files.

## Build

```bash
mvn clean install -Dquick -DnoPerf -T1C   # Fast dev build
mvn clean install -DskipTests              # Skip all tests
mvn test -pl <module-path>                 # Test specific module
```

- **`-Dquick`** — skip heavy tests
- **`-DnoPerf`** — relax memory limits
- **`-DnoDocker`** — skip Docker-based tests

## Code Coverage

New or changed Java code **MUST** keep ≥80% line coverage, measured on the diff (not the whole project).
Add tests until new lines reach ≥80%.

```bash
mvn verify -Dsonar   # JaCoCo runs ONLY with -Dsonar; the default build collects nothing
```

Report: `jacoco-report/target/site/jacoco-aggregate/jacoco.xml`. A line is uncovered when `ci="0"`.

## Docker

```bash
docker compose up --build   # From project root, uses compose.yaml (NOT docker-compose.yaml)
# Studio: http://localhost:8080
# Rule Services: http://localhost:8081
```

## Commit Convention

```
<type>: <subject>

<body>
```

Types: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `ci`

## Markdown Rules

- No HTML when Markdown equivalents exist
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
- Use domain language (`path`, `field`, `parent object`, `collection element`) instead of internal jargon (`chain`, `deepest-first traversal`, `segments produced by iteration`)
- Skip implementation details (specific collections, algorithms, iteration order, internal APIs) unless they affect observable behavior
- Preserve behavioral details — edge cases, special behavior, invariants, constraints, assumptions
- Structure for scanning: short intro sentence, then separate paragraphs for behavior and special cases
- Aim for 3 short paragraphs rather than 1 dense paragraph
- Refactor only for readability — do not change behavior, contracts, or assumptions
