/**
 * Zod schemas for MCP tool inputs
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
