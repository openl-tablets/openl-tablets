import apiCall from './apiCall'
import { toUrlSafeId } from './projectId'
import type { Comparison, ComparisonTable } from 'types/compare'

const API_OPTIONS = { throwError: true, suppressErrorPages: true }

/** The topic a comparison reports its progress on. */
export const comparisonStatusTopic = (comparisonId: string): string =>
    `/user/topic/compare/${encodeURIComponent(comparisonId)}/status`

/**
 * Starts comparing two Excel files.
 *
 * Comparison parses both files, so it runs on the server and reports its progress on its own topic.
 * The result is read once it has completed.
 *
 * @returns the identifier the comparison is read and watched by
 */
export const startFileComparison = async (first: File, second: File): Promise<string> => {
    const body = new FormData()
    body.append('file1', first, first.name)
    body.append('file2', second, second.name)
    const response = await apiCall('/compare/files', { method: 'POST', body }, API_OPTIONS) as { id: string }
    return response.id
}

/**
 * Starts comparing two versions of a module from its local history.
 *
 * @param projectId  the project the module belongs to
 * @param moduleName the module, or nothing for the first module of the project
 * @param first      the version shown on the first side
 * @param second     the version shown on the second side
 * @returns the identifier the comparison is read and watched by
 */
export const startLocalHistoryComparison = async (
    projectId: string,
    moduleName: string | undefined,
    first: string,
    second: string
): Promise<string> => {
    const query = moduleName ? `?module=${encodeURIComponent(moduleName)}` : ''
    const response = await apiCall(`/projects/${toUrlSafeId(projectId)}/local-history/compare${query}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ first, second }),
    }, API_OPTIONS) as { id: string }
    return response.id
}

/** What the two compared files hold, grouped by sheet. */
export const getComparison = async (comparisonId: string): Promise<Comparison> =>
    await apiCall(`/compare/${encodeURIComponent(comparisonId)}`, undefined, API_OPTIONS) as Comparison

/** One table of the comparison, as it stands in each of the two files. */
export const getComparisonTable = async (comparisonId: string, tableId: string): Promise<ComparisonTable> =>
    await apiCall(
        `/compare/${encodeURIComponent(comparisonId)}/tables/${encodeURIComponent(tableId)}`,
        undefined,
        API_OPTIONS
    ) as ComparisonTable

/** Releases the comparison and the files it reads. */
export const dropComparison = async (comparisonId: string): Promise<void> => {
    await apiCall(`/compare/${encodeURIComponent(comparisonId)}`, { method: 'DELETE' }, API_OPTIONS)
}
