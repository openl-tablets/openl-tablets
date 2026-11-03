import { holdersOf } from 'components/rawTableStyle'
import type { RawTableCell } from 'types/tables'

/** The sides of a cell a frame runs along. */
export interface FrameSides {
    top: boolean
    right: boolean
    bottom: boolean
    left: boolean
}

/** The cell drawn at each place of a table, the merged cell over it included. */
const ownersOf = (grid: RawTableCell[][]): Array<Array<RawTableCell | undefined>> =>
    holdersOf(grid).map(row => row.map(place => place && grid[place[0]]?.[place[1]]))

/** Whether every step of a run beyond a side of a cell holds. */
const everyStep = (count: number, holds: (step: number) => boolean): boolean => {
    for (let step = 0; step < count; step++) {
        if (!holds(step)) {
            return false
        }
    }
    return true
}

/**
 * The frames around the framed cells of a table, by the framed cell.
 *
 * Cells framed the same way next to each other share one frame. So a framed row, a framed column or a table framed in
 * every cell is framed as a whole. A side of a framed cell is framed unless the cells beyond it are framed the same
 * way. A merged cell is framed along a side unless every cell beyond that side is framed the same way.
 *
 * @param grid the cells as they are drawn, a row and a column of every place
 * @param frameOf how a cell of the grid, at its row and column, is framed, or false or undefined for a cell with no
 * frame
 */
export const framesOf = <K, >(
    grid: RawTableCell[][],
    frameOf: (cell: RawTableCell, row: number, column: number) => K | false | undefined
): Map<RawTableCell, FrameSides> => {
    const owners = ownersOf(grid)
    const kinds = new Map<RawTableCell, K>()
    grid.forEach((row, rowIndex) => row.forEach((cell, columnIndex) => {
        const kind = cell.covered ? undefined : frameOf(cell, rowIndex, columnIndex)
        if (kind !== undefined && kind !== false) {
            kinds.set(cell, kind)
        }
    }))
    const frames = new Map<RawTableCell, FrameSides>()
    grid.forEach((row, rowIndex) => row.forEach((cell, columnIndex) => {
        const kind = kinds.get(cell)
        if (kind === undefined) {
            return
        }
        const sameAt = (place: number, column: number) => {
            const owner = owners[place]?.[column]
            return owner !== undefined && kinds.get(owner) === kind
        }
        const height = cell.rowspan ?? 1
        const width = cell.colspan ?? 1
        frames.set(cell, {
            top: !everyStep(width, step => sameAt(rowIndex - 1, columnIndex + step)),
            bottom: !everyStep(width, step => sameAt(rowIndex + height, columnIndex + step)),
            left: !everyStep(height, step => sameAt(rowIndex + step, columnIndex - 1)),
            right: !everyStep(height, step => sameAt(rowIndex + step, columnIndex + width)),
        })
    }))
    return frames
}
