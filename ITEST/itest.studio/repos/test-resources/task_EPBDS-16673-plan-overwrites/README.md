# EPBDS-16673 — the plan says a workbook is replaced only where one stands

The plan behind the generation card reported a module as declared and the card wrote that as *the workbook
rules/Models.xlsx is replaced*, whether or not a file stood there. A reader was told they were about to lose
something that was not there.

The plan now answers two facts rather than one. `declared` says the project already reads a module of that
name — such a module is written where it reads, so its workbook is not the reader's to choose — and
`overwrites` says a file stands at that workbook today, which is what the warning is about.

`010-setup` creates a project, opened, whose `rules.xml` declares `Algorithms` at `rules/Algorithms.xlsx`
and whose repository holds no such file.

`020-plan`

- `010-a-module-declared-where-no-file-stands-is-added-not-replaced` — the module is `declared`, because the
  project settles where it goes, but nothing is overwritten.
- `015-a-workbook-no-module-reads-is-not-called-replaced-either` — `rules/Spare.xlsx` stands in the project
  but no module reads it, so the generation refuses to write over it rather than replacing it; calling that
  a replacement would promise what cannot happen.
- `020-and-the-workbook-it-names-is-not-there` — said plainly by the files API, so the plan above is read
  against what the project actually holds.
- `030-write-the-two-modules` and `040-now-that-a-file-stands-there-the-plan-says-it-is-replaced` — once the
  workbooks are written, the same plan calls both of them replaced.

`999-tierdown` closes the project and deletes it.
