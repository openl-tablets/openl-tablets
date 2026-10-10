/**
 * Project tool handlers — list/get projects, project status, open/save/close,
 * create projects and project branches.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { formatResponse, paginateCollection } from "../formatters.js";
import { isNotFoundError } from "../utils.js";
import { waitForCompilation } from "../stomp-waits.js";
import { getProjectTemplateZip } from "../project-templates.js";
import { registerTool, rethrowConflictAsActionable, type ToolResponse } from "./common.js";

/**
 * Handle tool execution errors with enhanced context
 *
 * @param error - Error to handle
 * @param toolName - Name of the tool that failed
 * @param toolArgs - Tool arguments that were passed (will be sanitized)
 * @returns ProtocolError with enhanced context
 */
/**
 * Severity ordering for `compilation.messages.items`. Anything not recognised
 * is sorted to the end so unknown severities can't push real ERRORs past a
 * response-format truncation point.
 */
const SEVERITY_RANK: Record<string, number> = { ERROR: 0, WARN: 1, INFO: 2 };

const UNKNOWN_SEVERITY_RANK = 99;

function severityRank(severity: string | undefined): number {
  if (!severity) return UNKNOWN_SEVERITY_RANK;
  return SEVERITY_RANK[severity] ?? UNKNOWN_SEVERITY_RANK;
}

/**
 * Apply the response-shaping rules for `openl_project_status`:
 *
 *  1. When `compileState === "ok"`, drop the noisy `items[]` list — counts and
 *     module/test totals are preserved so the caller still sees compile-summary.
 *  2. Otherwise, sort `items[]` by severity (ERROR → WARN → INFO) so the most
 *     actionable diagnostics survive the response-format character truncation
 *     (markdown does a dumb `.slice(0, 25000)` and the backend returns items in
 *     id-ascending order — without this, ERRORs end up past the cutoff when a
 *     project has many WARNs).
 *  3. Optional `severity` filter narrows items to the requested severities.
 *  4. Optional `maxMessages` caps the (already-sorted) items list.
 */
function shapeStatusResponse(
  status: Types.ProjectStatusView,
  severityFilter?: ("ERROR" | "WARN" | "INFO")[],
  maxMessages?: number,
): Types.ProjectStatusView {
  if (!status.compilation?.messages) {
    return status;
  }
  if (status.compileState === "ok") {
    return {
      ...status,
      compilation: {
        ...status.compilation,
        messages: { ...status.compilation.messages, items: [] },
      },
    };
  }
  let items = [...(status.compilation.messages.items ?? [])];
  items.sort((a, b) => severityRank(a.severity) - severityRank(b.severity));
  if (severityFilter && severityFilter.length > 0) {
    const allowed = new Set<string>(severityFilter);
    items = items.filter((m) => m.severity !== undefined && allowed.has(m.severity));
  }
  if (typeof maxMessages === "number" && maxMessages > 0 && items.length > maxMessages) {
    items = items.slice(0, maxMessages);
  }
  return {
    ...status,
    compilation: {
      ...status.compilation,
      messages: { ...status.compilation.messages, items },
    },
  };
}

/**
 * Build a short human-readable progress message for `notifications/progress`
 * from a status snapshot. The MCP client typically renders this next to the
 * progress bar; keep it terse.
 */
function progressMessage(status: Types.ProjectStatusView): string {
  if (status.compileState === "compiling") {
    const m = status.compilation?.modules;
    if (m && typeof m.total === "number" && m.total > 0) {
      return `Compiling — ${m.compiled} / ${m.total} modules`;
    }
    return "Compiling…";
  }
  if (status.compileState === "idle") {
    return "Starting lazy compilation";
  }
  // Terminal states aren't normally emitted via onProgress (the wait resolves first),
  // but include sensible labels just in case.
  return `Compile state: ${status.compileState}`;
}

/**
 * Resolve Studio's opaque id for an exact project name. Create/copy endpoints
 * return only branch/revision, so project IDs must come from /projects rather
 * than being reconstructed from request values.
 */
async function findProjectId(
  client: OpenLClient,
  repositoryId: string,
  projectName: string,
  branch?: string,
): Promise<string | undefined> {
  const limit = 200;
  let offset = 0;
  const seenPages = new Set<string>();

  // This is best-effort discovery after the create/copy already succeeded.
  // Stop on a repeated page (an older Studio ignoring offset) and keep a hard
  // cap so missing pagination metadata can never hang the mutation response.
  for (let pageCount = 0; pageCount < 100; pageCount += 1) {
    const page = await client.listProjectsPage({
      repository: repositoryId,
      name: projectName,
      branch,
      offset,
      limit,
    });
    const project = page.items.find((candidate) => candidate.name === projectName);
    if (project) {
      return typeof project.id === "string" && project.id.length > 0 ? project.id : undefined;
    }

    if (!page.serverPaginated) return undefined;

    const pageSignature = JSON.stringify(page.items.map((candidate) => [
      candidate.id,
      candidate.name,
      candidate.repository,
      candidate.branch,
    ]));
    if (seenPages.has(pageSignature)) return undefined;
    seenPages.add(pageSignature);

    const pageSize = page.pageSize ?? limit;
    const nextOffset = offset + pageSize;
    if (page.items.length === 0 || (page.total !== undefined && nextOffset >= page.total)) {
      return undefined;
    }
    offset = nextOffset;
  }
  return undefined;
}

async function tryFindCreatedProjectId(
  client: OpenLClient,
  repositoryId: string,
  projectName: string,
  branch?: string,
): Promise<string | undefined> {
  try {
    return await findProjectId(client, repositoryId, projectName, branch);
  } catch {
    // Creation has already committed successfully. Do not report the mutation
    // as failed merely because the follow-up discovery request was unavailable.
    return undefined;
  }
}

export function registerProjectHandlers(): void {
  registerTool({
    name: "list_projects",
    category: "Project",
    title: "List Projects",
    description:
      "List projects with the Studio filters for repository, status, dependency, name, author, branch, tags, sorting, and response expansions. Results are paginated (default 50, maximum 200): when a complete inventory is required, follow pagination.has_more and call again with pagination.next_offset until has_more is false. Returns project names, status (OPENED/CLOSED), metadata, and a convenient 'projectId' field from API to use with other tools. For local-only projects, do not pass repository filter 'local' (it may fail); list every page without that filter and filter results by repository === 'local' client-side. For such projects, open/save/close do not work; table/rule/test tools work without opening. IMPORTANT: The 'projectId' is returned exactly as provided by the API and should be used without modification. Pass either the id or name from openl_list_repositories() — both are accepted (case-insensitive). Do not invent example values; call openl_list_repositories() first if not in context. Use this to discover and filter projects.",
    schema: schemas.listProjectsSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const { limit = 50, offset = 0 } = typedArgs;

      // Extract filters (only those supported by ProjectFilters type)
      const filters: Types.ProjectFilters = {};
      // Convert repository name to ID for API call
      if (typedArgs.repository) {
        filters.repository = await client.getRepositoryIdByName(typedArgs.repository);
      }
      if (typedArgs.status) filters.status = typedArgs.status;
      if (typedArgs.dependsOn) filters.dependsOn = typedArgs.dependsOn;
      if (typedArgs.name) filters.name = typedArgs.name;
      if (typedArgs.author) filters.author = typedArgs.author;
      if (typedArgs.branch) filters.branch = typedArgs.branch;
      if (typedArgs.sort) filters.sort = typedArgs.sort;
      if (typedArgs.include) filters.include = typedArgs.include;
      if (typedArgs.tags) filters.tags = typedArgs.tags;

      filters.offset = offset;
      filters.limit = limit;

      const projectsPage = await client.listProjectsPage(filters);
      const projects = projectsPage.items;

      // Transform projects to include a flat projectId field for easier use.
      // projectId is an opaque backend value and must be passed through unchanged.
      const transformedProjects = projects.map((project) => {
        if (typeof project.id !== "string" || project.id.length === 0) {
          throw new ProtocolError(
            ProtocolErrorCode.InternalError,
            "Invalid project ID returned by backend: expected non-empty string."
          );
        }

        return {
          ...project,
          projectId: project.id,
        };
      });

      // If API already paginated, use its pagination metadata
      // Otherwise apply client-side pagination
      const paginated = paginateCollection(
        { ...projectsPage, items: transformedProjects },
        limit,
        offset,
      );

      const formattedResult = formatResponse(paginated.data, format, {
        pagination: paginated.pagination,
        responseMetadata: projectsPage.metadata,
        dataType: "projects",
      });

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "get_project",
    category: "Project",
    title: "Get Project Details",
    description:
      "Get comprehensive project information including details, modules, dependencies, and metadata. Returns full project structure, configuration, and status.",
    schema: schemas.getProjectSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const project = await client.getProject(typedArgs.projectId, typedArgs.include);

      const formattedResult = formatResponse(project, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "project_status",
    category: "Project",
    title: "Get Project Status",
    description:
      "Get the project's compile state, diagnostics, pending changes, and module/test summary. By default wait=true: a supplied branch switches the opened design project to that branch before validation; if Studio reports idle, the tool lazily starts compilation through the tables API; if compilation is already running, it waits for a terminal state (ok/warnings/errors) and emits progress notifications when available. Set wait=false only for a fast read-only snapshot, which may legitimately return idle or compiling and never switches branches. Edits made through the MCP table tools already trigger recompilation.",
    schema: schemas.projectStatusSchema,
    annotations: {
      readOnlyHint: false,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      let status: Types.ProjectStatusView;
      if (typedArgs.wait) {
        const progressToken = extra?._meta?.progressToken;
        const sendNotification = extra?.sendNotification;
        const onProgress =
          progressToken !== undefined && sendNotification
            ? (snap: Types.ProjectStatusView) => {
                // Notification failures are non-fatal — the wait still resolves on the
                // next terminal STOMP frame regardless of whether the client received
                // the progress update.
                const params: {
                  progressToken: string | number;
                  progress: number;
                  total?: number;
                  message?: string;
                } = {
                  progressToken,
                  progress: snap.compilation?.modules?.compiled ?? 0,
                  message: progressMessage(snap),
                };
                const total = snap.compilation?.modules?.total;
                if (typeof total === "number" && total > 0) {
                  params.total = total;
                }
                void sendNotification({
                  method: "notifications/progress",
                  params,
                }).catch(() => { /* ignore */ });
              }
            : undefined;
        status = await waitForCompilation(client, typedArgs.projectId, typedArgs.branch, {
          onProgress,
          signal: extra?.signal,
          timeoutMs: typedArgs.timeoutMs,
          compileOnIdle: true,
        });
      } else {
        status = await client.getProjectStatus(typedArgs.projectId, typedArgs.branch);
      }

      const shaped = shapeStatusResponse(status, typedArgs.severity, typedArgs.maxMessages);
      const payload = status.compileState === "idle"
        ? {
            ...shaped,
            note: typedArgs.wait
              ? "Studio has no compilable module registered for this project; compileState remains idle."
              : "No compilation is registered in this Studio session. Call openl_project_status with wait=true to start lazy compilation and obtain a conclusive result.",
          }
        : shaped;

      return {
        content: [{ type: "text", text: formatResponse(payload, format) }],
      };
    },
  });

  registerTool({
    name: "open_project",
    category: "Project",
    title: "Open Project for Editing",
    description:
      "Open a project for editing. Supports opening on specific branches or viewing specific Git revisions. Use this before making changes to project tables or rules.",
    schema: schemas.openProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      let action: "opened" | "switched_branch" = "opened";

      // If branch is specified, check whether the project is already opened.
      // If so, use switchBranch (PATCH without status) to avoid 409 Conflict.
      if (typedArgs.branch) {
        try {
          const project = await client.getProject(typedArgs.projectId);
          if (project.status === "OPENED" || project.status === "EDITING") {
            await client.switchBranch(typedArgs.projectId, typedArgs.branch);
            action = "switched_branch";
          } else {
            await client.openProject(typedArgs.projectId, {
              branch: typedArgs.branch,
              revision: typedArgs.revision,
              openDependencies: typedArgs.openDependencies,
            });
          }
        } catch {
          // If getProject fails, fall through to the default open logic
          await client.openProject(typedArgs.projectId, {
            branch: typedArgs.branch,
            revision: typedArgs.revision,
            openDependencies: typedArgs.openDependencies,
          });
        }
      } else {
        await client.openProject(typedArgs.projectId, {
          revision: typedArgs.revision,
          openDependencies: typedArgs.openDependencies,
        });
      }

      const message = action === "switched_branch"
        ? `Branch switched to '${typedArgs.branch}' successfully`
        : `Project opened successfully${typedArgs.branch ? ` on branch '${typedArgs.branch}'` : ""}${typedArgs.revision ? ` at revision '${typedArgs.revision}'` : ""}`;

      const result = {
        success: true,
        message,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "save_project",
    category: "Project",
    title: "Save Project to Git",
    description:
      "Save project changes to Git. Works only when project status is EDITING (after opening and making changes). Requires comment (used as revision/commit message). Creates a new revision and transitions project to OPENED. Optional closeAfterSave: true saves and closes in one request. Use after update_table, append_table, or other edits. Does not work for repository 'local'.",
    schema: schemas.saveProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      const result = await client.saveProject(typedArgs.projectId, typedArgs.comment, {
        closeAfterSave: typedArgs.closeAfterSave,
      });

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "close_project",
    category: "Project",
    title: "Close Project",
    description:
      "Close a project. If the project has unsaved changes (status EDITING), you must either save (saveChanges: true with comment) or discard (discardChanges: true). When discarding, ask the user for confirmation and then call again with confirmDiscard: true. Prevents accidental data loss.",
    schema: schemas.closeProjectSchema,
    annotations: {
      destructiveHint: true, // Can discard changes if requested
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      // Check current project status to see if there are unsaved changes
      const currentProject = await client.getProject(typedArgs.projectId);
      const hasUnsavedChanges = currentProject.status === "EDITING";

      // Validate that both saveChanges and discardChanges are not set to true
      if (typedArgs.saveChanges === true && typedArgs.discardChanges === true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          "Cannot set both saveChanges and discardChanges to true. Choose one option:\n" +
          "1. Set saveChanges: true (with comment) to save changes before closing\n" +
          "2. Set discardChanges: true to explicitly discard unsaved changes (destructive operation)"
        );
      }

      if (hasUnsavedChanges) {
        if (typedArgs.saveChanges === true) {
          // Save changes before closing
          if (!typedArgs.comment) {
            throw new ProtocolError(
              ProtocolErrorCode.InvalidParams,
              "comment is required when saveChanges is true. Provide a commit message describing the changes."
            );
          }
          const saveResult = await client.saveProject(typedArgs.projectId, typedArgs.comment);
          if (!saveResult.success) {
            const formattedResult = formatResponse(saveResult, format);
            return {
              content: [{ type: "text", text: formattedResult }],
            };
          }
          await client.closeProject(typedArgs.projectId);
          const result = {
            success: true,
            message: `Project saved and closed successfully with comment: "${typedArgs.comment}"`,
          };
          const formattedResult = formatResponse(result, format);
          return {
            content: [{ type: "text", text: formattedResult }],
          };
        } else if (typedArgs.discardChanges === true) {
          // Only proceed when confirmDiscard is explicitly true (false or undefined require confirmation)
          if (typedArgs.confirmDiscard === true) {
            await client.closeProject(typedArgs.projectId, { discardChanges: true });
            const result = {
              success: true,
              message: "Project closed (unsaved changes discarded)",
            };
            const formattedResult = formatResponse(result, format);
            return {
              content: [{ type: "text", text: formattedResult }],
            };
          }
          // confirmDiscard not set to true: require explicit user confirmation
          const result = {
            success: false,
            confirmationRequired: true,
            message: "The project has unsaved changes. Closing without saving will discard all changes permanently. Ask the user: 'Do you really want to close without saving? All unsaved changes will be lost.' If the user confirms, call openl_close_project again with the same projectId, discardChanges: true, and confirmDiscard: true (confirmDiscard must be set to true explicitly, not just provided).",
          };
          const formattedResult = formatResponse(result, format);
          return {
            content: [{ type: "text", text: formattedResult }],
          };
        } else {
          // Error: must choose to save or discard
          throw new ProtocolError(
            ProtocolErrorCode.InvalidParams,
            "Project has unsaved changes. You must either:\n" +
            "1. Set saveChanges: true (with comment) to save and close\n" +
            "2. Set discardChanges: true to close without saving (then ask user to confirm and call again with confirmDiscard: true)"
          );
        }
      } else {
        // No unsaved changes, safe to close
        await client.closeProject(typedArgs.projectId);
        const result = {
          success: true,
          message: "Project closed successfully",
        };
        const formattedResult = formatResponse(result, format);
        return {
          content: [{ type: "text", text: formattedResult }],
        };
      }
    },
  });

  registerTool({
    name: "create_project_branch",
    category: "Project",
    title: "Create Project Branch",
    description:
      "Create a new branch in a project's repository from a specified revision. Allows branching from specific revisions, tags, or other branches. If no revision is specified, the HEAD revision will be used.",
    schema: schemas.createBranchSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;

      await client.createBranch(typedArgs.projectId, typedArgs.branchName, typedArgs.revision);

      const result = {
        success: true,
        message: `Successfully created branch '${typedArgs.branchName}'${typedArgs.revision ? ` from revision ${typedArgs.revision}` : ""}`,
        branchName: typedArgs.branchName,
        revision: typedArgs.revision,
      };

      const formattedResult = formatResponse(result, format);

      return {
        content: [{ type: "text", text: formattedResult }],
      };
    },
  });

  registerTool({
    name: "create_project",
    category: "Project",
    title: "Create or Copy Project",
    description:
      "Create a new OpenL project in a design repository and commit it. Two modes, selected by the `template` argument:\n" +
      "• CREATE (omit `template`): create a BLANK project from the default empty skeleton.\n" +
      "• COPY (pass `template` = an existing project's exact projectId from openl_list_projects): use Studio's server-side project-copy API to copy that source project's FULL structure and rename its descriptor to projectName. Do not pass the displayed project name because mapped repositories may contain multiple projects with the same name.\n" +
      "Both modes are committed and indexed atomically. Omit `branch` for the repository's configured/default branch, or pass a target branch from openl_list_branches(); Studio also supports creating a missing branch from the base branch. Returns the new project name, commit revision, and Studio's opaque projectId. A name collision, missing copy source, or missing permission is rejected with an actionable error. Local repositories are not supported.",
    schema: schemas.createProjectSchema,
    annotations: {
      readOnlyHint: false,
      destructiveHint: false,
      idempotentHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const typedArgs = args;

      const format = typedArgs.response_format;
      const repositoryId = await client.getRepositoryIdByName(typedArgs.repository);

      const source = typedArgs.template;
      let created: Types.CreateProjectResult;
      try {
        created = source
          ? await client.copyProject(
              repositoryId,
              typedArgs.projectName,
              source,
              { comment: typedArgs.comment, branch: typedArgs.branch },
            )
          : await client.createProjectFromZip(
              repositoryId,
              typedArgs.projectName,
              getProjectTemplateZip("empty"),
              { comment: typedArgs.comment, branch: typedArgs.branch },
            );
      } catch (error) {
        if (source && isNotFoundError(error)) {
          throw new ProtocolError(
            ProtocolErrorCode.InvalidParams,
            `Cannot copy: source project '${source}' was not found. ` +
              "Use the exact projectId from openl_list_projects().",
          );
        }
        rethrowConflictAsActionable(
          error,
          `Cannot create project: a project named '${typedArgs.projectName}' already exists in repository '${typedArgs.repository}'. ` +
            `Choose a different projectName, or use openl_list_projects() to see existing names.`
        );
      }

      const projectId = await tryFindCreatedProjectId(
        client,
        repositoryId,
        typedArgs.projectName,
        typedArgs.branch,
      );

      const result = {
        success: true,
        mode: source ? "copy" : "create",
        ...(projectId ? { projectId } : {}),
        projectName: typedArgs.projectName,
        ...(source ? { source } : {}),
        repository: typedArgs.repository,
        branch: created.branch,
        revision: created.revision,
        message:
          `${source ? `Copied '${source}' to` : "Created project"} '${typedArgs.projectName}' in repository '${typedArgs.repository}'` +
          `${created.branch ? ` (branch '${created.branch}')` : ""}` +
          `${created.revision ? ` at revision ${created.revision}` : ""}.`,
        ...(!projectId ? {
          note:
            "The created or copied project could not be read back to resolve its canonical projectId. " +
            "Call openl_list_projects with this repository and project name before using project tools.",
        } : {}),
      };

      return {
        content: [{ type: "text", text: formatResponse(result, format) }],
      };
    },
  });
}
