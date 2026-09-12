import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { ProjectStatusDetailedMessage } from '../../services/projectStatus'
import { TableProblems } from './TableProblems'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

const message = (id: number, severity: 'ERROR' | 'WARN', summary: string): ProjectStatusDetailedMessage => ({
    id,
    severity,
    summary,
    stacktrace: false,
})

describe('TableProblems', () => {
    beforeEach(() => localStorage.clear())

    it('says nothing about a table the compiler said nothing about', () => {
        const { container } = render(<TableProblems messages={[]} />)

        expect(container).toBeEmptyDOMElement()
    })

    it('counts what the table raised, and lists it by severity', () => {
        const raised = [
            message(1, 'ERROR', 'Identifier is not found'),
            message(2, 'WARN', 'The rule is never reached'),
            message(3, 'WARN', 'Ambiguous field'),
        ]

        render(<TableProblems messages={raised} />)

        expect(screen.getByTestId('table-problems-errors')).toHaveTextContent('1')
        expect(screen.getByTestId('table-problems-warnings')).toHaveTextContent('2')
        expect(screen.getByTestId('table-message-1')).toHaveTextContent('Identifier is not found')
        expect(screen.getByTestId('table-message-3')).toHaveTextContent('Ambiguous field')
    })

    it('folds away, and stays folded for the next table', async () => {
        const { unmount } = render(<TableProblems messages={[message(1, 'ERROR', 'Identifier is not found')]} />)
        expect(screen.getByTestId('table-problems-body')).toBeInTheDocument()

        await userEvent.click(screen.getByTestId('table-problems-toggle'))

        expect(screen.queryByTestId('table-problems-body')).toBeNull()
        unmount()

        // The next table opens the way the reader left the section.
        render(<TableProblems messages={[message(2, 'WARN', 'Ambiguous field')]} />)
        expect(screen.queryByTestId('table-problems-body')).toBeNull()
    })

    it('keeps the page the reader opened when the screen redraws around it', async () => {
        const raised = Array.from({ length: 12 }, (_, index) => message(index + 1, 'WARN', `Warning ${index + 1}`))
        const { rerender } = render(<TableProblems messages={raised} />)

        await userEvent.click(screen.getByRole('button', { name: 'browser.compile.show_more' }))
        expect(screen.getByTestId('table-message-12')).toBeInTheDocument()

        // The editor redraws on every status the compilation pushes; the list must not fall back to page one.
        rerender(<TableProblems messages={raised} />)

        expect(screen.getByTestId('table-message-12')).toBeInTheDocument()
    })
})
