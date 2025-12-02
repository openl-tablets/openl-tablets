# Raw Tables API

## Overview

The Raw Tables API provides a way to read any table in its raw format as a 2D matrix of cells with merge information. Unlike the parsed APIs (Data Tables, Test Tables), the raw format does not parse table headers, determine data types, or validate structure - it simply returns the table as-is with cell merge metadata.

**Use Cases:**
- Exporting tables in their original Excel structure
- Reading tables of unknown or custom types
- Programmatic access to cell-level data
- Preserving exact cell formatting and merge information

## Quick Start

### Get Table in Raw Format

```bash
# Get raw format (2D matrix with merge information)
GET /projects/MyProject/tables/DATA_Bank?raw=true

# Get parsed format (default, existing behavior)
GET /projects/MyProject/tables/DATA_Bank
GET /projects/MyProject/tables/DATA_Bank?raw=false
```

### Example Response (Raw Format)

```json
{
  "id": "DATA_Bank",
  "tableType": "RawSource",
  "kind": "Bank data storage",
  "name": "Bank",
  "source": [
    [
      {"value": "bankID"},
      {"value": "bankRatings", "colspan": 2},
      {"covered": true}
    ],
    [
      {"value": "bank ID"},
      {"value": "currentData 1"},
      {"value": "currentData 2"}
    ]
  ]
}
```

## API Endpoints

### Get Table (Parsed or Raw)

```
GET /projects/{projectId}/tables/{tableId}[?raw=true|false]
```

**Parameters:**
- `projectId` (path, required) - Project identifier
- `tableId` (path, required) - Table identifier
- `raw` (query, optional, default: `false`) - Format flag
  - `false` - Returns parsed EditableTableView (default, existing behavior)
  - `true` - Returns raw 2D matrix as RawTableView

**Responses:**
- `raw=false` (default) → `EditableTableView` (DataView, TestView, etc.)
- `raw=true` → `RawTableView` (2D matrix format)

**Status Codes:**
- `200 OK` - Table retrieved successfully
- `404 Not Found` - Table or project not found
- `409 Conflict` - Project not opened

## Data Models

### EditableTableView and AppendTableView Interfaces

These interfaces define the contract for table models used in create/update and append operations respectively. Both have been updated with comprehensive OpenAPI schema annotations for improved API documentation visibility.

- **EditableTableView** - Marker interface for update/create operations with polymorphic deserialization support via `tableType` discriminator.
- **AppendTableView** - Marker interface for append operations with polymorphic deserialization support via `tableType` discriminator.

All implementations (DatatypeView, DatatypeAppend, etc.) now include detailed `@Schema` descriptions for their fields, improving OpenAPI documentation in Swagger UI.

### RawTableView

Represents the entire table in raw 2D matrix format as a structured view. Extends `TableView` and implements `EditableTableView`.

**Fields with OpenAPI Annotations:**
- `id` (String) - Table identifier (e.g., "DATA_Bank", inherited from TableView). Unique identifier of the table.
- `tableType` (String) - Always "RawSource" (inherited from TableView, identifies the raw format)
- `kind` (String) - Table kind/description from table properties (inherited from TableView). Allowed values include: Rules, Spreadsheet, Datatype, Data, Test, TBasic, Column Match, Method, Run, Constants, Conditions, Actions, Returns, Environment, Properties, Other.
- `name` (String) - Table name (inherited from TableView)
- `source` (List<List<RawTableCell>>) - 2D matrix of raw table cells with merge information

**Note:** Unlike the parsed APIs which separate headers from data rows, the `source` matrix includes all rows starting from row 0.

### RawTableCell

Represents a single cell in raw format with explicit span information.

**Fields with OpenAPI Annotations:**
- `value` (Object) - Cell value (null if covered by another cell's span)
- `colspan` (Integer) - Number of columns this cell spans (null if single column or covered)
- `rowspan` (Integer) - Number of rows this cell spans (null if single row or covered)
- `covered` (Boolean) - True if cell is masked by another cell's span

**Span Information:**

Span information is extracted from the underlying TableModel's rowspan/colspan. Cells are categorized as follows:

- **Origin Cell** (top-left of merged region):
  - `value` - contains the actual cell value
  - `colspan` - integer >= 2 (null if single column, only present if colspan > 1)
  - `rowspan` - integer >= 2 (null if single row, only present if rowspan > 1)
  - `covered` - null (not a covered cell)

- **Covered Cells** (part of merged region but not origin):
  - `value` - null (the value is in the origin cell)
  - `colspan` - null (cell is covered by another cell's span)
  - `rowspan` - null (cell is covered by another cell's span)
  - `covered` - true (explicitly marks this as a masked cell)

**Example:** If cell (0,0) is merged to span 2 columns and 2 rows:
- Cell (0,0): `{value: "Header", colspan: 2, rowspan: 2}` (origin cell spans 2x2)
- Cell (0,1): `{covered: true}` (masked by horizontal span)
- Cell (1,0): `{covered: true}` (masked by vertical span)
- Cell (1,1): `{covered: true}` (masked by both spans)

**Note:**
- Fields with `null` values are excluded from JSON response due to `@JsonInclude(NON_NULL)` annotation
- Covered cells appear in the matrix only with `{covered: true}`
- Clients can detect merged regions by checking: `colspan > 1 || rowspan > 1`
- Covered cells should be skipped when processing the matrix

### JSON Example

```json
{
  "id": "DATA_Bank",
  "tableType": "RawSource",
  "kind": "Bank data storage",
  "name": "Bank",
  "source": [
    [
      {
        "value": "Header1"
      },
      {
        "value": "Header2",
        "colspan": 2
      },
      {
        "value": "Header3"
      }
    ],
    [
      {
        "value": "SubHeader"
      },
      {
        "value": "Left",
        "rowspan": 2
      },
      {
        "value": "Right"
      },
      {
        "value": "SubHeader3"
      }
    ],
    [
      {
        "value": "Value1"
      },
      {
        "covered": true
      },
      {
        "value": "Value2"
      },
      {
        "value": "Value3"
      }
    ]
  ]
}
```

## Implementation Details

### Architecture

The raw table reading leverages OpenL's existing TableModel from TableEditor component:

```
RawTableReader (Component)
  ↓
  Uses TableModel.initializeTableModel(IGridTable)
  ↓
  Extracts cell data from TableModel (including rowspan/colspan)
  ↓
  Converts to RawTableView (2D matrix with merge metadata)
    ↓
    ProjectsController.getTable(?raw=true)
    ↓
    WorkspaceProjectService.getTableRaw(IOpenLTable)
```

### Why TableModel?

TableModel from TableEditor component provides:
- **Proper cell merging handling** - rowspan/colspan already calculated
- **Consistent with UI rendering** - same code path as HTML table editor
- **All table types supported** - works with Data, Test, Spreadsheet, etc.
- **Correct dimensions** - handles empty rows/columns properly
- **No duplication** - reuses existing OpenL infrastructure

### Files

**Models:**
- `RawTableCell.java` - Single cell representation with explicit colspan/rowspan and covered flag
- `RawTableView.java` - 2D matrix container extending TableView

**Readers:**
- `RawTableReader.java` - Converts IOpenLTable via TableModel to raw format
  - Method: `initialize()` - Reads table as 2D matrix with merge information
  - Handles cell merging with colspan/rowspan extraction
  - Works with any table type

**Writers:**
- `RawTableWriter.java` - Writes RawTableView back to the original table
  - Method: `updateHeader()` - No special processing (header is part of source matrix)
  - Method: `updateBusinessBody()` - Two-phase write with merge support
    - Phase 1: Iterates all rows/cells, writes values, skips covered cells, tracks merge regions
    - Phase 2: Applies all merge regions using MergeCellsAction
    - Cleans up extra rows after all writes
  - Method: `append(RawTableAppend)` - Appends rows with merge support
    - Gets current table height as starting position
    - Phase 1: Writes rows, skips covered cells, tracks merges
    - Phase 2: Applies merge regions
  - Method: `applyMergeRegions()` - Internal helper to apply tracked merges
    - Creates MergeCellsAction for each tracked region
    - Executes as UndoableCompositeAction
    - Ensures undo/redo support
  - Works with any table type
  - Cleans up removed rows automatically (update only)

**Append Models:**
- **RawTableAppend** - Request model for appending rows to raw tables
  - Implements AppendTableView interface
  - Field: `rows` (List<List<RawTableCell>>) - Rows to append as a 2D matrix of raw table cells
  - Supports polymorphic JSON deserialization via tableType

**Service Integration:**
- `WorkspaceProjectService.getTableRaw()` - Service method to retrieve raw table
- `WorkspaceProjectService.updateTable()` - Handles RawTableView updates through RawTableWriter
- `WorkspaceProjectService.appendTableLines()` - Appends rows using RawTableAppend
- `WorkspaceProjectService.getTableWriter()` - Factory method supporting RawTableView.TABLE_TYPE
- `ProjectsController.getTable()` - REST endpoint with `?raw` flag support (GET)
- `ProjectsController.updateTable()` - REST endpoint for updating tables (PUT, supports raw format)
- `ProjectsController.appendTableLines()` - REST endpoint for appending rows (POST, supports raw format)

### Merge Detection (Reading)

Merged cell information is automatically handled by TableModel through rowspan/colspan during read:
1. TableModel is initialized from IGridTable with proper dimension calculation
2. Each CellModel contains `getRowspan()` and `getColspan()` values
3. RawTableReader creates cells with:
   - `colspan` and `rowspan` set to their integer values (>= 1) for origin cells
   - `colspan` and `rowspan` set to `null` for covered cells (masked by another cell's span)
4. Covered cells in the matrix represent positions that are part of a merged region but not the origin

This approach ensures perfect alignment with how the table is rendered in the HTML table editor while providing explicit, semantic span information to clients.

### Merge Application (Writing)

Merge cell logic during write uses two-phase approach:

**Phase 1 - Write Values:**
- All cell values from the source matrix are written to the grid
- Covered cells (with `covered=true`) are skipped
- For cells with colspan/rowspan > 1, merge regions are tracked
- Grid may expand with new rows/columns as needed

**Phase 2 - Apply Merges:**
- After all cell values are written, tracked merge regions are applied
- Each merge region creates a MergeCellsAction
- All actions are combined into UndoableCompositeAction
- Actions are executed after grid dimensions are finalized

**Why Two-Phase?**
- Writing before merging ensures grid has all required rows/columns
- Merging before completing all writes could fail if grid expansion is needed
- Merge regions can be calculated during cell iteration without separate pass
- Preserves undo/redo information through UndoableCompositeAction

**MergeCellsAction Behavior:**
- Automatically removes any existing merges that overlap with new merge region
- Adds new merged region to the grid
- Supports undo operation to restore previous merge state

### Dimension Calculation

TableModel handles dimension calculation internally:
- **Height:** Obtained from `TableModel.getHeight()`
- **Width:** Determined from `TableModel.getCells()[0].length`

TableModel automatically excludes empty rows and columns, ensuring the matrix only includes relevant content without trailing empty rows/columns.

### Key Methods

**RawTableReader:**
- `initialize()` - Reads table as raw 2D matrix with merge information, using TableModel to properly handle cell merging, content extraction, and dimensions.

**RawTableWriter:**
- `updateBusinessBody()` - Writes entire source matrix in two phases: Phase 1 writes cell values and tracks merges, Phase 2 applies merge regions.
- `append()` - Appends new rows to end of table with merge support, skips covered cells.
- `applyMergeRegions()` - Internal helper to apply tracked merge regions using MergeCellsAction.

**WorkspaceProjectService:**
- `getTableRaw()` - Retrieves table in raw format as 2D matrix with merge information
- `updateTable()` - Updates table with new content (supports any EditableTableView including RawTableView)

## Usage Patterns

### Reading Raw Table Data

```bash
# Request
curl -X GET "http://localhost:8080/projects/MyProject/tables/DATA_Bank?raw=true" \
  -H "Accept: application/json" \
  -u <username>:<password>

# Response
{
  "id": "DATA_Bank",
  "tableType": "RawSource",
  "kind": "Bank information",
  "source": [[...cells...]]
}
```

### Writing Raw Table Data

Raw tables can be updated by sending the modified RawTableView back to the server:

```bash
# Request: Update table with raw format
curl -X PUT "http://localhost:8080/projects/MyProject/tables/DATA_Bank" \
  -H "Content-Type: application/json" \
  -u <username>:<password> \
  -d '{
    "id": "DATA_Bank",
    "tableType": "RawSource",
    "kind": "Bank information",
    "source": [
      [{"value": "BankID"}, {"value": "Rating"}],
      [{"value": "BANK1"}, {"value": "A"}],
      [{"value": "BANK2"}, {"value": "B"}]
    ]
  }'

# Response: 204 No Content (success)
```

**Write Processing:**
- RawTableWriter iterates through the source matrix row by row, cell by cell
- Writes each non-covered cell value directly to the table grid
- Covered cells (marked with `covered: true`) are automatically skipped
- Merge information (colspan/rowspan) is preserved implicitly through the matrix structure
- Any rows in the original table beyond the source matrix size are automatically removed
- No type validation or interpretation - raw values written as-is

**Key Characteristics:**
- Works with any table type (Data, Test, Spreadsheet, etc.)
- Preserves exact cell positioning and merge regions
- Treats entire source matrix uniformly (including headers)
- No schema interpretation or type conversion
- Merge regions are applied after all values are written (two-phase)

**Example: Writing with merged cells**

```json
{
  "id": "DATA_Merged",
  "tableType": "RawSource",
  "source": [
    [
      {"value": "Header1", "colspan": 2},
      {"value": "Header3"}
    ],
    [
      {"value": "Header1.1"},
      {"covered": true},
      {"value": "Header3.1"}
    ],
    [
      {"value": "Data1", "rowspan": 2},
      {"value": "Data2"},
      {"value": "Data3"}
    ],
    [
      {"covered": true},
      {"value": "Data2.2"},
      {"value": "Data3.2"}
    ]
  ]
}
```

In this example:
- Row 0, Col 0: "Header1" spans 2 columns (merges into row 0, cols 0-1)
- Row 1, Col 1: Covered by Header1's colspan
- Row 2, Col 0: "Data1" spans 2 rows (merges rows 2-3, col 0)
- Row 3, Col 0: Covered by Data1's rowspan

### Appending Rows to Raw Tables

Raw tables can be extended by appending new rows to the end without modifying existing content:

```bash
# Request: Append rows to table
curl -X POST "http://localhost:8080/projects/MyProject/tables/DATA_Bank/append" \
  -H "Content-Type: application/json" \
  -u <username>:<password> \
  -d '{
    "tableType": "RawSource",
    "rows": [
      [{"value": "BANK3"}, {"value": "C"}],
      [{"value": "BANK4"}, {"value": "D"}]
    ]
  }'

# Response: 204 No Content (success)
```

**Append Behavior:**
- New rows are added to the end of the existing table
- Covered cells (with `covered: true`) are skipped
- Existing table content is not affected
- Works with any table type
- Each row in the append request is a list of RawTableCell objects

**Example: Building append request programmatically**

```javascript
const appendRows = async (tableId, newRows) => {
  const appendRequest = {
    tableType: "RawSource",
    rows: newRows  // Array of row arrays (List<List<RawTableCell>>)
  };

  const response = await fetch(`/projects/MyProject/tables/${tableId}/append`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(appendRequest)
  });
  return response.status === 204;
};

// Usage
const newRows = [
  [
    {value: "NewBank1"},
    {value: "Rating1"}
  ],
  [
    {value: "NewBank2"},
    {value: "Rating2"}
  ]
];

const success = await appendRows("DATA_Bank", newRows);
```

### Read-Write Cycle

A complete example of reading a table, modifying it, and writing it back:

```javascript
// 1. Read raw table
const getRawTable = async () => {
  const response = await fetch('/projects/MyProject/tables/DATA_Bank?raw=true');
  return response.json();
};

// 2. Modify the table
const modifyTable = (rawTable) => {
  // Modify cell values
  rawTable.source[1][0].value = "New Value";

  // Covered cells are skipped, so just modify origin cells
  if (rawTable.source[1][1].colspan > 1) {
    // This is a merged cell
    rawTable.source[1][1].value = "Merged";
  }

  return rawTable;
};

// 3. Write back the modified table
const updateRawTable = async (rawTable) => {
  const response = await fetch('/projects/MyProject/tables/DATA_Bank', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(rawTable)
  });
  return response.status === 204;
};

// Full cycle
const table = await getRawTable();
const modified = modifyTable(table);
const success = await updateRawTable(modified);
```

**Key Points:**
- Raw table can be read, modified, and written back without loss of structure
- Covered cells are preserved in the matrix but skipped during writes
- Merge information is maintained through colspan/rowspan fields
- No type conversion or validation occurs

### Comparing Parsed vs Raw Formats

**Parsed Format (default):**
```bash
GET /projects/MyProject/tables/DATA_Bank
# Returns: DataView {id, name, kind, headers[], rows[]}
```

**Raw Format:**
```bash
GET /projects/MyProject/tables/DATA_Bank?raw=true
# Returns: RawTableView {id, tableType, kind, source[][]}
```

### REST API Operations Summary

Raw Tables API supports three main operations via REST endpoints:

| Operation | HTTP Method | Endpoint | Request Body | Response |
|-----------|------------|----------|--------------|----------|
| **Read** | GET | `/projects/{projectId}/tables/{tableId}?raw=true` | N/A | RawTableView (200) |
| **Update** | PUT | `/projects/{projectId}/tables/{tableId}` | RawTableView | 204 No Content |
| **Append** | POST | `/projects/{projectId}/tables/{tableId}/append` | RawTableAppend | 204 No Content |

**Example: Complete CRUD cycle with fetch API:**

```javascript
const API_BASE = 'http://localhost:8080/projects/MyProject/tables';

// Create (actually update with full structure)
const create = async (tableId, rawTable) => {
  const response = await fetch(`${API_BASE}/${tableId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(rawTable)
  });
  return response.status === 204;
};

// Read
const read = async (tableId) => {
  const response = await fetch(`${API_BASE}/${tableId}?raw=true`);
  return response.json();
};

// Update (same as create)
const update = async (tableId, rawTable) => {
  return create(tableId, rawTable);
};

// Append
const append = async (tableId, newRows) => {
  const response = await fetch(`${API_BASE}/${tableId}/append`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      tableType: 'RawSource',
      rows: newRows
    })
  });
  return response.status === 204;
};

// Usage example
const table = await read('DATA_Bank');
// Modify table...
await update('DATA_Bank', table);
await append('DATA_Bank', [[{value: 'NewRow'}]]);
```

### Matrix Navigation

```javascript
// Access cell at row 2, column 3
const cell = rawTableView.source[2][3];

// Check if cell is covered by another cell's span
if (cell.covered) {
  // Cell is masked by another cell's span
  // Skip this cell - the actual value is in the origin cell
  continue;
}

// Check if cell is an origin cell with spanning
if (cell.colspan > 1 || cell.rowspan > 1) {
  // Cell is origin of merged region
  // cell.value contains the actual value for this merged region
  // When processing row-by-row:
  //   - Skip next (colspan - 1) columns in current and following rows
  //   - Process continues after the spanned region
}

// Process cell normally (single cell with no spanning)
if (!cell.covered) {
  // Cell is a normal single cell or origin of merge
  // cell.value contains the value
  const value = cell.value;
}

// Example: Iterating and skipping covered cells
for (let row = 0; row < matrix.length; row++) {
  for (let col = 0; col < matrix[row].length; col++) {
    const cell = matrix[row][col];
    if (cell.covered) {
      // Skip covered cells
      continue;
    }
    // Process origin cell or single cell
    console.log(`Cell at (${row},${col}): ${cell.value}`);
  }
}
```

## API Differences from Parsed APIs

| Aspect | Parsed (DataView/TestView) | Raw (RawTableView) |
|--------|---------------------------|-------------------|
| **Base Class** | AbstractDataView | TableView (EditableTableView) |
| **Table Type** | "Data" / "Test" | "RawSource" |
| **Structure** | Headers + Rows collections | 2D Matrix (source) |
| **Headers** | Parsed list as field | Part of source matrix (row 0+) |
| **Data Access** | Type-safe field objects | Raw cell values (Object type) |
| **Type Info** | Field types extracted | Not included |
| **Validation** | Validated against schema | No validation applied |
| **Cell Spanning** | Not exposed | Explicit colspan/rowspan |
| **Covered Cells** | N/A | Marked with covered=true flag |
| **Use Case** | Type-safe data access | Export/Import/Raw access |

## Testing Scenarios

### Test 1: Simple Table
```bash
curl -X GET "http://localhost:8080/projects/MyProject/tables/DATA_Simple?raw=true"
# Expected: Matrix with simple cells, no merges
```

### Test 2: Table with Merged Cells
```bash
curl -X GET "http://localhost:8080/projects/MyProject/tables/DATA_Complex?raw=true"
# Expected: Matrix with merge metadata in RawTableCell
```

### Test 3: Format Validation
```bash
# Verify field types
curl -X GET "http://localhost:8080/projects/MyProject/tables/DATA_Bank?raw=false"
# Returns: DataView with typed fields

curl -X GET "http://localhost:8080/projects/MyProject/tables/DATA_Bank?raw=true"
# Returns: RawTableView with all cells as Object (no type info)
```

## Limitations

1. **Cell Formatting** - Cell styling, colors, fonts, borders not included (future enhancement).
2. **Formula Preservation** - Formulas are evaluated to values (OpenL behavior, not source formulas).
3. **Cell Comments** - Cell comments/notes not included (future enhancement).

## Current Capabilities

- ✅ Read tables in raw 2D matrix format
- ✅ Write/update tables in raw format
- ✅ Preserve cell merging (colspan/rowspan)
- ✅ Works with any table type
- ✅ Full matrix control without type interpretation

## Future Enhancements

- [ ] Add cell formatting/styling information (colors, fonts, borders)
- [ ] Support for cell comments/annotations
- [ ] Preserve source formulas (not just evaluated values)
- [ ] Batch export/import multiple tables in raw format
- [ ] Custom cell data type handling
- [ ] Merge information in write validation

## OpenAPI Schema Documentation

All table model classes have been updated with comprehensive OpenAPI schema annotations to provide detailed field documentation in Swagger UI and API documentation:

### Base Classes with OpenAPI Annotations:
- **TableView** - Base class for all table views with fields: `id`, `tableType`, `kind`, `name`, `properties`
- **ExecutableView** - Base class for executable tables with fields: `returnType`, `args`

### EditableTableView Implementations with OpenAPI Annotations:
- **DatatypeView** - Datatype table with fields: `extendz`, `fields`
- **VocabularyView** - Vocabulary table with fields: `type`, `values`
- **SpreadsheetView** - Spreadsheet table with fields: `rows`, `columns`, `cells`
- **SimpleSpreadsheetView** - Simple spreadsheet table with fields: `steps`
- **SimpleRulesView** - Simple rules table with fields: `headers`, `rules`
- **SmartRulesView** - Smart rules table with fields: `headers`, `rules`
- **LookupView** - Lookup table with fields: `headers`, `rows`
- **DataView** - Data table with fields: `dataType`
- **TestView** - Test table with fields: `testedTableName`
- **RawTableView** - Raw table with fields: `source`

### AppendTableView Implementations with OpenAPI Annotations:
- **DatatypeAppend** - Append to datatype table with field: `fields`
- **VocabularyAppend** - Append to vocabulary table with field: `values`
- **SimpleSpreadsheetAppend** - Append to simple spreadsheet with field: `steps`
- **SimpleRulesAppend** - Append to simple rules table with field: `rules`
- **SmartRulesAppend** - Append to smart rules table with field: `rules`
- **LookupAppend** - Append to lookup table with fields: `tableType`, `rows`
- **DataAppend** - Append to data table with field: `rows`
- **TestAppend** - Append to test table with field: `rows`
- **RawTableAppend** - Append to raw table with field: `rows`

All fields now include descriptive OpenAPI `@Schema` annotations that appear in:
- Swagger UI at `/swagger-ui/`
- OpenAPI specification at `/v3/api-docs`
- Generated API documentation

## See Also

- [DATA_TABLES_API.md](DATA_TABLES_API.md) - Parsed Data Tables API
- [TEST_TABLES_API.md](TEST_TABLES_API.md) - Parsed Test Tables API
- [DOCS_INDEX.md](DOCS_INDEX.md) - Complete documentation index
