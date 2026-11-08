import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { MessageRowWarning } from '../../types/localization'
import { languagesOf, type KeyRow } from './bundle'
import { MessageCell } from './MessageCell'
import { useMessageEdits, type MessageEdits } from './useMessageEdits'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

const [, french] = languagesOf([{ tag: '' }, { tag: 'fr' }], tag => tag)

/** The French text of the key, with what the check warned about it, and the edits the cell made. */
const FrenchCell = ({ warnings, seen, shown = true }: {
    warnings: MessageRowWarning[]
    seen: (edits: MessageEdits) => void
    /** Whether the cell is drawn: a virtual table takes it away when its row scrolls out of sight. */
    shown?: boolean
}) => {
    const edits = useMessageEdits()
    seen(edits)
    const row: KeyRow = {
        key: 'total',
        base: { '': 'The total is {0}', fr: 'Le total est {0，number}' },
        shared: {},
        warnings,
    }
    return shown && (
        <MessageCell
            canWrite
            edits={edits}
            language={french!}
            nameOf={tag => tag}
            row={row}
        />
    )
}

const FULL_WIDTH: MessageRowWarning = {
    language: 'fr',
    summary: "Full-width '，' instead of ',' in a placeholder.",
    spots: [{ start: 15, end: 16 }],
}

describe('MessageCell', () => {
    it('lists the warnings under the text being written, each with its piece marked, and keeps the text open', async () => {
        render(<FrenchCell seen={vi.fn()} warnings={[FULL_WIDTH]} />)

        await userEvent.click(screen.getByTestId('localization-cell'))
        const field = screen.getByTestId('localization-cell-input')
        expect(field).toHaveFocus()

        expect(screen.getByTestId('localization-cell-problems')).toHaveTextContent(FULL_WIDTH.summary)
        expect(screen.getByTestId('localization-cell-problem-code')).toHaveTextContent('…total est {0，number}')
        expect(screen.getByTestId('localization-cell-problem-code-marked')).toHaveTextContent('，')

        // Reading the warnings leaves the text being written.
        await userEvent.click(screen.getByTestId('localization-cell-problem-code'))
        expect(field).toHaveFocus()
    })

    it('writes a text as a step of its own', async () => {
        const seen = vi.fn<(edits: MessageEdits) => void>()
        render(<FrenchCell seen={seen} warnings={[]} />)

        await userEvent.click(screen.getByTestId('localization-cell'))
        await userEvent.clear(screen.getByTestId('localization-cell-input'))
        await userEvent.type(screen.getByTestId('localization-cell-input'), 'Somme{Enter}')

        expect(seen.mock.lastCall?.[0].actions).toEqual([{ operation: 'setValue', key: 'total', language: 'fr', value: 'Somme' }])
    })

    it('gives the own text up for the inherited one from beside the field', async () => {
        const seen = vi.fn<(edits: MessageEdits) => void>()
        render(<FrenchCell seen={seen} warnings={[]} />)

        await userEvent.click(screen.getByTestId('localization-cell'))
        await userEvent.click(screen.getByRole('button', { name: 'use_inherited' }))

        expect(seen.mock.lastCall?.[0].actions).toEqual([{ operation: 'removeValue', key: 'total', language: 'fr' }])
        expect(screen.queryByTestId('localization-cell-input')).toBeNull()
        // The language shows the text it falls back to now.
        expect(screen.getByTestId('localization-cell')).toHaveTextContent('The total is {0}')
    })

    it('opens the text from the keyboard, as a button', async () => {
        render(<FrenchCell seen={vi.fn()} warnings={[]} />)
        const cell = screen.getByTestId('localization-cell')

        expect(cell).toHaveAttribute('role', 'button')
        cell.focus()
        await userEvent.keyboard('{Enter}')

        expect(screen.getByTestId('localization-cell-input')).toHaveFocus()
    })

    it('puts the caret after the text, where the reader goes on writing it', async () => {
        render(<FrenchCell seen={vi.fn()} warnings={[]} />)
        screen.getByTestId('localization-cell').focus()
        await userEvent.keyboard('{Enter}')
        await userEvent.keyboard('!')

        expect(screen.getByTestId('localization-cell-input')).toHaveValue('Le total est {0，number}!')
    })

    it('keeps nothing on the Enter that ends the composition of a character', () => {
        const seen = vi.fn()
        render(<FrenchCell seen={seen} warnings={[]} />)
        fireEvent.click(screen.getByTestId('localization-cell'))
        const field = screen.getByTestId('localization-cell-input')
        fireEvent.change(field, { target: { value: 'Somme' } })

        fireEvent.keyDown(field, { key: 'Enter', isComposing: true })
        expect(screen.getByTestId('localization-cell-input')).toBeInTheDocument()

        fireEvent.keyDown(field, { key: 'Enter' })
        expect(seen.mock.lastCall?.[0].actions).toEqual([{ operation: 'setValue', key: 'total', language: 'fr', value: 'Somme' }])
    })

    it('keeps the text being written when the cell is taken away', () => {
        const seen = vi.fn()
        const { rerender } = render(<FrenchCell seen={seen} warnings={[]} />)
        fireEvent.click(screen.getByTestId('localization-cell'))
        fireEvent.change(screen.getByTestId('localization-cell-input'), { target: { value: 'Somme' } })

        rerender(<FrenchCell seen={seen} shown={false} warnings={[]} />)

        expect(seen.mock.lastCall?.[0].actions).toEqual([{ operation: 'setValue', key: 'total', language: 'fr', value: 'Somme' }])
    })
})
