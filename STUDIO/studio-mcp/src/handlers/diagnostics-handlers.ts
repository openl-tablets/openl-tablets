/**
 * Diagnostics tool handlers — report the running server's own identity.
 *
 * Local-only: the version, build, and runtime facts come from this process and
 * its build artifact (see {@link file://../build-info.ts}), so the tool answers
 * without OpenL Studio, credentials, or the network — it still works when the
 * configured studio is unreachable, which is exactly when a support request
 * needs the version.
 */
import * as schemas from "../schemas.js";
import { versionInfo } from "../build-info.js";
import { formatResponse } from "../formatters.js";
import { registerTool, type ToolResponse } from "./common.js";

export function registerDiagnosticsHandlers(): void {
  registerTool({
    name: "get_version",
    category: "Diagnostics",
    title: "Get Server Version and Build Info",
    description:
      "Report this MCP server's own version and build identity for diagnostics and bug reports: the version of the OpenL Studio it is built into, the build id (version plus the short commit it was built from, suffixed '.dirty' for a modified working tree), the full commit and its date, the branch or tag, the build timestamp, and the Node.js/platform/architecture it runs on. Quote 'build.id' when reporting a problem — snapshot builds of one version share it and are otherwise indistinguishable. Needs no OpenL Studio connection and returns no configuration, credentials, or URLs. A build.source of 'unavailable' means this install shipped without build metadata, so only the version is known.",
    schema: schemas.getVersionSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: false,
      idempotentHint: true,
    },
    handler: async (args): Promise<ToolResponse> => {
      const formattedResult = formatResponse(versionInfo(), args.response_format);

      return { content: [{ type: "text", text: formattedResult }] };
    },
  });
}
