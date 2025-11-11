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
export const projectIdSchema = z.string().describe("Project ID in format: repository_name-project_name");

export const repositoryNameSchema = z.string().describe("Repository name");

export const projectNameSchema = z.string().describe("Project name");

export const tableIdSchema = z.string().describe("Table ID");

export const branchNameSchema = z.string().describe("Branch name");

export const commentSchema = z.string().optional().describe("Commit comment for the change");

// Tool input schemas
export const listProjectsSchema = z.object({
  repository: z.string().optional().describe("Filter by repository name"),
  status: z.string().optional().describe("Filter by project status (OPENED, CLOSED, etc.)"),
  tag: z.string().optional().describe("Filter by tag name"),
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

export const listTablesSchema = z.object({
  projectId: projectIdSchema,
  tableType: z.string().optional().describe("Filter by table type (datatype, spreadsheet, simplerules, etc.)"),
  name: z.string().optional().describe("Filter by table name pattern"),
  file: z.string().optional().describe("Filter by Excel file name"),
});

export const getTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
});

export const updateTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  view: z.record(z.any()).describe("Table view data to update"),
  comment: commentSchema,
});

export const getProjectHistorySchema = z.object({
  repository: repositoryNameSchema,
  projectName: projectNameSchema,
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
  deploymentRepository: z.string().describe("Target deployment repository"),
  version: z.string().optional().describe("Specific version to deploy (optional)"),
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
  name: z.string().describe("Name of the rule/table to create"),
  ruleType: z.enum([
    "datatype",
    "vocabulary",
    "spreadsheet",
    "simplerules",
    "smartrules",
    "method",
    "data",
    "test",
  ]).describe("Type of rule to create"),
  file: z.string().optional().describe("Excel file to create the rule in (optional)"),
  comment: commentSchema,
});

// =============================================================================
// Phase 2: Testing & Validation Schemas
// =============================================================================

export const runTestSchema = z.object({
  projectId: projectIdSchema,
  testIds: z.array(z.string()).optional().describe("Specific test IDs to run"),
  tableIds: z.array(z.string()).optional().describe("Run tests related to these table IDs"),
  runAll: z.boolean().optional().describe("Run all tests (default: false)"),
});

export const getProjectErrorsSchema = z.object({
  projectId: projectIdSchema,
  includeWarnings: z.boolean().optional().describe("Include warnings in the result (default: true)"),
});

// =============================================================================
// Phase 3: Versioning & Execution Schemas
// =============================================================================

export const copyTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  newName: z.string().optional().describe("New name for the copied table (auto-generated if not provided)"),
  targetFile: z.string().optional().describe("Target Excel file to copy the table to (same file if not provided)"),
  comment: commentSchema,
});

export const executeRuleSchema = z.object({
  projectId: projectIdSchema,
  ruleName: z.string().describe("Name of the rule/method to execute"),
  inputData: z.record(z.any()).describe("Input data for rule execution as JSON object"),
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

