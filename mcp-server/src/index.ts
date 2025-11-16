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
  ListPromptsRequestSchema,
  GetPromptRequestSchema,
  ErrorCode,
  McpError,
} from "@modelcontextprotocol/sdk/types.js";

// Import our modular components
import { OpenLClient } from "./client.js";
import { SERVER_INFO } from "./constants.js";
import { PROMPTS, loadPromptContent, getPromptDefinition } from "./prompts-registry.js";
import { registerAllTools, getAllTools, executeTool } from "./tool-handlers.js";
import { parseProjectId, createProjectId, safeStringify, sanitizeError } from "./utils.js";
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
          prompts: {},
        },
      }
    );

    // Initialize all tool handlers
    registerAllTools(this.server, this.client);

    this.setupHandlers();
  }

  /**
   * Setup MCP request handlers
   */
  private setupHandlers(): void {
    // List available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => ({
      tools: getAllTools().map(({ name, title, description, inputSchema, annotations }) => ({
        name,
        description,
        inputSchema,
        ...(annotations && { annotations }),
      })),
    }));

    // Handle tool execution
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      const result = await executeTool(request.params.name, request.params.arguments, this.client);
      return result as any; // Type cast needed due to MCP SDK generic return type
    });

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
          description: "All projects across all repositories",
          mimeType: "application/json",
        },
        {
          uri: "openl://projects/{projectId}",
          name: "OpenL Project Details",
          description: "Get details for a specific project (use repository-projectName format)",
          mimeType: "application/json",
        },
        {
          uri: "openl://projects/{projectId}/tables",
          name: "Project Tables",
          description: "List all tables in a project",
          mimeType: "application/json",
        },
        {
          uri: "openl://projects/{projectId}/tables/{tableId}",
          name: "Table Details",
          description: "Get details for a specific table",
          mimeType: "application/json",
        },
        {
          uri: "openl://projects/{projectId}/history",
          name: "Project History",
          description: "Get Git commit history for a project",
          mimeType: "application/json",
        },
        {
          uri: "openl://projects/{projectId}/files/{filePath}",
          name: "Project File",
          description: "Download a file from a project",
          mimeType: "application/octet-stream",
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

    // List available prompts
    this.server.setRequestHandler(ListPromptsRequestSchema, async () => ({
      prompts: PROMPTS,
    }));

    // Get specific prompt with optional arguments
    this.server.setRequestHandler(GetPromptRequestSchema, async (request) => {
      const { name, arguments: args } = request.params;

      const prompt = getPromptDefinition(name);
      if (!prompt) {
        throw new McpError(
          ErrorCode.InvalidRequest,
          `Prompt not found: ${name}`
        );
      }

      try {
        const content = loadPromptContent(name, args);

        return {
          description: prompt.description,
          messages: [
            {
              role: "assistant" as const,
              content: {
                type: "text" as const,
                text: content,
              },
            },
          ],
        };
      } catch (error) {
        throw new McpError(
          ErrorCode.InternalError,
          `Failed to load prompt: ${error instanceof Error ? error.message : String(error)}`
        );
      }
    });
  }

  /**
   * Note: Tool execution is now handled by the tool-handlers module.
   * The handleToolCall method has been removed and replaced with the
   * registerAllTools/executeTool pattern for better modularity.
   */

  /**
   * REMOVED: The entire handleToolCall method with switch statement
   * has been replaced by the tool-handlers.ts module.
   * See registerAllTools() and executeTool() functions.
   */

  /*
  REMOVED METHOD - The switch statement handleToolCall has been completely removed.
  All tool handling is now done through tool-handlers.ts
  */

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
      let mimeType = "application/json";

      // Parse URI and extract parameters
      const uriMatch = uri.match(/^openl:\/\/([^\/]+)(?:\/(.+))?$/);
      if (!uriMatch) {
        throw new McpError(ErrorCode.InvalidRequest, `Invalid resource URI: ${uri}`);
      }

      const [, resourceType, path] = uriMatch;

      switch (resourceType) {
        case "repositories": {
          data = await this.client.listRepositories();
          break;
        }

        case "projects": {
          if (!path) {
            // openl://projects - List all projects
            data = await this.client.listProjects();
          } else {
            // Parse projects/{projectId} or projects/{projectId}/...
            const projectMatch = path.match(/^([^\/]+)(?:\/(.+))?$/);
            if (!projectMatch) {
              throw new McpError(ErrorCode.InvalidRequest, `Invalid project URI: ${uri}`);
            }

            const [, projectId, subPath] = projectMatch;

            if (!subPath) {
              // openl://projects/{projectId} - Get project details
              data = await this.client.getProject(projectId);
            } else if (subPath === "history") {
              // openl://projects/{projectId}/history - Get project history
              data = await this.client.getProjectHistory({ projectId });
            } else if (subPath.startsWith("tables")) {
              // Parse tables or tables/{tableId}
              const tableMatch = subPath.match(/^tables(?:\/(.+))?$/);
              if (!tableMatch) {
                throw new McpError(ErrorCode.InvalidRequest, `Invalid tables URI: ${uri}`);
              }

              const [, tableId] = tableMatch;

              if (!tableId) {
                // openl://projects/{projectId}/tables - List tables
                data = await this.client.listTables(projectId);
              } else {
                // openl://projects/{projectId}/tables/{tableId} - Get table
                data = await this.client.getTable(projectId, tableId);
              }
            } else if (subPath.startsWith("files/")) {
              // openl://projects/{projectId}/files/{filePath} - Download file
              const filePath = subPath.substring(6); // Remove "files/" prefix
              if (!filePath) {
                throw new McpError(ErrorCode.InvalidRequest, `File path is required: ${uri}`);
              }

              const fileBuffer = await this.client.downloadFile(projectId, filePath);
              mimeType = "application/octet-stream";

              // Return base64-encoded file content
              return {
                contents: [
                  {
                    uri,
                    mimeType,
                    text: fileBuffer.toString("base64"),
                  },
                ],
              };
            } else {
              throw new McpError(ErrorCode.InvalidRequest, `Unknown project subresource: ${subPath}`);
            }
          }
          break;
        }

        case "deployments": {
          data = await this.client.listDeployments();
          break;
        }

        default:
          throw new McpError(ErrorCode.InvalidRequest, `Unknown resource type: ${resourceType}`);
      }

      return {
        contents: [
          {
            uri,
            mimeType,
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
