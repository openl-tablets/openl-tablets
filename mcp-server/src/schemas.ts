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

// Project ID format: repository-name_project-name or repository-name-project-name
export const projectIdSchema = z.string().describe("Project ID in format 'repository_name-project_name' (e.g., 'design-InsuranceRules', 'production-AutoPremium')");

export const repositoryNameSchema = z.string().describe("Repository name (e.g., 'design', 'production', 'test')");

export const projectNameSchema = z.string().describe("Project name within the repository (e.g., 'InsuranceRules', 'AutoPremium', 'ClaimProcessing')");

export const tableIdSchema = z.string().describe("Table identifier - unique ID assigned by OpenL Tablets when table is created (e.g., 'calculatePremium_1234')");

export const branchNameSchema = z.string().describe("Git branch name (e.g., 'main', 'development', 'feature/new-rules')");

export const commentSchema = z.string().optional().describe("Commit comment describing the change (e.g., 'Updated CA premium rates', 'Fixed calculation bug')");

// Tool input schemas
export const listProjectsSchema = z.object({
  repository: z.string().optional().describe("Filter by repository name (e.g., 'design', 'production'). Omit to show projects from all repositories."),
  status: z.string().optional().describe("Filter by project status: 'OPENED' (currently being edited), 'CLOSED' (not locked), or other status values."),
  tag: z.string().optional().describe("Filter by tag name (e.g., 'v1.0', 'production', 'QA-approved'). Omit to show all tagged and untagged projects."),
  branch: z.string().optional().describe("Filter by branch name (e.g., 'main', 'development'). Omit to show projects from all branches."),
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
  status: z.enum(["LOCAL", "ARCHIVED", "OPENED", "VIEWING_VERSION", "EDITING", "CLOSED"]).optional().describe("Project status to set. OPENED = open and available for editing (read-only if locked by another user). EDITING = currently editing with unsaved changes (auto-set by OpenL on first edit, locks project). VIEWING_VERSION = viewing outdated version (another user saved, need to re-open for latest). CLOSED = closed and unlocked"),
  comment: commentSchema.describe("Git commit comment. When provided on a modified project, saves changes before applying status change. Required when closing a project with unsaved changes (unless discardChanges is true)"),
  discardChanges: z.boolean().optional().describe("Explicitly confirm discarding unsaved changes when closing. Set to true to close an EDITING project without saving. Safety flag to prevent accidental data loss."),
  confirm: z.boolean().optional().describe("Required when performing destructive operations like closing with unsaved changes and discardChanges=true"),
  branch: branchNameSchema.optional().describe("Switch to a different Git branch"),
  revision: z.string().optional().describe("Switch to a specific Git revision/commit hash for read-only viewing"),
  selectedBranches: z.array(z.string()).optional().describe("List of branches to select for multi-branch projects"),
  response_format: ResponseFormat.optional(),
}).strict();

export const listTablesSchema = z.object({
  projectId: projectIdSchema,
  tableType: z.string().optional().describe("Filter by table type: 'Rules', 'SimpleRules', 'SmartRules', 'SimpleLookup', 'SmartLookup', 'Spreadsheet', 'Datatype', 'Data', 'Test', 'Method', etc. Omit to show all types."),
  name: z.string().optional().describe("Filter by table name pattern using wildcards (e.g., 'calculate*', '*Premium*', 'validate*'). Omit to show all tables."),
  file: z.string().optional().describe("Filter by Excel file name (e.g., 'rules/Insurance.xlsx', 'Premium.xlsx'). Omit to show tables from all files."),
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
  appendData: z.object({
    tableType: z.string().describe("Table type (e.g., 'Datatype', 'Data')"),
    fields: z.array(z.object({
      name: z.string().describe("Field name"),
      type: z.string().describe("Field type (e.g., 'String', 'int', 'double')"),
      required: z.boolean().optional().describe("Whether field is required"),
      defaultValue: z.any().optional().describe("Default value for the field"),
    })).describe("Array of field definitions to append to the table"),
  }).describe("Data structure to append to the table"),
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
  comment: commentSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const deployProjectSchema = z.object({
  projectName: projectNameSchema,
  repository: repositoryNameSchema,
  deploymentRepository: z.string().describe("Target deployment repository name (e.g., 'production-deploy', 'staging-deploy'). Must be configured in OpenL Tablets."),
  version: z.string().optional().describe("Specific Git commit hash to deploy (e.g., '7a3f2b1c...'). Omit to deploy latest version (HEAD)."),
  confirm: z.boolean().describe("Must be true to proceed with deployment to production"),
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


