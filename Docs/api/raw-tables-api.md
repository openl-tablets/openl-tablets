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
  `maxRows`.
- `styles` — `true` adds the Excel style of every cell and the pieces of its text formatted with fonts of their own; see
  [Style of a Cell](#style-of-a-cell).
- `tableTheme` — the identifier of a table theme. Reports, for a Datatype, a Vocabulary or a Spreadsheet table, the
  look that the theme gives every cell in place of its Excel style, with or without `styles=true`; see
  [Table Theme](#table-theme). An empty value draws no theme, as the empty setting draws the formatting of the Excel
  file. Only with `raw=true`.
- `metaInfo` — `true` adds what the compiler knows about every cell.
- `module` — the module to read the table through. The answer is ready once that module is compiled, without waiting
  for the rest of the project.
- `runState` — `true` adds whether the table can be run.

**Response** (`200 OK`), shortened:

```json
{
  "id": "5bd8922c903afffdfb749b3841eddcf1",
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
- `pos` — the position of the table on its sheet, such as `B4:D12`. Read-only.
- `totalRows` — the number of rows of the table when the window leaves rows out. Read-only.
- `headerHeight` — the number of rows at the top that the header takes: the header line, the properties, and the
  service rows of a decision table. Read-only.
- `layout` — for a test table: `transposed` is `true` where a case is a column rather than a row, and `firstDataLine` is
  the line where the data begins. Absent for the other tables and for a test table whose cases have identifiers of their
  own. Read-only.
- `messages`, `runState`, `partial` — read-only; see [Table Endpoints](README.md#table-endpoints).

### RawTableCell

- `value` — a string, a number, a boolean, or a one-dimensional array of them; see
  [Multi-Value Cells](#multi-value-cells). Absent for an empty cell.
- `colspan`, `rowspan` — the number of columns and rows that the cell spans. Present for a value of 2 or more.
- `covered` — `true` for a position that another cell's span covers. Such a cell has no other field.
- `cell` — the address of the cell in A1 notation, such as `B4`. Read-only; the compilation messages use the same
  address.
- `formula` — the formula of the cell, such as `=B2*C2`, next to the `value` it computed. Read-only.
- `comment` — the note that a reader left on the cell. Read-only.
- `style` — the Excel style of the cell, with `styles=true`, or the look the table theme gives it, with `tableTheme`.
  Read-only.
- `runs` — the pieces of the text formatted with fonts of their own, with `styles=true` or `tableTheme`, when the text
  does not take the font of the cell; see [Style of a Cell](#style-of-a-cell). Read-only.
- `metaInfo` — what the compiler knows about the cell, with `metaInfo=true`. Read-only.

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

The `style` of a cell leaves out an attribute that has its default value:

- `background`, `color` — `#rrggbb`. The default background is white and the default font colour is black.
- `align`, `valign` — the horizontal and the vertical alignment.
- `bold`, `italic`, `underline`, `strikeout` — `true` for the font attribute.
- `indent` — the left indent in Excel units.
- `border` — the borders by side. Each side the workbook draws has its `style` (`solid`, `dashed`, `dotted`, `double`),
  its `width` in pixels, and its `color`, absent when black. A side without a border is absent.
- `fontFamily`, `fontSize` — set by the table theme only. A style read from the workbook has neither.
- `source` — `theme` for the look of a table theme, which a read with `tableTheme` reports in place of the style of
  the workbook; absent for the style the workbook holds, `workbook`. The style of a run names its source the same way.

A text formatted in pieces, such as a header with a grey keyword and a bold name, is read as `runs`, and so is a text
formatted whole in a font of its own, as one run. Put together, the texts of the runs give the text of the cell as the
workbook writes it, spaces around the value included. A run with a `style` draws its text with that style alone: an
attribute absent from it is at its default rather than taken from the cell. A run without a `style` takes the font of
the cell.

```json
{
  "cell": "B2",
  "value": "Datatype Commission",
  "runs": [
    {"text": "Datatype", "style": {"color": "#808080"}},
    {"text": " "},
    {"text": "Commission", "style": {"bold": true}}
  ]
}
```

### Meta Information of a Cell

The `metaInfo` of a cell has these fields, each of which is absent when the compiler has nothing to say:

- `usages` — the pieces of the cell's text that refer to something. Each has the `start` and the `end` in the text, a
  `description`, a `kind` (`rule`, `datatype`, `data`, `field`, `underlined`, or `other`), and, for a table, its
  `tableId`, `module`, and `projectId`.
- `type` — the type that the cell holds.
- `returnCell` — `true` for the cell that a decision table returns.
- `editor` — the editor that the cell asks for: `text`, `numeric`, `combo`, `date`, `multiselect`, `formula`,
  `boolean`, `array`, `range`, `integer`, `double`, or `multiline`.

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

The rows are added to the end of the table and have the cells of the same shape as the `source`. A row must not be wider
than the table: `400` with `openl.error.400.table.append.column.count.message`.

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

The answer is `201 Created` with the summary of the new table.

### Responses

A replacement and an append answer `204 No Content`, or `200 OK` with the new table id and the `Location` header when
the table had no room to grow and moved; see [Data Tables API](data-tables-api.md#responses).

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
  (`row`, `column`, `value`), `row` and `column` (`position`, `cells`), and `range` (`row`, `column`, `cells`; more than
  one cell).
- **`merge`** — merges a rectangle and keeps the value of the top-left cell. Target type `cells` with `row`, `column`,
  `rowspan`, and `colspan`.
- **`unmerge`** — splits the merged cell that covers a position. Target type `cells` with `row` and `column`.
- **`style`** — sets the style of a rectangle; see [Styling Cells](#styling-cells). Target type `cells`.
- **`theme`** — writes a table theme into the table; see [Table Theme](#table-theme). The edit names the `theme` and has
  no `target`.

A cell of a request has a `value`, optional `colspan` and `rowspan`, and `covered`. Examples:

```json
{"operation": "update", "target": {"type": "cell", "row": 5, "column": 2, "value": "Buenos Dias"}}
{"operation": "insert", "target": {"type": "rows", "position": 1,
  "cells": [[{"value": "min <= hour and hour < max", "colspan": 2}, {"covered": true}, {"value": "RET"}]]}}
{"operation": "merge", "target": {"type": "cells", "row": 3, "column": 0, "rowspan": 1, "colspan": 2}}
```

The answer is `204 No Content`, or `200 OK` with the new table id and the `Location` header when the table moved.

**Refusals** (`400`):

- `openl.error.400.table.action.position.invalid.message` — a position is out of range.
- `openl.error.400.table.action.row.width.message`, `openl.error.400.table.action.column.height.message` — a new row
  is not as wide as the table, or a new column is not as tall.
- `openl.error.400.table.action.merge.range.invalid.message` — the range to merge covers one cell or leaves the table.
- `openl.error.400.table.action.merge.overlap.message` — the range overlaps a merged cell.
- `openl.error.400.table.action.merge.data-loss.message` — the range holds more than one distinct value.
- `openl.error.400.table.action.style.empty.message` — a style names no attribute.
- A validation response with a list of `fields` — a value that is not a string, a number, a boolean, or an array
  of them.

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
      "background": "#ffff00",
      "color": "#0000ff",
      "align": "center",
      "bold": true,
      "indent": 1
    }
  }
}
```

- The `style` names the attributes to set: `background` and `color` as `#rrggbb`, `align`, `bold`, `italic`,
  `underline`, and `indent` from 0 to 15. An attribute that is left out is not touched.
- `align` set to `left` puts the cells back to the default alignment. `indent` set to `0` takes the indent away.
- A style that names no attribute is refused.
- The attributes are the ones of a styled read, except the borders and the vertical alignment, which are read-only.

### Table Theme

A table theme gives Datatype, Vocabulary and Spreadsheet tables one look. OpenL Studio offers every theme file in the
`table-themes` folder of its classpath and ships `default` and `green`. A theme is asked for by its identifier, the
name of its file without the extension. How a theme file is written is described in
[Appendix E: Table Themes](../user-guides/openl-studio/appendices/table-themes.md).

**Listing the themes.** Two endpoints list the themes, each with its identifier and the name it is shown by:

```http
GET /rest/table-themes
GET /rest/projects/{projectId}/tables/{tableId}/themes[?module=...]
```

```json
[{"id": "default", "name": "Default"}, {"id": "green", "name": "Green"}]
```

- The first lists every theme OpenL Studio offers, ordered by name.
- The second lists the themes that can be drawn over the table and written into it. Every theme styles every
  Datatype, Vocabulary and Spreadsheet table, so the list holds every theme for such a table, and none for a table
  of any other kind.

**Drawing the theme.** A read with `tableTheme=<id>` reports every cell the theme reaches in the look of the theme, in
place of the formatting of the workbook, and every other cell with its Excel style, as `styles=true` reads it:

- `style` — the cell style with the attributes the theme sets laid over it, and `source` set to `theme`.
- `runs` — the pieces the theme formats the header text in, each style of theirs with `source` set to `theme`. Any
  other text the workbook formats in pieces keeps them where the theme sets nothing of the font of the cell, and is
  drawn in the font of the theme otherwise, as writing the theme gives.

A line two cells of the table share is reported on the upper or the left cell, as the cell styles report the lines a
workbook holds: the line the theme draws over a section of a Spreadsheet is the `bottom` of the cells above it. A read
of some rows reports the line over the row under them on its last row as well, while the first row of the next rows
keeps it: the rows read one window after another draw the line as the whole table does.

The theme is a view only: a client edits a table from a read without `tableTheme`, whose styles are the ones the
workbook holds, so no edit writes the look the screen drew. Only the `theme` action writes a theme. A table of any
other kind is read with the styles of the workbook alone. A theme OpenL Studio does not offer is refused with `400`.

**Writing the theme into a table.** The `theme` action writes a theme into the table, alone or with other edits in
a batch:

```json
{"operation": "theme", "theme": "default"}
```

In a batch, the theme is written over the table as the edits before it left it, so the rows the batch adds are
themed with the rest. A `style` action that follows sets its styling over the theme. Where OpenL Studio records who
edits a table and when, a property the note of the edit adds takes the theme too. A table of any other kind is
refused with `400`, and so is a theme OpenL Studio does not offer.

**Writing the theme into the project.** One endpoint writes a theme into every Datatype, Vocabulary and Spreadsheet
table of every module of the project, and recompiles what it changes:

```http
POST /rest/projects/{projectId}/theme?theme={id}
```

It answers `200` with the identifiers of the tables themed and of the ones left as they are, which are written as
several partial tables. A table of any other kind is in neither list. A project compiled only in part, such as one whose
module compiles alone, is compiled whole first, so the theme reaches every module; a project whose compilation was
stopped is refused with `409`. Where OpenL Studio records who edits a table and when, each table themed is noted as
edited, as any edit of a table is. A property the note adds takes the theme, and a table without room for the note moves
and is named by where it stands once written:

```json
{ "themed": ["f55d6ff710d930c7cf6d43a377446bcd"], "skipped": [] }
```

Writing changes only the look of a table. Each cell keeps its value and every attribute the theme does not set, such
as its number format. The header keeps its text, cells outside the table are not touched, and a table of a
dependency project is left as it is. Writing the theme again adds no styles or fonts to the workbook.

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
