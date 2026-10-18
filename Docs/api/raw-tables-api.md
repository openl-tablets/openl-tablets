# Raw Tables API

The Raw Tables API reads and writes a table as a matrix of cells with merge information. It does not split the table
into a typed header and body, so it works with every table type, including the types that the other table APIs do not
model. The `tableType` of the table is `RawSource`.

All table types share the same endpoints and the same rules for the project and the table id; see
[Table Endpoints](README.md#table-endpoints).

- **Use cases** — exporting a table as it is in the workbook, reading a table of any type cell by cell, and editing the
  cells, rows, columns, merges, and styles of a table.
- **Not covered** — the typed models of the [Data](data-tables-api.md), [Test](test-tables-api.md), and
  [Lookup](lookup-api.md) tables have their own pages.

## Reading a Table

```http
GET /rest/projects/{projectId}/tables/{tableId}?raw=true
```

**Query parameters:**

- `raw` — `true` returns the matrix. The default `false` returns the typed model of the table.
- `startRow`, `maxRows` — read a window of the matrix: the zero-based row to start with and the number of rows. A window
  never cuts a merged cell in two. Read the next window from the end of the previous one, not from `startRow` plus
  `maxRows`. A window that starts past the last row holds no rows: `source` is empty and `totalRows` is the height of
  the table, with or without `maxRows`.
- `cutMerges` — `true` reads exactly the rows `startRow` and `maxRows` ask for, so the next window starts at
  `startRow` plus `maxRows`. A merged cell that the edge of the window cuts comes back cut: its first cell in the
  window holds the value of the merged cell, spans the rows of it inside the window, and names its whole area in
  `mergedArea`. The default `false` grows the window to keep every merged cell whole, so a merge longer than the
  window comes back whole.
- `styles` — `true` adds the Excel styles the cells use, each once, and names the style of every cell and of the pieces
  of its text formatted with fonts of their own; see [Style of a Cell](#style-of-a-cell). It also adds what the cells
  of the table mean; see [Roles of the Cells](#roles-of-the-cells).
- `metaInfo` — `true` adds what the compiler knows about every cell.
- `module` — the module to read the table through. The answer is ready once that module is compiled, without waiting
  for the rest of the project.
- `runState` — `true` adds whether the table can be run.

**Response** (`200 OK`), shortened:

```json
{
  "id": "N4T8Y2DR6FX1MB5C",
  "tableType": "RawSource",
  "kind": "Rules",
  "name": "Greeting1",
  "pos": "B4:D12",
  "source": [
    [
      {"cell": "B4", "value": "Rules String Greeting1 (Integer hour)", "colspan": 3},
      {"covered": true},
      {"covered": true}
    ],
    [
      {"cell": "B5", "value": "C1", "colspan": 2},
      {"covered": true},
      {"cell": "D5", "value": "RET1"}
    ],
    [
      {"cell": "B9", "value": 0},
      {"cell": "C9", "value": 12},
      {"cell": "D9", "value": "Good Morning"}
    ]
  ]
}
```

### RawTableView

- `id`, `tableType`, `kind`, `name` — as for the other tables. `kind` is the kind of the table: `Rules`,
  `Spreadsheet`, `Datatype`, `Data`, `Test`, `TBasic`, `Column Match`, `Method`, `Run`, `Constants`, `Conditions`,
  `Actions`, `Returns`, `Environment`, `Properties`, or `Other`.
- `source` — the matrix of cells. The first row is the header of the table, followed by its properties and the body.
  An empty list for a window that starts past the last row.
- `styles` — the Excel styles the cells of the read use, each once, with `styles=true`. A cell and a piece of its text
  name their style by its index in this list. Read-only.
- `regions`, `groups`, `signature` — what the cells of the whole table mean, with `styles=true`; see
  [Roles of the Cells](#roles-of-the-cells). Absent for a table of the kind `Other`. Read-only.
- `pos` — the position of the table on its sheet, such as `B4:D12`. Read-only.
- `totalRows` — the number of rows of the table when the window leaves rows out. Read-only.
- `headerHeight` — the number of rows at the top that the header takes: the header line, the properties, and the
  service rows of a decision table. Read-only.
- `layout` — for a test table: `transposed` is `true` where a case is a column rather than a row, `firstDataLine` is
  the line where the data begins, and `caseLines` is the number of lines each case takes, in the order of the cases —
  a case takes several lines where it defines the elements of a collection one per line. `caseLines` is absent where
  every case takes one line. `layout` is absent for the other tables and for a test table whose cases have identifiers
  of their own. Read-only.
- `messages`, `runState`, `partial` — read-only; see [Table Endpoints](README.md#table-endpoints). The version of the
  table is answered in the `ETag` header, not in the body; see [Versions](#versions).

### RawTableCell

- `value` — a string, a number, a boolean, or a one-dimensional array of them; see
  [Multi-Value Cells](#multi-value-cells). Absent for an empty cell. A text starting with `=` is an OpenL formula; see
  [Formulas](#formulas).
- `colspan`, `rowspan` — the number of columns and rows that the cell spans. Present for a value of 2 or more.
- `covered` — `true` for a position that another cell's span covers. Such a cell has no other field.
- `cell` — the address of the cell in A1 notation, such as `B4`. Read-only; the compilation messages use the same
  address.
- `formula` — the Excel formula of the cell, such as `=B2*C2`, next to the `value` it computed. A write stores it in
  place of the value; see [Formulas](#formulas).
- `comment` — the note that a reader left on the cell. Read-only.
- `style` — the index in `styles` of the Excel style of the cell, with `styles=true`. Absent for a cell in no style at
  all. Read-only.
- `runs` — the pieces of the text formatted with fonts of their own, with `styles=true`, when the text does not take
  the font of the cell; see [Style of a Cell](#style-of-a-cell). Read-only.
- `type` — the type of the value, with `styles=true`, when it differs from the type of the region of the cell; see
  [Roles of the Cells](#roles-of-the-cells). Read-only.
- `metaInfo` — what the compiler knows about the cell, with `metaInfo=true`. Read-only.
- `mergedArea` — the whole area of a merged cell that the edge of the window cuts, such as `B4:B299`, with
  `cutMerges=true`. Read-only.

The cell of a merged range that holds the value is the top-left one. The other positions of the range are `covered`.
For a cell of 2 columns and 2 rows at the top-left of a matrix:

```json
[
  [{"value": "Header", "colspan": 2, "rowspan": 2}, {"covered": true}],
  [{"covered": true}, {"covered": true}]
]
```

A client reads the matrix row by row and skips the covered cells.

### Style of a Cell

A read sends each style once: `styles` lists the styles its cells use, and a cell names its style by its index in the
list. A table of thousands of cells in a few styles sends those few. A style leaves out an attribute that has its
default value:

- `background`, `color` — `#rrggbb`. The default background is white and the default font colour is black.
- `align`, `valign` — the horizontal and the vertical alignment.
- `bold`, `italic`, `underline`, `strikeout` — `true` for the font attribute. The name and the size of a font are not
  read: a client draws a table in its own font.
- `indent` — the left indent in Excel units.
- `border` — the borders by side. Each side the workbook draws has its `style` (`solid`, `dashed`, `dotted`, `double`),
  its `width` in pixels, and its `color`, absent when black. A side without a border is absent. Each cell reports the
  borders it has itself, so a line two cells share may be reported by both of them, or by either; a client draws the
  wider line, as Excel does. A read of some rows reports the lines of its own cells only, so the line between two
  windows of rows is the one the cells on either side of it draw.

A text formatted in pieces, such as a header with a grey keyword and a bold name, is read as `runs`, and so is a text
formatted whole in a font of its own, as one run. A run names `to`, the index in the text of the cell after its last
character: it starts where the run before it ends, the first one at the start of the text, and the runs together cover
the text of the cell as the workbook writes it, spaces around the value included. The text is not repeated. A run with
a `style` draws its text with that style alone, named by its index in `styles`: an attribute absent from it is at its
default rather than taken from the cell. A run without a `style` takes the font of the cell.

```json
{
  "styles": [{"color": "#808080"}, {"bold": true}],
  "source": [
    [
      {
        "cell": "B2",
        "value": "Datatype Commission",
        "runs": [{"to": 8, "style": 0}, {"to": 9}, {"to": 19, "style": 1}]
      }
    ]
  ]
}
```

### Roles of the Cells

A read with `styles=true` says what the cells of the table mean, once for the whole table, so every window of its rows
shares it. A screen draws a table by what its cells mean rather than by the formatting of the workbook, as OpenL
Studio does unless the user turns on **Excel Formatting**. The roles are read from the compiled table; a table the
compiler cannot read takes them from its text. How each kind of table maps to them is described in
[Roles per table type](../architecture/table-formatting.md#roles-per-table-type) and
[Tables the compiler cannot read](../architecture/table-formatting.md#tables-the-compiler-cannot-read).

```json
{
  "pos": "B3:D44",
  "regions": [
    {"role": "signature", "top": 0, "left": 0, "bottom": 0, "right": 2},
    {"role": "inputTitle", "top": 1, "left": 0, "bottom": 1, "right": 1},
    {"role": "outputTitle", "top": 1, "left": 2, "bottom": 1, "right": 2},
    {"role": "value", "type": "number", "top": 2, "left": 0, "bottom": 41, "right": 0},
    {"role": "value", "type": "string", "top": 2, "left": 1, "bottom": 41, "right": 1},
    {"role": "output", "type": "number", "top": 2, "left": 2, "bottom": 41, "right": 2}
  ],
  "groups": {"rows": [14, 27]},
  "signature": [
    {"part": "keyword", "to": 10},
    {"part": "type", "to": 17},
    {"part": "name", "to": 25},
    {"part": "punctuation", "to": 26},
    {"part": "parameterType", "to": 32},
    {"part": "parameterName", "to": 39},
    {"part": "punctuation", "to": 40}
  ]
}
```

- `regions` — rectangles of cells that take one role, in table coordinates: the header row is row 0 and the first
  column of the table is column 0. They do not overlap and cover the table. A region names its `role` and, when all
  its values have one, their `type`. A cell whose value is of another type names its own `type`.
- `role` — `signature` (the header), `properties`, `code` (a declaration the compiler reads, such as `C1` or the field
  names of a Data table), `inputTitle` and `outputTitle` (the titles of what the table takes and gives), `key` (what
  names a row or a column, such as a rule name or a Data ID), `value` (a Spreadsheet step name among them), `output`
  (a value the table gives, such as a returned value or an expected result), `section` (a heading inside the body),
  and `result` (what the table returns).
- `type` — `code` (an OpenL expression), `number`, `date`, `boolean`, or `string`, as the compiler reads the value. A
  range takes the type of its bounds, an array the type of its elements.
- `groups` — the `rows` and the `columns` of the body that start a group, in table coordinates: the rules sharing a
  merged value of a condition, the columns of a lookup sharing a merged horizontal value, the conditions of a
  ColumnMatch table indented under one name. A list without lines is absent, and so is `groups` without either.
- `signature` — the pieces of the header text: `keyword`, `type`, `name`, `punctuation`, `parameterType`, and
  `parameterName`, each named by `to`, the index in the text after its last character. A piece starts where the piece
  before it ends and takes the spaces before it.

### Meta Information of a Cell

The `metaInfo` of a cell has these fields, each of which is absent when the compiler has nothing to say:

- `usages` — the pieces of the cell's text that refer to something. Each has the `start` and the `end` in the text, a
  `description`, a `kind` (`rule`, `datatype`, `data`, `field`, `underlined`, or `other`), and, for a table, its
  `tableId`, `module`, and `projectId`. A rule written in several versions names the table the compiler built to
  choose between them (`validateGapOverlap_<rule name>`), in the module the cell is read through. That table is
  read through the same module, its `runState` is `cannot-run`, and its `tableId` is made of its name, so it stays
  the same from one compilation to the next. Nothing but the compiler writes it: editing, copying, running, tracing
  or benchmarking it answers
  `400` with `openl.error.400.table.generated.message`, which names the table.
- `type` — the type that the cell holds.
- `returnCell` — `true` for the cell that a decision table returns.
- `editor` — the editor that the cell asks for: `text`, `numeric`, `combo`, `date`, `multiselect`, `formula`,
  `boolean`, `array`, `range`, `integer`, `double`, or `multiline`.
  A parseable numeric range in a lookup table's horizontal condition reports `range` even when the cell also has
  compiler-resolved usages. A formula reports `formula`. Other values with usages report `text`, or `multiline` when
  they contain a newline.

## Multi-Value Cells

- A context-parsed multi-value cell is returned as a JSON array, for example `{"value": ["MA2", "FA+", "SPA"]}`.
- Creating a table in an existing or a new module, a full update, an append, an insert, and the cell, row, column, and
  range actions accept the same one-dimensional array. The writer converts it to the scalar workbook representation
  that the OpenL type of the cell uses, so a representable raw response can be submitted unchanged.
- Array elements are strings, numbers, booleans, or null. Commas and trailing backslashes in string elements are
  preserved through the workbook representation. Null elements keep their positions, including in enum arrays.
- When the table metadata parses an array into typed values, an update and the source actions convert the JSON values
  back to that element type. Enum elements are stored by their constant names. Dates use the cell's Excel date format,
  the workbook date system, and the server locale for formatting and parsing. A cell with the General format uses the
  lossless ISO representation. So an unchanged raw response stays parseable after it is written back.
- An array has at least one element and does not consist of a single null. A string element is not empty and has no
  leading or trailing whitespace, because OpenL trims array elements. An invalid array, a JSON object, or a nested array
  is refused with `400`.

## Writing a Table

### Replace the Matrix

```http
PUT /rest/projects/{projectId}/tables/{tableId}
Content-Type: application/json

{
  "tableType": "RawSource",
  "source": [
    [{"value": "Data Bank bankData", "colspan": 2}, {"covered": true}],
    [{"value": "bankID"}, {"value": "rating"}],
    [{"value": "bank ID"}, {"value": "Rating"}],
    [{"value": "BANK1"}, {"value": "A"}]
  ]
}
```

- The writer writes every cell that is not covered, applies the merges, and removes the rows of the table that are
  below the new matrix.
- The first cell of the matrix is the header of a table that OpenL recognizes, such as `Rules`, `Datatype`, `Data`,
  `Spreadsheet`, or `Test`. For the kind `Other` any non-blank text is accepted. An empty matrix, and a header that
  OpenL does not recognize, are refused with `400`.
- A table that OpenL recognizes cannot be turned into one that it cannot parse: `400` with
  `openl.error.400.table.header.unrecognized.message`.
- A table that is written as several partial tables is read but not written: `400` with
  `openl.error.400.table.partial.message`. Excel edits such a table.

### Append Rows

```http
POST /rest/projects/{projectId}/tables/{tableId}/lines
Content-Type: application/json

{
  "tableType": "RawSource",
  "rows": [
    [{"value": "BANK3"}, {"value": "C"}],
    [{"value": "BANK4"}, {"value": "D"}]
  ]
}
```

The rows are added to the end of the table and have the cells of the same shape as the `source`. A row has one cell per
column of the table, a blank `{"value": null}` or a `{"covered": true}` one included: a shorter or a wider row answers
`400` with `openl.error.400.table.action.row.width.message`, and nothing is appended.

### Create a Table

```http
POST /rest/projects/{projectId}/tables
Content-Type: application/json

{
  "moduleName": "Main",
  "sheetName": "Rules",
  "table": {"tableType": "RawSource", "source": [[{"value": "Rules String Hello (Integer hour)"}]]}
}
```

- `moduleName` — the module that gets the table. It exists, unless `modulePath` is given.
- `modulePath` — the project-relative path of a new `.xlsx` module. A new module is created only from a raw source.
- `sheetName` — the sheet for the table. It has 1 to 31 characters and none of `/ \ * ? [ ] :`.
- `table` — the table. A table of any type of the [Table Endpoints](README.md#table-endpoints) is accepted.
- `regions` — optional, the roles of the cells of a raw table as the client built it, in the shape of
  [Roles of the Cells](#roles-of-the-cells) and in the coordinates of `source`. While tables are formatted on save, the
  table is written in the OpenL table style of these roles; a region outside `source` is left out. A table sent without
  them, or in a typed view, is written as sent and formatted at its next save.

The answer is `201 Created` with the summary of the new table.

### Formulas

A cell holds an OpenL formula or an Excel formula, and a write names which one it means:

- **`value`** — a text starting with `=`, such as `= product($F1:$F4)` or `= $Base * 2`, is an OpenL formula. It is
  written as the text it is, spaces included, and OpenL calculates it when the module is compiled. A text is never
  taken for an Excel formula, even when Excel could read it.
- **`formula`** — an Excel formula, starting with `=` and written as Excel writes it, such as `=B2*C2`. The cell is
  written as this formula, and Excel computes its value. OpenL reads the value the formula computed.

A cell of an [edit](#editing-the-source) names either a `value` or a `formula`: one naming both is refused with
`openl.error.400.table.action.cell.value-and-formula.message`. A table written whole — replaced, appended to or
created — takes both, as a read returns them, and writes the `formula`, ignoring the `value` beside it.

The Excel formulas of a table are computed again when the table is saved, so a formula keeps the value it computes
from the cells as they are saved, a cell written after it included. A formula OpenL Studio cannot compute keeps the
value it had.

A read returns an Excel formula in `formula`, beside the `value` it computed. So a cell read and written back as it is
keeps its formula. To replace an Excel formula with a value, leave `formula` out of the cell. A cell that already holds
the same Excel formula is left as it is.

```json
{"operation": "update", "target": {"type": "cell", "row": 4, "column": 1, "value": "= product($F1:$F4)"}}
{"operation": "update", "target": {"type": "cell", "row": 5, "column": 1, "formula": "=B2*C2"}}
```

A formula that does not start with `=`, holds nothing after it, or Excel cannot read is refused with
`openl.error.400.table.cell.formula.invalid.message`, and nothing is written. So is a formula OpenL Studio cannot
compute, such as one calling a function it does not implement: OpenL reads the value a formula computed, and the cell
would read as empty.

A table of a Markdown module holds no Excel formulas, so a read returns none, and a `formula` written into it — by an
edit, or by a table written whole or created — is refused with `openl.error.400.table.markdown.layout.message`, and
nothing is written. Its OpenL formulas are values.

### Versions

A read of the table, of a window too, answers the version of the whole table in the `ETag` header; the body carries
none. A write — a replacement, an append, an edit of the source, a write of the properties and a deletion — names the
version it is based on in the `If-Match` header:

```http
PUT /rest/projects/{projectId}/tables/{tableId}
If-Match: "9f2c…"
```

The version is compared when the write's turn has come. A table changed since the read, in Excel, on disk or through
another client, answers `412` with `openl.error.412.table.changed.since.read.message`, and nothing is written: read the
table again and repeat the edit. A write without `If-Match`, or with `*`, is made over any version; a weak tag matches
none. The headers follow RFC 9110: [`ETag`](https://www.rfc-editor.org/rfc/rfc9110#section-8.8.3),
[`If-Match`](https://www.rfc-editor.org/rfc/rfc9110#section-13.1.1) and [`412 Precondition
Failed`](https://www.rfc-editor.org/rfc/rfc9110#section-15.5.13).

### Responses

A replacement and an append answer `204 No Content`, or `200 OK` with the current table id and the `Location` header
when the write renamed the table or the request named the table by an id it had before; see
[Data Tables API](data-tables-api.md#responses).

### Formatted on Save

While the system setting `table.format-on-save` is on, as it is by default, every write formats the table it saves with
the OpenL table style, as [Reformatting the Excel Files](#reformatting-the-excel-files) does, with no compile:

- an edit takes the roles of the last compile of the table, moved along the rows and columns the write inserts and
  removes, and its header and properties as they stand;
- a copy takes the roles of the table it is copied from;
- a created table takes the `regions` of its request.

A table whose module was written to since its last compile, with automatic compilation off, is saved as it is written:
its roles are not known until the module is compiled. A table of an `.xls` workbook is never formatted.

An `.xlsx` file uploaded through the project files API, `POST` and `PUT /rest/projects/{projectId}/files/{path}` and a
ZIP archive expanded into a project folder, is formatted too, and so is a ZIP archive that overwrites a project,
`PUT /rest/repos/{repo-name}/projects/{project-name}?overwrite=true`. The upload is compiled once, with the project, to
learn the roles of its tables. A file that cannot be compiled is stored as it was uploaded.

### Blank Lines Are Refused

OpenL reads a table only as far as its first entirely blank row or column. A write that would leave one inside the table
is refused with `400` and `openl.error.400.table.action.line.all-empty.message`, and the workbook stays unchanged.

- The rule holds for every write: create, full update, append, and each cell, line, and range action.
- It is checked against the table that the write produces, not only against the rows of the request. A single-cell
  update that empties the last filled cell of its row or column is refused like a blank row.
- A cell that a merge spans into is not blank, so a line that an existing merge crosses stays valid.
- Blank lines around the table are not affected.

## Editing the Source

```http
POST /rest/projects/{projectId}/tables/{tableId}/actions
Content-Type: application/json
```

The request applies one edit to the raw source of any table. The `operation` selects the edit, and the `type` of the
`target` selects what it acts on. Positions are zero-based and address the matrix that the raw read returns.

- **`append`** — adds rows or columns to the end of the table. Target types: `rows`, `columns`, with `cells`.
- **`insert`** — inserts rows or columns at a `position`. The position of a row is from 1 to the table height, because
  row 0 is the header. The position of a column is from 0 to the table width. Target types: `rows`, `columns`, with
  `position` and `cells`.
- **`delete`** — deletes `count` rows or columns from a `position`. The header row cannot be deleted. Target types:
  `rows`, `columns`. A merged cell reaching past the block loses only the rows or columns it covers and keeps its
  value, even when the block takes away the line the value stood on. A merged cell inside the block goes away with it,
  value included, and one left a single cell is no longer merged.
- **`update`** — overwrites a cell, a row, a column, or a rectangle. The table is not resized. Target types: `cell`
  (`row`, `column`, `value`, optional `marks`; see [Marking Pieces of a Text](#marking-pieces-of-a-text)), `row` and
  `column` (`position`, `cells`), and `range` (`row`, `column`, `cells`; more than one cell).
- **`merge`** — merges a rectangle and keeps the value of the top-left cell. Target type `cells` with `row`, `column`,
  `rowspan`, and `colspan`.
- **`unmerge`** — splits the merged cell that covers a position. Target type `cells` with `row` and `column`.
- **`style`** — sets the style of a rectangle; see [Styling Cells](#styling-cells). Target type `cells`.

A cell of a request has a `value` or a [`formula`](#formulas), optional `colspan` and `rowspan`, and `covered`. An
`update` of a `cell` takes `value` and `formula` the same way. Examples:

```json
{"operation": "update", "target": {"type": "cell", "row": 5, "column": 2, "value": "Buenos Dias"}}
{"operation": "insert", "target": {"type": "rows", "position": 1,
  "cells": [[{"value": "min <= hour and hour < max", "colspan": 2}, {"covered": true}, {"value": "RET"}]]}}
{"operation": "merge", "target": {"type": "cells", "row": 3, "column": 0, "rowspan": 1, "colspan": 2}}
```

The answer is `204 No Content`, or `200 OK` with the current table id and the `Location` header when the edit renamed
the table or the request named the table by an id it had before.

**Refusals** (`400`):

- `openl.error.400.table.action.position.invalid.message` — a position is out of range.
- `openl.error.400.table.action.row.width.message`, `openl.error.400.table.action.column.height.message` — a new row
  is not as wide as the table, or a new column is not as tall.
- `openl.error.400.table.action.merge.range.invalid.message` — the range to merge covers one cell or leaves the table.
- `openl.error.400.table.action.merge.overlap.message` — the range overlaps a merged cell.
- `openl.error.400.table.action.merge.data-loss.message` — the range holds more than one distinct value.
- `openl.error.400.table.action.style.empty.message` — a style names no attribute.
- `openl.error.400.table.action.style.format-on-save.message` — a style names an attribute other than `indent` while
  the system setting `table.format-on-save` is on.
- `openl.error.400.table.action.style.colour.invalid.message` — a colour is given both as a theme colour and as `rgb`,
  as neither, or as `rgb` with a `tint`.
- `openl.error.400.table.action.style.colour.not-standard.message` — an `rgb` colour is not a standard colour of Excel.
- `openl.error.400.table.action.marks.header.message` — `marks` on the header cell of the table.
- `openl.error.400.table.action.marks.not-text.message` — `marks` on a value that is not a text.
- `openl.error.400.table.action.marks.invalid.message` — a mark leaves the text, covers no character, or overlaps or
  precedes the mark before it.
- `openl.error.400.table.action.marks.xls.message` — `marks` on a cell of an `.xls` file.
- `openl.error.400.table.action.cell.value-and-formula.message` — a cell names both a `value` and a `formula`; see
  [Formulas](#formulas).
- `openl.error.400.table.cell.formula.invalid.message` — a `formula` that Excel cannot read or OpenL Studio cannot
  compute; see [Formulas](#formulas).
- `openl.error.400.table.markdown.style.message` — `marks`, or a `style` naming an attribute other than `indent`, on
  a table of a Markdown module, whose file keeps no formatting.
- `openl.error.400.table.markdown.layout.message` — the edit leaves a table of a Markdown module in a shape its file
  cannot hold, or writes an Excel `formula` into it; the message tells why.
- A validation response with a list of `fields` — a value that is not a string, a number, a boolean, or an array
  of them; a mark without `from`, `to` or `color`, or with an unknown `color`.

### Appending and Inserting Structural Blocks

An `append` or an `insert` adds one or more rows or columns in a single request. A span in an earlier row or column may
cover later rows or columns of the same request. Mark each covered position with `covered: true`. The span is validated
against the table dimensions after the whole block is added.

- Spans declared in the same action must not overlap. An overlapping block is refused with `400` before its merges are
  applied.
- A raw read after an append or an insert returns the added cells and their spans, even when the compiled Datatype
  model does not include them yet. Such cells have no Datatype-specific metadata until the table is bound again.
- An insert allocates the whole block before it applies the inline merges. The rows or columns at and after the
  position shift together and keep their merges. An inline merge does not expand when another item of the same request
  is inserted.

### Marking Pieces of a Text

An `update` of a `cell` may carry `marks`: pieces of its text value written bold in one of four colours. A mark names
`from`, the index in the text of its first character, `to`, the index after its last character, and `color`:

- `error` — what is wrong or must not be missed; Red in Excel.
- `warning` — what needs care; Orange in Excel.
- `success` — what is right or confirmed; Green in Excel.
- `info` — what is worth a note; Light Blue in Excel.

```json
{"operation": "update", "target": {"type": "cell", "row": 3, "column": 1, "value": "Rate is 0.15 for new clients",
  "marks": [{"from": 8, "to": 12, "color": "warning"}]}}
```

- The marks follow the order of the text and do not overlap. Two marks of one colour that touch are written as one.
- A mark changes the text only: the cell keeps its style, no cell style is added to the workbook, and the cell compiles
  as before.
- An empty list writes the text with no marks. Without `marks`, a new value is written with no marks, and a value
  written unchanged keeps the marks it has.
- A value written as a number, a date or a boolean keeps no marks. Marks on an Excel `formula` are refused with
  `openl.error.400.table.action.marks.not-text.message`; an OpenL formula is a text and takes marks.
- A read with `styles=true` returns the marks as [`runs`](#style-of-a-cell) whose style is bold in the colour of the
  mark.

### Styling Cells

The `style` operation sets the style of every cell of a rectangle:

```json
{
  "operation": "style",
  "target": {
    "type": "cells",
    "row": 5,
    "column": 0,
    "rowspan": 1,
    "colspan": 3,
    "style": {
      "background": {"theme": "accent1", "tint": 0.6},
      "color": {"rgb": "#C00000"},
      "align": "center",
      "bold": true,
      "indent": 1
    }
  }
}
```

- The `style` names the attributes to set: `background` and `color`, `align`, `bold`, `italic`, `underline`, and
  `indent` from 0 to 15. An attribute that is left out is not touched.
- `align` set to `left` puts the cells back to the default alignment. `indent` set to `0` takes the indent away.
- A style that names no attribute is refused.
- The attributes are the ones of a styled read, except the borders and the vertical alignment, which are read-only.
- While the system setting `table.format-on-save` is on, as it is by default, a style may name the `indent` alone:
  the save writes the OpenL table style over any other styling. `GET /rest/settings` tells whether it is on, as
  `formatTablesOnSave`.

A colour is one of the palette of Excel, given one way:

- **A theme colour** — `{"theme": "accent1", "tint": 0.6}`: `lt1`, `dk1`, `lt2`, `dk2` for the first and the second
  background and text, `accent1` to `accent6` for the accents. The `tint` from -1 to 1 makes it lighter, above 0, or
  darker, below 0, as Excel names it: `0.6` for Lighter 60%, `-0.25` for Darker 25%; absent for the theme colour
  itself. The cell takes the colours of the theme of its workbook; a workbook without a theme, such as an `.xls` one,
  takes the colour Office draws it in.
- **A standard colour** — `{"rgb": "#C00000"}`, one of `#C00000`, `#FF0000`, `#FFC000`, `#FFFF00`, `#92D050`,
  `#00B050`, `#00B0F0`, `#0070C0`, `#002060` and `#7030A0`.

A styled read reports every colour as the `#rrggbb` it is drawn in.

### Reformatting the Excel Files

One endpoint formats every table of every `.xlsx` file of the project with the OpenL table style, and recompiles what
it changes. The style is described in
[Reformatting Excel Files](../user-guides/openl-studio/rules-editor.md#reformatting-excel-files): each part of a
table takes a built-in cell style of Excel, redefined with the theme colours and the theme fonts of the file.

```http
POST /rest/projects/{projectId}/format
```

It answers `200` with the identifiers of the tables formatted and of the ones left as they are, which are written as
several partial tables:

```json
{ "formatted": ["N4T8Y2DR6FX1MB5C"], "skipped": [] }
```

- A table of the kind `Other`, a table of an `.xls` file and a table of a dependency project are in neither list.
- A project compiled only in part, such as one whose module compiles alone, is compiled whole first, and a project
  written to since it was compiled is compiled from its files again, so every table is formatted as it stands. A
  project whose compilation was stopped is refused with `409`
  (`openl.error.409.table.format.project.stopped.message`).
- Each cell keeps its value, its number format, its indent, its text wrapping and its text rotation. The header keeps
  its text and is formatted in pieces; any other text formatted in pieces is written in the font of its cell. Cells
  outside the tables are not touched.
- Each Excel file is saved once, without the cell formats and the named styles no cell uses any more. Formatting
  again adds nothing to the file.

### Applying Several Edits

```http
POST /rest/projects/{projectId}/tables/{tableId}/actions/batch
Content-Type: application/json

{
  "actions": [
    {"operation": "update", "target": {"type": "cell", "row": 5, "column": 2, "value": "Buenos Dias"}},
    {"operation": "insert", "target": {"type": "rows", "position": 6, "cells": [[{"value": 6}, {"value": 9}, {"value": "Good Early Morning"}]]}},
    {"operation": "delete", "target": {"type": "rows", "position": 9, "count": 1}}
  ]
}
```

- **Order** — each edit has the shape of the single-edit request and addresses the table as the previous edit left it.
  An insert or a delete shifts the coordinates of everything that follows it.
- **One write** — the table is written once, after the last edit. An edit that is refused ends the sequence, and nothing
  of it reaches the table.
- **Response** — as for a single edit: `204`, or `200` with the new id and the `Location` header.

## Editing a Table

Editing begins with a request that answers how the cells of the table take a value, and that locks the project for the
caller:

```http
GET /rest/projects/{projectId}/tables/{tableId}/editors?startRow=0&maxRows=100
```

```json
{
  "kind": "raw",
  "editors": [
    {"editor": "numeric", "min": -2147483648, "max": 2147483647, "intOnly": true},
    {"editor": "combo", "choices": ["AL", "AZ"], "displayValues": ["Alabama", "Arizona"]}
  ],
  "cells": [
    {"row": 5, "column": 0, "editor": 0},
    {"row": 5, "column": 2, "editor": 1}
  ]
}
```

- **Window** — `startRow` and `maxRows` are the same window as the raw read, so the same row and column point at a
  cell in both.
- **Editors** — the ways of entering a value are listed once and pointed at by index, because a whole column usually
  asks for the same one. A cell that is not listed is written as plain text.
- **`kind`** — `raw` lists the cells one by one. `declared` also has `areas`: the parts of the table whose every cell
  asks for an editor, with the `row`, `column`, `rows`, and `columns` of the part. A part with `null` for `rows` or
  `columns` runs to the edge of the table and past it, so the cells of a rule added at the end are written the way their
  column declares.
- **Fields** — `combo` and `multiselect` carry `choices` and `displayValues`, `numeric` the `min` and `max` of the type,
  `array` and `range` the `entryEditor`, and `multiselect` and `array` the `separator`.

The lock makes the tables of the project read-only for everybody else, and their writes answer `409`:

```json
{
  "code": "openl.error.409.project.locked.by.message",
  "message": "The project is locked by user 'admin'."
}
```

Editing ends with the other side of the same address:

```http
DELETE /rest/projects/{projectId}/tables/{tableId}/editors
```

It releases the lock, but only where the project has nothing of its own left to protect: a table saved into the
workspace and not yet committed keeps the lock. A lock that another user holds is left alone, so the request is safe to
repeat and safe to send where editing never began. Saving or closing the project releases the lock too.

A client that writes tables without an editor needs none of this, because every write endpoint locks the project by
itself. See [The Project Editing Lock](../architecture/project-editing-lock.md).
