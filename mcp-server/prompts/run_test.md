# Smart Test Selection and Execution

Guide AI assistants to intelligently select and run tests based on rule changes.

## Quick Test Selection Logic

**After modifying rules, choose test scope:**

- **Modified 1 rule** → Run tests for that specific rule (fastest)
- **Modified 2-5 rules** → Run tests for affected module (thorough)
- **Modified 6+ rules** → Run all tests (comprehensive)
- **Before save/deploy** → Always run all tests (mandatory)

## Test Selection Workflow

```
1. Identify changed rules
   Know which tables were modified

2. Find related tests
   Tests named "Test_RuleName" are directly related
   Tests in same module test integration

3. Select test scope
   Use decision logic above

4. Run tests
   run_test() with appropriate selection

5. Review results
   All passing → Proceed with save
   Some failing → Fix issues and re-test
```

## Run Test Tool Usage

### Option 1: Run Tests for Specific Tables (Targeted)

**Use when**: Modified 1-3 specific rules, want fast feedback

```
run_test(
  projectId: "design-Insurance",
  tableNames: ["Test_calculatePremium", "Test_validatePolicy"]
)
```

**Benefits**: Fast execution, focused results

### Option 2: Run All Tests (Comprehensive)

**Use when**: Before saving, before deploying, modified many rules

```
run_test(
  projectId: "design-Insurance",
  runAllTests: true
)
```

**Benefits**: Complete validation, catches integration issues

### Option 3: Run Tests by ID (Precise)

**Use when**: Want to run specific test cases by their IDs

```
run_test(
  projectId: "design-Insurance",
  testIds: ["test-001", "test-002", "test-003"]
)
```

**Benefits**: Most precise control

## Common Testing Scenarios

### Scenario 1: After Modifying a Single Rule

```
1. Modified rule: calculateDiscount
2. Find related test: Test_calculateDiscount
3. Run targeted test:
   run_test(projectId, tableNames: ["Test_calculateDiscount"])
4. If passing → Save
5. If failing → Fix rule and re-test
```

### Scenario 2: After Creating New Rule

```
1. Created rule: validateClaim
2. Execute rule first to test behavior:
   execute_rule(projectId, "validateClaim", testData)
3. Create test table: Test_validateClaim
4. Run test to verify:
   run_test(projectId, tableNames: ["Test_validateClaim"])
```

### Scenario 3: Before Saving Project

```
Always run all tests before saving:

run_test(projectId, runAllTests: true)

If all pass → save_project(projectId)
If any fail → Fix and re-test
```

### Scenario 4: Before Deployment

```
Comprehensive pre-deployment validation:

1. validate_project(projectId) → Must pass
2. run_test(projectId, runAllTests: true) → All must pass
3. get_project_errors(projectId) → Must be 0 errors
4. If all pass → deploy_project(projectId, environment)
```

## Test Coverage Analysis

### Identify Untested Rules

```
1. List all rules
   list_tables(projectId, tableType: "Rules")

2. List all tests
   list_tables(projectId, tableType: "Test")

3. Compare
   Rules without matching Test_RuleName are untested

4. Create missing tests
   For each untested rule, create Test table
```

### Calculate Coverage

```
Coverage % = (Number of rules with tests / Total number of rules) × 100

Example:
- 15 rules total
- 12 have test tables
- Coverage: (12/15) × 100 = 80%

Target: 100% coverage before production deployment
```

## Test Results Interpretation

### All Tests Pass ✅
```
Result: All test cases passed
Action: Proceed with save or deployment
```

### Some Tests Fail ❌
```
Result: X out of Y tests failed
Action:
1. Review failure details (expected vs actual)
2. Determine if rule is wrong or test is wrong
3. Fix the issue
4. Re-run tests
5. Repeat until all pass
```

### Test Execution Error ⚠️
```
Result: Test couldn't execute (error before running)
Common causes:
- Missing dependencies
- Invalid test data
- Rule compilation errors

Action:
1. Fix underlying error
2. Validate project
3. Re-run tests
```

## Best Practices

### Testing During Development

**DO**:
- Run targeted tests after each rule change (fast feedback loop)
- Run all tests before every save
- Run all tests before any deployment
- Create tests for every new rule
- Fix failing tests immediately

**DON'T**:
- Skip tests for "small" changes
- Save with failing tests
- Deploy without running all tests
- Leave rules untested
- Ignore test failures

### Test-Driven Development

```
Recommended workflow:

1. Create test first (define expected behavior)
2. Create rule to satisfy test
3. Run test → should fail initially
4. Implement rule logic
5. Run test → should now pass
6. Refine rule if needed
7. Save when test passes
```

### Regression Testing

```
After any change:

1. Run tests for modified rules (targeted)
2. Run tests for related rules (integration)
3. Run all tests (comprehensive)

This catches:
- Direct breakage (modified rule fails)
- Indirect breakage (dependent rules fail)
- Integration issues (interaction problems)
```

## Quick Reference

| Situation | Test Command | Scope |
|-----------|-------------|-------|
| Modified 1 rule | `run_test(projectId, tableNames: ["Test_RuleName"])` | Targeted |
| Modified module | `run_test(projectId, tableNames: allModuleTests)` | Module |
| Before save | `run_test(projectId, runAllTests: true)` | All |
| Before deploy | `run_test(projectId, runAllTests: true)` | All |
| Created new rule | `run_test(projectId, tableNames: ["Test_NewRule"])` | Targeted |
| After merge | `run_test(projectId, runAllTests: true)` | All |

## Integration with Development Workflow

```
Complete development cycle:

1. Open project → open_project(projectId)
2. Modify rules → update_table() or create_rule()
3. Execute rules → execute_rule() for quick validation
4. Run targeted tests → run_test() for modified rules
5. If passing → Run all tests
6. If all passing → Validate project
7. If validated → Save project
8. If needed → Deploy project
```
