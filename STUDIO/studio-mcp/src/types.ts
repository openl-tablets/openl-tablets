/**
 * TypeScript types for OpenL Studio REST API
 */

export interface OpenLConfig {
  baseUrl: string;
  /**
   * The `Authorization` header the calls to OpenL Studio carry, such as `Token openl_pat_…`: the one of the request to
   * the MCP server, which OpenL Studio has checked. Calls go without credentials when it is missing.
   */
  authorization?: string;
  /**
   * The address of OpenL Studio its users open, ending with a slash, as the request to the MCP server tells it.
   * Defaults to the address the client calls.
   */
  publicUrl?: string;
  /**
   * The address of the client of the MCP server, as OpenL Studio names it in the request to the server. The calls name
   * it back, with `mcpSecret`, so that OpenL Studio records what the client does with the address of the client.
   */
  clientAddress?: string;
  /** The secret OpenL Studio gave this run of the MCP server, which tells its calls from any other request. */
  mcpSecret?: string;
  /** The MCP client the calls are made for, as it named itself when it opened its session. */
  mcpClient?: McpClientInfo;
}

/** The name and the version an MCP client reports of itself (`clientInfo`). */
export interface McpClientInfo {
  name: string;
  version?: string;
}

/** The build of OpenL Studio, as `/public/info/build.json` tells it. */
export interface StudioBuildInfo {
  "openl.version"?: string;
  "openl.build.number"?: string;
  "openl.start.time"?: string;
}

/** The machine and the JVM OpenL Studio runs on, as `/public/info/sys.json` tells them. */
export interface StudioSysInfo {
  "time.now"?: string;
  "os.name"?: string;
  "os.version"?: string;
  "os.arch"?: string;
  "vm.name"?: string;
  "vm.version"?: string;
  "vm.vendor"?: string;
  cpu?: number;
  /** The load of the CPUs of the machine, from 0 to 1; negative or absent when the JVM cannot tell it. */
  "os.SystemCpuLoad"?: number | null;
  "thread.count"?: number;
  "thread.daemon"?: number;
  "os.TotalPhysicalMemorySize"?: number | null;
  "os.FreePhysicalMemorySize"?: number | null;
  maxMemory?: number;
  totalMemory?: number;
  freeMemory?: number;
}

/** The kinds of the documents OpenL Studio serves at `/docs`: the specifications of `docs/ref`, the Reference Guide. */
export const GUIDE_TYPES = ["specification", "guide"] as const;

/** The formats a tool result is rendered in. */
export const RESPONSE_FORMATS = ["json", "markdown", "markdown_concise", "markdown_detailed"] as const;

export type ResponseFormat = (typeof RESPONSE_FORMATS)[number];

/** The statuses of a project in Studio. */
export const PROJECT_STATUSES = ["LOCAL", "DELETED", "OPENED", "VIEWING_VERSION", "EDITING", "CLOSED"] as const;

export type ProjectStatus = (typeof PROJECT_STATUSES)[number];

/** The expansions a read of projects may ask Studio for. */
export const PROJECT_INCLUDES = ["summary", "status", "deleted", "descriptor"] as const;

export interface ProjectViewModel {
  name: string;
  modifiedBy: string;
  modifiedAt: string;
  revision: string;
  branch?: string;
  branchDefault?: boolean;
  branchProtected?: boolean;
  compileStatus?: ProjectStatusView;
  path?: string;
  id: string;
  status?: ProjectStatus;
  tags?: Record<string, string>;
  comment?: string;
  repository?: string;
  repositoryInfo?: { name: string; features?: RepositoryFeatures };
  /** What the user may do with the project: an operation left out is not available. */
  capabilities?: ProjectCapabilities;
}

/** The lifecycle operations a project takes; the ones left out do not apply to it. */
export interface ProjectCapabilities {
  canOpen?: boolean;
  canSave?: boolean;
  canClose?: boolean;
}

export interface ModuleViewModel {
  modules?: ModuleViewModel[];
  name?: string;
  path?: string;
}

/**
 * Compact create/copy response from the repository project endpoints. The
 * revision is the Git commit SHA of the atomic project changeset.
 */
export interface CreateProjectResult {
  /** The id of the new project, the one the project list shows. */
  id: string;
  revision: string;
  /** Present only for repositories that support branches. */
  branch?: string;
}

/**
 * Request body of a file copy or move in a project (`POST /projects/{id}/file-copy`, `/file-move`); paths are
 * project-relative.
 */
export interface FilePathPairRequest {
  sourcePath: string;
  destinationPath: string;
}

/**
 * A file or folder node returned by the Projects: Files (BETA) API
 * (folder listings, `view=meta`, and file-search). The base contract is the
 * path/name/type/basePath quartet; the backend additionally returns `extension`,
 * `size` and `lastModified` for files, so they are modelled here as optional.
 */
/** The metadata of a file, with the version of its content that a replacement or a deletion of it names back. */
export type ProjectFileMeta = FsNode & { version?: string };

export interface FsNode {
  /** Project-relative path (e.g. 'folder/rules.xlsx'). */
  path: string;
  /** Simple file or folder name. */
  name: string;
  /** Resource type. */
  type: "file" | "folder";
  /**
   * Parent directory path, when provided. Project-relative for SUBTREE-scope
   * results; repository-relative for ANCESTORS-scope results (which cross the
   * project boundary).
   */
  basePath?: string;
  /** File extension without the dot (files only). */
  extension?: string;
  /** Size in bytes (files only). */
  size?: number;
  /** ISO-8601 last-modified timestamp (files only). */
  lastModified?: string;
  /**
   * Raw file content. The backend populates this for ANCESTORS-scope file-search
   * (which returns each matched file together with its content); it is absent for
   * SUBTREE listings/searches, which return metadata only.
   */
  content?: string;
}

/**
 * One AGENTS.md file in a project's resolved ancestry chain, as returned by
 * {@link OpenLClient.getProjectAgentContext} (surfaced by the
 * `openl_get_project_agent_context` tool). The chain follows the AGENTS.md spec:
 * starting at the project (or a
 * sub-folder of it) and walking up to the repository root, the nearest file wins.
 * Proximity is carried by array order (nearest-first); the presentation layer
 * renders the files into a single document with that precedence applied.
 */
export interface AgentsFile {
  /**
   * Path relative to the REPOSITORY root (not the project), e.g.
   * 'monorepo/Project-1/AGENTS.md'. ANCESTORS search crosses the project boundary,
   * so paths are repo-relative to disambiguate files at different levels.
   */
  path: string;
  /** Raw markdown content of the file. */
  content: string;
}

/**
 * Search query body for POST /projects/{projectId}/file-search (FileSearchQuery).
 * All fields are optional; an empty body matches everything in scope.
 */
export interface FileSearchQuery {
  /** Ant-glob path pattern, e.g. all xlsx under rules. */
  pattern?: string;
  /** Case-insensitive content substring. Studio reads each file as text, so a workbook cell is never found. */
  content?: string;
  /** Filter by file extensions (without the dot). */
  extensions?: string[];
  /** Restrict to files, folders, or both. */
  type?: "FILE" | "FOLDER" | "ANY";
  /** SUBTREE (default) searches within the project; ANCESTORS walks up to the repo root. */
  scope?: "SUBTREE" | "ANCESTORS";
  /** Whether to descend into nested folders. */
  recursive?: boolean;
  /** Project-relative path to start the search from. */
  from?: string;
  /** Historical revision to search (SUBTREE scope only). */
  version?: string;
}

/**
 * The bytes of a file OpenL Studio answered — a project file or a version of a conflicted one — with their type, by
 * which the caller tells text from anything else.
 */
export interface ProjectFileResponse {
  /** Raw response body. */
  data: Buffer;
  /** Response Content-Type header (lower-cased), if any. */
  contentType: string;
}

/**
 * An entry of the table of contents of the user guides OpenL Studio serves at `/docs/toc.json`: a page, or a folder
 * with the entries inside it.
 */
export interface UserGuideEntry {
  title: string;
  /** The page, relative to `/docs`; a folder without an `index.md` has none. */
  file?: string;
  /** The size of the page in bytes. */
  size?: number;
  children?: UserGuideEntry[];
}

/** The kinds of OpenL tables Studio tells apart. */
export const TABLE_KINDS = [
  "Rules",
  "Spreadsheet",
  "Datatype",
  "Data",
  "Test",
  "TBasic",
  "Column Match",
  "Method",
  "Run",
  "Constants",
  "Conditions",
  "Actions",
  "Returns",
  "Environment",
  "Properties",
  "Other",
] as const;

type TableKind = (typeof TABLE_KINDS)[number];

/** A table as the table list of Studio sums it up. */
export interface TableMetadata {
  id: string;
  /** Backend summary contract leaves this open for non-editable/custom table types. */
  tableType?: string;
  kind?: TableKind;
  name?: string;
  properties?: Record<string, unknown>;
  /** The dimension properties that tell this version of a table from its others, when it has others. */
  dimensions?: Record<string, unknown>;
  returnType?: string;
  signature?: string;
  file?: string;
  pos?: string;
  /** The module the table is written in. */
  module?: string;
  /** The name and the id of the project of the table, the one the list was asked for. */
  project?: string;
  projectId?: string;
}

/** Fields shared by executable and datatype nodes in the Studio dependency graph. */
interface BaseTableNodeView {
  dependencies?: string[];
  dependents?: string[];
  file?: string;
  id?: string;
  kind?: TableKind | "Dispatcher";
  name?: string;
  pos?: string;
  project?: string;
  properties?: Record<string, unknown>;
  tableType?: string;
}

/** A callable-table node in the Studio dependency graph. */
interface ExecutableNodeView extends BaseTableNodeView {
  kind?: Exclude<TableKind, "Datatype"> | "Dispatcher";
  /** Values of the dimension properties that tell this version from the others; dates in ISO 8601. */
  dimensions?: Record<string, unknown>;
  returnType?: string;
  signature?: string;
}

/** One field declared directly by a datatype dependency node. */
export interface DatatypeNodeFieldView {
  collection?: boolean;
  name?: string;
  ref?: string;
  type?: string;
}

/** A bounded preview of the values declared by a vocabulary table. */
export interface DatatypeNodeVocabularyView {
  truncated?: boolean;
  valueCount?: number;
  valuesPreview?: unknown[];
  valueType?: string;
}

/** A datatype or vocabulary node in the Studio dependency graph. */
export interface DatatypeNodeView extends BaseTableNodeView {
  kind?: "Datatype";
  extends?: string;
  fields?: DatatypeNodeFieldView[];
  vocabulary?: DatatypeNodeVocabularyView;
}

/** One OpenAPI-discriminated node in the Studio table dependency graph. */
export type TableNodeView = ExecutableNodeView | DatatypeNodeView;

/** One allowed value of an enum-backed table property. */
interface PropertyValueView {
  code?: string;
  value?: string;
}

/** A property that a table (or a Properties table entry) may declare. */
export interface PropertyDefinitionView {
  multiple?: boolean;
  name?: string;
  displayName?: string;
  group?: string;
  /** Whether the property selects a version of a table, such as a state or an effective date. */
  dimensional?: boolean;
  defaultValue?: string | null;
  type?: "text" | "date" | "boolean" | "enum";
  values?: PropertyValueView[];
}

/** Replacement property sent when copying a table. */
interface TableProperty {
  name: string;
  value?: string;
}

/** Request body for POST /projects/{projectId}/tables/{tableId}/copy. */
export interface CopyTableRequest {
  moduleName: string;
  modulePath?: string;
  name: string;
  properties?: TableProperty[];
  sheetName?: string;
}

/** Successful 202 response while an asynchronous Studio result is still being produced. */
export interface ResultNotReadyView {
  status: "notReady";
}

/** Result of a regular table execution through the Studio Run API. */
export interface RunExecutionResult {
  contextParameters?: TraceParameterValue[];
  errors?: MessageDescription[];
  executionTimeMs?: number;
  parameters?: TraceParameterValue[];
  result?: unknown;
  resultSchema?: Record<string, unknown>;
  tableId?: string;
  tableName?: string;
}

/** Branch metadata returned by the project branch-listing endpoint. */
export interface ProjectBranchInfo {
  base?: boolean;
  name?: string;
  protected?: boolean;
}

/** Query scopes accepted by GET /projects/{projectId}/branches. */
export const BRANCH_SCOPES = ["project", "repository"] as const;

export type BranchScope = (typeof BRANCH_SCOPES)[number];

/** The directions of a merge: the other branch into the current one, or the current one into the other. */
export const MERGE_MODES = ["receive", "send"] as const;

export type MergeMode = (typeof MERGE_MODES)[number];

export interface MergeRequest {
  mode: MergeMode;
  otherBranch: string;
}

export interface CheckMergeResult {
  blockedBy?: "bypass-required" | "protected-branch" | "locked";
  /** Whether Studio permits a merge attempt after permission, protection, and lock checks. */
  canMerge?: boolean;
  sourceBranch: string;
  /** `mergeable` means the source has pending changes; it does not predict a conflict-free merge. */
  status: "mergeable" | "up-to-date";
  targetBranch: string;
  /** What the merge would bring into the target branch: every project the source changed, and the files both changed. */
  scope?: unknown;
}

interface ConflictGroup {
  files?: string[];
  projectName?: string;
  projectPath?: string;
}

export interface MergeResultResponse {
  conflictGroups?: ConflictGroup[];
  status?: "success" | "conflicts" | "cancelled";
  /** What a merge that succeeded changed in the target branch: every project it changed, and their files. */
  summary?: unknown;
}

interface RevisionDetails {
  author?: string;
  branch?: string;
  commit?: string;
  exists?: boolean;
  modifiedAt?: string;
}

/** Which versions hold a conflicted file, and whether it is UTF-8 text. */
interface ConflictFileAvailability {
  ours: boolean;
  base: boolean;
  theirs: boolean;
  text: boolean;
}

export interface ConflictDetailsResponse {
  baseRevision?: RevisionDetails;
  conflictGroups?: ConflictGroup[];
  defaultMessage?: string;
  /** Which versions hold each conflicted file, and whether it is UTF-8 text, by the path of the file. */
  fileAvailability?: Record<string, ConflictFileAvailability>;
  oursRevision?: RevisionDetails;
  theirsRevision?: RevisionDetails;
}

/**
 * A part of a conflicted text file, as `GET /merge/conflicts/text` answers it: lines the merge took by itself, or a
 * conflict. Every text keeps its line breaks.
 */
export type ConflictTextBlock =
  | { kind: "merged"; text: string; ours?: string; base?: string; theirs?: string }
  | { kind: "conflict"; ours: string; base: string; theirs: string };

/** A conflicted text file merged line by line: its parts in the order of the file. */
export interface ConflictTextView {
  blocks: ConflictTextBlock[];
}

/**
 * How one conflict of a text file is resolved: by the lines of a version, by both versions in an order, or by lines
 * written in its place.
 */
export type ConflictTextDecision =
  | { side: "ours" | "theirs" | "base" | "ours-theirs" | "theirs-ours" }
  | { lines: string };

/** A cell of a table of a conflicted workbook, as `GET /merge/conflicts/workbook` sends it. */
export interface ConflictWorkbookCell extends Omit<RawTableCell, "value" | "style"> {
  value?: unknown;
  formula?: string;
  comment?: string;
  /** The style of the cell, by its index in the styles of the table. */
  style?: number;
  runs?: unknown[];
  type?: string;
}

/** A table as one version of a conflicted workbook has it. */
export interface ConflictTableVersion {
  range: string;
  cells: ConflictWorkbookCell[][];
  styles?: unknown[];
  rows: number;
  changedCells?: string[];
  formattingChanged?: boolean;
  conflictCells?: string[];
}

/** A cell of a merged table both branches changed differently, with what it is with either version. */
export interface ConflictCellView {
  row: number;
  column: number;
  cell: string;
  /** The cell in your version, which differs from the merged one when a branch inserted rows or columns before it. */
  ourCell: string;
  /** The cell in their version, which differs from the merged one when a branch inserted rows or columns before it. */
  theirCell: string;
  ours: ConflictWorkbookCell;
  theirs: ConflictWorkbookCell;
}

/** A conflicting table merged cell by cell: the conflicting cells hold the base version until they are decided. */
export interface ConflictTableMerge {
  range: string;
  cells: ConflictWorkbookCell[][];
  styles?: unknown[];
  rows: number;
  /** The cells one branch alone changed, which the merge takes from it by itself. */
  changedCells?: string[];
  conflicts: ConflictCellView[];
}

export interface ConflictTableView {
  id: string;
  name: string;
  reason: string;
  ours?: ConflictTableVersion;
  base?: ConflictTableVersion;
  theirs?: ConflictTableVersion;
  merged?: ConflictTableMerge;
}

export interface ConflictSheetView {
  name: string;
  status: "ours" | "theirs" | "merged" | "conflict";
  reason?: string;
  ours: boolean;
  base: boolean;
  theirs: boolean;
  tables: ConflictTableView[];
}

/** The conflicts of a conflicted workbook, as `GET /merge/conflicts/workbook` answers them. */
export interface ConflictWorkbookView {
  sheets: ConflictSheetView[];
  palette: boolean;
}

type ConflictCellSide = "ours" | "theirs";

/**
 * How one conflict of a workbook is resolved, as `POST /merge/conflicts/workbook` takes it: taken whole from a
 * version, or a table merged cell by cell.
 */
export type ConflictWorkbookDecision =
  | { id: string; side: "ours" | "theirs" | "base" }
  | {
    id: string;
    cells: {
      side?: ConflictCellSide;
      cells?: { row: number; column: number; side?: ConflictCellSide; value?: string }[];
    };
  };

/**
 * How one conflicted file is resolved by `POST /merge/conflicts/resolve`, with the merged file for `CUSTOM`: the bytes
 * of a workbook, or a text, which is sent in UTF-8.
 */
export interface FileConflictResolution {
  file: string;
  strategy: "BASE" | "OURS" | "THEIRS" | "CUSTOM";
  content?: ArrayBuffer | string;
}

export interface ResolveConflictsResponse {
  status: "success";
  resolvedFiles: string[];
  summary?: unknown;
}

export interface Repository {
  /** The id of the repository in Studio's access control, which no tool takes. */
  aclId?: string;
  /** Repository ID */
  id: string;
  /** Repository name */
  name: string;
  features?: RepositoryFeatures;
  type?: string;
}

/** Repository features (from OpenAPI) */
export interface RepositoryFeatures {
  branches?: boolean;
  /** Whether project changes can carry a comment. */
  comments?: boolean;
  mappedFolders?: boolean;
  searchable?: boolean;
}

/** Project revision from GET /projects/{projectId}/history. */
export interface ProjectRevision {
  revisionNo: string;
  shortRevisionNo?: string;
  createdAt: string;
  fullComment: string;
  author?: {
    displayName?: string;
    email?: string;
  };
  deleted: boolean;
  technicalRevision: boolean;
  commentParts?: string[];
}

/** One module working-copy snapshot from GET /projects/{projectId}/local-history. */
export interface ProjectHistoryItem {
  current?: boolean;
  id?: string;
  /** Moment the version was kept at, in ISO 8601; absent for the revision the module was opened at. */
  modifiedOn?: string;
}

/** An element of a comparison of two workbooks: a sheet with its tables, or a table, and whether it differs. */
export interface ComparisonNodeView {
  id?: string;
  name?: string;
  type?: "sheet" | "table";
  status?: "equal" | "changed" | "added" | "removed";
  children?: ComparisonNodeView[];
}

/** The result of comparing two workbooks, sheet by sheet. */
export interface ComparisonView {
  id: string;
  identical: boolean;
  sheets: ComparisonNodeView[];
}

/** Deployment view model (short version from OpenAPI 3.0.1) */
export interface DeploymentViewModelShort {
  id: string;
  name: string;
  repository: string;
  items?: DeploymentItemViewModelShort[];
}

interface DeploymentItemViewModelShort {
  modifiedAt: string;
  modifiedBy: string;
  name: string;
  /** The design revision the project was deployed from, when Studio has linked it. */
  designRevision?: {
    revision: string;
    modifiedBy?: string;
    modifiedAt?: string;
  };
}

/** Deploy project request (OpenAPI 3.0.1) */
export interface DeployProjectRequest {
  projectId: string;              // Project ID from backend
  deploymentName: string;         // Name for the deployment
  productionRepositoryId: string;
  comment?: string;
}

/** Redeploy project request (OpenAPI 3.0.1) */
export interface RedeployProjectRequest {
  projectId: string;              // Project ID from backend
  comment?: string;
}

/** Branch create request (OpenAPI 3.0.1) */
export interface BranchCreateRequest {
  branch: string;   // Branch name (required)
  revision?: string;    // Revision to branch from (optional)
}

/** Project status update model (request body for PATCH /projects/{id}) */
export interface ProjectStatusUpdateModel {
  /** Only OPENED and CLOSED can be set by the client; other states are backend-managed. */
  status?: "OPENED" | "CLOSED";
  branch?: string;
  revision?: string;
  comment?: string;
  save?: boolean;
  discardChanges?: boolean;
  openDependencies?: boolean;
}

/**
 * One side of a cell border: line style and width. Mirrors
 * `RawTableCellBorderSide` in the studio OpenAPI.
 */
interface RawTableCellBorderSide {
  style?: "solid" | "dashed" | "dotted" | "double";
  width?: number;
}

/**
 * Cell borders per side; a side is absent when the cell has no border there.
 * Mirrors `RawTableCellBorder` in the studio OpenAPI.
 */
interface RawTableCellBorder {
  top?: RawTableCellBorderSide;
  right?: RawTableCellBorderSide;
  bottom?: RawTableCellBorderSide;
  left?: RawTableCellBorderSide;
}

/**
 * Excel cell style carried by a raw cell when `styles=true` is requested.
 * Every field is absent for its default (white background, black left-aligned
 * regular font, no borders). Mirrors `RawTableCellStyle` in the studio OpenAPI.
 */
export interface RawTableCellStyle {
  /** Background colour as `#rrggbb`; absent when white (the default). */
  background?: string;
  /** Font colour as `#rrggbb`; absent when black (the default). */
  color?: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  /** Horizontal alignment; absent for the default left alignment. */
  align?: "right" | "center" | "justify";
  /** Vertical alignment; absent for the default bottom alignment. */
  valign?: "center" | "top";
  /** Left indent in Excel indent units; absent when zero. */
  indent?: number;
  border?: RawTableCellBorder;
}

/** A scalar value accepted in a raw table cell or multi-value array. */
type RawCellScalar = string | number | boolean | null;

/** A scalar cell value or Studio's one-dimensional multi-value representation. */
type RawCellValue = RawCellScalar | RawCellScalar[];

/**
 * A single cell in a raw table view's 2D source matrix.
 *
 * Mirrors `RawTableCell` in the studio OpenAPI. `cell` is the A1-notation
 * address (e.g. `B3`) and matches the cell address that compilation messages
 * reference — absent for covered cells.
 */
export interface RawTableCell {
  /** A1-notation cell address (e.g. `B3`). Read-only; absent for covered cells. */
  cell?: string;
  value?: RawCellValue;
  /** Number of columns this cell spans (>=2 when merging; absent otherwise). */
  colspan?: number;
  /** Number of rows this cell spans (>=2 when merging; absent otherwise). */
  rowspan?: number;
  /** True when this cell is masked by another cell's span. */
  covered?: boolean;
  /** The Excel formula of the cell (e.g. `=B2*C2`); a write stores it in place of the value. */
  formula?: string;
  /** The whole area of a merged cell the edge of a window cuts, in A1 notation (e.g. `B4:B299`). Read-only. */
  mergedArea?: string;
  /** Excel cell style; only present when the raw view was requested with `styles=true`. */
  style?: RawTableCellStyle;
  /** The Excel note of the cell. Read-only: a write keeps it. */
  comment?: string;
}

/**
 * Options for the table's raw source view: read the
 * source matrix in row slices and/or with per-cell Excel styles. Mirrors the
 * `offset`/`limit`/`styles` arguments, Studio's `startRow`/`maxRows`/`styles` query parameters of `GET .../tables/{tableId}`.
 */
export interface RawTableViewOptions {
  /** Zero-based index of the first row to return; omit to start at the top. */
  offset?: number;
  /** Maximum number of rows to return, counted from `offset`; omit to read to the end. */
  limit?: number;
  /** When true, each cell carries its Excel style in `style`. */
  styles?: boolean;
}

/**
 * Raw 2D view of a table — the only table-content representation exposed by
 * the MCP server. Mirrors
 * `RawTableView` in the studio OpenAPI (`tableType: "RawSource"`).
 */
export interface RawTableView {
  id?: string;
  tableType: "RawSource";
  kind?: TableKind;
  name?: string;
  messages?: DetailedMessageDescription[];
  /** Position of the table within the source file (read-only). */
  pos?: string;
  /** Empty only for a slice whose `startRow` is past the last row. */
  source: RawTableCell[][];
  /** The number of header rows of the table (read-only). */
  headerHeight?: number;
  /** The layout of a Test table (read-only). */
  layout?: unknown;
  /**
   * The version of the table read (read-only). A write of the table names it, so that a write over a table changed
   * since the read — on disk or through another client — is refused rather than overwriting that change.
   */
  version?: string;
  /**
   * Total number of rows when the returned window omits rows (a `startRow`
   * offset or a `maxRows` cap); absent when the whole table is returned.
   */
  totalRows?: number;
}

/**
 * A cell to write via a raw table-source edit (POST .../tables/{id}/actions).
 * Mirrors `RawCellInput` in the studio OpenAPI — the input counterpart of
 * {@link RawTableCell} (no read-only `cell` address). A cell may set
 * colspan/rowspan to merge; `covered` marks a cell masked by another's span.
 */
export interface RawCellInput {
  value?: RawCellValue;
  /** The Excel formula of the cell (e.g. `=B2*C2`); Studio writes it in place of the value. */
  formula?: string;
  colspan?: number;
  rowspan?: number;
  covered?: boolean;
}

/**
 * The `target` of a {@link RawTableSourceAction}: a row, a column, a single
 * cell, or a rectangular range of cells, plus the operation's coordinates and
 * payload. `type` is the CASE-SENSITIVE discriminator the backend reads; the
 * other fields are populated per operation (e.g. `position` for row/column
 * insert/delete/update, `cells` for the new contents, `row`/`column` for a
 * single cell, `rowspan`/`colspan` for a merge).
 */
export interface RawTableActionTarget {
  type: "row" | "column" | "cell" | "cells" | "rows" | "columns" | "range";
  position?: number;
  /** A single row/column is `RawCellInput[]`; a block (rows/columns/range) is `RawCellInput[][]`. */
  cells?: RawCellInput[] | RawCellInput[][];
  row?: number;
  column?: number;
  value?: RawCellValue;
  /** The Excel formula of a single cell (e.g. `=B2*C2`); Studio writes it in place of the value. */
  formula?: string;
  rowspan?: number;
  colspan?: number;
  /** Number of rows/columns to delete in a `rows`/`columns` delete (>= 1; defaults to 1). */
  count?: number;
}

/**
 * A single in-place edit of a table's raw source (POST
 * .../tables/{id}/actions). `operation` is the CASE-SENSITIVE discriminator —
 * append, insert, delete, update, merge or unmerge — and `target` carries the
 * resource it acts on. The table is always handled in raw format regardless of
 * its type. Mirrors `RawTableSourceAction` in the studio OpenAPI.
 */
export interface RawTableSourceAction {
  operation: "append" | "insert" | "delete" | "update" | "merge" | "unmerge";
  target: RawTableActionTarget;
}

/** Filters for listing projects (OpenAPI 3.0.1) */
export interface ProjectFilters {
  /** Repository ID */
  repository?: string;
  /** Project status */
  status?: ProjectStatus;
  dependsOn?: string;
  name?: string;
  author?: string;
  branch?: string;
  sort?: "name" | "status" | "updated";
  include?: Array<(typeof PROJECT_INCLUDES)[number]>;
  /** Project tags, e.g. { "insurance.home": "value" }; the client prefixes each name with `tags.` */
  tags?: Record<string, string>;
  /** Pagination: item offset (0-based) */
  offset?: number;
  /** MCP page size, mapped to the backend's size parameter */
  limit?: number;
}

// =============================================================================
// Testing & Validation Types
// =============================================================================

/** Test unit execution result (from OpenAPI) */
export interface TestUnitExecutionResult {
  id?: string;
  description?: string;
  status?: "TR_EXCEPTION" | "TR_NEQ" | "TR_OK";
  executionTimeMs?: number;
  contextParameters?: TraceParameterValue[];
  parameters?: TraceParameterValue[];
  errors?: MessageDescription[];
  testAssertions?: TestAssertionExecutionResult[];
}

export interface TestAssertionExecutionResult {
  actualValue?: unknown;
  description?: string;
  /** The field the assertion compares, when another assertion of the test has the same description. */
  field?: string;
  expectedValue?: unknown;
  status?: "TR_EXCEPTION" | "TR_NEQ" | "TR_OK";
}

/** Test case execution result (from OpenAPI) */
export interface TestCaseExecutionResult {
  name: string;
  tableId: string;
  description?: string;
  executionTimeMs: number;
  numberOfTests: number;
  numberOfFailures: number;
  /** Left out by Studio for a test table without failures when only the failed tests are asked for. */
  testUnits?: TestUnitExecutionResult[];
  /** How many failed tests the list leaves out, when Studio lists only the first of them. */
  omittedFailures?: number;
}

/** Tests execution summary (from OpenAPI) */
export interface TestsExecutionSummary {
  testCases: TestCaseExecutionResult[];
  executionTimeMs: number;
  numberOfTests: number; // Total number of tests (all tests)
  numberOfFailures: number; // Number of failed tests
  pageNumber: number;
  pageSize: number;
  numberOfElements: number;
  /** The number of test tables of the run, over every page */
  total?: number;
}

/** Test execution start response */
export interface TestExecutionStartResponse {
  started: true;
  /** True when a closed project of a design repository was opened to run its tests */
  projectOpened?: true;
}

/** Test results summary (without testCases array) */
export interface TestResultsSummary {
  executionTimeMs: number;
  numberOfTests: number;
  numberOfFailures: number;
  numberOfPassed: number;
}

/** How wide a search of tables reaches: the module named, the project, or the projects it depends on as well. */
export const TABLE_SCOPES = ["module", "project", "all"] as const;

/** The orders a list of tables comes in: by name, or as the workbooks hold them. */
export const TABLE_SORTS = ["name", "position"] as const;

/** Filters for listing tables */
export interface TableFilters {
  /** Filter by table kinds, of {@link TABLE_KINDS} */
  kind?: string[];
  /** Filter by table name fragment */
  name?: string;
  /** Text any cell of a table contains */
  text?: string;
  /** Text the header line of a table contains: its keyword, return type, name and arguments */
  header?: string;
  /** The module whose tables to list */
  module?: string;
  /** How wide the search reaches, of {@link TABLE_SCOPES} */
  scope?: string;
  /** The order of the tables, of {@link TABLE_SORTS} */
  sort?: string;
  /** Filter by project properties (will be prefixed with 'properties.' in query string) */
  properties?: Record<string, string>;
  /** Pagination: item offset (0-based) */
  offset?: number;
  /** Pagination: page size, mapped to the backend's size parameter */
  limit?: number;
}

/** Save project result. API returns 204 No Content and does not provide commit hash, version, author, or timestamp. */
export interface SaveProjectResult {
  /** False for a project without unsaved changes */
  saved: boolean;
  /** True when closeAfterSave asked to close the project, which is closed then */
  closed?: true;
}

/** Create new project table request (BETA API) */
export interface CreateNewTableRequest {
  /** Name of the module where the table will be created (required) */
  moduleName: string;
  /** Project-relative path for a new module. Must end in .xlsx. */
  modulePath?: string;
  /** Name of the sheet where the table will be created (optional, uses table name if not provided) */
  sheetName?: string;
  /** Complete raw source structure; Studio requires a nonblank name on creation. */
  table: RawTableWrite & { name: string };
}

/** A whole table as Studio takes it to write: its matrix of cells, with the spans of the merged ones. */
export interface RawTableWrite {
  tableType: "RawSource";
  name?: string;
  source: RawCellInput[][];
}

/** Generic paginated response */
export interface PageResponse<T> {
  content: T[];
  numberOfElements: number;
  pageNumber: number;
  pageSize: number;
  total?: number; // Total number of items (can be null if unknown)
}

/**
 * A page of a list endpoint of OpenL Studio.
 *
 * Studio pages the list itself, so a page must never be sliced a second time. The total is absent when Studio does
 * not know it, so callers must not substitute the current page size for it.
 */
export interface CollectionPage<T> {
  items: T[];
  pageNumber: number;
  pageSize: number;
  total?: number;
  /** Non-pagination fields returned alongside a backend page. */
  metadata?: Record<string, unknown>;
}

// =============================================================================
// Project Status Types (post-compilation snapshot)
// =============================================================================

/**
 * Project compilation state.
 *
 * Mirrors the backend's
 * `org.openl.studio.projects.model.project.status.CompileState` enum, which
 * is serialized as lowercase via `@JsonProperty` on each constant.
 */
export type CompileState = "idle" | "compiling" | "ok" | "warnings" | "errors";

/** Source-location discriminator. Backend has `TableMessageSource` and `ModuleMessageSource` variants; intentionally typed loosely to avoid coupling. */
type MessageSource = Record<string, unknown>;

/**
 * Compilation message. The backend flattens `MessageDescription`
 * (`id`/`summary`/`severity`) onto this type via `@JsonUnwrapped`, so those
 * fields appear at the top level alongside `location` and `stacktrace`.
 */
interface DetailedMessageDescription {
  id?: number;
  summary?: string;
  severity?: "ERROR" | "WARN" | "INFO";
  location?: MessageSource;
  stacktrace?: boolean;
}

interface CompilationMessages {
  items: DetailedMessageDescription[];
  total: number;
  errors: number;
  warnings: number;
}

interface CompilationModules {
  total: number;
  compiled: number;
  compiledModules?: string[];
}

interface CompilationTests {
  total: number;
}

interface CompilationDetails {
  messages: CompilationMessages;
  modules: CompilationModules;
  tests: CompilationTests;
}

interface ProjectModifiedBy {
  author?: string;
  /** ISO-8601 timestamp serialized from `ZonedDateTime`. */
  date?: string;
}

/**
 * Wire-level change type as serialized by the studio. The Java
 * `org.openl.studio.projects.model.project.status.ChangeType` enum is
 * annotated with `@JsonProperty("added")` / `"modified"` / `"deleted"`, so
 * the values on the wire are lowercase — matching how `CompileState` is
 * serialized.
 */
type FileChangeType = "added" | "modified" | "deleted";

interface FileChange {
  /** `<projectRealPath>/<file>` (forward slashes), matching the merge API. */
  path: string;
  type: FileChangeType;
}

interface PendingChanges {
  total: number;
  files: FileChange[];
}

/**
 * Post-compilation project status: the `compileStatus` of `GET /projects/{id}?include=status`.
 *
 * Named `ProjectStatusView` to avoid collision with the existing
 * {@link ProjectStatus} string-enum that represents project lifecycle states
 * (OPENED / CLOSED / EDITING / …).
 */
export interface ProjectStatusView {
  projectId: string;
  /** Present only for repositories that support branches. */
  branch?: string;
  revision?: string;
  compileState: CompileState;
  lastModifiedBy?: ProjectModifiedBy;
  /** Omitted when no compilation has been registered yet (e.g. `compileState: "idle"`). */
  compilation?: CompilationDetails;
  /** Omitted when the working copy is clean. */
  pendingChanges?: PendingChanges;
  /**
   * Set when another program, such as Excel or an IDE, changed a file of the project since Studio compiled it:
   * the tables Studio answers with are what the files held before.
   */
  changedOnDisk?: boolean;
}

// =============================================================================
// Trace Debug API Types (BETA) — interactive debugger
// =============================================================================

/** Debug session lifecycle status. */
export type DebugStatus =
  | "pending"
  | "running"
  | "suspended"
  | "completed"
  | "error"
  | "terminated";

/** Kind of a traced frame / call-tree node ("stepRef" appears only on CallNodeView). */
type FrameKind =
  | "decisionTable"
  | "spreadsheet"
  | "method"
  | "cmatch"
  | "tbasic"
  | "tbasicMethod"
  | "stepRef";

/** Trace parameter value (input, context, result, or step value) — may be lazy-loaded. */
export interface TraceParameterValue {
  name: string;
  description: string;
  /** true → value omitted; fetch via openl_get_trace_value with parameterId, or the test case by its id. */
  lazy?: boolean;
  parameterId?: number;
  /** The type of a value a test result leaves out. */
  type?: string;
  /** The key of a value a test result leaves out, such as the id of a data table row. */
  key?: string;
  value?: unknown;
  schema?: object;
}

/** Error/warning message attached to a failed frame. */
interface MessageDescription {
  id?: number;
  severity: "INFO" | "WARN" | "ERROR";
  summary: string;
}

/** Dispatcher badge — the table was chosen from overloaded (dimension-property) versions. */
interface DispatchInfo {
  candidates: Array<{ label: string; chosen: boolean }>;
}

/** Current line inside a frame. */
interface DebugLocationView {
  kind: "cell" | "dtrule" | "operation";
  row?: number;
  column?: number;
  /** Short cell reference, e.g. "R2C3" — breakpoint sub-step key. */
  ref?: string;
  /** Human-readable, e.g. "$Formula$HouseTotal" or a fired rule name. */
  label?: string;
}

/** A sub-step of a frame (only executable cells are steps). */
interface StepValueView {
  ref: string;
  /** A1 source-cell address for spreadsheet steps. */
  cell?: string;
  label?: string;
  status: "executed" | "current" | "pending";
  /** True when this is static table content rather than an executable step. */
  constant?: boolean;
  /** Decision-table evaluation outcome for a breakdown row. */
  decision?: "matched" | "unmatched" | "returned";
  /** Frozen computed value (variables endpoint only). */
  value?: TraceParameterValue;
  /**
   * Tables this step called, in execution order — embedded only for an already
   * expanded branch (profiling). In the lazy call tree (the /stack `tree` root
   * and every openl_expand_trace_tree page) the children are omitted and only
   * `childrenTotal` is set; load them with openl_expand_trace_tree.
   */
  children?: CallNodeView[];
  /**
   * Total tables this step called, set when `children` is not embedded (the lazy
   * tree). > 0 means the step is expandable — pass the owning node's uri+instance
   * and this step's `ref` to openl_expand_trace_tree.
   */
  childrenTotal?: number;
  durationMillis?: number;
  selfMillis?: number;
}

/** A node of the executed call tree (profiling) — structure and timings only, no values. */
interface CallNodeView {
  uri: string;
  name: string;
  kind: FrameKind;
  /**
   * Zero-based execution index of this table in the run — combine with the
   * breakpoint key as `uri@N` to replay this exact iteration, and pass it as
   * `instance` to openl_expand_trace_tree to expand this node's steps.
   */
  instance?: number;
  durationMillis: number;
  selfMillis: number;
  steps: StepValueView[];
  dispatch?: DispatchInfo;
  /** For kind=stepRef: the ref of the original step this node points at. */
  refStep?: string;
  /**
   * Sub-calls this node made that ran but were dropped from the retained tree
   * once it hit its size limit — absent when every sub-call was kept. Surface as
   * "+N not retained (tree too large)".
   */
  notRetained?: number;
}

/** What a trace stack read adds to the stack of a profiling run: its root `tree` and the size of its `profile`. */
export interface TraceStackOptions {
  includeTree?: boolean;
  profileTop?: number;
}

/**
 * One page of a step's executed sub-calls — the lazy profiling call tree loaded
 * one level at a time (openl_expand_trace_tree). Each child is itself shallow:
 * its steps carry `childrenTotal`, not embedded `children`.
 */
export interface TreeChildrenView {
  /** This page of the step's sub-calls, starting at the requested offset; absent when it is empty. */
  children?: CallNodeView[];
  /** The step's full sub-call count — page again while it exceeds offset + children.length. */
  total: number;
}

/** One frame of the live execution stack. */
interface DebugFrameView {
  index: number;
  depth: number;
  /**
   * Zero-based execution number of this table (0 the first time it runs, 1 the
   * second, …) — the number a `uri#ref@N` breakpoint and a watch series use.
   */
  instance?: number;
  /** Table source URI — breakpoint + raw-table key. */
  uri: string;
  /** Table id for the shared Tables API (?raw=true). */
  tableId: string;
  name: string;
  kind: FrameKind;
  location?: DebugLocationView;
  active: boolean;
  completed: boolean;
  error: boolean;
  steps?: StepValueView[];
  durationMillis?: number;
  selfMillis?: number;
  dispatch?: DispatchInfo;
}

/** Structured error of a terminal ERROR session. */
interface DebugError {
  summary: string;
  table?: string;
  location?: string;
  type?: string;
  detail?: string;
}

/** One hot table in the profile — time aggregated across all its invocations. */
interface ProfileHotspotView {
  uri: string;
  name: string;
  kind: FrameKind;
  /** Own time across all calls (excludes called tables); the hotspots' selfMillis sum to wall-clock. */
  selfMillis: number;
  /** Inclusive time (own + called tables). */
  totalMillis: number;
  /** How many times this table was invoked. */
  count: number;
}

/**
 * Constant-size profile overview (unlike the unbounded `tree`): the top-N
 * slowest tables plus run-wide totals. Present after a profiling run completes.
 */
interface ProfileSummaryView {
  /** Top-N slowest tables by selfMillis, most-expensive first. */
  hotspots: ProfileHotspotView[];
  /** Distinct tables that executed (may exceed hotspots.length). */
  distinctTables: number;
  /** Total table invocations in the run (the size of the full tree). */
  nodeCount: number;
  /** Wall-clock of the whole run. */
  totalMillis: number;
  /** true when the retained call tree reached its node limit; the hotspots still count every call. */
  truncated: boolean;
}

/** The live execution stack — returned by start / step / stack reads. */
export interface DebugStackView {
  status: DebugStatus;
  /** Frames ordered root (index 0) → current; empty after completion. */
  frames: DebugFrameView[];
  error?: DebugError;
  /**
   * The executed call tree's ROOT node after completion (profiling; only when
   * includeTree). One level deep — each step carries `childrenTotal`, not nested
   * `children`; drill down with openl_expand_trace_tree.
   */
  tree?: CallNodeView;
  /** Bounded profile overview after completion (profiling) — prefer this over `tree`. */
  profile?: ProfileSummaryView;
}

/** One watched cell's value at a single execution of its table. */
interface WatchPointView {
  /** 0-based execution index of the watched cell's table across the run. */
  instance: number;
  label?: string;
  /** Breakpoint key uri#cellRef to reach this cell (replay + breakpoint). */
  ref?: string;
  /** Path from the root call to the owning frame (table names). */
  path?: string[];
  /** Serialized like any traced value — lazy (lazy: true + parameterId) when large. */
  value?: TraceParameterValue;
}

/** All values a watched cell took across the run, one series per cell. */
interface WatchSeriesView {
  name: string;
  table?: string;
  tableUri?: string;
  /** Captured values in execution order, capped by the server to the first several executions. */
  points: WatchPointView[];
  /** Full number of executions of the table — may exceed points.length when capped. */
  total?: number;
}

/** Watched-cell values collected across a whole profiling-style run. */
export interface WatchView {
  series: WatchSeriesView[];
  /** true when the server's capture cap was reached, so some late executions are missing. */
  truncated?: boolean;
}

/** Lightweight status poll response. */
export interface DebugStatusView {
  status: DebugStatus;
}

/** Which rule fired and how its conditions evaluated (decision tables). */
interface DecisionView {
  firedRules: string[];
  conditions: Array<{ condition: string; rule: string; matched: boolean }>;
}

/** Frozen variables of one suspended frame. */
export interface DebugFrameVariables {
  parameters: TraceParameterValue[];
  context?: TraceParameterValue;
  result?: TraceParameterValue;
  steps: StepValueView[];
  gridColumns?: string[];
  gridRows?: string[];
  decision?: DecisionView;
  ruleNames?: string[];
  errors: MessageDescription[];
}

/** Execution highlight for one cell, keyed by A1 address in the table's sheet. */
export interface CellHighlight {
  cell: string;
  state: "current" | "result" | "conditionTrue" | "conditionFalse";
}

/** A rule table a breakpoint can be set on (name is the breakpoint key). */
export interface BreakpointTableView {
  name: string;
  kind: FrameKind;
}

/** Start debug session request — tableId required; testRanges for test tables, inputJson for regular methods. */
export interface StartTraceRequest {
  projectId: string;
  tableId: string;
  testRanges?: string;
  module?: string;
  inputJson?: string | object;
  /** Suspend at the entry of the first frame (default true). */
  stopAtEntry?: boolean;
  /** Retain the executed call tree — structure and timings, no values (default false). */
  profiling?: boolean;
  /** Build value-rich business-view titles in the retained tree (default false). */
  detailedTitles?: boolean;
  /** Suspend on uncaught rule errors (backend default true). */
  breakOnErrors?: boolean;
  /** Include the `tree` root node (one level — steps carry `childrenTotal`) in the response; false returns only the bounded `profile`. Drill down with getTraceTreeChildren. */
  includeTree?: boolean;
  /** Number of hotspots in the profile overview (backend default 20). */
  profileTop?: number;
}

/** The public settings of OpenL Studio, `GET /settings`: a Studio in single-user mode names no `userMode`. */
export interface StudioSettings {
  userMode?: string;
}

/** What a transfer link is asked to move, `POST /transfer-links`. */
export interface TransferLinkRequest {
  kind: "download" | "archive" | "conflictVersion" | "upload";
  projectId: string;
  path: string;
  branch?: string;
  version?: string;
  side?: "base" | "ours" | "theirs";
  conflictPolicy?: "FAIL" | "OVERWRITE" | "SKIP";
  /** The version of the file an upload replaces: the link writes nothing when the file changed since. */
  expectedVersion?: string;
  createFolders?: boolean;
  size?: number;
  sha256?: string;
}

/** A transfer link Studio issued: its address relative to the address of Studio, and when it stops working. */
export interface TransferLinkView {
  path: string;
  expiresAt: string;
}
