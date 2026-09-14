import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { RawTableCell } from 'types/tables'
import { TableEditToolbar } from './TableEditToolbar'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

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
    }
    render(
        <TableEditToolbar
            canUndo
            dirty
            blocked={null}
            canRedo={false}
            cell={{ value: 'Good Morning' } as RawTableCell}
            height={4}
            picked={{ row: 1, column: 1 }}
            saving={false}
            width={3}
            {...acted}
            {...over}
        />
    )
    return acted
}

describe('TableEditToolbar', () => {
    it('carries every action the legacy editor had, in one strip', () => {
        draw()

        const expected = [
            'save', 'undo', 'redo',
            'insert_row', 'remove_row', 'insert_column', 'remove_column',
            'align_left', 'align_center', 'align_right',
            'bold', 'italic', 'underline',
            'fill_colour', 'font_colour',
            'outdent', 'indent',
        ]
        expected.forEach(action => expect(screen.getByTestId(`table-edit-${action}`)).toBeInTheDocument())
    })

    it('offers nothing that acts on a cell until one is picked', () => {
        draw({ picked: null })

        expect(screen.getByTestId('table-edit-bold')).toBeDisabled()
        expect(screen.getByTestId('table-edit-insert_row')).toBeDisabled()
        // Taking an edit back does not need a cell.
        expect(screen.getByTestId('table-edit-undo')).toBeEnabled()
    })

    it('leaves the table header where it is', () => {
        draw({ picked: { row: 0, column: 0 } })

        expect(screen.getByTestId('table-edit-insert_row')).toBeDisabled()
        expect(screen.getByTestId('table-edit-remove_row')).toBeDisabled()
        expect(screen.getByTestId('table-edit-insert_column')).toBeDisabled()
        expect(screen.getByTestId('table-edit-remove_column')).toBeDisabled()
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
