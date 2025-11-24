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
import { parseProjectId, createProjectId, safeStringify, sanitizeError, generateCodeChallengeSync, generateAuthorizationUrl, openBrowser, generateCodeVerifier, isLocalhostRedirect } from "./utils.js";
import type * as Types from "./types.js";
import axios from "axios";

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
          description: "Get details for a specific project (use base64-encoded projectId format from openl_list_projects)",
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
export async function loadConfigFromEnv(): Promise<Types.OpenLConfig> {
  console.error(`[Config] Loading configuration from environment variables...`);
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

  // Log authentication configuration (without sensitive data)
  console.error(`[Config] Authentication methods:`);
  console.error(`[Config]   - OAuth2: ${!!process.env.OPENL_OAUTH2_CLIENT_ID ? 'configured' : 'not configured'}`);
  console.error(`[Config]   - API Key: ${!!config.apiKey ? 'configured' : 'not configured'}`);
  console.error(`[Config]   - Basic Auth: ${!!config.username && !!config.password ? `configured (username: ${config.username})` : 'not configured'}`);
  if (!config.username) {
    console.error(`[Config]   ⚠️  OPENL_USERNAME is not set`);
  }
  if (!config.password) {
    console.error(`[Config]   ⚠️  OPENL_PASSWORD is not set`);
  }

  // OAuth 2.1 configuration
  if (process.env.OPENL_OAUTH2_CLIENT_ID) {
    console.error(`[OAuth2] 🔐 OAuth2 configuration detected`);
    const clientSecret = process.env.OPENL_OAUTH2_CLIENT_SECRET;
    const issuerUri = process.env.OPENL_OAUTH2_ISSUER_URI;
    let tokenUrl = process.env.OPENL_OAUTH2_TOKEN_URL;
    const grantType = process.env.OPENL_OAUTH2_GRANT_TYPE || "client_credentials";
    const codeVerifier = process.env.OPENL_OAUTH2_CODE_VERIFIER;
    const authorizationCode = process.env.OPENL_OAUTH2_AUTHORIZATION_CODE;
    const redirectUriEnv = process.env.OPENL_OAUTH2_REDIRECT_URI;
    
    console.error(`[OAuth2]   Issuer URI: ${issuerUri || 'not set'}`);
    console.error(`[OAuth2]   Token URL: ${tokenUrl || 'not set (will be discovered)'}`);
    console.error(`[OAuth2]   Grant Type: ${grantType}`);

    // For PKCE flow (authorization_code), client_secret is optional
    // For other flows, client_secret is required
    if (!clientSecret && grantType !== "authorization_code") {
      throw new Error("OPENL_OAUTH2_CLIENT_SECRET is required when using OAuth 2.1 (except for PKCE authorization_code flow)");
    }

    // For authorization_code grant type, authorizationCode is always required
    if (grantType === "authorization_code") {
      if (!authorizationCode) {
        const authUrl = process.env.OPENL_OAUTH2_AUTHORIZATION_URL;
        const redirectUri = redirectUriEnv;
        
        if (authUrl && redirectUri) {
          // Generate PKCE parameters if not provided
          let codeChallenge: string | undefined;
          let codeVerifierToUse = codeVerifier;
          
          if (!codeVerifierToUse) {
            codeVerifierToUse = generateCodeVerifier();
            codeChallenge = generateCodeChallengeSync(codeVerifierToUse);
            console.error(`[OAuth2] 🔐 Generated code_verifier for PKCE: ${codeVerifierToUse.substring(0, 20)}...`);
            console.error(`[OAuth2] 💾 Save this code_verifier: ${codeVerifierToUse}`);
          } else {
            codeChallenge = generateCodeChallengeSync(codeVerifierToUse);
          }

          const authorizationUrl = generateAuthorizationUrl({
            authorizationUrl: authUrl,
            clientId: process.env.OPENL_OAUTH2_CLIENT_ID || "",
            scope: process.env.OPENL_OAUTH2_SCOPE,
            redirectUri,
            codeChallenge,
          });

          // Check if redirect URI is localhost (can use automatic interception)
          if (isLocalhostRedirect(redirectUri)) {
            console.error(`[OAuth2] ℹ️  Authorization code not set. Will use automatic browser flow on first API request.`);
            console.error(`[OAuth2]    Browser will open automatically when authorization is needed.`);
            console.error(`[OAuth2]    Redirect URI: ${redirectUri} (localhost - automatic flow supported)`);
            // Don't throw error - let the automatic flow handle it when token is requested
          } else {
            // Redirect URI is not localhost - cannot use automatic interception
            console.error(`[OAuth2] ⚠️  Authorization code not set and redirect URI is not localhost.`);
            console.error(`[OAuth2]    Redirect URI: ${redirectUri}`);
            console.error(`[OAuth2]    Automatic browser flow requires a localhost redirect URI.`);
            console.error(`[OAuth2]    Please set OPENL_OAUTH2_AUTHORIZATION_CODE manually or use a localhost redirect URI.`);
            console.error(`[OAuth2] 📋 Authorization URL for manual use:`);
            console.error(`[OAuth2] ${authorizationUrl}`);
            // Don't throw error - let the user manually set the code or use localhost redirect
          }
        } else {
          throw new Error(
            "OPENL_OAUTH2_AUTHORIZATION_CODE is required for authorization_code grant type. " +
            "Set OPENL_OAUTH2_AUTHORIZATION_CODE or configure OPENL_OAUTH2_AUTHORIZATION_URL and OPENL_OAUTH2_REDIRECT_URI for automatic browser flow."
          );
        }
      }
      // If codeVerifier is provided (PKCE flow), ensure it's valid
      if (codeVerifier) {
        if (codeVerifier.length < 43) {
          throw new Error("OPENL_OAUTH2_CODE_VERIFIER must be at least 43 characters long (PKCE requirement)");
        }
        if (codeVerifier.length > 128) {
          throw new Error("OPENL_OAUTH2_CODE_VERIFIER must be at most 128 characters long (PKCE requirement)");
        }
      }
    }

    // If issuer-uri is provided, try to discover token endpoint
    if (issuerUri && !tokenUrl) {
      // Remove trailing slash if present
      const baseUri = issuerUri.replace(/\/$/, "");
      
      // Try to discover token endpoint from well-known configuration
      try {
        const wellKnownUrl = `${baseUri}/.well-known/openid-configuration`;
        console.error(`[OAuth2] Discovering token endpoint from: ${wellKnownUrl}`);
        const response = await axios.get(wellKnownUrl, { timeout: 5000 });
        if (response.data && response.data.token_endpoint) {
          tokenUrl = response.data.token_endpoint;
          console.error(`[OAuth2] ✅ Discovered token endpoint from well-known: ${tokenUrl}`);
        } else {
          console.error(`[OAuth2] ⚠️  Well-known config found but token_endpoint missing`);
        }
      } catch (error) {
        const errorMsg = error instanceof Error ? error.message : String(error);
        console.error(`[OAuth2] ⚠️  Failed to discover token endpoint from well-known: ${errorMsg}, using defaults`);
      }
      
      // Fallback to common paths if discovery failed
      if (!tokenUrl) {
        // Try common token endpoint paths for different OAuth providers
        // Ping Identity typically uses /as/token.oauth2
        // Spring Security OAuth2 uses /oauth/token
        // Standard OAuth2 uses /token
        const commonPaths = [
          "/as/token.oauth2",  // Ping Identity
          "/oauth/token",      // Spring Security OAuth2
          "/token",            // Standard OAuth2
        ];
        
        // For Ping Identity, try /as/token.oauth2 first
        tokenUrl = `${baseUri}/as/token.oauth2`;
        console.error(`[OAuth2] Using issuer-uri: ${issuerUri}, constructed token-url: ${tokenUrl}`);
        console.error(`[OAuth2] ℹ️  If this fails with 404, try setting OPENL_OAUTH2_TOKEN_URL explicitly to one of: ${commonPaths.map(p => `${baseUri}${p}`).join(", ")}`);
      }
    }

    if (!tokenUrl) {
      throw new Error(
        "OPENL_OAUTH2_TOKEN_URL or OPENL_OAUTH2_ISSUER_URI is required when using OAuth 2.1"
      );
    }

    // Validate token URL format
    try {
      new URL(tokenUrl);
    } catch {
      throw new Error(`Invalid OPENL_OAUTH2_TOKEN_URL format: ${tokenUrl}`);
    }

    // Validate issuer URI format if provided
    if (issuerUri) {
      try {
        new URL(issuerUri);
      } catch {
        throw new Error(`Invalid OPENL_OAUTH2_ISSUER_URI format: ${issuerUri}`);
      }
    }

    const validGrantTypes = ["client_credentials", "authorization_code", "refresh_token"];
    const finalGrantType = grantType && validGrantTypes.includes(grantType)
      ? (grantType as "client_credentials" | "authorization_code" | "refresh_token")
      : "client_credentials";

    // Check if Basic Auth should be used (common for Ping Identity)
    const useBasicAuth = process.env.OPENL_OAUTH2_USE_BASIC_AUTH === "true" || 
                         process.env.OPENL_OAUTH2_USE_BASIC_AUTH === "1";

    // Generate code_challenge from code_verifier if provided
    let codeChallenge: string | undefined;
    let codeChallengeMethod: "S256" | "plain" | undefined;
    if (codeVerifier) {
      codeChallenge = generateCodeChallengeSync(codeVerifier);
      codeChallengeMethod = "S256";
      console.error(`[OAuth2] 🔐 PKCE: Generated code_challenge from code_verifier`);
    }

    config.oauth2 = {
      clientId: process.env.OPENL_OAUTH2_CLIENT_ID,
      clientSecret: clientSecret || undefined, // Optional for PKCE
      tokenUrl,
      authorizationUrl: process.env.OPENL_OAUTH2_AUTHORIZATION_URL,
      scope: process.env.OPENL_OAUTH2_SCOPE,
      grantType: finalGrantType,
      refreshToken: process.env.OPENL_OAUTH2_REFRESH_TOKEN,
      useBasicAuth,
      audience: process.env.OPENL_OAUTH2_AUDIENCE,
      resource: process.env.OPENL_OAUTH2_RESOURCE,
      // PKCE parameters
      codeVerifier: codeVerifier || undefined,
      codeChallenge: codeChallenge || process.env.OPENL_OAUTH2_CODE_CHALLENGE || undefined,
      codeChallengeMethod: codeChallengeMethod || (process.env.OPENL_OAUTH2_CODE_CHALLENGE_METHOD as "S256" | "plain") || undefined,
      authorizationCode: authorizationCode || undefined,
      redirectUri: redirectUriEnv || undefined,
    };
    
    console.error(`[OAuth2] ✅ Configuration loaded:`);
    console.error(`[OAuth2]   - Client ID: ${config.oauth2.clientId}`);
    console.error(`[OAuth2]   - Grant Type: ${config.oauth2.grantType}`);
    console.error(`[OAuth2]   - Token URL: ${config.oauth2.tokenUrl}`);
    console.error(`[OAuth2]   - Issuer URI: ${issuerUri || 'not set'}`);
    console.error(`[OAuth2]   - Use Basic Auth: ${config.oauth2.useBasicAuth}`);
    if (config.oauth2.codeVerifier) {
      console.error(`[OAuth2]   - PKCE: code_verifier present (code_challenge: ${config.oauth2.codeChallenge ? 'generated' : 'not set'})`);
    }
    if (config.oauth2.refreshToken) {
      console.error(`[OAuth2]   - Refresh Token: ***${config.oauth2.refreshToken.slice(-10)}`);
    }
    if (config.oauth2.authorizationCode) {
      console.error(`[OAuth2]   - Authorization Code: ***${config.oauth2.authorizationCode.slice(-10)}`);
    }
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
    const config = await loadConfigFromEnv();
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
