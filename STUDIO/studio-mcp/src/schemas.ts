/**
 * The Zod schemas of the tool arguments: each is a strict object, so an unknown argument is refused, and describes its
 * fields for the JSON Schema the tool advertises.
 */

import { z } from "zod";
import { parseJsonOrText } from "./http.js";
import {
  BRANCH_SCOPES, GUIDE_TYPES, MERGE_MODES, PROJECT_INCLUDES, PROJECT_STATUSES, RESPONSE_FORMATS, TABLE_KINDS,
} from "./types.js";

/** What the compile state of `include: status` does not say: an idle project is not compiled yet. */
const STATUS_INCLUDE_NOTE =
  "'status' adds compileState: 'idle' means Studio has not compiled the project yet, not that it compiles; " +
  "openl_project_status compiles it and reports its errors.";

/** The table kinds as the description of a filter names them: `'Rules', 'Spreadsheet'`. */
const TABLE_KIND_LIST = TABLE_KINDS.map((kind) => "'" + kind + "'").join(", ");

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

const repositoryNameSchema = z.string().describe("Repository identifier. Pass either the 'id' or the 'name' field from openl_list_repositories() — the tool accepts both (and is case-insensitive). DO NOT invent values like 'Design Repository' or 'design-repo'; the actual names are typically short tokens (e.g. 'Design'). Always call openl_list_repositories() first if you don't already have the value in context.");

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
  response_format: ResponseFormat,
  ...paginationShape,
});

export const listDeploymentsSchema = z.strictObject({
  repository: z.string().optional().describe("Production repository ID to filter deployments by."),
  project: z.string().optional().describe("Deployed project name to filter deployments by."),
  response_format: ResponseFormat,
  ...paginationShape,
});

export const listProjectsSchema = z.strictObject({
  repository: z.string().optional().describe("Filter by repository name (display name, not ID). Use the 'name' field from openl_list_repositories() response (e.g., if list_repositories returns {id: 'design-repo', name: 'Design Repository'}, use 'Design Repository' here, NOT 'design-repo'). Omit to show projects from all repositories."),
  status: z.enum(PROJECT_STATUSES).optional().describe("Filter by project status."),
  dependsOn: z.string().optional().describe("Return projects that depend on this project identifier."),
  name: z.string().optional().describe("Project name filter (partial, case-insensitive)."),
  author: z.string().optional().describe("Last-modifying author filter (partial, case-insensitive)."),
  branch: z.string().optional().describe("Branch filter (partial, case-insensitive)."),
  sort: z.enum(["name", "status", "updated"]).optional().describe("Field used to sort the returned page."),
  include: z.array(z.enum(PROJECT_INCLUDES)).optional().describe(`Optional response expansions and listing behavior from the Studio API. ${STATUS_INCLUDE_NOTE}`),
  tags: z.record(z.string(), z.string()).optional().describe("Filter by project tags. Tags must be prefixed with 'tags.' in the query string (e.g., tags.version='1.0', tags.environment='production'). This is handled automatically by the API client - provide as object with tag names as keys."),
  response_format: ResponseFormat,
  ...paginationShape,
});

export const getProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  include: z.array(z.enum(PROJECT_INCLUDES)).optional().describe(`Optional response expansions from the Studio API. ${STATUS_INCLUDE_NOTE}`),
});

export const projectStatusSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: branchNameSchema.optional().describe(
    "Optional target branch: the tool switches an opened design project to this branch before reading its state. Omit for repositories that do not support branches and for repository 'local'."
  ),
  timeoutMs: z.number().int().positive().max(600000).default(30000).describe(
    "Max time in milliseconds to wait for Studio to compile an idle project. On expiry, the status is returned as compiling (no error). Default 30000 (30 seconds). Cap 600000 (10 minutes)."
  ),
  severity: z.array(z.enum(["ERROR", "WARN", "INFO"])).optional().describe(
    "Filter compilation.messages.items to only these severities. Useful when the project has many warnings and you want to isolate errors: pass severity: ['ERROR']. Default: all severities. Note: items are always sorted ERROR → WARN → INFO before any filter or truncation is applied, so errors are visible without this filter."
  ),
  maxMessages: z.number().int().positive().max(1000).optional().describe(
    "Cap the number of items returned in compilation.messages.items. The list is sorted ERROR → WARN → INFO first so the most actionable items are preserved when capped. Pair with severity to bound very large diagnostic lists. Default: no cap (relies on the response-format character truncation)."
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
  saveChanges: z.boolean().optional().describe("If true, save changes before closing. A comment is required when the repository supports comments. If false or omitted and project has unsaved changes, will error unless discardChanges is true."),
  comment: commitCommentSchema.optional().describe("Commit comment of at most 255 characters. Required for saveChanges when the repository supports comments; ignored when it does not."),
  discardChanges: z.boolean().optional().describe("If true, close without saving (unsaved changes will be lost). When project is EDITING, you must also set confirmDiscard: true to confirm."),
  confirmDiscard: z.boolean().optional().describe("When closing with discardChanges: true and project has unsaved changes, must be set to true (explicitly) to confirm. Omitted or false will return a confirmation prompt. Ask the user first, then call again with confirmDiscard: true."),
});

export const listTablesSchema = z.strictObject({
  projectId: projectIdSchema,
  kind: z.array(z.string()).optional().describe(`Filter by table kinds (array of strings). Valid values: ${TABLE_KIND_LIST}. Omit to show all kinds.`),
  name: z.string().optional().describe("Filter by table name fragment (e.g., 'calculate', 'Premium'). Omit to show all tables."),
  properties: z.record(z.string(), z.string()).optional().describe("Filter by project properties. Properties must be prefixed with 'properties.' in the query string (e.g., properties.state='CA', properties.lob='Auto'). This is handled automatically by the API client."),
  response_format: ResponseFormat,
  ...paginationShape,
});

export const getTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  startRow: z.number().int().min(0).optional().describe("Zero-based index of the first source row; omit to start at the top. Combine with maxRows to read a large table in slices: the next slice starts at startRow plus maxRows."),
  maxRows: z.number().int().min(1).optional().describe("Maximum number of source rows, counted from startRow; omit to read to the end. A windowed response carries totalRows. A slice holds exactly these rows: a merged cell that its edge cuts comes back cut, its first cell in the slice holding the value of the merged cell and naming its whole area in mergedArea."),
  styles: z.boolean().optional().describe("If true, each raw cell carries its Excel style (background/font colour, bold/italic/underline, alignment, indent, borders)."),
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
  fromModule: nonBlankString.optional().describe("Optional module name whose runtime context should be used. Usually omit; discover module names with openl_list_project_modules()."),
  withSchema: z.boolean().optional().describe("Include result and parameter JSON Schemas. Default false because schemas can be large."),
  timeoutMs: z.number().int().positive().max(600000).default(120000).describe("Maximum time for the complete Studio start-and-result workflow, in milliseconds. Default 120000 (2 minutes), maximum 600000 (10 minutes). A timeout cancels the pending Studio run."),
});

export const getTableDependenciesSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("Optional table whose dependency neighborhood to return. Omit to return the whole project (or module) graph."),
  module: nonBlankString.optional().describe("When tableId is omitted, limit the project graph to this module. Discover names with openl_list_project_modules()."),
  layer: z.enum(["executable", "datatype", "all"]).optional().describe("When tableId is omitted, return executable tables, datatype/vocabulary nodes, or both (backend default all)."),
  direction: z.enum(["DEPENDENCIES", "DEPENDENTS", "BOTH"]).optional().describe("When tableId is provided, relations to traverse (backend default BOTH)."),
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
  moduleName: nonBlankString.describe("Module name exactly as returned by openl_list_project_modules()."),
});

export const listTablePropertyDefinitionsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableType: nonBlankString.optional().describe("Optional public table kind. Omit for properties allowed inside a Properties table; provide a kind to get properties allowed on that table kind."),
});

export const copyTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("ID of the source table to copy."),
  moduleName: nonBlankString.describe("Name of the destination module. It must already exist unless modulePath is supplied."),
  modulePath: z.string().min(6).regex(/\.xlsx$/i).optional().describe("Project-relative .xlsx path for a new destination module. Omit when moduleName already exists."),
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

/** A cell value for a raw table-source edit. */
const rawCellInputSchema = z.strictObject({
  value: cellValueSchema.optional().describe("Cell value as a scalar or representable one-dimensional scalar array. Null or omitted is an empty cell. A text starting with '=' is an OpenL formula, such as '= $Base * 2', and is written as text."),
  formula: excelFormulaSchema,
  colspan: z.number().int().min(1).optional().describe("Number of columns this cell spans (>= 2 to merge; omit or 1 for a single column)."),
  rowspan: z.number().int().min(1).optional().describe("Number of rows this cell spans (>= 2 to merge; omit or 1 for a single row)."),
  covered: z.boolean().optional().describe("Marks a cell covered by another cell's span; its value is ignored."),
});

/** A cell of an edit, which names either its value or its Excel formula. */
const actionCellSchema = rawCellInputSchema.refine(valueOrFormula, { error: VALUE_OR_FORMULA });

const rawTableCellSchema = rawCellInputSchema.extend({
  // A whole table is written back as openl_get_table read it, the formula beside the value it computed.
  formula: z.string().optional().describe("Excel formula of the cell as Excel writes it, such as '=B2*C2', as openl_get_table returns it beside the value it computed. The cell is written as this Excel formula and 'value' is ignored; omit it to write 'value'. An OpenL formula is a 'value' starting with '='."),
  cell: z.string().optional(),
  mergedArea: z.string().optional(),
}).describe("Writable raw cell. Studio exposes style only when reading with styles=true; table write APIs do not support changing style, so style is intentionally rejected.");

export const appendTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  appendData: z.strictObject({
    tableType: z.literal("RawSource"),
    rows: z.array(z.array(rawTableCellSchema).min(1)).min(1).describe("Non-empty source rows to append. Every row must cover the table's full width; use { value: null } for an intentionally blank cell."),
  }).describe("RawSource append payload. Typed table append DTOs are intentionally unsupported because they are lossy and incomplete."),
  version: tableVersionSchema,
});

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
  cells?: Array<Array<{ colspan?: number; rowspan?: number }>>;
  formula?: string;
};

/** Whether a block of cells covers more than one cell: two cells, or one that spans others. */
function coversManyCells(cells: NonNullable<EditArguments["cells"]>): boolean {
  const only = cells.length === 1 && cells[0].length === 1 ? cells[0][0] : undefined;
  return !only || (only.colspan ?? 1) > 1 || (only.rowspan ?? 1) > 1;
}

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
  if (data.action === "update_range" && data.cells && !coversManyCells(data.cells)) {
    ctx.addIssue({
      code: "custom", path: ["cells"], message: "A range must cover more than one cell: use 'update_cell' for one.",
    });
  }
}

export const editTableSchema = z.strictObject({
  ...tableActionBase,
  action: z.enum(Object.keys(EDIT_ACTIONS) as [EditAction, ...EditAction[]]).describe("The edit to make."),
  position: z.number().int().min(0).optional().describe("The row or column the action starts at."),
  count: z.number().int().min(1).optional().describe("How many rows or columns to delete; 1 by default."),
  row: z.number().int().min(0).optional().describe("The row of the cell, or of the top-left cell of a range."),
  column: z.number().int().min(0).optional().describe("The column of the cell, or of the top-left cell of a range."),
  rowspan: z.number().int().min(1).optional().describe("How many rows the merged cell spans."),
  colspan: z.number().int().min(1).optional().describe("How many columns the merged cell spans."),
  cells: z.array(z.array(actionCellSchema).min(1)).min(1).optional().describe(
    "Lines of cells: rows top to bottom for a row action and a range, columns left to right for a column action.",
  ),
  value: cellValueSchema.optional().describe("The value of the cell; null clears it. A text starting with '=' is an OpenL formula, written as text."),
  formula: excelFormulaSchema,
}).superRefine(checkEditArguments);

export const listBranchesSchema = z.strictObject({
  repository: repositoryNameSchema,
  ...paginationShape,
});

export const createBranchSchema = z.strictObject({
  projectId: projectIdSchema,
  branchName: branchNameSchema,
  revision: z.string().optional().describe("Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used."),
});

export const listProjectBranchesSchema = z.strictObject({
  projectId: projectIdSchema,
  scope: z.enum(BRANCH_SCOPES).default("project").describe(
    "Branches to list: 'project' (default) returns branches that already hold the project and can be switched to; 'repository' returns every repository branch, including merge targets that do not hold the project yet.",
  ),
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
  file: nonBlankString.describe("Project-relative conflicted file path from openl_get_merge_conflicts()."),
  side: z.enum(["BASE", "OURS", "THEIRS"]).describe("Version to read: common ancestor, current branch, or merging branch."),
});

export const getMergeConflictWorkbookSchema = z.strictObject({
  projectId: projectIdSchema,
  file: nonBlankString.describe("Conflicted Excel file path exactly as openl_get_merge_conflicts() lists it."),
  table: nonBlankString.optional().describe(
    "The id of one conflicting table from the overview. Omit to list every sheet and conflicting table with its conflicting cells; give it to read that table in your, the base and their version, and merged cell by cell.",
  ),
});

export const getMergeConflictTextSchema = z.strictObject({
  projectId: projectIdSchema,
  file: nonBlankString.describe("Conflicted text file path exactly as openl_get_merge_conflicts() lists it."),
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
    side: z.enum(["ours", "theirs", "base"]).describe("The version the conflict is taken from whole; the palette takes ours or theirs only."),
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
      file: nonBlankString.describe("Conflicted file path exactly as openl_get_merge_conflicts() lists it."),
      strategy: z.enum(["BASE", "OURS", "THEIRS"]).describe("Take the whole file from the base version, your version (the current branch) or their version."),
    }),
    z.strictObject({
      file: nonBlankString.describe("Conflicted Excel file path exactly as openl_get_merge_conflicts() lists it."),
      decisions: z.array(workbookDecisionSchema).min(1).describe("How each conflict of the workbook from openl_get_merge_conflict_workbook() is resolved; every conflict needs a decision, and a sheet taken whole needs none for its tables."),
    }),
    z.strictObject({
      file: nonBlankString.describe("Conflicted text file path exactly as openl_get_merge_conflicts() lists it."),
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
  projectName: z.string().describe(
    "Name for the new project. Becomes the project folder name and — when cloning — the renamed project name written into rules.xml. Must be unique in the repository; a collision is rejected with 409. Allowed characters: letters, digits, space, '_' and '-'."
  ),
  template: projectIdSchema.optional().describe(
    "How to create the project. OMIT to create a BLANK project from the \"Empty Project\" template OpenL Studio ships. To CLONE an existing project, pass its exact opaque projectId from openl_list_projects() without modification: its full structure is copied (rules, tests, settings, request/response examples) and the project is renamed to projectName. Never pass the displayed project name because mapped repositories may contain multiple projects with the same name."
  ),
  branch: branchNameSchema.optional().describe(
    "Target branch for either BLANK creation or CLONING. Omit for the repository's configured/default branch. Studio selects an existing branch case-insensitively; when the branch does not exist, Studio may create it from the repository base branch. Use openl_list_branches() first to avoid accidental branch creation."
  ),
  comment: commitCommentSchema.optional().describe(
    "Change comment of at most 255 characters for repositories that support comments. Applied to both BLANK creation and CLONING; Studio supplies its configured create/copy comment when omitted. Omit for repositories whose features.comments is false."
  ),
});

export const deployProjectSchema = z.strictObject({
  projectId: projectIdSchema.describe("Project ID to deploy. Use the exact 'projectId' value from openl_list_projects() response."),
  deploymentName: z.string().describe("Name for the deployment (e.g., 'InsuranceRules', 'AutoPremium'). This will be the deployment identifier."),
  productionRepositoryId: z.string().describe("Target production repository name (display name, not ID). Use the 'name' field from openl_list_deploy_repositories() response (e.g., if list_deploy_repositories returns {id: 'production-deploy', name: 'Production Deployment'}, use 'Production Deployment' here, NOT 'production-deploy'). Must be configured in OpenL Studio."),
  comment: commentSchema.describe("Deployment reason comment (e.g., 'Deploy version 1.2.0', 'Production release')"),
});

export const saveProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  comment: nonBlankString.max(MAX_COMMIT_COMMENT_LENGTH, commitCommentTooLong).optional().describe("Commit comment of at most 255 characters, required when the repository supports comments. Omit for repositories whose features.comments is false. Save only works when project status is EDITING; after save a new revision is created and project transitions to OPENED."),
  closeAfterSave: z.boolean().optional().describe("Optional. If true, close the project after saving (sends status CLOSED with the comment when supported in one request). Use when user asks to 'save and close'."),
});

// Table content contracts are deliberately RawSource-only. Studio's typed table
// DTOs are incomplete and lossy, so exposing them here would advertise edits
// that cannot reliably round-trip the underlying workbook.

const rawTableViewSchema = z.strictObject({
    tableType: z.literal("RawSource"),
    name: z.string().optional().describe("Table name (a valid Java identifier)."),
    id: z.string().optional().describe("Table id; ignored on create and validated against the path on update."),
    kind: z.enum(TABLE_KINDS).optional().describe("Informational table kind."),
    messages: z.array(z.any()).optional().describe("Read-only diagnostics tolerated when a get response is round-tripped."),
    headerHeight: z.number().int().optional().describe("Read-only number of header rows, tolerated when a get response is round-tripped."),
    layout: z.unknown().optional().describe("Read-only layout of a Test table, tolerated when a get response is round-tripped."),
    pos: z.string().optional(),
    source: z.array(z.array(rawTableCellSchema)).describe("Complete 2D source matrix. Preserve cell positions, covered placeholders, and spans when replacing a table. Remove read-only style objects returned by styles=true; Studio table write APIs cannot change formatting."),
    totalRows: z.number().int().optional().describe("Total row count when the response contains a window."),
    version: tableVersionSchema.optional().describe("The version a get response carries, tolerated when it is round-tripped; an update requires it."),
}).describe("Complete RawSource table structure. Typed table DTOs are intentionally unsupported because they cannot reliably round-trip workbook content.");

export const updateTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  view: rawTableViewSchema.extend({ version: tableVersionSchema }).describe("Full, non-windowed RawSource structure from openl_get_table() with modifications applied. Send the complete source matrix, not only changed cells; a view carrying totalRows is rejected, and the handler independently rejects a source with fewer rows than the live table."),
}).superRefine((value, ctx) => {
  if (value.view.totalRows !== undefined) {
    ctx.addIssue({
      code: "custom",
      path: ["view", "totalRows"],
      message: "A windowed RawSource view cannot replace the whole table because omitted rows would be deleted. Use openl_edit_table to edit a large table; remove rows only with its delete_rows action and an explicit position/count.",
    });
  }
});

const createTableViewSchema = rawTableViewSchema.omit({ totalRows: true }).extend({
  name: nonBlankString.describe("Required nonblank table name (a valid Java identifier, e.g. 'calculatePremium')."),
});

export const createProjectTableSchema = z.strictObject({
  projectId: projectIdSchema,
  moduleName: nonBlankString.describe("Name of an existing project module where the table will be created (for example, 'Main' or 'Rules')."),
  modulePath: z.string().min(6).regex(/\.xlsx$/i).optional().describe("Project-relative .xlsx path for a new module. When omitted, moduleName must identify an existing module."),
  sheetName: z.string().optional().describe("Name of the sheet where the table will be created within the Excel file. If not provided, the table name will be used as the sheet name."),
  table: createTableViewSchema,
});

// =============================================================================
// Trace Debug API Schemas (BETA) — interactive debugger
// =============================================================================

/**
 * Shaping a profiling run's response. The backend returns a constant-size
 * `profile` overview (top-N slowest tables) plus, only when asked, the `tree` —
 * now just the ROOT node (one level), the entry point for lazy drill-down.
 */
const traceProfileParams = {
  includeTree: z.boolean().optional().describe("Also return the executed call tree's ROOT node ('tree'), not just the bounded 'profile' overview (default false). Against a current OpenL Studio the tree is lazy — one level deep: the root's steps each carry a 'childrenTotal' count instead of nested children, so a large run is no longer returned whole. Drill into a branch with openl_expand_trace_tree; to find the hot table use 'profile' and replay into it with a breakpoint."),
  profileTop: z.number().int().min(1).max(500).optional().describe("Number of hotspots (slowest tables) in the 'profile' overview (backend default 20)."),
};

export const startTraceSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID to debug (e.g., 'calculatePremium_1234'). Get from openl_list_tables()."),
  testRanges: z.string().optional().describe("For test tables: comma-separated test-case ranges (e.g., '1-3,5'). Omit for regular rule execution."),
  inputJson: z.union([z.string(), z.record(z.string(), z.unknown())]).optional().describe("For regular rules: JSON input. Use object with params (required) and runtimeContext (optional). E.g. { params: { age: 25 }, runtimeContext: { lob: 'Auto' } }. Omit BOTH inputJson and testRanges to replay the previous run's remembered input (e.g. restarting with profiling or new breakpoints)."),
  fromModule: z.string().optional().describe("Module name to trace in the context of a specific opened module. Usually omit."),
  stopAtEntry: z.boolean().optional().describe("Suspend at the entry of the first frame (default true). Set false to run straight to the first breakpoint — or, with no breakpoints, to completion."),
  profiling: z.boolean().optional().describe("Retain the executed call tree — structure and timings, NO values (default false). With stopAtEntry: false and no breakpoints the run completes in this single call and returns a constant-size 'profile' overview (top-N slowest tables); the tree's root node comes with includeTree: true and is browsed level by level with openl_expand_trace_tree."),
  detailedTitles: z.boolean().optional().describe("Build value-rich business-view titles in the retained tree (backend default false). This can substantially increase response size."),
  breakOnErrors: z.boolean().optional().describe("Suspend on an uncaught rule error so its frame can be inspected (backend default true). Set false to let the error terminate the run."),
  breakpoints: z.array(z.string()).optional().describe("Initial breakpoint set — REPLACES the current set before starting. Key forms: '<name>' (entry of any same-named table), '<uri>' (entry of that table), '<uri>#R{r}C{c}' (spreadsheet cell), '<uri>#rule' (any decision-table rule fires), '<uri>#<ruleName>' (specific rule fires). Append '@N' to any key to break only on the table's N-th execution (0-based) — e.g. '<uri>#R48C0@3' hits the 4th run; N matches frames[].instance and a watch series' instance, so a watch outlier at instance 3 is reached with '@3'. Without '@N' a cell breakpoint hits EVERY pass."),
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
  breakpoints: z.array(z.string()).optional().describe("When provided, REPLACES the whole breakpoint set (empty array clears all). Key forms: '<name>' (entry of any same-named table), '<uri>' (entry of that table), '<uri>#R{r}C{c}' (spreadsheet cell), '<uri>#rule' (any decision-table rule fires), '<uri>#<ruleName>' (specific rule fires). Append '@N' to any key to break only on the table's N-th execution (0-based, matching frames[].instance and a watch series' instance) — e.g. '<uri>#R48C0@3'. Omit to just read the current set and available targets."),
});

export const getTraceValueSchema = z.strictObject({
  projectId: projectIdSchema,
  parameterId: z.number().int().nonnegative().describe("Parameter ID from a lazy ParameterValue (lazy: true) returned by openl_inspect_trace_frame."),
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
  testRanges: z.string().optional().describe("For test tables: comma-separated test-case ranges (e.g., '1-3,5')."),
  inputJson: z.union([z.string(), z.record(z.string(), z.unknown())]).optional().describe("For regular rules: JSON input { params, runtimeContext? }. Omit BOTH inputJson and testRanges to replay the previous run's remembered input."),
  fromModule: z.string().optional().describe("Module name to run in the context of a specific opened module. Usually omit."),
  withSchema: z.boolean().optional().describe("Include each watched value's JSON Schema (default false — the schema is large and rarely needed; a value that came lazy still carries its parameterId for openl_get_trace_value)."),
});

// =============================================================================
// Repository Features & Revisions Schemas
// =============================================================================

export const getRepositoryFeaturesSchema = z.strictObject({
  repository: repositoryNameSchema,
});

export const getProjectRevisionsSchema = z.strictObject({
  projectId: projectIdSchema,
  search: z.string().optional().describe("Search term to filter revisions by commit message or author"),
  techRevs: z.boolean().default(false).describe("Include technical revisions (default: false)"),
  offset: z.number().int().nonnegative().optional().describe("Item offset (0-based). Mutually exclusive with page."),
  page: z.number().int().nonnegative().optional().describe("Page number (0-based). Mutually exclusive with offset; the backend defaults to 0 when both are omitted."),
  size: z.number().int().positive().max(200).default(50).describe("Page size (default: 50, max: 200)"),
}).refine(
  (data) => data.page === undefined || data.offset === undefined,
  { error: "page and offset are mutually exclusive" },
);

// =============================================================================
// Local Changes & Restore Schemas
// =============================================================================

export const restoreProjectLocalChangeSchema = z.strictObject({
  projectId: projectIdSchema,
  moduleName: nonBlankString.describe("Module name used with openl_list_project_local_changes()."),
  historyId: nonBlankString.describe("Local history entry ID to restore, from openl_list_project_local_changes()."),
});

// =============================================================================
// Test Execution Schemas
// =============================================================================

export const startProjectTestsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("Table ID to run tests for a specific table. Table type can be test table or any other table. If not provided, tests for all test tables in the project will be run."),
  testRanges: z.string().optional().describe("Test ranges to run. Can be provided only if tableId is Test table. Example: '1-3,5' to run tests with numbers 1,2,3 and 5. If not provided, all tests in the test table will be run."),
  fromModule: z.string().optional().describe("Module name to run tests from."),
});

export const getTestResultsSummarySchema = z.strictObject({
  projectId: projectIdSchema,
  failuresOnly: z.boolean().optional().describe("Include only failed tests."),
  failures: z.number().int().positive().default(5).describe("Number of failed test units to include in the summary (default: 5, min: 1)"),
  unpaged: z.boolean().default(false).describe("Return all results without pagination"),
  response_format: ResponseFormat,
});

/** The fields of a full test-result read, the table filter aside. */
const testResultFields = {
  failuresOnly: z.boolean().optional().describe("Show only failed tests (default: false)"),
  failures: z.number().int().positive().default(5).describe("Number of failed test units to include in the summary (default: 5, min: 1)"),
  page: z.number().int().nonnegative().optional().describe("Page number (0-based). Mutually exclusive with offset"),
  offset: z.number().int().nonnegative().optional().describe("Offset for pagination. Mutually exclusive with page"),
  size: z.number().int().positive().max(200).optional().describe("Page size (number of results per page, maximum 200)"),
  limit: z.number().int().positive().max(200).optional().describe("Page size (alias for size, maps to size parameter)"),
  unpaged: z.boolean().default(false).describe("Return all results without pagination or a size limit, which can exceed what the client accepts. Mutually exclusive with page, offset, size, and limit"),
  response_format: ResponseFormat,
};

/** Whether the paging options of a test-result read exclude each other as they must. */
function isValidTestResultPaging(data: {
  page?: number;
  offset?: number;
  size?: number;
  limit?: number;
  unpaged: boolean;
}): boolean {
  // page vs offset
  if (data.page !== undefined && data.offset !== undefined) {
    return false;
  }
  // unpaged vs page/offset/size/limit
  return !(data.unpaged && (data.page !== undefined || data.offset !== undefined || data.size !== undefined || data.limit !== undefined));
}

const TEST_RESULT_PAGING_ERROR =
  "Invalid pagination parameters: page and offset are mutually exclusive; unpaged is mutually exclusive with page, offset, size, and limit";

export const getTestResultsSchema = z.strictObject({
  projectId: projectIdSchema,
  ...testResultFields,
}).refine(isValidTestResultPaging, { error: TEST_RESULT_PAGING_ERROR });

export const getTestResultsByTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID to filter test results for a specific table"),
  ...testResultFields,
}).refine(isValidTestResultPaging, { error: TEST_RESULT_PAGING_ERROR });

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
    "Branch the project must be on for this operation. Ignored when blank. Fails if the repository has no branches or the project is on another branch. Omit for repository 'local' and non-branch repositories."
  );

export const readProjectFileSchema = z.strictObject({
  projectId: projectIdSchema,
  path: z
    .string()
    .default("")
    .describe(
      "Project-relative path of a file (e.g. 'rules.xml', 'rules/Model.xlsx'), or of a folder ('rules/', or empty for the project root), which is answered as a ZIP archive by link. List a folder with openl_search_project_files."
    ),
  version: z
    .string()
    .optional()
    .describe("Historical revision (commit hash) to read. Omit to read the working copy. An unknown revision yields 404."),
  branch: fileBranchSchema,
});

/** The version of a file a write replaces or a deletion removes, as openl_read_project_file answered it. */
const fileVersionText =
  "The 'version' openl_read_project_file answered for the file. Studio changes the file only while it has this " +
  "version: a file changed since — on disk, such as in an IDE or in Excel, or through another client — is left as " +
  "it is, so read it again and repeat the change on what it holds now.";

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
    .describe("Ant-glob path pattern, e.g. 'rules/**/*.xlsx' or '**/*.xml'. This can find binary files by path, but content is never searched inside them."),
  content: z
    .string()
    .optional()
    .describe("Case-insensitive substring to match inside TEXT files only. Studio does not inspect binary content such as XLSX/XLS/ZIP/images; use pattern/extensions to locate binary files instead."),
  extensions: z
    .array(z.string())
    .optional()
    .describe("Filter by file extensions without the dot, e.g. ['xlsx','xml']. With content, only matching text files are inspected; binary extensions such as xlsx can be located but not searched internally."),
  type: z
    .enum(["FILE", "FOLDER", "ANY"])
    .optional()
    .describe("Restrict results to files, folders, or both (ANY, default)."),
  scope: z
    .enum(["SUBTREE", "ANCESTORS"])
    .optional()
    .describe("SUBTREE (default) searches within the project; ANCESTORS walks up to the repository root."),
  recursive: z
    .boolean()
    .optional()
    .describe("Whether to descend into nested folders. IMPORTANT: defaults to false (top level only) — set true to search the whole project/subtree. A '**' glob still needs recursive:true to actually descend."),
  from: z
    .string()
    .optional()
    .describe("Project-relative path to start the search from."),
  version: z
    .string()
    .optional()
    .describe("Historical revision (commit hash) to search; SUBTREE scope only."),
  branch: fileBranchSchema,
  fields: z
    .string()
    .optional()
    .describe("Comma-separated response fields to return per result (e.g. 'path,name,type'). When omitted, the full response is returned."),
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
      "Optional project-relative sub-folder to start the walk-up from, e.g. 'rules' or 'rules/pricing'. Use this to get the AGENTS.md chain that applies to a file deeper inside the project ('the AGENTS.md nearest the edited file wins'). Omit to start at the project root. Do NOT include the project name; the path is relative to the project root."
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

export const redeployProjectSchema = z.strictObject({
  deploymentId: z.string().describe("Deployment ID to redeploy (from list_deployments response)"),
  projectId: projectIdSchema,
  comment: commentSchema,
});
