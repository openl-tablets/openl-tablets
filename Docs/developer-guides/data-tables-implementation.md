# Data Tables API Implementation Guide

## Overview

This document describes the implementation of Data table support in the OpenL Studio Projects API. Data tables are now fully editable through REST API endpoints for getting, updating, and appending rows.

---

## Architecture

The implementation follows the existing architecture pattern used for other table types (Datatype, Rules, Spreadsheet, etc.).

### Component Structure

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                           │
│              ProjectsController.java                        │
│  (Existing endpoints for /projects/{id}/tables)            │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                    Service Layer                            │
│         WorkspaceProjectService.java                        │
│  - getTable()         -> DataTableReader                   │
│  - updateTable()      -> DataTableWriter                   │
│  - appendTableLines() -> DataTableWriter.append()          │
└──────────────────────────┬──────────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
┌────────▼─────┐  ┌─────────▼──────┐  ┌──────▼──────┐
│ DataTableReader   │ DataTableWriter │ OpenLTableUtils │
│ - supports()  │  │ - write()    │  │ - isDataTable() │
│ - read()      │  │ - append()   │  │                 │
└────────┬─────┘  └────────┬──────┘  └─────────────────┘
         │                │
         └────────┬───────┘
                  │
         ┌────────▼────────┐
         │  Model Classes  │
         ├─────────────────┤
         │ - DataView      │
         │ - DataAppend    │
         │ - DataRowView   │
         └─────────────────┘
```

---

## New Classes

### 1. Model Classes

#### `DataRowView.java`
Represents a single row in a Data table.

```java
public class DataRowView {
    public final Collection<Object> values;
    // Builder pattern for JSON deserialization
}
```

**Key Features**:
- Flexible value storage (supports any object type)
- Builder pattern for Jackson deserialization
- Immutable design

#### `DataView.java`
Represents a complete Data table for both reading and updating.

```java
public class DataView extends TableView implements EditableTableView {
    public static final String TABLE_TYPE = "Data";
    public final Collection<String> columns;
    public final Collection<DataRowView> rows;
}
```

**Key Features**:
- Extends `TableView` base class for common properties
- Implements `EditableTableView` for polymorphic JSON deserialization
- Stores column names and data rows
- Includes builder for JSON deserialization

#### `DataAppend.java`
Request model for appending rows to Data tables.

```java
public class DataAppend implements AppendTableView {
    public static final String TABLE_TYPE = DataView.TABLE_TYPE;
    public final Collection<DataRowView> rows;
}
```

**Key Features**:
- Implements `AppendTableView` for polymorphic deserialization
- Contains only new rows to append (non-destructive)
- Follows same pattern as `DatatypeAppend`, `SimpleRulesAppend`, etc.

### 2. Reader Implementation

#### `DataTableReader.java`
Reads OpenL Data tables and converts them to `DataView` objects.

```java
@Component
public class DataTableReader extends EditableTableReader<DataView, DataView.Builder> {
    @Override
    protected void initialize(DataView.Builder builder, IOpenLTable openLTable) {
        // Extract columns and rows from table body
    }

    @Override
    public boolean supports(IOpenLTable table) {
        return OpenLTableUtils.isDataTable(table);
    }
}
```

**Key Methods**:
- `supports()`: Returns true only for Data tables (XLS_DATA type)
- `initialize()`: Extracts table structure
  - `readColumns()`: Reads first row as column headers
  - `readRows()`: Reads data rows (starting from row 1)
- `getCellValue()`: Inherited from parent - handles type conversion

**Design Decisions**:
- Inherits from `EditableTableReader` to follow existing pattern
- First row treated as column headers (typical for Data tables)
- Data rows start from row 1
- All cell values converted using inherited utility methods

### 3. Writer Implementation

#### `DataTableWriter.java`
Writes `DataView` objects back to OpenL Data tables.

```java
public class DataTableWriter extends TableWriter<DataView> {
    @Override
    protected void updateHeader(DataView tableView) {
        // Update main header and column headers
    }

    @Override
    protected void updateBusinessBody(DataView tableView) {
        // Replace all data rows
    }

    public void append(DataAppend dataAppend) {
        // Add new rows without replacing existing data
    }
}
```

**Key Methods**:
- `updateHeader()`: Updates table name in main header + column names
- `updateColumnHeaders()`: Updates column headers in row 0
- `updateBusinessBody()`: Replaces all data rows
- `writeRow()`: Writes single row at specified position
- `append()`: Appends new rows to end of table

**Special Features**:
- Automatic cleanup of removed rows when table shrinks
- Automatic cleanup of removed columns when table shrinks
- Transaction-like behavior with edit/stopEditing

**Implementation Details**:
```java
// Edit transaction pattern
try {
    table.getGridTable().edit();
    // Perform modifications
    save();
} finally {
    table.getGridTable().stopEditing();
}
```

---

## Integration with Service Layer

### WorkspaceProjectService Updates

#### 1. Import Additions
```java
import org.openl.studio.projects.model.tables.DataAppend;
import org.openl.studio.projects.model.tables.DataView;
import org.openl.studio.projects.service.tables.write.DataTableWriter;
```

#### 2. `updateTable()` Method Enhancement
Added handling for `DataTableWriter`:
```java
if (writer instanceof DataTableWriter) {
    ((DataTableWriter) writer).write((DataView) tableView);
}
```

#### 3. `getTableWriter()` Method Enhancement
Added Data table routing:
```java
else if (Objects.equals(XlsNodeTypes.XLS_DATA.toString(), table.getType())) {
    if (DataView.TABLE_TYPE.equals(tableType)) {
        return new DataTableWriter(table);
    }
}
```

#### 4. `appendTableLines()` Method Enhancement
Added support for Data table append:
```java
else if (writer instanceof DataTableWriter) {
    ((DataTableWriter) writer).append((DataAppend) tableView);
}
```

### Registration with Polymorphic Deserialization

#### EditableTableView
```java
@JsonSubTypes({
    // ... existing types ...
    @JsonSubTypes.Type(value = DataView.class, name = DataView.TABLE_TYPE)
})
```

#### AppendTableView
```java
@JsonSubTypes({
    // ... existing types ...
    @JsonSubTypes.Type(value = DataAppend.class, name = DataView.TABLE_TYPE)
})
```

### Utility Methods

#### OpenLTableUtils
Added utility method for Data table detection:
```java
public static boolean isDataTable(IOpenLTable table) {
    return XlsNodeTypes.getEnumByValue(table.getType()) == XlsNodeTypes.XLS_DATA;
}
```

---

## Request/Response Flow

### Get Data Table Flow

```
HTTP GET /projects/{id}/tables/{tableId}
    ↓
ProjectsController.getTable()
    ↓
WorkspaceProjectService.getTable()
    ↓
readers.stream().filter(r -> r.supports(table)).findFirst()
    ↓
DataTableReader.read(IOpenLTable)
    ↓
Extract columns and rows from table body
    ↓
Return DataView object
    ↓
Jackson serializes to JSON
    ↓
HTTP 200 OK + JSON response
```

### Update Data Table Flow

```
HTTP PUT /projects/{id}/tables/{tableId}
    ↓
Jackson deserializes JSON to EditableTableView (polymorphic)
    ↓
ProjectsController.updateTable(EditableTableView)
    ↓
WorkspaceProjectService.updateTable()
    ↓
Permission check (BasePermission.WRITE)
    ↓
getTableWriter(table, tableView.getTableType())
    ↓
DataTableWriter instance created
    ↓
DataTableWriter.write(DataView)
    ├─ updateHeader() - Update table name
    ├─ updateColumnHeaders() - Update column headers (row 0)
    ├─ updateBusinessBody() - Replace data rows
    └─ Cleanup removed rows/columns
    ↓
Project.getCurrentProject().tryLockOrThrow()
    ↓
HTTP 204 No Content
```

### Append Rows Flow

```
HTTP POST /projects/{id}/tables/{tableId}/lines
    ↓
Jackson deserializes JSON to AppendTableView (polymorphic)
    ↓
ProjectsController.appendTableLines(AppendTableView)
    ↓
WorkspaceProjectService.appendTableLines()
    ↓
Permission check (BasePermission.WRITE)
    ↓
getTableWriter(table, tableView.getTableType())
    ↓
DataTableWriter instance created
    ↓
DataTableWriter.append(DataAppend)
    ├─ table.getGridTable().edit()
    ├─ Calculate next row position
    ├─ Write each new row
    ├─ save()
    └─ table.getGridTable().stopEditing()
    ↓
HTTP 204 No Content
```

---

## Key Design Patterns

### 1. Template Method Pattern
- `TableWriter<T>` defines template
- `DataTableWriter` implements `updateHeader()` and `updateBusinessBody()`
- Common operations (save, edit, stopEditing) handled by base class

### 2. Strategy Pattern
- Multiple reader implementations (`DataTableReader`, `DatatypeTableReader`, etc.)
- Selected at runtime based on `supports()` method
- Flexible addition of new table types

### 3. Builder Pattern
- `DataView.Builder` for immutable object construction
- JSON deserialization via Jackson
- Fluent API for programmatic use

### 4. Type Token Pattern
- `EditableTableView.TABLE_TYPE = "Data"` as discriminator
- Jackson uses `@JsonTypeInfo` with `tableType` property
- Polymorphic deserialization of request bodies

---

## Excel Structure

Data tables in Excel follow this structure:

```
Row 0 (Header):  | Data Customers                      |
Row 1 (Columns): | CustomerID | Name  | Email           | Country |
Row 2 (Data):    | 1          | John  | john@example    | USA     |
Row 3 (Data):    | 2          | Jane  | jane@example    | UK      |
Row 4 (Data):    | 3          | Bob   | bob@example     | Canada  |
...
```

**Key Points**:
- Row 0: Table header with name
- Row 1: Column headers
- Rows 2+: Data rows
- `DataTableReader` skips row 0 in `readColumns()` and starts from row 1
- `DataTableWriter` handles row 0 (name) and row 1 (columns) separately

---

## Type Conversion

Cell values are handled using inherited `getCellValue()` method from `EditableTableReader`:

```java
protected Object getCellValue(ICell cell, MetaInfoReader metaInfoReader) {
    // Extract formatted or object value
    // Handle type conversions (Date, etc.)
    // Return Java object
}
```

**Supported Types**:
- String
- Number (Integer, Double, BigDecimal)
- Date (ISO 8601 in JSON)
- Boolean
- Null (for empty cells)

---

## Error Handling

### Reader Errors
- `BadRequestException`: Table type not supported
- `NotFoundException`: Table not found
- Table compilation errors handled by ProjectModel

### Writer Errors
- `UnsupportedOperationException`: Table type/tableType mismatch
- `ForbiddenException`: No WRITE permission
- `ConflictException`: Project locked by another user
- Excel cell write errors propagated

---

## Testing Considerations

### Unit Tests
- `DataTableReader`: Parse various table structures
- `DataTableWriter`: Write and update operations
- `OpenLTableUtils.isDataTable()`: Type detection

### Integration Tests
- Full CRUD cycle (Get → Update → Append)
- Permission enforcement
- Concurrent access handling
- Large table handling
- Type conversion edge cases

### API Tests
- REST endpoint behavior
- JSON serialization/deserialization
- Error response codes
- Authorization enforcement

---

## Future Enhancements

1. **Pagination**: Implement row pagination for large tables
2. **Filtering**: Support filtering rows in GET requests
3. **Validation**: Server-side row validation before write
4. **Batch Operations**: Transactional batch updates
5. **Change Tracking**: Track who modified what and when
6. **Row Locking**: Pessimistic locking for concurrent updates

---

## Related Files

**Model Classes**:
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/model/tables/DataView.java`
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/model/tables/DataAppend.java`
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/model/tables/DataRowView.java`
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/model/tables/EditableTableView.java`
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/model/tables/AppendTableView.java`

**Reader/Writer Classes**:
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/service/tables/read/DataTableReader.java`
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/service/tables/write/DataTableWriter.java`

**Service Integration**:
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/service/WorkspaceProjectService.java`
- `/STUDIO/org.openl.rules.webstudio/src/org/openl/studio/projects/service/tables/OpenLTableUtils.java`

**API Documentation**:
- `/Docs/api/data-tables-api.md` - REST API guide with examples

---

## Consistency with Existing Implementation

This implementation follows the exact patterns used for other table types:

| Aspect | Pattern | Applied To |
|--------|---------|-----------|
| **Models** | `extends TableView implements EditableTableView` | DataView like DatatypeView |
| **Readers** | `extends EditableTableReader<View, Builder>` | DataTableReader like DatatypeTableReader |
| **Writers** | `extends TableWriter<View>` with edit/stopEditing | DataTableWriter like DatatypeTableWriter |
| **Registration** | Added to `@JsonSubTypes` | EditableTableView, AppendTableView |
| **Service** | instanceof checks in update/append methods | Consistent with other types |
| **Utilities** | Boolean `isXxxTable()` method | isDataTable() in OpenLTableUtils |

This ensures maintainability and consistency across the codebase.
