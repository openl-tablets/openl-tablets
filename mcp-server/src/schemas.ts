/**
 * Zod Schemas for MCP Tool Input Validation
 *
 * This module defines all input schemas for OpenL Tablets MCP tools using Zod.
 * Benefits:
 * - Type-safe input validation with runtime checks
 * - Automatic TypeScript type inference
 * - Self-documenting API through schema descriptions
 * - Clear validation error messages
 *
 * To add a new tool schema:
 * 1. Define the schema using z.object() with descriptive field names
 * 2. Add .describe() to each field for documentation
 * 3. Export the schema
 * 4. Reference it in tools.ts
 */

import { z } from "zod";

// Re-export z for convenience
export { z };

// Response format enum
export const ResponseFormat = z
  .enum(["json", "markdown", "markdown_concise", "markdown_detailed"])
  .default("markdown")
  .describe(
    "Response format: 'json' for structured data, 'markdown' for human-readable (default), 'markdown_concise' for brief summary (1-2 paragraphs), 'markdown_detailed' for full details with context"
  );

// Pagination parameters
export const PaginationParams = z.object({
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).optional();

// Project ID: base64-encoded format from openl_list_projects() response
export const projectIdSchema = z.string().describe("Project ID - base64-encoded format (default). Use the exact 'projectId' value returned from openl_list_projects() API response. Do not modify or reformat this value. The API returns projectId in base64 format: base64('repository:projectName:hashCode').");

export const repositoryNameSchema = z.string().describe("Repository name (e.g., 'design', 'production', 'test')");

export const projectNameSchema = z.string().describe("Project name within the repository (e.g., 'InsuranceRules', 'AutoPremium', 'ClaimProcessing')");

export const tableIdSchema = z.string().describe("Table identifier - unique ID assigned by OpenL Tablets when table is created (e.g., 'calculatePremium_1234')");

export const branchNameSchema = z.string().describe("Git branch name (e.g., 'main', 'development', 'feature/new-rules')");

export const commentSchema = z.string().optional().describe("Commit comment describing the change (e.g., 'Updated CA premium rates', 'Fixed calculation bug')");

// Tool input schemas
export const listProjectsSchema = z.object({
  repository: z.string().optional().describe("Filter by repository ID (NOT repository name). Use the 'id' field from openl_list_repositories() response (e.g., if list_repositories returns {id: 'design-repo', name: 'Design Repository'}, use 'design-repo' here, NOT 'Design Repository'). Omit to show projects from all repositories."),
  status: z.enum(["LOCAL", "ARCHIVED", "OPENED", "VIEWING_VERSION", "EDITING", "CLOSED"]).optional().describe("Filter by project status. Valid values: 'LOCAL', 'ARCHIVED', 'OPENED', 'VIEWING_VERSION', 'EDITING', 'CLOSED'."),
  tags: z.record(z.string()).optional().describe("Filter by project tags. Tags must be prefixed with 'tags.' in the query string (e.g., tags.version='1.0', tags.environment='production'). This is handled automatically by the API client - provide as object with tag names as keys."),
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();

export const getProjectSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const getProjectInfoSchema = z.object({
  repository: repositoryNameSchema,
  projectName: projectNameSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const projectActionSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const updateProjectStatusSchema = z.object({
  projectId: projectIdSchema,
  status: z.enum(["OPENED", "CLOSED"]).optional().describe("Project status to set. OPENED = open and available for editing (read-only if locked by another user). CLOSED = closed and unlocked. Note: Other statuses (LOCAL, ARCHIVED, VIEWING_VERSION, EDITING) are set automatically by OpenL and cannot be set manually."),
  comment: commentSchema.describe("Git commit comment. When provided on a modified project, saves changes before applying status change."),
  branch: branchNameSchema.optional().describe("Switch to a different Git branch"),
  revision: z.string().optional().describe("Switch to a specific Git revision/commit hash for read-only viewing"),
  selectedBranches: z.array(z.string()).optional().describe("List of branches to select for multi-branch projects"),
  response_format: ResponseFormat.optional(),
}).strict();

export const listTablesSchema = z.object({
  projectId: projectIdSchema,
  kind: z.array(z.string()).optional().describe("Filter by table kinds (array of strings). Valid values: 'Rules', 'Spreadsheet', 'Datatype', 'Data', 'Test', 'TBasic', 'Column Match', 'Method', 'Run', 'Constants', 'Conditions', 'Actions', 'Returns', 'Environment', 'Properties', 'Other'. Omit to show all kinds."),
  name: z.string().optional().describe("Filter by table name fragment (e.g., 'calculate', 'Premium'). Omit to show all tables."),
  properties: z.record(z.string()).optional().describe("Filter by project properties. Properties must be prefixed with 'properties.' in the query string (e.g., properties.state='CA', properties.lob='Auto'). This is handled automatically by the API client."),
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();

export const getTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const updateTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  view: z.record(z.any()).describe("FULL table structure from get_table() with your modifications applied. MUST include: id, tableType, kind, name, plus type-specific data (rules for SimpleRules, rows for Spreadsheet, fields for Datatype). Do NOT send only the changed fields - send the complete structure. Workflow: 1) currentTable = get_table(), 2) currentTable.rules[0]['Column'] = newValue, 3) update_table(view=currentTable)"),
  comment: commentSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const appendTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  appendData: z.discriminatedUnion("tableType", [
    // DatatypeAppend
    z.object({
      tableType: z.literal("Datatype"),
      fields: z.array(z.object({
        name: z.string().describe("Field name"),
        type: z.string().describe("Field type (e.g., 'String', 'int', 'double')"),
        required: z.boolean().optional().describe("Whether field is required"),
        defaultValue: z.any().optional().describe("Default value for the field"),
      })).describe("Array of field definitions to append"),
    }),
    // SimpleRulesAppend
    z.object({
      tableType: z.literal("SimpleRules"),
      rules: z.array(z.record(z.any())).describe("Array of rule objects to append. Each rule is a map with condition and action columns."),
    }),
    // SimpleSpreadsheetAppend
    z.object({
      tableType: z.literal("SimpleSpreadsheet"),
      steps: z.array(z.any()).describe("Array of spreadsheet step objects to append"),
    }),
    // SmartRulesAppend
    z.object({
      tableType: z.literal("SmartRules"),
      rules: z.array(z.record(z.any())).describe("Array of rule objects to append. Each rule is a map with condition and action columns."),
    }),
    // VocabularyAppend
    z.object({
      tableType: z.literal("Vocabulary"),
      values: z.array(z.any()).describe("Array of vocabulary value objects to append"),
    }),
  ]).describe("Data structure to append to the table. Structure depends on tableType: Datatype uses 'fields', SimpleRules/SmartRules use 'rules', SimpleSpreadsheet uses 'steps', Vocabulary uses 'values'"),
  comment: commentSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const listBranchesSchema = z.object({
  repository: repositoryNameSchema,
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();

export const createBranchSchema = z.object({
  projectId: projectIdSchema,
  branchName: branchNameSchema,
  revision: z.string().optional().describe("Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used."),
  response_format: ResponseFormat.optional(),
}).strict();

export const deployProjectSchema = z.object({
  projectId: projectIdSchema.describe("Project ID to deploy - base64-encoded format (default). Use the exact 'projectId' value from openl_list_projects() response (e.g., base64-encoded string)."),
  deploymentName: z.string().describe("Name for the deployment (e.g., 'InsuranceRules', 'AutoPremium'). This will be the deployment identifier."),
  productionRepositoryId: z.string().describe("Target production repository ID where the project will be deployed (e.g., 'production-deploy', 'staging-deploy'). Must be configured in OpenL Tablets."),
  comment: commentSchema.describe("Deployment reason comment (e.g., 'Deploy version 1.2.0', 'Production release')"),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Testing & Validation (Critical Missing Tools)
// =============================================================================
// Note: The following schemas are placeholders for tools that are temporarily disabled
// pending client.ts support for the OpenL Studio REST API endpoints.

export const validateProjectSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const testProjectSchema = z.object({
  projectId: projectIdSchema,
  testName: z.string().optional().describe("Specific test name to run (e.g., 'testPremiumCalculation'). Omit to run all tests in the project."),
  allTests: z.boolean().optional().describe("Set to true to explicitly run all tests in the project (default: false). When false and testName is omitted, runs all tests."),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Phase 1: New Tool Schemas
// =============================================================================

export const saveProjectSchema = z.object({
  projectId: projectIdSchema,
  comment: commentSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const uploadFileSchema = z.object({
  projectId: projectIdSchema,
  fileName: z.string().describe("Path where the file should be uploaded in the project (.xlsx or .xls). Can be a simple filename (e.g., 'Rules.xlsx'), subdirectory path (e.g., 'rules/Premium.xlsx'), or full path (e.g., 'Example 1 - Bank Rating/Bank Rating.xlsx'). To replace an existing file, use the exact 'file' field value from list_tables()."),
  fileContent: z.string().describe("Base64-encoded file content"),
  comment: z.string().optional().describe("Optional comment for when the file is eventually saved/committed to Git (e.g., 'Updated CA premium rates'). The upload itself does NOT create a commit - use update_project_status to save changes."),
  response_format: ResponseFormat.optional(),
}).strict();

export const downloadFileSchema = z.object({
  projectId: projectIdSchema,
  fileName: z.string().describe("Name of the Excel file to download. MUST use the exact 'file' field value from list_tables() response (e.g., 'Rules.xlsx', 'rules/Insurance.xlsx'). Do NOT construct paths manually or guess file names - always get the path from list_tables() first."),
  version: z.string().optional().describe("Git commit hash to download specific version (e.g., '7a3f2b1c...'). Omit for latest version (HEAD)"),
  response_format: ResponseFormat.optional(),
}).strict();

export const createRuleSchema = z.object({
  projectId: projectIdSchema,
  name: z.string().min(1).describe("Table name (must be valid Java identifier, e.g., 'calculatePremium')"),
  tableType: z.enum([
    // Decision Tables (most common)
    "Rules", "SimpleRules", "SmartRules", "SimpleLookup", "SmartLookup",
    // Spreadsheet (most common)
    "Spreadsheet",
    // Other types (rarely used)
    "Method", "TBasic", "Data", "Datatype", "Test", "Run", "Properties", "Configuration"
  ]).describe("Type of table to create. Most common: Rules/SimpleRules/SmartRules/SimpleLookup/SmartLookup (decision tables) or Spreadsheet (calculations)"),
  returnType: z.string().optional().describe("Return type (e.g., 'int', 'String', 'SpreadsheetResult', or custom type like 'Policy')"),
  parameters: z.array(z.object({
    type: z.string().describe("Parameter type (e.g., 'String', 'int', 'double', 'Policy')"),
    name: z.string().describe("Parameter name (e.g., 'driverType', 'age', 'policy')")
  })).optional().describe("Method parameters for the table signature"),
  file: z.string().optional().describe("Target Excel file (e.g., 'rules/Insurance.xlsx'). If not specified, uses default file."),
  properties: z.record(z.any()).optional().describe("Dimension properties (e.g., { state: 'CA', lob: 'Auto', effectiveDate: '01/01/2025' })"),
  comment: commentSchema,
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Phase 2: Testing & Validation Schemas
// =============================================================================
// Note: runTestSchema removed - endpoint doesn't exist in API

export const getProjectErrorsSchema = z.object({
  projectId: projectIdSchema,
  includeWarnings: z.boolean().optional().describe("Include warnings along with errors (default: true). Set to false to show only critical errors that block deployment."),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Phase 3: Versioning & Execution Schemas
// =============================================================================

export const executeRuleSchema = z.object({
  projectId: projectIdSchema,
  ruleName: z.string().describe("Name of the rule/method to execute (e.g., 'calculatePremium', 'validatePolicy'). Must match exact table name."),
  inputData: z.record(z.any()).describe("Input data for rule execution as JSON object with parameter names as keys (e.g., { \"driverType\": \"SAFE\", \"age\": 30, \"vehicleValue\": 25000 })"),
  response_format: ResponseFormat.optional(),
}).strict();

export const compareVersionsSchema = z.object({
  projectId: projectIdSchema,
  baseCommitHash: z.string().describe("Base Git commit hash to compare from (e.g., '7a3f2b1c...')"),
  targetCommitHash: z.string().describe("Target Git commit hash to compare to (e.g., '9e5d8a2f...')"),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Phase 4: Advanced Features
// =============================================================================

export const revertVersionSchema = z.object({
  projectId: projectIdSchema,
  targetVersion: z.string().describe("Git commit hash to revert to (e.g., '7a3f2b1c...')"),
  comment: commentSchema,
  confirm: z.boolean().describe("Must be true to proceed with this destructive operation"),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Phase 2: Git Version History Schemas
// =============================================================================

export const getFileHistorySchema = z.object({
  projectId: projectIdSchema,
  filePath: z.string().min(1).describe("File path within project (e.g., 'rules/Insurance.xlsx')"),
  limit: z.number().int().positive().max(200).optional().describe("Maximum number of commits to return (default: 50, max: 200)"),
  offset: z.number().int().nonnegative().optional().describe("Number of commits to skip for pagination (default: 0)"),
  response_format: ResponseFormat.optional(),
}).strict();

export const getProjectHistorySchema = z.object({
  projectId: projectIdSchema,
  limit: z.number().int().positive().max(200).optional().describe("Maximum commits to return (default: 50, max: 200)"),
  offset: z.number().int().nonnegative().optional().describe("Commits to skip for pagination (default: 0)"),
  branch: z.string().optional().describe("Git branch name (default: current branch)"),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Repository Features & Revisions Schemas
// =============================================================================

export const getRepositoryFeaturesSchema = z.object({
  repository: repositoryNameSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const getProjectRevisionsSchema = z.object({
  repository: repositoryNameSchema,
  projectName: projectNameSchema,
  branch: branchNameSchema.optional().describe("Branch name (optional, only if repository supports branches)"),
  search: z.string().optional().describe("Search term to filter revisions by commit message or author"),
  techRevs: z.boolean().optional().describe("Include technical revisions (default: false)"),
  page: z.number().int().nonnegative().optional().describe("Page number (0-based, default: 0)"),
  size: z.number().int().positive().max(200).optional().describe("Page size (default: 50, max: 200)"),
  response_format: ResponseFormat.optional(),
}).strict();

export const listDeployRepositoriesSchema = z.object({
  response_format: ResponseFormat.optional(),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
}).strict();

// =============================================================================
// Local Changes & Restore Schemas
// =============================================================================

export const listProjectLocalChangesSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const restoreProjectLocalChangeSchema = z.object({
  projectId: projectIdSchema,
  historyId: z.string().describe("History ID to restore (from list_project_local_changes response)"),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Test Execution Schemas
// =============================================================================

export const startProjectTestsSchema = z.object({
  projectId: projectIdSchema,
  tableId: z.string().optional().describe("Table ID to run tests for a specific table. Table type can be test table or any other table. If not provided, tests for all test tables in the project will be run."),
  testRanges: z.string().optional().describe("Test ranges to run. Can be provided only if tableId is Test table. Example: '1-3,5' to run tests with numbers 1,2,3 and 5. If not provided, all tests in the test table will be run."),
  response_format: ResponseFormat.optional(),
}).strict();

export const getProjectTestResultsSchema = z.object({
  projectId: projectIdSchema,
  failuresOnly: z.boolean().optional().describe("Show only failed tests (default: false)"),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
  waitForCompletion: z.boolean().optional().describe("Wait for test execution to complete before returning results. If false, returns current status immediately (default: true)"),
  response_format: ResponseFormat.optional(),
}).strict();

export const runProjectTestsSchema = z.object({
  projectId: projectIdSchema,
  tableId: z.string().optional().describe("Table ID to run tests for a specific table. Table type can be test table or any other table. If not provided, tests for all test tables in the project will be run."),
  testRanges: z.string().optional().describe("Test ranges to run. Can be provided only if tableId is Test table. Example: '1-3,5' to run tests with numbers 1,2,3 and 5. If not provided, all tests in the test table will be run."),
  failuresOnly: z.boolean().optional().describe("Show only failed tests (default: false)"),
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Redeploy Schema
// =============================================================================

export const redeployProjectSchema = z.object({
  deploymentId: z.string().describe("Deployment ID to redeploy (from list_deployments response)"),
  projectId: projectIdSchema,
  comment: commentSchema,
  response_format: ResponseFormat.optional(),
}).strict();


