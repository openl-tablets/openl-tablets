# EPBDS-16904 — a field bound to a context property keeps the link to its type

The type of a Datatype field bound to a runtime context property (`name : context.property`) is reported among the
usages of its cell as a `datatype` usage, with the table that declares the type, the same as for a field bound to
nothing. The default value of such a field is reported as a value of that type. A screen draws the type as a link to
its table, and edits the default value as one of the values of the type.

`010-setup` creates a project, opened, from the archive attached to the ticket. It holds the vocabulary `State`
(`CA`, `NY`) and the Datatype table `Policy`, whose field `situsState` is bound to the context property `usState`
and whose field `situsState2` is bound to nothing.

`020-meta-info` — both `State` cells, `B7` and `B8`, report `State` as a usage of the datatype table `State`, and
both default values, `D7` and `D8`, report the type `State`.
