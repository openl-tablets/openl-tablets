/**
 * Tool Handlers for OpenL Tablets MCP Server
 *
 * This module implements the registerTool pattern to replace the switch statement
 * in index.ts. Each tool is registered individually with its own handler function.
 *
 * Benefits:
 * - Cleaner separation of concerns
 * - Easier to test individual tools
 * - Better type safety with dedicated handlers
 * - Proper MCP annotations for each tool
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { ErrorCode, McpError } from "@modelcontextprotocol/sdk/types.js";
import { zodToJsonSchema } from "zod-to-json-schema";

import { OpenLClient } from "./client.js";
import * as schemas from "./schemas.js";
import { formatResponse, paginateResults } from "./formatters.js";
import { validateBase64, validateResponseFormat, validatePagination } from "./validators.js";
import { logger } from "./logger.js";
import { parseProjectId, createProjectId, isAxiosError, sanitizeError, safeStringify } from "./utils.js";
import type * as Types from "./types.js";

/**
 * Tool response structure
 */
interface ToolResponse {
  content: Array<{ type: string; text: string }>;
}

/**
 * Tool handler function type
 */
type ToolHandler = (args: unknown, client: OpenLClient) => Promise<ToolResponse>;

/**
 * Tool definition with MCP metadata
 */
interface ToolDefinition {
  name: string;
  title: string;
  description: string;
  inputSchema: Record<string, unknown>;
  version: string; // Semantic version (e.g., "2.0.0")
  annotations?: {
    readOnlyHint?: boolean;
    openWorldHint?: boolean;
    idempotentHint?: boolean;
    destructiveHint?: boolean;
  };
  handler: ToolHandler;
}

/**
 * Registry of all tool handlers
 */
const toolHandlers = new Map<string, ToolDefinition>();

/**
 * Register a single tool with the registry
 *
 * @param tool - Tool definition with handler
 */
function registerTool(tool: ToolDefinition): void {
  toolHandlers.set(tool.name, tool);
}

/**
 * Get a tool definition by name
 *
 * @param name - Tool name
 * @returns Tool definition or undefined
 */
export function getToolByName(name: string): ToolDefinition | undefined {
  return toolHandlers.get(name);
}

/**
 * Get all registered tools (for ListTools handler)
 *
 * @returns Array of tool definitions without handlers
 */
export function getAllTools(): Array<Omit<ToolDefinition, "handler">> {
  return Array.from(toolHandlers.values()).map(({ handler, ...tool }) => tool);
}

/**
 * Execute a tool by name
 *
 * @param name - Tool name
 * @param args - Tool arguments
 * @param client - OpenL client instance
 * @returns Tool execution result
 */
export async function executeTool(
  name: string,
  args: unknown,
  client: OpenLClient
): Promise<ToolResponse> {
  const tool = toolHandlers.get(name);
  if (!tool) {
    throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${name}`);
  }

  try {
    return await tool.handler(args, client);
  } catch (error: unknown) {
    throw handleToolError(error, name);
  }
}

/**
 * Register all OpenL Tablets tools
 *
 * This function registers all tools with their handlers, replacing the
 * switch statement pattern with a more modular registry-based approach.
 *
 * @param server - MCP Server instance (for future use)
 * @param client - OpenL Tablets API client (for future use)
 */
export function registerAllTools(server: Server, client: OpenLClient): void {
  // =============================================================================
  // Repository Tools
  // =============================================================================

  registerTool({
    name: "openl_list_repositories",
    title: "openl List Repositories",
    version: "1.0.0",
    description:
      "List all design repositories in OpenL Tablets. Returns repository names, types, and status information. Use this to discover available repositories before accessing projects.",
    inputSchema: zodToJsonSchema(
      schemas.z
        .object({
          response_format: schemas.ResponseFormat.optional(),
          limit: schemas.z.number().int().positive().max(200).default(50).optional(),
          offset: schemas.z.number().int().nonnegative().default(0).optional(),
        })
        .strict()
    ) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        response_format?: "json" | "markdown";
        limit?: number;
        offset?: number;
      } | undefined;

      const format = validateResponseFormat(typedArgs?.response_format);
      const { limit, offset } = validatePagination(typedArgs?.limit, typedArgs?.offset);

      const repositories = await client.listRepositories();

      // Apply pagination
      const paginated = paginateResults(repositories, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: {
          limit,
          offset,
          total: paginated.total_count,
        },
        dataType: "repositories",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_list_branches",
    title: "openl List Branches",
    version: "1.0.0",
    description:
      "List all Git branches in a repository. Returns branch names and metadata (current branch, commit info). Use this to see available branches before switching or comparing versions.",
    inputSchema: zodToJsonSchema(schemas.listBranchesSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        repository: string;
        response_format?: "json" | "markdown";
        limit?: number;
        offset?: number;
      };

      if (!typedArgs || !typedArgs.repository) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required argument: repository. To find valid repositories, use: openl_list_repositories()"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);
      const { limit, offset } = validatePagination(typedArgs.limit, typedArgs.offset);

      const branches = await client.listBranches(typedArgs.repository);

      // Apply pagination
      const paginated = paginateResults(branches, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: {
          limit,
          offset,
          total: paginated.total_count,
        },
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Project Tools
  // =============================================================================

  registerTool({
    name: "openl_list_projects",
    title: "openl List Projects",
    version: "1.0.0",
    description:
      "List all projects with optional filters (repository, status, tag). Returns project names, status (OPENED/CLOSED), metadata, and a convenient 'projectId' field (format: 'repository-projectName') to use with other tools.",
    inputSchema: zodToJsonSchema(schemas.listProjectsSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = (args as Types.ProjectFilters & {
        response_format?: "json" | "markdown";
        limit?: number;
        offset?: number;
      }) || {};

      const format = validateResponseFormat(typedArgs.response_format);
      const { limit, offset } = validatePagination(typedArgs.limit, typedArgs.offset);

      // Extract filters (only those supported by ProjectFilters type)
      const filters: Types.ProjectFilters = {};
      if (typedArgs.repository) filters.repository = typedArgs.repository;
      if (typedArgs.status) filters.status = typedArgs.status;
      // Note: tag and branch parameters are in the schema but not in ProjectFilters type
      // They are ignored by the API client

      const projects = await client.listProjects(filters);

      // Transform projects to include a flat projectId field for easier use
      // Handle both OpenL 6.0.0+ (base64 string) and older versions (object) formats
      const transformedProjects = projects.map((project) => {
        const { repository, projectName } = parseProjectId(project.id);
        return {
          ...project,
          projectId: createProjectId(repository, projectName),
        };
      });

      // Apply pagination
      const paginated = paginateResults(transformedProjects, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: {
          limit,
          offset,
          total: paginated.total_count,
        },
        dataType: "projects",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_get_project",
    title: "openl Get Project",
    version: "1.0.0",
    description:
      "Get comprehensive project information including details, modules, dependencies, and metadata. Returns full project structure, configuration, and status.",
    inputSchema: zodToJsonSchema(schemas.getProjectSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required argument: projectId. To find valid project IDs, use: openl_list_projects()");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const project = await client.getProject(typedArgs.projectId);

      const formattedResult = formatResponse(project, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_update_project_status",
    title: "openl Update Project Status",
    version: "1.0.0",
    description:
      "Update project status with safety checks for unsaved changes. Unified tool for all project state transitions: opening, closing, saving, or switching branches.",
    inputSchema: zodToJsonSchema(schemas.updateProjectStatusSchema) as Record<string, unknown>,
    annotations: {
      destructiveHint: true, // Can discard changes if requested
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        status?: "LOCAL" | "ARCHIVED" | "OPENED" | "VIEWING_VERSION" | "EDITING" | "CLOSED";
        comment?: string;
        discardChanges?: boolean;
        confirm?: boolean;
        branch?: string;
        revision?: string;
        selectedBranches?: string[];
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required argument: projectId. To find valid project IDs, use: openl_list_projects()");
      }

      // Destructive operation: require confirmation when discarding changes
      if (typedArgs.discardChanges === true && typedArgs.confirm !== true) {
        throw new McpError(
          ErrorCode.InvalidParams,
          `Discarding unsaved changes is a destructive operation that will permanently lose all uncommitted work in project "${typedArgs.projectId}". ` +
          `To save changes instead, provide a comment parameter without discardChanges. ` +
          `To proceed with discarding changes, set both discardChanges: true AND confirm: true in your request. ` +
          `To review what will be lost, first use: openl_get_project(projectId: "${typedArgs.projectId}")`
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const result = await client.updateProjectStatus(typedArgs.projectId, {
        status: typedArgs.status,
        comment: typedArgs.comment,
        discardChanges: typedArgs.discardChanges,
        branch: typedArgs.branch,
        revision: typedArgs.revision,
        selectedBranches: typedArgs.selectedBranches,
      });

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // File Management Tools
  // =============================================================================

  registerTool({
    name: "openl_upload_file",
    title: "openl Upload File",
    version: "1.0.0",
    description:
      "Upload an Excel file (.xlsx or .xls) containing rules to a project. The file is uploaded to OpenL Studio workspace but NOT committed to Git yet.",
    inputSchema: zodToJsonSchema(schemas.uploadFileSchema) as Record<string, unknown>,
    annotations: {
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        fileName: string;
        fileContent: string;
        comment?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.fileName || !typedArgs.fileContent) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required arguments: projectId, fileName, fileContent"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Validate base64 content
      if (!validateBase64(typedArgs.fileContent)) {
        throw new McpError(ErrorCode.InvalidParams, "Invalid base64 content in fileContent parameter");
      }

      // Decode base64 file content
      const buffer = Buffer.from(typedArgs.fileContent, "base64");

      const result = await client.uploadFile(typedArgs.projectId, typedArgs.fileName, buffer, typedArgs.comment);

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_download_file",
    title: "openl Download File",
    version: "1.0.0",
    description:
      "Download an Excel file from OpenL project. Can download latest version (HEAD) or specific historical version using Git commit hash.",
    inputSchema: zodToJsonSchema(schemas.downloadFileSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        fileName: string;
        version?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.fileName) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, fileName");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const fileBuffer = await client.downloadFile(typedArgs.projectId, typedArgs.fileName, typedArgs.version);

      // Return base64-encoded content with metadata
      const result = {
        fileName: typedArgs.fileName,
        fileContent: fileBuffer.toString("base64"),
        size: fileBuffer.length,
        version: typedArgs.version || "HEAD",
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Rules (Tables) Tools
  // =============================================================================

  registerTool({
    name: "openl_list_tables",
    title: "openl List Tables",
    version: "1.0.0",
    description: "List all tables/rules in a project with optional filters for type, name, and file",
    inputSchema: zodToJsonSchema(schemas.listTablesSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        tableType?: string;
        name?: string;
        file?: string;
        response_format?: "json" | "markdown";
        limit?: number;
        offset?: number;
      };

      if (!typedArgs || !typedArgs.projectId) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required argument: projectId. To find valid project IDs, use: openl_list_projects()");
      }

      const format = validateResponseFormat(typedArgs.response_format);
      const { limit, offset } = validatePagination(typedArgs.limit, typedArgs.offset);

      const filters: Types.TableFilters = {};
      if (typedArgs.tableType) filters.tableType = typedArgs.tableType as Types.TableType;
      if (typedArgs.name) filters.name = typedArgs.name;
      if (typedArgs.file) filters.file = typedArgs.file;

      const tables = await client.listTables(typedArgs.projectId, filters);

      // Apply pagination
      const paginated = paginateResults(tables, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: {
          limit,
          offset,
          total: paginated.total_count,
        },
        dataType: "tables",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_get_table",
    title: "openl Get Table",
    version: "1.0.0",
    description:
      "Get detailed information about a specific table/rule. Returns table structure, signature, conditions, actions, dimension properties, and all row data.",
    inputSchema: zodToJsonSchema(schemas.getTableSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        tableId: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.tableId) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, tableId. Use openl_list_tables() to find valid table IDs");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const table = await client.getTable(typedArgs.projectId, typedArgs.tableId);

      const formattedResult = formatResponse(table, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_update_table",
    title: "openl Update Table",
    version: "1.0.0",
    description:
      "Update table content including conditions, actions, and data rows. CRITICAL: Must send the FULL table structure (not just modified fields).",
    inputSchema: zodToJsonSchema(schemas.updateTableSchema) as Record<string, unknown>,
    annotations: {
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        tableId: string;
        view: Types.EditableTableView;
        comment?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.tableId || !typedArgs.view) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, tableId, view");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      await client.updateTable(typedArgs.projectId, typedArgs.tableId, typedArgs.view, typedArgs.comment);

      const result = {
        success: true,
        message: `Successfully updated table ${typedArgs.tableId}`,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_append_table",
    title: "openl Append Table",
    version: "1.0.0",
    description:
      "Append new rows/fields to an existing table. Used to add data to Datatype or Data tables without replacing the entire structure.",
    inputSchema: zodToJsonSchema(schemas.appendTableSchema) as Record<string, unknown>,
    annotations: {
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        tableId: string;
        appendData: Types.AppendTableView;
        comment?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.tableId || !typedArgs.appendData) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, tableId, appendData");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      await client.appendProjectTable(typedArgs.projectId, typedArgs.tableId, typedArgs.appendData);

      const result = {
        success: true,
        message: `Successfully appended ${typedArgs.appendData.fields.length} field(s) to table ${typedArgs.tableId}`,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_create_rule",
    title: "openl Create Rule",
    version: "1.0.0",
    description:
      "Create a new table/rule in OpenL project. Supports Decision Tables (Rules/SimpleRules/SmartRules/SimpleLookup/SmartLookup), Spreadsheet tables, and other types.",
    inputSchema: zodToJsonSchema(schemas.createRuleSchema) as Record<string, unknown>,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        name: string;
        tableType: Types.TableType;
        returnType?: string;
        parameters?: Array<{ type: string; name: string }>;
        file?: string;
        properties?: Record<string, unknown>;
        comment?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.name || !typedArgs.tableType) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, name, tableType");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const result = await client.createRule(typedArgs.projectId, {
        name: typedArgs.name,
        tableType: typedArgs.tableType,
        returnType: typedArgs.returnType,
        parameters: typedArgs.parameters,
        file: typedArgs.file,
        properties: typedArgs.properties,
        comment: typedArgs.comment,
      });

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Deployment Tools
  // =============================================================================

  registerTool({
    name: "openl_list_deployments",
    title: "openl List Deployments",
    version: "1.0.0",
    description:
      "List all active deployments across production environments. Returns deployment names, repositories, versions, and status information.",
    inputSchema: zodToJsonSchema(
      schemas.z
        .object({
          response_format: schemas.ResponseFormat.optional(),
          limit: schemas.z.number().int().positive().max(200).default(50).optional(),
          offset: schemas.z.number().int().nonnegative().default(0).optional(),
        })
        .strict()
    ) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        response_format?: "json" | "markdown";
        limit?: number;
        offset?: number;
      } | undefined;

      const format = validateResponseFormat(typedArgs?.response_format);
      const { limit, offset } = validatePagination(typedArgs?.limit, typedArgs?.offset);

      const deployments = await client.listDeployments();

      // Apply pagination
      const paginated = paginateResults(deployments, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: {
          limit,
          offset,
          total: paginated.total_count,
        },
        dataType: "deployments",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_deploy_project",
    title: "openl Deploy Project",
    version: "1.0.0",
    description:
      "Deploy a project to production environment. Publishes rules to a deployment repository for runtime execution.",
    inputSchema: zodToJsonSchema(schemas.deployProjectSchema) as Record<string, unknown>,
    annotations: {
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectName: string;
        repository: string;
        deploymentRepository: string;
        version?: string;
        confirm?: boolean;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectName || !typedArgs.repository || !typedArgs.deploymentRepository) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required arguments: projectName, repository, deploymentRepository"
        );
      }

      // Destructive operation: require confirmation
      if (typedArgs.confirm !== true) {
        const projectId = `${typedArgs.repository}-${typedArgs.projectName}`;
        throw new McpError(
          ErrorCode.InvalidParams,
          `Deploying to production is a destructive operation that publishes rules to "${typedArgs.deploymentRepository}". ` +
          `Before deploying, ensure:\n` +
          `  1. All tests pass (use OpenL WebStudio UI to run tests)\n` +
          `  2. Project validates without errors (use OpenL WebStudio UI to validate)\n` +
          `  3. You have reviewed recent changes with: openl_get_project_history(projectId: "${projectId}")\n\n` +
          `To proceed with deployment, set confirm: true in your request.`
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Build projectId from repository and projectName
      const projectId = `${typedArgs.repository}-${typedArgs.projectName}`;

      await client.deployProject({
        projectId,
        deploymentName: typedArgs.projectName, // Use project name as deployment name
        productionRepositoryId: typedArgs.deploymentRepository,
        comment: typedArgs.version ? `Deploy version ${typedArgs.version}` : undefined,
      });

      const result = {
        success: true,
        message: `Successfully deployed ${typedArgs.projectName} to ${typedArgs.deploymentRepository}`,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Execution Tools
  // =============================================================================

  registerTool({
    name: "openl_execute_rule",
    title: "openl Execute Rule",
    version: "1.0.0",
    description:
      "Execute a rule with input data to test its behavior and validate changes. Runs the rule with provided parameters and returns calculated result.",
    inputSchema: zodToJsonSchema(schemas.executeRuleSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        ruleName: string;
        inputData: Record<string, unknown>;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.ruleName || !typedArgs.inputData) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, ruleName, inputData");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const result = await client.executeRule({
        projectId: typedArgs.projectId,
        ruleName: typedArgs.ruleName,
        inputData: typedArgs.inputData,
      });

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Version Control Tools
  // =============================================================================

  registerTool({
    name: "openl_revert_version",
    title: "openl Revert Version",
    version: "1.0.0",
    description:
      "Revert project to a previous Git commit using commit hash. Creates a new commit that restores old content while preserving full history.",
    inputSchema: zodToJsonSchema(schemas.revertVersionSchema) as Record<string, unknown>,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        targetVersion: string;
        comment?: string;
        confirm?: boolean;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.targetVersion) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, targetVersion");
      }

      // Destructive operation: require confirmation
      if (typedArgs.confirm !== true) {
        throw new McpError(
          ErrorCode.InvalidParams,
          `This operation will revert project "${typedArgs.projectId}" to version "${typedArgs.targetVersion}", ` +
          `which is a destructive action that creates a new commit with the old state. ` +
          `To proceed, set confirm: true in your request. ` +
          `To review the target version first, use: openl_get_project_history(projectId: "${typedArgs.projectId}")`
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const result = await client.revertVersion({
        projectId: typedArgs.projectId,
        targetVersion: typedArgs.targetVersion,
        comment: typedArgs.comment,
      });

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // TEMPORARILY DISABLED - openl_get_file_history
  // Tool is not working correctly and needs implementation fixes
  /*
  registerTool({
    name: "openl_get_file_history",
    title: "openl get file history",
    version: "1.0.0",
    description:
      "Get Git commit history for a specific file. Returns list of commits with hashes, authors, timestamps, and commit types.",
    inputSchema: zodToJsonSchema(schemas.getFileHistorySchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        filePath: string;
        limit?: number;
        offset?: number;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.filePath) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, filePath");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const result = await client.getFileHistory({
        projectId: typedArgs.projectId,
        filePath: typedArgs.filePath,
        limit: typedArgs.limit,
        offset: typedArgs.offset,
      });

      const formattedResult = formatResponse(result, format, {
        dataType: "history",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });
  */

  // TEMPORARILY DISABLED - openl_get_project_history
  // Tool is not working correctly and needs implementation fixes
  /*
  registerTool({
    name: "openl_get_project_history",
    title: "openl get project history",
    version: "1.0.0",
    description:
      "Get Git commit history for entire project. Returns chronological list of all commits with metadata about files and tables changed.",
    inputSchema: zodToJsonSchema(schemas.getProjectHistorySchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        limit?: number;
        offset?: number;
        branch?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required argument: projectId. To find valid project IDs, use: openl_list_projects()");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Convert limit/offset to page/size for API compatibility
      const page = typedArgs.offset ? Math.floor(typedArgs.offset / (typedArgs.limit || 50)) : undefined;
      const size = typedArgs.limit;

      const result = await client.getProjectHistory({
        projectId: typedArgs.projectId,
        page,
        size,
        branch: typedArgs.branch,
      });

      const formattedResult = formatResponse(result, format, {
        dataType: "history",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });
  */

  logger.info(`Registered ${toolHandlers.size} OpenL Tablets tools`);
}

/**
 * Handle tool execution errors with enhanced context
 *
 * @param error - Error to handle
 * @param toolName - Name of the tool that failed
 * @returns McpError with enhanced context
 */
function handleToolError(error: unknown, toolName: string): McpError {
  // Enhanced error handling with context
  if (isAxiosError(error)) {
    const status = error.response?.status;
    const sanitizedMessage = sanitizeError(error);
    const endpoint = error.config?.url;
    const method = error.config?.method?.toUpperCase();

    const errorDetails = {
      status,
      endpoint,
      method,
      tool: toolName,
    };

    logger.error(`Tool error: ${toolName}`, errorDetails);

    throw new McpError(
      ErrorCode.InternalError,
      `OpenL Tablets API error (${status}): ${sanitizedMessage} [${method} ${endpoint}]`,
      errorDetails
    );
  }

  // Re-throw McpErrors as-is
  if (error instanceof McpError) {
    throw error;
  }

  // Wrap other errors with sanitization
  const sanitizedMessage = sanitizeError(error);
  logger.error(`Tool error: ${toolName}`, { error: sanitizedMessage });

  throw new McpError(
    ErrorCode.InternalError,
    `Error executing ${toolName}: ${sanitizedMessage}`,
    { tool: toolName }
  );
}
