# EPBDS-12301 — a type named as a value leads to the table declaring it

The name of a type written where a value stands, such as a vocabulary passed to `getValues`, is reported among the
usages of its cell as a `datatype` usage, with the table that declares the type, as it is where it is written as the
type of a parameter. A screen draws it as a link to that table, with a hint.

`010-setup` creates a project, opened, holding the vocabulary `Color` and the spreadsheet `colors`, whose cell
`C8` reads `=getValues(Color)`.

`020-meta-info` — the cell reports `Color` (characters 11 to 16) as a usage of the datatype table `Color`.
