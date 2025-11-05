# Contributing to OpenL Tablets

Thank you for your interest in contributing to OpenL Tablets! We welcome contributions from the community and are grateful for your support.

## Table of Contents

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

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When you create a bug report, include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples** to demonstrate the steps
- **Describe the behavior you observed** and what you expected to see
- **Include screenshots or animated GIFs** if applicable
- **Include your environment details** (OS, Java version, Maven version, etc.)

Use the bug report template when creating a new issue.

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion:

- **Use a clear and descriptive title**
- **Provide a detailed description** of the suggested enhancement
- **Explain why this enhancement would be useful** to most OpenL Tablets users
- **List any similar features** in other business rules engines if applicable

### Pull Requests

We actively welcome your pull requests:

1. **Fork the repository** and create your branch from the main branch
2. **Make your changes** following our coding standards
3. **Add tests** if you've added code that should be tested
4. **Ensure the test suite passes** (`mvn clean verify`)
5. **Update documentation** as needed
6. **Follow the commit message guidelines**
7. **Submit your pull request** using our PR template

#### Pull Request Guidelines

- Keep pull requests focused on a single feature or bug fix
- Include relevant issue numbers in your PR description
- Write clear, concise commit messages
- Update the CHANGELOG.md with your changes
- Ensure all CI checks pass
- Be responsive to feedback and questions

## Development Setup

### Requirements

- JDK 21+
- Maven 3.9.9
- Docker 27.5.0
- Docker Compose 2.32.4
- 1 GiB RAM free
- 2 GiB disk space free

### Build Instructions

Full build with tests:
```bash
mvn clean verify
```

Quick build with fewer tests:
```bash
mvn -Dquick -DnoPerf -T1C
```

Build options:
- `-DnoPerf` - Run tests without extreme memory limitation
- `-DnoDocker` - Skip dockerized tests
- `-Dquick` - Skip heavy or non-critical tests
- `-DskipTests` - Skip all tests

### Running OpenL Locally

Using Docker:
```bash
docker compose up
```

Then open http://localhost in your browser.

### Project Structure

- `DEV/` - Core OpenL engine and rules framework
- `STUDIO/` - OpenL Studio web application
- `WSFrontend/` - Rule Service web services
- `DEMO/` - Demo applications
- `Docs/` - Documentation
- `ITEST/` - Integration tests
- `Util/` - Utility modules and Maven plugins

## Coding Standards

### Java Code Style

- Follow standard Java naming conventions
- Use meaningful variable and method names
- Keep methods focused and concise
- Add JavaDoc comments for public APIs
- Use proper exception handling
- Avoid premature optimization

### Code Formatting

- Use 4 spaces for indentation (no tabs)
- Maximum line length: 120 characters
- Place opening braces on the same line
- Use proper spacing around operators and keywords

### Best Practices

- Write clean, self-documenting code
- Follow SOLID principles
- Prefer composition over inheritance
- Write unit tests for new functionality
- Keep dependencies minimal and up-to-date
- Handle edge cases and error conditions

## Testing Requirements

### Unit Tests

- Write JUnit 5 tests for all new functionality
- Aim for high code coverage (minimum 70%)
- Test both success and failure scenarios
- Use meaningful test method names
- Keep tests independent and repeatable

### Integration Tests

- Add integration tests for features that interact with external systems
- Use TestContainers for database and service dependencies
- Ensure tests clean up resources properly

### Running Tests

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=YourTestClass
```

Run tests without Docker:
```bash
mvn test -DnoDocker
```

## Commit Message Guidelines

We follow conventional commit message format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

- `feat` - A new feature
- `fix` - A bug fix
- `docs` - Documentation only changes
- `style` - Code style changes (formatting, semicolons, etc.)
- `refactor` - Code refactoring without functionality changes
- `perf` - Performance improvements
- `test` - Adding or updating tests
- `chore` - Maintenance tasks, dependency updates

### Examples

```
feat(studio): add support for Excel table validation

Implement new validation rules for Excel tables including
type checking and constraint validation.

Closes #123
```

```
fix(ruleservice): resolve memory leak in cache implementation

The cache was not properly releasing references to compiled
rules, causing memory growth over time.

Fixes #456
```

## Documentation

### When to Update Documentation

- Adding new features or APIs
- Changing existing functionality
- Fixing bugs that affect documented behavior
- Improving examples or tutorials

### Documentation Guidelines

- Write clear, concise documentation
- Include code examples where applicable
- Update both inline code comments and external docs
- Keep documentation in sync with code changes
- Use proper markdown formatting

### Documentation Location

- User documentation: `Docs/`
- API documentation: JavaDoc in source code
- Configuration guides: `Docs/Configuration.md`
- Developer guides: `Docs/developer-guide/`

## Getting Help

- Check existing [documentation](Docs/)
- Search [existing issues](https://github.com/openl-tablets/openl-tablets/issues)
- Visit the [OpenL Tablets website](https://openl-tablets.org)
- Ask questions in issue discussions

## Recognition

Contributors will be recognized in:
- The project's contributor list
- Release notes for significant contributions
- The project README for major features

Thank you for contributing to OpenL Tablets!
