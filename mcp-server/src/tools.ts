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
    description: "List all design repositories in OpenL Tablets. Returns repository names, types, and status information. Use this to discover available repositories before accessing projects.",
    inputSchema: zodToJsonSchema(schemas.z.object({})) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.REPOSITORY,
      requiresAuth: true,
    },
  },
  {
    name: "list_branches",
    description: "List all Git branches in a repository. Returns branch names and metadata (current branch, commit info). Use this to see available branches before switching or comparing versions.",
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
      "List all projects with optional filters (repository, status, tag). Returns project names, status (OPENED/CLOSED), metadata, and a convenient 'projectId' field (format: 'repository-projectName') to use with other tools. Use this to discover and filter projects before opening them for editing.",
    inputSchema: zodToJsonSchema(schemas.listProjectsSchema) as Record<string, unknown>,
    _meta: {
      version: "1.1.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },
  {
    name: "get_project",
    description: "Get comprehensive project information including details, modules, dependencies, and metadata. Returns full project structure, configuration, and status. Use this to understand project organization before making changes.",
    inputSchema: zodToJsonSchema(schemas.getProjectSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },
  {
    name: "update_project_status",
    description: "Update project status with safety checks for unsaved changes. Unified tool for all project state transitions: opening, closing, saving, or switching branches. Status behavior: OPENED (open for editing, read-only if locked by another user), EDITING (has unsaved changes, auto-set by OpenL on first edit), VIEWING_VERSION (viewing outdated version after another user saved, need to re-open), CLOSED (closed and unlocked). Prevents accidental data loss by requiring explicit confirmation when closing EDITING projects. Use cases: 1) Open: {status: 'OPENED'}, 2) Save and close: {status: 'CLOSED', comment: 'changes'}, 3) Save only: {comment: 'intermediate save'}, 4) Force close: {status: 'CLOSED', discardChanges: true}, 5) Switch branch: {branch: 'develop'}",
    inputSchema: zodToJsonSchema(schemas.updateProjectStatusSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
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
    description: "Upload an Excel file (.xlsx or .xls) containing rules to a project. Creates a new Git commit with the file. Returns commit hash, file size, and upload confirmation. Use this to add or replace Excel rule files.",
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
    description: "Download an Excel file from OpenL project. Can download latest version (HEAD) or specific historical version using Git commit hash. Returns base64-encoded file content. IMPORTANT: Use the exact 'file' field from list_tables() response as the fileName parameter (e.g., 'Rules.xlsx' or 'rules/Premium.xlsx'). Do NOT construct file paths manually - they may not match OpenL's internal structure.",
    inputSchema: zodToJsonSchema(schemas.downloadFileSchema) as Record<string, unknown>,
    _meta: {
      version: "2.1.0",
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
    description: "Get detailed information about a specific table/rule. Returns table structure, signature, conditions, actions, dimension properties, and all row data. Use this to understand existing rules before modifying them.",
    inputSchema: zodToJsonSchema(schemas.getTableSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "update_table",
    description: "Update table content including conditions, actions, and data rows. CRITICAL: Must send the FULL table structure (not just modified fields). Required workflow: 1) Call get_table() to retrieve complete structure, 2) Modify the returned object (e.g., update rules array, add fields), 3) Pass the ENTIRE modified object to update_table(). Required fields: id, tableType, kind, name, plus type-specific fields (rules for SimpleRules, rows for Spreadsheet, fields for Datatype). Modifies table in memory (requires save_project to persist changes).",
    inputSchema: zodToJsonSchema(schemas.updateTableSchema) as Record<string, unknown>,
    _meta: {
      version: "1.1.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "append_table",
    description: "Append new rows/fields to an existing table. Used to add data to Datatype or Data tables without replacing the entire structure. Specify the table type and array of field definitions with names, types, and optional required/defaultValue properties. More efficient than update_table for simple additions. Modifies table in memory (requires save_project to persist changes).",
    inputSchema: zodToJsonSchema(schemas.appendTableSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "create_rule",
    description: "Create a new table/rule in OpenL project. Supports Decision Tables (Rules/SimpleRules/SmartRules/SimpleLookup/SmartLookup), Spreadsheet tables, and other types. Specify table type, return type, parameters, and optional dimension properties. NOTE: This endpoint may not be supported in all OpenL versions (returns 405 in OpenL 6.0.0). Alternative: Use upload_file to upload Excel files with table definitions, or use OpenL WebStudio UI.",
    inputSchema: zodToJsonSchema(schemas.createRuleSchema) as Record<string, unknown>,
    _meta: {
      version: "2.0.0",
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
    description: "List all active deployments across production environments. Returns deployment names, repositories, versions, and status information. Use this to see what's currently deployed before making deployment decisions.",
    inputSchema: zodToJsonSchema(schemas.z.object({})) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.DEPLOYMENT,
      requiresAuth: true,
    },
  },
  {
    name: "deploy_project",
    description: "Deploy a project to production environment. Publishes rules to a deployment repository for runtime execution. Returns deployment status and confirmation. Requires validated project with no errors.",
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
  // Note: run_all_tests and run_test have been removed because the OpenL Tablets
  // REST API does not expose /tests/run or /tests/run-selected endpoints.
  // Use execute_rule to manually test individual rules with input data instead.
  {
    name: "validate_project",
    description: "Validate entire project for compilation errors, type errors, and warnings. Returns validation status and detailed error list if issues found. Required before deployment; use this to catch issues early.",
    inputSchema: zodToJsonSchema(schemas.validateProjectSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },
  {
    name: "get_project_errors",
    description: "Get comprehensive project error analysis with detailed categorization (type errors, syntax errors, reference errors) and auto-fix suggestions. Returns error severity, locations, and actionable guidance. Use this to understand and fix validation failures.",
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
    description: "Copy a rule/table within the same project or to a different Excel file. Creates a duplicate with optional new name and dimension properties. Returns new table ID and location. Use this to create rule variations or backups.",
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
    description: "Execute a rule with input data to test its behavior and validate changes. Runs the rule with provided parameters and returns calculated result. Use this to verify rule logic before deploying changes.",
    inputSchema: zodToJsonSchema(schemas.executeRuleSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "compare_versions",
    description: "Compare two Git commit versions of a project using commit hashes. Returns detailed diff showing added, modified, and removed tables/files with specific changes. Use this to review what changed between versions before reverting or deploying.",
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
    description: "Revert project to a previous Git commit using commit hash. Creates a new commit that restores old content while preserving full history. Returns new commit hash. Use this to roll back problematic changes while maintaining audit trail.",
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

  // =============================================================================
  // Phase 3: Dimension Properties Tools
  // =============================================================================
  {
    name: "get_file_name_pattern",
    description: "Get dimension properties file naming pattern from rules.xml. Returns pattern and list of properties used in file names (state, lob, effectiveDate, etc.).",
    inputSchema: zodToJsonSchema(schemas.getFileNamePatternSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
    },
  },
  {
    name: "set_file_name_pattern",
    description: "Set dimension properties file naming pattern in rules.xml. Pattern determines how properties are encoded in file names (e.g., '%state%-%lob%' creates 'CA-Auto.xlsx').",
    inputSchema: zodToJsonSchema(schemas.setFileNamePatternSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.PROJECT,
      requiresAuth: true,
      modifiesState: true,
    },
  },
  {
    name: "get_table_properties",
    description: "Get dimension properties for a specific table. Returns properties like state, lob, effectiveDate that determine when and where this rule version applies.",
    inputSchema: zodToJsonSchema(schemas.getTablePropertiesSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
    },
  },
  {
    name: "set_table_properties",
    description: "Set dimension properties for a table. Properties define business context (state, lob, effectiveDate, etc.) for rule versioning within the same Git commit.",
    inputSchema: zodToJsonSchema(schemas.setTablePropertiesSchema) as Record<string, unknown>,
    _meta: {
      version: "1.0.0",
      category: TOOL_CATEGORIES.RULES,
      requiresAuth: true,
      modifiesState: true,
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
