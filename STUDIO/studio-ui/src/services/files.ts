import apiCall from './apiCall'
import CONFIG from './config'
import { getProjectFiles } from './repositories'
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
    `/projects/${encodeURIComponent(projectId)}/files/${path.split('/').map(encodeURIComponent).join('/')}`

/**
 * Read a project file's raw text content, optionally at a specific historical revision. The endpoint
 * returns the file bytes with the file's own content type, so empty text responses are preserved.
 */
export async function getFileContent(projectId: string, path: string, version?: string): Promise<string> {
    const query = version ? `?version=${encodeURIComponent(version)}` : ''
    const content = await apiCall(
        `${fileUrl(projectId, path)}${query}`,
        undefined,
        { throwError: true, preserveEmptyText: true }
    )
    return typeof content === 'string' ? content : ''
}

/** Read a project file's raw bytes, optionally at a specific historical revision. */
export async function getFileBlob(projectId: string, path: string, version?: string): Promise<Blob> {
    const query = version ? `?version=${encodeURIComponent(version)}` : ''
    return await apiCall(
        `${fileUrl(projectId, path)}${query}`,
        undefined,
        { throwError: true, responseType: 'blob' }
    ) as Blob
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
        `/projects/${encodeURIComponent(projectId)}/file-move`,
        { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sourcePath, destinationPath }) },
        { throwError: true }
    )
}

/** Copy a file within a project to a new path. */
export async function copyFile(projectId: string, sourcePath: string, destinationPath: string): Promise<void> {
    await apiCall(
        `/projects/${encodeURIComponent(projectId)}/file-copy`,
        { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ sourcePath, destinationPath }) },
        { throwError: true }
    )
}

/** Trigger a browser download of a single project file. */
export function downloadFile(projectId: string, path: string): void {
    triggerDownload(`${CONFIG.CONTEXT}/web${fileUrl(projectId, path)}?download=true`, path.slice(path.lastIndexOf('/') + 1))
}

/** Trigger a browser download of a project folder as a zip archive (the trailing slash selects the folder). */
export function downloadFolder(projectId: string, path: string): void {
    const name = path.slice(path.lastIndexOf('/') + 1)
    triggerDownload(`${CONFIG.CONTEXT}/web${fileUrl(projectId, path)}/?download=true`, `${name}.zip`)
}

/**
 * Upload one or more files into a project folder (empty path targets the project root). The backend
 * enforces the WRITE/CREATE grant; each file keeps its own name.
 */
export async function uploadFiles(projectId: string, targetPath: string, files: File[]): Promise<void> {
    const form = new FormData()
    for (const file of files) {
        form.append('file', file, file.name)
    }
    await apiCall(fileUrl(projectId, targetPath), { method: 'POST', body: form }, { throwError: true })
}
