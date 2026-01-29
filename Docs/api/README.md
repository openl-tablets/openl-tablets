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

### 5. **Projects Merge API (BETA)** ([projects-merge-api.md](projects-merge-api.md))

The Projects Merge API provides comprehensive Git branch merge operations with conflict detection and resolution capabilities.

**Key Features:**
- Check merge feasibility between branches
- Bidirectional merging (receive/send modes)
- Automatic conflict detection
- Multiple conflict resolution strategies (BASE, OURS, THEIRS, CUSTOM)
- Session-based conflict state management
- Excel file prioritization in conflict lists

**Use Cases:**
- Automated branch merging in CI/CD pipelines
- Programmatic conflict resolution
- Multi-branch development workflows
- Release management and feature merging

**Example:**
```bash
# Check if merge is possible
POST /projects/MyProject/merge/check
{
  "mode": "receive",
  "otherBranch": "feature-auth"
}

# Perform merge
POST /projects/MyProject/merge
{
  "mode": "receive",
  "otherBranch": "feature-auth"
}

# If conflicts detected, resolve them
POST /projects/MyProject/merge/conflicts/resolve
Content-Type: multipart/form-data

resolutions=[
  {"filePath": "rules/BusinessRules.xlsx", "strategy": "OURS"},
  {"filePath": "config.xml", "strategy": "THEIRS"}
]
message="Merged feature-auth: kept our business rules"
```

**Architecture Documentation:**
- [Projects Merge Architecture](projects-merge-architecture.md) - Detailed architecture design, component interactions, and implementation details

---

### 6. **Personal Access Token API** ([personal-access-token-api.md](personal-access-token-api.md))

The Personal Access Token (PAT) API enables users to generate and manage authentication tokens for programmatic access to OpenL Tablets Studio. PATs provide an alternative to OAuth2/SAML authentication for service-to-service communication and API integrations.

**Key Features:**
- Create, list, retrieve, and delete personal access tokens
- Cryptographically secure token generation (Base62 encoding, 285 bits entropy)
- Optional token expiration for time-limited access
- User isolation (users manage only their own tokens)
- OAuth2/SAML authentication required for management operations
- PAT authentication cannot be used to manage PATs

**Use Cases:**
- Service-to-service authentication
- CI/CD pipeline integration
- API client authentication
- MCP server authentication
- Automated testing and deployment

**Token Format:**
```
openl_pat_<publicId>.<secret>
           ^^^^^^^^   ^^^^^^
           16 chars   32 chars
```

**Example:**
```bash
# Create a token (requires OAuth2/SAML authentication)
POST /rest/users/personal-access-tokens
Authorization: Bearer <oauth2-token>
{
  "name": "CI/CD Pipeline Token",
  "expiresAt": "2026-12-31T23:59:59Z"
}

# Response (token shown only once!)
{
  "publicId": "x1Y2z3A4b5C6d7E8",
  "name": "CI/CD Pipeline Token",
  "loginName": "jenkins",
  "token": "openl_pat_x1Y2z3A4b5C6d7E8.f9G0h1I2j3K4l5M6n7O8p9Q0r1S2t3U4v5W6",
  "createdAt": "2025-12-23T10:30:00Z",
  "expiresAt": "2026-12-31T23:59:59Z"
}

# Use token for API access
GET /rest/design/list
Authorization: Token openl_pat_x1Y2z3A4b5C6d7E8.f9G0h1I2j3K4l5M6n7O8p9Q0r1S2t3U4v5W6
```

**Security Features:**
- BCrypt secret hashing with automatic salting
- Timing attack prevention with constant-time validation
- Public ID and secret separation for fast lookups
- DoS prevention with token length limits
- No information disclosure on validation failures

**Architecture Documentation:**
- [Personal Access Token Architecture](personal-access-token-architecture.md) - Detailed architecture design, security considerations, and implementation details

---

### 7. **Projects Trace API (BETA)** ([projects-trace-api.md](projects-trace-api.md))

The Projects Trace API provides comprehensive debugging and execution tracing capabilities for OpenL rules. It enables step-by-step analysis of rule execution including decision table evaluations, spreadsheet cell computations, and method calls.

**Key Features:**
- Asynchronous trace execution with WebSocket progress notifications
- Hierarchical trace tree with lazy loading for large traces
- Lazy parameter loading for complex objects
- Test suite support with range selection (e.g., "1-3,5")
- Table HTML rendering with execution path highlighting
- Session-based task management (one trace per session)

**Use Cases:**
- Rule debugging and analysis
- Decision table execution visualization
- Test case failure investigation
- Performance analysis of rule chains
- Spreadsheet cell dependency tracing

**Example:**
```bash
# Start trace execution
POST /projects/MyProject/trace?tableId=DT_RiskRating
{
  "params": {"age": 35, "income": 75000},
  "runtimeContext": {"lob": "Auto"}
}
# Response: 202 Accepted

# Get trace result (after completion)
GET /projects/MyProject/trace

# Response
{
  "rootNodes": [
    {"key": 1, "title": "DT_RiskRating", "type": "method", "lazy": true}
  ],
  "totalNodes": 42
}

# Get node details
GET /projects/MyProject/trace/nodes/1
# Returns full details including parameters, context, result

# Get traced table with highlighting
GET /projects/MyProject/trace/nodes/1/table
# Returns HTML with traced cells highlighted
```

**Architecture Documentation:**
- [Projects Trace Architecture](projects-trace-architecture.md) - Detailed architecture design, component interactions, lazy loading strategy, and implementation details

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

### Table APIs

| Feature | Data | Test | Raw | SmartLookup | SimpleLookup |
|---------|------|------|-----|-------------|--------------|
| GET | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete |
| PUT | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete |
| POST | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete | ✅ Complete |
| Foreign Keys | ✅ Yes | ✅ Yes | ✅ Yes | ❌ N/A | ❌ N/A |
| Hierarchical | ❌ No | ❌ No | ✅ Merges | ✅ Complex | ✅ Simple |
| Type Safety | ✅ Yes | ✅ Yes | ❌ Raw | ✅ Yes | ✅ Yes |
| Testing | ✅ Integrated | ✅ Integrated | ✅ Integrated | ✅ Integrated | ✅ Integrated |

### Projects Merge API (BETA)

| Feature | Status | Notes |
|---------|--------|-------|
| Check Merge | ✅ Complete | Pre-merge validation |
| Perform Merge | ✅ Complete | Bidirectional (receive/send) |
| Get Conflicts | ✅ Complete | Session-based storage |
| Download Files | ✅ Complete | BASE/OURS/THEIRS versions |
| Resolve Conflicts | ✅ Complete | Multiple strategies |
| Cancel Merge | ✅ Complete | Session cleanup |
| Excel Priority | ✅ Yes | Business logic first |
| Custom Resolution | ✅ Yes | File upload support |
| Integration Tests | ✅ Complete | Full workflow coverage |

### Personal Access Token API

| Feature | Status | Notes |
|---------|--------|-------|
| Create Token | ✅ Complete | OAuth2/SAML authentication required |
| List Tokens | ✅ Complete | User's own tokens only |
| Get Token | ✅ Complete | Ownership verification |
| Delete Token | ✅ Complete | Immediate revocation |
| Token Validation | ✅ Complete | Constant-time, secure |
| PAT Authentication | ✅ Complete | Spring Security filter integration |
| Expiration Support | ✅ Complete | Optional time-limited tokens |
| Security Hardening | ✅ Complete | Timing attack prevention, BCrypt hashing |
| @NotPatAuth | ✅ Complete | PATs cannot manage PATs |
| Unit Tests | ✅ Complete | Comprehensive coverage |
| Integration Tests | ✅ Complete | Full workflow with OAuth2 |

### Projects Trace API (BETA)

| Feature | Status | Notes |
|---------|--------|-------|
| Start Trace | ✅ Complete | Async execution with CompletableFuture |
| Get Trace Result | ✅ Complete | Root nodes with total count |
| Get Node Children | ✅ Complete | Lazy loading support |
| Get Node Details | ✅ Complete | Parameters, context, result, errors |
| Cancel Trace | ✅ Complete | Idempotent cancellation |
| Lazy Parameters | ✅ Complete | On-demand loading for large values |
| Table HTML | ✅ Complete | Traced cells highlighting |
| Test Range Support | ✅ Complete | Parse ranges like "1-3,5" |
| WebSocket Progress | ✅ Complete | PENDING/STARTED/COMPLETED/ERROR |
| Session Management | ✅ Complete | One trace per session |
| JSON Schema | ✅ Complete | Schema generation for parameters |

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

**Last Updated**: 2026-01-28
**Version**: 6.0.0-SNAPSHOT

For API endpoint details, see individual table type documentation above.
