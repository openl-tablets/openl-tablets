import type { DataNode } from 'antd/es/tree'

/**
 * Извлекает заголовок узла из React элемента или строки
 */
export const extractNodeTitle = (node: DataNode): string => {
    if (typeof node.title === 'string') {
        return node.title
    }
    
    if (node.title && typeof node.title === 'object' && 'props' in node.title && node.title.props) {
        const titleElement = node.title as any
        if (titleElement.props.children) {
            const children = titleElement.props.children
            if (Array.isArray(children)) {
                for (const child of children) {
                    if (typeof child === 'string') {
                        return child
                    } else if (child && child.props && child.props.children) {
                        const grandChild = child.props.children
                        if (typeof grandChild === 'string') {
                            return grandChild
                        }
                    }
                }
            } else if (typeof children === 'string') {
                return children
            }
        }
    }
    
    return `Node ${node.key}`
}

/**
 * Находит путь к узлу в дереве
 */
export const findNodePath = (nodes: DataNode[], targetKey: number, currentPath: number[] = []): number[] | null => {
    for (const node of nodes) {
        const nodeKeyNum = Number(node.key)
        if (nodeKeyNum === targetKey) {
            return [...currentPath]
        }
        
        if (node.children && node.children.length > 0) {
            const newPath = [...currentPath, nodeKeyNum]
            const result = findNodePath(node.children, targetKey, newPath)
            if (result) {
                return result
            }
        }
    }
    return null
}

/**
 * Получает путь и заголовок узла
 */
export const getNodePathAndTitle = (nodes: DataNode[], nodeKey: number): { path: number[], title: string } => {
    const path = findNodePath(nodes, nodeKey) || []
    const node = findNodeInTree(nodes, nodeKey)
    const title = node ? extractNodeTitle(node) : `Node ${nodeKey}`
    
    return { path, title }
}

/**
 * Находит узел в дереве по ключу
 */
export const findNodeInTree = (nodes: DataNode[], targetKey: number): DataNode | null => {
    for (const node of nodes) {
        if (Number(node.key) === targetKey) {
            return node
        }
        
        if (node.children && node.children.length > 0) {
            const result = findNodeInTree(node.children, targetKey)
            if (result) {
                return result
            }
        }
    }
    return null
}
