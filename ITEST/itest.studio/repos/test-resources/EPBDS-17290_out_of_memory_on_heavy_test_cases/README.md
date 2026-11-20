# EPBDS-17290 — the tests of a project of heavy test cases run out of memory

A test table kept the results of all its cases until the whole table had run. When each case returns a large tree of
spreadsheet results, the heap filled up as the cases ran, and the run ended with `OutOfMemoryError`. Every spreadsheet
result also carried a copy of its table, which nothing read and which took about half of the memory of the results.
Now the values of a case can be given back to free memory as soon as the case ends, and a spreadsheet result keeps no
copy of its table.

`EPBDS-17290.zip` is a project written for this. The Spreadsheet `Item` multiplies a row by a column in one step, so
each call makes one small spreadsheet result. The Spreadsheet `Case` calls `Item` with two arrays of 800 numbers:
the rule runs for every pair of their elements, so a case returns 640,000 results. Its test `CaseTest` runs `Case`
12 times and checks the number of the results.

`010-setup` creates the project and opens it.

`020-every-case-of-the-heavy-test-table-passes`

- `010-run-the-tests`, `020-wait-tests-complete` — all the tests of the project run, as the REST API runs them.
- `030-every-case-passes` — all 12 cases of `CaseTest` ran, and none failed.

`999-tierdown` closes the project without its changes and erases it.

## What the suite catches

The size of the project is chosen for the heap of this module in the `perf` profile: `-Xmx456m`, as
`surefire.argLine.perf` of `itest.studio` sets it. Four cases run at a time, as `test.run.thread.count` sets. The
results of all the cases do not fit into that heap together. The results of the four cases that run at a time do fit,
but only without a copy of their table. So the run fails when either of these comes back:

- **a test table holds the results of every case until it ends** — the heap fills up with the results of the cases
  that ended;
- **a spreadsheet result carries a copy of its table** — the cases that run at a time no longer fit.

Such a run logs `OutOfMemoryError`, and `030-every-case-passes` finds fewer cases than 12. A step may also find
no answer at all: a server out of memory may never finish an answer it started.

The check needs that heap limit. A larger heap leaves the results more room, so the run may pass there even without
the fix: `-DnoPerf` gives `-Xmx712m`, and a build with `-Psonar`, as the Quick Build of a pull request runs, sets no
limit at all. The check bites in a build of the `perf` profile, such as a local `mvn verify` and the weekly Build.
