# Test Selection Logic

WHEN rules are modified, SELECT test scope:
- 1 rule → `tableNames: ["Test_RuleName"]`
- 2-5 rules → `tableNames: [all affected module tests]`
- 6+ rules → `runAllTests: true`
- Before save/deploy → `runAllTests: true` (MANDATORY)

AFTER modification:
1. Execute targeted tests first
2. IF pass AND not saving → done
3. IF saving → run all tests (no exceptions)

BEFORE save_project():
- `run_test(runAllTests: true)` MUST pass
- `validate_project()` MUST pass

BEFORE deploy_project():
- All above + `get_project_errors()` MUST be 0

## Integration Context

- Simple selection (1 rule) → Tool description sufficient
- Complex selection (many rules) → This prompt guides decision
- User overrides → Always allowed
