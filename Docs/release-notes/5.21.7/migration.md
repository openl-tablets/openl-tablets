---
title: OpenL Tablets 5.21.7 Migration Notes
---

## Rule Service Configuration Changes

The `ruleservice.datasource.type` property has been removed. Replace configuration as follows:

| Old Value                             | New Configuration                                                                   |
|:--------------------------------------|:------------------------------------------------------------------------------------|
| `ruleservice.datasource.type = local` | `production-repository.factory = org.openl.rules.repository.LocalRepositoryFactory` |

Remove any `ruleservice.useRuleServiceRuntimeContext` references from `rules-deploy.xml`.

## OpenL Rules Changes

* Remove `Custom1`, `Custom2`, and `Transactional` properties from rules — they are no longer supported.
* Remove commas from LOB names.
* Generic types are not supported (for example, `Map<String, String>`).
* Generated getter/setter methods now follow the JavaBeans v1.01 specification.
