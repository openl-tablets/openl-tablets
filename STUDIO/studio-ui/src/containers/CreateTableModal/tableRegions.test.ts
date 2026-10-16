import type { RawTableRegion, RawTableRole } from 'types/tables'
import {
    buildTableHeader,
    buildTableRegions,
    buildTableSource,
    initialRows,
    TABLE_PRESETS,
    type TableBuildContext,
    type TableCellValue,
    type TablePreset,
} from './tableSkeletons'

const context = (overrides: Partial<TableBuildContext> = {}): TableBuildContext => ({
    resultType: 'Boolean',
    resultFields: [],
    arguments: [{ type: 'Integer', name: 'age' }, { type: 'String', name: 'state' }],
    vocabularyType: 'String',
    extendsType: '',
    datatypeName: 'Customer',
    dataFields: [{ name: 'name', type: 'String' }, { name: 'age', type: 'Integer' }],
    targetName: 'Eligibility',
    targetColumns: [
        { name: 'age', title: 'Age', type: 'Integer' },
        { name: '_res_', title: 'Result', type: 'Boolean' },
    ],
    vocabularyValues: {},
    ...overrides,
})

/** The table a preset builds from its example rows, and the regions its builder emits for it. */
const built = (preset: TablePreset, over: Partial<TableBuildContext> = {}, transposed = false) => {
    const ctx = context(over)
    const source = buildTableSource(preset, buildTableHeader(preset, 'Table', ctx), ctx, initialRows(preset, ctx),
        transposed)
    return { source, regions: buildTableRegions(preset, ctx, source, transposed) }
}

const roleAt = (regions: RawTableRegion[], row: number, column: number): RawTableRole | undefined =>
    regions.find(region => region.top <= row && row <= region.bottom && region.left <= column
        && column <= region.right)?.role

const width = (source: TableCellValue[][]): number => Math.max(...source.map(row => row.length))

describe('buildTableRegions', () => {
    it.each(TABLE_PRESETS.filter(preset => preset !== 'freeForm'))(
        'covers every cell of a %s table once, inside the table',
        preset => {
            const { source, regions } = built(preset)
            for (let row = 0; row < source.length; row++) {
                for (let column = 0; column < width(source); column++) {
                    const holding = regions.filter(region => region.top <= row && row <= region.bottom
                        && region.left <= column && column <= region.right)
                    expect(holding, `${preset} ${row}:${column}`).toHaveLength(1)
                }
            }
            expect(roleAt(regions, 0, 0)).toBe('signature')
        })

    it('gives a free-form table no roles: OpenL reads it as of the type Other', () => {
        expect(built('freeForm').regions).toEqual([])
    })

    it('lays out a Datatype by its titles, types, the fields it gives and values', () => {
        const { regions } = built('datatype')

        expect(roleAt(regions, 1, 0)).toBe('inputTitle')
        expect(roleAt(regions, 2, 0)).toBe('code')
        expect(roleAt(regions, 2, 1)).toBe('output')
    })

    it('lays out the conditions and what a SmartRules table returns', () => {
        const { regions } = built('smartRules')

        expect(roleAt(regions, 1, 0)).toBe('inputTitle')
        expect(roleAt(regions, 1, 1)).toBe('inputTitle')
        expect(roleAt(regions, 1, 2)).toBe('outputTitle')
        expect(roleAt(regions, 2, 1)).toBe('value')
        expect(roleAt(regions, 2, 2)).toBe('output')
    })

    it('lays out a lookup of one argument as a rules table that opens with a rule', () => {
        const { regions } = built('smartLookup', { arguments: [{ type: 'Integer', name: 'age' }]})

        expect(roleAt(regions, 1, 0)).toBe('value')
        expect(roleAt(regions, 1, 1)).toBe('output')
    })

    it('titles a lookup by its conditions in the corner and its horizontal values over the grid it returns', () => {
        const { regions } = built('simpleLookup')

        expect(roleAt(regions, 1, 0)).toBe('inputTitle')
        expect(roleAt(regions, 1, 1)).toBe('outputTitle')
        expect(roleAt(regions, 2, 0)).toBe('value')
        expect(roleAt(regions, 2, 1)).toBe('output')
    })

    it('gives each row of the horizontal values of a lookup a region of its own', () => {
        const { regions } = built('simpleLookup', { arguments: [
            { type: 'String', name: 'plan' }, { type: 'String', name: 'term' },
            { type: 'String', name: 'size' }, { type: 'String', name: 'region' },
        ]})

        const titles = regions.filter(region => region.role === 'outputTitle')
        expect(titles.map(region => [region.top, region.bottom])).toEqual([[1, 1], [2, 2]])
    })

    it('lays out the code of a Rules table above its titles', () => {
        const { regions } = built('rules')

        expect(roleAt(regions, 1, 0)).toBe('code')
        expect(roleAt(regions, 3, 1)).toBe('code')
        expect(roleAt(regions, 4, 1)).toBe('outputTitle')
        expect(roleAt(regions, 5, 0)).toBe('value')
    })

    it('returns the last step of a Spreadsheet of one value, and no step of a SpreadsheetResult', () => {
        const single = built('spreadsheet', { resultType: 'Double' })
        expect(roleAt(single.regions, 1, 0)).toBe('inputTitle')
        expect(roleAt(single.regions, 1, 1)).toBe('outputTitle')
        expect(roleAt(single.regions, 2, 0)).toBe('result')

        const all = built('spreadsheet', { resultType: 'SpreadsheetResult' })
        expect(roleAt(all.regions, 2, 0)).toBe('value')
        expect(roleAt(all.regions, 2, 1)).toBe('output')
    })

    it('lays out the field names, the titles and the expected result of a Test table, either way round', () => {
        const upright = built('test')
        expect(roleAt(upright.regions, 1, 0)).toBe('code')
        expect(roleAt(upright.regions, 2, 0)).toBe('inputTitle')
        expect(roleAt(upright.regions, 2, 1)).toBe('outputTitle')
        expect(roleAt(upright.regions, 3, 0)).toBe('value')
        expect(roleAt(upright.regions, 3, 1)).toBe('output')

        const turned = built('test', {}, true)
        expect(roleAt(turned.regions, 1, 0)).toBe('code')
        expect(roleAt(turned.regions, 2, 1)).toBe('outputTitle')
        expect(roleAt(turned.regions, 2, 2)).toBe('output')
    })

    it('keys a Data table by its first field', () => {
        const { regions } = built('data')

        expect(roleAt(regions, 3, 0)).toBe('key')
        expect(roleAt(regions, 3, 1)).toBe('value')
    })

    it('names a setting of an Environment table and a property of a Properties table as keys', () => {
        expect(roleAt(built('environment').regions, 1, 0)).toBe('key')
        expect(roleAt(built('properties').regions, 1, 1)).toBe('value')
        expect(roleAt(built('constants').regions, 1, 0)).toBe('code')
        expect(roleAt(built('constants').regions, 1, 1)).toBe('output')
        expect(roleAt(built('vocabulary').regions, 1, 0)).toBe('value')
    })
})
