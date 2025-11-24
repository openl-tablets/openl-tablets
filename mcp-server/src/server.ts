#!/usr/bin/env node

/**
 * Express HTTP Server for OpenL Tablets MCP Server
 * 
 * Provides HTTP REST API for accessing MCP tools as a standalone service.
 * This allows the MCP server to be used as a microservice in Docker Compose.
 */

import express, { Request, Response, NextFunction } from 'express';
import cors from 'cors';
import { randomUUID } from 'node:crypto';
import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import { SSEServerTransport } from "@modelcontextprotocol/sdk/server/sse.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { isInitializeRequest } from "@modelcontextprotocol/sdk/types.js";
import { OpenLClient } from './client.js';
import { getAllTools, executeTool, registerAllTools } from './tool-handlers.js';
import { loadConfigFromEnv } from './index.js';
import { sanitizeError, safeStringify } from './utils.js';
import { SERVER_INFO } from './constants.js';
import { PROMPTS, loadPromptContent, getPromptDefinition } from './prompts-registry.js';
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

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Request logging middleware
app.use((req: Request, res: Response, next: NextFunction) => {
  console.log(`${new Date().toISOString()} ${req.method} ${req.path}`);
  next();
});

// Initialize OpenL client
let client: OpenLClient;

// Initialize OpenL client (async - will be awaited before server starts)
async function initializeClient(): Promise<void> {
  try {
    const config = await loadConfigFromEnv();
    client = new OpenLClient(config);
    console.log(`✅ OpenL client initialized with base URL: ${config.baseUrl}`);
  } catch (error) {
    console.error('❌ Failed to initialize OpenL client:', sanitizeError(error));
    process.exit(1);
  }
}

// Initialize MCP Server for SSE transport
let mcpServer: Server;

async function initializeMCPServer(): Promise<void> {
  try {
    mcpServer = new Server(
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

    // Register all tools (client must be initialized first)
    registerAllTools(mcpServer, client);

  // Setup MCP handlers (similar to index.ts)
  mcpServer.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: getAllTools().map(({ name, title, description, inputSchema, annotations }) => ({
      name,
      description,
      inputSchema,
      ...(annotations && { annotations }),
    })),
  }));

  mcpServer.setRequestHandler(CallToolRequestSchema, async (request) => {
    const result = await executeTool(request.params.name, request.params.arguments, client);
    return result as any;
  });

  mcpServer.setRequestHandler(ListResourcesRequestSchema, async () => ({
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
        description: "Get details for a specific project",
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
  mcpServer.setRequestHandler(ReadResourceRequestSchema, async (request) => {
    return handleResourceRead(request.params.uri, client);
  });

  mcpServer.setRequestHandler(ListPromptsRequestSchema, async () => ({
    prompts: PROMPTS,
  }));

  mcpServer.setRequestHandler(GetPromptRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    const prompt = getPromptDefinition(name);
    if (!prompt) {
      throw new Error(`Prompt not found: ${name}`);
    }
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
  });

    console.log(`✅ MCP Server initialized for SSE transport`);
  } catch (error) {
    console.error('❌ Failed to initialize MCP Server:', sanitizeError(error));
    process.exit(1);
  }
}

// Store SSE transports by session ID
const sseTransports: Record<string, SSEServerTransport> = {};
// Store streamableHttp transports by session ID
const streamableHttpTransports: Record<string, StreamableHTTPServerTransport> = {};

/**
 * Handle resource read requests (shared between SSE and REST API)
 */
async function handleResourceRead(
  uri: string,
  client: OpenLClient
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
        data = await client.listRepositories();
        break;
      }

      case "projects": {
        if (!path) {
          // openl://projects - List all projects
          data = await client.listProjects();
        } else {
          // Parse projects/{projectId} or projects/{projectId}/...
          const projectMatch = path.match(/^([^\/]+)(?:\/(.+))?$/);
          if (!projectMatch) {
            throw new McpError(ErrorCode.InvalidRequest, `Invalid project URI: ${uri}`);
          }

          const [, projectId, subPath] = projectMatch;

          if (!subPath) {
            // openl://projects/{projectId} - Get project details
            data = await client.getProject(projectId);
          } else if (subPath === "history") {
            // openl://projects/{projectId}/history - Get project history
            data = await client.getProjectHistory({ projectId });
          } else if (subPath.startsWith("tables")) {
            // Parse tables or tables/{tableId}
            const tableMatch = subPath.match(/^tables(?:\/(.+))?$/);
            if (!tableMatch) {
              throw new McpError(ErrorCode.InvalidRequest, `Invalid tables URI: ${uri}`);
            }

            const [, tableId] = tableMatch;

            if (!tableId) {
              // openl://projects/{projectId}/tables - List tables
              data = await client.listTables(projectId);
            } else {
              // openl://projects/{projectId}/tables/{tableId} - Get table
              data = await client.getTable(projectId, tableId);
            }
          } else if (subPath.startsWith("files/")) {
            // openl://projects/{projectId}/files/{filePath} - Download file
            const filePath = subPath.substring(6); // Remove "files/" prefix
            if (!filePath) {
              throw new McpError(ErrorCode.InvalidRequest, `File path is required: ${uri}`);
            }

            const fileBuffer = await client.downloadFile(projectId, filePath);
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
        data = await client.listDeployments();
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
 * SSE endpoint for MCP protocol (for Cursor direct connection)
 * GET /mcp/sse - Establishes SSE connection
 */
app.get('/mcp/sse', async (req: Request, res: Response) => {
  try {
    const transport = new SSEServerTransport('/mcp/messages', res);
    const sessionId = transport.sessionId;
    sseTransports[sessionId] = transport;

    res.on('close', () => {
      delete sseTransports[sessionId];
      console.log(`SSE session ${sessionId} closed`);
    });

    await mcpServer.connect(transport);
    console.log(`✅ SSE connection established: session ${sessionId}`);
  } catch (error) {
    console.error('❌ Failed to establish SSE connection:', sanitizeError(error));
    res.status(500).json({ error: 'Failed to establish SSE connection' });
  }
});

/**
 * StreamableHttp endpoint for MCP protocol (for Cursor direct connection)
 * POST /mcp/sse - Establishes streamableHttp connection
 * This allows Cursor to connect immediately without fallback to SSE
 */
app.post('/mcp/sse', async (req: Request, res: Response) => {
  try {
    // Check for existing session ID in headers
    const sessionId = req.headers['mcp-session-id'] as string | undefined;
    let transport: StreamableHTTPServerTransport;

    if (sessionId && streamableHttpTransports[sessionId]) {
      // Reuse existing transport
      transport = streamableHttpTransports[sessionId];
    } else if (!sessionId && isInitializeRequest(req.body)) {
      // New initialization request
      transport = new StreamableHTTPServerTransport({
        sessionIdGenerator: () => randomUUID(),
        onsessioninitialized: (id) => {
          streamableHttpTransports[id] = transport;
          console.log(`StreamableHttp session ${id} initialized`);
        }
      });

      // Clean up transport when closed
      transport.onclose = () => {
        if (transport.sessionId) {
          delete streamableHttpTransports[transport.sessionId];
          console.log(`StreamableHttp session ${transport.sessionId} closed`);
        }
      };

      // Connect server to transport
      await mcpServer.connect(transport);
      console.log(`✅ StreamableHttp connection established: session ${transport.sessionId || 'new'}`);
    } else {
      // Invalid request
      return res.status(400).json({ error: 'Invalid request' });
    }

    // Handle the request - StreamableHTTPServerTransport handles req/res internally
    // The transport will process the request and send the response
    await transport.handleRequest(req, res, req.body);
  } catch (error) {
    console.error('❌ Failed to handle StreamableHttp request:', sanitizeError(error));
    res.status(500).json({ error: 'Failed to handle StreamableHttp request' });
  }
});

/**
 * Message endpoint for SSE transport
 * POST /mcp/messages?sessionId=xxx - Sends messages to MCP server
 */
app.post('/mcp/messages', async (req: Request, res: Response) => {
  try {
    const sessionId = req.query.sessionId as string;
    const transport = sseTransports[sessionId];
    
    if (!transport) {
      return res.status(404).json({ error: 'Session not found' });
    }

    await transport.handlePostMessage(req, res, req.body);
  } catch (error) {
    console.error('❌ Failed to handle message:', sanitizeError(error));
    res.status(500).json({ error: 'Failed to handle message' });
  }
});

/**
 * Health check endpoint
 */
app.get('/health', (req: Request, res: Response) => {
  res.json({
    status: 'ok',
    timestamp: new Date().toISOString(),
    service: 'openl-mcp-server',
    version: '1.0.0'
  });
});

/**
 * List all available tools
 */
app.get('/tools', (req: Request, res: Response) => {
  try {
    const tools = getAllTools();
    res.json({
      tools,
      count: tools.length
    });
  } catch (error) {
    res.status(500).json({
      error: 'Failed to list tools',
      message: sanitizeError(error)
    });
  }
});

/**
 * Get tool information by name
 */
app.get('/tools/:toolName', (req: Request, res: Response) => {
  try {
    const { toolName } = req.params;
    const tools = getAllTools();
    const tool = tools.find(t => t.name === toolName);
    
    if (!tool) {
      return res.status(404).json({
        error: 'Tool not found',
        toolName
      });
    }
    
    res.json(tool);
  } catch (error) {
    res.status(500).json({
      error: 'Failed to get tool information',
      message: sanitizeError(error)
    });
  }
});

/**
 * Execute a tool
 */
app.post('/tools/:toolName/execute', async (req: Request, res: Response) => {
  try {
    const { toolName } = req.params;
    const args = req.body;
    
    console.log(`Executing tool: ${toolName}`, args);
    
    const result = await executeTool(toolName, args, client);
    
    res.json({
      tool: toolName,
      result
    });
  } catch (error: unknown) {
    const errorMessage = sanitizeError(error);
    console.error(`Error executing tool ${req.params.toolName}:`, errorMessage);
    
    res.status(500).json({
      error: 'Tool execution failed',
      tool: req.params.toolName,
      message: errorMessage
    });
  }
});

/**
 * Convenience endpoint: execute tool via POST body
 */
app.post('/execute', async (req: Request, res: Response) => {
  try {
    const { tool, arguments: args } = req.body;
    
    if (!tool) {
      return res.status(400).json({
        error: 'Missing required field: tool'
      });
    }
    
    console.log(`Executing tool: ${tool}`, args);
    
    const result = await executeTool(tool, args || {}, client);
    
    res.json({
      tool,
      result
    });
  } catch (error: unknown) {
    const errorMessage = sanitizeError(error);
    console.error(`Error executing tool:`, errorMessage);
    
    res.status(500).json({
      error: 'Tool execution failed',
      message: errorMessage
    });
  }
});

/**
 * Error handling middleware
 */
app.use((err: Error, req: Request, res: Response, next: NextFunction) => {
  console.error('Unhandled error:', err);
  res.status(500).json({
    error: 'Internal server error',
    message: sanitizeError(err)
  });
});

/**
 * 404 handler
 */
app.use((req: Request, res: Response) => {
  res.status(404).json({
    error: 'Not found',
    path: req.path,
    method: req.method
  });
});

/**
 * Start the server
 */
async function startServer(): Promise<void> {
  // Initialize client before starting server
  await initializeClient();
  
  // Initialize MCP server (requires client to be initialized)
  await initializeMCPServer();
  
  app.listen(PORT, () => {
    console.log(`🚀 OpenL MCP Server HTTP API listening on port ${PORT}`);
    console.log(`📋 Health check: http://localhost:${PORT}/health`);
    console.log(`🔧 Tools list: http://localhost:${PORT}/tools`);
    console.log(`⚙️  Execute tool: POST http://localhost:${PORT}/execute`);
    console.log(`🔌 MCP SSE endpoint (for Cursor): http://localhost:${PORT}/mcp/sse`);
    console.log(`📨 MCP Messages endpoint: http://localhost:${PORT}/mcp/messages`);
  });
}

// Start the server
startServer().catch((error) => {
  console.error('❌ Failed to start server:', sanitizeError(error));
  process.exit(1);
});

