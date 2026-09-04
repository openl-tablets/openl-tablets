import { areaId, bridgeHiddenNodes, buildGraphModel, DISPATCHER_KIND, entityLabel, findCycles, visibleNeighbours } from 'containers/tableGraph'

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

        // inheritance carries no cardinality; a relationship is marked with the cardinality read at the end it points at
        expect(edge(model, 'policy->vehicle')?.classes).toBe('extends')
        expect(edge(model, 'policy->car')?.classes).toBe('field one')
        expect(edge(model, 'policy->driver')?.classes).toBe('field many')
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

        // the merged relationship must not claim "one" while a collection field also exists
        expect(edge(model, 'policy->driver')?.classes).toBe('field many')
    })

    it('keeps the data model out of the call-graph problem markers', () => {
        const model = buildGraphModel([
            // two datatypes referring to each other by a field, and a datatype nothing else is built from
            { id: 'policy', name: 'Policy', kind: 'Datatype', dependencies: ['driver'], fields: [{ name: 'driver', type: 'Driver', ref: 'driver' }]},
            { id: 'driver', name: 'Driver', kind: 'Datatype', dependencies: ['policy'], fields: [{ name: 'policy', type: 'Policy', ref: 'policy' }]},
            { id: 'flat', name: 'Request', kind: 'Datatype' },
        ])

        // mutual references are ordinary modelling, not a call cycle
        expect(edge(model, 'policy->driver')?.classes).toBe('field one')
        expect(model.stats.cyclic).toBe(0)
        expect(model.callDependencies.get('policy')).toEqual([])
        // a flat datatype is not a table nobody uses — the graph carries no rule-to-datatype links to judge that
        expect(edge(model, 'flat')?.classes).toBe('entity')
        expect(model.stats.isolated).toBe(0)
    })

    it('draws a datatype field of its own type as a self-association, not as recursion', () => {
        const model = buildGraphModel([
            { id: 'driver', name: 'Driver', kind: 'Datatype', dependencies: ['driver'], fields: [{ name: 'mentor', type: 'Driver', ref: 'driver' }]},
        ])

        expect(edge(model, 'driver->driver')?.classes).toBe('field one')
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

describe('subject areas', () => {
    it('frames the data model of each project, and leaves the callable tables out of it', () => {
        const model = buildGraphModel([
            { id: 'policy', name: 'Policy', kind: 'Datatype', project: 'Motor' },
            { id: 'driver', name: 'Driver', kind: 'Datatype', project: 'Motor' },
            { id: 'address', name: 'Address', kind: 'Datatype', project: 'Shared' },
            { id: 'calc', name: 'calcPremium', kind: 'Spreadsheet', project: 'Motor' },
        ])

        const areas = model.elements.filter(element => element.classes === 'area')
        expect(areas.map(area => area.data.id)).toEqual([areaId('Motor'), areaId('Shared')])
        expect(areas.map(area => area.data['label'])).toEqual(['Motor', 'Shared'])
        // every datatype names the area of its own project; a rules table belongs to no area
        expect(edge(model, 'policy')?.data['parent']).toBe(areaId('Motor'))
        expect(edge(model, 'address')?.data['parent']).toBe(areaId('Shared'))
        expect(edge(model, 'calc')?.data['parent']).toBeUndefined()
    })

    it('leaves a datatype of an unnamed project unframed', () => {
        const model = buildGraphModel([{ id: 'policy', name: 'Policy', kind: 'Datatype' }])

        expect(model.elements.filter(element => element.classes === 'area')).toHaveLength(0)
        expect(edge(model, 'policy')?.data['parent']).toBeUndefined()
    })
})

describe('entityLabel', () => {
    it('draws a datatype as an ER entity box: the name, a rule, then the fields it declares', () => {
        const label = entityLabel({
            id: 'policy',
            name: 'Policy',
            kind: 'Datatype',
            fields: [
                { name: 'car', type: 'Car', ref: 'car' },
                { name: 'drivers', type: 'Driver[]', ref: 'driver', collection: true },
            ],
        })

        // the members read as two columns: the name, then the type it holds
        expect(label.split('\n')).toEqual(['Policy', '─'.repeat('drivers  Driver[]'.length), 'car      Car', 'drivers  Driver[]'])
    })

    it('leaves a datatype without fields as a bare name, with no empty compartment', () => {
        expect(entityLabel({ id: 'flat', name: 'Request', kind: 'Datatype' })).toBe('Request')
    })

    it('titles a vocabulary with the type it narrows, and lists its values', () => {
        const label = entityLabel({
            id: 'region',
            name: 'Region',
            kind: 'Datatype',
            tableType: 'Vocabulary',
            vocabulary: { valueType: 'String', valueCount: 3, valuesPreview: ['North', 'South', 'East'], truncated: false },
        })

        expect(label.split('\n')).toEqual(['Region: String', '──────────────', 'North', 'South', 'East'])
    })

    it('marks the values a truncated preview leaves out, in the place they were left out of', () => {
        const label = entityLabel({
            id: 'score',
            name: 'Score',
            kind: 'Datatype',
            tableType: 'Vocabulary',
            vocabulary: { valueType: 'Integer', valueCount: 8, valuesPreview: [100, 200, 300, 600, 700, 800], truncated: true },
        })

        // the gap sits between the first values and the last ones, and says how many are behind it
        expect(label.split('\n').slice(2)).toEqual(['100', '200', '300', '… +2 more', '600', '700', '800'])
    })

    it('draws a vocabulary that lists no values, whose preview the graph leaves out entirely', () => {
        const label = entityLabel({
            id: 'blank',
            name: 'Blank',
            kind: 'Datatype',
            tableType: 'Vocabulary',
            vocabulary: { valueType: 'String', valueCount: 0, truncated: false },
        })

        expect(label).toBe('Blank: String')
    })

    it('bounds a wide datatype so one type cannot take over the canvas', () => {
        const fields = Array.from({ length: 15 }, (_, index) => ({ name: `field${index}`, type: 'String' }))
        const label = entityLabel({ id: 'wide', name: 'Wide', kind: 'Datatype', fields })

        const lines = label.split('\n')
        expect(lines.slice(-2)).toEqual(['field11  String', '… +3 more'])
        expect(lines.filter(line => line.startsWith('field'))).toHaveLength(12)
    })

    it('cuts a name too long for its column and a line too long to scan', () => {
        const label = entityLabel({
            id: 'long',
            name: 'Long',
            kind: 'Datatype',
            fields: [{ name: 'aVeryLongFieldNameIndeed', type: 'SomeEquallyLongDatatype' }],
        })

        expect(label.split('\n')[2]).toBe('aVeryLongFieldN…  SomeEqual…')
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
