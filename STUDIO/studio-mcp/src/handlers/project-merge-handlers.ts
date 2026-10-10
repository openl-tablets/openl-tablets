/**
 * Project-aware branch lifecycle, merge execution, and read-only conflict inspection.
 * Merge conflict state belongs to the Studio HTTP session, so every step must
 * use the same OpenLClient (the MCP server already preserves its JSESSIONID).
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import {
  looksBinary,
  normalizeMimeType,
} from "../content-utils.js";
import { formatResponse } from "../formatters.js";
import { registerTool, type ToolResponse } from "./common.js";

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

function isUtf8ContinuationByte(byte: number | undefined): boolean {
  return byte !== undefined && (byte & 0xc0) === 0x80;
}

/** Move byte boundaries forward so a UTF-8 chunk never starts or ends inside a code point. */
function alignUtf8Chunk(data: Buffer, requestedStart: number, requestedEnd: number): [number, number] {
  let start = requestedStart;
  while (start < data.length && isUtf8ContinuationByte(data[start])) start += 1;

  let end = Math.max(start, requestedEnd);
  while (end < data.length && isUtf8ContinuationByte(data[end])) end += 1;
  return [start, end];
}

function mergeRequest(
  args: { mode: Types.MergeMode; otherBranch: string },
): Types.MergeRequest {
  return { mode: args.mode, otherBranch: args.otherBranch };
}

export function registerProjectMergeHandlers(): void {
  registerTool({
    name: "list_project_branches",
    category: "Project",
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
      const format = args.response_format;
      const branches = await client.listProjectBranches(args.projectId, args.scope);
      return { content: [{ type: "text", text: formatResponse(branches, format) }] };
    },
  });

  registerTool({
    name: "check_project_merge",
    category: "Project",
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
      const format = args.response_format;
      const result = await client.checkProjectMerge(args.projectId, mergeRequest(args));
      return { content: [{ type: "text", text: formatResponse(result, format) }] };
    },
  });

  registerTool({
    name: "merge_project_branches",
    category: "Project",
    title: "Merge Project Branches",
    description:
      "Attempt to merge project branches. The tool first repeats the relationship and permission precheck: up-to-date returns without a write, and permission/lock blockers fail before merge. The precheck does not predict conflicts, so an allowed attempt can still return status='conflicts'. mode='receive' merges otherBranch into the current branch; mode='send' merges the current branch into otherBranch. A conflict result creates read-only, session-bound conflict state: inspect it on this same MCP server, then hand resolution to the user in Studio or cancel the pending state. Never choose OURS or THEIRS automatically. force is only for an eligible protected-target bypass and requires confirmForce=true.",
    schema: schemas.mergeProjectBranchesSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const request = mergeRequest(args);
      const check = await client.checkProjectMerge(args.projectId, request);

      if (check.status === "up-to-date") {
        return {
          content: [{
            type: "text",
            text: formatResponse({ success: true, status: "up-to-date", check }, format),
          }],
        };
      }
      const forceBypassAllowed = args.force === true && check.blockedBy === "bypass-required";
      if (args.force === true && !forceBypassAllowed) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          "force=true is allowed only when Studio reports blockedBy='bypass-required'. Run openl_check_project_merge again without force.",
        );
      }
      if ((check.canMerge === false || check.blockedBy != null) && !forceBypassAllowed) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidRequest,
          `Merge is blocked${check.blockedBy ? ` by '${check.blockedBy}'` : ""}. ` +
            "Resolve the blocker and run openl_check_project_merge again.",
        );
      }

      const result = await client.mergeProjectBranches(args.projectId, request, args.force === true);
      const response = {
        success: result.status === "success",
        ...result,
        ...(result.status === "conflicts"
          ? {
              nextAction:
                "The relationship precheck does not predict conflicts; the actual merge found conflicts stored in this Studio HTTP session. Call openl_get_merge_conflicts on this same MCP server and inspect BASE/OURS/THEIRS with openl_read_merge_conflict_file. Do not choose a side automatically: present the conflicts to the user for manual resolution in Studio, then call openl_cancel_merge_conflicts to clear this pending MCP session state.",
            }
          : { check }),
      };
      return { content: [{ type: "text", text: formatResponse(response, format) }] };
    },
  });

  registerTool({
    name: "get_merge_conflicts",
    category: "Project",
    title: "Get Merge Conflicts",
    description:
      "Get the pending merge conflicts stored in this Studio HTTP session: grouped file paths, BASE/OURS/THEIRS revision details, and the default merge commit message. This is read-only evidence for a user who will resolve the conflict manually in Studio; the MCP server intentionally does not expose conflict resolution. Available only after openl_merge_project_branches returns status='conflicts'; use the same MCP server instance throughout inspection.",
    schema: schemas.getMergeConflictsSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const conflicts = await client.getMergeConflicts(args.projectId);
      return { content: [{ type: "text", text: formatResponse(conflicts, format) }] };
    },
  });

  registerTool({
    name: "read_merge_conflict_file",
    category: "Project",
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
      const requestedStart = Math.min(args.offset ?? 0, total);
      const requestedEnd = Math.min(requestedStart + (args.length ?? CONFLICT_FILE_CHUNK_SIZE), total);
      const requestedSlice = response.data.subarray(requestedStart, requestedEnd);
      const asBase64 = args.encoding === "base64" ||
        (args.encoding !== "utf-8" && looksBinary(requestedSlice));
      const [start, end] = asBase64
        ? [requestedStart, requestedEnd]
        : alignUtf8Chunk(response.data, requestedStart, requestedEnd);
      const slice = response.data.subarray(start, end);
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

      if (!asBase64) {
        const envelope = { ...metadata, content: slice.toString("utf-8") };
        return {
          content: [{ type: "text", text: formatResponse(envelope, "json", { skipTruncation: true }) }],
        };
      }

      // Use TextContent for arbitrary binary data. Some MCP clients send every
      // embedded blob to an image decoder and reject valid XLSX/ZIP bytes.
      const envelope = { ...metadata, content: slice.toString("base64") };
      return {
        content: [{
          type: "text",
          text: formatResponse(envelope, "json", { skipTruncation: true }),
        }],
      };
    },
  });

  registerTool({
    name: "cancel_merge_conflicts",
    category: "Project",
    title: "Clear Pending Merge Conflicts",
    description:
      "Abort the pending merge-conflict workflow by clearing its session state. This does not modify files or branches, but the stored conflict analysis is discarded; run the merge again to recreate it.",
    schema: schemas.cancelMergeConflictsSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      await client.cancelMergeConflicts(args.projectId);
      return {
        content: [{
          type: "text",
          text: formatResponse({ success: true, message: "Pending merge conflict state was cleared." }, format),
        }],
      };
    },
  });

  registerTool({
    name: "delete_project",
    category: "Project",
    title: "Delete Project",
    description:
      "Permanently delete a project through Studio. This may create a deletion commit in its design repository. Safety guard: confirmProjectName is required and must exactly match the project's current backend name; the handler reads the project immediately before deletion. An optional comment becomes the deletion commit message.",
    schema: schemas.deleteProjectSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
      const project = await client.getProject(args.projectId);
      if (args.confirmProjectName !== project.name) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          `Deletion confirmation does not match. Expected confirmProjectName='${project.name}'. ` +
            "No project was deleted.",
        );
      }
      await client.deleteProject(args.projectId, args.comment);
      return {
        content: [{
          type: "text",
          text: formatResponse({
            success: true,
            projectId: args.projectId,
            projectName: project.name,
            message: `Deleted project '${project.name}'.`,
          }, format),
        }],
      };
    },
  });

  registerTool({
    name: "delete_project_branch",
    category: "Project",
    title: "Delete Project Branch",
    description:
      "Delete a branch from the repository hosting a project. The tool first reads project-aware branch metadata: a base branch is always rejected, and a protected branch requires force=true plus confirmForce=true (and eligible Studio permissions). It then performs a safe-delete preflight against the repository base branch when the target is the project's current branch. A branch with commits absent from base, unsaved changes, or no authoritative divergence check is rejected unless confirmDataLoss=true explicitly acknowledges the reported risk. confirmBranchName must exactly equal branch. If the project is open on the deleted branch, Studio closes it first. Branch names containing '/' are supported.",
    schema: schemas.deleteProjectBranchSchema,
    annotations: {
      destructiveHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const format = args.response_format;
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
        let baseBranch = branches.find((candidate) => candidate.base)?.name;
        if (!baseBranch) {
          try {
            const repositoryBranches = await client.listProjectBranches(args.projectId, "repository");
            baseBranch = repositoryBranches.find((candidate) => candidate.base)?.name;
          } catch {
            // A failed safety probe must become an explicit-risk confirmation,
            // not an accidental unguarded delete or an inescapable hard failure.
          }
        }
        safety.baseBranch = baseBranch;

        if (!baseBranch) {
          safety.reason = "Studio did not identify the repository base branch, so divergence cannot be checked.";
        } else {
          try {
            const mergeCheck = await client.checkProjectMerge(args.projectId, {
              mode: "send",
              otherBranch: baseBranch,
            });
            safety.mergeCheck = mergeCheck;
            if (mergeCheck.sourceBranch !== branch.name || mergeCheck.targetBranch !== baseBranch) {
              safety.reason =
                `Studio checked branch '${mergeCheck.sourceBranch}' into '${mergeCheck.targetBranch}' instead of ` +
                `'${branch.name}' into '${baseBranch}', so deletion-target divergence was not verified.`;
            } else {
              safety.divergenceStatus = mergeCheck.status === "up-to-date" ? "up-to-date" : "unmerged";
            }
            if (!safety.reason && safety.divergenceStatus === "unmerged") {
              safety.reason =
                `Branch '${branch.name}' has commits that are not merged into base branch '${baseBranch}'.`;
            }
          } catch {
            safety.reason =
              `Studio could not verify whether branch '${branch.name}' is merged into base branch '${baseBranch}'.`;
          }
        }
      }

      if (safety.reason && args.confirmDataLoss !== true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidRequest,
          `${safety.reason} No branch was deleted. Review the risk, then either make the branch safe ` +
            "or call again with confirmDataLoss=true to explicitly accept possible data loss.",
        );
      }

      await client.deleteProjectBranch(args.projectId, args.branch, args.force === true);
      return {
        content: [{
          type: "text",
          text: formatResponse({
            success: true,
            branch: args.branch,
            forced: args.force === true,
            safety,
            dataLossConfirmed: safety.reason ? args.confirmDataLoss === true : false,
            message: `Deleted branch '${args.branch}'.`,
          }, format),
        }],
      };
    },
  });
}
