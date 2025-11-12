# MCP Prompts Implementation Plan

## Problem Analysis

### Current Situation ❌

1. **11 prompt markdown files exist** in `mcp-server/prompts/` directory
2. **PromptLoader class exists** in `src/prompts.ts` (functional utility for loading prompts)
3. **MCP server does NOT expose prompts** via the protocol
4. **MCP Inspector shows 0 prompts** because they're not registered

### Root Cause

The MCP server only registers **two capabilities**:
```typescript
capabilities: {
  tools: {},      // ✅ Registered
  resources: {},  // ✅ Registered
  // prompts: {} ❌ MISSING!
}
```

The `setupHandlers()` method registers handlers for:
- ✅ `ListToolsRequestSchema`
- ✅ `CallToolRequestSchema`
- ✅ `ListResourcesRequestSchema`
- ✅ `ReadResourceRequestSchema`
- ❌ **`ListPromptsRequestSchema` - NOT REGISTERED**
- ❌ **`GetPromptRequestSchema` - NOT REGISTERED**

---

## MCP Prompts Protocol Overview

### What Are MCP Prompts?

MCP Prompts are **pre-defined prompt templates** that:
- Guide AI assistants through complex workflows
- Provide domain-specific context and best practices
- Can accept arguments for dynamic content
- Return structured messages (user/assistant roles)

### MCP Prompt Structure

```typescript
interface Prompt {
  name: string;              // Unique identifier (e.g., "create_rule")
  title?: string;            // Human-readable title
  description?: string;      // What this prompt helps with
  arguments?: PromptArgument[]; // Optional dynamic arguments
}

interface GetPromptResult {
  description?: string;
  messages: PromptMessage[];  // Array of user/assistant messages
}

interface PromptMessage {
  role: "user" | "assistant";
  content: TextContent | ImageContent | EmbeddedResource;
}
```

### How Prompts Work

1. **List Prompts**: Client calls `prompts/list` → Server returns available prompts
2. **Get Prompt**: Client calls `prompts/get` with name → Server returns prompt messages
3. **Use Prompt**: Client injects prompt messages into conversation context

---

## Prompt Files Inventory

### 11 Existing Prompt Files

| File | Purpose | Lines | Complexity | Arguments Needed? |
|------|---------|-------|------------|-------------------|
| `create_rule.md` | Guide for creating OpenL tables | 328 | High | No - general guide |
| `create_test.md` | Guide for creating test tables | 234 | Medium | Yes - table type, params |
| `update_test.md` | Guide for updating tests | 186 | Medium | Yes - test ID, changes |
| `run_test.md` | Guide for test selection logic | 45 | Low | Yes - scope, changes |
| `datatype_vocabulary.md` | Guide for defining datatypes | 287 | High | No - general guide |
| `dimension_properties.md` | Dimension properties explained | 96 | Medium | No - conceptual |
| `execute_rule.md` | Rule execution guide | 67 | Medium | Yes - rule name, params |
| `deploy_project.md` | Deployment workflow | 58 | Medium | Yes - project ID, env |
| `get_project_errors.md` | Error analysis workflow | 72 | Medium | Yes - project ID |
| `file_history.md` | Git versioning for files | 42 | Low | Yes - file path |
| `project_history.md` | Project history guide | 38 | Low | Yes - project ID |

**Total**: 1,453 lines of domain-specific guidance

---

## Current vs Required Format Analysis

### Current Format ✅ **GOOD NEWS!**

The existing prompt files are **already in a compatible format**:

1. **Markdown formatted** ✅
2. **Self-contained guidance** ✅
3. **Clear structure with headings** ✅
4. **Domain-specific best practices** ✅
5. **Examples and code blocks** ✅

### What Needs to Change

#### ❌ **Minor Format Issues**:

1. **No YAML frontmatter** - Currently files lack metadata
   ```markdown
   ---
   name: create_rule
   description: Guide for creating OpenL tables
   ---
   # Content here...
   ```

2. **No argument placeholders** - Files don't have `{variable}` substitution markers
   ```markdown
   ## Creating Test for {tableName}
   Your table type is: {tableType}
   ```

3. **Not split into messages** - Content needs role markers (user/assistant)
   ```markdown
   [USER]
   I need help creating an OpenL table.

   [ASSISTANT]
   Let me guide you through the process...
   ```

#### ✅ **What's Already Good**:

- Content quality is excellent
- Structure is logical and clear
- Examples are comprehensive
- Domain knowledge is accurate

---

## Implementation Approaches

### Approach 1: Minimal Changes ⭐ **RECOMMENDED**

**Strategy**: Keep existing markdown files, wrap them at runtime

**Pros**:
- ✅ No rewriting needed
- ✅ Fast implementation (2-3 hours)
- ✅ Maintain current structure
- ✅ Easy to update prompts

**Cons**:
- ⚠️ No argument substitution (unless we add it)
- ⚠️ Single message per prompt (not multi-turn)

**Implementation**:
1. Create `PROMPTS` array in new file `src/prompts-registry.ts`
2. Load markdown files at server startup
3. Register `ListPromptsRequestSchema` and `GetPromptRequestSchema` handlers
4. Return entire markdown content as single `assistant` message

**Changes Required**:
```typescript
// src/prompts-registry.ts (NEW FILE)
export const PROMPTS = [
  {
    name: "create_rule",
    title: "Create OpenL Table",
    description: "Step-by-step guide for creating OpenL decision tables, spreadsheets, and datatypes",
  },
  {
    name: "create_test",
    title: "Create Test Table",
    description: "Guide for creating OpenL test tables with proper structure",
    arguments: [
      { name: "tableType", description: "Type of table being tested", required: false }
    ]
  },
  // ... 9 more
];

// Load content from markdown files
export function loadPromptContent(name: string, args?: Record<string, string>): string {
  const filePath = join(__dirname, "..", "prompts", `${name}.md`);
  let content = readFileSync(filePath, "utf-8");

  // Simple argument substitution if needed
  if (args) {
    for (const [key, value] of Object.entries(args)) {
      content = content.replace(new RegExp(`{${key}}`, "g"), value);
    }
  }

  return content;
}
```

```typescript
// src/index.ts (MODIFY)
import { ListPromptsRequestSchema, GetPromptRequestSchema } from "@modelcontextprotocol/sdk/types.js";
import { PROMPTS, loadPromptContent } from "./prompts-registry.js";

constructor(config: Types.OpenLConfig) {
  // ...
  this.server = new Server(
    { name: SERVER_INFO.NAME, version: SERVER_INFO.VERSION },
    {
      capabilities: {
        tools: {},
        resources: {},
        prompts: {},  // ✅ ADD THIS
      },
    }
  );
}

private setupHandlers(): void {
  // ... existing handlers ...

  // ✅ ADD: List available prompts
  this.server.setRequestHandler(ListPromptsRequestSchema, async () => ({
    prompts: PROMPTS,
  }));

  // ✅ ADD: Get specific prompt content
  this.server.setRequestHandler(GetPromptRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    const prompt = PROMPTS.find(p => p.name === name);

    if (!prompt) {
      throw new McpError(ErrorCode.InvalidRequest, `Prompt not found: ${name}`);
    }

    const content = loadPromptContent(name, args);

    return {
      description: prompt.description,
      messages: [
        {
          role: "assistant",
          content: {
            type: "text",
            text: content,
          },
        },
      ],
    };
  });
}
```

**Estimated Time**: 2-3 hours
**Risk**: Low
**Rewriting**: None

---

### Approach 2: Enhanced with Arguments

**Strategy**: Add YAML frontmatter + argument substitution

**Pros**:
- ✅ Dynamic prompts with context
- ✅ Metadata in files (self-documenting)
- ✅ Can pass project ID, table name, etc.

**Cons**:
- ⚠️ Requires updating all 11 files
- ⚠️ Need YAML parser

**Example Enhanced File**:
```markdown
---
name: create_test
description: Guide for creating OpenL test tables
arguments:
  - name: tableName
    description: Name of the table being tested
    required: false
  - name: tableType
    description: Type of table (Rules, SimpleRules, etc.)
    required: false
---

# Creating Test Table{if tableName} for {tableName}{end if}

{if tableType}
You're creating a test for a **{tableType}** table.
{end if}

## Test Table Structure
...
```

**Changes Required**:
- Add YAML frontmatter to all 11 files
- Add `{variable}` placeholders where context is useful
- Install `yaml` parser: `npm install yaml`
- Parse frontmatter in `loadPromptContent()`

**Estimated Time**: 6-8 hours
**Risk**: Medium
**Rewriting**: Moderate (add metadata + placeholders)

---

### Approach 3: Multi-Turn Prompts

**Strategy**: Split prompts into conversation-style exchanges

**Pros**:
- ✅ Natural conversation flow
- ✅ Progressive disclosure
- ✅ Better for complex workflows

**Cons**:
- ❌ Major rewriting required
- ❌ Harder to maintain
- ❌ Complex implementation

**Example**:
```markdown
[USER]
I want to create an OpenL table for calculating insurance premiums.

[ASSISTANT]
Great! Let me help you create a table. First, I need to understand your requirements:

1. What type of logic do you need?
   - Conditional logic (if-then rules)
   - Calculations (multi-step formulas)
   - Data storage

[USER]
Conditional logic with different rates for different driver types.

[ASSISTANT]
Perfect! For conditional logic, I recommend a **Decision Table**.
OpenL supports 5 types...
```

**Estimated Time**: 20-30 hours
**Risk**: High
**Rewriting**: Complete

---

## Recommended Implementation Plan ⭐

### Phase 1: Minimal Implementation (Week 1)

**Approach 1** - Expose existing prompts as-is

**Tasks**:
1. ✅ Create `src/prompts-registry.ts` with PROMPTS array
2. ✅ Add `prompts: {}` capability to server
3. ✅ Register `ListPromptsRequestSchema` handler
4. ✅ Register `GetPromptRequestSchema` handler
5. ✅ Load markdown files at runtime
6. ✅ Test with MCP Inspector
7. ✅ Update documentation

**Deliverables**:
- 11 prompts exposed via MCP
- Works with Claude Desktop immediately
- No prompt file changes needed

**Success Criteria**:
- MCP Inspector shows 11 prompts
- Each prompt returns full markdown content
- Claude Desktop can use prompts

---

### Phase 2: Enhanced Arguments (Week 2) - OPTIONAL

**Approach 2** - Add argument support for contextual prompts

**Tasks**:
1. Add YAML frontmatter to prompt files (prioritize by usage):
   - `create_test.md` - needs table context
   - `execute_rule.md` - needs rule name
   - `deploy_project.md` - needs project ID
2. Implement argument substitution in `loadPromptContent()`
3. Update PROMPTS registry with argument definitions
4. Test dynamic prompts with variables

**Deliverables**:
- 3-5 prompts support arguments
- Dynamic context injection works
- Better user experience

---

### Phase 3: Advanced Features (Future) - OPTIONAL

**Potential Enhancements**:
- Multi-turn conversation prompts
- Prompt versioning
- User-defined prompts
- Prompt templates library

---

## File-by-File Modification Assessment

### No Changes Needed (Approach 1) ✅

All 11 files work as-is:
- `create_rule.md` ✅
- `create_test.md` ✅
- `datatype_vocabulary.md` ✅
- `dimension_properties.md` ✅
- `execute_rule.md` ✅
- `deploy_project.md` ✅
- `get_project_errors.md` ✅
- `file_history.md` ✅
- `project_history.md` ✅
- `run_test.md` ✅
- `update_test.md` ✅

### Minor Changes (Approach 2) - Optional

Add frontmatter to these 5 high-value prompts:

1. **`create_test.md`** - Add `tableName`, `tableType` arguments
2. **`execute_rule.md`** - Add `ruleName`, `projectId` arguments
3. **`deploy_project.md`** - Add `projectId`, `environment` arguments
4. **`run_test.md`** - Add `tableIds`, `scope` arguments
5. **`update_test.md`** - Add `testId`, `tableName` arguments

**Example Enhancement**:
```diff
+ ---
+ name: create_test
+ description: Guide for creating OpenL test tables
+ arguments:
+   - name: tableName
+     description: Name of the table being tested
+     required: false
+ ---
+
  # Creating Test Tables in OpenL Tablets
+
+ {if tableName}
+ ## Creating Test for: {tableName}
+ {end if}
```

---

## Technical Implementation Details

### Prompt Registry Structure

```typescript
// src/prompts-registry.ts
import { readFileSync } from "fs";
import { join } from "path";

export interface PromptDefinition {
  name: string;
  title?: string;
  description?: string;
  arguments?: Array<{
    name: string;
    description: string;
    required?: boolean;
  }>;
}

export const PROMPTS: PromptDefinition[] = [
  {
    name: "create_rule",
    title: "Create OpenL Table",
    description: "Comprehensive guide for creating OpenL decision tables, spreadsheets, and datatypes with examples",
  },
  {
    name: "create_test",
    title: "Create Test Table",
    description: "Step-by-step guide for creating OpenL test tables with proper structure and validation",
    arguments: [
      { name: "tableName", description: "Name of the table being tested", required: false },
      { name: "tableType", description: "Type of table (Rules, SimpleRules, Spreadsheet, etc.)", required: false },
    ],
  },
  {
    name: "update_test",
    title: "Update Test Table",
    description: "Guide for modifying existing test tables, adding test cases, and updating expected values",
    arguments: [
      { name: "testId", description: "ID of the test table to update", required: false },
    ],
  },
  {
    name: "run_test",
    title: "Run Tests",
    description: "Test selection logic and workflow for running OpenL tests efficiently",
    arguments: [
      { name: "scope", description: "Test scope (single, multiple, all)", required: false },
    ],
  },
  {
    name: "datatype_vocabulary",
    title: "Define Datatypes",
    description: "Guide for creating custom datatypes and vocabularies in OpenL Tablets",
  },
  {
    name: "dimension_properties",
    title: "Dimension Properties",
    description: "Explanation of OpenL dimension properties for business versioning (state, lob, dates)",
  },
  {
    name: "execute_rule",
    title: "Execute Rule",
    description: "Guide for constructing test data and executing OpenL rules",
    arguments: [
      { name: "ruleName", description: "Name of the rule to execute", required: false },
    ],
  },
  {
    name: "deploy_project",
    title: "Deploy Project",
    description: "OpenL deployment workflow with validation checks and environment selection",
    arguments: [
      { name: "projectId", description: "ID of project to deploy", required: false },
      { name: "environment", description: "Target environment (dev, test, staging, prod)", required: false },
    ],
  },
  {
    name: "get_project_errors",
    title: "Analyze Errors",
    description: "OpenL error analysis workflow with pattern matching and fix recommendations",
    arguments: [
      { name: "projectId", description: "ID of project to analyze", required: false },
    ],
  },
  {
    name: "file_history",
    title: "File History",
    description: "Guide for viewing Git-based file version history in OpenL",
    arguments: [
      { name: "filePath", description: "Path to file", required: false },
    ],
  },
  {
    name: "project_history",
    title: "Project History",
    description: "Guide for viewing project-wide Git commit history",
    arguments: [
      { name: "projectId", description: "ID of project", required: false },
    ],
  },
];

const promptsDir = join(__dirname, "..", "prompts");

export function loadPromptContent(name: string, args?: Record<string, string>): string {
  const filePath = join(promptsDir, `${name}.md`);
  let content = readFileSync(filePath, "utf-8");

  // Simple argument substitution
  if (args) {
    for (const [key, value] of Object.entries(args)) {
      // Replace {key} with value
      content = content.replace(new RegExp(`\\{${key}\\}`, "g"), value);

      // Process conditionals: {if key}...{end if}
      const ifPattern = new RegExp(`\\{if ${key}\\}([\\s\\S]*?)\\{end if\\}`, "g");
      content = content.replace(ifPattern, value ? "$1" : "");
    }
  }

  // Remove unused conditionals
  content = content.replace(/\{if \w+\}[\s\S]*?\{end if\}/g, "");

  return content;
}
```

### Server Integration

```typescript
// src/index.ts - Add these imports
import {
  ListPromptsRequestSchema,
  GetPromptRequestSchema,
} from "@modelcontextprotocol/sdk/types.js";
import { PROMPTS, loadPromptContent } from "./prompts-registry.js";

// Update capabilities
this.server = new Server(
  {
    name: SERVER_INFO.NAME,
    version: SERVER_INFO.VERSION,
  },
  {
    capabilities: {
      tools: {},
      resources: {},
      prompts: {},  // ✅ ADD THIS LINE
    },
  }
);

// Add to setupHandlers()
private setupHandlers(): void {
  // ... existing handlers ...

  // List available prompts
  this.server.setRequestHandler(ListPromptsRequestSchema, async () => ({
    prompts: PROMPTS,
  }));

  // Get specific prompt with optional arguments
  this.server.setRequestHandler(GetPromptRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;

    const prompt = PROMPTS.find(p => p.name === name);
    if (!prompt) {
      throw new McpError(
        ErrorCode.InvalidRequest,
        `Prompt not found: ${name}`
      );
    }

    const content = loadPromptContent(name, args);

    return {
      description: prompt.description,
      messages: [
        {
          role: "assistant",
          content: {
            type: "text",
            text: content,
          },
        },
      ],
    };
  });
}
```

---

## Testing Strategy

### Unit Tests
```typescript
// tests/prompts.test.ts (NEW FILE)
describe("Prompts", () => {
  test("should load all 11 prompts", () => {
    expect(PROMPTS).toHaveLength(11);
  });

  test("should load create_rule prompt content", () => {
    const content = loadPromptContent("create_rule");
    expect(content).toContain("Creating Tables in OpenL Tablets");
  });

  test("should substitute arguments", () => {
    const content = loadPromptContent("create_test", {
      tableName: "calculatePremium"
    });
    expect(content).toContain("calculatePremium");
  });
});
```

### MCP Inspector Testing
1. Start MCP server
2. Open MCP Inspector
3. Navigate to "Prompts" tab
4. Verify all 11 prompts listed
5. Test each prompt:
   - Click prompt name
   - Verify content loads
   - Test with/without arguments

### Claude Desktop Testing
1. Configure MCP server in Claude Desktop
2. Start conversation
3. Type "Use prompt: create_rule"
4. Verify prompt content injected
5. Test interactive workflow

---

## Documentation Updates

### Update README.md
```markdown
## Prompts

The MCP server provides 11 expert prompts for OpenL Tablets workflows:

- **create_rule** - Guide for creating OpenL tables (decision tables, spreadsheets, datatypes)
- **create_test** - Creating test tables with proper structure
- **update_test** - Modifying existing tests
- **run_test** - Test selection and execution logic
- **datatype_vocabulary** - Defining custom datatypes and enumerations
- **dimension_properties** - Business versioning with dimension properties
- **execute_rule** - Constructing test data and executing rules
- **deploy_project** - Deployment workflow with validation
- **get_project_errors** - Error analysis and pattern matching
- **file_history** - Git-based file versioning
- **project_history** - Project-wide commit history

Access prompts via MCP Inspector or Claude Desktop.
```

### Create PROMPTS.md Documentation
- List all prompts
- Describe when to use each
- Show examples
- Document arguments

---

## Success Metrics

### Phase 1 Success
- ✅ All 11 prompts visible in MCP Inspector
- ✅ Each prompt returns valid content
- ✅ Claude Desktop can use prompts
- ✅ No errors in server logs
- ✅ Unit tests pass

### Phase 2 Success (if implemented)
- ✅ 5 high-value prompts support arguments
- ✅ Dynamic context injection works
- ✅ Argument validation prevents errors

---

## Risk Assessment

### Low Risk ✅
- **Approach 1** implementation
- No breaking changes
- Existing files work as-is
- Fast rollback (remove handlers)

### Medium Risk ⚠️
- **Approach 2** with arguments
- Requires file modifications
- Need testing for variable substitution
- More complex error handling

### High Risk ❌
- **Approach 3** multi-turn
- Major rewriting
- Complex maintenance
- Not recommended for V1

---

## Conclusion & Recommendation

### Recommended Path: **Approach 1** (Minimal Implementation)

**Why**:
1. ✅ **Fast implementation** - 2-3 hours
2. ✅ **Zero risk** - No file changes
3. ✅ **Immediate value** - All prompts available
4. ✅ **Easy maintenance** - Just add new .md files
5. ✅ **Works today** - Existing files are high quality

**Next Steps**:
1. Get approval for Approach 1
2. Implement prompts registry
3. Register MCP handlers
4. Test with MCP Inspector
5. Document prompts
6. (Optional) Add arguments in Phase 2

**Timeline**:
- Phase 1: 2-3 hours (can complete today)
- Testing: 1 hour
- Documentation: 1 hour
- **Total: 4-5 hours**

---

## Questions for Decision

1. **Proceed with Approach 1?** (Minimal, no file changes)
2. **Skip arguments for now?** (Add later if needed)
3. **Any specific prompts to prioritize?** (If implementing arguments)
4. **Claude Desktop testing available?** (For validation)

