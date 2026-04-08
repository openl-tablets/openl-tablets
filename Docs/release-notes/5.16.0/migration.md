---
title: OpenL Tablets 5.16.0 Migration Notes
---

## OpenL Rules — String Concatenation with `+` Operator

Avoid using the `+` operator to concatenate numeric and string values. This behavior has been reverted in 5.16.1. Use
explicit string conversion functions instead.
