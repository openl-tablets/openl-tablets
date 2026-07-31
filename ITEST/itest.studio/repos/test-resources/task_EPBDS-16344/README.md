# EPBDS-16344 — Basic Trace error path

Covers the Projects Trace API fixes for a failed Basic Trace run:

- business run-through (`breakOnErrors=false`, `detailedTitles`, `fullTree`) keeps the failed branch with `= ERROR` labels and no duplicate interrupted steps;
- break-on-exception parks on the thrower while ancestor frames still expose the same error in variables, and only the completed throwing frame has stack `error: true`.
