import { create } from 'zustand'
import type {
    TraceNodeView,
    TraceParameterValue,
    TraceExecutionStatus,
    TraceTreeDataNode,
} from 'types/trace'
import traceService from 'services/traceService'

interface TraceState {
    // Route params
    projectId: string | null
    tableId: string | null

    // Data
    rootNodes: TraceNodeView[]
    /** Selected tree key (path-based, e.g., "24-25") for tree highlighting */
    selectedTreeKey: string | null
    /** Selected node ID for API calls */
    selectedNodeId: number | null
    selectedNodeDetails: TraceNodeView | null

    // Tree data (transformed for Ant Design Tree)
    treeData: TraceTreeDataNode[]

    // UI State
    loading: boolean
    detailsLoading: boolean
    error: string | null
    showRealNumbers: boolean
    hideFailedNodes: boolean

    // Progress
    executionStatus: TraceExecutionStatus | null
    progressMessage: string | null

    // Actions
    setRouteParams: (projectId: string, tableId: string, showRealNumbers: boolean) => void
    fetchRootNodes: () => Promise<void>
    /** Fetch children for a node. treeKey is the path-based key, nodeId is the backend node ID. */
    fetchNodeChildren: (treeKey: string, nodeId: number) => Promise<TraceNodeView[]>
    /** Select a node. treeKey is for tree highlighting, nodeId is for API calls. */
    selectNode: (treeKey: string, nodeId: number) => Promise<void>
    fetchLazyParameter: (parameterId: number) => Promise<TraceParameterValue>
    setExecutionStatus: (status: TraceExecutionStatus, message?: string) => void
    toggleHideFailedNodes: () => void
    toggleShowRealNumbers: () => void
    updateTreeData: (parentKey: string, children: TraceTreeDataNode[]) => void
    reset: () => void
}

/**
 * Transform TraceNodeView to TraceTreeDataNode for Ant Design Tree.
 * Uses path-based keys to ensure uniqueness when same node appears under multiple parents.
 *
 * This handles duplicate nodes correctly - the same nodeId can appear multiple times
 * in the tree (e.g., node 25 under parents 24, 33, and 45), and each occurrence
 * gets a unique tree key based on its full path:
 * - "24-25" (node 25 under parent 24)
 * - "33-25" (node 25 under parent 33)
 * - "45-25" (node 25 under parent 45)
 *
 * Deeply nested duplicates also work: "24-25-30" vs "33-25-30" vs "45-25-30"
 *
 * @param node The trace node from backend
 * @param parentKey The parent's tree key (empty string for root level)
 */
const transformToTreeNode = (node: TraceNodeView, parentKey: string = ''): TraceTreeDataNode => ({
    key: parentKey ? `${parentKey}-${node.key}` : String(node.key),
    nodeId: node.key,
    title: node.title,
    tooltip: node.tooltip,
    type: node.type,
    extraClasses: node.extraClasses,
    isLeaf: !node.lazy,
    nodeData: node,
})

/**
 * Recursively update tree data with children for a specific node.
 * Uses path-based string keys for matching.
 */
const updateTreeChildren = (
    treeData: TraceTreeDataNode[],
    parentKey: string,
    children: TraceTreeDataNode[]
): TraceTreeDataNode[] => {
    return treeData.map((node) => {
        if (node.key === parentKey) {
            return { ...node, children }
        }
        if (node.children) {
            return {
                ...node,
                children: updateTreeChildren(node.children, parentKey, children),
            }
        }
        return node
    })
}

const initialState = {
    projectId: null,
    tableId: null,
    rootNodes: [],
    selectedTreeKey: null,
    selectedNodeId: null,
    selectedNodeDetails: null,
    treeData: [],
    loading: false,
    detailsLoading: false,
    error: null,
    showRealNumbers: false,
    hideFailedNodes: false,
    executionStatus: null,
    progressMessage: null,
}

export const useTraceStore = create<TraceState>((set, get) => ({
    ...initialState,

    setRouteParams: (projectId, tableId, showRealNumbers) => {
        set({ projectId, tableId, showRealNumbers })
    },

    fetchRootNodes: async () => {
        const { projectId, showRealNumbers } = get()
        if (!projectId) return

        set({ loading: true, error: null })
        try {
            // Get root nodes by calling getNodeChildren without nodeId
            const rootNodes = await traceService.getNodeChildren(projectId, undefined, showRealNumbers)
            const treeData = rootNodes.map(transformToTreeNode)
            set({
                rootNodes,
                treeData,
                loading: false,
                executionStatus: 'COMPLETED',
            })
        } catch (error: any) {
            const message = error?.message || 'Failed to load trace'
            set({ error: message, loading: false })
        }
    },

    fetchNodeChildren: async (treeKey, nodeId) => {
        const { projectId, showRealNumbers } = get()
        if (!projectId) return []

        try {
            const children = await traceService.getNodeChildren(projectId, nodeId, showRealNumbers)
            // Transform children with parent's tree key to generate unique path-based keys
            const childNodes = children.map(child => transformToTreeNode(child, treeKey))

            // Update tree data with children using the path-based tree key
            set((state) => ({
                treeData: updateTreeChildren(state.treeData, treeKey, childNodes),
            }))

            return children
        } catch (error: any) {
            console.error('Failed to fetch node children:', error)
            return []
        }
    },

    selectNode: async (treeKey, nodeId) => {
        const { projectId, showRealNumbers } = get()
        if (!projectId) return

        set({ selectedTreeKey: treeKey, selectedNodeId: nodeId, detailsLoading: true })
        try {
            const details = await traceService.getNodeDetails(projectId, nodeId, showRealNumbers)
            set({ selectedNodeDetails: details, detailsLoading: false })
        } catch (error: any) {
            console.error('Failed to fetch node details:', error)
            set({ selectedNodeDetails: null, detailsLoading: false })
        }
    },

    fetchLazyParameter: async (parameterId) => {
        const { projectId } = get()
        if (!projectId) throw new Error('No project ID')

        return traceService.getParameterValue(projectId, parameterId)
    },

    setExecutionStatus: (status, message) => {
        set({ executionStatus: status, progressMessage: message || null })
        // If completed, fetch the root nodes
        if (status === 'COMPLETED') {
            get().fetchRootNodes()
        }
    },

    toggleHideFailedNodes: () => {
        set((state) => ({ hideFailedNodes: !state.hideFailedNodes }))
    },

    toggleShowRealNumbers: () => {
        set((state) => ({ showRealNumbers: !state.showRealNumbers }))
        // Refetch with new setting
        get().fetchRootNodes()
    },

    updateTreeData: (parentKey: string, children: TraceTreeDataNode[]) => {
        set((state) => ({
            treeData: updateTreeChildren(state.treeData, parentKey, children),
        }))
    },

    reset: () => {
        set(initialState)
    },
}))
