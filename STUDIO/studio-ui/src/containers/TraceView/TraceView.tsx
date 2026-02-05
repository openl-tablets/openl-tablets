import React, { useEffect, useRef, useState, useCallback } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'
import { Alert, notification } from 'antd'
import { useTranslation } from 'react-i18next'
import { useTraceStore } from 'store'
import TraceTree from './components/TraceTree'
import TraceDetails from './components/TraceDetails'
import TraceProgress from './components/TraceProgress'
import useTraceProgress from './hooks/useTraceProgress'
import traceService from 'services/traceService'
import './TraceView.scss'

interface TraceViewParams {
    projectId: string
}

/**
 * Main trace view container.
 * Standalone page with resizable split layout between tree and details panels.
 *
 * URL format: /trace/{projectId}?tableId={tableId}&showRealNumbers={true|false}
 */
const TraceView: React.FC = () => {
    const { t } = useTranslation('trace')
    const { projectId } = useParams<keyof TraceViewParams>()
    const [searchParams] = useSearchParams()
    const tableId = searchParams.get('tableId')
    const showRealNumbers = searchParams.get('showRealNumbers') === 'true'

    const {
        setRouteParams,
        fetchRootNodes,
        executionStatus,
        setExecutionStatus,
        progressMessage,
        error,
        reset,
    } = useTraceStore()

    // Resizer state
    const [leftPanelWidth, setLeftPanelWidth] = useState(35) // percentage
    const [isResizing, setIsResizing] = useState(false)
    const containerRef = useRef<HTMLDivElement>(null)

    // WebSocket subscription for progress updates
    useTraceProgress({
        projectId: projectId || '',
        tableId: tableId || '',
        enabled: !!projectId && !!tableId,
    })

    // Initialize on mount
    useEffect(() => {
        if (projectId && tableId) {
            setRouteParams(projectId, tableId, showRealNumbers)
            fetchRootNodes()
        }

        return () => {
            reset()
        }
    }, [projectId, tableId, showRealNumbers, setRouteParams, fetchRootNodes, reset])

    // Handle cancel trace
    const handleCancelTrace = useCallback(async () => {
        if (!projectId) return
        try {
            await traceService.cancelTrace(projectId)
            // Update local status since backend doesn't emit WebSocket status on cancel
            setExecutionStatus('INTERRUPTED')
        } catch (err) {
            console.error('Failed to cancel trace:', err)
            const errorMessage = err instanceof Error ? err.message : String(err)
            notification.error({
                message: t('errors.cancelFailed'),
                description: errorMessage,
            })
        }
    }, [projectId, setExecutionStatus, t])

    // Resizer handlers
    const handleMouseDown = useCallback((e: React.MouseEvent) => {
        e.preventDefault()
        setIsResizing(true)
    }, [])

    const handleMouseMove = useCallback(
        (e: MouseEvent) => {
            if (!isResizing || !containerRef.current) return

            const containerRect = containerRef.current.getBoundingClientRect()
            const newWidth = ((e.clientX - containerRect.left) / containerRect.width) * 100

            // Limit to reasonable bounds (15% - 70%)
            if (newWidth >= 15 && newWidth <= 70) {
                setLeftPanelWidth(newWidth)
            }
        },
        [isResizing]
    )

    const handleMouseUp = useCallback(() => {
        setIsResizing(false)
    }, [])

    // Attach global mouse events for resizing
    useEffect(() => {
        if (isResizing) {
            document.addEventListener('mousemove', handleMouseMove)
            document.addEventListener('mouseup', handleMouseUp)
        }

        return () => {
            document.removeEventListener('mousemove', handleMouseMove)
            document.removeEventListener('mouseup', handleMouseUp)
        }
    }, [isResizing, handleMouseMove, handleMouseUp])

    // Determine if progress overlay should be shown
    const showProgressOverlay =
        executionStatus === 'PENDING' ||
        executionStatus === 'STARTED' ||
        executionStatus === 'ERROR' ||
        executionStatus === 'INTERRUPTED'

    if (!projectId || !tableId) {
        return (
            <div id="trace-view" className="trace-view-error">
                <Alert
                    type="error"
                    message={t('errors.notFound')}
                    description={t('errors.missingParams')}
                />
            </div>
        )
    }

    return (
        <div
            id="trace-view"
            ref={containerRef}
            className={isResizing ? 'resizing' : ''}
        >
            {/* Progress Overlay */}
            <TraceProgress
                status={executionStatus}
                message={progressMessage || undefined}
                onCancel={handleCancelTrace}
                visible={showProgressOverlay}
            />

            {/* Error Banner */}
            {error && !showProgressOverlay && (
                <Alert
                    type="error"
                    message={error}
                    className="trace-error-banner"
                    closable
                />
            )}

            {/* Left Panel - Tree */}
            <div
                className="trace-left-panel"
                style={{ width: `${leftPanelWidth}%` }}
            >
                <TraceTree />
            </div>

            {/* Resizer */}
            <div
                className="trace-resizer"
                onMouseDown={handleMouseDown}
            />

            {/* Right Panel - Details */}
            <div
                className="trace-right-panel"
                style={{ width: `${100 - leftPanelWidth}%` }}
            >
                <TraceDetails />
            </div>
        </div>
    )
}

export default TraceView
