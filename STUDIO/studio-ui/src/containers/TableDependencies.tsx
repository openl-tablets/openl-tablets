import React, { useEffect, useRef, useState, useCallback } from 'react'
import { Card, Radio, Spin, Alert, Space, Typography } from 'antd'
import { Network } from 'vis-network/standalone'
import { apiCall } from '../services/apiCall'
import { Table, DependencyDirection } from '../types/tables'
import './TableDependencies.scss'

const { Title, Text } = Typography

// Translation helper with fallback
const useTableDependenciesTranslations = () => {
    return {
        title: 'Table Dependencies',
        description: 'Visualize dependencies between tables. Click on a table to view its relationships.',
        loading: 'Loading tables...',
        error: 'Error loading tables',
        noTables: 'No tables found',
        noTablesDescription: 'There are no tables in the current project.',
        table: 'Table',
        allTables: 'All Tables',
        direction: 'Direction',
        dependsOn: 'Depends On',
        dependedBy: 'Depended By'
    }
}

export const TableDependencies = () => {
    const t = useTableDependenciesTranslations()
    const networkRef = useRef<HTMLDivElement>(null)
    const networkInstance = useRef<Network | null>(null)
    const [tables, setTables] = useState<Table[]>([])
    const [selectedTableId, setSelectedTableId] = useState<string | null>(null)
    const [dependencyDirection, setDependencyDirection] = useState<DependencyDirection>('dependsOn')
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    // Build reverse dependencies map (who depends on this table)
    const buildDependentsMap = useCallback((tables: Table[]): Map<string, Set<string>> => {
        const dependentsMap = new Map<string, Set<string>>()

        tables.forEach(table => {
            if (!dependentsMap.has(table.id)) {
                dependentsMap.set(table.id, new Set())
            }

            table.dependencies.forEach(depId => {
                if (!dependentsMap.has(depId)) {
                    dependentsMap.set(depId, new Set())
                }
                dependentsMap.get(depId)!.add(table.id)
            })
        })

        return dependentsMap
    }, [])

    // Fetch tables data
    useEffect(() => {
        const fetchTables = async () => {
            try {
                setLoading(true)
                setError(null)
                const data = await apiCall<Table[]>('/project/tables')
                setTables(data || [])
            } catch (err) {
                setError(err instanceof Error ? err.message : 'Failed to load tables')
            } finally {
                setLoading(false)
            }
        }

        fetchTables()
    }, [])

    // Initialize or update the network graph
    useEffect(() => {
        if (!networkRef.current || tables.length === 0) {
            return
        }

        const dependentsMap = buildDependentsMap(tables)

        // Prepare nodes and edges based on selected table and direction
        let nodes: { id: string; label: string; color?: string; font?: { size: number } }[] = []
        let edges: { from: string; to: string; arrows?: string }[] = []

        if (selectedTableId) {
            const selectedTable = tables.find(t => t.id === selectedTableId)
            if (!selectedTable) return

            const relatedTableIds = new Set<string>()
            relatedTableIds.add(selectedTableId)

            if (dependencyDirection === 'dependsOn') {
                // Show tables that selected table depends on
                selectedTable.dependencies.forEach(depId => {
                    relatedTableIds.add(depId)
                    edges.push({ from: selectedTableId, to: depId, arrows: 'to' })
                })
            } else {
                // Show tables that depend on selected table
                const dependents = dependentsMap.get(selectedTableId) || new Set()
                dependents.forEach(depId => {
                    relatedTableIds.add(depId)
                    edges.push({ from: depId, to: selectedTableId, arrows: 'to' })
                })
            }

            nodes = Array.from(relatedTableIds).map(tableId => {
                const table = tables.find(t => t.id === tableId)
                return {
                    id: tableId,
                    label: table?.name || tableId,
                    color: tableId === selectedTableId ? '#1890ff' : undefined,
                    font: { size: tableId === selectedTableId ? 16 : 14 }
                }
            })
        } else {
            // Show all tables and dependencies
            nodes = tables.map(table => ({
                id: table.id,
                label: table.name
            }))

            tables.forEach(table => {
                table.dependencies.forEach(depId => {
                    edges.push({ from: table.id, to: depId, arrows: 'to' })
                })
            })
        }

        // Vis.js network options
        const options = {
            nodes: {
                shape: 'box',
                margin: 10,
                widthConstraint: {
                    maximum: 200
                },
                font: {
                    size: 14
                }
            },
            edges: {
                smooth: {
                    type: 'cubicBezier',
                    forceDirection: 'horizontal',
                    roundness: 0.4
                },
                color: {
                    inherit: false,
                    color: '#848484'
                }
            },
            layout: {
                hierarchical: {
                    enabled: selectedTableId !== null,
                    direction: 'LR',
                    sortMethod: 'directed',
                    levelSeparation: 200,
                    nodeSpacing: 150
                }
            },
            physics: {
                enabled: selectedTableId === null,
                barnesHut: {
                    gravitationalConstant: -8000,
                    centralGravity: 0.3,
                    springLength: 200,
                    springConstant: 0.04,
                    damping: 0.09,
                    avoidOverlap: 0.5
                },
                stabilization: {
                    iterations: 200
                }
            },
            interaction: {
                hover: true,
                navigationButtons: true,
                keyboard: true
            }
        }

        // Destroy previous network instance
        if (networkInstance.current) {
            networkInstance.current.destroy()
        }

        // Create new network
        const network = new Network(
            networkRef.current,
            { nodes, edges },
            options
        )

        // Handle node click
        network.on('click', (params) => {
            if (params.nodes.length > 0) {
                const clickedNodeId = params.nodes[0] as string
                const clickedTable = tables.find(t => t.id === clickedNodeId)
                if (clickedTable) {
                    window.open(clickedTable.url, '_blank')
                }
            }
        })

        networkInstance.current = network

        return () => {
            if (networkInstance.current) {
                networkInstance.current.destroy()
                networkInstance.current = null
            }
        }
    }, [tables, selectedTableId, dependencyDirection, buildDependentsMap])

    const handleTableSelect = (tableId: string | null) => {
        setSelectedTableId(tableId)
    }

    if (loading) {
        return (
            <div className="table-dependencies-loading">
                <Spin size="large" tip={t.loading} />
            </div>
        )
    }

    if (error) {
        return (
            <div className="table-dependencies-error">
                <Alert
                    message={t.error}
                    description={error}
                    type="error"
                    showIcon
                />
            </div>
        )
    }

    if (tables.length === 0) {
        return (
            <div className="table-dependencies-empty">
                <Alert
                    message={t.noTables}
                    description={t.noTablesDescription}
                    type="info"
                    showIcon
                />
            </div>
        )
    }

    return (
        <div className="table-dependencies">
            <Card>
                <Space direction="vertical" size="large" style={{ width: '100%' }}>
                    <div className="table-dependencies-header">
                        <Title level={3}>{t.title}</Title>
                        <Text type="secondary">{t.description}</Text>
                    </div>

                    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
                        <div className="table-dependencies-controls">
                            <Space direction="horizontal" size="large">
                                <div>
                                    <Text strong>{t.table}:</Text>
                                    <Radio.Group
                                        value={selectedTableId || 'all'}
                                        onChange={(e) => handleTableSelect(e.target.value === 'all' ? null : e.target.value)}
                                        style={{ marginLeft: 8 }}
                                    >
                                        <Radio.Button value="all">{t.allTables}</Radio.Button>
                                        {tables.map(table => (
                                            <Radio.Button key={table.id} value={table.id}>
                                                {table.name}
                                            </Radio.Button>
                                        ))}
                                    </Radio.Group>
                                </div>

                                {selectedTableId && (
                                    <div>
                                        <Text strong>{t.direction}:</Text>
                                        <Radio.Group
                                            value={dependencyDirection}
                                            onChange={(e) => setDependencyDirection(e.target.value)}
                                            style={{ marginLeft: 8 }}
                                        >
                                            <Radio.Button value="dependsOn">
                                                {t.dependsOn}
                                            </Radio.Button>
                                            <Radio.Button value="dependedBy">
                                                {t.dependedBy}
                                            </Radio.Button>
                                        </Radio.Group>
                                    </div>
                                )}
                            </Space>
                        </div>

                        <div className="table-dependencies-graph" ref={networkRef} />
                    </Space>
                </Space>
            </Card>
        </div>
    )
}
