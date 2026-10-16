# Example 5 - Markdown Rules

Rules written in Markdown files instead of Excel workbooks. The project rates the yearly premium of a car insurance
policy.

- `rules/Model` — the datatypes of a policy, a driver and a vehicle.
- `rules/Rating` — the rating factors in `Factors`, the premium calculation and the premium level in `Premium`.
- `tests` — the policies to rate in `Policies`, and the tests of the rules.

`rules.xml` declares each folder as a module. This file is in no module folder, so it is no part of any module.

## How a module is written

- A module is a folder. Every Markdown file in it and in its sub-folders holds one table of the module.
- A sub-folder groups the tables in it: OpenL Studio shows them under a category named after the sub-folder. The
  tables at the root of the module are under a category named after the module folder.
- The tables that set up the whole module are written once, at the root of the module folder: the Environment table
  in `Environment.md`, the Properties table in `Properties.md` and the Constants table in `Constants.md`.
- The front matter of a file is the header of its table: the `table` key holds the text of the first table cell.
  The other keys are table properties, such as `description`. The `merges` key lists the merged cells of the table,
  such as `[A1:B1]`, counted from the first row of the table.
- The properties take three columns of the table, as in a workbook. A body narrower than that has the last cell of each
  of its rows merged over the columns it lacks; a body that keeps these columns apart writes them as empty columns.
- The first table after the front matter is the body of the table: every row of it, its first row included, is a
  row of the rules table.
- A cell is taken as written. A cell wrapped in backticks as a whole is unwrapped, `\|` is a vertical bar and `<br>`
  a line break.
- Everything else in a file is documentation. A file without front matter is documentation as a whole.

In OpenL Studio the tables of a Markdown module are shown, run, tested, traced and edited as any others, all but their
look: a Markdown file keeps no formatting. A file is also edited as text, on the **Files** tab of the project.
