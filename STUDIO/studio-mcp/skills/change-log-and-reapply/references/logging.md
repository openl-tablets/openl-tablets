# Writing the change log

The log holds what a person recognizes in the table plus enough previous
state for conflict detection — not a tool transcript, export or diff.

## Evidence for every write

Read immediately before and after each write. Prefer key-scoped reads
(`openl_get_table` row slices); read the whole table only when a narrower read
cannot prove presence, absence, uniqueness or the header mapping.

| Operation | Before | After |
|---|---|---|
| Add row | Zero rows match the key | Complete row, exactly one match |
| Edit row | Complete row, exactly one match | Complete row, same key, one match |
| Remove row | Complete row, exactly one match | Zero matches |
| Add/remove column | Header and expression prove absence/presence | Header and expression prove the result |

- Previous state may also come from the same table read on a named base
  revision, or from before/after snapshots the user supplied with a source.
  Memory, tests and current-state-only exports are not evidence.
- Do not log row snapshots against a layout a later column change will alter;
  if that order cannot be proven, report `NEEDS_BASE_STATE`.

## Operations

- `schemaVersion: "1.6.0"`, `$schema: "./openl-change-set.schema.json"`.
- Column operations go to the `structure` file, row operations to the `data`
  file; never mix them, never write an empty phase file. When both exist, set
  `metadata.phase`, `metadata.pairedFile` and distinct `changeSetId`s.
- `order`: positive, unique, dependency order (e.g. a vocabulary value before
  the row that uses it, a result column before the rows that fill it).
- Each operation: `purpose` (one business sentence), `dependsOn`, `status`
  (`verified` only after readback and compile passed), `evidence` (focused
  test counts only if focused tests ran). The full-suite result and tested
  revision go in `execution.tests` / `execution.saveRevision`.
- Targets: `workbook`, `sheetName`, `tableName` — never table ids.
- Add: complete `fields` (test rows included: inputs and expected values).
  Edit: complete `before` and `after`, not only changed fields. Remove:
  complete removed state. Column: `label` and `expr`.

```json
{ "keyFields": ["Operation", "Field Name"],
  "fields": { "Operation": { "value": "Default apply", "type": "string" },
              "Field Name": { "value": "Review Days", "type": "string" } },
  "dimensionValues": [ { "dimensions": [ { "name": "Tier", "value": "Basic" },
                                         { "name": "Region", "value": "CA, TX" } ],
                         "fields": { "Default Value": { "value": 0, "type": "integer" } } } ] }
```

## Semantic keys

`keyFields` holds field names (never values) that find exactly one row. Take
them from the domain catalog (copy exactly, catalog order), else the project's
guidance, else the smallest stable user-visible set proven unique. No ids, row
numbers or addresses. No unique compliant key → `FAIL` with
`AMBIGUOUS_SEMANTIC_KEY`.

A **generated order field** (a numbered column that only reflects physical
order, e.g. `Rule`; named by the catalog or project guidance) is left out of
`keyFields` and every snapshot.

## Values

- JSON string, number, boolean or null exactly as the table exposes them; no
  objects, arrays or tool wrappers. `type` names the literal: `string`,
  `boolean`, `null`, `integer` for a whole number, `decimal` for any other.
- Formatted text and collections (`AK, AL, AR`) stay strings, in the table's
  order; never split or sort them.

## Dimension tuples

For a row whose value columns are labeled by merged header bands, store the
ordinary fields in `fields` and the value cells in `dimensionValues`, one
entry per logical value column:

1. Read the header bands and the complete row; `openl_get_table` lists every
   merged cell's whole area in `merges`.
2. Expand every merge: each covered cell takes its anchor's value. A cell
   that traces to no single anchor → refuse to log the operation.
3. Each tuple holds exactly one `{name, value}` per header row; two columns
   with the same tuple, or an incomplete tuple → refuse.
4. Trim whitespace; a confirmed blank header is `""` (wildcard) — never
   `null` or omitted. Strip one enclosing list bracket (`[NY]` → `NY`). Keep
   numbers and booleans as scalars.

The tuple never names an Excel column.

## Reducing repeated mutations

Before the final rewrite, group row mutations by target and key chain, in
observed order — an edit that changes the key keeps the row in its group —
and reduce each group to its net effect:

| Observed | Logged |
|---|---|
| add A, edit A→B | add B |
| add A, remove A | nothing |
| edit A→B, edit B→C | edit A→C |
| edit A→B, remove B | remove A |
| remove A, add B (same key) | edit A→B, or nothing if A = B |

Each step's prior state must equal the previous `after` exactly, else `FAIL`
with `NON_CONTIGUOUS_ROW_HISTORY`. Overlapping multi-target operations that
cannot be reduced: split them, or `FAIL` with `AMBIGUOUS_ROW_CHAIN`. Result:
at most one mutation per target and key chain.

## Validation and report

Intermediate appends must parse. Before the final rewrite and again after
re-reading it, each file must pass the schema (a Draft 2020-12 validator; with
none, check it by hand and report `schema MANUAL`) and these checks the schema
cannot express: unique `order`; `keyFields` present in the row state and equal
to the catalog entry; no generated order field, ids, coordinates or ranges;
complete snapshots and unique tuples; one mutation per target and key. Any
failure → `FAIL`: keep the files (`execution.status: failed`) — they are the
only record of the writes — report the failures, and have the log corrected
before any reapplication.

```text
LOG_STATUS: COMPLETE | NEEDS_BASE_STATE | FAIL
LOCATION: <folder | delivered to the user>
STRUCTURE_FILE: <path | NOT_WRITTEN>
DATA_FILE: <path | NOT_WRITTEN>
OPERATIONS: <n> add, <n> edit, <n> remove
VALIDATION: schema <PASS|MANUAL|FAIL>, semantic <PASS|FAIL>
OPEN_ITEMS: <none | blockers>
```
