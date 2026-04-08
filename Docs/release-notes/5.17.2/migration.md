---
title: OpenL Tablets 5.17.2 Migration Notes
---

## Rounding Behavior Change for Double

`round(value)` and `round(value, scale)` functions for `Double` now align with the IEEE 754 specification and may behave
differently on boundary cases.

If you need the legacy behavior — which disregards Unit of Least Precision (ULP) arithmetic inaccuracies — use the new
`roundStrict(value)` and `roundStrict(value, scale)` functions instead.
