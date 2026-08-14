import { bridgeHiddenNodes, buildGraphModel, DISPATCHER_KIND, findCycles, visibleNeighbours } from 'containers/tableGraph'

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

    it('tells the data model edges apart so they can be drawn as a class diagram', () => {
        const model = buildGraphModel([
            {
                id: 'policy',
                name: 'Policy',
                kind: 'Datatype',
                dependencies: ['vehicle', 'car', 'driver'],
                extends: 'vehicle',
                fields: [
                    { name: 'car', type: 'Car', ref: 'car' },
                    { name: 'drivers', type: 'Driver[]', ref: 'driver', collection: true },
                    { name: 'number', type: 'String' },
                ],
            },
            { id: 'vehicle', name: 'Vehicle', kind: 'Datatype' },
            { id: 'car', name: 'Car', kind: 'Datatype' },
            { id: 'driver', name: 'Driver', kind: 'Datatype' },
        ])

        // UML notation: generalization carries no multiplicity, an association is labelled at the end it points at
        expect(edge(model, 'policy->vehicle')?.classes).toBe('extends')
        expect(edge(model, 'policy->vehicle')?.data['multiplicity']).toBeUndefined()
        expect(edge(model, 'policy->car')?.classes).toBe('field')
        expect(edge(model, 'policy->car')?.data['multiplicity']).toBe('0..1')
        expect(edge(model, 'policy->driver')?.classes).toBe('field')
        expect(edge(model, 'policy->driver')?.data['multiplicity']).toBe('0..*')
    })

    it('covers every field of the same type with one association', () => {
        const model = buildGraphModel([
            {
                id: 'policy',
                name: 'Policy',
                kind: 'Datatype',
                dependencies: ['driver'],
                fields: [
                    { name: 'primaryDriver', type: 'Driver', ref: 'driver' },
                    { name: 'additionalDrivers', type: 'Driver[]', ref: 'driver', collection: true },
                ],
            },
            { id: 'driver', name: 'Driver', kind: 'Datatype' },
        ])

        // the merged association must not claim the single field's 0..1 while a collection field also exists
        expect(edge(model, 'policy->driver')?.data['multiplicity']).toBe('0..*')
    })

    it('keeps the data model out of the call-graph problem markers', () => {
        const model = buildGraphModel([
            // two datatypes referring to each other by a field, and a datatype nothing else is built from
            { id: 'policy', name: 'Policy', kind: 'Datatype', dependencies: ['driver'], fields: [{ name: 'driver', type: 'Driver', ref: 'driver' }]},
            { id: 'driver', name: 'Driver', kind: 'Datatype', dependencies: ['policy'], fields: [{ name: 'policy', type: 'Policy', ref: 'policy' }]},
            { id: 'flat', name: 'Request', kind: 'Datatype' },
        ])

        // mutual references are ordinary modelling, not a call cycle
        expect(edge(model, 'policy->driver')?.classes).toBe('field')
        expect(model.stats.cyclic).toBe(0)
        expect(model.callDependencies.get('policy')).toEqual([])
        // a flat datatype is not a table nobody uses — the graph carries no rule-to-datatype links to judge that
        expect(edge(model, 'flat')?.classes).toBeUndefined()
        expect(model.stats.isolated).toBe(0)
    })

    it('draws a datatype field of its own type as a self-association, not as recursion', () => {
        const model = buildGraphModel([
            { id: 'driver', name: 'Driver', kind: 'Datatype', dependencies: ['driver'], fields: [{ name: 'mentor', type: 'Driver', ref: 'driver' }]},
        ])

        const selfLoop = edge(model, 'driver->driver')
        expect(selfLoop?.classes).toBe('field')
        expect(selfLoop?.data['multiplicity']).toBe('0..1')
    })

    it('leaves a plain call between tables unclassified', () => {
        const model = buildGraphModel([
            { id: 'a', name: 'A', kind: 'Rules', dependencies: ['b']},
            { id: 'b', name: 'B', kind: 'Rules' },
        ])

        expect(edge(model, 'a->b')?.classes).toBeUndefined()
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

        const bridges = bridgeHiddenNodes(new Set(['c', 'v']), new Set(['d']), deps)

        expect(bridges.map(bridge => bridge.data.id)).toEqual(['bridge:c->v'])
        expect(bridges[0]?.data).toMatchObject({ source: 'c', target: 'v' })
    })

    it('does not duplicate an existing direct edge', () => {
        const deps = new Map<string, string[]>([['c', ['d', 'v']], ['d', ['v']], ['v', []]])

        expect(bridgeHiddenNodes(new Set(['c', 'v']), new Set(['d']), deps)).toEqual([])
    })

    it('does not bridge through an explore-excluded node (only through filtered kinds)', () => {
        // dispatcher D -> [V1, V2], both versions -> X; a "show only" of V1's path hides V2 (not a hidden kind)
        const deps = new Map<string, string[]>([['D', ['V1', 'V2']], ['V1', ['X']], ['V2', ['X']], ['X', []]])

        // nothing is filtered by kind, so V2 is a hard boundary — no phantom D -> X edge
        expect(bridgeHiddenNodes(new Set(['D', 'V1', 'X']), new Set(), deps)).toEqual([])
    })
})

describe('visibleNeighbours', () => {
    it('returns direct neighbours when nothing is hidden', () => {
        const deps = new Map<string, string[]>([['a', ['b', 'c']]])

        expect(visibleNeighbours('a', deps, new Set(['a', 'b', 'c']), new Set())).toEqual(['b', 'c'])
    })

    it('bridges across a filtered-out neighbour so panel links stay clickable', () => {
        const deps = new Map<string, string[]>([['a', ['h']], ['h', ['b', 'c']]])

        expect(visibleNeighbours('a', deps, new Set(['a', 'b', 'c']), new Set(['h'])).sort()).toEqual(['b', 'c'])
    })
})

describe('findCycles', () => {
    it('finds an indirect cycle once, rooted at its smallest member', () => {
        const deps = new Map<string, string[]>([['a', ['b']], ['b', ['c']], ['c', ['a']]])

        const cycles = findCycles(deps)

        expect(cycles).toHaveLength(1)
        expect(cycles[0]?.nodes).toEqual(['a', 'b', 'c'])
    })

    it('ignores acyclic graphs and direct self-recursion', () => {
        expect(findCycles(new Map([['a', ['b']], ['b', ['c']], ['c', []]]))).toEqual([])
        expect(findCycles(new Map([['a', ['a']]]))).toEqual([])
    })

    it('finds multiple distinct cycles', () => {
        const deps = new Map<string, string[]>([['a', ['b', 'c']], ['b', ['a']], ['c', ['d']], ['d', ['a']]])

        expect(findCycles(deps).map(cycle => cycle.nodes.join('>')).sort()).toEqual(['a>b', 'a>c>d'])
    })
})
