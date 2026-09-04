import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { useTraceStore } from 'store/traceStore'
import TraceToolbar from './TraceToolbar'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))
// The debugger buttons are exercised in their own suite; a stub advertises their presence here.
vi.mock('./DebugToolbar', () => ({ default: () => <div data-testid="debug-toolbar" /> }))

describe('TraceToolbar', () => {
    beforeEach(() => {
        useTraceStore.getState().reset()
    })

    it('shows only the Show detailed view toggle in the business view', () => {
        useTraceStore.setState({ advanced: false })
        render(<TraceToolbar />)

        expect(screen.getByTestId('trace-detailed')).toBeInTheDocument()
        expect(screen.queryByTestId('debug-toolbar')).not.toBeInTheDocument()
        expect(screen.queryByTestId('trace-settings')).not.toBeInTheDocument()
    })

    it('shows the debugger buttons, the run status, and a settings gear in the advanced view', () => {
        useTraceStore.setState({ advanced: true, status: 'suspended' })
        render(<TraceToolbar />)

        expect(screen.getByTestId('debug-toolbar')).toBeInTheDocument()
        expect(screen.getByTestId('debug-status')).toHaveTextContent('debug.status.suspended')
        expect(screen.getByTestId('trace-settings')).toBeInTheDocument()
        // The run settings are behind the gear, not shown next to the buttons.
        expect(screen.queryByTestId('debug-profiling')).not.toBeInTheDocument()
    })

    it('swaps the buttons for the run settings when the gear is clicked', async () => {
        useTraceStore.setState({ advanced: true, status: 'suspended', profiling: true })
        render(<TraceToolbar />)

        await userEvent.click(screen.getByTestId('trace-settings'))

        expect(screen.queryByTestId('debug-toolbar')).not.toBeInTheDocument()
        expect(screen.getByTestId('debug-profiling')).toBeInTheDocument()
        // Detailed view acts on the executed tree, so it appears once profiling is on.
        expect(screen.getByTestId('trace-detailed')).toBeInTheDocument()
    })

    it('offers the detailed toggle in settings only while profiling is on', async () => {
        useTraceStore.setState({ advanced: true, status: 'suspended', profiling: false })
        render(<TraceToolbar />)

        await userEvent.click(screen.getByTestId('trace-settings'))

        expect(screen.getByTestId('debug-profiling')).toBeInTheDocument()
        expect(screen.queryByTestId('trace-detailed')).not.toBeInTheDocument()
    })

    it('toggles Show detailed view through the store', async () => {
        useTraceStore.setState({ advanced: false, showDetailed: false })
        render(<TraceToolbar />)

        await userEvent.click(screen.getByTestId('trace-detailed'))

        expect(useTraceStore.getState().showDetailed).toBe(true)
    })
})
