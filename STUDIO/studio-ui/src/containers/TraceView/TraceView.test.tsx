import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { DebugStatus } from 'types/trace'
import TraceView from './TraceView'

// A single mutable store snapshot the selector-based useTraceStore reads from, plus the launch URL params
// (the trace mode is read from `advanced` there, chosen at launch on the JSF page).
const { store, search } = vi.hoisted(() => ({
    store: { current: {} as Record<string, unknown> },
    search: { current: new URLSearchParams({ tableId: 't1' }) },
}))

const launchWith = (advanced?: boolean): void => {
    search.current = new URLSearchParams({ tableId: 't1', ...(advanced ? { advanced: 'true' } : {}) })
}

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('react-router-dom', () => ({
    useParams: () => ({ projectId: 'p1' }),
    useSearchParams: () => [search.current],
}))

vi.mock('store', () => ({
    useTraceStore: (selector: (s: Record<string, unknown>) => unknown) => selector(store.current),
}))

// The panels and background hooks are irrelevant to the mode-gating/banner/status-tag behaviour under test.
vi.mock('./components/DebugToolbar', () => ({ default: () => <div data-testid="debug-toolbar" /> }))
vi.mock('./components/DebugCallStack', () => ({ default: () => null }))
vi.mock('./components/TraceTree', () => ({ default: () => null }))
vi.mock('./components/SimpleTraceTree', () => ({ default: () => <div data-testid="simple-tree" /> }))
vi.mock('./components/HotspotsPanel', () => ({ default: () => null }))
vi.mock('./components/BreakpointsPanel', () => ({ default: () => <div data-testid="breakpoints-panel" /> }))
vi.mock('./components/WatchPanel', () => ({ default: () => <div data-testid="watch-panel" /> }))
vi.mock('./components/TraceDetails', () => ({ default: () => null }))
vi.mock('./hooks/useTraceProgress', () => ({ default: () => {} }))
vi.mock('./hooks/useTerminateOnClose', () => ({ default: () => {} }))

const setStore = (status: DebugStatus, extra: Record<string, unknown> = {}): void => {
    store.current = {
        setRouteParams: vi.fn(),
        start: vi.fn(),
        loadBreakpoints: vi.fn(),
        reset: vi.fn(),
        status,
        debugError: null,
        error: null,
        profiling: false,
        advanced: false,
        simpleRun: vi.fn(),
        simpleLoading: false,
        simpleReady: false,
        // Read by the real TraceToolbar rendered in the left column.
        loading: false,
        showDetailed: false,
        setShowDetailed: vi.fn(),
        setProfiling: vi.fn(),
        ...extra,
    }
}

describe('TraceView terminal outcome', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        launchWith() // default: launched into the business view
    })

    it('shows no status pill and no banner in the business view on a clean finish', () => {
        setStore('completed')
        render(<TraceView />)

        // The business view carries no status pills — the finished tree is the result, a "Finished" tag is noise.
        expect(screen.queryByTestId('debug-status')).not.toBeInTheDocument()
        expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })

    it('shows the Finished status tag but no banner in the advanced view on a clean finish', () => {
        setStore('completed', { advanced: true })
        render(<TraceView />)

        expect(screen.getByTestId('debug-status')).toHaveTextContent('debug.status.completed')
        // A clean finish is not an alarm — the tag alone says Finished.
        expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })

    it('keeps a stop silent in the simple view — inspections restart the session as a matter of course', () => {
        setStore('terminated')
        render(<TraceView />)

        expect(screen.queryByTestId('debug-status')).not.toBeInTheDocument()
        expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })

    it('shows a warning banner in the advanced mode when the run is stopped before it finishes', () => {
        setStore('terminated', { advanced: true })
        render(<TraceView />)

        expect(screen.getByTestId('debug-status')).toHaveTextContent('debug.status.terminated')
        expect(screen.getByRole('alert')).toBeInTheDocument()
    })

    it('surfaces a run failure through the banner in the business view, without a status pill', () => {
        setStore('error', { debugError: { summary: 'Division by zero' } })
        render(<TraceView />)

        // No status pill in the business view, but a real failure still surfaces — through the banner.
        expect(screen.queryByTestId('debug-status')).not.toBeInTheDocument()
        expect(screen.getByRole('alert')).toHaveTextContent('Division by zero')
    })
})

describe('TraceView mode gating', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        launchWith() // default: launched into the business view
    })

    it('opens straight into the business view and runs it — no Run button, no Advanced switch', () => {
        // The mode is fixed at launch, so the business view runs on open: there is no Run button to press
        // and no switch to change the mode. Its only control, Show detailed view, lives in the tree panel.
        setStore('running', { simpleLoading: true })
        render(<TraceView />)

        expect(store.current['simpleRun']).toHaveBeenCalled()
        expect(screen.getByTestId('simple-tree')).toBeInTheDocument()
        expect(screen.queryByTestId('simple-run')).not.toBeInTheDocument()
        expect(screen.queryByTestId('trace-advanced')).not.toBeInTheDocument()
        expect(screen.queryByTestId('debug-toolbar')).not.toBeInTheDocument()
        expect(screen.queryByTestId('debug-status')).not.toBeInTheDocument()
    })

    it('opens the advanced debugger when launched advanced, attaching to the session — no Advanced switch', () => {
        setStore('suspended', { advanced: true })
        launchWith(true)
        render(<TraceView />)

        // Advanced attaches to the launch session; it does not auto-run the business tree.
        expect(store.current['start']).toHaveBeenCalled()
        expect(store.current['simpleRun']).not.toHaveBeenCalled()
        expect(screen.getByTestId('debug-toolbar')).toBeInTheDocument()
        expect(screen.getByTestId('breakpoints-panel')).toBeInTheDocument()
        expect(screen.getByTestId('watch-panel')).toBeInTheDocument()
        expect(screen.queryByTestId('trace-advanced')).not.toBeInTheDocument()
        expect(screen.queryByTestId('simple-run')).not.toBeInTheDocument()
        expect(screen.getByTestId('debug-status')).toHaveTextContent('debug.status.suspended')
    })
})
