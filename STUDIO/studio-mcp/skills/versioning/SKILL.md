---
name: versioning
description: >
  Adds a new version of an OpenL rule table or module scoped by business dimension properties (effectiveDate,
  startRequestDate, lob, state, country, currency) and tests both periods. Use when a rule, rate or configuration
  must apply from a date or per LOB/state/country/currency, or on "effective date", "new version of the table",
  "applicable from", "starting from", "replace rates as of".
---

# Versioning

A new version is a change of the rules: it runs as an item of a plan the user approved under the `planning` skill.
Without an approved plan, read `planning` first and draft one — even when the user says "prepare" or "do it", write
nothing, not even a copy, before the approval.

OpenL selects among same-signature tables by their business dimension properties and the runtime context. The
mechanics are in the guides Studio serves — find them with `openl_list_guides` (`search`) and read with
`openl_get_guides`:

- **Rule Versioning** (table properties chapter) — the property list with matching context variables,
  effective/expiration and request-date selection, overlaps, `: context.<var>` Datatype fields.
- **Category and Module Level Properties**, **Properties Defined in the File Name / Folder Name**, **Keywords Usage
  in a File Name** (`CW`, `Any`) — inherited properties and module versioning.
- `spec/rules.xml` — `properties-file-name-pattern`.

Allowed property names, types and enum values for a table: `openl_list_table_property_definitions`.

## Procedure

1. **Find the level the project already uses.** Read `rules.xml` for `properties-file-name-pattern`, and look for
   module- or category-scope Properties tables with `openl_list_tables`. A table without a `properties` section may
   still inherit version properties — check before calling it unversioned.
2. **Module versioning** (the pattern versions files or folders) — the new version is a new module:
    - start from the latest file of the *same* module family; each family has its own dates, never derive one
      family's name from another's;
    - copy the whole file with `openl_copy_project_file` to the new name; never rebuild it table by table — a table
      copy is a table version;
    - make the period's changes in the new module only; the old one stays unchanged;
    - add no table-level version properties inside it.
3. **Table versioning** (no file pattern, task confirmed to one named table — the default):
    - never edit the existing version in place; copy it with `openl_copy_table` into the same module, passing the
      same `name` and the source table's `sheetName`;
    - set the new `effectiveDate` (and other dimensions) through `properties` on the copy;
    - change only the copy's data; keep table name, signature and parameter names identical.
4. **Category or module scope** — writing a property to a module/category Properties table or changing the file
   name pattern affects every table in scope: get the user's explicit approval first (a plan question under the
   `planning` skill). Reading inherited properties needs no approval.
5. **Compile** with `openl_project_status`; an ambiguous-dispatch error means two versions match the same context.
6. **Test** both periods (see Tests).

## Shared modules and files

Modules the pattern excludes (typically `.*Model`, `.*Tests`, `.*API`) and non-module files (`i18n/*.properties`,
`AGENTS.md`, descriptors) serve every version. A change there reaches the old period too:

- a new enum or vocabulary value is valid before its effective date (a validation message lists it in the old
  period as well);
- a new Datatype field must compile against every version of every module that builds that Datatype;
- a new message key is visible to all periods.

List each shared change in the plan as a risk with its effect on the old period, and prove the old period with tests.
A change that must not reach the old period belongs in the versioned module.

## Tests

The `testing` skill owns writing and running tests; for versioned tables add:

- one test table covering the old and the new period, each row carrying its own date — a `_context_.currentDate`
  (or `_context_.requestDate`, `_context_.usState`, …) column, or the request field bound with `: context.<var>`;
- rows on the exact `expirationDate` (still the expiring version — the date is inclusive) and on the day after;
- before reusing a test helper, check it does not hard-code a date — then it only ever reaches one version.

## Gotchas

- `openl_copy_table` `properties` **replaces** the whole property set: list every property to keep (e.g. `state`)
  next to the changed `effectiveDate`. Omitting it keeps the source properties — a duplicate version.
- `openl_copy_table` puts the copy on a sheet named after the table unless `sheetName` is given.
- Never version the same property at module (file name) and table level at once; never declare one property in both
  the file name pattern and the module Properties table.
- File names carry property **values**, never names: `MyModule-US-20270101.xlsx`, not `MyModule-Country-US-…`; the
  same for patterns (`%country%`, not `Country-%country%`). `CW` and `Any` are keywords, not values to replace.
- Change the file name pattern only when the project already versions by file, or the user asks to split rules per
  dimension value — a new property alone never implies it.
- Overlap validation does not cover date properties: overlapping effective ranges are not flagged, only tests catch
  them.
- Write a property date in the form the existing version shows (`openl_get_table`), not in a form you assume.
- Prefer a `: context.<var>` Datatype field to an explicit `runtimeContext`; `isProvideRuntimeContext` in
  `rules-deploy.xml` is deprecated — do not enable it unasked.
- If a tool cannot complete a step, say so and name the manual OpenL Studio step; never report success you did not
  get.

## Boundaries

- `planning` — plan, approvals, scope. `branching` — branch and save. `testing` — test mechanics and runs.
  `change-log-and-reapply` — recording and replaying the change. `trace-investigation` — why a version was picked.
