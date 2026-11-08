# The rights on the localization of a project

The screen of the messages offers changes only when the project tells the user `canWrite`, and the localization API
writes only for a user with the right to write, on a project nobody else holds. A user who may read the project reads
its messages either way.

`localization.zip` is the project of `repos/test-resources/localization_checked_without_compiling`: an empty
`Main.xlsx` and the default, French and Chinese (Taiwan) localization files.

`010-setup` adds the user `EPBDS_16933`, creates the project, grants the user the role of a viewer, and the user opens
the project.

`020-a-viewer-reads-and-writes-nothing`

- `010-the-project-is-not-written-by-the-viewer` — the capabilities of the project hold no `canWrite`.
- `020-the-viewer-reads-the-messages` — `greeting` with its texts and warnings.
- `030-a-change-of-the-viewer-is-forbidden` — a text written over `greeting` is refused with `403`.
- `040-the-text-stays` — `greeting` is as it was.

`030-a-contributor-writes`

- `010-grant-a-contributor` — the user is a contributor now.
- `020-the-project-is-written-by-the-contributor` — the capabilities hold `canWrite`.
- `030-the-change-is-written` — the text written over `greeting` is taken, and the project is the user's to save.
- `040-the-new-text` — `greeting` has the new default text.

`040-another-user-writes-nothing-to-a-held-project`

- `010-the-admin-opens-the-project` — the administrator opens the project the user holds.
- `020-the-project-is-not-written-by-the-admin` — no `canWrite`, though the administrator may unlock it.
- `030-a-change-of-the-admin-is-refused` — the change is refused with `409`: the project is locked.

`999-tierdown` closes the project, throwing the change away, and deletes the project and the user.
