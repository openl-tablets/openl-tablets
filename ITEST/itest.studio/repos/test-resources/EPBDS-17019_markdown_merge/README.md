# EPBDS-17019 — a table of a module of Markdown files merges line by line

A table of a module of Markdown files is a text file, a rule to a line, so Sync merges it as `git merge` merges any
text: changes of two branches that stand apart are taken both, and changes of one line are a conflict of text left to
a person.

`010-setup` creates the project from the Empty Project template, adds the table `rules/Rating/rate.md` and declares
the module `Rating` as its folder.

Each scenario writes the base of the table on master, branches from it, changes the table on the side branch and on
master, and receives the side branch into master:

- `020-different-rows-changed` — the first rule changed on one branch and the last one on the other merge by
  themselves: the file holds both, and the table compiles.
- `030-same-row-changed` — the first rule changed on both branches is a conflict of text; it is resolved by hand with
  a file of its own, which the file then holds, and the table compiles.

`999-tierdown` closes the project, deletes the side branches and deletes the project.
