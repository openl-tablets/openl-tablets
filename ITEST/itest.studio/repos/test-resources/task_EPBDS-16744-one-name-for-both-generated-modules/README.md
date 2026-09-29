# EPBDS-16744 — one name for both generated modules

The rules and the data types are generated from a specification into a module each. The settings saved one name
for both without a word, the dialog blamed the workbook, and the generation compared the names letter case and
all: `Models` and `models` went through as two modules whose workbooks are one file wherever letter case is not
told apart. The Editor refused such names where they were entered: *"Module names cannot be the same."*

`010-setup` creates a project, opened, that holds a specification and declares no module.

`020-names`:

- `010-settings-generating-into-modules-of-one-name-are-refused` — `rules.xml` naming `Models` and `models` for the
  tables it generates is refused where the settings are written.
- `020-the-generation-refuses-names-that-differ-only-in-letter-case` — the call that answered 204, now refused with
  the same message.
- `030-nothing-of-the-project-was-written` — the project reads no module: both refusals are complete.

`999-tierdown` closes the project and deletes it.
