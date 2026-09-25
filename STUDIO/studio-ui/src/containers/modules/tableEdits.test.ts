import { describe, expect, it } from 'vitest'
import type { RawTableCell } from 'types/tables'
import {
    asRead,
    blankLine,
    compile,
    type EditStep,
    keyOf,
    NO_EDITS,
    redo,
    replay,
    undo,
    withStep,
} from './tableEdits'

const write = (row: number, column: number, value: string): EditStep =>
    ({ kind: 'value', at: { row, column }, value })

const table: RawTableCell[][] = [
    [{ cell: 'A1', value: 'Rules' }, { cell: 'B1', value: 'Greeting' }],
    [{ cell: 'A2', value: 0 }, { cell: 'B2', value: 'Good Morning' }],
    [{ cell: 'A3', value: 12 }, { cell: 'B3', value: 'Good Afternoon' }],
]

/** The table as the given steps leave it. */
const after = (...steps: EditStep[]) => replay(table, steps)

/** What the given steps ask the table to be written with. */
const sent = (...steps: EditStep[]) => compile(table, after(...steps))

/**
 * A rules table with a merged group in it, the way an author writes one: the header is banked across the
 * table and `Young Driver` is one cell over the two rules it names.
 */
const grouped: RawTableCell[][] = [
    [{ cell: 'B4', value: 'Rules', colspan: 3 }, { covered: true }, { covered: true }],
    [{ cell: 'B5', value: 'R1' }, { cell: 'C5', value: 'Young Driver', rowspan: 2 }, { cell: 'D5', value: 'Married' }],
    [{ cell: 'B6', value: 'R2' }, { covered: true }, { cell: 'D6', value: 'Single' }],
]

/** The grouped table as the given steps leave it. */
const group = (...steps: EditStep[]) => replay(grouped, steps)

describe('tableEdits', () => {
    describe('what the reader did', () => {
        it('keeps the steps in the order they were made', () => {
            const buffer = withStep(withStep(NO_EDITS, write(1, 0, '6')), write(1, 1, 'Buenos Dias'))

            expect(buffer.steps).toHaveLength(2)
        })

        it('replaces the last step when the same cell is written again', () => {
            const buffer = withStep(withStep(NO_EDITS, write(1, 1, 'Buenos')), write(1, 1, 'Buenos Dias'))

            // The reader means the value they ended with, so the table is asked to write that cell once.
            expect(buffer.steps).toHaveLength(1)
        })

        it('takes a step back and puts it again', () => {
            const written = withStep(withStep(NO_EDITS, write(1, 0, '6')), write(1, 1, 'Buenos Dias'))

            const back = undo(written)
            expect(back.steps).toHaveLength(1)
            expect(redo(back).steps).toHaveLength(2)
        })

        it('has nothing to take back or put again on an untouched table', () => {
            expect(undo(NO_EDITS)).toBe(NO_EDITS)
            expect(redo(NO_EDITS)).toBe(NO_EDITS)
        })

        it('drops what was undone as soon as something else is done', () => {
            const back = undo(withStep(NO_EDITS, write(1, 0, '6')))

            expect(withStep(back, write(1, 1, 'Buenos Dias')).undone).toHaveLength(0)
        })
    })

    describe('the table as the steps leave it', () => {
        it('shows what was written and says which cells were touched', () => {
            const state = after(write(1, 1, 'Buenos Dias'))

            expect(state.rows[1]?.[1]?.value).toBe('Buenos Dias')
            // The cell keeps everything else it was read with, so the screen still draws it in its place.
            expect(state.rows[1]?.[1]?.cell).toBe('B2')
            expect(state.touched.has(keyOf(state, { row: 1, column: 1 }))).toBe(true)
        })

        it('leaves the table that was read as it stands', () => {
            after(write(1, 1, 'Buenos Dias'))

            expect(table[1]?.[1]?.value).toBe('Good Morning')
        })

        it('forgets the formula of a cell that is written over', () => {
            const withFormula: RawTableCell[][] = [[{ cell: 'A1', value: 4, formula: '=2+2' }]]

            expect(replay(withFormula, [write(0, 0, '5')]).rows[0]?.[0]).toEqual({ cell: 'A1', value: '5' })
        })

        it('pushes the rows below down when one is added', () => {
            const state = after({ kind: 'insertRow', at: 1 })

            expect(state.rows).toHaveLength(4)
            expect(state.rows[1]?.[0]?.value).toBe('')
            expect(state.rows[2]?.[0]?.value).toBe(0)
            // The added row is not one of the table's own, which is what tells the save to add it.
            expect(state.rowIds[1]?.startsWith('n')).toBe(true)
        })

        it('writes into the row that was added, not the one it pushed down', () => {
            const state = after({ kind: 'insertRow', at: 1 }, write(1, 0, '6'))

            expect(state.rows[1]?.[0]?.value).toBe('6')
            expect(state.rows[2]?.[0]?.value).toBe(0)
        })

        it('gives an added row the styling of the row above it, the way the workbook writes it', () => {
            const state = after(
                { kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } },
                { kind: 'insertRow', at: 2 }
            )

            expect(state.rows[2]?.[1]?.style?.bold).toBe(true)
            expect(state.rows[2]?.[1]?.value).toBe('')
        })

        it('gives an added column the styling of the column it pushes aside', () => {
            const state = after(
                { kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } },
                { kind: 'insertColumn', at: 1 }
            )

            expect(state.rows[1]?.[1]?.style?.bold).toBe(true)
            expect(state.rows[1]?.[1]?.value).toBe('')
            // the column that was styled is the one that moved aside
            expect(state.rows[1]?.[2]?.value).toBe('Good Morning')
        })

        it('takes away every row a merged cell reaches over', () => {
            const state = after({ kind: 'removeRow', at: 1, lines: 2 })

            expect(state.rows).toHaveLength(1)
            expect(state.rows[0]?.[0]?.value).toBe('Rules')
        })

        it('takes away every column a merged cell reaches over', () => {
            const state = after({ kind: 'removeColumn', at: 0, lines: 2 })

            expect(state.rows[0]).toHaveLength(0)
        })

        it('keeps the styling the reader asked for on the cell it was asked for', () => {
            const state = after({ kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } })

            expect(state.rows[1]?.[1]?.style?.bold).toBe(true)
        })

        it('carries the styling with the cell when a row is added above it', () => {
            const state = after(
                { kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } },
                { kind: 'insertRow', at: 1 }
            )

            expect(state.rows[2]?.[1]?.style?.bold).toBe(true)
            expect(state.rows[1]?.[1]?.style?.bold).toBeUndefined()
        })
    })

    describe('where a cell stood in the table that was read', () => {
        it('answers where a cell stood before a row moved it', () => {
            const state = after({ kind: 'insertRow', at: 1 })

            expect(asRead(state, { row: 2, column: 1 })).toEqual({ row: 1, column: 1 })
        })

        it('answers where a cell stood before a column moved it', () => {
            const state = after({ kind: 'insertColumn', at: 0 })

            expect(asRead(state, { row: 1, column: 1 })).toEqual({ row: 1, column: 0 })
        })

        it('answers nothing for a cell of a line the reader added', () => {
            const state = after({ kind: 'insertRow', at: 1 })

            // Nothing was read about it, so nothing is known: what it holds is written as plain text.
            expect(asRead(state, { row: 1, column: 0 })).toBeNull()
        })
    })

    describe('what is sent when the reader saves', () => {
        it('sends nothing at all for a table nobody changed', () => {
            expect(sent()).toEqual([])
        })

        it('sends nothing for a cell written back to what it held', () => {
            expect(sent(write(1, 1, 'Buenos Dias'), write(1, 1, 'Good Morning'))).toEqual([])
        })

        it('sends the cells that differ, addressed as they stand now', () => {
            expect(sent(write(1, 1, 'Buenos Dias'))).toEqual([
                { operation: 'update', target: { type: 'cell', row: 1, column: 1, value: 'Buenos Dias' } },
            ])
        })

        it('sends an added row with the values it ended with', () => {
            expect(sent({ kind: 'insertRow', at: 1 }, write(1, 0, '6'), write(1, 1, 'Buenos Dias'))).toEqual([
                {
                    operation: 'insert',
                    target: {
                        type: 'rows',
                        position: 1,
                        cells: [[{ value: '6' }, { value: 'Buenos Dias' }]],
                    },
                },
            ])
        })

        it('takes rows away from the bottom up, so the ones before them do not move', () => {
            expect(sent({ kind: 'removeRow', at: 1, lines: 1 }, { kind: 'removeRow', at: 1, lines: 1 })).toEqual([
                { operation: 'delete', target: { type: 'rows', position: 2, count: 1 } },
                { operation: 'delete', target: { type: 'rows', position: 1, count: 1 } },
            ])
        })

        it('asks for every row of a merged block to be taken away, the bottom one first', () => {
            expect(sent({ kind: 'removeRow', at: 1, lines: 2 })).toEqual([
                { operation: 'delete', target: { type: 'rows', position: 2, count: 1 } },
                { operation: 'delete', target: { type: 'rows', position: 1, count: 1 } },
            ])
        })

        it('takes a column away before it addresses any row', () => {
            const edits = sent({ kind: 'removeColumn', at: 1, lines: 1 }, write(1, 0, '6'))

            expect(edits[0]).toEqual({ operation: 'delete', target: { type: 'columns', position: 1, count: 1 } })
            expect(edits[1]).toEqual({
                operation: 'update',
                target: { type: 'cell', row: 1, column: 0, value: '6' },
            })
        })

        it('sends an added column holding only the rows the table already had', () => {
            const edits = sent({ kind: 'insertColumn', at: 1 }, write(0, 1, 'Note'))

            expect(edits).toEqual([{
                operation: 'insert',
                target: {
                    type: 'columns',
                    position: 1,
                    cells: [[{ value: 'Note' }, { value: '' }, { value: '' }]],
                },
            }])
        })

        it('takes a row away before it adds a column, so the column is as tall as the table', () => {
            const edits = sent({ kind: 'removeRow', at: 2, lines: 1 }, { kind: 'insertColumn', at: 1 }, write(0, 1, 'Note'))

            // The table takes a column that carries a cell per row it has at that moment: the row that went is
            // already gone, so the column is one shorter than the table was read as.
            expect(edits).toEqual([
                { operation: 'delete', target: { type: 'rows', position: 2, count: 1 } },
                {
                    operation: 'insert',
                    target: {
                        type: 'columns',
                        position: 1,
                        cells: [[{ value: 'Note' }, { value: '' }]],
                    },
                },
            ])
        })

        it('adds a row once the columns it carries are there', () => {
            const edits = sent({ kind: 'insertColumn', at: 1 }, write(0, 1, 'Note'),
                { kind: 'insertRow', at: 1 }, write(1, 0, 'new'))

            // The added row carries a cell for the added column too, so it goes in after that column.
            expect(edits[0]?.operation).toBe('insert')
            expect((edits[0] as { target: { type: string } }).target.type).toBe('columns')
            expect(edits[1]).toEqual({
                operation: 'insert',
                target: {
                    type: 'rows',
                    position: 1,
                    cells: [[{ value: 'new' }, { value: '' }, { value: '' }]],
                },
            })
        })

        it('sends the styling the reader asked for, and only that', () => {
            expect(sent({ kind: 'style', at: { row: 1, column: 1 }, style: { bold: true } })).toEqual([
                {
                    operation: 'style',
                    target: { type: 'cells', row: 1, column: 1, rowspan: 1, colspan: 1, style: { bold: true } },
                },
            ])
        })
    })

    describe('the merges a line laid down or taken away leaves behind', () => {
        it('grows the group over a row laid down inside it, and leaves that row no cell of its own there', () => {
            const state = group({ kind: 'insertRow', at: 2 })

            // The workbook grows the merge over the new row, so the row has cells only where the group
            // leaves it room — and they line up with their own columns rather than being pushed along.
            expect(state.rows[1]?.[1]?.rowspan).toBe(3)
            expect(state.rows[2]?.[1]?.covered).toBe(true)
            expect(state.rows[2]?.[0]?.value).toBe('')
            expect(state.rows[2]?.[2]?.value).toBe('')
        })

        it('grows the group over a row laid down under the last of its rules', () => {
            const state = group({ kind: 'insertRow', at: 3 })

            expect(state.rows[1]?.[1]?.rowspan).toBe(3)
            expect(state.rows[3]?.[1]?.covered).toBe(true)
        })

        it('pushes a group down when the row is laid down above it, and banks that row as the header is', () => {
            const state = group({ kind: 'insertRow', at: 1 })

            // A line takes the look of the one it is written from, its merges among them. Drawn as three
            // free cells the new row would take three values and keep one, the rest falling under the bank.
            expect(state.rows[1]?.[0]?.colspan).toBe(3)
            expect(state.rows[1]?.[1]?.covered).toBe(true)
            // The group itself is untouched by a row laid down above it; it only moves down.
            expect(state.rows[2]?.[1]?.rowspan).toBe(2)
            expect(state.rows[3]?.[1]?.covered).toBe(true)
        })

        it('shrinks the group to nothing when one of its two rules is taken away', () => {
            const state = group({ kind: 'removeRow', at: 2, lines: 1 })

            expect(state.rows[1]?.[1]?.rowspan).toBeUndefined()
            expect(state.rows[1]?.[1]?.value).toBe('Young Driver')
        })

        it('takes the group with the rows it stands over', () => {
            const state = group({ kind: 'removeRow', at: 1, lines: 2 })

            expect(state.rows).toHaveLength(1)
            expect(state.rows[0]?.[0]?.colspan).toBe(3)
        })

        it('grows the header over a column laid down under it', () => {
            const state = group({ kind: 'insertColumn', at: 1 })

            expect(state.rows[0]?.[0]?.colspan).toBe(4)
            expect(state.rows[0]?.[1]?.covered).toBe(true)
            expect(state.rows[1]?.[1]?.value).toBe('')
        })

        it('leaves a merged cell where it began when the column is laid down at it', () => {
            const state = group({ kind: 'insertColumn', at: 0 })

            // The header keeps the corner OpenL finds the table by, and grows over the new column instead.
            expect(state.rows[0]?.[0]?.value).toBe('Rules')
            expect(state.rows[0]?.[0]?.colspan).toBe(4)
            expect(state.rows[1]?.[0]?.value).toBe('')
            expect(state.rows[1]?.[1]?.value).toBe('R1')
        })

        it('keeps the header on screen when the column it is banked from is taken away', () => {
            const state = group({ kind: 'removeColumn', at: 0, lines: 1 })

            // The header holds its text in the cell it starts at. Taking that cell away without carrying the
            // text along leaves the bank standing over cells that hold nothing, and the table reads headless.
            expect(state.rows[0]?.[0]?.value).toBe('Rules')
            expect(state.rows[0]?.[0]?.colspan).toBe(2)
        })

        it('keeps a group on screen when the row it is named on is taken away', () => {
            const state = group({ kind: 'removeRow', at: 1, lines: 1 })

            expect(state.rows[1]?.[1]?.value).toBe('Young Driver')
            expect(state.rows[1]?.[1]?.rowspan).toBeUndefined()
        })

        it('narrows the header when a column is taken away from under it', () => {
            const state = group({ kind: 'removeColumn', at: 2, lines: 1 })

            expect(state.rows[0]?.[0]?.colspan).toBe(2)
            expect(state.rows[1]).toHaveLength(2)
        })

        it('sends a row laid down in a group with its values under their own columns', () => {
            const steps: EditStep[] = [
                { kind: 'insertRow', at: 2 },
                { kind: 'value', at: { row: 2, column: 0 }, value: 'R1b' },
                { kind: 'value', at: { row: 2, column: 2 }, value: 'Widowed' },
            ]

            // The column the group covers carries nothing and says so: a blank written there would be
            // dropped without a word, and the values after it would each land one column too far left.
            expect(compile(grouped, group(...steps))).toEqual([{
                operation: 'insert',
                target: {
                    type: 'rows',
                    position: 2,
                    cells: [[{ value: 'R1b' }, { value: '', covered: true }, { value: 'Widowed' }]],
                },
            }])
        })

        it('writes nothing to a cell the group has grown over', () => {
            const steps: EditStep[] = [
                { kind: 'insertColumn', at: 0 },
                { kind: 'value', at: { row: 1, column: 0 }, value: 'first' },
            ]

            // The header has grown over the new column, so the cell under it is not the reader's to write.
            expect(compile(grouped, group(...steps)).some(edit => edit.operation === 'update')).toBe(false)
        })
    })

    describe('what the table cannot take', () => {
        it('refuses a row the reader added and left empty', () => {
            expect(blankLine(after({ kind: 'insertRow', at: 1 }))).toBe('row')
        })

        it('refuses a column the reader added and left empty', () => {
            expect(blankLine(after({ kind: 'insertColumn', at: 1 }))).toBe('column')
        })

        it('takes an added row once something is written into it', () => {
            expect(blankLine(after({ kind: 'insertRow', at: 1 }, write(1, 0, '6')))).toBeNull()
        })

        it('says nothing about a row the table was read with, however empty', () => {
            const empty: RawTableCell[][] = [[{ value: 'Rules' }], [{ value: '' }]]

            expect(blankLine(replay(empty, []))).toBeNull()
        })
    })
})
