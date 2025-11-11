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
      "/repos"
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
      `/repos/${encodeURIComponent(repository)}/branches`
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
   * Build URL-safe project path
   * Encodes repository and project name to handle spaces and special characters
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns URL-encoded project path
   */
  private buildProjectPath(projectId: string): string {
    const [repository, projectName] = this.parseProjectId(projectId);
    return `/repos/${encodeURIComponent(repository)}/projects/${encodeURIComponent(projectName)}`;
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
   * Get comprehensive project information including details, modules, and dependencies
   * Combines both project details and project info into a single response
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Comprehensive project details
   */
  async getProject(projectId: string): Promise<Types.ComprehensiveProject> {
    const projectPath = this.buildProjectPath(projectId);

    // Fetch both project details and info in parallel
    const [projectResponse, infoResponse] = await Promise.all([
      this.axiosInstance.get<Types.Project>(projectPath),
      this.axiosInstance.get<Types.ProjectInfo>(`${projectPath}/info`),
    ]);

    // Merge the responses
    return {
      ...projectResponse.data,
      modules: infoResponse.data.modules,
      dependencies: infoResponse.data.dependencies,
      classpath: infoResponse.data.classpath,
    };
  }

  /**
   * Open a project for viewing/editing
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Success status
   */
  async openProject(projectId: string): Promise<boolean> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(`${projectPath}/open`);
    return true;
  }

  /**
   * Close an open project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Success status
   */
  async closeProject(projectId: string): Promise<boolean> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(`${projectPath}/close`);
    return true;
  }

  /**
   * Save project changes, creating a new version in the repository
   * This method validates the project before saving
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param comment - Optional comment describing the changes
   * @returns Save result with validation status
   */
  async saveProject(
    projectId: string,
    comment?: string
  ): Promise<Types.SaveProjectResult> {
    const projectPath = this.buildProjectPath(projectId);

    // First validate the project
    const validation = await this.validateProject(projectId);

    // If there are errors, return them without saving
    if (!validation.valid) {
      return {
        success: false,
        message: `Project has ${validation.errors.length} validation error(s). Fix errors before saving.`,
        validationErrors: validation.errors,
      };
    }

    // Save the project
    const response = await this.axiosInstance.post(
      `${projectPath}/save`,
      { comment }
    );

    // Extract commit information from response (FileData structure)
    const fileData = response.data;
    const commitHash = fileData.version || fileData.commitHash;

    return {
      success: true,
      commitHash,
      version: commitHash,  // Same as commitHash for backward compatibility
      author: fileData.author ? {
        name: fileData.author.name || "unknown",
        email: fileData.author.email || ""
      } : undefined,
      timestamp: fileData.modifiedAt || new Date().toISOString(),
      message: `Project saved successfully at commit ${commitHash?.substring(0, 8) || "unknown"}`,
    };
  }

  // =============================================================================
  // File Management
  // =============================================================================

  /**
   * Upload an Excel file with rules to a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param fileName - Name of the file to upload
   * @param fileContent - File content as Buffer or string
   * @param comment - Optional comment
   * @returns Upload result
   */
  async uploadFile(
    projectId: string,
    fileName: string,
    fileContent: Buffer | string,
    comment?: string
  ): Promise<Types.FileUploadResult> {
    const projectPath = this.buildProjectPath(projectId);

    // Validate file extension
    if (!fileName.match(/\.(xlsx|xls)$/i)) {
      return {
        success: false,
        fileName,
        message: "Only Excel files (.xlsx, .xls) are supported",
      };
    }

    // Convert to Buffer if string
    const buffer = Buffer.isBuffer(fileContent) ? fileContent : Buffer.from(fileContent);

    // Upload file using axios with buffer
    // Note: The actual implementation depends on OpenL Tablets API requirements
    // This is a placeholder that may need adjustment based on the actual API
    const response = await this.axiosInstance.post(
      `${projectPath}/files/${encodeURIComponent(fileName)}`,
      buffer,
      {
        headers: {
          "Content-Type": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        },
        params: comment ? { comment } : undefined,
      }
    );

    // Extract commit information from response (FileData structure)
    const fileData = response.data || {};
    const commitHash = fileData.version || fileData.commitHash;

    return {
      success: true,
      fileName,
      commitHash,
      version: commitHash,  // Same as commitHash for backward compatibility
      author: fileData.author ? {
        name: fileData.author.name || "unknown",
        email: fileData.author.email || ""
      } : undefined,
      timestamp: fileData.modifiedAt || new Date().toISOString(),
      size: fileData.size || buffer.length,
      message: `File uploaded successfully at commit ${commitHash?.substring(0, 8) || "unknown"}`,
    };
  }

  /**
   * Download an Excel file from a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param fileName - Name of the file to download
   * @param version - Optional Git commit hash to download specific version
   * @returns File content as Buffer
   */
  async downloadFile(projectId: string, fileName: string, version?: string): Promise<Buffer> {
    const projectPath = this.buildProjectPath(projectId);

    // Build request params
    const params: any = {};
    if (version) {
      params.version = version;  // Git commit hash
    }

    const response = await this.axiosInstance.get<ArrayBuffer>(
      `${projectPath}/files/${encodeURIComponent(fileName)}`,
      {
        responseType: "arraybuffer",
        params,
      }
    );

    return Buffer.from(response.data);
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
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(
      `${projectPath}/branches`,
      { name: branchName, comment }
    );
    return true;
  }

  // =============================================================================
  // Rules (Tables) Management
  // =============================================================================

  /**
   * List all tables/rules in a project with optional filters
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param filters - Optional filters for table type, name, and file
   * @returns Array of table metadata
   */
  async listTables(
    projectId: string,
    filters?: Types.TableFilters
  ): Promise<Types.TableMetadata[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.TableMetadata[]>(
      `${projectPath}/tables`,
      { params: filters }
    );
    return response.data;
  }

  /**
   * Create a new rule/table in a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param request - Rule creation request with name, type, and properties
   * @returns Creation result with table ID
   */
  async createRule(
    projectId: string,
    request: Types.CreateRuleRequest
  ): Promise<Types.CreateRuleResult> {
    const projectPath = this.buildProjectPath(projectId);

    try {
      // Build table signature if parameters provided
      let signature = request.name;
      if (request.returnType && request.parameters) {
        const params = request.parameters.map(p => `${p.type} ${p.name}`).join(", ");
        signature = `${request.returnType} ${request.name}(${params})`;
      }

      const response = await this.axiosInstance.post(
        `${projectPath}/tables`,
        {
          name: request.name,
          type: request.tableType,
          signature,
          returnType: request.returnType,
          parameters: request.parameters,
          properties: request.properties,
          file: request.file,
          comment: request.comment,
        }
      );

      return {
        success: true,
        tableId: response.data.id || `${request.name}-${request.tableType}`,
        tableName: request.name,
        tableType: request.tableType,
        file: response.data.file || request.file,
        message: `Created ${request.tableType} table '${request.name}' successfully`,
      };
    } catch (error: unknown) {
      return {
        success: false,
        message: `Failed to create ${request.tableType} table '${request.name}': ${sanitizeError(error)}`,
      };
    }
  }

  /**
   * Get detailed table data and structure
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param tableId - Table identifier
   * @returns Complete table view with data and structure
   */
  async getTable(projectId: string, tableId: string): Promise<Types.TableView> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.TableView>(
      `${projectPath}/tables/${encodeURIComponent(tableId)}`
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
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.put<Types.TableView>(
      `${projectPath}/tables/${encodeURIComponent(tableId)}`,
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
      `/deployments/${encodeURIComponent(deploymentRepository)}`,
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
  // Testing & Validation
  // =============================================================================

  /**
   * Run all tests in a project
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @returns Test suite execution results
   */
  async runAllTests(projectId: string): Promise<Types.TestSuiteResult> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.post<Types.TestSuiteResult>(
      `${projectPath}/tests/run`
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
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.ValidationResult>(
      `${projectPath}/validation`
    );
    return response.data;
  }

  /**
   * Run specific tests with smart selection
   * Can run specific test IDs, tests related to tables, or all tests
   *
   * @param request - Test execution request with selection criteria
   * @returns Test suite execution results
   */
  async runTest(request: Types.RunTestRequest): Promise<Types.TestSuiteResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    // Build request body based on selection criteria
    const body: Record<string, unknown> = {};

    if (request.testIds && request.testIds.length > 0) {
      body.testIds = request.testIds;
    }

    if (request.tableIds && request.tableIds.length > 0) {
      body.tableIds = request.tableIds;
    }

    // If runAll is true or no specific selection, run all tests
    const endpoint = request.runAll || (!request.testIds && !request.tableIds)
      ? `${projectPath}/tests/run`
      : `${projectPath}/tests/run-selected`;

    const response = await this.axiosInstance.post<Types.TestSuiteResult>(
      endpoint,
      Object.keys(body).length > 0 ? body : undefined
    );

    return response.data;
  }

  /**
   * Get detailed project errors with categorization and fix suggestions
   *
   * @param projectId - Project ID in format "repository-projectName"
   * @param includeWarnings - Include warnings in result (default: true)
   * @returns Detailed validation result with error categorization
   */
  async getProjectErrors(
    projectId: string,
    includeWarnings: boolean = true
  ): Promise<Types.DetailedValidationResult> {
    // Get validation result
    const validation = await this.validateProject(projectId);

    // Categorize errors
    const typeErrors: Types.ValidationError[] = [];
    const syntaxErrors: Types.ValidationError[] = [];
    const referenceErrors: Types.ValidationError[] = [];
    const validationErrors: Types.ValidationError[] = [];

    validation.errors.forEach((error) => {
      const message = error.message.toLowerCase();
      if (message.includes("type") || message.includes("cannot convert")) {
        typeErrors.push(error);
      } else if (message.includes("syntax") || message.includes("unexpected")) {
        syntaxErrors.push(error);
      } else if (message.includes("not found") || message.includes("reference")) {
        referenceErrors.push(error);
      } else {
        validationErrors.push(error);
      }
    });

    // Count auto-fixable errors (type conversions, simple syntax)
    const autoFixableCount = typeErrors.length + syntaxErrors.filter(
      (e) => e.message.includes("bracket") || e.message.includes("parenthes")
    ).length;

    return {
      valid: validation.valid,
      errors: validation.errors,
      warnings: includeWarnings ? validation.warnings : [],
      errorCount: validation.errors.length,
      warningCount: validation.warnings.length,
      errorsByCategory: {
        typeErrors,
        syntaxErrors,
        referenceErrors,
        validationErrors,
      },
      autoFixableCount,
    };
  }

  // =============================================================================
  // Phase 3: Versioning & Execution
  // =============================================================================

  /**
   * Copy a table/rule within a project
   *
   * @param request - Copy table request
   * @returns Copy result with new table ID
   */
  async copyTable(request: Types.CopyTableRequest): Promise<Types.CopyTableResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    try {
      const response = await this.axiosInstance.post(
        `${projectPath}/tables/${encodeURIComponent(request.tableId)}/copy`,
        {
          newName: request.newName,
          targetFile: request.targetFile,
          comment: request.comment,
        }
      );

      return {
        success: true,
        newTableId: response.data.id,
        message: `Table copied successfully${request.newName ? ` as '${request.newName}'` : ""}`,
      };
    } catch (error: unknown) {
      return {
        success: false,
        message: `Failed to copy table: ${sanitizeError(error)}`,
      };
    }
  }

  /**
   * Execute a rule with input data
   *
   * @param request - Execute rule request
   * @returns Execution result with output data
   */
  async executeRule(request: Types.ExecuteRuleRequest): Promise<Types.ExecuteRuleResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    try {
      const startTime = Date.now();
      const response = await this.axiosInstance.post(
        `${projectPath}/rules/${encodeURIComponent(request.ruleName)}/execute`,
        request.inputData
      );
      const executionTime = Date.now() - startTime;

      return {
        success: true,
        output: response.data,
        executionTime,
      };
    } catch (error: unknown) {
      return {
        success: false,
        error: sanitizeError(error),
      };
    }
  }

  /**
   * Compare two versions of a project
   *
   * @param request - Compare versions request
   * @returns Comparison result with differences
   */
  async compareVersions(request: Types.CompareVersionsRequest): Promise<Types.CompareVersionsResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    const response = await this.axiosInstance.get<Types.CompareVersionsResult>(
      `${projectPath}/versions/compare`,
      {
        params: {
          base: request.baseCommitHash,
          target: request.targetCommitHash,
        },
      }
    );

    return response.data;
  }

  // =============================================================================
  // Phase 4: Advanced Features
  // =============================================================================

  /**
   * Revert project to a previous version
   * Creates a new version with the content from the target version
   *
   * @param request - Revert version request
   * @returns Revert result with new version info
   */
  async revertVersion(request: Types.RevertVersionRequest): Promise<Types.RevertVersionResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    try {
      // Step 1: Get the target version content
      await this.axiosInstance.get(
        `${projectPath}/versions/${encodeURIComponent(request.targetVersion)}`
      );

      // Step 2: Validate the project
      const validation = await this.validateProject(request.projectId);

      if (!validation.valid) {
        return {
          success: false,
          message: `Project has validation errors. Fix them before reverting.`,
          validationErrors: validation.errors,
        };
      }

      // Step 3: Create new version with old content (revert)
      const revertResponse = await this.axiosInstance.post(
        `${projectPath}/revert`,
        {
          targetVersion: request.targetVersion,
          comment: request.comment || `Revert to version ${request.targetVersion}`,
        }
      );

      return {
        success: true,
        newVersion: revertResponse.data.version,
        message: `Successfully reverted to version ${request.targetVersion}. New version: ${revertResponse.data.version}`,
      };
    } catch (error: unknown) {
      return {
        success: false,
        message: `Failed to revert: ${sanitizeError(error)}`,
      };
    }
  }

  // =============================================================================
  // Phase 2: Git Version History Methods
  // =============================================================================

  /**
   * Parse commit type from comment
   *
   * @param comment - Commit comment
   * @returns Commit type
   */
  private parseCommitType(comment?: string): Types.CommitType {
    if (!comment) return "SAVE";
    if (comment.includes("Type: ARCHIVE")) return "ARCHIVE";
    if (comment.includes("Type: RESTORE")) return "RESTORE";
    if (comment.includes("Type: ERASE")) return "ERASE";
    if (comment.includes("Type: MERGE")) return "MERGE";
    return "SAVE";
  }

  /**
   * Get Git commit history for a specific file
   *
   * @param request - File history request
   * @returns File commit history with pagination
   */
  async getFileHistory(request: Types.GetFileHistoryRequest): Promise<Types.GetFileHistoryResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    const response = await this.axiosInstance.get(
      `${projectPath}/files/${encodeURIComponent(request.filePath)}/history`,
      {
        params: {
          limit: request.limit || 50,
          offset: request.offset || 0,
        },
      }
    );

    const commits = response.data.commits?.map((fileData: Types.FileData) => ({
      commitHash: fileData.version || "",
      author: fileData.author || { name: "unknown", email: "" },
      timestamp: fileData.modifiedAt || new Date().toISOString(),
      comment: fileData.comment || "",
      commitType: this.parseCommitType(fileData.comment),
      size: fileData.size,
    })) || [];

    return {
      filePath: request.filePath,
      commits,
      total: response.data.total || commits.length,
      hasMore: (request.offset || 0) + commits.length < (response.data.total || commits.length),
    };
  }

  /**
   * Get Git commit history for entire project
   *
   * @param request - Project history request
   * @returns Project commit history with pagination
   */
  async getProjectHistory(request: Types.GetProjectHistoryRequest): Promise<Types.GetProjectHistoryResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    const response = await this.axiosInstance.get(
      `${projectPath}/history`,
      {
        params: {
          limit: request.limit || 50,
          offset: request.offset || 0,
          branch: request.branch,
        },
      }
    );

    const commits = response.data.commits?.map((commit: any) => ({
      commitHash: commit.version || "",
      author: commit.author || { name: "unknown", email: "" },
      timestamp: commit.modifiedAt || new Date().toISOString(),
      comment: commit.comment || "",
      commitType: this.parseCommitType(commit.comment),
      filesChanged: commit.filesChanged || 0,
      tablesChanged: commit.tablesChanged,
    })) || [];

    return {
      projectId: request.projectId,
      branch: response.data.branch || request.branch || "main",
      commits,
      total: response.data.total || commits.length,
      hasMore: (request.offset || 0) + commits.length < (response.data.total || commits.length),
    };
  }

  // =============================================================================
  // Phase 3: Dimension Properties Methods
  // =============================================================================

  /**
   * Extract file name pattern from rules.xml content
   *
   * @param xmlContent - XML content from rules.xml
   * @returns File name pattern or null
   */
  private extractFileNamePattern(xmlContent: string): string | null {
    const match = xmlContent.match(/<properties-file-name-pattern>(.*?)<\/properties-file-name-pattern>/);
    return match ? match[1] : null;
  }

  /**
   * Extract property names from file name pattern
   *
   * @param pattern - File name pattern
   * @returns Array of property names
   */
  private extractPropertiesFromPattern(pattern: string | null): string[] {
    if (!pattern) return [];
    const matches = pattern.match(/%([^:%]+)(?::[^%]+)?%/g);
    if (!matches) return [];
    return matches.map(m => m.replace(/%([^:%]+)(?::[^%]+)?%/, "$1"));
  }

  /**
   * Get dimension properties file naming pattern
   *
   * @param request - Get file name pattern request
   * @returns File name pattern and extracted properties
   */
  async getFileNamePattern(request: Types.GetFileNamePatternRequest): Promise<Types.GetFileNamePatternResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    const response = await this.axiosInstance.get(
      `${projectPath}/rules.xml`
    );

    const pattern = this.extractFileNamePattern(response.data);
    const properties = this.extractPropertiesFromPattern(pattern);

    return {
      pattern,
      properties,
    };
  }

  /**
   * Set dimension properties file naming pattern
   *
   * @param request - Set file name pattern request
   * @returns Success status and pattern
   */
  async setFileNamePattern(request: Types.SetFileNamePatternRequest): Promise<Types.SetFileNamePatternResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    await this.axiosInstance.put(
      `${projectPath}/rules.xml/pattern`,
      { pattern: request.pattern }
    );

    return {
      success: true,
      pattern: request.pattern,
      message: "File name pattern updated successfully",
    };
  }

  /**
   * Get dimension properties for a table
   *
   * @param request - Get table properties request
   * @returns Table properties
   */
  async getTableProperties(request: Types.GetTablePropertiesRequest): Promise<Types.GetTablePropertiesResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    const response = await this.axiosInstance.get(
      `${projectPath}/tables/${encodeURIComponent(request.tableId)}/properties`
    );

    return {
      tableId: request.tableId,
      tableName: response.data.name || "",
      properties: response.data.properties || {},
    };
  }

  /**
   * Set dimension properties for a table
   *
   * @param request - Set table properties request
   * @returns Success status and properties
   */
  async setTableProperties(request: Types.SetTablePropertiesRequest): Promise<Types.SetTablePropertiesResult> {
    const projectPath = this.buildProjectPath(request.projectId);

    await this.axiosInstance.put(
      `${projectPath}/tables/${encodeURIComponent(request.tableId)}/properties`,
      {
        properties: request.properties,
        comment: request.comment || "Update table dimension properties",
      }
    );

    return {
      success: true,
      tableId: request.tableId,
      properties: request.properties,
      message: "Table properties updated successfully",
    };
  }
}
