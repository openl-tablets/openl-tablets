# Formatting on save in a workbook naming many styles

Excel copies the named styles of a sheet along with it, so a workbook built from the sheets of other workbooks can
name tens of thousands of styles. The workbook of `EPBDS-16931-Many-Styles.zip` names 20,000 copied styles, each with
a cell style of its own, beside the table `adults`.

**Format tables on save** is on by default, so every save formats the table it writes with the OpenL table style.

- **`010-read-unformatted`** — the table as uploaded, in no OpenL style. The read compiles the module, so the saves
  are timed alone.
- **`020-update-age`, `030-update-age-again`** — two saves of one cell, each formatting the table.
- **`040-read-formatted`** — the table in the OpenL table style: the header merged across the table and written in
  its pieces, the field names, the titles and the values in the styles of their roles.

The saves also guard the time formatting takes. The `perf` profile, active unless `-DnoPerf` is given, as on CI,
waits 10 seconds for each response (`http.timeout.read`). Each save takes about a second. Before EPBDS-16931,
formatting looked the styles of the roles up among the named styles in a time that grew with the square of their
number: the first save took 40 seconds and the second 11, and both failed with `Timeout`. With 10,000 styles they
took 10 and 6 seconds, too close to the limit to fail for sure.
