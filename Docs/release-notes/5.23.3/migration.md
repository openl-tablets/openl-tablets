---
title: OpenL Tablets 5.23.3 Migration Notes
---

### Quick Role-Based Pointers

* **If you are an Administrator / Platform Owner** → pay special attention to sections **1, 2**

---

### 1. WebStudio Configuration Changes

The "Unlimited number of copies" setting is automatically set to `false` after migrating to this version.

| Property                    | Before | After                                     |
|:----------------------------|:-------|:------------------------------------------|
| `project.history.count`     | `0`    | `100`                                     |
| `project.history.unlimited` | `true` | **Removed** — use `project.history.count` |

---

### 2. Rule Services Property Changes

The following properties have been renamed:

| Old Property                                  | New Property                                             |
|:----------------------------------------------|:---------------------------------------------------------|
| `ruleservice.aegisbinding.readXsiTypes`       | `ruleservice.aegis.readXsiTypes`                         |
| `ruleservice.aegisbinding.writeXsiTypes`      | `ruleservice.aegis.writeXsiTypes`                        |
| `ruleservice.aegisbinding.ignoreNamespaces`   | `ruleservice.aegis.ignoreNamespaces`                     |
| `ruleservice.deployer.enable`                 | `ruleservice.deployer.enabled`                           |
| `ruleservice.jackson.defaultTypingMode=SMART` | `ruleservice.jackson.defaultTypingMode=JAVA_LANG_OBJECT` |
