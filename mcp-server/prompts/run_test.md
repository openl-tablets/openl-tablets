# Run Test - Prompt Template

## Purpose
Help AI assistant intelligently select which tests to run based on rule changes.

## Prompt

## Smart Test Selection

You've made changes to the following rules:
{modified_tables_list}

---

### Affected Test Tables

I've analyzed your project and found these relevant tests:

#### 🎯 Directly Related Tests (Recommended)

{for each modified rule:}
**Rule:** `{rule_name}` ({rule_type})
**Related tests:**
- `Test_{rule_name}` - Direct unit tests for this rule
- `Integration_Test_{module}` - Integration tests for this module

**Coverage:** {test_coverage_percentage}% of rule scenarios

{end for}

---

### Test Selection Options

#### Option 1: Run Affected Tests Only ⚡ Fast (Recommended)
**Tests:** {affected_test_count} tests
**Estimated time:** ~{estimated_time_fast} seconds
**Coverage:** Tests directly impacted by your changes

**Test list:**
```
{list_of_affected_tests}
```

**Command:** Run tests: {affected_test_ids}

---

#### Option 2: Run Module Tests 🔍 Thorough
**Tests:** {module_test_count} tests
**Estimated time:** ~{estimated_time_medium} seconds
**Coverage:** All tests in affected modules

**Test list:**
```
{list_of_module_tests}
```

**Command:** Run tests: {module_test_ids}

---

#### Option 3: Run All Tests 🎯 Comprehensive
**Tests:** {all_test_count} tests
**Estimated time:** ~{estimated_time_full} seconds
**Coverage:** Entire project (recommended before deployment)

**Use when:**
- Before deploying to production
- Major changes across multiple modules
- Want comprehensive validation

**Command:** Run all tests

---

### Test Coverage Analysis

**Your changes impact:**
- Decision Tables: {affected_decision_tables}
- Spreadsheets: {affected_spreadsheets}
- Datatypes: {affected_datatypes}

**Test coverage:**
- ✅ Covered by tests: {covered_rules}
- ⚠️ Missing tests: {untested_rules}

{if untested_rules > 0:}
**⚠️ Warning:** Some modified rules don't have tests.

**Untested rules:**
{list_untested_rules}

**Recommendation:** Create test tables for these rules to ensure correctness.
Would you like me to help create tests? (yes/no)
{end if}

---

### Running Tests

**What would you like to do?**

1. Run affected tests only (fast validation) ✅ Recommended
2. Run module tests (thorough validation)
3. Run all tests (comprehensive validation)
4. Select specific tests manually
5. Create missing tests first

**Your choice:** (1-5)

---

### After Running Tests

I will:
1. Execute the selected tests
2. Show pass/fail summary
3. Display detailed results for any failures
4. Suggest fixes for failing tests
5. Recommend next steps (fix issues or proceed with save)

---

## Test Execution Best Practices

✅ **DO:**
- Run affected tests after each rule change
- Run all tests before saving project
- Run all tests before deploying to production
- Create tests for new rules

❌ **DON'T:**
- Skip tests for "small" changes
- Save project with failing tests
- Deploy without running tests
- Leave rules untested

---

**Ready to run tests?** (yes/no)
