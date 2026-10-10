---
title: Create Decision Tables
description: Create OpenL decision tables through their authoritative raw workbook layout
---

## Summary

Rules, SimpleRules, SmartRules, SimpleLookup, and SmartLookup describe OpenL
semantics, not MCP request variants. MCP exposes their content only as a
`RawSource` cell matrix.

## Workflow

1. Call `openl_get_project_agent_context` and apply the project's guidance.
2. Choose the decision-table semantics required by the rule and load that
   table's bundled specification.
3. Inspect an existing example with `openl_get_table` when possible. Read its
   header, signature cells, condition/action columns, rule rows, merges, and
   blank placeholders directly from `source`. Keep any context-parsed
   multi-value arrays as arrays instead of inventing OpenL lexical text.
4. Build a complete grid that follows the specification. Do not infer a typed
   JSON shape such as `args`, `headers`, or `rules`.
5. Call `openl_create_project_table` with
   `{ tableType: "RawSource", name, source }`.
6. Use raw cell/row/column action tools for corrections, then compile and create
   matching Test coverage.

The transport must remain `RawSource` even when the table encoded in the grid is
a SimpleRules or SmartLookup table. Typed decision-table DTOs are incomplete and
must not be added back to MCP schemas or prompts.
