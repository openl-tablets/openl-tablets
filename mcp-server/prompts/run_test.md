# Test Selection Logic

WHEN rules are modified, SELECT test scope:
- 1 rule → `tableIds: ["RuleTableId_1234"]` (run all tests FOR this table)
- 2-5 rules → `tableIds: [all affected rule table IDs]`
- 6+ rules → `runAll: true`
- Before save/deploy → `runAll: true` (MANDATORY)

**Parameter clarification:**
- `testIds`: Specific test table IDs (e.g., ["Test_calculatePremium_1234"])
- `tableIds`: Rule table IDs being tested (e.g., ["calculatePremium_1234"]) - runs ALL tests FOR these tables
- `runAll`: Run entire test suite

AFTER modification:
1. Execute targeted tests first
2. IF pass AND not saving → done
3. IF saving → run all tests (no exceptions)

BEFORE save_project():
- `run_test(runAll: true)` MUST pass
- `validate_project()` MUST pass

BEFORE deploy_project():
- All above + `get_project_errors()` MUST be 0

## Integration Context

- Simple selection (1 rule) → Tool description sufficient
- Complex selection (many rules) → This prompt guides decision
- User overrides → Always allowed
