# Test Tables API

The API reads a Test table as column headers and test cases, replaces them, and appends test cases. The `tableType` of
the table is `Test`. A Test table has the same headers, rows, and writing rules as a Data table; the header holds the
name of the tested table instead of a data type.

All table types share the same endpoints and the same rules for the project and the table id; see
[Table Endpoints](README.md#table-endpoints). The columns, the rows, the responses, and the errors of a write are
described in [Data Tables API](data-tables-api.md).

## Reading a Table

```http
GET /rest/projects/{projectId}/tables/{tableId}
```

**Response** (`200 OK`):

```json
{
  "id": "5d64e98cfb043d16d9aedc659e4b2674",
  "tableType": "Test",
  "kind": "Test",
  "name": "CoverageRateTest",
  "testedTableName": "CoverageRate",
  "headers": [
    {"fieldName": "coverageForm.", "foreignKey": "CoverageFormData", "displayName": "Coverage Form Data"},
    {"fieldName": "numberOfEmployees", "displayName": "Number Of Employees"},
    {"fieldName": "area", "displayName": "Area"},
    {"fieldName": "_res_", "displayName": "Expected Premium"}
  ],
  "rows": [
    {"values": [1, 7, "High", 301]},
    {"values": [2, 125, "Low", 602]}
  ]
}
```

The response leaves out the empty values: a column without a foreign key has no `foreignKey`.

## Layout of the Table

```
Test CoverageRate CoverageRateTest
coverageForm.       numberOfEmployees     area     _res_
>CoverageFormData
Coverage Form Data  Number Of Employees   Area     Expected Premium
1                   7                     High     301
```

- **Header** — `Test <testedTableName> <name>`. `testedTableName` is the second word of the header, and the `name`
  is the third word, which may be absent.
- **First row** — the field names: the input fields of the tested table and `_res_` for the expected result.
- **Foreign keys** — a second row, which exists only when a cell of the row starts with `>`.
- **Display names** — the row under the field names, or under the foreign keys.
- **Test cases** — all the following rows.

## Writing a Table

### Replace the Headers and the Test Cases

```http
PUT /rest/projects/{projectId}/tables/{tableId}
Content-Type: application/json

{
  "tableType": "Test",
  "name": "CoverageRateTest",
  "testedTableName": "CoverageRate",
  "headers": [
    {"fieldName": "numberOfEmployees", "displayName": "Number Of Employees"},
    {"fieldName": "_res_", "displayName": "Expected Premium"}
  ],
  "rows": [
    {"values": [7, 301]},
    {"values": [125, 602]}
  ]
}
```

### Append Test Cases

```http
POST /rest/projects/{projectId}/tables/{tableId}/lines
Content-Type: application/json

{
  "tableType": "Test",
  "rows": [
    {"values": [250, 801]}
  ]
}
```

A write answers `204 No Content`, or `200 OK` with the new table id when the table moved, and refuses a request without
a column title or with a row wider than the table; see [Data Tables API](data-tables-api.md#responses).

## Models

### TestView

- `id`, `tableType`, `kind`, `name`, `properties` — as for a Data table. `tableType` and `kind` are `Test`.
- `testedTableName` — the name of the table the test cases run.
- `headers` — the list of `DataHeaderView`.
- `rows` — the list of `DataRowView`, one for each test case.
- `messages`, `runState`, `partial` — read-only; see [Table Endpoints](README.md#table-endpoints).

### TestAppend

- `tableType` — `Test`.
- `rows` — the list of `DataRowView` to append.

`DataHeaderView` and `DataRowView` are described in [Data Tables API](data-tables-api.md#models).
