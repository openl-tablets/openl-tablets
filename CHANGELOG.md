# Changelog

All notable changes to OpenL Tablets will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Comprehensive GitHub governance files:
  - CONTRIBUTING.md with contribution guidelines
  - CODE_OF_CONDUCT.md based on Contributor Covenant
  - SECURITY.md with vulnerability reporting process
  - GitHub issue templates (bug report, feature request, documentation)
  - Pull request template
  - CODEOWNERS file for automatic reviewer assignment
- Enhanced README.md with better structure and sections
- Documentation index (Docs/README.md)
- Improved developer guide
- CITATION.cff for academic citations
- Dependabot configuration for automated dependency updates

### Changed
- Enhanced README.md structure with table of contents, features, use cases, and community sections

## [6.0.0-SNAPSHOT]

### Changed
- Updated Jetty to 12.1.3 (EPBDS-15276)
- Tags migration procedure update: DB connection handling, repositories with folders do not download projects (#1226)

### Fixed
- Dockerfile doesn't contain name and version of Maven artifact (EPBDS-15304)
- Disabled save button in user profile (EPBDS-15243)

### Documentation
- Created documentation for OpenL production configuration (EPBDS-14955) (#1201)

## Previous Versions

For changes in versions prior to 6.0.0, please refer to:
- [GitHub Releases](https://github.com/openl-tablets/openl-tablets/releases)
- [Git commit history](https://github.com/openl-tablets/openl-tablets/commits)

---

## Release Types

### Added
New features or functionality added to the project.

### Changed
Changes in existing functionality.

### Deprecated
Features that will be removed in upcoming releases.

### Removed
Features that have been removed.

### Fixed
Bug fixes.

### Security
Security-related changes or fixes.

### Performance
Performance improvements.

### Documentation
Documentation-only changes.

---

[Unreleased]: https://github.com/openl-tablets/openl-tablets/compare/HEAD...HEAD
[6.0.0-SNAPSHOT]: https://github.com/openl-tablets/openl-tablets/tree/HEAD
