/**
 * Local-change tool handlers — list a project's uncommitted local changes and
 * restore (revert) an individual change.
 */

import * as schemas from "../schemas.js";
import { formatResponse } from "../formatters.js";
import { registerTool, type ToolResponse } from "./common.js";


export function registerLocalChangeHandlers(): void {
  registerTool({
    name: "list_project_local_changes",
    category: "Project",
    title: "List Local Change History",
    description:
      "List a module's local edit history for an explicitly identified project. Returns versions, timestamps, and which version is current. The project must be opened first with openl_open_project; repository 'local' is unsupported because local projects cannot be opened. Use openl_list_project_modules to obtain the required moduleName.",
    schema: schemas.listProjectLocalChangesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const changes = await client.getProjectLocalChanges(typedArgs.projectId, typedArgs.moduleName);

      const formattedResult = formatResponse(changes, format, {
        dataType: "local_changes",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "restore_project_local_change",
    category: "Project",
    title: "Restore Previous Local Version",
    description:
      "Restore a project module to a version from its local edit history. Use the projectId and moduleName from the corresponding openl_list_project_local_changes call, plus a historyId returned by it. The project must be opened first; repository 'local' is unsupported.",
    schema: schemas.restoreProjectLocalChangeSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      await client.restoreProjectLocalChange(
        typedArgs.projectId,
        typedArgs.moduleName,
        typedArgs.historyId,
      );

      const result = {
        success: true,
        message: `Successfully restored project module to local history version '${typedArgs.historyId}'`,
        projectId: typedArgs.projectId,
        moduleName: typedArgs.moduleName,
        historyId: typedArgs.historyId,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });
}
