/**
 * TypeScript types for OpenL Studio REST API
 */

/**
 * HTTP authentication scheme presented to the OpenL Studio REST API.
 *
 * `Token` is the OpenL Personal Access Token scheme and stays the default. A
 * Studio running in oauth2 mode also accepts `Bearer <IdP access token>` — the
 * scheme an OAuth-capable MCP client sends — and the two are not
 * interchangeable: a JWT presented as `Token` does not authenticate.
 */
export type OpenLAuthScheme = "Token" | "Bearer";

export interface OpenLConfig {
  baseUrl: string;
  // Personal Access Token Authentication
  personalAccessToken?: string;
  /**
   * Scheme for the credential above. Defaults to `Token` when omitted, so every
   * existing caller (env PAT, CLI `--token`, stdio) is unaffected.
   */
  authScheme?: OpenLAuthScheme;
  // Request timeout in milliseconds
  timeout?: number;
}

export interface ProjectId {
  repository: string;
  projectName: string;
}

export interface LockInfo {
  lockedBy: string;
  lockedAt: string;
}

export type ProjectStatus =
  | "LOCAL"
  | "DELETED"
  | "OPENED"
  | "VIEWING_VERSION"
  | "EDITING"
  | "CLOSED";

export interface ProjectViewModel {
  name: string;
  modifiedBy: string;
  modifiedAt: string;
  revision: string;
  lockInfo?: LockInfo;
  branch?: string;
  branchDefault?: boolean;
  branchProtected?: boolean;
  capabilities?: ProjectCapabilities;
  compileStatus?: ProjectStatusView;
  dependencies?: ProjectDependencyViewModel[];
  descriptor?: DescriptorViewModel;
  path?: string;
  id: string;
  status?: ProjectStatus;
  tags?: Record<string, string>;
  comment?: string;
  repository?: string;
  repositoryInfo?: ProjectRepositoryModel;
  usedBy?: ProjectDependencyViewModel[];
}

export interface ProjectCapabilities {
  canClose?: boolean;
  canCompare?: boolean;
  canCopy?: boolean;
  canDelete?: boolean;
  canDeleteBranch?: boolean;
  canDeploy?: boolean;
  canExport?: boolean;
  canManage?: boolean;
  canManageBranches?: boolean;
  canOpen?: boolean;
  canSave?: boolean;
  canUnlock?: boolean;
  canViewHistory?: boolean;
  canWrite?: boolean;
}

export interface ProjectDependencyViewModel {
  id: string;
  name: string;
  branch?: string;
  branchDefault?: boolean;
  branchProtected?: boolean;
  missing?: boolean;
  repository?: string;
  status?: ProjectStatus;
  transitive?: boolean;
}

export interface ModuleViewModel {
  modules?: ModuleViewModel[];
  name?: string;
  path?: string;
}

export interface DescriptorViewModel {
  modules?: ModuleViewModel[];
  modulesDefault?: boolean;
  sources?: string[];
  sourcesDefault?: boolean;
}

export interface ProjectRepositoryModel {
  features?: RepositoryFeatures;
  id?: string;
  name?: string;
  type?: string;
}

/**
 * Compact create/copy response from the repository project endpoints. The
 * revision is the Git commit SHA of the atomic project changeset.
 */
export interface CreateProjectResult {
  revision: string;
  /** Present only for repositories that support branches. */
  branch?: string;
}

/**
 * Request body for repository file copy/move operations
 * (POST /repos/{repo}/file-copy and /file-move). Paths are mount-relative.
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
  /** Size in bytes, when reported by the backend. */
  size?: number;
  /** ISO-8601 last-modified timestamp, when reported by the backend. */
  lastModified?: string;
}

/**
 * Search query body for POST /projects/{projectId}/file-search (FileSearchQuery).
 * All fields are optional; an empty body matches everything in scope.
 */
export interface FileSearchQuery {
  /** Ant-glob path pattern, e.g. all xlsx under rules. */
  pattern?: string;
  /** Case-insensitive content substring. Studio inspects text files only, never binary files. */
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
 * Result of reading a single project file's bytes via the Projects: Files API.
 * Returned by {@link OpenLClient.readProjectFile} so the caller can decide how to
 * decode the payload (text vs base64) and whether the body was a file download or
 * a JSON listing/metadata response.
 */
export interface ProjectFileResponse {
  /** Raw response body. */
  data: Buffer;
  /** Response Content-Type header (lower-cased), if any. */
  contentType: string;
  /** Response Content-Disposition header, if any (present for file downloads). */
  contentDisposition: string;
}

export type TableKind =
  | "Rules"
  | "Spreadsheet"
  | "Datatype"
  | "Data"
  | "Test"
  | "TBasic"
  | "Column Match"
  | "Method"
  | "Run"
  | "Constants"
  | "Conditions"
  | "Actions"
  | "Returns"
  | "Environment"
  | "Properties"
  | "Other";

export interface SummaryTableView {
  id: string;
  /** Backend summary contract leaves this open for non-editable/custom table types. */
  tableType?: string;
  kind?: TableKind;
  name?: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  properties?: Record<string, any>;
  returnType?: string;
  signature?: string;
  file?: string;
  pos?: string;
}

/** Fields shared by executable and datatype nodes in the Studio dependency graph. */
export interface BaseTableNodeView {
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
export interface ExecutableNodeView extends BaseTableNodeView {
  kind?: Exclude<TableKind, "Datatype"> | "Dispatcher";
  dimensionProperties?: Record<string, string>;
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
export interface PropertyValueView {
  code?: string;
  value?: string;
}

/** A property that a table (or a Properties table entry) may declare. */
export interface PropertyDefinitionView {
  multiple?: boolean;
  name?: string;
  type?: "text" | "date" | "boolean" | "enum";
  values?: PropertyValueView[];
}

/** Replacement property sent when copying a table. */
export interface TableProperty {
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

/** Query scope accepted by GET /projects/{projectId}/branches. */
export type BranchScope = "project" | "repository";

export type MergeMode = "receive" | "send";

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
}

export interface ConflictGroup {
  files?: string[];
  projectName?: string;
  projectPath?: string;
}

export interface MergeResultResponse {
  conflictGroups?: ConflictGroup[];
  status?: "success" | "conflicts";
}

export interface RevisionDetails {
  author?: string;
  branch?: string;
  commit?: string;
  exists?: boolean;
  modifiedAt?: string;
}

export interface ConflictDetailsResponse {
  baseRevision?: RevisionDetails;
  conflictGroups?: ConflictGroup[];
  defaultMessage?: string;
  oursRevision?: RevisionDetails;
  theirsRevision?: RevisionDetails;
}

export interface MergeConflictFileResponse {
  data: Buffer;
  contentType: string;
  contentDisposition: string;
}

export interface RepositoryInfo {
  aclId: string;
  /** Repository ID */
  id: string;
  /** Repository name */
  name: string;
  capabilities?: RepositoryCapabilities;
  features?: RepositoryFeatures;
  mainBranchOnly?: boolean;
  type?: string;
}

/** Repository features (from OpenAPI) */
export interface RepositoryFeatures {
  branches?: boolean;
  mappedFolders?: boolean;
  searchable?: boolean;
}

/** Repository capabilities (from OpenAPI) */
export interface RepositoryCapabilities {
  canCreateProject?: boolean;
  canManage?: boolean;
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
  modifiedOn?: string;
}

/** Deployment view model (short version from OpenAPI 3.0.1) */
export interface DeploymentViewModel_Short {
  id: string;
  name: string;
  repository: string;
  items?: DeploymentItemViewModel_Short[];
}

export interface DeploymentItemViewModel_Short {
  modifiedAt: string;
  modifiedBy: string;
  name: string;
  revision: string;
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

export interface ProjectInfo {
  name: string;
  repository: string;
  path: string;
  branch?: string;
  modules?: Array<{
    name: string;
    rulesRootPath?: string;
  }>;
  dependencies?: Array<{
    name: string;
    autoIncluded?: boolean;
  }>;
  classpath?: string[];
  tags?: Record<string, string>;
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
  discardChanges?: boolean;
  openDependencies?: boolean;
  save?: boolean;
}

// =============================================================================
// Type Aliases for API Client
// =============================================================================

/** Repository information */
export type Repository = RepositoryInfo;

/** Project summary for list operations */
export type ProjectSummary = ProjectViewModel;

/** Full project details */
export type Project = ProjectViewModel;

/** Table metadata for list operations */
export type TableMetadata = SummaryTableView;

/**
 * One side of a cell border: line style and width. Mirrors
 * `RawTableCellBorderSide` in the studio OpenAPI.
 */
export interface RawTableCellBorderSide {
  style?: "solid" | "dashed" | "dotted" | "double";
  width?: number;
}

/**
 * Cell borders per side; a side is absent when the cell has no border there.
 * Mirrors `RawTableCellBorder` in the studio OpenAPI.
 */
export interface RawTableCellBorder {
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
export type RawCellScalar = string | number | boolean | null;

/** A scalar cell value or Studio's one-dimensional multi-value representation. */
export type RawCellValue = RawCellScalar | RawCellScalar[];

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
  /** Excel cell style; only present when the raw view was requested with `styles=true`. */
  style?: RawTableCellStyle;
}

/**
 * Options for the table's raw source view: read the
 * source matrix in row slices and/or with per-cell Excel styles. Mirrors the
 * `startRow`/`maxRows`/`styles` query parameters of `GET .../tables/{tableId}`.
 */
export interface RawTableViewOptions {
  /** Zero-based index of the first row to return; omit to start at the top. */
  startRow?: number;
  /** Maximum number of rows to return, counted from `startRow`; omit to read to the end. */
  maxRows?: number;
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
  /**
   * Total number of rows when the returned window omits rows (a `startRow`
   * offset or a `maxRows` cap); absent when the whole table is returned.
   */
  totalRows?: number;
}

/** Raw source rows accepted by POST .../tables/{tableId}/lines. */
export interface RawTableAppend {
  tableType: "RawSource";
  rows: RawTableCell[][];
}

/**
 * A cell to write via a raw table-source edit (POST .../tables/{id}/actions).
 * Mirrors `RawCellInput` in the studio OpenAPI — the input counterpart of
 * {@link RawTableCell} (no read-only `cell` address). A cell may set
 * colspan/rowspan to merge; `covered` marks a cell masked by another's span.
 */
export interface RawCellInput {
  value?: RawCellValue;
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
  include?: Array<"summary" | "status" | "deleted" | "descriptor">;
  /** Project tags - must start with `tags.` prefix, e.g., { "tags.insurance.home": "value" } */
  tags?: Record<string, string>;
  /** Pagination: page number (0-based, default: 0) */
  page?: number;
  /** Pagination: page size (default: 50) */
  size?: number;
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
  testUnits: TestUnitExecutionResult[];
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
}

/** Test execution start response */
export interface TestExecutionStartResponse {
  status: "started" | "accepted";
  projectId: string;
  tableId?: string;
  testRanges?: string;
  projectWasOpened?: boolean;
  message: string;
}

/** Test results summary (without testCases array) */
export interface TestResultsSummary {
  executionTimeMs: number;
  numberOfTests: number;
  numberOfFailures: number;
  numberOfPassed: number;
}

/** Single-project response from the current Studio API. */
export type ComprehensiveProject = ProjectViewModel;

/** Filters for listing tables */
export interface TableFilters {
  /** Filter by table kinds (array of strings). Valid values: 'Rules', 'Spreadsheet', 'Datatype', 'Data', 'Test', 'TBasic', 'Column Match', 'Method', 'Run', 'Constants', 'Conditions', 'Actions', 'Returns', 'Environment', 'Properties', 'Other' */
  kind?: string[];
  /** Filter by table name fragment */
  name?: string;
  /** Filter by project properties (will be prefixed with 'properties.' in query string) */
  properties?: Record<string, string>;
  /** Pagination: page number (0-based, default: 0) */
  page?: number;
  /** Pagination: page size (default: 50) */
  size?: number;
  /** Pagination: offset (alternative to page, for backward compatibility) */
  offset?: number;
  /** Pagination: limit (alternative to size, for backward compatibility) */
  limit?: number;
}

/** Save project result. API returns 204 No Content and does not provide commit hash, version, author, or timestamp. */
export interface SaveProjectResult {
  success: boolean;
  message: string;
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
  table: RawTableView & { name: string };
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
 * Normalized collection returned by list endpoints.
 *
 * `serverPaginated` distinguishes a backend page from a legacy bare array. A
 * bare array still needs client-side pagination; a backend page must never be
 * sliced a second time. The total can be absent on older Studio versions, so
 * callers must not substitute the current page size for it.
 */
export interface CollectionPage<T> {
  items: T[];
  serverPaginated: boolean;
  pageNumber?: number;
  pageSize?: number;
  total?: number;
  totalPages?: number;
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
export type MessageSource = Record<string, unknown>;

/**
 * Compilation message. The backend flattens `MessageDescription`
 * (`id`/`summary`/`severity`) onto this type via `@JsonUnwrapped`, so those
 * fields appear at the top level alongside `location` and `stacktrace`.
 */
export interface DetailedMessageDescription {
  id?: number;
  summary?: string;
  severity?: "ERROR" | "WARN" | "INFO";
  location?: MessageSource;
  stacktrace?: boolean;
}

export interface CompilationMessages {
  items: DetailedMessageDescription[];
  total: number;
  errors: number;
  warnings: number;
}

export interface CompilationModules {
  total: number;
  compiled: number;
  compiledModules?: string[];
}

export interface CompilationTests {
  total: number;
}

export interface CompilationDetails {
  messages: CompilationMessages;
  modules: CompilationModules;
  tests: CompilationTests;
}

export interface ProjectModifiedBy {
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
export type FileChangeType = "added" | "modified" | "deleted";

export interface FileChange {
  /** `<projectRealPath>/<file>` (forward slashes), matching the merge API. */
  path: string;
  type: FileChangeType;
}

export interface PendingChanges {
  total: number;
  files: FileChange[];
}

/**
 * Post-compilation project status returned by `GET /projects/{id}/status`.
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
export type FrameKind =
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
  /** true → value omitted; fetch via openl_get_trace_value with parameterId. */
  lazy?: boolean;
  parameterId?: number;
  value?: unknown;
  schema?: object;
}

/** Error/warning message attached to a failed frame. */
export interface MessageDescription {
  id?: number;
  severity: "INFO" | "WARN" | "ERROR";
  summary: string;
}

/** Dispatcher badge — the table was chosen from overloaded (dimension-property) versions. */
export interface DispatchInfo {
  candidates: Array<{ label: string; chosen: boolean }>;
}

/** Current line inside a frame. */
export interface DebugLocationView {
  kind: "cell" | "dtrule" | "operation";
  row?: number;
  column?: number;
  /** Short cell reference, e.g. "R2C3" — breakpoint sub-step key. */
  ref?: string;
  /** Human-readable, e.g. "$Formula$HouseTotal" or a fired rule name. */
  label?: string;
}

/** A sub-step of a frame (only executable cells are steps). */
export interface StepValueView {
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
export interface CallNodeView {
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

/**
 * One page of a step's executed sub-calls — the lazy profiling call tree loaded
 * one level at a time (openl_expand_trace_tree). Each child is itself shallow:
 * its steps carry `childrenTotal`, not embedded `children`.
 */
export interface TreeChildrenView {
  /** This page of the step's sub-calls, starting at the requested offset. */
  children: CallNodeView[];
  /** The step's full sub-call count — page again while it exceeds offset + children.length. */
  total: number;
}

/** One frame of the live execution stack. */
export interface DebugFrameView {
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
export interface DebugError {
  summary: string;
  table?: string;
  location?: string;
  type?: string;
  detail?: string;
}

/** One hot table in the profile — time aggregated across all its invocations. */
export interface ProfileHotspotView {
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
export interface ProfileSummaryView {
  /** Top-N slowest tables by selfMillis, most-expensive first. */
  hotspots: ProfileHotspotView[];
  /** Distinct tables that executed (may exceed hotspots.length). */
  distinctTables: number;
  /** Total table invocations in the run (the size of the full tree). */
  nodeCount: number;
  /** Wall-clock of the whole run. */
  totalMillis: number;
  /** true when more tables executed than were returned as hotspots. */
  truncated: boolean;
}

/** The live execution stack — returned by start / step / stack reads. */
export interface DebugStackView {
  /** Debug-session identity, also carried by WebSocket status events. */
  sessionId?: string;
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
export interface WatchPointView {
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
export interface WatchSeriesView {
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
export interface DecisionView {
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
  fromModule?: string;
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

// =============================================================================
// API Error Response Types
// =============================================================================

/** Error detail in API error response (for 400 status) */
export interface ApiErrorDetail {
  code?: string;
  message?: string;
}

/** Field validation error in API error response (for 400 status) */
export interface ApiFieldError {
  code?: string;
  field?: string;
  message?: string;
  rejectedValue?: unknown;
}

/** Extracted error information from API response */
export interface ExtractedErrorInfo {
  code?: string;
  message?: string;
  errors?: ApiErrorDetail[];
  fields?: ApiFieldError[];
  rawResponse?: unknown; // Original response data if structure doesn't match expected formats
}
