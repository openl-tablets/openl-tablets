# What a Sync brings in, and what the user may not read

A branch holds every project, so a Sync of one project merges the changes of the other projects too. The check of
a merge tells which projects it would change (`scope`), and the result of the merge what it changed (`summary`).
A project the user may not read is counted, never named.

`EPBDS-16896-Own` and `EPBDS-16896-Hidden` are created on master; `EPBDS-16896-reader` may write `EPBDS-16896-Own`
only. The side branch changes a file of each and adds `EPBDS-16896-Added`.

- `030-check-as-admin` — as admin: the three projects, `EPBDS-16896-Own` first, then `EPBDS-16896-Added` (added) and
  `EPBDS-16896-Hidden` by name.
- `040-check-as-reader` — as `EPBDS-16896-reader`: `EPBDS-16896-Own` alone, the two other projects counted in
  `hiddenProjects`.
- `050-merge-as-reader` — as `EPBDS-16896-reader`: the summary of the merge, `EPBDS-16896-Own` alone, two projects
  counted.
