---
name: planning
description: >
  OpenL change planning — the entry point for building, changing, or fixing
  anything in an OpenL Tablets project: readies the projects, drafts one
  ordered plan with test cases, gets one approval, executes it item by item
  through the owning skills, verifies, and summarizes. Use proactively for
  "configure X", "add/update the rule for Y", "fix bug in Z", "implement
  PROJ-1234", "apply this ticket", "why is this wrong", or continuing or
  redoing an earlier task. Not for read-only questions or a plain test run.
---

# OpenL Change Planning

This skill owns the plan, the one approval, the tracking in `plan.md`, and the summary. It delegates:

- root cause of a wrong result → `trace-investigation`;
- branch creation, sync, merge → `branching` (repositories that support branches only);
- test rows, the test run, failure classification → `testing`;
- version and dimension properties → `versioning`;
- change log and replay mechanics → `change-log-and-reapply`;
- domain edit order, business vocabulary, evidence scenarios → the installed domain catalog skill, when there is one.

Scope, tier and estimation come from an upstream change-request analysis when one is handed over: plan from it, never
re-estimate.

## Workflow

1. **Understand.** Read a ticket in full (description, acceptance criteria, comments, samples) and a spec in full;
   missing acceptance criteria is an open question. Check a plain prompt against the live project before assuming
   what is new. A wrong result goes to `trace-investigation` first, then a fix is planned. Continuing an earlier task
   → Resume; redoing work after a conflict, refresh or replaced local state, or a backport →
   Reapplication (see [Modes](#modes)).
2. **Make the projects ready** (no approval needed — opening is not a change). Match the request to projects; an
   ambiguous match is a question. Include every project the domain catalog declares required (for example a shared
   library). Load each project's guidance, then open it on its latest revision. If a required project cannot be made
   ready, stop with the [blocked report](#blocked-report) — never plan against a partial set.
3. **Detect the mode.** A project with `repository: "local"` is local mode; anything else is remote. In remote mode
   record whether the repository supports branches (`features.branches` of `openl_list_repositories`): branch,
   sync and Finish items exist only when it does.
4. **Find the test cases.** Every change needs at least one input with its expected output, configuration changes
   included. When none is given, draft them from the catalog's evidence scenarios, mark them drafted, and have the
   user confirm them with the plan.
5. **Load the domain catalog** that covers the project's domain and take its change plan as the ordered domain items.
   Without a catalog, plan from the project's own conventions and say so.
6. **Choose the view** ([Views](#views)) and **draft one plan** — read
   [references/templates.md](references/templates.md) before writing it. Order: context (research, impact check,
   branch in remote mode, change log start only on a [trigger](#change-log)), domain items, ⏸ external gates, test
   items last, then the Finish item. Across several projects use one branch name and list every identifier that must
   match exactly.
7. **Ask everything in the plan message**, so the user answers once: genuine ambiguities, decisions expensive to
   reverse, data that cannot be inferred, drafted test cases, and every decision the catalog marks **Ask** — with
   its options and the recommended one, never recorded as an assumption. An assumption is only what no reviewer
   would decide differently. Never ask what the project conventions or the catalog already answer.
8. **Get one approval** with the approval block, then stop. Silence or an unrelated reply is not approval. A scope
   edit means restating the plan and asking again. Save the approved plan as `plan.md` — in the change log folder,
   otherwise in `openl-change-log/<slug>/` of the user's working folder.
9. **Execute item by item.** Never silently add, drop or reorder items. After each item: read back the changed rows,
   check compilation with `openl_project_status`, then save (remote) and mark the item done in `plan.md` at once.
   Add no test rows and run no tests between changes. If execution proves the plan wrong or incomplete (for example
   a new dimension is needed), stop, restate the plan and get approval again.
10. **Test, then summarize.** Add the test rows for the confirmed cases, save, run the full suite once through
    `testing`. A fix inside an approved item is verified, saved and the suite re-run once; a fix outside the plan
    needs a restated plan. Before the summary, check every requirement and every confirmed case against readback and
    test evidence and close gaps inside the approved items. The summary lists every open problem the user must decide
    — never leave one only in `plan.md`.
11. **Finish** (only when the plan has the Finish item). Offer the merge, naming the target branch, only when: 0
    failures on the saved revision, every confirmed case checked, nothing unsaved, no open conflict, every ⏸ gate
    closed, no deviation awaiting a decision — otherwise report what is missing. On "yes" hand off to `branching`;
    mark the item done only once the merge is confirmed on the target.
12. **Offer the feedback** when items were recorded: list them and ask whether to send them to the OpenL team; on
    "yes" give one ready-to-send text.

## Approval rules

- **Nothing is written before approval** — no cell, branch, change log or save.
- **Exactly one approval**, for the plan. A catalog or mechanic skill called from the plan does not ask again for
  work the plan covers. Green tests, clean compilation or a clean preflight are checks to record, not approvals.
- **Existing test rows** may change only if the plan item names the Test table, the row key, and the old and new
  expected value; otherwise `testing` asks its own named approval.
- **Gates with another approver** (contract owner, domain expert) are ⏸ items; dependent work waits for them.
- **New scope stops execution** until the restated plan is approved. An `AGENTS.md` edit is a plan item like any
  other.
- **Never merge, sync, save or discard an unsaved working copy, or send feedback on your own initiative.**

## Change log

Start it, and load `change-log-and-reapply`, only when:

- the user asks for it;
- the task is a reapplication, a backport, or carries a change to another branch;
- the working state is expected to be replaced before the merge — a local workbook refreshed from the repository, or
  another branch or user known to edit the same workbooks.

A durable location alone is not a trigger, and a catalog does not turn the log on by default. Otherwise `plan.md`
with the saved revision of each item is the record; the plan says `Change log: not used — <reason>`.

## Views

Every user-facing message is in the **technical** or the **business** view; the view changes wording only — never
the items, their numbers, the one approval, or a safeguard. Choose without asking: an explicit request in the
conversation, then the user's own stated preference (never a project's `AGENTS.md`), then the user's wording (any
table, column, cell, sheet, formula or row term → technical; only requirements and business labels → business);
unclear → technical. Name the view on the plan's first line; a switch re-renders and needs no new approval. `plan.md`
is always technical, with each item's outcome line. Business-view rendering rules are in
[references/templates.md](references/templates.md).

## Modes

- **Local** — no branch, merge, sync or Finish items (the plan says "No merge — local"); local projects are never
  opened or saved, so writes land directly and the tests run without opening; report the local project state
  instead of a revision. Missing project guidance never blocks — read the live project and say the guidance is
  missing; creating an `AGENTS.md` is only an approved plan item. Every other safeguard stays.
- **Resume** — find `plan.md` (ask the user if needed), make the projects ready, check only the
  items marked done with a key-scoped read, and continue from the first unmarked item. Never redo a marked item; if
  the live state disagrees with a mark, report it and ask. The original approval covers the remaining unchanged
  items.
- **Reapplication** — make the projects ready; in remote mode name the target branch (the
  task branch, or the base a new one is cut from). Run the read-only preflight of `change-log-and-reapply` on its
  latest revision. A conflict is reported and stops; a clean preflight becomes a reapplication plan with its own
  approval block, appended to `plan.md`, then executed like any plan.

## Gotchas

- **A found unsaved working copy is not saved or discarded before approval.** Report what it holds and its branch,
  and make keep-or-discard the first plan item; this beats any startup save rule of another skill.
- **Before each save**, the project's pending changes must hold only files this item wrote, and the branch must have
  no revision newer than the one the item started from; otherwise stop and ask.
- **Save groups.** Dependent structural items that cannot compile one at a time (a new dimension across datatypes,
  helpers and result constructors) are declared in the plan as one save group with the reason: read back each item,
  compile and save once at the end of the group. Never group independent items to postpone a save.
- **End a turn only at an item boundary with nothing unsaved**, so the task can be resumed.
- **A cell value starting with `=` is a formula.** Write `'= true`, `'= getBoolean(...)` with a leading apostrophe to
  store it as text.
- **A drafted test case is not proof** until the user confirms it, and a green suite does not close the task while a
  confirmed case is unchecked.
- **Record feedback when it happens**, in the `Feedback` section of `plan.md`: `tool_error`, `unexpected_result`
  (readback differs — other rows changed, rows shifted, merged cells lost, a table created elsewhere), `skill_gap`
  (the request or the catalog was wrong, the user corrected a term, the plan was restated), `missing_capability` (a
  manual step in OpenL Studio), `other`. Each item: category, source, requested, happened, effect; a cause only
  marked "hypothesis"; no payloads, rule values beyond keys, client names or credentials.

## Blocked report

When a step cannot continue, report in this shape and stop:

```text
BLOCKED at step <n> — <step name>
Project: <name> (<remote|local>)
Error: <exact tool error or observed state, one line>
Done so far: <completed items, or none>
Unsaved state: <yes/no and what>
Need from you: <one concrete choice — e.g. fix and retry, skip, or abort>
```
