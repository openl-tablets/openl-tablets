import React from 'react'
import { render, screen, act } from '@testing-library/react'
import { describe, it, expect, beforeEach } from 'vitest'
import { LoadingOverlay } from './LoadingOverlay'
import { useAppStore } from 'store'

describe('LoadingOverlay', () => {
    beforeEach(() => {
        useAppStore.setState({ loaderCount: 0 })
    })

    it('renders nothing while no operation is running', () => {
        render(<LoadingOverlay />)

        expect(screen.queryByTestId('loading-overlay')).not.toBeInTheDocument()
    })

    it('opens on showLoader and closes on the paired hideLoader', () => {
        render(<LoadingOverlay />)

        act(() => useAppStore.getState().showLoader())
        expect(screen.getByTestId('loading-overlay')).toBeInTheDocument()

        act(() => useAppStore.getState().hideLoader())
        expect(screen.queryByTestId('loading-overlay')).not.toBeInTheDocument()
    })

    it('stays open until the last concurrent operation finishes', () => {
        render(<LoadingOverlay />)

        act(() => {
            useAppStore.getState().showLoader()
            useAppStore.getState().showLoader()
        })
        act(() => useAppStore.getState().hideLoader())
        expect(screen.getByTestId('loading-overlay')).toBeInTheDocument()

        act(() => useAppStore.getState().hideLoader())
        expect(screen.queryByTestId('loading-overlay')).not.toBeInTheDocument()
    })

    it('ignores an unpaired hideLoader', () => {
        render(<LoadingOverlay />)

        act(() => useAppStore.getState().hideLoader())
        expect(screen.queryByTestId('loading-overlay')).not.toBeInTheDocument()

        // The count did not go negative: a single show still opens the overlay.
        act(() => useAppStore.getState().showLoader())
        expect(screen.getByTestId('loading-overlay')).toBeInTheDocument()
    })
})
