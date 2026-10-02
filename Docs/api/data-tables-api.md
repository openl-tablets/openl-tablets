# Data Tables API

The API reads a Data table as column headers and rows, replaces them, and appends rows. The `tableType` of the table is
`Data`.

All table types share the same endpoints and the same rules for the project and the table id; see
[Table Endpoints](README.md#table-endpoints).

## Reading a Table

```http
GET /rest/projects/{projectId}/tables/{tableId}
```

**Response** (`200 OK`):

```json
{
  "id": "572eb024e1c3f2b1b94d76740067e605",
  "tableType": "Data",
  "kind": "Data",
  "name": "CoverageFormData",
  "dataType": "CoverageForm",
  "headers": [
    {"fieldName": "_PK_", "displayName": "_PK_"},
    {"fieldName": "coverageType", "displayName": "coverageType"},
    {"fieldName": "limit", "displayName": "limit"}
  ],
  "rows": [
    {"values": [1, "Basic", 100]},
    {"values": [2, "Basic", 250]}
  ]
}
```

The response leaves out the empty values: a column without a foreign key has no `foreignKey`.

## Layout of the Table

A Data table in the workbook, with and without foreign keys:

```
Data Bank bankData                                Data QualityIndicators qualityData
bankID           bankRatings      currentData     _PK_   reportDate   lossesInThisYear
                 >bankRatingList  >bankData       key    Report Date  Losses in This Year
bank ID          Bank Ratings     Current Data    2010   ...          no
commerz          MA2, FA+, SPA    2010
```

- **Header** — `Data <dataType> <name>`. `dataType` is the second word of the header.
- **First row** — the field names (`fieldName`).
- **Foreign keys** — a second row, which exists only when a cell of the row starts with `>`. The text after the `>` is
  the `foreignKey` of the column.
- **Display names** — the row under the field names, or under the foreign keys (`displayName`).
- **Data rows** — all the following rows.

## Writing a Table

### Replace the Headers and the Rows

```http
PUT /rest/projects/{projectId}/tables/{tableId}
Content-Type: application/json

{
  "tableType": "Data",
  "name": "bankData",
  "dataType": "Bank",
  "headers": [
    {"fieldName": "bankID", "displayName": "bank ID"},
    {"fieldName": "bankRatings", "foreignKey": "bankRatingList", "displayName": "Bank Ratings"}
  ],
  "rows": [
    {"values": ["commerz", "MA2, FA+, SPA"]},
    {"values": ["deutsche", "FB+, FA+"]}
  ]
}
```

The table in the workbook takes the given columns and rows. The columns and the rows that the request leaves out are
removed.

### Append Rows

```http
POST /rest/projects/{projectId}/tables/{tableId}/lines
Content-Type: application/json

{
  "tableType": "Data",
  "rows": [
    {"values": ["dresdner", "FA+, FA"]}
  ]
}
```

A row must not have more values than the table has columns.

### Responses

- **`204 No Content`** — the table was written and keeps its id.
- **`200 OK`** — the table had no room to grow and moved. The body is `{"id": "<new id>"}`, and the `Location` header
  is the address of the table under the new id.
- **`400 Bad Request`** — the request is refused:
    - `openl.error.400.table.column.required.message` — the request has no column;
    - `openl.error.400.table.column-title.required.message` — no column has a `displayName`;
    - `openl.error.400.table.append.column.count.message` — an appended row is wider than the table.
- **`409 Conflict`** — the project is locked by another user, or is not opened.

A title row left blank would end the table and leave every row below it out, so a table without any `displayName` is
refused. One untitled column among titled ones is accepted, and a `GET` reports such a table the same way, so what is
read can be written back unchanged.

## Models

### DataView

- `id` — the table id. The server fills it in.
- `tableType` — `Data`.
- `kind` — `Data`.
- `name` — the name of the table.
- `dataType` — the type of the records.
- `properties` — the table properties, a map of names to values.
- `headers` — the list of `DataHeaderView`.
- `rows` — the list of `DataRowView`.
- `messages`, `runState`, `partial` — read-only; see [Table Endpoints](README.md#table-endpoints).

### DataHeaderView

- `fieldName` — the field of the data type the column fills.
- `foreignKey` — the Data table the column refers to. Absent for an ordinary column.
- `displayName` — the title of the column.

### DataRowView

- `values` — the values of the row in the order of the columns.

### DataAppend

- `tableType` — `Data`.
- `rows` — the list of `DataRowView` to append.

## Cell Values

A cell is returned as the value the workbook holds: a number, a boolean, or a string. A date is returned as
`YYYY-MM-DD`, and a date with a time as `YYYY-MM-DDThh:mm:ss`. An empty cell is `null`.
