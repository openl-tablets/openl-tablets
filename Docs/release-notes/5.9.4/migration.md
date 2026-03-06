---
title: OpenL Tablets 5.9.4 Migration Notes
---

## User Workspace Location

Previous releases used two workspace locations. This release standardizes to a single location:

| Mode        | Location                                                   |
|:------------|:-----------------------------------------------------------|
| Single-user | `[OPENL_HOME]/user-workspace` (root level)                 |
| Multi-user  | `[OPENL_HOME]/user-workspace/[USERNAME]` (username folder) |

If a custom workspace location was configured, reconfigure it via the Administration UI after upgrading.
