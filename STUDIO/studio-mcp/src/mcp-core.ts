import { Server, ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { Tool } from "@modelcontextprotocol/server";
import { SERVER_INFO, mcpToolName, stripToolPrefix, toolAllowList } from "./constants.js";
import { versionInfo } from "./build-info.js";
import { PROMPTS, loadPromptContent, getPromptDefinition } from "./prompts-registry.js";
import { registerAllTools, getAllTools, executeTool, hasTool } from "./handlers/index.js";
import { sanitizeError } from "./utils.js";
import type { OpenLClient } from "./client.js";

/**
 * Register every MCP request handler on `server`, dispatching to `client`.
 */
export function registerMcpHandlers(server: Server, client: OpenLClient): void {
  // List available tools. The registry holds bare names; the `openl_`
  // namespace prefix is a protocol concern applied only here, on the wire.
  server.setRequestHandler('tools/list', async () => {
    const allowed = toolAllowList();
    const tools = allowed ? getAllTools().filter((t) => allowed.has(t.name)) : getAllTools();
    return {
      tools: tools.map(({ name, title, description, inputSchema, annotations }) => ({
        name: mcpToolName(name),
        title,
        description,
        inputSchema: inputSchema as Tool["inputSchema"],
        ...(annotations && { annotations }),
      })),
    };
  });

  // Handle tool execution. Adapt the SDK v2 context into the small stable shape
  // the tool handlers need (progress token, notification sender, AbortSignal).
  // Strip the wire prefix back to the bare registry name before dispatching.
  server.setRequestHandler('tools/call', async (request, ctx) => {
    const toolName = stripToolPrefix(request.params.name);
    // Filtering tools/list alone would be decoration: a client that already knows a
    // name can still call it. The allow-list has to gate execution, and it has to do
    // so with the SAME error an unregistered name gets, so a withheld tool is
    // indistinguishable from one this build never had.
    const allowed = toolAllowList();
    if (allowed && !allowed.has(toolName)) {
      throw new ProtocolError(ProtocolErrorCode.MethodNotFound, `Unknown tool: ${request.params.name}`);
    }
    try {
      const result = await executeTool(toolName, request.params.arguments, client, {
        signal: ctx.mcpReq.signal,
        _meta: ctx.mcpReq._meta,
        sendNotification: (notification) => ctx.mcpReq.notify(notification),
      });
      return result;
    } catch (error) {
      // A tool's own failure (backend 4xx/5xx, argument validation) must reach the
      // calling agent as an `isError` RESULT, not a thrown JSON-RPC protocol error.
      // A throw is surfaced by clients as a generic "tool execution failed" with the
      // detail dropped; an isError result carries the message into the model's
      // context so it can self-correct (e.g. "column height 6 exceeds table height
      // 5"). executeTool already wrapped the cause into a ProtocolError with a detailed,
      // sanitized message. Only a genuinely unknown tool stays a protocol error —
      // distinguished by the registry, NOT by the error code: a backend HTTP 405
      // also maps to ProtocolErrorCode.MethodNotFound, so a code check would wrongly re-throw
      // a real tool failure as a protocol error.
      if (!hasTool(toolName)) {
        throw error;
      }
      const message = error instanceof ProtocolError ? error.message : sanitizeError(error);
      return {
        content: [{ type: "text" as const, text: message }],
        isError: true,
      };
    }
  });

  // List available prompts
  server.setRequestHandler('prompts/list', async () => ({
    prompts: PROMPTS,
  }));

  // Get specific prompt with optional arguments
  server.setRequestHandler('prompts/get', async (request) => {
    const { name, arguments: args } = request.params;

    const prompt = getPromptDefinition(name);
    if (!prompt) {
      throw new ProtocolError(
        ProtocolErrorCode.InvalidRequest,
        `Prompt not found: ${name}`
      );
    }

    try {
      const content = loadPromptContent(name, args);

      return {
        description: prompt.description,
        messages: [
          {
            role: "user" as const,
            content: {
              type: "text" as const,
              text: content,
            },
          },
        ],
      };
    } catch (error) {
      throw new ProtocolError(
        ProtocolErrorCode.InternalError,
        `Failed to load prompt: ${error instanceof Error ? error.message : String(error)}`
      );
    }
  });
}

/**
 * Build a fully-configured MCP `Server` for one OpenL client: capabilities
 * declared, tools registered, every request handler wired. The caller attaches
 * a transport.
 */
export function createConfiguredServer(client: OpenLClient): Server {
  const server = new Server(
    {
      name: SERVER_INFO.NAME,
      version: versionInfo().version,
      description: SERVER_INFO.DESCRIPTION,
    },
    {
      capabilities: {
        tools: {},
        prompts: {},
      },
    }
  );

  registerAllTools();
  registerMcpHandlers(server, client);

  return server;
}
