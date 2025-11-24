#!/usr/bin/env node

/**
 * MCP Proxy Server
 * 
 * Local proxy that connects to Cursor via stdio (MCP protocol)
 * and forwards requests to the MCP HTTP API running in Docker container.
 * 
 * This allows Cursor to use MCP tools from a Docker container
 * without needing direct stdio access to the container.
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
import axios, { AxiosInstance } from "axios";

const MCP_API_URL = process.env.MCP_API_URL || "http://localhost:3000";

// Create axios instance for HTTP API
const apiClient: AxiosInstance = axios.create({
  baseURL: MCP_API_URL,
  timeout: 60000,
  headers: {
    "Content-Type": "application/json",
  },
});

class MCPProxyServer {
  private server: Server;

  constructor() {
    this.server = new Server(
      {
        name: "openl-mcp-proxy",
        version: "1.0.0",
      },
      {
        capabilities: {
          tools: {},
          resources: {},
          prompts: {},
        },
      }
    );

    this.setupHandlers();
  }

  private setupHandlers(): void {
    // List available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      try {
        const response = await apiClient.get("/tools");
        return {
          tools: response.data.tools.map((tool: any) => ({
            name: tool.name,
            description: tool.description,
            inputSchema: tool.inputSchema,
            ...(tool.annotations && { annotations: tool.annotations }),
          })),
        };
      } catch (error: any) {
        throw new McpError(
          ErrorCode.InternalError,
          `Failed to fetch tools: ${error.message}`
        );
      }
    });

    // Handle tool execution
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      try {
        const { name, arguments: args } = request.params;
        
        const response = await apiClient.post("/execute", {
          tool: name,
          arguments: args || {},
        });

        return response.data.result;
      } catch (error: any) {
        if (error.response) {
          const errorData = error.response.data;
          throw new McpError(
            ErrorCode.InternalError,
            errorData.message || `Tool execution failed: ${error.message}`
          );
        }
        throw new McpError(
          ErrorCode.InternalError,
          `Failed to execute tool: ${error.message}`
        );
      }
    });

    // List available resources
    this.server.setRequestHandler(ListResourcesRequestSchema, async () => {
      try {
        // Proxy resources from HTTP API if available
        // For now, return empty list as HTTP API doesn't expose resources
        return { resources: [] };
      } catch (error: any) {
        throw new McpError(
          ErrorCode.InternalError,
          `Failed to list resources: ${error.message}`
        );
      }
    });

    // Handle resource read
    this.server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
      throw new McpError(
        ErrorCode.MethodNotFound,
        "Resource reading not supported via HTTP proxy"
      );
    });

    // List available prompts
    this.server.setRequestHandler(ListPromptsRequestSchema, async () => {
      try {
        // Prompts are not exposed via HTTP API
        return { prompts: [] };
      } catch (error: any) {
        throw new McpError(
          ErrorCode.InternalError,
          `Failed to list prompts: ${error.message}`
        );
      }
    });

    // Handle prompt get
    this.server.setRequestHandler(GetPromptRequestSchema, async (request) => {
      throw new McpError(
        ErrorCode.MethodNotFound,
        "Prompts not supported via HTTP proxy"
      );
    });
  }

  async start(): Promise<void> {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    console.error(`✅ MCP Proxy connected to ${MCP_API_URL}`);
  }
}

// Start the proxy server
async function main(): Promise<void> {
  try {
    // Test connection to HTTP API
    try {
      const healthCheck = await apiClient.get("/health");
      console.error(`✅ Connected to MCP HTTP API at ${MCP_API_URL}`);
    } catch (error: any) {
      console.error(`❌ Failed to connect to MCP HTTP API at ${MCP_API_URL}`);
      console.error(`   Error: ${error.message}`);
      console.error(`   Make sure the Docker container is running:`);
      console.error(`   docker compose up mcp-server`);
      process.exit(1);
    }

    const proxy = new MCPProxyServer();
    await proxy.start();
  } catch (error: unknown) {
    const message = error instanceof Error ? error.message : String(error);
    console.error("Failed to start MCP Proxy:", message);
    process.exit(1);
  }
}

main();

