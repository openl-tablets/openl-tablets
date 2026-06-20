import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Divider, Empty, Modal, Select, Space, Spin, Tag, Tooltip, Typography } from 'antd'
import {
    AimOutlined,
    BgColorsOutlined,
    CloseOutlined,
    ExportOutlined,
    PartitionOutlined,
    RetweetOutlined,
    RollbackOutlined,
    ZoomInOutlined,
    ZoomOutOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import cytoscape, { type Core } from 'cytoscape'
import dagre from 'cytoscape-dagre'
import { useGlobalEvents } from '../hooks'
import { apiCall, type ApiCallOptions } from '../services'
import {
    bridgeHiddenNodes,
    buildGraphModel,
    DISPATCHER_KIND,
    findCycles,
    type GraphCycle,
    type GraphNode,
    kindColor,
    visibleNeighbours,
} from './tableGraph'

cytoscape.use(dagre)

const GRAPH_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

type Direction = 'DEPENDENCIES' | 'DEPENDENTS' | 'BOTH'

// Upper bound on enumerated cycles, so a densely connected project cannot flood the UI.
const CYCLE_SEARCH_LIMIT = 100

// A cycle longer than this many tables is shown truncated (…) in its chip; the full chain stays in the chip tooltip.
const CYCLE_LABEL_HEAD = 3

// The full call chain, closed back to the first table: A → B → C → A.
const cycleTooltip = (names: string[]): string => [...names, names[0] ?? ''].join(' → ')

const cycleLabel = (names: string[]): string => {
    if (names.length <= CYCLE_LABEL_HEAD) {
        return cycleTooltip(names)
    }
    return `${names.slice(0, CYCLE_LABEL_HEAD).join(' → ')} → … (${names.length})`
}

// Distinguishes tables that share a display name in the candidates bar — their location, else signature/kind.
const candidateLabel = (node: GraphNode): string => {
    const location = [node.file, node.pos].filter(Boolean).join(' · ')
    return location || node.signature || node.kind || node.id
}

const candidateTooltip = (node: GraphNode): string => [node.kind, node.project, candidateLabel(node)].filter(Boolean).join(' · ')

// The editor endpoint resolves tables in the active project only, so a table from a dependency project (a different
// project name) cannot be opened. Dispatcher nodes are synthetic and have no editor table at all.
const canOpenTable = (node: GraphNode | undefined, projectName?: string): boolean =>
    !!node && node.kind !== DISPATCHER_KIND && (!projectName || !node.project || node.project === projectName)

const GRAPH_LAYOUT = {
    name: 'dagre',
    rankDir: 'LR',
    nodeSep: 18,
    rankSep: 70,
    animate: false,
} as unknown as cytoscape.LayoutOptions

// Schematic palette. One accent is reserved for selection and never names a table kind; red means "problem" only.
const SELECT_ACCENT = '#fa8c16'
const PROBLEM_ACCENT = '#f5222d'
const DISPATCHER_ACCENT = '#ffc53d'
const HAIRLINE = '#e8e8e8'
// IDs, cells and signatures are code, so they are set in a monospace utility face.
const MONO = 'ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace'
// A faint blueprint dot-grid behind the graph, so nodes read as placed on a board rather than floating in a void.
const CANVAS_BG = '#fbfcfe'
const DOT_GRID = 'radial-gradient(circle, #e6ebf2 1.1px, transparent 1.2px)'

const buildStyle = (maxWeight: number) => [
    {
        selector: 'node',
        style: {
            'background-color': 'data(color)',
            'label': 'data(label)',
            'color': '#ffffff',
            'font-size': 11,
            'font-weight': 500,
            // a dark halo keeps the white label readable on light kind fills (e.g. Properties grey)
            'text-outline-color': 'rgba(0, 0, 0, 0.45)',
            'text-outline-width': 1.4,
            'text-valign': 'center',
            'text-halign': 'center',
            'text-wrap': 'ellipsis',
            'text-max-width': '120px',
            'shape': 'round-rectangle',
            'width': 'label',
            'height': 'label',
            'padding': '6px',
            'border-color': '#000000',
            'border-opacity': 0.35,
            'border-width': `mapData(weight, 0, ${Math.max(maxWeight, 1)}, 0, 7)`,
        },
    },
    {
        selector: 'node.isolated',
        style: { 'border-width': 3, 'border-style': 'dashed', 'border-color': PROBLEM_ACCENT, 'border-opacity': 1 },
    },
    {
        // the technical dispatcher table that selects one overloaded version: a distinct cut-rectangle with a gold frame
        selector: 'node.dispatcher',
        style: { 'shape': 'cut-rectangle', 'border-width': 3, 'border-color': DISPATCHER_ACCENT, 'border-opacity': 1 },
    },
    {
        // selection is a detached ring (elevation), not a fill colour — so it never collides with a table kind
        selector: 'node.highlighted',
        style: {
            'outline-width': 3,
            'outline-color': SELECT_ACCENT,
            'outline-offset': 3,
            'outline-opacity': 1,
            'border-color': '#7a3b00',
            'border-width': 2,
            'border-opacity': 1,
        },
    },
    { selector: '.faded', style: { 'opacity': 0.12 } },
    { selector: '.hidden', style: { 'display': 'none' } },
    {
        selector: 'edge',
        style: {
            'width': 1.3,
            'line-color': '#c0c0c0',
            'target-arrow-color': '#c0c0c0',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
            'arrow-scale': 0.8,
        },
    },
    {
        selector: 'edge.cycle',
        style: { 'line-color': PROBLEM_ACCENT, 'target-arrow-color': PROBLEM_ACCENT, 'line-style': 'dashed', 'width': 2 },
    },
    {
        // self-loops (a table that calls itself): dagre ignores them for ranking, so give them an explicit, compact
        // loop on top of the node. A tight sweep keeps both ends anchored near the top-centre — otherwise the target
        // endpoint lands on the node side and, on a wide node, the arrowhead is flung far past the box.
        selector: 'edge:loop',
        style: {
            'curve-style': 'bezier',
            'control-point-step-size': 36,
            'loop-direction': '0deg',
            'loop-sweep': '-28deg',
            'line-color': PROBLEM_ACCENT,
            'target-arrow-color': PROBLEM_ACCENT,
            'line-style': 'dashed',
            'width': 2,
        },
    },
] as unknown as cytoscape.StylesheetCSS[]

/**
 * Detail passed from the legacy JSF page via the {@code openTableGraphModal} event.
 */
export interface TableGraphModalDetail {
    projectId: string
    /** Name of the opened project. Tables from other (dependency) projects in the graph cannot be opened in the editor. */
    projectName?: string
}

/** The editor keeps the open table in the URL fragment as `…table?id=<tableId>`; read it so the graph can preselect it. */
const tableIdFromHash = (): string | undefined => globalThis.location.hash.match(/[?&]id=([^&]+)/)?.[1]

/**
 * Opens the tapped table in the editor via the backend-resolved URL, then closes the graph. The backend returns a
 * page-relative fragment (e.g. {@code #repo/project/module/table}) that the editor shell hosting this modal resolves on
 * hash change, so it is navigated as-is — prefixing the origin would leave the editor page and drop the context path.
 * The endpoint resolves tables in the active project only, so a foreign-project table yields no URL and is left alone.
 */
const openTable = (id: string): void => {
    apiCall(`/compile/table/${id}/url`, { method: 'GET' }, GRAPH_API_OPTIONS)
        .then((data: { url?: string | null }) => {
            if (data?.url) {
                globalThis.location.href = `${data.url}?id=${id}`
                globalThis.dispatchEvent(new CustomEvent('openTableGraphModal', { detail: null }))
            }
        })
        .catch(() => undefined)
}

/**
 * TableGraphModal renders an interactive dependency graph of the current project's tables with Cytoscape: nodes are
 * coloured by table kind, sized by how widely they are used, cycles are highlighted, and a side panel lets the user
 * focus, explore and open tables.
 *
 * @example globalThis.dispatchEvent(new CustomEvent('openTableGraphModal', {detail: {projectId}}))
 */
export const TableGraphModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<TableGraphModalDetail>('openTableGraphModal')

    const [visible, setVisible] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(false)
    const [nodes, setNodes] = useState<GraphNode[]>([])
    const [hiddenKinds, setHiddenKinds] = useState<Set<string>>(new Set())
    const [explore, setExplore] = useState<{ id: string, direction: Direction, via?: string }>()
    const [selectedId, setSelectedId] = useState<string>()
    const [searchName, setSearchName] = useState<string>()
    const [cycles, setCycles] = useState<GraphCycle[] | null>(null)
    const [activeCycle, setActiveCycle] = useState<GraphCycle>()
    const [legendOpen, setLegendOpen] = useState(true)

    const containerRef = useRef<HTMLDivElement>(null)
    const cyRef = useRef<Core | null>(null)
    // The table open in the editor when the graph was launched, to preselect it once the graph is laid out.
    const pendingSelectRef = useRef<string | undefined>(undefined)
    // The opened project's name, read inside the cytoscape tap handler to skip opening foreign-project tables.
    const projectNameRef = useRef<string | undefined>(undefined)

    useEffect(() => {
        const hasDetails = !!detail?.projectId
        setVisible(hasDetails)
        if (!hasDetails || !detail) {
            return
        }
        let cancelled = false
        setLoading(true)
        setError(false)
        setNodes([])
        setSelectedId(undefined)
        setSearchName(undefined)
        setExplore(undefined)
        setHiddenKinds(new Set())
        setCycles(null)
        setActiveCycle(undefined)
        // Preselect the table the user is editing, and remember which project can be opened (see the tap handler).
        pendingSelectRef.current = tableIdFromHash()
        projectNameRef.current = detail.projectName
        // The id may arrive in the standard Base64 alphabet; normalize to the URL-safe form (the backend decodes both).
        const projectId = detail.projectId.replaceAll('+', '-').replaceAll('/', '_')
        apiCall(`/projects/${projectId}/tables/graph`, { method: 'GET' }, GRAPH_API_OPTIONS)
            .then((data: GraphNode[]) => {
                if (!cancelled) {
                    setNodes(Array.isArray(data) ? data : [])
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setNodes([])
                    setError(true)
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false)
                }
            })
        return () => {
            cancelled = true
        }
    }, [detail])

    const model = useMemo(() => buildGraphModel(nodes), [nodes])
    // Index tables by display name once; several tables can share a name, and the candidates bar lets the user pick.
    const byName = useMemo(() => {
        const index = new Map<string, GraphNode[]>()
        nodes.forEach(node => {
            const named = index.get(node.name)
            if (named) {
                named.push(node)
            } else {
                index.set(node.name, [node])
            }
        })
        return index
    }, [nodes])
    const nodeOptions = useMemo(() => [...byName.keys()].sort().map(name => ({ label: name, value: name })), [byName])
    const nameMatches = useMemo(() => (searchName ? byName.get(searchName) ?? [] : []), [byName, searchName])
    // Tables per kind, for the legend's counts (a kind with 0 tables is simply absent).
    const kindCounts = useMemo(() => {
        const counts = new Map<string, number>()
        nodes.forEach(node => counts.set(node.kind ?? '', (counts.get(node.kind ?? '') ?? 0) + 1))
        return counts
    }, [nodes])
    const maxWeight = useMemo(() => Math.max(1, ...[...model.dependents.values()].map(list => list.length)), [model])

    // Nodes that are currently shown, taking the kind filter and the "show only" exploration into account.
    const visibleIds = useMemo(() => {
        const reachable = (start: string): Set<string> => {
            const seen = new Set([start])
            const queue = [start]
            // Dequeue with a moving head index; Array.shift() is O(n) and would make the traversal O(n²).
            for (let head = 0; head < queue.length; head++) {
                const current = queue[head] as string
                let next: string[] = []
                if (explore?.direction !== 'DEPENDENTS') {
                    next.push(...(model.dependencies.get(current) ?? []))
                }
                if (explore?.direction !== 'DEPENDENCIES') {
                    next.push(...(model.dependents.get(current) ?? []))
                }
                // dispatcher path: from the dispatcher node follow only the chosen version, then expand it in full
                if (explore?.via && current === explore.id) {
                    next = next.filter(id => id === explore.via)
                }
                next.filter(id => !seen.has(id)).forEach(id => {
                    seen.add(id)
                    queue.push(id)
                })
            }
            return seen
        }
        const scope = explore ? reachable(explore.id) : new Set(model.byId.keys())
        return new Set([...scope].filter(id => !hiddenKinds.has(model.byId.get(id)?.kind ?? '')))
    }, [model, hiddenKinds, explore])

    // Tables removed by the kind filter — the only ones bridging may cross (an exploration boundary stays hard, so a
    // dispatcher never inherits a phantom edge to a node that one of its explore-hidden sibling versions points at).
    const kindHidden = useMemo(
        () => new Set([...model.byId.keys()].filter(id => hiddenKinds.has(model.byId.get(id)?.kind ?? ''))),
        [model, hiddenKinds]
    )

    // Create the Cytoscape instance whenever the graph changes; wire selection and open-on-double-tap.
    useEffect(() => {
        if (!visible || loading || !containerRef.current || model.elements.length === 0) {
            return
        }
        const cy = cytoscape({ container: containerRef.current, elements: model.elements, style: buildStyle(maxWeight) })
        cyRef.current = cy
        cy.layout(GRAPH_LAYOUT).run()

        // Preselect the table that was open in the editor (focuses and centres it via the selection effect).
        if (pendingSelectRef.current && model.byId.has(pendingSelectRef.current)) {
            setSelectedId(pendingSelectRef.current)
        }
        pendingSelectRef.current = undefined

        let lastTap = { id: '', time: 0 }
        cy.on('tap', 'node', event => {
            const id = event.target.id()
            const now = Date.now()
            setSelectedId(id)
            setSearchName(undefined)
            setActiveCycle(undefined)
            // Double-tap opens the table, but only when it lives in the active project — foreign tables cannot be opened.
            if (id === lastTap.id && now - lastTap.time < 350) {
                if (canOpenTable(model.byId.get(id), projectNameRef.current)) {
                    openTable(id)
                }
                lastTap = { id: '', time: 0 }
            } else {
                lastTap = { id, time: now }
            }
        })
        cy.on('tap', event => {
            if (event.target === cy) {
                setSelectedId(undefined)
                setSearchName(undefined)
                setActiveCycle(undefined)
            }
        })
        return () => {
            cy.destroy()
            cyRef.current = null
        }
    }, [visible, loading, model, maxWeight])

    // Apply the kind filter / exploration scope.
    useEffect(() => {
        const cy = cyRef.current
        if (!cy) {
            return
        }
        cy.batch(() => {
            cy.nodes().forEach(node => {
                node.toggleClass('hidden', !visibleIds.has(node.id()))
            })
            // reconnect tables across the ones just hidden, so filtering a kind rebuilds links instead of cutting them
            cy.remove('edge.bridge')
            cy.add(bridgeHiddenNodes(visibleIds, kindHidden, model.dependencies))
        })
    }, [visibleIds, kindHidden, model])

    // Focus the selected table or the picked cycle: highlight it, fade the rest, and bring it into view.
    useEffect(() => {
        const cy = cyRef.current
        if (!cy) {
            return
        }
        cy.elements().removeClass('faded highlighted')
        if (activeCycle) {
            const ids = activeCycle.nodes
            const cycleEdges = ids.map((id, index) => `${id}->${ids[(index + 1) % ids.length] ?? ''}`)
            cy.batch(() => {
                cy.elements().not('.hidden').addClass('faded')
                ids.forEach(id => cy.getElementById(id).removeClass('faded').addClass('highlighted'))
                cycleEdges.forEach(edgeId => cy.getElementById(edgeId).removeClass('faded'))
            })
            cy.animate({ center: { eles: cy.getElementById(ids[0] ?? '') }, zoom: Math.max(cy.zoom(), 1) }, { duration: 300 })
            return
        }
        if (!selectedId) {
            return
        }
        const node = cy.getElementById(selectedId)
        if (node.empty() || node.hasClass('hidden')) {
            return
        }
        const neighbourhood = node.closedNeighborhood()
        cy.elements().not(neighbourhood).not('.hidden').addClass('faded')
        node.addClass('highlighted')
        cy.animate({ center: { eles: node }, zoom: Math.max(cy.zoom(), 1) }, { duration: 300 })
        // visibleIds is a dependency because a kind filter / exploration change re-hides nodes, and the fade set must be
        // recomputed against the new '.hidden' nodes — otherwise the previous highlight/fade is left stale.
    }, [selectedId, activeCycle, visibleIds])

    // The bottom bars (cycles / name candidates) take vertical space from the graph; keep the canvas matched.
    useEffect(() => {
        cyRef.current?.resize()
    }, [cycles, nameMatches])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openTableGraphModal', { detail: null }))
    }, [])

    const toggleKind = useCallback((kind: string) => {
        setHiddenKinds(prev => {
            const next = new Set(prev)
            if (next.has(kind)) {
                next.delete(kind)
            } else {
                next.add(kind)
            }
            return next
        })
    }, [])

    const zoomBy = useCallback((factor: number) => {
        const cy = cyRef.current
        if (cy) {
            cy.zoom({ level: cy.zoom() * factor, renderedPosition: { x: cy.width() / 2, y: cy.height() / 2 } })
        }
    }, [])

    const selected = selectedId ? model.byId.get(selectedId) : undefined
    const currentProjectName = detail?.projectName
    // Resolve the panel's neighbour lists once per selection/filter change, not on every modal re-render.
    const selectedUses = useMemo(
        () => (selected && selected.kind !== DISPATCHER_KIND
            ? visibleNeighbours(selected.id, model.dependencies, visibleIds, kindHidden)
            : []),
        [selected, model, visibleIds, kindHidden]
    )
    const selectedUsedBy = useMemo(
        () => (selected ? visibleNeighbours(selected.id, model.dependents, visibleIds, kindHidden) : []),
        [selected, model, visibleIds, kindHidden]
    )
    const hasGraph = !loading && !error && model.elements.length > 0

    // The graph's vital signs, shown in the modal header: size, plus problem counts that reveal what they point at.
    const renderVitalSigns = () => (
        <Typography.Text style={{ fontWeight: 400, fontSize: 13 }} type="secondary">
            {t('graph:stats', model.stats)}
            {model.stats.cyclic > 0 && (
                <>
                    {' · '}
                    <Typography.Text
                        onClick={() => { setCycles(findCycles(model.dependencies, 2, CYCLE_SEARCH_LIMIT)); setActiveCycle(undefined) }}
                        style={{ fontSize: 13, cursor: 'pointer' }}
                        type="danger"
                    >
                        {t('graph:stats_cyclic', model.stats)}
                    </Typography.Text>
                </>
            )}
            {model.stats.isolated > 0 && <>{' · '}{t('graph:stats_isolated', model.stats)}</>}
        </Typography.Text>
    )

    // A titled list of linked table names. The pick handler decides what choosing one does — focus it (uses / used by),
    // or, for a dispatcher, isolate that version's path.
    const renderIdLinks = (label: string, ids: string[], onPick: (id: string) => void) => (
        ids.length === 0 ? null : (
            <>
                <Divider style={{ margin: '8px 0' }} />
                <Typography.Text strong>{label}</Typography.Text>
                <Space orientation="vertical" size={2} style={{ width: '100%' }}>
                    {ids.map(id => {
                        const name = model.byId.get(id)?.name ?? id
                        return (
                            <Typography.Link key={id} onClick={() => onPick(id)} style={{ display: 'block', maxWidth: '100%' }}>
                                <Typography.Text ellipsis={{ tooltip: name }} style={{ color: 'inherit', maxWidth: '100%' }}>
                                    {name}
                                </Typography.Text>
                            </Typography.Link>
                        )
                    })}
                </Space>
            </>
        )
    )

    // The found cycles, shown as a bar under the graph. Each chip is clickable and highlights the cycle; long chains are
    // truncated with the full path kept in the chip tooltip.
    const renderCyclesBar = () => (
        <div data-testid="table-graph-cycles" style={{ borderTop: '1px solid #f0f0f0', marginTop: 8, paddingTop: 8 }}>
            <Space size={8}>
                <Typography.Text strong>
                    {cycles && cycles.length > 0 ? t('graph:cycles_found', { count: cycles.length }) : t('graph:cycles_none')}
                </Typography.Text>
                {cycles && cycles.length >= CYCLE_SEARCH_LIMIT && (
                    <Typography.Text type="secondary">{t('graph:cycles_more', { count: CYCLE_SEARCH_LIMIT })}</Typography.Text>
                )}
            </Space>
            {cycles && cycles.length > 0 && (
                <div style={{ height: 76, marginTop: 4, overflowY: 'auto' }}>
                    <Space wrap size={4}>
                        {cycles.map((cycle, index) => {
                            const names = cycle.nodes.map(id => model.byId.get(id)?.name ?? id)
                            return (
                                <Tag
                                    key={cycle.id}
                                    color={activeCycle?.id === cycle.id ? 'red' : 'default'}
                                    data-testid={`table-graph-cycle-${index}`}
                                    style={{ cursor: 'pointer', margin: 0, userSelect: 'none' }}
                                    title={cycleTooltip(names)}
                                    onClick={() => {
                                        setActiveCycle(cycle)
                                        setSelectedId(undefined)
                                    }}
                                >
                                    {cycleLabel(names)}
                                </Tag>
                            )
                        })}
                    </Space>
                </div>
            )}
        </div>
    )

    // When a searched name maps to several tables, list them as a bar under the graph so the user can pick which one to
    // focus. Each chip shows the table's location (or signature) and highlights that table on click.
    const renderMatchesBar = () => (
        <div data-testid="table-graph-matches" style={{ borderTop: '1px solid #f0f0f0', marginTop: 8, paddingTop: 8 }}>
            <Typography.Text strong>{t('graph:search_matches', { count: nameMatches.length, name: searchName })}</Typography.Text>
            <div style={{ height: 76, marginTop: 4, overflowY: 'auto' }}>
                <Space wrap size={4}>
                    {nameMatches.map((node, index) => (
                        <Tag
                            key={node.id}
                            color={selectedId === node.id ? 'blue' : 'default'}
                            data-testid={`table-graph-match-${index}`}
                            onClick={() => setSelectedId(node.id)}
                            style={{ cursor: 'pointer', margin: 0, maxWidth: 260, userSelect: 'none' }}
                        >
                            <Typography.Text ellipsis={{ tooltip: candidateTooltip(node) }} style={{ color: 'inherit', maxWidth: 240 }}>
                                {candidateLabel(node)}
                            </Typography.Text>
                        </Tag>
                    ))}
                </Space>
            </div>
        </div>
    )

    // Summary meta read from the backend (signature, return type, location, properties), shown under the table name.
    const renderMeta = (node: GraphNode) => {
        const rows = [
            [t('graph:meta.signature'), node.signature],
            [t('graph:meta.returns'), node.returnType],
            [t('graph:meta.file'), node.file],
            [t('graph:meta.pos'), node.pos],
        ].filter((row): row is [string, string] => Boolean(row[1]))
        const properties = Object.entries(node.properties ?? {})
        if (rows.length === 0 && properties.length === 0) {
            return null
        }
        return (
            <>
                <Divider style={{ margin: '10px 0' }} />
                {rows.length > 0 && (
                    <div style={{ fontSize: 12, display: 'grid', gridTemplateColumns: 'auto 1fr', columnGap: 8, rowGap: 3 }}>
                        {rows.map(([label, value]) => (
                            <React.Fragment key={label}>
                                <Typography.Text type="secondary">{label}</Typography.Text>
                                <Typography.Text ellipsis={{ tooltip: value }} style={{ fontFamily: MONO, fontSize: 11 }}>
                                    {value}
                                </Typography.Text>
                            </React.Fragment>
                        ))}
                    </div>
                )}
                {properties.length > 0 && (
                    <Space wrap size={4} style={{ marginTop: rows.length > 0 ? 8 : 0 }}>
                        {properties.map(([key, value]) => (
                            <Tag key={key} style={{ margin: 0, fontFamily: MONO }}>{`${key}: ${String(value)}`}</Tag>
                        ))}
                    </Space>
                )}
            </>
        )
    }

    const renderNodeInfo = (node: GraphNode) => {
        const isDispatcher = node.kind === DISPATCHER_KIND
        const foreign = !isDispatcher && !canOpenTable(node, currentProjectName)
        const dims = Object.entries(node.dimensionProperties ?? {})
        return (
            <>
                <Typography.Title ellipsis={{ tooltip: node.name }} level={5} style={{ marginTop: 0, marginBottom: 6 }}>
                    {node.name}
                </Typography.Title>
                <Space wrap size={4} style={{ marginBottom: 8 }}>
                    {node.kind && <Tag color={kindColor(node.kind)}>{node.kind}</Tag>}
                    {node.project && <Tag>{node.project}</Tag>}
                </Space>
                {dims.length > 0 && (
                    <div style={{ marginBottom: 8 }}>
                        <Typography.Text style={{ display: 'block', marginBottom: 2 }} type="secondary">
                            {t('graph:meta.business_dimension')}
                        </Typography.Text>
                        <Space wrap size={[4, 4]} style={{ width: '100%' }}>
                            {dims.map(([key, value]) => (
                                <Tag key={key} color="gold" style={{ margin: 0, maxWidth: '100%' }}>
                                    <Typography.Text ellipsis={{ tooltip: `${key}: ${value}` }} style={{ color: 'inherit', maxWidth: 220 }}>
                                        {`${key}: ${value}`}
                                    </Typography.Text>
                                </Tag>
                            ))}
                        </Space>
                    </div>
                )}
                {renderMeta(node)}
                <div style={{ marginTop: 10 }}>
                    {isDispatcher && (
                        <Typography.Paragraph style={{ marginBottom: 8 }} type="secondary">
                            {t('graph:panel.dispatcher_hint')}
                        </Typography.Paragraph>
                    )}
                    {foreign && (
                        <Typography.Paragraph style={{ marginBottom: 8 }} type="secondary">
                            {t('graph:panel.external', { project: node.project })}
                        </Typography.Paragraph>
                    )}
                    {!isDispatcher && !foreign && (
                        <Button
                            icon={<ExportOutlined />}
                            onClick={() => openTable(node.id)}
                            size="small"
                            style={{ marginBottom: 10 }}
                            type="primary"
                        >
                            {t('graph:panel.open')}
                        </Button>
                    )}
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                        <Typography.Text type="secondary">{t('graph:panel.focus')}</Typography.Text>
                        <Space.Compact size="small">
                            <Button onClick={() => setExplore({ id: node.id, direction: 'DEPENDENCIES' })}>
                                {t('graph:panel.focus_uses')}
                            </Button>
                            <Button onClick={() => setExplore({ id: node.id, direction: 'DEPENDENTS' })}>
                                {t('graph:panel.focus_used_by')}
                            </Button>
                            <Button onClick={() => setExplore({ id: node.id, direction: 'BOTH' })}>
                                {t('graph:panel.focus_both')}
                            </Button>
                        </Space.Compact>
                    </div>
                </div>
                {isDispatcher
                    ? renderIdLinks(t('graph:panel.highlight_path'), model.dependencies.get(node.id) ?? [],
                        id => setExplore({ id: node.id, direction: 'DEPENDENCIES', via: id }))
                    : renderIdLinks(t('graph:panel.uses'), selectedUses, setSelectedId)}
                {renderIdLinks(t('graph:panel.dependents'), selectedUsedBy, setSelectedId)}
            </>
        )
    }

    // The schematic's key: decodes every colour and marker actually on the graph, and doubles as a per-kind filter.
    const renderLegend = () => {
        const marker = (swatch: React.ReactNode, label: string) => (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ width: 14, display: 'inline-flex', justifyContent: 'center', flex: '0 0 auto' }}>{swatch}</span>
                <Typography.Text style={{ fontSize: 12 }}>{label}</Typography.Text>
            </div>
        )
        return (
            <div
                data-testid="table-graph-legend"
                style={{
                    position: 'absolute', top: 8, left: 8, zIndex: 2, width: 210, maxHeight: 'calc(100% - 16px)',
                    overflowY: 'auto', background: '#ffffff', border: `1px solid ${HAIRLINE}`, borderRadius: 8,
                    boxShadow: '0 2px 10px rgba(0, 0, 0, 0.08)', padding: '8px 10px',
                }}
            >
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
                    <Typography.Text strong style={{ fontSize: 11, letterSpacing: 0.5 }}>
                        {t('graph:legend.show').toUpperCase()}
                    </Typography.Text>
                    <Button icon={<CloseOutlined />} onClick={() => setLegendOpen(false)} size="small" type="text" />
                </div>
                <Space orientation="vertical" size={2} style={{ width: '100%' }}>
                    {model.kinds.map(kind => {
                        const hidden = hiddenKinds.has(kind)
                        return (
                            <div
                                key={kind}
                                onClick={() => toggleKind(kind)}
                                style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', opacity: hidden ? 0.4 : 1, userSelect: 'none' }}
                            >
                                <span style={{ width: 12, height: 12, borderRadius: 3, background: kindColor(kind), flex: '0 0 auto' }} />
                                <Typography.Text ellipsis style={{ fontSize: 12, flex: 1, textDecoration: hidden ? 'line-through' : 'none' }}>
                                    {kind}
                                </Typography.Text>
                                <Typography.Text style={{ fontSize: 11, fontFamily: MONO }} type="secondary">
                                    {kindCounts.get(kind) ?? 0}
                                </Typography.Text>
                            </div>
                        )
                    })}
                </Space>
                <Divider style={{ margin: '8px 0' }} />
                <Space orientation="vertical" size={4} style={{ width: '100%' }}>
                    {kindCounts.has(DISPATCHER_KIND)
                        && marker(<span style={{ width: 11, height: 11, border: `2px solid ${DISPATCHER_ACCENT}` }} />, t('graph:legend.dispatcher'))}
                    {model.stats.isolated > 0
                        && marker(<span style={{ width: 11, height: 11, border: `2px dashed ${PROBLEM_ACCENT}` }} />, t('graph:legend.isolated'))}
                    {marker(<span style={{ width: 14, height: 0, borderTop: `2px dashed ${PROBLEM_ACCENT}` }} />, t('graph:legend.cycle'))}
                    {marker(
                        <span style={{ display: 'inline-flex', gap: 2, alignItems: 'center' }}>
                            <span style={{ width: 7, height: 11, border: '1px solid #999' }} />
                            <span style={{ width: 7, height: 11, border: '3px solid #555' }} />
                        </span>,
                        t('graph:legend.weight')
                    )}
                </Space>
                <Typography.Text style={{ fontSize: 11, display: 'block', marginTop: 8 }} type="secondary">
                    {t('graph:legend.hint')}
                </Typography.Text>
            </div>
        )
    }

    return (
        <Modal
            destroyOnHidden
            footer={null}
            onCancel={handleClose}
            open={visible}
            width="92vw"
            title={
                <Space wrap align="center" size={10}>
                    <Space size={8}>
                        <PartitionOutlined />
                        {t('graph:title')}
                    </Space>
                    {hasGraph && renderVitalSigns()}
                </Space>
            }
        >
            <Spin spinning={loading}>
                {!hasGraph ? (
                    <Empty description={error ? t('graph:load_failed') : t('graph:empty')} />
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', height: '74vh' }}>
                        <Space wrap align="center" size={12} style={{ marginBottom: 8 }}>
                            <Select
                                allowClear
                                data-testid="table-graph-search"
                                options={nodeOptions}
                                placeholder={t('graph:search_placeholder')}
                                showSearch={{ optionFilterProp: 'label' }}
                                style={{ width: 260 }}
                                value={selected?.name ?? searchName}
                                onChange={name => {
                                    setSearchName(name)
                                    setActiveCycle(undefined)
                                    const matches = name ? byName.get(name) ?? [] : []
                                    setSelectedId(matches.length === 1 ? matches[0]?.id : undefined)
                                }}
                            />
                            <Space.Compact>
                                <Tooltip title={t('graph:fit')}>
                                    <Button icon={<AimOutlined />} onClick={() => cyRef.current?.fit(undefined, 30)} />
                                </Tooltip>
                                <Tooltip title={t('graph:zoom_in')}>
                                    <Button icon={<ZoomInOutlined />} onClick={() => zoomBy(1.2)} />
                                </Tooltip>
                                <Tooltip title={t('graph:zoom_out')}>
                                    <Button icon={<ZoomOutOutlined />} onClick={() => zoomBy(1 / 1.2)} />
                                </Tooltip>
                            </Space.Compact>
                            <Button
                                data-testid="table-graph-find-cycles"
                                icon={<RetweetOutlined />}
                                type={cycles === null ? 'default' : 'primary'}
                                onClick={() => {
                                    if (cycles === null) {
                                        setCycles(findCycles(model.dependencies, 2, CYCLE_SEARCH_LIMIT))
                                    } else {
                                        setCycles(null)
                                        setActiveCycle(undefined)
                                    }
                                }}
                            >
                                {t('graph:find_cycles')}
                            </Button>
                            <Button
                                icon={<BgColorsOutlined />}
                                onClick={() => setLegendOpen(open => !open)}
                                type={legendOpen ? 'primary' : 'default'}
                            >
                                {t('graph:legend.show')}
                            </Button>
                            {explore && (
                                <Button icon={<RollbackOutlined />} onClick={() => setExplore(undefined)}>
                                    {t('graph:panel.back')}
                                </Button>
                            )}
                        </Space>
                        <div style={{ display: 'flex', flex: 1, minHeight: 0, gap: 8 }}>
                            <div style={{ position: 'relative', flex: 1, minWidth: 0 }}>
                                <div
                                    ref={containerRef}
                                    data-testid="table-graph"
                                    style={{
                                        width: '100%', height: '100%', border: `1px solid ${HAIRLINE}`, borderRadius: 8,
                                        background: CANVAS_BG, backgroundImage: DOT_GRID, backgroundSize: '18px 18px',
                                    }}
                                />
                                {legendOpen && renderLegend()}
                            </div>
                            {selected && (
                                <div style={{ width: 296, overflowY: 'auto', paddingLeft: 12, borderLeft: `1px solid ${HAIRLINE}` }}>
                                    {renderNodeInfo(selected)}
                                </div>
                            )}
                        </div>
                        {nameMatches.length > 1 && renderMatchesBar()}
                        {cycles !== null && renderCyclesBar()}
                    </div>
                )}
            </Spin>
        </Modal>
    )
}
