# Executing Rules for Testing

Guide AI assistants to construct test data and execute rules to verify behavior.

## When to Execute Rules

Use `execute_rule` tool in these scenarios:
- **After creating a new rule** - Verify it works as expected
- **After modifying a rule** - Ensure changes work correctly
- **Before saving** - Quick validation of logic
- **During debugging** - Understand rule behavior
- **Creating test data** - See what inputs produce what outputs

## Quick Execution Workflow

```
1. Get rule signature
   get_table(projectId, tableId) → See parameters and return type

2. Construct test data
   Build JSON with values for all parameters

3. Execute rule
   execute_rule(projectId, ruleName, parameters: {...})

4. Review output
   Check if result matches expectations

5. Iterate if needed
   Adjust rule or test data and re-execute
```

## Test Data Construction Patterns

### Simple Types (String, int, double, boolean)

**Format**: Direct values in JSON
```json
{
  "driverType": "SAFE",
  "age": 30,
  "premium": 1000.50,
  "eligible": true
}
```

### Custom Datatypes (Objects)

**Format**: JSON object matching datatype structure
```json
{
  "customer": {
    "name": "John Doe",
    "age": 30,
    "state": "CA"
  },
  "policy": {
    "type": "Auto",
    "effectiveDate": "01/01/2025"
  }
}
```

**Steps**:
1. Use `list_tables` with filter `tableType=Datatype` to find datatype definition
2. Use `get_table` to see datatype structure
3. Build JSON matching all required fields

### Arrays

**Format**: JSON array with elements of specified type
```json
{
  "drivers": [
    {"name": "John", "age": 30},
    {"name": "Jane", "age": 28}
  ],
  "states": ["CA", "NY", "TX"]
}
```

### Dates

**Format**: String in MM/DD/YYYY or YYYY-MM-DD format
```json
{
  "effectiveDate": "01/01/2025",
  "expirationDate": "2025-12-31"
}
```

## Test Data Templates

### Template 1: Typical Case (Use This First)

Start with typical, valid values that should work:
```json
{
  "paramName1": "typical value",
  "paramName2": 100,
  "paramName3": true
}
```

**Expected**: Normal, successful execution

### Template 2: Boundary Values

Test edge cases:
```json
{
  "age": 18,          // Minimum
  "amount": 0,        // Zero
  "percentage": 1.0   // Maximum
}
```

**Expected**: Should still work, might return edge case results

### Template 3: Invalid Data (Error Testing)

Test with invalid inputs:
```json
{
  "age": -5,          // Negative (invalid)
  "state": "XX",      // Invalid state
  "amount": null      // Null value
}
```

**Expected**: Validation error or rule handles gracefully

## Common Execution Scenarios

### Scenario 1: Test Decision Table

**Rule**: `calculateDiscount(String tier, double amount)`

**Test Data**:
```json
{
  "tier": "GOLD",
  "amount": 1500.00
}
```

**Execute**:
```
execute_rule(
  projectId: "design-Pricing",
  ruleName: "calculateDiscount",
  parameters: {"tier": "GOLD", "amount": 1500.00}
)
```

**Expected Output**: Discount value (e.g., 0.15 for 15%)

### Scenario 2: Test Spreadsheet Calculation

**Rule**: `calculatePremium(int baseAmount, String riskLevel)`

**Test Data**:
```json
{
  "baseAmount": 1000,
  "riskLevel": "HIGH"
}
```

**Execute**:
```
execute_rule(
  projectId: "design-Insurance",
  ruleName: "calculatePremium",
  parameters: {"baseAmount": 1000, "riskLevel": "HIGH"}
)
```

**Expected Output**: If return type is `SpreadsheetResult`, returns full calculation matrix. If return type is `int`, returns final premium value.

### Scenario 3: Test Rule with Custom Datatype

**Rule**: `validatePolicy(Policy policy, Driver driver)`

**Get Datatype Structure First**:
```
list_tables(projectId, tableType: "Datatype", name: "Policy")
get_table(projectId, tableId: "Policy")
```

**Build Test Data** matching datatype:
```json
{
  "policy": {
    "type": "Auto",
    "state": "CA",
    "effectiveDate": "01/01/2025"
  },
  "driver": {
    "name": "John Doe",
    "age": 30,
    "licenseNumber": "D1234567"
  }
}
```

**Execute**:
```
execute_rule(
  projectId: "design-Underwriting",
  ruleName: "validatePolicy",
  parameters: {...}
)
```

**Expected Output**: Boolean (true/false) or validation result object

## Return Type Interpretation

**Simple types** (int, double, String, boolean):
- Rule returns single value directly

**SpreadsheetResult**:
- Returns entire calculation matrix
- All intermediate values accessible
- Use to see calculation breakdown

**Custom datatypes**:
- Returns object matching datatype structure
- Check all fields in result

**Arrays**:
- Returns array of elements
- May be empty if no matches found

## Troubleshooting Execution

### Error: "Parameter type mismatch"
- Check parameter types in rule signature
- Ensure JSON values match expected types
- Convert strings to numbers if needed

### Error: "Datatype not found"
- Use `list_tables` to find datatype
- Check spelling (case-sensitive)
- Ensure datatype exists in project

### Error: "Missing required parameter"
- Check rule signature for all required parameters
- Provide values for all parameters
- Use null for optional parameters if needed

### Unexpected Output
- Review rule logic with `get_table`
- Check test data values
- Execute with different test data to isolate issue
- Compare to existing test tables

## Best Practices

**DO**:
- Start with typical test data
- Test edge cases and boundaries
- Execute after every rule change
- Use execution results to create test tables
- Iterate quickly during development

**DON'T**:
- Skip execution before saving
- Use only one test case
- Assume rule works without testing
- Deploy without executing rules first

## Integration with Testing

**Quick Test Cycle**:
```
1. Modify rule → update_table()
2. Execute rule → execute_rule() with test data
3. If passing → Create test table
4. Run tests → run_test()
5. Save → save_project()
```

**Creating Test Tables from Execution**:
1. Execute rule with various inputs
2. Note inputs that work correctly
3. Create Test table with those inputs as test cases
4. Set expected outputs based on execution results
5. Run test to validate
