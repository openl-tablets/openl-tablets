# EPBDS-16492 — spreadsheet errors remain linked after table properties

Spreadsheet table properties occupy workbook rows above the table body. They must not shift a compilation error's
source range when OpenL Studio maps the error back to a table and cell.

- **One property row** — the formula error is linked to `C5`, not to the following `Step2` cell.
- **Two property rows** — the formula error is linked to `C6` and remains visible on the table instead of becoming
  an unlinked project-level error.
- **Property order** — properties are written in request order, so the workbook rows and error-cell coordinates are
  deterministic across Studio restarts.
- **Test execution** — a formula error in the last spreadsheet row remains attributed to that table, so OpenL Studio
  reports its tests as blocked by the compilation error instead of running the invalid method. The Test table body
  carries the same `Tested rules have errors` warning as the legacy UI.
