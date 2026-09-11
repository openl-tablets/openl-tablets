import { describe, expect, it } from 'vitest'
import type { ModuleTable } from 'types/tables'
import {
    activeLevels,
    buildTableTree,
    DEFAULT_GROUPING,
    GROUP_BY_CATEGORY,
    GROUP_BY_KIND,
    GROUP_BY_NONE,
    GROUP_BY_TABLE_TYPE,
} from './tableGrouping'

const table = (name: string, extra: Partial<ModuleTable> = {}): ModuleTable => ({
    id: `id-${name}`,
    name,
    kind: 'Rules',
    tableType: 'SimpleRules',
    ...extra,
})

describe('tableGrouping', () => {
    it('lists the tables by name when nothing groups them', () => {
        const nodes = buildTableTree([table('beta'), table('Alpha')], [])

        expect(nodes.map(node => node.title)).toEqual(['Alpha', 'beta'])
        expect(nodes.every(node => node.table !== undefined)).toBe(true)
    })

    it('gathers the tables by the level asked for', () => {
        const nodes = buildTableTree(
            [table('Premium'), table('Policy', { kind: 'Datatype' }), table('Rate')],
            [GROUP_BY_KIND]
        )

        expect(nodes.map(node => node.title)).toEqual(['Datatype', 'Rules'])
        expect(nodes[1]?.children.map(child => child.title)).toEqual(['Premium', 'Rate'])
    })

    it('groups one level inside another', () => {
        const nodes = buildTableTree(
            [
                table('Premium', { properties: { category: 'Pricing' }}),
                table('Discount', { properties: { category: 'Pricing' }, tableType: 'SmartRules' }),
            ],
            [GROUP_BY_CATEGORY, GROUP_BY_TABLE_TYPE]
        )

        expect(nodes.map(node => node.title)).toEqual(['Pricing'])
        expect(nodes[0]?.children.map(child => child.title)).toEqual(['SimpleRules', 'SmartRules'])
    })

    it('keeps a table that carries no value for the level beside the groups, not hidden under one', () => {
        const nodes = buildTableTree(
            [table('Premium', { properties: { category: 'Pricing' }}), table('Loose')],
            [GROUP_BY_CATEGORY]
        )

        expect(nodes.map(node => node.title)).toEqual(['Pricing', 'Loose'])
        // The one without a category is a table in its own right, not an empty group.
        expect(nodes[1]?.table?.name).toEqual('Loose')
    })

    it('gives every node a key of its own, so two tables of the same name never collide', () => {
        const nodes = buildTableTree(
            [table('Premium', { id: 'first' }), table('Premium', { id: 'second', kind: 'Datatype' })],
            [GROUP_BY_KIND]
        )
        const keys = nodes.flatMap(node => [node.key, ...node.children.map(child => child.key)])

        expect(new Set(keys).size).toEqual(keys.length)
    })

    it('counts only the levels that group', () => {
        expect(activeLevels([GROUP_BY_KIND, GROUP_BY_NONE])).toEqual([GROUP_BY_KIND])
        expect(activeLevels([GROUP_BY_NONE, GROUP_BY_NONE])).toEqual([])
        expect(activeLevels(DEFAULT_GROUPING)).toEqual([GROUP_BY_KIND])
    })
})
