import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import type { DebugFrameView } from 'types/trace'
import DebugCallStack from 'containers/TraceView/components/DebugCallStack'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        getVariables: vi.fn().mockResolvedValue({ parameters: [], steps: [], errors: []}),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

import traceService from 'services/traceService'

const getVariables = traceService.getVariables as ReturnType<typeof vi.fn>

const frame = (index: number, over: Partial<DebugFrameView> = {}): DebugFrameView => ({
    index,
    depth: index + 1,
    instance: 0,
    uri: `u${index}`,
    tableId: `t${index}`,
    name: `Frame${index}`,
    kind: 'spreadsheet',
    active: false,
    completed: false,
    error: false,
    steps: [],
    ...over,
})

describe('DebugCallStack', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1' })
    })

    it('shows the empty state when there is no live stack', () => {
        useTraceStore.setState({ frames: []})
        render(<DebugCallStack />)
        expect(screen.getByText('debug.notSuspended')).toBeInTheDocument()
        expect(screen.queryByTestId('debug-callstack')).toBeNull()
    })

    it('renders frames current-first (top of stack shown before the root)', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame(0, { name: 'ROOT' }), frame(1, { name: 'Child', active: true })],
            selectedFrameIndex: 1,
        })
        render(<DebugCallStack />)

        const stack = screen.getByTestId('debug-callstack')
        const names = Array.from(stack.querySelectorAll('[data-testid^="debug-frame-"] span'))
            .map(el => el.textContent)
        // Current frame (Child) precedes the root (ROOT).
        expect(names[0]).toBe('Child')
        expect(names).toContain('ROOT')
        // The active frame is marked for assistive tech.
        expect(screen.getByTestId('debug-frame-1')).toHaveAttribute('aria-current', 'true')
    })

    it('shows each frame kind, its location label, and an error tag', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame(0, {
                name: 'CalcRate',
                kind: 'decisionTable',
                active: true,
                error: true,
                location: { kind: 'dtrule', label: 'C7' },
            })],
            selectedFrameIndex: 0,
        })
        render(<DebugCallStack />)

        expect(screen.getByText('decisionTable')).toBeInTheDocument()
        expect(screen.getByText('C7')).toBeInTheDocument()
        expect(screen.getByText('severity.ERROR')).toBeInTheDocument()
    })

    it('selects a frame on click, loading its variables', async () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame(0, { name: 'ROOT' }), frame(1, { name: 'Child', active: true })],
            selectedFrameIndex: 1,
        })
        render(<DebugCallStack />)

        await userEvent.click(screen.getByTestId('debug-frame-0'))
        expect(useTraceStore.getState().selectedFrameIndex).toBe(0)
        await waitFor(() => expect(getVariables).toHaveBeenCalledWith('p1', 0))
    })

    it('selects a frame from the keyboard (Space activates the row)', async () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame(0, { name: 'ROOT' }), frame(1, { name: 'Child', active: true })],
            selectedFrameIndex: 1,
        })
        render(<DebugCallStack />)

        screen.getByTestId('debug-frame-0').focus()
        await userEvent.keyboard(' ')
        await waitFor(() => expect(useTraceStore.getState().selectedFrameIndex).toBe(0))
    })
})
