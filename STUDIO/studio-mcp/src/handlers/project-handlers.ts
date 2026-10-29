/**
 * Project tool handlers — list/get projects, project status, open/save/close,
 * create projects and project branches, and list the modules and worksheets of a project.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { paginateCollection, type Truncation } from "../formatters.js";
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

/**
 * The cut of a status over the size limit of an answer: its leading compile messages, the errors first, so the answer
 * stays a whole status rather than a preview of its text. Nothing to cut without messages.
 */
function messageTruncation(status: Types.ProjectStatusView): Truncation | undefined {
  const { compilation } = status;
  const items = compilation?.messages?.items;
  if (!compilation?.messages || !items?.length) {
    return undefined;
  }
  const { messages } = compilation;
  return {
    items: items.length,
    unit: "compile messages",
    cut: (count) => ({ ...status, compilation: { ...compilation, messages: { ...messages, items: items.slice(0, count) } } }),
    next: () => "choose the messages to read with severity or maxMessages",
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

/** A project as the project tools answer it, with its capabilities by name and a compile status that may be partial. */
type AnsweredProject = Omit<Types.ProjectViewModel, "compileStatus" | "capabilities"> & {
  compileStatus?: Partial<Types.ProjectStatusView>;
  capabilities?: string[];
};

/** The operations a project takes, by name: `canOpen`, `canSave`. Studio leaves out the ones it does not take. */
function capabilityNames(capabilities: Types.ProjectCapabilities): string[] {
  return Object.entries(capabilities).filter(([, available]) => available).map(([name]) => name);
}

/**
 * A project as the project tools answer it: its capabilities as a list of names, and its compile status without the
 * id, the branch, the revision and the last change the project names already, which an agent would otherwise read
 * twice. A value that differs stays.
 */
function answeredProject({ capabilities, ...project }: Types.ProjectViewModel): AnsweredProject {
  const answered = { ...project, ...(capabilities ? { capabilities: capabilityNames(capabilities) } : {}) };
  if (!project.compileStatus) {
    return answered;
  }
  const { projectId, branch, revision, lastModifiedBy, ...status } = project.compileStatus;
  const sameChange = lastModifiedBy?.author === project.modifiedBy && lastModifiedBy?.date === project.modifiedAt;
  return {
    ...answered,
    compileStatus: {
      ...(projectId === project.id ? {} : { projectId }),
      ...(branch === project.branch ? {} : { branch }),
      ...(revision === project.revision ? {} : { revision }),
      ...(sameChange ? {} : { lastModifiedBy }),
      ...status,
    },
  };
}

export function registerProjectHandlers(): void {
  registerTool({
    name: "list_projects",
    title: "List Projects",
    description:
      "List projects, filtered by repository (its id or name from openl_list_repositories), status, dependency, name, author, branch and tags, page by page (50 by default, 200 at most): follow pagination.next_offset while has_more is true for a complete list. Each project names its id as 'projectId', which the other tools take as it is, and leaves its capabilities to openl_get_project. The repository filter 'local' may fail: list without it and pick the projects whose repository is 'local'. Such a project, like a project edited in place (no canOpen, canSave and canClose in the capabilities of openl_get_project), is never opened, saved or closed and has no branches: its tables, rules and tests work as they are.",
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
      // Each project carries its opaque id as projectId, the name the other tools take it by, and only so. The
      // repository of a page is the same for many projects, and openl_list_repository_features tells it; the capabilities
      // of a project, openl_get_project.
      const transformedProjects = projectsPage.items.map((project) => {
        const { id, repositoryInfo, capabilities, ...rest } = answeredProject(project);
        return { projectId: id, ...rest };
      });

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
      "Get a project: its name, branch, status, repository, last change and revision, and its 'capabilities', the operations it takes. canOpen, canSave, canClose and canManageBranches are left out when the operation is not available: for a project edited in place (a file repository in single-user mode) the first three always are, as the project is always open and every edit is already in its files. include adds its compile status (status) or its rules.xml descriptor (descriptor).",
    schema: schemas.getProjectSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const project = await client.getProject(args.projectId, args.include);

      const { id, ...answered } = answeredProject(project);
      return formatResult({ projectId: id, ...answered });
    },
  });

  registerTool({
    name: "project_status",
    title: "Get Project Status",
    description:
      "Get the project's compile state, diagnostics, pending changes, and module/test summary. A supplied branch puts the project on that branch first, opening a closed one. If Studio has not compiled the project yet (idle), the tool compiles it through the tables API before answering, waiting up to timeoutMs. A compilation that is already running, or outlasts timeoutMs, is reported as compiling: call the tool again for its result. Edits made through the MCP table tools already trigger recompilation. A project whose files another program, such as Excel or an IDE, changed since Studio compiled them is compiled again from those files, with a note to read its tables again: what was read before is what the files held before the change.",
    schema: schemas.projectStatusSchema,
    annotations: {
      destructiveHint: false,
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

      return formatResult(payload, "json", { truncation: messageTruncation(payload) });
    },
  });

  registerTool({
    name: "open_project",
    title: "Open Project for Editing",
    description:
      "Open a project for editing. Supports opening on specific branches or viewing specific Git revisions; a project open already is left as it is when neither asks otherwise. Use this before making changes to project tables or rules — unless the capabilities of openl_get_project lack canOpen: a project edited in place (a file repository in single-user mode) is always open, so edit its tables and files right away.",
    schema: schemas.openProjectSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const options = { revision: args.revision, openDependencies: args.openDependencies };
      const action = args.branch
        ? await client.openOnBranch(args.projectId, args.branch, options)
        : await client.openProject(args.projectId, options);

      // The branch and the revision are the ones the call asked for
      return formatResult(action === "already_open" ? { alreadyOpen: true } : { opened: true });
    },
  });

  registerTool({
    name: "save_project",
    title: "Save Project",
    description:
      "Save project changes. Works when project status is EDITING (after opening and making changes). A comment is required only when repository features.comments is true; omit it when comments are unsupported. Creates a new revision and transitions project to OPENED. Optional closeAfterSave: true saves and closes in one request; a project without unsaved changes is then closed as it is. Use after update_table, edit_table, or other edits. Does not work for repository 'local', nor when the capabilities of openl_get_project lack canSave: a project edited in place (a file repository in single-user mode) has every edit in its files already, with nothing to save.",
    schema: schemas.saveProjectSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      return formatResult(await client.saveProject(args.projectId, args.comment, {
        closeAfterSave: args.closeAfterSave,
      }));
    },
  });

  registerTool({
    name: "close_project",
    title: "Close Project",
    description:
      "Close a project. If the project has unsaved changes (status EDITING), you must either save (saveChanges: true; comment required only when repository features.comments is true) or discard (discardChanges: true). When discarding, ask the user for confirmation and then call again with confirmDiscard: true. Prevents accidental data loss. Not for a project whose capabilities (openl_get_project) lack canClose: a project edited in place (a file repository in single-user mode) is never closed, as its files are the project itself.",
    schema: schemas.closeProjectSchema,
    annotations: {
      destructiveHint: true, // Can discard changes if requested
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const reply = (result: Record<string, unknown>): ToolResponse =>
        formatResult(result);
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
        return reply({ closed: true });
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
        return reply({ closed: true, saved: true });
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
          confirmationRequired: true,
          message: "The project has unsaved changes. Closing without saving will discard all changes permanently. Ask the user: 'Do you really want to close without saving? All unsaved changes will be lost.' If the user confirms, call openl_close_project again with the same projectId, discardChanges: true, and confirmDiscard: true (confirmDiscard must be set to true explicitly, not just provided).",
        });
      }
      await client.closeProject(args.projectId, { discardChanges: true });
      return reply({ closed: true, discarded: true });
    },
  });

  registerTool({
    name: "create_project_branch",
    title: "Create Project Branch",
    description:
      "Create a new branch in a project's repository from a specified revision. Allows branching from specific revisions, tags, or other branches. If no revision is specified, the HEAD revision will be used. Only for a repository with branches (features.branches of openl_list_repository_features; canManageBranches in the capabilities of openl_get_project): the folder of a file repository has none, a project edited in place included.",
    schema: schemas.createBranchSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.createBranch(args.projectId, args.branchName, args.revision);
      return formatResult({ created: args.branchName });
    },
  });

  registerTool({
    name: "create_project",
    title: "Create or Copy Project",
    description:
      "Create a new OpenL project in a design repository. Two modes, selected by the `template` argument:\n" +
      "• CREATE (omit `template`): create a BLANK project from the \"Empty Project\" template OpenL Studio ships, as a user of Studio does.\n" +
      "• COPY (pass `template` = an existing project's exact projectId from openl_list_projects): use Studio's server-side project-copy API to copy that source project's FULL structure and rename its descriptor to projectName. Do not pass the displayed project name because mapped repositories may contain multiple projects with the same name.\n" +
      "Both modes are written and indexed atomically. Omit `branch` for the repository's configured/default branch, or pass a target branch from openl_list_branches() when branches are supported; Studio also supports creating a missing branch from the base branch. Omit `comment` when repository features.comments is false. Answers the projectId, branch and revision of the new project. A name collision, missing copy source, or missing permission is rejected with an actionable error. Local repositories are not supported.",
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

      return formatResult({ projectId: created.id, branch: created.branch, revision: created.revision });
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
      return formatResult(modules);
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
      return formatResult(sheets);
    },
  });
}
