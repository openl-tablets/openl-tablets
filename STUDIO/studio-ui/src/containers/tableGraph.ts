import type { ElementDefinition } from 'cytoscape'
import type { DagreLayoutOptions } from 'cytoscape-dagre'
import type { DatatypeField } from 'types/tables'

// Dagre-specific options (rankDir, nodeSep, rankSep) live in cytoscape-dagre's own DagreLayoutOptions, not in the
// base @types/cytoscape LayoutOptions union; typing the constant with it keeps these fields type-checked.
export const GRAPH_LAYOUT: DagreLayoutOptions = {
    name: 'dagre',
    rankDir: 'LR',
    nodeSep: 18,
    rankSep: 70,
    animate: false,
}

/**
 * A field a datatype declares, as the graph reports it. A field whose type is another datatype of the graph names
 * that datatype's node in {@code ref}; a field of a simple type carries its type only.
 */
export interface GraphField extends DatatypeField {
    ref?: string
    collection?: boolean
}

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
    // summary meta, shown in the side panel on click
    tableType?: string
    returnType?: string
    signature?: string
    file?: string
    pos?: string
    properties?: Record<string, unknown>
    // dimension properties this version is selected by (the dispatching/versioning rules)
    dimensionProperties?: Record<string, string>
    // data model of a datatype node: the datatype it extends and the fields it declares
    extends?: string
    fields?: GraphField[]
}

/**
 * Kind of the technical node that stands for an OpenMethodDispatcher — the generated table that selects one overloaded
 * version at runtime. Matches {@code ProjectTablesGraphService.DISPATCHER_KIND} on the backend.
 */
export const DISPATCHER_KIND = 'Dispatcher'

/** Kind of a datatype table — a datatype or a vocabulary — the only node kind that carries a data model. */
export const DATATYPE_KIND = 'Datatype'

const KIND_COLORS: Record<string, string> = {
    [DISPATCHER_KIND]: '#874d00',
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
    /** id -> ids of tables it calls: the dependencies without the data model, for the cycle hunt */
    callDependencies: Map<string, string[]>
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
 * How a data model edge is drawn and labelled, following UML class diagram notation: {@code extends} is a
 * generalization, {@code field} an association whose multiplicity is written at the end it points at. A datatype field
 * is optional and a collection field may be empty, hence {@code 0..1} and {@code 0..*}. Returns undefined for a plain
 * call between tables, which is not part of the data model.
 *
 * The graph draws one edge per pair of datatypes, so a datatype declaring several fields of the same type gets one
 * association covering them all — a collection as soon as one of them is. Inheritance outranks a field of the parent's
 * own type. The side panel lists every field separately, whatever the canvas merges.
 */
const dataModelEdge = (source: GraphNode | undefined, target: string): { relation: string, multiplicity?: string } | undefined => {
    if (!source) {
        return undefined
    }
    if (source.extends === target) {
        return { relation: 'extends' }
    }
    const fields = (source.fields ?? []).filter(entry => entry.ref === target)
    if (fields.length === 0) {
        return undefined
    }
    return { relation: 'field', multiplicity: fields.some(field => field.collection) ? '0..*' : '0..1' }
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

    // Cycles are a call-graph problem. Two datatypes referring to each other by a field is ordinary modelling, and a
    // datatype cannot inherit in a circle at all, so the data model is left out of the cycle hunt entirely.
    const callDependencies = new Map<string, string[]>(
        [...dependencies].map(([source, targets]) => [source, targets.filter(target => !dataModelEdge(byId.get(source), target))])
    )
    const cycleEdges = findCycleEdges(ids, callDependencies)
    const elements: ElementDefinition[] = []
    let isolated = 0

    nodes.forEach(node => {
        const used = dependents.get(node.id)!.length
        const uses = dependencies.get(node.id)!.length
        // A datatype built from no other datatype is a flat datatype, not a table nobody uses: the graph carries no
        // rule-to-datatype links, so it cannot tell an unused datatype from a used one. Never mark one as isolated.
        const orphan = used === 0 && uses === 0 && !selfLoops.has(node.id) && node.kind !== DATATYPE_KIND
        if (orphan) {
            isolated += 1
        }
        const element: ElementDefinition = {
            data: { id: node.id, label: node.name, kind: node.kind ?? '', color: kindColor(node.kind), weight: used },
        }
        const classes: string[] = []
        if (orphan) {
            classes.push('isolated')
        }
        if (node.kind === DISPATCHER_KIND) {
            classes.push('dispatcher')
        }
        if (classes.length > 0) {
            element.classes = classes.join(' ')
        }
        elements.push(element)
    })

    let edges = 0
    dependencies.forEach((targets, source) => targets.forEach(target => {
        edges += 1
        const id = `${source}->${target}`
        const element: ElementDefinition = { data: { id, source, target } }
        const dataModel = dataModelEdge(byId.get(source), target)
        const cyclic = cycleEdges.has(id)
        if (dataModel || cyclic) {
            element.classes = [dataModel?.relation, cyclic ? 'cycle' : undefined].filter(Boolean).join(' ')
        }
        if (dataModel?.multiplicity) {
            element.data['multiplicity'] = dataModel.multiplicity
        }
        elements.push(element)
    }))

    // A table calling itself is recursion — a red self-loop, kept out of the counters. A datatype with a field of its
    // own type is an ordinary self-association instead, and keeps the data model's notation.
    selfLoops.forEach(id => {
        const dataModel = dataModelEdge(byId.get(id), id)
        const element: ElementDefinition = {
            data: { id: `${id}->${id}`, source: id, target: id },
            classes: dataModel?.relation ?? 'cycle',
        }
        if (dataModel?.multiplicity) {
            element.data['multiplicity'] = dataModel.multiplicity
        }
        elements.push(element)
    })

    const cyclicNodes = new Set<string>()
    cycleEdges.forEach(edge => edge.split('->').forEach(id => cyclicNodes.add(id)))

    const kinds = [...new Set(nodes.map(node => node.kind).filter((kind): kind is string => Boolean(kind)))]
        .sort((a, b) => a.localeCompare(b))

    return {
        elements,
        byId,
        dependencies,
        callDependencies,
        dependents,
        kinds,
        stats: { nodes: nodes.length, edges, cyclic: cyclicNodes.size, isolated },
    }
}

/**
 * Resolves a table's neighbours in a relation map (uses or used-by) to the visible tables only, bridging across the
 * tables that were filtered out by kind ({@code passThrough}). Tables hidden for any other reason — e.g. excluded by a
 * "show only" exploration — act as a hard boundary and are not crossed, so a dispatcher never inherits an edge that
 * actually belongs to one of its hidden sibling versions. When nothing is bridged it returns the direct visible
 * neighbours.
 */
export const visibleNeighbours = (
    id: string,
    relations: Map<string, string[]>,
    visible: Set<string>,
    passThrough: Set<string>
): string[] => {
    const result = new Set<string>()
    const walked = new Set<string>()
    const queue = [id]
    // Iterate with for-of (it keeps yielding items pushed during the walk) instead of Array.shift(), which is O(n).
    for (const current of queue) {
        if (!walked.has(current)) {
            walked.add(current)
            ;(relations.get(current) ?? []).forEach(next => {
                if (visible.has(next)) {
                    result.add(next)
                } else if (passThrough.has(next)) {
                    queue.push(next)
                }
            })
        }
    }
    result.delete(id)
    return [...result]
}

/**
 * Builds bridge edges that reconnect visible tables across the kind-filtered ones ({@code passThrough}). When a kind is
 * hidden (e.g. a dispatcher), its incoming and outgoing links would otherwise be cut; bridging restores the transitive
 * connection (caller &#8594; version). Only those tables are bridged — tables excluded by an exploration are a hard
 * boundary — and direct links between visible tables are left untouched.
 */
export const bridgeHiddenNodes = (
    visible: Set<string>,
    passThrough: Set<string>,
    dependencies: Map<string, string[]>
): ElementDefinition[] => {
    const bridges: ElementDefinition[] = []
    const added = new Set<string>()
    visible.forEach(source => {
        const direct = new Set((dependencies.get(source) ?? []).filter(dep => visible.has(dep)))
        visibleNeighbours(source, dependencies, visible, passThrough).forEach(target => {
            const key = `${source}->${target}`
            if (!direct.has(target) && !added.has(key)) {
                added.add(key)
                bridges.push({ data: { id: `bridge:${key}`, source, target }, classes: 'bridge' })
            }
        })
    })
    return bridges
}

/** A simple call cycle: tables that, following their dependencies, lead back to the first one (e.g. A &#8594; B &#8594; C &#8594; A). */
export interface GraphCycle {
    /** stable key for the cycle */
    id: string
    /** the tables on the cycle, in call order; the last one calls back into the first */
    nodes: string[]
}

/**
 * Finds simple call cycles in the dependency graph. Direct self-recursion (a table calling itself) is excluded — it is
 * drawn as a self-loop instead. Each cycle is reported once, rooted at its smallest member, so rotations are not
 * duplicated. The search is bounded by {@code limit} cycles and a step budget so a densely connected graph cannot hang
 * the UI.
 */
export const findCycles = (dependencies: Map<string, string[]>, minNodes = 2, limit = 100): GraphCycle[] => {
    const cycles: GraphCycle[] = []
    let steps = 0
    const stepLimit = 200_000

    const explore = (start: string, current: string, path: string[], onPath: Set<string>): void => {
        if (cycles.length >= limit || steps >= stepLimit) {
            return
        }
        steps += 1
        ;(dependencies.get(current) ?? []).forEach(next => {
            if (next === start && path.length >= minNodes && cycles.length < limit) {
                cycles.push({ id: path.join('>'), nodes: [...path]})
            } else if (next > start && !onPath.has(next)) {
                onPath.add(next)
                path.push(next)
                explore(start, next, path, onPath)
                path.pop()
                onPath.delete(next)
            }
        })
    }

    ;[...dependencies.keys()].sort((a, b) => a.localeCompare(b)).forEach(start => explore(start, start, [start], new Set([start])))
    return cycles
}
