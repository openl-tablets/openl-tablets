---
title: OpenL Tablets 5.22.2 Migration Notes
---

### Quick Role-Based Pointers

* **If you are a Rules Author** → pay special attention to section **1**

---

### 1. Null Behavior Change in Multiplication and Division

The behavior for `null` values in multiplication and division operations has changed. Previously, `null` was treated as
`1` in these operations.

To restore the legacy behavior, add the following import statement to your rules:

```java
import org.openl.rules.binding.MulDivNullToOneOperators.*;
```
