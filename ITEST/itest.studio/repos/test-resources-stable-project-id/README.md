# Stable Project Ids of Folders Changed Outside OpenL Studio

`StableProjectIdTest` runs these phases around changes it makes on disk, in the `design-file` repository:

- `010-setup` creates two projects, so that OpenL Studio has listed both folders, and gives the user `user16938` the
  Manager role on `EPBDS-16946-Removed`;
- the test then moves the folder of `EPBDS-16946-Moved` into `archive/` and deletes the folder of
  `EPBDS-16946-Removed`;
- `020-folders-changed-on-disk` verifies that the old id of the moved project reads it, answering its new id in the
  body and the address under it in `Content-Location`, and that the id of the deleted project answers `410 Gone`;
- `030-a-project-created-in-the-deleted-folder` (EPBDS-16938) creates a project in the folder of the deleted one and
  reads its access: only its creator holds a role, though the deleted project had shared its access with `user16938`,
  and the audit log records the Manager role of `user16938` revoked;
- `999-tierdown` deletes the moved project by its new id, finds the project created in the deleted folder by its
  name and deletes it, and deletes the user.

`StableProjectIdTest` names the projects and the user for every phase (`MOVED_NAME`, `REMOVED_NAME`, `USER`): it runs
each phase on its own, so neither a common `itest.env` nor an id captured in one phase reaches the next.

The repository notices changes on disk by polling them, so the first step after the changes retries until the listing
of the projects shows them.

A project deleted in OpenL Studio is verified where the generic scenarios delete their projects: their tierdowns read
it back as `410 Gone`, and `task_EPBDS-16307-project-creation` also copies from the deleted source and reads its
access.
