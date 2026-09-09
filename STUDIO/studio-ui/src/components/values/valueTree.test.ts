import { buildValueTreeData, complexValueSummary, describeSimpleValue, isComplexValue } from 'components/values/valueTree'

describe('describeSimpleValue', () => {
    it('shows every kind of plain value the way a debugger does', () => {
        expect(describeSimpleValue(null)).toEqual({ display: 'null', kind: 'null' })
        expect(describeSimpleValue(undefined)).toEqual({ display: 'undefined', kind: 'null' })
        expect(describeSimpleValue('Auto')).toEqual({ display: '"Auto"', kind: 'string' })
        expect(describeSimpleValue(25)).toEqual({ display: '25', kind: 'number' })
        expect(describeSimpleValue(false)).toEqual({ display: 'false', kind: 'boolean' })
        expect(describeSimpleValue(BigInt(7))).toEqual({ display: '7', kind: 'other' })
    })
})

describe('isComplexValue / complexValueSummary', () => {
    it('tells values with inner structure apart and counts what they hold', () => {
        expect(isComplexValue({ a: 1 })).toBe(true)
        expect(isComplexValue([])).toBe(true)
        expect(isComplexValue(null)).toBe(false)
        expect(isComplexValue('text')).toBe(false)
        expect(complexValueSummary({ a: 1, b: 2 })).toBe('{2 fields}')
        expect(complexValueSummary([1, 2, 3])).toBe('{3 elements}')
    })
})

describe('buildValueTreeData', () => {
    const title = ({ name }: { name: string }) => name

    it('builds one node per field and element, recursively, with stable keys', () => {
        const tree = buildValueTreeData({ name: 'policy', value: { drivers: [{ age: 25 }], vin: 'X' } }, title, 'p')

        expect(tree).toMatchObject({ key: 'p', title: 'policy', isLeaf: false })
        const [drivers, vin] = tree.children ?? []
        expect(drivers).toMatchObject({ key: 'p-0', title: 'drivers', isLeaf: false })
        expect(drivers?.children?.[0]).toMatchObject({ key: 'p-0-0', title: '[0]', isLeaf: false })
        expect(drivers?.children?.[0]?.children?.[0]).toMatchObject({ key: 'p-0-0-0', title: 'age', isLeaf: true })
        expect(vin).toMatchObject({ key: 'p-1', title: 'vin', isLeaf: true })
    })

    it('makes a leaf of a plain value and of an empty structure', () => {
        expect(buildValueTreeData({ name: 'n', value: 1 }, title)).toEqual({ key: '0', title: 'n', isLeaf: true })
        expect(buildValueTreeData({ name: 'n', value: {} }, title)).toEqual({ key: '0', title: 'n', isLeaf: true })
    })
})
