/**
 * Project tool handlers — list/get projects, project status, open/save/close,
 * create projects and project branches, and list the modules and worksheets of a project.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { paginateCollection } from "../formatters.js";
import { isNotFoundError, isRequestTimeout } from "../http.js";
import { registerTool, formatResult, type ToolResponse } from "./common.js";
import { refreshFromDisk } from "./freshness.js";

/**
 * Severity ordering for `compilation.messages.items`. Anything not recognised
 * is sorted to the end so unknown severities can't push real ERRORs past a
 * response-format truncation point.
 */
const SEVERITY_RANK: Record<string, number> = { ERROR: 0, WARN: 1, INFO: 2 };

function severityRank(severity: string | undefined): number {
  return SEVERITY_RANK[severity ?? ""] ?? 99;
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
  let items = (status.compilation.messages.items ?? [])
    .toSorted((a, b) => severityRank(a.severity) - severityRank(b.severity));
  if (severityFilter?.length) {
    const allowed = new Set<string>(severityFilter);
    items = items.filter((m) => m.severity !== undefined && allowed.has(m.severity));
  }
  if (maxMessages) {
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

/** What `openl_project_status` adds when it compiled the project again from files another program changed. */
const CHANGED_ON_DISK_NOTE =
  "Files of the project were changed outside Studio, so the project is compiled again from them. " +
  "Read the tables again before editing them; call openl_project_status again for the result of the compilation.";

/** What `openl_project_status` adds to a state that is not a final result of a compilation. */
const STATUS_NOTES: Partial<Record<Types.CompileState, string>> = {
  idle: "Studio has no compilable module registered for this project; compileState remains idle.",
  compiling: "The compilation is still running in Studio. Call openl_project_status again for its result.",
};

/**
 * Reads the compile state of a project for `openl_project_status`.
 *
 * A given branch that differs from the one the project is opened on is switched to first. A project Studio has not
 * compiled yet (`idle`) is compiled through the tables API, which answers once the compilation ends.
 *
 * A compilation that was already running, or that outlasts `timeoutMs`, is reported as it is: `compiling`.
 *
 * A project whose files another program changed since Studio compiled them is compiled again from those files, waiting
 * for the compilation up to `timeoutMs` as for an idle project: the status that is answered tells `refreshed`.
 */
async function readProjectStatus(
  client: OpenLClient,
  projectId: string,
  branch: string | undefined,
  options: { signal?: AbortSignal; timeoutMs?: number },
): Promise<Types.ProjectStatusView & { refreshed?: true }> {
  let status = await client.getProjectStatus(projectId);
  if (branch && status.branch && branch !== status.branch) {
    await client.openOnBranch(projectId, branch);
    status = await client.getProjectStatus(projectId);
  }
  if (status.changedOnDisk && await refreshFromDisk(client, projectId, options.signal, options.timeoutMs)) {
    return { ...(await client.getProjectStatus(projectId)), refreshed: true };
  }
  if (status.compileState !== "idle") {
    return status;
  }
  try {
    await client.triggerProjectCompilation(projectId, options);
  } catch (error) {
    // Studio goes on compiling after the wait gives up; the status read below reports it as running.
    if (!isRequestTimeout(error)) {
      throw error;
    }
  }
  return client.getProjectStatus(projectId);
}

export function registerProjectHandlers(): void {
  registerTool({
    name: "list_projects",
    title: "List Projects",
    description:
      "List projects with the Studio filters for repository, status, dependency, name, author, branch, tags, sorting, and response expansions. Results are paginated (default 50, maximum 200): when a complete inventory is required, follow pagination.has_more and call again with pagination.next_offset until has_more is false. Returns project names, status (OPENED/CLOSED), metadata, and a convenient 'projectId' field from API to use with other tools. For local-only projects, do not pass repository filter 'local' (it may fail); list every page without that filter and filter results by repository === 'local' client-side. For such projects, open/save/close do not work; table/rule/test tools work without opening. The same holds for a project edited in place — a project of a file repository in single-user mode, as a developer runs OpenL Studio on their machine: it is always open, every edit is written straight into its files, and its folder has no branches; openl_get_project tells it by capabilities without canOpen, canSave and canClose. IMPORTANT: The 'projectId' is returned exactly as provided by the API and should be used without modification. Pass either the id or name from openl_list_repositories() — both are accepted (case-insensitive). Do not invent example values; call openl_list_repositories() first if not in context. Use this to discover and filter projects.",
    schema: schemas.listProjectsSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { response_format: format, repository, ...filters } = args;
      const projectsPage = await client.listProjectsPage({
        ...filters,
        // The client may name the repository; Studio takes its id.
        repository: repository ? await client.getRepositoryIdByName(repository) : undefined,
      });
      // Each project carries its opaque id once more as projectId, the field the other tools take
      const transformedProjects = projectsPage.items.map((project) => ({ ...project, projectId: project.id }));

      const paginated = paginateCollection({ ...projectsPage, items: transformedProjects }, filters.offset);

      return formatResult(paginated.data, format, {
        pagination: paginated.pagination,
        responseMetadata: projectsPage.metadata,
        dataType: "projects",
      });
    },
  });

  registerTool({
    name: "get_project",
    title: "Get Project Details",
    description:
      "Get comprehensive project information including details, modules, dependencies, and metadata. Returns full project structure, configuration, and status. Its 'capabilities' tell which lifecycle tools apply: canOpen, canSave, canClose and canManageBranches are left out when the operation is not available — for a project edited in place (a file repository in single-user mode) the first three always are, as the project is always open and every edit is already in its files.",
    schema: schemas.getProjectSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const project = await client.getProject(args.projectId, args.include);

      return formatResult(project, args.response_format);
    },
  });

  registerTool({
    name: "project_status",
    title: "Get Project Status",
    description:
      "Get the project's compile state, diagnostics, pending changes, and module/test summary. A supplied branch switches the opened design project to that branch first. If Studio has not compiled the project yet (idle), the tool compiles it through the tables API before answering, waiting up to timeoutMs. A compilation that is already running, or outlasts timeoutMs, is reported as compiling: call the tool again for its result. Edits made through the MCP table tools already trigger recompilation. A project whose files another program, such as Excel or an IDE, changed since Studio compiled them is compiled again from those files, with a note to read its tables again: what was read before is what the files held before the change.",
    schema: schemas.projectStatusSchema,
    annotations: {
      readOnlyHint: false,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client, extra): Promise<ToolResponse> => {
      const status = await readProjectStatus(client, args.projectId, args.branch, {
        signal: extra?.signal,
        timeoutMs: args.timeoutMs,
      });
      const { refreshed, ...read } = status;
      const shaped = shapeStatusResponse(read, args.severity, args.maxMessages);
      const note = refreshed ? CHANGED_ON_DISK_NOTE : STATUS_NOTES[status.compileState];
      const payload = note ? { ...shaped, note } : shaped;

      return formatResult(payload, args.response_format);
    },
  });

  registerTool({
    name: "open_project",
    title: "Open Project for Editing",
    description:
      "Open a project for editing. Supports opening on specific branches or viewing specific Git revisions. Use this before making changes to project tables or rules — unless capabilities.canOpen of openl_get_project is absent: a project edited in place (a file repository in single-user mode) is always open, so edit its tables and files right away.",
    schema: schemas.openProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      let action: "opened" | "switched_branch" = "opened";

      if (args.branch) {
        action = await client.openOnBranch(args.projectId, args.branch, {
          revision: args.revision,
          openDependencies: args.openDependencies,
        });
      } else {
        await client.openProject(args.projectId, {
          revision: args.revision,
          openDependencies: args.openDependencies,
        });
      }

      const onBranch = args.branch ? ` on branch '${args.branch}'` : "";
      const atRevision = args.revision ? ` at revision '${args.revision}'` : "";
      const message = action === "switched_branch"
        ? `Branch switched to '${args.branch}' successfully`
        : `Project opened successfully${onBranch}${atRevision}`;

      const result = {
        success: true,
        message,
      };

      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "save_project",
    title: "Save Project",
    description:
      "Save project changes. Works when project status is EDITING (after opening and making changes). A comment is required only when repository features.comments is true; omit it when comments are unsupported. Creates a new revision and transitions project to OPENED. Optional closeAfterSave: true saves and closes in one request; a project without unsaved changes is then closed as it is. Use after update_table, append_table, or other edits. Does not work for repository 'local', nor when capabilities.canSave of openl_get_project is absent: a project edited in place (a file repository in single-user mode) has every edit in its files already, with nothing to save.",
    schema: schemas.saveProjectSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const result = await client.saveProject(args.projectId, args.comment, {
        closeAfterSave: args.closeAfterSave,
      });

      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "close_project",
    title: "Close Project",
    description:
      "Close a project. If the project has unsaved changes (status EDITING), you must either save (saveChanges: true; comment required only when repository features.comments is true) or discard (discardChanges: true). When discarding, ask the user for confirmation and then call again with confirmDiscard: true. Prevents accidental data loss. Not for a project whose capabilities.canClose (openl_get_project) is absent: a project edited in place (a file repository in single-user mode) is never closed, as its files are the project itself.",
    schema: schemas.closeProjectSchema,
    annotations: {
      destructiveHint: true, // Can discard changes if requested
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const reply = (result: Record<string, unknown>): ToolResponse =>
        formatResult(result, args.response_format);
      if (args.saveChanges === true && args.discardChanges === true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          "Cannot set both saveChanges and discardChanges to true. Choose one option:\n" +
          "1. Set saveChanges: true (with comment if supported) to save changes before closing\n" +
          "2. Set discardChanges: true to explicitly discard unsaved changes (destructive operation)"
        );
      }

      // A project without unsaved changes closes as it is
      const currentProject = await client.getProject(args.projectId);
      if (currentProject.status !== "EDITING") {
        await client.closeProject(args.projectId);
        return reply({ success: true, message: "Project closed successfully" });
      }

      if (args.saveChanges === true) {
        const supportsComments = currentProject.repositoryInfo?.features?.comments !== false;
        if (supportsComments && !args.comment) {
          throw new ProtocolError(
            ProtocolErrorCode.InvalidParams,
            "comment is required when saveChanges is true. Provide a commit message describing the changes."
          );
        }
        await client.saveProject(args.projectId, args.comment, { closeAfterSave: true });
        return reply({ success: true, message: supportsComments
          ? `Project saved and closed successfully with comment: "${args.comment}"`
          : "Project saved and closed successfully" });
      }

      if (args.discardChanges !== true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          "Project has unsaved changes. You must either:\n" +
          "1. Set saveChanges: true (with comment if supported) to save and close\n" +
          "2. Set discardChanges: true to close without saving (then ask user to confirm and call again with confirmDiscard: true)"
        );
      }

      // Only confirmDiscard: true discards the changes; false or undefined asks the user first
      if (args.confirmDiscard !== true) {
        return reply({
          success: false,
          confirmationRequired: true,
          message: "The project has unsaved changes. Closing without saving will discard all changes permanently. Ask the user: 'Do you really want to close without saving? All unsaved changes will be lost.' If the user confirms, call openl_close_project again with the same projectId, discardChanges: true, and confirmDiscard: true (confirmDiscard must be set to true explicitly, not just provided).",
        });
      }
      await client.closeProject(args.projectId, { discardChanges: true });
      return reply({ success: true, message: "Project closed (unsaved changes discarded)" });
    },
  });

  registerTool({
    name: "create_project_branch",
    title: "Create Project Branch",
    description:
      "Create a new branch in a project's repository from a specified revision. Allows branching from specific revisions, tags, or other branches. If no revision is specified, the HEAD revision will be used. Only for a repository with branches (features.branches of openl_list_repository_features; capabilities.canManageBranches of openl_get_project): the folder of a file repository has none, a project edited in place included.",
    schema: schemas.createBranchSchema,
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.createBranch(args.projectId, args.branchName, args.revision);

      const fromRevision = args.revision ? ` from revision ${args.revision}` : "";
      const result = {
        success: true,
        message: `Successfully created branch '${args.branchName}'${fromRevision}`,
        branchName: args.branchName,
        revision: args.revision,
      };

      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "create_project",
    title: "Create or Copy Project",
    description:
      "Create a new OpenL project in a design repository. Two modes, selected by the `template` argument:\n" +
      "• CREATE (omit `template`): create a BLANK project from the \"Empty Project\" template OpenL Studio ships, as a user of Studio does.\n" +
      "• COPY (pass `template` = an existing project's exact projectId from openl_list_projects): use Studio's server-side project-copy API to copy that source project's FULL structure and rename its descriptor to projectName. Do not pass the displayed project name because mapped repositories may contain multiple projects with the same name.\n" +
      "Both modes are written and indexed atomically. Omit `branch` for the repository's configured/default branch, or pass a target branch from openl_list_branches() when branches are supported; Studio also supports creating a missing branch from the base branch. Omit `comment` when repository features.comments is false. Returns the new project name, revision, and Studio's opaque projectId. A name collision, missing copy source, or missing permission is rejected with an actionable error. Local repositories are not supported.",
    schema: schemas.createProjectSchema,
    annotations: {
      readOnlyHint: false,
      destructiveHint: false,
      idempotentHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const repositoryId = await client.getRepositoryIdByName(args.repository);

      const source = args.template;
      let created: Types.CreateProjectResult;
      try {
        created = source
          ? await client.copyProject(
              repositoryId,
              args.projectName,
              source,
              { comment: args.comment, branch: args.branch },
            )
          : await client.createEmptyProject(
              repositoryId,
              args.projectName,
              { comment: args.comment, branch: args.branch },
            );
      } catch (error) {
        if (source && isNotFoundError(error)) {
          throw new ProtocolError(
            ProtocolErrorCode.InvalidParams,
            `Cannot copy: source project '${source}' was not found. ` +
              "Use the exact projectId from openl_list_projects().",
          );
        }
        throw error;
      }

      const result = {
        success: true,
        mode: source ? "copy" : "create",
        projectId: created.id,
        projectName: args.projectName,
        ...(source ? { source } : {}),
        repository: args.repository,
        branch: created.branch,
        revision: created.revision,
        message: createdMessage(source, args.projectName, args.repository, created),
      };

      return formatResult(result, args.response_format);
    },
  });

  registerTool({
    name: "list_project_modules",
    title: "List Project Modules",
    description:
      "List the modules declared by a project, including module names, rules-root paths/patterns, and modules matched by a pattern. Use the returned name when creating/copying a table, selecting a module graph, or listing worksheets.",
    schema: schemas.projectOnlySchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const modules = await client.listProjectModules(args.projectId);
      return formatResult(modules, args.response_format);
    },
  });

  registerTool({
    name: "list_module_sheets",
    title: "List Module Worksheets",
    description:
      "List worksheet names in a project module. First call openl_list_project_modules to get the exact moduleName. Use a returned sheet name as the destination sheet when creating or copying a table.",
    schema: schemas.projectModuleSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const sheets = await client.listModuleSheets(args.projectId, args.moduleName);
      return formatResult(sheets, args.response_format);
    },
  });
}

/** The message of a created or copied project: where it is, on which branch and at which revision. */
function createdMessage(
  source: string | undefined,
  projectName: string,
  repository: string,
  created: Types.CreateProjectResult,
): string {
  const what = source ? `Copied '${source}' to` : "Created project";
  const branch = created.branch ? ` (branch '${created.branch}')` : "";
  const revision = created.revision ? ` at revision ${created.revision}` : "";
  return `${what} '${projectName}' in repository '${repository}'${branch}${revision}.`;
}
