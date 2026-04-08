---
title: OpenL Tablets 5.21.4 Migration Notes
---

## OpenL Rules Changes

Test tables no longer support business dimensional properties. Replace business dimensional property definitions with
the `_context_` input parameter.

Test table expectations must define either one result or an error exclusively, not both at the same time.
