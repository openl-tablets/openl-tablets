import type { RawTableCell, TableEdit } from 'types/tables'

/** Where a cell sits in the table, as the raw read reports it. */
export interface CellAt {
    row: number
    column: number
}

/** The edits a reader has made, what they undid, and the table as those edits leave it. */
export interface EditBuffer {
    /** The edits to send, in the order they were made. */
    edits: TableEdit[]
    /** What was undone, newest first, waiting to be put back. */
    undone: TableEdit[]
}

export const NO_EDITS: EditBuffer = { edits: [], undone: []}

/** Whether the two positions name the same cell. */
export const sameCell = (one: CellAt | null, other: CellAt | null): boolean =>
    one !== null && other !== null && one.row === other.row && one.column === other.column

/** The cell an edit writes to. */
const cellOf = (edit: TableEdit): CellAt => ({ row: edit.target.row, column: edit.target.column })

/**
 * The buffer with one more edit in it.
 *
 * <p>Writing a cell that the last edit already wrote replaces that edit rather than following it: a reader who
 * types a value and then corrects it means the value they ended with, and the table is asked to write it once.
 *
 * <p>A new edit leaves nothing to put back, so what was undone is dropped.
 */
export const withEdit = (buffer: EditBuffer, edit: TableEdit): EditBuffer => {
    const last = buffer.edits.at(-1)
    const edits = last && sameCell(cellOf(last), cellOf(edit))
        ? [...buffer.edits.slice(0, -1), edit]
        : [...buffer.edits, edit]
    return { edits, undone: []}
}

/** The buffer with its last edit taken back, ready to be put again. */
export const undo = (buffer: EditBuffer): EditBuffer => {
    const last = buffer.edits.at(-1)
    return last === undefined
        ? buffer
        : { edits: buffer.edits.slice(0, -1), undone: [last, ...buffer.undone]}
}

/** The buffer with the edit that was last taken back put again. */
export const redo = (buffer: EditBuffer): EditBuffer => {
    const [next, ...rest] = buffer.undone
    return next === undefined ? buffer : { edits: [...buffer.edits, next], undone: rest }
}

/**
 * The table as the edits leave it, and which of its cells they touched.
 *
 * <p>The edits are replayed over the table that was read rather than applied where they were made, so taking one
 * back needs nothing beyond dropping it from the list.
 */
export const replay = (rows: RawTableCell[][], edits: TableEdit[]): {
    rows: RawTableCell[][]
    touched: Set<string>
} => {
    if (edits.length === 0) {
        return { rows, touched: new Set() }
    }
    const written = rows.map(row => [...row])
    const touched = new Set<string>()
    for (const edit of edits) {
        const { row, column, value } = edit.target
        const line = written[row]
        const cell = line?.[column]
        if (line !== undefined && cell !== undefined) {
            // What the reader typed stands on its own; whatever formula the cell was written with is gone.
            const { formula, ...kept } = cell
            line[column] = { ...kept, value }
            touched.add(keyOf({ row, column }))
        }
    }
    return { rows: written, touched }
}

/** How a cell is named among the ones an edit touched. */
export const keyOf = (at: CellAt): string => `${at.row}:${at.column}`
