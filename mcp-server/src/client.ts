/**
 * OpenL Tablets API Client
 *
 * Provides a high-level interface for interacting with OpenL Tablets WebStudio REST API.
 * Handles all HTTP communication, error handling, and response parsing.
 */

import axios, { AxiosInstance } from "axios";
import * as Types from "./types.js";
import { AuthenticationManager } from "./auth.js";
import { DEFAULTS, PROJECT_ID_PATTERN } from "./constants.js";
import { validateTimeout, sanitizeError } from "./utils.js";

/**
 * Client for OpenL Tablets WebStudio REST API
 *
 * Usage:
 * ```typescript
 * const client = new OpenLClient({
 *   baseUrl: "http://localhost:8080/webstudio/rest",
 *   username: "admin",
 *   password: "admin"
 * });
 *
 * const projects = await client.listProjects();
 * ```
 */
export class OpenLClient {
  private baseUrl: string;
  private axiosInstance: AxiosInstance;
  private authManager: AuthenticationManager;

  /**
   * Create a new OpenL Tablets API client
   *
   * @param config - Client configuration including base URL and authentication
   */
  constructor(config: Types.OpenLConfig) {
    this.baseUrl = config.baseUrl;

    // Validate and set timeout
    const timeout = validateTimeout(config.timeout, DEFAULTS.TIMEOUT);

    // Create Axios instance with default configuration
    this.axiosInstance = axios.create({
      baseURL: this.baseUrl,
      timeout,
      headers: {
        "Content-Type": "application/json",
      },
    });

    // Setup authentication
    this.authManager = new AuthenticationManager(config);
    this.authManager.setupInterceptors(this.axiosInstance);
  }

  /**
   * Get the base URL of the OpenL Tablets instance
   */
  public getBaseUrl(): string {
    return this.baseUrl;
  }

  /**
   * Get the current authentication method
   */
  public getAuthMethod(): string {
    return this.authManager.getAuthMethod();
  }

  // =============================================================================
  // Repository Management
  // =============================================================================

  /**
   * List all design repositories
   *
   * @returns Array of repository information
   */
  async listRepositories(): Promise<Types.Repository[]> {
    const response = await this.axiosInstance.get<Types.Repository[]>(
      "/design-repositories"
    );
    return response.data;
  }

  /**
   * List branches in a repository
   *
   * @param repository - Repository name
   * @returns Array of branch names
   */
  async listBranches(repository: string): Promise<string[]> {
    const response = await this.axiosInstance.get<string[]>(
      `/design-repositories/${repository}/branches`
    );
    return response.data;
  }

  // =============================================================================
  // Project Management
  // =============================================================================

  /**
   * Parse a project ID into repository and project name
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Tuple of [repository, projectName]
   * @throws Error if project ID format is invalid
   */
  private parseProjectId(projectId: string): [string, string] {
    const match = projectId.match(PROJECT_ID_PATTERN);
    if (!match) {
      throw new Error(
        `Invalid project ID format: ${projectId}. Expected: repository-projectName`
      );
    }
    return [match[1], match[2]];
  }

  /**
   * List all projects with optional filters
   *
   * @param filters - Optional filters for repository, status, and tag
   * @returns Array of project summaries
   */
  async listProjects(
    filters?: Types.ProjectFilters
  ): Promise<Types.ProjectSummary[]> {
    const response = await this.axiosInstance.get<Types.ProjectSummary[]>(
      "/projects",
      { params: filters }
    );
    return response.data;
  }

  /**
   * Get detailed project information
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Project details
   */
  async getProject(projectId: string): Promise<Types.Project> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.get<Types.Project>(
      `/design-repositories/${repository}/projects/${projectName}`
    );
    return response.data;
  }

  /**
   * Get project information including modules and dependencies
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Project information
   */
  async getProjectInfo(projectId: string): Promise<Types.ProjectInfo> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.get<Types.ProjectInfo>(
      `/design-repositories/${repository}/projects/${projectName}/info`
    );
    return response.data;
  }

  /**
   * Open a project for viewing/editing
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Success status
   */
  async openProject(projectId: string): Promise<boolean> {
    const [repository, projectName] = this.parseProjectId(projectId);
    await this.axiosInstance.post(
      `/design-repositories/${repository}/projects/${projectName}/open`
    );
    return true;
  }

  /**
   * Close an open project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Success status
   */
  async closeProject(projectId: string): Promise<boolean> {
    const [repository, projectName] = this.parseProjectId(projectId);
    await this.axiosInstance.post(
      `/design-repositories/${repository}/projects/${projectName}/close`
    );
    return true;
  }

  /**
   * Get project version history
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Array of project history entries
   */
  async getProjectHistory(projectId: string): Promise<Types.ProjectHistory[]> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.get<Types.ProjectHistory[]>(
      `/design-repositories/${repository}/projects/${projectName}/history`
    );
    return response.data;
  }

  /**
   * Create a new branch in a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param branchName - Name for the new branch
   * @param comment - Optional comment describing the branch
   * @returns Success status
   */
  async createBranch(
    projectId: string,
    branchName: string,
    comment?: string
  ): Promise<boolean> {
    const [repository, projectName] = this.parseProjectId(projectId);
    await this.axiosInstance.post(
      `/design-repositories/${repository}/projects/${projectName}/branches`,
      { name: branchName, comment }
    );
    return true;
  }

  // =============================================================================
  // Rules (Tables) Management
  // =============================================================================

  /**
   * List all tables/rules in a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Array of table metadata
   */
  async listTables(projectId: string): Promise<Types.TableMetadata[]> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.get<Types.TableMetadata[]>(
      `/design-repositories/${repository}/projects/${projectName}/tables`
    );
    return response.data;
  }

  /**
   * Get detailed table data and structure
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param tableId - Table identifier
   * @returns Complete table view with data and structure
   */
  async getTable(projectId: string, tableId: string): Promise<Types.TableView> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.get<Types.TableView>(
      `/design-repositories/${repository}/projects/${projectName}/tables/${tableId}`
    );
    return response.data;
  }

  /**
   * Update table content
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param tableId - Table identifier
   * @param view - Updated table view with modifications
   * @param comment - Optional comment describing the changes
   * @returns Updated table view
   */
  async updateTable(
    projectId: string,
    tableId: string,
    view: Types.EditableTableView,
    comment?: string
  ): Promise<Types.TableView> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.put<Types.TableView>(
      `/design-repositories/${repository}/projects/${projectName}/tables/${tableId}`,
      { view, comment }
    );
    return response.data;
  }

  // =============================================================================
  // Deployment Management
  // =============================================================================

  /**
   * List all deployments
   *
   * @returns Array of deployment information
   */
  async listDeployments(): Promise<Types.DeploymentInfo[]> {
    const response = await this.axiosInstance.get<Types.DeploymentInfo[]>(
      "/deployments"
    );
    return response.data;
  }

  /**
   * Deploy a project to a deployment repository
   *
   * @param projectName - Name of the project to deploy
   * @param repository - Source repository containing the project
   * @param deploymentRepository - Target deployment repository
   * @param version - Optional specific version to deploy
   * @returns Deployment result
   */
  async deployProject(
    projectName: string,
    repository: string,
    deploymentRepository: string,
    version?: string
  ): Promise<Types.DeploymentResult> {
    const response = await this.axiosInstance.post<Types.DeploymentResult>(
      `/deployments/${deploymentRepository}`,
      {
        projectName,
        repository,
        version,
      }
    );
    return response.data;
  }

  // =============================================================================
  // Health Check
  // =============================================================================

  /**
   * Check server connectivity and authentication status
   *
   * @returns Health check result with server status and reachability
   */
  async healthCheck(): Promise<{
    status: string;
    baseUrl: string;
    authMethod: string;
    timestamp: string;
    serverReachable: boolean;
    error?: string;
  }> {
    const authMethod = this.getAuthMethod();

    try {
      // Try to list repositories as a connectivity check
      await this.listRepositories();

      return {
        status: "healthy",
        baseUrl: this.baseUrl,
        authMethod,
        timestamp: new Date().toISOString(),
        serverReachable: true,
      };
    } catch (error: unknown) {
      return {
        status: "unhealthy",
        baseUrl: this.baseUrl,
        authMethod,
        timestamp: new Date().toISOString(),
        serverReachable: false,
        error: sanitizeError(error),
      };
    }
  }

  // =============================================================================
  // Project Creation & Management
  // =============================================================================

  /**
   * Create a new project in a repository
   *
   * @param repository - Repository name
   * @param projectName - Name for the new project
   * @param comment - Creation comment
   * @param projectTemplate - Optional template to use
   * @returns Created project information
   */
  async createProject(
    repository: string,
    projectName: string,
    comment?: string,
    projectTemplate?: string
  ): Promise<Types.Project> {
    const response = await this.axiosInstance.post<Types.Project>(
      `/design-repositories/${repository}/projects`,
      {
        name: projectName,
        comment,
        template: projectTemplate,
      }
    );
    return response.data;
  }

  /**
   * Delete a project from a repository
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param comment - Deletion comment
   * @returns Success status
   */
  async deleteProject(projectId: string, comment?: string): Promise<boolean> {
    const [repository, projectName] = this.parseProjectId(projectId);
    await this.axiosInstance.delete(
      `/design-repositories/${repository}/projects/${projectName}`,
      { data: { comment } }
    );
    return true;
  }

  // =============================================================================
  // File Management
  // =============================================================================

  /**
   * Upload a file to a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param filePath - Path where file should be stored
   * @param content - Base64 encoded file content
   * @param comment - Upload comment
   * @returns Success status
   */
  async uploadFile(
    projectId: string,
    filePath: string,
    content: string,
    comment?: string
  ): Promise<boolean> {
    const [repository, projectName] = this.parseProjectId(projectId);
    await this.axiosInstance.post(
      `/design-repositories/${repository}/projects/${projectName}/files`,
      {
        path: filePath,
        content,
        comment,
      }
    );
    return true;
  }

  /**
   * List files in a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Array of file information
   */
  async listFiles(projectId: string): Promise<Types.FileInfo[]> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.get<Types.FileInfo[]>(
      `/design-repositories/${repository}/projects/${projectName}/files`
    );
    return response.data;
  }

  /**
   * Delete a file from a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param filePath - Path to file to delete
   * @param comment - Deletion comment
   * @returns Success status
   */
  async deleteFile(
    projectId: string,
    filePath: string,
    comment?: string
  ): Promise<boolean> {
    const [repository, projectName] = this.parseProjectId(projectId);
    await this.axiosInstance.delete(
      `/design-repositories/${repository}/projects/${projectName}/files/${encodeURIComponent(filePath)}`,
      { data: { comment } }
    );
    return true;
  }

  // =============================================================================
  // Table/Rule Creation & Deletion
  // =============================================================================

  /**
   * Create a new table/rule in a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param tableName - Name for the new table
   * @param tableType - Type of table to create
   * @param file - Excel file where table should be created
   * @param comment - Creation comment
   * @returns Created table view
   */
  async createTable(
    projectId: string,
    tableName: string,
    tableType: string,
    file: string,
    comment?: string
  ): Promise<Types.TableView> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.post<Types.TableView>(
      `/design-repositories/${repository}/projects/${projectName}/tables`,
      {
        name: tableName,
        type: tableType,
        file,
        comment,
      }
    );
    return response.data;
  }

  /**
   * Delete a table/rule from a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param tableId - Table identifier
   * @param comment - Deletion comment
   * @returns Success status
   */
  async deleteTable(
    projectId: string,
    tableId: string,
    comment?: string
  ): Promise<boolean> {
    const [repository, projectName] = this.parseProjectId(projectId);
    await this.axiosInstance.delete(
      `/design-repositories/${repository}/projects/${projectName}/tables/${tableId}`,
      { data: { comment } }
    );
    return true;
  }

  // =============================================================================
  // Testing & Validation
  // =============================================================================

  /**
   * Run a specific test table
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param testTableId - ID of the test table to run
   * @returns Test execution result
   */
  async runTest(
    projectId: string,
    testTableId: string
  ): Promise<Types.TestResult> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.post<Types.TestResult>(
      `/design-repositories/${repository}/projects/${projectName}/tests/${testTableId}/run`
    );
    return response.data;
  }

  /**
   * Run all tests in a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Test suite execution results
   */
  async runAllTests(projectId: string): Promise<Types.TestSuiteResult> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.post<Types.TestSuiteResult>(
      `/design-repositories/${repository}/projects/${projectName}/tests/run`
    );
    return response.data;
  }

  /**
   * Validate a project for errors
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Validation results with errors and warnings
   */
  async validateProject(projectId: string): Promise<Types.ValidationResult> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.get<Types.ValidationResult>(
      `/design-repositories/${repository}/projects/${projectName}/validation`
    );
    return response.data;
  }

  // =============================================================================
  // Rule Execution
  // =============================================================================

  /**
   * Execute rules with provided input data
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param tableName - Name of the rule table to execute
   * @param inputData - Input data for rule execution
   * @returns Rule execution result
   */
  async executeRules(
    projectId: string,
    tableName: string,
    inputData: Record<string, unknown>
  ): Promise<Types.RuleExecutionResult> {
    const [repository, projectName] = this.parseProjectId(projectId);
    const response = await this.axiosInstance.post<Types.RuleExecutionResult>(
      `/design-repositories/${repository}/projects/${projectName}/execute/${tableName}`,
      inputData
    );
    return response.data;
  }
}
