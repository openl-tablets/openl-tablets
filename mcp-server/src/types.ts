/**
 * TypeScript types for OpenL Tablets REST API
 */

export interface OpenLConfig {
  baseUrl: string;
  username?: string;
  password?: string;
  apiKey?: string;
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
  properties?: Record<string, any>;
  returnType?: string;
  signature?: string;
  file: string;
  pos: string;
}

export interface TableProperty {
  name: string;
  type: string;
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
    defaultValue?: any;
  }>;
  parentType?: string;
}

export interface SpreadsheetView extends EditableTableView {
  rows?: any[][];
  columnNames?: string[];
  columnTypes?: string[];
}

export interface SimpleRulesView extends EditableTableView {
  rules?: Array<{
    conditions?: Record<string, any>;
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
