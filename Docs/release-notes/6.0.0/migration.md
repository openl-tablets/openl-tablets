---
title: OpenL Tablets 6.0.0 Migration Notes
---

## Rules Authors

### Rules & Rule Execution

* The **variations** feature has been completely removed.
    * Variation-based endpoints are no longer created.
    * Rules and services relying on variations must be rewritten using array-based or explicit rule logic.
* The default value of `isProvideRuntimeContext` is now **false**.
    * Rules that relied on implicit runtime context (for example, `currentDate`, `requestId`) may receive `null` unless
      enabled explicitly.

### Metadata & OpenAPI

* Project tags are now stored inside the project structure.
    * Tags are migrated automatically.
    * Tag changes are version-controlled and visible in Git history.
* Rule and datatype descriptions are automatically exposed in generated OpenAPI documentation.

### Removed Configuration Properties & Legacy Formats

* `custom.spreadsheet.type` — removed. Custom `SpreadsheetResult` types are always generated; the legacy
  `custom.spreadsheet.type=false` mode no longer exists.
* `dispatching.mode` — removed. Overloaded-method dispatching always uses the Java approach.
* `language` property — removed. Only the default OpenL language remains.
* `OpenL.properties` configuration files are no longer loaded. Move any settings to system properties or environment
  variables.
* Deprecated operator classes `StringOperators`, `WholeNumberDivideOperators`, and `MulDivNullToOneOperators` have been
  removed.
* Legacy `rules.xml` (pre-5.25 format, deprecated tags, and the `CWPropertyFileNameProcessor` reference) is migrated
  automatically on first save. No manual action is required.

## Developers

### Runtime Environment

* Java **21** is required. Earlier Java versions are no longer supported.
* All `javax.*` packages have been replaced with `jakarta.*`.
  Custom Java code must be updated accordingly and all dependencies must be Jakarta EE 10 compatible.
* The `org.openl.core` and `org.openl.grammars` Maven modules have been merged into `org.openl.rules`.
    * Code that embeds OpenL as a library must update its dependencies to `org.openl.rules`.

### Integration & Serialization

* JSON polymorphic serialization now uses **NAME-based** type identification instead of **CLASS-based**.
    * Client applications relying on CLASS-based typing may require updates.
* OAuth2 authentication now uses the `sub` claim as the default username attribute.
    * Existing OAuth2 integrations should be verified after upgrade.
* Embedded JDBC drivers are no longer included.
    * Required database drivers must be provided explicitly.
* Rule Services customization annotations have been removed: `@AnyType`, `@InjectOpenClass`, `@InjectOpenMember`,
  `@InjectRulesDeploy`, `@InjectServiceClassLoader`, and `@MixInClassFor`.
    * Custom service interfaces relying on these annotations must be reworked.
* Several public annotations, including `@ContextProperty`, have moved to the `org.openl.rules.annotations` package.
    * Update imports in custom Java code accordingly.

## Administrators

### Access Control & Permissions

* The legacy permission model has been replaced with role-based ACL.
  Permissions are migrated automatically, but effective access may differ after upgrade.
* `security.default-group` no longer grants implicit READ access to repositories.
  Administrators must explicitly configure permissions for the default group after upgrade.
* Project creation and deletion are additionally controlled by the system property:

```properties
security.allow-project-create-delete = true  # Default (allows creation)
security.allow-project-create-delete = false # Hides create/delete buttons
```

* Role assignments and default group permissions should be reviewed after upgrade.

#### User Access & Permission Mapping

| Legacy Permission                                   | New Behavior                 | Notes / Action                                                                                  |
|:----------------------------------------------------|:-----------------------------|:------------------------------------------------------------------------------------------------|
| **View Projects**                                   | Included in all roles        | Deprecated as a standalone permission; project viewing is now available to all roles by default |
| **Run and Trace Tables**                            | System action                | No longer permission-restricted; available to all users regardless of role                      |
| **Edit Projects**                                   | Included in Contributor role | Covered by the Edit permission                                                                  |
| **Create Projects**                                 | Separate Create permission   | Included in Contributor and Manager roles; can be globally restricted                           |
| **Delete Projects**                                 | Separate Delete permission   | Included in Contributor and Manager roles                                                       |
| **Erase Projects**                                  | Merged into Delete           | Delete and erase are now the same action                                                        |
| **Lock / Unlock Projects**                          | Removed                      | Project locking functionality was deprecated                                                    |
| **Deploy Configuration (Create/Edit/Delete/Erase)** | System action                | Available to users with Viewer access to Design repo and Edit access to Deploy repo             |

### User Email and Display Name

* A deploy asks for the email address and the display name of the user: up to 7.0.x a deploy to a deployment
  repository of any type, since 7.1.0 only a deploy to a Git deployment repository, which records them in its commits.
  OpenL Tablets 5.x keeps neither, so every user upgraded from 5.x, administrators included, has both empty and is
  refused such a deploy until they are filled.
    * Before 7.0.0 the **Deploy** dialog shows only **Deploy Failed — Failed to deploy configuration. Please try
      again.** The REST API answers `400` with `Email and display name must be configured before this operation.`
    * This refusal is about the profile, not about access rights: the groups and roles migrated from 5.x keep their
      right to deploy. A deploy refused for missing access answers `403` instead, and the **Deploy** dialog says
      **You do not have permission to deploy to the selected repository.** For the access that may change on
      upgrade, such as the implicit READ access of `security.default-group`, see **Access Control & Permissions**.
* Fill the email address and the display name of every user who deploys to a Git deployment repository, or, up to
  7.0.x, to a deployment repository of any type:
    * a user enters **Email** and **Display Name** in **My Profile**; since 6.4.0, OpenL Studio asks for them right
      after sign-in;
    * an administrator enters them for a user in **Administration > Users**;
    * with SAML or OAuth2, OpenL Studio takes them from the identity provider at every sign-in. Name the attributes
      the identity provider sends, in **Administration > Security** (**Attribute for Email**, **Attribute for Display
      Name**) or in the properties:

```properties
security.saml.attribute.email = urn:oid:0.9.2342.19200300.100.1.3
security.saml.attribute.display-name = urn:oid:2.16.840.1.113730.3.1.241
security.oauth2.attribute.email = email
security.oauth2.attribute.display-name = name
```

### Administration, Deployment & Removed Features

* The following components and protocols are no longer supported:
    * RMI
    * CAS SSO
    * Legacy APIs
    * Install Wizard
* The Administration UI has been fully redesigned.
    * No functional loss; administrators should familiarize themselves with the new layout.
* Demo distribution and deployment packaging have changed.
    * No breaking impact for production systems; primarily affects evaluation and onboarding scenarios.
