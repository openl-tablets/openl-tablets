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
import axios, { AxiosInstance } from "axios";
import FormData from "form-data";
import type * as Types from "./types.js";

/**
 * OpenL Tablets API Client
 */
class OpenLClient {
  private client: AxiosInstance;
  private baseUrl: string;

  constructor(config: Types.OpenLConfig) {
    this.baseUrl = config.baseUrl.replace(/\/$/, "");

    const auth: any = {};
    if (config.username && config.password) {
      auth.username = config.username;
      auth.password = config.password;
    }

    this.client = axios.create({
      baseURL: this.baseUrl,
      auth: Object.keys(auth).length > 0 ? auth : undefined,
      headers: {
        "Content-Type": "application/json",
        ...(config.apiKey ? { "X-API-Key": config.apiKey } : {}),
      },
    });
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

    this.client = new OpenLClient({
      baseUrl,
      username,
      password,
      apiKey,
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
          name: "list_repositories",
          description: "List all design repositories in OpenL Tablets",
          inputSchema: {
            type: "object",
            properties: {},
          },
        },
        {
          name: "list_projects",
          description: "List projects with optional filters (repository, status, tag)",
          inputSchema: {
            type: "object",
            properties: {
              repository: {
                type: "string",
                description: "Filter by repository name",
              },
              status: {
                type: "string",
                description: "Filter by project status (OPENED, CLOSED, etc.)",
              },
              tag: {
                type: "string",
                description: "Filter by tag name",
              },
            },
          },
        },
        {
          name: "get_project",
          description: "Get detailed information about a specific project",
          inputSchema: {
            type: "object",
            properties: {
              projectId: {
                type: "string",
                description: "Project ID in format: repository_name-project_name",
              },
            },
            required: ["projectId"],
          },
        },
        {
          name: "get_project_info",
          description: "Get project info including modules and dependencies",
          inputSchema: {
            type: "object",
            properties: {
              repository: {
                type: "string",
                description: "Repository name",
              },
              projectName: {
                type: "string",
                description: "Project name",
              },
            },
            required: ["repository", "projectName"],
          },
        },
        {
          name: "open_project",
          description: "Open a project for viewing or editing",
          inputSchema: {
            type: "object",
            properties: {
              projectId: {
                type: "string",
                description: "Project ID in format: repository_name-project_name",
              },
            },
            required: ["projectId"],
          },
        },
        {
          name: "close_project",
          description: "Close an open project",
          inputSchema: {
            type: "object",
            properties: {
              projectId: {
                type: "string",
                description: "Project ID in format: repository_name-project_name",
              },
            },
            required: ["projectId"],
          },
        },
        {
          name: "list_tables",
          description: "List all tables (rules) in a project",
          inputSchema: {
            type: "object",
            properties: {
              projectId: {
                type: "string",
                description: "Project ID in format: repository_name-project_name",
              },
            },
            required: ["projectId"],
          },
        },
        {
          name: "get_table",
          description: "Get detailed table (rule) data including structure and content",
          inputSchema: {
            type: "object",
            properties: {
              projectId: {
                type: "string",
                description: "Project ID in format: repository_name-project_name",
              },
              tableId: {
                type: "string",
                description: "Table ID",
              },
            },
            required: ["projectId", "tableId"],
          },
        },
        {
          name: "update_table",
          description: "Update a table (rule) with new data",
          inputSchema: {
            type: "object",
            properties: {
              projectId: {
                type: "string",
                description: "Project ID",
              },
              tableId: {
                type: "string",
                description: "Table ID",
              },
              view: {
                type: "object",
                description: "Table view data to update",
              },
              comment: {
                type: "string",
                description: "Commit comment",
              },
            },
            required: ["projectId", "tableId", "view"],
          },
        },
        {
          name: "get_project_history",
          description: "Get version history for a project",
          inputSchema: {
            type: "object",
            properties: {
              repository: {
                type: "string",
                description: "Repository name",
              },
              projectName: {
                type: "string",
                description: "Project name",
              },
            },
            required: ["repository", "projectName"],
          },
        },
        {
          name: "list_branches",
          description: "List branches for a repository (if supported)",
          inputSchema: {
            type: "object",
            properties: {
              repository: {
                type: "string",
                description: "Repository name",
              },
            },
            required: ["repository"],
          },
        },
        {
          name: "create_branch",
          description: "Create a new branch for a project",
          inputSchema: {
            type: "object",
            properties: {
              projectId: {
                type: "string",
                description: "Project ID",
              },
              branchName: {
                type: "string",
                description: "New branch name",
              },
              comment: {
                type: "string",
                description: "Branch creation comment",
              },
            },
            required: ["projectId", "branchName"],
          },
        },
        {
          name: "list_deployments",
          description: "List all project deployments",
          inputSchema: {
            type: "object",
            properties: {},
          },
        },
        {
          name: "deploy_project",
          description: "Deploy a project to production",
          inputSchema: {
            type: "object",
            properties: {
              projectName: {
                type: "string",
                description: "Project name to deploy",
              },
              repository: {
                type: "string",
                description: "Source repository",
              },
              deploymentRepository: {
                type: "string",
                description: "Target deployment repository",
              },
              version: {
                type: "string",
                description: "Specific version to deploy (optional)",
              },
            },
            required: ["projectName", "repository", "deploymentRepository"],
          },
        },
      ],
    }));

    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      try {
        const { name, arguments: args } = request.params;

        switch (name) {
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
            const project = await this.client.getProject(args.projectId);
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
            const info = await this.client.getProjectInfo(args.repository, args.projectName);
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
            await this.client.openProject(args.projectId);
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
            await this.client.closeProject(args.projectId);
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
            const tables = await this.client.listTables(args.projectId);
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
            const table = await this.client.getTable(args.projectId, args.tableId);
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
            await this.client.updateTable(args.projectId, args.tableId, args.view, args.comment);
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
            const history = await this.client.getProjectHistory(args.repository, args.projectName);
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
            const branches = await this.client.listBranches(args.repository);
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
            await this.client.createBranch(args.projectId, args.branchName, args.comment);
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
            await this.client.deployProject(args as Types.DeployRequest);
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
          throw new McpError(
            ErrorCode.InternalError,
            `OpenL Tablets API error (${status}): ${message}`
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
