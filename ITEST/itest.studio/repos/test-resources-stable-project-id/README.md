# Stable Project Ids of Folders Changed Outside OpenL Studio

`StableProjectIdTest` runs these phases around changes it makes on disk, in the `design-file` repository:

- `010-setup` creates two projects, so that OpenL Studio has listed both folders;
- the test then moves the folder of `EPBDS-16946-Moved` into `archive/` and deletes the folder of
  `EPBDS-16946-Removed`;
- `020-folders-changed-on-disk` verifies that the old id of the moved project reads it, answering its new id in the
  body and the address under it in `Content-Location`, and that the id of the deleted project answers `410 Gone`;
- `999-tierdown` deletes the moved project by its new id.

The repository notices changes on disk by polling them, so the first step after the changes retries until the listing
of the projects shows them.

A project deleted in OpenL Studio is verified where the generic scenarios delete their projects: their tierdowns read
it back as `410 Gone`, and `task_EPBDS-16307-project-creation` also copies from the deleted source and reads its
access.
