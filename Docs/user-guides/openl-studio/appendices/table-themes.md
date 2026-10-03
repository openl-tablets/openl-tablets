## Appendix E: Table Themes

A table theme describes the look OpenL Studio gives Datatype and Vocabulary tables. A theme is drawn over the tables
while they are viewed, or written into the Excel file, as described in
[Applying the Table Theme](../rules-editor.md#applying-the-table-theme). OpenL Studio ships the **Default** and
**Green** themes. This appendix describes how a theme file is written.

The following topics are included:

-   [Theme File](#theme-file)
-   [Parts of a Table](#parts-of-a-table)
-   [Style Attributes](#style-attributes)
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
-   **`datatype`** — the look of a Datatype table.
-   **`vocabulary`** — the look of a Vocabulary table, which is a Datatype table that declares the type of its values,
    such as `Datatype Gender <String>`.
-   **`base`** — a look that styles no table by itself. It holds what `datatype` and `vocabulary` share, as described
    in [Reusing Parts of a Theme](#reusing-parts-of-a-theme).

A theme styles only the kinds of table it has a look for. A theme without `vocabulary` is not offered for a Vocabulary
table, and applying it to a project leaves Vocabulary tables as they are.

### Parts of a Table

A look consists of the following parts, each of them optional:

-   **`style`** — the style every cell of the table starts from. Each of the other parts is laid over it.
-   **`header`** — the header cell:
    -   **`style`** — the style of the header cell.
    -   **`keyword`** — the font of the `Datatype` keyword.
    -   **`name`** — the font of the table name.
    -   **`type`** — the font of the text that follows the name: the type of a Vocabulary, such as `<String>`, or the
        parent of a Datatype, such as `extends Person`.
-   **`titles`** — the title row of a Datatype table that names its columns, such as a table with a description
    column.
-   **`type`** — the column of field types of a Datatype table.
-   **`name`** — the column of field names of a Datatype table.
-   **`values`** — the default values and the other columns of a Datatype table, or the values of a Vocabulary table.
-   **`lastRow`** — the style laid over the cells of the last row, for example, a line that closes the table.

A Datatype table can be written transposed, with a field in each column. Its field types, field names, and default
values are then rows, and its titles are the first column. The parts follow the fields: `type` styles the row of
field types, `name` styles the row of field names, and `titles` styles the column of titles. `lastRow` still styles
the last row of the table.

The keyword, the name, and the type of the header take only the font attributes. The fill, the alignment, and the
borders of the header cell come from `header.style`. The text of the header is never changed.

The rows of table properties that follow the header keep the formatting they have in the Excel file.

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

The line style is one of `hair`, `thin`, `medium`, `thick`, `dashed`, `dotted`, and `double`.

### Reusing Parts of a Theme

A theme file is read with its YAML anchors, aliases, and merge keys resolved:

-   **`&name`** — anchors a part under a name.
-   **`*name`** — repeats the anchored part.
-   **`<<: *name`** — merges the anchored part into another part. The keys of the anchored part are taken, and a key
    written next to the merge key replaces the anchored key of the same name.

A merge reaches one level only: a part written next to the merge key replaces the whole anchored part of the same
name. To change one attribute of a nested part, merge that part as well. In the following example, the Vocabulary
header takes the base header and changes only the font of the name:

```yaml
base: &base
  header: &header
    style: {background: "#c6e0b4", align: center}
    keyword: &muted {color: "#548235"}
    name: {bold: true}
    type: *muted

vocabulary:
  <<: *base
  header:
    <<: *header
    name: {bold: false}
```

### Theme Example

The following theme gives Datatype and Vocabulary tables a dark header with light text, and closes each table with a
line under its last row:

```yaml
name: Corporate

base: &base
  style:
    fontFamily: Calibri
    fontSize: 11
  header: &header
    style:
      background: "#1f4e78"
      color: "#ffffff"
      align: center
    keyword: {color: "#bdd7ee"}
    name: {bold: true}
    type: {color: "#bdd7ee"}
  lastRow:
    border:
      bottom: {style: medium, color: "#1f4e78"}

datatype:
  <<: *base
  titles: {bold: true}
  name: {background: "#ddebf7"}

vocabulary:
  <<: *base
  header:
    <<: *header
    name: {bold: true, italic: true}
  values: {align: center}
```
