# OpenL Tablets 6.0.0 Release Notes

**Release Date:** January 22, 2026

---

## New Features

### Personal Access Tokens (PAT) for API Authentication
Personal Access Tokens (PATs) provide a secure, non-interactive way to authenticate with OpenL Studio APIs. Users can generate, list, and revoke tokens through both the REST API and the user interface. PATs support optional expiration dates and are stored securely, with the secret value shown only once at creation. This enables seamless integration with CI/CD pipelines, automation tools, and external systems without requiring interactive logins or exposing user credentials.

### Projects Merge REST API
The new Projects Merge REST API allows users to safely merge Git branches within OpenL Studio. The API performs pre-merge validation, detects conflicts, and offers multiple conflict resolution strategies. Users can manage merge sessions, download conflicted files for review, and resolve all conflicts before completing the merge. This ensures that merges are reliable and that no partial or inconsistent changes are introduced into the codebase.

### ACL (Access Control List) Management
OpenL Tablets 6.0.0 introduces a comprehensive Access Control List (ACL) system for fine-grained security management. Administrators can import ACL configurations in bulk via a secure REST API, manage access rights for repository roots, and assign roles and permissions to users and groups at a granular level. The new UI makes it easy to view and edit user and group access, with clear differentiation between inherited and direct permissions. Optimized role and permission lists simplify management, and all changes are validated for consistency and security.

### Java 21 Support
This release brings full support for Java 17 and Java 21, enabling OpenL Tablets to run on the latest Java runtimes. The platform has been updated to remove legacy Java 8 dependencies, reflection hacks, and features incompatible with modern Java versions. Docker images now use Eclipse Temurin 21 by default. Deprecated features and libraries, such as the cacheable property, cloning library, XStream, SOAP, WADL, jxpath, and cassandra, have been removed to ensure compatibility and maintainability. Enhanced error handling provides clear feedback if unsupported class versions are encountered.

### Admin UI & System Updates
The Admin panel has been completely redesigned with a modern, user-friendly interface. Administrators can now manage users, groups, and project rights more efficiently, with improved navigation and visibility. The new UI displays online status and clearly marks administrator accounts. Repository and system settings are accessible and editable from the Admin view, with sensitive fields protected and changes triggering application restarts as needed. WebSocket integration enables real-time notifications and messaging between admins and users. System and email settings management is streamlined, and error handling is more robust throughout the admin experience.

### REST API Enhancements
The REST API has been expanded to support new operations, including creating tables in projects, running tests and retrieving summaries, and receiving real-time test status updates via WebSockets. The /projects/{projectId}/tables endpoint now supports a wider range of table types, such as Rules, Data, Test, SmartLookup, and SimpleLookup. The Get projects API includes support for project dependencies and paging, making it easier to manage large numbers of projects programmatically.

### OAuth2 & SSO Integration
OpenL Tablets now supports the /.well-known/oauth-protected-resource endpoint, improving integration with OAuth2 and SSO providers. The authentication flows for OAuth2 and SAML have been enhanced to better support automation, external user systems, and seamless user experiences across integrated environments.

### Documentation & Tooling
Documentation has been reorganized for easier navigation and search, with migration notes and upgrade instructions included to help users transition to 6.0.0. A new skill is available to help users create new OpenL projects, and the documentation is structured to be more accessible for both new and experienced users.

---

## Improvements

**Studio**
- Name filtering and pagination for projects/tables API
- Filtering and pagination for test summary API
- Enhanced error handling and validation in Admin UI
- Improved Admin UI for user/group/project rights
- Enhanced project import performance and progress indicators
- Improved validation engine with detailed error messages
- Project import now supports large files and resumes interrupted imports
- Enhanced repository management and configuration
- Improved group/user management and security notifications
- Enhanced REST API documentation and OpenAPI integration
- Optimized role and access management lists
- Improved project selection and editing in ACL UI
- Online status and admin visibility in user lists
- WebSocket notifications for admin-user messaging

**DEMO**
- Dockerfile improvements for artifact name/version flexibility
- DEMO now installs required JRE automatically

**Documentation**
- Documentation moved and split for LLM-friendliness
- Actualized and organized content for easier reference
- ACL and Admin UI documentation

**Other**
- Skill to create new OpenL projects (agent skills integration)
- Review and optimization of toolset in MCP

---

## Breaking Changes & Migration Notes

- Tag migration may affect launch time and commit history when upgrading from 5.27.x to 6.0.0. Review migration instructions and test thoroughly.
- Default group security property no longer grants READ permission to all projects by default.
- See the full migration guide for configuration and code changes.
- Some deprecated code and APIs have been removed; review the migration guide for details.
- Dropped support for Java 8 and legacy features incompatible with Java 21
- Cacheable property and cloning library removed

---

## Bug Fixes

- Numerous bug fixes in Admin UI, repository management, and security
- Improved error reporting and troubleshooting for compilation and test failures
- Addressed issues with OAuth2 client secret handling, group/user management, and UI glitches
- Fixed issues with project import, table creation, and REST API edge cases
- Improved handling of external user groups and security tab requests
- Fixed ClassCastException and StackOverflowError in specific rule compilation scenarios
- Fixed project opening and class version errors for Java 21
- Admin UI user editing and permission fixes

---

## Updated Libraries

- Spring Framework 6.2.11
- Spring Boot 3.5.6
- Spring Security 6.5.5
- OpenSAML 5.1.6
- Nimbus JOSE+JWT 10.5
- OpenTelemetry 2.20.1
- Swagger Core 2.2.37
- Log4j 2.25.2
- Jetty 12.0.27
- Amazon AwsSDK 2.34.2
- Hibernate ORM 6.6.29.Final
- Jakarta Mail 2.1.5
- JAXB Runtime 4.0.6
- Guava 33.5.0-jre
- Glassfish Jakarta Faces 4.0.12
- Mockito 5.20.0
- PostgreSQL 42.7.8

---

For a full list of changes and tickets, see the EPBDS JIRA project and the OpenL Tablets repository.

*Thank you to all contributors and users for making this release possible!*
