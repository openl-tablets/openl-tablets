# EPBDS-16959 — a test run and an edit of the project

A test run works with the project as it was compiled when the run started. An edit of the project drops what the
run produced, and a read then says why the results are gone instead of saying there never were any.

- `020-outdated` — the tests run, a property of the test table is written, and the summary answers `410`
  `execution.outdated.message`.
- `030-stop` — the test run and the benchmark of the session are stopped, which answers `204` whether or not the
  execution is still running, and the next test run reads the results of the project as it is now.
