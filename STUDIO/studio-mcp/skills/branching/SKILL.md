---
name: branching
description: >
  Manages OpenL project branches: opening or creating a task branch on its
  latest revision, syncing with the base branch, handling merge conflicts,
  merging to the base after dependent projects pass, the hotfix two-branch
  rule, and branch deletion. Use for "create a branch", "switch branches",
  "sync with development", "merge to development", a merge conflict, a bug in
  a released version, or "delete the branch"; `planning` hands branch items to
  it. Only for a repository that supports branches (`features.branches`).
---

# OpenL Branching

## Rules

- Never edit rules on the base branch; work on a task branch. A repository whose `features.branches` is false
  (`openl_list_repositories`) — `local` among them — has no branches: skip every step here and say so.
- Merge, sync, delete a branch, or pick a conflict side only on the user's explicit request — at most recommend.
- Under an approved plan `planning` decides when things happen: before approval only find and open existing branches;
  creating one is a plan item. An unsaved working copy found at startup belongs to `planning` then.
- When a tool cannot do an operation, say so and name the manual OpenL Studio step; never report it done.

## Start

1. Look for a branch named by the task id (`openl_list_project_branches`); open it rather than creating a duplicate.
2. Otherwise create one from the base branch, passing the base as `revision` explicitly. Use the repository's naming
   pattern; fallback `PROJ-1234-add-discount-tier`, or `fix-rate-rounding-20260728` without a ticket — lowercase, no
   spaces.
3. Open the project on that branch and confirm branch and revision before touching a table.

Unsaved working copy without a plan: read its branch and tell the user what it holds — it may be a prior session's
edits. Save it to its branch (commented as a prior-session checkpoint) or discard it only on their answer; if it was on
another or unknown branch, reopen on the task branch afterwards.

## Sync and merge

- A plain "sync" is **receive** only. **Send** only on an explicit merge, finalize or send request.
- Before a send: find the target with `scope: "repository"` (ask if ambiguous); find the projects that depend on this
  one and run their suites too — a dependent failure blocks the merge, and "no dependents" must be checked, not
  assumed.
- Receive from the target first when it moved on; save, re-run the full suite through `testing`, stop on a failure.
- Send. `up-to-date` → report "nothing to merge". A lock or protection blocker → report it and stop; use the
  protected-branch bypass only when the user asks for it after you explain it.
- After the send, report the target's new revision and offer branch deletion.

## Conflicts

1. Read the conflicts and, for a workbook, its conflicting tables and cells; compare yours/theirs/base as evidence.
2. Recommend per file or table: both changes needed and disjoint → merge cell by cell; one side already holds the other
   → take that side (a merge would duplicate it).
3. The user decides every side — in Studio, or by telling you, then resolve exactly that. Or cancel the pending merge
   and reapply the change log (`change-log-and-reapply`, run as a `planning` Reapplication).
4. Save the result and re-run the full suite on the saved revision. Never report "ready to merge" with a conflict open.

## Hotfix: two branches, never a merge

A bug in a released version is fixed twice:

1. Branch off the **released** revision (not development, not what is open), fix, save, test, release from it.
2. Separately redo the fix's intent on a fresh task branch off **development**, against its current tables.

Never merge the hotfix branch into development (partial history) nor development into a released branch (ships
unreleased work).

## Delete

Only on request, hotfix branches included. First verify the changes reached the target and its tests pass; never
delete a branch with unsaved edits or an open conflict. Confirm data loss or force only when the user does.

## Gotchas

- `openl_open_project` with `revision` gives a read-only view; to work on an old revision, branch from it and open the
  branch.
- `openl_create_project_branch` without `revision` branches from HEAD, which may not be the intended base.
- A merge target may not hold the project yet: list branches with `scope: "repository"`, not the default scope.
- `openl_check_project_merge` `mergeable` is no conflict preview; conflicts appear only on the merge itself.
- Studio keeps the conflict state for this client: inspect, resolve or cancel it through the same MCP server.
- Confirm each save moved the revision on; an unchanged revision means it did not commit.
- Report a compile state only from `openl_project_status`: `idle` means not compiled yet, not clean.
- Never fold a prior session's pending edits into this task's save.
