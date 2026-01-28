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
    selectedNodeId: number | null
    selectedNodeDetails: TraceNodeView | null
    traceTableHtml: string | null

    // Tree data (transformed for Ant Design Tree)
    treeData: TraceTreeDataNode[]

    // UI State
    loading: boolean
    detailsLoading: boolean
    tableLoading: boolean
    error: string | null
    showRealNumbers: boolean
    hideFailedNodes: boolean

    // Progress
    executionStatus: TraceExecutionStatus | null
    progressMessage: string | null

    // Actions
    setRouteParams: (projectId: string, tableId: string, showRealNumbers: boolean) => void
    fetchRootNodes: () => Promise<void>
    fetchNodeChildren: (nodeId: number) => Promise<TraceNodeView[]>
    selectNode: (nodeId: number) => Promise<void>
    fetchLazyParameter: (parameterId: number) => Promise<TraceParameterValue>
    fetchTraceTable: (nodeId: number) => Promise<void>
    setExecutionStatus: (status: TraceExecutionStatus, message?: string) => void
    toggleHideFailedNodes: () => void
    toggleShowRealNumbers: () => void
    updateTreeData: (parentKey: number, children: TraceTreeDataNode[]) => void
    reset: () => void
}

/**
 * Transform TraceNodeView to TraceTreeDataNode for Ant Design Tree.
 */
const transformToTreeNode = (node: TraceNodeView): TraceTreeDataNode => ({
    key: node.key,
    title: node.title,
    tooltip: node.tooltip,
    type: node.type,
    extraClasses: node.extraClasses,
    isLeaf: !node.lazy,
    nodeData: node,
})

/**
 * Recursively update tree data with children for a specific node.
 */
const updateTreeChildren = (
    treeData: TraceTreeDataNode[],
    parentKey: number,
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
    selectedNodeId: null,
    selectedNodeDetails: null,
    traceTableHtml: null,
    treeData: [],
    loading: false,
    detailsLoading: false,
    tableLoading: false,
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

    fetchNodeChildren: async (nodeId) => {
        const { projectId, showRealNumbers } = get()
        if (!projectId) return []

        try {
            const children = await traceService.getNodeChildren(projectId, nodeId, showRealNumbers)
            const childNodes = children.map(transformToTreeNode)

            // Update tree data with children
            set((state) => ({
                treeData: updateTreeChildren(state.treeData, nodeId, childNodes),
            }))

            return children
        } catch (error: any) {
            console.error('Failed to fetch node children:', error)
            return []
        }
    },

    selectNode: async (nodeId) => {
        const { projectId, showRealNumbers } = get()
        if (!projectId) return

        set({ selectedNodeId: nodeId, detailsLoading: true, traceTableHtml: null })
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

    fetchTraceTable: async (nodeId) => {
        const { projectId } = get()
        if (!projectId) return

        set({ tableLoading: true })
        try {
            const html = await traceService.getTraceTableHtml(projectId, nodeId)
            set({ traceTableHtml: html, tableLoading: false })
        } catch (error: any) {
            console.error('Failed to fetch trace table:', error)
            set({ traceTableHtml: null, tableLoading: false })
        }
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

    updateTreeData: (parentKey, children) => {
        set((state) => ({
            treeData: updateTreeChildren(state.treeData, parentKey, children),
        }))
    },

    reset: () => {
        set(initialState)
    },
}))
