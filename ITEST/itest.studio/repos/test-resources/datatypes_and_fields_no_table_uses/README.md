# The datatypes and the fields no table of a project uses are told about

Once the whole project is compiled, OpenL Studio tells about each field of a datatype that no table of the project
reads or sets, on the name of the field, and about each datatype and vocabulary that no table refers to, on its table.

These are information messages of the linter, `"severity": "INFO"`: they count as no warning of the project, and a list
of messages tells them after the errors and the warnings.

`datatype-usage.zip` holds the main cases of the project of `DEV/org.openl.rules.test`, `DatatypeUsageCheckTest`, which
checks the rest: the `Model` module declares the datatypes and the vocabularies, and the `Rating` module uses them in
rules, a Data table and Test tables. `email` of `Driver`, `hobby` of `Adult`, `premium` and `agentCode` of `Policy` and
`limit` of `Coverage` are used by no table, and neither are the datatype `Discount` and the vocabulary `Region`. The
Method `describeCoverage` returns twice, so its compilation warns that the second return is never reached.

`010-setup` creates the project, opens it, and finds the tables of its types and the Method `describeCoverage`.

`020-the-linter-tells`

- `010-compile` — compiles `Rating`, and with it the whole project.
- `020-the-status` — the status lists the warning of the compilation first, then the messages of the linter, by the
  table each is about.
- `030-the-fields-of-a-datatype` — `Policy` tells the messages about its fields, each on the cell of the name.
- `040-a-datatype-no-table-refers-to` — `Discount` tells one message on the table, and none about its fields.
- `050-a-vocabulary-no-table-refers-to` — `Region` tells one message on the table.
- `060-a-datatype-used-whole` — `Vehicle`, whose fields a Data table, a Test table and rules use, tells nothing.

`030-a-field-a-rule-reads-is-used`

- `010-a-rule-reads-the-premium` — adds a Spreadsheet to `Rating` that reads `premium` of `Policy`.
- `020-compile` — compiles `Rating` again.
- `030-the-status` — the message about `premium` is gone, the others stay.
- `040-the-fields-of-the-datatype` — `Policy` tells the message about `agentCode` alone.

`999-tierdown` closes the project and deletes it.
