# EPBDS-16460 — a rule that takes another spreadsheet's result as an argument

`PolicyOverrideCalculation` takes the result of the `RatingDetails` spreadsheet and computes from its cells. A
spreadsheet result has no properties of its own to read JSON into, so the argument used to arrive empty: the run
returned a calculation over nothing and the trace showed the parameter as `{}`.

The argument is now read through the bean class OpenL Rule Services publishes for that spreadsheet — its step
names, `Plan`, `Coverages` and `Total` — so the request body of a deployed service can be replayed as is.

- `020-run` — the run API computes `Doubled` from the supplied `Total` and echoes the argument back in the same
  shape it was sent, rather than as the engine's internal row/column tables;
- `030-trace` — the trace suspends at the entry of the same rule, lists `ratingDetails` among the frame's
  parameters, and serves its populated value through the lazy-parameter endpoint the trace window reads.
