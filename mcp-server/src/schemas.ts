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
});

export const getProjectSchema = z.object({
  projectId: projectIdSchema,
});

export const getProjectInfoSchema = z.object({
  repository: repositoryNameSchema,
  projectName: projectNameSchema,
});

export const projectActionSchema = z.object({
  projectId: projectIdSchema,
});

export const updateProjectStatusSchema = z.object({
  projectId: projectIdSchema,
  status: z.enum(["LOCAL", "ARCHIVED", "OPENED", "VIEWING_VERSION", "EDITING", "CLOSED"]).optional().describe("Project status to set. OPENED = open and available for editing (read-only if locked by another user). EDITING = currently editing with unsaved changes (auto-set by OpenL on first edit, locks project). VIEWING_VERSION = viewing outdated version (another user saved, need to re-open for latest). CLOSED = closed and unlocked"),
  comment: commentSchema.describe("Git commit comment. When provided on a modified project, saves changes before applying status change. Required when closing a project with unsaved changes (unless discardChanges is true)"),
  discardChanges: z.boolean().optional().describe("Explicitly confirm discarding unsaved changes when closing. Set to true to close an EDITING project without saving. Safety flag to prevent accidental data loss."),
  branch: branchNameSchema.optional().describe("Switch to a different Git branch"),
  revision: z.string().optional().describe("Switch to a specific Git revision/commit hash for read-only viewing"),
  selectedBranches: z.array(z.string()).optional().describe("List of branches to select for multi-branch projects"),
});

export const listTablesSchema = z.object({
  projectId: projectIdSchema,
  tableType: z.string().optional().describe("Filter by table type: 'Rules', 'SimpleRules', 'SmartRules', 'SimpleLookup', 'SmartLookup', 'Spreadsheet', 'Datatype', 'Data', 'Test', 'Method', etc. Omit to show all types."),
  name: z.string().optional().describe("Filter by table name pattern using wildcards (e.g., 'calculate*', '*Premium*', 'validate*'). Omit to show all tables."),
  file: z.string().optional().describe("Filter by Excel file name (e.g., 'rules/Insurance.xlsx', 'Premium.xlsx'). Omit to show tables from all files."),
});

export const getTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
});

export const updateTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  view: z.record(z.any()).describe("Table view data to update - JSON object containing table structure with conditions, actions, and data rows. Structure varies by table type."),
  comment: commentSchema,
});

export const listBranchesSchema = z.object({
  repository: repositoryNameSchema,
});

export const createBranchSchema = z.object({
  projectId: projectIdSchema,
  branchName: branchNameSchema,
  comment: commentSchema,
});

export const deployProjectSchema = z.object({
  projectName: projectNameSchema,
  repository: repositoryNameSchema,
  deploymentRepository: z.string().describe("Target deployment repository name (e.g., 'production-deploy', 'staging-deploy'). Must be configured in OpenL Tablets."),
  version: z.string().optional().describe("Specific Git commit hash to deploy (e.g., '7a3f2b1c...'). Omit to deploy latest version (HEAD)."),
});

// =============================================================================
// Testing & Validation (Critical Missing Tools)
// =============================================================================

export const runAllTestsSchema = z.object({
  projectId: projectIdSchema,
});

export const validateProjectSchema = z.object({
  projectId: projectIdSchema,
});

// =============================================================================
// Phase 1: New Tool Schemas
// =============================================================================

export const saveProjectSchema = z.object({
  projectId: projectIdSchema,
  comment: commentSchema,
});

export const uploadFileSchema = z.object({
  projectId: projectIdSchema,
  fileName: z.string().describe("Name of the Excel file to upload (.xlsx or .xls)"),
  fileContent: z.string().describe("Base64-encoded file content"),
  comment: commentSchema,
});

export const downloadFileSchema = z.object({
  projectId: projectIdSchema,
  fileName: z.string().describe("Name of the Excel file to download (e.g., 'rules/Insurance.xlsx')"),
  version: z.string().optional().describe("Git commit hash to download specific version (e.g., '7a3f2b1c...'). Omit for latest version (HEAD)"),
});

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
});

// =============================================================================
// Phase 2: Testing & Validation Schemas
// =============================================================================

export const runTestSchema = z.object({
  projectId: projectIdSchema,
  testIds: z.array(z.string()).optional().describe("Specific test IDs to run (e.g., ['test_calculatePremium_001', 'test_validate_002']). Mutually exclusive with tableIds and runAll."),
  tableIds: z.array(z.string()).optional().describe("Run all tests related to these table IDs (e.g., ['calculatePremium_1234']). Tests table being tested, not test table IDs."),
  runAll: z.boolean().optional().describe("Run all tests in the project (default: false). Set to true to run comprehensive test suite."),
});

export const getProjectErrorsSchema = z.object({
  projectId: projectIdSchema,
  includeWarnings: z.boolean().optional().describe("Include warnings along with errors (default: true). Set to false to show only critical errors that block deployment."),
});

// =============================================================================
// Phase 3: Versioning & Execution Schemas
// =============================================================================

export const copyTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  newName: z.string().optional().describe("New name for the copied table (e.g., 'calculatePremium_v2', 'validatePolicy_backup'). Auto-generated if omitted."),
  targetFile: z.string().optional().describe("Target Excel file path (e.g., 'rules/Premium_v2.xlsx'). Copies to same file if omitted."),
  comment: commentSchema,
});

export const executeRuleSchema = z.object({
  projectId: projectIdSchema,
  ruleName: z.string().describe("Name of the rule/method to execute (e.g., 'calculatePremium', 'validatePolicy'). Must match exact table name."),
  inputData: z.record(z.any()).describe("Input data for rule execution as JSON object with parameter names as keys (e.g., { \"driverType\": \"SAFE\", \"age\": 30, \"vehicleValue\": 25000 })"),
});

export const compareVersionsSchema = z.object({
  projectId: projectIdSchema,
  baseCommitHash: z.string().describe("Base Git commit hash to compare from (e.g., '7a3f2b1c...')"),
  targetCommitHash: z.string().describe("Target Git commit hash to compare to (e.g., '9e5d8a2f...')"),
});

// =============================================================================
// Phase 4: Advanced Features
// =============================================================================

export const revertVersionSchema = z.object({
  projectId: projectIdSchema,
  targetVersion: z.string().describe("Git commit hash to revert to (e.g., '7a3f2b1c...')"),
  comment: commentSchema,
});

// =============================================================================
// Phase 2: Git Version History Schemas
// =============================================================================

export const getFileHistorySchema = z.object({
  projectId: projectIdSchema,
  filePath: z.string().min(1).describe("File path within project (e.g., 'rules/Insurance.xlsx')"),
  limit: z.number().int().positive().max(200).optional().describe("Maximum number of commits to return (default: 50, max: 200)"),
  offset: z.number().int().nonnegative().optional().describe("Number of commits to skip for pagination (default: 0)"),
});

export const getProjectHistorySchema = z.object({
  projectId: projectIdSchema,
  limit: z.number().int().positive().max(200).optional().describe("Maximum commits to return (default: 50, max: 200)"),
  offset: z.number().int().nonnegative().optional().describe("Commits to skip for pagination (default: 0)"),
  branch: z.string().optional().describe("Git branch name (default: current branch)"),
});

// =============================================================================
// Phase 3: Dimension Properties Schemas
// =============================================================================

export const getFileNamePatternSchema = z.object({
  projectId: projectIdSchema,
});

export const setFileNamePatternSchema = z.object({
  projectId: projectIdSchema,
  pattern: z.string().describe("File name pattern with property placeholders (e.g., '.*-%state%-%effectiveDate:MMddyyyy%-%lob%')"),
});

export const getTablePropertiesSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
});

export const setTablePropertiesSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  properties: z.record(z.any()).describe("Dimension properties object (e.g., { state: 'CA', lob: 'Auto', effectiveDate: '01/01/2025' })"),
  comment: commentSchema,
});

