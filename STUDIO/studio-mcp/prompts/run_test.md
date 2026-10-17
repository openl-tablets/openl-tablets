---
title: Run Tests
description: Test selection logic and workflow for running OpenL tests efficiently based on scope (single table, multiple tables, or all tests)
arguments:
  - name: scope
    description: "Test scope: 'single', 'multiple', or 'all'"
    required: false
  - name: tableIds
    description: Comma-separated list of table IDs being tested
    required: false
---

## Summary

**When tables modified, run targeted tests first** (1-5 tables → specific tableIds, 6+ → runAll). Before save/deploy, ALWAYS run all tests (no exceptions).

**Test Execution Workflow:**
1. Use `openl_start_project_tests()` to start test execution
   - For design repositories: project is automatically opened if closed
   - For repository 'local': project is not opened; tests run directly
   - Returns execution status and metadata
2. Use `openl_get_test_results_summary()` for brief summary (without testCases)
3. Use `openl_get_test_results()` for full results with pagination
   - Result tools wait internally while the asynchronous test run is in progress; do not poll them from the agent
   - **IMPORTANT**: Pagination applies to test tables, not individual test cases
   - Each page returns test results aggregated by table (e.g., one table may contain multiple tests)
   - Example: Page 1 might show 5 tables with aggregated test counts (7 tests, 8 tests, etc.)
   - **NOTE**: The 'unpaged' parameter may not work correctly on the backend - use pagination (page/offset/size) instead
4. Use `openl_get_test_results_by_table()` for results filtered by table ID
   - It waits for the run to finish before filtering; an in-progress run is not interpreted as zero tests
   - **NOTE**: The 'unpaged' parameter may not work correctly on the backend - use pagination if needed

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
2. Get results: `openl_get_test_results(projectId)` or `openl_get_test_results_summary(projectId)`

AFTER modification:
1. Start targeted tests first (with specific `tableId`): `openl_start_project_tests(projectId, { tableId })`
2. Get results: `openl_get_test_results(projectId)` or `openl_get_test_results_by_table(projectId, tableId)`
3. IF pass AND not saving → done
4. IF saving → start all tests (omit `tableId`, no exceptions): `openl_start_project_tests(projectId)`

BEFORE openl_save_project() (design repositories only; not applicable for repository 'local'):
- openl_save_project works only when project status is EDITING; comment is required; after save, new revision is created and project → OPENED
- `openl_start_project_tests(projectId)` → `openl_get_test_results(projectId)` MUST pass
- `openl_project_status(projectId)` MUST report no errors (saving also validates the project)
- For repository 'local': do not call openl_save_project; run tests without open/save.

BEFORE openl_deploy_project():
- All above + `openl_project_status(projectId)` MUST report 0 errors in its diagnostics

## Integration Context

- Simple selection (1 rule) → Tool description sufficient
- Complex selection (many rules) → This prompt guides decision
- User overrides → Always allowed
