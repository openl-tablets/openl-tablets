# Lookup Tables API

The API reads a SmartLookup or a SimpleLookup table as a tree of column headers and rows that mirror it, replaces them,
and appends rows. The `tableType` of the table is `SmartLookup` or `SimpleLookup`; both use the same model.

All table types share the same endpoints and the same rules for the project and the table id; see
[Table Endpoints](README.md#table-endpoints).

## Reading a Table

```http
GET /rest/projects/{projectId}/tables/{tableId}
```

**Response** (`200 OK`):

```json
{
  "id": "d2bb169ab4ac556aa8b147f99a723719",
  "tableType": "SmartLookup",
  "kind": "Rules",
  "name": "CoverageRate",
  "returnType": "Double",
  "args": [
    {"name": "coverageForm", "type": "CoverageForm"},
    {"name": "numberOfEmployees", "type": "Integer"},
    {"name": "area", "type": "String"}
  ],
  "headers": [
    {"title": "Coverage Type"},
    {"title": "Limit"},
    {"title": "<= 14", "children": [{"title": "Low"}, {"title": "High"}]},
    {"title": ">= 15", "children": [{"title": "Low"}, {"title": "High"}]}
  ],
  "rows": [
    {
      "Coverage Type": "Basic",
      "Limit": 100,
      "<= 14": {"Low": 20, "High": 30},
      ">= 15": {"Low": 30, "High": 66}
    },
    {
      "Coverage Type": "Basic",
      "Limit": "[200.0..300.0]",
      "<= 14": {"Low": 30, "High": 50},
      ">= 15": {"Low": 60, "High": 88}
    }
  ]
}
```

The response leaves out the empty values: a header without children has no `children`, and a row has no key for an
empty cell.

## Headers and Rows

- **Header** — a column title. A header with `children` groups the columns of its children; a header without them is
  one column.
- **Row** — an object keyed by the title of each top-level header. The value of a header without children is the cell.
  The value of a header with children is an object keyed by the titles of the children.
- **Titles are keys** — the same title under two parents, such as `Low` under `<= 14` and under `>= 15`, is a different
  key in each nested object.
- **Cells** — a number, a boolean, or a string. A range or a condition is a string, such as `[200.0..300.0]`.

Schematically, the first row of the table above is this part of the workbook:

```
SmartLookup Double CoverageRate (CoverageForm coverageForm, Integer numberOfEmployees, String area)
Coverage Type | Limit | <= 14        | >= 15
              |       | Low  | High  | Low  | High
Basic         | 100   | 20   | 30    | 30   | 66
```

## Writing a Table

### Replace the Headers and the Rows

```http
PUT /rest/projects/{projectId}/tables/{tableId}
Content-Type: application/json
```

The body has the same fields as the response. A row needs an object for every header that has children.

### Append Rows

```http
POST /rest/projects/{projectId}/tables/{tableId}/lines
Content-Type: application/json

{
  "tableType": "SmartLookup",
  "rows": [
    {
      "Coverage Type": "Additional",
      "<= 14": {"Low": 30, "High": 50},
      ">= 15": {"Low": 60, "High": 88}
    }
  ]
}
```

The titles of the rows are matched with the headers of the table in the workbook, not with headers of the request. A
key that is not in a row leaves the cell empty.

A write answers `204 No Content`, or `200 OK` with the new table id when the table moved; see
[Data Tables API](data-tables-api.md#responses).

## Models

### LookupView

- `id`, `tableType`, `kind`, `name`, `properties` — as for a Data table. `tableType` is `SmartLookup` or
  `SimpleLookup`, and `kind` is `Rules`.
- `returnType` — the type of the value the table returns.
- `args` — the list of arguments. Each has a `name` and a `type`.
- `collect` — whether the table uses the `Collect` keyword to aggregate the matching rows.
- `headers` — the list of `LookupHeaderView`.
- `rows` — the list of rows, each an object as described above.
- `messages`, `runState`, `partial` — read-only; see [Table Endpoints](README.md#table-endpoints).

### LookupHeaderView

- `title` — the title of the column.
- `children` — the list of `LookupHeaderView` that the header groups.

### LookupAppend

- `tableType` — `SmartLookup` or `SimpleLookup`.
- `rows` — the list of rows to append.
