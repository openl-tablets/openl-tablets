import React from 'react'
import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ExecutionWindow } from './ExecutionWindow'
import { ExecutionWindowContext } from './ExecutionModal'
import { EXECUTION_EDITOR_NAVIGATION, EXECUTION_WINDOW_KEY, publishExecution } from 'services/executionWindow'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))
vi.mock('./TestsResultModal', () => ({
    TestsResultModal: ({ projectId }: { projectId: string }) => {
        const { openTable } = React.useContext(ExecutionWindowContext)
        const [opened, setOpened] = React.useState<boolean | null>(null)
        return (
            <button
                data-opened={String(opened)}
                data-testid="tests-window"
                onClick={() => setOpened(openTable?.('/projects/p1/modules/Rules?table=t1') ?? null)}
            >
                {projectId}
            </button>
        )
    },
}))
vi.mock('./RunResultModal', () => ({
    RunResultModal: ({ tableId }: { tableId: string }) => <div data-testid="run-window">{tableId}</div>,
}))
vi.mock('./BenchmarkResultModal', () => ({
    BenchmarkResultModal: ({ tableId }: { tableId: string }) => <div data-testid="benchmark-window">{tableId}</div>,
}))

describe('ExecutionWindow', () => {
    beforeEach(() => localStorage.clear())
    afterEach(() => vi.unstubAllGlobals())

    it('shows an empty state and follows later runs from the editor', () => {
        render(<ExecutionWindow />)
        expect(screen.getByText('noResults')).toBeInTheDocument()

        act(() => {
            publishExecution({ kind: 'tests', projectId: 'p1', tableId: 't1' })
            window.dispatchEvent(new StorageEvent('storage', { key: EXECUTION_WINDOW_KEY }))
        })
        expect(screen.getByTestId('tests-window')).toHaveTextContent('p1')

        act(() => {
            publishExecution({ kind: 'benchmark', projectId: 'p1', tableId: 't2' })
            window.dispatchEvent(new StorageEvent('storage', { key: EXECUTION_WINDOW_KEY }))
        })
        expect(screen.getByTestId('benchmark-window')).toHaveTextContent('t2')
    })

    it('restores the last run when the window is refreshed', () => {
        publishExecution({ kind: 'run', projectId: 'p1', tableId: 't3', fileOptions: {} })
        render(<ExecutionWindow />)
        expect(screen.getByTestId('run-window')).toHaveTextContent('t3')
        expect(document.title).toBe('windowTitle')
    })

    it('opens a result table in the original editor and keeps the results window', async () => {
        const dispatchEvent = vi.fn()
        const focus = vi.fn()
        vi.stubGlobal('opener', { closed: false, dispatchEvent, focus })
        publishExecution({ kind: 'tests', projectId: 'p1', tableId: 't1' })
        render(<ExecutionWindow />)

        await userEvent.click(screen.getByTestId('tests-window'))

        expect(dispatchEvent).toHaveBeenCalledWith(expect.objectContaining({
            type: EXECUTION_EDITOR_NAVIGATION,
            detail: '/projects/p1/modules/Rules?table=t1',
        }))
        expect(focus).toHaveBeenCalledOnce()
        expect(screen.getByTestId('tests-window')).toHaveAttribute('data-opened', 'true')
        expect(screen.getByTestId('tests-window')).toBeInTheDocument()
    })

    it('reports navigation success when the editor cannot be focused', async () => {
        const dispatchEvent = vi.fn()
        const focus = vi.fn(() => { throw new Error('Focus blocked') })
        vi.stubGlobal('opener', { closed: false, dispatchEvent, focus })
        publishExecution({ kind: 'tests', projectId: 'p1', tableId: 't1' })
        render(<ExecutionWindow />)

        await userEvent.click(screen.getByTestId('tests-window'))

        expect(dispatchEvent).toHaveBeenCalledOnce()
        expect(focus).toHaveBeenCalledOnce()
        expect(screen.getByTestId('tests-window')).toHaveAttribute('data-opened', 'true')
    })

    it('reports navigation failure when the editor rejects the event', async () => {
        const dispatchEvent = vi.fn(() => { throw new Error('Editor unavailable') })
        const focus = vi.fn()
        vi.stubGlobal('opener', { closed: false, dispatchEvent, focus })
        publishExecution({ kind: 'tests', projectId: 'p1', tableId: 't1' })
        render(<ExecutionWindow />)

        await userEvent.click(screen.getByTestId('tests-window'))

        expect(focus).not.toHaveBeenCalled()
        expect(screen.getByTestId('tests-window')).toHaveAttribute('data-opened', 'false')
    })

    it('opens a table in a new tab when the original editor has closed', async () => {
        const open = vi.spyOn(window, 'open').mockReturnValue({} as Window)
        vi.stubGlobal('opener', { closed: true })
        publishExecution({ kind: 'tests', projectId: 'p1', tableId: 't1' })
        render(<ExecutionWindow />)

        await userEvent.click(screen.getByTestId('tests-window'))

        expect(open).toHaveBeenCalledWith('/projects/p1/modules/Rules?table=t1', '_blank')
    })
})
