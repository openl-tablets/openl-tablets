# EPBDS-17159, EPBDS-17217 — Every answer of a trace of a rule that overflows the stack

The project, built through the REST API, holds one spreadsheet, `Recurse`, whose only step is `= Recurse(n + 1)`,
so every run fails with `StackOverflowError`. The suite sends the requests the trace window sends and checks that each
of them answers in full:

- `020-business-trace` — the business view opens the trace, cancels the launch session and traces the whole run. The
  run keeps its executed tree with the calls marked `= ERROR`, and so does the next trace of the same table;
- `030-click-the-deepest-call` — a click on the deepest call of that tree runs to it again. The run stops on the
  overflow, the call it stopped in reports it, and Trace into File writes the calls of the run as failed.

Where the stack ran out, a class initialized there for the first time stayed broken until the JVM restarted
(EPBDS-17159). When that hit, both traces answered without `treeNodes` and Trace into File wrote every call with an
empty line. It depends on what the JVM ran before, so the suite does not catch it on every run.

The depth of the overflow changes from run to run (EPBDS-17217), so the goldens mask the lists and the call is read
where the run stopped, not at the call the click asked for.
