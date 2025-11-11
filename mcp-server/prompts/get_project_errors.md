# Analyzing and Fixing Project Errors

Guide AI assistants to understand, categorize, and fix project errors efficiently.

## Quick Error Analysis Workflow

```
1. Get errors
   get_project_errors(projectId) → See all errors with details

2. Categorize
   Group by type: type errors, syntax errors, reference errors

3. Fix patterns
   Apply common fixes for each error type

4. Validate
   validate_project(projectId) → Confirm fixes worked

5. Test
   run_all_tests(projectId) → Ensure nothing broke
```

## Common Error Types & Quick Fixes

### 1. Type Mismatch Errors

**Pattern**: `Cannot convert String to Integer` or `Type mismatch: expected X, got Y`

**Common Causes**:
- Column data type doesn't match rule signature
- Input data format incorrect
- Missing type conversion

**Quick Fixes**:
```
Option 1: Add type conversion
  Integer.parseInt(stringValue)
  Double.parseDouble(stringValue)

Option 2: Change column type
  Update table to match expected type

Option 3: Fix rule signature
  Change parameter type to match data
```

**Example**:
```
Error: Cannot convert "30" (String) to int
Fix: Integer.parseInt(age) or change column to int type
```

---

### 2. Reference Errors

**Pattern**: `Datatype 'Customer' not found` or `Table 'RuleName' not found`

**Common Causes**:
- Referenced datatype/table doesn't exist
- Typo in name (case-sensitive)
- Table in different module not imported

**Quick Fixes**:
```
Step 1: Check if exists
  list_tables(projectId, tableType: "Datatype", name: "Customer")

Step 2: If missing, create it
  create_rule(projectId, name: "Customer", tableType: "Datatype", ...)

Step 3: If exists, fix spelling
  Check exact name (case-sensitive: "Customer" ≠ "customer")
```

**Example**:
```
Error: Datatype 'Policy' not found
Fix 1: Create Policy datatype if missing
Fix 2: Correct spelling if typo ("Polcy" → "Policy")
```

---

### 3. Syntax Errors

**Pattern**: `Unexpected token` or `Invalid syntax at line X`

**Common Causes**:
- Excel formula syntax error
- Invalid OpenL syntax
- Missing/unbalanced brackets
- Special characters in wrong places

**Quick Fixes**:
```
Check:
- Balanced parentheses: ( )
- Balanced brackets: [ ]
- Balanced braces: { }
- Valid Excel formula syntax
- OpenL Tablets syntax rules
```

**Example**:
```
Error: Unexpected token at line 15
Common issues:
- Missing closing parenthesis
- Extra bracket
- Invalid Excel formula: =SUM(A1:A10 (missing )
```

---

### 4. Circular Reference Errors

**Pattern**: `Circular dependency detected: RuleA → RuleB → RuleA`

**Common Causes**:
- Rule calls itself indirectly
- Two rules call each other
- Dependency cycle in datatypes

**Quick Fixes**:
```
Step 1: Identify cycle
  Review dependency chain shown in error

Step 2: Break cycle
  Option A: Extract shared logic to new rule
  Option B: Use Method table for shared functionality
  Option C: Redesign to remove circular dependency
```

**Example**:
```
Error: calculatePremium → getRiskFactor → calculatePremium
Fix: Extract risk calculation to separate rule: determineRisk()
```

---

### 5. Validation Errors

**Pattern**: `Rule validation failed` or `Invalid rule configuration`

**Common Causes**:
- Business logic errors
- Invalid dimension properties
- Missing required configuration
- Conflicting rules

**Quick Fixes**:
```
Step 1: Review error details
  Check specific validation message

Step 2: Fix configuration
  Correct dimension properties
  Fix rule logic
  Remove conflicts
```

---

## Error Resolution Strategies

### Strategy 1: Fix by Category (Fastest)

```
1. Group errors by type
2. Fix all type errors together (same pattern)
3. Fix all reference errors together
4. Fix all syntax errors together
5. Validate after each category
```

### Strategy 2: Fix by Severity (Safest)

```
1. Fix blocking errors first (prevent save/deploy)
2. Fix warnings next (improve quality)
3. Address info messages last (optimizations)
```

### Strategy 3: Fix by File (Organized)

```
1. Group errors by file/table
2. Fix all errors in one file
3. Move to next file
4. Validate after each file
```

## Error-Free Workflow

```
1. Get all errors
   get_project_errors(projectId)

2. Analyze and categorize
   Type errors: X
   Reference errors: Y
   Syntax errors: Z

3. Apply fixes
   Fix each error using patterns above

4. Validate fixes
   validate_project(projectId) → Should return success

5. Run tests
   run_all_tests(projectId) → Ensure all passing

6. Save
   save_project(projectId)
```

## Troubleshooting Complex Errors

### Multiple Related Errors

If one error causes cascading errors:
1. Fix the root cause first
2. Re-validate to see if others disappear
3. Fix remaining errors

### Persistent Errors After Fix

If error persists after fix:
1. Verify fix was applied correctly
2. Check if error message changed
3. Review related code
4. Try alternative fix approach

### Unclear Error Messages

If error message is unclear:
1. Check file and line number
2. Review surrounding code context
3. Look for recent changes in that area
4. Compare to similar working code

## Prevention Best Practices

**DO**:
- Validate frequently during development
- Fix errors immediately when they appear
- Test after every significant change
- Use consistent naming conventions
- Document complex logic

**DON'T**:
- Accumulate errors (fix as you go)
- Skip validation before saving
- Ignore warnings (they become errors)
- Deploy with any errors present
- Guess at fixes (understand the error first)

## Quick Reference: Error → Fix Mapping

| Error Type | Quick Fix Tool/Action |
|------------|----------------------|
| Type mismatch | Add type conversion or change column type |
| Datatype not found | `create_rule` with `tableType: "Datatype"` |
| Table not found | Check spelling or create missing table |
| Syntax error | Review Excel formula syntax, balance brackets |
| Circular dependency | Extract shared logic to new rule |
| Missing parameter | Add parameter to rule signature |
| Invalid property | Check dimension properties values |
| Compilation error | Review code syntax, fix Java-like errors |

## After Fixing All Errors

```
Final Checklist:
✓ validate_project → Success (0 errors)
✓ run_all_tests → All passing
✓ save_project → Create commit
✓ Ready for deployment
```
