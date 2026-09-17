import { beforeEach, describe, expect, it } from 'vitest'
import type { ModuleTable } from 'types/tables'
import { DEFAULT_VIEW, loadView, saveView, TABLE_VIEWS, treeOf, widthOf } from './tableGrouping'

/** Reads a label as its key, so a test can name the groups by what they are called. */
const key = (name: string): string => name

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
            [table('Premium'), table('Policy', { sheet: 'Data' }), table('Rate')], 'excelSheet', key)

        expect(nodes.map(node => node.title)).toEqual(['Data', 'Rules'])
        expect(nodes[1]?.children.map(child => child.title)).toEqual(['Premium', 'Rate'])
    })

    it('gathers the tables by their family in the type view, in the order and under the names of the Editor', () => {
        const nodes = treeOf([
            table('Policy', { kind: 'Datatype', tableType: 'Datatype' }),
            table('Premium'),
            table('Env', { kind: 'Environment', tableType: 'Environment' }),
            table('CarType', { kind: 'Datatype', tableType: 'Vocabulary' }),
            table('Loose', { kind: 'Future', tableType: 'Future' }),
            table('Total', { kind: 'Spreadsheet', tableType: 'Spreadsheet' }),
        ], 'type', key)

        // The decision tables lead, as the Editor's tree led with them; a vocabulary is a datatype that declares
        // values rather than fields, so it stands in a group of its own, after the datatypes; a family the Editor
        // never knew comes last.
        expect(nodes.map(node => node.title)).toEqual([
            'browser.module.types.decision',
            'browser.module.types.spreadsheet',
            'browser.module.types.datatype',
            'browser.module.types.vocabulary',
            'browser.module.types.configuration',
            'Future',
        ])
        expect(nodes[3]?.children.map(child => child.title)).toEqual(['CarType'])
        expect(nodes[2]?.children.map(child => child.title)).toEqual(['Policy'])
        expect(nodes[0]?.hint).toEqual('browser.module.type_hints.decision')
        expect(nodes[5]?.hint).toBeUndefined()
    })

    it('reads a category in steps between its dashes, and the inversed view reads the steps the other way', () => {
        const tables = [
            table('Premium', { properties: { category: 'Auto-Pricing' } }),
            table('Discount', { properties: { category: 'Auto-Discounts' } }),
        ]

        const detailed = treeOf(tables, 'categoryDetailed', key)
        expect(detailed.map(node => node.title)).toEqual(['Auto'])
        expect(detailed[0]?.children.map(child => child.title)).toEqual(['Discounts', 'Pricing'])

        const inversed = treeOf(tables, 'categoryInversed', key)
        expect(inversed.map(node => node.title)).toEqual(['Discounts', 'Pricing'])
        expect(inversed[0]?.children.map(child => child.title)).toEqual(['Auto'])
    })

    it('files a table that declares no category under the sheet it is written on, as the Editor did', () => {
        const nodes = treeOf(
            [table('Premium', { properties: { category: 'Pricing' } }), table('Loose', { sheet: 'Rating' })],
            'category', key)

        expect(nodes.map(node => node.title)).toEqual(['Pricing', 'Rating'])
        expect(nodes[1]?.children.map(child => child.title)).toEqual(['Loose'])
    })

    it('files a category of one step by that step alone, beside the deeper ones, in the order of their names', () => {
        const tables = [
            table('Total', { sheet: 'Calculation' }),
            table('Score', { sheet: 'Client-Scoring' }),
            table('Age', { sheet: 'Driver-Eligibility' }),
            table('Rate', { sheet: 'Driver-Premium' }),
        ]

        // Detailed: the first step leads; a category of one step holds its tables itself.
        const detailed = treeOf(tables, 'categoryDetailed', key)
        expect(detailed.map(node => node.title)).toEqual(['Calculation', 'Client', 'Driver'])
        expect(detailed[0]?.children.map(child => child.title)).toEqual(['Total'])
        expect(detailed[2]?.children.map(child => child.title)).toEqual(['Eligibility', 'Premium'])

        // Inversed: the second step leads, and a category of one step stands at the root by its only step.
        const inversed = treeOf(tables, 'categoryInversed', key)
        expect(inversed.map(node => node.title)).toEqual(['Calculation', 'Eligibility', 'Premium', 'Scoring'])
        expect(inversed[0]?.children.map(child => child.title)).toEqual(['Total'])
        expect(inversed[3]?.children.map(child => child.title)).toEqual(['Client'])
    })

    it('files a properties table by its scope: the module ones in a folder at the root, a category one inside its category', () => {
        const tables = [
            table('Premium', { sheet: 'Pricing' }),
            table('ModuleDefaults', { kind: 'Properties', tableType: 'Properties', sheet: 'Pricing', properties: { scope: 'Module' } }),
            table('PricingDefaults', { kind: 'Properties', tableType: 'Properties', sheet: 'Env', properties: { scope: 'Category', category: 'Pricing' } }),
        ]

        const nodes = treeOf(tables, 'category', key)

        expect(nodes.map(node => node.title)).toEqual(['browser.module.module_properties', 'Pricing'])
        expect(nodes[0]?.children.map(child => child.title)).toEqual(['ModuleDefaults'])
        expect(nodes[1]?.children.map(child => child.title)).toEqual(['browser.module.category_properties', 'Premium'])
        expect(nodes[1]?.children[0]?.children.map(child => child.title)).toEqual(['PricingDefaults'])
    })

    it('gives every node a key of its own, so two tables of the same name never collide', () => {
        const nodes = treeOf(
            [table('Premium', { id: 'first' }), table('Premium', { id: 'second', sheet: 'Data' })], 'excelSheet', key)
        const keys = nodes.flatMap(node => [node.key, ...node.children.map(child => child.key)])

        expect(new Set(keys).size).toEqual(keys.length)
    })

    it('lists the tables by name when the view groups by nothing they carry', () => {
        const nodes = treeOf([table('beta', { sheet: '' }), table('Alpha', { sheet: '' })], 'excelSheet', key)

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
            const nodes = treeOf([carPrice('Insurance'), carPrice('Banking'), table('Premium')], 'excelSheet', key)
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
            const nodes = treeOf([...tables, namesake('by-year', 'Trade')], 'excelSheet', key)

            expect(nodes[0]?.children.map(child => child.children.length)).toEqual([2, 2])
        })

        it('files a version under the folder even where the branch holds only that one', () => {
            // The other version is written on another sheet, so this branch gathers a single one.
            const nodes = treeOf([carPrice('Banking'), carPrice('Insurance', { sheet: 'More' })], 'excelSheet', key)

            expect(nodes.map(node => node.title)).toEqual(['More', 'Rules'])
            expect(nodes.every(sheet => sheet.children[0]?.title === 'CarPrice')).toBe(true)
            expect(nodes.flatMap(sheet => sheet.children.flatMap(folder => folder.children.map(v => v.title))))
                .toEqual(['CarPrice [lob=Insurance]', 'CarPrice [lob=Banking]'])
        })

        it('gives the folder and its versions keys of their own', () => {
            const nodes = treeOf([carPrice('Banking'), carPrice('Insurance')], 'excelSheet', key)
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
            const shallow = treeOf([table('Premium')], 'excelSheet', key)
            const deep = treeOf([table('A name far longer than the others', { sheet: 'Rules' })], 'excelSheet', key)

            expect(widthOf(deep)).toBeGreaterThan(widthOf(shallow))
            // A row is wider than its own indent and chrome, whatever it is called.
            expect(widthOf(shallow)).toBeGreaterThan(56)
        })

        it('is nothing to scroll when there is nothing to show', () => {
            expect(widthOf([])).toEqual(0)
        })
    })
})
