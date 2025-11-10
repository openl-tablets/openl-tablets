# Execute Rule - Prompt Template

## Purpose
Help AI assistant construct valid test data and execute rules to verify behavior.

## Prompt

## Rule Execution Setup

**Rule to execute:** `{rule_name}`
**Type:** {rule_type}

---

### Rule Signature

```java
{return_type} {rule_name}({parameters})
```

**Return type:** `{return_type}`
**Parameters:**

{for each parameter:}
- **{parameter_name}**: `{parameter_type}` {required_indicator}
  - Description: {parameter_description}
  - Valid values: {valid_values_or_range}
  - Example: `{example_value}`
{end for}

---

### Input Data Required

To execute this rule, provide values for all parameters:

#### Parameter Details

{for each parameter:}
##### {index}. {parameter_name} ({parameter_type})

**Type:** `{parameter_type}`
**Required:** {is_required}

{if simple_type:}
**Format:** {format_description}
**Example:** `{example_value}`

**Your value:** _______________

{else if datatype:}
**Structure:** This is a custom datatype with these fields:
```json
{datatype_structure_json}
```

**Example:**
```json
{example_datatype_instance}
```

**Your value (JSON):**
```json
{user_to_fill}
```

{else if array:}
**Element type:** `{array_element_type}`
**Format:** Array of {array_element_type}

**Example:**
```json
{example_array}
```

**Your value (JSON):**
```json
{user_to_fill}
```

{end if}

{end for}

---

### Quick Test Data Templates

I can generate test data for common scenarios:

#### Template 1: Typical Case ✅ Recommended
**Scenario:** {typical_scenario_description}
**Input data:**
```json
{typical_test_data}
```
**Expected output:** ~{expected_output_typical}

#### Template 2: Edge Case - Minimum Values
**Scenario:** {minimum_scenario_description}
**Input data:**
```json
{minimum_test_data}
```
**Expected output:** ~{expected_output_minimum}

#### Template 3: Edge Case - Maximum Values
**Scenario:** {maximum_scenario_description}
**Input data:**
```json
{maximum_test_data}
```
**Expected output:** ~{expected_output_maximum}

#### Template 4: Error Case
**Scenario:** {error_scenario_description}
**Input data:**
```json
{error_test_data}
```
**Expected output:** Error or validation failure

#### Template 5: Custom
**Your scenario:** ___________________________
**Input data:**
```json
{
  // Fill in your custom test data
}
```

---

### Test Data Selection

**What would you like to do?**

1. Use Template 1 (typical case) ⚡ Quick test
2. Use Template 2 (minimum values)
3. Use Template 3 (maximum values)
4. Use Template 4 (error case)
5. Provide custom test data
6. Generate random valid data
7. Use data from existing test table

**Your choice:** (1-7)

---

### Data Validation

Before executing, I'll validate your input data:

**Validation checks:**
- ✓ All required parameters provided
- ✓ Data types match parameter types
- ✓ Values within valid ranges
- ✓ Referenced datatypes are complete
- ✓ Arrays have correct element types

{if validation_errors:}
⚠️ **Validation errors found:**
{list_validation_errors}

**Please fix these before execution.**
{end if}

---

### Execution Options

**Execution mode:**

1. **Execute once** - Run with single input
2. **Execute batch** - Run with multiple inputs (array)
3. **Execute and compare** - Run and compare to expected output
4. **Execute with tracing** - Run with detailed execution trace (debug mode)

**Select mode:** (1-4)

---

### Expected Output

**Based on the rule logic, expected output:**

**Return type:** `{return_type}`

{if simple_return_type:}
**Expected value:** ~`{estimated_output}`
{else if datatype_return:}
**Expected structure:**
```json
{return_datatype_structure}
```
{else if array_return:}
**Expected:** Array of `{array_element_type}`
{end if}

**Confidence:** {confidence_level} (based on rule analysis)

---

### Execution Preview

**Ready to execute:**

```json
{
  "rule": "{rule_name}",
  "parameters": {
    {parameter_values_preview}
  },
  "expected_output": {expected_output_preview},
  "execution_mode": "{selected_mode}"
}
```

**Execute now?** (yes/no/modify)

---

### After Execution

I will show you:

1. **Actual output:**
   ```json
   {actual_output}
   ```

2. **Execution time:** {execution_time_ms} ms

3. **Comparison to expected:**
   - Match: {match_status} ✅/❌
   - Differences: {differences_if_any}

4. **Execution trace** (if enabled):
   - Rules called
   - Intermediate values
   - Decision path taken

5. **Analysis:**
   - Does output match expected? {yes/no}
   - Any unexpected behavior? {yes/no}
   - Recommendations: {recommendations}

---

### Use Cases for Rule Execution

✅ **When to execute rules:**
- After creating a new rule - verify it works
- After modifying a rule - ensure changes work correctly
- Before saving - quick validation
- Debugging - understand rule behavior
- Creating test data - see what inputs produce what outputs

✅ **Benefits:**
- Immediate feedback on rule changes
- No need to deploy to test
- Fast iteration during development
- Helps create test tables with expected outputs

---

### Quick Execution Workflow

**For rapid testing:**

1. I suggest typical test data
2. You approve or modify
3. I execute the rule
4. We review output together
5. If incorrect, we fix the rule
6. Repeat until correct
7. Save the working rule

**Start quick execution?** (yes/no)

---

## Test Data Generation Tips

### For Decision Tables
- Test each condition combination
- Test boundary values
- Test default cases

### For Spreadsheets
- Test with zero values
- Test with negative values (if applicable)
- Test extreme values

### For Datatypes
- Ensure all required fields populated
- Test with null/optional fields

---

**Ready to execute `{rule_name}`?** (yes/no)

**Need help with test data?** (yes/no)
