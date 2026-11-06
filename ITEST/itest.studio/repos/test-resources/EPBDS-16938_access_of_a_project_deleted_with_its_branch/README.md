# EPBDS-16938 — the access of a project deleted with its branch passes to a new project in its folder

Access to a project is kept by its folder, for all branches at once. Deleting the only branch that held a project
left its access behind, and the next project created in that folder from an archive or by copy took it over: a user
the old project was shared with managed the new one, which nobody had shared. Now the deletion of the branch removes
the access of each project it held alone, as Delete does, once no remaining branch holds the folder. A project
created in a folder no branch holds starts with the access of its creator only, however the old project was removed.

The project is made from the archive `Sample Project.zip` of `task_EPBDS-15993`.

`itest.env` names the project, its branch and the user. The ids are captured from the responses.

`010-setup` creates the user `user16938` and the project `EPBDS-16938` in the new branch `EPBDS-16938-feature`, gives
the user the Manager role on it, and reads its access: its creator and the user.

`020-delete-the-branch`

- `010-delete-the-only-branch-of-the-project` — Delete Branch removes the branch and the project with it.
- `020-the-role-is-revoked-with-the-branch` — the audit log records the Manager role of the user revoked.

`030-a-project-created-in-the-folder`

- `010-create-a-project-in-the-folder` — a project of the same name is created from the archive in `master`, in the
  same folder.
- `020-only-its-creator-has-access` — its access lists its creator only.

`999-tierdown` erases the project and deletes the user.
