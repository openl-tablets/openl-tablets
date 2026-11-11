# OpenL Tablets — Markdown Tables

A module can be a folder of Markdown files instead of an Excel workbook: one table per file. The engine reads it into
the same tables a workbook gives, so the tables compile, run, test and trace alike, with the same messages.

## Module Folder

```
rules/
  Rating/                  - a module folder: rules-root path="rules/Rating"
    Environment.md         - the Environment table of the module, at its root only
    Constants.md           - the Constants table of the module, at its root only
    Factors/               - a category: the tables of a sub-folder lie on a sheet named after it
      ageFactor.md         - a table, the file named after it
      ageFactor1.md        - a second table of the same name
    Premium/
      premium.md
    README.md              - a file without front matter: documentation, no table
```

| Rule | Detail | Example |
|---|---|---|
| Declared in `rules.xml` | A path to the folder, or a mask that matches folders holding a file of a table | `rules/Rating`, `rules/*` |
| Matched by a mask | A folder of a file of a table, a front matter with `table`; a workbook in it is a module of its own | `rules/Docs/README.md` is none |
| Not in the default masks | Without `<modules>` only `rules/**/*.xlsx` and `tests/**/*.xlsx` are modules | — |
| Module name | The name of the folder, dots included | `rules/Rates.v2` is `Rates.v2` |
| Sheet name | The folder of the file; the module folder for a file at its root | `Factors` |
| Reading order | Paths in natural order: a number in a name by its value | `rate.md`, `rate1.md`, `rate2.md`, `rate10.md` |
| File name | The name of the table; a taken name gets a number | `premium.md`, `premium1.md` |
| Module tables | `Environment.md` and `Constants.md` at the root, nothing else in them | — |
| Properties table | A table like any other; one of a category stands in the folder of the category | `Factors/Properties.md` |
| Empty folder | The error `The module folder holds no Markdown file.` | — |
| File name pattern | `properties-file-name-pattern` reads the properties of a module from the folder path | `rules/2025-01-01_US` |

- Workbook modules and Markdown modules live in one project and use each other.
- A table id depends on the module folder and the table, not on the file: renaming or moving a file inside the folder
  keeps the id; renaming the folder gives new ids.

## File

```
---
table: SmartRules Double ageFactor(Integer age)
properties:
  tags: age
merges:
  - A1:B1
---

The younger or the older the driver, the higher the risk.

> A comment for the authors: no part of the description.

| Age |     | Factor |
|-----|-----|--------|
| 0   | 25  | 1.5    |
| 25  | 65  | 1      |
| 65  | 120 | 1.2    |

Text after the table is documentation.
```

| Part | What it holds | Rule |
|---|---|---|
| Front matter | The header of the table, YAML between two `---` lines | Starts on the first line of the file |
| Description | The text between the front matter and the body: the `description` property of the table | Taken as written; blocks of text joined with a blank line |
| Comment | A quote (`> …`) or an HTML comment (`<!-- … -->`) above the body | Not compiled, no part of the description |
| Body | The first GFM table, or the first code block, after the front matter | Every row is a body row, the first one too |
| Parts of a Spreadsheet | The GFM tables after the body that repeat its first row, a caption above each | See [Spreadsheet Captions](#spreadsheet-captions) |
| Code block | The body of a `Method` table, one cell | Any fence language; a new file writes `java` |
| No body | A table of a header alone; the text after its front matter is its description | `Datatype Adult extends Driver` |
| Text after the body | Documentation: not compiled | Paragraphs, headings, tables |

- One table per file: only the first GFM table or code block is the body, a Spreadsheet going on in its parts. Rows a
  blank line cuts off it are a problem.
- A file without front matter is documentation as a whole, even with a GFM table in it. So is a file whose front
  matter has none of the keys `table`, `properties` and `merges`, such as a page of a site with its `title`.
- A line of a description starting with `>`, a code fence or `|` is written with `\` before it, as Markdown escapes it,
  so it is not taken for a comment, a code block or the body.

## Front Matter Keys

| Key | Value | Example |
|---|---|---|
| `table` | The text of the first table cell, the header | `table: Rules String greeting(Integer hour)` |
| `properties` | The table properties but the description, `name: value` one to a line under it | `properties:` then `  tags: rates` |
| `merges` | The merged ranges of the body, one to a line under it, in A1 notation; none in a Data, Test or Run table | `merges:` then `  - A1:B1` |

- Any other key is a problem: the table is read without it, and the key stays in the file as written.
- The description is never a property of the front matter: a `description` under `properties` is a problem, and the
  table is read without it.
- Values are read as written: `NO` stays `NO`; a list is joined with commas.
- A YAML comment, `# SPDX-License-Identifier: Apache-2.0`, may stand in the front matter.

## Merged Ranges

| Rule | Detail |
|---|---|
| Counted from the body | `A1` is the first cell of the first body row; the header and properties are not counted |
| One to a line | A save writes them in reading order: by the row of the first cell, then by its column |
| The same ranges | Kept as written while they stay the same, in whatever order the file lists them |
| Covered cells | Left empty in the body; the first cell of the range holds the value |
| Outside the table | Problem `The merged range A1:C1 is outside the table`, range left out |
| Overlapping | Problem `The merged ranges A1:B1 and B1:B2 overlap`, range left out |
| Over a value | Problem `The cell B1 has a value, but the merged range A1:B1 covers it`, range left out |
| In a Data, Test or Run table | Error: the table holds no merged cells, a record is one row; every range left out |

## Spreadsheet Captions

A Spreadsheet groups its steps under captions: in a workbook, a row whose first cell holds the caption and is merged
across the table. A file writes the table in parts instead, the caption between two of them as a paragraph, so the
caption needs no merged range.

```
---
table: Spreadsheet SpreadsheetResult premium(Double base)
---

| Step     | Value           |
|----------|-----------------|
| Base     | `= base`        |
| Discount | `= $Base * 0.1` |

Final Calculation

| Step     | Value                 |
|----------|-----------------------|
| Total    | `= $Base - $Discount` |
```

| Rule | Detail |
|---|---|
| A part | A GFM table after the body that repeats the first row of the table, the names of its columns |
| A caption | A paragraph between two parts: a row of the body, its text in the first cell, merged across the table |
| Blank lines | Around the caption: a line right under a row is a row of the table, as GFM reads it |
| The repeated first row | Read once: it is no row of the table |
| Several captions | Paragraphs one after another between two parts, a row each |
| A part of the first row alone | Allowed: a table that ends with a caption ends with such a part |
| Merged ranges | `merges` counts the rows of the table put together: the captions in, the repeated first rows out |
| Text after the last part | Documentation, as after the body of any table |
| Table types | `Spreadsheet` and `Calc` alone; a table of another type is one GFM table |

- A table after a Spreadsheet that does not repeat its first row is a problem, and the table is read without it.
- A save writes a row merged across a Spreadsheet, with a value in its first cell alone, as a caption. A caption
  that would read back as other Markdown, such as `# Total`, `- Total` or a text of two lines, stays a merged range.
- The caption is no step of the Spreadsheet, as a row of a workbook merged over every titled column is none: the
  result has no field for it, and a formula that refers to it does not compile.

## Records of Data and Test Tables

A record of a Data, Test or Run table is a row of the file, and none of its cells is merged: a `merges` key in such a
table is an error, and the table is read without the ranges. An element of an array or a list field takes columns of
its own, with its index in the name of the field: `drivers[0].name`, `drivers[1].name`, and an element of a nested
collection, `drivers[0].vehicles[1].vin`.

```
---
table: Data Policy policies
---

| id | drivers[0].name | drivers[0].vehicles[0].vin | drivers[0].vehicles[1].vin | drivers[1].name |
|----|-----------------|----------------------------|----------------------------|-----------------|
| Id | Driver          | VIN                        | VIN                        | Driver          |
| P1 | Ann             | V1                         | V2                         | Bob             |
| P2 | Cid             | V4                         |                            |                 |
```

| Rule | Detail |
|---|---|
| Blank columns of an element | No element: `P2` has one driver and one vehicle |
| A blank element before a filled one | The collection holds `null` at the index of the blank element |
| An array of values | Its values in one cell, apart by commas: `speeding, parking` |
| The expected result of a test | `_res_.drivers[1].name`; a blank cell expects no value there |

- A workbook record of several rows, its collections given row by row and its cells merged over them, is written as
  one row of the file: each element of a collection the rows define gets columns of its own, the columns of a nested
  collection after the columns of its element. Each column keeps its title, and the values of an array field the rows
  give are joined in one cell. A column naming the key of the elements of a collection, `drivers._PK_`, is left out:
  the index tells the elements apart.

## Cells

| Written | Read as | Note |
|---|---|---|
| `42`, `-7` | `Integer` | Up to 10 digits that fit an `int` |
| `true`, `false` | `Boolean` | Lower case only; `TRUE` stays text |
| `2026-03-02` | `Date`, the start of the day | Time zone of the reader |
| `2026-03-02T10:30:15` | `Date` | Seconds may have a fraction |
| `007`, `+3`, `3.0`, `03/02/2026`, `2147483648` | Text | The type a table declares reads it as before |
| `= $Premium * 2` | Text that starts with `=`: a formula | As in a workbook cell |
| `` `$Age * 2` `` | `$Age * 2` | A cell wrapped in backticks as a whole is unwrapped; GitHub shows it as code |
| `a \\| b` | `a \| b` | A pipe is escaped, inside backticks too |
| `line 1<br>line 2` | Two lines | A line break; a value holding the text `<br>` cannot be written |
| `&nbsp;&nbsp;SET` | `SET` indented by one level | TBasic and ColumnMatch: two `&nbsp;` per level |

- A cell is taken as written, not rendered as Markdown.
- An empty cell is blank.

## Tables Written in Parts

- Each part of a `TablePart` table is a file of its own: `table: TablePart Rates row 1 of 2`.
- The parts are joined across the module folder and its sub-folders; the table belongs to the file of its first part.
- A file holds a table of any size, so a table needs no parts in Markdown.

## Errors

| Message | Where | Effect |
|---|---|---|
| `The front matter is not closed with a line of '---'` | Line 1 | No table |
| `Invalid YAML: <problem>` | The line of the problem | No table |
| `The front matter must be a set of 'key: value' lines` | Line 1 | No table |
| `The front matter has no 'table' key with the header of the table` | Line 1 | No table |
| `The properties of the table must be a set of 'name: value' lines under 'properties'` | The key | No table |
| `'<key>' is not a key of the front matter: a table property is written under 'properties'` | The key | Key ignored |
| `The description of a table is the text between the front matter and the table, not a property under 'properties'` | The property | Property ignored |
| `The row has more cells than the first row of the table` | The row | Extra cells ignored |
| `A blank line cuts these rows off the table above. Remove it: a file holds one table` | The rows | Rows ignored |
| `The table does not repeat the first row of the Spreadsheet above, so it is no part of it` | The table | Table ignored |
| `A Data, Test or Run table holds no merged cells: index each element, such as 'drivers[0].name'` | The table | Merged ranges ignored |
| `The Environment table of a module is written in Environment.md at the root of the module folder` | The table | Error |
| `Environment.md holds the Environment table of the module and nothing else` | The table | Error |

- An error points at the cell of the table, at the header of the table when no cell shows it, such as a key of the
  front matter, or at the line of the file when the file holds no table. Each error tells the line of the file as
  well, which OpenL Studio opens it on: only the text of the file shows what breaks the format.
- Other messages are those of a workbook, word for word.

## Editing in OpenL Studio

| Action | Result in the file |
|---|---|
| Change a cell, a row, a column | Only the rows that changed are written; the rest keep their text |
| Change a property, merge or unmerge cells | Only the keys that changed are written |
| Merge cells of a Data, Test or Run table | Refused: a record is one row; a workbook merges them as before |
| Merge a row of a Spreadsheet across the table, or unmerge it | The table is split into parts at the caption, or its parts are joined |
| Change the description | The text above the table is written where its first block stood; the comments stay |
| Create a table | A new file named after the table, in the folder of its category |
| Copy a table | Between workbooks and Markdown modules, both ways: the table and its description, without the comments and the text after it; a Data, Test or Run table gets a row for each record |
| Remove a table | The file is deleted; the last table of a module stays |
| Change the look | Refused: a file keeps no formatting; OpenL Studio draws the table in the OpenL table style |
| Write an Excel formula | Refused: a text starting with `=` is an OpenL formula, written as it is |
| Save over a file another program changed | Refused, as for a workbook: the file keeps what the other program wrote |

## Not Kept

| Excel feature | In a Markdown file |
|---|---|
| Formula | The value the formula computed |
| Formatting, colours, comments | None; the Excel Formatting view lays the OpenL table style over the table |
| Order of tables on a sheet | The order of the file names |
| Several tables in one place | One table per file |

- Never put text before the first `---`: the file becomes documentation and its table is gone, with no message.

## Converting Workbooks

- [OpenL Maven Plugin](openl-maven-plugin.md) packages a project with its workbooks written as Markdown (`markdown`).
- A table with no Markdown form keeps its workbook, and the build log names the table and the reason.
