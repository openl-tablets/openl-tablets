# EPBDS-16765 — a line laid down in a vocabulary takes the type of its values

A vocabulary declares the type of its values in its header, so a value written into a line laid down under the
values is entered as that type too. The editors API answers for the values with an area that reaches past the
table's edge, as it answers for the columns of a Data table.

`010-setup` creates a project, opened, holding `Datatype Hour <Integer>` with the values 12 and 24.

`020-editors` — the values are one `declared` area, from the first row under the header on, of any number of rows
and columns, entered as whole numbers.
