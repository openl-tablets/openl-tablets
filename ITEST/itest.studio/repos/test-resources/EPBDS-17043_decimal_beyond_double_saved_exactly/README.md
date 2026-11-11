# EPBDS-17043 — a decimal of more digits than a double is saved with every digit

A cell held a written number as a `double`. A `BigDecimal` of more digits lost the last ones on save:
`0.6666666666666666666666666666666667` became `0.6666666666666666`, so a test expecting the quotient of a
`BigDecimal` division never passed. Now a number a `double` does not hold exactly is saved as a text, which OpenL
reads back as the same number, and the table API reads it as that text.

`EPBDS-17043.zip` is a project written for this. The Spreadsheet `divide` returns `a / b` of two `BigDecimal`
values. Its test `divideTest` divides 22 by 33 and expects `0.6`.

`010-setup` creates the project, opens it and finds the test table.

`020-the-expected-value-written-with-every-digit`

- `010-write-the-quotient` — the expected value becomes `0.6666666666666666666666666666666667`.
- `020-the-table-reads-it-back` — the raw table reads the value back as it was written: a text of all its digits,
  which a client reading a JSON number as a `double` would round.
- `030-run-the-tests`, `040-wait-for-the-tests` — the tests of the project run.
- `050-the-test-passes` — the test passes: the expected and the actual value are the same 34 digits.

`999-tierdown` closes the project without its changes and deletes it.
