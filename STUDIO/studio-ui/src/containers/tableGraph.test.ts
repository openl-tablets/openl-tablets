import { bridgeHiddenNodes, buildGraphModel, DISPATCHER_KIND, visibleNeighbours } from 'containers/tableGraph'

const edge = (model: ReturnType<typeof buildGraphModel>, id: string) => model.elements.find(element => element.data.id === id)

describe('buildGraphModel', () => {
    it('draws a recursive table as a red self-loop and keeps it out of the counters', () => {
        const model = buildGraphModel([{ id: 'a', name: 'A', dependencies: ['a']}])

        const selfLoop = edge(model, 'a->a')
        expect(selfLoop?.data.source).toBe('a')
        expect(selfLoop?.data.target).toBe('a')
        expect(selfLoop?.classes).toBe('cycle')
        // recursion is not a dependency on another table, and the node is not "isolated"
        expect(model.dependencies.get('a')).toEqual([])
        expect(edge(model, 'a')?.classes).toBeUndefined()
        expect(model.stats).toMatchObject({ edges: 0, cyclic: 0, isolated: 0 })
    })

    it('marks the edges of a cross-table cycle as cyclic', () => {
        const model = buildGraphModel([
            { id: 'a', name: 'A', dependencies: ['b']},
            { id: 'b', name: 'B', dependencies: ['a']},
        ])

        expect(edge(model, 'a->b')?.classes).toBe('cycle')
        expect(edge(model, 'b->a')?.classes).toBe('cycle')
        expect(model.stats.cyclic).toBe(2)
    })

    it('tags the dispatcher node with a class so the UI can style it apart', () => {
        const model = buildGraphModel([
            { id: 'd', name: 'mySPR(int)', kind: DISPATCHER_KIND, dependencies: ['a', 'b']},
            { id: 'a', name: 'AR' },
            { id: 'b', name: 'AZ' },
        ])

        expect(edge(model, 'd')?.classes).toBe('dispatcher')
        expect(model.kinds).toContain(DISPATCHER_KIND)
    })

    it('flags isolated tables and computes reverse adjacency', () => {
        const model = buildGraphModel([
            { id: 'a', name: 'A', dependencies: ['b']},
            { id: 'b', name: 'B' },
            { id: 'c', name: 'C' },
        ])

        expect(model.dependents.get('b')).toEqual(['a'])
        expect(edge(model, 'c')?.classes).toBe('isolated')
        expect(model.stats.isolated).toBe(1)
    })
})

describe('bridgeHiddenNodes', () => {
    it('reconnects visible tables across a hidden dispatcher', () => {
        const deps = new Map<string, string[]>([['c', ['d']], ['d', ['v']], ['v', []]])

        const bridges = bridgeHiddenNodes(new Set(['c', 'v']), deps)

        expect(bridges.map(bridge => bridge.data.id)).toEqual(['bridge:c->v'])
        expect(bridges[0]?.data).toMatchObject({ source: 'c', target: 'v' })
    })

    it('does not duplicate an existing direct edge', () => {
        const deps = new Map<string, string[]>([['c', ['d', 'v']], ['d', ['v']], ['v', []]])

        expect(bridgeHiddenNodes(new Set(['c', 'v']), deps)).toEqual([])
    })
})

describe('visibleNeighbours', () => {
    it('returns direct neighbours when nothing is hidden', () => {
        const deps = new Map<string, string[]>([['a', ['b', 'c']]])

        expect(visibleNeighbours('a', deps, new Set(['a', 'b', 'c']))).toEqual(['b', 'c'])
    })

    it('bridges across a hidden neighbour so panel links stay clickable', () => {
        const deps = new Map<string, string[]>([['a', ['h']], ['h', ['b', 'c']]])

        expect(visibleNeighbours('a', deps, new Set(['a', 'b', 'c'])).sort()).toEqual(['b', 'c'])
    })
})
