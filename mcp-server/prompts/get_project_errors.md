# Get Project Errors - Prompt Template

## Purpose
Provide detailed error analysis with fix suggestions to help AI assistant resolve issues.

## Prompt

## Project Error Analysis

**Project:** `{project_name}`
**Status:** {error_count} errors, {warning_count} warnings

---

### Error Summary

**Severity breakdown:**
- 🔴 **Errors:** {error_count} (must fix before save/deploy)
- 🟡 **Warnings:** {warning_count} (recommended to fix)
- 🟢 **Info:** {info_count} (optional improvements)

**Categories:**
- Type errors: {type_error_count}
- Syntax errors: {syntax_error_count}
- Reference errors: {reference_error_count}
- Validation errors: {validation_error_count}

---

### Errors (Must Fix)

{for each error:}
#### Error {index}: {error_type}

**Message:** {error_message}

**Location:**
- File: `{file_name}`
- Table: `{table_name}` ({table_type})
- Line: {line_number}, Column: {column_number}
- Cell: {cell_reference} {if applicable}

**Severity:** 🔴 ERROR

**Code snippet:**
```
{code_context_before}
→ {problematic_code}  ⚠️ Error here
{code_context_after}
```

**Problem explanation:**
{detailed_explanation}

**Root cause:**
{root_cause_analysis}

---

**💡 Suggested fix:**

{fix_suggestion_explanation}

**Option 1: Quick fix** (recommended)
```
{quick_fix_code}
```

**Option 2: Alternative approach**
```
{alternative_fix_code}
```

**Option 3: Refactor** (if structural issue)
```
{refactoring_suggestion}
```

---

**Why this works:**
{explanation_of_fix}

**Would you like me to apply this fix?** (yes/no/show-alternatives)

---

{end for}

---

### Common Error Patterns & Fixes

#### 1. Type Mismatch Errors

**Pattern:** `Cannot convert String to Integer`

**Common causes:**
- Column data type doesn't match rule signature
- Input data format incorrect
- Missing type conversion

**Fix:**
```java
// Before (error)
Integer age = customerData;  // customerData is String

// After (fixed)
Integer age = Integer.parseInt(customerData);
```

---

#### 2. Reference Errors

**Pattern:** `Datatype 'Customer' not found`

**Common causes:**
- Referenced datatype not defined
- Typo in datatype name
- Datatype in different module not imported

**Fix:**
1. Check if datatype exists: Use `list_tables` with filter `tableType=datatype`
2. If missing, create it: Use `create_rule` to create datatype
3. If exists, check spelling matches exactly (case-sensitive)

---

#### 3. Syntax Errors

**Pattern:** `Unexpected token at line 15`

**Common causes:**
- Excel formula syntax error
- Invalid OpenL syntax
- Missing brackets/parentheses

**Fix:**
- Review Excel formula syntax
- Check OpenL Tablets syntax guide
- Validate brackets are balanced

---

#### 4. Circular Reference Errors

**Pattern:** `Circular dependency detected: RuleA → RuleB → RuleA`

**Common causes:**
- Rule calls itself indirectly
- Two rules call each other
- Dependency cycle in datatypes

**Fix:**
1. Identify the cycle: {cycle_path}
2. Break the cycle by:
   - Extract common logic to separate rule
   - Use method table for shared functionality
   - Redesign rule dependencies

---

### Warnings (Recommended to Fix)

{for each warning:}
#### Warning {index}: {warning_type}

**Message:** {warning_message}

**Location:**
- File: `{file_name}`
- Table: `{table_name}`

**Severity:** 🟡 WARNING

**Impact:**
{impact_description}

**Recommendation:**
{recommendation}

**Fix (optional):**
```
{suggested_fix}
```

{end for}

---

### Automated Fix Options

I can help fix these errors:

**Auto-fixable errors:** {auto_fixable_count}/{error_count}

1. **Fix all type conversion errors** ({type_error_count} errors)
   - Add appropriate type conversions
   - Update column types

2. **Fix naming/reference errors** ({reference_error_count} errors)
   - Correct typos in datatype references
   - Add missing imports

3. **Fix syntax errors** ({syntax_error_count} errors)
   - Correct Excel formula syntax
   - Balance brackets

4. **Fix one by one** (manual review)
   - Review and approve each fix

**What would you like to do?**
- (1-4) Select auto-fix option
- (custom) I'll fix them manually
- (help) Explain a specific error

---

### Error-Free Checklist

To make your project error-free:

- [ ] Fix all {error_count} errors listed above
- [ ] Review {warning_count} warnings
- [ ] Run `validate_project` again to confirm
- [ ] Run `run_all_tests` to ensure rules work correctly
- [ ] Save project with `save_project`

---

### Next Steps

**Recommended workflow:**

1. **Fix errors** - Start with auto-fixable errors
2. **Validate** - Run `validate_project` to confirm fixes
3. **Test** - Run `run_all_tests` to ensure correctness
4. **Save** - Use `save_project` to persist changes
5. **Deploy** - Use `deploy_project` when ready

**Start with auto-fix?** (yes/no)

---

## Error Categories Explained

### Type Errors
**What:** Data type mismatches (String vs Integer, etc.)
**Fix:** Add type conversions or fix column types

### Syntax Errors
**What:** Invalid code syntax, formula errors
**Fix:** Correct syntax according to OpenL/Excel rules

### Reference Errors
**What:** Referenced table/datatype doesn't exist
**Fix:** Create missing dependencies or fix names

### Validation Errors
**What:** Business rule validation failures
**Fix:** Correct rule logic or data

---

**Total errors to fix:** {error_count}
**Estimated fix time:** ~{estimated_fix_time} minutes

**Ready to start fixing?** (yes/no)
