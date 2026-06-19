import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Divider, Empty, Modal, Select, Space, Spin, Tag, Tooltip, Typography } from 'antd'
import {
    AimOutlined,
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

// Only technical/auxiliary kinds are worth excluding; content kinds (Rules, Spreadsheet…) always stay visible.
const FILTERABLE_KINDS = [DISPATCHER_KIND, 'Test']

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

const GRAPH_LAYOUT = {
    name: 'dagre',
    rankDir: 'LR',
    nodeSep: 18,
    rankSep: 70,
    animate: false,
} as unknown as cytoscape.LayoutOptions

const buildStyle = (maxWeight: number) => [
    {
        selector: 'node',
        style: {
            'background-color': 'data(color)',
            'label': 'data(label)',
            'color': '#ffffff',
            'font-size': 10,
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
        style: { 'border-width': 3, 'border-style': 'dashed', 'border-color': '#f5222d', 'border-opacity': 1 },
    },
    {
        // the technical dispatcher table that selects one overloaded version: a distinct cut-rectangle with a gold frame
        selector: 'node.dispatcher',
        style: { 'shape': 'cut-rectangle', 'border-width': 3, 'border-color': '#ffc53d', 'border-opacity': 1 },
    },
    {
        selector: 'node.highlighted',
        style: { 'border-width': 4, 'border-style': 'solid', 'border-color': '#fa8c16', 'border-opacity': 1 },
    },
    { selector: '.faded', style: { 'opacity': 0.1 } },
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
        style: { 'line-color': '#f5222d', 'target-arrow-color': '#f5222d', 'line-style': 'dashed', 'width': 2 },
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
            'line-color': '#f5222d',
            'target-arrow-color': '#f5222d',
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
}

/**
 * Opens the tapped table in the editor via the backend-resolved URL. The backend returns a page-relative fragment
 * (e.g. {@code #repo/project/module/table}) that the editor shell hosting this modal resolves on hash change, so it is
 * navigated as-is — prefixing the origin would leave the editor page and drop the context path.
 */
const openTable = (id: string): void => {
    apiCall(`/compile/table/${id}/url`, { method: 'GET' }, GRAPH_API_OPTIONS)
        .then((data: { url?: string | null }) => {
            if (data?.url) {
                globalThis.location.href = `${data.url}?id=${id}`
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

    const containerRef = useRef<HTMLDivElement>(null)
    const cyRef = useRef<Core | null>(null)

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

        let lastTap = { id: '', time: 0 }
        cy.on('tap', 'node', event => {
            const id = event.target.id()
            const now = Date.now()
            setSelectedId(id)
            setSearchName(undefined)
            setActiveCycle(undefined)
            if (id === lastTap.id && now - lastTap.time < 350) {
                openTable(id)
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
    }, [selectedId, activeCycle])

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
    const filterableKinds = model.kinds.filter(kind => FILTERABLE_KINDS.includes(kind))
    const statsText = [
        t('graph:stats', model.stats),
        model.stats.cyclic > 0 ? t('graph:stats_cyclic', model.stats) : null,
        model.stats.isolated > 0 ? t('graph:stats_isolated', model.stats) : null,
    ].filter(Boolean).join(' · ')

    // A titled list of linked table names. The pick handler decides what choosing one does — focus it (uses / used by),
    // or, for a dispatcher, isolate that version's path.
    const renderIdLinks = (label: string, ids: string[], onPick: (id: string) => void) => (
        ids.length === 0 ? null : (
            <>
                <Divider style={{ margin: '8px 0' }} />
                <Typography.Text strong>{label}</Typography.Text>
                <Space orientation="vertical" size={2} style={{ width: '100%' }}>
                    {ids.map(id => (
                        <Typography.Link key={id} ellipsis onClick={() => onPick(id)}>
                            {model.byId.get(id)?.name ?? id}
                        </Typography.Link>
                    ))}
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
            <div style={{ fontSize: 12, marginBottom: 8 }}>
                {rows.map(([label, value]) => (
                    <div key={label}>
                        <Typography.Text type="secondary">{label}: </Typography.Text>
                        <Typography.Text ellipsis={{ tooltip: value }}>{value}</Typography.Text>
                    </div>
                ))}
                {properties.length > 0 && (
                    <Space wrap size={4} style={{ marginTop: 2 }}>
                        {properties.map(([key, value]) => (
                            <Tag key={key} style={{ margin: 0 }}>{`${key}: ${String(value)}`}</Tag>
                        ))}
                    </Space>
                )}
            </div>
        )
    }

    const renderNodeInfo = (node: GraphNode) => (
        <>
            <Typography.Title ellipsis level={5} style={{ marginTop: 0 }}>
                {node.name}
            </Typography.Title>
            <Space wrap size={4} style={{ marginBottom: 8 }}>
                {node.kind && <Tag color={kindColor(node.kind)}>{node.kind}</Tag>}
                {node.project && <Tag>{node.project}</Tag>}
            </Space>
            {node.dimensionProperties && Object.keys(node.dimensionProperties).length > 0 && (
                <div style={{ marginBottom: 8 }}>
                    <Typography.Text style={{ display: 'block' }} type="secondary">
                        {t('graph:meta.business_dimension')}
                    </Typography.Text>
                    <Space wrap size={[4, 4]} style={{ marginTop: 2, width: '100%' }}>
                        {Object.entries(node.dimensionProperties).map(([key, value]) => (
                            <Tag key={key} color="gold" style={{ margin: 0, maxWidth: '100%' }}>
                                <Typography.Text
                                    ellipsis={{ tooltip: `${key}: ${value}` }}
                                    style={{ color: 'inherit', maxWidth: 220 }}
                                >
                                    {`${key}: ${value}`}
                                </Typography.Text>
                            </Tag>
                        ))}
                    </Space>
                </div>
            )}
            {renderMeta(node)}
            {node.kind === DISPATCHER_KIND ? (
                <Typography.Paragraph style={{ marginBottom: 0 }} type="secondary">
                    {t('graph:panel.dispatcher_hint')}
                </Typography.Paragraph>
            ) : (
                <Space wrap size={4}>
                    <Button icon={<ExportOutlined />} onClick={() => openTable(node.id)} size="small" type="primary">
                        {t('graph:panel.open')}
                    </Button>
                </Space>
            )}
            <div style={{ marginTop: 8 }}>
                <Typography.Text type="secondary">{t('graph:panel.explore')}: </Typography.Text>
                <Typography.Link onClick={() => setExplore({ id: node.id, direction: 'DEPENDENCIES' })}>
                    {t('graph:panel.explore_uses')}
                </Typography.Link>
                {' · '}
                <Typography.Link onClick={() => setExplore({ id: node.id, direction: 'DEPENDENTS' })}>
                    {t('graph:panel.explore_used_by')}
                </Typography.Link>
                {' · '}
                <Typography.Link onClick={() => setExplore({ id: node.id, direction: 'BOTH' })}>
                    {t('graph:panel.explore_both')}
                </Typography.Link>
            </div>
            {node.kind === DISPATCHER_KIND
                ? renderIdLinks(t('graph:panel.highlight_path'), model.dependencies.get(node.id) ?? [],
                    id => setExplore({ id: node.id, direction: 'DEPENDENCIES', via: id }))
                : renderIdLinks(t('graph:panel.uses'), selectedUses, setSelectedId)}
            {renderIdLinks(t('graph:panel.dependents'), selectedUsedBy, setSelectedId)}
        </>
    )

    return (
        <Modal
            destroyOnHidden
            footer={null}
            onCancel={handleClose}
            open={visible}
            width="92vw"
            title={
                <Space>
                    <PartitionOutlined />
                    {t('graph:title')}
                </Space>
            }
        >
            <Spin spinning={loading}>
                {!hasGraph ? (
                    <Empty description={error ? t('graph:load_failed') : t('graph:empty')} />
                ) : (
                    <div style={{ display: 'flex', flexDirection: 'column', height: '74vh' }}>
                        <Space wrap style={{ marginBottom: 8 }}>
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
                            <Tooltip title={t('graph:fit')}>
                                <Button icon={<AimOutlined />} onClick={() => cyRef.current?.fit(undefined, 30)} />
                            </Tooltip>
                            <Tooltip title={t('graph:zoom_in')}>
                                <Button icon={<ZoomInOutlined />} onClick={() => zoomBy(1.2)} />
                            </Tooltip>
                            <Tooltip title={t('graph:zoom_out')}>
                                <Button icon={<ZoomOutOutlined />} onClick={() => zoomBy(1 / 1.2)} />
                            </Tooltip>
                            <Tooltip title={t('graph:find_cycles')}>
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
                                />
                            </Tooltip>
                            <Typography.Text type="secondary">{statsText}</Typography.Text>
                        </Space>
                        {(filterableKinds.length > 0 || explore) && (
                            <Space wrap size={4} style={{ marginBottom: 8 }}>
                                {filterableKinds.map(kind => (
                                    <Tag
                                        key={kind}
                                        color={hiddenKinds.has(kind) ? 'default' : kindColor(kind)}
                                        onClick={() => toggleKind(kind)}
                                        style={{ cursor: 'pointer', opacity: hiddenKinds.has(kind) ? 0.45 : 1, userSelect: 'none' }}
                                    >
                                        {kind}
                                    </Tag>
                                ))}
                                {explore && (
                                    <Button icon={<RollbackOutlined />} onClick={() => setExplore(undefined)} size="small">
                                        {t('graph:panel.back')}
                                    </Button>
                                )}
                            </Space>
                        )}
                        <div style={{ display: 'flex', flex: 1, minHeight: 0, gap: 8 }}>
                            <div
                                ref={containerRef}
                                data-testid="table-graph"
                                style={{ flex: 1, minWidth: 0, border: '1px solid #f0f0f0', borderRadius: 6 }}
                            />
                            {selected && (
                                <div style={{ width: 260, overflowY: 'auto', paddingLeft: 8, borderLeft: '1px solid #f0f0f0' }}>
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
