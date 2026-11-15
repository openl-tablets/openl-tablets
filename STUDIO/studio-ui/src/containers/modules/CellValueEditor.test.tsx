import { render, screen } from '@testing-library/react'
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

    it('gives a yes-or-no cell its checkbox alone, with no field around it (EPBDS-17251)', () => {
        const { container } = editor('boolean')

        expect(screen.getByTestId('table-cell-input')).toBeChecked()
        expect(container.querySelector('.field')).toBeNull()
    })
})
