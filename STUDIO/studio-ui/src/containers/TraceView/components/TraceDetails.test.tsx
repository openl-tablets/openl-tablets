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
        getStepInputs: vi.fn().mockResolvedValue({ inputs: [], result: null, cell: null }),
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

    it('shows a loading panel while a business click has cleared the frame and is re-running', () => {
        // simpleInspect drops selectedFrameIndex so a previous (e.g. throwing) table is not held on screen;
        // Details must spin for that window instead of flashing "no selection" or the stale table.
        useTraceStore.setState({ selectedFrameIndex: null, variablesLoading: true })
        render(<TraceDetails />)
        expect(screen.getByTestId('debug-details-loading')).toBeInTheDocument()
        expect(screen.queryByText('details.noSelection')).toBeNull()
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
        // The business view shows no title — the tree already names the rule — but the input and result appear.
        expect(screen.queryByText('CoveragePremium')).toBeNull()
        expect(screen.getByText('age')).toBeInTheDocument()
        expect(screen.getByText('30')).toBeInTheDocument()
        expect(screen.getByText('1.5')).toBeInTheDocument()
        // The traced source table is rendered for the selected frame index.
        expect(screen.getByTestId('stub-table')).toHaveTextContent('table:0')
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

    it('renders the spreadsheet grid for a spreadsheet frame in the advanced view', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ kind: 'spreadsheet', uri: 'uSpread' })],
            selectedFrameIndex: 0,
            variables: variables({ gridColumns: ['Formula'], gridRows: ['Base']}),
            variablesLoading: false,
            advanced: true,
        })
        render(<TraceDetails />)

        expect(screen.getByTestId('stub-spreadsheet')).toHaveTextContent('uSpread')
        expect(screen.queryByTestId('stub-decision')).toBeNull()
    })

    it('renders the decision panel for a decision-table frame in the advanced view', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ kind: 'decisionTable', name: 'CalcRate' })],
            selectedFrameIndex: 0,
            variables: variables({ decision: { firedRules: ['Standard'], conditions: []} }),
            variablesLoading: false,
            advanced: true,
        })
        render(<TraceDetails />)

        expect(screen.getByTestId('stub-decision')).toHaveTextContent('CalcRate')
        expect(screen.queryByTestId('stub-spreadsheet')).toBeNull()
    })

    it('hides the grid and decision panels in the business view — the tree already shows them', () => {
        // The business view shows the steps and the decision breakdown in the tree, so the extra grid /
        // decision table below the traced table would just duplicate it; they are an advanced detail.
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ kind: 'spreadsheet', uri: 'uSpread' })],
            selectedFrameIndex: 0,
            variables: variables({ gridColumns: ['Formula'], gridRows: ['Base']}),
            variablesLoading: false,
            advanced: false,
        })
        const first = render(<TraceDetails />)
        expect(screen.queryByTestId('stub-spreadsheet')).toBeNull()
        first.unmount()

        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ kind: 'decisionTable', name: 'CalcRate' })],
            selectedFrameIndex: 0,
            variables: variables({ decision: { firedRules: ['Standard'], conditions: []} }),
            variablesLoading: false,
            advanced: false,
        })
        render(<TraceDetails />)
        expect(screen.queryByTestId('stub-decision')).toBeNull()
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
        // The step is self-contained: its inputs, its own value (named `return`) and its cell address all
        // come from the one step-inputs payload — the frame's variables are present in the store but ignored.
        const getStepInputs = traceService.getStepInputs as ReturnType<typeof vi.fn>
        getStepInputs.mockResolvedValue({
            inputs: [
                { name: '$LimitIndex', description: 'Double', lazy: false, value: 0.05 },
                { name: 'MaxLimit', description: 'Integer', lazy: false, value: 5000 },
            ],
            result: { name: 'return', description: 'Double', lazy: false, value: 250000 },
            cell: 'C7',
        })
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ uri: 'uOwner', name: 'BankRatingCalculation',
                steps: [{ ref: 'S9', label: '$Value$Limit', cell: 'C7', status: 'executed' }]})],
            selectedFrameIndex: 0,
            variables: variables(),
            variablesLoading: false,
            simpleFocus: { ref: 'S9', label: '$Value$Limit', ownerUri: 'uOwner', ownerInstance: 0 },
        })
        render(<TraceDetails />)

        expect(screen.queryByText('$Value$Limit')).toBeNull() // no duplicated title in the business view
        // The step's inputs arrive from the step-inputs endpoint, named as the formula writes them.
        expect(getStepInputs).toHaveBeenCalledWith('p1', 0, 'S9')
        expect(await screen.findByText('$LimitIndex')).toBeInTheDocument()
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

    it('highlights a focused cell from the step-inputs payload when the live outline omits it', async () => {
        // A static/description cell is absent from the frame's live step outline; its A1 address comes from
        // the step-inputs payload, so the click still points the traced table at the right cell — and the
        // frame's heavy variables are never needed (null here, as the store skips them for a focused step).
        const getStepInputs = traceService.getStepInputs as ReturnType<typeof vi.fn>
        getStepInputs.mockResolvedValue({
            inputs: [],
            result: { name: 'return', description: 'String', lazy: false, value: 'Bank Rating Group' },
            cell: 'A5',
        })
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ uri: 'uOwner', steps: [{ ref: 'S7', label: '$Description', status: 'executed' }]})],
            selectedFrameIndex: 0,
            variables: null,
            variablesLoading: false,
            simpleFocus: { ref: 'S7', label: '$Description', ownerUri: 'uOwner', ownerInstance: 0 },
        })
        render(<TraceDetails />)

        await waitFor(() => expect(screen.getByTestId('stub-table')).toHaveTextContent('table:0:A5'))
    })

    it('drops the previous step cell while the next step loads, so no wrong cell is highlighted', async () => {
        // Switching to a second static-cell step: until its fetch resolves there is no live cell to fall back
        // to, so the previous step's cell must not linger and light the wrong cell in the traced table.
        const getStepInputs = traceService.getStepInputs as ReturnType<typeof vi.fn>
        let resolveSecond: (value: unknown) => void = () => {}
        getStepInputs.mockImplementation((_p: string, _i: number, ref: string) =>
            ref === 'S7'
                ? Promise.resolve({ inputs: [], result: null, cell: 'A5' })
                : new Promise(res => { resolveSecond = res }))
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ uri: 'uOwner', steps: [
                { ref: 'S7', label: '$D1', status: 'executed' },
                { ref: 'S8', label: '$D2', status: 'executed' },
            ]})],
            selectedFrameIndex: 0,
            variables: null,
            variablesLoading: false,
            simpleFocus: { ref: 'S7', label: '$D1', ownerUri: 'uOwner', ownerInstance: 0 },
        })
        render(<TraceDetails />)
        await waitFor(() => expect(screen.getByTestId('stub-table')).toHaveTextContent('table:0:A5'))

        // Focus the second step; its step-inputs fetch is still pending.
        act(() => useTraceStore.setState({
            simpleFocus: { ref: 'S8', label: '$D2', ownerUri: 'uOwner', ownerInstance: 0 },
        }))
        // During the loading window the traced table highlights nothing — never the previous step's A5.
        await waitFor(() => expect(screen.getByTestId('stub-table')).toHaveTextContent('table:0:none'))

        await act(async () => { resolveSecond({ inputs: [], result: null, cell: 'B9' }) })
        await waitFor(() => expect(screen.getByTestId('stub-table')).toHaveTextContent('table:0:B9'))
    })

    it('shows no result for a focused step that has not produced a value', async () => {
        // The step-inputs payload carries no result (a formula cell that has not run yet); the panel resolves
        // to the empty-result state once it arrives.
        const getStepInputs = traceService.getStepInputs as ReturnType<typeof vi.fn>
        getStepInputs.mockResolvedValue({ inputs: [], result: null, cell: null })
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ uri: 'uOwner',
                steps: [{ ref: 'S9', label: '$Value$Limit', cell: 'C7', status: 'pending' }]})],
            selectedFrameIndex: 0,
            variables: null,
            variablesLoading: false,
            simpleFocus: { ref: 'S9', label: '$Value$Limit', ownerUri: 'uOwner', ownerInstance: 0 },
        })
        render(<TraceDetails />)

        expect(await screen.findByText('details.noResult')).toBeInTheDocument()
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

    it('lists frame errors in Result\'s place when the selected frame reported messages', () => {
        // A failed frame has no return value — Errors replace the empty Result block above the traced table,
        // in both the business and advanced views.
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame()],
            selectedFrameIndex: 0,
            variables: variables({
                result: null,
                errors: [{ severity: 'ERROR', summary: 'Division by zero' }],
            }),
            variablesLoading: false,
        })
        render(<TraceDetails />)
        expect(screen.getByText('details.errors')).toBeInTheDocument()
        expect(screen.getByText('Division by zero')).toBeInTheDocument()
        // Result's title is rendered as "details.result:" — absent when Errors take its place.
        expect(screen.queryByText('details.result:')).toBeNull()
        expect(screen.queryByText('details.noResult')).toBeNull()
    })

    it('shows the error on the focused step the run failed on, from its step-inputs', async () => {
        // The failing step carries its own error in the step-inputs payload, so clicking it explains why it
        // failed — not only the whole table's frame — the way the advanced view shows the error on both.
        // Errors sit where Result would, above the traced table.
        const getStepInputs = traceService.getStepInputs as ReturnType<typeof vi.fn>
        getStepInputs.mockResolvedValue({
            inputs: [{ name: '$AllEmployeesWithSelectedClass', description: 'Employee[]', lazy: false, value: []}],
            result: null,
            cell: 'C9',
            errors: [{ severity: 'ERROR', summary: 'There are no employee records' }],
        })
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame({ uri: 'uOwner', name: 'ValidatePlanNumberForSuppCoverage',
                steps: [{ ref: 'S3', label: '$Validation', cell: 'C9', status: 'executed' }]})],
            selectedFrameIndex: 0,
            variables: variables(),
            variablesLoading: false,
            simpleFocus: { ref: 'S3', label: '$Validation', ownerUri: 'uOwner', ownerInstance: 0 },
        })
        render(<TraceDetails />)

        expect(getStepInputs).toHaveBeenCalledWith('p1', 0, 'S3')
        expect(await screen.findByText('There are no employee records')).toBeInTheDocument()
        expect(screen.getByText('details.errors')).toBeInTheDocument()
        expect(screen.queryByText('details.result:')).toBeNull()
    })

    it('shows frame errors in Result\'s place in the advanced view too', () => {
        useTraceStore.setState({
            status: 'suspended',
            frames: [frame()],
            selectedFrameIndex: 0,
            variables: variables({
                result: null,
                errors: [{ severity: 'ERROR', summary: 'Division by zero' }],
            }),
            variablesLoading: false,
            advanced: true,
        })
        render(<TraceDetails />)
        expect(screen.getByText('details.errors')).toBeInTheDocument()
        expect(screen.getByText('Division by zero')).toBeInTheDocument()
        expect(screen.queryByText('details.result:')).toBeNull()
    })
})
