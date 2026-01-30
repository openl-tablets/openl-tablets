# OpenL Tablets 5.27.12 - 5.27.14 Release Notes

This document contains combined release notes for OpenL Tablets versions 5.27.12, 5.27.13, and 5.27.14.

---

## Summary

| Version | Release Date | New Features | Improvements | Bug Fixes | Library Updates |
|---------|--------------|--------------|--------------|-----------|-----------------|
| 5.27.14 | October 8, 2025 | 0 | 0 | 1 | 20 |
| 5.27.13 | September 5, 2025 | 0 | 4 | 3 | 21 |
| 5.27.12 | July 22, 2025 | 2 | 2 | 4 | 27 |

**GitHub Releases:**
- [5.27.14](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.14)
- [5.27.13](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.13)
- [5.27.12](https://github.com/openl-tablets/openl-tablets/releases/tag/5.27.12)

---

## Version 5.27.14

**Release Date:** October 8, 2025

### Bug Fixes

#### OpenL Rule Services

- **Spring Context Classloader Fix**: Resolves an issue where the Spring Context was not using the correct classloader in certain deployment scenarios. This fix ensures that the `ServiceInvocationAdvice` properly defines and utilizes the correct classloader for Spring Context operations, preventing class loading issues in complex deployment environments where multiple classloaders are present. This is particularly important for applications that use custom classloader hierarchies or deploy OpenL rules as part of a larger enterprise application.

### Updated Libraries

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Nimbus JOSE+JWT | 10.4.2 | 10.5 |
| Snappy Java | - | 1.1.10.8 |
| OpenTelemetry | 2.19.0 | 2.20.1 |
| Swagger Core | 2.2.36 | 2.2.38 |
| Swagger Parser | 2.1.33 | 2.1.34 |
| Log4j | 2.25.1 | 2.25.2 |
| AWS SDK | 2.33.0 | 2.34.9 |
| Azure Storage Blob | 12.31.2 | 12.31.3 |
| Netty | 4.2.4.Final | 4.2.6.Final |
| H2 Database | - | 2.4.240 |
| JUnit | 5.13.4 | 5.14.0 |
| Mockito | 5.19.0 | 5.20.0 |
| XMLUnit | 2.10.3 | 2.10.4 |
| PostgreSQL JDBC | 42.7.7 | 42.7.8 |
| Jakarta Mail | - | 1.6.8 |
| Commons Lang3 | 3.18.0 | 3.19.0 |
| Gson | - | 2.13.2 |
| Bouncy Castle | 1.81 | 1.82 |
| Guava | - | 33.5.0-jre |

---

## Version 5.27.13

**Release Date:** September 5, 2025

### Improvements

#### OpenL Rule Services

- **Swagger/OpenAPI Migration**: Migrates to the latest Swagger libraries (Core 2.2.36, Parser 2.1.33) with improved model converters. The `PropertySchemaCustomizingConverter` has been enhanced with better schema customization capabilities, supporting more complex data type representations in the generated OpenAPI specifications. This update ensures compatibility with the latest OpenAPI tooling and improves the accuracy of generated API documentation for rule services.

#### OpenL Studio

- **Removed Unused Configuration Property**: Cleans up the unused `repo-file.support-deployments` property from the codebase, reducing configuration complexity and potential confusion for administrators.

#### Testing Infrastructure

- **Improved Test Resource Management**: Modernizes test infrastructure by replacing manual `Files.createTempDirectory()` and `close()` patterns with JUnit 5's `@TempDir` and `@AutoClose` annotations. This change ensures consistent cleanup of temporary resources during test execution, reducing the likelihood of resource leaks and improving test reliability.

- **Test Compatibility Improvements**: Fixes the location of temporary folders when tests are run under Java 21, ensuring consistent test behavior across different JDK versions.

### Bug Fixes

#### OpenL Rule Services

- **AccessDeniedHandler Order Fix**: Resolves an issue where the order of `AccessDeniedHandler` execution was unpredictable. The new `ConflictedAccessDeniedHandler` implementation provides a deterministic ordering mechanism, ensuring consistent security behavior across different deployment environments and preventing potential security edge cases where the wrong handler might be invoked.

#### OpenL Studio

- **Windows Compatibility Fix**: Addresses an issue where temporary folder cleanup failed on Windows systems due to file locking by JGit. The cleanup process now handles this scenario gracefully, preventing test failures and resource cleanup issues on Windows development and CI environments.

### Updated Libraries

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Nimbus JOSE+JWT | 10.3.1 | 10.4.2 |
| JSON Smart | - | 2.6.0 |
| Byte Buddy | 1.17.6 | 1.17.7 |
| OpenTelemetry | 2.18.0 | 2.19.0 |
| Jackson | 2.19.2 | 2.20.0 |
| Apache CXF | 3.6.7 | 3.6.8 |
| gRPC | 1.73.0 | 1.75.0 |
| Swagger Core | - | 2.2.36 |
| Swagger Parser | - | 2.1.33 |
| Jetty | - | 10.0.26 |
| AWS SDK | 2.32.4 | 2.33.0 |
| Azure Storage Blob | 12.31.0 | 12.31.2 |
| Netty | 4.2.3.Final | 4.2.4.Final |
| HikariCP | - | 7.0.2 |
| JUnit | 5.13.3 | 5.13.4 |
| Mockito | 5.18.0 | 5.19.0 |
| Oracle JDBC | 23.8.0.25.04 | 23.9.0.25.07 |
| Commons Codec | 1.18.0 | 1.19.0 |
| Commons Compress | 1.27.1 | 1.28.0 |
| Groovy | 4.0.27 | 4.0.28 |

---

## Version 5.27.12

**Release Date:** July 22, 2025

### New Features

#### Java 25 Support

OpenL Tablets now fully supports Java 25, ensuring compatibility with the latest Java release. This update includes modifications to the build configuration and OpenTelemetry instrumentation module to properly function under Java 25's enhanced module system and runtime characteristics.

**Key Changes:**
- Updated GitHub Actions build workflow to include Java 25 in the test matrix
- Modified OpenTelemetry instrumentation module to use standard service provider mechanism
- Adjusted POM configuration to ensure proper compatibility with Java 25 runtime

This allows organizations planning to upgrade to Java 25 to continue using OpenL Tablets without compatibility concerns.

#### Datatype Java Source Generation Example

Adds a comprehensive Maven integration test example demonstrating how to generate Java source files from OpenL Datatype tables. This example, located in the `openl-gen-decompile-datatypes` integration test module, shows developers how to:

- Configure Maven to compile OpenL rules and extract datatype definitions
- Generate human-readable Java source files from compiled datatypes
- Integrate the generated sources into the standard Maven build lifecycle
- Extend base service classes with generated datatype-aware implementations

This feature is particularly valuable for teams that want to use OpenL-defined datatypes in their broader Java codebase while maintaining the datatypes in Excel for business user accessibility.

### Improvements

#### OpenL Studio

- **RichFaces Upgrade**: Upgrades to the latest version of RichFaces, the JSF component library used by OpenL Studio's legacy web interface. This update includes a custom workaround for a file upload issue introduced in the upstream RichFaces library, implemented through a custom `UIFileUpload` component. The upgrade improves stability and security of the web interface while maintaining backward compatibility with existing deployments. Related jQuery workarounds have also been removed as they are no longer necessary with the updated library.

#### OpenL Rule Services

- **Spring 6 Compatibility**: Enhances compatibility with Spring Framework 6.x by updating validation message handling to use standard `ValidationMessages.properties`, adjusting OpenAPI parameter and response service implementations, and updating security configuration beans. These changes ensure smooth operation when deploying OpenL Rule Services in Spring 6-based application environments.

### Bug Fixes

#### OpenL Core

- **DoubleRange.contains() NaN Handling**: Fixes a bug where the `DoubleRange.contains()` method did not properly handle `NaN` (Not a Number) values. Previously, passing `NaN` to the contains method could produce incorrect or unpredictable results. The fix ensures that `NaN` values are handled consistently according to IEEE 754 floating-point semantics, preventing potential rule evaluation errors in calculations involving undefined or error values.

#### OpenL Studio

- **Method Graph for Overloaded Tables**: Fixes an issue where the dependency graph visualization in OpenL Studio did not correctly display relationships for overloaded decision tables. The `DependencyRulesGraph` component now properly identifies and visualizes dependencies when multiple tables share the same name but have different signatures, providing accurate traceability information for complex rule sets.

- **Method Graph for Multicall Methods**: Resolves a problem where the dependency graph did not correctly represent calls made through `MultiCallOpenMethod` instances. The fix updates the `BindingDependencies` and `RulesBindingDependencies` classes to properly track and report dependencies that involve array-based multicall invocations, ensuring complete dependency visualization.

- **NPE in Non-Branch Repositories**: Fixes a `NullPointerException` that occurred when using project dependencies with database repositories that do not have branching enabled. The `ProjectDependencyResolverImpl` now properly handles null branch values, allowing seamless use of dependent projects in DB Repository configurations without branch support.

### Updated Libraries

| Library | Previous Version | New Version |
|---------|------------------|-------------|
| Nimbus JOSE+JWT | - | 10.3.1 |
| Byte Buddy | - | 1.17.6 |
| OpenTelemetry | - | 2.18.0 |
| Jackson | - | 2.19.2 |
| Apache CXF | - | 3.6.7 |
| gRPC | - | 1.73.0 |
| Log4j | - | 2.25.1 |
| Maven API | - | 3.9.11 |
| AWS SDK | - | 2.32.4 |
| Azure Storage Blob | - | 12.31.0 |
| Netty | - | 4.2.3.Final |
| MS SQL JDBC | - | 12.10.1.jre11 |
| JUnit | - | 5.13.3 |
| Mockito | - | 5.18.0 |
| Datasource Proxy | - | 1.11.0 |
| XMLUnit | - | 2.10.3 |
| TestContainers | - | 1.21.3 |
| Oracle JDBC | - | 23.8.0.25.04 |
| PostgreSQL JDBC | - | 42.7.7 |
| Commons Codec | - | 1.18.0 |
| Commons Compress | - | 1.27.1 |
| Commons Collections4 | - | 4.5.0 |
| Commons IO | - | 2.20.0 |
| Commons JXPath | - | 1.4.0 |
| Commons Lang3 | - | 3.18.0 |
| Bouncy Castle | - | 1.81 |
| Groovy | - | 4.0.27 |

---

## Live Demo

Try OpenL Tablets online: https://openl-tablets.org/demo

---

## Getting Help

- **Documentation**: https://openl-tablets.org/documentation
- **Community Forum**: https://openl-tablets.org/forum
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Commercial Support**: contact@openl-tablets.org

---

## Appendix: Ticket References

### Version 5.27.14

| Ticket | Summary |
|--------|---------|
| EPBDS-15181 | Define correct classloader for the Spring Context |
| EPBDS-15276 | Update dependencies |

### Version 5.27.13

| Ticket | Summary |
|--------|---------|
| EPBDS-15181 | Migrate to the latest Swagger and fix model converters |
| EPBDS-15181 | Update dependencies |
| EPBDS-15116 | Fix unpredictable order of AccessDeniedHandler |
| EPBDS-14233 | Use @TempDir & @AutoClose instead of manual temp directory management |
| EPBDS-10560 | Remove duplicated test |
| EPBDS-11081 | Clean up unused repo-file.support-deployments property |
| EPBDS-15094 | Update Release workflow due changes on the Sonatype portal |

### Version 5.27.12

| Ticket | Summary |
|--------|---------|
| EPBDS-15056 | Java 25 Support |
| EPBDS-15098 | Example how to generate Java sources from the Datatype tables |
| EPBDS-15078 | Fix to support Spring 6 |
| EPBDS-14965 | Upgrade to the latest version of RichFaces |
| EPBDS-14433 | Remove workaround with upgrading jQuery |
| EPBDS-14407 | Remove workaround with upgrading jQuery |
| EPBDS-15096 | Fix DoubleRange.contains() for NaN |
| EPBDS-14622 | Fix method graph for overloaded tables |
| EPBDS-14621 | Fix method graph for multicall method |
| EPBDS-14977 | Fix NPE when using dependencies in non-branch enabled repositories |
| EPBDS-15055 | Update libraries |
| EPBDS-15094 | Update Release workflow due changes on the Sonatype portal |
