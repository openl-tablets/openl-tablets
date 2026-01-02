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
      "List all design repositories in OpenL Tablets. Returns repository information including 'id' (internal identifier) and 'name' (display name). Use the 'name' field when working with repositories in other tools. Example: if response contains {id: 'design-repo', name: 'Design Repository'}, use 'Design Repository' (the name) in other tools like list_projects(repository: 'Design Repository').",
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

      const format = validateResponseFormat(typedArgs && typedArgs.response_format);
      const { limit, offset } = validatePagination(typedArgs && typedArgs.limit, typedArgs && typedArgs.offset);

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
      "List all Git branches in a repository. Returns branch names and metadata (current branch, commit info). Use this to see available branches before switching or comparing versions. Use repository name (not ID) - e.g., 'Design Repository' instead of 'design-repo'.",
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

      // Convert repository name to ID for API call
      const repositoryId = await client.getRepositoryIdByName(typedArgs.repository);
      const branches = await client.listBranches(repositoryId);

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
      "List all projects with optional filters (repository, status, tags). Returns project names, status (OPENED/CLOSED), metadata, and a convenient 'projectId' field (base64-encoded format from API) to use with other tools. IMPORTANT: The 'projectId' is returned exactly as provided by the API and should be used without modification. Use repository name (not ID) - e.g., 'Design Repository' instead of 'design-repo'. Example: if list_repositories returns {id: 'design-repo', name: 'Design Repository'}, use repository: 'Design Repository' (the name).",
    inputSchema: zodToJsonSchema(schemas.listProjectsSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = (args as {
        repository?: string;
        status?: "LOCAL" | "ARCHIVED" | "OPENED" | "VIEWING_VERSION" | "EDITING" | "CLOSED";
        tags?: Record<string, string>;
        response_format?: "json" | "markdown";
        limit?: number;
        offset?: number;
      }) || {};

      const format = validateResponseFormat(typedArgs.response_format);
      const { limit, offset } = validatePagination(typedArgs.limit, typedArgs.offset);

      // Extract filters (only those supported by ProjectFilters type)
      const filters: Types.ProjectFilters = {};
      // Convert repository name to ID for API call
      if (typedArgs.repository) {
        filters.repository = await client.getRepositoryIdByName(typedArgs.repository);
      }
      if (typedArgs.status) filters.status = typedArgs.status;
      if (typedArgs.tags) filters.tags = typedArgs.tags;

      const projectsResponse = await client.listProjects(filters);

      // Handle case when API returns object instead of array
      // Some API versions return { data: [...], pagination: {...} } or similar structures
      let projects: Types.ProjectSummary[];
      if (Array.isArray(projectsResponse)) {
        projects = projectsResponse;
      } else if (projectsResponse && typeof projectsResponse === 'object' && 'data' in projectsResponse && Array.isArray((projectsResponse as any).data)) {
        // API returned object with 'data' field containing array
        projects = (projectsResponse as any).data;
      } else if (projectsResponse && typeof projectsResponse === 'object' && 'content' in projectsResponse && Array.isArray((projectsResponse as any).content)) {
        // API returned object with 'content' field containing array
        projects = (projectsResponse as any).content;
      } else {
        // Fallback: try to convert to array or use empty array
        logger.warn('Unexpected projects response format, expected array but got object', {
          responseType: typeof projectsResponse,
          hasData: 'data' in projectsResponse,
          hasContent: 'content' in projectsResponse,
        });
        projects = [];
      }

      // Transform projects to include a flat projectId field for easier use
      // Use original project.id from API response without modification
      // Handle both OpenL 6.0.0+ (base64 string) and older versions (object) formats
      const transformedProjects = projects.map((project) => {
        // Use original project.id directly - if it's a string (base64), use as-is
        // If it's an object (old format), convert to base64 format like the API expects
        let projectId: string;
        if (typeof project.id === 'string') {
          // Already in base64 format from API - use directly
          projectId = project.id;
        } else {
          // Old format object - convert to base64 format
          const colonFormat = `${project.id.repository}:${project.id.projectName}`;
          projectId = Buffer.from(colonFormat, 'utf-8').toString('base64');
        }
        return {
          ...project,
          projectId,
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
        status?: "OPENED" | "CLOSED";
        comment?: string;
        branch?: string;
        revision?: string;
        selectedBranches?: string[];
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required argument: projectId. To find valid project IDs, use: openl_list_projects()");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Note: discardChanges and confirm are client-side safety checks handled by client.updateProjectStatus
      // They are not part of the Java API but are used for safety
      const result = await client.updateProjectStatus(typedArgs.projectId, {
        status: typedArgs.status,
        comment: typedArgs.comment,
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

  // TEMPORARILY DISABLED - openl_upload_file
  // Tool is not working correctly and needs implementation fixes
  /*
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
  */

  // TEMPORARILY DISABLED - openl_download_file
  // Tool is not working correctly and needs implementation fixes
  /*
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
  */

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
        kind?: string[];
        name?: string;
        properties?: Record<string, string>;
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
      if (typedArgs.kind && typedArgs.kind.length > 0) {
        filters.kind = typedArgs.kind;
      }
      if (typedArgs.name) filters.name = typedArgs.name;
      if (typedArgs.properties) filters.properties = typedArgs.properties;

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
        appendData: {
          tableType: string;
          fields?: Array<{ name: string; type: string; required?: boolean; defaultValue?: any }>;
          rules?: Array<Record<string, any>>;
          steps?: Array<any>;
          values?: Array<any>;
        };
        comment?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.tableId || !typedArgs.appendData) {
        throw new McpError(ErrorCode.InvalidParams, "Missing required arguments: projectId, tableId, appendData");
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Convert to AppendTableView format expected by client
      const appendData: Types.AppendTableView = typedArgs.appendData as any;

      await client.appendProjectTable(typedArgs.projectId, typedArgs.tableId, appendData);

      // Generate appropriate success message based on table type
      let itemCount = 0;
      let itemType = "items";
      if (typedArgs.appendData.fields) {
        itemCount = typedArgs.appendData.fields.length;
        itemType = "field(s)";
      } else if (typedArgs.appendData.rules) {
        itemCount = typedArgs.appendData.rules.length;
        itemType = "rule(s)";
      } else if (typedArgs.appendData.steps) {
        itemCount = typedArgs.appendData.steps.length;
        itemType = "step(s)";
      } else if (typedArgs.appendData.values) {
        itemCount = typedArgs.appendData.values.length;
        itemType = "value(s)";
      }

      const result = {
        success: true,
        message: `Successfully appended ${itemCount} ${itemType} to table ${typedArgs.tableId}`,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // TEMPORARILY DISABLED - openl_create_rule
  // Tool is not working correctly and needs implementation fixes
  /*
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
  */

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

      const format = validateResponseFormat(typedArgs && typedArgs.response_format);
      const { limit, offset } = validatePagination(typedArgs && typedArgs.limit, typedArgs && typedArgs.offset);

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
      "Deploy a project to production environment. Publishes rules to a deployment repository for runtime execution. Use production repository name (not ID) - e.g., 'Production Deployment' instead of 'production-deploy'.",
    inputSchema: zodToJsonSchema(schemas.deployProjectSchema) as Record<string, unknown>,
    annotations: {
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        deploymentName: string;
        productionRepositoryId: string;
        comment?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.deploymentName || !typedArgs.productionRepositoryId) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required arguments: projectId, deploymentName, productionRepositoryId"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Convert production repository name to ID for API call
      const productionRepositoryId = await client.getProductionRepositoryIdByName(typedArgs.productionRepositoryId);

      await client.deployProject({
        projectId: typedArgs.projectId,
        deploymentName: typedArgs.deploymentName,
        productionRepositoryId: productionRepositoryId,
        comment: typedArgs.comment,
      });

      const result = {
        success: true,
        message: `Successfully deployed ${typedArgs.deploymentName} to ${typedArgs.productionRepositoryId}`,
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

  // TEMPORARILY DISABLED - openl_execute_rule
  // Tool is not working correctly and needs implementation fixes
  /*
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
  */

  // =============================================================================
  // Version Control Tools
  // =============================================================================

  // TEMPORARILY DISABLED - openl_revert_version
  // Tool is not working correctly and needs implementation fixes
  /*
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
  */

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

  // =============================================================================
  // Repository Features & Revisions Tools
  // =============================================================================

  registerTool({
    name: "openl_list_repository_features",
    title: "openl List Design Repository Features",
    version: "1.0.0",
    description:
      "Get features supported by a design repository (branching, searchable, etc.). Use this to check if a repository supports specific features like branching before performing operations that depend on those features. Use repository name (not ID) - e.g., 'Design Repository' instead of 'design-repo'.",
    inputSchema: zodToJsonSchema(schemas.getRepositoryFeaturesSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        repository: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.repository) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required argument: repository. To find valid repositories, use: openl_list_repositories()"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Convert repository name to ID for API call
      const repositoryId = await client.getRepositoryIdByName(typedArgs.repository);
      const features = await client.getRepositoryFeatures(repositoryId);

      const formattedResult = formatResponse(features, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_repository_project_revisions",
    title: "Openl List Design Repository Project Revisions",
    version: "1.0.0",
    description:
      "Get revision history (commit history) of a project in a design repository. Returns list of revisions with commit hashes, authors, timestamps, and commit types. Supports pagination and filtering by branch and search term. Use repository name (not ID) - e.g., 'Design Repository' instead of 'design-repo'.",
    inputSchema: zodToJsonSchema(schemas.getProjectRevisionsSchema) as Record<string, unknown>,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        repository: string;
        projectName: string;
        branch?: string;
        search?: string;
        techRevs?: boolean;
        page?: number;
        size?: number;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.repository || !typedArgs.projectName) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required arguments: repository, projectName"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Convert repository name to ID for API call
      const repositoryId = await client.getRepositoryIdByName(typedArgs.repository);
      const revisions = await client.getProjectRevisions(repositoryId, typedArgs.projectName, {
        branch: typedArgs.branch,
        search: typedArgs.search,
        techRevs: typedArgs.techRevs,
        page: typedArgs.page,
        size: typedArgs.size,
      });

      const formattedResult = formatResponse(revisions, format, {
        pagination: {
          limit: revisions.pageSize,
          offset: revisions.pageNumber * revisions.pageSize,
          total: revisions.totalElements || revisions.numberOfElements,
        },
        dataType: "revisions",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_list_deploy_repositories",
    title: "openl List Deploy Repositories",
    version: "1.0.0",
    description:
      "List all deployment repositories in OpenL Tablets. Returns repository names, their types, and status information. Use this to discover all available deployment repositories before deploying projects.",
    inputSchema: zodToJsonSchema(schemas.listDeployRepositoriesSchema) as Record<string, unknown>,
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

      const format = validateResponseFormat(typedArgs && typedArgs.response_format);
      const { limit, offset } = validatePagination(typedArgs && typedArgs.limit, typedArgs && typedArgs.offset);

      const repositories = await client.listDeployRepositories();

      // Apply pagination
      const paginated = paginateResults(repositories, limit, offset);

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: {
          limit,
          offset,
          total: paginated.total_count,
        },
        dataType: "deploy_repositories",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Branch Creation Tool
  // =============================================================================

  registerTool({
    name: "openl_create_project_branch",
    title: "openl Create Project Branch",
    version: "1.0.0",
    description:
      "Create a new branch in a project's repository from a specified revision. Allows branching from specific revisions, tags, or other branches. If no revision is specified, the HEAD revision will be used.",
    inputSchema: zodToJsonSchema(schemas.createBranchSchema) as Record<string, unknown>,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        branchName: string;
        revision?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.branchName) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required arguments: projectId, branchName"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      await client.createBranch(typedArgs.projectId, typedArgs.branchName, typedArgs.revision);

      const result = {
        success: true,
        message: `Successfully created branch '${typedArgs.branchName}'${typedArgs.revision ? ` from revision ${typedArgs.revision}` : ""}`,
        branchName: typedArgs.branchName,
        revision: typedArgs.revision,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Local Changes & Restore Tools
  // =============================================================================

  registerTool({
    name: "openl_list_project_local_changes",
    title: "openl List Project Local Changes",
    version: "1.0.0",
    description:
      "List local change history for a project. Returns list of workspace history items with versions, authors, timestamps, and comments. Use this to see all local changes before restoring a previous version.",
    inputSchema: zodToJsonSchema(schemas.listProjectLocalChangesSchema) as Record<string, unknown>,
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
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required argument: projectId. To find valid project IDs, use: openl_list_projects()"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Note: This endpoint requires project to be loaded in WebStudio session
      // The projectId is used for context but the endpoint uses session state
      const changes = await client.getProjectLocalChanges(typedArgs.projectId);

      const formattedResult = formatResponse(changes, format, {
        dataType: "local_changes",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "openl_restore_project_local_change",
    title: "openl Restore Project Local Change",
    version: "1.0.0",
    description:
      "Restore a project to a specified version from its local history. Use the historyId from list_project_local_changes response. This restores the workspace state to a previous local change.",
    inputSchema: zodToJsonSchema(schemas.restoreProjectLocalChangeSchema) as Record<string, unknown>,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        historyId: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId || !typedArgs.historyId) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required arguments: projectId, historyId. Use openl_list_project_local_changes() to find valid history IDs."
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      // Note: This endpoint requires project to be loaded in WebStudio session
      await client.restoreProjectLocalChange(typedArgs.projectId, typedArgs.historyId);

      const result = {
        success: true,
        message: `Successfully restored project to history version '${typedArgs.historyId}'`,
        historyId: typedArgs.historyId,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Test Execution Tools
  // =============================================================================

  registerTool({
    name: "openl_run_project_tests",
    title: "openl Run Project Tests",
    version: "2.0.0",
    description:
      "Run project tests - unified tool that starts test execution and retrieves results. Automatically uses all headers from the test start response when fetching results. Supports options to target specific tables, test ranges, filtering failures, pagination, and waiting for completion.",
    inputSchema: zodToJsonSchema(schemas.runProjectTestsSchema) as Record<string, unknown>,
    annotations: {
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        projectId: string;
        tableId?: string;
        testRanges?: string;
        failuresOnly?: boolean;
        limit?: number;
        offset?: number;
        waitForCompletion?: boolean;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.projectId) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required argument: projectId. To find valid project IDs, use: openl_list_projects()"
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      const summary = await client.runProjectTests(typedArgs.projectId, {
        tableId: typedArgs.tableId,
        testRanges: typedArgs.testRanges,
        query: typedArgs.failuresOnly ? { failuresOnly: true } : undefined,
        pagination: typedArgs.limit || typedArgs.offset
          ? {
              limit: typedArgs.limit,
              offset: typedArgs.offset,
            }
          : undefined,
        waitForCompletion: typedArgs.waitForCompletion !== false, // Default to true
      });

      const formattedResult = formatResponse(summary, format, {
        pagination: {
          limit: summary.pageSize || typedArgs.limit || 50,
          offset: (summary.pageNumber || 0) * (summary.pageSize || 50),
          total: summary.totalElements || summary.numberOfTests,
        },
        dataType: "test_results",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  // =============================================================================
  // Redeploy Tool
  // =============================================================================

  registerTool({
    name: "openl_redeploy_project",
    title: "openl Redeploy Project",
    version: "1.0.0",
    description:
      "Redeploy an existing deployment with a new project version. Use this to update a deployment with a newer version of the project or rollback to a previous version.",
    inputSchema: zodToJsonSchema(schemas.redeployProjectSchema) as Record<string, unknown>,
    annotations: {
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args as {
        deploymentId: string;
        projectId: string;
        comment?: string;
        response_format?: "json" | "markdown";
      };

      if (!typedArgs || !typedArgs.deploymentId || !typedArgs.projectId) {
        throw new McpError(
          ErrorCode.InvalidParams,
          "Missing required arguments: deploymentId, projectId. Use openl_list_deployments() to find valid deployment IDs."
        );
      }

      const format = validateResponseFormat(typedArgs.response_format);

      await client.redeployProject(typedArgs.deploymentId, {
        projectId: typedArgs.projectId,
        comment: typedArgs.comment,
      });

      const result = {
        success: true,
        message: `Successfully redeployed ${typedArgs.deploymentId} with project ${typedArgs.projectId}`,
        deploymentId: typedArgs.deploymentId,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

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
    const status = error.response && error.response.status;
    const sanitizedMessage = sanitizeError(error);
    const endpoint = error.config && error.config.url;
    const method = error.config && error.config.method ? error.config.method.toUpperCase() : undefined;

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
