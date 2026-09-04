import { renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { useLoadGeneration } from './useLoadGeneration'

describe('useLoadGeneration', () => {
    it('gives the newest reload the user waits for both the answer and the spinner', () => {
        const { result } = renderHook(() => useLoadGeneration())

        const first = result.current.start(false).generation
        const second = result.current.start(false).generation

        expect(result.current.isLatest(first)).toBe(false)
        expect(result.current.ownsSpinner(first)).toBe(false)
        expect(result.current.isLatest(second)).toBe(true)
        expect(result.current.ownsSpinner(second)).toBe(true)
    })

    it('keeps the spinner with the reload that put it up when a quiet one starts behind it', () => {
        const { result } = renderHook(() => useLoadGeneration())

        // The user acts and waits; the backend then echoes that action and a quiet re-read starts.
        const visible = result.current.start(false).generation
        const quiet = result.current.start(true).generation

        // The quiet one wins the answer, but the spinner is still the visible one's to hide.
        expect(result.current.isLatest(quiet)).toBe(true)
        expect(result.current.ownsSpinner(visible)).toBe(true)
        expect(result.current.ownsSpinner(quiet)).toBe(false)
    })

    it('gives a quiet reload no spinner at all when nothing else is waiting', () => {
        const { result } = renderHook(() => useLoadGeneration())

        expect(result.current.ownsSpinner(result.current.start(true).generation)).toBe(false)
    })

    it('keeps one identity across renders, so a memoised loader is not rebuilt', () => {
        const { result, rerender } = renderHook(() => useLoadGeneration())
        const first = result.current

        rerender()

        expect(result.current).toBe(first)
    })
})
