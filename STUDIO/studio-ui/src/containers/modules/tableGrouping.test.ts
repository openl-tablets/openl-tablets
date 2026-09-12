import { beforeEach, describe, expect, it } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { DEFAULT_VIEW, loadView, saveView, TABLE_VIEWS, treeOf, widthOf } from './tableGrouping'

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

    describe('the versions of one table', () => {
        const carPrice = (lob: string, extra: Partial<ModuleTable> = {}): ModuleTable => table('CarPrice', {
            id: `car-${lob}`,
            displayName: `CarPrice [lob=${lob}]`,
            overloadGroup: 'CarPrice(java.lang.String)',
            ...extra,
        })

        it('gathers them under the name they share, each called by what tells it apart', () => {
            const nodes = treeOf([carPrice('Insurance'), carPrice('Banking'), table('Premium')], 'excelSheet')
            const sheet = nodes[0]

            expect(sheet?.title).toEqual('Rules')
            expect(sheet?.children.map(child => child.title)).toEqual(['CarPrice', 'Premium'])
            expect(sheet?.children[0]?.children.map(child => child.title))
                .toEqual(['CarPrice [lob=Banking]', 'CarPrice [lob=Insurance]'])
        })

        it('keeps two tables that merely share a name apart', () => {
            // Another CarPrice, taking another argument: its versions are its own, not this one's.
            const namesake = (id: string, lob: string): ModuleTable => table('CarPrice', {
                id,
                displayName: `CarPrice [lob=${lob}]`,
                overloadGroup: 'CarPrice(java.lang.Integer)',
            })
            const tables = [carPrice('Banking'), carPrice('Insurance'), namesake('by-age', 'Retail')]
            const nodes = treeOf([...tables, namesake('by-year', 'Trade')], 'excelSheet')

            expect(nodes[0]?.children.map(child => child.children.length)).toEqual([2, 2])
        })

        it('files a version under the folder even where the branch holds only that one', () => {
            // The other version is written on another sheet, so this branch gathers a single one.
            const nodes = treeOf([carPrice('Banking'), carPrice('Insurance', { sheet: 'More' })], 'excelSheet')

            expect(nodes.map(node => node.title)).toEqual(['More', 'Rules'])
            expect(nodes.every(sheet => sheet.children[0]?.title === 'CarPrice')).toBe(true)
            expect(nodes.flatMap(sheet => sheet.children.flatMap(folder => folder.children.map(v => v.title))))
                .toEqual(['CarPrice [lob=Insurance]', 'CarPrice [lob=Banking]'])
        })

        it('gives the folder and its versions keys of their own', () => {
            const nodes = treeOf([carPrice('Banking'), carPrice('Insurance')], 'excelSheet')
            const folder = nodes[0]?.children[0]
            const keys = [folder?.key, ...(folder?.children.map(child => child.key) ?? [])]

            expect(new Set(keys).size).toEqual(keys.length)
        })
    })

    describe('the view the tree opens on', () => {
        beforeEach(() => localStorage.clear())

        it('follows the Default Order of the user\'s own settings when this browser has chosen none', () => {
            expect(loadView('type')).toEqual('type')
        })

        it('falls back to the engine\'s own default when the settings name no view it knows', () => {
            expect(loadView(undefined)).toEqual(DEFAULT_VIEW)
            expect(loadView('whatever the settings hold')).toEqual(DEFAULT_VIEW)
        })

        it('keeps what this browser chose, which was chosen later than the settings were written', () => {
            saveView('category')

            expect(loadView('type')).toEqual('category')
        })
    })

    describe('how wide the tree scrolls', () => {
        it('reckons the width from the longest name, deep in the tree', () => {
            const shallow = treeOf([table('Premium')], 'excelSheet')
            const deep = treeOf([table('A name far longer than the others', { sheet: 'Rules' })], 'excelSheet')

            expect(widthOf(deep)).toBeGreaterThan(widthOf(shallow))
            // A row is wider than its own indent and chrome, whatever it is called.
            expect(widthOf(shallow)).toBeGreaterThan(56)
        })

        it('is nothing to scroll when there is nothing to show', () => {
            expect(widthOf([])).toEqual(0)
        })
    })
})
