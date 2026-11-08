/**
 * Project tool handlers — list/get projects, project status, open/save/close,
 * create projects and project branches, and list the modules and worksheets of a project.
 */
import { ProtocolError, ProtocolErrorCode } from "@modelcontextprotocol/server";
import type { OpenLClient } from "../client.js";
import * as schemas from "../schemas.js";
import type * as Types from "../types.js";
import { isWorkbook, namesOfSet } from "../utils.js";
import { fitLines, formatStudioPage, formatWholeList, type Column } from "../markdown-list.js";
import { formatCount, tableLabel } from "../text.js";
import { isNotFoundError, isRequestTimeout } from "../http.js";
import { registerTool, formatResult, textResult, type ToolHandlerExtra, type ToolResponse } from "./common.js";
import { changedTablesIfCompared } from "./comparison.js";
import { refreshFromDisk, untilCompiled } from "./freshness.js";

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
 * The compile messages `openl_project_status` answers: none of a compilation still running, which has made no counts
 * yet, nor of an "ok" one unless a severity is asked for; else those of the severities asked for, errors first, so the
 * most actionable survive a cut (Studio sends them in id order). Unless `grouped` is false, the messages of one table
 * after its first of a severity are counted in `more` of the first: an error in one table makes every table that uses
 * it fail too, hundreds of messages of one cause. `limit` caps them.
 */
function answeredMessages(
  { compileState, compilation }: Types.ProjectStatusView,
  { severity, limit, grouped }: { severity?: ("ERROR" | "WARN" | "INFO")[]; limit?: number; grouped?: boolean },
): AnsweredMessage[] {
  const allowed = new Set<string>(severity);
  if (compileState === "compiling" || (compileState === "ok" && allowed.size === 0)) {
    return [];
  }
  const items = (compilation?.messages?.items ?? [])
    .filter((m) => allowed.size === 0 || (m.severity !== undefined && allowed.has(m.severity)))
    .toSorted((a, b) => severityRank(a.severity) - severityRank(b.severity));
  return (grouped === false ? items : groupedByTable(items)).slice(0, limit);
}

/** A compile message as `openl_project_status` answers it, with the count of the messages of its table it stands for. */
type AnsweredMessage = Types.DetailedMessageDescription & { more?: number };

/**
 * The first message of each table and severity, with the count of the others of them in `more`, in the order of the
 * first ones. A message that names no table stands for itself.
 */
function groupedByTable(messages: Types.DetailedMessageDescription[]): AnsweredMessage[] {
  const groups = Map.groupBy(messages, (message, index) => {
    const table = message.location?.id;
    return typeof table === "string" ? `${message.severity} ${table}` : index;
  });
  return groups.values().map(([first, ...rest]) => (rest.length > 0 ? { ...first, more: rest.length } : first)).toArray();
}

/**
 * The status with the tables each changed workbook holds otherwise than the saved revision, when they are asked for:
 * the pending changes name files only. Studio holds one comparison per client, so the workbooks are compared one by
 * one; a workbook whose comparison fails is named without its tables.
 */
async function withChangedTables(
  client: OpenLClient,
  status: Types.ProjectStatusView,
  asked: boolean | undefined,
  extra: ToolHandlerExtra | undefined,
): Promise<Types.ProjectStatusView> {
  const { pendingChanges, revision, branch, projectId } = status;
  if (!asked || !pendingChanges?.files.length || !revision) {
    return status;
  }
  // A pending change names its file by the path of the project in its repository, a comparison by the path in it
  const root = (await client.getProject(projectId)).path;
  const files = [];
  // One workbook after another: Studio keeps one comparison per client, and a second one cancels the first
  for (const file of pendingChanges.files) {
    if (file.type !== "modified" || !isWorkbook(file.path)) {
      files.push(file);
      continue;
    }
    const path = root && file.path.startsWith(`${root}/`) ? file.path.slice(root.length + 1) : file.path;
    const tables = await changedTablesIfCompared(client, // NOSONAR typescript:S9382
      () => client.compareProjectFiles(projectId, { path, branch, revision }, { path }), extra);
    files.push(tables ? { ...file, tables } : file);
  }
  return { ...status, pendingChanges: { ...pendingChanges, files } };
}

/** A place of a file by line and column, as Studio tells it: lines from 1, columns from 0, the end left out. */
interface FilePlace {
  line: number;
  column: number;
  endLine: number;
  endColumn: number;
}

/** A place of a file as an editor names it, columns from 1: `3:13`, or `3:11-3:20` for more than one character. */
function filePlaceLabel({ line, column, endLine, endColumn }: FilePlace): string {
  const start = `${line}:${column + 1}`;
  return endLine === line && endColumn <= column + 1 ? start : `${start}-${endLine}:${endColumn}`;
}

/**
 * The place a message about a localization file names: the file, where in it, and the key, as
 * `i18n/message_fr.properties 2:5, 2:12 total`; a message about the whole file names the file alone.
 */
function localizationPlaceOf(location: Record<string, unknown>): string {
  const { path, key, places } = location as { path?: string; key?: string; places?: FilePlace[] };
  return [path, places?.map(filePlaceLabel).join(", "), key].filter(Boolean).join(" ");
}

/**
 * The place a compile message names: its table by name and id, which the table tools take, with its cell, or else its
 * module; the project first when it is another than the one of the status. A message about a localization file names
 * the file and the key.
 */
function placeOf(location: Record<string, unknown> | undefined, statusProject: string): string {
  if (location?.type === "localization") {
    return localizationPlaceOf(location);
  }
  const { projectId, project, id, name, cell, module } = (location ?? {}) as Record<string, string | undefined>;
  const table = id === undefined ? "" : `${tableLabel(name ?? id, id)}${cell === undefined ? "" : ` ${cell}`}`;
  const place = table || (module === undefined ? "" : `module ${module}`);
  return project === undefined || projectId === statusProject ? place : `${project}: ${place}`;
}

/** The name of a language in the header of the table of the keys: its tag, the default file as `default`. */
const languageHeader = (tag: string): string => (tag === "" ? "default" : tag);

/**
 * What a key shows in a language: its own value, `""` for an empty one, `(no value)` for a key written without one,
 * `[Base] value` for the value a project it depends on gives, or nothing when the language falls back.
 */
function localizationCell(message: Types.LocalizationMessage, tag: string): string {
  const own = message.values[tag];
  if (own === null) {
    return "(no value)";
  }
  if (own !== undefined) {
    return own === "" ? '""' : own;
  }
  const shared = message.shared?.[tag];
  return shared === undefined ? "" : `[${shared.project}] ${shared.value}`;
}

/** The columns of the table of the keys: the key, a column per language, and the warnings by language. */
function localizationColumns(languages: Types.LocalizationLanguage[]): Array<Column<Types.LocalizationMessage>> {
  return [
    { header: "key", value: (message) => message.key },
    ...languages.map(({ tag }) => ({ header: languageHeader(tag), value: (message: Types.LocalizationMessage) => localizationCell(message, tag) })),
    {
      header: "warnings",
      value: (message) => message.warnings?.map(({ language, summary }) => `${languageHeader(language)}: ${summary}`).join("; "),
    },
  ];
}

/** What the cells of the page need explained, and the files a change writes whole in UTF-8. */
function localizationNotes(languages: Types.LocalizationLanguage[], messages: Types.LocalizationMessage[]): string[] {
  const falls = messages.some((message) => languages.some(({ tag }) => localizationCell(message, tag) === ""));
  const legacy = languages.filter((language) => language.legacy).map(({ tag }) => languageHeader(tag));
  return [
    ...(falls ? ["An empty cell falls back to the parent language, then to the default one; `[project]` marks the value of a project it depends on."] : []),
    ...(legacy.length > 0 ? [`Not in UTF-8: ${legacy.join(", ")}; the first change writes such a file whole in UTF-8.`] : []),
  ];
}

/** An argument the schema has checked the action takes. */
function given<T>(value: T | undefined, name: string): T {
  if (value === undefined) {
    throw new ProtocolError(ProtocolErrorCode.InvalidParams, `The action takes '${name}'.`);
  }
  return value;
}

/** A change of the localization files as OpenL Studio takes it. */
function localizationAction({ action, key, language, value, values }: schemas.LocalizationEdit): Types.LocalizationAction {
  switch (action) {
    case "set_value":
      return { operation: "setValue", key: given(key, "key"), language: given(language, "language"), value: given(value, "value") };
    case "remove_value":
      return { operation: "removeValue", key: given(key, "key"), language: given(language, "language") };
    case "add_key":
      return { operation: "addKey", key: given(key, "key"), values: given(values, "values") };
    case "remove_key":
      return { operation: "removeKey", key: given(key, "key") };
    case "add_language":
      return { operation: "addLanguage", language: given(language, "language"), ...(values === undefined ? {} : { values }) };
  }
}

/** A compile message on one line: `ERROR RateCalc (T1) B2: Need to close '(' (+2 more)`. */
function messageLine({ severity, summary, location, more }: AnsweredMessage, statusProject: string): string {
  const place = placeOf(location, statusProject);
  return `${severity ?? "INFO"}${place ? ` ${place}` : ""}: ${summary ?? ""}${more ? ` (+${more} more)` : ""}`;
}

/** A file changed since the last save on one line, with the tables a changed workbook holds otherwise. */
function changeLine({ path, type, tables }: Types.FileChange): string {
  const changed = (tables ?? []).map(({ sheet, table, status }) => `${[sheet, table].filter(Boolean).join("/")} ${status ?? ""}`.trim());
  return `- ${path} ${type}${changed.length ? `: ${changed.join(", ")}` : ""}`;
}

/**
 * The status as text, which an agent reads after every edit: the state and its counts, the branch and the last change,
 * the files changed since the last save, then the messages answered, a line each, cut by whole lines to the size of an
 * answer, and the note of the state. JSON spent most of a status on keys, a message on six of them.
 */
function statusText(status: Types.ProjectStatusView, messages: AnsweredMessage[], note: string | undefined): string {
  const { projectId, compileState, compilation, branch, revision, lastModifiedBy, pendingChanges } = status;
  // Zero errors of a compilation that has not ended would read as a project that compiles
  const counted = compileState === "compiling" ? undefined : compilation;
  const counts = [
    counted?.messages && `${formatCount(counted.messages.errors, "error")}, ${formatCount(counted.messages.warnings, "warning")}`,
    // Studio counts the test tables, not the tests they hold
    counted?.tests && formatCount(counted.tests.total, "test table"),
    compilation?.modules && `${compilation.modules.compiled} of ${formatCount(compilation.modules.total, "module")} compiled`,
  ].filter(Boolean);
  const version = [
    branch && `Branch: ${branch}.`,
    revision && `Revision: ${revision}.`,
    lastModifiedBy && `Last change: ${lastModifiedBy.author}, ${lastModifiedBy.date}.`,
  ].filter(Boolean).join(" ");
  return fitLines({
    head: [
      `Compile state: ${compileState}${counts.length ? `; ${counts.join("; ")}` : ""}.`,
      ...(version ? [version] : []),
      ...(pendingChanges?.files.length
        ? [`Changed since the last save, ${formatCount(pendingChanges.total, "file")}:`, ...pendingChanges.files.map(changeLine)]
        : []),
      ...(messages.length ? ["", `Messages, errors first, ${messages.length} of ${compilation?.messages?.total}:`] : []),
    ],
    lines: messages.map((message) => messageLine(message, projectId)),
    tail: note ? ["", note] : [],
    more: (left) => `… ${formatCount(left, "more message")}: choose them with severity or limit.`,
  });
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
 * compiled yet (`idle`) is compiled through the tables API, which answers once the compilation ends, and a compilation
 * that is running, after an edit for one, is waited for: both up to `timeoutMs`.
 *
 * A compilation that outlasts `timeoutMs` is reported as it is: `compiling`.
 *
 * A project whose files another program changed since Studio compiled them is compiled again from those files, waiting
 * for the compilation up to `timeoutMs` as for an idle project: the status that is answered tells `refreshed`.
 */
async function readProjectStatus(
  client: OpenLClient,
  projectId: string,
  branch: string | undefined,
  options: { signal?: AbortSignal; timeoutMs: number },
): Promise<Types.ProjectStatusView & { refreshed?: true }> {
  const deadline = Date.now() + options.timeoutMs;
  let status = await client.getProjectStatus(projectId);
  if (branch && status.branch && branch !== status.branch) {
    await client.openOnBranch(projectId, branch);
    status = await client.getProjectStatus(projectId);
  }
  if (status.changedOnDisk && await refreshFromDisk(client, projectId, options.signal, options.timeoutMs)) {
    return { ...(await client.getProjectStatus(projectId)), refreshed: true };
  }
  if (status.compileState === "idle") {
    try {
      await client.triggerProjectCompilation(projectId, options);
    } catch (error) {
      // Studio goes on compiling after the wait gives up; the status read below reports it as running.
      if (!isRequestTimeout(error)) {
        throw error;
      }
    }
    // The compilation ended, or outlasted timeoutMs
    return client.getProjectStatus(projectId);
  }
  if (status.compileState !== "compiling") {
    return status;
  }
  await untilCompiled(client, projectId, deadline, options.signal);
  return client.getProjectStatus(projectId);
}

/** A project as the project tools answer it, with its capabilities by name and a compile status that may be partial. */
type AnsweredProject = Omit<Types.ProjectViewModel, "compileStatus" | "capabilities"> & {
  compileStatus?: Partial<Types.ProjectStatusView>;
  capabilities?: string[];
};

/**
 * A project as the project tools answer it: its capabilities as a list of names, and its compile status without the
 * id, the branch, the revision and the last change the project names already, which an agent would otherwise read
 * twice; a value that differs stays. Without its `details` it leaves out where it lives, its tags, its last comment and
 * its default-branch flags, and names the projects using it by their ids and names.
 */
function answeredProject({ capabilities, ...whole }: Types.ProjectViewModel, details: boolean): AnsweredProject {
  const { path: _path, tags: _tags, comment: _comment, branchDefault: _default, inDefaultBranch: _inDefault, usedBy, ...brief } = whole;
  const project = details ? whole : { ...brief, ...(usedBy ? { usedBy: usedBy.map(({ id, name }) => ({ id, name })) } : {}) };
  const answered = { ...project, ...(capabilities ? { capabilities: namesOfSet(capabilities) } : {}) };
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

/** The includes of a project tool that Studio takes, if any: `details`, the details of a project, is the tool's own. */
function studioIncludes<T extends string>(include: Array<T | "details"> | undefined): T[] | undefined {
  const studios = include?.filter((name): name is T => name !== "details");
  return studios?.length ? studios : undefined;
}

/** The last change of a project; Studio may leave its time or its author out. */
const MODIFIED_COLUMN: Column<Types.ProjectViewModel> = {
  header: "modified",
  value: (project) => [project.modifiedAt, project.modifiedBy && `by ${project.modifiedBy}`].filter(Boolean).join(" "),
};

/**
 * A note naming the repositories listed, all or `repositoryId`, whose cross-branch project index Studio has not
 * finished or could not build: the list may miss their projects of other branches, and a branch that failed lists its
 * projects as Studio last indexed them. None while every index is ready.
 */
function unreadyIndexNote(health: unknown, repositoryId?: string): string | undefined {
  const unready = Object.entries((health ?? {}) as Record<string, { state?: string; failedBranches?: string[] }>)
    .filter(([repository, index]) => index?.state !== "ready" && (!repositoryId || repository === repositoryId));
  if (unready.length === 0) {
    return undefined;
  }
  // Each repository names its own branches that failed
  const failed = (branches: string[] | undefined): string => (branches?.length ? `, failed: ${branches.join(", ")}` : "");
  const named = unready.map(([repository, index]) => `${repository} (${index?.state ?? "unknown"}${failed(index?.failedBranches)})`);
  return `The project index of ${named.join(", ")} is not ready: projects of other branches may be missing`
    + (unready.some(([, index]) => index?.failedBranches?.length) ? ", and a branch that failed lists its projects as last indexed." : ".");
}

export function registerProjectHandlers(): void {
  registerTool({
    name: "list_projects",
    title: "List Projects",
    description:
      "List projects as a table, the first line naming the offset of the next page. Its projectId is what the other tools take, as it is; openl_get_project tells the capabilities. Filtering by repository 'local' may fail: list without it and pick those. A project of repository 'local', or edited in place (no canOpen, canSave, canClose), is never opened, saved or closed and has no branches.",
    schema: schemas.listProjectsSchema,
    aliases: { search: "name" },
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const { repository, ...filters } = args;
      // The client may name the repository; Studio takes its id.
      const repositoryId = repository ? await client.getRepositoryIdByName(repository) : undefined;
      const projectsPage = await client.listProjectsPage({ ...filters, include: studioIncludes(filters.include), repository: repositoryId });
      const { projectIndexHealth, ...summary } = projectsPage.metadata ?? {};
      // Each project carries its opaque id as projectId, the name the other tools take it by; the capabilities of a
      // project are told by openl_get_project
      const columns: Array<Column<Types.ProjectViewModel>> = [
        { header: "projectId", value: (project) => project.id },
        { header: "name", value: (project) => project.name },
        { header: "repository", value: (project) => project.repository },
        { header: "branch", value: (project) => project.branch },
        { header: "status", value: (project) => project.status },
        // The last change, a third of each row, is read only by a list sorted by it, or asked for
        ...(filters.sort === "updated" || filters.include?.includes("details") ? [MODIFIED_COLUMN] : []),
        ...(filters.include?.includes("status")
          ? [{ header: "compileState", value: (project: Types.ProjectViewModel) => project.compileStatus?.compileState }]
          : []),
      ];
      return textResult(formatStudioPage(projectsPage, filters.offset, {
        noun: "projects",
        columns,
        notes: [
          Object.keys(summary).length > 0 ? `Summary: ${JSON.stringify(summary)}` : undefined,
          unreadyIndexNote(projectIndexHealth, repositoryId),
        ].filter((note) => note !== undefined),
      }));
    },
  });

  registerTool({
    name: "get_project",
    title: "Get Project Details",
    description:
      "Get a project: its name, branch, status, repository, last change, revision and 'capabilities', the operations it takes. canOpen, canSave, canClose and canManageBranches are left out when not available; a project edited in place (a file repository in single-user mode) never has the first three, as it is always open and its edits are in its files.",
    schema: schemas.getProjectSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const project = await client.getProject(args.projectId, studioIncludes(args.include));

      const { id, ...answered } = answeredProject(project, args.include?.includes("details") === true);
      return formatResult({ projectId: id, ...answered });
    },
  });

  registerTool({
    name: "project_status",
    title: "Get Project Status",
    description:
      "Get the compile state of a project, its messages, its files changed since the last save, and its module and test counts. A project not compiled yet (idle) is compiled first and a running compilation waited for, up to timeoutMs; one that outlasts it is answered as compiling, without counts: call again.",
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
      const changes = await withChangedTables(client, status, args.changes, extra);
      const note = status.refreshed ? CHANGED_ON_DISK_NOTE : STATUS_NOTES[status.compileState];
      return textResult(statusText(changes, answeredMessages(status, args), note));
    },
  });

  registerTool({
    name: "project_localization_problems",
    title: "Check Project Localization",
    description:
      "Check the localization files (i18n/message*.properties) of an opened project without compiling it: a line per problem with the file, its places as line:column from 1, and the key. openl_project_status checks the calls of msg.",
    schema: schemas.projectLocalizationProblemsSchema,
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const problems = await client.getLocalizationProblems(args.projectId);
      if (problems.length === 0) {
        return textResult("No problems in the localization files.");
      }
      return textResult(fitLines({
        head: [`${formatCount(problems.length, "problem")} in the localization files:`],
        lines: problems.map((problem) => messageLine(problem, args.projectId)),
        more: (left) => `… ${formatCount(left, "more problem")}.`,
      }));
    },
  });

  registerTool({
    name: "project_localization_messages",
    title: "Read Project Localization",
    description:
      "Read the keys of the localization files of an opened project a page at a time: a row per key with its value in each language and its warnings.",
    schema: schemas.projectLocalizationMessagesSchema,
    aliases: { query: "search", text: "search" },
    annotations: {
      readOnlyHint: true,
      openWorldHint: true,
      idempotentHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const page = await client.getLocalizationMessages(args.projectId, args);
      const languages = (page.metadata?.languages ?? []) as Types.LocalizationLanguage[];
      if (languages.length === 0) {
        return textResult("The project has no localization files: add the default one with openl_project_localization_edit add_language ''.");
      }
      if (args.key !== undefined && page.items.length === 0) {
        return textResult(`No language of the project, nor of a project it depends on, has the key '${args.key}'.`);
      }
      return textResult(formatStudioPage(page, args.offset, {
        noun: "keys",
        columns: localizationColumns(languages),
        notes: localizationNotes(languages, page.items),
      }));
    },
  });

  registerTool({
    name: "project_localization_edit",
    title: "Edit Project Localization",
    description:
      "Change the localization files of an opened project by key in one write; comments and other lines stay, new keys go to the end. One refused change refuses all. Example: actions [{action:'add_key', key:'note', values:{'':'A note', de:'Eine Notiz'}}].",
    schema: schemas.editProjectLocalizationSchema,
    aliasedLists: ["actions"],
    aliases: { operation: "action", text: "value", texts: "values" },
    // The operations as the REST API of Studio names them
    valueAliases: {
      setvalue: "set_value", removevalue: "remove_value", addkey: "add_key", removekey: "remove_key", addlanguage: "add_language",
    },
    annotations: {
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.applyLocalizationActions(args.projectId, args.actions.map(localizationAction));
      return textResult(`${formatCount(args.actions.length, "change")} written to the localization files.`);
    },
  });

  registerTool({
    name: "open_project",
    title: "Open Project for Editing",
    description:
      "Open a project on a branch or at a Git revision for viewing; an open project stays as it is unless one is asked. A project without canOpen in openl_get_project (edited in place) is always open.",
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
      "Save the unsaved edits of a project (status EDITING) as a new revision; the project becomes OPENED.",
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
      "Close a project, saving or discarding its unsaved changes. Not for a project without canClose in openl_get_project (edited in place).",
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
        await client.saveProject(args.projectId, args.comment, { closeAfterSave: true });
        return reply({ closed: true, saved: true });
      }

      if (args.discardChanges !== true) {
        throw new ProtocolError(
          ProtocolErrorCode.InvalidParams,
          "Project has unsaved changes. You must either:\n" +
          "1. Set saveChanges: true (with comment if supported) to save and close\n" +
          "2. Set discardChanges: true to close without saving, once the user agrees to lose the changes"
        );
      }
      // discardChanges is the confirmation: the tool is marked destructive for the client to ask the user too
      await client.closeProject(args.projectId, { discardChanges: true });
      return reply({ closed: true, discarded: true });
    },
  });

  registerTool({
    name: "create_project_branch",
    title: "Create Project Branch",
    description:
      "Create a branch in the repository of a project. Only for a repository with branches (features.branches of openl_list_repositories, canManageBranches of openl_get_project); a file repository has none.",
    schema: schemas.createBranchSchema,
    annotations: {
      destructiveHint: false,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      await client.createBranch(args.projectId, args.branch, args.revision);
      return formatResult({ created: args.branch });
    },
  });

  registerTool({
    name: "create_project",
    title: "Create or Copy Project",
    description:
      "Create a project in a design repository, blank or as a copy of the template project renamed to name. Answers its projectId, branch and revision.",
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
              args.name,
              source,
              { comment: args.comment, branch: args.branch },
            )
          : await client.createEmptyProject(
              repositoryId,
              args.name,
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
      "List the modules of a project with their rules paths or patterns, and the modules a pattern matches. A tool that takes a module takes the name answered.",
    schema: schemas.projectOnlySchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const modules = await client.listProjectModules(args.projectId);
      return textResult(formatWholeList(modules, {
        noun: "modules",
        columns: [
          { header: "module", value: (module) => module.name },
          { header: "path", value: (module) => module.path },
        ],
      }));
    },
  });

  registerTool({
    name: "list_module_sheets",
    title: "List Module Worksheets",
    description:
      "List the worksheets of a module, the destination sheets of a table created or copied.",
    schema: schemas.projectModuleSchema,
    annotations: {
      readOnlyHint: true,
      idempotentHint: true,
      openWorldHint: true,
    },
    handler: async (args, client): Promise<ToolResponse> => {
      const sheets = await client.listModuleSheets(args.projectId, args.module);
      return textResult(formatWholeList(sheets, { noun: "sheets", columns: [{ header: "sheet", value: (sheet) => sheet }] }));
    },
  });
}
