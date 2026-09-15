# EPBDS-16599 — Where a test table's cases begin

`TestCases.xlsx` holds the same rules table tested three ways over, one for each shape a test table is written
in:

- `downTheRowsTest` — a case is a row, which is the shape the Editor numbered;
- `acrossTheColumnsTest` — the table is written the other way round, so a case is a column;
- `oneCaseTest` — written the other way round and holding a single case.

The Editor numbered only the first of the three. The suite covers what a screen needs to number all of them:
the read says which way round the table is written (`layout.transposed`) and the line its data begins on
(`layout.firstDataLine`), and says nothing at all about a table that is not a test.
