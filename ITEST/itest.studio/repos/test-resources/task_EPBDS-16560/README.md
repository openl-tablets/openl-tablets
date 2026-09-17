# EPBDS-16560: the input a table takes to be executed

`GET /projects/{projectId}/tables/{tableId}/input` describes what the Trace launcher (and later Run and
Benchmark) asks for. A rule table answers with its declared parameters, each with the JSON schema of the values
it accepts, and with the schema of the runtime context once the project's `rules-deploy.xml` says it provides
one (`040-runtime-context`); a project without the file, or with the option off, is asked for no context, as
OpenL Rule Services provides none by default. A test table declares no parameters of its own: its cases are
read from `GET .../input/cases`, 25 to a page.

In a page of cases a value with inner structure (a datatype) is marked `lazy` and left out.
`GET .../input/cases/{caseId}` reads one case with every value written in full. An unknown table, a rule table
asked for its cases, and an unknown case are all a 404; a page size below 1 is a 400.

A run or a benchmark of the test table may name its cases by a range of ids (`testRanges=1-3,5`). A range that
names a case the table does not have is refused with a 400 before the run is scheduled, so the refusal reaches
the caller instead of failing on the run's own thread.

A run that finds no test to run - a rule table no test table covers, or the tests of the table kept in another
module than the one the run was limited to - completes with an empty summary, and `GET .../tests/summary`
asked for as a workbook saves it as an empty workbook rather than failing (`050-empty-results`).
