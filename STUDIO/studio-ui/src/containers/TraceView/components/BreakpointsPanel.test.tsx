import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import BreakpointsPanel from 'containers/TraceView/components/BreakpointsPanel'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        getBreakpointTables: vi.fn().mockResolvedValue([{ name: 'DetermineHousePremium' }, { name: 'CoveragePremium' }]),
        setBreakpoints: vi.fn().mockResolvedValue(undefined),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

import traceService from 'services/traceService'

const getBreakpointTables = traceService.getBreakpointTables as ReturnType<typeof vi.fn>
const setBreakpoints = traceService.setBreakpoints as ReturnType<typeof vi.fn>

describe('BreakpointsPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1' })
    })

    it('arms a breakpoint by name without that table being on screen', async () => {
        render(<BreakpointsPanel />)
        // Candidate tables are loaded lazily when the picker opens.
        await userEvent.click(screen.getByRole('combobox'))
        await waitFor(() => expect(getBreakpointTables).toHaveBeenCalledWith('p1'))

        await userEvent.click(await screen.findByText('DetermineHousePremium'))
        await waitFor(() => expect(setBreakpoints).toHaveBeenCalled())
        expect(useTraceStore.getState().breakpoints).toContain('DetermineHousePremium')
    })

    it('lists armed breakpoints and removes them', async () => {
        useTraceStore.setState({
            breakpoints: ['CoveragePremium'],
            breakpointLabels: { CoveragePremium: 'CoveragePremium' },
        })
        render(<BreakpointsPanel />)
        expect(screen.getByText('CoveragePremium')).toBeInTheDocument()

        await userEvent.click(screen.getByRole('button'))
        await waitFor(() => expect(setBreakpoints).toHaveBeenCalledWith('p1', []))
        expect(useTraceStore.getState().breakpoints).not.toContain('CoveragePremium')
    })

    it('shows a hint when there are no breakpoints', () => {
        render(<BreakpointsPanel />)
        expect(screen.getByText('debug.noBreakpoints')).toBeInTheDocument()
    })
})
