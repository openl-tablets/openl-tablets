# EPBDS-16743 — a module generated into a workbook named apart from it

A project that keeps its workbooks under `rules/` reads them by the default patterns, and a pattern names each
module after its workbook. A module generated into a workbook of another name — `Alg` into `rules/Alg12.xlsx` — is
therefore declared beside the patterns, as the generation declared it in 6.4.0. `rules.xml` was then refused for
reading `rules/Alg12.xlsx` twice, although the engine reads a workbook a module declares for that module alone and
leaves it out of the pattern. The refusal came after the workbooks, the classes and the deployment descriptor had
been written.

`010-setup` creates a project, opened, whose `rules.xml` declares no module: it reads its workbooks by the default
patterns.

`020-generation`:

- `010-a-rules-module-is-written-to-a-workbook-named-apart-from-it` — the call of the ticket, which answered 400
  "The path 'rules/Alg12.xlsx' is already read by another module."
- `020-the-project-reads-it-under-the-name-it-was-given` — `Alg` reads `rules/Alg12.xlsx`, and no module `Alg12`
  stands beside it. `Mod` is named after its workbook, so the pattern reads it.

`030-names`:

- `010-a-module-named-like-a-workbook-a-pattern-reads-is-refused` — renaming `Alg` to `Mod` is refused with 400
  "More than one module is named 'Mod'.": the pattern reads `rules/Mod.xlsx` as a module of that name.

`999-tierdown` closes the project and deletes it.
