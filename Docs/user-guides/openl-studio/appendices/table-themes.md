## Appendix E: Table Themes

A table theme describes the look OpenL Studio gives its tables: every table but a table of the type **Other**. A
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
-   **`tbasic`** — what a TBasic table changes in the base: an algorithm written in steps. The themes that OpenL
    Studio ships give it the look of a Spreadsheet table, and the row that names its columns the look of the code of
    a Rules table.
-   **`method`** — what a Method table changes in the base: a method written as code. The themes that OpenL Studio
    ships write nothing for it, so it takes the base alone.
-   **`data`** — what a Data table changes in the base.
-   **`test`** — what a Test table changes in the base. The themes that OpenL Studio ships give a Test table the look
    of a Data table, as described in [Reusing Parts of a Theme](#reusing-parts-of-a-theme).
-   **`run`** — what a Run table changes in the base. A Run table is written as a Test table without the expected
    results, and the themes that OpenL Studio ships give it the look of a Data table as well.
-   **`rules`** — what a Rules table changes in the base. The themes that OpenL Studio ships give every other kind of
    decision table the look of a Rules table.
-   **`simpleRules`** — what a SimpleRules table changes in the base.
-   **`smartRules`** — what a SmartRules table changes in the base.
-   **`simpleLookup`** — what a SimpleLookup table changes in the base.
-   **`smartLookup`** — what a SmartLookup table changes in the base.
-   **`columnMatch`** — what a ColumnMatch table changes in the base: a decision tree that checks the arguments row
    by row. The themes that OpenL Studio ships give it the look of a Rules table.
-   **`conditions`** — what a Conditions table changes in the base: the conditions that decision tables take by
    their titles. The themes that OpenL Studio ships give a Conditions, an Actions, and a Returns table the look of
    a Rules table.
-   **`actions`** — what an Actions table changes in the base.
-   **`returns`** — what a Returns table changes in the base. As a key of the theme file, it names this kind of
    table; the `returns` part of a decision table is written inside a look, such as `rules`.
-   **`environment`** — what an Environment table changes in the base. The themes that OpenL Studio ships draw it, a
    technical table, in greys.
-   **`properties`** — what a Properties table changes in the base: the properties a module or a category of tables
    shares, such as `Properties Catalogue`. The themes that OpenL Studio ships give it the look of an Environment
    table.
-   **`constants`** — what a Constants table changes in the base.

A theme is one style for every kind of table: it styles every table but a table of the type **Other**, such as a
table of no kind that OpenL Tablets knows or a part of a table written as several partial tables. Each kind takes the
base, and the key of the kind writes only what it changes. A kind the theme writes nothing for takes the base alone.

### Parts of a Table

A look consists of the following parts, each of them optional. Every kind of table takes these parts:

-   **`style`** — the style every cell of the table starts from. Each of the other parts is laid over it.
-   **`header`** — the header cell:
    -   **`style`** — the style of the header cell.
    -   **`keyword`** — the font of the keyword, such as `Datatype`, `Spreadsheet`, or `SmartRules`.
    -   **`name`** — the font of the table name.
    -   **`type`** — the font of the type that the header names besides the table: the type of a Vocabulary, such as
        `<String>`, the parent of a Datatype, such as `extends Person`, the type a Spreadsheet or a decision table
        returns, such as `SpreadsheetResult` or `Collect Error[]`, with the algorithm a ColumnMatch table names
        before it, such as `<MATCH> String`, the type of the rows of a Data table, such as `Policy`, or the method a
        Test or a Run table calls.
    -   **`parameters`** — the font of the parameters of a Spreadsheet or a decision table, such as
        `( Policy policy )`.
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

A TBasic table also takes the following parts:

-   **`code`** — the row that names the columns by their IDs, such as `operation` or `condition`.
-   **`titles`** — the row of titles, such as **Operation**.
-   **`stepTitle`** — the title of the column of labels, laid over `titles`.
-   **`steps`** — the column of labels, the names a step goes to or calls a subroutine by, such as **Calculation**.
-   **`condition`** — the conditions of the steps, such as `i <= n`. The themes that OpenL Studio ships write no
    `condition`, so the conditions take `style`.
-   **`values`** — what the steps run: their actions, and what a step runs before and after its action.
-   **`sections`** — a step that starts a subroutine with `SUB` or `FUNCTION`, laid over the style of each of its
    cells.
-   **`result`** — a step that returns with `RETURN`, laid over the style of each of its cells.

The description and the operation of a step take `style`. The indent of an operation tells the level of the step,
and a theme never changes it. An indent does not show in a centered text, so keep the `style` of a TBasic table
aligned to the left, as the themes that OpenL Studio ships do.

A Method table takes no part of its own: the code it holds takes `style`.

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

A decision table — a Rules, SimpleRules, SmartRules, SimpleLookup, or SmartLookup table — also takes the following
parts:

-   **`code`** — the rows a Rules table declares its columns in: the kind of each column, such as `C1` or `RET1`, its
    expression, and its parameters. A line the style draws above or below goes round all of these rows, not round each
    of them. A SimpleRules, a SmartRules, and a lookup table have no such rows.
-   **`titles`** — the titles of the conditions, such as **Driver Age**, and of the column that names the rules.
-   **`values`** — the values the conditions are checked against, and the names of the rules.
-   **`horizontals`** — the values of the horizontal conditions across the top of a lookup table, such as **Male**
    and **Female**.
-   **`returnTitles`** — the titles of the columns the table returns or acts in, such as **Factor**.
-   **`returns`** — the values the table returns or acts with. In a lookup table, these are the values where its
    conditions meet.
-   **`groups`** — the style laid over the first rule of a group and over the rule after the group, for example, a
    line above that sets the group apart. The rules that share a value of a condition merged over them make a group.

A decision table can be written transposed, with a rule in each column. The parts then follow the conditions and the
returns: each of them is a row that holds its code, its title, and a value of each rule. The lines the parts draw
turn with them: a line above a part is drawn on its left and a line below it on its right, so a line between the
columns of conditions runs between their rows and a line over a group of rules runs on its left. The lines of
`style` and `lastRow` stay where they are named, and `lastRow` still styles the last row of the table.

A ColumnMatch table also takes the following parts:

-   **`code`** — the row that names the columns by their IDs, such as `names` or `values`.
-   **`titles`** — the row of titles.
-   **`returnTitles`** — the rows that give what the table returns or scores, but for their values: the **Return
    Values** row of a `MATCH` table, the **Score** row of a `SCORE` table, and the **Return Values**, **Total Score**,
    and **Score** rows of a `WEIGHTED` table.
-   **`returns`** — the values of those rows, such as the values the table returns.
-   **`name`** — the names the conditions check, such as `age`.
-   **`values`** — what a condition checks its name with and against: the operation, the weight, and the values.
-   **`groups`** — the style laid over the first row of a group and over the row after the group, for example, a
    line above that sets the group apart. A condition whose name is not indented makes a group with the conditions
    indented under it, which the table checks together.

The indent of a name tells the group, and a theme never changes it. Keep the names aligned to the left, so the
indent shows, as the themes that OpenL Studio ships do.

A Conditions, an Actions, and a Returns table declare the conditions, the actions, and the returns that decision
tables take by their titles. Each part of a declaration stands in a row — its inputs, its expression, its
parameters, and its title — and each declaration in a column. Such a table also takes the following parts:

-   **`code`** — the inputs, the expressions, and the parameters, such as `Integer age` and `age >= minAge`. A line
    the style draws above or below goes round all of these rows, not round each of them.
-   **`titles`** — the titles of the conditions of a Conditions table, such as **Age Band**.
-   **`returnTitles`** — the titles of the actions of an Actions table and of the returns of a Returns table.

A cell that names a part, such as **Inputs** or **Title**, takes the style of the part it names. A Conditions, an
Actions, or a Returns table can be written transposed, with a declaration in each row. The parts then follow the
columns, and their lines turn with them as in a transposed decision table.

An Environment and a Properties table also take the following parts:

-   **`name`** — the column of the settings of an Environment table, such as `import` or `dependency`, and of the
    properties of a Properties table, such as `scope`.
-   **`values`** — the values of the settings and of the properties, such as the packages a module imports.

A Constants table also takes the following parts:

-   **`type`** — the column of the types of the constants, such as `Integer`.
-   **`name`** — the column of the names of the constants, such as `DEFAULT_AGE`.
-   **`values`** — the values of the constants.

A Constants table can be written transposed, with a constant in each column. The parts then follow the constants:
`type` styles the row of types, `name` styles the row of names, and `values` styles the row of values. `lastRow`
still styles the last row of the table.

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

In the following example, every kind of decision table takes the look of a Rules table, and a SimpleLookup table
fills the values of its horizontal conditions with a colour of its own:

```yaml
rules: &rules
  titles: {bold: true, background: "#d0cece"}
  horizontals: {bold: true, background: "#b4c6e7"}
  returns: {background: "#ddebf7"}

simpleRules: *rules
smartRules: *rules
simpleLookup:
  <<: *rules
  horizontals: {bold: true, background: "#8faadc"}
smartLookup: *rules
```

In the following example, a Properties table takes the look of an Environment table, as in the themes that OpenL
Studio ships:

```yaml
environment: &technical
  header: {style: {background: "#e7e6e6"}}
  name: {background: "#f2f2f2"}

properties: *technical
```

### Theme Example

The following theme gives every table a dark header with light text. Its base style names every attribute, so it
overrides the look the Excel file gives a table, and it draws only its own lines: one under the properties and one
under the last row. A Spreadsheet table also gets a filled row of column titles, bold italic headings of its
sections, and bold steps that are marked for the result or give it; a TBasic table looks like a Spreadsheet table
with muted column IDs. A Data, a Test, and a Run table get muted field names, filled titles, highlighted IDs, and grey
values that are not filled. A decision table gets muted code closed by a line, filled titles of its conditions and of
what it returns, bold horizontal conditions, filled returns, and a line over every group of rules, and a ColumnMatch
table looks like it; a Conditions, an Actions, and a Returns table look like its code and its titles. An Environment
and a Properties table get a dark grey header and grey settings, and a Constants table gets muted types and filled
names. A Method table takes the base alone:

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
  name: &fieldName {background: "#ddebf7"}

vocabulary:
  values: {align: center}

spreadsheet: &spreadsheet
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

rules: &rules
  code: &code {color: "#808080", border: {bottom: *line}}
  titles: {bold: true, align: center, background: "#d9d9d9"}
  values: {align: center}
  horizontals: {bold: true, align: center, background: "#bdd7ee"}
  returnTitles: {bold: true, align: center, background: "#bdd7ee"}
  returns: {align: center, background: "#ddebf7"}
  groups: {border: {top: *line}}

simpleRules: *rules
smartRules: *rules
simpleLookup: *rules
smartLookup: *rules

conditions: *rules
actions: *rules
returns: *rules

tbasic:
  <<: *spreadsheet
  code: *code

columnMatch: *rules

environment: &technical
  header: {style: {background: "#595959"}}
  name: {background: "#f2f2f2"}

properties: *technical

constants:
  type: {color: "#808080"}
  name: *fieldName
```
