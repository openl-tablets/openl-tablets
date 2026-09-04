import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import type { StepValueView } from 'types/trace'
import SpreadsheetGrid from 'containers/TraceView/components/SpreadsheetGrid'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        setBreakpoints: vi.fn().mockResolvedValue(undefined),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

import traceService from 'services/traceService'

const setBreakpoints = traceService.setBreakpoints as ReturnType<typeof vi.fn>

// A scalar step value: renders inline (no antd Tree), so no act() loops.
const scalar = (value: number) => ({ name: '', description: 'Double', lazy: false, value })

const step = (ref: string, status: StepValueView['status'], over: Partial<StepValueView> = {}): StepValueView =>
    ({ ref, label: ref, status, ...over })

describe('SpreadsheetGrid', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        // The breakpoint gutter belongs to the advanced debugger; these cases exercise it.
        useTraceStore.setState({ projectId: 'p1', advanced: true })
    })

    it('shows the empty state when there are no steps', () => {
        render(<SpreadsheetGrid columns={['Formula']} frameUri="u0" rows={['Rate']} steps={[]} />)
        expect(screen.getByText('debug.noSteps')).toBeInTheDocument()
        expect(screen.queryByTestId('debug-spreadsheet-grid')).toBeNull()
    })

    it('shows the empty state when columns/rows are missing even if steps exist', () => {
        render(<SpreadsheetGrid columns={null} frameUri="u0" rows={null} steps={[step('R0C0', 'executed')]} />)
        expect(screen.getByText('debug.noSteps')).toBeInTheDocument()
    })

    it('lays the steps out as a grid keyed by row/column reference', () => {
        render(
            <SpreadsheetGrid
                columns={['Formula', 'Value']}
                frameUri="u0"
                rows={['Base', 'Total']}
                steps={[
                    step('R0C0', 'executed', { value: scalar(1) }),
                    step('R1C1', 'current'),
                ]}
            />
        )

        expect(screen.getByTestId('debug-spreadsheet-grid')).toBeInTheDocument()
        // Row and column headers come from the passed names.
        expect(screen.getByText('Base')).toBeInTheDocument()
        expect(screen.getByText('Formula')).toBeInTheDocument()
        // Executed cell shows its computed scalar value.
        expect(screen.getByTestId('debug-cell-R0C0')).toHaveTextContent('1')
        // The current cell shows the "executing" badge in its own cell.
        expect(screen.getByTestId('debug-cell-R1C1')).toHaveTextContent('debug.executing')
    })

    it('renders a pending marker for a not-yet-run cell that has no value', () => {
        render(
            <SpreadsheetGrid
                columns={['Formula']}
                frameUri="u0"
                rows={['Base']}
                steps={[step('R0C0', 'pending')]}
            />
        )
        expect(screen.getByTestId('debug-cell-R0C0')).toHaveTextContent('debug.pending')
    })

    it('offers a breakpoint gutter only on not-yet-executed cells', () => {
        render(
            <SpreadsheetGrid
                columns={['Formula']}
                frameUri="u0"
                rows={['Base', 'Total']}
                steps={[
                    step('R0C0', 'executed', { value: scalar(1) }),
                    step('R1C0', 'pending'),
                ]}
            />
        )
        // Executed cells have no gutter; pending cells do.
        expect(screen.queryByTestId('debug-cell-bp-R0C0')).toBeNull()
        expect(screen.getByTestId('debug-cell-bp-R1C0')).toBeInTheDocument()
    })

    it('toggles a breakpoint (keyed frameUri#ref) when the gutter is clicked', async () => {
        render(
            <SpreadsheetGrid
                columns={['Formula']}
                frameUri="u0"
                rows={['Base']}
                steps={[step('R0C0', 'pending', { label: '$Base' })]}
            />
        )

        await userEvent.click(screen.getByTestId('debug-cell-bp-R0C0'))
        await waitFor(() => expect(setBreakpoints).toHaveBeenCalledWith('p1', ['u0#R0C0']))
        expect(useTraceStore.getState().breakpoints).toContain('u0#R0C0')
        expect(useTraceStore.getState().breakpointLabels['u0#R0C0']).toBe('$Base')
    })

    it('toggles the breakpoint from the keyboard (Enter on the gutter)', async () => {
        render(
            <SpreadsheetGrid
                columns={['Formula']}
                frameUri="u0"
                rows={['Base']}
                steps={[step('R0C0', 'pending')]}
            />
        )

        screen.getByTestId('debug-cell-bp-R0C0').focus()
        await userEvent.keyboard('{Enter}')
        await waitFor(() => expect(useTraceStore.getState().breakpoints).toContain('u0#R0C0'))
    })

    it('shows no breakpoint gutter in the business view', () => {
        useTraceStore.setState({ advanced: false })
        render(
            <SpreadsheetGrid
                columns={['Formula']}
                frameUri="u0"
                rows={['Base']}
                steps={[step('R0C0', 'pending')]}
            />
        )
        // Breakpoints are a debugger feature; the business view offers none, even on a not-yet-run cell.
        expect(screen.queryByTestId('debug-cell-bp-R0C0')).toBeNull()
        expect(screen.getByTestId('debug-cell-R0C0')).toHaveTextContent('debug.pending')
    })

    it('marks an armed gutter as active', () => {
        useTraceStore.setState({ breakpoints: ['u0#R0C0']})
        render(
            <SpreadsheetGrid
                columns={['Formula']}
                frameUri="u0"
                rows={['Base']}
                steps={[step('R0C0', 'pending')]}
            />
        )
        // An already-armed gutter advertises removal rather than adding.
        expect(screen.getByTestId('debug-cell-bp-R0C0')).toHaveAttribute('aria-label', 'debug.removeBreakpoint')
    })
})
