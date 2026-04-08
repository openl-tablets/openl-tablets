---
title: OpenL Tablets 5.7.5 Migration Notes
---

## NumberValue Null Arithmetic

Arithmetic operations on `NumberValue` types now treat `null` values as errors instead of zero. Review and update rules
that relied on the previous behavior of treating `null` as zero in arithmetic expressions.
