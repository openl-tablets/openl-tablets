# EPBDS-16960 — the results of a test table say how the table is written

A test table can be written transposed: its cases across the columns and its parameters down the rows. The Test
Results window lays the results of such a table out the same way, a case to a column. The results tell it which
tables are written so.

`EPBDS-16960.zip` holds the module `Rules` with the Simple Rules table `premium` and two test tables of it with the
same two cases: `PremiumTest`, written the usual way round, and `TransposedPremiumTest`, written transposed.

`010-setup` creates the project and opens it.

`020-the-results-say-which-table-is-transposed`

- `010-run-the-tests` … `020-wait-tests-complete` — the tests of the project run.
- `030-only-the-transposed-table-says-so` — the results of `TransposedPremiumTest` say `"transposed": true`; those
  of `PremiumTest` say nothing of it. Both list the same cases.

`999-tierdown` closes and erases the project.
