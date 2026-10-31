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
| Declared in `rules.xml` | A path to the folder, or a mask that matches folders holding a `.md` file | `rules/Rating`, `rules/*` |
| Not in the default masks | Without `<modules>` only `rules/**/*.xlsx` and `tests/**/*.xlsx` are modules | — |
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
---

The younger or the older the driver, the higher the risk.

> A comment for the authors: no part of the description.

| Age     | Factor |
|---------|--------|
| < 25    | 1.5    |
| 25 - 65 | 1      |
| >= 65   | 1.2    |

Text after the table is documentation.
```

| Part | What it holds | Rule |
|---|---|---|
| Front matter | The header of the table, YAML between two `---` lines | Starts on the first line of the file |
| Description | The text between the front matter and the body: the `description` property of the table | Taken as written; blocks of text joined with a blank line |
| Comment | A quote (`> …`) or an HTML comment (`<!-- … -->`) above the body | Not compiled, no part of the description |
| Body | The first GFM table, HTML table or code block after the front matter | Every row is a body row, the first one too |
| Code block | The body of a `Method` table, one cell | Any fence language; a new file writes `java` |
| No body | A table of a header alone; the text after its front matter is its description | `Datatype Adult extends Driver` |
| Text after the body | Documentation: not compiled | Paragraphs, headings, tables |

- One table per file: only the first GFM table, HTML table or code block is the body. Rows a blank line cuts off it
  are a problem.
- A file without front matter is documentation as a whole, even with a GFM table in it.
- A line of a description starting with `>`, a code fence or `|` is written with `\` before it, as Markdown escapes it,
  so it is not taken for a comment, a code block or the body.

## Front Matter Keys

| Key | Value | Example |
|---|---|---|
| `table` | The text of the first table cell, the header | `table: Rules String greeting(Integer hour)` |
| `properties` | The table properties but the description, `name: value` one to a line under it | `properties:` then `  tags: rates` |

- Any other key is a problem: the table is read without it, and the key stays in the file as written. So is `merges`:
  merged cells are written in an HTML table.
- The description is never a property of the front matter: a `description` under `properties` is a problem, and the
  table is read without it.
- Values are read as written: `NO` stays `NO`; a list is joined with commas.
- A YAML comment, `# SPDX-License-Identifier: Apache-2.0`, may stand in the front matter.

## Merged Cells

A body with merged cells is an HTML table, as GFM has no merged cell; a body without any is a GFM table. A merged
cell is one `<td>` that spans its rows and columns with `rowspan` and `colspan`, and a cell it covers is not written.

```
---
table: SmartRules Double ageFactor(Integer age)
---

<table>
<tr><td colspan="2">Age</td><td>Factor</td></tr>
<tr><td>0</td><td>25</td><td>1.5</td></tr>
<tr><td>25</td><td>65</td><td>1</td></tr>
<tr><td>65</td><td>120</td><td>1.2</td></tr>
</table>
```

| Rule | Detail |
|---|---|
| Layout | A cell takes the first column of its row that no merged cell above spans down into |
| Width | The columns the first row spans |
| Written by OpenL Studio | A row to a line between `<table>` and `</table>`; the first merge turns a GFM table into an HTML one, the last unmerge back |
| Read | Any layout: a cell to a line, `<th>` for `<td>`, `<thead>` and `<tbody>`, the closing tags a browser does without |
| Blank line | Ends the table, as it ends a block of HTML in GFM: the rows after it are lost |
| Preview | GitHub and GitLab show the table with its merged cells |

| Written in a cell | Read as | Note |
|---|---|---|
| `<br>` | A line break | `<br/>` too |
| `&lt;`, `&gt;`, `&amp;`, `&quot;`, `&apos;` | `<`, `>`, `&`, `"`, `'` | Any other named reference stays as written |
| `&#65;`, `&#x41;` | `A` | A number of no character stays as written |
| `&nbsp;&nbsp;SET` | `SET` indented by one level | As in a GFM table |
| A line break of the file | A space | With the spaces around it |
| Any other tag | Text of the cell | — |

- A cell is taken as written otherwise, as in a GFM table: `|` and backticks are characters of the cell.
- OpenL Studio writes `<` as `&lt;` only before a letter, `/`, `!` or `?`, and `&` as `&amp;` only before a reference
  other than `&nbsp;`, so `<= 14` and `a && b` stay as written.

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
| `line 1<br>line 2` | Two lines | A line break; a GFM table cannot hold the text `<br>` |
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
| `Merged cells are written in an HTML table, with 'rowspan' and 'colspan', not under 'merges'` | The key | Key ignored |
| `The row has more cells than the first row of the table` | The row | Extra cells ignored |
| `The merged cell reaches past the table` | The cell | Cut to the table |
| `The merged cell overlaps another one` | The cell | Cut to the cells no other merged cell takes |
| `The table is not closed with '</table>': a blank line or the end of the file cuts it off` | The last row | Rows after it ignored |
| `A blank line cuts these rows off the table above. Remove it: a file holds one table` | The rows | Rows ignored |
| `The Environment table of a module is written in Environment.md at the root of the module folder` | The table | Error |
| `Environment.md holds the Environment table of the module and nothing else` | The table | Error |

- An error points at the cell of the table, or at the line of the file when no cell shows it.
- Other messages are those of a workbook, word for word.

## Editing in OpenL Studio

| Action | Result in the file |
|---|---|
| Change a cell, a row, a column | Only the rows that changed are written; the rest keep their text |
| Merge or unmerge cells | Only the rows of the merged cell are written; the first merge or the last unmerge writes the table anew, as an HTML or a GFM table |
| Change a property | Only the keys that changed are written |
| Change the description | The text above the table is written where its first block stood; the comments stay |
| Create a table | A new file named after the table, in the folder of its category |
| Copy a table | Between workbooks and Markdown modules, both ways: the table and its description, without the comments and the text after it |
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
