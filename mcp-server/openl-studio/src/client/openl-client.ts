/**
 * OpenL Studio REST API Client
 * Handles authentication, session management, and API calls
 */

import { writeFileSync } from 'fs';
import type { OpenLStudioConfig } from '../config/types.js';
import { logger } from '../utils/logger.js';
import {
  AuthenticationError,
  NotFoundError,
  PermissionError,
  ServerError,
  NetworkError,
  ValidationError,
} from '../utils/errors.js';
import type {
  ProjectInfo,
  RepositoryInfo,
  OpenProjectParams,
  CloseProjectParams,
  ExportProjectParams,
  CopyProjectParams,
  DeleteProjectParams,
  ApiResponse,
} from './types.js';

/**
 * OpenL Studio API Client
 */
export class OpenLClient {
  private baseUrl: string;
  private username: string;
  private password: string;
  private sessionCookie?: string;
  private isAuthenticated: boolean = false;

  constructor(config: OpenLStudioConfig) {
    this.baseUrl = config.baseUrl.replace(/\/$/, ''); // Remove trailing slash
    this.username = config.username;
    this.password = config.password;
    // Note: timeout and retries could be used for future retry logic
  }

  /**
   * Authenticate with OpenL Studio
   */
  async authenticate(): Promise<void> {
    logger.info('Authenticating with OpenL Studio...');

    try {
      const response = await fetch(`${this.baseUrl}/login`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: new URLSearchParams({
          j_username: this.username,
          j_password: this.password,
        }),
        redirect: 'manual', // Don't follow redirects
      });

      // Check for authentication cookies
      const setCookie = response.headers.get('set-cookie');
      if (setCookie) {
        // Extract JSESSIONID or similar
        const match = setCookie.match(/JSESSIONID=([^;]+)/);
        if (match) {
          this.sessionCookie = match[0];
          this.isAuthenticated = true;
          logger.info('Authentication successful');
          return;
        }
      }

      // Alternative: Check if redirect to success page
      if (response.status === 302 || response.status === 301) {
        const location = response.headers.get('location');
        if (location && !location.includes('login')) {
          // Successful login (redirected away from login page)
          const setCookie2 = response.headers.get('set-cookie');
          if (setCookie2) {
            this.sessionCookie = setCookie2;
            this.isAuthenticated = true;
            logger.info('Authentication successful');
            return;
          }
        }
      }

      throw new AuthenticationError('Authentication failed: Invalid credentials');
    } catch (error: any) {
      if (error instanceof AuthenticationError) {
        throw error;
      }
      logger.error('Authentication error:', error);
      throw new NetworkError(`Failed to authenticate: ${error.message}`);
    }
  }

  /**
   * Make an authenticated HTTP request
   */
  private async request<T = any>(
    path: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const headers: Record<string, string> = {
      ...(options.headers as Record<string, string>),
    };

    // Add session cookie if authenticated
    if (this.sessionCookie) {
      headers['Cookie'] = this.sessionCookie;
    }

    logger.debug(`Request: ${options.method || 'GET'} ${url}`);

    try {
      const response = await fetch(url, {
        ...options,
        headers,
      });

      // Handle authentication errors
      if (response.status === 401 || response.status === 403) {
        // Try to re-authenticate once
        if (this.isAuthenticated) {
          logger.warn('Session expired, re-authenticating...');
          this.isAuthenticated = false;
          await this.authenticate();
          // Retry the request
          return this.request<T>(path, options);
        }
        throw new AuthenticationError('Authentication required');
      }

      // Handle not found
      if (response.status === 404) {
        throw new NotFoundError(`Resource not found: ${path}`);
      }

      // Handle validation errors
      if (response.status === 400) {
        const text = await response.text();
        throw new ValidationError(`Invalid request: ${text}`);
      }

      // Handle permission errors
      if (response.status === 403) {
        throw new PermissionError('Permission denied');
      }

      // Handle server errors
      if (response.status >= 500) {
        throw new ServerError(`Server error: ${response.statusText}`);
      }

      // Handle success
      if (response.ok) {
        const contentType = response.headers.get('content-type');
        if (contentType?.includes('application/json')) {
          return (await response.json()) as T;
        }
        return (await response.text()) as T;
      }

      throw new ServerError(`Unexpected response: ${response.status} ${response.statusText}`);
    } catch (error: any) {
      if (
        error instanceof AuthenticationError ||
        error instanceof NotFoundError ||
        error instanceof ValidationError ||
        error instanceof PermissionError ||
        error instanceof ServerError
      ) {
        throw error;
      }
      logger.error(`Request error for ${url}:`, error);
      throw new NetworkError(`Network request failed: ${error.message}`);
    }
  }

  /**
   * Ensure client is authenticated
   */
  private async ensureAuthenticated(): Promise<void> {
    if (!this.isAuthenticated) {
      await this.authenticate();
    }
  }

  /**
   * List all repositories
   */
  async listRepositories(): Promise<RepositoryInfo[]> {
    await this.ensureAuthenticated();
    logger.info('Listing repositories...');

    try {
      // Note: Actual endpoint may vary - this is a placeholder
      const response = await this.request<any>('/admin/repositories');

      // Parse response and convert to RepositoryInfo[]
      // The actual structure depends on the API response
      if (Array.isArray(response)) {
        return response.map((repo: any) => ({
          name: repo.name || repo.id,
          type: repo.type || 'unknown',
          path: repo.path,
        }));
      }

      return [];
    } catch (error) {
      logger.error('Failed to list repositories:', error);
      throw error;
    }
  }

  /**
   * List all projects in a repository
   */
  async listProjects(repositoryName?: string): Promise<ProjectInfo[]> {
    await this.ensureAuthenticated();
    logger.info(`Listing projects${repositoryName ? ` in ${repositoryName}` : ''}...`);

    try {
      // If repository specified, get projects for that repo
      if (repositoryName) {
        const path = `/user-workspace/${encodeURIComponent(repositoryName)}/projects`;
        const response = await this.request<any>(path);
        return this.parseProjectsResponse(response, repositoryName);
      }

      // Otherwise, get all projects across all repositories
      const repos = await this.listRepositories();
      const allProjects: ProjectInfo[] = [];

      for (const repo of repos) {
        try {
          const projects = await this.listProjects(repo.name);
          allProjects.push(...projects);
        } catch (error) {
          logger.warn(`Failed to list projects for repository ${repo.name}:`, error);
        }
      }

      return allProjects;
    } catch (error) {
      logger.error('Failed to list projects:', error);
      throw error;
    }
  }

  /**
   * Parse projects response
   */
  private parseProjectsResponse(response: any, repositoryName: string): ProjectInfo[] {
    if (Array.isArray(response)) {
      return response.map((proj: any) => ({
        name: proj.name,
        repository: repositoryName,
        status: proj.status || 'CLOSED',
        lastModified: proj.lastModified,
        version: proj.version,
        branch: proj.branch,
      }));
    }

    if (response && typeof response === 'object' && response.projects) {
      return this.parseProjectsResponse(response.projects, repositoryName);
    }

    return [];
  }

  /**
   * Get project information
   */
  async getProjectInfo(repositoryName: string, projectName: string): Promise<ProjectInfo> {
    await this.ensureAuthenticated();
    logger.info(`Getting info for project ${projectName} in ${repositoryName}...`);

    try {
      const path = `/user-workspace/${encodeURIComponent(repositoryName)}/projects/${encodeURIComponent(projectName)}/info`;
      const response = await this.request<any>(path);

      return {
        name: projectName,
        repository: repositoryName,
        status: response.status || 'CLOSED',
        lastModified: response.lastModified,
        dependencies: response.dependencies || [],
        version: response.version,
        branch: response.branch,
      };
    } catch (error) {
      logger.error(`Failed to get project info for ${projectName}:`, error);
      throw error;
    }
  }

  /**
   * Open a project
   */
  async openProject(params: OpenProjectParams): Promise<ApiResponse> {
    await this.ensureAuthenticated();
    logger.info(`Opening project ${params.projectName} in ${params.repositoryName}...`);

    try {
      const path = `/user-workspace/${encodeURIComponent(params.repositoryName)}/projects/${encodeURIComponent(params.projectName)}/open?open-dependencies=${params.openDependencies !== false}`;

      await this.request(path, {
        method: 'POST',
      });

      return {
        success: true,
        message: `Project ${params.projectName} opened successfully`,
      };
    } catch (error) {
      logger.error(`Failed to open project ${params.projectName}:`, error);
      throw error;
    }
  }

  /**
   * Close a project
   */
  async closeProject(params: CloseProjectParams): Promise<ApiResponse> {
    await this.ensureAuthenticated();
    logger.info(`Closing project ${params.projectName} in ${params.repositoryName}...`);

    try {
      const path = `/user-workspace/${encodeURIComponent(params.repositoryName)}/projects/${encodeURIComponent(params.projectName)}/close`;

      await this.request(path, {
        method: 'POST',
      });

      return {
        success: true,
        message: `Project ${params.projectName} closed successfully`,
      };
    } catch (error) {
      logger.error(`Failed to close project ${params.projectName}:`, error);
      throw error;
    }
  }

  /**
   * Export a project as ZIP
   */
  async exportProject(params: ExportProjectParams): Promise<string> {
    await this.ensureAuthenticated();
    logger.info(`Exporting project ${params.projectName} from ${params.repositoryName}...`);

    try {
      // Export endpoint may vary - this is based on ExportBean analysis
      const version = params.version || 'latest';
      const path = `/export/${encodeURIComponent(params.projectName)}/version/${encodeURIComponent(version)}`;

      const response = await fetch(`${this.baseUrl}${path}`, {
        method: 'GET',
        headers: {
          'Cookie': this.sessionCookie || '',
        },
      });

      if (!response.ok) {
        throw new ServerError(`Export failed: ${response.statusText}`);
      }

      // Get binary data
      const buffer = await response.arrayBuffer();

      // Determine output path
      const outputPath = params.outputPath || `./${params.projectName}.zip`;

      // Write to file
      writeFileSync(outputPath, Buffer.from(buffer));

      logger.info(`Project exported to ${outputPath}`);
      return outputPath;
    } catch (error) {
      logger.error(`Failed to export project ${params.projectName}:`, error);
      throw error;
    }
  }

  /**
   * Copy a project
   */
  async copyProject(params: CopyProjectParams): Promise<ApiResponse> {
    await this.ensureAuthenticated();
    logger.info(`Copying project ${params.sourceProject} to ${params.destinationProject}...`);

    try {
      // Copy endpoint based on CopyBean analysis
      const response = await this.request('/copy', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          sourceRepository: params.sourceRepository,
          sourceProject: params.sourceProject,
          destinationRepository: params.destinationRepository,
          destinationProject: params.destinationProject,
          copyHistory: params.copyHistory || false,
          comment: params.comment || 'Copied via MCP',
        }),
      });

      return {
        success: true,
        message: `Project copied from ${params.sourceProject} to ${params.destinationProject}`,
        data: response,
      };
    } catch (error) {
      logger.error(`Failed to copy project ${params.sourceProject}:`, error);
      throw error;
    }
  }

  /**
   * Delete a project
   */
  async deleteProject(params: DeleteProjectParams): Promise<ApiResponse> {
    await this.ensureAuthenticated();
    logger.info(`Deleting project ${params.projectName} from ${params.repositoryName}...`);

    try {
      const path = `/user-workspace/${encodeURIComponent(params.repositoryName)}/projects/${encodeURIComponent(params.projectName)}/delete`;

      await this.request(path, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          comment: params.comment || 'Deleted via MCP',
        }),
      });

      return {
        success: true,
        message: `Project ${params.projectName} deleted successfully`,
      };
    } catch (error) {
      logger.error(`Failed to delete project ${params.projectName}:`, error);
      throw error;
    }
  }
}
