import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import type { DebugFrameView, StepValueView } from 'types/trace'
import TraceTree from 'containers/TraceView/components/TraceTree'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        setBreakpoints: vi.fn().mockResolvedValue(undefined),
        resume: vi.fn().mockResolvedValue(undefined),
        getVariables: vi.fn().mockResolvedValue({ parameters: [], steps: [], errors: []}),
        cancelTrace: vi.fn().mockResolvedValue(undefined),
        getStack: vi.fn().mockResolvedValue({ status: 'SUSPENDED', frames: []}),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

import traceService from 'services/traceService'

const resume = traceService.resume as ReturnType<typeof vi.fn>
const getVariables = traceService.getVariables as ReturnType<typeof vi.fn>
const cancelTrace = traceService.cancelTrace as ReturnType<typeof vi.fn>

const step = (ref: string, status: StepValueView['status'], label?: string): StepValueView =>
    ({ ref, label: label ?? ref, status })

const frame = (index: number, over: Partial<DebugFrameView> = {}): DebugFrameView => ({
    index,
    depth: index + 1,
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

describe('TraceTree', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
    })

    it('renders a frame with its steps and runs to a not-yet-reached step', async () => {
        useTraceStore.setState({
            projectId: 'p1',
            status: 'SUSPENDED',
            frames: [frame(0, {
                name: 'ROOT',
                active: true,
                steps: [step('R0C0', 'executed', '$Step1'), step('R1C0', 'pending', '$Step2')],
            })],
            selectedFrameIndex: 0,
        })

        render(<TraceTree />)
        expect(screen.getByText('ROOT')).toBeInTheDocument()
        expect(screen.getByText('$Step1')).toBeInTheDocument()
        expect(screen.getByText('$Step2')).toBeInTheDocument()

        // A pending step arms a one-shot breakpoint on that cell and resumes.
        await userEvent.click(screen.getByTestId('tree-step-0-R1C0'))
        await waitFor(() => expect(resume).toHaveBeenCalledWith('p1'))
        expect(useTraceStore.getState().breakpoints).toContain('u0#R1C0')
    })

    it('drills the current step into the child frame so every level shows at once', () => {
        useTraceStore.setState({
            status: 'SUSPENDED',
            frames: [
                frame(0, { name: 'ROOT', steps: [
                    step('R0C0', 'executed', '$Step1'),
                    step('R1C0', 'current', '$Step2'),
                ]}),
                frame(1, { name: 'Child', active: true, kind: 'decisionTable', steps: [
                    step('R1', 'executed'),
                    step('R2', 'pending'),
                ]}),
            ],
            selectedFrameIndex: 1,
        })

        render(<TraceTree />)
        // The parent cells stay visible while the child frame is expanded under the current cell.
        expect(screen.getByText('$Step1')).toBeInTheDocument()
        expect(screen.getByText('$Step2')).toBeInTheDocument()
        expect(screen.getByText('Child')).toBeInTheDocument()
        expect(screen.getByTestId('tree-step-1-R1')).toBeInTheDocument()
        expect(screen.getByTestId('tree-step-1-R2')).toBeInTheDocument()
    })

    it('does not run to an executed step; it selects the frame to read the result', async () => {
        useTraceStore.setState({
            projectId: 'p1',
            status: 'SUSPENDED',
            frames: [frame(0, { name: 'ROOT', active: true, steps: [step('R0C0', 'executed', '$Step1')]})],
            selectedFrameIndex: null,
        })

        render(<TraceTree />)
        await userEvent.click(screen.getByTestId('tree-step-0-R0C0'))
        await waitFor(() => expect(getVariables).toHaveBeenCalled())
        expect(resume).not.toHaveBeenCalled()
        expect(useTraceStore.getState().breakpoints).not.toContain('u0#R0C0')
    })

    it('keeps an executed sub-call collapsed and expands its retained structure on demand', async () => {
        useTraceStore.setState({
            status: 'SUSPENDED',
            frames: [frame(0, {
                name: 'ROOT',
                active: true,
                steps: [{
                    ...step('R0C0', 'executed', '$CoveragePremium'),
                    children: [{
                        uri: 'u9',
                        name: 'CoveragePremium',
                        kind: 'decisionTable',
                        durationMillis: 3.4,
                        selfMillis: 3.4,
                        steps: [step('R1', 'executed')],
                    }],
                }],
            })],
            selectedFrameIndex: 0,
        })

        render(<TraceTree />)
        // Collapsed by default — the returned branch is not drawn until asked for.
        expect(screen.queryByText('CoveragePremium')).not.toBeInTheDocument()

        await userEvent.click(screen.getByTestId('tree-toggle-f0/R0C0'))
        expect(screen.getByText('CoveragePremium')).toBeInTheDocument()
        expect(screen.getByText('R1')).toBeInTheDocument()
        // The rule can be replayed to directly, not only its table (key is uri#ref).
        expect(screen.getByTestId('tree-replay-u9#R1')).toBeInTheDocument()
    })

    it('renders the completed tree with a node timing and toggles Total/Self', async () => {
        useTraceStore.setState({
            status: 'COMPLETED',
            frames: [],
            tree: { uri: 'uRoot', name: 'ROOT', kind: 'spreadsheet', durationMillis: 42, selfMillis: 12, steps: []},
        })
        render(<TraceTree />)
        // The empty live stack does not hide the tree; total time shows by default.
        expect(screen.getByText('ROOT')).toBeInTheDocument()
        expect(screen.getByText('42 ms')).toBeInTheDocument()

        // Switching to Self time shows the node's own time instead of the inclusive total.
        await userEvent.click(screen.getByText('tree.timeSelf'))
        expect(screen.getByText('12 ms')).toBeInTheDocument()
    })

    it('replays a returned node by restarting the trace and running to it', async () => {
        useTraceStore.setState({
            projectId: 'p1',
            tableId: 'tRoot',
            status: 'COMPLETED',
            frames: [],
            tree: { uri: 'uRoot', name: 'ROOT', kind: 'spreadsheet', durationMillis: 42, selfMillis: 42, steps: []},
        })
        render(<TraceTree />)
        await userEvent.click(screen.getByTestId('tree-replay-uRoot'))
        // Replay restarts (terminate) and then runs to the table.
        await waitFor(() => expect(cancelTrace).toHaveBeenCalledWith('p1'))
        await waitFor(() => expect(resume).toHaveBeenCalledWith('p1'))
    })

    it('shows the empty state when there is no stack', () => {
        useTraceStore.setState({ status: null, frames: [], tree: null })
        render(<TraceTree />)
        expect(screen.getByText('debug.notSuspended')).toBeInTheDocument()
    })

    it('badges a version-dispatched frame with the number of versions', () => {
        useTraceStore.setState({
            status: 'SUSPENDED',
            frames: [frame(0, {
                name: 'RateRule',
                active: true,
                dispatch: { candidates: [
                    { label: 'effectiveDate: 01/01/2020', chosen: false },
                    { label: 'effectiveDate: 01/01/2021', chosen: true },
                ]},
            })],
            selectedFrameIndex: 0,
        })
        render(<TraceTree />)
        expect(screen.getByTestId('tree-dispatch')).toHaveTextContent('2')
    })
})
