/**
 * TypeScript types for OpenL Tablets REST API
 */

export type AuthMethod = "basic" | "apiKey" | "oauth2";

export interface OAuth2Config {
  clientId: string;
  clientSecret: string;
  tokenUrl: string;
  authorizationUrl?: string;
  scope?: string;
  grantType?: "client_credentials" | "authorization_code" | "refresh_token";
  refreshToken?: string;
}

export interface OAuth2Token {
  access_token: string;
  token_type: string;
  expires_in?: number;
  refresh_token?: string;
  scope?: string;
  expires_at?: number; // Calculated expiration timestamp
}

export interface OpenLConfig {
  baseUrl: string;
  // Basic Authentication
  username?: string;
  password?: string;
  // API Key Authentication
  apiKey?: string;
  // OAuth 2.1 Authentication
  oauth2?: OAuth2Config;
  // Client Document ID (for request tracking)
  clientDocumentId?: string;
  // Request timeout in milliseconds
  timeout?: number;
}

export interface ProjectId {
  repository: string;
  projectName: string;
}

export interface LockInfo {
  locked: boolean;
  lockedBy?: string;
  lockedAt?: string;
}

export type ProjectStatus =
  | "LOCAL"
  | "ARCHIVED"
  | "OPENED"
  | "VIEWING_VERSION"
  | "EDITING"
  | "CLOSED";

export interface ProjectViewModel {
  name: string;
  modifiedBy: string;
  modifiedAt: string;
  lockInfo?: LockInfo;
  branch?: string;
  revision?: string;
  path: string;
  // OpenL 6.0.0+ returns base64-encoded string, older versions return object
  id: ProjectId | string;
  status: ProjectStatus;
  tags?: Record<string, string>;
  comment?: string;
  repository: string;
  selectedBranches?: string[];
}

export type TableType =
  // Decision Tables (most common - 5 variants)
  | "Rules"           // Standard decision table with explicit C/A/RET columns
  | "SimpleRules"     // Simplified decision table with positional matching
  | "SmartRules"      // Flexible decision table with smart parameter matching
  | "SimpleLookup"    // Two-dimensional lookup table
  | "SmartLookup"     // Two-dimensional lookup with smart matching
  // Spreadsheet (most common - calculations)
  | "Spreadsheet"     // Multi-step calculations with formulas
  // Other types (rarely used)
  | "Method"          // Custom Java-like methods
  | "TBasic"          // Complex flow control algorithms
  | "Data"            // Relational data tables
  | "Datatype"        // Custom data structure definitions
  | "Test"            // Unit test tables
  | "Run"             // Test suite execution
  | "Properties"      // Dimension properties configuration
  | "Configuration";  // Environment settings

export type TableKind =
  | "XLS_DT"
  | "XLS_SPREADSHEET"
  | "XLS_DATATYPE"
  | "XLS_VOCABULARY"
  | "XLS_METHOD"
  | "XLS_PROPERTIES"
  | "XLS_DATA"
  | "XLS_TEST"
  | "XLS_RUN";

export interface SummaryTableView {
  id: string;
  tableType: TableType;
  kind: TableKind;
  name: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  properties?: Record<string, any>;
  returnType?: string;
  signature?: string;
  file: string;
  pos: string;
}

export interface TableProperty {
  name: string;
  type: string;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  value?: any;
  description?: string;
}

export interface EditableTableView {
  id: string;
  tableType: TableType;
  kind: TableKind;
  name: string;
  technicalName?: string;
  properties?: TableProperty[];
  uri?: string;
  isBusinessView?: boolean;
  editable?: boolean;
  file?: string;
}

export interface DatatypeView extends EditableTableView {
  fields?: Array<{
    name: string;
    type: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    defaultValue?: any;
  }>;
  parentType?: string;
}

export interface SpreadsheetView extends EditableTableView {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  rows?: any[][];
  columnNames?: string[];
  columnTypes?: string[];
}

export interface SimpleRulesView extends EditableTableView {
  rules?: Array<{
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    conditions?: Record<string, any>;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    actions?: Record<string, any>;
  }>;
  conditionColumns?: string[];
  actionColumns?: string[];
}

export interface RepositoryInfo {
  id: string;
  name: string;
  type: string;
  features?: {
    branches?: boolean;
    mappedFolders?: boolean;
    searchable?: boolean;
  };
}

export interface FileData {
  name: string;
  version?: string;
  author?: string;
  modifiedAt?: string;
  comment?: string;
  size?: number;
  branch?: string;
  deleted?: boolean;
}

export interface ProjectHistoryItem extends FileData {
  version: string;
  author: string;
  modifiedAt: string;
  comment: string;
}

export interface DeploymentInfo {
  id: string;
  name: string;
  projectName: string;
  projectVersion?: string;
  repository: string;
  status: string;
  deployedAt?: string;
  deployedBy?: string;
}

/** Deployment view model (short version from OpenAPI 3.0.1) */
export interface DeploymentViewModel_Short {
  id: string;
  name: string;
  projectId: string;
  productionRepositoryId: string;
  deployedAt?: string;
  deployedBy?: string;
  status?: string;
}

/** Deploy project request (OpenAPI 3.0.1) */
export interface DeployProjectRequest {
  projectId: string;              // Base64-encoded project ID
  deploymentName: string;         // Name for the deployment
  productionRepositoryId: string;
  comment?: string;
}

/** Redeploy project request (OpenAPI 3.0.1) */
export interface RedeployProjectRequest {
  projectId: string;              // Base64-encoded project ID
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

export interface CreateProjectRequest {
  comment?: string;
}

export interface UpdateTableRequest {
  view: EditableTableView;
  comment?: string;
}

export interface DeployRequest {
  projectName: string;
  repository: string;
  version?: string;
  deploymentRepository: string;
}

/** Branch create request (OpenAPI 3.0.1) */
export interface BranchCreateRequest {
  branch: string;       // Branch name (required)
  revision?: string;    // Revision to branch from (optional)
}

export interface ProjectUpdateRequest {
  status?: ProjectStatus;
  comment?: string;
}

/** Project status update model (OpenAPI 3.0.1) */
export interface ProjectStatusUpdateModel {
  status: "OPENED" | "CLOSED";
  branch?: string;
  revision?: string;
  comment?: string;
  selectedBranches?: string[];
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

/** Project history entry */
export type ProjectHistory = ProjectHistoryItem;

/** Table metadata for list operations */
export type TableMetadata = SummaryTableView;

/** Full table view with data */
export type TableView = EditableTableView;

/** Deployment result */
export type DeploymentResult = DeploymentInfo;

/** Filters for listing projects */
export interface ProjectFilters {
  repository?: string;
  status?: string;
  tag?: string;
}

// =============================================================================
// Testing & Validation Types
// =============================================================================

/** Test execution result */
export interface TestResult {
  testName: string;
  status: "PASSED" | "FAILED" | "ERROR";
  message?: string;
  executionTime?: number;
  failureDetails?: string;
}

/** Test suite result */
export interface TestSuiteResult {
  projectName: string;
  totalTests: number;
  passed: number;
  failed: number;
  errors: number;
  executionTime: number;
  tests: TestResult[];
}

/** Project validation result */
export interface ValidationResult {
  valid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}

/** Validation error */
export interface ValidationError {
  severity: "ERROR";
  message: string;
  location?: string;
  table?: string;
  line?: number;
}

/** Validation warning */
export interface ValidationWarning {
  severity: "WARNING";
  message: string;
  location?: string;
  table?: string;
}

// =============================================================================
// Phase 1: New Types for Extended Functionality
// =============================================================================

/** Comprehensive project details combining Project and ProjectInfo */
export interface ComprehensiveProject extends ProjectViewModel {
  /** Modules in the project */
  modules?: Array<{
    name: string;
    rulesRootPath?: string;
  }>;
  /** Project dependencies */
  dependencies?: Array<{
    name: string;
    autoIncluded?: boolean;
  }>;
  /** Classpath entries */
  classpath?: string[];
}

/** Filters for listing tables */
export interface TableFilters {
  /** Filter by table type (datatype, spreadsheet, simplerules, etc.) */
  tableType?: TableType;
  /** Filter by table name pattern */
  name?: string;
  /** Filter by Excel file name */
  file?: string;
}

/** Save project result */
export interface SaveProjectResult {
  success: boolean;
  commitHash?: string;       // Git commit hash (e.g., "7a3f2b1c...")
  version?: string;          // Same as commitHash
  author?: {
    name: string;
    email: string;
  };
  timestamp?: string;        // ISO timestamp
  message?: string;
  validationErrors?: ValidationError[];
}

/** File upload result */
export interface FileUploadResult {
  success: boolean;
  fileName: string;
  commitHash?: string;       // Git commit hash created by upload
  version?: string;          // Same as commitHash
  author?: {
    name: string;
    email: string;
  };
  timestamp?: string;        // ISO timestamp
  size?: number;
  message?: string;
}

/** Rule creation request */
export interface CreateRuleRequest {
  name: string;
  tableType: TableType;           // Type of table to create
  returnType?: string;            // Return type (e.g., 'int', 'String', 'SpreadsheetResult')
  parameters?: Array<{            // Method parameters
    type: string;                 // Parameter type (e.g., 'String', 'int', 'Policy')
    name: string;                 // Parameter name (e.g., 'driverType', 'age')
  }>;
  file?: string;                  // Target Excel file (optional, uses default if not specified)
  properties?: Record<string, unknown>;  // Dimension properties (state, lob, effectiveDate, etc.)
  comment?: string;               // Commit comment
}

/** Rule creation result */
export interface CreateRuleResult {
  success: boolean;
  tableId?: string;
  tableName?: string;
  tableType?: TableType;
  file?: string;
  message?: string;
}

// =============================================================================
// Phase 2: Testing & Validation Types
// =============================================================================

/** Test execution request with smart selection */
export interface RunTestRequest {
  projectId: string;
  /** Specific test IDs to run (optional) */
  testIds?: string[];
  /** Run tests related to specific tables (optional) */
  tableIds?: string[];
  /** Run all tests if true */
  runAll?: boolean;
}

/** Enhanced validation result with detailed errors */
export interface DetailedValidationResult extends ValidationResult {
  /** Total error count */
  errorCount: number;
  /** Total warning count */
  warningCount: number;
  /** Errors grouped by category */
  errorsByCategory?: {
    typeErrors: ValidationError[];
    syntaxErrors: ValidationError[];
    referenceErrors: ValidationError[];
    validationErrors: ValidationError[];
  };
  /** Auto-fixable error count */
  autoFixableCount?: number;
}

// =============================================================================
// Phase 3: Versioning & Execution Types
// =============================================================================

/** Copy table request */
export interface CopyTableRequest {
  projectId: string;
  tableId: string;
  newName?: string;
  targetFile?: string;
  comment?: string;
}

/** Copy table result */
export interface CopyTableResult {
  success: boolean;
  newTableId?: string;
  message?: string;
}

/** Execute rule request */
export interface ExecuteRuleRequest {
  projectId: string;
  ruleName: string;
  inputData: Record<string, unknown>;
}

/** Execute rule result */
export interface ExecuteRuleResult {
  success: boolean;
  output?: unknown;
  executionTime?: number;
  error?: string;
}

/** Compare versions request */
export interface CompareVersionsRequest {
  projectId: string;
  baseCommitHash: string;     // Git commit hash to compare from
  targetCommitHash: string;   // Git commit hash to compare to
}

/** Version difference */
export interface VersionDifference {
  added: string[];
  modified: string[];
  removed: string[];
}

/** Compare versions result */
export interface CompareVersionsResult {
  tables: VersionDifference;
  files: VersionDifference;
  summary: {
    totalChanges: number;
    tablesAdded: number;
    tablesModified: number;
    tablesRemoved: number;
  };
}

// =============================================================================
// Phase 4: Advanced Features
// =============================================================================

/** Revert version request */
export interface RevertVersionRequest {
  projectId: string;
  targetVersion: string;       // Git commit hash to revert to
  comment?: string;
}

/** Revert version result */
export interface RevertVersionResult {
  success: boolean;
  newVersion?: string;         // New Git commit hash created by revert
  message?: string;
  validationErrors?: ValidationError[];
}

// =============================================================================
// Phase 2: Git Version History Types
// =============================================================================

/** Commit type from OpenL operations */
export type CommitType = "SAVE" | "ARCHIVE" | "RESTORE" | "ERASE" | "MERGE";

/** Get file history request */
export interface GetFileHistoryRequest {
  projectId: string;
  filePath: string;
  limit?: number;        // Max commits to return (default: 50)
  offset?: number;       // Skip first N commits (for pagination)
}

/** File history commit entry */
export interface FileHistoryCommit {
  commitHash: string;
  author: { name: string; email: string };
  timestamp: string;
  comment: string;
  commitType: CommitType;
  size?: number;
}

/** Get file history result */
export interface GetFileHistoryResult {
  filePath: string;
  commits: FileHistoryCommit[];
  total: number;         // Total commits available
  hasMore: boolean;      // More commits available
}

/** Get project history request */
export interface GetProjectHistoryRequest {
  projectId: string;
  page?: number;        // Page number (default: 0, min: 0)
  size?: number;        // Page size (default: 50, min: 1)
  search?: string;      // Regex search term
  techRevs?: boolean;   // Include non-project revisions (default: false)
  branch?: string;      // Optional: specific branch (default: current branch)
}

/** Project revision (short version from OpenAPI 3.0.1) */
export interface ProjectRevision_Short {
  commitHash: string;
  version?: string;     // Alias for commitHash
  author: { name: string; email: string };
  modifiedAt: string;   // ISO timestamp
  comment: string;
  commitType?: CommitType;
  filesChanged?: number;
  tablesChanged?: number;
}

/** Paginated response for project history (OpenAPI 3.0.1) */
export interface PageResponseProjectRevision_Short {
  content: ProjectRevision_Short[];
  numberOfElements: number;
  pageNumber: number;
  pageSize: number;
  totalElements?: number;
  totalPages?: number;
}

/** Project history commit entry (legacy) */
export interface ProjectHistoryCommit {
  commitHash: string;
  author: { name: string; email: string };
  timestamp: string;
  comment: string;
  commitType: CommitType;
  filesChanged: number;
  tablesChanged?: number;
}

/** Get project history result (legacy) */
export interface GetProjectHistoryResult {
  projectId: string;
  branch: string;
  commits: ProjectHistoryCommit[];
  total: number;
  hasMore: boolean;
}

// =============================================================================
// Phase 3: Dimension Properties Types
// =============================================================================

/** Get file name pattern request */
export interface GetFileNamePatternRequest {
  projectId: string;
}

/** Get file name pattern result */
export interface GetFileNamePatternResult {
  pattern: string | null;  // e.g., ".*-%state%-%effectiveDate:MMddyyyy%-%lob%"
  properties: string[];     // Extracted property names: ["state", "effectiveDate", "lob"]
}

/** Set file name pattern request */
export interface SetFileNamePatternRequest {
  projectId: string;
  pattern: string;  // e.g., ".*-%state%-%lob%"
}

/** Set file name pattern result */
export interface SetFileNamePatternResult {
  success: boolean;
  pattern: string;
  message: string;
}

/** Get table properties request */
export interface GetTablePropertiesRequest {
  projectId: string;
  tableId: string;
}

/** Get table properties result */
export interface GetTablePropertiesResult {
  tableId: string;
  tableName: string;
  properties: Record<string, any>;  // e.g., { state: "CA", lob: "Auto", effectiveDate: "01/01/2025" }
}

/** Set table properties request */
export interface SetTablePropertiesRequest {
  projectId: string;
  tableId: string;
  properties: Record<string, any>;  // e.g., { state: "CA", lob: "Auto", effectiveDate: "01/01/2025" }
  comment?: string;
}

/** Set table properties result */
export interface SetTablePropertiesResult {
  success: boolean;
  tableId: string;
  properties: Record<string, any>;
  message: string;
}
