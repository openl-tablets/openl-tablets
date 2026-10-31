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
  in `Environment.md` and the Constants table in `Constants.md`. A Properties table is a table like any other: one of a
  category stands in the folder of the category, as it stands on the sheet of the category in a workbook.
- The front matter of a file is the header of its table: the `table` key holds the text of the first table cell.
  The `properties` key holds the table properties, a name and a value on each line under it, such as
  `tags: rating`. The `merges` key lists the merged cells of the table, a range to a line
  under it, such as `- A1:B1`, counted from the first row of the table. A range added or taken away is a line of its
  own, so two changes of one table merge as text. Any other key is an error, and the table is read without it.
- The text between the front matter and the table is the description of the table, such as `Yearly premium of a
  policy.` in `premium.md`. A quote, a block of lines starting with `>`, is a comment for the authors and no part of
  the description; so is an HTML comment. The description is never written under `properties`.
- The properties take three columns of the table, as in a workbook. A body narrower than that has the last cell of each
  of its rows merged over the columns it lacks; a body that keeps these columns apart writes them as empty columns.
- The first table after the front matter is the body of the table: every row of it, its first row included, is a
  row of the rules table. A table with no body, such as a `Datatype` that adds no field to its parent, is its front
  matter and its description.
- A cell is taken as written. A cell wrapped in backticks as a whole is unwrapped, `\|` is a vertical bar and `<br>`
  a line break.
- A cell is indented by two `&nbsp;` before its text for each level, as a workbook indents it: `&nbsp;&nbsp;Fee` is
  `Fee` one level in.
- A whole number, `true` and `false`, and a date written as `2026-03-02` or `2026-03-02T10:30:15` read as the
  number, the boolean and the date a cell of a workbook holds, so a `Spreadsheet` step of such a value takes the type
  it takes in a workbook. Any other text stays text, such as `007`, `3.0`, `TRUE` or `03/02/2026`: the type a table
  declares reads it, and a number reads as a `Double` where none is declared.
- The text after the table is documentation. A file without front matter is documentation as a whole.
- A module path with a wildcard, such as `rules/Auto-*`, makes a module of every folder of Markdown files it
  matches, as it makes one of every file it matches.
- The file name pattern of a project (`properties-file-name-pattern`) reads the properties of a module folder from
  its path, as it reads those of a workbook from its file name, and every table of the module takes them. With the
  pattern `%lob%-%state%`, the folders `rules/Auto-CA` and `rules/Auto-NY` are the versions of a module for two
  states. The files inside a module folder are not matched: a table writes its own properties in its front matter.

In OpenL Studio the tables of a Markdown module are shown, run, tested, traced and edited as any others, all but their
look: a Markdown file keeps no formatting. With **Excel Formatting** on, a table is drawn in the OpenL table style, as
a table of a workbook formatted on save; the style is never written into the file. A file is also edited as text, on
the **Files** tab of the project, where **Open in Editor** opens the table of the file in the editor.

## Format errors

- An error of a Markdown file reads as the error of a workbook: its text tells what is wrong, and its link leads to the
  place.
- A file whose front matter breaks the format holds no table. The error points at the line of the file, and OpenL
  Studio opens the file on that line, on the **Files** tab.
- A table whose body breaks the format is read without the part that breaks it: a merged range over a value, over
  another range or outside the table, the cells of a row beyond those of the first row, the rows a blank line cuts
  off. So is a table whose front matter holds a key other than `table`, `properties` and `merges`, such as a property
  written beside `table` rather than under `properties`. The error points at the cell of the table that shows the
  problem, or at the line of the file when no cell shows it.
