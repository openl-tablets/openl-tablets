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
  id: ProjectId;
  status: ProjectStatus;
  tags?: Record<string, string>;
  comment?: string;
  repository: string;
  selectedBranches?: string[];
}

export type TableType =
  | "datatype"
  | "vocabulary"
  | "spreadsheet"
  | "simplerules"
  | "smartrules"
  | "tbasic"
  | "method"
  | "properties"
  | "data"
  | "test"
  | "run";

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

export interface BranchCreateRequest {
  name: string;
  comment?: string;
}

export interface ProjectUpdateRequest {
  status?: ProjectStatus;
  comment?: string;
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
  ruleType: TableType;
  file?: string;
  properties?: Record<string, unknown>;
  comment?: string;
}

/** Rule creation result */
export interface CreateRuleResult {
  success: boolean;
  tableId?: string;
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
