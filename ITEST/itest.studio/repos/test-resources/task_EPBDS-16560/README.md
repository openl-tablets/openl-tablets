# EPBDS-16560: the input a table takes to be executed

`GET /projects/{projectId}/tables/{tableId}/input` describes what the Trace launcher (and later Run and
Benchmark) asks for. A rule table answers with its declared parameters, each with the JSON schema of the values
it accepts, and with the schema of the runtime context. A test table declares no parameters of its own: its
cases are read from `GET .../input/cases`, 25 to a page.

In a page of cases a value with inner structure (a datatype) is marked `lazy` and left out.
`GET .../input/cases/{caseId}` reads one case with every value written in full. An unknown table, a rule table
asked for its cases, and an unknown case are all a 404; a page size below 1 is a 400.
