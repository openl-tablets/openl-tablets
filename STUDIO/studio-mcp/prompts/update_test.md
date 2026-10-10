---
title: Update Test Table
description: Update OpenL test cases through the authoritative raw table source
arguments:
  - name: testId
    description: ID of the test table to update
    required: false
  - name: tableName
    description: Name of the table being tested
    required: false
---

## Summary

Test tables must be edited through `RawSource`. Do not construct a typed `Test`
request with `headers` or `rows[{values}]`; that representation is incomplete and
is not an MCP contract.

## Workflow

1. Call `openl_get_table(testId)` and inspect its `source` matrix.
   Preserve context-parsed multi-value arrays as arrays when updating or
   round-tripping Test cells.
2. Consult the bundled Test table specification before changing header,
   parameter, `_res_`, or `_error_` cells.
3. Prefer a narrow raw action:
   - append/insert/delete test cases with the raw row tools;
   - update expected values with `openl_update_table_cell` or
     `openl_update_table_range`.
4. If a complete replacement is necessary, modify the returned object and pass
   the full `{ tableType: "RawSource", source: [...] }` view to
   `openl_update_table`.
5. Run the test table and inspect failures. Re-check `openl_project_status`.
6. Save only validated Git-backed projects; repository `local` has no save step.

Always use the current `tableId` returned by an edit because relocation changes
the id.

Writable arrays must be non-empty and one-dimensional with scalar
string/number/boolean/null elements. `[null]`, nested arrays, objects, and string
elements surrounded by Studio-trimmed whitespace or control characters are
rejected.
