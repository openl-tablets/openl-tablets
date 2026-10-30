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
      "Restore a project module to a version from its local edit history. Use the projectId and moduleName from the corresponding openl_list_project_local_changes call, plus a historyId returned by it. The project must be opened first; repository 'local' is unsupported. The whole module takes that version, so every later edit of the module is undone too. The answer names as 'undo' the historyId of the module as it was right before, which restores it back. To put back one value only: restore, read the value with openl_get_table, restore 'undo', and set the value with openl_edit_table on the table read again.",
    schema: schemas.restoreProjectLocalChangeSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, moduleName, historyId } = args;
      const before = new Set((await client.getProjectLocalChanges(projectId, moduleName)).map(({ id }) => id));
      await client.restoreProjectLocalChange(projectId, moduleName, historyId);
      // Studio keeps the module as it was in the history, which it lists newest first: the version the restore added.
      // The restore is done once Studio answers it, so a failed read of the history leaves only the undo unknown
      const after = await client.getProjectLocalChanges(projectId, moduleName).catch(() => undefined);
      if (!after) {
        return formatResult({
          restored: historyId,
          note: "The history could not be read after the restore: openl_list_project_local_changes lists the version that undoes it.",
        });
      }
      const undo = after.find(({ id, current }) => !current && !before.has(id))?.id;

      return formatResult({ restored: historyId, undo });
    },
  });
}
