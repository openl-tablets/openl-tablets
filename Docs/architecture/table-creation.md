# Table Creation and Copying

OpenL Studio creates every kind of OpenL table from one dialog. The dialog is not an editor: it produces a table
that compiles, in the place the author chose, and leaves everything past that to the table editor.

An existing table is copied from a separate dialog. The browser reads its raw cell matrix, changes the header and
properties selected by the author, and submits the result through the same table-creation API.

## Anatomy

A settings strip over a sheet.

The strip carries the table type, the table name, the destination, and — for the types that declare a method — the
signature. The sheet below it is the table as it will be written: the top cell holds the OpenL header, the rows
under it hold the body.

What is shown is what is written. Nothing is added on the way to the workbook and nothing is hidden from the grid,
so the dialog can be read as the finished table rather than as a form that describes one.

## The header cell is the authority

The header text is generated from the type, the name, the signature and the settings specific to that type. It is a
read-only preview of the exact header OpenL Studio will write. A Datatype's optional **Extends** setting, for example,
writes `extends <parentType>`.

The name OpenL will compile is read back out of that header, the same way the compiler reads it — never from the
Name field. The field is one way to generate the header, not the record of what the table is called.

## Table types

Fifteen types, each one a keyword, a body the dialog knows how to lay out, and an example of a row.

| Type                        | Body the dialog builds                                      | Named |
|-----------------------------|-------------------------------------------------------------|-------|
| Datatype                    | Type, Name, Default Value, Mandatory, Description, Examples | yes   |
| Vocabulary                  | one value column under a simple base type in `< >`          | yes   |
| Constants                   | Type, Name, Default Value                                   | no    |
| Spreadsheet                 | columns the author names, starting at Steps and Formula     | yes   |
| Smart Rules, Simple Rules   | one column per argument, then the result                    | yes   |
| Smart Lookup, Simple Lookup | a two-dimensional matrix (see below)                        | yes   |
| Rules                       | Condition and Output over the declarations OpenL needs      | yes   |
| Test, Run                   | a column per value a call supplies, plus the result         | yes   |
| Data                        | columns generated from a datatype                           | yes   |
| Environment                 | Key and Value                                               | no    |
| Properties                  | Property and Value                                          | no    |
| Free Form                   | a plain grid, and nothing else                              | no    |

A type that is not named is identified by its keyword alone, and the dialog does not offer the field. Everywhere
the field is offered it opens empty and is required. The author must enter a legal OpenL identifier, because it
becomes the table's name.

Three types take their shape from something outside the signature: Test and Run from the table under test, Data
from a datatype. Their columns change when that definition changes.

Test, Run and Data also offer **Transposed**. Clear, the structural rows run across the sheet and every record is a
row. Selected, each declared field becomes a row and every record becomes a column. The grid changes orientation
immediately and the submitted cell matrix has the same vertical shape.

Their generated business titles use Title Case for every word. `mainDriverAge`, for example, is displayed as
`Main Driver Age`, while the technical field or path keeps its original spelling.

A Test or Run table is built in the dialog, out of the signature the tested table declares. It opens a datatype
argument up the way a Data table does: one column per field, named by the path OpenL reads it back with, as deep as
the datatypes nest. A datatype that refers back to itself stops there. The expected result stays one column
whatever its type — a whole object is what a generated test compares — and a Run table has none, because it only
calls.

The signature is text the compiler already parsed: the tables list carries it, and reading a header the compiler
read is not the same as parsing OpenL. What the dialog cannot see is what a type means, so the datatypes an
argument opens up are read one by one — and only the ones an argument actually names.

A free-form table is the exception to all of this. OpenL does not recognize it, so it has no header to write and
no name to give: the grid is the table, its columns are named the way a sheet names them, and OpenL names the
table after whatever its first cell says. Only that first cell is required.

## Destination

A module and a sheet, both free text with suggestions.

The module list offers what the project declares, the sheet list what the chosen module's own workbook holds, and
neither list restricts what can be typed. Choosing a module chooses a sheet in it, because a sheet belongs to one.

A name that is not in the list is a module or a sheet that does not exist yet, and creating it is part of creating
the table. The author names it; the system works out the rest — where a new module belongs (with the rules, or with
the tests when the table is a Test or a Run), and that it has to be registered with the project.

The destination decides only where the table is written. It never narrows what the table may refer to. Test can
exercise any executable table in the project that returns a value, whichever module holds it. Run can also exercise
one returning nothing, because it calls without asserting a result.

## Copying a table

The **Copy** action on an editable table opens a modal titled with that table's name. Its settings use the same
layout and controls as table creation:

- **Table Name** — an OpenL identifier. It may match an existing table name; the browser writes it into the copied
  header.
- **Module** — free text with suggestions from the modules the project declares.
- **Sheet** — free text with suggestions from the selected module's workbook.

An unlisted module or sheet name creates that destination, exactly as it does in the creation dialog. A new module
is placed with the rules, or with the tests when a Test or Run table is copied.

Properties follow the destination. The source table's kind selects the properties applicable to that kind at the
Table inheritance level, so a Rules table and a Spreadsheet can offer different sets. Properties belonging to a
Properties table's contents, such as `scope`, are not offered here; Table-level properties, such as `version`, are.
Each applicable property keeps its value from the source, except the version: a copy is offered the first version
the table's versions leave free, because the one the source stands for is by definition taken.

A property is offered the way the rest of OpenL Studio names it — by its display name, under its group. An author
reads `state` as *US States* under *Business Dimension*, so the dimensional properties are presented as a group
rather than looked up one by one, and the value written stays the technical name a table declares.

The value editor follows the property definition: text input for text, a date picker for a date, a check box for a
boolean, and a dropdown for an enum. An enum dropdown shows its display value and writes its code into the workbook.
A single-value enum exposes no text input; an enum accepting several values uses a multi-select. Completing the last
row adds another; a row can be deleted with the same control as a Spreadsheet argument. Multiple selected values wrap
across lines in a bounded value column instead of widening the table. The date picker displays a date in the user's
locale and writes it to the workbook in the ISO 8601 `yyyy-MM-dd` form. A partially filled row or repeated property
name makes the form invalid, and so does a value the property's own pattern refuses.

The version is the one property with an editor of its own: three spinners for the numbers the engine orders versions
by, and the version the copied table stands for named beside them. It opens on the first version the table's versions
leave free, and a version any of them carries is refused — by the dialog and by the server, which is the authority.

The copy is written on the server. The dialog sends the destination, the copy's name and its properties; the table
itself never crosses the network. The server rebuilds it on the destination sheet the way the table editor writes a
new table, so the copy keeps the source's cell styles, merged cells and comments, and — being a new table — is
stamped with the author and date OpenL Studio records for a table it creates, while it is set to record them.

Its properties section is laid out the way one written by hand is: the marker down the left, the property name beside
it, and the value reaching the table's right edge. A value left in its own column would leave the columns beside it
as cells of the section rather than as part of the value.

## The grid

The grid opens filled in. A first row of example data says more about the shape of a row than an empty grid does,
and every cell holds a value of the type its own column declares — so a table created untouched is a table that
works. The example is a placeholder to write over, not a suggestion.

A type the dialog knows how to spell out gets a value of it: `1`, `1.0`, `TRUE`, `Text1`, `A`, a date stored as
ISO 8601 `yyyy-MM-dd`, a range written as one. A vocabulary gets the first value it offers. Everything else is a
value no single cell can hold — another datatype, a collection, a type this project does not declare — and is
written the way such a value is referenced in OpenL: `<field>_id_1`, the row of a Data table that holds it. Which of
the three applies is decided per cell, so the same rule covers Datatype defaults and examples, Constants,
Vocabulary values, a Test table's arguments, a Data table's fields, a lookup's axes and a signature's result.

The editor follows that declared type too. Boolean cells are a three-state selection — `TRUE`, `FALSE`, or empty —
and vocabularies use a dropdown that allows only their declared values or empty, without a text input. Numeric values
use a numeric input; Byte, Short, Integer and Long values stay within the range of their declared Java type. Dates use
a date picker, and Character values accept one UTF-16 character, matching OpenL's Character converter. String values
and types represented by a Data-row reference remain text. Changing a Datatype or Constants field type clears its old
default and example instead of carrying a value into a type that may reject it. A Datatype's Mandatory cell is a check
box: checked writes `true`, while unchecked leaves the cell empty.

Rows and columns follow the author rather than the other way round: filling the last row adds another below it,
and where the type has no fixed set of columns — Free Form, Spreadsheet, lookups — filling the last column adds
another beside it. Rows can be inserted and deleted; a wide table scrolls rather than widening the dialog.

Some types own the rows at the top of the body. A Spreadsheet owns the row in which its columns are named; a
lookup owns one row for every argument that runs across the top. Those rows are written to the sheet like any
other, but they belong to the table type: they carry no row controls, and the table is not valid until they are
filled.

Blank rows never reach the workbook. OpenL reads a blank row as the end of a table, so the trailing row kept for
input — and any row left empty in the middle — is dropped on the way out.

## Lookups

A lookup is read where a row and a column cross.

The leading arguments run down the left, one column each; the trailing ones run across the top, one row each. The
corner where the two meet is written as a merged cell, and its height is precisely what tells OpenL how many
arguments are horizontal — so the corner's rows and columns always add up to the signature.

The corner is kept as square as it can be and gains a row before a column: two arguments give 1×1, three 2×1,
five 3×2. Fewer than two arguments cannot form a lookup at all.

```text
SimpleLookup Integer rate(String make, String year, String area)

| make     | 2024  | 2025  |   <- year, the first row of the band
| (merged) | North | South |   <- area, the second
| Audi     | 10    | 20    |
    ^ make, the only vertical argument
```

## What the server is asked for

One question per resource, each asked of the thing that owns the answer:

- **The project's modules** — so the destination can be chosen.
- **The project's types** — its Datatype tables, which is what a vocabulary is written as too.
- **The properties applicable to the requested place** — without a table kind,
  `/projects/{projectId}/properties` describes what may appear in the contents of a Properties table. With a table
  kind, it describes the properties the table itself may carry, filtered by kind and Table inheritance level. Each
  definition carries how the property is presented — its display name, its group and whether the engine dispatches on
  it — and the shape of its value: text, a date, a boolean, or enum codes paired with their display values, plus the
  value it defaults to and the pattern the compiler validates it with. A dialog names, groups, prefills and validates
  a property from that answer rather than from a copy of the rules kept beside it. Date formatting is a client
  concern and is not part of property metadata.
- **The versions of the table being copied** — `/projects/{projectId}/tables/{tableId}/properties` answers with the
  table's own properties and, for a kind of table that carries versions, the one it stands for, the ones its versions
  already carry and the first one left free.
- **The tables a test can call** — the same tables list, narrowed to the kinds OpenL compiles into a method.
- **A module's worksheets** — the only one of these that belongs to a module rather than to the project.
- **The fields of one datatype or the values of one vocabulary** — read when an author picks it, when a tested
  argument opens up into it, or when a typed value needs its finite choices, because the list names tables, not their
  contents.

Nothing else. No table is generated on the server: every skeleton, the generated ones included, is laid out in the
dialog, out of what these answers say the project holds. A server that lays a table out is a second place where
table shapes are decided, and the dialog would show one thing while another was written.

The resources stay separate and are requested when they are needed. Only what every table type needs is asked for
on opening — the modules, the project's types, and the properties. The rest is asked for when it is wanted: a
module's sheets when the destination moves, a datatype's fields when one is picked, and the callable tables when a
Test or a Run table is being written. This keeps independent resources from waiting for one another and avoids
fetching project-level data again when only the module changes.

The callable tables are worth deferring: the list is the project's whole compiled surface, and most tables being
written are not tests. Opening the dialog from Create Test is the exception — that opens straight onto a Test
table, whose skeleton is built from the signature the list carries.

Two of them are not resources of their own: a project's types and its callable tables are both **tables**, and the
tables list already filters by kind.

Copying uses the same resources and adds one read:

- **The source table as `RawSource`** — its header, explicit property rows, body values and merges.

Everything else the dialog needs is fixed by the OpenL language rather than by the project — the table types, the
scalar types, the Environment keywords — and is held in the client. Asking the server for a constant costs a
round trip behind a module compilation and answers a question that has only one answer.

The table itself is submitted as the finished cell matrix, with its merges, and not as a description of a table
type for the server to lay out. One writer materializes it whatever the type, so no table type can be created
that the dialog cannot also show.

## Rules inherited from OpenL

The behaviour that looks arbitrary is not; each rule is the compiler's:

- A blank row ends a table, so no blank row may be written.
- The height of a lookup's top-left cell is the number of horizontal arguments, so that cell must be merged, and
  a row of the band left empty would change the count.
- A Properties table compiles only when it declares `scope`, so that row is there from the start.
- A Spreadsheet needs at least one step, a lookup at least one row to look up.
- A table name becomes an identifier, so it is validated as one.
- A table name does not have to be project-wide unique. OpenL distinguishes overloads and versions using their
  signatures and properties, including properties supplied by the file name, a Properties table, or the table's own
  properties section.
- A worksheet name cannot contain `/ \ * ? [ ] :`, because Excel does not allow it.
- Every property row has both a name and a value, and one property name appears at most once.

## Non-goals

The creation dialog does not merge cells beyond the corner a lookup needs, does not format, and does not validate
against the compiler — a table that OpenL rejects for a reason the dialog does not know about is rejected on save,
and the dialog stays open with the message. The copy dialog does not edit body cells and does not carry Excel
formatting or comments through the raw write. Editing a table after it exists is the table editor's job.
