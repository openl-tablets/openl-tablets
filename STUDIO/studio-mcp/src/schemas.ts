/**
 * Zod Schemas for MCP Tool Input Validation
 *
 * This module defines all input schemas for OpenL MCP tools using Zod.
 * Benefits:
 * - Type-safe input validation with runtime checks
 * - Automatic TypeScript type inference
 * - Self-documenting API through schema descriptions
 * - Clear validation error messages
 *
 * To add a new tool schema:
 * 1. Define the schema using z.object() with descriptive field names
 * 2. Add .describe() to each field for documentation
 * 3. Export the schema
 * 4. Attach it to the tool's `schema` field in its handler module
 */

import { z } from "zod";
import { isValidBase64 } from "./content-utils.js";

// Re-export z for convenience
export { z };

// Response format enum
export const ResponseFormat = z
  .enum(["json", "markdown", "markdown_concise", "markdown_detailed"])
  .default("json")
  .describe(
    "Response format: 'json' for structured, round-trippable data (default), 'markdown' for human-readable output, 'markdown_concise' for a brief summary (1-2 paragraphs), or 'markdown_detailed' for full details with context"
  );

// Pagination parameters
export const PaginationParams = z.object({
  limit: z.number().int().positive().max(200).default(50).optional(),
  offset: z.number().int().nonnegative().default(0).optional(),
});

// Project ID: opaque backend identifier from openl_list_projects() response
export const projectIdSchema = z.string().min(1, "Project ID must not be empty.").describe("Project ID returned by backend. Use the exact 'projectId' value from openl_list_projects() response without modification or reformatting.");

export const repositoryNameSchema = z.string().describe("Repository identifier. Pass either the 'id' or the 'name' field from openl_list_repositories() — the tool accepts both (and is case-insensitive). DO NOT invent values like 'Design Repository' or 'design-repo'; the actual names are typically short tokens (e.g. 'Design'). Always call openl_list_repositories() first if you don't already have the value in context.");

export const projectNameSchema = z.string().describe("Project name within the repository (e.g., 'InsuranceRules', 'AutoPremium', 'ClaimProcessing')");

export const tableIdSchema = z.string().describe("Table identifier - unique ID assigned by OpenL Studio (e.g., 'calculatePremium_1234'). VOLATILE: derived from the table's location, so it changes when an edit relocates the table (it had no room to grow in place) — use the 'tableId' returned by the latest openl_update_table/openl_append_table response, or refresh via openl_list_tables().");

export const branchNameSchema = z.string().describe("Git branch name (e.g., 'main', 'development', 'feature/new-rules')");

export const commentSchema = z.string().optional().describe("Commit comment describing the change (e.g., 'Updated CA premium rates', 'Fixed calculation bug')");

// Tool input schemas
export const listRepositoriesSchema = z.object({
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

export const listDeploymentsSchema = z.object({
  repository: z.string().optional().describe("Production repository ID to filter deployments by."),
  project: z.string().optional().describe("Deployed project name to filter deployments by."),
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

export const listProjectsSchema = z.object({
  repository: z.string().optional().describe("Filter by repository name (display name, not ID). Use the 'name' field from openl_list_repositories() response (e.g., if list_repositories returns {id: 'design-repo', name: 'Design Repository'}, use 'Design Repository' here, NOT 'design-repo'). Omit to show projects from all repositories."),
  status: z.enum(["LOCAL", "DELETED", "OPENED", "VIEWING_VERSION", "EDITING", "CLOSED"]).optional().describe("Filter by project status."),
  dependsOn: z.string().optional().describe("Return projects that depend on this project identifier."),
  name: z.string().optional().describe("Project name filter (partial, case-insensitive)."),
  author: z.string().optional().describe("Last-modifying author filter (partial, case-insensitive)."),
  branch: z.string().optional().describe("Branch filter (partial, case-insensitive)."),
  sort: z.enum(["name", "status", "updated"]).optional().describe("Field used to sort the returned page."),
  include: z.array(z.enum(["summary", "status", "deleted", "descriptor"])).optional().describe("Optional response expansions and listing behavior from the Studio API."),
  tags: z.record(z.string(), z.string()).optional().describe("Filter by project tags. Tags must be prefixed with 'tags.' in the query string (e.g., tags.version='1.0', tags.environment='production'). This is handled automatically by the API client - provide as object with tag names as keys."),
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

export const getProjectSchema = z.object({
  projectId: projectIdSchema,
  include: z.array(z.enum(["summary", "status", "deleted", "descriptor"])).optional().describe("Optional response expansions from the Studio API."),
  response_format: ResponseFormat.optional(),
}).strict();

export const projectStatusSchema = z.object({
  projectId: projectIdSchema,
  branch: branchNameSchema.optional().describe(
    "Optional target branch. With wait=true (default), the tool switches an opened design project to this branch before validating it. With wait=false, this is a read-only assertion and Studio returns 409 when it differs from the currently opened branch. Omit for repositories that do not support branches and for repository 'local'."
  ),
  wait: z.boolean().default(true).optional().describe(
    "When true (default), returns a conclusive compile state: an idle project is compiled lazily through Studio's tables API, while an already-running compilation is followed over the real-time status topic until compileState is terminal (ok/warnings/errors). Progress notifications are emitted when available. Set false only for a one-shot read-only snapshot that may return idle or compiling."
  ),
  timeoutMs: z.number().int().positive().max(600000).default(120000).optional().describe(
    "Max time in milliseconds to wait for compilation when wait=true. On expiry, the last-seen status is returned (no error). Default 120000 (2 minutes). Cap 600000 (10 minutes). Ignored when wait=false."
  ),
  severity: z.array(z.enum(["ERROR", "WARN", "INFO"])).optional().describe(
    "Filter compilation.messages.items to only these severities. Useful when the project has many warnings and you want to isolate errors: pass severity: ['ERROR']. Default: all severities. Note: items are always sorted ERROR → WARN → INFO before any filter or truncation is applied, so errors are visible without this filter."
  ),
  maxMessages: z.number().int().positive().max(1000).optional().describe(
    "Cap the number of items returned in compilation.messages.items. The list is sorted ERROR → WARN → INFO first so the most actionable items are preserved when capped. Pair with severity to bound very large diagnostic lists. Default: no cap (relies on the response-format character truncation)."
  ),
  response_format: ResponseFormat.optional(),
}).strict();

export const openProjectSchema = z.object({
  projectId: projectIdSchema,
  branch: branchNameSchema.optional().describe("Open project on a specific Git branch (e.g., 'main', 'development', 'feature/new-rules')"),
  revision: z.string().optional().describe("Open project at a specific Git revision/commit hash for read-only viewing"),
  openDependencies: z.boolean().optional().describe("Also open dependency projects (backend default false)."),
  response_format: ResponseFormat.optional(),
}).strict();

export const closeProjectSchema = z.object({
  projectId: projectIdSchema,
  saveChanges: z.boolean().optional().describe("If true, save changes before closing (requires comment). If false or omitted and project has unsaved changes, will error unless discardChanges is true."),
  comment: commentSchema.describe("Git commit comment. Required if saveChanges is true. Optional if saveChanges is false or omitted."),
  discardChanges: z.boolean().optional().describe("If true, close without saving (unsaved changes will be lost). When project is EDITING, you must also set confirmDiscard: true to confirm."),
  confirmDiscard: z.boolean().optional().describe("When closing with discardChanges: true and project has unsaved changes, must be set to true (explicitly) to confirm. Omitted or false will return a confirmation prompt. Ask the user first, then call again with confirmDiscard: true."),
  response_format: ResponseFormat.optional(),
}).strict();

export const listTablesSchema = z.object({
  projectId: projectIdSchema,
  kind: z.array(z.string()).optional().describe("Filter by table kinds (array of strings). Valid values: 'Rules', 'Spreadsheet', 'Datatype', 'Data', 'Test', 'TBasic', 'Column Match', 'Method', 'Run', 'Constants', 'Conditions', 'Actions', 'Returns', 'Environment', 'Properties', 'Other'. Omit to show all kinds."),
  name: z.string().optional().describe("Filter by table name fragment (e.g., 'calculate', 'Premium'). Omit to show all tables."),
  properties: z.record(z.string(), z.string()).optional().describe("Filter by project properties. Properties must be prefixed with 'properties.' in the query string (e.g., properties.state='CA', properties.lob='Auto'). This is handled automatically by the API client."),
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

export const getTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  startRow: z.number().int().min(0).optional().describe("Zero-based index of the first source row; omit to start at the top. Combine with maxRows to read a large table in slices."),
  maxRows: z.number().int().min(1).optional().describe("Maximum number of source rows, counted from startRow; omit to read to the end. A windowed response carries totalRows."),
  styles: z.boolean().optional().describe("If true, each raw cell carries its Excel style (background/font colour, bold/italic/underline, alignment, indent, borders)."),
  response_format: ResponseFormat.optional(),
}).strict();

export const deleteTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const runTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID of a regular executable table. Use openl_list_tables() to discover it; use the Tests tools instead for Test tables."),
  inputJson: z.union([
    z.array(z.unknown()),
    z.record(z.string(), z.unknown()),
  ]).superRefine((input, ctx) => {
    if (!Array.isArray(input) && Array.isArray(input.params)) {
      ctx.addIssue({
        code: "custom",
        path: ["params"],
        message: "params must be an object keyed by method parameter name; Studio does not bind positional arrays in the params wrapper. Use { params: { parameterName: value }, runtimeContext? }.",
      });
    }
  }).describe("Method input as JSON. Use an object keyed by method parameter name, or { params: { parameterName: value }, runtimeContext? }. A top-level array is the value of a single array-valued parameter, not a positional argument list. The value is sent to Studio unchanged."),
  fromModule: z.string().trim().min(1).optional().describe("Optional module name whose runtime context should be used. Usually omit; discover module names with openl_list_project_modules()."),
  withSchema: z.boolean().optional().describe("Include result and parameter JSON Schemas. Default false because schemas can be large."),
  timeoutMs: z.number().int().positive().max(600000).default(120000).optional().describe("Maximum time for the complete Studio start-and-result workflow, in milliseconds. Default 120000 (2 minutes), maximum 600000 (10 minutes). A timeout cancels the pending Studio run."),
  response_format: ResponseFormat.optional(),
}).strict();

export const getTableDependenciesSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("Optional table whose dependency neighborhood to return. Omit to return the whole project (or module) graph."),
  module: z.string().trim().min(1).optional().describe("When tableId is omitted, limit the project graph to this module. Discover names with openl_list_project_modules()."),
  layer: z.enum(["executable", "datatype", "all"]).optional().describe("When tableId is omitted, return executable tables, datatype/vocabulary nodes, or both (backend default all)."),
  direction: z.enum(["DEPENDENCIES", "DEPENDENTS", "BOTH"]).optional().describe("When tableId is provided, relations to traverse (backend default BOTH)."),
  depth: z.number().int().min(1).optional().describe("When tableId is provided, maximum traversal depth from that table."),
  response_format: ResponseFormat.optional(),
}).strict().superRefine((value, ctx) => {
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

export const listProjectModulesSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const listModuleSheetsSchema = z.object({
  projectId: projectIdSchema,
  moduleName: z.string().trim().min(1).describe("Module name exactly as returned by openl_list_project_modules()."),
  response_format: ResponseFormat.optional(),
}).strict();

export const listTablePropertyDefinitionsSchema = z.object({
  projectId: projectIdSchema,
  tableType: z.string().trim().min(1).optional().describe("Optional public table kind. Omit for properties allowed inside a Properties table; provide a kind to get properties allowed on that table kind."),
  response_format: ResponseFormat.optional(),
}).strict();

export const copyTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("ID of the source table to copy."),
  moduleName: z.string().trim().min(1).describe("Name of the destination module. It must already exist unless modulePath is supplied."),
  modulePath: z.string().regex(/.+\.xlsx$/i).optional().describe("Project-relative .xlsx path for a new destination module. Omit when moduleName already exists."),
  name: z.string().trim().min(1).describe("Name for the copied table."),
  sheetName: z.string().optional().describe("Destination worksheet name. Defaults to the copied table's name."),
  properties: z.array(z.object({
    name: z.string().trim().min(1).describe("Property name."),
    value: z.string().optional().describe("Property value in display-string form. A blank value removes the property."),
  }).strict()).optional().describe("Replacement table properties. Omit to retain the source table's properties."),
  response_format: ResponseFormat.optional(),
}).strict();

const arrayCellStringSchema = z.string().regex(
  // Mirrors Studio's StringUtils.isSpaceOrControl(char), which is broader than
  // JavaScript \s: ISO controls and every Unicode space separator are trimmed.
  /^[^\u0000-\u0020\u007F-\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000](?:[\s\S]*[^\u0000-\u0020\u007F-\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000])?$/,
  "Array string elements must be non-empty and have no leading or trailing characters trimmed by Studio (whitespace or ISO controls).",
);
const arrayCellNonNullValueSchema = z.union([
  arrayCellStringSchema,
  z.number(),
  z.boolean(),
]);
const arrayCellValueSchema = z.union([
  z.array(arrayCellNonNullValueSchema).length(1, {
    error: "A single-value array must contain exactly one non-null scalar value.",
  }),
  z.array(z.union([arrayCellNonNullValueSchema, z.null()])).min(2, {
    error: "An array containing null must have at least two elements; [null] is not representable.",
  }),
]);

/** The round-trip-safe raw cell value domain exposed by the Studio API. */
const cellValueSchema = z.union([
  z.string(),
  z.number(),
  z.boolean(),
  z.null(),
  arrayCellValueSchema,
], {
  error: "Cell value must be a string, number, boolean, null, or a representable one-dimensional array of those scalar values. Arrays must be non-empty; [null], nested arrays, objects, and string elements surrounded by Studio-trimmed whitespace or control characters are not supported.",
});

const rawTableCellSchema = z.strictObject({
  value: cellValueSchema.optional().describe("Cell value as a scalar or representable one-dimensional scalar array. Null or omitted is an empty cell."),
  colspan: z.number().int().min(1).optional(),
  rowspan: z.number().int().min(1).optional(),
  covered: z.boolean().optional(),
  cell: z.string().optional(),
}).describe("Writable raw cell. Studio exposes style only when reading with styles=true; table write APIs do not support changing style, so style is intentionally rejected.");

export const appendTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  appendData: z.strictObject({
    tableType: z.literal("RawSource"),
    rows: z.array(z.array(rawTableCellSchema).min(1)).min(1).describe("Non-empty source rows to append. Every row must cover the table's full width; use { value: null } for an intentionally blank cell."),
  }).describe("RawSource append payload. Typed table append DTOs are intentionally unsupported because they are lossy and incomplete."),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Raw table-source actions (POST /projects/{projectId}/tables/{tableId}/actions)
//
// One narrow tool per operation×orientation. Each applies a SINGLE in-place edit
// to the table's RAW source (any table type), like every table-content tool.
// Positions are
// 0-based; row 0 is the header row and column 0 carries the leading labels, so
// insert positions start at 1. An edit that relocates the table changes its id —
// the tools surface the new id the same way update/append do.
// =============================================================================

/** A cell value for a raw table-source edit. */
const rawCellInputSchema = z.object({
  value: cellValueSchema.optional().describe("Cell value as a scalar or representable one-dimensional scalar array. Null or omitted is an empty cell."),
  colspan: z.number().int().min(1).optional().describe("Number of columns this cell spans (>= 2 to merge; omit or 1 for a single column)."),
  rowspan: z.number().int().min(1).optional().describe("Number of rows this cell spans (>= 2 to merge; omit or 1 for a single row)."),
  covered: z.boolean().optional().describe("Marks a cell covered by another cell's span; its value is ignored."),
}).strict();

const rowCellsSchema = z.array(rawCellInputSchema).min(1).describe(
  "Row cells, left to right. Required and non-empty — provide one cell per column (use { value: null } for a blank cell). A cell may set colspan/rowspan to merge. Must not be wider than the table.",
);
const columnCellsSchema = z.array(rawCellInputSchema).min(1).describe(
  "Column cells, top to bottom. Required and non-empty — provide one cell per row (use { value: null } for a blank cell). A cell may set colspan/rowspan to merge. Must not be taller than the table.",
);

/** One or more lines (rows or columns), each a non-empty list of cells — the 2D block shared by the rows/columns/range tools. */
const cellBlockSchema = z.array(z.array(rawCellInputSchema).min(1)).min(1);
/** One or more rows: outer = rows (top to bottom), inner = cells in that row (left to right). */
const rowsBlockSchema = cellBlockSchema.describe(
  "Rows top to bottom, each a non-empty list of cells left to right (one cell per column; use { value: null } for a blank cell). Pass one row to add/insert a single row, several for a block. Each row as wide as the table.",
);
/** One or more columns: outer = columns (left to right), inner = cells in that column (top to bottom). */
const columnsBlockSchema = cellBlockSchema.describe(
  "Columns left to right, each a non-empty list of cells top to bottom (one cell per row; use { value: null } for a blank cell). Pass one column to add/insert a single column, several for a block. Each column as tall as the table.",
);

const tableActionBase = {
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  response_format: ResponseFormat.optional(),
};

export const updateTableRowSchema = z.object({
  ...tableActionBase,
  position: z.number().int().min(0).describe("0-based index of the row to overwrite (0..height-1). The table is not resized."),
  cells: rowCellsSchema,
}).strict();

export const updateTableColumnSchema = z.object({
  ...tableActionBase,
  position: z.number().int().min(0).describe("0-based index of the column to overwrite (0..width-1). The table is not resized."),
  cells: columnCellsSchema,
}).strict();

export const updateTableCellSchema = z.object({
  ...tableActionBase,
  row: z.number().int().min(0).describe("0-based row index of the cell (0..height-1)."),
  column: z.number().int().min(0).describe("0-based column index of the cell (0..width-1)."),
  value: cellValueSchema.describe("New scalar or representable one-dimensional scalar-array value. Required — pass null explicitly to clear so the intent is unambiguous."),
}).strict();

export const mergeTableCellsSchema = z.object({
  ...tableActionBase,
  row: z.number().int().min(0).describe("0-based row index of the top-left cell of the range (0..height-1)."),
  column: z.number().int().min(0).describe("0-based column index of the top-left cell of the range (0..width-1)."),
  rowspan: z.number().int().min(1).describe("Number of rows the merged cell spans (>= 1)."),
  colspan: z.number().int().min(1).describe("Number of columns the merged cell spans (>= 1)."),
}).strict().refine((d) => d.rowspan * d.colspan > 1, {
  // A 1×1 merge is a no-op; the range must cover more than one cell (the tool's contract).
  error: "A merge must cover more than one cell: rowspan × colspan must be greater than 1.",
  path: ["rowspan"],
});

export const unmergeTableCellsSchema = z.object({
  ...tableActionBase,
  row: z.number().int().min(0).describe("0-based row index of any cell in the merged region (0..height-1)."),
  column: z.number().int().min(0).describe("0-based column index of any cell in the merged region (0..width-1)."),
}).strict();

// -----------------------------------------------------------------------------
// Row / column table-source actions — append/insert/delete ONE OR MORE rows or
// columns with a single tool per operation. The studio takes one block target
// (`rows`/`columns`, accepting one or more), so a single row/column is just a
// one-element block. (Cell- and range-level edits stay separate below: they are
// different shapes, not a one-vs-many choice.)
// -----------------------------------------------------------------------------

export const appendTableRowsSchema = z.object({
  ...tableActionBase,
  cells: rowsBlockSchema,
}).strict();

export const appendTableColumnsSchema = z.object({
  ...tableActionBase,
  cells: columnsBlockSchema,
}).strict();

export const insertTableRowsSchema = z.object({
  ...tableActionBase,
  position: z.number().int().min(1).describe("0-based index the first new row will occupy (1..height; height appends to the end). Rows at and below it shift down."),
  cells: rowsBlockSchema,
}).strict();

export const insertTableColumnsSchema = z.object({
  ...tableActionBase,
  position: z.number().int().min(1).describe("0-based index the first new column will occupy (1..width; width appends to the end). Columns at and to the right of it shift right."),
  cells: columnsBlockSchema,
}).strict();

export const deleteTableRowsSchema = z.object({
  ...tableActionBase,
  position: z.number().int().min(1).describe("0-based index of the first body row to delete (1..height-1). The header row (0) cannot be deleted. Rows below the deleted block shift up."),
  count: z.number().int().min(1).optional().describe("Number of rows to delete starting at 'position' (default 1)."),
}).strict();

export const deleteTableColumnsSchema = z.object({
  ...tableActionBase,
  position: z.number().int().min(1).describe("0-based index of the first column to delete (1..width-1). The leading-label column (0) cannot be deleted. Columns to the right of the deleted block shift left."),
  count: z.number().int().min(1).optional().describe("Number of columns to delete starting at 'position' (default 1)."),
}).strict();

export const updateTableRangeSchema = z.object({
  ...tableActionBase,
  row: z.number().int().min(0).describe("0-based row index of the top-left cell of the range (0..height-1)."),
  column: z.number().int().min(0).describe("0-based column index of the top-left cell of the range (0..width-1)."),
  cells: cellBlockSchema.describe(
    "Block rows top to bottom, each a non-empty list of cells left to right. Anchored at ('row','column'); must cover more than one cell and fit within the table (the table is not resized).",
  ),
}).strict().refine((d) => {
  // The range must cover more than one cell. Only a single un-spanned cell fails
  // that — a lone cell with colspan/rowspan still covers more than one cell (the
  // studio's UpdateRange allows colspan/rowspan), so don't reject it locally.
  const only = d.cells.length === 1 && d.cells[0].length === 1 ? d.cells[0][0] : undefined;
  if (!only) return true; // 2+ input cells already cover more than one cell
  return (only.colspan ?? 1) > 1 || (only.rowspan ?? 1) > 1;
}, {
  // A single un-spanned cell — use openl_update_table_cell instead.
  error: "An update range must cover more than one cell. For a single cell use openl_update_table_cell.",
  path: ["cells"],
});

export const listBranchesSchema = z.object({
  repository: repositoryNameSchema,
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

export const createBranchSchema = z.object({
  projectId: projectIdSchema,
  branchName: branchNameSchema,
  revision: z.string().optional().describe("Revision to branch from. Allows to branch from specific revision, tag or another branch. If not specified, HEAD revision will be used."),
  response_format: ResponseFormat.optional(),
}).strict();

export const listProjectBranchesSchema = z.object({
  projectId: projectIdSchema,
  scope: z.enum(["project", "repository"]).default("project").optional().describe(
    "Branches to list: 'project' (default) returns branches that already hold the project and can be switched to; 'repository' returns every repository branch, including merge targets that do not hold the project yet.",
  ),
  response_format: ResponseFormat.optional(),
}).strict();

const mergeRequestFields = {
  projectId: projectIdSchema,
  otherBranch: z.string().trim().min(1).describe("The other branch: source for receive mode, target for send mode. Discover all merge targets with openl_list_project_branches(scope='repository')."),
  mode: z.enum(["receive", "send"]).describe("receive merges the other branch into the project's current branch; send merges the current branch into otherBranch."),
};

export const checkProjectMergeSchema = z.object({
  ...mergeRequestFields,
  response_format: ResponseFormat.optional(),
}).strict();

export const mergeProjectBranchesSchema = z.object({
  ...mergeRequestFields,
  force: z.boolean().optional().describe("Bypass eligible protected-target restrictions. Default false. Use only after Studio reports blockedBy='bypass-required'."),
  confirmForce: z.boolean().optional().describe("Must be true when force=true, confirming the protected-branch bypass."),
  response_format: ResponseFormat.optional(),
}).strict().superRefine((value, ctx) => {
  if (value.force && value.confirmForce !== true) {
    ctx.addIssue({ code: "custom", path: ["confirmForce"], message: "confirmForce must be true when force=true." });
  }
});

export const getMergeConflictsSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const readMergeConflictFileSchema = z.object({
  projectId: projectIdSchema,
  file: z.string().trim().min(1).describe("Project-relative conflicted file path from openl_get_merge_conflicts()."),
  side: z.enum(["BASE", "OURS", "THEIRS"]).describe("Version to read: common ancestor, current branch, or merging branch."),
  encoding: z.enum(["auto", "utf-8", "base64"]).optional().describe("Content encoding. Default auto detects binary content."),
  offset: z.number().int().nonnegative().optional().describe("Byte offset in the downloaded file. Default 0."),
  length: z.number().int().min(1).max(16000).optional().describe("Target bytes returned from offset. Default and maximum target 16000; a UTF-8 response may add up to 3 bytes to finish a character. Continue with nextOffset when hasMore is true."),
  response_format: ResponseFormat.optional(),
}).strict();

export const cancelMergeConflictsSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const deleteProjectSchema = z.object({
  projectId: projectIdSchema,
  confirmProjectName: z.string().trim().min(1).describe("Required safety confirmation: exact project name returned by openl_get_project(). The delete is rejected if it does not match."),
  comment: z.string().optional().describe("Optional deletion commit message, validated by the repository's comment template when configured."),
  response_format: ResponseFormat.optional(),
}).strict();

export const deleteProjectBranchSchema = z.object({
  projectId: projectIdSchema,
  branch: z.string().trim().min(1).describe("Exact branch name from openl_list_project_branches(). The repository base branch cannot be deleted."),
  confirmBranchName: z.string().trim().min(1).describe("Required safety confirmation; must exactly equal branch."),
  confirmDataLoss: z.boolean().optional().describe(
    "Explicitly allow deletion after the tool reports that the branch has commits absent from the base branch, has unsaved working-copy changes, or its divergence from base cannot be verified (for example, the base is unknown, the merge check fails or checks different branches, or the target is not current). Omit for the initial safe-delete attempt.",
  ),
  force: z.boolean().optional().describe("Bypass protected-branch restrictions for eligible managers. Default false."),
  confirmForce: z.boolean().optional().describe("Must be true when force=true, confirming the protected-branch bypass."),
  response_format: ResponseFormat.optional(),
}).strict().superRefine((value, ctx) => {
  if (value.branch !== value.confirmBranchName) {
    ctx.addIssue({ code: "custom", path: ["confirmBranchName"], message: "confirmBranchName must exactly equal branch." });
  }
  if (value.force && value.confirmForce !== true) {
    ctx.addIssue({ code: "custom", path: ["confirmForce"], message: "confirmForce must be true when force=true." });
  }
});

// =============================================================================
// Project Creation & Cloning Schema (single tool: blank create or clone)
// =============================================================================

export const createProjectSchema = z.object({
  repository: repositoryNameSchema,
  projectName: projectNameSchema.describe(
    "Name for the new project (the ticket's `project`). Becomes the project folder name and — when cloning — the renamed project name written into rules.xml. Must be unique in the repository; a collision is rejected with 409. Allowed characters: letters, digits, space, '_' and '-'."
  ),
  template: projectIdSchema.min(1, "Source projectId must not be empty.").optional().describe(
    "How to create the project (the ticket's `template`). OMIT to create a BLANK project from the default empty skeleton. To CLONE an existing project, pass its exact opaque projectId from openl_list_projects() without modification: its full structure is copied (rules, tests, settings, request/response examples) and the project is renamed to projectName. Never pass the displayed project name because mapped repositories may contain multiple projects with the same name."
  ),
  branch: branchNameSchema.optional().describe(
    "Target branch for either BLANK creation or CLONING. Omit for the repository's configured/default branch. Studio selects an existing branch case-insensitively; when the branch does not exist, Studio may create it from the repository base branch. Use openl_list_branches() first to avoid accidental branch creation."
  ),
  comment: commentSchema.describe(
    "Commit comment for audit. Applied to both BLANK creation and CLONING; Studio supplies its configured create/copy comment when omitted."
  ),
  response_format: ResponseFormat.optional(),
}).strict();

export const deployProjectSchema = z.object({
  projectId: projectIdSchema.describe("Project ID to deploy. Use the exact 'projectId' value from openl_list_projects() response."),
  deploymentName: z.string().describe("Name for the deployment (e.g., 'InsuranceRules', 'AutoPremium'). This will be the deployment identifier."),
  productionRepositoryId: z.string().describe("Target production repository name (display name, not ID). Use the 'name' field from openl_list_deploy_repositories() response (e.g., if list_deploy_repositories returns {id: 'production-deploy', name: 'Production Deployment'}, use 'Production Deployment' here, NOT 'production-deploy'). Must be configured in OpenL Studio."),
  comment: commentSchema.describe("Deployment reason comment (e.g., 'Deploy version 1.2.0', 'Production release')"),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Phase 1: New Tool Schemas
// =============================================================================

export const saveProjectSchema = z.object({
  projectId: projectIdSchema,
  comment: z.string().trim().min(1).describe("Required. Comment for the new revision (commit message). Save only works when project status is EDITING; after save a new revision is created and project transitions to OPENED."),
  closeAfterSave: z.boolean().optional().describe("Optional. If true, close the project after saving (sends status CLOSED with comment in one request). Use when user asks to 'save and close'."),
  response_format: ResponseFormat.optional(),
}).strict();

// Table content contracts are deliberately RawSource-only. Studio's typed table
// DTOs are incomplete and lossy, so exposing them here would advertise edits
// that cannot reliably round-trip the underlying workbook.

const rawTableViewSchema = z.strictObject({
    tableType: z.literal("RawSource"),
    name: z.string().optional().describe("Table name (a valid Java identifier)."),
    id: z.string().optional().describe("Table id; ignored on create and validated against the path on update."),
    kind: z.enum(["Rules", "Spreadsheet", "Datatype", "Data", "Test", "TBasic", "Column Match", "Method", "Run", "Constants", "Conditions", "Actions", "Returns", "Environment", "Properties", "Other"]).optional().describe("Informational table kind."),
    messages: z.array(z.any()).optional().describe("Read-only diagnostics tolerated when a get response is round-tripped."),
    pos: z.string().optional(),
    source: z.array(z.array(rawTableCellSchema)).describe("Complete 2D source matrix. Preserve cell positions, covered placeholders, and spans when replacing a table. Remove read-only style objects returned by styles=true; Studio table write APIs cannot change formatting."),
    totalRows: z.number().int().optional().describe("Total row count when the response contains a window."),
}).describe("Complete RawSource table structure. Typed table DTOs are intentionally unsupported because they cannot reliably round-trip workbook content.");

export const updateTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema,
  view: rawTableViewSchema.describe("Full, non-windowed RawSource structure from openl_get_table() with modifications applied. Send the complete source matrix, not only changed cells; a view carrying totalRows is rejected, and the handler independently rejects a source with fewer rows than the live table."),
  response_format: ResponseFormat.optional(),
}).strict().superRefine((value, ctx) => {
  if (value.view.totalRows !== undefined) {
    ctx.addIssue({
      code: "custom",
      path: ["view", "totalRows"],
      message: "A windowed RawSource view cannot replace the whole table because omitted rows would be deleted. Use narrow raw table actions to edit a large table; remove rows only with openl_delete_table_rows and an explicit position/count.",
    });
  }
});

const createTableViewSchema = rawTableViewSchema.omit({ totalRows: true }).extend({
  name: z.string().trim().min(1).describe("Required nonblank table name (a valid Java identifier, e.g. 'calculatePremium')."),
});

export const createProjectTableSchema = z.object({
  projectId: projectIdSchema,
  moduleName: z.string().trim().min(1).describe("Name of an existing project module where the table will be created (for example, 'Main' or 'Rules')."),
  modulePath: z.string().regex(/.+\.xlsx$/i).optional().describe("Project-relative .xlsx path for a new module. When omitted, moduleName must identify an existing module."),
  sheetName: z.string().optional().describe("Name of the sheet where the table will be created within the Excel file. If not provided, the table name will be used as the sheet name."),
  table: createTableViewSchema,
  response_format: ResponseFormat.optional(),
}).strict();

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

export const startTraceSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID to debug (e.g., 'calculatePremium_1234'). Get from openl_list_tables()."),
  testRanges: z.string().optional().describe("For test tables: comma-separated test-case ranges (e.g., '1-3,5'). Omit for regular rule execution."),
  inputJson: z.union([z.string(), z.record(z.string(), z.any())]).optional().describe("For regular rules: JSON input. Use object with params (required) and runtimeContext (optional). E.g. { params: { age: 25 }, runtimeContext: { lob: 'Auto' } }. Omit BOTH inputJson and testRanges to replay the previous run's remembered input (e.g. restarting with profiling or new breakpoints)."),
  fromModule: z.string().optional().describe("Module name to trace in the context of a specific opened module. Usually omit."),
  stopAtEntry: z.boolean().optional().describe("Suspend at the entry of the first frame (default true). Set false to run straight to the first breakpoint — or, with no breakpoints, to completion."),
  profiling: z.boolean().optional().describe("Retain the executed call tree — structure and timings, NO values (default false). With stopAtEntry: false and no breakpoints the run completes in this single call and returns a constant-size 'profile' overview (top-N slowest tables); the tree's root node comes with includeTree: true and is browsed level by level with openl_expand_trace_tree."),
  detailedTitles: z.boolean().optional().describe("Build value-rich business-view titles in the retained tree (backend default false). This can substantially increase response size."),
  breakOnErrors: z.boolean().optional().describe("Suspend on an uncaught rule error so its frame can be inspected (backend default true). Set false to let the error terminate the run."),
  breakpoints: z.array(z.string()).optional().describe("Initial breakpoint set — REPLACES the current set before starting. Key forms: '<name>' (entry of any same-named table), '<uri>' (entry of that table), '<uri>#R{r}C{c}' (spreadsheet cell), '<uri>#rule' (any decision-table rule fires), '<uri>#<ruleName>' (specific rule fires). Append '@N' to any key to break only on the table's N-th execution (0-based) — e.g. '<uri>#R48C0@3' hits the 4th run; N matches frames[].instance and a watch series' instance, so a watch outlier at instance 3 is reached with '@3'. Without '@N' a cell breakpoint hits EVERY pass."),
  ...traceProfileParams,
  response_format: ResponseFormat.optional(),
}).strict();

export const stepTraceSchema = z.object({
  projectId: projectIdSchema,
  type: z.enum(["into", "over", "out"]).describe("'out' (main tool for declarative rules — decision tables, spreadsheets, rating): run the current frame to its own exit so its result is inspectable, then continue in the caller. 'into' / 'over' are ADVANCED (imperative TBasic / loops): 'into' enters the next call or sub-step; 'over' advances to the next sub-step of the current frame (nested calls run through). For 'which table returned what', prefer 'out' plus breakpoints over stepping through expressions."),
  withValues: z.boolean().optional().describe("After the step, also return the active frame's variables (the same content as openl_inspect_trace_frame on the top frame) as 'variables' — saves the usual step→inspect round-trip when you step 'out' to read a frame's result. Default false."),
  ...traceProfileParams,
  response_format: ResponseFormat.optional(),
}).strict();

export const resumeTraceSchema = z.object({
  projectId: projectIdSchema,
  timeoutMs: z.number().int().positive().max(600000).optional().describe("Maximum time to wait for the next suspension or completion, in milliseconds. Default 30000, cap 600000. On timeout the current (still running) status is returned — call openl_resume_trace again to keep waiting, or openl_stop_trace to give up."),
  ...traceProfileParams,
  response_format: ResponseFormat.optional(),
}).strict();

export const inspectTraceFrameSchema = z.object({
  projectId: projectIdSchema,
  frameIndex: z.number().int().nonnegative().describe("Stack frame index from the frames[] of the last stack response (0 = root, highest = current)."),
  withHighlights: z.boolean().optional().describe("Also return the frame's cell-highlight overlay (A1-keyed) plus the raw table grid to merge it with (default false)."),
  full: z.boolean().optional().describe("Return the complete untrimmed response, including value JSON schemas (default false — trimmed via ?fields to save tokens)."),
  onlyExecutedSteps: z.boolean().optional().describe("Keep only executed steps (drop pending/current-without-value) so the response is just the computed factors (default false)."),
  excludeStepValues: z.array(z.union([z.number(), z.string(), z.boolean()])).optional().describe("Drop executed steps whose scalar value equals one of these — to hide neutral factors and surface the outlier (e.g. [1] in rating, where a factor of 1.0 means 'no effect'). Lazy step values are resolved before the comparison, so a neutral factor that came lazy is dropped too. Do not use for tables where those values are meaningful."),
  response_format: ResponseFormat.optional(),
}).strict();

export const setTraceBreakpointsSchema = z.object({
  projectId: projectIdSchema,
  set: z.array(z.string()).optional().describe("When provided, REPLACES the whole breakpoint set (empty array clears all). Key forms: '<name>' (entry of any same-named table), '<uri>' (entry of that table), '<uri>#R{r}C{c}' (spreadsheet cell), '<uri>#rule' (any decision-table rule fires), '<uri>#<ruleName>' (specific rule fires). Append '@N' to any key to break only on the table's N-th execution (0-based, matching frames[].instance and a watch series' instance) — e.g. '<uri>#R48C0@3'. Omit to just read the current set and available targets."),
  response_format: ResponseFormat.optional(),
}).strict();

export const getTraceValueSchema = z.object({
  projectId: projectIdSchema,
  parameterId: z.number().int().nonnegative().describe("Parameter ID from a lazy ParameterValue (lazy: true) returned by openl_inspect_trace_frame."),
  withSchema: z.boolean().optional().describe("Also return the value's JSON Schema (default false — the schema is large and rarely needed; the value itself already shows the structure)."),
  response_format: ResponseFormat.optional(),
}).strict();

export const expandTraceTreeSchema = z.object({
  projectId: projectIdSchema,
  uri: z.string().describe("Source URI of the node whose step to expand: the root node's `uri` from the /stack `tree` (a profiling openl_start_trace / openl_resume_trace with includeTree: true), or a child node's `uri` from an earlier openl_expand_trace_tree page."),
  instance: z.number().int().nonnegative().describe("Zero-based execution index of that node in the run (its `instance`; 0 for the root) — picks the exact loop iteration when the table ran more than once."),
  step: z.string().describe("Reference of the step within that node to expand, e.g. 'R1C0' — a step whose `childrenTotal` > 0 (a step with childrenTotal 0 or absent made no sub-calls)."),
  offset: z.number().int().nonnegative().optional().describe("Index of the first sub-call to return, for paging a loop's thousands of children (default 0). When the response's total exceeds offset + returned children, call again with offset advanced by the returned count (the reply's nextOffset)."),
  limit: z.number().int().min(1).max(200).optional().describe("How many sub-calls to return per page (backend default 100). Keep it modest — a page of many wide nodes can still be large."),
  response_format: ResponseFormat.optional(),
}).strict();

export const stopTraceSchema = z.object({
  projectId: projectIdSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const watchTraceCellsSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID to run (e.g., 'calculatePremium_1234'). Get from openl_list_tables()."),
  cells: z.array(z.string()).min(1).describe("Cell names to watch, e.g. ['$VehiclePriceFactor']. The value of each named cell is captured at EVERY execution of its table across the whole run — one series per cell."),
  testRanges: z.string().optional().describe("For test tables: comma-separated test-case ranges (e.g., '1-3,5')."),
  inputJson: z.union([z.string(), z.record(z.string(), z.any())]).optional().describe("For regular rules: JSON input { params, runtimeContext? }. Omit BOTH inputJson and testRanges to replay the previous run's remembered input."),
  fromModule: z.string().optional().describe("Module name to run in the context of a specific opened module. Usually omit."),
  withSchema: z.boolean().optional().describe("Include each watched value's JSON Schema (default false — the schema is large and rarely needed; a value that came lazy still carries its parameterId for openl_get_trace_value)."),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Repository Features & Revisions Schemas
// =============================================================================

export const getRepositoryFeaturesSchema = z.object({
  repository: repositoryNameSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const getProjectRevisionsSchema = z.object({
  projectId: projectIdSchema,
  search: z.string().optional().describe("Search term to filter revisions by commit message or author"),
  techRevs: z.boolean().default(false).optional().describe("Include technical revisions (default: false)"),
  offset: z.number().int().nonnegative().optional().describe("Item offset (0-based). Mutually exclusive with page."),
  page: z.number().int().nonnegative().optional().describe("Page number (0-based). Mutually exclusive with offset; the backend defaults to 0 when both are omitted."),
  size: z.number().int().positive().max(200).default(50).optional().describe("Page size (default: 50, max: 200)"),
  response_format: ResponseFormat.optional(),
}).strict().refine(
  (data) => data.page === undefined || data.offset === undefined,
  { message: "page and offset are mutually exclusive" },
);

export const listDeployRepositoriesSchema = z.object({
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

// =============================================================================
// Local Changes & Restore Schemas
// =============================================================================

// Studio can default an omitted module to the first descriptor entry, but local history
// belongs to a specific edited module, so MCP requires an explicit, stable selection.
export const listProjectLocalChangesSchema = z.object({
  projectId: projectIdSchema,
  moduleName: z.string().trim().min(1).describe("Module name exactly as returned by openl_list_project_modules()."),
  response_format: ResponseFormat.optional(),
}).strict();

export const restoreProjectLocalChangeSchema = z.object({
  projectId: projectIdSchema,
  moduleName: z.string().trim().min(1).describe("Module name used with openl_list_project_local_changes()."),
  historyId: z.string().trim().min(1).describe("Local history entry ID to restore, from openl_list_project_local_changes()."),
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Test Execution Schemas
// =============================================================================

export const startProjectTestsSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema.optional().describe("Table ID to run tests for a specific table. Table type can be test table or any other table. If not provided, tests for all test tables in the project will be run."),
  testRanges: z.string().optional().describe("Test ranges to run. Can be provided only if tableId is Test table. Example: '1-3,5' to run tests with numbers 1,2,3 and 5. If not provided, all tests in the test table will be run."),
  fromModule: z.string().optional().describe("Module name to run tests from."),
  response_format: ResponseFormat.optional(),
}).strict();

export const getTestResultsSummarySchema = z.object({
  projectId: projectIdSchema,
  failuresOnly: z.boolean().optional().describe("Include only failed tests."),
  failures: z.number().int().positive().default(5).optional().describe("Number of failed test units to include in the summary (default: 5, min: 1)"),
  unpaged: z.boolean().default(false).optional().describe("Return all results without pagination"),
  response_format: ResponseFormat.optional(),
}).strict();

export const getTestResultsSchema = z.object({
  projectId: projectIdSchema,
  failuresOnly: z.boolean().optional().describe("Show only failed tests (default: false)"),
  failures: z.number().int().positive().default(5).optional().describe("Number of failed test units to include in the summary (default: 5, min: 1)"),
  page: z.number().int().nonnegative().optional().describe("Page number (0-based). Mutually exclusive with offset"),
  offset: z.number().int().nonnegative().optional().describe("Offset for pagination. Mutually exclusive with page"),
  size: z.number().int().positive().max(200).optional().describe("Page size (number of results per page, maximum 200)"),
  limit: z.number().int().positive().max(200).optional().describe("Page size (alias for size, maps to size parameter)"),
  unpaged: z.boolean().default(false).optional().describe("Return all results without pagination. Mutually exclusive with page, offset, size, and limit"),
  response_format: ResponseFormat.optional(),
}).strict().refine(
  (data) => {
    // Validate mutual exclusivity: page vs offset
    if (data.page !== undefined && data.offset !== undefined) {
      return false;
    }
    // Validate mutual exclusivity: unpaged vs page/offset/size/limit
    if (data.unpaged === true && (data.page !== undefined || data.offset !== undefined || data.size !== undefined || data.limit !== undefined)) {
      return false;
    }
    return true;
  },
  {
    message: "Invalid pagination parameters: page and offset are mutually exclusive; unpaged is mutually exclusive with page, offset, size, and limit",
  }
);

export const getTestResultsByTableSchema = z.object({
  projectId: projectIdSchema,
  tableId: tableIdSchema.describe("Table ID to filter test results for a specific table"),
  failuresOnly: z.boolean().optional().describe("Show only failed tests (default: false)"),
  failures: z.number().int().positive().default(5).optional().describe("Number of failed test units to include in the summary (default: 5, min: 1)"),
  page: z.number().int().nonnegative().optional().describe("Page number (0-based). Mutually exclusive with offset"),
  offset: z.number().int().nonnegative().optional().describe("Offset for pagination. Mutually exclusive with page"),
  size: z.number().int().positive().max(200).optional().describe("Page size (number of results per page, maximum 200)"),
  limit: z.number().int().positive().max(200).optional().describe("Page size (alias for size, maps to size parameter)"),
  unpaged: z.boolean().default(false).optional().describe("Return all results without pagination. Mutually exclusive with page, offset, size, and limit"),
  response_format: ResponseFormat.optional(),
}).strict().refine(
  (data) => {
    // Validate mutual exclusivity: page vs offset
    if (data.page !== undefined && data.offset !== undefined) {
      return false;
    }
    // Validate mutual exclusivity: unpaged vs page/offset/size/limit
    if (data.unpaged === true && (data.page !== undefined || data.offset !== undefined || data.size !== undefined || data.limit !== undefined)) {
      return false;
    }
    return true;
  },
  {
    message: "Invalid pagination parameters: page and offset are mutually exclusive; unpaged is mutually exclusive with page, offset, size, and limit",
  }
);

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

export const readProjectFileSchema = z.object({
  projectId: projectIdSchema,
  path: z
    .string()
    .default("")
    .describe(
      "Project-relative path to a file or folder (e.g. 'rules/Model.xlsx' or 'rules/'). Empty string (default) or a path ending in '/' lists the project root / that folder; a file path returns the file content."
    ),
  view: z
    .enum(["meta"])
    .optional()
    .describe(
      "For a file, set to 'meta' to return JSON metadata (name, size, extension, lastModified) instead of the file content. Omit to read content (files) or list entries (folders)."
    ),
  download: z
    .boolean()
    .optional()
    .describe(
      "For a folder, set true to download the folder and its contents as a ZIP archive (base64 content in a JSON text envelope). Ignored for files."
    ),
  recursive: z
    .boolean()
    .optional()
    .describe("Folder listing only: include nested resources recursively (default false)."),
  viewMode: z
    .enum(["FLAT", "NESTED"])
    .optional()
    .describe("Folder listing only: FLAT returns a flat list, NESTED returns a tree (default FLAT)."),
  extensions: z
    .array(z.string())
    .optional()
    .describe("Folder listing only: filter by file extensions without the dot, e.g. ['xlsx','xml']."),
  namePattern: z
    .string()
    .optional()
    .describe("Folder listing only: filter by name (case-insensitive contains match)."),
  foldersOnly: z
    .boolean()
    .optional()
    .describe("Folder listing only: if true, return only folders (default false)."),
  version: z
    .string()
    .optional()
    .describe(
      "Historical revision (commit hash) to read. Omit to read the latest revision. Applies to file content/metadata and folder listing/ZIP. An unknown revision yields 404."
    ),
  branch: fileBranchSchema,
  fields: z
    .string()
    .optional()
    .describe(
      "Comma-separated response fields to return for metadata/listing responses, including nested selection (e.g. 'id,name'). When omitted, the full response is returned."
    ),
  encoding: z
    .enum(["auto", "utf-8", "base64"])
    .default("auto")
    .describe(
      "How to return file content. 'auto' (default) returns text as UTF-8 and binary as base64 content in a JSON text envelope; 'utf-8' forces text; 'base64' forces the base64 envelope. Ignored for metadata/listing responses."
    ),
  offset: z
    .number()
    .int()
    .nonnegative()
    .optional()
    .describe(
      "Byte offset to start reading file content from (default 0). NOTE: the backend does not support partial transfers, so the whole file is fetched and then sliced client-side. offset/length are BYTE offsets — a range boundary that lands inside a multi-byte UTF-8 character makes that character decode to U+FFFD (�) at the seam; for exact bytes use encoding='base64'."
    ),
  length: z
    .number()
    .int()
    .positive()
    .optional()
    .describe("Maximum number of bytes of file content to return starting at 'offset'. Omit for the rest of the file. Byte count, not character count (see the note on 'offset')."),
  response_format: ResponseFormat.optional(),
}).strict();

const writeProjectFileBaseSchema = z.object({
  projectId: projectIdSchema,
  path: filePathSchema,
  createFolders: z
    .boolean()
    .default(true)
    .describe("If true (default), missing intermediate folders are created automatically; otherwise the parent folder must already exist."),
  conflictPolicy: z
    .enum(["FAIL", "OVERWRITE", "SKIP"])
    .optional()
    .describe("How to handle a target file that already exists: FAIL (default) returns an error; OVERWRITE replaces its content in place; SKIP leaves the existing file unchanged and reports it skipped. Has no effect when creating a new file."),
  message: z
    .string()
    .optional()
    .describe("Optional commit message. PRESENT → the write is committed to Git after saving the project (a new revision is created). ABSENT → the write stays in the project WORKING COPY (commit it later with openl_save_project). NOTE: committing saves ALL pending project changes (OpenL has no per-file commit), and only works for design (Git) repositories — not 'local'."),
  branch: fileBranchSchema,
  response_format: ResponseFormat.optional(),
});

const writeUtf8ProjectFileSchema = writeProjectFileBaseSchema.extend({
  content: z
    .string()
    .describe("UTF-8 text content."),
  encoding: z
    .literal("utf-8")
    .optional()
    .describe("Optional explicit UTF-8 encoding; omitted means UTF-8."),
}).strict();

const writeLegacyBase64ProjectFileSchema = writeProjectFileBaseSchema.extend({
  content: z
    .string()
    .refine(isValidBase64, "Content is not valid base64.")
    .describe("Legacy base64 binary content. Whitespace and line wrapping are accepted."),
  encoding: z
    .literal("base64")
    .describe("Marks the legacy content parameter as base64."),
}).strict();

const writeBlobProjectFileSchema = writeProjectFileBaseSchema.extend({
  blob: z
    .base64()
    .meta({ contentMediaType: "application/octet-stream" })
    .describe("Binary file bytes encoded as base64. Uses JSON Schema 2020-12 contentEncoding='base64'."),
}).strict();

/**
 * Exactly one content representation is accepted. Keeping this as a Zod union
 * makes the same mutually exclusive alternatives visible in the generated MCP JSON Schema;
 * superRefine alone would enforce it only at runtime.
 */
export const writeProjectFileSchema = z.union([
  writeUtf8ProjectFileSchema,
  writeLegacyBase64ProjectFileSchema,
  writeBlobProjectFileSchema,
]);

export const deleteProjectFileSchema = z.object({
  projectId: projectIdSchema,
  path: filePathSchema,
  branch: fileBranchSchema,
  response_format: ResponseFormat.optional(),
}).strict();

export const searchProjectFilesSchema = z.object({
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
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

// Intentionally has no `response_format` or pagination: the tool returns a single
// aggregated markdown document (one AGENTS.md chain), which has no alternate format
// or pages — do not add them to match the other tools.
export const getProjectAgentContextSchema = z.object({
  projectId: projectIdSchema,
  folder: z
    .string()
    .max(1024)
    .optional()
    .describe(
      "Optional project-relative sub-folder to start the walk-up from, e.g. 'rules' or 'rules/pricing'. Use this to get the AGENTS.md chain that applies to a file deeper inside the project ('the AGENTS.md nearest the edited file wins'). Omit to start at the project root. Do NOT include the project name; the path is relative to the project root."
    ),
  branch: fileBranchSchema,
}).strict();

// =============================================================================
// Diagnostics Schemas
// =============================================================================

// Only the output format is negotiable: the payload is the server's own version,
// build, and runtime identity, which take no input.
export const getVersionSchema = z.object({
  response_format: ResponseFormat.optional(),
}).strict();

// =============================================================================
// Guidance Schemas
// =============================================================================

// Intentionally argument-less: the onboarding text is fixed per build and the
// orientation is computed from the bundled guides index.
export const getStartedSchema = z.object({}).strict();

export const listGuidesSchema = z.object({
  type: z
    .enum(["specification", "guide"])
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
  response_format: ResponseFormat.optional(),
}).merge(PaginationParams).strict();

// Like getProjectAgentContextSchema, intentionally no `response_format`: the tool
// returns the requested markdown documents verbatim.
export const getGuidesSchema = z.object({
  ids: z
    .array(z.string().min(1))
    .min(1)
    .max(5)
    .describe(
      "1-5 guide ids exactly as returned by openl_list_guides (e.g. 'spec/rules.xml', 'guide/introduction/basic-concepts'). Each entry's size_bytes is in the index — fetch only what you need."
    ),
}).strict();

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
  response_format: ResponseFormat.optional(),
};

export const copyProjectFileSchema = z.object(copyMovePairSchema).strict();

export const moveProjectFileSchema = z.object(copyMovePairSchema).strict();

// =============================================================================
// Redeploy Schema
// =============================================================================

export const redeployProjectSchema = z.object({
  deploymentId: z.string().describe("Deployment ID to redeploy (from list_deployments response)"),
  projectId: projectIdSchema,
  comment: commentSchema,
  response_format: ResponseFormat.optional(),
}).strict();
