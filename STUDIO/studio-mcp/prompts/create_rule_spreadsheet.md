---
title: Create Spreadsheet Tables
description: Create OpenL Spreadsheet tables through their authoritative raw workbook layout
---

## Summary

Spreadsheet tables are useful for multi-step calculations and intermediate
values. Their MCP representation is always `RawSource`; there is no supported
typed Spreadsheet or SimpleSpreadsheet request.

## Workflow

1. Call `openl_get_project_agent_context` and apply the project's guidance.
2. Load the bundled Spreadsheet specification and formula guidance. OpenL
   expressions are stored as cell text; do not substitute Excel formulas.
3. Inspect an existing Spreadsheet with `openl_get_table` when possible and
   preserve its row/column headers, formulas, blanks, merges, covered cells, and
   context-parsed multi-value arrays.
4. Build the complete `source` matrix and call `openl_create_project_table` with
   `{ tableType: "RawSource", name, source }`.
5. Prefer raw action tools for individual formula/row/column corrections. Use
   `openl_update_table` only for a complete matrix replacement and
   `openl_append_table` only for full-width raw rows.
6. Compile, run tests, and save validated Git-backed projects.

Never send `steps`, `rows` plus `cells`, `args`, or `returnType` as typed table
fields. Those DTOs are incomplete; the semantic Spreadsheet structure belongs
inside the raw grid.
