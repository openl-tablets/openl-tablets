# EPBDS-16661 — a generation writes over the workbook a module reads

A project declaring nothing in `rules.xml` reads every workbook under `rules/` by the default pattern, wherever in
that folder it keeps them. Generating tables into a module name the project already reads has to replace the
workbook that module reads. Laying `rules/Models.xlsx` beside `rules/models/Models.xlsx` leaves the project with
two modules of one name, and the author's own module is silently dropped from every compilation.

`010-setup` creates a project, opened, whose data-types module lives in a subfolder — `rules/Algorithms.xlsx` and
`rules/models/Models.xlsx` — and uploads a specification into it.

`020-generation`:

- `010-the-project-reads-its-models-under-rules-models` — the module list before: `Algorithms` and `Models`, the
  latter read from `rules/models/Models.xlsx`.
- `020-the-default-plan-names-the-workbook-each-module-reads` — the plan asked with no names, as the Overview asks
  it for a project whose descriptor names none, answers the default names with the workbooks those modules read,
  both marked as ones the generation writes over — not the default `rules/Models.xlsx`.
- `030-generate-into-the-two-modules` — runs the generation with that plan.
- `040-the-project-still-holds-two-modules` — the module list after is the same two.
- `050-no-second-workbook-was-laid-beside-it` — `rules/Models.xlsx` does not exist.
- `060-the-models-workbook-was-replaced-not-created` — the local history of `Models` holds the workbook as it stood
  before the generation, which a workbook created rather than replaced would not.

`999-tierdown` closes the project, discarding the generation, and deletes it.
