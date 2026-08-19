# EPBDS-16459 — a table write may not leave a blank line inside the table

OpenL reads a table only as far as the first entirely blank row or column, so a write that leaves one inside the
table silently drops everything beyond it while reporting success.

- **020** — a typed `Test` table that titles none of its columns is refused, because its title row would reach
  the sheet blank and cut the test cases off; the same table with titles is created whole.
- **030** — creating a raw table whose middle column is blank in every row is refused.
- **040** — a complete raw update that blanks a middle column is refused, and the table stays as it was.
- **050** — cell and range actions are refused once they would leave a whole row or column blank; the clears that
  keep the line filled still go through.
