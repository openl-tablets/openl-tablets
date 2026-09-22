# EPBDS-16666 — a migrate of a project that declares nothing writes a descriptor the project can hold

A project without a `rules.xml` reads every workbook in its root as a module. Migrating it moves the workbooks
under `rules/` and writes the `rules.xml` that declares them — a write the project files API checks, and refuses
for a new descriptor that names no project. The migrate wrote such a descriptor, so it answered 400 with the
workbooks already moved: the project declared nothing, listed no module and, with no root workbook left, was
offered no migrate to repair itself with.

`010-setup` creates a project, opened, from an archive holding an `.xls` and an `.xlsx` in its root, and deletes
the `rules.xml` the import wrote for them, so the project declares nothing.

`020-migrate`:

- `010-the-migrate-is-offered-for-the-root-workbooks` — both root workbooks are reported as movable.
- `020-the-migrate-moves-them-and-writes-the-descriptor` — the migrate is accepted.
- `030-the-descriptor-names-the-project-and-keeps-every-moved-workbook-a-module` — the descriptor names the
  project and declares the module set in its minimal form: the `.xlsx` through the `rules/**/*.xlsx` pattern, the
  `.xls`, which no pattern matches, on its own.
- `040-the-workbooks-lie-under-rules` — the root holds the descriptor and the `rules/` folder alone.
- `050-the-modules-are-the-moved-workbooks` — the project reads both workbooks as its modules.
- `060-nothing-is-left-to-migrate` — the migrate is no longer offered.

`030-nested-workbook` — a migrate never widens the module set:

- `010-create-a-project-keeping-a-workbook-under-tests` — the project keeps a scratch workbook under `tests/`,
  which is no module of a project without a descriptor, since only its root is read.
- `020-the-descriptor-the-import-wrote-is-deleted` — the project declares nothing again.
- `030-the-migrate-is-offered-for-the-root-workbook-alone` — the scratch workbook is not moved.
- `040-the-migrate-moves-it-and-writes-the-descriptor` — the migrate is accepted.
- `050-the-descriptor-declares-the-moved-workbook-on-its-own` — the minimal form would declare no modules and
  read the scratch workbook through the `tests/**/*.xlsx` default, so the moved workbook stays declared on its
  own instead.
- `060-the-scratch-workbook-is-still-no-module` — the module set is what it was.
- `070-the-rewrite-that-would-make-it-one-is-named-and-withheld` — the rewrite to the minimal form is offered
  as blocked, naming the workbook it would turn into a module, as it is for any descriptor.

`999-tierdown` closes both projects, discarding their changes, and erases them.
