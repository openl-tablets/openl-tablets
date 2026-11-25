import { renderHook } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { useReadAgain } from './useReadAgain'

describe('useReadAgain', () => {
    it('tells the first read of what the key names from every read after it', () => {
        const { result } = renderHook(() => useReadAgain('p1'))

        expect(result.current()).toBe(false)
        expect(result.current()).toBe(true)
        expect(result.current()).toBe(true)
    })

    it('starts over for another key, such as another project', () => {
        const { result, rerender } = renderHook(({ key }) => useReadAgain(key), { initialProps: { key: 'p1' } })
        result.current()

        rerender({ key: 'p2' })

        expect(result.current()).toBe(false)
        expect(result.current()).toBe(true)
    })
})
