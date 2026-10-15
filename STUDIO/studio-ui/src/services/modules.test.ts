import apiCall from 'services/apiCall'
import { cancelModuleCompilation, getModuleTables, getRawTable, startModuleCompilation } from 'services/modules'
import type { MockedFunction } from 'vitest'

vi.mock('services/apiCall', async () => {
    const actual = await vi.importActual<typeof import('services/apiCall')>('services/apiCall')
    return { __esModule: true, ...actual, default: vi.fn() }
})

describe('modules service', () => {
    const mockApiCall = apiCall as MockedFunction<typeof apiCall>

    beforeEach(() => {
        vi.clearAllMocks()
        mockApiCall.mockResolvedValue(null)
    })

    it('asks for a module to be compiled, and says when it is to be built again', async () => {
        await startModuleCompilation('p1', 'Bank Rating')
        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/p1/modules/Bank%20Rating/compile', { method: 'POST' }, expect.anything())

        await startModuleCompilation('p1', 'Bank Rating', true)
        expect(mockApiCall).toHaveBeenLastCalledWith(
            '/projects/p1/modules/Bank%20Rating/compile?reset=true', { method: 'POST' }, expect.anything())
    })

    it('tells a compilation to stop', async () => {
        await cancelModuleCompilation('p1', 'Bank Rating')

        expect(mockApiCall).toHaveBeenCalledWith(
            '/projects/p1/modules/Bank%20Rating/compile', { method: 'DELETE' }, expect.anything())
    })

    it('lists the tables of a module, the free-form ones only when they are asked for', async () => {
        await getModuleTables('p1', 'Bank Rating')

        const [listed] = mockApiCall.mock.calls[0] as [string]
        expect(listed).toContain('/projects/p1/tables?')
        expect(listed).toContain('module=Bank+Rating')
        expect(listed).toContain('unpaged=true')
        // The tree draws a module the way it is written, so it asks for the tables in that order.
        expect(listed).toContain('sort=position')
        // Left out by default, as the Editor's tree hid them: they take no part in the rules.
        expect(listed).not.toContain('includeOther')

        await getModuleTables('p1', 'Bank Rating', { includeOther: true })

        const [withOther] = mockApiCall.mock.calls[1] as [string]
        expect(withOther).toContain('includeOther=true')
    })

    it('reads a table through the module it belongs to, a window at a time', async () => {
        mockApiCall.mockResolvedValue({ id: 'table-1', name: 'Rates', source: []})
        await getRawTable('p1', 'table-1', { module: 'Bank Rating', startRow: 120, maxRows: 120 })

        const [url] = mockApiCall.mock.calls[0] as [string]
        expect(url).toContain('/projects/p1/tables/table-1?')
        // Styles ride with every read; showing formulas or hiding the header is the screen's own choice.
        expect(url).toContain('raw=true')
        expect(url).toContain('styles=true')
        expect(url).toContain('module=Bank+Rating')
        expect(url).toContain('startRow=120')
        expect(url).toContain('maxRows=120')
        expect(url).not.toContain('formulas')
    })

    it('reads the values alone, without the styles the workbook has', async () => {
        mockApiCall.mockResolvedValue({ id: 'table-1', name: 'Rates', source: []})
        await getRawTable('p1', 'table-1', { styles: false })

        const [url] = mockApiCall.mock.calls[0] as [string]
        expect(url).toContain('styles=false')
    })

    it('reads every cell with the style it names, each style kept once', async () => {
        const fill = { background: '#ddebf7' }
        mockApiCall.mockResolvedValue({
            id: 'table-1',
            name: 'Rates',
            styles: [fill, { bold: true }],
            source: [[{ value: 'Datatype Rate', runs: [{ to: 9 }, { style: 1, to: 13 }]}], [{ style: 0 }], [{ style: 0 }]],
        })

        const table = await getRawTable('p1', 'table-1')

        expect(table.source[0]?.[0]?.runs).toEqual([{ text: 'Datatype ' }, { style: { bold: true }, text: 'Rate' }])
        expect(table.source[1]?.[0]?.style).toEqual(fill)
        expect(table.source[2]?.[0]?.style).toBe(table.source[1]?.[0]?.style)
        expect(table).not.toHaveProperty('styles')
    })
})
