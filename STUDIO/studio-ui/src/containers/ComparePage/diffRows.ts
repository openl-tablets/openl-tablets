import type { RawTableCell } from 'types/tables'
import type { Alignment } from './alignment'

/** How many equal rows are shown on either side of a row that differs. */
const CONTEXT_ROWS = 1

/** A row a table shows: a row lined up, by its index, or a gap standing for the equal rows left out there. */
export type ShownLine = { line: number } | { gap: number }

/** The rows of a table drawn, each a row lined up or a gap, and the cell of each gap with the rows it leaves out. */
export interface ShownGrid {
    rows: RawTableCell[][]
    gaps: ReadonlyMap<RawTableCell, number>
}

/**
 * The rows to draw of a table, or null for the whole table.
 *
 * A row differs when it is added, removed or holds a changed cell. The rows that differ are shown with an equal row
 * on either side of them, and the first row, the header of the table, is always shown. The equal rows left out
 * between them are a gap that says how many they are. A gap would leave out a single row in the space of the row, so
 * that row is shown instead.
 *
 * Every row is kept once a column is added or removed, because such a column runs through the equal rows too. A
 * table with no row that differs is shown whole.
 */
export const rowsToShow = (alignment: Alignment | null, showEqualRows: boolean): ShownLine[] | null => {
    const columnsDiffer = alignment?.columns.some(column => column.status === 'added' || column.status === 'removed')
    return alignment && !columnsDiffer
        ? keptRows(alignment.rows.map(row => row.status !== 'equal'), showEqualRows)
        : null
}

/**
 * The rows to draw of a table whose columns all stand in both versions, or null for the whole table, as
 * {@link rowsToShow} picks them.
 *
 * @param differs whether each row differs
 */
export const keptRows = (differs: readonly boolean[], showEqualRows: boolean): ShownLine[] | null => {
    if (showEqualRows) {
        return null
    }
    const count = differs.length
    const differ = differs.flatMap((rowDiffers, index) => (rowDiffers ? [index] : []))
    if (differ.length === 0) {
        return null
    }
    const kept = new Set([0])
    differ.forEach(index => {
        for (let line = index - CONTEXT_ROWS; line <= index + CONTEXT_ROWS; line++) {
            kept.add(line)
        }
    })
    // A single row between two kept ones is shown rather than left out.
    for (let line = 1; line < count - 1; line++) {
        if (kept.has(line - 1) && kept.has(line + 1)) {
            kept.add(line)
        }
    }
    if (kept.has(count - 2)) {
        kept.add(count - 1)
    }
    const shown: ShownLine[] = []
    let left = 0
    for (let line = 0; line < count; line++) {
        if (kept.has(line)) {
            if (left > 0) {
                shown.push({ gap: left })
                left = 0
            }
            shown.push({ line })
        } else {
            left++
        }
    }
    if (left > 0) {
        shown.push({ gap: left })
    }
    return shown
}

/** The runs of rows of a merge that are drawn next to each other, by their places among the rows drawn. */
const runsOf = (places: number[]): number[][] => places.reduce<number[][]>((runs, place) => {
    const run = runs.at(-1)
    if (run?.at(-1) === place - 1) {
        run.push(place)
    } else {
        runs.push([place])
    }
    return runs
}, [])

/**
 * The table with only the given rows, a gap row where equal rows are left out, and the merges made whole again.
 *
 * A merge spans only the rows that are left, and never a gap: a merge a gap cuts through is drawn on either side of
 * it, its value in each part and its address in the first one. Without that a cell would keep a merge reaching rows that are no longer drawn, and the
 * columns after it would move. A gap is one cell across the table.
 *
 * @param width the number of columns of the table
 */
export const keepRows = (source: RawTableCell[][], shown: readonly ShownLine[], width: number): ShownGrid => {
    const placeOf = new Map<number, number>()
    shown.forEach((line, place) => {
        if ('line' in line) {
            placeOf.set(line.line, place)
        }
    })
    const rows: RawTableCell[][] = shown.map(line => ('line' in line
        ? (source[line.line] ?? []).map(cell => ({ ...cell }))
        : []))
    // The merges are read from the table as it stands and written into the copy. Reading the copy would
    // meet a cell that has just been moved to a row further down and narrow it a second time.
    source.forEach((row, index) => row.forEach((cell, column) => {
        const span = cell.rowspan ?? 1
        if (cell.covered || span <= 1) {
            return
        }
        const places = Array.from({ length: span }, (_, offset) => placeOf.get(index + offset))
            .filter(place => place !== undefined)
        runsOf(places).forEach(([head, ...covered], part) => {
            // A part after a gap repeats the value of the merge, not its address: one address names one cell.
            const { rowspan: _span, cell: address, ...value } = cell
            const shownCell = part === 0 && address !== undefined ? { ...value, cell: address } : value
            rows[head!]![column] = covered.length > 0 ? { ...shownCell, rowspan: covered.length + 1 } : shownCell
            covered.forEach(place => {
                rows[place]![column] = { covered: true }
            })
        })
    }))
    const gaps = new Map<RawTableCell, number>()
    shown.forEach((line, place) => {
        if ('gap' in line) {
            const gap: RawTableCell = width > 1 ? { colspan: width } : {}
            gaps.set(gap, line.gap)
            rows[place] = [gap, ...Array.from({ length: width - 1 }, (): RawTableCell => ({ covered: true }))]
        }
    })
    return { rows, gaps }
}
