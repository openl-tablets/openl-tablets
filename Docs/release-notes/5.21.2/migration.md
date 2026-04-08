---
title: OpenL Tablets 5.21.2 Migration Notes
---

## Rule Services — SOAP v1.1 Strict Validation

Due to strict SOAP v1.1 validation, `null` values must be formatted as:

```xml
<aField xsi:nil="true"/>
```

Update any SOAP clients that send `null` values to use this format.
