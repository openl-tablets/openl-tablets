# OpenL Error Analysis

## Workflow
1. `get_project_errors()` → See all OpenL validation errors
2. Categorize by OpenL error pattern
3. Apply OpenL-specific fix
4. `validate_project()` → Confirm (0 errors)

## OpenL Error Patterns → Fixes

### Type Mismatch (OpenL Type System)
**Pattern**: "Cannot convert String to Integer" in OpenL rule
**Fix**: Add OpenL type conversion OR change column type in Excel OR fix rule signature

### Reference Not Found (OpenL Tables)
**Pattern**: "Datatype 'Customer' not found" OR "Table 'RuleName' not found"
**Fix**:
1. `list_tables(tableType="Datatype")` → Check if exists
2. IF missing → create OpenL Datatype table
3. IF exists → fix spelling (OpenL is case-sensitive)

### Syntax Error (OpenL BEX Grammar)
**Pattern**: "Unexpected token" in OpenL expression
**Fix**: Check OpenL Business Expression (BEX) syntax, balance brackets, verify Excel formula syntax

### Circular Dependency (OpenL Table Calls)
**Pattern**: "Circular dependency: RuleA → RuleB → RuleA"
**Fix**: Extract shared logic to new OpenL table

### Decision Table Validation (OpenL validateDT Property)
**Pattern**: "Gaps or overlaps in decision table"
**Fix**:
- Check `validateDT` property setting
- Fill gaps in decision table conditions
- Resolve overlapping conditions

### Property Overlap (OpenL Dimension Properties)
**Pattern**: "Ambiguous method dispatch" with dimension properties
**Fix**: Resolve "bad overlap" in Business Dimension properties (state, lob, effectiveDate, etc.)

## Fix Strategy
IF <10 errors → Fix by OpenL error category
IF 10-50 errors → Fix by Excel file
IF >50 errors → Fix root cause first (may cascade fix others)

## BEFORE save/deploy
- `validate_project()` → MUST be 0 errors
- `run_test(runAllTests: true)` → MUST pass
