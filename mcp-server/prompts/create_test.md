---
name: create_test
description: Guide for creating OpenL test tables with proper structure and validation
arguments:
  - name: tableName
    description: Name of the table being tested
    required: false
  - name: tableType
    description: Type of table (Rules, SimpleRules, Spreadsheet, etc.)
    required: false
---

## Summary

**Test tables mirror method signatures**: Create with columns matching tested table parameters plus _res_ (expected result) or _error_ (expected error). Minimum 3 rows required (header, column definitions, at least 1 test case). Run tests immediately after creation to verify structure.

# Creating Test Tables in OpenL Tablets

{if tableName}
## Creating Test for: **{tableName}**

You're creating a test table for the `{tableName}` rule.

{end if}
{if tableType}
### Table Type: {tableType}

Since you're testing a **{tableType}** table, pay attention to:
- Parameter matching strategy specific to {tableType}
- Expected return type and structure

{end if}
## When to Create Test Tables

- After creating/modifying {if tableName}{tableName}{end if} → Validate rule behavior
- Before save/deploy → MANDATORY testing (catches errors early)
- Regression testing → Prevent bugs from reappearing
- Edge case testing → Validate boundary conditions
- Integration testing → Test rules calling other rules

## Test Table Structure (3-row minimum)

OpenL Test tables have this exact structure:

```text
Row 1: Test <methodName> <testTableName>
Row 2: param1    param2    ...    _res_    [_error_]    [_context_.*]    [_description_]
Row 3: Display1  Display2  ...    Result   [Error Msg]  [Context]        [Description]
Row 4+: Test data rows with expected values
```

## Special Column Headers (OpenL Reserved)

OpenL defines 4 special column names:

| Column | Purpose | Required | Notes |
|--------|---------|----------|-------|
| `_res_` | Expected result | YES (for success tests) | Must match method return type |
| `_error_` | Expected error message | YES (for error tests) | Use for exception testing |
| `_context_.*` | Runtime context values | NO | Format: `_context_.property` (e.g., `_context_.userId`) |
| `_description_` | Test case description | NO | Human-readable test documentation |

⚠️ **CRITICAL:** NEVER use both `_res_` AND `_error_` in same test table (ambiguous expectation)

## Decision Tree for Test Creation

### STEP 1: Identify Tested Method

```
openl_list_tables(tableType="Rules|SimpleRules|SmartRules|Spreadsheet|Method")
→ Find rule to test

openl_get_table(tableId="ruleName_1234")
→ Get signature: name, parameters, returnType
```

### STEP 2: Match Parameters (Left-to-Right)

Test table parameter columns MUST match rule signature EXACTLY:

- **Column order matters** (positional matching)
- **Type must match** (String, int, double, Policy, etc.)
- **Name should match** (for clarity)

**Example:**
```text
Rule: Rules double calculatePremium(String driverType, int age)

Test columns: driverType (String), age (int), _res_ (double)
```

### STEP 3: Select Expected Outcome Column

**IF testing success scenarios:**
```text
Add _res_ column with expected return values
Type: Must match method returnType
```

**IF testing error scenarios:**
```text
Add _error_ column with expected exception messages
Type: String (error message text)
```

**Never both in same table** (create separate test tables for errors)

### STEP 4: Add Optional Columns (If Needed)

**Context columns** (for runtime environment):
```text
_context_.userId       → Runtime user ID
_context_.currentDate  → Runtime date context
_context_.locale       → Runtime locale setting
```

**Description column** (for documentation):
```text
_description_ → Human-readable test case name
```

## Example Patterns

### 1. Simple Type Test (Primitives)

```text
Test calculatePremium calculatePremiumTest

driverType    age    _res_
Driver Type   Age    Premium
"SAFE"        25     1000.0
"RISKY"       45     1500.0
"SAFE"        65     900.0
```

**create_rule parameters:**
```json
{
  "tableType": "Test",
  "name": "calculatePremiumTest",
  "parameters": [
    { "type": "String", "name": "driverType" },
    { "type": "int", "name": "age" },
    { "type": "double", "name": "_res_" }
  ]
}
```

### 2. Complex Datatype Test (OpenL Datatypes)

```text
Test processPolicy processPolicyTest

policy.type    policy.state    policy.premium    _res_
Policy Type    State          Premium           Tax
"Auto"         "CA"           1000.0            100.0
"Home"         "TX"           2000.0            150.0
```

**Use object notation** for Datatype table fields: `objectName.fieldName`

### 3. Error Testing (Exception Validation)

```text
Test validateAge validateAgeTest

age    _error_
Age    Expected Error
-5     "Age must be positive"
150    "Age exceeds maximum allowed value"
null   "Age is required"
```

**create_rule parameters:**
```json
{
  "tableType": "Test",
  "name": "validateAgeTest",
  "parameters": [
    { "type": "int", "name": "age" },
    { "type": "String", "name": "_error_" }
  ]
}
```

### 4. Context Testing (Runtime Context)

```text
Test applyDiscount applyDiscountTest

amount    _context_.userLevel    _description_           _res_
Amount    User Level             Test Description        Discount
100.0     "GOLD"                 "Gold member discount"  0.15
50.0      "SILVER"               "Silver member disc"    0.10
200.0     "BRONZE"               "Bronze member disc"    0.05
```

### 5. Array/Collection Testing

**For array parameters, use Data table reference:**

```text
Test calculateTotalPremium calculateTotalPremiumTest

drivers    _res_
Drivers    Total Premium
> DriversDataTable    5000.0
```

**Foreign key syntax:** `> DataTableName [columnName]`

### 6. SpreadsheetResult Testing

```text
Test calculateBreakdown calculateBreakdownTest

baseAmount    riskLevel    _res_.Premium    _res_.Tax
Base          Risk         Premium Result   Tax Result
1000          "HIGH"       1500.0           150.0
500           "LOW"        600.0            60.0
```

**Use `_res_.fieldName`** to access SpreadsheetResult fields

## Integration with create_rule Tool

Use the existing `create_rule` tool with `tableType="Test"`:

```json
{
  "name": "create_rule",
  "arguments": {
    "projectId": "design-insurance-rules",
    "name": "calculatePremiumTest",
    "tableType": "Test",
    "parameters": [
      { "type": "String", "name": "driverType" },
      { "type": "int", "name": "age" },
      { "type": "double", "name": "_res_" }
    ],
    "comment": "Created test for calculatePremium rule"
  }
}
```

## Complete Workflow

### BEFORE Creating Test

1. Find rule to test:
```text
openl_list_tables(projectId, tableType="Rules")
```

2. Get rule signature:
```text
openl_get_table(projectId, tableId="calculatePremium_1234")
→ Returns: { signature: { name, parameters, returnType } }
```

### CREATE Test Table

3. Build parameter list:
```javascript
testParameters = [
  ...ruleParameters,  // Match rule signature
  { type: returnType, name: "_res_" }  // Add expected result
]
```

4. Create test table:
```text
openl_create_rule(
  projectId,
  name: ruleName + "Test",
  tableType: "Test",
  parameters: testParameters
)
```

### AFTER Creating Test

5. Add test cases:
```text
openl_update_table(
  projectId,
  tableId: testTableId,
  view: { rows: [
    { driverType: "SAFE", age: 25, _res_: 1000 },
    { driverType: "RISKY", age: 45, _res_: 1500 }
  ]}
)
```

6. Run tests to verify:
```text
openl_test_project(projectId, tableIds: [ruleTableId])
→ Should show all tests passing
```

7. Save project:
```text
openl_update_project_status(projectId, comment: "Added tests for calculatePremium")
```

## Common Mistakes to Avoid

❌ **Wrong:** Mismatch parameter order
```text
Rule: calculatePremium(String type, int age)
Test: age, type, _res_  // WRONG ORDER
```

✅ **Correct:** Match exact parameter order
```text
Test: type, age, _res_  // MATCHES RULE
```

❌ **Wrong:** Both _res_ and _error_ in same table
```text
Test: param1, _res_, _error_  // AMBIGUOUS
```

✅ **Correct:** Separate test tables
```text
SuccessTest: param1, _res_
ErrorTest: param1, _error_
```

❌ **Wrong:** Type mismatch
```text
Rule returns: double
Test _res_: int  // TYPE MISMATCH
```

✅ **Correct:** Exact type match
```text
Rule returns: double
Test _res_: double  // MATCHES
```

## Precision for Floating-Point Comparisons

For double/float comparisons, add precision property:

```json
{
  "name": "create_rule",
  "arguments": {
    "tableType": "Test",
    "properties": {
      "precision": 2  // 2 decimal places tolerance
    }
  }
}
```

OpenL uses delta comparison: `Math.abs(expected - actual) < 10^(-precision)`

## Integration Context

- **Simple test creation** (1-2 parameters) → Tool description sufficient
- **Complex test creation** (datatypes, arrays, context) → This prompt guides AI
- **Error testing** → This prompt clarifies _error_ column usage
- **User overrides** → Always allowed
