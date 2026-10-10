/**
 * OpenL Studio API Client
 *
 * Provides a high-level interface for interacting with OpenL Studio REST API.
 * Handles all HTTP communication, error handling, and response parsing.
 */

import axios, { AxiosInstance, AxiosResponse } from "axios";
import FormData from "form-data";
import type * as Types from "./types.js";
import { AuthenticationManager } from "./auth.js";
import { DEFAULTS, ERROR_LOCAL_REPOSITORY, REPOSITORY_LOCAL } from "./constants.js";
import {
  isResultNotReady,
  validateTimeout,
  sanitizeError,
  normalizeOpenLBaseUrl,
} from "./utils.js";

/** Validate that an optional table id in a replacement body matches the path id. */
export function validateTableIdMatch(tableId: string, viewId: string | undefined): void {
  if (viewId !== undefined && viewId !== tableId) {
    throw new Error(
      `Table ID mismatch: tableId parameter is "${tableId}" but view.id is "${viewId}". ` +
      `These must match. Use the same ID from get_table() for both parameters.`
    );
  }
}

/**
 * Client for OpenL Studio REST API
 *
 * Usage:
 * ```typescript
 * const client = new OpenLClient({
 *   baseUrl: "http://localhost:8080",
 *   personalAccessToken: "openl_pat_…"
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
  private jsessionId: string | null = null; // Store JSESSIONID cookie for session management
  /**
   * Gate that serializes the very first cookie-less request so that any
   * requests fired in parallel before the JSESSIONID is captured wait for it.
   * Without this, LLM clients that dispatch multiple tool calls concurrently
   * (typical for Claude Desktop / Cursor when a model emits several tool_use
   * blocks in one turn) each get a fresh studio session, breaking session-
   * scoped state like the compilation registry.
   * Reset to `null` once the cookie has been captured, after which requests
   * proceed in parallel without further serialization.
   */
  private firstRequestGate: Promise<void> | null = null;
  private testExecutionHeaders: Map<string, Record<string, string>> = new Map(); // Store headers for test execution sessions

  /**
   * Create a new OpenL Studio API client
   *
   * @param config - Client configuration including base URL and authentication
   */
  constructor(config: Types.OpenLConfig) {
    this.baseUrl = normalizeOpenLBaseUrl(config.baseUrl);

    // Validate and set timeout
    const timeout = validateTimeout(config.timeout, DEFAULTS.TIMEOUT);

    // Create Axios instance with default configuration
    this.axiosInstance = axios.create({
      baseURL: this.baseUrl,
      timeout,
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
      },
    });

    // Setup authentication
    this.authManager = new AuthenticationManager(config);
    this.authManager.setupInterceptors(this.axiosInstance);

    // Setup cookie management: extract JSESSIONID from responses and add to requests
    this.setupCookieInterceptors();
  }

  /**
   * Setup interceptors to automatically handle JSESSIONID cookies
   * Extracts JSESSIONID from set-cookie headers and adds it to all subsequent requests
   */
  private setupCookieInterceptors(): void {
    const debug = process.env.DEBUG_COOKIE === "true";

    // Shared helper used by both success and error response paths to release the
    // first-request gate (if this config opened one) so queued requests can fire.
    const releaseFirstRequestGate = (config: unknown): void => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const release = (config as any)?._releaseFirstRequestGate;
      if (release) {
        release();
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (config as any)._releaseFirstRequestGate = undefined;
      }
    };

    // Response interceptor: Extract JSESSIONID from set-cookie headers.
    this.axiosInstance.interceptors.response.use(
      (response) => {
        const setCookieHeader = response.headers['set-cookie'];
        if (setCookieHeader) {
          const cookies = Array.isArray(setCookieHeader) ? setCookieHeader : [setCookieHeader];
          for (const cookie of cookies) {
            const jsessionMatch = cookie.match(/JSESSIONID=([^;]+)/);
            if (jsessionMatch) {
              const previous = this.jsessionId;
              this.jsessionId = jsessionMatch[1];
              if (debug) {
                console.error(
                  `[Cookie] CAPTURE ${response.config?.method?.toUpperCase()} ${response.config?.url} → JSESSIONID=${this.jsessionId.substring(0, 12)}…${previous && previous !== this.jsessionId ? ` (was ${previous.substring(0, 12)}…)` : ""}`
                );
              }
              break;
            }
          }
        }
        // Re-arm the bootstrap gate. Once a JSESSIONID is captured, future
        // requests carry it and need no gate. If THIS was the bootstrap but the
        // response issued no cookie (e.g. GET /repos never sets one), clear the
        // gate too — otherwise siblings waiting on it would wake on a resolved
        // gate with a still-null cookie and each open its own studio session.
        // Clearing it lets the next waiter/incoming request re-bootstrap, so we
        // keep serializing until a cookie actually lands.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        const wasBootstrap = Boolean((response.config as any)?._releaseFirstRequestGate);
        if (this.jsessionId || wasBootstrap) {
          this.firstRequestGate = null;
        }
        releaseFirstRequestGate(response.config);
        return response;
      },
      (error) => {
        // Always release the gate on error, otherwise queued requests would
        // wait forever if the bootstrap request failed. Re-arm it when the
        // failed request was the bootstrap so the next waiter re-bootstraps
        // rather than spinning on an already-resolved gate.
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        if ((error?.config as any)?._releaseFirstRequestGate) {
          this.firstRequestGate = null;
        }
        releaseFirstRequestGate(error?.config);
        return Promise.reject(error);
      }
    );

    // Request interceptor: bootstrap-gate + add JSESSIONID to Cookie header.
    this.axiosInstance.interceptors.request.use(
      async (config) => {
        // Bootstrap gate: until a JSESSIONID is captured, serialize requests so
        // they share one studio session instead of each opening its own. A
        // sibling waits on the in-flight bootstrap; if that bootstrap returns
        // without a cookie (e.g. GET /repos issues none), the gate is re-armed
        // (see the response interceptor) and the woken sibling loops to become
        // the next bootstrap — so we keep serializing until a cookie lands.
        while (!this.jsessionId) {
          if (this.firstRequestGate) {
            // A sibling is bootstrapping; wait, then re-check the loop condition.
            await this.firstRequestGate;
          } else {
            // We are the bootstrap. Open the gate and remember how to release it.
            let resolveGate!: () => void;
            this.firstRequestGate = new Promise<void>((r) => { resolveGate = r; });
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            (config as any)._releaseFirstRequestGate = resolveGate;
            break;
          }
        }

        if (this.jsessionId && config.headers) {
          // Check if Cookie header already exists
          const existingCookie = config.headers['Cookie'] || config.headers['cookie'];
          if (existingCookie) {
            // Append JSESSIONID if not already present
            if (!existingCookie.includes('JSESSIONID=')) {
              config.headers['Cookie'] = `${existingCookie}; JSESSIONID=${this.jsessionId}`;
            }
          } else {
            // Set Cookie header with JSESSIONID
            config.headers['Cookie'] = `JSESSIONID=${this.jsessionId}`;
          }
          if (debug) {
            console.error(
              `[Cookie] SEND    ${config.method?.toUpperCase()} ${config.url} ← JSESSIONID=${this.jsessionId.substring(0, 12)}…`
            );
          }
        } else if (debug) {
          console.error(
            `[Cookie] SEND    ${config.method?.toUpperCase()} ${config.url} ← (no cookie)`
          );
        }
        return config;
      },
      (error) => Promise.reject(error)
    );
  }

  /**
   * Get the base URL of the OpenL Studio instance
   */
  public getBaseUrl(): string {
    return this.baseUrl;
  }

  /**
   * Current `JSESSIONID` captured from a prior HTTP response, or `null` if none
   * has been seen yet. Two consumers rely on this:
   *
   * 1. The STOMP transport, which reuses the same session for the WebSocket
   *    handshake (the studio authenticates STOMP via the HTTP session cookie,
   *    not via STOMP CONNECT headers).
   * 2. The CLI `--cookie-jar` flag, which round-trips this value through a file
   *    so session-coupled flows (notably trace: the debug session is
   *    server-side and keyed by JSESSIONID; every step/inspect/resume call
   *    must present the same cookie) work across separate `npx` invocations.
   */
  public getSessionCookie(): string | null {
    return this.jsessionId;
  }

  /**
   * Get the current authentication method
   */
  public getAuthMethod(): string {
    return this.authManager.getAuthMethod();
  }

  /**
   * Restore a previously captured JSESSIONID so subsequent requests reuse
   * an existing server-side session. Pair with `getSessionCookie()` after
   * a request to round-trip the session through a file or other store.
   *
   * @param value - JSESSIONID value (without the `JSESSIONID=` prefix), or `null` to clear
   */
  public setSessionCookie(value: string | null): void {
    this.jsessionId = value;
  }

  /**
   * Get the `Authorization` header value this client uses for REST. Exposed
   * so non-axios consumers (e.g. the STOMP WebSocket handshake) can attach
   * the same credentials — the studio's REST filter chain authenticates on
   * the WS upgrade just like every other `/rest/*` request.
   */
  public getAuthorizationHeader(): string | undefined {
    return this.authManager.getAuthorizationHeader();
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
   * Resolve a user-supplied repository identifier (id OR display name,
   * case-insensitive) to the canonical repository id used by the OpenL REST
   * API. This is the contract advertised by the tool descriptions under
   * `src/handlers/` — LLMs tend to pass whichever of the two
   * fields they see first in `openl_list_repositories()` output, sometimes
   * with case drift, so we accept both forms.
   *
   * Match order (most specific first to avoid surprises when an id and a
   * name happen to collide):
   *   1. Exact id match
   *   2. Exact name match
   *   3. Case-insensitive id match
   *   4. Case-insensitive name match
   *
   * @param repositoryIdOrName - Repository id (e.g. "design") or display name
   *   (e.g. "Design Repository"); either is accepted, case-insensitively.
   * @returns Canonical repository id (e.g., "design")
   * @throws Error if no repository matches in any of the four checks
   */
  async getRepositoryIdByName(repositoryIdOrName: string): Promise<string> {
    const repositories = await this.listRepositories();

    const exactId = repositories.find(r => r.id === repositoryIdOrName);
    if (exactId) return exactId.id;
    const exactName = repositories.find(r => r.name === repositoryIdOrName);
    if (exactName) return exactName.id;

    const needle = repositoryIdOrName.toLowerCase();
    const ciId = repositories.find(r => r.id.toLowerCase() === needle);
    if (ciId) return ciId.id;
    const ciName = repositories.find(r => r.name.toLowerCase() === needle);
    if (ciName) return ciName.id;

    const available = repositories.map(r => `${r.id} (${r.name})`).join(", ");
    throw new Error(
      `Repository "${repositoryIdOrName}" not found. ` +
      `Available repositories: ${available || "none"}. ` +
      `Use openl_list_repositories() to see all available repositories.`
    );
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
    const repositories = await this.listRepositories();
    const match = repositories.find((candidate) => candidate.id === repository);
    if (!match) {
      throw new Error(`Repository "${repository}" not found.`);
    }
    return match.features ?? {};
  }

  /**
   * List deployment repositories
   *
   * @param useCache - Whether to use cached repositories (default: true)
   * @returns Array of deployment repository information
   */
  async listDeployRepositories(_useCache: boolean = true): Promise<Types.Repository[]> {
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
   * Get the revision history of a project in its current branch.
   *
   * @param projectId - Stable project ID
   * @param options - Query options (search, technical revisions, pagination)
   * @returns Paginated project revisions
   */
  async getProjectRevisions(
    projectId: string,
    options?: {
      search?: string;
      techRevs?: boolean;
      offset?: number;
      page?: number;
      size?: number;
    }
  ): Promise<Types.PageResponse<Types.ProjectRevision>> {
    const params: Record<string, string | number | boolean> = {};
    if (options?.search) params.search = options.search;
    if (options?.techRevs !== undefined) params.techRevs = options.techRevs;
    if (options?.page !== undefined) params.page = options.page;
    else if (options?.offset !== undefined) params.offset = options.offset;
    if (options?.size !== undefined) params.size = options.size;

    const response = await this.axiosInstance.get<Types.PageResponse<Types.ProjectRevision>>(
      `/projects/${encodeURIComponent(projectId)}/history`,
      { params }
    );
    return response.data;
  }

  // =============================================================================
  // Project Creation & Repository Files (repo-mount, direct-to-branch)
  // =============================================================================

  /**
   * Create a new project in a design repository from a ZIP skeleton.
   *
   * Maps to `PUT /repos/{repo}/projects/{name}` (multipart, `template` = zip),
   * which commits the project in a single FULL changeset and returns the commit
   * revision. An optional branch targets an existing branch (or lets Studio
   * create a missing branch from the repository base). A collision returns 409.
   *
   * @param repositoryId - Canonical repository id (resolve via getRepositoryIdByName)
   * @param projectName - New project name (also the project folder)
   * @param templateZip - ZIP archive whose root entries become the project files
   * @param options - Optional comment, target branch, and mapped-folder path
   * @returns The created project's revision (commit SHA) and branch (if supported)
   */
  async createProjectFromZip(
    repositoryId: string,
    projectName: string,
    templateZip: Buffer,
    options?: { comment?: string; path?: string; branch?: string }
  ): Promise<Types.CreateProjectResult> {
    const form = new FormData();
    form.append("template", templateZip, {
      filename: "template.zip",
      contentType: "application/zip",
    });
    if (options?.comment) form.append("comment", options.comment);
    if (options?.path) form.append("path", options.path);
    if (options?.branch) form.append("branch", options.branch);

    const response = await this.axiosInstance.put<Types.CreateProjectResult>(
      `/repos/${encodeURIComponent(repositoryId)}/projects/${encodeURIComponent(projectName)}`,
      form,
      { headers: form.getHeaders() }
    );
    return response.data;
  }

  /** Copy a project addressed by its stable ID and register the copy in Studio. */
  async copyProject(
    targetRepositoryId: string,
    targetProjectName: string,
    sourceProject: string,
    options?: { comment?: string; branch?: string; revision?: string; path?: string },
  ): Promise<Types.CreateProjectResult> {
    const response = await this.axiosInstance.post<Types.CreateProjectResult>(
      `/repos/${encodeURIComponent(targetRepositoryId)}/projects/${encodeURIComponent(targetProjectName)}/from-project`,
      {
        sourceProject,
        ...(options?.comment ? { comment: options.comment } : {}),
        ...(options?.branch ? { branch: options.branch } : {}),
        ...(options?.revision ? { revision: options.revision } : {}),
        ...(options?.path ? { path: options.path } : {}),
      },
    );
    return response.data;
  }

  // =============================================================================
  // Project Management
  // =============================================================================

  /**
   * Build URL-safe project path for OpenL API
   *
   * projectId is treated as an opaque backend identifier.
   *
   * @param projectId - Project ID returned by backend
   * @returns URL-encoded project path
   */
  private buildProjectPath(projectId: string): string {
    // Normalize the projectId to avoid issues with surrounding whitespace
    // and double-encoding of already-percent-encoded values.
    const trimmed = projectId.trim();

    let normalizedId = trimmed;

    // If the ID appears to contain percent-encoded sequences, attempt to decode
    // it first to avoid double-encoding (e.g., %20 -> %2520).
    if (/%[0-9A-Fa-f]{2}/.test(trimmed)) {
      try {
        normalizedId = decodeURIComponent(trimmed);
      } catch {
        // If decoding fails (malformed encoding), fall back to the trimmed value.
        normalizedId = trimmed;
      }
    }

    return `/projects/${encodeURIComponent(normalizedId)}`;
  }

  /** Encode a slash-delimited trailing path while preserving its separators. */
  private encodePathSegments(value: string): string {
    this.assertSafeProjectPath(value);
    return value.split("/").map(encodeURIComponent).join("/");
  }

  /** Normalize the response shapes used by Studio collection endpoints. */
  private normalizeCollectionPage<T>(responseData: unknown): Types.CollectionPage<T> {
    if (Array.isArray(responseData)) {
      return { items: responseData as T[], serverPaginated: false };
    }

    if (!responseData || typeof responseData !== "object") {
      return { items: [], serverPaginated: false };
    }

    const outer = responseData as Record<string, unknown>;
    const candidate = outer.data && typeof outer.data === "object" ? outer.data : outer;

    const pageFields = new Set([
      "content", "data", "numberOfElements", "pageNumber", "pageSize",
      "total", "totalElements", "totalPages",
    ]);
    const extractMetadata = (source: Record<string, unknown>): Record<string, unknown> =>
      Object.fromEntries(Object.entries(source).filter(([key]) => !pageFields.has(key)));
    const outerMetadata = candidate === outer ? {} : extractMetadata(outer);

    if (Array.isArray(candidate)) {
      return {
        items: candidate as T[],
        serverPaginated: false,
        ...(Object.keys(outerMetadata).length > 0 ? { metadata: outerMetadata } : {}),
      };
    }

    const page = candidate as Record<string, unknown>;
    if (!Array.isArray(page.content)) {
      return { items: [], serverPaginated: false };
    }

    const number = (value: unknown): number | undefined =>
      typeof value === "number" && Number.isFinite(value) ? value : undefined;
    const pageNumber = number(page.pageNumber);
    const pageSize = number(page.pageSize);
    const metadata = { ...outerMetadata, ...extractMetadata(page) };

    return {
      items: page.content as T[],
      serverPaginated: pageNumber !== undefined || pageSize !== undefined,
      pageNumber,
      pageSize,
      total: number(page.total) ?? number(page.totalElements),
      totalPages: number(page.totalPages),
      ...(Object.keys(metadata).length > 0 ? { metadata } : {}),
    };
  }

  /**
   * List projects while preserving the backend's pagination metadata.
   *
   * Tool handlers should use this method so they do not paginate an already
   * paginated backend response a second time.
   */
  async listProjectsPage(
      filters?: Types.ProjectFilters
  ): Promise<Types.CollectionPage<Types.ProjectSummary>> {
    // Build query parameters, handling tags with 'tags.' prefix and pagination
    const params: Record<string, string | number | string[]> = {};
    if (filters?.repository) params.repository = filters.repository;
    if (filters?.status) params.status = filters.status;
    if (filters?.dependsOn) params.dependsOn = filters.dependsOn;
    if (filters?.name) params.name = filters.name;
    if (filters?.author) params.author = filters.author;
    if (filters?.branch) params.branch = filters.branch;
    if (filters?.sort) params.sort = filters.sort;
    if (filters?.include?.length) params.include = filters.include;
    if (filters?.tags) {
      // Tags must be prefixed with 'tags.' in query string
      Object.entries(filters.tags).forEach(([key, value]) => {
        params[`tags.${key}`] = value;
      });
    }

    // The current Studio API accepts a true item offset. Keep page/size support
    // for direct client consumers, but never approximate offset as a page.
    if (filters?.page !== undefined) {
      params.page = filters.page;
    } else if (filters?.offset !== undefined) {
      params.offset = filters.offset;
    }

    if (filters?.size !== undefined) {
      params.size = filters.size;
    } else if (filters?.limit !== undefined) {
      params.size = filters.limit;
    }

    const response = await this.axiosInstance.get<Types.PageResponse<Types.ProjectSummary> | Types.ProjectSummary[] | { content?: Types.ProjectSummary[]; data?: Types.ProjectSummary[] }>(
        "/projects",
        { params, paramsSerializer: { indexes: null } }
    );

    return this.normalizeCollectionPage<Types.ProjectSummary>(response.data);
  }

  /**
   * List all projects with optional filters and pagination.
   *
   * This array-returning method remains for API compatibility. MCP handlers
   * use {@link listProjectsPage} to retain pagination metadata.
   */
  async listProjects(
      filters?: Types.ProjectFilters
  ): Promise<Types.ProjectSummary[]> {
    return (await this.listProjectsPage(filters)).items;
  }

  /**
   * Get project details by ID
   *
   * @param projectId - Opaque project ID returned by backend.
   * @returns Project details
   */
  async getProject(
    projectId: string,
    include?: Types.ProjectFilters["include"],
  ): Promise<Types.ComprehensiveProject> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.Project>(projectPath, {
      params: include?.length ? { include } : undefined,
      paramsSerializer: { indexes: null },
    });
    return response.data as Types.ComprehensiveProject;
  }

  /**
   * Get post-compilation project status (compile state, diagnostics, pending changes).
   * Read-only — does not trigger compilation. Works for all repositories including "local".
   *
   * @param projectId - Opaque project ID from the backend.
   * @param branch - Optional branch name. When provided, the backend asserts it matches
   *                 the project's currently opened branch (409 on mismatch).
   */
  async getProjectStatus(projectId: string, branch?: string): Promise<Types.ProjectStatusView> {
    const url = `${this.buildProjectPath(projectId)}/status`;
    const params: Record<string, string> = {};
    if (branch) {
      params.branch = branch;
    }
    const response = await this.axiosInstance.get<Types.ProjectStatusView>(url, { params });
    return response.data;
  }

  /**
   * Lazily initialize and await project compilation through Studio's tables API.
   * Studio has no dedicated compile endpoint: listing even one table opens the
   * project model, registers its compilation job, and waits for it to finish.
   * The caller supplies the wait timeout so large projects are not constrained
   * by the client's shorter general-purpose HTTP timeout.
   */
  async triggerProjectCompilation(
    projectId: string,
    options: { signal?: AbortSignal; timeoutMs?: number } = {},
  ): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.get(`${projectPath}/tables`, {
      params: { offset: 0, size: 1 },
      signal: options.signal,
      timeout: options.timeoutMs,
    });
  }

  /**
   * Throws if the project is in a local repository (repository === "local").
   * Local repositories are not connected to a remote Git; status change (open/save/close) is not supported by the API.
   */
  private async ensureNotLocalRepository(projectId: string): Promise<void> {
    const project = await this.getProject(projectId);
    if (project.repository === REPOSITORY_LOCAL) {
      throw new Error(ERROR_LOCAL_REPOSITORY);
    }
  }

  /**
   * Fetches the project and throws if it is in a local repository.
   * Use when you need the project data and the "not local" check in one GET.
   */
  private async getProjectAndEnsureNotLocal(projectId: string): Promise<Types.ComprehensiveProject> {
    const project = await this.getProject(projectId);
    if (project.repository === REPOSITORY_LOCAL) {
      throw new Error(ERROR_LOCAL_REPOSITORY);
    }
    return project;
  }

  /**
   * Delete a project
   *
   * @param projectId - Opaque project ID returned by backend.
   * @returns void (204 No Content on success)
   */
  async deleteProject(projectId: string, comment?: string): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.delete(
      projectPath,
      comment !== undefined ? { params: { comment } } : undefined,
    );
    // Returns 204 No Content
  }

  /**
   * Open a project for viewing/editing.
   *
   * Sends PATCH /projects/{projectId} with status "OPENED".
   * Use this only for projects that are not yet opened (status CLOSED, etc.).
   * For switching branches on an already opened project, use {@link switchBranch} instead
   * to avoid a 409 Conflict error.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param options - Optional branch, revision, and comment
   * @returns Success status (204 No Content on success)
   */
  async openProject(
    projectId: string,
    options?: { branch?: string; revision?: string; comment?: string; openDependencies?: boolean }
  ): Promise<boolean> {
    await this.ensureNotLocalRepository(projectId);
    const projectPath = this.buildProjectPath(projectId);

    const updateModel: Types.ProjectStatusUpdateModel = {
      status: "OPENED",
      ...options,
    };

    await this.axiosInstance.patch(projectPath, updateModel);
    return true;
  }

  /**
   * Switch branch on an already opened project.
   *
   * Sends PATCH /projects/{projectId} with only the branch field (no status).
   * This avoids the 409 Conflict error that occurs when sending status "OPENED"
   * for a project that is already opened or being edited.
   *
   * The OpenL Studio backend validator (canOpen) rejects re-opening an already
   * opened project. However, a PATCH with just {"branch": "..."} is accepted
   * and returns 204.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param branch - Target branch name to switch to
   * @returns Success status (204 No Content on success)
   */
  async switchBranch(
    projectId: string,
    branch: string
  ): Promise<boolean> {
    await this.ensureNotLocalRepository(projectId);
    const projectPath = this.buildProjectPath(projectId);

    const switchModel: Types.ProjectStatusUpdateModel = {
      branch,
    };

    await this.axiosInstance.patch(projectPath, switchModel);
    return true;
  }

  /**
   * Close an open project
   *
   * Updates project status to CLOSED using PATCH /projects/{projectId}
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param comment - Optional comment describing why the project is being closed
   * @returns Success status (204 No Content on success)
   */
  async closeProject(
    projectId: string,
    options?: { comment?: string; discardChanges?: boolean },
  ): Promise<boolean> {
    await this.ensureNotLocalRepository(projectId);
    const projectPath = this.buildProjectPath(projectId);
    const updateModel: Types.ProjectStatusUpdateModel = {
      status: "CLOSED",
      comment: options?.comment,
      discardChanges: options?.discardChanges,
    };

    await this.axiosInstance.patch(projectPath, updateModel);
    return true;
  }

  /**
   * Update project status with safety checks for unsaved changes
   *
   * Only OPENED and CLOSED can be set; other statuses are set automatically by the backend.
   * Prevents accidental data loss by requiring explicit confirmation when closing projects with unsaved changes.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param request - Status update request; status may be OPENED or CLOSED only
   * @returns Success status (204 No Content on success)
   * @throws Error if trying to close EDITING project without save or explicit discard
   */
  async updateProjectStatus(
    projectId: string,
    request: {
      status?: "OPENED" | "CLOSED";
      comment?: string;
      discardChanges?: boolean;
      branch?: string;
      revision?: string;
    }
  ): Promise<{ success: boolean; message: string }> {
    const projectPath = this.buildProjectPath(projectId);

    // SAFETY CHECK: Prevent closing with unsaved changes without explicit confirmation
    if (request.status === "CLOSED") {
      const currentProject = await this.getProjectAndEnsureNotLocal(projectId);
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
    } else {
      await this.ensureNotLocalRepository(projectId);
    }

    // Build the API request using ProjectStatusUpdateModel from the current API.
    const updateModel: Types.ProjectStatusUpdateModel = {
      status: request.status,
      comment: request.comment,
      branch: request.branch,
      revision: request.revision,
      discardChanges: request.discardChanges,
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
   * Save project changes, creating a new revision in the repository
   *
   * Works only when project status is EDITING. Requires comment; the server creates a new
   * revision with that comment and transitions the project to OPENED (or CLOSED if closeAfterSave).
   * Uses PATCH /projects/{projectId} with body { comment } or { comment, status: "CLOSED" }.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param comment - Comment for the new revision (required when project is EDITING; used as commit message)
   * @param options - Optional. closeAfterSave: if true, send status CLOSED so project is saved and closed in one request.
   * @returns Save result; if project is not EDITING, returns success with message "nothing to save" (no API call).
   * @throws Error if comment is missing or empty when project is EDITING
   */
  async saveProject(
    projectId: string,
    comment: string,
    options?: { closeAfterSave?: boolean }
  ): Promise<Types.SaveProjectResult> {
    const project = await this.getProjectAndEnsureNotLocal(projectId);
    if (project.status !== "EDITING") {
      return {
        success: true,
        message: "There are no changes in the project; nothing to save.",
      };
    }
    if (!comment.trim()) {
      throw new Error("comment is required for save; it is used as the revision (commit) message.");
    }

    const projectPath = this.buildProjectPath(projectId);

    // Save via PATCH /projects/{projectId} (Update project status API).
    // When project is EDITING and comment is present, the server creates a new revision and sets status to OPENED (or CLOSED if requested).
    const body: { comment: string; status?: "CLOSED" } = { comment: comment.trim() };
    if (options?.closeAfterSave) {
      body.status = "CLOSED";
    }
    await this.axiosInstance.patch(projectPath, body);

    const message = comment.trim();

    return {
      success: true,
      message,
    };
  }

  // =============================================================================
  // File Management
  // =============================================================================

  /**
   * Download an Excel file from a project
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param fileName - Name of the file to download (use the exact 'file' value from list_tables response)
   * @param version - Optional Git commit hash to download specific version
   * @returns File content as Buffer
   * @throws Error with helpful message if file not found (404)
   */
  async downloadFile(projectId: string, fileName: string, version?: string): Promise<Buffer> {
    const projectPath = this.buildProjectPath(projectId);

    // Build request params
    const params: any = {};
    if (version) {
      params.version = version;  // Git commit hash
    }

    // IMPORTANT: list_tables returns file paths like "Example 2 - Corporate Rating/Corporate Rating.xlsx"
    // The OpenL API expects the full path AS-IS from list_tables, including the project directory.
    // We'll try multiple variations to handle different scenarios.

    const pathsToTry: string[] = [];

    // Try the fileName exactly as provided first
    pathsToTry.push(fileName);

    // Keep a fallback without leading project directory for APIs that normalize paths.
    if (fileName.includes("/")) {
      const withoutProjectDir = fileName.substring(fileName.indexOf("/") + 1);
      if (withoutProjectDir && withoutProjectDir !== fileName) {
        pathsToTry.push(withoutProjectDir);
      }
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

  // =============================================================================
  // Project Files (BETA)
  // =============================================================================
  //
  // Thin wrappers over the "Projects: Files (BETA)" REST API
  // (/projects/{projectId}/files/{path}, /file-search, /file-copy, /file-move).
  // Unlike the legacy downloadFile (Excel-only, path-guessing), these
  // operate on ANY repo file by exact project-relative path and expose the raw
  // API surface (branch, version, conflictPolicy, glob/content search, copy/move).

  /**
   * Percent-encode each segment of a project-relative file path while preserving
   * '/' separators and any trailing slash (which denotes a folder to the API).
   * Leading slashes are dropped — paths are always project-relative.
   */
  private encodeProjectFilePath(path: string): string {
    const trimmed = (path ?? "").replace(/^\/+/, "");
    if (trimmed === "") return "";
    const hasTrailingSlash = trimmed.endsWith("/");
    const segments = trimmed.split("/").filter((seg) => seg.length > 0);
    this.assertSafeProjectPath(path);
    const encoded = segments.map(encodeURIComponent).join("/");
    return hasTrailingSlash ? `${encoded}/` : encoded;
  }

  /**
   * Defense-in-depth path validation: reject '.'/'..' segments so a caller-supplied
   * path can't escape the project subtree. URL-path operations (read/write/delete)
   * also need this because encodeURIComponent leaves '.'/'..' untouched (both are
   * RFC 3986 unreserved) and a downstream URL normalizer could collapse them; body-
   * path operations (copy/move source & destination, search 'from') don't go through
   * encodeProjectFilePath, so they call this directly rather than trusting the backend.
   *
   * @param path - Project-relative path to validate (no-op for empty/undefined).
   */
  private assertSafeProjectPath(path: string | undefined): void {
    if (!path) return;
    const segments = path.split("/").filter((seg) => seg.length > 0);
    if (segments.some((seg) => seg === "." || seg === "..")) {
      throw new Error(
        "Invalid path: '.' and '..' segments are not allowed; paths must be project-relative."
      );
    }
  }

  /**
   * Normalize a file path carried in a JSON request BODY (copy/move source &
   * destination, search 'from'): strip leading slashes so it's consistently
   * project-relative (matching the URL-path encoder), and validate it. Unlike
   * encodeProjectFilePath this does NOT percent-encode — body paths are sent raw
   * in JSON, so encoding would corrupt names containing spaces or reserved chars
   * (e.g. "My File.xlsx" -> "My%20File.xlsx", a literal name the backend can't find).
   */
  private normalizeBodyPath(path: string): string {
    const normalized = (path ?? "").replace(/^\/+/, "");
    this.assertSafeProjectPath(normalized);
    return normalized;
  }

  /**
   * Read a file's bytes, a file's metadata, or a folder listing from a project.
   *
   * Maps to `GET /projects/{projectId}/files/{path}`. The single endpoint serves
   * several response shapes depending on the path and query params:
   *  - file path                -> the file's raw bytes (Content-Disposition: attachment)
   *  - file path + view=meta    -> JSON metadata (FsNode)
   *  - folder path              -> JSON array of FsNode (or a tree when viewMode=NESTED)
   *  - folder path + download   -> a ZIP archive of the folder (attachment)
   *
   * The raw body is returned as a Buffer together with the Content-Type and
   * Content-Disposition headers so the caller can distinguish a file/ZIP download
   * (attachment) from a JSON listing/metadata response and decode accordingly.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param path - Project-relative path; empty or trailing-slash lists a folder.
   * @param options - Query parameters mirroring the REST API.
   */
  async readProjectFile(
    projectId: string,
    path: string,
    options?: {
      view?: "meta";
      download?: boolean;
      recursive?: boolean;
      viewMode?: "FLAT" | "NESTED";
      extensions?: string[];
      namePattern?: string;
      foldersOnly?: boolean;
      version?: string;
      branch?: string;
      fields?: string;
    }
  ): Promise<Types.ProjectFileResponse> {
    const projectPath = this.buildProjectPath(projectId);
    const encodedPath = this.encodeProjectFilePath(path);

    const params: Record<string, unknown> = {};
    if (options?.view) params.view = options.view;
    if (options?.download) params.download = "true";
    if (options?.recursive !== undefined) params.recursive = options.recursive;
    if (options?.viewMode) params.viewMode = options.viewMode;
    if (options?.extensions && options.extensions.length > 0) {
      // Spring binds a Set<String> query param from either repeated keys or a
      // comma-separated value; the comma form is the most portable.
      params.extensions = options.extensions.join(",");
    }
    if (options?.namePattern) params.namePattern = options.namePattern;
    if (options?.foldersOnly !== undefined) params.foldersOnly = options.foldersOnly;
    if (options?.version) params.version = options.version;
    if (options?.branch) params.branch = options.branch;
    if (options?.fields) params.fields = options.fields;

    const response = await this.axiosInstance.get<ArrayBuffer>(
      `${projectPath}/files/${encodedPath}`,
      {
        responseType: "arraybuffer",
        params,
        headers: { Accept: "*/*" },
      }
    );

    const headers = (response.headers ?? {}) as Record<string, unknown>;
    const headerValue = (name: string): string => {
      const v = headers[name] ?? headers[name.toLowerCase()];
      return typeof v === "string" ? v : "";
    };
    return {
      data: Buffer.from(response.data),
      contentType: headerValue("content-type").toLowerCase(),
      contentDisposition: headerValue("content-disposition"),
    };
  }

  /**
   * Write (create or replace) a file in a project's working copy.
   *
   * CREATE-only: maps to `POST /projects/{projectId}/files/{path}` with the raw
   * bytes as an `application/octet-stream` body. POST is create semantics — a
   * pre-existing target yields HTTP 409 (the backend does NOT apply conflictPolicy
   * to a single-file POST; to replace an existing file use {@link updateProjectFile}
   * / PUT). The write lands in the project working copy (NOT committed to Git) —
   * commit via {@link saveProject}.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param path - Project-relative file path.
   * @param content - Raw file bytes.
   * @param options - createFolders / branch.
   * @returns The backend's file-metadata response (may be empty).
   */
  async writeProjectFile(
    projectId: string,
    path: string,
    content: Buffer,
    options?: { createFolders?: boolean; branch?: string }
  ): Promise<unknown> {
    const projectPath = this.buildProjectPath(projectId);
    const encodedPath = this.encodeProjectFilePath(path);

    const params: Record<string, unknown> = {};
    if (options?.createFolders !== undefined) params.createFolders = options.createFolders;
    if (options?.branch) params.branch = options.branch;

    const response = await this.axiosInstance.post(
      `${projectPath}/files/${encodedPath}`,
      content,
      {
        headers: { "Content-Type": "application/octet-stream" },
        params,
      }
    );
    return response.data;
  }

  /**
   * OVERWRITE an existing file: maps to `PUT /projects/{projectId}/files/{path}`
   * with the raw bytes as an `application/octet-stream` body (the backend's
   * `updateResource`). PUT is update semantics — it replaces the content of an
   * EXISTING file in place (HTTP 204) and returns 404 if the file does not exist
   * (it does not create). The update lands in the project working copy (NOT
   * committed to Git) — commit via {@link saveProject}.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param path - Project-relative file path (must already exist).
   * @param content - Raw replacement bytes.
   * @param options - branch.
   */
  async updateProjectFile(
    projectId: string,
    path: string,
    content: Buffer,
    options?: { branch?: string }
  ): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    const encodedPath = this.encodeProjectFilePath(path);
    await this.axiosInstance.put(
      `${projectPath}/files/${encodedPath}`,
      content,
      {
        headers: { "Content-Type": "application/octet-stream" },
        params: options?.branch ? { branch: options.branch } : undefined,
      }
    );
  }

  /**
   * Delete a file or folder from a project by its project-relative path.
   *
   * Maps to `DELETE /projects/{projectId}/files/{path}` (HTTP 204). The backend
   * auto-cleans dangling references to the deleted resource from project config.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param path - Project-relative path to the resource.
   * @param options - Optional branch.
   */
  async deleteProjectFile(
    projectId: string,
    path: string,
    options?: { branch?: string }
  ): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    const encodedPath = this.encodeProjectFilePath(path);
    await this.axiosInstance.delete(`${projectPath}/files/${encodedPath}`, {
      params: options?.branch ? { branch: options.branch } : undefined,
    });
  }

  /**
   * Search a project's files/folders by glob pattern, extensions, type and a
   * case-insensitive content substring. Studio applies content matching only
   * to text files; binary formats such as XLSX and ZIP are not inspected.
   *
   * Maps to `POST /projects/{projectId}/file-search` (body = FileSearchQuery).
   * Returns the matching nodes. SUBTREE scope (default) may target a historical
   * `version`; ANCESTORS walks up to the repository root.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param query - Search criteria (all fields optional).
   * @param options - branch / fields query params.
   */
  async searchProjectFiles(
    projectId: string,
    query: Types.FileSearchQuery,
    options?: { branch?: string; fields?: string }
  ): Promise<Types.FsNode[]> {
    const body = query.from !== undefined ? { ...query, from: this.normalizeBodyPath(query.from) } : query;
    const projectPath = this.buildProjectPath(projectId);
    const params: Record<string, unknown> = {};
    if (options?.branch) params.branch = options.branch;
    if (options?.fields) params.fields = options.fields;

    const response = await this.axiosInstance.post<Types.FsNode[]>(
      `${projectPath}/file-search`,
      body,
      Object.keys(params).length > 0 ? { params } : undefined
    );
    return response.data;
  }

  /**
   * Resolve the chain of AGENTS.md files that apply to a project, per the
   * AGENTS.md specification: start at the project directory (or a sub-folder of
   * it when `folder` is given), walk UP through every parent directory to the
   * repository root, and collect the AGENTS.md found at each level. Levels with
   * no AGENTS.md are skipped (not an error); a project with none anywhere yields
   * an empty array.
   *
   * Implemented as a fixed ANCESTORS-scope `file-search` (`from = <folder>/AGENTS.md`),
   * which returns every same-named ancestor nearest-first WITH its content.
   * Proximity is carried by the array order alone (nearest-first); the
   * `openl_get_project_agent_context` tool renders them as one document via
   * `formatAgentsDocument`.
   *
   * @param projectId - Project ID or name (same resolution as other project calls).
   * @param options - `folder`: project-relative sub-folder to start the walk from
   *                   (for "the AGENTS.md nearest the edited file"); `branch`: pin
   *                   the project's branch.
   * @returns AGENTS.md files ordered nearest-first; empty array when none exist.
   */
  async getProjectAgentContext(
    projectId: string,
    options?: { folder?: string; branch?: string }
  ): Promise<Types.AgentsFile[]> {
    const folder = this.trimSlashes(options?.folder ?? "");
    const from = folder ? `${folder}/AGENTS.md` : "AGENTS.md";

    const nodes = await this.searchProjectFiles(
      projectId,
      { scope: "ANCESTORS", from },
      options?.branch ? { branch: options.branch } : undefined
    );

    return nodes.map((node) => ({
      path: node.path,
      content: node.content ?? "",
      size: node.size,
      lastModified: node.lastModified,
    }));
  }

  /** Strip leading and trailing '/' from a path (used to normalize the AGENTS.md `folder`). */
  private trimSlashes(path: string): string {
    return (path ?? "").replace(/^\/+|\/+$/g, "");
  }

  /**
   * Copy a file within a project to a new location.
   *
   * Maps to `POST /projects/{projectId}/file-copy` (body = {sourcePath,
   * destinationPath}, HTTP 201). Intermediate destination folders are created
   * automatically. A destination collision returns HTTP 409 (no overwrite option).
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param pair - Source and destination project-relative paths.
   * @param options - Optional branch.
   */
  async copyProjectFile(
    projectId: string,
    pair: Types.FilePathPairRequest,
    options?: { branch?: string }
  ): Promise<void> {
    const body: Types.FilePathPairRequest = {
      sourcePath: this.normalizeBodyPath(pair.sourcePath),
      destinationPath: this.normalizeBodyPath(pair.destinationPath),
    };
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(
      `${projectPath}/file-copy`,
      body,
      options?.branch ? { params: { branch: options.branch } } : undefined
    );
  }

  /**
   * Move or rename a file within a project.
   *
   * Maps to `POST /projects/{projectId}/file-move` (body = {sourcePath,
   * destinationPath}, HTTP 204). Intermediate destination folders are created
   * automatically; the source file is deleted after the move.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param pair - Source and destination project-relative paths.
   * @param options - Optional branch.
   */
  async moveProjectFile(
    projectId: string,
    pair: Types.FilePathPairRequest,
    options?: { branch?: string }
  ): Promise<void> {
    const body: Types.FilePathPairRequest = {
      sourcePath: this.normalizeBodyPath(pair.sourcePath),
      destinationPath: this.normalizeBodyPath(pair.destinationPath),
    };
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(
      `${projectPath}/file-move`,
      body,
      options?.branch ? { params: { branch: options.branch } } : undefined
    );
  }

  /**
   * Create a new branch in a project
   *
   * @param projectId - Opaque project ID returned by backend.
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

  /** List project-holding branches or every repository branch, including base/protected flags. */
  async listProjectBranches(
    projectId: string,
    scope?: Types.BranchScope,
  ): Promise<Types.ProjectBranchInfo[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.ProjectBranchInfo[]>(
      `${projectPath}/branches`,
      scope ? { params: { scope } } : undefined,
    );
    return response.data;
  }

  /** Delete a project branch; slash-delimited branch names remain path segments. */
  async deleteProjectBranch(projectId: string, branch: string, force: boolean = false): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.delete(
      `${projectPath}/branches/${this.encodePathSegments(branch)}`,
      { params: { force } },
    );
  }

  /** Check branch relationship and attempt blockers; conflicts are discovered only by the merge. */
  async checkProjectMerge(
    projectId: string,
    request: Types.MergeRequest,
  ): Promise<Types.CheckMergeResult> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.post<Types.CheckMergeResult>(
      `${projectPath}/merge/check`,
      request,
    );
    return response.data;
  }

  /** Perform a branch merge, returning success or the created conflict session. */
  async mergeProjectBranches(
    projectId: string,
    request: Types.MergeRequest,
    force: boolean = false,
  ): Promise<Types.MergeResultResponse> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.post<Types.MergeResultResponse>(
      `${projectPath}/merge`,
      request,
      { params: { force } },
    );
    return response.data;
  }

  /** Read the pending merge conflict details stored in this HTTP session. */
  async getMergeConflicts(projectId: string): Promise<Types.ConflictDetailsResponse> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.ConflictDetailsResponse>(
      `${projectPath}/merge/conflicts`,
    );
    return response.data;
  }

  /** Download one BASE/OURS/THEIRS version of a conflicted file. */
  async readMergeConflictFile(
    projectId: string,
    file: string,
    side: "BASE" | "OURS" | "THEIRS",
  ): Promise<Types.MergeConflictFileResponse> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<ArrayBuffer>(
      `${projectPath}/merge/conflicts/files`,
      {
        responseType: "arraybuffer",
        params: { file, side },
        headers: { Accept: "*/*" },
      },
    );
    const headers = (response.headers ?? {}) as Record<string, unknown>;
    const header = (name: string): string => {
      const value = headers[name] ?? headers[name.toLowerCase()];
      return typeof value === "string" ? value : "";
    };
    return {
      data: Buffer.from(response.data),
      contentType: header("content-type").toLowerCase(),
      contentDisposition: header("content-disposition"),
    };
  }

  /** Clear the pending merge conflict state from this HTTP session. */
  async cancelMergeConflicts(projectId: string): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.delete(`${projectPath}/merge/conflicts`);
  }

  // =============================================================================
  // Rules (Tables) Management
  // =============================================================================

  /** List tables while preserving the backend's pagination metadata. */
  async listTablesPage(
      projectId: string,
      filters?: Types.TableFilters
  ): Promise<Types.CollectionPage<Types.TableMetadata>> {
    const projectPath = this.buildProjectPath(projectId);

    // Build query parameters, handling kind (array), properties with 'properties.' prefix, and pagination
    const params: Record<string, string | string[] | number> = {};
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

    // The current Studio API accepts a true item offset.
    if (filters?.page !== undefined) {
      params.page = filters.page;
    } else if (filters?.offset !== undefined) {
      params.offset = filters.offset;
    }

    if (filters?.size !== undefined) {
      params.size = filters.size;
    } else if (filters?.limit !== undefined) {
      params.size = filters.limit;
    }

    const response = await this.axiosInstance.get<Types.PageResponse<Types.TableMetadata> | Types.TableMetadata[]>(
        `${projectPath}/tables`,
        { params, paramsSerializer: { indexes: null } }
    );

    return this.normalizeCollectionPage<Types.TableMetadata>(response.data);
  }

  /**
   * List all tables/rules in a project with optional filters and pagination.
   * Retains the historical array return type for direct client consumers.
   */
  async listTables(
      projectId: string,
      filters?: Types.TableFilters
  ): Promise<Types.TableMetadata[]> {
    return (await this.listTablesPage(projectId, filters)).items;
  }

  /** Execute a regular (non-test) table asynchronously. */
  async startTableRun(
    projectId: string,
    tableId: string,
    inputJson: unknown[] | Record<string, unknown>,
    options?: { fromModule?: string; signal?: AbortSignal; timeoutMs?: number }
  ): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(
      `${projectPath}/run`,
      inputJson,
      {
        params: {
          tableId,
          ...(options?.fromModule && { fromModule: options.fromModule }),
        },
        signal: options?.signal,
        timeout: options?.timeoutMs,
      }
    );
  }

  /** Read the current regular table run result, or `notReady` while it is still running. */
  async getTableRunResult(
    projectId: string,
    options?: { fields?: string; signal?: AbortSignal; timeoutMs?: number }
  ): Promise<Types.RunExecutionResult | Types.ResultNotReadyView> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.RunExecutionResult | Types.ResultNotReadyView>(
      `${projectPath}/run/result`,
      {
        params: options?.fields ? { fields: options.fields } : undefined,
        signal: options?.signal,
        timeout: options?.timeoutMs,
      }
    );
    if (response.status === 202) {
      return { status: "notReady" };
    }
    return response.data;
  }

  /** Cancel and clear the current regular table run. */
  async cancelTableRun(projectId: string): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.delete(`${projectPath}/run`);
  }

  /** Return either the whole project graph or one table's dependency neighborhood. */
  async getTableDependencies(
    projectId: string,
    options?: {
      tableId?: string;
      module?: string;
      layer?: "executable" | "datatype" | "all";
      direction?: "DEPENDENCIES" | "DEPENDENTS" | "BOTH";
      depth?: number;
    }
  ): Promise<Types.TableNodeView[]> {
    const projectPath = this.buildProjectPath(projectId);
    const url = options?.tableId
      ? `${projectPath}/tables/${encodeURIComponent(options.tableId)}/graph`
      : `${projectPath}/tables/graph`;
    const params = options?.tableId
      ? {
          ...(options.direction && { direction: options.direction }),
          ...(options.depth !== undefined && { depth: options.depth }),
        }
      : options?.module || options?.layer
        ? {
            ...(options.module && { module: options.module }),
            ...(options.layer && { layer: options.layer }),
          }
        : undefined;
    const response = await this.axiosInstance.get<Types.TableNodeView[]>(url, { params });
    return response.data;
  }

  /** Return the modules declared by a project descriptor. */
  async listProjectModules(projectId: string): Promise<Types.ModuleViewModel[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.ModuleViewModel[]>(`${projectPath}/modules`);
    return response.data;
  }

  /** Return worksheet names in one project module. */
  async listModuleSheets(projectId: string, moduleName: string): Promise<string[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<string[]>(
      `${projectPath}/modules/${encodeURIComponent(moduleName)}/sheets`
    );
    return response.data;
  }

  /** Return the table properties allowed by Studio for the requested context. */
  async listTablePropertyDefinitions(
    projectId: string,
    tableType?: string
  ): Promise<Types.PropertyDefinitionView[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.PropertyDefinitionView[]>(
      `${projectPath}/properties`,
      { params: tableType ? { tableType } : undefined }
    );
    return response.data;
  }

  /** Copy a table server-side, preserving its source layout and formatting. */
  async copyTable(
    projectId: string,
    tableId: string,
    request: Types.CopyTableRequest
  ): Promise<Types.TableMetadata> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.post<Types.TableMetadata>(
      `${projectPath}/tables/${encodeURIComponent(tableId)}/copy`,
      request
    );
    return response.data;
  }

  /**
   * Create a new table in a project using BETA API
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param request - Table creation request with moduleName, sheetName, and complete table structure
   * @returns Created table summary with table ID
   */
  async createProjectTable(
    projectId: string,
    request: Types.CreateNewTableRequest
  ): Promise<Types.TableMetadata> {
    const projectPath = this.buildProjectPath(projectId);

    const response = await this.axiosInstance.post<Types.TableMetadata>(
      `${projectPath}/tables`,
      {
        moduleName: request.moduleName,
        modulePath: request.modulePath,
        sheetName: request.sheetName,
        table: request.table,
      }
    );

    return response.data;
  }

  /**
   * Get detailed table data and structure
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param tableId - Table identifier
   * @param options - Read the matrix in row slices
   *   (`startRow`/`maxRows`) and/or with per-cell Excel styles (`styles`)
   * @returns The table's raw source matrix
   */
  async getTable(projectId: string, tableId: string, options?: Types.RawTableViewOptions): Promise<Types.RawTableView> {
    const projectPath = this.buildProjectPath(projectId);
    const params = {
      raw: true,
      ...(options?.startRow !== undefined && { startRow: options.startRow }),
      ...(options?.maxRows !== undefined && { maxRows: options.maxRows }),
      ...(options?.styles && { styles: true }),
    };
    const response = await this.axiosInstance.get<Types.RawTableView>(
      `${projectPath}/tables/${encodeURIComponent(tableId)}`,
      { params }
    );
    return response.data;
  }

  /**
   * Extract the table's post-write id reported by an update/append response.
   *
   * Studio PR #1778 (EPBDS-16086): a write that RELOCATES the table (it had no
   * room to grow in place, so it moved to a free area and its content/position-
   * derived id changed) responds 200 with body `{ id }` and a `Location` header
   * pointing at the table under its new id; an in-place write responds 204.
   * Studios without that change always respond 204/empty.
   *
   * @returns the new table id when the studio reported one, else undefined
   *   (the id is unchanged on a current studio, or unknown on an older one —
   *   the caller falls back to resolving it heuristically).
   */
  private parseWrittenTableId(response: AxiosResponse): string | undefined {
    const body = response.data as { id?: unknown } | undefined;
    if (body && typeof body.id === "string" && body.id.length > 0) {
      return body.id;
    }
    // Fallback: last `/tables/{id}` segment of the Location header. Prefer the
    // body above — Location is request-derived and a reverse proxy may rewrite it.
    const headers = (response.headers ?? {}) as Record<string, unknown>;
    const location = headers.location ?? headers.Location;
    if (typeof location === "string") {
      const match = location.match(/\/tables\/([^/?#]+)/);
      if (match) {
        return decodeURIComponent(match[1]);
      }
    }
    return undefined;
  }

  /**
   * Update table content
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param tableId - Table identifier
   * @param view - Complete raw table source
   * @returns the table's id after the write when the studio relocated it (id
   *   changed), otherwise undefined (204 — id unchanged, or older studio)
   * @throws Error if an optional body id does not match the path table id
   */
  async updateTable(
    projectId: string,
    tableId: string,
    view: Types.RawTableView
  ): Promise<string | undefined> {
    validateTableIdMatch(tableId, view.id);

    const projectPath = this.buildProjectPath(projectId);
    // Studio accepts RawTableView directly as the request body.
    const response = await this.axiosInstance.put(
      `${projectPath}/tables/${encodeURIComponent(tableId)}`,
      view
    );
    // 204 No Content when the id is unchanged; 200 + { id } + Location when relocated.
    return this.parseWrittenTableId(response);
  }

  /**
   * Append lines to a project table
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param tableId - Table identifier
   * @param appendData - Raw source rows to append
   * @returns the table's id after the append when the studio relocated it (id
   *   changed), otherwise undefined (204 — id unchanged, or older studio)
   */
  async appendProjectTable(
    projectId: string,
    tableId: string,
    appendData: Types.RawTableAppend
  ): Promise<string | undefined> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.post(
      `${projectPath}/tables/${encodeURIComponent(tableId)}/lines`,
      appendData
    );
    return this.parseWrittenTableId(response);
  }

  /**
   * Apply a single in-place edit to a table's raw source (append, insert,
   * delete, update, merge or unmerge a row/column/cell). The table is handled
   * in raw format regardless of its type.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param tableId - Table identifier
   * @param action - The edit to apply (operation + target)
   * @returns the table's id after the edit when the studio relocated it (id
   *   changed), otherwise undefined (204 — id unchanged)
   */
  async editTableSource(
    projectId: string,
    tableId: string,
    action: Types.RawTableSourceAction
  ): Promise<string | undefined> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.post(
      `${projectPath}/tables/${encodeURIComponent(tableId)}/actions`,
      action
    );
    // 204 No Content when the id is unchanged; 200 + { id } + Location when relocated.
    return this.parseWrittenTableId(response);
  }

  /**
   * Delete a table from the currently opened project. The whole table area is
   * cleared from the sheet regardless of table type, so the table no longer
   * exists once the project is recompiled.
   *
   * @param projectId - Opaque project ID returned by backend.
   * @param tableId - Table identifier
   * @returns nothing (204 No Content on success)
   */
  async deleteTable(projectId: string, tableId: string): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.delete(
      `${projectPath}/tables/${encodeURIComponent(tableId)}`
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
  async listDeployments(filters?: { repository?: string; project?: string }): Promise<Types.DeploymentViewModel_Short[]> {
    const response = await this.axiosInstance.get<Types.DeploymentViewModel_Short[]>(
      "/deployments",
      { params: filters && Object.values(filters).some((value) => value !== undefined) ? filters : undefined }
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
    await this.axiosInstance.post(
      "/deployments",
      {
        projectId: request.projectId,
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
    await this.axiosInstance.post(
      `/deployments/${encodeURIComponent(deploymentId)}`,
      {
        projectId: request.projectId,
        comment: request.comment,
      }
    );
  }

  /** Get one named module's local edit history from an opened project. */
  async getProjectLocalChanges(
    projectId: string,
    moduleName: string
  ): Promise<Types.ProjectHistoryItem[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.ProjectHistoryItem[]>(
      `${projectPath}/local-history`,
      { params: { module: moduleName } }
    );
    return response.data;
  }

  /** Restore a named module from one opened project's local edit history. */
  async restoreProjectLocalChange(
    projectId: string,
    moduleName: string,
    historyId: string
  ): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(
      `${projectPath}/local-history/restore`,
      { version: historyId },
      { params: { module: moduleName } }
    );
  }

  // =============================================================================
  // Test Execution Session Management
  // =============================================================================

  /**
   * Store test execution headers for a project.
   * Always keyed by projectId only — a project can have only one active test session.
   *
   * @param projectId - Project ID
   * @param headers - Headers from test start response
   */
  private storeTestExecutionHeaders(
    projectId: string,
    headers: Record<string, string>
  ): void {
    this.testExecutionHeaders.set(projectId, headers);
  }

  /**
   * Get test execution headers for a project
   *
   * @param projectId - Project ID
   * @returns Headers if found, undefined otherwise
   */
  private getTestExecutionHeaders(
    projectId: string
  ): Record<string, string> | undefined {
    return this.testExecutionHeaders.get(projectId);
  }

  /**
   * Clear test execution headers for a project
   *
   * @param projectId - Project ID
   */
  private clearTestExecutionHeaders(projectId: string): void {
    this.testExecutionHeaders.delete(projectId);
  }

  /**
   * Extract headers from test start response
   *
   * @param headers - Response headers from axios
   * @returns Extracted headers ready for use in subsequent requests
   */
  private extractTestExecutionHeaders(headers: Record<string, unknown>): Record<string, string> {
    const responseHeaders: Record<string, string> = {};
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
      'accept',
    ];

    const setCookieValues: string[] = [];

    Object.keys(headers).forEach((key) => {
      const lowerKey = key.toLowerCase();

      if (lowerKey === 'set-cookie') {
        const value = headers[key];
        if (value !== undefined && value !== null) {
          const cookies = Array.isArray(value) ? value : [String(value)];
          cookies.forEach((cookie) => {
            const nameValue = cookie.split(';')[0].trim();
            if (nameValue) {
              setCookieValues.push(nameValue);
            }
          });
        }
      } else if (!excludeHeaders.includes(lowerKey)) {
        const value = headers[key];
        if (value !== undefined && value !== null) {
          responseHeaders[key] = Array.isArray(value) ? value.join(", ") : String(value);
        }
      }
    });

    if (setCookieValues.length > 0) {
      responseHeaders['Cookie'] = setCookieValues.join('; ');
    }

    return responseHeaders;
  }

  // =============================================================================
  // New Test Execution Methods
  // =============================================================================

  /**
   * Start project tests execution
   *
   * For design repositories: ensures project is opened before starting tests; automatically opens if closed.
   * For repository 'local': does not open the project; runs tests directly (local projects are always editable).
   *
   * @param projectId - Project ID
   * @param options - Test execution options
   * @returns Test execution start response
   * @throws Error if test execution fails
   */
  async startProjectTests(
    projectId: string,
    options?: {
      tableId?: string;
      testRanges?: string;
      fromModule?: string;
    }
  ): Promise<Types.TestExecutionStartResponse> {
    const projectPath = this.buildProjectPath(projectId);

    // Local projects are always editable — skip open; for design repos open if needed
    let projectWasOpened = false;
    let needsOpen = false;
    try {
      const project = await this.getProject(projectId);
      needsOpen = project.repository !== REPOSITORY_LOCAL &&
                  project.status !== "OPENED" && project.status !== "EDITING";
    } catch {
      // getProject failed — attempt open anyway (will throw for local with a clear message)
      needsOpen = true;
    }
    if (needsOpen) {
      try {
        await this.openProject(projectId);
        projectWasOpened = true;
      } catch (openError) {
        throw new Error(`Failed to open project: ${sanitizeError(openError)}.`);
      }
    }

    // Clear old headers for this project before storing new ones
    this.clearTestExecutionHeaders(projectId);

    // Build API parameters
    const params: Record<string, string | number | boolean> = {};
    if (options?.tableId) params.tableId = options.tableId;
    if (options?.testRanges) params.testRanges = options.testRanges;
    if (options?.fromModule) params.fromModule = options.fromModule;

    // Start test execution
    const startResponse = await this.axiosInstance.post(
      `${projectPath}/tests/run`,
      undefined,
      { params }
    );

    // Extract and store headers
    const responseHeaders = this.extractTestExecutionHeaders(startResponse.headers || {});
    this.storeTestExecutionHeaders(projectId, responseHeaders);

    return {
      status: "started",
      projectId,
      tableId: options?.tableId,
      testRanges: options?.testRanges,
      projectWasOpened,
      message: `Test execution started${projectWasOpened ? " (project was automatically opened)" : ""}`,
    };
  }

  /**
   * Get test results summary (without testCases array)
   *
   * @param projectId - Project ID
   * @param options - Summary options
   * @returns Test results summary, or `notReady` while the execution is still running
   * @throws Error if headers not found or request fails
   */
  async getTestResultsSummary(
    projectId: string,
    options?: {
      failuresOnly?: boolean;
      failures?: number;
      unpaged?: boolean;
      signal?: AbortSignal;
      timeoutMs?: number;
    }
  ): Promise<Types.TestResultsSummary | Types.ResultNotReadyView> {
    const projectPath = this.buildProjectPath(projectId);
    const headers = this.getTestExecutionHeaders(projectId);

    if (!headers) {
      throw new Error(
        `No test execution session found for project '${projectId}'. ` +
        `Use openl_start_project_tests() to start test execution first.`
      );
    }

    const params: Record<string, string | number | boolean> = {};
    if (options?.failuresOnly !== undefined) params.failuresOnly = options.failuresOnly;
    if (options?.failures !== undefined) params.failures = options.failures;
    if (options?.unpaged) params.unpaged = true;

    const response = await this.axiosInstance.get<Types.TestsExecutionSummary | Types.ResultNotReadyView>(
      `${projectPath}/tests/summary`,
      {
        params,
        headers: {
          ...headers,
          "Accept": "application/json",
        },
        signal: options?.signal,
        timeout: options?.timeoutMs,
      }
    );

    if (response.status === 202) {
      return { status: "notReady" };
    }
    if (isResultNotReady(response.data)) {
      return response.data;
    }
    const summary = response.data;
    const numberOfPassed = summary.numberOfTests - summary.numberOfFailures;

    return {
      executionTimeMs: summary.executionTimeMs,
      numberOfTests: summary.numberOfTests,
      numberOfFailures: summary.numberOfFailures,
      numberOfPassed,
    };
  }

  /**
   * Get full test results with pagination
   *
   * @param projectId - Project ID
   * @param options - Result options including pagination
   * @returns Full test execution summary with testCases, or `notReady` while execution is still running
   * @throws Error if headers not found or request fails
   */
  async getTestResults(
    projectId: string,
    options?: {
      failuresOnly?: boolean;
      failures?: number;
      page?: number;
      offset?: number;
      size?: number;
      limit?: number; // Alias for size
      unpaged?: boolean;
      signal?: AbortSignal;
      timeoutMs?: number;
    }
  ): Promise<Types.TestsExecutionSummary | Types.ResultNotReadyView> {
    const projectPath = this.buildProjectPath(projectId);
    const headers = this.getTestExecutionHeaders(projectId);

    if (!headers) {
      throw new Error(
        `No test execution session found for project '${projectId}'. ` +
        `Use openl_start_project_tests() to start test execution first.`
      );
    }

    const params: Record<string, string | number | boolean> = {};
    if (options?.failuresOnly) params.failuresOnly = true;
    if (options?.failures !== undefined) params.failures = options.failures;
    if (options?.page !== undefined) params.page = options.page;
    if (options?.offset !== undefined) params.offset = options.offset;
    if (options?.size !== undefined) params.size = options.size;
    else if (options?.limit !== undefined) params.size = options.limit; // Map limit to size
    if (options?.unpaged) params.unpaged = true;

    const response = await this.axiosInstance.get<Types.TestsExecutionSummary | Types.ResultNotReadyView>(
      `${projectPath}/tests/summary`,
      {
        params,
        headers: {
          ...headers,
          "Accept": "application/json",
        },
        signal: options?.signal,
        timeout: options?.timeoutMs,
      }
    );

    if (response.status === 202) {
      return { status: "notReady" };
    }
    return response.data;
  }

  /**
   * Get test results filtered by table ID
   *
   * @param projectId - Project ID
   * @param tableId - Table ID to filter results
   * @param options - Result options
   * @returns Filtered test execution summary, or `notReady` while execution is still running
   * @throws Error if headers not found or request fails
   */
  async getTestResultsByTable(
    projectId: string,
    tableId: string,
    options?: {
      failuresOnly?: boolean;
      failures?: number;
      page?: number;
      offset?: number;
      size?: number;
      limit?: number;
      unpaged?: boolean;
      signal?: AbortSignal;
      timeoutMs?: number;
    }
  ): Promise<Types.TestsExecutionSummary | Types.ResultNotReadyView> {
    if (options?.unpaged) {
      const unpagedResults = await this.getTestResults(projectId, {
        failuresOnly: options.failuresOnly,
        failures: options.failures,
        unpaged: true,
        signal: options.signal,
        timeoutMs: options.timeoutMs,
      });
      if (isResultNotReady(unpagedResults)) {
        return unpagedResults;
      }
      const filteredTestCases = (unpagedResults.testCases || []).filter(
        (testCase) => testCase.tableId === tableId
      );
      const numberOfTests = filteredTestCases.reduce(
        (sum, tc) => sum + tc.numberOfTests,
        0
      );
      const numberOfFailures = filteredTestCases.reduce(
        (sum, tc) => sum + tc.numberOfFailures,
        0
      );

      return {
        ...unpagedResults,
        testCases: filteredTestCases,
        numberOfTests,
        numberOfFailures,
      };
    }

    // Collect all test results across pages, then filter by tableId.
    // Pagination options from the caller are applied AFTER filtering, to avoid
    // missing the requested table when it is not on the selected page.
    const baseOptions = {
      failuresOnly: options?.failuresOnly,
      failures: options?.failures,
      // Use caller's size/limit only as page size when iterating pages.
      size: options?.size,
      limit: options?.limit,
      signal: options?.signal,
      timeoutMs: options?.timeoutMs,
    };
    let pageIndex = 0;
    let templateSummary: Types.TestsExecutionSummary | null = null;
    const allMatchingTestCases: Types.TestCaseExecutionResult[] = [];

    // Iterate pages until no more test cases are returned.
    // We do not use caller's page/offset here to ensure we scan all tables.
    const pageSize = baseOptions.size ?? baseOptions.limit ?? 50;

    while (true) {
      const pageResults = await this.getTestResults(projectId, {
        ...baseOptions,
        size: pageSize,
        page: pageIndex,
      });
      if (isResultNotReady(pageResults)) {
        return pageResults;
      }
      if (!templateSummary) {
        templateSummary = pageResults;
      }

      // Stop if no test cases returned
      if (!pageResults.testCases || pageResults.testCases.length === 0) {
        break;
      }

      const pageMatches = pageResults.testCases.filter(
        (testCase) => testCase.tableId === tableId
      );
      allMatchingTestCases.push(...pageMatches);

      // The current TestsExecutionSummary reports current-page counts, not a
      // totalPages field. A short page terminates the scan.
      const hasMorePages = pageResults.numberOfElements >= pageSize;

      if (!hasMorePages) {
        break;
      }

      pageIndex += 1;

      // Safety limit: prevent infinite loops (max 1000 pages)
      if (pageIndex >= 1000) {
        break;
      }
    }

    if (!templateSummary) {
      // No pages returned any results; construct an empty summary shape by
      // calling getTestResults once (without pagination options).
      const fallbackSummary = await this.getTestResults(projectId, {
        failuresOnly: options?.failuresOnly,
        failures: options?.failures,
        signal: options?.signal,
        timeoutMs: options?.timeoutMs,
      });
      if (isResultNotReady(fallbackSummary)) {
        return fallbackSummary;
      }
      templateSummary = fallbackSummary;
    }

    // Apply caller's pagination options within the filtered test cases.
    let pagedTestCases = allMatchingTestCases;
    const hasPaginationOptions =
      options?.page !== undefined ||
      options?.offset !== undefined ||
      options?.size !== undefined ||
      options?.limit !== undefined;

    if (hasPaginationOptions && allMatchingTestCases.length > 0) {
      const pageSize = options?.size ?? options?.limit;
      let start = 0;
      if (options?.offset !== undefined) {
        start = options.offset;
      } else if (options?.page !== undefined && pageSize !== undefined) {
        start = options.page * pageSize;
      }
      const end = pageSize !== undefined ? start + pageSize : undefined;
      pagedTestCases = allMatchingTestCases.slice(start, end);
    }

    const numberOfTests = pagedTestCases.reduce(
      (sum, tc) => sum + tc.numberOfTests,
      0
    );
    const numberOfFailures = pagedTestCases.reduce(
      (sum, tc) => sum + tc.numberOfFailures,
      0
    );

    return {
      ...templateSummary,
      testCases: pagedTestCases,
      numberOfTests,
      numberOfFailures,
    };
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
  // Trace Debug API (BETA) — interactive debugger
  //
  // The debug session is server-side and bound to the HTTP session (JSESSIONID):
  // the cookie interceptors above carry the same session across all calls of one
  // debug flow. One active session per user; starting a new one terminates the
  // previous.
  // =============================================================================

  /**
   * Start an interactive debug session for a table and run it to the first
   * suspension (the table entry when stopAtEntry, otherwise the first breakpoint)
   * or to a terminal state. Returns the initial execution stack.
   *
   * For TestSuiteMethod: use testRanges (e.g. "1-3,5").
   * For regular methods: use inputJson with { params: {...}, runtimeContext?: {...} }.
   * A restart with neither re-runs the remembered last input (replay).
   */
  async startTrace(request: Types.StartTraceRequest): Promise<Types.DebugStackView> {
    const projectPath = this.buildProjectPath(request.projectId);
    const params = new URLSearchParams({ tableId: request.tableId });
    if (request.testRanges) params.set("testRanges", request.testRanges);
    if (request.fromModule) params.set("fromModule", request.fromModule);
    if (request.stopAtEntry != null) params.set("stopAtEntry", String(request.stopAtEntry));
    if (request.profiling != null) params.set("profiling", String(request.profiling));
    if (request.detailedTitles != null) params.set("detailedTitles", String(request.detailedTitles));
    if (request.breakOnErrors != null) params.set("breakOnErrors", String(request.breakOnErrors));
    if (request.includeTree != null) params.set("includeTree", String(request.includeTree));
    if (request.profileTop != null) params.set("profileTop", String(request.profileTop));

    const body = request.inputJson != null
      ? (typeof request.inputJson === "string" ? request.inputJson : JSON.stringify(request.inputJson))
      : undefined;

    const response = await this.axiosInstance.post<Types.DebugStackView>(
      `${projectPath}/trace?${params.toString()}`,
      body,
      body != null ? { headers: { "Content-Type": "application/json" } } : undefined
    );
    return response.data;
  }

  /**
   * Lightweight status poll of the debug session.
   */
  async getTraceStatus(projectId: string): Promise<Types.DebugStatusView> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.DebugStatusView>(
      `${projectPath}/trace/status`
    );
    return response.data;
  }

  /**
   * Read the execution stack (frames root → current). Readable while suspended
   * or terminal; 409 while the worker is still running. `view: "compact"` keeps
   * steps only on the active frame; `includeTree`/`profileTop` shape a
   * profiling run's terminal `tree`/`profile`.
   */
  async getTraceStack(
    projectId: string,
    options?: { view?: "full" | "compact"; includeTree?: boolean; profileTop?: number }
  ): Promise<Types.DebugStackView> {
    const projectPath = this.buildProjectPath(projectId);
    const params: Record<string, string> = {};
    if (options?.view) params.view = options.view;
    if (options?.includeTree != null) params.includeTree = String(options.includeTree);
    if (options?.profileTop != null) params.profileTop = String(options.profileTop);
    const response = await this.axiosInstance.get<Types.DebugStackView>(
      `${projectPath}/trace/stack`,
      { params: Object.keys(params).length ? params : undefined }
    );
    return response.data;
  }

  /**
   * One page of a step's executed sub-calls — the profiling call tree loaded one
   * level at a time instead of all at once. Address the parent node by its `uri`
   * + `instance` (from the `tree` root or an earlier page) and name the `step`
   * (its `ref`); page a loop's many sub-calls with `offset`/`limit`. Each child
   * comes back shallow — its steps carry `childrenTotal`, expanded by the same
   * call. Available only for a profiling run (only then is the tree retained).
   */
  async getTraceTreeChildren(
    projectId: string,
    options: {
      uri: string;
      instance: number;
      step: string;
      offset?: number;
      limit?: number;
    }
  ): Promise<Types.TreeChildrenView> {
    const projectPath = this.buildProjectPath(projectId);
    // Pass every value through axios `params` so the URI's own query characters
    // (?sheet=…&range=…) are percent-encoded rather than merged into the path.
    const params: Record<string, string> = {
      uri: options.uri,
      instance: String(options.instance),
      step: options.step,
    };
    if (options.offset != null) params.offset = String(options.offset);
    if (options.limit != null) params.limit = String(options.limit);
    const response = await this.axiosInstance.get<Types.TreeChildrenView>(
      `${projectPath}/trace/tree/children`,
      { params }
    );
    return response.data;
  }

  /**
   * Step once (into / over / out) and return the new stack once the worker
   * re-suspends (the backend waits synchronously, bounded ~30s). `view:
   * "compact"` (the tool default) keeps steps only on the active frame.
   */
  async traceStep(
    projectId: string,
    type: "into" | "over" | "out",
    options?: { view?: "full" | "compact"; includeTree?: boolean; profileTop?: number }
  ): Promise<Types.DebugStackView> {
    const projectPath = this.buildProjectPath(projectId);
    const params: Record<string, string> = { type };
    if (options?.view) params.view = options.view;
    if (options?.includeTree != null) params.includeTree = String(options.includeTree);
    if (options?.profileTop != null) params.profileTop = String(options.profileTop);
    const response = await this.axiosInstance.post<Types.DebugStackView>(
      `${projectPath}/trace/step`,
      undefined,
      { params }
    );
    return response.data;
  }

  /**
   * Resume execution to the next breakpoint or completion. Asynchronous:
   * returns 202 immediately — poll getTraceStatus until it leaves running,
   * then read getTraceStack.
   */
  async traceResume(projectId: string): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.post(`${projectPath}/trace/resume`);
  }

  /**
   * Freeze and read the variables of a suspended frame. `fields` is the
   * standard response projection (nested selection supported) used to keep
   * value schemas and other bulk out of the agent's token budget.
   */
  async getTraceFrameVariables(
    projectId: string,
    frameIndex: number,
    fields?: string,
    includeSchema: boolean = false,
  ): Promise<Types.DebugFrameVariables> {
    const projectPath = this.buildProjectPath(projectId);
    const params = { ...(fields ? { fields } : {}), ...(includeSchema ? { includeSchema: true } : {}) };
    const response = await this.axiosInstance.get<Types.DebugFrameVariables>(
      `${projectPath}/trace/frames/${frameIndex}/variables`,
      { params: Object.keys(params).length ? params : undefined }
    );
    return response.data;
  }

  /**
   * Execution highlight overlay for a frame's table, keyed by A1 cell address.
   * Merge with the raw table grid (getTable(..., raw: true)).
   */
  async getTraceFrameHighlights(
    projectId: string,
    frameIndex: number
  ): Promise<Types.CellHighlight[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.CellHighlight[]>(
      `${projectPath}/trace/frames/${frameIndex}/highlights`
    );
    return response.data;
  }

  /**
   * Active breakpoint keys. Session-scoped, persist across runs, work without
   * an active debug session.
   */
  async getTraceBreakpoints(projectId: string): Promise<string[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<string[]>(
      `${projectPath}/trace/breakpoints`
    );
    return response.data;
  }

  /**
   * Replace the whole breakpoint set (effective on the next frame enter /
   * current-line change).
   */
  async setTraceBreakpoints(projectId: string, uris: string[]): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.put(`${projectPath}/trace/breakpoints`, { uris });
  }

  /**
   * Rule tables a breakpoint can be set on, deduplicated by name. With an
   * active session only tables reachable from the traced table are returned.
   */
  async getTraceBreakpointTables(projectId: string): Promise<Types.BreakpointTableView[]> {
    const projectPath = this.buildProjectPath(projectId);
    const response = await this.axiosInstance.get<Types.BreakpointTableView[]>(
      `${projectPath}/trace/breakpoint-tables`
    );
    return response.data;
  }

  /**
   * Get lazy-loaded parameter value. `fields` is the standard response
   * projection — used to drop the value's JSON Schema from the default reply.
   */
  async getTraceParameter(
    projectId: string,
    parameterId: number,
    fields?: string,
    includeSchema: boolean = false,
  ): Promise<Types.TraceParameterValue> {
    const projectPath = this.buildProjectPath(projectId);
    const params = { ...(fields ? { fields } : {}), ...(includeSchema ? { includeSchema: true } : {}) };
    const response = await this.axiosInstance.get<Types.TraceParameterValue>(
      `${projectPath}/trace/parameters/${parameterId}`,
      { params: Object.keys(params).length ? params : undefined }
    );
    return response.data;
  }

  /**
   * Replace the watched-cell set (applied on the next start). Watches capture a
   * named cell's value at every execution of its table across a whole run.
   */
  async setTraceWatches(projectId: string, cells: string[]): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.put(`${projectPath}/trace/watches`, { cells });
  }

  /**
   * Collected watched-cell values — one series per cell, one point per execution
   * of its table. Read after a run completes (409 while still running). `fields`
   * is the standard response projection — used to drop each point value's JSON
   * Schema from the default reply.
   */
  async getTraceWatch(
    projectId: string,
    fields?: string,
    includeSchema: boolean = false,
  ): Promise<Types.WatchView> {
    const projectPath = this.buildProjectPath(projectId);
    const params = { ...(fields ? { fields } : {}), ...(includeSchema ? { includeSchema: true } : {}) };
    const response = await this.axiosInstance.get<Types.WatchView>(
      `${projectPath}/trace/watch`,
      { params: Object.keys(params).length ? params : undefined }
    );
    return response.data;
  }

  /**
   * Terminate the debug session and clear the parameter registry. Idempotent.
   */
  async stopTrace(projectId: string): Promise<void> {
    const projectPath = this.buildProjectPath(projectId);
    await this.axiosInstance.delete(`${projectPath}/trace`);
  }

}
