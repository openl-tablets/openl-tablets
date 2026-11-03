/**
 * The Zod schemas of the tool arguments: each is a strict object, so an unknown argument is refused, and describes its
 * fields for the JSON Schema the tool advertises.
 */

import { z } from "zod";
import { parseJsonOrText } from "./http.js";
import {
  BRANCH_SCOPES, GUIDE_TYPES, MERGE_MODES, PROJECT_INCLUDES, PROJECT_STATUSES, TABLE_KINDS,
  TABLE_SCOPES, TABLE_SORTS,
} from "./types.js";

/** What the compile state of `include: status` does not say: an idle project is not compiled yet. */
const STATUS_INCLUDE_NOTE =
  "'status' adds compileState: 'idle' means not compiled yet, not that it compiles; openl_project_status compiles it.";


// Pagination parameters, spread last into the arguments of a paged tool
const paginationShape = {
  limit: z.number().int().positive().max(200).default(50),
  offset: z.number().int().nonnegative().default(0),
};

// The ids most tools take carry no description, or a short one: their names say what they are, a refusal names the tool
// that lists them, and every agent loads their schemas in every session
export const projectIdSchema = z.string().min(1, "Project ID must not be empty.");

const repositoryNameSchema = z.string().describe("A repository id or name from openl_list_repositories.");

/** A string with more than whitespace in it, trimmed. */
const nonBlankString = z.string().trim().min(1);

// A table is taken by its name too, which openl_list_tables answers, and a name of several tables is refused with their ids
const tableIdSchema = z.string();

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
  repository: z.string().optional().describe("A production repository id."),
  project: z.string().optional().describe("The exact name of a deployed project; adds the revisions and authors of its deployments."),
  ...paginationShape,
});

export const listProjectsSchema = z.strictObject({
  repository: repositoryNameSchema.optional(),
  status: z.enum(PROJECT_STATUSES).optional(),
  dependsOn: z.string().optional().describe("A projectId: list the projects depending on it."),
  name: z.string().optional().describe("Part of the name, in any case."),
  author: z.string().optional().describe("Part of the last author's name, in any case."),
  branch: z.string().optional().describe("Part of the branch name, in any case."),
  sort: z.enum(["name", "status", "updated"]).optional().describe("'updated' adds the last change of each project."),
  include: z.array(z.enum([...PROJECT_INCLUDES, "details"])).optional().describe(`'details' adds the last change of each project. ${STATUS_INCLUDE_NOTE}`),
  tags: z.record(z.string(), z.string()).optional().describe("Tag values by tag name, such as {version: '1.0'}."),
  ...paginationShape,
});

export const getProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  include: z.array(z.enum(["status", "descriptor", "details"])).optional().describe(`'descriptor' adds rules.xml; 'details' the path, tags, last comment and the projects using it in full. ${STATUS_INCLUDE_NOTE}`),
});

export const projectStatusSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: z.string().optional().describe(
    "Put the project on this branch first, opening a closed one. Omit for a repository without branches."
  ),
  timeoutMs: z.number().int().positive().max(600000).default(30000).describe(
    "How long to wait for the compilation; on expiry the state is answered as compiling, with no error."
  ),
  severity: z.array(z.enum(["ERROR", "WARN", "INFO"])).optional().describe(
    "Only these severities; messages come errors first anyway."
  ),
  limit: z.number().int().positive().max(1000).optional().describe(
    "Most messages to answer, errors first. Without it, an answer too large keeps the first messages and says how many."
  ),
  grouped: z.boolean().default(true).describe(
    "false lists every message of a table instead of its first with '(+N more)'."
  ),
  changes: z.boolean().optional().describe(
    "Also name the tables each changed workbook holds otherwise than the saved revision, taking seconds a workbook."
  ),
});

export const openProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: z.string().optional().describe("Open the project on this branch."),
  revision: z.string().optional().describe("A Git revision to open, read-only."),
  openDependencies: z.boolean().optional().describe("Also open the projects it depends on."),
});

export const closeProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  saveChanges: z.boolean().optional().describe("Save the changes before closing."),
  comment: commitCommentSchema.optional().describe("Commit comment for saveChanges, ignored by a repository without comments; omit for the one the repository configures."),
  discardChanges: z.boolean().optional().describe("Close without saving, losing every unsaved change: ask the user first."),
});

export const listTablesSchema = z.strictObject({
  projectId: projectIdSchema,
  kind: z.union([z.enum(TABLE_KINDS), z.array(z.enum(TABLE_KINDS))]).optional()
    .transform((kind) => (typeof kind === "string" ? [kind] : kind)),
  name: z.string().optional().describe("A part of the table name."),
  text: nonBlankString.optional().describe("Text any cell of the table contains, case-insensitive: a value, a key, a message, a formula."),
  header: nonBlankString.optional().describe("Text of the header line: keyword, return type, name or arguments, such as a datatype the rules take."),
  module: nonBlankString.optional().describe("A module as openl_list_project_modules names it."),
  scope: z.enum(TABLE_SCOPES).optional().describe("'all' adds the projects this one depends on; by default the module when one is named, else the project."),
  sort: z.enum(TABLE_SORTS).optional().describe("'position': workbook by workbook, sheet by sheet, top to bottom."),
  propertyFilter: z.record(z.string(), z.string(), { error: "propertyFilter keeps the tables with these property values, such as { state: 'CA' }." })
    .optional().describe("Property values the tables have, such as { state: 'CA' }."),
  ...paginationShape,
});

export const getTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  find: nonBlankString.optional().describe("Read only the rows with a cell containing this text, case-insensitive, and the header rows, 'rows' keyed by position. '900' finds the number 900."),
  offset: z.number().int().min(0).optional().describe("The first row to read, from 0; the next slice starts at window.next_offset."),
  limit: z.number().int().min(1).optional().describe("How many rows to read from offset; omit to read up to 100 rows whole, or the first 50 of a larger table. A merged cell the slice cuts keeps its value in its first row there, and merges names its whole area."),
  styles: z.boolean().optional().describe("Add 'styles', the Excel style of each styled cell: colours, font, alignment, indent, borders."),
}).refine((args) => args.find === undefined || (args.offset === undefined && args.limit === undefined), {
  error: "find searches the whole table: it takes no offset or limit.",
  path: ["find"],
});

/** The version of a table a write is based on, as openl_get_table or the previous edit answered it. */
const tableVersionSchema = nonBlankString.describe(
  "The 'version' the last read or edit of the table answered.",
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
  tableId: tableIdSchema,
  inputJson: z.union([
    z.string().refine(isJsonInput, { error: "inputJson as a string must be a JSON object or array" }),
    z.array(z.unknown()),
    z.record(z.string(), z.unknown()),
  ]).describe("Also as a JSON string: an object keyed by parameter name; { params, runtimeContext? } with params by name or in order; or an array of the arguments in order. A method whose only parameter is an array takes a top-level array as its value."),
  module: nonBlankString.optional().describe("The module whose runtime context to use; usually omitted."),
  withSchema: z.boolean().optional().describe("Also answer the JSON Schema of each parameter."),
  timeoutMs: z.number().int().positive().max(600000).default(120000).describe("How long to wait for the result; a timeout cancels the run."),
});

export const getTableDependenciesSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("The table whose neighborhood to read; omit for the whole graph."),
  module: nonBlankString.optional().describe("Without tableId, the module to limit the graph to."),
  layer: z.enum(["executable", "datatype", "all"]).optional().describe("Without tableId: executable tables, datatypes and vocabularies, or all of them."),
  direction: z.enum(["DEPENDENCIES", "DEPENDENTS", "BOTH"]).optional().describe("With tableId: DEPENDENCIES it calls, DEPENDENTS calling it, BOTH by default."),
  depth: z.number().int().min(1).optional().describe("With tableId, how many calls away to go; 1 by default, the direct ones."),
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
  module: nonBlankString.describe("As openl_list_project_modules names it."),
});

export const listTablePropertyDefinitionsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableType: z.enum(TABLE_KINDS).optional().describe("Omit for the properties of a Properties table."),
});

/** Where a table copied or created goes when it is not in a module of the project yet, and its sheet. */
const newModuleParams = {
  modulePath: z.string().min(6).regex(/\.xlsx$/i).optional().describe("The project-relative .xlsx path of a new module."),
  sheetName: z.string().optional().describe("The sheet; the table name by default."),
};

export const copyTableSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  module: nonBlankString.describe("The destination module, an existing one unless modulePath is given."),
  ...newModuleParams,
  name: nonBlankString.describe("The name of the copy."),
  properties: z.record(nonBlankString, z.string()).optional().describe("Replace all of the source's properties: name every one the copy keeps. Omit to keep them."),
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
/** A cell of an edit names either its value or its Excel formula, as Studio refuses both. */
const VALUE_OR_FORMULA = "A cell takes either 'value' or 'formula', not both.";
const valueOrFormula = (cell: { value?: unknown; formula?: string }): boolean =>
  cell.formula === undefined || cell.value === undefined || cell.value === null;

/** A row or a column of a table, of a position from 0 in the whole table. */
const positionSchema = z.number().int().min(0);

/** The cells of a whole table, as openl_get_table answers them: its rows, its merged cells and its Excel formulas. */
const tableGridShape = {
  rows: z.array(z.array(cellValueSchema)).describe("Every row, top to bottom. A text starting with '=' is an OpenL formula."),
  merges: z.array(z.tuple([positionSchema, positionSchema, z.number().int().min(1), z.number().int().min(1)]))
    .optional().describe("A merged cell holds its value at its first row and column."),
  formulas: z.array(z.tuple([positionSchema, positionSchema, z.string().startsWith("=")]))
    .optional().describe("Written as the Excel formula, whatever the value in rows."),
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
  if (refused && name === "count" && action.startsWith("insert_")) {
    // Agents give an insert the count a delete takes
    return `'${action}' does not take 'count': it inserts the lines of 'cells', each with a value, as Studio refuses an empty line, which would split the table.`;
  }
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

/**
 * The arguments of one edit of a table, of the action it names. An edit of 'actions' takes them without their
 * descriptions, which the arguments of the tool give once: every agent loads the schema of the tool.
 */
const editShape = {
  action: z.enum(Object.keys(EDIT_ACTIONS) as [EditAction, ...EditAction[]]),
  position: z.number().int().min(0).optional(),
  count: z.number().int().min(1).optional(),
  row: z.number().int().min(0).optional(),
  column: z.number().int().min(0).optional(),
  rowspan: z.number().int().min(1).optional(),
  colspan: z.number().int().min(1).optional(),
  cells: z.array(z.array(cellValueSchema).min(1)).min(1).optional(),
  value: cellValueSchema.optional(),
  formula: z.string().optional(),
};

/** The arguments of an edit as the tool describes them. */
const describedEditShape = {
  action: editShape.action.describe("The edit to make."),
  position: editShape.position.describe("The row or column the action starts at."),
  count: editShape.count.describe("How many rows or columns to delete; 1 by default."),
  row: editShape.row.describe("The row of the cell, or of the top-left cell."),
  column: editShape.column.describe("The column of the cell, or of the top-left cell."),
  rowspan: editShape.rowspan.describe("How many rows the merged cell spans."),
  colspan: editShape.colspan.describe("How many columns the merged cell spans."),
  cells: editShape.cells.describe(
    "Lines of values as openl_get_table rows hold them: rows for a row action and a range, columns for a column action; null for an empty or merge-covered cell.",
  ),
  value: editShape.value.describe("null clears the cell. A text starting with '=' is an OpenL formula."),
  formula: editShape.formula.describe("An Excel formula such as '=B2*C2', without 'value'; an OpenL formula is a 'value' starting with '='."),
};

/** One edit of a table: an action and the arguments it takes. */
const editSchema = z.strictObject(editShape).superRefine(checkEditArguments);

export type TableEdit = z.output<typeof editSchema>;

export const editTableSchema = z.strictObject({
  ...tableActionBase,
  ...describedEditShape,
  action: describedEditShape.action.optional(),
  actions: z.array(editSchema).min(1).optional().describe("Several edits, each an 'action' with its arguments, instead of one 'action'."),
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
  branch: z.string(),
  revision: z.string().optional().describe("A revision, tag or branch to branch from; HEAD when omitted."),
});

export const listProjectBranchesSchema = z.strictObject({
  projectId: projectIdSchema,
  scope: z.enum(BRANCH_SCOPES).default("project").describe(
    "'project': the branches holding the project, to switch to or delete; 'repository': every branch, such as a merge target without the project.",
  ),
  name: z.string().optional().describe("Part of the branch name, case-insensitive."),
  ...paginationShape,
});

const mergeRequestFields = {
  projectId: projectIdSchema,
  otherBranch: nonBlankString.describe("The source branch with receive, the target with send; openl_list_project_branches scope 'repository' lists them."),
  mode: z.enum(MERGE_MODES).describe("receive merges otherBranch into the project's current branch; send merges the current branch into otherBranch."),
};

export const checkProjectMergeSchema = z.strictObject({
  ...mergeRequestFields,
});

export const mergeProjectBranchesSchema = z.strictObject({
  ...mergeRequestFields,
  force: z.boolean().optional().describe("Bypass a protected target, only after blockedBy 'bypass-required' and once the user approves it."),
});

/** A conflicted file, as the conflict tools take it. */
const conflictPathSchema = nonBlankString.describe("A conflicted file as openl_get_merge_conflicts lists it, or its path in the project.");

export const readMergeConflictSchema = z.strictObject({
  projectId: projectIdSchema,
  path: conflictPathSchema,
  table: nonBlankString.optional().describe("The id of a conflicting table the workbook overview lists, to read its rows."),
  fromConflict: z.number().int().min(0).optional().describe("The conflict of a text file to read on from, when an answer was cut."),
  side: z.enum(["BASE", "OURS", "THEIRS"]).optional().describe("One whole version: the common ancestor, the current branch or the other branch."),
}).refine((args) => [args.table, args.fromConflict, args.side].filter((given) => given !== undefined).length <= 1, {
  error: "table reads a workbook, fromConflict a text file, side one whole version: give one of them.",
  path: ["side"],
});

const textDecisionSchema = z.union([
  z.strictObject({
    side: z.enum(["ours", "theirs", "base", "ours-theirs", "theirs-ours"]).describe("The lines the conflict takes: yours, theirs, the base, or both, yours first (ours-theirs) or theirs first."),
  }),
  z.strictObject({
    lines: z.string().describe("Lines written in place of the conflict, separated by \\n, with the line breaks of the file; '' leaves none."),
  }),
]);

const cellSideSchema = z.enum(["ours", "theirs"]);
const cellIndexSchema = z.number().int().min(0);

const workbookDecisionSchema = z.union([
  z.strictObject({
    id: nonBlankString.describe("A table id, a sheet name (taking the whole sheet), or '/palette'."),
    side: z.enum(["ours", "theirs", "base"]).describe("The version the conflict takes whole, losing the cells only the other branch changed, which cells keeps for a byCells table. The palette takes ours or theirs."),
  }),
  z.strictObject({
    id: nonBlankString.describe("The id of a table whose overview says byCells: true."),
    cells: z.strictObject({
      side: cellSideSchema.optional().describe("The version every conflicting cell takes unless decided singly."),
      cells: z.array(z.strictObject({
        row: cellIndexSchema.describe("Row of the merged table, from 0."),
        column: cellIndexSchema.describe("Column of the merged table, from 0."),
        side: cellSideSchema.optional().describe("The version the cell takes its value and comment from."),
        value: z.string().optional().describe("A value typed as in Excel: '=' starts a formula, TRUE/FALSE are booleans, numbers use a dot, dates are 2026-10-22 or 2026-10-22 14:30, an apostrophe keeps text, '' clears the cell; the formatting stays."),
      }).refine((cell) => cell.side !== undefined || cell.value !== undefined, {
        error: "A cell decision gives a side, a value, or both.",
      })).optional().describe("Single cells, overriding the side of the table."),
    }).describe("The cell-by-cell merge: Studio merges the cells one branch changed, and every conflicting cell needs a side or a value."),
  }),
]);

export const resolveMergeConflictsSchema = z.strictObject({
  projectId: projectIdSchema,
  resolutions: z.array(z.union([
    z.strictObject({
      path: conflictPathSchema,
      strategy: z.enum(["BASE", "OURS", "THEIRS"]).describe("Take the whole file from the base, your (current branch) or their version; a version fileAvailability marks false deletes the file."),
    }),
    z.strictObject({
      path: conflictPathSchema,
      decisions: z.array(workbookDecisionSchema).min(1).describe("One decision per conflict openl_read_merge_conflict lists; a sheet taken whole covers its tables."),
    }),
    z.strictObject({
      path: conflictPathSchema,
      conflicts: z.array(textDecisionSchema).describe("One decision per conflict of openl_read_merge_conflict, in their order; the merged lines stay."),
    }),
  ])).min(1).describe("One per conflicted file."),
  message: z.string().optional().describe("Commit message; Studio builds one when omitted."),
});

export const deleteProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  confirmProjectName: nonBlankString.describe("The exact current name of the project, from openl_get_project, to confirm the deletion."),
  comment: commitCommentSchema.optional().describe("Deletion commit message, checked against the comment template of the repository."),
});

export const deleteProjectBranchSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: nonBlankString.describe("The exact name from openl_list_project_branches."),
  confirmDataLoss: z.boolean().optional().describe(
    "Accept the data loss a first attempt reported, once the user approves it.",
  ),
  force: z.boolean().optional().describe("Delete a protected branch, as an eligible manager, once the user approves it."),
});

// =============================================================================
// Project Creation & Cloning Schema (single tool: blank create or clone)
// =============================================================================

export const createProjectSchema = z.strictObject({
  repository: repositoryNameSchema,
  name: z.string().describe(
    "The folder name of the project, and the new name in rules.xml of a copy; unique in the repository. Letters, digits, space, '_' and '-'."
  ),
  template: projectIdSchema.optional().describe(
    "The exact projectId from openl_list_projects of the project to copy, rules, tests and settings, never its displayed name. Omit for a blank project from Studio's \"Empty Project\" template."
  ),
  branch: z.string().optional().describe(
    "Omit for the default branch of the repository. Studio takes an existing branch in any case and creates a missing one: check openl_list_branches first."
  ),
  comment: commitCommentSchema.optional().describe(
    "Commit comment; omit for the one the repository configures, and for a repository whose features.comments is false."
  ),
});

export const deployProjectSchema = z.strictObject({
  projectId: projectIdSchema,
  deploymentName: z.string().optional().describe("The name of a new deployment."),
  repository: z.string().optional().describe("The production repository of a new deployment, by an id or name from openl_list_deploy_repositories."),
  deploymentId: z.string().optional().describe("A deployment from openl_list_deployments to deploy to again."),
  comment: z.string().optional(),
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
  comment: nonBlankString.max(MAX_COMMIT_COMMENT_LENGTH, commitCommentTooLong).optional().describe("Commit comment, ignored by a repository without comments; omit for the one the repository configures."),
  closeAfterSave: z.boolean().optional().describe("Close the project after saving; one without unsaved changes is closed as it is."),
});

// A table is read and written in one shape: its rows of values, and its merged cells and Excel formulas by position.

export const updateTableSchema = z.strictObject({
  ...tableActionBase,
  ...tableGridShape,
});

export const createProjectTableSchema = z.strictObject({
  projectId: projectIdSchema,
  module: nonBlankString.describe("The module to create the table in, a new one with modulePath."),
  ...newModuleParams,
  name: nonBlankString.describe("The table name, a Java identifier, as its header row names it."),
  ...tableGridShape,
});

// =============================================================================
// Trace Debug API Schemas (BETA) — interactive debugger
// =============================================================================

/**
 * Shaping the answer of a profiling run, which completes in a start, a step or a resume: the constant-size `profile`,
 * and the root of the call tree only when asked.
 */
/** The input of a traced run, of a start and of a watch alike. */
const traceInputParams = {
  testRanges: z.string().optional().describe("Test-case ranges of a test table, such as '1-3,5'; a replay does not remember them."),
  inputJson: z.union([z.string(), z.record(z.string(), z.unknown())]).optional().describe("The input of a rule, keyed by parameter name, or { params, runtimeContext? }. Without inputJson and testRanges the input of the previous run is replayed."),
  module: z.string().optional().describe("The module whose context to run in; usually omit."),
};

const traceProfileParams = {
  includeTree: z.boolean().optional().describe("Add the call-tree root line of a profiling run, whose uri and instance openl_expand_trace_tree takes."),
  profileTop: z.number().int().min(1).max(500).optional().describe("How many slowest tables 'profile' lists, 20 by default."),
};

export const startTraceSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  ...traceInputParams,
  stopAtEntry: z.boolean().optional().describe("Suspend at the entry of the table, true by default; false runs to the first breakpoint, or with none to the end."),
  profiling: z.boolean().optional().describe("Retain the call tree, its timings without values, for 'profile' and openl_expand_trace_tree."),
  detailedTitles: z.boolean().optional().describe("Put values in the titles of the call tree; this can make the answers much larger."),
  breakOnErrors: z.boolean().optional().describe("Suspend at an uncaught rule error, true by default; false lets the error end the run."),
  breakpoints: z.array(z.string()).optional().describe("Breakpoint keys, as openl_set_trace_breakpoints takes them, to replace the set with first; left out, the set stays."),
  ...traceProfileParams,
});

export const stepTraceSchema = z.strictObject({
  projectId: projectIdSchema,
  type: z.enum(["into", "over", "out"]).describe("'out' runs the current frame to its exit, the main step for decision tables and spreadsheets; 'into' enters the next call and 'over' runs to the next step of the frame, both for TBasic and loops."),
  withValues: z.boolean().optional().describe("Also answer the values of the current frame as openl_inspect_trace_frame reads them: a step out and its result in one call."),
  ...traceProfileParams,
});

export const resumeTraceSchema = z.strictObject({
  projectId: projectIdSchema,
  attach: z.boolean().optional().describe("Keep waiting for a run an earlier call left running at its timeout, without resuming again, which would run past a stop reached since."),
  timeoutMs: z.number().int().positive().max(600000).optional().describe("How long to wait for the next stop; 30000 by default."),
  ...traceProfileParams,
});

export const inspectTraceFrameSchema = z.strictObject({
  projectId: projectIdSchema,
  frameIndex: z.number().int().nonnegative().describe("The '#N' of a frame of the last stack: 0 is the root, the highest the current frame."),
  withHighlights: z.boolean().optional().describe("Also answer the cell highlights of the frame, keyed by A1 address, and the table grid to lay them on."),
  full: z.boolean().optional().describe("Answer untrimmed: the JSON schemas of the values and every rule name."),
  onlyExecutedSteps: z.boolean().optional().describe("Keep only the steps that computed a value."),
  excludeStepValues: z.array(z.union([z.number(), z.string(), z.boolean()])).optional().describe("Drop the executed steps whose value equals one of these, lazy values resolved first, to hide neutral factors, such as [1] in rating. Not for a table where these values matter."),
});

export const setTraceBreakpointsSchema = z.strictObject({
  projectId: projectIdSchema,
  breakpoints: z.array(z.string()).optional().describe("The keys to replace the whole set with; [] clears it. Leave out to read the set."),
  name: nonBlankString.optional().describe("Without breakpoints, list only the targets whose name contains this, ignoring case."),
});

export const getTraceValueSchema = z.strictObject({
  projectId: projectIdSchema,
  parameterId: z.number().int().nonnegative().describe("The parameterId of a lazy value of openl_inspect_trace_frame, openl_step_trace withValues or openl_watch_trace_cells."),
  withSchema: z.boolean().optional().describe("Also answer the JSON Schema of the value, which is large: ask it only when the type structure matters."),
});

export const expandTraceTreeSchema = z.strictObject({
  projectId: projectIdSchema,
  uri: nonBlankString.describe("The uri of the node: the call-tree root of a stack, or a child of an earlier answer."),
  instance: z.number().int().nonnegative().describe("The instance of that node, the execution of its table from 0."),
  step: nonBlankString.describe("The ref of the step of that node to expand, such as 'R1C0', one whose childrenTotal is above 0."),
  offset: z.number().int().nonnegative().optional().describe("The first sub-call to answer: the nextOffset of the previous page."),
  limit: z.number().int().min(1).max(200).optional().describe("Sub-calls per page, 100 by default; a page of wide nodes can still be large."),
});

export const watchTraceCellsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  cells: z.array(z.string()).min(1).describe("The names of the cells to watch, such as ['$VehiclePriceFactor']."),
  ...traceInputParams,
  withSchema: z.boolean().optional().describe("Also answer the JSON Schema of each value, which is large."),
});

// =============================================================================
// Revision Schemas
// =============================================================================

export const getProjectRevisionsSchema = z.strictObject({
  projectId: projectIdSchema,
  branch: z.string().optional().describe("As openl_list_project_branches names it; omit for the branch the project is on. A branch without the project has no revisions."),
  search: z.string().optional().describe("Matched against the commit comment and the author."),
  techRevs: z.boolean().default(false).describe("Also list technical revisions."),
  ...paginationShape,
});

export const getProjectRevisionChangesSchema = z.strictObject({
  projectId: projectIdSchema,
  revision: nonBlankString.optional().describe("Later revision, a 'revisionNo'; omit for the working copy."),
  base: nonBlankString.optional().describe("Earlier revision; omit for the one 'revision' was made on."),
  path: nonBlankString.optional().describe("A file: tell what changed in it."),
  branch: z.string().optional().describe("Of the revisions of a workbook; omit for the project's."),
  ...paginationShape,
});

// =============================================================================
// Local Changes & Restore Schemas
// =============================================================================

export const listProjectLocalChangesSchema = z.strictObject({
  projectId: projectIdSchema,
  module: nonBlankString.optional().describe("As openl_list_project_modules names it; omit for every module."),
  historyId: nonBlankString.optional().describe("A version of the module: answer instead the tables a restore to it would change."),
}).refine((args) => args.historyId === undefined || args.module !== undefined, {
  error: "historyId is a version of the history of one module: name the module.",
  path: ["historyId"],
});

export const restoreProjectLocalChangeSchema = z.strictObject({
  projectId: projectIdSchema,
  module: nonBlankString.describe("As openl_list_project_local_changes names it."),
  historyId: nonBlankString.describe("The version to restore, from openl_list_project_local_changes."),
});

// =============================================================================
// Test Execution Schemas
// =============================================================================

export const runProjectTestsSchema = z.strictObject({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("A table whose tests to run, or a test table."),
  testRanges: z.string().optional().describe("Tests of the test table tableId, such as '1-3,5'."),
  module: z.string().optional().describe("Module name to run tests from."),
});

/** Whether the options of a test-result read go together: paging options exclude each other, and a summary pages nothing. */
function checkTestResultOptions(data: {
  summary: boolean;
  tableId?: string;
  caseId?: string;
  offset?: number;
  limit?: number;
}, ctx: z.RefinementCtx): void {
  const paged = data.offset !== undefined || data.limit !== undefined;
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
  tableId: tableIdSchema.optional().describe("The test table whose tests to read, every failed one; omit for every test table."),
  caseId: nonBlankString.optional().describe("A test of tableId, to read with its inputs as JSON, whole up to 12,500 characters."),
  failuresOnly: z.boolean().default(true).describe("Only the failed tests; without tableId at most 5 of each test table, the rest counted."),
  offset: z.number().int().nonnegative().optional().describe("The test table to start from, or with tableId the test, as pagination.next_offset names it."),
  limit: z.number().int().positive().max(200).optional().describe("How many test tables a page holds, or with tableId how many tests, 50 by default."),
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
    "Path relative to the project folder, e.g. 'rules/Model.xlsx'; a folder ends with '/'."
  );

const fileBranchSchema = z.string().optional().describe("Fails unless the project is on this branch.");

export const readProjectFileSchema = z.strictObject({
  projectId: projectIdSchema,
  path: z
    .string()
    .default("")
    .describe(
      "Path relative to the project folder; a folder ends with '/' ('' for the root)."
    ),
  revision: z
    .string()
    .optional()
    .describe("A commit from openl_repository_project_revisions to read the file at, not the working copy; the answer then has no 'version'."),
  branch: fileBranchSchema,
});

/** The version of a file a write replaces or a deletion removes, as openl_read_project_file answered it. */
const fileVersionText =
  "The file's 'version' from openl_read_project_file or the last write.";

const writeProjectFileBaseSchema = z.strictObject({
  projectId: projectIdSchema,
  path: filePathSchema,
  createFolders: z
    .boolean()
    .default(true)
    .describe("Create the missing folders of a new file."),
  version: nonBlankString
    .optional()
    .describe(`${fileVersionText} Required to replace a file; without it the write creates a new file and writes nothing over one.`),
  branch: fileBranchSchema,
});

export const writeProjectFileSchema = writeProjectFileBaseSchema.extend({
  content: z
    .string()
    .describe("The whole text of the file."),
  message: z
    .string()
    .optional()
    .describe("Commits the write with every pending change of the project; without it the write stays in the working copy."),
});

export const createUploadLinkSchema = writeProjectFileBaseSchema.extend({
  size: z
    .number()
    .int()
    .nonnegative()
    .optional()
    .describe("Bytes the uploaded file must have; another size is refused."),
  sha256: z
    .string()
    .regex(/^[0-9a-fA-F]{64}$/, "sha256 is the hex SHA-256 of the file, 64 characters.")
    .optional()
    .describe("Hex SHA-256 the uploaded file must have, from `sha256sum`; other content is refused."),
});

export const deleteProjectFileSchema = z.strictObject({
  projectId: projectIdSchema,
  path: filePathSchema,
  version: nonBlankString
    .optional()
    .describe(`${fileVersionText} Required for a file, not for a folder.`),
  branch: fileBranchSchema,
});

export const searchProjectFilesSchema = z.strictObject({
  projectId: projectIdSchema,
  pattern: z
    .string()
    .optional()
    .describe("Ant-glob path pattern, e.g. 'rules/**/*.xlsx'; a name pattern such as '*.xlsx' matches at any depth with recursive."),
  content: z
    .string()
    .optional()
    .describe("Case-insensitive text; workbook cells, compressed ZIP entries and files over 1 MB are never searched: find those by pattern or extensions."),
  extensions: z
    .array(z.string())
    .optional()
    .describe("Without the dot, e.g. ['xlsx','xml']."),
  type: z
    .enum(["FILE", "FOLDER", "ANY"])
    .optional()
    .describe("ANY by default."),
  scope: z
    .enum(["SUBTREE", "ANCESTORS"])
    .optional()
    .describe("SUBTREE (default) searches the project; ANCESTORS walks up from 'from' to the repository root and answers at each level the text file up to 1 MB named exactly 'pattern' (no glob), its content up to 5,000 bytes or contentOmitted."),
  recursive: z
    .boolean()
    .optional()
    .describe("Search nested folders too; false keeps to the top level even with '**'. True by default with content, else false."),
  from: z
    .string()
    .optional()
    .describe("A folder to search in (its entries, or all under it with recursive), or to walk up from with ANCESTORS."),
  revision: z
    .string()
    .optional()
    .describe("A commit to search at instead of the working copy; SUBTREE only."),
  branch: fileBranchSchema,
  fields: z
    .string()
    .optional()
    .describe("Comma-separated match fields, e.g. 'path,name,type'; an ANCESTORS match has every field."),
  ...paginationShape,
});

// Intentionally has no pagination: the tool returns a single aggregated markdown document (one AGENTS.md chain), read
// in parts by its sections.
export const getProjectAgentContextSchema = z.strictObject({
  projectId: projectIdSchema,
  folder: z
    .string()
    .max(1024)
    .optional()
    .describe(
      "A folder of the project, such as 'rules/pricing', for the guidance of the files in it."
    ),
  sections: z
    .array(nonBlankString)
    .min(1)
    .optional()
    .describe("Section headings, case-insensitive; a section comes with its subsections."),
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
      "Add the build number, the start and current time, the system, architecture, Java, CPUs and their load, threads, and the memory of the machine and the JVM."
    ),
});

// =============================================================================
// Guidance Schemas
// =============================================================================

export const listGuidesSchema = z.strictObject({
  type: z
    .enum(GUIDE_TYPES)
    .optional()
    .describe(
      "The type its id starts with: 'spec/' or 'guide/'."
    ),
  search: z
    .string()
    .max(200)
    .optional()
    .describe(
      "A case-insensitive part of the id or the title."
    ),
  ...paginationShape,
});

export const getGuidesSchema = z.strictObject({
  ids: z
    .array(z.string().min(1))
    .min(1)
    .max(5)
    .describe(
      "Ids exactly as openl_list_guides answers them, such as 'spec/rules.xml'."
    ),
});

const copyMovePairSchema = {
  projectId: projectIdSchema,
  sourcePath: filePathSchema.describe("The path of the file in the project."),
  destinationPath: filePathSchema.describe("The new path in the project."),
  branch: fileBranchSchema,
};

/** A copy or a move of a file within a project. */
export const fileTransferSchema = z.strictObject(copyMovePairSchema);

// =============================================================================
// Redeploy Schema
// =============================================================================
