import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { CellValueEditor } from './CellValueEditor'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

/**
 * @author Yury Molchan
 */
describe('CellValueEditor', () => {
    const editor = (kind: 'boolean' | 'text') => render(
        <CellValueEditor
            asked={undefined}
            className="field"
            kind={kind}
            onCancel={vi.fn()}
            onChange={vi.fn()}
            onCommit={vi.fn()}
            value="true"
        />
    )

    it('gives the class of a field to a field to write in', () => {
        const { container } = editor('text')

        expect(container.querySelector('.field')).toBe(screen.getByTestId('table-cell-input'))
    })

    it('lets a number cell of a module with constants take the name of one (EPBDS-13456)', () => {
        const onChange = vi.fn()
        render(
            <CellValueEditor
                asked={{ editor: 'numeric', constants: ['Const16', 'Const20'], intOnly: true }}
                kind="numeric"
                onCancel={vi.fn()}
                onChange={onChange}
                onCommit={vi.fn()}
                value=""
            />
        )

        const field = screen.getByTestId('table-cell-input')
        fireEvent.change(field, { target: { value: 'Const16' } })

        expect(onChange).toHaveBeenCalledWith('Const16')
        expect(field).toHaveAttribute('list', screen.getByTestId('table-cell-constants').id)
        expect([...screen.getByTestId('table-cell-constants').querySelectorAll('option')].map(o => o.value))
            .toEqual(['Const16', 'Const20'])
    })

    it('keeps a number cell without constants to numbers', () => {
        render(
            <CellValueEditor
                asked={{ editor: 'numeric', intOnly: true }}
                kind="numeric"
                onCancel={vi.fn()}
                onChange={vi.fn()}
                onCommit={vi.fn()}
                value="5"
            />
        )

        expect(screen.getByRole('spinbutton')).toBe(screen.getByTestId('table-cell-input'))
        expect(screen.queryByTestId('table-cell-constants')).toBeNull()
    })

    it('gives a yes-or-no cell its checkbox alone, with no field around it (EPBDS-17251)', () => {
        const { container } = editor('boolean')

        expect(screen.getByTestId('table-cell-input')).toBeChecked()
        expect(container.querySelector('.field')).toBeNull()
    })
})
