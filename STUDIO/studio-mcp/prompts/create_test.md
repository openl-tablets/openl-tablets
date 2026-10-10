---
title: Create Test Table
description: Create OpenL test tables from their authoritative raw workbook source
arguments:
  - name: tableName
    description: Name of the table being tested
    required: false
  - name: tableType
    description: Type of table being tested
    required: false
---

## Summary

Create Test tables only as `RawSource` matrices. Typed `Test` DTOs are
intentionally unsupported because they omit workbook-level structure and cannot
round-trip reliably.

{if tableName}
Target table: `{tableName}`.
{end if}
{if tableType}
Target semantic table kind: `{tableType}`. The transport still remains `RawSource`.
{end if}

## Workflow

1. Load the project agent context and the bundled Test table specification.
2. Inspect the target rule and an existing Test table with `openl_get_table` when
   available. Its `source` is the authoritative shape to adapt.
3. Build the complete workbook grid, including the Test header, parameter
   columns, `_res_`/`_error_` expectations, blank cells, and merged-cell
   placeholders required by the specification. Multi-value parameter or
   attribute cells may use the one-dimensional arrays returned by Studio.
4. Create it with:

```json
{
  "projectId": "<projectId>",
  "moduleName": "Main",
  "table": {
    "tableType": "RawSource",
    "name": "TestRuleName",
    "source": [[{ "value": "..." }]]
  }
}
```

5. Call `openl_project_status`, run the new tests, and fix source cells through
   the raw action tools. Save only after validation for Git-backed repositories;
   repository `local` has no open/save step.

Never substitute typed `headers`, `rows[{values}]`, or `testedTableName` payloads
for the raw source.

Array-valued cells must be non-empty and contain only string/number/boolean/null
elements. `[null]`, nested arrays, objects, and string elements surrounded by
Studio-trimmed whitespace or control characters are rejected; Studio performs
the context-dependent serialization.
