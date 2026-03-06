---
title: OpenL Tablets 5.17.0 Migration Notes
---

## Configuration Property Changes

The following production repository configuration properties have been renamed or removed:

| Old Property (5.16.2 and earlier)                                                  | New Property (5.17.0+)                                                  |
|:-----------------------------------------------------------------------------------|:------------------------------------------------------------------------|
| `production-repository.local.home`                                                 | `production-repository.uri`                                             |
| `production-repository.remote.rmi.url`                                             | `production-repository.remote.webdav.url`                               |
| `production-repository.db.url`                                                     | `org.openl.rules.repository.factories.JdbcDBRepositoryFactory`          |
| `org.openl.rules.repository.factories.DBProductionRepositoryFactory`               | `org.openl.rules.repository.factories.WebDavRepositoryFactory`          |
| `org.openl.rules.repository.factories.WebDavJackrabbitProductionRepositoryFactory` | `org.openl.rules.repository.factories.RmiJackrabbitRepositoryFactory`   |
| `org.openl.rules.repository.factories.RmiJackrabbitProductionRepositoryFactory`    | `org.openl.rules.repository.factories.LocalJackrabbitRepositoryFactory` |
| `org.openl.rules.repository.factories.LocalJackrabbitProductionRepositoryFactory`  | *(removed)*                                                             |
| `production-repository.jcr.nodetypes`                                              | *(removed)*                                                             |

## OpenL Rules — Empty Cell Behavior

Empty cells in Data and Test tables now return `null` instead of empty collections. Review rules and tests that rely on
empty collections returned from blank cells and update accordingly.
