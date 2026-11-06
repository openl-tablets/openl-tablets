# A group line kept when the first rule of the group is removed

A SmartRules table groups its rules by merged condition values: THEFT over two rules, COMPREHENSIVE over four, and
each First Party value of COMPREHENSIVE over two. **Reformat Excel files** draws a dotted line above each group, so
the raw read of the table carries a `dotted` top border on the first row of each.

Removing the first rule of COMPREHENSIVE saves the table, formatted with no compile. The rule left first in the
group keeps the dotted line above it, which a save lost before EPBDS-17039.
