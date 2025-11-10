#!/usr/bin/env node

/**
 * OpenL Tablets MCP Server
 *
 * Provides Model Context Protocol interface for OpenL Tablets Rules Management System.
 * Exposes tools and resources for managing rules projects, tables, and deployments.
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
import axios, { AxiosInstance, AxiosError } from "axios";
import FormData from "form-data";
import { zodToJsonSchema } from "zod-to-json-schema";
import type * as Types from "./types.js";
import * as schemas from "./schemas.js";

/**
 * OpenL Tablets API Client with OAuth 2.1 and Client Document ID support
 */
class OpenLClient {
  private client: AxiosInstance;
  private baseUrl: string;
  private config: Types.OpenLConfig;
  private oauth2Token?: Types.OAuth2Token;
  private tokenRefreshPromise?: Promise<Types.OAuth2Token>;

  constructor(config: Types.OpenLConfig) {
    this.baseUrl = config.baseUrl.replace(/\/$/, "");
    this.config = config;

    // Setup basic auth if provided
    const auth: any = {};
    if (config.username && config.password) {
      auth.username = config.username;
      auth.password = config.password;
    }

    // Create axios instance
    this.client = axios.create({
      baseURL: this.baseUrl,
      auth: Object.keys(auth).length > 0 ? auth : undefined,
      timeout: config.timeout || 30000,
      headers: {
        "Content-Type": "application/json",
        ...(config.apiKey ? { "X-API-Key": config.apiKey } : {}),
        ...(config.clientDocumentId ? { "X-Client-Document-ID": config.clientDocumentId } : {}),
      },
    });

    // Setup request interceptor for OAuth 2.1
    this.client.interceptors.request.use(
      async (config) => {
        // If OAuth 2.1 is configured, add bearer token
        if (this.config.oauth2) {
          const token = await this.getValidToken();
          if (token) {
            config.headers.Authorization = `Bearer ${token.access_token}`;
          }
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Setup response interceptor to handle token refresh on 401
    this.client.interceptors.response.use(
      (response) => response,
      async (error) => {
        const originalRequest = error.config;

        // If 401 and OAuth is configured, try to refresh token
        if (
          error.response?.status === 401 &&
          this.config.oauth2 &&
          !originalRequest._retry
        ) {
          originalRequest._retry = true;

          try {
            // Refresh the token
            await this.refreshOAuth2Token();
            // Retry the original request
            return this.client(originalRequest);
          } catch (refreshError) {
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  /**
   * Get a valid OAuth 2.1 token, refreshing if necessary
   */
  private async getValidToken(): Promise<Types.OAuth2Token | null> {
    if (!this.config.oauth2) {
      return null;
    }

    // If we have a token and it's still valid, return it
    if (this.oauth2Token && this.isTokenValid(this.oauth2Token)) {
      return this.oauth2Token;
    }

    // If a refresh is already in progress, wait for it
    if (this.tokenRefreshPromise) {
      return this.tokenRefreshPromise;
    }

    // Otherwise, get a new token
    this.tokenRefreshPromise = this.obtainOAuth2Token();
    try {
      this.oauth2Token = await this.tokenRefreshPromise;
      return this.oauth2Token;
    } finally {
      this.tokenRefreshPromise = undefined;
    }
  }

  /**
   * Check if a token is still valid (with 60 second buffer)
   */
  private isTokenValid(token: Types.OAuth2Token): boolean {
    if (!token.expires_at) {
      return true; // No expiration info, assume valid
    }
    const now = Date.now() / 1000;
    const buffer = 60; // 60 second buffer
    return token.expires_at > now + buffer;
  }

  /**
   * Obtain a new OAuth 2.1 token
   */
  private async obtainOAuth2Token(): Promise<Types.OAuth2Token> {
    if (!this.config.oauth2) {
      throw new Error("OAuth 2.1 not configured");
    }

    const { clientId, clientSecret, tokenUrl, scope, grantType } = this.config.oauth2;

    const params = new URLSearchParams();
    params.append("client_id", clientId);
    params.append("client_secret", clientSecret);
    params.append("grant_type", grantType || "client_credentials");

    if (scope) {
      params.append("scope", scope);
    }

    try {
      const response = await axios.post(tokenUrl, params, {
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
        },
      });

      const token: Types.OAuth2Token = response.data;

      // Calculate expiration timestamp if expires_in is provided
      if (token.expires_in) {
        token.expires_at = Date.now() / 1000 + token.expires_in;
      }

      return token;
    } catch (error: any) {
      throw new Error(
        `Failed to obtain OAuth 2.1 token: ${error.response?.data?.error_description || error.message}`
      );
    }
  }

  /**
   * Refresh OAuth 2.1 token
   */
  private async refreshOAuth2Token(): Promise<Types.OAuth2Token> {
    if (!this.config.oauth2) {
      throw new Error("OAuth 2.1 not configured");
    }

    // If we have a refresh token, use it
    if (this.oauth2Token?.refresh_token || this.config.oauth2.refreshToken) {
      const { clientId, clientSecret, tokenUrl } = this.config.oauth2;
      const refreshToken = this.oauth2Token?.refresh_token || this.config.oauth2.refreshToken;

      const params = new URLSearchParams();
      params.append("client_id", clientId);
      params.append("client_secret", clientSecret);
      params.append("grant_type", "refresh_token");
      params.append("refresh_token", refreshToken!);

      try {
        const response = await axios.post(tokenUrl, params, {
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
        });

        const token: Types.OAuth2Token = response.data;

        if (token.expires_in) {
          token.expires_at = Date.now() / 1000 + token.expires_in;
        }

        this.oauth2Token = token;
        return token;
      } catch (error: any) {
        // If refresh fails, try to get a new token
        return this.obtainOAuth2Token();
      }
    }

    // Otherwise, get a new token
    return this.obtainOAuth2Token();
  }

  // Repository Management
  async listRepositories(): Promise<Types.RepositoryInfo[]> {
    const response = await this.client.get("/repos");
    return response.data;
  }

  async getRepositoryFeatures(repoName: string): Promise<any> {
    const response = await this.client.get(`/repos/${repoName}/features`);
    return response.data;
  }

  async listBranches(repoName: string): Promise<string[]> {
    const response = await this.client.get(`/repos/${repoName}/branches`);
    return response.data;
  }

  // Project Management
  async listProjects(filters?: {
    repository?: string;
    status?: string;
    tag?: string;
  }): Promise<Types.ProjectViewModel[]> {
    const params = new URLSearchParams();
    if (filters?.repository) params.append("repository", filters.repository);
    if (filters?.status) params.append("status", filters.status);
    if (filters?.tag) params.append("tag", filters.tag);

    const response = await this.client.get(`/projects${params.toString() ? `?${params}` : ""}`);
    return response.data;
  }

  async getProject(projectId: string): Promise<Types.ProjectViewModel> {
    const response = await this.client.get(`/projects/${projectId}`);
    return response.data;
  }

  async getProjectInfo(repoName: string, projectName: string): Promise<Types.ProjectInfo> {
    const response = await this.client.get(
      `/user-workspace/${repoName}/projects/${projectName}/info`
    );
    return response.data;
  }

  async openProject(projectId: string): Promise<void> {
    await this.client.patch(`/projects/${projectId}`, { status: "OPENED" });
  }

  async closeProject(projectId: string): Promise<void> {
    await this.client.patch(`/projects/${projectId}`, { status: "CLOSED" });
  }

  async createProject(
    repoName: string,
    projectName: string,
    zipBuffer: Buffer,
    comment?: string
  ): Promise<void> {
    const formData = new FormData();
    formData.append("file", zipBuffer, { filename: `${projectName}.zip` });
    if (comment) {
      formData.append("comment", comment);
    }

    await this.client.put(`/repos/${repoName}/projects/${projectName}`, formData, {
      headers: formData.getHeaders(),
    });
  }

  async getProjectHistory(
    repoName: string,
    projectName: string
  ): Promise<Types.ProjectHistoryItem[]> {
    const response = await this.client.get(
      `/repos/${repoName}/projects/${projectName}/history`
    );
    return response.data;
  }

  async createBranch(projectId: string, branchName: string, comment?: string): Promise<void> {
    await this.client.post(`/projects/${projectId}/branches`, {
      name: branchName,
      comment,
    });
  }

  // Table Management
  async listTables(projectId: string): Promise<Types.SummaryTableView[]> {
    const response = await this.client.get(`/projects/${projectId}/tables`);
    return response.data;
  }

  async getTable(projectId: string, tableId: string): Promise<Types.EditableTableView> {
    const response = await this.client.get(`/projects/${projectId}/tables/${tableId}`);
    return response.data;
  }

  async updateTable(
    projectId: string,
    tableId: string,
    view: Types.EditableTableView,
    comment?: string
  ): Promise<void> {
    await this.client.put(`/projects/${projectId}/tables/${tableId}`, {
      view,
      comment,
    });
  }

  async appendTableLines(
    projectId: string,
    tableId: string,
    lines: any[],
    comment?: string
  ): Promise<void> {
    await this.client.post(`/projects/${projectId}/tables/${tableId}/lines`, {
      lines,
      comment,
    });
  }

  // Deployment Management
  async listDeployments(): Promise<Types.DeploymentInfo[]> {
    const response = await this.client.get("/deployments");
    return response.data;
  }

  async deployProject(request: Types.DeployRequest): Promise<void> {
    await this.client.post("/deployments", request);
  }

  async redeployProject(deploymentId: string): Promise<void> {
    await this.client.post(`/deployments/${deploymentId}`);
  }

  async listProductionRepositories(): Promise<Types.RepositoryInfo[]> {
    const response = await this.client.get("/production-repos");
    return response.data;
  }

  // Health Check
  async healthCheck(): Promise<{
    status: string;
    baseUrl: string;
    authMethod: string;
    timestamp: string;
    serverReachable: boolean;
    error?: string;
  }> {
    const authMethod = this.config.oauth2
      ? "OAuth 2.1"
      : this.config.apiKey
      ? "API Key"
      : this.config.username
      ? "Basic Auth"
      : "None";

    try {
      // Try to list repositories as a connectivity test
      await this.listRepositories();
      return {
        status: "healthy",
        baseUrl: this.baseUrl,
        authMethod,
        timestamp: new Date().toISOString(),
        serverReachable: true,
      };
    } catch (error: any) {
      return {
        status: "unhealthy",
        baseUrl: this.baseUrl,
        authMethod,
        timestamp: new Date().toISOString(),
        serverReachable: false,
        error: error.message || "Unknown error",
      };
    }
  }
}

/**
 * MCP Server Implementation
 */
class OpenLMCPServer {
  private server: Server;
  private client: OpenLClient;

  constructor() {
    this.server = new Server(
      {
        name: "openl-tablets-server",
        version: "1.0.0",
      },
      {
        capabilities: {
          resources: {},
          tools: {},
        },
      }
    );

    // Initialize client from environment variables
    const baseUrl = process.env.OPENL_BASE_URL || "http://localhost:8080/webstudio/rest";
    const username = process.env.OPENL_USERNAME;
    const password = process.env.OPENL_PASSWORD;
    const apiKey = process.env.OPENL_API_KEY;
    const clientDocumentId = process.env.OPENL_CLIENT_DOCUMENT_ID;
    const timeout = process.env.OPENL_TIMEOUT ? parseInt(process.env.OPENL_TIMEOUT) : undefined;

    // OAuth 2.1 configuration
    let oauth2: Types.OAuth2Config | undefined;
    if (process.env.OPENL_OAUTH2_CLIENT_ID && process.env.OPENL_OAUTH2_TOKEN_URL) {
      oauth2 = {
        clientId: process.env.OPENL_OAUTH2_CLIENT_ID,
        clientSecret: process.env.OPENL_OAUTH2_CLIENT_SECRET || "",
        tokenUrl: process.env.OPENL_OAUTH2_TOKEN_URL,
        authorizationUrl: process.env.OPENL_OAUTH2_AUTHORIZATION_URL,
        scope: process.env.OPENL_OAUTH2_SCOPE,
        grantType: (process.env.OPENL_OAUTH2_GRANT_TYPE as any) || "client_credentials",
        refreshToken: process.env.OPENL_OAUTH2_REFRESH_TOKEN,
      };
    }

    this.client = new OpenLClient({
      baseUrl,
      username,
      password,
      apiKey,
      oauth2,
      clientDocumentId,
      timeout,
    });

    this.setupHandlers();
    this.setupErrorHandling();
  }

  private setupErrorHandling(): void {
    this.server.onerror = (error) => {
      console.error("[MCP Error]", error);
    };

    process.on("SIGINT", async () => {
      await this.server.close();
      process.exit(0);
    });
  }

  private setupHandlers(): void {
    this.setupResourceHandlers();
    this.setupToolHandlers();
  }

  private setupResourceHandlers(): void {
    // List available resources
    this.server.setRequestHandler(ListResourcesRequestSchema, async () => ({
      resources: [
        {
          uri: "openl://repositories",
          name: "Repositories",
          description: "List of all design repositories",
          mimeType: "application/json",
        },
        {
          uri: "openl://projects",
          name: "Projects",
          description: "List of all projects across repositories",
          mimeType: "application/json",
        },
        {
          uri: "openl://deployments",
          name: "Deployments",
          description: "List of all project deployments",
          mimeType: "application/json",
        },
      ],
    }));

    // Read resource content
    this.server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
      const uri = request.params.uri;

      if (uri === "openl://repositories") {
        const repos = await this.client.listRepositories();
        return {
          contents: [
            {
              uri,
              mimeType: "application/json",
              text: JSON.stringify(repos, null, 2),
            },
          ],
        };
      }

      if (uri === "openl://projects") {
        const projects = await this.client.listProjects();
        return {
          contents: [
            {
              uri,
              mimeType: "application/json",
              text: JSON.stringify(projects, null, 2),
            },
          ],
        };
      }

      if (uri === "openl://deployments") {
        const deployments = await this.client.listDeployments();
        return {
          contents: [
            {
              uri,
              mimeType: "application/json",
              text: JSON.stringify(deployments, null, 2),
            },
          ],
        };
      }

      throw new McpError(ErrorCode.InvalidRequest, `Unknown resource: ${uri}`);
    });
  }

  private setupToolHandlers(): void {
    this.server.setRequestHandler(ListToolsRequestSchema, async () => ({
      tools: [
        {
          name: "health_check",
          description: "Check OpenL Tablets server connectivity and authentication status",
          inputSchema: zodToJsonSchema(schemas.z.object({})) as any,
          _meta: {
            version: "1.0.0",
            category: "system",
            requiresAuth: true,
          },
        },
        {
          name: "list_repositories",
          description: "List all design repositories in OpenL Tablets",
          inputSchema: zodToJsonSchema(schemas.z.object({})) as any,
          _meta: {
            version: "1.0.0",
            category: "repository",
            requiresAuth: true,
          },
        },
        {
          name: "list_projects",
          description: "List projects with optional filters (repository, status, tag)",
          inputSchema: zodToJsonSchema(schemas.listProjectsSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "project",
            requiresAuth: true,
          },
        },
        {
          name: "get_project",
          description: "Get detailed information about a specific project",
          inputSchema: zodToJsonSchema(schemas.getProjectSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "project",
            requiresAuth: true,
          },
        },
        {
          name: "get_project_info",
          description: "Get project info including modules and dependencies",
          inputSchema: zodToJsonSchema(schemas.getProjectInfoSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "project",
            requiresAuth: true,
          },
        },
        {
          name: "open_project",
          description: "Open a project for viewing or editing",
          inputSchema: zodToJsonSchema(schemas.projectActionSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "project",
            requiresAuth: true,
            modifiesState: true,
          },
        },
        {
          name: "close_project",
          description: "Close an open project",
          inputSchema: zodToJsonSchema(schemas.projectActionSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "project",
            requiresAuth: true,
            modifiesState: true,
          },
        },
        {
          name: "list_tables",
          description: "List all tables (rules) in a project",
          inputSchema: zodToJsonSchema(schemas.listTablesSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "rules",
            requiresAuth: true,
          },
        },
        {
          name: "get_table",
          description: "Get detailed table (rule) data including structure and content",
          inputSchema: zodToJsonSchema(schemas.getTableSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "rules",
            requiresAuth: true,
          },
        },
        {
          name: "update_table",
          description: "Update a table (rule) with new data",
          inputSchema: zodToJsonSchema(schemas.updateTableSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "rules",
            requiresAuth: true,
            modifiesState: true,
          },
        },
        {
          name: "get_project_history",
          description: "Get version history for a project",
          inputSchema: zodToJsonSchema(schemas.getProjectHistorySchema) as any,
          _meta: {
            version: "1.0.0",
            category: "version-control",
            requiresAuth: true,
          },
        },
        {
          name: "list_branches",
          description: "List branches for a repository (if supported)",
          inputSchema: zodToJsonSchema(schemas.listBranchesSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "version-control",
            requiresAuth: true,
          },
        },
        {
          name: "create_branch",
          description: "Create a new branch for a project",
          inputSchema: zodToJsonSchema(schemas.createBranchSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "version-control",
            requiresAuth: true,
            modifiesState: true,
          },
        },
        {
          name: "list_deployments",
          description: "List all project deployments",
          inputSchema: zodToJsonSchema(schemas.z.object({})) as any,
          _meta: {
            version: "1.0.0",
            category: "deployment",
            requiresAuth: true,
          },
        },
        {
          name: "deploy_project",
          description: "Deploy a project to production",
          inputSchema: zodToJsonSchema(schemas.deployProjectSchema) as any,
          _meta: {
            version: "1.0.0",
            category: "deployment",
            requiresAuth: true,
            modifiesState: true,
          },
        },
      ],
    }));

    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      try {
        const { name, arguments: args } = request.params;

        // Type guard for args
        if (!args) {
          throw new McpError(ErrorCode.InvalidParams, "Missing arguments");
        }

        switch (name) {
          case "health_check": {
            const health = await this.client.healthCheck();
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(health, null, 2),
                },
              ],
            };
          }

          case "list_repositories": {
            const repos = await this.client.listRepositories();
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(repos, null, 2),
                },
              ],
            };
          }

          case "list_projects": {
            const projects = await this.client.listProjects(args);
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(projects, null, 2),
                },
              ],
            };
          }

          case "get_project": {
            const project = await this.client.getProject(args.projectId as string);
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(project, null, 2),
                },
              ],
            };
          }

          case "get_project_info": {
            const info = await this.client.getProjectInfo(
              args.repository as string,
              args.projectName as string
            );
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(info, null, 2),
                },
              ],
            };
          }

          case "open_project": {
            await this.client.openProject(args.projectId as string);
            return {
              content: [
                {
                  type: "text",
                  text: `Project ${args.projectId} opened successfully`,
                },
              ],
            };
          }

          case "close_project": {
            await this.client.closeProject(args.projectId as string);
            return {
              content: [
                {
                  type: "text",
                  text: `Project ${args.projectId} closed successfully`,
                },
              ],
            };
          }

          case "list_tables": {
            const tables = await this.client.listTables(args.projectId as string);
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(tables, null, 2),
                },
              ],
            };
          }

          case "get_table": {
            const table = await this.client.getTable(
              args.projectId as string,
              args.tableId as string
            );
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(table, null, 2),
                },
              ],
            };
          }

          case "update_table": {
            await this.client.updateTable(
              args.projectId as string,
              args.tableId as string,
              args.view as Types.EditableTableView,
              args.comment as string | undefined
            );
            return {
              content: [
                {
                  type: "text",
                  text: `Table ${args.tableId} updated successfully`,
                },
              ],
            };
          }

          case "get_project_history": {
            const history = await this.client.getProjectHistory(
              args.repository as string,
              args.projectName as string
            );
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(history, null, 2),
                },
              ],
            };
          }

          case "list_branches": {
            const branches = await this.client.listBranches(args.repository as string);
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(branches, null, 2),
                },
              ],
            };
          }

          case "create_branch": {
            await this.client.createBranch(
              args.projectId as string,
              args.branchName as string,
              args.comment as string | undefined
            );
            return {
              content: [
                {
                  type: "text",
                  text: `Branch ${args.branchName} created successfully`,
                },
              ],
            };
          }

          case "list_deployments": {
            const deployments = await this.client.listDeployments();
            return {
              content: [
                {
                  type: "text",
                  text: JSON.stringify(deployments, null, 2),
                },
              ],
            };
          }

          case "deploy_project": {
            const deployRequest: Types.DeployRequest = {
              projectName: args.projectName as string,
              repository: args.repository as string,
              deploymentRepository: args.deploymentRepository as string,
              version: args.version as string | undefined,
            };
            await this.client.deployProject(deployRequest);
            return {
              content: [
                {
                  type: "text",
                  text: `Project ${args.projectName} deployed successfully`,
                },
              ],
            };
          }

          default:
            throw new McpError(ErrorCode.MethodNotFound, `Unknown tool: ${name}`);
        }
      } catch (error: any) {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status;
          const message = error.response?.data?.message || error.message;
          const endpoint = error.config?.url;
          const method = error.config?.method?.toUpperCase();

          // Enhanced error message with context
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
        throw error;
      }
    });
  }

  async run(): Promise<void> {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    console.error("OpenL Tablets MCP Server running on stdio");
  }
}

// Start the server
const server = new OpenLMCPServer();
server.run().catch(console.error);
