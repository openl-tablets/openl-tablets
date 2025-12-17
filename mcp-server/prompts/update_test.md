---
name: update_test
description: Guide for modifying existing tests, adding test cases, and updating expected values
arguments:
  - name: testId
    description: ID of the test table to update
    required: false
  - name: tableName
    description: Name of the table being tested
    required: false
---

## Summary

**Test updates preserve structure**: Use openl_get_table() to fetch current structure, modify rows (add/update/remove test cases or fix _res_/_error_ values), then openl_update_table() with FULL view. Always run tests after updates to verify.

# Updating Test Tables

{if testId}
## Updating Test: **{testId}**
{end if}
{if tableName}

**Testing Table**: {tableName}
{end if}

## When to Update

- Test fails → Fix expected values (_res_ or _error_){if tableName} for {tableName}{end if}
- Add test cases → Cover new scenarios
- Rule modified → Add edge cases
- Remove obsolete tests → Clean up outdated cases

## Test Table View Structure

Test table view mirrors Excel structure in JSON:

```json
{
  "header": {
    "keyword": "Test",
    "testedMethod": "calculatePremium",
    "testTableName": "calculatePremiumTest"
  },
  "columns": [
    { "name": "driverType", "type": "String" },
    { "name": "age", "type": "int" },
    { "name": "_res_", "type": "double" }
  ],
  "rows": [
    { "driverType": "SAFE", "age": 25, "_res_": 1000.0 },
    { "driverType": "RISKY", "age": 45, "_res_": 1500.0 }
  ]
}
```

## Update Decision Logic

### WHEN Adding Test Cases

```text
1. openl_get_table(tableId=testTableId) → Get current structure
2. Preserve columns (parameter order matters)
3. Add new rows with test data
4. openl_update_table(view={...existingRows, ...newRows})
```

**Example:** Add boundary test
```json
{
  "rows": [
    ...existingRows,
    { "driverType": "SAFE", "age": 16, "_res_": 1200.0 },  // Min age
    { "driverType": "SAFE", "age": 80, "_res_": 800.0 }    // Max age
  ]
}
```

### WHEN Fixing Expected Values

```text
1. Identify failing test case (row index)
2. Update _res_ or _error_ value
3. update_table with corrected row
```

**Example:** Fix expected result
```json
{
  "rows": [
    { "driverType": "SAFE", "age": 25, "_res_": 1100.0 }  // Was 1000.0
  ]
}
```

### WHEN Removing Test Cases

```text
1. get_table → Get all rows
2. Filter out obsolete rows
3. update_table with filtered rows
```

### WHEN Adding Columns (Rare)

⚠️ **Caution:** Adding columns changes test structure

**Valid cases:**
- Add `_context_.*` column → Runtime context testing
- Add `_description_` column → Test documentation

**Invalid cases:**
- Change parameter columns → Breaks test (must match tested method)
- Add both `_res_` and `_error_` → Ambiguous (violates OpenL rules)

## Update Patterns

### Pattern 1: Add Success Test Case

```text
Original test:
Row: { param1: "A", param2: 10, _res_: 100 }

Add test case:
Row: { param1: "B", param2: 20, _res_: 200 }
```

### Pattern 2: Add Error Test Case

```text
IF test table has _error_ column:
Row: { param1: "INVALID", _error_: "Invalid parameter value" }

IF test table has _res_ column only:
→ Create separate error test table with _error_ column
```

### Pattern 3: Update Complex Datatype

```text
Test with Datatype parameter:
Row: { "policy.type": "Auto", "policy.state": "CA", "_res_": 100 }

Update state:
Row: { "policy.type": "Auto", "policy.state": "TX", "_res_": 90 }
```

### Pattern 4: Update Array Test Data

```text
Test with array reference:
Row: { "drivers": "> DriversDataTable", "_res_": 5000 }

Change reference:
Row: { "drivers": "> UpdatedDriversDataTable", "_res_": 5500 }
```

### Pattern 5: Add Context

```text
Original (no context):
Columns: param1, param2, _res_

Add context:
Columns: param1, param2, _context_.userId, _res_
Rows: { param1: "A", param2: 10, _context_.userId: "user123", _res_: 100 }
```

## Preserving Test Structure

### MUST Preserve

✅ Column order (parameters match tested method signature)
✅ Column types (int, String, double, etc.)
✅ Special column names (_res_, _error_, _context_.*, _description_)
✅ Test table name (Test <method> <testName>)

### CAN Modify

✅ Row data (test case values)
✅ Number of rows (add/remove test cases)
✅ Expected results (_res_ values)
✅ Expected errors (_error_ values)

### CANNOT Modify

❌ Parameter column names (must match method signature)
❌ Parameter column order (positional matching)
❌ Add _error_ to table with _res_ (ambiguous)
❌ Add _res_ to table with _error_ (ambiguous)

## Integration with update_table Tool

Use existing `update_table` tool:

```json
{
  "name": "update_table",
  "arguments": {
    "projectId": "design-insurance-rules",
    "tableId": "Test_calculatePremium_1234",
    "view": {
      "rows": [
        { "driverType": "SAFE", "age": 25, "_res_": 1000.0 },
        { "driverType": "RISKY", "age": 45, "_res_": 1500.0 },
        { "driverType": "SAFE", "age": 16, "_res_": 1200.0 }
      ]
    },
    "comment": "Added boundary test case for minimum age"
  }
}
```

## Validation After Update

AFTER updating test table:

```text
1. openl_start_project_tests(projectId, { tableId: testedRuleId }) → Start tests for the rule
2. openl_get_project_test_results(projectId, { waitForCompletion: true }) → Get test results
2. IF all pass → Proceed
3. IF fail → Review test data (type mismatch, wrong expected value)
4. openl_update_project_status() → Persist changes (only after tests pass)
```

## Common Update Scenarios

### Scenario 1: Test Fails After Rule Change

```text
PROBLEM: Rule logic changed, test expects old behavior

SOLUTION:
1. openl_get_table(testTableId) → Review test cases
2. Identify outdated expected values
3. Update _res_ to match new rule behavior
4. run_test → Verify pass
```

### Scenario 2: Add Negative Test Cases

```text
PROBLEM: Need to test error conditions

SOLUTION:
IF test table has _res_:
→ Create NEW test table with _error_ column (cannot mix)

IF test table has _error_:
→ Add error test rows to existing table
```

### Scenario 3: Precision Issues (Floating Point)

```text
PROBLEM: Test fails with: Expected 100.123, Actual 100.124

SOLUTION:
1. openl_get_table(testTableId) → Check precision property
2. IF no precision → Add table property: { precision: 2 }
3. OpenL uses delta: |expected - actual| < 10^(-precision)
```

### Scenario 4: Test Data Too Large

```text
PROBLEM: Many test cases, Excel table too long

SOLUTION:
1. Split into multiple test tables by scenario:
   - calculatePremiumTest_Success
   - calculatePremiumTest_EdgeCases
   - calculatePremiumTest_Boundaries
2. Use Data tables for large datasets:
   - Reference: > TestDataTable
```

## Foreign Keys in Test Updates

### Update Data Table Reference

```text
Original:
Row: { "customer": "> CustomersDataTable", "_res_": 100 }

Update reference:
Row: { "customer": "> UpdatedCustomersDataTable", "_res_": 150 }
```

### Update Nested Reference

```text
Original:
Row: { "policy.vehicle": "> Policies.vehicles", "_res_": 1000 }

Update:
Row: { "policy.vehicle": "> UpdatedPolicies.vehicles", "_res_": 1100 }
```

## Integration Context

- **Simple updates** (add/modify few rows) → Tool description sufficient
- **Complex updates** (structure changes, datatypes, arrays) → This prompt guides AI
- **Validation failures** → This prompt clarifies preservation rules
- **User overrides** → Always allowed

## Quick Reference

| Operation | Command |
|-----------|---------|
| Add row | `openl_update_table(view={rows: [...existing, newRow]})` |
| Modify row | `openl_update_table(view={rows: [modifiedRow]})` |
| Remove row | `openl_update_table(view={rows: filtered})` |
| Fix expected value | Update `_res_` or `_error_` in row |
| Add context | Add column `_context_.property` |
| Add description | Add column `_description_` |
| Verify changes | `openl_start_project_tests(projectId, { tableId: ruleId })` then `openl_get_project_test_results(projectId, { waitForCompletion: true })` |
| Persist | `openl_update_project_status()` (after tests pass) |
