import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Divider, Empty, Modal, Select, Space, Spin, Tag, Tooltip, Typography } from 'antd'
import {
    AimOutlined,
    ExportOutlined,
    PartitionOutlined,
    RollbackOutlined,
    ZoomInOutlined,
    ZoomOutOutlined,
} from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import cytoscape, { type Core } from 'cytoscape'
import dagre from 'cytoscape-dagre'
import { useGlobalEvents } from '../hooks'
import { apiCall, type ApiCallOptions } from '../services'
import { buildGraphModel, type GraphNode, kindColor } from './tableGraph'

let extensionsRegistered = false
if (!extensionsRegistered) {
    cytoscape.use(dagre)
    extensionsRegistered = true
}

const GRAPH_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

type Direction = 'DEPENDENCIES' | 'DEPENDENTS' | 'BOTH'

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

/** Opens the tapped table in the editor via the backend-resolved URL. */
const openTable = (id: string): void => {
    apiCall(`/compile/table/${id}/url`, { method: 'GET' }, GRAPH_API_OPTIONS)
        .then((data: { url?: string | null }) => {
            if (data?.url) {
                globalThis.location.href = `${globalThis.location.origin}/${data.url}?id=${id}`
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
    const [explore, setExplore] = useState<{ id: string, direction: Direction }>()
    const [selectedId, setSelectedId] = useState<string>()

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
        setExplore(undefined)
        setHiddenKinds(new Set())
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
    const nodeOptions = useMemo(() => nodes.map(node => ({ label: node.name, value: node.id })), [nodes])
    const maxWeight = useMemo(() => Math.max(1, ...[...model.dependents.values()].map(list => list.length)), [model])

    // Nodes that are currently shown, taking the kind filter and the "show only" exploration into account.
    const visibleIds = useMemo(() => {
        const reachable = (start: string): Set<string> => {
            const seen = new Set([start])
            const queue = [start]
            while (queue.length) {
                const current = queue.shift() as string
                const next: string[] = []
                if (explore?.direction !== 'DEPENDENTS') {
                    next.push(...(model.dependencies.get(current) ?? []))
                }
                if (explore?.direction !== 'DEPENDENCIES') {
                    next.push(...(model.dependents.get(current) ?? []))
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
        })
    }, [visibleIds])

    // Focus the selected table: highlight it, fade everything outside its neighbourhood, and centre on it.
    useEffect(() => {
        const cy = cyRef.current
        if (!cy) {
            return
        }
        cy.elements().removeClass('faded highlighted')
        if (!selectedId) {
            return
        }
        const node = cy.getElementById(selectedId)
        if (node.empty()) {
            return
        }
        const neighbourhood = node.closedNeighborhood()
        cy.elements().not(neighbourhood).not('.hidden').addClass('faded')
        node.addClass('highlighted')
        cy.animate({ center: { eles: node }, zoom: Math.max(cy.zoom(), 1) }, { duration: 300 })
    }, [selectedId])

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
    const hasGraph = !loading && !error && model.elements.length > 0

    const renderRelationSection = (label: string, ids: string[]) => (
        ids.length === 0 ? null : (
            <>
                <Divider style={{ margin: '8px 0' }} />
                <Typography.Text strong>{label}</Typography.Text>
                <Space orientation="vertical" size={2} style={{ width: '100%' }}>
                    {ids.map(id => (
                        <Typography.Link key={id} ellipsis onClick={() => setSelectedId(id)}>
                            {model.byId.get(id)?.name ?? id}
                        </Typography.Link>
                    ))}
                </Space>
            </>
        )
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
                                onChange={setSelectedId}
                                options={nodeOptions}
                                placeholder={t('graph:search_placeholder')}
                                showSearch={{ optionFilterProp: 'label' }}
                                style={{ width: 260 }}
                                value={selectedId}
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
                            <Typography.Text type="secondary">{t('graph:stats', model.stats)}</Typography.Text>
                        </Space>
                        <Space wrap size={4} style={{ marginBottom: 8 }}>
                            {model.kinds.map(kind => (
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
                        <div style={{ display: 'flex', flex: 1, minHeight: 0, gap: 8 }}>
                            <div
                                ref={containerRef}
                                data-testid="table-graph"
                                style={{ flex: 1, minWidth: 0, border: '1px solid #f0f0f0', borderRadius: 6 }}
                            />
                            {selected && (
                                <div style={{ width: 260, overflowY: 'auto', paddingLeft: 8, borderLeft: '1px solid #f0f0f0' }}>
                                    <Typography.Title ellipsis level={5} style={{ marginTop: 0 }}>
                                        {selected.name}
                                    </Typography.Title>
                                    <Space wrap size={4} style={{ marginBottom: 8 }}>
                                        {selected.kind && <Tag color={kindColor(selected.kind)}>{selected.kind}</Tag>}
                                        {selected.project && <Tag>{selected.project}</Tag>}
                                    </Space>
                                    <Space wrap size={4}>
                                        <Button icon={<ExportOutlined />} onClick={() => openTable(selected.id)} size="small" type="primary">
                                            {t('graph:panel.open')}
                                        </Button>
                                    </Space>
                                    <div style={{ marginTop: 8 }}>
                                        <Typography.Text type="secondary">{t('graph:panel.explore')}: </Typography.Text>
                                        <Typography.Link onClick={() => setExplore({ id: selected.id, direction: 'DEPENDENCIES' })}>
                                            {t('graph:panel.explore_uses')}
                                        </Typography.Link>
                                        {' · '}
                                        <Typography.Link onClick={() => setExplore({ id: selected.id, direction: 'DEPENDENTS' })}>
                                            {t('graph:panel.explore_used_by')}
                                        </Typography.Link>
                                        {' · '}
                                        <Typography.Link onClick={() => setExplore({ id: selected.id, direction: 'BOTH' })}>
                                            {t('graph:panel.explore_both')}
                                        </Typography.Link>
                                    </div>
                                    {renderRelationSection(t('graph:panel.dependencies'), model.dependencies.get(selected.id) ?? [])}
                                    {renderRelationSection(t('graph:panel.dependents'), model.dependents.get(selected.id) ?? [])}
                                </div>
                            )}
                        </div>
                    </div>
                )}
            </Spin>
        </Modal>
    )
}
