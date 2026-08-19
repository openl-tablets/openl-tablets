# EPBDS-16459 — a table write may not leave a blank line inside the table

OpenL reads a table only as far as the first entirely blank row or column, so a write that leaves one inside the
table silently drops everything beyond it while reporting success.

- **020** — a typed `Test` table that titles none of its columns is refused, because its title row would reach
  the sheet blank and cut the test cases off; the same table with titles is created whole.
