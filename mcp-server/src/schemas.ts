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
// Project Creation & Management
// =============================================================================

export const createProjectSchema = z.object({
  repository: repositoryNameSchema,
  projectName: projectNameSchema,
  comment: commentSchema,
  projectTemplate: z.string().optional().describe("Template to use for new project"),
});

export const deleteProjectSchema = z.object({
  projectId: projectIdSchema,
  comment: commentSchema,
});

// =============================================================================
// File Management
// =============================================================================

export const uploadFileSchema = z.object({
  projectId: projectIdSchema,
  filePath: z.string().describe("Path where file should be stored in project"),
  content: z.string().describe("Base64 encoded file content"),
  comment: commentSchema,
});

export const listFilesSchema = z.object({
  projectId: projectIdSchema,
});

export const deleteFileSchema = z.object({
  projectId: projectIdSchema,
  filePath: z.string().describe("Path to file to delete"),
  comment: commentSchema,
});

// =============================================================================
// Table/Rule Creation & Deletion
// =============================================================================

export const createTableSchema = z.object({
  projectId: projectIdSchema,
  tableName: z.string().describe("Name for the new table"),
  tableType: z.enum([
    "datatype",
    "vocabulary",
    "spreadsheet",
    "simplerules",
    "smartrules",
    "method",
    "test",
    "data",
  ]).describe("Type of table to create"),
  file: z.string().describe("Excel file where table should be created"),
  comment: commentSchema,
});

export const deleteTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  comment: commentSchema,
});

// =============================================================================
// Testing & Validation
// =============================================================================

export const runTestSchema = z.object({
  projectId: projectIdSchema,
  testTableId: z.string().describe("ID of the test table to run"),
});

export const runAllTestsSchema = z.object({
  projectId: projectIdSchema,
});

export const validateProjectSchema = z.object({
  projectId: projectIdSchema,
});

// =============================================================================
// Rule Execution
// =============================================================================

export const executeRulesSchema = z.object({
  projectId: projectIdSchema,
  tableName: z.string().describe("Name of the rule table to execute"),
  inputData: z.record(z.any()).describe("Input data for rule execution"),
});

