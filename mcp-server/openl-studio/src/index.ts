#!/usr/bin/env node

/**
 * OpenL Studio MCP Server
 * Main entry point for the Model Context Protocol server
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';

import { loadConfig } from './config/config.js';
import { OpenLClient } from './client/openl-client.js';
import { logger } from './utils/logger.js';

// Import all tools
import { listRepositoriesTool, listRepositories } from './tools/list-repositories.js';
import { listProjectsTool, listProjects } from './tools/list-projects.js';
import { getProjectInfoTool, getProjectInfo } from './tools/get-project-info.js';
import { openProjectTool, openProject } from './tools/open-project.js';
import { closeProjectTool, closeProject } from './tools/close-project.js';
import { exportProjectTool, exportProject } from './tools/export-project.js';
import { copyProjectTool, copyProject } from './tools/copy-project.js';

// Server version
const SERVER_VERSION = '1.0.0';

/**
 * Main server class
 */
class OpenLStudioMCPServer {
  private server: Server;
  private client: OpenLClient | null = null;

  constructor() {
    this.server = new Server(
      {
        name: 'openl-studio-mcp-server',
        version: SERVER_VERSION,
      },
      {
        capabilities: {
          tools: {},
        },
      }
    );

    this.setupHandlers();
  }

  /**
   * Set up request handlers
   */
  private setupHandlers(): void {
    // List available tools
    this.server.setRequestHandler(ListToolsRequestSchema, async () => {
      return {
        tools: [
          listRepositoriesTool,
          listProjectsTool,
          getProjectInfoTool,
          openProjectTool,
          closeProjectTool,
          exportProjectTool,
          copyProjectTool,
        ],
      };
    });

    // Handle tool calls
    this.server.setRequestHandler(CallToolRequestSchema, async (request) => {
      try {
        if (!this.client) {
          throw new Error('OpenL Studio client not initialized');
        }

        const { name, arguments: args } = request.params;

        logger.info(`Tool called: ${name}`);

        let result: string;

        switch (name) {
          case 'list_repositories':
            result = await listRepositories(args as any || {}, this.client);
            break;

          case 'list_projects':
            result = await listProjects(args as any || {}, this.client);
            break;

          case 'get_project_info':
            result = await getProjectInfo(args as any || {}, this.client);
            break;

          case 'open_project':
            result = await openProject(args as any || {}, this.client);
            break;

          case 'close_project':
            result = await closeProject(args as any || {}, this.client);
            break;

          case 'export_project':
            result = await exportProject(args as any || {}, this.client);
            break;

          case 'copy_project':
            result = await copyProject(args as any || {}, this.client);
            break;

          default:
            throw new Error(`Unknown tool: ${name}`);
        }

        return {
          content: [
            {
              type: 'text',
              text: result,
            },
          ],
        };
      } catch (error: any) {
        logger.error(`Tool execution error:`, error);
        return {
          content: [
            {
              type: 'text',
              text: `Error: ${error.message}`,
            },
          ],
          isError: true,
        };
      }
    });
  }

  /**
   * Initialize the server
   */
  async initialize(): Promise<void> {
    try {
      // Load configuration
      logger.info('Loading configuration...');
      const config = loadConfig();

      // Set log level
      if (config.logLevel) {
        logger.setLevel(config.logLevel);
      }

      // Initialize OpenL Studio client
      logger.info(`Connecting to OpenL Studio at ${config.openl.baseUrl}...`);
      this.client = new OpenLClient(config.openl);

      // Authenticate
      await this.client.authenticate();

      logger.info('OpenL Studio MCP Server initialized successfully');
    } catch (error: any) {
      logger.error('Failed to initialize server:', error);
      throw error;
    }
  }

  /**
   * Start the server
   */
  async start(): Promise<void> {
    const transport = new StdioServerTransport();
    await this.server.connect(transport);
    logger.info('OpenL Studio MCP Server running on stdio');
  }
}

/**
 * Main entry point
 */
async function main(): Promise<void> {
  try {
    const server = new OpenLStudioMCPServer();
    await server.initialize();
    await server.start();

    // Handle shutdown gracefully
    process.on('SIGINT', () => {
      logger.info('Received SIGINT, shutting down...');
      process.exit(0);
    });

    process.on('SIGTERM', () => {
      logger.info('Received SIGTERM, shutting down...');
      process.exit(0);
    });
  } catch (error: any) {
    logger.error('Fatal error:', error);
    process.exit(1);
  }
}

// Run the server
main().catch((error) => {
  console.error('Unhandled error:', error);
  process.exit(1);
});
