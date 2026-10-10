---
title: Create OpenL Table
description: Comprehensive guide for creating OpenL rules, spreadsheets, and data definitions from raw workbook source
---

## Summary

Choose the OpenL table kind that matches the user's logic, then express it as an
exact workbook grid. The MCP table-content API is `RawSource`-only: typed table
DTOs are intentionally unavailable because they omit important cells,
properties, spans, and layout information.

## Workflow

1. Load `openl_get_project_agent_context` and discover the relevant bundled
   table specification with `openl_list_guides`/`openl_get_guides`.
2. Select the semantic table kind—Rules, lookup, Spreadsheet, Datatype,
   Vocabulary, Data, Test, or another supported OpenL kind—based on the guide.
3. When possible, inspect a correct table of that kind with `openl_get_table` and
   adapt its `source` matrix. Preserve blank and covered cells, merged spans, and
   formula text exactly. Preserve context-parsed multi-value arrays as arrays;
   Studio is responsible for their OpenL workbook serialization.
4. Create the table with `openl_create_project_table` using only:

```json
{
  "moduleName": "Main",
  "table": {
    "tableType": "RawSource",
    "name": "TableName",
    "source": [[{ "value": "..." }]]
  }
}
```

5. Check `openl_project_status`, add/run tests, and correct the raw cells with
   the narrow source action tools. Save validated Git-backed projects; do not
   open/save repository `local` projects.

Never send typed `SimpleRules`, `Spreadsheet`, `Datatype`, or `Test` request
objects to create/update/append tools. The semantic kind lives inside the raw
OpenL grid; the transport discriminator remains `RawSource`.

Writable values are scalar string/number/boolean/null values or non-empty
one-dimensional arrays of those scalars. `[null]`, nested arrays, objects, and
string elements surrounded by Studio-trimmed whitespace or control characters
are not representable.
