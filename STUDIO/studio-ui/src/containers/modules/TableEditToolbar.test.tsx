import { act, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { markerSlot } from './OpenCell'
import { TableEditToolbar } from './TableEditToolbar'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

/** The actions that change the table, after the ones that save it and take steps back or forth. */
const CHANGING = [
    'insert_row', 'remove_row', 'insert_column', 'remove_column',
    'merge', 'unmerge',
    'align_left', 'align_center', 'align_right',
    'bold', 'italic', 'underline',
    'fill_colour', 'font_colour',
    'outdent', 'indent',
]

const draw = (over: Partial<Parameters<typeof TableEditToolbar>[0]> = {}) => {
    const acted = {
        onUndo: vi.fn(),
        onRedo: vi.fn(),
        onSave: vi.fn(),
        onCancel: vi.fn(),
        onInsertRow: vi.fn(),
        onRemoveRow: vi.fn(),
        onInsertColumn: vi.fn(),
        onRemoveColumn: vi.fn(),
        onStyle: vi.fn(),
        onPreview: vi.fn(),
        onMerge: vi.fn(),
        onUnmerge: vi.fn(),
    }
    render(
        <TableEditToolbar
            canUndo
            dirty
            formatting
            indenting
            whole
            blocked={null}
            canRedo={false}
            cell={{ value: 'Good Morning' }}
            markers={markerSlot()}
            marking={false}
            mergeBlocked={null}
            picked={{ row: 1, column: 1 }}
            saving={false}
            unmergeable={false}
            {...acted}
            {...over}
        />
    )
    return acted
}

describe('TableEditToolbar', () => {
    it('withholds adding a column while only part of the table is on screen', async () => {
        const acted = draw({ whole: false })

        // An added column carries a value for every row of the table, and only the loaded ones are known.
        const insert = screen.getByTestId('table-edit-insert_column')
        expect(insert).toBeDisabled()
        await userEvent.click(insert)
        expect(acted.onInsertColumn).not.toHaveBeenCalled()
        // Taking one away needs no values, so it stays offered.
        expect(screen.getByTestId('table-edit-remove_column')).toBeEnabled()
    })

    it('carries every action the legacy editor had, in one strip', () => {
        draw()

        const expected = ['save', 'undo', 'redo', ...CHANGING]
        expected.forEach(action => expect(screen.getByTestId(`table-edit-${action}`)).toBeInTheDocument())
    })

    it('offers nothing that acts on a cell until one is picked', () => {
        draw({ picked: null })

        expect(screen.getByTestId('table-edit-bold')).toBeDisabled()
        expect(screen.getByTestId('table-edit-insert_row')).toBeDisabled()
        // Taking an edit back does not need a cell.
        expect(screen.getByTestId('table-edit-undo')).toBeEnabled()
    })

    it('offers on the header only what leaves the corner OpenL finds the table by where it is', () => {
        draw({ picked: { row: 0, column: 0 } })

        // The header is banked across the table, and every column action leaves the corner where it is: the
        // bank widens over a column laid down before the first one and narrows over one taken away.
        expect(screen.getByTestId('table-edit-insert_row')).toBeEnabled()
        expect(screen.getByTestId('table-edit-insert_column')).toBeEnabled()
        expect(screen.getByTestId('table-edit-remove_column')).toBeEnabled()
        // Taking the header's own row away does not: the table would start on a line OpenL reads as no header.
        expect(screen.getByTestId('table-edit-remove_row')).toBeDisabled()
    })

    it('merges the cells picked, and says why it cannot', async () => {
        const acted = draw()

        await userEvent.click(screen.getByTestId('table-edit-merge'))
        expect(acted.onMerge).toHaveBeenCalledTimes(1)
        // A single cell is not merged with anything, and says how to pick more of them.
        expect(screen.getByTestId('table-edit-unmerge')).toBeDisabled()
    })

    it('withholds merging cells that cannot be merged, and takes a merged cell apart', async () => {
        const acted = draw({ mergeBlocked: 'The cells hold different values.', unmergeable: true })

        expect(screen.getByTestId('table-edit-merge')).toBeDisabled()
        await userEvent.click(screen.getByTestId('table-edit-unmerge'))
        expect(acted.onUnmerge).toHaveBeenCalledTimes(1)
    })

    it('offers the look of a cell only where the formatting is kept and seen', () => {
        draw({ formatting: false, indenting: false })

        // A save that formats the table writes over the look, and the Studio look does not show it.
        const look = ['align_left', 'align_center', 'align_right', 'bold', 'italic', 'underline',
            'fill_colour', 'font_colour', 'outdent', 'indent']
        look.forEach(action => expect(screen.queryByTestId(`table-edit-${action}`)).not.toBeInTheDocument())
        // What changes the table itself stays.
        expect(screen.getByTestId('table-edit-insert_row')).toBeInTheDocument()
    })

    it('keeps the indent of a table that reads its structure from it', () => {
        draw({ formatting: false, indenting: true })

        expect(screen.getByTestId('table-edit-indent')).toBeInTheDocument()
        expect(screen.getByTestId('table-edit-outdent')).toBeInTheDocument()
        expect(screen.queryByTestId('table-edit-bold')).not.toBeInTheDocument()
        expect(screen.queryByTestId('table-edit-fill_colour')).not.toBeInTheDocument()
    })

    it('opens the palette of Excel: the theme colours with their shades, and the standard colours', async () => {
        draw()

        await userEvent.click(screen.getByTestId('table-edit-fill_colour'))

        expect(await screen.findByTestId('table-edit-palette')).toBeInTheDocument()
        expect(screen.getAllByTestId('table-edit-swatch')).toHaveLength(70)
    })

    it('shows a colour on the cell while the pointer rests on it, and takes it back off', async () => {
        const acted = draw()

        await userEvent.click(screen.getByTestId('table-edit-fill_colour'))
        const swatches = await screen.findAllByTestId('table-edit-swatch')
        const first = swatches[0] as HTMLElement

        await userEvent.hover(first)
        expect(acted.onPreview).toHaveBeenLastCalledWith({ background: { theme: 'lt1' } })
        // Nothing is written by looking: the colour is on the cell, not in what the table will be saved as.
        expect(acted.onStyle).not.toHaveBeenCalled()

        await userEvent.unhover(first)
        expect(acted.onPreview).toHaveBeenLastCalledWith(null)
    })

    it('fills the cell with the colour the reader settles on', async () => {
        const acted = draw()

        await userEvent.click(screen.getByTestId('table-edit-fill_colour'))
        const swatches = await screen.findAllByTestId('table-edit-swatch')
        await userEvent.click(swatches[1] as HTMLElement)

        expect(acted.onStyle).toHaveBeenCalledWith({ background: { theme: 'dk1' } })
        expect(acted.onPreview).toHaveBeenLastCalledWith(null)
    })

    it('colours the text of the cell with a standard colour', async () => {
        const acted = draw()

        await userEvent.click(screen.getByTestId('table-edit-font_colour'))
        const swatches = await screen.findAllByTestId('table-edit-swatch')
        await userEvent.click(swatches.at(-1) as HTMLElement)

        expect(acted.onStyle).toHaveBeenCalledWith({ color: { rgb: '#7030A0' } })
    })

    it('sets the font of the picked cell', async () => {
        const { onStyle } = draw()

        await userEvent.click(screen.getByTestId('table-edit-bold'))

        expect(onStyle).toHaveBeenCalledWith({ bold: true })
    })

    it('turns off what the cell already carries', async () => {
        const { onStyle } = draw({ cell: { value: 'x', style: { bold: true } } })

        await userEvent.click(screen.getByTestId('table-edit-bold'))

        expect(onStyle).toHaveBeenCalledWith({ bold: false })
    })

    it('indents the picked cell one step at a time', async () => {
        const { onStyle } = draw({ cell: { value: 'x', style: { indent: 2 } } })

        await userEvent.click(screen.getByTestId('table-edit-indent'))
        expect(onStyle).toHaveBeenCalledWith({ indent: 3 })

        await userEvent.click(screen.getByTestId('table-edit-outdent'))
        expect(onStyle).toHaveBeenCalledWith({ indent: 1 })
    })

    it('does not write a table that would be split by a blank line', () => {
        draw({ blocked: 'browser.module.edit_blank_row' })

        expect(screen.getByTestId('table-edit-save')).toBeDisabled()
    })

    it('has nothing to write on a table nobody changed', () => {
        draw({ dirty: false, canUndo: false })

        expect(screen.getByTestId('table-edit-save')).toBeDisabled()
        expect(screen.getByTestId('table-edit-undo')).toBeDisabled()
    })

    it('offers the marks in a workbook that holds them, with nothing to mark until a cell holding a text is open', () => {
        draw({ marking: true })

        for (const key of ['error', 'warning', 'success', 'info', 'clear']) {
            expect(screen.getByTestId(`table-edit-mark_${key}`)).toBeDisabled()
        }
    })

    it('offers no marks in a workbook that cannot hold them', () => {
        const markers = markerSlot()
        markers.set(vi.fn())
        draw({ marking: false, markers })

        expect(screen.queryByTestId('table-edit-mark_error')).not.toBeInTheDocument()
    })

    it('marks the text of the open cell and keeps the focus in the cell', async () => {
        const onMark = vi.fn()
        const markers = markerSlot()
        draw({ marking: true, markers })
        // The open cell hands its marker over after the band of actions is drawn, and the marks come on then.
        act(() => markers.set(onMark))

        const warning = screen.getByTestId('table-edit-mark_warning')
        // A press that took the focus would close the cell and lose the piece of the text the reader selected.
        expect(fireEvent.mouseDown(warning)).toBe(false)
        await userEvent.click(warning)
        await userEvent.click(screen.getByTestId('table-edit-mark_clear'))

        expect(onMark.mock.calls).toEqual([['warning'], [null]])
    })
})
