# Projects Merge API - Architecture Design

**Status**: BETA — the design behind [Projects Merge API](projects-merge-api.md)

The API merges the Git branches of a project, keeps the conflicts of a merge or of a project save for the client, and
resolves them. This page describes the components, the flows, and the rules that keep the editor consistent.

## Components

All the code is in `STUDIO/studio-backend`.

```mermaid
flowchart TB
    Client["REST client"] --> Ctrl["ProjectsMergeController<br/>/rest/projects/{projectId}/merge"]
    Ctrl --> MS["ProjectsMergeService<br/>check, validate, merge"]
    Ctrl --> ML["ProjectsMergeLauncher<br/>run a merge in the background"]
    ML --> MS
    ML --> MR["ProjectsMergeRegistry<br/>@ClientSessionScope"]
    ML --> SH
    ML -- status --> Topic["/user/topic/projects/{projectId}/merge/status"]
    Ctrl --> CS["ProjectsMergeConflictsService<br/>describe, read, resolve"]
    Ctrl --> SH["ProjectsMergeConflictsSessionHolder<br/>@ClientSessionScope"]
    Ctrl --> CL["ComparisonLauncher<br/>compare a conflicted workbook"]
    Ctrl --> WS["WorkspaceProjectService<br/>workspace, editor, WebStudio"]
    MS --> Git["BranchRepository (Git)"]
    CS --> Git
    CS --> XM["XlsWorkbookMerger<br/>sheet and cell merge"]
    Save["Project save"] -- SaveMergeConflictEvent --> Lst["SaveMergeConflictEventListener"]
    Lst --> SH
```

- **`ProjectsMergeController`** (`org.openl.studio.projects.rest.controller`) — the endpoints. It pauses the compilation of
  an opened project for the length of a resolution, and brings the workspace and the editor to the state after it.
- **`ProjectsMergeService`** (`org.openl.studio.projects.service.merge`) — checks where the branches stand, refuses a
  merge that must not be performed, and merges.
- **`ProjectsMergeLauncher`** — validates a merge and runs it on the `mergeExecutor` pool, apart from the request that
  starts it, reports its progress on the WebSocket, and brings the editor up to date once the merge has ended.
- **`ProjectsMergeRegistry`** — keeps the merge a client started last (`StartedMerge`): the merge itself and the
  editor state it found.
- **`ProjectsMergeConflictsService`** — describes a conflict (revisions, file availability, default message), groups the
  conflicted files by project, reads one version of a file, and resolves.
- **`ProjectsMergeConflictsSessionHolder`** — keeps the unresolved conflict of one client.
- **`SaveMergeConflictEventListener`** — stores the conflict that a project save found in the same holder.
- **`ComparisonLauncher`** (`org.openl.studio.compare.service`) — starts the comparison of the two versions of a
  conflicted workbook; the comparison is read through the Compare API.
- **`XlsWorkbookMerger`** (`org.openl.rules.xls.merge`) — merges two versions of a workbook against their base, sheet
  by sheet. A sheet both versions changed goes to **`XlsSheetMerger`**, which merges it cell by cell, the value, the
  style and the comment of a cell each on its own, unless the changes overlap. When that fails, for instance because
  **`XlsSheetShifts`** tells that a version moved the content of the sheet, **`XlsTableMerger`** merges the sheet
  table by table instead: **`XlsSheetTables`** splits each version into tables and pairs them by their headers,
  **`LineAlignment`** aligns the rows or the columns of a table changed in both versions with the base version, and
  **`XlsFormulaReferences`** keeps the sheet a conflict when a formula or a defined name refers to content that moves.
- **Models** (`org.openl.studio.projects.model.merge`) — the records of the requests and the responses.

## Check and Merge

`POST /merge/check` and `POST /merge` run the same validation, in the same order:

1. **State** — the project is in the design repository and not only in the workspace, the repository supports
   branches and has a branch besides the current one, and the project has no unsaved changes. Otherwise `409` with
   `project.merge.invalid.state.message`.
2. **Permission** — the user has the write, delete, or create permission on some artefact of the project. Otherwise
   `403`.
3. **Branches** — `otherBranch` is not the current branch, and it exists. Otherwise `409`.
4. **Bypass** — for a protected target branch, `force=true` and the bypass eligibility (the Manager role while
   `security.allow-bypass-protected-branches` is `true`). Otherwise `409` or `403`.
5. **Lock** — the project is not locked on the target branch. Otherwise `409`.
6. **Changes** — the target does not hold every change of the source yet. Otherwise `409`.

`check` reports the result as `CheckMergeResult` and never refuses for steps 4 to 6: `blockedBy` carries the protected
branch and the lock, and `status` says `up-to-date` for step 6. `merge` runs all six, and then merges:

- **`receive`** merges `otherBranch` into the current branch. **`send`** merges the current branch into `otherBranch`.
  The target of the merge is the branch that changes.
- The Git repository merges the source into the target with the user as the author, and the service waits up to
  30 seconds until the project index publishes the target branch. The merge is committed by then, so an index that
  is late is logged and the merge stays a `success`: a failure would have the user merge again what is already
  merged (EPBDS-16541). A resolution waits for the index the same way.
- A conflict in Git gives a `MergeConflictInfo` with the details of the conflict. Otherwise the merge is `success`.

## Merge in the Background

A merge of a large project parses three versions of every conflicted workbook and takes minutes, longer than a reverse
proxy waits for an answer (EPBDS-16469). `POST /merge` therefore answers `202` once the merge is validated and runs it
on the `mergeExecutor` pool, which runs two merges at once and queues the rest: each merge holds its workbooks in
memory.

```mermaid
sequenceDiagram
    participant C as Client
    participant R as Request thread
    participant P as mergeExecutor
    C->>C: subscribe to /user/topic/projects/{projectId}/merge/status
    C->>R: POST /merge
    R->>R: validate, register the merge
    R-->>C: 202 Accepted (PENDING on the topic)
    P->>P: merge in the repository (STARTED)
    P-->>C: COMPLETED, or ERROR with the reason
    C->>R: GET /merge
    R->>R: bring the editor up to date, once
    R-->>C: 200 success or conflicts
```

- **One merge of a client at a time.** `ProjectsMergeRegistry` keeps the merge a client started last. Another merge is
  refused with `409` (`project.merge.running.message`) while it runs: it would change the project the running merge
  changes. An ended merge stays until the next one starts, or until the conflicts it left are resolved or cancelled
  (`ProjectsMergeLauncher.forget`), so its result can be read again: kept longer, it would report conflicts nobody has
  any more and hold what comparing the conflicted workbooks found. Two starts of one client never run at once: a start
  holds the lock of the client (`ProjectsMergeRegistry.startLock()`) from its checks until it has taken its merge, so
  the merge it replaces has ended and reached the editor.
- **The project is read-only for the client while it is merged.** `MergeInProgressInterceptor` refuses every request
  with a `projectId` path variable but a read (`GET`, `HEAD`, `OPTIONS`) with `409` `project.merge.running.message`
  while a merge of that project by the client runs. An edit, a save, a branch switch or a close would be dropped by the
  reopen at the end of the merge, or would take the merge onto a branch it was not validated for. A request that changes
  nothing though it is no `GET` — a compilation, a run of tests or tables, a trace, a comparison, a search — is a read
  too: its endpoint carries `@ChangesNothing`, so a page reloaded during a merge still compiles and shows the project.
  The endpoints of `ProjectsMergeController` stay open, and find an ended merge brought to the editor all the same. The
  project is resolved only while a merge of the client runs. Other clients, other users included, work on as before; the
  merge still merges the branch it was validated with (`StartedMerge.currentBranch`).
- **A merge that runs is no error** (EPBDS-16636). The same merge asked for again while it runs — the same project,
  branch and mode — answers `202` and is the merge already running. It is not validated again: the running merge may
  have committed already, which leaves nothing to merge. `GET /merge` answers `202` while it runs and never `409`. Two
  such requests running alongside take one merge: the second one waits until the first has taken it, and finds it
  running.
- **The background works in the repository only.** The merge is handed the user workspace taken while the request
  answered, and an opened project stays open and compiled in the editor meanwhile: a client that reloads the page in
  the middle of a merge keeps a working editor. `POST /merge` does not wait for the compilation either: the editor
  model is the one of `WebStudio`, read when the merge ends. The editor state belongs to the requests of the client:
  `WebStudio.reset()` publishes `WorkspaceResetEvent` to `@ClientSessionScope` listeners, which no thread outside a
  request finds.
- **A merge that waited for a thread asks again what other users can change.** When it starts, a target branch where
  another user has taken up the project, or that holds the other branch already, refuses it with
  `project.merge.branch.locked.message` or `project.branch.merge.not.mergeable.message`, reported as `ERROR`, and
  `GET /merge` answers `409` with the reason.
- **A merge that ends is completed before it is announced**, so a client that reads the result on `COMPLETED` finds it.
  A failure no handler takes, out of memory for one, still ends the merge as failed and reports `ERROR`: a merge that
  never ended would refuse every later merge of the client. A status the socket cannot send is logged and changes
  nothing in the merge: its result is read from `GET /merge` all the same.

## Cancelling a Merge

`DELETE /merge/conflicts` cancels a running merge as well as the conflicts a merge has left. A merge waiting for a
thread ends at once and never runs. A running merge is stopped by interrupting its thread:

- **The repository stops at a step that has published nothing.** `GitRepository.merge` waits for the lock of the
  repository interruptibly — behind another long merge — and asks `Thread.isInterrupted()` before each conflicted
  workbook it compares and merges, a workbook of a large project takes seconds, and before it pushes. Up to the push
  the merge is local. An interrupted merge throws `InterruptedIOException`, and the repository resets the branch to
  the commit the merge started from, as it does for every failed merge, so a long merge stops within seconds and
  leaves the branches as it found them. A comparison of a workbook that an interrupt cuts off is a stopped merge, not
  a conflict.
- **The reset is not cut off.** JGit writes the work tree through channels an interrupt closes, so `resetToCommit`
  clears the interrupt status for the reset and sets it again afterwards.
- **An interrupt is never swallowed.** Whoever catches an `InterruptedException` sets the status again, and the
  repository leaves it set. The launcher tells a cancelled merge from a failed one by it: a failure while the thread
  is interrupted ends the merge as `cancelled`, reported as `INTERRUPTED`, when the interrupt caused it — an
  `InterruptedIOException` other than a network timeout, a `ClosedByInterruptException` or an `InterruptedException`
  among its causes. Any other failure stays a failure, logged and reported as `ERROR`. The status stays set when the
  merge returns; the pool clears it before it gives the thread other work.
- **The interrupt reaches this merge only.** `StartedMerge.Execution` knows the thread running the merge and lets it
  go when the merge ends, so a cancel that comes later does not interrupt the work the pool gives the thread next.
- **A merge that has started to publish finishes.** A push cut off half way may have reached the remote repository
  already, while the merge would be reset locally and reported as cancelled. So `GitRepository.merge` announces the push
  through `MergePublishing`, on the thread of the merge, before its last check of the interrupt, and
  `StartedMerge.Execution` no longer interrupts the merge from then on: a cancel that came before still stops it, as the
  merge is local up to then. Its result is `success`, whatever the cancel asked for, and it still waits for the project
  index: `ProjectIndex.awaitBranchUninterruptibly` waits on through an interrupt and sets the status again afterwards.

## Merge in the Editor

A merge is made in the repository, so a closed project is merged as well as an opened one. Only an opened project has
the editor state to keep consistent. `ProjectsMergeLauncher`:

1. brings the merge that ended before to the editor, refuses unresolved conflicts, and validates the merge before it
   touches the editor;
2. merges in the background, while the editor keeps the project open, compiled and read-only for the client;
3. brings the editor up to date in the first request of the client about the project that finds the merge ended —
   `MergeInProgressInterceptor` does it before every such request, and `GET /merge` and `POST /merge` before they read
   the merge (`settleFinished`):
    - on `success`, reads the project again from the workspace, which builds its projects anew on a refresh, pauses
      the compilation of the project (`WebStudioWorkspaceRelatedDependencyManager.pause()`), so it does not read a
      project being closed (EPBDS-12353), closes the project, refreshes the workspace, and opens the project again on
      its branch (a project that the merge deleted stays closed, and a project that the merge renamed is found by its
      path), then resets `WebStudio` and the module info. The reset drops the paused dependency manager; a failure on
      the way resumes it. A project with changes of its own, or switched to another branch meanwhile — by another
      client of the user, which shares the workspace — is left as it is: closing it would drop those changes;
    - on `conflicts`, stores the `MergeConflictInfo` in the session holder;
    - after a failure or a cancel, leaves the editor as it is: the merge changed nothing.

Requests that find the merge ended while another one brings the editor up to date wait until it is done
(`StartedMerge.settleOnce`), so none of them goes on with a project that is being opened again.
`DELETE /merge/conflicts` brings an ended merge to the editor before it looks for its conflicts: a merge that ended
after the request was let in keeps them only then.

A failure to bring the editor up to date is logged, not raised: it belongs to no request that happened to find the
merge ended, and the result of the merge stays what it is.

## Conflict Storage

`ProjectsMergeConflictsSessionHolder` is a `@ClientSessionScope` bean: it lives as long as the client, whether a browser
session or the state kept for a credential (see [Client Sessions](../architecture/client-sessions.md)).

- It keeps **one** conflict, with the `ProjectIdModel` of the project. A conflict that is stored for another project
  replaces it.
- The reads are by project: asking for another project than the stored one finds nothing, so a client sees
  `404 Not Found` with `project.merge.result.not.found.message`.
- `check` and `merge` refuse with `409` while the conflict of the project is stored.
- A conflict ends when it is resolved, when `DELETE /merge/conflicts` cancels it, or with the client.
- The holder stores two kinds of conflict. A **merge conflict** has the branches (`mergeBranchFrom`,
  `mergeBranchTo`, `currentBranch`). A **save conflict** has none: the project save found that another user changed
  the project, and `PATCH /rest/projects/{projectId}` answers `409` with `project.save.merge.conflict.message`.
  In a save conflict `OURS` is the version being saved and `THEIRS` is the version that another user saved first.

## Resolution

`POST /merge/conflicts/resolve` takes the resolution of each conflicted file:

1. **Validation** — each file is in the conflict, no file is named twice, and a `CUSTOM` resolution has a non-empty
   uploaded file. An uploaded Excel or ZIP file is read to see that it is complete.
2. **Versions** — `BASE`, `OURS`, and `THEIRS` are read from the commits of the conflict, not from the working tree. A
   version that does not hold the file deletes the file.
3. **Workbooks** — a conflicted Excel file whose changes do not overlap is merged by `XlsWorkbookMerger` without a
   decision: a sheet changed in one branch only is taken from it, a sheet changed in both is merged table by table, row
   by row and cell by cell. The same merge completes a `POST /merge` and a project save without a conflict when every
   conflicted file is such a workbook. Such files are not listed in the request or in `resolvedFiles`.
4. **Commit** — for a merge conflict the repository completes the merge with the resolved files and the message of the
   request. For a save conflict the project is saved with them. Without a message the service builds one: the commit of
   the other branch, the conflicted files, and the workbooks resolved automatically.
5. **Project descriptor** — a resolved workbook whose module the `rules.xml` of one side declares and the `rules.xml` of
   the branch does not is declared again in a second commit with the same message.
6. **Editor** — the controller clears the stored conflict, refreshes the workspace, and opens the project again if it was
   opened, as after a merge.

A refused resolution leaves the stored conflict as it was, so the client can send the request again.

## Errors

The errors are `RestRuntimeException`s with message codes in `ValidationMessages.properties`. The advice maps them to
`{"code": "openl.error.<status>.<key>", "message": "..."}`; see [Errors](README.md#errors) and the table in
[Projects Merge API](projects-merge-api.md#error-handling). A Git failure of a merge is reported on the status topic
as `ERROR` with its reason, and `GET /merge` answers it with `500`.

## Testing

- **Unit tests** — `STUDIO/studio-backend/test/org/openl/studio/projects/service/merge` and the controller
  tests next to the controller; `ProjectsMergeLauncherTest` runs merges on a queue the test drains, so it sees the
  request answer before the merge runs.
- **Git** — `GitRepositoryMergeConflictsInExcelTest` and the other merge tests in `STUDIO/org.openl.rules.repository.git`.
- **Workbook merge** — `XlsSheetMergerTest`, `XlsTableMergerTest` and `XlsSheetShiftsTest` in
  `STUDIO/org.openl.rules.xls.merge`: the changes of the two branches, kind by kind, merged or kept a conflict;
  `XlsTableMergerTest` crosses what each branch does with a table: leaves, creates, removes, changes or moves it.
- **Integration** — the declarative suites in `ITEST/itest.studio`; `task_EPBDS-16883-excel-cell-merge` merges a
  sheet changed in both branches through the table API, a table theme included. A merge step is three requests: the
  `POST` answered `202`, a `GET /merge` retried until the merge has ended, and a `GET /merge` checking its result.
