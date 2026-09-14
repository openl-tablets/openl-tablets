import type { RawCellStyleInput, RawTableCell, RawTableCellInput, TableEdit } from 'types/tables'

/** Where a cell sits in the table, as it stands on screen. */
export interface CellAt {
    row: number
    column: number
}

/**
 * One thing the reader did, in the coordinates the table had when they did it.
 *
 * <p>These are kept in the order they were made and replayed over the table that was read, so taking one back
 * needs nothing beyond dropping it from the list. They are turned into the edits the API takes only when the
 * reader saves.
 */
export type EditStep =
    | { kind: 'value', at: CellAt, value: string }
    | { kind: 'style', at: CellAt, style: RawCellStyleInput }
    | { kind: 'insertRow', at: number }
    | { kind: 'removeRow', at: number }
    | { kind: 'insertColumn', at: number }
    | { kind: 'removeColumn', at: number }

/** What the reader has done, and what they took back and may put again. */
export interface EditBuffer {
    steps: EditStep[]
    undone: EditStep[]
}

export const NO_EDITS: EditBuffer = { steps: [], undone: []}

/** Whether the two positions name the same cell. */
export const sameCell = (one: CellAt | null, other: CellAt | null): boolean =>
    one !== null && other !== null && one.row === other.row && one.column === other.column

/** How a cell is named among the ones the reader touched, by what it is rather than where it sits. */
const cellKey = (rowId: string, columnId: string): string => `${rowId}|${columnId}`

/** The table as the steps leave it, with what each row and column is, and what the reader did to its cells. */
export interface EditedTable {
    rows: RawTableCell[][]
    /** What each row is: `o<index>` for one the table was read with, `n<number>` for one the reader added. */
    rowIds: string[]
    columnIds: string[]
    /** Cells whose value the reader wrote, named by what they are. */
    touched: Set<string>
    /** The styling the reader asked for, cell by cell, named by what they are. */
    styled: Map<string, RawCellStyleInput>
}

const blank = (): RawTableCell => ({ value: '' })

const idsOf = (count: number): string[] => Array.from({ length: count }, (_, index) => `o${index}`)

/** The index the row or column was read at, or null when the reader added it. */
const readAt = (id: string): number | null => (id.startsWith('o') ? Number(id.slice(1)) : null)

/** Applies one step, and answers how many rows and columns the reader has added in all. */
const apply = (state: EditedTable, step: EditStep, added: number): number => {
    switch (step.kind) {
        case 'value':
        case 'style': {
            const row = state.rows[step.at.row]
            const cell = row?.[step.at.column]
            const rowId = state.rowIds[step.at.row]
            const columnId = state.columnIds[step.at.column]
            if (row !== undefined && cell !== undefined && rowId !== undefined && columnId !== undefined) {
                const key = cellKey(rowId, columnId)
                if (step.kind === 'value') {
                    // What the reader typed stands on its own; the formula the cell was written with is gone.
                    const { formula, ...kept } = cell
                    row[step.at.column] = { ...kept, value: step.value }
                    state.touched.add(key)
                } else {
                    state.styled.set(key, { ...state.styled.get(key), ...step.style })
                    row[step.at.column] = { ...cell, style: { ...cell.style, ...step.style } }
                }
            }
            return added
        }
        case 'insertRow':
            state.rows.splice(step.at, 0, Array.from({ length: state.columnIds.length }, blank))
            state.rowIds.splice(step.at, 0, `n${added}`)
            return added + 1
        case 'removeRow':
            state.rows.splice(step.at, 1)
            state.rowIds.splice(step.at, 1)
            return added
        case 'insertColumn':
            state.rows.forEach(row => row.splice(step.at, 0, blank()))
            state.columnIds.splice(step.at, 0, `n${added}`)
            return added + 1
        case 'removeColumn':
            state.rows.forEach(row => row.splice(step.at, 1))
            state.columnIds.splice(step.at, 1)
            return added
    }
}

/**
 * The table as the steps leave it.
 *
 * <p>Each step is replayed at the place it was made, which is what it meant at the time: a row added later moves
 * everything below it, and a value written before that move still lands where the reader wrote it.
 */
export const replay = (rows: RawTableCell[][], steps: EditStep[]): EditedTable => {
    const state: EditedTable = {
        rows: rows.map(row => [...row]),
        rowIds: idsOf(rows.length),
        columnIds: idsOf(rows[0]?.length ?? 0),
        touched: new Set(),
        styled: new Map(),
    }
    let added = 0
    for (const step of steps) {
        added = apply(state, step, added)
    }
    return state
}

/** The buffer with one more step in it; a new step leaves nothing to put back. */
export const withStep = (buffer: EditBuffer, step: EditStep): EditBuffer => {
    const last = buffer.steps.at(-1)
    // Writing a cell the last step already wrote replaces it: the reader means the value they ended with.
    const replaces = last?.kind === 'value' && step.kind === 'value' && sameCell(last.at, step.at)
    return {
        steps: replaces ? [...buffer.steps.slice(0, -1), step] : [...buffer.steps, step],
        undone: [],
    }
}

/** The buffer with its last step taken back, ready to be put again. */
export const undo = (buffer: EditBuffer): EditBuffer => {
    const last = buffer.steps.at(-1)
    return last === undefined
        ? buffer
        : { steps: buffer.steps.slice(0, -1), undone: [last, ...buffer.undone]}
}

/** The buffer with the step that was last taken back put again. */
export const redo = (buffer: EditBuffer): EditBuffer => {
    const [next, ...rest] = buffer.undone
    return next === undefined ? buffer : { steps: [...buffer.steps, next], undone: rest }
}

/** What a cell holds, as the API takes it. */
const written = (cell: RawTableCell | undefined): RawTableCellInput => ({ value: cell?.value ?? '' })

/** Whether the cell carries nothing at all, so a line of such cells would split the table. */
const empty = (cell: RawTableCell | undefined): boolean => cell?.value == null || String(cell.value).trim() === ''

/**
 * Why the table cannot be written yet, or null when it can.
 *
 * <p>OpenL reads a table as the block of filled cells reachable from its corner, so a row or a column left
 * entirely blank would end the table there and drop everything beyond it. The server refuses such a write, and
 * the reader is told before it is sent.
 */
export const blankLine = (state: EditedTable): 'row' | 'column' | null => {
    const addedRow = state.rowIds.some((id, index) =>
        readAt(id) === null && (state.rows[index] ?? []).every(empty))
    if (addedRow) {
        return 'row'
    }
    const addedColumn = state.columnIds.some((id, index) =>
        readAt(id) === null && state.rows.every(row => empty(row[index])))
    return addedColumn ? 'column' : null
}

/** The columns the reader took away and the ones they added, in an order the table can take them. */
const columnEdits = (width: number, state: EditedTable): TableEdit[] => {
    const edits: TableEdit[] = []
    const kept = new Set(state.columnIds.map(readAt))
    // Highest position first, so taking one away does not move the ones before it.
    for (let column = width - 1; column >= 0; column--) {
        if (!kept.has(column)) {
            edits.push({ operation: 'delete', target: { type: 'columns', position: column, count: 1 } })
        }
    }
    state.columnIds.forEach((id, column) => {
        if (readAt(id) === null) {
            // Only the rows the table already had are there at this point; the added ones come with their own.
            const cells = state.rows
                .filter((_, row) => readAt(state.rowIds[row] ?? '') !== null)
                .map(row => written(row[column]))
            edits.push({ operation: 'insert', target: { type: 'columns', position: column, cells: [cells]} })
        }
    })
    return edits
}

/** The rows the reader took away and the ones they added, each carrying the values it ended with. */
const rowEdits = (height: number, state: EditedTable): TableEdit[] => {
    const edits: TableEdit[] = []
    const kept = new Set(state.rowIds.map(readAt))
    for (let row = height - 1; row >= 0; row--) {
        if (!kept.has(row)) {
            edits.push({ operation: 'delete', target: { type: 'rows', position: row, count: 1 } })
        }
    }
    state.rowIds.forEach((id, row) => {
        if (readAt(id) === null) {
            edits.push({
                operation: 'insert',
                target: { type: 'rows', position: row, cells: [(state.rows[row] ?? []).map(written)]},
            })
        }
    })
    return edits
}

/** The cells whose value differs from the one the table was read with, addressed as they stand now. */
const valueEdits = (original: RawTableCell[][], state: EditedTable): TableEdit[] => {
    const edits: TableEdit[] = []
    state.rows.forEach((cells, row) => {
        const wasRow = readAt(state.rowIds[row] ?? '')
        cells.forEach((cell, column) => {
            const wasColumn = readAt(state.columnIds[column] ?? '')
            const was = wasRow === null || wasColumn === null ? undefined : original[wasRow]?.[wasColumn]
            if (was !== undefined && String(was.value ?? '') !== String(cell.value ?? '')) {
                edits.push({ operation: 'update', target: { type: 'cell', row, column, value: cell.value ?? '' } })
            }
        })
    })
    return edits
}

/** The styling the reader asked for, addressed as the cells stand now. */
const styleEdits = (state: EditedTable): TableEdit[] => {
    const edits: TableEdit[] = []
    state.rows.forEach((cells, row) => {
        cells.forEach((_, column) => {
            const style = state.styled.get(cellKey(state.rowIds[row] ?? '', state.columnIds[column] ?? ''))
            if (style !== undefined) {
                edits.push({
                    operation: 'style',
                    target: { type: 'cells', row, column, rowspan: 1, colspan: 1, style },
                })
            }
        })
    })
    return edits
}

/**
 * The edits to send, in an order the table can take them.
 *
 * <p>What the reader did is not sent step by step: the table as they left it is compared with the table that was
 * read, and only the difference crosses the wire. Rows and columns they took away go first, highest position
 * first so the ones before them do not move; what they added follows, carrying the values it ended with; then
 * the cells they wrote, and last the styling they asked for.
 *
 * <p>A cell written and written back to what it held produces nothing at all.
 */
export const compile = (original: RawTableCell[][], state: EditedTable): TableEdit[] => [
    // Columns first: taking one away or adding one moves no row, so every row is addressed the same after it.
    ...columnEdits(original[0]?.length ?? 0, state),
    ...rowEdits(original.length, state),
    ...valueEdits(original, state),
    ...styleEdits(state),
]

/** How a cell is named among the ones the reader touched, by where it sits now. */
export const keyOf = (state: EditedTable, at: CellAt): string =>
    cellKey(state.rowIds[at.row] ?? '', state.columnIds[at.column] ?? '')
