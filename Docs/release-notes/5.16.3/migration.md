---
title: OpenL Tablets 5.16.3 Migration Notes
---

## Production Repository Configuration Property Changes

The following properties have been unified under a single `production-repository.uri` property:

| Old Property                              | New Property                |
|:------------------------------------------|:----------------------------|
| `production-repository.local.home`        | `production-repository.uri` |
| `production-repository.remote.rmi.url`    | `production-repository.uri` |
| `production-repository.remote.webdav.url` | `production-repository.uri` |
| `production-repository.db.url`            | `production-repository.uri` |

The following properties have been removed:

* `production-repository.jcr.nodetypes`
* `production-repository.config`
* `production-repository.name`

Repository factory classes have been refactored. Update any references to `DBProductionRepositoryFactory`,
`JdbcDBRepositoryFactory`, WebDAV, RMI, and local Jackrabbit repository factory class names accordingly.
