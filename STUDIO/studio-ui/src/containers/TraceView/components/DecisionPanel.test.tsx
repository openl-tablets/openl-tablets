import React from 'react'
import { render, screen } from '@testing-library/react'
import type { DecisionView } from 'types/trace'

vi.mock('react-i18next', () => {
    const t = (key: string, opts?: { rules?: string }) =>
        key === 'decision.fired' ? `Fired: ${opts?.rules}` : key
    return { useTranslation: () => ({ t }) }
})

import DecisionPanel from 'containers/TraceView/components/DecisionPanel'

describe('DecisionPanel', () => {
    it('shows the fired rule and per-rule condition outcomes', () => {
        const decision: DecisionView = {
            firedRules: ['Standard'],
            conditions: [
                { condition: 'Age', rule: 'Standard', matched: true },
                { condition: 'State', rule: 'Standard', matched: true },
                { condition: 'Age', rule: 'Senior', matched: true },
                { condition: 'State', rule: 'Senior', matched: false },
            ],
        }
        render(<DecisionPanel decision={decision} />)

        expect(screen.getByText('Fired: Standard')).toBeInTheDocument()
        expect(screen.getByText('Senior')).toBeInTheDocument()
        // Each rule lists its conditions, so each condition label appears once per rule.
        expect(screen.getAllByText('Age')).toHaveLength(2)
        expect(screen.getAllByText('State')).toHaveLength(2)
    })

    it('reports when no rule fired', () => {
        render(<DecisionPanel decision={{ firedRules: [], conditions: []}} />)
        expect(screen.getByText('decision.noneFired')).toBeInTheDocument()
    })
})
