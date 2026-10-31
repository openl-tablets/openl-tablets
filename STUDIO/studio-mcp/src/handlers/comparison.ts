/**
 * The tables two versions of a workbook differ in, as OpenL Studio compares them in the background: two versions of
 * a module of its local history, or a file of the working copy and the same file of a revision.
 *
 * Studio holds one comparison per client, and starting another drops the one before, so a caller compares one pair
 * at a time.
 */

import type { OpenLClient } from "../client.js";
import type { ToolHandlerExtra } from "./common.js";
import { waitForResult, type WaitOptions } from "./wait.js";

/** How a comparison is waited for while Studio compares the two versions. */
const COMPARISON_WAIT: WaitOptions = {
  timeoutMs: 60_000,
  progressMessage: "Comparing the two versions",
  timeoutMessage: "The comparison of the two versions did not end in time.",
};

/** A table one version holds otherwise than the other: added, removed or changed since the first. */
export interface ChangedTable {
  sheet?: string;
  table?: string;
  status?: string;
}

/**
 * The tables the second version holds otherwise than the first, of the comparison `start` asks Studio for; a sheet
 * Studio compares as a whole, without its tables, is named alone. The comparison is dropped once read.
 */
export async function changedTables(
  client: OpenLClient,
  start: () => Promise<string>,
  extra: ToolHandlerExtra | undefined,
): Promise<ChangedTable[]> {
  const id = await start();
  const comparison = await waitForResult((request) => client.getComparison(id, request), COMPARISON_WAIT, extra)
    .finally(() => client.deleteComparison(id).catch(() => undefined));
  return comparison.sheets.flatMap((sheet) => {
    const tables = (sheet.children ?? []).filter((table) => table.status !== "equal")
      .map((table) => ({ sheet: sheet.name, table: table.name, status: table.status }));
    if (tables.length > 0 || sheet.status === "equal") {
      return tables;
    }
    return [{ sheet: sheet.name, table: "", status: sheet.status }];
  });
}
