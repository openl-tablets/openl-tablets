import React from 'react'
import { act, render, screen, waitFor } from '@testing-library/react'
import traceService from 'services/traceService'
import { useTraceStore } from 'store/traceStore'
import type { DebugFrameVariables, DebugFrameView } from 'types/trace'
import TraceDetails from 'containers/TraceView/components/TraceDetails'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        getVariables: vi.fn().mockResolvedValue({ parameters: [], steps: [], errors: []}),
        getFrameHighlights: vi.fn().mockResolvedValue([]),
        getStepInputs: vi.fn().mockResolvedValue([]),
    },
}))

vi.mock('react-i18next', () => {
    const t = (key: string) => key
    return { useTranslation: () => ({ t }) }
})

// Stub the heavy children so TraceDetails' own orchestration (which panel for which frame kind) is
// isolated and fast. Each stub advertises its presence and the load-bearing prop.
vi.mock('containers/TraceView/components/TraceTableView', () => ({
    __esModule: true,
    default: ({ frameIndex, highlightCell }: { frameIndex: number; highlightCell?: string | null }) => (
        <div data-testid="stub-table">table:{frameIndex}:{highlightCell ?? 'none'}</div>
    ),
}))
vi.mock('containers/TraceView/components/SpreadsheetGrid', () => ({
    __esModule: true,
    default: ({ frameUri }: { frameUri: string }) => <div data-testid="stub-spreadsheet">{frameUri}</div>,
}))
vi.mock('containers/TraceView/components/DecisionPanel', () => ({
    __esModule: true,
    default: ({ frameName }: { frameName: string }) => <div data-testid="stub-decision">{frameName}</div>,
}))

const frame = (over: Partial<DebugFrameView> = {}): DebugFrameView => ({
    index: 0,
    depth: 1,
    instance: 0,
    uri: 'u0',
    tableId: 't0',
    name: 'CoveragePremium',
    kind: 'spreadsheet',
    active: true,
    completed: false,
    error: false,
    steps: [],
    ...over,
})

const variables = (over: Partial<DebugFrameVariables> = {}): DebugFrameVariables => ({
    parameters: [{ name: 'age', description: 'int', lazy: false, value: 30 }],
    result: { name: 'result', description: 'Double', lazy: false, value: 1.5 },
    steps: [],
    errors: [],
    ...over,
})

describe('TraceDetails', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        useTraceStore.getState().reset()
        useTraceStore.setState({ projectId: 'p1' })
    })

    it('shows the no-selection empty state when no frame is selected', () => {
        useTraceStore.setState({ selectedFrameIndex: null })
        render(<TraceDetails />)
        expect(screen.getByText('details.noSelection')).toBeInTheDocument()
        expect(screen.queryByTestId('debug-details')).toBeNull()
    })

    it('renders parameters, result, and the traced table for a selected frame', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame()],
            selectedFrameIndex: 0,
            variables: variables(),
            variablesLoading: false,
        })
        render(<TraceDetails />)

        expect(screen.getByTestId('debug-details')).toBeInTheDocument()
        // The frame title, its input parameter, and its result all appear.
        expect(screen.getByText('CoveragePremium')).toBeInTheDocument()
        expect(screen.getByText('age')).toBeInTheDocument()
        expect(screen.getByText('30')).toBeInTheDocument()
        expect(screen.getByText('1.5')).toBeInTheDocument()
        // The traced source table is rendered for the selected frame index.
        expect(screen.getByTestId('stub-table')).toHaveTextContent('table:0')
        // The colour legend is present.
        expect(screen.getByTestId('trace-legend')).toBeInTheDocument()
    })

    it('adds the context to the parameter list when present', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame()],
            selectedFrameIndex: 0,
            variables: variables({
                context: { name: 'ctx.currentDate', description: 'Date', lazy: false, value: '2020-01-01' },
            }),
            variablesLoading: false,
        })
        render(<TraceDetails />)
        // Context is folded into the parameters section alongside the inputs.
        expect(screen.getByText('ctx.currentDate')).toBeInTheDocument()
    })

    it('renders the spreadsheet grid for a spreadsheet frame', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ kind: 'spreadsheet', uri: 'uSpread' })],
            selectedFrameIndex: 0,
            variables: variables({ gridColumns: ['Formula'], gridRows: ['Base']}),
            variablesLoading: false,
        })
        render(<TraceDetails />)

        expect(screen.getByTestId('stub-spreadsheet')).toHaveTextContent('uSpread')
        expect(screen.queryByTestId('stub-decision')).toBeNull()
    })

    it('renders the decision panel for a decision-table frame', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ kind: 'decisionTable', name: 'CalcRate' })],
            selectedFrameIndex: 0,
            variables: variables({ decision: { firedRules: ['Standard'], conditions: []} }),
            variablesLoading: false,
        })
        render(<TraceDetails />)

        expect(screen.getByTestId('stub-decision')).toHaveTextContent('CalcRate')
        expect(screen.queryByTestId('stub-spreadsheet')).toBeNull()
    })

    it('shows a spinner and hides the frame panels while variables are loading', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ kind: 'spreadsheet' })],
            selectedFrameIndex: 0,
            variables: null,
            variablesLoading: true,
        })
        render(<TraceDetails />)

        expect(screen.getByText('loadingDetails')).toBeInTheDocument()
        // The kind-specific panel is not drawn until variables settle.
        expect(screen.queryByTestId('stub-spreadsheet')).toBeNull()
    })

    it('presents a focused step: the values its formula consumed, its own value as `return`', async () => {
        // The suspension pauses in the owning table right after the step executed; the panel keeps the
        // step's identity, and the Parameters are the step's own inputs — what its formula consumed —
        // fetched in the formula's terms, not the owning table's parameter list.
        const getStepInputs = traceService.getStepInputs as ReturnType<typeof vi.fn>
        getStepInputs.mockResolvedValue([
            { name: '$LimitIndex', description: 'Double', lazy: false, value: 0.05 },
            { name: 'MaxLimit', description: 'Integer', lazy: false, value: 5000 },
        ])
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ uri: 'uOwner', name: 'BankRatingCalculation',
                steps: [{ ref: 'S9', label: '$Value$Limit', cell: 'C7', status: 'executed' }]})],
            selectedFrameIndex: 0,
            variables: variables({
                steps: [{ ref: 'S9', label: '$Value$Limit', status: 'executed',
                    value: { name: 'S9', description: 'Double', lazy: false, value: 250000 } }],
            }),
            variablesLoading: false,
            simpleFocus: { ref: 'S9', label: '$Value$Limit', ownerUri: 'uOwner', ownerInstance: 0 },
        })
        render(<TraceDetails />)

        expect(screen.getByText('$Value$Limit')).toBeInTheDocument() // titled by the step
        // The step's inputs arrive from the step-inputs endpoint, named as the formula writes them.
        expect(getStepInputs).toHaveBeenCalledWith('p1', 0, 'S9')
        await waitFor(() => expect(screen.getByText('$LimitIndex')).toBeInTheDocument())
        expect(screen.getByText('0.05')).toBeInTheDocument()
        expect(screen.getByText('MaxLimit')).toBeInTheDocument()
        expect(screen.getByText('5000')).toBeInTheDocument()
        expect(screen.queryByText('age')).toBeNull() // not the whole table's parameter list
        // The step's own value is the result, presented as `return` — not as the raw cell ref S9.
        expect(screen.getByText('return')).toBeInTheDocument()
        expect(screen.queryByText('S9')).toBeNull()
        expect(screen.getByText('250000')).toBeInTheDocument()
        expect(screen.queryByText('1.5')).toBeNull() // not the whole table's result
        // The traced table points at the clicked step's cell; the frame panels stay off.
        expect(screen.getByTestId('stub-table')).toHaveTextContent('table:0:C7')
        expect(screen.queryByTestId('stub-spreadsheet')).toBeNull()
    })

    it('shows no result for a focused step that has not produced a value', async () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ uri: 'uOwner',
                steps: [{ ref: 'S9', label: '$Value$Limit', cell: 'C7', status: 'pending' }]})],
            selectedFrameIndex: 0,
            variables: variables({ steps: []}),
            variablesLoading: false,
            simpleFocus: { ref: 'S9', label: '$Value$Limit', ownerUri: 'uOwner', ownerInstance: 0 },
        })
        await act(async () => {
            render(<TraceDetails />)
        })

        expect(screen.getByText('details.noResult')).toBeInTheDocument()
    })

    it('ignores the step focus in the advanced mode and shows the frame as-is', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame()],
            selectedFrameIndex: 0,
            variables: variables(),
            variablesLoading: false,
            advanced: true,
            simpleFocus: { ref: 'S1', label: '$Value$X', ownerUri: 'u0', ownerInstance: 0 },
        })
        render(<TraceDetails />)

        expect(screen.getByText('CoveragePremium')).toBeInTheDocument()
        expect(screen.queryByText('$Value$X')).toBeNull()
    })

    it('lists frame errors when the selected frame reported messages', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame()],
            selectedFrameIndex: 0,
            variables: variables({
                errors: [{ severity: 'ERROR', summary: 'Division by zero' }],
            }),
            variablesLoading: false,
        })
        render(<TraceDetails />)
        expect(screen.getByText('details.errors')).toBeInTheDocument()
        expect(screen.getByText('Division by zero')).toBeInTheDocument()
    })
})
