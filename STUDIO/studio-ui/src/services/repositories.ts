import apiCall, { asArray, isApiHttpError, type ApiCallOptions } from './apiCall'
import CONFIG from './config'
import { triggerDownload } from '../utils/download'
import type { Repository, RepositoryConfig } from '../types/repositories'
import type { Project, ProjectsPage } from '../types/projects'
import type { FsNode } from '../types/files'
import { ProjectStatus } from '../constants/project'

const THROW_API_OPTIONS = { throwError: true } satisfies ApiCallOptions
const PROJECT_MODIFIED_ERROR_CODE = 'openl.error.409.project.close.modified.message'

export type ProjectInclude = 'summary' | 'status' | 'deleted' | 'descriptor'

export const isProjectModifiedConflict = (error: unknown): boolean => {
    if (!isApiHttpError(error) || error.status !== 409) {
        return false
    }
    const payload = error.payload
    return !!payload
        && typeof payload === 'object'
        && 'code' in payload
        && payload['code'] === PROJECT_MODIFIED_ERROR_CODE
}

/**
 * Fetch the design repositories the current user may read, including the effective access
 * (role, grants, create/manage capabilities) computed server-side for action gating.
 */
export async function getDesignRepositories(apiOptions: ApiCallOptions = THROW_API_OPTIONS): Promise<Repository[]> {
    const response = await apiCall('/repos', undefined, apiOptions)
    return asArray(response)
}

export interface GetProjectsQuery {
    page?: number
    size?: number
    /** Ask for every project at once instead of a page of them. */
    unpaged?: boolean
    /** Comma-separated response fields to keep, for a caller that needs only a few of them. */
    fields?: string
    name?: string | undefined
    author?: string | undefined
    branch?: string | undefined
    sort?: 'name' | 'status' | 'updated'
    statuses?: Iterable<ProjectStatus | string>
    repositories?: Iterable<string>
    tags?: Iterable<string>
    includes?: Iterable<ProjectInclude>
}

/**
 * Fetch one server page of projects visible to the current user across all design repositories.
 */
export async function getProjects(
    query: GetProjectsQuery = {},
    apiOptions: ApiCallOptions = THROW_API_OPTIONS
): Promise<ProjectsPage> {
    const params = new URLSearchParams()
    setParam(params, 'page', query.page)
    setParam(params, 'size', query.size)
    setParam(params, 'unpaged', query.unpaged)
    setParam(params, 'name', query.name?.trim())
    setParam(params, 'author', query.author?.trim())
    setParam(params, 'branch', query.branch?.trim())
    setParam(params, 'sort', query.sort)
    setParam(params, 'fields', query.fields)
    appendRepeated(params, 'include', query.includes)
    appendRepeated(params, 'status', query.statuses)
    appendRepeated(params, 'repository', query.repositories)
    appendTags(params, query.tags)

    const queryString = params.toString()
    const request = queryString ? `/projects?${queryString}` : '/projects'
    const response = await apiCall(request, undefined, apiOptions)
    if (Array.isArray(response)) {
        return {
            content: response,
            pageNumber: query.page ?? 0,
            pageSize: response.length,
            numberOfElements: response.length,
            total: response.length,
        }
    }
    return {
        content: response?.content ?? [],
        pageNumber: response?.pageNumber ?? query.page ?? 0,
        pageSize: response?.pageSize ?? query.size ?? 0,
        numberOfElements: response?.numberOfElements ?? response?.content?.length ?? 0,
        total: response?.total,
        statusCounts: response?.statusCounts,
        repositoryCounts: response?.repositoryCounts,
        tagCounts: response?.tagCounts,
        statuses: response?.statuses,
        projectIndexHealth: response?.projectIndexHealth,
    }
}

function setParam(params: URLSearchParams, key: string, value: boolean | number | string | undefined): void {
    if (value !== undefined && value !== '') {
        params.set(key, String(value))
    }
}

function appendRepeated(params: URLSearchParams, key: string, values: Iterable<ProjectStatus | string> | undefined): void {
    for (const value of values ?? []) {
        if (value) {
            params.append(key, String(value))
        }
    }
}

function appendTags(params: URLSearchParams, tags: Iterable<string> | undefined): void {
    for (const tag of tags ?? []) {
        const separator = tag.indexOf(':')
        if (separator < 1) {
            continue
        }
        const type = tag.slice(0, separator)
        const value = tag.slice(separator + 1)
        if (value) {
            params.append(`tags.${type}`, value)
        }
    }
}

/**
 * Load a single project by id. This detail response carries everything the workspace needs — including
 * the dependency graph and, when the descriptor is requested, the resolved modules and sources that are
 * too costly to compute for every row of the list. The rest of rules.xml the UI reads from the file.
 */
export async function getProject(
    projectId: string,
    options: { includes?: Iterable<ProjectInclude> } = {},
    apiOptions: ApiCallOptions = THROW_API_OPTIONS
): Promise<Project> {
    const params = new URLSearchParams()
    appendRepeated(params, 'include', options.includes)
    const query = params.toString()
    const path = `/projects/${encodeURIComponent(projectId)}${query ? `?${query}` : ''}`
    return await apiCall(path, undefined, apiOptions)
}

/**
 * List a project's files as a flat, recursive set of nodes (files and folders) with their paths.
 */
export async function getProjectFiles(projectId: string, recursive = true, version?: string): Promise<FsNode[]> {
    const params = new URLSearchParams({ viewMode: 'FLAT' })
    if (recursive) {
        params.set('recursive', 'true')
    }
    if (version) {
        params.set('version', version)
    }
    const response = await apiCall(
        `/projects/${encodeURIComponent(projectId)}/files/?${params.toString()}`,
        undefined,
        // The Files tab shows its own error state; a failure here must not take over the whole screen.
        { throwError: true, suppressErrorPages: true }
    )
    return asArray(response)
}

/**
 * List the immediate sub-folders of a repository folder. Returns one level only (not recursive), so a
 * folder tree can be expanded lazily — important because a repository may hold very many folders.
 *
 * @param repositoryId design repository id
 * @param path repository-relative folder path; empty lists the repository root
 */
export async function listRepoFolders(repositoryId: string, path = ''): Promise<FsNode[]> {
    // The backend lists a folder's children only when the path ends with a slash; without it the path is
    // read as a file. The root path is already "files/", so the slash is only appended to a sub-path.
    const encodedPath = path.split('/').filter(Boolean).map(encodeURIComponent).join('/')
    const folderPath = encodedPath ? `${encodedPath}/` : ''
    const response = await apiCall(
        `/repos/${encodeURIComponent(repositoryId)}/files/${folderPath}?foldersOnly=true&recursive=false`,
        undefined,
        { throwError: true }
    )
    return asArray(response)
}

/** Delete a project. The backend enforces the DELETE grant and project state; callers should refresh afterwards. */
export async function deleteProject(projectId: string): Promise<void> {
    await apiCall(`/projects/${encodeURIComponent(projectId)}`, { method: 'DELETE' }, { throwError: true })
}

/** Force-release a project's lock (requires administration rights). */
export async function unlockProject(projectId: string): Promise<void> {
    await apiCall(`/projects/${encodeURIComponent(projectId)}/lock`, { method: 'DELETE' }, { throwError: true })
}

/**
 * Copy a project into the target repository under a new name. The copy happens entirely server-side:
 * the backend copies the project folder, renames the descriptor, grants access and re-indexes the
 * workspace. No download/re-upload round-trip.
 */
export async function copyProject(
    sourceRepositoryId: string,
    sourceProjectName: string,
    targetRepositoryId: string,
    newName: string,
    comment?: string,
    path?: string,
    revision?: string,
    branch?: string
): Promise<void> {
    await apiCall(
        `/repos/${encodeURIComponent(targetRepositoryId)}/projects/${encodeURIComponent(newName)}/from-project`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                sourceRepositoryId,
                sourceProjectName,
                ...(comment?.trim() ? { comment: comment.trim() } : {}),
                ...(path?.trim() ? { path: path.trim() } : {}),
                ...(revision?.trim() ? { revision: revision.trim() } : {}),
                ...(branch?.trim() ? { branch: branch.trim() } : {}),
            }),
        },
        { throwError: true }
    )
}

/**
 * Create a project in a design repository from an uploaded ZIP archive.
 *
 * Reuses the create-from-ZIP endpoint, which unpacks the archive, registers the project and grants the
 * creator access. The comment is a query parameter; the archive is the `template` multipart field. The
 * backend enforces the CREATE grant on the repository.
 */
export interface CreateProjectOptions {
    /** Uploaded content: a single .zip, one or more Excel files, or a single OpenAPI file. */
    files?: File[]
    /** Bundled/custom template to create from (no upload). */
    template?: { type: string, category: string, name: string }
    /** Module names/paths applied only when the upload is an OpenAPI file. */
    openApi?: { modelsModuleName?: string, modelsPath?: string, algorithmsModuleName?: string, algorithmsPath?: string }
    comment?: string | undefined
    path?: string | undefined
    overwrite?: boolean
    /** Status the created project should have in the workspace. Left unset, the backend keeps its default. */
    status?: 'OPENED' | 'CLOSED'
    /** Existing target branch or a new branch name to create from the repository base branch. */
    branch?: string
}

/**
 * Create a project in a design repository. The backend picks the strategy from the request content:
 * a ZIP archive, Excel files, an OpenAPI file (all uploaded as {@link CreateProjectOptions.files}), or a
 * named template. Overwrite applies only to a single ZIP upload.
 */
export async function createProject(
    repositoryId: string,
    projectName: string,
    options: CreateProjectOptions
): Promise<void> {
    const form = new FormData()
    for (const file of options.files ?? []) {
        form.append('template', file, file.name)
    }
    if (options.template) {
        form.append('templateType', options.template.type)
        form.append('templateCategory', options.template.category)
        form.append('templateName', options.template.name)
    }
    for (const [key, value] of Object.entries(options.openApi ?? {})) {
        if (value) {
            form.append(key, value)
        }
    }
    const params = new URLSearchParams()
    if (options.comment) {
        params.set('comment', options.comment)
    }
    if (options.path) {
        params.set('path', options.path)
    }
    if (options.overwrite) {
        params.set('overwrite', 'true')
    }
    if (options.status) {
        params.set('status', options.status)
    }
    if (options.branch?.trim()) {
        params.set('branch', options.branch.trim())
    }
    const query = params.toString() ? `?${params.toString()}` : ''
    await apiCall(
        `/repos/${encodeURIComponent(repositoryId)}/projects/${encodeURIComponent(projectName)}${query}`,
        { method: 'PUT', body: form },
        { throwError: true }
    )
}

export interface ProjectTemplateGroup {
    type: string
    category: string
    templates: string[]
}

/** List the available project templates (bundled + custom), grouped by category. */
export async function getProjectTemplates(): Promise<ProjectTemplateGroup[]> {
    const response = await apiCall('/repos/project-templates', undefined, { throwError: true })
    return asArray(response)
}

/** Publish one or more local workspace projects to a design repository, keeping their names. */
export async function createProjectsFromWorkspace(
    repositoryId: string,
    body: { names: string[], path?: string | undefined, comment?: string | undefined, branch?: string | undefined }
): Promise<void> {
    await apiCall(
        `/repos/${encodeURIComponent(repositoryId)}/projects/from-workspace`,
        { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) },
        { throwError: true }
    )
}

/** List the actual Git branches of a design repository. */
export async function getDesignRepositoryBranches(repositoryId: string): Promise<string[]> {
    const response = await apiCall(
        `/repos/${encodeURIComponent(repositoryId)}/branches`,
        undefined,
        THROW_API_OPTIONS
    )
    return asArray(response)
}

/**
 * Trigger a browser download of the whole project as a ZIP archive.
 *
 * @param version revision to download; omit for the workspace copy, which carries the local changes of a
 *                project being edited
 */
export function downloadProject(projectId: string, version?: string): void {
    const params = new URLSearchParams({ download: 'true' })
    if (version) {
        params.set('version', version)
    }
    triggerDownload(
        `${CONFIG.CONTEXT}/web/projects/${encodeURIComponent(projectId)}/files/?${params.toString()}`
    )
}

export type ProjectStatusToSet = 'OPENED' | 'CLOSED'

export interface SetProjectStatusOptions {
    discardChanges?: boolean
    openDependencies?: boolean
}

export interface DiscardChangesOptions {
    discardChanges?: boolean
}

/** Send a PATCH to the single-project endpoint with a JSON body. */
async function patchProject(projectId: string, body: Record<string, unknown>): Promise<void> {
    await apiCall(`/projects/${encodeURIComponent(projectId)}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    }, { throwError: true })
}

/**
 * Open or close a project in the current user's workspace. The backend enforces the ACL and workspace
 * state independently; the caller should refresh the project list afterwards to reflect the new state.
 */
export async function setProjectStatus(
    projectId: string,
    status: ProjectStatusToSet,
    options: SetProjectStatusOptions = {}
): Promise<void> {
    await patchProject(projectId, {
        status,
        ...(options.openDependencies ? { openDependencies: true } : {}),
        ...(options.discardChanges ? { discardChanges: true } : {}),
    })
}

/**
 * Save (commit) a project's local modifications. The backend commits when the project is modified and
 * supplies its own default commit message when no comment is provided.
 */
export async function saveProject(projectId: string, comment?: string): Promise<void> {
    await patchProject(projectId, {
        save: true,
        ...(comment?.trim() ? { comment: comment.trim() } : {}),
    })
}

/**
 * Read the settings of the repository the project is stored in — the branch and comment rules the project
 * forms suggest values from and validate against. Asked per project, so it also works for a user granted
 * access to the project alone rather than to the whole repository.
 */
export async function getRepositoryConfig(projectId: string): Promise<RepositoryConfig> {
    const response = await apiCall(
        `/projects/${encodeURIComponent(projectId)}/repository-config`,
        undefined,
        THROW_API_OPTIONS
    )
    return response as RepositoryConfig
}

/**
 * Read the settings of a repository the user may create a project in — the same branch and comment rules,
 * asked before a project exists.
 */
export async function getDesignRepositoryConfig(repositoryId: string): Promise<RepositoryConfig> {
    const response = await apiCall(
        `/repos/${encodeURIComponent(repositoryId)}/config`,
        undefined,
        THROW_API_OPTIONS
    )
    return response as RepositoryConfig
}

export interface ProjectBranch {
    name: string
    /** Present only for a protected branch. */
    protected?: boolean
    /** The repository base branch; it can never be deleted. */
    base?: boolean
}

/** Create a new branch for a project, optionally from a specific revision (defaults to HEAD). */
export async function createProjectBranch(projectId: string, branch: string, revision?: string): Promise<void> {
    await apiCall(`/projects/${encodeURIComponent(projectId)}/branches`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(revision ? { branch, revision } : { branch }),
    }, { throwError: true })
}

export interface RevisionAuthor {
    email?: string
    displayName?: string
}

export interface ProjectRevision {
    revisionNo: string
    shortRevisionNo: string
    createdAt: string
    fullComment: string
    author?: RevisionAuthor
    deleted: boolean
    technicalRevision: boolean
    commentParts?: string[]
}

export interface RevisionPage {
    content: ProjectRevision[]
    pageNumber: number
    pageSize: number
    numberOfElements: number
    total: number | null
}

export interface RevisionQuery {
    search?: string
    techRevs?: boolean
    page?: number
    size?: number
}

/** Default page size for the project history, matching the legacy UI's incremental loading. */
export const REVISIONS_PAGE_SIZE = 20

/**
 * A page of a project's revision history (newest first) via the design-repository history API — the same
 * endpoint the legacy UI uses. Supports a text search, the technical-revisions toggle and paging. For
 * branch-capable repositories the branch is part of the path (with '/' replaced by a space, as the legacy
 * UI does).
 */
export async function getProjectRevisions(
    repositoryId: string,
    projectName: string,
    branch: string | null,
    query: RevisionQuery = {}
): Promise<RevisionPage> {
    const params = new URLSearchParams()
    if (query.search?.trim()) {
        params.set('search', query.search.trim())
    }
    if (query.techRevs) {
        params.set('techRevs', 'true')
    }
    params.set('page', String(query.page ?? 0))
    params.set('size', String(query.size ?? REVISIONS_PAGE_SIZE))
    const branchSegment = branch ? `/branches/${encodeURIComponent(branch.replaceAll('/', ' '))}` : ''
    const url = `/repos/${encodeURIComponent(repositoryId)}${branchSegment}/projects/${encodeURIComponent(projectName)}/history?${params.toString()}`
    const response = await apiCall(url, undefined, { throwError: true })
    return {
        content: asArray(response?.content),
        pageNumber: response?.pageNumber ?? (query.page ?? 0),
        pageSize: response?.pageSize ?? (query.size ?? REVISIONS_PAGE_SIZE),
        numberOfElements: response?.numberOfElements ?? 0,
        total: response?.total ?? null,
    }
}

/** Open a project at a specific historical revision. */
export async function openProjectRevision(
    projectId: string,
    revision: string,
    options: DiscardChangesOptions = {}
): Promise<void> {
    const body: Record<string, unknown> = { revision, status: ProjectStatus.Opened }
    if (options.discardChanges) {
        body['discardChanges'] = true
    }
    await patchProject(projectId, body)
}

/** List the branches available for a project (only meaningful for repositories that support branches). */
export async function getProjectBranches(projectId: string): Promise<ProjectBranch[]> {
    const response = await apiCall(`/projects/${encodeURIComponent(projectId)}/branches`, undefined, { throwError: true })
    return asArray(response)
}

/** Switch a project to a different branch. The backend opens the project on that branch. */
export async function switchProjectBranch(
    projectId: string,
    branch: string,
    options: DiscardChangesOptions = {}
): Promise<void> {
    await patchProject(projectId, {
        branch,
        ...(options.discardChanges ? { discardChanges: true } : {}),
    })
}

export interface TagType {
    name: string
    /** Custom values beyond {@link values} are allowed. */
    extensible: boolean
    /** The tag may be left unset. */
    nullable: boolean
    values: string[]
}

/** List the configured tag types (name + allowed values) used to tag projects. */
export async function getTagTypes(): Promise<TagType[]> {
    const response = await apiCall('/tags/types', undefined, { throwError: true })
    return asArray(response)
}
