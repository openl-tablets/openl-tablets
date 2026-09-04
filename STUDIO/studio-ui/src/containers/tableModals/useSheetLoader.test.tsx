import { act, renderHook } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { useSheetLoader } from './useSheetLoader'
import { getModuleSheets } from 'services/projects'

vi.mock('services/projects', () => ({ getModuleSheets: vi.fn() }))

vi.mock('antd', () => ({ notification: { error: vi.fn() } }))

const MODULES = [{ name: 'Main' }]

describe('useSheetLoader', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('stops the spinner when sheets are handed over while a request is still in flight', async () => {
        let answer: ((sheets: string[]) => void) | undefined
        vi.mocked(getModuleSheets).mockReturnValue(new Promise<string[]>(resolve => { answer = resolve }) as never)
        const { result } = renderHook(() => useSheetLoader('error'))

        act(() => void result.current.load('p1', 'Main', MODULES))
        expect(result.current.loading).toBe(true)

        // The caller already knows the sheets - the answer still on its way is nobody's answer now.
        act(() => result.current.prime('Main', ['Sheet1']))

        expect(result.current.loading).toBe(false)
        expect(result.current.sheets).toEqual(['Sheet1'])

        // The overtaken reply changes nothing when it finally lands.
        await act(async () => {
            answer?.(['Stale'])
            await Promise.resolve()
        })
        expect(result.current.loading).toBe(false)
        expect(result.current.sheets).toEqual(['Sheet1'])
    })
})
