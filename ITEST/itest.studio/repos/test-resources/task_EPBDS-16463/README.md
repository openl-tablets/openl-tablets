# EPBDS-16463 — spreadsheet results in the run and tests APIs

A spreadsheet result carries no properties of its own to read or write, so it travels as the bean class OpenL Rule
Services generates for the spreadsheet. The run and tests APIs used to write the raw value instead, which comes out
as the engine's internal row and column tables — a shape no client can read back, and one no schema described.

`PolicyOverrideCalculationTest` passes a spreadsheet result as an argument and `WrapperTest` asserts a step whose
value is a spreadsheet result, so the tests summary covers both places a spreadsheet result reaches a client.

- `020-tests` — every parameter and assertion carries the spreadsheet's own step names, and the schema of the
  argument describes exactly those steps.

The result of a run is covered by the goldens of `task_EPBDS-15752` and `task_EPBDS-16160`.
