import { notification } from 'antd'
import apiCall, { type ApiCallOptions } from './apiCall'
import i18n from '../i18n'

const PROJECT_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

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
            description: error instanceof Error ? error.message : String(error),
        })
        return false
    }
}
