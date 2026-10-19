/**
 * The end of every table edit, shared by the full update/append tools
 * (`table-handlers.ts`) and the raw-source actions (`table-action-handlers.ts`).
 *
 * Studio keeps the id of a table while it stays the same table: a move keeps it,
 * and only a rename, or a change of the table's `id` property, gives it a new one,
 * which the write answers. Studio also serves an id from before a rename under the
 * new one, and answers 410 for a deleted table, so the server keeps no ids of its
 * own.
 */

import type { OpenLClient } from "../client.js";
import type { RawTableView, ResponseFormat } from "../types.js";
import { formatResult, type ToolResponse } from "./common.js";

/**
 * Read a table back after an edit, with the messages of its compilation.
 *
 * The studio's table-edit endpoints (update / append / actions) compile the
 * written module at once only when automatic compilation is on. So after an
 * edit we read the table by id, which makes a subsequent openl_project_status
 * reflect the change. Best-effort:
 * the edit has already been committed, so a failure here is swallowed (the
 * status simply refreshes on the next table read) and answered as undefined
 * so callers don't claim `recompileTriggered: true` falsely.
 */
async function readTableBack(
  client: OpenLClient,
  projectId: string,
  tableId: string,
): Promise<RawTableView | undefined> {
  try {
    // A read of one row compiles the table as a read of all of it does
    return await client.getTable(projectId, tableId, { startRow: 0, maxRows: 1 });
  } catch {
    // Best-effort: the edit already applied; compile status refreshes on next read.
    return undefined;
  }
}

/**
 * Shared post-edit tail for the table-editing tools (update/append/raw actions).
 *
 * Reads the table back to trigger the studio recompile (noting a failure with
 * the tool's own verb, e.g. "update"/"append"/"edit"), and answers the standard
 * result (`success`, `message`, `recompileTriggered`, current `tableId`, plus
 * `tableIdChanged`/`previousTableId` and a `note` when present) in the format
 * the caller asked for.
 *
 * A table that does not compile after the edit is answered with `compiles: false`
 * and the `errors` of its compilation: the edit was applied, but the table needs a fix.
 *
 * @param args       the arguments of the edit: the table, and the format of the answer
 * @param reportedId the id Studio answered for the table, when the edit renamed it
 * @param message    the success message for this specific edit
 * @param editVerb   the verb of the edit for the recompile-miss note ("update", "append", "edit")
 */
export async function finishTableEdit(
  client: OpenLClient,
  args: { projectId: string; tableId: string; response_format?: ResponseFormat },
  reportedId: string | undefined,
  message: string,
  editVerb: string,
): Promise<ToolResponse> {
  const { projectId, tableId: requestedId } = args;
  const currentId = reportedId ?? requestedId;
  const notes: string[] = [];
  const idChanged = currentId !== requestedId;
  if (idChanged) {
    notes.push(
      `The table is now known by the id '${currentId}', no longer '${requestedId}'. Use 'tableId' from this response for subsequent calls.`,
    );
  }

  // Reading the table back makes openl_project_status reflect the change, also
  // when automatic compilation is off.
  const table = await readTableBack(client, projectId, currentId);
  const recompileTriggered = table !== undefined;
  if (!recompileTriggered) {
    notes.push(
      `The table could not be read back under id '${currentId}', but the ${editVerb} WAS applied. Run openl_project_status to see whether the project compiles.`,
    );
  }
  const errors = (table?.messages ?? [])
    .filter((item) => item.severity === "ERROR")
    .map((item) => item.summary ?? "");
  if (errors.length > 0) {
    notes.push(
      `The table does NOT compile after the ${editVerb}, which WAS applied: fix the errors, or restore the module from its local edit history (openl_list_project_local_changes, openl_restore_project_local_change).`,
    );
  }

  return formatResult({
    success: true,
    message,
    recompileTriggered,
    ...(errors.length > 0 ? { compiles: false, errors } : {}),
    tableId: currentId,
    ...(idChanged ? { tableIdChanged: true, previousTableId: requestedId } : {}),
    ...(notes.length > 0 ? { note: notes.join(" ") } : {}),
  }, args.response_format);
}
