import React, { useCallback, useMemo } from 'react'
import { Tree, Checkbox, Tooltip, Empty, Spin } from 'antd'
import type { TreeProps, TreeDataNode } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
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
        totalNodes,
        selectedNodeId,
        hideFailedNodes,
        toggleHideFailedNodes,
        fetchNodeChildren,
        selectNode,
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
     */
    const transformNode = useCallback(
        (node: TraceTreeDataNode): TreeDataNode => {
            const nodeClassName = getNodeClassName(node.extraClasses)
            return {
                key: node.key,
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
     */
    const handleLoadData: TreeProps['loadData'] = async (treeNode) => {
        const nodeId = treeNode.key as number
        await fetchNodeChildren(nodeId)
    }

    /**
     * Handle node selection.
     */
    const handleSelect: TreeProps['onSelect'] = (selectedKeys) => {
        if (selectedKeys.length > 0) {
            const nodeId = selectedKeys[0] as number
            selectNode(nodeId)
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
                {totalNodes > 0 && (
                    <span className="trace-tree-count">
                        {t('tree.totalNodes', { count: totalNodes })}
                    </span>
                )}
            </div>
            <div className="trace-tree-content">
                {displayData.length > 0 ? (
                    <Tree
                        showIcon
                        showLine={{ showLeafIcon: false }}
                        loadData={handleLoadData}
                        onSelect={handleSelect}
                        selectedKeys={selectedNodeId ? [selectedNodeId] : []}
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
