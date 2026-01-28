import apiCall from './apiCall'
import type {
    TraceNodeView,
    TraceParameterValue,
} from 'types/trace'

/**
 * API service for trace execution endpoints.
 * All endpoints require project-level READ access.
 */
export const traceService = {
    /**
     * Get node children for lazy loading, or root nodes if nodeId is not provided.
     * @param nodeId Parent node ID to get children for (omit for root nodes)
     */
    getNodeChildren: (
        projectId: string,
        nodeId?: number,
        showRealNumbers = false
    ): Promise<TraceNodeView[]> => {
        const params = new URLSearchParams()
        if (nodeId !== undefined) {
            params.set('id', String(nodeId))
        }
        params.set('showRealNumbers', String(showRealNumbers))
        return apiCall(
            `/projects/${encodeURIComponent(projectId)}/trace/nodes?${params.toString()}`,
            undefined,
            { throwError: true }
        )
    },

    /**
     * Get full node details including parameters, context, result, errors.
     * @param nodeId Node ID to get details for
     */
    getNodeDetails: (
        projectId: string,
        nodeId: number,
        showRealNumbers = false
    ): Promise<TraceNodeView> =>
        apiCall(
            `/projects/${encodeURIComponent(projectId)}/trace/nodes/${nodeId}?showRealNumbers=${showRealNumbers}`,
            undefined,
            { throwError: true }
        ),

    /**
     * Get lazy-loaded parameter value.
     * @param parameterId Parameter ID from TraceParameterValue
     */
    getParameterValue: (
        projectId: string,
        parameterId: number
    ): Promise<TraceParameterValue> =>
        apiCall(
            `/projects/${encodeURIComponent(projectId)}/trace/parameters/${parameterId}`,
            undefined,
            { throwError: true }
        ),

    /**
     * Get traced table HTML fragment with highlighted cells.
     * @param nodeId Node ID to get table for
     * @param showFormulas Show formulas instead of values
     */
    getTraceTableHtml: async (
        projectId: string,
        nodeId: number,
        showFormulas = false
    ): Promise<string> => {
        const response = await fetch(
            `${window.location.pathname.replace(/\/[^/]*$/, '')}/web/projects/${encodeURIComponent(projectId)}/trace/nodes/${nodeId}/table?showFormulas=${showFormulas}`,
            {
                headers: { Accept: 'text/html' },
            }
        )
        if (!response.ok) {
            throw new Error(`Failed to fetch table: ${response.status}`)
        }
        return response.text()
    },

    /**
     * Cancel ongoing trace execution.
     * Returns 204 on success, 404 if no trace exists.
     */
    cancelTrace: (projectId: string): Promise<void> =>
        apiCall(
            `/projects/${encodeURIComponent(projectId)}/trace`,
            { method: 'DELETE' },
            { throwError: true }
        ),
}

export default traceService
