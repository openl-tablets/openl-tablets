import {
    buildLevelTreeData,
    buildValueTreeData,
    complexValueSummary,
    describeSimpleValue,
    isComplexValue,
    levelKey,
    LINES_PER_STEP,
    segmentsOf,
} from 'components/values/valueTree'
import type { ValueLevel } from 'types/execution'

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

    const policy = { drivers: [{ age: 25 }], vin: 'X' }

    /** A tree opened at the given nodes, with the given number of lines listed under some of them. */
    const reach = (open: string[], listed: [string, number][] = []) => ({
        open: new Set(open),
        listed: new Map(listed),
        renderMore: (key: string, left: number) => `more of ${key}: ${left}`,
    })

    it('builds one node per field and element of every open node, with stable keys', () => {
        const tree = buildValueTreeData({ name: 'policy', value: policy }, title, 'p', reach(['p', 'p-0', 'p-0-0']))

        expect(tree).toMatchObject({ key: 'p', title: 'policy', isLeaf: false })
        const [drivers, vin] = tree.children ?? []
        expect(drivers).toMatchObject({ key: 'p-0', title: 'drivers', isLeaf: false })
        expect(drivers?.children?.[0]).toMatchObject({ key: 'p-0-0', title: '[0]', isLeaf: false })
        expect(drivers?.children?.[0]?.children?.[0]).toMatchObject({ key: 'p-0-0-0', title: 'age', isLeaf: true })
        expect(vin).toMatchObject({ key: 'p-1', title: 'vin', isLeaf: true })
    })

    it('builds a node that is not open without its fields, as one that opens', () => {
        expect(buildValueTreeData({ name: 'policy', value: policy }, title, 'p'))
            .toEqual({ key: 'p', title: 'policy', isLeaf: false })
    })

    it('opens one level at a time', () => {
        const [drivers, vin] = buildValueTreeData({ name: 'policy', value: policy }, title, 'p', reach(['p']))
            .children ?? []

        // The fields of the open node are there; theirs wait for their own node to open.
        expect(drivers).toEqual({ key: 'p-0', title: 'drivers', isLeaf: false })
        expect(vin).toEqual({ key: 'p-1', title: 'vin', isLeaf: true })
    })

    it('lists the lines of an open node a step at a time, with a line that lists more', () => {
        const ages = Array.from({ length: LINES_PER_STEP * 2 + 50 }, (_, index) => index)
        const lines = (listed: [string, number][] = []) =>
            buildValueTreeData({ name: 'ages', value: ages }, title, 'a', reach(['a'], listed)).children ?? []

        const first = lines()
        expect(first).toHaveLength(LINES_PER_STEP + 1)
        expect(first.at(-2)).toMatchObject({ key: `a-${LINES_PER_STEP - 1}`, title: `[${LINES_PER_STEP - 1}]` })
        expect(first.at(-1)).toEqual({ key: 'a-more', title: `more of a: ${LINES_PER_STEP + 50}`, isLeaf: true })

        expect(lines([['a', LINES_PER_STEP * 2]]).at(-1)).toMatchObject({ title: 'more of a: 50' })
        // Every line listed, nothing is left to list.
        const all = lines([['a', LINES_PER_STEP * 3]])
        expect(all).toHaveLength(ages.length)
        expect(all.at(-1)).toMatchObject({ key: `a-${ages.length - 1}` })
    })

    it('titles a value with inner structure by the count of its lines, and keeps what it is known as', () => {
        const summaries = new Map<string, string | undefined>()
        const record = ({ name, summary }: { name: string, summary?: string | undefined }) => {
            summaries.set(name, summary)
            return name
        }

        buildValueTreeData({ name: 'policy', value: policy, summary: 'Policy (X)' }, record, 'p', reach(['p']))

        expect(summaries).toEqual(new Map([
            ['policy', 'Policy (X)'],
            ['drivers', '{1 elements}'],
            ['vin', undefined],
        ]))
    })

    it('makes a leaf of a plain value and of an empty structure', () => {
        expect(buildValueTreeData({ name: 'n', value: 1 }, title)).toEqual({ key: '0', title: 'n', isLeaf: true })
        expect(buildValueTreeData({ name: 'n', value: {} }, title)).toEqual({ key: '0', title: 'n', isLeaf: true })
    })
})

describe('buildLevelTreeData', () => {
    /** Titles every node by its name and, for one with inner structure, what it holds. */
    const title = ({ name, summary }: { name: string, summary?: string | undefined }) =>
        (summary ? `${name} ${summary}` : name)

    const reach = (levels: Record<string, ValueLevel>, failures: Record<string, string> = {}) => ({
        levels: new Map(Object.entries(levels).map(([segments, level]) => [`v ${segments}`, level])),
        failures: new Map(Object.entries(failures).map(([segments, reason]) => [`v ${segments}`, reason])),
        renderMore: (key: string, left: number) => `more of ${key}: ${left}`,
        renderFailure: (key: string, reason: string) => `${key} failed: ${reason}`,
    })

    const driver: ValueLevel = {
        total: 4,
        lines: [
            { name: 'name', segment: 'name', value: 'Sara' },
            { name: 'licenses', segment: 'licenses', size: 2, elements: true },
            { name: 'claims', segment: 'claims', size: 0, elements: true },
        ],
    }

    it('keys a node by the path it is read with', () => {
        const key = levelKey('v', ['drivers', '0'])

        expect(key).toBe('v ["drivers","0"]')
        expect(segmentsOf('v', key)).toEqual(['drivers', '0'])
    })

    it('builds a node that is not read yet as one that opens', () => {
        expect(buildLevelTreeData({ name: 'driver', value: {} }, title, 'v', reach({})))
            .toEqual({ key: 'v []', title: 'driver', isLeaf: false })
    })

    it('builds the lines of a level that is read, and a line that reads the rest', () => {
        const [name, licenses, claims, more] = buildLevelTreeData({ name: 'driver', value: {} }, title, 'v',
            reach({ '[]': driver })).children ?? []

        expect(name).toEqual({ key: 'v ["name"]', title: 'name', isLeaf: true })
        // A value with inner structure opens until it is read; an empty one has nothing to open.
        expect(licenses).toEqual({ key: 'v ["licenses"]', title: 'licenses {2 elements}', isLeaf: false })
        expect(claims).toEqual({ key: 'v ["claims"]', title: 'claims {0 elements}', isLeaf: true })
        expect(more).toEqual({ key: 'v [] more', title: 'more of v []: 1', isLeaf: true })
    })

    it('says why a level could not be read, after the lines read before', () => {
        const lines = buildLevelTreeData({ name: 'driver', value: {} }, title, 'v',
            reach({ '[]': driver }, { '[]': 'The project is being compiled' })).children ?? []

        expect(lines).toHaveLength(4)
        expect(lines.at(-1)).toEqual({
            key: 'v [] failed',
            title: 'v [] failed: The project is being compiled',
            isLeaf: true,
        })
    })
})
