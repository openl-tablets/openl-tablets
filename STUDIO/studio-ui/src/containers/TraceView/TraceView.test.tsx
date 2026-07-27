import { render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import type { DebugStatus } from 'types/trace'
import TraceView from './TraceView'

// A single mutable store snapshot the selector-based useTraceStore reads from.
const { store } = vi.hoisted(() => ({
    store: {
        current: {} as Record<string, unknown>,
    },
}))

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

vi.mock('react-router-dom', () => ({
    useParams: () => ({ projectId: 'p1' }),
    useSearchParams: () => [new URLSearchParams({ tableId: 't1' })],
}))

vi.mock('store', () => ({
    useTraceStore: (selector: (s: Record<string, unknown>) => unknown) => selector(store.current),
}))

// The panels and background hooks are irrelevant to the terminal-banner/status-tag behaviour under test.
vi.mock('./components/DebugToolbar', () => ({ default: () => <div data-testid="debug-toolbar" /> }))
vi.mock('./components/DebugCallStack', () => ({ default: () => null }))
vi.mock('./components/TraceTree', () => ({ default: () => null }))
vi.mock('./components/HotspotsPanel', () => ({ default: () => null }))
vi.mock('./components/BreakpointsPanel', () => ({ default: () => null }))
vi.mock('./components/WatchPanel', () => ({ default: () => null }))
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
        ...extra,
    }
}

describe('TraceView terminal outcome', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('shows the Finished status tag but no banner on a clean finish', () => {
        setStore('completed')
        render(<TraceView />)

        expect(screen.getByTestId('debug-status')).toHaveTextContent('debug.status.completed')
        // A clean finish is not an alarm — the tag alone says Finished.
        expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    })

    it('shows a warning banner when the run is stopped before it finishes', () => {
        setStore('terminated')
        render(<TraceView />)

        expect(screen.getByTestId('debug-status')).toHaveTextContent('debug.status.terminated')
        expect(screen.getByRole('alert')).toBeInTheDocument()
    })

    it('shows the failure summary in the banner when the run errors', () => {
        setStore('error', { debugError: { summary: 'Division by zero' } })
        render(<TraceView />)

        expect(screen.getByTestId('debug-status')).toHaveTextContent('debug.status.error')
        expect(screen.getByRole('alert')).toHaveTextContent('Division by zero')
    })
})
