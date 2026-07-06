import React from 'react'
import { render, screen } from '@testing-library/react'
import { ErrorBoundary } from './ErrorBoundary'

const { logError } = vi.hoisted(() => ({ logError: vi.fn() }))

vi.mock('utils/errorHandling', () => ({
    errorHandler: { logError },
}))

const Boom = (): React.ReactElement => {
    throw new Error('boom')
}

describe('ErrorBoundary', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('renders children when there is no error', () => {
        render(
            <ErrorBoundary>
                <div data-testid="child" />
            </ErrorBoundary>,
            { onCaughtError: () => {} }
        )
        expect(screen.getByTestId('child')).toBeInTheDocument()
    })

    it('reports the caught error and shows the fallback UI', () => {
        render(
            <ErrorBoundary>
                <Boom />
            </ErrorBoundary>,
            { onCaughtError: () => {} }
        )

        expect(screen.getByText('Something went wrong')).toBeInTheDocument()
        expect(logError).toHaveBeenCalledTimes(1)
        expect(logError.mock.calls[0]?.[0]).toBeInstanceOf(Error)
        expect((logError.mock.calls[0]?.[0] as Error).message).toBe('boom')
    })

    it('renders a custom fallback when provided', () => {
        render(
            <ErrorBoundary fallback={<div data-testid="custom-fallback" />}>
                <Boom />
            </ErrorBoundary>,
            { onCaughtError: () => {} }
        )
        expect(screen.getByTestId('custom-fallback')).toBeInTheDocument()
    })
})
