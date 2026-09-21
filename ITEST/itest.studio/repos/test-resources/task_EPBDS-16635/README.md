# EPBDS-16635: a run table runs as it stands

A `Run` table compiles to a test suite the way a `Test` table does: it lists the cases the rules it names are
run over, and states no expected values. `POST /projects/{projectId}/tests/run?tableId=…` of such a table
runs its cases and reports them, one per case, as a test table's run does, each with the value it returned as
its `result`; the workbook the summary is saved to carries the value in a `Result` column. Run through the test
tables that cover it instead - the way any other table is run - it had nothing to show, and the result was empty.

The project is the one attached to the ticket: one `Rules` table, `Hello`, and the `Run` table `HelloRun`
that runs it over one case.
