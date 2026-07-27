import { useEffect, useCallback, useRef } from 'react'
import { useWebSocket } from 'hooks/useWebSocket'
import { useTraceStore } from 'store'
import type { DebugStatus, TraceProgressMessage } from 'types/trace'

interface UseTraceProgressOptions {
    projectId: string
    tableId: string
    enabled?: boolean
}

/**
 * Hook for subscribing to trace execution progress via WebSocket.
 * Automatically connects and subscribes to the trace status topic.
 */
export const useTraceProgress = ({
    projectId,
    tableId,
    enabled = true,
}: UseTraceProgressOptions): void => {
    const { isConnected, subscribe, unsubscribe } = useWebSocket({
        autoConnect: enabled,
    })
    const onSocketStatus = useTraceStore(s => s.onSocketStatus)
    const subscriptionIdRef = useRef<string | null>(null)

    const handleMessage = useCallback(
        (message: { body: string }) => {
            try {
                // Try to parse as JSON first ({status, message, sessionId})
                const data: TraceProgressMessage = JSON.parse(message.body)
                onSocketStatus(data.status, data.message, data.sessionId)
            } catch {
                // Fall back to a plain status string
                onSocketStatus(message.body as DebugStatus)
            }
        },
        [onSocketStatus]
    )

    useEffect(() => {
        if (!enabled || !isConnected || !projectId || !tableId) {
            return
        }

        // Subscribe to trace progress topic
        const topic = `/user/topic/projects/${encodeURIComponent(projectId)}/tables/${encodeURIComponent(tableId)}/trace/status`

        subscriptionIdRef.current = subscribe(
            topic,
            handleMessage,
            `trace-progress-${projectId}-${tableId}`
        )

        // Cleanup on unmount or when dependencies change
        return () => {
            if (subscriptionIdRef.current) {
                unsubscribe(subscriptionIdRef.current)
                subscriptionIdRef.current = null
            }
        }
    }, [enabled, isConnected, projectId, tableId, subscribe, unsubscribe, handleMessage])
}

export default useTraceProgress
