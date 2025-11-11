/**
 * MCP Tool Definitions
 *
 * This module defines all available tools for the OpenL Tablets MCP server.
 * Each tool includes:
 * - Name and description
 * - Input schema (Zod-based validation)
 * - Metadata (_meta) for categorization and versioning
 *
 * To add a new tool:
 * 1. Add a Zod schema in schemas.ts
 * 2. Add the tool definition here
 * 3. Add the tool handler in the server implementation
 */

import { zodToJsonSchema } from "zod-to-json-schema";
import * as schemas from "./schemas.js";
import { TOOL_CATEGORIES } from "./constants.js";

/**
 * Tool metadata interface
 */
export interface ToolMetadata {
  /** Semantic version of the tool */
  version: string;
  /** Category for logical grouping */
  category: string;
  /** Whether the tool requires authentication */
  requiresAuth: boolean;
  /** Whether the tool modifies server state */
  modifiesState?: boolean;
}

/**
 * Tool definition interface
 */
export interface ToolDefinition {
  /** Unique tool name */
  name: string;
  /** Human-readable description */
  description: string;
  /** JSON Schema for input validation */
  inputSchema: Record<string, unknown>;
  /** Tool metadata */
  _meta: ToolMetadata;
}

/**
 * All available MCP tools for OpenL Tablets
 *
 * Tools are organized by category:
 * - System: Health checks and diagnostics
 * - Repository: Repository and branch management
 * - Project: Project lifecycle management
 * - Rules: Table/rules viewing and editing
 * - Version Control: Branching and history
 * - Deployment: Production deployment
 */
export const TOOLS: ToolDefinition[] = [
  // =============================================================================
  // Repository Tools
  // =============================================================================
  {
    name: "list_repositories",
    description: "List all design repositories in OpenL Tablets",
    inputSchema: zodToJsonSchema(schemas.z.object({})) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.REPOSITORY,
      requiresAuth: true,
    },
  },
  {
    name: "list_branches",
    description: "List branches in a repository",
    inputSchema: zodToJsonSchema(schemas.listBranchesSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.REPOSITORY,
      requiresAuth: true,
    },
  },

  // =============================================================================
  // Project Tools
  // =============================================================================
  {
    name: "list_projects",
    description:
      "List projects with optional filters (repository, status, tag)",
    inputSchema: zodToJsonSchema(schemas.listProjectsSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },
  {
    name: "get_project",
    description: "Get comprehensive project information including details, modules, dependencies, and metadata",
    inputSchema: zodToJsonSchema(schemas.getProjectSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },
  {
    name: "open_project",
    description: "Open a project for viewing/editing",
    inputSchema: zodToJsonSchema(schemas.projectIdSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "close_project",
    description: "Close an open project",
    inputSchema: zodToJsonSchema(schemas.projectActionSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "save_project",
    description: "Save project changes, creating a new version in repository (validates before saving)",
    inputSchema: zodToJsonSchema(schemas.saveProjectSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
      modifiesState: true,
    },
  },

  // =============================================================================
  // File Management Tools
  // =============================================================================
  {
    name: "upload_file",
    description: "Upload an Excel file with rules to a project (.xlsx or .xls only)",
    inputSchema: zodToJsonSchema(schemas.uploadFileSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "download_file",
    description: "Download an Excel file from OpenL project. Can download latest version (HEAD) or specific historical version using Git commit hash. Returns base64-encoded file content.",
    inputSchema: zodToJsonSchema(schemas.downloadFileSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },

  // =============================================================================
  // Rules (Tables) Tools
  // =============================================================================
  {
    name: "list_tables",
    description: "List all tables/rules in a project with optional filters for type, name, and file",
    inputSchema: zodToJsonSchema(schemas.listTablesSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "get_table",
    description: "Get detailed table data and structure",
    inputSchema: zodToJsonSchema(schemas.getTableSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "update_table",
    description: "Update table content",
    inputSchema: zodToJsonSchema(schemas.updateTableSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "create_rule",
    description: "Create a new rule/table in a project (Decision Table, Spreadsheet, Datatype, Test, etc.)",
    inputSchema: zodToJsonSchema(schemas.createRuleSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
      modifiesState: true,
    },
  },

  // =============================================================================
  // Deployment Tools
  // =============================================================================
  {
    name: "list_deployments",
    description: "List all deployments",
    inputSchema: zodToJsonSchema(schemas.z.object({})) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.DEPLOYMENT,
      requiresAuth: true,
    },
  },
  {
    name: "deploy_project",
    description: "Deploy a project to production",
    inputSchema: zodToJsonSchema(schemas.deployProjectSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.DEPLOYMENT,
      requiresAuth: true,
      modifiesState: true,
    },
  },

  // =============================================================================
  // Testing & Validation Tools
  // =============================================================================
  {
    name: "run_all_tests",
    description: "Run all tests in a project to validate rules correctness",
    inputSchema: zodToJsonSchema(schemas.runAllTestsSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "run_test",
    description: "Run specific tests with smart selection - can run selected test IDs, tests related to specific tables, or all tests",
    inputSchema: zodToJsonSchema(schemas.runTestSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "validate_project",
    description: "Validate project for errors and warnings before deployment",
    inputSchema: zodToJsonSchema(schemas.validateProjectSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },
  {
    name: "get_project_errors",
    description: "Get detailed project errors with categorization (type, syntax, reference errors) and auto-fix suggestions",
    inputSchema: zodToJsonSchema(schemas.getProjectErrorsSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },

  // =============================================================================
  // Phase 3: Versioning & Execution Tools
  // =============================================================================
  {
    name: "copy_table",
    description: "Copy a rule/table within a project or to another file",
    inputSchema: zodToJsonSchema(schemas.copyTableSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "execute_rule",
    description: "Execute a rule with input data to test its behavior and validate changes",
    inputSchema: zodToJsonSchema(schemas.executeRuleSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "compare_versions",
    description: "Compare two Git commit versions of a project using commit hashes to see what changed (added, modified, removed tables)",
    inputSchema: zodToJsonSchema(schemas.compareVersionsSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
      category: TOOL_CATEGORIES.VERSION_CONTROL,
      requiresAuth: true,
    },
  },

  // =============================================================================
  // Phase 4: Advanced Features
  // =============================================================================
  {
    name: "revert_version",
    description: "Revert project to a previous Git commit version using commit hash (creates new commit with old content, preserves history)",
    inputSchema: zodToJsonSchema(schemas.revertVersionSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
      category: TOOL_CATEGORIES.VERSION_CONTROL,
      requiresAuth: true,
      modifiesState: true,
    },
  },

  // =============================================================================
  // Phase 2: Git Version History Tools
  // =============================================================================
  {
    name: "get_file_history",
    description: "Get Git commit history for a specific file. Returns list of commits with hashes, authors, timestamps, and commit types. Use this to see all versions of a file over time.",
    inputSchema: zodToJsonSchema(schemas.getFileHistorySchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.VERSION_CONTROL,
      requiresAuth: true,
    },
  },
  {
    name: "get_project_history",
    description: "Get Git commit history for entire project. Returns chronological list of all commits with metadata about files and tables changed. Supports pagination and branch filtering.",
    inputSchema: zodToJsonSchema(schemas.getProjectHistorySchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.VERSION_CONTROL,
      requiresAuth: true,
    },
  },
];

/**
 * Get a tool definition by name
 *
 * @param name - Tool name
 * @returns Tool definition or undefined if not found
 */
export function getToolByName(name: string): ToolDefinition | undefined {
  return TOOLS.find((tool) => tool.name === name);
}

/**
 * Get all tools in a specific category
 *
 * @param category - Tool category
 * @returns Array of tool definitions in that category
 */
export function getToolsByCategory(category: string): ToolDefinition[] {
  return TOOLS.filter((tool) => tool._meta.category === category);
}

/**
 * Get all tool names
 *
 * @returns Array of all tool names
 */
export function getAllToolNames(): string[] {
  return TOOLS.map((tool) => tool.name);
}
