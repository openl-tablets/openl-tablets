import { describe, expect, it } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { DEFAULT_VIEW, TABLE_VIEWS, treeOf } from './tableGrouping'

const table = (name: string, extra: Partial<ModuleTable> = {}): ModuleTable => ({
    id: `id-${name}`,
    name,
    kind: 'Rules',
    tableType: 'SimpleRules',
    sheet: 'Rules',
    ...extra,
})

describe('tableGrouping', () => {
    it('opens on the view the engine itself defaults to', () => {
        // rules.tree.view.default = excelSheet
        expect(DEFAULT_VIEW).toEqual('excelSheet')
        expect(TABLE_VIEWS).toContain('excelSheet')
    })

    it('gathers the tables by the sheet they are written on', () => {
        const nodes = treeOf(
            [table('Premium'), table('Policy', { sheet: 'Data' }), table('Rate')],
            'excelSheet'
        )

        expect(nodes.map(node => node.title)).toEqual(['Data', 'Rules'])
        expect(nodes[1]?.children.map(child => child.title)).toEqual(['Premium', 'Rate'])
    })

    it('gathers the tables by their family in the type view', () => {
        const nodes = treeOf([table('Premium'), table('Policy', { kind: 'Datatype' })], 'type')

        expect(nodes.map(node => node.title)).toEqual(['Datatype', 'Rules'])
    })

    it('reads a category in steps, and the inversed view reads the steps the other way', () => {
        const tables = [
            table('Premium', { properties: { category: 'Auto.Pricing' } }),
            table('Discount', { properties: { category: 'Auto.Discounts' } }),
        ]

        const detailed = treeOf(tables, 'categoryDetailed')
        expect(detailed.map(node => node.title)).toEqual(['Auto'])
        expect(detailed[0]?.children.map(child => child.title)).toEqual(['Discounts', 'Pricing'])

        const inversed = treeOf(tables, 'categoryInversed')
        expect(inversed.map(node => node.title)).toEqual(['Discounts', 'Pricing'])
    })

    it('keeps a table that carries no value for the level beside the groups, not hidden under one', () => {
        const nodes = treeOf(
            [table('Premium', { properties: { category: 'Pricing' } }), table('Loose')],
            'category'
        )

        expect(nodes.map(node => node.title)).toEqual(['Pricing', 'Loose'])
        // The one without a category is a table in its own right, not an empty group.
        expect(nodes[1]?.table?.name).toEqual('Loose')
    })

    it('gives every node a key of its own, so two tables of the same name never collide', () => {
        const nodes = treeOf(
            [table('Premium', { id: 'first' }), table('Premium', { id: 'second', sheet: 'Data' })],
            'excelSheet'
        )
        const keys = nodes.flatMap(node => [node.key, ...node.children.map(child => child.key)])

        expect(new Set(keys).size).toEqual(keys.length)
    })

    it('lists the tables by name when the view groups by nothing they carry', () => {
        const nodes = treeOf([table('beta', { sheet: '' }), table('Alpha', { sheet: '' })], 'excelSheet')

        expect(nodes.map(node => node.title)).toEqual(['Alpha', 'beta'])
        expect(nodes.every(node => node.table !== undefined)).toBe(true)
    })
})
