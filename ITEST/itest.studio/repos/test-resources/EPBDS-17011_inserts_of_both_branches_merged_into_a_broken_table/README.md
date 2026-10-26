# Rows and columns both branches insert into a table, merged into a table that compiles

The setup creates, through the tables API, the table `discount` with the property `category` set to `Auto`, and the
decision table `risk`, whose rules of US share the country merged over them. Each scenario branches from master,
changes the same table in the side branch and in master through the tables API, and receives the side branch into
master. Sync merges every scenario by itself.

| Scenario | Changes | Result |
|----------|---------|--------|
| `020-properties-added-in-both` | The side branch sets `lob` to `Auto`, master sets `description` | one properties cell over `description`, `lob` and `category` |
| `030-rules-inserted-into-a-group` | Each branch inserts a rule of US among the rules of US | the country of US merged over the rules of both |
| `040-conditions-added-in-both` | Each branch adds a condition before the return value, named `C3` | the condition of master keeps `C3`, the one of the side branch is `C4` |

The tables API writes a new property above the properties a table has, so each branch inserts a row right under the
header of `discount`. The value of `lob` is the value of `category` on purpose: the row of `lob` then shares as many
values with the row of `category` as the row the properties cell stretched over. Sync used to take one row for the
other, merge the workbook with no conflict and leave `category` outside the properties cell, which broke the table.
It also used to keep both conditions named `C3`, which a decision table cannot define twice.
