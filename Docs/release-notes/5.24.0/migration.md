---
title: OpenL Tablets 5.24.0 Migration Notes
---

## Rules Authors

### WebStudio Project History and Local Changes

Project change history created before the migration to 5.24.0 will be unavailable after the upgrade. The underlying
files remain intact, but no **Local Changes** records or restore capability will exist for pre-migration history.

**Required action**: Save or export any necessary unsaved changes before upgrading.

Migration from 5.23.x to 5.24.0 is automatic. If upgrading from an earlier version, migrate step-by-step to the latest
5.23.x minor version first, then to 5.24.0 (for example: 5.22.7 → 5.23.11 → 5.24.0).

## Administrators

### Administrator User Removed

The default `admin` user is deleted after migration to 5.24.0.

**Required action**: Configure a new administrator via the `security.administrators` property in `webstudio.properties`
before or immediately after upgrade.

### Rule Services Property Changes

The following `application.properties` changes are required for Rule Services:

| Property                                               | Change                                                                                        |
|:-------------------------------------------------------|:----------------------------------------------------------------------------------------------|
| `ruleservice.datasource.filesystem.supportDeployments` | **Removed** — delete from configuration                                                       |
| `production-repository.support-deployments`            | **Removed** — delete from configuration                                                       |
| `ruleservice.instantiation.strategy.lazy`              | **Changed default from `true` to `false`** — set explicitly if lazy instantiation is required |

Deployed service uniqueness is now determined by **Deploy path** instead of **Service name**. If your integrations
reference services by name, verify that service names remain consistent after upgrade.
