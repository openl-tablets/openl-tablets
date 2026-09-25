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
    | { kind: 'removeRow', at: number, lines: number }
    | { kind: 'insertColumn', at: number }
    | { kind: 'removeColumn', at: number, lines: number }

/** What the reader has done, and what they took back and may put again. */
interface EditBuffer {
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

/**
 * A blank cell wearing the styling of the one it is laid down beside.
 *
 * <p>A line added to a table takes the look of the line it is written from — the row above it, the column it
 * pushes aside — which is what the workbook ends up holding. The table on screen says so from the start rather
 * than only once it is saved and read again.
 */
const blankLike = (cell: RawTableCell | undefined): RawTableCell =>
    (cell?.style === undefined ? { value: '' } : { value: '', style: cell.style })

const idsOf = (count: number): string[] => Array.from({ length: count }, (_, index) => `o${index}`)

/** The index the row or column was read at, or null when the reader added it. */
const readAt = (id: string): number | null => (id.startsWith('o') ? Number(id.slice(1)) : null)

/** A block of cells the workbook holds as one: where it starts, and how far it reaches. */
interface Merge {
    row: number
    column: number
    rows: number
    columns: number
}

/** How far a merge reaches along one direction of the table: the line it starts at, and how many it takes. */
interface Reach {
    at: number
    lines: number
}

/** One direction of the table, so a line laid down or taken away reads the same down it and across it. */
interface Axis {
    reach: (merge: Merge) => Reach
    moved: (merge: Merge, reach: Reach) => Merge
}

const DOWN: Axis = {
    reach: merge => ({ at: merge.row, lines: merge.rows }),
    moved: (merge, reach) => ({ ...merge, row: reach.at, rows: reach.lines }),
}

const ACROSS: Axis = {
    reach: merge => ({ at: merge.column, lines: merge.columns }),
    moved: (merge, reach) => ({ ...merge, column: reach.at, columns: reach.lines }),
}

/** The merges of a table, read off the spans its cells carry. */
const mergesOf = (rows: RawTableCell[][]): Merge[] => {
    const merges: Merge[] = []
    rows.forEach((cells, row) => cells.forEach((cell, column) => {
        if (cell.covered !== true && ((cell.rowspan ?? 1) > 1 || (cell.colspan ?? 1) > 1)) {
            merges.push({ row, column, rows: cell.rowspan ?? 1, columns: cell.colspan ?? 1 })
        }
    }))
    return merges
}

/** A cell a merge reaches over, which holds nothing of its own — the way a table is read with one. */
const COVERED: RawTableCell = { covered: true }

/**
 * The table laid out with the given merges: the cell a merge starts at reaches over the rest of it, and every
 * cell it reaches holds nothing.
 *
 * <p>A table is read this way and drawn this way, so it has to stay this way while it is edited. A span left
 * reaching over a line that is no longer beneath it draws the lines after it out of their columns, and a
 * reader then writes into a cell of one column under the heading of another.
 */
const laidOut = (rows: RawTableCell[][], merges: Merge[]): RawTableCell[][] => {
    const starts = new Map<string, Merge>()
    const covered = new Set<string>()
    for (const merge of merges) {
        starts.set(`${merge.row}:${merge.column}`, merge)
        for (let row = merge.row; row < merge.row + merge.rows; row++) {
            for (let column = merge.column; column < merge.column + merge.columns; column++) {
                covered.add(`${row}:${column}`)
            }
        }
        covered.delete(`${merge.row}:${merge.column}`)
    }
    return rows.map((cells, row) => cells.map((cell, column) => {
        const where = `${row}:${column}`
        if (covered.has(where)) {
            return COVERED
        }
        const { colspan, covered: reached, rowspan, ...kept } = cell
        const merge = starts.get(where)
        return merge === undefined ? kept : {
            ...kept,
            ...(merge.rows > 1 && { rowspan: merge.rows }),
            ...(merge.columns > 1 && { colspan: merge.columns }),
        }
    }))
}

/**
 * The merges of a table once a line is laid down at `at`, written from the line at `beside`.
 *
 * <p>A merge the line at `beside` sits in reaches over the new line too — the workbook grows it rather than
 * breaking it — and a merge lying along that line is written again on the new one, the way the rest of the
 * line's look is. Everything starting past the new line moves along by one.
 */
const afterInsert = (merges: Merge[], axis: Axis, at: number, beside: number): Merge[] =>
    merges.flatMap(merge => {
        const reach = axis.reach(merge)
        const alongside = reach.at <= beside && beside < reach.at + reach.lines
        if (alongside && reach.lines > 1) {
            return [axis.moved(merge, { at: reach.at, lines: reach.lines + 1 })]
        }
        const pushed = reach.at < at ? merge : axis.moved(merge, { at: reach.at + 1, lines: reach.lines })
        return alongside ? [axis.moved(merge, { at, lines: 1 }), pushed] : [pushed]
    })

/**
 * The merges of a table once the lines from `at` are taken away.
 *
 * <p>A merge loses the lines of it that went and moves back by the ones that went before it. One left holding
 * a single cell is a merge no longer.
 */
const afterRemove = (merges: Merge[], axis: Axis, at: number, lines: number): Merge[] =>
    merges.flatMap(merge => {
        const reach = axis.reach(merge)
        const taken = Math.max(0, Math.min(reach.at + reach.lines, at + lines) - Math.max(reach.at, at))
        const before = Math.max(0, Math.min(reach.at, at + lines) - at)
        const left = axis.moved(merge, { at: reach.at - before, lines: reach.lines - taken })
        return left.rows > 1 || left.columns > 1 ? [left] : []
    })

/**
 * Carries what a merge holds to the cell that becomes its first, where the line it is held in is going.
 *
 * <p>A merge keeps its value in the cell it starts at and nothing in the cells it reaches over. Take that
 * first line away and the merge still stands — over cells that hold nothing, so a header banked across the
 * table would read as blank. The workbook carries the value along instead, and so does this.
 */
const carryOrigins = (rows: RawTableCell[][], merges: Merge[], axis: Axis, at: number, lines: number): void => {
    for (const merge of merges) {
        const reach = axis.reach(merge)
        const held = rows[merge.row]?.[merge.column]
        // Only a merge whose first line is among the ones going, and which reaches past them, has anything
        // to carry: one that goes whole carries nothing, and one starting earlier keeps the cell it is in.
        if (held === undefined || reach.at < at || reach.at >= at + lines
                || reach.at + reach.lines <= at + lines) {
            continue
        }
        const to = axis.moved(merge, { at: at + lines, lines: reach.lines })
        const row = rows[to.row]
        if (row !== undefined) {
            row[to.column] = held
        }
    }
}

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
        case 'insertRow': {
            const above = state.rows[step.at - 1]
            const merges = afterInsert(mergesOf(state.rows), DOWN, step.at, step.at - 1)
            state.rows.splice(step.at, 0,
                Array.from({ length: state.columnIds.length }, (_, column) => blankLike(above?.[column])))
            state.rowIds.splice(step.at, 0, `n${added}`)
            state.rows = laidOut(state.rows, merges)
            return added + 1
        }
        case 'removeRow':
            removeLines(state, DOWN, step.at, step.lines)
            return added
        case 'insertColumn': {
            const merges = afterInsert(mergesOf(state.rows), ACROSS, step.at, step.at)
            state.rows.forEach(row => {
                // A cell the new column falls inside keeps its place, and the blank is laid down within the
                // merge: the workbook grows the merge from where it began rather than moving it along.
                const aside = row[step.at]
                row.splice(step.at + ((aside?.colspan ?? 1) > 1 ? 1 : 0), 0, blankLike(aside))
            })
            state.columnIds.splice(step.at, 0, `n${added}`)
            state.rows = laidOut(state.rows, merges)
            return added + 1
        }
        case 'removeColumn':
            removeLines(state, ACROSS, step.at, step.lines)
            return added
    }
}

/**
 * Takes lines away along the axis, and leaves the groups they ran through standing over what is left.
 *
 * <p>A group the lines were the beginning of is carried down to the first line still there, so what it held
 * is not lost with the line that named it.
 */
const removeLines = (state: EditedTable, axis: Axis, at: number, lines: number): void => {
    const standing = mergesOf(state.rows)
    carryOrigins(state.rows, standing, axis, at, lines)
    const merges = afterRemove(standing, axis, at, lines)
    if (axis === DOWN) {
        state.rows.splice(at, lines)
        state.rowIds.splice(at, lines)
    } else {
        state.rows.forEach(row => row.splice(at, lines))
        state.columnIds.splice(at, lines)
    }
    state.rows = laidOut(state.rows, merges)
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

/**
 * What a cell holds, as the API takes it.
 *
 * <p>A cell a merge reaches over says so rather than carrying a blank: its value lives in the cell the merge
 * starts at, and a blank written over it would be dropped without a word.
 */
const written = (cell: RawTableCell | undefined): RawTableCellInput =>
    (cell?.covered === true ? { value: '', covered: true } : { value: cell?.value ?? '' })

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

/** The lines the reader took away, highest first, so taking one away does not move the ones before it. */
const lineDeletes = (count: number, ids: string[], type: 'rows' | 'columns'): TableEdit[] => {
    const edits: TableEdit[] = []
    const kept = new Set(ids.map(readAt))
    for (let line = count - 1; line >= 0; line--) {
        if (!kept.has(line)) {
            edits.push(type === 'rows'
                ? { operation: 'delete', target: { type: 'rows', position: line, count: 1 } }
                : { operation: 'delete', target: { type: 'columns', position: line, count: 1 } })
        }
    }
    return edits
}

/** The columns the reader added, each carrying a cell for every row the table has by then. */
const columnInserts = (state: EditedTable): TableEdit[] => {
    const edits: TableEdit[] = []
    state.columnIds.forEach((id, column) => {
        if (readAt(id) === null) {
            // The rows the reader took away are gone by now and the ones they added are not there yet, so the
            // column is as tall as the rows the table already had and kept — which is what the table is then.
            const cells = state.rows
                .filter((_, row) => readAt(state.rowIds[row] ?? '') !== null)
                .map(row => written(row[column]))
            edits.push({ operation: 'insert', target: { type: 'columns', position: column, cells: [cells]} })
        }
    })
    return edits
}

/** The rows the reader added, each carrying the values it ended with — the added columns among them. */
const rowInserts = (state: EditedTable): TableEdit[] => {
    const edits: TableEdit[] = []
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
            // A cell a merge has grown over holds nothing of its own, and the API refuses a write to one.
            if (cell.covered !== true && was !== undefined && String(was.value ?? '') !== String(cell.value ?? '')) {
                edits.push({ operation: 'update', target: { type: 'cell', row, column, value: cell.value ?? '' } })
            }
        })
    })
    return edits
}

/** The styling the reader asked for, addressed as the cells stand now. */
const styleEdits = (state: EditedTable): TableEdit[] => {
    // The reader styles a few cells of a table that holds thousands, so the asking is read from what they
    // did and placed, rather than every cell being asked what they did to it.
    const rowAt = new Map(state.rowIds.map((id, row) => [id, row]))
    const columnAt = new Map(state.columnIds.map((id, column) => [id, column]))
    const placed: { row: number, column: number, style: RawCellStyleInput }[] = []
    state.styled.forEach((style, key) => {
        const [rowId = '', columnId = ''] = key.split('|')
        const row = rowAt.get(rowId)
        const column = columnAt.get(columnId)
        // A cell whose row or column the reader went on to take away has nowhere left to be styled.
        if (row !== undefined && column !== undefined) {
            placed.push({ row, column, style })
        }
    })
    return placed
        .sort((one, other) => one.row - other.row || one.column - other.column)
        .map(({ row, column, style }) => ({
            operation: 'style',
            target: { type: 'cells', row, column, rowspan: 1, colspan: 1, style },
        }))
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
    // What was taken away goes first, so nothing added has to be addressed around a line that is about to go.
    // Rows are taken away before a column is added because an added column carries a cell per row, and the
    // table has to be the height that column was built for.
    ...lineDeletes(original[0]?.length ?? 0, state.columnIds, 'columns'),
    ...lineDeletes(original.length, state.rowIds, 'rows'),
    ...columnInserts(state),
    // An added row carries a cell per column, the added ones among them, so it goes in once they are there.
    ...rowInserts(state),
    ...valueEdits(original, state),
    ...styleEdits(state),
]

/** How a cell is named among the ones the reader touched, by where it sits now. */
export const keyOf = (state: EditedTable, at: CellAt): string =>
    cellKey(state.rowIds[at.row] ?? '', state.columnIds[at.column] ?? '')

/**
 * Where the cell now sitting here stood in the table that was read, or null when the reader added its line.
 *
 * <p>What the compiler knows about a cell — the type it holds, the editor it asks for — was read with the
 * table and is addressed as the table was then. A row or a column laid down since has moved the cell, so
 * asking by where it sits now answers for whichever cell used to stand there: a decimal opens as a whole
 * number, and the value typed is saved without its point.
 */
export const asRead = (state: EditedTable, at: CellAt): CellAt | null => {
    const row = rowAsRead(state, at.row)
    const column = columnAsRead(state, at.column)
    return row === null || column === null ? null : { row, column }
}

/** Where the row now sitting here stood in the table that was read, or null when the reader laid it down. */
export const rowAsRead = (state: EditedTable, row: number): number | null => readAt(state.rowIds[row] ?? '')

/** Where the column now sitting here stood in the table that was read, or null when the reader laid it down. */
export const columnAsRead = (state: EditedTable, column: number): number | null =>
    readAt(state.columnIds[column] ?? '')

/**
 * Where a part of the table beginning at this row of the table that was read begins on screen now.
 *
 * <p>A row the reader laid down stood nowhere in the table that was read, so whether it belongs to a part is
 * settled by where it is drawn: the part begins after the last row that was read before it, wherever that row
 * has been moved to. A part beginning past the last row the table was read with — the first rule of a table
 * that holds none — therefore begins right after the headings, which is where such a rule is written.
 */
export const rowDrawnFrom = (state: EditedTable, read: number): number => drawnFrom(state.rowIds, read)

/** Where a part beginning at this column of the table that was read begins on screen now. */
export const columnDrawnFrom = (state: EditedTable, read: number): number => drawnFrom(state.columnIds, read)

const drawnFrom = (ids: string[], read: number): number =>
    ids.findLastIndex(id => {
        const at = readAt(id)
        return at !== null && at < read
    }) + 1
