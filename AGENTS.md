# OpenL Tablets — Agent Instructions

OpenL Tablets is a business rules engine that compiles Excel spreadsheets into executable Java via runtime bytecode generation.

## Strict Rules

- Follow `.editorconfig` formatting (LF endings, 4-space indent for Java/XML, 120 char line length)
- Use correct casing: **OpenL**, **OpenL Studio**, **OpenL Rule Services**, **OpenL Tablets**
- Use Java 21+ features where appropriate
- Check module-specific `AGENTS.md` before modifying any module
- Never use deprecated APIs — migrate to alternatives
- Run tests after changes
- No HTML in Markdown when equivalents exist (see Markdown Rules below)
- All new file names must be only in ASCII alphanumeric without spaces and any special characters, except for `-_.`
- Existed file names contains spaces and `,+%$#` symbols for tests purposes and must not be renamed during refactoring.
- Execute `mvn validate -N` after changes and before commiting to ensure all files are formatted correctly.

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
- Single located images MUST have descriptive title text
- Prefer bullet lists over dense prose
- Tables only when both columns are short or 3+ columns; otherwise use `- **label** — description`
