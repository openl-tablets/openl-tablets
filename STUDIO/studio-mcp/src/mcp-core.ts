import { McpServer, type StandardSchemaWithJSON } from "@modelcontextprotocol/server";
import { SERVER_INFO, mcpToolName } from "./constants.js";
import { registerAllTools, getAllTools, executeTool } from "./handlers/index.js";
import type { OpenLClient } from "./client.js";
import { SKILLS_EXTENSION, registerSkills, SKILLS_OVERVIEW } from "./skills.js";

/**
 * The schema a tool is registered with: it publishes the JSON Schema of the tool as it is, and passes the arguments
 * on as they came, since `executeTool` validates them with the hints and the hooks of each tool.
 */
function publishedSchema(inputSchema: Record<string, unknown>): StandardSchemaWithJSON {
  return {
    "~standard": {
      version: 1,
      vendor: "studio-mcp",
      validate: (value) => ({ value }),
      jsonSchema: { input: () => inputSchema, output: () => inputSchema },
    },
  };
}

/**
 * The instructions a client gives its agent when it connects: start with `openl_get_started`, and follow the skill
 * that matches the task. They name the skills for the clients that do not list them through the skills extension.
 */
const INSTRUCTIONS = `This server connects you to OpenL Studio, the IDE of the OpenL Tablets business rules engine. \
Call \`openl_get_started\` once, before any other \`openl_\` tool.

${SKILLS_OVERVIEW}`;

// The tools are the same for every server, so they are registered, and their wire names and configurations built,
// once, when the module loads.
registerAllTools();
const TOOLS = getAllTools().map(({ name, title, description, inputSchema, annotations }) => ({
  name,
  wireName: mcpToolName(name),
  config: { title, description, inputSchema: publishedSchema(inputSchema), annotations },
}));

/**
 * Build an MCP server for one OpenL client. The caller attaches a transport.
 *
 * It serves every tool under its wire name, with the `openl_` namespace prefix, the registry holding bare names, and
 * the OpenL skills of `skills.ts`. A client picks the tools its agent uses. A tool's own failure (backend 4xx/5xx,
 * argument validation) reaches the calling agent as an `isError` result carrying the detailed, sanitized message
 * `executeTool` wraps it in, so the model can self-correct.
 */
export function createConfiguredServer(client: OpenLClient): McpServer {
  const server = new McpServer(
    {
      name: SERVER_INFO.NAME,
      version: SERVER_INFO.VERSION,
      description: SERVER_INFO.DESCRIPTION,
    },
    // The tools and the skills never change while the server runs
    {
      instructions: INSTRUCTIONS,
      capabilities: {
        tools: { listChanged: false },
        resources: { listChanged: false },
        extensions: { [SKILLS_EXTENSION]: {} },
      },
    },
  );

  for (const { name, wireName, config } of TOOLS) {
    // Adapt the SDK context into the small stable shape the tool handlers need (AbortSignal, progress token,
    // notification sender).
    server.registerTool(
      wireName,
      config,
      (args, ctx) => executeTool(name, args, client, {
        signal: ctx.mcpReq.signal,
        _meta: ctx.mcpReq._meta,
        sendNotification: (notification) => ctx.mcpReq.notify(notification),
      }),
    );
  }
  registerSkills(server);

  return server;
}
