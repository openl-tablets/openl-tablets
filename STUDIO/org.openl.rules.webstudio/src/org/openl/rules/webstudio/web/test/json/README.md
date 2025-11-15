# Test Cases JSON Import Feature

## Overview

This feature allows you to import test cases from JSON files into OpenL Studio test tables. This is useful for:
- Bulk importing test cases from external sources
- Generating test cases programmatically
- Sharing test data across teams
- Automating test case creation

## How to Use

### 1. Access the Import UI

Navigate to: `/pages/modules/test/importTestCasesFromJson.html` in your OpenL Studio instance.

Alternatively, you can use the REST API directly (see API section below).

### 2. Prepare Your JSON File

Create a JSON file following this structure:

```json
{
  "tableName": "YourMethodName",
  "testTableName": "YourMethodNameTest",
  "appendToExisting": false,
  "testCases": [
    {
      "_id_": "TC001",
      "_description_": "Test case description",
      "parameters": {
        "param1": "value1",
        "param2": 123,
        "param3": true
      },
      "_res_": "expectedResult"
    }
  ]
}
```

### 3. JSON Structure

#### Root Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `tableName` | string | Yes | The name of the executable table/method to test |
| `testTableName` | string | No | The technical name for the test table. If not provided, defaults to `{tableName}Test` |
| `appendToExisting` | boolean | No | If true, appends to existing test table (currently not supported - will throw error). Default: false |
| `testCases` | array | Yes | Array of test case objects |

#### Test Case Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `_id_` | string | No | Unique identifier for the test case. Auto-generated if not provided |
| `_description_` | string | No | Human-readable description of the test case |
| `parameters` | object | Yes | Map of parameter names to values. Parameter names must match the method signature |
| `_res_` | any | Yes | Expected result of the test execution |
| `_error_` | string | No | Expected error message (for testing error conditions) |
| `_context_` | object | No | Runtime context properties (advanced usage) |

### 4. Parameter Types

Parameters in the `parameters` object will be automatically converted to the appropriate types:

- **Strings**: `"text"`
- **Numbers**: `123` or `45.67`
- **Booleans**: `true` or `false`
- **Null**: `null`
- **Complex objects**: JSON objects will be converted to the appropriate Java types

### 5. Upload the File

1. Click "Select JSON File" and choose your JSON file
2. Click "Import Test Cases"
3. Wait for the success message
4. **Important**: Save your project to persist the changes

## Examples

### Example 1: Simple Test Cases

```json
{
  "tableName": "add",
  "testCases": [
    {
      "parameters": {"a": 1, "b": 2},
      "_res_": 3
    },
    {
      "parameters": {"a": 10, "b": 20},
      "_res_": 30
    }
  ]
}
```

### Example 2: Test Cases with Descriptions and IDs

```json
{
  "tableName": "calculateDiscount",
  "testTableName": "discountTests",
  "testCases": [
    {
      "_id_": "DISCOUNT_001",
      "_description_": "10% discount for premium customers",
      "parameters": {
        "customerType": "PREMIUM",
        "purchaseAmount": 100.0
      },
      "_res_": 10.0
    },
    {
      "_id_": "DISCOUNT_002",
      "_description_": "No discount for standard customers below threshold",
      "parameters": {
        "customerType": "STANDARD",
        "purchaseAmount": 50.0
      },
      "_res_": 0.0
    }
  ]
}
```

### Example 3: Testing Error Conditions

```json
{
  "tableName": "divide",
  "testCases": [
    {
      "_id_": "DIV_001",
      "_description_": "Normal division",
      "parameters": {"a": 10, "b": 2},
      "_res_": 5
    },
    {
      "_id_": "DIV_002",
      "_description_": "Division by zero should fail",
      "parameters": {"a": 10, "b": 0},
      "_error_": "Division by zero"
    }
  ]
}
```

## REST API

### Endpoint

```
POST /test/import/json
```

### Request

- **Content-Type**: `multipart/form-data`
- **Parameter**: `file` (the JSON file)

### Response

Success (200):
```json
{
  "success": true,
  "message": "Successfully imported 4 test cases to table calculateDiscount",
  "tableUri": "sheet1!A1:F5"
}
```

Error (400/500):
```json
{
  "success": false,
  "message": "Error description",
  "tableUri": null
}
```

## Limitations

- The target table must be an executable table (decision table, method table, etc.)
- The target table must exist in the current project
- All parameter names in the JSON must match the method signature
- Appending to existing test tables is not currently supported
- Complex types (custom objects) must be provided as JSON objects with matching field names

## Troubleshooting

### "Table not found"
- Ensure the `tableName` exactly matches the method name in your project
- Check that the table is executable (not a data table or property table)

### "Parameter mismatch"
- Verify that all required parameters are present in the `parameters` object
- Check that parameter names match exactly (case-sensitive)

### "Invalid JSON"
- Validate your JSON using a JSON validator
- Check for trailing commas, missing quotes, or other syntax errors

## Implementation Details

### Files Created

- `TestCaseData.java` - Model for individual test case
- `TestCasesImportRequest.java` - Model for import request
- `TestCasesJsonImportService.java` - Service handling JSON parsing and test table creation
- `TestCasesJsonImportController.java` - REST controller
- `importTestCasesFromJson.html` - Web UI for file upload
- `sample-test-cases.json` - Example JSON file
