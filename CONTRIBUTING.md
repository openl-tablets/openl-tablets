# Contributing to OpenL Tablets

Thank you for your interest in contributing to OpenL Tablets! We welcome contributions from the community and are grateful for your support.

The approved rules for code, builds, commits and documentation live in [AGENTS.md](AGENTS.md) and the folder-specific
`AGENTS.md` files. This guide summarizes them for contributors and links to them; when the two differ, `AGENTS.md`
wins.

## Table of Contents

- [Quick Start](#quick-start)
- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Enhancements](#suggesting-enhancements)
  - [Pull Requests](#pull-requests)
- [Development Setup](#development-setup)
- [Coding Standards](#coding-standards)
- [Testing Requirements](#testing-requirements)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Documentation](#documentation)

## Quick Start

### Prerequisites

- **JDK 21+** (Java Development Kit)
- **Maven**
- **Docker** — for `docker compose` and the TestContainers-based tests
- **Node.js and npm** — only to run the frontend tooling directly in `STUDIO/studio-ui`; the Maven build downloads
  its own copy

### Build and Run

```bash
# 1. Clone the repository
git clone https://github.com/openl-tablets/openl-tablets.git
cd openl-tablets

# 2. Build OpenL Tablets without tests
mvn clean install -DskipTests -T1C

# 3. Start the containers
docker compose up --build
```

The Maven build must run first: `docker compose` runs no Maven, it builds the images from the build output in
`STUDIO/studio-backend/target/webapp` and `WSFrontend/org.openl.rules.ruleservice.ws/target/webapp`.

`docker compose up --build` starts:

- **PostgreSQL** — the database of OpenL Studio and of the deployment repository
- **OpenL Studio** — http://localhost:8080
- **OpenL Rule Services** — http://localhost:8081
- **nginx** — http://localhost, a landing page that links OpenL Studio at `/studio` and OpenL Rule Services at
  `/services`

**Need help?**
- Check [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)
- Review [Development Setup](#development-setup) below
- See [AGENTS.md](AGENTS.md) for the project rules and AI-assisted development

---

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Report bugs in [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues). Report security
vulnerabilities privately, as [SECURITY.md](SECURITY.md) describes, not in public issues.

Before creating a bug report, check the existing issues to avoid duplicates. When you create a bug report, include
as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**, one action per step
- **Describe the behavior you observed**, with the exact error text, and what you expected to see
- **Include screenshots** if applicable, and the stack trace when there is one
- **Include your environment details** (OS, Java version, OpenL Tablets version, etc.)
- **Reproduce the defect with a small project of your own** — do not attach client projects or data taken from them

### Suggesting Enhancements

Enhancement suggestions are tracked as [GitHub Issues](https://github.com/openl-tablets/openl-tablets/issues).
When creating an enhancement suggestion:

- **Use a clear and descriptive title**
- **Provide a detailed description** of the suggested enhancement
- **Explain why this enhancement would be useful** to most OpenL Tablets users
- **List any similar features** in other business rules engines if applicable

### Pull Requests

We actively welcome your pull requests:

1. **Fork the repository** and create your branch from the `main` branch
2. **Make your changes** following our coding standards
3. **Add tests** for the changed code
4. **Ensure the build passes** (`mvn`), and format the files with `mvn validate -N`
5. **Update documentation** in the same commit as the change
6. **Follow the commit message guidelines**
7. **Submit your pull request**, describing what it changes and why

#### Pull Request Guidelines

- Keep pull requests focused on a single feature or bug fix
- Include relevant issue numbers in your PR description
- Write clear, concise commit messages
- When a change affects upgrading, describe it in the release notes of the upcoming version:
  `Docs/release-notes/<version>/migration.md`, next to the version's `index.md`
- Ensure all CI checks pass
- Be responsive to feedback and questions

## Development Setup

### Requirements

- JDK 21+ — the root `pom.xml` compiles with `<release>21</release>`
- Maven
- Docker
- 8 GiB RAM free
- 2 GiB disk space free

The Node.js and npm versions of the frontend build are pinned in the root `pom.xml` (`node.version`,
`npm.version`); the minimum versions for running npm directly are the `engines` of `STUDIO/studio-ui/package.json`.

### Build Instructions

The commands below are a short form of [Build and Verify](AGENTS.md#build-and-verify) in `AGENTS.md`.

Full build with tests (the default goal is `clean verify`):
```bash
mvn
```

Fast development build:
```bash
mvn clean install -Dquick -DnoPerf -T1C
```

Build options:
- `-Dquick` — skip heavy tests
- `-DnoPerf` — relax memory limits
- `-DnoDocker` — skip Docker-based tests
- `-DskipTests` — skip all tests and drop the integration-test modules (ITEST, the archetypes) from the reactor
- `-Dnpm.test.skip`, `-Dnpm.typecheck.skip`, `-Dnpm.build.skip` — leave out one `studio-ui` step at a time

Format the files and check the mirrored versions before committing:
```bash
mvn validate -N
```

Locally Spotless rewrites unformatted files. On CI, where the `CI` environment variable activates the `ci` profile,
Spotless only checks them and fails the build.

### Running OpenL Locally

Build first, then start the containers:
```bash
mvn clean install -DskipTests -T1C
docker compose up --build
```

Then open http://localhost:8080 for OpenL Studio, http://localhost:8081 for OpenL Rule Services, or
http://localhost for a page that links both.

### Project Structure

- `DEV/` — Core rules engine (type system, parser, binding, bytecode generation, project model)
- `STUDIO/` — OpenL Studio web application (Spring backend and React/TypeScript frontend)
- `WSFrontend/` — OpenL Rule Services (REST endpoints, Kafka, logging, metrics)
- `DEMO/` — Demo package
- `Docs/` — Documentation site and user guides
- `ITEST/` — Integration tests
- `Util/` — Maven plugin, project archetypes and shared libraries (OpenAPI, Excel builder, YAML, OpenTelemetry)

## Coding Standards

The full list is in [AGENTS.md](AGENTS.md); check the folder-specific `AGENTS.md` before changing files in a folder.

### Java Code Style

- Use modern Java 21 syntax and features (`var`, `record`, `sealed`, `switch` expressions, record patterns, text
  blocks)
- Use Lombok where it removes hand-written code (`@RequiredArgsConstructor`, `@Getter`/`@Setter`, `@Slf4j`)
- Use JSpecify annotations (`@NullMarked` on packages, `@Nullable`/`@NonNull` on reference types)
- Never use deprecated APIs
- Keep methods compact and single-purpose
- Use the narrowest visibility
- Write JavaDoc as [AGENTS.md](AGENTS.md#javadoc-rules) describes

### Code Formatting

Follow [.editorconfig](.editorconfig):

- LF line endings
- 4 spaces for indentation in Java and XML (no tabs)
- Maximum line length: 120 characters

### Best Practices

- Follow **KISS**, **YAGNI** and **DRY**
- Write unit tests for new functionality
- Keep dependencies minimal and up-to-date; versions are managed in the root `pom.xml` and
  `STUDIO/studio-ui/package.json`
- Handle edge cases and error conditions
- Use the correct casing: **OpenL**, **OpenL Studio**, **OpenL Rule Services**, **OpenL Tablets**

## Testing Requirements

### Unit Tests

- Write JUnit 5 tests for all new functionality; test classes and test methods are package-private
- Keep at least 80% line coverage on new or changed Java code
- Test both success and failure scenarios
- Use meaningful test method names
- Keep tests independent and repeatable

### Integration Tests

- Add integration tests in `ITEST/` for features that interact with external systems
- Use TestContainers for database and service dependencies
- Ensure tests clean up resources properly

### Running Tests

Run all tests:
```bash
mvn test
```

Run the tests of one module:
```bash
mvn test -pl <module-path>
```

Run a single test:
```bash
mvn test -pl <module-path> -Dtest=ClassName#method
```

Run tests without Docker:
```bash
mvn test -DnoDocker
```

Measure coverage (JaCoCo runs only with `-Dsonar`):
```bash
mvn verify -Dsonar
```

## Commit Message Guidelines

Commits follow the [Commit Convention](AGENTS.md#commit-convention) in `AGENTS.md`:

```
EPBDS-NNNNN <subject>
```

- **One logical change per commit**, with its tests and documentation, buildable on its own
- **Prefix with the Jira ticket** (`EPBDS-NNNNN`); skip the prefix for unrelated changes such as a dependency bump
- **Start the subject with an imperative verb** and explain _why_ or _what_, not the mechanical move
- **For bug fixes, name the cause and its observable effect**
- **Subject line only**; add a body only when one line of up to 80 symbols cannot explain the change
- **Fold fixes into the commit that introduced them** on your branch instead of stacking follow-up commits
- **No `Co-Authored-By:` or other co-author trailers**

### Examples

```
EPBDS-15494 Stream file downloads instead of buffering
```

```
EPBDS-15981 Fix NPE when ProjectDescriptor.name is null
```

## Documentation

### When to Update Documentation

Documentation mirrors functionality. Update it in the same commit as the change when you:

- Add new features or APIs
- Change existing functionality
- Fix bugs that affect documented behavior
- Change what a screen shows — recapture the affected screenshots in the guide's `images/` folder

### Documentation Guidelines

- Write clear, concise documentation
- Include code examples where applicable
- Update every place that describes the behavior: `Docs/` guides, `AGENTS.md` files and JavaDoc
- Follow the [Markdown Rules](AGENTS.md#markdown-rules) and [Docs/AGENTS.md](Docs/AGENTS.md)

Validate the user guides (links, images, Markdown):
```bash
mvn test -pl STUDIO/studio-docs
```

### Documentation Location

- User guides: `Docs/user-guides/`
- Developer guides: `Docs/developer-guides/`
- Architecture notes: `Docs/architecture/`
- Release and migration notes: `Docs/release-notes/<version>/`
- API documentation: JavaDoc in source code

## Getting Help

- Check existing [documentation](Docs/)
- Search [existing issues](https://github.com/openl-tablets/openl-tablets/issues)
- Visit the [OpenL Tablets website](https://openl-tablets.org)
- Ask questions in [GitHub Discussions](https://github.com/openl-tablets/openl-tablets/discussions)

Thank you for contributing to OpenL Tablets!
