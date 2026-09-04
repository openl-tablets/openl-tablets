import apiCall, { isApiHttpError, LOCAL_LOAD_API_OPTIONS } from './apiCall'
import { encodeProjectPath, toUrlSafeId } from './projectId'
import CONFIG from './config'
import { getProjectFiles, revisionQueryParams, toRevisionPage, type RevisionPage, type RevisionQuery } from './repositories'
import { triggerDownload } from '../utils/download'

/** Text file extensions the generic editor can open. Files with no extension are also treated as text. */
const TEXT_EXTENSIONS = new Set([
    'txt', 'md', 'xml', 'json', 'yaml', 'yml', 'properties', 'groovy', 'java', 'js', 'jsx', 'ts', 'tsx',
    'css', 'scss', 'less', 'html', 'htm', 'csv', 'sql', 'sh', 'bat', 'conf', 'ini', 'log', 'gitignore',
])

/** Whether a file can be opened in the generic text editor (as opposed to Excel/binary content). */
export function isEditableTextFile(name: string): boolean {
    const dot = name.lastIndexOf('.')
    if (dot <= 0) {
        return true
    }
    return TEXT_EXTENSIONS.has(name.slice(dot + 1).toLowerCase())
}

/** Build the files-API URL for a single file, encoding each path segment while keeping the separators. */
const fileUrl = (projectId: string, path: string): string =>
    `/projects/${toUrlSafeId(projectId)}/files/${encodeProjectPath(path)}`

/**
 * Read a project file's raw text content, optionally at a specific historical revision. The endpoint
 * returns the file bytes with the file's own content type; the body is always read as text, so a JSON
 * file keeps its exact formatting instead of being parsed into an object.
 */
export async function getFileContent(projectId: string, path: string, version?: string): Promise<string> {
    const query = version ? `?version=${encodeURIComponent(version)}` : ''
    const response = await apiCall(
        `${fileUrl(projectId, path)}${query}`,
        undefined,
        // A missing file is the pane's to report inline; it must not replace the whole screen with the
        // global 404 page, which a folder path or a file gone since the tree was read would otherwise do.
        { throwError: true, responseType: 'response', suppressErrorPages: true }
    ) as Response
    return response.text()
}


/**
 * Overwrite a project file with new text content. The backend enforces the WRITE grant and the project's
 * editable state; a plain-text body routes to the raw-update handler (a JSON body would create a folder).
 */
export async function updateFileContent(projectId: string, path: string, content: string): Promise<void> {
    await apiCall(
        fileUrl(projectId, path),
        { method: 'PUT', headers: { 'Content-Type': 'text/plain' }, body: content },
        { throwError: true }
    )
}

/**
 * Replace a project file with an uploaded one, keeping its path. The backend enforces the WRITE grant and
 * the project's editable state.
 */
export async function replaceFile(projectId: string, path: string, file: File): Promise<void> {
    const form = new FormData()
    form.append('file', file)
    await apiCall(fileUrl(projectId, path), { method: 'PUT', body: form }, { throwError: true })
}

/** Create a new text file, creating intermediate folders when the entered path includes them. */
export async function createTextFile(projectId: string, path: string): Promise<void> {
    await apiCall(
        `${fileUrl(projectId, path)}?createFolders=true`,
        { method: 'POST', headers: { 'Content-Type': 'text/plain' }, body: '' },
        { throwError: true }
    )
}

/** Delete a project file or folder. The backend enforces the DELETE grant and the project's state. */
export async function deleteFile(projectId: string, path: string): Promise<void> {
    await apiCall(fileUrl(projectId, path), { method: 'DELETE' }, { throwError: true })
}

/**
 * Write text content to a file at the project root. Overwriting an existing file and creating a new one
 * go through different requests of the files API — a distinction the callers name by the mode instead of
 * making themselves.
 */
export async function writeRootFile(projectId: string, name: string, content: string, mode: 'create' | 'overwrite'): Promise<void> {
    if (mode === 'overwrite') {
        await updateFileContent(projectId, name, content)
    } else {
        await uploadFiles(projectId, '', [new File([content], name, { type: 'application/xml' })])
    }
}

/**
 * Whether a file with the given name exists directly in the project root. Uses a root listing so a
 * missing file is a normal empty result, rather than fetching the file and treating a 404 as "absent".
 */
export async function rootFileExists(projectId: string, name: string): Promise<boolean> {
    // Root-level listing only: the descriptor lives at the project root, so skip the recursive tree walk.
    const files = await getProjectFiles(projectId, false)
    return files.some(node => node.type === 'file' && node.path === name)
}

/** Move or rename a file within a project. */
export async function moveFile(projectId: string, sourcePath: string, destinationPath: string): Promise<void> {
    await apiCall(
        `/projects/${toUrlSafeId(projectId)}/file-move`,
        { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sourcePath, destinationPath }) },
        { throwError: true }
    )
}

/** Copy a file within a project to a new path. */
export async function copyFile(projectId: string, sourcePath: string, destinationPath: string): Promise<void> {
    await apiCall(
        `/projects/${toUrlSafeId(projectId)}/file-copy`,
        { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sourcePath, destinationPath }) },
        { throwError: true }
    )
}

/**
 * A page of a single file's revision history, newest first.
 *
 * Only the revisions that changed the file are reported, so the caller offers a history of the file itself
 * rather than of the project around it. A revision that removed the file comes back marked as deleted.
 *
 * The endpoint is a sibling of the files API rather than a path inside it, so a project folder named
 * {@code history} keeps its own address.
 */
export async function getFileRevisions(
    projectId: string,
    path: string,
    query: RevisionQuery = {}
): Promise<RevisionPage> {
    const url = `/projects/${toUrlSafeId(projectId)}/file-history/${encodeProjectPath(path)}?${revisionQueryParams(query)}`
    return toRevisionPage(await apiCall(url, undefined, { throwError: true }), query)
}

/**
 * Whether the project holds the file at the given revision.
 *
 * A file added later does not exist in the revisions before it, and the browser answers a refused download
 * by quietly downloading nothing — so the callers that let a user reach back into the history ask first.
 *
 * Only a "not found" answers false; any other failure is raised, so a broken connection is never reported
 * as a missing file.
 */
export async function fileExistsAt(projectId: string, path: string, version?: string): Promise<boolean> {
    const params = new URLSearchParams({ view: 'meta' })
    if (version) {
        params.set('version', version)
    }
    try {
        await apiCall(`${fileUrl(projectId, path)}?${params}`, undefined, LOCAL_LOAD_API_OPTIONS)
        return true
    } catch (error) {
        if (isApiHttpError(error) && error.status === 404) {
            return false
        }
        throw error
    }
}

/**
 * Trigger a browser download of a single project file.
 *
 * @param version revision to download; omit for the workspace copy, which carries the local changes of a
 *                project being edited
 */
export function downloadFile(projectId: string, path: string, version?: string): void {
    const params = new URLSearchParams({ download: 'true' })
    if (version) {
        params.set('version', version)
    }
    triggerDownload(
        `${CONFIG.CONTEXT}/web${fileUrl(projectId, path)}?${params}`,
        path.slice(path.lastIndexOf('/') + 1)
    )
}

/** Trigger a browser download of a project folder as a zip archive (the trailing slash selects the folder). */
export function downloadFolder(projectId: string, path: string): void {
    const name = path.slice(path.lastIndexOf('/') + 1)
    triggerDownload(`${CONFIG.CONTEXT}/web${fileUrl(projectId, path)}/?download=true`, `${name}.zip`)
}

/**
 * Upload a file into a project folder (empty path targets the project root) under the given name. The
 * backend enforces the WRITE/CREATE grant.
 */
export async function uploadFile(projectId: string, targetPath: string, file: File, name: string): Promise<void> {
    await upload(projectId, targetPath, [{ file, name }])
}

/** Upload files into a project folder, each under its own name. */
export async function uploadFiles(projectId: string, targetPath: string, files: File[]): Promise<void> {
    await upload(projectId, targetPath, files.map(file => ({ file, name: file.name })))
}

async function upload(
    projectId: string,
    targetPath: string,
    entries: Array<{ file: File, name: string }>
): Promise<void> {
    const form = new FormData()
    // The third argument is the name the file lands under, which the user may have changed.
    entries.forEach(entry => form.append('file', entry.file, entry.name))
    await apiCall(fileUrl(projectId, targetPath), { method: 'POST', body: form }, { throwError: true })
}
