/**
 * Local-change tool handlers — list a project's uncommitted local changes and
 * restore (revert) an individual change.
 */

import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import { formatWholeList, markdownTable } from "../markdown-list.js";
import { formatCount } from "../text.js";
import { registerTool, formatResult, textResult, type ToolHandlerExtra, type ToolResponse } from "./common.js";
import { waitForResult, type WaitOptions } from "./wait.js";

/** How a preview of a restore waits while Studio compares the two versions. */
const COMPARISON_WAIT: WaitOptions = {
  timeoutMs: 60_000,
  progressMessage: "Comparing the version with the current one",
  timeoutMessage: "The comparison of the two versions did not end in time.",
};

export function registerLocalChangeHandlers(): void {
  registerTool({
    name: "list_project_local_changes",
    title: "List Local Change History",
    description:
      "List the local edit history of a module of a project, or of every module, as a table: each version by the historyId openl_restore_project_local_change takes, with the time it was kept, the current one marked, which a restore leaves as it is. With a historyId, answer instead the tables a restore of the module to that version would change: every edit of the module after it is undone with them. The project must be opened first with openl_open_project; repository 'local' is unsupported because local projects cannot be opened. It is no check for unsaved changes: openl_project_status reports the pending changes of the project.",
    schema: schemas.listProjectLocalChangesSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const { projectId, module, historyId } = args;
      if (module !== undefined && historyId !== undefined) {
        return textResult(await restoreEffect(client, projectId, module, historyId, extra));
      }
      const modules = module === undefined
        ? (await client.listProjectModules(projectId)).flatMap(({ name }) => (name === undefined ? [] : [name]))
        : [module];
      const versions = (await Promise.all(modules.map(async (name) =>
        (await client.getProjectLocalChanges(projectId, name)).map((change) => ({ module: name, ...change })))))
        .flat();

      // Each entry is named by the historyId openl_restore_project_local_change takes
      return textResult(formatWholeList(versions, {
        noun: "versions",
        columns: [
          ...(module === undefined ? [{ header: "module", value: (change: typeof versions[number]) => change.module }] : []),
          { header: "historyId", value: (change) => change.id },
          { header: "kept at", value: (change) => change.modifiedOn },
          { header: "current", value: (change) => change.current },
        ],
      }));
    },
  });

  registerTool({
    name: "restore_project_local_change",
    title: "Restore Previous Local Version",
    description:
      "Restore a project module to a version from its local edit history. Use the projectId and module from the corresponding openl_list_project_local_changes call, plus a historyId returned by it; the same call with the historyId tells first which tables the restore changes. The project must be opened first; repository 'local' is unsupported. The whole module takes that version, so every later edit of the module is undone too. The answer names as 'undo' the historyId of the module as it was right before, which restores it back. To put back one value only: restore, read the value with openl_get_table, restore 'undo', and set the value with openl_edit_table on the table read again.",
    schema: schemas.restoreProjectLocalChangeSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { projectId, module: moduleName, historyId } = args;
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

/** The tables a restore of a module to a version of its history would change: those it holds otherwise than now. */
async function restoreEffect(
  client: OpenLClient,
  projectId: string,
  module: string,
  historyId: string,
  extra: ToolHandlerExtra | undefined,
): Promise<string> {
  const current = (await client.getProjectLocalChanges(projectId, module)).find((change) => change.current)?.id;
  if (current === undefined) {
    return `The history of ${module} marks no current version, so a restore to ${historyId} cannot be compared with ` +
      "it: no preview can tell what the restore changes.";
  }
  if (current === historyId) {
    return `${historyId} is the current version of ${module}: a restore to it changes nothing.`;
  }
  const id = await client.compareLocalVersions(projectId, module, historyId, current);
  const comparison = await waitForResult((request) => client.getComparison(id, request), COMPARISON_WAIT, extra)
    .finally(() => client.deleteComparison(id).catch(() => undefined));
  const changed = comparison.sheets.flatMap((sheet) => {
    const tables = (sheet.children ?? []).filter((table) => table.status !== "equal")
      .map((table) => ({ sheet: sheet.name, table: table.name, status: table.status }));
    return tables.length > 0 || sheet.status === "equal" ? tables : [{ sheet: sheet.name, table: "", status: sheet.status }];
  });
  if (changed.length === 0) {
    return `A restore of ${module} to ${historyId} changes no table: the version holds what the module holds now.`;
  }
  // The status reads from the version restored to the current one: what the restore undoes
  return [
    `A restore of ${module} to ${historyId} changes ${formatCount(changed.length, "table")}; ` +
      "'added' is a table the restore removes, 'removed' one it brings back.",
    markdownTable(changed, [
      { header: "sheet", value: (row) => row.sheet },
      { header: "table", value: (row) => row.table },
      { header: "since the version", value: (row) => row.status },
    ]),
  ].join("\n\n");
}
