---
title: OpenL Tablets 5.23.1 Migration Notes
---

## Administrators

### WebStudio Property Changes

| Old Property                          | New Property                  |
|:--------------------------------------|:------------------------------|
| `default.openl.compatibility.version` | `openl.compatibility.version` |

A new property is introduced to track initial WebStudio configuration state:

```properties
webstudio.configured = false
```

### Rule Services Property Changes

| Old Property                                        | Change                                                               |
|:----------------------------------------------------|:---------------------------------------------------------------------|
| `ruleservice.baseAddress`                           | **Removed**                                                          |
| `ruleservice.jackson.serializationInclusion=ALWAYS` | Changed to `ruleservice.jackson.serializationInclusion=USE_DEFAULTS` |
