import React, { useRef, useEffect } from 'react'
import { Spin, Tree } from 'antd'
import type { DataNode } from 'antd/es/tree'

interface TraceTreeProps {
    treeData: DataNode[]
    loading: boolean
    selectedKeys: React.Key[]
    expandedKeys: React.Key[]
    onSelect: (keys: React.Key[], info: any) => void
    onExpand: (expandedKeys: React.Key[]) => void
    loadData: (node: any) => void
    onContextMenu: (event: MouseEvent, nodeKey: number, path: number[], title?: string) => void
}

export const TraceTree: React.FC<TraceTreeProps> = ({
    treeData,
    loading,
    selectedKeys,
    expandedKeys,
    onSelect,
    onExpand,
    loadData,
    onContextMenu
}) => {
    const treeRef = useRef<HTMLDivElement>(null)

    // Handle right-click on tree nodes
    useEffect(() => {
        const handleContextMenu = (event: MouseEvent) => {
            const target = event.target as HTMLElement
            const treeNode = target.closest('.ant-tree-node-content-wrapper')
            
            if (treeNode && treeRef.current?.contains(treeNode)) {
                event.preventDefault()
                event.stopPropagation()
                
                // Try multiple ways to find the node key
                let nodeKey: number | null = null
                
                // Method 1: Look for data-node-key attribute
                const nodeKeyElement = treeNode.querySelector('[data-node-key]')
                if (nodeKeyElement) {
                    nodeKey = Number(nodeKeyElement.getAttribute('data-node-key'))
                }
                
                // Method 2: Look for the key in the tree node's parent li element
                if (!nodeKey) {
                    const liElement = treeNode.closest('li')
                    if (liElement) {
                        const keyAttr = liElement.getAttribute('data-node-key')
                        if (keyAttr) {
                            nodeKey = Number(keyAttr)
                        }
                    }
                }
                
                // Method 3: Look for the key in the tree node itself
                if (!nodeKey) {
                    const keyAttr = treeNode.getAttribute('data-node-key')
                    if (keyAttr) {
                        nodeKey = Number(keyAttr)
                    }
                }
                
                // Method 4: Look for the key in any child element
                if (!nodeKey) {
                    const allElements = treeNode.querySelectorAll('[data-node-key]')
                    for (const element of allElements) {
                        const key = Number(element.getAttribute('data-node-key'))
                        if (key) {
                            nodeKey = key
                            break
                        }
                    }
                }
                
                if (nodeKey) {
                    // Calculate the path to this node
                    const path: number[] = []
                    let currentElement = treeNode.closest('li')
                    
                    // Extract the title from the current node
                    let nodeTitle: string | undefined
                    const titleElement = treeNode.querySelector('[data-node-title]')
                    if (titleElement) {
                        nodeTitle = titleElement.getAttribute('data-node-title') || undefined
                    }
                    
                    // Walk up the tree to build the path
                    while (currentElement) {
                        // First, try to find the node key in the current element
                        const currentKeyElement = currentElement.querySelector('[data-node-key]')
                        if (currentKeyElement) {
                            const currentKey = Number(currentKeyElement.getAttribute('data-node-key'))
                            if (currentKey && currentKey !== nodeKey) {
                                path.unshift(currentKey)
                            }
                        }
                        
                        // Find the parent li element
                        const parentLi = currentElement.parentElement?.closest('li') || null
                        if (!parentLi) {
                            break
                        }
                        
                        currentElement = parentLi
                    }
                    
                    onContextMenu(event, nodeKey, path, nodeTitle)
                }
            }
        }

        const handleClick = (event: MouseEvent) => {
            const target = event.target as HTMLElement
            const isContextMenu = target.closest('[data-context-menu]')
            
            if (!isContextMenu) {
                // Hide context menu if clicking outside
                onContextMenu(event, 0, [], undefined)
            }
        }

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onContextMenu(event, 0, [], undefined)
            }
        }

        document.addEventListener('contextmenu', handleContextMenu, true)
        document.addEventListener('click', handleClick, true)
        document.addEventListener('keydown', handleKeyDown)

        return () => {
            document.removeEventListener('contextmenu', handleContextMenu, true)
            document.removeEventListener('click', handleClick, true)
            document.removeEventListener('keydown', handleKeyDown)
        }
    }, [onContextMenu])

    return (
        <div ref={treeRef} style={{ paddingTop: 8, height: 'calc(100vh - 120px)', overflow: 'auto' }}>
            {loading ? (
                <Spin />
            ) : (
                <Tree
                    showIcon
                    showLine
                    className="trace-tree"
                    expandedKeys={expandedKeys}
                    loadData={loadData}
                    onExpand={onExpand}
                    onSelect={onSelect}
                    selectedKeys={selectedKeys}
                    style={{ height: '100%' }}
                    treeData={treeData}
                />
            )}
        </div>
    )
}
