import React from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useTraceStore } from 'store/traceStore'
import type { DecisionView } from 'types/trace'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: { setBreakpoints: vi.fn().mockResolvedValue(undefined) },
}))

vi.mock('react-i18next', () => {
    const t = (key: string, opts?: { rules?: string; table?: string }) =>
        key === 'decision.fired' ? `Fired: ${opts?.rules}` : key
    return { useTranslation: () => ({ t }) }
})

import DecisionPanel from 'containers/TraceView/components/DecisionPanel'

const decision: DecisionView = {
    firedRules: ['Standard'],
    conditions: [
        { condition: 'Age', rule: 'Standard', matched: true },
        { condition: 'State', rule: 'Standard', matched: true },
        { condition: 'Age', rule: 'Senior', matched: true },
        { condition: 'State', rule: 'Senior', matched: false },
    ],
}

describe('DecisionPanel', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1' })
    })

    it('shows the fired rule and per-rule condition outcomes', () => {
        render(<DecisionPanel decision={decision} frameName="DT" frameUri="dt/uri" />)

        expect(screen.getByText('Fired: Standard')).toBeInTheDocument()
        expect(screen.getByText('Senior')).toBeInTheDocument()
        // Each rule lists its conditions, so each condition label appears once per rule.
        expect(screen.getAllByText('Age')).toHaveLength(2)
        expect(screen.getAllByText('State')).toHaveLength(2)
    })

    it('reports when no rule fired', () => {
        render(<DecisionPanel decision={{ firedRules: [], conditions: []}} frameName="DT" frameUri="dt/uri" />)
        expect(screen.getByText('decision.noneFired')).toBeInTheDocument()
    })

    it('toggles a rule-fired breakpoint keyed to the table', async () => {
        render(<DecisionPanel decision={decision} frameName="DT" frameUri="dt/uri" />)
        const toggle = screen.getByRole('checkbox')
        expect(toggle).not.toBeChecked()

        await userEvent.click(toggle)
        expect(useTraceStore.getState().breakpoints).toContain('dt/uri#rule')

        await userEvent.click(toggle)
        expect(useTraceStore.getState().breakpoints).not.toContain('dt/uri#rule')
    })

    it('offers the rule-fired breakpoint before any rule has fired', async () => {
        render(<DecisionPanel decision={null} frameName="DT" frameUri="dt/uri" />)

        // At decision-table entry there is no firing yet, but the breakpoint must already be settable.
        expect(screen.getByText('decision.notYetFired')).toBeInTheDocument()
        await userEvent.click(screen.getByRole('checkbox'))
        expect(useTraceStore.getState().breakpoints).toContain('dt/uri#rule')
    })

    it('toggles a breakpoint on a specific rule via its gutter', async () => {
        render(<DecisionPanel decision={decision} frameName="DT" frameUri="dt/uri" />)

        await userEvent.click(screen.getByTestId('decision-rule-bp-Senior'))
        expect(useTraceStore.getState().breakpoints).toContain('dt/uri#Senior')
        // The any-rule and other-rule keys are untouched.
        expect(useTraceStore.getState().breakpoints).not.toContain('dt/uri#rule')
        expect(useTraceStore.getState().breakpoints).not.toContain('dt/uri#Standard')
    })
})
