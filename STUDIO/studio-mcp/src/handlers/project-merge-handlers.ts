/**
 * Project-aware branch lifecycle, merge execution, conflict inspection, and the resolution the user decides.
 * Studio keeps the merge conflict state for the credential of the client, so
 * every step of one merge finds it.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { z } from "zod";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { normalizeMimeType, sliceContent } from "../content-utils.js";
import { conflictCount, mergedTextOf } from "../text-merge.js";
import { registerTool, formatResult, type ToolResponse } from "./common.js";
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

const CONFLICT_FILE_CHUNK_SIZE = 16_000;

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
 * Checks whether `branchName`, the current branch of the project, is merged into the base branch of the repository.
 * Fills in the base branch, the merge check and the divergence status, and the reason not to delete the branch when
 * there is one.
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
  let mergeCheck: Types.CheckMergeResult;
  try {
    mergeCheck = await client.checkProjectMerge(projectId, { mode: "send", otherBranch: baseBranch });
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
    safety.reason = `Branch '${branchName}' has commits that are not merged into base branch '${baseBranch}'.`;
  }
}

function mergeRequest(
  args: { mode: Types.MergeMode; otherBranch: string },
): Types.MergeRequest {
  return { mode: args.mode, otherBranch: args.otherBranch };
}

/** A cell as an agent reads it: its value, its formula, its comment and its merging, without its formatting. */
function cellOf(cell: Types.ConflictWorkbookCell): Record<string, unknown> {
  if (cell.covered) return { covered: true };
  return {
    ...(cell.cell ? { cell: cell.cell } : {}),
    value: cell.value ?? null,
    ...(cell.formula ? { formula: cell.formula } : {}),
    ...(cell.comment ? { comment: cell.comment } : {}),
    ...(cell.colspan ? { colspan: cell.colspan } : {}),
    ...(cell.rowspan ? { rowspan: cell.rowspan } : {}),
  };
}

function cellsOf(cells: Types.ConflictWorkbookCell[][]): Record<string, unknown>[][] {
  return cells.map((row) => row.map(cellOf));
}

/** What a cell of a merged table both branches changed differently holds with either version. */
function conflictCellsOf(merged: Types.ConflictTableMerge): Record<string, unknown>[] {
  return merged.conflicts.map((conflict) => ({
    row: conflict.row,
    column: conflict.column,
    cell: conflict.cell,
    ourCell: conflict.ourCell,
    theirCell: conflict.theirCell,
    ours: cellOf(conflict.ours),
    theirs: cellOf(conflict.theirs),
  }));
}

function versionOf(version: Types.ConflictTableVersion | undefined): Record<string, unknown> | undefined {
  return version && {
    range: version.range,
    rows: version.rows,
    changedCells: version.changedCells ?? [],
    conflictCells: version.conflictCells ?? [],
    formattingChanged: version.formattingChanged ?? false,
    cells: cellsOf(version.cells),
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
      return {
        file,
        sheet: sheet.name,
        id: table.id,
        name: table.name,
        reason: table.reason,
        byCells: Boolean(table.merged),
        // A version without the table, and a table not merged cell by cell, are left out of the answer.
        ours: versionOf(table.ours),
        base: versionOf(table.base),
        theirs: versionOf(table.theirs),
        merged: table.merged && {
          range: table.merged.range,
          rows: table.merged.rows,
          changedCells: table.merged.changedCells ?? [],
          cells: cellsOf(table.merged.cells),
          conflicts: conflictCellsOf(table.merged),
        },
      };
    }
  }
  throw new ProtocolError(
    ProtocolErrorCode.InvalidParams,
    `'${id}' is no conflicting table of '${file}'. Call openl_get_merge_conflict_workbook without table to list them.`,
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

/** A conflicted text file as an agent reads it: the merged lines around the conflicts, and each conflict numbered. */
function textOverview(file: string, blocks: Types.ConflictTextBlock[]): Record<string, unknown> {
  let conflict = 0;
  return {
    file,
    conflicts: conflictCount(blocks),
    blocks: blocks.map((block) => block.kind === "merged"
      ? { kind: "merged", ...mergedLinesOf(block.text) }
      : { kind: "conflict", conflict: conflict++, ours: block.ours, base: block.base, theirs: block.theirs }),
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
        "conflict of openl_get_merge_conflict_text, in their order.",
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
    return { file: resolution.file, strategy: resolution.strategy };
  }
  const content = "decisions" in resolution
    ? await client.mergeConflictWorkbook(projectId, resolution.file, resolution.decisions)
    : await mergedText(client, projectId, resolution.file, resolution.conflicts);
  return { file: resolution.file, strategy: "CUSTOM", content };
}

export function registerProjectMergeHandlers(): void {
  registerTool({
    name: "list_project_branches",
    title: "List Project Branches",
    description:
      "List branch metadata in the repository that hosts a project, including repository base and protected flags. scope='project' (default) returns branches that already hold the project and are candidates for switching or deletion. Inspect the flags before deletion: the base branch cannot be deleted, while a protected branch requires force and explicit confirmation. scope='repository' returns every repository branch, including branches that do not hold the project yet; use it to discover otherBranch values before openl_check_project_merge or openl_merge_project_branches. Unlike openl_list_branches, this tool is project-aware and returns safety metadata.",
    schema: schemas.listProjectBranchesSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const branches = await client.listProjectBranches(args.projectId, args.scope);
      return formatResult(branches, args.response_format);
    },
  });

  registerTool({
    name: "check_project_merge",
    title: "Check Project Merge",
    description:
      "Check merge direction, branch relationship, permissions, and blockers without changing Git. This is not a conflict preview: status='mergeable' means the source has changes absent from the target, not that the merge is conflict-free; conflicts are discovered only by openl_merge_project_branches. status='up-to-date' means the target already contains the source. canMerge reports whether Studio permits the attempt after permission, protection, and lock checks. scope tells what the merge would bring in: every project the source changed, how many of its files, and the files both branches changed, which the merge combines or reports as conflicts. mode='receive' merges otherBranch into the project's current branch; mode='send' merges the current branch into otherBranch. Discover merge targets with openl_list_project_branches(scope='repository'), because a valid target may not hold the project yet.",
    schema: schemas.checkProjectMergeSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const result = await client.checkProjectMerge(args.projectId, mergeRequest(args));
      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "merge_project_branches",
    title: "Merge Project Branches",
    description:
      "Attempt to merge project branches. The tool first repeats the relationship and permission precheck: up-to-date returns without a write, and permission/lock blockers fail before merge. The precheck does not predict conflicts, so an allowed attempt can still return status='conflicts'. Studio runs the merge in the background; the tool waits until it ends, for up to 10 minutes. mode='receive' merges otherBranch into the current branch; mode='send' merges the current branch into otherBranch. The merge brings in every project the source branch changed, not only this one: summary of a success lists the projects and files it changed. A conflict result creates conflict state Studio keeps for this client: inspect it on this same MCP server and present it to the user, who resolves it in Studio or decides how openl_resolve_merge_conflicts resolves it; cancel the pending state when the user takes over. Never choose OURS or THEIRS automatically. force is only for an eligible protected-target bypass and requires confirmForce=true.",
    schema: schemas.mergeProjectBranchesSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const request = mergeRequest(args);
      const check = await client.checkProjectMerge(args.projectId, request);

      if (check.status === "up-to-date") {
        return formatResult({ success: true, status: "up-to-date", check }, args.response_format);
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
      const response = {
        success: result.status === "success",
        ...result,
        ...(result.status === "conflicts"
          ? {
              nextAction:
                "The relationship precheck does not predict conflicts; the actual merge found conflicts Studio keeps for this client. Call openl_get_merge_conflicts on this same MCP server, inspect BASE/OURS/THEIRS with openl_read_merge_conflict_file, a conflicted text file line by line with openl_get_merge_conflict_text and a conflicted workbook with openl_get_merge_conflict_workbook. Do not choose a side on your own: present the conflicts to the user, and resolve them with openl_resolve_merge_conflicts only as the user decides, or leave the resolution to the user in Studio and call openl_cancel_merge_conflicts to clear this pending MCP session state.",
            }
          : { check }),
      };
      return formatResult(response, args.response_format);
    },
  });

  registerTool({
    name: "get_merge_conflicts",
    title: "Get Merge Conflicts",
    description:
      "Get the pending merge conflicts Studio keeps for this client: grouped file paths, which versions hold each file and whether it is UTF-8 text (fileAvailability), BASE/OURS/THEIRS revision details, and the default merge commit message. Present them to the user, who decides how each file is resolved: in Studio, or through openl_resolve_merge_conflicts with the decisions the user made. Available only after openl_merge_project_branches returns status='conflicts'; use the same MCP server instance throughout inspection and resolution.",
    schema: schemas.projectOnlySchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const conflicts = await client.getMergeConflicts(args.projectId);
      return formatResult(conflicts, args.response_format);
    },
  });

  registerTool({
    name: "read_merge_conflict_file",
    title: "Read Merge Conflict File Version",
    description:
      "Read one BASE, OURS, or THEIRS version of a conflicted file from the current session as evidence for the resolution the user decides. Never infer or apply a winning side on your own; a conflicted text file reads line by line with openl_get_merge_conflict_text, a conflicted workbook table by table with openl_get_merge_conflict_workbook. A version fileAvailability marks false does not hold the file, such as the base of a file both branches added. UTF-8 and base64 binary chunks are returned in a JSON text envelope with MIME and byte-range metadata. Both forms include nextOffset while more data remains. length targets at most 16000 bytes; a UTF-8 chunk may include up to 3 extra bytes to finish its last character. The backend still downloads the whole file before this client-side slice. Use the exact file path from openl_get_merge_conflicts.",
    schema: schemas.readMergeConflictFileSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const response = await client.readMergeConflictFile(args.projectId, args.file, args.side);
      const total = response.data.length;
      const { start, end, slice, asBase64 } = sliceContent(response.data, {
        offset: args.offset,
        length: args.length ?? CONFLICT_FILE_CHUNK_SIZE,
        encoding: args.encoding,
      });
      const metadata = {
        file: args.file,
        side: args.side,
        ...(response.contentType ? { contentType: response.contentType } : {}),
        mimeType: normalizeMimeType(response.contentType),
        ...(response.contentDisposition ? { contentDisposition: response.contentDisposition } : {}),
        encoding: asBase64 ? "base64" : "utf-8",
        byteLength: total,
        offset: start,
        returnedBytes: slice.length,
        hasMore: end < total,
        ...(end < total ? { nextOffset: end } : {}),
      };

      // Use TextContent for arbitrary binary data too. Some MCP clients send every
      // embedded blob to an image decoder and reject valid XLSX/ZIP bytes.
      const envelope = { ...metadata, content: slice.toString(asBase64 ? "base64" : "utf-8") };
      return formatResult(envelope, "json", { skipTruncation: true });
    },
  });

  registerTool({
    name: "get_merge_conflict_workbook",
    title: "Get Merge Conflict Workbook",
    description:
      "Read the conflicts of a conflicted Excel file of the pending merge, as Studio merges it table by table: a file that your, the base and their version all hold, as fileAvailability of openl_get_merge_conflicts tells. A workbook both branches added has no base version and is resolved whole, with a strategy. Without table: every sheet either branch changed (status ours/theirs/merged/conflict; a conflicting sheet with a reason is resolved only as a whole) and every conflicting table, its id, reason and ranges; a table with byCells: true is merged cell by cell — the cells only one branch changed are merged by Studio — and lists its conflictCells, each with row and column in the merged table (from 0), the cell your and their version have it in (ourCell, theirCell) and what your (ours) and their (theirs) version holds there. With table: that table's cells in your, the base and their version, and in the merged table, whose conflicting cells hold the base value until decided and whose changedCells Studio took from the one branch that changed them. Present the conflicts to the user; resolve with openl_resolve_merge_conflicts only as the user decides.",
    schema: schemas.getMergeConflictWorkbookSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const workbook = await client.getMergeConflictWorkbook(args.projectId, args.file);
      const data = args.table ? workbookTable(args.file, workbook, args.table) : workbookOverview(args.file, workbook);
      return formatResult(data, args.response_format);
    },
  });

  registerTool({
    name: "get_merge_conflict_text",
    title: "Get Merge Conflict Text",
    description:
      "Read a conflicted text file of the pending merge as Studio merges it line by line, as the Merge line by line window of Studio shows it: a file openl_get_merge_conflicts lists with fileAvailability text: true that your and their version both hold. The parts of the file come in their order: the lines the merge took by itself (kind merged: a run of more than 8 lines reads as its first and last 3 lines, head and tail, with omittedLines between them), and each conflict (kind conflict) numbered from 0, with the lines of your (ours), the base and their (theirs) version, every text with its line breaks. Each version holds every line its branch has in that place, so taking both keeps each whole. Present the conflicts to the user; resolve with openl_resolve_merge_conflicts only as the user decides.",
    schema: schemas.getMergeConflictTextSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { blocks } = await client.getMergeConflictText(args.projectId, args.file);
      return formatResult(textOverview(args.file, blocks), args.response_format);
    },
  });

  registerTool({
    name: "resolve_merge_conflicts",
    title: "Resolve Merge Conflicts",
    description:
      "Resolve the pending merge conflicts as the user decided, and complete the merge (or the save that found them) with one commit. Give one resolution per conflicted file of openl_get_merge_conflicts: a strategy (OURS, THEIRS, BASE) takes the whole file from a version; conflicts merge a conflicted text file as openl_get_merge_conflict_text reads it, keeping the lines the merge took by itself — one decision per conflict, in their order: a version ({side: ours, theirs or base}), both versions whole ({side: ours-theirs or theirs-ours}), or lines written in its place ({lines}); decisions merge a conflicted workbook as openl_get_merge_conflict_workbook lists it — a table or a sheet taken whole from a version ({id, side}), or a table with byCells: true merged cell by cell ({id, cells}): every conflicting cell needs a side, from the cell itself or the whole table, or a typed value, and any cell may take a typed value. A text file and a workbook merged as decided are sent as the CUSTOM file. Never choose a side or type lines or a value the user did not decide or approve. A refused decision leaves the conflict pending, so the call can be repeated.",
    schema: schemas.resolveMergeConflictsSchema,
    annotations: {
      destructiveHint: true,
      idempotentHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const details = await client.getMergeConflicts(args.projectId);
      const named = new Set(args.resolutions.map((resolution) => resolution.file));
      const missing = (details.conflictGroups ?? [])
        .flatMap((group) => group.files ?? [])
        .filter((file) => !named.has(file));
      if (missing.length > 0) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Every conflicted file needs a resolution; missing: ${missing.join(", ")}.`,
        );
      }
      // Each file merged as decided is merged on its own, side by side, into its content.
      const resolutions = await Promise.all(args.resolutions.map(
        (resolution) => fileResolution(client, args.projectId, resolution)));
      const response = await client.resolveMergeConflicts(args.projectId, resolutions, args.message);
      return formatResult(response, args.response_format);
    },
  });

  registerTool({
    name: "cancel_merge_conflicts",
    title: "Clear Pending Merge Conflicts",
    description:
      "Abort the pending merge-conflict workflow by clearing its session state. This does not modify files or branches, but the stored conflict analysis is discarded; run the merge again to recreate it.",
    schema: schemas.projectOnlySchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.cancelMergeConflicts(args.projectId);
      return formatResult({ success: true, message: "Pending merge conflict state was cleared." }, args.response_format);
    },
  });

  registerTool({
    name: "delete_project",
    title: "Delete Project",
    description:
      "Permanently delete a project through Studio. This may create a deletion commit in its design repository. Safety guard: confirmProjectName is required and must exactly match the project's current backend name; the handler reads the project immediately before deletion. An optional comment becomes the deletion commit message.",
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
      return formatResult({
        success: true,
        projectId: args.projectId,
        projectName: project.name,
        message: `Deleted project '${project.name}'.`,
      }, args.response_format);
    },
  });

  registerTool({
    name: "delete_project_branch",
    title: "Delete Project Branch",
    description:
      "Delete a branch from the repository hosting a project. The tool first reads project-aware branch metadata: a base branch is always rejected, and a protected branch requires force=true plus confirmForce=true (and eligible Studio permissions). It then performs a safe-delete preflight against the repository base branch when the target is the project's current branch. A branch with commits absent from base, unsaved changes, or no authoritative divergence check is rejected unless confirmDataLoss=true explicitly acknowledges the reported risk. confirmBranchName must exactly equal branch. If the project is open on the deleted branch, Studio closes it first. Branch names containing '/' are supported.",
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
          `Branch '${args.branch}' is protected. If an eligible manager explicitly approves the bypass, call again with force=true and confirmForce=true.`,
        );
      }

      const project = await client.getProject(args.projectId);
      const safety: BranchDeletionSafety = {
        currentBranch: project.branch,
        divergenceStatus: "not-checked",
        projectStatus: project.status,
        unsavedChanges: project.branch === branch.name && project.status === "EDITING",
      };

      if (project.branch !== branch.name) {
        safety.reason = project.branch
          ? `Studio can only compare the project's current branch '${project.branch}', not deletion target '${branch.name}'. ` +
            `Open '${branch.name}' with openl_open_project and retry to obtain an authoritative divergence check.`
          : `Studio did not report a current branch for deletion target '${branch.name}', so divergence cannot be checked.`;
      } else if (safety.unsavedChanges) {
        safety.reason = `Branch '${branch.name}' has unsaved working-copy changes that deletion will discard.`;
      } else {
        await checkDivergence(client, args.projectId, branches, args.branch, safety);
      }

      if (safety.reason && args.confirmDataLoss !== true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidRequest,
          `${safety.reason} No branch was deleted. Review the risk, then either make the branch safe ` +
            "or call again with confirmDataLoss=true to explicitly accept possible data loss.",
        );
      }

      await client.deleteProjectBranch(args.projectId, args.branch, args.force === true);
      return formatResult({
        success: true,
        branch: args.branch,
        forced: args.force === true,
        safety,
        // A risky delete reaches this point only with confirmDataLoss=true.
        dataLossConfirmed: safety.reason !== undefined,
        message: `Deleted branch '${args.branch}'.`,
      }, args.response_format);
    },
  });
}
