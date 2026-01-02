/**
 * OpenL Tablets API Client
 *
 * Provides a high-level interface for interacting with OpenL Tablets WebStudio REST API.
 * Handles all HTTP communication, error handling, and response parsing.
 */

import axios, { AxiosInstance } from "axios";
import * as Types from "./types.js";
import { AuthenticationManager } from "./auth.js";
import { DEFAULTS, PROJECT_ID_PATTERN, TEST_POLLING } from "./constants.js";
import { validateTimeout, sanitizeError, parseProjectId as parseProjectIdUtil } from "./utils.js";

/**
 * Check if test execution logging is enabled (via environment variable)
 */
const DEBUG_TESTS = process.env.DEBUG_TESTS === "true" || process.env.DEBUG === "true";

/**
 * Client for OpenL Tablets WebStudio REST API
 *
 * Usage:
 * ```typescript
 * const client = new OpenLClient({
 *   baseUrl: "http://localhost:8080/rest",
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
  private repositoriesCache: Types.Repository[] | null = null;

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
   * @param useCache - Whether to use cached repositories (default: true)
   * @returns Array of repository information
   */
  async listRepositories(useCache: boolean = true): Promise<Types.Repository[]> {
    if (useCache && this.repositoriesCache !== null) {
      return this.repositoriesCache;
    }
    
    const response = await this.axiosInstance.get<Types.Repository[]>(
      "/repos"
    );
    this.repositoriesCache = response.data;
    return response.data;
  }

  /**
   * Map repository name to repository ID
   * 
   * This function allows users to work with repository names (user-friendly)
   * while the server uses repository IDs internally for API calls.
   * 
   * @param repositoryName - Repository name (e.g., "Design Repository")
   * @returns Repository ID (e.g., "design-repo")
   * @throws Error if repository name not found
   */
  async getRepositoryIdByName(repositoryName: string): Promise<string> {
    const repositories = await this.listRepositories();
    const repository = repositories.find(r => r.name === repositoryName);
    
    if (!repository) {
      const availableNames = repositories.map(r => r.name).join(", ");
      throw new Error(
        `Repository with name "${repositoryName}" not found. ` +
        `Available repositories: ${availableNames || "none"}. ` +
        `Use openl_list_repositories() to see all available repositories.`
      );
    }
    
    return repository.id;
  }

  /**
   * Map repository ID to repository name
   * 
   * @param repositoryId - Repository ID (e.g., "design-repo")
   * @returns Repository name (e.g., "Design Repository")
   * @throws Error if repository ID not found
   */
  async getRepositoryNameById(repositoryId: string): Promise<string> {
    const repositories = await this.listRepositories();
    const repository = repositories.find(r => r.id === repositoryId);
    
    if (!repository) {
      const availableIds = repositories.map(r => r.id).join(", ");
      throw new Error(
        `Repository with ID "${repositoryId}" not found. ` +
        `Available repository IDs: ${availableIds || "none"}. ` +
        `Use openl_list_repositories() to see all available repositories.`
      );
    }
    
    return repository.name;
  }

  /**
   * Clear repositories cache (useful after repository changes)
   */
  clearRepositoriesCache(): void {
    this.repositoriesCache = null;
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

  /**
   * Get repository features (branching support, searchable, etc.)
   *
   * @param repository - Repository ID
   * @returns Repository features
   */
  async getRepositoryFeatures(repository: string): Promise<Types.RepositoryFeatures> {
    const response = await this.axiosInstance.get<Types.RepositoryFeatures>(
      `/repos/${encodeURIComponent(repository)}/features`
    );
    return response.data;
  }

  /**
   * List deployment repositories
   *
   * @param useCache - Whether to use cached repositories (default: true)
   * @returns Array of deployment repository information
   */
  async listDeployRepositories(useCache: boolean = true): Promise<Types.Repository[]> {
    // Note: We could cache this separately, but for simplicity, we'll fetch each time
    // since deployment repositories change less frequently
    const response = await this.axiosInstance.get<Types.Repository[]>(
      "/production-repos"
    );
    return response.data;
  }

  /**
   * Map production repository name to repository ID
   * 
   * This function allows users to work with production repository names (user-friendly)
   * while the server uses repository IDs internally for API calls.
   * 
   * @param repositoryName - Production repository name (e.g., "Production Deployment")
   * @returns Repository ID (e.g., "production-deploy")
   * @throws Error if repository name not found
   */
  async getProductionRepositoryIdByName(repositoryName: string): Promise<string> {
    const repositories = await this.listDeployRepositories();
    const repository = repositories.find(r => r.name === repositoryName);
    
    if (!repository) {
      const availableNames = repositories.map(r => r.name).join(", ");
      throw new Error(
        `Production repository with name "${repositoryName}" not found. ` +
        `Available production repositories: ${availableNames || "none"}. ` +
        `Use openl_list_deploy_repositories() to see all available production repositories.`
      );
    }
    
    return repository.id;
  }

  /**
   * Get project revision history from repository
   *
   * @param repository - Repository ID
   * @param projectName - Project name
   * @param options - Query options (branch, search, pagination, etc.)
   * @returns Paginated project revisions
   */
  async getProjectRevisions(
    repository: string,
    projectName: string,
    options?: {
      branch?: string;
      search?: string;
      techRevs?: boolean;
      page?: number;
      size?: number;
    }
  ): Promise<Types.PageResponse<Types.ProjectRevision>> {
    const params: Record<string, string | number | boolean> = {};
    if (options?.branch) params.branch = options.branch;
    if (options?.search) params.search = options.search;
    if (options?.techRevs !== undefined) params.techRevs = options.techRevs;
    if (options?.page !== undefined) params.page = options.page;
    if (options?.size !== undefined) params.size = options.size;

    const url = options?.branch
      ? `/repos/${encodeURIComponent(repository)}/branches/${encodeURIComponent(options.branch)}/projects/${encodeURIComponent(projectName)}/history`
      : `/repos/${encodeURIComponent(repository)}/projects/${encodeURIComponent(projectName)}/history`;

    const response = await this.axiosInstance.get<Types.PageResponse<Types.ProjectRevision>>(
      url,
      { params }
    );
    return response.data;
  }

  // =============================================================================
  // Project Management
  // =============================================================================

  /**
   * Parse a project ID into repository and project name
   *
   * Accepts multiple formats for user convenience:
   * 1. Base64 format (default): "ZGVzaWduOTpTYW1wbGUgUHJvamVjdDpjY2VkYzY5MmJlZWM5YmNmYTdiZmFiOTZmNzZmYTNhZjU0MTk4MjFkM2M5NDVkYTlmN2VjNzZjNmNkMDhlMDQ0" (from OpenL 6.0.0+ API, format: base64("repository:projectName:hashCode"))
   * 2. Dash format: "repository-projectName" (backward compatibility, user-friendly)
   * 3. Colon format: "repository:projectName" (backward compatibility, decoded base64)
   *
   * @param projectId - Project ID in any supported format
   * @returns Tuple of [repository, projectName]
   * @throws Error if project ID format is invalid
   */
  private parseProjectId(projectId: string): [string, string] {
    // First, try base64 format (primary format: repository:projectName:hashCode)
    if (PROJECT_ID_PATTERN.test(projectId.replace(/\s/g, ""))) {
      try {
        const parsed = parseProjectIdUtil(projectId);
        return [parsed.repository, parsed.projectName];
      } catch (error) {
        // If base64 decode fails, fall through to try other formats
      }
    }

    // Try dash format (user-friendly format from list_projects, backward compatibility)
    // Pattern: repository-projectName
    const dashPattern = /^([^-]+)-(.+)$/;
    const dashMatch = projectId.match(dashPattern);
    if (dashMatch) {
      return [dashMatch[1], dashMatch[2]];
    }

    // Try colon format (repository:projectName, backward compatibility)
    if (projectId.includes(':')) {
      try {
        const parsed = parseProjectIdUtil(projectId);
        return [parsed.repository, parsed.projectName];
      } catch (error) {
        // Fall through to error
      }
    }

    // If all formats fail, throw error
    throw new Error(
      `Invalid project ID format: ${projectId}. Expected formats:\n` +
      `  - Base64-encoded string (e.g., "ZGVzaWduOTpTYW1wbGUgUHJvamVjdDpjY2VkYzY5MmJlZWM5YmNmYTdiZmFiOTZmNzZmYTNhZjU0MTk4MjFkM2M5NDVkYTlmN2VjNzZjNmNkMDhlMDQ0")\n` +
      `  - "repository-projectName" (e.g., "design-Example 1 - Bank Rating") - backward compatibility\n` +
      `  - "repository:projectName" (e.g., "design:Example 1 - Bank Rating") - backward compatibility\n\n` +
      `To discover valid project IDs, use: openl_list_projects()`
    );
  }

  /**
   * Convert project ID to base64-encoded format
   *
   * OpenL 6.0.0+ uses base64-encoded IDs in URL paths.
   * Format: base64("repository:projectName")
   *
   * @param projectId - Project ID in any format
   * @returns Base64-encoded project ID
   * @throws Error if projectId format is invalid
   */
  private toBase64ProjectId(projectId: string): string {
    // If projectId is URL-encoded (contains %), decode it first
    let decodedId = projectId;
    if (projectId.includes('%')) {
      try {
        decodedId = decodeURIComponent(projectId);
      } catch (error) {
        // If decoding fails, use original - might not be URL-encoded
        decodedId = projectId;
      }
    }

    // If contains '-' or ':', definitely needs parsing and encoding
    if (decodedId.includes('-') || decodedId.includes(':')) {
      const [repository, projectName] = this.parseProjectId(decodedId);
      const colonFormat = `${repository}:${projectName}`;
      return Buffer.from(colonFormat, 'utf-8').toString('base64');
    }

    // No dash or colon - might be already base64, but validate explicitly
    // First check if it matches base64 pattern
    const normalizedId = decodedId.replace(/\s/g, "");
    if (!PROJECT_ID_PATTERN.test(normalizedId)) {
      throw new Error(
        `Invalid project ID format: "${projectId}" does not match base64 pattern. ` +
        `Expected formats: base64-encoded string, "repository-projectName", or "repository:projectName"`
      );
    }

    // Validate by attempting decode/re-encode round-trip
    try {
      const decoded = Buffer.from(normalizedId, "base64").toString("utf-8");
      
      // Check if decoded string has expected format (repository:projectName or repository:projectName:hashCode)
      const parts = decoded.split(":");
      if (parts.length < 2 || !parts[0] || !parts[1]) {
        throw new Error(`Decoded base64 does not have expected format: "${decoded}"`);
      }

      // Verify round-trip: re-encode and compare (but normalize padding)
      const reEncoded = Buffer.from(decoded, "utf-8").toString("base64");
      // Base64 padding can vary, so compare decoded values instead of encoded strings
      const reDecoded = Buffer.from(reEncoded, "base64").toString("utf-8");
      if (reDecoded !== decoded) {
        throw new Error(`Base64 round-trip validation failed for: "${projectId}"`);
      }

      // Valid base64 - return normalized version (preserve original padding if it was valid)
      // Use the original normalizedId to preserve any padding characters
      return normalizedId;
    } catch (error) {
      // If decode fails or validation fails, throw descriptive error
      throw new Error(
        `Invalid base64 project ID: "${projectId}". ` +
        `Validation failed: ${error instanceof Error ? error.message : String(error)}. ` +
        `Expected formats: base64-encoded string, "repository-projectName", or "repository:projectName"`
      );
    }
  }

  /**
   * Build URL-safe project path for OpenL 6.0.0+ API
   *
   * OpenL 6.0.0+ changed the API structure to use base64-encoded project IDs:
   * - Old: /repos/{repository}/projects/{projectName}
   * - New: /projects/{base64-id}
   *
   * @param projectId - Project ID in any format
   * @returns URL-encoded project path
   */
  private buildProjectPath(projectId: string): string {
    // If projectId is already URL-encoded (contains %), decode it first to get clean base64
    // Then re-encode it properly for URL
    let cleanBase64Id: string;
    if (projectId.includes('%')) {
      // URL-encoded: decode first, then normalize
      try {
        const decoded = decodeURIComponent(projectId);
        cleanBase64Id = this.toBase64ProjectId(decoded);
      } catch (error) {
        // If decoding fails, try treating as non-encoded
        cleanBase64Id = this.toBase64ProjectId(projectId);
      }
    } else {
      // Not URL-encoded: convert to base64 if needed
      cleanBase64Id = this.toBase64ProjectId(projectId);
    }
    
    if (DEBUG_TESTS) {
      console.error(`[Tests] buildProjectPath:`);
      console.error(`[Tests]   Input projectId: ${projectId.substring(0, 50)}...`);
      console.error(`[Tests]   Clean base64Id: ${cleanBase64Id.substring(0, 50)}...`);
      console.error(`[Tests]   Base64Id ends with: ${cleanBase64Id.slice(-5)}`);
    }
    
    // Always URL-encode the final base64 string for URL safety
    // This ensures padding characters (=) are properly encoded as %3D
    const encodedPath = `/projects/${encodeURIComponent(cleanBase64Id)}`;
    
    if (DEBUG_TESTS) {
      console.error(`[Tests]   Final path: ${encodedPath.substring(0, 80)}...`);
    }
    
    return encodedPath;
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
    // Build query parameters, handling tags with 'tags.' prefix
    const params: Record<string, string> = {};
    if (filters?.repository) params.repository = filters.repository;
    if (filters?.status) params.status = filters.status;
    if (filters?.tags) {
      // Tags must be prefixed with 'tags.' in query string
      Object.entries(filters.tags).forEach(([key, value]) => {
        params[`tags.${key}`] = value;
      });
    }
    
    const response = await this.axiosInstance.get<Types.ProjectSummary[] | { content?: Types.ProjectSummary[]; data?: Types.ProjectSummary[] }>(
      "/projects",
      { params }
    );
    
    // Handle different response formats:
    // 1. Direct array: [...]
    // 2. Paginated response: { content: [...], pageable: {...} }
    // 3. Wrapped response: { data: [...] }
    const responseData = response.data;
    if (Array.isArray(responseData)) {
      return responseData;
    } else if (responseData && typeof responseData === 'object') {
      if ('content' in responseData && Array.isArray(responseData.content)) {
        return responseData.content;
      } else if ('data' in responseData && Array.isArray(responseData.data)) {
        return responseData.data;
      }
    }
    
    // Fallback: return empty array if format is unexpected
    return [];
  }

  /**
   * Get project details by ID
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @returns Project details
   */
  async getProject(projectId: string): Promise<Types.ComprehensiveProject> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.Project>(projectPath);
    return response.data as Types.ComprehensiveProject;
  }

  /**
   * Delete a project
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @returns void (204 No Content on success)
   */
  async deleteProject(projectId: string): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.delete(projectPath);
    // Returns 204 No Content
  }

  /**
   * Open a project for viewing/editing
   *
   * Updates project status to OPENED using PATCH /projects/{projectId}
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param options - Optional branch, revision, and comment
   * @returns Success status (204 No Content on success)
   */
  async openProject(
    projectId: string,
    options?: { branch?: string; revision?: string; comment?: string; selectedBranches?: string[] }
  ): Promise<boolean> {
    const projectPath = this.buildProjectPath(projectId);
    const updateModel: Types.ProjectStatusUpdateModel = {
      status: "OPENED",
      ...options,
    };

    await this.axiosInstance.patch(projectPath, updateModel);
    return true;
  }

  /**
   * Close an open project
   *
   * Updates project status to CLOSED using PATCH /projects/{projectId}
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param comment - Optional comment describing why the project is being closed
   * @returns Success status (204 No Content on success)
   */
  async closeProject(projectId: string, comment?: string): Promise<boolean> {
    const projectPath = this.buildProjectPath(projectId);
    const updateModel: Types.ProjectStatusUpdateModel = {
      status: "CLOSED",
      comment,
    };

    await this.axiosInstance.patch(projectPath, updateModel);
    return true;
  }

  /**
   * Update project status with safety checks for unsaved changes
   *
   * Unified method to handle all project status transitions (open, close, save, etc.)
   * Prevents accidental data loss by requiring explicit confirmation when closing
   * projects with unsaved changes.
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param request - Status update request with optional fields
   * @returns Success status (204 No Content on success)
   * @throws Error if trying to close EDITING project without save or explicit discard
   */
  async updateProjectStatus(
    projectId: string,
    request: {
      status?: "LOCAL" | "ARCHIVED" | "OPENED" | "VIEWING_VERSION" | "EDITING" | "CLOSED";
      comment?: string;
      discardChanges?: boolean;
      branch?: string;
      revision?: string;
      selectedBranches?: string[];
    }
  ): Promise<{ success: boolean; message: string }> {
    const projectPath = this.buildProjectPath(projectId);

    // SAFETY CHECK: Prevent closing with unsaved changes without explicit confirmation
    if (request.status === "CLOSED") {
      // Fetch current project state to check for unsaved changes
      const currentProject = await this.getProject(projectId);

      if (currentProject.status === "EDITING") {
        // Project has unsaved changes
        if (!request.comment && !request.discardChanges) {
          throw new Error(
            "Cannot close project with unsaved changes. " +
            "Options:\n" +
            "1. Provide 'comment' to save changes before closing: {status: 'CLOSED', comment: 'your message'}\n" +
            "2. Set 'discardChanges: true' to explicitly discard unsaved changes: {status: 'CLOSED', discardChanges: true}"
          );
        }
      }
    }

    // Build the API request (discardChanges is MCP-only, not sent to API)
    const updateModel: Types.ProjectStatusUpdateModel = {
      status: request.status,
      comment: request.comment,
      branch: request.branch,
      revision: request.revision,
      selectedBranches: request.selectedBranches,
    };

    // Call the OpenL Studio API
    await this.axiosInstance.patch(projectPath, updateModel);

    // Build success message based on what happened
    let message = "Project status updated successfully";
    if (request.status === "CLOSED" && request.comment) {
      message = "Project saved and closed successfully";
    } else if (request.status === "CLOSED" && request.discardChanges) {
      message = "Project closed (changes discarded)";
    } else if (request.status === "OPENED") {
      message = "Project opened successfully";
    } else if (request.comment && !request.status) {
      message = "Project changes saved successfully";
    }

    return { success: true, message };
  }

  /**
   * Save project changes, creating a new version in the repository
   * This method validates the project before saving
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
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
      message: `Project saved successfully at commit ${(commitHash && commitHash.substring(0, 8)) || "unknown"}`,
    };
  }

  // =============================================================================
  // File Management
  // =============================================================================

  /**
   * Upload an Excel file with rules to a project
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
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

    try {
      // Upload file using axios with buffer
      // IMPORTANT: Use the fileName exactly as provided - can be simple name, subdirectory path, or full path
      // e.g., "Rules.xlsx", "rules/Premium.xlsx", or "Example 1 - Bank Rating/Bank Rating.xlsx"
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

      // Extract file metadata from response (FileData structure)
      // Note: The file is uploaded to workspace but NOT committed to Git yet
      const fileData = response.data || {};
      const version = fileData.version || fileData.commitHash;

      return {
        success: true,
        fileName,
        commitHash: version,  // Not actually a commit hash yet - file is in workspace
        version,
        author: fileData.author ? {
          name: fileData.author.name || "unknown",
          email: fileData.author.email || ""
        } : undefined,
        timestamp: fileData.modifiedAt || new Date().toISOString(),
        size: fileData.size || buffer.length,
        message: `File uploaded successfully to workspace. Use update_project_status to save changes to Git.`,
      };
    } catch (error: any) {
      // Provide helpful error messages for common upload failures
      if (error.response && error.response.status === 404) {
        throw new Error(
          `Upload failed: Invalid path "${fileName}" in project "${projectId}". ` +
          `Ensure the project is open and the file path is valid. ` +
          `Valid formats: simple name ('Rules.xlsx'), subdirectory ('rules/Premium.xlsx'), or full path ('Example 1 - Bank Rating/Bank Rating.xlsx'). ` +
          `To verify project exists and is open, use: openl_get_project(projectId: "${projectId}")`
        );
      }
      if (error.response && error.response.status === 409) {
        throw new Error(
          `Upload failed: Conflict detected for "${fileName}". ` +
          `The file may be locked by another user or there may be uncommitted changes. ` +
          `Try opening the project first or resolving any conflicts.`
        );
      }
      // Re-throw other errors with additional context
      throw new Error(`Upload failed for "${fileName}": ${error.message}`);
    }
  }

  /**
   * Download an Excel file from a project
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param fileName - Name of the file to download (use the exact 'file' value from list_tables response)
   * @param version - Optional Git commit hash to download specific version
   * @returns File content as Buffer
   * @throws Error with helpful message if file not found (404)
   */
  async downloadFile(projectId: string, fileName: string, version?: string): Promise<Buffer> {
    const projectPath = this.buildProjectPath(projectId);

    // Parse project ID to get the project name (without repository prefix)
    const [, projectName] = this.parseProjectId(projectId);

    // Build request params
    const params: any = {};
    if (version) {
      params.version = version;  // Git commit hash
    }

    // IMPORTANT: list_tables returns file paths like "Example 2 - Corporate Rating/Corporate Rating.xlsx"
    // The OpenL API expects the full path AS-IS from list_tables, including the project directory.
    // We'll try multiple variations to handle different scenarios.

    const projectPrefix = `${projectName}/`;
    const pathsToTry: string[] = [];

    // Try the fileName exactly as provided first
    pathsToTry.push(fileName);

    // If fileName doesn't have the project prefix, try adding it
    if (!fileName.startsWith(projectPrefix) && !fileName.includes('/')) {
      // User might have passed just the base filename like "Corporate Rating.xlsx"
      // Try with the project name prefix: "Example 2 - Corporate Rating/Corporate Rating.xlsx"
      pathsToTry.push(`${projectPrefix}${fileName}`);
    }

    let lastError: any;

    // Try each path until one works
    for (const pathToTry of pathsToTry) {
      try {
        // Encode each path segment separately to preserve directory structure
        // Don't encode forward slashes within the path
        const encodedPath = pathToTry.split('/').map(encodeURIComponent).join('/');

        const response = await this.axiosInstance.get<ArrayBuffer>(
          `${projectPath}/files/${encodedPath}`,
          {
            responseType: "arraybuffer",
            params,
          }
        );

        return Buffer.from(response.data);
      } catch (error: any) {
        lastError = error;
        // If not a 404, don't try other paths
        if (error.response && error.response.status !== 404) {
          break;
        }
        // Continue to next path on 404
      }
    }

    // All paths failed, provide helpful error message
    if (lastError && lastError.response && lastError.response.status === 404) {
      throw new Error(
        `File not found: "${fileName}". ` +
        `Tried paths: ${pathsToTry.map(p => `"${p}"`).join(", ")}. ` +
        `The file does not exist in project "${projectId}". ` +
        `To find available files: 1) Call list_tables(projectId="${projectId}") to see all tables and their file paths, ` +
        `2) Use the exact 'file' field value from a table entry as the fileName parameter. ` +
        `Common causes: File path typo, wrong project, or file was deleted.`
      );
    } else if (lastError && lastError.response && lastError.response.status === 400) {
      throw new Error(
        `Invalid file path: "${fileName}". ` +
        `The OpenL API rejected this file path (400 Bad Request). ` +
        `You must use the exact 'file' field value from list_tables() response, including any directory prefix. ` +
        `For example, if list_tables shows "Example 2 - Corporate Rating/Corporate Rating.xlsx", use that full path. ` +
        `Original error: ${lastError.message}`
      );
    }

    // Re-throw other errors
    throw lastError;
  }

  /**
   * Create a new branch in a project
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param branchName - Name for the new branch
   * @param revision - Optional Git revision to branch from
   * @returns Success status
   */
  async createBranch(
    projectId: string,
    branchName: string,
    revision?: string
  ): Promise<boolean> {
    const projectPath = this.buildProjectPath(projectId);
    const request: Types.BranchCreateRequest = {
      branch: branchName,
      revision,
    };
    await this.axiosInstance.post(
      `${projectPath}/branches`,
      request
    );
    return true;
  }

  // =============================================================================
  // Rules (Tables) Management
  // =============================================================================

  /**
   * List all tables/rules in a project with optional filters
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param filters - Optional filters for table type, name, and file
   * @returns Array of table metadata
   */
  async listTables(
    projectId: string,
    filters?: Types.TableFilters
  ): Promise<Types.TableMetadata[]> {
    const projectPath = this.buildProjectPath(projectId);
    
    // Build query parameters, handling kind (array) and properties with 'properties.' prefix
    const params: Record<string, string | string[]> = {};
    if (filters?.kind && filters.kind.length > 0) {
      // API expects 'kind' as array parameter
      params.kind = filters.kind;
    }
    if (filters?.name) params.name = filters.name;
    if (filters?.properties) {
      // Properties must be prefixed with 'properties.' in query string
      Object.entries(filters.properties).forEach(([key, value]) => {
        params[`properties.${key}`] = value;
      });
    }
    
    const response = await this.axiosInstance.get<Types.TableMetadata[]>(
      `${projectPath}/tables`,
      { params }
    );
    return response.data;
  }

  /**
   * Create a new rule/table in a project
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
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
      const errorMsg = sanitizeError(error);

      // Check if this is a 405 Method Not Allowed error
      if (errorMsg.includes('405')) {
        return {
          success: false,
          message: `Table creation via REST API is not supported in OpenL Tablets 6.0.0. ` +
                  `Tables must be created by uploading/modifying Excel files directly. ` +
                  `Use upload_file to upload an Excel file with the table definition, or ` +
                  `use the OpenL WebStudio UI to create tables interactively.`,
        };
      }

      return {
        success: false,
        message: `Failed to create ${request.tableType} table '${request.name}': ${errorMsg}`,
      };
    }
  }

  /**
   * Get detailed table data and structure
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
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
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param tableId - Table identifier
   * @param view - Updated table view with modifications (MUST include full table structure from get_table)
   * @param comment - Optional comment describing the changes (NOTE: not supported by OpenAPI schema, will be ignored)
   * @returns void (204 No Content on success)
   * @throws Error if view is missing required fields
   */
  async updateTable(
    projectId: string,
    tableId: string,
    view: Types.EditableTableView,
    comment?: string
  ): Promise<void> {
    // Validate that view contains required fields
    // OpenL API requires the FULL table structure, not just modified fields
    const requiredFields = ['id', 'tableType', 'kind', 'name'];
    const missingFields = requiredFields.filter(field => !(field in view));

    if (missingFields.length > 0) {
      throw new Error(
        `Invalid table view: missing required fields: ${missingFields.join(', ')}. ` +
        `The view parameter must contain the FULL table structure from get_table(), not just the modified fields. ` +
        `Workflow: 1) Call get_table() to retrieve current structure, 2) Modify the returned object, 3) Pass the complete object to update_table().`
      );
    }

    // Validate tableId matches view.id
    if (view.id !== tableId) {
      throw new Error(
        `Table ID mismatch: tableId parameter is "${tableId}" but view.id is "${view.id}". ` +
        `These must match. Use the same ID from get_table() for both parameters.`
      );
    }

    const projectPath = this.buildProjectPath(projectId);
    // OpenAPI schema expects EditableTableView directly as request body
    // Comment parameter is not supported by the schema
    await this.axiosInstance.put(
      `${projectPath}/tables/${encodeURIComponent(tableId)}`,
      view
    );
    // Returns 204 No Content
  }

  /**
   * Append lines to a project table
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @param tableId - Table identifier
   * @param appendData - Data to append with fields and table type
   * @returns void (200 OK on success per schema)
   */
  async appendProjectTable(
    projectId: string,
    tableId: string,
    appendData: Types.AppendTableView
  ): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(
      `${projectPath}/tables/${encodeURIComponent(tableId)}/lines`,
      appendData
    );
  }

  // =============================================================================
  // Deployment Management
  // =============================================================================

  /**
   * List all deployments with optional repository filter
   *
   * @param repository - Optional repository ID to filter deployments
   * @returns Array of deployment information
   */
  async listDeployments(repository?: string): Promise<Types.DeploymentViewModel_Short[]> {
    const response = await this.axiosInstance.get<Types.DeploymentViewModel_Short[]>(
      "/deployments",
      { params: repository ? { repository } : undefined }
    );
    return response.data;
  }

  /**
   * Deploy a project to production repository
   *
   * @param request - Deployment request with project ID, deployment name, and target repository
   * @returns Success status (204 No Content on success)
   */
  async deployProject(request: Types.DeployProjectRequest): Promise<void> {
    // Ensure projectId is in base64 format
    const base64ProjectId = this.toBase64ProjectId(request.projectId);

    await this.axiosInstance.post(
      "/deployments",
      {
        projectId: base64ProjectId,
        deploymentName: request.deploymentName,
        productionRepositoryId: request.productionRepositoryId,
        comment: request.comment,
      }
    );
  }

  /**
   * Redeploy an existing deployment
   *
   * @param deploymentId - Deployment ID to redeploy
   * @param request - Redeploy request with project ID and optional comment
   * @returns Success status (204 No Content on success)
   */
  async redeployProject(
    deploymentId: string,
    request: Types.RedeployProjectRequest
  ): Promise<void> {
    // Ensure projectId is in base64 format
    const base64ProjectId = this.toBase64ProjectId(request.projectId);

    await this.axiosInstance.post(
      `/deployments/${encodeURIComponent(deploymentId)}`,
      {
        projectId: base64ProjectId,
        comment: request.comment,
      }
    );
  }

  /**
   * Get project local changes (workspace history)
   *
   * @param projectId - Project ID
   * @returns List of local change history items
   */
  async getProjectLocalChanges(projectId: string): Promise<Types.ProjectHistoryItem[]> {
    // Note: This endpoint requires the project to be loaded in WebStudio session
    // The endpoint is /history/project and uses session-based project context
    const response = await this.axiosInstance.get<Types.ProjectHistoryItem[]>(
      "/history/project"
    );
    return response.data;
  }

  /**
   * Restore project to a local change version
   *
   * @param projectId - Project ID
   * @param historyId - History ID to restore
   * @returns Success status (204 No Content on success)
   */
  async restoreProjectLocalChange(projectId: string, historyId: string): Promise<void> {
    // Note: This endpoint requires the project to be loaded in WebStudio session
    // The endpoint is /history/restore and uses session-based project context
    await this.axiosInstance.post(
      "/history/restore",
      historyId,
      {
        headers: {
          "Content-Type": "text/plain",
        },
      }
    );
  }

  /**
   * Run project tests - unified method that starts tests and retrieves results
   *
   * @param projectId - Project ID
   * @param options - Test execution options (tableId, testRanges, query, pagination, waitForCompletion)
   * @returns Test execution summary
   * @throws Error if test execution fails
   */
  async runProjectTests(
    projectId: string,
    options?: {
      tableId?: string;
      testRanges?: string;
      query?: {
        failuresOnly?: boolean;
      };
      pagination?: {
        offset?: number;
        limit?: number;
      };
      waitForCompletion?: boolean;
    }
  ): Promise<Types.TestsExecutionSummary> {
    const projectPath = this.buildProjectPath(projectId);

    // Start test execution (returns 202 Accepted)
    const params: Record<string, string | number | boolean> = {};
    if (options?.tableId) params.fromModule = options.tableId;
    if (options?.testRanges) params.testRanges = options.testRanges;

    if (DEBUG_TESTS) {
      console.error(`[Tests] ========================================`);
      console.error(`[Tests] Starting test execution:`);
      console.error(`[Tests]   Project: ${projectId}`);
      console.error(`[Tests]   Endpoint: POST ${projectPath}/tests/run`);
      if (options?.tableId) console.error(`[Tests]   TableId: ${options.tableId}`);
      if (options?.testRanges) console.error(`[Tests]   TestRanges: ${options.testRanges}`);
      console.error(`[Tests]   Params:`, params);
    }

    // Start tests and capture all response headers
    const startResponse = await this.axiosInstance.post(
      `${projectPath}/tests/run`,
      undefined,
      { params }
    );

    if (DEBUG_TESTS) {
      console.error(`[Tests] Test execution started:`);
      console.error(`[Tests]   Status: ${startResponse.status} ${startResponse.statusText}`);
      console.error(`[Tests]   Response headers:`, Object.keys(startResponse.headers || {}));
    }

    // Extract all headers from the start response
    // Exclude standard response headers that shouldn't be forwarded to requests
    const responseHeaders: Record<string, string> = {};
    if (startResponse.headers) {
      // Headers that should NOT be forwarded (standard HTTP response headers)
      const excludeHeaders = [
        'content-type',
        'content-length',
        'content-encoding',
        'transfer-encoding',
        'connection',
        'server',
        'date',
        'etag',
        'last-modified',
        'cache-control',
        'expires',
        'vary',
        'access-control-allow-origin',
        'access-control-allow-methods',
        'access-control-allow-headers',
        'access-control-expose-headers',
      ];
      
      Object.keys(startResponse.headers).forEach((key) => {
        const lowerKey = key.toLowerCase();
        // Forward all headers except standard response headers
        // This includes custom headers (x-*), set-cookie, and any other headers
        // that the server might return for tracking test execution
        if (!excludeHeaders.includes(lowerKey)) {
          const value = startResponse.headers[key];
          if (value !== undefined && value !== null) {
            responseHeaders[key] = Array.isArray(value) ? value.join(", ") : String(value);
          }
        }
      });
    }

    if (DEBUG_TESTS) {
      console.error(`[Tests] Extracted headers to forward:`, Object.keys(responseHeaders));
    }

    // Build summary query parameters
    const summaryParams: Record<string, string | number | boolean> = {};
    if (options?.query?.failuresOnly) summaryParams.failuresOnly = true;
    if (options?.pagination?.offset !== undefined) {
      summaryParams.page = Math.floor((options.pagination.offset || 0) / (options.pagination.limit || 50));
    }
    if (options?.pagination?.limit !== undefined) summaryParams.size = options.pagination.limit;

    // If waitForCompletion is false, just return current status
    if (options?.waitForCompletion === false) {
      if (DEBUG_TESTS) {
        console.error(`[Tests] Getting test results (no wait):`);
        console.error(`[Tests]   Endpoint: GET ${projectPath}/tests/summary`);
        console.error(`[Tests]   Params:`, summaryParams);
        console.error(`[Tests]   Headers:`, Object.keys(responseHeaders));
      }

      const response = await this.axiosInstance.get<Types.TestsExecutionSummary>(
        `${projectPath}/tests/summary`,
        {
          params: summaryParams,
          headers: responseHeaders, // Use all headers from start response
        }
      );

      if (DEBUG_TESTS) {
        console.error(`[Tests] Test results received:`);
        console.error(`[Tests]   Status: ${response.status}`);
        console.error(`[Tests]   Tests: ${response.data.numberOfTests || 0}, Failures: ${response.data.numberOfFailures || 0}`);
        console.error(`[Tests] ========================================`);
      }

      return response.data;
    }

    // Otherwise, poll for test completion with exponential backoff
    const startTime = Date.now();
    let interval: number = TEST_POLLING.INITIAL_INTERVAL;
    let lastExecutionTime = 0;
    let attempts = 0;

    if (DEBUG_TESTS) {
      console.error(`[Tests] Starting polling for test completion:`);
      console.error(`[Tests]   Max retries: ${TEST_POLLING.MAX_RETRIES}`);
      console.error(`[Tests]   Timeout: ${TEST_POLLING.TIMEOUT}ms`);
      console.error(`[Tests]   Initial interval: ${TEST_POLLING.INITIAL_INTERVAL}ms`);
    }

    while (attempts < TEST_POLLING.MAX_RETRIES) {
      // Check timeout
      if (Date.now() - startTime > TEST_POLLING.TIMEOUT) {
        if (DEBUG_TESTS) {
          console.error(`[Tests] ❌ Timeout after ${TEST_POLLING.TIMEOUT}ms`);
        }
        throw new Error(
          `Test execution timed out after ${TEST_POLLING.TIMEOUT}ms. ` +
          `Tests may still be running. Check status manually or increase timeout.`
        );
      }

      // Wait before polling (except first attempt)
      if (attempts > 0) {
        if (DEBUG_TESTS) {
          console.error(`[Tests] Waiting ${interval}ms before next poll attempt...`);
        }
        await new Promise(resolve => setTimeout(resolve, interval));
        // Exponential backoff: increase interval up to MAX_INTERVAL
        interval = Math.min(interval * 1.5, TEST_POLLING.MAX_INTERVAL);
      }

      try {
        if (DEBUG_TESTS) {
          console.error(`[Tests] Poll attempt ${attempts + 1}/${TEST_POLLING.MAX_RETRIES}:`);
          console.error(`[Tests]   Endpoint: GET ${projectPath}/tests/summary`);
          console.error(`[Tests]   Params:`, summaryParams);
        }

        const response = await this.axiosInstance.get<Types.TestsExecutionSummary>(
          `${projectPath}/tests/summary`,
          {
            params: summaryParams,
            headers: responseHeaders, // Use all headers from start response
          }
        );
        const summary = response.data;

        if (DEBUG_TESTS) {
          console.error(`[Tests]   Status: ${response.status}`);
          console.error(`[Tests]   ExecutionTimeMs: ${summary.executionTimeMs || 0}`);
          console.error(`[Tests]   Tests: ${summary.numberOfTests || 0}, Failures: ${summary.numberOfFailures || 0}`);
          console.error(`[Tests]   TestCases: ${summary.testCases?.length || 0}`);
        }

        // Check if execution is complete by comparing executionTimeMs
        // If executionTimeMs has changed, tests are still running
        // If it's stable (same as last check) and we have results, assume complete
        if (summary.executionTimeMs > 0) {
          if (summary.executionTimeMs === lastExecutionTime && summary.testCases.length > 0) {
            // Execution time is stable and we have results - likely complete
            if (DEBUG_TESTS) {
              console.error(`[Tests] ✅ Tests completed (stable execution time)`);
              console.error(`[Tests] ========================================`);
            }
            return summary;
          }

          // Check if we have all expected test results
          // If numberOfTests is set and matches testCases length, execution is complete
          if (summary.numberOfTests !== undefined && summary.numberOfTests > 0) {
            const completedTests = summary.testCases.length;
            if (completedTests >= summary.numberOfTests) {
              // All tests have completed
              if (DEBUG_TESTS) {
                console.error(`[Tests] ✅ Tests completed (all ${completedTests}/${summary.numberOfTests} tests finished)`);
                console.error(`[Tests] ========================================`);
              }
              return summary;
            }
          }

          lastExecutionTime = summary.executionTimeMs;
          if (DEBUG_TESTS) {
            console.error(`[Tests]   Tests still running (execution time changed or incomplete)`);
          }
        } else if (summary.testCases.length > 0) {
          // We have results but no execution time - assume complete if we have test cases
          if (DEBUG_TESTS) {
            console.error(`[Tests] ✅ Tests completed (results available)`);
            console.error(`[Tests] ========================================`);
          }
          return summary;
        } else {
          if (DEBUG_TESTS) {
            console.error(`[Tests]   No results yet, continuing to poll...`);
          }
        }

        attempts++;
      } catch (error) {
        // If it's a 404 or other error, wait a bit and retry (tests might not be ready yet)
        if (DEBUG_TESTS) {
          const errorStatus = (error as any)?.response?.status;
          const errorMessage = (error as any)?.message;
          console.error(`[Tests]   ⚠️  Error: ${errorStatus || 'unknown'} - ${errorMessage || 'unknown error'}`);
        }
        if (attempts < 3) {
          if (DEBUG_TESTS) {
            console.error(`[Tests]   Retrying (attempt ${attempts + 1}/3)...`);
          }
          attempts++;
          continue;
        }
        if (DEBUG_TESTS) {
          console.error(`[Tests] ❌ Max retries reached, throwing error`);
          console.error(`[Tests] ========================================`);
        }
        throw error;
      }
    }

    // Max retries reached
    if (DEBUG_TESTS) {
      console.error(`[Tests] ❌ Max polling attempts (${TEST_POLLING.MAX_RETRIES}) reached`);
      console.error(`[Tests]   Elapsed time: ${Math.round((Date.now() - startTime) / 1000)}s`);
      console.error(`[Tests] ========================================`);
    }
    throw new Error(
      `Test execution did not complete within ${TEST_POLLING.MAX_RETRIES} polling attempts ` +
      `(${Math.round((Date.now() - startTime) / 1000)}s). ` +
      `Tests may still be running. Check status manually.`
    );
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
  // Note: runAllTests() and runTest() methods removed - endpoints don't exist in API
  // Use executeRule() to manually test individual rules instead

  /**
   * Validate a project for errors
   *
   * Note: The REST API does not expose a /validation endpoint.
   * This method will return a 404 error. Validation may occur
   * automatically when compiling or deploying projects.
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
   * @returns Validation results with errors and warnings
   * @throws Error if endpoint doesn't exist (404)
   */
  async validateProject(projectId: string): Promise<Types.ValidationResult> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.ValidationResult>(
      `${projectPath}/validation`
    );
    return response.data;
  }

  /**
   * Get detailed project errors with categorization and fix suggestions
   *
   * @param projectId - Project ID in base64-encoded format (default). Supports backward compatibility with "repository-projectName" and "repository:projectName" formats.
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
   * Note: The REST API does not expose a /files/{path}/history endpoint.
   * This method will return a 404 error. File history may need to be accessed
   * through project-level history or external Git tools.
   *
   * @param request - File history request
   * @returns File commit history with pagination
   * @throws Error if endpoint doesn't exist (404)
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

    const commits = (response.data.commits && Array.isArray(response.data.commits)) 
      ? response.data.commits.map((fileData: Types.FileData) => ({
          commitHash: fileData.version || "",
          author: fileData.author || { name: "unknown", email: "" },
          timestamp: fileData.modifiedAt || new Date().toISOString(),
          comment: fileData.comment || "",
          commitType: this.parseCommitType(fileData.comment),
          size: fileData.size,
        }))
      : [];

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
   * Uses the OpenAPI 3.0.1 endpoint structure:
   * - /repos/{repo}/projects/{project-name}/history
   * - /repos/{repo}/branches/{branch}/projects/{project-name}/history
   *
   * @param request - Project history request with pagination parameters
   * @returns Project commit history with paginated response
   */
  async getProjectHistory(request: Types.GetProjectHistoryRequest): Promise<Types.GetProjectHistoryResult> {
    // Parse project ID to get repository and project name
    const [repository, projectName] = this.parseProjectId(request.projectId);

    // Build the endpoint URL based on whether a branch is specified
    let endpoint: string;
    if (request.branch) {
      endpoint = `/repos/${encodeURIComponent(repository)}/branches/${encodeURIComponent(request.branch)}/projects/${encodeURIComponent(projectName)}/history`;
    } else {
      endpoint = `/repos/${encodeURIComponent(repository)}/projects/${encodeURIComponent(projectName)}/history`;
    }

    // Build query parameters using OpenAPI 3.0.1 parameter names
    const params: Record<string, unknown> = {
      page: (request.page !== undefined && request.page !== null) ? request.page : 0,
      size: (request.size !== undefined && request.size !== null) ? request.size : 50,
    };
    if (request.search) {
      params.search = request.search;
    }
    if (request.techRevs !== undefined) {
      params.techRevs = request.techRevs;
    }

    const response = await this.axiosInstance.get<Types.PageResponseProjectRevision_Short>(
      endpoint,
      { params }
    );

    // Convert PageResponseProjectRevision_Short to legacy GetProjectHistoryResult format
    const commits = response.data.content.map((revision) => ({
      commitHash: revision.commitHash || revision.version || "",
      author: revision.author || { name: "unknown", email: "" },
      timestamp: revision.modifiedAt || new Date().toISOString(),
      comment: revision.comment || "",
      commitType: this.parseCommitType(revision.comment),
      filesChanged: revision.filesChanged || 0,
      tablesChanged: revision.tablesChanged,
    }));

    return {
      projectId: request.projectId,
      branch: request.branch || "main",
      commits,
      total: response.data.totalElements || response.data.numberOfElements,
      hasMore: (response.data.pageNumber + 1) < (response.data.totalPages || 1),
    };
  }

}
