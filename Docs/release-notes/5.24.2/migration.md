---
title: OpenL Tablets 5.24.2 Migration Notes
---

## Rules Authors

### Condition Expression Parameter Matching Priority Change

The priority of parameter matching in condition expressions has changed when a parameter name's capitalization differs
between its declaration and its usage.

Review any rules tables where parameter names are referenced with different capitalization than their declared form, and
update them to ensure correct matching behavior after upgrade.

![Parameter Matching Priority Change](images/parameter-matching.png)
