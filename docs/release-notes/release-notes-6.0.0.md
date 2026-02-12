## Release Notes

[v6.0.0](https://github.com/openl-tablets/openl-tablets/releases/tag/6.0.0) on the GitHub

**Release Date:** February 2026

OpenL Tablets 6.0.0 is a major release that introduces a completely redesigned administration interface built with modern React technology, a simplified and more robust Access Control Lists (ACL) system with streamlined role-based permissions, and migration to Jakarta EE 10 and Java 21. The release also includes MCP integration for AI-powered workflows, Personal Access Tokens for automation, and comprehensive API improvements with enhanced OpenAPI documentation.

### New Features

**Redesigned Administration Interface**

The administration panel has been completely redesigned using modern React technology, replacing the legacy JSF/RichFaces implementation. The new interface provides a more intuitive, responsive, and user-friendly experience for system administrators.

Key components include Security Settings for managing authentication modes (Multi-user, Active Directory, SAML, OAuth2), User Management with enhanced creation, editing, and deletion workflows, Group Management with streamlined administration and member count display, and Repository Management with intuitive configuration for Git, AWS S3, and JDBC repositories including connection testing.

The new UI is built using React with modern component architecture. All administrative operations are backed by RESTful APIs, enabling potential automation and integration scenarios. System Settings provide centralised management of system properties including date formats, auto-compile settings, and thread pool configurations.

**Enhanced Access Control Lists (ACL)**

OpenL Tablets 6.0.0 introduces a completely redesigned access control system that simplifies permissions management while providing more granular control over resources. The new ACL system replaces the previous 16-permission model with a streamlined 3-role approach.

The simplified role model consists of three primary roles: Viewer (read-only access to resources), Contributor (full edit access including the ability to create, modify, and delete content), and Manager (complete administrative control with the ability to assign access rights to other users). Access control is applied at the repository level, allowing administrators to grant specific roles for Design and Deployment repositories independently.

The system integrates with external user management systems including Active Directory, SAML, and OAuth2 providers. A new REST API enables bulk import and export of ACL configurations, facilitating automated deployment processes across multiple environments. A dedicated Access Rights tab with an 'All' dropdown option has been added for User and Group management, providing the ability to assign default roles to all repositories within a repository root.

**Jakarta EE 10 and Java 21 Migration**

OpenL Tablets 6.0.0 completes the migration to Jakarta EE 10 and Java 21, providing a modern, secure, and high-performance foundation for the platform. This migration affects both OpenL Studio and Rule Services.

Key framework upgrades include Spring Framework 6.2, Spring Security 6.4, Spring Boot 3.4, OpenSAML 5.1, Hibernate ORM 6.6, Hibernate Validator 8.0, and Jetty 12. The migration also removes reflection hacks previously needed for Java compatibility, improving stability and maintainability.

Docker images have been updated to use eclipse-temurin:21-jre-alpine as the base image, which typically contains no known vulnerabilities.

**MCP (Model Context Protocol) Integration**

OpenL Tablets 6.0.0 introduces support for the Model Context Protocol (MCP), enabling integration with AI assistants such as Claude Desktop, ChatGPT Desktop, and IDE plugins. This unlocks new possibilities for AI-powered business rule management and interaction.

The implementation includes an OAuth 2.1-compliant /.well-known/oauth-protected-resource endpoint that allows MCP clients to dynamically discover OAuth configuration and initiate Authorization Code + PKCE login flows. The MCP server has been moved to a dedicated repository for independent development and deployment.

A comprehensive REST API has been extended to support AI-powered workflows, including table viewing, editing, and creation through the API, with pagination and filtering support for projects and tables.

### Improvements

**OpenL Studio:**
* Simplified deployment workflow by removing the deploy configuration step. Users can now deploy projects directly through a streamlined popup dialogue when clicking the Deploy button.
* Project tags are now stored as part of the OpenL project (in tags.properties files) rather than in the database, allowing tags to migrate with projects across environments.
* Automated OpenL project recognition in Git-based design repositories, removing the need for the 'Flat folder structure' checkbox.
* Added WebSocket-based messaging between the admin panel and users for real-time notifications and updates.
* Added support for customising the default user in single-user mode.
* Removed the user workspace setting from the UI for security reasons. Folder paths can now only be defined via environment variables.
* Introduced Personal Access Tokens (PATs) for secure, non-interactive authentication. PATs support optional expiration dates, immediate revocation, and are intended for CI/CD pipelines, service-to-service communication, and automation scripts.
* Extended the /projects/{projectId}/tables API to support additional table types including Rules, Data, Test, SmartLookup, and SimpleLookup tables. Added a create table API endpoint.
* Improved the get projects/tables API with name filtering, pagination, total count attributes, and an unpaged option for retrieving full lists.
* Added a REST API for running tests in OpenL Studio.

**Rule Services:**
* Added support for generating meaningful OpenAPI descriptions from Excel documentation, including descriptions for datatype fields, method arguments, and spreadsheet result structures.
* Added the ability to inject the current ProjectDescriptor into service method handlers.
* Migrated Rule Services to Jakarta EE 10 with updated Spring Framework 6.2 and Spring Boot 3.4.

**Core:**
* Added statistical functions including standard deviation, correlation, and linear regression.
* Fixed the dependency graph display for tables containing business dimensional properties.
* Fixed table connection display on the dependencies graph when one table calls another using Multi Call.

**DEMO:**
* The DEMO package now automatically installs the required JRE, eliminating the need for users to set up Java separately.
* Replaced the DEMO Docker image with a compose.yaml file for easier deployment and configuration.

### Fixed Bugs

**OpenL Studio:**
* Fixed an issue where defining a 'Default Group' would grant READ permission to all projects.
* Fixed Git LFS not working with Bitbucket repositories.
* Fixed an issue where OpenL Studio would work incorrectly or crash if a project with an OpenAPI file had a dependency on other projects.
* Fixed an Internal Server Error that appeared when deploying a project without specifying base.path in the configuration.
* Fixed OAuth2 not working with the default configuration.

**Core:**
* Fixed an IllegalStateException that appeared in Trace if the rule had a version with the Origin='Deviation' property.
* Resolved a vulnerability related to Jackson deserialisation by using NAME instead of CLASS by default.
* Addressed direct vulnerabilities in the commons-jxpath library.

### Updated Libraries

**Fixed Vulnerabilities:**
* CVE-2025-41249 (Spring Framework)
* CVE-2025-41248 (Spring Security)

**Runtime Dependencies:**
* Amazon AWS SDK 2.34.2
* Angus Mail 2.0.5
* BouncyCastle 1.82
* Glassfish Jakarta Faces 4.0.12
* Gson 2.13.2
* Guava 33.5.0-jre
* Hibernate ORM 6.6.29.Final
* Jakarta Mail 2.1.5
* Jakarta XML Bind 4.0.4
* JAXB Runtime 4.0.6
* Jetty 12.0.27
* Log4j 2.25.2
* Netty 4.2.6.Final
* Nimbus JOSE+JWT 10.5
* OpenSAML 5.1.6
* OpenTelemetry 2.20.1
* Spring Boot 3.5.6
* Spring Framework 6.2.11
* Spring Integration 6.5.2
* Spring Security 6.5.5
* Swagger Core 2.2.37
* Swagger Parser 2.1.34

**Test Dependencies:**
* Mockito 5.20.0
* PostgreSQL 42.7.8
* XMLUnit 2.10.4

**Maven:**
* Apache Maven Compiler Plugin 3.14.1
* Dependency Check Maven Plugin 12.1.6
* License Maven Plugin 2.7.0
* Maven Surefire Plugin 3.5.4
* Rewrite Maven Plugin 6.19.0

### Breaking Changes

**Core:**
* Dropped support for the 'isProvideVariations' feature. Users should create explicit array-returning rules as a replacement.
* Changed 'isProvideRuntimeContext' default value to false. Projects relying on runtime context injection must explicitly set this to true.
* Dropped support for the 'cacheable' property, which used the cloning library incompatible with Java 21.
* Dropped support for the 'recalculate' property, previously designed for the Variation functionality cache.
* Dropped support for the Spreadsheet result calculation step library. Use SpreadsheetResult directly instead.
* Removed deprecated code including: @AnyType, @InjectOpenClass, @InjectOpenMember, @InjectRulesDeploy, @InjectServiceClassLoader annotations; StringOperators, WholeNumberDivideOperators, MulDivNullToOneOperators classes; deprecated RulesUtils methods; and support for rules.xml versions 5.23 and older.
* Removed deprecated DoubleRange and StringRange methods including BoundType enum, setLowerBound(), setUpperBound(), and related bound type accessors.
* Dropped the 'language' property support and OpenL.properties configurations.
* Deleted properties: custom.spreadsheet.type and dispatching.mode.

**Rule Services:**
* Dropped RMI protocol support.
* Dropped Apache Hive support for store log data. Use the 'org.openl.rules.ruleservice.ws.storelogdata.db' module as a replacement.
* Dropped Apache Cassandra support.
* Dropped Activiti extension support.
* Migrated to Jakarta EE 10, which changes the javax.* namespace to jakarta.* for all EE APIs.

**OpenL Studio:**
* Removed the /rest/repo/* API endpoints. Use the new /rest/projects/* API instead.
* Removed embedded JDBC drivers. Clients should mount the required driver versions into /opt/openl/lib.
* Removed the Install Wizard. Initial configuration is now handled through the Admin panel with default single-user setup.
* Dropped CAS SSO authentication support. Use OAuth2 or SAML as alternatives.
* Removed support for running VBScript under Windows, following Microsoft's deprecation of VBScript.
* Removed Java Preferences-based configuration.
* Removed the 'Demo' authentication mode from the Admin panel. Demo setup is now preconfigured only in the Demo package.

### Migration Notes

**Jakarta EE 10 Migration**

All javax.* package imports must be changed to jakarta.* for Servlet, Persistence, Validation, and other EE APIs. This affects both OpenL Studio and Rule Services.

Action required:
1. Update all javax.servlet.* imports to jakarta.servlet.*
2. Update all javax.persistence.* imports to jakarta.persistence.*
3. Update all javax.validation.* imports to jakarta.validation.*
4. Review and update any custom filters, listeners, or interceptors
5. Update deployment descriptors (web.xml) if using javax references

**JDBC Drivers Removed from Distribution**

OpenL Studio no longer ships with embedded JDBC drivers. Database connectivity requires manual driver installation.

Action required:
1. Download the JDBC driver JAR for your database (PostgreSQL, MySQL, Oracle, etc.)
2. Mount the driver into /opt/openl/lib in the Docker container
3. Update your compose.yaml or Dockerfile accordingly

Example:
```yaml
volumes:
  - ./drivers/postgresql-42.7.8.jar:/opt/openl/lib/postgresql-42.7.8.jar
```

**isProvideVariations Feature Removed**

The Variation functionality has been completely removed. Projects using isProvideVariations in rules-deploy.xml will no longer work.

Action required:
1. Remove `<isProvideVariations>true</isProvideVariations>` from rules-deploy.xml
2. Create a new rule that accepts an array of input parameters and returns an array of results
3. Update integration code to use the new array-based endpoint

**isProvideRuntimeContext Default Changed to false**

Projects that rely on runtime context injection (e.g., for module dispatching or property-based versioning) must now explicitly enable it.

Action required:
1. If your project uses runtime context, add `<isProvideRuntimeContext>true</isProvideRuntimeContext>` to your rules-deploy.xml

**CAS SSO Support Dropped**

CAS (Central Authentication Service) Single Sign-On is no longer supported.

Action required:
1. Migrate to OAuth2 or SAML authentication
2. Update your identity provider configuration accordingly

**ACL Permission Model Changed**

The previous 16-permission model has been replaced with a 3-role model (Viewer, Contributor, Manager).

Action required:
1. Review existing user/group permissions
2. Map old permissions to the new role model
3. Update any automation scripts that manage permissions via the API

**REST API Changes**

The /rest/repo/* API endpoints have been removed.

Action required:
1. Migrate API clients from /rest/repo/* to the new /rest/projects/* endpoints
2. Review the updated OpenAPI documentation for new endpoint signatures

**Deprecated Annotations and Classes Removed**

Several deprecated annotations and utility classes have been removed.

Action required:
1. Remove usage of @AnyType, @InjectOpenClass, @InjectOpenMember, @InjectRulesDeploy, @InjectServiceClassLoader annotations
2. Replace StringOperators, WholeNumberDivideOperators, MulDivNullToOneOperators with standard operators
3. Replace deprecated RulesUtils methods with current alternatives
4. Ensure rules.xml is version 5.24 or later

### Live Demo

Check [OpenL Tablets demo](http://demo.openl-tablets.org/) in action

### Need help?
* [Documentation](https://openl-tablets.org/documentation)
* [Videos](https://openl-tablets.org/documentation/videos)
* [Forum](http://sourceforge.net/p/openl-tablets/discussion/)
* [Contact Us](https://openl-tablets.org/community/contact-us)
