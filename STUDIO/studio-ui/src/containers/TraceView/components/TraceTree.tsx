import React, { useCallback, useMemo } from 'react'
import { Tree, Checkbox, Tooltip, Button, Empty, Spin } from 'antd'
import type { TreeProps, TreeDataNode } from 'antd'
import { DownloadOutlined } from '@ant-design/icons'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import traceService from 'services/traceService'
import { getTraceIcon, getNodeClassName, parseNodeStatus } from './TraceIcons'
import type { TraceTreeDataNode } from 'types/trace'
import './TraceTree.scss'

interface TraceTreeProps {
    onSelect?: (nodeId: number) => void
}

/**
 * Left panel tree component for navigating trace nodes.
 * Uses Ant Design Tree with lazy loading.
 */
const TraceTree: React.FC<TraceTreeProps> = ({ onSelect }) => {
    const { t } = useTranslation('trace')
    const {
        treeData,
        loading,
        selectedTreeKey,
        hideFailedNodes,
        toggleHideFailedNodes,
        fetchNodeChildren,
        selectNode,
        projectId,
        showRealNumbers,
        executionStatus,
    } = useTraceStore()

    /**
     * Filter tree data based on hideFailedNodes setting.
     */
    const filterTreeData = useCallback(
        (nodes: TraceTreeDataNode[]): TraceTreeDataNode[] => {
            if (!hideFailedNodes) return nodes

            return nodes
                .filter((node) => {
                    const status = parseNodeStatus(node.extraClasses)
                    return !status.isFail && !status.isNoResult
                })
                .map((node) => ({
                    ...node,
                    children: node.children
                        ? filterTreeData(node.children)
                        : undefined,
                }))
        },
        [hideFailedNodes]
    )

    /**
     * Transform TraceTreeDataNode to Ant Design TreeDataNode.
     * Stores nodeId in the data-node-id attribute for retrieval during events.
     */
    const transformNode = useCallback(
        (node: TraceTreeDataNode): TreeDataNode & { nodeId: number } => {
            const nodeClassName = getNodeClassName(node.extraClasses, node.nodeData?.error)
            return {
                key: node.key,
                nodeId: node.nodeId,
                title: (
                    <Tooltip title={node.tooltip} mouseEnterDelay={0.5}>
                        <span className={`trace-node-title ${nodeClassName}`}>
                            {node.title}
                        </span>
                    </Tooltip>
                ),
                icon: getTraceIcon(node.type, node.extraClasses),
                isLeaf: node.isLeaf,
                children: node.children?.map(transformNode),
                className: nodeClassName,
            }
        },
        []
    )

    const displayData = useMemo(() => {
        const filtered = filterTreeData(treeData)
        return filtered.map(transformNode)
    }, [treeData, filterTreeData, transformNode])

    /**
     * Handle lazy loading of node children.
     * Uses both treeKey (path-based) for tree structure and nodeId for API call.
     */
    const handleLoadData: TreeProps['loadData'] = async (treeNode) => {
        const treeKey = treeNode.key as string
        const nodeId = (treeNode as any).nodeId as number
        await fetchNodeChildren(treeKey, nodeId)
    }

    /**
     * Handle node selection.
     * Extracts nodeId from the path-based key (last segment after last '-').
     */
    const handleSelect: TreeProps['onSelect'] = (selectedKeys) => {
        if (selectedKeys.length > 0) {
            const treeKey = selectedKeys[0] as string
            // Extract nodeId from path-based key (e.g., "24-25" -> 25, "25" -> 25)
            const keyParts = treeKey.split('-')
            const nodeId = parseInt(keyParts[keyParts.length - 1], 10)
            selectNode(treeKey, nodeId)
            onSelect?.(nodeId)
        }
    }

    if (loading) {
        return (
            <div className="trace-tree-loading">
                <Spin tip={t('loading')} />
            </div>
        )
    }

    return (
        <div className="trace-tree">
            <div className="trace-tree-options">
                <Checkbox
                    checked={!hideFailedNodes}
                    onChange={() => toggleHideFailedNodes()}
                >
                    {t('tree.showDetails')}
                </Checkbox>
                <Tooltip title={t('actions.downloadTooltip')}>
                    <Button
                        type="text"
                        size="small"
                        icon={<DownloadOutlined />}
                        onClick={() => projectId && traceService.exportTrace(projectId, showRealNumbers, false)}
                        disabled={executionStatus !== 'COMPLETED' || !projectId}
                    />
                </Tooltip>
            </div>
            <div className="trace-tree-content">
                {displayData.length > 0 ? (
                    <Tree
                        showIcon
                        showLine={{ showLeafIcon: false }}
                        loadData={handleLoadData}
                        onSelect={handleSelect}
                        selectedKeys={selectedTreeKey ? [selectedTreeKey] : []}
                        treeData={displayData}
                        blockNode
                    />
                ) : (
                    <Empty
                        description={t('tree.noNodes')}
                        image={Empty.PRESENTED_IMAGE_SIMPLE}
                    />
                )}
            </div>
        </div>
    )
}

export default TraceTree
