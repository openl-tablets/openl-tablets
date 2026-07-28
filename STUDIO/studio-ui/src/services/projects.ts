import { notification } from 'antd'
import { errorMessage } from 'utils/errorMessage'
import apiCall, { asArray, type ApiCallOptions } from './apiCall'
import i18n from '../i18n'
import { encodeProjectPath as encodePath, toUrlSafeId } from './projectId'
import type { ProjectModule } from 'types/projects'
import type { ProjectProperty } from 'types/tables'

const PROJECT_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

const projectResource = async <T>(projectId: string, resource: string): Promise<T[]> => asArray(
    await apiCall(`/projects/${toUrlSafeId(projectId)}/${resource}`, undefined, PROJECT_API_OPTIONS)
)

/** Modules the project declares, patterns already resolved to the files they matched. */
export const getProjectModules = (projectId: string): Promise<ProjectModule[]> =>
    projectResource(projectId, 'modules')

/** Worksheets of one module's workbook. */
export const getModuleSheets = (projectId: string, moduleName: string): Promise<string[]> =>
    projectResource(projectId, `modules/${encodeURIComponent(moduleName)}/sheets`)

/**
 * Properties applicable to one place in a workbook. Without a table kind, returns properties for the contents of a
 * Properties table. With a kind, returns properties for the table's own properties section.
 */
export const getProjectProperties = (projectId: string, tableType?: string): Promise<ProjectProperty[]> =>
    projectResource(projectId, `properties${tableType ? `?tableType=${encodeURIComponent(tableType)}` : ''}`)


/** A file to upload into a project, addressed by its project-relative '/'-separated path. */
export interface ProjectUploadEntry {
    path: string
    file: Blob
}

/**
 * Send an upload request and report the outcome. Shows a success notification when the request
 * succeeds, or an error notification carrying the backend message on failure. Returns whether
 * the upload succeeded, so callers can react (e.g. close a dialog and reload) themselves.
 */
async function uploadToProject(
    url: string,
    params: RequestInit,
    success: { title: string, description: string },
    failureTitle: string
): Promise<boolean> {
    try {
        await apiCall(url, params, PROJECT_API_OPTIONS)
        notification.success(success)
        return true
    } catch (error) {
        notification.error({
            title: failureTitle,
            description: errorMessage(error),
        })
        return false
    }
}

/**
 * Update the project content from a zip archive. The working copy is replaced with the archive
 * expanded at the project root: entries overwrite the files with the same path, new entries are
 * added, and project files absent from the archive are deleted.
 *
 * @param projectId   project identifier provided by the backend, in either Base64 alphabet
 * @param projectName project name used in the notification text
 * @param archive     the zip archive content
 * @returns {@code true} when the project was updated, {@code false} otherwise
 */
export async function updateProjectFromZip(projectId: string, projectName: string, archive: Blob): Promise<boolean> {
    return uploadToProject(
        `/projects/${toUrlSafeId(projectId)}/files/?conflictPolicy=REPLACE`,
        { method: 'POST', headers: { 'Content-Type': 'application/zip' }, body: archive },
        {
            title: i18n.t('project:notifications.project_updated'),
            description: i18n.t('project:notifications.project_updated_description', { project: projectName }),
        },
        i18n.t('project:notifications.project_update_failed')
    )
}

/**
 * Update the project content from individually picked files. The working copy is replaced with
 * the entries, each landing at its project-relative path: missing intermediate folders are
 * created, files with the same path are overwritten, and project files absent from the upload
 * are deleted.
 *
 * @param projectId   project identifier provided by the backend, in either Base64 alphabet
 * @param projectName project name used in the notification text
 * @param entries     the files together with their project-relative paths
 * @returns {@code true} when the project was updated, {@code false} otherwise
 */
export async function updateProjectFromFiles(
    projectId: string,
    projectName: string,
    entries: ProjectUploadEntry[]
): Promise<boolean> {
    const formData = new FormData()
    entries.forEach(({ path, file }) => formData.append('file', file, path))
    return uploadToProject(
        `/projects/${toUrlSafeId(projectId)}/files/?conflictPolicy=REPLACE`,
        { method: 'POST', body: formData },
        {
            title: i18n.t('project:notifications.project_updated'),
            description: i18n.t('project:notifications.project_updated_description', { project: projectName }),
        },
        i18n.t('project:notifications.project_update_failed')
    )
}


/**
 * Delete a project from its repository.
 *
 * Shows a success notification when the project is removed, or an error notification
 * carrying the backend message on failure. Returns whether the deletion succeeded, so
 * callers can react (e.g. close a dialog and refresh) without rendering feedback themselves.
 *
 * @param projectId   URL-path-safe project identifier provided by the backend
 * @param projectName project name used in the notification text
 * @param comment     commit message for the deletion; sent as a query parameter
 * @returns {@code true} when the project was deleted, {@code false} otherwise
 */
export async function deleteProject(projectId: string, projectName: string, comment?: string): Promise<boolean> {
    const query = comment ? `?comment=${encodeURIComponent(comment)}` : ''
    try {
        await apiCall(`/projects/${projectId}${query}`, { method: 'DELETE' }, PROJECT_API_OPTIONS)
        notification.success({
            title: i18n.t('repository:notifications.project_deleted'),
            description: i18n.t('repository:notifications.project_deleted_description', { project: projectName }),
        })
        return true
    } catch (error) {
        notification.error({
            title: i18n.t('repository:notifications.project_delete_failed'),
            description: errorMessage(error),
        })
        return false
    }
}

/**
 * Delete a file or folder from the project working copy. The change is staged in the
 * working copy and becomes part of the next project save.
 *
 * Shows a success notification when the resource is removed, or an error notification
 * carrying the backend message on failure. Returns whether the deletion succeeded, so
 * callers can react (e.g. close a dialog and refresh) without rendering feedback themselves.
 *
 * @param projectId URL-path-safe project identifier provided by the backend
 * @param path      file or folder path relative to the project root; may contain '/' separators
 * @param name      file or folder name used in the notification text
 * @param isFolder  whether the resource is a folder; affects the notification wording only
 * @returns {@code true} when the resource was deleted, {@code false} otherwise
 */
export async function deleteProjectFile(
    projectId: string,
    path: string,
    name: string,
    isFolder: boolean
): Promise<boolean> {
    const encodedPath = encodePath(path)
    const kind = isFolder ? 'folder' : 'file'
    try {
        await apiCall(`/projects/${projectId}/files/${encodedPath}`, { method: 'DELETE' }, PROJECT_API_OPTIONS)
        notification.success({
            title: i18n.t(`repository:notifications.${kind}_deleted`),
            description: i18n.t(`repository:notifications.${kind}_deleted_description`, { name }),
        })
        return true
    } catch (error) {
        notification.error({
            title: i18n.t(`repository:notifications.${kind}_delete_failed`),
            description: errorMessage(error),
        })
        return false
    }
}
