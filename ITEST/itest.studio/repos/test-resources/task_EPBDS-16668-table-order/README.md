# EPBDS-16668 — the tables are listed in the order the module is written in when that is asked for

The tables of a module were always answered by name, and the Editor's tree sorted them by name again, so the
By Excel Sheet view could not show the order the workbook holds its tables in — which is what the guide says it
shows. The order is the compiler's own: workbook by workbook, sheet by sheet, and top to bottom within a sheet.
Nothing had to be read out of the workbook again to get it, only kept.

`010-setup` creates a project, opened, whose one module is the workbook of the ticket, as it is: its tables stand
in no alphabetical order — `_MyRules` (row 2), a free-form `Test123` (row 3), two versions of `MyRules` (rows 6
and 7), another `Test123` (row 14) and `Atable` (row 18).

`020-listing` reads them whole, the free-form tables included, so every table of the module takes part:

- `010-by-name-unless-another-order-is-asked-for` — a request that asks for no order is answered by name, the
  way it always was, so nothing that reads this endpoint moves.
- `020-in-the-order-the-module-is-written-in` — `sort=position` answers the tables where they stand.

`999-tierdown` closes the project and deletes it.
