# EPBDS-16667 — a table opens when its properties name their values as the engine reads them

An enumeration's value is written into a cell by its constant's name, and the engine reads such a cell ignoring
case — the reference guide writes `validateDT` as `on` and `off`. The table screen read the same cell by an exact
match and threw, so `GET /projects/{id}/tables/{tableId}?raw=true` answered 500 with
`No enum constant org.openl.rules.enumeration.ValidateDTEnum.on` and the table was not drawn.

`010-setup` creates a project, opened, whose one module is the workbook of the ticket, as it is: fifteen tables
over two sheets, with `validateDT` written as `on` and `off` in four places — the Properties table of each sheet,
and the two rules tables that declare the property in a properties section of their own.

`020-reading` reads each of those four tables whole, so every property value of the module is read: the
enumerations `usregion`, `country`, `currency`, `lang`, `state`, `region`, `origin`, `caProvinces`, `caRegions`,
`emptyResultProcessing` and `validateDT`, the lists among them, beside the dates, flags and numbers.

- `010-a-properties-table-naming-its-values-that-way-is-drawn` — the call of the ticket, on the module's
  Properties table: the cell written `on` reads as the constant `ON` it names.
- `020-the-same-table-is-drawn-in-the-view-of-its-own-kind` — the read without `raw`, which a Properties table
  answers with its source as well, was refused the same way.
- `030-a-table-declaring-such-a-value-itself-is-drawn` — a rules table carrying the property in a properties
  section of its own, where it is written `off`.
- `040-a-category-sheet-says-the-same-and-is-drawn-too` and `050-and-so-is-the-table-declaring-it-there` — the
  other sheet of the module says the same, so no table of it is left unreadable.

`999-tierdown` closes the project and deletes it.
