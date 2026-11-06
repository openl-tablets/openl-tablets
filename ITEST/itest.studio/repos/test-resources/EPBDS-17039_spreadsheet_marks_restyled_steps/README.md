# The marks of a Spreadsheet keep its look

The raw read of a table with its styles carries what the Studio look draws (`regions`) and what the Excel file holds
(`styles`). Both are checked after **Reformat Excel files**, and after a save that marks a step.

- `Quote` returns `SpreadsheetResult` and marks the steps `Premium*` and `Tax*` and the column `Description*`. A
  marked step name is a bold `key`, with no line. The cells it returns, the marked steps × the marked column, keep
  the fill of the values and are `returned`, bold; the values of the column `Value` are not. The marked title stays a
  title with no line under it. No region is the `result`.
- `Total` returns `Double`: its step `RETURN` is the `result`, bold, with no fill, a line above it and a double line
  below it.
- Marking the step `Audit` with `*` and saving twice makes its name a bold `key` and its description bold, with no
  line, as the first marks.
