---
title: OpenL Tablets 5.23.8 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Developer** → pay special attention to section **2**
* **If you are an Administrator / Platform Owner** → pay special attention to section **1**

---

### 1. Rule Services Property Changes

The following properties have been removed or renamed:

| Property                                           | Change                                                  |
|:---------------------------------------------------|:--------------------------------------------------------|
| `ruleservice.datasource.filesystem.supportVersion` | **Removed**                                             |
| `ruleservice.datasource.deploy.clean.datasource`   | **Removed** — use startup scripts instead               |
| `ruleservice.datasource.dir`                       | **Renamed** to `production-repository.uri`              |
| `ruleservice.tmp.dir`                              | **Removed** — replaced by the standard `java.io.tmpdir` |

---

### 2. Deprecated API Classes

The following classes are deprecated and must be migrated:

| Deprecated Class                                                | Migration Action                                                                                                                                                             |
|:----------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `org.openl.rules.ruleservice.core.interceptors.AnyType`         | Replace with `org.openl.rules.ruleservice.core.interceptors.RulesType`.                                                                                                      |
| `org.openl.rules.project.resolving.CWPropertyFileNameProcessor` | Remove any references from `rules.xml` files. The default implementation covers this functionality out-of-the-box.                                                           |
| `org.openl.rules.project.resolving.PropertiesFileNameProcessor` | Migrate custom implementations to use the new API. The default implementation covers multiple filename parsing scenarios and can be used instead of a custom implementation. |
