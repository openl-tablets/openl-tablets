/**
 * Local-change tool handlers — list a project's uncommitted local changes and
 * restore (revert) an individual change.
 */

import * as schemas from "../schemas.js";

import { registerTool, formatResult, type ToolResponse } from "./common.js";

export function registerLocalChangeHandlers(): void {
  registerTool({
    name: "list_project_local_changes",
    title: "List Local Change History",
    description:
      "List a module's local edit history for an explicitly identified project. Returns each version by the historyId openl_restore_project_local_change takes, with its time, and marks the current one, which a restore leaves as it is. The project must be opened first with openl_open_project; repository 'local' is unsupported because local projects cannot be opened. Use openl_list_project_modules to obtain the required moduleName. It is no check for unsaved changes: openl_project_status reports the pending changes of the project.",
    schema: schemas.projectModuleSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const changes = await client.getProjectLocalChanges(args.projectId, args.moduleName);

      // Each entry is named by the historyId openl_restore_project_local_change takes
      return formatResult(changes.map(({ id, ...change }) => ({ historyId: id, ...change })));
    },
  });

  registerTool({
    name: "restore_project_local_change",
    title: "Restore Previous Local Version",
    description:
      "Restore a project module to a version from its local edit history. Use the projectId and moduleName from the corresponding openl_list_project_local_changes call, plus a historyId returned by it. The project must be opened first; repository 'local' is unsupported.",
    schema: schemas.restoreProjectLocalChangeSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.restoreProjectLocalChange(
        args.projectId,
        args.moduleName,
        args.historyId,
      );

      const result = {
        success: true,
        message: `Successfully restored project module to local history version '${args.historyId}'`,
        projectId: args.projectId,
        moduleName: args.moduleName,
        historyId: args.historyId,
      };

      return formatResult(result);
    },
  });
}
