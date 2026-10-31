/**
 * The Zod schemas of the tool arguments: each is a strict object, so an unknown argument is refused, and describes its
 * fields for the JSON Schema the tool advertises.
 */

import { z } from "zod";
import { parseJsonOrText } from "./http.js";
import {
  BRANCH_SCOPES, GUIDE_TYPES, MERGE_MODES, PROJECT_INCLUDES, PROJECT_STATUSES, RESPONSE_FORMATS, TABLE_KINDS,
  TABLE_SCOPES, TABLE_SORTS,
} from "./types.js";

/** What the compile state of `include: status` does not say: an idle project is not compiled yet. */
const STATUS_INCLUDE_NOTE =
  "'status' adds compileState: 'idle' means Studio has not compiled the project yet, not that it compiles; " +
  "openl_project_status compiles it and reports its errors.";


const ResponseFormat = z
  .enum(RESPONSE_FORMATS)
  .default("json")
  .describe(
    "Response format: 'json' for structured, round-trippable data (default), 'markdown' for human-readable output, 'markdown_concise' for a brief summary (1-2 paragraphs), or 'markdown_detailed' for full details with context"
  );

// Pagination parameters, spread last into the arguments of a paged tool
const paginationShape = {
  limit: z.number().int().positive().max(200).default(50),
  offset: z.number().int().nonnegative().default(0),
};

// The ids take a short description, as most tools take one of them and every agent loads their schemas every session
export const projectIdSchema = z.string().min(1, "Project ID must not be empty.").describe("The 'projectId' openl_list_projects answered, as it is.");

const repositoryNameSchema = z.string().describe("The 'id' or 'name' of a repository, as openl_list_repositories answers them; never a made-up one.");

/** A string with more than whitespace in it, trimmed. */
const nonBlankString = z.string().trim().min(1);

const tableIdSchema = z.string().describe("The 'id' of the table openl_list_tables answered, or the 'tableId' of its last edit.");

const branchNameSchema = z.string().describe("Git branch name (e.g., 'main', 'development', 'feature/new-rules')");

const commentSchema = z.string().optional().describe("Commit comment describing the change (e.g., 'Updated CA premium rates', 'Fixed calculation bug')");

/** The most characters of a commit comment Studio keeps: a database repository stores it in a column of 255. */
const MAX_COMMIT_COMMENT_LENGTH = 255;

const commitCommentTooLong = `A commit comment has at most ${MAX_COMMIT_COMMENT_LENGTH} characters.`;

/** A commit comment of a design project, which Studio refuses when it is longer than it keeps. */
const commitCommentSchema = z.string().max(MAX_COMMIT_COMMENT_LENGTH, commitCommentTooLong);

/** A tool that lists a page of a list the server holds whole: deploy repositories. */
export const pagedListSchema = z.strictObject({
  ...paginationShape,
});

/** The design repositories, a page of a list the server holds whole. */
export const listRepositoriesSchema = z.strictObject({
  ...paginationShape,
});

export const listDeploymentsSchema = z.strictObject({
  repository: z.string().optional().describe("The id of a production repository to filter deployments by."),
  project: z.string().optional().describe("The exact name of a deployed project to filter deployments by; it adds the revisions and authors of its deployments."),
  ...paginationShape,
});

export const listProjectsSchema = z.strictObject({
  repository: repositoryNameSchema.optional(),
  status: z.enum(PROJECT_STATUSES).optional().describe("Filter by project status."),
  dependsOn: z.string().optional().describe("The projectId of a project: return the projects that depend on it."),
  name: z.string().optional().describe("Project name filter (partial, case-insensitive)."),
  author: z.string().optional().describe("Last-modifying author filter (partial, case-insensitive)."),
  branch: z.string().optional().describe("Branch filter (partial, case-insensitive)."),
  sort: z.enum(["name", "status", "updated"]).optional().describe("Field used to sort the returned page."),
  include: z.array(z.enum(PROJECT_INCLUDES)).optional().describe(`Optional response expansions and listing behavior from the Studio API. ${STATUS_INCLUDE_NOTE}`),
  tags: z.record(z.string(), z.string()).optional().describe("Filter by project tags. Tags must be prefixed with 'tags.' in the query string (e.g., tags.version='1.0', tags.environment='production'). This is handled automatically by the API client - provide as object with tag names as keys."),
  ...paginationShape,
});

export const getProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  include: z.array(z.enum(["status", "descriptor"])).optional().describe(`What to add: the compile status (status) or the rules.xml descriptor (descriptor). ${STATUS_INCLUDE_NOTE}`),
});

export const projectStatusSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: branchNameSchema.optional().describe(
    "Optional target branch: the tool puts the project on this branch before reading its state, switching an opened project and opening a closed one. Omit for repositories that do not support branches and for repository 'local'."
  ),
  timeoutMs: z.number().int().positive().max(600000).default(30000).describe(
    "Max time in milliseconds to wait for Studio to compile an idle project. On expiry, the status is returned as compiling (no error). Default 30000 (30 seconds). Cap 600000 (10 minutes)."
  ),
  severity: z.array(z.enum(["ERROR", "WARN", "INFO"])).optional().describe(
    "Filter compilation.messages.items to only these severities. Useful when the project has many warnings and you want to isolate errors: pass severity: ['ERROR']. Default: all severities. Note: items are always sorted ERROR → WARN → INFO before any filter or truncation is applied, so errors are visible without this filter."
  ),
  limit: z.number().int().positive().max(1000).optional().describe(
    "Cap the number of items returned in compilation.messages.items. The list is sorted ERROR → WARN → INFO first so the most actionable items are preserved when capped. Pair with severity to bound very large diagnostic lists. Default: no cap; an answer over the size limit keeps the first messages, errors first, and tells how many it holds."
  ),
});

export const openProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: branchNameSchema.optional().describe("Open project on a specific Git branch (e.g., 'main', 'development', 'feature/new-rules')"),
  revision: z.string().optional().describe("Open project at a specific Git revision/commit hash for read-only viewing"),
  openDependencies: z.boolean().optional().describe("Also open dependency projects (backend default false)."),
});

export const closeProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  saveChanges: z.boolean().optional().describe("If true, save changes before closing. If false or omitted and project has unsaved changes, will error unless discardChanges is true."),
  comment: commitCommentSchema.optional().describe("Commit comment of at most 255 characters for saveChanges, ignored by a repository without comments. Omit it for the comment the repository configures for a save, such as 'Project X is saved.'"),
  discardChanges: z.boolean().optional().describe("If true, close without saving (unsaved changes will be lost). When project is EDITING, you must also set confirmDiscard: true to confirm."),
  confirmDiscard: z.boolean().optional().describe("When closing with discardChanges: true and project has unsaved changes, must be set to true (explicitly) to confirm. Omitted or false will return a confirmation prompt. Ask the user first, then call again with confirmDiscard: true."),
});

export const listTablesSchema = z.strictObject({
  projectId: projectIdSchema,
  kind: z.union([z.enum(TABLE_KINDS), z.array(z.enum(TABLE_KINDS))]).optional()
    .transform((kind) => (typeof kind === "string" ? [kind] : kind))
    .describe("Filter by a table kind or a list of them. Omit to show all kinds."),
  name: z.string().optional().describe("Filter by table name fragment (e.g., 'calculate', 'Premium'). Omit to show all tables."),
  text: nonBlankString.optional().describe("Text any cell of a table contains, case-insensitive: a value, a key, a message, a formula. The way to find where a value is used, which file search cannot, as it never reads a workbook cell."),
  header: nonBlankString.optional().describe("Text the header line of a table contains: its keyword, return type, name and arguments, such as a datatype the rules take."),
  module: nonBlankString.optional().describe("The module whose tables to list, as openl_list_project_modules names it."),
  scope: z.enum(TABLE_SCOPES).optional().describe("How wide the search reaches: 'module' (the module named), 'project' (every module) or 'all' (the projects this one depends on as well); the module when one is named, else the project."),
  sort: z.enum(TABLE_SORTS).optional().describe("'name' (default) or 'position': workbook by workbook, sheet by sheet, top to bottom."),
  properties: z.record(z.string(), z.string()).optional().describe("Filter by table properties, such as { state: 'CA', lob: 'Auto' }."),
  ...paginationShape,
});

export const getTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  find: nonBlankString.optional().describe("Read only the rows with a cell whose text contains this, case-insensitive, such as a key or a value, with the header rows; 'rowIndexes' gives the position of each row read. A number is found by its digits, '900' finding 900."),
  offset: z.number().int().min(0).optional().describe("Zero-based index of the first row to read; omit to start at the top. Combine with limit to read a large table in slices: the next slice starts at window.next_offset."),
  limit: z.number().int().min(1).optional().describe("Maximum number of rows, counted from offset; omit to read to the end. A slice holds exactly these rows: a merged cell that its edge cuts keeps its value in the first row of the slice, and merges names its whole area."),
  styles: z.boolean().optional().describe("If true, 'styles' lists the Excel style of each styled cell (background/font colour, bold/italic/underline, alignment, indent, borders)."),
}).refine((args) => args.find === undefined || (args.offset === undefined && args.limit === undefined), {
  error: "find searches the whole table: it takes no offset or limit.",
  path: ["find"],
});

/** The version of a table a write is based on, as openl_get_table or the previous edit answered it. */
const tableVersionSchema = nonBlankString.describe(
  "The 'version' of the table that openl_get_table, or the previous edit of the table, answered. Studio refuses the " +
  "write when the table changed since — on disk, such as in Excel, or through another client — and writes nothing: " +
  "read the table again and repeat the edit on what it holds now.",
);

const tableActionBase = {
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  version: tableVersionSchema,
};

/** A tool that names one table: its project and its id. */
export const tableOnlySchema = z.strictObject(tableActionBase);

/** Whether a string holds a JSON object or array, the inputs a method takes. */
function isJsonInput(text: string): boolean {
  const value = parseJsonOrText(text);
  return typeof value === "object" && value !== null;
}

export const runTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID of a regular executable table. Use openl_list_tables() to discover it; use the Tests tools instead for Test tables."),
  inputJson: z.union([
    z.string().refine(isJsonInput, { error: "inputJson as a string must be a JSON object or array" }),
    z.array(z.unknown()),
    z.record(z.string(), z.unknown()),
  ]).describe("Method input as JSON, or the same as a JSON string: an object keyed by method parameter name; { params, runtimeContext? } with params keyed by name or listed in order; or an array of the argument values in order. For a method whose only parameter is an array, a top-level array is that parameter's value. The value is sent to Studio unchanged."),
  module: nonBlankString.optional().describe("Optional module name whose runtime context should be used. Usually omit; discover module names with openl_list_project_modules()."),
  withSchema: z.boolean().optional().describe("Also answer the JSON Schema of each parameter: its fields and the values each vocabulary allows."),
  timeoutMs: z.number().int().positive().max(600000).default(120000).describe("Maximum time for the complete Studio start-and-result workflow, in milliseconds. Default 120000 (2 minutes), maximum 600000 (10 minutes). A timeout cancels the pending Studio run."),
});

export const getTableDependenciesSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("Optional table whose dependency neighborhood to return. Omit to return the whole project (or module) graph."),
  module: nonBlankString.optional().describe("When tableId is omitted, limit the project graph to this module. Discover names with openl_list_project_modules()."),
  layer: z.enum(["executable", "datatype", "all"]).optional().describe("When tableId is omitted, return executable tables, datatype/vocabulary nodes, or both (backend default all)."),
  direction: z.enum(["DEPENDENCIES", "DEPENDENTS", "BOTH"]).optional().describe("When tableId is provided, relations to traverse. Default BOTH: the tables it calls and the tables calling it."),
  depth: z.number().int().min(1).optional().describe("When tableId is provided, maximum traversal depth from that table."),
  response_format: ResponseFormat,
}).superRefine((value, ctx) => {
  if (value.tableId) {
    if (value.module) {
      ctx.addIssue({ code: "custom", path: ["module"], message: "module cannot be combined with tableId; it applies only to the whole-project graph." });
    }
    if (value.layer) {
      ctx.addIssue({ code: "custom", path: ["layer"], message: "layer cannot be combined with tableId; the root table determines the graph layer." });
    }
  }
  if (!value.tableId && (value.direction || value.depth !== undefined)) {
    ctx.addIssue({ code: "custom", path: [value.direction ? "direction" : "depth"], message: "direction and depth require tableId." });
  }
});

/** The arguments of a tool that names only the project. */
export const projectOnlySchema = z.strictObject({
  projectId: projectIdSchema,
});

/**
 * A tool that names one module of a project: its sheets, its local history. Studio can default an omitted module to
 * the first descriptor entry, but such a tool works on a specific module, so the module is required.
 */
export const projectModuleSchema = z.strictObject({
  projectId: projectIdSchema,
  module: nonBlankString.describe("Module name exactly as returned by openl_list_project_modules()."),
});

export const listTablePropertyDefinitionsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableType: z.enum(TABLE_KINDS).optional().describe("A table kind, for the properties allowed on it; omit for those allowed in a Properties table."),
});

export const copyTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("ID of the source table to copy."),
  module: nonBlankString.describe("Name of the destination module. It must already exist unless modulePath is supplied."),
  modulePath: z.string().min(6).regex(/\.xlsx$/i).optional().describe("Project-relative .xlsx path for a new destination module. Omit when module already exists."),
  name: nonBlankString.describe("Name for the copied table."),
  sheetName: z.string().optional().describe("Destination worksheet name. Defaults to the copied table's name."),
  properties: z.array(z.strictObject({
    name: nonBlankString.describe("Property name."),
    value: z.string().optional().describe("Property value in display-string form. A blank value removes the property."),
  })).optional().describe("Replacement table properties. Omit to retain the source table's properties."),
});

const scalarCellValueSchema = z.union([z.string(), z.number(), z.boolean(), z.null()]);

/**
 * The raw cell value domain of the Studio API: a scalar, or a one-dimensional array of scalars. Studio refuses an array
 * it cannot store as it is: an empty one, a lone null, or a string element it would trim.
 */
const cellValueSchema = z.union([scalarCellValueSchema, z.array(scalarCellValueSchema)], {
  error: "Cell value must be a string, number, boolean, null, or a one-dimensional array of those scalar values.",
});

/** The Excel formula of a cell of an edit, which names it instead of a value. */
const excelFormulaSchema = z.string().optional().describe("Excel formula of the cell as Excel writes it, such as '=B2*C2'. The cell is written as this Excel formula; send it without 'value', which Studio refuses beside it. An OpenL formula is a 'value' starting with '='.");

/** A cell of an edit names either its value or its Excel formula, as Studio refuses both. */
const VALUE_OR_FORMULA = "A cell takes either 'value' or 'formula', not both.";
const valueOrFormula = (cell: { value?: unknown; formula?: string }): boolean =>
  cell.formula === undefined || cell.value === undefined || cell.value === null;

/** A row or a column of a table, of a position from 0 in the whole table. */
const positionSchema = z.number().int().min(0);

/** The cells of a whole table, as openl_get_table answers them: its rows, its merged cells and its Excel formulas. */
const tableGridShape = {
  rows: z.array(z.array(cellValueSchema)).describe("Every row of the table, top to bottom, one value per column: a scalar, a one-dimensional array of scalars, or null for an empty cell or one a merged cell covers. A text starting with '=' is an OpenL formula."),
  merges: z.array(z.tuple([positionSchema, positionSchema, z.number().int().min(1), z.number().int().min(1)]))
    .optional().describe("The merged cells, each as [row, column, rowspan, colspan] from 0; the value of a merged cell is at its first row and column."),
  formulas: z.array(z.tuple([positionSchema, positionSchema, z.string().startsWith("=")]))
    .optional().describe("The Excel formulas, each as [row, column, '=B2*C2']: the cell is written as the formula, whatever its value in rows."),
};

// =============================================================================
// Raw table-source edits (POST /projects/{projectId}/tables/{tableId}/actions)
//
// One tool for every in-place edit of a table's raw source, as Studio takes them at one endpoint: the action names
// the edit, and each action takes its own arguments.
// =============================================================================

/** The edits of openl_edit_table, by the arguments each takes; a name ending in `?` may be left out. */
export const EDIT_ACTIONS = {
  append_rows: ["cells"],
  append_columns: ["cells"],
  insert_rows: ["position", "cells"],
  insert_columns: ["position", "cells"],
  delete_rows: ["position", "count?"],
  delete_columns: ["position", "count?"],
  update_row: ["position", "cells"],
  update_column: ["position", "cells"],
  update_cell: ["row", "column", "value?", "formula?"],
  update_range: ["row", "column", "cells"],
  merge: ["row", "column", "rowspan", "colspan"],
  unmerge: ["row", "column"],
} as const;

export type EditAction = keyof typeof EDIT_ACTIONS;

const EDIT_ARGUMENTS = ["position", "count", "row", "column", "rowspan", "colspan", "cells", "value", "formula"] as const;

/** The actions that add or remove whole rows or columns, where the header row and the label column stay. */
const LINE_ACTIONS: ReadonlySet<EditAction> = new Set([
  "insert_rows", "insert_columns", "delete_rows", "delete_columns",
]);

type EditArguments = Partial<Record<(typeof EDIT_ARGUMENTS)[number], unknown>> & {
  action: EditAction;
  position?: number;
  rowspan?: number;
  colspan?: number;
  cells?: unknown[][];
  formula?: string;
};

/** Why an action refuses an argument: it needs the argument left out, or does not take the one given. */
function argumentIssue(action: EditAction, name: string, present: boolean): string | undefined {
  const takes = EDIT_ACTIONS[action] as readonly string[];
  if (!present && takes.includes(name)) {
    return `'${action}' needs '${name}'.`;
  }
  const refused = present && !takes.includes(name) && !takes.includes(`${name}?`);
  return refused ? `'${action}' does not take '${name}'.` : undefined;
}

/** Why `update_cell` refuses what it sets the cell to: neither a value nor a formula, or both. */
function cellContentIssue(data: EditArguments): { path: string; message: string } | undefined {
  if (data.value === undefined && data.formula === undefined) {
    return { path: "value", message: "'update_cell' needs 'value', null to clear the cell, or 'formula'." };
  }
  return valueOrFormula(data) ? undefined : { path: "formula", message: VALUE_OR_FORMULA };
}

/** The arguments an edit takes: each it needs, none it does not, and the bounds of its action. */
function checkEditArguments(data: EditArguments, ctx: z.RefinementCtx): void {
  for (const name of EDIT_ARGUMENTS) {
    const message = argumentIssue(data.action, name, data[name] !== undefined);
    if (message) {
      ctx.addIssue({ code: "custom", path: [name], message });
    }
  }
  if (LINE_ACTIONS.has(data.action) && data.position === 0) {
    ctx.addIssue({ code: "custom", path: ["position"], message: "Row 0 and column 0 stay: 'position' starts at 1." });
  }
  if ((data.action === "update_row" || data.action === "update_column") && data.cells && data.cells.length !== 1) {
    ctx.addIssue({ code: "custom", path: ["cells"], message: `'${data.action}' takes one line of cells.` });
  }
  const content = data.action === "update_cell" ? cellContentIssue(data) : undefined;
  if (content) {
    ctx.addIssue({ code: "custom", path: [content.path], message: content.message });
  }
  if (data.action === "merge" && (data.rowspan ?? 1) * (data.colspan ?? 1) === 1) {
    ctx.addIssue({ code: "custom", path: ["rowspan"], message: "A merge must cover more than one cell." });
  }
  if (data.action === "update_range" && data.cells?.length === 1 && data.cells[0].length === 1) {
    ctx.addIssue({
      code: "custom", path: ["cells"], message: "A range must cover more than one cell: use 'update_cell' for one.",
    });
  }
}

/** The arguments of one edit of a table, of the action it names. */
const editShape = {
  action: z.enum(Object.keys(EDIT_ACTIONS) as [EditAction, ...EditAction[]]).describe("The edit to make."),
  position: z.number().int().min(0).optional().describe("The row or column the action starts at."),
  count: z.number().int().min(1).optional().describe("How many rows or columns to delete; 1 by default."),
  row: z.number().int().min(0).optional().describe("The row of the cell, or of the top-left cell of a range."),
  column: z.number().int().min(0).optional().describe("The column of the cell, or of the top-left cell of a range."),
  rowspan: z.number().int().min(1).optional().describe("How many rows the merged cell spans."),
  colspan: z.number().int().min(1).optional().describe("How many columns the merged cell spans."),
  cells: z.array(z.array(cellValueSchema).min(1)).min(1).optional().describe(
    "Lines of values, as the rows of openl_get_table hold them: rows top to bottom for a row action and a range, columns left to right for a column action.",
  ),
  value: cellValueSchema.optional().describe("The value of the cell; null clears it. A text starting with '=' is an OpenL formula, written as text."),
  formula: excelFormulaSchema,
};

/** One edit of a table: an action and the arguments it takes. */
const editSchema = z.strictObject(editShape).superRefine(checkEditArguments);

export type TableEdit = z.output<typeof editSchema>;

export const editTableSchema = z.strictObject({
  ...tableActionBase,
  ...editShape,
  action: editShape.action.optional(),
  actions: z.array(editSchema).min(1).optional().describe("Several edits, each an object of an 'action' and its arguments, made in order as one change: each sees the table as the previous one left it, and when one is refused nothing is written. Give either 'actions' or one 'action' with its arguments."),
}).superRefine((data, ctx) => {
  if (data.actions === undefined) {
    if (data.action === undefined) {
      ctx.addIssue({ code: "custom", path: ["action"], message: "An edit names an 'action', or several in 'actions'." });
      return;
    }
    checkEditArguments({ ...data, action: data.action }, ctx);
    return;
  }
  const beside = EDIT_ARGUMENTS.filter((name) => data[name] !== undefined);
  if (data.action !== undefined || beside.length > 0) {
    ctx.addIssue({
      code: "custom", path: ["actions"],
      message: "'actions' holds every edit with its arguments: give no 'action' or edit argument beside it.",
    });
  }
});

export const listBranchesSchema = z.strictObject({
  repository: repositoryNameSchema,
  ...paginationShape,
});

export const createBranchSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: branchNameSchema,
  revision: z.string().optional().describe("Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used."),
});

export const listProjectBranchesSchema = z.strictObject({
  projectId: projectIdSchema,
  scope: z.enum(BRANCH_SCOPES).default("project").describe(
    "Branches to list: 'project' (default) returns branches that already hold the project and can be switched to; 'repository' returns every repository branch, including merge targets that do not hold the project yet.",
  ),
  name: z.string().optional().describe("Branch name filter (partial, case-insensitive)."),
  ...paginationShape,
});

const mergeRequestFields = {
  projectId: projectIdSchema,
  otherBranch: nonBlankString.describe("The other branch: source for receive mode, target for send mode. Discover all merge targets with openl_list_project_branches(scope='repository')."),
  mode: z.enum(MERGE_MODES).describe("receive merges the other branch into the project's current branch; send merges the current branch into otherBranch."),
};

const confirmForceSchema = z.boolean().optional().describe("Must be true when force=true, confirming the protected-branch bypass.");

/** Refuses a `force` the caller did not confirm with `confirmForce`. */
function requireForceConfirmation(
  value: { force?: boolean; confirmForce?: boolean },
  ctx: z.RefinementCtx<{ force?: boolean; confirmForce?: boolean }>,
): void {
  if (value.force && value.confirmForce !== true) {
    ctx.addIssue({ code: "custom", path: ["confirmForce"], message: "confirmForce must be true when force=true." });
  }
}

export const checkProjectMergeSchema = z.strictObject({
  ...mergeRequestFields,
});

export const mergeProjectBranchesSchema = z.strictObject({
  ...mergeRequestFields,
  force: z.boolean().optional().describe("Bypass eligible protected-target restrictions. Default false. Use only after Studio reports blockedBy='bypass-required'."),
  confirmForce: confirmForceSchema,
}).superRefine(requireForceConfirmation);

export const readMergeConflictFileSchema = z.strictObject({
  projectId: projectIdSchema,
  path: nonBlankString.describe("Conflicted file path as openl_get_merge_conflicts() lists it, relative to the repository, or its path within the project."),
  side: z.enum(["BASE", "OURS", "THEIRS"]).describe("Version to read: common ancestor, current branch, or merging branch."),
});

export const getMergeConflictWorkbookSchema = z.strictObject({
  projectId: projectIdSchema,
  path: nonBlankString.describe("Conflicted Excel file path as openl_get_merge_conflicts() lists it, or its path within the project."),
  table: nonBlankString.optional().describe(
    "The id of one conflicting table from the overview. Omit to list every sheet and conflicting table with its conflicting cells; give it to read that table in your, the base and their version, and merged cell by cell.",
  ),
});

export const getMergeConflictTextSchema = z.strictObject({
  projectId: projectIdSchema,
  path: nonBlankString.describe("Conflicted text file path as openl_get_merge_conflicts() lists it, or its path within the project."),
  fromConflict: z.number().int().min(0).optional().describe("The conflict to start at, with the merged lines before it, to read the rest of a file an answer cut."),
});

const textDecisionSchema = z.union([
  z.strictObject({
    side: z.enum(["ours", "theirs", "base", "ours-theirs", "theirs-ours"]).describe("The lines the conflict takes: of your, their or the base version, or of both versions whole, yours first (ours-theirs) or theirs first (theirs-ours)."),
  }),
  z.strictObject({
    lines: z.string().describe("The lines written in place of the conflict, separated by \\n; a line break after the last line is optional, and '' leaves no line. They are written with the line breaks of the file."),
  }),
]);

const cellSideSchema = z.enum(["ours", "theirs"]);
const cellIndexSchema = z.number().int().min(0);

const workbookDecisionSchema = z.union([
  z.strictObject({
    id: nonBlankString.describe("A table id, a sheet name (taking the whole sheet), or '/palette'."),
    side: z.enum(["ours", "theirs", "base"]).describe("The version the conflict is taken from whole, with the cells only the other branch changed lost; a table that says byCells: true keeps them with cells. The palette takes ours or theirs only."),
  }),
  z.strictObject({
    id: nonBlankString.describe("The id of a table whose overview says byCells: true."),
    cells: z.strictObject({
      side: cellSideSchema.optional().describe("The version every conflicting cell of the table takes, unless the cell is decided otherwise."),
      cells: z.array(z.strictObject({
        row: cellIndexSchema.describe("Row of the merged table, from 0."),
        column: cellIndexSchema.describe("Column of the merged table, from 0."),
        side: cellSideSchema.optional().describe("The version a conflicting cell takes its value and its comment from."),
        value: z.string().optional().describe("A value typed for any cell, read as Excel reads typing: '=' starts a formula, TRUE/FALSE are booleans, numbers use a dot, dates are 2026-10-22 or 2026-10-22 14:30, an apostrophe keeps text, '' clears the cell. It keeps the formatting of the merged cell."),
      }).refine((cell) => cell.side !== undefined || cell.value !== undefined, {
        error: "A cell decision gives a side, a value, or both.",
      })).optional().describe("Decisions for single cells; a cell decision overrides the one of the table."),
    }).describe("How the table is merged cell by cell: the cells only one branch changed are merged by Studio, and every conflicting cell needs a side or a value."),
  }),
]);

export const resolveMergeConflictsSchema = z.strictObject({
  projectId: projectIdSchema,
  resolutions: z.array(z.union([
    z.strictObject({
      path: nonBlankString.describe("Conflicted file path as openl_get_merge_conflicts() lists it, or its path within the project."),
      strategy: z.enum(["BASE", "OURS", "THEIRS"]).describe("Take the whole file from the base version, your version (the current branch) or their version; a version fileAvailability marks false, which does not hold the file, deletes it."),
    }),
    z.strictObject({
      path: nonBlankString.describe("Conflicted Excel file path as openl_get_merge_conflicts() lists it, or its path within the project."),
      decisions: z.array(workbookDecisionSchema).min(1).describe("How each conflict of the workbook from openl_get_merge_conflict_workbook() is resolved; every conflict needs a decision, and a sheet taken whole needs none for its tables."),
    }),
    z.strictObject({
      path: nonBlankString.describe("Conflicted text file path as openl_get_merge_conflicts() lists it, or its path within the project."),
      conflicts: z.array(textDecisionSchema).describe("How each conflict of the text file from openl_get_merge_conflict_text() is resolved: one decision per conflict, in their order; the lines the merge took by itself are kept."),
    }),
  ])).min(1).describe("One resolution for every conflicted file of the pending conflict."),
  message: z.string().optional().describe("Commit message; Studio builds one from the conflict when omitted."),
});

export const deleteProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  confirmProjectName: nonBlankString.describe("Required safety confirmation: exact project name returned by openl_get_project(). The delete is rejected if it does not match."),
  comment: commitCommentSchema.optional().describe("Optional deletion commit message of at most 255 characters, validated by the repository's comment template when configured."),
});

export const deleteProjectBranchSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: nonBlankString.describe("Exact branch name from openl_list_project_branches(). The repository base branch cannot be deleted."),
  confirmBranchName: nonBlankString.describe("Required safety confirmation; must exactly equal branch."),
  confirmDataLoss: z.boolean().optional().describe(
    "Explicitly allow deletion after the tool reports that the branch has commits absent from the base branch, has unsaved working-copy changes, or its divergence from base cannot be verified (for example, the base is unknown, the merge check fails or checks different branches, or the target is not current). Omit for the initial safe-delete attempt.",
  ),
  force: z.boolean().optional().describe("Bypass protected-branch restrictions for eligible managers. Default false."),
  confirmForce: confirmForceSchema,
}).superRefine((value, ctx) => {
  if (value.branch !== value.confirmBranchName) {
    ctx.addIssue({ code: "custom", path: ["confirmBranchName"], message: "confirmBranchName must exactly equal branch." });
  }
  requireForceConfirmation(value, ctx);
});

// =============================================================================
// Project Creation & Cloning Schema (single tool: blank create or clone)
// =============================================================================

export const createProjectSchema = z.strictObject({
  repository: repositoryNameSchema,
  name: z.string().describe(
    "Name for the new project. Becomes the project folder name and — when cloning — the renamed project name written into rules.xml. Must be unique in the repository; a collision is rejected with 409. Allowed characters: letters, digits, space, '_' and '-'."
  ),
  template: projectIdSchema.optional().describe(
    "How to create the project. OMIT to create a BLANK project from the \"Empty Project\" template OpenL Studio ships. To CLONE an existing project, pass its exact opaque projectId from openl_list_projects() without modification: its full structure is copied (rules, tests, settings, request/response examples) and the project is renamed to name. Never pass the displayed project name because mapped repositories may contain multiple projects with the same name."
  ),
  branch: branchNameSchema.optional().describe(
    "Target branch for either BLANK creation or CLONING. Omit for the repository's configured/default branch. Studio selects an existing branch case-insensitively; when the branch does not exist, Studio may create it from the repository base branch. Use openl_list_branches() first to avoid accidental branch creation."
  ),
  comment: commitCommentSchema.optional().describe(
    "Change comment of at most 255 characters for repositories that support comments. Applied to both BLANK creation and CLONING; Studio supplies its configured create/copy comment when omitted. Omit for repositories whose features.comments is false."
  ),
});

export const deployProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  deploymentName: z.string().optional().describe("The name of a new deployment."),
  repository: z.string().optional().describe("The 'id' or 'name' of the production repository of a new deployment, as openl_list_deploy_repositories and openl_list_deployments answer them."),
  deploymentId: z.string().optional().describe("The 'id' of a deployment to deploy the project to again, from openl_list_deployments."),
  comment: commentSchema.describe("Deployment reason comment (e.g., 'Deploy version 1.2.0', 'Production release')"),
}).superRefine((data, ctx) => {
  const created = data.deploymentName !== undefined || data.repository !== undefined;
  if (data.deploymentId !== undefined && created) {
    ctx.addIssue({
      code: "custom", path: ["deploymentId"],
      message: "A deploymentId deploys to that deployment again: it takes no deploymentName or repository.",
    });
  } else if (data.deploymentId === undefined && (data.deploymentName === undefined || data.repository === undefined)) {
    ctx.addIssue({
      code: "custom", path: ["deploymentName"],
      message: "A new deployment needs deploymentName and repository; deploymentId redeploys an existing one.",
    });
  }
});

export const saveProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  comment: nonBlankString.max(MAX_COMMIT_COMMENT_LENGTH, commitCommentTooLong).optional().describe("Commit comment of at most 255 characters, ignored by a repository without comments. Omit it for the comment the repository configures for a save, such as 'Project X is saved.' Save only works when project status is EDITING; after save a new revision is created and project transitions to OPENED."),
  closeAfterSave: z.boolean().optional().describe("Optional. If true, close the project after saving (sends status CLOSED with the comment when supported in one request). Use when user asks to 'save and close'."),
});

// A table is read and written in one shape: its rows of values, and its merged cells and Excel formulas by position.

export const updateTableSchema = z.strictObject({
  ...tableActionBase,
  ...tableGridShape,
});

export const createProjectTableSchema = z.strictObject({
  projectId: projectIdSchema,
  module: nonBlankString.describe("Name of an existing project module where the table will be created (for example, 'Main' or 'Rules')."),
  modulePath: z.string().min(6).regex(/\.xlsx$/i).optional().describe("Project-relative .xlsx path for a new module. When omitted, module must identify an existing module."),
  sheetName: z.string().optional().describe("Name of the sheet where the table will be created within the Excel file. If not provided, the table name will be used as the sheet name."),
  name: nonBlankString.describe("The name of the table, a valid Java identifier such as 'calculatePremium', as its header row names it."),
  ...tableGridShape,
});

// =============================================================================
// Trace Debug API Schemas (BETA) — interactive debugger
// =============================================================================

/**
 * Shaping the answer of a profiling run, which completes in a start, a step or a resume: the constant-size `profile`,
 * and the root of the call tree only when asked.
 */
const traceProfileParams = {
  includeTree: z.boolean().optional().describe("Add 'tree', the root of the executed call tree of a profiling run, one level deep, which openl_expand_trace_tree walks."),
  profileTop: z.number().int().min(1).max(500).optional().describe("How many of the slowest tables 'profile' lists; 20 by default."),
};

export const startTraceSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID to debug (e.g., 'calculatePremium_1234'). Get from openl_list_tables()."),
  testRanges: z.string().optional().describe("For test tables: comma-separated test-case ranges (e.g., '1-3,5'), which a replay does not remember: pass them again. Omit for regular rule execution."),
  inputJson: z.union([z.string(), z.record(z.string(), z.unknown())]).optional().describe("For regular rules: the input keyed by parameter name, or { params, runtimeContext? }, e.g. { params: { age: 25 }, runtimeContext: { lob: 'Auto' } }. Omit BOTH inputJson and testRanges to replay the input of the previous run, e.g. restarting with profiling or new breakpoints."),
  module: z.string().optional().describe("Module name to trace in the context of a specific opened module. Usually omit."),
  stopAtEntry: z.boolean().optional().describe("Suspend at the entry of the first frame (default true). Set false to run straight to the first breakpoint — or, with no breakpoints, to completion."),
  profiling: z.boolean().optional().describe("Retain the executed call tree — structure and timings, NO values (default false). With stopAtEntry: false and no breakpoints the run completes in this single call and returns a constant-size 'profile' overview (top-N slowest tables); the tree's root node comes with includeTree: true and is browsed level by level with openl_expand_trace_tree."),
  detailedTitles: z.boolean().optional().describe("Build value-rich business-view titles in the retained tree (backend default false). This can substantially increase response size."),
  breakOnErrors: z.boolean().optional().describe("Suspend on an uncaught rule error so its frame can be inspected (backend default true). Set false to let the error terminate the run."),
  breakpoints: z.array(z.string()).optional().describe("Breakpoints to set before the start, replacing the set, with the keys openl_set_trace_breakpoints describes."),
  ...traceProfileParams,
});

export const stepTraceSchema = z.strictObject({
  projectId: projectIdSchema,
  type: z.enum(["into", "over", "out"]).describe("'out' (main tool for declarative rules — decision tables, spreadsheets, rating): run the current frame to its own exit so its result is inspectable, then continue in the caller. 'into' / 'over' are ADVANCED (imperative TBasic / loops): 'into' enters the next call or sub-step; 'over' advances to the next sub-step of the current frame (nested calls run through). For 'which table returned what', prefer 'out' plus breakpoints over stepping through expressions."),
  withValues: z.boolean().optional().describe("After the step, also return the active frame's variables (the same content as openl_inspect_trace_frame on the top frame) as 'variables' — saves the usual step→inspect round-trip when you step 'out' to read a frame's result. Default false."),
  ...traceProfileParams,
});

export const resumeTraceSchema = z.strictObject({
  projectId: projectIdSchema,
  attach: z.boolean().optional().describe("Wait for the next stop of a run an earlier call left running at its timeout, without resuming the session again: a resume would run past a stop the run reached since."),
  timeoutMs: z.number().int().positive().max(600000).optional().describe("Maximum time to wait for the next suspension or completion, in milliseconds. Default 30000, cap 600000. On timeout the current (still running) status is returned — call openl_resume_trace again to keep waiting, or openl_stop_trace to give up."),
  ...traceProfileParams,
});

export const inspectTraceFrameSchema = z.strictObject({
  projectId: projectIdSchema,
  frameIndex: z.number().int().nonnegative().describe("Stack frame index from the frames[] of the last stack response (0 = root, highest = current)."),
  withHighlights: z.boolean().optional().describe("Also return the frame's cell-highlight overlay (A1-keyed) plus the raw table grid to merge it with (default false)."),
  full: z.boolean().optional().describe("Return the complete untrimmed response, including value JSON schemas (default false — trimmed via ?fields to save tokens)."),
  onlyExecutedSteps: z.boolean().optional().describe("Keep only executed steps (drop pending/current-without-value) so the response is just the computed factors (default false)."),
  excludeStepValues: z.array(z.union([z.number(), z.string(), z.boolean()])).optional().describe("Drop executed steps whose scalar value equals one of these — to hide neutral factors and surface the outlier (e.g. [1] in rating, where a factor of 1.0 means 'no effect'). Lazy step values are resolved before the comparison, so a neutral factor that came lazy is dropped too. Do not use for tables where those values are meaningful."),
});

export const setTraceBreakpointsSchema = z.strictObject({
  projectId: projectIdSchema,
  breakpoints: z.array(z.string()).optional().describe("The breakpoints to replace the whole set with; an empty array clears it. Leave out to read the set."),
});

export const getTraceValueSchema = z.strictObject({
  projectId: projectIdSchema,
  parameterId: z.number().int().nonnegative().describe("The parameterId of a lazy value (lazy: true) of openl_inspect_trace_frame, openl_step_trace with withValues, or openl_watch_trace_cells."),
  withSchema: z.boolean().optional().describe("Also return the value's JSON Schema (default false — the schema is large and rarely needed; the value itself already shows the structure)."),
});

export const expandTraceTreeSchema = z.strictObject({
  projectId: projectIdSchema,
  uri: z.string().describe("Source URI of the node whose step to expand: the root node's `uri` from the /stack `tree` (a profiling openl_start_trace / openl_resume_trace with includeTree: true), or a child node's `uri` from an earlier openl_expand_trace_tree page."),
  instance: z.number().int().nonnegative().describe("Zero-based execution index of that node in the run (its `instance`; 0 for the root) — picks the exact loop iteration when the table ran more than once."),
  step: z.string().describe("Reference of the step within that node to expand, e.g. 'R1C0' — a step whose `childrenTotal` > 0 (a step with childrenTotal 0 or absent made no sub-calls)."),
  offset: z.number().int().nonnegative().optional().describe("Index of the first sub-call to return, for paging a loop's thousands of children (default 0). When the response's total exceeds offset + returned children, call again with offset advanced by the returned count (the reply's nextOffset)."),
  limit: z.number().int().min(1).max(200).optional().describe("How many sub-calls to return per page (backend default 100). Keep it modest — a page of many wide nodes can still be large."),
});

export const watchTraceCellsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID to run (e.g., 'calculatePremium_1234'). Get from openl_list_tables()."),
  cells: z.array(z.string()).min(1).describe("Cell names to watch, e.g. ['$VehiclePriceFactor']. The value of each named cell is captured at EVERY execution of its table across the whole run — one series per cell."),
  testRanges: z.string().optional().describe("For test tables: comma-separated test-case ranges (e.g., '1-3,5'), which a replay does not remember."),
  inputJson: z.union([z.string(), z.record(z.string(), z.unknown())]).optional().describe("For regular rules: the input keyed by parameter name, or { params, runtimeContext? }. Omit BOTH inputJson and testRanges to replay the input of the previous run."),
  module: z.string().optional().describe("Module name to run in the context of a specific opened module. Usually omit."),
  withSchema: z.boolean().optional().describe("Include each watched value's JSON Schema (default false — the schema is large and rarely needed; a value that came lazy still carries its parameterId for openl_get_trace_value)."),
});

// =============================================================================
// Revision Schemas
// =============================================================================

export const getProjectRevisionsSchema = z.strictObject({
  projectId: projectIdSchema,
  search: z.string().optional().describe("Search term to filter revisions by commit message or author"),
  techRevs: z.boolean().default(false).describe("Include technical revisions (default: false)"),
  ...paginationShape,
});

// =============================================================================
// Local Changes & Restore Schemas
// =============================================================================

export const listProjectLocalChangesSchema = z.strictObject({
  projectId: projectIdSchema,
  module: nonBlankString.optional().describe("The module whose history to list, as openl_list_project_modules names it; omit for every module."),
  historyId: nonBlankString.optional().describe("A version of the history of the module: answer instead the tables a restore to it would change, those the version holds otherwise than the current one."),
}).refine((args) => args.historyId === undefined || args.module !== undefined, {
  error: "historyId is a version of the history of one module: name the module.",
  path: ["historyId"],
});

export const restoreProjectLocalChangeSchema = z.strictObject({
  projectId: projectIdSchema,
  module: nonBlankString.describe("Module name used with openl_list_project_local_changes()."),
  historyId: nonBlankString.describe("Local history entry ID to restore, from openl_list_project_local_changes()."),
});

// =============================================================================
// Test Execution Schemas
// =============================================================================

export const startProjectTestsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("Table ID to run tests for a specific table. Table type can be test table or any other table. If not provided, tests for all test tables in the project will be run."),
  testRanges: z.string().optional().describe("Test ranges to run. Can be provided only if tableId is Test table. Example: '1-3,5' to run tests with numbers 1,2,3 and 5. If not provided, all tests in the test table will be run."),
  module: z.string().optional().describe("Module name to run tests from."),
});

/** Whether the options of a test-result read go together: paging options exclude each other, and a summary pages nothing. */
function checkTestResultOptions(data: {
  summary: boolean;
  tableId?: string;
  caseId?: string;
  offset?: number;
  limit?: number;
  unpaged: boolean;
}, ctx: z.RefinementCtx): void {
  const paged = data.offset !== undefined || data.limit !== undefined;
  if (data.unpaged && paged) {
    ctx.addIssue({ code: "custom", path: ["unpaged"], message: "unpaged is mutually exclusive with offset and limit." });
  }
  if (data.summary && (paged || data.tableId !== undefined)) {
    ctx.addIssue({ code: "custom", path: ["summary"], message: "A summary takes no tableId, offset or limit." });
  }
  if (data.caseId !== undefined && data.tableId === undefined) {
    ctx.addIssue({ code: "custom", path: ["caseId"], message: "caseId names a test of the test table tableId names." });
  }
}

export const getTestResultsSchema = z.strictObject({
  projectId: projectIdSchema,
  summary: z.boolean().default(false).describe("Read only the time of the run and how many tests passed and failed."),
  tableId: tableIdSchema.optional().describe("The test table to read, page by page of its tests; omit to read every test table, page by page."),
  caseId: nonBlankString.optional().describe("The id of one test of tableId, to read it with its inputs as JSON, whole up to 12,500 characters and cut in their structure beyond, the cuts named: the other reads name an input with inner structure only by its type and key."),
  failuresOnly: z.boolean().default(true).describe("List only the failed tests, true by default; false lists every test."),
  failures: z.number().int().positive().default(5).describe("With failuresOnly, how many failed tests each test table lists, 5 by default; a test table with more tells how many it leaves out. The results of one tableId list them all."),
  offset: z.number().int().nonnegative().optional().describe("The test table to start from, or with tableId the test, as pagination.next_offset names it."),
  limit: z.number().int().positive().max(200).optional().describe("How many test tables a page holds, or with tableId how many tests, 50 by default."),
  unpaged: z.boolean().default(false).describe("Read every test table in one answer without a size limit, which can exceed what the client accepts."),
}).superRefine(checkTestResultOptions);

// =============================================================================
// Project Files (BETA) Schemas
// =============================================================================
// Map 1:1 onto the "Projects: Files (BETA)" REST API:
//   GET    /projects/{projectId}/files/{path}     -> openl_read_project_file
//   POST   /projects/{projectId}/files/{path}     -> openl_write_project_file
//   DELETE /projects/{projectId}/files/{path}     -> openl_delete_project_file
//   POST   /projects/{projectId}/file-search      -> openl_search_project_files
//   POST   /projects/{projectId}/file-copy        -> openl_copy_project_file
//   POST   /projects/{projectId}/file-move        -> openl_move_project_file

const filePathSchema = z
  .string()
  .min(1)
  .describe(
    "Project-relative path to the resource (e.g. 'rules/Model.xlsx'). Do NOT include the project name itself; paths are relative to the project root. A trailing slash denotes a folder."
  );

const fileBranchSchema = branchNameSchema
  .optional()
  .describe(
    "The branch the project must be on; the call fails when it is on another. Leave out for a repository without branches."
  );

export const readProjectFileSchema = z.strictObject({
  projectId: projectIdSchema,
  path: z
    .string()
    .default("")
    .describe(
      "Project-relative path of a file (e.g. 'rules.xml', 'rules/Model.xlsx'), or of a folder ('rules/', or empty for the project root), which is answered as a ZIP archive by link. List a folder with openl_search_project_files."
    ),
  revision: z
    .string()
    .optional()
    .describe("A commit to read the file at, as openl_repository_project_revisions lists it; omit for the working copy. Not the 'version' a write or a deletion takes, which the answer then leaves out."),
  branch: fileBranchSchema,
});

/** The version of a file a write replaces or a deletion removes, as openl_read_project_file answered it. */
const fileVersionText =
  "The 'version' openl_read_project_file answered for the file. A file changed since, in an IDE, in Excel or through " +
  "another client, is left as it is: read it again and repeat the change.";

const writeProjectFileBaseSchema = z.strictObject({
  projectId: projectIdSchema,
  path: filePathSchema,
  createFolders: z
    .boolean()
    .default(true)
    .describe("If true (default), missing intermediate folders of a new file are created automatically; otherwise the parent folder must already exist."),
  version: nonBlankString
    .optional()
    .describe(`Required to replace a file that exists. ${fileVersionText} Omit it to create a new file: Studio writes nothing when a file is already at the path.`),
  branch: fileBranchSchema,
});

export const writeProjectFileSchema = writeProjectFileBaseSchema.extend({
  content: z
    .string()
    .describe("UTF-8 text content of the file."),
  message: z
    .string()
    .optional()
    .describe("Optional commit message. PRESENT → the write is committed to Git after saving the project (a new revision is created). ABSENT → the write stays in the project WORKING COPY (commit it later with openl_save_project). NOTE: committing saves ALL pending project changes (OpenL has no per-file commit), and only works for design (Git) repositories — not 'local'."),
});

export const createUploadLinkSchema = writeProjectFileBaseSchema.extend({
  size: z
    .number()
    .int()
    .nonnegative()
    .optional()
    .describe("Size in bytes the uploaded file must have; where Studio authenticates its clients, it refuses a body of another size and writes nothing."),
  sha256: z
    .string()
    .regex(/^[0-9a-fA-F]{64}$/, "sha256 is the hex SHA-256 of the file, 64 characters.")
    .optional()
    .describe("Hex SHA-256 the uploaded file must have, e.g. from `sha256sum`; where Studio authenticates its clients, it refuses other content and writes nothing."),
});

export const deleteProjectFileSchema = z.strictObject({
  projectId: projectIdSchema,
  path: filePathSchema,
  version: nonBlankString
    .optional()
    .describe(`Required to delete a file. ${fileVersionText} A folder is deleted without it.`),
  branch: fileBranchSchema,
});

export const searchProjectFilesSchema = z.strictObject({
  projectId: projectIdSchema,
  pattern: z
    .string()
    .optional()
    .describe("Ant-glob path pattern, such as 'rules/**/*.xlsx' or '**/*.xml'; a pattern of a name, such as '*.xlsx', matches at any depth with recursive."),
  content: z
    .string()
    .optional()
    .describe("Case-insensitive substring of a file read as text: the text of a workbook cell or of a compressed ZIP entry is never found, and a file over 1 MB is never read, so find workbooks, ZIP files, images and large files by pattern or extensions, and the tables a text is in with openl_list_tables text. The answer counts the workbooks the search covers in notSearched."),
  extensions: z
    .array(z.string())
    .optional()
    .describe("File extensions without the dot, such as ['xlsx','xml']."),
  type: z
    .enum(["FILE", "FOLDER", "ANY"])
    .optional()
    .describe("Restrict results to files, folders, or both (ANY, default)."),
  scope: z
    .enum(["SUBTREE", "ANCESTORS"])
    .optional()
    .describe("SUBTREE (default) searches within the project; ANCESTORS walks up from 'from' to the repository root, finding at each level the text file, such as a .md, .json or .yaml file up to 1 MB, whose exact name 'pattern' gives, not a glob, whatever content, extensions and type say; it answers its content up to 5,000 bytes and contentOmitted beyond."),
  recursive: z
    .boolean()
    .optional()
    .describe("Search the nested folders too; false searches the top level only, even with a '**' pattern. True by default with content, false otherwise."),
  from: z
    .string()
    .optional()
    .describe("A project-relative folder: SUBTREE lists its entries, everything under it with recursive; ANCESTORS walks up from it."),
  revision: z
    .string()
    .optional()
    .describe("A commit to search the files at, SUBTREE scope only; omit for the working copy."),
  branch: fileBranchSchema,
  fields: z
    .string()
    .optional()
    .describe("Comma-separated fields of a match, such as 'path,name,type'; path, type, size and lastModified by default, every field of an ANCESTORS match."),
  ...paginationShape,
});

// Intentionally has no `response_format` or pagination: the tool returns a single
// aggregated markdown document (one AGENTS.md chain), which has no alternate format
// or pages — do not add them to match the other tools.
export const getProjectAgentContextSchema = z.strictObject({
  projectId: projectIdSchema,
  folder: z
    .string()
    .max(1024)
    .optional()
    .describe(
      "A folder of the project, such as 'rules/pricing', for the guidance of a file inside it; the project root when omitted."
    ),
  branch: fileBranchSchema,
});

// =============================================================================
// Diagnostics Schemas
// =============================================================================

// Only the amount of detail is negotiable: the answer is a fixed Markdown report.
export const getServerInfoSchema = z.strictObject({
  full: z
    .boolean()
    .optional()
    .describe(
      "Add the build number, the start and the current time, and the operating system, architecture, Java, CPUs and their load, threads, and the memory of the machine and of the JVM OpenL Studio runs on. Default false: the version and the address only."
    ),
});

// =============================================================================
// Guidance Schemas
// =============================================================================

// Intentionally argument-less: the onboarding text is fixed per build and the
// orientation is computed from the documents OpenL Studio serves.
export const getStartedSchema = z.strictObject({});

export const listGuidesSchema = z.strictObject({
  type: z
    .enum(GUIDE_TYPES)
    .optional()
    .describe(
      "Filter by document type: 'specification' (config-file and project-layout specs, e.g. rules.xml) or 'guide' (OpenL Tablets Reference Guide chapters). Omit for both."
    ),
  search: z
    .string()
    .max(200)
    .optional()
    .describe(
      "Case-insensitive substring matched against each entry's id and title (e.g. 'decision table', 'spreadsheet', 'rules.xml')."
    ),
  ...paginationShape,
});

// Like getProjectAgentContextSchema, intentionally no `response_format`: the tool
// returns the requested markdown documents verbatim.
export const getGuidesSchema = z.strictObject({
  ids: z
    .array(z.string().min(1))
    .min(1)
    .max(5)
    .describe(
      "1-5 guide ids exactly as returned by openl_list_guides (e.g. 'spec/rules.xml', 'guide/introduction/basic-concepts'). Each entry's size is in the index — fetch only what you need."
    ),
});

const copyMovePairSchema = {
  projectId: projectIdSchema,
  sourcePath: z
    .string()
    .min(1)
    .describe("Project-relative path of the source file (e.g. 'rules/Model.xlsx')."),
  destinationPath: z
    .string()
    .min(1)
    .describe("Project-relative destination path (e.g. 'rules/Model-copy.xlsx'). Intermediate folders are created automatically."),
  branch: fileBranchSchema,
};

/** A copy or a move of a file within a project. */
export const fileTransferSchema = z.strictObject(copyMovePairSchema);

// =============================================================================
// Redeploy Schema
// =============================================================================
