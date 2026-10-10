---
title: Validate Project After Edit
description: "Workflow for validating OpenL projects after editing tables/rules: call openl_project_status, branch on compileState, surface errors with location, and re-validate after fixes"
arguments:
  - name: projectId
    description: ID of the project being edited
    required: false
  - name: branch
    description: Branch the project is opened on (omit for repository 'local' and non-branch repos)
    required: false
---

## Summary

After any edit to a table or rule (`openl_update_table`, `openl_append_table`, `openl_create_project_table`), call `openl_project_status` to confirm the project still compiles. If `compileState` is `errors`, surface the diagnostics with file/module context, propose fixes, re-validate. Only `openl_save_project` once `compileState` is `ok` (or you have explicit approval to save with warnings).

Table content is always `RawSource`. Preserve the complete raw matrix—including
context-parsed multi-value arrays—on replacement and use the narrow raw action
tools for isolated fixes; do not reconstruct a typed table DTO or guess OpenL
array text from diagnostics.

# Validate-After-Edit Workflow

{if projectId}
## Validating: **{projectId}**{if branch} on branch **{branch}**{end if}
{end if}

## When to call `openl_project_status`

- Immediately after each edit operation (one call per logical change-set, not per row)
- Before `openl_save_project`
- When the user asks "did that compile?" or "is the project still green?"

`openl_project_status` defaults to `wait: true` and returns a conclusive result. If the studio has no compiled model in the current session (`idle`), the tool lazily triggers compilation through the tables API; if compilation is already running, it waits for the terminal state. Pass `wait: false` only when a quick snapshot is explicitly needed.

## Response shape (relevant fields)

```json
{
  compileState: "idle" | "compiling" | "ok" | "warnings" | "errors",
  compilation: {
    messages: { items: [...], total, errors, warnings },
    modules:  { total, compiled, compiledModules?: [...] },
    tests:    { total }
  },
  pendingChanges: { total, files: [{ path, type: "added"|"modified"|"deleted" }] },
  lastModifiedBy: { author, date },
  revision, branch
}
```

When `compileState === "ok"` the tool drops `compilation.messages.items` to keep the response small — counts and module/test totals are preserved.

## Branching by `compileState`

### `ok`
- Safe to `openl_save_project` (requires `comment`).
- For local projects (`repository: "local"`), there is no save — the change is already on disk.

### `errors`
- DO NOT save. Filter `compilation.messages.items` by `severity === "ERROR"`.
- Group by `location` (file / module / table) so the user sees the impact area.
- For each error, propose a concrete fix. Common patterns:
  - **Type mismatch** → adjust column/field type or add a conversion.
  - **Reference not found** → check `openl_list_tables` for spelling (OpenL is case-sensitive) or create the missing Datatype.
  - **BEX syntax** → balance brackets, verify Excel formula syntax.
  - **Decision-table gaps/overlaps** → fill gaps or resolve overlapping conditions; check `validateDT` property.
  - **Ambiguous dispatch** → resolve overlap in dimension properties (state, lob, effectiveDate, …).
- Apply the fix via `openl_update_table` / `openl_append_table` and call `openl_project_status` again.

### `warnings`
- Surface the warnings (filter `severity === "WARN"`) and ask the user whether to address them or proceed.
- Do not silently save — warnings often indicate latent issues.

### `compiling`
- A `wait: false` snapshot can return this while Studio is compiling. Call `openl_project_status` again with the default `wait: true` to wait without polling.

### `idle`
- A `wait: false` snapshot can return this when no compilation is registered in the Studio session. Call again with the default `wait: true`; the tool triggers lazy compilation automatically. If it still returns `idle`, the project has no compilable module (or a design project must first be opened with `openl_open_project`).

## Response format tips

- `response_format: "markdown_concise"` — best for the "is it green?" check. Shows state + counts.
- `response_format: "markdown_detailed"` / `"json"` — use when fixing, so all message fields (id, summary, severity, location, stacktrace) are visible.

## What NOT to do

- Do not save when `compileState` is `errors`. Even if the user insists, surface the diagnostics first.
- Do not poll `openl_project_status` in a tight loop — use its default `wait: true` behavior.
- Do not assume `pendingChanges` mirrors your edits perfectly — it diffs the working copy against the design revision, so newly created files count as `ADDED` until saved.
- Do not pass `branch` for `repository: "local"` projects — the backend will return 409.
