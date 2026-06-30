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
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

import traceService from 'services/traceService'

const resume = traceService.resume as ReturnType<typeof vi.fn>
const getVariables = traceService.getVariables as ReturnType<typeof vi.fn>

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

    it('shows the empty state when there is no stack', () => {
        useTraceStore.setState({ status: null, frames: []})
        render(<TraceTree />)
        expect(screen.getByText('debug.notSuspended')).toBeInTheDocument()
    })
})
