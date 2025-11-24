## Summary

**Execute rules for quick validation**: Use openl_execute_rule() to test individual rules with specific input data. Construct inputData as JSON matching rule parameters (simple types as literals, custom Datatypes as nested objects with all required fields).

# OpenL Rule Execution

{if ruleName}
## Executing Rule: `{ruleName}`
{end if}
{if projectId}

**Project**: {projectId}
{end if}

WHEN to execute:
- After creating/modifying {if ruleName}{ruleName}{end if} OpenL table → Verify behavior
- Before saving → Quick validation
- During debugging → Understand OpenL table logic

## OpenL Test Data Construction

### Simple Types
Direct JSON values:
```json
{"driverType": "SAFE", "age": 30, "premium": 1000.50}
```

### Custom Datatypes (OpenL Datatype Tables)

STEP 1: Find OpenL datatype structure
```text
openl_list_tables(tableType="Datatype", name="Policy")
openl_get_table(tableId="Policy")
```

STEP 2: Build JSON matching OpenL Datatype structure
```json
{
  "policy": {"type": "Auto", "state": "CA", "effectiveDate": "01/01/2025"},
  "driver": {"name": "John Doe", "age": 30}
}
```

### SpreadsheetResult (OpenL Spreadsheets)

IF returnType = `SpreadsheetResult` → Returns entire calculation matrix
IF returnType = specific type (int, double, etc.) → Returns final cell value

### Dates (OpenL Date Format)
- `"MM/DD/YYYY"` (e.g., "01/01/2025")
- `"YYYY-MM-DD"` (e.g., "2025-01-01")

### Arrays
```json
{
  "vehicles": [
    {"make": "Toyota", "model": "Camry"},
    {"make": "Honda", "model": "Accord"}
  ]
}
```

## Error Handling (OpenL-Specific)

- **"Parameter type mismatch"** → Check OpenL rule signature types
- **"Datatype not found"** → `openl_list_tables(tableType="Datatype")` to find available datatypes
- **"Missing parameter"** → Provide all required parameters from OpenL rule header

## Workflow

1. `openl_get_table({if ruleName}tableId="{ruleName}"{end if})` → See OpenL table signature (returnType, parameters)
2. Build JSON matching OpenL parameter types
3. `openl_execute_rule({if ruleName}ruleName="{ruleName}", {end if}inputData: {...})`
4. Review output (matches OpenL returnType?)
