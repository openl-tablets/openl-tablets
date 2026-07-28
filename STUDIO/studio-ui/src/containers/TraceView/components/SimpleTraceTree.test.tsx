import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SimpleTraceTree from './SimpleTraceTree'

// A single mutable store snapshot the selector-based useTraceStore reads from.
const { store } = vi.hoisted(() => ({
    store: {
        current: {} as Record<string, unknown>,
    },
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('store', () => ({
    useTraceStore: (selector: (s: Record<string, unknown>) => unknown) => selector(store.current),
    treeChildKey: (uri: string, instance: number, step: string) => JSON.stringify([uri, instance, step]),
}))

const inspect = vi.fn()

const callNode = (uri: string, instance: number, steps: object[] = [], extra: object = {}): object =>
    ({ uri, name: uri, instance, kind: 'spreadsheet', durationMillis: 0, selfMillis: 0, steps, ...extra })

// Root uR: S1 called uA twice, S2 made one call (uB), S3 called nothing; S0 and S0b are static
// description cells. All sub-calls are already downloaded — the component never fetches.
const sampleTree = () => callNode('uR', 0, [
    { ref: 'S0', label: '$Description$S1', status: 'executed', constant: true },
    { ref: 'S1', label: '$Value$S1', status: 'executed', childrenTotal: 2 },
    { ref: 'S0b', label: '$Description$S2', status: 'executed', constant: true },
    { ref: 'S2', label: '$Value$S2', status: 'executed', childrenTotal: 1 },
    { ref: 'S3', label: '$Value$S3', status: 'executed' },
])

const sampleChildren = () => ({
    [JSON.stringify(['uR', 0, 'S1'])]: [
        callNode('uA', 0, [{ ref: 'SA', status: 'executed' }]),
        callNode('uA', 1, [{ ref: 'SA', status: 'executed' }]),
    ],
    [JSON.stringify(['uR', 0, 'S2'])]: [callNode('uB', 0, [{ ref: 'SB', status: 'executed' }])],
})

const setStore = (extra: Record<string, unknown> = {}): void => {
    store.current = {
        simpleTree: sampleTree(),
        simpleChildren: sampleChildren(),
        simpleReady: true,
        simpleLoading: false,
        simpleSelectedKey: null,
        simpleInspect: inspect,
        status: 'completed',
        showDetailed: false,
        setShowDetailed: vi.fn(),
        ...extra,
    }
}

describe('SimpleTraceTree', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('asks to run before a tree exists', () => {
        setStore({ simpleTree: null, simpleReady: false })
        render(<SimpleTraceTree />)

        expect(screen.getByText('simple.pressRun')).toBeInTheDocument()
    })

    it('shows a single calculation progress while the one request runs — no per-page count', () => {
        // One request runs the whole calculation and returns the tree; there is nothing to page, so the
        // meaningless "N of M" line is gone — just that the calculation is running.
        setStore({ simpleTree: null, simpleReady: false, simpleLoading: true })
        render(<SimpleTraceTree />)

        expect(screen.getByTestId('simple-tree-progress')).toHaveTextContent('simple.calculating')
    })

    it('renders the root open with its steps, and deeper branches collapsed', () => {
        setStore()
        render(<SimpleTraceTree />)

        expect(screen.getByTestId('simple-node-tree')).toHaveTextContent('uR')
        expect(screen.getByTestId('simple-step-tree/S1')).toHaveTextContent('$Value$S1')
        // The calls S1 made stay collapsed until expanded.
        expect(screen.queryByTestId('simple-node-tree/S1#0')).not.toBeInTheDocument()
        // No replay buttons anywhere — replay is an advanced-mode feature.
        expect(document.querySelector('button')).toBeNull()
    })

    it('expands a step from the downloaded snapshot, with no backend involved', async () => {
        setStore()
        render(<SimpleTraceTree />)

        await userEvent.click(screen.getByTestId('simple-toggle-tree/S1'))

        expect(screen.getByTestId('simple-node-tree/S1#0')).toHaveTextContent('uA')
        expect(screen.getByTestId('simple-node-tree/S1#1')).toHaveTextContent('uA')
    })

    it('does not badge a looped call with its execution pass — that is an advanced-mode detail', async () => {
        setStore()
        render(<SimpleTraceTree />)

        await userEvent.click(screen.getByTestId('simple-toggle-tree/S1'))

        // uA runs twice; the advanced tree marks the 2nd pass "#2", but the business view stays plain.
        expect(screen.getByTestId('simple-node-tree/S1#1')).not.toHaveTextContent('#2')
    })

    it('marks a capped step with how many executions the full tree omitted', async () => {
        // The one-shot full tree keeps a looped step's first sub-calls inline and reports the full count;
        // the rest read as omitted (the size-cap "not retained" marker), never silently missing.
        const cappedTree = () => callNode('uR', 0, [
            { ref: 'S1', label: '$Value$S1', status: 'executed', childrenTotal: 5, children: [
                callNode('uA', 0, [{ ref: 'SA', status: 'executed' }]),
                callNode('uA', 1, [{ ref: 'SA', status: 'executed' }]),
            ]},
        ])
        setStore({ simpleTree: cappedTree(), simpleChildren: {} })
        render(<SimpleTraceTree />)

        await userEvent.click(screen.getByTestId('simple-toggle-tree/S1'))

        expect(screen.getByTestId('simple-node-tree/S1#0')).toHaveTextContent('uA')
        expect(screen.getByTestId('simple-node-tree/S1#1')).toHaveTextContent('uA')
        // 5 executed − 2 kept inline = 3 omitted, shown as the truncation marker.
        expect(screen.getByText('tree.notRetained')).toBeInTheDocument()
    })

    const decisionTree = () => {
        const dt = callNode('uDT', 0, [
            { ref: 'c0', label: 'Condition: MC1, Rules: [R1]', status: 'executed', decision: 'unmatched' },
            { ref: 'R2', label: 'Returned rule: [R2]', status: 'executed', decision: 'returned' },
        ], { kind: 'decisionTable' })
        return callNode('uR', 0, [{ ref: 'S1', label: '$Value$S1', status: 'executed', children: [dt]}])
    }

    const expandDecisionTable = async (): Promise<void> => {
        await userEvent.click(screen.getByTestId('simple-toggle-tree/S1')) // reveal the DT node
        await userEvent.click(screen.getByTestId('simple-toggle-tree/S1#0')) // expand the DT node
    }

    it('hides a decision table’s conditions in the business view by default, keeping the returned rule', async () => {
        setStore({ simpleTree: decisionTree(), simpleChildren: {} })
        render(<SimpleTraceTree />)

        await expandDecisionTable()

        // Default off — the business view shows only the returned rule, not the per-condition breakdown.
        expect(screen.queryByText('Condition: MC1, Rules: [R1]')).not.toBeInTheDocument()
        expect(screen.getByText('Returned rule: [R2]')).toBeInTheDocument()
    })

    it('inspects the owning decision-table frame when a returned-rule row is clicked, not an empty step', async () => {
        // A DT breakdown row is not a spreadsheet cell — it has no step-inputs. Clicking it must inspect the
        // DT frame (whose Details carry the rule's result), with focus undefined so the frame variables load
        // instead of the empty step-inputs a focus would trigger.
        setStore({ simpleTree: decisionTree(), simpleChildren: {} })
        render(<SimpleTraceTree />)

        await expandDecisionTable()
        await userEvent.click(screen.getByTestId('simple-step-tree/S1#0/R2'))

        expect(inspect).toHaveBeenCalledWith(expect.objectContaining({
            key: 'uDT@0', // the DT frame, not a step focus
            selectionKey: 'tree/S1#0/R2', // the clicked row keeps its own highlight
            frameUri: 'uDT',
            frameInstance: 0,
            stepType: 'out',
        }))
        expect(inspect.mock.calls.at(-1)?.[0]?.focus).toBeUndefined() // frame view, so variables load
    })

    it('reveals a decision table’s conditions when Show detailed trace is on', async () => {
        setStore({ simpleTree: decisionTree(), simpleChildren: {}, showDetailed: true })
        render(<SimpleTraceTree />)

        await expandDecisionTable()

        // The business view still profiles, so the conditions are already in the tree — the toggle shows them
        // as informational rows (a red cross for the unmatched one), never as clickable steps.
        const condition = screen.getByTestId('simple-condition-tree/S1#0/c0')
        expect(condition).toHaveTextContent('Condition: MC1, Rules: [R1]')
        expect(condition.querySelector('[data-icon="close"]')).toBeInTheDocument()
        expect(screen.queryByTestId('simple-step-tree/S1#0/c0')).not.toBeInTheDocument()
        expect(screen.getByText('Returned rule: [R2]')).toBeInTheDocument()
    })

    it('toggles the detailed view from the header checkbox', async () => {
        const setShowDetailed = vi.fn()
        setStore({ setShowDetailed })
        render(<SimpleTraceTree />)

        await userEvent.click(screen.getByTestId('simple-detailed'))

        expect(setShowDetailed).toHaveBeenCalledWith(true)
    })

    it('inspects a call on click: runs it and reads its inputs and result', async () => {
        setStore()
        render(<SimpleTraceTree />)

        await userEvent.click(screen.getByTestId('simple-node-tree'))

        expect(inspect).toHaveBeenCalledWith(expect.objectContaining(
            { key: 'uR@0', frameUri: 'uR', frameInstance: 0, stepType: 'out' }))
        // A call keeps the plain frame view — no step focus.
        expect(inspect.mock.calls[0]?.[0]?.focus).toBeUndefined()
    })

    it('runs a clicked step in place within its owning table, focused on the step', async () => {
        setStore()
        render(<SimpleTraceTree />)

        // Every step — with or without calls of its own — executes within its owner; Details then shows
        // the owner's inputs and the step's value, like the classic trace did for a spreadsheet cell.
        await userEvent.click(screen.getByTestId('simple-step-tree/S2'))

        expect(inspect).toHaveBeenCalledWith(expect.objectContaining({
            key: 'uR#S2@0',
            frameUri: 'uR',
            frameInstance: 0,
            stepType: 'over',
            focus: { ref: 'S2', label: '$Value$S2', ownerUri: 'uR', ownerInstance: 0 },
        }))

        await userEvent.click(screen.getByTestId('simple-step-tree/S3'))

        expect(inspect).toHaveBeenLastCalledWith(expect.objectContaining(
            { key: 'uR#S3@0', stepType: 'over' }))
    })

    // A step whose formula reuses another step's result appears in the tree as a stepRef node.
    const referencingTree = () => callNode('uR', 0, [
        { ref: 'S1', label: '$Value$S1', status: 'executed', childrenTotal: 2 }, // original, called uA twice
        { ref: 'S2', label: '$Value$S2', status: 'executed', children: [
            { uri: 'uR', name: '$Value$S1', instance: 0, kind: 'stepRef',
                durationMillis: 0, selfMillis: 0, steps: [], refStep: 'S1' }, // reuses S1
        ]},
    ])

    it('renders a referenced step inline in the business view — expandable and inspectable in place', async () => {
        // The advanced tree shows a dead REF link to the original; the business view draws the referenced
        // step right here so its subtree and value are one expand/click away, no hunt for the original.
        setStore({ simpleTree: referencingTree() })
        render(<SimpleTraceTree />)

        await userEvent.click(screen.getByTestId('simple-toggle-tree/S2')) // reveal the reference under S2
        const ref = screen.getByTestId('simple-step-tree/S2#0')
        expect(ref).toHaveTextContent('$Value$S1') // drawn as the referenced step, not a bare link
        expect(ref).toHaveTextContent('tree.referenceTag') // still marked as a reference

        // It carries the original's own subtree: expanding it reveals S1's two calls inline — no jump.
        await userEvent.click(screen.getByTestId('simple-toggle-tree/S2#0'))
        expect(screen.getByTestId('simple-node-tree/S2#0#0')).toHaveTextContent('uA')
        expect(screen.getByTestId('simple-node-tree/S2#0#1')).toHaveTextContent('uA')

        // Clicking it inspects the original step in place, with the reference row's own selection key so
        // only this row highlights (not the original occurrence elsewhere).
        await userEvent.click(screen.getByTestId('simple-step-tree/S2#0'))
        expect(inspect).toHaveBeenCalledWith(expect.objectContaining({
            key: 'uR#S1@0',
            selectionKey: 'tree/S2#0',
            frameUri: 'uR',
            stepType: 'over',
            focus: expect.objectContaining({ ref: 'S1', ownerUri: 'uR', ownerInstance: 0 }),
        }))
    })

    it('shows a static cell and reads it by running its owning table through', async () => {
        setStore()
        render(<SimpleTraceTree />)

        // The description cell is a tree row like any step…
        expect(screen.getByTestId('simple-step-tree/S0')).toHaveTextContent('$Description$S1')

        // …but it has no line to run to, so a click runs the whole owning table instead, while the
        // selection key stays this cell's own so only its row highlights.
        await userEvent.click(screen.getByTestId('simple-step-tree/S0'))

        expect(inspect).toHaveBeenCalledWith(expect.objectContaining({
            key: 'uR@0',
            selectionKey: 'uR#S0@0',
            frameUri: 'uR',
            frameInstance: 0,
            stepType: 'out',
            focus: expect.objectContaining({ ref: 'S0', label: '$Description$S1' }),
        }))
    })

    it('highlights only the clicked static cell — not the owner or its sibling static cells', () => {
        // Old bug: a static cell shared the owning table's run key, so selecting one lit up every static
        // cell of the table and the owner node. The selection now keys off the cell's own row.
        setStore({ simpleSelectedKey: 'uR@0' }) // the owner's run key
        const first = render(<SimpleTraceTree />)
        const ownerHighlighted = screen.getByTestId('simple-node-tree').className
        first.unmount()

        setStore({ simpleSelectedKey: 'uR#S0@0' }) // one static cell's own key
        render(<SimpleTraceTree />)
        const owner = screen.getByTestId('simple-node-tree').className
        const clicked = screen.getByTestId('simple-step-tree/S0').className
        const sibling = screen.getByTestId('simple-step-tree/S0b').className

        expect(owner).not.toBe(ownerHighlighted) // the owner is no longer co-highlighted
        expect(clicked).not.toBe(sibling) // only the clicked static cell is highlighted
    })

    it('highlights the row whose values are being shown', async () => {
        setStore({ simpleSelectedKey: 'uA@0' })
        render(<SimpleTraceTree />)
        await userEvent.click(screen.getByTestId('simple-toggle-tree/S1'))

        // Class names are hashed and merged, so the selection reads as the styling difference between
        // twin rows that are identical in every way except that one is selected.
        const selected = screen.getByTestId('simple-node-tree/S1#0')
        const twin = screen.getByTestId('simple-node-tree/S1#1')
        expect(selected.className).not.toBe(twin.className)
    })
})
