# Reapplying a change log

## Phase 1 — validate and preflight (no writes)

1. **Validate.** Read every phase file, record its SHA-256 digest, and check
   it against this skill's schema (never the file's own `$schema` URI).
   `schemaVersion` 1.5.0 and 1.6.0 are accepted. A file that mixes phases,
   lacks `workbook`/`sheetName`/`tableName`, repeats a key mutation, carries a
   generated order field, or has `keyFields` differing from the domain catalog
   → `INVALID_CHANGE_SET`. Never repair it.
2. **Confirm the project** matches `project.name`; never redirect a log to
   another project.
3. **Read the latest state.** First check the pending changes
   (`openl_project_status`): any → stop, and have the user keep or discard them
   through `planning`, since reopening discards them. Then close and reopen the
   project on the latest revision of `target_branch` (remote) or the local
   project, and record the revision. Find each table by workbook, sheet and table name.
4. **Preflight everything.** Evaluate the structure file against live
   headers, project the post-structure layout in memory, then evaluate the
   data file against that projection. Each operation is **missing**
   (precondition holds) or **conflict**.
5. **Any conflict** → stop before any branch or write. Report every conflict;
   say the log must be corrected or re-recorded. Never skip, merge, overwrite
   or mark anything as already applied.
6. **Clean** → the reapplication plan in dependency order: file digests,
   structure phase, data phase, targets, a save after each verified write,
   one test run on the final saved revision. List the values each column
   removal deletes: the log does not record them, so the approval must. Present
   it through `planning` and stop.

## Conflict matrix

| Operation | Precondition | Conflict |
|---|---|---|
| Add row | No row with the same derived key or the same complete state | Key match, full-content match, or ambiguous |
| Add column | No column with the same expression | Same expression (under any label), or ambiguous header |
| Edit row | Exactly one key match, equal to `before` | Absent, several, or any field/tuple differs from `before` |
| Remove row | Exactly one key match, equal to the logged state | Absent, several, or any difference |
| Remove column | Exactly one column with that label and expression | Absent, several, or the expression under another label |

Similarity is never fuzzy. Reads too thin to decide → `AMBIGUOUS_LIVE_STATE`
(a conflict).

## Comparing values

- Field and dimension names compare exactly. Leave the live generated order
  field out of row comparison.
- Scalars compare by kind and value: `1`, `"1"`, `true`, `"true"` differ.
  Integer and decimal forms are equal only when the live column is one
  numeric type and the values are equal.
- A collection-valued string compares as an unordered set of members (strip
  one enclosing bracket, split by OpenL collection syntax, trim). Decide that
  a value is a collection from the live table's declaration only — never from
  brackets, commas or the logged `type`. Undecidable and the strings differ
  → `AMBIGUOUS_VALUE_SEMANTICS`. Write the logged string unchanged.
- Rows: complete state equality — missing, extra or changed fields are
  unequal. Add similarity checks both the derived key and the complete state.
- Columns: the expression is the identity; labels are trimmed and may repeat.
  Expressions compare exactly except CRLF/LF.
- Dimension tuples: rebuild live tuples exactly as in
  [logging.md](logging.md#dimension-tuples); compare as unordered name→value
  maps with equal fields per tuple. Never fall back to physical columns.

## Generated order fields

- Add: read the whole ordering column; use `1` for an empty table, else
  `max + 1` (never fill a gap); several adds take consecutive values.
  Recompute right before each add — drift from the plan → stop with
  `ORDER_FIELD_DRIFT`; a missing or non-integer column →
  `AMBIGUOUS_ORDER_FIELD_STATE`.
- Edit keeps the live value; remove never renumbers.

## Phase 2 — after approval

1. Quote the approval; re-read the files and require unchanged digests, else
   rerun Phase 1.
2. Remote: create or confirm the task branch through `branching` and confirm
   its head still equals the preflighted revision, else rerun Phase 1. Local:
   confirm the local revision is unchanged.
3. Apply and verify every structure operation before any data operation.
4. Before each write re-read the target and compare with the projected state;
   after it read back, compare with the intended state, and save. Stop on the
   first mismatch.
5. Compile (`openl_project_status`), then run the full suite on the saved
   revision through `testing`, reading per-row results.
6. Compilation and suite pass → update the log: `status: reapplied` with
   evidence per operation, `execution.saveRevision`, a `reapplied` history
   entry; update `plan.md`. Otherwise keep the log, set
   `execution.status: failed`, and report the failure: it is corrected before
   another reapplication.

## Reports

```text
<order> <id> <action> <elementType> — <workbook> / <sheet> / <table>
Key: <derived key or column label/expression>
Reason: <reason code>
Expected: <precondition or snapshot>
Observed: <live state or match count>
```

```text
REAPPLY_STATUS: INVALID_CHANGE_SET | CONFLICT | AWAITING_APPROVAL | COMPLETE | FAILED
PROJECT: <name> (<remote|local>)
PREFLIGHT: structure <PASS|FAIL|NOT_RUN>, data <PASS|FAIL|NOT_RUN>
MISSING / APPLIED: <n> / <n>
CONFLICTS: <none | list>
TESTS: <exact counts | NOT_RUN>
SAVE: <final revision | NOT_SAVED>
```

`COMPLETE` only when every missing operation was applied, verified and saved,
and the full suite on the final saved revision had zero failures.
