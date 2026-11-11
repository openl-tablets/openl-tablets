# EPBDS-17053 — a test of a dependency opens in its own project

The tests of a project run the test tables of the projects it depends on as well. Their results named no module for
such a table, so the Test Results window wrote its name as plain text and gave no way to open it. Now a test table of
a dependency names its module and the project it is written in, and its name opens it there.

`NotClickable.zip` and `Sample-Project.zip` are the projects attached to the ticket. `NotClickable` holds the module
`example2` with the Spreadsheet `mySpr` and its test table. `Sample Project` depends on `NotClickable`, and its module
`Main` holds only the Rules table `Hello`.

`010-setup` creates both projects and opens them. A search of every table `Sample Project` compiles, the projects
it depends on included, finds the test table `mySpr` in `example2` of `NotClickable`.

`020-the-results-name-the-project-of-the-test`

- `010-run-the-tests` … `020-wait-tests-complete` — the tests of `Sample Project` run.
- `030-the-summary-names-the-project` — the results name the module `example2` and the id of `NotClickable` for the
  test table `mySpr`.

`999-tierdown` closes and erases both projects.
