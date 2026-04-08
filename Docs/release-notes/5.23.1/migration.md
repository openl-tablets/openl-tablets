---
title: OpenL Tablets 5.23.1 Migration Notes
---

### Quick Role-Based Pointers

* **If you are an Administrator / Platform Owner** → pay special attention to sections **1, 2**

---

### 1. WebStudio Property Changes

| Old Property                          | New Property                  |
|:--------------------------------------|:------------------------------|
| `default.openl.compatibility.version` | `openl.compatibility.version` |

A new property is introduced to track initial WebStudio configuration state:

```properties
webstudio.configured = false
```

---

### 2. Rule Services Property Changes

| Old Property                                        | Change                                                               |
|:----------------------------------------------------|:---------------------------------------------------------------------|
| `ruleservice.baseAddress`                           | **Removed**                                                          |
| `ruleservice.jackson.serializationInclusion=ALWAYS` | Changed to `ruleservice.jackson.serializationInclusion=USE_DEFAULTS` |
