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

// Root uR with three steps: S1 called uA twice, S2 made one call (uB), S3 called nothing.
// All sub-calls are already downloaded into simpleChildren — the component never fetches.
const sampleTree = () => callNode('uR', 0, [
    { ref: 'S1', label: '$Value$S1', status: 'executed', childrenTotal: 2 },
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
        simpleLoadedCount: 0,
        simpleTotalCount: null,
        simpleSelectedKey: null,
        simpleInspect: inspect,
        status: 'completed',
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

    it('shows the calculation progress while the run executes', () => {
        setStore({ simpleTree: null, simpleReady: false, simpleLoading: true, status: 'running' })
        render(<SimpleTraceTree />)

        expect(screen.getByTestId('simple-tree-progress')).toHaveTextContent('simple.calculating')
    })

    it('shows the download progress while the tree is prepared', () => {
        setStore({ simpleReady: false, simpleLoading: true, simpleLoadedCount: 5, simpleTotalCount: 9 })
        render(<SimpleTraceTree />)

        expect(screen.getByTestId('simple-tree-progress')).toHaveTextContent('simple.preparing')
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
