# EPBDS-16672 — a generated module is written to a workbook, or to nothing at all

A module is read from an Excel workbook. The generation never looked at the extension a module path ends in,
so `POST /projects/{id}/openapi/generation` accepted `rules/Alg.txt`, wrote a workbook under it and left
`rules.xml` naming it as a module: the file is then served as plain text and read as no module at all. The
old Modules Settings dialog refused such a path — *"The generated file must have an Excel format."* — and
creating a project from a specification still refuses it.

`010-setup` creates a project, opened, that holds a specification and declares no module of its own, so the
two modules the generation writes are both new and the paths the request carries are the paths it would use.

`020-generation` asks for each module in turn to be written somewhere that is no workbook, and then for both
to be written where they belong.

- `005-a-path-the-repository-cannot-hold-is-refused-before-anything-is-written` — `rules/.xlsx` names a
  folder and an extension with nothing in between. The repository refuses such a path when the write
  reaches it, which is one module too late, so it is refused up front as the Editor's dialog refused it.
- `010-the-rules-module-is-refused-a-file-that-is-no-workbook` — the call of the ticket, which answered 204.
- `020-and-so-is-the-data-types-module` — the other module is judged by the same rule, so neither side of the
  request is a way in.
- `030-nothing-of-the-project-was-written` — the project reads no module: a refusal is complete, not partial.
- `040-the-same-generation-into-workbooks-is-written` and
  `050-and-the-project-reads-the-two-modules-it-wrote` — the rule refuses what is not a workbook and nothing
  besides.

`999-tierdown` closes the project and deletes it.
