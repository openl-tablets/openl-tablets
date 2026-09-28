# EPBDS-16724 — editing a table locks the project

The lock exists for one thing: the user has changes in their workspace, and another user must not write the same
project over them. In the JSF editor it came for free — every action was a round trip that applied the edit to a
`TableEditorModel` on the server, so **Edit** locked the project (`TableBean.beforeEditAction()` → `tryLock()`)
and leaving the table page released it, but only `if (!currentProject.isModified())`.

The React editor keeps the edits in the browser until they are saved, so nothing reached the server between Edit
and Save and the lock was taken only by the save. A second user was offered the same table, wrote it, saved
first, and the first user's save was refused with a 500 over work that could no longer be saved anywhere.

Editing a table now says so at both ends of one address:
`GET /tables/{id}/editors` begins it — the meta information the screen needs, and the lock — and `DELETE` of the
same address ends it, releasing the lock only where the project has nothing of its own left to protect. A client
that writes tables without an editor needs neither: every write endpoint locks the project by itself. The Unlock
action on the project is untouched and stays an administrator's.

The project is read here as the *other* user throughout: a project you are holding yourself reads as one you may
write, and only the other user's read says who is holding it.

- `020-taking-a-table-up` — the second writer may edit the project; the first writer then begins editing.
- `025-closing-the-editor-with-nothing-written` — the editor closes with nothing written and the project is free
  at once, as leaving the table page freed it in the old editor. Then it is taken up again.
- `030-the-second-writer-is-refused` — the read now answers `VIEWING_VERSION`, `lockedBy: admin` and no
  `canWrite`, and both beginning to edit and writing the table answer `409` naming who is holding it. This is the
  bug: before the fix the second writer kept `canWrite`, was offered both, and the first writer's save failed.
- `035-a-refused-reader-closing-takes-nothing-away` — the refused reader's editor closes too, and that leaves the
  holder's lock exactly where it is.
- `040-the-save-gives-the-project-back` — the first writer writes the table. Now the project has something to
  protect: closing the editor keeps the lock, and only the save releases it.
- `050-the-second-writer-takes-it-up-now` — the second writer begins editing, and the first writer is turned away
  in turn: the refusal is about who is holding the project, not about who the user is.
