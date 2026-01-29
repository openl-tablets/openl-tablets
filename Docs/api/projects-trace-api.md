# Projects Trace API Documentation

**Version**: 6.0.0-SNAPSHOT
**Status**: BETA
**Base Path**: `/projects/{projectId}/trace`
**Last Updated**: 2026-01-29

---

## Table of Contents

1. [Overview](#overview)
2. [API Reference](#api-reference)
3. [Data Models](#data-models)
4. [Workflows](#workflows)
5. [Error Handling](#error-handling)
6. [Examples](#examples)
7. [Best Practices](#best-practices)

---

## Overview

The Projects Trace API provides a comprehensive REST interface for debugging and tracing OpenL rules execution. This API enables developers to start trace execution for rules tables, retrieve hierarchical trace trees, and inspect execution details including parameters, runtime context, and results.

### Key Features

- **Asynchronous Execution**: Non-blocking trace execution with WebSocket progress notifications
- **Hierarchical Trace Tree**: Complete execution tree with lazy-loading support for large traces
- **Lazy Parameter Loading**: Large parameter values loaded on-demand to optimize initial response size
- **Test Suite Support**: Trace individual test cases or ranges from test suite methods
- **Table HTML Rendering**: View traced tables with execution path highlighting
- **Session Management**: One trace per session with automatic cancellation of previous traces

### Use Cases

1. **Rule Debugging**: Trace execution to understand why a rule produced a specific result
2. **Decision Table Analysis**: Visualize which conditions matched and which rules fired
3. **Performance Analysis**: Identify slow execution paths in complex rule chains
4. **Test Case Debugging**: Trace specific test cases to understand failures
5. **Spreadsheet Cell Tracing**: Follow cell evaluation order and dependencies

### Architecture Overview

``` mermaid
flowchart TB
    subgraph REST["REST Controller Layer"]
        C[ProjectsTraceController<br/>- 7 REST endpoints<br/>- Start/cancel trace<br/>- Get results/export<br/>- Lazy loading]
    end

    subgraph SERVICE["Service Layer"]
        S1[TraceExecutorService<br/>- Async execution<br/>- Test suite tracing]
        S2[TraceTableHtmlService<br/>- HTML rendering]
        S3[TraceExportService<br/>- Text export]
        S4[TableInputParserService<br/>- JSON input parsing]
    end

    subgraph SESSION["Session Layer"]
        R1[ExecutionTraceResultRegistry<br/>- Task tracking<br/>- One trace per session]
        R2[TraceParameterRegistry<br/>- Lazy parameter storage]
    end

    subgraph CORE["Core Tracing"]
        T[TreeBuildTracer<br/>- Trace collection<br/>- Lazy nodes]
        H[TraceHelper<br/>- Node caching]
    end

    C --> S1
    C --> S2
    C --> S3
    C --> S4
    C --> R1
    C --> R2
    S1 --> T
    T --> H
```

---

## API Reference

### 1. Start Trace Execution

**Endpoint**: `POST /projects/{projectId}/trace`

**Description**: Starts asynchronous trace execution for a table. Any previous trace for this session is automatically cancelled.

**HTTP Method**: POST

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Query Parameters**:
- `tableId` (string, required): Table ID to trace
- `testRanges` (string, optional): Test ranges for TestSuiteMethod (e.g., "1-3,5")
- `fromModule` (string, optional): Module name for current opened module execution

**Request Body** (optional):
```json
{
  "runtimeContext": {
    "lob": "Auto",
    "usState": "CA",
    "currentDate": "2026-01-28"
  },
  "params": {
    "age": 25,
    "driverRecord": {
      "accidents": 0,
      "violations": 1
    }
  }
}
```

**Response**: `202 Accepted` (no body)

**Behavior**:
1. Cancels any previous trace execution
2. Clears parameter registry
3. Creates new TraceHelper for session caching
4. Detects if table is TestSuiteMethod or regular method
5. For TestSuiteMethod: Parses testRanges and executes traces
6. For regular methods: Parses JSON input parameters and runtime context
7. Submits async task via TraceExecutorService
8. Registers WebSocket progress listener

**WebSocket Progress Events**:
- `PENDING`: Trace queued
- `STARTED`: Execution in progress
- `COMPLETED`: Execution finished successfully
- `INTERRUPTED`: Cancelled by user
- `ERROR`: Failed with exception

**Errors**:
- `404 Not Found`: Table not found

---

### 2. Get Trace Node Children

**Endpoint**: `GET /projects/{projectId}/trace/nodes`

**Description**: Retrieves child nodes for lazy loading, or root nodes if no node ID is provided. Use this to progressively load the trace tree.

**HTTP Method**: GET

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Query Parameters**:
- `id` (integer, optional): Node ID (omit for root nodes, defaults to 0)
- `showRealNumbers` (boolean, default: false): Show exact numbers

**Response**: `200 OK`
```json
[
  {
    "key": 3,
    "title": "DecisionTable BankRating",
    "tooltip": "Decision table with 12 rules",
    "type": "method",
    "lazy": true,
    "extraClasses": "",
    "error": false
  },
  {
    "key": 4,
    "title": "Rule 1: rating = 'A'",
    "tooltip": "Condition matched",
    "type": "rule",
    "lazy": false,
    "extraClasses": "rule result",
    "error": false
  }
]
```

**Behavior**:
1. If `id` is not provided, returns root nodes (immediate children of the trace root)
2. If `id` is provided, retrieves node from TraceHelper cache by ID and returns its children
3. Initializes lazy children on the node (if any)
4. Maps children to simple TraceNodeView (no detailed fields)

**Errors**:
- `404 Not Found`: No trace task exists
- `409 Conflict`: Trace execution not yet completed

---

### 3. Get Trace Node Details

**Endpoint**: `GET /projects/{projectId}/trace/nodes/{nodeId}`

**Description**: Retrieves detailed information for a single trace node including parameters, context, result, and errors.

**HTTP Method**: GET

**Path Parameters**:
- `projectId` (string, required): Project identifier
- `nodeId` (integer, required): Node ID to retrieve

**Query Parameters**:
- `showRealNumbers` (boolean, default: false): Show exact numbers

**Response**: `200 OK`
```json
{
  "key": 4,
  "title": "Rule 1: rating = 'A'",
  "tooltip": "Condition matched",
  "type": "rule",
  "lazy": false,
  "extraClasses": "rule result",
  "error": false,
  "parameters": [
    {
      "name": "bank",
      "description": "Bank",
      "lazy": false,
      "parameterId": null,
      "value": {
        "bankID": "commerz",
        "bankRatings": ["MA2", "FA+"]
      },
      "schema": {
        "type": "object",
        "properties": {
          "bankID": {"type": "string"},
          "bankRatings": {"type": "array", "items": {"type": "string"}}
        }
      }
    },
    {
      "name": "userData",
      "description": "UserData",
      "lazy": true,
      "parameterId": 7,
      "value": null,
      "schema": {
        "type": "object",
        "properties": {}
      }
    }
  ],
  "context": {
    "name": "context",
    "description": "IRulesRuntimeContext",
    "lazy": false,
    "parameterId": null,
    "value": {
      "lob": "Auto",
      "currentDate": "2026-01-28"
    },
    "schema": {}
  },
  "result": {
    "name": "result",
    "description": "String",
    "lazy": false,
    "parameterId": null,
    "value": "A",
    "schema": {"type": "string"}
  },
  "errors": []
}
```

**Notes**:
- Uses `@JsonView(GenericView.Full.class)` - includes all detailed fields
- Parameters with `lazy: true` must be fetched separately via `/parameters/{parameterId}`
- JSON Schema provided for each parameter to aid client-side rendering
- `errors` contains MessageDescription objects if execution failed

**Errors**:
- `404 Not Found`: No trace task exists or node not found
- `409 Conflict`: Trace execution not yet completed

---

### 4. Cancel Trace Execution

**Endpoint**: `DELETE /projects/{projectId}/trace`

**Description**: Cancels the current trace execution if running. Always succeeds (idempotent).

**HTTP Method**: DELETE

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Response**: `204 No Content`

**Behavior**:
1. Cancels current task if running
2. Clears parameter registry

---

### 5. Get Lazy Parameter Value

**Endpoint**: `GET /projects/{projectId}/trace/parameters/{parameterId}`

**Description**: Retrieves the full JSON value for a lazy-loaded parameter.

**HTTP Method**: GET

**Path Parameters**:
- `projectId` (string, required): Project identifier
- `parameterId` (integer, required): Parameter ID from trace node details

**Response**: `200 OK`
```json
{
  "name": "userData",
  "description": "UserData",
  "lazy": false,
  "parameterId": null,
  "value": {
    "userId": "12345",
    "preferences": {
      "theme": "dark",
      "notifications": true
    },
    "history": [
      {"date": "2026-01-01", "action": "login"},
      {"date": "2026-01-15", "action": "purchase"}
    ]
  },
  "schema": {
    "type": "object",
    "properties": {
      "userId": {"type": "string"},
      "preferences": {"type": "object"},
      "history": {"type": "array"}
    }
  }
}
```

**Notes**:
- Returns full value (not lazy) with `lazy: false` and `parameterId: null`
- Used for progressively loading large parameter values

**Errors**:
- `404 Not Found`: Parameter ID not found in registry
- `409 Conflict`: Trace execution not yet completed

---

### 6. Get Traced Table HTML

**Endpoint**: `GET /projects/{projectId}/trace/nodes/{nodeId}/table`

**Description**: Returns an HTML fragment for the traced table with execution path highlighting.

**HTTP Method**: GET

**Path Parameters**:
- `projectId` (string, required): Project identifier
- `nodeId` (integer, required): Trace node ID

**Query Parameters**:
- `showFormulas` (boolean, default: false): Show cell formulas instead of values

**Response**: `200 OK`
- Content-Type: `text/html`
- Body: HTML table fragment with traced cells highlighted

**Example Response**:
```html
<table class="openl-table traced">
  <tr>
    <td class="header">Condition</td>
    <td class="header">Rating</td>
  </tr>
  <tr class="traced-row">
    <td class="traced-cell matched">age > 25</td>
    <td class="traced-cell result">A</td>
  </tr>
  <tr>
    <td>age <= 25</td>
    <td>B</td>
  </tr>
</table>
```

**Errors**:
- `404 Not Found`: No trace task exists
- `409 Conflict`: Trace execution not yet completed

---

### 7. Export Trace to File

**Endpoint**: `GET /projects/{projectId}/trace/export`

**Description**: Exports the complete trace tree to a text file. Optionally releases trace memory after export.

**HTTP Method**: GET

**Path Parameters**:
- `projectId` (string, required): Project identifier

**Query Parameters**:
- `showRealNumbers` (boolean, default: false): Show exact numbers without formatting
- `release` (boolean, default: false): Release trace memory after export (clears registry)

**Response**: `200 OK`
- Content-Type: `text/plain`
- Content-Disposition: `attachment; filename="trace.txt"`
- Body: Plain text trace tree

**Example Response**:
```text
DT_RiskAssessment
├── Rule 1: creditScore >= 700
│   ├── Condition: creditScore = 720 >= 700 → true
│   └── Result: LOW_RISK
├── Rule 2: income >= 50000
│   ├── Condition: income = 75000 >= 50000 → true
│   └── Result: (combined)
└── Final Result: LOW_RISK
```

**Behavior**:
1. Streams trace directly to response (no RAM buffering for large traces)
2. If timeout occurs during export, appends `!!!TRACE WAS LIMITED BY TIMEOUT!!!` message
3. If `release=true`, clears both trace result registry and parameter registry after export
4. UTF-8 encoding

**Use Cases**:
- Save trace for offline analysis
- Share trace with team members
- Archive trace for debugging sessions
- Memory cleanup after debugging (with `release=true`)

**Errors**:
- `404 Not Found`: No trace task exists
- `409 Conflict`: Trace execution not yet completed

---

## Data Models

### TraceInputRequest

```typescript
interface TraceInputRequest {
  runtimeContext?: Record<string, any>;  // IRulesRuntimeContext properties
  params?: Record<string, any>;          // Method parameters by name
}
```

**Example**:
```json
{
  "runtimeContext": {
    "lob": "Auto",
    "usState": "CA"
  },
  "params": {
    "age": 30,
    "coverage": "Full"
  }
}
```

---

### TraceNodeView

```typescript
interface TraceNodeView {
  // Short fields (always included)
  key: number;               // Unique node identifier
  title: string;             // Display name
  tooltip: string;           // Hover text
  type: string;              // Node type
  lazy: boolean;             // Whether children can be loaded
  extraClasses: string;      // CSS styling classes
  error: boolean;            // Whether this node has an error

  // Full fields (only in detailed view)
  parameters?: TraceParameterValue[];  // Input parameters
  context?: TraceParameterValue;       // Runtime context
  result?: TraceParameterValue;        // Method result
  errors?: MessageDescription[];       // Execution errors
}
```

**Node Types**:
- `"method"` - Method/function execution
- `"test"` - Test suite execution
- `"rule"` - Decision table rule
- `"condition"` - Condition evaluation
- `"result"` - Result value
- `"spreadsheet"` - Spreadsheet cell
- `"match"` - Pattern match operation

**Extra Classes** (for styling):
- `"rule fail"` - Failed rule condition
- `"rule result"` - Rule that produced result
- `"rule no_result"` - Rule without result
- `"condition result"` - Successful condition
- `"value"` - Value/result node

---

### TraceParameterValue

```typescript
interface TraceParameterValue {
  name: string;              // Parameter name
  description: string;       // Type description
  lazy: boolean;             // Whether value is lazy-loaded
  parameterId?: number;      // ID for lazy loading (null if value included)
  value?: any;               // Full JSON value (null if lazy=true)
  schema: object;            // JSON Schema for type validation
}
```

**Lazy Loading**:
- If `lazy: true`, `value` is null and `parameterId` is set
- Use `GET /trace/parameters/{parameterId}` to fetch full value
- Large objects/arrays are marked as lazy to optimize initial response

---

### TraceExecutionStatus (WebSocket)

```typescript
enum TraceExecutionStatus {
  PENDING = "PENDING",           // Awaiting execution start
  STARTED = "STARTED",           // Execution in progress
  COMPLETED = "COMPLETED",       // Successfully finished
  INTERRUPTED = "INTERRUPTED",   // Cancelled by user
  ERROR = "ERROR"                // Failed with exception
}
```

---

### MessageDescription (Errors)

```typescript
interface MessageDescription {
  severity: "ERROR" | "WARNING" | "INFO";
  summary: string;
  detail?: string;
  sourceLocation?: string;
}
```

---

## Workflows

### Workflow 1: Trace a Regular Method

```
1. Start trace execution
   POST /projects/MyProject/trace?tableId=TABLE_BankRating
   {
     "params": {"bankId": "commerz"},
     "runtimeContext": {"lob": "Commercial"}
   }

   Response: 202 Accepted

2. Wait for completion (via WebSocket or polling)
   WebSocket: PENDING -> STARTED -> COMPLETED

3. Get root nodes
   GET /projects/MyProject/trace/nodes

   Response: [
     {"key": 1, "title": "BankRating", "lazy": true, ...}
   ]

4. Expand node to get children
   GET /projects/MyProject/trace/nodes?id=1

   Response: [
     {"key": 2, "title": "Rule 1", "lazy": false, ...},
     {"key": 3, "title": "Rule 2", "lazy": false, ...}
   ]

5. Get node details
   GET /projects/MyProject/trace/nodes/2

   Response: {
     "key": 2,
     "title": "Rule 1: matched",
     "parameters": [...],
     "result": {...}
   }

6. Load lazy parameter if needed
   GET /projects/MyProject/trace/parameters/7

   Response: {
     "name": "complexData",
     "value": {...full value...}
   }
```

---

### Workflow 2: Trace Test Cases

```
1. Start trace for specific test cases
   POST /projects/MyProject/trace?tableId=TEST_BankRating&testRanges=1-3,5

   Response: 202 Accepted

2. Wait for completion

3. Get root nodes (test cases)
   GET /projects/MyProject/trace/nodes

   Response: [
     {"key": 1, "title": "Test Case 1", "type": "test", ...},
     {"key": 2, "title": "Test Case 2", "type": "test", ...},
     {"key": 3, "title": "Test Case 3", "type": "test", ...},
     {"key": 4, "title": "Test Case 5", "type": "test", ...}
   ]

4. Expand specific test case
   GET /projects/MyProject/trace/nodes?id=1

   Response: [
     {"key": 5, "title": "BankRating", "type": "method", ...},
     {"key": 6, "title": "Rule 1", "type": "rule", ...}
   ]
```

---

### Workflow 3: View Traced Table

```
1. Start and complete trace
   POST /projects/MyProject/trace?tableId=DT_PricingRules
   ...wait for completion...

2. Get root nodes and find table node
   GET /projects/MyProject/trace/nodes
   GET /projects/MyProject/trace/nodes?id=1

3. Get HTML table with highlighting
   GET /projects/MyProject/trace/nodes/3/table

   Response: <table class="traced">...highlighted cells...</table>

4. Optionally show formulas
   GET /projects/MyProject/trace/nodes/3/table?showFormulas=true

   Response: <table>...formulas instead of values...</table>
```

---

### Workflow 4: Cancel and Restart

```
1. Start trace
   POST /projects/MyProject/trace?tableId=TABLE_A
   Response: 202 Accepted

2. Cancel before completion
   DELETE /projects/MyProject/trace
   Response: 204 No Content

3. Start new trace (previous cancelled automatically)
   POST /projects/MyProject/trace?tableId=TABLE_B
   Response: 202 Accepted
```

---

### Workflow 5: Export and Release Trace

```
1. Start and complete trace
   POST /projects/MyProject/trace?tableId=DT_PricingRules
   ...wait for completion...

2. Explore trace interactively
   GET /projects/MyProject/trace/nodes
   GET /projects/MyProject/trace/nodes?id=1
   GET /projects/MyProject/trace/nodes/5

3. Export trace to file for archiving
   GET /projects/MyProject/trace/export?showRealNumbers=true

   Response: (text file download)
   Content-Disposition: attachment; filename="trace.txt"

4. When done, export and release memory
   GET /projects/MyProject/trace/export?release=true

   Response: (text file download)
   Note: Trace memory is released after this call
```

---

## Error Handling

### Error Response Format

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "table.message",
  "path": "/projects/MyProject/trace"
}
```

---

### Common Error Scenarios

#### 1. Table Not Found

**Scenario**: The specified tableId does not exist

**Request**:
```bash
POST /projects/MyProject/trace?tableId=INVALID_TABLE
```

**Response**: `404 Not Found`
```json
{
  "message": "table.message"
}
```

**Resolution**: Verify the tableId is correct

---

#### 2. No Trace Task Exists

**Scenario**: Attempting to get results without starting a trace

**Request**:
```bash
GET /projects/MyProject/trace/nodes
```

**Response**: `404 Not Found`
```json
{
  "message": "trace.execution.task.message"
}
```

**Resolution**: Start a trace first with POST

---

#### 3. Trace Not Yet Completed

**Scenario**: Attempting to get results while trace is still running

**Request**:
```bash
GET /projects/MyProject/trace/nodes
```

**Response**: `409 Conflict`
```json
{
  "message": "trace.execution.not.completed.message"
}
```

**Resolution**: Wait for WebSocket COMPLETED event or poll until ready

---

#### 4. Node Not Found

**Scenario**: Requesting details for an invalid node ID

**Request**:
```bash
GET /projects/MyProject/trace/nodes/999
```

**Response**: `404 Not Found`
```json
{
  "message": "trace.node.not.found.message"
}
```

**Resolution**: Use valid node ID from trace nodes list

---

#### 5. Parameter Not Found

**Scenario**: Requesting a lazy parameter with invalid ID

**Request**:
```bash
GET /projects/MyProject/trace/parameters/999
```

**Response**: `404 Not Found`
```json
{
  "message": "trace.parameter.not.found.message"
}
```

**Resolution**: Use parameterId from node details

---

## Examples

### Example 1: Trace a Decision Table

```bash
# Step 1: Start trace
curl -X POST "http://localhost:8080/projects/MyProject/trace?tableId=DT_RiskAssessment" \
  -H "Content-Type: application/json" \
  -d '{
    "params": {
      "age": 35,
      "income": 75000,
      "creditScore": 720
    },
    "runtimeContext": {
      "lob": "Personal",
      "usState": "NY"
    }
  }'

# Response: 202 Accepted (no body)

# Step 2: Wait and get root nodes
curl "http://localhost:8080/projects/MyProject/trace/nodes"

# Response
[
  {
    "key": 1,
    "title": "DT_RiskAssessment",
    "type": "method",
    "lazy": true
  }
]

# Step 3: Get children
curl "http://localhost:8080/projects/MyProject/trace/nodes?id=1"

# Response
[
  {"key": 2, "title": "Rule 1: creditScore >= 700", "type": "rule", "extraClasses": "rule result"},
  {"key": 3, "title": "Rule 2: income >= 50000", "type": "rule", "extraClasses": "rule result"},
  {"key": 4, "title": "Result: LOW_RISK", "type": "result"}
]

# Step 4: Get rule details
curl "http://localhost:8080/projects/MyProject/trace/nodes/2"

# Response
{
  "key": 2,
  "title": "Rule 1: creditScore >= 700",
  "type": "rule",
  "parameters": [
    {"name": "creditScore", "value": 720, "description": "int"}
  ],
  "result": {"name": "result", "value": true, "description": "boolean"}
}
```

---

### Example 2: Trace Test Cases

```bash
# Trace test cases 1, 2, 3, and 5
curl -X POST "http://localhost:8080/projects/MyProject/trace?tableId=TEST_RiskAssessment&testRanges=1-3,5"

# Response: 202 Accepted

# Get root nodes (test cases)
curl "http://localhost:8080/projects/MyProject/trace/nodes"

# Response
[
  {"key": 1, "title": "Test Case 1: age=25, income=50000", "type": "test"},
  {"key": 2, "title": "Test Case 2: age=45, income=100000", "type": "test"},
  {"key": 3, "title": "Test Case 3: age=30, income=75000", "type": "test"},
  {"key": 4, "title": "Test Case 5: age=60, income=25000", "type": "test"}
]
```

---

### Example 3: Get Table HTML with Highlighting

```bash
curl "http://localhost:8080/projects/MyProject/trace/nodes/3/table" \
  -H "Accept: text/html"

# Response (HTML)
<table class="openl-table traced">
  <tr class="header-row">
    <td>C1</td><td>C2</td><td>RET1</td>
  </tr>
  <tr class="traced-row matched">
    <td class="condition traced">creditScore >= 700</td>
    <td class="condition traced">income >= 50000</td>
    <td class="return traced">LOW_RISK</td>
  </tr>
  <tr>
    <td>creditScore >= 600</td>
    <td>income >= 30000</td>
    <td>MEDIUM_RISK</td>
  </tr>
</table>
```

---

### Example 4: Cancel Trace

```bash
# Cancel current trace
curl -X DELETE "http://localhost:8080/projects/MyProject/trace"

# Response: 204 No Content
```

---

### Example 5: Export Trace to File

```bash
# Export trace to file
curl "http://localhost:8080/projects/MyProject/trace/export" \
  -o trace.txt

# Export with exact numbers
curl "http://localhost:8080/projects/MyProject/trace/export?showRealNumbers=true" \
  -o trace_detailed.txt

# Export and release memory (cleanup)
curl "http://localhost:8080/projects/MyProject/trace/export?release=true" \
  -o trace_final.txt

# Response: Plain text trace tree
DT_RiskAssessment
├── Rule 1: creditScore >= 700
│   ├── Condition: creditScore = 720 >= 700 → true
│   └── Result: LOW_RISK
└── Final Result: LOW_RISK
```

---

## Best Practices

### 1. Use WebSocket for Progress

Instead of polling, subscribe to WebSocket for real-time status:

```javascript
const ws = new WebSocket("ws://localhost:8080/ws/trace-progress");
ws.onmessage = (event) => {
  const status = JSON.parse(event.data);
  if (status.status === "COMPLETED") {
    fetchRootNodes();
  }
};
```

### 2. Implement Lazy Loading for Large Traces

```javascript
// Initial load - get root nodes
const rootNodes = await fetch("/projects/MyProject/trace/nodes");
const nodes = await rootNodes.json();

// On user expand - load children
async function expandNode(nodeId) {
  const children = await fetch(`/projects/MyProject/trace/nodes?id=${nodeId}`);
  return children.json();
}

// On user click - load details
async function getDetails(nodeId) {
  const details = await fetch(`/projects/MyProject/trace/nodes/${nodeId}`);
  return details.json();
}
```

### 3. Handle Lazy Parameters

```javascript
async function getFullParameterValue(param) {
  if (param.lazy && param.parameterId) {
    const response = await fetch(
      `/projects/MyProject/trace/parameters/${param.parameterId}`
    );
    return response.json();
  }
  return param;
}
```

### 4. Use Test Ranges Efficiently

```bash
# Single test case
?testRanges=5

# Range of test cases
?testRanges=1-10

# Multiple ranges
?testRanges=1-3,5,8-10

# All test cases (omit parameter)
# Just use tableId without testRanges
```

### 5. Poll with Backoff if Not Using WebSocket

```javascript
async function waitForCompletion(projectId, maxAttempts = 30) {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const result = await fetch(`/projects/${projectId}/trace/nodes`);
      if (result.status === 200) {
        return result.json();
      }
    } catch (e) {
      if (e.status !== 409) throw e;  // 409 = not completed yet
    }
    await sleep(1000 * Math.min(i + 1, 5));  // Exponential backoff, max 5s
  }
  throw new Error("Trace execution timeout");
}
```

### 6. Clean Up on User Navigation

```javascript
// Cancel trace when user leaves the page
window.addEventListener("beforeunload", () => {
  navigator.sendBeacon(`/projects/${projectId}/trace`, "");
  // Or use DELETE if supported
});
```

### 7. Handle Real Numbers Option

```javascript
// For debugging, show exact values
const detailedResult = await fetch(
  `/projects/MyProject/trace/nodes/${nodeId}?showRealNumbers=true`
);

// For display, use formatted values (default)
const displayResult = await fetch(
  `/projects/MyProject/trace/nodes/${nodeId}`
);
```

### 8. Use Export for Memory Management

```javascript
// After debugging session, export and release memory
async function finishDebugging(projectId) {
  // Download trace file for records
  const response = await fetch(
    `/projects/${projectId}/trace/export?release=true`
  );

  // Save to file
  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'trace.txt';
  a.click();

  // Memory is now released server-side
}
```

### 9. Handle Large Trace Exports

```javascript
// For very large traces, stream the response
async function exportLargeTrace(projectId) {
  const response = await fetch(
    `/projects/${projectId}/trace/export?showRealNumbers=true`
  );

  // Stream to file (for large traces)
  const reader = response.body.getReader();
  const chunks = [];

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    chunks.push(value);
  }

  // Handle timeout message at end
  const text = new TextDecoder().decode(
    new Uint8Array(chunks.flatMap(c => [...c]))
  );

  if (text.includes('!!!TRACE WAS LIMITED BY TIMEOUT!!!')) {
    console.warn('Trace was truncated due to timeout');
  }

  return text;
}
```

---

## Technical Implementation Notes

### Session Management

**Storage**: One trace task per HTTP session per project

**Lifecycle**:
1. Start trace: Previous task cancelled, new task registered
2. Get results: Task must be completed
3. Cancel: Task cancelled, registry cleared
4. Session timeout: Task automatically cleaned up

### Async Execution

**Executor**: Uses Spring `@Async` with `testSuiteExecutor` thread pool

**CompletableFuture**: Non-blocking execution with callback on completion

### Trace Tree Caching

**TraceHelper**: Uses BidiMap for O(1) lookup by node ID

**Lazy Nodes**: ITracerObject children materialized on first access

### Parameter Registry

**Session-scoped**: Each user session has independent parameter storage

**On-demand**: Large values not serialized until requested

---

## Related APIs

- **Projects API**: Project management operations
- **Tables API**: Table read/write operations
- **Test API**: Test execution and results

---

## Changelog

### Version 6.0.0-SNAPSHOT (BETA)
- Initial implementation of Projects Trace API
- Asynchronous trace execution with CompletableFuture
- Lazy loading for trace nodes and parameters
- WebSocket progress notifications
- Test suite support with range selection
- Table HTML rendering with trace highlighting
- Session-based task management
- **Added**: Export trace to file endpoint (`GET /trace/export`)
- **Added**: `error` field in TraceNodeView for error indication
- **Added**: `release` parameter for memory cleanup after export
- **Added**: TableInputParserService for flexible JSON input parsing
- **Added**: TraceExportService for text export with timeout handling

---

## Support

For issues or questions:
- **GitHub Issues**: https://github.com/openl-tablets/openl-tablets/issues
- **Documentation**: https://openl-tablets.org
- **API Status**: BETA - Subject to changes in future releases

---

**Note**: This API is currently in BETA status. The interface may change in future releases. Feedback and bug reports are welcome.
