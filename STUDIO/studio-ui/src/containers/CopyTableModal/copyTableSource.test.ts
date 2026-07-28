import type { RawTable } from 'types/tables'
import { buildCopySource, readTableProperties } from './copyTableSource'

describe('copyTableSource', () => {
    it('reads a merged property section and keeps typed cell values', () => {
        expect(readTableProperties([
            [{ value: 'Rules Boolean Quote()' }],
            [{ value: 'properties', rowspan: 2 }, { value: 'active' }, { value: false }],
            [{ value: null, covered: true }, { value: 'priority' }, { value: 10 }],
            [{ value: 'C1' }],
        ])).toEqual([
            { name: 'active', value: false },
            { name: 'priority', value: 10 },
        ])
    })

    it('renames the last matching header token and replaces the old properties', () => {
        const table: RawTable = {
            tableType: 'RawSource',
            kind: 'Rules',
            name: 'Quote',
            source: [
                [{ value: 'Rules Quote Quote(Quote input)', colspan: 3 }, { value: null, covered: true },
                    { value: null, covered: true }],
                [{ value: 'properties' }, { value: 'version' }, { value: '1.0.0' }],
                [{ value: 'C1' }, { value: 'RET1' }, { value: null }],
            ],
        }

        expect(buildCopySource(table, 'QuoteCopy', [{ name: 'lob', value: 'Auto' }])).toEqual([
            [
                { value: 'Rules Quote QuoteCopy(Quote input)', colspan: 3 },
                { value: null, covered: true },
                { value: null, covered: true },
            ],
            [{ value: 'properties' }, { value: 'lob' }, { value: 'Auto' }],
            [{ value: 'C1' }, { value: 'RET1' }, { value: null }],
        ])
    })

    it('keeps a body row that opens with the word Properties', () => {
        // OpenL reads the section marker case-sensitively, so this row is data — not a properties block to drop.
        const source = [
            [{ value: 'Data Person persons' }, { value: null }],
            [{ value: 'Properties' }, { value: 'name' }],
            [{ value: 'x' }, { value: 'John' }],
        ]

        expect(readTableProperties(source)).toEqual([])
        expect(buildCopySource({ tableType: 'RawSource', kind: 'Data', name: 'persons', source }, 'clients', []))
            .toEqual([
                [{ value: 'Data Person clients', colspan: 2 }, { value: null, covered: true }],
                [{ value: 'Properties' }, { value: 'name' }],
                [{ value: 'x' }, { value: 'John' }],
            ])
    })

    it('names an unnamed table instead of overwriting its keyword', () => {
        // A table written without a name is reported under its keyword, which the copy must keep.
        const table: RawTable = {
            tableType: 'RawSource',
            kind: 'Constants',
            name: 'Constants',
            source: [[{ value: 'Constants' }], [{ value: 'Integer' }, { value: 'MAX' }, { value: '10' }]],
        }

        expect(buildCopySource(table, 'Limits', [])[0]?.[0]).toEqual({ value: 'Constants Limits', colspan: 3 })
    })

    it('takes the whole header as the name of a free-form table', () => {
        const table: RawTable = {
            tableType: 'RawSource',
            kind: 'Other',
            name: 'Some free text longer than the fifty-seven characters a name is cut to...',
            source: [[{ value: 'Some free text longer than the fifty-seven characters a name is cut to and beyond' }]],
        }

        expect(buildCopySource(table, 'A note', [])).toEqual([[{ value: 'A note' }]])
    })

    it('widens a narrow table when properties are added', () => {
        const table: RawTable = {
            tableType: 'RawSource',
            kind: 'Datatype',
            name: 'Customer',
            source: [
                [{ value: 'Datatype Customer' }],
                [{ value: 'String' }],
            ],
        }

        expect(buildCopySource(table, 'Client', [{ name: 'active', value: true }])).toEqual([
            [
                { value: 'Datatype Client', colspan: 3 },
                { value: null, covered: true },
                { value: null, covered: true },
            ],
            [{ value: 'properties' }, { value: 'active' }, { value: true }],
            [{ value: 'String' }, { value: null }, { value: null }],
        ])
    })
})
