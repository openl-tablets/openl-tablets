# EPBDS-16861 — Trace of a rule that overflows the stack

The project holds one spreadsheet, `Recurse`, that calls itself with no end, so every run fails with
`StackOverflowError`. The suite traces it the two ways OpenL Studio does:

- business view (`fullTree`) — the whole executed tree comes as the flat `treeNodes` list, every call after the one
  that made it;
- step debugger — the run parks once, at the root call, and shows the stack where it ran out: the deepest frame of
  the start response stays readable and reports the overflow.

The suites run with `-Xss256k`, so the recursion stops after a few dozen calls, a number that differs between
machines: the goldens mask the lists themselves, and the deepest frame read back checks the stack where the run
stopped. The depth at which the old nested tree broke the 1000-level JSON limit (about 250 calls) is covered by
`TraceDebugMapperTest`.
