import { notification } from 'antd'
import apiCall, { type ApiCallOptions } from './apiCall'
import i18n from '../i18n'

/**
 * Options for branch mutations: surface errors to the caller (so we can show our own
 * notification with the backend message) and skip the React full-page error takeover,
 * since branch deletion is triggered from the repository editor rather than a route.
 */
const BRANCH_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

/**
 * Delete a branch from the repository that hosts the project.
 *
 * Shows a success notification when the branch is removed, or an error notification
 * carrying the backend message on failure. Returns whether the deletion succeeded, so
 * callers can react (e.g. close a dialog and refresh) without rendering feedback themselves.
 *
 * @param projectId URL-path-safe project identifier (URL-safe Base64 id or a plain business name)
 * @param branch    branch to delete; may contain '/' separators
 * @param force     bypass protected-branch restrictions for eligible users
 * @returns {@code true} when the branch was deleted, {@code false} otherwise
 */
export async function deleteBranch(projectId: string, branch: string, force = false): Promise<boolean> {
    const query = force ? '?force=true' : ''
    // Branch names may contain '/' (kept as path separators for the {*branch} mapping); encode each
    // segment so reserved characters such as '#' or '%' do not corrupt the URL.
    const encodedBranch = branch.split('/').map(encodeURIComponent).join('/')
    try {
        await apiCall(
            `/projects/${projectId}/branches/${encodedBranch}${query}`,
            { method: 'DELETE' },
            BRANCH_API_OPTIONS
        )
        notification.success({
            title: i18n.t('repository:notifications.branch_deleted'),
            description: i18n.t('repository:notifications.branch_deleted_description', { branch }),
        })
        return true
    } catch (error) {
        notification.error({
            title: i18n.t('repository:notifications.branch_delete_failed'),
            description: error instanceof Error ? error.message : String(error),
        })
        return false
    }
}
