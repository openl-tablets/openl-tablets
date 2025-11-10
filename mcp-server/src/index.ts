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
import axios from "axios";

// Import our modular components
import { OpenLClient } from "./client.js";
import { TOOLS } from "./tools.js";
import { SERVER_INFO } from "./constants.js";
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
   * @param args - Tool arguments
   * @returns Tool execution result
   */
  private async handleToolCall(
    name: string,
    args: any
  ): Promise<{ content: Array<{ type: string; text: string }> }> {
    try {
      let result: any;

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

        case "get_project_info": {
          if (!args) throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
          const { projectId } = args as { projectId: string };
          result = await this.client.getProjectInfo(projectId);
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
          const { projectId } = args as { projectId: string };
          result = await this.client.listTables(projectId);
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

        default:
          throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${name}`);
      }

      // Return formatted result
      return {
        content: [
          {
            type: "text",
            text: JSON.stringify(result, null, 2),
          },
        ],
      };
    } catch (error: any) {
      // Enhanced error handling with context
      if (axios.isAxiosError(error)) {
        const status = error.response?.status;
        const message = error.response?.data?.message || error.message;
        const endpoint = error.config?.url;
        const method = error.config?.method?.toUpperCase();

        const errorDetails = {
          status,
          message,
          endpoint,
          method,
          tool: name,
        };

        throw new McpError(
          ErrorCode.InternalError,
          `OpenL Tablets API error (${status}): ${message} [${method} ${endpoint}]`,
          errorDetails
        );
      }

      // Re-throw McpErrors as-is
      if (error instanceof McpError) {
        throw error;
      }

      // Wrap other errors
      throw new McpError(
        ErrorCode.InternalError,
        `Error executing ${name}: ${error.message || "Unknown error"}`,
        { tool: name, error: error.toString() }
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
      let data: any;

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
            text: JSON.stringify(data, null, 2),
          },
        ],
      };
    } catch (error: any) {
      if (error instanceof McpError) {
        throw error;
      }

      throw new McpError(
        ErrorCode.InternalError,
        `Error reading resource ${uri}: ${error.message || "Unknown error"}`
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
 */
function loadConfigFromEnv(): Types.OpenLConfig {
  const baseUrl = process.env.OPENL_BASE_URL;
  if (!baseUrl) {
    throw new Error("OPENL_BASE_URL environment variable is required");
  }

  const config: Types.OpenLConfig = {
    baseUrl,
    username: process.env.OPENL_USERNAME,
    password: process.env.OPENL_PASSWORD,
    apiKey: process.env.OPENL_API_KEY,
    clientDocumentId: process.env.OPENL_CLIENT_DOCUMENT_ID,
    timeout: process.env.OPENL_TIMEOUT
      ? parseInt(process.env.OPENL_TIMEOUT, 10)
      : undefined,
  };

  // OAuth 2.1 configuration
  if (process.env.OPENL_OAUTH2_CLIENT_ID) {
    config.oauth2 = {
      clientId: process.env.OPENL_OAUTH2_CLIENT_ID,
      clientSecret: process.env.OPENL_OAUTH2_CLIENT_SECRET || "",
      tokenUrl: process.env.OPENL_OAUTH2_TOKEN_URL || "",
      authorizationUrl: process.env.OPENL_OAUTH2_AUTHORIZATION_URL,
      scope: process.env.OPENL_OAUTH2_SCOPE,
      grantType: (process.env.OPENL_OAUTH2_GRANT_TYPE as any) || "client_credentials",
      refreshToken: process.env.OPENL_OAUTH2_REFRESH_TOKEN,
    };
  }

  return config;
}

/**
 * Main entry point
 */
async function main() {
  try {
    const config = loadConfigFromEnv();
    const server = new OpenLMCPServer(config);
    await server.start();
  } catch (error: any) {
    console.error("Failed to start OpenL Tablets MCP server:", error.message);
    process.exit(1);
  }
}

// Start the server
main();
