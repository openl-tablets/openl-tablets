import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
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
        // Resume and pause share one slot; pause only appears while the worker is actually running.
        expect(screen.queryByTestId('debug-pause')).not.toBeInTheDocument()
    })

    it('while running, swaps resume for pause and disables stepping', () => {
        useTraceStore.setState({ status: 'running', loading: false })
        render(<DebugToolbar />)

        expect(screen.getByTestId('debug-pause')).toBeEnabled()
        expect(screen.queryByTestId('debug-resume')).not.toBeInTheDocument()
        expect(screen.getByTestId('debug-step-into')).toBeDisabled()
        expect(screen.getByTestId('debug-step-over')).toBeDisabled()
        expect(screen.getByTestId('debug-step-out')).toBeDisabled()
    })

    it('shows pause disabled while the run is still starting (pending)', () => {
        useTraceStore.setState({ status: 'pending', loading: false })
        render(<DebugToolbar />)

        // The pause slot is present during start-up, but nothing is running yet, so it cannot be clicked.
        expect(screen.getByTestId('debug-pause')).toBeDisabled()
        expect(screen.queryByTestId('debug-resume')).not.toBeInTheDocument()
    })

    it('ignores a second pause click while the first request is still in flight', async () => {
        const actions = stubActions()
        // Hold the pause request open so the second click lands before the first settles.
        let releasePause: () => void = () => {}
        actions.pause.mockReturnValue(new Promise<void>((resolve) => { releasePause = resolve }))
        useTraceStore.setState({ status: 'running', loading: false })
        render(<DebugToolbar />)

        const pauseButton = screen.getByTestId('debug-pause')
        await userEvent.click(pauseButton)
        // The button disables on the first click, so the second click is a no-op.
        await userEvent.click(pauseButton)
        expect(actions.pause).toHaveBeenCalledTimes(1)

        releasePause()
        await waitFor(() => expect(pauseButton).toBeEnabled())
    })

    it('disables every control on a terminal (completed) session, except rerun', () => {
        useTraceStore.setState({ status: 'completed', loading: false })
        render(<DebugToolbar />)

        expect(screen.getByTestId('debug-resume')).toBeDisabled()
        expect(screen.queryByTestId('debug-pause')).not.toBeInTheDocument()
        expect(screen.getByTestId('debug-step-into')).toBeDisabled()
        // There is no stop button — closing the window terminates the session.
        expect(screen.queryByTestId('debug-stop')).not.toBeInTheDocument()
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
