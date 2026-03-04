---
title: OpenL Tablets 5.23.0 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Developer** → pay special attention to section **2**
* **If you are an Administrator / Platform Owner** → pay special attention to sections **1, 2, 3**

---

### 1. WebStudio Configuration Changes

#### Property File Consolidation

All separate configuration files (`rules-production.properties`, `db.properties`, `system.properties`) must be merged
into a single `application.properties` or `webstudio.properties` file.

#### Property Renames

| Old Property                   | New Property              |
|:-------------------------------|:--------------------------|
| `repository.encode.decode.key` | `secret.key`              |
| `webstudio.home`               | `openl.home`              |
| `design-repository.*`          | `repository.design.*`     |
| `production-repository.*`      | `repository.production.*` |
| `user.settings.home`           | **Removed**               |

#### Secret Key for Password Encoding

The new `secret.key` property enables encoding of password fields. Passwords ending with `.password` can be wrapped in
`ENC(encodedValue)` format for secure storage:

```properties
secret.key = mySecretKey
repository.design.password = ENC(encodedPassword)
```

#### Removed Repository Implementations

The following repository factory implementations have been removed. Migrate to Git-based repositories:

* `LocalJackrabbitRepositoryFactory`
* `WebDavRepositoryFactory`
* `RmiJackrabbitRepositoryFactory`

The default design repository factory is now:

```properties
repository.design.factory = org.openl.rules.repository.git.GitRepository
```

---

### 2. Rule Services Configuration Changes

#### Property Renames

| Old Property                                  | New Property                                   |
|:----------------------------------------------|:-----------------------------------------------|
| `ruleservice.logging.store.enabled`           | `ruleservice.store.logs.enabled`               |
| `ruleservice.logging.store.cassandra.enabled` | `ruleservice.store.logs.cassandra.enabled`     |
| `ruleservice.logging.store.elastic.enabled`   | `ruleservice.store.logs.elasticsearch.enabled` |

#### Removed Deprecated Property Files

The following deprecated property files have been removed:

* `rules-production.properties`
* `openl-ruleservice.properties`
* Related extension files

Migrate all settings to `application.properties`.

---

### 3. Repository Migration

JCR (Jackrabbit) repository implementation is no longer supported. Migrate all design repositories to Git by updating
the `repository.design.factory` property.
