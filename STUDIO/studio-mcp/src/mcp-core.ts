import { CLIENT_INFO_META_KEY, McpServer, type StandardSchemaWithJSON } from "@modelcontextprotocol/server";
import { SERVER_INFO, mcpToolName } from "./constants.js";
import { registerAllTools, getAllTools, executeTool } from "./handlers/index.js";
import type { OpenLClient } from "./client.js";
import { SKILLS, SKILLS_OVERVIEW } from "./skills.js";
import { traceToolCall } from "./tool-telemetry.js";
import type { McpClientInfo } from "./types.js";

/**
 * The name and the version of the client a request of the stateless protocol (2026-07-28) carries in its `_meta`
 * envelope, or nothing for a request of a session, which named its client when it opened.
 */
export function clientInfoOf(envelope: object | undefined): McpClientInfo | undefined {
  const info = (envelope as Record<string, unknown> | undefined)?.[CLIENT_INFO_META_KEY];
  if (typeof info !== "object" || info === null || typeof (info as { name?: unknown }).name !== "string") {
    return undefined;
  }
  const { name, version } = info as { name: string; version?: unknown };
  return { name, version: typeof version === "string" ? version : undefined };
}

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
 * The instructions a client gives its agent when it connects: how to work with OpenL Studio through the tools, in the
 * order a task takes, and the skills, which they name for the clients that do not list them through the skills
 * extension. Every client loads them once, where a tool of the same text would cost a call and its answer on every
 * later turn.
 */
const INSTRUCTIONS = `This server connects you to OpenL Studio, the IDE of the OpenL Tablets business rules engine. Rules \
live in Excel tables (decision tables, spreadsheets, datatypes, tests) inside versioned projects.

## How to work

1. Find the project with \`openl_list_projects\` (\`name\`). Open it with \`openl_open_project\` before any table, \
test or trace tool; a project whose \`capabilities\` of \`openl_get_project\` lack \`canOpen\` is always open.
2. Before you change a project, read its guidance with \`openl_get_project_agent_context\`. Look up OpenL syntax, \
table types and functions with \`openl_list_guides\` and \`openl_get_guides\`, never from memory.
3. Find tables with \`openl_list_tables\` by \`name\`, \`kind\` or \`module\`; its \`text\` finds every table a \
value, key or message is used in, which a file search never does. Read a row of a large table with \
\`openl_get_table\` \`find\`, not page by page. \`openl_get_table_dependencies\` with \`direction: DEPENDENTS\` \
finds what uses a table.
4. Edit with \`openl_edit_table\`, several cells in one call with \`actions\`, passing the \`version\` the last read \
or edit answered. Then check \`compiles\` in its answer, \`openl_project_status\`, and the tests: \
\`openl_start_project_tests\`, then \`openl_get_test_results\`.
5. \`openl_project_status\` with \`changes: true\` names the tables changed in the working copy since the last save. A \
restore with \`openl_restore_project_local_change\` undoes every later edit of the module: preview it first. Edits \
stay in the working copy until \`openl_save_project\` commits them: say so when you leave them unsaved. \
\`openl_deploy_project\` deploys. A project of the \`local\` repository, or edited in place, has no save: its edits \
are in its files already.

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
      },
    },
  );
  for (const { name, wireName, config } of TOOLS) {
    // Adapt the SDK context into the small stable shape the tool handlers need (AbortSignal, progress token,
    // notification sender).
    server.registerTool(
      wireName,
      config,
      (args, ctx) => {
        // A client of the stateless protocol names itself in each request; a session got the name when it opened
        client.callsFor(clientInfoOf(ctx.mcpReq.envelope));
        return traceToolCall(wireName, () => executeTool(name, args, client, {
          signal: ctx.mcpReq.signal,
          _meta: ctx.mcpReq._meta,
          sendNotification: (notification) => ctx.mcpReq.notify(notification),
        }));
      },
    );
  }
  SKILLS.attach(server);

  return server;
}
