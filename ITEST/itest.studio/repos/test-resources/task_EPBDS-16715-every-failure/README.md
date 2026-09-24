# EPBDS-16715 — the summary can be asked for every failure of a test table

The screen offers **Failures per test** to cap how many failures of one test table the results list, and the
engine has always read a cap of `TestUnitsResults.ALL_FAILURES` as "every one of them". The REST API had no
way to ask for that, so the **All** the old pages offered could not be offered at all. It is asked for the
way the same operation already asks for every test table on one page: a flag of its own, `allFailures`,
beside a `failures` that stays a count.

`EPBDS-16715.zip` is a project written for this: one rule that always answers `hello`, and one test table
whose seven cases all expect `bye`. **Seven failures in one table is the point** — more than the default cap
of five, so a request for all of them answers with something a request for the default cannot.

`020-summary`

- `010-the-default-count-lists-the-first-few` — `failures=5` lists five of the seven.
- `020-every-failure-is-listed` — `allFailures=true` lists all seven. Against the unfixed code the parameter
  does not exist and the default caps the list at five, which this golden catches — as it would catch a fix
  that took the flag and then quietly fell back to the default.
- `030-a-count-of-none-is-refused` — `failures=0` is a bad request. The count stays a count, so nothing can
  ask for a summary that reports failures at the top and lists none of them.

`999-tierdown` closes the project and deletes it.
