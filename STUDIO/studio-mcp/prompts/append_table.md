---
title: Append to Table
description: Append complete raw source rows to an existing OpenL table safely
arguments:
  - name: tableId
    description: ID of the table to append data to
    required: false
  - name: tableType
    description: OpenL table kind whose workbook layout is being edited
    required: false
---

## Summary

`openl_append_table` accepts only the authoritative raw workbook representation:

```json
{
  "appendData": {
    "tableType": "RawSource",
    "rows": [[{ "value": "..." }, { "value": null }]]
  }
}
```

Typed append payloads such as `fields`, `rules`, `steps`, `values`, or
`rows: [{ values: [...] }]` are intentionally unsupported. Studio's typed table
DTOs are incomplete and can lose workbook structure.

## Workflow

1. Call `openl_get_table` and inspect `source` to determine the exact row width,
   merged-cell placeholders, and surrounding layout.
2. Consult the relevant bundled specification/guide for the table kind.
3. Build each new row as an array of raw cell objects. Every row must cover the
   full table width. Use `{ "value": null }` for a blank cell and preserve
   `{ "covered": true }` placeholders for merged regions. A context-dependent
   multi-value cell uses Studio's one-dimensional scalar-array representation;
   do not serialize it to guessed OpenL text.
4. Call `openl_append_table` with
   `appendData: { tableType: "RawSource", rows }`.
5. Continue with the returned `tableId`, verify through `openl_get_table`, then
   check `openl_project_status` and run relevant tests.

Prefer `openl_append_table_rows` for the same operation when a narrow raw action
is clearer. Use the other raw action tools for isolated cell/row/column edits;
use `openl_update_table` only when replacing the complete source matrix.

For repository `local`, edit and test directly. For Git-backed repositories,
follow the open/edit/validate/save lifecycle.

Writable arrays must be non-empty and contain only string/number/boolean/null
elements. `[null]`, nested arrays, objects, and string elements surrounded by
Studio-trimmed whitespace or control characters are rejected.
