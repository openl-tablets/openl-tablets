# EPBDS-16640 — the cell values the editor reads

A cell of a column the engine types as a number may still hold text its author wrote. The engine refuses
such a cell and says so, and the editor has to show what stands there — otherwise the author is shown a
value they never wrote and cannot find the text the message is about.

`010-setup` creates a project whose decision table returns a `Double`: one row holds `0.3` written as text,
which is that number, and another holds `1abc`, which is not a number at all.

`020-cell-values`:

- `010-text-in-a-number-cell-is-read-as-the-author-wrote-it` — the raw read answers `0.3` with the number
  the text stands for, and `1abc` with the text itself. Reading a number out of the front of it and
  answering `1` is what this pins against.

`999-tierdown` deletes the project.
