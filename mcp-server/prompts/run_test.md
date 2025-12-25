---
name: run_test
description: Test selection logic and workflow for efficient test execution
arguments:
  - name: scope
    description: Test scope (single, multiple, all)
    required: false
  - name: tableIds
    description: Comma-separated list of table IDs being tested
    required: false
---

## Summary

**When tables modified, run targeted tests first** (1-5 tables → specific tableIds, 6+ → runAll). Before save/deploy, ALWAYS run all tests (no exceptions).

**Test Execution Workflow:**
1. Use `openl_start_project_tests()` to start test execution
2. Use `openl_get_project_test_results()` to retrieve results (with `waitForCompletion: true` to wait for completion)

# Test Selection Logic

{if scope}
## Test Scope: {scope}
{end if}
{if tableIds}

**Tables to Test**: {tableIds}
{end if}

WHEN rules are modified, SELECT test scope:
- 1 rule → `tableId: "RuleTableId_1234"` (run all tests FOR this table)
- 2-5 rules → Run tests for each affected rule table separately
- 6+ rules → Run all tests (omit `tableId` parameter)
- Before save/deploy → Run all tests (omit `tableId` parameter, MANDATORY)

**Parameter clarification:**
- `tableId`: Rule table ID being tested (e.g., "calculatePremium_1234") - runs ALL tests FOR this table
- `testRanges`: Specific test ranges for Test tables (e.g., "1-3,5" to run tests 1, 2, 3, and 5)
- Omit `tableId` to run all tests in the project

**Test Execution Steps:**
1. Start tests: `openl_start_project_tests(projectId, { tableId?, testRanges? })`
2. Get results: `openl_get_project_test_results(projectId, { waitForCompletion: true, failuresOnly?: boolean })`

AFTER modification:
1. Execute targeted tests first (with specific `tableId`)
2. IF pass AND not saving → done
3. IF saving → run all tests (omit `tableId`, no exceptions)

BEFORE openl_update_project_status():
- `openl_start_project_tests()` + `openl_get_project_test_results(waitForCompletion: true)` MUST pass
- `Validate in OpenL WebStudio UI (openl_validate_project temporarily disabled)` MUST pass

BEFORE openl_deploy_project():
- All above + `openl_get_project_errors()` MUST be 0

## Integration Context

- Simple selection (1 rule) → Tool description sufficient
- Complex selection (many rules) → This prompt guides decision
- User overrides → Always allowed
