# OpenL Tablets Table API Documentation

This directory contains comprehensive API documentation for all OpenL Tablets table types supported through the REST API.

## Overview

OpenL Tablets provides a complete REST API for reading, writing, and managing various table types. Each table type has its own dedicated API documentation with examples, data models, and implementation details.

---

## Table API Documentation

### 1. **Data Tables API** ([data-tables-api.md](data-tables-api.md))

Complete implementation of Data Tables API for OpenL Studio with support for complex table structures.

**Key Features:**
- GET - Retrieve complete Data table with all columns and rows
- PUT - Update entire Data table (replace columns and rows)
- POST - Append new rows to existing Data table
- Support for foreign keys and display names
- Complex field metadata (field names, foreign keys, display names)

**Use Cases:**
- Storing reference data (Bank information, Quality Indicators)
- Managing data with relational structure (foreign keys)
- Accessing data programmatically via REST API

**Example:**
```bash
GET /projects/MyProject/tables/DATA_Bank
```

```json
{
  "id": "DATA_Bank",
  "tableType": "Data",
  "dataType": "Bank",
  "name": "bankData",
  "headers": [
    {"fieldName": "bankID", "foreignKey": null, "displayName": "bank ID"},
    {"fieldName": "bankRatings", "foreignKey": "bankRatingList", "displayName": "Bank Ratings"}
  ],
  "rows": [
    {"values": ["commerz", "MA2, FA+, SPA"]},
    {"values": ["deutsche", "FB+, FA+"]}
  ]
}
```

---

### 2. **Test Tables API** ([test-tables-api.md](test-tables-api.md))

Complete implementation of Test Tables API for OpenL Studio. Test tables are similar to Data tables but used for testing other tables with test cases and assertions.

**Key Features:**
- GET - Retrieve test table with test cases
- PUT - Update test table with new test cases
- POST - Append new test cases
- Support for foreign keys
- Similar structure to Data tables with tested table reference
- Maximum code reuse through inheritance hierarchy

**Use Cases:**
- Testing rule tables with various input scenarios
- Storing test cases and expected results
- Validating rule implementations
- Regression testing

**Example:**
```bash
GET /projects/MyProject/tables/TEST_BankLimitIndex
```

```json
{
  "id": "TEST_BankLimitIndex",
  "tableType": "Test",
  "testedTableName": "BankLimitIndex",
  "name": "BankLimitIndexTest",
  "headers": [
    {"fieldName": "bank", "foreignKey": null, "displayName": "bank ID"},
    {"fieldName": "bankRatingGroup", "foreignKey": null, "displayName": "Bank Rating Group"},
    {"fieldName": "_res_", "foreignKey": null, "displayName": "Bank Limit Index"}
  ],
  "rows": [
    {"values": ["commerz", "R2", 1]}
  ]
}
```

---

### 3. **Raw Tables API** ([raw-tables-api.md](raw-tables-api.md))

The Raw Tables API provides a way to read any table in its raw format as a 2D matrix of cells with merge information.

**Key Features:**
- GET with `?raw=true` parameter - Read table as raw 2D matrix
- PUT - Update table in raw format
- POST - Append rows in raw format
- Preserves cell merging (colspan/rowspan)
- Works with any table type
- No type interpretation or validation

**Use Cases:**
- Exporting tables in their original Excel structure
- Reading tables of unknown or custom types
- Programmatic access to cell-level data
- Preserving exact cell formatting and merge information

**Example:**
```bash
GET /projects/MyProject/tables/DATA_Bank?raw=true
```

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

---

### 4. **Lookup View API** ([lookup-api.md](lookup-api.md))

Lookup tables are used to retrieve values based on dimension parameters, returning a result from a multi-dimensional array.

**Supported Types:**
- **SmartLookup**: Lookup table with multi-dimensional hierarchy support for complex column grouping structures
- **SimpleLookup**: Lookup table with simpler hierarchical structure for basic column grouping

**Key Features:**
- GET - Retrieve lookup table structure and data
- PUT - Update entire lookup table
- POST - Append new lookup rows
- Hierarchical header support (multi-level column grouping)
- Hierarchical row structure matching header hierarchy
- Support for both SmartLookup and SimpleLookup table types

**Use Cases:**
- Multi-dimensional data lookups
- Complex column grouping structures
- Pricing tables with hierarchical discounts
- Rating matrices with multiple dimensions

**Example (SmartLookup):**
```bash
GET /projects/MyProject/tables/LOOKUP_PrimaryBreedFactor
```

```json
{
  "tableType": "SmartLookup",
  "name": "PrimaryBreedFactor",
  "returnType": "Double",
  "args": [
    {"name": "species", "type": "Species"},
    {"name": "breed", "type": "Breed"}
  ],
  "headers": [
    {"title": "Species", "children": []},
    {"title": "Breed", "children": []},
    {
      "title": "AccidentOnly",
      "children": [
        {"title": "Purebred", "children": []},
        {"title": "MixedBreed", "children": []}
      ]
    }
  ],
  "rows": [
    {
      "Species": "Dog",
      "Breed": "Affenpinscher",
      "AccidentOnly": {"Purebred": "1.04", "MixedBreed": "1.04"}
    }
  ]
}
```

---

## REST API Endpoints Summary

All table APIs follow a consistent REST pattern:

| Operation | HTTP Method | Endpoint | Request Body | Response |
|-----------|------------|----------|--------------|----------|
| **Read** | GET | `/projects/{projectId}/tables/{tableId}` | N/A | TableView (200) |
| **Update** | PUT | `/projects/{projectId}/tables/{tableId}` | EditableTableView | 204 No Content |
| **Append** | POST | `/projects/{projectId}/tables/{tableId}/lines` | AppendTableView | 204 No Content |

**Special Cases:**
- Raw Tables: Add `?raw=true` parameter to GET for raw format
- Example: `GET /projects/MyProject/tables/DATA_Bank?raw=true`

---

## Common Features Across All APIs

### Authentication
- All endpoints require authentication
- Bearer token or basic auth supported
- Authorization: `Authorization: Bearer {token}`

### Error Handling
| Status | Meaning |
|--------|---------|
| 200 | GET successful |
| 204 | PUT/POST successful (no content) |
| 400 | Invalid request format/data |
| 401 | Missing/invalid authentication |
| 403 | Insufficient permissions |
| 404 | Table/project not found |
| 409 | Project locked by another user |

### Data Type Support
All table APIs support standard types:
- String
- Number (Integer, Double, BigDecimal)
- Date (ISO 8601 format)
- Boolean
- Null/empty values

---

## Architecture Overview

### Inheritance Hierarchy (Data/Test Tables)

```
TableView (base)
└── AbstractDataView (headers + rows)
    ├── DataView (+ dataType)
    └── TestView (+ testedTableName)
```

### Reader/Writer Pattern

```
EditableTableReader (interface)
└── AbstractDataTableReader (common logic)
    ├── DataTableReader (+ dataType extraction)
    └── TestTableReader (+ testedTableName extraction)

TableWriter (interface)
└── AbstractDataTableWriter (common logic)
    ├── DataTableWriter (+ dataType writing)
    └── TestTableWriter (+ testedTableName writing)
```

### Code Reuse
- **Data & Test Tables**: Share 95% of logic through inheritance
- **Reader Layer**: ~90 lines of common parsing code reused
- **Writer Layer**: ~155 lines of common writing code reused
- **Concrete implementations**: ~40-55 lines each (header-specific logic only)

---

## Quick Reference

### Supported Table Operations

| Table Type | GET | PUT | POST (Append) |
|-----------|-----|-----|---------------|
| **Data** | ✅ | ✅ | ✅ |
| **Test** | ✅ | ✅ | ✅ |
| **Raw** | ✅ | ✅ | ✅ |
| **SmartLookup** | ✅ | ✅ | ✅ |
| **SimpleLookup** | ✅ | ✅ | ✅ |

### Key Concepts

**Foreign Keys (Data/Test Tables)**
- Reference to another Data table
- Denoted with `>` prefix in Excel (e.g., `>bankRatingList`)
- Stored in `foreignKey` field in JSON

**Display Names (Data/Test Tables)**
- Human-readable column names
- Extracted from appropriate row based on FK presence
- Shown to users in UI

**Hierarchical Headers (Lookup Tables)**
- Support multi-level column grouping
- Organized in tree structure with parent/child relationships
- SmartLookup: Complex multi-dimensional hierarchies
- SimpleLookup: Simple hierarchies

**Hierarchical Rows (Lookup Tables)**
- Row structure mirrors header structure
- Leaf headers → scalar values
- Parent headers → nested objects
- No key duplication across hierarchy levels

**Raw Cells (Raw Tables)**
- Explicit span information (colspan/rowspan)
- Covered cells marked with `covered: true` flag
- Preserves cell merging from original Excel

---

## Testing Scenarios

Each API documentation includes comprehensive testing scenarios:

1. **Basic Operations**: GET, PUT, POST for each table type
2. **Foreign Key Handling** (Data/Test): Tables with and without foreign keys
3. **Merged Cells** (Raw): Origin cells and covered cells
4. **Hierarchical Structure** (Lookup): SmartLookup and SimpleLookup patterns
5. **Edge Cases**: Empty fields, null values, complex nesting

Refer to the specific API documentation for detailed test cases and expected results.

---

## Backward Compatibility

✅ **Fully backward compatible** across all APIs:
- Existing tables work with new API
- Optional fields are truly optional
- No breaking changes to public interfaces
- All previous APIs continue to work

---

## Performance Characteristics

### Time Complexity

**Data/Test Tables:**
- GET: O(n) where n = rows + columns
- PUT: O(n) - replaces entire table
- POST: O(k) where k = rows being appended

**Raw Tables:**
- GET: O(n) where n = cells in matrix
- PUT: O(n + m) where m = merge regions
- POST: O(k + m') where k = new rows, m' = merge regions in new rows

**Lookup Tables:**
- GET: O(n) where n = cells + hierarchies
- PUT: O(n + h) where h = header hierarchy depth
- POST: O(k + h) where k = new rows

**Optimization Tips:**
- Large tables (10k+ rows): Consider pagination (future enhancement)
- Batch appends: More efficient than individual requests
- Raw tables: Two-phase write optimizes merge application

---

## Security

✅ **All APIs include comprehensive security**:
- Permission checks (WRITE required for mutations)
- Project locking enforcement
- ACL integration
- User authentication required
- Input validation and sanitization

---

## Related Documentation

- [Public API Reference](public-api-reference.md) - General OpenL API overview
- [Architecture Guide](/Docs/ARCHITECTURE.md) - System architecture
- [Getting Started](/Docs/START_HERE.md) - Developer onboarding

---

## Implementation Status

| Feature | Data | Test | Raw | SmartLookup | SimpleLookup |
|---------|------|------|-----|-------------|--------------|
| GET | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete |
| PUT | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete |
| POST | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete |
| Foreign Keys | ✅ Yes | ✅ Yes | ✅ Yes | ❌ N/A | ❌ N/A |
| Hierarchical | ❌ No | ❌ No | ✅ Merges | ✅ Complex | ✅ Simple |
| Type Safety | ✅ Yes | ✅ Yes | ❌ Raw | ✅ Yes | ✅ Yes |
| Testing | ✅ Integrated | ✅ Integrated | ✅ Integrated | ✅ Integrated | ✅ Integrated |

---

## Future Enhancements

- [ ] Pagination support for large tables
- [ ] Cell formatting/styling information
- [ ] Cell comments/annotations
- [ ] Source formula preservation
- [ ] Batch export/import multiple tables
- [ ] Custom cell data type handling
- [ ] GraphQL API support

---

**Last Updated**: 2025-12-02
**Version**: 6.0.0-SNAPSHOT

For API endpoint details, see individual table type documentation above.
