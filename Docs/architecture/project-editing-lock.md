# The Project Editing Lock

A project is reserved for one user while it is being written. The reservation is what keeps two people from
editing the same workbook into two different shapes and losing one of them at the commit.

## The whole lifecycle belongs to the back end

No client manages the lock, and there is nothing for one to call: no endpoint asks for a lock and none gives one
up. Every integration — the Studio UI, an MCP server, an agent driving the REST API — writes tables through the
same endpoints, and an integration that had to orchestrate a lock around its writes would eventually forget to,
and write over somebody's work.

The lock is a consequence of what is asked for, never a request of its own. Editing a table is asked for at both
ends and the lock follows: the request that begins editing takes it, and the one that ends editing gives back
what there is no longer anything to protect. Neither says the word lock, and neither is about it.

The one endpoint that does name the lock is `DELETE /projects/{id}/lock`, and it is not for a client to manage
its own: it breaks somebody else's, and it is an administrator's — see below.

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

## How an operation says it holds the project

By being marked `@LockForEditing`. Nothing in the method body takes or releases a lock: the annotation is read by
`ProjectLockInterceptor`, which settles that the caller may write the project, locks it, and gives back a lock it
took itself where the call was refused — so a request that wrote nothing leaves the project free, and a lock the
caller was already holding is left alone.

The write right is settled *before* the lock on purpose. A lock stands in the name of whoever holds it, so one
taken for a reader who may not write would turn a legitimate writer away with a conflict naming somebody who was
never editing at all.

It is Spring AOP through an advisor rather than an aspect: `ProjectLockPostProcessor` publishes a
`DefaultPointcutAdvisor` over the annotation, the same shape `CommitInfoPostProcessor` beside it already uses,
and the application carries no AspectJ. It proxies the target class rather than its interfaces, because the
service is injected by class.

A method marked `@LockForEditing` has to name the project it is about; one that names none is a mistake and says
so, rather than running as an unguarded write.

The `GET` that begins editing is deliberately not a safe method: it answers with the metadata and takes the lock
in the same breath, because that request is the only thing that unambiguously says "I am taking this table up to
write it".

While a project is held, everyone else reads its tables as read-only — their read of the project carries no
`canWrite` and names who is holding it — so the Studio does not offer them Edit at all.

## A lock left behind

Editing begins and ends with two requests, and the editor sends them in the order the reader did them. A tab
closed in the moment between the two — Edit pressed and the tab shut before the server has answered it — is the
one case that can leave a project held with nobody editing it: the take is already on its way and the release
has nothing to be sent after. The reader gets the project back by saving or closing it, and an administrator can
break the lock; EPBDS-15633 is where this goes away altogether.

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
