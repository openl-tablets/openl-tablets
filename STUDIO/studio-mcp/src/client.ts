/**
 * OpenL Studio API Client: the REST calls the tools of the server make, on the HTTP client of `http.ts`.
 */

import { buildUrl, HttpClient, type HttpResponse } from "./http.js";
import type * as Types from "./types.js";
import { authorizationHeaders, callerHeaders, logUnauthorized } from "./auth.js";
import { isResultNotReady } from "./utils.js";
import { trimChar } from "./text.js";

/** Milliseconds a request to OpenL Studio may take unless it names its own timeout. */
const REQUEST_TIMEOUT_MS = 30_000;

/**
 * The query of the status of a project in short: the project with its status, kept to the compile state and the
 * change on disk. Studio builds no field `fields` leaves out, so neither the projects using this one nor the messages
 * of the compilation are looked for.
 */
export const STATUS_SUMMARY_QUERY = {
  include: ["status"],
  fields: "compileStatus(compileState,changedOnDisk)",
} as const;

/** What the status in short tells: nothing at all for a project Studio answers with no status. */
export type ProjectStatusSummary = Partial<Pick<Types.ProjectStatusView, "compileState" | "changedOnDisk">>;

/**
 * Repository identifier for local (non-remote) repositories.
 * Projects in repository "local" are stored only on the server; status change (open/save/close) is not supported by
 * the API.
 */
const REPOSITORY_LOCAL = "local";

/** Error message when an operation is attempted on a project in a local repository */
const ERROR_LOCAL_REPOSITORY =
  "Project is in a local repository (repository: 'local'). " +
  "Local repositories are not connected to a remote Git; " +
  "changing project status (open, save, close) is not supported. " +
  "Use projects from a design repository connected to a remote Git.";

/**
 * Ensure OpenL API base URL points to REST endpoint.
 *
 * Adds `/rest` suffix:
 * - `http://host:8080` -> `http://host:8080/rest`
 * - `http://host:8080/studio` -> `http://host:8080/studio/rest`
 *
 * Keeps URL unchanged when path already ends with `/rest`.
 *
 * @param baseUrl - The address of OpenL Studio
 * @returns Normalized URL with `/rest` suffix
 */
export function normalizeOpenLBaseUrl(baseUrl: string): string {
  const url = URL.parse(baseUrl);
  if (!url) {
    throw new Error(
      `Invalid address of OpenL Studio: "${baseUrl}". Must be a valid absolute URL (e.g., "http://localhost:8080").`
    );
  }
  const pathWithoutTrailingSlashes = trimChar(url.pathname, "/", false);

  if (!pathWithoutTrailingSlashes.endsWith("/rest")) {
    url.pathname = `${pathWithoutTrailingSlashes}/rest`;
  }

  return url.toString().replace(/\/$/, "");
}

/** The bytes of a file answer, with the type and the disposition OpenL Studio gave them. */
function toFileContent(response: HttpResponse<ArrayBuffer>): Types.ProjectFileResponse {
  return {
    data: Buffer.from(response.data),
    contentType: (response.headers["content-type"] ?? "").toLowerCase(),
  };
}

/** The version an `ETag` header names, its quotes and a weak prefix left out; none without the header. */
function versionOf(entityTag: string | undefined): string | undefined {
  return entityTag?.replace(/^W\//, "").replace(/^"(.*)"$/, "$1") || undefined;
}

/** The entity tag of a version, as an `If-Match` header names it. */
export function entityTag(version: string): string {
  return `"${version}"`;
}

/**
 * The header naming the version a write is based on, so that Studio refuses a write over what changed since it was
 * read; none when the version is not known, and the write is then not compared.
 */
function ifMatch(version: string | undefined): Record<string, string> {
  return version === undefined ? {} : { "If-Match": entityTag(version) };
}

/** The query of a projected read: the fields asked for, or else the whole reply with the JSON Schemas of its values. */
function projection(fields: string | undefined): Record<string, unknown> {
  return fields ? { fields } : { includeSchema: true };
}

/** The query parameters of a map Studio takes with a prefix, such as `tags.<name>`. */
function prefixed(prefix: string, values: Record<string, unknown> | undefined): Record<string, unknown> {
  return Object.fromEntries(Object.entries(values ?? {}).map(([key, value]) => [`${prefix}.${key}`, value]));
}

/** The query of a trace stack read: its compact view, which keeps steps only on the active frame, and the options. */
function traceStackParams(options?: Types.TraceStackOptions): Record<string, unknown> {
  return { view: "compact", includeTree: options?.includeTree, profileTop: options?.profileTop };
}

/** The path of a project in the Studio API, by its opaque id. */
/** The repository a user-supplied id or name names: the exact id, the exact name, then either ignoring case. */
function matchRepository<R extends { id: string; name: string }>(repositories: R[], idOrName: string): R | undefined {
  const needle = idOrName.toLowerCase();
  return repositories.find(r => r.id === idOrName)
    ?? repositories.find(r => r.name === idOrName)
    ?? repositories.find(r => r.id.toLowerCase() === needle)
    ?? repositories.find(r => r.name.toLowerCase() === needle);
}

function projectPath(projectId: string): string {
  return `/projects/${encodeURIComponent(projectId)}`;
}

/** The path of a repository in the Studio API. */
function repositoryPath(repositoryId: string): string {
  return `/repos/${encodeURIComponent(repositoryId)}`;
}

/** The path of a table of a project in the Studio API. */
function tablePath(projectId: string, tableId: string): string {
  return `${projectPath(projectId)}/tables/${encodeURIComponent(tableId)}`;
}

/** Encode a slash-delimited trailing path while preserving its separators. */
function encodePathSegments(value: string): string {
  assertSafeProjectPath(value);
  return value.split("/").map(encodeURIComponent).join("/");
}

/**
 * Percent-encode each segment of a project-relative file path while preserving
 * '/' separators and any trailing slash (which denotes a folder to the API).
 * Leading slashes are dropped — paths are always project-relative.
 */
function encodeProjectFilePath(path: string): string {
  const relative = normalizeBodyPath(path);
  const encoded = relative.split("/").filter((seg) => seg.length > 0).map(encodeURIComponent).join("/");
  return relative.endsWith("/") ? `${encoded}/` : encoded;
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
function assertSafeProjectPath(path: string): void {
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
function normalizeBodyPath(path: string): string {
  const normalized = path.replace(/^\/+/, "");
  assertSafeProjectPath(normalized);
  return normalized;
}

/** The items of a page of OpenL Studio, with any other fields of the page as its metadata. */
function toCollectionPage<T>(page: Types.PageResponse<T>): Types.CollectionPage<T> {
  const { content, pageNumber, pageSize, total, numberOfElements, ...metadata } = page;
  return {
    items: content,
    pageNumber,
    pageSize,
    total,
    ...(Object.keys(metadata).length > 0 ? { metadata } : {}),
  };
}

/**
 * Client for OpenL Studio REST API
 *
 * Usage:
 * ```typescript
 * const client = new OpenLClient({
 *   baseUrl: "http://localhost:8080",
 *   authorization: "Token openl_pat_…"
 * });
 *
 * const page = await client.listProjectsPage();
 * ```
 */
export class OpenLClient {
  /** The address of OpenL Studio its users open, ending with a slash. */
  readonly publicUrl: string;
  private readonly http: HttpClient;
  private repositoriesCache: Types.Repository[] | null = null;
  /** The address of the client of the MCP server the calls are made for. */
  private clientAddress: string | undefined;
  /** The MCP client the calls are made for, once it is known. */
  private mcpClient: Types.McpClientInfo | undefined;

  /**
   * Create a new OpenL Studio API client
   *
   * @param config - Client configuration including base URL and authentication
   */
  constructor(config: Types.OpenLConfig) {
    const baseUrl = normalizeOpenLBaseUrl(config.baseUrl);
    // The base the client calls ends with /rest: its parent is the address of Studio.
    this.publicUrl = config.publicUrl ?? new URL(".", baseUrl).href;
    this.clientAddress = config.clientAddress;
    this.mcpClient = config.mcpClient;

    // The HTTP client of the Studio REST API. Studio keeps the state of the client for its credential, so the
    // client keeps no session cookie.
    this.http = new HttpClient({
      baseUrl,
      timeout: REQUEST_TIMEOUT_MS,
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json",
        ...authorizationHeaders(config.authorization),
      },
      // Read for every call: a session of the 2025 protocol may go on from another address, and a client of the
      // stateless protocol names itself in each of its requests
      headersOf: () => callerHeaders(config.mcpSecret, this.clientAddress, this.mcpClient),
      onError: (error) => logUnauthorized(error, config.authorization),
    });
  }

  /**
   * Names the address the client of the MCP server calls from now, as OpenL Studio passed it with the request. A
   * request that names none keeps the address the client had.
   */
  callsFrom(clientAddress: string | undefined): void {
    this.clientAddress = clientAddress ?? this.clientAddress;
  }

  /**
   * Names the MCP client the calls are made for: the name and the version the client reported to the server, which
   * OpenL Studio records with what the client does. A client that names nothing keeps the name it had.
   */
  callsFor(client: Types.McpClientInfo | undefined): void {
    this.mcpClient = client ?? this.mcpClient;
  }

  // =============================================================================
  // Server Info
  // =============================================================================

  /** The build of OpenL Studio: its version, build number and start time. */
  async getBuildInfo(): Promise<Types.StudioBuildInfo> {
    return (await this.http.get<Types.StudioBuildInfo>("/public/info/build.json")).data;
  }

  /** The machine and the JVM OpenL Studio runs on: the time, the system, Java, CPUs, threads and memory. */
  async getSysInfo(): Promise<Types.StudioSysInfo> {
    return (await this.http.get<Types.StudioSysInfo>("/public/info/sys.json")).data;
  }

  // =============================================================================
  // User Guides — served by OpenL Studio at /docs, beside the REST API
  // =============================================================================

  /** The table of contents of the user guides OpenL Studio serves. */
  async getUserGuideContents(): Promise<Types.UserGuideEntry> {
    return (await this.http.get<Types.UserGuideEntry>("../docs/toc.json")).data;
  }

  /**
   * The text of a file of the user guides, by its path from the address of OpenL Studio, such as
   * `docs/ref/rules.xml.md`.
   */
  async readUserGuide(path: string): Promise<string> {
    const response = await this.getBytes(`../${path}`);
    return new TextDecoder().decode(response.data);
  }

  // =============================================================================
  // Repository Management
  // =============================================================================

  /**
   * List all design repositories, once for the client
   *
   * @returns Array of repository information
   */
  async listRepositories(): Promise<Types.Repository[]> {
    this.repositoriesCache ??= (await this.http.get<Types.Repository[]>("/repos")).data;
    return this.repositoriesCache;
  }

  /**
   * The id of the design repository an id or a display name names, matched exactly first, then case-insensitively:
   * the tools take either, as agents pass whichever of the two they see in `openl_list_repositories`.
   *
   * @throws Error if no repository matches
   */
  async getRepositoryIdByName(repositoryIdOrName: string): Promise<string> {
    return (await this.findRepository(repositoryIdOrName)).id;
  }

  /** The design repository a user-supplied id or name names, matched as {@link getRepositoryIdByName} tells. */
  private async findRepository(repositoryIdOrName: string): Promise<Types.Repository> {
    const repositories = await this.listRepositories();
    const match = matchRepository(repositories, repositoryIdOrName);
    if (match) return match;

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
    return (await this.http.get<string[]>(
      `${repositoryPath(repository)}/branches`
    )).data;
  }

  /**
   * List deployment repositories
   *
   * @returns Array of deployment repository information
   */
  async listDeployRepositories(): Promise<Types.Repository[]> {
    return (await this.http.get<Types.Repository[]>(
      "/production-repos"
    )).data;
  }

  /**
   * The id of the production repository a user-supplied id or name names, matched as a design repository is
   *
   * @param repositoryIdOrName - The id (e.g. "production-deploy") or the name (e.g. "Production Deployment")
   * @returns Repository ID (e.g., "production-deploy")
   * @throws Error if no repository has that id or name
   */
  async getProductionRepositoryId(repositoryIdOrName: string): Promise<string> {
    const repositories = await this.listDeployRepositories();
    const repository = matchRepository(repositories, repositoryIdOrName);

    if (!repository) {
      const available = repositories.map(r => `${r.id} (${r.name})`).join(", ");
      throw new Error(
        `Production repository "${repositoryIdOrName}" not found. ` +
        `Available production repositories: ${available || "none"}. ` +
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
    const params = {
      search: options?.search || undefined,
      techRevs: options?.techRevs,
      page: options?.page,
      offset: options?.offset,
      size: options?.size,
    };
    const url = `${projectPath(projectId)}/history`;
    return (await this.http.get<Types.PageResponse<Types.ProjectRevision>>(url, { params })).data;
  }

  /**
   * Get the files of a project a revision changed, against the given base or else the revision it was made on.
   *
   * Maps to `GET /projects/{projectId}/history/changes`.
   */
  async getProjectRevisionChanges(
    projectId: string,
    revision: string,
    base?: string
  ): Promise<Types.ProjectRevisionChanges> {
    const url = `${projectPath(projectId)}/history/changes`;
    return (await this.http.get<Types.ProjectRevisionChanges>(url, { params: { revision, base } })).data;
  }

  // =============================================================================
  // Project Creation
  // =============================================================================

  /**
   * Create a new, closed project in a design repository from the "Empty Project" template OpenL Studio ships, the
   * one its users start a blank project from.
   *
   * Maps to `PUT /repos/{repo}/projects/{name}` (multipart: the type, the category and the name of the template),
   * which commits the project in a single changeset and returns the commit revision. An optional branch targets an
   * existing branch (or lets Studio create a missing branch from the repository base). A collision returns 409.
   *
   * @param repositoryId - Canonical repository id (resolve via getRepositoryIdByName)
   * @param projectName - New project name (also the project folder)
   * @param options - Optional comment and target branch
   * @returns The created project's revision (commit SHA) and branch (if supported)
   */
  async createEmptyProject(
    repositoryId: string,
    projectName: string,
    options?: { comment?: string; branch?: string }
  ): Promise<Types.CreateProjectResult> {
    const form = new FormData();
    form.append("templateType", "predefined");
    form.append("templateCategory", "templates");
    form.append("templateName", "Empty Project");
    // Studio opens a project it creates from a template unless it is told otherwise
    form.append("status", "CLOSED");
    if (options?.comment) form.append("comment", options.comment);
    if (options?.branch) form.append("branch", options.branch);

    // A form goes as multipart with its boundary, whatever the JSON default of the client.
    return (await this.http.put<Types.CreateProjectResult>(
      `${repositoryPath(repositoryId)}/projects/${encodeURIComponent(projectName)}`,
      form,
    )).data;
  }

  /**
   * Copy a project addressed by its stable ID and register the copy in Studio, which finds the source project by its
   * ID in every repository. A source project that does not exist answers 404.
   */
  async copyProject(
    targetRepositoryId: string,
    targetProjectName: string,
    sourceProject: string,
    options?: { comment?: string; branch?: string },
  ): Promise<Types.CreateProjectResult> {
    return (await this.http.post<Types.CreateProjectResult>(
      `${repositoryPath(targetRepositoryId)}/projects/${encodeURIComponent(targetProjectName)}/from-project`,
      {
        sourceProject,
        ...(options?.comment ? { comment: options.comment } : {}),
        ...(options?.branch ? { branch: options.branch } : {}),
      },
    )).data;
  }

  // =============================================================================
  // Project Management
  // =============================================================================

  /** Reads the bytes of an answer of any type. */
  private getBytes(url: string, params?: Record<string, unknown>): Promise<HttpResponse<ArrayBuffer>> {
    return this.http.get<ArrayBuffer>(url, { params, responseType: "arraybuffer", headers: { Accept: "*/*" } });
  }

  /** Reads a result OpenL Studio produces in the background, which it answers with 202 while it is not ready. */
  private async getResult<T>(
    url: string,
    options: { params?: Record<string, unknown>; signal?: AbortSignal; timeoutMs?: number } = {},
  ): Promise<T | Types.ResultNotReadyView> {
    const response = await this.http.get<T>(url, {
      params: options.params,
      signal: options.signal,
      timeout: options.timeoutMs,
    });
    return response.status === 202 ? { status: "notReady" } : response.data;
  }

  /** List projects while preserving the backend's pagination metadata. */
  async listProjectsPage(
      filters?: Types.ProjectFilters
  ): Promise<Types.CollectionPage<Types.ProjectViewModel>> {
    const params = {
      repository: filters?.repository || undefined,
      status: filters?.status || undefined,
      dependsOn: filters?.dependsOn || undefined,
      name: filters?.name || undefined,
      author: filters?.author || undefined,
      branch: filters?.branch || undefined,
      sort: filters?.sort || undefined,
      include: filters?.include,
      ...prefixed("tags", filters?.tags),
      // The Studio API takes a true item offset
      offset: filters?.offset,
      size: filters?.limit,
    };
    const page = await this.http.get<Types.PageResponse<Types.ProjectViewModel>>("/projects", { params });
    return toCollectionPage(page.data);
  }

  /**
   * Get project details by ID
   *
   * @returns Project details
   */
  async getProject(
    projectId: string,
    include?: Types.ProjectFilters["include"],
  ): Promise<Types.ProjectViewModel> {
    return (await this.http.get<Types.ProjectViewModel>(projectPath(projectId), { params: { include } })).data;
  }

  /**
   * Get post-compilation project status (compile state, diagnostics, pending changes):
   * the `compileStatus` Studio returns with the project for `include=status`.
   * Read-only — does not trigger compilation. Works for all repositories including "local".
   */
  async getProjectStatus(projectId: string): Promise<Types.ProjectStatusView> {
    const status = (await this.getProject(projectId, ["status"])).compileStatus;
    if (!status) {
      throw new Error(`OpenL Studio returned no status for project '${projectId}'.`);
    }
    return status;
  }

  /**
   * Compile a project Studio has not compiled yet, through its tables API.
   * Studio has no dedicated compile endpoint: listing even one table opens the
   * project model, registers its compilation job, and answers once it ends.
   * The caller gives the time to wait, so a large project is not cut by the
   * shorter general timeout of the client; Studio goes on compiling after it.
   */
  async triggerProjectCompilation(
    projectId: string,
    options: { signal?: AbortSignal; timeoutMs?: number } = {},
  ): Promise<void> {
    await this.http.get(`${projectPath(projectId)}/tables`, {
      params: { offset: 0, size: 1 },
      signal: options.signal,
      timeout: options.timeoutMs,
    });
  }

  /**
   * The status of a project in short: the compile state, and whether its files changed on disk since Studio compiled
   * them. Studio builds no field the query leaves out, so this is cheap enough to ask before every call.
   */
  async getProjectStatusSummary(projectId: string, signal?: AbortSignal): Promise<ProjectStatusSummary> {
    const project = (await this.http.get<Types.ProjectViewModel>(projectPath(projectId), {
      params: STATUS_SUMMARY_QUERY,
      signal,
    })).data;
    return project.compileStatus ?? {};
  }

  /**
   * Compile a project again from its files, dropping what Studio compiled before. Studio compiles every module of
   * the project whichever module is named, and answers at once: the compilation goes on in the background.
   */
  async recompileProject(projectId: string, moduleName: string): Promise<void> {
    await this.http.post(`${projectPath(projectId)}/modules/${encodeURIComponent(moduleName)}/compile`, undefined, {
      params: { reset: true },
    });
  }

  /**
   * Fetches the project and throws if it is in a local repository (repository === "local").
   * Local repositories are not connected to a remote Git; status change (open/save/close) is not supported by the API.
   */
  private async getProjectAndEnsureNotLocal(projectId: string): Promise<Types.ProjectViewModel> {
    const project = await this.getProject(projectId);
    if (project.repository === REPOSITORY_LOCAL) {
      throw new Error(ERROR_LOCAL_REPOSITORY);
    }
    return project;
  }

  /**
   * Delete a project
   */
  async deleteProject(projectId: string, comment?: string): Promise<void> {
    await this.http.delete(projectPath(projectId), { params: { comment } });
  }

  /**
   * Open a project for viewing/editing.
   *
   * Sends PATCH /projects/{projectId} with status "OPENED".
   * Use this only for projects that are not yet opened (status CLOSED, etc.).
   * To put a project on a branch whatever its status, use {@link openOnBranch}: Studio answers
   * 409 Conflict when an already opened project is opened again.
   *
   * @param options - Optional revision, and whether to open the dependencies too
   */
  async openProject(
    projectId: string,
    options?: { revision?: string; openDependencies?: boolean }
  ): Promise<"opened" | "already_open"> {
    const project = await this.getProjectAndEnsureNotLocal(projectId);
    // Studio refuses to open an opened project again: one asked for nothing else is left as it is
    if (!options?.revision && !options?.openDependencies && (project.status === "OPENED" || project.status === "EDITING")) {
      return "already_open";
    }
    const updateModel: Types.ProjectStatusUpdateModel = {
      status: "OPENED",
      ...options,
    };

    await this.http.patch(projectPath(projectId), updateModel);
    return "opened";
  }

  /**
   * Puts a project on a branch.
   *
   * A project that is not opened is opened on the branch, with the given options. An opened or edited project
   * switches its branch alone; with a revision or with its dependencies asked for, it is opened again on the branch
   * with them. Studio refuses to open again a project with unsaved changes: 409 Conflict.
   *
   * A project of a local repository has no branches and is refused.
   *
   * @returns `switched_branch` when only the branch of an opened project was switched, `opened` otherwise.
   */
  async openOnBranch(
    projectId: string,
    branch: string,
    options: { revision?: string; openDependencies?: boolean } = {},
  ): Promise<"opened" | "switched_branch"> {
    const project = await this.getProjectAndEnsureNotLocal(projectId);
    const opened = project.status === "OPENED" || project.status === "EDITING";
    const reopen = !opened || Boolean(options.revision) || options.openDependencies === true;
    const update: Types.ProjectStatusUpdateModel = reopen ? { status: "OPENED", ...options, branch } : { branch };
    await this.http.patch(projectPath(projectId), update);
    return reopen ? "opened" : "switched_branch";
  }

  /**
   * Close an open project
   *
   * Updates project status to CLOSED using PATCH /projects/{projectId}
   *
   * @param options - Whether to discard the unsaved changes
   */
  async closeProject(
    projectId: string,
    options?: { discardChanges?: boolean },
  ): Promise<void> {
    await this.getProjectAndEnsureNotLocal(projectId);
    const updateModel: Types.ProjectStatusUpdateModel = {
      status: "CLOSED",
      discardChanges: options?.discardChanges,
    };

    await this.http.patch(projectPath(projectId), updateModel);
  }

  /**
   * Save project changes, creating a new revision in the repository
   *
   * Saves only a project with unsaved changes (status EDITING). The server creates a new revision and transitions
   * the project to OPENED (or CLOSED if closeAfterSave). A comment is required only when the repository supports it.
   * Uses PATCH /projects/{projectId} with save: true, the comment when supported, and status when closing.
   *
   * A project without unsaved changes has nothing to save. With closeAfterSave it is still closed, unless it is
   * closed already.
   *
   * @param comment - Comment for the new revision when the repository supports comments
   * @param options - Optional. closeAfterSave: if true, send status CLOSED so project is saved and closed in one
   *   request.
   * @returns Whether the project had changes to save, and whether it was closed
   * @throws Error if a comment is missing when the repository supports comments and the project is EDITING
   */
  async saveProject(
    projectId: string,
    comment?: string,
    options?: { closeAfterSave?: boolean }
  ): Promise<Types.SaveProjectResult> {
    const project = await this.getProjectAndEnsureNotLocal(projectId);
    if (project.status !== "EDITING") {
      const closes = options?.closeAfterSave === true && project.status !== "CLOSED";
      if (closes) {
        await this.http.patch(projectPath(projectId), { status: "CLOSED" });
      }
      return options?.closeAfterSave ? { saved: false, closed: true } : { saved: false };
    }
    const supportsComments = project.repositoryInfo?.features?.comments !== false;
    const message = supportsComments ? comment?.trim() : undefined;
    if (supportsComments && !message) {
      throw new Error("comment is required for save; it is used as the revision (commit) message.");
    }
    await this.http.patch(projectPath(projectId), {
      save: true,
      comment: message,
      status: options?.closeAfterSave ? "CLOSED" : undefined,
    });
    return options?.closeAfterSave ? { saved: true, closed: true } : { saved: true };
  }

  // =============================================================================
  // Project Files (BETA)
  // =============================================================================
  //
  // Thin wrappers over the "Projects: Files (BETA)" REST API
  // (/projects/{projectId}/files/{path}, /file-search, /file-copy, /file-move).
  // They operate on any file of the project by its exact project-relative path and expose the raw API surface
  // (branch, version, conflictPolicy, glob/content search, copy/move).

  /**
   * Read the bytes of a file of a project, with the type OpenL Studio gives them: `GET
   * /projects/{projectId}/files/{path}`, of the working copy or of a `version`.
   */
  async readProjectFile(
    projectId: string,
    path: string,
    options?: { version?: string; branch?: string },
  ): Promise<Types.ProjectFileResponse> {
    return toFileContent(await this.getBytes(`${projectPath(projectId)}/files/${encodeProjectFilePath(path)}`, {
      version: options?.version || undefined,
      branch: options?.branch || undefined,
    }));
  }

  /**
   * Write a file to a project's working copy with the raw bytes as an `application/octet-stream` body. The write is
   * not committed to Git; commit it via {@link saveProject}.
   *
   * A write naming the `version` of a file replaces it, `PUT /projects/{projectId}/files/{path}`, while it has that
   * version, and Studio answers 412 for a file changed since. Any other write is `POST
   * /projects/{projectId}/files/{path}`, which creates the file, and Studio answers 409 for a file already there.
   *
   * @param path - Project-relative file path.
   * @param content - Raw file bytes.
   * @param options - createFolders / branch, or the version of the file replaced.
   */
  async writeProjectFile(
    projectId: string,
    path: string,
    content: Buffer,
    options?: {
      createFolders?: boolean;
      branch?: string;
      version?: string;
    }
  ): Promise<void> {
    const encodedPath = encodeProjectFilePath(path);
    if (options?.version !== undefined) {
      await this.http.put(`${projectPath(projectId)}/files/${encodedPath}`, content, {
        headers: { "Content-Type": "application/octet-stream", ...ifMatch(options.version) },
        params: { branch: options.branch || undefined },
      });
      return;
    }

    await this.http.post(
      `${projectPath(projectId)}/files/${encodedPath}`,
      content,
      {
        headers: { "Content-Type": "application/octet-stream" },
        params: {
          createFolders: options?.createFolders,
          conflictPolicy: "FAIL",
          branch: options?.branch || undefined,
        },
      }
    );
  }

  /**
   * Delete a file or folder from a project by its project-relative path.
   *
   * Maps to `DELETE /projects/{projectId}/files/{path}` (HTTP 204). The backend
   * auto-cleans dangling references to the deleted resource from project config.
   *
   * @param path - Project-relative path to the resource.
   * @param options - Optional branch, and the version of a file the deletion is based on: Studio deletes nothing
   *   when the file changed since.
   */
  async deleteProjectFile(
    projectId: string,
    path: string,
    options?: { branch?: string; version?: string }
  ): Promise<void> {
    const encodedPath = encodeProjectFilePath(path);
    await this.http.delete(`${projectPath(projectId)}/files/${encodedPath}`, {
      headers: ifMatch(options?.version),
      params: { branch: options?.branch || undefined },
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
   * @param query - Search criteria (all fields optional).
   * @param options - branch / fields query params.
   */
  async searchProjectFiles(
    projectId: string,
    query: Types.FileSearchQuery,
    options?: { branch?: string; fields?: string }
  ): Promise<Types.FsNode[]> {
    const body = query.from !== undefined ? { ...query, from: normalizeBodyPath(query.from) } : query;
    return (await this.http.post<Types.FsNode[]>(`${projectPath(projectId)}/file-search`, body, {
      params: { branch: options?.branch || undefined, fields: options?.fields || undefined },
    })).data;
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
   * @param projectId - Stable project ID.
   * @param options - `folder`: project-relative sub-folder to start the walk from
   *                   (for "the AGENTS.md nearest the edited file"); `branch`: pin
   *                   the project's branch.
   * @returns AGENTS.md files ordered nearest-first; empty array when none exist.
   */
  async getProjectAgentContext(
    projectId: string,
    options?: { folder?: string; branch?: string }
  ): Promise<Types.AgentsFile[]> {
    const folder = options?.folder && trimChar(options.folder, "/");
    const from = folder ? `${folder}/AGENTS.md` : "AGENTS.md";

    const nodes = await this.searchProjectFiles(
      projectId,
      { scope: "ANCESTORS", from },
      { branch: options?.branch }
    );

    return nodes.map((node) => ({ path: node.path, content: node.content ?? "" }));
  }

  /**
   * Copy or move a file within a project to another project-relative path.
   *
   * Maps to `POST /projects/{projectId}/file-copy` (HTTP 201) or `POST /projects/{projectId}/file-move` (HTTP 204),
   * the body being {sourcePath, destinationPath}. Intermediate destination folders are created automatically, and a
   * move deletes the source. A destination collision returns HTTP 409: there is no overwrite option.
   *
   * @param action - Copy or move.
   * @param pair - Source and destination project-relative paths.
   * @param options - Optional branch.
   */
  async transferProjectFile(
    action: "copy" | "move",
    projectId: string,
    pair: Types.FilePathPairRequest,
    options?: { branch?: string }
  ): Promise<void> {
    const body: Types.FilePathPairRequest = {
      sourcePath: normalizeBodyPath(pair.sourcePath),
      destinationPath: normalizeBodyPath(pair.destinationPath),
    };
    await this.http.post(`${projectPath(projectId)}/file-${action}`, body, {
      params: { branch: options?.branch || undefined },
    });
  }

  /**
   * Create a new branch in a project
   *
   * @param branchName - Name for the new branch
   * @param revision - Optional Git revision to branch from
   */
  async createBranch(
    projectId: string,
    branchName: string,
    revision?: string
  ): Promise<void> {
    const request: Types.BranchCreateRequest = {
      branch: branchName,
      revision,
    };
    await this.http.post(
      `${projectPath(projectId)}/branches`,
      request
    );
  }

  /** List project-holding branches or every repository branch, including base/protected flags. */
  async listProjectBranches(
    projectId: string,
    scope?: Types.BranchScope,
  ): Promise<Types.ProjectBranchInfo[]> {
    return (await this.http.get<Types.ProjectBranchInfo[]>(
      `${projectPath(projectId)}/branches`,
      { params: { scope } },
    )).data;
  }

  /** Delete a project branch; slash-delimited branch names remain path segments. */
  async deleteProjectBranch(projectId: string, branch: string, force: boolean): Promise<void> {
    await this.http.delete(
      `${projectPath(projectId)}/branches/${encodePathSegments(branch)}`,
      { params: { force } },
    );
  }

  /** Check branch relationship and attempt blockers; conflicts are discovered only by the merge. */
  async checkProjectMerge(
    projectId: string,
    request: Types.MergeRequest,
  ): Promise<Types.CheckMergeResult> {
    return (await this.http.post<Types.CheckMergeResult>(
      `${projectPath(projectId)}/merge/check`,
      request,
    )).data;
  }

  /**
   * Start a branch merge. Studio validates it and runs it in the background; read how it ended with
   * {@link getMergeResult}.
   */
  async mergeProjectBranches(
    projectId: string,
    request: Types.MergeRequest,
    force: boolean,
  ): Promise<void> {
    await this.http.post(`${projectPath(projectId)}/merge`, request, { params: { force } });
  }

  /**
   * Read the result of the merge this client started last: success, or the conflicts Studio stored for this client,
   * or `notReady` while the merge is still running.
   */
  async getMergeResult(
    projectId: string,
    options?: { signal?: AbortSignal; timeoutMs?: number },
  ): Promise<Types.MergeResultResponse | Types.ResultNotReadyView> {
    return this.getResult(`${projectPath(projectId)}/merge`, options);
  }

  /**
   * The metadata of a file or a folder of a project — its name, type and size — without its content: `GET
   * /projects/{projectId}/files/{path}?view=meta`.
   */
  async getProjectFileMeta(
    projectId: string,
    path: string,
    options?: { version?: string; branch?: string },
  ): Promise<Types.ProjectFileMeta> {
    const response = await this.http.get<Types.FsNode>(`${projectPath(projectId)}/files/${encodeProjectFilePath(path)}`, {
      params: { view: "meta", version: options?.version || undefined, branch: options?.branch || undefined },
    });
    const version = versionOf(response.headers.etag);
    return version === undefined ? response.data : { ...response.data, version };
  }

  /** The public settings of OpenL Studio; a Studio in single-user mode names no `userMode`. */
  async getSettings(): Promise<Types.StudioSettings> {
    return (await this.http.get<Types.StudioSettings>("/settings")).data;
  }

  /**
   * Issue a transfer link, which moves one file with no credential in the request: `POST /transfer-links`. A
   * Studio in single-user mode issues none.
   */
  async createTransferLink(request: Types.TransferLinkRequest): Promise<Types.TransferLinkView> {
    return (await this.http.post<Types.TransferLinkView>("/transfer-links",
      { ...request, path: normalizeBodyPath(request.path) })).data;
  }

  /**
   * The address of a file of a project in the REST API, at the address of Studio its users open: the link of a
   * download or an upload in single-user mode, where the API needs no credentials.
   */
  projectFileUrl(projectId: string, path: string, params: Record<string, string | undefined>): string {
    return this.publicApiUrl(`${projectPath(projectId)}/files/${encodeProjectFilePath(path)}`, params);
  }

  /** The address of a version of a conflicted file in the REST API, at the address of Studio its users open. */
  conflictFileUrl(projectId: string, file: string, side: string): string {
    return this.publicApiUrl(`${projectPath(projectId)}/merge/conflicts/files`, { file, side });
  }

  private publicApiUrl(path: string, params: Record<string, string | undefined>): string {
    return buildUrl({ baseURL: `${this.publicUrl}rest`, url: path, params });
  }


  /** Read the pending merge conflict details Studio keeps for this client. */
  async getMergeConflicts(projectId: string): Promise<Types.ConflictDetailsResponse> {
    return (await this.http.get<Types.ConflictDetailsResponse>(
      `${projectPath(projectId)}/merge/conflicts`,
    )).data;
  }

  /** Download one BASE/OURS/THEIRS version of a conflicted file. */
  async readMergeConflictFile(
    projectId: string,
    file: string,
    side: "BASE" | "OURS" | "THEIRS",
  ): Promise<Types.ProjectFileResponse> {
    return toFileContent(await this.getBytes(`${projectPath(projectId)}/merge/conflicts/files`, { file, side }));
  }

  /** Read a conflicted text file merged line by line: the lines merged by themselves and the conflicts, in order. */
  async getMergeConflictText(projectId: string, file: string): Promise<Types.ConflictTextView> {
    return (await this.http.get<Types.ConflictTextView>(
      `${projectPath(projectId)}/merge/conflicts/text`,
      { params: { file } },
    )).data;
  }

  /** Clear the pending merge conflict state Studio keeps for this client. */
  async cancelMergeConflicts(projectId: string): Promise<void> {
    await this.http.delete(`${projectPath(projectId)}/merge/conflicts`);
  }

  /**
   * Read the conflicts of a conflicted workbook: the sheets either branch changed, and each conflicting table in your,
   * the base and their version, and merged cell by cell when only some of its cells conflict.
   */
  async getMergeConflictWorkbook(projectId: string, file: string): Promise<Types.ConflictWorkbookView> {
    return (await this.http.get<Types.ConflictWorkbookView>(
      `${projectPath(projectId)}/merge/conflicts/workbook`,
      { params: { file } },
    )).data;
  }

  /**
   * Merge a conflicted workbook with each conflict resolved as decided, and answer the merged workbook. Nothing is
   * saved: the workbook resolves the file through {@link resolveMergeConflicts}.
   */
  async mergeConflictWorkbook(
    projectId: string,
    file: string,
    decisions: Types.ConflictWorkbookDecision[],
  ): Promise<ArrayBuffer> {
    return (await this.http.post<ArrayBuffer>(
      `${projectPath(projectId)}/merge/conflicts/workbook`,
      { decisions },
      { params: { file }, responseType: "arraybuffer", headers: { Accept: "*/*" } },
    )).data;
  }

  /**
   * Resolve the pending conflicts and complete the merge, or the save that found them: each file is taken from a
   * version, or from the merged content sent as `CUSTOM`.
   */
  async resolveMergeConflicts(
    projectId: string,
    resolutions: Types.FileConflictResolution[],
    message?: string,
  ): Promise<Types.ResolveConflictsResponse> {
    const form = new FormData();
    resolutions.forEach((resolution, index) => {
      form.append(`resolutions[${index}].filePath`, resolution.file);
      form.append(`resolutions[${index}].strategy`, resolution.strategy);
      if (resolution.content) {
        // The upload carries the name of the conflicted file, as the upload of a browser does.
        const name = resolution.file.slice(resolution.file.lastIndexOf("/") + 1);
        form.append(`resolutions[${index}].file`, new Blob([resolution.content]), name);
      }
    });
    if (message) form.append("message", message);
    return (await this.http.post<Types.ResolveConflictsResponse>(
      `${projectPath(projectId)}/merge/conflicts/resolve`,
      form,
    )).data;
  }

  // =============================================================================
  // Rules (Tables) Management
  // =============================================================================

  /** List tables while preserving the backend's pagination metadata. */
  async listTablesPage(
      projectId: string,
      filters?: Types.TableFilters
  ): Promise<Types.CollectionPage<Types.TableMetadata>> {
    const params = {
      // Repeated 'kind' keys
      kind: filters?.kind?.length ? filters.kind : undefined,
      name: filters?.name || undefined,
      ...prefixed("properties", filters?.properties),
      // The Studio API takes a true item offset
      offset: filters?.offset,
      size: filters?.limit,
    };
    const url = `${projectPath(projectId)}/tables`;
    const page = await this.http.get<Types.PageResponse<Types.TableMetadata>>(url, { params });
    return toCollectionPage(page.data);
  }

  /** Execute a regular (non-test) table asynchronously. */
  async startTableRun(
    projectId: string,
    tableId: string,
    inputJson: string | unknown[] | Record<string, unknown>,
    options?: { fromModule?: string; timeoutMs?: number }
  ): Promise<void> {
    await this.http.post(
      `${projectPath(projectId)}/run`,
      inputJson,
      {
        params: {
          tableId,
          ...(options?.fromModule && { fromModule: options.fromModule }),
        },
        timeout: options?.timeoutMs,
      }
    );
  }

  /** Read the current regular table run result, or `notReady` while it is still running. */
  async getTableRunResult(
    projectId: string,
    options?: { fields?: string; signal?: AbortSignal; timeoutMs?: number }
  ): Promise<Types.RunExecutionResult | Types.ResultNotReadyView> {
    return this.getResult(`${projectPath(projectId)}/run/result`, {
      ...options,
      params: { fields: options?.fields || undefined },
    });
  }

  /** Cancel and clear the current regular table run. */
  async cancelTableRun(projectId: string): Promise<void> {
    await this.http.delete(`${projectPath(projectId)}/run`);
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
    const url = options?.tableId
      ? `${tablePath(projectId, options.tableId)}/graph`
      : `${projectPath(projectId)}/tables/graph`;
    // A table's neighborhood is read by direction and depth; the whole graph by module and layer.
    const params = options?.tableId
      ? { direction: options.direction || undefined, depth: options.depth }
      : { module: options?.module || undefined, layer: options?.layer || undefined };
    return (await this.http.get<Types.TableNodeView[]>(url, { params })).data;
  }

  /** Return the modules declared by a project descriptor. */
  async listProjectModules(projectId: string): Promise<Types.ModuleViewModel[]> {
    return (await this.http.get<Types.ModuleViewModel[]>(`${projectPath(projectId)}/modules`)).data;
  }

  /** Return worksheet names in one project module. */
  async listModuleSheets(projectId: string, moduleName: string): Promise<string[]> {
    return (await this.http.get<string[]>(
      `${projectPath(projectId)}/modules/${encodeURIComponent(moduleName)}/sheets`
    )).data;
  }

  /** Return the table properties allowed by Studio for the requested context. */
  async listTablePropertyDefinitions(
    projectId: string,
    tableType?: string
  ): Promise<Types.PropertyDefinitionView[]> {
    return (await this.http.get<Types.PropertyDefinitionView[]>(
      `${projectPath(projectId)}/properties`,
      { params: { tableType: tableType || undefined } }
    )).data;
  }

  /** Copy a table server-side, preserving its source layout and formatting. */
  async copyTable(
    projectId: string,
    tableId: string,
    request: Types.CopyTableRequest
  ): Promise<Types.TableMetadata> {
    return (await this.http.post<Types.TableMetadata>(
      `${tablePath(projectId, tableId)}/copy`,
      request
    )).data;
  }

  /**
   * Create a new table in a project using BETA API
   *
   * @param request - Table creation request with moduleName, sheetName, and complete table structure
   * @returns Created table summary with table ID
   */
  async createProjectTable(
    projectId: string,
    request: Types.CreateNewTableRequest
  ): Promise<Types.TableMetadata> {
    return (await this.http.post<Types.TableMetadata>(`${projectPath(projectId)}/tables`, request)).data;
  }

  /**
   * Get detailed table data and structure
   *
   * @param options - Read the matrix in row slices
   *   (`startRow`/`maxRows`) and/or with per-cell Excel styles (`styles`)
   * @returns The table's raw source matrix
   */
  async getTable(projectId: string, tableId: string, options?: Types.RawTableViewOptions): Promise<Types.RawTableView> {
    const params = {
      raw: true,
      startRow: options?.startRow,
      maxRows: options?.maxRows,
      // A window holds exactly the rows asked for, so no merged cell makes a part of the table unreadable
      cutMerges: true,
      styles: options?.styles || undefined,
    };
    const response = await this.http.get<Types.RawTableView>(tablePath(projectId, tableId), { params });
    // Studio answers the version of the table in the ETag header; the tool answers it beside the table, and a write
    // names it back
    const version = versionOf(response.headers.etag);
    return version === undefined ? response.data : { ...response.data, version };
  }

  /**
   * Writes a table and answers the id Studio gives it: a write that renamed the table, or that named it by an id from
   * before a rename, answers 200 with `{ id }`; any other write answers 204, which leaves the id as it was.
   */
  private async writeTable(
    method: "put" | "post",
    path: string,
    body: unknown,
    version: string | undefined,
  ): Promise<string | undefined> {
    return (await this.http[method]<{ id?: string } | undefined>(path, body, { headers: ifMatch(version) })).data?.id;
  }

  /**
   * Update table content
   *
   * @param view - Complete raw table source; its `version`, as the read answered it, is named so that the write is
   *   refused when the table changed since
   * @returns the table's id after the write when Studio answered one, otherwise undefined (204 — id unchanged)
   */
  async updateTable(
    projectId: string,
    tableId: string,
    view: Types.RawTableView
  ): Promise<string | undefined> {
    // Studio accepts RawTableView directly as the request body.
    return this.writeTable("put", tablePath(projectId, tableId), view, view.version);
  }

  /**
   * Apply a single in-place edit to a table's raw source (append, insert,
   * delete, update, merge or unmerge a row/column/cell). The table is handled
   * in raw format regardless of its type.
   *
   * @param action - The edit to apply (operation + target)
   * @param version - The version of the table the edit is based on, as a read or the previous edit answered it
   * @returns the table's id after the edit when Studio answered one, otherwise undefined (204 — id unchanged)
   */
  async editTableSource(
    projectId: string,
    tableId: string,
    action: Types.RawTableSourceAction,
    version: string
  ): Promise<string | undefined> {
    return this.writeTable("post", `${tablePath(projectId, tableId)}/actions`, action, version);
  }

  /**
   * Delete a table from the currently opened project. The whole table area is
   * cleared from the sheet regardless of table type, so the table no longer
   * exists once the project is recompiled.
   *
   * @param version - The version of the table the deletion is based on, as a read answered it
   */
  async deleteTable(projectId: string, tableId: string, version: string): Promise<void> {
    await this.http.delete(tablePath(projectId, tableId), { headers: ifMatch(version) });
  }

  // =============================================================================
  // Deployment Management
  // =============================================================================

  /**
   * List all deployments with optional repository filter
   *
   * @param filters - Optional repository ID and project name to filter deployments by
   * @returns Array of deployment information
   */
  async listDeployments(
    filters?: { repository?: string; project?: string },
  ): Promise<Types.DeploymentViewModelShort[]> {
    return (await this.http.get<Types.DeploymentViewModelShort[]>(
      "/deployments",
      { params: filters }
    )).data;
  }

  /**
   * Deploy a project to production repository
   *
   * @param request - Deployment request with project ID, deployment name, and target repository
   */
  async deployProject(request: Types.DeployProjectRequest): Promise<void> {
    await this.http.post("/deployments", request);
  }

  /**
   * Redeploy an existing deployment
   *
   * @param deploymentId - Deployment ID to redeploy
   * @param request - Redeploy request with project ID and optional comment
   */
  async redeployProject(
    deploymentId: string,
    request: Types.RedeployProjectRequest
  ): Promise<void> {
    await this.http.post(`/deployments/${encodeURIComponent(deploymentId)}`, request);
  }

  /** Get one named module's local edit history from an opened project. */
  async getProjectLocalChanges(
    projectId: string,
    moduleName: string
  ): Promise<Types.ProjectHistoryItem[]> {
    return (await this.http.get<Types.ProjectHistoryItem[]>(
      `${projectPath(projectId)}/local-history`,
      { params: { module: moduleName } }
    )).data;
  }

  /** Restore a named module from one opened project's local edit history. */
  async restoreProjectLocalChange(
    projectId: string,
    moduleName: string,
    historyId: string
  ): Promise<void> {
    await this.http.post(
      `${projectPath(projectId)}/local-history/restore`,
      { version: historyId },
      { params: { module: moduleName } }
    );
  }

  // =============================================================================
  // Test Execution Methods
  // =============================================================================

  /**
   * Start project tests execution
   *
   * For design repositories: ensures project is opened before starting tests; automatically opens if closed.
   * For repository 'local': does not open the project; runs tests directly (local projects are always editable).
   *
   * @param projectId - Project ID
   * @param options - Test execution options
   * @returns That the tests started, and whether the project was opened for them
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
    // Local projects are always editable — skip open; for design repos open if needed
    const project = await this.getProject(projectId);
    const projectWasOpened = project.repository !== REPOSITORY_LOCAL &&
      project.status !== "OPENED" && project.status !== "EDITING";
    if (projectWasOpened) {
      await this.http.patch(projectPath(projectId), { status: "OPENED" });
    }

    const params = {
      tableId: options?.tableId || undefined,
      testRanges: options?.testRanges || undefined,
      fromModule: options?.fromModule || undefined,
    };

    // Start test execution. Studio keeps the run for this client: its session, or the credential
    // of a request that has one, so the results are read with the same client, not a stored token.
    await this.http.post(`${projectPath(projectId)}/tests/run`, undefined, { params });

    // The tests the call selected are not told back
    return projectWasOpened ? { started: true, projectOpened: true } : { started: true };
  }

  /**
   * Get test results summary (without testCases array)
   *
   * @param projectId - Project ID
   * @param options - Summary options
   * @returns Test results summary, or `notReady` while the execution is still running
   * @throws Error if no tests were started for the project or the request fails
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
    const params = {
      failuresOnly: options?.failuresOnly,
      failures: options?.failures,
      unpaged: options?.unpaged || undefined,
    };
    const summary = await this.getResult<Types.TestsExecutionSummary>(`${projectPath(projectId)}/tests/summary`, {
      ...options,
      params,
    });
    if (isResultNotReady(summary)) {
      return summary;
    }
    return {
      executionTimeMs: summary.executionTimeMs,
      numberOfTests: summary.numberOfTests,
      numberOfFailures: summary.numberOfFailures,
      numberOfPassed: summary.numberOfTests - summary.numberOfFailures,
    };
  }

  /**
   * Get full test results with pagination, of every test table of the run or of the one `tableId` names
   *
   * @param projectId - Project ID
   * @param options - Result options including pagination
   * @returns Full test execution summary with testCases, or `notReady` while execution is still running
   * @throws Error if no tests were started for the project or the request fails
   */
  async getTestResults(
    projectId: string,
    options?: {
      tableId?: string;
      failuresOnly?: boolean;
      failures?: number;
      offset?: number;
      limit?: number;
      unpaged?: boolean;
      signal?: AbortSignal;
      timeoutMs?: number;
    }
  ): Promise<Types.TestsExecutionSummary | Types.ResultNotReadyView> {
    const params = {
      tableId: options?.tableId,
      failuresOnly: options?.failuresOnly || undefined,
      failures: options?.failures,
      // The results of one test table hold every failure it has
      allFailures: options?.tableId ? true : undefined,
      offset: options?.offset,
      size: options?.limit,
      unpaged: options?.unpaged || undefined,
    };

    return this.getResult(`${projectPath(projectId)}/tests/summary`, { ...options, params });
  }

  // =============================================================================
  // Trace Debug API (BETA) — interactive debugger
  //
  // The debug session is server-side: Studio keeps it for the credential of the
  // client, so every call of one debug flow finds it. One active session per
  // user; starting a new one terminates the previous.
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
    const params = {
      tableId: request.tableId,
      testRanges: request.testRanges || undefined,
      fromModule: request.fromModule || undefined,
      stopAtEntry: request.stopAtEntry,
      profiling: request.profiling,
      detailedTitles: request.detailedTitles,
      breakOnErrors: request.breakOnErrors,
      includeTree: request.includeTree,
      profileTop: request.profileTop,
      // Only the active frame lists its steps, as a step and a resume answer
      view: "compact",
    };
    const url = `${projectPath(request.projectId)}/trace`;
    return (await this.http.post<Types.DebugStackView>(url, request.inputJson, { params })).data;
  }

  /**
   * Lightweight status poll of the debug session.
   */
  async getTraceStatus(projectId: string): Promise<Types.DebugStatusView> {
    return (await this.http.get<Types.DebugStatusView>(
      `${projectPath(projectId)}/trace/status`
    )).data;
  }

  /**
   * Read the execution stack (frames root → current) in its compact view, which
   * keeps steps only on the active frame. Readable while suspended or terminal;
   * 409 while the worker is still running. `includeTree`/`profileTop` shape a
   * profiling run's terminal `tree`/`profile`.
   */
  async getTraceStack(projectId: string, options?: Types.TraceStackOptions): Promise<Types.DebugStackView> {
    return (await this.http.get<Types.DebugStackView>(
      `${projectPath(projectId)}/trace/stack`,
      { params: traceStackParams(options) }
    )).data;
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
    // Pass every value through `params` so the URI's own query characters
    // (?sheet=…&range=…) are percent-encoded rather than merged into the path.
    const params = {
      uri: options.uri,
      instance: options.instance,
      step: options.step,
      offset: options.offset,
      limit: options.limit,
    };
    return (await this.http.get<Types.TreeChildrenView>(
      `${projectPath(projectId)}/trace/tree/children`,
      { params }
    )).data;
  }

  /**
   * Step once (into / over / out) and return the new stack, in its compact view,
   * once the worker re-suspends (the backend waits synchronously, bounded ~30s).
   */
  async traceStep(
    projectId: string,
    type: "into" | "over" | "out",
    options?: Types.TraceStackOptions
  ): Promise<Types.DebugStackView> {
    return (await this.http.post<Types.DebugStackView>(
      `${projectPath(projectId)}/trace/step`,
      undefined,
      { params: { type, ...traceStackParams(options) } }
    )).data;
  }

  /**
   * Resume execution to the next breakpoint or completion. Asynchronous:
   * returns 202 immediately — poll getTraceStatus until it leaves running,
   * then read getTraceStack.
   */
  async traceResume(projectId: string): Promise<void> {
    await this.http.post(`${projectPath(projectId)}/trace/resume`);
  }

  /**
   * Freeze and read the variables of a suspended frame. `fields` is the
   * standard response projection (nested selection supported) used to keep
   * value schemas and other bulk out of the agent's token budget; without it,
   * the whole reply comes back, the JSON Schemas of the values included.
   */
  async getTraceFrameVariables(
    projectId: string,
    frameIndex: number,
    fields?: string,
  ): Promise<Types.DebugFrameVariables> {
    return (await this.http.get<Types.DebugFrameVariables>(
      `${projectPath(projectId)}/trace/frames/${frameIndex}/variables`,
      { params: projection(fields) }
    )).data;
  }

  /**
   * Execution highlight overlay for a frame's table, keyed by A1 cell address.
   * Merge with the raw table grid (getTable).
   */
  async getTraceFrameHighlights(
    projectId: string,
    frameIndex: number
  ): Promise<Types.CellHighlight[]> {
    return (await this.http.get<Types.CellHighlight[]>(
      `${projectPath(projectId)}/trace/frames/${frameIndex}/highlights`
    )).data;
  }

  /**
   * Active breakpoint keys. Session-scoped, persist across runs, work without
   * an active debug session.
   */
  async getTraceBreakpoints(projectId: string): Promise<string[]> {
    return (await this.http.get<string[]>(
      `${projectPath(projectId)}/trace/breakpoints`
    )).data;
  }

  /**
   * Replace the whole breakpoint set (effective on the next frame enter /
   * current-line change).
   */
  async setTraceBreakpoints(projectId: string, uris: string[]): Promise<void> {
    await this.http.put(`${projectPath(projectId)}/trace/breakpoints`, { uris });
  }

  /**
   * Rule tables a breakpoint can be set on, deduplicated by name. With an
   * active session only tables reachable from the traced table are returned.
   */
  async getTraceBreakpointTables(projectId: string): Promise<Types.BreakpointTableView[]> {
    return (await this.http.get<Types.BreakpointTableView[]>(
      `${projectPath(projectId)}/trace/breakpoint-tables`
    )).data;
  }

  /**
   * Get lazy-loaded parameter value. `fields` is the standard response
   * projection — used to drop the value's JSON Schema from the default reply;
   * without it, the whole reply comes back, the schema included.
   */
  async getTraceParameter(
    projectId: string,
    parameterId: number,
    fields?: string,
  ): Promise<Types.TraceParameterValue> {
    return (await this.http.get<Types.TraceParameterValue>(
      `${projectPath(projectId)}/trace/parameters/${parameterId}`,
      { params: projection(fields) }
    )).data;
  }

  /**
   * Replace the watched-cell set (applied on the next start). Watches capture a
   * named cell's value at every execution of its table across a whole run.
   */
  async setTraceWatches(projectId: string, cells: string[]): Promise<void> {
    await this.http.put(`${projectPath(projectId)}/trace/watches`, { cells });
  }

  /**
   * Collected watched-cell values — one series per cell, one point per execution
   * of its table. Read after a run completes (409 while still running). `fields`
   * is the standard response projection — used to drop each point value's JSON
   * Schema from the default reply; without it, the whole reply comes back.
   */
  async getTraceWatch(
    projectId: string,
    fields?: string,
  ): Promise<Types.WatchView> {
    return (await this.http.get<Types.WatchView>(
      `${projectPath(projectId)}/trace/watch`,
      { params: projection(fields) }
    )).data;
  }

  /**
   * Terminate the debug session and clear the parameter registry. Idempotent.
   */
  async stopTrace(projectId: string): Promise<void> {
    await this.http.delete(`${projectPath(projectId)}/trace`);
  }

}
