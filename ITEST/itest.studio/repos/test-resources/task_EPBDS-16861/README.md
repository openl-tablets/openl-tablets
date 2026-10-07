# EPBDS-16861 — Trace of a rule that overflows the stack

The project holds one spreadsheet, `Recurse`, that calls itself with no end, so every run fails with
`StackOverflowError`. The business view (`fullTree`) traces it, and the whole executed tree comes as the flat
`treeNodes` list, every call after the one that made it.

The suites run with `-Xss256k`, so the recursion stops after a few dozen calls, and the golden matches the list as a
whole. The depth at which the old nested tree broke the 1000-level JSON limit (about 250 calls) is covered by
`TraceDebugMapperTest`.
