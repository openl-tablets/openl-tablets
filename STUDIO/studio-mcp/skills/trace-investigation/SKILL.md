---
name: trace-investigation
description: >
  Investigates an unexpected OpenL rule result with the trace (debugger) tools, names the root
  cause and proposes the minimal fix. Use when the user asks why OpenL produced a wrong
  premium or amount, a null or blank value, a rejected claim, a rule that fired or was skipped
  incorrectly, pastes a rule input payload for explanation, or asks to fix a dispatch table or
  a rule row.
---

# Investigate an OpenL rule result

## Report shape — never reorder

1. **Root cause** — one short paragraph in business language: what produced the wrong outcome
   and why, classified as one of the types below.
2. **Suggested fix** — the minimal change: table, row or condition, corrected logic.
3. **Evidence** — trace facts and table data that prove it.

Infer the audience from the phrasing, never ask. A business user ("why was my claim
rejected?") gets no table names, ids, formulas or URLs, and a fix described as a request to
the rules team. A developer or BA ("why is `AgeFactor` null?") gets table names, formulas,
values, and an offer to apply the fix.

| Type | Fix |
|---|---|
| MISSING DATA — lookup has no row for the input | add the row or reroute |
| WRONG DATA — a row of a lookup or data table holds a wrong value: a typo, a scale (5 for 0.05) or a unit | correct the value; the formula stays |
| WRONG ROUTING — dispatch sends the input to a table that does not cover it | change the dispatch condition |
| MISSING RULE — no decision-table row covers the case | add a rule row |
| FORMULA BUG — wrong field or operation | correct the formula |
| INPUT MISSING/MALFORMED — the caller sent a missing or invalid value | fix the calling system |
| UPSTREAM MAPPING DEFECT — clean run, valid branch, but the JSON misrepresents the UI entry | fix the mapping layer; optional guard in OpenL |

## Workflow

1. **Pin the problem.** Get the expected and the actual outcome; if unclear, ask one question.
   If a ticket id is given and an issue-tracker tool is connected, read the ticket first.
2. **Find the project and entry point** yourself — never ask the user to. Match the project by
   the product words, open it. Pick the entry table by context, then confirm by comparing its
   signature's parameter types with the top-level keys of the user's JSON. The exposed methods
   may be filtered in `rules.xml` (spec `rules.xml` in `openl_get_guides`). Ask only if two
   candidates still fit.
3. **Run the trace.** Add `runtimeContext` only when the rules dispatch on it (`lob`,
   `usState`, dates). Default sequence: profiling
   overview → `openl_watch_trace_cells` on the suspect factors → replay with a `<ref>@<instance>`
   breakpoint at the outlier → `openl_inspect_trace_frame` → `openl_stop_trace`.
4. **Analyse.** Read [references/root-cause-catalog.md](references/root-cause-catalog.md) — the
   section for the issue type — then classify. Read the real table (`openl_get_table`) before
   concluding; for decision tables, dispatching and runtime context look the semantics up in
   the Reference Guide (`openl_list_guides` search `decision table`, `rule versioning`,
   `runtime context`) instead of guessing.
5. **Propose the fix** and stop. Apply it only after the user agrees, through the `planning`
   skill (edit, validate, test, save). Suggest a ticket title when a bug or data gap was found.

## Gotchas

- Profiling start: pass the input, `profiling: true`, `stopAtEntry: false` and an explicit
  `breakpoints: []` — omitting `breakpoints` keeps the previous session's set, so the
  "one-call" run stops somewhere unexpected.
- `profile.hotspots` ranks by time and holds only the top N; a watch series is capped
  (compare its length with `total`). Absence from either is not proof a table or factor is
  fine — go to the suspect directly with a watch or breakpoint.
- On replay of a rule send no input — its last input is remembered; a test table takes its
  test ranges again, as they are not.
- A trace 404 means no debug session — it ends after about 10 idle minutes or with a new
  start — or a frame or value id it does not hold: start the session again. Still failing —
  report the exact error and what you ruled out.
- If the trace cannot run, say so. Never present hand-reading of tables as a trace result.
- A run ending in `error` carries the failing table, step and exception — that frame is the
  first pointer.
- Null can be the intended result; read the whole lookup before calling data missing.
- A clean run with a valid branch is not an OpenL bug until the input JSON is checked against
  what was entered on the UI.
- Trace values contain the payload's personal data and business logic — quote only the
  evidence lines, never dump a whole run.
- A save commits every pending change of the project as one revision: if anything besides the
  fix is pending, list it and get a separate yes, or leave the fix unsaved.
- When the failing table calls a dependency project, investigate there and report the root
  cause at the dependency level.

## Not this skill

- Failing test rows and test runs — `testing`.
- Applying any change to the project — `planning`.
- Fixing on a separate branch — `branching`.
- A rule that differs by date, LOB, state or currency version — `versioning` for the fix.
