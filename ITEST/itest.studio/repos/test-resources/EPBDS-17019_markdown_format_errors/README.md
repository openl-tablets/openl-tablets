# EPBDS-17019 — the format errors of a Markdown file belong to its table and lead to its line

`format-errors.zip` holds a project of one module, `Greetings`, written as a folder of Markdown files, one table to a
file:

- `farewell.md` — three problems of its front matter: the key `license`, which the front matter does not know, a
  `description` written as a property, and a merged range wider than the table. The table is read without them.
- `salute.md` — a row with more cells than the first row of the table.
- `welcome.md` — a front matter that is not closed, so the file holds no table.
- `greeting.md` — a file that breaks no rule.

A format error of a file that holds a table belongs to the table: to the cell it shows in, or to the header of the
table when no cell shows it, as a key of the front matter. Each one tells the path of the file, its line and the place
of the line besides, as only the text of the file shows what is wrong. A file without a table is pointed at by its
line alone, as a message of the module.

`010-setup` creates the project and opens it.

`020-the-tables-hold-the-errors-of-their-files`

- `010-the-tables` — the table `farewell` has three errors, `salute` one, and `greeting` none.
- `020-the-farewell-table-holds-the-errors-of-its-front-matter` — the body of the table `farewell` holds its three
  errors, each with the line of the file it is on.
- `030-the-salute-table-holds-the-error-of-its-row` — the body of the table `salute` holds the error of its row.
- `040-the-status` — the compilation lists the five errors: those of the tables, and the one of `welcome.md` at the
  line of the file.

`999-tierdown` closes the project and deletes it.
