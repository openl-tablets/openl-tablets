# OpenL Tablets MCP Server - Usage Examples

This document provides practical examples of using the OpenL Tablets MCP Server with AI assistants.

## Table of Contents
- [Getting Started](#getting-started)
- [Repository Operations](#repository-operations)
- [Project Management](#project-management)
- [Working with Rules (Tables)](#working-with-rules-tables)
- [Version Control](#version-control)
- [Deployment](#deployment)
- [Common Workflows](#common-workflows)

## Getting Started

### Initial Setup with Claude Desktop

1. Build the MCP server:
```bash
cd mcp-server
npm install
npm run build
```

2. Add to your Claude Desktop config (see [Configuration Examples](../setup/examples/))

3. Restart Claude Desktop

4. Verify connection by asking Claude:
   - "Can you list the OpenL Tablets repositories?"
   - "Show me all projects in OpenL Tablets"

## Repository Operations

### List All Repositories

**Prompt to Claude:**
```text
List all OpenL Tablets repositories and their features
```

**What happens:**
- MCP server calls `openl_list_repositories` tool
- Returns list of design repositories with their capabilities (branches, search, etc.)

**Example Response (JSON format):**
```json
[
  {
    "id": "design",
    "name": "Design Repository",
    "type": "git",
    "features": {
      "branches": true,
      "mappedFolders": false,
      "searchable": true
    }
  }
]
```

**With Markdown formatting:**
You can request the response in Markdown format for better readability:
```text
List all OpenL Tablets repositories in markdown format
```

**Tool call with response_format:**
```json
{
  "name": "openl_list_repositories",
  "arguments": {
    "response_format": "markdown"
  }
}
```

**Example Response (Markdown format):**
```markdown
# OpenL Tablets Repositories

## Design Repository
- **ID:** design
- **Type:** git
- **Features:**
  - Branches: ✓
  - Mapped Folders: ✗
  - Searchable: ✓
```

### Check Repository Features

**Prompt to Claude:**
```text
What features does the 'design' repository support?
```

**Response includes:**
- Branch support
- Search capabilities
- Folder mapping

## Project Management

### List All Projects

**Prompt to Claude:**
```text
Show me all projects in OpenL Tablets
```

**With filters:**
```text
Show me all OPENED projects in the 'design' repository
```
```text
Show me projects tagged with 'production'
```

**With pagination:**
```text
Show me the first 10 projects in the 'design' repository
```

**Tool call with pagination:**
```json
{
  "name": "openl_list_projects",
  "arguments": {
    "repository": "design",
    "limit": 10,
    "offset": 0
  }
}
```

**To get the next page:**
```json
{
  "name": "openl_list_projects",
  "arguments": {
    "repository": "design",
    "limit": 10,
    "offset": 10
  }
}
```

**With response_format:**
```json
{
  "name": "openl_list_projects",
  "arguments": {
    "repository": "design",
    "response_format": "markdown"
  }
}
```

**Example Response (Markdown format):**
```markdown
# Projects in Repository: design

## insurance-rules
- **Status:** OPENED
- **Modified:** 2025-11-15 14:30:00 by john.doe
- **Branch:** main
- **Tags:** production, v2.3.1

## fleet-management
- **Status:** CLOSED
- **Modified:** 2025-11-14 09:15:00 by jane.smith
- **Branch:** develop
- **Tags:** staging
```

### Get Project Details

**Prompt to Claude:**
```text
Get detailed information about the project 'insurance-rules' in the 'design' repository
```

**Response includes:**
- Project name and status
- Last modification time and user
- Lock information
- Branch (if applicable)
- Tags
- Latest commit comment

### Open a Project for Editing

**Prompt to Claude:**
```text
Open the project 'design-insurance-rules' for editing
```

**Important:** Projects must be opened before you can view or modify their tables.

### Get Project Structure

**Prompt to Claude:**
```text
Show me the structure of project 'insurance-rules' including modules and dependencies
```

**Response includes:**
- Modules and their Excel files
- Project dependencies
- Classpath entries
- Tags

## Working with Rules (Tables)

### List Tables in a Project

**Prompt to Claude:**
```text
List all tables in the project 'design-insurance-rules'
```

**Example Response:**
```json
[
  {
    "id": "Rules.xls_1234",
    "tableType": "simplerules",
    "kind": "Rules",
    "name": "CalculatePremium",
    "returnType": "Double",
    "signature": "Double CalculatePremium(String vehicleType, Integer age)",
    "file": "Rules.xlsx"
  },
  {
    "id": "Rules.xls_5678",
    "tableType": "datatype",
    "kind": "Datatype",
    "name": "Policy",
    "file": "Datatypes.xlsx"
  }
]
```

### Get Table Details

**Prompt to Claude:**
```text
Show me the details of the table 'CalculatePremium' in project 'design-insurance-rules'
```

**For Decision Tables (JSON format):**
```json
{
  "id": "Rules.xls_1234",
  "tableType": "simplerules",
  "name": "CalculatePremium",
  "properties": [
    {"name": "category", "value": "Premium Calculation"}
  ],
  "rules": [
    {
      "conditions": {"vehicleType": "Car", "age": "<25"},
      "actions": {"premium": "1200"}
    }
  ]
}
```

**Tool call with response_format:**
```json
{
  "name": "openl_get_table",
  "arguments": {
    "repository": "design",
    "project": "insurance-rules",
    "table_id": "Rules.xls_1234",
    "response_format": "markdown"
  }
}
```

**Example Response (Markdown format):**
```markdown
# Decision Table: CalculatePremium

**Type:** simplerules
**ID:** Rules.xls_1234
**Category:** Premium Calculation

## Rules

| Vehicle Type | Age   | Premium |
|-------------|-------|---------|
| Car         | <25   | 1200    |
| Car         | ≥25   | 800     |
| Motorcycle  | <25   | 900     |
| Motorcycle  | ≥25   | 600     |
```

**For Datatypes:**
```json
{
  "id": "Rules.xls_5678",
  "tableType": "datatype",
  "name": "Policy",
  "fields": [
    {"name": "policyNumber", "type": "String"},
    {"name": "holderName", "type": "String"},
    {"name": "premium", "type": "Double"}
  ]
}
```

### Understanding Table Types

Different table types serve different purposes:

- **Decision Tables** (`simplerules`, `smartrules`) - Business rules with conditions and actions
- **Datatypes** - Custom data structures
- **Spreadsheets** - Calculation tables
- **Vocabularies** - Enumeration definitions
- **Method Tables** - Reusable methods
- **Data Tables** - Test or reference data

### Modify a Table

**Prompt to Claude:**
```text
Update the 'CalculatePremium' table in project 'design-insurance-rules' to add a new rule for motorcycles
```

**Claude will:**
1. Get the current table structure
2. Modify the appropriate fields
3. Call `openl_update_table` with the changes
4. Include a commit comment

## Version Control

### View Project History

**Prompt to Claude:**
```text
Show me the version history for project 'insurance-rules' in the 'design' repository
```

**Response includes:**
- Version/revision IDs
- Authors
- Timestamps
- Commit comments

### List Branches

**Prompt to Claude:**
```text
What branches exist in the 'design' repository?
```

### Create a New Branch

**Prompt to Claude:**
```text
Create a new branch called 'feature/new-premium-logic' for the project 'design-insurance-rules'
```

## Deployment

### List Current Deployments

**Prompt to Claude:**
```text
Show me all deployed projects
```

**Response includes:**
- Deployment IDs
- Project names and versions
- Deployment repositories
- Status and timestamps

### Deploy a Project

**Prompt to Claude:**
```text
Deploy the project 'insurance-rules' from the 'design' repository to the 'production' repository
```

**Claude will:**
1. Call `openl_deploy_project` with the specified parameters
2. Monitor the deployment status
3. Confirm successful deployment

### Deploy Specific Version

**Prompt to Claude:**
```text
Deploy version 'v2.3.1' of project 'insurance-rules' to production
```

## Common Workflows

### Workflow 1: Review and Modify a Rule

**Conversation with Claude:**

```text
User: I need to update the premium calculation for young drivers

Claude: I'll help you with that. First, let me list the projects and find the relevant one.
[Calls openl_list_projects]

Claude: I found the 'insurance-rules' project. Let me open it and see what tables it contains.
[Calls openl_update_project_status to open the project, then openl_list_tables]

Claude: I found a table called 'CalculatePremium' that likely contains the premium logic. Let me get its details.
[Calls openl_get_table]

Claude: Here's the current rule for young drivers (age < 25):
- Vehicle: Car -> Premium: $1200
- Vehicle: Motorcycle -> Premium: $900

What changes would you like to make?

User: Increase car premium to $1400 for drivers under 25

Claude: I'll update that rule for you.
[Calls openl_update_table with modified data]

Claude: Done! The premium for cars with drivers under 25 is now $1400. The change has been committed with the comment "Updated premium for young drivers".
```

### Workflow 2: Create New Rule from Scratch

**Conversation with Claude:**

```text
User: Add a new discount rule for senior drivers over 60 years old

Claude: I'll add that rule. First, let me open the project and locate the appropriate table.
[Opens project, lists tables, gets current table structure]

Claude: I'll add a new rule to the CalculatePremium table with a 15% discount for drivers over 60.
[Updates table with new rule]

Claude: The new senior discount rule has been added. Drivers over 60 will now receive a 15% discount on their premium.
```

### Workflow 3: Compare Versions

**Conversation with Claude:**

```text
User: What changed in the insurance-rules project over the last week?

Claude: Let me check the version history for that project.
[Calls openl_get_project_history]

Claude: Here are the changes from the last week:

1. Version abc123 (2 days ago by john.doe)
   - "Updated premium calculation for electric vehicles"

2. Version def456 (5 days ago by jane.smith)
   - "Added new discount rules for senior drivers"

3. Version ghi789 (7 days ago by john.doe)
   - "Fixed validation logic for policy numbers"

Would you like to see the details of any specific version?
```

### Workflow 4: Deploy with Verification

**Conversation with Claude:**

```text
User: Deploy the latest insurance-rules to production

Claude: I'll help you deploy to production. Let me first verify the current state.
[Calls openl_get_project, checks status and latest changes]

Claude: The current version was modified 2 hours ago by john.doe with the comment "Finalized Q4 premium updates". The project is in OPENED status.

Before deploying, here's what will be deployed:
- Project: insurance-rules
- Repository: design
- Latest modification: Q4 premium updates

Should I proceed with the deployment to production?

User: Yes, proceed

Claude: Deploying now...
[Calls openl_deploy_project]

Claude: Successfully deployed 'insurance-rules' to production. You can verify the deployment using the openl_list_deployments command if needed.
```

### Workflow 5: Troubleshooting Rule Issues

**Conversation with Claude:**

```text
User: The premium calculation seems wrong for motorcycles

Claude: Let me investigate. I'll check the relevant rules.
[Calls openl_update_project_status to open project, openl_list_tables, then openl_get_table for CalculatePremium]

Claude: I found the issue. The motorcycle rule has:
- Condition: vehicleType = "Motorcycle"
- Action: premium = 900

But I notice there's a conflicting rule below it that overrides this value. The rule precedence is:
1. Motorcycle base: $900
2. Age < 25 override: +$300 (total $1200)

Is this the expected behavior, or should young driver premium be calculated differently?

User: Young drivers should get a percentage increase, not a flat amount

Claude: I'll update the logic to use a percentage-based calculation.
[Calls openl_update_table with percentage-based formula]

Claude: Updated! Now the premium for young motorcycle riders is calculated as: base_premium * 1.33 (33% increase) instead of a flat addition.
```

## Tips for Working with Claude

1. **Be Specific**: Mention exact project names, table names, and repositories
2. **Open First**: Remember to open projects before accessing tables
3. **Check Status**: Ask Claude to verify project status before making changes
4. **Use Comments**: When modifying tables, provide meaningful commit comments
5. **Review Changes**: Ask Claude to show you the changes before committing
6. **Branch for Safety**: Create branches for experimental changes
7. **Test Before Deploy**: Verify changes in a test environment first

## Testing

### Run Project Tests

**Prompt to Claude:**
```text
Run all tests for project 'design-insurance-rules' and show me the results
```

**What happens:**
1. MCP server calls `openl_run_project_tests` to start test execution and retrieve results
2. Tool automatically uses all headers from test start response when fetching results

**Example Workflow:**
```text
# Unified tool - starts tests and gets results in one call
openl_run_project_tests(
  projectId: "ZGVzaWduOTpJbnN1cmFuY2UgUnVsZXM6...",
  tableId: "calculatePremium_1234",  # Optional: test specific table
  waitForCompletion: true,  # Wait for completion (default)
  failuresOnly: false  # Optional: show only failures
)
```

**Example Response:**
```json
{
  "testCases": [
    {
      "name": "Test_calculatePremium",
      "tableId": "Test_calculatePremium_1234",
      "executionTimeMs": 45,
      "numberOfTests": 5,
      "numberOfFailures": 0,
      "testUnits": [
        {
          "name": "Test 1",
          "status": "PASSED",
          "executionTimeMs": 8
        }
      ]
    }
  ],
  "executionTimeMs": 45,
  "numberOfTests": 5,
  "numberOfFailures": 0
}
```

### Run Tests for Specific Table

**Prompt to Claude:**
```text
Run tests for the calculatePremium table in project 'design-insurance-rules'
```

**What happens:**
- Uses `tableId` parameter to target specific table
- Only tests related to that table are executed

**Example:**
```text
openl_run_project_tests(
  projectId: "ZGVzaWduOTpJbnN1cmFuY2UgUnVsZXM6...",
  tableId: "calculatePremium_1234"
)
```

### Check Test Status Without Waiting

**Prompt to Claude:**
```text
Check current test status for project 'design-insurance-rules' without waiting
```

**What happens:**
- Uses `waitForCompletion: false` to get current status immediately
- Useful for checking progress of long-running tests

**Example:**
```text
openl_run_project_tests(
  projectId: "ZGVzaWduOTpJbnN1cmFuY2UgUnVsZXM6...",
  waitForCompletion: false  # Return immediately
)
```

### Run Specific Test Ranges

**Prompt to Claude:**
```text
Run tests 1-3 and 5 for the Test_calculatePremium table
```

**What happens:**
- Uses `testRanges` parameter to run specific test cases
- Format: "1-3,5" runs tests 1, 2, 3, and 5

**Example:**
```text
openl_run_project_tests(
  projectId: "ZGVzaWduOTpJbnN1cmFuY2UgUnVsZXM6...",
  tableId: "Test_calculatePremium_1234",
  testRanges: "1-3,5"
)
```

## Advanced Usage

### Working with Complex Datatypes

**Prompt:**
```text
Show me the structure of the 'Policy' datatype and create a new field for 'discountPercentage'
```

### Batch Operations

**Prompt:**
```text
List all decision tables across all projects that contain the word 'premium' in their name
```

### Cross-Project Analysis

**Prompt:**
```text
Compare the 'Vehicle' datatype between the 'auto-insurance' and 'fleet-insurance' projects
```

### Generating Documentation

**Prompt:**
```text
Generate documentation for all tables in the 'insurance-rules' project, including their purpose and structure
```

## Troubleshooting with Claude

If you encounter issues, ask Claude to help diagnose:

```text
Claude, I'm getting an error when trying to update a table. Can you check:
1. Is the project opened?
2. Do I have the correct table ID?
3. Is there a lock on the project?
```

Claude can access the error messages and guide you through resolution.

## Resources

- See README.md for installation and configuration
- Check the OpenL Tablets documentation for rule syntax
- Use Claude to explore the API capabilities: "What can you do with OpenL Tablets?"
