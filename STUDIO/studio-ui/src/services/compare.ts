import apiCall, { asArray, readTaskResult } from './apiCall'
import { decodeText } from '../utils/decodeText'
import { expandCells } from './rawTables'
import type { Comparison, ComparisonSide, ComparisonSideData, ComparisonTable, ComparisonTableData } from 'types/compare'
import type { RawTableCell, RawTableCellData, RawTableCellStyle } from 'types/tables'

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
    const response = await apiCall(`/projects/${projectId}/local-history/compare${query}`, {
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
        `/projects/${projectId}/compare/files${suffix}`,
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
    const response = await apiCall(`/projects/${projectId}/compare`, {
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
        `/projects/${projectId}/merge/conflicts/compare?file=${encodeURIComponent(path)}`
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
        `/projects/${projectId}/merge/conflicts`,
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
        `/projects/${projectId}/merge/conflicts/text?file=${encodeURIComponent(path)}`,
        undefined,
        API_OPTIONS
    ) as ConflictText

/** How the merge takes a sheet of a conflicted workbook by itself. */
export type ConflictSheetStatus = 'ours' | 'theirs' | 'merged' | 'conflict'

/** Why a sheet of a conflicted workbook is resolved only as a whole. */
export type ConflictSheetReason =
    | 'removedAndChanged'
    | 'addedInBoth'
    | 'visibility'
    | 'pictures'
    | 'settings'
    | 'touchingTables'
    | 'movedContent'

/** Why both branches changed a table of a workbook in ways the merge cannot combine. */
export type ConflictTableReason =
    | 'cells'
    | 'rows'
    | 'rowsAndColumns'
    | 'moved'
    | 'removedAndChanged'
    | 'mergedCells'
    | 'addedInBoth'

/** A table as one version of a conflicted workbook has it. */
export interface ConflictTableVersion {
    /** The place of the table on its sheet, in A1 notation. */
    range: string
    /** The cells of the table, row by row, each carrying its own style; the first rows only of a long table. */
    cells: RawTableCell[][]
    /** How many rows the table has. */
    rows: number
    /**
     * The cells whose value, formula, comment or merging the branch changed against the base version, and the other
     * branch has not alike.
     */
    changedCells: string[]
    /** Whether the branch changed the formatting of cells of the table, and the other branch has not alike. */
    formattingChanged: boolean
    /**
     * The cells whose value, formula, comment or merging both branches changed differently, as this version has
     * them.
     */
    conflictCells: string[]
}

/** A version of a table as the server sends it: each style once, the cells naming theirs by index. */
interface ConflictTableVersionData extends Omit<ConflictTableVersion, 'cells'> {
    cells: RawTableCellData[][]
    styles: RawTableCellStyle[]
}

/** A conflicting table as the server sends it. */
interface ConflictTableData extends Omit<ConflictTable, 'ours' | 'base' | 'theirs'> {
    ours?: ConflictTableVersionData
    base?: ConflictTableVersionData
    theirs?: ConflictTableVersionData
}

/** A conflicted workbook as the server sends it. */
interface ConflictWorkbookData extends Omit<ConflictWorkbook, 'sheets'> {
    sheets: (Omit<ConflictSheet, 'tables'> & { tables: ConflictTableData[] })[]
}

/** A table both branches changed in ways the merge cannot combine: it is taken whole from one version. */
export interface ConflictTable {
    /** Identifies the conflict in a resolution. */
    id: string
    /** The text of the first cell of the table, its header, or its place when the cell is empty. */
    name: string
    reason: ConflictTableReason
    /** The table in each version; absent from a version that has none. */
    ours?: ConflictTableVersion
    base?: ConflictTableVersion
    theirs?: ConflictTableVersion
}

/** A sheet of a conflicted workbook either branch changed. Its name identifies it in a resolution. */
export interface ConflictSheet {
    name: string
    status: ConflictSheetStatus
    /** Why the sheet is resolved only as a whole; absent when its tables are resolved one by one. */
    reason?: ConflictSheetReason
    /** Whether each version has the sheet. */
    ours: boolean
    base: boolean
    theirs: boolean
    /** The conflicting tables of the sheet, from top to bottom. */
    tables: ConflictTable[]
}

/** A conflicted workbook as the merge reads it: the sheets either branch changed, and the conflicts left. */
export interface ConflictWorkbook {
    sheets: ConflictSheet[]
    /** Whether both branches changed the palette of an .xls workbook differently, a conflict of its own. */
    palette: boolean
}

/** Identifies the palette of an .xls workbook among the conflicts of a workbook. */
export const PALETTE_CONFLICT = '/palette'

/** A version of a table as a screen reads it: every cell carrying its own style. */
const expandVersion = ({ cells, styles, ...rest }: ConflictTableVersionData): ConflictTableVersion =>
    ({ ...rest, cells: expandCells(cells, styles) })

/** A conflicted workbook as the merge reads it, for its conflicts to be resolved table by table. */
export const getConflictWorkbook = async (projectId: string, path: string): Promise<ConflictWorkbook> => {
    const workbook = await apiCall(
        `/projects/${projectId}/merge/conflicts/workbook?file=${encodeURIComponent(path)}`,
        undefined,
        API_OPTIONS
    ) as ConflictWorkbookData
    return {
        ...workbook,
        sheets: workbook.sheets.map(sheet => ({
            ...sheet,
            tables: sheet.tables.map(({ ours, base, theirs, ...table }) => ({
                ...table,
                ...(ours && { ours: expandVersion(ours) }),
                ...(base && { base: expandVersion(base) }),
                ...(theirs && { theirs: expandVersion(theirs) }),
            })),
        })),
    }
}

/**
 * Merges a conflicted workbook, each conflict taken whole from the version decided for it, and answers with the
 * merged workbook. Nothing is saved.
 *
 * @param decisions the version each conflict is taken from, by the identifier of the conflict
 */
export const mergeConflictWorkbook = async (
    projectId: string,
    path: string,
    decisions: Record<string, ConflictSide>
): Promise<Blob> =>
    await apiCall(
        `/projects/${projectId}/merge/conflicts/workbook?file=${encodeURIComponent(path)}`,
        {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ decisions: Object.entries(decisions).map(([id, side]) => ({ id, side })) }),
        },
        // The merge changes nothing in the workspace, so it tells no other screen to read it again.
        { ...API_OPTIONS, responseType: 'blob', skipWorkspaceEvent: true }
    ) as Blob

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
        `/projects/${projectId}/merge/conflicts/files`
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
