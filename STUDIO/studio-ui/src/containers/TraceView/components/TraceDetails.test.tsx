import React from 'react'
import { render, screen } from '@testing-library/react'
import { useTraceStore } from 'store/traceStore'
import type { DebugFrameVariables, DebugFrameView } from 'types/trace'
import TraceDetails from 'containers/TraceView/components/TraceDetails'

vi.mock('services/traceService', () => ({
    __esModule: true,
    default: {
        getVariables: vi.fn().mockResolvedValue({ parameters: [], steps: [], errors: []}),
        getFrameHighlights: vi.fn().mockResolvedValue([]),
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
    default: ({ frameIndex }: { frameIndex: number }) => (
        <div data-testid="stub-table">table:{frameIndex}</div>
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
