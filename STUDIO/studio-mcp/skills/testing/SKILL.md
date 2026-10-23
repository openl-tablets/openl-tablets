---
name: testing
description: >
  Adds OpenL test rows, runs the project's test suite on the saved revision and
  reports verified per-row results, classifying any failure before fixing it.
  Use when a task's rule changes are complete, a test fails, a test case must be
  added or an existing row changed, or on "run tests", "did the tests pass",
  "verify my changes", "is it safe to sync or deploy".
---

# OpenL Testing

Test-case confirmation belongs to `planning`; save, sync and branch mechanics to `branching`; dated test rows to
`versioning`. For Test table syntax (`_res_`, `_error_`, `_description_`, `_context_`, field paths, foreign keys to
Data tables) fetch the Reference Guide's Test table chapter with `openl_get_guides`.

## Rules

- **Adding test rows is a change of the project**: it runs under a plan the user approved (the `planning` skill).
  Running the suite and reading results need no approval.
- **Existing test rows and tables are never changed or removed** without an approval naming exactly what changes —
  or a plan item naming the table, the row key, and the old and new expected value. "Fix the failing tests" means fix
  the cause, never adjust an expected value to match wrong output.
- **Tests run on the latest saved revision**, never on unsaved or previously reported state.
- **Full suite, once per task**, after all changes and their test rows are saved — not between edits. A narrower scope
  only when the user names it.
- If the tools cannot do an operation, say so and name the manual OpenL Studio step.

## Procedure

1. **Add test rows** as the last edits, after all rule changes are saved; then save. Each rule change gets at least
   one new row. Take inputs from the project's real vocabularies and datatypes. Each expected column must name a field
   of the tested method's actual result type — read that type first.
2. **Pre-test check** — before the run, and again after any change made outside this session (sync, receive, another
   user's save, the user's edit in Studio):
   - Compare the opened revision (`openl_project_status`) with the branch's latest one
     (`openl_repository_project_revisions`).
   - A pending change this task did not write: stop and ask. Never save, close or discard it — closing discards it.
   - The branch has a newer revision: with nothing pending, reopen on it; with something pending, stop and ask. Report
     each newer revision (author, time, comment); if it touches a table this task changed or tests, re-read that
     table. A manual edit is kept, never overwritten.
   - Repository `local` has no revisions and cannot be opened: skip this step.
3. **Compile check** — `openl_project_status` must reach `ok`, `warnings` or `errors`. Count error messages, including
   those in test tables; with errors, fix, save and recheck. Without a terminal state the compile state is
   "unverified" — never infer it from test results. Between a fix and its re-run, repeat step 2 only if someone else
   may have changed the project; step 3 always runs.
4. **Run** `openl_start_project_tests`, then read the summary. Read per-row detail (`openl_get_test_results_by_table`)
   for every table with rows added in this task and every table reported failing.
5. **On a failure**, classify before changing anything:
   - rule logic → fix the rule, save, re-run;
   - reference or lookup data the rule reads → report what is missing or wrong;
   - the test's own inputs → report the mismatch and ask for the named approval;
   - stale state → redo steps 2–3 and re-run.
6. **Report**: total / passed / failed confirmed by per-row detail; the tested revision (or "local workspace");
   compile errors and warnings after the final save (counts or "unverified"), even when all passed; for each failure
   the row (quote `_id_` / `_description_` when present), expected vs. actual, the cause and its action.

## Gotchas

- **A green suite does not prove the project compiles.** Studio skips a test column it cannot bind (e.g. an expected
  field the result type lacks — a generic helper may expose fewer fields than the typed one), reports a compile error
  on the test table and still passes the row with one check fewer. Confirm each new row's checked-field count equals
  its expected columns; an error in a test table blocks the task like an error in a rule.
- The summary count alone can hide a new row's failure — hence the per-row read in step 4.
- `idle` or `compiling` says nothing about errors; an `idle` state right after a save has no messages yet.
- A row with an `_error_` value passes only when the rule raises that error — check `_error_` before calling an
  exception a defect.
- A row fails if any of its `_res_.…` columns mismatches (nested fields, `[i]` array items, `$Step` cells of a
  SpreadsheetResult), not only the first one noticed.
- A blank `_res_` / `_error_` cell in a row asserts "no value / no error"; compare it like any other expected value.
- The row right under the column-path header holds column titles, not data.
- `_id_` gaps are normal — removed cases are not renumbered.
- When an input column takes its value from a Data table (a `>Table` foreign key), the values to change live in that
  Data table, under the same approval rule as test rows.
- A test whose `_res_` is a sentinel such as `ok` and that has no business inputs checks a data invariant; its fix is
  the reference data, not rule logic.
