import { beforeEach, describe, expect, it, vi } from 'vitest'
import apiCall, { readTaskResult } from './apiCall'
import {
    comparisonStatusTopic,
    dropComparison,
    getConflictFileText,
    getConflictWorkbook,
    getComparison,
    getComparisonTable,
    mergeConflictWorkbook,
    startFileComparison,
} from './compare'
import { jsonResponse } from 'testing/responses'

vi.mock('./apiCall', async (importOriginal) => {
    const original = await importOriginal<typeof import('./apiCall')>()
    return { ...original, default: vi.fn(), readTaskResult: vi.fn() }
})

const OPTIONS = { throwError: true, suppressErrorPages: true }

describe('compare service', () => {
    beforeEach(() => {
        vi.clearAllMocks()
    })

    it('reads a version of a conflicted file not written in UTF-8 with every byte a character of its own', async () => {
        // "Prämie=1" written in ISO-8859-1: the umlaut is one byte, which is no UTF-8.
        vi.mocked(apiCall).mockResolvedValue(new Blob([new Uint8Array([0x50, 0x72, 0xe4, 0x6d, 0x69, 0x65, 0x3d, 0x31])]))

        await expect(getConflictFileText('06YZYX2FFV2RT', 'p/i18n/message.properties', 'theirs'))
            .resolves.toBe('Prämie=1')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/06YZYX2FFV2RT/merge/conflicts/files?file=p%2Fi18n%2Fmessage.properties&side=theirs',
            undefined,
            { ...OPTIONS, responseType: 'blob' }
        )
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

    it('reads what the comparison found, once the comparison has ended', async () => {
        vi.mocked(readTaskResult).mockResolvedValue(jsonResponse({ id: 'cmp 1', identical: true, sheets: []}))

        const comparison = await getComparison('cmp 1')

        expect(comparison.identical).toBe(true)
        expect(readTaskResult).toHaveBeenCalledWith('/compare/cmp%201', undefined, OPTIONS)
    })

    it('reads one table of the comparison', async () => {
        vi.mocked(readTaskResult).mockResolvedValue(jsonResponse({ id: '0-1', name: 'Rules', status: 'changed' }))

        const table = await getComparisonTable('cmp-1', '0-1')

        expect(table.name).toBe('Rules')
        expect(readTaskResult).toHaveBeenCalledWith('/compare/cmp-1/tables/0-1', undefined, OPTIONS)
    })

    it('reads each side of a table with the style every cell names', async () => {
        vi.mocked(readTaskResult).mockResolvedValue(jsonResponse({
            id: '0-1',
            name: 'Rules',
            status: 'changed',
            first: { source: [[{ cell: 'B2', style: 0, value: 'x' }]], styles: [{ bold: true }], changedCells: ['B2']},
        }))

        const table = await getComparisonTable('cmp-1', '0-1')

        expect(table.first).toEqual({ source: [[{ cell: 'B2', style: { bold: true }, value: 'x' }]], changedCells: ['B2']})
        expect(table).not.toHaveProperty('second')
    })

    it('releases the comparison', async () => {
        vi.mocked(apiCall).mockResolvedValue(undefined)

        await dropComparison('cmp-1')

        expect(apiCall).toHaveBeenCalledWith('/compare/cmp-1', { method: 'DELETE' }, OPTIONS)
    })

    it('names the topic the comparison reports its progress on', () => {
        expect(comparisonStatusTopic('cmp 1')).toBe('/user/topic/compare/cmp%201/status')
    })

    it('reads the conflicts of a conflicted workbook, each cell of a version carrying its own style', async () => {
        const version = {
            range: 'A1:A1',
            cells: [[{ cell: 'A1', value: 'Rules rate', style: 0 }]],
            styles: [{ font: { bold: true } }],
            rows: 1,
            changedCells: [],
        }
        vi.mocked(apiCall).mockResolvedValue({
            sheets: [{
                name: 'Rules',
                status: 'conflict',
                ours: true,
                base: true,
                theirs: false,
                tables: [
                    { id: 'Rules/A1', name: 'Rules rate', reason: 'removedAndChanged', ours: version, base: version },
                ],
            }],
            palette: false,
        })

        const workbook = await getConflictWorkbook('06YZYX2FFV2RT', 'p/rules/Rates.xlsx')

        const table = workbook.sheets[0]!.tables[0]!
        expect(table.ours).toEqual({
            range: 'A1:A1',
            cells: [[{ cell: 'A1', value: 'Rules rate', style: { font: { bold: true } } }]],
            rows: 1,
            changedCells: [],
        })
        expect(table.base?.cells).toEqual(table.ours?.cells)
        expect(table).not.toHaveProperty('theirs')
        expect(table).not.toHaveProperty('merged')

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/06YZYX2FFV2RT/merge/conflicts/workbook?file=p%2Frules%2FRates.xlsx',
            undefined,
            OPTIONS)
    })

    it('reads a table merged cell by cell, its cells and the versions of its conflicts carrying their styles',
        async () => {
            const bold = { font: { bold: true } }
            vi.mocked(apiCall).mockResolvedValue({
                sheets: [{
                    name: 'Rules',
                    status: 'conflict',
                    ours: true,
                    base: true,
                    theirs: true,
                    tables: [{
                        id: 'Rules/A1:A2',
                        name: 'Rules rate',
                        reason: 'cells',
                        merged: {
                            range: 'A1:A2',
                            cells: [[{ cell: 'A1', value: 'Rules rate', style: 0 }], [{ cell: 'A2', value: 1 }]],
                            styles: [bold],
                            rows: 2,
                            changedCells: ['A1'],
                            conflicts: [{
                                row: 1,
                                column: 0,
                                cell: 'A2',
                                ourCell: 'A2',
                                theirCell: 'A2',
                                ours: { cell: 'A2', value: 10, style: 0 },
                                theirs: { cell: 'A2', value: 20 },
                            }],
                        },
                    }],
                }],
                palette: false,
            })

            const workbook = await getConflictWorkbook('06YZYX2FFV2RT', 'p/rules/Rates.xlsx')

            expect(workbook.sheets[0]!.tables[0]!.merged).toEqual({
                range: 'A1:A2',
                cells: [[{ cell: 'A1', value: 'Rules rate', style: bold }], [{ cell: 'A2', value: 1 }]],
                rows: 2,
                changedCells: ['A1'],
                conflicts: [{
                    row: 1,
                    column: 0,
                    cell: 'A2',
                    ourCell: 'A2',
                    theirCell: 'A2',
                    ours: { cell: 'A2', value: 10, style: bold },
                    theirs: { cell: 'A2', value: 20 },
                }],
            })
        })

    it('sends the version each conflict of a workbook is taken from and answers with the merged workbook', async () => {
        const merged = new Blob(['merged'])
        vi.mocked(apiCall).mockResolvedValue(merged)

        await expect(mergeConflictWorkbook('06YZYX2FFV2RT', 'p/rules/Rates.xlsx', {
            'Rules/A1:B6': 'theirs',
            'Notes': 'base',
        }, {
            'Rules/A9:C12': { side: 'ours', cells: [{ row: 2, column: 1, value: '1.45' }]},
        })).resolves.toBe(merged)

        expect(apiCall).toHaveBeenCalledWith(
            '/projects/06YZYX2FFV2RT/merge/conflicts/workbook?file=p%2Frules%2FRates.xlsx',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    decisions: [
                        { id: 'Rules/A1:B6', side: 'theirs' },
                        { id: 'Notes', side: 'base' },
                        { id: 'Rules/A9:C12', cells: { side: 'ours', cells: [{ row: 2, column: 1, value: '1.45' }]} },
                    ],
                }),
            },
            { ...OPTIONS, responseType: 'blob', skipWorkspaceEvent: true })
    })
})
