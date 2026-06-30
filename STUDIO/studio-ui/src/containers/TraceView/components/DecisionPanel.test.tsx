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

// AntD Select virtualises its dropdown, which does not render in jsdom; replace it with a native
// <select> so option selection is deterministic. The rest of antd (Card, Checkbox, Tag, Tooltip) stays real.
interface MockSelectProps {
    options?: { value: string; label: string }[]
    value?: string[]
    onSelect?: (value: string) => void
    onDeselect?: (value: string) => void
    'data-testid'?: string
}

vi.mock('antd', async () => {
    const actual = await vi.importActual<typeof import('antd')>('antd')
    const Select = ({ options = [], value = [], onSelect, onDeselect, ...rest }: MockSelectProps) => (
        <select
            multiple
            data-testid={rest['data-testid']}
            value={value}
            onChange={e => {
                const next = Array.from(e.target.selectedOptions, o => o.value)
                next.filter(v => !value.includes(v)).forEach(v => onSelect?.(v))
                value.filter(v => !next.includes(v)).forEach(v => onDeselect?.(v))
            }}
        >
            {options.map(o => (
                <option key={o.value} value={o.value}>{o.label}</option>
            ))}
        </select>
    )
    return { ...actual, Select }
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

    it('arms a breakpoint on any rule chosen from the all-rules dropdown', async () => {
        // The full rule list is available even before any rule fires (decision is null).
        render(<DecisionPanel decision={null} frameName="DT" frameUri="dt/uri" ruleNames={['R1', 'R2', 'R3']} />)

        await userEvent.selectOptions(screen.getByTestId('decision-rule-select'), 'R2')
        expect(useTraceStore.getState().breakpoints).toContain('dt/uri#R2')
    })

    it('hides the rule dropdown when the rule list is unavailable', () => {
        render(<DecisionPanel decision={decision} frameName="DT" frameUri="dt/uri" />)
        expect(screen.queryByTestId('decision-rule-select')).not.toBeInTheDocument()
    })

    it('caps the rule breakdown for a collect-all table and expands on demand', async () => {
        const many = Array.from({ length: 40 }, (_, i) => `R${i + 1}`)
        render(<DecisionPanel
            decision={{ firedRules: many, conditions: many.map(rule => ({ condition: 'C1', rule, matched: true })) }}
            frameName="DT"
            frameUri="dt/uri"
        />)

        // First rules are shown, rules beyond the cap are hidden until expanded.
        expect(screen.getByText('R1')).toBeInTheDocument()
        expect(screen.queryByText('R40')).not.toBeInTheDocument()

        await userEvent.click(screen.getByTestId('decision-show-all-rules'))
        expect(screen.getByText('R40')).toBeInTheDocument()
    })
})
