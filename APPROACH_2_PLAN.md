# Approach 2 Implementation Plan: Enhanced Arguments

## Overview

Add dynamic argument support to prompts for contextual, personalized guidance.

**Goal**: Make prompts more helpful by injecting project-specific context (table names, project IDs, etc.)

**Effort**: 6-8 hours
**Risk**: Medium (requires file modifications)
**Value**: High (better user experience)

---

## What Changes

### Phase 1: Infrastructure Updates (1-2 hours)

#### 1. Install YAML Parser
```bash
npm install yaml
npm install --save-dev @types/yaml
```

#### 2. Update `src/prompts-registry.ts`

**Add YAML parsing**:
```typescript
import YAML from 'yaml';

interface PromptFrontmatter {
  name: string;
  description?: string;
  arguments?: Array<{
    name: string;
    description: string;
    required?: boolean;
  }>;
}

function parsePromptFile(content: string): {
  frontmatter: PromptFrontmatter | null;
  body: string;
} {
  const frontmatterPattern = /^---\n([\s\S]*?)\n---\n([\s\S]*)$/;
  const match = content.match(frontmatterPattern);

  if (match) {
    const frontmatter = YAML.parse(match[1]) as PromptFrontmatter;
    const body = match[2];
    return { frontmatter, body };
  }

  return { frontmatter: null, body: content };
}

// Update loadPromptContent to use parsePromptFile
export function loadPromptContent(
  name: string,
  args?: Record<string, string>
): string {
  const filePath = join(promptsDir, `${name}.md`);

  let content: string;
  try {
    content = readFileSync(filePath, "utf-8");
  } catch (error) {
    throw new Error(
      `Failed to load prompt '${name}': ${error instanceof Error ? error.message : String(error)}`
    );
  }

  // Parse frontmatter
  const { body } = parsePromptFile(content);

  // Apply argument substitution if provided
  if (args) {
    return substituteArguments(body, args);
  }

  return body;
}
```

**No changes needed to `substituteArguments()`** - it already handles conditionals and variables!

---

### Phase 2: Update Prompt Files (3-4 hours)

Priority order based on value:

#### High Priority (3 files) - Update First

##### 1. `create_test.md` ⭐ HIGH VALUE

**Why**: Most commonly used, benefits greatly from context

**Add frontmatter**:
```yaml
---
name: create_test
description: Guide for creating OpenL test tables with proper structure and validation
arguments:
  - name: tableName
    description: Name of the table being tested
    required: false
  - name: tableType
    description: Type of table (Rules, SimpleRules, Spreadsheet, etc.)
    required: false
---
```

**Add placeholders in content**:
```markdown
# Creating Test Tables in OpenL Tablets

{if tableName}
## Creating Test for: **{tableName}**

You're creating a test table for the `{tableName}` rule.
{end if}

{if tableType}
### Table Type: {tableType}

Since you're testing a **{tableType}** table, pay attention to:
{if tableType}
- {tableType} tables use [specific characteristics]
{end if}
{end if}

## When to Create Test Tables
...rest of content...
```

**Lines affected**: ~10 lines added (frontmatter + placeholders)

---

##### 2. `execute_rule.md` ⭐ HIGH VALUE

**Why**: Execution guidance is much better with specific rule context

**Add frontmatter**:
```yaml
---
name: execute_rule
description: Guide for constructing test data and executing OpenL rules
arguments:
  - name: ruleName
    description: Name of the rule to execute
    required: false
  - name: projectId
    description: ID of the project containing the rule
    required: false
---
```

**Add placeholders**:
```markdown
# OpenL Rule Execution

{if ruleName}
## Executing Rule: `{ruleName}`
{end if}

{if projectId}
**Project**: {projectId}
{end if}

WHEN to execute:
- After creating/modifying {if ruleName}{ruleName}{end if} → Verify behavior
...
```

**Lines affected**: ~8 lines

---

##### 3. `deploy_project.md` ⭐ HIGH VALUE

**Add frontmatter**:
```yaml
---
name: deploy_project
description: OpenL deployment workflow with validation checks and environment selection
arguments:
  - name: projectId
    description: ID of project to deploy
    required: false
  - name: environment
    description: Target environment (dev, test, staging, prod)
    required: false
---
```

**Add placeholders**:
```markdown
# OpenL Deployment Workflow

{if projectId}
## Deploying Project: **{projectId}**
{end if}

{if environment}
**Target Environment**: {environment}

{if environment}
Environment-specific checks for **{environment}**:
{end if}
{end if}

BEFORE any deployment (MANDATORY):
...
```

**Lines affected**: ~10 lines

---

#### Medium Priority (4 files) - Update Second

##### 4. `update_test.md`

**Arguments**: `testId`, `tableName`

**Placeholders**:
```markdown
{if testId}
## Updating Test: {testId}
{end if}

{if tableName}
**Testing Table**: {tableName}
{end if}
```

**Lines affected**: ~6 lines

---

##### 5. `run_test.md`

**Arguments**: `scope`, `tableIds`

**Placeholders**:
```markdown
{if scope}
## Test Scope: {scope}
{end if}

{if tableIds}
**Tables to test**: {tableIds}
{end if}
```

**Lines affected**: ~6 lines

---

##### 6. `get_project_errors.md`

**Arguments**: `projectId`

**Placeholders**:
```markdown
{if projectId}
## Analyzing Errors for: {projectId}
{end if}
```

**Lines affected**: ~4 lines

---

##### 7. `file_history.md`

**Arguments**: `filePath`, `projectId`

**Placeholders**:
```markdown
{if filePath}
## File History: {filePath}
{end if}

{if projectId}
**Project**: {projectId}
{end if}
```

**Lines affected**: ~6 lines

---

#### Low Priority (4 files) - Update Last

##### 8. `project_history.md`

**Arguments**: `projectId`

**Lines affected**: ~4 lines

##### 9-11. `create_rule.md`, `datatype_vocabulary.md`, `dimension_properties.md`

**No arguments** - These are conceptual guides that don't benefit from context

**Action**: Add frontmatter only (no placeholders)

---

### Phase 3: Update Tests (1 hour)

#### Add tests for frontmatter parsing

```typescript
describe("Frontmatter parsing", () => {
  test("should parse YAML frontmatter correctly", () => {
    // Test that frontmatter is extracted and parsed
  });

  test("should handle files without frontmatter", () => {
    // Test backward compatibility
  });

  test("frontmatter arguments should match PROMPTS registry", () => {
    // Test consistency between frontmatter and registry
  });
});

describe("Enhanced argument substitution", () => {
  test("should substitute variables with frontmatter context", () => {
    const content = loadPromptContent("create_test", {
      tableName: "calculatePremium",
      tableType: "SimpleRules"
    });

    expect(content).toContain("calculatePremium");
    expect(content).toContain("SimpleRules");
  });

  test("should handle conditional blocks based on arguments", () => {
    const withArgs = loadPromptContent("execute_rule", {
      ruleName: "getRate"
    });
    const withoutArgs = loadPromptContent("execute_rule");

    expect(withArgs).toContain("getRate");
    expect(withoutArgs).not.toContain("Executing Rule:");
  });
});
```

**New tests**: ~15 additional tests

---

## File Modification Summary

| File | Change Type | Lines | Risk | Priority |
|------|-------------|-------|------|----------|
| `src/prompts-registry.ts` | Add YAML parsing | +30 | Low | P0 |
| `create_test.md` | Add frontmatter + placeholders | +10 | Low | P1 |
| `execute_rule.md` | Add frontmatter + placeholders | +8 | Low | P1 |
| `deploy_project.md` | Add frontmatter + placeholders | +10 | Low | P1 |
| `update_test.md` | Add frontmatter + placeholders | +6 | Low | P2 |
| `run_test.md` | Add frontmatter + placeholders | +6 | Low | P2 |
| `get_project_errors.md` | Add frontmatter + placeholders | +4 | Low | P2 |
| `file_history.md` | Add frontmatter + placeholders | +6 | Low | P2 |
| `project_history.md` | Add frontmatter + placeholders | +4 | Low | P3 |
| `create_rule.md` | Add frontmatter only | +6 | Low | P3 |
| `datatype_vocabulary.md` | Add frontmatter only | +5 | Low | P3 |
| `dimension_properties.md` | Add frontmatter only | +4 | Low | P3 |
| `tests/prompts.test.ts` | Add frontmatter tests | +50 | Low | P0 |

**Total**: ~149 lines added across 13 files

---

## Implementation Order

### Step 1: Infrastructure (30 min)
1. Install `yaml` package
2. Update `prompts-registry.ts` with frontmatter parsing
3. Run existing tests to ensure no regression

### Step 2: High Priority Prompts (2 hours)
1. Update `create_test.md`
2. Update `execute_rule.md`
3. Update `deploy_project.md`
4. Test each file after modification

### Step 3: Medium Priority Prompts (1.5 hours)
1. Update `update_test.md`
2. Update `run_test.md`
3. Update `get_project_errors.md`
4. Update `file_history.md`

### Step 4: Low Priority Prompts (30 min)
1. Update `project_history.md`
2. Add frontmatter to conceptual guides
3. Verify all files parse correctly

### Step 5: Enhanced Tests (1 hour)
1. Add frontmatter parsing tests
2. Add enhanced substitution tests
3. Add backward compatibility tests
4. Run full test suite

### Step 6: Documentation & Build (30 min)
1. Update README with frontmatter examples
2. Build project
3. Test in MCP Inspector
4. Commit and push

---

## Backward Compatibility

✅ **Fully backward compatible**:
- Files without frontmatter work as before
- Prompts without arguments work unchanged
- All existing tests pass
- No breaking changes to API

---

## Testing Strategy

### Manual Testing in MCP Inspector

**Test Case 1: Prompt with arguments**
```
Prompt: create_test
Arguments: { tableName: "calculatePremium", tableType: "SimpleRules" }
Expected: See "Creating Test for: calculatePremium" and "Table Type: SimpleRules"
```

**Test Case 2: Prompt without arguments**
```
Prompt: create_test
Arguments: {}
Expected: Generic content without personalization
```

**Test Case 3: Partial arguments**
```
Prompt: execute_rule
Arguments: { ruleName: "getRate" }
Expected: Shows ruleName but not projectId section
```

### Automated Tests

```bash
npm run test:unit
# Should pass all 94 existing tests + ~15 new tests = 109 total
```

---

## Expected Results

### Before (Approach 1)
```
User: "Use create_test prompt"
Prompt returns:
# Creating Test Tables in OpenL Tablets

## When to Create Test Tables
- After creating/modifying decision table → Validate rule behavior
...
```

### After (Approach 2)
```
User: "Use create_test prompt with tableName=calculatePremium and tableType=SimpleRules"
Prompt returns:
# Creating Test Tables in OpenL Tablets

## Creating Test for: **calculatePremium**

You're creating a test table for the `calculatePremium` rule.

### Table Type: SimpleRules

Since you're testing a **SimpleRules** table, pay attention to:
- SimpleRules tables use positional parameter matching
...

## When to Create Test Tables
- After creating/modifying calculatePremium → Validate rule behavior
...
```

**Much more contextual and helpful!**

---

## Risks & Mitigations

### Risk 1: YAML parsing errors
**Mitigation**: Graceful fallback to non-frontmatter mode

### Risk 2: Placeholder syntax conflicts with examples
**Mitigation**: Use unique syntax `{if var}...{end if}` that's unlikely in examples

### Risk 3: Test failures due to content changes
**Mitigation**: Update content validation tests to be flexible

### Risk 4: Increased file sizes
**Mitigation**: Frontmatter is minimal (~5-10 lines per file)

---

## Rollback Plan

If issues arise:

1. **Partial rollback**: Keep infrastructure, remove frontmatter from problematic files
2. **Full rollback**: Revert to Approach 1 (all tests still pass)
3. **Fix forward**: Debug specific file, others continue working

---

## Success Criteria

✅ **Phase 1 Complete When**:
- `yaml` package installed
- Frontmatter parsing implemented
- All existing tests pass
- No regressions

✅ **Phase 2 Complete When**:
- 7 high/medium priority prompts updated
- Arguments work correctly in each
- Manual testing shows personalized content

✅ **Phase 3 Complete When**:
- All 11 prompts have frontmatter
- Tests cover frontmatter parsing
- MCP Inspector shows dynamic content
- Documentation updated

✅ **Overall Success**:
- All ~109 tests passing
- Build succeeds
- MCP Inspector shows prompts with arguments
- Dynamic substitution working
- No breaking changes

---

## Timeline

| Phase | Duration | Can Start |
|-------|----------|-----------|
| Infrastructure | 30 min | Immediately |
| High Priority (3 files) | 2 hours | After infrastructure |
| Medium Priority (4 files) | 1.5 hours | After high priority |
| Low Priority (4 files) | 30 min | After medium priority |
| Testing | 1 hour | After all files updated |
| Documentation | 30 min | After testing |
| **Total** | **6 hours** | - |

**Can be done in phases**: Infrastructure + High Priority (2.5 hrs) gives immediate value

---

## Decision Points

### Option A: Full Implementation
- Do all 11 files
- Complete feature set
- 6 hours effort

### Option B: High Priority Only
- Do 3 high-value files
- 80% of benefit
- 2.5 hours effort

### Option C: Incremental
- Start with infrastructure
- Add files one by one as needed
- Lower risk, gradual rollout

---

## Recommendation

**Start with Infrastructure + High Priority (Option B)**

**Why**:
1. Quick wins (2.5 hours)
2. Most valuable prompts enhanced first
3. Can evaluate before continuing
4. Lower risk than full rollout
5. Backward compatible

**Then**: Based on feedback, decide whether to continue with medium/low priority files

---

## Questions Before Proceeding

1. **Scope**: Full implementation (Option A) or High Priority (Option B)?
2. **Timeline**: Do all at once or incremental?
3. **Testing**: Manual testing in MCP Inspector available?
4. **Review**: Want to review frontmatter examples before I start?

Let me know your preference and I'll execute accordingly!
