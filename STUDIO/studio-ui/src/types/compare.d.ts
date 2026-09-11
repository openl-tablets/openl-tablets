import type { RawTableCell } from './tables'

/** How an element of the second file relates to the same element of the first one. */
export type ComparisonNodeStatus = 'equal' | 'changed' | 'added' | 'removed'

/** Kind of element in a comparison tree. */
export type ComparisonNodeType = 'sheet' | 'table'

/** A property of a table that reads differently in the two files, such as its name or its size. */
export interface ComparisonPropertyChange {
    property: string
    first?: string | null
    second?: string | null
}

/** An element of the comparison tree: a sheet, or a table of a sheet. */
export interface ComparisonNode {
    /** Identifier of the element within its comparison; a table is read by it. */
    id: string
    name: string
    type: ComparisonNodeType
    status: ComparisonNodeStatus
    /** Properties that read differently in the two files; absent when there are none. */
    changes?: ComparisonPropertyChange[]
    /** Tables of a sheet; absent for a table. */
    children?: ComparisonNode[]
}

/** What the two compared files hold, grouped by sheet. */
export interface Comparison {
    id: string
    /** Whether the two files hold the same elements with the same content. */
    identical: boolean
    sheets: ComparisonNode[]
}

/** One side of a compared table. */
export interface ComparisonSide {
    /** The table as a matrix of cells, with its merges and its Excel styling. */
    source: RawTableCell[][]
    /** Addresses of the cells that read differently in the other file, in A1 notation; absent when none do. */
    changedCells?: string[]
}

/** A table of the comparison, as it stands in each of the two files. */
export interface ComparisonTable {
    id: string
    name: string
    status: ComparisonNodeStatus
    /** Absent when only the second file holds the table. */
    first?: ComparisonSide
    /** Absent when only the first file holds the table. */
    second?: ComparisonSide
}

/** How a comparison is going, as the server reports it over the WebSocket: as any other work. */
export type ComparisonStatus = ExecutionStatus
