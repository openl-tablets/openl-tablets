# OpenL Tablets MCP Server - Comprehensive Tool Plan

## Business Rules Management Context

OpenL Tablets is an Excel-based Business Rules Management System where:
- **Rules are stored in Excel files** (.xlsx, .xls) with specific table formats
- **Rule versioning uses file naming conventions** (e.g., `Policy_2023_01.xlsx`, `Policy_2023_02.xlsx`)
- **Rule types include**: Decision Tables, Spreadsheets, Datatypes, Test Tables, Data Tables
- **Workflow**: Develop → Test → Validate → Save → Deploy
- **WebStudio**: Web-based IDE for rules management

## Tool Architecture Strategy

### Prompt-Enhanced Tools
Some tools will use **prompt templates** to provide AI assistants with contextual understanding:
- Rule type selection (Decision Table vs Spreadsheet vs Datatype)
- Version naming conventions
- Test selection based on changed rules
- Error interpretation and fixing suggestions

### API-Combined Tools
Tools will combine multiple OpenL REST API calls to provide complete business operations:
- Save project = validate + create version + save
- Revert version = get old version + open + validate + save as new version

---

## Final Tool Set (23 Tools)

### Category 1: Repository Management (2 tools)
Purpose: Browse and navigate design repositories where rules are stored

#### 1. `list_repositories`
**Purpose**: List all design repositories containing rules projects
**Business Value**: Find where rules projects are stored
**API**: `GET /design-repositories`
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ KEEP

#### 2. `list_branches`
**Purpose**: List branches in a repository for parallel development
**Business Value**: See different versions of rules (dev, staging, production branches)
**API**: `GET /design-repositories/{repo}/branches`
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ KEEP

---

### Category 2: Project Management (5 tools)
Purpose: Manage rules projects (collections of Excel files with business rules)

#### 3. `list_projects`
**Purpose**: List all rules projects across repositories with filters
**Business Value**: Find projects to work with
**API**: `GET /design-repositories/projects`
**Filters**: repository, status, tag
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ KEEP

#### 4. `get_project`
**Purpose**: Get comprehensive project details including modules, dependencies, last modified info
**Business Value**: Understand project structure and who made recent changes
**API**: Combine `GET /design-repositories/{repo}/projects/{project}` + `GET /design-repositories/{repo}/projects/{project}/info`
**Prompt Enhancement**: None needed
**Notes**: Merge with `get_project_info` into single comprehensive tool
**Keep/Remove**: ✅ KEEP (merge get_project + get_project_info)

#### 5. `open_project`
**Purpose**: Open project for editing (locks the project)
**Business Value**: Required before modifying any rules
**API**: `POST /design-repositories/{repo}/projects/{project}/open`
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ KEEP

#### 6. `close_project`
**Purpose**: Close project after editing (releases lock)
**Business Value**: Release lock when done editing
**API**: `POST /design-repositories/{repo}/projects/{project}/close`
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ KEEP

#### 7. `save_project`
**Purpose**: Save project creating a new version in repository
**Business Value**: Persist changes to repository, create audit trail
**API**: Combine: validate_project + save + close
**Workflow**:
  1. Run `validate_project` to check for errors
  2. If errors exist, return them and block save
  3. If no errors, create new version
  4. Return save confirmation with new version info
**Prompt Enhancement**:
  - "Before saving, the project will be validated automatically"
  - "If errors exist, you'll need to fix them before saving"
**Keep/Remove**: ✅ ADD NEW

---

### Category 3: File Management (2 tools)
Purpose: Manage Excel files containing business rules

#### 8. `upload_file`
**Purpose**: Upload Excel file with rules to project (only .xlsx, .xls)
**Business Value**: Import rules edited externally or created by other tools
**API**: `POST /design-repositories/{repo}/projects/{project}/files`
**File Types**: Only Excel files (.xlsx, .xls)
**Prompt Enhancement**:
  - "Only Excel files (.xlsx, .xls) containing OpenL rules tables are supported"
  - "File will be parsed and validated for OpenL table formats"
**Notes**: Validate file extension before upload
**Keep/Remove**: ✅ ADD NEW

#### 9. `download_file`
**Purpose**: Download Excel file from project
**Business Value**: Edit rules in external Excel editor, backup, analysis
**API**: `GET /design-repositories/{repo}/projects/{project}/files/{filename}`
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ ADD NEW

---

### Category 4: Rules Management (6 tools)
Purpose: Create, read, update rules (tables) within Excel files

#### 10. `list_tables`
**Purpose**: List all rules/tables in a project with filters
**Business Value**: Find specific rules to work with
**API**: `GET /design-repositories/{repo}/projects/{project}/tables`
**Filters to ADD**:
  - `tableType`: Filter by type (simplerules, spreadsheet, datatype, test, data, method)
  - `name`: Filter by table name pattern
  - `file`: Filter by Excel file name
**Prompt Enhancement**:
  - Explain table types: "Decision Table (simplerules), Spreadsheet (spreadsheet), Datatype (datatype), Test Table (test)"
**Keep/Remove**: ✅ KEEP (add filter support)

#### 11. `get_table`
**Purpose**: Get detailed rule/table structure and data
**Business Value**: View rule logic, understand structure
**API**: `GET /design-repositories/{repo}/projects/{project}/tables/{tableId}`
**Returns**: Full table view with properties, columns, rows, fields
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ KEEP

#### 12. `update_table`
**Purpose**: Update rule/table content
**Business Value**: Modify business logic
**API**: `PUT /design-repositories/{repo}/projects/{project}/tables/{tableId}`
**Input**: Modified table view object
**Prompt Enhancement**:
  - "Ensure table structure matches OpenL format"
  - "Provide comment explaining the change"
**Keep/Remove**: ✅ KEEP

#### 13. `create_rule`
**Purpose**: Create new rule/table in project
**Business Value**: Add new business logic
**API**: `POST /design-repositories/{repo}/projects/{project}/tables`
**Prompt Enhancement**: CRITICAL - Use prompt to help select rule type
```
Prompt Template:
"What type of rule do you want to create?

1. Decision Table (simplerules) - Most common
   Use for: IF-THEN rules, condition-action logic
   Example: Calculate insurance premium based on age, risk, coverage

2. Spreadsheet (spreadsheet) - Calculations
   Use for: Complex calculations, formulas
   Example: Financial calculations, scoring models

3. Datatype (datatype) - Data structures
   Use for: Define custom data types used in rules
   Example: Customer, Policy, Claim structures

4. Test Table (test) - Unit tests
   Use for: Test your rules with sample data
   Example: Test premium calculation with various inputs

5. Data Table (data) - Reference data
   Use for: Lookup tables, configuration data
   Example: State codes, rate tables

6. Method Table (method) - Functions
   Use for: Reusable logic, helper functions
   Example: Date calculations, validations

Based on your description, I recommend: [TYPE]"
```
**Keep/Remove**: ✅ ADD NEW

#### 14. `copy_table`
**Purpose**: Copy/duplicate rule for versioning or modification
**Business Value**: Create rule variants, version rules at table level
**API**: `POST /design-repositories/{repo}/projects/{project}/tables/{tableId}/copy`
**Prompt Enhancement**: Critical - Understand versioning intent
```
Prompt Template:
"Are you versioning:
1. The entire Excel file (file-level versioning) - Recommended
   → Use file naming: Policy_2024_v1.xlsx → Policy_2024_v2.xlsx

2. Just this specific rule/table (table-level versioning)
   → Copy table within same file or to new file

File-level versioning is recommended as it maintains consistency across related rules."
```
**Keep/Remove**: ✅ ADD NEW

#### 15. `version_file`
**Purpose**: Create new version of entire Excel file with rules
**Business Value**: Version all rules in file together (OpenL best practice)
**API**: Combine copy file + update version properties
**Workflow**:
  1. Parse current file name for version (e.g., `Policy_2023_v1.xlsx`)
  2. Suggest next version (e.g., `Policy_2023_v2.xlsx` or `Policy_2024_v1.xlsx`)
  3. Copy file with new name
  4. Update version properties in rules.xml
**Prompt Enhancement**: CRITICAL - Version naming convention
```
Prompt Template:
"Current file: {filename}
Detected version pattern: {pattern}

Suggested next versions:
1. Increment patch: {filename_v2} (minor changes)
2. New period: {filename_2024_v1} (yearly/monthly version)
3. Custom: [enter version]

OpenL best practice: Use file naming for versioning
Pattern: {RuleName}_{Period}_{Version}.xlsx
Example: InsurancePolicy_2024_v1.xlsx"
```
**Keep/Remove**: ✅ ADD NEW

---

### Category 5: Testing & Validation (4 tools)
Purpose: Validate rules correctness and run tests

#### 16. `run_test`
**Purpose**: Run specific tests based on changed rules or test selection
**Business Value**: Quick feedback on specific rule changes
**API**: `POST /design-repositories/{repo}/projects/{project}/tests/run`
**Parameters**:
  - `testIds`: Array of test table IDs to run
  - `tableIds`: Run tests related to specific rule tables
**Prompt Enhancement**: CRITICAL - Smart test selection
```
Prompt Template:
"Select tests to run:

Based on your changes to: {modified_tables}

Recommended tests:
{list of related test tables}

Options:
1. Run only affected tests (faster) - Recommended
2. Run all tests (comprehensive)
3. Select specific tests

Running tests before save ensures rules work correctly."
```
**Keep/Remove**: ✅ ADD NEW

#### 17. `run_all_tests`
**Purpose**: Run all tests in project (pre-save validation)
**Business Value**: Comprehensive validation before saving/deploying
**API**: `POST /design-repositories/{repo}/projects/{project}/tests/run-all`
**Returns**: TestSuiteResult with pass/fail counts and details
**Prompt Enhancement**:
  - "Running all tests - this may take a few moments for large projects"
  - "Tests should pass before saving changes"
**Auto-trigger**: Before `save_project`
**Keep/Remove**: ✅ KEEP (already added)

#### 18. `validate_project`
**Purpose**: Check project for syntax errors, compilation errors, warnings
**Business Value**: Prevent invalid rules from being saved
**API**: `GET /design-repositories/{repo}/projects/{project}/validation`
**Returns**: ValidationResult with errors and warnings
**Prompt Enhancement**:
  - "Validation checks for: syntax errors, type mismatches, missing dependencies, circular references"
**Auto-trigger**: Before `save_project`
**Keep/Remove**: ✅ KEEP (already added)

#### 19. `get_project_errors`
**Purpose**: Get detailed list of all errors with locations and fix suggestions
**Business Value**: Understand what needs to be fixed
**API**: Same as validate_project but with enhanced error formatting
**Prompt Enhancement**: CRITICAL - Error interpretation and fix suggestions
```
Prompt Template:
"Project has {error_count} errors:

Error 1: {error_message}
Location: {file}:{table}:{line}
Severity: {ERROR|WARNING}

Suggested fix:
{ai_generated_suggestion_based_on_error_type}

Common fixes:
- Type mismatch: Check data types in table columns
- Missing reference: Ensure referenced datatype exists
- Syntax error: Check Excel formula syntax
"
```
**Notes**: Merge with validate_project? Or keep separate for detailed error analysis?
**Keep/Remove**: ✅ ADD NEW (or merge with validate_project)

---

### Category 6: Execution (1 tool)
Purpose: Execute rules to test behavior

#### 20. `execute_rule`
**Purpose**: Execute a specific rule with test data to verify behavior
**Business Value**: AI can validate rule changes by executing them
**API**: `POST /design-repositories/{repo}/projects/{project}/rules/{ruleName}/execute`
**Input**:
  - `ruleName`: Name of rule method to execute
  - `inputData`: JSON with input parameters
**Returns**: Execution result with output data
**Prompt Enhancement**: CRITICAL - Help construct test data
```
Prompt Template:
"To execute rule '{ruleName}', provide input data:

Rule signature: {returnType} {ruleName}({parameters})

Required inputs:
{list parameters with types}

Example:
{generate example JSON based on parameter types}

This allows me to verify the rule works correctly before you save."
```
**Keep/Remove**: ✅ ADD NEW

---

### Category 7: Versioning (2 tools)
Purpose: Compare and revert versions

#### 21. `compare_versions`
**Purpose**: Show differences between two project versions
**Business Value**: Understand what changed between versions
**API**: `GET /design-repositories/{repo}/projects/{project}/versions/compare`
**Parameters**: `version1`, `version2`
**Returns**: Diff showing added/modified/deleted tables
**Prompt Enhancement**:
  - "Comparing versions shows which rules changed"
  - "Use this to understand updates or find when issues were introduced"
**Keep/Remove**: ✅ ADD NEW

#### 22. `revert_version`
**Purpose**: Revert to a previous project version
**Business Value**: Rollback breaking changes
**API**: Complex workflow combining multiple operations
**Workflow**:
  1. Get old version content
  2. Open project for editing
  3. Validate old version content
  4. If valid, save as new version (revert doesn't delete history)
  5. Close project
**Prompt Enhancement**: CRITICAL - Explain revert process
```
Prompt Template:
"Reverting to version {version}:

This will:
1. Load project state from version {version}
2. Validate it works with current OpenL version
3. Save as NEW version (preserves history)

Current version: {current_version}
After revert: {next_version} (with content from {version})

History is preserved - you can revert again if needed.

Proceed? (yes/no)"
```
**Keep/Remove**: ✅ ADD NEW

---

### Category 8: Deployment (2 tools)
Purpose: Deploy rules to environments

#### 23. `list_deployments`
**Purpose**: List all deployments across environments
**Business Value**: See what's deployed where
**API**: `GET /production-repositories/deployments`
**Prompt Enhancement**: None needed
**Keep/Remove**: ✅ KEEP

#### 24. `deploy_project`
**Purpose**: Deploy/promote project to environment
**Business Value**: Move rules to production
**API**: `POST /production-repositories/{deployRepo}/deploy`
**Parameters**:
  - `projectName`: Project to deploy
  - `repository`: Source repository
  - `deploymentRepository`: Target environment
**Prompt Enhancement**: CRITICAL - Environment selection and permissions
```
Prompt Template:
"Available deployment environments:
{list available production repositories with descriptions}

1. dev - Development environment
2. test - Testing/QA environment
3. staging - Pre-production
4. production - Live production (requires approval)

Current permissions: {user_deployment_permissions}

Before deploying to {environment}:
✓ All tests passed
✓ Project validated (no errors)
✓ Code review completed (for production)

Select environment: "
```
**Keep/Remove**: ✅ KEEP (enhance with prompts)

---

## Tools to REMOVE (2 tools)

### Remove: `health_check`
**Reason**: Not part of business rules workflow
**Alternative**: MCP clients handle connectivity testing
**Keep/Remove**: ❌ REMOVE

### Remove: `get_project_history`
**Reason**: Can be part of `get_project` response
**Alternative**: Include last modified info in get_project
**Keep/Remove**: ❌ REMOVE (merge into get_project)

### Remove: `create_branch`
**Reason**: Not critical for rules workflow
**Alternative**: WebStudio UI for branch management
**Keep/Remove**: ❌ REMOVE

---

## Prompt Template System

### Implementation Strategy

Create a `prompts/` directory with prompt templates for each tool:

```
mcp-server/
├── prompts/
│   ├── create_rule.md          # Rule type selection prompt
│   ├── copy_table.md           # Versioning decision prompt
│   ├── version_file.md         # Version naming prompt
│   ├── run_test.md             # Test selection prompt
│   ├── get_project_errors.md   # Error interpretation prompt
│   ├── execute_rule.md         # Test data construction prompt
│   ├── revert_version.md       # Revert explanation prompt
│   └── deploy_project.md       # Deployment environment prompt
```

### Prompt Delivery

Tools will return prompts as part of their response when:
1. User provides minimal input (e.g., "create a rule" without specifying type)
2. Multiple options exist (e.g., rule types, environments)
3. Important decisions need explanation (e.g., versioning strategy)

Example tool response with prompt:
```json
{
  "status": "needs_input",
  "prompt": "{prompt_template_content}",
  "options": ["option1", "option2"],
  "recommended": "option1",
  "next_step": "user_selects_option"
}
```

---

## Final Tool Count

### Total: 22 Tools (was 15, removing 3, adding 10, merging 2)

**Repository**: 2 tools
**Project**: 5 tools (merged get_project + get_project_info, added save_project)
**Files**: 2 tools (upload_file, download_file)
**Rules**: 6 tools (added create_rule, copy_table, version_file, enhanced list_tables)
**Testing**: 4 tools (added run_test, get_project_errors, kept run_all_tests, validate_project)
**Execution**: 1 tool (execute_rule)
**Versioning**: 2 tools (compare_versions, revert_version)
**Deployment**: 2 tools (kept list_deployments, deploy_project)

---

## Implementation Priority

### Phase 1: Core Operations (Must Have)
1. `save_project` - Cannot persist changes without this
2. `upload_file` - Cannot import Excel rules
3. `download_file` - Cannot export for external editing
4. `create_rule` - Cannot add new rules
5. Merge `get_project` + `get_project_info`
6. Add filters to `list_tables`

### Phase 2: Testing & Validation (Critical)
7. `run_test` - Smart test selection
8. `get_project_errors` - Detailed error analysis
9. Keep `run_all_tests` and `validate_project`

### Phase 3: Versioning & Execution (Important)
10. `version_file` - Proper file versioning
11. `copy_table` - Table duplication
12. `execute_rule` - Rule execution testing
13. `compare_versions` - Version comparison
14. `revert_version` - Rollback capability

### Phase 4: Cleanup
15. Remove `health_check`
16. Remove `create_branch`
17. Remove `get_project_history` (merge into get_project)

---

## API Integration Notes

### Combined API Calls

**save_project**:
```
1. POST /validate → check errors
2. POST /save → create version
3. POST /close → release lock
```

**get_project** (merged):
```
1. GET /projects/{id} → basic info
2. GET /projects/{id}/info → modules, dependencies
3. Return combined comprehensive response
```

**revert_version**:
```
1. GET /projects/{id}/versions/{version} → old content
2. POST /projects/{id}/open → lock
3. POST /validate → check compatibility
4. POST /save → save as new version
5. POST /close → release lock
```

**run_test** (smart selection):
```
1. GET /projects/{id}/tables → get all tables
2. Analyze modified tables (from input)
3. GET /projects/{id}/tables?type=test → get test tables
4. Match tests to modified tables (by name patterns)
5. POST /tests/run → run selected tests
```

---

## Business Rules Workflow Coverage

### ✅ Full Coverage

1. **Develop Rules**
   - ✅ Upload Excel files
   - ✅ Create rules
   - ✅ Edit rules
   - ✅ Version rules (file and table level)
   - ✅ Copy rules

2. **Test Rules**
   - ✅ Run specific tests
   - ✅ Run all tests
   - ✅ Execute rules with test data
   - ✅ Get detailed errors

3. **Validate Rules**
   - ✅ Check syntax errors
   - ✅ Get error details with fix suggestions
   - ✅ Auto-validate before save

4. **Save Rules**
   - ✅ Create new version
   - ✅ Validate before save
   - ✅ Run tests before save

5. **Deploy Rules**
   - ✅ Deploy to environments
   - ✅ Environment selection
   - ✅ Permission checking

6. **Maintain Rules**
   - ✅ Compare versions
   - ✅ Revert to old versions
   - ✅ Download for backup
   - ✅ View deployment status

---

## Summary

This plan provides **22 focused tools** that cover the complete business rules management lifecycle while using **prompt templates** to guide AI assistants in making context-aware decisions about rule types, versioning strategies, test selection, and deployment environments.

**Key Innovations:**
1. **Prompt-enhanced tools** that educate AI about OpenL concepts
2. **Combined API operations** for complete business workflows
3. **Smart defaults** (e.g., auto-validate before save)
4. **Business focus** (file versioning over Git branches)
5. **Error interpretation** with fix suggestions
