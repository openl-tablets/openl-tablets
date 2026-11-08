import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fetchMessageKeys, fetchMessages } from '../../services/localization'
import type { MessagesPage } from '../../types/localization'
import { NO_FILTER } from './messageFilter'
import { PAGE_SIZE, useMessageKey, useMessageKeys, useMessagePages } from './useMessagePages'

vi.mock('../../services/localization', () => ({
    fetchMessages: vi.fn(),
    fetchMessageKeys: vi.fn(),
}))

// The server leaves out an empty list, so a project without localization files answers no languages at all.
const NO_FILES = { content: [], pageNumber: 0, pageSize: 250, numberOfElements: 0, total: 0 }

const rowsOf = (from: number, count: number) =>
    Array.from({ length: count }, (_, at) => ({ key: `key.${from + at}`, values: { '': `Text ${from + at}` } }))

const pageOf = (page: number, total: number): MessagesPage => {
    const content = rowsOf(page * PAGE_SIZE, Math.min(PAGE_SIZE, total - page * PAGE_SIZE))
    return { languages: [{ tag: '' }], content, pageNumber: page, pageSize: PAGE_SIZE, numberOfElements: content.length, total }
}

/** A read the test answers when it chooses. */
const deferred = <T, >() => {
    let resolve: (value: T) => void = () => undefined
    const promise = new Promise<T>(done => {
        resolve = done
    })
    return { promise, resolve }
}

describe('useMessagePages', () => {
    beforeEach(() => {
        vi.mocked(fetchMessages).mockResolvedValue(NO_FILES)
    })

    it('reads a project without localization files as one without languages', async () => {
        const { result } = renderHook(() => useMessagePages('p1', NO_FILTER, 0, true))
        await waitFor(() => expect(result.current.languages).toEqual([]))
        expect(result.current.rows).toEqual([])
        expect(result.current.loading).toBe(false)
    })

    it('reads the next page when asked, until every key is read', async () => {
        vi.mocked(fetchMessages).mockImplementation((_, __, { page }) => Promise.resolve(pageOf(page, PAGE_SIZE + 3)))
        const { result } = renderHook(() => useMessagePages('p1', NO_FILTER, 0, true))
        await waitFor(() => expect(result.current.rows).toHaveLength(PAGE_SIZE))

        act(() => result.current.loadMore())
        await waitFor(() => expect(result.current.rows).toHaveLength(PAGE_SIZE + 3))
        expect(result.current.rows.at(-1)?.key).toBe(`key.${PAGE_SIZE + 2}`)
        expect(fetchMessages).toHaveBeenLastCalledWith('p1', NO_FILTER, { page: 1, size: PAGE_SIZE })

        act(() => result.current.loadMore())
        expect(fetchMessages).toHaveBeenCalledTimes(2)
    })

    it('takes a page shorter than the size as the last one, whatever the total said', async () => {
        // Keys were added elsewhere since the first page was counted: the total says more than the page holds.
        vi.mocked(fetchMessages).mockResolvedValue({ ...pageOf(0, 3), total: PAGE_SIZE + 3 })
        const { result } = renderHook(() => useMessagePages('p1', NO_FILTER, 0, true))
        await waitFor(() => expect(result.current.rows).toHaveLength(3))

        act(() => result.current.loadMore())

        // Nothing more is read, and no key is shown twice.
        expect(fetchMessages).toHaveBeenCalledTimes(1)
        expect(result.current.total).toBe(3)
        expect(result.current.rows.map(row => row.key)).toEqual(['key.0', 'key.1', 'key.2'])
    })

    it('asks for one page at a time', async () => {
        vi.mocked(fetchMessages).mockResolvedValueOnce(pageOf(0, PAGE_SIZE * 3))
        const { result } = renderHook(() => useMessagePages('p1', NO_FILTER, 0, true))
        await waitFor(() => expect(result.current.rows).toHaveLength(PAGE_SIZE))
        const next = deferred<MessagesPage>()
        vi.mocked(fetchMessages).mockReturnValueOnce(next.promise)

        act(() => result.current.loadMore())
        act(() => result.current.loadMore())
        expect(fetchMessages).toHaveBeenCalledTimes(2)
        await act(async () => next.resolve(pageOf(1, PAGE_SIZE * 3)))
        expect(result.current.rows).toHaveLength(PAGE_SIZE * 2)
    })

    it('drops a page of a query the reader has left', async () => {
        const late = deferred<MessagesPage>()
        vi.mocked(fetchMessages).mockReturnValueOnce(late.promise).mockResolvedValueOnce(pageOf(0, 2))
        const { result, rerender } = renderHook(({ query }) => useMessagePages('p1', query, 0, true),
            { initialProps: { query: NO_FILTER } })

        rerender({ query: { ...NO_FILTER, search: 'key' } })
        await waitFor(() => expect(result.current.rows).toHaveLength(2))
        await act(async () => late.resolve(pageOf(0, 9)))

        expect(result.current.total).toBe(2)
        expect(result.current.rows).toHaveLength(2)
    })

    it('says why the keys could not be read', async () => {
        vi.mocked(fetchMessages).mockRejectedValue(new Error('The project is not opened.'))
        const { result } = renderHook(() => useMessagePages('p1', NO_FILTER, 0, true))

        await waitFor(() => expect(result.current.failure).toBe('The project is not opened.'))
        expect(result.current.loading).toBe(false)
    })

    it('reads nothing while it is not asked to', () => {
        renderHook(() => useMessagePages('p1', NO_FILTER, 0, false))
        expect(fetchMessages).not.toHaveBeenCalled()
    })
})

describe('useMessageKey', () => {
    beforeEach(() => {
        vi.mocked(fetchMessages).mockResolvedValue(NO_FILES)
    })

    it('reads a key of a project without localization files as one without languages', async () => {
        const { result } = renderHook(() => useMessageKey('p1', 'greeting', 0, true))
        await waitFor(() => expect(result.current.languages).toEqual([]))
        expect(result.current.row).toBeNull()
    })

    it('reads the key alone, and forgets it for another key until that one is read', async () => {
        vi.mocked(fetchMessages).mockResolvedValueOnce(pageOf(0, 1))
        const { result, rerender } = renderHook(({ key }) => useMessageKey('p1', key, 0, true),
            { initialProps: { key: 'key.0' } })
        await waitFor(() => expect(result.current.row?.key).toBe('key.0'))
        expect(fetchMessages).toHaveBeenCalledWith('p1', NO_FILTER, { key: 'key.0', page: 0, size: 1 })

        vi.mocked(fetchMessages).mockReturnValueOnce(new Promise(() => undefined))
        rerender({ key: 'other' })
        expect(result.current.row).toBeUndefined()
    })

    it('says why the key could not be read', async () => {
        vi.mocked(fetchMessages).mockRejectedValue(new Error('Gone'))
        const { result } = renderHook(() => useMessageKey('p1', 'greeting', 0, true))
        await waitFor(() => expect(result.current.failure).toBe('Gone'))
        expect(result.current.row).toBeNull()
    })
})

describe('useMessageKeys', () => {
    it('reads the keys of the tree, and lists none it could not read', async () => {
        vi.mocked(fetchMessageKeys).mockResolvedValueOnce(['greeting']).mockRejectedValueOnce(new Error('Gone'))
        const { result, rerender } = renderHook(({ token }) => useMessageKeys('p1', token, true),
            { initialProps: { token: 0 } })
        await waitFor(() => expect(result.current).toEqual(['greeting']))

        rerender({ token: 1 })
        await waitFor(() => expect(result.current).toEqual([]))
    })
})
