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
