# EPBDS-17144: a BigDecimal parameter of a run keeps every digit of a JSON number

The input of a run was read through `double`, so a `BigDecimal` parameter given `2.000000000000000005` held `2.0`
and matched the rule for `2`.

The project is the workbook attached to the ticket: `SimpleRules String myBigDecimal(BigDecimal v)` with the rows
`2 → two` and `2.000000000000000005 → precise`. The table is run with the number given by name and by position, and
both runs answer `precise`.

The answer of the rule is what tells the fix. The framework reads the numbers of a response as `double`, so the
echoed parameter compares equal with or without its digits.

A `BigDecimal` is described as `{"type": "number", "format": "decimal"}`, which tells it from a `double`. The input
description of the table (`015-input-schema`) and the parameter of a run result carry the format.
