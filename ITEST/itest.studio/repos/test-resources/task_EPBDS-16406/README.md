# EPBDS-16406 — a breakpoint on a table name honours its `@N` instance suffix

`Calculate` calls `Iteration` three times, so that table has the executions `@0`, `@1` and `@2` within one run.
Every scenario runs the same trace with `stopAtEntry=false`, so the only thing that can suspend it is the
breakpoint the scenario sets.

An indexed name key used to be looked up verbatim: `Iteration@1` was accepted by the breakpoints API and then
matched nothing, so the run completed without suspending and the missed breakpoint was reported nowhere. A name key
now selects an execution exactly as a URI key does:

- `020-name-instance-breakpoint` — `Iteration@1` suspends on the second call. This is the request that used to run
  to completion;
- `030-uri-instance-breakpoint` — the same suspension addressed by `<uri>@1`. It is the control the scenario above
  is expected to match frame for frame;
- `040-name-breakpoint` — an unsuffixed name still fires on the first execution;
- `050-after-name-instance-breakpoint` — `after:Iteration@1` lets that same call run and suspends at its own exit,
  with `$Doubled` executed and the frame's result on the stack;
- `060-non-matching-instance` — `Iteration@9` names an execution the run never reaches, so nothing suspends;
- `070-name-sub-step-breakpoint` — `Iteration#R0C0` suspends on that cell. A sub-step is addressed by either
  form of its table, which is what makes it reachable before the run: `GET /trace/breakpoint-tables` offers
  targets by name, and a URI only exists once the table appears in a live stack;
- `080-name-sub-step-instance-breakpoint` — `Iteration#R0C0@1` narrows that cell to the second execution, so
  the two suffixes compose.
