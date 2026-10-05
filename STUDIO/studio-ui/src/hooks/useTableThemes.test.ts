import { renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getTableThemes, getTableThemesOf } from '../services/tables'
import { errorHandler } from '../utils/errorHandling'
import { offeredTheme, toThemeOptions, useTableThemes, useTableThemesOf } from './useTableThemes'

vi.mock('../services/tables', () => ({ getTableThemes: vi.fn(), getTableThemesOf: vi.fn() }))
vi.mock('../utils/errorHandling', () => ({ errorHandler: { logError: vi.fn() } }))

const THEMES = [{ id: 'default', name: 'Default' }, { id: 'green', name: 'Green' }]

describe('useTableThemes', () => {
    beforeEach(() => {
        vi.mocked(errorHandler.logError).mockClear()
    })

    it('knows no theme until the list arrives, then offers the themes Studio has', async () => {
        vi.mocked(getTableThemes).mockResolvedValue(THEMES)

        const { result } = renderHook(() => useTableThemes())

        expect(result.current).toBeUndefined()
        await waitFor(() => expect(result.current).toEqual(THEMES))
    })

    it('offers no theme when the list cannot be read, and keeps the reason', async () => {
        vi.mocked(getTableThemes).mockRejectedValue(new Error('down'))

        const { result } = renderHook(() => useTableThemes())

        await waitFor(() => expect(result.current).toEqual([]))
        expect(errorHandler.logError).toHaveBeenCalledWith(expect.objectContaining({ message: 'down' }),
            { message: 'The table themes cannot be read' })
    })
})

describe('useTableThemesOf', () => {
    beforeEach(() => {
        vi.mocked(errorHandler.logError).mockClear()
    })

    it('asks nothing until it is asked, then offers the themes the server says suit the table', async () => {
        vi.mocked(getTableThemesOf).mockResolvedValue(THEMES)

        const { result, rerender } = renderHook(({ asked }) => useTableThemesOf('p1', 't1', 'Main', asked),
            { initialProps: { asked: false } })

        expect(result.current).toBeUndefined()
        expect(getTableThemesOf).not.toHaveBeenCalled()

        rerender({ asked: true })

        await waitFor(() => expect(result.current).toEqual(THEMES))
        expect(getTableThemesOf).toHaveBeenCalledWith('p1', 't1', 'Main')
    })

    it('asks once per table, and again for another table', async () => {
        vi.mocked(getTableThemesOf).mockResolvedValue(THEMES)

        const { result, rerender } = renderHook(({ tableId, asked }) => useTableThemesOf('p1', tableId, 'Main', asked),
            { initialProps: { tableId: 't1', asked: true } })
        await waitFor(() => expect(result.current).toEqual(THEMES))

        // Stopping and starting again keeps the answer the table already has.
        rerender({ tableId: 't1', asked: false })
        rerender({ tableId: 't1', asked: true })
        expect(result.current).toEqual(THEMES)
        expect(getTableThemesOf).toHaveBeenCalledTimes(1)

        const green = [{ id: 'green', name: 'Green' }]
        vi.mocked(getTableThemesOf).mockResolvedValue(green)
        rerender({ tableId: 't2', asked: true })

        // The themes of the previous table are not offered for the next one.
        expect(result.current).toBeUndefined()
        await waitFor(() => expect(result.current).toEqual(green))
        expect(getTableThemesOf).toHaveBeenLastCalledWith('p1', 't2', 'Main')
    })

    it('offers no theme when the themes of the table cannot be read, and keeps the reason', async () => {
        vi.mocked(getTableThemesOf).mockRejectedValue(new Error('down'))

        const { result } = renderHook(() => useTableThemesOf('p1', 't1', undefined, true))

        await waitFor(() => expect(errorHandler.logError).toHaveBeenCalledWith(
            expect.objectContaining({ message: 'down' }), { message: 'The table themes of the table cannot be read' }))
        expect(result.current).toBeUndefined()
    })
})

describe('toThemeOptions', () => {
    it('offers each theme by its identifier under its name, and nothing while no theme is known', () => {
        expect(toThemeOptions(THEMES)).toEqual([
            { value: 'default', label: 'Default' },
            { value: 'green', label: 'Green' },
        ])
        expect(toThemeOptions(undefined)).toEqual([])
    })
})

describe('offeredTheme', () => {
    it('names the theme the settings name while Studio offers it, and none otherwise', () => {
        expect(offeredTheme(THEMES, 'green')).toBe('green')
        expect(offeredTheme(THEMES, 'removed')).toBeUndefined()
        expect(offeredTheme(THEMES, undefined)).toBeUndefined()
        expect(offeredTheme(undefined, 'green')).toBeUndefined()
    })
})
