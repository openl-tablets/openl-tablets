import type { ElementDefinition } from 'cytoscape'

/**
 * A single table of the dependency graph, as returned by the project/table graph endpoints.
 */
export interface GraphNode {
    id: string
    name: string
    kind?: string
    project?: string
    dependencies?: string[]
    dependents?: string[]
}

const KIND_COLORS: Record<string, string> = {
    'Rules': '#1677ff',
    'Smart Rules': '#2f54eb',
    'Spreadsheet': '#722ed1',
    'Datatype': '#13c2c2',
    'Vocabulary': '#08979c',
    'Data': '#52c41a',
    'Test': '#fa8c16',
    'Run': '#a0d911',
    'TBasic': '#eb2f96',
    'Column Match': '#9254de',
    'Method': '#4096ff',
    'Constants': '#faad14',
    'Conditions': '#fa541c',
    'Actions': '#f5222d',
    'Returns': '#597ef7',
    'Environment': '#8c8c8c',
    'Properties': '#bfbfbf',
}
const DEFAULT_COLOR = '#8c8c8c'

export const kindColor = (kind?: string): string => (kind ? KIND_COLORS[kind] : undefined) ?? DEFAULT_COLOR

export interface GraphModel {
    elements: ElementDefinition[]
    byId: Map<string, GraphNode>
    /** id -> ids of tables it depends on (forward edges, filtered to the node set) */
    dependencies: Map<string, string[]>
    /** id -> ids of tables that depend on it (reverse edges, computed) */
    dependents: Map<string, string[]>
    /** distinct table kinds present, sorted */
    kinds: string[]
    stats: { nodes: number, edges: number, cyclic: number, isolated: number }
}

/**
 * Finds the edges between distinct tables that take part in a cycle, using Tarjan's strongly connected components: an
 * edge is cyclic when its endpoints share an SCC of size &gt; 1. Self-loops (recursion) are handled by the caller.
 */
const findCycleEdges = (ids: string[], dependencies: Map<string, string[]>): Set<string> => {
    let index = 0
    const idx = new Map<string, number>()
    const low = new Map<string, number>()
    const onStack = new Set<string>()
    const stack: string[] = []
    const component = new Map<string, number>()
    let componentId = 0

    const connect = (v: string): void => {
        idx.set(v, index)
        low.set(v, index)
        index += 1
        stack.push(v)
        onStack.add(v)
        for (const w of dependencies.get(v) ?? []) {
            if (!idx.has(w)) {
                connect(w)
                low.set(v, Math.min(low.get(v) ?? 0, low.get(w) ?? 0))
            } else if (onStack.has(w)) {
                low.set(v, Math.min(low.get(v) ?? 0, idx.get(w) ?? 0))
            }
        }
        if (low.get(v) === idx.get(v)) {
            let w = ''
            do {
                w = stack.pop() ?? ''
                onStack.delete(w)
                component.set(w, componentId)
            } while (w !== v)
            componentId += 1
        }
    }

    ids.forEach(v => {
        if (!idx.has(v)) {
            connect(v)
        }
    })

    const componentSize = new Map<number, number>()
    component.forEach(c => componentSize.set(c, (componentSize.get(c) ?? 0) + 1))

    const cycleEdges = new Set<string>()
    dependencies.forEach((targets, source) => targets.forEach(target => {
        const sameComponent = component.get(source) === component.get(target)
        if (sameComponent && (componentSize.get(component.get(source) ?? -1) ?? 0) > 1) {
            cycleEdges.add(`${source}->${target}`)
        }
    }))
    return cycleEdges
}

/**
 * Builds the Cytoscape elements and the lookup maps for a list of graph nodes. Reverse adjacency (dependents) is
 * computed so the UI can show "used by" even when the project graph only carries forward dependencies.
 */
export const buildGraphModel = (nodes: GraphNode[]): GraphModel => {
    const byId = new Map(nodes.map(node => [node.id, node]))
    const ids = [...byId.keys()]
    const idSet = new Set(ids)
    const dependencies = new Map<string, string[]>(ids.map(id => [id, []]))
    const dependents = new Map<string, string[]>(ids.map(id => [id, []]))
    const selfLoops = new Set<string>()

    const link = (from: string, to: string): void => {
        if (!idSet.has(from) || !idSet.has(to)) {
            return
        }
        if (from === to) {
            // a table that calls itself (recursion): drawn as a self-loop, kept out of the dependency counts
            selfLoops.add(from)
            return
        }
        if (!dependencies.get(from)!.includes(to)) {
            dependencies.get(from)!.push(to)
        }
        if (!dependents.get(to)!.includes(from)) {
            dependents.get(to)!.push(from)
        }
    }

    nodes.forEach(node => {
        (node.dependencies ?? []).forEach(target => link(node.id, target))
        // table-explore responses also carry dependents (who uses this node) — reverse them into the same maps
        ;(node.dependents ?? []).forEach(source => link(source, node.id))
    })

    const cycleEdges = findCycleEdges(ids, dependencies)
    const elements: ElementDefinition[] = []
    let isolated = 0

    nodes.forEach(node => {
        const used = dependents.get(node.id)!.length
        const uses = dependencies.get(node.id)!.length
        const orphan = used === 0 && uses === 0 && !selfLoops.has(node.id)
        if (orphan) {
            isolated += 1
        }
        const element: ElementDefinition = {
            data: { id: node.id, label: node.name, kind: node.kind ?? '', color: kindColor(node.kind), weight: used },
        }
        if (orphan) {
            element.classes = 'isolated'
        }
        elements.push(element)
    })

    let edges = 0
    dependencies.forEach((targets, source) => targets.forEach(target => {
        edges += 1
        const id = `${source}->${target}`
        const element: ElementDefinition = { data: { id, source, target } }
        if (cycleEdges.has(id)) {
            element.classes = 'cycle'
        }
        elements.push(element)
    }))

    // Recursive tables are drawn as red self-loops (same colour as cross-table cycles) but kept out of the counters.
    selfLoops.forEach(id => {
        elements.push({ data: { id: `${id}->${id}`, source: id, target: id }, classes: 'cycle' })
    })

    const cyclicNodes = new Set<string>()
    cycleEdges.forEach(edge => edge.split('->').forEach(id => cyclicNodes.add(id)))

    const kinds = [...new Set(nodes.map(node => node.kind).filter((kind): kind is string => Boolean(kind)))].sort()

    return {
        elements,
        byId,
        dependencies,
        dependents,
        kinds,
        stats: { nodes: nodes.length, edges, cyclic: cyclicNodes.size, isolated },
    }
}
