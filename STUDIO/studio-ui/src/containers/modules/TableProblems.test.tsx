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

    it('says why a table of several partial tables cannot be edited here', () => {
        render(<TableProblems messages={[]} partial />)

        // Said even where the compiler raised nothing: the reader is otherwise left wondering why the table
        // cannot be written.
        expect(screen.getByTestId('table-problems-partial')).toHaveTextContent('browser.module.partial_table')
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

    it('offers the way into the cell a message was raised against', async () => {
        const onEditCell = vi.fn()
        const raised: ProjectStatusDetailedMessage[] = [
            { ...message(1, 'ERROR', 'Identifier is not found'), location: { type: 'table', cell: 'D9' } },
            message(2, 'ERROR', 'Cannot parse the module'),
        ]
        render(<TableProblems messages={raised} onEditCell={onEditCell} />)

        await userEvent.click(screen.getByTestId('table-message-1-edit'))

        expect(onEditCell).toHaveBeenCalledWith('D9')
        // A message that names no cell has none to open.
        expect(screen.queryByTestId('table-message-2-edit')).toBeNull()
    })

    it('offers no way in to a reader who may not write the table', () => {
        const raised: ProjectStatusDetailedMessage[] = [
            { ...message(1, 'ERROR', 'Identifier is not found'), location: { type: 'table', cell: 'D9' } },
        ]
        render(<TableProblems messages={raised} />)

        expect(screen.queryByTestId('table-message-1-edit')).toBeNull()
    })

    it('marks the piece of the cell a message is about', () => {
        const raised: ProjectStatusDetailedMessage[] = [
            {
                ...message(1, 'ERROR', 'Identifier bonuss is not found'),
                location: { type: 'table', cell: 'D9', start: 11, end: 17 },
            },
            { ...message(2, 'ERROR', 'Cannot parse the module'), location: { type: 'table', cell: 'D9' } },
        ]

        // The rule is what the cell says, and the screen holds the cell: the message names only the piece.
        render(<TableProblems cellText={() => '=Premium * bonuss / 100'} messages={raised} />)

        expect(screen.getByTestId('table-message-1-code')).toHaveTextContent('=Premium * bonuss / 100')
        expect(screen.getByTestId('table-message-1-code-marked')).toHaveTextContent('bonuss')
        // A message about no part of the cell in particular marks none of it.
        expect(screen.queryByTestId('table-message-2-code')).toBeNull()
    })

    it('shows no rule where the screen does not hold the cell', () => {
        const raised: ProjectStatusDetailedMessage[] = [{
            ...message(1, 'ERROR', 'Identifier bonuss is not found'),
            location: { type: 'table', cell: 'D9', start: 11, end: 17 },
        }]

        render(<TableProblems messages={raised} />)

        expect(screen.queryByTestId('table-message-1-code')).toBeNull()
    })

    it('reads the stack trace behind a message only when the reader opens it', async () => {
        const onStacktrace = vi.fn().mockResolvedValue('org.openl.OpenLRuntimeException: boom\n\tat Rules.java:1')
        const raised: ProjectStatusDetailedMessage[] = [
            { ...message(1, 'ERROR', 'Cannot run the rule'), stacktrace: true },
            message(2, 'ERROR', 'Identifier is not found'),
        ]
        render(<TableProblems messages={raised} onStacktrace={onStacktrace} />)

        // A trace runs to thousands of characters, so nothing is read until the reader asks for it.
        expect(onStacktrace).not.toHaveBeenCalled()
        // A message carrying no trace offers none.
        expect(screen.queryByTestId('table-message-2-stacktrace-toggle')).toBeNull()

        await userEvent.click(screen.getByTestId('table-message-1-stacktrace-toggle'))

        expect(onStacktrace).toHaveBeenCalledWith(raised[0])
        expect(await screen.findByTestId('table-message-1-stacktrace')).toHaveTextContent('at Rules.java:1')

        await userEvent.click(screen.getByTestId('table-message-1-stacktrace-toggle'))

        expect(screen.queryByTestId('table-message-1-stacktrace')).toBeNull()
    })

    it('draws only the head of a long stack trace until the reader asks for the rest', async () => {
        const deep = Array.from({ length: 200 }, (_, at) => `\tat org.openl.Rules.step${at}(Rules.java:${at})`)
        const onStacktrace = vi.fn().mockResolvedValue(`java.lang.RuntimeException: boom\n${deep.join('\n')}`)
        render(<TableProblems
            messages={[{ ...message(1, 'ERROR', 'Cannot run the rule'), stacktrace: true }]}
            onStacktrace={onStacktrace}
        />)

        await userEvent.click(screen.getByTestId('table-message-1-stacktrace-toggle'))

        // Two hundred lines put on screen whole are laid out and painted again on every frame of a scroll.
        const shown = await screen.findByTestId('table-message-1-stacktrace')
        expect(shown).toHaveTextContent('step0')
        expect(shown).not.toHaveTextContent('step199')

        await userEvent.click(screen.getByRole('button', { name: 'browser.compile.show_more_text' }))

        expect(screen.getByTestId('table-message-1-stacktrace')).toHaveTextContent('step199')
    })

    it('says so when the stack trace cannot be read', async () => {
        const onStacktrace = vi.fn().mockRejectedValue(new Error('gone'))
        render(<TableProblems
            messages={[{ ...message(1, 'ERROR', 'Cannot run the rule'), stacktrace: true }]}
            onStacktrace={onStacktrace}
        />)

        await userEvent.click(screen.getByTestId('table-message-1-stacktrace-toggle'))

        expect(await screen.findByTestId('table-message-1-stacktrace'))
            .toHaveTextContent('browser.compile.stacktrace_failed')
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

    it('takes the height it was dragged to, and keeps it for the next table', () => {
        localStorage.setItem('openl.module.tableProblems.height', '320')

        render(<TableProblems messages={[message(1, 'ERROR', 'Identifier is not found')]} />)

        expect(screen.getByTestId('table-problems-body')).toHaveStyle({ height: '320px' })
        expect(screen.getByTestId('table-problems-resizer')).toBeInTheDocument()
    })
})
