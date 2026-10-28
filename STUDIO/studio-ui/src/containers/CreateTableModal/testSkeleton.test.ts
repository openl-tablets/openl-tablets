import type { DatatypeField, ProjectTable } from 'types/tables'
import {
    buildTargetStructure,
    canTargetTable,
    expandArguments,
    parseArguments,
    targetTableName,
} from './testSkeleton'

const table = (overrides: Partial<ProjectTable> = {}): ProjectTable => ({
    id: 'table-id',
    tableType: 'SimpleRules',
    name: 'Premium',
    returnType: 'Double',
    signature: 'Premium(Policy policy)',
    ...overrides,
})

const datatypes: Record<string, DatatypeField[]> = {
    Policy: [{ name: 'number', type: 'String' }, { name: 'mainDriver', type: 'Driver' }],
    Driver: [{ name: 'age', type: 'Integer' }, { name: 'policy', type: 'Policy' }],
}

const fieldsOf = (typeName: string): DatatypeField[] | null => datatypes[typeName] ?? null

describe('parseArguments', () => {
    it('reads the arguments out of the signature the header declares', () => {
        expect(parseArguments('Premium(Policy policy, Integer age)')).toEqual([
            { type: 'Policy', name: 'policy' },
            { type: 'Integer', name: 'age' },
        ])
    })

    it('reads a signature however its author spaced it', () => {
        expect(parseArguments('Hello (  Integer   hour )')).toEqual([{ type: 'Integer', name: 'hour' }])
        expect(parseArguments('Rate(Integer[] years)')).toEqual([{ type: 'Integer[]', name: 'years' }])
    })

    it('declares nothing for a table that takes nothing, or whose header could not be read', () => {
        expect(parseArguments('Greeting()')).toEqual([])
        expect(parseArguments(undefined)).toEqual([])
        expect(parseArguments('Greeting')).toEqual([])
        // A parameter written without a name is not one OpenL can bind, so there is no column to fill it from.
        expect(parseArguments('Rate(Integer)')).toEqual([])
    })
})

describe('expandArguments', () => {
    it('gives every nested field of a tested argument its own column', () => {
        // One column per value the call needs, addressed by the path OpenL reads it back with. A datatype already
        // on the path is not opened again: it would describe an endless chain of columns.
        expect(expandArguments(parseArguments('Premium(Policy policy)'), fieldsOf)).toEqual([
            { name: 'policy.number', title: 'Policy Number', type: 'String' },
            { name: 'policy.mainDriver.age', title: 'Policy Main Driver Age', type: 'Integer' },
            { name: 'policy.mainDriver.policy', title: 'Policy Main Driver Policy', type: 'Policy' },
        ])
    })

    it('keeps a value no datatype describes in one column', () => {
        expect(expandArguments(parseArguments('Rate(String make, Driver[] drivers)'), fieldsOf))
            .toEqual([
                { name: 'make', title: 'Make', type: 'String' },
                // A collection is filled from several rows of the same test case, not from a column per element.
                { name: 'drivers', title: 'Drivers', type: 'Driver[]' },
            ])
    })

    it('opens a datatype again in every argument it is nested in', () => {
        // Only a datatype already on the path stops the walk: another argument of the same type has columns of its own.
        expect(expandArguments(parseArguments('Premium(Policy first, Policy second)'), fieldsOf)
            .map(column => column.name)).toEqual([
            'first.number', 'first.mainDriver.age', 'first.mainDriver.policy',
            'second.number', 'second.mainDriver.age', 'second.mainDriver.policy',
        ])
    })
})

describe('buildTargetStructure', () => {
    it('ends a Test table with the column its result is compared against', () => {
        const structure = buildTargetStructure(table(), 'test', fieldsOf)

        expect(structure.table.name).toBe('Premium')
        expect(structure.columns.at(-1)).toEqual({ name: '_res_', title: 'Result', type: 'Double' })
    })

    it('leaves a Run table without one, because it only calls', () => {
        const structure = buildTargetStructure(table(), 'run', fieldsOf)

        expect(structure.columns.map(column => column.name))
            .toEqual(['policy.number', 'policy.mainDriver.age', 'policy.mainDriver.policy'])
    })

    it('names the generated table after the table it exercises and what it does to it', () => {
        expect(targetTableName('Premium', 'test')).toBe('PremiumTest')
        expect(targetTableName('Premium', 'run')).toBe('PremiumRun')
    })

    it('leaves a table returning nothing to a Run table, which has nothing to assert', () => {
        const returningNothing = table({ returnType: 'void' })

        expect(canTargetTable(table(), 'test')).toBe(true)
        expect(canTargetTable(returningNothing, 'test')).toBe(false)
        expect(canTargetTable(returningNothing, 'run')).toBe(true)
    })
})
