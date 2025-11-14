/**
 * Type definitions for OpenL Studio REST API
 */

/**
 * Project information
 */
export interface ProjectInfo {
  name: string;
  repository: string;
  status: 'OPEN' | 'CLOSED' | 'VIEWING';
  lastModified?: string;
  dependencies?: string[];
  version?: string;
  branch?: string;
}

/**
 * Repository information
 */
export interface RepositoryInfo {
  name: string;
  type: string;
  path?: string;
}

/**
 * Parameters for opening a project
 */
export interface OpenProjectParams {
  repositoryName: string;
  projectName: string;
  openDependencies?: boolean;
}

/**
 * Parameters for closing a project
 */
export interface CloseProjectParams {
  repositoryName: string;
  projectName: string;
}

/**
 * Parameters for copying a project
 */
export interface CopyProjectParams {
  sourceRepository: string;
  sourceProject: string;
  destinationRepository: string;
  destinationProject: string;
  copyHistory?: boolean;
  comment?: string;
}

/**
 * Parameters for exporting a project
 */
export interface ExportProjectParams {
  repositoryName: string;
  projectName: string;
  version?: string;
  outputPath?: string;
}

/**
 * Parameters for deleting a project
 */
export interface DeleteProjectParams {
  repositoryName: string;
  projectName: string;
  comment?: string;
}

/**
 * API response for project operations
 */
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  error?: string;
  message?: string;
}

/**
 * Login credentials
 */
export interface LoginCredentials {
  username: string;
  password: string;
}
