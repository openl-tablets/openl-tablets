import apiCall, { asArray, readTaskResult } from './apiCall'
import { toUrlSafeId } from './projectId'
import { decodeText } from '../utils/decodeText'
import { expandCells } from './rawTables'
import type { Comparison, ComparisonSide, ComparisonSideData, ComparisonTable, ComparisonTableData } from 'types/compare'

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
 * A version of a conflicted file: the base both branches started from, the version the workspace holds, or the
 * version being merged in.
 */
export type ConflictSide = 'base' | 'ours' | 'theirs'

/**
 * Starts comparing two versions of a conflicted Excel file.
 *
 * @param first  the version the comparison starts from
 * @param second the version it is compared with
 * @returns the identifier the comparison is read and watched by
 */
export const startConflictComparison = async (
    projectId: string,
    path: string,
    first: ConflictSide,
    second: ConflictSide
): Promise<string> => {
    const response = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/merge/conflicts/compare?file=${encodeURIComponent(path)}`
            + `&first=${first}&second=${second}`,
        { method: 'POST' },
        API_OPTIONS
    ) as { id: string }
    return response.id
}

/** What the merge did to a conflicted file: changed on both sides, or gone from one of them. */
export type ConflictFileStatus = 'modified' | 'deleted'

/** Which versions of a conflicted file hold it. */
export type ConflictFileVersions = Record<ConflictSide, boolean>

/**
 * Which versions of a conflicted file hold it, as the merge reports it.
 *
 * A version the merge does not report on is taken to hold the file.
 */
export const getConflictFileVersions = async (
    projectId: string,
    path: string
): Promise<ConflictFileVersions> => {
    const conflicts = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/merge/conflicts`,
        undefined,
        API_OPTIONS
    ) as { fileAvailability?: Record<string, { base?: boolean; ours?: boolean; theirs?: boolean }> }
    const availability = conflicts.fileAvailability?.[path]
    return {
        base: availability?.base !== false,
        ours: availability?.ours !== false,
        theirs: availability?.theirs !== false,
    }
}

/** Which versions of a conflicted file hold it, as the merge reports it, and whether it is text to merge by lines. */
export interface ConflictFileAvailability {
    ours: boolean
    theirs: boolean
    base: boolean
    /** Whether every version that holds the file holds it as UTF-8 text, which is merged line by line. */
    text: boolean
}

/** Lines of a conflicted text file the merge took by itself, with their line breaks. */
export interface MergedTextBlock {
    kind: 'merged'
    /** The lines the merge took. */
    text: string
    /** The lines of each version, given when a branch changed them; lines no branch changed are the text alone. */
    ours?: string
    base?: string
    theirs?: string
}

/** Lines the branches changed differently: each version with its line breaks; one holding no line is empty. */
export interface ConflictedTextBlock {
    kind: 'conflict'
    ours: string
    base: string
    theirs: string
}

/** A part of a conflicted text file: lines the merge took by itself, or a conflict. */
export type ConflictTextBlock = MergedTextBlock | ConflictedTextBlock

/** A conflicted text file merged line by line: its parts in their order. */
export interface ConflictText {
    blocks: ConflictTextBlock[]
}

/** A conflicted text file merged line by line, for the conflicts of its lines to be resolved in place. */
export const getConflictText = async (projectId: string, path: string): Promise<ConflictText> =>
    await apiCall(
        `/projects/${toUrlSafeId(projectId)}/merge/conflicts/text?file=${encodeURIComponent(path)}`,
        undefined,
        API_OPTIONS
    ) as ConflictText

/**
 * One version of a conflicted file, as text: what a file that is not a workbook is compared as. A file not written in
 * UTF-8 reads as Windows-1252, every byte a character of its own.
 */
export const getConflictFileText = async (
    projectId: string,
    path: string,
    side: ConflictSide
): Promise<string> => {
    const file = await apiCall(
        `/projects/${toUrlSafeId(projectId)}/merge/conflicts/files`
            + `?file=${encodeURIComponent(path)}&side=${side}`,
        undefined,
        { ...API_OPTIONS, responseType: 'blob' }
    ) as Blob
    return decodeText(await file.arrayBuffer())
}

/** Reads a part of a comparison, once the comparison has ended. */
const readComparison = async (url: string): Promise<unknown> =>
    (await readTaskResult(url, undefined, API_OPTIONS)).json()

/** What the two compared files hold, grouped by sheet. */
export const getComparison = async (comparisonId: string): Promise<Comparison> =>
    await readComparison(`/compare/${encodeURIComponent(comparisonId)}`) as Comparison

/** A side of a compared table as a screen reads it: every cell carrying its own style. */
const expandSide = (side: ComparisonSideData): ComparisonSide => {
    const { source, styles, ...rest } = side
    return { ...rest, source: expandCells(source, styles) }
}

/** One table of the comparison, as it stands in each of the two files. */
export const getComparisonTable = async (comparisonId: string, tableId: string): Promise<ComparisonTable> => {
    const { first, second, ...table } = await readComparison(
        `/compare/${encodeURIComponent(comparisonId)}/tables/${encodeURIComponent(tableId)}`
    ) as ComparisonTableData
    const read: ComparisonTable = table
    if (first !== undefined) {
        read.first = expandSide(first)
    }
    if (second !== undefined) {
        read.second = expandSide(second)
    }
    return read
}

/** Releases the comparison and the files it reads. */
export const dropComparison = async (comparisonId: string): Promise<void> => {
    await apiCall(`/compare/${encodeURIComponent(comparisonId)}`, { method: 'DELETE' }, API_OPTIONS)
}
