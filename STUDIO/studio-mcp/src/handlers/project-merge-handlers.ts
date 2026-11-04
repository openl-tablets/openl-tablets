/**
 * Project-aware branch lifecycle, merge execution, conflict inspection, and the resolution the user decides.
 * Studio keeps the merge conflict state for the credential of the client, so
 * every step of one merge finds it.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { z } from "zod";
import type { OpenLClient } from "../client.js";
import { isHttpError } from "../http.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import type { Truncation } from "../formatters.js";
import { formatListOf } from "../markdown-list.js";
import { gridOf, rowIn, rowsAt, type Grid, type KeyedRows } from "../table-grid.js";
import { downloadLink, inlineText } from "../transfer-links.js";
import { conflictCount, mergedTextOf } from "../text-merge.js";
import { authorOf, headlineOf, isWorkbook } from "../utils.js";
import { registerTool, formatResult, textResult, type ToolResponse } from "./common.js";
import { waitForResult, type WaitOptions } from "./wait.js";

/** How long a merge may run in the background before the tool stops waiting: a large project takes minutes. */
const MERGE_TIMEOUT_MS = 600_000;

/** How the merge tool waits for the merge Studio runs in the background. */
const MERGE_WAIT: WaitOptions = {
  timeoutMs: MERGE_TIMEOUT_MS,
  progressMessage: "The merge is still running…",
  timeoutMessage: `The merge did not finish within ${MERGE_TIMEOUT_MS} ms. It may still be running in Studio: call ` +
    "openl_get_merge_conflicts or open the project later to see how it ended.",
};

/** The conflicted files of the pending merge, by their paths in the repository: the project folder comes first. */
function conflictedFiles(details: Types.ConflictDetailsResponse): string[] {
  return (details.conflictGroups ?? []).flatMap((group) => group.files ?? []);
}

/**
 * A conflicted file by the path the agent names it: as the conflicts list it, or by its path within the project,
 * which an agent takes from the other file tools and Studio does not find.
 */
function conflictedPath(files: string[], path: string): string {
  if (files.length === 0 || files.includes(path)) {
    return path;
  }
  const within = files.filter((file) => file.endsWith(`/${path.replace(/^\/+/, "")}`));
  if (within.length === 1) {
    return within[0];
  }
  throw new ProtocolError(ProtocolErrorCode.InvalidParams,
    `'${path}' is no conflicted file of the pending merge; the conflicted files are ${files.join(", ")}.`);
}

/**
 * A read of a conflicted file, and the path it was read by: a path Studio does not find is looked up in the
 * conflicts, which costs a request only when the agent named the file otherwise than they list it.
 */
async function readConflicted<T>(
  client: OpenLClient,
  projectId: string,
  path: string,
  read: (file: string) => Promise<T>,
): Promise<[string, T]> {
  try {
    return [path, await read(path)];
  } catch (error) {
    if (!isHttpError(error) || error.response?.status !== 404) {
      throw error;
    }
    const file = conflictedPath(conflictedFiles(await client.getMergeConflicts(projectId)), path);
    if (file === path) {
      throw error;
    }
    return [file, await read(file)];
  }
}

/** How many merged lines of a conflicted text file are read whole; a longer run is read by its ends. */
const SHOWN_LINES = 8;
const END_LINES = 3;

interface BranchDeletionSafety {
  baseBranch?: string;
  currentBranch?: string;
  divergenceStatus: "not-checked" | "unmerged" | "up-to-date";
  mergeCheck?: Types.CheckMergeResult;
  projectStatus?: Types.ProjectStatus;
  reason?: string;
  unsavedChanges: boolean;
}

/**
 * The base branch of the repository of a project, or undefined when Studio does not tell it. A failed probe becomes a
 * risk to confirm, not an unguarded delete or a hard failure there is no way past.
 */
async function repositoryBaseBranch(client: OpenLClient, projectId: string): Promise<string | undefined> {
  try {
    return (await client.listProjectBranches(projectId, "repository")).find((candidate) => candidate.base)?.name;
  } catch {
    return undefined;
  }
}

/**
 * Checks whether `branchName` is merged into the base branch of the repository. Studio checks a merge of the current
 * branch of the project, so it tells this only when the project is on `branchName` or on the base branch. Fills in the
 * base branch, the merge check and the divergence status, and the reason not to delete the branch when there is one.
 */
async function checkDivergence(
  client: OpenLClient,
  projectId: string,
  branches: Types.ProjectBranchInfo[],
  branchName: string,
  safety: BranchDeletionSafety,
): Promise<void> {
  const baseBranch = branches.find((candidate) => candidate.base)?.name
    ?? await repositoryBaseBranch(client, projectId);
  safety.baseBranch = baseBranch;
  if (!baseBranch) {
    safety.reason = "Studio did not identify the repository base branch, so divergence cannot be checked.";
    return;
  }
  const current = safety.currentBranch;
  if (current !== branchName && current !== baseBranch) {
    safety.reason = current
      ? `Studio can only compare the project's current branch '${current}', not deletion target '${branchName}'. ` +
        `Open '${branchName}' or base branch '${baseBranch}' with openl_open_project and retry to obtain an ` +
        "authoritative divergence check."
      : `Studio did not report a current branch for deletion target '${branchName}', so divergence cannot be checked.`;
    return;
  }
  // From the deletion target, the merge sends it into the base branch; from the base branch, it receives the target
  const request: Types.MergeRequest = current === branchName
    ? { mode: "send", otherBranch: baseBranch }
    : { mode: "receive", otherBranch: branchName };
  let mergeCheck: Types.CheckMergeResult;
  try {
    mergeCheck = await client.checkProjectMerge(projectId, request);
  } catch {
    safety.reason =
      `Studio could not verify whether branch '${branchName}' is merged into base branch '${baseBranch}'.`;
    return;
  }
  safety.mergeCheck = mergeCheck;
  if (mergeCheck.sourceBranch !== branchName || mergeCheck.targetBranch !== baseBranch) {
    safety.reason =
      `Studio checked branch '${mergeCheck.sourceBranch}' into '${mergeCheck.targetBranch}' instead of ` +
      `'${branchName}' into '${baseBranch}', so deletion-target divergence was not verified.`;
  } else if (mergeCheck.status === "up-to-date") {
    safety.divergenceStatus = "up-to-date";
  } else {
    safety.divergenceStatus = "unmerged";
    safety.reason = `Branch '${branchName}' has commits that are not merged into base branch '${baseBranch}'.` +
      await unmergedRevisions(client, projectId, branchName, baseBranch);
  }
}

/** How many of the newest revisions of the deletion target a refusal names, and of the base branch it compares them with. */
const UNMERGED_SHOWN = 10;
const BASE_REVISIONS_READ = 100;

/**
 * The newest revisions of the project on the deletion target that the base branch does not hold, one line each, for
 * the user to decide on: the merge check tells only that there are some. Revisions are taken until the first one the
 * base branch holds, among its newest {@link BASE_REVISIONS_READ}. Empty when the histories cannot be read, or when
 * the commits the base branch misses change other projects only.
 */
async function unmergedRevisions(
  client: OpenLClient,
  projectId: string,
  branchName: string,
  baseBranch: string,
): Promise<string> {
  const pages = await Promise.all([
    client.getProjectRevisions(projectId, { branch: branchName, offset: 0, limit: UNMERGED_SHOWN }),
    client.getProjectRevisions(projectId, { branch: baseBranch, offset: 0, limit: BASE_REVISIONS_READ }),
  ]).catch(() => undefined);
  if (!pages) {
    return "";
  }
  const [target, base] = pages;
  const merged = new Set(base.content.map((revision) => revision.revisionNo));
  const first = target.content.findIndex((revision) => merged.has(revision.revisionNo));
  const unmerged = first === -1 ? target.content : target.content.slice(0, first);
  if (unmerged.length === 0) {
    return "";
  }
  const lines = unmerged.map((revision) => {
    const author = authorOf(revision);
    const comment = headlineOf(revision);
    return `\n- ${revision.revisionNo} ${revision.createdAt}${author ? ` ${author}` : ""}${comment ? `: ${comment}` : ""}`;
  });
  // A full page the base branch holds none of may go on past it
  const more = unmerged.length === UNMERGED_SHOWN
    ? "\n- … older ones: openl_repository_project_revisions with this branch lists them"
    : "";
  return ` Its revisions of this project that the base branch does not hold:${lines.join("")}${more}`;
}

function mergeRequest(
  args: { mode: Types.MergeMode; otherBranch: string },
): Types.MergeRequest {
  return { mode: args.mode, otherBranch: args.otherBranch };
}

/**
 * A cell as an agent reads it: its value, its formula, its comment and its merging, without its formatting, and
 * without its A1 address, which its place in the grid and the range of its version give.
 */
function cellOf(cell: Types.ConflictWorkbookCell): Record<string, unknown> {
  if (cell.covered) return { covered: true };
  return {
    value: cell.value ?? null,
    ...(cell.formula ? { formula: cell.formula } : {}),
    ...(cell.comment ? { comment: cell.comment } : {}),
    ...(cell.colspan ? { colspan: cell.colspan } : {}),
    ...(cell.rowspan ? { rowspan: cell.rowspan } : {}),
  };
}

/** What a cell of a merged table both branches changed differently holds with either version. */
function conflictCellsOf(merged: Types.ConflictTableMerge): Record<string, unknown>[] {
  return merged.conflicts.map((conflict) => ({
    row: conflict.row,
    column: conflict.column,
    cell: conflict.cell,
    // The address of the cell in either version, only where the version moved it
    ...(conflict.ourCell === conflict.cell ? {} : { ourCell: conflict.ourCell }),
    ...(conflict.theirCell === conflict.cell ? {} : { theirCell: conflict.theirCell }),
    ours: cellOf(conflict.ours),
    theirs: cellOf(conflict.theirs),
  }));
}

/** The rows of a version of a conflicting table an answer holds: every row Studio sent, or the rows of the conflict. */
type ConflictRows = Grid | (KeyedRows & { rowsNotSent?: number[] });

/** The rows of a conflicting table that name it and its columns, kept beside the rows of the conflict. */
const CONFLICT_HEADER_ROWS = 2;

/**
 * The rows of one version of a conflicting table that tell the conflict, in the shape openl_get_table reads rows found:
 * the header rows and the rows holding one of `addresses`, A1 cells of the sheet, keyed by their positions in the
 * version, without the formatting of the cells. Studio sends the first 200 rows of a version: the rows of the conflict
 * past them are named in `rowsNotSent`, their cells being in the conflicts alone. A version that names no such cell
 * answers every row sent. One conflicting cell of a lookup of hundreds of rows was four versions of every row sent.
 */
function conflictRowsOf(cells: Types.ConflictWorkbookCell[][], range: string, addresses: string[]): ConflictRows {
  const source = cells.map((row) => row.map(({ style: _style, runs: _runs, type: _type, ...cell }) => cell as Types.RawTableCell));
  const grid = gridOf(source, 0, range);
  const rows = new Set(addresses.flatMap((address) => {
    const row = rowIn(address, range);
    return row !== undefined && row >= CONFLICT_HEADER_ROWS ? [row] : [];
  }));
  if (rows.size === 0) {
    return grid;
  }
  const notSent = [...rows].filter((row) => row >= cells.length).toSorted((a, b) => a - b);
  return {
    ...rowsAt(grid, cells.keys().filter((row) => row < CONFLICT_HEADER_ROWS || rows.has(row)).toArray()),
    ...(notSent.length > 0 ? { rowsNotSent: notSent } : {}),
  };
}

/** A version of a conflicting table, or the merged table, with the rows of its changed cells and of `cells`. */
function tableRowsOf(
  table: Pick<Types.ConflictTableVersion, "range" | "rows" | "cells" | "changedCells">,
  cells: string[],
): ConflictRows & { range: string; totalRows: number; changedCells: string[] } {
  const changedCells = table.changedCells ?? [];
  return {
    range: table.range,
    totalRows: table.rows,
    changedCells,
    ...conflictRowsOf(table.cells, table.range, [...changedCells, ...cells]),
  };
}

/** A version of a conflicting table with the rows of its changed and conflicting cells, and of the cells `also` names. */
function versionOf(version: Types.ConflictTableVersion | undefined, also: string[]): Record<string, unknown> | undefined {
  if (!version) {
    return undefined;
  }
  const conflictCells = version.conflictCells ?? [];
  return {
    conflictCells,
    formattingChanged: version.formattingChanged ?? false,
    ...tableRowsOf(version, [...conflictCells, ...also]),
  };
}

/** The sheets of a conflicted workbook and their conflicting tables, without the cells of the tables. */
function workbookOverview(file: string, workbook: Types.ConflictWorkbookView): Record<string, unknown> {
  return {
    file,
    palette: workbook.palette,
    sheets: workbook.sheets.map((sheet) => ({
      name: sheet.name,
      status: sheet.status,
      ...(sheet.reason ? { reason: sheet.reason } : {}),
      inVersions: { ours: sheet.ours, base: sheet.base, theirs: sheet.theirs },
      tables: sheet.tables.map((table) => ({
        id: table.id,
        name: table.name,
        reason: table.reason,
        byCells: Boolean(table.merged),
        ranges: {
          ...(table.ours ? { ours: table.ours.range } : {}),
          ...(table.base ? { base: table.base.range } : {}),
          ...(table.theirs ? { theirs: table.theirs.range } : {}),
          ...(table.merged ? { merged: table.merged.range } : {}),
        },
        ...(table.merged ? { conflictCells: conflictCellsOf(table.merged) } : {}),
      })),
    })),
  };
}

/** One conflicting table of a conflicted workbook in every version, and merged cell by cell when it merges so. */
function workbookTable(file: string, workbook: Types.ConflictWorkbookView, id: string): Record<string, unknown> {
  for (const sheet of workbook.sheets) {
    const table = sheet.tables.find((candidate) => candidate.id === id);
    if (table) {
      const conflicts = table.merged?.conflicts ?? [];
      return {
        file,
        sheet: sheet.name,
        id: table.id,
        name: table.name,
        reason: table.reason,
        byCells: Boolean(table.merged),
        // A version without the table, and a table not merged cell by cell, are left out of the answer. Each version
        // keeps the rows of the conflicting cells at its own address of them, which rows a branch inserted move
        ours: versionOf(table.ours, conflicts.map((conflict) => conflict.ourCell)),
        base: versionOf(table.base, conflicts.map((conflict) => conflict.cell)),
        theirs: versionOf(table.theirs, conflicts.map((conflict) => conflict.theirCell)),
        merged: table.merged && {
          ...tableRowsOf(table.merged, conflicts.map((conflict) => conflict.cell)),
          conflicts: conflictCellsOf(table.merged),
        },
      };
    }
  }
  throw new ProtocolError(
    ProtocolErrorCode.InvalidParams,
    `'${id}' is no conflicting table of '${file}'. Call openl_read_merge_conflict without table to list them.`,
  );
}

/**
 * The lines of a merged part of a conflicted text file: all of them, or the first and the last lines of a long run,
 * with how many lines between them are left out.
 */
function mergedLinesOf(text: string): Record<string, unknown> {
  const lines = text.split(/(?<=\n)/);
  if (lines.length <= SHOWN_LINES) return { text };
  return {
    head: lines.slice(0, END_LINES).join(""),
    omittedLines: lines.length - 2 * END_LINES,
    tail: lines.slice(-END_LINES).join(""),
  };
}

/**
 * A conflicted text file as an agent reads it: the merged lines around the conflicts, and each conflict numbered.
 * `fromConflict` starts it at a conflict, with the merged lines before it, to read the rest of a file one answer
 * could not hold.
 */
function textOverview(
  file: string,
  blocks: Types.ConflictTextBlock[],
  fromConflict = 0,
): { file: string; conflicts: number; blocks: Array<Record<string, unknown>> } {
  let conflict = 0;
  const all = blocks.map((block) => block.kind === "merged"
    ? { kind: "merged", ...mergedLinesOf(block.text) }
    : { kind: "conflict", conflict: conflict++, ours: block.ours, base: block.base, theirs: block.theirs });
  const at = all.findIndex((block) => block.conflict === fromConflict);
  if (fromConflict > 0 && at < 0) {
    throw new ProtocolError(ProtocolErrorCode.InvalidParams,
      `'${file}' has ${conflict} conflicts, numbered from 0: there is no conflict ${fromConflict}.`);
  }
  return { file, conflicts: conflict, blocks: all.slice(fromConflict > 0 ? Math.max(at - 1, 0) : 0) };
}

/** The cut of a conflicted text over the size limit of an answer: its leading blocks, continued at a conflict. */
function textTruncation(overview: ReturnType<typeof textOverview>): Truncation {
  const { blocks } = overview;
  return {
    items: blocks.length,
    unit: "blocks",
    cut: (count) => ({ ...overview, blocks: blocks.slice(0, count) }),
    next: (count) => {
      const next = blocks.slice(count).find((block) => block.kind === "conflict");
      return next ? `continue with fromConflict=${String(next.conflict)}` : "the rest holds no conflict";
    },
  };
}

/** A conflicted text file with each conflict resolved as decided: one decision for every conflict, in their order. */
async function mergedText(
  client: OpenLClient,
  projectId: string,
  file: string,
  decisions: Types.ConflictTextDecision[],
): Promise<string> {
  const { blocks } = await client.getMergeConflictText(projectId, file);
  const conflicts = conflictCount(blocks);
  if (decisions.length !== conflicts) {
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `'${file}' has ${conflicts} conflicts, but ${decisions.length} decisions were given. Give one decision per ` +
        "conflict of openl_read_merge_conflict, in their order.",
    );
  }
  return mergedTextOf(blocks, decisions);
}

/** How a conflicted file is resolved: taken from a version, or merged into the content of the file as decided. */
async function fileResolution(
  client: OpenLClient,
  projectId: string,
  resolution: z.output<typeof schemas.resolveMergeConflictsSchema>["resolutions"][number],
): Promise<Types.FileConflictResolution> {
  if ("strategy" in resolution) {
    return { file: resolution.path, strategy: resolution.strategy };
  }
  const content = "decisions" in resolution
    ? await client.mergeConflictWorkbook(projectId, resolution.path, resolution.decisions)
    : await mergedText(client, projectId, resolution.path, resolution.conflicts);
  if ((typeof content === "string" ? content.length : content.byteLength) === 0) {
    // Studio takes no empty upload as the content of a file
    throw new ProtocolError(
      ProtocolErrorCode.InvalidParams,
      `'${resolution.path}' as decided would be empty, which Studio cannot store as merged content. Keep a line, or ` +
        "take a version with a strategy: one fileAvailability marks false deletes the file.",
    );
  }
  return { file: resolution.path, strategy: "CUSTOM", content };
}

/**
 * One whole version of a conflicted file: a text inline, verbatim, when it is short, else a download link. A version
 * fileAvailability marks false does not hold the file, such as the base of a file both branches added.
 */
async function conflictVersion(
  client: OpenLClient,
  projectId: string,
  file: string,
  version: "BASE" | "OURS" | "THEIRS",
): Promise<ToolResponse> {
  const side = version.toLowerCase() as "base" | "ours" | "theirs";
  const conflicts = await client.getMergeConflicts(projectId);
  const path = conflictedPath(conflictedFiles(conflicts), file);
  const branch = side === "base" ? undefined : conflicts[`${side}Revision`]?.branch;
  const link = { kind: "conflictVersion", projectId, path, side, branch } as const;
  const available = conflicts.fileAvailability?.[path];
  if (available && !available[side]) {
    throw new ProtocolError(ProtocolErrorCode.InvalidParams,
      `The ${version} version of '${path}' does not hold the file: fileAvailability marks it false.`);
  }
  if (available && !available.text) {
    return formatResult(await downloadLink(client, link));
  }
  const response = await client.readMergeConflictFile(projectId, path, version);
  const text = inlineText(response);
  return text === undefined ? formatResult(await downloadLink(client, link, response.data.length)) : textResult(text);
}

export function registerProjectMergeHandlers(): void {
  registerTool({
    name: "list_project_branches",
    title: "List Project Branches",
    description:
      "List the branches of the repository hosting a project with their base and protected flags, the base branch first, then the protected ones; unlike openl_list_branches, it is project-aware.",
    schema: schemas.listProjectBranchesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const branches = await client.listProjectBranches(args.projectId, args.scope);
      const fragment = args.name?.toLowerCase();
      const matching = fragment === undefined
        ? branches
        : branches.filter((branch) => branch.name?.toLowerCase().includes(fragment));
      // The base branch and the protected ones lead, as the branches a merge or a deletion is checked against
      const ordered = matching.toSorted((a, b) => branchRank(a) - branchRank(b));
      return textResult(formatListOf(ordered, args.limit, args.offset, {
        noun: "branches", one: "branch",
        columns: [
          { header: "branch", value: (branch) => branch.name },
          { header: "flags", value: (branch) => [...(branch.base ? ["base"] : []), ...(branch.protected ? ["protected"] : [])] },
        ],
      }));
    },
  });

  registerTool({
    name: "check_project_merge",
    title: "Check Project Merge",
    description:
      "Check a merge without changing Git: 'mergeable' means the source has changes the target lacks; 'up-to-date' means the target holds the source. canMerge tells whether Studio allows the attempt after permission, protection and lock checks, and scope what the merge brings in: every project the source changed, its file count, the files both branches changed, and conflicts: those that may conflict, estimated without merging.",
    schema: schemas.checkProjectMergeSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const result = await client.checkProjectMerge(args.projectId, mergeRequest(args));
      return formatResult(result);
    },
  });

  registerTool({
    name: "merge_project_branches",
    title: "Merge Project Branches",
    description:
      "Merge one branch of a project into another, after checking it: an up-to-date target answers without a write, and a blocker fails first. It waits up to 10 minutes. Conflicts answer status 'conflicts' and stay pending for this client: present them to the user, who resolves them in Studio or decides how openl_resolve_merge_conflicts does; cancel them when the user takes over. Never choose OURS or THEIRS yourself. The summary of a success lists every project the source changed with its files, and the target revisions before and after.",
    schema: schemas.mergeProjectBranchesSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const request = mergeRequest(args);
      const check = await client.checkProjectMerge(args.projectId, request);

      if (check.status === "up-to-date") {
        return formatResult({ status: "up-to-date", check });
      }
      const forceBypassAllowed = args.force === true && check.blockedBy === "bypass-required";
      if (args.force === true && !forceBypassAllowed) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          "force=true is allowed only when Studio reports blockedBy='bypass-required'. Run openl_check_project_merge again without force.",
        );
      }
      if ((check.canMerge === false || check.blockedBy != null) && !forceBypassAllowed) {
        const blocker = check.blockedBy ? ` by '${check.blockedBy}'` : "";
        throw new ProtocolError(
          ProtocolErrorCode.InvalidRequest,
          `Merge is blocked${blocker}. Resolve the blocker and run openl_check_project_merge again.`,
        );
      }

      await client.mergeProjectBranches(args.projectId, request, args.force === true);
      const result = await waitForResult(
        (request) => client.getMergeResult(args.projectId, request),
        MERGE_WAIT,
        extra,
      );
      // The answer of a success holds what it changed; the check before it holds nothing new
      const response = {
        ...result,
        ...(result.status === "conflicts"
          ? { nextAction: "Show the conflicts to the user: openl_get_merge_conflicts lists them; resolve each only as the user decides." }
          : {}),
      };
      return formatResult(response);
    },
  });

  registerTool({
    name: "get_merge_conflicts",
    title: "Get Merge Conflicts",
    description:
      "Get the pending merge conflicts of this client, after openl_merge_project_branches answered 'conflicts': the conflicted files in groups, fileAvailability (the versions holding each file, and whether it is UTF-8 text), the BASE, OURS and THEIRS revisions and the default commit message. Present them to the user, who decides how each file is resolved: in Studio, or through openl_resolve_merge_conflicts.",
    schema: schemas.projectOnlySchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const conflicts = await client.getMergeConflicts(args.projectId);
      return formatResult(conflicts);
    },
  });

  registerTool({
    name: "read_merge_conflict",
    title: "Read Merge Conflict",
    description:
      "Read a conflicted file of the pending merge as evidence for the user's decision. A workbook all three versions hold: without table, the sheets a branch changed and the conflicting tables, a table with byCells: true with its conflictCells, each by its row and column in the merged table; with table, that table in your, the base and their version and merged, the header and the rows of changed cells only, keyed by position. A text file: the merged lines and each conflict, numbered from 0, with your, the base and their lines. side reads one whole version, a short text inline, else by a download link; a workbook both branches added has no base and takes a strategy.",
    schema: schemas.readMergeConflictSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      if (args.side) {
        return conflictVersion(client, args.projectId, args.path, args.side);
      }
      if (isWorkbook(args.path)) {
        const [path, workbook] = await readConflicted(client, args.projectId, args.path,
          (file) => client.getMergeConflictWorkbook(args.projectId, file));
        return formatResult(args.table ? workbookTable(path, workbook, args.table) : workbookOverview(path, workbook));
      }
      const [path, { blocks }] = await readConflicted(client, args.projectId, args.path,
        (file) => client.getMergeConflictText(args.projectId, file));
      const overview = textOverview(path, blocks, args.fromConflict);
      return formatResult(overview, { truncation: textTruncation(overview) });
    },
  });

  registerTool({
    name: "resolve_merge_conflicts",
    title: "Resolve Merge Conflicts",
    description:
      "Resolve the pending merge conflicts as the user decided and complete the merge, or the save that found them, in one commit: one resolution per conflicted file, by strategy (the whole file from a version), conflicts (a text file) or decisions (a workbook), as openl_read_merge_conflict reads them. Never choose a side, lines or a value the user did not decide. Example: {\"projectId\":\"…\",\"resolutions\":[{\"path\":\"rules/Rates.xlsx\",\"decisions\":[{\"id\":\"…\",\"side\":\"theirs\"}]}]}. A refused call leaves the conflicts pending.",
    schema: schemas.resolveMergeConflictsSchema,
    annotations: {
      destructiveHint: true,
      idempotentHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const files = conflictedFiles(await client.getMergeConflicts(args.projectId));
      const decided = args.resolutions.map((resolution) => ({ ...resolution, path: conflictedPath(files, resolution.path) }));
      const named = new Set(decided.map((resolution) => resolution.path));
      const missing = files.filter((file) => !named.has(file));
      if (missing.length > 0) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Every conflicted file needs a resolution; missing: ${missing.join(", ")}.`,
        );
      }
      // Each file merged as decided is merged on its own, side by side, into its content.
      const resolutions = await Promise.all(decided.map(
        (resolution) => fileResolution(client, args.projectId, resolution)));
      const response = await client.resolveMergeConflicts(args.projectId, resolutions, args.message);
      return formatResult(response);
    },
  });

  registerTool({
    name: "cancel_merge_conflicts",
    title: "Clear Pending Merge Conflicts",
    description:
      "Abort the pending merge: discard its conflicts and stop a merge still running. Files and branches stay as they are; merging again recreates the conflicts.",
    schema: schemas.projectOnlySchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.cancelMergeConflicts(args.projectId);
      return formatResult({ cancelled: true });
    },
  });

  registerTool({
    name: "delete_project",
    title: "Delete Project",
    description:
      "Permanently delete a project, with a deletion commit where the repository makes one. On a repository with branches it deletes the project on its current branch only, answered as deletedOnBranch: other branches keep it.",
    schema: schemas.deleteProjectSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const project = await client.getProject(args.projectId);
      if (args.confirmProjectName !== project.name) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Deletion confirmation does not match. Expected confirmProjectName='${project.name}'. ` +
            "No project was deleted.",
        );
      }
      await client.deleteProject(args.projectId, args.comment);
      // Studio deletes the project on the branch it is on, which the agent must not take for every branch
      return formatResult(project.branch ? { deletedOnBranch: project.branch } : { deleted: true });
    },
  });

  registerTool({
    name: "delete_project_branch",
    title: "Delete Project Branch",
    description:
      "Delete a branch of the repository hosting a project, once it is merged into the base branch; a refusal names the risk and the argument that accepts it, which needs the user's approval. Studio closes the project first when it is open on the branch.",
    schema: schemas.deleteProjectBranchSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const branches = await client.listProjectBranches(args.projectId);
      const branch = branches.find((candidate) => candidate.name === args.branch);
      if (!branch) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Branch '${args.branch}' is not available to this project. Call openl_list_project_branches again.`,
        );
      }
      if (branch.base) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Branch '${args.branch}' is the repository base branch and cannot be deleted.`,
        );
      }
      if (branch.protected && args.force !== true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Branch '${args.branch}' is protected. If an eligible manager explicitly approves the bypass, call again with force=true.`,
        );
      }

      const project = await client.getProject(args.projectId);
      const safety: BranchDeletionSafety = {
        currentBranch: project.branch,
        divergenceStatus: "not-checked",
        projectStatus: project.status,
        unsavedChanges: project.branch === branch.name && project.status === "EDITING",
      };

      if (safety.unsavedChanges) {
        safety.reason = `Branch '${branch.name}' has unsaved working-copy changes that deletion will discard.`;
      } else {
        await checkDivergence(client, args.projectId, branches, args.branch, safety);
      }

      if (safety.reason && args.confirmDataLoss !== true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidRequest,
          // The revisions a reason lists end it on a line of their own
          `${safety.reason}${safety.reason.includes("\n") ? "\n" : " "}No branch was deleted. Review the risk, then either make the branch safe ` +
            "or call again with confirmDataLoss=true to explicitly accept possible data loss.",
        );
      }

      await client.deleteProjectBranch(args.projectId, args.branch, args.force === true);
      // A risky delete reaches this point only with confirmDataLoss=true, the risk it accepted in reason
      return formatResult({
        deleted: args.branch,
        divergenceStatus: safety.divergenceStatus,
        ...(safety.baseBranch ? { baseBranch: safety.baseBranch } : {}),
        ...(args.force === true ? { forced: true } : {}),
        ...(safety.reason === undefined ? {} : { dataLossConfirmed: true, reason: safety.reason }),
      });
    },
  });
}

/** The order of a branch in a list: the base branch, then the protected ones, then the rest. */
function branchRank(branch: Types.ProjectBranchInfo): number {
  if (branch.base) {
    return 0;
  }
  return branch.protected ? 1 : 2;
}
