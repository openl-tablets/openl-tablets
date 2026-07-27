import cytoscape from 'cytoscape'
import dagre from 'cytoscape-dagre'
import { buildGraphModel, GRAPH_LAYOUT, type GraphNode } from 'containers/tableGraph'
import { restoreNativeGlobals } from 'utils/prototypeJsCompat'

// Unlike TableGraphModal.test.tsx (which mocks cytoscape), this suite runs the real cytoscape + cytoscape-dagre
// layout on every graph shape buildGraphModel can emit. The dagre engine bundled into cytoscape-dagre has broken
// on major upgrades before (a TypeError deep inside the layout run), and a mocked test cannot catch that.
cytoscape.use(dagre)

const runLayout = (nodes: GraphNode[]) => {
    const model = buildGraphModel(nodes)
    const cy = cytoscape({ headless: true, styleEnabled: false, elements: model.elements })
    cy.layout(GRAPH_LAYOUT as unknown as cytoscape.LayoutOptions).run()
    return cy
}

const expectLaidOut = (nodes: GraphNode[]) => {
    const cy = runLayout(nodes)
    expect(cy.nodes()).toHaveLength(nodes.length)
    cy.nodes().forEach(node => {
        const { x, y } = node.position()
        expect(Number.isFinite(x)).toBe(true)
        expect(Number.isFinite(y)).toBe(true)
    })
    return cy
}

describe('tableGraph dagre layout', () => {
    it('lays out a project dependency graph with the real dagre engine', () => {
        // the shape of a real project graph: spreadsheets calling shared rule tables, plus an unused table
        const cy = expectLaidOut([
            { id: 'policy', name: 'DeterminePolicyPremium', kind: 'Spreadsheet', dependencies: ['driver', 'vehicle', 'discount']},
            { id: 'driver', name: 'DetermineDriverPremium', kind: 'Spreadsheet', dependencies: ['risk', 'premium']},
            { id: 'vehicle', name: 'DetermineVehiclePremium', kind: 'Spreadsheet', dependencies: ['premium', 'surcharge']},
            { id: 'risk', name: 'DriverRisk', kind: 'Rules' },
            { id: 'premium', name: 'BasePremium', kind: 'Rules' },
            { id: 'surcharge', name: 'AgeSurcharge', kind: 'Rules' },
            { id: 'discount', name: 'ClientDiscount', kind: 'Rules' },
            { id: 'lonely', name: 'UnusedTable', kind: 'Rules' },
        ])
        // rankDir LR must spread dependency ranks horizontally, not stack everything on one x
        const xs = new Set(cy.nodes().map(node => Math.round(node.position('x'))))
        expect(xs.size).toBeGreaterThan(1)
    })

    it('lays out a recursive table drawn as a self-loop', () => {
        expectLaidOut([
            { id: 'a', name: 'A', dependencies: ['a', 'b']},
            { id: 'b', name: 'B' },
        ])
    })

    it('lays out call cycles between tables', () => {
        expectLaidOut([
            { id: 'a', name: 'A', dependencies: ['b']},
            { id: 'b', name: 'B', dependencies: ['c']},
            { id: 'c', name: 'C', dependencies: ['a', 'd']},
            { id: 'd', name: 'D', dependencies: ['c']},
        ])
    })

    it('lays out a dispatcher fanning out to many overloaded versions', () => {
        const versions: GraphNode[] = Array.from({ length: 30 }, (_, index) => ({ id: `v${index}`, name: `Rule v${index}`, kind: 'Rules' }))
        expectLaidOut([
            { id: 'hub', name: 'myRule(int)', kind: 'Dispatcher', dependencies: versions.map(version => version.id) },
            ...versions,
        ])
    })

    it('lays out an empty and a single-table graph', () => {
        expectLaidOut([])
        expectLaidOut([{ id: 'only', name: 'OnlyTable' }])
    })

    it('lays out under Prototype.js global pollution once the shim restored Object.values (EPBDS-16212)', () => {
        // legacy JSF pages load Prototype.js: enumerable Array.prototype extensions + a for-in Object.values;
        // dagre iterates Object.values of an array and crashed on the extensions until the shim restores it
        Object.defineProperty(Array.prototype, 'each', { value: () => [], enumerable: true, configurable: true })
        const forInValues = (obj: object) => {
            const result: unknown[] = []
            for (const key in obj) {
                result.push((obj as Record<string, unknown>)[key])
            }
            return result
        }
        Object.defineProperty(Object, 'values', { value: forInValues, writable: true, configurable: true })
        try {
            restoreNativeGlobals()
            expectLaidOut([
                { id: 'policy', name: 'DeterminePolicyPremium', kind: 'Spreadsheet', dependencies: ['driver', 'premium']},
                { id: 'driver', name: 'DetermineDriverPremium', kind: 'Spreadsheet', dependencies: ['premium']},
                { id: 'premium', name: 'BasePremium', kind: 'Rules' },
            ])
        } finally {
            Reflect.deleteProperty(Array.prototype, 'each')
        }
    })
})
