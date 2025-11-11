# OpenL Tablets MCP - Corrected Understanding

## Executive Summary

After thorough review of OpenL Tablets documentation, I now understand there are **TWO INDEPENDENT VERSIONING SYSTEMS** and **FIVE DIFFERENT DECISION TABLE TYPES** that my implementation must support correctly.

---

## PART 1: TWO INDEPENDENT VERSIONING SYSTEMS

### System 1: Git-Based Repository Versioning

**Purpose**: Track changes to project files over time through version control

**Implementation**:
- Every save/upload operation creates a Git commit
- Version identifier = Git commit hash (e.g., "7a3f2b1c3e...")
- File names **never change** between versions
- Same file path (e.g., `rules/Insurance.xlsx`) exists across all commits with different content

**Data Structure**:
```typescript
interface FileData {
  name: string;           // File path: "rules/Insurance.xlsx"
  version: string;        // Git commit hash: "7a3f2b1c3e..."
  author: UserInfo;       // Git author with name/email
  modifiedAt: string;     // ISO timestamp
  comment: string;        // Git commit message
  branch?: string;        // Git branch name
  size?: number;
  uniqueId?: string;
}
```

**Operations**:
- `listHistory(filePath)` → Returns array of FileData with commit hashes
- `getFileVersion(filePath, commitHash)` → Gets content at specific commit
- `save(project, comment)` → Creates new commit, returns FileData with new commit hash
- `revert(commitHash)` → Checks out previous commit

**Commit Message Structure**:
```
{user-message} Type: {commit-type}.

Types: SAVE, ARCHIVE, RESTORE, ERASE, MERGE
```

**Backend Variations**:
- **GitRepository**: Native Git commits with JGit
- **Azure Blob**: Git-like versioning emulated via `.versions/versions.yaml` metadata
- **AWS S3**: Bucket-level versioning with S3-generated version IDs
- **FileSystem**: No versioning support
- **Database**: Optional version history table

### System 2: OpenL Core Dimension Properties Versioning

**Purpose**: Support multiple versions of the SAME rule with identical signatures but different business contexts

**Implementation**:
- Business dimension properties define when/where rules apply
- Multiple rule versions coexist with same name and signature
- Runtime context selects appropriate version based on request properties
- Applied at multiple levels: table, category (sheet), module, file name pattern

**Core Dimension Properties**:

| Property | Purpose | Example Values |
|----------|---------|----------------|
| **effectiveDate** | When rule becomes legally active | 2025-01-01 |
| **expirationDate** | When rule expires | 2025-12-31 |
| **startRequestDate** | When rule is introduced operationally | 2025-01-15 |
| **endRequestDate** | When rule is retired from system | 2025-12-15 |
| **lob** | Line of Business | "Auto", "Home", "Life" |
| **state** | US State | "CA", "NY", "TX", "CW" (country-wide) |
| **country** | Country code | "US", "CA", "UK" |
| **currency** | Currency code | "USD", "EUR", "GBP" |
| **region** | US Region | "West", "East", "South" |
| **usregion** | Detailed US region | "Pacific", "Mountain" |
| **language** | Language code | "en", "es", "fr" |
| **caProvince** | Canadian Province | "ON", "BC", "QC" |
| **origin** | Base vs Deviation | "Base", "Deviation" |
| **nature** | User-defined business meaning | Custom values |
| **version** | Scenario version | "v1", "v2", "draft" |
| **active** | Active flag | true/false |

**File Name Patterns**:

Configured in `rules.xml`:
```xml
<properties-file-name-pattern>.*-%state%-%effectiveDate:MMddyyyy%-%lob%</properties-file-name-pattern>
```

**Examples**:
- `InsuranceRules-CA-01012025-Auto.xlsx` (California, effective Jan 1 2025, Auto LOB)
- `InsuranceRules-TX-06152025-Home.xlsx` (Texas, effective Jun 15 2025, Home LOB)
- `InsuranceRules-CW-01012025-Life.xlsx` (Country-wide, effective Jan 1 2025, Life LOB)

**Date Format Specifiers**:
- `%propertyName:dateFormat%` where dateFormat uses Java SimpleDateFormat
- Default: `yyyyMMdd`
- Examples: `MMddyyyy`, `ddMMyyyy`, `yyyy-MM-dd`

**Special Values**:
- **Any**: Matches any value for enumeration properties
- **CW** (Country-Wide): Replaces all US state values
- **Comma-separated**: Multiple values for array properties: `Product-CRV,MTH,STR-01022018.xlsx`

**Table-Level Properties**:

In Excel, immediately after table header:
```
Rules int calculatePremium(String state, int age)
properties
effectiveDate    01/01/2025
state            CA
lob              Auto
```

**Properties Table** (Category/Module Level):

```
Properties
scope       Category
category    Pricing
effectiveDate    01/01/2025
lob         Auto
```

**Runtime Selection Logic**:

When multiple versioned rules exist:
1. Filters rules matching dimension properties (date ranges, LOB, state, etc.)
2. Applies priority hierarchy:
   - Table-level properties (highest)
   - Category-level properties
   - Module-level properties
   - Default values (lowest)
3. For request dates: Selects latest StartRequestDate, then earliest EndRequestDate
4. For origin: Deviation takes precedence over Base
5. Handles overlap validation (No overlap, Good overlap with embedding, Bad overlap = error)

**Overlap Types**:
- **No Overlap**: Distinct property sets, one rule applies
- **Good Overlap**: One rule has most detailed (embedded) property set, takes precedence
- **Bad Overlap**: Conflicting sets with no clear hierarchy → error

**Version/Active Properties**:

Alternative scenario tracking:
```
Properties
version     Q1_2025_Scenario
active      true
```

Multiple versions of same table can coexist; only one marked active.

### How Both Systems Work Together

**Example Scenario**:

```
Repository Versioning (Git):
├─ Commit a1b2c3d... (Jan 1, 2025)
│  └─ rules/Insurance-CA-01012025-Auto.xlsx
│     ├─ calculatePremium(String state, int age) [effectiveDate: 01/01/2025, state: CA]
│     └─ calculatePremium(String state, int age) [effectiveDate: 06/01/2025, state: CA]
│
├─ Commit e4f5g6h... (Feb 1, 2025)
│  └─ rules/Insurance-CA-01012025-Auto.xlsx
│     ├─ calculatePremium(String state, int age) [effectiveDate: 01/01/2025, state: CA] (modified)
│     └─ calculatePremium(String state, int age) [effectiveDate: 06/01/2025, state: CA]
│
└─ Commit i7j8k9l... (Mar 1, 2025)
   └─ rules/Insurance-CA-01012025-Auto.xlsx
      ├─ calculatePremium(String state, int age) [effectiveDate: 01/01/2025, state: CA]
      ├─ calculatePremium(String state, int age) [effectiveDate: 06/01/2025, state: CA]
      └─ calculatePremium(String state, int age) [effectiveDate: 09/01/2025, state: CA] (added)
```

**Key Points**:
1. **Git commits** track file changes over time (a1b2c3d → e4f5g6h → i7j8k9l)
2. **Within each commit**, multiple rule versions coexist based on dimension properties
3. **File name** stays the same (`Insurance-CA-01012025-Auto.xlsx`) with properties encoded
4. **Runtime**: OpenL selects appropriate commit (latest or specific) AND appropriate rule version based on request context

---

## PART 2: FIVE DECISION TABLE TYPES

My original analysis only described the **Rules Table** structure. OpenL Tablets actually supports **5 different decision table types**, each with different Excel structures.

### Type 1: Rules Table (Standard Decision Table)

**Header Format**:
```
Rules <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure** (5+ rows):

```
Row 1: Rules int getPremium(String driverType, int age)
       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       Merged across all columns

Row 2: C1              C2          A1                  RET1
       ^^^             ^^^         ^^^                 ^^^^
       Column type markers

Row 3: driverType == ?    age < ?     premium = ?         premium
       ^^^^^^^^^^^^^^^    ^^^^^^^^    ^^^^^^^^^^^         ^^^^^^^
       Boolean expressions           Execute statements   Return expression

Row 4: String driverType  int age     int premium         int premium
       ^^^^^^^^^^^^^^^^^  ^^^^^^^^    ^^^^^^^^^^^         ^^^^^^^^^^^
       Parameter types and names

Row 5: Driver Type        Age         Set Premium         Final Premium
       ^^^^^^^^^^^        ^^^         ^^^^^^^^^^^         ^^^^^^^^^^^^^
       Business-friendly descriptions

Row 6: "SAFE"             25          800                 800
Row 7: "RISKY"            18          1500                1500
...
```

**Column Types**:
- `C1, C2, C3...` = Conditions (all must be true for row to execute)
- `A1, A2, A3...` = Actions (executed if all conditions true)
- `RET1` = Return value (first non-empty returned)
- `HC1, HC2...` = Horizontal conditions (for lookup tables)
- `MC1` = Merged rule column (for vertical result arrays)

**Features**:
- Most flexible and explicit
- Full control over condition expressions
- Supports complex Boolean logic
- Can have multiple action columns
- External condition/action/return definitions supported

### Type 2: Simple Rules Table

**Header Format**:
```
SimpleRules <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure**:

```
Row 1: SimpleRules int getPremium(String driverType, int age)

Row 2: [No column type markers - parameters match by POSITION]

Row 3: driverType == ?    age < ?     premium
       ^^^^^^^^^^^^^^^    ^^^^^^^^    ^^^^^^^
       Simple equality    Simple comparison    Return expression

Row 4: String             int         int
       ^^^^^^             ^^^         ^^^
       Parameter types only (names inferred from Row 1)

Row 5: Driver Type        Age         Premium
       ^^^^^^^^^^^        ^^^         ^^^^^^^
       Titles (optional for SimpleRules)

Row 6: "SAFE"             25          800
Row 7: "RISKY"            18          1500
...
```

**Key Differences from Rules Table**:
- **No C1/A1/RET1 markers** in Row 2
- **Positional parameter matching**: First condition column matches first input parameter
- **Simplified syntax**: Condition columns associate with input parameters in strict order
- **No explicit action columns**: Only conditions and return
- Titles are optional (parameters identified by position)

**When to Use**:
- Simple decision logic
- Parameters naturally map left-to-right
- No need for complex actions
- Faster to create than Rules table

### Type 3: Smart Rules Table

**Header Format**:
```
SmartRules <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure**:

```
Row 1: SmartRules int getPremium(String driverType, int age)

Row 2: [No column type markers - parameters match by NAME]

Row 3: driverType == ?    age < ?     premium
       ^^^^^^^^^^^^^^^    ^^^^^^^^    ^^^^^^^
       Conditions              Return

Row 4: String             int         int

Row 5: Driver Type        Age         Premium Value
       ^^^^^^^^^^^        ^^^         ^^^^^^^^^^^^^
       Titles MATCH parameter names automatically

Row 6: "SAFE"             25          800
Row 7: "RISKY"            18          1500
...
```

**Parameter Matching Algorithm**:
1. **Parse parameter name** into words (capital letters start new words)
   - `driverType` → ["driver", "Type"]
   - `age` → ["age"]
2. **Parse column title** into words
   - "Driver Type" → ["Driver", "Type"]
   - "Age" → ["Age"]
3. **Calculate matching percentage** (word overlap)
   - "Driver Type" matches `driverType`: 100% (2/2 words)
   - "Age" matches `age`: 100% (1/1 word)
4. **Select highest match**; throw error if ambiguous

**Abbreviation Support**:
- "Dr Type" matches `driverType` (abbreviation recognized)
- "Drv Tp" matches `driverType` (multiple abbreviations)

**Field-Level Matching**:
For complex object parameters:
- `policy.premium` matches title "Policy Premium" or "Premium"
- Matches nested fields automatically

**Collect Keyword**:
```
SmartRules String[] Collect getWarnings(Policy policy)
```
Collects all matching rule results into an array.

**When to Use**:
- Column order may change
- Parameters have descriptive names
- Want flexibility in Excel layout
- Working with complex objects (field matching)

### Type 4: Simple Lookup Table

**Header Format**:
```
SimpleLookup <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure** (Combines vertical and horizontal conditions):

```
Row 1: SimpleLookup int getPremium(String risk, int age)

Row 2: HC1   HC2   HC3   HC4   HC5
       C1    18    25    35    45    65
             ^^^   ^^^   ^^^   ^^^   ^^^
             Horizontal condition values (age ranges)

Row 3:       RET1  RET1  RET1  RET1  RET1

Row 4: risk  age   age   age   age   age
       ^^^^  <     <     <     <     <
       Vertical   Horizontal conditions (equality checks)

Row 5: Risk  Age18 Age25 Age35 Age45 Age65

Row 6: "LOW"   400   500   600   700   800
Row 7: "MED"   800  1000  1200  1400  1600
Row 8: "HIGH" 1200  1500  1800  2000  2200
```

**How It Works**:
1. **Vertical conditions** (left columns): Check equality
   - `risk == "LOW"` or `risk == "MED"` or `risk == "HIGH"`
2. **Horizontal conditions** (top rows): Check equality or range
   - `age < 18` or `age < 25` or `age < 35`, etc.
3. **Return value**: At intersection of matching row and column

**Parameter Association**:
- First `N` parameters → vertical conditions (left to right)
- Next `M` parameters → horizontal conditions (left to right)
- Height of first column title cell determines how many HC rows

**Supports**:
- Ranges: `[1-10]`, `(0-100)`, `[1-10)`, `(0-100]`
- Arrays: `{"CA", "NY", "TX"}` (matches if parameter in array)

**When to Use**:
- Two-dimensional lookup tables
- Cross-reference tables
- Rate tables varying by two factors

### Type 5: Smart Lookup Table

**Header Format**:
```
SmartLookup <ReturnType> ruleName(<ParamType1> param1, <ParamType2> param2, ...)
```

**Excel Structure**:

```
Row 1: SmartLookup int getPremium(String riskLevel, int driverAge)

Row 2: HC1        HC2   HC3   HC4   HC5
       C1         18    25    35    45    65

Row 3:            RET1  RET1  RET1  RET1  RET1

Row 4: riskLevel  age   age   age   age   age

Row 5: Risk Level Age18 Age25 Age35 Age45 Age65
       ^^^^^^^^^^
       Matches "riskLevel" parameter by NAME (not position)

Row 6: "LOW"        400   500   600   700   800
Row 7: "MEDIUM"     800  1000  1200  1400  1600
Row 8: "HIGH"      1200  1500  1800  2000  2200
```

**Parameter Matching**:
- **Vertical conditions**: Match by title (like SmartRules)
  - "Risk Level" matches `riskLevel` parameter
- **Horizontal conditions**: Select from remaining input parameters (positional)
  - First remaining parameter after vertical matches
- **Smart matching algorithm**: Same word-splitting logic as SmartRules

**Condition Types**:
- **Equality**: `"LOW"` matches exact value
- **Inclusion**: `["CA", "NY"]` matches if parameter in array
- **Ranges**: `[0-100]` matches if parameter in range

**Collect Support**:
```
SmartLookup String[] Collect getApplicableRules(...)
```

**When to Use**:
- Lookup table with flexible column ordering
- Parameters have descriptive names
- Want automatic parameter matching
- Two-dimensional decision logic

---

## Decision Table Type Comparison

| Feature | Rules | SimpleRules | SmartRules | SimpleLookup | SmartLookup |
|---------|-------|-------------|------------|--------------|-------------|
| **Column Markers** | C1, A1, RET1 | None | None | HC1, C1, RET1 | HC1, C1, RET1 |
| **Parameter Matching** | Explicit | Positional | By Name | Positional | Vertical: Name, Horizontal: Position |
| **Action Columns** | Yes (A1, A2...) | No | No | No | No |
| **Complex Conditions** | Yes | Simple only | Simple only | Equality/Inclusion | Equality/Inclusion |
| **Horizontal Conditions** | Via HC columns | No | No | Yes | Yes |
| **Collect Keyword** | Yes | Yes | Yes | Yes | Yes |
| **Flexibility** | Highest | Low | High | Medium | Highest |
| **Ease of Use** | Complex | Easy | Medium | Medium | Easy |
| **Excel Layout Freedom** | Fixed | Fixed | Flexible | Fixed | Flexible |

---

## Impact on MCP Implementation

### Tools That Need Correction

#### 1. `version_file` - REMOVE ENTIRELY ❌

This tool is based on hallucination. There is no "copy file and increment version number" operation.

**Reality**:
- Git commits provide repository versioning
- Dimension properties provide rule versioning
- No file renaming for versions

**Replace with**:
- `get_file_history` - List Git commits for a file
- `get_file_version` - Download specific commit version
- File name patterns are for dimension properties, not sequential versions

#### 2. `create_rule` - UPDATE for 5 table types ⚠️

Current implementation doesn't specify which decision table type to create.

**Correct Implementation**:
```typescript
async createRule(request: {
  projectId: string;
  tableName: string;
  tableType: "Rules" | "SimpleRules" | "SmartRules" | "SimpleLookup" | "SmartLookup";
  returnType: string;
  parameters: Array<{ type: string; name: string }>;

  // For Rules, SimpleRules, SmartRules
  conditions?: Array<{
    expression: string;      // e.g., "driverType == ?"
    paramType?: string;      // e.g., "String" (for Rules)
    paramName?: string;      // e.g., "driverType" (for Rules)
    title?: string;          // e.g., "Driver Type"
  }>;

  // For SimpleLookup, SmartLookup
  verticalConditions?: Array<{ ... }>;
  horizontalConditions?: Array<{ ... }>;

  // For Rules table only
  actions?: Array<{
    expression: string;      // e.g., "premium = basePremium * 0.8"
    paramType: string;
    paramName: string;
    title: string;
  }>;

  returnColumn: {
    expression: string;      // e.g., "premium" or calculation
    paramType?: string;      // For Rules table
    paramName?: string;      // For Rules table
    title?: string;
  };

  // Dimension properties (optional)
  properties?: {
    effectiveDate?: string;
    expirationDate?: string;
    startRequestDate?: string;
    endRequestDate?: string;
    state?: string;
    lob?: string;
    [key: string]: any;
  };

  file?: string;
  comment?: string;
}): Promise<CreateRuleResult>
```

#### 3. Update `save_project` - Return commit hash ⚠️

```typescript
export interface SaveProjectResult {
  success: boolean;
  commitHash: string;        // Git commit hash
  version: string;           // Same as commitHash
  author: {
    name: string;
    email: string;
  };
  timestamp: string;
  message?: string;
  validationErrors?: ValidationError[];
}
```

#### 4. Update `upload_file` / `download_file` - Version support ⚠️

```typescript
// Upload returns commit hash
export interface FileUploadResult {
  success: boolean;
  fileName: string;
  commitHash: string;
  version: string;    // Same as commitHash
  size?: number;
  author: { name: string; email: string };
  timestamp: string;
  message?: string;
}

// Download accepts optional version (commit hash)
async downloadFile(request: {
  projectId: string;
  fileName: string;
  version?: string;  // Git commit hash to download
}): Promise<{
  content: Buffer;
  metadata: FileData;
}>
```

#### 5. Update `compare_versions` - Use commit hashes ⚠️

```typescript
export const compareVersionsSchema = z.object({
  projectId: projectIdSchema,
  baseCommitHash: z.string().describe("Base Git commit hash (e.g., '7a3f2b1c...')"),
  targetCommitHash: z.string().describe("Target Git commit hash (e.g., '9e5d8a2f...')"),
});
```

#### 6. Update `revert_version` - Checkout commit ⚠️

```typescript
export const revertVersionSchema = z.object({
  projectId: projectIdSchema,
  commitHash: z.string().describe("Git commit hash to revert to"),
  comment: commentSchema,
});
```

#### 7. Add NEW tools for dimension properties 🆕

```typescript
// Get file name pattern from rules.xml
async getFileNamePattern(projectId: string): Promise<string>

// Set file name pattern in rules.xml
async setFileNamePattern(projectId: string, pattern: string): Promise<void>

// Get table properties
async getTableProperties(projectId: string, tableId: string): Promise<Record<string, any>>

// Set table properties
async setTableProperties(
  projectId: string,
  tableId: string,
  properties: Record<string, any>
): Promise<void>

// List all rule versions (different dimension property combinations)
async listRuleVersions(
  projectId: string,
  ruleName: string
): Promise<Array<{
  signature: string;
  properties: Record<string, any>;
  tableId: string;
  file: string;
}>>
```

### Prompt Templates to Update

#### `create_rule.md` - Explain all 5 table types

```markdown
# Creating Rules in OpenL Tablets

You are helping the user create a decision table. OpenL Tablets supports 5 different decision table types:

## 1. Rules Table (Standard)
- **Use when**: Need complex Boolean conditions, multiple action columns, or full control
- **Format**: `Rules <ReturnType> ruleName(<params>)`
- **Structure**: Explicit column markers (C1, C2, A1, RET1)
- **Example**: Insurance premium with complex risk calculations

## 2. Simple Rules Table
- **Use when**: Simple decision logic, parameters map left-to-right
- **Format**: `SimpleRules <ReturnType> ruleName(<params>)`
- **Structure**: No column markers, positional parameter matching
- **Example**: Discount calculation based on customer tier and amount

## 3. Smart Rules Table
- **Use when**: Want flexible column ordering, descriptive parameter names
- **Format**: `SmartRules <ReturnType> ruleName(<params>)`
- **Structure**: No column markers, matches parameters by title
- **Example**: Policy validation with many parameters

## 4. Simple Lookup Table
- **Use when**: Two-dimensional lookup (like rate table)
- **Format**: `SimpleLookup <ReturnType> ruleName(<params>)`
- **Structure**: Horizontal conditions (HC1, HC2...) across top, vertical on left
- **Example**: Premium rates varying by risk level and age bracket

## 5. Smart Lookup Table
- **Use when**: Two-dimensional lookup with flexible parameter matching
- **Format**: `SmartLookup <ReturnType> ruleName(<params>)`
- **Structure**: Horizontal conditions with smart vertical matching
- **Example**: Tax rates by state and income bracket

Ask the user which type best fits their needs based on their business logic.

## Dimension Properties

After creating the table, ask if they want to add dimension properties for versioning:

- **effectiveDate/expirationDate**: When rule is legally valid
- **startRequestDate/endRequestDate**: When rule is operationally used
- **state**: US state (CA, NY, TX, CW=country-wide)
- **lob**: Line of Business (Auto, Home, Life)
- **currency, country, language**: Localization
- **version/active**: Scenario tracking

These properties allow multiple versions of the same rule to coexist, with OpenL selecting the appropriate version at runtime based on context.
```

#### `version_file.md` - DELETE and replace with `file_history.md`

```markdown
# Working with File History in OpenL Tablets

OpenL Tablets uses **Git for version control**. Every time you save a project or upload a file, a Git commit is created automatically.

## Version Identifier = Git Commit Hash

Versions are identified by Git commit hashes (e.g., "7a3f2b1c3e..."), not sequential numbers like v1, v2, v3.

## Operations

### View File History
Lists all commits that modified a specific file, showing:
- Commit hash (version identifier)
- Author name and email
- Timestamp
- Commit message
- Commit type (SAVE, ARCHIVE, RESTORE, ERASE, MERGE)

### Download Specific Version
Retrieves file content from a specific commit using the commit hash.

### Compare Versions
Shows differences between two commits using their commit hashes.

### Revert to Previous Version
Checks out a previous commit, creating a new commit that restores the old state.

## Dimension Properties Versioning (Separate System)

OpenL ALSO supports **business rule versioning** through dimension properties:
- Multiple versions of the same rule in ONE file
- Differentiated by properties (state, lob, effectiveDate, etc.)
- Runtime selection based on request context
- File name patterns can encode properties: `Rules-%state%-%lob%.xlsx`

These two systems are INDEPENDENT:
1. Git tracks file changes over time
2. Dimension properties track business rule variants within each file version
```

---

## Conclusion

My implementation had two major categories of errors:

### Versioning Errors
1. **Hallucinated file naming versions** (v1 → v2 → v3) instead of Git commits
2. **Missed dimension properties versioning** completely
3. **Didn't understand** the two systems work independently

### Decision Table Errors
1. **Only knew about Rules table** structure
2. **Missed 4 other table types** (SimpleRules, SmartRules, SimpleLookup, SmartLookup)
3. **Didn't understand** parameter matching differences (positional vs name-based)
4. **Didn't know about** horizontal conditions (lookup tables)

The corrected implementation must:
- Use Git commit hashes for repository versioning
- Support dimension properties for business rule versioning
- Support all 5 decision table types with their specific structures
- Update all tools to return/accept commit hashes
- Add new tools for dimension properties management
- Completely rewrite prompt templates with correct concepts
