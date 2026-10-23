---
name: change-log-and-reapply
description: >
  Keeps a schema-validated log of every applied change of an OpenL feature and
  reapplies only the missing changes after a merge conflict, project refresh,
  replaced local state or backport, blocking on any conflicting or similar
  change already in the target. Use when a plan starts the change log, or on
  "keep a log of the changes", "reapply the changes", "redo the feature after
  the conflict", "replay the change set", "carry this change to another branch".
---

# OpenL Change Log and Reapply

Records applied changes as semantic snapshots and replays only what is
missing — never by row position, never over a conflicting change. Log mode
needs the approved plan; reapply mode needs the log folder and, for a
repository with branches, the `target_branch` (the task branch, or the base a new branch
will start from).

## Ownership

- **Approval, plan, item tracking, summary** — the `planning` skill. Logging
  asks for no approval of its own; a reapplication plan is a new plan with its
  own single approval.
- **Branches and saves** — `branching`; **test runs** — `testing`.
- **What the change should be** (edit order, semantic keys, generated order
  fields) — a domain catalog skill when installed, else the project's
  `AGENTS.md` (`openl_get_project_agent_context`).
- Never edit workbook files locally; all reads and writes go through `openl_*`.

## Log location

`openl-change-log/<ticket-or-slug>/` in the user's working folder — one that
outlives the session and a later session can open. With several candidates,
use the one the task's files come from, or ask once. Never inside the OpenL
project: it would be saved and merged with the rules. If no such folder is
writable, keep the log in the session, say at the start that it cannot be
resumed elsewhere, and hand the validated files to the user at the end.

```text
openl-change-log/<slug>/
├── plan.md                                # approved plan, item status updated
├── openl-change-set.schema.json           # copy of assets/openl-change-set.schema.json
├── <slug>.structure.openl-changeset.json  # column operations, only if any
└── <slug>.data.openl-changeset.json       # row operations, only if any
```

## Mode: log

1. **Before the first write**, create the folder, save the approved plan as
   `plan.md`, and copy [the schema](assets/openl-change-set.schema.json).
2. **Structure first**: apply and verify column changes, then take row
   `before` evidence against the new layout.
3. **After every verified write**, append the operation with its evidence to
   its phase file at once (`execution.status: in-progress`), and mark the item
   in `plan.md`. Read [references/logging.md](references/logging.md) before
   the first append.
4. **At the end**, reduce repeated mutations, validate, rewrite, re-read and
   validate again; set `execution.status: complete`, `saveRevision` and the
   final test counts. Report `LOG_STATUS`.

## Mode: reapply

Follow [references/reapply.md](references/reapply.md): preflight the
**whole** log read-only on the latest target revision; **any conflict →
stop** before any branch or write; a clean preflight becomes a reapplication
plan through `planning`, appended to `plan.md`; write only after its approval.

## Gotchas

- Never identify a row or column by row number, table or row id, cell
  address, column range or position — only by semantic key, expression and
  dimension tuple.
- A live row that already equals the logged `after` is a **conflict** (a
  similar change made independently), not "already done".
- An edit or removal cannot be logged after the fact: if logging started late,
  only additions with proven absence in the base can be logged; report
  `NEEDS_BASE_STATE` for the rest. Never rebuild `before` from memory or tests.
- A key-value change is a remove under the old key plus an add under the new.
- Never repair, re-split or normalize a user's log during reapplication —
  reject it as `INVALID_CHANGE_SET`.
- Only applied changes go into the JSON; planned ones stay in `plan.md`.
- A failed step after earlier writes: stop, report what is saved, ask whether
  to fix forward or revert. Never roll back automatically; never report a
  partial run as complete.
