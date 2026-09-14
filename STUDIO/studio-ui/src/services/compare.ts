import apiCall, { asArray } from './apiCall'
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

/** One side of a comparison of a project: which file, and where it is read from. */
export interface ProjectFileSide {
    /** Path of the file inside the project. */
    path: string
    /** Branch the revision belongs to; left out for the working copy. */
    branch?: string | undefined
    /** Revision to read the file from; left out for the working copy. */
    revision?: string | undefined
}

/**
 * The Excel files of the project that can be compared.
 *
 * @param projectId the project to read
 * @param where     the branch and the revision to read them at, or nothing for the working copy
 * @returns the paths of the files inside the project
 */
export const getProjectCompareFiles = async (
    projectId: string,
    where: { branch?: string | undefined; revision?: string | undefined } = {}
): Promise<string[]> => {
    const query = new URLSearchParams()
    if (where.branch) {
        query.set('branch', where.branch)
    }
    if (where.revision) {
        query.set('revision', where.revision)
    }
    const suffix = query.size > 0 ? `?${query}` : ''
    const response = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/compare/files${suffix}`,
        undefined,
        API_OPTIONS
    )
    return asArray<string>(response)
}

/**
 * Starts comparing two Excel files of a project, each taken from the working copy or from a revision.
 *
 * @returns the identifier the comparison is read and watched by
 */
export const startProjectComparison = async (
    projectId: string,
    first: ProjectFileSide,
    second: ProjectFileSide
): Promise<string> => {
    const response = await apiCall(`/projects/${toUrlSafeId(projectId)}/compare`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ first, second }),
    }, API_OPTIONS) as { id: string }
    return response.id
}

/**
 * Starts comparing the two versions of a conflicted Excel file: the one being merged in against the
 * one the workspace holds.
 *
 * @returns the identifier the comparison is read and watched by
 */
export const startConflictComparison = async (projectId: string, path: string): Promise<string> => {
    const response = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/merge/conflicts/compare?file=${encodeURIComponent(path)}`,
        { method: 'POST' },
        API_OPTIONS
    ) as { id: string }
    return response.id
}

/** What the merge did to a conflicted file: changed on both sides, or gone from one of them. */
export type ConflictFileStatus = 'modified' | 'deleted'

/**
 * What became of a conflicted file, as the merge reports it.
 *
 * A file both versions still hold was modified on both sides; one that a version no longer holds was
 * deleted there, and the two versions cannot be put against each other.
 */
export const getConflictFileStatus = async (
    projectId: string,
    path: string
): Promise<ConflictFileStatus> => {
    const conflicts = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/merge/conflicts`,
        undefined,
        API_OPTIONS
    ) as { fileAvailability?: Record<string, { ours?: boolean; theirs?: boolean }> }
    const availability = conflicts.fileAvailability?.[path]
    return availability && (availability.ours === false || availability.theirs === false)
        ? 'deleted'
        : 'modified'
}

/** One version of a conflicted file, as text: what a file that is not a workbook is compared as. */
export const getConflictFileText = async (
    projectId: string,
    path: string,
    side: 'OURS' | 'THEIRS'
): Promise<string> => {
    const file = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/merge/conflicts/files`
            + `?file=${encodeURIComponent(path)}&side=${side}`,
        undefined,
        { ...API_OPTIONS, responseType: 'blob' }
    ) as Blob
    return await file.text()
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
