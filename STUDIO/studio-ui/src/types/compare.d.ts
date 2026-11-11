import type { RawTableCell, RawTableCellData, RawTableCellStyle } from './tables'

/** How an element of the second file relates to the same element of the first one. */
type ComparisonNodeStatus = 'equal' | 'changed' | 'moved' | 'added' | 'removed'

/** What became of a row or a column of a table both files hold. */
export type ComparisonLineStatus = 'equal' | 'changed' | 'added' | 'removed'

/** Kind of element in a comparison tree. */
type ComparisonNodeType = 'sheet' | 'table'

/** An element of the comparison tree: a sheet, or a table of a sheet. */
export interface ComparisonNode {
    /** Identifier of the element within its comparison; a table is read by it. */
    id: string
    name: string
    type: ComparisonNodeType
    status: ComparisonNodeStatus
    /** Whether the second file moved the table to another place on purpose, whatever else changed. */
    moved?: boolean
    /**
     * Whether the formatting is all that differs in the table: no cell reads differently and neither file holds a line
     * the other does not. Such a table is `changed`, and may be moved as well.
     */
    formattingOnly?: boolean | null
    /** The place of the table in the first file; absent for a sheet and for a table the first file does not hold. */
    firstRange?: string | null
    /** The place of the table in the second file; absent for a sheet and for a table the second file does not hold. */
    secondRange?: string | null
    /** What your version did to the element, in a comparison of a conflicted file with its base. */
    yours?: ComparisonNodeStatus | null
    /** What their version did to the element, in a comparison of a conflicted file with its base. */
    theirs?: ComparisonNodeStatus | null
    /** Whether the table holds cells outside OpenL tables, such as notes or formulas beside them. */
    other?: boolean | null
    /** Whether both versions of a conflict changed cells of the table differently. */
    conflict?: boolean | null
    /** Tables of a sheet; absent for a table. */
    children?: ComparisonNode[]
}

/**
 * A row or a column of a table both files hold, lined up as a text comparison lines up lines.
 *
 * A line only one file holds has no place in the other one.
 */
export interface ComparisonLine {
    /** The line in the first file, from 0; absent when only the second file holds it. */
    first?: number | null
    /** The line in the second file, from 0; absent when only the first file holds it. */
    second?: number | null
    status: ComparisonLineStatus
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
    /**
     * Addresses of the cells that read differently in the other file, in A1 notation; absent when none do.
     * Only the cells of the lines both files hold are listed.
     */
    changedCells?: string[]
}

/** One side of a compared table as the server sends it: the cells name their styles by index into `styles`. */
export interface ComparisonSideData extends Omit<ComparisonSide, 'source'> {
    source: RawTableCellData[][]
    /** The Excel styles the cells of the side use, each once. */
    styles?: RawTableCellStyle[]
}

/** A table of the comparison as the server sends it, each side naming its styles by index. */
export interface ComparisonTableData extends Omit<ComparisonTable, 'first' | 'second'> {
    first?: ComparisonSideData
    second?: ComparisonSideData
}

/** A table of the comparison, as it stands in each of the two files. */
export interface ComparisonTable {
    id: string
    name: string
    status: ComparisonNodeStatus
    /** Whether the second file moved the table to another place on purpose, whatever else changed. */
    moved?: boolean
    /** Whether the formatting of a cell both files hold differs; a change of formatting alone marks no cell. */
    formattingChanged?: boolean
    /** The place of the table in the first file; absent when only the second file holds it. */
    firstRange?: string | null
    /** The place of the table in the second file; absent when only the first file holds it. */
    secondRange?: string | null
    /** The rows, lined up; absent or empty for a table only one file holds. */
    rows?: ComparisonLine[]
    /** The columns, lined up; absent or empty for a table only one file holds. */
    columns?: ComparisonLine[]
    /** Absent when only the second file holds the table. */
    first?: ComparisonSide
    /** Absent when only the first file holds the table. */
    second?: ComparisonSide
}

/** How a comparison is going, as the server reports it over the WebSocket: as any other work. */
export type ComparisonStatus = ExecutionStatus

/**
 * A row or a column of a table, as the base and the two versions of a conflict hold it. A line of the base stands
 * against what each version made of it; a line a version inserted stands against no line of the others.
 */
export interface ConflictComparisonLine {
    /** The line in the base, from 0; absent for a line a version inserted. */
    base?: number | null
    /** The line in your version, from 0; absent when it does not hold the line. */
    yours?: number | null
    /** The line in their version, from 0; absent when it does not hold the line. */
    theirs?: number | null
    /** What your version did to the line: `added` for a line it inserted, `equal` for one their version inserted. */
    yoursStatus: ComparisonLineStatus
    /** What their version did to the line: `added` for a line it inserted, `equal` for one your version inserted. */
    theirsStatus: ComparisonLineStatus
}

/** A version of a table of a conflict: its cells, the ones it changed and the ones both versions changed differently. */
export interface ConflictComparisonSide extends ComparisonSide {
    conflictCells?: string[] | null
}

/** A table of a conflict, as the base and the two versions hold it, the lines lined up by the base. */
export interface ConflictComparisonTable {
    id: string
    name: string
    status: ComparisonNodeStatus
    yoursStatus: ComparisonNodeStatus
    theirsStatus: ComparisonNodeStatus
    baseRange?: string | null
    yoursRange?: string | null
    theirsRange?: string | null
    /** The table as the base holds it; absent for a table a version added. */
    base?: ConflictComparisonSide
    /** The table as your version holds it; absent when it does not hold the table. */
    yours?: ConflictComparisonSide
    /** The table as their version holds it; absent when it does not hold the table. */
    theirs?: ConflictComparisonSide
    rows?: ConflictComparisonLine[]
    columns?: ConflictComparisonLine[]
}

/** A table of a conflict as the server sends it, each version naming its styles by index. */
export interface ConflictComparisonTableData extends Omit<ConflictComparisonTable, 'base' | 'yours' | 'theirs'> {
    base?: ConflictComparisonSideData
    yours?: ConflictComparisonSideData
    theirs?: ConflictComparisonSideData
}

/** A version of a table of a conflict as the server sends it, naming its styles by index. */
export type ConflictComparisonSideData = ComparisonSideData & Pick<ConflictComparisonSide, 'conflictCells'>
