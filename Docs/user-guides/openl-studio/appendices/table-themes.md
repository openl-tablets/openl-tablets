## Appendix E: Table Themes

A table theme describes the look OpenL Studio gives Datatype, Vocabulary, Spreadsheet, Data, Test, and Run tables. A
theme is drawn over the tables while they are viewed, or written into the Excel file, as described in
[Applying the Table Theme](../rules-editor.md#applying-the-table-theme). OpenL Studio ships the **Default** and
**Green** themes. This appendix describes how a theme file is written.

The following topics are included:

-   [Theme File](#theme-file)
-   [Parts of a Table](#parts-of-a-table)
-   [Style Attributes](#style-attributes)
-   [Extending the Base](#extending-the-base)
-   [Reusing Parts of a Theme](#reusing-parts-of-a-theme)
-   [Theme Example](#theme-example)

### Theme File

A theme is a YAML file with the `.yaml` extension:

-   **Identifier** — the file name without the extension, such as `corporate` for `corporate.yaml`. **My Settings**
    and the API refer to the theme by it, so it tells the themes apart: of two files of one name, OpenL Studio offers
    the one it reads first and writes a warning about the other to its log.
-   **Name** — the name the file declares, which OpenL Studio shows wherever a theme is chosen. Two themes may declare
    one name.

A file is not offered when it cannot be read, declares no name, writes a key twice, writes a colour in another way than
`#rrggbb`, writes a font size that is not a whole number from 1 to 409, or contains an attribute that is not described
in this appendix. The reason is written to the OpenL Studio log, and the other themes are offered as usual.

A theme file holds the following keys:

-   **`name`** — required. The name of the theme shown in OpenL Studio.
-   **`base`** — the skin every table shares, such as its header and its properties. Each kind of table extends it,
    as described in [Extending the Base](#extending-the-base). The base styles no table by itself.
-   **`datatype`** — what a Datatype table changes in the base.
-   **`vocabulary`** — what a Vocabulary table changes in the base. A Vocabulary table is a Datatype table that
    declares the type of its values, such as `Datatype Gender <String>`.
-   **`spreadsheet`** — what a Spreadsheet table changes in the base.
-   **`data`** — what a Data table changes in the base.
-   **`test`** — what a Test table changes in the base. The themes that OpenL Studio ships give a Test table the look
    of a Data table, as described in [Reusing Parts of a Theme](#reusing-parts-of-a-theme).
-   **`run`** — what a Run table changes in the base. A Run table is written as a Test table without the expected
    results, and the themes that OpenL Studio ships give it the look of a Data table as well.

A theme is one style for every kind of table: it styles every Datatype, Vocabulary, Spreadsheet, Data, Test, and Run
table. Each kind takes the base, and the key of the kind writes only what it changes. A kind the theme writes nothing
for takes the base alone.

### Parts of a Table

A look consists of the following parts, each of them optional. Every kind of table takes these parts:

-   **`style`** — the style every cell of the table starts from. Each of the other parts is laid over it.
-   **`header`** — the header cell:
    -   **`style`** — the style of the header cell.
    -   **`keyword`** — the font of the keyword, such as `Datatype` or `Spreadsheet`.
    -   **`name`** — the font of the table name.
    -   **`type`** — the font of the type that the header names besides the table: the type of a Vocabulary, such as
        `<String>`, the parent of a Datatype, such as `extends Person`, the type a Spreadsheet returns, such as
        `SpreadsheetResult`, the type of the rows of a Data table, such as `Policy`, or the method a Test or a Run
        table calls.
    -   **`parameters`** — the font of the parameters of a Spreadsheet, such as `( Policy policy )`.
-   **`properties`** — the rows of table properties that follow the header. The section is one part of the table
    however many properties it holds: a line the style draws above or below it goes round the whole section, for
    example, a line that closes the properties.
-   **`lastRow`** — the style laid over the cells of the last row, for example, a line that closes the table.

A Datatype table also takes the following parts:

-   **`titles`** — the title row of a Datatype table that names its columns, such as a table with a description
    column.
-   **`type`** — the column of field types.
-   **`name`** — the column of field names.
-   **`values`** — the default values and the other columns.

A Datatype table can be written transposed, with a field in each column. Its field types, field names, and default
values are then rows, and its titles are the first column. The parts follow the fields: `type` styles the row of
field types, `name` styles the row of field names, and `titles` styles the column of titles. `lastRow` still styles
the last row of the table.

A Vocabulary table also takes **`values`**, the style of its values.

A Spreadsheet table also takes the following parts:

-   **`titles`** — the row that names the columns, such as **Formula**.
-   **`stepTitle`** — the title of the column of steps, such as **Step**, laid over `titles`.
-   **`steps`** — the column of step names.
-   **`values`** — the formulas and the values of the steps.
-   **`sections`** — a step whose name cell is merged across its row, laid over `steps`. Such a step has no value and
    heads the steps that follow it, such as **Policy Factors Calculation**.
-   **`marked`** — a step or a column whose name is marked with `*` for the result of the Spreadsheet, such as
    `PolicyNumber*`, laid over its own style.
-   **`result`** — the step whose value a Spreadsheet returns when it returns a type other than `SpreadsheetResult`,
    laid over its own style. It is the step named `RETURN`, or the last step when no step is named so; a column named
    `RETURN` takes its place.

A Data, a Test, and a Run table also take the following parts:

-   **`name`** — the row of field names, such as `policyNumber`, and the row of the tables that some fields take
    their values from, such as `>PolicyData`.
-   **`titles`** — the row of titles, such as **Policy Number**.
-   **`values`** — the values.
-   **`ids`** — the values that name a row of a Data table, laid over `values`. In a Data table, these are its IDs:
    the column `_PK_`, or the first column when the table has none. In a Test and a Run table, these are the values
    of every column that takes them from a Data table by their IDs, such as `Policy1`.
-   **`empty`** — a value that is not filled, laid over its own style.

A Data, a Test, or a Run table can be written transposed, with a field in each row. The parts then follow the fields:
`name` styles the column of field names, `titles` styles the column of titles, and `ids` styles the row of IDs.
`lastRow` still styles the last row of the table.

A part that a kind of table does not take is not used for it, so `base` can hold the parts of every kind.

The keyword, the name, the type, and the parameters of the header take only the font attributes. The fill, the
alignment, and the borders of the header cell come from `header.style`. The text of the header is never changed.

### Style Attributes

A style consists of the following attributes, each of them optional. An attribute that the theme does not set keeps
the formatting the cell has in the Excel file, so a cell keeps its number format, its text wrapping, and any other
formatting the theme says nothing about.

| Attribute    | Value                                | Description                                                  |
|--------------|--------------------------------------|--------------------------------------------------------------|
| `fontFamily` | Font name                            | Font of the text, such as `Franklin Gothic Book`.            |
| `fontSize`   | Whole number from 1 to 409           | Size of the font in points, as Excel sizes a font.           |
| `bold`       | `true` or `false`                    | Whether the font is bold.                                    |
| `italic`     | `true` or `false`                    | Whether the font is italic.                                  |
| `underline`  | `true` or `false`                    | Whether the text is underlined.                              |
| `strikeout`  | `true` or `false`                    | Whether the text is struck out.                              |
| `color`      | `"#rrggbb"`                          | Colour of the font.                                          |
| `background` | `"#rrggbb"`                          | Colour the cell is filled with.                              |
| `align`      | `left`, `center`, `right`, `justify` | Horizontal alignment of the text.                            |
| `valign`     | `top`, `center`, `bottom`            | Vertical alignment of the text.                              |
| `border`     | Sides of the cell                    | Borders of the cell, as described below.                     |

Write a colour as `#rrggbb`, with six hexadecimal digits, and in quotes: in YAML, `#` starts a comment. A shorter
or named colour, such as `#fff` or `red`, is refused. An `.xls` file holds its colours in a palette of 56: a colour
of the theme the palette has no room for is written as the nearest colour it holds, while the screen draws the
colour of the theme. An `.xls` file also holds at most 4,000 cell styles, and an `.xlsx` file 64,000: a theme that
needs more styles than the file has room for is refused, and the file is left as it was. Save an `.xls` file as
`.xlsx` to write the theme into it.

The `border` attribute holds the `top`, `right`, `bottom`, and `left` sides. A side that the theme does not set keeps
the border the cell has. A side is a line style, which is drawn in black, or a line style with a colour:

```yaml
border:
  top: thin
  bottom: {style: medium, color: "#548235"}
```

The line style is one of `none`, `hair`, `thin`, `medium`, `thick`, `dashed`, `dotted`, and `double`. The `none`
style takes the border of the side away. A theme whose base style takes away every side draws only the lines it names,
and the lines that the Excel file draws inside a table are taken away. A base style that fills every cell white does
the same for the fills: a cell that the theme fills no other way is white.

The themes that OpenL Studio ships name every attribute in their base style, so they override the look the Excel file
gives a table: no fill, line, font, or alignment of the file shows through. A text that the file formats in pieces of
its own, other than the header, is drawn and written in the font of its cell.

```yaml
base:
  style:
    fontFamily: Franklin Gothic Book
    fontSize: 10
    bold: false
    italic: false
    underline: false
    strikeout: false
    color: "#000000"
    background: "#ffffff"
    align: left
    valign: top
    border: {top: none, right: none, bottom: none, left: none}
```

### Extending the Base

Each kind of table extends the base: it writes only what it changes, and takes the rest of the base as it is. A part
that the kind writes is laid over the same part of the base attribute by attribute, and the header piece by piece. In
the following example, a Datatype takes the header of the base and only fills it, as the themes that OpenL Studio
ships do; its alignment, its lines, and the fonts of its pieces stay those of the base:

```yaml
base:
  header:
    style: {align: center, border: {top: thin, bottom: thin}}
    keyword: {color: "#808080"}
    name: {bold: true}

datatype:
  header:
    style: {background: "#b4c6e7"}
```

### Reusing Parts of a Theme

A theme file is read with its YAML anchors, aliases, and merge keys resolved, so a part written once can be repeated:

-   **`&name`** — anchors a part under a name.
-   **`*name`** — repeats the anchored part.
-   **`<<: *name`** — merges the anchored part into another part. The keys of the anchored part are taken, and a key
    written next to the merge key replaces the anchored key of the same name.

In the following example, the keyword, the type, and the parameters of the header take one colour, and one line
closes the header, the properties, and the table:

```yaml
base:
  header:
    style:
      border:
        top: &line {style: thin, color: "#548235"}
        bottom: *line
    keyword: &muted {color: "#548235"}
    type: *muted
    parameters: *muted
  properties:
    border: {bottom: *line}
  lastRow:
    border: {bottom: *line}
```

In the following example, a Test and a Run table take the look of a Data table, as in the themes that OpenL Studio
ships:

```yaml
data: &data
  titles: {bold: true, background: "#ddebf7"}
  ids: {bold: true, background: "#fff2cc"}

test: *data
run: *data
```

### Theme Example

The following theme gives every table a dark header with light text. Its base style names every attribute, so it
overrides the look the Excel file gives a table, and it draws only its own lines: one under the properties and one
under the last row. A Spreadsheet table also gets a filled row of column titles, bold italic headings of its
sections, and bold steps that are marked for the result or give it. A Data, a Test, and a Run table get muted field
names, filled titles, highlighted IDs, and grey values that are not filled:

```yaml
name: Corporate

base:
  style:
    fontFamily: Calibri
    fontSize: 11
    bold: false
    italic: false
    underline: false
    strikeout: false
    color: "#000000"
    background: "#ffffff"
    align: left
    valign: top
    border: {top: none, right: none, bottom: none, left: none}
  header:
    style:
      background: "#1f4e78"
      color: "#ffffff"
      align: center
    keyword: &light {color: "#bdd7ee"}
    name: {bold: true}
    type: *light
    parameters: *light
  properties:
    border:
      bottom: &line {style: medium, color: "#1f4e78"}
  lastRow:
    border:
      bottom: *line

datatype:
  titles: {bold: true}
  name: {background: "#ddebf7"}

vocabulary:
  values: {align: center}

spreadsheet:
  titles: {bold: true, background: "#ddebf7"}
  sections: {bold: true, italic: true, background: "#ddebf7"}
  marked: {bold: true}
  result: {bold: true}

data: &data
  header: {style: {align: left}}
  name: {color: "#808080"}
  titles: {bold: true, align: center, background: "#ddebf7"}
  ids: {bold: true, background: "#fff2cc"}
  empty: {background: "#f2f2f2"}

test: *data
run: *data
```
