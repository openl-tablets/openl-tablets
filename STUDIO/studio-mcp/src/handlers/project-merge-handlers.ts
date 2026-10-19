/**
 * Project-aware branch lifecycle, merge execution, and read-only conflict inspection.
 * Studio keeps the merge conflict state for the credential of the client, so
 * every step of one merge finds it.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { normalizeMimeType, sliceContent } from "../content-utils.js";
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
      "Check merge direction, branch relationship, permissions, and blockers without changing Git. This is not a conflict preview: status='mergeable' means the source has changes absent from the target, not that the merge is conflict-free; conflicts are discovered only by openl_merge_project_branches. status='up-to-date' means the target already contains the source. canMerge reports whether Studio permits the attempt after permission, protection, and lock checks. mode='receive' merges otherBranch into the project's current branch; mode='send' merges the current branch into otherBranch. Discover merge targets with openl_list_project_branches(scope='repository'), because a valid target may not hold the project yet.",
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
      "Attempt to merge project branches. The tool first repeats the relationship and permission precheck: up-to-date returns without a write, and permission/lock blockers fail before merge. The precheck does not predict conflicts, so an allowed attempt can still return status='conflicts'. Studio runs the merge in the background; the tool waits until it ends, for up to 10 minutes. mode='receive' merges otherBranch into the current branch; mode='send' merges the current branch into otherBranch. A conflict result creates read-only, session-bound conflict state: inspect it on this same MCP server, then hand resolution to the user in Studio or cancel the pending state. Never choose OURS or THEIRS automatically. force is only for an eligible protected-target bypass and requires confirmForce=true.",
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
                "The relationship precheck does not predict conflicts; the actual merge found conflicts Studio keeps for this client. Call openl_get_merge_conflicts on this same MCP server and inspect BASE/OURS/THEIRS with openl_read_merge_conflict_file. Do not choose a side automatically: present the conflicts to the user for manual resolution in Studio, then call openl_cancel_merge_conflicts to clear this pending MCP session state.",
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
      "Get the pending merge conflicts Studio keeps for this client: grouped file paths, BASE/OURS/THEIRS revision details, and the default merge commit message. This is read-only evidence for a user who will resolve the conflict manually in Studio; the MCP server intentionally does not expose conflict resolution. Available only after openl_merge_project_branches returns status='conflicts'; use the same MCP server instance throughout inspection.",
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
      "Read one BASE, OURS, or THEIRS version of a conflicted file from the current session as read-only evidence for manual user resolution in Studio. Never infer or apply a winning side automatically. UTF-8 and base64 binary chunks are returned in a JSON text envelope with MIME and byte-range metadata. Both forms include nextOffset while more data remains. length targets at most 16000 bytes; a UTF-8 chunk may include up to 3 extra bytes to finish its last character. The backend still downloads the whole file before this client-side slice. Use the exact file path from openl_get_merge_conflicts.",
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
