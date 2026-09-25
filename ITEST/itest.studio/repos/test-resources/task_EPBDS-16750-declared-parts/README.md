# EPBDS-16750 — a table says what a part of it holds, not what each cell holds

The editors endpoint answered cell by cell: it walked the window, asked the compiler what each cell held and
named the cell. But a table declares what a whole part of it holds — a Data table its columns, a decision
table its conditions and its actions, a lookup the cells its rules meet in — and every line written under that
declaration holds the same thing. So the answer repeated one editor down the whole table, and said nothing at
all about a line the reader had just laid down, or about a table whose columns are declared and which holds no
rows and no rules yet.

The answer now names those parts. It comes in two kinds: `raw`, for a table that can only be read as the cells
it holds, and `declared`, which carries `areas` — each a rectangle of the matrix and the editor its cells ask
for. A part left open along an axis (`rows` or `columns` null) runs to the table's edge and on past it, so a
line laid down there belongs to it too. A cell of its own is named only where it is written some other way than
the part it stands in. The Editor takes a cell's own answer first and falls back to its part.

`020-lines` — the Data and Test tables of the bundled **Tutorial 2**.

- `010-a-column-standing-for-the-values-themselves` — `Data Integer numbers`, whose one column is `this`: the
  table is a table of whole numbers, and the column holds what the table is a table of. This is the column the
  old code dropped, because `this` carries no field of its own to read a type from.
- `020-a-column-chosen-from-what-its-type-allows` — `Data Person person1`: `dob` is a date, `gender` is an
  alias datatype and is chosen from `Male`/`Female`. The three columns holding plain text are not named at all,
  and neither is any cell: twenty cells that used to be named one by one are now two parts and none.
- `030-a-table-written-the-other-way-round` — `Data Person person3`, the same table written vertically: the
  parts are rows 3 and 4, each running across from the third column.
- `040-every-row-of-the-table-goes` / `050-the-column-is-declared-all-the-same` — the five rows of `numbers`
  are taken away and the table is read again. Nothing stands in its body, and the column still says that what
  goes into it is a whole number.

`030-rules` — the decision tables of the bundled **Tutorial 1**.

- `010-a-condition-the-rules-run-down` — `DriverPremium2`, led by a `# Rule` column that names its rules: the
  return runs down its own column from the first rule, and the rule names and the two conditions holding text
  are not named at all.
- `020-a-table-written-the-other-way-round` — `Greeting1`, whose conditions are rows: each runs across from the
  fifth column, one row tall.
- `030-the-cells-a-lookups-rules-meet-in` — `DriverPremium4`, whose rules run down one condition and across
  another: what they meet in runs both ways, so a rule written either way joins it.
- `040-a-lookup-of-two-conditions-running-across` — `CarPrice`, with two rows of horizontal conditions above
  the cells the rules meet in.
- `050-a-table-whose-columns-carry-their-own-types` — `DriverPremium3`, a `SimpleRules` table whose columns
  carry no types of their own: they come from the method it is written for.
- `060-every-rule-of-the-table-goes` / `070-the-columns-are-declared-all-the-same` — every rule of
  `DriverPremium3` is taken away and the table is read again. It holds nothing but its titles, and the columns
  still say what a rule written under them would hold.

`040-smart` — the smart lookups of the bundled **Tutorial 8**, where the conditions hold types of their own
and every shape a part can take is answered for at once.

- `010-a-lookup-whose-rules-run-down-and-across` — `CoverageRate`: the answer carries all three shapes
  together — the condition the rules run down (`rows` null, one column), the one they run across (one row,
  `columns` null) and the cells they meet in (both null). The lookups of Tutorial 1 hold text in their
  conditions, so they name the meeting cells and nothing else; here the conditions are ranges and are named.
- `020-a-condition-holding-many-values-at-once` — `GaragingZipFactor`: the condition the rules run across is a
  range of whole numbers and the meeting cells hold decimals, while the condition they run down holds text and
  is not named at all.

`999-tierdown` closes all three projects and deletes them.
