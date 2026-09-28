# The Project Editing Lock

A project is reserved for one user while it is being written. The reservation is what keeps two people from
editing the same workbook into two different shapes and losing one of them at the commit.

## The whole lifecycle belongs to the back end

No client asks for the lock and no client gives it up. There is no API to take one and none to release one, and
none is wanted: every integration — the Studio UI, an MCP server, an agent driving the REST API — writes tables
through the same endpoints, and an integration that had to orchestrate a lock around its writes would sooner or
later forget to, and write over somebody's work.

So the lock is a consequence of what is asked for, never a request of its own:

| When | What happens |
|---|---|
| Editing a table begins — `GET /projects/{id}/tables/{tableId}/editors`, the request an editor makes when the user presses **Edit**. It answers with how the cells are written and locks the project. | The project is reserved for that user. Held by somebody else, it answers `409` naming them. The table is resolved as a write resolves it, so one that could never be written through those editors — a table gathered from several partial tables, or one belonging to a project this one depends on — is refused and nothing is held. |
| A table is written — the update, append, action, properties, create, copy and delete endpoints | The project is reserved, for the same reason and with the same refusal. This is what covers a client that writes without an editor at all. |
| Editing a table ends — `DELETE` of the same address | The reservation is released, **but only where the project has nothing of its own left to protect**: a table saved into the workspace and not yet committed keeps it. A lock another user holds is left alone. |
| The project is saved — `PATCH /projects/{id}` with `save` | The reservation goes with the save. `RulesProject.save()` ends in `unlock()`, and it always has. |
| The project is closed | The reservation goes with it. |

The lock exists for one thing: the user has changes in their workspace that another user must not write over.
So it is held from the moment a table is taken up until either there is nothing left to protect — the editor is
closed with nothing written — or the changes reach the design repository.

There is one lock per project, not one per table, and two tables of it taken up at once — two tabs of the same
browser — share it. The first editor closed gives it back, because nothing has been written yet and so there is
nothing to protect; the other tab's first write takes the project again, or is refused where somebody else took
it meanwhile. The JSF editor let the lock go the same way, on the unload of whichever table page was left first.

While a project is held, everyone else reads its tables as read-only — their read of the project carries no
`canWrite` and names who is holding it — so the Studio does not offer them Edit at all.

## Breaking a lock is an administrator's, and only for the case it was made for

`DELETE /projects/{id}/lock` breaks the lock whoever holds it, and it requires administration rights on the
project. It exists for one situation: the user holding the project cannot save it, or no longer has access to
it, and the project would otherwise stay reserved for nobody. It is not a way to take a project from somebody
who is working in it — breaking their reservation can lose the work it was protecting.

## History

This is how the JSF editor worked, and there the lock came for free: every action — a cell written, a row
inserted — was a round trip that applied the edit to a `TableEditorModel` living on the server, so the lock
protected real server-side state rather than an intention. **Edit** sent `startEditing`,
`TableBean.beforeEditAction()` called `tryLock()`, and leaving the table page called `MainBean.onPageUnload` →
`TableBean.tryUnlock`, which let the lock go only `if (!currentProject.isModified())` — the rule the `DELETE`
above keeps.

The first React editor kept the button but sent nothing, because the edits live in the browser until they are
saved. The lock was then taken only by the save, and between pressing Edit and saving a second user was offered
the same table; whoever saved last was refused, over work that could no longer be saved anywhere
(EPBDS-16724). Editing a table now says so at both ends, and the reservation is back where it had always been.
