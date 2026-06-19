import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Empty, Modal, Space, Spin } from 'antd'
import { PartitionOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import cytoscape, { type Core, type ElementDefinition } from 'cytoscape'
import { useGlobalEvents } from '../hooks'
import { apiCall, type ApiCallOptions } from '../services'

const GRAPH_API_OPTIONS: ApiCallOptions = { throwError: true, suppressErrorPages: true }

/**
 * A single table of the dependency graph, as returned by {@code GET /projects/{id}/tables/graph}.
 */
interface TableNode {
    id: string
    name: string
    kind?: string
    project?: string
    dependencies?: string[]
    dependents?: string[]
}

/**
 * Detail passed from the legacy JSF page via the {@code openTableGraphModal} event.
 */
export interface TableGraphModalDetail {
    projectId: string
}

/**
 * Converts the flat list of graph nodes into Cytoscape elements. Edges are drawn from a table to each table it
 * depends on; dependency references that point outside the returned node set are skipped.
 */
const toElements = (nodes: TableNode[]): ElementDefinition[] => {
    const ids = new Set(nodes.map(node => node.id))
    const elements: ElementDefinition[] = nodes.map(node => ({
        data: { id: node.id, label: node.name, kind: node.kind ?? '' },
    }))
    nodes.forEach(node => {
        (node.dependencies ?? [])
            .filter(dependency => ids.has(dependency))
            .forEach(dependency => {
                elements.push({ data: { id: `${node.id}->${dependency}`, source: node.id, target: dependency } })
            })
    })
    return elements
}

const GRAPH_STYLE = [
    {
        selector: 'node',
        style: {
            'label': 'data(label)',
            'font-size': 11,
            'background-color': '#1677ff',
            'color': '#ffffff',
            'text-valign': 'center',
            'text-halign': 'center',
            'text-wrap': 'wrap',
            'text-max-width': '140px',
            'shape': 'round-rectangle',
            'width': 'label',
            'height': 'label',
            'padding': '8px',
        },
    },
    {
        selector: 'edge',
        style: {
            'width': 1.5,
            'line-color': '#bfbfbf',
            'target-arrow-color': '#bfbfbf',
            'target-arrow-shape': 'triangle',
            'curve-style': 'bezier',
        },
    },
] as unknown as cytoscape.StylesheetCSS[]

const GRAPH_LAYOUT = { name: 'breadthfirst', directed: true, padding: 24, spacingFactor: 1.25 } as cytoscape.LayoutOptions

/**
 * TableGraphModal renders the dependency graph of the current project's tables with Cytoscape.
 *
 * @example open it by dispatching a custom event from the legacy UI:
 * globalThis.dispatchEvent(new CustomEvent('openTableGraphModal', {detail: {projectId}}))
 */
export const TableGraphModal: React.FC = () => {
    const { t } = useTranslation()
    const { detail } = useGlobalEvents<TableGraphModalDetail>('openTableGraphModal')

    const [visible, setVisible] = useState(false)
    const [loading, setLoading] = useState(false)
    const [nodes, setNodes] = useState<TableNode[]>([])

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
        setNodes([])
        // The id may arrive in the standard Base64 alphabet; normalize to the URL-safe form so it never
        // contains a slash, which servlet containers reject in a path segment (the backend decodes both).
        const projectId = detail.projectId.replaceAll('+', '-').replaceAll('/', '_')
        apiCall(`/projects/${projectId}/tables/graph`, { method: 'GET' }, GRAPH_API_OPTIONS)
            .then((data: TableNode[]) => {
                if (!cancelled) {
                    setNodes(Array.isArray(data) ? data : [])
                }
            })
            .catch(() => {
                if (!cancelled) {
                    setNodes([])
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

    const elements = useMemo(() => toElements(nodes), [nodes])

    useEffect(() => {
        if (!visible || loading || !containerRef.current || elements.length === 0) {
            return
        }
        const cy = cytoscape({
            container: containerRef.current,
            elements,
            style: GRAPH_STYLE,
            layout: GRAPH_LAYOUT,
        })
        cyRef.current = cy
        // Resolve the table's editor URL from the backend (webStudio.url("table", uri)), then open it.
        cy.on('tap', 'node', event => {
            const id = event.target.id()
            apiCall(`/compile/table/${id}/url`, { method: 'GET' }, GRAPH_API_OPTIONS)
                .then((data: { url?: string | null }) => {
                    if (data?.url) {
                        globalThis.location.href = `${globalThis.location.origin}/${data.url}?id=${id}`
                    }
                })
                .catch(() => undefined)
        })
        return () => {
            cy.destroy()
            cyRef.current = null
        }
    }, [visible, loading, elements])

    const handleClose = useCallback(() => {
        globalThis.dispatchEvent(new CustomEvent('openTableGraphModal', { detail: null }))
    }, [])

    return (
        <Modal
            destroyOnHidden
            footer={null}
            onCancel={handleClose}
            open={visible}
            width="90vw"
            title={
                <Space>
                    <PartitionOutlined />
                    {t('graph:title')}
                </Space>
            }
        >
            <Spin spinning={loading}>
                {!loading && elements.length === 0 ? (
                    <Empty description={t('graph:empty')} />
                ) : (
                    <div ref={containerRef} data-testid="table-graph" style={{ height: '70vh', width: '100%' }} />
                )}
            </Spin>
        </Modal>
    )
}
