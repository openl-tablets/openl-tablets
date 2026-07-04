import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import DebugToolbar from 'containers/TraceView/components/DebugToolbar'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        step: vi.fn().mockResolvedValue({ status: 'suspended', frames: []}),
        resume: vi.fn().mockResolvedValue(undefined),
        pause: vi.fn().mockResolvedValue(undefined),
        cancelTrace: vi.fn().mockResolvedValue(undefined),
        getStack: vi.fn().mockRejectedValue(new Error('no session')),
        startTrace: vi.fn().mockResolvedValue({ status: 'suspended', frames: []}),
        setBreakpoints: vi.fn().mockResolvedValue(undefined),
        getVariables: vi.fn().mockResolvedValue({ parameters: [], steps: [], errors: []}),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// AntD notification pops on the profiling toggle; keep it silent and inspectable.
vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    return { ...actual, notification: { ...actual.notification, info: vi.fn() } }
})

/** Replace the store's async actions with spies so a click can be attributed to one action. */
const stubActions = () => {
    const actions = {
        stepInto: vi.fn().mockResolvedValue(undefined),
        stepOver: vi.fn().mockResolvedValue(undefined),
        stepOut: vi.fn().mockResolvedValue(undefined),
        resume: vi.fn().mockResolvedValue(undefined),
        pause: vi.fn().mockResolvedValue(undefined),
        terminate: vi.fn().mockResolvedValue(undefined),
        rerun: vi.fn().mockResolvedValue(undefined),
        setProfiling: vi.fn().mockResolvedValue(undefined),
    }
    useTraceStore.setState(actions)
    return actions
}

describe('DebugToolbar', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1', tableId: 't1' })
    })

    it('enables the stepping controls only while suspended', () => {
        useTraceStore.setState({ status: 'suspended', loading: false })
        render(<DebugToolbar />)

        expect(screen.getByTestId('debug-resume')).toBeEnabled()
        expect(screen.getByTestId('debug-step-into')).toBeEnabled()
        expect(screen.getByTestId('debug-step-over')).toBeEnabled()
        expect(screen.getByTestId('debug-step-out')).toBeEnabled()
        expect(screen.getByTestId('debug-stop')).toBeEnabled()
        // Pause only makes sense while the worker is actually running.
        expect(screen.getByTestId('debug-pause')).toBeDisabled()
    })

    it('while running, offers pause and stop but disables resume and stepping', () => {
        useTraceStore.setState({ status: 'running', loading: false })
        render(<DebugToolbar />)

        expect(screen.getByTestId('debug-pause')).toBeEnabled()
        expect(screen.getByTestId('debug-stop')).toBeEnabled()
        expect(screen.getByTestId('debug-resume')).toBeDisabled()
        expect(screen.getByTestId('debug-step-into')).toBeDisabled()
        expect(screen.getByTestId('debug-step-over')).toBeDisabled()
        expect(screen.getByTestId('debug-step-out')).toBeDisabled()
    })

    it('disables every control on a terminal (completed) session, except rerun', () => {
        useTraceStore.setState({ status: 'completed', loading: false })
        render(<DebugToolbar />)

        expect(screen.getByTestId('debug-resume')).toBeDisabled()
        expect(screen.getByTestId('debug-pause')).toBeDisabled()
        expect(screen.getByTestId('debug-step-into')).toBeDisabled()
        expect(screen.getByTestId('debug-stop')).toBeDisabled()
        // Rerun stays available so the user can start over from a finished run.
        expect(screen.getByTestId('debug-rerun')).toBeEnabled()
    })

    it('greys the stepping controls while a synchronous step is loading', () => {
        useTraceStore.setState({ status: 'suspended', loading: true })
        render(<DebugToolbar />)

        expect(screen.getByTestId('debug-resume')).toBeDisabled()
        expect(screen.getByTestId('debug-step-into')).toBeDisabled()
        expect(screen.getByTestId('debug-rerun')).toBeDisabled()
    })

    it('routes each button to its matching store action', async () => {
        const actions = stubActions()
        useTraceStore.setState({ status: 'suspended', loading: false })
        render(<DebugToolbar />)

        await userEvent.click(screen.getByTestId('debug-resume'))
        expect(actions.resume).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByTestId('debug-step-into'))
        expect(actions.stepInto).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByTestId('debug-step-over'))
        expect(actions.stepOver).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByTestId('debug-step-out'))
        expect(actions.stepOut).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByTestId('debug-stop'))
        expect(actions.terminate).toHaveBeenCalledTimes(1)

        await userEvent.click(screen.getByTestId('debug-rerun'))
        expect(actions.rerun).toHaveBeenCalledTimes(1)
    })

    it('pauses only while running, and pause reaches the store action', async () => {
        const actions = stubActions()
        useTraceStore.setState({ status: 'running', loading: false })
        render(<DebugToolbar />)

        await userEvent.click(screen.getByTestId('debug-pause'))
        expect(actions.pause).toHaveBeenCalledTimes(1)
    })

    it('reflects profiling state and toggles it through the store', async () => {
        const actions = stubActions()
        useTraceStore.setState({ status: 'suspended', loading: false, profiling: false })
        render(<DebugToolbar />)

        const toggle = screen.getByTestId('debug-profiling')
        expect(toggle).not.toBeChecked()

        await userEvent.click(toggle)
        expect(actions.setProfiling).toHaveBeenCalledWith(true)
    })
})
