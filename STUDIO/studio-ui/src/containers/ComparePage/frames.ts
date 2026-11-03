import type { RawTableCell } from 'types/tables'

/** The sides of a cell a frame runs along. */
export interface FrameSides {
    top: boolean
    right: boolean
    bottom: boolean
    left: boolean
}

/** The cell drawn at each place of a table, the merged cell over it included. */
const ownersOf = (grid: RawTableCell[][]): Array<Array<RawTableCell | undefined>> => {
    const owners: Array<Array<RawTableCell | undefined>> = grid.map(() => [])
    grid.forEach((row, rowIndex) => row.forEach((cell, columnIndex) => {
        if (cell.covered) {
            return
        }
        for (let down = 0; down < (cell.rowspan ?? 1); down++) {
            const line = (owners[rowIndex + down] ??= [])
            for (let across = 0; across < (cell.colspan ?? 1); across++) {
                line[columnIndex + across] = cell
            }
        }
    }))
    return owners
}

/**
 * The frame around the changed cells of a table, by the changed cell.
 *
 * Changed cells next to each other share one frame. So a changed row, a changed column or a table changed in every
 * cell is framed as a whole. A side of a changed cell is framed unless the cells beyond it changed too. A merged cell
 * is framed along a side unless every cell beyond that side changed.
 *
 * @param grid the cells as they are drawn, a row and a column of every place
 * @param changed whether a cell of the grid, at its row and column, changed
 */
export const framesOf = (
    grid: RawTableCell[][],
    changed: (cell: RawTableCell, row: number, column: number) => boolean
): Map<RawTableCell, FrameSides> => {
    const owners = ownersOf(grid)
    const marked = new Set<RawTableCell>()
    grid.forEach((row, rowIndex) => row.forEach((cell, columnIndex) => {
        if (!cell.covered && changed(cell, rowIndex, columnIndex)) {
            marked.add(cell)
        }
    }))
    const changedAt = (row: number, column: number) => {
        const owner = owners[row]?.[column]
        return owner !== undefined && marked.has(owner)
    }
    /** Whether every place of a run beyond a side changed. */
    const allChanged = (count: number, at: (step: number) => boolean) =>
        Array.from({ length: count }, (_, step) => at(step)).every(Boolean)

    const frames = new Map<RawTableCell, FrameSides>()
    grid.forEach((row, rowIndex) => row.forEach((cell, columnIndex) => {
        if (!marked.has(cell)) {
            return
        }
        const height = cell.rowspan ?? 1
        const width = cell.colspan ?? 1
        frames.set(cell, {
            top: !allChanged(width, step => changedAt(rowIndex - 1, columnIndex + step)),
            bottom: !allChanged(width, step => changedAt(rowIndex + height, columnIndex + step)),
            left: !allChanged(height, step => changedAt(rowIndex + step, columnIndex - 1)),
            right: !allChanged(height, step => changedAt(rowIndex + step, columnIndex + width)),
        })
    }))
    return frames
}
