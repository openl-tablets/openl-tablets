# Plan and Summary Templates

## Plan

Post it before touching anything, for one item or twenty. Omit an empty group.

```text
View: <technical | business> — reply "<business view | technical view>" to switch.
Plan — <ticket or short task name>
Mode: <remote | local>   Projects: <names>   Branch: <name | none (local)>
Ready: <projects opened at revision … | local, available>

Context
⬜ 1. <research or diagnosis, if needed>
⬜ 2. <dependency impact check>
⬜ 3. <check for an existing branch, else create it (branching) — repositories with branches only>
⬜ 4. <start the change log — only on a trigger; else "Change log: not used — <reason>">

<Domain> change plan (from <catalog skill>) — each item or declared save group is verified and saved
⬜ 5. <domain item>
⬜ 6. <domain item>   [save group with 5 — <why they cannot compile separately>]

External gates
⏸ 7. <gate — named approver>

Test (after all changes)
⬜ 8. Add test rows C1..Cn (testing)
⬜ 9. Run the full suite once on the saved revision

Finish
⬜ 10. Check dependent projects' suites, then ask to merge <task branch> → <base>; merge only on your "yes" (branching)

Test cases
- C1 (from AC): <input> → <expected>
- C2 (drafted, please confirm): <input> → <expected>

Open questions (every catalog "Ask" decision, with options and the recommended one)
Risks (what the configuration cannot guarantee on its own)
Assumptions (only what no reviewer would decide differently)

⛔ AWAITING APPROVAL — execution has NOT started.
Reply "proceed" to execute, or tell me what to adjust.
```

- The approval block is always the last two lines, verbatim.
- The technical view lists, for every rule row an item writes, its exact cells: keys, conditions, expression text
  verbatim or "blank", result — with identifiers spelled as stored.
- Include Finish when the repository supports branches and the changes must reach a base branch (the default).
  Otherwise replace it with one line saying why: "No merge — local", "No merge — the repository has no branches",
  the user keeps the work on the task branch, or nothing in OpenL changes.
  Approving the plan approves only asking; the merge waits for a "yes" after the summary.

## Summary

```text
Done — <task name>

- <item>: <what changed, one line>
Tests: <X/Y passed> on <project>, read from per-row detail.
Verification case: <input> → expected <…> → actual <…> — <match | mismatch>.
State: <branch and revision | local project saved>.
Change log: <path | not used>.
Deviations from the plan: <none | what and why>.
Open problems: <none | problem — effect — decision needed, one per line>.
Feedback: <none | category — what happened, one per line>.

<Finish item:> Ready to merge this to <base branch>?
<feedback items:> Send the feedback to the OpenL team?
```

## Business view

For product owners and analysts who know the product, not OpenL. Same groups, items and numbers as the technical
plan; only the wording changes.

- Use the catalog's business vocabulary: field labels, displayed values, segment and operation names. Leave out
  table, column, sheet and file names, cell addresses, expressions, raw codes, revisions, tool and skill names.
- The branch is "a separate working copy"; merging is "publishing".
- Each item is one outcome line — what the rules do after it (the catalog's `Outcome` field when it has one).
- Test cases are business scenarios; risks state the effect on a user or a downstream system, not the mechanism.
- Status lines: "Step 6 of 15 done: allowed values for the three fields are in place."
- Ask decisions the catalog tags `technical` go to their own group with a shortcut:
  `Technical questions (a developer can answer; reply "use recommended" to accept all)`.
- Keep exact identifiers on one `For the record:` line after the explanation for a change to an existing test row, a
  blocked report, a workaround for a tool defect, and any change to a shared project.
- The summary says what the rules now do, each acceptance criterion as proven or not proven (with why), the
  decisions needed, and that the full detail is in `plan.md` and the technical summary.

The same item in both views:

```text
Business:  ⬜ 9. Review Days is pre-filled with 10 for Priority and 5 for Standard when a review is required.
Technical: ⬜ 9. FieldDefaultValues — append 2 rows, operation [Default apply]:
                 Review Days | Review Required?=[true] | = segment == "Priority" | 10
                 Review Days | Review Required?=[true] | = segment == "Standard" | 5
For the record (existing test row): OperationApplicability, row 14, _error_: "[Create, Recalculate]" → "[Create, Recalculate, RENEWAL]"
```
