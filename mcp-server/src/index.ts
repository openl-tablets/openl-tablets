#!/usr/bin/env node

/**
 * OpenL Tablets MCP Server
 *
 * Model Context Protocol server for OpenL Tablets Rules Management System.
 * Provides tools and resources for managing rules projects, tables, and deployments.
 *
 * Features:
 * - Multiple authentication methods (Basic Auth, API Key, OAuth 2.1)
 * - Type-safe input validation with Zod
 * - Automatic token management
 * - Request tracking with Client Document ID
 * - Comprehensive error handling
 *
 * @see https://github.com/openl-tablets/openl-tablets
 * @see https://modelcontextprotocol.io/
 */

import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import {
  CallToolRequestSchema,
  ListResourcesRequestSchema,
  ListToolsRequestSchema,
  ReadResourceRequestSchema,
  ErrorCode,
  McpError,
} from "@modelcontextprotocol/sdk/types.js";

// Import our modular components
import { OpenLClient } from "./client.js";
import { TOOLS } from "./tools.js";
import { SERVER_INFO } from "./constants.js";
import { isAxiosError, sanitizeError, safeStringify } from "./utils.js";
import type * as Types from "./types.js";

/**
 * MCP Server for OpenL Tablets
 *
 * Handles MCP protocol communication and routes requests to the OpenL client.
 */
class OpenLMCPServer {
  private server: Server;
  private client: OpenLClient;

  /**
   * Create a new MCP server instance
   *
   * @param config - OpenL Tablets configuration
   */
  constructor(config: Types.OpenLConfig) {
    // Initialize OpenL API client
    this.client = new OpenLClient(config);

    // Initialize MCP server
    this.server = new Server(
      {
        name: SERVER_INFO.NAME,
        version: SERVER_INFO.VERSION,
      },
      {
        capabilities: {
          tools: {},
          resources: {},
        },
      }
    );

    this.setupHandlers();
  }

  /**
   * Setup MCP request handlers
   */
  private setupHandlers(): void {
    // List available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => ({
      tools: TOOLS,
    }));

    // Handle tool execution
    this.server.setRequestHandler(CallToolRequestSchema, async (request) =>
      this.handleToolCall(request.params.name, request.params.arguments)
    );

    // List available resources
    this.server.setRequestHandler(ListResourcesRequestSchema, async () => ({
      resources: [
        {
          uri: "openl://repositories",
          name: "OpenL Repositories",
          description: "All design repositories in OpenL Tablets",
          mimeType: "application/json",
        },
        {
          uri: "openl://projects",
          name: "OpenL Projects",
          description: "All projects across repositories",
          mimeType: "application/json",
        },
        {
          uri: "openl://deployments",
          name: "OpenL Deployments",
          description: "All deployment repositories and deployed projects",
          mimeType: "application/json",
        },
      ],
    }));

    // Handle resource reads
    this.server.setRequestHandler(ReadResourceRequestSchema, async (request) =>
      this.handleResourceRead(request.params.uri)
    );
  }

  /**
   * Handle tool execution requests
   *
   * @param name - Tool name
   * @param args - Tool arguments (validated by Zod schemas)
   * @returns Tool execution result
   */
  private async handleToolCall(
    name: string,
    args: unknown
  ): Promise<{ content: Array<{ type: string; text: string }> }> {
    try {
      let result: unknown;

      // Route tool calls to appropriate client methods
      switch (name) {
        // System tools
        case "health_check": {
          result = await this.client.healthCheck();
          break;
        }

        // Repository tools
        case "list_repositories": {
          result = await this.client.listRepositories();
          break;
        }

        case "list_branches": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { repository } = args as { repository: string };
          result = await this.client.listBranches(repository);
          break;
        }

        // Project tools
        case "list_projects": {
          const filters = args as Types.ProjectFilters | undefined;
          result = await this.client.listProjects(filters);
          break;
        }

        case "get_project": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId } = args as { projectId: string };
          result = await this.client.getProject(projectId);
          break;
        }

        case "open_project": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId } = args as { projectId: string };
          result = await this.client.openProject(projectId);
          break;
        }

        case "close_project": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId } = args as { projectId: string };
          result = await this.client.closeProject(projectId);
          break;
        }

        case "save_project": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, comment } = args as {
            projectId: string;
            comment?: string;
          };
          result = await this.client.saveProject(projectId, comment);
          break;
        }

        // File management tools
        case "upload_file": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, fileName, fileContent, comment } = args as {
            projectId: string;
            fileName: string;
            fileContent: string;
            comment?: string;
          };
          // Decode base64 file content
          const buffer = Buffer.from(fileContent, "base64");
          result = await this.client.uploadFile(projectId, fileName, buffer, comment);
          break;
        }

        case "download_file": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, fileName } = args as {
            projectId: string;
            fileName: string;
          };
          const fileBuffer = await this.client.downloadFile(projectId, fileName);
          // Return base64-encoded content
          result = {
            fileName,
            fileContent: fileBuffer.toString("base64"),
            size: fileBuffer.length,
          };
          break;
        }

        case "get_project_history": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId } = args as { projectId: string };
          result = await this.client.getProjectHistory(projectId);
          break;
        }

        case "create_branch": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, branchName, comment } = args as {
            projectId: string;
            branchName: string;
            comment?: string;
          };
          result = await this.client.createBranch(projectId, branchName, comment);
          break;
        }

        // Rules (Tables) tools
        case "list_tables": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, tableType, name, file } = args as {
            projectId: string;
            tableType?: string;
            name?: string;
            file?: string;
          };
          const filters: Types.TableFilters = {};
          if (tableType) filters.tableType = tableType as Types.TableType;
          if (name) filters.name = name;
          if (file) filters.file = file;
          result = await this.client.listTables(projectId, filters);
          break;
        }

        case "get_table": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, tableId } = args as {
            projectId: string;
            tableId: string;
          };
          result = await this.client.getTable(projectId, tableId);
          break;
        }

        case "update_table": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, tableId, view, comment } = args as {
            projectId: string;
            tableId: string;
            view: Types.EditableTableView;
            comment?: string;
          };
          result = await this.client.updateTable(projectId, tableId, view, comment);
          break;
        }

        case "create_rule": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, name, ruleType, file, comment } = args as {
            projectId: string;
            name: string;
            ruleType: Types.TableType;
            file?: string;
            comment?: string;
          };
          result = await this.client.createRule(projectId, {
            name,
            ruleType,
            file,
            comment,
          });
          break;
        }

        // Deployment tools
        case "list_deployments": {
          result = await this.client.listDeployments();
          break;
        }

        case "deploy_project": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectName, repository, deploymentRepository, version } = args as {
            projectName: string;
            repository: string;
            deploymentRepository: string;
            version?: string;
          };
          result = await this.client.deployProject(
            projectName,
            repository,
            deploymentRepository,
            version
          );
          break;
        }

        // Testing & Validation tools
        case "run_all_tests": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId } = args as { projectId: string };
          result = await this.client.runAllTests(projectId);
          break;
        }

        case "validate_project": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId } = args as { projectId: string };
          result = await this.client.validateProject(projectId);
          break;
        }

        case "run_test": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, testIds, tableIds, runAll } = args as {
            projectId: string;
            testIds?: string[];
            tableIds?: string[];
            runAll?: boolean;
          };
          result = await this.client.runTest({
            projectId,
            testIds,
            tableIds,
            runAll,
          });
          break;
        }

        case "get_project_errors": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, includeWarnings } = args as {
            projectId: string;
            includeWarnings?: boolean;
          };
          result = await this.client.getProjectErrors(projectId, includeWarnings);
          break;
        }

        // Phase 3: Versioning & Execution tools
        case "version_file": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, currentFileName, newFileName, comment } = args as {
            projectId: string;
            currentFileName: string;
            newFileName?: string;
            comment?: string;
          };
          result = await this.client.versionFile({
            projectId,
            currentFileName,
            newFileName,
            comment,
          });
          break;
        }

        case "copy_table": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, tableId, newName, targetFile, comment } = args as {
            projectId: string;
            tableId: string;
            newName?: string;
            targetFile?: string;
            comment?: string;
          };
          result = await this.client.copyTable({
            projectId,
            tableId,
            newName,
            targetFile,
            comment,
          });
          break;
        }

        case "execute_rule": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, ruleName, inputData } = args as {
            projectId: string;
            ruleName: string;
            inputData: Record<string, unknown>;
          };
          result = await this.client.executeRule({
            projectId,
            ruleName,
            inputData,
          });
          break;
        }

        case "compare_versions": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId, version1, version2 } = args as {
            projectId: string;
            version1: string;
            version2: string;
          };
          result = await this.client.compareVersions({
            projectId,
            version1,
            version2,
          });
          break;
        }

        default:
          throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${name}`);
      }

      // Return formatted result
      return {
        content: [
          {
            type: "text",
            text: safeStringify(result, 2),
          },
        ],
      };
    } catch (error: unknown) {
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
          tool: name,
        };

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
      throw new McpError(
        ErrorCode.InternalError,
        `Error executing ${name}: ${sanitizedMessage}`,
        { tool: name }
      );
    }
  }

  /**
   * Handle resource read requests
   *
   * @param uri - Resource URI
   * @returns Resource content
   */
  private async handleResourceRead(
    uri: string
  ): Promise<{ contents: Array<{ uri: string; mimeType: string; text: string }> }> {
    try {
      let data: unknown;

      switch (uri) {
        case "openl://repositories": {
          data = await this.client.listRepositories();
          break;
        }

        case "openl://projects": {
          data = await this.client.listProjects();
          break;
        }

        case "openl://deployments": {
          data = await this.client.listDeployments();
          break;
        }

        default:
          throw new McpError(ErrorCode.InvalidRequest, `Unknown resource: ${uri}`);
      }

      return {
        contents: [
          {
            uri,
            mimeType: "application/json",
            text: safeStringify(data, 2),
          },
        ],
      };
    } catch (error: unknown) {
      if (error instanceof McpError) {
        throw error;
      }

      const sanitizedMessage = sanitizeError(error);
      throw new McpError(
        ErrorCode.InternalError,
        `Error reading resource ${uri}: ${sanitizedMessage}`
      );
    }
  }

  /**
   * Start the MCP server
   */
  async start(): Promise<void> {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
  }
}

/**
 * Load configuration from environment variables
 *
 * @returns OpenL Tablets configuration
 * @throws Error if required configuration is missing or invalid
 */
function loadConfigFromEnv(): Types.OpenLConfig {
  const baseUrl = process.env.OPENL_BASE_URL;
  if (!baseUrl) {
    throw new Error("OPENL_BASE_URL environment variable is required");
  }

  // Validate base URL format
  try {
    new URL(baseUrl);
  } catch {
    throw new Error(`Invalid OPENL_BASE_URL format: ${baseUrl}`);
  }

  // Parse and validate timeout
  let timeout: number | undefined;
  if (process.env.OPENL_TIMEOUT) {
    const parsedTimeout = parseInt(process.env.OPENL_TIMEOUT, 10);
    if (isNaN(parsedTimeout) || parsedTimeout <= 0) {
      throw new Error(`Invalid OPENL_TIMEOUT value: ${process.env.OPENL_TIMEOUT}`);
    }
    timeout = parsedTimeout;
  }

  const config: Types.OpenLConfig = {
    baseUrl,
    username: process.env.OPENL_USERNAME,
    password: process.env.OPENL_PASSWORD,
    apiKey: process.env.OPENL_API_KEY,
    clientDocumentId: process.env.OPENL_CLIENT_DOCUMENT_ID,
    timeout,
  };

  // OAuth 2.1 configuration
  if (process.env.OPENL_OAUTH2_CLIENT_ID) {
    const clientSecret = process.env.OPENL_OAUTH2_CLIENT_SECRET;
    const tokenUrl = process.env.OPENL_OAUTH2_TOKEN_URL;

    if (!clientSecret) {
      throw new Error("OPENL_OAUTH2_CLIENT_SECRET is required when using OAuth 2.1");
    }

    if (!tokenUrl) {
      throw new Error("OPENL_OAUTH2_TOKEN_URL is required when using OAuth 2.1");
    }

    // Validate token URL format
    try {
      new URL(tokenUrl);
    } catch {
      throw new Error(`Invalid OPENL_OAUTH2_TOKEN_URL format: ${tokenUrl}`);
    }

    const grantType = process.env.OPENL_OAUTH2_GRANT_TYPE;
    const validGrantTypes = ["client_credentials", "authorization_code", "refresh_token"];

    config.oauth2 = {
      clientId: process.env.OPENL_OAUTH2_CLIENT_ID,
      clientSecret,
      tokenUrl,
      authorizationUrl: process.env.OPENL_OAUTH2_AUTHORIZATION_URL,
      scope: process.env.OPENL_OAUTH2_SCOPE,
      grantType:
        grantType && validGrantTypes.includes(grantType)
          ? (grantType as "client_credentials" | "authorization_code" | "refresh_token")
          : "client_credentials",
      refreshToken: process.env.OPENL_OAUTH2_REFRESH_TOKEN,
    };
  }

  // Validate at least one authentication method is configured
  if (!config.username && !config.apiKey && !config.oauth2) {
    throw new Error(
      "At least one authentication method must be configured " +
        "(username/password, API key, or OAuth 2.1)"
    );
  }

  return config;
}

/**
 * Main entry point
 */
async function main(): Promise<void> {
  try {
    const config = loadConfigFromEnv();
    const server = new OpenLMCPServer(config);
    await server.start();
  } catch (error: unknown) {
    const sanitizedMessage = sanitizeError(error);
    console.error("Failed to start OpenL Tablets MCP server:", sanitizedMessage);
    process.exit(1);
  }
}

// Start the server
main();
