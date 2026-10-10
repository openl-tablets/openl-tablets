import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { TableEditToolbar } from './TableEditToolbar'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

/** The actions that change the table, after the ones that save it and take steps back or forth. */
const CHANGING = [
    'insert_row', 'remove_row', 'insert_column', 'remove_column',
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
    }
    render(
        <TableEditToolbar
            canUndo
            dirty
            whole
            blocked={null}
            canRedo={false}
            cell={{ value: 'Good Morning' } as RawTableCell}
            picked={{ row: 1, column: 1 }}
            saving={false}
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

    it('opens on the sixty colours of the palette of Excel', async () => {
        draw()

        await userEvent.click(screen.getByTestId('table-edit-fill_colour'))

        expect(await screen.findByTestId('table-edit-palette')).toBeInTheDocument()
        expect(screen.getAllByTestId('table-edit-swatch')).toHaveLength(60)
    })

    it('shows a colour on the cell while the pointer rests on it, and takes it back off', async () => {
        const acted = draw()

        await userEvent.click(screen.getByTestId('table-edit-fill_colour'))
        const swatches = await screen.findAllByTestId('table-edit-swatch')
        const first = swatches[0] as HTMLElement

        await userEvent.hover(first)
        expect(acted.onPreview).toHaveBeenLastCalledWith({ backgroundTheme: { name: 'lt1' } })
        // Nothing is written by looking: the colour is on the cell, not in what the table will be saved as.
        expect(acted.onStyle).not.toHaveBeenCalled()

        await userEvent.unhover(first)
        expect(acted.onPreview).toHaveBeenLastCalledWith(null)
    })

    it('fills the cell with the colour the reader settles on', async () => {
        const acted = draw()

        await userEvent.click(screen.getByTestId('table-edit-fill_colour'))
        const swatches = await screen.findAllByTestId('table-edit-swatch')
        await userEvent.click(swatches[14] as HTMLElement)

        expect(acted.onStyle).toHaveBeenCalledWith({ backgroundTheme: { name: 'accent1', tint: 0.8 } })
        expect(acted.onPreview).toHaveBeenLastCalledWith(null)
    })

    it('colours the text of the cell with the colour the reader settles on', async () => {
        const acted = draw()

        await userEvent.click(screen.getByTestId('table-edit-font_colour'))
        const swatches = await screen.findAllByTestId('table-edit-swatch')
        await userEvent.click(swatches[1] as HTMLElement)

        expect(acted.onStyle).toHaveBeenCalledWith({ colorTheme: { name: 'dk1' } })
    })

    it('sets the font of the picked cell', async () => {
        const { onStyle } = draw()

        await userEvent.click(screen.getByTestId('table-edit-bold'))

        expect(onStyle).toHaveBeenCalledWith({ bold: true })
    })

    it('turns off what the cell already carries', async () => {
        const { onStyle } = draw({ cell: { value: 'x', style: { bold: true } } as RawTableCell })

        await userEvent.click(screen.getByTestId('table-edit-bold'))

        expect(onStyle).toHaveBeenCalledWith({ bold: false })
    })

    it('indents the picked cell one step at a time', async () => {
        const { onStyle } = draw({ cell: { value: 'x', style: { indent: 2 } } as RawTableCell })

        await userEvent.click(screen.getByTestId('table-edit-indent'))
        expect(onStyle).toHaveBeenCalledWith({ indent: 3 })

        await userEvent.click(screen.getByTestId('table-edit-outdent'))
        expect(onStyle).toHaveBeenCalledWith({ indent: 1 })
    })

    it('turns off the formatting of a cell while the table is shown in the theme, and says why', async () => {
        const { onStyle } = draw({ formattable: false, cell: { value: 'x', style: { indent: 2 } } as RawTableCell })

        const formatting = ['align_left', 'align_center', 'align_right', 'bold', 'italic', 'underline',
            'fill_colour', 'font_colour']
        formatting.forEach(action => expect(screen.getByTestId(`table-edit-${action}`)).toBeDisabled())
        await userEvent.click(screen.getByTestId('table-edit-bold'))
        expect(onStyle).not.toHaveBeenCalled()
        // The indent sets out the steps of a TBasic algorithm, so it is changed in either look.
        await userEvent.click(screen.getByTestId('table-edit-indent'))
        expect(onStyle).toHaveBeenCalledWith({ indent: 3 })
        expect(screen.getByTestId('table-edit-outdent')).toBeEnabled()

        await userEvent.hover(screen.getByTestId('table-edit-bold'))
        expect(await screen.findByText('browser.module.edit_format_with_excel')).toBeInTheDocument()
    })

    it('says why a colour cannot be set while the table is shown in the theme', async () => {
        draw({ formattable: false })

        await userEvent.hover(screen.getByTestId('table-edit-font_colour'))

        expect(await screen.findByText('browser.module.edit_format_with_excel')).toBeInTheDocument()
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
})
