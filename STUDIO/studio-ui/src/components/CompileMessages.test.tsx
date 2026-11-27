import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, onTestFinished, vi } from 'vitest'
import { layOut } from 'testing/layOut'
import type { ProjectStatusDetailedMessage } from '../services/projectStatus'
import { CompileMessages } from './CompileMessages'

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t, i18n: { language: 'en' } }) }
})

/** How tall a message is drawn. */
const ROW = 30
/** How tall the element the messages scroll in is shown. */
const VIEW = 120

const LONG = `${'A long text of a message '.repeat(20)}\nline 2\nline 3\nline 4\nline 5`

const messages = (count: number): ProjectStatusDetailedMessage[] => Array.from({ length: count }, (_, index) => ({
    id: index + 1,
    severity: 'INFO',
    summary: index === 0 ? LONG : `Message ${index + 1}`,
    stacktrace: false,
}))

/**
 * The element the messages scroll in, as tall as the screen shows it. A browser tells it scrolled after the scrolling,
 * so the test tells it when it has scrolled ({@code scrolled}).
 */
const scroller = () => {
    const element = document.createElement('div')
    const at = { top: 0 }
    Object.defineProperty(element, 'clientHeight', { value: VIEW })
    Object.defineProperty(element, 'clientWidth', { value: 400 })
    Object.defineProperty(element, 'scrollTop', { get: () => at.top, set: (value: number) => { at.top = value } })
    element.scrollTo = (({ top: to }: ScrollToOptions) => {
        at.top = to ?? at.top
    }) as typeof element.scrollTo
    document.body.append(element)
    onTestFinished(() => element.remove())
    const scrolled = (to: number) => act(() => {
        at.top = to
        element.dispatchEvent(new Event('scroll'))
    })
    return { element, scrolled }
}

/** The messages drawn, by their ids. */
const drawn = () => screen.queryAllByTestId(/^compile-message-\d+$/)
    .map(message => Number(message.getAttribute('data-testid')?.slice('compile-message-'.length)))

describe('CompileMessages', () => {
    beforeEach(() => {
        // Every message is as tall as one line.
        layOut(HTMLLIElement.prototype, 'offsetHeight', () => ROW)
    })

    it('draws the messages in sight and a few around them, not the ones far below', async () => {
        const { element } = scroller()

        render(<CompileMessages messages={messages(500)} scroller={element} />, { container: element })

        await waitFor(() => expect(drawn()).toContain(1))
        expect(drawn()).not.toContain(500)
        expect(drawn().length).toBeLessThan(50)
        // The messages left out keep their room, so the list scrolls as far as it would drawn whole.
        expect(Number.parseFloat(element.querySelector('ul')?.style.height ?? '0')).toBe(500 * ROW)
    })

    it('draws the messages scrolled to, and lets go of the ones scrolled past', async () => {
        const { element, scrolled } = scroller()
        render(<CompileMessages messages={messages(500)} scroller={element} />, { container: element })
        await waitFor(() => expect(drawn()).toContain(1))

        await scrolled(300 * ROW)

        await waitFor(() => expect(drawn()).toContain(301))
        expect(drawn()).not.toContain(1)
    })

    it('keeps a message opened in full while it is scrolled out of sight', async () => {
        const { element, scrolled } = scroller()
        render(<CompileMessages messages={messages(500)} scroller={element} />, { container: element })
        await waitFor(() => expect(drawn()).toContain(1))
        fireEvent.click(screen.getByRole('button', { name: 'browser.compile.show_more_text' }))
        expect(screen.getByTestId('compile-message-1').textContent).toContain('line 5')

        await scrolled(300 * ROW)
        await waitFor(() => expect(drawn()).not.toContain(1))
        await scrolled(0)

        await waitFor(() => expect(drawn()).toContain(1))
        expect(screen.getByTestId('compile-message-1').textContent).toContain('line 5')
    })

    it('shows a stack trace read while its message was out of sight once the message is back', async () => {
        const { element, scrolled } = scroller()
        const raised = messages(500).map(message => (message.id === 1 ? { ...message, stacktrace: true } : message))
        let answer!: (trace: string) => void
        const onStacktrace = vi.fn(() => new Promise<string>(resolve => {
            answer = resolve
        }))
        render(<CompileMessages messages={raised} onStacktrace={onStacktrace} scroller={element} />, { container: element })
        await waitFor(() => expect(drawn()).toContain(1))
        fireEvent.click(screen.getByTestId('compile-message-1-stacktrace-toggle'))

        // The reader scrolls away and back while the trace is on its way
        await scrolled(300 * ROW)
        await waitFor(() => expect(drawn()).not.toContain(1))
        await scrolled(0)
        await waitFor(() => expect(drawn()).toContain(1))
        await act(async () => {
            answer('at org.openl.Rules.rate(Rules.java:42)')
        })

        expect(await screen.findByTestId('compile-message-1-stacktrace')).toHaveTextContent('Rules.java:42')
        expect(onStacktrace).toHaveBeenCalledTimes(1)
    })

    it('marks each message by the stripe of its severity', () => {
        const raised: ProjectStatusDetailedMessage[] = [
            { id: 1, severity: 'ERROR', summary: 'Broken', stacktrace: false },
            { id: 2, severity: 'WARN', summary: 'Deprecated', stacktrace: false },
            { id: 3, severity: 'INFO', summary: 'Not used', stacktrace: false },
        ]

        // An element with no layout to measure: every message is drawn
        render(<CompileMessages messages={raised} scroller={document.createElement('div')} />)

        const stripes = [1, 2, 3].map(id => screen.getByTestId(`compile-message-${id}`).parentElement?.className)
        expect(new Set(stripes).size).toBe(3)
    })
})
