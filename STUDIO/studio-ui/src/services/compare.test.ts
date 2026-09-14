import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall from './apiCall'
import {
    comparisonStatusTopic,
    dropComparison,
    getComparison,
    getComparisonTable,
    startFileComparison,
} from './compare'

vi.mock('./apiCall', async (importOriginal) => {
    const original = await importOriginal<typeof import('./apiCall')>()
    return { ...original, default: vi.fn() }
})

const OPTIONS = { throwError: true, suppressErrorPages: true }

describe('compare service', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('sends the two files to compare and answers with the comparison', async () => {
        vi.mocked(apiCall).mockResolvedValue({ id: 'cmp-1' })
        const first = new File(['first'], 'first.xlsx')
        const second = new File(['second'], 'second.xlsx')

        await expect(startFileComparison(first, second)).resolves.toBe('cmp-1')

        const call = vi.mocked(apiCall).mock.calls[0]
        expect(call?.[0]).toBe('/compare/files')
        expect(call?.[1]?.method).toBe('POST')
        const body = call?.[1]?.body as FormData
        expect((body.get('file1') as File).name).toBe('first.xlsx')
        expect((body.get('file2') as File).name).toBe('second.xlsx')
    })

    it('reads what the comparison found', async () => {
        vi.mocked(apiCall).mockResolvedValue({ id: 'cmp 1', identical: true, sheets: []})

        await getComparison('cmp 1')

        expect(apiCall).toHaveBeenCalledWith('/compare/cmp%201', undefined, OPTIONS)
    })

    it('reads one table of the comparison', async () => {
        vi.mocked(apiCall).mockResolvedValue({ id: '0-1', name: 'Rules', status: 'changed' })

        await getComparisonTable('cmp-1', '0-1')

        expect(apiCall).toHaveBeenCalledWith('/compare/cmp-1/tables/0-1', undefined, OPTIONS)
    })

    it('releases the comparison', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await dropComparison('cmp-1')

        expect(apiCall).toHaveBeenCalledWith('/compare/cmp-1', { method: 'DELETE' }, OPTIONS)
    })

    it('names the topic the comparison reports its progress on', () => {
        expect(comparisonStatusTopic('cmp 1')).toBe('/user/topic/compare/cmp%201/status')
    })
})
